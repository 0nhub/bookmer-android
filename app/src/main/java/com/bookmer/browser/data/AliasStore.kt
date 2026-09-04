package com.bookmer.browser.data

import android.content.Context
import android.location.Geocoder
import android.webkit.WebSettings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors

data class AliasNamedItem(val code: String, val name: String)

data class AliasPlaceSuggestion(
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
)

enum class AliasOs(val label: String) {
    DEFAULT("Default"),
    ANDROID("Android"),
    IOS("iOS"),
    WINDOWS("Windows"),
    MACOS("macOS"),
    LINUX("Linux"),
}

/**
 * Settings → Browser Data → Metadata — mirrors iOS [BookmerAliasStore].
 * Default = real device values. Lists come from the platform Locale / TimeZone APIs.
 */
class AliasStore(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val geoExecutor = Executors.newSingleThreadExecutor()

    var operatingSystem by mutableStateOf(AliasOs.DEFAULT)
        private set
    var countryCode by mutableStateOf<String?>(null)
        private set
    var languageCode by mutableStateOf<String?>(null)
        private set
    var locationName by mutableStateOf<String?>(null)
        private set
    var locationLatitude by mutableStateOf<Double?>(null)
        private set
    var locationLongitude by mutableStateOf<Double?>(null)
        private set
    var timeZoneIdentifier by mutableStateOf<String?>(null)
        private set
    var revision by mutableIntStateOf(0)
        private set

    val hasLocation: Boolean
        get() = locationName != null && locationLatitude != null && locationLongitude != null

    val isOff: Boolean
        get() = operatingSystem == AliasOs.DEFAULT
            && countryCode == null
            && languageCode == null
            && !hasLocation
            && timeZoneIdentifier == null

    val operatingSystemLabel: String get() = operatingSystem.label
    val countryLabel: String
        get() = countryCode?.let { countryName(it) ?: it } ?: "Default"
    val locationLabel: String get() = locationName ?: "Default"
    val languageLabel: String
        get() = languageCode?.let { languageName(it) ?: it } ?: "Default"
    val timeZoneLabel: String
        get() = timeZoneIdentifier?.let { timeZoneDisplayName(it) } ?: "Default"

    init {
        load()
    }

    fun chooseOperatingSystem(value: AliasOs) {
        if (operatingSystem == value) return
        operatingSystem = value
        preferences.edit().putString(KEY_OS, value.name).apply()
        bump()
    }

    fun chooseCountry(code: String?) {
        val next = code?.uppercase()?.takeIf { it.isNotBlank() }
        if (countryCode == next) return
        countryCode = next
        preferences.edit().putString(KEY_COUNTRY, next.orEmpty()).apply()
        bump()
    }

    fun chooseLanguage(code: String?) {
        val next = code?.lowercase()?.takeIf { it.isNotBlank() }
        if (languageCode == next) return
        languageCode = next
        preferences.edit().putString(KEY_LANGUAGE, next.orEmpty()).apply()
        bump()
    }

    fun chooseLocation(name: String?, latitude: Double?, longitude: Double?) {
        val cleaned = name?.trim()?.takeIf { it.isNotEmpty() }
        val hasPair = cleaned != null && latitude != null && longitude != null
        val nameToStore = if (hasPair) cleaned else null
        val latToStore = if (hasPair) latitude else null
        val lonToStore = if (hasPair) longitude else null
        if (locationName == nameToStore && locationLatitude == latToStore && locationLongitude == lonToStore) return
        locationName = nameToStore
        locationLatitude = latToStore
        locationLongitude = lonToStore
        preferences.edit().apply {
            putString(KEY_LOCATION_NAME, nameToStore.orEmpty())
            if (latToStore != null) putString(KEY_LOCATION_LAT, latToStore.toString()) else remove(KEY_LOCATION_LAT)
            if (lonToStore != null) putString(KEY_LOCATION_LON, lonToStore.toString()) else remove(KEY_LOCATION_LON)
        }.apply()
        bump()
    }

    fun clearLocation() = chooseLocation(null, null, null)

    fun chooseTimeZone(identifier: String?) {
        val next = identifier?.trim()?.takeIf { it.isNotEmpty() }
        if (timeZoneIdentifier == next) return
        timeZoneIdentifier = next
        preferences.edit().putString(KEY_TIMEZONE, next.orEmpty()).apply()
        bump()
    }

    fun effectiveDesktop(tabPrefersDesktop: Boolean): Boolean {
        if (tabPrefersDesktop) return true
        return when (operatingSystem) {
            AliasOs.WINDOWS, AliasOs.MACOS, AliasOs.LINUX -> true
            AliasOs.DEFAULT, AliasOs.ANDROID, AliasOs.IOS -> false
        }
    }

    fun userAgent(context: Context, desktop: Boolean): String {
        val mobileDefault = WebSettings.getDefaultUserAgent(context)
        return when (operatingSystem) {
            AliasOs.DEFAULT -> if (desktop) CHROME_DESKTOP_UA else mobileDefault
            AliasOs.IOS -> if (desktop) {
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15"
            } else {
                mobileDefault.replace(Regex("""\([^)]*\)"""), "(iPhone; CPU iPhone OS 18_0 like Mac OS X)")
            }
            AliasOs.ANDROID -> if (desktop) CHROME_DESKTOP_UA else CHROME_ANDROID_UA
            AliasOs.WINDOWS -> CHROME_WINDOWS_UA
            AliasOs.MACOS -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15"
            AliasOs.LINUX -> CHROME_LINUX_UA
        }
    }

    fun injectionScript(tabPrefersDesktop: Boolean): String {
        val payload = scriptPayloadJson(tabPrefersDesktop)
        return "window.__bookmerAlias=$payload;\n$SCRIPT_BODY"
    }

    fun searchPlaces(query: String, onResult: (List<AliasPlaceSuggestion>) -> Unit) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            onResult(emptyList())
            return
        }
        geoExecutor.execute {
            val results = runCatching {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.getDefault())
                    .getFromLocationName(trimmed, 10)
                    .orEmpty()
                    .mapNotNull { address ->
                        val lat = address.latitude
                        val lon = address.longitude
                        val city = address.locality
                            ?: address.subAdminArea
                            ?: address.featureName
                            ?: return@mapNotNull null
                        val subtitle = listOfNotNull(
                            address.adminArea,
                            address.countryName,
                        ).joinToString(", ")
                        AliasPlaceSuggestion(city, subtitle, lat, lon)
                    }
                    .distinctBy { "${it.name.lowercase()}|${it.latitude}|${it.longitude}" }
            }.getOrDefault(emptyList())
            android.os.Handler(android.os.Looper.getMainLooper()).post { onResult(results) }
        }
    }

    /** Migrate legacy AppSettings metadata strings once. */
    fun migrateFromLegacySettings(settings: AppSettings) {
        if (preferences.getBoolean(KEY_MIGRATED, false)) return
        if (operatingSystem == AliasOs.DEFAULT && settings.metadataOs != "Default") {
            AliasOs.entries.find { it.label.equals(settings.metadataOs, ignoreCase = true) }?.let {
                if (it != AliasOs.DEFAULT) chooseOperatingSystem(it)
            }
        }
        if (languageCode == null && settings.metadataLanguage != "Default") {
            val code = settings.metadataLanguage.substringBefore('-').lowercase()
            if (code.length == 2) chooseLanguage(code)
        }
        if (timeZoneIdentifier == null && settings.metadataTimeZone != "Default") {
            chooseTimeZone(settings.metadataTimeZone)
        }
        preferences.edit().putBoolean(KEY_MIGRATED, true).apply()
    }

    private fun bump() {
        revision += 1
    }

    private fun load() {
        operatingSystem = runCatching {
            AliasOs.valueOf(preferences.getString(KEY_OS, AliasOs.DEFAULT.name) ?: AliasOs.DEFAULT.name)
        }.getOrDefault(AliasOs.DEFAULT)
        countryCode = preferences.getString(KEY_COUNTRY, null)?.takeIf { it.isNotBlank() }
        languageCode = preferences.getString(KEY_LANGUAGE, null)?.takeIf { it.isNotBlank() }
        locationName = preferences.getString(KEY_LOCATION_NAME, null)?.takeIf { it.isNotBlank() }
        locationLatitude = preferences.getString(KEY_LOCATION_LAT, null)?.toDoubleOrNull()
        locationLongitude = preferences.getString(KEY_LOCATION_LON, null)?.toDoubleOrNull()
        if (!hasLocation) {
            locationName = null
            locationLatitude = null
            locationLongitude = null
        }
        timeZoneIdentifier = preferences.getString(KEY_TIMEZONE, null)?.takeIf { it.isNotBlank() }
    }

    private fun scriptPayloadJson(tabPrefersDesktop: Boolean): String {
        val payload = JSONObject().put("off", isOff)
        if (isOff) return payload.toString()

        if (operatingSystem != AliasOs.DEFAULT) {
            val desktop = effectiveDesktop(tabPrefersDesktop)
            val ua = userAgent(context, desktop)
            payload.put("ua", ua)
            payload.put("appVersion", appVersion(ua))
            payload.put("platform", jsPlatform(operatingSystem, desktop))
            payload.put("vendor", jsVendor(operatingSystem))
            payload.put("mobile", !desktop)
            payload.put("touch", if (desktop) 0 else 5)
            if (operatingSystem == AliasOs.ANDROID || operatingSystem == AliasOs.WINDOWS || operatingSystem == AliasOs.LINUX) {
                payload.put("uaPlatform", uaDataPlatform(operatingSystem))
                payload.put("platformVersion", uaDataPlatformVersion(operatingSystem))
                payload.put("architecture", if (operatingSystem == AliasOs.ANDROID) "arm" else "x86")
                payload.put("model", if (operatingSystem == AliasOs.ANDROID) "Pixel 8" else "")
                payload.put("uaFullVersion", "131.0.6778.135")
                payload.put(
                    "brands",
                    JSONArray().apply {
                        put(JSONObject().put("brand", "Not(A:Brand").put("version", "8"))
                        put(JSONObject().put("brand", "Chromium").put("version", "131"))
                        put(JSONObject().put("brand", "Google Chrome").put("version", "131"))
                    },
                )
            }
        }

        languageCode?.let { lang ->
            val primary = countryCode?.let { "$lang-$it" } ?: lang
            payload.put("language", primary)
            payload.put(
                "languages",
                JSONArray().apply {
                    put(primary)
                    if (primary != lang) put(lang)
                },
            )
        }

        intlLocaleIdentifier()?.let { localeId ->
            payload.put("locale", localeId)
            payload.put("hour12", usesHour12(localeId))
        } ?: countryCode?.let {
            payload.put("hour12", usesHour12("en-$it"))
        }

        timeZoneIdentifier?.let { payload.put("timeZone", it) }

        if (locationLatitude != null && locationLongitude != null) {
            payload.put(
                "geo",
                JSONObject().put("lat", locationLatitude).put("lon", locationLongitude),
            )
        }
        return payload.toString()
    }

    private fun intlLocaleIdentifier(): String? = when {
        languageCode != null && countryCode != null -> "$languageCode-$countryCode"
        languageCode != null -> languageCode
        else -> null
    }

    companion object {
        private const val PREFS = "bookmer.alias"
        private const val KEY_OS = "os"
        private const val KEY_COUNTRY = "country"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_LOCATION_NAME = "locationName"
        private const val KEY_LOCATION_LAT = "locationLat"
        private const val KEY_LOCATION_LON = "locationLon"
        private const val KEY_TIMEZONE = "timeZone"
        private const val KEY_MIGRATED = "migratedFromSettings"

        private const val CHROME_ANDROID_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        private const val CHROME_DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        private const val CHROME_WINDOWS_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        private const val CHROME_LINUX_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        val countries: List<AliasNamedItem> by lazy {
            Locale.getISOCountries()
                .mapNotNull { code ->
                    val upper = code.uppercase()
                    val name = Locale("", upper).displayCountry.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    AliasNamedItem(upper, name)
                }
                .sortedBy { it.name.lowercase(Locale.getDefault()) }
        }

        val languages: List<AliasNamedItem> by lazy {
            Locale.getISOLanguages()
                .mapNotNull { code ->
                    val id = code.lowercase()
                    if (id.length != 2) return@mapNotNull null
                    val name = Locale(id).displayLanguage.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    AliasNamedItem(id, name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                }
                .distinctBy { it.code }
                .sortedBy { it.name.lowercase(Locale.getDefault()) }
        }

        val timeZones: List<AliasNamedItem> by lazy {
            TimeZone.getAvailableIDs()
                .filter { it.contains('/') }
                .map { id -> AliasNamedItem(id, timeZoneDisplayName(id)) }
                .sortedBy { it.name.lowercase(Locale.getDefault()) }
        }

        fun countryName(code: String): String? =
            Locale("", code.uppercase()).displayCountry.takeIf { it.isNotBlank() }

        fun languageName(code: String): String? {
            val name = Locale(code.lowercase()).displayLanguage.takeIf { it.isNotBlank() } ?: return null
            return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

        fun timeZoneDisplayName(identifier: String): String {
            val city = identifier.substringAfterLast('/')
                .replace('_', ' ')
            val abbrev = TimeZone.getTimeZone(identifier).getDisplayName(false, TimeZone.SHORT, Locale.getDefault())
            return if (abbrev.isNotBlank() && abbrev != identifier) "$city ($abbrev)" else city
        }

        fun filterItems(items: List<AliasNamedItem>, query: String): List<AliasNamedItem> {
            val q = query.trim()
            if (q.isEmpty()) return items
            return items.filter {
                it.name.contains(q, ignoreCase = true) || it.code.contains(q, ignoreCase = true)
            }
        }

        private fun appVersion(ua: String): String {
            val idx = ua.indexOf("Mozilla/")
            return if (idx >= 0) ua.substring(idx + "Mozilla/".length) else ua
        }

        private fun jsPlatform(os: AliasOs, desktop: Boolean): String = when (os) {
            AliasOs.DEFAULT, AliasOs.IOS -> if (desktop) "MacIntel" else "iPhone"
            AliasOs.ANDROID -> "Linux armv8l"
            AliasOs.WINDOWS -> "Win32"
            AliasOs.MACOS -> "MacIntel"
            AliasOs.LINUX -> "Linux x86_64"
        }

        private fun jsVendor(os: AliasOs): String = when (os) {
            AliasOs.ANDROID, AliasOs.WINDOWS, AliasOs.LINUX -> "Google Inc."
            AliasOs.DEFAULT, AliasOs.IOS, AliasOs.MACOS -> "Apple Computer, Inc."
        }

        private fun uaDataPlatform(os: AliasOs): String = when (os) {
            AliasOs.ANDROID -> "Android"
            AliasOs.WINDOWS -> "Windows"
            AliasOs.LINUX -> "Linux"
            AliasOs.MACOS -> "macOS"
            AliasOs.IOS, AliasOs.DEFAULT -> "iOS"
        }

        private fun uaDataPlatformVersion(os: AliasOs): String = when (os) {
            AliasOs.ANDROID -> "14.0.0"
            AliasOs.WINDOWS -> "15.0.0"
            AliasOs.LINUX -> "6.8.0"
            AliasOs.MACOS -> "14.6.1"
            AliasOs.IOS, AliasOs.DEFAULT -> "18.5.0"
        }

        private fun usesHour12(localeId: String): Boolean {
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.forLanguageTag(localeId), "j")
            return pattern.contains('a') || pattern.contains('h') || pattern.contains('K')
        }

        // Same spoof body as iOS BookmerAlias.swift
        private const val SCRIPT_BODY = """
(function() {
  var cfg = window.__bookmerAlias;
  if (!cfg || cfg.off) return;
  if (window.__bookmerAliasInstalled) return;
  window.__bookmerAliasInstalled = true;

  function override(obj, key, getter) {
    try {
      Object.defineProperty(obj, key, {
        configurable: true,
        enumerable: true,
        get: getter
      });
    } catch (e) {}
  }

  function overrideNav(key, value) {
    override(Navigator.prototype, key, function() { return value; });
    override(navigator, key, function() { return value; });
  }

  if (cfg.ua) {
    overrideNav('userAgent', cfg.ua);
    if (cfg.appVersion) overrideNav('appVersion', cfg.appVersion);
    if (cfg.platform) overrideNav('platform', cfg.platform);
    if (cfg.vendor) overrideNav('vendor', cfg.vendor);
    if (typeof cfg.touch === 'number') overrideNav('maxTouchPoints', cfg.touch);
  }

  if (cfg.language) {
    overrideNav('language', cfg.language);
    var langs = cfg.languages || [cfg.language];
    overrideNav('languages', Object.freeze(langs.slice()));
  }

  if (cfg.brands && cfg.uaPlatform) {
    var uad = {
      brands: cfg.brands,
      mobile: !!cfg.mobile,
      platform: cfg.uaPlatform,
      getHighEntropyValues: function() {
        return Promise.resolve({
          brands: cfg.brands,
          mobile: !!cfg.mobile,
          platform: cfg.uaPlatform,
          platformVersion: cfg.platformVersion || '',
          architecture: cfg.architecture || '',
          bitness: '64',
          model: cfg.model || '',
          uaFullVersion: cfg.uaFullVersion || '',
          fullVersionList: cfg.brands
        });
      },
      toJSON: function() {
        return { brands: cfg.brands, mobile: !!cfg.mobile, platform: cfg.uaPlatform };
      }
    };
    overrideNav('userAgentData', uad);
  }

  var OrigDTF = Intl.DateTimeFormat;
  var OrigNumberFormat = Intl.NumberFormat;

  function tzOffsetMinutes(date) {
    if (!cfg.timeZone) return date.getTimezoneOffset();
    var fmt = new OrigDTF('en-US', {
      timeZone: cfg.timeZone,
      hourCycle: 'h23',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
    var parts = fmt.formatToParts(date);
    var map = {};
    for (var i = 0; i < parts.length; i++) map[parts[i].type] = parts[i].value;
    var asUTC = Date.UTC(
      Number(map.year),
      Number(map.month) - 1,
      Number(map.day),
      Number(map.hour),
      Number(map.minute),
      Number(map.second)
    );
    return Math.round((date.getTime() - asUTC) / 60000);
  }

  function pad(n) {
    return n < 10 ? '0' + n : String(n);
  }

  function formatOrderedDate(date) {
    var y = date.getFullYear();
    var m = pad(date.getMonth() + 1);
    var d = pad(date.getDate());
    if (cfg.dateOrder === 'dmy') return d + '/' + m + '/' + y;
    if (cfg.dateOrder === 'mdy') return m + '/' + d + '/' + y;
    if (cfg.dateOrder === 'ymd') return y + '-' + m + '-' + d;
    return null;
  }

  function patchedDTF(locales, options) {
    var loc = locales;
    var opts = options ? Object.assign({}, options) : {};
    if (!locales && cfg.locale) loc = cfg.locale;
    if (cfg.timeZone && !opts.timeZone) opts.timeZone = cfg.timeZone;
    if (typeof cfg.hour12 === 'boolean' && opts.hour12 === undefined && !opts.hourCycle) {
      opts.hour12 = cfg.hour12;
    }
    return new OrigDTF(loc, opts);
  }
  patchedDTF.prototype = OrigDTF.prototype;
  patchedDTF.supportedLocalesOf = OrigDTF.supportedLocalesOf.bind(OrigDTF);
  Intl.DateTimeFormat = patchedDTF;

  var origResolved = OrigDTF.prototype.resolvedOptions;
  OrigDTF.prototype.resolvedOptions = function() {
    var o = origResolved.call(this);
    if (cfg.timeZone) o.timeZone = cfg.timeZone;
    if (cfg.locale) o.locale = cfg.locale;
    return o;
  };

  var origToLocaleDateString = Date.prototype.toLocaleDateString;
  Date.prototype.toLocaleDateString = function(locales, options) {
    var ordered = formatOrderedDate(this);
    if (ordered && (!options || (!options.weekday && !options.dateStyle && !options.era))) {
      return ordered;
    }
    return origToLocaleDateString.call(this, locales || cfg.locale, options);
  };

  var origToLocaleString = Date.prototype.toLocaleString;
  Date.prototype.toLocaleString = function(locales, options) {
    return origToLocaleString.call(this, locales || cfg.locale, options);
  };

  var origToLocaleTimeString = Date.prototype.toLocaleTimeString;
  Date.prototype.toLocaleTimeString = function(locales, options) {
    return origToLocaleTimeString.call(this, locales || cfg.locale, options);
  };

  if (cfg.timeZone) {
    Date.prototype.getTimezoneOffset = function() {
      return tzOffsetMinutes(this);
    };
  }

  function patchedNumberFormat(locales, options) {
    var loc = locales;
    var opts = options || {};
    if (cfg.units && (opts.style === 'unit' || opts.unit)) {
      loc = cfg.units === 'imperial' ? 'en-US' : 'de-DE';
    } else if (!locales && cfg.locale) {
      loc = cfg.locale;
    }
    return new OrigNumberFormat(loc, opts);
  }
  patchedNumberFormat.prototype = OrigNumberFormat.prototype;
  patchedNumberFormat.supportedLocalesOf = OrigNumberFormat.supportedLocalesOf.bind(OrigNumberFormat);
  Intl.NumberFormat = patchedNumberFormat;

  if (cfg.geo && navigator.geolocation) {
    var makePos = function() {
      return {
        coords: {
          latitude: cfg.geo.lat,
          longitude: cfg.geo.lon,
          accuracy: 50,
          altitude: null,
          altitudeAccuracy: null,
          heading: null,
          speed: null
        },
        timestamp: Date.now()
      };
    };
    navigator.geolocation.getCurrentPosition = function(success, error) {
      if (typeof success === 'function') {
        setTimeout(function() { success(makePos()); }, 0);
      }
    };
    navigator.geolocation.watchPosition = function(success, error) {
      if (typeof success === 'function') {
        setTimeout(function() { success(makePos()); }, 0);
      }
      return 1;
    };
  }
})();
"""
    }
}

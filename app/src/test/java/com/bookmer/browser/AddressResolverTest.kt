package com.bookmer.browser

import com.bookmer.browser.browser.AddressResolver
import com.bookmer.browser.data.AppSettings
import com.bookmer.browser.data.BookmerUrls
import com.bookmer.browser.data.CustomSearchEngine
import com.bookmer.browser.data.SearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddressResolverTest {
    @Test fun emptyInputDoesNothing() = assertNull(AddressResolver.resolve("   ", AppSettings()))

    @Test fun homeMarkerStaysNative() =
        assertEquals(BookmerUrls.HOME, AddressResolver.resolve(BookmerUrls.HOME, AppSettings()))

    @Test fun hostGetsHttps() =
        assertEquals("https://example.com", AddressResolver.resolve("example.com", AppSettings()))

    @Test fun fullUrlIsPreserved() =
        assertEquals("https://example.com/path", AddressResolver.resolve("https://example.com/path", AppSettings()))

    @Test fun queryUsesSelectedEngine() =
        assertEquals("https://www.google.com/search?q=bookmer%20browser",
            AddressResolver.resolve("bookmer browser", AppSettings(searchEngine = SearchEngine.GOOGLE)))

    @Test fun customEngineWins() {
        val custom = CustomSearchEngine(id = "custom", name = "Custom", template = "https://search.example/?q=@@@")
        assertEquals("https://search.example/?q=hello",
            AddressResolver.resolve("hello", AppSettings(customSearchEngines = listOf(custom), selectedCustomSearchEngineId = "custom")))
    }
}

# Billing — Google Play PRO

## Produkt

| | |
|--|--|
| Play-Product-ID | `com.bookmer.browser.pro.yearly` |
| Typ | Jährliches Auto-Renew-Abo |
| Zielpreis | 29 € / Jahr (Store lokalisiert die Anzeige) |
| Client | `data/BookmerProStore.kt` |

## Client-Ablauf

1. `BookmerProStore.start()` verbindet BillingClient, lädt Produktdetails, prüft Käufe.
2. Kauf → Acknowledge → Purchase-Token behalten.
3. Wenn angemeldet → `BookmerApiClient.registerGoogleSubscription` → **`POST /pay/google`** mit Token / Package / Product-ID.
4. Server prüft über Google Play Developer API und schreibt dieselbe PRO-Periode wie Stripe / Apple.
5. `isPro` = Play-Entitlement **oder** `session.hasPro`.

## Backend (bookmer-platform — nicht in diesem Repo)

Erwartete Teile:

- Route `POST /pay/google`
- Env: `GOOGLE_PLAY_PACKAGE_NAME`, `GOOGLE_PLAY_PRODUCT_IDS`, `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`
- DB-Migration für Google-Abos (z. B. `012_google_subscriptions.sql`)

Produktion: Play-Console-Produkt für `com.bookmer.browser`, Service-Account, Migration, Env gesetzt.

## UI

Settings → Abo / PRO: Preis, Kauf, Restore, Sync-Fehlerzustände (`isLoadingProduct`, `isPurchasing`, `isRestoring`, `accountSyncFailed`, …).

## iOS-Parität

iOS nutzt StoreKit mit entsprechendem Jahres-PRO-SKU; Android hält den Account-PRO-Status über `/pay/google` am gemeinsamen Bookmer-User — kein isoliertes Android-only-Flag.

# Billing — Google Play PRO

## Product

| | |
|--|--|
| Play product id | `com.bookmer.browser.pro.yearly` |
| Type | Auto-renewable yearly subscription |
| Target price | €29 / year (store localizes display) |
| Client class | `data/BookmerProStore.kt` |

## Client flow

1. `BookmerProStore.start()` connects BillingClient, loads product details, refreshes purchases.
2. Purchase → acknowledge → keep purchase token.
3. If signed in → `BookmerApiClient.registerGoogleSubscription` → **`POST /pay/google`** with purchase token / package / product id.
4. Server verifies with Google Play Developer API and writes the same PRO period row used by Stripe / Apple.
5. `isPro` = Play entitlement **OR** `session.hasPro`.

## Backend (bookmer-platform — not in this repo)

Expected pieces (may already exist in platform):

- Route `POST /pay/google`
- Env: `GOOGLE_PLAY_PACKAGE_NAME`, `GOOGLE_PLAY_PRODUCT_IDS`, `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`
- DB migration for Google subscriptions (e.g. `012_google_subscriptions.sql`)

Production still requires: Play Console product configured for `com.bookmer.browser`, service account linked, migration applied, env set.

## UI

Settings → Subscription / PRO section: price, purchase, restore, account sync error states (`isLoadingProduct`, `isPurchasing`, `isRestoring`, `accountSyncFailed`, …).

## iOS parity

iOS uses StoreKit with an equivalent yearly PRO SKU; Android must keep account PRO state on the shared Bookmer user record via `/pay/google`, not a siloed Android-only flag.

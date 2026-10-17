# dieroboter Patches

Personal patch bundle for Morphe

## AI disclaimer

The patches in this repository were written by AI. Each patch was verified as working at the time of release. These patches are provided as-is, without a warranty of any kind.

---

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=dieroboter/morphe-patches

## Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.2.1](https://github.com/dieroboter/morphe-patches/releases/tag/v1.2.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;31 patches total
<details open>
<summary>Flappy Bird&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

**Supported versions:**

| 1.3 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Adapt to modern displays](#adapt-to-modern-displays) | Widens the game world from 512 to 640 units with the matching geometry adjustments, reworks the scene layout for the wider field, adds display-cutout and immersive window handling (fixes the black bar on tall screens), and moves the per-frame texture setup to a one-time init call (the port's flicker fix). |  |
| [Convert to 64-bit](#convert-to-64-bit) | Removes the bundled 32-bit native libraries (libandengine.so for armeabi, armeabi-v7a and x86). The engine's native loading is exception-guarded in the stock code and falls back to its Java implementation, so the game becomes ABI-neutral and installs on 64-bit-only devices. |  |
| [Modernize manifest and resources](#modernize-manifest-and-resources) | Makes the game activity the launcher and removes the splash screen and AdMob activity, drops the INTERNET and ACCESS_NETWORK_STATE permissions, raises the SDK levels to the port's min 14 / target 34 (current Android refuses to install the stock target 15), adds an adaptive launcher icon and display-cutout theme settings, removes the dead Google sign-in resources, and replaces the sound effects with the port's re-encoded versions. The app's version is left untouched. |  |
| [Remove ads and Google services hooks](#remove-ads-and-google-services-hooks) | Hides the AdMob banner and removes the ad request so no ad is ever loaded, disconnects the Google Play Games activity hook, and removes the port-removed tap-zone handler and scene trigger tied to the old services UI. |  |

</details>

<details open>
<summary>Geometry Dash Meltdown&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

**Supported versions:**

| 2.2.147 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Disable AppMetrica activation](#disable-appmetrica-activation) | Makes AppMetrica.activateReporter a no-op, so the AppMetrica reporter the Yandex mediation glue sets up is never activated and its events are dropped. |  |
| [Disable ad consent flow](#disable-ad-consent-flow) | Flips the AppLovin terms-and-privacy flow in LiteRobTopActivity.setupGAM from enabled to disabled, so the first-launch ad consent dialog never appears. |  |
| [Disable banner and interstitial ads](#disable-banner-and-interstitial-ads) | No-ops the native bridge methods that show banner and interstitial ads (LiteRobTopActivity._enableBanner / _showInterstitial) and makes _hasCachedInterstitial report false. Rewarded video ads are left working. |  |
| [Strip ad tracking permissions](#strip-ad-tracking-permissions) | Removes the ad identifier permissions (AD_ID and the Ad Services permissions: ad ID, attribution and topics) from the manifest. |  |

</details>

<details open>
<summary>Geometry Dash SubZero&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

**Supported versions:**

| 2.2.147 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Disable AppMetrica activation](#disable-appmetrica-activation) | Makes AppMetrica.activateReporter a no-op, so the AppMetrica reporter the Yandex mediation glue sets up is never activated and its events are dropped. |  |
| [Disable ad consent flow](#disable-ad-consent-flow) | Flips the AppLovin terms-and-privacy flow in LiteRobTopActivity.setupGAM from enabled to disabled, so the first-launch ad consent dialog never appears. |  |
| [Disable banner and interstitial ads](#disable-banner-and-interstitial-ads) | No-ops the native bridge methods that show banner and interstitial ads (LiteRobTopActivity._enableBanner / _showInterstitial) and makes _hasCachedInterstitial report false. Rewarded video ads are left working. |  |
| [Strip ad tracking permissions](#strip-ad-tracking-permissions) | Removes the ad identifier permissions (AD_ID and the Ad Services permissions: ad ID, attribution and topics) from the manifest. |  |

</details>

<details open>
<summary>Geometry Dash World&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

**Supported versions:**

| 2.2.147 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Disable AppMetrica activation](#disable-appmetrica-activation) | Makes AppMetrica.activateReporter a no-op, so the AppMetrica reporter the Yandex mediation glue sets up is never activated and its events are dropped. |  |
| [Disable ad consent flow](#disable-ad-consent-flow) | Flips the AppLovin terms-and-privacy flow in LiteRobTopActivity.setupGAM from enabled to disabled, so the first-launch ad consent dialog never appears. |  |
| [Disable banner and interstitial ads](#disable-banner-and-interstitial-ads) | No-ops the native bridge methods that show banner and interstitial ads (LiteRobTopActivity._enableBanner / _showInterstitial) and makes _hasCachedInterstitial report false. Rewarded video ads are left working. |  |
| [Strip ad tracking permissions](#strip-ad-tracking-permissions) | Removes the ad identifier permissions (AD_ID and the Ad Services permissions: ad ID, attribution and topics) from the manifest. |  |

</details>

<details open>
<summary>GIF Maker&nbsp;&nbsp;•&nbsp;&nbsp;7 patches</summary>
<br>

**Supported versions:**

| 2.0.8 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Disable AppMetrica activation](#disable-appmetrica-activation) | Makes AppMetrica.activateReporter a no-op, so the AppMetrica reporter the Yandex mediation glue sets up is never activated and its events are dropped. |  |
| [Disable Singular telemetry](#disable-singular-telemetry) | Prevents the Singular analytics SDK from initializing: its only init call site (the PremiumHelper Singular manager coroutine) returns immediately. |  |
| [Disable Toto backend](#disable-toto-backend) | Disables the Toto server-driven config/offers backend: its app-start initialization, config reporting (including the background worker), FCM promotional-push token registration and the worker scheduling all become no-ops. |  |
| [Disable ad consent flow](#disable-ad-consent-flow) | Disables the Google UMP ad-consent flow: the SDK's consent request returns immediately, so no consent information is requested and no consent form is ever loaded or shown. |  |
| [Disable rate prompts](#disable-rate-prompts) | Disables the in-app rate-us prompts (the SDK's rate-dialog source gate always returns false) and hides the 'Rate us' row in the settings screen. |  |
| [Strip ad tracking permissions](#strip-ad-tracking-permissions) | Removes the ad identifier permissions (AD_ID and the Ad Services permissions: ad ID, attribution and topics) from the manifest. |  |
| [Unlock premium](#unlock-premium) | Unlocks premium: forces the PremiumHelper SDK's isPremium check to always return true, which also suppresses banner/interstitial ads and the paywall and relaunch offers through the SDK's own premium paths. |  |

</details>

<details open>
<summary>WiFi FTP Server&nbsp;&nbsp;•&nbsp;&nbsp;8 patches</summary>
<br>

**Supported versions:**

| 2.4.0 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Disable Crashlytics reporting](#disable-crashlytics-reporting) | Disables Firebase Crashlytics data collection and empties the app's Crashlytics log/exception calls, which otherwise report the server URL and configuration on every start and stop. |  |
| [Disable ad consent flow](#disable-ad-consent-flow) | Disables the Google UMP consent flow that gates ad loading. The consent info update never runs and ads are treated as always allowed to load. Has no visible effect if 'Remove banner ads' is also enabled. |  |
| [Disable mDNS service advertisement](#disable-mdns-service-advertisement) | Stops the server from announcing itself as 'WiFiFTP' over mDNS/NSD (_ftp._tcp) on the local network. Direct connections by IP address are unaffected. |  |
| [Hide Get Pro version button](#hide-get-pro-version-button) | Hides the 'Get Pro version' button on the main screen, which only opens the paid Pro app's store listing. |  |
| [Material You icon](#material-you-icon) | Replaces the legacy launcher icon with a Material You adaptive icon: white folder and WiFi glyph on the app's blue, with a monochrome layer for themed icons. |  |
| [Remove Rate this app menu item](#remove-rate-this-app-menu-item) | Removes the 'Rate this app' item, which opens the app's store listing, from the main screen menu. |  |
| [Remove banner ads](#remove-banner-ads) | Removes the AdMob banner ad from the main screen. The ads SDK is never initialized and the banner view is never created or loaded. |  |
| [Strip ad tracking permissions and app ID](#strip-ad-tracking-permissions-and-app-id) | Removes the AdMob application ID, the MobileAds initialization provider and the ad identifier permissions (AD_ID and Ad Services) from the manifest. Requires 'Remove banner ads'. |  |

</details>

<!-- PATCHES_END -->

### Building locally

- Run `./gradlew buildAndroid`
- The built patches .mpp file is found in `patches/build/libs/patches-*.mpp`
- Patch the mpp file using [Morphe-Desktop](https://github.com/MorpheApp/morphe-desktop)
  like any other patch bundle.

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more information.

## License

dieroboter Patches are licensed under the [GNU General Public License v3.0](LICENSE)

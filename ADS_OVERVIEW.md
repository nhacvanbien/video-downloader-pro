# Bản đồ hiển thị Ads trong app

> Tài liệu này mô tả toàn bộ nơi app hiển thị/cấu hình quảng cáo (AdMob + Facebook Audience Network mediation, qua thư viện `io.mmonetize:ads-ui-view`). **Không bao gồm** tính năng Ad-Blocker (`AdHostDao`, `AdBlockerHelper`, `adblockserverlist3`) — đó là chức năng chặn quảng cáo khi browse web, không liên quan tới monetization ads.
>
> Ghi chú quan trọng: mọi class trong `com.ads.admob.*` (`TevoAdmobFactory`, các Helper...) chỉ là **lớp facade/API rỗng** — logic load/hiển thị ad thật nằm trong thư viện binary `io.mmonetize:ads-ui-view` (không có source ở đây). Các file trong package này chỉ định nghĩa API mà code app gọi vào.

## 1. Hạ tầng / SDK ads (`app/src/main/java/com/ads/admob/**`)

| File | Vai trò |
|---|---|
| `admob/TevoAdmobFactory.kt` | Facade tĩnh cho luồng AdMob: `initAdmob(context, config)`, `requestNativeAd(...)`, `populateNativeAdView(...)`. Dùng trực tiếp (không qua helper) ở `BrowserFragment` và `VideoDownloaderApplication`. |
| `cmp/TevoConsentManager.kt` | Singleton quản lý consent (GDPR/CMP). `getInstance(context)` + `initReleaseConsent(onConsentResponse)`. Hiện chỉ callback `onResponse(null)` ngay (stub, chưa có UMP/CMP thật trong source). Dùng ở `SplashActivity`. |
| `cmp/interfaces/OnConsentResponse.kt` | Interface callback: `onResponse(errorMessage)`, `onPolicyRequired(isRequired)`. |
| `config/TevoAdsConfig.kt` | Config holders: `NetworkProvider` (enum, có `ADMOB`), `EventConfig` (tỷ giá/currency để tính revenue), `TevoAdjustConfig` (token Adjust attribution + flag production), `TevoAdsConfig.Builder` (khoảng cách interstitial, mediation provider, test devices...). Được build 1 lần trong `VideoDownloaderApplication.initTevoAdLib()`. |
| `data/ContentAd.kt` | Data class đại diện chung cho 1 ad đã load (`open class ContentAd`), dùng trong các callback thay cho type AdMob thô. |
| `helper/adnative/NativeAdHelper.kt` + `params/NativeAdParam.kt` | Helper load **Native Ad**. `NativeAdConfig(idAds, canShowAds, canReloadAds, layoutId, adPlacement)`; expose `setNativeContentView()`, `setShimmerLayoutView()`, `requestAds(...)`. API chính được dùng ở gần như mọi màn hình có native ad. |
| `helper/appoppen/AppResumeAdHelper.kt` | Helper **App Open/Resume Ad**. `AppResumeAdConfig(idAds, listClassInValid, canShowAds, adPlacement)`; `setEnableAppResumeOnScreen()` / `setDisableAppResumeOnScreen()` để bật/tắt. Khởi tạo 1 lần trong `VideoDownloaderApplication`. |
| `helper/banner/BannerAdHelper.kt` + `params/BannerAdParam.kt` | Helper **Banner Ad**. `BannerAdConfig(idAds, canShowAds, canReloadAds, adPlacement)`; `setBannerContentView()`, `requestAds(...)`, `registerAdListener()`. |
| `helper/interstitial/InterstitialHelpers.kt` | 2 class: `InterstitialAdSplashHelper` (interstitial riêng cho splash, có `timeDelay`, `timeOut`, `showReady`, id ad ưu tiên) và `InterstitialAdsHelper` (generic, singleton theo placement qua `getInstance(placement)`, `setInterstitialAdConfig()`, `requestInterAds()`, `forceShowInterstitial()`). |
| `helper/interstitial/params/InterstitialAdParam.kt` | Sealed param `Request` / `Show(ad)`. |
| `helper/interstitial/test/InterstitialAdsHelper.kt` | `typealias` re-export của `InterstitialAdsHelper` gốc — được `InterAdsManager.kt` dùng (có vẻ là tên legacy). |
| `listener/AdListeners.kt` | Các interface callback: `BannerAdCallBack`, `NativeAdCallback`, `InterstitialAdCallback`, `InterstitialAdRequestCallBack`, `InterstitialAdShowCallBack` — method đều có default body rỗng. |

### `util/AdsConstant.kt` — cấu hình/toggle ads (driven bởi Firebase Remote Config, set ở `SplashActivity`)
- Cờ bật/tắt hiển thị: `showInterSplash`, `showBannerSplash`, `showNativeLanguage1_1`, `showNativeLanguage1_2`, `showNativeOnboardFullscreen1_1`, `showNativeOnboardFullscreen1_2`, `showNativePermission`, `showNativeHome`, `showBannerAll`, `showNativeSmallAll`, `showOpenResume`, `showInterAll`.
- Cờ ưu tiên mediation "High": `useNativeLanguage11High`, `useNativeLanguage12High`, `useNativeOnboardFullscreen11High`, `useNativeOnboardFullscreen12High`, `useInterSplashHigh`.
- Timing: `timeApplyLfo` (4s), `timeLoadingOnboard` (2s), `interIntervalTime` (tối thiểu 20s giữa 2 lần hiện interstitial).
- Cache ad đã preload (LiveData): `nativeAdsLanguage1_1`, `nativeAdsLanguage1_2`, `nativeAdsOnboardFullscreen1_1`, `nativeAdsOnboardFullscreen1_2`, `nativeAdsPermission`, `nativeAdsSmallAll`.
- `newUILfo`: cờ bật UI onboarding thay thế.
- Hàm preload: `requestNativeLFO1/2()`, `requestNativeFullscreen1/2()`, `requestNativePermission()`, `requestNativeAdsSmallAll()` — mỗi hàm có chiến lược waterfall qua `requestNativeAlternate()`: thử unit "High" trước, fallback unit thường nếu fail.

### `util/InterAdsManager.kt` — điều phối Interstitial trung tâm (placement `inter_all`)
- `configInterAds(context)`: đăng ký config (`BuildConfig.INTER_ALL`, gate bởi `AdsConstant.showInterAll`).
- `requestInter(context, adPlacement)`: preload.
- `showInterAll(activity, onAction)`: **hàm gate chính của toàn app** — nếu offline, hoặc màn `NativeFullActivity` chưa từng hiện + chưa đủ interval, sẽ điều hướng qua `NativeFullActivity` (native ad fullscreen thay thế) trước khi tiếp tục; ngược lại force-show interstitial thật rồi gọi `onAction()`.
- `CheckTimeShowAdsInter`: theo dõi `lastShow`, áp interval tối thiểu (`AdsConstant.interIntervalTime`, mặc định 20s).
- Extension dùng khắp UI: `FragmentActivity.showInterAll{}`, `Fragment.showInterAll{}`, `View.setOnClickListenerWithShowInterAd(activity){}` — bọc các hành động điều hướng bằng 1 lớp gate interstitial.

## 2. Ad unit ID / build config (`app/build.gradle.kts`)

Cả 2 flavor `develop` và `production` (dimension `"default"`) định nghĩa **cùng bộ `buildConfigField`**; `develop` dùng test ad unit ID của Google (`ca-app-pub-3940256099942544/...`), `production` dùng ID thật (`ca-app-pub-1249320623511529/...`):

- `INTER_SPLASH`, `BANNER_SPLASH`
- `NATIVE_LANGUAGE_1_1`, `NATIVE_LANGUAGE_1_2`
- `NATIVE_ONBOARD_FULLSCREEN_1_1`, `NATIVE_ONBOARD_FULLSCREEN_1_2`
- `NATIVE_PERMISSION`, `NATIVE_HOME`
- `BANNER_ALL`, `NATIVE_SMALL_ALL`
- `OPEN_RESUME`
- `INTER_SPLASH_HIGH`
- `NATIVE_LANGUAGE_1_1_HIGH`, `NATIVE_LANGUAGE_1_2_HIGH`
- `NATIVE_ONBOARD_FULLSCREEN_1_1_HIGH`, `NATIVE_ONBOARD_FULLSCREEN_1_2_HIGH`
- `INTER_ALL`, `NATIVE_FULL_ALL`

Theo flavor còn có: `facebook_app_id`, `facebook_client_token`, `Minimum_Fetch` (interval fetch Remote Config tối thiểu — 5s dev / 3600s prod).

`buildTypes` (`debug`/`release`) đều set `manifestPlaceholders`: `app_id` (AdMob App ID `ca-app-pub-1249320623511529~7642263518`), `facebook_app_id`, `facebook_client_token` — **giống nhau ở cả debug/release** (chỉ khác theo flavor, không khác theo build type).

Dependency liên quan: `io.mmonetize:ads-ui-view:0.0.26-rc04` (`gradle/libs.versions.toml`), `facebook.ads` + `facebook.sdk` (mediation adapter Facebook Audience Network), `facebook.shimmer` (view shimmer loading cho native ad).

## 3. Vị trí hiển thị theo từng màn hình

| Màn hình (file) | Loại ad | Khi nào / hiển thị ở đâu |
|---|---|---|
| `ui/splash/SplashActivity.kt` | **Interstitial** (`INTER_SPLASH`/`_HIGH`) + **Banner** (`BANNER_SPLASH`) | Sau khi consent (`TevoConsentManager`) resolve và fetch Remote Config xong: request song song interstitial splash (`InterstitialAdSplashHelper`, delay 5s, timeout 30s) và banner (`binding.frAdsBanner`). Đồng thời preload native ad cho màn Language kế tiếp (`AdsConstant.requestNativeLFO1/2`). Sau khi cả 2 request xong (và progress ≥90% nếu lần đầu mở app) thì show interstitial (nếu có) rồi điều hướng tiếp qua `startNextAct()`. Cũng gọi `InterAdsManager.configInterAds(this)` để prime interstitial `INTER_ALL` toàn app. |
| `ui/intro/IntroActivity.kt` | Chỉ trigger preload **Native** | Gọi `AdsConstant.requestNativePermission(this)` trong `initView()` để preload sẵn native ad cho màn Permission. |
| `ui/intro/IntroFragment.kt` (qua `IntroAdapter.kt`) | **Native** (fullscreen onboarding: `NATIVE_ONBOARD_FULLSCREEN_1_1`/`_1_2`) | `IntroAdapter` chèn trang `layout_intro_native_ad` ở vị trí 1 (nếu `showNativeOnboardFullscreen1_1`) và vị trí 3 (nếu `showNativeOnboardFullscreen1_2`) giữa 3 slide intro tĩnh. Mỗi trang show shimmer loading 2s rồi render ad đã preload từ `AdsConstant.nativeAdsOnboardFullscreen1_1/1_2` (hoặc request mới) vào `layout_native_on_boarding_full`. Chặn nút "Next" xuất hiện cho tới khi hết `timeLoadingOnboard`. |
| `ui/language/LanguageActivity.kt` | **Native** (`NATIVE_LANGUAGE_1_1` trong `layout_native_language`, `NATIVE_LANGUAGE_1_2` trong `layout_native_language_2`) | Nếu `fromSplash=true` (lần đầu mở app): show native ad #1 ngay (`setNativeAd1()`), sau khi chọn ngôn ngữ show tiếp native ad #2 (`setNativeAd2()`, chỉ load lần chọn đầu). Cũng preload native ad cho onboarding-fullscreen (`requestNativeFullscreen1/2`). Nếu vào từ Settings (không phải từ splash) thì chỉ show native ad #1. |
| `ui/language/LanguageAppliedActivity.kt` | Không có | Không có code ad — chỉ hiện xác nhận "đã áp dụng" rồi tự chuyển qua `IntroActivity` sau `AdsConstant.timeApplyLfo` giây (mặc định 4s). |
| `ui/guide/GuideActivity.kt` | Không (dead code) | Import `AdsConstant`/`NativeAd`/`NativeAdView` nhưng `loadNativeGuide()` đang bị comment — hiện không hiện ad nào ở đây. |
| `ui/permission/PermissionActivity.kt` | **Native** (`NATIVE_PERMISSION`, layout `layout_native_ad_small_bottom`) | Show ngay trong `initView()` qua `setNativePermission()`, dùng lại `AdsConstant.nativeAdsPermission` đã preload từ `IntroActivity`. Cũng tắt App Resume Ad (`setDisableAppResumeOnScreen()`) trước khi mở Settings hệ thống, và bật lại (`setEnableAppResumeOnScreen()`) ở `onResume()`. |
| `ui/tab/TabsActivity.kt` | Gate **Interstitial** (`INTER_ALL`) | Nhấn vào 1 tab có sẵn hoặc nút "add tab" đều bọc qua `showInterAll{...}`/`setOnClickListenerWithShowInterAd`. |
| `ui/nativefull/NativeFullActivity.kt` | **Native** fullscreen (`NATIVE_FULL_ALL`, layout `layout_native_on_boarding_full`) | Màn **thay thế cho interstitial**, được `InterAdsManager.showInterAll()` gọi lần đầu tiên (trước khi logic interval/one-shot kích hoạt) — chặn back, hiện nút "Next"/skip sau 4s, bấm sẽ gọi `InterAdsManager.onNativeFullActivityFinished()` rồi `finish()` để tiếp tục hành động đang chờ. |
| `ui/processing/ProcessingFragment.kt` | Chỉ gate **Interstitial** (không có native ad ở file này) | Bấm icon "Guide" (`binding.imgGuide`) bọc qua `setOnClickListenerWithShowInterAd` trước khi mở `GuideActivity`. Cũng toggle App Resume Ad ở `onResume()`. `ProcessAdapter` (list tiến trình download) **không có code ad**. |
| `ui/pin/PinActivity.kt` | Không (dead code) | Import `AdsConstant` nhưng không gọi — không hiện ad ở màn nhập PIN. |
| `ui/pin/SecurityActivity.kt` | Không (dead code) | Import ad classes, toggle visibility `binding.frAds` quanh dialog nhưng chưa từng request/load ad thật — container còn sót lại, chưa wire. |
| `dialog/DialogExitApp.kt` | Không (dead code) | Cũng import ad class nhưng không request — chỉ là dialog xác nhận Yes/No khi thoát app (gọi từ `MainActivity.onBackPressed()`). |
| `MainActivity.kt` | **Banner** (`BANNER_ALL`) + preload **Interstitial** (`INTER_ALL`) | `loadAd()` (gọi từ `initView()`) show banner vào `binding.frBanner` (ẩn lại sau khi có impression — hành vi one-shot khá lạ) và pre-request `INTER_ALL` qua `InterAdsManager.requestInter()`. `onBackPressed()` được override để show `DialogExitApp` (post event EventBus `"hide_ads"`/`"show_ads"` quanh dialog) thay vì hành vi interstitial-on-back của base class. |
| `base/BaseActivity.kt` | Gate **Interstitial** (mặc định toàn app) | `onBackPressed()` override gọi `showInterAll { super.onBackPressed() }` — **mọi** Activity kế thừa `BaseActivity` sẽ show interstitial khi bấm back, trừ khi tự override (như `MainActivity`). |
| `ui/browser/BrowserFragment.kt` (tab Home) | **Native** (`NATIVE_HOME`, layout `layout_native_ad_small_bottom`) + gate **Interstitial** | `setNativeAd()` gọi trực tiếp `TevoAdmobFactory.requestNativeAd`/`populateNativeAdView` (không qua `NativeAdHelper`) vào `binding.frAdsNative`. Gate interstitial bọc: bấm Bookmarks (`layoutBookmark`), History (`layoutHistory`), submit URL bar (IME done), icon search (`imgSearch`). |
| `ui/browser/webTab/WebTabActivity.kt` | **Banner** (`BANNER_ALL`) + gate **Interstitial** | `loadAd()` show banner vào `binding.frBanner` (gọi từ `initView()`); bấm back khi mở dạng "tab" (`intent.getStringExtra("open")=="tab"`) bọc qua `showInterAll` trước khi quay về `MainActivity`. |
| `ui/browser/webTab/VideoInfoAdapter.kt` (bottom sheet chọn định dạng, mở từ `ProcessingFragment`) | **Native** (`NATIVE_SMALL_ALL`, layout `layout_native_ad_small_bottom`) | Chỉ show ở **item đầu tiên** (`bindingAdapterPosition == 0`) của RecyclerView danh sách video detect được (`showNativeAd()` vào `binding.frAds`); các item khác clear container. |
| `ui/downloaded/DownloadedFragment.kt` | **Native** (`NATIVE_SMALL_ALL`) + gate **Interstitial** | Implement `VideoListener.onLoadNativeAd()` cho ad row do `VideoAdapter` chèn; `onItemClicked()` (play video đã tải) bọc `startVideo()` qua `showInterAll`. |
| `ui/downloaded/VideoAdapter.kt` (grid/list downloads) | **Native** (item chèn vào list, `item_native_download.xml`) | Chèn 1 row `VIEW_TYPE_NATIVE_AD` ở vị trí cố định `NATIVE_AD_POSITION = 1` (item thứ 2) trong RecyclerView khi list không rỗng; việc load ad thật giao lại cho `videoListener.onLoadNativeAd(...)` (implement ở `DownloadedFragment`, `PrivateVideoActivity`, `SelectVideoActivity`). |
| `ui/privateVideo/PrivateVideoActivity.kt` | **Native** (`NATIVE_SMALL_ALL`, placement `native_private_folder`) + gate **Interstitial** | `showNativeAd()` vào `binding.frAds`; implement `onLoadNativeAd()` cho `VideoAdapter`; `onItemClicked()` (mở video private) bọc `startVideo()` qua `showInterAll`. |
| `ui/privateVideo/SelectVideoActivity.kt` | **Native** (item chèn qua `VideoAdapter`/`onLoadNativeAd`) + gate **Interstitial** | Implement `onLoadNativeAd()`; toggle visibility `frAds` quanh luồng share/select; `onItemClicked()` bọc playback qua `showInterAll`. |
| `ui/history/HistoryActivity.kt` | **Native** (`NATIVE_SMALL_ALL`, placement `native_history`) + gate **Interstitial** | `showNativeAd()` vào `binding.frAds` (gọi khi init); import `showInterAll` cho các hành động điều hướng từ màn này. |
| `ui/media/PlayMediaActivity.kt` | **Native** (`NATIVE_SMALL_ALL`, placement `native_video_detail`) | Chỉ show khi media là **video và không ở chế độ fill-screen** (`isFill == false`) — `showNativeAd()` vào `binding.frAds`; ẩn hoàn toàn khi fill-screen/landscape. |
| `ui/disclaimers/DisclaimersActivity.kt` | Không | Không có code ad. |

### Mapping layout native ad

| Layout | Dùng ở |
|---|---|
| `layout_native_on_boarding_full.xml` | `IntroFragment` (slide onboarding), `NativeFullActivity` (màn native fullscreen thay interstitial) |
| `layout_intro_native_ad.xml` | Container trang trong `IntroAdapter`/`IntroFragment` (chứa `fr_ads`, `shimmer_full`, `text_next`, `viewLoading`), bên trong chứa `layout_native_on_boarding_full` |
| `layout_native_language.xml` | `LanguageActivity` native ad #1 (`NATIVE_LANGUAGE_1_1`) |
| `layout_native_language_2.xml` | `LanguageActivity` native ad #2 (`NATIVE_LANGUAGE_1_2`) |
| `layout_native_ad_small_bottom.xml` | Template native ad "nhỏ" dùng chung ở: `PermissionActivity`, `BrowserFragment` (Home), `VideoInfoAdapter`, `DownloadedFragment`, `HistoryActivity`, `PrivateVideoActivity`, `PlayMediaActivity` |
| `item_native_download.xml` | Row native ad chèn vào RecyclerView của `VideoAdapter` (list Downloaded, Private Video, Select Video) |
| `layout_native_ad_full.xml`, `layout_native_ad_medium_top.xml`, `layout_native_ad_small_top.xml`, `layout_native_no_media_top.xml`, `layout_native_no_media_bottom.xml`, `layout_native_no_media_shimmer.xml` | **Không thấy được reference ở đâu trong Kotlin source** — có vẻ là layout native ad cũ/không dùng nữa. |

## 4. Khởi tạo ở tầng Application (`VideoDownloaderApplication.kt`)

- `onCreate()` gọi `initTevoAdLib()` (sau khi setup Firebase/WorkManager/YoutubeDL).
- `initTevoAdLib()`:
  - Build `TevoAdjustConfig.Build("tcv1br2lo1s0", true).build()` (token Adjust attribution + flag production).
  - Build `TevoAdsConfig.Builder(mexaAdjustConfig = tevoAdjustConfig)` với `.intervalBetweenInterstitial(15000L)`, `.buildVariantProduce(false)`, `.mediationProvider(NetworkProvider.ADMOB)`, `.eventConfig(EventConfig(exchangeRate = 26000, exchangeCurrency = "VND"))`, `.listTestDevices(...)`.
  - Gọi `TevoAdmobFactory.initAdmob(this, tevoAdsConfig)` — điểm init SDK AdMob/mmonetize duy nhất toàn app (no-op trong source hiện thấy, logic thật nằm trong AAR mmonetize).
- **App Open/Resume Ad**: `appResumeAdHelper` (lazy property trên Application, truy cập qua `VideoDownloaderApplication.instance.appResumeAdHelper`) build bởi `initAppOpenAd()`:
  - Dùng ad unit `BuildConfig.OPEN_RESUME`, gate bởi `AdsConstant.showOpenResume`.
  - `listClassInValid` (màn KHÔNG được show resume ad): `AdActivity` (activity ad của AdMob), `SplashActivity`, `WebTabActivity`.
  - Bind theo lifecycle `ProcessLifecycleOwner.get()` (toàn app foreground/background, không phải 1 Activity) — đây là ad "hiện khi app resume/foreground".
  - Được enable/disable tại vài điểm cụ thể: `PermissionActivity` và `ProcessingFragment` tắt trước khi mở Settings hệ thống (tránh resume ad hiện chèn lên màn Settings), bật lại ở `onResume()`.

## 5. AndroidManifest.xml

`app/src/main/AndroidManifest.xml` (trong `<application>`):
```xml
<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value="${app_id}" />
<meta-data android:name="com.facebook.sdk.ApplicationId" android:value="${facebook_app_id}" />
<meta-data android:name="com.facebook.sdk.ClientToken" android:value="${facebook_client_token}" />
```
3 placeholder này lấy từ `manifestPlaceholders` trong `app/build.gradle.kts`:
- `app_id` → AdMob App ID: `ca-app-pub-1249320623511529~7642263518` (giống nhau ở cả `debug`/`release`, không tách theo build type như ad unit ID).
- `facebook_app_id` → `1303924174289230`.
- `facebook_client_token` → `ea374c985a08b0c8c400bcd17c54f4d4`.

Có 1 `<property android:name="android.adservices.AD_SERVICES_CONFIG" .../>` đang bị comment, tham chiếu `@xml/gma_ad_services_config` — dùng cho Privacy Sandbox/Attribution Reporting API của AdMob nếu bật lại sau này.

## 6. Vài điểm cần lưu ý khi refactor/dọn ads

1. `BaseActivity.onBackPressed()` show interstitial (`showInterAll`) cho **mọi** màn hình mặc định — `MainActivity` là ngoại lệ duy nhất (show dialog xác nhận thoát thay vì ad).
2. `InterAdsManager.showInterAll()` route qua `NativeFullActivity` (màn native ad fullscreen, chặn back) ở lần trigger đầu tiên / khi chưa đủ interval, chỉ show interstitial "thật" sau đó — thực chất có 2 kiểu trải nghiệm "giống interstitial" khác nhau đằng sau 1 API.
3. Các file sau import class ad nhưng **không thực sự request/hiện ad** (dead code, có thể dọn khi cleanup): `PinActivity`, `SecurityActivity`, `DialogExitApp`, `GuideActivity`.
4. Ad unit `NATIVE_SMALL_ALL` được tái sử dụng ở nhiều màn "danh sách nội dung" (Downloaded, Private Video, Select Video, History, Video Detail/PlayMedia, bottom sheet video-info) với `adPlacement` khác nhau để phân tách khi đo revenue/analytics, nhưng dùng chung layout `layout_native_ad_small_bottom`.
5. 6 layout native ad trong `res/layout/` (`layout_native_ad_full.xml`, `layout_native_ad_medium_top.xml`, `layout_native_ad_small_top.xml`, `layout_native_no_media_top.xml`, `layout_native_no_media_bottom.xml`, `layout_native_no_media_shimmer.xml`) hiện không được reference ở đâu trong code Kotlin — có thể là layout cũ còn sót lại.

---

## Phụ lục: gỡ plugin `com.tevo.android.application` (2026-08-06)

Lỗi build gốc: `Plugin [id: 'com.tevo.android.application', version: '0.0.5', apply: false] was not found in any of the following sources`.

Nguyên nhân: plugin này được publish riêng lên GitHub Packages (`https://maven.pkg.github.com/tevo-studio/code-android-gradle-plugin`), yêu cầu credentials (`congvc-dev` + token) khai báo cứng trong `settings.gradle.kts` — token này không còn truy cập được / hết quyền.

Đã xoá hoàn toàn khỏi project (không tìm thấy block cấu hình DSL nào của plugin này ngoài việc `apply` nó — plugin chỉ được apply, không có extension block riêng nào bị dùng trong `app/build.gradle.kts`):
- `build.gradle.kts`: bỏ `alias(libs.plugins.tevo.android.application) apply false`.
- `app/build.gradle.kts`: bỏ `alias(libs.plugins.tevo.android.application)`.
- `gradle/libs.versions.toml`: bỏ entry `tevo-android-application = { id = "com.tevo.android.application", version = "0.0.5" }`.
- `settings.gradle.kts`: bỏ luôn maven repo `tevo-studio/code-android-gradle-plugin` trong `pluginManagement` (repo này chỉ tồn tại để serve plugin đó, không phục vụ dependency nào khác).

### Cập nhật tiếp theo: gỡ luôn dependency `io.mmonetize:ads-ui-view` (401 Unauthorized)

Sau khi gỡ plugin, build tiếp tục fail ở bước `:app:dataBindingMergeDependencyArtifactsDevelopDebug` vì **401 Unauthorized** khi resolve `io.mmonetize:ads-ui-view:0.0.26-rc04` từ repo `tevo-studio/mMonetize` — cùng token `congvc-dev` đã hết hạn/bị revoke (không chỉ riêng plugin, token này chết cho mọi repo GitHub Packages private của Tevo).

Kiểm tra cho thấy **không có file source nào import `io.mmonetize.*` trực tiếp** — toàn bộ code ads trong app chỉ gọi vào các class facade nội bộ ở `com.ads.admob.*` (xem mục 1), và các facade này hiện là **stub rỗng** (không gọi API mmonetize nào trong source thấy được). Nhiều khả năng logic thật trước đây được **inject vào lúc build bởi chính plugin `com.tevo.android.application` đã gỡ ở trên** (bytecode transform/ASM lấy implementation từ AAR mmonetize rồi ghép vào các method rỗng này) — nghĩa là ngay cả khi build qua được với plugin cũ, nếu token hết hạn thì mmonetize cũng không tải được và ads coi như đã không hoạt động từ trước.

Theo yêu cầu, đã xoá hẳn dependency này (không ảnh hưởng compile phần còn lại vì không ai import trực tiếp):
- `app/build.gradle.kts`: bỏ `implementation(libs.mmonetize.ads.ui.view)`.
- `gradle/libs.versions.toml`: bỏ entry `mmonetize-ads-ui-view`.
- `settings.gradle.kts`: bỏ block `exclusiveContent { ... GitHubPackages ... tevo-studio/mMonetize ... }`.

**Hệ quả**: toàn bộ code trong `com.ads.admob.*` và các helper (`NativeAdHelper`, `BannerAdHelper`, `InterstitialAdsHelper`, `AppResumeAdHelper`, `TevoAdmobFactory`...) vẫn còn trong source và vẫn compile được (vì là stub thuần Kotlin), nhưng **không còn base library nào implement logic AdMob/mediation thật phía sau** — nếu muốn có ads thật trở lại, cần thay thế bằng SDK khác (AdMob SDK chính chủ + Facebook Audience Network mediation adapter đã có sẵn trong `app/build.gradle.kts`) và tự viết lại phần implementation trong các helper này, dùng `ADS_OVERVIEW.md` này làm bản đồ vị trí cần khôi phục.

**Không đụng tới** repo `tevoteam/tevo-ads` (group `com.tevo`) trong `settings.gradle.kts` — repo này hiện không có dependency nào request tới, nên tồn tại vô hại (không gây lỗi build) dù dùng cùng token đã hết hạn.

⚠️ **Lưu ý bảo mật**: `settings.gradle.kts` đang chứa credentials dạng plaintext (username `congvc-dev` + GitHub token) cho repo `tevo-ads` còn lại. Token này đã bị commit vào git history và đã chết (401) — nên revoke hẳn trên GitHub và xoá luôn block repo này nếu chắc chắn không cần dùng lại.

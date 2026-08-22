# CLAUDE.md

Đọc file này **trước khi** thêm feature mới hoặc sửa code trong repo này. Nó ghi lại kiến trúc hiện tại và các bẫy đã từng dính khi migrate — bỏ qua sẽ lặp lại lỗi cũ.

## Tổng quan

Android app tải video (`com.videodownloader.videoplayer.videosaver.download.video`), package Kotlin gốc `com.smarttool.videodownloader`. App đã hoàn tất migrate sang **Clean Architecture + Koin + Jetpack Compose**, không còn XML/View layer, không còn Hilt/Dagger, không còn RxJava. Chỉ còn **2 Activity**: `SplashActivity` và `MainActivity` (single-Activity, điều hướng bằng Navigation-Compose).

## Cấu trúc thư mục

```
app/src/main/java/com/smarttool/videodownloader/
├── core/            # code dùng chung toàn app
│   ├── di/          # Koin modules (AppModule, LegacyViewModelModule)
│   ├── datastore/   # AppPreferencesDataSource + AppPreferenceKeys (thay SharedPreferences)
│   ├── navigation/  # AppNavHost, AppRoute
│   ├── ui/          # theme, components dùng chung (RetainedAndroidView, MediaThumbnail, dialogs...)
│   ├── network/, file/, ads/, notification/, scheduler/, permission/, browser/, coroutines/, logging/
├── data/            # data layer dùng chung (RoomConverter, data/downloader/ = downloader engine)
└── feature/<name>/{domain,data,presentation,di}/   # mỗi feature 1 package riêng
```

Các feature hiện có: `browser`, `disclaimers`, `downloads`, `guide`, `history`, `intro`, `language`, `library`, `main`, `media`, `nativefull`, `onboarding`, `permission`, `pin`, `settings`, `splash`, `tab`.

**`history` là reference implementation chuẩn nhất** — khi không chắc pattern cho feature mới, copy theo shape của nó.

## Quy tắc bắt buộc

### 1. One class per file
Mỗi class/interface/enum/data class **một file riêng**, tên file trùng tên type.
- `domain/` — 1 file/repository interface, 1 file/use case, 1 file/model.
- `data/` — 1 file/repository impl, mapper, hoặc data source.
- `presentation/` — ViewModel, UiState, mỗi composable màn hình — file riêng.
- **Không bao giờ** gộp interface + nhiều use case vào 1 file `XxxRepository.kt`.

### 2. Preferences → DataStore
`PreferenceHelper` (SharedPreferences) đã bị xoá hoàn toàn. Pattern chuẩn:
`domain/XRepository` (suspend/Flow) → `data/XRepositoryImpl` (wrap `AppPreferencesDataSource`) → `domain/usecase/GetX/SetXUseCase` → ViewModel gọi use case trong `viewModelScope.launch`.
Không có migration dữ liệu cũ từ `app_prefs` — cố ý, app được rebuild sạch.
**Ngoại lệ:** code không phải suspend context / không phải "màn hình" (Worker, `CustomProxyController`, `YoutubedlHelper`, `VideoDownloaderApplication.onCreate`) inject thẳng `AppPreferencesDataSource` và dùng accessor `…Blocking`.

### 3. Koin DI
- Mỗi feature có `di/` module riêng, đăng ký trong Koin.
- Singleton dùng chung khai báo ở `core/di/AppModule.kt`.
- **KHÔNG** khai báo `single<Context> { androidContext() }` — `startKoin { androidContext(...) }` đã đăng ký Context/Application, redeclare gây infinite recursion, app crash lúc khởi động.

### 4. Downloader engine — chỉ qua 1 cửa
`feature/downloads/domain/DownloaderGateway` là boundary duy nhất lên `data/downloader/`. `AndroidDownloaderGateway` là nơi DUY NHẤT chọn giữa `CustomRegularDownloader` và `YoutubeDlDownloader`. Không gọi thẳng các engine này từ UI/ViewModel.

### 5. Giữ instance View qua điều hướng Compose (BẮT BUỘC, đừng phá)
App vẫn giữ vài View thật (WebView, PlayerView, banner ad) sống xuyên suốt navigation vì composable bị dispose khi màn khác đè lên:
- `core/ui/components/RetainedAndroidView.kt` — dùng cho mọi View cần giữ sống; `AndroidView(factory = { view })` trần sẽ **crash lần compose thứ 2** ("child already has a parent").
- `feature/browser/presentation/WebTabViewHost.kt` và `feature/downloads/presentation/ProcessingWebViewHost.kt` do **Activity sở hữu**, không nằm trong composition. Rời màn phải gọi `release()` tường minh (BackHandler/onBack), KHÔNG suy ra từ composable bị dispose.
- `feature/media/presentation/MediaPlayerHolder.kt` giữ ExoPlayer, tạo bằng `remember(url)`, release trong `DisposableEffect`.
- `PlayerView` trong `MediaPlayerHolder` PHẢI inflate từ `res/layout/view_media_player.xml` (có `app:surface_type="texture_view"`), KHÔNG được gọi `PlayerView(context)` trần. Constructor trần không có `AttributeSet` nên rơi về mặc định `SURFACE_TYPE_SURFACE_VIEW`; `SurfaceView` compositing bởi SurfaceFlinger ở layer riêng, huỷ surface bất đồng bộ với việc View bị gỡ khỏi cây → video đè (ghost frame) lên màn hình vài giây sau khi back. `surface_type` chỉ đọc được từ XML attrs lúc construct, không có setter runtime.
- `MainActivity` cần `android:configChanges="orientation|screenSize|..."` trong manifest — nếu Activity bị recreate lúc xoay ngang sẽ mất WebView + back stack.

### 6. Compose Activity phải extend đúng base
Dùng `base/BaseComposeActivity`, không phải `AppCompatActivity` trần — base class áp flag immersive-fullscreen, thiếu sẽ bị status bar đè lên toolbar.

### 7. Dialog (`core/ui/dialogs/`)
Nếu tạo `Dialog(context)` rồi set `ComposeView` bên trong: PHẢI chụp lại `context` gốc thành property riêng (`private val hostContext = context`) TRƯỚC khi `Dialog` bọc nó trong `ContextThemeWrapper`, rồi dùng `hostContext` để set `ViewTree*Owner`. Bỏ qua bước này → `IllegalStateException: ViewTreeLifecycleOwner not found`, crash toàn app, không lộ ra lúc build/lint. Dùng helper có sẵn `Dialog.setComposeContent(context, content)`.

### 8. String resources — thêm/sửa string PHẢI đồng bộ tất cả locale
Repo có ~30 thư mục `values-<locale>/strings.xml` (`app/src/main/res/values-*`) ngoài `values/strings.xml` (mặc định, tiếng Anh). Khi thêm mới, sửa, hoặc xoá bất kỳ `<string>` nào trong `values/strings.xml`:
- Thêm: phải thêm bản dịch tương ứng vào **tất cả** `values-*/strings.xml` (trừ string có `translatable="false"` như `app_name`, `app_download_channel_id`). Dịch tự nhiên theo đúng ngôn ngữ/vùng của từng qualifier (`pt` khác `pt-rBR`, `zh` khác `zh-rTW`, `in` = Indonesian, `fil` = Filipino, `ms` = Malay...), không để trống hoặc copy nguyên tiếng Anh. Giữ nguyên placeholder (`%s`, `%1$s`...) và escape apostrophe (`\'`) đúng convention đã dùng trong file đó.
- Sửa nội dung tiếng Anh: cân nhắc cập nhật lại bản dịch liên quan nếu ý nghĩa thay đổi đáng kể.
- Xoá: xoá luôn ở tất cả locale để tránh string mồ côi.
Trước khi coi feature xong, chạy kiểm tra nhanh: so `grep -oE 'name="[^"]+"' values/strings.xml` với từng `values-*/strings.xml` bằng `comm` để tìm key thiếu — xem cách làm ở lịch sử sửa `string_queued`/`string_fetching_video_info`/rating block. Đừng để lặp lại tình trạng nhiều locale bị thiếu hàng chục key do quên bước này qua nhiều lần thêm string trước đó.

## Build

- Gradle **phải chạy bằng JDK 21**, không phải JDK trong `gradle.properties` (đang trỏ JBR 25, AGP 8.13 sẽ reject):
  ```
  ./gradlew <task> -Dorg.gradle.java.home=/Users/hoanglua/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home
  ```
- Có flavor `develop`/`production`, không có task `compileDebugKotlin` trần — dùng `:app:compileDevelopDebugKotlin` để check nhanh.
- **Không chạy `spotlessApply`** — formatting không được enforce trong repo này, chạy sẽ rewrite ~85 file không liên quan vào diff của bạn. `spotlessCheck` fail sẵn trên các file đó, không phải do bạn.

## Bẫy khi refactor bằng script (đã dính nhiều lần)

- Đừng thay FQN theo prefix (vd `WebTab` là substring của `WebTabActivity`).
- Đừng rename theo `\bTênLớp\b` toàn cây khi có lớp trùng tên ở namespace khác.
- Đừng regex xoá import hàng loạt.
- Trước khi `rm` bất kỳ file nào: `grep` cả `app/src/main/res/` — custom View có thể chỉ được XML tham chiếu, không hiện ra khi grep code Kotlin.
- Compile sau **mỗi bước**, đừng dồn nhiều thay đổi rồi mới compile.

## Bẫy trong download & WebView (đã dính, đã fix — đừng lặp lại)

- `FileUtil.moveMedia()` / `moveFileToDownloadsFolder()` trả về **path thật sự** sau khi move (kiểu `String?`, không phải `Boolean`). Trên Android Q+, khi lưu vào Downloads công khai, MediaStore dedup tên file khi trùng (`name(1)`, `name(2)`...) — path bạn tính sẵn trước khi move (`target`/`toUri`) có thể không khớp file thật sự được tạo. LUÔN dùng giá trị `moveMedia` trả về để set `VideoTaskItem.filePath`, đừng tự suy path từ tên gốc.
- `NotificationsHelper.createNotificationBuilder()` nhánh `VideoTaskState.SUCCESS` chỉ insert vào Room (`videoTaskItemRepository.insertVideoTaskItem`) nếu `File(task.filePath).exists()` đúng. Nếu `filePath` sai (xem trên) thì insert bị skip **âm thầm** — không log, không exception, không crash. Video tải xong (file có trên sdcard) nhưng không hiện ở tab Downloaded / không có row trong DB → kiểm tra chỗ này đầu tiên.
- Title video lấy từ `WebTabViewModel.currentTitle`, chỉ nên set qua event `TitleReceived` (bắn từ `WebChromeClient.onReceivedTitle`, wired ở cả `WebTabViewHost` và `ProcessingWebViewHost`). ĐỪNG đọc `view.title` trong `onPageStarted`/`shouldOverrideUrlLoading` rồi coi là title thật — tại thời điểm đó WebView trả về chính URL vì trang chưa parse xong `<title>`.

### 9. Comment trong code — viết ít, đúng chỗ
KHÔNG thêm comment chỉ để kể lại việc mình vừa sửa gì ("đổi X thành Y vì trước đó bị lỗi Z", "bare glyph vì icon cũ có sẵn vòng tròn"). Loại comment này mô tả *lịch sử chỉnh sửa*, chỗ của nó là commit message, không phải trong code — đọc lại sau vài tháng chỉ thấy nhiễu.

Chỉ comment khi giải thích một ràng buộc **không đọc ra được từ code** và người sửa sau dễ vô tình phá (ví dụ các bẫy đã liệt kê ở file này: thứ tự `moveMedia`, `surface_type` của PlayerView, `hostContext` của Dialog). Mặc định: không comment. Giữ đúng mật độ comment của code xung quanh.

## Ghi chú khác

- Package ads `com.ads.admob` là **STUB** trong repo này (không có SDK thật) — không thể verify hành vi ads trên máy, chỉ verify được đường dây gọi (`InterAdsManager`, `setDisableAppResumeOnScreen`/`setEnableAppResumeOnScreen`).
- Bridge View-based ad SDK vào Compose qua `core/ui/components/NativeAdContainer.kt`; luôn compose container kể cả khi ads tắt (đừng skip, sẽ lệch layout so với bản gốc).
- `feature/library` phục vụ cả Downloaded, PrivateVideo và SelectVideo từ chung `LibraryScreen` + `LibraryViewModel`; biến thể private là Koin-qualified instance `PrivateLibrary` (`isPrivate = true`).
- **Private area di chuyển file thật.** `MediaLibraryRepositoryImpl.setPrivate` gọi `FileUtil.moveToPrivateStorage` / `moveOutOfPrivateStorage` để chuyển file giữa MediaStore công khai và `FileUtil.privateDir` (`<externalFilesDir>/private_media`, có `.nomedia`), rồi mới ghi DB bằng `updateSecurityAndPath` — cờ `is_security` và `file_path` LUÔN đi cùng nhau. Đừng thêm lại API chỉ set mỗi `is_security`: trước đây private chỉ là cờ hiển thị nên Gallery/app khác vẫn thấy file. Khi xoá file khỏi public phải xoá cả row MediaStore (`deleteFromPublicStorage`), xoá mỗi file sẽ để lại row treo và Gallery vẫn liệt kê.
- `FileUtil.PRIVATE_FOLDER_NAME` phải khác `FOLDER_NAME`: khi `IS_APP_DATA_DIR_USE = true` thì `folderDir` chính là `<externalFilesDir>/FOLDER_NAME`, dùng chung thư mục sẽ khiến `.nomedia` ẩn luôn cả thư viện thường.
- Thư mục tải về = Downloads công khai + subfolder do user đặt trong Settings (DataStore `DOWNLOAD_LOCATION_SUBFOLDER`, đọc qua `downloadLocationSubfolderBlocking()`). Mọi chỗ ghi/rename qua MediaStore phải dùng `FileUtil.downloadsRelativePath()`, đừng hardcode `Environment.DIRECTORY_DOWNLOADS` nữa. Cố ý KHÔNG dùng SAF tree picker — sẽ phải viết lại toàn bộ list/rename/delete/dedupe sang DocumentFile.
- Có 2 kiểu "download button state" khác nhau (`DownloadButtonState` ở pipeline detect cũ vs `DownloadButtonUiState` ở Compose layer) — host map qua lại, không merge chúng.

## Khi không chắc

Nếu cấu trúc thực tế trong repo khác với mô tả ở đây (feature mới, file đã bị xoá/đổi tên), **tin vào code hiện tại**, không tin file này — cập nhật lại CLAUDE.md nếu phát hiện lệch.

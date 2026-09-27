# CleanPix - Android (Kotlin + Jetpack Compose)

Ứng dụng native phát hiện và dọn dẹp ảnh trùng lặp (**Exact**) và ảnh tương đồng (**Similar**) cho Android, xây dựng hoàn toàn bằng **Kotlin** và **Jetpack Compose (Material 3)**.

---

## 1. Điểm Nổi Bật Về Kỹ Thuật

- **Bảng Màu Thương Hiệu**: Primary `#2F6BFF` (Deep Blue), Accent `#FFB300` (Gold Amber), Nền sáng `#FAFAFA`, Nền tối `#121212`.
- **UI Declarative Hiện Đại**: 100% Jetpack Compose với Material 3, hỗ trợ Dark/Light Theme.
- **Hỗ Trợ Toàn Diện Quyền Truy Cập (Tất cả phiên bản Android)**:
  - Android 14 (API 34): `READ_MEDIA_VISUAL_USER_SELECTED` (quyền truy cập một phần, hiển thị banner hướng dẫn và nút mở Cài đặt).
  - Android 13 (API 33): `READ_MEDIA_IMAGES`.
  - Android 12 trở xuống: `READ_EXTERNAL_STORAGE` (`maxSdkVersion="32"`).
  - Android 9 trở xuống: `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion="28"`).
- **Thuật Toán Exact Duplicate 100%**:
  - Lọc nhanh theo dung lượng file `sizeBytes`.
  - Không dùng cột `DATA` (đã deprecated), tạo URI bằng `ContentUris.withAppendedId` và đọc luồng `openInputStream`.
  - Băm luồng **SHA-256** với bộ đệm 64 KB, không nạp toàn bộ ảnh vào RAM, chống tràn bộ nhớ (OOM).
  - Hai file cùng dung lượng nhưng khác mã SHA-256 KHÔNG bao giờ bị gom chung.
- **Thuật Toán Similar Photo**:
  - Tính **dHash (Difference Hash) 64-bit** từ thumbnail 9x8 grayscale (`ContentResolver.loadThumbnail` trên API 29+, `inSampleSize` trên API thấp hơn, giải phóng `recycle()` bitmap tạm tức thì).
  - Đo khoảng cách Hamming (`java.lang.Long.bitCount(a XOR b)`) với 3 mức độ nhạy tuỳ chỉnh:
    - **Strict**: $\le 3$ bit.
    - **Normal**: $\le 5$ bit (mặc định).
    - **Loose**: $\le 10$ bit.
  - **Quy tắc vàng**: 1 đại diện từ mỗi Exact Set + các ảnh không trùng $\rightarrow$ **Không bao giờ có ảnh xuất hiện ở cả 2 tab**.
- **Cơ Chế Xóa Scoped Storage 3 Phân Nhánh Chuẩn**:
  - **API 30+**: `MediaStore.createTrashRequest` (mặc định) qua `StartIntentSenderForResult`, ảnh được đưa vào thùng rác hệ thống (có thể khôi phục trong 30 ngày).
  - **API 29**: `contentResolver.delete`, bắt `RecoverableSecurityException` để mở intent xác nhận của người dùng.
  - **API $\le 28$**: Xóa trực tiếp qua `contentResolver.delete` với quyền `WRITE_EXTERNAL_STORAGE`.
- **An Toàn Khi Xóa**:
  - Không bao giờ tự tick chọn ảnh được đánh dấu yêu thích (`isFavorite`).
  - Hiển thị hộp thoại cảnh báo xác nhận nếu người dùng chọn xóa TOÀN BỘ ảnh trong một bộ.
  - Chỉ cộng vào "Photos cleaned so far" sau khi việc xóa được hệ điều hành xác nhận thành công.
- **Kết Quả Đo Hiệu Năng Thực Tế (Benchmark)**:
  - 10.000 ảnh (49.995.000 cặp): **~51 ms**
  - 30.000 ảnh (449.985.000 cặp): **~248 ms** (hoàn toàn dưới ngưỡng 5 giây).

---

## 2. Cấu Trúc Thư Mục

```
PhotosRemover-Android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/photosremover/app/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/ (PhotoItem, DuplicateSet, ScanProgress, SimilarSensitivity)
│   │   │   │   │   ├── repository/ (PhotoRepository)
│   │   │   │   │   └── local/ (CleanedStatsStore)
│   │   │   │   ├── domain/ (DuplicateEngine)
│   │   │   │   └── ui/
│   │   │   │       ├── theme/ (Color, Theme, Type)
│   │   │   │       ├── screens/ (HomeScreen, ScanningScreen, ResultsScreen, PreviewScreen)
│   │   │   │       ├── components/ (LaserScannerAnimation, PhotoCard, SetCard, BottomActionBar, AppDrawer)
│   │   │   │       └── viewmodel/ (MainViewModel)
│   │   │   └── res/
│   │   └── test/
│   │       └── java/com/photosremover/app/DuplicateEngineTest.kt
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 3. Hướng Dẫn Mở & Chạy Dự Án

### Mở bằng Android Studio:
1. Mở **Android Studio** (Hedgehog, Iguana, Jellyfish hoặc mới hơn).
2. Chọn **Open** $\rightarrow$ Trỏ tới thư mục `d:\PhotosRemover\PhotosRemover-Android`.
3. Chờ Gradle đồng bộ (Sync Project with Gradle Files).
4. Chọn thiết bị ảo (Emulator) chạy **Android 11 trở lên (API 30+)** hoặc cắm điện thoại thật bật USB Debugging.
5. Nhấn nút **Run 'app'** (`Shift + F10`).

### Chạy Unit Test & Benchmark:
```bash
./gradlew test
```

### Build APK Debug:
```bash
./gradlew assembleDebug
```
File APK đầu ra sẽ nằm tại: `app/build/outputs/apk/debug/app-debug.apk`.

# Remo Duplicate Photos Remover - iOS (Swift + SwiftUI)

Ứng dụng native phát hiện và dọn dẹp ảnh trùng lặp (**Exact**) và ảnh tương đồng (**Similar**) cho iOS, xây dựng hoàn toàn bằng **Swift 5.9+** và **SwiftUI**.

> [!IMPORTANT]
> **Đã tích hợp sẵn file `PhotosRemover.xcodeproj`**:
> Dự án đã được đóng gói và tích hợp sẵn đầy đủ file **`PhotosRemover.xcodeproj`** (kèm Shared Scheme & Assets)!
> Bạn có thể **click đúp vào file `PhotosRemover.xcodeproj`** hoặc chạy `open PhotosRemover.xcodeproj` trên macOS để mở trực tiếp trong Xcode mà **không cần** cài đặt XcodeGen hay chạy thêm lệnh nào.

---

## 1. Điểm Nổi Bật Về Kỹ Thuật

- **Mở Trực Tiếp Trên Xcode (`PhotosRemover.xcodeproj`)**:
  - Tích hợp sẵn Project PBX structure chuẩn Apple, kèm Shared Scheme `PhotosRemover` sẵn sàng chạy Simulator hoặc thiết bị thật.
  - Vẫn giữ file `project.yml` và script `generate_xcodeproj.py` nếu muốn tự động hóa tái tạo dự án trong CI/CD.
- **App Icon Chuẩn Nhận Diện Remo**:
  - Tích hợp sẵn `Assets.xcassets/AppIcon.appiconset` đầy đủ mọi độ phân giải (1024x1024 Universal/App Store, 180x180, 120x120, 87x87, 80x80, 58x58, 40x40, 29x29, iPad 152x152, 167x167).
  - Không còn bất kỳ cảnh báo "AppIcon not found" nào khi build trên Xcode.
- **UI Declarative Hiện Đại**: 100% SwiftUI, hỗ trợ Dark/Light Mode, đồng bộ màu vàng hổ phách đặc trưng (`#FFC800`), hiệu ứng laser scan sống động.
- **Phân Tích & Biểu Đồ Dung Lượng Bộ Nhớ (Storage Breakdown)**:
  - Tích hợp `DeviceStorageService` và biểu đồ Donut Chart phân tích dung lượng máy (Ảnh, Video, Hệ thống, Ứng dụng & Khác, Bộ nhớ trống).
- **Hỗ Trợ Quét Cả Ảnh & Video & Sắp Xếp Theo Dung Lượng**:
  - Tự động phân loại 3 tab riêng biệt: **Exact** (ảnh trùng 100%), **Similar** (ảnh tương đồng), và **Videos** (video trùng khớp).
  - Hỗ trợ sắp xếp theo dung lượng giảm dần (**Dung lượng lớn nhất** / **Dung lượng nhỏ nhất** / **Số lượng ảnh**) giúp giải phóng dung lượng tối đa một cách nhanh chóng.
  - Trích xuất frame video bằng `AVFoundation` (`AVAssetImageGenerator`) tại các mốc thời gian để đối chiếu độ sai khác thị giác và thời lượng.
- **Quản Lý Quyền Truy Cập Ảnh Chuẩn**:
  - Hỗ trợ đầy đủ: `.authorized`, `.limited` (hiển thị banner truy cập một phần), `.denied`, `.restricted`.
- **Thuật Toán Exact Duplicate (Trùng 100%)**:
  - Lọc dung lượng nhanh theo `sizeBytes`.
  - Tính mã băm **SHA-256** bằng `CryptoKit` với luồng dữ liệu chunk không tốn RAM.
- **Thuật Toán Similar (Tương Đồng)**:
  - Tính **dHash 64-bit** (Difference Hash) từ thumbnail 9x8 grayscale vẽ bằng `CGContext`.
  - So sánh khoảng cách Hamming với 3 mức độ nhạy: Strict (≤ 3), Normal (≤ 5), Loose (≤ 10).
- **An Toàn Khi Xóa**:
  - Không bao giờ tự tick ảnh yêu thích (Favorite).
  - Xác nhận an toàn và đưa vào thư mục **Recently Deleted** (lưu 30 ngày).

---

## 2. Cấu Trúc Thư Mục

```
PhotosRemover-iOS/
├── PhotosRemover.xcodeproj/    # File project Xcode chuẩn đã tạo sẵn, mở được ngay
│   ├── project.pbxproj
│   ├── project.xcworkspace/
│   └── xcshareddata/
├── project.yml                 # Cấu hình XcodeGen (tuỳ chọn)
├── generate_xcodeproj.py       # Script Python sinh project độc lập không phụ thuộc công cụ ngoài
├── PhotosRemover/
│   ├── Assets.xcassets/        # Đầy đủ AppIcon và AccentColor chuẩn Apple
│   │   ├── AppIcon.appiconset/ # Bộ icon mọi kích thước (1024x1024, iPhone, iPad)
│   │   │   ├── AppIcon-1024.png
│   │   │   ├── AppIcon-60x60@3x.png ...
│   │   │   └── Contents.json
│   │   ├── AccentColor.colorset/
│   │   └── Contents.json
│   ├── App/
│   │   ├── PhotosRemoverApp.swift
│   │   └── Info.plist
│   ├── Models/
│   │   ├── PhotoItem.swift
│   │   ├── DuplicateSet.swift
│   │   ├── ScanProgress.swift
│   │   ├── SimilarSensitivity.swift
│   │   └── StorageBreakdown.swift
│   ├── Services/
│   │   ├── CleanedStatsStore.swift
│   │   ├── DeviceStorageService.swift
│   │   ├── PhotoLibraryService.swift
│   │   ├── DuplicateEngine.swift
│   │   └── ExclusionStore.swift
│   ├── ViewModels/
│   │   ├── MainViewModel.swift
│   ├── Views/
│   │   ├── HomeView.swift
│   │   ├── ScanningView.swift
│   │   ├── ResultsView.swift
│   │   ├── PreviewView.swift
│   │   └── Components/
│   │       ├── LaserScannerView.swift
│   │       ├── PhotoCardView.swift
│   │       ├── SetCardView.swift
│   │       ├── BottomActionBarView.swift
│   │       ├── DrawerView.swift
│   │       └── StorageDonutChartView.swift
│   └── PhotosRemoverTests/
│       └── DuplicateEngineTests.swift
└── README.md
```

---

## 3. Hướng Dẫn Mở & Chạy Dự Án (Trên macOS)

### Yêu cầu:
- macOS Ventura (13.0+) hoặc Sonoma/Sequoia (14.0+).
- **Xcode 15.0 trở lên**.

### Cách 1: Mở Trực Tiếp (Khuyên dùng - Nhanh nhất)
Chỉ cần mở thư mục `PhotosRemover-iOS` trên máy Mac và:
- **Click đúp vào file `PhotosRemover.xcodeproj`**.
- Hoặc gõ lệnh trong Terminal:
  ```bash
  cd PhotosRemover-iOS
  open PhotosRemover.xcodeproj
  ```
- Dự án sẽ mở ngay trong Xcode với Scheme `PhotosRemover` đã được chọn sẵn.
- Chọn thiết bị (Simulator iPhone 15/16 hoặc máy thật) và bấm **Run (`⌘R`)** hoặc test với **`⌘U`**!

### Cách 2: Tự động hóa qua XcodeGen (Nếu muốn)
Nếu bạn thích dùng XcodeGen:
```bash
brew install xcodegen
cd PhotosRemover-iOS
xcodegen generate
open PhotosRemover.xcodeproj
```

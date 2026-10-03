# Kiểm tra batch Brighton–Bournemouth 2026/27

Ngày chuẩn bị: 2026-10-03. Chỉ thêm dữ liệu local; không sửa importer/schema,
không tạo test Java riêng, không chạy full test/build và không nhập H2.

## Kiểm tra dữ liệu và nguồn

- Hai API roster đúng tên CLB, season=2026 và asOf=2026-10-03 trả 30 Brighton,
  26 AFC Bournemouth. Có đúng 56 ID duy nhất, cùng 56 cặp player_id/club_id
  với hai CSV hồ sơ 02/10; không giới hạn roster 25 người.
- profiles.csv và roster-status.csv có một dòng cho mỗi ID trong roster hiện hành.
- Hồ sơ bản cuối giữ mọi ô đã đủ của API. Chỉ bổ sung height_cm=182 cho Younes
  Ibrahim (2000030176, club_id=1000000397) từ đúng SofaScore ID 1899640.
  profile-updates.csv chỉ có một dòng cho ô này; không sửa hồ sơ khác.
- Chema Andrés OVR69 và Nehemiah Oriola OVR60 giữ nguồn người dùng 02/10,
  đã có trên API. Các CSV gốc 02/10 trước bổ sung vẫn được giữ nguyên.
- 53 hồ sơ EA FC 27 xác minh được, Position/Alt Positions ghi trong sources.md.
  Tên/biến thể, quốc tịch và tuổi khớp dữ liệu hiện có; 53 OVR trên trang đều
  khớp OVR hiện có. Không dùng hồ sơ FC 26 hoặc OVR thẻ sự kiện.
- positions.csv có 53 ID duy nhất thuộc roster: Brighton27, Bournemouth26.
  Chỉ CDM → CM cho chính/phụ, gộp mã trùng; primary có trong eligible.
  Không dùng nhóm rộng để quyết định quyền chơi vị trí.
- API chưa có vị trí cho 56 người, nên expected_* trống. Ba người Brighton
  MISSING vẫn có dòng hồ sơ/tracking nhưng không có incoming trống trong CSV
  vị trí (reader không chấp nhận vị trí mới trống).
- Giữ nguyên tất cả file dữ liệu đã có trước batch này, đối chiếu SHA-256.

## Reader hiện có

Gọi trực tiếp PlayerProfileCsvReader và PlayerPositionCsvReader từ các class đã
biên dịch hiện có ở backend/target/classes, không khởi động Spring/database.
Lệnh tạm chỉ gọi hai reader và in số dòng, nằm dưới backend/target/ (Git ignored),
không thêm test JUnit hoặc mã dự án. Dùng Java source launcher, không build backend.

```text
COMMON_READERS_OK profiles=56 positions=53
exit code=0
```

Reader kiểm tra header/định dạng, ID dương và không lặp, ngày, chiều cao,
LEFT/RIGHT/BOTH, số áo/OVR, season_year=2026, mã vị trí được hỗ trợ,
eligible không trùng và chứa primary. Kiểm tra batch đối chiếu thêm ID/CLB
với snapshot roster API và giữ các trường đã có; không truy cập MySQL.

Lần gọi bằng JShell ban đầu đã đọc được hai CSV nhưng lỗi khi lưu lịch sử vào
Windows Registry bị chặn. Đã thay bằng lệnh gọi reader trực tiếp ở trên, kết thúc
thành công. Không sửa importer/schema để xử lý lỗi công cụ kiểm tra này.

Không chạy importer/H2, không thử lại các batch trước; hành vi xung đột/idempotent
của importer đã có kiểm tra chung từ các bước trước. Lượt này không báo đã kiểm
tra nhập DB hoặc production, chỉ xác nhận CSV qua reader và đối chiếu roster.

## Độ phủ bản cuối

| CLB | Hồ sơ | Quốc tịch | DOB | Chiều cao | Chân thuận | Số áo | OVR số | Có vị trí | MISSING vị trí |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Brighton | 30 | 30 | 30 | 30 | 30 | 30 | 29 | 27 | 3 |
| Bournemouth | 26 | 26 | 26 | 26 | 26 | 26 | 26 | 26 | 0 |

- 53 primary, 101 mã eligible, không có primary ngoài tập hợp.
- Chema Andrés 2000030174: thiếu primary_position, eligible_positions; giữ OVR69.
- Nehemiah Oriola 2000030170: thiếu primary_position, eligible_positions; giữ OVR60.
- Younes Ibrahim 2000030176: thiếu fc27_overall, primary_position, eligible_positions;
  chiều cao đã đủ182 cm, OVR vẫn trống/SQL NULL.
- Không gán 0 hoặc tự cấp OVR. Ba người chưa đủ dữ liệu chọn Fantasy.
- Mọi URL và lý do thiếu được ghi trong sources.md và missing-fields.txt.

## Áp dụng hồ sơ sau này

Importer hồ sơ chỉ chèn, báo xung đột khi dữ liệu đã lưu khác profiles.csv.
Do height_cm Younes vẫn NULL trong snapshot API, phải review rồi cập nhật có
điều kiện đúng ô theo profile-updates.csv trước khi chạy profiles.csv cuối vào
production. Bản kê cập nhật một ô không phải đầu vào PlayerProfileCsvReader.
Khi được phép ghi, vẫn cần xác nhận đích, membership và giá trị hiện hành, sao
lưu SQL mới; không tự ghi đè một giá trị khác. Lượt này chưa làm các bước đó.

## File mới cần review/commit

Trong thư mục này: profiles.csv, positions.csv, roster-status.csv, sources.md,
missing-fields.txt, profile-updates.csv, validation.md.

Commit message đề xuất:

```text
data(players): prepare Brighton and Bournemouth 2026 profiles and positions
```

Chưa ghi MySQL production, commit, push hoặc deploy; không thay membership,
nhóm vị trí rộng, dữ liệu trận, OVR người dùng hoặc dữ liệu/logic mùa 2024/25.

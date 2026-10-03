# Kiểm tra batch hull-leeds — 2026/27

Ngày kiểm tra: 2026-10-04. Dùng snapshot roster PremierHub `season=2026&asOf=2026-10-03`.

## Reader hiện có

```text
COMMON_READERS_OK profiles=56 positions=56
```

Đã chạy `PlayerProfileCsvReader` và `PlayerPositionCsvReader` trên đúng hai CSV mới.

## Kiểm tra dữ liệu chung

- PASS: profiles.csv và roster-status.csv có đúng 56 ID của toàn bộ hai roster, mỗi ID một dòng.
- PASS: club_id giữ nguyên membership PremierHub; không thêm cầu thủ ngoài roster.
- PASS: cả sáu trường hồ sơ/OVR khớp snapshot API; không ghi đè giá trị đã có.
- PASS: positions.csv có 56 người, 104 mã eligible sau ánh xạ CDM → CM.
- PASS: season_year=2026; mã hợp lệ, primary thuộc eligible, không trùng ID hoặc mã trong tập hợp.
- PASS: roster-status và URL nguồn có dòng riêng cho từng ID; người thiếu không có incoming vị trí giả.
- PASS: người thiếu giữ ô CSV trống (reader xử lý NULL), không gán 0 hoặc suy vị trí từ nhóm rộng.

## Độ phủ

| CLB | Roster | Quốc tịch | Ngày sinh | Chiều cao | Chân thuận | Số áo | OVR | Vị trí |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Hull City AFC | 34 | 34 | 34 | 34 | 34 | 34 | 34 | 34 |
| Leeds United FC | 22 | 22 | 22 | 22 | 22 | 22 | 22 | 22 |

## Thiếu và khác biệt nguồn

Không còn trường thiếu trong hai roster này.

Có 3 khác biệt ngày sinh/quốc tịch giữa nguồn, giữ giá trị hồ sơ đã có và ghi chi tiết tại sources.md / missing-fields.txt. Khác biệt không bị đổi thành NULL.

Alfie Cresswell (2000020081): giữ chiều cao 178 cm đã có trong API do người dùng cung cấp; không dùng ô trống của snapshot hồ sơ cũ.

## Phạm vi

Chỉ thêm dữ liệu local. Không sửa code/importer/schema, membership, nhóm rộng, OVR đã có, thống kê trận hay mùa 2024/25. Không tạo test Java riêng, nhập H2, chạy full test/build hoặc kiểm tra lại các batch trước. Chưa ghi MySQL production, commit, push hoặc deploy.

File cần commit: profiles.csv, positions.csv, roster-status.csv, sources.md, missing-fields.txt, validation.md trong thư mục này. Scratch và log backend/target/ không commit.

Commit message chung cho ba batch:

```text
data(players): prepare 2026 profiles and positions for final six clubs
```

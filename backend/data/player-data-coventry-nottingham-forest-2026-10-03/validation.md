# Kiểm tra batch coventry-nottingham-forest — 2026/27

Ngày kiểm tra: 2026-10-04. Dùng snapshot roster PremierHub `season=2026&asOf=2026-10-03`.

## Reader hiện có

```text
COMMON_READERS_OK profiles=54 positions=53
```

Đã chạy `PlayerProfileCsvReader` và `PlayerPositionCsvReader` trên đúng hai CSV mới.

## Kiểm tra dữ liệu chung

- PASS: profiles.csv và roster-status.csv có đúng 54 ID của toàn bộ hai roster, mỗi ID một dòng.
- PASS: club_id giữ nguyên membership PremierHub; không thêm cầu thủ ngoài roster.
- PASS: cả sáu trường hồ sơ/OVR khớp snapshot API; không ghi đè giá trị đã có.
- PASS: positions.csv có 53 người, 101 mã eligible sau ánh xạ CDM → CM.
- PASS: season_year=2026; mã hợp lệ, primary thuộc eligible, không trùng ID hoặc mã trong tập hợp.
- PASS: roster-status và URL nguồn có dòng riêng cho từng ID; người thiếu không có incoming vị trí giả.
- PASS: người thiếu giữ ô CSV trống (reader xử lý NULL), không gán 0 hoặc suy vị trí từ nhóm rộng.

## Độ phủ

| CLB | Roster | Quốc tịch | Ngày sinh | Chiều cao | Chân thuận | Số áo | OVR | Vị trí |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Coventry City FC | 30 | 30 | 30 | 29 | 29 | 29 | 29 | 29 |
| Nottingham Forest FC | 24 | 24 | 24 | 24 | 24 | 24 | 24 | 24 |

## Thiếu và khác biệt nguồn

George Shepherd (2000030180): thiếu height_cm, preferred_foot, shirt_number, fc27_overall, primary_position, eligible_positions. Giữ MISSING; chưa đủ điều kiện chọn Fantasy.

Có 1 khác biệt ngày sinh/quốc tịch giữa nguồn, giữ giá trị hồ sơ đã có và ghi chi tiết tại sources.md / missing-fields.txt. Khác biệt không bị đổi thành NULL.

## Phạm vi

Chỉ thêm dữ liệu local. Không sửa code/importer/schema, membership, nhóm rộng, OVR đã có, thống kê trận hay mùa 2024/25. Không tạo test Java riêng, nhập H2, chạy full test/build hoặc kiểm tra lại các batch trước. Chưa ghi MySQL production, commit, push hoặc deploy.

File cần commit: profiles.csv, positions.csv, roster-status.csv, sources.md, missing-fields.txt, validation.md trong thư mục này. Scratch và log backend/target/ không commit.

Commit message chung cho ba batch:

```text
data(players): prepare 2026 profiles and positions for final six clubs
```

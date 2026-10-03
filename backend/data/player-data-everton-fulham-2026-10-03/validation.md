# Kiểm tra dữ liệu Everton – Fulham 2026/27

Ngày: 2026-10-03. Chỉ thêm dữ liệu local, không sửa mã/schema/importer.

## Roster, hồ sơ và nguồn

- API PremierHub `season=2026&asOf=2026-10-03`: Everton 20, Fulham 26.
- profiles.csv và roster-status.csv có đúng 46 ID duy nhất, một dòng cho mỗi ID
  hiện hành; cặp player_id/club_id khớp API và hai batch hồ sơ 02/10.
- Toàn bộ giá trị hồ sơ và OVR trong profiles.csv bằng snapshot API; không sửa ô
  đã có hoặc điền số thay NULL. Không đổi membership, nhóm vị trí rộng hoặc thống kê.
- Đã đọc 44 hồ sơ EA FC 27 chính thức: canonical URL/EA ID, tiêu đề FC27, đúng
  ratingsEntries.items[0], không dùng Similar players. 44 ngày sinh chính xác và
  44 OVR khớp hồ sơ đã lưu; 43 quốc tịch khớp trực tiếp hoặc tên quốc gia tương ứng.
- Gonzalo García 2000030082 có nationality cũ D Mallorca Yo không khớp EA Spain.
  Giữ nguyên theo yêu cầu; profile_status=REVIEW_REQUIRED và ghi trong nguồn/text.
  Không dùng EA để sửa trường hồ sơ SofaScore; không coi ô đã có này là NULL.
- Chỉ đọc lại SofaScore Macaulay Zepa để tìm height_cm/shirt_number thiếu; nguồn
  vẫn không ghi hai ô này. Không lấy số followers thành số áo, không suy từ nguồn khác.
- Braiden Graham và Macaulay Zepa chưa tìm được hồ sơ EA FC27 đúng người. Giữ OVR
  NULL và trạng thái vị trí MISSING; không chặn 44 người đã xác minh trong cùng batch.

## Kiểm tra reader và dữ liệu chung

Đã gọi hai reader hiện có từ backend/target/classes, qua Java source launcher
chỉ gọi PlayerProfileCsvReader và PlayerPositionCsvReader/in số dòng. Lệnh tạm
nằm trong backend/target/ (Git ignored); không tạo test Java riêng hay build backend.

```text
COMMON_READERS_OK profiles=46 positions=44
exit code=0
NEW_BATCH_CHECKS_OK profiles=46 positions=44 roster_status=46 missing_ids=2 review_ids=1 canonical_profiles=44
```

Reader kiểm tra header, số cột, ID dương và duy nhất, định dạng ngày, chiều cao,
LEFT/RIGHT/BOTH, số áo/OVR, season2026, mã vị trí và primary thuộc tập eligible.
Kiểm tra chung đối chiếu thêm ID/CLB roster, nguồn EA, bảo toàn giá trị đã có,
các file đồng nhất danh sách và UTF-8. Kiểm tra cú pháp nationality của reader
không xác minh tên quốc gia thực tế; vì vậy Gonzalo vẫn cần review dù reader đạt.

44 primary, 76 mã eligible; không trùng ID/mã, đúng một primary trong tập.
Chỉ CDM → CM, gộp trùng sau ánh xạ. Hai MISSING không có dòng incoming trống
trong positions.csv vì reader không chấp nhận; vẫn có trong profiles/tracking.

## Độ phủ từng trường

| CLB | Roster | Quốc tịch có ô | DOB | Cao | Chân thuận | Số áo | Đủ năm ô | OVR số | Vị trí | Review hồ sơ |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Everton | 20 | 20 | 20 | 20 | 20 | 20 | 20 | 19 | 19 | 0 |
| Fulham | 26 | 26 | 26 | 25 | 26 | 25 | 25 | 25 | 25 | 1 |

Đủ năm ô Fulham gồm một hồ sơ nationality cần xác nhận, không phải 25 hồ sơ
đã xác minh lại. Danh sách thiếu và review chi tiết ở missing-fields.txt/sources.md:

- Braiden Graham 2000030192: fc27_overall, primary_position, eligible_positions.
- Macaulay Zepa 2000030194: height_cm, shirt_number, fc27_overall,
  primary_position, eligible_positions.
- Gonzalo García 2000030082: nationality cần xác nhận (đã có giá trị, không NULL).

Không chạy importer/H2, full test/build, kiểm tra lại batch cũ, ghi production,
commit, push hoặc deploy. Các batch Brentford–Palace/Brighton và dữ liệu/logic
mùa 2024/25 giữ nguyên. Không tuyên bố đã kiểm tra nhập DB/idempotent trong lượt này.

## File mới cần review/commit

profiles.csv, positions.csv, roster-status.csv, sources.md, missing-fields.txt,
validation.md. Không có profile-updates.csv vì không thay đổi giá trị hồ sơ nào.

Commit message đề xuất:

```text
data(players): prepare Everton and Fulham 2026 profiles and positions
```

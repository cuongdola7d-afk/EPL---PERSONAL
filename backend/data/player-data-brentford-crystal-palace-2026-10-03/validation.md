# Kiểm tra dữ liệu local Brentford – Crystal Palace

Ngày: 2026-10-03; chỉ mùa 2026/27. Không sửa mã/schema/importer.

## Đối chiếu batch

- API `season=2026&asOf=2026-10-03`: Brentford 28, Crystal Palace 24.
- profiles.csv và roster-status.csv có đúng 52 ID duy nhất, đúng cặp ID/CLB của
  roster hiện hành và hai batch hồ sơ 02/10. Không thêm người ngoài roster.
- Giữ nguyên 312 giá trị hồ sơ/OVR đã đủ (52 × 6); không có ô cần bổ sung SofaScore.
- 52 hồ sơ EA FC 27 khớp danh tính qua tên/EA ID, quốc tịch và tuổi theo DOB.
  Hai hồ sơ đọc trực tiếp HTML cũng khớp ngày sinh. Tất cả OVR EA khớp giá trị đã có.
- positions.csv có 52 primary và 95 mã eligible; mã hợp lệ, không lặp ID/mã,
  đúng một primary trong tập eligible, chỉ CDM → CM và gộp trùng sau ánh xạ.
- expected_* trống khớp snapshot API thiếu vị trí. Không suy từ nhóm vị trí rộng.
- Không có MISSING; roster-status.csv và missing-fields.txt ghi đầy đủ độ phủ.

## Hai reader hiện có

Đã gọi PlayerProfileCsvReader và PlayerPositionCsvReader từ class đã biên dịch
hiện có trong backend/target/classes qua Java source launcher. Lệnh tạm chỉ gọi
hai reader/in số dòng nằm ở backend/target/ (Git ignored), không phải test JUnit.

```text
COMMON_READERS_OK profiles=52 positions=52
exit code=0
```

Reader kiểm tra header, ID dương và duy nhất, định dạng ngày, chiều cao, chân thuận,
số áo/OVR, season_year=2026, các mã vị trí, tập không lặp và chứa primary.
Đối chiếu batch kiểm tra thêm roster/membership và bảo toàn trường đã có.

Không chạy importer/H2, full test/build hoặc kiểm tra lại batch cũ. Không tuyên bố
đã nhập DB hay kiểm tra idempotent trong lượt này. Không ghi MySQL production,
commit, push hoặc deploy. Không sửa dữ liệu/logic 2024/25, OVR đã có, membership,
nhóm vị trí rộng hoặc thống kê trận.

## File để review/commit

profiles.csv, positions.csv, roster-status.csv, sources.md, missing-fields.txt,
validation.md. Nguồn và các trường thiếu luôn có file riêng theo quy trình đã chốt.

```text
data(players): prepare Brentford and Palace 2026 data and Brighton supplements
```

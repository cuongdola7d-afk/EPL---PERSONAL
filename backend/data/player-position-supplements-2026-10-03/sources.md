# James Rowswell – bổ sung vị trí 2026/27

Nguồn vị trí: người dùng cung cấp ngày 03/10/2026: `James Rowswell : cb/ rb`.
Chuẩn hóa thành primary **CB**, eligible **CB|RB**, theo thứ tự người dùng ghi.
Không gắn nhãn EA đã xác minh cho bổ sung này.

- PremierHub `player_id=2000030248`, `club_id=1000000073`, Tottenham Hotspur FC.
- [API đối chiếu ID/CLB và dữ liệu đang có](https://epl-personal-production.up.railway.app/api/players/2000030248?season=2026&asOf=2026-10-03).
- API chưa có vị trí; expected_* trống. OVR **61** hiện có giữ nguyên, nguồn người
  dùng ngày 02/10 trong `backend/data/player-profile-updates-2026-10-02.csv`.
- `positions.csv` có đúng một dòng theo PlayerPositionCsvReader. Đây là bổ sung
  riêng; giữ nguyên toàn bộ batch Chelsea–Tottenham ngày 03/10, kể cả snapshot
  hai người MISSING của batch đó. Khi áp dụng bổ sung sau batch gốc, James đủ dữ
  liệu vị trí; Mahdi Nicoll-Jazuli vẫn chờ OVR và vị trí. Chưa ghi production.
- Kiểm tra reader/importer chung trên H2 riêng; kết quả ở validation.md cùng thư mục.

Không sửa hồ sơ, OVR, membership, nhóm rộng, thống kê trận hoặc mùa 2024/25.

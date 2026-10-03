# Kiểm tra bổ sung James Rowswell

Ngày 2026-10-03. Nguồn vị trí là người dùng, không phải EA đã xác minh.

- API xác nhận player_id 2000030248, Tottenham club_id 1000000073, OVR 61,
  primary NULL/eligible rỗng. Không sửa OVR.
- CSV có một dòng: primary CB, eligible CB|RB, season_year 2026;
  expected_* trống. Dùng đúng PlayerPositionCsvReader/PlayerPositionImporter.
- H2 riêng backend/target/aston-villa-newcastle-data/rowswell-db.mv.db, seed
  schema và dữ liệu hiện có của đúng một người; không dùng cấu hình MySQL.

| Lần | Rows | Inserted | Updated |
| --- | ---: | ---: | ---: |
| 1 | 1 | 1 | 0 |
| 2 | 1 | 0 | 0 |

Cả hai lệnh exit code 0. Đọc lại H2: primary CB, hai eligible CB và RB,
OVR 61 giữ nguyên. Không tạo test riêng hoặc chạy full test/build.

Batch Chelsea–Tottenham ngày 03/10 được giữ nguyên toàn bộ; bổ sung này áp dụng
sau batch đó khi phát hành sau này. Mahdi Nicoll-Jazuli vẫn chờ OVR và vị trí.
Chưa ghi production, commit, push hoặc deploy; không sửa mùa 2024/25.

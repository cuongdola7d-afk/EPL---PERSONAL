# Bổ sung trường hồ sơ mùa 2026/27, ngày 02/10/2026

Nguồn của 13 giá trị trong `player-profile-updates-2026-10-02.csv` là phần người dùng điền vào `player-profiles-2026-10-02-missing-fields.txt`. Đây là dữ liệu người dùng cung cấp; lượt cập nhật này không xác minh thêm URL EA SPORTS FC 27 hoặc SofaScore. Mười cầu thủ có dấu `x` ở cuối dòng không được điền giá trị thay thế.

Các CSV `players.csv` theo CLB được giữ như bản chụp của các batch nhập ban đầu. Vì importer hồ sơ chỉ chèn và từ chối nội dung khác, không chạy lại các CSV cũ để áp dụng bản bổ sung này. CSV bổ sung lưu đúng 13 ô được thay đổi, theo `player_id`, `club_id`, tên cột và giá trị mới.

## Ghi Railway MySQL production

- Đích đã kiểm tra: `altaria.proxy.rlwy.net`, database `railway`, MySQL 9.4.0. Trước ghi có 534 hồ sơ mùa 2026/27. Cả 13 cặp ID/CLB khớp CSV gốc, có đúng một membership hiệu lực ngày 02/10/2026, và cả 13 ô cũ đều là SQL `NULL`.
- Sao lưu SQL mới trước ghi (ngoài Git): `backend/local-backups/player-profile-updates-2026-10-02/premierhub-before-profile-updates-20261002-174814.sql`; 797.681 byte; SHA-256 `691FE5ABD94141FFE918E4CF0AB28A6E423AB814A3AD7D732FC6011E4307D657`.
- Một lệnh `UPDATE` có điều kiện khóa `(league_id=39, season_year=2026, player_id, club_id)`, membership hiện hành và trường cũ `IS NULL` đã cập nhật đúng 13 dòng. Chạy lại cùng lệnh cập nhật 0 dòng.
- Đọc lại MySQL: 13/13 giá trị khớp CSV; 534 hồ sơ, 524 OVR có số, 10 OVR `NULL`, 6 chiều cao `NULL`, 2 chân thuận `NULL`, 2 số áo `NULL`. Mười cầu thủ đánh dấu `x` đều giữ OVR `NULL`; toàn bộ sáu chiều cao, hai chân thuận và hai số áo còn thiếu thuộc nhóm này.
- API production trả 534 cầu thủ hiện hành, khớp cả 12 OVR mới và 10 OVR `NULL` của nhóm `x`. Bảng `players` vẫn 984 dòng, `manual_fixture_player_stats` vẫn 2.000 dòng, mùa 2024/25 còn trong `seasons`.

Không sửa roster, membership, dữ liệu trận hay mã chạy. Đây là thay đổi dữ liệu MySQL; không cần deploy để các OVR mới xuất hiện qua API hiện hành.

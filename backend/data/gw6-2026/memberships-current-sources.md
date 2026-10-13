# GW6 2026/27 — membership hiện hành cho 22 cầu thủ

Người dùng giao sửa membership để hiện trên web, cho phép nhập Aiven sau kiểm tra local và backup. Không nhập thống kê GW6, sửa tổng mùa, snapshot Fantasy, commit hoặc push.

## Căn cứ và lựa chọn ngày

- Đối chiếu ID, tên và vị trí từ roster/`player_season_stats` hiện có; CLB từ ảnh Lineups GW6 người dùng gửi và nguồn trận đã thu thập. Không tìm lại thống kê GW1–5 hoặc thay danh tính/vị trí.
- Ngày bắt đầu là ngày trận GW6 đã được xác nhận trong ảnh: 10/10 hoặc 11/10/2026. Đây là mốc đã có bằng chứng cho khoảng hiện hành mới, không phải ngày chuyển nhượng và không suy lùi về đầu mùa. Người dùng xác nhận phạm vi sửa CLB hiện tại; ảnh và roster không có bằng chứng CLB khác cho 22 người này.
- `end_date` để trống (SQL NULL), không hết hiệu lực sau ngày trận. 100 khoảng cũ đều đã kết thúc trước khoảng mới; giữ nguyên chúng. Importer kiểm tra chồng lấn toàn batch rồi thêm 22 khoảng; không cần đóng/gộp/kéo dài khoảng cũ.
- CSV dùng `ManualRosterImporter`, giữ chính xác tên và fantasy_position đang lưu. Chưa có người thiếu ID, thiếu dòng mùa/CLB hoặc khoảng xung đột ở preflight.

## Danh sách và khoảng đang lưu

| Player ID | Tên hiện có | CLB | Fixture bằng chứng | Khoảng cũ [start;end) | Khoảng hiện hành đề xuất |
| --- | --- | --- | --- | --- | --- |
| 2000016002 | Anan Khalaili | Crystal Palace FC | 1000560596 | 2026-08-22 → 2026-08-23 / 1000000354<br>2026-08-28 → 2026-08-29 / 1000000354<br>2026-09-05 → 2026-09-06 / 1000000354<br>2026-09-12 → 2026-09-13 / 1000000354<br>2026-09-20 → 2026-09-21 / 1000000354 | 2026-10-11 → NULL |
| 2000020024 | Daizen Maeda | Ipswich Town FC | 1000560599 | 2026-08-22 → 2026-08-23 / 1000000349<br>2026-08-30 → 2026-08-31 / 1000000349<br>2026-09-04 → 2026-09-05 / 1000000349<br>2026-09-12 → 2026-09-13 / 1000000349<br>2026-09-19 → 2026-09-20 / 1000000349 | 2026-10-10 → NULL |
| 2000020028 | Abdoul Ouattara | Ipswich Town FC | 1000560599 | 2026-08-22 → 2026-08-23 / 1000000349<br>2026-08-30 → 2026-08-31 / 1000000349<br>2026-09-04 → 2026-09-05 / 1000000349<br>2026-09-12 → 2026-09-13 / 1000000349<br>2026-09-19 → 2026-09-20 / 1000000349 | 2026-10-10 → NULL |
| 2000020033 | Luke O'Nien | Sunderland AFC | 1000560592 | 2026-08-22 → 2026-08-23 / 1000000071<br>2026-08-30 → 2026-08-31 / 1000000071<br>2026-09-05 → 2026-09-06 / 1000000071<br>2026-09-12 → 2026-09-13 / 1000000071<br>2026-09-20 → 2026-09-21 / 1000000071 | 2026-10-10 → NULL |
| 2000020035 | Trai Hume | Sunderland AFC | 1000560592 | 2026-08-22 → 2026-08-23 / 1000000071<br>2026-08-30 → 2026-08-31 / 1000000071<br>2026-09-05 → 2026-09-06 / 1000000071<br>2026-09-12 → 2026-09-13 / 1000000071<br>2026-09-20 → 2026-09-21 / 1000000071 | 2026-10-10 → NULL |
| 2000020060 | Ola Aina | Nottingham Forest FC | 1000560596 | 2026-08-22 → 2026-08-23 / 1000000351<br>2026-08-29 → 2026-08-30 / 1000000351<br>2026-09-05 → 2026-09-06 / 1000000351<br>2026-09-12 → 2026-09-13 / 1000000351<br>2026-09-19 → 2026-09-20 / 1000000351 | 2026-10-11 → NULL |
| 2000020065 | Jayden Bogle | Leeds United FC | 1000560593 | 2026-08-22 → 2026-08-23 / 1000000341<br>2026-08-30 → 2026-08-31 / 1000000341<br>2026-09-05 → 2026-09-06 / 1000000341<br>2026-09-14 → 2026-09-15 / 1000000341<br>2026-09-20 → 2026-09-21 / 1000000341 | 2026-10-10 → NULL |
| 2000020068 | Ethan Ampadu | Leeds United FC | 1000560593 | 2026-08-22 → 2026-08-23 / 1000000341<br>2026-08-30 → 2026-08-31 / 1000000341<br>2026-09-05 → 2026-09-06 / 1000000341<br>2026-09-14 → 2026-09-15 / 1000000341<br>2026-09-20 → 2026-09-21 / 1000000341 | 2026-10-10 → NULL |
| 2000020069 | Brenden Aaronson | Leeds United FC | 1000560593 | 2026-08-22 → 2026-08-23 / 1000000341<br>2026-08-30 → 2026-08-31 / 1000000341<br>2026-09-05 → 2026-09-06 / 1000000341<br>2026-09-14 → 2026-09-15 / 1000000341<br>2026-09-20 → 2026-09-21 / 1000000341 | 2026-10-10 → NULL |
| 2000020071 | Noah Okafor | Leeds United FC | 1000560593 | 2026-08-22 → 2026-08-23 / 1000000341<br>2026-08-30 → 2026-08-31 / 1000000341<br>2026-09-05 → 2026-09-06 / 1000000341<br>2026-09-14 → 2026-09-15 / 1000000341<br>2026-09-20 → 2026-09-21 / 1000000341 | 2026-10-10 → NULL |
| 2000020087 | Tyrique George | Everton FC | 1000560594 | 2026-08-22 → 2026-08-23 / 1000000062<br>2026-08-29 → 2026-08-30 / 1000000062<br>2026-09-06 → 2026-09-07 / 1000000062<br>2026-09-12 → 2026-09-13 / 1000000062<br>2026-09-19 → 2026-09-20 / 1000000062 | 2026-10-11 → NULL |
| 2000020093 | Brennan Johnson | Everton FC | 1000560594 | 2026-08-22 → 2026-08-23 / 1000000062<br>2026-08-29 → 2026-08-30 / 1000000062<br>2026-09-06 → 2026-09-07 / 1000000062<br>2026-09-12 → 2026-09-13 / 1000000062<br>2026-09-19 → 2026-09-20 / 1000000062 | 2026-10-11 → NULL |
| 2000020103 | Tyrick Mitchell | Crystal Palace FC | 1000560596 | 2026-08-22 → 2026-08-23 / 1000000354<br>2026-08-28 → 2026-08-29 / 1000000354<br>2026-09-05 → 2026-09-06 / 1000000354<br>2026-09-12 → 2026-09-13 / 1000000354<br>2026-09-20 → 2026-09-21 / 1000000354 | 2026-10-11 → NULL |
| 2000020105 | Yéremy Pino | Crystal Palace FC | 1000560596 | 2026-08-22 → 2026-08-23 / 1000000354<br>2026-08-28 → 2026-08-29 / 1000000354<br>2026-09-05 → 2026-09-06 / 1000000354<br>2026-09-12 → 2026-09-13 / 1000000354<br>2026-09-20 → 2026-09-21 / 1000000354 | 2026-10-11 → NULL |
| 2000020107 | Evann Guessand | Crystal Palace FC | 1000560596 | 2026-08-22 → 2026-08-23 / 1000000354<br>2026-08-28 → 2026-08-29 / 1000000354 | 2026-10-11 → NULL |
| 2000020116 | Óscar Mingueza | Crystal Palace FC | 1000560596 | 2026-08-22 → 2026-08-23 / 1000000354<br>2026-08-28 → 2026-08-29 / 1000000354<br>2026-09-05 → 2026-09-06 / 1000000354<br>2026-09-12 → 2026-09-13 / 1000000354<br>2026-09-20 → 2026-09-21 / 1000000354 | 2026-10-11 → NULL |
| 2000030002 | Keane Lewis-Potter | Brentford FC | 1000560601 | 2026-08-22 → 2026-08-23 / 1000000402<br>2026-08-30 → 2026-08-31 / 1000000402<br>2026-09-05 → 2026-09-06 / 1000000402<br>2026-09-12 → 2026-09-13 / 1000000402<br>2026-09-18 → 2026-09-19 / 1000000402 | 2026-10-10 → NULL |
| 2000030044 | Georginio Rutter | Brighton & Hove Albion FC | 1000560592 | 2026-08-23 → 2026-08-24 / 1000000397<br>2026-09-19 → 2026-09-20 / 1000000397 | 2026-10-10 → NULL |
| 2000030060 | Alejandro Garnacho | Aston Villa FC | 1000560601 | 2026-08-23 → 2026-08-24 / 1000000058<br>2026-08-31 → 2026-09-01 / 1000000058<br>2026-09-05 → 2026-09-06 / 1000000058<br>2026-09-12 → 2026-09-13 / 1000000058<br>2026-09-19 → 2026-09-20 / 1000000058 | 2026-10-10 → NULL |
| 2000030062 | Alysson | Aston Villa FC | 1000560601 | 2026-08-23 → 2026-08-24 / 1000000058<br>2026-08-31 → 2026-09-01 / 1000000058<br>2026-09-05 → 2026-09-06 / 1000000058<br>2026-09-12 → 2026-09-13 / 1000000058 | 2026-10-10 → NULL |
| 2000030101 | Mathys Tel | Tottenham Hotspur FC | 1000560600 | 2026-08-22 → 2026-08-23 / 1000000073<br>2026-08-29 → 2026-08-30 / 1000000073<br>2026-09-05 → 2026-09-06 / 1000000073<br>2026-09-12 → 2026-09-13 / 1000000073 | 2026-10-10 → NULL |
| 2000030114 | Juanlu Sánchez | AFC Bournemouth | 1000560595 | 2026-08-23 → 2026-08-24 / 1000001044<br>2026-09-05 → 2026-09-06 / 1000001044<br>2026-09-20 → 2026-09-21 / 1000001044 | 2026-10-10 → NULL |

## Trạng thái kiểm tra

- Preflight đọc Aiven `defaultdb`, UUID `715946cc-c33a-11f1-8fd9-8e0831c19405`, phiên bản 8.4.8. Đã đọc cấu hình Render qua API chỉ đọc và xác nhận host/port/database/user/password trùng kết nối Aiven; không in giá trị bí mật.
- File: `manual-memberships-current-2026-GW6.csv`; SHA-256 `fd8d0b1d2fbd805ab76fee1512d2b3f66d85f0c3419ba7c0b46add33bf20bd5f`.
- Local/API, backup, production và kiểm tra nhập lại: **đã đạt**, kết quả chi tiết bên dưới.

## Kết quả local

- Dùng bản đọc hẹp của 22 ID trên H2 memory riêng; chạy ManualRosterImporter thật và API PlayerController/FootballQueries qua MockMvc, không khởi động Spring hoặc chạy sync/schema trên production. Biên dịch mã hiện có, không sửa dependency/code.
- Trước sửa roster/list/detail không có 22 người (detail 404) nhưng API matches trả đủ 100 dòng GW1–5. Sau sửa list/detail 22/22 đúng CLB, lịch sử GW1–5 giữ nguyên; asOf=2026-11-01 vẫn có 22 người. Lịch sử không phụ thuộc membership hiện tại: trước sửa, người không có roster hiện hành vẫn xem được lịch sử nhờ khoảng tại ngày trận.
- Lần đầu rows=22 playersInserted=0 membershipsInserted=0 intervalsInserted=22 intervalsUpdated=0. Lần hai mọi thay đổi=0. Thống kê, tổng mùa, tên/ID và khoảng cũ giữ nguyên. CSV GW6 đạt reader; bốn ngoại lệ PLAYED/rating trống giữ nguyên.

## Backup và nhập production

- Đích Aiven `defaultdb`, UUID `715946cc-c33a-11f1-8fd9-8e0831c19405`, MySQL 8.4.8; cấu hình datasource Render đã so sánh qua API chỉ đọc, kết nối Java nhập dùng TLS VERIFY_IDENTITY và CA hiện có.
- Backup mới hoàn tất UTC `2026-10-12T13:25:11.934342+00:00` trước nhập: `backend/local-backups/gw6-current-memberships-20261012-132506/aiven-before-current-memberships-20261012-132506.sql`; **193473 byte**; SHA-256 `705c332f8ad4056038f445cf32584f269ea1293903ff7d64e85b2d473635cfd1`. File được Git ignore; ba bảng đầy đủ DDL/dữ liệu: players, player_season_stats, manual_player_memberships. Đã kiểm tra mã thoát dump, marker hoàn tất, đọc được file và phạm vi ba bảng; chưa thử restore backup này.
- Chạy importer hiện có trực tiếp với datasource đã xác minh, không khởi động Spring/SQL init hoặc runner khác. Lần đầu thêm 22 khoảng; lần hai tất cả số thay đổi=0. Không cập nhật khoảng cũ, tạo player hoặc dòng mùa mới.
- API production `https://epl-personal.onrender.com`: GET /api/players?season=2026 có đủ 22 người; GET /api/players/{id}?season=2026 trả 200 cho 22/22, trùng CLB và nội dung danh sách. GET /api/players/{id}/matches?season=2026 của cả 22 người giữ nguyên 100 dòng GW1–5 từng payload so với trước nhập. Trước nhập list/detail của họ đều vắng/404 nhưng lịch sử đã có 100 dòng.
- Đọc MySQL sau nhập: 122 khoảng cho 22 người = 100 cũ giữ nguyên + 22 mới end_date=NULL; không chồng lấn. Tổng mùa, profile, vị trí, danh tính và fixture giữ nguyên; Fantasy gameweeks/entries/draft picks/submitted snapshots GW6 bằng nhau trước/sau.
- Đối chiếu toàn CSV GW6: 360/360 dòng có đúng một membership hiệu lực tại ngày trận; 0 lỗi membership. CSV thống kê SHA-256 vẫn `5fa0d29878b299c0c5c6e7f5e066bdc832e2b7ff5798d4bd72fbf7b06c2c1c87`; bốn ngoại lệ vẫn PLAYED và rating/fantasy_points trống ở CSV/sidecar. Không nhập thống kê GW6: database vẫn 0 dòng GW6, không tự tạo bốn dòng NULL trong SQL.
- Frontend trang cầu thủ/chi tiết CLB/BXH gọi API roster ngày hiện tại, không ép asOf cũ. Fantasy dùng asOf riêng và snapshot đã lưu; không đổi asOf hoặc các snapshot đó.
- Đã gọi trực tiếp API qua web Vercel `https://premierhub.vercel.app/api/players?season=2026`: đủ 22 người, từng payload/CLB khớp Render. Không chỉ kiểm tra kết nối trực tiếp backend.

## Bổ sung trận cuối — 13/10/2026

CSV hiện hành nay có 23 khoảng: giữ nguyên 22 khoảng đã nhập, thêm Jacob Murphy `2000030076`, Newcastle `1000000067`, từ `2026-10-12` đến NULL. Ảnh Lineups và nguồn trận cuối xác minh CLB, ngày bắt đầu có căn cứ; năm khoảng cũ GW1–5 giữ nguyên, không chồng lấn hoặc kéo lùi đầu mùa. Importer thêm đúng một khoảng và chạy lại 0; list/detail API Vercel hiện cầu thủ, lịch sử cũ giữ nguyên. Backup/kiểm chứng và thống kê mới ở [checkpoint trận cuối](final-fixture-2026-10-13.md). Các mục 22 người và chưa nhập thống kê phía trên là lịch sử trước bước này.
- CSV membership hiện hành và bảng nguồn này là đầu vào/lịch sử nhập cần giữ. Không thu thập lại GW1–5, tái tính tổng mùa, nhập thống kê GW6, công bố Fantasy, commit, push hoặc deploy.

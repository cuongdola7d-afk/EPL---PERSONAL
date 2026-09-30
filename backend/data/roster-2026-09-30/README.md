# Roster PremierHub 2026/27 — snapshot 30/09/2026

**Trạng thái: đã nhập và kiểm tra production — 534 cầu thủ hiện tại ở cả 20 CLB.**
28 trường hợp lệch vị trí vẫn chờ theo quyết định của người dùng; đây chưa phải toàn bộ 562 người khả dụng trong FPL.

Nguồn chính: [FPL bootstrap-static](https://fantasy.premierleague.com/api/bootstrap-static/). Snapshot có 667 dòng, gồm 105 dòng unavailable. Có 562 người còn khả dụng (kể cả chấn thương/nghi ngờ/treo giò); 28 người xung đột vị trí được giữ trong danh sách chờ theo quyết định của người dùng. Bốn CSV có 534 người, không giới hạn 25 người/CLB.

## Cách dùng và bảo toàn dữ liệu

- CSV đúng 7 cột. Các dòng hiện tại đã có giữ nguyên ngày và ID; khoảng mới bắt đầu **2026-09-30**, chỉ xác nhận tại ngày snapshot, không dùng để chứng minh membership GW1/GW2.
- Đối chiếu cả 869 ID trong database và tất cả CSV roster đã có. 423 ID được tái sử dụng, 111 ID mới liên tiếp `2000030145..2000030255`. FPL ID/code chỉ là khóa đối chiếu, không thay `player_id` nội bộ.
- Tên có dấu được giữ UTF-8. Người đã có giữ nguyên `players.name`. Các alias đã rà riêng gồm Ben/Benjamin White, Costinha/João Pedro Loureiro da Costa, Vitalii/Vitaliy Mykolenko, Josh/Joshua King, Abdul Fatawu/Abdul Fatawu Issahaku, Jaden Philogene/Philogene-Bidace, Tino/Valentino Livramento, Jair, Andrew/Andy Robertson và Mykhailo/Mykhaylo Mudryk.
- Vị trí lấy `element_type`: 1 GK, 2 DEF, 3 MID, 4 FWD. Không thay vị trí mùa đã lưu nếu xung đột. Xem [danh sách chờ](pending-confirmation.md).
- Không sửa/xóa khoảng membership lịch sử, thống kê GW1, GW4–GW5, hoặc dữ liệu 2024/25. Không sync football-data/API-Football, không cron/Pre-deploy Command.
- `batch-01..04-identities.json` lưu FPL ID/code, tên nguồn, ID database và cách ghép; `batch-01..04-progress.json` lưu tiến độ từng batch. Nhập bằng importer từ mã nguồn cùng đợt sửa hỗ trợ tái sử dụng ID provider.

## Số lượng

| CLB | Trước (API/DB) | Sau (API/DB) | ID mới |
| --- | ---: | ---: | ---: |
| Arsenal | 24 | 24 | 0 |
| Aston Villa | 0 | 27 | 7 |
| Bournemouth | 0 | 26 | 6 |
| Brentford | 0 | 28 | 9 |
| Brighton | 0 | 30 | 10 |
| Chelsea | 25 | 27 | 2 |
| Coventry City | 25 | 30 | 3 |
| Crystal Palace | 0 | 24 | 8 |
| Everton | 0 | 20 | 3 |
| Fulham | 0 | 26 | 5 |
| Hull City | 0 | 34 | 15 |
| Ipswich Town | 0 | 26 | 7 |
| Leeds | 0 | 22 | 7 |
| Liverpool | 25 | 31 | 5 |
| Man City | 25 | 26 | 1 |
| Man Utd | 26 | 30 | 4 |
| Newcastle | 0 | 28 | 7 |
| Nott'm Forest | 0 | 24 | 2 |
| Spurs | 0 | 29 | 6 |
| Sunderland | 0 | 22 | 4 |
| **Tổng** | **150** | **534** | **111** |

“Trước” truy vấn `season=2026&asOf=2026-09-30`, không tính membership lịch sử đã kết thúc.

## Kiểm tra

- 12 test liên quan: `mvn -Dtest=ManualRosterCsvReaderTest,ManualRosterImportTest -DforkCount=0 test`. Máy mới dùng JDK 25.0.2, biên dịch release 21. `forkCount=0` tránh lỗi classpath khi đường dẫn Windows có dấu; không đổi POM.
- Importer đọc dữ liệu đối chiếu một lần trong transaction rồi ghi JDBC batch, tránh truy vấn lại theo từng dòng qua mạng. Khi nhập MySQL trong đợt này, bật `rewriteBatchedStatements=true` cho riêng tiến trình importer; không đổi cấu hình Railway. Vẫn kiểm tra toàn CSV trước khi ghi, rollback khi lỗi, không ghi đè tên/vị trí/thống kê đã có.
- H2 cô lập được nạp bản sao 3.762 dòng/14 bảng từ production. Nhập bốn batch lần đầu: thêm 111 players, 150 player_season_stats và 384 intervals; `intervalsUpdated=0`.
- Nhập lần hai mỗi batch: tất cả số thêm/cập nhật đều 0. Không chồng khoảng, 534 ID duy nhất, 20 CLB, vị trí hợp lệ. Mọi dòng cũ của 14 bảng giữ nguyên; API query 2024 và roster trước ngày 30/09 giữ nguyên.
- Backup MySQL, bản sao JSON và log chi tiết chỉ lưu tại `backend/local-backups/roster-2026-09-30/` (Git ignored). Không commit credential/backup/build output.
- Production nhập bốn batch thành công, số thêm đúng H2; 869 → 980 players toàn database, 501 → 651 player_season_stats, 550 → 934 membership intervals. Mọi dòng cũ trong cả 14 bảng giữ nguyên, không có khoảng chồng lấn. Bằng chứng số lượng và SHA-256 từng bảng ở [verification.json](verification.json).
- API production `https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-09-30` trả 534 ID duy nhất, 20 CLB, tất cả vị trí hợp lệ và khớp CSV. So sánh 35 phản hồi API: chỉ roster ngày 30/09 thay đổi; 34 phản hồi được bảo vệ giữ nguyên, gồm chi tiết 24 fixture có thống kê ở hai mùa, roster 2024, roster 29/09, CLB, lịch đấu và BXH. GW1, GW4–GW5 và Fantasy Replay 2024/25 không bị sửa.
- Backup trước nhập: `backend/local-backups/roster-2026-09-30/before-import-20260930T034149Z.sql`, 536.244 byte, SHA-256 `6398bd8d4be7d912ee6366a55ab387ac1072420839b6d0caabfe77eecb5b16af`. Xác nhận database `railway`, MySQL 9.4.0, hai mùa mỗi mùa 20 CLB; API trước nhập khớp 150 người/6 CLB. Lần importer cũ bị dừng do quá nhiều lượt mạng đã được kiểm tra rollback: cả 14 bảng không đổi trước khi chạy bản ghi batch.

## File cần commit và bước tiếp theo

- Thư mục `backend/data/roster-2026-09-30/` (bốn CSV, nguồn, bảng đối chiếu, tiến độ, danh sách chờ và bằng chứng kiểm tra).
- `README.md` ở gốc repo; `ManualRosterCsvReader.java`, `ManualRosterImporter.java` trong `backend/src/main/java/com/premierhub/roster/`; hai test tương ứng trong `backend/src/test/java/com/premierhub/roster/`.
- Không đưa `.env.local`, `local-backups/`, `target/` vào commit. `.vscode/settings.json` có thay đổi riêng trong phiên này, không thuộc phần chỉnh sửa roster.
- Commit message đề xuất: `feat: expand 2026/27 rosters with safe batch imports`.

Chưa commit/push, chưa thu thập GW2. Batch GW2 đầu tiên đề xuất: Crystal Palace–Manchester City (`1000560555`, 28/08) và Liverpool–Nottingham Forest (`1000560552`, 29/08). Trước thống kê, cần xác minh membership đúng ngày trận; không dùng khoảng mới bắt đầu 30/09 làm bằng chứng cho GW2.

## Dấu vết nguồn

- Git ban đầu: local `main`, `origin/main` và GitHub main đều `1e04e51dce1851548c4ff4a5fc15023e203fbbe6`.
- Snapshot FPL SHA-256: `0270f4f776a6cd46faf11eb83d5436c59c3fecec7c997b151a253d2ed6dc744e`.
- Snapshot được lấy ngày 30/09/2026, deadline GW1 `2026-08-21T17:30:00Z` xác nhận mùa 2026/27. Dữ liệu FPL có thể thay đổi; dùng file snapshot kèm theo để tái hiện lần đối chiếu này.

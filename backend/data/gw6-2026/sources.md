# GW6 Premier League 2026/27 — checkpoint 13/10/2026

**Checkpoint hiện hành (13/10/2026):** Coventry–Newcastle `1000560597` FINISHED 4–1 đã nhập Aiven sau backup được kiểm tra. Toàn GW6 đủ 10 trận, 400 dòng thống kê, 20 đội hình và xác nhận CSV cuối; 315 PLAYED, 85 DID_NOT_PLAY, 310 rating và 5 confirmed-unrated (thêm Dan Burn). Các importer chạy lại đều thay đổi 0; tổng mùa đã cập nhật qua GW6. API Vercel cả 10 trận trả đúng 11–11 đá chính; sân trận cuối đã kiểm tra Chrome desktop/mobile. Mã backend hiện tại đọc Aiven trả `ready=true`, 0 cảnh báo; API ADMIN production còn 16 cảnh báo cho 8 lượt chọn chấn thương do Render đang chạy `0b3fd0e`, chưa có sửa đổi `e3ef0f4`. **Bước còn lại: deploy backend có `e3ef0f4` (HEAD hiện tại `0cca281` đã chứa), kiểm tra lại API readiness rồi ADMIN tự công bố.** Không cần deploy frontend để hiện dữ liệu mới. Chưa công bố, commit, push hoặc deploy. Căn cứ và kiểm chứng chi tiết ở [trận cuối](final-fixture-2026-10-13.md); các ghi chú pending bên dưới là lịch sử.

**Checkpoint cho lượt tiếp theo (13/10/2026):** chín fixture FINISHED đã có thống kê, sân 11v11 và bằng chứng Fantasy; sáu xác nhận chấn thương ngoài danh sách trận cho tám lượt chọn đã nhập. Coventry City–Newcastle United `1000560597` vẫn **pending** theo dữ liệu/checkpoint gần nhất; chưa có thống kê, đội hình hoặc bằng chứng kết quả cho trận này và không công bố Fantasy GW6. Khi làm tiếp, đọc lại API/provider để xác nhận trạng thái thực tế trước mọi bước nhập. Ghi chú này không phải xác nhận trạng thái trận trực tiếp ngày 13/10.

Đã nhập xác nhận CSV cuối cho chín trận FINISHED và bốn xác nhận không được SofaScore chấm vào Aiven. [Checkpoint Fantasy trước khi xác nhận vắng mặt](fantasy-readiness-2026-10-12.md) ghi Coventry–Newcastle và sáu cầu thủ được chọn nằm ngoài danh sách trận; sáu trường hợp đó đã được xử lý ở checkpoint mới phía trên. Các ghi chú “chưa nhập bằng chứng Fantasy” bên dưới là lịch sử trước bước này.

Người dùng xác nhận tiếp ngày 12/10/2026: Nicolò Savona, Nikola Milenković (Forest–Palace `1000560596`), Alexander Isak, Dominik Szoboszlai (Liverpool–Man City `1000560598`), Reece James (Chelsea–Bournemouth `1000560595`) và James Wright (Aston Villa–Brentford `1000560601`) **không được đăng ký trận vì chấn thương**. Chỉ áp dụng điểm Fantasy 0 cho đúng cặp fixture/player trong [confirmed-not-registered.csv](confirmed-not-registered.csv), gồm tám lượt chọn ở bốn đội đã chốt. Đây là vắng mặt ngoài danh sách trận, khác `SUB_UNUSED`/`DID_NOT_PLAY`; không tạo thống kê trận hoặc rating 0, không suy rộng sang GW khác. Trạng thái nhập và readiness sau xử lý sẽ được ghi ở checkpoint Fantasy.

Đã nhập đủ sáu xác nhận vào Aiven, kiểm tra điểm 0 cho tám lượt chọn và chạy lại readiness bằng backend mã mới: chỉ còn sáu cảnh báo của Coventry–Newcastle. [Checkpoint sau xử lý](fantasy-absence-confirmed.md) ghi backup, kết quả importer, kiểm tra dữ liệu và phần backend cần deploy. Những dòng “còn sáu cầu thủ” ở checkpoint trước phía trên chỉ là lịch sử trước khi nhận xác nhận chấn thương.

## Sân 11v11 — đã nhập và kiểm tra production

- Nguyên nhân chung: Aiven production chưa có dữ liệu ở ba bảng đội hình; bảng `club_season_formations` còn thiếu `default_source`, `source_note`. File trước đó có vai trò/thay người nhưng chưa có sơ đồ trận và vị trí sân; importer cũ chỉ chấp nhận URL SofaScore. Vì vậy API trả `startersStatus=MISSING` và frontend không vẽ sân.
- Đã dùng ảnh Lineups người dùng cung cấp để điền 18 sơ đồ, 198 vị trí STARTER và mã tham chiếu `USER_SCREENSHOT: ...` theo fixture/CLB; không tạo URL giả hoặc suy ra đá chính từ phút/rating. Nottingham Forest đúng 3-4-2-1 theo ảnh. Batch có 360 vai trò: 198 STARTER, 85 SUB_USED, 77 SUB_UNUSED. `clubs.csv` vẫn giữ default nguồn USER, nhưng cập nhật số observation GW6 tương ứng. Các chỉ số thống kê/rating trước đó không đổi.
- Trước khi ghi: xác nhận cấu hình Render trỏ đúng Aiven `defaultdb` UUID `715946cc-c33a-11f1-8fd9-8e0831c19405`, không dùng Railway. Backup ngoài Git: `backend/local-backups/gw6-lineup-pitch-20261012-142944/aiven-before-gw6-lineups-20261012-142944.sql`, 689.750 byte, SHA-256 `e9fa6fa717306b07a41d8a0b5ccd6bd3768021705230c47f0ff459690955bcf5`. Dump đủ DDL/bảy bảng và số dòng, đã phục hồi kiểm tra bằng MySQL local. Migration thêm hai cột đã chạy thử và chạy lại không đổi schema.
- Aiven: importer lần đầu ghi 18 metadata CLB, 18 sơ đồ trận và 360 dòng vai trò; chạy lại `clubChanges=0`, `fixtureChanges=0`, `playerChanges=0`. SQL đọc lại khớp từng ô trong CSV. API Vercel trả đủ 11+11 STARTER có tọa độ cho cả 9 fixture FINISHED, đúng dự bị vào sân/không vào sân. Browser production kiểm tra 9 trận ở 1440px và 390px: sân hiển thị đủ 22 cầu thủ, không chồng thẻ, không tràn ngang hoặc lỗi JavaScript; ảnh kiểm tra nằm trong `backend/target/gw6-pitch/` và không commit.
- Các bảng thống kê, rating, membership, tổng mùa, toàn bộ GW1–5, dữ liệu 2024/25, snapshot Fantasy và chi tiết Coventry–Newcastle giữ nguyên khi đối chiếu trước/sau. `missing.csv` nay chỉ còn trận Coventry–Newcastle chưa kết thúc. Không công bố Fantasy, không deploy/commit/push. API và frontend đang chạy đã đọc được dữ liệu mới nên không cần deploy để thấy sân; mã importer đã sửa cần được deploy về sau nếu muốn chạy lại từ bản backend production.

## Checkpoint trước khi xử lý sân — ghi lại nguyên nhân và dữ liệu nền

## Trạng thái và bước tiếp theo — sau nhập production

- Đã đồng bộ kết quả/BXH và nhập cả hai đội của **9 fixture FINISHED**: `1000560592`, `1000560593`, `1000560594`, `1000560595`, `1000560596`, `1000560598`, `1000560599`, `1000560600`, `1000560601`. Có **360 dòng: 283 PLAYED, 77 DID_NOT_PLAY; 279 PLAYED có rating; 4 PLAYED đã xác nhận không được chấm; 0 rating còn chờ**. Phút/bàn/kiến tạo/thẻ và rating đã chốt giữ nguyên; SHA CSV không đổi. Mỗi đội thực tế có 20 người, không thêm người để đạt định mức.
- Bốn ngoại lệ Merino, Ayden Heaven, Marcus Rashford và Dwight McNeil vẫn PLAYED; rating/fantasy_points SQL NULL. DID_NOT_PLAY có rating NULL và fantasy_points=0. `confirmed-unrated.csv` giữ nguyên.
- Coventry City–Newcastle United `1000560597` vẫn chờ: nguồn TIMED, database/API SCHEDULED, kickoff `2026-10-12T19:00:00Z` = **13/10/2026 02:00 Việt Nam**. Tỉ số NULL, 0 dòng thống kê hoặc đội hình; không thay fixture này.
- Xác nhận Aiven `defaultdb`, MySQL 8.4.8, UUID `715946cc-c33a-11f1-8fd9-8e0831c19405` trùng endpoint và thông tin truy cập trong cấu hình Render production; không dùng Railway. Backup mới được tạo và kiểm tra trước ghi, chi tiết ở checkpoint bên dưới.
- Đồng bộ dùng `FootballDataClient`, `FootballDataBatch`, `FootballDataSync` hiện có. Truy vấn toàn mùa có 3 trạng thái GW6 cũ; truy vấn riêng GW6 mới xác nhận đủ 9 FINISHED và 1 TIMED. Đối chiếu ID/CLB/UTC trước khi thay các payload GW6 trong batch nguồn. Batch đầy đủ 20 đội/380 fixture/20 dòng BXH đã được kiểm tra; BXH nguồn khớp tất cả 59 kết quả đã kết thúc. Phạm vi ghi chỉ 9 fixture cùng 20 dòng BXH, không cập nhật đội hoặc fixture khác.
- Membership: 22 khoảng mở đã nhập ở lượt trước được giữ nguyên, chạy importer lại tất cả thêm/cập nhật=0. Toàn bộ 360 dòng hợp lệ tại ngày trận; roster hiện tại và chi tiết API của 22 người vẫn hiển thị đúng CLB. Lịch sử dùng membership tại ngày trận. Nguồn và khoảng cũ: [memberships-current-sources.md](memberships-current-sources.md).
- Đội hình: file có **360 vai trò và phút thay người của 18 đội**, gồm 198 STARTER, 85 SUB_USED, 77 SUB_UNUSED. Local importer trên schema hiện tại của repo nhập 360 rồi lặp tất cả thay đổi=0. **Production chưa nhập vai trò/thay người**: `LineupBatchImporter` gặp Unknown column `default_source` khi đọc `club_season_formations`; toàn transaction đội hình rollback. Bảng production còn thiếu `default_source` và `source_note`; các cột phút thay người đã có, nhưng importer vẫn bị chặn ở metadata CLB. Chưa tự migration hoặc viết SQL riêng để lách importer. Cả ba bảng đội hình vẫn trống như trước, default USER từ bundle và phạm vi GW1–5 giữ nguyên. API startersStatus=MISSING; không coi vai trò/thay người đã có trên web. Vị trí/tọa độ trong CSV vẫn trống. Preflight đã kiểm tra nội dung metadata nhưng bỏ sót cấu trúc cột vì bảng trống; sau lỗi đã đối chiếu schema và ghi chính xác blocker.
- **Sơ đồ thực tế của 18 đội chưa nhập**: `formations.csv` chỉ header; validator hiện yêu cầu URL SofaScore. Người dùng đã miễn yêu cầu URL, không yêu cầu gửi link lại và không tạo URL giả. Bằng chứng sơ đồ từ ảnh vẫn ở bảng bên dưới. API hiển thị formationSource=CLUB_DEFAULT, không coi đó là sơ đồ trận đã xác minh. `missing.csv` ghi riêng blocker kỹ thuật này.
- Kiểm tra local: mô phỏng toàn bộ luồng trên H2 bằng importer thật đạt; thống kê nhập 360 rồi lặp inserted=0; đội hình nhập 360 rồi lặp tất cả=0; membership lặp tất cả=0. Không sửa mã/schema/cấu hình; không chạy full test/build hoặc tạo test từng batch. Production thống kê và membership đạt tương ứng; phần đội hình rollback do schema cũ như trên. MySQL đọc lại đối chiếu từng ô CSV thống kê đều khớp; không khẳng định đội hình đã nhập.
- API Vercel: đã kiểm tra danh sách 10 trận GW6, chi tiết từng trận trong 9 trận đã nhập, BXH 20 đội, roster/detail 22 người và lịch sử 360 cầu thủ. Đủ 360 dòng lịch sử GW6; chỉ số/rating/điểm khớp MySQL; vai trò/thay người hiện MISSING đúng với transaction chưa nhập. 2.000 dòng SQL GW1–5 giữ nguyên; 100 payload lịch sử GW1–5 của 22 người khớp baseline; toàn bộ dòng lịch sử GW1–5 của 360 người khớp dữ liệu trước nhập.
- **Tổng mùa lưu `player_season_stats` vẫn đến GW5**, chưa cộng 9 trận GW6: cơ chế hiện có yêu cầu đủ 10 FINISHED/400 dòng, tiếp tục xác nhận 40/trận và 20/CLB từ GW1. `rebuildLatestComplete()` sau nhập và chạy lại xác nhận fixtures=50/matchRows=2000/seasonRows=593/updatedRows=0. Không gọi rebuildThroughGameweek(6), không ghi SQL tổng mùa để lách giới hạn. API goals/assists cộng trực tiếp các thống kê trận FINISHED đã lưu, nên khớp GW1–5 cộng 9 trận GW6; đây không phải xác nhận bảng tổng mùa đã theo kịp. Tổng appearances/minutes GW6 vẫn chưa được cập nhật trong bảng tổng mùa.
- Những ô PLAYED đã thiếu từ GW1 được giữ nguyên, không thu thập lại: Declan Rice `2000001023` tại `1000560542` thiếu minutes/goals/assists; Kobbie Mainoo `2000006025` tại `1000560543` thiếu goals/assists; Jack Clarke `2000011003` tại `1000560544` thiếu assists; Jack Hinshelwood `2000030041` tại `1000560549` thiếu assists. Service báo missingMinutes=1/missingGoals=2/missingAssists=4; tổng tương ứng vẫn NULL.
- Fantasy: **không nhập xác nhận fixture/unrated vào bộ bằng chứng Fantasy, không gọi readiness ADMIN, không công bố/tái công bố hoặc tái tính kết quả**. Bản ghi gameweek, 4 entries, 44 draft picks và 44 submitted picks GW6 giữ nguyên từng dòng, gồm deadline/rosterAsOf. Không coi bằng chứng Fantasy hoàn tất chỉ vì đã nhập thống kê/vai trò. Mùa 2024/25 giữ nguyên; không cron/Pre-deploy Command/commit/push/deploy.
- Bước tiếp theo: xử lý Coventry–Newcastle khi FINISHED; giữ nguyên 9 trận đã nhập khi mở rộng CSV. Đội hình cần migration tương thích importer và kiểm tra trước nhập; sơ đồ từ ảnh cần sửa validator không bắt URL và tổng mùa vòng chưa đủ trận cần sửa service trong lượt riêng. Không thu thập lại GW1–5, không yêu cầu lại xác nhận bốn ngoại lệ hoặc URL được miễn; công bố Fantasy vẫn cần lệnh riêng của người dùng.

## Nguồn lịch, ID và kết quả

- Đọc ngày **12/10/2026**: [football-data.org GW6](https://api.football-data.org/v4/competitions/PL/matches?season=2026&matchday=6), HTTP 200. Mùa provider `2026`, matchday 6. Giữ nguyên UTC; đối chiếu provider ID bằng `football_data_fixtures.fixture_id`, không dùng provider ID làm fixture_id.
- API sau nhập: [Vercel GW6](https://premierhub.vercel.app/api/matches?season=2026&matchweek=6), HTTP 200; 9 FINISHED và 1 SCHEDULED. Cấu hình backend Render đã đối chiếu Aiven; không dùng Railway cũ.
- Cache nguồn và bản đọc SQL/kiểm tra phụ nằm ở `backend/target/gw6-collection/`, được ignore; không chứa credentials trong bộ file dữ liệu. Thư mục GW trước được tái sử dụng để ghép danh tính, không thu thập lại chỉ số/rating các vòng đó.

| Fixture nội bộ | Provider ID | Trận | Kickoff UTC | Nguồn mới | Điểm |
| --- | --- | --- | --- | --- | --- |
| 1000560592 | 560592 | Sunderland AFC – Brighton & Hove Albion FC | 2026-10-10T14:00:00Z | FINISHED | 0–2 |
| 1000560593 | 560593 | Arsenal FC – Leeds United FC | 2026-10-10T11:30:00Z | FINISHED | 2–1 |
| 1000560594 | 560594 | Hull City AFC – Everton FC | 2026-10-11T13:00:00Z | FINISHED | 2–2 |
| 1000560595 | 560595 | Chelsea FC – AFC Bournemouth | 2026-10-10T14:00:00Z | FINISHED | 5–1 |
| 1000560596 | 560596 | Crystal Palace FC – Nottingham Forest FC | 2026-10-11T13:00:00Z | FINISHED | 3–1 |
| 1000560597 | 560597 | Coventry City FC – Newcastle United FC | 2026-10-12T19:00:00Z | TIMED | — |
| 1000560598 | 560598 | Liverpool FC – Manchester City FC | 2026-10-11T15:30:00Z | FINISHED | 0–3 |
| 1000560599 | 560599 | Ipswich Town FC – Fulham FC | 2026-10-10T14:00:00Z | FINISHED | 2–1 |
| 1000560600 | 560600 | Manchester United FC – Tottenham Hotspur FC | 2026-10-10T16:30:00Z | FINISHED | 1–1 |
| 1000560601 | 560601 | Aston Villa FC – Brentford FC | 2026-10-10T14:00:00Z | FINISHED | 2–2 |

## Nguồn chỉ số StatMuse và yêu cầu rating/Lineups

Chỉ lấy MinutesPlayed, Goals, Assists, YellowCards và RedCards từ dữ liệu Player Stats trên trang trận StatMuse. Không lấy Rating của StatMuse làm rating SofaScore, không tự truy cập trang trận SofaScore để lấy rating. Phút lấy đúng nguồn, không tự tính bằng 90 trừ phút thay người; thí dụ thay người được hiển thị phút 63 nhưng nguồn có 62/28 phút.

DID_NOT_PLAY có căn cứ danh sách dự bị và toàn bộ sự kiện thay người của trận kết thúc. Khi nguồn không có Player Stats cho dự bị không dùng, chỉ số sự kiện 0 chỉ được điền sau khi tổng Goals/Assists/YellowCards/RedCards của đội đã được giải thích toàn bộ bởi các dòng PLAYED và không có sự kiện bàn/thẻ của người dự bị đó; không coi một ô thiếu bất kỳ là 0. Mọi đội của chín trận đã qua đối chiếu này.

| Fixture | Đội | Danh sách | PLAYED | DID_NOT_PLAY | Rating đã đọc | Không chấm đã xác nhận | Ảnh người dùng | Sơ đồ quan sát | StatMuse |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- | --- |
| 1000560592 | Sunderland AFC | 20 | 15 | 5 | 15 | 0 | 9–10 | 4-2-3-1 | [Nguồn](https://www.statmuse.com/fc/match/-112815) |
| 1000560592 | Brighton & Hove Albion FC | 20 | 16 | 4 | 16 | 0 | 9–10 | 4-2-3-1 | [Nguồn](https://www.statmuse.com/fc/match/-112815) |
| 1000560593 | Arsenal FC | 20 | 16 | 4 | 15 | 1 | 1–2 | 4-2-3-1 | [Nguồn](https://www.statmuse.com/fc/match/-112816) |
| 1000560593 | Leeds United FC | 20 | 16 | 4 | 16 | 0 | 1–2 | 3-4-2-1 | [Nguồn](https://www.statmuse.com/fc/match/-112816) |
| 1000560594 | Everton FC | 20 | 16 | 4 | 16 | 0 | 15–16 | 4-2-3-1 | [Nguồn](https://www.statmuse.com/fc/match/-112822) |
| 1000560594 | Hull City AFC | 20 | 16 | 4 | 16 | 0 | 15–16 | 5-4-1 | [Nguồn](https://www.statmuse.com/fc/match/-112822) |
| 1000560595 | Chelsea FC | 20 | 16 | 4 | 16 | 0 | 5–6 | 3-4-2-1 | [Nguồn](https://www.statmuse.com/fc/match/-112817) |
| 1000560595 | AFC Bournemouth | 20 | 16 | 4 | 16 | 0 | 5–6 | 4-2-3-1 | [Nguồn](https://www.statmuse.com/fc/match/-112817) |
| 1000560596 | Nottingham Forest FC | 20 | 15 | 5 | 15 | 0 | 13–14 | 3-4-2-1 | [Nguồn](https://www.statmuse.com/fc/match/-112821) |
| 1000560596 | Crystal Palace FC | 20 | 15 | 5 | 14 | 1 | 13–14 | 3-4-2-1 | [Nguồn](https://www.statmuse.com/fc/match/-112821) |
| 1000560598 | Liverpool FC | 20 | 16 | 4 | 16 | 0 | 17–18 | 4-2-3-1 | [Nguồn](https://www.statmuse.com/fc/match/-112814) |
| 1000560598 | Manchester City FC | 20 | 15 | 5 | 15 | 0 | 17–18 | 4-2-3-1 | [Nguồn](https://www.statmuse.com/fc/match/-112814) |
| 1000560599 | Fulham FC | 20 | 16 | 4 | 16 | 0 | 7–8 | 4-2-3-1 | [Nguồn](https://www.statmuse.com/fc/match/-112819) |
| 1000560599 | Ipswich Town FC | 20 | 16 | 4 | 16 | 0 | 7–8 | 4-2-3-1 | [Nguồn](https://www.statmuse.com/fc/match/-112819) |
| 1000560600 | Manchester United FC | 20 | 16 | 4 | 14 | 2 | 11–12 | 4-2-3-1 | [Nguồn](https://www.statmuse.com/fc/match/-112818) |
| 1000560600 | Tottenham Hotspur FC | 20 | 16 | 4 | 16 | 0 | 11–12 | 4-2-3-1 | [Nguồn](https://www.statmuse.com/fc/match/-112818) |
| 1000560601 | Aston Villa FC | 20 | 15 | 5 | 15 | 0 | 3–4 | 4-2-3-1 | [Nguồn](https://www.statmuse.com/fc/match/-112813) |
| 1000560601 | Brentford FC | 20 | 16 | 4 | 16 | 0 | 3–4 | 4-2-3-1 | [Nguồn](https://www.statmuse.com/fc/match/-112813) |

### Bằng chứng từ ảnh người dùng ngày 12/10/2026

- Đánh số ảnh theo thứ tự 18 ảnh đính kèm trong tin nhắn: mỗi cặp gồm sân đá chính và bảng dự bị/thay người, cùng hai đội của một trận. Cặp 1–2 Arsenal–Leeds; 3–4 Villa–Brentford; 5–6 Chelsea–Bournemouth; 7–8 Ipswich–Fulham; 9–10 Sunderland–Brighton; 11–12 Manchester United–Tottenham; 13–14 Palace–Forest; 15–16 Hull–Everton; 17–18 Liverpool–Manchester City. Ảnh không có URL trận hoặc ID sự kiện SofaScore; không tạo URL hay đường dẫn ảnh giả.
- Vai trò của toàn bộ 360 người được đối chiếu từng người với ảnh; số 11 đá chính được xác nhận trực tiếp từ sân, không suy từ rating/phút. Danh tính Palace trong ảnh là C. Canvot = Jaydee Canvot `2000020096`, Q. Timber = Quinten Timber `2000030188`; không nhầm với cầu thủ khác ở CLB khác. J. Schuster của Brentford = Jannik Schuster `2000030099`. Giữ các alias đã xác nhận trong mục danh tính bên dưới.
- Ảnh người dùng đã cung cấp là bằng chứng cho sơ đồ trận và vị trí 11 cầu thủ đá chính của mỗi đội; `formations.csv` dùng mã `USER_SCREENSHOT: ...` tham chiếu đúng cặp ảnh, fixture và CLB. Palace, Chelsea, Leeds và Nottingham Forest dùng 3-4-2-1; Hull dùng 5-4-1; các đội còn lại 4-2-3-1. Default USER của Palace/Chelsea/Leeds vẫn giữ giá trị đã chốt, không đổi theo một trận.
- Phút thay người trong `lineups/players.csv` lấy đúng số hiển thị ở bảng ảnh. Ben Gannon-Doak và Aaron Wan-Bissaka/Aaron Hickey vào ở 46 theo ảnh dù đồng hồ nguồn StatMuse là 45:00; không sửa minutes thi đấu từ StatMuse. Ayden Heaven/Marcus Rashford hiển thị 90 ở ảnh, nguồn đồng hồ StatMuse ghi phút 90 với extraMinute=5; không dùng số phút thay người hiển thị để tính lại tổng phút.
- Bốn ngoại lệ đã được người dùng xác nhận trong phản hồi ngày 12/10/2026: Arsenal/Mikel Merino `2000001017`; Manchester United/Ayden Heaven `2000006023` và Marcus Rashford `2000006009`; Palace/Dwight McNeil `2000020097`. Xác nhận nguyên văn: “cả 4 đều là cầu thủ không đủ số phút nên cứ để rating như các cầu thủ dự bị”. Cả bốn là SUB_USED/PLAYED, minutes=1 theo thống kê đã thu thập; giữ rating và fantasy_points trống trong `confirmed-unrated.csv`, không đổi thành DID_NOT_PLAY hoặc đặt rating=0. Đã bỏ bốn dòng rating khỏi `missing.csv`; bằng chứng unrated mới chỉ chuẩn bị file, chưa nhập database.
- Chỉ cột rating được bổ sung 279 ô theo ảnh; không dùng icon bàn/kiến tạo/thẻ để sửa các chỉ số đã thu thập. Bảng người/ID cuối file có thêm cột rating để đối chiếu lại toàn bộ kết quả chép ảnh.

## Membership từng thiếu tại ngày trận — đã xử lý cả 22

Danh sách dưới đây là bằng chứng preflight trước sửa: các ID đã tồn tại và có dòng player_season_stats đúng CLB, nhưng khoảng cũ chưa bao phủ GW6. Đã bổ sung 22 khoảng mở từ ngày trận GW6 bằng importer hiện có sau kiểm tra local và backup; không đổi ID, vị trí, tổng mùa hoặc khoảng cũ. Các dòng membership_missing trong bảng là tình trạng cũ để truy vết, đã được bỏ khỏi missing.csv. Xem [kết quả nhập và ngày bắt đầu](memberships-current-sources.md).

| Fixture | CLB | Player ID | Tên trên nguồn | Vấn đề |
| --- | --- | --- | --- | --- |
| 1000560592 | Sunderland AFC | 2000020035 | Trai Hume | membership_missing |
| 1000560592 | Sunderland AFC | 2000020033 | Luke O'Nien | membership_missing |
| 1000560592 | Brighton & Hove Albion FC | 2000030044 | Georginio Rutter | membership_missing |
| 1000560593 | Leeds United FC | 2000020068 | Ethan Ampadu | membership_missing |
| 1000560593 | Leeds United FC | 2000020065 | Jayden Bogle | membership_missing |
| 1000560593 | Leeds United FC | 2000020069 | Brenden Aaronson | membership_missing |
| 1000560593 | Leeds United FC | 2000020071 | Noah Okafor | membership_missing |
| 1000560594 | Everton FC | 2000020093 | Brennan Johnson | membership_missing |
| 1000560594 | Everton FC | 2000020087 | Tyrique George | membership_missing |
| 1000560595 | AFC Bournemouth | 2000030114 | Juanlu | membership_missing |
| 1000560596 | Crystal Palace FC | 2000020103 | Tyrick Mitchell | membership_missing |
| 1000560596 | Crystal Palace FC | 2000020105 | Yeremi Pino | membership_missing |
| 1000560596 | Crystal Palace FC | 2000020116 | Óscar Mingueza | membership_missing |
| 1000560596 | Crystal Palace FC | 2000020107 | Evann Guessand | membership_missing |
| 1000560596 | Crystal Palace FC | 2000016002 | Anan Khalaili | membership_missing |
| 1000560596 | Nottingham Forest FC | 2000020060 | Ola Aina | membership_missing |
| 1000560599 | Ipswich Town FC | 2000020028 | Guimissongui Ouattara | membership_missing |
| 1000560599 | Ipswich Town FC | 2000020024 | Daizen Maeda | membership_missing |
| 1000560600 | Tottenham Hotspur FC | 2000030101 | Mathys Tel | membership_missing |
| 1000560601 | Aston Villa FC | 2000030060 | Alejandro Garnacho | membership_missing |
| 1000560601 | Aston Villa FC | 2000030062 | Alysson | membership_missing |
| 1000560601 | Brentford FC | 2000030002 | Keane Lewis-Potter | membership_missing |

## Danh tính và ngoại lệ được kế thừa

- Các tên khác tên PremierHub được ghép bằng StatMuse player ID trong `gw2-*/lineups.csv`, `gw3-*/lineups.csv`, `gw4-remaining-nine/lineups.csv`, `gw5-*/lineups.csv` đã có; không dùng fuzzy match. Ví dụ Alisson/Alisson Becker, Matty/Matt Crooks, Oliver/Oli McBurnie, John/John Victor, Guimissongui/Abdoul Ouattara.
- Riquelme Angulo `2000030255`: giữ Juan Angulo theo [hồ sơ Sunderland](../sunderland-profiles-2026-10-02/sources.md), nơi đã đối chiếu Juan Riquelme Angulo. Sindre Egeli `2000020029`: Sindre Walle Egeli theo [hồ sơ Ipswich](../ipswich-town-profiles-2026-10-02/sources.md).
- Chema `2000030174`: dùng danh tính José María Andrés Baixauli / Chema Andrés trong [bổ sung Brighton đã được người dùng chốt](../player-data-brighton-supplements-2026-10-03/sources.md) và [hồ sơ Brighton](../brighton-profiles-2026-10-02/sources.md), có membership hiệu lực từ 30/09. Các vòng cũ có ID Chema Andrés `2000030259` với membership từng ngày trận trước 30/09; không sửa/hợp nhất ID, rating hoặc dữ liệu lịch sử của ID đó. Ghi rõ khác biệt này để đối chiếu ảnh GW6 với danh tính hiện hành.

## Danh sách cầu thủ đã đối chiếu ảnh để tiếp tục từ checkpoint

Tên/ID và năm chỉ số giữ nguyên nguồn StatMuse; vai trò đã đối chiếu ảnh Lineups người dùng. Rating chỉ chép từ 18 ảnh SofaScore ở bảng nguồn trên. Dấu — ở cột rating là ô trống: SUB_UNUSED không có rating; bốn SUB_USED đã xác nhận không được chấm do không đủ phút. Không có điểm Fantasy trong bảng này.

| Fixture | CLB | Player ID | Tên nguồn | Vai trò xác nhận | Phút | Bàn | KT | Vàng | Đỏ | Rating ảnh |
| --- | --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1000560592 | Sunderland AFC | 2000012002 | Granit Xhaka | STARTER | 90 | 0 | 0 | 0 | 0 | 7.4 |
| 1000560592 | Sunderland AFC | 2000030103 | Kevin Danso | STARTER | 90 | 0 | 0 | 0 | 0 | 5.9 |
| 1000560592 | Sunderland AFC | 2000020040 | Omar Alderete | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560592 | Sunderland AFC | 2000020034 | Reinildo | STARTER | 72 | 0 | 0 | 0 | 0 | 7.1 |
| 1000560592 | Sunderland AFC | 2000020049 | Nordi Mukiele | STARTER | 90 | 0 | 0 | 0 | 0 | 7.4 |
| 1000560592 | Sunderland AFC | 2000020031 | Thomas Meunier | STARTER | 33 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560592 | Sunderland AFC | 2000020037 | Enzo Le Fée | STARTER | 80 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560592 | Sunderland AFC | 2000020041 | Wilson Isidor | STARTER | 90 | 0 | 0 | 0 | 0 | 6.1 |
| 1000560592 | Sunderland AFC | 2000030254 | Malick Fofana | SUB_USED | 57 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560592 | Sunderland AFC | 2000030252 | Dayann Methalie | SUB_USED | 18 | 0 | 0 | 0 | 0 | 6.3 |
| 1000560592 | Sunderland AFC | 2000020048 | Melker Ellborg | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560592 | Sunderland AFC | 2000020039 | Chemsdine Talbi | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560592 | Sunderland AFC | 2000020036 | Noah Sadiki | STARTER | 90 | 0 | 0 | 0 | 0 | 7.2 |
| 1000560592 | Sunderland AFC | 2000020035 | Trai Hume | STARTER | 90 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560592 | Sunderland AFC | 2000020033 | Luke O'Nien | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560592 | Sunderland AFC | 2000020042 | Chris Rigg | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560592 | Sunderland AFC | 2000020030 | Robin Roefs | STARTER | 90 | 0 | 0 | 0 | 0 | 5.9 |
| 1000560592 | Sunderland AFC | 2000020050 | Jocelin Ta Bi | SUB_USED | 18 | 0 | 0 | 0 | 0 | 6.6 |
| 1000560592 | Sunderland AFC | 2000012001 | Nilson Angulo | STARTER | 72 | 0 | 0 | 0 | 0 | 6.0 |
| 1000560592 | Sunderland AFC | 2000030255 | Riquelme Angulo | SUB_USED | 10 | 0 | 0 | 0 | 0 | 6.3 |
| 1000560592 | Brighton & Hove Albion FC | 2000030118 | Jason Steele | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560592 | Brighton & Hove Albion FC | 2000030045 | Lewis Dunk | SUB_USED | 31 | 0 | 0 | 0 | 0 | 7.1 |
| 1000560592 | Brighton & Hove Albion FC | 2000030169 | Matt O'Riley | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560592 | Brighton & Hove Albion FC | 2000030049 | Pascal Groß | STARTER | 90 | 0 | 1 | 0 | 0 | 7.3 |
| 1000560592 | Brighton & Hove Albion FC | 2000030119 | Pascal Struijk | STARTER | 56 | 0 | 0 | 0 | 1 | 6.2 |
| 1000560592 | Brighton & Hove Albion FC | 2000030055 | Bart Verbruggen | STARTER | 90 | 0 | 0 | 0 | 0 | 7.4 |
| 1000560592 | Brighton & Hove Albion FC | 2000030044 | Georginio Rutter | SUB_USED | 3 | 0 | 1 | 0 | 0 | 6.7 |
| 1000560592 | Brighton & Hove Albion FC | 2000030048 | Yasin Ayari | STARTER | 87 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560592 | Brighton & Hove Albion FC | 2000030052 | Ibrahim Osman | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560592 | Brighton & Hove Albion FC | 2000030054 | Malick Yalcouyé | SUB_USED | 23 | 0 | 0 | 0 | 0 | 6.8 |
| 1000560592 | Brighton & Hove Albion FC | 2000030167 | Ferdi Kadıoğlu | STARTER | 90 | 0 | 0 | 0 | 0 | 7.9 |
| 1000560592 | Brighton & Hove Albion FC | 2000030174 | Chema | STARTER | 90 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560592 | Brighton & Hove Albion FC | 2000030043 | Diego Gómez | STARTER | 87 | 0 | 0 | 0 | 0 | 7.0 |
| 1000560592 | Brighton & Hove Albion FC | 2000030117 | Olivier Boscagli | STARTER | 67 | 0 | 0 | 1 | 0 | 7.0 |
| 1000560592 | Brighton & Hove Albion FC | 2000030173 | Jaouen Hadjam | SUB_USED | 3 | 1 | 0 | 0 | 0 | 7.7 |
| 1000560592 | Brighton & Hove Albion FC | 2000030042 | Maxim De Cuyper | STARTER | 67 | 1 | 0 | 0 | 0 | 7.5 |
| 1000560592 | Brighton & Hove Albion FC | 2000030046 | Luka Vušković | STARTER | 90 | 0 | 0 | 0 | 0 | 7.6 |
| 1000560592 | Brighton & Hove Albion FC | 2000030051 | Charalampos Kostoulas | STARTER | 59 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560592 | Brighton & Hove Albion FC | 2000030050 | Costinha | SUB_USED | 23 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560592 | Brighton & Hove Albion FC | 2000030121 | Promise David | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560593 | Arsenal FC | 2000001004 | Ben White | STARTER | 62 | 0 | 1 | 0 | 0 | 7.4 |
| 1000560593 | Arsenal FC | 2000001007 | Bukayo Saka | STARTER | 90 | 0 | 0 | 0 | 0 | 7.2 |
| 1000560593 | Arsenal FC | 2000001001 | David Raya | STARTER | 90 | 0 | 0 | 0 | 0 | 7.6 |
| 1000560593 | Arsenal FC | 2000001023 | Declan Rice | STARTER | 90 | 0 | 1 | 0 | 0 | 7.2 |
| 1000560593 | Arsenal FC | 2000001009 | Eberechi Eze | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560593 | Arsenal FC | 2000001013 | Ezri Konsa | STARTER | 90 | 0 | 0 | 0 | 0 | 7.0 |
| 1000560593 | Arsenal FC | 2000001006 | Gabriel Magalhães | STARTER | 90 | 0 | 0 | 0 | 0 | 6.3 |
| 1000560593 | Arsenal FC | 2000001018 | Kai Havertz | STARTER | 62 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560593 | Arsenal FC | 2000001011 | Kepa Arrizabalaga | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560593 | Arsenal FC | 2000001008 | Martin Ødegaard | STARTER | 89 | 0 | 0 | 1 | 0 | 6.7 |
| 1000560593 | Arsenal FC | 2000001017 | Mikel Merino | SUB_USED | 1 | 0 | 0 | 0 | 0 | — |
| 1000560593 | Arsenal FC | 2000001012 | Viktor Gyökeres | SUB_USED | 28 | 0 | 0 | 0 | 0 | 6.6 |
| 1000560593 | Arsenal FC | 2000001010 | Jurriën Timber | SUB_USED | 28 | 0 | 0 | 0 | 0 | 6.8 |
| 1000560593 | Arsenal FC | 2000001015 | Noni Madueke | STARTER | 90 | 0 | 0 | 0 | 0 | 7.1 |
| 1000560593 | Arsenal FC | 2000001022 | Bruno Guimarães | SUB_USED | 28 | 1 | 0 | 0 | 0 | 8.3 |
| 1000560593 | Arsenal FC | 2000001024 | Myles Lewis-Skelly | STARTER | 62 | 0 | 0 | 0 | 0 | 7.1 |
| 1000560593 | Arsenal FC | 2000001021 | Martín Zubimendi | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560593 | Arsenal FC | 2000001020 | Riccardo Calafiori | STARTER | 81 | 1 | 0 | 0 | 0 | 6.7 |
| 1000560593 | Arsenal FC | 2000001005 | Piero Hincapié | SUB_USED | 9 | 0 | 0 | 0 | 0 | 6.9 |
| 1000560593 | Arsenal FC | 2000001025 | Max Dowman | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560593 | Leeds United FC | 2000020067 | Dominic Calvert-Lewin | STARTER | 90 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560593 | Leeds United FC | 2000020068 | Ethan Ampadu | STARTER | 90 | 0 | 0 | 1 | 0 | 7.4 |
| 1000560593 | Leeds United FC | 2000020066 | Harry Wilson | SUB_USED | 20 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560593 | Leeds United FC | 2000014004 | James Justin | STARTER | 78 | 1 | 0 | 1 | 0 | 7.0 |
| 1000560593 | Leeds United FC | 2000014002 | James Trafford | STARTER | 90 | 0 | 0 | 0 | 0 | 7.6 |
| 1000560593 | Leeds United FC | 2000020065 | Jayden Bogle | STARTER | 85 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560593 | Leeds United FC | 2000020084 | Lukas Nmecha | SUB_USED | 12 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560593 | Leeds United FC | 2000020072 | Sean Longstaff | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560593 | Leeds United FC | 2000030221 | Ilia Gruev | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560593 | Leeds United FC | 2000020069 | Brenden Aaronson | SUB_USED | 20 | 0 | 0 | 0 | 0 | 6.3 |
| 1000560593 | Leeds United FC | 2000014001 | Anton Stach | STARTER | 90 | 0 | 0 | 0 | 0 | 7.6 |
| 1000560593 | Leeds United FC | 2000030226 | Jean Mattéo Bahoya | SUB_USED | 5 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560593 | Leeds United FC | 2000030224 | Michael Zetterer | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560593 | Leeds United FC | 2000020080 | Nico Elvedi | STARTER | 85 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560593 | Leeds United FC | 2000014003 | Jaka Bijol | SUB_USED | 5 | 0 | 0 | 0 | 0 | 7.0 |
| 1000560593 | Leeds United FC | 2000020071 | Noah Okafor | STARTER | 70 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560593 | Leeds United FC | 2000030225 | Melvin Bard | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560593 | Leeds United FC | 2000030220 | Gabriel Gudmundsson | STARTER | 90 | 0 | 1 | 0 | 0 | 6.9 |
| 1000560593 | Leeds United FC | 2000020070 | Tarik Muharemović | STARTER | 90 | 0 | 0 | 1 | 0 | 6.9 |
| 1000560593 | Leeds United FC | 2000020082 | Ao Tanaka | STARTER | 70 | 0 | 0 | 1 | 0 | 6.2 |
| 1000560594 | Hull City AFC | 2000030205 | Joe Gelhardt | STARTER | 64 | 1 | 0 | 0 | 0 | 7.0 |
| 1000560594 | Hull City AFC | 2000010005 | John Egan | STARTER | 90 | 0 | 0 | 0 | 0 | 6.3 |
| 1000560594 | Hull City AFC | 2000020008 | Oliver McBurnie | STARTER | 90 | 0 | 0 | 1 | 0 | 6.3 |
| 1000560594 | Hull City AFC | 2000020011 | Paddy McNair | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560594 | Hull City AFC | 2000010004 | Regan Slater | STARTER | 74 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560594 | Hull City AFC | 2000020005 | Ryan Giles | STARTER | 63 | 0 | 0 | 0 | 0 | 6.2 |
| 1000560594 | Hull City AFC | 2000010002 | Semi Ajayi | STARTER | 90 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560594 | Hull City AFC | 284500 | Tim Iroegbunam | SUB_USED | 26 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560594 | Hull City AFC | 2000010003 | Nobel Mendy | STARTER | 90 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560594 | Hull City AFC | 2000030210 | Brooke Norton-Cuffy | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560594 | Hull City AFC | 2000030209 | Robinio Vaz | SUB_USED | 5 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560594 | Hull City AFC | 2000030211 | Sorba Thomas | STARTER | 85 | 0 | 1 | 0 | 0 | 7.2 |
| 1000560594 | Hull City AFC | 2000010001 | Kostas Tzolakis | STARTER | 90 | 0 | 0 | 0 | 0 | 6.6 |
| 1000560594 | Hull City AFC | 2000030206 | Hidemasa Morita | SUB_USED | 16 | 0 | 0 | 0 | 0 | 6.6 |
| 1000560594 | Hull City AFC | 2000020015 | Dillon Phillips | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560594 | Hull City AFC | 2000020004 | Lewie Coyle | STARTER | 90 | 0 | 0 | 0 | 0 | 6.1 |
| 1000560594 | Hull City AFC | 2000020007 | Matty Crooks | STARTER | 64 | 0 | 0 | 1 | 0 | 6.3 |
| 1000560594 | Hull City AFC | 2000020009 | Bachir Belloumi | SUB_USED | 26 | 1 | 0 | 0 | 0 | 7.9 |
| 1000560594 | Hull City AFC | 2000020006 | Elliot Stroud | SUB_USED | 27 | 0 | 1 | 0 | 0 | 6.7 |
| 1000560594 | Hull City AFC | 2000020010 | Lucas Herrington | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560594 | Everton FC | 2000030191 | Ainsley Maitland-Niles | STARTER | 77 | 0 | 1 | 0 | 0 | 6.6 |
| 1000560594 | Everton FC | 30407 | Christian Nørgaard | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560594 | Everton FC | 2000030029 | Jack Grealish | STARTER | 85 | 0 | 0 | 0 | 0 | 7.0 |
| 1000560594 | Everton FC | 2000020091 | James Garner | STARTER | 90 | 0 | 0 | 0 | 0 | 8.1 |
| 1000560594 | Everton FC | 2000020089 | James Tarkowski | STARTER | 90 | 0 | 0 | 0 | 0 | 8.8 |
| 1000560594 | Everton FC | 2000015005 | Jarrad Branthwaite | STARTER | 90 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560594 | Everton FC | 2000020085 | Jordan Pickford | STARTER | 90 | 0 | 0 | 0 | 0 | 6.0 |
| 1000560594 | Everton FC | 2000020109 | Mark Travers | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560594 | Everton FC | 2000020111 | Michael Keane | STARTER | 90 | 0 | 0 | 0 | 0 | 6.9 |
| 1000560594 | Everton FC | 2000020093 | Brennan Johnson | STARTER | 65 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560594 | Everton FC | 2000015001 | Kiernan Dewsbury Hall | STARTER | 90 | 1 | 1 | 0 | 0 | 8.4 |
| 1000560594 | Everton FC | 2000020094 | Carlos Alcaraz | SUB_USED | 5 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560594 | Everton FC | 2000020113 | Tyler Dibling | SUB_USED | 5 | 0 | 0 | 0 | 0 | 6.9 |
| 1000560594 | Everton FC | 2000020112 | Jake O'Brien | SUB_USED | 13 | 0 | 0 | 0 | 0 | 6.6 |
| 1000560594 | Everton FC | 2000020087 | Tyrique George | SUB_USED | 25 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560594 | Everton FC | 2000015004 | Harrison Armstrong | STARTER | 77 | 0 | 0 | 0 | 0 | 7.3 |
| 1000560594 | Everton FC | 2000015002 | Thierno Barry | STARTER | 85 | 1 | 0 | 0 | 0 | 7.9 |
| 1000560594 | Everton FC | 2000020090 | Merlin Röhl | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560594 | Everton FC | 2000030192 | Braiden Graham | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560594 | Everton FC | 2000020088 | Hayden Hackney | SUB_USED | 13 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560595 | Chelsea FC | 2000005009 | Cole Palmer | STARTER | 87 | 0 | 1 | 0 | 0 | 6.6 |
| 1000560595 | Chelsea FC | 2000005013 | Danny Welbeck | SUB_USED | 8 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560595 | Chelsea FC | 2000005001 | Emiliano Martínez | STARTER | 90 | 0 | 0 | 0 | 0 | 7.5 |
| 1000560595 | Chelsea FC | 2000005008 | João Pedro | STARTER | 71 | 2 | 0 | 0 | 0 | 8.5 |
| 1000560595 | Chelsea FC | 2000005011 | Jordan Henderson | STARTER | 90 | 2 | 1 | 0 | 0 | 9.4 |
| 1000560595 | Chelsea FC | 2000005018 | Moisés Caicedo | STARTER | 71 | 0 | 0 | 0 | 0 | 8.0 |
| 1000560595 | Chelsea FC | 2000005012 | Morgan Rogers | STARTER | 83 | 1 | 0 | 0 | 0 | 8.3 |
| 1000560595 | Chelsea FC | 2000005007 | Pedro Neto | STARTER | 82 | 0 | 1 | 0 | 0 | 6.8 |
| 1000560595 | Chelsea FC | 2000005025 | Roméo Lavia | SUB_USED | 19 | 0 | 0 | 0 | 0 | 6.9 |
| 1000560595 | Chelsea FC | 2000005003 | Wesley Fofana | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560595 | Chelsea FC | 2000005019 | Malo Gusto | SUB_USED | 3 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560595 | Chelsea FC | 2000005006 | Levi Colwill | STARTER | 90 | 0 | 0 | 0 | 0 | 7.3 |
| 1000560595 | Chelsea FC | 2000005022 | Joshua Acheampong | STARTER | 90 | 0 | 0 | 0 | 0 | 6.6 |
| 1000560595 | Chelsea FC | 2000005020 | Pep Chavarría | STARTER | 90 | 0 | 1 | 0 | 0 | 7.2 |
| 1000560595 | Chelsea FC | 2000005005 | Maxence Lacroix | STARTER | 90 | 0 | 0 | 0 | 0 | 6.8 |
| 1000560595 | Chelsea FC | 2000005002 | Marco Palestra | SUB_USED | 19 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560595 | Chelsea FC | 2000005014 | Jorrel Hato | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560595 | Chelsea FC | 2000005016 | Geovany Quenda | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560595 | Chelsea FC | 2000005023 | Mike Penders | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560595 | Chelsea FC | 2000005024 | Estevão | SUB_USED | 7 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560595 | AFC Bournemouth | 2000030108 | Adam Smith | STARTER | 71 | 0 | 0 | 0 | 0 | 5.2 |
| 1000560595 | AFC Bournemouth | 2000030040 | Daniel Jebbison | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560595 | AFC Bournemouth | 2000030115 | David Brooks | STARTER | 45 | 0 | 0 | 0 | 0 | 6.3 |
| 1000560595 | AFC Bournemouth | 2000030037 | Lewis Cook | SUB_USED | 18 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560595 | AFC Bournemouth | 2000030031 | Marcus Tavernier | STARTER | 72 | 0 | 0 | 1 | 0 | 5.6 |
| 1000560595 | AFC Bournemouth | 1125 | Ryan Christie | STARTER | 72 | 0 | 0 | 1 | 0 | 7.3 |
| 1000560595 | AFC Bournemouth | 2000030036 | Tyler Adams | STARTER | 90 | 0 | 0 | 1 | 0 | 5.9 |
| 1000560595 | AFC Bournemouth | 2000030030 | Đorđe Petrović | STARTER | 90 | 0 | 0 | 0 | 0 | 4.8 |
| 1000560595 | AFC Bournemouth | 2000030034 | Ben Gannon-Doak | SUB_USED | 45 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560595 | AFC Bournemouth | 2000030109 | James Hill | STARTER | 90 | 0 | 0 | 0 | 0 | 6.2 |
| 1000560595 | AFC Bournemouth | 2000030114 | Juanlu | SUB_USED | 19 | 0 | 0 | 0 | 0 | 6.6 |
| 1000560595 | AFC Bournemouth | 2000030116 | Álvaro Rodríguez | SUB_USED | 18 | 0 | 0 | 0 | 0 | 6.2 |
| 1000560595 | AFC Bournemouth | 2000030032 | Evanilson | STARTER | 90 | 1 | 0 | 0 | 0 | 7.7 |
| 1000560595 | AFC Bournemouth | 2000030154 | Julio Soler | SUB_USED | 7 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560595 | AFC Bournemouth | 2000030157 | Michele Di Gregorio | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560595 | AFC Bournemouth | 2000030113 | Bafodé Diakité | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560595 | AFC Bournemouth | 2000030111 | Adrien Truffert | STARTER | 83 | 0 | 0 | 1 | 0 | 6.1 |
| 1000560595 | AFC Bournemouth | 2000030035 | António Silva | STARTER | 90 | 0 | 0 | 0 | 0 | 5.5 |
| 1000560595 | AFC Bournemouth | 2000030038 | Alex Tóth | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560595 | AFC Bournemouth | 2000030033 | Rayan | STARTER | 90 | 0 | 0 | 0 | 0 | 6.6 |
| 1000560596 | Crystal Palace FC | 2000020097 | Dwight McNeil | SUB_USED | 1 | 0 | 0 | 0 | 0 | — |
| 1000560596 | Crystal Palace FC | 2000020101 | Eddie Nketiah | SUB_USED | 8 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560596 | Crystal Palace FC | 2218 | Ismaïla Sarr | SUB_USED | 8 | 0 | 0 | 0 | 0 | 6.2 |
| 1000560596 | Crystal Palace FC | 2490 | Jefferson Lerma | STARTER | 71 | 1 | 0 | 1 | 0 | 7.4 |
| 1000560596 | Crystal Palace FC | 2000030184 | Remi Matthews | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560596 | Crystal Palace FC | 2000020103 | Tyrick Mitchell | STARTER | 90 | 0 | 0 | 0 | 0 | 7.8 |
| 1000560596 | Crystal Palace FC | 2000020117 | Will Hughes | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560596 | Crystal Palace FC | 2000020099 | Chris Richards | STARTER | 90 | 0 | 0 | 0 | 0 | 7.3 |
| 1000560596 | Crystal Palace FC | 3339 | Cheick Doucouré | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560596 | Crystal Palace FC | 2000020098 | Adam Wharton | STARTER | 90 | 0 | 1 | 0 | 0 | 8.1 |
| 1000560596 | Crystal Palace FC | 2000020106 | Jørgen Strand Larsen | STARTER | 82 | 0 | 0 | 0 | 0 | 7.2 |
| 1000560596 | Crystal Palace FC | 2000020105 | Yeremi Pino | STARTER | 82 | 1 | 0 | 1 | 0 | 7.5 |
| 1000560596 | Crystal Palace FC | 2000020116 | Óscar Mingueza | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560596 | Crystal Palace FC | 2000016001 | Daichi Kamada | STARTER | 89 | 1 | 0 | 0 | 0 | 7.2 |
| 1000560596 | Crystal Palace FC | 2000030186 | Honest Ahanor | SUB_USED | 19 | 0 | 0 | 0 | 0 | 6.9 |
| 1000560596 | Crystal Palace FC | 2000020114 | Walter Benítez | STARTER | 90 | 0 | 0 | 0 | 0 | 8.1 |
| 1000560596 | Crystal Palace FC | 2000020107 | Evann Guessand | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560596 | Crystal Palace FC | 2000020096 | Jaydee Canvot | STARTER | 90 | 0 | 0 | 0 | 0 | 7.2 |
| 1000560596 | Crystal Palace FC | 2000030188 | Quinten Timber | STARTER | 90 | 0 | 1 | 0 | 0 | 6.9 |
| 1000560596 | Crystal Palace FC | 2000016002 | Anan Khalaili | STARTER | 90 | 0 | 0 | 0 | 0 | 6.3 |
| 1000560596 | Nottingham Forest FC | 2000020077 | Callum Hudson-Odoi | SUB_USED | 19 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560596 | Nottingham Forest FC | 2000020061 | Chris Wood | SUB_USED | 19 | 0 | 0 | 0 | 0 | 6.3 |
| 1000560596 | Nottingham Forest FC | 2000013001 | James Mcatee | STARTER | 90 | 0 | 0 | 0 | 0 | 6.2 |
| 1000560596 | Nottingham Forest FC | 161948 | Liam Delap | STARTER | 71 | 0 | 0 | 0 | 0 | 5.8 |
| 1000560596 | Nottingham Forest FC | 2000020052 | Matz Sels | STARTER | 90 | 0 | 0 | 0 | 0 | 7.7 |
| 1000560596 | Nottingham Forest FC | 2000020055 | Morgan Gibbs-White | STARTER | 90 | 0 | 0 | 0 | 0 | 7.6 |
| 1000560596 | Nottingham Forest FC | 2000020053 | Neco Williams | STARTER | 90 | 0 | 0 | 0 | 0 | 7.2 |
| 1000560596 | Nottingham Forest FC | 2000020060 | Ola Aina | STARTER | 82 | 0 | 0 | 1 | 0 | 6.5 |
| 1000560596 | Nottingham Forest FC | 19305 | Ryan Yates | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560596 | Nottingham Forest FC | 2000013002 | Murillo | STARTER | 90 | 0 | 0 | 1 | 0 | 6.6 |
| 1000560596 | Nottingham Forest FC | 2000020076 | Nicolás Domínguez | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560596 | Nottingham Forest FC | 2000020054 | Ibrahim Sangaré | SUB_USED | 25 | 0 | 0 | 0 | 0 | 6.9 |
| 1000560596 | Nottingham Forest FC | 2000020095 | Daniel Muñoz | STARTER | 90 | 0 | 0 | 0 | 0 | 6.6 |
| 1000560596 | Nottingham Forest FC | 2000020063 | Xaver Schlager | STARTER | 65 | 0 | 0 | 0 | 0 | 6.9 |
| 1000560596 | Nottingham Forest FC | 2000020062 | Luca Netz | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560596 | Nottingham Forest FC | 2000020058 | Dan Ndoye | STARTER | 71 | 1 | 0 | 0 | 0 | 8.2 |
| 1000560596 | Nottingham Forest FC | 2000020064 | Arnaud Kalimuendo | SUB_USED | 8 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560596 | Nottingham Forest FC | 2000020074 | Ousmane Diomandé | STARTER | 90 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560596 | Nottingham Forest FC | 2000020059 | Jair | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560596 | Nottingham Forest FC | 2000020073 | John | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560598 | Liverpool FC | 2000003010 | Alexis Mac Allister | STARTER | 90 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560598 | Liverpool FC | 2000003001 | Alisson | STARTER | 90 | 0 | 0 | 0 | 0 | 5.4 |
| 1000560598 | Liverpool FC | 2000003002 | Joe Gomez | SUB_USED | 3 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560598 | Liverpool FC | 2000003012 | Kostas Tsimikas | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560598 | Liverpool FC | 2000003004 | Virgil van Dijk | STARTER | 90 | 0 | 0 | 1 | 0 | 6.2 |
| 1000560598 | Liverpool FC | 2000003006 | Milos Kerkez | STARTER | 90 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560598 | Liverpool FC | 2000003020 | Ryan Gravenberch | STARTER | 61 | 0 | 0 | 2 | 1 | 6.5 |
| 1000560598 | Liverpool FC | 2000003003 | Wataru Endō | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560598 | Liverpool FC | 2000003022 | James McConnell | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560598 | Liverpool FC | 2000003021 | Trey Nyoni | SUB_USED | 25 | 0 | 0 | 0 | 0 | 6.2 |
| 1000560598 | Liverpool FC | 2000003023 | Lewis Koumas | STARTER | 74 | 0 | 0 | 0 | 0 | 6.3 |
| 1000560598 | Liverpool FC | 2000003015 | Giorgi Mamardashvili | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560598 | Liverpool FC | 2000003019 | Ronald Araújo | STARTER | 74 | 0 | 0 | 1 | 0 | 6.3 |
| 1000560598 | Liverpool FC | 2000030231 | Federico Chiesa | SUB_USED | 3 | 0 | 0 | 0 | 0 | 6.3 |
| 1000560598 | Liverpool FC | 2000003014 | Víctor Muñoz | STARTER | 90 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560598 | Liverpool FC | 2000003024 | Rio Ngumoha | SUB_USED | 16 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560598 | Liverpool FC | 2000003007 | Florian Wirtz | STARTER | 65 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560598 | Liverpool FC | 2000003018 | Jeremie Frimpong | SUB_USED | 16 | 0 | 0 | 0 | 0 | 6.1 |
| 1000560598 | Liverpool FC | 2000003005 | Jérémy Jacquet | STARTER | 87 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560598 | Liverpool FC | 2000003017 | Bradley Barcola | STARTER | 87 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560598 | Manchester City FC | 2000004003 | Elliot Anderson | STARTER | 90 | 0 | 1 | 0 | 0 | 8.1 |
| 1000560598 | Manchester City FC | 2000004016 | Géronimo Rulli | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560598 | Manchester City FC | 2000004005 | Iliman Ndiaye | STARTER | 55 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560598 | Manchester City FC | 2000004004 | Marc Guéhi | STARTER | 90 | 0 | 0 | 0 | 0 | 7.3 |
| 1000560598 | Manchester City FC | 2000004006 | Mateo Kovačić | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560598 | Manchester City FC | 2000004002 | Rúben Dias | STARTER | 90 | 0 | 0 | 0 | 0 | 7.3 |
| 1000560598 | Manchester City FC | 2000004020 | Antoine Semenyo | STARTER | 90 | 0 | 0 | 0 | 0 | 7.2 |
| 1000560598 | Manchester City FC | 2000004011 | Enzo Fernández | STARTER | 79 | 0 | 0 | 0 | 0 | 7.1 |
| 1000560598 | Manchester City FC | 2000004014 | Joško Gvardiol | SUB_USED | 25 | 0 | 1 | 0 | 0 | 7.0 |
| 1000560598 | Manchester City FC | 2000004025 | Rico Lewis | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560598 | Manchester City FC | 2000004015 | Matheus Nunes | STARTER | 90 | 0 | 0 | 0 | 0 | 7.5 |
| 1000560598 | Manchester City FC | 2000004007 | Erling Haaland | STARTER | 79 | 1 | 0 | 0 | 0 | 7.6 |
| 1000560598 | Manchester City FC | 2000004009 | Jérémy Doku | SUB_USED | 35 | 1 | 0 | 0 | 0 | 8.7 |
| 1000560598 | Manchester City FC | 2000004018 | Nico O’Reilly | STARTER | 65 | 0 | 0 | 1 | 0 | 6.6 |
| 1000560598 | Manchester City FC | 2000004021 | Abdukodir Khusanov | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560598 | Manchester City FC | 2000004013 | Vitor Reis | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560598 | Manchester City FC | 2000004001 | Gianluigi Donnarumma | STARTER | 90 | 0 | 0 | 0 | 0 | 8.4 |
| 1000560598 | Manchester City FC | 2000004008 | Rayan Cherki | STARTER | 90 | 1 | 0 | 0 | 0 | 8.6 |
| 1000560598 | Manchester City FC | 2000004017 | Ayyoub Bouaddi | SUB_USED | 11 | 0 | 0 | 1 | 0 | 6.6 |
| 1000560598 | Manchester City FC | 2000004019 | Allan | SUB_USED | 11 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560599 | Ipswich Town FC | 2000020044 | Christian Walton | STARTER | 90 | 0 | 0 | 1 | 0 | 6.7 |
| 1000560599 | Ipswich Town FC | 2000020027 | Chuba Akpom | SUB_USED | 20 | 0 | 0 | 0 | 0 | 7.2 |
| 1000560599 | Ipswich Town FC | 2000020023 | Dara O'Shea | STARTER | 90 | 1 | 0 | 0 | 0 | 7.8 |
| 1000560599 | Ipswich Town FC | 2000020045 | Darnell Furlong | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560599 | Ipswich Town FC | 2000020022 | Issa Diop | STARTER | 90 | 0 | 0 | 0 | 0 | 7.7 |
| 1000560599 | Ipswich Town FC | 2000011003 | Jack Clarke | SUB_USED | 28 | 0 | 1 | 0 | 0 | 6.8 |
| 1000560599 | Ipswich Town FC | 138931 | Jaden Philogene | STARTER | 62 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560599 | Ipswich Town FC | 2000020019 | Kjell Scherpen | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560599 | Ipswich Town FC | 2000020020 | Leif Davis | STARTER | 90 | 0 | 0 | 1 | 0 | 6.5 |
| 1000560599 | Ipswich Town FC | 2000011002 | Julio César Enciso | STARTER | 70 | 0 | 0 | 0 | 0 | 7.2 |
| 1000560599 | Ipswich Town FC | 2000011004 | Saša Lukić | STARTER | 90 | 0 | 0 | 0 | 0 | 7.1 |
| 1000560599 | Ipswich Town FC | 2000030217 | Florentino | STARTER | 62 | 0 | 0 | 0 | 0 | 7.1 |
| 1000560599 | Ipswich Town FC | 2000020021 | Jacob Greaves | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560599 | Ipswich Town FC | 2000030218 | Exequiel Palacios | SUB_USED | 28 | 0 | 0 | 0 | 0 | 7.1 |
| 1000560599 | Ipswich Town FC | 2000020028 | Guimissongui Ouattara | STARTER | 90 | 1 | 0 | 0 | 0 | 7.8 |
| 1000560599 | Ipswich Town FC | 2000030215 | Azor Matusiwa | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560599 | Ipswich Town FC | 2000020024 | Daizen Maeda | STARTER | 80 | 0 | 0 | 0 | 0 | 6.3 |
| 1000560599 | Ipswich Town FC | 2000030219 | Zian Flemming | SUB_USED | 20 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560599 | Ipswich Town FC | 2000011001 | Emersonn | STARTER | 70 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560599 | Ipswich Town FC | 2000020029 | Sindre Egeli | SUB_USED | 10 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560599 | Fulham FC | 2000030136 | Alex Iwobi | STARTER | 86 | 0 | 0 | 0 | 0 | 7.6 |
| 1000560599 | Fulham FC | 2000030138 | Bernd Leno | STARTER | 90 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560599 | Fulham FC | 2000030142 | Emile Smith Rowe | SUB_USED | 4 | 0 | 0 | 1 | 0 | 6.3 |
| 1000560599 | Fulham FC | 2729 | Joachim Andersen | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560599 | Fulham FC | 2000030140 | Kenny Tete | STARTER | 90 | 0 | 0 | 0 | 0 | 7.0 |
| 1000560599 | Fulham FC | 2000030088 | Ryan Sessegnon | STARTER | 90 | 1 | 0 | 0 | 0 | 7.0 |
| 1000560599 | Fulham FC | 2000030089 | Sander Berge | STARTER | 90 | 0 | 0 | 0 | 0 | 7.8 |
| 1000560599 | Fulham FC | 2000030084 | Timothy Castagne | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560599 | Fulham FC | 2000030087 | Calvin Bassey | STARTER | 90 | 0 | 0 | 1 | 0 | 6.8 |
| 1000560599 | Fulham FC | 2000030085 | Rodrigo Muniz | SUB_USED | 15 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560599 | Fulham FC | 2000030093 | Oscar Bobb | STARTER | 75 | 0 | 0 | 0 | 0 | 6.6 |
| 1000560599 | Fulham FC | 2000030092 | Shea Charles | STARTER | 70 | 0 | 0 | 0 | 0 | 6.8 |
| 1000560599 | Fulham FC | 2000030083 | Joshua King | STARTER | 75 | 0 | 0 | 1 | 0 | 7.0 |
| 1000560599 | Fulham FC | 2000030082 | Gonzalo García | STARTER | 86 | 0 | 0 | 0 | 0 | 5.8 |
| 1000560599 | Fulham FC | 2000030139 | Benjamin Lecomte | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560599 | Fulham FC | 2000030195 | Hugo Larsson | SUB_USED | 4 | 0 | 0 | 0 | 0 | 6.3 |
| 1000560599 | Fulham FC | 2000030197 | David Affengruber | STARTER | 90 | 0 | 0 | 0 | 0 | 5.8 |
| 1000560599 | Fulham FC | 2000030090 | Kevin | SUB_USED | 15 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560599 | Fulham FC | 2000030091 | César Palacios | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560599 | Fulham FC | 2000030196 | Manuel Ángel | SUB_USED | 20 | 0 | 0 | 1 | 0 | 6.3 |
| 1000560600 | Manchester United FC | 2000006015 | Amad Diallo | STARTER | 58 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560600 | Manchester United FC | 2000006008 | Bruno Fernandes | STARTER | 90 | 0 | 0 | 1 | 0 | 7.1 |
| 1000560600 | Manchester United FC | 2000006018 | Bryan Mbeumo | STARTER | 90 | 1 | 0 | 0 | 0 | 8.5 |
| 1000560600 | Manchester United FC | 2000006002 | Diogo Dalot | SUB_USED | 32 | 0 | 0 | 1 | 0 | 6.7 |
| 1000560600 | Manchester United FC | 2000006005 | Harry Maguire | STARTER | 90 | 0 | 0 | 0 | 0 | 6.8 |
| 1000560600 | Manchester United FC | 2000006012 | Karl Darlow | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560600 | Manchester United FC | 2000006021 | Luke Shaw | STARTER | 89 | 0 | 0 | 1 | 0 | 6.5 |
| 1000560600 | Manchester United FC | 2000006009 | Marcus Rashford | SUB_USED | 1 | 0 | 0 | 0 | 0 | — |
| 1000560600 | Manchester United FC | 2000006007 | Mason Mount | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560600 | Manchester United FC | 2000006017 | Youri Tielemans | STARTER | 58 | 0 | 0 | 0 | 0 | 7.5 |
| 1000560600 | Manchester United FC | 2000006019 | Carlos Baleba | SUB_USED | 32 | 0 | 0 | 0 | 0 | 6.8 |
| 1000560600 | Manchester United FC | 2000006006 | Lisandro Martínez | STARTER | 90 | 0 | 0 | 1 | 0 | 7.4 |
| 1000560600 | Manchester United FC | 2000006016 | Andrey Santos | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560600 | Manchester United FC | 2000006010 | Matheus Cunha | STARTER | 89 | 0 | 1 | 0 | 0 | 6.9 |
| 1000560600 | Manchester United FC | 2000006025 | Kobbie Mainoo | STARTER | 90 | 0 | 0 | 0 | 0 | 7.1 |
| 1000560600 | Manchester United FC | 2000006023 | Ayden Heaven | SUB_USED | 1 | 0 | 0 | 0 | 0 | — |
| 1000560600 | Manchester United FC | 2000006011 | Joshua Zirkzee | SUB_USED | 32 | 0 | 0 | 0 | 0 | 7.0 |
| 1000560600 | Manchester United FC | 2000006014 | Leny Yoro | STARTER | 58 | 0 | 0 | 0 | 0 | 7.1 |
| 1000560600 | Manchester United FC | 2000006001 | Senne Lammens | STARTER | 90 | 0 | 0 | 0 | 0 | 6.8 |
| 1000560600 | Manchester United FC | 2000006026 | Shea Lacey | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560600 | Tottenham Hotspur FC | 2000030104 | Ben Davies | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560600 | Tottenham Hotspur FC | 2000030025 | Conor Gallagher | STARTER | 73 | 0 | 0 | 1 | 0 | 6.3 |
| 1000560600 | Tottenham Hotspur FC | 2000030018 | Dominic Solanke | STARTER | 90 | 1 | 0 | 0 | 0 | 7.2 |
| 1000560600 | Tottenham Hotspur FC | 2000030020 | James Maddison | SUB_USED | 17 | 0 | 0 | 0 | 0 | 7.0 |
| 1000560600 | Tottenham Hotspur FC | 2000030028 | Jan Paul van Hecke | STARTER | 90 | 0 | 0 | 1 | 0 | 6.6 |
| 1000560600 | Tottenham Hotspur FC | 2000030102 | Martin Dúbravka | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560600 | Tottenham Hotspur FC | 19145 | Tosin Adarabioyo | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560600 | Tottenham Hotspur FC | 2000030026 | Marcos Senesi | STARTER | 88 | 0 | 0 | 0 | 0 | 6.8 |
| 1000560600 | Tottenham Hotspur FC | 2000030100 | Sandro Tonali | STARTER | 90 | 0 | 1 | 0 | 0 | 7.6 |
| 1000560600 | Tottenham Hotspur FC | 2000030016 | Destiny Udogie | STARTER | 73 | 0 | 0 | 0 | 0 | 7.3 |
| 1000560600 | Tottenham Hotspur FC | 152849 | Micky van de Ven | SUB_USED | 17 | 0 | 0 | 0 | 0 | 6.6 |
| 1000560600 | Tottenham Hotspur FC | 2000030023 | Rodrigo Bentancur | SUB_USED | 12 | 0 | 0 | 0 | 1 | 4.6 |
| 1000560600 | Tottenham Hotspur FC | 15911 | Mohammed Kudus | STARTER | 65 | 0 | 0 | 0 | 0 | 6.2 |
| 1000560600 | Tottenham Hotspur FC | 2000030017 | Archie Gray | STARTER | 90 | 0 | 0 | 1 | 0 | 6.8 |
| 1000560600 | Tottenham Hotspur FC | 2000030246 | Sávio | STARTER | 90 | 0 | 0 | 0 | 0 | 7.0 |
| 1000560600 | Tottenham Hotspur FC | 2000030022 | Lucas Bergvall | STARTER | 65 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560600 | Tottenham Hotspur FC | 2000030024 | Mateus Fernandes | SUB_USED | 2 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560600 | Tottenham Hotspur FC | 2000030015 | Antonín Kinský | STARTER | 90 | 0 | 0 | 0 | 0 | 6.3 |
| 1000560600 | Tottenham Hotspur FC | 2000030107 | Omar Marmoush | SUB_USED | 25 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560600 | Tottenham Hotspur FC | 2000030101 | Mathys Tel | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560601 | Aston Villa FC | 18846 | Aaron Wan-Bissaka | SUB_USED | 45 | 0 | 0 | 0 | 0 | 6.6 |
| 1000560601 | Aston Villa FC | 2000030061 | Emiliano Buendía | STARTER | 63 | 0 | 0 | 1 | 0 | 6.3 |
| 1000560601 | Aston Villa FC | 2000030059 | John McGinn | STARTER | 90 | 1 | 0 | 0 | 0 | 8.1 |
| 1000560601 | Aston Villa FC | 2000030058 | Matty Cash | STARTER | 90 | 0 | 0 | 1 | 0 | 6.7 |
| 1000560601 | Aston Villa FC | 2000030068 | Ross Barkley | SUB_USED | 5 | 0 | 0 | 0 | 0 | 6.7 |
| 1000560601 | Aston Villa FC | 2000030147 | Tammy Abraham | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560601 | Aston Villa FC | 2000030065 | Tyrone Mings | STARTER | 90 | 0 | 0 | 0 | 0 | 5.9 |
| 1000560601 | Aston Villa FC | 2000030056 | Victor Nilsson-Lindelöf | STARTER | 90 | 0 | 0 | 0 | 0 | 6.6 |
| 1000560601 | Aston Villa FC | 2000030122 | Boubacar Kamara | STARTER | 90 | 0 | 0 | 0 | 0 | 7.2 |
| 1000560601 | Aston Villa FC | 2000030066 | Pau Torres | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560601 | Aston Villa FC | 283058 | Nicolas Jackson | STARTER | 90 | 1 | 0 | 0 | 0 | 8.1 |
| 1000560601 | Aston Villa FC | 2000030060 | Alejandro Garnacho | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560601 | Aston Villa FC | 2000030057 | João Gomes | STARTER | 85 | 0 | 0 | 0 | 0 | 6.9 |
| 1000560601 | Aston Villa FC | 2000030067 | Lamare Bogarde | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560601 | Aston Villa FC | 2000030146 | Johan Manzambi | STARTER | 85 | 0 | 1 | 0 | 0 | 7.4 |
| 1000560601 | Aston Villa FC | 2000030063 | Matteo Ruggeri | STARTER | 45 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560601 | Aston Villa FC | 2000030124 | Zion Suzuki | STARTER | 90 | 0 | 0 | 0 | 0 | 5.8 |
| 1000560601 | Aston Villa FC | 2000030069 | Marco Bizot | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560601 | Aston Villa FC | 2000030150 | Ibrahim Mbaye | SUB_USED | 27 | 0 | 0 | 0 | 0 | 6.9 |
| 1000560601 | Aston Villa FC | 2000030062 | Alysson | SUB_USED | 5 | 0 | 0 | 0 | 0 | 6.4 |
| 1000560601 | Brentford FC | 2000030007 | Callum Wilson | SUB_USED | 6 | 0 | 0 | 1 | 0 | 6.4 |
| 1000560601 | Brentford FC | 2000030001 | Caoimhin Kelleher | STARTER | 90 | 0 | 0 | 0 | 0 | 7.4 |
| 1000560601 | Brentford FC | 2000030095 | Kristoffer Ajer | STARTER | 90 | 1 | 0 | 1 | 0 | 7.9 |
| 1000560601 | Brentford FC | 2000030013 | Mathias Jensen | SUB_USED | 24 | 0 | 0 | 0 | 0 | 6.8 |
| 1000560601 | Brentford FC | 2000030158 | Sepp van den Berg | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560601 | Brentford FC | 2000030003 | Vitaly Janelt | STARTER | 90 | 0 | 0 | 1 | 0 | 6.9 |
| 1000560601 | Brentford FC | 2000030012 | Dango Ouattara | SUB_USED | 24 | 0 | 0 | 0 | 0 | 6.8 |
| 1000560601 | Brentford FC | 2000030008 | Aaron Hickey | SUB_USED | 45 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560601 | Brentford FC | 2000030011 | Yehor Yarmoliuk | STARTER | 66 | 1 | 0 | 0 | 0 | 7.5 |
| 1000560601 | Brentford FC | 2000030094 | Kevin Schade | STARTER | 51 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560601 | Brentford FC | 2000030002 | Keane Lewis-Potter | STARTER | 66 | 0 | 0 | 0 | 0 | 6.6 |
| 1000560601 | Brentford FC | 2000030010 | Jaidon Anthony | STARTER | 84 | 0 | 1 | 0 | 0 | 6.6 |
| 1000560601 | Brentford FC | 153066 | Fabio Carvalho | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560601 | Brentford FC | 2000030165 | Benjamin Fredrick | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560601 | Brentford FC | 2000030096 | Hákon Rafn Valdimarsson | SUB_UNUSED | 0 | 0 | 0 | 0 | 0 | — |
| 1000560601 | Brentford FC | 2000030006 | Igor Thiago | STARTER | 90 | 0 | 0 | 1 | 0 | 6.9 |
| 1000560601 | Brentford FC | 2000030004 | Michael Kayode | STARTER | 90 | 0 | 0 | 1 | 0 | 6.7 |
| 1000560601 | Brentford FC | 2000030099 | Jannik Schuster | STARTER | 45 | 0 | 0 | 0 | 0 | 6.5 |
| 1000560601 | Brentford FC | 2000030163 | El Hadji Malick Diouf | STARTER | 90 | 0 | 0 | 0 | 0 | 5.8 |
| 1000560601 | Brentford FC | 2000030005 | Mamadou Sangaré | SUB_USED | 39 | 0 | 0 | 0 | 0 | 6.7 |

## Hồ sơ kiểm tra trước nhập thống kê — lưu lịch sử

- File dữ liệu và tài liệu GW6 đã được bổ sung hai file membership hiện hành. Không sửa mã/schema/cấu hình. Production chỉ thêm 22 khoảng vào manual_player_memberships; thống kê, tổng mùa, vị trí/profile, ID, fixture và snapshot Fantasy giữ nguyên. Không commit, push hoặc deploy.
- SHA-256 CSV: `5fa0d29878b299c0c5c6e7f5e066bdc832e2b7ff5798d4bd72fbf7b06c2c1c87`. Bản trước ảnh ở cache ignored để đối chiếu: 279 ô rating được bổ sung; toàn bộ chín cột khác, thứ tự 360 dòng và khóa fixture/player giữ nguyên. Không cập nhật phút/bàn/kiến tạo/thẻ từ ảnh.
- Reader chung: biên dịch riêng `ManualMatchStatsCsvReader` bằng `javac --release 21`, chạy JShell không khởi động Spring/database. Kết quả sau ảnh: `READER rows=360 played=283 did_not_play=77`, `MISSING played_metrics=0 played_rating=4`. Không chạy full test/build hoặc importer.
- Đối chiếu bộ đội hình bằng file: 18 nhóm CLB/trận, mỗi nhóm 11 STARTER, ID và participation khớp CSV thống kê, 85 SUB_USED có người ra/phút thay ở ảnh, 77 SUB_UNUSED không có phút vào/ra. Cùng ngày/nguồn mỗi nhóm; không có vị trí/tọa độ tự suy. Header ba file khớp `LineupBatchImporter`; 18 default USER kế thừa nguyên dòng đã chốt, summary không có observations khớp `formations.csv` chỉ header. Đây là kiểm tra file độc lập; chưa chạy validator importer với database.
- `missing.csv`: **10 dòng**, gồm 9 fixture_status_not_synced và 1 fixture_not_finished. Không còn membership_missing hoặc rating chờ. Đã bỏ yêu cầu URL theo người dùng; giới hạn validator nhập sơ đồ từ ảnh vẫn được ghi riêng. Chưa đồng bộ trạng thái fixture hoặc nhập thống kê GW6.
- Trước nhập phải đối chiếu default/summary/observations hiện hành để không ghi đè lịch sử mẫu; các metadata GW1–5 ở `clubs.csv` là bản chuẩn bị kế thừa, chưa được preflight lại trên database sau lượt này. Nhập đội hình/thống kê local/production, idempotence thống kê, API thống kê GW6, tổng mùa và Fantasy: chưa. Membership đã nhập và kiểm tra riêng; không đồng nghĩa thống kê GW6 đã nhập.
- Các file dữ liệu/tài liệu trong thư mục GW6 đang chưa commit, gồm CSV thống kê, sources/missing, confirmed-unrated, ba CSV lineups và hai file membership hiện hành. Người dùng yêu cầu không commit/push; đã giữ nguyên yêu cầu này.

### Kiểm tra sau xác nhận không chấm

- Sidecar đúng một trong các header của `FantasyEvidenceImporter`: `fixture_id,player_id,name,minutes,rating,fantasy_points`. Bốn khóa duy nhất khớp chính xác toàn bộ PLAYED có rating trống; minutes=1 khớp CSV cuối; rating/fantasy_points trống. Không chạy importer hoặc sửa thống kê.
- CSV thống kê giữ nguyên SHA-256 đã kiểm tra bằng reader ở lượt ảnh trước; reader vẫn báo bốn rating trống về mặt dữ liệu, nay cả bốn đã có xác nhận ngoại lệ, không còn là rating chưa thu thập. 279 rating + 4 ngoại lệ = 283 PLAYED. Nhập thống kê production, tổng mùa và công bố Fantasy vẫn chưa thực hiện; chỉ membership được nhập riêng theo quyền người dùng cho phép.

## Checkpoint membership hiện hành — production 12/10/2026

- Đã xử lý đủ 22/22; local importer lần đầu intervalsInserted=22 rồi lần hai mọi thay đổi=0. API local trước sửa: roster/detail không có 22 người nhưng vẫn trả 100 dòng GW1–5, xác nhận lịch sử dùng ngày trận; sau sửa danh sách/detail đủ 22, lịch sử không đổi; asOf=2026-11-01 vẫn đủ 22.
- Aiven nhập 22 khoảng mở, intervalsUpdated=0, playersInserted=0, membershipsInserted=0; nhập lại tất cả=0. API Render danh sách và chi tiết: 22/22 xuất hiện đúng CLB; 100 dòng lịch sử GW1–5 của cả 22 người giữ nguyên từng payload so với trước sửa. Không sửa code API vì cơ chế lịch sử hiện có đã xét membership theo fixtures.match_date.
- SQL đọc lại: 360 dòng CSV GW6 đều có đúng một membership hiệu lực tại ngày trận trong hai CLB fixture; 0 chồng lấn; 100 khoảng cũ, thống kê GW1–5, tổng mùa, profile/vị trí/ID và snapshot Fantasy GW6 giữ nguyên. GW6 trong manual_fixture_player_stats vẫn 0 dòng. 279 rating và 4 PLAYED không được chấm trong CSV/sidecar giữ nguyên.
- Backup ba bảng importer có thể tác động nằm ngoài Git; đường dẫn/dung lượng/SHA-256 và phạm vi ở memberships-current-sources.md. Chưa nhập thống kê GW6, cập nhật fixture, chấm/công bố Fantasy hoặc tổng mùa; trận cuối và các trạng thái fixture vẫn chờ theo checkpoint thu thập.

## Hồ sơ nhập 9 trận — production 12/10/2026

- Backup mới: `backend/local-backups/gw6-nine-fixtures-20261012-135659/aiven-before-gw6-nine-fixtures-20261012-135659.sql`; **687,551 byte**, SHA-256 `1f8f4d8160ee496416286b988b41c4eb9a5f75ca3b9853a042d0014a85a88adc`. Ngoài Git, dump completed, đủ DDL 10 bảng và số INSERT từng bảng khớp COUNT(*) trước nhập. Chưa thử restore. Các bảng: `seasons`, `fixtures`, `football_data_fixtures`, `standings`, `manual_fixture_player_stats`, `player_season_stats`, `manual_player_memberships`, `club_season_formations`, `fixture_lineups`, `fixture_lineup_players`.
- SHA CSV thống kê giữ nguyên: `5fa0d29878b299c0c5c6e7f5e066bdc832e2b7ff5798d4bd72fbf7b06c2c1c87`. Không đổi các CSV thống kê/lineups/membership/unrated.
- API tại `https://premierhub.vercel.app`: 360 lịch sử cầu thủ, 360 dòng GW6, 1541 dòng GW1–5 có thống kê thuộc 360 người trong batch, 9 chi tiết trận, 18 đội hình hiện MISSING, 20 dòng BXH.
- Bản nguồn football-data.org, baseline SQL/API, local/production logs, đối chiếu MySQL/API và checksum backup ở `backend/target/gw6-production/`, được ignore. Helpers dùng lại lớp đã biên dịch trong `backend/target/classes`; không sửa mã ứng dụng hoặc tạo test Java trong src/test.
- File cần commit là toàn bộ 9 file dữ liệu/tài liệu trong `backend/data/gw6-2026/`; không đưa target, backup hoặc secrets vào Git. Gợi ý: `data(gw6): record nine imported fixtures and verified memberships`. Chưa commit/push/deploy.

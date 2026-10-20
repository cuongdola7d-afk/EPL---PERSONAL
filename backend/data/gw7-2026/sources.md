# GW7 Premier League 2026/27 — checkpoint 2026-10-21

Trạng thái hiện hành: đã nhập Aiven đủ 10 fixture FINISHED, 400 thống kê, 20 đội hình/220 đá chính và bằng chứng Fantasy sau backup mới. Tổng mùa đã cập nhật qua GW7 và chạy lặp updatedRows=0. API công khai, 10 sân desktop/mobile và Đội hình tiêu biểu GW7 COMPLETE/93,80 đã đối chiếu. Quyết định 21/10/2026 mặc định 0 điểm khi danh sách cuối xác minh cầu thủ không được đăng ký, bất kể lý do vắng. Đã nhập căn cứ NOT_REGISTERED của Pep vào Aiven, không suy chấn thương hoặc tạo thống kê giả. Service mã mới đọc Aiven: ready=true, fixtures=10, participants=1, currentVersion=0, issues=[]. Còn deploy backend/frontend mới và ADMIN kiểm readiness HTTP rồi công bố; chưa tự công bố, commit, push hoặc deploy.

## Quyết định bổ sung và căn cứ Pep — 21/10/2026

- Người dùng yêu cầu thường trực: trường hợp tương tự Pep hoặc không đá mặc định như người không ra sân; tự xử lý đến readiness khi được giao thu thập kèm ảnh đủ 10 trận, không hỏi lại lý do vắng mặt. Quyết định này thay thế yêu cầu làm rõ chấn thương trong checkpoint ban đầu.
- Pep Chavarría, player_id 2000005020, Chelsea club_id 1000000061, là một trong 11 lượt chọn GW7 của một đội đã chốt. Ảnh 3–4 (sân + toàn bộ dự bị) và StatMuse trận 112827 đã thu thập xác nhận danh sách Everton–Chelsea đầy đủ không có Pep. Fixture 1000560603 FINISHED, membership tại ngày trận hợp lệ. Không có thống kê hoặc vai trò trận của Pep, không có bằng chứng chấn thương; không ghi nhãn INJURY_NOT_REGISTERED.
- `confirmed-not-registered.csv`: đúng một dòng NOT_REGISTERED cho cặp 1000560603/2000005020; không áp dụng sang GW khác. `missing.csv` không còn trường vắng mặt chưa rõ: lý do vắng không còn là dữ liệu bắt buộc.
- Backend/importer/schema/validator frontend được mở rộng giữ hỗ trợ lý do chấn thương cũ và nhận NOT_REGISTERED/NOT_REGISTERED_CONFIRMED. Test cả hai lý do: 0 điểm, rating NULL, idempotence, không tạo thống kê/lineup giả; thiếu nguồn/lineup hoặc trận hoãn vẫn chặn công bố.
- Local H2 dựng từ dữ liệu Aiven GW7 đã nhập: thêm absenceChanges=1, chạy lặp cả ba loại thay đổi bằng 0; readiness ready=true, fixtures=10, participants=1, currentVersion=0, issues=[]; không công bố.
- Production: sau backup mới đã áp dụng `backend/sql/2026-10-21-fantasy-match-absence-mysql.sql`, chạy lần hai unchanged. Importer thật trong transaction SERIALIZABLE nhập đúng một NOT_REGISTERED (Pep, fixture 1000560603), confirmed_at UTC 2026-10-20T18:33:42.510679Z, source_ref `data/gw7-2026/sources.md`. Lần đầu absenceChanges=1, fixtureChanges=0, unratedChanges=0; lần lặp cả ba bằng 0. Không cần nhập lại 400 thống kê/đội hình hoặc rebuild tổng mùa vì dữ liệu thô không đổi.
- Đọc lại Aiven xác nhận 7 căn cứ vắng mặt, giữ nguyên đầy đủ 6 căn cứ injury cũ; không có thống kê/vai trò giả của Pep. Đối chiếu hash trước/sau: fixtures, thống kê, đội hình, membership, tổng mùa 2024/2026, bằng chứng fixture/unrated và toàn bộ đội/deadline/rosterAsOf/snapshot/kết quả Fantasy không thay đổi.
- Service mã mới trên Aiven trả ready=true, fixtures=10, participants=1, currentVersion=0, issues=[], history=[]. Đây là kiểm tra service thật trên database production, chưa phải HTTP readiness của bản Render cũ. Backend/frontend đang chạy cần deploy mã hỗ trợ lý do mới; ADMIN đọc lại readiness bằng phiên đăng nhập thông thường rồi chủ động công bố.

### Kiểm tra mã và schema của quy tắc mới

- `cd backend; mvn -q "-Dtest=Fantasy*Test" test`: đạt 43 test, 0 lỗi/0 failure, gồm 19 FantasyResultIntegrationTest. Lần chạy đầu test mới lỗi do cố xóa parent lineup còn FK; đã sửa thiết lập test thành đánh dấu lineup chưa xác minh, chạy lại đạt. Không chạy lại toàn suite ngoài phạm vi Fantasy.
- `cd backend; mvn -q -DskipTests package`: exit 0, build artifact thành công; đây là build sau test riêng, không gọi là full suite.
- `cd frontend; node --test src/fantasy/results.test.js`: 7/7 đạt, gồm đọc kết quả 0 điểm cho cả lý do chấn thương cũ và NOT_REGISTERED_CONFIRMED mới; rating khác NULL hoặc điểm khác 0 vẫn bị từ chối.
- `cd frontend; npm.cmd run build`: đạt. `npm run build` trước đó không chạy được do PowerShell chặn npm.ps1, đã dùng npm.cmd của tooling hiện có.
- MySQL local 9.6 cô lập trên loopback 33037: migration nâng CHECK, giữ dòng injury cũ, nhận NOT_REGISTERED, từ chối UNKNOWN và chạy lặp không đổi. Sau kiểm tra đã shutdown instance riêng. Migration xử lý tiền tố charset trong CHECK_CLAUSE, không đoán tên constraint; điều kiện không nhận diện sẽ dừng trước ALTER.
- Backup bổ sung trước thay đổi Aiven: `backend/local-backups/gw7-absence-rule-20261020-182845/aiven-before-absence-rule.sql`, 892.561 byte, SHA-256 `2aa47612d9c169aae4699d4eafa50c1437fb31b15b6afa044db4610ba939214b`, UTC 2026-10-20T18:29:35.006941Z. Đủ DDL và số dòng của 15 bảng, marker hoàn tất và Git ignore đã kiểm; chưa thử restore.
- Render live được đọc qua API là commit `fed31e7723d2434262bd45eb2ba7cca85ce4c2ab`; đối chiếu code commit này vẫn chỉ chấp nhận INJURY_NOT_REGISTERED. Vì vậy cần deploy backend mới và frontend validator mới trước khi ADMIN công bố. Chưa gọi HTTP readiness bằng phiên ADMIN; ADMIN tự đọc UI bằng phiên đăng nhập thông thường sau deploy.

## Nhập và kiểm tra production — 21/10/2026

- Đích Render được đọc cấu hình và đối chiếu Aiven `defaultdb`, UUID `715946cc-c33a-11f1-8fd9-8e0831c19405`; TLS VERIFY_IDENTITY bằng CA/hostname. Không dùng Railway hoặc H2 làm production.
- Backup trước ghi: `backend/local-backups/gw7-2026-20261020-170438/aiven-before-gw7-20261020-170438.sql`, 891.955 byte, SHA-256 `6e0ffc460268633aee7c37d9f3d0521a66c080ce58ea4db102553e05eb887845`. Hoàn tất UTC `2026-10-20T17:05:29.017774+00:00` (21/10 giờ Việt Nam). Đã kiểm DDL, số dòng của đủ 20 bảng, marker hoàn tất và Git ignore; chưa thử restore.
- Local H2 cô lập: reader/importer thật kiểm tra toàn batch với dữ liệu nền cần thiết; membership, thống kê, đội hình và bằng chứng chạy lặp thay đổi 0; tổng mùa và readiness đạt đúng kết quả còn blocker Pep. Không sửa mã ứng dụng/schema; không viết test từng GW hoặc chạy full suite/build.
- Production: đồng bộ 10 fixture hiện hữu và 20 dòng BXH, không tạo fixture/CLB mới; thêm 1 players + 1 dòng mùa cho Timur Tuterov và 2 membership mở (Timur/Luca); nhập 400 thống kê, 20 metadata CLB, 20 đội hình/sơ đồ và 400 vai trò; thêm 10 xác nhận CSV cuối và 4 confirmed-unrated, không có xác nhận vắng mặt suy đoán.
- Lệnh đầu chạm timeout 240 giây của script tại bước kiểm tra thống kê. Đọc lại xác nhận 400 thống kê đã commit, 0 đội hình và 0 fixture confirmations; không đoán rollback. Đã tiếp tục cùng input/backup với thời gian chờ phù hợp; kiểm tra importer thống kê trả inserted=0, membership tất cả thêm/cập nhật=0. Đội hình lặp clubChanges=0/fixtureChanges=0/playerChanges=0; bằng chứng lặp fixtureChanges=0/unratedChanges=0/absenceChanges=0.
- Tổng mùa: service tự tái tính qua GW7 (70 fixture/2.800 dòng trận/594 dòng mùa); gọi lại hai lần đều updatedRows=0. So với baseline, 304 dòng mùa hiện có thay đổi và 1 dòng mùa mới được tính; bốn tổng của mọi dòng khớp cộng PLAYED thực tế. NULL cũ giữ nguyên: 1 minutes, 2 goals, 4 assists. Không chạy SQL tổng mùa để lách validator.
- Kiểm tra độc lập MySQL đã khớp từng chỉ số/rating/điểm/vai trò/thay người, sơ đồ và summary. Toàn bộ thống kê/đội hình GW1–6, membership cũ, bảng vị trí hợp lệ và bốn tập dữ liệu mùa 2024/25 được đối chiếu trước/sau không đổi. Snapshot GW7, deadline/rosterAsOf, lượt chọn và trạng thái công bố không đổi. Hồ sơ mở rộng/OVR/eligiblePositions của Timur vẫn chưa thêm.
- Readiness bằng service đọc đúng Aiven: `ready=false`, fixtures=10, participants=1, currentVersion=0; chỉ `SELECTED_STAT_MISSING` và `PLAYER_MATCH_UNRESOLVED` của Pep Chavarría 2000005020 tại Everton–Chelsea 1000560603. API ADMIN qua HTTP chưa đối chiếu bằng phiên đăng nhập chuẩn. Kiểm duyệt tự động từ chối phương án truy xuất cookie từ bảng Spring Session trước khi thực thi vì truy xuất phiên ngoài luồng xác thực chuẩn; không dùng phương án đó, không lưu/in cookie hoặc tạo phiên ADMIN.

## Dữ liệu và nguồn

- CSV thống kê cuối: `manual-match-stats-2026-GW7.csv`, đúng header 10 cột; 400 dòng thực tế từ danh sách hai đội mỗi trận, 305 PLAYED, 95 DID_NOT_PLAY, 301 PLAYED có rating, 4 PLAYED được bảng cuối xác nhận không chấm. Không thêm người/dòng giả để đủ định mức.
- football-data.org được tải bằng FootballDataClient.download()/FootballDataBatch hiện có: batch đủ 20 đội/380 fixture/20 dòng BXH, GW7 có 10 FINISHED. Đối chiếu internal fixture_id qua football_data_fixtures.fixture_id với provider_id; UTC giữ nguyên nguồn. Đã đồng bộ và đọc lại đúng 10 trận GW7 cùng BXH, không cập nhật fixture ngoài GW7.
- Phút/bàn/kiến tạo/thẻ lấy từ Player Stats của các trang StatMuse dưới đây; không lấy rating StatMuse, không lấy 90 trừ phút thay người. Rating chỉ chép từ ảnh người dùng cung cấp trong yêu cầu GW7 ngày 2026-10-20.
- 22 ảnh trong tin nhắn ngày 2026-10-20: cặp 1–2 Tottenham–Coventry; 3–4 Everton–Chelsea; 5–6 Brentford–Liverpool; 7–8 Fulham–Hull; 9–10 Man City–Ipswich; 11–12 Newcastle–Villa; 13–14 Bournemouth–Sunderland; 15–16 Brighton–Palace; 17–18 Leeds–Man United; 19–20 Forest–Arsenal. 21–22 lặp cặp 1–2, không nhân đôi dữ liệu. Một ảnh sân và một bảng dự bị xác nhận đầy đủ vai trò hai đội; dùng USER_SCREENSHOT, không bịa URL SofaScore.
- Vai trò: 220 STARTER, 85 SUB_USED, 95 SUB_UNUSED, 20 sơ đồ GW7 và 220 tọa độ đá chính. Sơ đồ/vị trí được đọc từ bố trí sân; nguồn StatMuse độc lập khớp các sơ đồ. Clubs giữ nguyên default USER; bộ formations chứa cả observations đã lưu trước GW7 để cập nhật summary tích lũy, không thu thập/sửa lại các trận cũ. Players chỉ chứa GW7.
- SUB_UNUSED có căn cứ danh sách cuối + sự kiện thay người. Chỉ ghi sự kiện 0 sau khi các PLAYED giải thích đầy đủ tổng đội và không có sự kiện mâu thuẫn của dự bị. Brighton thắng 5–0 gồm bốn bàn của cầu thủ Brighton và một bàn phản lưới của Jaydee Canvot (StatMuse isOwnGoal=true); không cộng bàn phản lưới cho cầu thủ Brighton hoặc bàn ghi được của Canvot.
- Phút thay người trong đội hình giữ số ở ảnh; số phút thi đấu giữ đúng StatMuse. Trường hợp Frimpong vào 46 và ra 52 vẫn SUB_USED/PLAYED, 6 phút, không đổi thành DID_NOT_PLAY.

| Fixture nội bộ | Provider | Trận | UTC | Tỉ số | StatMuse |
| --- | --- | --- | --- | --- | --- |
| 1000560602 | 560602 | Fulham FC – Hull City AFC | 2026-10-17T14:00:00Z | 1–0 | https://www.statmuse.com/fc/match/-112823 |
| 1000560603 | 560603 | Everton FC – Chelsea FC | 2026-10-17T11:30:00Z | 0–2 | https://www.statmuse.com/fc/match/-112827 |
| 1000560604 | 560604 | AFC Bournemouth – Sunderland AFC | 2026-10-18T13:00:00Z | 2–0 | https://www.statmuse.com/fc/match/-112832 |
| 1000560605 | 560605 | Leeds United FC – Manchester United FC | 2026-10-18T13:00:00Z | 1–1 | https://www.statmuse.com/fc/match/-112829 |
| 1000560606 | 560606 | Tottenham Hotspur FC – Coventry City FC | 2026-10-19T19:00:00Z | 3–1 | https://www.statmuse.com/fc/match/-112826 |
| 1000560607 | 560607 | Newcastle United FC – Aston Villa FC | 2026-10-17T16:30:00Z | 0–1 | https://www.statmuse.com/fc/match/-112825 |
| 1000560608 | 560608 | Nottingham Forest FC – Arsenal FC | 2026-10-18T15:30:00Z | 0–1 | https://www.statmuse.com/fc/match/-112828 |
| 1000560609 | 560609 | Brighton & Hove Albion FC – Crystal Palace FC | 2026-10-18T13:00:00Z | 5–0 | https://www.statmuse.com/fc/match/-112831 |
| 1000560610 | 560610 | Manchester City FC – Ipswich Town FC | 2026-10-17T14:00:00Z | 3–2 | https://www.statmuse.com/fc/match/-112824 |
| 1000560611 | 560611 | Brentford FC – Liverpool FC | 2026-10-17T14:00:00Z | 1–2 | https://www.statmuse.com/fc/match/-112830 |

## Danh tính và membership đã nhập

- Timur Tuterov, Sunderland, số áo 57: ảnh 13–14 và StatMuse player 61257 xác nhận cùng người, đá tiền đạo. Người dùng đã cho phép bổ sung hồ sơ tối thiểu trong phản hồi của lượt này. Cấp ID manual 2000030260 sau truy vấn MAX(id), tìm tên và xác nhận ID chưa dùng; không dùng provider ID làm ID nội bộ. Importer tạo players + dòng mùa FORWARD + membership mở từ ngày đầu có bằng chứng 2026-10-18. Không thêm player_season_profiles, OVR hoặc quyền eligiblePositions khi chưa có căn cứ; giá trị thiếu giữ NULL. FORWARD là vị trí tổng quát được xác minh từ sân, không tự cấp quyền ST Fantasy.
- Luca Williams-Barnett 2000030105: ảnh 1–2/21–22 và danh sách StatMuse xác nhận dự bị Tottenham. Khoảng cũ 2026-08-22→2026-08-23 giữ nguyên; bổ sung khoảng mở từ ngày trận 2026-10-19, không kéo lùi về đầu mùa hoặc thêm khoảng một ngày. ID/dòng mùa FORWARD hiện có giữ nguyên.
- Chema / C. Andrés trên ảnh 15 là José María Andrés Baixauli 2000030174, theo ngoại lệ đã đối chiếu trong GW6 sources.md và batch player-data-brighton-supplements-2026-10-03; membership hiện hành từ 2026-09-30. ID lịch sử Chema Andrés 2000030259 chỉ có khoảng trước 30/09; không hợp nhất hoặc sửa lịch sử hai ID.
- Kaine Kessler-Hayden của StatMuse (7324), K. Kesler-Hayden trên ảnh = Kaine Kesler-Hayden 2000030179 theo hồ sơ Coventry, membership mở từ 30/09; không tạo người mới. Các alias đã có khác: Riquelme Angulo = Juan Angulo 2000030255; Guimissongui Ouattara = Abdoul Ouattara; Florentino = Florentino Ibrain Morris Luís; Chema = José María Andrés Baixauli; Anis/Anan Khalaili, André/Andrey Santos, C./Cristhian Mosquera và tên ngắn/đầy đủ đối chiếu ID hiện có, không đổi tên trong DB.

## PLAYED không được chấm rating

Ảnh bảng dự bị cuối cho thấy đã vào sân nhưng ô rating trống. Áp dụng quyết định thường trực 13/10/2026, không cần xác nhận riêng lại: giữ PLAYED và chỉ số thực tế, rating/fantasy_points thô NULL; xác nhận theo đúng fixture/player trong confirmed-unrated.csv để scoring Fantasy nhận 0 có lý do. Không tham gia đội tiêu biểu bằng rating giả.

| Fixture | Player ID | Tên | Phút StatMuse | Ảnh |
| --- | --- | --- | ---: | --- |
| 1000560603 | 2000005016 | Geovany Quenda | 1 | 3-4 |
| 1000560607 | 18961 | Dan Burn | 1 | 11-12 |
| 1000560609 | 2000030120 | Michael Svoboda | 1 | 15-16 |
| 1000560611 | 2000003018 | Jeremie Frimpong | 6 | 5-6 |

## Fantasy và bước tiếp theo

- Baseline GW7 có một đội đã chốt/11 lượt chọn; 10 ID có trong danh sách trận, Pep ở ngoài danh sách. Phản hồi ban đầu “Chưa rõ hoặc vắng vì lý do khác” từng bị chặn bởi mã chỉ hỗ trợ chấn thương; quyết định 21/10/2026 phía trên thay thế điều kiện đó, không cần hỏi lại.
- Reader/importer, tổng mùa, API công khai, 10 sân 11v11 và API/tab Đội hình tiêu biểu đã hoàn tất như checkpoint. Theo dõi riêng việc nhập căn cứ NOT_REGISTERED và deploy mã hỗ trợ trước công bố; không thu thập lại thống kê đã xong.
- ADMIN dùng phiên đăng nhập thông thường kiểm readiness sau deploy; chỉ công bố khi ready=true. Codex không gọi publish/recalculate, không đổi đội/deadline/rosterAsOf/snapshot.

## API và giao diện đã đối chiếu

- API qua Vercel: danh sách GW7 đủ 10 FINISHED/hasManualStats=true; chi tiết từng fixture khớp tập 400 playerId, participationStatus, minutes/goals/assists/yellowCards/redCards, rating và fantasyPoints với CSV/MySQL. Cả 20 đội có 11 STARTER, sơ đồ nguồn FIXTURE, tọa độ, SUB_USED/SUB_UNUSED và phút thay người đúng ảnh.
- Lịch sử GW7 và chi tiết roster hiện tại đã kiểm tra riêng sáu ID: Dan Burn 18961, Frimpong 2000003018, Quenda 2000005016, Luca Williams-Barnett 2000030105, Michael Svoboda 2000030120, Timur Tuterov 2000030260. Timur xuất hiện đúng Sunderland, fc27Overall/primaryPosition NULL, eligiblePositions rỗng; bốn unrated giữ PLAYED, phút thực tế và rating/fantasyPoints NULL.
- Chrome production: cả 10 fixture ở 1440px và 390px (20 lượt), đúng 22 ID đá chính, sơ đồ và bốn nhóm dự bị; không tràn ngang/chồng thẻ/exception JavaScript hoặc request ghi. Tab Đội hình tiêu biểu ở cả hai kích thước mặc định theo Vòng thi GW7, đủ 38 lựa chọn, 11 ID/vị trí/rating và tổng khớp API; đã chọn GW6 để xác nhận vẫn xem vòng cũ được. Không thay đội đã chốt hoặc mở GW8.
- Artifacts kiểm chứng/ảnh nằm tại backend/target/gw7-collection/, được Git ignore. Không coi bước API ADMIN readiness qua HTTP là đã đạt; cần phiên đăng nhập thông thường của ADMIN như giới hạn đã ghi trên.

## Đội hình tiêu biểu GW7 đã cập nhật/kiểm tra

GET /api/fantasy/2026/team-of-week?gameweek=7: COMPLETE, sơ đồ 4-3-3, tổng rating **93,80**; completedFixtures=10/recordedFixtures=10, playedCount=305/ratedCount=301/candidateCount=284, excludedMissingPositions=17, excludedNullRatings=4. Hai nhóm thiếu trường được báo đúng, không gán vị trí hoặc rating giả. API đọc trực tiếp thống kê/vị trí đã lưu, không nhập bảng đội tiêu biểu và không phụ thuộc công bố Fantasy.

Đã kiểm từng ID, vị trí hợp lệ, fixture/rating nguồn, không trùng người và phép cộng tổng; một phép phân công tối ưu bằng Python độc lập xác nhận tổng lớn nhất và tie-break theo ID khớp service/API.

| Ô | Player ID | Tên | CLB trận | Fixture | Rating |
| --- | ---: | --- | --- | ---: | ---: |
| GK | 2000020030 | Robin Roefs | Sunderland AFC | 1000560604 | 7.60 |
| LB | 2000030095 | Kristoffer Ajer | Brentford FC | 1000560611 | 7.60 |
| LCB | 2000030028 | Jan Paul van Hecke | Tottenham Hotspur FC | 1000560606 | 8.20 |
| RCB | 2000030117 | Olivier Boscagli | Brighton & Hove Albion FC | 1000560609 | 8.20 |
| RB | 2000030109 | James Hill | AFC Bournemouth | 1000560604 | 8.60 |
| LCM | 2000003010 | Alexis Mac Allister | Liverpool FC | 1000560611 | 8.00 |
| CM | 2000004022 | Phil Foden | Manchester City FC | 1000560610 | 8.20 |
| RCM | 2000030020 | James Maddison | Tottenham Hotspur FC | 1000560606 | 8.00 |
| LW | 2000030136 | Alex Iwobi | Fulham FC | 1000560602 | 9.80 |
| ST | 2000004007 | Erling Haaland | Manchester City FC | 1000560610 | 9.90 |
| RW | 2000005007 | Pedro Neto | Chelsea FC | 1000560603 | 9.70 |

17 người PLAYED chưa có eligiblePositions nên không vào tập chọn (không tự bổ sung trong lượt thống kê):

- Anan Khalaili `2000016002`.
- Daizen Maeda `2000020024`.
- Abdoul Ouattara `2000020028`.
- Luke O'Nien `2000020033`.
- Trai Hume `2000020035`.
- Jayden Bogle `2000020065`.
- Ethan Ampadu `2000020068`.
- Noah Okafor `2000020071`.
- Tyrique George `2000020087`.
- Brennan Johnson `2000020093`.
- Tyrick Mitchell `2000020103`.
- Yéremy Pino `2000020105`.
- Keane Lewis-Potter `2000030002`.
- Georginio Rutter `2000030044`.
- Alysson `2000030062`.
- Mathys Tel `2000030101`.
- Timur Tuterov `2000030260`.

## Bàn giao

- File cần commit: toàn bộ 9 file mới trong backend/data/gw7-2026/ (CSV cuối, membership, ba CSV lineup, hai sidecar, missing.csv và sources.md). Không commit backup, credentials hoặc artifacts target.
- Commit message đề xuất: `data: import verified GW7 2026/27 stats and lineups`.
- Checkpoint nhập thống kê ban đầu đã hoàn tất; phần còn lại hiện theo quyết định bổ sung 21/10/2026 ở đầu file. Không cần làm rõ lý do vắng của Pep hoặc thu thập lại 10 trận.
- Lượt nhập thống kê ban đầu không sửa mã/schema và không chạy full test/build. Lượt sửa quy tắc 21/10 có thay đổi mã/schema, test/build được ghi riêng phía trên. Không tự commit, push, deploy, cron, Pre-deploy Command hoặc công bố/tái công bố.

## Bảng đối chiếu từng người từ nguồn cuối

| Fixture | CLB | ID | Tên DB | Vai trò | Rating | Phút | Bàn | KT | Vàng | Đỏ |
| --- | --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1000560602 | Fulham FC | 2000030138 | Bernd Leno | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560602 | Fulham FC | 2000030086 | Antonee Robinson | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560602 | Fulham FC | 2000030087 | Calvin Bassey | STARTER | 7.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560602 | Fulham FC | 2000030197 | David Affengruber | STARTER | 7.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560602 | Fulham FC | 2000030084 | Timothy Castagne | STARTER | 7.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560602 | Fulham FC | 2000030092 | Shea Charles | STARTER | 7.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560602 | Fulham FC | 2000030089 | Sander Berge | STARTER | 6.6 | 90 | 0 | 1 | 1 | 0 |
| 1000560602 | Fulham FC | 2000030136 | Alex Iwobi | STARTER | 9.8 | 90 | 1 | 0 | 0 | 0 |
| 1000560602 | Fulham FC | 2000030083 | Joshua King | STARTER | 7.5 | 77 | 0 | 0 | 1 | 0 |
| 1000560602 | Fulham FC | 2000030093 | Oscar Bobb | STARTER | 7.0 | 77 | 0 | 0 | 0 | 0 |
| 1000560602 | Fulham FC | 2000030082 | Gonzalo García | STARTER | 6.2 | 84 | 0 | 0 | 0 | 0 |
| 1000560602 | Fulham FC | 2729 | Joachim Andersen | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560602 | Fulham FC | 19025 | Tom Cairney | SUB_USED | 7.1 | 13 | 0 | 0 | 0 | 0 |
| 1000560602 | Fulham FC | 2000030085 | Rodrigo Muniz | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560602 | Fulham FC | 2000030088 | Ryan Sessegnon | SUB_USED | 6.6 | 12 | 0 | 0 | 0 | 1 |
| 1000560602 | Fulham FC | 2000030090 | Kevin | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560602 | Fulham FC | 2000030091 | César Palacios | SUB_USED | 6.8 | 6 | 0 | 0 | 0 | 0 |
| 1000560602 | Fulham FC | 2000030139 | Benjamin Lecomte | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560602 | Fulham FC | 2000030140 | Kenny Tete | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560602 | Fulham FC | 2000030196 | Manuel Ángel Morán Ibáñez | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000010001 | Konstantinos Tzolakis | STARTER | 5.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000020004 | Lewie Coyle | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000010002 | Semi Ajayi | STARTER | 6.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000010005 | John Egan | STARTER | 7.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000020010 | Lucas Herrington | STARTER | 6.8 | 45 | 0 | 0 | 1 | 0 |
| 1000560602 | Hull City AFC | 2000030210 | Brooke Norton-Cuffy | STARTER | 6.4 | 67 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000020009 | Mohamed Belloumi | STARTER | 7.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000020007 | Matt Crooks | STARTER | 6.5 | 67 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000010004 | Regan Slater | STARTER | 6.7 | 74 | 0 | 0 | 1 | 0 |
| 1000560602 | Hull City AFC | 2000030211 | Sorba Thomas | STARTER | 6.6 | 67 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000020008 | Oli McBurnie | STARTER | 6.4 | 90 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 284500 | Tim Iroegbunam | SUB_USED | 6.5 | 45 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000020005 | Ryan Giles | SUB_USED | 6.9 | 23 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000020006 | Elliot Stroud | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000020011 | Paddy McNair | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000020015 | Dillon Phillips | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000020018 | Jens Hjertø-Dahl | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000030205 | Joe Gelhardt | SUB_USED | 6.1 | 23 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000030206 | Hidemasa Morita | SUB_USED | 6.8 | 16 | 0 | 0 | 0 | 0 |
| 1000560602 | Hull City AFC | 2000030209 | Robinio Vaz | SUB_USED | 6.5 | 23 | 0 | 0 | 0 | 0 |
| 1000560603 | Chelsea FC | 2000005001 | Emiliano Martinez | STARTER | 7.5 | 74 | 0 | 0 | 0 | 0 |
| 1000560603 | Chelsea FC | 2000005014 | Jorrel Hato | STARTER | 7.2 | 89 | 0 | 0 | 0 | 0 |
| 1000560603 | Chelsea FC | 2000005006 | Levi Colwill | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560603 | Chelsea FC | 2000005005 | Maxence Lacroix | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560603 | Chelsea FC | 2000005022 | Josh Acheampong | STARTER | 7.1 | 90 | 0 | 0 | 1 | 0 |
| 1000560603 | Chelsea FC | 2000005011 | Jordan Henderson | STARTER | 7.1 | 90 | 0 | 0 | 0 | 0 |
| 1000560603 | Chelsea FC | 2000005018 | Moises Caicedo | STARTER | 6.9 | 89 | 0 | 0 | 0 | 0 |
| 1000560603 | Chelsea FC | 2000005012 | Morgan Rogers | STARTER | 7.1 | 90 | 0 | 0 | 1 | 0 |
| 1000560603 | Chelsea FC | 2000005009 | Cole Palmer | STARTER | 7.1 | 90 | 0 | 0 | 1 | 0 |
| 1000560603 | Chelsea FC | 2000005007 | Pedro Neto | STARTER | 9.7 | 89 | 2 | 0 | 1 | 0 |
| 1000560603 | Chelsea FC | 2000005008 | Joao Pedro | STARTER | 7.0 | 89 | 0 | 0 | 0 | 0 |
| 1000560603 | Chelsea FC | 2000005002 | Marco Palestra | SUB_USED | 6.7 | 1 | 0 | 0 | 0 | 0 |
| 1000560603 | Chelsea FC | 2000005003 | Wesley Fofana | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560603 | Chelsea FC | 2000005004 | Valentin Barco | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560603 | Chelsea FC | 2000005013 | Danny Welbeck | SUB_USED | 6.4 | 1 | 0 | 0 | 0 | 0 |
| 1000560603 | Chelsea FC | 2000005016 | Geovany Quenda | SUB_USED | NULL | 1 | 0 | 0 | 0 | 0 |
| 1000560603 | Chelsea FC | 2000005021 | Mahdi Nicoll-Jazuli | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560603 | Chelsea FC | 2000005023 | Mike Penders | SUB_USED | 7.2 | 16 | 0 | 0 | 0 | 0 |
| 1000560603 | Chelsea FC | 2000005024 | Estevao | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560603 | Chelsea FC | 2000005025 | Romeo Lavia | SUB_USED | 6.7 | 1 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 2000020085 | Jordan Pickford | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 2000020086 | Vitaliy Mykolenko | STARTER | 7.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 2000015005 | Jarrad Branthwaite | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 2000020089 | James Tarkowski | STARTER | 7.0 | 90 | 0 | 0 | 1 | 0 |
| 1000560603 | Everton FC | 2000030191 | Ainsley Maitland-Niles | STARTER | 6.2 | 54 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 2000020091 | James Garner | STARTER | 6.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 2000015004 | Harrison Armstrong | STARTER | 7.0 | 84 | 0 | 0 | 1 | 0 |
| 1000560603 | Everton FC | 2000030029 | Jack Grealish | STARTER | 6.5 | 84 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 2000015001 | Kiernan Dewsbury-Hall | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 2000020090 | Merlin Röhl | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 2000015002 | Thierno Barry | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 30407 | Christian Nørgaard | SUB_USED | 6.2 | 6 | 0 | 0 | 1 | 0 |
| 1000560603 | Everton FC | 2000020087 | Tyrique George | SUB_USED | 6.3 | 36 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 2000020088 | Hayden Hackney | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 2000020093 | Brennan Johnson | SUB_USED | 5.8 | 6 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 2000020094 | Carlos Alcaraz | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 2000020109 | Mark Travers | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 2000020112 | Jake O'Brien | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 2000020113 | Tyler Dibling | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560603 | Everton FC | 2000030192 | Braiden Graham | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000020030 | Robin Roefs | STARTER | 7.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000020034 | Reinildo Mandava | STARTER | 6.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000020040 | Omar Alderete | STARTER | 5.7 | 58 | 0 | 0 | 1 | 0 |
| 1000560604 | Sunderland AFC | 2000030103 | Kevin Danso | STARTER | 3.0 | 16 | 0 | 0 | 0 | 1 |
| 1000560604 | Sunderland AFC | 2000020049 | Nordi Mukiele | STARTER | 7.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000020036 | Noah Sadiki | STARTER | 6.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000012002 | Granit Xhaka | STARTER | 7.4 | 90 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000020037 | Enzo Le Fée | STARTER | 6.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000020042 | Chris Rigg | STARTER | 6.6 | 64 | 0 | 0 | 1 | 0 |
| 1000560604 | Sunderland AFC | 2000020035 | Trai Hume | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000030260 | Timur Tuterov | STARTER | 6.4 | 58 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000012001 | Nilson Angulo | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000020033 | Luke O'Nien | SUB_USED | 7.0 | 32 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000020039 | Chemsdine Talbi | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000020043 | Habib Diarra | SUB_USED | 6.4 | 32 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000020048 | Melker Ellborg | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000030252 | Dayann Méthalie | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000030253 | Jules Ahoka | SUB_USED | 6.7 | 26 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000030254 | Malick Fofana | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560604 | Sunderland AFC | 2000030255 | Juan Angulo | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030030 | Đorđe Petrović | STARTER | 6.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030111 | Adrien Truffert | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030035 | António Silva | STARTER | 7.8 | 90 | 0 | 0 | 1 | 0 |
| 1000560604 | AFC Bournemouth | 2000030109 | James Hill | STARTER | 8.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030108 | Adam Smith | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030036 | Tyler Adams | STARTER | 7.6 | 88 | 0 | 0 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030031 | Marcus Tavernier | STARTER | 6.8 | 78 | 1 | 0 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030037 | Lewis Cook | STARTER | 7.7 | 61 | 0 | 0 | 1 | 0 |
| 1000560604 | AFC Bournemouth | 1125 | Ryan Christie | STARTER | 7.1 | 90 | 0 | 0 | 1 | 0 |
| 1000560604 | AFC Bournemouth | 2000030033 | Rayan | STARTER | 7.9 | 88 | 0 | 0 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030032 | Evanilson | STARTER | 8.9 | 78 | 1 | 1 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030034 | Ben Gannon-Doak | SUB_USED | 6.9 | 2 | 0 | 0 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030039 | Justin Kluivert | SUB_USED | 6.2 | 29 | 0 | 0 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030113 | Bafodé Diakité | SUB_USED | 6.7 | 2 | 0 | 0 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030114 | Juanlu Sánchez | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030115 | David Brooks | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030116 | Álvaro Rodriguez | SUB_USED | 6.4 | 12 | 0 | 0 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030154 | Julio Soler Barreto | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030155 | Amine Adli | SUB_USED | 6.1 | 12 | 0 | 0 | 0 | 0 |
| 1000560604 | AFC Bournemouth | 2000030157 | Michele Di Gregorio | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006001 | Senne Lammens | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006002 | Diogo Dalot | STARTER | 7.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006006 | Lisandro Martinez | STARTER | 6.6 | 45 | 0 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006005 | Harry Maguire | STARTER | 7.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006014 | Leny Yoro | STARTER | 6.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006008 | Bruno Fernandes | STARTER | 6.0 | 90 | 0 | 0 | 1 | 0 |
| 1000560605 | Manchester United FC | 2000006016 | Andrey Santos | STARTER | 6.5 | 90 | 0 | 0 | 1 | 0 |
| 1000560605 | Manchester United FC | 2000006019 | Carlos Baleba | STARTER | 6.6 | 73 | 0 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006017 | Youri Tielemans | STARTER | 7.5 | 83 | 0 | 1 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006015 | Amad Diallo | STARTER | 7.1 | 90 | 0 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006018 | Bryan Mbeumo | STARTER | 6.6 | 90 | 1 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006003 | Noussair Mazraoui | SUB_USED | 7.4 | 45 | 0 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006004 | Matthijs de Ligt | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006007 | Mason Mount | SUB_USED | 7.0 | 17 | 0 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006009 | Marcus Rashford | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006010 | Matheus Cunha | SUB_USED | 6.5 | 7 | 0 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006011 | Joshua Zirkzee | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006012 | Karl Darlow | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000006026 | Shea Lacey | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560605 | Manchester United FC | 2000030233 | Harry Amass | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000014002 | James Trafford | STARTER | 6.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000020070 | Tarik Muharemović | STARTER | 7.3 | 90 | 0 | 1 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000020080 | Nico Elvedi | STARTER | 7.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000014004 | James Justin | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000030220 | Gabriel Gudmundsson | STARTER | 6.2 | 90 | 0 | 0 | 1 | 0 |
| 1000560605 | Leeds United FC | 2000020082 | Ao Tanaka | STARTER | 6.6 | 80 | 0 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000020068 | Ethan Ampadu | STARTER | 6.9 | 90 | 0 | 0 | 1 | 0 |
| 1000560605 | Leeds United FC | 2000020065 | Jayden Bogle | STARTER | 7.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000020071 | Noah Okafor | STARTER | 6.5 | 73 | 0 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000014001 | Anton Stach | STARTER | 6.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000020067 | Dominic Calvert-Lewin | STARTER | 7.2 | 90 | 1 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000014003 | Jaka Bijol | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000020066 | Harry Wilson | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000020069 | Brenden Aaronson | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000020072 | Sean Longstaff | SUB_USED | 6.6 | 10 | 0 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000020084 | Lukas Nmecha | SUB_USED | 6.5 | 17 | 0 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000030221 | Ilia Gruev | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000030224 | Michael Zetterer | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000030225 | Melvin Bard | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560605 | Leeds United FC | 2000030226 | Jean-Mattéo Bahoya | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 2000030015 | Antonín Kinský | STARTER | 6.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 152849 | Micky van de Ven | STARTER | 7.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 19145 | Tosin Adarabioyo | STARTER | 7.3 | 70 | 0 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 2000030028 | Jan Paul van Hecke | STARTER | 8.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 2000030100 | Sandro Tonali | STARTER | 7.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 2000030027 | Andy Robertson | STARTER | 6.8 | 68 | 0 | 1 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 2000030020 | James Maddison | STARTER | 8.0 | 68 | 1 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 2000030017 | Archie Gray | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 15911 | Mohammed Kudus | STARTER | 7.4 | 86 | 0 | 1 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 2000030107 | Omar Marmoush | STARTER | 7.2 | 86 | 1 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 2000030018 | Dominic Solanke | STARTER | 6.7 | 90 | 0 | 1 | 1 | 0 |
| 1000560606 | Tottenham Hotspur FC | 30435 | Dejan Kulusevski | SUB_USED | 6.6 | 4 | 0 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 2000030024 | Mateus Fernandes | SUB_USED | 6.5 | 20 | 0 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 2000030025 | Conor Gallagher | SUB_USED | 6.7 | 22 | 0 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 2000030026 | Marcos Senesi | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 2000030101 | Mathys Tel | SUB_USED | 6.3 | 4 | 0 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 2000030102 | Martin Dúbravka | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 2000030104 | Ben Davies | SUB_USED | 7.9 | 22 | 1 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 2000030105 | Luca Williams-Barnett | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560606 | Tottenham Hotspur FC | 2000030251 | Callum Olusesi | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000002016 | Carl Rushworth | STARTER | 4.4 | 90 | 0 | 0 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000002002 | Jay Dasilva | STARTER | 5.8 | 90 | 0 | 0 | 1 | 0 |
| 1000560606 | Coventry City FC | 2000002001 | Ethan Pinnock | STARTER | 7.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000002003 | Bobby Thomas | STARTER | 6.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000002024 | Milan van Ewijk | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000002005 | Matt Grimes | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000002007 | Caleb Yirenkyi | STARTER | 6.1 | 59 | 0 | 0 | 1 | 0 |
| 1000560606 | Coventry City FC | 2000002020 | Brandon Thomas-Asante | STARTER | 6.6 | 77 | 0 | 1 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000002004 | Jack Rudoni | STARTER | 6.8 | 86 | 1 | 0 | 1 | 0 |
| 1000560606 | Coventry City FC | 2000002006 | Tatsuhiro Sakamoto | STARTER | 7.2 | 77 | 0 | 0 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000002029 | Sidiki Cherif | STARTER | 6.4 | 59 | 0 | 0 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000002008 | Ellis Simms | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000002009 | Ephron Mason-Clark | SUB_USED | 6.9 | 13 | 0 | 0 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000002011 | Stephen Mfuni | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000002013 | Taiwo Awoniyi | SUB_USED | 6.1 | 31 | 0 | 0 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000002014 | Frank Onyeka | SUB_USED | 7.1 | 31 | 0 | 0 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000002022 | Dan Bentley | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000002026 | Victor Torp | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000002028 | Gustavo Hamer | SUB_USED | 6.8 | 13 | 0 | 0 | 0 | 0 |
| 1000560606 | Coventry City FC | 2000030179 | Kaine Kesler-Hayden | SUB_USED | 6.5 | 4 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 2000030124 | Zion Suzuki | STARTER | 7.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 18846 | Aaron Wan-Bissaka | STARTER | 6.8 | 76 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 2000030065 | Tyrone Mings | STARTER | 7.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 2000030056 | Victor Lindelöf | STARTER | 7.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 2000030058 | Matty Cash | STARTER | 7.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 2000030057 | João Gomes | STARTER | 7.2 | 71 | 0 | 1 | 1 | 0 |
| 1000560607 | Aston Villa FC | 2000030122 | Boubacar Kamara | STARTER | 7.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 2000030061 | Emiliano Buendía | STARTER | 7.1 | 71 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 2000030146 | Johan Manzambi | STARTER | 8.0 | 71 | 1 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 2000030062 | Alysson | STARTER | 6.6 | 45 | 0 | 0 | 1 | 0 |
| 1000560607 | Aston Villa FC | 283058 | Nicolas Jackson | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 144729 | Taylor Harwood-Bellis | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 2000030059 | John McGinn | SUB_USED | 7.0 | 45 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 2000030066 | Pau Torres | SUB_USED | 6.8 | 14 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 2000030067 | Lamare Bogarde | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 2000030068 | Ross Barkley | SUB_USED | 6.8 | 19 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 2000030069 | Marco Bizot | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 2000030148 | Brian Madjo | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 2000030149 | Leon Goretzka | SUB_USED | 7.1 | 19 | 0 | 0 | 0 | 0 |
| 1000560607 | Aston Villa FC | 2000030150 | Ibrahim Mbaye | SUB_USED | 7.4 | 19 | 0 | 0 | 0 | 0 |
| 1000560607 | Newcastle United FC | 2000030074 | Lukáš Horníček | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560607 | Newcastle United FC | 2000030128 | Lewis Hall | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560607 | Newcastle United FC | 2000030130 | Sven Botman | STARTER | 7.5 | 90 | 0 | 0 | 1 | 0 |
| 1000560607 | Newcastle United FC | 2000030131 | Malick Thiaw | STARTER | 7.6 | 90 | 0 | 0 | 1 | 0 |
| 1000560607 | Newcastle United FC | 2000030078 | Lewis Miley | STARTER | 7.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560607 | Newcastle United FC | 2000030106 | Nico González | STARTER | 7.2 | 90 | 0 | 0 | 1 | 0 |
| 1000560607 | Newcastle United FC | 723 | Joelinton | STARTER | 6.5 | 89 | 0 | 0 | 1 | 0 |
| 1000560607 | Newcastle United FC | 2000030129 | Harvey Barnes | STARTER | 6.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560607 | Newcastle United FC | 2000030071 | Joe Willock | STARTER | 6.7 | 55 | 0 | 0 | 0 | 0 |
| 1000560607 | Newcastle United FC | 2000030238 | Matias Fernandez-Pardo | STARTER | 6.7 | 55 | 0 | 0 | 0 | 0 |
| 1000560607 | Newcastle United FC | 2000030072 | Yoane Wissa | STARTER | 7.0 | 90 | 0 | 0 | 1 | 0 |
| 1000560607 | Newcastle United FC | 18911 | Nick Pope | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560607 | Newcastle United FC | 18961 | Dan Burn | SUB_USED | NULL | 1 | 0 | 0 | 0 | 0 |
| 1000560607 | Newcastle United FC | 2000030073 | William Osula | SUB_USED | 6.1 | 35 | 0 | 0 | 1 | 0 |
| 1000560607 | Newcastle United FC | 2000030075 | Fabian Schär | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560607 | Newcastle United FC | 2000030076 | Jacob Murphy | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560607 | Newcastle United FC | 2000030077 | Aladji Bamba | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560607 | Newcastle United FC | 2000030080 | Sean Steur | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560607 | Newcastle United FC | 2000030081 | Amar Dedić | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560607 | Newcastle United FC | 2000030134 | Bazoumana Touré | SUB_USED | 6.5 | 35 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001001 | David Raya | STARTER | 7.4 | 90 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001020 | Riccardo Calafiori | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001006 | Gabriel Magalhaes | STARTER | 8.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001013 | Ezri Konsa | STARTER | 7.4 | 90 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001004 | Ben White | STARTER | 6.9 | 65 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001022 | Bruno Guimaraes | STARTER | 7.6 | 65 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001023 | Declan Rice | STARTER | 7.7 | 90 | 0 | 1 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001009 | Eberechi Eze | STARTER | 6.4 | 45 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001008 | Martin Odegaard | STARTER | 7.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001007 | Bukayo Saka | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001018 | Kai Havertz | STARTER | 6.3 | 65 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001003 | Cristhian Mosquera | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001010 | Jurrien Timber | SUB_USED | 7.2 | 25 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001012 | Viktor Gyokeres | SUB_USED | 6.5 | 25 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001015 | Noni Madueke | SUB_USED | 7.4 | 45 | 1 | 0 | 1 | 0 |
| 1000560608 | Arsenal FC | 2000001017 | Mikel Merino | SUB_USED | 6.6 | 25 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001019 | Illan Meslier | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001021 | Martin Zubimendi | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001024 | Myles Lewis-Skelly | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560608 | Arsenal FC | 2000001025 | Max Dowman | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000020052 | Matz Sels | STARTER | 6.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000013002 | Murillo | STARTER | 7.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000020074 | Ousmane Diomande | STARTER | 7.1 | 90 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000020059 | Jair | STARTER | 6.7 | 84 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000020053 | Neco Williams | STARTER | 6.4 | 90 | 0 | 0 | 1 | 0 |
| 1000560608 | Nottingham Forest FC | 2000020063 | Xaver Schlager | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000013001 | James McAtee | STARTER | 7.1 | 84 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000020095 | Daniel Muñoz | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000020076 | Nicolás Domínguez | STARTER | 6.7 | 73 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000020058 | Dan Ndoye | STARTER | 6.3 | 72 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 161948 | Liam Delap | STARTER | 7.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 19305 | Ryan Yates | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000020054 | Ibrahim Sangaré | SUB_USED | 6.4 | 17 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000020057 | Nikola Milenković | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000020060 | Ola Aina | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000020061 | Chris Wood | SUB_USED | 6.6 | 6 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000020062 | Luca Netz | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000020064 | Arnaud Kalimuendo | SUB_USED | 6.6 | 18 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000020073 | John Victor | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560608 | Nottingham Forest FC | 2000020077 | Callum Hudson-Odoi | SUB_USED | 6.8 | 6 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000020114 | Walter Benítez | STARTER | 5.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000030186 | Honest Ahanor | STARTER | 6.0 | 52 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000020099 | Chris Richards | STARTER | 6.1 | 90 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000020096 | Jaydee Canvot | STARTER | 5.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000020103 | Tyrick Mitchell | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000030188 | Quinten Timber | STARTER | 6.2 | 45 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000020098 | Adam Wharton | STARTER | 6.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000016002 | Anan Khalaili | STARTER | 6.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000020105 | Yéremy Pino | STARTER | 6.6 | 60 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000016001 | Daichi Kamada | STARTER | 7.1 | 80 | 0 | 0 | 1 | 0 |
| 1000560609 | Crystal Palace FC | 2000020106 | Jørgen Strand Larsen | STARTER | 6.2 | 60 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2218 | Ismaïla Sarr | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2490 | Jefferson Lerma | SUB_USED | 6.6 | 38 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000020097 | Dwight McNeil | SUB_USED | 6.5 | 45 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000020101 | Eddie Nketiah | SUB_USED | 6.6 | 30 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000020102 | Jean-Philippe Mateta | SUB_USED | 6.5 | 30 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000020116 | Óscar Mingueza | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000020117 | Will Hughes | SUB_USED | 6.4 | 10 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000030184 | Remi Matthews | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560609 | Crystal Palace FC | 2000030189 | Darío Osorio | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030055 | Bart Verbruggen | STARTER | 7.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030117 | Olivier Boscagli | STARTER | 8.2 | 80 | 0 | 1 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030045 | Lewis Dunk | STARTER | 7.6 | 89 | 0 | 0 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030046 | Luka Vušković | STARTER | 7.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030167 | Ferdi Kadıoğlu | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030174 | José María Andrés Baixauli | STARTER | 7.0 | 90 | 0 | 0 | 1 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030049 | Pascal Groß | STARTER | 6.6 | 70 | 0 | 0 | 1 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030042 | Maxim De Cuyper | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030048 | Yasin Ayari | STARTER | 7.4 | 80 | 1 | 0 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030043 | Diego Gómez | STARTER | 7.7 | 90 | 1 | 0 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030051 | Charalampos Kostoulas | STARTER | 7.8 | 70 | 1 | 1 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030041 | Jack Hinshelwood | SUB_USED | 7.1 | 20 | 1 | 0 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030044 | Georginio Rutter | SUB_USED | 6.7 | 20 | 0 | 0 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030050 | Costinha | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030052 | Ibrahim Osman | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030054 | Malick Yalcouyé | SUB_USED | 6.7 | 10 | 0 | 0 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030118 | Jason Steele | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030120 | Michael Svoboda | SUB_USED | NULL | 1 | 0 | 0 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030169 | Matt O'Riley | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560609 | Brighton & Hove Albion FC | 2000030173 | Jaouen Hadjam | SUB_USED | 6.6 | 10 | 0 | 0 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004001 | Gianluigi Donnarumma | STARTER | 6.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004018 | Nico O'Reilly | STARTER | 6.1 | 60 | 0 | 0 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004004 | Marc Guehi | STARTER | 7.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004002 | Ruben Dias | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004015 | Matheus Nunes | STARTER | 6.9 | 75 | 0 | 0 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004017 | Ayyoub Bouaddi | STARTER | 7.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004006 | Mateo Kovacic | STARTER | 7.3 | 49 | 0 | 1 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004009 | Jeremy Doku | STARTER | 7.2 | 76 | 0 | 1 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004022 | Phil Foden | STARTER | 8.2 | 76 | 1 | 0 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004005 | Iliman Ndiaye | STARTER | 6.3 | 90 | 0 | 1 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004007 | Erling Haaland | STARTER | 9.9 | 90 | 2 | 0 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004003 | Elliot Anderson | SUB_USED | 6.7 | 14 | 0 | 0 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004008 | Rayan Cherki | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004011 | Enzo Fernandez | SUB_USED | 7.6 | 41 | 0 | 0 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004012 | Rayan Ait-Nouri | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004014 | Josko Gvardiol | SUB_USED | 6.2 | 30 | 0 | 0 | 1 | 0 |
| 1000560610 | Manchester City FC | 2000004016 | Geronimo Rulli | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004019 | Allan Andrade Elias | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004020 | Antoine Semenyo | SUB_USED | 6.5 | 14 | 0 | 0 | 0 | 0 |
| 1000560610 | Manchester City FC | 2000004021 | Abdukodir Khusanov | SUB_USED | 6.3 | 15 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000020044 | Christian Walton | STARTER | 6.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000020021 | Jacob Greaves | STARTER | 6.5 | 89 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000020022 | Issa Diop | STARTER | 6.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000020023 | Dara O'Shea | STARTER | 7.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000020020 | Leif Davis | STARTER | 6.8 | 90 | 0 | 0 | 1 | 0 |
| 1000560610 | Ipswich Town FC | 2000030218 | Exequiel Palacios | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000030217 | Florentino Ibrain Morris Luís | STARTER | 6.5 | 70 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000020028 | Abdoul Ouattara | STARTER | 6.5 | 90 | 0 | 1 | 1 | 0 |
| 1000560610 | Ipswich Town FC | 2000011002 | Julio Enciso | STARTER | 6.2 | 77 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000020024 | Daizen Maeda | STARTER | 7.2 | 70 | 1 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000011001 | Emersonn | STARTER | 6.6 | 77 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 138931 | Jaden Philogene-Bidace | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000011003 | Jack Clarke | SUB_USED | 6.7 | 1 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000020019 | Kjell Scherpen | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000020025 | Abdul Fatawu Issahaku | SUB_USED | 7.5 | 20 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000020027 | Chuba Akpom | SUB_USED | 6.8 | 13 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000020045 | Darnell Furlong | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000020046 | Cédric Kipré | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000030215 | Azor Matusiwa | SUB_USED | 6.9 | 20 | 0 | 0 | 0 | 0 |
| 1000560610 | Ipswich Town FC | 2000030219 | Zian Flemming | SUB_USED | 6.9 | 13 | 1 | 0 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003001 | Alisson Becker | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003006 | Milos Kerkez | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003004 | Virgil van Dijk | STARTER | 7.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003005 | Jeremy Jacquet | STARTER | 7.3 | 90 | 0 | 0 | 1 | 0 |
| 1000560611 | Liverpool FC | 2000003019 | Ronald Araujo | STARTER | 6.4 | 45 | 0 | 0 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003008 | Dominik Szoboszlai | STARTER | 6.9 | 31 | 0 | 0 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003010 | Alexis Mac Allister | STARTER | 8.0 | 90 | 0 | 0 | 1 | 0 |
| 1000560611 | Liverpool FC | 2000003017 | Bradley Barcola | STARTER | 6.7 | 90 | 0 | 1 | 1 | 0 |
| 1000560611 | Liverpool FC | 2000003007 | Florian Wirtz | STARTER | 6.6 | 90 | 0 | 1 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003014 | Victor Munoz | STARTER | 7.7 | 78 | 1 | 0 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003009 | Alexander Isak | STARTER | 7.7 | 78 | 1 | 0 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003002 | Joe Gomez | SUB_USED | 7.1 | 12 | 0 | 0 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003003 | Wataru Endo | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003011 | Cody Gakpo | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003012 | Kostas Tsimikas | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003015 | Giorgi Mamardashvili | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003018 | Jeremie Frimpong | SUB_USED | NULL | 6 | 0 | 0 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003021 | Trey Nyoni | SUB_USED | 6.8 | 59 | 0 | 0 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003023 | Lewis Koumas | SUB_USED | 6.4 | 12 | 0 | 0 | 0 | 0 |
| 1000560611 | Liverpool FC | 2000003024 | Rio Ngumoha | SUB_USED | 7.2 | 39 | 0 | 0 | 0 | 0 |
| 1000560611 | Brentford FC | 2000030001 | Caoimhin Kelleher | STARTER | 6.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560611 | Brentford FC | 2000030002 | Keane Lewis-Potter | STARTER | 7.5 | 76 | 1 | 0 | 0 | 0 |
| 1000560611 | Brentford FC | 2000030095 | Kristoffer Ajer | STARTER | 7.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560611 | Brentford FC | 2000030158 | Sepp van den Berg | STARTER | 6.3 | 70 | 0 | 0 | 0 | 0 |
| 1000560611 | Brentford FC | 2000030004 | Michael Kayode | STARTER | 7.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560611 | Brentford FC | 2000030003 | Vitaly Janelt | STARTER | 6.5 | 89 | 0 | 0 | 2 | 1 |
| 1000560611 | Brentford FC | 2000030011 | Yehor Yarmoliuk | STARTER | 5.6 | 44 | 0 | 0 | 0 | 1 |
| 1000560611 | Brentford FC | 2000030010 | Jaidon Anthony | STARTER | 6.6 | 76 | 0 | 0 | 0 | 0 |
| 1000560611 | Brentford FC | 2000030013 | Mathias Jensen | STARTER | 6.8 | 45 | 0 | 0 | 0 | 0 |
| 1000560611 | Brentford FC | 2000030012 | Dango Ouattara | STARTER | 6.9 | 45 | 0 | 0 | 0 | 0 |
| 1000560611 | Brentford FC | 2000030006 | Igor Thiago | STARTER | 5.6 | 90 | 0 | 1 | 1 | 0 |
| 1000560611 | Brentford FC | 153066 | Fabio Carvalho | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560611 | Brentford FC | 2000030005 | Mamadou Sangare | SUB_USED | 6.5 | 45 | 0 | 0 | 0 | 0 |
| 1000560611 | Brentford FC | 2000030007 | Callum Wilson | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560611 | Brentford FC | 2000030008 | Aaron Hickey | SUB_USED | 7.5 | 20 | 0 | 0 | 0 | 0 |
| 1000560611 | Brentford FC | 2000030009 | Mikkel Damsgaard | SUB_USED | 6.9 | 14 | 0 | 0 | 0 | 0 |
| 1000560611 | Brentford FC | 2000030014 | Nathan Collins | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560611 | Brentford FC | 2000030094 | Kevin Schade | SUB_USED | 6.4 | 45 | 0 | 0 | 1 | 0 |
| 1000560611 | Brentford FC | 2000030096 | Hákon Rafn Valdimarsson | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560611 | Brentford FC | 2000030163 | El Hadji Malick Diouf | SUB_USED | 7.1 | 14 | 0 | 0 | 0 | 0 |

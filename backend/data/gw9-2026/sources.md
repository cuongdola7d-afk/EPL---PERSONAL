# GW9 Premier League 2026/27 — đã nhập 9/10 trận; checkpoint 02/11/2026

Người dùng gửi 18 ảnh và yêu cầu “GW10”, để Everton–Newcastle sau. API PremierHub và football-data.org xác nhận cả 9 cặp đấu trong ảnh thuộc **GW9**; trận còn lại là **Newcastle–Everton**. Người dùng đã xác nhận xử lý GW9 theo ảnh và cho phép bổ sung hồ sơ tối thiểu Subuloye. Không thay lịch hoặc chuyển các ảnh sang fixture GW10.

## Trạng thái hiện tại — production đã xong 9 trận FINISHED

- Đã nhập 360 người thực tế từ 18 ảnh: 198 STARTER, 82 SUB_USED, 80 SUB_UNUSED; đủ danh sách hai đội của 9 trận. 280 PLAYED, 274 có rating và 6 confirmed-unrated theo bảng cuối người dùng cung cấp. Không thêm người hoặc dòng giả.
- Đã tải batch mùa đầy đủ 20 CLB/380 fixture/20 dòng BXH và đối chiếu endpoint riêng GW9. Dùng FootballDataSync hiện có cập nhật đúng 10 fixture GW9 và BXH 20 CLB; provider xác nhận 9 FINISHED, Newcastle–Everton TIMED được chuẩn hóa SCHEDULED theo parser hiện có. Không tạo fixture hoặc sửa lịch các GW khác. BXH có tổng played=178, tương ứng 89 trận kết thúc.
- Sau quyền người dùng, đã bổ sung đúng 1 player/1 dòng mùa DEFENDER/1 khoảng membership mở cho Olutayo Subuloye, ID 2000030262, Chelsea từ 31/10/2026. Ảnh 3–4 và StatMuse 112847/player 65309 khớp tên/số 37/dự bị không vào sân. Không có OVR, hồ sơ mùa mở rộng, primaryPosition hoặc eligiblePositions được tạo. Không thêm khoảng một ngày, sửa khoảng cũ hoặc tạo ID Emegha mới.
- Đã nhập thống kê, 18 đội hình/198 STARTER, sơ đồ/vị trí/phút vào-ra theo ảnh, 9 xác nhận CSV cuối/đủ danh sách hai đội và 6 xác nhận unrated. Chạy lặp membership không đổi; thống kê inserted=0; lineup clubChanges=0/fixtureChanges=0/playerChanges=0; evidence fixtureChanges=0/unratedChanges=0/absenceChanges=0.
- Đã đối chiếu MySQL và API công khai cho từng giá trị của cả 360 dòng; mọi ID/status/chỉ số/rating/fantasyPoints và formation/role/row/slot/phút thay người khớp CSV. PLAYED unrated giữ rating/fantasyPoints NULL, DID_NOT_PLAY có fantasyPoints=0. Lịch sử 6 unrated và Subuloye, hồ sơ hiện tại và roster Chelsea đã kiểm tra. API lịch đủ 10 fixture, 9 FINISHED/hasManualStats; Newcastle–Everton chưa có thống kê/đội hình/bằng chứng.
- Readiness bằng service ADMIN thật đọc Aiven: ready=false, fixtures=10, participants=0, currentVersion=0/history rỗng. Cả 6 cảnh báo chỉ thuộc fixture 1000560622: FIXTURE_PENDING; LINEUP_UNCONFIRMED/STARTERS_UNCONFIRMED cho hai CLB; FIXTURE_UNCONFIRMED. Không còn blocker của 9 trận đã nhập. GW9 chưa có lượt chọn đã chốt nên confirmed-not-registered.csv chỉ header; không tạo bằng chứng vắng mặt khi không có lượt chọn cần xử lý.
- Tổng mùa vẫn **qua GW8**, 80 fixture/3.200 dòng, 596 dòng mùa sau hồ sơ mới. Importer và hai lần rebuildLatestComplete cho updatedRows=0, missingMinutes=1/missingGoals=2/missingAssists=4 lịch sử giữ NULL. Đã đọc lại toàn bộ dòng mùa: các tổng cũ giữ nguyên, Subuloye tổng 0. Service hiện yêu cầu vòng hoàn chỉnh, nên chưa cộng 9 trận GW9; không chạy SQL tổng mùa thủ công hoặc báo tổng mùa đã qua GW9.
- Chrome production 1440px/390px: 9 trận × 2 = 18 sân, mỗi sân 22 STARTER đúng ID, bốn nhóm dự bị/vai trò và hai formation đúng; không chồng thẻ/tràn ngang/lỗi JavaScript hoặc request ghi. Tab đội tiêu biểu mặc định GW9 theo Vòng thi, 38 lựa chọn, 11 ID/vị trí/rating/tổng khớp và vẫn xem được GW7. Đây là kiểm chứng giao diện thật sau nhập.
- Dữ liệu mùa 2024/25, thống kê/đội hình các GW cũ, snapshot/đội nháp/đội chốt, deadline/rosterAsOf và kết quả đã công bố giữ nguyên. Không mở GW mới, công bố/tái công bố, commit, push hoặc deploy.

## Nguồn

Ảnh gốc được tham chiếu theo thứ tự trong tin nhắn người dùng ngày 02/11/2026: `USER_SCREENSHOT: GW9; supplied 2026-11-02; images 1-18`. Mỗi cặp là sân/đá chính và bảng dự bị hai đội. Thư mục lưu bản chép CSV; chưa có file ảnh gốc độc lập. Rating chỉ từ các ảnh này, không dùng rating StatMuse/OVR.

Lịch: [PremierHub GW9](https://premierhub.vercel.app/api/matches?season=2026&matchweek=9), [PremierHub GW10](https://premierhub.vercel.app/api/matches?season=2026&matchweek=10), football-data.org `v4/competitions/PL/matches?season=2026&matchday=9`; ID nội bộ ánh xạ qua `football_data_fixtures.fixture_id`, không dùng provider ID làm khóa CSV.

| Ảnh | Fixture nội bộ / provider | Trận | Tỉ số | Nguồn chỉ số |
| --- | --- | --- | --- | --- |
| 1–2 | 1000560631 / 560631 | Liverpool–Arsenal | 0–1 | [StatMuse 112844](https://www.statmuse.com/fc/match/-112844) |
| 3–4 | 1000560626 / 560626 | Chelsea–Manchester United | 2–0 | [StatMuse 112847](https://www.statmuse.com/fc/match/-112847) |
| 5–6 | 1000560624 / 560624 | Bournemouth–Leeds | 2–0 | [StatMuse 112852](https://www.statmuse.com/fc/match/-112852) |
| 7–8 | 1000560630 / 560630 | Brentford–Nottingham Forest | 2–0 | [StatMuse 112849](https://www.statmuse.com/fc/match/-112849) |
| 9–10 | 1000560625 / 560625 | Coventry–Sunderland | 3–1 | [StatMuse 112850](https://www.statmuse.com/fc/match/-112850) |
| 11–12 | 1000560629 / 560629 | Hull–Ipswich | 2–2 | [StatMuse 112851](https://www.statmuse.com/fc/match/-112851) |
| 13–14 | 1000560623 / 560623 | Manchester City–Brighton | 0–1 | [StatMuse 112845](https://www.statmuse.com/fc/match/-112845) |
| 15–16 | 1000560627 / 560627 | Tottenham–Crystal Palace | 2–0 | [StatMuse 112848](https://www.statmuse.com/fc/match/-112848) |
| 17–18 | 1000560628 / 560628 | Aston Villa–Fulham | 1–1 | [StatMuse 112843](https://www.statmuse.com/fc/match/-112843) |

Newcastle–Everton: fixture **1000560622**, provider **560622**, [StatMuse 112846](https://www.statmuse.com/fc/match/-112846), kickoff **02/11/2026 20:00 UTC = 03/11/2026 03:00 Việt Nam**. Giữ pending, không nhập thống kê/đội hình hoặc bằng chứng cho trận chưa kết thúc.

## Quy ước và ngoại lệ

- `screenshot-evidence.csv` giữ tên/số áo, vai trò, rating và phút vào/ra từ ảnh; tọa độ theo thứ tự từ trên xuống của từng tuyến. Đây là bằng chứng thu thập, không phải đầu vào importer. Phút vào/ra không dùng thay thời lượng; phút/bàn/kiến tạo/thẻ lấy Player Stats StatMuse, đối chiếu tổng đội/sự kiện. Dự bị không vào sân được xác minh danh sách; không lấy NULL đơn thuần làm bằng chứng.
- `lineups/` dùng tọa độ theo góc nhìn đội; đảo thứ tự slot của đội khách so với bản chép ảnh. Sơ đồ trận theo bố trí ảnh, giữ sơ đồ mặc định USER và observations lịch sử; không dùng nhãn chiến thuật StatMuse để ghi đè sơ đồ ảnh.
- Bản chép chỉ đánh dấu `NO_RATING_SHOWN` cho người vào sân không có rating; không chép toàn bộ biểu tượng bàn/thẻ/kiến tạo. Các chỉ số này phải lấy từ nguồn Player Stats đã đối chiếu.
- Tên khác giữa nguồn và ảnh đã đối chiếu trong cùng fixture/CLB/số áo: Lindelof/Nilsson-Lindelöf; Chema/C. Andrés; Yirenkyi/C. Irvenik; Reinildo/R. Mandava; Iroegbunam/I. Troegbounam; Bachir/Mohamed Belloumi; Philogene/Philogene-Bidace; Florentino/F. Luís; Guimissongui/Abdoul Ouattara; Manuel Ángel/Morán. Emegha dùng ID hiện có 2000005015, không tạo người thứ hai do Emanuel/Emmanuel.
- 6 PLAYED confirmed-unrated: Geovany Quenda (Chelsea); Joe Gelhardt (Hull); Chuba Akpom (Ipswich); Luca Williams-Barnett (Tottenham); Jefferson Lerma và Will Hughes (Crystal Palace). Giữ rating/fantasy_points thô NULL, bằng chứng riêng `confirmed-unrated.csv`; Fantasy nhận 0 khi bằng chứng đã nhập. Không tạo rating 0 hoặc đưa vào đội tiêu biểu bằng điểm giả.
- Mọi `player_id` của bộ nhập đã kiểm tra membership ngày trận; hồ sơ Subuloye đã nhập tối thiểu theo quyền người dùng, không thêm OVR/eligiblePositions.

## Backup và kiểm chứng

- Đích đã đối chiếu với cấu hình Render: Aiven `defaultdb`, UUID `715946cc-c33a-11f1-8fd9-8e0831c19405`; TLS VERIFY_IDENTITY.
- Backup mới trước ghi: `backend/local-backups/gw9-collection-20261102-115118/aiven-before-gw9.sql`, 1,107,200 byte, SHA-256 `6d0b9a0dc6d1ea48f8a8a7cd8edbd108a98aa599f8cb9b40ef27be911aa6e235`, UTC `2026-11-02T11:51:50.496632+00:00`. Đã kiểm đủ DDL/số dòng 20 bảng, marker hoàn tất và Git ignore; chưa thử restore. Backup nằm ngoài tập file Git theo dõi.
- Reader chung đọc đủ 360 dòng. H2 cô lập và production dùng đúng ManualRosterImporter/ManualMatchStatsImporter/LineupBatchImporter/FantasyEvidenceImporter, cùng transaction SERIALIZABLE; nhập lặp không đổi. Không khởi động Spring/schema init trên production.
- Helpers và báo cáo tại `backend/target/gw9-collection/` được Git ignore: `local.private.log`, `apply.private.log`, `verification-report.json`, `browser-report.json`, ảnh sân/đội tiêu biểu và `backup-report.json`. Đã đọc kết quả thực tế, không coi câu log PASS là bằng chứng duy nhất. Không chạy Maven full suite/build cho thay đổi dữ liệu.

## Đội hình tiêu biểu tạm tính GW9

API `/api/fantasy/2026/team-of-week?gameweek=9`: COMPLETE, **95,10**, completedFixtures=9/recordedFixtures=9, playedCount=280/ratedCount=274/candidateCount=257, excludedMissingPositions=18/excludedNullRatings=6. COMPLETE chỉ nghĩa là xếp được 11 người hợp lệ; toàn vòng vẫn chưa đủ trận. Các nhóm thiếu rating/vị trí có thể giao nhau. Đã kiểm rating nguồn, quyền vị trí, 11 ID khác nhau và tổng; phép phân công Python độc lập xác nhận tổng tối ưu và tie-break ID. Không nhập bảng đội tiêu biểu riêng hoặc tự gán vị trí/rating.

| Slot | Player ID | Name | Rating |
| --- | --- | --- | ---: |
| GK | 2000030055 | Bart Verbruggen | 9.60 |
| LB | 2000030027 | Andy Robertson | 8.60 |
| LCB | 2000001013 | Ezri Konsa | 8.70 |
| RCB | 2000030028 | Jan Paul van Hecke | 9.80 |
| RB | 2000005022 | Josh Acheampong | 8.30 |
| LCM | 284500 | Tim Iroegbunam | 7.90 |
| CM | 2000002004 | Jack Rudoni | 7.90 |
| RCM | 2000005012 | Morgan Rogers | 8.40 |
| LW | 2000004009 | Jeremy Doku | 9.10 |
| ST | 283058 | Nicolas Jackson | 8.50 |
| RW | 2000030093 | Oscar Bobb | 8.30 |

## Bước tiếp theo

Chỉ còn **Newcastle–Everton 1000560622** theo yêu cầu để sau. Khi trận được provider xác nhận FINISHED và có ảnh danh sách/rating cuối, tiếp tục đúng thư mục này: bổ sung CSV chung và đội hình/bằng chứng, kiểm tra backup mới trước nhập, đối chiếu mọi lượt chọn hiện có, tổng mùa/Đội hình tiêu biểu/readiness. Không thu thập lại 9 trận đã xong. ADMIN tự công bố khi đủ điều kiện; chưa công bố Fantasy GW9.

`missing.csv` chỉ còn fixture chưa FINISHED. Các file cần commit thuộc `backend/data/gw9-2026/`: CSV thống kê chung, membership, screenshot evidence/fixtures, ba CSV lineups, sidecar unrated/not-registered, missing và sources. Commit message gợi ý: `data: collect nine finished GW9 matches for 2026/27`. Chưa commit/push/deploy.

## Lịch sử mở Fantasy GW9 — giữ checkpoint ngày 26/10/2026

# Fantasy GW9 mùa 2026/27 — bắt đầu ngày 26/10/2026

Người dùng yêu cầu tiếp tục dữ liệu GW8, bỏ qua công bố Fantasy GW8 vì chưa bắt đầu, rồi bắt đầu GW9. Đã mở GW9 production bằng `GameweekService.publishDeadline` hiện có, không tạo cuộc thi/kết quả GW8 hoặc công bố điểm vòng nào. Công việc nhập GW8 còn theo checkpoint riêng; phần không phụ thuộc hồ sơ mới được tiếp tục độc lập.

- Đích đã đối chiếu: Render dùng Aiven `defaultdb`, UUID `715946cc-c33a-11f1-8fd9-8e0831c19405`, TLS VERIFY_IDENTITY.
- Trước ghi đã tạo và kiểm tra backup 21 bảng: `backend/local-backups/gw8-and-start-gw9-20261026-121828/aiven-before-gw8-and-gw9.sql`, 1.002.361 byte, SHA-256 `a2adb2ccd32c1eeffb1c12feb7da70562140bd9557ab8b41fffdf9a20c0662a3`; UTC 26/10/2026 12:19:03.110865. Đủ DDL/số dòng/marker hoàn tất, được Git ignore; chưa thử restore.
- Đã xác nhận đủ 10 fixture GW9 có kickoff UTC, chưa bắt đầu; lịch được đối chiếu football-data.org. Không lấy ngày trống giờ hoặc dữ liệu một GW để giả batch mùa đầy đủ.
- Trận đầu: Chelsea FC – Manchester United FC, fixture nội bộ **1000560626**, provider **560626**, **31/10/2026 12:30 UTC = 19:30 Việt Nam**.
- Deadline đã ghi: **30/10/2026 00:00 Việt Nam = 29/10/2026 17:00 UTC**, đúng quy tắc ngày liền trước ngày trận đầu. Roster cố định **26/10/2026**, revision **1**, workflow **OPEN**.
- Mở thực tế lúc UTC **26/10/2026 12:23:55.743271**, dùng ADMIN ID 2 đã được chủ dự án cho phép từ trước và đối chiếu role hiện hành. Service ghi cấu hình + audit trong transaction SERIALIZABLE; gọi lặp bị từ chối 409 và giữ nguyên cấu hình. Không giả session hoặc sửa quyền tài khoản.
- API `GET /api/fantasy/2026/gameweeks`: `recommendedGameweek=9`, GW9 `configured=true/status=OPEN/canEdit=true`, GW8 `configured=false`; deadline/rosterAsOf đúng bản ghi. API roster GW9 ban đầu trả 559 người tại ngày 26/10. Roster có thể phản ánh membership đã xác minh bổ sung trong bước nhập GW8; không đổi mốc ngày đã chốt.
- Đã đối chiếu giữ nguyên cấu hình/lịch sử các vòng cũ, đội nháp/đội chốt và mọi kết quả đã công bố. Không sao chép đội GW7, tạo đội thay người dùng, mở bù GW8 hoặc công bố/tái công bố kết quả.
- Kiểm chứng: mở GW9 được kiểm tra trên H2 cô lập trước production; kiểm tra dữ liệu thực tế/API công khai như trên. Không thay mã ứng dụng/schema/dependency; chưa commit, push hoặc deploy.
- Chrome production không đăng nhập ở 1440px và 390px: mặc định GW9, trạng thái đang mở, hạn 30/10/2026 00:00 và mốc roster 26/10 hiển thị đúng, sân 11 ô; không tràn ngang/lỗi JavaScript hoặc request ghi. Sau batch dữ liệu GW8, đối chiếu lại cấu hình + audit GW9 giữ nguyên; đội người chơi và kết quả cũ không đổi.
- Ngày 27/10 đã hoàn tất 10/10 trận GW8 sau khi chủ dự án duyệt hồ sơ tối thiểu Teddy. MySQL và API xác nhận GW9 vẫn OPEN/canEdit=true, recommendedGameweek=9, deadline 29/10 17:00 UTC và rosterAsOf 26/10 giữ nguyên; không mở hoặc công bố Fantasy GW8. Xem checkpoint GW8 về dữ liệu, backup và kiểm chứng cuối.

Nguồn API: [lịch GW9](https://premierhub.vercel.app/api/matches?season=2026&matchweek=9), [các vòng Fantasy](https://premierhub.vercel.app/api/fantasy/2026/gameweeks), [roster GW9](https://premierhub.vercel.app/api/fantasy/2026/gameweeks/9/players). Chi tiết dữ liệu GW8 tại [checkpoint GW8](../gw8-2026/sources.md).

Bước tiếp theo: người chơi lưu đội GW9 trước deadline. Sau khi có trận FINISHED và ảnh cuối, thu thập GW9 theo workflow; ADMIN tự công bố sau khi dữ liệu đủ và readiness đạt. Hiện chưa thu thập thống kê/rating GW9 hoặc tạo bằng chứng trận chưa đá.

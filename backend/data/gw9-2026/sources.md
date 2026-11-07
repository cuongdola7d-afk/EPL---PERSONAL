# GW9 Premier League 2026/27 — đã nhập và kiểm chứng đủ 10/10 trận; checkpoint 07/11/2026

Người dùng gửi 18 ảnh và yêu cầu “GW10”, để Everton–Newcastle sau. API PremierHub và football-data.org xác nhận cả 9 cặp đấu trong ảnh thuộc **GW9**; trận còn lại là **Newcastle–Everton**. Người dùng đã xác nhận xử lý GW9 theo ảnh và cho phép bổ sung hồ sơ tối thiểu Subuloye. Không thay lịch hoặc chuyển các ảnh sang fixture GW10.

## Trạng thái hiện tại — production đã xong 10/10 trận FINISHED

- Đã nhập đủ 400 cầu thủ thực tế từ 20 ảnh: 220 STARTER, 90 SUB_USED, 90 SUB_UNUSED; 310 PLAYED, 304 có rating và 6 confirmed-unrated. Mỗi fixture có danh sách đầy đủ hai đội; không thêm người hoặc dòng giả.
- Football-data.org xác nhận Newcastle United–Everton FINISHED 0–1, fixture nội bộ 1000560622/provider 560622, kickoff 02/11/2026 20:00 UTC. Batch mùa có 20 CLB/380 fixture/20 BXH; cả 10 fixture GW9 FINISHED và BXH có played=182 (91 trận).
- Trận cuối có 30 PLAYED, 10 SUB_UNUSED, 22 đá chính và 8 vào sân; Thierno Barry ghi bàn. Cả hai đội dùng sơ đồ 4-2-3-1 theo ảnh. Rating 30 người ra sân đọc được từ ảnh.
- Đã nhập dữ liệu trận/đội hình/vai trò/sơ đồ/bằng chứng cho đủ 10 fixture. Importer chạy lặp không tạo thay đổi: thống kê 400 dòng, đội hình 20 CLB/80 quan sát sơ đồ/400 vai trò và bằng chứng 10 fixture. Membership đã đối chiếu, không thêm khoảng mới cho trận này.
- Đã đọc lại production MySQL và API công khai cho 40 dòng trận cuối: ID/status/chỉ số/rating/fantasyPoints, đội hình, vai trò/phút thay người, sơ đồ/slot khớp CSV. PLAYED unrated các trận trước giữ rating/fantasyPoints NULL; DID_NOT_PLAY nhận 0 điểm Fantasy.
- Tổng mùa đã rebuild tới GW9 qua importer: 90 fixture/3.600 dòng trận; 596 dòng mùa. Lượt rebuild lặp updatedRows=0; 1 minutes, 2 goals, 4 assists chưa xác minh vẫn NULL. Fixture cũ và mùa 2024/25 không đổi.
- Readiness Aiven: ready=true, fixtures=10, participants=0, issues rỗng. Không có lượt chọn GW9 đã chốt hoặc kết quả GW9 được công bố. Không tạo xác nhận vắng mặt hay công bố Fantasy.
- API Đội hình tiêu biểu GW9 COMPLETE, 95,10; completedFixtures=10/recordedFixtures=10, playedCount=310/ratedCount=304/candidateCount=283, excludedMissingPositions=22/excludedNullRatings=6. Đã kiểm tra 11 cầu thủ, vị trí hợp lệ và tổng rating.
- Chrome production 1440px và 390px: 10 trận ở mỗi độ rộng, 22 đá chính đúng mỗi sân, bench/vai trò/sơ đồ khớp API; không chồng thẻ, tràn ngang, lỗi JavaScript hoặc request ghi. Đội hình tiêu biểu mặc định GW9, 11 lựa chọn khớp API; GW7 vẫn xem được.
- Đã giữ nguyên đội nháp/đội chốt, deadline/rosterAsOf và kết quả Fantasy đã công bố. GW9 sẵn sàng để ADMIN tự công bố; Codex không công bố/tái công bố. Không commit, push hoặc deploy.

## Nguồn

Ảnh gốc được tham chiếu theo thứ tự trong tin nhắn người dùng ngày 02/11/2026 (images 1-18) và 07/11/2026 (Newcastle–Everton images 3-4 trong checkpoint thu thập). Mỗi cặp là sân/đá chính và bảng dự bị hai đội. Thư mục lưu bản chép CSV; chưa có file ảnh gốc độc lập. Rating chỉ từ ảnh người dùng cung cấp, không dùng rating StatMuse/OVR.

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
| 19–20 | 1000560622 / 560622 | Newcastle United–Everton | 0–1 | [StatMuse 112846](https://www.statmuse.com/fc/match/-112846) |

Newcastle–Everton: fixture 1000560622, provider 560622, [StatMuse 112846](https://www.statmuse.com/fc/match/-112846), kickoff 02/11/2026 20:00 UTC = 03/11/2026 03:00 Việt Nam, kết thúc 0–1. StatMuse xác nhận danh sách trận, sự kiện, thống kê cầu thủ/đội; ảnh người dùng là nguồn rating, đội hình, sơ đồ và vai trò.

## Quy ước và ngoại lệ

- `screenshot-evidence.csv` giữ tên/số áo, vai trò, rating và phút vào/ra từ ảnh; tọa độ theo thứ tự từ trên xuống của từng tuyến. Đây là bằng chứng thu thập, không phải đầu vào importer. Phút vào/ra không dùng thay thời lượng; phút/bàn/kiến tạo/thẻ lấy Player Stats StatMuse, đối chiếu tổng đội/sự kiện. Dự bị không vào sân được xác minh danh sách; không lấy NULL đơn thuần làm bằng chứng.
- `lineups/` dùng tọa độ theo góc nhìn đội; đảo thứ tự slot của đội khách so với bản chép ảnh. Sơ đồ trận theo bố trí ảnh, giữ sơ đồ mặc định USER và observations lịch sử; không dùng nhãn chiến thuật StatMuse để ghi đè sơ đồ ảnh.
- Bản chép chỉ đánh dấu `NO_RATING_SHOWN` cho người vào sân không có rating; không chép toàn bộ biểu tượng bàn/thẻ/kiến tạo. Các chỉ số này phải lấy từ nguồn Player Stats đã đối chiếu.
- Tên khác giữa nguồn và ảnh đã đối chiếu trong cùng fixture/CLB/số áo: Lindelof/Nilsson-Lindelöf; Chema/C. Andrés; Yirenkyi/C. Irvenik; Reinildo/R. Mandava; Iroegbunam/I. Troegbounam; Bachir/Mohamed Belloumi; Philogene/Philogene-Bidace; Florentino/F. Luís; Guimissongui/Abdoul Ouattara; Manuel Ángel/Morán. Emegha dùng ID hiện có 2000005015, không tạo người thứ hai do Emanuel/Emmanuel.
- 6 PLAYED confirmed-unrated: Geovany Quenda (Chelsea); Joe Gelhardt (Hull); Chuba Akpom (Ipswich); Luca Williams-Barnett (Tottenham); Jefferson Lerma và Will Hughes (Crystal Palace). Giữ rating/fantasy_points thô NULL, bằng chứng riêng `confirmed-unrated.csv`; Fantasy nhận 0 khi bằng chứng đã nhập. Không tạo rating 0 hoặc đưa vào đội tiêu biểu bằng điểm giả.
- Mọi `player_id` của bộ nhập đã kiểm tra membership ngày trận; hồ sơ Subuloye đã nhập tối thiểu theo quyền người dùng, không thêm OVR/eligiblePositions.

## Backup và kiểm chứng

- Backup mới trước ghi: `backend/local-backups/gw9-collection-20261107-123725/aiven-before-gw9.sql`, 1,202,253 byte, SHA-256 `f0fb8047b1fbf0da39f93cd8ba92fdf43f80f175877491f4f2b15676eadc0b5b`, UTC `2026-11-07T12:37:55.913104+00:00`. Đã kiểm đủ DDL/số dòng 20 bảng, marker hoàn tất, server UUID và Git ignore; chưa thử restore. Backup nằm ngoài tập file Git theo dõi.

- Reader chung đọc đủ 400 dòng. H2 cô lập và production dùng `FootballDataSync`/`ManualRosterImporter`/`ManualMatchStatsImporter`/`LineupBatchImporter`/`FantasyEvidenceImporter`; production chạy transaction SERIALIZABLE và lượt nhập lặp không đổi. Đã đọc lại MySQL/API; Fantasy protected rows khớp snapshot.

## Đội hình tiêu biểu GW9

[API Đội hình tiêu biểu GW9](https://premierhub.vercel.app/api/fantasy/2026/team-of-week?gameweek=9): COMPLETE, **95,10**, completedFixtures=10/recordedFixtures=10, playedCount=310/ratedCount=304/candidateCount=283, excludedMissingPositions=22/excludedNullRatings=6. Đã kiểm rating nguồn, quyền vị trí, 11 ID khác nhau và tổng tối ưu. Không nhập bảng đội tiêu biểu riêng hoặc tự gán vị trí/rating.


| Slot | Player ID | Name | Rating |
| --- | ---: | --- | ---: |
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

## Trạng thái kết thúc thu thập GW9

Đủ 10 fixture FINISHED, dữ liệu trận/đội hình/bằng chứng đã nhập và kiểm chứng; tổng mùa, API đội hình tiêu biểu và readiness đã cập nhật. GW9 chưa được công bố Fantasy. Chỉ ADMIN công bố khi sẵn sàng. Không cần thu thập thêm ảnh cho GW9; tiếp tục vòng khác theo yêu cầu.

CSV thuần chưa cần test riêng/full suite; chưa commit hoặc push.

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

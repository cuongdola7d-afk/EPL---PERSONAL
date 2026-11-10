# GW10 Premier League 2026/27 — đã nhập và kiểm chứng 10/10 trận; 09/11/2026

Người dùng yêu cầu thu thập đủ GW10 từ 20 ảnh cuối, bỏ qua công bố kết quả vì chưa mở cuộc thi, và bắt đầu Fantasy GW11. GW10 chưa có cấu hình cuộc thi hoặc đội đã chốt. Quyền ngày 09/11/2026 cho phép thêm hai hồ sơ tối thiểu Modou Kéba Cissé và Luca Gunter; không thêm OVR hoặc quyền vị trí Fantasy.

## Trạng thái

- Đã đối chiếu đủ 10 fixture với PremierHub và football-data.org: tất cả FINISHED. Danh sách thực tế từ ảnh/StatMuse có 400 cầu thủ: 220 STARTER, 93 SUB_USED, 87 SUB_UNUSED; 313 PLAYED và 305 rating.
- Đã nhập production trong transaction SERIALIZABLE, commit UTC **09/11/2026 16:02:28**: 400 thống kê mới, 20 đội hình/400 vai trò/220 STARTER, 20 quan sát sơ đồ mới (100 tích lũy), 10 xác nhận CSV cuối và 8 xác nhận unrated. Hai hồ sơ tối thiểu đã nhập đúng quyền người dùng. Membership/statistics/lineups/evidence chạy lặp không thêm hoặc cập nhật.
- Đã đọc lại MySQL và API công khai: đủ 10 FINISHED/hasManualStats; từng ID/status/chỉ số/rating/fantasyPoints/role/vị trí/phút vào-ra/sơ đồ khớp CSV. Lịch sử của 8 unrated và hai người mới, hồ sơ hiện tại, membership và roster được đối chiếu. Rating/fantasy_points thô của PLAYED unrated vẫn NULL.
- Tổng mùa đã cập nhật qua GW10: 100 fixture/4.000 dòng, 598 dòng mùa. Tính độc lập từ PLAYED khớp từng appearances/minutes/goals/assists; rebuild lặp updatedRows=0. Thiếu lịch sử 1 minutes, 2 goals và 4 assists tiếp tục giữ NULL. Thống kê/vai trò/đội hình trận cũ và mùa 2024/25 giữ nguyên.
- GW10 không mở cuộc thi, không có đội đã chốt hoặc kết quả công bố. Readiness `ready=false`, fixtures=10, participants=0, currentVersion=0, chỉ có **GW_NOT_OPENED**; không còn lỗi cấp fixture/thống kê/đội hình/bằng chứng. Đây là trạng thái bỏ qua cuộc thi được người dùng yêu cầu, không phải thiếu dữ liệu trận.
- Đội hình tiêu biểu API GW10 COMPLETE, **92,40**; 313 PLAYED/305 rated/285 ứng viên, excludedMissingPositions=20/excludedNullRatings=8, 10 completed/10 recorded. Phép phân công độc lập kiểm tổng tối ưu, tie-break ID, 11 ID và quyền vị trí.
- Chrome production ở 1440px/390px: cả 10 sân đều 22 đá chính đúng ID, bốn nhóm dự bị và sơ đồ đúng; không chồng thẻ/tràn ngang/lỗi JavaScript/request ghi. Vòng thi và tab đội tiêu biểu mặc định GW11 sau mở; chọn GW10 thấy 11 ID/vị trí/rating/tổng đúng API, vẫn xem được GW7.
- Đã mở GW11 đúng service và kiểm chứng MySQL/API/giao diện; xem [checkpoint GW11](../gw11-2026/sources.md). Đội nháp/đội đã chốt, deadline/rosterAsOf và kết quả vòng cũ giữ nguyên. Không công bố/tái công bố Fantasy, commit, push hoặc deploy.

## Nguồn

Ảnh 1–20 là ảnh sân và dự bị hai đội trong tin nhắn ngày 09/11/2026. Rating, vai trò, sơ đồ, vị trí và phút thay người chép từ ảnh; không dùng rating nguồn khác. StatMuse cung cấp phút/bàn/kiến tạo/thẻ và danh sách trận để đối chiếu. Lịch, kickoff UTC, trạng thái, tỉ số, BXH lấy football-data.org (batch mùa đầy đủ 20 CLB/380 fixture/20 dòng BXH và endpoint riêng GW10).

| Ảnh | Fixture nội bộ / provider | Trận | Tỉ số | Nguồn chỉ số |
| --- | --- | --- | --- | --- |
| 1–2 | 1000560639 / 560639 | Manchester United–Aston Villa | 4–4 | [StatMuse 112857](https://www.statmuse.com/fc/match/-112857) |
| 3–4 | 1000560636 / 560636 | Sunderland–Chelsea | 1–0 | [StatMuse 112854](https://www.statmuse.com/fc/match/-112854) |
| 5–6 | 1000560637 / 560637 | Ipswich–Bournemouth | 1–2 | [StatMuse 112860](https://www.statmuse.com/fc/match/-112860) |
| 7–8 | 1000560635 / 560635 | Crystal Palace–Liverpool | 1–3 | [StatMuse 112861](https://www.statmuse.com/fc/match/-112861) |
| 9–10 | 1000560638 / 560638 | Brighton–Brentford | 2–0 | [StatMuse 112862](https://www.statmuse.com/fc/match/-112862) |
| 11–12 | 1000560641 / 560641 | Nottingham Forest–Manchester City | 3–1 | [StatMuse 112858](https://www.statmuse.com/fc/match/-112858) |
| 13–14 | 1000560633 / 560633 | Fulham–Newcastle | 1–1 | [StatMuse 112853](https://www.statmuse.com/fc/match/-112853) |
| 15–16 | 1000560640 / 560640 | Arsenal–Hull | 1–0 | [StatMuse 112855](https://www.statmuse.com/fc/match/-112855) |
| 17–18 | 1000560632 / 560632 | Leeds–Tottenham | 1–0 | [StatMuse 112859](https://www.statmuse.com/fc/match/-112859) |
| 19–20 | 1000560634 / 560634 | Everton–Coventry | 3–1 | [StatMuse 112856](https://www.statmuse.com/fc/match/-112856) |

[API PremierHub GW10](https://premierhub.vercel.app/api/matches?season=2026&matchweek=10), [API GW11](https://premierhub.vercel.app/api/matches?season=2026&matchweek=11), [Đội hình tiêu biểu GW10](https://premierhub.vercel.app/api/fantasy/2026/team-of-week?gameweek=10).

## Ngoại lệ và danh tính

- 8 PLAYED được ảnh cuối xác nhận không có rating: Granit Xhaka (Sunderland), Joe Gomez và Lewis Koumas (Liverpool), Malick Yalcouyé (Brighton), Martín Zubimendi (Arsenal), Joe Rodon và Ilia Gruev (Leeds), Carlos Alcaraz (Everton). Giữ rating/fantasy_points thô NULL và chỉ số thực tế; nhập confirmed-unrated.csv để scoring tính 0 khi cần, không tạo rating 0.
- Modou Kéba Cissé: ID mới 2000030263, Aston Villa 1000000058, DEFENDER, membership mở từ trận xác minh 08/11/2026; số 48 SUB_UNUSED khớp ảnh/StatMuse 64509. StatMuse gắn MIDFIELDER, nhưng [hồ sơ CLB](https://next.avfc.co.uk/teams/mens/modoucisse) xác nhận Central Defender. Dùng DEFENDER tổng quát, không tự thêm CB/eligiblePositions/OVR.
- Luca Gunter: ID mới 2000030264, Tottenham 1000000073, GOALKEEPER, membership mở từ trận xác minh 07/11/2026; số 46 SUB_UNUSED khớp ảnh/StatMuse 63173 và [hồ sơ CLB](https://www.tottenhamhotspur.com/player/536108/luca-gunter). Không suy membership từ lịch sử cho mượn trên trang hồ sơ: danh sách cuối trận là căn cứ tại ngày trận.
- ID cầu thủ có sẵn giữ nguyên. Tên viết tắt/biến thể được đối chiếu tên đầy đủ, CLB, số áo và lịch sử nguồn. Không đổi vị trí Fantasy theo vị trí trận. Tọa độ đội khách đảo slot sang góc nhìn đội. 80 quan sát sơ đồ đã lưu được kế thừa, thêm 20 quan sát GW10.
- confirmed-not-registered.csv chỉ header vì chưa có lượt chọn GW10 đã chốt; không tạo dòng vắng mặt hoặc DID_NOT_PLAY giả.

## Fantasy GW11

Đã đối chiếu đủ 10 fixture/kickoff UTC với endpoint riêng GW11 và batch mùa football-data.org. Đã mở bằng `GameweekService.publishDeadline(11,2,reason,2026-11-09)` lúc UTC **09/11/2026 16:02:27.251919**; OPEN/canEdit=true, revision=1. Trận đầu Manchester City–Fulham (1000560643/560643), **21/11/2026 12:30 UTC = 19:30 Việt Nam**. Deadline **19/11/2026 17:00 UTC = 20/11/2026 00:00 Việt Nam**; rosterAsOf **09/11/2026**, 564 người duy nhất có membership đúng mốc. Gọi mở lặp bị 409 và giữ cấu hình. GW10 vẫn chưa cấu hình; không công bố điểm GW10/GW11.

## Backup và kiểm tra trước ghi

- Đã đối chiếu cấu hình backend Render với Aiven `defaultdb`, server UUID `715946cc-c33a-11f1-8fd9-8e0831c19405`, MySQL 8.4.8, TLS VERIFY_IDENTITY.
- Backup mới: `backend/local-backups/gw10-collection-20261109-155201/aiven-before-gw10-and-gw11.sql`; 1.212.784 byte; SHA-256 `e8017cf5c783f6e1db99d66a2d5cc57f69ba72f8fe88f7f22096b0abe33302fc`; tạo UTC `2026-11-09T15:52:32.296295+00:00`. Đã kiểm DDL/số dòng 20 bảng, marker hoàn tất và Git ignore; chưa thử restore.
- H2 dùng các importer/service hiện có: 400 thống kê mới, 20 CLB/100 quan sát sơ đồ/400 vai trò, 10 xác nhận fixture và 8 unrated. Lượt nhập lặp không đổi. Rebuild tổng mùa 100 fixture/4.000 dòng/598 dòng mùa, lượt lặp updatedRows=0; các tổng chưa xác minh giữ NULL.
- H2: Đội hình tiêu biểu COMPLETE 92,40; 313 PLAYED/305 rated/285 ứng viên. Readiness dữ liệu từng fixture không còn lỗi, riêng cuộc thi GW10 chưa mở theo yêu cầu. Mở GW11 bằng service cho đúng deadline/roster và lặp bị từ chối 409, không thay cấu hình đã mở.
- Helpers và báo cáo nằm trong `backend/target/gw10-collection/`, Git ignore. Chỉ thay CSV/tài liệu; không chạy full suite cho dữ liệu thuần, không khởi động Spring/schema init trên production.

## Đội hình tiêu biểu GW10 đã đối chiếu API

| Slot | Player ID | Cầu thủ | Rating |
| --- | ---: | --- | ---: |
| GK | 2000014002 | James Trafford | 8.70 |
| LB | 2000006013 | Patrick Dorgu | 8.40 |
| LCB | 2000010002 | Semi Ajayi | 8.90 |
| RCB | 2000030028 | Jan Paul van Hecke | 8.20 |
| RB | 2000003008 | Dominik Szoboszlai | 7.90 |
| LCM | 2000003010 | Alexis Mac Allister | 8.10 |
| CM | 2000006008 | Bruno Fernandes | 8.20 |
| RCM | 2000030049 | Pascal Groß | 8.50 |
| LW | 2000003007 | Florian Wirtz | 8.30 |
| ST | 161948 | Liam Delap | 9.00 |
| RW | 2000001015 | Noni Madueke | 8.20 |

Không nhập bảng đội tiêu biểu riêng hoặc gán vị trí/rating để đủ đội. Trường hợp thiếu vị trí/rating bị loại đúng dữ liệu hiện có. Bằng chứng kiểm chứng cuối tại `verification-report.json`/`browser-report.json` cùng thư mục target được ignore; ảnh sân và đội tiêu biểu đã lưu ở đó.

## Bước tiếp theo

GW10 đã thu thập đủ. Bỏ qua công bố Fantasy GW10 theo yêu cầu. Người chơi có thể lưu đội GW11 trước deadline; thu thập GW11 khi có fixture FINISHED và ảnh cuối tương ứng. `missing.csv` không còn dòng thiếu; chưa commit/push/deploy.

# GW8 Premier League 2026/27 — dữ liệu đủ 10/10, checkpoint 27/10/2026

## Tiếp tục ngày 27/10/2026 — đã được phép bổ sung Teddy

Người dùng đã xác nhận “Cho phép bổ sung hồ sơ tối thiểu” cho Teddy Sharman-Lowe. Quyền này thay trạng thái chờ xác nhận của checkpoint 26/10 phía dưới; chỉ bổ sung danh tính/dòng mùa GOALKEEPER/membership có căn cứ, giữ OVR và quyền vị trí Fantasy chưa xác minh trống.

- Đã kiểm tra lại Render/Aiven đúng `defaultdb`, UUID `715946cc-c33a-11f1-8fd9-8e0831c19405`. Không có hồ sơ Teddy/Sharman hoặc ID 2000030261; fixture 1000560617 FINISHED 0–2, 9 trận trước vẫn có 360 dòng. ID đề xuất còn trống và được dùng cho danh tính đã xác minh bằng ảnh 9–10 + StatMuse player 7329.
- Backup mới trước lần ghi cuối: `backend/local-backups/gw8-teddy-finish-20261026-193331/aiven-before-gw8-final-fixture.sql`, 971.436 byte, SHA-256 `381d6daf1fad376b09e7637df92c4c99ec9e55f93f8bf7bf1e28db6e5a1b8a8d`, UTC `2026-10-26T19:34:02.656730Z` (27/10 giờ Việt Nam). Đã kiểm DDL/số dòng đủ 17 bảng, marker hoàn tất và Git ignore; chưa thử restore.
- Đã kiểm tra H2 cô lập bằng importer thật trên dữ liệu nền 9 trận đã nhập, sau đó nhập production trong transaction SERIALIZABLE: thêm đúng 1 player/1 dòng mùa GOALKEEPER/1 khoảng membership mở Chelsea từ 24/10 cho Teddy 2000030261; 40 thống kê và 2 đội hình/22 STARTER của fixture 1000560617. Không thêm OVR, player_season_profiles, player_specific_positions hoặc eligiblePositions cho Teddy; API trả fc27Overall/primaryPosition NULL, eligiblePositions rỗng. Teddy là dự bị không vào sân có căn cứ danh sách cuối, DID_NOT_PLAY/phút 0/rating NULL, không phải dòng giả.
- Chạy lặp membership không thay đổi; thống kê inserted=0; lineup clubChanges=0/fixtureChanges=0/playerChanges=0. Xác nhận fixture cuối thay đổi đúng 1; lần xác nhận lại bằng toàn CSV 400 dòng/10 trận có fixtureChanges=0/unratedChanges=0/absenceChanges=0. `missing.csv` hiện chỉ header.
- Tổng mùa tự rebuild qua GW8 khi nhập trận cuối: 80 fixture, 3.200 dòng trận, 595 dòng mùa; 303 dòng mùa hiện có được cập nhật theo dữ liệu GW8, Teddy có tổng 0 vì không ra sân. Rebuild và chạy lặp updatedRows=0. Giữ nguyên 1 missingMinutes, 2 missingGoals, 4 missingAssists lịch sử dưới dạng NULL; không sửa mùa 2024/25 hoặc raw stats các trận cũ.
- Đọc lại MySQL và API: đủ 10 FINISHED/hasManualStats, đúng tập 400 ID và mọi giá trị thống kê/rating/fantasyPoints, 20 đội hình/220 STARTER, formation/role/row/slot/phút thay người khớp CSV. Lịch sử và hồ sơ Teddy, Elanga và ba unrated đã kiểm tra. 303 PLAYED/97 DID_NOT_PLAY, 300 rated/3 confirmed-unrated; người không chấm giữ rating/fantasyPoints thô NULL. Riêng Kadıoğlu giữ 5 phút theo StatMuse và nhãn ra 6' theo ảnh, không lấy phút thay người làm thời lượng.
- API Đội hình tiêu biểu GW8: COMPLETE, 10 completed/10 recorded, 303 played/300 rated/280 candidates, excludedMissingPositions=20, excludedNullRatings=3; 11 ID/vị trí/rating và tổng 93,50 khớp dữ liệu nguồn. Phép phân công Python độc lập xác nhận tổng tối ưu và tie-break ID. Không thêm rating/vị trí giả hoặc nhập bảng đội tiêu biểu riêng.
- Chrome production sau khi nhập đủ: 10 fixture × 1440px/390px = 20 lượt sân, mỗi sân 22 ID đá chính đúng và 4 nhóm dự bị/vai trò/phút thay người khớp; không chồng thẻ, tràn ngang, lỗi JavaScript hoặc request ghi. Tab đội tiêu biểu ở cả hai kích thước mặc định theo Vòng thi GW9, chọn GW8 xem đủ 11 ID/vị trí/rating/tổng và vẫn xem lại GW7; 38 lựa chọn. Kiểm `browser-report.json` đủ 20 checks/10 fixture và 2 checks đội tiêu biểu, không suy từ câu log.
- Readiness bằng service thật đọc Aiven chỉ còn `GW_NOT_OPENED`, participants=0/currentVersion=0/history rỗng; không còn blocker dữ liệu fixture. Đây là kết quả đúng yêu cầu bỏ qua cuộc thi GW8 chưa mở, không ghi nhận ready=true hoặc tạo cuộc thi/kết quả GW8 để vượt kiểm tra. Dữ liệu trận GW8 đã đủ, không công bố Fantasy.
- Kiểm chứng bằng các helper operational trong `backend/target/gw8-collection/`: `python .../finish.py local`, `python .../finish.py apply`, `python .../verify-finish.py` đều đạt. H2/production đều nhập lặp không đổi; không chạy Maven full suite/build cho thay đổi chỉ dữ liệu. Artifacts/backup được Git ignore; chưa commit, push hoặc deploy.
- GW9 giữ nguyên cấu hình đã công bố: deadline 30/10/2026 00:00 Việt Nam, rosterAsOf 26/10. GW8 không mở/công bố Fantasy; không thay đội/deadline/roster hoặc kết quả của bất kỳ GW nào.
- Bước tiếp theo: người chơi lưu đội GW9 trước deadline; khi có trận/ảnh cuối GW9 thì thu thập tiếp theo workflow. Không còn thiếu dữ liệu trận GW8 trong phạm vi đã giao; không mở bù hoặc công bố Fantasy GW8. Giữ 20 người PLAYED thiếu quyền vị trí ngoài đội tiêu biểu, không tự gán quyền để tăng độ phủ.

## Checkpoint ngày 26/10/2026 — lịch sử trước khi duyệt Teddy

Người dùng đã yêu cầu tiếp tục toàn bộ quy trình GW8 từ dữ liệu máy khác, bỏ qua công bố Fantasy GW8 vì chưa mở cuộc thi, rồi bắt đầu Fantasy GW9. Yêu cầu này thay giới hạn chỉ lưu ảnh của checkpoint ban đầu dưới đây. Không mở bù GW8, tạo đội/kết quả GW8 hoặc công bố/tái công bố vòng nào; việc mở GW9 được cho phép riêng trong yêu cầu này.

- Đã xác nhận lại cấu hình Render trỏ Aiven `defaultdb`, UUID `715946cc-c33a-11f1-8fd9-8e0831c19405`, TLS VERIFY_IDENTITY; không dùng Railway cũ.
- Đã dùng lại nguyên bản 400 dòng ảnh, không thu thập lại rating. Tải Player Stats/sự kiện/danh sách cuối StatMuse 10 trận để đối chiếu phút, bàn, kiến tạo, thẻ và danh tính.
- football-data.org toàn mùa ban đầu trả 5 FINISHED/5 SCHEDULED trong GW8. Đối chiếu endpoint chính thức `competitions/PL/matches?season=2026&matchday=8` ngày 26/10 xác nhận đủ 10 FINISHED và tỉ số khớp StatMuse; cập nhật batch chuẩn bị từ chính payload provider, không tự suy FINISHED từ ảnh. BXH tải về có tổng played=160 (80 trận).
- GW9 có đủ 10 timestamp UTC; trận đầu 31/10/2026 12:30 UTC, deadline tính theo luật là 30/10/2026 00:00 Việt Nam (29/10 17:00 UTC). Chưa mở GW9 ở bước kiểm tra này.
- Đang đối chiếu hồ sơ/membership: Junior Kroupi đã có ID 2000030156; Anthony Elanga cần bổ sung khoảng hiện hành có căn cứ trận 25/10, không sửa khoảng cũ. Teddy Sharman-Lowe (dự bị Chelsea) chưa có ID; đã hỏi quyền bổ sung hồ sơ tối thiểu theo mục 2 workflow, giữ OVR/quyền vị trí Fantasy chưa xác minh trống.
- Đã kiểm tra local/importer, backup và nhập production **9/10 trận**: 360 thống kê, 18 đội hình và 3 xác nhận unrated; chạy lặp không đổi. MySQL/API từng ô và Chrome cả 9 sân desktop/mobile đã đối chiếu đạt. Chelsea–Tottenham 1000560617 chờ quyền hồ sơ Teddy; ID 2000030261 trong bản chuẩn bị chỉ là đề xuất, chưa có trong Aiven. Tổng mùa vẫn qua GW7, không tuyên bố GW8 hoàn tất. GW9 đã mở production riêng theo yêu cầu, không mở/công bố Fantasy GW8. Chi tiết bên dưới.

## Checkpoint ban đầu ngày 26/10/2026 — lịch sử trước yêu cầu tiếp tục

**Đang dừng theo yêu cầu mới nhất của người dùng: chỉ lưu dữ liệu 20 ảnh, chưa nhập database.** Không tiếp tục thu thập nguồn ngoài, đồng bộ lịch/BXH, kiểm tra hoặc ghi database, mở GW9, hay công bố Fantasy trong lượt này.

- Đã chép đủ 10 cặp ảnh sân + danh sách dự bị, cả hai đội mỗi trận: **400 dòng**, gồm **220 STARTER, 83 SUB_USED, 97 SUB_UNUSED**. Đây là số người thực tế nhìn thấy trong ảnh, không thêm người để đạt định mức.
- 303 người xuất hiện trên sân; 300 người có rating hiển thị và 3 người không hiện rating trong bảng cuối. Rating chỉ lấy từ ảnh người dùng, không dùng rating StatMuse hoặc OVR.
- Đã lưu số áo, tên hiển thị, vai trò, rating, phút vào/ra, số áo người được thay, vị trí tương đối trên ảnh và những ký hiệu đọc được. Chưa có phút thi đấu hoặc bộ chỉ số trận đã xác minh.
- Chưa chạy importer, chưa nhập local/production, chưa tạo backup database. Chưa kiểm tra membership hoặc tổng mùa/Đội hình tiêu biểu/readiness. Không có thao tác ghi database, commit, push hoặc deploy.
- Kiểm tra CSV bằng Python: đủ 10 fixture/20 đội, mỗi đội 11 đá chính; các cặp thay người khớp số áo và phút; rating trong khoảng 0–10; 3 ô rating trống đúng các trường hợp dưới đây. Đây là kiểm tra bản chép ảnh, không phải validation của importer.
- Ảnh gốc được tham chiếu theo thứ tự trong tin nhắn người dùng ngày 26/10/2026. Thư mục này lưu **dữ liệu đã chép**, chưa có file ảnh gốc độc lập.

## Các file đã lưu

| File | Nội dung |
| --- | --- |
| `screenshot-evidence.csv` | Bản chép 400 người từ ảnh; có tên/số áo, không có `player_id` chưa xác minh. |
| `screenshot-fixtures.csv` | Ánh xạ cặp ảnh với fixture/CLB của API công khai, lịch UTC và trạng thái API tại thời điểm đọc; sơ đồ đọc từ bố trí sân. |
| `missing.csv` | Các bước xác minh còn hoãn theo yêu cầu dừng, không phải lỗi thiếu ảnh. |
| `sources.md` | Nguồn, quy ước đọc CSV và checkpoint tiếp tục. |

Hai CSV `screenshot-*.csv` là bằng chứng thu thập, **không phải đầu vào trực tiếp** của ManualMatchStatsImporter, LineupBatchImporter hoặc FantasyEvidenceImporter. Chưa tạo CSV thống kê cuối 10 cột, sidecar theo `player_id` hay bộ ba CSV đội hình để nhập.

## Tham chiếu ảnh

Nguồn chung: `USER_SCREENSHOT: GW8; supplied 2026-10-26; images 1–20 in user message`. Mỗi cặp gồm ảnh số lẻ là sân/đá chính, ảnh số chẵn là danh sách dự bị của cả hai đội.

| Ảnh | Fixture nội bộ từ API | Trận | Sơ đồ theo bố trí sân, chủ/khách |
| --- | --- | --- | --- |
| 1–2 | 1000560620 | Ipswich Town – Nottingham Forest | 4-2-3-1 / 3-4-2-1 |
| 3–4 | 1000560614 | Aston Villa – Manchester City | 4-2-3-1 / 4-2-3-1 |
| 5–6 | 1000560616 | Arsenal – Everton | 4-2-3-1 / 4-2-3-1 |
| 7–8 | 1000560618 | Coventry City – Fulham | 4-2-3-1 / 4-2-3-1 |
| 9–10 | 1000560617 | Chelsea – Tottenham Hotspur | 4-2-3-1 / 4-2-3-1 |
| 11–12 | 1000560613 | Crystal Palace – Newcastle United | 3-4-2-1 / 4-2-3-1 |
| 13–14 | 1000560619 | Hull City – Brentford | 4-2-3-1 / 4-2-3-1 |
| 15–16 | 1000560612 | Liverpool – Brighton | 4-2-3-1 / 4-2-3-1 |
| 17–18 | 1000560615 | Manchester United – Bournemouth | 3-4-2-1 / 4-2-3-1 |
| 19–20 | 1000560621 | Sunderland – Leeds United | 4-2-3-1 / 3-4-2-1 |

## Quy ước của bản chép

- `display_name` giữ tên viết tắt/biến thể trên ảnh; chưa tự gộp danh tính hoặc đổi sang tên trong database. Không dùng tên/số áo làm khóa nhập khi chưa đối chiếu `player_id` và membership ngày trận.
- `STARTER`: trên sân ở ảnh số lẻ; `SUB_USED`: bảng dự bị có phút vào và người ra; `SUB_UNUSED`: có trong bảng dự bị cuối nhưng không có dấu vào sân. Chưa chuyển vai trò thành dòng thống kê SQL.
- `substitution_in_minute`/`substitution_out_minute` giữ đúng số trên ảnh, bao gồm nhãn `90'`. Đây không phải thời lượng thi đấu; chưa có giây/phút bù giờ để tự bổ sung.
- `replaced_shirt_number` thuộc cùng CLB/fixture và khớp người được ghi sau `Out:`. Tên người ra đối chiếu trong cùng bản chép.
- `pitch_row_index`: hàng 0 là thủ môn, sau đó các tuyến từ hậu vệ đến tiền đạo. `pitch_slot_index`: thứ tự từ trên xuống trong cùng tuyến **đúng chiều ảnh**, bắt đầu 0, cho cả đội bên trái và bên phải. Không tự đổi thành vị trí LB/RB hoặc eligiblePositions. Dự bị để trống hai cột này.
- `visible_symbols` chỉ lưu biểu tượng nhìn thấy: `CAPTAIN`, `YELLOW_CARD`, `GOAL`, `GOAL_X2`, `OWN_GOAL`, `ASSIST`, `PLAYER_OF_MATCH`, `INJURY`, `INJURY_SUBSTITUTION`, `NO_RATING_SHOWN`. Các biểu tượng bàn/phản lưới/kiến tạo/thẻ cần được đối chiếu bộ chỉ số trước khi nhập thống kê. Ô trống không xác nhận chỉ số bằng 0; dấu chấn thương chỉ mô tả ký hiệu trên ảnh, không suy chẩn đoán hoặc lý do vắng người khác.
- Ký hiệu bóng đỏ cạnh M. Penders (ảnh 9) và P. Groß (ảnh 15) được ghi `OWN_GOAL`, không cộng thành bàn ghi được hoặc tự coi là thẻ đỏ.
- Số áo 8 xuất hiện ở cả H. Wilson trên sân và Sean Longstaff ở dự bị Leeds trong ảnh 19–20. Giữ nguyên hai dòng nguồn, không gộp hoặc tự sửa số áo; phải đối chiếu danh tính bằng tên/ID khi tiếp tục.
- Không lấy tổng phút = 90 trừ phút thay người; không gán rating 0; không suy bàn/kiến tạo/thẻ 0 từ việc không thấy biểu tượng.

## Ba người không hiện rating

| Fixture | CLB | Người trên ảnh | Căn cứ |
| --- | --- | --- | --- |
| 1000560620 | Nottingham Forest | Chris Wood, số 9 | Ảnh 2: vào 90', thay L. Delap; không có ô rating. |
| 1000560613 | Newcastle United | Malick Thiaw, số 12 | Ảnh 12: vào 90', thay A. Dedić; không có ô rating. |
| 1000560612 | Brighton | F. Kadıoğlu, số 24 | Ảnh 15: đá chính, ô rating là dấu gạch; ảnh 16: Costinha vào 6' thay người này. |

Giữ rating trống, vai trò ra sân theo ảnh. Khi được phép tiếp tục, đối chiếu `player_id` rồi tạo `confirmed-unrated.csv` theo quy tắc thường trực; không tạo bằng chứng SQL hoặc tính điểm trong bước chỉ lưu ảnh này.

## Thông tin API đã đọc trước yêu cầu dừng

- [Danh sách trận GW8](https://premierhub.vercel.app/api/matches?season=2026&matchweek=8): đủ 10 fixture/20 CLB; tại lần đọc ngày 26/10/2026 cả 10 vẫn `SCHEDULED`, tỉ số NULL, `hasManualStats=false`. Chỉ lưu trạng thái đã quan sát, không coi là trạng thái cuối hoặc tự đổi FINISHED theo ảnh.
- [Danh sách vòng thi](https://premierhub.vercel.app/api/fantasy/2026/gameweeks): GW8 và GW9 `configured=false`, deadline/rosterAsOf chưa được công bố; recommendedGameweek=7. Không mở/cấu hình lại vòng nào.
- File cấu hình hiện có `backend/.env.local` trỏ Railway cũ, chưa có `FOOTBALL_DATA_API_KEY`. Chưa xác minh được kết nối Aiven hiện hành; không dùng Railway để nhập production.
- Trước yêu cầu dừng đã thử đọc nguồn công khai và tìm một số trang StatMuse. Chưa lấy đủ bộ Player Stats hoặc nhập chỉ số từ các kết quả đó vào CSV này. Tiếp tục sau phải dùng nguồn hiện hành và ghi căn cứ theo từng trận.

## Bước tiếp theo của checkpoint ban đầu — đã được yêu cầu tiếp tục

1. Đọc lại checkpoint này và dùng nguyên bản chép ảnh, không bắt người dùng cung cấp lại các rating/danh sách đã lưu.
2. Xác định cấu hình Aiven của Render và khóa football-data.org qua file/env được ignore; không dùng endpoint Railway cũ hoặc yêu cầu gửi bí mật vào chat. Đối chiếu mapping provider, trạng thái kết thúc và dữ liệu mùa của mã sync hiện có.
3. Đối chiếu đầy đủ danh tính/membership; thu thập phút/bàn/kiến tạo/thẻ theo quy trình; tạo CSV thống kê cuối, bộ CSV đội hình và bằng chứng unrated/vắng mặt nếu có căn cứ.
4. Chỉ sau xác minh đích, dữ liệu và backup mới mới nhập theo quy trình được giao; kiểm tra lặp, tổng mùa, API/sân/Đội hình tiêu biểu và readiness. Chưa được coi GW8 đã hoàn tất ở checkpoint này.
5. GW9 chờ chỉ dẫn tiếp theo của người dùng; không tự mở hoặc công bố kết quả.

## Thực hiện tiếp trên máy này — 26/10/2026

- Đã chuẩn bị một CSV cuối `manual-match-stats-2026-GW8.csv` đủ 400 dòng, 303 PLAYED, 97 DID_NOT_PLAY, 300 rating từ ảnh và 3 PLAYED confirmed-unrated. **Đây là bản chuẩn bị đủ 10 trận; ID 2000030261 của Teddy Sharman-Lowe đang là đề xuất, chưa được tạo trên production khi chưa có xác nhận người dùng. Không nhập toàn bộ bản này trước khi xử lý quyền hồ sơ mới.** Không sửa hai CSV screenshot gốc.
- `manual-memberships-current-2026-GW8.csv` có hai khoảng mở dự kiến: Anthony Elanga 2000030070/Newcastle từ 25/10 và Teddy/Chelsea từ 24/10. Elanga đã có hồ sơ/dòng mùa, khoảng cũ kết thúc 06/09 giữ nguyên; không tạo khoảng một ngày. Teddy chỉ được tạo players + dòng mùa GOALKEEPER + membership nếu được duyệt; không thêm OVR, hồ sơ FC27 hoặc eligiblePositions chưa xác minh.
- `lineups/` chứa 400 vai trò, 220 tọa độ đá chính, 20 observations GW8 và 40 observations cũ giữ nguyên để cập nhật tổng mẫu. Giữ default USER. Số ô ảnh là từ trên xuống; đội khách được phản chiếu về chiều trái–phải của đội trước khi ghi row/slot/match_position. Đối chiếu 112 vị trí biên với dữ liệu formation StatMuse; khác biệt riêng nhãn chiến thuật Manchester United không ghi đè sơ đồ 3-4-2-1 có trên ảnh.
- Ba confirmed-unrated được nhập theo quy tắc sẵn có: Chris Wood (Forest, vào 90'), Malick Thiaw (Newcastle, vào 90'), Ferdi Kadıoğlu (Brighton, đá chính/ra 6'). Giữ PLAYED, phút/thống kê từ nguồn và rating/fantasy_points thô NULL; không ghi rating 0 hay đổi thành DNP.
- Không có đội chốt GW8 vì cuộc thi chưa mở; `confirmed-not-registered.csv` chỉ header. Không tạo đội, điểm hay kết quả GW8 để bù vòng bỏ lỡ.
- Kiểm tra H2 cô lập bằng importer thật với bản sao dữ liệu nền: phương án đủ 10 trận/400 dòng đạt, chạy lặp membership/stats/lineup/evidence đều thay đổi 0; tổng mùa qua GW8 là 80 fixture/3.200 dòng/595 dòng mùa và updatedRows=0 khi lặp. Đây là kiểm chứng local của phương án có Teddy, không phải kết quả production.
- Đã kiểm tra riêng phương án 9 trận không chứa Teddy hoặc Chelsea–Tottenham: 360 thống kê/198 STARTER, một membership Elanga, 18 đội hình và 3 căn cứ unrated; chạy lặp không đổi. Service tổng mùa vẫn dừng GW7 do vòng 8 thiếu một trận thống kê; không lách validator tổng mùa bằng SQL.
- Backup production trước ghi: `backend/local-backups/gw8-and-start-gw9-20261026-121828/aiven-before-gw8-and-gw9.sql`, 1.002.361 byte, SHA-256 `a2adb2ccd32c1eeffb1c12feb7da70562140bd9557ab8b41fffdf9a20c0662a3`, UTC `2026-10-26T12:19:03.110865Z`. Đã kiểm DDL/số dòng đủ 21 bảng, Git ignore và marker hoàn tất; chưa thử restore.
- **GW9 đã mở production**, revision 1, rosterAsOf 26/10, deadline 30/10/2026 00:00 Việt Nam; xem [checkpoint GW9](../gw9-2026/sources.md). API và Chrome desktop/390px đã xác nhận mặc định GW9, deadline đúng và sân 11 ô, không tràn ngang/lỗi JavaScript/request ghi. Không công bố điểm, không sửa snapshot/cấu hình các vòng cũ.
- Nhập production 9 trận độc lập đã commit trong transaction SERIALIZABLE bằng importer hiện có: đồng bộ 20 fixture GW8/GW9 + BXH 20 CLB từ nguồn chính thức, thêm đúng một membership mở của Elanga (không thêm người/dòng mùa), 360 thống kê, 18 đội hình/198 STARTER, 9 xác nhận fixture và 3 unrated. Chạy lặp stats inserted=0; membership/lineup/evidence đều changes=0. Không tạo Teddy, nhập trận Chelsea–Tottenham hoặc sửa cấu hình GW9.
- Tổng mùa qua GW7 lặp: fixtures=70, matchRows=2800, seasonRows=594, updatedRows=0; giữ 1 missingMinutes, 2 missingGoals và 4 missingAssists lịch sử dưới dạng NULL. Chưa tổng hợp qua GW8 vì thiếu 40 dòng của fixture 1000560617; không chạy SQL tổng mùa để lách cơ chế hiện có.
- Readiness GW8: participants=0/currentVersion=0, GW_NOT_OPENED là trạng thái được yêu cầu bỏ qua; các cảnh báo dữ liệu còn lại chỉ thuộc 1000560617 (lineup/starter/fixture chưa xác nhận). Đội hình tiêu biểu hiện tính được 11 người/tổng 93,50 từ dữ liệu **9/10 trận**, không coi COMPLETE của đội tiêu biểu là GW8 đầy đủ.
- Chelsea–Tottenham 1000560617 chưa nhập vì thiếu quyền tạo hồ sơ Teddy; câu hỏi đã gửi theo mục 2 workflow, không tự coi im lặng là đồng ý. Sau khi có xác nhận: kiểm tra ID đề xuất còn trống, backup mới nếu backup hiện tại không còn phù hợp, nhập phần còn lại bằng importer, lặp changes=0, tái tính tổng mùa qua GW8 và đối chiếu API/sân/Đội hình tiêu biểu đủ 10/10. Giữ nguyên GW9 đã mở, không công bố Fantasy GW8.
- Không thay mã ứng dụng/schema/dependency/cấu hình Render/Aiven; không chạy Maven full suite/build vì chỉ nhập dữ liệu. Chưa commit, push hoặc deploy. Kiểm chứng importer trên H2 và lần chạy production được ghi riêng, không gọi đó là test JUnit toàn ứng dụng.
- Sau commit đã đọc lại MySQL: mọi ô của 360 thống kê/360 vai trò và 18 summary/formation mới khớp CSV; giữ thống kê/đội hình GW1–7, mùa 2024/25, OVR/quyền vị trí, đội/kết quả/deadline/rosterAsOf cũ và cấu hình GW9. Elanga có đúng một khoảng mở mới; không tạo người mới hay thay khoảng cũ. Dòng mùa qua GW7 vẫn khớp phép cộng độc lập, số dòng mùa 2026=594 không đổi.
- API công khai: 10 fixture GW8 FINISHED, đúng 9 hasManualStats; chi tiết 9 trận khớp tập ID, status, phút/bàn/kiến tạo/thẻ, rating/fantasyPoints, formation/vai trò/tọa độ/phút thay người. Ba unrated giữ PLAYED/rating/fantasyPoints NULL. Lịch sử và roster hiện tại của Chris Wood 2000020061, Elanga 2000030070, Malick Thiaw 2000030131, Ferdi Kadıoğlu 2000030167 đã đối chiếu.
- Chrome production: **18 lượt sân = 9 fixture × 1440px/390px**, đúng 22 STARTER và 4 nhóm dự bị mỗi sân, không tràn ngang/chồng thẻ/lỗi JavaScript/request ghi. Hai lượt tab đội tiêu biểu mặc định theo GW9, chọn riêng GW8 để xem 11 ID/vị trí/rating/tổng đúng API, vẫn chọn lại được GW7; 38 lựa chọn.
- Artifacts local/production/API/browser ở `backend/target/gw8-collection/`, được Git ignore. Bước tiếp tục duy nhất về dữ liệu là hồ sơ Teddy và nhập 40 dòng/2 đội hình còn lại; không thu thập lại 9 trận đã nhập, không mở lại hoặc đổi deadline/rosterAsOf GW9.
- Delta dự kiến sau khi Teddy được duyệt và ID còn trống: membership importer thêm 1 player/1 dòng mùa/1 khoảng, không sửa Elanga; CSV cuối 400 dòng chỉ thêm 40 thống kê còn thiếu, lineup thêm 40 vai trò/2 đội và evidence thêm 1 fixture, không thêm unrated. Sau đó tổng mùa qua GW8 có 80 fixture/3.200 dòng/595 dòng mùa và chạy lặp updatedRows=0. Đây là số dự kiến từ kiểm chứng local, chưa thực hiện ở production.

### Nguồn thống kê trận và đối chiếu ID

Mỗi trang StatMuse dưới đây được tải ngày 26/10/2026. Phút/bàn/kiến tạo/thẻ lấy từ Player Stats, vai trò và sự kiện được đối chiếu với bản chép ảnh; tổng từng đội khớp nguồn. Bàn phản lưới của Mike Penders/Pascal Groß không cộng vào goals cá nhân. Dự bị không vào sân chỉ ghi sự kiện 0 sau khi tổng/sự kiện xác minh, không suy từ ô ảnh trống. Rating luôn từ ảnh, không lấy rating StatMuse.

Các biến thể tên đã ghép bằng danh tính/CLB, source player ID, danh sách và số áo (không dùng số áo đơn độc): V. Lindelöf = Victor Nilsson-Lindelöf; U. Namzambi = Johan Manzambi; C. Andrés = Chema/José María Andrés Baixauli 2000030174 (giữ ngoại lệ ID đã chốt GW6); R. Mandava = Reinildo; Juan Riquelme Angulo = Riquelme Angulo; Tom King = Thomas King; Mahdi Nicoll-Jazuli = Mahdi Jazuli; J. P. van Hecke = Jan Paul van Hecke; Junior Kroupi 2000030156 = Eli Junior Kroupi; J. Philogene-Bidace = Jaden Philogene; J. Enciso = Julio César Enciso; A. F. Issahaku = Abdul Fatawu; F. Luís = Florentino; A. Ouattara = Guimissongui Ouattara; Emerson = Emersonn; John Victor = John; Kaine Kesler-Hayden = Kaine Kessler-Hayden (giữ ID 2000030179); M. v. Ewijk = Milan van Ewijk; Manuel Ángel Morán = Manuel Ángel; Matt Crooks = Matty Crooks; M. Belloumi = Bachir Belloumi. Harry Wilson và Sean Longstaff cùng số áo 8 trong bản ảnh Leeds vẫn là hai ID riêng, không gộp/sửa dữ liệu nguồn.

| Fixture | Provider | Match | Score | Source |
| --- | --- | --- | --- | --- |
| 1000560612 | 560612 | Liverpool FC - Brighton & Hove Albion FC | 2-0 | https://www.statmuse.com/fc/match/-112834 |
| 1000560613 | 560613 | Crystal Palace FC - Newcastle United FC | 0-2 | https://www.statmuse.com/fc/match/-112841 |
| 1000560614 | 560614 | Aston Villa FC - Manchester City FC | 2-1 | https://www.statmuse.com/fc/match/-112833 |
| 1000560615 | 560615 | Manchester United FC - AFC Bournemouth | 0-3 | https://www.statmuse.com/fc/match/-112838 |
| 1000560616 | 560616 | Arsenal FC - Everton FC | 2-0 | https://www.statmuse.com/fc/match/-112836 |
| 1000560617 | 560617 | Chelsea FC - Tottenham Hotspur FC | 0-2 | https://www.statmuse.com/fc/match/-112837 |
| 1000560618 | 560618 | Coventry City FC - Fulham FC | 0-1 | https://www.statmuse.com/fc/match/-112840 |
| 1000560619 | 560619 | Hull City AFC - Brentford FC | 1-1 | https://www.statmuse.com/fc/match/-112842 |
| 1000560620 | 560620 | Ipswich Town FC - Nottingham Forest FC | 1-1 | https://www.statmuse.com/fc/match/-112839 |
| 1000560621 | 560621 | Sunderland AFC - Leeds United FC | 0-0 | https://www.statmuse.com/fc/match/-112835 |

### Verified player rows — 400 rows imported; Teddy authorized 27/10/2026

| Fixture | Club | Player ID | Name | Role | Rating | Minutes | Goals | Assists | Yellow | Red |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1000560612 | Liverpool FC | 2000003001 | Alisson Becker | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003006 | Milos Kerkez | STARTER | 6.9 | 87 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003004 | Virgil van Dijk | STARTER | 7.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003005 | Jeremy Jacquet | STARTER | 8.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003019 | Ronald Araujo | STARTER | 8.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003010 | Alexis Mac Allister | STARTER | 7.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003020 | Ryan Gravenberch | STARTER | 7.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003011 | Cody Gakpo | STARTER | 7.1 | 90 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003007 | Florian Wirtz | STARTER | 7.4 | 64 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003017 | Bradley Barcola | STARTER | 7.8 | 76 | 0 | 1 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003009 | Alexander Isak | STARTER | 7.3 | 76 | 1 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003002 | Joe Gomez | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003003 | Wataru Endo | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003012 | Kostas Tsimikas | SUB_USED | 6.6 | 3 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003015 | Giorgi Mamardashvili | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003021 | Trey Nyoni | SUB_USED | 7.0 | 26 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003022 | James McConnell | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003023 | Lewis Koumas | SUB_USED | 6.5 | 14 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000003024 | Rio Ngumoha | SUB_USED | 6.4 | 14 | 0 | 0 | 0 | 0 |
| 1000560612 | Liverpool FC | 2000030231 | Federico Chiesa | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030055 | Bart Verbruggen | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030167 | Ferdi Kadıoğlu | STARTER | NULL | 5 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030046 | Luka Vušković | STARTER | 6.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030045 | Lewis Dunk | STARTER | 7.1 | 90 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030117 | Olivier Boscagli | STARTER | 5.7 | 45 | 0 | 0 | 1 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030049 | Pascal Groß | STARTER | 5.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030174 | José María Andrés Baixauli | STARTER | 6.8 | 53 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030043 | Diego Gómez | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030048 | Yasin Ayari | STARTER | 6.6 | 74 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030042 | Maxim De Cuyper | STARTER | 6.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030051 | Charalampos Kostoulas | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030041 | Jack Hinshelwood | SUB_USED | 6.5 | 45 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030044 | Georginio Rutter | SUB_USED | 6.9 | 16 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030050 | Costinha | SUB_USED | 7.5 | 85 | 0 | 0 | 1 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030052 | Ibrahim Osman | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030054 | Malick Yalcouyé | SUB_USED | 7.0 | 37 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030118 | Jason Steele | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030121 | Promise David | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030169 | Matt O'Riley | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560612 | Brighton & Hove Albion FC | 2000030173 | Jaouen Hadjam | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560613 | Newcastle United FC | 2000030074 | Lukáš Horníček | STARTER | 8.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560613 | Newcastle United FC | 2000030081 | Amar Dedić | STARTER | 7.5 | 89 | 0 | 1 | 0 | 0 |
| 1000560613 | Newcastle United FC | 2000030075 | Fabian Schär | STARTER | 7.5 | 90 | 0 | 0 | 1 | 0 |
| 1000560613 | Newcastle United FC | 18961 | Dan Burn | STARTER | 7.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560613 | Newcastle United FC | 2000030128 | Lewis Hall | STARTER | 7.1 | 90 | 0 | 0 | 1 | 0 |
| 1000560613 | Newcastle United FC | 2000030106 | Nico González | STARTER | 7.7 | 90 | 0 | 0 | 1 | 0 |
| 1000560613 | Newcastle United FC | 723 | Joelinton | STARTER | 6.9 | 84 | 0 | 0 | 0 | 0 |
| 1000560613 | Newcastle United FC | 2000030076 | Jacob Murphy | STARTER | 7.6 | 90 | 0 | 1 | 0 | 0 |
| 1000560613 | Newcastle United FC | 2000030071 | Joe Willock | STARTER | 6.3 | 76 | 0 | 0 | 0 | 0 |
| 1000560613 | Newcastle United FC | 2000030129 | Harvey Barnes | STARTER | 7.4 | 76 | 1 | 0 | 0 | 0 |
| 1000560613 | Newcastle United FC | 2000030073 | William Osula | STARTER | 7.3 | 76 | 1 | 0 | 0 | 0 |
| 1000560613 | Newcastle United FC | 18911 | Nick Pope | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560613 | Newcastle United FC | 2000030070 | Anthony Elanga | SUB_USED | 6.7 | 14 | 0 | 0 | 0 | 0 |
| 1000560613 | Newcastle United FC | 2000030072 | Yoane Wissa | SUB_USED | 6.8 | 14 | 0 | 0 | 0 | 0 |
| 1000560613 | Newcastle United FC | 2000030077 | Aladji Bamba | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560613 | Newcastle United FC | 2000030079 | Jacob Ramsey | SUB_USED | 6.5 | 14 | 0 | 0 | 0 | 0 |
| 1000560613 | Newcastle United FC | 2000030080 | Sean Steur | SUB_USED | 6.5 | 6 | 0 | 0 | 0 | 0 |
| 1000560613 | Newcastle United FC | 2000030130 | Sven Botman | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560613 | Newcastle United FC | 2000030131 | Malick Thiaw | SUB_USED | NULL | 1 | 0 | 0 | 0 | 0 |
| 1000560613 | Newcastle United FC | 2000030134 | Bazoumana Touré | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 2000020114 | Walter Benítez | STARTER | 5.4 | 90 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 2000020096 | Jaydee Canvot | STARTER | 6.5 | 90 | 0 | 0 | 1 | 0 |
| 1000560613 | Crystal Palace FC | 2000030182 | Axel Disasi | STARTER | 6.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 2000020099 | Chris Richards | STARTER | 6.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 2000020103 | Tyrick Mitchell | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 2000016001 | Daichi Kamada | STARTER | 6.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 2000020098 | Adam Wharton | STARTER | 6.1 | 90 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 2000016002 | Anan Khalaili | STARTER | 7.3 | 90 | 0 | 0 | 1 | 0 |
| 1000560613 | Crystal Palace FC | 2000020105 | Yéremy Pino | STARTER | 6.6 | 90 | 0 | 0 | 1 | 0 |
| 1000560613 | Crystal Palace FC | 2000020097 | Dwight McNeil | STARTER | 6.8 | 62 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 2000020102 | Jean-Philippe Mateta | STARTER | 6.2 | 61 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 2490 | Jefferson Lerma | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 3339 | Cheick Doucouré | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 2000020101 | Eddie Nketiah | SUB_USED | 6.2 | 29 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 2000020107 | Evann Guessand | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 2000020116 | Óscar Mingueza | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 2000020117 | Will Hughes | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 2000030184 | Remi Matthews | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 2000030187 | Ben Chilwell | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560613 | Crystal Palace FC | 2000030189 | Darío Osorio | SUB_USED | 7.1 | 28 | 0 | 0 | 0 | 0 |
| 1000560614 | Aston Villa FC | 2000030124 | Zion Suzuki | STARTER | 9.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560614 | Aston Villa FC | 2000030064 | Ian Maatsen | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560614 | Aston Villa FC | 2000030066 | Pau Torres | STARTER | 6.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560614 | Aston Villa FC | 2000030056 | Victor Lindelöf | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560614 | Aston Villa FC | 2000030058 | Matty Cash | STARTER | 6.7 | 90 | 0 | 0 | 1 | 0 |
| 1000560614 | Aston Villa FC | 2000030057 | João Gomes | STARTER | 6.7 | 81 | 0 | 0 | 0 | 0 |
| 1000560614 | Aston Villa FC | 2000030122 | Boubacar Kamara | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560614 | Aston Villa FC | 2000030146 | Johan Manzambi | STARTER | 6.9 | 65 | 0 | 0 | 1 | 0 |
| 1000560614 | Aston Villa FC | 2000030059 | John McGinn | STARTER | 7.4 | 65 | 0 | 1 | 0 | 0 |
| 1000560614 | Aston Villa FC | 2000030062 | Alysson | STARTER | 6.6 | 58 | 0 | 0 | 0 | 0 |
| 1000560614 | Aston Villa FC | 283058 | Nicolas Jackson | STARTER | 9.3 | 81 | 2 | 0 | 0 | 0 |
| 1000560614 | Aston Villa FC | 2000030061 | Emiliano Buendía | SUB_USED | 6.5 | 25 | 0 | 0 | 1 | 0 |
| 1000560614 | Aston Villa FC | 2000030065 | Tyrone Mings | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560614 | Aston Villa FC | 2000030067 | Lamare Bogarde | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560614 | Aston Villa FC | 2000030068 | Ross Barkley | SUB_USED | 6.6 | 25 | 0 | 0 | 0 | 0 |
| 1000560614 | Aston Villa FC | 2000030069 | Marco Bizot | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560614 | Aston Villa FC | 2000030123 | George Hemmings | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560614 | Aston Villa FC | 2000030148 | Brian Madjo | SUB_USED | 6.7 | 9 | 0 | 0 | 1 | 0 |
| 1000560614 | Aston Villa FC | 2000030149 | Leon Goretzka | SUB_USED | 6.5 | 9 | 0 | 0 | 0 | 0 |
| 1000560614 | Aston Villa FC | 2000030150 | Ibrahim Mbaye | SUB_USED | 6.6 | 32 | 0 | 1 | 0 | 0 |
| 1000560614 | Manchester City FC | 2000004001 | Gianluigi Donnarumma | STARTER | 4.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560614 | Manchester City FC | 2000004021 | Abdukodir Khusanov | STARTER | 7.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560614 | Manchester City FC | 2000004004 | Marc Guehi | STARTER | 5.8 | 45 | 0 | 0 | 1 | 0 |
| 1000560614 | Manchester City FC | 2000004002 | Ruben Dias | STARTER | 6.4 | 90 | 0 | 0 | 0 | 0 |
| 1000560614 | Manchester City FC | 2000004014 | Josko Gvardiol | STARTER | 6.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560614 | Manchester City FC | 2000004003 | Elliot Anderson | STARTER | 7.5 | 90 | 0 | 0 | 1 | 0 |
| 1000560614 | Manchester City FC | 2000004011 | Enzo Fernandez | STARTER | 7.1 | 90 | 0 | 0 | 0 | 0 |
| 1000560614 | Manchester City FC | 2000004005 | Iliman Ndiaye | STARTER | 7.5 | 68 | 0 | 1 | 0 | 0 |
| 1000560614 | Manchester City FC | 2000004008 | Rayan Cherki | STARTER | 6.1 | 45 | 0 | 0 | 1 | 0 |
| 1000560614 | Manchester City FC | 2000004009 | Jeremy Doku | STARTER | 7.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560614 | Manchester City FC | 2000004007 | Erling Haaland | STARTER | 7.9 | 90 | 1 | 0 | 0 | 0 |
| 1000560614 | Manchester City FC | 2000004006 | Mateo Kovacic | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560614 | Manchester City FC | 2000004012 | Rayan Ait-Nouri | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560614 | Manchester City FC | 2000004013 | Vitor Reis | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560614 | Manchester City FC | 2000004016 | Geronimo Rulli | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560614 | Manchester City FC | 2000004017 | Ayyoub Bouaddi | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560614 | Manchester City FC | 2000004018 | Nico O'Reilly | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560614 | Manchester City FC | 2000004022 | Phil Foden | SUB_USED | 6.6 | 45 | 0 | 0 | 0 | 0 |
| 1000560614 | Manchester City FC | 2000004023 | Ryan McAidoo | SUB_USED | 6.8 | 22 | 0 | 0 | 1 | 0 |
| 1000560614 | Manchester City FC | 2000004025 | Rico Lewis | SUB_USED | 6.1 | 45 | 0 | 0 | 0 | 0 |
| 1000560615 | Manchester United FC | 2000006001 | Senne Lammens | STARTER | 5.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560615 | Manchester United FC | 2000006023 | Ayden Heaven | STARTER | 6.2 | 64 | 0 | 0 | 1 | 0 |
| 1000560615 | Manchester United FC | 2000006005 | Harry Maguire | STARTER | 6.6 | 90 | 0 | 0 | 1 | 0 |
| 1000560615 | Manchester United FC | 2000006014 | Leny Yoro | STARTER | 6.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560615 | Manchester United FC | 2000006002 | Diogo Dalot | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560615 | Manchester United FC | 2000006019 | Carlos Baleba | STARTER | 6.6 | 71 | 0 | 0 | 0 | 0 |
| 1000560615 | Manchester United FC | 2000006016 | Andrey Santos | STARTER | 6.9 | 90 | 0 | 0 | 1 | 0 |
| 1000560615 | Manchester United FC | 2000006015 | Amad Diallo | STARTER | 6.8 | 77 | 0 | 0 | 0 | 0 |
| 1000560615 | Manchester United FC | 2000006007 | Mason Mount | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560615 | Manchester United FC | 2000006017 | Youri Tielemans | STARTER | 5.9 | 45 | 0 | 0 | 1 | 0 |
| 1000560615 | Manchester United FC | 2000006018 | Bryan Mbeumo | STARTER | 6.4 | 90 | 0 | 0 | 0 | 0 |
| 1000560615 | Manchester United FC | 2000006003 | Noussair Mazraoui | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560615 | Manchester United FC | 2000006004 | Matthijs de Ligt | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560615 | Manchester United FC | 2000006006 | Lisandro Martinez | SUB_USED | 7.3 | 26 | 0 | 0 | 0 | 0 |
| 1000560615 | Manchester United FC | 2000006008 | Bruno Fernandes | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560615 | Manchester United FC | 2000006010 | Matheus Cunha | SUB_USED | 6.4 | 45 | 0 | 0 | 0 | 0 |
| 1000560615 | Manchester United FC | 2000006011 | Joshua Zirkzee | SUB_USED | 6.2 | 19 | 0 | 0 | 0 | 0 |
| 1000560615 | Manchester United FC | 2000006012 | Karl Darlow | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560615 | Manchester United FC | 2000006026 | Shea Lacey | SUB_USED | 6.7 | 13 | 0 | 0 | 0 | 0 |
| 1000560615 | Manchester United FC | 2000030236 | Tyler Fletcher | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030030 | Đorđe Petrović | STARTER | 7.1 | 90 | 0 | 0 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030108 | Adam Smith | STARTER | 6.9 | 87 | 0 | 0 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030109 | James Hill | STARTER | 9.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030035 | António Silva | STARTER | 7.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030111 | Adrien Truffert | STARTER | 7.5 | 90 | 0 | 1 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030036 | Tyler Adams | STARTER | 7.2 | 90 | 0 | 0 | 1 | 0 |
| 1000560615 | AFC Bournemouth | 2000030037 | Lewis Cook | STARTER | 6.9 | 74 | 0 | 1 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030033 | Rayan | STARTER | 7.8 | 90 | 1 | 0 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030039 | Justin Kluivert | STARTER | 6.6 | 62 | 0 | 1 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030031 | Marcus Tavernier | STARTER | 7.5 | 86 | 1 | 0 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030032 | Evanilson | STARTER | 7.4 | 86 | 1 | 0 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 1125 | Ryan Christie | SUB_USED | 7.5 | 16 | 0 | 0 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030034 | Ben Gannon-Doak | SUB_USED | 6.4 | 28 | 0 | 0 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030113 | Bafodé Diakité | SUB_USED | 6.8 | 3 | 0 | 0 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030115 | David Brooks | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030116 | Álvaro Rodriguez | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030154 | Julio Soler Barreto | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030155 | Amine Adli | SUB_USED | 6.4 | 4 | 0 | 0 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030156 | Junior Kroupi | SUB_USED | 6.4 | 4 | 0 | 0 | 0 | 0 |
| 1000560615 | AFC Bournemouth | 2000030157 | Michele Di Gregorio | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001001 | David Raya | STARTER | 7.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001024 | Myles Lewis-Skelly | STARTER | 7.4 | 67 | 0 | 0 | 1 | 0 |
| 1000560616 | Arsenal FC | 2000001006 | Gabriel Magalhaes | STARTER | 9.0 | 90 | 1 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001013 | Ezri Konsa | STARTER | 8.1 | 90 | 1 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001004 | Ben White | STARTER | 7.7 | 90 | 0 | 1 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001017 | Mikel Merino | STARTER | 6.6 | 67 | 0 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001023 | Declan Rice | STARTER | 8.2 | 79 | 0 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001009 | Eberechi Eze | STARTER | 6.9 | 79 | 0 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001008 | Martin Odegaard | STARTER | 8.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001015 | Noni Madueke | STARTER | 6.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001012 | Viktor Gyokeres | STARTER | 6.4 | 67 | 0 | 1 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001003 | Cristhian Mosquera | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001005 | Piero Hincapie | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001010 | Jurrien Timber | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001011 | Kepa Arrizabalaga | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001018 | Kai Havertz | SUB_USED | 6.6 | 23 | 0 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001020 | Riccardo Calafiori | SUB_USED | 6.9 | 23 | 0 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001021 | Martin Zubimendi | SUB_USED | 6.7 | 11 | 0 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001022 | Bruno Guimaraes | SUB_USED | 6.8 | 23 | 0 | 0 | 0 | 0 |
| 1000560616 | Arsenal FC | 2000001025 | Max Dowman | SUB_USED | 6.4 | 11 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 2000020085 | Jordan Pickford | STARTER | 7.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 2000020090 | Merlin Röhl | STARTER | 6.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 2000020089 | James Tarkowski | STARTER | 7.6 | 90 | 0 | 0 | 1 | 0 |
| 1000560616 | Everton FC | 2000020112 | Jake O'Brien | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 2000020086 | Vitaliy Mykolenko | STARTER | 6.4 | 73 | 0 | 0 | 1 | 0 |
| 1000560616 | Everton FC | 2000015004 | Harrison Armstrong | STARTER | 7.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 2000020091 | James Garner | STARTER | 6.9 | 90 | 0 | 0 | 1 | 0 |
| 1000560616 | Everton FC | 2000020093 | Brennan Johnson | STARTER | 6.2 | 71 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 2000015001 | Kiernan Dewsbury-Hall | STARTER | 6.7 | 85 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 2000020087 | Tyrique George | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 2000015002 | Thierno Barry | STARTER | 7.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 30407 | Christian Nørgaard | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 2000020088 | Hayden Hackney | SUB_USED | 6.8 | 17 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 2000020094 | Carlos Alcaraz | SUB_USED | 6.4 | 5 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 2000020109 | Mark Travers | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 2000020111 | Michael Keane | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 2000020113 | Tyler Dibling | SUB_USED | 6.9 | 19 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 2000030190 | Tom King | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 2000030191 | Ainsley Maitland-Niles | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560616 | Everton FC | 2000030192 | Braiden Graham | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000005023 | Mike Penders | STARTER | 5.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000005014 | Jorrel Hato | STARTER | 6.8 | 64 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000005006 | Levi Colwill | STARTER | 6.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000005005 | Maxence Lacroix | STARTER | 6.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000005022 | Josh Acheampong | STARTER | 6.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000005011 | Jordan Henderson | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000005018 | Moises Caicedo | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000005016 | Geovany Quenda | STARTER | 6.4 | 71 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000005009 | Cole Palmer | STARTER | 6.1 | 79 | 0 | 0 | 2 | 1 |
| 1000560617 | Chelsea FC | 2000005007 | Pedro Neto | STARTER | 6.8 | 87 | 0 | 0 | 1 | 0 |
| 1000560617 | Chelsea FC | 2000005012 | Morgan Rogers | STARTER | 5.8 | 90 | 0 | 0 | 1 | 0 |
| 1000560617 | Chelsea FC | 2000005002 | Marco Palestra | SUB_USED | 6.0 | 26 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000005003 | Wesley Fofana | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000005004 | Valentin Barco | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000005013 | Danny Welbeck | SUB_USED | 6.4 | 3 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000005019 | Malo Gusto | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000005021 | Mahdi Nicoll-Jazuli | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000005024 | Estevao | SUB_USED | 6.5 | 19 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000005025 | Romeo Lavia | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560617 | Chelsea FC | 2000030261 | Teddy Sharman-Lowe | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 2000030015 | Antonín Kinský | STARTER | 7.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 2000030017 | Archie Gray | STARTER | 7.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 2000030028 | Jan Paul van Hecke | STARTER | 7.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 152849 | Micky van de Ven | STARTER | 7.5 | 90 | 0 | 0 | 1 | 0 |
| 1000560617 | Tottenham Hotspur FC | 2000030027 | Andy Robertson | STARTER | 6.8 | 64 | 0 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 2000030100 | Sandro Tonali | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 2000030024 | Mateus Fernandes | STARTER | 7.0 | 90 | 0 | 0 | 1 | 0 |
| 1000560617 | Tottenham Hotspur FC | 15911 | Mohammed Kudus | STARTER | 7.0 | 88 | 0 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 2000030020 | James Maddison | STARTER | 6.8 | 50 | 0 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 2000030101 | Mathys Tel | STARTER | 8.1 | 88 | 1 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 2000030107 | Omar Marmoush | STARTER | 6.9 | 64 | 0 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 30435 | Dejan Kulusevski | SUB_USED | 6.7 | 2 | 0 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 47519 | Pedro Porro | SUB_USED | 6.7 | 26 | 0 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 2000030018 | Dominic Solanke | SUB_USED | 6.9 | 26 | 0 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 2000030025 | Conor Gallagher | SUB_USED | 6.9 | 40 | 0 | 0 | 1 | 0 |
| 1000560617 | Tottenham Hotspur FC | 2000030026 | Marcos Senesi | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 2000030102 | Martin Dúbravka | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 2000030104 | Ben Davies | SUB_USED | 6.6 | 2 | 0 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 2000030105 | Luca Williams-Barnett | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560617 | Tottenham Hotspur FC | 2000030251 | Callum Olusesi | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 2000030138 | Bernd Leno | STARTER | 8.1 | 90 | 0 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 2000030084 | Timothy Castagne | STARTER | 7.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 2000030197 | David Affengruber | STARTER | 7.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 2000030087 | Calvin Bassey | STARTER | 7.4 | 90 | 0 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 2000030086 | Antonee Robinson | STARTER | 6.3 | 90 | 0 | 0 | 1 | 0 |
| 1000560618 | Fulham FC | 2000030092 | Shea Charles | STARTER | 7.7 | 90 | 0 | 0 | 1 | 0 |
| 1000560618 | Fulham FC | 2000030195 | Hugo Larsson | STARTER | 6.7 | 62 | 0 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 2000030093 | Oscar Bobb | STARTER | 8.2 | 80 | 1 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 2000030083 | Joshua King | STARTER | 6.9 | 80 | 0 | 1 | 0 | 0 |
| 1000560618 | Fulham FC | 2000030136 | Alex Iwobi | STARTER | 7.2 | 73 | 0 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 2000030082 | Gonzalo García | STARTER | 6.6 | 73 | 0 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 2729 | Joachim Andersen | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 19025 | Tom Cairney | SUB_USED | 6.6 | 10 | 0 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 2000030085 | Rodrigo Muniz | SUB_USED | 6.7 | 17 | 0 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 2000030089 | Sander Berge | SUB_USED | 7.0 | 28 | 0 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 2000030090 | Kevin | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 2000030091 | César Palacios | SUB_USED | 6.7 | 17 | 0 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 2000030139 | Benjamin Lecomte | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 2000030140 | Kenny Tete | SUB_USED | 6.9 | 10 | 0 | 0 | 0 | 0 |
| 1000560618 | Fulham FC | 2000030196 | Manuel Ángel Morán Ibáñez | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002016 | Carl Rushworth | STARTER | 6.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002002 | Jay Dasilva | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002001 | Ethan Pinnock | STARTER | 7.1 | 90 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002003 | Bobby Thomas | STARTER | 7.1 | 59 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002024 | Milan van Ewijk | STARTER | 6.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002005 | Matt Grimes | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002007 | Caleb Yirenkyi | STARTER | 6.7 | 66 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002020 | Brandon Thomas-Asante | STARTER | 6.9 | 66 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002004 | Jack Rudoni | STARTER | 6.6 | 90 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002006 | Tatsuhiro Sakamoto | STARTER | 6.9 | 85 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002029 | Sidiki Cherif | STARTER | 6.5 | 66 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002009 | Ephron Mason-Clark | SUB_USED | 6.3 | 24 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002011 | Stephen Mfuni | SUB_USED | 6.4 | 31 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002013 | Taiwo Awoniyi | SUB_USED | 6.4 | 24 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002014 | Frank Onyeka | SUB_USED | 6.6 | 24 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002022 | Dan Bentley | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002026 | Victor Torp | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002027 | Yann Gboho | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000002028 | Gustavo Hamer | SUB_USED | 6.5 | 5 | 0 | 0 | 0 | 0 |
| 1000560618 | Coventry City FC | 2000030179 | Kaine Kesler-Hayden | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000010001 | Konstantinos Tzolakis | STARTER | 7.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000020005 | Ryan Giles | STARTER | 5.8 | 64 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000020010 | Lucas Herrington | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000010002 | Semi Ajayi | STARTER | 7.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000020004 | Lewie Coyle | STARTER | 6.6 | 64 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000010005 | John Egan | STARTER | 6.4 | 90 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 284500 | Tim Iroegbunam | STARTER | 7.0 | 71 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000020006 | Elliot Stroud | STARTER | 7.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000010004 | Regan Slater | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000020009 | Mohamed Belloumi | STARTER | 8.2 | 85 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000020008 | Oli McBurnie | STARTER | 7.2 | 85 | 1 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000020007 | Matt Crooks | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000020011 | Paddy McNair | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000020015 | Dillon Phillips | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000030205 | Joe Gelhardt | SUB_USED | 6.0 | 5 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000030206 | Hidemasa Morita | SUB_USED | 6.5 | 19 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000030207 | Mohamed-Ali Cho | SUB_USED | 7.2 | 26 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000030209 | Robinio Vaz | SUB_USED | 6.6 | 5 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000030210 | Brooke Norton-Cuffy | SUB_USED | 6.8 | 26 | 0 | 0 | 0 | 0 |
| 1000560619 | Hull City AFC | 2000030211 | Sorba Thomas | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030001 | Caoimhin Kelleher | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030004 | Michael Kayode | STARTER | 7.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030158 | Sepp van den Berg | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030095 | Kristoffer Ajer | STARTER | 6.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030002 | Keane Lewis-Potter | STARTER | 6.2 | 77 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030005 | Mamadou Sangare | STARTER | 7.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030008 | Aaron Hickey | STARTER | 6.5 | 61 | 0 | 0 | 1 | 0 |
| 1000560619 | Brentford FC | 2000030012 | Dango Ouattara | STARTER | 7.5 | 77 | 1 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030013 | Mathias Jensen | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030094 | Kevin Schade | STARTER | 6.0 | 61 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030006 | Igor Thiago | STARTER | 7.6 | 90 | 0 | 1 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030007 | Callum Wilson | SUB_USED | 6.8 | 13 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030009 | Mikkel Damsgaard | SUB_USED | 6.9 | 29 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030010 | Jaidon Anthony | SUB_USED | 7.1 | 29 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030014 | Nathan Collins | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030096 | Hákon Rafn Valdimarsson | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030099 | Jannik Schuster | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030163 | El Hadji Malick Diouf | SUB_USED | 6.8 | 13 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030164 | Gustavo Nunes Fernandes Gomes | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560619 | Brentford FC | 2000030165 | Benjamin Fredrick | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000020044 | Christian Walton | STARTER | 6.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000020020 | Leif Davis | STARTER | 8.1 | 90 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000020023 | Dara O'Shea | STARTER | 6.4 | 90 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000020022 | Issa Diop | STARTER | 6.4 | 90 | 0 | 0 | 1 | 0 |
| 1000560620 | Ipswich Town FC | 2000020028 | Abdoul Ouattara | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000030217 | Florentino Ibrain Morris Luís | STARTER | 6.7 | 84 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000030218 | Exequiel Palacios | STARTER | 7.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 138931 | Jaden Philogene-Bidace | STARTER | 6.5 | 60 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000011002 | Julio Enciso | STARTER | 8.3 | 85 | 1 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000020025 | Abdul Fatawu Issahaku | STARTER | 6.0 | 72 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000011001 | Emersonn | STARTER | 6.5 | 59 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000011003 | Jack Clarke | SUB_USED | 6.3 | 30 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000011005 | Marcelino Núñez | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000020019 | Kjell Scherpen | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000020021 | Jacob Greaves | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000020024 | Daizen Maeda | SUB_USED | 6.3 | 18 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000020027 | Chuba Akpom | SUB_USED | 6.6 | 5 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000020045 | Darnell Furlong | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000030215 | Azor Matusiwa | SUB_USED | 6.5 | 6 | 0 | 0 | 0 | 0 |
| 1000560620 | Ipswich Town FC | 2000030219 | Zian Flemming | SUB_USED | 6.7 | 31 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000020052 | Matz Sels | STARTER | 6.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000020059 | Jair | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000020057 | Nikola Milenković | STARTER | 6.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000013002 | Murillo | STARTER | 6.7 | 90 | 0 | 0 | 1 | 0 |
| 1000560620 | Nottingham Forest FC | 2000020095 | Daniel Muñoz | STARTER | 7.6 | 90 | 0 | 1 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000013001 | James McAtee | STARTER | 7.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000020063 | Xaver Schlager | STARTER | 7.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000020053 | Neco Williams | STARTER | 7.7 | 83 | 1 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000020055 | Morgan Gibbs-White | STARTER | 6.8 | 90 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000020058 | Dan Ndoye | STARTER | 6.9 | 68 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 161948 | Liam Delap | STARTER | 6.2 | 89 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 19305 | Ryan Yates | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000020054 | Ibrahim Sangaré | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000020060 | Ola Aina | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000020061 | Chris Wood | SUB_USED | NULL | 1 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000020062 | Luca Netz | SUB_USED | 6.5 | 7 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000020064 | Arnaud Kalimuendo | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000020073 | John Victor | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000020074 | Ousmane Diomande | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560620 | Nottingham Forest FC | 2000020077 | Callum Hudson-Odoi | SUB_USED | 6.5 | 22 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000020030 | Robin Roefs | STARTER | 7.4 | 90 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000020034 | Reinildo Mandava | STARTER | 6.6 | 90 | 0 | 0 | 1 | 0 |
| 1000560621 | Sunderland AFC | 2000020033 | Luke O'Nien | STARTER | 7.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000020049 | Nordi Mukiele | STARTER | 7.3 | 51 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000020035 | Trai Hume | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000020036 | Noah Sadiki | STARTER | 6.7 | 90 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000012002 | Granit Xhaka | STARTER | 7.3 | 90 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000012001 | Nilson Angulo | STARTER | 7.1 | 78 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000020037 | Enzo Le Fée | STARTER | 6.6 | 78 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000020042 | Chris Rigg | STARTER | 6.9 | 90 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000020038 | Brian Brobbey | STARTER | 6.8 | 64 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000020039 | Chemsdine Talbi | SUB_USED | 6.5 | 12 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000020040 | Omar Alderete | SUB_USED | 6.7 | 39 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000020043 | Habib Diarra | SUB_USED | 6.7 | 12 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000020048 | Melker Ellborg | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000030252 | Dayann Méthalie | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000030253 | Jules Ahoka | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000030254 | Malick Fofana | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000030255 | Juan Angulo | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560621 | Sunderland AFC | 2000030260 | Timur Tuterov | SUB_USED | 6.5 | 26 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000014002 | James Trafford | STARTER | 8.4 | 90 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000014004 | James Justin | STARTER | 7.0 | 90 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000014003 | Jaka Bijol | STARTER | 7.1 | 90 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000020070 | Tarik Muharemović | STARTER | 7.5 | 90 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000020065 | Jayden Bogle | STARTER | 7.1 | 90 | 0 | 0 | 1 | 0 |
| 1000560621 | Leeds United FC | 2000020068 | Ethan Ampadu | STARTER | 7.5 | 90 | 0 | 0 | 1 | 0 |
| 1000560621 | Leeds United FC | 2000020082 | Ao Tanaka | STARTER | 6.3 | 88 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000030220 | Gabriel Gudmundsson | STARTER | 7.2 | 90 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000020066 | Harry Wilson | STARTER | 6.5 | 65 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000020069 | Brenden Aaronson | STARTER | 6.2 | 64 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000020067 | Dominic Calvert-Lewin | STARTER | 5.9 | 84 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000014001 | Anton Stach | SUB_USED | 6.8 | 25 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000020071 | Noah Okafor | SUB_USED | 6.6 | 26 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000020072 | Sean Longstaff | SUB_USED | 6.6 | 2 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000020081 | Alfie Cresswell | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000020084 | Lukas Nmecha | SUB_USED | 6.6 | 6 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000030221 | Ilia Gruev | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000030224 | Michael Zetterer | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000030225 | Melvin Bard | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| 1000560621 | Leeds United FC | 2000030226 | Jean-Mattéo Bahoya | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |

### Team of week GW8: verified against API, coverage 10/10 fixtures

Played: 303; rated: 300; candidates: 280; excluded missing positions: 20; excluded NULL rating: 3. Total: 93.50. Independent assignment and ID tie-break verified. No invented positions or ratings.

| Slot | Player ID | Name | Fixture | Rating |
| --- | --- | --- | --- | --- |
| GK | 2000030124 | Zion Suzuki | 1000560614 | 9.00 |
| LB | 2000020020 | Leif Davis | 1000560620 | 8.10 |
| LCB | 2000001006 | Gabriel Magalhaes | 1000560616 | 9.00 |
| RCB | 2000003005 | Jeremy Jacquet | 1000560612 | 8.30 |
| RB | 2000030109 | James Hill | 1000560615 | 9.50 |
| LCM | 2000001008 | Martin Odegaard | 1000560616 | 8.00 |
| CM | 2000001023 | Declan Rice | 1000560616 | 8.20 |
| RCM | 2000030092 | Shea Charles | 1000560618 | 7.70 |
| LW | 2000030093 | Oscar Bobb | 1000560618 | 8.20 |
| ST | 283058 | Nicolas Jackson | 1000560614 | 9.30 |
| RW | 2000020009 | Mohamed Belloumi | 1000560619 | 8.20 |

# GW8 Premier League 2026/27 — chỉ lưu dữ liệu từ 20 ảnh

## Checkpoint ngày 26/10/2026

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

## Bước tiếp theo — chỉ khi người dùng yêu cầu tiếp tục

1. Đọc lại checkpoint này và dùng nguyên bản chép ảnh, không bắt người dùng cung cấp lại các rating/danh sách đã lưu.
2. Xác định cấu hình Aiven của Render và khóa football-data.org qua file/env được ignore; không dùng endpoint Railway cũ hoặc yêu cầu gửi bí mật vào chat. Đối chiếu mapping provider, trạng thái kết thúc và dữ liệu mùa của mã sync hiện có.
3. Đối chiếu đầy đủ danh tính/membership; thu thập phút/bàn/kiến tạo/thẻ theo quy trình; tạo CSV thống kê cuối, bộ CSV đội hình và bằng chứng unrated/vắng mặt nếu có căn cứ.
4. Chỉ sau xác minh đích, dữ liệu và backup mới mới nhập theo quy trình được giao; kiểm tra lặp, tổng mùa, API/sân/Đội hình tiêu biểu và readiness. Chưa được coi GW8 đã hoàn tất ở checkpoint này.
5. GW9 chờ chỉ dẫn tiếp theo của người dùng; không tự mở hoặc công bố kết quả.

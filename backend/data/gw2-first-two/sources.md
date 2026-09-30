# GW2 2026/27: hai trận đầu — dữ liệu chờ rating

**Trạng thái: DRAFT, chưa nhập H2 hoặc MySQL production.** SofaScore không hiển thị `rating` cho 4 người `PLAYED` vào sân cuối trận. Importer cho phép ô này trống, nhưng chưa nhập trước khi người dùng xác nhận cách xử lý.

## Phạm vi và nguồn

| Fixture PremierHub | Trận / ngày | Danh sách trận, phút, bàn, kiến tạo, thẻ | Rating SofaScore đã xác minh |
| --- | --- | --- | --- |
| `1000560555` | Crystal Palace 1–4 Manchester City, 2026-08-28 | [StatMuse](https://www.statmuse.com/fc/match/8-28-2026-cry-vs-mci-112781) | [Lineups SofaScore](https://widgets.sofascore.com/embed/lineups?id=16363252&widgetTheme=light), 30/32 người ra sân có rating |
| `1000560552` | Liverpool 2–2 Nottingham Forest, 2026-08-29 | [StatMuse](https://www.statmuse.com/fc/match/-112774) | [Lineups SofaScore](https://widgets.sofascore.com/embed/lineups?id=16363254&widgetTheme=light), 29/31 người ra sân có rating |

Fixture ID, kết quả và thứ tự được lấy từ [API PremierHub `season=2026&matchweek=2&status=FINISHED`](https://epl-personal-production.up.railway.app/api/matches?season=2026&matchweek=2&status=FINISHED) ngày 2026-09-30. Kết quả API là trận `1000560555` ngày 28/08 rồi `1000560552` ngày 29/08.

SofaScore còn xác nhận tỷ số, bàn và thay người ở [trận Palace–City](https://www.sofascore.com/football/match/manchester-city-crystal-palace/hr) và [trận Liverpool–Forest](https://www.sofascore.com/football/match/liverpool-fc-nottingham-forest/osU). Các rating của StatMuse **không** được dùng làm Fantasy rating.

## File dữ liệu

- `lineups.csv`: đủ 80 người (11 đá chính, người dự bị vào sân, người dự bị không vào sân cho từng đội). `source_name` và `statmuse_player_id` giúp kiểm tra ghép danh tính; `name` và `player_id` là của PremierHub.
- `sofascore-ratings.csv`: 80 danh tính SofaScore kèm ID nguồn và 59 rating đang hiển thị trong widget Lineups; ô trống được giữ nguyên. Đây là bảng đối chiếu nguồn để so với rating người dùng sẽ gửi.
- `manual-match-stats-2026-GW2-first-two-DRAFT.csv`: đúng 10 cột importer, 80 dòng; 63 `PLAYED`, 17 `DID_NOT_PLAY`. StatMuse cung cấp đủ phút, bàn, kiến tạo, thẻ cho 63 người ra sân. SofaScore có rating cho 59 người; 4 ô còn thiếu để trống. 17 người dự bị không vào sân được ghi 0 phút/sự kiện vì có tên trong danh sách dự bị và không có thống kê ra sân hoặc sự kiện thay vào; rating để trống.
- `manual-players-2026-GW2-first-two-DRAFT.csv`: 80 khoảng `[ngày trận, ngày kế tiếp)` cho đúng CLB ngày trận. Không kéo dài membership tới 30/09 khi chưa có bằng chứng liên tục. Đây là **khoảng đề xuất**, chưa nhập.
- `missing.csv`: đúng fixture, CLB, `player_id`, tên và trường `rating` cần người dùng hỗ trợ; 4 dòng.

Hai trận: Palace–City 32 người `PLAYED`, 8 `DID_NOT_PLAY`, 30 rating SofaScore xác minh; Liverpool–Forest 31 `PLAYED`, 9 `DID_NOT_PLAY`, 29 rating xác minh. Palace có 1 bàn do Gianluigi Donnarumma phản lưới; không gán bàn này cho cầu thủ Palace. Tổng bàn ghi cho cầu thủ là 0–4 và 2–2, khớp với tỷ số khi tính bàn phản lưới.

## Rating và danh tính cần chú ý

Rating trong CSV lấy từ trường `statistics.rating` của widget Lineups SofaScore đúng event, không lấy `ratingVersions.alternative`. [Bài tường thuật Liverpool–Forest](https://www.sofascore.com/news/liverpool-2-2-nottingham-forest-williams-steals-the-spotlight) ghi khác widget hiện tại ở bốn người: Víctor Muñoz 7.3 so với widget **7.4**; Dominik Szoboszlai 7.2 so với **7.1**; Ola Aina 7.2 so với **7.1**; Matz Sels 7.1 so với **7.0**. CSV dùng widget hiện tại gắn trực tiếp với trận. [Bài Palace–City](https://www.sofascore.com/news/crystal-palace-1-4-man-city-cherki-hits-8-6) và widget cùng ghi Rayan Cherki 8.6. Không suy từ điểm trung bình đội.

ID của 79 người đã có được đối chiếu theo CSV GW1 và roster đã nhập, rồi so tên với danh sách StatMuse. Các tên nguồn khác tên PremierHub được ghi cả hai ở `lineups.csv`: Alisson / Alisson Becker; Jair Cunha / Jair; John / John Victor; Yeremi Pino / Yéremy Pino; và các dấu/phiên âm tương ứng. Daniel Muñoz `2000020095` thuộc Palace ngày GW1/GW2 dù snapshot 30/09 đặt anh ở Forest; file GW2 giữ `MID` đã lưu cho Palace. Tyrick Mitchell, Yéremy Pino, Anan Khalaili và Ola Aina cũng giữ vị trí mùa 2026 đã lưu trong GW1, không xử lý danh sách lệch FPL.

Claudio Echeverri có tên trong danh sách dự bị Man City ngày 28/08 trên StatMuse, nhưng chưa có trong CSV roster đã nhập; [FPL snapshot 30/09](../roster-2026-09-30/fpl-bootstrap-static.json) ghi `element_type=3` (MID), dù lúc đó trạng thái là unavailable. `2000030256` là **ID mới tạm chọn** nối sau dải batch roster; cần kiểm tra trực tiếp `players` production trước khi nhập, không hợp nhất với ID khác. Khoảng của anh chỉ là 28/08–29/08. Dữ liệu ngày 30/09 không được dùng làm bằng chứng membership GW2.

API `players?season=2026&asOf=2026-08-28` và `...2026-08-29` hiện trả không có ai trong bốn đội tại ngày trận. Vì vậy cần nạp các khoảng ngày trận trước CSV thống kê. Không sửa, xóa hoặc lùi ngày các khoảng đã có; GW1, GW4–GW5 và mùa 2024/25 nằm ngoài batch này.

## Bước còn chờ

Người dùng sẽ gửi rating SofaScore; dừng thu thập rating ở đây. Đối chiếu dữ liệu người dùng với 59 rating trong `sofascore-ratings.csv` và xử lý bốn người trong `missing.csv`: Kaden Braithwaite và Vitor Reis (`1000560555`), Callum Hudson-Odoi và Nicolás Domínguez (`1000560552`). Widget SofaScore liệt kê họ vào sân nhưng trường `statistics.rating` vắng mặt; **không tự điền 0**. Sau khi chốt rating hoặc xác nhận giữ ô trống có căn cứ, bỏ hậu tố `DRAFT`, kiểm tra H2 trên bản sao production, sao lưu MySQL, nhập mỗi CSV một lần, nhập lại xác nhận thêm 0 và kiểm tra API lịch sử cầu thủ bốn đội. Không commit/push tự động.

# GW2 2026/27: hai trận FINISHED đầu tiên

**Đã nhập MySQL production ngày 2026-09-30.** Phạm vi chỉ gồm hai trận dưới đây; không nhập lại roster toàn mùa, GW1, derby GW4, batch GW5 hoặc mùa 2024/25. Không tự thu thập rating SofaScore từ nay về sau.

| Fixture PremierHub | Trận / ngày | Đội hình và thống kê trận | Rating |
| --- | --- | --- | --- |
| `1000560555` | Crystal Palace 1–4 Manchester City, 2026-08-28 | [StatMuse](https://www.statmuse.com/fc/match/8-28-2026-cry-vs-mci-112781) | Ảnh đội hình và dự bị do người dùng gửi trong cuộc trò chuyện; 30 rating hiển thị |
| `1000560552` | Liverpool 2–2 Nottingham Forest, 2026-08-29 | [StatMuse](https://www.statmuse.com/fc/match/-112774) | Ảnh đội hình và dự bị do người dùng gửi trong cuộc trò chuyện; 29 rating hiển thị |

Fixture ID, kết quả và thứ tự được lấy từ [API PremierHub `season=2026&matchweek=2&status=FINISHED`](https://epl-personal-production.up.railway.app/api/matches?season=2026&matchweek=2&status=FINISHED). Ảnh người dùng xác nhận từng giá trị trong 59 rating đã có ở `sofascore-ratings.csv`; chỉ sau khi đối chiếu khớp tên, trận và giá trị, chúng mới được đưa vào CSV cuối. Rating của StatMuse không được dùng làm Fantasy rating. Chỉ số phút, bàn, kiến tạo và thẻ trong CSV cuối là dữ liệu StatMuse đã thu thập trước khi có yêu cầu dừng thu thập rating SofaScore.

## File dữ liệu

- `lineups.csv`: 80 cầu thủ của bốn đội, gồm đá chính, dự bị vào sân và dự bị không vào sân; ghi cả tên nguồn, tên PremierHub và ID đối chiếu.
- `sofascore-ratings.csv`: bảng đối chiếu 80 danh tính nguồn, với 59 rating cũ nay khớp ảnh người dùng. Không dùng file này để tự thu thập rating cho trận khác.
- `manual-match-stats-2026-GW2-first-two.csv`: **file đã nhập**, đúng 10 cột, 80 dòng; 63 `PLAYED`, 17 `DID_NOT_PLAY`. Có 59 rating do ảnh người dùng xác nhận, 4 rating `PLAYED` trống vì ảnh không hiện điểm, 17 rating `DID_NOT_PLAY` trống. Không điền 0 cho rating thiếu.
- `manual-players-2026-GW2-first-two.csv`: **file đã nhập**, 80 khoảng `[ngày trận, ngày kế tiếp)` chứng minh CLB tại ngày trận. Không kéo dài đến 30/09 khi chưa có bằng chứng liên tục.
- Hai file `*-DRAFT.csv` giữ lại bản làm việc trước khi ảnh được xác nhận. Các ô rating trong DRAFT vẫn trống; không dùng DRAFT để nhập lại.
- `missing.csv`: bốn cầu thủ đã vào sân nhưng ảnh không hiển thị `rating`; ghi rõ fixture, CLB, ID, tên và trường còn thiếu.

Palace–City có 32 người `PLAYED` và 8 `DID_NOT_PLAY`; Liverpool–Forest có 31 và 9. 17 người dự bị không vào sân được ghi 0 phút/sự kiện vì danh sách dự bị và sự kiện thay người cho thấy họ không vào sân. Palace có một bàn do Gianluigi Donnarumma phản lưới; không gán bàn đó cho cầu thủ Palace. Bàn của cầu thủ trong CSV là 0–4 và 2–2.

## Bốn rating không hiển thị

| Fixture | Cầu thủ | `player_id` | Trường |
| --- | --- | --- | --- |
| `1000560555` | Kaden Braithwaite | `2000004024` | `rating` |
| `1000560555` | Vitor Reis | `2000004013` | `rating` |
| `1000560552` | Callum Hudson-Odoi | `2000020077` | `rating` |
| `1000560552` | Nicolás Domínguez | `2000020076` | `rating` |

Ảnh do người dùng cung cấp cho thấy cả bốn vào sân rất muộn và không có số rating cạnh tên; các ô này và `fantasy_points` tương ứng được lưu `NULL`. Nếu có rating bổ sung về sau, cần người dùng cung cấp; không tự tìm SofaScore hoặc đoán giá trị. Với bốn người không có rating, các trường còn lại vẫn lấy từ StatMuse và đã được nhập.

## Danh tính và khoảng CLB

79 ID đã có được đối chiếu với CSV GW1 và roster hiện có; `lineups.csv` giữ tên nguồn khác tên PremierHub như Alisson/Alisson Becker, Jair Cunha/Jair, John/John Victor và Yeremi Pino/Yéremy Pino. Daniel Muñoz `2000020095` thuộc Palace tại ngày trận dù snapshot 30/09 đặt anh ở Forest; vị trí `MID` lịch sử được giữ. Tyrick Mitchell, Yéremy Pino, Anan Khalaili và Ola Aina cũng giữ nguyên vị trí mùa 2026 đã lưu. Không hợp nhất ID hay sửa vị trí lịch sử.

Claudio Echeverri nằm trong danh sách dự bị Man City ngày 28/08 của StatMuse. Anh chưa có trong production trước batch; ID mới `2000030256` được kiểm tra là còn trống trước khi nhập. [FPL snapshot 30/09](../roster-2026-09-30/fpl-bootstrap-static.json) cho vị trí MID, còn trận ngày 28/08 chứng minh membership Man City chỉ cho khoảng 28/08–29/08. File không suy ra membership liên tục từ snapshot 30/09.

## Kiểm tra và sao lưu

- Tạo bản H2 từ đủ 14 bảng MySQL production trước khi nhập; importer H2 thêm 1 player, 1 player–club, 80 khoảng ngày và 80 dòng trận. H2 cho từng trận 40 dòng: fixture `1000560555` có 32 `PLAYED`, 30 rating; fixture `1000560552` có 31 `PLAYED`, 29 rating.
- Sao lưu MySQL bằng `mysqldump --single-transaction` trước khi nhập: `backend/local-backups/gw2-first-two/premierhub-before-gw2-first-two-20260930-223112.sql` (bị Git ignore, 567674 byte, SHA-256 `CA99DE9FCB082ECA6DAD69E468B33C9F8BEB20357EA61EE667C6044C079F97A0`, 14 bảng).
- Production: roster nhập 1 player, 1 player–club, 80 khoảng; stats nhập 80 dòng. Chạy lại cả hai file: mọi bộ đếm thêm/cập nhật đều 0.
- Chạy API backend hiện tại trên `127.0.0.1:18080` với MySQL production rồi tắt sau kiểm tra. Cả 80 endpoint `/api/players/{id}/matches?season=2026` trả đúng fixture, CLB, phút, bàn, kiến tạo, thẻ và rating như CSV. URL triển khai công khai không trả phản hồi trong môi trường kiểm tra này, nên chưa xác nhận được kết quả qua bản đang triển khai. Endpoint `/api/matches/{id}/details` của code hiện tại vẫn trả mảng `homePlayers` và `awayPlayers` trống cho dữ liệu manual; yêu cầu lần này chỉ kiểm tra API lịch sử cầu thủ.

Không commit hoặc push tự động.

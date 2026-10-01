# GW2 2026/27 — năm trận còn lại

**Chưa nhập MySQL production.** Chỉ xử lý mùa 2026/27. Người dùng đã gửi năm cặp ảnh đội hình và dự bị, xác nhận 151 rating. Ảnh không hiển thị rating của Romaine Mundle, Aaron Hickey, Rico Henry và Callum Wilson; bốn ô đó vẫn trống. Không lấy rating từ StatMuse hoặc tự thu thập SofaScore.

| Fixture PremierHub | Ngày, kết quả | Nguồn StatMuse |
| --- | --- | --- |
| `1000560561` | 2026-08-29, Coventry 0–1 Hull | [Trận](https://www.statmuse.com/fc/match/8-29-2026-cov-vs-hul-112780), [Coventry](https://www.statmuse.com/fc/ask/coventry-player-stats-vs-hull-city-on-august-29-2026), [Hull](https://www.statmuse.com/fc/ask/hull-city-player-stats-vs-coventry-on-august-29-2026) |
| `1000560554` | 2026-08-30, Sunderland 1–0 Fulham | [Trận](https://www.statmuse.com/fc/match/8-29-2026-sun-vs-ful-112775), [Sunderland](https://www.statmuse.com/fc/ask/sunderland-player-stats-vs-fulham-on-august-30-2026), [Fulham](https://www.statmuse.com/fc/ask/fulham-player-stats-vs-sunderland-on-august-30-2026) |
| `1000560556` | 2026-08-30, Chelsea 4–3 Brighton | [Trận](https://www.statmuse.com/fc/match/8-30-2026-che-vs-bha-112776), [Chelsea](https://www.statmuse.com/fc/ask/chelsea-player-stats-vs-brighton-on-august-30-2026), [Brighton](https://www.statmuse.com/fc/ask/brighton-player-stats-vs-chelsea-on-august-30-2026) |
| `1000560559` | 2026-08-30, Leeds 1–1 Brentford | [Trận](https://www.statmuse.com/fc/match/8-30-2026-lee-vs-bre-112779), [Leeds](https://www.statmuse.com/fc/ask/leeds-player-stats-vs-brentford-on-august-30-2026), [Brentford](https://www.statmuse.com/fc/ask/brentford-player-stats-vs-leeds-on-august-30-2026) |
| `1000560557` | 2026-08-31, Aston Villa 0–1 Arsenal | [Trận](https://www.statmuse.com/fc/match/8-29-2026-avl-vs-ars-112773), [Villa](https://www.statmuse.com/fc/ask/aston-villa-player-stats-vs-arsenal-on-august-31-2026), [Arsenal](https://www.statmuse.com/fc/ask/arsenal-player-stats-vs-aston-villa-on-august-31-2026) |

Fixture ID, ngày, CLB, kết quả và `hasManualStats=false` lấy từ API PremierHub local nối MySQL **chỉ đọc**: `/api/matches?season=2026&matchweek=2&status=FINISHED`. Ngày trong slug URL StatMuse của Sunderland và Aston Villa khác ngày StatMuse hiển thị; CSV dùng ngày trên trang và API PremierHub. StatMuse là nguồn thống nhất cho đội hình, dự bị, người vào sân và năm cột `minutes`, `goals`, `assists`, `yellow_cards`, `red_cards`. Mỗi bảng cầu thủ có đủ năm số cho người vào sân; không còn ô chỉ số nào chưa xác nhận. Người không vào sân nằm trong danh sách dự bị trên trang trận nhưng vắng khỏi bảng thống kê và không có sự kiện thay vào: `DID_NOT_PLAY`, 0 phút và 0 sự kiện.

## File và đối chiếu

- `manual-match-stats-2026-GW2-last-five.csv`: 200 dòng, đúng 10 cột; 40 người/trận, 20 người/đội; 155 `PLAYED`, 45 `DID_NOT_PLAY`. Có 151 rating từ ảnh người dùng và bốn rating `NULL`; Fantasy points của bốn người này cũng `NULL`.
- `manual-players-2026-GW2-last-five.csv`: 200 khoảng membership một ngày `[ngày trận, ngày kế tiếp)` để xác nhận CLB tại ngày trận; 2 ID mới. Không kéo dài khoảng đến snapshot 30/09, không sửa vị trí hoặc membership cũ.
- `user-provided-ratings.csv`: đối chiếu từng cầu thủ trong ảnh với fixture và PremierHub ID, gồm 151 rating có số, bốn cầu thủ đã ra sân nhưng không có rating trên ảnh và 45 người không ra sân. Hai file `*-DRAFT.csv` giữ bản trước khi nhận ảnh, không dùng làm file nhập.
- `lineups.csv`: vai trò `STARTER`, `SUB_USED`, `SUB_UNUSED`, ID nguồn và ID PremierHub cho cả 200 cầu thủ. `missing.csv` và `ratings-needed.md`: chỉ còn bốn rating nêu trên. Không thiếu chỉ số trận khác.

Các ID đã có được đối chiếu bằng roster tại ngày GW1 và snapshot 30/09, tên/CLB trên StatMuse và ID cầu thủ nguồn. Liam Kitching giữ ID `2000020002` có từ GW1. StatMuse có Villa #74 (`source_id=64850`) trong dữ liệu dự bị nhưng không hiện tên ở danh sách; [ghi chú GW1 đã có](../manual-match-stats-2026-GW1-patch-progress.md) và ảnh người dùng vừa gửi xác nhận #74 là Luka Lynch, ID `2000030127`, không ra sân. Hai cầu thủ dự bị chưa có trong roster PremierHub là Amario Cozier-Duberry (`2000030257`, Brighton) và Wilfried Gnonto (`2000030258`, Leeds). [FPL snapshot 30/09](../roster-2026-09-30/fpl-bootstrap-static.json) ghi cả hai là tiền vệ (`element_type=3`) và không khả dụng do cho mượn về sau; chỉ dùng snapshot để xác nhận danh tính/vị trí, còn membership ngày trận lấy từ StatMuse. Không hợp nhất ID lịch sử.

Tổng bàn của bốn trận khớp tỷ số; riêng Chelsea–Brighton có sáu bàn được ghi cho cầu thủ và [một bàn phản lưới của João Pedro](https://www.statmuse.com/fc/match/8-30-2026-che-vs-bha-112776), nên tổng tỷ số là 4–3. CSV không gán bàn phản lưới cho người ghi bàn thắng.

## Kiểm tra

- Bộ đọc `ManualRosterCsvReader` và `ManualMatchStatsCsvReader` chấp nhận cả hai CSV. Khóa `(fixture_id, player_id)` của stats và `(player_id, club_id, start_date)` của membership đều không trùng.
- Trên **bản H2 cô lập** sao chép từ lần kiểm tra trước: nhập roster thêm 2 cầu thủ, 2 liên kết cầu thủ–CLB và 200 khoảng ngày; nhập stats thêm 200 dòng. Nhập lại stats thêm 0. API `/api/matches/{id}/details?season=2026` trên H2 trả 20 người mỗi đội ở cả năm trận, đúng 155 `PLAYED` và 45 `DID_NOT_PLAY`; mọi rating, điểm Fantasy, phút/bàn/kiến tạo/thẻ khớp CSV. Có 151 rating/điểm từ ảnh người dùng; bốn người chưa được chấm vẫn `rating=NULL`, `fantasy_points=NULL`; người `DID_NOT_PLAY` có `fantasy_points=0` theo importer.
- Chưa nhập MySQL production, chưa commit/push/deploy. Chỉ điền bốn ô còn trống nếu người dùng cung cấp bằng chứng mới.

Commit message đề xuất sau khi chốt dữ liệu: `data: complete remaining five GW2 2026 manual match fixtures`.

# GW3 2026/27 — năm fixture FINISHED còn lại (DRAFT)

Chưa ghi MySQL production. Fixture ID, ngày, CLB, tỷ số và trạng thái FINISHED lấy từ PremierHub `/api/matches?season=2026&matchweek=3&status=FINISHED`, theo năm mục cuối của API. StatMuse là nguồn thống nhất cho đội hình, người vào sân, phút, bàn, kiến tạo và thẻ. Không lấy rating từ StatMuse hoặc SofaScore; `rating` của cả 200 dòng CSV đều trống để chờ ảnh người dùng.

| Fixture | Ngày, tỷ số | Đội hình, thay người, bàn thắng | Chỉ số hai đội |
| --- | --- | --- | --- |
| `1000560568` | 05/09, Fulham 2–3 Palace | [StatMuse](https://www.statmuse.com/fc/match/-112783) | [Fulham](https://www.statmuse.com/fc/ask/fulham-player-stats-vs-crystal-palace-on-september-5-2026), [Palace](https://www.statmuse.com/fc/ask/crystal-palace-player-stats-vs-fulham-on-september-5-2026) |
| `1000560569` | 05/09, Hull 0–0 Villa | [StatMuse](https://www.statmuse.com/fc/match/-112792) | [Hull](https://www.statmuse.com/fc/ask/hull-city-player-stats-vs-aston-villa-on-september-5-2026), [Villa](https://www.statmuse.com/fc/ask/aston-villa-player-stats-vs-hull-city-on-september-5-2026) |
| `1000560571` | 05/09, Newcastle 2–2 Bournemouth | [StatMuse](https://www.statmuse.com/fc/match/9-5-2026-new-vs-bou-112785) | [Newcastle](https://www.statmuse.com/fc/ask/newcastle-player-stats-vs-bournemouth-on-september-5-2026), [Bournemouth](https://www.statmuse.com/fc/ask/bournemouth-player-stats-vs-newcastle-on-september-5-2026) |
| `1000560567` | 06/09, Everton 2–2 Man Utd | [StatMuse](https://www.statmuse.com/fc/match/-112787) | [Everton](https://www.statmuse.com/fc/ask/everton-player-stats-vs-manchester-united-on-september-6-2026), [Man Utd](https://www.statmuse.com/fc/ask/manchester-united-player-stats-vs-everton-on-september-6-2026) |
| `1000560570` | 06/09, Arsenal 2–1 Chelsea | [StatMuse](https://www.statmuse.com/fc/match/9-6-2026-ars-vs-che-112786) | [Arsenal](https://www.statmuse.com/fc/ask/arsenal-player-stats-vs-chelsea-on-september-6-2026), [Chelsea](https://www.statmuse.com/fc/ask/chelsea-player-stats-vs-arsenal-on-september-6-2026) |

## Đối chiếu

- StatMuse liệt kê 11 đá chính và 9 dự bị mỗi đội ở cả năm trận. Danh sách `PLAYED` từ bảng chỉ số khớp chính xác dấu thay vào ở danh sách dự bị; 43 người `SUB_UNUSED` không có dấu thay vào. Vì vậy `DID_NOT_PLAY`, 0 phút và 0 sự kiện của họ có căn cứ. `lineups.csv` lưu vai trò, ID nguồn, ID PremierHub và CLB cho cả 200 người.
- `manual-players-2026-GW3-last-five-DRAFT.csv` chỉ gồm 160 membership còn thiếu tại ngày trận, dạng `[ngày trận, ngày kế tiếp)`. Toàn bộ ID cầu thủ đã tồn tại; không thêm hoặc hợp nhất ID.
- Tổng bàn cầu thủ trùng tỷ số ở bốn trận. Tại Newcastle–Bournemouth, Newcastle có hai bàn cầu thủ; Bournemouth có một bàn của Marcus Tavernier và [một bàn phản lưới của Malick Thiaw](https://www.statmuse.com/fc/match/9-5-2026-new-vs-bou-112785). CSV giữ `goals=0` của Thiaw, không gán bàn phản lưới như bàn thắng cầu thủ.
- Không có ô `minutes`, `goals`, `assists`, `yellow_cards` hoặc `red_cards` nào chưa xác minh cho người ra sân. `ratings-needed.csv` liệt kê 157 người `PLAYED` theo fixture, tên, ID, CLB và trạng thái. `missing-fields.csv` chỉ có 157 ô `rating`.

## Kiểm tra H2 cô lập

`manual-match-stats-2026-GW3-last-five-DRAFT.csv` có đúng 10 cột, 200 dòng, 40 người/trận; 157 `PLAYED` và 43 `DID_NOT_PLAY`. Khóa `(fixture_id, player_id)` và khóa membership không trùng. Importer chấp nhận đủ membership tại ngày trận: roster thêm 160 khoảng, stats thêm 200 dòng. Nhập lại roster thêm 0 khoảng và stats thêm 0 dòng. API H2 `/api/matches/{id}/details?season=2026` trả 20 người/đội cho cả năm trận, đúng trạng thái và chỉ số; rating/điểm Fantasy của 157 người ra sân là `NULL`.

Các file DRAFT của hai batch được giữ nguyên. Ảnh người dùng đã cung cấp 153 rating; Valentino Livramento (`158694`), Ryan Christie (`1125`), Noussair Mazraoui (`2000006003`) và Noni Madueke (`2000001015`) không hiện rating. Người dùng xác nhận họ vào sân quá ít phút nên không được chấm. File `manual-match-stats-2026-GW3-last-five.csv` là bản đã chốt, giữ `rating=NULL` và `fantasy_points=NULL` cho bốn người này. Chi tiết kiểm tra chung ở `../gw3-ratings/summary.md`. Chưa nhập MySQL production, commit, push hoặc deploy.

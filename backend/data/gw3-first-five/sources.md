# GW3 2026/27 — năm fixture FINISHED đầu tiên (DRAFT)

Chưa ghi MySQL production. Fixture ID, ngày, CLB, tỷ số và trạng thái FINISHED lấy từ PremierHub `/api/matches?season=2026&matchweek=3&status=FINISHED`, theo đúng thứ tự API. StatMuse là **nguồn thống nhất cho dữ liệu trận**: đội hình 11 đá chính + 9 dự bị mỗi đội, người vào sân và các cột phút, bàn, kiến tạo, thẻ vàng, thẻ đỏ. Không lấy rating từ StatMuse hoặc SofaScore; toàn bộ ô `rating` đang trống để người dùng cung cấp.

| Fixture | Ngày, tỷ số | Đội hình và diễn biến | Bảng chỉ số hai đội |
| --- | --- | --- | --- |
| `1000560566` | 04/09, Ipswich 0–2 Liverpool | [StatMuse](https://www.statmuse.com/fc/match/9-4-2026-ips-vs-liv-112790) | [Ipswich](https://www.statmuse.com/fc/ask/ipswich-player-stats-vs-liverpool-on-september-4-2026), [Liverpool](https://www.statmuse.com/fc/ask/liverpool-player-stats-vs-ipswich-on-september-4-2026) |
| `1000560562` | 05/09, Forest 0–0 Tottenham | [StatMuse](https://www.statmuse.com/fc/match/-112788) | [Forest](https://www.statmuse.com/fc/ask/nottingham-forest-player-stats-vs-tottenham-on-september-5-2026), [Tottenham](https://www.statmuse.com/fc/ask/tottenham-player-stats-vs-nottingham-forest-on-september-5-2026) |
| `1000560563` | 05/09, Man City 1–0 Coventry | [StatMuse](https://www.statmuse.com/fc/match/9-5-2026-mci-vs-cov-112784) | [Man City](https://www.statmuse.com/fc/ask/manchester-city-player-stats-vs-coventry-on-september-5-2026), [Coventry](https://www.statmuse.com/fc/ask/coventry-player-stats-vs-manchester-city-on-september-5-2026) |
| `1000560564` | 05/09, Brighton 1–1 Leeds | [StatMuse](https://www.statmuse.com/fc/match/-112791) | [Brighton](https://www.statmuse.com/fc/ask/brighton-player-stats-vs-leeds-on-september-5-2026), [Leeds](https://www.statmuse.com/fc/ask/leeds-player-stats-vs-brighton-on-september-5-2026) |
| `1000560565` | 05/09, Brentford 1–1 Sunderland | [StatMuse](https://www.statmuse.com/fc/match/9-5-2026-bre-vs-sun-112789) | [Brentford](https://www.statmuse.com/fc/ask/brentford-player-stats-vs-sunderland-on-september-5-2026), [Sunderland](https://www.statmuse.com/fc/ask/sunderland-player-stats-vs-brentford-on-september-5-2026) |

## Quy tắc đối chiếu

- `lineups.csv` giữ vai trò `STARTER`, `SUB_USED`, `SUB_UNUSED`, tên và ID StatMuse bên cạnh ID PremierHub. ID cũ đối chiếu với ba batch GW2 đã được kiểm tra; ID mới của riêng StatMuse được đối chiếu tên và CLB trong roster PremierHub. Iliman Ndiaye dùng ID PremierHub của Man City `2000004005` ở trận này; ID Everton của trận GW2 vẫn giữ nguyên, không hợp nhất lịch sử.
- `DID_NOT_PLAY` chỉ áp dụng cho người ở ghế dự bị của trang trận nhưng không nằm trong bảng chỉ số người ra sân hoặc sự kiện thay người. Khi đó 0 phút và 0 sự kiện có căn cứ; importer lưu `fantasy_points=0`. Người `PLAYED` có rating/điểm `NULL` cho đến khi nhận ảnh rating.
- `manual-players-2026-GW3-first-five-DRAFT.csv` chỉ gồm 161 membership **thiếu tại ngày trận**, mỗi khoảng `[ngày trận, ngày kế tiếp)`. Có một cầu thủ mới: Chema Andrés, Brighton, ID dự kiến `2000030259`, MID. [Brighton xác nhận chuyển nhượng 01/09](https://www.brightonandhovealbion.com/media-article/mft-lowdown-chema-andres-interview-september-2026); snapshot FPL 30/09 trong repo ghi `element_type=3` (MID). StatMuse ghi anh ở ghế dự bị Brighton–Leeds và không vào sân.
- Năm cặp tổng bàn thắng cầu thủ là `0–2`, `0–0`, `1–0`, `1–1`, `1–1`, đúng tỷ số; không có bàn phản lưới cần điều chỉnh trong năm trận này. Không còn chỉ số trận nào ngoài rating chưa được nguồn xác nhận.

## File và kiểm tra

- `manual-match-stats-2026-GW3-first-five-DRAFT.csv`: 200 dòng, 10 cột, 40 người/trận; 150 `PLAYED`, 50 `DID_NOT_PLAY`.
- `ratings-needed.csv`: đủ 150 tên, ID, CLB, fixture và trạng thái `PLAYED` để người dùng gửi rating theo từng trận. `missing-fields.csv` ghi đúng 150 ô rating đang trống.
- Khóa `(fixture_id, player_id)` và khóa membership không trùng; mỗi đội 20 người; membership tại ngày trận được importer xác nhận duy nhất.
- Trên H2 cô lập: roster thêm 1 cầu thủ, 1 quan hệ cầu thủ–CLB, 161 khoảng; stats thêm 200 dòng. Nhập lại roster thêm `0/0/0`, stats thêm `0`. API H2 `/api/matches/{id}/details?season=2026` trả 20 người mỗi đội cho cả năm trận, đúng trạng thái và từng chỉ số CSV; rating/điểm của cả 150 người ra sân vẫn `NULL`.

Ảnh người dùng đã cung cấp 148 rating. Ousmane Diomande (`2000020074`, Forest) và Pascal Struijk (`2000030119`, Brighton) không hiện rating; người dùng xác nhận họ vào sân quá ít phút nên không được chấm. File `manual-match-stats-2026-GW3-first-five.csv` là bản đã chốt, giữ `rating=NULL` và `fantasy_points=NULL` cho hai người này. Chi tiết kiểm tra chung ở `../gw3-ratings/summary.md`. Không nhập MySQL production, commit, push hoặc deploy.

# GW2 2026/27 — batch ba trận tiếp theo

**Chưa nhập MySQL.** Người dùng đã gửi ba cặp ảnh đội hình và dự bị ngày 2026-10-01, xác nhận 93 rating cho cầu thủ đã vào sân. Ảnh không hiển thị rating của Bazoumana Touré trong fixture `1000560558`, nên ô đó vẫn trống. Không dùng rating từ StatMuse và không tự thu thập SofaScore.

| Fixture PremierHub | Ngày, kết quả | Nguồn đội hình, phút, bàn, kiến tạo, thẻ |
| --- | --- | --- |
| `1000560558` | 2026-08-29, Tottenham 0–2 Newcastle | [StatMuse match](https://www.statmuse.com/fc/match/8-29-2026-tot-vs-new-112777), [Tottenham player stats](https://www.statmuse.com/fc/ask/tottenham-player-stats-vs-newcastle-on-august-29-2026), [Newcastle player stats](https://www.statmuse.com/fc/ask/newcastle-player-stats-vs-tottenham-on-august-29-2026) |
| `1000560560` | 2026-08-29, Bournemouth 1–1 Everton | [StatMuse match](https://www.statmuse.com/fc/match/8-29-2026-bou-vs-eve-112782), [Bournemouth player stats](https://www.statmuse.com/fc/ask/bournemouth-player-stats-vs-everton-on-august-29-2026), [Everton player stats](https://www.statmuse.com/fc/ask/everton-player-stats-vs-bournemouth-on-august-29-2026) |
| `1000560553` | 2026-08-30, Manchester United 5–2 Ipswich | [StatMuse match](https://www.statmuse.com/fc/match/8-30-2026-mun-vs-ips-112778), [United player stats](https://www.statmuse.com/fc/ask/man-united-player-stats-vs-ipswich-on-august-30-2026), [Ipswich player stats](https://www.statmuse.com/fc/ask/ipswich-player-stats-vs-man-united-on-august-30-2026) |

Fixture ID, ngày, CLB, kết quả và trạng thái `hasManualStats=false` lấy từ API PremierHub local nối MySQL ở chế độ chỉ đọc: `/api/matches?season=2026&matchweek=2&status=FINISHED`. StatMuse là **nguồn duy nhất cho thống kê trận** trong batch này. Các bảng `player stats` ghi rõ `MIN`, `G`, `A`, `YC`, `RC` cho 94 người đã vào sân. Trang match cho biết 11 đá chính và chín người dự bị mỗi đội; 26 người không có trong bảng `player stats` và không được thay vào được ghi `DID_NOT_PLAY`, 0 phút và 0 sự kiện. Số 0 của người đã vào sân được lấy trực tiếp từ bảng `player stats`, không suy từ ô trống.

## File và kiểm tra

- `lineups.csv`: 120 danh tính, vai trò, ID nguồn và ID PremierHub, 20 người mỗi đội.
- `manual-match-stats-2026-GW2-next-three.csv`: file 10 cột để kiểm tra trước khi nhập, 120 khóa `(fixture_id, player_id)` không trùng; 94 `PLAYED`, 26 `DID_NOT_PLAY`; 93 rating từ ảnh người dùng và một rating `NULL` của Bazoumana Touré.
- `user-provided-ratings.csv`: bảng đối chiếu 120 cầu thủ trong ảnh theo fixture và ID PremierHub, gồm 93 rating có số, một cầu thủ đã vào sân nhưng không có rating trên ảnh và 26 người không vào sân.
- `manual-players-2026-GW2-next-three.csv`: 120 khoảng membership một ngày `[ngày trận, ngày kế tiếp)`, đúng CLB tại ngày trận; chưa có khoảng trùng trong dữ liệu hiện có khi đọc API `asOf` cho 29/08 và 30/08.
- Hai file `*-DRAFT.csv` giữ bản trước khi nhận ảnh; không dùng làm file nhập.
- `missing.csv` và `ratings-needed.md`: chỉ còn Bazoumana Touré (`rating`). Không có trường phút, bàn, kiến tạo hoặc thẻ còn thiếu từ StatMuse cho 94 người đã vào sân.

CSV qua cả `ManualMatchStatsCsvReader` và `ManualRosterCsvReader` của backend; mỗi fixture có 40 người, mỗi đội 20. Tổng bàn cầu thủ khớp Tottenham 0–Newcastle 2 và Bournemouth 1–Everton 1. United có bốn bàn cầu thủ và một bàn phản lưới của Jacob Greaves, đúng tỷ số 5–2; Ipswich có hai bàn cầu thủ. Thẻ vàng theo đội lần lượt 1–2, 3–2, 1–3; không có thẻ đỏ. Các tổng này khớp trang match StatMuse.

Nhập thử trên bản H2 sao chép từ MySQL ở chế độ chỉ đọc: roster thêm 0 player, 1 liên kết player–club (Tim Iroegbunam–Everton), 120 khoảng ngày; stats thêm 120 dòng. Backend local nối H2 trả 20 cầu thủ mỗi đội cho cả ba fixture, số rating lần lượt 30, 31, 32. Bazoumana Touré vẫn `PLAYED`, 1 phút, `rating=NULL`, `fantasy_points=NULL`. Đây chỉ là kiểm tra H2; MySQL production chưa được ghi.

Ảnh người dùng xác nhận 30 rating cho Tottenham–Newcastle, 31 cho Bournemouth–Everton và 32 cho United–Ipswich. Bruno Fernandes là `10.0` trong ảnh United–Ipswich. Bazoumana Touré vào sân muộn nhưng ảnh không có số rating bên cạnh tên; không suy ra `0`. Với importer hiện tại, `PLAYED` không có rating cho `fantasy_points=NULL`.

## Danh tính cần lưu ý

- Iliman Ndiaye dùng ID Everton lịch sử `2000015003` tại 29/08, dù snapshot 30/09 có ID khác ở Manchester City. Không hợp nhất ID.
- Tim Iroegbunam dùng ID hiện có `284500` và membership Everton một ngày 29/08, dù snapshot 30/09 ghi Hull City. Không đổi membership Hull.
- Guimissongui Ouattara của StatMuse tương ứng Abdoul Ouattara `2000020028` đã có ở Ipswich GW1. [Hồ sơ tên đầy đủ Abdoul Guemissongui Ouattara](https://www.prideofanglia.com/page.php?fullname=Abdoul+Ouattara&page=playerProfile) chỉ dùng để đối chiếu danh tính, không dùng làm nguồn thống kê trận.

Chỉ bổ sung các khoảng ngày được trận đấu chứng minh; không kéo dài tới snapshot 30/09 và không sửa vị trí đã lưu.

## Batch kế tiếp

Sau khi chốt ba trận này, xử lý **cùng một batch cả năm trận GW2 còn lại**: `1000560561` Coventry–Hull (29/08), `1000560554` Sunderland–Fulham (30/08), `1000560556` Chelsea–Brighton (30/08), `1000560559` Leeds–Brentford (30/08), `1000560557` Aston Villa–Arsenal (31/08).

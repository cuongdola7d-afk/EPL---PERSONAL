# GW2 2026/27 — batch ba trận tiếp theo (DRAFT)

**Chưa nhập MySQL.** `rating` để trống cho toàn bộ 94 cầu thủ đã vào sân; người dùng sẽ cung cấp. Không dùng rating từ StatMuse và không tự thu thập SofaScore.

| Fixture PremierHub | Ngày, kết quả | Nguồn đội hình, phút, bàn, kiến tạo, thẻ |
| --- | --- | --- |
| `1000560558` | 2026-08-29, Tottenham 0–2 Newcastle | [StatMuse match](https://www.statmuse.com/fc/match/8-29-2026-tot-vs-new-112777), [Tottenham player stats](https://www.statmuse.com/fc/ask/tottenham-player-stats-vs-newcastle-on-august-29-2026), [Newcastle player stats](https://www.statmuse.com/fc/ask/newcastle-player-stats-vs-tottenham-on-august-29-2026) |
| `1000560560` | 2026-08-29, Bournemouth 1–1 Everton | [StatMuse match](https://www.statmuse.com/fc/match/8-29-2026-bou-vs-eve-112782), [Bournemouth player stats](https://www.statmuse.com/fc/ask/bournemouth-player-stats-vs-everton-on-august-29-2026), [Everton player stats](https://www.statmuse.com/fc/ask/everton-player-stats-vs-bournemouth-on-august-29-2026) |
| `1000560553` | 2026-08-30, Manchester United 5–2 Ipswich | [StatMuse match](https://www.statmuse.com/fc/match/8-30-2026-mun-vs-ips-112778), [United player stats](https://www.statmuse.com/fc/ask/man-united-player-stats-vs-ipswich-on-august-30-2026), [Ipswich player stats](https://www.statmuse.com/fc/ask/ipswich-player-stats-vs-man-united-on-august-30-2026) |

Fixture ID, ngày, CLB, kết quả và trạng thái `hasManualStats=false` lấy từ API PremierHub local nối MySQL ở chế độ chỉ đọc: `/api/matches?season=2026&matchweek=2&status=FINISHED`. StatMuse là **nguồn duy nhất cho thống kê trận** trong batch này. Các bảng `player stats` ghi rõ `MIN`, `G`, `A`, `YC`, `RC` cho 94 người đã vào sân. Trang match cho biết 11 đá chính và chín người dự bị mỗi đội; 26 người không có trong bảng `player stats` và không được thay vào được ghi `DID_NOT_PLAY`, 0 phút và 0 sự kiện. Số 0 của người đã vào sân được lấy trực tiếp từ bảng `player stats`, không suy từ ô trống.

## File và kiểm tra

- `lineups.csv`: 120 danh tính, vai trò, ID nguồn và ID PremierHub, 20 người mỗi đội.
- `manual-match-stats-2026-GW2-next-three-DRAFT.csv`: đúng 10 cột, 120 khóa `(fixture_id, player_id)` không trùng; 94 `PLAYED`, 26 `DID_NOT_PLAY`; 94 ô rating của `PLAYED` trống để người dùng cung cấp.
- `manual-players-2026-GW2-next-three-DRAFT.csv`: 120 khoảng membership một ngày `[ngày trận, ngày kế tiếp)`, đúng CLB tại ngày trận; chưa có khoảng trùng trong dữ liệu hiện có khi đọc API `asOf` cho 29/08 và 30/08.
- `missing.csv` và `ratings-needed.md`: danh sách 94 rating cần người dùng gửi. Không có trường phút, bàn, kiến tạo hoặc thẻ còn thiếu từ StatMuse cho 94 người đã vào sân.

CSV qua cả `ManualMatchStatsCsvReader` và `ManualRosterCsvReader` của backend; mỗi fixture có 40 người, mỗi đội 20. Tổng bàn cầu thủ khớp Tottenham 0–Newcastle 2 và Bournemouth 1–Everton 1. United có bốn bàn cầu thủ và một bàn phản lưới của Jacob Greaves, đúng tỷ số 5–2; Ipswich có hai bàn cầu thủ. Thẻ vàng theo đội lần lượt 1–2, 3–2, 1–3; không có thẻ đỏ. Các tổng này khớp trang match StatMuse.

## Danh tính cần lưu ý

- Iliman Ndiaye dùng ID Everton lịch sử `2000015003` tại 29/08, dù snapshot 30/09 có ID khác ở Manchester City. Không hợp nhất ID.
- Tim Iroegbunam dùng ID hiện có `284500` và membership Everton một ngày 29/08, dù snapshot 30/09 ghi Hull City. Không đổi membership Hull.
- Guimissongui Ouattara của StatMuse tương ứng Abdoul Ouattara `2000020028` đã có ở Ipswich GW1. [Hồ sơ tên đầy đủ Abdoul Guemissongui Ouattara](https://www.prideofanglia.com/page.php?fullname=Abdoul+Ouattara&page=playerProfile) chỉ dùng để đối chiếu danh tính, không dùng làm nguồn thống kê trận.

Chỉ bổ sung các khoảng ngày được trận đấu chứng minh; không kéo dài tới snapshot 30/09 và không sửa vị trí đã lưu.

## Batch kế tiếp

Sau khi chốt ba trận này, xử lý **cùng một batch cả năm trận GW2 còn lại**: `1000560561` Coventry–Hull (29/08), `1000560554` Sunderland–Fulham (30/08), `1000560556` Chelsea–Brighton (30/08), `1000560559` Leeds–Brentford (30/08), `1000560557` Aston Villa–Arsenal (31/08).

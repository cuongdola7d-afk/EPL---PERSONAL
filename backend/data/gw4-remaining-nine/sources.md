# GW4 2026/27 — chín trận bổ sung

Derby Manchester (`1000560580`) đã có 40 dòng trên production trước batch này và được giữ nguyên. Hai file cuối để lưu cùng mã nguồn là `manual-players-2026-GW4-remaining-nine.csv` (320 khoảng membership) và `manual-match-stats-2026-GW4-remaining-nine.csv` (360 dòng). Các file mang hậu tố `DRAFT` hoặc `RATINGS` là bản làm việc, không dùng để nhập production.

| Fixture | Trận | Nguồn thống nhất cho đội hình và chỉ số |
| --- | --- | --- |
| `1000560572` | Crystal Palace 2–3 Ipswich | [StatMuse](https://www.statmuse.com/fc/match/-112801) |
| `1000560573` | Liverpool 0–0 Fulham | [StatMuse](https://www.statmuse.com/fc/match/-112794) |
| `1000560574` | Aston Villa 1–2 Nottingham Forest | [StatMuse](https://www.statmuse.com/fc/match/-112793) |
| `1000560575` | Tottenham 0–0 Everton | [StatMuse](https://www.statmuse.com/fc/match/-112797) |
| `1000560576` | Bournemouth 2–2 Brentford | [StatMuse](https://www.statmuse.com/fc/match/9-12-2026-bou-vs-bre-112802) |
| `1000560577` | Sunderland 0–2 Arsenal | [StatMuse](https://www.statmuse.com/fc/match/9-12-2026-sun-vs-ars-112795) |
| `1000560578` | Coventry 0–5 Brighton | [StatMuse](https://www.statmuse.com/fc/match/-112800) |
| `1000560579` | Leeds 4–1 Newcastle | [StatMuse](https://www.statmuse.com/fc/match/9-12-2026-lee-vs-new-112799) |
| `1000560581` | Chelsea 2–2 Hull | [StatMuse](https://www.statmuse.com/fc/match/-112796) |

Mỗi fixture có 20 cầu thủ mỗi đội (11 đá chính, chín dự bị). Người thực sự vào sân được đối chiếu với sự kiện thay người. Phút, bàn, kiến tạo và thẻ được lấy từ cùng trang trận StatMuse. Leeds–Newcastle có một bàn phản lưới của Malick Thiaw; `goals` của anh vẫn bằng 0. Rating lấy từ ảnh SofaScore người dùng gửi, lưu thành điểm Fantasy cho mùa này. `user-provided-ratings.csv` ghi 278 rating đã đọc; `confirmed-unrated.csv` ghi ba người có ra sân nhưng không hiển thị rating: Chris Wood (1 phút), Álvaro Rodríguez (1 phút) và Sean Longstaff (3 phút). Cả ba là `PLAYED`, giữ `rating=NULL` và `fantasy_points=NULL`. Không gán 0 cho rating. 79 người `DID_NOT_PLAY` có `rating=NULL`, `fantasy_points=0`.

Ảnh Substitutions Bournemouth–Brentford cuối cùng xác nhận Ryan Christie 6.4, Lewis Cook 6.6, Ben Gannon-Doak 7.2, David Brooks 6.4, El Hadji Malick Diouf 7.0, Dango Ouattara 6.5, Mikkel Damsgaard 6.6 và Michael Kayode 6.7. Álvaro Rodríguez vào sân phút 90 nhưng không hiện rating. `ratings-still-needed.csv` hiện không còn dòng cần giải quyết.

Hai CSV cuối đã thử trên H2 cô lập sao chép từ production: thêm 320 membership và 360 dòng thống kê; chạy lại cả hai thêm 0. API H2 khớp từng trường CSV, chín trận 40 dòng mỗi trận, và derby vẫn 40 dòng. Trước ghi production, preflight xác nhận đúng host Railway/schema `railway`, chín trận chưa có khóa thống kê nào, derby có 40 dòng/26 rating. Bản sao lưu SQL mới được lưu ngoài Git tại `backend/local-backups/gw4-2026/premierhub-before-gw4-20261001-230233.sql` (668950 byte; SHA-256 `6ABB64A1952263EEAB28C0EB765E71ECF1BEFEB754B110A6A1EE60424345C01D`).

Production import ngày 01/10/2026 thêm 320 khoảng membership, 360 dòng cầu thủ–trận; nhập lại thêm 0. So sánh đọc lại MySQL khớp từng trường CSV ở chín trận. Toàn GW4 có **10 fixture, 400 dòng**: 304 `PLAYED` có rating và điểm bằng rating; ba `PLAYED` không được chấm có cả hai trường `NULL`; 93 `DID_NOT_PLAY` có rating `NULL` và điểm 0. API production báo `hasManualStats=true` cho cả 10 trận; endpoint chi tiết khớp cả chín CSV mới và derby, endpoint lịch sử cầu thủ trả đúng trường hợp có và không có rating. Không sửa mã, commit, push hoặc deploy trong lượt nhập production này.

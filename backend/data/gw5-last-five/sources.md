# GW5 2026/27 — batch sau, năm trận theo ngày thi đấu

Batch được thu thập ban đầu để chờ ảnh rating. `lineups.csv` có đủ 200 cầu thủ của năm trận. `manual-match-stats-2026-GW5-last-five-DRAFT.csv` chỉ có 140 khóa còn thiếu trên MySQL production: 104 `PLAYED` ban đầu để rating trống và 36 `DID_NOT_PLAY`. `manual-players-2026-GW5-last-five-DRAFT.csv` bổ sung 140 khoảng membership đúng ngày trận. `ratings-needed.csv` là danh sách chờ ảnh ở giai đoạn DRAFT. Không tự gán rating 0 cho người không hiển thị điểm.

| Fixture | Trận | Dòng production trước batch | CSV bổ sung | PLAYED cần đối chiếu ảnh | Nguồn thống nhất đội hình/chỉ số |
| --- | --- | ---: | ---: | ---: | --- |
| `1000560589` | Nottingham Forest 0–1 Coventry, 19/09 | 0 | 40 | 29 | [StatMuse](https://www.statmuse.com/fc/match/9-19-2026-for-vs-cov-112808) |
| `1000560582` | Bournemouth 0–1 Liverpool, 20/09 | 20 Liverpool | 20 Bournemouth | 16 | [StatMuse](https://www.statmuse.com/fc/match/9-20-2026-bou-vs-liv-112812) |
| `1000560583` | Fulham 1–1 Manchester United, 20/09 | 20 Manchester United | 20 Fulham | 16 | [StatMuse](https://www.statmuse.com/fc/match/9-20-2026-ful-vs-mun-112803) |
| `1000560585` | Leeds 0–0 Crystal Palace, 20/09 | 0 | 40 | 29 | [StatMuse](https://www.statmuse.com/fc/match/9-20-2026-lee-vs-cry-112809) |
| `1000560590` | Manchester City 5–3 Sunderland, 20/09 | 20 Manchester City | 20 Sunderland | 14 | [StatMuse](https://www.statmuse.com/fc/match/9-20-2026-mci-vs-sun-112804) |

Đã đọc trực tiếp 80 dòng GW5 có sẵn trên Railway MySQL; 60 dòng Liverpool, Manchester United và Manchester City ở batch này khớp CSV cũ và năm cột chỉ số trên nguồn. Mỗi trang StatMuse có 11 người đá chính và chín dự bị mỗi đội; cầu thủ vào sân khớp sự kiện thay người. Fulham–Manchester United có một bàn phản lưới của Lisandro Martínez, nên tổng `goals` của Fulham bằng 0 nhưng tỷ số là 1–1. `DID_NOT_PLAY` chỉ dành cho dự bị không vào sân, chỉ số 0; `PLAYED` có đủ phút, bàn, kiến tạo, thẻ vàng và thẻ đỏ nhưng rating vẫn trống.

Importer H2 cô lập sao chép production và dữ liệu GW4 đã nhận roster 140 khoảng, stats 140 dòng sau batch đầu, không có xung đột. Dữ liệu production GW5 cũ không được ghi đè.

## Ảnh rating nhận sau bản DRAFT

Ảnh người dùng đã cho rating của 100 trong 104 người `PLAYED` mới. Bốn người có vào sân nhưng ảnh không hiển thị rating là Ben Gannon-Doak (3 phút), Alex Tóth (3 phút), Jean-Mattéo Bahoya (1 phút) và Joel Latibeaudiere (1 phút); họ giữ `rating=NULL`, `fantasy_points=NULL`, không điền 0. Bản áp rating nằm ở `manual-match-stats-2026-GW5-last-five-RATINGS.csv`; CSV cuối chỉ gồm 140 khóa mới là `manual-match-stats-2026-GW5-last-five.csv`.

Đối chiếu 20 dòng Manchester City đã lưu phát hiện ba xung đột tại fixture `1000560590`:

| Cầu thủ | Player ID | Rating production | Rating trong ảnh mới |
| --- | ---: | ---: | ---: |
| Erling Haaland | `2000004007` | 6.3 | 8.3 |
| Rayan Cherki | `2000004008` | 7.9 | 8.0 |
| Antoine Semenyo | `2000004020` | 9.3 | 9.5 |

Theo quyết định của người dùng, giữ ba rating production và không nhập lại các khóa này.

`user-provided-ratings.csv` lưu 142 rating hiển thị trong ảnh (gồm 42 khóa production đã có); `confirmed-unrated.csv` ghi bốn người không được chấm; `production-rating-conflicts.csv` ghi ba chênh lệch và quyết định `KEEP_PRODUCTION`. Hai bản `RATINGS` đã nhập thử trên H2 cô lập, thêm 320 khóa mới và giữ nguyên 80 khóa cũ. H2 mô phỏng toàn GW5 có 400 dòng: 298 `PLAYED` có rating, bốn `PLAYED` không được chấm giữ cả rating và điểm `NULL`, 98 `DID_NOT_PLAY` có điểm 0.

Hai CSV cuối của batch là `manual-players-2026-GW5-last-five.csv` và `manual-match-stats-2026-GW5-last-five.csv`. Trước ghi production, preflight xác nhận Railway MySQL `railway`, 80 khóa cũ khớp CSV, 320 khóa mới chưa tồn tại và GW4 vẫn 400 dòng. Bản sao lưu SQL mới lưu ngoài Git tại `backend/local-backups/gw5-2026/premierhub-before-gw5-20261001-235815.sql` (714011 byte; SHA-256 `E9E8F9327D1C665EDA41690E5409621CB3B20C28822F0158B4FD11FFF2434D84`). Production import ngày 02/10/2026 thêm 140 khoảng membership và 140 dòng thống kê; nhập lại cả hai thêm 0. MySQL đọc lại khớp từng trường của cả 80 dòng cũ lẫn 320 dòng mới. API production trả đủ 10 trận GW5, mỗi trận 20 cầu thủ mỗi đội, và lịch sử cầu thủ cho thấy rating cũ được giữ nguyên. Toàn GW5 có **400 dòng: 298 có rating, bốn `PLAYED` không được chấm, 98 `DID_NOT_PLAY` có điểm 0**. Không sửa mùa 2024/25, mã ứng dụng hoặc cấu hình deploy.

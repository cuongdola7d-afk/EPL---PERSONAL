# GW5 2026/27 — batch sau, năm trận theo ngày thi đấu

Batch này chỉ là dữ liệu chuẩn bị để chờ ảnh rating, **chưa nhập production**. `lineups.csv` có đủ 200 cầu thủ của năm trận. `manual-match-stats-2026-GW5-last-five-DRAFT.csv` chỉ có 140 khóa còn thiếu trên MySQL production: 104 `PLAYED` đang để rating trống và 36 `DID_NOT_PLAY`. `manual-players-2026-GW5-last-five-DRAFT.csv` bổ sung 140 khoảng membership đúng ngày trận. `ratings-needed.csv` liệt kê 104 người cần đối chiếu ảnh. Không tự gán rating 0 cho người không hiển thị điểm.

| Fixture | Trận | Đã có trên production | CSV draft còn thiếu | PLAYED chờ rating | Nguồn thống nhất đội hình/chỉ số |
| --- | --- | ---: | ---: | ---: | --- |
| `1000560589` | Nottingham Forest 0–1 Coventry, 19/09 | 0 | 40 | 29 | [StatMuse](https://www.statmuse.com/fc/match/9-19-2026-for-vs-cov-112808) |
| `1000560582` | Bournemouth 0–1 Liverpool, 20/09 | 20 Liverpool | 20 Bournemouth | 16 | [StatMuse](https://www.statmuse.com/fc/match/9-20-2026-bou-vs-liv-112812) |
| `1000560583` | Fulham 1–1 Manchester United, 20/09 | 20 Manchester United | 20 Fulham | 16 | [StatMuse](https://www.statmuse.com/fc/match/9-20-2026-ful-vs-mun-112803) |
| `1000560585` | Leeds 0–0 Crystal Palace, 20/09 | 0 | 40 | 29 | [StatMuse](https://www.statmuse.com/fc/match/9-20-2026-lee-vs-cry-112809) |
| `1000560590` | Manchester City 5–3 Sunderland, 20/09 | 20 Manchester City | 20 Sunderland | 14 | [StatMuse](https://www.statmuse.com/fc/match/9-20-2026-mci-vs-sun-112804) |

Đã đọc trực tiếp 80 dòng GW5 có sẵn trên Railway MySQL; 60 dòng Liverpool, Manchester United và Manchester City ở batch này khớp CSV cũ và năm cột chỉ số trên nguồn. Mỗi trang StatMuse có 11 người đá chính và chín dự bị mỗi đội; cầu thủ vào sân khớp sự kiện thay người. Fulham–Manchester United có một bàn phản lưới của Lisandro Martínez, nên tổng `goals` của Fulham bằng 0 nhưng tỷ số là 1–1. `DID_NOT_PLAY` chỉ dành cho dự bị không vào sân, chỉ số 0; `PLAYED` có đủ phút, bàn, kiến tạo, thẻ vàng và thẻ đỏ nhưng rating vẫn trống.

Importer H2 cô lập sao chép production và dữ liệu GW4 đã nhận roster 140 khoảng, stats 140 dòng sau batch đầu, không có xung đột. Dữ liệu production GW5 cũ không được ghi đè.

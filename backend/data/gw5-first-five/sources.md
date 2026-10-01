# GW5 2026/27 — batch đầu, năm trận theo ngày thi đấu

Batch này chỉ là dữ liệu chuẩn bị để chờ ảnh rating, **chưa nhập production**. `lineups.csv` có đủ 200 cầu thủ của năm trận. `manual-match-stats-2026-GW5-first-five-DRAFT.csv` chỉ có 180 khóa còn thiếu trên MySQL production: 141 `PLAYED` đang để rating trống và 39 `DID_NOT_PLAY`. `manual-players-2026-GW5-first-five-DRAFT.csv` bổ sung 180 khoảng membership đúng ngày trận. `ratings-needed.csv` liệt kê 141 người cần đối chiếu ảnh. Không tự gán rating 0 cho người không hiển thị điểm.

| Fixture | Trận | Đã có trên production | CSV draft còn thiếu | PLAYED chờ rating | Nguồn thống nhất đội hình/chỉ số |
| --- | --- | ---: | ---: | ---: | --- |
| `1000560591` | Brentford 3–0 Chelsea, 18/09 | 20 Chelsea | 20 Brentford | 16 | [StatMuse](https://www.statmuse.com/fc/match/-112810) |
| `1000560584` | Everton 1–0 Ipswich, 19/09 | 0 | 40 | 31 | [StatMuse](https://www.statmuse.com/fc/match/9-19-2026-eve-vs-ips-112807) |
| `1000560586` | Brighton 3–0 Arsenal, 19/09 | 0 | 40 | 32 | [StatMuse](https://www.statmuse.com/fc/match/9-19-2026-bha-vs-ars-112811) |
| `1000560587` | Tottenham 2–3 Aston Villa, 19/09 | 0 | 40 | 32 | [StatMuse](https://www.statmuse.com/fc/match/9-19-2026-tot-vs-avl-112806) |
| `1000560588` | Newcastle 2–1 Hull, 19/09 | 0 | 40 | 30 | [StatMuse](https://www.statmuse.com/fc/match/9-19-2026-new-vs-hul-112805) |

Đã đọc trực tiếp 80 dòng GW5 có sẵn trên Railway MySQL; 20 dòng Chelsea của trận `1000560591` khớp CSV cũ và năm cột chỉ số trên nguồn. Mỗi trang StatMuse có 11 người đá chính và chín dự bị mỗi đội; cầu thủ vào sân khớp sự kiện thay người, tổng bàn thắng khớp tỷ số. `DID_NOT_PLAY` chỉ dành cho dự bị không vào sân, chỉ số 0; `PLAYED` có đủ phút, bàn, kiến tạo, thẻ vàng và thẻ đỏ nhưng rating vẫn trống.

Importer H2 cô lập sao chép production và dữ liệu GW4 đã nhận roster 180 khoảng, stats 180 dòng, không có xung đột. Dữ liệu production GW5 cũ không được ghi đè.

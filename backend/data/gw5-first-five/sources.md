# GW5 2026/27 — batch đầu, năm trận theo ngày thi đấu

Batch được thu thập ban đầu để chờ ảnh rating. `lineups.csv` có đủ 200 cầu thủ của năm trận. `manual-match-stats-2026-GW5-first-five-DRAFT.csv` chỉ có 180 khóa còn thiếu trên MySQL production: 141 `PLAYED` ban đầu để rating trống và 39 `DID_NOT_PLAY`. `manual-players-2026-GW5-first-five-DRAFT.csv` bổ sung 180 khoảng membership đúng ngày trận. `ratings-needed.csv` là danh sách chờ ảnh ở giai đoạn DRAFT. Không tự gán rating 0 cho người không hiển thị điểm.

| Fixture | Trận | Dòng production trước batch | CSV bổ sung | PLAYED cần đối chiếu ảnh | Nguồn thống nhất đội hình/chỉ số |
| --- | --- | ---: | ---: | ---: | --- |
| `1000560591` | Brentford 3–0 Chelsea, 18/09 | 20 Chelsea | 20 Brentford | 16 | [StatMuse](https://www.statmuse.com/fc/match/-112810) |
| `1000560584` | Everton 1–0 Ipswich, 19/09 | 0 | 40 | 31 | [StatMuse](https://www.statmuse.com/fc/match/9-19-2026-eve-vs-ips-112807) |
| `1000560586` | Brighton 3–0 Arsenal, 19/09 | 0 | 40 | 32 | [StatMuse](https://www.statmuse.com/fc/match/9-19-2026-bha-vs-ars-112811) |
| `1000560587` | Tottenham 2–3 Aston Villa, 19/09 | 0 | 40 | 32 | [StatMuse](https://www.statmuse.com/fc/match/9-19-2026-tot-vs-avl-112806) |
| `1000560588` | Newcastle 2–1 Hull, 19/09 | 0 | 40 | 30 | [StatMuse](https://www.statmuse.com/fc/match/9-19-2026-new-vs-hul-112805) |

Đã đọc trực tiếp 80 dòng GW5 có sẵn trên Railway MySQL; 20 dòng Chelsea của trận `1000560591` khớp CSV cũ và năm cột chỉ số trên nguồn. Mỗi trang StatMuse có 11 người đá chính và chín dự bị mỗi đội; cầu thủ vào sân khớp sự kiện thay người, tổng bàn thắng khớp tỷ số. `DID_NOT_PLAY` chỉ dành cho dự bị không vào sân, chỉ số 0; `PLAYED` có đủ phút, bàn, kiến tạo, thẻ vàng và thẻ đỏ nhưng rating vẫn trống.

Importer H2 cô lập sao chép production và dữ liệu GW4 đã nhận roster 180 khoảng, stats 180 dòng, không có xung đột. Dữ liệu production GW5 cũ không được ghi đè.

## Ảnh rating nhận sau bản DRAFT

Ảnh người dùng đã cho rating của cả 141 người `PLAYED` mới trong batch. Bản áp rating nằm ở `manual-match-stats-2026-GW5-first-five-RATINGS.csv`; CSV cuối chỉ gồm 180 khóa mới là `manual-match-stats-2026-GW5-first-five.csv`. Đối chiếu 20 dòng Chelsea đã lưu phát hiện Danny Welbeck (`fixture_id=1000560591`, `player_id=2000005013`) có rating **6.6 trên production** nhưng **6.8 trong ảnh mới**. Theo quyết định của người dùng, giữ **6.6** trên production và không nhập lại khóa này.

`user-provided-ratings.csv` lưu 156 rating hiển thị trong ảnh (gồm 15 khóa Chelsea đã có); `production-rating-conflicts.csv` ghi chênh lệch và quyết định `KEEP_PRODUCTION`. Bản `RATINGS` đã nhập thử H2 cô lập thành công cùng batch sau; H2 giữ nguyên 80 khóa production cũ. Hai CSV cuối của batch là `manual-players-2026-GW5-first-five.csv` và `manual-match-stats-2026-GW5-first-five.csv`. Production import ngày 02/10/2026 thêm 180 khoảng membership và 180 dòng thống kê; nhập lại cả hai thêm 0. MySQL và API production xác nhận 40 dòng mỗi trận, khớp từng trường CSV cùng 20 dòng Chelsea cũ.

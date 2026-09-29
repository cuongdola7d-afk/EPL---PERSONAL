# GW1 2026/27 — năm fixture còn lại (batch local chưa đầy đủ)

Kiểm tra ngày **2026-09-29**. Fixture ID, club ID, ngày và tỉ số lấy từ H2 local; chỉ dùng SofaScore cho cầu thủ và chỉ số. Hai CSV nhập có đúng header 7 cột roster và 10 cột thống kê. File `*-missing.csv` liệt kê ô `NULL` theo người đã ghi vào batch.

| Fixture | Trận | `PLAYED` có bằng chứng | `DID_NOT_PLAY` | Thiếu rating | Thiếu phút |
| --- | --- | ---: | ---: | ---: | ---: |
| `1000560547` | Brentford 3–0 Tottenham, 22/08 | 28 | 0 | 24 | 28 |
| `1000560548` | Man City 2–1 Bournemouth, 23/08 | 22 | 0 | 18 | 21 |
| `1000560549` | Brighton 4–0 Aston Villa, 23/08 | 29 | 0 | 25 | 28 |
| `1000560550` | Newcastle 2–2 Liverpool, 23/08 | 26 | 0 | 25 | 26 |
| `1000560551` | Fulham 2–3 Chelsea, 24/08 | 26 | 0 | 23 | 26 |
| **Tổng** | | **131** | **0** | **115** | **129** |

## Bằng chứng theo trận

- `1000560547`: [diễn biến Brentford–Tottenham](https://www.sofascore.com/football/match/brentford-tottenham-hotspur/Isab), [bài tổng kết và rating Kelleher/Senesi/Robertson/van Hecke](https://www.sofascore.com/news/brentford-3-0-tottenham-kelleher-shines-as-stats-stack-up).
- `1000560548`: [diễn biến Man City–Bournemouth](https://www.sofascore.com/football/match/bournemouth-manchester-city/rkb), [bài tổng kết và bốn rating](https://www.sofascore.com/news/man-city-2-1-bournemouth-guehi-leads-late-rally-key-stats). [Hồ sơ Grealish](https://www.sofascore.com/football/player/jack-grealish/189061) cho thấy anh trở lại Everton sau GW1, vì vậy chỉ ghi membership Man City trong ngày trận.
- `1000560549`: [diễn biến Brighton–Villa](https://www.sofascore.com/football/match/aston-villa-brighton-and-hove-albion/FP), [bài tổng kết và rating](https://www.sofascore.com/news/brighton-4-0-aston-villa-hinshelwood-brace-headlines-a-dominant-premier-league-opener). Bài này ghi rating Jack Hinshelwood **9.0** ở phần phân tích nhưng **8.9** ở mục Player of the Match, nên rating của anh giữ `NULL` đến khi có ảnh đúng trận. Bài xác nhận anh chơi 64 phút. Bàn đầu tiên là **phản lưới Victor Lindelöf**: ba bàn của cầu thủ Brighton được ghi là 3, không thêm bàn phản lưới vào `goals` của Lindelöf. Thẻ vàng bị VAR đổi thành thẻ đỏ của João Gomes chưa được dùng để suy ra số thẻ vàng cuối cùng; chỉ ghi thẻ đỏ `1`.
- `1000560550`: [diễn biến Newcastle–Liverpool](https://www.sofascore.com/football/match/liverpool-fc-newcastle-united/OU), [bài tổng kết và rating Elanga](https://www.sofascore.com/news/liverpool-draw-late-at-newcastle-key-stats).
- `1000560551`: [diễn biến Fulham–Chelsea](https://www.sofascore.com/football/match/fulham-chelsea/NsT), [bài tổng kết và ba rating Chelsea](https://www.sofascore.com/news/fulham-2-3-chelsea-palmer-stars-in-premier-league-win). [Hồ sơ Enzo Fernández](https://www.sofascore.com/football/player/enzo-fernandez/974505) ghi chuyển từ Chelsea sang Man City ngày 01/09/2026. Dòng Chelsea GW1 dùng lại **player_id `2000004011`** đã cấp ở roster Man City; hai khoảng không chồng lấn.

Vị trí cầu thủ mới lấy từ trang đội của SofaScore: [Brentford](https://www.sofascore.com/football/team/brentford/50), [Tottenham](https://www.sofascore.com/football/team/tottenham-hotspur/33), [Bournemouth](https://www.sofascore.com/football/team/bournemouth/60), [Brighton](https://www.sofascore.com/football/team/brighton-and-hove-albion/30), [Aston Villa](https://www.sofascore.com/football/team/aston-villa/40), [Newcastle](https://www.sofascore.com/football/team/newcastle-united/39), [Fulham](https://www.sofascore.com/football/team/fulham/43). Mikey Moore được đối chiếu thêm qua [hồ sơ cầu thủ](https://www.sofascore.com/football/player/mikey-moore/1403192). ID đã có trong roster repo được giữ nguyên; 93 ID mới thuộc dải `2000030001` trở đi. Mỗi membership mới chỉ có hiệu lực `[ngày trận, ngày kế tiếp)`, không tự lùi hoặc kéo dài sang vòng khác.

## Giới hạn của batch

Trang SofaScore đọc được trong phiên này hiển thị **diễn biến** và một số rating trong bài tổng kết, nhưng tab đội hình không cung cấp danh sách đá chính/dự bị và bảng rating/phút đầy đủ ở bản nội dung truy xuất được. Vì vậy batch **chỉ ghi những người có bằng chứng ra sân từ diễn biến hoặc bài tổng kết**; không tuyên bố đã phủ đủ 22 cầu thủ đá chính, toàn bộ người vào sân hoặc dự bị. Không có dòng `DID_NOT_PLAY`: thiếu danh sách dự bị không phải bằng chứng không ra sân. Nhiều ô `goals`, `assists`, thẻ và phút của `PLAYED` giữ `NULL`; không suy ra `0` từ khoảng trống. Cần ảnh tab **Lineups** đúng năm trận, gồm đá chính, người vào sân, dự bị, rating và phút để hoàn thiện; đặc biệt cần xác nhận rating Jack Hinshelwood.

H2 cô lập `backend/target/gw1-last-five-20260929.mv.db` được sao từ H2 local, rồi nhập ba roster đã phát hành của Man City/Liverpool/Chelsea trước batch này. Importer roster thêm **93 player, 94 cặp player–club, 131 khoảng**, nhập lại thêm **0**. Importer thống kê thêm **131** dòng, nhập lại thêm **0**. Không có khóa `(fixture_id, player_id)` trùng hoặc membership sai ngày; 400 dòng thống kê thô mùa 2024/25 vẫn còn. Đây là kiểm tra local; **chưa nhập MySQL production**. Importer hiện báo xung đột nếu cùng khóa có dữ liệu khác, nên muốn bổ sung những ô `NULL` này sau khi có ảnh cần quy trình cập nhật có kiểm chứng, không nhập đè CSV mới một cách âm thầm.

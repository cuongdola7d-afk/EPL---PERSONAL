# Aaron Wan-Bissaka — Aston Villa GW1 2026/27

**Bản bổ sung chỉ để kiểm tra trên H2; chưa nhập MySQL production.**

- PremierHub `player_id=18846`, Aston Villa `club_id=1000000058`, GW1 fixture `1000560549` ngày 2026-08-23. API hiện chỉ có lịch sử từ GW2 cho ID này.
- [FPL bootstrap-static snapshot 30/09](../roster-2026-09-30/fpl-bootstrap-static.json) ghi Aaron Wan-Bissaka (`fpl_id=611`) ở Aston Villa (`team=2`), `team_join_date=2026-08-21`, `element_type=2` (DEF). Vì vậy CSV một dòng chỉ xác nhận membership CLB tại ngày GW1, với khoảng ngắn `[2026-08-23, 2026-08-24)`.
- [Ảnh Lineups SofaScore đã dùng cho GW1](../manual-match-stats-2026-GW1-last-five-LOCAL-notes.md) tạo 40 người cho Brighton–Villa, không có anh. [Trang trận SofaScore](https://www.sofascore.com/football/match/aston-villa-brighton-and-hove-albion/FP) và [đội hình StatMuse](https://www.statmuse.com/fc/match/8-23-2026-bha-vs-avl-112770) xác nhận danh sách đá chính/dự bị của Villa không có Aaron Wan-Bissaka.
- CSV GW1 và MySQL production đều không có dòng thống kê `(1000560549, 18846)`; MySQL cũng không có membership của anh ngày 23/08. GW2 dùng cùng ID `18846`; không lệch ID.

Không tạo CSV thống kê, `PLAYED`, `DID_NOT_PLAY`, phút hoặc rating. `DID_NOT_PLAY` chỉ phù hợp với cầu thủ có tên trên ghế dự bị nhưng không vào sân; ở GW1 anh không có tên trong đội hình trận.

Nếu nhập **chỉ** membership này, API lịch sử sẽ liệt kê fixture GW1 với `stats=null`. Giao diện hiện dùng nhãn chung “Thiếu thống kê cầu thủ” cho trường hợp đó; nhãn này không xác nhận anh ở đội hình hay ra sân.

## Kiểm tra H2

- Sao chép bản H2 kiểm tra GW2 sang thư mục local ignored. Trước nhập, không có membership của ID `18846` ngày 23/08.
- Importer roster thêm 0 cầu thủ, 0 liên kết cầu thủ–CLB, **1 khoảng membership**, cập nhật 0 khoảng; nhập lại thêm/cập nhật 0.
- API H2 `/api/players/18846/matches?season=2026` nay có GW1 `1000560549` với `stats=null`, rồi GW2 `1000560557` vẫn `PLAYED`, 13 phút. API chi tiết GW1 vẫn trả đúng 20 người mỗi đội và không tạo dòng Aaron.
- Không sửa backend/frontend, MySQL production, các batch GW2 hoặc mùa 2024/25. Quyết định nhập membership này ở production nằm ngoài lượt kiểm tra hiện tại.

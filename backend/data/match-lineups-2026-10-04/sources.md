# Nguồn đội hình 2026/27 — phạm vi GW1–GW5

Ngày rà soát local: **2026-10-04**. Chưa ghi MySQL production.

## Kết quả nguồn

- Tám file `backend/data/gw*/lineups.csv` đã có 1.560 cầu thủ đăng ký trận, thuộc 78 cặp CLB–fixture / 39 trận. Mỗi đội có đúng 11 `STARTER` và 9 `SUB_USED` hoặc `SUB_UNUSED`. `players.csv` giữ nguyên ID/vai trò đã lưu và trỏ tới `sources.md` của từng batch. Các batch cũ dùng StatMuse cho vai trò; lượt này chỉ tái sử dụng dữ liệu đó, không truy cập lại StatMuse hoặc thu thập rating/chỉ số.
- Những ghi chú SofaScore GW1 và GW4 đã lưu xác nhận từng có ảnh Lineups, nhưng không chứa danh sách 11 ID đá chính, sơ đồ hoặc tọa độ đủ để phục hồi bố cục. Không suy các dữ liệu này từ bảng PLAYED, rating, phút, vị trí mùa hay eligiblePositions.
- **Người dùng đã chốt sơ đồ mặc định của đủ 20 CLB ngày 04/10/2026.** `clubs.csv` lưu `default_source=USER` và ghi chú quyết định; 14 CLB dùng 4-2-3-1, 5 CLB dùng 3-4-3, Hull City dùng 5-4-1. Danh sách này là cấu hình mùa do người dùng chọn, không phải kết quả thống kê các fixture.
- **Chưa xác minh được sơ đồ thực tế của trận nào.** `formations.csv` vẫn chỉ có header; `verified_matches=0`, `formation_counts` và `fixture_ids` trống. Không ghi quyết định của người dùng thành bằng chứng trận hoặc tự thêm số lần dùng sơ đồ. Phạm vi GW1–GW5 trong CSV là phạm vi quan sát dự kiến; mặc định do người dùng chốt dùng cho toàn mùa 2026/27 khi fixture chưa có sơ đồ riêng.

## Truy xuất SofaScore đã thử

Chỉ SofaScore được dùng cho việc tìm bằng chứng sơ đồ mới:

- [Tottenham–Everton, 12/09/2026](https://www.sofascore.com/football/match/everton-tottenham-hotspur/UwXh#tab:lineups): trang overview có đúng cặp đội/ngày, nhưng truy xuất tab Lineups không trả sơ đồ hoặc danh sách vị trí.
- [Aston Villa–Nottingham Forest, 12/09/2026](https://www.sofascore.com/football/match/aston-villa-nottingham-forest/osP#tab:lineups): tương tự, không đọc được dữ liệu đội hình.
- Metadata [Premier League seasons](https://www.sofascore.com/api/v1/unique-tournament/17/seasons) có mùa 26/27, ID `96668`; metadata này không phải bằng chứng sơ đồ.
- Các endpoint `unique-tournament/17/season/96668/events/round/1`, `events/last/0`, `sport/football/scheduled-events/2026-09-12` và `team/33/events/last/0` không truy xuất được qua công cụ web. Biến thể `?tab=lineups` / `/lineups` cũng không đọc được. Truy cập HTTP trực tiếp trang SofaScore trả lỗi connection reset.

Không coi “không truy xuất được” là một trận đã kiểm tra sơ đồ. Chưa đối chiếu sơ đồ với website khác.

## Cần bổ sung ảnh

[`missing-lineups.csv`](missing-lineups.csv) liệt kê **100 cặp CLB–trận của 50 fixture GW1–GW5**, gồm fixture_id, GW, ngày, CLB, các dữ liệu còn thiếu:

- `formation_missing=1`: cần ảnh Lineups cho thấy tên CLB và chuỗi sơ đồ; ảnh đúng trận/ngày. Nếu ảnh bao gồm cả hai đội thì một ảnh có thể phục vụ hai dòng CSV.
- `positions_missing=1`: cần ảnh sân đủ 11 đá chính và vị trí/tọa độ; không chỉ bảng rating.
- `roles_missing=1`: chưa phục hồi được nhãn STARTER/SUB_USED/SUB_UNUSED vào dữ liệu mới của 22 cặp CLB–trận, thuộc 11 fixture: toàn bộ GW1 `1000560542`–`1000560551` và GW4 `1000560580`. Thống kê đã lưu vẫn đủ 50 trận × 40 dòng; ghi chú cũ xác nhận đã thu thập ảnh Lineups. Cần tìm lại nguồn cũ trước, không coi đây là 11 trận chưa thu thập thống kê hoặc yêu cầu người dùng cung cấp lại toàn bộ ảnh.

Những trận đã có vai trò chỉ cần bổ sung sơ đồ/vị trí; không cần nhập lại rating, số phút, bàn, kiến tạo hoặc thẻ.

## Phạm vi từng CLB

| Club ID | CLB | Default | Trận kiểm tra sơ đồ | Số lần mỗi sơ đồ | Trận có vai trò |
| --- | --- | --- | ---: | --- | ---: |
| 1000001044 | AFC Bournemouth | 4-2-3-1 | 0 | Chưa có bằng chứng | 4 |
| 1000000057 | Arsenal FC | 4-2-3-1 | 0 | Chưa có bằng chứng | 4 |
| 1000000058 | Aston Villa FC | 4-2-3-1 | 0 | Chưa có bằng chứng | 4 |
| 1000000402 | Brentford FC | 4-2-3-1 | 0 | Chưa có bằng chứng | 4 |
| 1000000397 | Brighton & Hove Albion FC | 4-2-3-1 | 0 | Chưa có bằng chứng | 4 |
| 1000000061 | Chelsea FC | 3-4-3 | 0 | Chưa có bằng chứng | 4 |
| 1000001076 | Coventry City FC | 3-4-3 | 0 | Chưa có bằng chứng | 4 |
| 1000000354 | Crystal Palace FC | 3-4-3 | 0 | Chưa có bằng chứng | 4 |
| 1000000062 | Everton FC | 4-2-3-1 | 0 | Chưa có bằng chứng | 4 |
| 1000000063 | Fulham FC | 4-2-3-1 | 0 | Chưa có bằng chứng | 4 |
| 1000000322 | Hull City AFC | 5-4-1 | 0 | Chưa có bằng chứng | 4 |
| 1000000349 | Ipswich Town FC | 4-2-3-1 | 0 | Chưa có bằng chứng | 4 |
| 1000000341 | Leeds United FC | 3-4-3 | 0 | Chưa có bằng chứng | 4 |
| 1000000064 | Liverpool FC | 4-2-3-1 | 0 | Chưa có bằng chứng | 4 |
| 1000000065 | Manchester City FC | 4-2-3-1 | 0 | Chưa có bằng chứng | 3 |
| 1000000066 | Manchester United FC | 4-2-3-1 | 0 | Chưa có bằng chứng | 3 |
| 1000000067 | Newcastle United FC | 4-2-3-1 | 0 | Chưa có bằng chứng | 4 |
| 1000000351 | Nottingham Forest FC | 3-4-3 | 0 | Chưa có bằng chứng | 4 |
| 1000000071 | Sunderland AFC | 4-2-3-1 | 0 | Chưa có bằng chứng | 4 |
| 1000000073 | Tottenham Hotspur FC | 4-2-3-1 | 0 | Chưa có bằng chứng | 4 |

Các số trong cột “Vai trò” đếm trận có nhãn đá chính đã phục hồi vào cấu trúc mới, **không phải** số trận đã kiểm tra sơ đồ. Nguồn của toàn bộ default trong bảng là quyết định người dùng ngày 04/10/2026. Sơ đồ thực tế đã xác minh của fixture vẫn được ưu tiên; vị trí mùa chỉ tạo bố cục minh họa.

# Nguồn đội hình 2026/27 — phạm vi GW1–GW5

Ngày rà soát local: **2026-10-04**. Chưa ghi MySQL production.

## Kết quả nguồn

- Tám file `backend/data/gw*/lineups.csv` đã có 1.560 cầu thủ đăng ký trận, thuộc 78 cặp CLB–fixture / 39 trận. Mỗi đội có đúng 11 `STARTER` và 9 `SUB_USED` hoặc `SUB_UNUSED`. `players.csv` giữ nguyên ID/vai trò đã lưu và trỏ tới `sources.md` của từng batch. Các batch cũ dùng StatMuse cho vai trò; lượt này chỉ tái sử dụng dữ liệu đó, không truy cập lại StatMuse hoặc thu thập rating/chỉ số.
- Những ghi chú SofaScore GW1 và GW4 đã lưu xác nhận từng có ảnh Lineups, nhưng không chứa danh sách 11 ID đá chính, sơ đồ hoặc tọa độ đủ để phục hồi bố cục. Không suy các dữ liệu này từ bảng PLAYED, rating, phút, vị trí mùa hay eligiblePositions.
- **Chưa xác minh được sơ đồ của trận nào.** `formations.csv` chỉ có header; `clubs.csv` có đúng 20 CLB, `default_formation` trống, `verified_matches=0`, `formation_counts` và `fixture_ids` trống. Không gán mặc định 4-3-3 hoặc một sơ đồ theo nhận định về HLV.

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
- `roles_missing=1`: còn thiếu 11 ID đá chính và danh sách dự bị có xác nhận vai trò. Có 22 cặp CLB–trận thiếu vai trò, thuộc 11 fixture: toàn bộ GW1 `1000560542`–`1000560551` và GW4 `1000560580`.

Những trận đã có vai trò chỉ cần bổ sung sơ đồ/vị trí; không cần nhập lại rating, số phút, bàn, kiến tạo hoặc thẻ.

## Phạm vi từng CLB

| Club ID | CLB | Default | Trận kiểm tra sơ đồ | Số lần mỗi sơ đồ | Trận có vai trò |
| --- | --- | --- | ---: | --- | ---: |
| 1000001044 | AFC Bournemouth | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000000057 | Arsenal FC | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000000058 | Aston Villa FC | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000000402 | Brentford FC | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000000397 | Brighton & Hove Albion FC | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000000061 | Chelsea FC | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000001076 | Coventry City FC | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000000354 | Crystal Palace FC | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000000062 | Everton FC | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000000063 | Fulham FC | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000000322 | Hull City AFC | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000000349 | Ipswich Town FC | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000000341 | Leeds United FC | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000000064 | Liverpool FC | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000000065 | Manchester City FC | NULL | 0 | Chưa có bằng chứng | 3 |
| 1000000066 | Manchester United FC | NULL | 0 | Chưa có bằng chứng | 3 |
| 1000000067 | Newcastle United FC | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000000351 | Nottingham Forest FC | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000000071 | Sunderland AFC | NULL | 0 | Chưa có bằng chứng | 4 |
| 1000000073 | Tottenham Hotspur FC | NULL | 0 | Chưa có bằng chứng | 4 |

Các số trong cột “Vai trò” đếm trận có danh sách 11 đá chính đã lưu, **không phải** số trận đã kiểm tra sơ đồ. Tất cả 20 CLB vẫn thiếu sơ đồ thường dùng cho tới khi có bằng chứng Lineups đọc được.

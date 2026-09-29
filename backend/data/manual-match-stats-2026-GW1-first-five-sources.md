# GW1 2026/27 — 5 trận đầu, bản nháp chưa thể nhập

Kiểm tra ngày **2026-09-29**. Hai file `*-DRAFT.csv` chỉ chứa các cầu thủ có bằng chứng đã ra sân từ **đúng trận** trên SofaScore. Chưa có ảnh Lineups đầy đủ, danh sách dự bị không vào sân hoặc thống kê từng người cho toàn bộ hai đội. **Không chạy importer với bản nháp này**: `PLAYED` thiếu rating sẽ để điểm Fantasy `NULL`, còn trận/đội chưa được phủ hết.

## Fixture và nguồn trận

Thứ tự lấy từ H2 local theo `match_date, fixture_id`; cả 5 đều `FINISHED`. `club_id` và `fixture_id` là ID PremierHub trong database, không phải ID SofaScore.

| Fixture | Ngày | Club ID và kết quả | SofaScore đúng trận |
| --- | --- | --- | --- |
| `1000560542` | 2026-08-21 | Arsenal `1000000057` 3–0 Coventry `1000001076` | [Diễn biến Arsenal–Coventry](https://www.sofascore.com/football/match/arsenal-coventry-city/lsR) |
| `1000560543` | 2026-08-22 | Hull `1000000322` 2–0 Man Utd `1000000066` | [Diễn biến Hull–Man Utd](https://www.sofascore.com/football/match/hull-city-manchester-united/KsWb), [bài tổng kết cùng trận](https://www.sofascore.com/news/hull-city-2-0-manchester-united-set-pieces-and-saves) |
| `1000560544` | 2026-08-22 | Ipswich `1000000349` 2–1 Sunderland `1000000071` | [Diễn biến Ipswich–Sunderland](https://www.sofascore.com/football/match/sunderland-ipswich-town/HsQ), [bài tổng kết cùng trận](https://www.sofascore.com/news/ipswich-town-2-1-sunderland-late-strike-settles-opener) |
| `1000560545` | 2026-08-22 | Forest `1000000351` 0–1 Leeds `1000000341` | [Bài tổng kết đúng trận Premier League](https://www.sofascore.com/news/nottingham-forest-0-1-leeds-united-stachs-late-free-kick). URL trận đấu Forest–Leeds chung trên SofaScore hiện mở **trận EFL Cup 25/08**, không dùng diễn biến đó. |
| `1000560546` | 2026-08-22 | Everton `1000000062` 2–0 Palace `1000000354` | [Diễn biến Everton–Palace](https://www.sofascore.com/football/match/everton-crystal-palace/hY), [bài tổng kết cùng trận](https://www.sofascore.com/news/everton-2-0-crystal-palace-dewsbury-hall-leads-the-way) |

## Phạm vi CSV và membership

| Fixture | Dòng `PLAYED` đã định danh | Rating SofaScore đã có | Rating còn thiếu |
| --- | ---: | ---: | ---: |
| `1000560542` | 21 | 0 | 21 |
| `1000560543` | 18 | 4 | 14 |
| `1000560544` | 7 | 6 | 1 |
| `1000560545` | 7 | 7 | 0 |
| `1000560546` | 7 | 5 | 2 |
| **Tổng** | **60** | **22** | **38** |

Không có dòng `DID_NOT_PLAY`: thiếu ảnh dự bị nên chưa thể kết luận ai không vào sân. Các số `0` cũng không được tự suy ra từ việc cầu thủ không xuất hiện trong dòng sự kiện. Chỉ có bàn thắng, kiến tạo, thẻ dương và hai giá trị phút được nguồn đúng trận nêu trực tiếp. Tổng bàn thắng dương trong nháp lần lượt là 3, 2, 3, 1, 2, khớp tỉ số của 5 trận. 58/60 ô phút còn trống.

CSV roster có **34 khoảng lịch sử cho ID đã tồn tại**: 21 Arsenal/Coventry từ 21/08 đến trước 22/09; 13 Man Utd từ 22/08 đến trước 03/09. `end_date` là biên loại trừ theo importer. Khoảng cũ mở từ 22/09 hoặc 03/09 vẫn giữ nguyên, không sửa hàng loạt roster. Có **26 ID mới** chỉ cho cầu thủ được xác nhận đã chơi trận này, `start_date` là ngày trận và `end_date` trống. Không cấp ID cho người chỉ xuất hiện trong roster hiện tại hoặc chưa định danh chắc chắn. Vì là nháp, các khoảng này **chưa được nhập**.

Vị trí GK/DEF/MID/FWD của 26 người mới đối chiếu từ trang cầu thủ SofaScore; việc họ chơi cho CLB vào ngày GW1 lấy từ diễn biến/bài tổng kết đúng trận ở bảng trên:

- Hull: [Tzolakis](https://www.sofascore.com/football/player/konstantinos-tzolakis/953414), [Ajayi](https://www.sofascore.com/football/player/semi-ajayi/307274), [Mendy](https://www.sofascore.com/football/player/nobel-mendy/1458073), [Slater](https://www.sofascore.com/football/player/regan-slater/864474), [Egan](https://www.sofascore.com/football/player/john-egan/100578).
- Ipswich: [Emersonn](https://www.sofascore.com/football/player/emersonn/1128844), [Enciso](https://www.sofascore.com/football/player/julio-enciso/973556), [Clarke](https://www.sofascore.com/football/player/jack-clarke/921005), [Lukić](https://www.sofascore.com/football/player/sasa-lukic/371222), [Núñez](https://www.sofascore.com/football/player/marcelino-nunez/1014801).
- Sunderland: [Angulo](https://www.sofascore.com/football/player/nilson-angulo/1116571), [Xhaka](https://www.sofascore.com/football/player/granit-xhaka/117777).
- Forest: [McAtee](https://www.sofascore.com/football/player/james-mcatee/1003334), [Murillo](https://www.sofascore.com/football/player/murillo/1199282).
- Leeds: [Stach](https://www.sofascore.com/football/player/anton-stach/889861), [Trafford](https://www.sofascore.com/football/player/james-trafford/980643), [Bijol](https://www.sofascore.com/football/player/jaka-bijol/886930), [Justin](https://www.sofascore.com/football/player/james-justin/827681), [Rodon](https://www.sofascore.com/football/player/joe-rodon/828640).
- Everton: [Dewsbury-Hall](https://www.sofascore.com/football/player/kiernan-dewsbury-hall/861970), [Barry](https://www.sofascore.com/football/player/thierno-barry/1395746), [Ndiaye](https://www.sofascore.com/football/player/iliman-ndiaye/914309), [Armstrong](https://www.sofascore.com/football/player/harrison-armstrong/1627560), [Branthwaite](https://www.sofascore.com/football/player/jarrad-branthwaite/979563).
- Palace: [Kamada](https://www.sofascore.com/football/player/daichi-kamada/794338), [Khalaili](https://www.sofascore.com/football/player/anan-khalaili/1403362).

## Cần bổ sung từ ảnh SofaScore

Ảnh **Lineups của cả hai đội** cho từng fixture, gồm đá chính, dự bị và rating; ảnh **thống kê cầu thủ** để điền phút/bàn/kiến tạo/thẻ còn trống. Người có tên trong danh sách dự bị nhưng không có lần vào sân mới có thể ghi `DID_NOT_PLAY`. Một số người đã xuất hiện trong diễn biến/bài đúng trận nhưng **chưa có ID trong CSV roster**; cần đối chiếu trang cầu thủ trước khi cấp ID mới:

- `1000560542`: diễn biến công khai đã ánh xạ đủ 21 tên xuất hiện; những người đá chính/dự bị còn lại chỉ xác định được qua ảnh Lineups.
- `1000560543`: Hull — Oli McBurnie, Lewie Coyle, Ryan Giles, Lucas Herrington, Cody Drameh, Matt Targett, Paddy McNair, Matt Crooks, Liam Millar, Elliot Stroud. Man Utd — những người khác trong đội hình chưa thấy ở nguồn tĩnh.
- `1000560544`: Ipswich — Kjell Scherpen, Abdul Fatawu Issahaku, Dara O'Shea, Chuba Akpom, Kasey McAteer. Sunderland — Brian Brobbey, Wilson Isidor, Luke O'Nien, Omar Alderete, Chemsdine Talbi, Daniel Ballard, Habib Diarra, Chris Rigg.
- `1000560545`: Forest — Morgan Gibbs-White, Igor Jesus, Matz Sels, Ola Aina, Jair, Chris Wood. Leeds — Dominic Calvert-Lewin, Noah Okafor. Chỉ bài đúng trận 22/08 được dùng, không lấy sự kiện trận EFL Cup 25/08.
- `1000560546`: Everton — Jordan Pickford, Vitaliy Mykolenko, Merlin Röhl, Tyrique George, Dwight McNeil, Evann Guessand, Hayden Hackney, James Garner, Beto, Brennan Johnson, Carlos Alcaraz. Palace — Eddie Nketiah, Jean-Philippe Mateta, Dean Henderson, Daniel Muñoz, Yéremy Pino, Jørgen Strand Larsen, Chadi Riad, Takehiro Tomiyasu.

Danh sách trên là **những tên đã thấy trong nguồn tĩnh nhưng chưa nhập**, không phải danh sách thiếu đầy đủ của 5 trận. Không suy ra người khác vắng mặt hoặc đã rời CLB.

Rating thiếu trong **các dòng đã có ID**:

- `1000560542`: Ben White, Piero Hincapié, Gabriel Magalhães, Bukayo Saka, Martin Ødegaard, Eberechi Eze, Christos Tzolis, Noni Madueke, Mikel Merino, Kai Havertz, Riccardo Calafiori, Martin Zubimendi, Declan Rice; Jack Rudoni, Caleb Yirenkyi, Ellis Simms, Taiwo Awoniyi, Loum Tchaouna, Brandon Thomas-Asante, Victor Torp, Gustavo Hamer.
- `1000560543`: Diogo Dalot, Noussair Mazraoui, Harry Maguire, Bruno Fernandes, Marcus Rashford, Matheus Cunha, Patrick Dorgu, Andrey Santos, Youri Tielemans, Luke Shaw, Benjamin Šeško, Kobbie Mainoo, Shea Lacey; Regan Slater.
- `1000560544`: Saša Lukić.
- `1000560545`: không thiếu rating trong 7 dòng đã định danh, nhưng còn nhiều cầu thủ chưa có roster/dòng thống kê.
- `1000560546`: Thierno Barry, Harrison Armstrong.

**Chưa fixture nào hoàn chỉnh.** Số `PLAYED` mỗi trận còn thấp hơn số cầu thủ thực tế của hai đội; không dùng batch này để tính tổng Fantasy của trận. Khi có ảnh, bổ sung roster cho người chưa có ID, xác nhận từng rating/chỉ số và dự bị không vào sân, rồi mới kiểm tra importer trên H2 cô lập.

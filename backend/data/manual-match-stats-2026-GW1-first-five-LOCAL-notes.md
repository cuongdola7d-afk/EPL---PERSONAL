# GW1 2026/27 — năm fixture đầu, dữ liệu local

Kiểm tra ngày **2026-09-29**. Phạm vi chỉ gồm fixture `1000560542`–`1000560546`. Hai file do người dùng cung cấp, `*-RATINGS.csv` và `*-PENDING-ROSTER.csv`, được giữ nguyên. CSV nhập được tạo riêng với header 7 cột roster và 10 cột thống kê.

## Nguồn và quy tắc ghép

- Fixture ID, club ID, ngày và tỉ số lấy từ H2 local. Các nguồn SofaScore đúng trận: [Arsenal–Coventry](https://www.sofascore.com/football/match/arsenal-coventry-city/lsR), [Hull–Man Utd](https://www.sofascore.com/football/match/hull-city-manchester-united/KsWb), [Ipswich–Sunderland](https://www.sofascore.com/football/match/sunderland-ipswich-town/HsQ), [Forest–Leeds **Premier League 22/08**](https://www.sofascore.com/news/nottingham-forest-0-1-leeds-united-stachs-late-free-kick), [Everton–Palace](https://www.sofascore.com/football/match/everton-crystal-palace/hY). URL trận chung Forest–Leeds hiện mở EFL Cup 25/08 nên không dùng sự kiện ở trang đó.
- Giữ nguyên **60 rating và các chỉ số** trong `*-RATINGS.csv`, gồm sáu lựa chọn chênh `0,1` đã chốt từ ảnh. Thêm 92 dòng `PLAYED` và 46 dòng `DID_NOT_PLAY` từ file đối chiếu; giữ nguyên 90 rating có sẵn trong 92 dòng `PLAYED`. `DID_NOT_PLAY` chỉ dùng cho người được ghi là dự bị không vào sân: rating trống, phút `0`, các chỉ số khác trống, điểm Fantasy do importer lưu là `0`.
- ID cũ được đối chiếu với các roster CSV đã phát hành và bản nháp trước. “Arnaud Amenda” ở file đối chiếu được ánh xạ tới **Aurèle Amenda `2000002021`**, căn cứ [hồ sơ SofaScore](https://www.sofascore.com/football/player/aurele-amenda/999276) và [tin chuyển nhượng Coventry](https://www.sofascore.com/news/what-aurele-amenda-brings-to-coventry-as-the-premier-league-challenge-begins); tên chuẩn lưu trong roster không đổi. 143 ID mới thuộc dải `200001...` của bản nháp trước và `200002...` cấp ở lượt này; đã kiểm tra không trùng các ID trong repo và H2 local.
- Vị trí đối chiếu theo [Arsenal](https://www.sofascore.com/football/team/arsenal/42), [Coventry](https://www.sofascore.com/football/team/coventry-city/11), [Hull](https://www.sofascore.com/football/team/hull-city/96), [Ipswich](https://www.sofascore.com/football/team/ipswich-town/32), [Sunderland](https://www.sofascore.com/football/team/sunderland/41), [Forest](https://www.sofascore.com/football/team/nottingham-forest/14), [Leeds](https://www.sofascore.com/football/team/leeds-united/34), [Everton](https://www.sofascore.com/football/team/everton/48), [Palace](https://www.sofascore.com/football/team/crystal-palace/7) và hồ sơ cá nhân SofaScore cho người đã chuyển đội hoặc không còn ở danh sách hiện tại. Các trường hợp dễ nhầm đã kiểm tra hồ sơ: [Liam Kitching](https://www.sofascore.com/football/player/liam-kitching/921006), [Cody Drameh](https://www.sofascore.com/football/player/cody-drameh/991577), [Liam Millar](https://www.sofascore.com/football/player/liam-millar/902083), [Jayden Lienou](https://www.sofascore.com/football/player/jayden-lienou/1899485), [Alfie Cresswell](https://www.sofascore.com/football/player/alfie-cresswell/1809530), [Borna Sosa](https://www.sofascore.com/football/player/borna-sosa/357584), [Lucas Perri](https://www.sofascore.com/football/player/lucas-perri/871290), [Beto](https://www.sofascore.com/football/player/beto/987489), [Daniel Muñoz](https://www.sofascore.com/football/player/daniel-munoz/870360).
- Khoảng roster được thêm chỉ có hiệu lực **trong ngày trận** `[match_date, match_date + 1 ngày)`; đây là bằng chứng của GW1, không tự khẳng định cầu thủ vẫn thuộc đội ở ngày khác. 55 ID đã có roster về sau vẫn giữ nguyên khoảng cũ, không chồng lấn. Việc nhập H2 bản sao cho thấy 143 cầu thủ mới, 198 khoảng mới; nhập lại thêm `0`.

## Trạng thái từng fixture

| Fixture | `PLAYED` | `DID_NOT_PLAY` | Rating thiếu | Bàn thắng dương khớp tỉ số | Phút của `PLAYED` còn `NULL` |
| --- | ---: | ---: | ---: | --- | ---: |
| `1000560542` Arsenal 3–0 Coventry | 30 | 9 | 0 | 3–0 | 30 |
| `1000560543` Hull 2–0 Man Utd | 31 | 8 | 0 | 2–0 | 30 |
| `1000560544` Ipswich 2–1 Sunderland | 32 | 8 | 0 | 2–1 | 31 |
| `1000560545` Forest 0–1 Leeds | 28 | 12 | **1** | 0–1 | 28 |
| `1000560546` Everton 2–0 Palace | 31 | 9 | **1** | 2–0 | 31 |
| **Tổng** | **152** | **46** | **2** | | **150** |

Hai rating chưa lấy được ở đúng trận SofaScore: **Arnaud Kalimuendo** (`1000560545`, `2000020064`) và **Carlos Alcaraz** (`1000560546`, `2000020094`). Rating và điểm Fantasy của hai dòng này vẫn `NULL`, không tự đặt điểm. Các trường chưa xác minh khác cũng giữ `NULL`; **không coi ô trống là số 0**. [Bảng chi tiết theo từng cầu thủ](manual-match-stats-2026-GW1-first-five-LOCAL-missing.csv) liệt kê chính xác các trường còn thiếu trong 152 dòng `PLAYED`.

Ở H2 cô lập, importer thống kê lần đầu thêm **198**, lần hai thêm **0**. Cả 46 dòng `DID_NOT_PLAY` có rating `NULL` và fantasy_points `0`; 150 dòng `PLAYED` có rating/điểm bằng rating nguồn, hai dòng còn lại giữ `NULL`. 400 thống kê thô mùa 2024/25 của bản H2 gốc vẫn là 400 trong bản sao. Đây là kiểm tra **local**, chưa nhập Railway MySQL.

Giới hạn: trang SofaScore công khai ở phiên kiểm tra không hiện đủ phút và chỉ số 0 theo từng người; hai rating nêu trên cũng chưa tìm được. Dữ liệu nhập được về cấu trúc và membership, nhưng **chưa nên tuyên bố năm trận hoàn toàn đủ thống kê cầu thủ hoặc điểm Fantasy**. Cần ảnh màn hình thống kê của đúng trận để bổ sung các ô trong bảng thiếu; importer sẽ báo xung đột nếu nhập lại CSV đã có nội dung khác, nên việc bổ sung sau này cần quy trình cập nhật dữ liệu có kiểm chứng.

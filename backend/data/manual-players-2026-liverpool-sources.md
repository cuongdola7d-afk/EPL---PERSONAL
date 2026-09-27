# Liverpool 2026/27: batch khởi đầu

**Ngày kiểm tra: 27/09/2026.** CSV cùng thư mục gồm tối đa 25 người để nhập thủ công sau này; **không phải roster đầy đủ** hoặc đội hình của một trận cụ thể. `GET /api/clubs?season=2026` trên bản sao H2 local trả `Liverpool FC`, `club_id=1000000064`.

## Nguồn và cách chọn

1. [Premier League, danh sách đăng ký 2026/27 công bố 03/09/2026](https://www.premierleague.com/en/news/4706139/see-all-the-202627-premier-league-squad-lists): mục Liverpool liệt kê cả nhóm 25 người đăng ký và nhóm U21. **Cả 25 tên trong CSV đều có trong hai nhóm này.** [Liverpool FC công bố danh sách 25 người cùng ngày](https://www.liverpoolfc.com/news/liverpool-submit-premier-league-squad-list-2026-27?amp=1) xác nhận thêm nhóm đội một.
2. [Premier League, danh sách số áo 22/09/2026](https://www.premierleague.com/en/news/4725682/all-20-premier-league-clubs-squad-numbers-for-202627-season) và [Liverpool FC, trang đội nam](https://www.liverpoolfc.com/teams/mens-team?position=all) được đối chiếu ngày 27/09/2026. Danh sách số áo không đủ một mình để kết luận cầu thủ vẫn ở CLB vì có thể chậm cập nhật.
3. [FPL bootstrap-static](https://fantasy.premierleague.com/api/bootstrap-static/) kiểm tra ngày **27/09/2026**, `team=14`, `element_type`: 1 GK, 2 DEF, 3 MID, 4 FWD. CSV dùng **vị trí Fantasy** này. Vì vậy Cody Gakpo, Bradley Barcola, Victor Munoz và Lewis Koumas là `MID` dù trang Liverpool xếp vài người ở nhóm tiền đạo. FPL còn cho biết phút và tình trạng hiện tại để ưu tiên người có cơ hội ra sân; tình trạng chấn thương có thể thay đổi.

`start_date=2026-09-03` là ngày sớm nhất **trong các nguồn đã kiểm tra** có danh sách Premier League xác nhận từng người trong CSV thuộc Liverpool, không phải ngày ký hợp đồng hoặc ngày gia nhập thực tế. Những người U21 như Jeremy Jacquet, Trey Nyoni, Rio Ngumoha, Lewis Koumas và Jayden Danns cũng nằm trong danh sách 03/09. **Không dùng batch này để tái hiện GW1 hoặc roster trước 03/09/2026**; cần nguồn lịch sử theo từng cầu thủ để làm việc đó. `end_date` để trống vì chưa có bằng chứng ngày rời Liverpool của các dòng được chọn.

ID `2000003001..2000003025` được cấp thủ công, không suy từ tên hoặc ID FPL; dải này không trùng các ID đã cấp hoặc giữ chỗ cho Arsenal–Coventry (`2000001001..2000001025`, `2000002001..2000002029`). Khi cầu thủ chuyển CLB, phải dùng lại ID đã cấp.

## Các trường hợp tạm hoãn hoặc chưa xác minh

Mỗi quyết định dưới đây được kiểm tra ngày **27/09/2026**. Tạm hoãn do giới hạn 25 hoặc tình trạng thi đấu **không** có nghĩa là cầu thủ đã rời CLB.

| Cầu thủ | Quyết định và căn cứ | Nguồn |
| --- | --- | --- |
| Conor Bradley | Tạm hoãn; FPL ghi chấn thương đầu gối, 0 phút, chưa rõ ngày trở lại. | [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Giovanni Leoni | Tạm hoãn; FPL ghi chấn thương đầu gối, 0 phút, chưa rõ ngày trở lại. | [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Vitezslav Jaros | Tạm hoãn; FPL ghi chấn thương đầu gối, 0 phút, chưa rõ ngày trở lại; đã chọn ba GK. | [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Federico Chiesa | Tạm hoãn; FPL xếp MID và ghi chấn thương lưng, 0 phút, chưa rõ ngày trở lại. | [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Harvey Davies | Tạm hoãn do giới hạn 25; FPL ghi GK, 0 phút và đang có thể thi đấu, nhưng batch đã có ba GK. | [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Luke Chambers | Tạm hoãn do giới hạn 25; có trong danh sách đăng ký và trang đội nam ghi DEF, nhưng không có dòng FPL để kiểm tra vị trí Fantasy/khả năng thi đấu. | [Liverpool FC](https://www.liverpoolfc.com/teams/mens-team?position=all), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Isaac Mabaya | **Chưa xác minh** vị trí Fantasy và khả năng thi đấu: có trong danh sách đăng ký 03/09 nhưng không có dòng FPL ngày kiểm tra. Chưa cấp ID. | [Premier League](https://www.premierleague.com/en/news/4706139/see-all-the-202627-premier-league-squad-lists), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Wellity Lucky | Tạm hoãn do giới hạn 25; FPL ghi DEF, 0 phút; PL đăng ký ở nhóm U21. | [Premier League](https://www.premierleague.com/en/news/4706139/see-all-the-202627-premier-league-squad-lists), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Harvey Elliott, Armin Pecsi, Calvin Ramsay | Tạm hoãn; trang đội nam Liverpool ghi trong nhóm **On loan**. Chưa cấp ID trong batch này. | [Liverpool FC](https://www.liverpoolfc.com/teams/mens-team?position=all) |

Hugo Ekitike **vẫn được đưa vào** để giữ ba lựa chọn FWD; FPL ngày kiểm tra ghi chấn thương Achilles, chưa rõ ngày trở lại. Jayden Danns có 0 phút PL nhưng FPL ghi đang có thể thi đấu và PL đăng ký U21. Đây là lựa chọn cho danh sách ban đầu, không dự báo họ sẽ ra sân trận tới. Không nhập bàn thắng, kiến tạo hoặc điểm Fantasy.

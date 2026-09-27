# Manchester City 2026/27: batch khởi đầu

**Ngày kiểm tra: 27/09/2026.** Đây là danh sách chọn lọc tối đa 25 cầu thủ để nhập thủ công, **không phải roster đầy đủ hoặc đội hình một trận**. API local `GET /api/clubs?season=2026` trên bản sao H2 trả Manchester City FC với `club_id=1000000065`.

## Căn cứ CSV

- [Premier League: danh sách đăng ký 2026/27 ngày 03/09/2026](https://www.premierleague.com/en/news/4706139/see-all-the-202627-premier-league-squad-lists), mục Manchester City, gồm nhóm đội một và U21: **cả 25 người trong CSV đều có tên**. [Danh sách số áo PL ngày 22/09/2026](https://www.premierleague.com/en/news/4725682/all-20-premier-league-clubs-squad-numbers-for-202627-season) được đối chiếu thêm; nguồn này có thể chậm cập nhật chuyển nhượng nên không dùng đơn lẻ.
- [FPL bootstrap-static](https://fantasy.premierleague.com/api/bootstrap-static/) kiểm tra **27/09/2026**, `team=15`, xác nhận tên/CLB hiện được FPL gắn, vị trí Fantasy (`element_type`: 1 GK, 2 DEF, 3 MID, 4 FWD), phút và tình trạng thi đấu. CSV dùng vị trí Fantasy, không đổi các tiền đạo cánh thành `FWD`: FPL hiện chỉ xếp **Erling Haaland** là `FWD` của Manchester City. Vì vậy batch chỉ có một lựa chọn FWD; không tạo thêm vị trí thiếu căn cứ.
- `start_date=2026-09-03` là ngày **sớm nhất trong các nguồn đã kiểm tra** xác nhận từng người được chọn thuộc Manchester City qua danh sách đăng ký Premier League. Đây không phải ngày ký hợp đồng hoặc ngày gia nhập thực tế. `end_date` trống khi chưa có căn cứ ngày rời CLB. **Batch không dùng để tái hiện GW1 hoặc roster trước 03/09/2026**; cần nguồn lịch sử theo từng người cho thời gian đó.
- `player_id=2000004001..2000004025` được cấp nội bộ và giữ ổn định cho những người này, không suy từ tên hoặc ID FPL. Không trùng dải đã cấp/giữ chỗ Arsenal–Coventry (`2000001001..2000001025`, `2000002001..2000002029`) hoặc Liverpool (`2000003001..2000003025`).

FPL ngày kiểm tra ghi Phil Foden **bị treo giò đến 17/10**, Antoine Semenyo **nghi vấn chấn thương mắt cá, 75% khả năng thi đấu**. Cả hai vẫn có tên trong danh sách vì tình trạng thi đấu không phải bằng chứng đã rời CLB. Kaden Braithwaite và Vitor Reis thuộc nhóm U21 nhưng FPL đã ghi nhận phút PL; họ là lựa chọn chiều sâu, không được mô tả là cầu thủ đá chính thường xuyên. Một vài GK có 0 phút nhưng vẫn thuộc danh sách đội một và cho đủ lựa chọn thủ môn.

## Người tạm hoãn hoặc chưa xác minh

Mỗi dòng dưới đây được kiểm tra **27/09/2026**. Tạm hoãn chỉ có nghĩa là không có trong batch giới hạn 25, không tự suy ra người đó đã rời CLB. Không cấp ID cho người chưa đưa vào CSV.

| Cầu thủ | Quyết định, lý do | Nguồn |
| --- | --- | --- |
| Floyd Samba | Tạm hoãn do giới hạn 25; PL đăng ký U21, FPL xếp MID nhưng 0 phút tính đến ngày kiểm tra. | [Premier League, 03/09](https://www.premierleague.com/en/news/4706139/see-all-the-202627-premier-league-squad-lists), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Claudio Echeverri | **Chưa xác minh tình trạng hiện tại bằng thông báo CLB**; PL còn số áo ngày 22/09 nhưng FPL báo cho Benfica mượn. Tạm hoãn khi nguồn mâu thuẫn. | [Premier League, 22/09](https://www.premierleague.com/en/news/4725682/all-20-premier-league-clubs-squad-numbers-for-202627-season), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Rodri | Tạm hoãn; FPL báo đã chuyển đến Barcelona, không có trong danh sách đăng ký Man City ngày 03/09. Chưa dùng thông tin này để ghi lịch sử chuyển đội. | [FPL](https://fantasy.premierleague.com/api/bootstrap-static/), [Premier League, 03/09](https://www.premierleague.com/en/news/4706139/see-all-the-202627-premier-league-squad-lists) |
| Tijjani Reijnders | Tạm hoãn; FPL báo đã chuyển đến Al Qadsiah, không có trong danh sách đăng ký Man City ngày 03/09. Chưa dùng thông tin này để ghi lịch sử chuyển đội. | [FPL](https://fantasy.premierleague.com/api/bootstrap-static/), [Premier League, 03/09](https://www.premierleague.com/en/news/4706139/see-all-the-202627-premier-league-squad-lists) |
| Max Alleyne | Tạm hoãn; danh sách đăng ký PL nhóm U21 ghi rõ `Loan`, FPL báo cho Burnley mượn. | [Premier League, 03/09](https://www.premierleague.com/en/news/4706139/see-all-the-202627-premier-league-squad-lists), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Divine Mukasa | Tạm hoãn; danh sách đăng ký PL nhóm U21 ghi `Loan`, FPL báo cho West Ham mượn. | [Premier League, 03/09](https://www.premierleague.com/en/news/4706139/see-all-the-202627-premier-league-squad-lists), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Jeremy Monga | Tạm hoãn; danh sách đăng ký PL nhóm U21 ghi `Loan`, FPL báo cho Swansea mượn. | [Premier League, 03/09](https://www.premierleague.com/en/news/4706139/see-all-the-202627-premier-league-squad-lists), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Kalvin Phillips | Tạm hoãn; FPL báo cho Sheffield United mượn. | [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |

Không nhập bàn thắng, kiến tạo hoặc điểm Fantasy trong batch này.

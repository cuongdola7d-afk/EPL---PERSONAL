# Manchester United 2026/27: roster khởi đầu

**Kiểm tra ngày 27/09/2026.** Đây là 25 lựa chọn ban đầu, không phải toàn bộ roster hay đội hình một trận. `GET https://epl-personal-production.up.railway.app/api/clubs?season=2026` trả `Manchester United FC` với `club_id=1000000066`.

## Căn cứ cho CSV

- [Premier League: squad lists 2026/27, công bố 03/09/2026](https://www.premierleague.com/en/news/4706139/see-all-the-202627-premier-league-squad-lists): cả 25 người trong CSV có tên ở mục Manchester United, gồm nhóm 25 người hoặc U21. Đây là nguồn có ngày sớm nhất đã kiểm tra xác nhận họ thuộc CLB, nên mọi dòng dùng `start_date=2026-09-03`.
- [Premier League: số áo 2026/27, công bố 22/09/2026](https://www.premierleague.com/en/news/4725682/all-20-premier-league-clubs-squad-numbers-for-202627-season): đối chiếu lại cả 25 người vẫn nằm dưới Manchester United. Tên CSV là tên ngắn: Bruno Fernandes là Bruno Miguel Borges Fernandes, Matheus Cunha là Matheus Santos Carneiro da Cunha, Andrey Santos là Andrey Nascimento dos Santos trong danh sách đăng ký.
- [Fantasy Premier League bootstrap-static](https://fantasy.premierleague.com/api/bootstrap-static/), kiểm tra **27/09/2026**: cả 25 người có `team=16` (Man Utd); vị trí lấy từ `element_type` của FPL (`1=GK`, `2=DEF`, `3=MID`, `4=FWD`). CSV có **3 GK, 8 DEF, 12 MID, 2 FWD**. Patrick Dorgu, Marcus Rashford, Bryan Mbeumo và Matheus Cunha đều là `MID` theo FPL. Tình trạng chấn thương/nghi vấn thi đấu không được hiểu là đã rời CLB; vì thế Tom Heaton, Amad Diallo và Manuel Ugarte vẫn được chọn khi hai nguồn PL cùng xác nhận CLB.

Các `player_id=2000006001..2000006025` là ID nội bộ mới, ổn định khi chuyển CLB; không lấy từ FPL và không trùng ID đã cấp hoặc giữ chỗ trong các batch trước. `end_date` trống vì chưa có căn cứ ngày rời CLB. **Không dùng batch để tái hiện roster trước 03/09/2026 hoặc GW1**; cần nguồn lịch sử theo từng người cho các ngày đó. Không nhập thống kê hay điểm Fantasy.

## Tạm hoãn

| Người | Quyết định ngày 27/09/2026 | Nguồn |
| --- | --- | --- |
| Shea Lacey; Jack Fletcher; Tyler Fletcher; Harry Amass | Tạm hoãn do giới hạn 25 và ưu tiên nhóm đội một; PL có số áo, FPL có vị trí Fantasy. Không cấp ID. | [PL số áo](https://www.premierleague.com/en/news/4725682/all-20-premier-league-clubs-squad-numbers-for-202627-season), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Dermot Mee | Tạm hoãn: có trong nhóm 25 người PL và danh sách số áo, nhưng không có dòng FPL hiện tại để kiểm tra vị trí Fantasy. Không cấp ID. | [PL squad lists](https://www.premierleague.com/en/news/4706139/see-all-the-202627-premier-league-squad-lists), [PL số áo](https://www.premierleague.com/en/news/4725682/all-20-premier-league-clubs-squad-numbers-for-202627-season), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Chido Obi; Altay Bayindir | **Chưa xác minh cho batch hiện tại**: FPL còn dòng nhưng đánh dấu `u`; không có tên trong danh sách số áo ngày 22/09. Không suy ra đã rời CLB chỉ từ sự vắng mặt này. | [PL số áo](https://www.premierleague.com/en/news/4725682/all-20-premier-league-clubs-squad-numbers-for-202627-season), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |

# Chelsea 2026/27: roster khởi đầu

**Kiểm tra ngày 27/09/2026.** Đây là 25 lựa chọn ban đầu, không phải toàn bộ roster hay đội hình một trận. `GET https://epl-personal-production.up.railway.app/api/clubs?season=2026` trả `Chelsea FC` với `club_id=1000000061`.

## Căn cứ cho CSV

- [Premier League: squad lists 2026/27, công bố 03/09/2026](https://www.premierleague.com/en/news/4706139/see-all-the-202627-premier-league-squad-lists): cả 25 người trong CSV có tên ở mục Chelsea, gồm nhóm 25 người hoặc U21. Vì đây là nguồn có ngày sớm nhất đã kiểm tra xác nhận họ thuộc Chelsea, mọi dòng dùng `start_date=2026-09-03`.
- [Premier League: số áo 2026/27, công bố 22/09/2026](https://www.premierleague.com/en/news/4725682/all-20-premier-league-clubs-squad-numbers-for-202627-season): đối chiếu lại cả 25 người vẫn nằm dưới Chelsea. Dùng tên ngắn dễ đọc trong CSV; ví dụ Emiliano Martinez là Damián Emiliano Martínez Romero, Jamie Bynoe-Gittens là Jamie Gittens trong bài số áo, và Estevao là Estêvão Willian trong FPL.
- [Fantasy Premier League bootstrap-static](https://fantasy.premierleague.com/api/bootstrap-static/), kiểm tra **27/09/2026**: cả 25 người có `team=6` (Chelsea); vị trí lấy từ `element_type` của FPL (`1=GK`, `2=DEF`, `3=MID`, `4=FWD`). CSV có **2 GK, 9 DEF, 11 MID, 3 FWD**. Ví dụ Valentin Barco và Geovany Quenda là `MID` theo FPL, dù vai trò ngoài sân có thể được mô tả khác. Tình trạng chấn thương/nghi vấn thi đấu không được hiểu là đã rời CLB.

Các `player_id=2000005001..2000005025` là ID nội bộ mới, ổn định khi chuyển CLB; không lấy từ FPL và không trùng các ID đã cấp hoặc giữ chỗ trong bốn batch trước. `end_date` trống vì chưa có căn cứ ngày rời CLB. **Không dùng batch để tái hiện roster trước 03/09/2026 hoặc GW1**; cần nguồn lịch sử theo từng người cho các ngày đó. Không nhập thống kê hay điểm Fantasy.

## Tạm hoãn

| Người | Quyết định ngày 27/09/2026 | Nguồn |
| --- | --- | --- |
| Aaron Anselmino | Tạm hoãn do giới hạn 25; FPL xác nhận DEF nhưng ghi 0 phút. Không cấp ID. | [PL số áo](https://www.premierleague.com/en/news/4725682/all-20-premier-league-clubs-squad-numbers-for-202627-season), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Shumaira Mheuka | Tạm hoãn do giới hạn 25; có trong U21 và FPL xếp FWD, 0 phút. Không cấp ID. | [PL squad lists](https://www.premierleague.com/en/news/4706139/see-all-the-202627-premier-league-squad-lists), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Teddy Sharman-Lowe; Gabriel Slonina; Olutayo Subuloye | Tạm hoãn: có tên trong danh sách PL nhưng không có dòng FPL hiện tại để xác nhận vị trí Fantasy. Không cấp ID. | [PL số áo](https://www.premierleague.com/en/news/4725682/all-20-premier-league-clubs-squad-numbers-for-202627-season), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Robert Sanchez; Filip Jorgensen; Trevoh Chalobah; Benoit Badiashile; Marc Guiu | **Chưa xác minh cho batch hiện tại**: FPL còn dòng nhưng đánh dấu `u`; không có tên trong danh sách số áo Chelsea ngày 22/09. Không suy ra đã rời CLB chỉ từ sự vắng mặt này. | [PL số áo](https://www.premierleague.com/en/news/4725682/all-20-premier-league-clubs-squad-numbers-for-202627-season), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |

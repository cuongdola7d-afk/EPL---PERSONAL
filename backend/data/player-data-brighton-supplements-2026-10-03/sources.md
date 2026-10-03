# Brighton: bổ sung do người dùng, 2026/27

Nguồn vị trí và OVR mới: tin nhắn người dùng ngày 2026-10-03:
“Chema Andrés : cm, Nehemiah Oriola : lw/lm, Younes Ibrahim ; cam overall 60”.
Đây là dữ liệu người dùng xác nhận, không ghi là OVR/vị trí đã xác minh trên EA.
Vị trí đầu tiên trong danh sách được dùng làm primary; giữ nguyên ID/CLB.

| player_id | Tên PremierHub | primary | eligible | OVR |
| --- | --- | --- | --- | ---: |
| 2000030174 | José María Andrés Baixauli / Chema Andrés | CM | CM | 69, người dùng 02/10 đã có |
| 2000030170 | Nehemiah Oriola | LW | LW, LM | 60, người dùng 02/10 đã có |
| 2000030176 | Younes Ibrahim | CAM | CAM | 60, người dùng 03/10 bổ sung |

club_id cả ba: 1000000397 (Brighton); season_year=2026. Năm trường hồ sơ và OVR
Chema/Oriola giữ từ profiles.csv đã chuẩn bị trong batch Brighton–Bournemouth.
Younes giữ chiều cao 182 cm đã thu thập ở batch đó; chỉ thêm OVR60 trong lượt này.
URL SofaScore kế thừa cho năm trường hồ sơ đã có:

- Chema Andrés: https://www.sofascore.com/football/player/chema-andres/1464641 .
- Nehemiah Oriola: https://www.sofascore.com/football/player/nehemiah-oriola/1899611 .
- Younes Ibrahim: https://www.sofascore.com/football/player/younes-ibrahim/1899640 .

Note hồ sơ trước và nguồn chiều cao Younes: file
../player-data-brighton-bournemouth-2026-10-03/sources.md và batch Brighton 02/10.

Giữ nguyên toàn bộ file của batch Brighton–Bournemouth, gồm snapshot ba người MISSING.
Bổ sung mới nằm riêng tại đây; roster-status.csv tại đây là trạng thái sau bổ sung
của đúng ba ID. Không rerun reader/test/importer đối với batch cũ.

profiles.csv đúng định dạng reader cho ba hồ sơ đầy đủ; positions.csv có ba dòng
vị trí USER_PROVIDED. expected_* trống theo dữ liệu vị trí chưa nhập trong batch cũ.
profile-updates.csv chỉ kê đúng ô fc27_overall60 của Younes, không phải đầu vào
PlayerProfileCsvReader. Importer hồ sơ insert-only sẽ báo xung đột nếu chạy ngay
khi production vẫn có OVR NULL. Khi được phép ghi sau này, cần áp dụng có điều kiện
height_cm182 từ profile-updates.csv của batch cũ và OVR60 từ file bổ sung này trước
khi chạy hồ sơ cuối. Không ghi đè giá trị khác; lượt này chưa truy cập/ghi MySQL.

Không còn trường dữ liệu cần bổ sung trong ba hồ sơ local. Nguồn EA cho các giá trị
người dùng chưa xác minh; điều này không thay giá trị đã được người dùng chốt.

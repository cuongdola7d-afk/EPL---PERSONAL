# Các trường hợp cần xác nhận — 30/09/2026

Người dùng đã chọn **ghi danh sách chờ và giữ lịch sử** cho 28 trường hợp lệch vị trí. Không nhập các dòng này, không thay vị trí mùa đã lưu.

| CLB | Cầu thủ | player_id hiện có | Vị trí database → FPL |
| --- | --- | --- | --- |
| Aston Villa | Alysson | 2000030062 | FORWARD / FPL MID |
| Aston Villa | Alejandro Garnacho | 2000030060 | FORWARD / FPL MID |
| Aston Villa | Triston Rowe | 2000030125 | MIDFIELDER / FPL DEF |
| Bournemouth | Juanlu Sánchez | 2000030114 | MIDFIELDER / FPL DEF |
| Brentford | Keane Lewis-Potter | 2000030002 | DEFENDER / FPL MID |
| Brighton | Georginio Rutter | 2000030044 | MIDFIELDER / FPL FWD |
| Brighton | Zadok Yohanna | 2000030053 | FORWARD / FPL MID |
| Crystal Palace | Evann Guessand | 2000020107 | FORWARD / FPL MID |
| Crystal Palace | Tyrick Mitchell | 2000020103 | MIDFIELDER / FPL DEF |
| Crystal Palace | Óscar Mingueza | 2000020116 | MIDFIELDER / FPL DEF |
| Crystal Palace | Yéremy Pino | 2000020105 | FORWARD / FPL MID |
| Crystal Palace | Anan Khalaili | 2000016002 | MIDFIELDER / FPL DEF |
| Everton | Brennan Johnson | 2000020093 | FORWARD / FPL MID |
| Everton | Tyrique George | 2000020087 | FORWARD / FPL MID |
| Ipswich Town | Daizen Maeda | 2000020024 | FORWARD / FPL MID |
| Ipswich Town | Abdoul Ouattara | 2000020028 | MIDFIELDER / FPL DEF |
| Leeds | Jayden Bogle | 2000020065 | MIDFIELDER / FPL DEF |
| Leeds | Noah Okafor | 2000020071 | FORWARD / FPL MID |
| Leeds | Brenden Aaronson | 2000020069 | FORWARD / FPL MID |
| Leeds | Ethan Ampadu | 2000020068 | DEFENDER / FPL MID |
| Newcastle | Anthony Elanga | 2000030070 | FORWARD / FPL MID |
| Newcastle | Jacob Murphy | 2000030076 | FORWARD / FPL MID |
| Nott'm Forest | Ola Aina | 2000020060 | MIDFIELDER / FPL DEF |
| Spurs | Mathys Tel | 2000030101 | FORWARD / FPL MID |
| Spurs | Luca Williams-Barnett | 2000030105 | FORWARD / FPL MID |
| Sunderland | Trai Hume | 2000020035 | MIDFIELDER / FPL DEF |
| Sunderland | Luke O'Nien | 2000020033 | MIDFIELDER / FPL DEF |
| Sunderland | Romaine Mundle | 2000020051 | FORWARD / FPL MID |

Vị trí hiện nằm trong `player_season_stats`, dùng chung cho cặp cầu thủ–CLB trong cả mùa. Muốn đổi riêng vị trí hiện tại mà giữ nguyên GW1 cần thiết kế vị trí theo ngày và kiểm tra API lịch sử trước khi triển khai. Không dùng ID mới để né xung đột.

## Trường hợp nguồn chưa đủ rõ

| CLB | Cầu thủ | Lý do chờ |
| --- | --- | --- |
| Coventry City | Oliver Dovin | FPL `171`, GK, status=u, vẫn ghi cho Leyton Orient mượn. Có thông báo trở về điều trị chấn thương; chưa đủ căn cứ xác nhận đã chấm dứt cho mượn/được đăng ký thi đấu hiện tại cho Coventry. Chưa cấp ID mới. |
| Tottenham | Richarlison | FPL `527`, FWD, status=u, ghi “not included in squad”; không tự suy ra đã chuyển CLB. ID lịch sử `2000030019` giữ nguyên; chưa thêm khoảng hiện tại. |
| Manchester United | Dermot Mee | Danh sách PL ghi tên nhưng không có dòng FPL để xác minh vị trí Fantasy hiện tại. Chưa cấp ID mới. |

Nguồn: [FPL bootstrap-static](https://fantasy.premierleague.com/api/bootstrap-static/), [PL squad lists 03/09/2026](https://www.premierleague.com/en/news/4706139/see-all-the-202627-premier-league-squad-lists), [Coventry thông báo Dovin 25/09/2026](https://www.ccfc.co.uk/news/2026/september/25/oliver-dovin-returns-from-loan-due-to-injury/).

## ID trùng đã tồn tại, không hợp nhất trong đợt này

| Người | ID dùng cho roster hiện tại | ID lịch sử giữ nguyên |
| --- | --- | --- |
| Ethan Pinnock | 2000002001 (Coventry, khoảng mở từ 22/09) | 2000030098 (Brentford, 22–23/08) |
| Iliman Ndiaye | 2000004005 (Manchester City, khoảng mở từ 03/09) | 2000015003 (Everton, 22–23/08) |

Ngoài ra database đã có các cặp ID provider 2024 và ID thủ công 2026 cho cùng tên. Các ID thay thế tìm được nằm ở `existing_other_ids` trong bốn file identities; đây là ứng viên đối chiếu, không phải chỉ thị hợp nhất. Đợt này ưu tiên ID thủ công 2026 đã dùng, hoặc tái sử dụng ID provider khi chưa có ID 2026. Không đổi tên/ID/dòng thống kê cũ. Không có ID mới nào được cấp cho người đã xác định tồn tại.

# Batch Arsenal – Coventry 2026/27

Kiểm tra ngày **27/09/2026**. Đây là danh sách khởi đầu, tối đa 25 người mỗi đội, **không phải roster đầy đủ**. Trận GW1 đầu tiên của API local là Arsenal FC (`club_id=1000000057`) – Coventry City FC (`club_id=1000001076`); hai ID đối chiếu qua `GET /api/clubs?season=2026`. CSV chưa nhập vào production.

## Căn cứ cho các dòng giữ lại

- [Premier League: danh sách số áo công bố 22/09/2026](https://www.premierleague.com/en/news/4725682/all-20-premier-league-clubs-squad-numbers-for-202627-season), mục Arsenal và Coventry City. Đối chiếu CLB và vị trí Fantasy bằng [FPL bootstrap-static](https://fantasy.premierleague.com/api/bootstrap-static/) ngày 27/09/2026 (`team=1` Arsenal, `team=7` Coventry; `element_type`: 1 GK, 2 DEF, 3 MID, 4 FWD). Danh sách số áo có thể chậm cập nhật chuyển nhượng, nên các trường hợp nghi vấn được kiểm tra riêng dưới đây.
- `start_date=2026-09-22` là ngày có danh sách Premier League xác nhận những người **còn giữ trong CSV** thuộc CLB, không phải ngày ký hợp đồng hoặc ngày đầu tiên thực tế. `end_date` để trống vì chưa có căn cứ về ngày rời đội của họ. **Batch này không dùng để tái hiện roster GW1 hay bất kỳ vòng nào trước 22/09/2026**; không lùi `start_date` để khớp lịch trận. Cần nguồn lịch sử cho từng người nếu muốn tái hiện các vòng đó.
- `player_id` là ID nội bộ, không suy từ tên hay lấy ID FPL. ID của người tạm hoãn ở dưới được **giữ chỗ, không tái sử dụng**; khi có căn cứ đưa lại, dùng chính ID đó.

## Quyết định cho các trường hợp cần rà lại

Ngày kiểm tra của **từng dòng là 27/09/2026**. “Tạm hoãn” chỉ có nghĩa không nhập trong batch giới hạn này, không tự suy ra cầu thủ đã rời CLB.

| CLB | Cầu thủ | Quyết định, lý do | Nguồn |
| --- | --- | --- | --- |
| Arsenal | Gabriel Martinelli | Tạm hoãn; Al-Hilal xác nhận đã ký từ Arsenal ngày 03/09. | [Al-Hilal](https://alhilal.com/en/news/gabriel-martinelli-joins-al-hilal-for-four-years) |
| Arsenal | Gabriel Jesus | Tạm hoãn; Premier League xác nhận chuyển đến Barcelona ngày 02/09. | [Premier League](https://www.premierleague.com/en/news/4705808) |
| Arsenal | Reiss Nelson | Tạm hoãn; Feyenoord xác nhận hợp đồng mới ngày 02/09. | [Feyenoord](https://www.feyenoord.com/en/news/reiss-nelson-returns-to-feyenoord-0209261) |
| Arsenal | Fabio Vieira | Tạm hoãn; Hamburg xác nhận mua đứt từ Arsenal ngày 01/09. | [Hamburger SV](https://www.hsv.de/en/news/welcome-back-fabio-vieira-returns-to-hsv) |
| Arsenal | Ethan Nwaneri (`2000001016`) | Tạm hoãn, **bỏ khỏi CSV**; Bundesliga xác nhận cho Dortmund mượn cả mùa từ 01/09. Danh sách số áo PL vẫn có tên anh, nhưng FPL ngày kiểm tra ghi `status=u`, đang cho Dortmund mượn. Giữ ID để theo dõi lịch sử. | [Bundesliga](https://www.bundesliga.com/en/bundesliga/news/arsenal-ethan-nwaneri-borussia-dortmund-loan-deal-injury-ole-book-38938), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Coventry | Oliver Dovin | Tạm hoãn; CLB xác nhận trở về từ diện cho mượn để điều trị chấn thương vai ngày 25/09. Anh vẫn thuộc Coventry; batch đã có ba GK. FPL còn ghi thông tin cho mượn cũ nên ưu tiên thông báo mới của CLB. | [Coventry City](https://www.ccfc.co.uk/news/2026/september/25/oliver-dovin-returns-from-loan-due-to-injury/), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Coventry | Liam Kitching | Tạm hoãn; Coventry xác nhận đang được cho Sheffield United mượn. | [Coventry City, 24/09](https://www.ccfc.co.uk/news/2026/september/24/eight-sky-blues-stars-head-out-on-international-duty-/) |
| Coventry | Miguel Brau | Tạm hoãn; Académico de Viseu xác nhận mượn từ Coventry cho mùa 2026/27. | [Académico de Viseu, 28/08](https://academicodeviseu.pt/noticia.php?n=1376) |
| Coventry | Kai Andrews | Tạm hoãn; Coventry xác nhận đang được cho Oxford United mượn. | [Coventry City, 24/09](https://www.ccfc.co.uk/news/2026/september/24/eight-sky-blues-stars-head-out-on-international-duty-/) |
| Coventry | Raphael Borges Rodrigues | **Chưa xác minh** bằng thông báo chuyển nhượng của CLB; FPL ghi cho Burton mượn và biên bản Burton ngày 05/09 ghi anh thi đấu. Không đưa vào batch Coventry khi chưa chắc thời hạn/tình trạng hiện tại. | [FPL](https://fantasy.premierleague.com/api/bootstrap-static/), [Burton Albion, 05/09](https://www.burtonalbionfc.co.uk/matches/first-team/2026/g2647853) |
| Coventry | George Shepherd | Tạm hoãn để ưu tiên người có khả năng ra sân đội một; PL đăng ký anh ở nhóm U21, FPL ngày kiểm tra ghi 0 phút. Đây **không** phải bằng chứng rời Coventry. | [Premier League, 03/09](https://www.premierleague.com/en/news/4706139/see-all-the-202627-premier-league-squad-lists), [FPL](https://fantasy.premierleague.com/api/bootstrap-static/) |
| Coventry | Norman Bassette | Tạm hoãn; Westerlo xác nhận mượn từ Coventry, kèm quyền chọn mua. | [KVC Westerlo, 22/07](https://kvcwesterlo.be/2026/07/22/welkom-norman-bassette/) |
| Coventry | Jahnoah Markelo | Tạm hoãn; Shabab Al Ahli xác nhận ký và đăng ký anh cho mùa 2026/27. | [Shabab Al Ahli, 08/08](https://www.shababalahli.ae/?go=news&more=147) |

## Bốn dòng Coventry tạm hoãn để đạt giới hạn 25

Cả bốn vẫn nằm trong [danh sách số áo PL ngày 22/09](https://www.premierleague.com/en/news/4725682/all-20-premier-league-clubs-squad-numbers-for-202627-season). [FPL bootstrap-static](https://fantasy.premierleague.com/api/bootstrap-static/) kiểm tra ngày **27/09/2026** ghi mỗi người **0 phút** và trạng thái chấn thương (`status=i`). Đây là quyết định ưu tiên tạm thời khi giới hạn 25 người, **không phải kết luận họ đã rời Coventry**.

| Cầu thủ | ID giữ chỗ | Quyết định, lý do | Nguồn và ngày kiểm tra |
| --- | --- | --- | --- |
| Haji Wright | `2000002010` | Tạm hoãn; FPL ghi chấn thương đùi, chưa rõ ngày trở lại. | [FPL](https://fantasy.premierleague.com/api/bootstrap-static/), 27/09/2026 |
| Kaine Kesler-Hayden | `2000002017` | Tạm hoãn; FPL ghi chấn thương gân kheo, chưa rõ ngày trở lại. | [FPL](https://fantasy.premierleague.com/api/bootstrap-static/), 27/09/2026 |
| Luke Woolfenden | `2000002023` | Tạm hoãn; FPL ghi chấn thương đầu gối, chưa rõ ngày trở lại. | [FPL](https://fantasy.premierleague.com/api/bootstrap-static/), 27/09/2026 |
| Josh Eccles | `2000002025` | Tạm hoãn; FPL ghi chấn thương chưa xác định, dự kiến trở lại 12/10. | [FPL](https://fantasy.premierleague.com/api/bootstrap-static/), 27/09/2026 |

CSV sau rà soát: Arsenal **24**, Coventry **25**. Các vị trí được giữ đủ lựa chọn GK/DEF/MID/FWD; số lượng này có thể thay đổi khi có thêm nguồn đáng tin cậy. Không tái sử dụng năm ID đã giữ chỗ.

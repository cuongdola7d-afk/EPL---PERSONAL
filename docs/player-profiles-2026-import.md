# Hồ sơ cầu thủ 2026/27: schema và importer

## Định dạng và quy tắc

`player_season_profiles` lưu hồ sơ theo khóa `(league_id, season_year, player_id, club_id)` và tham chiếu `player_season_stats`. Importer chỉ nhận Premier League 2026/27 (`league_id=39`, `season_year=2026`). Nó không tạo hoặc sửa cầu thủ, membership, thống kê trận, hay dữ liệu 2024/25.

CSV gồm `player_id,club_id,nationality,birth_date,height_cm,preferred_foot,shirt_number,fc27_overall`. Ô trống được lưu là SQL `NULL`, không phải 0. Ngày có dạng `YYYY-MM-DD`; chân thuận là `LEFT`, `RIGHT` hoặc `BOTH`; chiều cao là 100–250 cm, số áo và OVR là 1–99 khi có giá trị. `fc27_overall` thường chứa OVR cơ bản đã xác minh từ EA SPORTS FC 27. Theo chỉ định của người dùng, riêng Max Dowman được ghi **72** vào cột này như ước tính PremierHub, dù chưa có rating EA được xác minh; nguồn của ngoại lệ được ghi trong `sources.md` của Arsenal.

Mỗi lần nhập phải cung cấp ngày chốt membership. Importer kiểm tra cầu thủ có đúng một membership hiệu lực tại ngày đó, thuộc `club_id` trong CSV và có dòng `player_season_stats` cùng mùa. Nó đọc, kiểm tra toàn bộ file trước khi chèn trong một transaction; ID trùng hoặc hồ sơ đã lưu khác bất kỳ trường nào làm lệnh thất bại và không ghi đè. Nhập lại cùng nội dung thêm 0.

Người có `fc27_overall IS NULL` vẫn có hồ sơ cầu thủ nhưng **chưa đủ điều kiện chọn Fantasy**. Lượt này chỉ lưu dữ liệu; giao diện và logic chọn Fantasy chưa được thay đổi. Carlos Baleba giữ membership Manchester United (`club_id=1000000066`); nhãn Brighton trên EA không làm đổi CLB của anh.

## Dùng lại cho CLB tiếp theo

1. Chốt roster từ PremierHub theo `season=2026&asOf=<ngày>`, tạo CSV tám cột cùng file ghi chú URL nguồn như hai batch hiện tại.
2. Thử importer trên H2 cô lập và chạy lại, kiểm tra OVR trống vẫn là `NULL`, ID/membership và giá trị từng trường.
3. Trước khi ghi production, xác nhận đúng Railway MySQL của backend, đối chiếu các cặp `player_id,club_id` đang hiệu lực, rồi tạo bản sao lưu SQL mới ngoài Git.
4. Cung cấp các biến `PREMIERHUB_JDBC_URL`, `PREMIERHUB_DB_USER`, `PREMIERHUB_DB_PASSWORD` qua môi trường và chạy từ thư mục `backend/`:

```powershell
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar `
  --spring.profiles.active=prod --spring.main.web-application-type=none `
  --premierhub.player-profiles.enabled=true `
  --premierhub.player-profiles.file=data/<club-profiles-date>/players.csv `
  --premierhub.player-profiles.as-of=2026-10-02
```

5. Chạy lại cùng lệnh để xác nhận `inserted=0`, rồi đọc MySQL so sánh từng ô với CSV và kiểm tra các bảng roster/trận không đổi. `schema.sql` tạo bảng mới bằng `CREATE TABLE IF NOT EXISTS` khi backend khởi động; khi production chưa có bảng, sao lưu phải hoàn tất **trước** lần khởi động importer đầu tiên.

## Hai batch ngày 02/10/2026

- CSV: `backend/data/manchester-united-profiles-2026-10-02/players.csv` (30 người) và `backend/data/arsenal-profiles-2026-10-02/players.csv` (24 người). Không có batch nào khác được nhập.
- H2 cô lập với CSV hiện tại: thêm 30 + 24; chạy lại cả hai thêm 0. Kết quả 54 hồ sơ, 53 OVR có số (gồm Dowman 72), một OVR `NULL`; test còn xác nhận từ chối ID trùng, OVR 0, membership hết hiệu lực và xung đột.
- Backend `mvn package` sau thay đổi: 224 test qua, 0 lỗi; JAR được build thành công. Trên Windows của workspace này, thư mục tạm Java cho JUnit được đặt trong `backend/target/test-tmp` để các test dùng `@TempDir` có quyền ghi.
- Production trước ghi: xác nhận Railway MySQL host `altaria.proxy.rlwy.net`, database `railway`; 54 cặp ID/CLB trong hai CSV khớp chính xác membership đang hiệu lực ngày 02/10/2026. Trước đó bảng hồ sơ chưa tồn tại, `players` có 984 dòng và `manual_fixture_player_stats` có 2.000 dòng.
- Bản sao lưu SQL mới, Git ignored: `backend/local-backups/player-profiles-2026-10-02/premierhub-before-profiles-20261002-131254.sql` (756.207 byte; SHA-256 `1DBD7DA78FCD9C89E34E27B2EE334D07407CF8DBF72C9E6D9F83DA78D6B61783`).
- Production ban đầu nhập MU thêm 30, Arsenal thêm 24; chạy lại mỗi CSV thêm 0, kể cả khi chạy lại bằng JAR build cuối. Sau đó người dùng yêu cầu ghi 72 trực tiếp cho Max Dowman vào `fc27_overall` trong Arsenal CSV và MySQL. Vì thế 54 hồ sơ có 53 OVR số (52 từ EA, một ước tính PremierHub), và chỉ Bendito Mantato (`2000030235`) còn `NULL`. Carlos Baleba (`2000006019`) có `club_id=1000000066`. `players` vẫn 984 dòng, `manual_fixture_player_stats` vẫn 2.000 dòng và mùa 2024/25 vẫn hiện diện.
- Trước khi cập nhật Dowman, đã sao lưu production vào `backend/local-backups/player-profiles-2026-10-02/premierhub-before-dowman72-20261002-134022.sql` (761.735 byte; SHA-256 `A298DA08EAFEDF5DC7FA91F3DDDFA111A10538533E58D1407ADBCAA2CC2AF1AB`). Chỉ cập nhật một dòng `(39,2026,2000001025,1000000057)` từ `NULL` thành 72; chạy lại importer Arsenal trên production thêm 0. Không tạo bảng ước tính riêng.

## Liverpool và Manchester City ngày 02/10/2026

- `backend/data/liverpool-profiles-2026-10-02/players.csv`: 31 ID hiện hành, 29 OVR EA được xác minh, hai OVR `NULL` (Jayden Danns, Wellity Lucky).
- `backend/data/manchester-city-profiles-2026-10-02/players.csv`: 26 ID hiện hành, 22 OVR EA được xác minh, bốn OVR `NULL` (Allan Andrade Elias, Floyd Samba, Kaden Braithwaite, Ryan McAidoo).
- Cả 57 ID/CLB khớp membership production ngày 02/10/2026; mỗi người đúng một dòng và đủ năm trường SofaScore. File `sources.md` của từng CLB ghi URL hồ sơ cùng trường hợp EA còn nhãn CLB cũ.
- H2 cô lập nhập 31 + 26 và nhập lại thêm 0; backend `mvn package` qua 225 test, 0 lỗi.
- Trước khi nhập production đã xác nhận đúng Railway MySQL `railway`, kiểm tra 57/57 cặp ID/CLB có membership hiện hành và dòng mùa hợp lệ; bảng hồ sơ chưa có dòng Liverpool/Man City. Bản sao lưu SQL Git ignored: `backend/local-backups/player-profiles-2026-10-02/premierhub-before-liverpool-city-20261002-135010.sql` (762.922 byte; SHA-256 `C9446F3ACE3226C574F6F7724A32FFF9775298FB4C934114D5B0B0845D190392`).
- Production nhập Liverpool 31 và Manchester City 26; chạy lại mỗi CSV thêm 0. Đọc lại MySQL khớp từng ô của 57 dòng CSV. Tổng bốn CLB đã nhập: 111 hồ sơ, 104 OVR số, 7 OVR `NULL`; `players` vẫn 984, `manual_fixture_player_stats` vẫn 2.000 và mùa 2024/25 vẫn có trong bảng `seasons`. Chưa commit, push hoặc deploy.

## Chelsea và Tottenham ngày 02/10/2026

- `backend/data/chelsea-profiles-2026-10-02/players.csv`: 27 ID hiện hành, đủ năm trường SofaScore, 26 OVR EA FC 27; Mahdi Nicoll-Jazuli còn OVR `NULL`.
- `backend/data/tottenham-profiles-2026-10-02/players.csv`: 29 ID hiện hành, đủ năm trường SofaScore, 28 OVR EA FC 27; James Rowswell còn OVR `NULL`.
- Cả 56 ID/CLB khớp membership production ngày 02/10/2026, không trùng ID; file `sources.md` từng CLB ghi URL hồ sơ và các khác biệt tên/nhãn CLB. H2 cô lập nhập 27 + 29 rồi nhập lại thêm 0. Backend `mvn package` qua 226 test, 0 lỗi.
- Trước khi nhập production đã xác nhận đúng Railway MySQL `railway`, 56/56 cặp ID/CLB có membership và dòng mùa hợp lệ; bảng hồ sơ chưa có Chelsea/Tottenham. Bản sao lưu SQL Git ignored: `backend/local-backups/player-profiles-2026-10-02/premierhub-before-chelsea-tottenham-20261002-141043.sql` (767.072 byte; SHA-256 `F2773EC61259C5163D11538A6FC239BE7899D32CFD98318DD2F2F583973B3CE7`).
- Production nhập Chelsea 27 và Tottenham 29; chạy lại mỗi CSV thêm 0. Đọc lại MySQL khớp từng ô của 56 dòng CSV. Tổng sáu CLB đã nhập: 167 hồ sơ, 158 OVR số, 9 OVR `NULL`; `players` vẫn 984, `manual_fixture_player_stats` vẫn 2.000 và mùa 2024/25 vẫn có trong `seasons`. Chưa commit, push hoặc deploy.

## Brighton và Brentford: batch local ngày 02/10/2026

- `backend/data/brighton-profiles-2026-10-02/players.csv`: 30 ID hiện hành, 27 OVR EA FC 27. José María Andrés Baixauli (Chema Andrés), Nehemiah Oriola và Younes Ibrahim có OVR `NULL` vì chưa xác minh được hồ sơ cơ bản FC 27 chính thức. SofaScore không ghi chiều cao Younes Ibrahim nên `height_cm` cũng `NULL`.
- `backend/data/brentford-profiles-2026-10-02/players.csv`: 28 ID hiện hành, đủ năm trường SofaScore và 28 OVR EA FC 27.
- Cả 58 cặp ID/CLB khớp chính xác membership Railway MySQL hiệu lực ngày 02/10/2026 (truy vấn chỉ đọc), không trùng ID. Production chưa có hồ sơ Brighton/Brentford; không thay đổi roster hay membership theo nhãn CLB trên EA hoặc SofaScore. `sources.md` mỗi CLB ghi URL cá nhân và khác biệt tên/CLB.
- H2 cô lập nhập 30 + 28 và chạy lại thêm 0; xác nhận 55 OVR có số, 3 OVR `NULL`, một chiều cao `NULL`. Backend `mvn package` qua 227 test, 0 lỗi. Tại thời điểm chuẩn bị hai CSV này, chưa nhập MySQL production.

## Nhập Brighton, Brentford, Leeds và Everton ngày 02/10/2026

- Leeds: 22 ID hiện hành, 22 OVR EA FC 27, riêng Alfie Cresswell thiếu chiều cao trên SofaScore nên `height_cm=NULL`. Everton: 20 ID hiện hành, 19 OVR EA FC 27, Braiden Graham thiếu OVR chính thức nên `fc27_overall=NULL`.
- File `backend/data/player-profiles-2026-10-02-missing-fields.txt` ghi toàn bộ 14 cầu thủ/15 ô còn thiếu trong 10 batch hồ sơ đã thu thập. Ô trống giữ SQL `NULL`, không gán 0. Hồ sơ OVR `NULL` chưa đủ điều kiện chọn Fantasy; phần giao diện/logic Fantasy chưa được sửa trong batch này.
- H2 cô lập nhập Leeds 22 + Everton 20, chạy lại thêm 0; Brighton 30 + Brentford 28 cũng chạy lại thêm 0. Backend `mvn package` qua 228 test, 0 lỗi. Trước khi ghi production, đối chiếu chính xác 100/100 cặp ID/CLB với membership hiệu lực ngày 02/10/2026, xác nhận cả 100 có dòng `player_season_stats` mùa 2026; bảng profile chưa có dòng nào của bốn CLB. Đích ghi được kiểm tra là Railway MySQL 9.4.0, database `railway`.
- Bản sao lưu SQL mới ngoài Git: `backend/local-backups/player-profiles-2026-10-02/premierhub-before-brighton-brentford-leeds-everton-20261002-145342.sql` (771.009 byte; SHA-256 `64241374D91B7BC5C1BB46CC33C5D76F4B418A1A9FD4264550708CAF10D280AE`).
- Production nhập Brighton 30, Brentford 28, Leeds 22, Everton 20; mỗi CSV chạy lại thêm 0. Đọc lại MySQL khớp từng ô của cả 100 dòng CSV: bốn CLB có 96 OVR số, 4 OVR `NULL`, 2 chiều cao `NULL`. Tổng 10 CLB đã nhập: 267 hồ sơ, 254 OVR số, 13 OVR `NULL`, 2 chiều cao `NULL`; `players` vẫn 984, `manual_fixture_player_stats` vẫn 2.000 và mùa 2024/25 vẫn có trong `seasons`. Chưa commit, push hoặc deploy.

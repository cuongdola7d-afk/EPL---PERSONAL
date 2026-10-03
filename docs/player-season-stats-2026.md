# Tổng thống kê cầu thủ 2026/27

`player_season_stats` được tạo từ roster nên bốn cột `appearances`, `minutes`, `goals`, `assists` ban đầu là `NULL`. Importer trận ghi vào `manual_fixture_player_stats`. Lệnh dưới đây tái tính bốn cột tổng mùa đến hết một gameweek đã hoàn chỉnh; không đổi `position`, roster, membership, bảng trận hoặc mùa 2024/25.

## Quy tắc

- Cần đúng 10 fixture `FINISHED` và 40 dòng cầu thủ (20 mỗi CLB) ở từng gameweek từ GW1 đến gameweek chọn. Mỗi dòng phải khớp membership ở ngày thi đấu và dòng `player_season_stats` của đúng CLB. Thiếu dữ liệu thì transaction dừng trước khi ghi.
- `appearances` là số dòng `PLAYED`. `DID_NOT_PLAY` không tăng số trận, phút, bàn thắng hay kiến tạo.
- `minutes`, `goals`, `assists` là tổng các dòng `PLAYED`. Nếu một dòng `PLAYED` thiếu chỉ số nào, tổng chỉ số đó là SQL `NULL`; không tự điền 0.
- Nếu cầu thủ không có dòng trận nào trong phạm vi gameweek đã kiểm tra đầy đủ, hoặc chỉ có `DID_NOT_PLAY`, cả bốn tổng là 0. Bảng mùa có khóa `(league_id, season_year, player_id, club_id)`, nên cầu thủ chuyển CLB có tổng riêng theo từng CLB.
- Chạy lại cùng gameweek và cùng dữ liệu phải báo `updated=0`. Khi importer trận hoặc patch hoàn tất một gameweek, nó tự tái tính đến gameweek đầy đủ mới nhất trong cùng transaction.

## Checklist cho mỗi gameweek tiếp theo

1. Hoàn tất CSV của cả 10 trận, kiểm tra mỗi trận có 40 dòng hợp lệ (20 mỗi CLB) và nhập vào `manual_fixture_player_stats`. Không coi việc thu thập hoặc nhập một phần vòng là đã hoàn tất.
2. Sau khi nhập đủ 400 dòng và cả 10 fixture `FINISHED`, tái tính `player_season_stats` từ GW1 đến hết gameweek mới bằng lệnh bên dưới với `through-gameweek` tương ứng. Importer tự làm bước này khi vòng vừa đầy đủ; vẫn kiểm tra kết quả thực tế.
3. Đọc lại MySQL: xác nhận số trận, 400 dòng của vòng mới và các tổng `appearances`, `minutes`, `goals`, `assists` của mùa 2026/27. Ghi tên cầu thủ và trường nào vẫn `NULL` do dòng `PLAYED` thiếu dữ liệu; không biến `NULL` thành 0.
4. Chạy lại cùng lệnh; chỉ chốt vòng khi báo `updated=0`. Giữ nguyên thống kê trận, roster, membership và mùa 2024/25.

## Chạy lệnh

Từ `backend/`, sau khi xác nhận đúng database và tạo SQL backup, cung cấp ba biến môi trường `PREMIERHUB_JDBC_URL`, `PREMIERHUB_DB_USER`, `PREMIERHUB_DB_PASSWORD` rồi chạy:

```powershell
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=prod --premierhub.manual-season-stats.enabled=true --premierhub.manual-season-stats.through-gameweek=5
```

Lệnh in số fixture, dòng trận, dòng mùa, dòng đã đổi và số tổng vẫn thiếu từng trường. Sau khi chạy, đọc lại MySQL để đối chiếu số dòng và chạy lại xác nhận `updated=0`.

## Kết quả production ngày 03/10/2026

- Đã xác nhận Railway MySQL `railway` 9.4.0, 50 fixture `FINISHED`, 2.000 dòng trận. Trước khi ghi, 593/593 dòng mùa 2026/27 có `appearances=NULL`.
- Sao lưu SQL toàn bộ database ngoài Git: `backend/local-backups/season-stats-2026/premierhub-before-season-stats-20261003-001537.sql` (797.558 byte; SHA-256 `2E4F0F892C7AB24928634AF3CEAB7B41F6197AC8D103CD86F3BEBB2749C1CB88`).
- Lần đầu: `fixtures=50`, `matchRows=2000`, `seasonRows=593`, `updated=593`. Lần hai: `updated=0`. MySQL đọc lại: 0 dòng thiếu `appearances`, 1 dòng thiếu `minutes`, 2 dòng thiếu `goals`, 4 dòng thiếu `assists`; tổng số lần ra sân là 1.538.
- Các chỉ số `PLAYED` chưa có trong nguồn trận: Declan Rice (GW1, thiếu phút/bàn thắng/kiến tạo), Kobbie Mainoo (GW1, thiếu bàn thắng/kiến tạo), Jack Clarke và Jack Hinshelwood (GW1, thiếu kiến tạo). Tổng tương ứng giữ SQL `NULL`; rating/fantasy và thống kê trận không bị sửa.
- Mùa 2024/25 vẫn có 63 dòng. Bản sửa mã/API và importer ở local cần phát hành riêng; thao tác MySQL ở trên đã có hiệu lực ngay, không cần deploy để bốn cột trong database thay đổi.

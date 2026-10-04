# HLV và sân nhà hiện tại — 2026/27

`clubs` hiện có `id`, `name`, `city`; chưa có trường HLV/sân. Bảng `club_season_information` có khóa `(league_id, season_year, club_id)` và FK vào `season_clubs`. Bảng tách thông tin hiện tại khỏi roster và thống kê, chỉ chấp nhận mùa 2026. `verified_on` là ngày kiểm tra nguồn, không phải ngày bắt đầu nhiệm kỳ. Không dùng thông tin này để mô tả HLV của trận lịch sử.

## CSV và lệnh nhập một lần

CSV UTF-8:

```csv
club_id,season_year,manager_name,manager_status,stadium_name,verified_on
1000000057,2026,Mikel Arteta,PERMANENT,Emirates Stadium,2026-10-04
```

Ô trống lưu SQL NULL. `manager_status` là `PERMANENT` hoặc `INTERIM`; tên/status phải cùng có dữ liệu hoặc cùng trống. Reader kiểm tra ID trùng, mùa, ngày, tên và mã trạng thái. Importer khóa các CLB của mùa, kiểm tra ID thuộc roster, đối chiếu từng trường đã lưu, rồi mới ghi trong một transaction. Dữ liệu khác (kể cả ngày kiểm tra) báo `club_id`, trường, giá trị hiện tại/mới; không tự cập nhật. Khi cần đổi HLV sau này phải đối chiếu giá trị cũ và chuẩn bị thao tác cập nhật được review riêng.

Chạy từ `backend/`, cấu hình kết nối qua biến môi trường, sau khi xác nhận đúng database và tạo SQL backup ngoài Git:

```powershell
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar '--premierhub.club-information.enabled=true' '--premierhub.club-information.file=data/club-information-2026-10-04/clubs.csv'
```

Lệnh không khởi động web server, thoát sau khi nhập. `schema.sql` tạo bảng mới nếu chưa có; chỉ bật cờ importer trong lệnh thủ công, không đặt ở Railway runtime, cron hoặc pre-deploy. Chạy lại cùng CSV trả `inserted=0`.

## API và giao diện

`GET /api/clubs?season=2026`, tìm kiếm và chi tiết CLB trả thêm:

```json
{
  "id": 1000000057,
  "name": "Arsenal FC",
  "city": "",
  "managerName": "Mikel Arteta",
  "managerStatus": "PERMANENT",
  "stadiumName": "Emirates Stadium",
  "informationVerifiedOn": "2026-10-04"
}
```

CLB chưa nhập trả các trường mới NULL. Mùa 2024 dùng truy vấn cũ và không đọc bảng thông tin này. Ô tên CLB mùa 2026 hiển thị HLV và sân nhà, thêm `(tạm quyền)` cho `INTERIM`; thiếu hiển thị `Chưa cập nhật`.

Batch và nguồn: `backend/data/club-information-2026-10-04/`. SQL production có thể được nhập bằng importer local trước khi phát hành code. Khi API production chưa có các trường mới, phải phát hành backend và frontend để hai dòng xuất hiện trên web; không coi dữ liệu SQL đã nhập là giao diện đã deploy.

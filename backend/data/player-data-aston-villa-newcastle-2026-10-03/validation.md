# Kiểm tra batch Aston Villa–Newcastle 2026/27 sau bổ sung

Ngày cập nhật: 2026-10-03. Chỉ sửa dữ liệu local, không sửa code/schema/UI,
không tạo test Java riêng, không chạy toàn bộ test/build.

## Bản cuối

- 55/55 ID roster giữ nguyên: Aston Villa 27, Newcastle 28. Không thay club_id,
  nhóm vị trí rộng, membership, thống kê trận hoặc mùa 2024/25.
- profiles.csv: 55 ID duy nhất, đủ sáu trường cho mỗi người.
- positions.csv: 55 ID duy nhất, primary thuộc eligible; 48 bộ vị trí EA cũ
  giữ nguyên, bổ sung đúng bảy bộ vị trí người dùng cung cấp.
- roster-status.csv: 55 người, 48 VERIFIED và bảy USER_PROVIDED ở position_status.
  Không còn MISSING hoặc missing_fields; đây là trạng thái batch local.
- sources.md phân biệt nguồn EA/SofaScore với nguồn người dùng ngày 03/10.
  Bốn OVR 60 được lưu trực tiếp vào fc27_overall theo yêu cầu, không phải EA
  chính thức đã xác minh. James Wright 61, Leon Goretzka 78, Miodrag Pivas 62
  giữ nguyên nguồn người dùng 02/10.
- profile-updates.csv ghi đúng tám ô hồ sơ mới, đều từ trống: ba chiều cao,
  chân thuận Michael Mills, bốn OVR 60. Không thay các ô đã có.
- missing-fields.txt nay ghi không còn dữ liệu thiếu, đồng thời giữ danh sách
  bảy người chưa có URL EA FC 27 xác minh. Các trường người dùng bổ sung không
  được ghi thành SofaScore/EA xác minh.
- Sáu file Chelsea–Tottenham được đối chiếu SHA-256 và giữ nguyên.
  Bổ sung James Rowswell CB/RB riêng cũng không thay đổi trong lượt này.

## Kiểm tra importer chung trên H2 mới

JAR hiện có: backend/target/premierhub-backend-0.1.0-SNAPSHOT.jar.
DB mới: backend/target/aston-villa-newcastle-data/user-supplements-db.mv.db.
Mỗi lệnh đặt rõ URL H2, username sa, password trống,
spring.sql.init.mode=never và spring.main.web-application-type=none.

Seed schema hiện có, 55 ID/CLB/nhóm rộng và goals/assists từ snapshot API đã dùng.
Membership local mô phỏng hiệu lực ngày 03/10 từ roster, không phải ngày chuyển
nhượng thật. DB mới ban đầu không có hồ sơ hay vị trí: kiểm tra CSV bản cuối
như một lần chèn mới, không mô phỏng việc tự ghi đè production.

| File | Lần | Rows | Inserted | Updated |
| --- | ---: | ---: | ---: | ---: |
| profiles.csv | 1 | 55 | 55 | Không có thao tác cập nhật |
| profiles.csv | 2 | 55 | 0 | Không có thao tác cập nhật |
| positions.csv | 1 | 55 | 55 | 0 |
| positions.csv | 2 | 55 | 0 | 0 |

Cả bốn lệnh exit code 0. Reader/importer chung kiểm tra ID, đúng CLB/mùa,
định dạng, mã vị trí, ID trùng, primary thuộc eligible và nhập lặp.
Không thêm kiểm tra Java riêng cho batch.

## Đọc lại H2

| CLB | Hồ sơ | Quốc tịch | DOB | Chiều cao | Chân thuận | Số áo | OVR số | Primary |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Aston Villa | 27 | 27 | 27 | 27 | 27 | 27 | 27 | 27 |
| Newcastle | 28 | 28 | 28 | 28 | 28 | 28 | 28 | 28 |

- 55 primary, 96 eligible, 0 primary nằm ngoài eligible.
- Wright GK/OVR61; Goretzka CM/OVR78; Pivas CB/OVR62.
- Kyran RW + RM, 183 cm, OVR60; Mason CM + RB, 181 cm, OVR60.
- Michael ST, 179 cm, LEFT, OVR60; Vakhtang ST, OVR60.
- Không có ô hồ sơ NULL trong DB kiểm tra mới.
- Log/snapshot/seed/DB nằm dưới backend/target/, Git ignored.

## Khi áp dụng vào production sau này

Importer hồ sơ hiện tại chỉ chèn và từ chối nội dung khác đã lưu. Không dùng
profiles.csv cuối để tự ghi đè hồ sơ production còn chứa NULL. profile-updates.csv
là bản kê tám ô thay đổi (player_id,club_id,field,value), cùng định dạng bản bổ
sung 02/10; không phải đầu vào PlayerProfileCsvReader và chưa có importer mới.

Khi được phép ghi production, phải xác nhận ID/membership và giá trị hiện hành,
sao lưu SQL mới, rồi cập nhật đúng tám ô đã review với điều kiện giá trị cũ NULL
(trường hợp đã đúng nội dung thì bỏ qua; có giá trị khác thì báo xung đột).
Sau đó profiles.csv khớp dữ liệu đã bổ sung và importer phải thêm 0.
Lượt này chưa thực hiện thao tác đó.

## Lịch sử kiểm tra trước bổ sung

Bản đầu có 55 hồ sơ, 48 bộ vị trí và bảy MISSING; profile nhập thử thêm 0 vì
khớp snapshot API. Vị trí lần đầu thêm 48, lần hai thêm/cập nhật 0. Bản cuối
ở trên thay thế dữ liệu thiếu bằng nguồn người dùng, không thay đổi nguồn
EA của 48 người đã xác minh.

## File cần commit

Trong thư mục batch này: profiles.csv, positions.csv, roster-status.csv,
sources.md, missing-fields.txt, validation.md và profile-updates.csv (mới).

Commit message đề xuất:

```text
data(players): complete Villa and Newcastle profiles and positions with user values
```

Chưa ghi MySQL production, commit, push hoặc deploy.

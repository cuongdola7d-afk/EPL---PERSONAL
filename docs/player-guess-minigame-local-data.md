# Dữ liệu để chơi Minigame local — 08/10/2026

Người dùng đã giao hoàn tất phần dữ liệu local sau khi giao diện kết nối API. Chỉ dùng snapshot đã review trong repo, không truy cập production, không thu thập lại nguồn và không dùng cầu thủ giả của mockup.

## Chạy

Từ `backend/`:

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=minigame-local'
```

Từ `frontend/`, chạy `npm.cmd run dev`, mở `http://localhost:5173/#minigame`, đăng nhập/đăng ký bằng email và chọn daily hoặc luyện tập. Cả hai cần tài khoản; Google không bắt buộc.

Profile tự nạp dữ liệu vào **`backend/target/minigame-local`**. Command chỉ đăng ký khi có profile `minigame-local`, không có `prod` và `premierhub.minigame.local-data.enabled=true`. Nó kiểm tra cả URL cấu hình và URL thực của JDBC connection: chỉ cho phép file H2 đúng đường dẫn `target/minigame-local`, từ chối MySQL, H2 TCP/memory, file khác hoặc JDBC option ngoài các option đã cho phép. Không có endpoint nhập dữ liệu.

Khi muốn dùng database test riêng bằng cách override datasource, phải tắt bootstrap:

```text
--premierhub.minigame.local-data.enabled=false
```

Snapshot được đọc từ thư mục `data/` khi chạy trong `backend/`. Không dùng `mvn clean` nếu cần giữ tài khoản/tiến trình H2 trong `target/`: clean xóa database đó. Khởi động lại thông thường giữ dữ liệu; bootstrap chỉ thêm phần chưa có. Sau clean, dữ liệu bóng đá có thể nạp lại từ CSV nhưng tài khoản/tiến trình cũ đã mất nếu không có backup.

## Nguồn được tái sử dụng

- [Roster 30/09](../backend/data/roster-2026-09-30/README.md): `batch-01.csv` đến `batch-04.csv`, 534 ID/tên/CLB, vị trí rộng và khoảng membership đã chốt. Giữ nguyên các ngày bắt đầu/kết thúc; không kéo ngược membership về GW1.
- [Bộ hồ sơ/vị trí cuối 04/10](../backend/data/player-data-production-2026-10-04/README.md): `profiles-final.csv` (534), `positions-final.csv` (532), `club-coverage.csv` (20 tên/ID CLB). Đây là dữ liệu và nguồn đã chốt trong repository; chữ “production” trong tên thư mục chỉ là tên batch lịch sử, không phải kết nối production.
- Thiếu trường theo [missing-fields.txt](../backend/data/player-data-production-2026-10-04/missing-fields.txt) vẫn giữ `NULL`/không có vị trí. OVR thủ công do người dùng đặt giữ nguyên nguồn và giá trị; Max Dowman vẫn 72, vì vậy không vào pool đáp án >=75. Không đặt city, chỉ số trận hoặc tổng mùa chưa biết.

Command kiểm tra đủ file, số dòng của snapshot, ID duy nhất, CLB thuộc snapshot, membership hiệu lực tại ngày 04/10 và khóa ID/CLB giữa roster/hồ sơ khớp toàn bộ trước khi nhập. Nó tạo các identity từ đúng ID/tên đã chốt (gồm ID provider có sẵn trong roster), rồi tái sử dụng `ManualRosterImporter`, `PlayerProfileImporter`, `PlayerPositionImporter` trong một transaction chung.

Tên/CLB/membership/hồ sơ/vị trí khác dữ liệu đã lưu làm lệnh thất bại và rollback toàn bộ. Bootstrap không patch dữ liệu đang có, không xóa các dòng ngoài snapshot, không sửa mùa 2024/25, tài khoản, câu hỏi, selector daily hay kết quả. Importer chỉ tạo các dòng `player_season_stats` cần cho khóa ngoại, các tổng trận/phút/bàn/kiến tạo để `NULL`.

Đây là snapshot được review, không phải cơ chế đồng bộ tự động các chuyển nhượng sau 04/10. Luật chơi vẫn lấy membership hiệu lực và tuổi theo ngày tạo ván trên server; mọi ván giữ snapshot khi bắt đầu.

## Kết quả kiểm chứng

Sau nhập và đọc lại H2 local:

| Dữ liệu | Số lượng |
| --- | ---: |
| CLB mùa 2026 | 20 |
| Cầu thủ/membership/hồ sơ | 534 |
| Bộ vị trí cụ thể | 532 |
| Người đủ 8 gợi ý và OVR >=75 | 364 |
| OVR còn NULL | 3 |
| Chiều cao còn NULL | 1 |

Danh sách chọn khi đoán gồm cả 534 người; 170 người chưa đủ điều kiện làm đáp án vẫn có thể được đoán theo luật đã chốt. Membership snapshot đều khớp ngày 08/10 khi kiểm tra. API `/api/players?season=2026` trả 534 dòng. Khởi động lại in `MINIGAME_LOCAL_DATA snapshot=2026-10-04 inserted=0 roster=534 eligibleAnswers=364`. Bootstrap không tự tạo ván/kết quả; trước khi sao chép sang DB test, local có 0 ván và 0 kết quả daily.

Trước ghi đã dừng tiến trình backend local do Codex mở và sao lưu file H2 tại `backend/target/minigame-local-backup-<timestamp>/`. Bản sao này và log được Git ignore; không đưa vào commit. Không dừng dịch vụ của người dùng hoặc đọc biến kết nối production.

52 test backend liên quan qua: 6 test bootstrap mới, 20 test luật, 25 test tích hợp Minigame và 1 test persistence. Test mới nhập bộ CSV thật vào H2 cô lập, kiểm tra thiếu trường, dữ liệu lịch sử, rollback xung đột, membership không bị kéo dài, file thiếu, URL đích bị từ chối, daily/practice và reimport giữ nguyên tiến trình/selector. Lượt đầu fixture memory dùng connection đóng sau tạo schema gặp lỗi H2 CHECK; fixture cuối giữ một connection cho mỗi test và đóng sau test, rồi nhóm 52 test qua. Không sửa schema hay bỏ CHECK để che lỗi này. Chưa chạy lại toàn suite backend; các lỗi sẵn có đã ghi ở tài liệu API.

Chrome thật chạy ở 1440px và 390px với **bản sao H2 dưới `backend/target/minigame-real-ui-check/db`**, backend 8085 và Vite 5185. Bản sao chứa roster thật vừa nạp, không có cầu thủ giả. Đã xác nhận đăng ký/login email, bấm đúp chỉ một start, reload giữ ván, đoán Dowman 72 hợp lệ nhưng sai, mở hint, tìm đáp án theo 8 gợi ý, thắng daily 30 điểm và BXH đồng hạng; lịch sử chỉ đọc; luyện tập loại đáp án daily, thắng ở 0 điểm và không cộng BXH. Không có lỗi JavaScript hay tràn ngang. 36 POST Minigame kiểm thử chỉ gửi vào bản sao; không tạo tài khoản hay điểm kiểm thử trong database local đang dùng để chơi. Script/ảnh/result ở cùng thư mục `target/` đó.

## Phạm vi và phần còn lại

Code mới: `PlayerGuessLocalDataCommand.java` và test tương ứng; bật bootstrap trong `application-minigame-local.properties`. Không thêm dependency, không sửa luật/API/frontend/POM hoặc file dữ liệu nguồn. Có thể chơi local ngay bằng roster đã chốt.

Chưa ghi production, commit, push hoặc deploy. Trước phát hành còn cần migration và kiểm thử MySQL thử theo kế hoạch đã có.

# Hướng dẫn Backup Aiven và Dựng H2 Thử Nghiệm

> Quy trình chuẩn hóa ngày **25/11/2026**. Luôn dùng đúng 2 lệnh cố định trong `backend/scripts/`; không tự viết lại script backup hoặc script dựng H2 mới cho từng đợt thao tác.

## 1. Vị trí lưu và bảo mật backup

- Mọi bản dump được lưu tại `backend/local-backups/<Label>-<yyyyMMdd-HHmmss>/aiven-<Label>-<yyyyMMdd-HHmmss>.sql`.
- Thư mục `backend/local-backups/` nằm trong `.gitignore` (không bao giờ được commit).
- **Cảnh báo bảo mật:** file dump chứa bảng `accounts` (gồm email người dùng và mật khẩu đã băm) và dữ liệu phiên. **Không bao giờ** đẩy lên GitHub, đính kèm vào issue/PR hoặc chia sẻ qua link công khai.
- Credentials kết nối Aiven nằm ngoài repo tại `D:\File Jva\PrismaXI-secrets\aiven-production.env`; chứng chỉ CA nằm tại `backend/secrets/aiven-ca.pem` (đã gitignore). Script chỉ nạp biến môi trường vào tiến trình con `mysqldump`, không in ra màn hình, không ghi tạm secrets ra đĩa.

## 2. Hai lệnh cố định

Chạy từ thư mục gốc repo `D:\File Jva\EPL---PERSONAL` trên Windows PowerShell 5.1+ (nếu máy bật ExecutionPolicy `Restricted`, chạy qua `powershell -NoProfile -ExecutionPolicy Bypass -File .\backend\scripts\...`):

### Backup Aiven MySQL

```powershell
.\backend\scripts\backup-aiven.ps1 -Label don-du-lieu
```

- Tự chạy `mysqldump` với `--ssl-mode=VERIFY_IDENTITY`, `--single-transaction`, `--routines`, `--triggers`, `--set-gtid-purged=OFF`, `--column-statistics=0`, `--default-character-set=utf8mb4`, `--skip-lock-tables`.
- Tự kiểm tra dòng kết thúc `Dump completed`, đếm số bảng `CREATE TABLE` và tính SHA-256.
- In ra đúng **1 dòng kết quả**:

```text
BACKUP_OK path=D:\File Jva\EPL---PERSONAL\backend\local-backups\don-du-lieu-20261125-171000\aiven-don-du-lieu-20261125-171000.sql bytes=5123456 tables=44 sha256=<64-hex>
```

Nếu thiếu `mysqldump`, thiếu file secrets/CA hoặc kết nối lỗi, lệnh in `BACKUP_FAILED <lý do>` (đã che secrets) và trả mã thoát khác 0.

### Dựng H2 thử nghiệm từ bản backup mới nhất (không kết nối Aiven)

```powershell
.\backend\scripts\h2-from-latest-backup.ps1
```

Hoặc chỉ định rõ một file `.sql`:

```powershell
.\backend\scripts\h2-from-latest-backup.ps1 -Backup "D:\File Jva\EPL---PERSONAL\backend\local-backups\don-du-lieu-20261125-171000\aiven-don-du-lieu-20261125-171000.sql"
```

- Tạo mới hoàn toàn file H2 tại `backend/target/h2-from-backup/premierhub.mv.db` từ 6 file schema local và toàn bộ câu `INSERT` của file dump (bỏ qua `SPRING_SESSION*`).
- Hoàn toàn offline, **không kết nối Aiven**.
- In ra đúng **1 dòng kết quả**:

```text
H2_OK jdbcUrl=jdbc:h2:file:D:/File Jva/EPL---PERSONAL/backend/target/h2-from-backup/premierhub;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE tables=48 source=D:\File Jva\EPL---PERSONAL\backend\local-backups\...
```

## 3. Khi nào bắt buộc backup (quy tắc 23/11/2026)

- **Bắt buộc backup trước khi chạy production:**
  - Đổi schema (chạy migration `CREATE`/`ALTER`/`DROP`).
  - Xóa hoặc sửa hàng loạt dữ liệu đã có (ví dụ: gộp ID trùng, dọn dữ liệu cũ).
  - Nhập lại hoặc ghi đè dữ liệu cũ.
- **Không bắt buộc backup:**
  - Nhập dữ liệu vòng đấu thường (chỉ thêm dòng mới, importer idempotent, đã chạy thử thành công 2 lượt trên H2 mới dựng từ dump gần nhất).
- Nếu người dùng đã ghi rõ trong prompt *"người dùng đã backup lúc …"* thì không chạy backup lại.

## 4. Cách khôi phục

### (1) Mở dump để kiểm tra hoặc dựng H2 local

- Có thể mở trực tiếp file `.sql` để đối chiếu dữ liệu cũ, hoặc chạy `.\backend\scripts\h2-from-latest-backup.ps1 -Backup <file.sql>` để dựng lại đúng trạng thái database tại thời điểm backup trên H2 local và chạy backend trỏ H2 để kiểm tra.

### (2) Khôi phục ngược lên Aiven production

> **Cảnh báo:** **Chỉ thực hiện khi người dùng yêu cầu rõ ràng bằng chỉ thị riêng.** Không bao giờ tự khôi phục lên Aiven.

Trình tự an toàn bắt buộc khi được yêu cầu khôi phục:

1. **Backup hiện trạng trước khi ghi đè:** chạy `.\backend\scripts\backup-aiven.ps1 -Label before-restore` để giữ nguyên bản chụp ngay trước lúc khôi phục.
2. **Tạm dừng thao tác ghi bên ngoài:** đảm bảo các job tự động (`FplProvisionalSync`, `FantasyAutomation`, `FootballDataAutoSync`) không chạy đè trong lúc restore (hoặc thực hiện trong khung bảo trì đã thống nhất).
3. **Khôi phục từ file dump đã xác minh SHA-256:** nạp credentials từ `D:\File Jva\PrismaXI-secrets\aiven-production.env` vào biến môi trường con `MYSQL_PWD` và nạp file dump qua `mysql.exe` với `--ssl-mode=VERIFY_IDENTITY --ssl-ca=backend\secrets\aiven-ca.pem --default-character-set=utf8mb4`.
4. **Kiểm tra số bảng và số dòng sau khôi phục:** đối chiếu tổng số bảng (`information_schema.tables`) và số dòng ở các bảng trọng yếu (`accounts`, `players`, `fixtures`, `manual_fixture_player_stats`, `manual_fixture_player_extra_stats`, `fantasy_gameweeks`, `fantasy_entries`, `fantasy_team_results`, `player_season_stats`) so với mốc mong muốn trước khi mở lại bình thường.

## 5. Khuyến nghị lưu trữ dài hạn

- Thỉnh thoảng sao chép thư mục `backend/local-backups/` sang ổ USB hoặc cloud lưu trữ cá nhân có bảo mật riêng tư (không chia sẻ công khai) để tránh mất dữ liệu khi hỏng ổ cứng máy local.
- Giữ khoảng **5 bản backup gần nhất** (cộng thêm các mốc lớn như trước khi đổi schema quan trọng) và dọn bớt các bản cũ hơn nếu dung lượng đĩa hạn chế.

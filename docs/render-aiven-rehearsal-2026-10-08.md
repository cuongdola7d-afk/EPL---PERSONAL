# Render/Aiven — checkpoint sao chép ban đầu 08/10/2026

**Cập nhật sau checkpoint:** Google Client ID và Secret trên Render đã khớp Railway, deployment mới nhất live lúc 21:40 ngày 08/10; callback vẫn Vercel. Phần chặn Google bên dưới mô tả thời điểm diễn tập trước khi người dùng sửa. Theo dõi bản cuối và trạng thái traffic ở [checkpoint cutover cuối](render-aiven-final-cutover-2026-10-08.md); không dùng dump diễn tập này để chuyển traffic.

Phạm vi được giao: kiểm tra Render/Aiven, backup đầy đủ Railway, nhập bản sao vào Aiven sau preflight không xung đột, đối chiếu và chuẩn bị cutover. Không đổi proxy Vercel, dừng Railway, commit hoặc push. Không tạo app account, phiên đăng nhập hoặc ván chơi thử bằng thao tác tự động.

## Kết quả đã kiểm chứng

- Render URL: `https://epl-personal.onrender.com`.
- GET `/actuator/health` trước và sau restore trả HTTP 200, JSON `{"status":"UP"}`. Endpoint ẩn components/details; hostname/schema và chính sách TLS được xác minh thêm bằng cấu hình Render API và MySQL bên dưới.
- **Trước restore**, GET `/api/clubs?season=2026` trả HTTP **500**, JSON code `INTERNAL_ERROR` khi schema đích chưa có bảng. **Sau restore và startup**, endpoint trả **200, 20 đội**; không còn lỗi này.
- GET trực tiếp `/api/auth/me` không kèm proof trả HTTP **403**, JSON code `AUTH_PROXY_REQUIRED`, đúng hành vi chặn truy cập private ngoài proxy. Không đăng nhập, không tái sử dụng phiên trong dump.
- Railway nguồn: MySQL **9.7.2**, schema `railway`, đúng UUID đã lưu trong hướng dẫn chuyển.
- Backup mới: `D:\File Jva\PrismaXI-backups\render-aiven-20261008-200542\railway-full.sql`.
- Dump bắt đầu 20:05:43, hoàn tất 20:07:58 ngày 08/10/2026 (Asia/Ho_Chi_Minh), exit code 0, **42 bảng**, **1.238.135 byte**. Inventory CREATE TABLE khớp nguồn; có footer hoàn tất; không có CREATE DATABASE, USE hoặc DROP TABLE.
- SHA-256: `e7a64dc6d3c3c5d4abac09640979f8437d7089de4a785e76714bc587377ccb08`.
- Dump dùng `--single-transaction --hex-blob --tz-utc`, giữ đủ tài khoản/identities, hai bảng session, Fantasy, Minigame, dữ liệu bóng đá và mọi mùa. Không nhập lại CSV, sửa hoặc reset nguồn.
- Railway vẫn nhận ghi. Backup này là snapshot diễn tập, **không phải bản cuối để chuyển traffic**.
- Đã restore toàn bộ dump vào MySQL **9.6.0 cô lập trên local**: 42 bảng, **9.558 dòng**, đủ hai bảng session. Đã tạo fingerprints exact counts/hash rows của chính snapshot dump để đối chiếu sau khi nhập Aiven. Tiến trình fixture restore đã được dừng và kiểm tra port 33031 không còn lắng nghe; MySQL local hiện hữu không bị dừng. Kết quả 9.6.0 này chưa chứng minh tương thích với phiên bản Aiven chưa đọc được.
- Đã kết nối Aiven **MySQL 8.4.8**, schema **`defaultdb`**, UUID `6eed0cfc-c2f7-11f1-bb2b-2a39d4ad4300`, lower_case_table_names=0. CLI dùng **VERIFY_IDENTITY** với CA đúng service, cipher **TLS_AES_128_GCM_SHA256**, session UTC. Trước nhập: **0 bảng**, không dữ liệu xung đột, không connection khác vào defaultdb và không transaction InnoDB đang chạy.
- Người dùng xác nhận Render đã tạm dừng trước restore. Đã nhập toàn dump vào Aiven, **exit code 0**, thời gian **123,57 giây**. Không sửa dump, không nhập CSV, không DROP/reset nguồn hoặc ghi đè bảng đích; không tạo tài khoản/ván thử.
- Đối chiếu snapshot: **42 bảng / 9.558 dòng**, **0 bảng lệch counts/hash**, tên/case bảng và AUTO_INCREMENT khớp. Schema, cột/index/constraints khớp sau chuẩn hóa cách SHOW CREATE hiển thị: Aiven mặc định ANSI ẩn table options và MySQL 8.4 ghi thêm CHARACTER SET utf8mb4 ngầm định. Chỉ đổi sql_mode của session đọc để so, **không đổi global SQL mode Aiven**.
- Inventory đích: 42 InnoDB, 42 PRIMARY KEY, 11 UNIQUE, **56 FOREIGN KEY**, **56 CHECK**. Đã kiểm tra cả 56 khóa ngoại: **0 dòng mồ côi**.
- Dữ liệu quan trọng trong snapshot khớp: accounts 5, account_identities 4; Fantasy entries 4, draft picks 44, submitted picks 44; Minigame games 12, actions 73, daily_results 1; manual_fixture_player_stats 2.000, player_season_stats 656. Hai bảng session có **0 dòng tại chính thời điểm snapshot**, được restore đầy đủ schema và đối chiếu hash; không xóa phiên sau restore và không bỏ qua bảng session.
- Đây là kiểm chứng **bản dump cụ thể** restore sang 8.4.8, không khẳng định mọi tính năng/dữ liệu của MySQL 9.7 đều downgrade được. Khi dump cuối thay đổi schema, chạy lại preflight/restore/đối chiếu phù hợp.
- Đã đọc cấu hình runtime Render qua API key người dùng lưu trong thư mục Git ignore. Service có tên Dashboard `EPL---PERSONAL`, URL `https://epl-personal.onrender.com`, Docker Free, health `/actuator/health`, auto deploy off, không suspended. Live SHA **61de7ae015756c7fc1d3d6d8538746a6cc8f7363** khớp HEAD đã review; Docker Command để trống, sử dụng entrypoint.
- Runtime đúng **prod,render**, JDBC host/port/defaultdb và credentials khớp Aiven local, URL không có query/fragment/credential. CA Secret File khớp CA local. Không có override datasource/TLS hoặc SQL/session init bật lại; Minigame enabled=true, local-data=false. Profile của SHA này dùng **VERIFY_IDENTITY**. Trên Aiven đã thấy **1 connection Connector/J có TLS** sau khi Render được bật lại.
- Proxy secret Render khớp Railway hiện tại; public origin và Google callback vẫn Vercel; Google client secret khớp nguồn. **Còn chặn auth Google trước cutover:** `PREMIERHUB_GOOGLE_CLIENT_ID` trên Render khác Railway và không có suffix chuẩn `.apps.googleusercontent.com`; không phải khác biệt khoảng trắng. Chưa sửa environment Render. Đã chuẩn bị đúng giá trị nguồn trong file Git ignore `backend/secrets/render-google-client-id.correct.txt`; copy riêng giá trị đó vào đúng biến Render, không in vào chat/Git. Không đổi Google callback hoặc tạo OAuth client mới.
- Khi user vừa resume, các GET đầu tiên có timeout ở mức 50 giây. Đã retry riêng 6 endpoint từng timeout sau startup: tất cả đạt. Đây là số đo lúc resume; **chưa đo cold start sau idle qua proxy Vercel** vì proxy vẫn Railway. Không báo mọi request cold sẽ thành công.
- API warm: health UP; clubs/standings 2024 và 2026 mỗi endpoint **200 / 20 đội**; players 2026 **200 / 534 cầu thủ**; matches 2026 **200 / 380 trận**; Minigame info **200**, timezone Asia/Ho_Chi_Minh, initialScore 100. Private auth và practice/current gọi trực tiếp thiếu proof đều **403 AUTH_PROXY_REQUIRED**. Không gọi start/guess/leaderboard, không login hoặc tạo app account/ván/đội tự động.
- Đã lưu 11 báo cáo inventory/schema/FK/fingerprints/runtime/API trong `D:\File Jva\PrismaXI-backups\render-aiven-20261008-200542\aiven-copy-verification\`, ngoài Git, cùng backup để không mất bằng chứng khi Maven clean target. Không chứa giá trị credentials hoặc đáp án trong các báo cáo; full dump là dữ liệu nhạy cảm và vẫn ngoài Git.

## Credentials riêng, không đưa vào Git

Đã xác nhận `backend/secrets/` được `.gitignore` bỏ qua và không có file được Git theo dõi trong thư mục này.

1. `backend/secrets/railway-migration.json`: credentials nguồn đã lấy bằng Railway CLI và lưu riêng, không in ra chat/log. Không thay bằng credentials Aiven.
2. Mở `backend/secrets/aiven-migration.json` bằng editor, nhập các trường `host`, `port` (số), `user`, `password` của đúng Aiven service. Giữ `database` là `defaultdb` như Render đang cấu hình. JSON cần escape dấu `"` hoặc `\` nếu có trong giá trị.
3. Tải CA Certificate từ đúng service/project Aiven, lưu `backend/secrets/aiven-ca.pem`; trường `ssl_ca` trong JSON đã trỏ path này. Đây là bản local để MySQL CLI kiểm chứng CA/hostname. Secret File trên Render vẫn dùng `/etc/secrets/aiven-ca.pem`.
4. Không gửi credentials trong chat, không export toàn bộ environment ra output và không commit JSON/CA/truststore/dump. Nếu file đã có thông tin, không ghi đè bằng mẫu rỗng.
5. Để đọc trực tiếp cấu hình và deployment Render, có thể lưu riêng API key Render trong `backend/secrets/render-api-key.txt`; không cần in key. Nếu không dùng API, kiểm tra Dashboard: `SPRING_PROFILES_ACTIVE=prod,render`, JDBC URL `jdbc:mysql://<AIVEN_HOST>:<AIVEN_PORT>/defaultdb` không có query; CA đúng service; không có environment override tắt VERIFY_IDENTITY hoặc bật SQL/session schema init.

## Kiểm tra cần hoàn thành trước nhập Aiven

- Đọc VERSION(), DATABASE(), server UUID, collation, engine, columns, constraints/indexes, AUTO_INCREMENT và COUNT(*) thực tế cho mỗi bảng. Kiểm tra view/routine/trigger/event nếu có.
- Kết nối CLI phải dùng `--ssl-mode=VERIFY_IDENTITY --ssl-ca=<đúng CA>`, hostname Aiven và `Ssl_cipher` không rỗng. Không hạ mức kiểm chứng để xử lý lỗi.
- Health UP không cho biết bảng nào được tạo. `prod,render` trong repo tắt DDL startup; xác định thực tế bằng inventory đích, không suy đoán rằng database rỗng.
- Nếu có bảng/dữ liệu trùng ở `defaultdb`, báo tên/số dòng trước; không DROP/TRUNCATE/ghi đè hoặc nhập chồng tự động. Backup đích và phương án xử lý cần được chốt riêng.
- Nếu phiên bản Aiven thấp hơn 9.7.2, kiểm tra cú pháp/collation/constraints và thử logical restore trên đúng đích; không coi downgrade đã tương thích chỉ vì TLS/health tốt. Không copy datadir.
- Tạm dừng **Render backend đích** và xác nhận không còn writer/transaction vào Aiven trong lúc restore/đối chiếu; không dừng Railway trong lượt sao chép ban đầu. Kể cả GET Minigame và cleanup session cũng có thể ghi.
- Restore toàn dump vào `defaultdb` bằng stdin dạng byte, không `--force`, không thay tên schema bằng replace toàn file. Dump không có CREATE DATABASE/USE nên có thể chọn đích `defaultdb` trực tiếp.
- So inventory/schema/index/FK/CHECK/AUTO_INCREMENT và exact counts/hash rows toàn bộ 42 bảng với chính snapshot dump đã restore cô lập. Bao gồm BLOB hai bảng session, không dùng phiên trong dump để impersonate. Nguồn đang có ghi mới nên không so snapshot với nguồn hiện tại rồi kết luận mất dữ liệu.
- Sau đối chiếu, bật lại Render, kiểm tra health và API công khai; API private không có proof phải bị chặn. Chưa xác minh user flow qua Vercel vì proxy còn Railway. Không tạo tài khoản/ván/đội thử bằng automation.

**Trạng thái hiện tại:** đã nhập và đối chiếu bản sao Aiven đạt, Render đã được người dùng bật lại, TLS/runtime/API đọc đã kiểm tra. Cần sửa Google Client ID Render, kiểm tra startup/cold và thực hiện dump cuối sau freeze trước cutover. Không đổi kết nối Railway, environment Render hoặc proxy hiện tại; không commit/push. Không chuyển traffic từ bản diễn tập này.

## Proxy đã chuẩn bị, chưa áp dụng

File review: [deployment/vercel.render.cutover.json](deployment/vercel.render.cutover.json).

Thay đổi duy nhất được chuẩn bị so với `frontend/vercel.json`:

```diff
- "dest": "https://epl-personal-production.up.railway.app/api/$1"
+ "dest": "https://epl-personal.onrender.com/api/$1"
```

Giữ nguyên `/api/(.*)`, `$1`, proof header lấy từ env Vercel, no-store, thứ tự filesystem/SPA và cùng origin. Không dùng `VITE_API_BASE_URL`. Khi cutover thật, sửa cả kỳ vọng Railway trong `frontend/src/api/deployment.test.js` và chạy riêng test liên quan; chưa sửa các file frontend trong lượt này.

## Cửa sổ bảo trì để chuyển bản cuối

Chỉ đặt lịch sau khi restore diễn tập và API/TLS được xác minh. Đã đo dump khoảng 2 phút 16 giây, restore Aiven khoảng 2 phút 4 giây; cần cộng thời gian đối chiếu, startup/warm và deploy frontend để chốt thời lượng. Có thể dự trù 15–30 phút rồi điều chỉnh theo diễn tập đầy đủ; không cam kết trước khi các bước còn lại đạt. Tránh 00:00 Việt Nam và Fantasy deadline thực tế.

Trước đặt lịch, sửa Google Client ID Render bằng giá trị nguồn đã chuẩn bị, để redeploy/startup hoàn tất và đọc lại runtime cho khớp. Không cần sửa frontend/domain/callback Google cho bước này.

1. Thông báo bảo trì, chặn thao tác UI và dừng importer/worker. Drain rồi tạm dừng **Railway backend** theo quyền ở lượt cutover, giữ Railway MySQL nguyên trạng. Xác nhận không còn writer/transaction; banner/chặn POST riêng không đủ.
2. Dừng Render backend. Xuất **dump cuối mới** sau freeze ra thư mục backup mới ngoài Git; inventory/hash nguồn khi quiescent.
3. Restore vào schema Aiven cuối đã được xác nhận mới/rỗng. Vì `defaultdb` sẽ chứa bản diễn tập sau bước sao chép, không nhập chồng dump cuối. Chọn schema mới và cập nhật JDBC Render, hoặc backup rồi thay thế đích diễn tập sau khi có quyền rõ ràng; không tự reset database nào.
4. Đối chiếu đủ schema/data/session/Fantasy/Minigame và thống kê. Khởi động Render, warm health/API, kiểm chứng cấu hình/TLS/schema cuối. Railway backend vẫn dừng để tránh hai bản dữ liệu cùng nhận ghi.
5. Áp dụng đúng thay đổi proxy trên, deploy Vercel theo quyền lượt cutover. Chủ tài khoản thật kiểm tra tài khoản/Google, đội Fantasy đã lưu và Minigame tiếp tục/reload; không gửi mật khẩu/token.
6. Gỡ bảo trì khi đạt. Giữ backup và Railway MySQL nguyên trạng. Sau khi có ghi mới tại Aiven, không trỏ lại bản Railway cũ nếu chưa reconcile các ghi đó.

Hướng dẫn gốc: [render-aiven-migration.md](render-aiven-migration.md).

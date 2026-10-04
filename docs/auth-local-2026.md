# Tài khoản PrismaXI — email/mật khẩu local

## Phạm vi đã hoàn thành

Bước 1 của [kế hoạch Fantasy 2026/27](fantasy-multiplayer-2026-plan.md): đăng ký, đăng nhập, đăng xuất và xem tài khoản của chính mình. Không yêu cầu xác thực email trước khi chơi. Google đã có code OIDC và liên kết vào cùng tài khoản, kiểm tra với provider local; [hướng dẫn Google](google-auth-2026.md) ghi cấu hình và phần thử Google thật còn thiếu. Khôi phục mật khẩu, lưu đội dự thi, chốt đội, chấm điểm và BXH người chơi chưa triển khai. Tài khoản độc lập với `player_id` và dữ liệu mùa bóng; không thay luật Fantasy hay dữ liệu 2024/25.

Spring Boot giữ nguyên **4.1.1**, Java mục tiêu **21**. Dependency do BOM hiện có quản lý: Spring Security **7.1.1**, Spring Session **4.1.1**; dùng các starter Security, Session JDBC và Security Test, không thêm Redis hoặc JWT.

## Cách chạy an toàn trên local

Từ `backend/`, chạy JAR đã build với profile bắt buộc `auth-local`:

```powershell
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=auth-local --server.port=8080
```

Profile này ghi đè URL/user/password datasource kế thừa bằng H2 riêng `jdbc:h2:file:./target/auth-local;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE`. Không tải `.env.local` của production để thử tài khoản. Không chạy đồng thời profile `prod`; không truyền JDBC MySQL qua tham số. H2 nằm trong `backend/target/`, được Git bỏ qua và mất nếu chạy `clean`; đây là dữ liệu thử, không phải kho tài khoản phát hành.

Từ `frontend/`, chạy `npm.cmd run dev`, mở `http://localhost:5173`. Vite proxy `/api` đến backend local 8080, vì vậy trình duyệt gửi cookie cùng origin. Không phải cấu hình `VITE_API_BASE_URL` cho dev. Nếu dùng cổng frontend khác, cấu hình đúng origin trong `PREMIERHUB_CORS_ALLOWED_ORIGINS` của backend; các origin mặc định là localhost/127.0.0.1:5173.

Nút đăng nhập/tài khoản nằm trong header. Đăng ký thành công chuyển sang form đăng nhập, không tự đăng nhập. Popup không điều hướng hay tháo component Fantasy. Mật khẩu được xóa khỏi state sau thao tác thành công và khi đổi form; không ghi thông tin xác thực vào localStorage/sessionStorage. Đóng popup xóa state form. Lựa chọn Fantasy hiện có không chuyển thành đội dự thi.

## API và dữ liệu

| Method | Endpoint | Hành vi |
| --- | --- | --- |
| GET | `/api/auth/csrf` | Lấy `token`, `headerName`; tạo phiên ẩn danh khi cần. |
| POST | `/api/auth/register` | Body `email`, `displayName`, `password`; 201 với tài khoản mới, 409 nếu email trùng. |
| POST | `/api/auth/login` | Body `email`, `password`; 200 với tài khoản, 401 chung cho sai email/mật khẩu. |
| POST | `/api/auth/logout` | 204; hủy phiên server và xóa cookie phiên. |
| GET | `/api/auth/me` | Tài khoản của phiên hiện tại; chưa đăng nhập hoặc phiên đã hủy trả 401. |

Mỗi POST auth lấy CSRF mới qua GET trước đó, giữ cookie bằng `credentials: 'include'` và gửi token vào header trả về (`X-CSRF-TOKEN`). Thiếu/sai CSRF trả 403. Token CSRF chỉ ở bộ nhớ trong request, không phải token đăng nhập. Login đổi mã phiên để chống session fixation và thay CSRF; logout vô hiệu hóa phiên và CSRF. Client không đọc cookie HttpOnly.

Response tài khoản chỉ chứa `id`, `email`, `displayName`, `role`, `createdAt`. ID độc lập với cầu thủ; email riêng tư chỉ trả qua đăng ký/đăng nhập/thông tin chính tài khoản. Email được `strip()` và lowercase bằng `Locale.ROOT` cả đăng ký lẫn đăng nhập, có unique constraint SQL chống đăng ký trùng đồng thời. Tên hiển thị 2–80 ký tự, bỏ khoảng trắng hai đầu, không chứa ký tự điều khiển. Mật khẩu tối thiểu 8 ký tự khi đăng ký, tối đa 72 byte UTF-8 để BCrypt không cắt ngầm mật khẩu nhiều byte. Không chuẩn hóa hoặc trim mật khẩu.

`PasswordEncoderFactories.createDelegatingPasswordEncoder()` lưu hash `{bcrypt}` với mặc định của Spring Security. Không trả hash hay mã phiên trong response; DTO request có `toString()` đã che credentials, handler lỗi không trả rejected value và không ghi request body/password vào log. Role đăng ký luôn là `USER` trong SQL, không lấy role từ request; chưa có luồng cấp ADMIN.

`auth-schema.sql` ban đầu bổ sung `accounts`, `SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES`; lượt Google thêm `account_identities` với khóa duy nhất provider/subject và FK tới cùng account. Các bảng phiên theo Spring Session JDBC, tăng `PRINCIPAL_NAME` lên 254 ký tự cho email; các attributes được serialize bằng cơ chế chuẩn. `created_at` ghi/đọc theo UTC, response dùng `Instant`. Cookie tên `SESSION`, HttpOnly, SameSite=Lax trên local; phiên hết hạn sau 30 phút không hoạt động và được Spring Session dọn phiên hết hạn. Không dùng session trong localStorage. Schema bóng đá `schema.sql` không thay đổi.

Các GET tra cứu clubs/players/matches/standings và đội hình tiêu biểu tiếp tục công khai. Chỉ endpoint kiểm tra đội Fantasy hiện có `/api/fantasy/2026/validate` được miễn CSRF vì chỉ đọc dữ liệu và không ghi đội người dùng; đây không phải ngoại lệ cho các API ghi ở bước sau. Những API riêng được thêm sau phải có rule xác thực/quyền rõ ràng; cấu hình hiện tại mặc định từ chối endpoint khác.

## Kiểm tra đã chạy

Backend test/build bằng một lệnh scoped từ `backend/`:

```powershell
mvn.cmd '-Dtest=AuthIntegrationTest,ApiCorsConfigurationTest,FantasyControllerTest,ClubControllerTest,PlayerControllerTest,MatchControllerTest,StandingControllerTest,TeamOfWeekControllerTest' '-Dspring.datasource.url=jdbc:h2:mem:auth-build;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1' '-Dspring.datasource.username=sa' '-Dspring.datasource.password=' package
```

49 test qua, gồm 7 test tích hợp auth gửi HTTP thật đến server RANDOM_PORT và H2 memory riêng. Kiểm tra email chuẩn hóa/trùng, chỉ USER dù request gửi ADMIN, hash BCrypt và response không lộ credentials, sai mật khẩu/email có cùng lỗi, validation/JSON lỗi không lộ password, đổi phiên/CSRF, replay phiên cũ bị từ chối, logout xóa phiên trong JDBC, API me 401 khi chưa đăng nhập, GET công khai cho cả hai mùa, Fantasy validate công khai và CORS auth chỉ nhận origin cụ thể. Lần chạy đầu phát hiện thiếu starter Security Test của Boot 4 trong MVC slice và mock ClubStatisticsService của test CORS cũ; đã sửa rồi chạy lại cùng phạm vi để xác nhận. Không chạy full suite.

Frontend từ `frontend/`:

```powershell
node --test src/api/auth.test.js src/api/fantasy.test.js src/fantasy/lineup.test.js src/api/request.test.js
npm.cmd run build
```

15 test qua; build Vite thành công. Mỗi phần frontend chạy một lượt. JDK chạy Maven trên máy kiểm tra là 26, POM vẫn biên dịch release 21.

Trình duyệt Chrome thật gọi backend JAR/H2 file riêng `backend/target/auth-browser-20261004`, không mock API auth: thử ở **1440×1000** và **390×844**. Đăng ký → đăng nhập → reload → đăng xuất thành công; kiểm tra xác nhận mật khẩu lệch, sai mật khẩu, đăng ký trùng, cookie HttpOnly/SameSite, API me 401 sau logout và tra cứu công khai 200. Không có lỗi JavaScript hoặc phần tử popup tràn viewport. Giá trị Fantasy thử nghiệm trong localStorage giữ nguyên xuyên suốt; không xuất hiện khóa lưu credentials. Ảnh và kết quả kiểm tra nằm trong `backend/target/auth-check/`, không commit. H2 thử không có toàn bộ roster production; việc này không phải kiểm kê hay xác minh lại dữ liệu bóng đá.

## Vercel–Railway và việc còn lại trước phát hành

Local đã kiểm tra; **chưa triển khai hoặc ghi MySQL production**. Cấu hình `prod` bật cookie Secure và tắt tự chạy SQL lúc khởi động (`spring.sql.init.mode=never`). Phải chuẩn bị migration MySQL cho các bảng account/session/identity mới, review/backup ngoài Git và được giao nhập SQL trước khi dùng bản này trên Railway. DDL local có `CREATE INDEX IF NOT EXISTS`, không chạy nguyên file đó trên MySQL; chưa kiểm thử migration MySQL. Cần giữ kết nối MySQL ở UTC khi nhập dữ liệu tài khoản, kiểm tra lưu phiên trên DB và cookie qua HTTPS sau phát hành.

Vercel `*.vercel.app` và Railway `*.railway.app` là khác site. **Cookie SameSite=Lax hiện tại không phù hợp cho fetch đăng nhập giữa hai site đó**. Chốt một trong các cách trước phát hành:

- Ưu tiên proxy `/api` cùng origin, hoặc domain riêng cho frontend/backend thuộc cùng site; xác minh rewrite/proxy có chuyển đúng Set-Cookie, cookie path/domain và request cookie.
- Nếu tiếp tục khác site: đặt `server.servlet.session.cookie.same-site=none`, giữ `server.servlet.session.cookie.secure=true`, HTTPS; auth fetch đã dùng `credentials: 'include'`. `PREMIERHUB_CORS_ALLOWED_ORIGINS` phải là origin frontend chính xác và CORS auth đã `allowCredentials(true)`, không dùng `*`. Vẫn giữ CSRF. Chính sách chặn cookie bên thứ ba của trình duyệt có thể khiến cách này không hoạt động; cần thử trên deployment thật, không coi SameSite=None là bảo đảm.

Chưa xác minh domain/rewrite Railway–Vercel thực tế hoặc cấu hình forwarded headers/trusted proxy; chưa có giới hạn tốc độ đăng nhập/đăng ký. Những việc này cần hoàn thiện và thử trước phát hành công khai. Google chỉ bật khi backend có cấu hình; khôi phục mật khẩu chưa có trên UI.

## Google dùng cùng tài khoản: cập nhật tiến độ

Đã thêm OAuth2/OIDC client chuẩn của Spring Security theo BOM hiện có, dùng cùng session/me/logout. Google mới tạo USER hoặc dùng subject đã liên kết; email trùng yêu cầu đăng nhập account cũ và xác nhận liên kết, không tự gộp. LINK giữ account hiện có và chỉ ghi liên kết sau POST có CSRF và proof Google trong phiên.

Chi tiết API, Console, bốn biến env, callback local trực tiếp backend 8080 và khác biệt production ở [google-auth-2026.md](google-auth-2026.md). 24 test scoped backend và 18 test frontend đã qua ở lượt Google; provider OIDC thử có ký JWT, state/nonce và token/userinfo HTTP. Browser desktop/390px thử lại email, Google disabled và thông báo callback bằng query mẫu. **Chưa thử Google thật** vì chưa có credentials; bước tiếp theo là bạn cấu hình Console/env và thử flow thực tế trên H2. Khôi phục mật khẩu và rate limiting vẫn chưa làm.

## File lượt email ban đầu (đã bàn giao)

Danh sách dưới là lượt email trước. File cần commit **lượt Google hiện tại** xem [google-auth-2026.md](google-auth-2026.md).

- `backend/pom.xml`
- `backend/src/main/java/com/premierhub/accounts/Account.java`
- `backend/src/main/java/com/premierhub/accounts/AccountResponse.java`
- `backend/src/main/java/com/premierhub/accounts/AccountRepository.java`
- `backend/src/main/java/com/premierhub/accounts/AccountService.java`
- `backend/src/main/java/com/premierhub/accounts/AccountInputException.java`
- `backend/src/main/java/com/premierhub/accounts/DuplicateEmailException.java`
- `backend/src/main/java/com/premierhub/accounts/AuthController.java`
- `backend/src/main/java/com/premierhub/accounts/AuthExceptionHandler.java`
- `backend/src/main/java/com/premierhub/config/SecurityConfiguration.java`
- `backend/src/main/java/com/premierhub/config/ApiCorsConfiguration.java`
- `backend/src/main/resources/auth-schema.sql`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-auth-local.properties`
- `backend/src/main/resources/application-prod.properties`
- `backend/src/test/java/com/premierhub/accounts/AuthIntegrationTest.java`
- `backend/src/test/java/com/premierhub/config/ApiCorsConfigurationTest.java`
- `backend/src/test/java/com/premierhub/web/FantasyControllerTest.java`
- `frontend/src/App.jsx`
- `frontend/src/components/AccountMenu.jsx`
- `frontend/src/components/AccountMenu.css`
- `frontend/src/api/auth.js`
- `frontend/src/api/auth.test.js`
- `docs/auth-local-2026.md`
- `docs/fantasy-multiplayer-2026-plan.md`

Commit message đề xuất: `feat: add local PrismaXI email accounts with JDBC sessions and CSRF`.

## Tài liệu framework đã đối chiếu

- [Spring Security — session authentication and explicit context saving](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).
- [Spring Security — CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
- [Spring Session JDBC](https://docs.spring.io/spring-session/reference/configuration/jdbc.html).
- [Spring Boot — Spring Session](https://docs.spring.io/spring-boot/reference/web/spring-session.html).
- [Spring Boot 4 modular starters, including Security Test](https://spring.io/blog/2025/10/28/modularizing-spring-boot/).

# Tài khoản PrismaXI — email/mật khẩu local

## Phạm vi đã hoàn thành

Bước 1 của [kế hoạch Fantasy 2026/27](fantasy-multiplayer-2026-plan.md): đăng ký, đăng nhập, đăng xuất và xem tài khoản của chính mình. Không yêu cầu xác thực email trước khi chơi. Email, Google OIDC và liên kết cùng tài khoản đã được kiểm chứng trên local; [hướng dẫn Google](google-auth-2026.md) ghi cấu hình và phần deployment còn thiếu. Đã bổ sung giới hạn tần suất xác thực dưới đây. Khôi phục mật khẩu, lưu đội dự thi, chốt đội, chấm điểm và BXH người chơi chưa triển khai. Tài khoản độc lập với `player_id` và dữ liệu mùa bóng; không thay luật Fantasy hay dữ liệu 2024/25.

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

Chưa xác minh domain/rewrite Railway–Vercel thực tế. Giới hạn xác thực local đã được bổ sung bên dưới; danh sách proxy tin cậy và giới hạn khi chạy nhiều instance vẫn cần chốt trước phát hành. Google chỉ bật khi backend có cấu hình; khôi phục mật khẩu chưa có trên UI.

## Google dùng cùng tài khoản: cập nhật tiến độ

Đã thêm OAuth2/OIDC client chuẩn của Spring Security theo BOM hiện có, dùng cùng session/me/logout. Google mới tạo USER hoặc dùng subject đã liên kết; email trùng yêu cầu đăng nhập account cũ và xác nhận liên kết, không tự gộp. LINK giữ account hiện có và chỉ ghi liên kết sau POST có CSRF và proof Google trong phiên.

Chi tiết API, Console, bốn biến env, callback local trực tiếp backend 8080 và khác biệt production ở [google-auth-2026.md](google-auth-2026.md). 24 test scoped backend và 18 test frontend đã qua ở lượt Google; provider OIDC thử có ký JWT, state/nonce và token/userinfo HTTP. Sau đó người dùng đã thử Google thật trên local: reload/đăng nhập lại giữ ID, logout trả AUTH_REQUIRED và liên kết email giữ cùng tài khoản. Đối chiếu H2 có 3 tài khoản/3 danh tính riêng, không trùng email/provider-subject; tài khoản email ID 3 đã liên kết Google và có phiên còn hạn. Không chạy lại toàn bộ kiểm tra Google thật trong lượt giới hạn tần suất. Khôi phục mật khẩu chưa làm.

## Giới hạn tần suất xác thực — local, 04/10/2026

Không thêm dependency/dịch vụ hoặc bảng SQL. `AuthRateLimiter` giữ các bộ đếm trong bộ nhớ **từng JVM**; một cửa sổ bắt đầu từ yêu cầu được chấp nhận đầu tiên. Cả thao tác thành công và thất bại đều tính một lượt, sau CSRF và validation, trước truy vấn tài khoản/hash mật khẩu hoặc tạo luồng Google. Chỉ đếm request hợp lệ vào controller; JSON/field không hợp lệ và CSRF sai được xử lý bằng cơ chế hiện có. Đây không phải giới hạn mọi traffic/DoS ở tầng mạng.

| Thao tác/phạm vi | Mặc định | Biến môi trường ngưỡng | Biến môi trường cửa sổ |
| --- | --- | --- | --- |
| POST `/api/auth/login`, theo IP | 20 lượt/5 phút | `PREMIERHUB_AUTH_LOGIN_IP_ATTEMPTS` | `PREMIERHUB_AUTH_LOGIN_IP_WINDOW` |
| Cùng endpoint, theo email strip/lowercase ROOT | 5 lượt/5 phút | `PREMIERHUB_AUTH_LOGIN_EMAIL_ATTEMPTS` | `PREMIERHUB_AUTH_LOGIN_EMAIL_WINDOW` |
| POST `/api/auth/register`, theo IP | 5 lượt/15 phút | `PREMIERHUB_AUTH_REGISTER_IP_ATTEMPTS` | `PREMIERHUB_AUTH_REGISTER_IP_WINDOW` |
| POST `/api/auth/google/start`, theo IP, chung LOGIN/LINK | 10 lượt/5 phút | `PREMIERHUB_AUTH_GOOGLE_IP_ATTEMPTS` | `PREMIERHUB_AUTH_GOOGLE_IP_WINDOW` |

Ngưỡng là số nguyên dương; cửa sổ dùng cú pháp Duration của Spring Boot, ví dụ `30s`, `5m`, `15m`, từ 1 giây tới 7 ngày. Có thể dùng các property tương ứng `premierhub.auth-rate-limit.login-ip.attempts/window`, `login-email.attempts/window`, `registration-ip.attempts/window`, `google-ip.attempts/window`. Đồng hồ bộ đếm dùng `System.nanoTime` để không bị chỉnh đồng hồ lịch làm thay đổi thời gian chờ.

Đăng nhập phải còn lượt ở **cả hai** bộ đếm. Đổi email không né được giới hạn IP; đổi IP không né được giới hạn email đã chuẩn hóa. Bộ đếm không phụ thuộc email có tồn tại hay không và phản hồi 429 không nêu email/bộ đếm nào bị chặn. IP và email khác nhau không chia sẻ bộ đếm; người dùng chung IP/NAT vẫn chia sẻ hạn mức IP theo thiết kế. Không khóa tài khoản trong database, không gia hạn cửa sổ vì request bị chặn, không xóa lượt sau login thành công; hết cửa sổ tự thử lại được.

Vượt ngưỡng trả HTTP **429**, `Retry-After: <số giây nguyên làm tròn lên>` và JSON `code: AUTH_RATE_LIMITED`, `message` tiếng Việt, `retryAfterSeconds`. Nếu nhiều bộ đếm chặn, lấy thời gian chờ dài nhất. `Cache-Control: no-store`; CORS auth expose `Retry-After`. Frontend ưu tiên header, hỗ trợ số giây hoặc HTTP date, fallback số giây JSON rồi thông báo chờ chung. Các form email và nút Google dùng chung xử lý lỗi, hiện trong `role=alert`; không tự gửi lại request hay lưu credentials. Thời gian hiển thị là tại lúc nhận phản hồi, chưa có đếm ngược trực tiếp.

`PREMIERHUB_AUTH_LIMIT_MAX_ENTRIES` mặc định **10000** (2..1000000) tính chung các khóa IP/email/thao tác. `PREMIERHUB_AUTH_LIMIT_CLEANUP_INTERVAL` mặc định **60s** (1s..1d). Một thread daemon dọn khóa hết hạn định kỳ và đóng khi ứng dụng dừng; cũng dọn trước khi kiểm tra dung lượng. Kiểm tra/tăng bộ đếm có đồng bộ, không vượt ngưỡng khi request chạy song song. Khi hết dung lượng, không xóa khóa đang hiệu lực: thao tác cần khóa mới trả 429 đến mốc hết hạn sớm nhất; thao tác có sẵn khóa vẫn theo hạn mức của nó. Một đợt nhiều khóa mới có thể tạm chặn người dùng khác, cần theo dõi và điều chỉnh dung lượng khi phát hành.

**Khởi động lại:** bộ đếm bị xóa, nhưng tài khoản/liên kết và phiên JDBC vẫn được lưu riêng. **Nhiều instance:** mỗi JVM có hạn mức riêng, không phải giới hạn toàn cụm; tổng số lượt thực tế có thể tăng theo số instance. Bản này phù hợp chạy một instance. Trước khi mở rộng, cần bộ đếm chung atomically trên hạ tầng/database hiện có hoặc giới hạn ở proxy đã xác minh; chưa bổ sung Redis/dịch vụ trả phí hoặc thay đổi production trong lượt này.

### IP và proxy tin cậy

Mặc định `server.forward-headers-strategy=none` và `PREMIERHUB_AUTH_TRUSTED_PROXIES` rỗng: dùng IP socket từ `getRemoteAddr()`, bỏ qua mọi `X-Forwarded-For`/`Forwarded`. Chỉ cấu hình IP/CIDR thực tế của các proxy do mình kiểm soát, phân cách bằng dấu phẩy; không điền tên miền hoặc `/0`. Ví dụ minh họa `10.0.0.2/32,2001:db8:1::/48` **không phải** dải Railway đã xác minh.

Nếu socket peer thuộc danh sách tin cậy, resolver ghép các header X-Forwarded-For và đi từ phải sang trái qua các proxy tin cậy, dừng ở hop không tin cậy đầu tiên. Proxy phải xóa header client tự khai rồi ghi IP socket, hoặc nối IP socket vào cuối chuỗi; không được chuyển nguyên header client mà không thêm IP thật. Địa chỉ chỉ nhận IP literal IPv4/IPv6, chuẩn hóa trước khi dùng khóa; không DNS lookup cho hostname. Chuỗi sai, quá 2048 ký tự hoặc 32 hop dùng lại peer IP. `Forwarded` không được dùng để xác định IP.

Resolver từ chối khởi động nếu bật global forwarding `native/framework`: các cơ chế đó có thể thay socket peer trước khi kiểm tra trust. **Không áp dụng hướng dẫn bật `SERVER_FORWARD_HEADERS_STRATEGY=framework` cũ để phát hành bản này.** Scheme/host callback HTTPS sau Railway proxy cần được kiểm tra và thiết kế cùng chính sách proxy tin cậy trước phát hành; chưa xác minh CIDR/forwarding Railway hoặc thay cấu hình production. Local callback trực tiếp 8080 vẫn hoạt động.

Không ghi password, cookie, OAuth token, credentials, email hoặc IP vào log giới hạn. Logout, GET `/me`, CSRF, callback/confirm/cancel Google không dùng bộ đếm mới; bảo vệ session/CSRF/state/nonce/xác nhận liên kết giữ nguyên.

### Kiểm tra và file của lượt giới hạn

Chạy từ `backend/`, không `clean` để giữ H2 local của người dùng:

```powershell
mvn.cmd '-Dtest=AuthRateLimiterTest,AuthClientIpResolverTest,AuthRateLimitIntegrationTest' '-Dspring.datasource.url=jdbc:h2:mem:auth-rate-build;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1' '-Dspring.datasource.username=sa' '-Dspring.datasource.password=' package
```

Frontend từ `frontend/`: `node --test src/api/auth.test.js`, rồi `npm.cmd run build`. Test dùng H2 memory riêng và Google disabled; clock giả kiểm tra hết hạn mà không ngủ. Không đọc/ghi H2 tài khoản người dùng hoặc MySQL production bằng test, không chạy lại Google thật.

Kết quả: **17 test backend qua** (7 limiter, 5 IP/proxy, 5 HTTP integration); Maven package thành công. Lần đầu dừng ở lỗi biên dịch `IntStream.map` trong test đồng thời; đã sửa thành `mapToObj` và chạy lại đúng phạm vi. **6 test frontend qua**, Vite build thành công một lượt. Backend kiểm tra vượt ngưỡng/IP/email chuẩn hóa, hết hạn, người dùng khác, nhiều request song song, dung lượng/cleanup, CSRF, CORS Retry-After, phiên hiện tại/logout và API công khai.

Chrome **1440px và 390px**: 8 ca UI cho email login/register, Google LOGIN/LINK đều hiển thị “thử lại sau 42 giây”, không tràn ngang hoặc lỗi JavaScript; marker localStorage giữ nguyên. Chỉ mock các phản hồi auth 429/status/CSRF trong một profile Chrome cô lập để kiểm tra giao diện; HTTP integration bên trên dùng limiter thật/H2 thật. Không thao tác màn hình Google hoặc tạo tài khoản thật trong kiểm tra UI. Chrome lần khởi chạy đầu lỗi GPU trong môi trường kiểm tra; lần chạy headless không dùng GPU hoàn tất. Kết quả/ảnh ở `backend/target/auth-rate-check/`, không commit. Backend auth-local và frontend local đã chạy lại bản mới tại localhost:8080/5173.

File cần commit:

- `backend/src/main/java/com/premierhub/accounts/`: `AuthRateLimitSettings.java`, `AuthRateLimitException.java`, `AuthRateLimiter.java`, `AuthClientIpResolver.java`, `AuthController.java`, `GoogleAuthController.java`, `AuthExceptionHandler.java`.
- `backend/src/main/java/com/premierhub/config/ApiCorsConfiguration.java`, `backend/src/main/resources/application.properties`.
- `backend/src/test/java/com/premierhub/accounts/`: `AuthRateLimiterTest.java`, `AuthClientIpResolverTest.java`, `AuthRateLimitIntegrationTest.java`.
- `frontend/src/api/auth.js`, `frontend/src/api/auth.test.js`.
- `docs/auth-local-2026.md`, `docs/google-auth-2026.md`, `docs/fantasy-multiplayer-2026-plan.md`.

Commit message: `feat(auth): rate limit registration and sign-in attempts`. Chưa commit/push/deploy; không migration SQL cho bộ giới hạn. Khôi phục mật khẩu làm trong lượt riêng.

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

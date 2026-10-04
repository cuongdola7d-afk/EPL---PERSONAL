# Phát hành tài khoản PrismaXI qua Vercel–Railway

## Checkpoint production hiện tại

Người dùng đã push main và cho phép phát hành sau lượt chuẩn bị. Migration đã hoàn tất trên MySQL Railway 9.7.2 sau backup mới được kiểm tra. Vercel và Railway **đã phát hành thành công cùng commit `446f044`**; cookie/CSRF/proxy và các trang công khai đã kiểm chứng trên production. **Google thật đã được người dùng xác nhận và đối chiếu MySQL: 2 account USER, 2 identity Google riêng, không trùng email/provider-subject; phiên được lưu đúng account.** Hiện chưa có account email–mật khẩu/ca LINK từ email để đối chiếu riêng trên production; kiểm chứng local vẫn giữ nguyên. Xem [biên bản và checkpoint hiện tại](auth-production-2026-10-04.md); không áp dụng lại phần “chưa ghi production” của biên bản bàn giao local bên dưới làm trạng thái hiện tại.

## Trạng thái bàn giao, 04/10/2026

Đã chuẩn bị mã, cấu hình và migration, **chưa sửa cấu hình dịch vụ, ghi SQL production hoặc deploy**. Email/Google thật/LINK/rate limit đã được người dùng kiểm chứng local trước lượt này, không chạy lại toàn bộ. Khôi phục mật khẩu và Fantasy mới không thuộc phạm vi.

Đích phát hành:

- Frontend: `https://premierhub.vercel.app`.
- Railway upstream: `https://epl-personal-production.up.railway.app`.
- Auth public origin: **frontend**, không phải Railway.
- Google callback production: **`https://premierhub.vercel.app/api/auth/google/callback`**.

## Đường truy cập và cookie

`frontend/vercel.json` đặt route `/api/(.*)` trước filesystem/SPA fallback, proxy nguyên path/query sang Railway `/api/$1`. Route thêm **request header** `x-prismaxi-proxy-secret` bằng transform `set` từ biến môi trường riêng của Vercel; ghi đè header khách tự gửi. Không có Vercel Function/framework/dependency/dịch vụ trả phí mới. Root Directory của project phải là **`frontend`** để dùng đúng cấu hình này. Backend URL là đích cố định trong file, không lấy URL upstream từ request.

`auth.js` luôn gọi URL tương đối `/api/auth/...`, kể cả authorize Google. Register/login, CSRF, `/me`, logout, Google status/start/authorize/callback/confirm/cancel vì vậy cùng origin/cookie. `request.js` cũng mặc định dùng cùng origin nếu không đặt VITE_API_BASE_URL; URL cũ chỉ còn tùy chọn cho các API bóng đá công khai, không ảnh hưởng auth. Khi phát hành, bỏ VITE_API_BASE_URL ở Vercel để toàn bộ API đi qua route mới, rồi build lại.

Profile `prod` bật cookie **SESSION, Secure, HttpOnly, SameSite=Lax, Path=/**; **không có Domain**. Browser nhận Set-Cookie từ frontend origin và giữ cookie host-only ở `premierhub.vercel.app`. Không đặt Domain thành Railway, `.vercel.app` hoặc domain chung. Proxy giữ Cookie và từng Set-Cookie, kể cả xóa cookie lúc logout. Backend không đổi cookie name/CSRF/session JDBC đã có. Lax cho phép cookie trên callback Google GET dạng điều hướng trang cấp cao; không cần SameSite=None/cookie bên thứ ba. Profile auth-local vẫn HTTP với Secure=false và callback trực tiếp localhost:8080; Vite proxy/local Google đã đạt tiếp tục dùng được.

`AuthProxyFilter` chạy trước security context/OAuth trong SecurityFilterChain, không đăng ký hai lần ở servlet. Mọi `/api/auth/**` production phải có secret đúng và một IP literal từ `X-Vercel-Forwarded-For`; thiếu/sai/trùng header bị trả 403 `AUTH_PROXY_REQUIRED`, không tạo luồng auth. GET bóng đá công khai và actuator health trực tiếp Railway giữ nguyên. Filter đặt `Cache-Control`, `CDN-Cache-Control`, `Vercel-CDN-Cache-Control` no-store cho auth; route Vercel cũng tắt caching API. Không cache response có phiên/CSRF hoặc Location Google.

CSRF vẫn bắt buộc với POST auth, bao gồm logout/LINK confirmation; callback Google vẫn dùng state/nonce/PKCE/session và return route nội bộ đã kiểm tra. Không nhận origin/return URL từ Host/header khách. Google frontend origin và callback phải khớp public origin; cấu hình lệch bị từ chối lúc khởi động. Không thay lựa chọn Fantasy/localStorage hoặc chuyển dữ liệu H2 sang MySQL.

## HTTPS, IP và rate limit

Giữ **`server.forward-headers-strategy=none`**, không bật `SERVER_FORWARD_HEADERS_STRATEGY=framework/native`. Sau khi so secret bằng timing-safe comparison, filter cung cấp scheme HTTPS/server name/port/request URL từ **PREMIERHUB_AUTH_PUBLIC_ORIGIN cố định** cho Spring OAuth. Header Forwarded/X-Forwarded-* không quyết định scheme/host và được che khỏi các filter/controller phía sau. Secret cũng được che; không ghi header/body/credential/token vào log.

Vercel có tài liệu rằng header visitor IP được tạo ở edge và X-Forwarded-For khách gửi bị ghi đè. Backend dùng **X-Vercel-Forwarded-For**, chỉ sau khi có proof secret, chuẩn hóa IPv4/IPv6 và lưu bằng servlet attribute riêng cho AuthClientIpResolver. Không tin X-Forwarded-For do Railway hoặc client gửi và không cần đoán CIDR của mạng nội bộ Railway. Các yêu cầu đi thẳng vào Railway không có secret không được dùng auth, dù tự khai các header giống Vercel. Secret phải được giữ riêng ở hai dịch vụ, upstream Vercel→Railway dùng HTTPS. Đây là ranh giới tin cậy của proxy, không phải danh tính người dùng và không thay cookie/CSRF.

**Cần kiểm tra khi deploy thật:** Railway phải chuyển tiếp nguyên header secret và X-Vercel-Forwarded-For do Vercel tạo. Nếu header thiếu hoặc bị đổi thành một chuỗi IP, backend từ chối an toàn; không bật trust toàn internet để khắc phục. Chỉ sửa chính sách sau khi xác minh chuỗi proxy. CDN/proxy khác đặt trước Vercel có thể làm IP phản ánh proxy đó; chưa hỗ trợ cấu hình Trusted Proxy Enterprise của Vercel.

Giai đoạn thử nghiệm dùng **một replica/instance Railway**, không scale nhiều instance. Rate limit vẫn bounded/in-memory từng JVM, cleanup định kỳ và không gia hạn vì lượt bị chặn. Restart/redeploy làm mất bộ đếm; session/tài khoản ở MySQL vẫn tách biệt. Nhiều instance không chia sẻ bộ đếm, có thể tăng tổng lượt theo số instance; chưa có giới hạn toàn cụm. Người dùng chung NAT chia sẻ hạn mức IP. Dung lượng đầy có thể tạm chặn khóa mới đến lúc có khóa hết hạn. Giữ mặc định ban đầu và theo dõi 429; các biến chỉnh ngưỡng trong auth-local-2026.md. Chưa thêm Redis hay dịch vụ trả phí.

## Biến cần cấu hình sau khi được giao phát hành

### Railway — server, không dùng tiền tố VITE_

| Biến | Giá trị/yêu cầu |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `PREMIERHUB_JDBC_URL` | Giữ đúng database MySQL hiện có; không đổi sang H2. Xác minh Connector/J connectionTimeZone=UTC và forceConnectionTimeZoneToSession=true, cấu hình TLS theo kết nối Railway thực tế. Không chép URL localhost của kiểm tra. |
| `PREMIERHUB_DB_USER`, `PREMIERHUB_DB_PASSWORD` | Credentials runtime hiện có từ môi trường; cần SELECT/INSERT/UPDATE/DELETE cho auth/session và quyền bóng đá hiện có. Không cấp DDL runtime để thay migration. |
| `JAVA_TOOL_OPTIONS` | Giữ options hiện có, bảo đảm `-Duser.timezone=UTC` để các Timestamp/DATETIME tài khoản được ghi/đọc đúng UTC. Không thêm secret vào Java options. |
| `PREMIERHUB_AUTH_PUBLIC_ORIGIN` | `https://premierhub.vercel.app` |
| `PREMIERHUB_AUTH_PROXY_SECRET` | Secret ngẫu nhiên **32 byte dạng 64 ký tự hex**, giống Vercel. Không dùng giá trị test hoặc đưa vào Git/chat/log. |
| `PREMIERHUB_CORS_ALLOWED_ORIGINS` | `https://premierhub.vercel.app` |
| `PREMIERHUB_GOOGLE_FRONTEND_ORIGIN` | `https://premierhub.vercel.app` |
| `PREMIERHUB_GOOGLE_CALLBACK_URI` | `https://premierhub.vercel.app/api/auth/google/callback` |
| `PREMIERHUB_GOOGLE_CLIENT_ID`, `PREMIERHUB_GOOGLE_CLIENT_SECRET` | Credentials Google Web application ở môi trường backend. Không chuyển secret sang frontend. |
| `SERVER_FORWARD_HEADERS_STRATEGY` | Bỏ biến ghi đè, hoặc `none` |
| Cookie overrides | Bỏ Domain override; giữ Secure=true/HttpOnly=true/Path=/SameSite=Lax. Settings từ chối cấu hình không phù hợp. |
| `PREMIERHUB_AUTH_TRUSTED_PROXIES` | Để rỗng; auth production dùng proof proxy, không dùng CIDR/X-Forwarded-For cũ. |

`spring.sql.init.mode=never` và `spring.session.jdbc.initialize-schema=never` giữ nguyên: startup không migration/khởi tạo/reset bảng. PORT do Railway cấp vẫn dùng cấu hình hiện có. Không tạo cron/Pre-deploy Command. Nếu Google chưa cấu hình, email vẫn dùng được; nếu bật Google thì cả frontend origin/callback phải đúng cùng origin trên.

### Vercel

- Root Directory **frontend**, framework Vite, build `npm run build`, output `dist`; file vercel.json đã ghi các mục build/route.
- Thêm **PREMIERHUB_AUTH_PROXY_SECRET**, cùng giá trị Railway, vào environment **Production** hoặc staging tương ứng, không có tiền tố VITE_. File JSON chỉ chứa tên biến, không giá trị.
- Bỏ **VITE_API_BASE_URL** để mọi API dùng proxy frontend. Không đặt Google client secret hoặc DB credentials ở Vercel.
- Không gắn preview không tin cậy vào database production. Staging cần backend/database/secret riêng và public origin/callback của chính staging; destination trong cấu hình staging cũng phải trỏ đúng upstream staging. Chưa bật preview tự ghi production.

### Google Cloud Console

OAuth client loại **Web application**:

- Authorized JavaScript origin nếu khai báo: `https://premierhub.vercel.app`.
- **Authorized redirect URI: `https://premierhub.vercel.app/api/auth/google/callback`**, không thêm `/` cuối, không dùng callback Railway cho bản qua proxy.
- Có thể giữ riêng các URI local đã đạt: origin `http://localhost:5173`, redirect `http://localhost:8080/api/auth/google/callback`.
- Scope vẫn chỉ openid/email/profile. Nếu app External/Testing, thêm người dùng thử vào test users theo cấu hình Console. Chưa thay Console hoặc credentials trong lượt chuẩn bị này.

## Migration MySQL và thứ tự phát hành

File: **backend/sql/2026-10-04-accounts-mysql.sql**, MySQL **8.0.17+**. Chỉ CREATE TABLE IF NOT EXISTS cho accounts, account_identities, SPRING_SESSION, SPRING_SESSION_ATTRIBUTES; không DROP/TRUNCATE/reset, không sửa bảng bóng đá, không INSERT tài khoản H2. Có email unique, role CHECK, provider/subject primary key, FK/index account_id, session ID unique, expiry/principal indexes, attribute primary key và cascade theo phiên. utf8mb4_0900_bin là collation NO PAD/case-sensitive để subject khác case/khoảng trắng không bị tự gộp. DATETIME(6) tài khoản được dùng với UTC; phiên lưu milliseconds.

Trước migration **được giao thực hiện sau**:

1. Xác nhận đúng MySQL host/port/database và `SELECT VERSION(), DATABASE()`; đối chiếu phiên bản/collation trên server thực tế. Không chạy migration với MariaDB hoặc MySQL cũ mà chưa điều chỉnh/review.
2. `SHOW CREATE TABLE` các bảng auth đã có và đối chiếu định nghĩa file. Nếu khác (ví dụ hash NOT NULL, collation/index thiếu, session principal ngắn), dừng và chuẩn bị ALTER nhỏ được review. IF NOT EXISTS không sửa cấu trúc cũ; không coi chạy không lỗi là schema đúng.
3. Backup ngoài Git bằng công cụ MySQL, kiểm tra backup dùng được. DDL MySQL implicit commit; không có rollback transaction cho migration. Không drop bảng để rollback lỗi: sửa tiếp sau review hoặc phục hồi backup theo phạm vi đã chốt.
4. Tạm giữ tính năng auth chưa phát hành; nhập SQL một lần, chạy lại và xác nhận dữ liệu/index/constraints không đổi. Không nhập H2 accounts/identities/sessions.
5. Cấu hình env/cookie/callback và **một replica**; publish backend khi được phép. GET bóng đá trực tiếp vẫn dùng được, auth trực tiếp Railway sẽ trả 403 theo thiết kế.
6. Publish frontend có route/env secret khi được phép; kiểm tra toàn luồng qua domain frontend. Khoảng chuyển đổi hai deploy có thể tạm trả AUTH_PROXY_REQUIRED; phối hợp thứ tự, không mở direct auth hoặc tắt CSRF để né lỗi.

Lệnh CLI dưới dùng MySQL có sẵn trên máy, chạy từ gốc repo **chỉ khi có quyền nhập SQL/phát hành**. MYSQL_HOST/PORT/DATABASE/USER phải được xác nhận riêng cho đúng đích; MYSQL_PWD chỉ nạp bí mật từ môi trường, không đặt password vào argv:

```powershell
$mysqlClient = 'C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe'
$mysqlDump = 'C:\Program Files\MySQL\MySQL Server 9.6\bin\mysqldump.exe'
foreach ($requiredName in @('MYSQL_HOST', 'MYSQL_PORT', 'MYSQL_DATABASE', 'MYSQL_USER', 'MYSQL_BACKUP_PATH', 'PREMIERHUB_DB_PASSWORD')) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($requiredName))) {
        throw "Missing environment variable: $requiredName"
    }
}
$oldMysqlPassword = $env:MYSQL_PWD
$env:MYSQL_PWD = $env:PREMIERHUB_DB_PASSWORD
try {
    & $mysqlClient --no-defaults --protocol=TCP --host=$env:MYSQL_HOST --port=$env:MYSQL_PORT --user=$env:MYSQL_USER --database=$env:MYSQL_DATABASE --execute='SELECT VERSION(), DATABASE();'
    if ($LASTEXITCODE -ne 0) { throw 'Preflight failed; stop migration.' }
    # Confirm schemas/backup destination outside Git before continuing.
    & $mysqlDump --no-defaults --protocol=TCP --host=$env:MYSQL_HOST --port=$env:MYSQL_PORT --user=$env:MYSQL_USER --single-transaction --no-tablespaces --databases $env:MYSQL_DATABASE --result-file=$env:MYSQL_BACKUP_PATH
    if ($LASTEXITCODE -ne 0) { throw 'Backup failed; stop migration.' }
    & $mysqlClient --no-defaults --protocol=TCP --host=$env:MYSQL_HOST --port=$env:MYSQL_PORT --user=$env:MYSQL_USER --database=$env:MYSQL_DATABASE --execute='SOURCE backend/sql/2026-10-04-accounts-mysql.sql'
    if ($LASTEXITCODE -ne 0) { throw 'Migration failed; inspect schema before retrying.' }
} finally { $env:MYSQL_PWD = $oldMysqlPassword }
```

Account runtime có thể không có quyền dump/DDL; khi đó dùng credentials migration được cấp riêng từ môi trường. Không in chúng. Cấu hình TLS/CA cho CLI theo MySQL đích được xác minh trước khi chạy; ví dụ localhost không TLS trong test không phải cấu hình production. Lệnh SOURCE bảo toàn nội dung UTF-8/newline file; không ghép lệnh SQL bằng chuỗi password. Phiên bản client 9.6 là của máy kiểm tra, không phải đã xác minh Railway dùng 9.6.

## Kiểm tra đã thực hiện và còn thiếu

- **13 test backend** qua: 5 proxy filter/settings, 3 HTTP integration, 5 resolver IP; Maven package thành công. H2 memory cô lập kiểm tra proof giả/thiếu/trùng, trusted HTTPS/origin, cookie, CSRF/session/logout và callback LOGIN/LINK hủy; không dùng Google thật. Lệnh từ backend:

```powershell
mvn.cmd '-Dtest=AuthProxyFilterTest,AuthProxyIntegrationTest,AuthClientIpResolverTest' '-Dspring.datasource.url=jdbc:h2:mem:auth-release-build;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1' '-Dspring.datasource.username=sa' '-Dspring.datasource.password=' '-Ddebug=false' '-Dlogging.level.root=INFO' package
```

- **10 test frontend** qua: `node --test src/api/auth.test.js src/api/request.test.js src/api/deployment.test.js`, Vite build thành công một lượt. Kiểm tra same-origin/default URL, auth/Google authorize tương đối, route trước SPA, env-backed request secret và no-store. Không validate/deploy cấu hình bằng dịch vụ Vercel thật trong lượt này.
- **MySQL 9.6.0 thực tế, local riêng**: khởi tạo datadir trong target/auth-release-check/mysql, bind 127.0.0.1:33017, không dùng service/database hiện có. Migration chạy hai lần, giữ account/identity fixture và bảng sentinel ngoài phạm vi; unique/FK/role CHECK/session attribute cascade được kiểm chứng. Case và trailing-space của subject khác nhau được giữ riêng. Không coi đây là kiểm chứng phiên bản/schema MySQL Railway.
- **Chrome HTTPS qua proxy local và profile prod kết nối MySQL riêng**: cookie Secure/HttpOnly/Lax/Path=/host localhost, đổi session ID, JS không đọc cookie, CSRF sai bị 403, reload giữ ID, logout xóa cookie và `/me` 401; createdAt đúng UTC và marker localStorage giữ nguyên. LOGIN/LINK đi qua authorize và callback trên https://localhost:5443, hủy provider quay về đúng query/hash; LINK giữ account. Google request được chặn trước mạng bằng mô phỏng hủy, không thử lại consent/token Google thật. Proxy mô phỏng route/header transforms, **không phải CDN Vercel/Railway thật**.
- Lần chạy Maven đầu bị PowerShell dừng bởi cảnh báo native stderr; đã chạy lại đúng phạm vi với logging INFO. Kiểm tra HTTPS cần chỉnh cấu hình credentials của MySQL test và helper trình duyệt trước khi hoàn tất; không sửa mã ứng dụng thêm sau build. Log/kết quả/helpers nằm ở backend/target/auth-release-check, được ignore.

**Còn phải thử trên deployment thật sau khi được phép:** Vercel chấp nhận route/env transform; header visitor IP/proof đi qua Railway; Set-Cookie/Cookie không bị bỏ hoặc cache; Google login thật và LINK confirmation dùng callback frontend; logout/reload/CSRF/429; session sau restart backend trên cùng MySQL; đúng quyền/phiên bản/collation/schema Railway. Không báo production hoạt động trước khi làm các bước này. Bộ đếm rate limit vẫn mất khi restart và không chia sẻ nhiều instance. Khôi phục mật khẩu vẫn chưa triển khai.

## File cần commit — lượt chuẩn bị phát hành

- backend/src/main/java/com/premierhub/accounts/AuthProxySettings.java
- backend/src/main/java/com/premierhub/accounts/AuthProxyFilter.java
- backend/src/main/java/com/premierhub/accounts/AuthClientIpResolver.java
- backend/src/main/java/com/premierhub/config/SecurityConfiguration.java
- backend/src/main/resources/application-prod.properties
- backend/src/test/java/com/premierhub/accounts/AuthProxyFilterTest.java
- backend/src/test/java/com/premierhub/accounts/AuthProxyIntegrationTest.java
- backend/sql/2026-10-04-accounts-mysql.sql
- frontend/vercel.json
- frontend/src/api/auth.js
- frontend/src/api/request.js
- frontend/src/api/request.test.js
- frontend/src/api/deployment.test.js
- docs/auth-release-2026.md
- docs/auth-local-2026.md
- docs/google-auth-2026.md
- docs/fantasy-multiplayer-2026-plan.md

Commit message đề xuất: **feat(auth): prepare same-origin Vercel proxy and MySQL account deployment**. Không commit/push/deploy, không tạo cron/Pre-deploy Command trong lượt này.

## Tài liệu chính thức đã đối chiếu

- [Vercel rewrites: external proxy, visitor headers, env secret transform và cache](https://vercel.com/docs/routing/rewrites).
- [Vercel static config: routes/transforms](https://vercel.com/docs/project-configuration/vercel-json).
- [Vercel request headers: X-Forwarded-For và X-Vercel-Forwarded-For](https://vercel.com/docs/headers/request-headers).
- [Railway public networking/HTTPS](https://docs.railway.com/networking/public-networking).
- [Spring Session 4.1.1 MySQL schema](https://github.com/spring-projects/spring-session/blob/4.1.1/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-mysql.sql).
- [MySQL 8.0.17: utf8mb4_0900_bin và NO PAD](https://dev.mysql.com/doc/relnotes/mysql/8.0/en/news-8-0-17.html).

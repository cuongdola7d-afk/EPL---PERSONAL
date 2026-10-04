# Google OIDC cho tài khoản PrismaXI — bước 1

## Trạng thái và phạm vi

Đã triển khai code đăng nhập Google và liên kết có xác nhận vào **cùng `accounts.id`**, Spring Security, JDBC session, cookie HttpOnly, CSRF, logout và `/api/auth/me` hiện có. Spring Boot vẫn 4.1.1/Java mục tiêu 21; chỉ thêm `spring-boot-starter-oauth2-client` do BOM quản lý. Các API bóng đá, luật Fantasy, lựa chọn localStorage và dữ liệu mùa 2024/25 không thay đổi.

**Google thật đã được người dùng kiểm chứng trên local:** reload/đăng nhập lại giữ đúng ID, logout trả AUTH_REQUIRED, liên kết tài khoản email giữ cùng ID. Đối chiếu H2 không trùng email/provider-subject, tài khoản email đã có liên kết và phiên còn hạn. Kiểm tra tự động của lượt triển khai trước dùng provider OIDC local có ký JWT/HTTP thật; kiểm tra deployment vẫn chưa làm. Lượt này bổ sung giới hạn khởi tạo LOGIN/LINK theo IP, chi tiết ở [auth-local-2026.md](auth-local-2026.md#giới-hạn-tần-suất-xác-thực--local-04102026). Khôi phục mật khẩu, chốt đội, chấm điểm/BXH và phát hành chưa làm.

## Chính xác cấu hình cần để thử local

Backend đọc bốn biến sau, không đọc credentials từ frontend:

| Biến backend | Giá trị local |
| --- | --- |
| `PREMIERHUB_GOOGLE_CLIENT_ID` | Client ID của OAuth client loại Web application do bạn tạo. |
| `PREMIERHUB_GOOGLE_CLIENT_SECRET` | Client secret của chính client đó; giữ riêng ngoài Git. |
| `PREMIERHUB_GOOGLE_FRONTEND_ORIGIN` | `http://localhost:5173` |
| `PREMIERHUB_GOOGLE_CALLBACK_URI` | `http://localhost:8080/api/auth/google/callback` |

Hai URI có mặc định trên trong `application-auth-local.properties`; có thể đặt env để ghi đè. Ngoài profile local, khi bật Google phải cấu hình hai URI rõ ràng. Origin không có path hoặc dấu `/` cuối. Callback bắt buộc có đúng path `/api/auth/google/callback`, không query/fragment. URL dùng HTTPS, ngoại trừ HTTP localhost/127.0.0.1 để thử local. Nếu thiếu một trong hai credentials, Google bị vô hiệu hóa; email login vẫn chạy và UI ghi rõ chưa cấu hình. Không in credentials hoặc nhập chúng vào chat.

Trong PowerShell riêng cho backend, đặt env trên máy của bạn rồi chạy JAR đã build:

```powershell
$env:PREMIERHUB_GOOGLE_CLIENT_ID = '<client-id-cua-ban>'
$env:PREMIERHUB_GOOGLE_CLIENT_SECRET = [System.Net.NetworkCredential]::new('', (Read-Host 'Google Client Secret' -AsSecureString)).Password
$env:PREMIERHUB_GOOGLE_FRONTEND_ORIGIN = 'http://localhost:5173'
$env:PREMIERHUB_GOOGLE_CALLBACK_URI = 'http://localhost:8080/api/auth/google/callback'
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=auth-local --server.port=8080
```

Lệnh chạy từ `backend/`, chỉ ghi H2 `backend/target/auth-local`, không tải env MySQL production. H2 này mất nếu chạy Maven `clean`. Không dùng profile `prod` để thử ghi tài khoản. Code không tự đọc file `.env`; có thể dùng cách nạp env riêng đã được ignore nhưng không in nội dung/secret ra log.

Frontend chạy từ `frontend/` bằng `npm.cmd run dev -- --port 5173 --strictPort`, mở **`http://localhost:5173`**. Vite proxy `/api` sang 8080; dev không cần `VITE_API_BASE_URL`. Dùng cùng hostname `localhost` cho cả hai cổng: cookie không phân biệt cổng. Callback đi trực tiếp vào backend 8080 để URL mà Spring thấy khớp redirect URI; không đặt callback 5173 qua Vite `changeOrigin:true` hiện có. Nếu đổi hostname/cổng, sửa URI env và đăng ký chính xác callback tương ứng trong Console; bổ sung frontend origin vào `PREMIERHUB_CORS_ALLOWED_ORIGINS` nếu khác các origin local mặc định.

## Google Cloud Console

1. Chọn/tạo project, cấu hình Google Auth Platform (tên ứng dụng, email liên hệ và audience phù hợp). Nếu dùng audience External ở trạng thái Testing, thêm tài khoản Google dùng thử vào danh sách test users. Chỉ yêu cầu các scope danh tính `openid`, `email`, `profile`.
2. Tạo OAuth client loại **Web application**. Với local trên, origin ứng dụng là `http://localhost:5173`; nếu khai báo Authorized JavaScript origins, dùng đúng origin này. Luồng hiện tại là backend OIDC, không dùng Google JavaScript SDK.
3. Trong **Authorized redirect URIs**, thêm chính xác `http://localhost:8080/api/auth/google/callback` (không thêm `/` cuối). Callback không phải `/api/auth/google/start` hoặc URL trang Fantasy.
4. Đặt Client ID/secret vào env backend như trên và khởi động lại backend; không đưa file credentials vào repo. Mở popup tài khoản: nút “Tiếp tục với Google” chỉ hoạt động khi backend báo đã cấu hình.

Hướng dẫn tạo client/redirect dựa trên [Google OAuth cho web server](https://developers.google.com/identity/protocols/oauth2/web-server); cấu hình audience/test users theo [hướng dẫn consent screen](https://developers.google.com/workspace/guides/configure-oauth-consent).

## Luồng đăng nhập và liên kết

| Endpoint | Bảo vệ và tác dụng |
| --- | --- |
| GET `/api/auth/google/status` | Công khai; trả `enabled`, `linked`, `pendingEmail`. Email đang chờ chỉ trả cho chính tài khoản có phiên/proof hợp lệ. |
| POST `/api/auth/google/start` | CSRF, body `mode: LOGIN hoặc LINK`, `returnPath`; LINK yêu cầu đang đăng nhập. Trả authorizationPath cố định. |
| GET `/api/auth/google/authorize/google` | Spring tạo state, nonce và PKCE; phải có intent POST trong cùng phiên, còn hạn và đúng tài khoản. |
| GET `/api/auth/google/callback` | Spring xử lý authorization code/OIDC; guard kiểm tra intent/state/tài khoản trước khi tiếp tục. |
| POST `/api/auth/google/link/confirm` | Phiên và CSRF, body `{ "confirmed": true }`; dùng danh tính Google đã xác minh đang chờ trong session. |
| POST `/api/auth/google/link/cancel` | Phiên và CSRF; xóa proof liên kết đang chờ, không ghi liên kết. |

Google được đăng ký qua `CommonOAuth2Provider.GOOGLE`, chỉ scope openid/email/profile. Spring xác minh ID token bằng cơ chế OIDC chuẩn với chữ ký/JWKS, issuer, audience, thời hạn và nonce; state được lưu trong session, thêm PKCE S256. Profile lấy từ kết quả backend đã xác minh, không có API nhận Google ID/email tự khai để đăng nhập hoặc liên kết. Yêu cầu claim `email_verified=true` của Google; không có bước email xác thực thêm của PrismaXI. [Cơ chế Google OIDC](https://developers.google.com/identity/openid-connect/openid-connect), [cấu hình OAuth2 Login của Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html).

`account_identities` mới có khóa chính `(provider, subject_id)`, FK `account_id` tới tài khoản và thời điểm UTC. Subject đã có liên kết sẽ dùng đúng account cũ dù email Google đổi; không đổi email/tên/role của tài khoản cũ. Google mới chưa có account/email trùng sẽ tạo USER với password_hash NULL và liên kết trong cùng transaction. Xung đột đồng thời rollback cả hai thao tác và yêu cầu thử lại; không để lại account mồ côi hoặc tự gộp theo email.

Nếu Google mới trùng email có sẵn: không đăng nhập vào account đó, không lưu liên kết; frontend yêu cầu đăng nhập account hiện có rồi chọn **Liên kết Google**. Có thể đăng nhập bằng email/mật khẩu hoặc danh tính Google đã liên kết trước đó. Người đang đăng nhập cũng có thể bắt đầu LINK với Google có email khác; email account giữ nguyên.

LINK thành công tại Google mới chỉ lưu proof (subject/email đã xác minh) vào session, chưa ghi DB. Frontend hiển thị email Google để người dùng bấm **Xác nhận liên kết Google** hoặc hủy. Xác nhận đọc target account từ phiên và proof; client không được chọn subject/account_id/email để liên kết. Một subject không thuộc hai account; lặp lại cùng account là idempotent. Đổi account/logout trong khi OAuth hoặc confirmation đang chờ không cho liên kết vào account mới hoặc khôi phục phiên đã logout. Intent/proof hết hạn sau 10 phút; mỗi session có một attempt đang chờ.

OAuth dùng chiến lược chống session fixation/đổi CSRF hiện có. Sau thành công, principal của phiên là tài khoản PrismaXI cùng dạng với email login; không giữ principal Google, ID token, access/refresh token lâu dài trong session hoặc client storage. Repository authorized client dùng cho sign-in không giữ provider tokens. Logout của PrismaXI hủy phiên ứng dụng; không đăng xuất toàn bộ tài khoản Google khỏi trình duyệt.

Sau callback, backend chỉ quay về **origin cố định từ env** + route nội bộ đã kiểm tra của SPA (`/`, query và hash). Từ chối URL tuyệt đối, authority `//`, path khác `/`, backslash, control characters và path mã hóa khác root. Không dùng Host/URL quay lại tùy ý từ request làm origin. Kết quả Google chỉ là mã cố định ở query (`success`, `confirm_link`, `cancelled`, `provider_error`, `link_required`, ...); không đưa token/code/error_description lên frontend. Frontend xóa query kết quả khi đọc, giữ hash/trang đang mở và kiểm tra `/me` để xác định đăng nhập thật.

## Kiểm tra triển khai ban đầu và xác nhận Google thật local

Backend từ `backend/`:

```powershell
mvn.cmd '-Dtest=GoogleOAuthIntegrationTest,AuthIntegrationTest,ApiCorsConfigurationTest,FantasyControllerTest' '-Dspring.datasource.url=jdbc:h2:mem:google-build;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1' '-Dspring.datasource.username=sa' '-Dspring.datasource.password=' package
```

**24 test qua**, gồm 9 OAuth integration, 8 email/auth integration, 4 CORS và 3 Fantasy controller. Provider local dùng key RSA sinh trong bộ nhớ, JWT ký thật, JWKS, token/userinfo HTTP; test không tắt kiểm tra nonce hoặc giả lập principal đã đăng nhập qua security-test. Đã kiểm tra Google mới/cũ và email đổi theo subject; USER dù provider gửi claim ADMIN; cùng `/me`/logout/phiên, email trùng không gộp, xác nhận/hủy/idempotent, identity thuộc account khác, state/nonce sai, email chưa verified, provider hủy/lỗi, đổi account/logout trong OAuth và return URL không hợp lệ. Lỗi ghi local cũng xóa principal OIDC tạm thời, không để phiên Google còn authenticated khi hoàn tất account thất bại. Các endpoint email và tra cứu công khai vẫn qua kiểm tra.

Lượt đầu 23 test scoped/build thành công. Sau đó đổi callback local về trực tiếp 8080 để tránh Vite đổi Host và đóng gói lại cấu hình; rà soát cuối bổ sung xử lý lỗi persistence để xóa principal OIDC tạm thời khi thất bại, cùng một test có ý nghĩa cho trường hợp đó. Chạy lại cùng phạm vi 24 test/build để xác nhận thay đổi mới. Không chạy full suite hoặc test theo từng GW/CLB.

Frontend từ `frontend/`:

```powershell
node --test src/api/auth.test.js src/utils/googleAuth.test.js src/api/fantasy.test.js src/fantasy/lineup.test.js src/api/request.test.js
npm.cmd run build
```

**18 test qua**, build Vite thành công, mỗi phần một lượt. Chrome desktop 1440px/390px gọi backend JAR/H2 riêng `target/google-browser-20261004` đã thử lại email signup/login/reload/logout, nút Google disabled khi thiếu credentials và thông báo kết quả hủy/provider lỗi/cần liên kết bằng query mẫu. Không tràn ngang, không lỗi JavaScript, dữ liệu Fantasy thử trong localStorage giữ nguyên. Thông báo query mẫu là kiểm tra UI, không phải xác nhận callback Google thật. Ảnh/kết quả ở `backend/target/google-check/`, không commit.

Sau lượt triển khai ban đầu, bạn đã cấu hình Console/env và thử Google thật trên local: đăng nhập → reload → logout → đăng nhập lại giữ ID, cùng liên kết tài khoản email. Đã đọc lại **H2 local**, không trùng email/danh tính, tài khoản email có liên kết và phiên còn hạn. Đây là xác nhận local theo thao tác người dùng, không phải kiểm chứng Railway/Vercel. Các ca lỗi provider/state/nonce được test tự động với provider local; không khẳng định mọi ca hủy/xung đột đã được người dùng thử lại trên Google thật.

## Vercel/Railway — chuẩn bị, chưa đổi production

Chưa đặt env, chạy migration, ghi MySQL production hoặc deploy. `application-prod.properties` giữ nguyên trong lượt này. Cần migration đã review/backup cho `account_identities` cùng các bảng account/session đã có kế hoạch; H2 DDL không thay thế migration MySQL. Subject phải so sánh chính xác, chọn collation phù hợp khi viết migration. Không đổi schema bóng đá.

Với frontend/backend khác domain, đặt `PREMIERHUB_GOOGLE_FRONTEND_ORIGIN` là origin frontend HTTPS thực tế, `PREMIERHUB_GOOGLE_CALLBACK_URI` là `https://<backend-domain>/api/auth/google/callback`; đăng ký đúng callback đó trong Console và origin frontend thực tế nếu khai báo JavaScript origin. Không dùng dấu placeholder như hostname thật. Đặt `PREMIERHUB_CORS_ALLOWED_ORIGINS` chính xác; frontend production dùng `VITE_API_BASE_URL` là origin backend. Secret chỉ ở Railway/backend.

Chốt domain/proxy trước phát hành. Giữ cookie Secure; nếu vẫn khác site Vercel/Railway, Lax hiện có chưa đủ cho fetch có cookie: cần None+Secure hoặc giải pháp cùng origin/cùng site, kiểm tra chặn cookie bên thứ ba và CSRF như [auth-local-2026.md](auth-local-2026.md). OAuth callback top-level và fetch auth là hai trường hợp khác nhau, không coi callback thành công là fetch đã nhận phiên.

Spring phải thấy scheme/host callback đúng URI đã đăng ký khi đứng sau Railway proxy. Bản có giới hạn xác thực giữ `server.forward-headers-strategy=none` để xác định socket peer trước khi kiểm tra danh sách proxy tin cậy; không bật global `native/framework`. Xác minh dải IP và cách proxy ghi/nối X-Forwarded-For cùng việc xử lý scheme/host HTTPS đáng tin trước phát hành, xem chính sách ở auth-local-2026.md. Chưa thiết kế/xác minh forwarding HTTPS Railway cho bản này hoặc thay cấu hình production. Thử HTTPS, cookie, callback và return route trên deployment thật trước khi gọi đã phát hành.

## File cần commit cho lượt Google

- `backend/pom.xml`
- `backend/src/main/java/com/premierhub/accounts/AccountRepository.java`
- `backend/src/main/java/com/premierhub/accounts/AuthExceptionHandler.java`
- `backend/src/main/java/com/premierhub/accounts/GoogleAccountException.java`
- `backend/src/main/java/com/premierhub/accounts/GoogleAccountService.java`
- `backend/src/main/java/com/premierhub/accounts/GoogleAuthController.java`
- `backend/src/main/java/com/premierhub/accounts/GoogleIdentityRepository.java`
- `backend/src/main/java/com/premierhub/accounts/GoogleOAuthFlow.java`
- `backend/src/main/java/com/premierhub/accounts/GoogleOAuthSecurity.java`
- `backend/src/main/java/com/premierhub/accounts/GoogleOAuthSettings.java`
- `backend/src/main/java/com/premierhub/config/SecurityConfiguration.java`
- `backend/src/main/resources/application-auth-local.properties`
- `backend/src/main/resources/auth-schema.sql`
- `backend/src/test/java/com/premierhub/accounts/AuthIntegrationTest.java`
- `backend/src/test/java/com/premierhub/accounts/GoogleOAuthIntegrationTest.java`
- `backend/src/test/java/com/premierhub/accounts/LocalOidcProvider.java`
- `frontend/src/api/auth.js`
- `frontend/src/api/auth.test.js`
- `frontend/src/components/AccountMenu.jsx`
- `frontend/src/components/AccountMenu.css`
- `frontend/src/utils/googleAuth.js`
- `frontend/src/utils/googleAuth.test.js`
- `docs/google-auth-2026.md`
- `docs/auth-local-2026.md`
- `docs/fantasy-multiplayer-2026-plan.md`

Commit message: `feat: add Google OIDC sign-in and confirmed account linking`.

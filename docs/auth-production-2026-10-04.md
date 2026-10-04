# Phát hành tài khoản production — checkpoint 04/10/2026

Người dùng đã cho phép migration tài khoản, cấu hình Railway/Vercel và redeploy đúng commit đã push. Không tự commit/push, không sửa mùa 2024/25, không chép tài khoản H2. Chưa thay đổi mã ứng dụng hoặc chạy lại test/build local.

## Trạng thái mới nhất — đã phát hành và xác nhận Google thật

Người dùng báo các thao tác vừa thử đều OK. Đọc MySQL production sau phản hồi: **2 tài khoản USER (ID 1, 2), 2 email khác nhau, mỗi tài khoản có đúng một identity provider='google'**; không trùng provider/subject, không identity mồ côi. ID 1 không có phiên authenticated còn hạn; ID 2 có một phiên authenticated còn hạn. Không in email/subject/hash/session/token và không thay đổi dữ liệu trong lượt đối chiếu. Google thật production được ghi nhận đạt dựa trên thao tác người dùng và dữ liệu account/identity/session đã lưu.

Hai tài khoản hiện tại đều Google-only (password_hash NULL). Chưa có account email–mật khẩu hoặc ca LINK từ account email trong dữ liệu production để đối chiếu độc lập; các luồng đó vẫn giữ kết quả kiểm chứng local đã đạt. Không tự tạo thêm account hoặc yêu cầu lặp toàn bộ thao tác Google. Các đoạn chờ thử/baseline dưới đây là ghi nhận trước phản hồi mới này.

- Người dùng xác nhận Vercel Production đã có proxy secret khớp Railway, xóa VITE_API_BASE_URL và Google Console đã thêm callback frontend. Không yêu cầu VERCEL_TOKEN.
- GitHub commit status xác nhận Vercel SUCCESS cho `446f04459f8c0631ce0307302f0260af53066cef`, deployment https://vercel.com/cuong-pl/premierhub/9WPV6BoTEoer3vLKgDecwamASJkC. Origin production phục vụ bundle `/assets/index-BOjka90l.js` khớp byte với bundle release local đã build trước đó; không còn backend API base cũ, không có proxy secret/Google client secret trong bundle hoặc secret response header. Không cần bấm Redeploy thêm.
- Railway triển khai bằng serviceInstanceDeployV2 với commitSha tường minh, không upload working tree: deployment **a153aa7b-7865-4246-8aa1-76bd59895fe4**, status **SUCCESS**, cùng commit `446f044`. Theo dõi BUILDING → DEPLOYING → SUCCESS; `/actuator/health` 200/UP, log mẫu có application started và không có ERROR.
- Auth trực tiếp Railway trả **403 AUTH_PROXY_REQUIRED**. Qua Vercel, `/api/auth/me` chưa đăng nhập trả **401 AUTH_REQUIRED**, Google status 200/enabled=true. Điều này kiểm chứng Vercel gửi đúng proof/header IP để backend chấp nhận proxy; không nới kiểm tra proxy/CSRF.
- Chrome riêng trên domain production: cookie SESSION host `premierhub.vercel.app`, Secure/HttpOnly/SameSite=Lax/Path=/, JS không đọc được. CSRF endpoint hoạt động/no-store; POST logout thiếu CSRF trả 403, có CSRF trả 204, xóa cookie, /me trở lại 401. Đọc MySQL xác nhận phiên vô danh của lần kiểm tra đã bị xóa.
- API công khai qua Vercel đều 200/no-store: clubs 20, standings mùa 2026 có 20, matches GW1 có 10 và chi tiết player 2000020085. Browser render CLB/BXH/lịch/cầu thủ; lần lượt bảng 20 dòng, lịch 10 trận, danh sách cầu thủ 12 thẻ ban đầu. Form Google được bật, dialog không tràn ở 390px, không có uncaught JavaScript exception. Marker storage trong browser kiểm tra riêng được giữ nguyên; không tác động trình duyệt/đội Fantasy của người dùng.
- Kiểm tra khởi tạo Google LOGIN qua server: authorization path cùng origin, Location tới accounts.google.com với callback `https://premierhub.vercel.app/api/auth/google/callback`, scope chỉ openid/email/profile, có state/nonce/PKCE. Không follow redirect tới Google; hủy phiên kiểm tra bằng logout. **Không gọi đây là Google thật đã đạt**.
- Baseline sau kiểm tra: production có **0 account, 0 Google identity, 0 phiên có principal**. Không tạo account thử hoặc copy H2. Người dùng sẽ tự thử email/Google/LINK; chỉ đối chiếu ID/identity/session sau khi có kết quả thao tác.
- Không chạy lại migration, không sửa mã, không test/build local lại, không commit/push hoặc thay dữ liệu bóng đá. Chỉ runtime auth/session và deployment/env đã được người dùng cho phép thay đổi. Browser/helper kết quả ở backend/target/auth-production-release được ignore; browser kiểm tra riêng được dừng sau khi hoàn tất.

### Người dùng thử thật và bước đối chiếu tiếp theo

1. Mở https://premierhub.vercel.app → Đăng nhập → Đăng ký email → đăng nhập. Mở https://premierhub.vercel.app/api/auth/me ghi ID, reload website và kiểm tra vẫn đúng tài khoản; đăng xuất, /me phải trả 401 AUTH_REQUIRED. Không gửi mật khẩu/cookie/token.
2. Tiếp tục với Google, tự thao tác màn hình Google. Reload, đăng xuất rồi đăng nhập Google lại; đối chiếu cùng ID. Callback phải về origin frontend và trang đang mở.
3. Nếu Google email trùng account email đã tạo, việc yêu cầu liên kết là đúng: đăng nhập email → Liên kết Google → xác nhận → đăng xuất → Google login lại dùng cùng ID. Không tự gộp theo email.
4. Báo ID/email dùng thử và bước đạt/lỗi. Agent đọc đúng account/identity/session của lần thử để xác nhận không trùng và phiên; không in hash/token/provider subject hoặc duyệt toàn bộ dữ liệu bóng đá. Sau logout cuối, principal/session liên quan phải được thu hồi. Trước phản hồi/thao tác này, email/Google production chưa được đánh dấu hoàn thành đầu cuối.

Các phần dưới là biên bản trước khi hai deployment mới sẵn sàng, không phải phần còn chặn hiện tại.

## Deployment khi bắt đầu

- Commit main/origin/main: `446f04459f8c0631ce0307302f0260af53066cef`.
- Vercel báo deployment thành công cho commit này qua GitHub commit status; deployment dashboard: https://vercel.com/cuong-pl/premierhub/8nfmcBGMeS8Si2vPjMq7q6835Aa9. Chưa có quyền API/dashboard Vercel để kiểm tra metadata đầy đủ hoặc thay env.
- Railway deployment `469aa0b6-7fce-4e55-9554-4727a496bfb5` cho commit này FAILED. Startup dừng ở AuthProxySettings vì thiếu PREMIERHUB_AUTH_PROXY_SECRET; Railway còn thiếu public origin/Google/cookie/proxy env.
- Railway giữ deployment `11bb8240-f31c-4337-8986-8ae6c8b469be`, commit `3a0a314f7ce399ef4ed25adde98667f418bc5a7b`, SUCCESS.
- Frontend HTTP 200; backend `/actuator/health` 200/UP; `/api/clubs` qua Vercel 200, 20 CLB. `/api/auth/me` qua Vercel và trực tiếp Railway đều 401 AUTH_REQUIRED ở bản backend cũ; chưa chứng minh proxy guard production mới hoạt động.
- Bundle frontend đang có URL backend cũ. Chưa bỏ VITE_API_BASE_URL trên Vercel; không thấy proxy secret trong bundle được kiểm tra.

## MySQL đã hoàn thành

- Project Railway `pure-achievement`, ID `d693350f-1fcf-40c0-8b08-2522a51521ac`, environment production `292453d1-e079-4cc7-a68d-fab4f70c007e`.
- Backend service `659da72c-ebc5-4449-a6f3-76cc6da16c4c`, MySQL service `d000f42d-bf5c-40df-a0e1-eb0bc2705711`.
- Đối chiếu private host, database, user và password giữa hai service bằng biến Railway, không in credentials. MySQL `railway`, public proxy `altaria.proxy.rlwy.net:41569`, server version `9.7.2`, server UUID `8835db23-b8ca-11f1-89a0-a2aa18198d9d`. CLI kết nối với ssl-mode=REQUIRED; collation utf8mb4_0900_bin có NO PAD.
- Trước ghi không có bảng auth nào. Backup mới: `backend/local-backups/auth-production-20261004-221652/railway-before-auth-20261004-221652.sql`, Git ignored, **841.141 byte**, SHA-256 `aa7c96cb2dcccd5d6513bd7cab03d6ddd3a9441592ee3296c6e9ea64c9316c9e`.
- Backup exit 0, có dấu hoàn tất cuối dump và đủ CREATE TABLE cho cả 22 bảng ban đầu. Kiểm tra này hoàn tất trước migration. Chưa thử restore backup.
- Dùng đúng `backend/sql/2026-10-04-accounts-mysql.sql` của commit `446f044`; chạy hai lần. SHOW CREATE TABLE không đổi giữa hai lần; danh sách bảng sau chỉ thêm accounts, account_identities, SPRING_SESSION, SPRING_SESSION_ATTRIBUTES. Cả bốn bảng mới có 0 dòng ở thời điểm kiểm tra.
- Không nhập tài khoản/identity/session H2 hoặc ghi thống kê bóng đá. Không xóa/reset bảng hiện có.
- Sau migration, GET qua origin Vercel vẫn HTTP 200/JSON: standings mùa 2026 có 20 dòng, matches GW1 có 10 dòng, chi tiết cầu thủ 2000020085 mùa 2026 trả dữ liệu. Chưa thử render từng trang bằng browser production; các request này đang được bản backend cũ phục vụ.

## Railway đã lưu cấu hình, chưa redeploy

Các biến được upsert không replace toàn bộ và skipDeploys=true; đọc lại xác nhận khớp. Credentials lấy riêng từ môi trường/file local đã ignore; không ghi giá trị vào tài liệu/log/chat.

- PREMIERHUB_AUTH_PUBLIC_ORIGIN và PREMIERHUB_GOOGLE_FRONTEND_ORIGIN: `https://premierhub.vercel.app`.
- PREMIERHUB_GOOGLE_CALLBACK_URI: `https://premierhub.vercel.app/api/auth/google/callback`.
- PREMIERHUB_AUTH_PROXY_SECRET: secret mới ngẫu nhiên 32 byte/64 ký tự hex, server only; Vercel phải dùng đúng cùng giá trị.
- PREMIERHUB_GOOGLE_CLIENT_ID/SECRET: lấy credentials Google đã kiểm chứng local, không đổi giá trị trong .env.local.
- JDBC giữ đúng private host/database/user/password hiện có; bổ sung connectionTimeZone=UTC, forceConnectionTimeZoneToSession=true, sslMode=REQUIRED. JAVA_TOOL_OPTIONS giữ options cũ và đặt user.timezone=UTC.
- SPRING_PROFILES_ACTIVE=prod, CORS chỉ frontend trên, SERVER_FORWARD_HEADERS_STRATEGY=none, cookie Secure=true/HttpOnly=true/SameSite=lax/Path=/; Domain không có. PREMIERHUB_AUTH_TRUSTED_PROXIES rỗng.
- Deployment manifest đang có một replica ở asia-southeast1-eqsg3a; không thay scale. Rate counters vẫn từng JVM, reset khi restart.

Chưa redeploy Railway mới khi Vercel chưa có secret: tránh chuyển browser auth sang 403 trong thời gian thiếu quyền cấu hình frontend. Các biến lưu trong service chưa chứng minh đã có hiệu lực trong process đang chạy.

## Phần còn chặn và thứ tự tiếp tục

1. Cần quyền Vercel: thêm VERCEL_TOKEN vào `backend/.env.local` (Git ignored) để agent gọi API; không gửi token trong chat. Hoặc người dùng thao tác dashboard https://vercel.com/cuong-pl/premierhub/settings/environment-variables.
2. Xác nhận Root Directory frontend. Tạo PREMIERHUB_AUTH_PROXY_SECRET cho **Production** bằng giá trị đang lưu ở Railway backend Variables, không tiền tố VITE_; xóa VITE_API_BASE_URL khỏi Production. Không tạo secret mới riêng cho Vercel, không đặt Google secret/DB credentials ở Vercel.
3. Google Console → Google Auth Platform → Clients (hoặc APIs & Services → Credentials) → Web application client đang dùng → Authorized redirect URIs: thêm `https://premierhub.vercel.app/api/auth/google/callback`, giữ callback local. Origin nếu khai báo: `https://premierhub.vercel.app`. Chờ người dùng báo đã lưu, không yêu cầu gửi secret.
4. Khi Vercel env đã sẵn sàng, deploy Railway **commit 446f04459f8c0631ce0307302f0260af53066cef**. Dùng serviceInstanceDeployV2 với commitSha tường minh; không redeploy bản SUCCESS cũ 3a0a314 và không upload working tree. Theo dõi deployment/health, không tắt CSRF/proxy guard.
5. Redeploy Vercel deployment của cùng commit, với project settings/env hiện tại. Xác nhận production alias, SHA và trạng thái READY. Không deploy commit tài liệu chưa push.
6. Qua origin frontend kiểm tra CSRF cookie Secure/HttpOnly/host-only/Path=/Lax, /me 401 khi chưa đăng nhập, logout có CSRF xóa phiên/cookie, thiếu CSRF bị 403, Google status/start/redirect có callback frontend và API công khai. Trực tiếp Railway auth phải 403 AUTH_PROXY_REQUIRED. Kiểm tra bundle không chứa proxy/Google secrets và không còn API base cũ.
7. Sau khi website sẵn sàng, người dùng tự thử email và Google thật; đối chiếu ID/phiên sau reload/logout/login lại. Chỉ lúc đó mới ghi Google production đã kiểm chứng. Chưa tạo tài khoản thử production hoặc gửi Google consent trong checkpoint này.

Không lặp migration từ đầu khi tiếp tục: đọc schema/report hiện có; backup trên là backup trước migration, không dùng làm backup cho một thay đổi mới. Helpers/report riêng trong backend/target/auth-production-release được ignore, không phải file commit.

File tài liệu cần commit: docs/auth-production-2026-10-04.md, docs/auth-release-2026.md, docs/fantasy-multiplayer-2026-plan.md. Commit message: `docs(auth): record production account migration and release status`.

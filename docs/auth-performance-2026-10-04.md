# Đo độ trễ sau phát hành auth — 04/10/2026

Commit production đang chạy: 446f044. Lượt này chỉ đọc cấu hình/log/SQL production, đo HTTP và sửa frontend local. Không thay region/env production, redeploy, tạo account production, nhập H2 hoặc ghi dữ liệu bóng đá. Các request CSRF/đăng xuất dùng phiên vô danh riêng, được dọn sau đo.

## Cùng API trực tiếp Railway và proxy Vercel

API: GET /api/standings?season=2026, không cookie, 5 lượt mỗi đường, xen kẽ direct/proxy, tất cả HTTP 200 và 20 dòng. Mỗi request mở kết nối TLS mới. TTFB đo đến dòng trạng thái HTTP đầu tiên, tổng đến khi đọc xong body; số liệu tính bằng ms. Kết nối TLS có kiểm tra certificate. curl/Schannel trên máy không lấy được credentials TLS, nên dùng Python HTTPSConnection để đo thành công, không tắt xác minh certificate.

| Lượt | Railway TTFB | Railway tổng | Vercel TTFB | Vercel tổng |
| --- | ---: | ---: | ---: | ---: |
| 1 | 514,34 | 514,61 | 2.409,38 | 2.409,60 |
| 2 | 446,65 | 446,84 | 896,97 | 897,18 |
| 3 | 464,80 | 465,04 | 1.413,91 | 1.414,11 |
| 4 | 513,75 | 513,95 | 880,52 | 880,74 |
| 5 | 447,64 | 447,83 | 1.362,44 | 1.578,28 |
| Trung vị lượt 2–5 | 456,22 | 456,44 | 1.129,71 | 1.155,65 |

Lượt 1 là lần đầu của phép đo, **không phải cold start đã được xác nhận**. Riêng Vercel lượt 1 tốn 1.101,61 ms ở DNS/TCP/TLS setup, các lượt sau 72,60–94,82 ms. Không quy toàn bộ chênh lệch lần đầu cho auth hoặc Serverless.

Đối chiếu x-railway-request-id với HTTP log: cả direct/proxy có upstreamRqDuration 335–347 ms, gần nhau. Vercel chờ từ sau TLS đến status 785,70–1.341,11 ms, đã bao gồm mạng từ client/edge/proxy tới upstream. Backend thực xử lý khoảng 0,34 giây; phần còn lại là tuyến mạng/proxy, chưa có Server-Timing của Vercel để tách chính xác thời gian nội bộ proxy khỏi network transit. Body nhỏ, không có dấu hiệu payload lớn gây chậm chính.

## Region và Serverless

- Vercel dùng static external rewrite, **không có Vercel Function** cho /api. Region Function không áp dụng; đặt Functions Region không sửa route này. Header x-vercel-id ghi incoming edge hkg1; khi qua proxy, x-railway-edge ghi cdg1 (Paris), trong khi direct ghi hkg1. Đây là region edge/tuyến ingress, không phải region JVM hoặc MySQL.
- Backend Railway deployment manifest: asia-southeast1-eqsg3a (Singapore), 1 replica, Serverless/sleepApplication=true.
- MySQL deployment manifest: sfo (legacy California), 1 replica, Serverless=false. JDBC hiện dùng mysql.railway.internal; backend và DB vẫn ở hai châu lục dù dùng private hostname.
- Cold start được đối chiếu riêng: container dừng 16:30:52 UTC, khởi động 16:32:31, app started 16:32:46. Request /api/auth/me lúc 16:32:47 trả 401 nhưng totalDuration **17.197 ms**, upstreamRqDuration **1.077 ms**; responseDetails="Retried single replica", có nhiều lỗi connection refused trước khi upstream sẵn sàng. Khoảng 16.120 ms nằm ngoài thời gian xử lý upstream, khớp vòng khởi động container. Lượt /me sau đó 1.340 ms, không còn retry. Log trước đó còn một lượt BXH 18.329 ms khi container vừa khởi động, các lượt khi chạy ổn định khoảng 0,34 giây nếu không session.

Serverless là nguyên nhân của độ trễ khi đánh thức; không phải lời giải thích cho độ trễ mọi request khi container đã chạy.

## Cookie phiên là chi phí dư trên API công khai

Frontend public helper trước sửa dùng fetch mặc định cùng origin, nên tự gửi SESSION. Spring Security/Spring Session có thể đọc và lưu JDBC session ngay cả khi endpoint thống kê permitAll. DB khác region khiến các lượt này đắt hơn; không cần gán dữ liệu thống kê theo user.

A/B 3 lượt trên production bằng **phiên vô danh riêng**, không mượn cookie người dùng:

| Lượt | Có SESSION: TTFB / tổng | Bỏ SESSION: TTFB / tổng | Railway upstream có / bỏ SESSION |
| --- | ---: | ---: | ---: |
| 1 | 2.186,20 / 2.186,53 | 1.386,48 / 1.386,77 | 1.669 / 336 |
| 2 | 2.648,74 / 2.649,06 | 856,45 / 856,73 | 1.661 / 340 |
| 3 | 2.201,79 / 2.217,37 | 851,50 / 869,57 | 1.666 / 336 |

Request ID được ghép đúng log, response/data không đổi. Upstream trung vị giảm **1.666 → 336 ms (~80%)**. Chênh lệch ổn định ~1,33 giây chứng minh chi phí session dư; không phải phỏng đoán từ tổng latency Vercel.

## Auth, SQL, connection pool và BCrypt

- /me production được gọi **một lần lúc mount**. Sau chuyển BXH và Lịch đấu vẫn chỉ một /me, không polling/chuỗi /me lặp do đổi tab. React StrictMode không tạo double effect trong production build. Google status chỉ gọi khi mở dialog và khi account ID đổi. Các POST auth dùng CSRF → POST nối tiếp để giữ đúng token/phiên; không bỏ CSRF nhằm lấy tốc độ.
- Các account production đã thử đều Google-only: Google flow không gọi BCrypt để xác minh mật khẩu. Không thể quy độ chậm Google này cho BCrypt.
- PasswordEncoder hiện dùng BCrypt cost 10. Benchmark synthetic trong JVM local, đúng thư viện build hiện có: 5 lần matches **54,67–57,57 ms**, trung vị 57,25 ms. Không in/hash/password và không tạo account production. Đây không phải timing BCrypt CPU Railway, không dùng để khẳng định CPU production tương đương.
- MySQL có unique email, session ID index và expiry/principal index. EXPLAIN ANALYZE lookup email/session ID không tồn tại dùng khóa tra cứu và chạy 0,00903/0,00124 ms ở server. Không thấy thiếu index ở đường này; không phải phép đo các transaction thực tế hoặc toàn bộ query workload.
- Performance_schema production **OFF**, không có statement digest/timer để phân rã session transactions. Không bật lại hoặc restart DB. Production /actuator chỉ expose health/info; máy chưa có SSH key Railway, nên chưa lấy được Hikari pending/acquire metrics trong JVM chạy thật.
- Không có env Hikari override; cấu hình thư viện mặc định max pool 10. MySQL snapshot Threads_connected=11, Threads_running=2; số này **không chứng minh** pool production không từng chờ. Probe Hikari/H2 local riêng acquire 0,003–0,009 ms, threadsAwaitingConnection=0; không coi là kiểm chứng pool production. Không tự tăng pool vì chưa có bằng chứng saturation.
- /me có session vẫn cần các lượt DB, login Google/callback/LINK cũng cần persistence. Sửa gửi cookie trên API công khai không giảm mọi chi phí auth; đặt JVM và DB gần nhau là bước hạ độ trễ các luồng này.

## Sửa local và đo hành vi mới

frontend/src/api/request.js: public GET helper dùng credentials='omit', giữ route/proxy, JSON validation và AbortSignal. Account APIs ở auth.js tiếp tục credentials='include'; không thay cookie/CSRF/state/nonce/PKCE, liên kết Google hoặc cache response tài khoản. Không đổi luật Fantasy hoặc data mùa. Việc public GET không tải session cũng có nghĩa các lượt chỉ đọc thống kê không tự gia hạn phiên auth; hoạt động auth/private vẫn xử lý phiên như trước.

Trong Chrome riêng, import **đúng source helper đã sửa local** vào trang production để đo A/B hành vi request, không deploy code. Cùng SESSION riêng vẫn tồn tại trong browser, nhưng helper public mới bỏ gửi cookie:

| Lượt | Trước: TTFB / tổng | Helper sửa: TTFB / tổng |
| --- | ---: | ---: |
| 1 | 1.975,60 / 1.975,90 | 876,10 / 876,40 |
| 2 | 1.966,20 / 1.967,10 | 624,80 / 625,80 |
| 3 | 1.940,00 / 1.940,40 | 867,00 / 867,20 |
| Trung vị | **1.966,20 / 1.967,10** | **867,00 / 867,20** |

Median tổng giảm ~56%. Cookie phiên được giữ nguyên, /me vô danh vẫn 401, logout cuối dọn phiên. **Đây là A/B hành vi mới trước phát hành, không phải số đo sau deployment production mới**. Không tạo account thật để đo. Chưa có số sau đổi region/tắt Serverless vì production không bị sửa.

Kiểm tra đúng phần sửa: `node --test src/api/request.test.js src/api/auth.test.js` **11 test qua**; `npm run build` frontend **một lượt qua**. Không Maven/full test/backend build. Test xác minh public omit/account include, giữ signal/validation/AbortError, auth CSRF/Google/429 không hồi quy. Helpers/profiles/report trong backend/target/auth-performance được ignore, không commit.

## Thay đổi production để người dùng thực hiện

1. Railway → project pure-achievement → EPL---PERSONAL → Settings → Deploy → **Serverless OFF**. Giữ một replica, healthcheck /actuator/health. Khi người dùng chọn redeploy, dùng đúng commit đã push có phần sửa frontend/hoặc commit backend hiện tại nếu chỉ đổi backend setting. Setting chỉ có hiệu lực trên container mới; không dùng cron ping làm workaround.
2. Đặt backend gần MySQL. Phương án ít đụng dữ liệu: backend Settings → Deploy → region **US West Metal (California), us-west2**, giữ MySQL sfo. Chỉ đổi backend không volume; không tự di chuyển DB/volume. Hoặc nếu muốn toàn bộ Singapore thì cần kế hoạch riêng backup/di chuyển MySQL volume và downtime, không chỉ chọn lại nhãn region. Sau đổi phải xác nhận actual deployment manifest và đo lại RTT/latency; chưa biết mức giảm đến khi thực hiện.
3. Nếu muốn bỏ vòng proxy cho **API bóng đá công khai**, tùy chọn Vercel Production VITE_API_BASE_URL=`https://epl-personal-production.up.railway.app`, rồi người dùng redeploy frontend. request.js chỉ dùng biến này cho public lookups; auth.js vẫn đường tương đối /api/auth và Google callback giữ origin Vercel, proxy secret server-only. CORS Railway đã cho phép frontend này, public request omit cookie. Không dùng biến này để gọi auth trực tiếp Railway. Tùy chọn này thay đường đi API công khai; có thể giữ proxy như hiện tại nếu ưu tiên toàn bộ /api cùng origin.
4. Sau người dùng phát hành: đo lại 5 cặp direct/proxy, A/B session, first request sau idle và /me/login thật của chính người dùng. Nếu vẫn chậm khi đã cùng region/container không sleep, cần Hikari metrics/tracing có quyền giới hạn để đo acquisition/query/BCrypt trong JVM; không expose secrets/actuator nhạy cảm công khai. Chưa có Vercel Function để đổi region.

## File commit của lượt này

- frontend/src/api/request.js
- frontend/src/api/request.test.js
- docs/auth-performance-2026-10-04.md

Commit message: `perf(frontend): omit session cookies from public football lookups`.

Các thay đổi tài liệu auth/release từ lượt trước được giữ nguyên, không tự commit/push/deploy.

## Nguồn chính thức

- [Railway regions và ảnh hưởng volume](https://docs.railway.com/deployments/regions).
- [Railway Serverless: container mới và wake-up](https://docs.railway.com/deployments/serverless).
- [Railway: độ trễ khi app và database khác region](https://docs.railway.com/deployments/troubleshooting/slow-deployments).
- [Vercel external rewrites](https://vercel.com/docs/routing/rewrites).

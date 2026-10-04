# Context và kế hoạch Fantasy PrismaXI có tài khoản — 2026/27

## Mục tiêu và phạm vi lượt này

Hoàn thiện Fantasy có tài khoản trước hạn chốt GW6 mùa 2026/27 để thử nghiệm với người chơi thật. Triển khai từng bước nhỏ, không làm toàn bộ trong một lượt. Chỉ phát triển mùa 2026/27, không sửa mùa 2024/25 hoặc thống kê bóng đá đã có.

Lượt tạo tài liệu ban đầu chỉ lưu context và kế hoạch. Bước 1 đã có **email/mật khẩu local**, code **Google OIDC và liên kết có xác nhận**; xem [hướng dẫn tài khoản](auth-local-2026.md) và [Google](google-auth-2026.md). Google mới được kiểm tra với provider local, chưa thử Google thật vì thiếu credentials. Khôi phục mật khẩu làm sau; chưa triển khai chốt đội, chấm điểm hoặc BXH người chơi. Deadline thực tế GW6 chưa được xác minh; không xem mục tiêu này là quyền mở cuộc thi hay mở lại vòng đã hết hạn.

## Hiện trạng và điểm đã đối chiếu trong repo

- Theo context người dùng: backend Spring Boot, frontend React/Vite, MySQL trên Railway, frontend trên Vercel. Lượt tài khoản chỉ kiểm tra H2 local, không đọc/ghi production hoặc thử deployment.
- [backend/pom.xml](../backend/pom.xml) giữ Java mục tiêu 21 và Spring Boot 4.1.1, Security/Session JDBC và starter kiểm thử Security theo BOM hiện có (Security 7.1.1, Session 4.1.1); thêm OAuth2 Client cho Google. Email local và OAuth với provider local đã qua kiểm tra; Google thật/deployment chưa xác minh.
- Đã có roster, OVR, primaryPosition và eligiblePositions. [FantasyLineupService](../backend/src/main/java/com/premierhub/service/FantasyLineupService.java) và [lineup.js](../frontend/src/fantasy/lineup.js) kiểm tra đúng vị trí, 11 người khác nhau, giới hạn CLB và OVR. Các sơ đồ Fantasy đang có là `4-2-1-3`, `4-3-3`, `4-4-2`, `3-5-2`; giữ nguyên các tên/ô này, không lấy sơ đồ đội hình trận thay luật Fantasy. LCB/RCB dùng quyền CB, LCM/RCM dùng quyền CM.
- [FantasyPage.jsx](../frontend/src/components/FantasyPage.jsx) lưu lựa chọn trong localStorage. `POST /api/fantasy/2026/validate` trong [FantasyController](../backend/src/main/java/com/premierhub/web/FantasyController.java) chỉ kiểm tra đội hình, chưa lưu đội dự thi hay xác nhận tham gia một GW. Mốc roster hiện có là `2026-10-02`; khi thiết kế vòng thi cần quyết định cách dùng mốc roster theo GW, không tự đổi dữ liệu/membership đang có.
- [schema.sql](../backend/src/main/resources/schema.sql) giữ nguyên các bảng bóng đá. [auth-schema.sql](../backend/src/main/resources/auth-schema.sql) mới chỉ tạo tài khoản và phiên JDBC trên H2 local; chưa có bảng đội dự thi/kết quả cuộc thi. Migration MySQL production còn phải chuẩn bị và được giao thực hiện.
- Local: [vite.config.js](../frontend/vite.config.js) proxy `/api` tới `http://localhost:8080`. Production: [request.js](../frontend/src/api/request.js) gọi origin backend qua `VITE_API_BASE_URL`; [auth.js](../frontend/src/api/auth.js) gửi `credentials: 'include'` và CSRF cho auth. Chưa có Vercel rewrite được xác minh; không giả định production có proxy cùng origin.
- [ApiCorsConfiguration](../backend/src/main/java/com/premierhub/config/ApiCorsConfiguration.java) giữ GET tra cứu và POST Fantasy validate công khai, bổ sung GET/POST `/api/auth/**` với allowCredentials và origin chính xác qua `PREMIERHUB_CORS_ALLOWED_ORIGINS`. Local cookie HttpOnly/SameSite=Lax đã thử; cấu hình khác site Vercel–Railway phải giải quyết trước phát hành.
- Đã có thống kê/rating GW1–GW5 theo context và hồ sơ nhập trước đó; rating do người dùng cung cấp từ SofaScore. Không kiểm kê lại toàn database hoặc thu thập lại các vòng này khi lưu kế hoạch.
- OVR chỉ dùng giới hạn sức mạnh đội; điểm thi đấu lấy từ rating, không phải tổng OVR. [Đội hình tiêu biểu](team-of-week-2026.md) là chức năng riêng, chưa phải hệ thống tài khoản/đội dự thi/BXH người chơi.

## Quy tắc sản phẩm đã chốt

1. Đăng ký/đăng nhập bằng email–mật khẩu và Google. Bản đầu không yêu cầu email xác thực trước khi chơi.
2. Mỗi tài khoản có đúng một đội dự thi và một sơ đồ cho mỗi GW. Có thể lưu nháp, nhưng không tạo nhiều đội dự thi có hiệu lực cho cùng user/season/GW.
3. Đủ 11 cầu thủ khác nhau, đúng eligiblePositions, tối đa 3 người/CLB, tổng OVR không quá 860. Giữ các sơ đồ Fantasy đang có.
4. Hạn chốt là **00:00 giờ Việt Nam của ngày liền trước ngày trận đầu tiên của GW diễn ra**. Lưu thời điểm trong database bằng UTC.
5. Hạn đã công bố giữ cố định nếu lịch trận thay đổi; chỉ quản trị viên được điều chỉnh, có thông báo.
6. Người chơi sửa nháp thoải mái trước hạn. Muốn đổi đội dự thi phải bấm chốt lại; đội đã chốt gần nhất vẫn có hiệu lực cho đến khi chốt mới thành công. Sửa nháp không tự thay đội đã chốt.
7. Sau hạn backend từ chối thay đổi. Không chốt đúng hạn thì không tham gia GW; không tự lấy đội vòng trước.
8. Trước hạn chỉ xem đội mình. Sau hạn có thể xem đội đã chốt của người khác; không lộ nháp qua API.
9. Điểm đội bằng tổng rating của 11 người. Người xác nhận không ra sân hoặc không được SofaScore chấm nhận 0 điểm trong cuộc thi.
10. Rating chưa thu thập là dữ liệu đang chờ, không tự coi là 0.
11. Chờ đủ trận và dữ liệu mới công bố kết quả/BXH; trận hoãn có thể khiến GW tiếp tục chờ.
12. Có BXH từng GW và BXH mùa cộng các GW đã công bố. Bằng điểm thì đồng hạng; người chơi bỏ lỡ vòng không được cộng điểm vòng đó.
13. GW1–GW5 dùng thử nghiệm/Replay. Không cho tham gia cuộc thi chính thức của vòng đã biết kết quả.

Deadline tính từ ngày của kickoff đầu tiên sau khi chuyển UTC sang `Asia/Ho_Chi_Minh`, lùi một ngày lịch rồi lấy 00:00 Việt Nam, cuối cùng chuyển về UTC để lưu. Không nhầm với 00:00 ngày có trận hoặc “24 giờ trước kickoff”. Ví dụ minh họa: ngày Việt Nam của trận đầu là 05/10 thì hạn là 04/10 lúc 00:00 Việt Nam, tức 03/10 lúc 17:00 UTC; đây **không phải** deadline GW6 đã xác minh. Điều kiện chốt phải là thời gian server trước deadline; tại deadline và sau đó phải từ chối.

Không thay rating/fantasy_points NULL của thống kê nguồn thành 0 để chấm cuộc thi. Điểm cuộc thi 0 của người đã được xác nhận không được chấm là kết quả được lưu riêng; rating còn chờ vẫn chưa có điểm xác định. Schema thống kê hiện có participation_status và rating NULL nhưng chưa có trạng thái riêng phân biệt mọi trường hợp PLAYED không có rating; bước 4 phải giải quyết điểm này bằng xác nhận rõ, không suy luận từ NULL.

## Lộ trình triển khai theo từng lượt

### Bước 1 — Tài khoản

- **Checkpoint local:** email/mật khẩu, tên hiển thị, đăng xuất, `/me`, USER, CSRF, phiên JDBC và giao diện desktop/390px đã hoàn thành. Google OIDC và liên kết có xác nhận đã có code/kiểm tra provider local; Google thật chờ credentials, khôi phục mật khẩu chưa làm. Bước 1 tổng thể chưa hoàn thành. Hướng dẫn/file Google ở [google-auth-2026.md](google-auth-2026.md).
- Email/mật khẩu, Google, đăng xuất, tên hiển thị và khôi phục mật khẩu.
- Dùng cơ chế xác thực tiêu chuẩn của Spring Security, không tự thiết kế mã hóa/băm mật khẩu. Giải thích nhu cầu và phạm vi dependency trước khi thêm.
- Kiểm tra danh tính Google ở backend bằng cơ chế tiêu chuẩn; không tin email/tên/ID do frontend tự gửi. Liên kết tài khoản phải có xác nhận sở hữu phù hợp; không tự gộp tài khoản chỉ vì chuỗi email trùng nhau.
- Phân quyền người chơi/quản trị viên; không cho tự đăng ký quyền admin hoặc gửi role để nâng quyền. Email không công khai trên BXH.
- Các trang tra cứu công khai hiện tại vẫn dùng được khi chưa đăng nhập.
- Kiểm tra đăng nhập, đăng xuất, cookie/session hoặc token, CSRF/CORS và proxy trên local; chuẩn bị cấu hình/callback tương ứng Vercel–Railway. Kiểm tra deployment thực tế là bước được giao phát hành/thử nghiệm riêng, không tự deploy trong lượt triển khai local.
- Nếu cần chia nhỏ, thực hiện phần đủ review của bước 1, ghi phần còn lại rõ ràng; chưa gọi bước 1 hoàn thành khi Google hoặc khôi phục mật khẩu vẫn thiếu. Không kéo bước 2–5 vào lượt tài khoản.

### Bước 2 — Vòng thi và deadline

- Lưu season, GW, deadline và trạng thái `OPEN`, `LOCKED`, `AWAITING_RESULTS`, `PUBLISHED`.
- Kiểm tra giờ server tại mỗi lần chốt; không phụ thuộc đồng hồ trình duyệt hay cron chạy đúng giờ. Cách lưu/truy xuất trạng thái phải giữ đúng quy tắc ngay cả khi chưa có tác vụ nền đổi trạng thái.
- Đọc lịch GW6 và UTC đã lưu để xác định deadline thực tế trước khi mở cuộc thi. Không tự mở lại vòng hết hạn; hạn đã công bố không tự đổi theo lịch sync mới.
- Luồng quản trị điều chỉnh deadline phải có thông báo và ghi nhận thay đổi. Phân biệt dữ liệu cuộc thi chính thức với thử nghiệm/Replay GW1–GW5.

### Bước 3 — Đội nháp và đội đã chốt

- Lưu theo tài khoản trong MySQL để dùng trên nhiều thiết bị; mỗi user/season/GW chỉ có một đội dự thi có hiệu lực.
- Kiểm tra lại mọi luật bằng dữ liệu server khi chốt, không tin OVR/CLB/vị trí do client gửi. Giữ các ngoại lệ OVR/vị trí đã được người dùng xác nhận trong dữ liệu hiện có.
- Chốt lại atomically, xử lý hai request đồng thời, kiểm tra deadline trong luồng ghi; chỉ chủ tài khoản được sửa đội. Request không thành công không làm mất đội đã chốt trước đó.
- Lưu thông tin OVR, CLB, vị trí dùng lúc chốt; cập nhật hồ sơ sau này không đổi đội đã khóa. Quyết định khóa/transaction/phiên bản và cấu trúc lưu được chốt khi triển khai bước này.
- Tận dụng giao diện Fantasy hiện có; không tự chốt hoặc tự đưa lựa chọn cũ từ localStorage vào cuộc thi. Người chơi phải chọn vòng và chủ động chốt đội hợp lệ.

### Bước 4 — Chấm và công bố

- Nối rating đã nhập bằng [quy trình Gameweek](gameweek-data-workflow.md), đúng fixture/player_id/mùa/GW, không tự tìm rating hoặc thu thập lại chỉ số đã có.
- Có trạng thái phân biệt rating chưa nhập với người đã xác nhận không được chấm; không suy luận mọi NULL là 0. Chưa đủ trận/dữ liệu thì vòng tiếp tục AWAITING_RESULTS, không công bố một phần như kết quả cuối.
- Quản trị viên kiểm tra độ đầy đủ rồi tính/công bố; lưu điểm từng cầu thủ, tổng đội và phiên bản kết quả.
- Chạy lại không tạo trùng. Sửa rating sau công bố cần thao tác tính lại được ghi nhận, cập nhật cả BXH GW và mùa nhất quán. Không tự ghi đè thống kê bóng đá hoặc membership.

### Bước 5 — Kết quả và BXH

- Xem đội đã chốt, điểm từng người, tổng điểm và thứ hạng; bộ chọn GW, BXH mùa và xem đội người khác sau deadline.
- BXH mùa chỉ cộng các GW đã công bố; bằng điểm đồng hạng, không dùng OVR hoặc ID làm tiêu chí phụ để tách hạng. Cách hiển thị thứ tự trong nhóm đồng hạng không đổi hạng/điểm.
- Không lộ email, đội trước deadline hoặc nháp của người khác qua API dù giao diện đã ẩn; kiểm tra quyền ở backend.

### Bước 6 — Thử nghiệm và phát hành

- Kiểm tra chốt sát hạn/tại hạn, chốt đồng thời, chốt lỗi vẫn giữ đội cũ, quyền sở hữu đội, thiếu rating, người không được chấm và tính lại kết quả/BXH.
- Thử đăng nhập và chốt đội trên deployment thực tế khi được giao. Xác minh callback, cookie/proxy, nhiều thiết bị và thời điểm server.
- Dữ liệu thử phân biệt rõ với cuộc thi chính thức; GW1–GW5 chỉ Replay, không sửa thống kê đã biết. Migration production cần kế hoạch và backup trước khi thực hiện.

## Quyết định kỹ thuật bước tài khoản: đã chốt local và phần còn mở

Phần email/mật khẩu và phiên local đã chốt theo yêu cầu bước 1; các phần Google, khôi phục và phát hành còn mở được ghi riêng dưới đây. Không thay đổi luật sản phẩm phía trên. Không yêu cầu người dùng gửi credentials trong tin nhắn; chỉ cấu hình bằng env được ignore.

| Quyết định | Nội dung cần chọn/xác nhận trước khi triển khai phần liên quan |
| --- | --- |
| Cơ chế xác thực và duy trì đăng nhập | Đã chọn Spring Security và Spring Session JDBC, cookie HttpOnly `SESSION`, hết hạn sau 30 phút không hoạt động; login đổi mã phiên/CSRF, logout hủy phiên server. H2 file dùng thử local; production dùng MySQL hiện có sau migration/backup được giao, chưa thử restart/nhiều instance trên Railway. Không dùng JWT hoặc Redis. |
| Origin, cookie và proxy | Local Vite proxy cùng origin đã thử, SameSite=Lax. Production bật Secure nhưng Lax chưa phù hợp fetch khác site Vercel–Railway. Chốt proxy cùng origin/domain cùng site, hoặc None+Secure với CORS origin chính xác, credentials và CSRF; kiểm tra cookie bên thứ ba trên trình duyệt thực tế trước phát hành. Chi tiết ở auth-local-2026.md. |
| Google OAuth/OIDC | Đã có OAuth2 Client chuẩn, openid/email/profile, state/nonce/PKCE và callback `/api/auth/google/callback`; local callback localhost:8080, frontend localhost:5173. Env/Console ghi chính xác trong google-auth-2026.md. Máy kiểm tra chưa có credentials; chưa tạo client/đăng ký URI hoặc thử Google thật/deployment. |
| Liên kết Google với tài khoản email | Không gộp theo email. Đã có LINK từ account đang đăng nhập, Google xác minh → proof session 10 phút → POST xác nhận có CSRF; subject chỉ thuộc một account. Email trùng ở LOGIN yêu cầu đăng nhập account cũ rồi LINK; Google email chưa verified bị từ chối. Chưa có luồng thêm mật khẩu cho Google-only hoặc gỡ liên kết. |
| Mô hình tài khoản và chính sách dữ liệu | `accounts`: ID riêng, email strip/lowercase ROOT và unique SQL, tên 2–80 ký tự, hash, role, thời điểm UTC. Chỉ USER khi đăng ký email/Google, request/provider claim không cấp role. Không công khai email trên API tra cứu. Google-only có hash NULL; `account_identities` dùng provider/subject làm khóa duy nhất, FK cùng accounts.id. |
| Mật khẩu và lỗi đăng nhập | Đã dùng DelegatingPasswordEncoder với BCrypt mặc định, đăng ký tối thiểu 8 ký tự/tối đa 72 byte UTF-8; sai email và mật khẩu có cùng lỗi, không trả hash/credentials. Giới hạn tốc độ thử đăng nhập/đăng ký/reset còn phải làm trước phát hành. Google-only dùng OIDC; hash NULL bị từ chối khi dùng email/mật khẩu. |
| Khôi phục mật khẩu | Nhà cung cấp gửi email, địa chỉ gửi/domain, URL reset, thời hạn và sử dụng một lần; cách lưu token an toàn, thu hồi token cũ và session sau reset. Phải có kênh gửi thật để thử end-to-end; không coi in token ra log là tính năng khôi phục đã hoàn tất. |
| Tài khoản quản trị đầu tiên | Cách cấp admin có kiểm soát ngoài luồng đăng ký, xác nhận người quản trị và ghi nhận thao tác. Không tự cấp admin theo request, email tùy ý hay tài khoản đăng ký đầu tiên. |
| Migration và thứ tự phát hành | Local H2 có accounts, account_identities và hai bảng Spring Session trong auth-schema.sql. Profile prod giữ tắt tự chạy SQL (`spring.sql.init.mode=never`), cần migration MySQL riêng đã review và backup trước khi được giao nhập; không chạy DDL H2 nguyên xi trên MySQL. Chưa thêm reset schema/công cụ migration, chưa phát hành hoặc đặt env production. |

Email/mật khẩu + Google, logout, khôi phục mật khẩu, quyền người chơi/admin, trang công khai và không yêu cầu xác thực email trước khi chơi **đã chốt**, không mở lại thành câu hỏi về việc có cần các chức năng này hay không. Endpoint email/session có trong auth-local-2026.md, Google/env/danh tính có trong google-auth-2026.md; mail/reset vẫn chưa triển khai, không ghi tên dự kiến thành lệnh đang có.

## Cách làm việc, kiểm tra và báo cáo

- Chỉ đọc phần repo liên quan, không kiểm kê lại toàn bộ dữ liệu đã hoàn thành. Mỗi lượt triển khai một bước hoặc một phần đủ review; ghi đã làm, còn thiếu và bước tiếp theo trong tài liệu/checkpoint.
- Tái sử dụng validation, sân bóng, thẻ cầu thủ và quy trình rating hiện có. Không tự mở cuộc thi chính thức hoặc chuyển lựa chọn localStorage thành đội dự thi.
- Với mã thay đổi: test đúng phạm vi và build phần thay đổi một lần; mở rộng kiểm tra khi có lỗi/thay đổi mới cần xác minh. Không viết test riêng cho từng CLB/GW. Lượt chỉ tài liệu kiểm tra nội dung/liên kết/diff, không chạy application test/build.
- Credentials chỉ đặt trong env được ignore; không in secret, mật khẩu hoặc token reset/Google/session ra log, không đưa secret vào cấu hình frontend công khai. Dùng cấu hình hiện có làm căn cứ, không tự tạo credentials.
- Không tự commit, push, deploy hoặc ghi production. Khi cần migration production, trình bày schema/phạm vi, backup, kiểm tra và kế hoạch phát hành trước bước được giao thực hiện. Không tạo cron/Pre-deploy Command để thay kiểm tra deadline ở server.
- Mỗi lượt báo file cần commit, commit message, kiểm tra đã chạy/chưa chạy và giới hạn còn lại. Phần local và bước nhập SQL/phát hành phải tách rõ.

## Trạng thái bàn giao bước 1 — email và Google local

- Hoàn thành local: đăng ký, đăng nhập, đăng xuất, tài khoản hiện tại; hash BCrypt, email unique/chuẩn hóa, chỉ USER, phiên JDBC/cookie HttpOnly và CSRF; form cùng loading/lỗi trong PrismaXI. Không thay đội Fantasy trong trình duyệt.
- Kiểm tra lượt email trước: 49 test backend qua và Maven package thành công trên H2 cô lập; 15 test frontend qua, Vite build thành công. Chrome desktop 1440px/390px gọi server thật đã thử đăng ký → đăng nhập → reload → đăng xuất, lỗi trùng email/sai mật khẩu, API riêng 401 sau logout, cookie và giữ localStorage Fantasy. Không kiểm kê hoặc nhập lại dữ liệu bóng đá.
- Lần test backend đầu phát hiện cấu hình MVC slice cần Security Test starter của Boot 4 và mock service còn thiếu trong test CORS; đã sửa, chạy lại cùng phạm vi để xác nhận. Không chạy full suite, không tạo test cho từng GW/CLB.
- Lượt Google: đã triển khai OIDC chuẩn và bảng provider/subject liên kết cùng account, USER cho account Google mới, không gộp email; proof theo session và xác nhận có CSRF. Sau login dùng cùng principal/phiên/me/logout. Có nút Google/config-disabled, thông báo hủy/lỗi/cần LINK và return route nội bộ đã kiểm tra. 24 test backend scoped và 18 test frontend qua, build thành công; provider local ký JWT/HTTP xác minh state/nonce, liên kết và xử lý lỗi persistence không giữ principal OIDC tạm thời. Sau rà soát có thay đổi xử lý lỗi này nên kiểm tra lại đúng phạm vi. Chrome desktop/390px thử email và UI thông báo; không gọi là Google thật đầu cuối.
- Chưa hoàn thành: **Google thật chờ bạn cấu hình credentials/Console và thử**, khôi phục mật khẩu, giới hạn tốc độ trước phát hành, migration MySQL và cookie/domain Vercel–Railway. Bước 2–6, cuộc thi chính thức, deadline GW6 thực tế và kiểm tra deployment chưa triển khai. Bước 1 tổng thể còn phần tiếp theo.
- Chỉ ghi H2 thử local; không nối/ghi MySQL production, không tự commit/push/deploy. Schema bóng đá, membership, mùa 2024/25 và luật Fantasy giữ nguyên.
- File cần commit, biến env và Console của lượt Google: [google-auth-2026.md](google-auth-2026.md). AGENTS.md và application-prod.properties không sửa trong lượt này.
- Commit message lượt Google: `feat: add Google OIDC sign-in and confirmed account linking`.
- Lượt tiếp theo: **cấu hình OAuth client local và thử Google thật theo google-auth-2026.md**. Sau đó giao phần khôi phục mật khẩu riêng, cần chốt kênh gửi email và reset an toàn; chưa kéo chốt/chấm/BXH vào bước tài khoản.

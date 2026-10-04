# Context và kế hoạch Fantasy PrismaXI có tài khoản — 2026/27

## Mục tiêu và phạm vi lượt này

**Checkpoint bổ sung bước 3 — 05/10/2026:** đã quyết định và triển khai rosterAsOf riêng cho mỗi GW; mặc định ngày công bố theo giờ Việt Nam, cho phép quản trị chọn ngày có roster hiệu lực. Mốc cố định sau công bố; danh sách chọn, kiểm tra đội và lưu/chốt cùng đọc database; snapshot giữ nguyên. Migration H2/MySQL nâng cấp GW cũ bằng ngày công bố Việt Nam, chạy lại giữ mốc đã chọn. Đã kiểm tra đúng phạm vi và đo một tình huống 20 tài khoản lưu/chốt đồng thời trên MySQL local: không lỗi, thời gian truy vấn/lấy khóa GW cao nhất 1,76 giây. Không thay cơ chế khóa; chưa ghi/phát hành production. Chi tiết và file commit ở [fantasy-roster-reference-2026.md](fantasy-roster-reference-2026.md). Ghi nhận giữ mốc 2026-10-02 của checkpoint trước đã được thay bằng quyết định này trong multiplayer; phần khách luyện tập vẫn giữ hành vi cũ.

**Checkpoint bước 3 — 05/10/2026, hoàn thành local:** đã lưu nháp/đội chốt theo tài khoản, mùa và GW; kiểm tra luật bằng database, snapshot khi chốt, transaction và phiên bản chống cập nhật cũ. Giao diện đọc server khi reload/đổi GW, không nhập tự động lựa chọn localStorage; đăng xuất/đổi tài khoản xóa đội riêng khỏi giao diện. Kiểm tra đúng phạm vi, build và trình duyệt desktop/390px đã đạt. Migration đã thử hai lần trên MySQL local cô lập, chưa ghi production hoặc phát hành. Chi tiết API, kiểm tra, giới hạn và toàn bộ file cần commit ở [fantasy-user-lineups-2026.md](fantasy-user-lineups-2026.md). Mốc roster vẫn giữ 2026-10-02 theo validator hiện có; chưa chấm điểm/kết quả/BXH. Checkpoint bước 2 và các đoạn hiện trạng ban đầu dưới đây là lịch sử triển khai.

**Checkpoint mới nhất 05/10/2026:** tài khoản đã phát hành, người dùng xác nhận email/Google/liên kết/phiên hoạt động và vấn đề tốc độ đã xử lý; không lặp toàn bộ kiểm tra auth. **Bước 2 đã hoàn thiện local**: cấu hình GW/deadline UTC, audit quản trị, trạng thái theo Clock server và giao diện countdown. GW6 Arsenal–Leeds bắt đầu 10/10/2026 · 18:30 Việt Nam, deadline 09/10/2026 · 00:00 Việt Nam = 08/10/2026 · 17:00 UTC, còn hạn tại 05/10. Migration mới chưa nhập production, cuộc thi chưa tự mở. Chi tiết, API, kiểm tra, file commit và thứ tự phát hành ở [fantasy-gameweeks-2026.md](fantasy-gameweeks-2026.md). Các ghi nhận “chưa phát hành auth” phía dưới là lịch sử checkpoint trước, không phải trạng thái hiện tại. Khôi phục mật khẩu vẫn làm riêng.

Hoàn thiện Fantasy có tài khoản trước hạn chốt GW6 mùa 2026/27 để thử nghiệm với người chơi thật. Triển khai từng bước nhỏ, không làm toàn bộ trong một lượt. Chỉ phát triển mùa 2026/27, không sửa mùa 2024/25 hoặc thống kê bóng đá đã có.

Lượt tạo tài liệu ban đầu chỉ lưu context và kế hoạch. Bước 1 đã có **email/mật khẩu, Google OIDC, liên kết và giới hạn xác thực được kiểm chứng local**; reload/đăng nhập lại giữ ID, không tạo tài khoản trùng. Nay đã chuẩn bị **proxy cùng origin Vercel–Railway, cookie/Google callback và migration MySQL**, kiểm chứng trên MySQL 9.6/proxy HTTPS riêng; chưa phát hành hoặc đổi production. Xem [tài khoản](auth-local-2026.md), [Google](google-auth-2026.md), [phát hành](auth-release-2026.md). Khôi phục mật khẩu làm sau; chưa triển khai chốt đội, chấm điểm hoặc BXH người chơi. Deadline thực tế GW6 chưa được xác minh; không xem mục tiêu này là quyền mở cuộc thi hay mở lại vòng đã hết hạn.

## Hiện trạng và điểm đã đối chiếu trong repo

- Theo context người dùng: backend Spring Boot, frontend React/Vite, MySQL trên Railway, frontend trên Vercel. Lượt tài khoản chỉ kiểm tra H2 local, không đọc/ghi production hoặc thử deployment.
- [backend/pom.xml](../backend/pom.xml) giữ Java mục tiêu 21 và Spring Boot 4.1.1, Security/Session JDBC và starter kiểm thử Security theo BOM hiện có (Security 7.1.1, Session 4.1.1); thêm OAuth2 Client cho Google. Email, provider OIDC local và Google thật local đã qua kiểm tra; deployment chưa xác minh. Lượt giới hạn không thêm dependency.
- Đã có roster, OVR, primaryPosition và eligiblePositions. [FantasyLineupService](../backend/src/main/java/com/premierhub/service/FantasyLineupService.java) và [lineup.js](../frontend/src/fantasy/lineup.js) kiểm tra đúng vị trí, 11 người khác nhau, giới hạn CLB và OVR. Các sơ đồ Fantasy đang có là `4-2-1-3`, `4-3-3`, `4-4-2`, `3-5-2`; giữ nguyên các tên/ô này, không lấy sơ đồ đội hình trận thay luật Fantasy. LCB/RCB dùng quyền CB, LCM/RCM dùng quyền CM.
- [FantasyPage.jsx](../frontend/src/components/FantasyPage.jsx) lưu lựa chọn trong localStorage. `POST /api/fantasy/2026/validate` trong [FantasyController](../backend/src/main/java/com/premierhub/web/FantasyController.java) chỉ kiểm tra đội hình, chưa lưu đội dự thi hay xác nhận tham gia một GW. Mốc roster hiện có là `2026-10-02`; khi thiết kế vòng thi cần quyết định cách dùng mốc roster theo GW, không tự đổi dữ liệu/membership đang có.
- [schema.sql](../backend/src/main/resources/schema.sql) giữ nguyên các bảng bóng đá. [auth-schema.sql](../backend/src/main/resources/auth-schema.sql) dùng H2 local; [migration MySQL](../backend/sql/2026-10-04-accounts-mysql.sql) đã chuẩn bị và thử hai lần trên MySQL 9.6 riêng, không reset/copy tài khoản H2. Chưa nhập production hoặc có bảng đội dự thi/kết quả cuộc thi.
- Local: [vite.config.js](../frontend/vite.config.js) proxy `/api` tới `http://localhost:8080`. Production: [vercel.json](../frontend/vercel.json) chuẩn bị external proxy tới Railway, env-backed request secret và no-store. [auth.js](../frontend/src/api/auth.js) luôn dùng URL tương đối/credentials/CSRF; [request.js](../frontend/src/api/request.js) mặc định cùng origin, VITE_API_BASE_URL chỉ còn tùy chọn cho tra cứu công khai. Auth callback đặt trên frontend; route mới chưa deploy/kiểm chứng ở Vercel thật.
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

- **Checkpoint local:** email/mật khẩu, tên hiển thị, đăng xuất, `/me`, USER, CSRF, phiên JDBC, Google thật/LINK và rate limit đã đạt; không kiểm tra lại toàn bộ. Code/cấu hình phát hành và migration MySQL nay đã chuẩn bị/kiểm chứng local; deployment thật và khôi phục mật khẩu chưa làm. Bước 1 tổng thể chưa hoàn thành. Hướng dẫn ở google-auth-2026.md và auth-release-2026.md.
- Email/mật khẩu, Google, đăng xuất, tên hiển thị và khôi phục mật khẩu.
- Dùng cơ chế xác thực tiêu chuẩn của Spring Security, không tự thiết kế mã hóa/băm mật khẩu. Giải thích nhu cầu và phạm vi dependency trước khi thêm.
- Kiểm tra danh tính Google ở backend bằng cơ chế tiêu chuẩn; không tin email/tên/ID do frontend tự gửi. Liên kết tài khoản phải có xác nhận sở hữu phù hợp; không tự gộp tài khoản chỉ vì chuỗi email trùng nhau.
- Phân quyền người chơi/quản trị viên; không cho tự đăng ký quyền admin hoặc gửi role để nâng quyền. Email không công khai trên BXH.
- Các trang tra cứu công khai hiện tại vẫn dùng được khi chưa đăng nhập.
- Kiểm tra đăng nhập, đăng xuất, cookie/session hoặc token, CSRF/CORS và proxy trên local; chuẩn bị cấu hình/callback tương ứng Vercel–Railway. Kiểm tra deployment thực tế là bước được giao phát hành/thử nghiệm riêng, không tự deploy trong lượt triển khai local.
- Nếu cần chia nhỏ, thực hiện phần đủ review của bước 1, ghi phần còn lại rõ ràng; chưa gọi bước 1 hoàn thành khi Google hoặc khôi phục mật khẩu vẫn thiếu. Không kéo bước 2–5 vào lượt tài khoản.

### Bước 2 — Vòng thi và deadline

- **Hoàn thiện local:** hai bảng GW/audit; publication đóng băng hạn, adjustment ADMIN có CSRF/proxy/reason/revision; OPEN/LOCKED/AWAITING_RESULTS tính mỗi request bằng Clock, PUBLISHED dành riêng luồng kết quả chưa triển khai. GW chưa công bố không tham gia; GW1–GW5 chỉ Replay chưa mở. Có API/no-store, countdown theo giờ server và cập nhật lúc hết hạn, giữ lựa chọn đội trong trình duyệt.
- Kiểm tra: 19 test backend đúng phạm vi và đóng gói thành công bằng artifact kiểm tra riêng sau lỗi rename JAR Windows; 5 test frontend và build một lượt qua. Clock đọc sau SQL và test truy vấn qua deadline tránh quyết định OPEN cũ. Migration chạy hai lần trên MySQL 9.6 riêng giữ dữ liệu/constraints; không gọi H2 là kiểm chứng MySQL. Browser desktop/390px, đổi GW giữ đội và tự tải lại trạng thái LOCKED khi hết countdown đạt; chi tiết và giới hạn trong fantasy-gameweeks-2026.md.
- **Còn trước phát hành:** backup/migration hai bảng mới, deploy được giao, quyền quản trị có kiểm soát và thao tác công bố deadline rõ ràng. Chưa lưu/chốt đội, chấm điểm/BXH, nhập SQL production, commit/push/deploy; bước tiếp theo là bước 3 khi được giao.

- Lưu season, GW, deadline và trạng thái `OPEN`, `LOCKED`, `AWAITING_RESULTS`, `PUBLISHED`.
- Kiểm tra giờ server tại mỗi lần chốt; không phụ thuộc đồng hồ trình duyệt hay cron chạy đúng giờ. Cách lưu/truy xuất trạng thái phải giữ đúng quy tắc ngay cả khi chưa có tác vụ nền đổi trạng thái.
- Đọc lịch GW6 và UTC đã lưu để xác định deadline thực tế trước khi mở cuộc thi. Không tự mở lại vòng hết hạn; hạn đã công bố không tự đổi theo lịch sync mới.
- Luồng quản trị điều chỉnh deadline phải có thông báo và ghi nhận thay đổi. Phân biệt dữ liệu cuộc thi chính thức với thử nghiệm/Replay GW1–GW5.

### Bước 3 — Đội nháp và đội đã chốt

**Đã hoàn thành local:** ba bảng cho metadata, lựa chọn nháp và snapshot đội chốt; khóa database, expectedVersion, CSRF/session/kiểm tra proxy và quyền chủ tài khoản. Nháp có thể thiếu người; đội chốt đủ 11 người đúng sơ đồ/vị trí, tối đa 3 người/CLB, OVR không quá 860. Chỉnh nháp giữ nguyên đội chốt; chốt lại lỗi hoặc vượt deadline rollback toàn bộ. Clock được kiểm tra sau khi lấy khóa và trước/sau SQL ghi. API chỉ phục vụ chủ tài khoản, chưa mở xem đội người khác.

**Kiểm chứng:** 31 test backend và 15 test frontend đúng phạm vi đạt; build phần thay đổi đạt. Các sửa phát hiện trong kiểm tra được chạy lại đúng phần liên quan. Trình duyệt đã qua lưu nháp → chốt → sửa → chốt lại → reload, đổi GW, tab cũ xung đột, logout/đổi tài khoản và vòng khóa ở 390px. MySQL 9.6 local đã kiểm tra migration chạy lại, khóa/index/check/FK và rollback; đây chưa phải kiểm chứng service đầu cuối trên MySQL production.

**Còn lại:** backup/migration/phát hành khi được giao; chính sách roster theo GW đã hoàn thiện local trong checkpoint bổ sung. Khóa cấu hình GW vẫn tuần tự hóa các thao tác ghi cùng vòng; phép đo 20 tài khoản local đã ghi nhận hàng đợi, chưa thay khóa hoặc suy rộng sang tải production. Không tự mở lại vòng hết hạn. Bước tiếp theo là bước 4 khi người dùng yêu cầu, không tự bắt đầu chấm điểm.

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

Phần email/mật khẩu, Google và phiên đã chốt/kiểm chứng trên local theo yêu cầu bước 1; giới hạn tần suất nay đã có. Các phần khôi phục và phát hành còn mở được ghi riêng dưới đây. Không thay đổi luật sản phẩm phía trên. Không yêu cầu người dùng gửi credentials trong tin nhắn; chỉ cấu hình bằng env được ignore.

| Quyết định | Nội dung cần chọn/xác nhận trước khi triển khai phần liên quan |
| --- | --- |
| Cơ chế xác thực và duy trì đăng nhập | Đã chọn Spring Security và Spring Session JDBC, cookie HttpOnly `SESSION`, hết hạn sau 30 phút không hoạt động; login đổi mã phiên/CSRF, logout hủy phiên server. H2 file dùng thử local; production dùng MySQL hiện có sau migration/backup được giao, chưa thử restart/nhiều instance trên Railway. Không dùng JWT hoặc Redis. |
| Origin, cookie và proxy | Đã chọn `/api` cùng frontend origin qua Vercel external proxy, Secure/HttpOnly/host-only/Path=/Lax. AuthProxyFilter xác minh env secret, pin HTTPS/host và tin visitor IP của Vercel có proof; không bật global forwarding/trust mọi X-Forwarded-For. MySQL/proxy HTTPS local đã thử cookie/CSRF/logout/redirect; Vercel–Railway thật chưa thử. Chi tiết/biến/Console ở auth-release-2026.md. |
| Google OAuth/OIDC | Đã có OAuth2 Client chuẩn, openid/email/profile, state/nonce/PKCE và callback `/api/auth/google/callback`; local callback localhost:8080, frontend localhost:5173. Env/Console ghi chính xác trong google-auth-2026.md. Người dùng đã cấu hình credentials và kiểm chứng Google thật trên local; deployment chưa thử. Khởi tạo LOGIN/LINK dùng chung giới hạn IP. |
| Liên kết Google với tài khoản email | Không gộp theo email. Đã có LINK từ account đang đăng nhập, Google xác minh → proof session 10 phút → POST xác nhận có CSRF; subject chỉ thuộc một account. Email trùng ở LOGIN yêu cầu đăng nhập account cũ rồi LINK; Google email chưa verified bị từ chối. Chưa có luồng thêm mật khẩu cho Google-only hoặc gỡ liên kết. |
| Mô hình tài khoản và chính sách dữ liệu | `accounts`: ID riêng, email strip/lowercase ROOT và unique SQL, tên 2–80 ký tự, hash, role, thời điểm UTC. Chỉ USER khi đăng ký email/Google, request/provider claim không cấp role. Không công khai email trên API tra cứu. Google-only có hash NULL; `account_identities` dùng provider/subject làm khóa duy nhất, FK cùng accounts.id. |
| Mật khẩu và lỗi đăng nhập | Đã dùng DelegatingPasswordEncoder với BCrypt mặc định, đăng ký tối thiểu 8 ký tự/tối đa 72 byte UTF-8; sai email và mật khẩu có cùng lỗi, không trả hash/credentials. Đã thêm giới hạn login theo IP/email chuẩn hóa, register/Google start theo IP; 429 + Retry-After và thông báo Việt. Bộ đếm bounded từng JVM, reset khi restart, chưa chia sẻ nhiều instance. Reset chưa triển khai. Google-only dùng OIDC; hash NULL bị từ chối khi dùng email/mật khẩu. |
| Khôi phục mật khẩu | Nhà cung cấp gửi email, địa chỉ gửi/domain, URL reset, thời hạn và sử dụng một lần; cách lưu token an toàn, thu hồi token cũ và session sau reset. Phải có kênh gửi thật để thử end-to-end; không coi in token ra log là tính năng khôi phục đã hoàn tất. |
| Tài khoản quản trị đầu tiên | Cách cấp admin có kiểm soát ngoài luồng đăng ký, xác nhận người quản trị và ghi nhận thao tác. Không tự cấp admin theo request, email tùy ý hay tài khoản đăng ký đầu tiên. |
| Migration và thứ tự phát hành | Đã có migration MySQL 8.0.17+ cho 4 bảng auth/session, kiểm chứng hai lần trên MySQL 9.6 riêng và giữ dữ liệu/constraints. Profile prod giữ tắt SQL init, không H2 copy/reset. Còn đối chiếu version/schema Railway, backup ngoài Git và được giao nhập SQL/publish theo auth-release-2026.md; chưa đặt env/phát hành production. |

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
- Google thật đã được người dùng xác nhận trên local; đọc lại H2 có 3 tài khoản/3 danh tính Google riêng, không trùng email/provider-subject; tài khoản email ID 3 đã liên kết và có phiên còn hạn. Reload/đăng nhập lại giữ ID. Không chạy lại toàn bộ kiểm tra này khi thêm giới hạn.
- Chưa hoàn thành: khôi phục mật khẩu, nhập migration và kiểm chứng cookie/proxy/Google/session trên deployment Vercel–Railway thật, giới hạn dùng chung nhiều instance. Mã/cấu hình/migration đã chuẩn bị và kiểm chứng riêng, không coi là đã phát hành. Bước 2–6, cuộc thi chính thức và deadline GW6 thực tế chưa triển khai/xác minh.
- Chỉ ghi H2 thử local; không nối/ghi MySQL production, không tự commit/push/deploy. Schema bóng đá, membership, mùa 2024/25 và luật Fantasy giữ nguyên.
- File cần commit, biến env và Console của lượt Google: [google-auth-2026.md](google-auth-2026.md). AGENTS.md và application-prod.properties không sửa trong lượt này.
- Commit message lượt Google: `feat: add Google OIDC sign-in and confirmed account linking`.
- Lượt giới hạn xác thực: thêm cửa sổ theo IP/email, cleanup định kỳ và dung lượng tối đa, trust proxy tường minh; giữ JDBC session/HttpOnly/CSRF và LINK confirmation. Không thêm dịch vụ/dependency/bảng SQL. Cấu hình, hạn chế restart/multi-instance, lệnh kiểm tra và đầy đủ file cần commit trong auth-local-2026.md. Commit message: `feat(auth): rate limit registration and sign-in attempts`.
- Kiểm tra lượt giới hạn: 17 test backend trên H2 memory và Maven package thành công; 6 test frontend và Vite build thành công. Sửa lỗi biên dịch trong test đồng thời rồi chạy lại cùng phạm vi backend. Chrome desktop/390px qua 8 ca hiển thị 429 với phản hồi auth mock, không kiểm tra lại Google thật; không tràn ngang/lỗi JavaScript hay thay marker localStorage. Chi tiết và artifacts ở auth-local-2026.md.
- Lượt tiếp theo: **khôi phục mật khẩu trong lượt riêng**, cần chốt kênh gửi email và reset an toàn; chưa kéo chốt/chấm/BXH vào bước tài khoản. Trước phát hành phải giải quyết proxy/cookie/migration và giới hạn toàn cụm nếu dùng nhiều instance.

## Checkpoint chuẩn bị phát hành tài khoản — 04/10/2026

Đối chiếu sau phản hồi người dùng “mọi thứ đều OK”: Google thật production đã đạt, MySQL có 2 account USER/2 email/2 Google identity riêng, không trùng hoặc mồ côi; ID 1 không có phiên authenticated còn hạn, ID 2 có một phiên. Hai account đều Google-only; chưa có ca email–mật khẩu/LINK từ email production để đối chiếu độc lập, không tự tạo thêm tài khoản. Biên bản auth-production-2026-10-04.md là trạng thái mới nhất; các ghi nhận chờ thử dưới đây thuộc thời điểm trước phản hồi.

Checkpoint mới sau khi người dùng push/cho phép phát hành: [auth-production-2026-10-04.md](auth-production-2026-10-04.md). MySQL production đã được xác nhận, backup đủ 22 bảng và migration bốn bảng auth đã chạy hai lần. Vercel/Railway đã phát hành thành công đúng `446f044`, Google Console callback được người dùng xác nhận. Browser production kiểm chứng cookie/CSRF/logout vô danh và các trang công khai, Google redirect đúng origin/state/nonce/PKCE. Email/Google thật production còn chờ người dùng thao tác rồi đối chiếu ID/identity/session; baseline chưa có account. Không sửa mã hoặc chạy lại test/build local; không lặp migration khi tiếp tục.

- Chọn external proxy `/api` của Vercel với env secret transform, auth URL tương đối, cookie host-only Secure/HttpOnly/Lax và callback **https://premierhub.vercel.app/api/auth/google/callback**. Backend chỉ tin IP/HTTPS sau proof proxy; URL quay về vẫn frontend origin cố định + route nội bộ.
- Migration 4 bảng auth/session, không xóa/reset hoặc chép H2. MySQL 9.6 cô lập đã chạy hai lần, giữ dữ liệu và xác minh unique/FK/CHECK/cascade/subject exact. Chưa xác minh phiên bản/schema/permissions MySQL Railway.
- 13 test backend và package qua; 10 test frontend và Vite build qua. Chrome qua HTTPS proxy local + MySQL thật kiểm chứng cookie, CSRF, reload/logout, LOGIN/LINK callback hủy giữ ID/hash; marker Fantasy giữ nguyên. Không thử lại Google thật hoặc deployment. Test credentials/helpers ở target được ignore, không nhập dữ liệu bóng đá.
- Danh sách env Railway/Vercel, Console, backup/migration và thứ tự publish/file cần commit ở [auth-release-2026.md](auth-release-2026.md). Không đổi cấu hình dịch vụ, ghi SQL production, commit/push/deploy. Giai đoạn thử nghiệm đề xuất một replica; rate counters vẫn reset khi restart và không chia sẻ nhiều instance.
- Commit message: `feat(auth): prepare same-origin Vercel proxy and MySQL account deployment`.
- Bước tiếp theo khi được giao: cấu hình/migration/publish và kiểm tra trên deployment thật theo tài liệu, hoặc phần khôi phục mật khẩu riêng; không tự mở cuộc thi/Fantasy mới.

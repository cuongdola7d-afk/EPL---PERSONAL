# Bước 2 — Gameweek và deadline Fantasy 2026/27

**Bổ sung sau bước 3:** cấu hình GW có rosterAsOf cố định khi công bố, mặc định ngày mở theo giờ Việt Nam hoặc ngày quản trị chọn có dữ liệu. POST publish-deadline nhận thêm trường rosterAsOf tùy chọn dạng YYYY-MM-DD; bỏ trường này để dùng mặc định. GET thông tin GW trả mốc đã lưu; GET /api/fantasy/2026/gameweeks/{gameweek}/players trả roster tại mốc đó. Điều chỉnh deadline không đổi rosterAsOf. Cần migration bổ sung trước phát hành code; xem [fantasy-roster-reference-2026.md](fantasy-roster-reference-2026.md). Kiểm tra/checkpoint dưới đây ghi nhận lượt bước 2 trước phần bổ sung.

Checkpoint 05/10/2026: hoàn thiện mã local, migration và kiểm tra đúng phạm vi. Chưa nhập production, công bố deadline production hoặc phát hành. Không triển khai lưu/chốt đội, chấm điểm, BXH hoặc Replay; không thay luật Fantasy, dữ liệu bóng đá, membership hay mùa 2024/25. Phần auth đang dùng được giữ nguyên, không kiểm tra lại Google thật.

## Deadline GW6 đã xác minh

Đọc API production công khai, không đọc tài khoản hoặc ghi database:

`GET https://epl-personal-production.up.railway.app/api/matches?season=2026&matchweek=6`

- Đủ 10 fixture trong GW6, đều có kickoff UTC đầy đủ.
- Trận đầu: Arsenal FC – Leeds United FC, fixture `1000560593`, trạng thái SCHEDULED.
- Kickoff: **2026-10-10T11:30:00Z**, tức **thứ Bảy 10/10/2026 · 18:30, giờ Việt Nam**.
- Deadline: **thứ Sáu 09/10/2026 · 00:00, giờ Việt Nam**, tức **2026-10-08T17:00:00Z**.
- Tại ngày Việt Nam 05/10/2026, GW6 chưa hết hạn. Đây là deadline tính theo lịch, **chưa phải thông báo mở cuộc thi hoặc deadline đã ghi/công bố production**.
- Đã đọc thêm GW7 để có phương án thử tiếp: Everton FC – Chelsea FC, fixture `1000560603`, 17/10/2026 · 18:30 Việt Nam; deadline dự kiến 16/10/2026 · 00:00 Việt Nam = 2026-10-15T17:00:00Z. Không tự mở GW7.

Thuật toán: `kickoffUtc.atZone(Asia/Ho_Chi_Minh).toLocalDate().minusDays(1).atStartOfDay(Asia/Ho_Chi_Minh).toInstant()`. Dùng ngày lịch Việt Nam, không trừ 24 giờ từ kickoff. Date-only hoặc timestamp không parse được không tạo giờ 00:00 giả; không công bố nếu chưa đủ 10 fixture có thời điểm UTC.

## Dữ liệu và trạng thái

Hai bảng mới, độc lập với thống kê bóng đá:

- `fantasy_gameweeks`: khóa `(season, gameweek)`, `deadline_utc`, `deadline_published_at`, snapshot `first_fixture_id`/`first_kickoff_utc`, `workflow_status`, `results_published_at`, `revision`, `updated_at`.
- `fantasy_deadline_changes`: khóa `(season, gameweek, revision)`, hạn cũ/mới, thời điểm, account quản trị và lý do công khai. Publication đầu tiên cũng có lịch sử, hạn cũ NULL. API công khai không trả account ID/email người quản trị.

Migration [2026-10-05-fantasy-gameweeks-mysql.sql](../backend/sql/2026-10-05-fantasy-gameweeks-mysql.sql) chỉ CREATE TABLE IF NOT EXISTS, không seed/update/drop/reset bảng đã có. MySQL dùng DATETIME(6) biểu diễn UTC, session SQL UTC; schema H2 dùng TIMESTAMP(6). Ràng buộc chỉ mùa 2026, GW6–38, enum trạng thái, revision, FK lịch sử tới account và cấu hình. GW1–GW5 chỉ hiển thị Replay chưa mở; chưa tạo dữ liệu cuộc thi chính thức cho chúng.

`workflow_status` là trạng thái lưu; API và guard luôn trả/kiểm tra trạng thái hiệu lực với Clock server tại request. Không cần cron đổi row OPEN đúng giây:

| Trạng thái | Quy tắc |
| --- | --- |
| OPEN | Có deadline đã công bố, workflow còn OPEN và `serverNow < deadlineUtc`. |
| LOCKED | Tại deadline hoặc sau đó, trước thời điểm trận bắt đầu; không cho chỉnh/chốt đội. |
| AWAITING_RESULTS | Đã khóa và có fixture đang/đã đấu, hoặc đã tới kickoff của fixture SCHEDULED. Giữ trạng thái này khi tất cả trận FINISHED hoặc còn chờ trận/dữ liệu. |
| PUBLISHED | Chỉ dữ liệu do luồng công bố kết quả sau này ghi workflow PUBLISHED cùng results_published_at. Không có endpoint đặt trạng thái này trong bước 2. |

Chưa công bố cấu hình: `configured=false`, `status=null`, `deadlineUtc=null`, `canEdit=false`. `candidateDeadlineUtc` chỉ là dự kiến theo lịch đầy đủ. Không gán OPEN hoặc cho tham gia từ deadline dự kiến. `recommendedGameweek` chọn vòng còn hạn, đủ lịch, từ GW6; có thể là vòng chưa công bố, giao diện phải ghi rõ điều đó.

`requireOpen(gameweek)` từ chối GW chưa mở hoặc đã khóa bằng giờ server, kể cả row lưu còn OPEN. Bước 3 phải gọi guard bên trong transaction lưu/chốt, sau khi giữ lock cấu hình/đội và ngay trước thao tác ghi. Bước 2 chưa có API lưu/chốt để gắn guard; endpoint validate cũ vẫn chỉ kiểm tra đội hình local, không phải tham gia cuộc thi.

## API và quản trị

API đọc công khai, `Cache-Control: no-store`, không gửi cookie tài khoản:

- `GET /api/fantasy/2026/gameweeks`: `serverTimeUtc`, `recommendedGameweek`, 38 mục GW.
- `GET /api/fantasy/2026/gameweeks/{gw}`: cùng envelope, chỉ một mục; timestamp server dùng cùng lần đọc Clock quyết định trạng thái.

API ghi dùng cùng phiên auth hiện có, role ADMIN, cookie/CSRF, origin và proof proxy Vercel. Không cấp ADMIN cho tài khoản thật trong lượt này:

- `POST /api/fantasy/2026/admin/gameweeks/{gw}/publish-deadline`, body `{"reason":"Thông báo mở vòng ..."}`. Server tính hạn từ lịch đã lưu, ghi snapshot, publication và audit trong một transaction. Không nhận deadline/role/status từ client. Publication lặp trả 409, không đổi hạn cũ.
- `POST /api/fantasy/2026/admin/gameweeks/{gw}/adjust-deadline`, body `{"deadlineUtc":"2026-10-07T17:00:00Z","expectedRevision":1,"reason":"Lý do điều chỉnh công khai ..."}`. Lock row, kiểm tra revision, ghi audit atomically. Hạn mới khác hạn cũ, ở tương lai, trước kickoff snapshot; lý do 3–500 ký tự. Vòng đã khóa không được mở lại/điều chỉnh trong bước này. HTTP 409 cho xung đột/vòng khóa; 400 cho đầu vào sai.

Thay lịch sync không gọi hai API trên và không ghi lại deadline. Giao diện hiển thị lịch sử điều chỉnh như thông báo, giữ thời điểm publication ban đầu. Chưa có màn hình quản trị riêng hoặc gửi notification/email; thao tác rõ ràng hiện là API có xác thực.

GET overview đọc fixtures, cấu hình và audit theo ba query, không query từng GW và không tải JDBC session. POST admin mới được AuthProxyFilter kiểm tra như /api/auth; CSRF không được miễn cho các thao tác ghi này. Không thay cookie, Google/LINK, giới hạn auth hoặc cache phản hồi tài khoản.

## Giao diện

Khối “Vòng thi · 2026/27” trên Fantasy, độc lập với GW Đội hình tiêu biểu và lựa chọn đội đang lưu. Có chọn GW1–38, deadline Việt Nam, trạng thái, countdown, loading/lỗi/thử lại và thông báo chưa công bố.

Countdown dùng `serverTimeUtc` và thời gian trôi đơn điệu `performance.now()`, không dùng Date.now để quyết định khóa. Khi hết hạn không giữ nhãn OPEN: hiển thị đang xác nhận, gọi lại backend; lỗi thì báo/thử lại. Trở lại tab sau khi bị ẩn cũng tải lại trạng thái. Không thêm polling /me hoặc ghi đội dự thi/localStorage. Các thao tác chọn cầu thủ hiện có vẫn là bản nháp trong trình duyệt.

## Kiểm tra local

- H2 memory cô lập, Clock mock điều khiển: trước deadline một nanosecond còn OPEN; đúng/sau hạn LOCKED; guard từ chối dù row OPEN. Clock được đọc sau SQL: test truy vấn đi qua deadline phải trả LOCKED cùng timestamp mới. Chuyển ngày Việt Nam vượt nửa đêm và giữ nguyên ngày, không nhầm trừ 24 giờ.
- Lịch đổi sau publication không đổi hạn; adjustment có reason/revision/audit, không mở lại vòng khóa; rollback khi audit FK lỗi. Thiếu kickoff/date-only/thiếu fixture và Replay không tham gia.
- Trận FINISHED vẫn AWAITING_RESULTS; PUBLISHED chỉ đọc từ dữ liệu công bố có timestamp. Kiểm tra endpoint công khai, role/CSRF và proof proxy cho admin; mùa 2024 không có API mới và fixture sentinel giữ nguyên.
- **19 test backend đạt**, gồm GameweekIntegrationTest, FantasyControllerTest và AuthProxyFilterTest. Không chạy lại toàn bộ auth. Maven chạy bằng JDK 26 nhưng compile release 21, giữ nguyên POM/framework/dependencies.
- **5 test frontend đạt**, gồm gameweek.test.js và fantasy.test.js; frontend Vite build một lượt thành công.
- Lần package backend đầu lỗi Windows rename JAR sang .original. Đóng gói thành công bằng POM kiểm tra riêng trong target với classifier gameweek-check; không sửa POM nguồn. Chạy lại đúng phạm vi sau bổ sung test proxy/mùa cũ, và sau sửa đọc Clock sau truy vấn để tránh OPEN cũ khi SQL chờ qua deadline; không chạy full suite. Artifact tạm không commit.
- **MySQL 9.6.0 riêng**, loopback 33027, datadir mới dưới target/gameweek-check: migration hai lần, giữ cấu hình/audit và sentinel; unique, CHECK mùa/trạng thái và FK account được xác minh. Không dùng MySQL production hoặc chép H2 accounts.
- **Browser desktop 1440px và 390px đạt**: hiển thị 09/10/2026 · 00:00 Việt Nam và OPEN cho GW6, không tràn ngang (khối GW rộng 358px ở viewport 390px); đổi sang GW7 giữ nguyên localStorage Fantasy. Deadline thử ngắn hết hạn tạo request GW thứ hai và hiển thị LOCKED từ API; không lỗi JavaScript. Profile Chrome riêng; GW7 có deadline ngắn nhân tạo trong H2 memory để thử expiry, không phải deadline GW7 thật. Không sửa profile/trình duyệt của người dùng. Không chạy lại browser sau thay đổi Clock sau SQL; thay đổi đó được xác minh bằng test backend điều khiển thời gian. Đã dừng các process/database thử do lượt này tạo.

Artifacts kiểm tra, fixture API đã đọc, SQL seed và log nằm trong backend/target/gameweek-check, được ignore; không commit.

Lệnh kiểm tra bình thường từ backend (không cần lặp nếu code không đổi):

```powershell
mvn.cmd '-Dtest=GameweekIntegrationTest,FantasyControllerTest,AuthProxyFilterTest' package
```

Từ frontend:

```powershell
node --test src/fantasy/gameweek.test.js src/api/fantasy.test.js
npm.cmd run build
```

## Nhập SQL/phát hành còn lại

Chỉ khi được giao phát hành: xác nhận MySQL đích và backup ngoài Git, áp dụng migration hai bảng mới **trước** deploy backend có API mới. Profile prod vẫn sql.init.mode=never, không tạo bảng lúc khởi động. Không chạy seed thử/nhập H2 lên production. Nếu production chưa migration, API mới chưa thể hoạt động; không tự thay bằng deadline mở từ frontend.

Lệnh mẫu sau khi đã xác nhận endpoint/schema, credentials lấy từ cấu hình client ngoài Git; từ backend:

```powershell
Get-Content -Raw sql/2026-10-05-fantasy-gameweeks-mysql.sql | mysql.exe --defaults-extra-file=<file-client-ngoai-Git> --database=<database-da-xac-nhan>
```

MySQL DDL auto-commit. Chạy lại migration kiểm tra không tạo trùng và đối chiếu dữ liệu có sẵn; không drop bảng để rollback. Sau đó phát hành mã khi được phép, cấp quản trị qua quy trình có kiểm soát riêng và chủ động công bố GW phù hợp. Chưa nhập/configure/open production trong lượt này. Nếu đến lúc triển khai GW6 đã khóa, chọn GW tương lai; không mở lại.

## File cần commit

- backend/sql/2026-10-05-fantasy-gameweeks-mysql.sql
- backend/src/main/resources/fantasy-gameweek-schema.sql
- backend/src/main/resources/application.properties
- backend/src/main/java/com/premierhub/fantasy/GameweekConfiguration.java
- backend/src/main/java/com/premierhub/fantasy/GameweekRepository.java
- backend/src/main/java/com/premierhub/fantasy/GameweekService.java
- backend/src/main/java/com/premierhub/fantasy/GameweekController.java
- backend/src/main/java/com/premierhub/config/SecurityConfiguration.java
- backend/src/main/java/com/premierhub/config/ApiCorsConfiguration.java
- backend/src/main/java/com/premierhub/accounts/AuthProxyFilter.java
- backend/src/test/java/com/premierhub/fantasy/GameweekIntegrationTest.java
- backend/src/test/java/com/premierhub/accounts/AuthProxyFilterTest.java
- frontend/src/api/gameweeks.js
- frontend/src/fantasy/gameweek.js
- frontend/src/fantasy/gameweek.test.js
- frontend/src/components/FantasyGameweek.jsx
- frontend/src/components/FantasyGameweek.css
- frontend/src/components/FantasyPage.jsx
- docs/fantasy-gameweeks-2026.md
- docs/fantasy-multiplayer-2026-plan.md

Commit message: `feat(fantasy): add 2026 gameweek deadlines and contest states`.

Bước tiếp theo khi được giao: bước 3 — lưu nháp/đội chốt theo account/GW, khóa transaction và guard server ngay trước ghi, giữ đội chốt cũ nếu request lỗi. Không tự chuyển đội localStorage thành đội dự thi.

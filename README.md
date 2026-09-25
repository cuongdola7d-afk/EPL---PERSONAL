# PremierHub

PremierHub là dự án học Full-stack qua dữ liệu bóng đá. Giai đoạn 1 xây dựng lõi Java thuần; Giai đoạn 2 cung cấp API chỉ đọc bằng Spring Boot. Frontend React hiện có bốn trang tra cứu dùng API thật: câu lạc bộ, cầu thủ, lịch đấu/kết quả và bảng xếp hạng.

## Công nghệ hiện tại

- Java 21
- Maven Wrapper (không bắt buộc cài Maven)
- JUnit Jupiter
- Spring Boot 4.1.1, Spring Web MVC và Bean Validation
- API tra cứu đọc database: H2 file khi chạy local, MySQL khi chạy trên Railway; CSV chỉ còn cho console demo và bài học cũ
- React, JavaScript và Vite cho frontend

## Chức năng đã hoàn thành

- Model và validation cho `Club`, `Player`, `Match`, `Standing`.
- Tìm tên câu lạc bộ/cầu thủ không phân biệt hoa thường.
- Import câu lạc bộ, cầu thủ và trận đấu từ file UTF-8 CSV.
- Phân biệt trận `SCHEDULED` và `FINISHED`; tính hòa, thắng, thua và điểm.
- Lọc cầu thủ theo câu lạc bộ, tìm cầu thủ, lấy danh sách vua phá lưới.
- Tính bảng xếp hạng từ các trận đã kết thúc trong console demo; web API đọc BXH nhà cung cấp đã lưu trong database.
- Chương trình console demo và unit test cho model, CSV reader, service.
- API `GET` cho Club, Player, Match và Standing; JSON lỗi chung cho request không hợp lệ.
- Unit test, controller web slice test và Spring context integration test.
- Trang CLB tìm theo tên, lọc thành phố, sắp xếp A–Z/Z–A và làm mới dữ liệu.
- Trang cầu thủ lọc theo CLB/vị trí, sắp xếp bàn thắng và tính tổng bàn thắng/kiến tạo của kết quả lọc.
- Trang lịch đấu/kết quả lọc theo CLB, vòng đấu, trạng thái; trang BXH hiển thị thứ hạng và các chỉ số từ API.
- Cả bốn trang có trạng thái tải, dữ liệu rỗng và lỗi API.

## Cấu trúc chính

```text
backend/
├── mvnw, mvnw.cmd, .mvn/wrapper/
├── pom.xml
├── data/                         # CSV cho console demo
└── src/
    ├── main/java/com/premierhub/
    │   ├── config/, csv/, model/, repository/, service/, web/
    │   └── PremierHubApplication.java
    ├── main/resources/
    │   ├── application.properties, application-prod.properties
    │   └── data/                  # CSV đóng gói trong JAR
    └── test/java/com/premierhub/
docs/                               # tài liệu học theo từng bước
requests.http                        # request mẫu
frontend/                           # React + Vite; src/api/ gọi bốn API tra cứu
```

## Định dạng CSV

`clubs.csv`:

```csv
id,name,city
1,Arsenal,London
```

`players.csv`:

```csv
id,name,clubId,position,goals,assists
1,Sample Player,1,FORWARD,5,3
```

`position` nhận một trong: `GOALKEEPER`, `DEFENDER`, `MIDFIELDER`, `FORWARD`.

`matches.csv`:

```csv
id,homeClubId,awayClubId,matchweek,date,status,homeGoals,awayGoals
1,1,2,1,2025-08-16,FINISHED,2,1
2,2,3,2,2025-08-20,SCHEDULED,,
```

Ngày dùng định dạng `yyyy-MM-dd`. Trận `FINISHED` cần đủ hai tỉ số không âm; trận `SCHEDULED` phải để trống cả hai tỉ số.

Reader hiện dùng `split(",", -1)` để phục vụ bài học. Vì vậy mỗi trường không được chứa dấu phẩy, dấu ngoặc kép hoặc xuống dòng. Đây chưa phải bộ phân tích CSV tổng quát. Dữ liệu mẫu là dữ liệu minh họa cho việc học, không đại diện cho một mùa giải thật.

## Chạy và build backend

Cần **JDK 21** (hoặc JDK mới hơn có thể biên dịch cho Java 21). Không cần cài Maven vì repo có Maven Wrapper. Từ PowerShell trên Windows:

```powershell
cd backend
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Để build và chạy executable JAR:

```powershell
cd backend
.\mvnw.cmd clean package
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar
```

Trên Linux/macOS, dùng `./mvnw` thay cho `.\mvnw.cmd`. Ứng dụng chạy ở cổng 8080 khi không đặt biến môi trường `PORT`; ví dụ trên PowerShell, `$env:PORT=9090` đổi cổng sang 9090. Khi deploy, đặt `SPRING_PROFILES_ACTIVE=prod` để dùng cấu hình production. Không cần bật profile này khi chạy local.

Health check: `http://localhost:8080/actuator/health` (thay cổng nếu đã đặt `PORT`). Request mẫu ở [requests.http](requests.http). Web API đọc dữ liệu đã đồng bộ trong `backend/premierhub-local.mv.db` khi chạy local. Thư mục `backend/data` và CSV trong resources chỉ phục vụ console demo, CSV reader và test cũ; không nạp vào web API.

Luồng tra cứu: `API-Football → lệnh FootballSync → database → FootballQueries → controller → JSON → frontend`. Không có endpoint đồng bộ công khai.

## Chạy frontend cùng backend tại máy local

Cần Node.js tương thích với Vite (20.19+ hoặc 22.12+) và npm. Mở **hai terminal** từ thư mục gốc repo:

Terminal 1 — chạy backend trên cổng 8080:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Terminal 2 — cài dependency và chạy frontend:

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

Mở `http://localhost:5173/` (hoặc URL Vite in ra nếu cổng 5173 bận). Điều hướng bốn trang cũng mở trực tiếp bằng `#clubs`, `#players`, `#matches`, `#standings`. Trên Linux/macOS dùng `./mvnw` và `npm` thay cho `mvnw.cmd` và `npm.cmd`. `npm.cmd` giúp chạy npm trong PowerShell khi chính sách máy chặn script `npm.ps1`. Vite chuyển request `/api/...` từ frontend sang `http://localhost:8080`:

- CLB: `GET /api/clubs`, hoặc `/api/clubs/search?keyword=...` khi tìm tên; lọc thành phố và sắp xếp trên dữ liệu nhận được.
- Cầu thủ: `GET /api/players` với `club`, `position` khi chọn; sắp xếp và tính tổng trên kết quả nhận được.
- Lịch đấu: `GET /api/matches` với `club`, `matchweek`, `status` khi áp dụng bộ lọc.
- Chi tiết trận: `GET /api/matches/{id}/details?season=2024` trả `match`, `homePlayers`, `awayPlayers` từ database. Mỗi cầu thủ có thêm `score` gồm `status` (`COMPLETE`/`PROVISIONAL`), `confirmedPoints` và `parts` giải thích từng khoản điểm. Bấm **Xem chi tiết cầu thủ** ở trang Lịch đấu để mở thống kê và điểm; trận chưa có thống kê trả hai danh sách rỗng, ID không tồn tại trả 404. Giá trị `null` nghĩa là chưa có dữ liệu, không phải số 0.
- BXH: `GET /api/standings`; chỉ số lấy từ BXH nhà cung cấp và được đọc lại từ database theo mỗi request.

Các endpoint nhận `season` tùy chọn, mặc định `2024` (mùa 2024/25). Hiện chỉ mùa này được đồng bộ; bộ lọc Gameweek của trang Lịch đấu dùng `matchweek`. Cầu thủ có thể hiện nhiều dòng khi chuyển CLB vì thống kê mùa được lưu riêng cho từng CLB.

Điểm v1 theo cầu thủ–trận: ra sân 1–59 phút +1, từ 60 phút +2; mỗi bàn của thủ môn/hậu vệ/tiền vệ/tiền đạo lần lượt +10/+6/+5/+4; kiến tạo +3; thẻ vàng −1, thẻ đỏ −3. Service `MatchScoringService` áp dụng quy tắc này khi đọc chi tiết trận. Nếu một chỉ số cần thiết là `null`, khoản đó không được tính là 0: API đánh dấu `PROVISIONAL`, trả tổng các khoản đã xác định và giao diện báo **Tạm tính** hoặc **Chưa đủ dữ liệu**. Chưa tính giữ sạch lưới hay bonus; đây không phải điểm Fantasy cuối cùng.

Mười fixture Gameweek 1 (`1208021`–`1208030`) có file bằng chứng API-Football rút gọn trong `data/fixture-<id>-evidence.json`. Lệnh nhập một lần lưu ID bàn thắng/kiến tạo, người vào/ra sân và đội hình vào `fixture_score_evidence` theo khóa fixture ID. Trước khi lưu và suy luận, backend kiểm tra 40 ID, 11 đá chính + 9 dự bị mỗi đội, người vào sân có phút trong H2, bàn thắng/kiến tạo khớp dữ liệu dương và sự kiện bàn thắng khớp tỉ số. Bàn thường và bàn phạt đền cộng vào thống kê bàn thắng cầu thủ. Với phản lưới, cầu thủ phải thuộc đội đối phương của đội được ghi bàn; bàn chỉ cộng vào tỉ số đội hưởng, không cộng hay trừ điểm bàn thắng cho cầu thủ trong v1. Nếu kiểm tra sai, lệnh nhập dừng trước khi lưu và giữ nguyên bản ghi bằng chứng cũ nếu có. Khi đúng, `inferred.minutes/goals/assists` chứa riêng giá trị suy luận; các trường gốc trong `fixture_player_stats` và JSON chi tiết vẫn giữ `null`. Chạy lại cùng file cập nhật một dòng bằng chứng, không tạo dòng trùng. Lệnh local từ `backend/` (chọn file tương ứng):

```powershell
.\mvnw.cmd -q -DskipTests package
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --premierhub.fixture-evidence.enabled=true --premierhub.fixture-evidence.file=data/fixture-1208021-evidence.json
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --premierhub.fixture-evidence.enabled=true --premierhub.fixture-evidence.file=data/fixture-1208022-evidence.json
# Tương tự, chọn file data/fixture-<id>-evidence.json của các trận 1208023–1208030.
```

Lựa chọn CLB trong bộ lọc lấy từ Club API. Khi chạy bằng `npm.cmd run dev`, frontend luôn gọi `/api/...` qua Vite proxy, dù `VITE_API_BASE_URL` có được đặt trong môi trường local.

Nếu trang báo lỗi API, mở `http://localhost:8080/actuator/health` rồi thử trực tiếp `/api/clubs`, `/api/players`, `/api/matches` hoặc `/api/standings` trên cùng host. Nếu không phản hồi, kiểm tra terminal backend, JDK và biến `PORT` (local cần cổng 8080 để khớp proxy). Nếu backend trả dữ liệu nhưng frontend vẫn lỗi, xem tab Network trong Developer Tools để kiểm tra request `/api/...`, rồi xác nhận Vite đang chạy đúng cổng/đúng thư mục. Chạy `npm.cmd run build` trong `frontend/` để kiểm tra bản build; `frontend/dist/` và `frontend/node_modules/` được Git bỏ qua.

## Deploy frontend lên Vercel, dùng backend Railway

Frontend là ứng dụng Vite tĩnh; backend Spring Boot tiếp tục chạy trên Railway. Trước khi triển khai, kiểm tra URL backend Railway công khai qua `https://<railway-domain>/actuator/health` và `https://<railway-domain>/api/clubs`. URL này là **origin** (giao thức + host, có thể có cổng), không chứa `/api` hay đường dẫn khác. Repo không lưu sẵn domain Railway hoặc Vercel.

1. Sau khi review và đưa commit frontend/backend này lên GitHub, vào [Vercel Dashboard](https://vercel.com/new), chọn **Add New → Project**, kết nối GitHub nếu chưa kết nối. Khi cấp quyền cho Vercel GitHub App, chọn đúng repo riêng tư `EPL---PERSONAL` (hoặc chọn **Only select repositories** rồi cấp quyền cho repo đó), sau đó **Import** repo.
2. Trong **Configure Project**, chọn **Framework Preset: Vite** và **Root Directory: `frontend`** bằng nút **Edit**. **Build Command: `npm run build`**, **Output Directory: `dist`** (tính từ `frontend/`). Để Install Command mặc định của Vercel; dependency được cài theo `frontend/package-lock.json`. Không chọn `backend/` hay thư mục gốc làm Root Directory.
3. Trong **Environment Variables**, thêm `VITE_API_BASE_URL=https://<railway-domain>` cho **Production**; thêm cho **Preview** nếu muốn dùng bản preview. Không thêm `/api` ở cuối. Dấu `/` cuối URL vẫn được code xử lý. Biến `VITE_` được Vite đóng vào JavaScript gửi cho trình duyệt, nên chỉ đặt URL công khai, **không đặt token/mật khẩu**. Bấm **Deploy**.
4. Lấy domain production trong **Project → Domains** hoặc deployment production, ví dụ `https://<project>.vercel.app`. Trên Railway, vào service backend → **Variables**, đặt `PREMIERHUB_CORS_ALLOWED_ORIGINS=https://<project>.vercel.app`. Nếu cần nhiều domain, phân cách bằng dấu phẩy, ví dụ `https://<project>.vercel.app,https://<custom-domain>`. Đây là danh sách origin chính xác, không có `/` cuối và không dùng `*`. Deploy lại backend nếu Railway chưa tự tạo deployment khi biến thay đổi.
5. Sau khi backend Railway chạy với biến mới, mở domain Vercel và thử cả bốn trang: `/#clubs`, `/#players`, `/#matches`, `/#standings`. Thử tìm/lọc và kiểm tra tab **Network**: request phải đi tới `https://<railway-domain>/api/...`, trả JSON và có header `Access-Control-Allow-Origin` bằng đúng domain Vercel. Nếu đổi `VITE_API_BASE_URL` trên Vercel, cần tạo **deployment mới** vì biến được đóng vào bản build.

Backend luôn cho phép `http://localhost:5173` và `http://127.0.0.1:5173` khi gọi trực tiếp; local thông thường dùng Vite proxy. Nếu Vite chạy trên cổng khác và muốn gọi trực tiếp backend, thêm origin local đó vào `PREMIERHUB_CORS_ALLOWED_ORIGINS`.

Vercel tạo domain Preview riêng; domain này có thể khác domain Production. Muốn Preview gọi API, thêm **origin Preview cụ thể** vào `PREMIERHUB_CORS_ALLOWED_ORIGINS` trên Railway rồi redeploy backend. Nếu URL Preview thay đổi theo mỗi deployment, dùng một domain Preview/branch ổn định hoặc cập nhật từng origin cần thử. Không dùng `*.vercel.app` hay `*` để mở CORS cho mọi site.

Nếu trang production báo lỗi API: (1) kiểm tra trực tiếp `https://<railway-domain>/actuator/health` và `/api/clubs`; (2) trong Network xác nhận request dùng đúng Railway URL và không bị lỗi mixed content; (3) kiểm tra `Origin` của request có trong `PREMIERHUB_CORS_ALLOWED_ORIGINS` và response có `Access-Control-Allow-Origin`; (4) kiểm tra cả Vercel deployment mới và Railway deployment mới đã hoàn tất. Có thể kiểm tra CORS bằng lệnh sau, thay các domain bằng URL thật:

```powershell
curl.exe -i -H "Origin: https://<project>.vercel.app" "https://<railway-domain>/api/clubs"
curl.exe -i -H "Origin: https://other.example" "https://<railway-domain>/api/clubs"
```

Lệnh đầu phải có `Access-Control-Allow-Origin: https://<project>.vercel.app`; lệnh thứ hai phải bị từ chối và không có header này. Nguồn tham khảo: [Vercel monorepo](https://vercel.com/docs/monorepos), [Vercel GitHub](https://vercel.com/docs/git/vercel-for-github), [Vercel build](https://vercel.com/docs/builds/configure-a-build), [Vite env](https://vite.dev/guide/env-and-mode), [Spring MVC CORS](https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html).

## Nguồn dữ liệu 2024/25 và giới hạn Free

PremierHub dùng API-Football với `league=39`, `season=2024`. Lần kiểm chứng thực tế cho thấy 20 đội, 10 trận vòng 1, 20 dòng BXH, thống kê mùa cầu thủ và 40 dòng thống kê cầu thủ của một trận đã kết thúc. `GET /players?league=39&season=2024` báo 57 trang nhưng gói Free từ chối `page=4` vì **mỗi truy vấn chỉ truy cập tối đa 3 trang**. Lệnh đồng bộ chia truy vấn theo CLB; đội nào có hơn 3 trang vẫn thiếu một phần cầu thủ. Job ghi `truncatedClubIds` và `complete=false` trong trường hợp này. Không dùng dữ liệu giả để lấp chỗ thiếu. Những trường như bàn thắng, kiến tạo, phút thi đấu hoặc rating có thể là `null` trong response; database giữ `NULL` cho thống kê theo trận. Màn hình tổng mùa hiển thị `0` cho bàn thắng/kiến tạo thiếu để giữ cấu trúc JSON cũ.

Ảnh chụp H2 local ngày 25/09/2026, **không chạy đồng bộ thêm**: 20 CLB, 10 trận Gameweek 1, 400 bản ghi thống kê cầu thủ–trận và 20 dòng BXH cuối mùa (38 trận/đội). Bảng `players` có 432 cầu thủ do thống kê trận cung cấp, nhưng trang `/api/players` chỉ trả **63 dòng thống kê mùa cầu thủ–CLB** từ ba trang đầu của truy vấn toàn giải. Bournemouth, Brentford và Crystal Palace hiện chưa có dòng thống kê mùa nào trong H2. Trang Cầu thủ vì vậy chưa đủ dữ liệu để chọn đội Fantasy; kết quả lọc rỗng có thể chỉ là dữ liệu chưa được tải. BXH hiện có là BXH cuối mùa, không phải BXH sau Gameweek 1.

Đặt `API_FOOTBALL_KEY` trong môi trường backend hoặc file Git-ignored `backend/.env.local` với một dòng `API_FOOTBALL_KEY=...`. Key chỉ được đọc bởi backend/job, không đặt vào `VITE_`. Kiểm chứng theo Gameweek:

```powershell
cd backend
node scripts/verify-api-football.mjs 2024 1
```

Script kiểm tra đội, trận, BXH, trang cầu thủ đầu và thống kê cầu thủ của trận đã kết thúc, tối đa 8 request, cách nhau 6,5 giây. Không in key hoặc response đầy đủ. Trước đây Free đã từ chối mùa 2026/27; ứng dụng này cố ý dùng mùa 2024/25 có thể truy cập.

## Đồng bộ từng Gameweek và database

Chạy từ `backend/` sau khi đã build JAR. Lệnh sau đồng bộ vòng 1 và thoát; có thể chạy lại an toàn:

```powershell
.\mvnw.cmd -q -DskipTests package
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --premierhub.sync.enabled=true --premierhub.sync.season=2024 --premierhub.sync.gameweek=1 --premierhub.sync.budget=70
```

Vòng tiếp theo: đổi `--premierhub.sync.gameweek=2`. Mỗi lần chạy chỉ tải một Gameweek; nếu hết ngân sách request, job giữ checkpoint và in `complete=false`, chạy lại sau khi quota được cấp. `--premierhub.sync.budget` giới hạn tối đa 90 request/lần, mặc định 70; job còn giữ tối thiểu 5 request trong quota ngày và giãn các request 6,5 giây. Job lấy lại danh sách trận của Gameweek để phát hiện trạng thái/tỉ số thay đổi, nhưng bỏ qua thống kê trận đã có nếu fingerprint trận chưa đổi. Nếu nhà cung cấp sửa thống kê cầu thủ mà không đổi trận, chạy cùng Gameweek với `--premierhub.sync.refresh-stats=true`. Muốn cập nhật lại thống kê tổng mùa đã lưu, thêm `--premierhub.sync.refresh-players=true` khi còn quota. Không cần chạy lại toàn bộ mùa.

Local dùng H2 file `backend/premierhub-local.mv.db`, giữ dữ liệu qua lần chạy. Schema nằm trong `backend/src/main/resources/schema.sql`, tự tạo bảng còn thiếu lúc khởi động. Các bảng chính là `seasons`, `clubs`, `season_clubs`, `players`, `player_season_stats`, `fixtures`, `standings`, `fixture_player_stats`, `sync_states`. ID đội, cầu thủ và trận dùng ID của nhà cung cấp. Thống kê cầu thủ theo trận có khóa `(fixture_id, player_id)`, lưu phút, bàn thắng, kiến tạo, thẻ, rating và các chỉ số sẵn có khác, cùng JSON gốc để xử lý thêm về sau. Chi tiết trận đã có điểm v1; chưa có giao diện Fantasy.

### Railway

Không dùng H2 file trên Railway vì storage của web service có thể không bền qua deploy. Trên tài khoản Railway, bạn cần tự thêm MySQL (xem chi phí/gói dịch vụ trước khi tạo), rồi đặt các biến sau cho **web service** và **Cron service** của backend:

- `SPRING_PROFILES_ACTIVE=prod`
- `PREMIERHUB_JDBC_URL=jdbc:mysql://<MYSQLHOST>:<MYSQLPORT>/<MYSQLDATABASE>` (điền bằng Railway reference variables tới MySQL cùng project)
- `PREMIERHUB_DB_USER=<MYSQLUSER>` và `PREMIERHUB_DB_PASSWORD=<MYSQLPASSWORD>` (dùng reference variables, không ghi mật khẩu vào Git)
- `API_FOOTBALL_KEY=<key>` chỉ cho Cron service; web service không cần key để tra cứu
- Giữ `PREMIERHUB_CORS_ALLOWED_ORIGINS` cho domain Vercel như phần trên.

Nếu về sau dùng Cron để đồng bộ vòng khác, build cùng source `backend/` và đặt Start Command như lệnh Java ở trên (thay Gameweek và giữ `--spring.main.web-application-type=none`), đặt Cron Schedule theo UTC tùy ngày muốn chạy. Job phải chạy xong rồi thoát, không chạy scheduler trong web service Serverless. Xem log `SYNC ...` để biết request, Gameweek và checkpoint. Để đưa bản GW1 hiện có lên production, dùng **snapshot** bên dưới, không chạy đồng bộ API-Football.

Sau deploy, thử `https://<railway-domain>/api/clubs?season=2024`, `/api/players?season=2024`, `/api/matches?season=2024&matchweek=1`, `/api/standings?season=2024`; rồi mở bốn trang trên Vercel. Nếu API trả mảng rỗng, kiểm tra Cron đã dùng cùng database với web service và đã in kết quả sync.

## Đưa bản tra cứu Gameweek 1 mùa 2024/25 lên database

Snapshot `backend/src/main/resources/data/gw1-2024-snapshot.json` được xuất từ H2 local đã kiểm chứng; ứng dụng production đọc snapshot đóng trong JAR, **không cần file H2 local hoặc API-Football key**. Snapshot có 20 CLB, 432 hồ sơ cầu thủ, 63 dòng thống kê mùa, 10 trận, 20 dòng BXH cuối mùa, 400 dòng cầu thủ–trận và 10 bằng chứng tính điểm. Trang Cầu thủ chỉ hiển thị 63 dòng thống kê mùa hiện có; dữ liệu mùa này chưa đầy đủ. Giá trị `NULL` của provider được giữ nguyên. Trang Lịch đấu ghi rõ GW1 2024/25; BXH ghi rõ đó là **BXH cuối mùa 2024/25**, không phải BXH sau GW1.

Nhập vào một database **đã tạo hoặc đang dùng** (không xóa bảng/dòng cũ). Dùng đúng JDBC URL và credentials của database mà **web service Railway** sử dụng:

```powershell
cd backend
.\mvnw.cmd -q -DskipTests package
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --premierhub.snapshot.mode=import
```

Lệnh đọc `PREMIERHUB_JDBC_URL`, `PREMIERHUB_DB_USER`, `PREMIERHUB_DB_PASSWORD` từ môi trường, in số dòng mới ở mỗi bảng cùng `verifiedFixtures=10`, rồi thoát. Trên Railway, đặt tạm **Pre-deploy Command** cho backend service trong environment `production`:

```text
java -jar /app/target/premierhub-backend-0.1.0-SNAPSHOT.jar --spring.main.web-application-type=none --premierhub.snapshot.mode=import
```

Pre-deploy chạy trong private network với biến của backend, trước khi web service nhận traffic; nó ghi vào MySQL, không dùng volume của web container. Backend cần `SPRING_PROFILES_ACTIVE=prod` và ba biến `PREMIERHUB_JDBC_URL`, `PREMIERHUB_DB_USER`, `PREMIERHUB_DB_PASSWORD` tham chiếu tới service MySQL. Kiểm tra log `GW1 SNAPSHOT ... verifiedFixtures=10`; chạy lại deployment phải in số dòng mới bằng `0`. **Gỡ Pre-deploy Command sau khi đã kiểm tra**, để lần deploy sau không phụ thuộc snapshot lịch sử. Không đặt API key trong frontend hoặc Git. Nếu một bản ghi cùng ID đã có nhưng nội dung khác, lệnh dừng và rollback thay vì ghi đè dữ liệu production; các bảng khác và mùa khác không bị xóa.

Sau khi job hoàn tất, kiểm tra `/api/clubs?season=2024` (20 dòng), `/api/players?season=2024` (63 dòng), `/api/matches?season=2024&matchweek=1` (10 dòng), `/api/standings?season=2024` (20 dòng), và `/api/matches/1208021/details?season=2024` đến `1208030` (`VERIFIED`, 40/40 `COMPLETE` mỗi trận). Kiểm tra bốn trang và mở chi tiết các trận tại domain Vercel; Vercel cần root directory `frontend`, build `npm run build`, output `dist` và `VITE_API_BASE_URL` là origin Railway công khai. Nếu web còn hiển thị dữ liệu cũ, kiểm tra Vercel deployment đã lấy commit mới và cả job lẫn web đang kết nối cùng database.

## Quy tắc bảng xếp hạng

Web API dùng hạng/điểm/hiệu số do API-Football trả về và đọc database mỗi request. Bộ tính BXH từ trận (thắng 3, hòa 1) chỉ còn trong console demo và test Java cũ.

Các ví dụ học tập đã hoàn thiện và test tương ứng nằm trong [docs/LEARNING_TASKS.md](docs/LEARNING_TASKS.md).

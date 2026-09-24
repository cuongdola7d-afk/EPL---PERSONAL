# PremierHub

PremierHub là dự án học Full-stack qua dữ liệu bóng đá. Giai đoạn 1 xây dựng lõi Java thuần; Giai đoạn 2 cung cấp API chỉ đọc bằng Spring Boot. Frontend React hiện có bốn trang tra cứu dùng API thật: câu lạc bộ, cầu thủ, lịch đấu/kết quả và bảng xếp hạng.

## Công nghệ hiện tại

- Java 21
- Maven Wrapper (không bắt buộc cài Maven)
- JUnit Jupiter
- Spring Boot 4.1.1, Spring Web MVC và Bean Validation
- Dữ liệu CSV trong bộ nhớ; chưa dùng database
- React, JavaScript và Vite cho frontend

## Chức năng đã hoàn thành

- Model và validation cho `Club`, `Player`, `Match`, `Standing`.
- Tìm tên câu lạc bộ/cầu thủ không phân biệt hoa thường.
- Import câu lạc bộ, cầu thủ và trận đấu từ file UTF-8 CSV.
- Phân biệt trận `SCHEDULED` và `FINISHED`; tính hòa, thắng, thua và điểm.
- Lọc cầu thủ theo câu lạc bộ, tìm cầu thủ, lấy danh sách vua phá lưới.
- Tính bảng xếp hạng từ các trận đã kết thúc.
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

Health check: `http://localhost:8080/actuator/health` (thay cổng nếu đã đặt `PORT`). Request mẫu ở [requests.http](requests.http). Dữ liệu API nằm trong `backend/src/main/resources/data`, được nạp từ classpath lúc ứng dụng khởi động và hiện chỉ đọc. File trong `backend/data` phục vụ console demo.

Luồng Club API: `HTTP → ClubController → ClubService → ClubRepository → ClubResponse → JSON`. Các API khác dùng cùng dữ liệu Club và CSV tương ứng trong bộ nhớ.

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
- BXH: `GET /api/standings`; chỉ số được tính từ các trận đã kết thúc.

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

## Quy tắc bảng xếp hạng

Thắng 3 điểm, hòa 1 điểm, thua 0 điểm. Chỉ trận `FINISHED` được tính. Thứ tự lần lượt theo điểm giảm dần, hiệu số giảm dần, bàn thắng giảm dần và tên câu lạc bộ tăng dần.

Các ví dụ học tập đã hoàn thiện và test tương ứng nằm trong [docs/LEARNING_TASKS.md](docs/LEARNING_TASKS.md).

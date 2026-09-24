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

Lựa chọn CLB trong bộ lọc lấy từ Club API. Frontend chưa deploy và chưa cấu hình URL Railway.

Nếu trang báo lỗi API, mở `http://localhost:8080/actuator/health` rồi thử trực tiếp `/api/clubs`, `/api/players`, `/api/matches` hoặc `/api/standings` trên cùng host. Nếu không phản hồi, kiểm tra terminal backend, JDK và biến `PORT` (local cần cổng 8080 để khớp proxy). Nếu backend trả dữ liệu nhưng frontend vẫn lỗi, xem tab Network trong Developer Tools để kiểm tra request `/api/...`, rồi xác nhận Vite đang chạy đúng cổng/đúng thư mục. Chạy `npm.cmd run build` trong `frontend/` để kiểm tra bản build; `frontend/dist/` và `frontend/node_modules/` được Git bỏ qua.

## Quy tắc bảng xếp hạng

Thắng 3 điểm, hòa 1 điểm, thua 0 điểm. Chỉ trận `FINISHED` được tính. Thứ tự lần lượt theo điểm giảm dần, hiệu số giảm dần, bàn thắng giảm dần và tên câu lạc bộ tăng dần.

Các ví dụ học tập đã hoàn thiện và test tương ứng nằm trong [docs/LEARNING_TASKS.md](docs/LEARNING_TASKS.md).

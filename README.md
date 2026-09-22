# PremierHub

PremierHub là dự án học Full-stack qua dữ liệu bóng đá. Giai đoạn 1 xây dựng lõi Java thuần; Giai đoạn 2 cung cấp API chỉ đọc cho câu lạc bộ, cầu thủ, trận đấu và bảng xếp hạng bằng Spring Boot.

## Công nghệ hiện tại

- Java 21
- Maven Wrapper (không bắt buộc cài Maven)
- JUnit Jupiter
- Spring Boot 4.1.1, Spring Web MVC và Bean Validation
- Dữ liệu CSV trong bộ nhớ; chưa dùng database

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

## Chạy và build

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

## Quy tắc bảng xếp hạng

Thắng 3 điểm, hòa 1 điểm, thua 0 điểm. Chỉ trận `FINISHED` được tính. Thứ tự lần lượt theo điểm giảm dần, hiệu số giảm dần, bàn thắng giảm dần và tên câu lạc bộ tăng dần.

Các ví dụ học tập đã hoàn thiện và test tương ứng nằm trong [docs/LEARNING_TASKS.md](docs/LEARNING_TASKS.md).

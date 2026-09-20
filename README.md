# PremierHub

PremierHub là dự án học Full-stack qua dữ liệu bóng đá. Giai đoạn 1 xây dựng lõi Java thuần; bước đầu Giai đoạn 2 đưa lõi đó vào Spring Boot và cung cấp API chỉ đọc cho câu lạc bộ.

## Công nghệ hiện tại

- Java 21
- Maven
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
- API `GET` cho danh sách câu lạc bộ, câu lạc bộ theo ID và tìm kiếm theo tên.
- Spring context test và MockMvc test cho Club API.

## Cấu trúc chính

```text
backend/
├── data/
│   ├── clubs.csv
│   ├── players.csv
│   └── matches.csv
├── pom.xml
└── src/
    ├── main/java/com/premierhub/
    │   ├── App.java
    │   ├── PremierHubApplication.java
    │   ├── config/ClubDataConfiguration.java
    │   ├── csv/
    │   │   ├── ClubCsvReader.java
    │   │   ├── MatchCsvReader.java
    │   │   └── PlayerCsvReader.java
    │   ├── model/
    │   │   ├── Club.java
    │   │   ├── Match.java
    │   │   ├── MatchStatus.java
    │   │   ├── Player.java
    │   │   ├── Position.java
    │   │   └── Standing.java
    │   ├── service/
    │       ├── LeagueTableService.java
    │       └── PremierHubService.java
    │   └── web/
    │       ├── ClubController.java
    │       └── dto/ClubResponse.java
    ├── main/resources/data/clubs.csv
    └── test/java/com/premierhub/
        ├── csv/
        ├── model/
        └── service/
docs/
├── LEARNING_TASKS.md
├── PROGRESS.md
└── phase-1.md
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
id,homeClubId,awayClubId,date,status,homeGoals,awayGoals
1,1,2,2025-08-16,FINISHED,2,1
2,2,3,2025-08-20,SCHEDULED,,
```

Ngày dùng định dạng `yyyy-MM-dd`. Trận `FINISHED` cần đủ hai tỉ số không âm; trận `SCHEDULED` phải để trống cả hai tỉ số.

Reader hiện dùng `split(",", -1)` để phục vụ bài học. Vì vậy mỗi trường không được chứa dấu phẩy, dấu ngoặc kép hoặc xuống dòng. Đây chưa phải bộ phân tích CSV tổng quát. Dữ liệu mẫu là dữ liệu minh họa cho việc học, không đại diện cho một mùa giải thật.

## Chạy project

Cần JDK 21 trở lên; Maven biên dịch với Java release 21.

```powershell
cd backend
mvn clean test
mvn spring-boot:run
```

Ứng dụng chạy tại `http://localhost:8080`. Các endpoint hiện có:

- `GET /api/clubs`
- `GET /api/clubs/{id}`
- `GET /api/clubs/search?keyword=united`

Các request mẫu nằm trong `docs/api-requests.http`. API đọc `src/main/resources/data/clubs.csv` từ classpath, nên resource hoạt động khi chạy trong IDE lẫn JAR. File được đọc một lần lúc tạo application context.

`Club` là model nghiệp vụ: nó giữ dữ liệu hợp lệ và logic `matchesName`. `ClubResponse` là DTO của HTTP API: record này xác định đúng các field JSON mà client được nhận. Mapping dùng Java thông thường, không dùng mapper framework.

Luồng request: `HTTP → ClubController → PremierHubService → danh sách Club đã nạp từ CSV → ClubResponse → JSON`.

## Quy tắc bảng xếp hạng

Thắng 3 điểm, hòa 1 điểm, thua 0 điểm. Chỉ trận `FINISHED` được tính. Thứ tự lần lượt theo điểm giảm dần, hiệu số giảm dần, bàn thắng giảm dần và tên câu lạc bộ tăng dần.

Các ví dụ học tập đã hoàn thiện và test tương ứng nằm trong [docs/LEARNING_TASKS.md](docs/LEARNING_TASKS.md).

# PremierHub

PremierHub là dự án học Full-stack qua dữ liệu bóng đá. Sprint 1 xây dựng lõi Java thuần trước khi chuyển sang Spring Boot: đọc CSV, kiểm tra dữ liệu, tìm kiếm câu lạc bộ/cầu thủ, thống kê cầu thủ và tính bảng xếp hạng.

## Công nghệ Sprint 1

- Java 21
- Maven
- JUnit Jupiter
- Java standard library; chưa dùng Spring Boot hoặc database

## Chức năng đã hoàn thành

- Model và validation cho `Club`, `Player`, `Match`, `Standing`.
- Tìm tên câu lạc bộ/cầu thủ không phân biệt hoa thường.
- Import câu lạc bộ, cầu thủ và trận đấu từ file UTF-8 CSV.
- Phân biệt trận `SCHEDULED` và `FINISHED`; tính hòa, thắng, thua và điểm.
- Lọc cầu thủ theo câu lạc bộ, tìm cầu thủ, lấy danh sách vua phá lưới.
- Tính bảng xếp hạng từ các trận đã kết thúc.
- Chương trình console demo và unit test cho model, CSV reader, service.

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
    │   └── service/
    │       ├── LeagueTableService.java
    │       └── PremierHubService.java
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
mvn compile exec:java
```

Lệnh demo mặc định đọc thư mục `backend/data`. Có thể truyền thư mục dữ liệu khác:

```powershell
mvn compile exec:java -Dexec.args="D:\duong-dan\data"
```

Đường dẫn chỉ được truyền khi chạy; source code không hard-code đường dẫn máy cá nhân.

## Quy tắc bảng xếp hạng

Thắng 3 điểm, hòa 1 điểm, thua 0 điểm. Chỉ trận `FINISHED` được tính. Thứ tự lần lượt theo điểm giảm dần, hiệu số giảm dần, bàn thắng giảm dần và tên câu lạc bộ tăng dần.

Các bài tự luyện tiếp theo nằm trong [docs/LEARNING_TASKS.md](docs/LEARNING_TASKS.md).

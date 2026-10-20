# PremierHub — Hướng dẫn cộng tác

## Mục tiêu và bối cảnh

PremierHub là dự án học Full-stack qua thực hành, với các chức năng đích:
- Tra cứu lịch đấu và bảng xếp hạng Premier League.
- Thống kê đội bóng và cầu thủ.
- Tài khoản người dùng và đội bóng yêu thích.
- Bình luận và cộng đồng.

Người học đã biết Python, Java, OOP và SQL/MySQL cơ bản; chưa học lập trình web và Spring Boot. Giải thích bằng tiếng Việt, gắn lý thuyết trực tiếp với code và ví dụ nhỏ.

## Công nghệ

- Hiện tại: Java 21, Maven, JUnit Jupiter và Spring Boot 4.1.1.
- Maven coordinates: `com.premierhub:premierhub-backend`, phiên bản hiện tại `0.1.0-SNAPSHOT`.
- Backend về sau: Spring Boot; database: MySQL.
- Frontend: bắt đầu với HTML/CSS/JavaScript, sau đó React.
- Dữ liệu ban đầu: import CSV.
- Chưa tự ý thêm framework, database hoặc dependency khi chưa giải thích nhu cầu và kế hoạch.

## Cấu trúc hiện tại

```text
/
├── AGENTS.md
├── README.md
├── .gitignore
├── docs/phase-1.md
├── .vscode/
│   └── settings.json
└── backend/
    ├── pom.xml
    ├── data/
    │   ├── clubs.csv
    │   ├── players.csv
    │   └── matches.csv
    └── src/
        ├── main/java/com/premierhub/
        │   ├── App.java
        │   ├── PremierHubApplication.java
        │   ├── config/ClubDataConfiguration.java
        │   ├── csv/
        │   ├── model/
        │   ├── service/
        │   └── web/
        ├── main/resources/data/clubs.csv
        └── test/java/com/premierhub/
            ├── AppTest.java
            ├── csv/ClubCsvReaderTest.java
            └── model/ClubTest.java
```

Giai đoạn 1 đã hoàn thành lõi Java/CSV cho Club, Player, Match và Standing. Giai đoạn 2 có API chỉ đọc cho Club, Player và Match; các CSV tương ứng được tải từ classpath một lần khi khởi động. Player và Match API dùng các lớp riêng trong `config/`, `repository/`, `service/`, `web/` và `web/dto/`. Tài liệu ở `docs/stage-2-player-api.md` và `docs/stage-2-match-api.md`, request mẫu ở `requests.http`. Chưa có API cho Standing, frontend hoặc database. `backend/target/` là đầu ra build, không phải mã nguồn để commit.

## Quy tắc code

- Viết code tương thích Java 21; phân biệt JDK đang chạy Maven với release mục tiêu trong POM.
- Dùng UTF-8, thụt lề Java 4 dấu cách và cách đặt dấu ngoặc nhất quán.
- Tên package viết thường dưới `com.premierhub`; lớp dùng PascalCase, phương thức và biến dùng camelCase, hằng số dùng UPPER_SNAKE_CASE.
- Dùng tên tiếng Anh có nghĩa cho định danh; ưu tiên lớp nhỏ, trách nhiệm rõ ràng và giải pháp dễ hiểu với người mới học web.
- Tách nghiệp vụ khỏi nhập/xuất console, đọc CSV và truy cập database khi các phần này được triển khai; không tạo sẵn kiến trúc phức tạp chưa cần thiết.
- Khi xử lý dữ liệu đầu vào, kiểm tra điều kiện hợp lệ và báo lỗi rõ ràng; không nuốt exception.
- Test dùng JUnit Jupiter, đặt trong `backend/src/test/java` với package tương ứng. Test hành vi có ý nghĩa và các trường hợp lỗi liên quan; tránh test chỉ khẳng định một hằng số.
- Không commit mật khẩu, thông tin kết nối bí mật, log hoặc đầu ra build.
- Giữ thay đổi đúng phạm vi; không ghi đè công việc đang có của người dùng.

## Cách cộng tác

1. Kiểm tra code, cấu hình và trạng thái Git liên quan trước khi đề xuất thay đổi.
2. Giải thích mục đích, lý thuyết cần biết, các file dự kiến sửa và cách kiểm tra trước khi sửa file. Với bước đã được người dùng giao rõ ràng, tiếp tục thực hiện sau phần giải thích; không hỏi lại quyền đã có.
3. Chia công việc thành bước nhỏ: Codex viết khung và phần khó, giao một số phần vừa sức để người học tự code với hướng dẫn và tiêu chí hoàn thành rõ ràng. Không tự hoàn thành phần đã dành cho người học trừ khi được yêu cầu.
4. Sau mỗi bước, giải thích thay đổi và chạy test phù hợp trước khi commit. Nếu không chạy được, nêu rõ nguyên nhân; không báo test thành công khi chưa kiểm chứng.
5. Xem diff và kết quả test trước commit; chỉ commit khi người dùng yêu cầu hoặc đã cho phép rõ ràng. Không tự push.
6. Báo cáo ngắn gọn phần đã làm, kết quả kiểm tra và bài thực hành tiếp theo.

## Quy trình thống kê trận mùa 2026/27

- Nhập đúng danh sách cầu thủ thực tế của mỗi fixture FINISHED; quy tắc cũ 40 dòng/trận, 400 dòng/vòng chỉ là giới hạn còn tồn tại trong service tổng mùa, không phải chuẩn dữ liệu để thêm cầu thủ hoặc dòng `DID_NOT_PLAY` giả.
- Sau nhập, kiểm tra/tái tính `appearances`, `minutes`, `goals`, `assists` của `player_season_stats` bằng cơ chế hiện có theo đúng phạm vi nó hỗ trợ; đọc lại MySQL và chạy lặp xác nhận `updated=0` khi có thể. Nếu giới hạn 40/400 hoặc yêu cầu vòng đủ 10 trận chặn dữ liệu thực tế, báo rõ tổng mùa chưa theo kịp và xử lý giới hạn chung bằng thay đổi mã được kiểm tra đúng phạm vi; không chạy SQL thủ công để lách kiểm tra.
- Chỉ số chưa xác minh giữ SQL `NULL`; `DID_NOT_PLAY` không cộng vào tổng. Không sửa dữ liệu mùa 2024/25.
- Cách chạy và quy tắc kiểm tra chi tiết ở `docs/player-season-stats-2026.md`.

## Quy trình Gameweek cố định cho PrismaXI

- Quyết định thường trực ngày 21/10/2026: cầu thủ Fantasy đã chốt được xác minh không có trong danh sách trận cuối nhận 0 điểm, bất kể chấn thương, treo giò, quyết định HLV hoặc chưa rõ lý do vắng mặt. Tự đối chiếu mọi lượt chọn với danh sách đầy đủ hai đội của fixture FINISHED, membership ngày trận và nhập `confirmed-not-registered.csv` với `NOT_REGISTERED` theo đúng fixture/player; không hỏi lại lý do vắng mặt. Giữ căn cứ chấn thương cũ; không tạo thống kê/DID_NOT_PLAY/rating giả. Thiếu dòng đơn thuần, nguồn chưa đủ, danh tính/membership mâu thuẫn hoặc trận chưa xong vẫn là pending. Khi người dùng gửi ảnh rating/danh sách cuối đủ 10 trận và yêu cầu thu thập, tự hoàn tất thu thập → backup → nhập dữ liệu/bằng chứng → đối chiếu mọi đội đã chốt → tổng mùa/Đội hình tiêu biểu → readiness; mục tiêu sẵn sàng để ADMIN công bố, không dừng sau nhập thống kê. Không tự công bố hoặc deploy.
- Quyết định thường trực bổ sung ngày 13/10/2026: khi yêu cầu “thu thập…”/“Tổng hợp GW…” đã có đủ dữ liệu được xác minh và nhập của cả 10 trận FINISHED, tự hoàn thành bước cập nhật/đối chiếu Đội hình tiêu biểu của GW đó, không chờ người dùng nhắc riêng. Đọc API và kiểm tra tab mặc định theo GW hiện tại, 11 ID/vị trí/rating và tổng; ghi kết quả trong checkpoint. Nếu còn thiếu vị trí hoặc API production chưa có mã cần thiết, báo rõ phần bị chặn, không coi bước này đã xong.
- Cầu thủ đã ra sân nhưng ảnh/bảng rating cuối do người dùng cung cấp xác nhận không được SofaScore chấm: mặc định tính 0 điểm Fantasy như người không ra sân. Quyết định này cho phép tự ghi căn cứ theo fixture/player_id vào `confirmed-unrated.csv` và nhập bằng chứng cùng batch, không hỏi xác nhận riêng lại từng người khi nguồn đã rõ. Vẫn giữ PLAYED, phút/thống kê thực tế và rating/fantasy_points thô NULL; không đổi thành DID_NOT_PLAY hoặc rating 0. Rating chưa cung cấp/ảnh thiếu hoặc không đọc được vẫn là dữ liệu đang chờ; không suy ngoại lệ từ NULL đơn thuần. Người không được chấm không tham gia chọn đội tiêu biểu bằng rating giả.

- Khi người dùng nhắn “Tổng hợp GW N mùa 2026/27”, “thu thập Gameweek…” hoặc tương đương, bắt buộc đọc và áp dụng [docs/gameweek-data-workflow.md](docs/gameweek-data-workflow.md), tiếp tục checkpoint; yêu cầu này cho phép hoàn thành và nhập production phần đã xác minh theo quy trình thường trực, không cần nhắc riêng từng lỗi.
- Xác định đủ 10 fixture từ API PremierHub, đối chiếu ID nội bộ với football-data.org; đồng bộ lịch, trạng thái, tỉ số, BXH bằng mã hiện có. Chỉ nhập thống kê cho fixture đã xác nhận `FINISHED`; fixture chưa đá/chưa xong ghi pending và không tự đổi thành `FINISHED`.
- Với hai đội mỗi fixture FINISHED, thu thập danh sách trận đầy đủ, 11 đá chính, dự bị vào sân/không vào sân, sơ đồ/vị trí, thống kê hiện hành; kiểm tra membership ngày trận và roster hiện tại, bổ sung khoảng có căn cứ bằng importer, không tạo khoảng một ngày hay chồng lấn. Ảnh Lineups người dùng gửi là nguồn hợp lệ, không bắt buộc hoặc bịa URL SofaScore; rating chỉ từ người dùng, PLAYED được xác nhận không chấm giữ rating/fantasy_points `NULL` và bằng chứng riêng.
- Sau khi xác minh đúng production MySQL của backend và tạo/kiểm tra backup mới ngoài Git, nhập thống kê, đội hình/sơ đồ và bằng chứng CSV cuối/Fantasy; chạy importer lặp xác nhận không tạo trùng. Đối chiếu mọi cầu thủ của các đội Fantasy đã chốt với danh sách trận; người ngoài danh sách cuối đầy đủ được tính 0 riêng theo fixture/player_id, bất kể lý do vắng mặt, không tạo thống kê/rating giả hoặc suy rộng sang GW khác. Chỉ hỏi bổ sung khi nguồn/danh tính/membership thực sự chưa xác minh; không hỏi lại lý do vắng mặt.
- Kiểm tra tổng mùa, API tỉ số/lịch sử cầu thủ/sân 11v11 và readiness theo từng fixture/nguyên nhân. Chỉ báo cả GW hoàn tất khi đủ 10 trận và readiness đạt; ADMIN tự công bố Fantasy sau đó, Codex không công bố/tái công bố. Giữ mùa 2024/25, snapshot đội, deadline, rosterAsOf; không tự commit, push, deploy, tạo cron hoặc Pre-deploy Command. CSV thuần không cần test riêng từng trận/full suite.
- Sau nhập đủ dữ liệu GW, kiểm tra API Đội hình tiêu biểu `/api/fantasy/2026/team-of-week?gameweek={gw}` và tab tương ứng. Service đọc thống kê/rating đã nhập và vị trí hợp lệ để tính trực tiếp; không thu thập rating riêng hoặc nhập một bảng đội tiêu biểu. Tab mặc định theo GW đang hiển thị ở Vòng thi, hỗ trợ GW1–38 và vẫn cho chọn xem vòng cũ. Nếu thiếu rating/vị trí/trận thì báo độ phủ, không tự gán người/điểm để đủ 11. Xem `docs/team-of-week-2026.md`.

## Kế hoạch Fantasy có tài khoản — PrismaXI 2026/27

- Fantasy nhiều người chơi đã có tài khoản, đội đã chốt, readiness và luồng ADMIN công bố. Đọc [docs/fantasy-multiplayer-2026-plan.md](docs/fantasy-multiplayer-2026-plan.md) và [docs/fantasy-results-2026.md](docs/fantasy-results-2026.md) để giữ luật đã chốt. Quyền nhập dữ liệu GW đã xác minh theo quy trình trên không bao gồm mở GW mới, đổi đội/deadline/rosterAsOf hoặc công bố/tái công bố Fantasy.

## Lệnh kiểm tra

Chạy từ thư mục `backend/`:

```powershell
mvn clean test
mvn spring-boot:run
```

Lệnh đầu biên dịch và chạy test; lệnh sau chạy `com.premierhub.App` qua exec-maven-plugin.

## Hướng Sprint 0 được đề xuất

- Hoàn thiện README với mục tiêu, yêu cầu môi trường và lệnh chạy/test.
- Bổ sung ignore đầu ra Maven `target/`, xem xét file cấu hình IDE trước khi đưa vào Git và thống nhất môi trường Java.
- Đã bắt đầu nghiệp vụ với lớp `Club` (thay tên đề xuất `Team`) và test hành vi; phần tìm tên dành cho người học thực hành.
- Giải thích, chạy test và xem diff trước mỗi commit được cho phép.
- Sau nền tảng này mới tiến tới CSV, rồi database/web theo từng bước học. Đây là định hướng đề xuất, không phải các chức năng đã hoàn thành.

## Lộ trình người dùng đã chọn

1. Java và CSV: `Club`, `Player`, `Match`, `Standing`, đọc CSV, tìm kiếm/lọc, bảng xếp hạng, thống kê cầu thủ và unit test.
2. Spring Boot: REST API, controller/service/repository/DTO, JSON và Postman.
3. Frontend cơ bản: HTML/CSS/JavaScript, `fetch` và giao diện dữ liệu bóng đá.
4. Database: ERD, MySQL, JPA/Hibernate và chuyển dữ liệu CSV sang database.
5. Tài khoản và cộng đồng: đăng ký/đăng nhập, bảo vệ mật khẩu bằng hàm băm phù hợp, USER/ADMIN, đội yêu thích, bình luận và quản lý bình luận.
6. React và triển khai: chuyển frontend sang React, tách frontend/backend, Docker, testing và deploy.

Triển khai từng bài nhỏ theo cách cộng tác ở trên; không tự hoàn thành bài tập đã giao cho người học.

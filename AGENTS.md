# PremierHub — Hướng dẫn cộng tác

## Mục tiêu và bối cảnh

PremierHub là dự án học Full-stack qua thực hành, với các chức năng đích:
- Tra cứu lịch đấu và bảng xếp hạng Premier League.
- Thống kê đội bóng và cầu thủ.
- Tài khoản người dùng và đội bóng yêu thích.
- Bình luận và cộng đồng.

Người học đã biết Python, Java, OOP và SQL/MySQL cơ bản; chưa học lập trình web và Spring Boot. Giải thích bằng tiếng Việt, gắn lý thuyết trực tiếp với code và ví dụ nhỏ.

## Công nghệ

- Hiện tại: Java, Maven, JUnit Jupiter; Java release mục tiêu là 21.
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
├── .vscode/
│   └── settings.json
└── backend/
    ├── pom.xml
    └── src/
        ├── main/java/com/premierhub/App.java
        └── test/java/com/premierhub/AppTest.java
```

`App` hiện là chương trình Hello World; `AppTest` là test mẫu, chưa kiểm tra nghiệp vụ. Chưa có frontend, database, bộ dữ liệu CSV hoặc chức năng nghiệp vụ. `backend/target/` là đầu ra build, không phải mã nguồn để commit. Cập nhật phần cấu trúc này khi dự án thay đổi.

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

## Lệnh kiểm tra

Chạy từ thư mục `backend/`:

```powershell
mvn clean test
mvn compile exec:java
```

Lệnh đầu biên dịch và chạy test; lệnh sau chạy `com.premierhub.App` qua exec-maven-plugin.

## Hướng Sprint 0 được đề xuất

- Hoàn thiện README với mục tiêu, yêu cầu môi trường và lệnh chạy/test.
- Bổ sung ignore đầu ra Maven `target/`, xem xét file cấu hình IDE trước khi đưa vào Git và thống nhất môi trường Java.
- Bắt đầu nghiệp vụ nhỏ với lớp `Team` và test hành vi; chia rõ phần khung do Codex làm và phần người học thực hành.
- Giải thích, chạy test và xem diff trước mỗi commit được cho phép.
- Sau nền tảng này mới tiến tới CSV, rồi database/web theo từng bước học. Đây là định hướng đề xuất, không phải các chức năng đã hoàn thành.

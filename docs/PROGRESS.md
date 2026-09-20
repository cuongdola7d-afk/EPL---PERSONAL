# Tiến độ PremierHub

## Sprint 1 — Java và CSV: hoàn thành

- Hoàn thành model `Club`, `Player`, `Match`, `Standing` và validation.
- Hoàn thành reader cho ba file CSV đơn giản UTF-8.
- Hoàn thành tìm kiếm, lọc cầu thủ, thống kê vua phá lưới và bảng xếp hạng.
- Hoàn thành chương trình console demo và unit test.
- Dữ liệu hiện nằm trong bộ nhớ; chưa có Spring Boot, API hoặc database.

## Giai đoạn 2 — Spring Boot Club API: bước 1 hoàn thành

- Chuyển Maven project sang Spring Boot 4.1.1, Java release 21.
- Thêm class khởi động và REST API chỉ đọc cho `Club`.
- Tải `clubs.csv` một lần từ classpath khi application context khởi động.
- Thêm `ClubResponse`, validation HTTP, context test và MockMvc test.
- Chưa có Player/Match/Standing API, database hoặc frontend.

## Bước kế tiếp

Toàn bộ phần code từng dành cho người học trong `LEARNING_TASKS.md` đã được hoàn thiện và có test. Bước kế tiếp của Giai đoạn 2 nên được lập kế hoạch riêng, không mở rộng API trong thay đổi hiện tại.

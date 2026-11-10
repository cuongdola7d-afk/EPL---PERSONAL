# Giai đoạn 2: chuẩn bị chạy và deploy backend

> Ghi chép này mô tả lần chuẩn bị deploy ở giai đoạn CSV ban đầu. Cập nhật ngày 10/11/2026: web hiện đọc SQL; console đọc ba CSV demo trong `backend/data/` từ filesystem, không cần database. Ba bản sao demo resource đã được bỏ; JAR vẫn giữ snapshot JSON 2024 và hai CSV fallback đội hình được Maven đóng gói riêng. Xem [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) trước thao tác vận hành hiện tại.

**Executable JAR** là file chứa ứng dụng Java đã biên dịch, thư viện cần thiết và các resource được đóng gói. Ở mốc API CSV ban đầu, resource gồm các CSV demo trong `src/main/resources/data`, nên khi đó có thể chạy `java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar` mà không cần trỏ đến CSV bên ngoài. Đây là luồng dữ liệu chỉ đọc của mốc lịch sử đó, không phải luồng đọc SQL hiện tại.

**Maven Wrapper** gồm `mvnw`, `mvnw.cmd` và `.mvn/wrapper/maven-wrapper.properties`. Nó ghim Maven 3.9.14 cho project và tự tải Maven vào cache của người dùng nếu máy chưa có. Repo không lưu cache hoặc JAR đã tải. Java 21 vẫn là yêu cầu của project.

**Environment variable** là giá trị hệ điều hành truyền vào tiến trình. `application.properties` dùng `server.port=${PORT:8080}`: thiếu `PORT` thì cổng là 8080; có `PORT` thì dùng cổng được cung cấp. `SPRING_PROFILES_ACTIVE=prod` bật **Spring profile** production, khiến Spring đọc thêm `application-prod.properties` bên cạnh cấu hình chung. File production đặt logging ở INFO và không bật debug. Không ghi secret vào hai file cấu hình.

**Actuator** cung cấp endpoint vận hành. Chỉ `health` và `info` được expose qua HTTP; `/actuator/health` trả trạng thái để hosting biết ứng dụng đã chạy. Chi tiết thành phần health được ẩn, còn endpoint nhạy cảm như `/actuator/env` không công khai. Fallback error không gửi stack trace, tên exception hoặc thông báo nội bộ cho client; lỗi của API vẫn dùng `ApiErrorResponse`. Stack trace có thể tiết lộ cấu trúc code và dữ liệu cấu hình.

Luồng deploy: dùng Maven Wrapper chạy `clean test`, `clean package`; chuyển executable JAR sang môi trường chạy có Java 21; đặt `PORT` và `SPRING_PROFILES_ACTIVE=prod`; chạy `java -jar`; gọi `/actuator/health` rồi kiểm tra API. Chưa có database, frontend hay Docker trong bước này.

## Bài tập tự thực hành

1. Từ `backend/`, tự chạy `.\mvnw.cmd clean package` và ghi lại tên JAR trong `target/`.
2. Tự chạy JAR bằng `java -jar`, gọi `/actuator/health` và `/api/clubs`, ghi HTTP status và một phần JSON quan sát được.
3. Dừng ứng dụng, đặt `$env:PORT=9090` trong PowerShell rồi chạy JAR lại. Gọi hai URL trên với cổng 9090 và ghi khác biệt.
4. Xóa biến `PORT` trong phiên PowerShell sau khi thực hành bằng `Remove-Item Env:PORT`.

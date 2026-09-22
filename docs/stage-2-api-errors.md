# Giai đoạn 2: validation và lỗi HTTP

`@RestControllerAdvice` áp dụng cách trả lỗi chung cho các REST controller. Mỗi phương thức `@ExceptionHandler` nhận một nhóm exception rồi tạo `ApiErrorResponse` với thời điểm, HTTP status, tên lỗi, mã lỗi, thông báo và đường dẫn request. Controller chỉ ném `ResourceNotFoundException` khi không tìm thấy ID; service ném `InvalidFilterException` khi position/status không thuộc enum hợp lệ.

Bean Validation kiểm tra dữ liệu đầu vào trước khi gọi service: `@Positive` cho ID, `@Min(1)` cho vòng đấu và limit, `@NotBlank` cho keyword bắt buộc. Filter chuỗi tùy chọn dùng `@Pattern`: `null` được chấp nhận, nhưng chuỗi rỗng hoặc toàn khoảng trắng bị từ chối. Service vẫn giữ kiểm tra nghiệp vụ để dùng được ngoài HTTP.

HTTP **400** nghĩa là request không hợp lệ, như thiếu keyword, sai kiểu số, giá trị ngoài phạm vi hoặc filter enum không tồn tại. HTTP **404** nghĩa là ID hợp lệ nhưng không có tài nguyên tương ứng. HTTP **500** nghĩa là lỗi máy chủ không mong đợi. Khi xảy ra 500, server ghi log chi tiết nhưng client chỉ nhận thông báo chung; trả exception nội bộ hoặc stack trace có thể làm lộ chi tiết triển khai.

Controller tests dùng `@WebMvcTest` và service giả lập bằng `@MockitoBean` để kiểm tra riêng binding, validation, mã HTTP và JSON. `PremierHubApplicationTest` vẫn dùng `@SpringBootTest` để kiểm tra các bean ứng dụng tạo được cùng nhau. Unit test của service kiểm tra logic lọc enum mà không cần HTTP.

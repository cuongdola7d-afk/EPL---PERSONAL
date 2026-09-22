# Giai đoạn 2: Player REST API

`PlayerController` nhận HTTP và đổi kết quả sang `PlayerResponse`. `PlayerService` tìm và lọc cầu thủ. `PlayerRepository` cung cấp danh sách và tìm theo ID; `InMemoryPlayerRepository` giữ danh sách bất biến trong bộ nhớ. `PlayerResponse` là dữ liệu JSON công khai gồm ID, tên, CLB, vị trí và thống kê.

`@RestController` đánh dấu lớp xử lý HTTP và để Spring chuyển giá trị trả về thành JSON. `@RequestMapping("/api/players")` đặt tiền tố URL; `@GetMapping` gắn phương thức với GET. `@RequestParam` đọc `club` và `position` từ query string, `@PathVariable` đọc `id` từ đường dẫn.

Spring tạo `PlayerRepository` và `PlayerService` trong `PlayerDataConfiguration`, rồi đưa `PlayerService` vào constructor của controller: đó là Dependency Injection. Không cần tự tạo service trong controller.

Ví dụ `GET /api/players?club=Arsenal&position=Forward`: controller nhận hai tham số và gọi service. Service lấy danh sách từ repository, chuẩn hóa chuỗi bằng cách bỏ khoảng trắng đầu/cuối và chuyển về chữ thường, rồi giữ cầu thủ thuộc Arsenal **và** chơi vị trí Forward. Controller đổi từng kết quả thành DTO; Spring trả JSON với HTTP 200. Không khớp thì kết quả là `[]`. ID không có dùng `ResponseStatusException` để trả 404, giống Club API; global exception handling sẽ được hoàn thiện ở bước sau.

Không trả trực tiếp `Player` vì model là dữ liệu nội bộ; DTO cho phép kiểm soát trường công khai và thêm tên CLB thay vì bắt người dùng API tự tra `clubId`. CSV được đọc một lần lúc Spring khởi động từ `src/main/resources/data/players.csv`; đọc ở mỗi request sẽ tốn I/O và khiến dữ liệu có thể thay đổi giữa các request.

`PlayerServiceTest` kiểm tra logic lọc, chuẩn hóa, kết hợp điều kiện, danh sách rỗng và tìm ID mà không cần HTTP. `PlayerControllerTest` dùng MockMvc kiểm tra URL, status và JSON qua toàn bộ Spring context. Test CSV reader cũ kiểm tra phân tích và báo lỗi dữ liệu CSV.

## TODO cho bạn: lọc theo tên cầu thủ

Thêm `GET /api/players?name=saka`. Tên cần được tìm theo chuỗi con, không phân biệt hoa thường và bỏ khoảng trắng đầu/cuối. Kết hợp được với `club` và `position` theo logic AND; không khớp trả `200` và `[]`.

Các file dự kiến sửa: `PlayerController.java`, `PlayerService.java`, `requests.http`. Cân nhắc dùng `Player.matchesName(...)` đã có, nhưng tự quyết định cách xử lý `name` rỗng. Viết unit test cho tên đơn lẻ, khác kiểu chữ, khoảng trắng, không khớp và kết hợp ba filter; thêm MockMvc test cho URL với `name`. Không cần sửa repository hay CSV.

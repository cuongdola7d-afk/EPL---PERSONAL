# Giai đoạn 2: Standing REST API

Bảng xếp hạng được **tính từ Match**, không đọc từ CSV standings. `MatchRepository` đã giữ dữ liệu CSV trong bộ nhớ sau khi ứng dụng khởi động. `StandingService` dùng lại `LeagueTableService` của giai đoạn 1 để tính bảng một lần khi Spring tạo service. Thuật toán chỉ tính trận `FINISHED`; trận `SCHEDULED` không có điểm hay tỉ số để cộng.

`StandingController` nhận URL, `clubId` hoặc `limit`, quyết định mã HTTP và đổi kết quả sang DTO. `StandingService` giữ bảng đã tính, gán vị trí, lấy N đội đầu bảng và tìm đội theo ID. `StandingResponse` chỉ chứa các trường JSON công khai: vị trí, CLB, số trận, thắng/hòa/thua, bàn thắng/thua, hiệu số và điểm. Controller được Spring đưa service vào constructor.

`LeagueTableService` sắp xếp theo điểm giảm dần, rồi hiệu số giảm dần, bàn thắng giảm dần, cuối cùng tên CLB tăng dần. Sau khi sắp xếp, `StandingService` gán `position` từ 1 đến số đội. `limit` chỉ cắt phần đầu của bảng, nên vị trí vẫn đúng; `GET /api/standings/{clubId}` cũng dùng vị trí trong toàn bảng.

Với `GET /api/standings?limit=5`, controller đọc `limit`, trả 400 nếu nhỏ hơn 1, rồi gọi service lấy năm đội đầu. Controller đổi từng mục sang `StandingResponse`; Spring tạo JSON và trả 200. Nếu không có CLB, kết quả là `[]`. Logic sắp xếp nằm ở service vì cùng quy tắc phải dùng được ngoài HTTP, như phần console và unit test.

`StandingServiceTest` kiểm tra thứ tự theo từng tiêu chí, vị trí, tìm ID, giới hạn, dữ liệu rỗng và việc bỏ qua trận chưa hoàn thành. `StandingControllerTest` dùng MockMvc kiểm tra URL, mã HTTP, số phần tử và các trường JSON. `LeagueTableServiceTest` cũ tiếp tục kiểm tra thuật toán tính điểm từ trận đấu.

## TODO cho bạn: lọc theo tên câu lạc bộ

Tự thêm `GET /api/standings?club=Arsenal`. Bộ lọc không phân biệt chữ hoa/chữ thường và bỏ khoảng trắng đầu/cuối. Không tìm thấy trả HTTP 200 với `[]`. Hãy quyết định cách kết hợp với `limit` sao cho dễ hiểu và ghi lại trong tài liệu.

Gợi ý file cần sửa: `StandingController.java`, `StandingService.java`, `requests.http` và tài liệu này. Viết unit test cho tên khớp, khác kiểu chữ, khoảng trắng và không khớp; viết MockMvc test cho query parameter `club`. Không cần sửa thuật toán xếp hạng.

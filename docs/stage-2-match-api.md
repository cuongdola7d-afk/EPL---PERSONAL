# Giai đoạn 2: Match REST API

`MatchController` nhận URL, query parameter và ID, chọn mã HTTP và đổi trận đấu thành `MatchResponse`. `MatchService` tìm theo ID, lọc theo CLB, vòng đấu và trạng thái. `MatchRepository` cung cấp dữ liệu; `InMemoryMatchRepository` giữ danh sách bất biến sau khi CSV được đọc một lần lúc khởi động. `MatchResponse` giới hạn dữ liệu JSON công khai, gồm ID và tên hai CLB, vòng đấu, ngày, trạng thái và tỉ số.

CSV giai đoạn 1 chưa có vòng đấu. API cần `matchweek` rõ ràng nên model và CSV được thêm cột này. Không suy vòng đấu từ ngày: lịch thực tế có thể dời trận. `MatchCsvReader` vẫn đọc được `Path` cho các phần Java cũ và nay đọc thêm `InputStream` từ classpath cho Spring.

Với `GET /api/matches?club=Arsenal&matchweek=1`, controller lấy hai query parameter rồi gọi service. Service đọc danh sách từ repository, so tên Arsenal đã chuẩn hóa với **cả đội nhà lẫn đội khách**, đồng thời chỉ giữ trận ở vòng 1. Controller đổi kết quả thành DTO và Spring trả JSON. Kiểm tra cả hai phía vì Arsenal có thể xuất hiện ở bất cứ phía nào. Lọc không khớp trả `200` với `[]`: yêu cầu tìm danh sách đã xử lý thành công, chỉ là không có phần tử. Tìm ID không có trả 404; vòng đấu dưới 1 trả 400. Cách 404 dùng `ResponseStatusException` giống Club và Player API.

`MatchServiceTest` kiểm tra tìm kiếm, chuẩn hóa, kết hợp điều kiện và lỗi vòng đấu mà không cần HTTP. `MatchControllerTest` dùng MockMvc kiểm tra endpoint, JSON và mã HTTP với dữ liệu classpath. `MatchCsvReaderTest` kiểm tra đọc và từ chối CSV không hợp lệ; `MatchTest` kiểm tra quy tắc của model.

## TODO cho bạn: lọc theo ngày thi đấu

Tự thêm `GET /api/matches?date=YYYY-MM-DD`. Chỉ nhận ngày đúng định dạng ISO; ngày sai cần trả 400. Cho phép kết hợp với `club`, `matchweek` và `status` theo logic AND; không có kết quả trả `200` và `[]`.

Gợi ý file cần sửa: `MatchController.java`, `MatchService.java`, `requests.http`. Viết unit test cho ngày khớp, không khớp và kết hợp filter; viết MockMvc test cho ngày hợp lệ và sai định dạng. Chưa cần đổi CSV hay model vì `Match` đã có `matchDate`.

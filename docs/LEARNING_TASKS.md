# Các ví dụ học tập đã hoàn thiện

Toàn bộ phần từng dành cho người học tự code đã được triển khai trong project và được kiểm tra bằng test. Tài liệu này giữ lại mục tiêu học tập, vị trí implementation và test xác minh; không còn phần code bắt buộc nào bị để trống.

## 1. Tìm kiếm tên — Easy — Đã hoàn thành

- Kiến thức: chuỗi, guard clause, `Locale.ROOT`, tìm chuỗi con.
- File/method: `Club.matchesName` và `Player.matchesName`.
- Đầu vào/đầu ra: nhận `String`; trả `false` với `null`/blank, còn lại tìm không phân biệt hoa thường và bỏ khoảng trắng hai đầu từ khóa.
- Gợi ý: xử lý trường hợp không hợp lệ trước, sau đó chuẩn hóa cả tên và từ khóa.
- Test kiểm tra: `ClubTest`, `PlayerTest`.

## 2. Kiểm tra CSV lỗi — Easy — Đã hoàn thành

- Kiến thức: Arrange–Act–Assert, `@TempDir`, kiểm tra exception.
- File/method: `ClubCsvReaderTest`, `PlayerCsvReaderTest`, `MatchCsvReaderTest`.
- Đầu vào/đầu ra: các file có header sai, thiếu cột hoặc dấu ngoặc kép làm reader ném `IllegalArgumentException` có số dòng và nguyên nhân.
- Implementation: đã có test cho header sai thứ tự và trường có dấu ngoặc kép.
- Test kiểm tra: ba test class CSV reader.

## 3. Comparator bảng xếp hạng — Medium — Đã hoàn thành

- Kiến thức: `Comparator`, `reversed`, chuỗi tiêu chí sắp xếp.
- File/method: hằng `TABLE_ORDER` trong `LeagueTableService`.
- Đầu vào/đầu ra: nhiều `Standing`; thứ tự theo điểm, hiệu số, bàn thắng giảm dần rồi tên tăng dần.
- Gợi ý: đảo chiều từng tiêu chí số riêng; không đảo toàn bộ comparator sau khi thêm tiêu chí tên.
- Test kiểm tra: `LeagueTableServiceTest.ordersByPointsGoalDifferenceGoalsForThenName`.

## 4. Lọc cầu thủ theo vị trí — Easy — Đã hoàn thành

- Kiến thức: enum, Stream `filter`, thiết kế method service.
- File/method: `PremierHubService.getPlayersByPosition` và `PremierHubServiceTest`.
- Đầu vào/đầu ra: nhận `Position`, trả danh sách cầu thủ đúng vị trí và giữ thứ tự dữ liệu ban đầu.
- Implementation: từ chối `null`, so sánh trực tiếp enum và giữ thứ tự ban đầu.
- Test kiểm tra: `filtersPlayersByPositionAndPreservesInputOrder`.

## 5. Hiệu số bàn thắng — Easy — Đã hoàn thành

- Kiến thức: thuộc tính suy ra, invariant của object.
- File/method: `Standing.getGoalDifference`, `StandingTest`.
- Đầu vào/đầu ra: với bàn thắng 4, bàn thua 7 thì hiệu số phải là `-3`.
- Gợi ý: hiệu số có thể âm; không dùng giá trị tuyệt đối.
- Test kiểm tra: `StandingTest.allowsNegativeGoalDifference` xác nhận `4 - 7 = -3`.

## 6. Trận chưa đấu — Easy — Đã hoàn thành

- Kiến thức: state validation, `OptionalInt`, exception.
- File/method: `MatchTest`, các method `isDraw`, `getWinnerClubId`, `getPointsFor`.
- Đầu vào/đầu ra: trận `SCHEDULED` không có người thắng/thua và chưa thể cấp điểm.
- Gợi ý: kiểm tra cả kết quả rỗng lẫn exception khi hỏi điểm.
- Test kiểm tra: `MatchTest.representsScheduledMatchWithoutScore` kiểm tra cả winner, loser và điểm.

## 7. Hòa tiêu chí bảng xếp hạng — Medium — Đã hoàn thành

- Kiến thức: thiết kế dữ liệu test và đọc bảng kết quả.
- File/method: `LeagueTableServiceTest`.
- Đầu vào/đầu ra: tạo lịch đấu để hai đội bằng điểm nhưng khác hiệu số hoặc bàn thắng; xác nhận đội đúng đứng trước.
- Gợi ý: tính tay từng dòng `P/W/D/L/GF/GA/GD/Pts` trước khi viết assertion.
- Test kiểm tra: `LeagueTableServiceTest.ordersByPointsGoalDifferenceGoalsForThenName`.

## 8. Kiểm thử demo với thư mục tạm — Medium — Đã hoàn thành

- Kiến thức: integration test nhỏ, `@TempDir`, `ByteArrayOutputStream`.
- File/method: `App.runDemo` và `AppTest`.
- Đầu vào/đầu ra: tạo đủ ba CSV trong thư mục tạm, chạy demo và kiểm tra output có tên câu lạc bộ cùng tiêu đề bảng xếp hạng.
- Implementation: test tạo đủ ba CSV, bắt output bằng `ByteArrayOutputStream` và kiểm tra Club, thống kê cầu thủ, số bản ghi import cùng bảng xếp hạng.
- Test kiểm tra: `AppTest.runsDemoFromCsvFiles`.

# Ví dụ học tập Giai đoạn 2 — Spring Boot Club API

## 9. Class khởi động — Easy — Đã hoàn thành

- Kiến thức: `@SpringBootApplication`, component scan, auto-configuration.
- File/method: `PremierHubApplication` và method `main`.
- Đầu vào/đầu ra: giải thích điều gì xảy ra từ lúc `SpringApplication.run` được gọi đến khi server sẵn sàng nhận request.
- Giải thích: `@SpringBootApplication` đánh dấu class cấu hình, bật auto-configuration theo dependency và component scan từ package `com.premierhub`. `SpringApplication.run` tạo application context, đăng ký bean, khởi động Tomcat và bắt đầu nhận request.
- Test kiểm tra: `PremierHubApplicationTest.contextLoads`.

## 10. Endpoint lấy danh sách — Easy — Đã hoàn thành

- Kiến thức: `@RestController`, `@RequestMapping`, `@GetMapping`, JSON serialization.
- File/method: `ClubController.getAll`.
- Đầu vào/đầu ra: `GET /api/clubs` không có input; trả JSON array và HTTP 200.
- Gợi ý: controller gọi service, map từng model sang DTO rồi trả danh sách.
- Test kiểm tra: `ClubControllerTest.getAllReturnsJsonArray`.

## 11. Luồng Controller → Service — Easy — Đã hoàn thành

- Kiến thức: constructor injection và phân chia trách nhiệm.
- File/method: constructor `ClubController`, `getById`, `PremierHubService.findClubById`.
- Đầu vào/đầu ra: ID hợp lệ trả câu lạc bộ; ID không tồn tại trả HTTP 404.
- Gợi ý: đặt breakpoint lần lượt trong controller, service và `ClubResponse.from`.
- Test kiểm tra: hai test `getExistingClubReturnsClub` và `getMissingClubReturnsNotFound`.

## 12. Model và DTO — Easy — Đã hoàn thành

- Kiến thức: domain model, API contract, Java record.
- File/method: `Club` và `ClubResponse`.
- Kết quả: `Club` bảo vệ dữ liệu và chứa `matchesName`; `ClubResponse` định nghĩa contract JSON chỉ gồm `id`, `name`, `city`. Thay đổi nội bộ model không bắt buộc làm thay đổi API.
- Mapping: `ClubResponse.from` dùng Java thông thường; `matchesName` không xuất hiện trong JSON.
- Test kiểm tra: `ClubControllerTest.getExistingClubReturnsClub` xác nhận các field JSON công khai.

## 13. MockMvc và JSONPath — Easy — Đã hoàn thành

- Kiến thức: request giả lập, status matcher, JSONPath.
- File/method: `ClubControllerTest`.
- Đầu vào/đầu ra: gọi search với chữ thường và kiểm tra cả `id`, `name`, `city` của phần tử đầu tiên.
- Test kiểm tra: `ClubControllerTest.searchReturnsMatchingClubs`.

## 14. Validation keyword — Medium — Đã hoàn thành

- Kiến thức: Bean Validation, `@NotBlank`, lỗi HTTP 400 và validation dự phòng ở service.
- File/method: `ClubController.search`, `PremierHubService.findClubsByName`.
- Đầu vào/đầu ra: keyword hợp lệ trả danh sách; missing, empty hoặc blank trả HTTP 400.
- Gợi ý: quan sát khác biệt giữa validation tại HTTP boundary và guard clause khi service được gọi trực tiếp.
- Test kiểm tra: các test keyword trong `ClubControllerTest` và `PremierHubServiceTest`.

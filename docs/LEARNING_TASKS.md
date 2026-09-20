# Bài tự luyện sau Sprint 1

Các bài dưới đây dùng implementation hoàn chỉnh hiện tại làm tài liệu đối chiếu. Nên tự làm trên branch riêng hoặc viết lại trên giấy trước, sau đó chạy test liên quan. Không cần xóa code đang hoạt động.

## 1. Viết lại tìm kiếm tên — Easy

- Kiến thức: chuỗi, guard clause, `Locale.ROOT`, tìm chuỗi con.
- File/method: `Club.matchesName` và `Player.matchesName`.
- Đầu vào/đầu ra: nhận `String`; trả `false` với `null`/blank, còn lại tìm không phân biệt hoa thường và bỏ khoảng trắng hai đầu từ khóa.
- Gợi ý: xử lý trường hợp không hợp lệ trước, sau đó chuẩn hóa cả tên và từ khóa.
- Test kiểm tra: `ClubTest`, `PlayerTest`.

## 2. Thêm một trường hợp CSV lỗi — Easy

- Kiến thức: Arrange–Act–Assert, `@TempDir`, kiểm tra exception.
- File/method: chọn một trong `ClubCsvReaderTest`, `PlayerCsvReaderTest`, `MatchCsvReaderTest`.
- Đầu vào/đầu ra: tạo file có một lỗi chưa được test riêng; reader phải ném `IllegalArgumentException` có số dòng và nguyên nhân.
- Gợi ý: thử header có cột đúng tên nhưng sai thứ tự, hoặc một dòng chứa dấu ngoặc kép.
- Test kiểm tra: chạy test class reader đã chọn.

## 3. Tự viết comparator bảng xếp hạng — Medium

- Kiến thức: `Comparator`, `reversed`, chuỗi tiêu chí sắp xếp.
- File/method: hằng `TABLE_ORDER` trong `LeagueTableService`.
- Đầu vào/đầu ra: nhiều `Standing`; thứ tự theo điểm, hiệu số, bàn thắng giảm dần rồi tên tăng dần.
- Gợi ý: đảo chiều từng tiêu chí số riêng; không đảo toàn bộ comparator sau khi thêm tiêu chí tên.
- Test kiểm tra: `LeagueTableServiceTest.ordersByPointsGoalDifferenceGoalsForThenName`.

## 4. Thêm lọc cầu thủ theo vị trí — Easy

- Kiến thức: enum, Stream `filter`, thiết kế method service.
- File/method: thêm method trong `PremierHubService` và test trong `PremierHubServiceTest`.
- Đầu vào/đầu ra: nhận `Position`, trả danh sách cầu thủ đúng vị trí và giữ thứ tự dữ liệu ban đầu.
- Gợi ý: từ chối `null` bằng thông báo rõ ràng; không chuyển enum thành chuỗi để so sánh.
- Test kiểm tra: test mới của bạn và toàn bộ `PremierHubServiceTest`.

## 5. Giải thích và kiểm tra hiệu số — Easy

- Kiến thức: thuộc tính suy ra, invariant của object.
- File/method: `Standing.getGoalDifference`, `StandingTest`.
- Đầu vào/đầu ra: với bàn thắng 4, bàn thua 7 thì hiệu số phải là `-3`.
- Gợi ý: hiệu số có thể âm; không dùng giá trị tuyệt đối.
- Test kiểm tra: bổ sung một test trong `StandingTest`.

## 6. Thêm test trận chưa đấu — Easy

- Kiến thức: state validation, `OptionalInt`, exception.
- File/method: `MatchTest`, các method `isDraw`, `getWinnerClubId`, `getPointsFor`.
- Đầu vào/đầu ra: trận `SCHEDULED` không có người thắng/thua và chưa thể cấp điểm.
- Gợi ý: kiểm tra cả kết quả rỗng lẫn exception khi hỏi điểm.
- Test kiểm tra: test mới trong `MatchTest`.

## 7. Thêm trường hợp hòa tiêu chí bảng xếp hạng — Medium

- Kiến thức: thiết kế dữ liệu test và đọc bảng kết quả.
- File/method: `LeagueTableServiceTest`.
- Đầu vào/đầu ra: tạo lịch đấu để hai đội bằng điểm nhưng khác hiệu số hoặc bàn thắng; xác nhận đội đúng đứng trước.
- Gợi ý: tính tay từng dòng `P/W/D/L/GF/GA/GD/Pts` trước khi viết assertion.
- Test kiểm tra: test mới và toàn bộ `LeagueTableServiceTest`.

## 8. Kiểm thử demo với thư mục tạm — Medium

- Kiến thức: integration test nhỏ, `@TempDir`, `ByteArrayOutputStream`.
- File/method: `App.runDemo` và `AppTest`.
- Đầu vào/đầu ra: tạo đủ ba CSV trong thư mục tạm, chạy demo và kiểm tra output có tên câu lạc bộ cùng tiêu đề bảng xếp hạng.
- Gợi ý: dùng `PrintStream` bọc `ByteArrayOutputStream`; giữ dataset thật nhỏ nhưng các khóa `clubId` phải hợp lệ.
- Test kiểm tra: test mới trong `AppTest`, sau đó `mvn clean test`.

# Lưu đội tham gia Fantasy 2026/27

Luồng hiện hành: một nút **Lưu đội hình** cập nhật trực tiếp đội tham gia; không yêu cầu người chơi lưu nháp rồi chốt riêng. GW6 đã mở production và người dùng đã kiểm chứng lưu → sửa → lưu lại → reload; xem [checkpoint production](fantasy-production-2026-10-05.md). Rating/kết quả/BXH vận hành theo [hướng dẫn công bố](fantasy-results-2026.md). Các số liệu kiểm tra local dưới đây là lịch sử bước 3, không phải yêu cầu chạy lại.

## Lưu trữ và luật

- fantasy_entries có khóa chính (account_id, season, gameweek), thời điểm lưu/chốt, phiên bản hiện hành và metadata hai đội. Không tạo entry khi chỉ đọc; tài khoản chưa lưu có version 0 và chưa có đội.
- fantasy_draft_picks cho phép thiếu người hoặc rỗng. Người đã chọn vẫn phải hợp lệ theo sơ đồ, quyền vị trí, OVR và giới hạn CLB.
- fantasy_submitted_picks lưu snapshot sơ đồ/ô, ID, tên cầu thủ, CLB, OVR, vị trí chính, eligiblePositions và quyền vị trí dùng để xác nhận. Thay đổi hồ sơ hoặc membership về sau không viết lại snapshot.
- Các khóa chính, khóa duy nhất và khóa ngoại bảo đảm một entry hiện hành mỗi tài khoản/GW và không lặp cầu thủ trong từng đội. Snapshot không phụ thuộc khóa ngoại tới hồ sơ bóng đá có thể thay đổi.
- Dùng validator Fantasy hiện có: 11 ID khác nhau khi chốt, đúng ô của bốn sơ đồ hiện có, eligiblePositions hợp lệ, không thiếu OVR, tối đa 3 người/CLB và tổng OVR ≤ 910 theo yêu cầu 06/10 (backend/MySQL production đã đồng bộ sau backup và migration được cho phép riêng). Không nhận CLB/OVR/quyền vị trí từ client.
- Mỗi GW lưu rosterAsOf khi quản trị công bố/mở vòng: mặc định ngày công bố theo giờ Việt Nam, hoặc ngày được chọn có roster hiệu lực trong database. Danh sách chọn và validator dùng cùng mốc lưu này; không đổi theo ngày hiện tại. Mốc 2026-10-02 chỉ còn trong phần luyện tập của khách. Xem [bổ sung roster và đo khóa MySQL](fantasy-roster-reference-2026.md).
- Chỉ phục vụ GW6–GW38 mùa 2026/27; GW1–GW5 chưa triển khai Replay.

## Transaction, thời gian và nhiều tab

Thứ tự khóa là cấu hình GW → tài khoản → entry hiện hành. Sau khi lấy khóa, service kiểm tra Clock server và OPEN; từ đúng deadline trở đi bị từ chối. expectedVersion phải khớp bản đang lưu, nếu khác trả 409 và không tự ghi lại.

Validator đọc roster từ database một lần; dữ liệu đó cũng dùng tạo snapshot. Service kiểm tra giờ ngay trước ghi và sau các SQL ghi. Nếu hết hạn trong quá trình truy vấn/ghi, toàn bộ transaction rollback, kể cả snapshot cũ đã bị thay trong transaction. Sau lần kiểm tra cuối không truy vấn thêm để dựng response. Thời điểm ghi dùng UTC và cắt tới microsecond để không làm tròn một thời điểm trước hạn thành đúng hạn trong MySQL.

**Lưu đội hình** hợp lệ đồng bộ draft và snapshot đội tham gia trong cùng transaction. Lưu lại lỗi giữ nguyên đội đã lưu và phiên bản trước đó. API nháp cũ còn tương thích và không đổi snapshot, nhưng không phải bước bắt buộc hay nút riêng trên website. Giới hạn thời gian dựa trên lần chấp nhận của server; việc commit hoặc phản hồi mạng có thể hoàn tất sau lần kiểm tra đó.

Khóa hàng cấu hình GW hiện tuần tự hóa thao tác ghi của mọi người trong cùng vòng. Phép đo local 20 tài khoản ghi đồng thời có hàng đợi lấy khóa cao nhất 1,76 giây, không lỗi; chưa benchmark tải lớn hoặc đo trên deployment thật. Giữ cơ chế khóa trong lượt bổ sung roster. Chỉ lưu đội chốt hiện hành, không lưu lịch sử mọi đội từng chốt; số phiên bản vẫn phát hiện dữ liệu cũ.

## API riêng của người đăng nhập

| Thao tác | Đường dẫn |
| --- | --- |
| Đọc | GET /api/fantasy/2026/me/gameweeks/{gameweek} |
| Lưu đội hình / lưu lại đội tham gia | POST /api/fantasy/2026/me/gameweeks/{gameweek}/submit |
| API nháp tương thích client cũ; không dùng trong luồng website hiện hành | POST /api/fantasy/2026/me/gameweeks/{gameweek}/draft |

Body ghi gồm formation, picks (ô → player_id) và expectedVersion. Backend lấy tài khoản từ session. Header X-PrismaXI-Account-ID chỉ đối chiếu session còn thuộc tài khoản giao diện đang dùng; không dùng header hoặc body để chọn chủ đội. Session đổi tài khoản trả SESSION_CHANGED thay vì ghi lựa chọn của tab cũ sang tài khoản mới.

Giữ cookie HttpOnly/Secure theo cấu hình auth, CSRF cho POST và kiểm tra proxy production cho API riêng. Frontend dùng URL /api cùng origin, credentials include, lấy CSRF trước ghi. Không cache phản hồi đội riêng; không có API đọc nháp/đội của người khác. Lỗi gồm 401 chưa đăng nhập, 403 CSRF/quyền, 409 bản cũ/vòng khóa/session đổi và 422 luật đội hình.

## Giao diện

AccountMenu chia sẻ trạng thái tài khoản với App; đổi GW không gọi lại /me. Fantasy tải đúng dữ liệu server theo tài khoản/GW, hủy hoặc bỏ response của request cũ. Không tự chốt, tự nhập đội khách từ localStorage hay ghi đội riêng vào localStorage.

Giao diện có một nút **Lưu đội hình** tại thanh chọn sơ đồ, loading/lỗi, tổng OVR; bỏ khung metadata đội đã lưu theo yêu cầu 06/10. Lưu thành công hiển thị thông báo 2,5 giây. Trước deadline có thể sửa rồi lưu lại; reload đọc đúng đội server. Từ deadline giữ snapshot trên sân, khóa chỉnh sửa/lưu và báo chờ kết quả. Sau công bố điểm từng người hiện góc trên phải avatar, ô kết quả dưới sân chỉ hiện tổng điểm; tab BXH GW/cả mùa giữ nguyên. Xung đột cần người dùng tải lại bản server; không retry ghi tự động.

Thông báo giữa các tab chỉ mang tín hiệu đổi session và ID tab, không mang tài khoản, đội hoặc token. Đăng xuất/đổi tài khoản xóa đội riêng ở các tab trước khi tải session mới. Không thay lựa chọn luyện tập của khách.

## Kiểm chứng đã thực hiện

- Backend: 31 test đúng phạm vi — FantasyEntryIntegrationTest (10), FantasyLineupServiceTest (5), GameweekIntegrationTest (10), AuthProxyFilterTest (6). H2 memory cô lập, Clock điều khiển được, không kết nối production.
- Frontend: 15 test — entry (4), lineup (8), gameweek (3). Kiểm tra request CSRF/session/phiên bản, 409 không retry và response khác tài khoản không được áp dụng.
- Test bao phủ luật đội hình, quyền sở hữu, nháp độc lập snapshot, chốt lại, hồ sơ thay đổi, hai request đồng thời, dữ liệu cũ, đúng/sau hạn, chờ khóa và truy vấn/SQL kéo dài vượt hạn.
- Build backend và Vite đạt. Sau sửa check SQL NULL và tín hiệu session tự phát giữa tab, chỉ chạy lại test/build phần liên quan; không chạy toàn bộ kiểm thử auth/Google thật.
- MySQL 9.6 local cô lập trên loopback 33027: migration chạy hai lần giữ dữ liệu; kiểm tra duplicate entry/player, FK, phạm vi mùa, check NULL và rollback giữ đội chốt. Đây là kiểm chứng migration/SQL, chưa phải service đầu cuối trên MySQL production. Không sao chép tài khoản H2.
- Kiểm chứng lịch sử bước 3 gồm API nháp độc lập/snapshot, lưu đội đủ 11 người/OVR 858, lưu lại/reload, đổi GW không mang đội cũ, tab cũ 409, vòng khóa, logout/xóa đội riêng và quyền sở hữu ở desktop/390px. Đây không phải hướng dẫn người chơi dùng hai nút. Kiểm chứng luồng một nút hiện hành ở [fantasy-player-flow-2026.md](fantasy-player-flow-2026.md); production đã đạt theo checkpoint, không chạy lại lượt tài liệu này.
- Helper, tài khoản và seed thử nằm trong backend/target bị Git ignore; không đưa vào commit.

## Migration và trạng thái production

File MySQL: [2026-10-05-fantasy-entries-mysql.sql](../backend/sql/2026-10-05-fantasy-entries-mysql.sql). Tạo ba bảng bằng CREATE TABLE IF NOT EXISTS; không xóa/reset/seed dữ liệu. Yêu cầu bảng accounts và fantasy_gameweeks từ các migration trước đã tồn tại. Production giữ SQL initialization tắt như cấu hình hiện có.

Migration entry cùng ba migration Fantasy khác đã nhập production ngày 05/10, bao gồm [roster_as_of](../backend/sql/2026-10-05-fantasy-roster-reference-mysql.sql). GW6 OPEN/deadline và rosterAsOf đã cố định, đội thật ID 1/GW6 phiên bản 2 hợp lệ đã đối chiếu. Không chạy lại migration/luồng lưu đã đạt. Với một đích mới trong tương lai, vẫn cần được giao, xác nhận MySQL, backup mới và nhập theo đúng thứ tự; không tự mở lại vòng hết hạn.

Chấm điểm/kết quả/BXH đã có mã phát hành; GW6 còn chờ dữ liệu thực và ADMIN công bố, không tự công bố khi nhập thống kê. Chưa triển khai xem đội người khác, lịch sử mọi lần lưu đội, Replay hoặc quên mật khẩu.

## File của triển khai bước 3 trước đây

Backend:

- backend/sql/2026-10-05-fantasy-entries-mysql.sql
- backend/src/main/resources/fantasy-entry-schema.sql
- backend/src/main/resources/application.properties
- backend/src/main/java/com/premierhub/fantasy/FantasyEntryController.java
- backend/src/main/java/com/premierhub/fantasy/FantasyEntryRepository.java
- backend/src/main/java/com/premierhub/fantasy/FantasyEntryService.java
- backend/src/main/java/com/premierhub/fantasy/GameweekRepository.java
- backend/src/main/java/com/premierhub/fantasy/GameweekService.java
- backend/src/main/java/com/premierhub/service/FantasyLineupService.java
- backend/src/main/java/com/premierhub/accounts/AuthProxyFilter.java
- backend/src/main/java/com/premierhub/config/ApiCorsConfiguration.java
- backend/src/main/java/com/premierhub/config/SecurityConfiguration.java
- backend/src/test/java/com/premierhub/fantasy/FantasyEntryIntegrationTest.java
- backend/src/test/java/com/premierhub/accounts/AuthProxyFilterTest.java

Frontend:

- frontend/src/api/fantasyEntries.js
- frontend/src/fantasy/entry.js
- frontend/src/fantasy/entry.test.js
- frontend/src/hooks/useFantasyEntry.js
- frontend/src/components/FantasyEntry.css
- frontend/src/components/FantasyPage.jsx
- frontend/src/components/FantasyGameweek.jsx
- frontend/src/components/AccountMenu.jsx
- frontend/src/App.jsx

Tài liệu:

- docs/fantasy-user-lineups-2026.md
- docs/fantasy-multiplayer-2026-plan.md

Commit message đề xuất: feat(fantasy): save user drafts and submit gameweek lineups

Bước vận hành tiếp theo: thu thập GW6 theo checkpoint, chuẩn bị dữ liệu và quyền ADMIN có kiểm soát theo [hướng dẫn kết quả](fantasy-results-2026.md), chỉ nhập/công bố khi được giao.

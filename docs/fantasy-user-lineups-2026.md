# Đội nháp và đội đã chốt Fantasy 2026/27

Checkpoint 05/10/2026: hoàn thành mã, migration và kiểm tra local cho bước 3. Không ghi MySQL production, sửa thống kê bóng đá/mùa 2024/25, commit, push hoặc deploy. Không triển khai chấm rating, kết quả hay BXH.

## Lưu trữ và luật

- fantasy_entries có khóa chính (account_id, season, gameweek), thời điểm lưu/chốt, phiên bản hiện hành và metadata hai đội. Không tạo entry khi chỉ đọc; tài khoản chưa lưu có version 0 và chưa có đội.
- fantasy_draft_picks cho phép thiếu người hoặc rỗng. Người đã chọn vẫn phải hợp lệ theo sơ đồ, quyền vị trí, OVR và giới hạn CLB.
- fantasy_submitted_picks lưu snapshot sơ đồ/ô, ID, tên cầu thủ, CLB, OVR, vị trí chính, eligiblePositions và quyền vị trí dùng để xác nhận. Thay đổi hồ sơ hoặc membership về sau không viết lại snapshot.
- Các khóa chính, khóa duy nhất và khóa ngoại bảo đảm một entry hiện hành mỗi tài khoản/GW và không lặp cầu thủ trong từng đội. Snapshot không phụ thuộc khóa ngoại tới hồ sơ bóng đá có thể thay đổi.
- Dùng validator Fantasy hiện có: 11 ID khác nhau khi chốt, đúng ô của bốn sơ đồ hiện có, eligiblePositions hợp lệ, không thiếu OVR, tối đa 3 người/CLB và tổng OVR ≤ 860. Không nhận CLB/OVR/quyền vị trí từ client.
- Mỗi GW lưu rosterAsOf khi quản trị công bố/mở vòng: mặc định ngày công bố theo giờ Việt Nam, hoặc ngày được chọn có roster hiệu lực trong database. Danh sách chọn và validator dùng cùng mốc lưu này; không đổi theo ngày hiện tại. Mốc 2026-10-02 chỉ còn trong phần luyện tập của khách. Xem [bổ sung roster và đo khóa MySQL](fantasy-roster-reference-2026.md).
- Chỉ phục vụ GW6–GW38 mùa 2026/27; GW1–GW5 chưa triển khai Replay.

## Transaction, thời gian và nhiều tab

Thứ tự khóa là cấu hình GW → tài khoản → entry hiện hành. Sau khi lấy khóa, service kiểm tra Clock server và OPEN; từ đúng deadline trở đi bị từ chối. expectedVersion phải khớp bản đang lưu, nếu khác trả 409 và không tự ghi lại.

Validator đọc roster từ database một lần; dữ liệu đó cũng dùng tạo snapshot. Service kiểm tra giờ ngay trước ghi và sau các SQL ghi. Nếu hết hạn trong quá trình truy vấn/ghi, toàn bộ transaction rollback, kể cả snapshot cũ đã bị thay trong transaction. Sau lần kiểm tra cuối không truy vấn thêm để dựng response. Thời điểm ghi dùng UTC và cắt tới microsecond để không làm tròn một thời điểm trước hạn thành đúng hạn trong MySQL.

Chốt hợp lệ cũng lưu bản nháp hiện tại rồi thay đội chốt trong cùng transaction. Lưu nháp riêng không đổi snapshot. Lần chốt lỗi giữ nguyên đội đã chốt và phiên bản trước đó. Giới hạn thời gian dựa trên lần chấp nhận của server; việc commit hoặc phản hồi mạng có thể hoàn tất sau lần kiểm tra đó.

Khóa hàng cấu hình GW hiện tuần tự hóa thao tác ghi của mọi người trong cùng vòng. Phép đo local 20 tài khoản ghi đồng thời có hàng đợi lấy khóa cao nhất 1,76 giây, không lỗi; chưa benchmark tải lớn hoặc đo trên deployment thật. Giữ cơ chế khóa trong lượt bổ sung roster. Chỉ lưu đội chốt hiện hành, không lưu lịch sử mọi đội từng chốt; số phiên bản vẫn phát hiện dữ liệu cũ.

## API riêng của người đăng nhập

| Thao tác | Đường dẫn |
| --- | --- |
| Đọc | GET /api/fantasy/2026/me/gameweeks/{gameweek} |
| Lưu nháp | POST /api/fantasy/2026/me/gameweeks/{gameweek}/draft |
| Chốt/chốt lại | POST /api/fantasy/2026/me/gameweeks/{gameweek}/submit |

Body ghi gồm formation, picks (ô → player_id) và expectedVersion. Backend lấy tài khoản từ session. Header X-PrismaXI-Account-ID chỉ đối chiếu session còn thuộc tài khoản giao diện đang dùng; không dùng header hoặc body để chọn chủ đội. Session đổi tài khoản trả SESSION_CHANGED thay vì ghi lựa chọn của tab cũ sang tài khoản mới.

Giữ cookie HttpOnly/Secure theo cấu hình auth, CSRF cho POST và kiểm tra proxy production cho API riêng. Frontend dùng URL /api cùng origin, credentials include, lấy CSRF trước ghi. Không cache phản hồi đội riêng; không có API đọc nháp/đội của người khác. Lỗi gồm 401 chưa đăng nhập, 403 CSRF/quyền, 409 bản cũ/vòng khóa/session đổi và 422 luật đội hình.

## Giao diện

AccountMenu chia sẻ trạng thái tài khoản với App; đổi GW không gọi lại /me. Fantasy tải đúng dữ liệu server theo tài khoản/GW, hủy hoặc bỏ response của request cũ. Không tự chốt, tự nhập đội khách từ localStorage hay ghi đội riêng vào localStorage.

Giao diện có lưu nháp/chốt, loading/lỗi, tổng OVR, thời điểm chốt và lời nhắc khi nháp khác đội đã chốt. Khi khóa, sân hiển thị snapshot đội chốt và nút chỉnh/lưu/chốt bị vô hiệu hóa. Xung đột cần người dùng tải lại bản server; không retry ghi tự động.

Thông báo giữa các tab chỉ mang tín hiệu đổi session và ID tab, không mang tài khoản, đội hoặc token. Đăng xuất/đổi tài khoản xóa đội riêng ở các tab trước khi tải session mới. Không thay lựa chọn luyện tập của khách.

## Kiểm chứng đã thực hiện

- Backend: 31 test đúng phạm vi — FantasyEntryIntegrationTest (10), FantasyLineupServiceTest (5), GameweekIntegrationTest (10), AuthProxyFilterTest (6). H2 memory cô lập, Clock điều khiển được, không kết nối production.
- Frontend: 15 test — entry (4), lineup (8), gameweek (3). Kiểm tra request CSRF/session/phiên bản, 409 không retry và response khác tài khoản không được áp dụng.
- Test bao phủ luật đội hình, quyền sở hữu, nháp độc lập snapshot, chốt lại, hồ sơ thay đổi, hai request đồng thời, dữ liệu cũ, đúng/sau hạn, chờ khóa và truy vấn/SQL kéo dài vượt hạn.
- Build backend và Vite đạt. Sau sửa check SQL NULL và tín hiệu session tự phát giữa tab, chỉ chạy lại test/build phần liên quan; không chạy toàn bộ kiểm thử auth/Google thật.
- MySQL 9.6 local cô lập trên loopback 33027: migration chạy hai lần giữ dữ liệu; kiểm tra duplicate entry/player, FK, phạm vi mùa, check NULL và rollback giữ đội chốt. Đây là kiểm chứng migration/SQL, chưa phải service đầu cuối trên MySQL production. Không sao chép tài khoản H2.
- Trình duyệt desktop và 390px: chọn → lưu nháp thiếu người → chốt 11 người/OVR 858 → sửa nháp không đổi đội chốt → chốt lại → reload; đổi GW không mang đội cũ; tab cũ nhận 409; vòng khóa không cho ghi, không tràn ngang; logout xóa đội riêng cả hai tab; tài khoản B không nhận đội A. Không có lỗi JavaScript trong lượt thử.
- Helper, tài khoản và seed thử nằm trong backend/target bị Git ignore; không đưa vào commit.

## Migration và phát hành còn lại

File MySQL: [2026-10-05-fantasy-entries-mysql.sql](../backend/sql/2026-10-05-fantasy-entries-mysql.sql). Tạo ba bảng bằng CREATE TABLE IF NOT EXISTS; không xóa/reset/seed dữ liệu. Yêu cầu bảng accounts và fantasy_gameweeks từ các migration trước đã tồn tại. Production giữ SQL initialization tắt như cấu hình hiện có.

Khi được giao phát hành: xác nhận đúng MySQL đích, backup mới và kiểm tra hoàn tất; áp dụng migration này bằng quy trình SQL hiện có trước phát hành code. Nếu bước 2 chưa nhập, thực hiện migration GW trước. Trước code mới cần thêm [migration roster_as_of](../backend/sql/2026-10-05-fantasy-roster-reference-mysql.sql); các GW cũ lấy ngày công bố ban đầu theo giờ Việt Nam, không sửa snapshot. Cần cấu hình/công bố GW hợp lệ đang OPEN; không tự mở lại vòng đã hết hạn. Sau deploy kiểm tra qua origin frontend thật, bao gồm CSRF, quyền, nhiều tab/thiết bị và vòng khóa.

Chưa thực hiện các bước production trên. Chưa triển khai xem đội người khác, lịch sử mọi lần chốt, Replay, chấm điểm/kết quả/BXH hay quên mật khẩu.

## File cần commit

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

Bước tiếp theo theo kế hoạch: bước 4 — chấm và công bố, chỉ bắt đầu khi được yêu cầu.

# Mốc roster theo Fantasy Gameweek và phép đo khóa local

Checkpoint 05/10/2026: hoàn thiện phần bổ sung bước 3 trên local. Không ghi production, sửa dữ liệu bóng đá/mùa 2024/25, commit, push hoặc deploy. Không triển khai chấm điểm. Các kiểm tra auth đã đạt không chạy lại.

## Mốc lưu và API

fantasy_gameweeks.roster_as_of là DATE NOT NULL, cùng khóa (season, gameweek). Đây là ngày roster, không phải timestamp UTC. Timestamp công bố và deadline vẫn lưu UTC.

Khi quản trị gọi POST /api/fantasy/2026/admin/gameweeks/{gameweek}/publish-deadline, body có reason và có thể thêm rosterAsOf dạng YYYY-MM-DD. Bỏ rosterAsOf thì lấy ngày công bố theo Asia/Ho_Chi_Minh bằng Clock server. Database phải có ít nhất một membership hiệu lực tại ngày đó và player_season_stats tương ứng; nếu không có, trả 409, không tạo cấu hình/audit. Điều này xác nhận có dữ liệu, không chứng nhận roster đã đầy đủ hoặc mọi cầu thủ đủ OVR/vị trí.

Sau công bố, không có thao tác tự cập nhật mốc. Công bố lại bị từ chối; adjust-deadline chỉ đổi deadline/audit, giữ mốc roster. Quyền ADMIN, session, CSRF và proof proxy tiếp tục áp dụng cho thao tác công bố.

- GET /api/fantasy/2026/gameweeks và /gameweeks/{gameweek} trả rosterAsOf; GW chưa cấu hình trả NULL.
- GET /api/fantasy/2026/gameweeks/{gameweek}/players trả season, gameweek, rosterAsOf và players từ membership hiệu lực tại ngày đã lưu. Không nhận ngày do frontend quyết định; response no-store. Đây là dữ liệu roster công khai, không chứa đội riêng hoặc thông tin tài khoản.
- POST /api/fantasy/2026/validate?gameweek={gameweek} đọc mốc GW từ database. Lưu nháp/chốt dùng rosterAsOf từ chính hàng cấu hình đang giữ khóa trong transaction.

Frontend chọn GW rồi tải endpoint roster của GW đó; kiểm tra GW/mốc response khớp thông tin vòng trước khi áp dụng. Chờ/lỗi tải roster thì không cho lưu/chốt. Nút kiểm tra đội truyền GW, không truyền ngày hoặc thông tin CLB/OVR/vị trí để backend tin dùng. Không tải roster khách trong khi chờ session hoặc mốc GW của người đăng nhập.

Ngày 2026-10-02 chỉ còn dùng cho builder luyện tập của khách và endpoint validate không chỉ định GW. Luồng multiplayer không dùng mặc định đó. Snapshot đã chốt không được tính lại bằng roster/mốc/hồ sơ mới; chốt lại hợp lệ vẫn tạo snapshot mới như trước.

## Migration

[2026-10-05-fantasy-roster-reference-mysql.sql](../backend/sql/2026-10-05-fantasy-roster-reference-mysql.sql) là migration bổ sung, yêu cầu bảng fantasy_gameweeks đã có. Dùng information_schema và prepared statement để thêm cột khi thiếu; chạy lại không tạo trùng. Không sửa migration GW cũ đã có thể phát hành.

Với GW cũ chưa có mốc, migration gán DATE(deadline_published_at + INTERVAL 7 HOUR), rồi đặt NOT NULL. Đây là ngày công bố ban đầu theo Việt Nam, không phải ngày chạy migration. Mốc đã có giữ nguyên; không update bảng đội nháp/snapshot, account hoặc football. Trước phát hành cần đối chiếu ngày backfill của GW cũ và roster có tại ngày đó; migration không chứng nhận dữ liệu roster đầy đủ.

Schema H2 local có cùng cột và nâng cấp cột thiếu khi startup, backfill bằng ngày công bố Việt Nam, không reset dữ liệu. Production tiếp tục SQL initialization tắt; không dùng câu lệnh H2 để migrate MySQL.

Khi được giao production: xác nhận đích/backup, áp dụng migration GW nếu còn thiếu, migration entry nếu còn thiếu và migration roster bổ sung trước deploy code mới. Không sao chép tài khoản/seed của probe hoặc H2 lên production. Lượt này chỉ thực thi trên database local cô lập.

## Kiểm tra đúng phạm vi

Đã đạt 5 kịch bản backend mới và 3 test frontend; build hai phần đạt. Sau lỗi fixture nâng cấp H2, chỉ chạy lại phương thức schema còn lỗi; không chạy lại toàn bộ các test cũ.

- FantasyRosterReferenceTest, chỉ các phương thức roster*: hai GW 05/10 và 07/10 có membership khác nhau, API list và validator cùng dùng database dù client gửi ngày khác; mặc định ngày Việt Nam khi UTC còn ngày hôm trước; ngày chọn thiếu dữ liệu bị từ chối; công bố lại/điều chỉnh deadline không đổi mốc; snapshot giữ nguyên khi ngày/hồ sơ đổi; nâng cấp H2 chạy lại giữ mốc explicit.
- Frontend roster.test.js: hai GW gọi đúng endpoint và validate đúng GW; response GW/mốc cũ bị từ chối; GW đã cấu hình phải trả mốc roster.
- MySQL 9.6 local, database fantasy_roster_20261005, loopback 33027: migration hai lần; công bố UTC 04/10 18:00 được backfill 05/10 Việt Nam; mốc explicit giữ nguyên khi chạy lại.
- Build backend và frontend đạt; không chạy toàn bộ auth hoặc các test luật đội đã đạt. Trong lúc chuẩn bị fixture mới đã sửa lỗi thứ tự setup và khóa ngoại trong test, rồi chạy lại đúng lớp/phương thức liên quan. Schema H2 bổ sung cũng được kiểm tra lại đúng phạm vi.

Lệnh test backend thông thường: mvn "-Dtest=FantasyRosterReferenceTest#roster*" test (từ backend). Lệnh frontend: node --test src/fantasy/roster.test.js (từ frontend). Build dùng Maven package/Vite; Windows dùng POM kiểm tra trong target với classifier như lượt trước, không sửa POM nguồn/framework/dependency.

## Một tình huống ghi đồng thời trên MySQL local

Chạy chính FantasyEntryService của Spring với MySQL 9.6 local, không mô phỏng SQL ghi: 20 tài khoản khác nhau cùng GW6 bắt đầu đồng thời, 10 lưu nháp và 10 chốt. Pool tối đa 24, kết nối/validator đã làm nóng; không cố tình giữ khóa hoặc sleep. Probe chỉ bọc đo thời gian phương thức lấy khóa, không sửa code/cơ chế khóa, deadline hoặc expectedVersion.

| Chỉ số | Thấp nhất | Trung vị | P95 | Cao nhất |
| --- | ---: | ---: | ---: | ---: |
| Truy vấn/lấy khóa GW, gồm chờ hàng đợi (ms) | 1,143 | 1.078,568 | 1.702,506 | 1.760,919 |
| Toàn thao tác service + transaction (ms) | 54,651 | 1.136,985 | 1.760,816 | 1.845,062 |

Nhóm hoàn tất trong 1.846,906 ms. Không lỗi (0/20); database có 20 entry hiện hành, 10 đội chốt và đúng 110 snapshot cầu thủ của nhóm. Một truy vấn lấy khóa trước phép đo, không cạnh tranh, tốn 23,094 ms; đó là lần làm nóng, không phải baseline ổn định để trừ ra thời gian chờ thuần.

Số đo thời gian khóa bao gồm roundtrip/query MySQL và chờ row lock; không tách riêng thời gian chờ InnoDB. Kết quả cho thấy hàng đợi theo GW, nhưng chỉ là một tình huống local, không bao gồm HTTP/proxy/JDBC session và không đại diện phần cứng/network/tải production. Giữ khóa hiện tại theo phạm vi lượt này. Nếu muốn tối ưu sau, dùng số đo này làm căn cứ thiết kế và kiểm tra lại deadline/audit/chống ghi đè; không tự thay ngay.

Probe, SQL seed, log và JSON kết quả nằm trong backend/target/roster-reference-check, Git ignore. Không đưa dữ liệu thử vào commit hoặc production.

## File cần commit trong lượt bổ sung

- backend/sql/2026-10-05-fantasy-roster-reference-mysql.sql
- backend/src/main/resources/fantasy-gameweek-schema.sql
- backend/src/main/java/com/premierhub/config/SecurityConfiguration.java
- backend/src/main/java/com/premierhub/fantasy/GameweekRepository.java
- backend/src/main/java/com/premierhub/fantasy/GameweekService.java
- backend/src/main/java/com/premierhub/fantasy/GameweekController.java
- backend/src/main/java/com/premierhub/fantasy/FantasyEntryService.java
- backend/src/main/java/com/premierhub/service/FantasyLineupService.java
- backend/src/main/java/com/premierhub/web/FantasyController.java
- backend/src/test/java/com/premierhub/fantasy/FantasyRosterReferenceTest.java
- backend/src/test/java/com/premierhub/fantasy/FantasyEntryIntegrationTest.java
- backend/src/test/java/com/premierhub/fantasy/GameweekIntegrationTest.java
- backend/src/test/java/com/premierhub/web/FantasyControllerTest.java
- frontend/src/api/fantasy.js
- frontend/src/api/gameweeks.js
- frontend/src/components/FantasyGameweek.jsx
- frontend/src/components/FantasyPage.jsx
- frontend/src/fantasy/roster.test.js
- docs/fantasy-roster-reference-2026.md
- docs/fantasy-gameweeks-2026.md
- docs/fantasy-user-lineups-2026.md
- docs/fantasy-multiplayer-2026-plan.md

Commit message: fix(fantasy): use a gameweek-specific roster reference date

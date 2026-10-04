# Chấm điểm và công bố Fantasy 2026/27 — bước 4

Checkpoint 05/10/2026: hoàn thiện trên local, chỉ dùng dữ liệu giả lập cô lập. Chưa nhập migration/xác nhận nguồn lên production, chưa công bố cuộc thi thật, không xử lý cuộc thi chính thức GW1–5. Không xây BXH trong lượt này.

## Điểm và bằng chứng nguồn

- Đọc đội đã chốt từ `fantasy_entries` và snapshot `fantasy_submitted_picks`; không lấy bản nháp, lựa chọn trình duyệt hoặc đội của GW trước.
- Đọc `manual_fixture_player_stats` của league 39, season 2026, fixture thuộc đúng GW. Rating có giá trị là điểm SofaScore. `DID_NOT_PLAY` đã xác nhận nhận 0; `PLAYED` chỉ nhận 0 khi có xác nhận SofaScore không chấm. NULL chưa thu thập chặn công bố.
- Tính bằng `BigDecimal`; tổng lưu `DECIMAL(12,2)`. Một người có nhiều trận trong GW được cộng từng khóa fixture/player đúng một lần. Không dùng `fantasy_points` v1 hay OVR làm điểm.
- Kiểm tra tính hợp lệ bằng snapshot lúc chốt: 11 người/ô duy nhất, đúng sơ đồ và quyền vị trí đã lưu, tối đa 3 người/CLB, OVR không quá 860, thời điểm chốt trước deadline. Không áp lại rosterAsOf/hồ sơ hiện tại để thay đổi đội chốt.
- Breakdown giữ ô, ID, tên, CLB snapshot, vị trí, điểm người và từng fixture với CLB thực tế trong dòng thống kê, rating thô, điểm và mã lý do `SOFASCORE_RATING`, `DID_NOT_PLAY`, `SOFASCORE_UNRATED_CONFIRMED`.

Trước bước này, xác nhận người không được chấm chỉ có trong `confirmed-unrated.csv`/ghi chú nguồn. Hai bảng mới lưu bằng chứng tối thiểu:

| Bảng | Nội dung |
| --- | --- |
| `fantasy_unrated_confirmations` | PK fixture_id/player_id, nguồn, lý do, thời điểm xác nhận UTC; FK tới dòng thống kê thô |
| `fantasy_fixture_confirmations` | Fixture, hash dữ liệu đã kiểm tra, nguồn, thời điểm xác nhận UTC |

Hash fixture bao gồm trạng thái/trận, chỉ số thô, xác nhận không chấm, đội hình/sơ đồ/vai trò đã xác minh. Thay đổi dữ liệu sau xác nhận chặn công bố/tái tính cho tới khi kiểm tra lại CSV cuối. Membership vẫn kiểm tra trực tiếp theo ngày trận, không sửa membership. Hash không phụ thuộc thời điểm chạy importer.

## Nhập xác nhận bằng file hiện có

Giữ nguyên reader/importer thống kê và CSV 10 cột:

```text
season,fixture_id,player_id,status,rating,minutes,goals,assists,yellow_cards,red_cards
```

`FantasyEvidenceImporter` kiểm tra CSV cuối bằng `ManualMatchStatsCsvReader`, đối chiếu chính xác các khóa và từng giá trị với database đã nhập. Nó chỉ ghi bảng xác nhận mới; không nhập/sửa rating, fantasy_points, roster hoặc thống kê bóng đá. File nguồn phải tồn tại, không rỗng. Người chạy phải dùng nguồn/danh sách trận đã được xác minh theo [quy trình Gameweek](gameweek-data-workflow.md).

File xác nhận phải tên `confirmed-unrated.csv`; hỗ trợ ba header thực sự đã có trong repo:

```text
fixture_id,player_id,name,minutes,rating,fantasy_points
fixture_id,club,player_id,name,status,minutes
fixture_id,club,player_id,name,status,field,reason
```

Chỉ dùng một header trong một file. rating/fantasy_points phải trống; status nếu có phải PLAYED; field nếu có phải rating. Khóa không trùng, phút nếu có phải khớp dòng lưu, lý do không rỗng. Không đưa danh sách “rating đang chờ” vào file xác nhận. Reader sidecar không hỗ trợ ô chứa dấu phẩy được bọc quote; đây là giới hạn các file hiện có.

Lệnh thật, từ `backend/`, sau khi đã nhập thống kê và đội hình vào **database local cô lập** và đã cấu hình datasource local. Thay đường dẫn bằng CSV cuối/nguồn của GW cần xử lý; ví dụ sau không được dùng với credentials production:

```powershell
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=auth-local "--spring.datasource.url=jdbc:h2:file:./target/fantasy-results-local;MODE=MySQL;DATABASE_TO_LOWER=TRUE" --spring.sql.init.mode=never --premierhub.fantasy-evidence.enabled=true --premierhub.fantasy-evidence.stats-file=data/manual-gw6/stats.csv --premierhub.fantasy-evidence.unrated-file=data/manual-gw6/confirmed-unrated.csv --premierhub.fantasy-evidence.source-file=data/manual-gw6/sources.md
```

Đường dẫn H2 trong ví dụ là database local đã chuẩn bị schema/dữ liệu; thay bằng datasource thử nghiệm đã nhập tương ứng, không trỏ production. Profile auth-local mặc định dùng target/auth-local, nên phải truyền datasource qua command nếu dùng database cô lập khác. Các cờ `stats-file`/`unrated-file` là tùy chọn nhưng phải có ít nhất một; `source-file` bắt buộc. Nếu không có ai không được chấm thì file sidecar chỉ có header. Chỉ nhập sidecar chưa xác nhận hoàn tất fixture. Có đầy đủ CSV cuối thì importer kiểm tra dữ liệu/vai trò của cả hai đội và lưu hash xác nhận fixture. Không ép 40 dòng/trận hoặc 20 người/CLB.

Khi cung cấp CSV cuối cùng sidecar, sidecar là danh sách xác nhận hiện hành cho các fixture đầy đủ trong CSV: xác nhận cũ không còn trong file sẽ bị bỏ, trong cùng transaction. Nhờ vậy, sau khi rating nguồn được sửa từ NULL thành số, có thể xác nhận lại mà không giữ ngoại lệ không chấm đã lỗi thời. Chỉ nhập sidecar riêng không xóa xác nhận khác. Xung đột nguồn/lý do đã lưu hoặc giá trị CSV/database bị từ chối; cần giải quyết có kiểm soát trước khi nhập lại.

Output chỉ có `FANTASY_EVIDENCE fixtures=... fixture_changes=... unrated_changes=...`; chạy lại cùng file có changes=0. Command chạy không mở HTTP server và đóng context khi xong. Đã bổ sung điều kiện chế độ web cho AuthController/filter chain để command không phụ thuộc AuthenticationConfiguration/HttpSecurity; cơ chế bảo vệ web giữ nguyên.

## Readiness và công bố

API dưới `/api/fantasy/2026`, không cache:

| Method/path | Quyền và nội dung |
| --- | --- |
| GET `/admin/gameweeks/{gw}/readiness` | ADMIN; điều kiện, blocker cụ thể, số fixture/đội chốt, phiên bản hiện tại và lịch sử lý do/người công bố |
| POST `/admin/gameweeks/{gw}/publish-results` | ADMIN + CSRF; công bố lần đầu |
| POST `/admin/gameweeks/{gw}/recalculate-results` | ADMIN + CSRF; tái tính toàn GW đã công bố |
| GET `/me/gameweeks/{gw}/result` | Chủ session; cần X-PrismaXI-Account-ID để phát hiện phiên đã đổi; không nhận user_id để chọn người khác |

Body POST chỉ cần `{"expectedVersion":0,"reason":"Đã xác nhận dữ liệu toàn vòng"}`. Backend không nhận điểm từ client. Lần tái tính dùng version hiện tại và lý do mới. Các route ADMIN/me tiếp tục dùng proof proxy, session JDBC và CSRF hiện có; CORS admin thêm GET cho readiness, không mở origin/quyền mới.

Readiness chặn nếu:

- Cuộc thi chưa mở, thiếu danh sách fixture hoặc thời gian Clock server còn trước deadline. Tại đúng deadline là đã hết hạn chốt và có thể kiểm tra công bố.
- Bất kỳ fixture nào chưa FINISHED, kể cả hoãn; không công bố phần đã xong.
- Thiếu xác nhận CSV/danh sách cuối của cả hai đội hoặc hash đã đổi.
- Thiếu sơ đồ thực tế/vai trò và nguồn đã xác minh, thiếu đúng 11 STARTER mỗi đội; người vào thay/dự bị dùng danh sách thực tế. Không suy từ phút, rating hay sơ đồ mặc định CLB.
- ID/vai trò/CLB/status không khớp; thiếu membership hợp lệ tại ngày trận. PLAYED thiếu phút/bàn/kiến tạo/thẻ hoặc rating chưa xác nhận. DNP vi phạm phút 0/rating NULL hoặc có sự kiện dương.
- Người được chọn thiếu dòng thống kê trong fixture của CLB họ thuộc tại ngày trận, hoặc không xác định được trận có căn cứ trong GW. Không tự coi mất dòng là DNP.
- Snapshot đội chốt không hợp lệ hoặc chốt từ deadline trở đi.

Issue có `fixtureId`, `playerId`, `accountId` khi xác định được, `code`, `message`; không có điểm tạm. Lỗi POST chưa sẵn sàng là 409 `RESULTS_NOT_READY` cùng readiness. Danh sách fixture lấy từ database của GW, không suy danh sách/sự hoàn tất từ tổng số dòng; công bố cuộc thi GW ban đầu vẫn dùng kiểm tra lịch đã có ở bước 2.

Công bố dùng transaction SERIALIZABLE, khóa GW rồi đọc/khóa nguồn và đội tham gia. Lưu phiên bản, kết quả tất cả đội, cuối cùng mới đặt GW PUBLISHED; lỗi một đội rollback toàn GW. Hash phiên bản dựa trên nguồn đã xác nhận và snapshot đội chốt, không dựa bản nháp. Gọi lại với cùng dữ liệu trả `unchanged=true`, không tạo phiên bản/lịch sử giả. Dữ liệu đổi cần xác nhận nguồn mới, version đúng và thao tác tái tính có lý do. Tái tính lưu bản mới cho toàn vòng, giữ bản cũ và lịch sử; API đọc một phiên bản nhất quán. Trong lúc chờ tái tính thành công, bản đã công bố trước tiếp tục là kết quả hiện hành.

Hai bảng kết quả: `fantasy_result_publications` (PK mùa/GW/version, hash, người/thời điểm, PUBLISH/RECALCULATE, lý do) và `fantasy_team_results` (PK tài khoản/mùa/GW/version, submitted_version, tổng và JSON breakdown). Không có bảng BXH trong lượt này.

## Giao diện và dữ liệu riêng

Fantasy có khu vực kết quả đội mình: tải/lỗi/thử lại, “Đang chờ kết quả” không kèm điểm; khi công bố hiển thị tổng, version, thời gian Việt Nam và 11 dòng gồm điểm từng trận/lý do 0. Không chốt đội thì NOT_PARTICIPATING, không tự tạo điểm 0. ADMIN có kiểm tra readiness, danh sách blocker, lý do, nút công bố/tái tính và lịch sử phiên bản. Sự kiện công bố tải lại trạng thái GW.

Đổi account/GW hủy request cũ và xóa kết quả trước; backend luôn xác định chủ từ session. API/result không công khai email hoặc bản nháp. Không tự nhập lựa chọn localStorage, không đổi đội chốt, không có API xem kết quả người khác trong bước này.

## Migration và kiểm chứng

Schema local: [fantasy-result-schema.sql](../backend/src/main/resources/fantasy-result-schema.sql). Migration: [2026-10-05-fantasy-results-mysql.sql](../backend/sql/2026-10-05-fantasy-results-mysql.sql), MySQL 8.0.17+, bốn bảng bổ sung, FK/index/unique, UTC, CREATE TABLE IF NOT EXISTS; cần các migration tài khoản/GW/entry và bảng dữ liệu bóng đá trước đó. Không có DROP/reset/seed hay sao chép tài khoản H2. Production giữ SQL init tắt; bước backup/nhập migration/phát hành chỉ thực hiện khi được giao riêng.

- 10 test backend `FantasyResultIntegrationTest` đạt: thập phân, DNP/unrated, NULL chờ và thiếu dòng, hạn/trận hoãn/chưa xác nhận, nhiều trận, nhập lặp/xung đột, ba header sidecar, công bố lặp/tái tính, thay ngoại lệ bằng rating, rollback lỗi đội thứ hai, ADMIN/CSRF/quyền chủ. Chỉ chạy phần mới, không chạy lại toàn bộ auth.
- 4 test frontend `results.test.js` đạt; Maven package và Vite build đạt. Các thay đổi phát hiện trong kiểm tra được xác minh lại đúng phạm vi. Maven dùng POM build kiểm tra/classifier trong target vì lỗi đổi tên JAR Windows trước đó; JUnit TEMP/TMP được đặt trong target để tránh quyền dọn thư mục temp hệ thống. Không commit đầu ra build.
- MySQL 9.6 loopback riêng, database `fantasy_results_20261005`: service thực tạo 2 version/4 kết quả cho 2 đội; tổng 66.77 → 67.77, công bố lặp không thêm bản, rating NULL thô giữ nguyên. Chạy migration lại giữ 2 version/4 kết quả. Đây là kiểm chứng MySQL local, chưa phải Railway.
- Command từ JAR cũng đã chạy với datasource MySQL cô lập trên: exit 0, `fixtures=2 fixture_changes=0 unrated_changes=0`. Lịch sử version/lý do/người công bố được đối chiếu API và giao diện mở ở 390px; không tràn ngang.
- Trình duyệt gọi backend H2 cô lập qua frontend local: chờ không điểm → ADMIN readiness/công bố → trạng thái PUBLISHED → gọi lặp giữ version → xem 11 dòng/0 có lý do → reload → đổi GW không mang kết quả cũ. GW thử có fixture hoãn hiển thị đúng fixture và chặn nút công bố. Desktop 1440px và mobile 390px không tràn ngang/chồng hàng; không có lỗi JavaScript.
- Toàn bộ seed/helper/ảnh/report nằm trong target được ignore. Dữ liệu giả lập có 24/22 dòng mỗi fixture, chứng minh readiness không yêu cầu 40. Không đọc/nhập/công bố cuộc thi thật GW1–5 trong lượt này.

## Còn lại và bước tiếp theo

Chưa nhập xác nhận cho dữ liệu thực, chưa khẳng định GW nào thật đã sẵn sàng. Trước dùng production cần backup, migration các bước chưa phát hành, ADMIN có kiểm soát, nhập thống kê/đội hình/ngoại lệ thực đã được xác nhận và kiểm tra readiness. Không có dữ liệu thực nào được thay bằng seed local.

Transaction toàn GW cần giữ khóa nguồn/đội trong lúc chấm; đã kiểm chứng rollback/nhất quán, chưa đo tải production. Source_ref lưu đường dẫn/nguồn và hash dữ liệu database; phải giữ file nguồn/checkpoint, không coi file nguồn tồn tại là bằng chứng đã thu thập xong. BXH GW/mùa và xem đội người khác thuộc bước 5, chỉ bắt đầu khi được giao.

File cần commit (target/dist không commit):

- `backend/sql/2026-10-05-fantasy-results-mysql.sql`
- `backend/src/main/resources/fantasy-result-schema.sql`, `application.properties`
- `backend/src/main/java/com/premierhub/PremierHubApplication.java`
- `backend/src/main/java/com/premierhub/accounts/AuthController.java`
- `backend/src/main/java/com/premierhub/config/ApiCorsConfiguration.java`, `SecurityConfiguration.java`
- `backend/src/main/java/com/premierhub/service/FantasyLineupService.java`
- `backend/src/main/java/com/premierhub/fantasy/FantasyEvidenceCommand.java`, `FantasyEvidenceImporter.java`, `FantasyResultRepository.java`, `FantasyResultService.java`, `FantasyResultController.java`
- `backend/src/test/java/com/premierhub/fantasy/FantasyResultIntegrationTest.java`
- `frontend/src/api/fantasyEntries.js`, `fantasyResults.js`
- `frontend/src/components/FantasyGameweek.jsx`, `FantasyPage.jsx`, `FantasyResults.jsx`, `FantasyResults.css`
- `frontend/src/fantasy/results.js`, `results.test.js`
- `docs/fantasy-results-2026.md`, `fantasy-multiplayer-2026-plan.md`, `gameweek-data-workflow.md`

Commit message: `feat(fantasy): score submitted lineups and publish gameweek results`.

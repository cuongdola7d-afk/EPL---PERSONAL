# Bố cục đội hình trận mùa 2026/27

## Đã hoàn thành local

- Thêm dữ liệu riêng cho sơ đồ CLB, sơ đồ từng trận và vai trò đăng ký trận. Không thêm cột vào thống kê, membership hoặc vị trí Fantasy.
- Chuẩn bị batch [`match-lineups-2026-10-04`](../backend/data/match-lineups-2026-10-04/sources.md): CSV chung cho 20 CLB; 1.560 vai trò đã phục hồi của 39 trận; danh sách cần phục hồi nguồn. Người dùng chốt defaultFormation ngày 04/10/2026: 14 CLB dùng 4-2-3-1, 5 CLB dùng 3-4-3, Hull City dùng 5-4-1. Ghi nguồn USER; số trận xác minh sơ đồ thực tế vẫn bằng 0.
- Importer kiểm tra mùa 2026, CLB của fixture, 11 STARTER, ID trùng, nguồn SofaScore cho sơ đồ mới, tọa độ ô, bằng chứng thay người, sự khớp giữa vai trò và participation_status đã lưu. Toàn batch chạy trong transaction; lỗi rollback. Chạy lại cùng dữ liệu không tạo trùng hoặc cập nhật lại.
- Với nguồn OBSERVED, defaultFormation được tính từ các observation đã xác minh: đếm tần suất, nếu hòa chọn sơ đồ dùng ở trận gần nhất theo ngày trận; ID fixture làm quy tắc ổn định khi cùng ngày. Với nguồn USER, dùng sơ đồ người dùng chốt và bắt buộc ghi chú nguồn. Trong cả hai trường hợp, số trận/tần suất/danh sách fixture vẫn phải khớp observation thật; quyết định USER không tự tạo bằng chứng trận. Nguồn MISSING chỉ dùng khi chưa có default và chưa có observation.
- API chi tiết trận chỉ thêm `homeLineup`/`awayLineup` ở mùa 2026. Mùa 2024 vẫn dùng luồng và JSON cũ; luật Fantasy giữ nguyên.
- Frontend ưu tiên sơ đồ fixture đã xác minh, tiếp theo defaultFormation của CLB và nhãn **“Bố trí theo sơ đồ thường dùng”**. Thiếu cả hai hiển thị **“Chưa có sơ đồ”**. Mỗi đội có sơ đồ riêng; parser hỗ trợ các chuỗi 2–5 tuyến với tổng 10 cầu thủ ngoài thủ môn, độc lập các sơ đồ Fantasy.
- 11 người trên sân phải thuộc vai trò STARTER có bằng chứng. Bỏ hoàn toàn cách xếp 11 người theo số phút/rating cho mùa 2026. Không có vai trò đủ tin cậy thì chỉ hiển thị danh sách chưa xác nhận, không tạo đội hình giả.
- Tọa độ đã xác minh trong trận được giữ trước. Các ô còn thiếu bố trí minh họa theo vị trí mùa/vị trí trận đã biết, gắn nhãn minh họa; không ghi các ô này thành vị trí thi đấu thực tế. Không dùng eligiblePositions để xác nhận đá chính hoặc ghi đè vị trí Fantasy.
- Tách danh sách đá chính, đã vào thay và dự bị không vào sân. Rating/thẻ/phút đi cùng player_id; dấu thay vào dựa trên SUB_USED, thời điểm vào/ra chỉ hiện khi đã có bằng chứng. Không suy thời điểm thay người từ số phút.

## SQL và importer

Ba bảng mới trong `schema.sql` và bản DDL riêng [`schema-mysql.sql`](../backend/data/match-lineups-2026-10-04/schema-mysql.sql):

| Bảng | Khóa | Dữ liệu |
| --- | --- | --- |
| club_season_formations | league_id + season_year + club_id | default_formation, updated_on, default_source, source_note, scope_from_gw/to_gw, verified_matches, formation_counts, fixture_ids |
| fixture_lineups | fixture_id + club_id | sơ đồ và nguồn/ngày xác minh sơ đồ; nguồn/ngày xác minh vai trò |
| fixture_lineup_players | fixture_id + club_id + player_id | STARTER/SUB_USED/SUB_UNUSED, match_position, row_index/slot_index, phút thay vào/ra đã xác minh |

`row_index=0` là tuyến thủ môn; những tuyến sau theo thứ tự phòng ngự → tấn công. `slot_index=0` là ô đầu tuyến theo thứ tự của sơ đồ nguồn. Các cặp row/slot phải nằm trong formation của fixture, không được trùng; chỉ STARTER mới có ô. Các ô trống giữ SQL NULL.

Các CSV dùng UTF-8, cột không chứa dấu phẩy/quote; bỏ trống ô để ghi NULL. Header nằm trong `LineupBatchImporter`. `clubs.csv` thêm hai cột `default_source` (USER/OBSERVED/MISSING) và `source_note`; nguồn USER bắt buộc có sơ đồ hợp lệ và ghi chú. Thứ tự trong `formation_counts` là thứ tự tên sơ đồ tăng dần, phân cách `;`; ví dụ `3-4-2-1=2;4-3-3=1`. `fixture_ids` sắp theo ngày trận, rồi ID. Phạm vi GW mô tả observation; default USER áp dụng toàn mùa khi thiếu sơ đồ fixture.

Lệnh dưới chỉ dành cho **H2 local đã có bản sao fixtures/players/statistics**, không trỏ datasource vào MySQL:

```powershell
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar '--spring.datasource.url=jdbc:h2:file:./target/formation-local;MODE=MySQL;DATABASE_TO_LOWER=TRUE' '--spring.datasource.username=sa' '--spring.datasource.password=' '--premierhub.match-lineups.enabled=true' '--premierhub.match-lineups.directory=data/match-lineups-2026-10-04'
```

Importer không nhập bảng cầu thủ, fixtures, membership hoặc thống kê. H2 rỗng sẽ bị từ chối vì không có các khóa nguồn. Bản kiểm tra lượt này nằm trong `backend/target/formation-check/`, bị Git ignore.

## Kiểm tra trước khi người dùng chốt default

- Backend: 21 test trong 5 lớp liên quan đều đạt; Maven package thành công. Các lớp: `LineupBatchImporterTest` (4), `ManualMatchDetailTest`, `FixtureEvidenceServiceTest`, `MatchKickoffQueryTest`, `FantasyLineupServiceTest` (17 test còn lại). Lượt đầu phát hiện lỗi CHECK của H2 và quyền xóa thư mục tạm JUnit trên Windows; đã sửa CHECK và đặt thư mục tạm trong `target/`. Chạy lại phần bị lỗi để xác nhận, không tạo test theo từng CLB.
- Frontend: một lượt `node --test src/utils/matchLineup.test.js src/utils/matchView.test.js src/api/matches.test.js src/fantasy/lineup.test.js` — 15 test đạt. Một lượt `npm.cmd run build` — thành công.
- H2 local được dựng từ snapshot đọc trước đó: 20 CLB, 50 fixture và 2.000 dòng thống kê. Import batch: 20 dòng sơ đồ CLB chưa có default; 78 lượt CLB có vai trò; 1.560 vai trò cầu thủ. Import lại: `clubChanges=0`, `fixtureChanges=0`, `playerChanges=0`. So sánh toàn bộ thống kê trước/sau không có thay đổi. API trả 78 XI VERIFIED và 22 lượt CLB thiếu vai trò; không sinh sơ đồ chưa xác minh.
- Chrome desktop 1440px và mobile 390px: thử cặp `4-2-3-1` / `3-4-2-1`, thêm cặp `4-5-1` / `3-1-3-2-1` để kiểm tra nhiều tuyến. Đủ 22 thẻ đá chính, ID không trùng, đúng hai nửa sân; đo hình chữ nhật không chồng thẻ, không tràn ngang hoặc ra ngoài sân. Kiểm tra nhãn sơ đồ fixture/default, nhãn minh họa, rating/thẻ/thay người theo ID và link cầu thủ mùa 2026. Không có lỗi JavaScript.
- Kiểm tra browser trên payload từ H2 thật: trận có vai trò nhưng thiếu formation hiện hai danh sách 11 đá chính, không dựng sân giả; GW1 thiếu cả vai trò/sơ đồ hiện danh sách chưa xác nhận. Kết quả và ảnh kiểm tra nằm trong `backend/target/formation-check/`, không commit.

Fixture dùng hai sơ đồ khác nhau trong test/browser là dữ liệu kiểm thử, **không phải** observation được xác minh của CLB mùa 2026/27. Không đưa các sơ đồ thử vào CSV nguồn hoặc production.

## Kiểm tra sau khi người dùng chốt default ngày 04/10/2026

- Một lượt Maven package với 5 lớp test liên quan: **22 test đạt**, BUILD SUCCESS. Test mới xác nhận default USER khi chưa có observation, số đếm vẫn bằng 0, nguồn được trả qua API, importer idempotent; khi có sơ đồ fixture thật thì ưu tiên fixture và giữ default USER. Từ chối nguồn USER không có ghi chú hoặc default OBSERVED không khớp observation.
- H2 local mới: cả 20 default khớp đúng CSV; 14 × 4-2-3-1, 5 × 3-4-3, 1 × 5-4-1. API của 50 fixture trả fallback CLB và ghi chú quyết định người dùng; `verified_matches=0`, không tạo observation giả. Import lần hai có 0 thay đổi. Toàn bộ 2.000 dòng thống kê giữ nguyên; 78 lượt CLB có nhãn đá chính đã phục hồi và 22 lượt chưa phục hồi không đổi.
- Chrome 1440px/390px với payload API từ H2: kiểm tra đủ cả 4-2-3-1, 3-4-3, 5-4-1; đúng 11 đá chính mỗi đội, không trùng ID/chồng thẻ/tràn ngang hoặc ra ngoài sân. Có nhãn fallback và minh họa. Fixture GW1 chưa phục hồi nhãn đá chính vẫn báo thiếu vai trò, dù đã có default; không sinh XI giả. Không có lỗi JavaScript. Kết quả ở `backend/target/formation-check/*user-default*`, không commit.
- Không sửa frontend trong lượt chốt default; dùng component/parser đã kiểm tra ở lượt trước.

## Còn lại trước nhập SQL/phát hành

1. Phục hồi ảnh/nguồn Lineups đã thu thập trước đây; chỉ cần người dùng bổ sung phần không tìm lại được. Thống kê đã có đủ 50 trận, không thu thập lại rating/chỉ số. `missing-lineups.csv` theo dõi bằng chứng sơ đồ/vị trí thực tế và 11 trận chưa phục hồi nhãn vai trò, không phải danh sách trận thiếu thống kê.
2. Giữ 20 default USER đã chốt. Khi có sơ đồ fixture thật, nhập observation và số đếm tương ứng; API ưu tiên fixture đó, không đổi default USER sang kết quả tần suất nếu chưa được yêu cầu.
3. Sau khi được giao bước nhập production: sao lưu, áp dụng riêng DDL mới, chạy importer đã kiểm tra, chạy lần hai xác nhận thay đổi bằng 0 và đọc lại. Lượt này **chưa thực hiện** DDL hay nhập dữ liệu MySQL production.
4. Phát hành backend trước frontend để có metadata mới. Nếu frontend gặp backend chưa cập nhật, nó báo thiếu vai trò/sơ đồ và không dựng XI từ thống kê.

Không commit, push hoặc deploy trong lượt này.

DDL hiện tại dành cho tạo mới các bảng metadata chưa nhập production. Trước **import**, database có bảng cũ cần nâng cấp hai cột nguồn và CHECK cũ; `CREATE TABLE IF NOT EXISTS` không tự nâng cấp bảng đã có. API đọc hiện tương thích schema cũ theo bản sửa dưới đây, không cần chạy DDL để xem trận. Không sửa/xóa database local cũ của người dùng.

## Sửa HTTP 500 khi xem trận ngày 04/10/2026

- Xác nhận bằng kết nối MySQL **chỉ đọc**: bảng `club_season_formations` đang có 10 cột của phiên bản trước, chưa có `source_note` hoặc `default_source`. Ba bảng metadata đội hình đã tồn tại. Query mới yêu cầu `source_note` nên gây lỗi SQLState 42S22 cho mọi trận; `CREATE TABLE IF NOT EXISTS` không thêm cột thiếu. Vị trí mùa NULL không phải nguyên nhân trên dữ liệu production đã kiểm tra (0 fixture có NULL), nhưng có thể gây NPE nên được xử lý giữ nguyên NULL.
- `MatchLineupQueries` đọc được cấu trúc cũ không có ghi chú. Chỉ phục hồi khi thiếu bảng/cột metadata tùy chọn; lỗi truy cập database và thống kê bắt buộc vẫn báo lỗi, không bị biến thành kết quả rỗng. Có log cảnh báo khoảng trống schema.
- Maven đóng gói đúng hai CSV nguồn `clubs.csv` và `players.csv` vào JAR, không sao chép rating/thống kê. Khi SQL chưa có default hoặc vai trò, API dùng quyết định 20 CLB và vai trò đã thu thập từ CSV. Metadata SQL đã có vẫn được ưu tiên; tọa độ/thời điểm thay người chỉ đọc từ metadata trận SQL đã xác minh. Không có thao tác import/DDL hoặc ghi database trong đường đọc.
- Vai trò dự phòng phải khớp toàn bộ ID và participation_status của đội trong fixture, đủ 11 STARTER và không trùng cầu thủ. Không khớp thì INCOMPLETE, không dựng XI giả. Trận chưa có bằng chứng vai trò vẫn MISSING. Vị trí mùa lấy từ SQL nếu có; các vị trí minh họa không được ghi thành vị trí trận thực tế.
- Một lượt test backend: **26 test đạt** trong 6 lớp liên quan; Maven package cuối cùng thành công. Test mới gọi API thật qua MockMvc cho schema thiếu cột, thiếu toàn bộ metadata, dữ liệu NULL và participation không khớp; đồng thời bảo đảm lỗi bảng thống kê bắt buộc không bị che giấu và không ghi dữ liệu nguồn.

- Code mới đọc trực tiếp MySQL hiện tại bằng connection read-only, không khởi động Spring/schema initializer: **50/50 fixture GW1–GW5 đọc thành công**, tổng 2.000 dòng cầu thủ, đầy đủ default 20 CLB, 78 lượt CLB có XI VERIFIED và 22 lượt chưa phục hồi vai trò. Không chạy DDL/import hoặc cập nhật dữ liệu production. Đây là kiểm tra code mới với datasource thật, **không phải** đã phát hành bản sửa lên API đang chạy.
- Chrome với các response vừa đọc, desktop 1440px và mobile 390px: cả 4-2-3-1, 3-4-3, 5-4-1 đều có 22 thẻ đá chính đúng ID, không chồng hoặc tràn; hiển thị nhãn fallback/minh họa. GW1 có default nhưng thiếu vai trò vẫn không dựng XI giả. Không có lỗi JavaScript. HTTP 200 đã được kiểm tra qua MockMvc; browser dùng response được phục vụ qua mock API local, không gọi/ghi production.
- Đã kiểm tra JAR chứa đúng bytes của hai CSV nguồn và xem diff. Không sửa frontend, không commit, push hoặc deploy. Website đang chạy cần phát hành backend mới để nhận bản sửa; nâng cấp schema/nhập metadata production vẫn là bước riêng chưa thực hiện.

File cần commit cho bản sửa lỗi:

- `backend/pom.xml`
- `backend/src/main/java/com/premierhub/lineups/BundledMatchLineups.java`
- `backend/src/main/java/com/premierhub/lineups/MatchLineupQueries.java`
- `backend/src/main/java/com/premierhub/service/FootballQueries.java`
- `backend/src/test/java/com/premierhub/lineups/MatchLineupCompatibilityTest.java`
- `docs/match-lineups-2026.md`

Commit message: `fix: keep match details compatible with legacy lineup schema`

## File từ lượt triển khai đội hình trước

Backend/API/importer:

- `backend/src/main/java/com/premierhub/PremierHubApplication.java`
- `backend/src/main/java/com/premierhub/service/FootballQueries.java`
- `backend/src/main/java/com/premierhub/web/dto/MatchDetailResponse.java`
- `backend/src/main/java/com/premierhub/web/dto/MatchLineupResponse.java`
- `backend/src/main/java/com/premierhub/lineups/Formation.java`
- `backend/src/main/java/com/premierhub/lineups/FormationSummary.java`
- `backend/src/main/java/com/premierhub/lineups/MatchLineupQueries.java`
- `backend/src/main/java/com/premierhub/lineups/LineupBatchImporter.java`
- `backend/src/main/java/com/premierhub/lineups/LineupBatchCommand.java`
- `backend/src/main/resources/schema.sql`
- `backend/src/test/java/com/premierhub/lineups/LineupBatchImporterTest.java`

Frontend:

- `frontend/src/components/MatchDetail.jsx`
- `frontend/src/components/MatchPage.css`
- `frontend/src/utils/matchLineup.js`
- `frontend/src/utils/matchLineup.test.js`

Dữ liệu/nguồn/tài liệu:

- `backend/data/match-lineups-2026-10-04/clubs.csv`
- `backend/data/match-lineups-2026-10-04/formations.csv`
- `backend/data/match-lineups-2026-10-04/players.csv`
- `backend/data/match-lineups-2026-10-04/missing-lineups.csv`
- `backend/data/match-lineups-2026-10-04/sources.md`
- `backend/data/match-lineups-2026-10-04/schema-mysql.sql`
- `docs/match-lineups-2026.md`

Commit message đề xuất: `feat: add verified 2026/27 match lineups and club formation defaults`

## File cần commit cho lượt chốt default

- `backend/data/match-lineups-2026-10-04/clubs.csv`
- `backend/data/match-lineups-2026-10-04/schema-mysql.sql`
- `backend/data/match-lineups-2026-10-04/sources.md`
- `backend/src/main/java/com/premierhub/lineups/LineupBatchImporter.java`
- `backend/src/main/java/com/premierhub/lineups/MatchLineupQueries.java`
- `backend/src/main/resources/schema.sql`
- `backend/src/test/java/com/premierhub/lineups/LineupBatchImporterTest.java`
- `docs/match-lineups-2026.md`

Commit message đề xuất: `feat: set user-confirmed club formations for 2026/27`

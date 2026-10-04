# Bố cục đội hình trận mùa 2026/27

## Đã hoàn thành local

- Thêm dữ liệu riêng cho sơ đồ CLB, sơ đồ từng trận và vai trò đăng ký trận. Không thêm cột vào thống kê, membership hoặc vị trí Fantasy.
- Chuẩn bị batch [`match-lineups-2026-10-04`](../backend/data/match-lineups-2026-10-04/sources.md): CSV chung cho 20 CLB; 1.560 vai trò đã lưu của 39 trận; danh sách cần bổ sung ảnh. Chưa có sơ đồ nào đủ bằng chứng, nên mọi defaultFormation để NULL và số trận kiểm tra sơ đồ bằng 0. Dữ liệu còn thiếu không được coi là 4-3-3.
- Importer kiểm tra mùa 2026, CLB của fixture, 11 STARTER, ID trùng, nguồn SofaScore cho sơ đồ mới, tọa độ ô, bằng chứng thay người, sự khớp giữa vai trò và participation_status đã lưu. Toàn batch chạy trong transaction; lỗi rollback. Chạy lại cùng dữ liệu không tạo trùng hoặc cập nhật lại.
- defaultFormation được tính từ các observation đã xác minh trong phạm vi GW của CLB: đếm tần suất, nếu hòa chọn sơ đồ dùng ở trận gần nhất theo ngày trận; ID fixture làm quy tắc ổn định khi cùng ngày. CSV phải khớp kết quả tính, số trận và danh sách fixture; không chỉ tin một chuỗi sơ đồ tự nhập.
- API chi tiết trận chỉ thêm `homeLineup`/`awayLineup` ở mùa 2026. Mùa 2024 vẫn dùng luồng và JSON cũ; luật Fantasy giữ nguyên.
- Frontend ưu tiên sơ đồ fixture đã xác minh, tiếp theo defaultFormation của CLB và nhãn **“Bố trí theo sơ đồ thường dùng”**. Thiếu cả hai hiển thị **“Chưa có sơ đồ”**. Mỗi đội có sơ đồ riêng; parser hỗ trợ các chuỗi 2–5 tuyến với tổng 10 cầu thủ ngoài thủ môn, độc lập các sơ đồ Fantasy.
- 11 người trên sân phải thuộc vai trò STARTER có bằng chứng. Bỏ hoàn toàn cách xếp 11 người theo số phút/rating cho mùa 2026. Không có vai trò đủ tin cậy thì chỉ hiển thị danh sách chưa xác nhận, không tạo đội hình giả.
- Tọa độ đã xác minh trong trận được giữ trước. Các ô còn thiếu bố trí minh họa theo vị trí mùa/vị trí trận đã biết, gắn nhãn minh họa; không ghi các ô này thành vị trí thi đấu thực tế. Không dùng eligiblePositions để xác nhận đá chính hoặc ghi đè vị trí Fantasy.
- Tách danh sách đá chính, đã vào thay và dự bị không vào sân. Rating/thẻ/phút đi cùng player_id; dấu thay vào dựa trên SUB_USED, thời điểm vào/ra chỉ hiện khi đã có bằng chứng. Không suy thời điểm thay người từ số phút.

## SQL và importer

Ba bảng mới trong `schema.sql` và bản DDL riêng [`schema-mysql.sql`](../backend/data/match-lineups-2026-10-04/schema-mysql.sql):

| Bảng | Khóa | Dữ liệu |
| --- | --- | --- |
| club_season_formations | league_id + season_year + club_id | default_formation, updated_on, scope_from_gw/to_gw, verified_matches, formation_counts, fixture_ids |
| fixture_lineups | fixture_id + club_id | sơ đồ và nguồn/ngày xác minh sơ đồ; nguồn/ngày xác minh vai trò |
| fixture_lineup_players | fixture_id + club_id + player_id | STARTER/SUB_USED/SUB_UNUSED, match_position, row_index/slot_index, phút thay vào/ra đã xác minh |

`row_index=0` là tuyến thủ môn; những tuyến sau theo thứ tự phòng ngự → tấn công. `slot_index=0` là ô đầu tuyến theo thứ tự của sơ đồ nguồn. Các cặp row/slot phải nằm trong formation của fixture, không được trùng; chỉ STARTER mới có ô. Các ô trống giữ SQL NULL.

Các CSV dùng UTF-8, cột không chứa dấu phẩy/quote; bỏ trống ô để ghi NULL. Header nằm trong `LineupBatchImporter`. Thứ tự trong `formation_counts` là thứ tự tên sơ đồ tăng dần, phân cách `;`; ví dụ `3-4-2-1=2;4-3-3=1`. `fixture_ids` sắp theo ngày trận, rồi ID.

Lệnh dưới chỉ dành cho **H2 local đã có bản sao fixtures/players/statistics**, không trỏ datasource vào MySQL:

```powershell
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar '--spring.datasource.url=jdbc:h2:file:./target/formation-local;MODE=MySQL;DATABASE_TO_LOWER=TRUE' '--spring.datasource.username=sa' '--spring.datasource.password=' '--premierhub.match-lineups.enabled=true' '--premierhub.match-lineups.directory=data/match-lineups-2026-10-04'
```

Importer không nhập bảng cầu thủ, fixtures, membership hoặc thống kê. H2 rỗng sẽ bị từ chối vì không có các khóa nguồn. Bản kiểm tra lượt này nằm trong `backend/target/formation-check/`, bị Git ignore.

## Kiểm tra

- Backend: 21 test trong 5 lớp liên quan đều đạt; Maven package thành công. Các lớp: `LineupBatchImporterTest` (4), `ManualMatchDetailTest`, `FixtureEvidenceServiceTest`, `MatchKickoffQueryTest`, `FantasyLineupServiceTest` (17 test còn lại). Lượt đầu phát hiện lỗi CHECK của H2 và quyền xóa thư mục tạm JUnit trên Windows; đã sửa CHECK và đặt thư mục tạm trong `target/`. Chạy lại phần bị lỗi để xác nhận, không tạo test theo từng CLB.
- Frontend: một lượt `node --test src/utils/matchLineup.test.js src/utils/matchView.test.js src/api/matches.test.js src/fantasy/lineup.test.js` — 15 test đạt. Một lượt `npm.cmd run build` — thành công.
- H2 local được dựng từ snapshot đọc trước đó: 20 CLB, 50 fixture và 2.000 dòng thống kê. Import batch: 20 dòng sơ đồ CLB chưa có default; 78 lượt CLB có vai trò; 1.560 vai trò cầu thủ. Import lại: `clubChanges=0`, `fixtureChanges=0`, `playerChanges=0`. So sánh toàn bộ thống kê trước/sau không có thay đổi. API trả 78 XI VERIFIED và 22 lượt CLB thiếu vai trò; không sinh sơ đồ chưa xác minh.
- Chrome desktop 1440px và mobile 390px: thử cặp `4-2-3-1` / `3-4-2-1`, thêm cặp `4-5-1` / `3-1-3-2-1` để kiểm tra nhiều tuyến. Đủ 22 thẻ đá chính, ID không trùng, đúng hai nửa sân; đo hình chữ nhật không chồng thẻ, không tràn ngang hoặc ra ngoài sân. Kiểm tra nhãn sơ đồ fixture/default, nhãn minh họa, rating/thẻ/thay người theo ID và link cầu thủ mùa 2026. Không có lỗi JavaScript.
- Kiểm tra browser trên payload từ H2 thật: trận có vai trò nhưng thiếu formation hiện hai danh sách 11 đá chính, không dựng sân giả; GW1 thiếu cả vai trò/sơ đồ hiện danh sách chưa xác nhận. Kết quả và ảnh kiểm tra nằm trong `backend/target/formation-check/`, không commit.

Fixture dùng hai sơ đồ khác nhau trong test/browser là dữ liệu kiểm thử, **không phải** observation được xác minh của CLB mùa 2026/27. Không đưa các sơ đồ thử vào CSV nguồn hoặc production.

## Còn lại trước nhập SQL/phát hành

1. Bổ sung ảnh SofaScore Lineups theo `missing-lineups.csv`; điền formation, tọa độ, các vai trò còn thiếu và nguồn/ngày xác minh. Không cần thu thập lại rating/thống kê.
2. Tái tổng hợp CSV 20 CLB từ các fixture đã kiểm tra; cập nhật default/tần suất/số trận/danh sách fixture theo đúng quy tắc. Hiện toàn bộ 20 CLB chưa có default xác minh.
3. Sau khi được giao bước nhập production: sao lưu, áp dụng riêng DDL mới, chạy importer đã kiểm tra, chạy lần hai xác nhận thay đổi bằng 0 và đọc lại. Lượt này **chưa thực hiện** DDL hay nhập dữ liệu MySQL production.
4. Phát hành backend trước frontend để có metadata mới. Nếu frontend gặp backend chưa cập nhật, nó báo thiếu vai trò/sơ đồ và không dựng XI từ thống kê.

Không commit, push hoặc deploy trong lượt này.

## File cần commit

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

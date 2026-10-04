# Quy trình thu thập Gameweek PrismaXI — mùa 2026/27

Áp dụng bắt buộc khi người dùng nhắn “tổng hợp GW…”, “thu thập Gameweek…” hoặc tương đương. Đây là quy trình chuẩn bị và nhập dữ liệu theo yêu cầu, không phải tác vụ tự động. Chỉ xử lý Premier League `league_id=39`, mùa 2026/27 (`season=2026` trong CSV/API, `season_year=2026` trong SQL). Không sửa mùa 2024/25.

## 1. Bắt đầu từ phần còn lại của vòng được giao

1. Đọc checkpoint và file đã có của đúng GW; kiểm tra Git và dữ liệu liên quan. Nếu vòng đã hoàn thành, chỉ báo trạng thái hoặc xử lý phần người dùng yêu cầu bổ sung. Không thu thập lại GW đã xong, không kiểm kê toàn database.
2. Xác nhận danh sách fixture của vòng bằng lịch hiện có/football-data.org. Dùng ID nội bộ `fixtures.id`, không nhầm với `football_data_fixtures.provider_id`; ánh xạ bằng `football_data_fixtures.fixture_id`. Giữ thời điểm UTC đầy đủ từ `utcDate`/`kickoff_utc`, không tự thêm giờ vào ngày không có giờ.
3. Xử lý cả hai đội của **mọi fixture FINISHED** trong GW. Ghi trận hoãn, chưa kết thúc hoặc chưa xác nhận vào danh sách chờ trong `sources.md` và `missing.csv`. Không để trận chờ ngăn phần độc lập của các trận đã xong.
4. Có thể chia nội bộ thành batch 5 trận nhưng phải tiếp tục hết danh sách FINISHED, không dừng ở batch đầu. Lấy đúng danh sách trận và cầu thủ thực tế, không ép 10 trận đã xong hoặc 40 người/trận.

Khi cần đọc database, chỉ truy vấn đúng GW/fixture/player liên quan. Ví dụ SQL chỉ đọc, thay `6` bằng GW được giao:

```sql
SELECT f.id, f.gameweek, f.home_club_id, f.away_club_id,
       f.match_date, f.status, f.home_goals, f.away_goals,
       d.provider_id, d.kickoff_utc
FROM fixtures f
LEFT JOIN football_data_fixtures d ON d.fixture_id=f.id
WHERE f.league_id=39 AND f.season_year=2026 AND f.gameweek=6
ORDER BY f.id;
```

Importer membership dùng `fixtures.match_date`, không dùng ngày hiện tại hoặc ngày Việt Nam của giao diện để tự sửa khoảng membership.

## 2. Nguồn và roster

| Dữ liệu | Nguồn được dùng |
| --- | --- |
| Lịch, thời điểm UTC, trạng thái, tỉ số, BXH | football-data.org; tận dụng dữ liệu/cache đã có |
| Phút, bàn thắng, kiến tạo, thẻ vàng, thẻ đỏ | StatMuse; giữ thông tin chưa xác minh là trống |
| Rating | SofaScore do người dùng gửi ảnh hoặc bảng; không tự tìm rating, không dùng nguồn khác hoặc OVR |
| Đá chính, dự bị, người vào thay, sơ đồ thực tế, vị trí trận | Bằng chứng đã có hoặc ảnh Lineups người dùng cung cấp |

Ghi URL, ảnh/bảng do người dùng gửi, ngày kiểm tra và phạm vi nguồn theo fixture/CLB trong `sources.md`. Không suy ra đá chính từ rating, phút, eligiblePositions hay sơ đồ thường dùng. Giữ sơ đồ mặc định do người dùng chốt; không biến nó thành sơ đồ trận đã xác minh.

Tạm thời không theo dõi chuyển đến/chuyển đi, không thu thập chuyển nhượng hoặc sửa membership vì chuyển nhượng. Dùng ID và roster hiện có. Nếu chưa có player_id, có nhiều ID có thể khớp, thiếu dòng mùa/CLB hoặc thiếu membership hợp lệ tại ngày trận, ghi tên, fixture, CLB và vấn đề để người dùng xác nhận. Không âm thầm bỏ cầu thủ, đặt ID giả hoặc đặt ngày membership. Người chưa có ID được giữ trong danh sách nguồn/thiếu; chưa thể tạo dòng thống kê nhập được cho họ.

Chỉ bổ sung hồ sơ, OVR và vị trí của người mới khi được yêu cầu. Giữ mọi ngoại lệ người dùng đã xác nhận, dẫn lại bằng chứng thay vì yêu cầu xác nhận lại.

## 3. Một thư mục và một CSV thống kê cuối cho mỗi GW

Quy ước cho vòng mới, ví dụ `backend/data/gw6-2026/`:

```text
gw6-2026/
├── manual-match-stats-2026-GW6.csv
├── sources.md
├── missing.csv
└── lineups/                 # Chỉ tạo khi có dữ liệu cần nhập bằng LineupBatchImporter
    ├── clubs.csv
    ├── formations.csv
    └── players.csv
```

Một CSV thống kê chung cho toàn vòng sau khi chốt; trong khi làm tiếp cập nhật cùng file, không nhân bản REVIEW/RATINGS/FINAL. Các vòng cũ đã chia batch được tái sử dụng nguyên trạng theo checkpoint; không thu thập lại hoặc dọn/move hàng loạt. Chỉ tạo file phụ khi importer thực sự cần, ví dụ patch cho một fixture đã nhập hoặc ảnh nguồn cần giữ.

### CSV thống kê: đúng header 10 cột hiện có

Header lấy từ `ManualMatchStatsCsvReader.HEADER`:

```csv
season,fixture_id,player_id,status,rating,minutes,goals,assists,yellow_cards,red_cards
```

- UTF-8; `season=2026`; khóa duy nhất `(fixture_id,player_id)`. Không thêm tên, CLB, vai trò, formation hoặc fantasy_points vào file này. Luôn giữ cột rating kể cả khi đang chờ ảnh.
- Reader không hỗ trợ trường có dấu nháy kép hoặc tab. Ô trống là chưa biết/NULL; không viết chữ `NULL`, không dùng dấu phẩy làm dấu thập phân. Rating nằm trong 0..10 với tối đa hai chữ số thập phân; phút 0..180, bàn/kiến tạo 0..20, thẻ vàng 0..2, thẻ đỏ 0..1 theo reader.
- Reader cho phép trường PLAYED còn trống để chuẩn bị file; đọc thành công **không có nghĩa** dữ liệu đã đầy đủ hoặc đã kiểm tra membership.

### `missing.csv` và checkpoint

Dùng header đã có trong các batch cũ, như `backend/data/gw2-first-two/missing.csv`:

```csv
fixture_id,club,player_id,name,field
```

Mỗi trường thiếu một dòng; ID/tên để trống khi vấn đề thuộc cấp fixture hoặc chưa biết ID. Cột `field` ghi trường đang thiếu như rating/minutes hoặc vấn đề ID/membership/xác nhận đội hình; đây là danh sách công việc, không phải CSV nhập SQL. Diễn giải vấn đề, trận chờ, ngoại lệ và thông tin cần người dùng bổ sung trong `sources.md`. Không thêm cột vào CSV thống kê để chứa các ghi chú này.

Giữ một checkpoint ngắn ngay trong `sources.md`, cập nhật sau mỗi batch hoặc khi nhận ảnh mới:

- GW/mùa; danh sách fixture đã xử lý cả hai đội, còn chờ và lý do.
- Chỉ số/rating/đội hình đã hoàn thành; ảnh hoặc trường còn thiếu, ngoại lệ đã xác nhận.
- File thống kê dùng để nhập, lần kiểm tra reader/membership gần nhất; phần thay đổi kể từ checkpoint.
- Trạng thái riêng: chuẩn bị file, kiểm tra local, đã nhập local, đã nhập production, API đã đối chiếu, tổng mùa cập nhật đến GW nào. Chưa làm bước nào thì ghi “chưa”.
- Bước tiếp theo cụ thể. Phiên sau tiếp tục bước đó, không bắt đầu lại từ đầu.

### Dữ liệu đội hình: giữ định dạng importer hiện có

`LineupBatchImporter.importDirectory` đọc đúng ba file trong `lineups/` với các header sau:

```csv
club_id,season_year,default_formation,updated_on,scope_from_gw,scope_to_gw,verified_matches,formation_counts,fixture_ids,default_source,source_note
```

```csv
fixture_id,club_id,season_year,formation,verified_on,source_url
```

```csv
fixture_id,club_id,season_year,player_id,role,match_position,row_index,slot_index,substitution_in_minute,substitution_out_minute,verified_on,source_note
```

- `clubs.csv` bắt buộc có CLB liên quan; giữ default nguồn `USER` đã chốt. Các số đếm/phạm vi phải khớp observations trong `formations.csv`; không lấy mẫu một GW ghi đè lịch sử mẫu đã lưu. Nếu cần mẫu tích lũy, kế thừa observations đã có, không thu thập lại; đối chiếu trước khi nhập.
- `formations.csv` chỉ chứa sơ đồ trận đã xác minh. Importer hiện yêu cầu `source_url` là URL `https://www.sofascore.com/...`, `https://api.sofascore.com/...` hoặc `https://sofascore.com/...`. Nếu ảnh chưa đủ nguồn URL để nhập, ghi thiếu và tiếp tục phần độc lập; không tạo URL giả hoặc ghi sơ đồ CLB thành sơ đồ trận. File chưa có observations có thể chỉ có header.
- Vai trò hợp lệ: `STARTER`, `SUB_USED`, `SUB_UNUSED`; đúng 11 STARTER đã xác nhận mỗi đội. Danh sách ID của cả đội phải khớp thống kê đã lưu: STARTER/SUB_USED tương ứng PLAYED, SUB_UNUSED tương ứng DID_NOT_PLAY. Không yêu cầu 20 người/CLB ở importer đội hình.
- `match_position`/tọa độ chỉ ghi khi có bằng chứng. `row_index` và `slot_index` phải cùng trống hoặc cùng có giá trị; tọa độ cần formation trận đã xác minh, STARTER và match_position. Phút thay người có thể trống nếu chưa biết; không suy ra từ tổng phút. Không sửa vị trí mùa/Fantasy.
- Cùng một đội/trận phải dùng ngày và ghi chú nguồn thống nhất. Không tự ghi đè metadata khác nội dung: importer đội hình có upsert, vì vậy phải so sánh trước và báo xung đột từng cầu thủ/trường, không dựa vào importer để tự từ chối mọi thay đổi.

`lineups.csv` 10 cột trong các batch cũ là bằng chứng thu thập, **không phải** đầu vào trực tiếp của LineupBatchCommand. Có thể tái sử dụng để chuẩn bị ba file đúng header trên; thiếu vai trò thì báo thiếu, không suy đoán.

## 4. NULL, số 0 và điều kiện hoàn thành

- Không biến ô chưa biết thành 0. Chỉ ghi 0 khi nguồn/bằng chứng xác nhận không có sự kiện đó.
- PLAYED cần đủ minutes, goals, assists, yellow_cards, red_cards và rating, hoặc ngoại lệ rating đã được người dùng xác nhận. Phút PLAYED phải dương hoặc còn trống trong bản chưa đầy đủ.
- PLAYED không được SofaScore chấm: giữ rating trống và `fantasy_points` SQL NULL, ghi xác nhận trong sources.md. Phân biệt với “chưa gửi/chưa đọc được rating”; trường hợp sau vẫn nằm trong missing.csv. Không gán rating/fantasy_points bằng 0 cho người không được chấm.
- DID_NOT_PLAY cần bằng chứng: rating trống, minutes=0; importer tính fantasy_points=0. Các sự kiện chỉ 0 khi có căn cứ; reader cho phép sự kiện trống và từ chối sự kiện dương đối với DID_NOT_PLAY.
- Nếu nguồn không truy xuất được, báo một danh sách thiếu gộp theo trận/CLB/cầu thủ để người dùng cung cấp. Tiếp tục phần không phụ thuộc dữ liệu thiếu.
- Chỉ gọi **toàn GW đầy đủ** khi danh sách trận đã xác nhận, không còn trận chờ và cả hai đội của mọi trận đã có dữ liệu/bằng chứng bắt buộc hoặc ngoại lệ được xác nhận. Nếu chỉ các trận FINISHED hiện có đã đủ, báo rõ số trận xử lý và số còn chờ. Tách độ đầy đủ của thống kê, đội hình và tổng mùa; không dùng “đủ 400 dòng” thay cho kiểm tra thực tế.

## 5. Kiểm tra đầu vào, không nhập production khi chỉ “tổng hợp”

Reader/importer chung hiện có:

| Thành phần | Kiểm tra/hành vi thực tế |
| --- | --- |
| `ManualMatchStatsCsvReader.read(Path)` | Header, 10 cột, khóa trùng, kiểu/range, status, season; không mở database |
| `ManualMatchStatsImporter.importFile(Path)` | Fixture FINISHED mùa 2026, membership duy nhất trong CLB trận, dòng player_season_stats đúng CLB, khóa không bị fixture_player_stats của provider sở hữu; transaction và không ghi đè thống kê khác nội dung |
| `ManualMatchStatsPatchImporter.fillMissing(Path,int)` | Chỉ bổ sung NULL của dòng đã tồn tại trong một fixture, kiểm tra membership/status, từ chối giá trị xung đột; ô trống không đổi dữ liệu |
| `LineupBatchImporter.importDirectory(Path)` | Kiểm tra schema CSV đội hình, vai trò, 11 STARTER, ID/status khớp thống kê, tọa độ và phạm vi formation; sau đó mới upsert |

Repo **không có cờ dry-run/validate-only** cho các command nhập này. Không gọi command trên production để thử validation; khởi động Spring cũng có thể chạy SQL init theo cấu hình hiện tại. Kiểm tra membership/khóa/xung đột bằng đọc hẹp trên dữ liệu đã có. Muốn chạy đầy đủ validator của importer, dùng bản database local tách biệt đã có đủ fixture, roster, membership và thống kê liên quan; báo rõ có nhập local hay chưa. Không tự chạy roster importer để vượt lỗi membership. Nếu chưa kiểm tra được một điều kiện, ghi rõ trong checkpoint.

Ví dụ gọi reader chung từ `backend/` bằng JShell của JDK đang có, không khởi động Spring/database. Dùng lớp đã biên dịch khớp mã hiện tại; nếu chưa có lớp, chỉ biên dịch riêng reader dưới đây, không chạy Maven full test/build:

```powershell
javac -encoding UTF-8 --release 21 -d target/gameweek-reader src/main/java/com/premierhub/manualstats/ManualMatchStatsCsvReader.java
jshell --class-path target/gameweek-reader
```

Trong JShell, thay đường dẫn GW cho đúng:

```java
import java.nio.file.Path;
import com.premierhub.manualstats.ManualMatchStatsCsvReader;
var rows = new ManualMatchStatsCsvReader().read(Path.of("data/gw6-2026/manual-match-stats-2026-GW6.csv"));
System.out.println("rows=" + rows.size());
```

Thoát bằng `/exit`. Lỗi reader là lỗi đầu vào cần sửa; không báo đạt khi còn exception. Đây chỉ là kiểm tra định dạng/giá trị, không phải kiểm tra membership hoặc tuyên bố đủ dữ liệu. Không viết test Java riêng cho từng GW.

Membership theo importer: `manual_player_memberships.start_date <= fixtures.match_date` và `end_date IS NULL OR end_date > fixtures.match_date`, thuộc một trong hai CLB trận, đồng thời có khóa mùa/CLB tương ứng trong `player_season_stats` khi nhập dòng mới. Khoảng là `[start_date,end_date)`. Giữ nguyên dữ liệu ngày; thiếu hoặc nhiều membership hợp lệ phải báo.

Khi nhận rating mới: ghép đúng `(fixture_id,player_id)` với ảnh/bảng, kiểm tra tên/CLB bằng ID hiện có; chỉ cập nhật rating được cung cấp, giữ các chỉ số cũ. Chạy reader chung và đối chiếu **phần thay đổi**, không thu thập lại phút/bàn/thẻ. Không dùng patch để thay giá trị khác đã lưu.

## 6. Chỉ khi được yêu cầu “nhập SQL”

Yêu cầu nhập SQL là quyền thực hiện bước nhập; không hỏi lại quyền đã có nếu đích/file đã rõ. Trước khi ghi:

1. Xác nhận đích MySQL production bằng cấu hình kết nối và đọc danh tính database/server; không mặc định H2 local là production, không in mật khẩu/URL chứa bí mật. Cấu hình prod hiện dùng `PREMIERHUB_JDBC_URL`, `PREMIERHUB_DB_USER`, `PREMIERHUB_DB_PASSWORD`; các import command không tự nạp ba biến này từ `.env.local`.
2. Tạo SQL backup mới ngoài Git trước lần ghi, lưu đích/thời gian/phạm vi, đường dẫn, dung lượng và SHA-256 trong checkpoint. `backend/local-backups/` đã được ignore. Repo không có command backup MySQL 2026 chuyên dụng; dùng công cụ xuất SQL của môi trường đã xác minh, kiểm tra backup đọc được và có bảng sẽ bị tác động. Không dùng `premierhub.snapshot.mode`: đó là snapshot GW1 mùa 2024/25. Chưa tạo được backup thì chưa ghi.
3. So sánh đầu vào với dữ liệu đích theo fixture/player_id và từng trường. Lỗi/xung đột báo fixture, player_id, tên, trường, giá trị đã lưu và giá trị dự kiến; không tự đổi hoặc xóa dữ liệu cũ. Kiểm tra schema cần thiết trước khi nhập đội hình; không âm thầm chạy migration.
4. Chạy importer hiện có, đọc kết quả, chạy lại đúng đầu vào để xác nhận không thêm/cập nhật; đối chiếu MySQL và API từng trận. Các lệnh dưới đây chạy từ `backend/`, dùng JAR hiện có khớp code được duyệt, **không phải lệnh cần chạy khi chỉ tổng hợp**. Không bật các runner sync/roster khác hoặc thay cấu hình deploy.

Nhập CSV thống kê chung của GW, ví dụ GW6:

```powershell
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=prod --spring.sql.init.mode=never --premierhub.manual-match-stats.enabled=true --premierhub.manual-match-stats.file=data/gw6-2026/manual-match-stats-2026-GW6.csv
```

Chạy lại phải báo `MANUAL_MATCH_STATS ... inserted=0`; command thường không có bộ đếm updated vì không cập nhật dòng cũ. Cấu hình `spring.sql.init.mode=never` dùng property Spring đã có, tránh chạy `schema.sql`/SQL init khi chỉ nhập dữ liệu vào schema đã xác minh.

Nếu bổ sung rating/chỉ số vào NULL **đã nhập**, command patch chỉ nhận một fixture mỗi lần. Chỉ lúc này mới tạo CSV 10 cột nhỏ cho fixture đó trong cùng thư mục GW; ô trống nghĩa là không đổi. Trỏ file patch đã kiểm tra vào option `premierhub.manual-match-stats.file`, thêm option thật `premierhub.manual-match-stats.fill-missing-fixture`. Ví dụ PowerShell dùng biến đích đã chốt:

```powershell
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=prod --spring.sql.init.mode=never --premierhub.manual-match-stats.enabled=true "--premierhub.manual-match-stats.file=$patchFile" "--premierhub.manual-match-stats.fill-missing-fixture=$fixtureId"
```

`$patchFile` là đường dẫn file một fixture, `$fixtureId` là ID nội bộ thật đã xác nhận, phải gán trước khi chạy; không dùng ID ví dụ để ghi SQL. Lần lặp phải báo `updated=0 filled_cells=0`. Patch không thêm người mới và không được dùng cả CSV nhiều fixture. CSV chung cuối vòng vẫn là bản dữ liệu đầy đủ dùng để đối chiếu; không nhập lại bằng importer thường nếu dữ liệu đích đang thiếu trường mà CSV đã bổ sung.

Nhập đội hình sau thống kê, chỉ khi có bộ ba CSV hợp lệ, đủ nguồn và đã kiểm tra không ghi đè metadata khác nội dung:

```powershell
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=prod --spring.sql.init.mode=never --premierhub.match-lineups.enabled=true --premierhub.match-lineups.directory=data/gw6-2026/lineups
```

Lặp phải có `clubChanges=0`, `fixtureChanges=0`, `playerChanges=0`; số observations/players trong Result là số đầu vào, không phải số thay đổi.

### Tổng mùa: giới hạn hiện có phải báo riêng

`ManualMatchStatsImporter`/patch gọi `ManualSeasonStatsService.rebuildLatestComplete()` khi có thay đổi. Service hiện vẫn xác định vòng hoàn chỉnh bằng 10 fixture FINISHED/400 dòng, và `rebuildThroughGameweek` kiểm tra 40 dòng/trận, 20 người/CLB cho **mọi vòng từ GW1**. Đây là giới hạn code hiện hữu, không phải quy tắc thu thập mới. Số dòng thực tế khác 40 hoặc còn trận hoãn có thể làm tổng mùa chỉ dừng ở GW trước; ngay cả tổng 400 cũng chưa đủ nếu số người từng đội không khớp giới hạn cũ.

Không bịa DID_NOT_PLAY/khóa người để qua kiểm tra, không tự sửa service khi chỉ làm dữ liệu. Vẫn xử lý các trận FINISHED độc lập; báo rõ thống kê trận đã nhập và tổng mùa chưa theo kịp. Nếu bị giới hạn này chặn, nêu nhu cầu sửa code trong một lượt được giao riêng; không chạy SQL tổng mùa thủ công để lách validator hoặc tuyên bố GW hoàn tất.

Khi dữ liệu thực tế đáp ứng điều kiện service và đã được giao nhập SQL, chạy tái tính hiện có rồi lặp xác nhận `updated=0`:

```powershell
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=prod --spring.sql.init.mode=never --premierhub.manual-season-stats.enabled=true --premierhub.manual-season-stats.through-gameweek=6
```

Đọc lại `player_season_stats.appearances`, `minutes`, `goals`, `assists`; chỉ PLAYED cộng vào tổng. Trường PLAYED còn thiếu giữ tổng tương ứng NULL. Xác nhận phạm vi GW thực sự đã cập nhật, không chỉ suy từ việc importer không báo lỗi. Xem thêm [player-season-stats-2026.md](player-season-stats-2026.md); yêu cầu 40/400 ở tài liệu cũ mô tả giới hạn service, không áp đặt số người trong quy trình này.

### Đối chiếu sau nhập

- `GET /api/matches?season=2026&matchweek=6`: đúng danh sách fixture/trạng thái/tỉ số của vòng, không chỉ đếm trận đã nhập.
- Với **từng** fixture FINISHED đã nhập: `GET /api/matches/{fixture_id}/details?season=2026`, đối chiếu đúng hai CLB, tập playerId, participationStatus, minutes/goals/assists/yellowCards/redCards/rating/fantasyPoints với CSV và membership ngày trận. Không ép 20 người/đội khi nguồn thực tế khác. PLAYED không được chấm phải có cả rating/fantasyPoints NULL; DID_NOT_PLAY có fantasyPoints=0.
- Nếu nhập đội hình: đối chiếu homeLineup/awayLineup, role, formation/source và ID; thiếu dữ liệu phải hiện thiếu, không suy từ sơ đồ giao diện.
- Không báo API đã phản ánh nếu chưa kiểm tra hoặc backend đang chạy chưa có chức năng tương ứng. Dữ liệu đơn thuần không cần deploy; nếu cần sửa mã thì xử lý phạm vi đó khi được giao, chạy test/build liên quan một lần.

## 7. Báo cáo cuối mỗi lượt

1. Số fixture FINISHED đã hoàn thành cả hai đội, còn thiếu/chờ; danh sách trận hoãn/chưa kết thúc và trạng thái xác nhận danh sách vòng.
2. Số PLAYED, DID_NOT_PLAY, PLAYED có rating, PLAYED không được chấm đã xác nhận và PLAYED còn chờ rating. Không gộp NULL của DID_NOT_PLAY vào “không được chấm”. Báo độ đầy đủ thống kê và đội hình riêng.
3. Một danh sách thông tin còn thiếu gộp theo fixture/CLB/cầu thủ để người dùng bổ sung.
4. CSV thống kê cuối dùng để nhập, nguồn/missing/checkpoint; trạng thái kiểm tra, nhập local/production, idempotence, đối chiếu API và tổng mùa thực tế. Nếu chưa nhập thì ghi rõ.
5. File cần commit và commit message phù hợp phần thực sự đã thay đổi. Không tự commit, push, deploy, tạo cron hoặc Pre-deploy Command.

Chỉ dữ liệu/tài liệu: không viết test Java cho từng GW, không chạy full test/build. Nếu mã/schema được yêu cầu sửa, chạy kiểm tra/build đúng phạm vi một lần; mọi hạn chế chưa xử lý phải được báo thay vì lách bằng dữ liệu giả.

## Căn cứ trong repo và câu lệnh dùng cho vòng tiếp theo

Tài liệu được đối chiếu với [reader](../backend/src/main/java/com/premierhub/manualstats/ManualMatchStatsCsvReader.java), [importer thống kê](../backend/src/main/java/com/premierhub/manualstats/ManualMatchStatsImporter.java), [patch importer](../backend/src/main/java/com/premierhub/manualstats/ManualMatchStatsPatchImporter.java), [stats command](../backend/src/main/java/com/premierhub/manualstats/ManualMatchStatsCommand.java), [service tổng mùa](../backend/src/main/java/com/premierhub/manualstats/ManualSeasonStatsService.java), [season command](../backend/src/main/java/com/premierhub/manualstats/ManualSeasonStatsCommand.java), [lineup importer](../backend/src/main/java/com/premierhub/lineups/LineupBatchImporter.java), [lineup command](../backend/src/main/java/com/premierhub/lineups/LineupBatchCommand.java), [schema](../backend/src/main/resources/schema.sql), [cấu hình chung](../backend/src/main/resources/application.properties), [cấu hình prod](../backend/src/main/resources/application-prod.properties) và [MatchController](../backend/src/main/java/com/premierhub/web/MatchController.java).

football-data.org được xử lý bằng [FootballDataClient](../backend/src/main/java/com/premierhub/sync/FootballDataClient.java), [FootballDataBatch](../backend/src/main/java/com/premierhub/sync/FootballDataBatch.java), [FootballDataCommand](../backend/src/main/java/com/premierhub/sync/FootballDataCommand.java) và [FootballDataSync](../backend/src/main/java/com/premierhub/sync/FootballDataSync.java). Command sync hiện ghi database và batch parser đòi 20 đội/380 fixture/20 dòng BXH của cả mùa; không đưa JSON chỉ một GW vào importer này, không tự bật sync production khi chỉ tổng hợp. Tận dụng lịch/UTC/status đã có; thiếu cập nhật thì chuẩn bị phần còn thiếu và báo, không tự sửa provider mapping hoặc chuyển nhượng.

Câu lệnh nhắn cho vòng tiếp theo:

> Tổng hợp GW6 mùa 2026/27 theo docs/gameweek-data-workflow.md, tiếp tục checkpoint và chỉ chuẩn bị file; chưa nhập SQL.

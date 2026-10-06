# Chấm điểm và công bố Fantasy 2026/27 — bước 4

Trạng thái 05/10/2026: mã chấm điểm, công bố và BXH GW/mùa đã phát hành; bốn migration Fantasy đã nhập và GW6 đã OPEN. Người dùng đã kiểm chứng lưu → sửa → lưu lại → reload. Chưa nhập thống kê/xác nhận nguồn hoặc công bố kết quả GW6. Lượt chốt quy trình này chỉ sửa tài liệu và đọc API BXH, không chạy lại migration/luồng lưu/test/build. Xem [checkpoint production](fantasy-production-2026-10-05.md).

## Luồng cuối của người chơi và GW6

Một nút **Lưu đội hình** cập nhật trực tiếp đội tham gia qua `/submit`. Trước deadline có thể chỉnh và lưu lại; chỉnh chưa lưu không thay đội dự thi, lưu lỗi giữ đội trước. Không yêu cầu lưu nháp rồi chốt riêng. Từ đúng deadline, giữ đội đã lưu trên sân, khóa chỉnh sửa và báo chờ kết quả. Sau công bố, hiển thị điểm từng người, tổng, phiên bản và BXH Gameweek/Cả mùa; người chưa lưu đội hợp lệ không tự có đội/kết quả.

GW6 đã công bố deadline **09/10/2026 00:00 Asia/Ho_Chi_Minh = 08/10/2026 17:00 UTC**, rosterAsOf **05/10/2026**. Không đổi deadline khi lịch sync thay đổi. Hệ thống dùng Clock server, không cần cron khóa đội. Thông báo điểm nằm trong website; chưa có email/push. Chi tiết polling/Top 200 và API BXH ở [luồng người chơi](fantasy-player-flow-2026.md).

## Vận hành GW6 từ thu thập đến kết quả

Đây là hướng dẫn cho các lượt được giao sau, **không phải yêu cầu thực thi nhập/công bố trong lượt cập nhật tài liệu**. “Tổng hợp GW6” chỉ chuẩn bị và kiểm tra file; quyền nhập SQL không đồng nghĩa quyền công bố Fantasy.

1. Tiếp tục checkpoint GW6 theo [quy trình Gameweek](gameweek-data-workflow.md), lấy đủ dữ liệu **cả hai đội của từng fixture FINISHED**. Trận hoãn/chưa xong ghi chờ; tiếp tục phần độc lập. Lịch/UTC/trạng thái/tỉ số từ football-data.org, phút/bàn/kiến tạo/thẻ từ StatMuse; rating SofaScore chỉ từ ảnh/bảng người dùng cung cấp. Không thu thập lại dữ liệu đã hoàn thành.
2. Dùng một CSV thống kê cuối 10 cột, `sources.md`, `missing.csv`, bộ CSV đội hình/sơ đồ hiện có và `confirmed-unrated.csv` nếu có ngoại lệ không được chấm. Không thêm cột vào CSV thống kê, suy ra đá chính từ phút/rating hoặc ép 40 người/trận. Xác nhận không chấm phải được nhập database; ghi chú đơn thuần chưa đủ cho scoring. Rating chưa thu thập vẫn là blocker, không tính 0.
3. Khi được giao nhập đúng đích: kiểm tra input/membership/xung đột, backup SQL mới ngoài Git và kiểm tra hoàn tất, nhập thống kê → đội hình → bằng chứng Fantasy bằng các command dưới đây; lặp xác nhận không thay đổi, đối chiếu API từng fixture. Giữ dữ liệu thô NULL của người không được chấm. **Các command này không công bố kết quả, không chuyển GW sang PUBLISHED.**
4. Sau deadline, ADMIN kiểm tra readiness toàn GW. Mọi trận phải FINISHED và có bằng chứng đầy đủ; từng người của đội tham gia phải có rating hoặc căn cứ 0. Thiếu dòng thống kê không mặc nhiên DNP. Trận hoãn giữ cả vòng chờ; nhập xong vài trận không đủ công bố cuối.
5. Khi ready=true, ADMIN chủ động công bố bằng UI hoặc API + CSRF. Backend tự tính tổng/breakdown, ghi cả GW trong transaction và chỉ đặt PUBLISHED sau khi thành công. Kiểm tra kết quả tài khoản thật và cả hai BXH. Không công bố điểm tạm hoặc đội thử.
6. Nếu rating được sửa sau công bố, cập nhật nguồn có kiểm soát, xác nhận lại và kiểm tra readiness rồi **tái tính cả GW với lý do/phiên bản**. BXH lấy phiên bản mới nhất, không cộng trùng bản cũ.

### Command nhập thật — chỉ chạy khi được giao nhập SQL

Chạy từ `backend/` bằng JAR đã được duyệt khớp mã phát hành. Datasource production lấy từ môi trường: `PREMIERHUB_JDBC_URL`, `PREMIERHUB_DB_USER`, `PREMIERHUB_DB_PASSWORD`; command không tự nạp `.env.local`. Private hostname Railway chỉ dùng trong mạng Railway; nếu vận hành từ máy ngoài, cấu hình kết nối public proxy đã xác minh với TLS/UTC qua môi trường, không đưa password lên command line/log. Không đổi start command của service, bật runner thường trực, tạo cron hoặc Pre-deploy Command.

Các đường dẫn dưới đây là bộ file **sẽ chuẩn bị** khi thu thập GW6, không phải dữ liệu đã có/đã nhập. `$releaseJar` trỏ artifact đúng phiên bản; nếu tên artifact khác thì thay bằng tên thực tế, không build lại chỉ để nhập dữ liệu.

```powershell
$releaseJar = 'target/premierhub-backend-0.1.0-SNAPSHOT.jar'
$statsFile = 'data/gw6-2026/manual-match-stats-2026-GW6.csv'
$sourceFile = 'data/gw6-2026/sources.md'
$unratedFile = 'data/gw6-2026/confirmed-unrated.csv'
$lineupsDirectory = 'data/gw6-2026/lineups'

java -jar $releaseJar --spring.profiles.active=prod --spring.sql.init.mode=never --premierhub.manual-match-stats.enabled=true "--premierhub.manual-match-stats.file=$statsFile"
java -jar $releaseJar --spring.profiles.active=prod --spring.sql.init.mode=never --premierhub.match-lineups.enabled=true "--premierhub.match-lineups.directory=$lineupsDirectory"
java -jar $releaseJar --spring.profiles.active=prod --spring.sql.init.mode=never --premierhub.fantasy-evidence.enabled=true "--premierhub.fantasy-evidence.stats-file=$statsFile" "--premierhub.fantasy-evidence.unrated-file=$unratedFile" "--premierhub.fantasy-evidence.source-file=$sourceFile"
```

Không có người không được chấm: bỏ đối số `unrated-file` nếu chưa từng có xác nhận của các fixture đó, hoặc dùng file chỉ header làm danh sách cuối rỗng. Khi cần bỏ xác nhận cũ đã hết hiệu lực, phải truyền sidecar cuối hiện hành (có thể chỉ header) cùng CSV đầy đủ. Importer không coi file rating đang chờ là xác nhận không chấm. Bộ đội hình đọc `clubs.csv`, `formations.csv`, `players.csv` theo header ở quy trình Gameweek; kiểm tra không ghi đè metadata CLB/sơ đồ khác nội dung trước nhập.

Lặp cùng input: thống kê `inserted=0`; đội hình `clubChanges=0 fixtureChanges=0 playerChanges=0`; bằng chứng `fixture_changes=0 unrated_changes=0`. Các command chạy một lần ở chế độ không mở HTTP rồi đóng context. Tổng mùa bóng đá là bước riêng theo giới hạn service đã ghi trong quy trình Gameweek; scoring không dùng `player_season_stats` để cộng điểm, không ép thêm dòng giả để qua giới hạn tổng mùa.

### ADMIN production: công cụ và tài khoản đã được cấp quyền

Code đã có khu vực **Quản trị kết quả** trong tab **Đội hình của bạn**, bên dưới phần kết quả, chỉ hiện khi `account.role === 'ADMIN'`. Chọn GW6 → **Kiểm tra readiness** → xử lý danh sách blocker → nhập lý do → **Công bố kết quả**; có phiên bản rồi thì nút thành **Tái tính và công bố phiên bản mới**. UI tự lấy CSRF và gửi đúng currentVersion, hiển thị lịch sử/lý do/người công bố. Đăng nhập qua `https://premierhub.vercel.app`, không gọi auth/admin trực tiếp Railway để tránh mất phiên/proof proxy.

Chủ dự án đã cho phép và đã cấp **ADMIN riêng ID 2 — Cường Murdock** ngày 05/10/2026 sau backup/đối chiếu danh tính; ID 1 vẫn USER. [Checkpoint/audit cấp quyền](fantasy-production-2026-10-05.md#cấp-admin-có-xác-nhận-riêng--05102026) ghi chi tiết. Repo chưa có API/UI cấp quyền; đây là thao tác vận hành có kiểm soát bằng SQL, không phải quyền tự đăng ký ADMIN. Schema hỗ trợ một quyền duy nhất; đăng ký mới vẫn USER. Backend `/admin/**` vẫn yêu cầu ADMIN dù tự gọi URL, không dùng helper mở deadline để bỏ qua phân quyền công bố.

Sau cập nhật role, **đăng xuất/đăng nhập lại ID 2** để phiên Security nhận ROLE_ADMIN; reload đơn thuần chưa đủ. Đã kiểm chứng phiên thật ngày 05/10: `/api/auth/me` 200/id=2/role=ADMIN, GET readiness 200/ready=false, 10 fixture/1 người tham gia/currentVersion=0. Readiness báo chưa đến deadline, các trận chưa FINISHED và chưa có thống kê/đội hình/bằng chứng: đây là điều kiện dữ liệu đang chờ, không phải lỗi quyền. ID 1 USER đã kiểm chứng readiness 403 ACCESS_DENIED bằng phiên thật trước đó. Chưa công bố điểm.

### API có thể dùng trực tiếp

Các route đầy đủ cho GW6:

| Thao tác | Method/path |
| --- | --- |
| Readiness/lịch sử | GET `/api/fantasy/2026/admin/gameweeks/6/readiness` |
| Công bố lần đầu | POST `/api/fantasy/2026/admin/gameweeks/6/publish-results` |
| Tái tính toàn vòng | POST `/api/fantasy/2026/admin/gameweeks/6/recalculate-results` |
| Kết quả của mình | GET `/api/fantasy/2026/me/gameweeks/6/result`, header `X-PrismaXI-Account-ID` bằng ID của tài khoản đang đăng nhập |
| BXH GW6 | GET `/api/fantasy/2026/leaderboard?gameweek=6` |
| BXH cả mùa | GET `/api/fantasy/2026/leaderboard` |

Nếu không dùng nút UI, đoạn sau chạy trong DevTools **ngay tại website Vercel**, sau khi đã đăng nhập ADMIN. Chỉ khai báo helper và gọi GET readiness, chưa công bố; không in CSRF/cookie:

```javascript
async function fantasyAdminGw6(action, body) {
  if (location.origin !== 'https://premierhub.vercel.app') throw new Error('Mở đúng website production.');
  if (!['readiness', 'publish-results', 'recalculate-results'].includes(action)) throw new Error('Thao tác không hợp lệ.');
  const headers = { Accept: 'application/json' };
  if (body) {
    const csrfResponse = await fetch('/api/auth/csrf', { credentials: 'include' });
    if (!csrfResponse.ok) throw new Error('Không lấy được CSRF.');
    const csrf = await csrfResponse.json();
    headers[csrf.headerName] = csrf.token;
    headers['Content-Type'] = 'application/json';
  }
  const response = await fetch('/api/fantasy/2026/admin/gameweeks/6/' + action, {
    credentials: 'include', headers,
    ...(body ? { method: 'POST', body: JSON.stringify(body) } : {}),
  });
  const data = await response.json();
  if (!response.ok) throw new Error('HTTP ' + response.status + ' · ' + (data.message ?? 'Không thực hiện được.'));
  return data;
}
let readiness = await fantasyAdminGw6('readiness');
console.table(readiness.issues);
```

Chỉ khi được giao công bố và đã đọc readiness ready=true/currentVersion=0, **chủ động chạy riêng**:

```javascript
if (!readiness.ready || readiness.currentVersion !== 0) throw new Error('Chưa sẵn sàng công bố lần đầu.');
await fantasyAdminGw6('publish-results', {
  expectedVersion: readiness.currentVersion,
  reason: 'Đã xác nhận đầy đủ dữ liệu và rating SofaScore GW6 của cả hai đội mỗi trận.',
});
```

Backend kiểm tra lại readiness và version trong transaction; không nhận điểm từ client. 409 `RESULTS_NOT_READY`: đọc GET readiness lại để lấy fixtureId/playerId/accountId/code/message, hoàn thiện phần thiếu rồi thử khi đủ; không tắt CSRF/bỏ validator. 409 phiên bản: tải lại readiness/lịch sử, không đoán expectedVersion. Dữ liệu không đổi có thể trả `unchanged=true` và giữ phiên bản, không phải lỗi.

Kiểm tra BXH chỉ đọc từ PowerShell, không cần account/cookie:

```powershell
Invoke-RestMethod 'https://premierhub.vercel.app/api/fantasy/2026/leaderboard?gameweek=6'
Invoke-RestMethod 'https://premierhub.vercel.app/api/fantasy/2026/leaderboard'
```

Lần đọc production trong lượt tài liệu 05/10/2026: cả hai **HTTP 200**, `Cache-Control: no-store`, `status: "AWAITING_RESULTS"`, `version: null`, `publishedGameweeks: 0`, `players: []`; gameweek là 6 hoặc null cho mùa. Đây là trạng thái trống hợp lệ, không lỗi 500/điểm giả. Sau công bố phải thấy PUBLISHED, phiên bản mới nhất/điểm đúng; BXH mùa chỉ cộng GW đã công bố và bằng điểm đồng hạng. Kết quả riêng cần session chủ và header ID; dùng nút **Cập nhật kết quả** hoặc reload để đối chiếu 11 dòng/tổng/version.

### Rating sửa: công cụ hiện có và phần còn thiếu

**NULL → rating được người dùng bổ sung:** patch importer chỉ nhận một fixture và chỉ lấp NULL; ô trống không thay dữ liệu. Sau backup/đối chiếu và quyền nhập, chuẩn bị file patch 10 cột riêng cho đúng fixture, giữ CSV cuối chung đã cập nhật, rồi dùng command thật:

```powershell
# Gán $patchFile và $fixtureId bằng file/ID nội bộ thật đã được xác nhận trước khi chạy.
java -jar $releaseJar --spring.profiles.active=prod --spring.sql.init.mode=never --premierhub.manual-match-stats.enabled=true "--premierhub.manual-match-stats.file=$patchFile" "--premierhub.manual-match-stats.fill-missing-fixture=$fixtureId"
```

Lặp phải `updated=0 filled_cells=0`. Nếu NULL trước đó được xác nhận không chấm, bỏ người đó khỏi sidecar cuối, rồi chạy lại evidence command với CSV cuối/sidecar/nguồn. Importer loại xác nhận cũ trong phạm vi các fixture đầy đủ đã truyền và cập nhật hash.

**Rating đã có số → số khác:** importer thường từ chối xung đột; patch không ghi đè số đã có. Repo chưa có command sửa rating khác nội dung. Cần một lượt xử lý được chủ dự án duyệt rõ fixture/player_id, giá trị cũ/mới và nguồn, backup mới, cập nhật có kiểm soát; không ghi tên một cờ overwrite không tồn tại hoặc xóa dòng để nhập lại. Trong lượt quy trình này chưa sửa dữ liệu. Sau khi nguồn database thực sự đã sửa, cập nhật CSV cuối/nguồn, chạy evidence command xác nhận lại. Hash cũ không còn hợp lệ sẽ chặn tái tính cho tới khi xác nhận lại đầy đủ.

Sau cập nhật và evidence, ADMIN lấy readiness mới, chỉ tái tính khi ready=true và currentVersion>0, nhập lý do thật nêu phạm vi sửa:

```javascript
readiness = await fantasyAdminGw6('readiness');
if (!readiness.ready || readiness.currentVersion < 1) throw new Error('Chưa đủ điều kiện tái tính GW đã công bố.');
const correctionReason = prompt('Ghi lý do thực tế: fixture/player_id, rating cũ/mới và nguồn đã xác nhận');
if (!correctionReason || correctionReason.trim().length < 3) throw new Error('Cần lý do tái tính.');
await fantasyAdminGw6('recalculate-results', {
  expectedVersion: readiness.currentVersion,
  reason: correctionReason.trim(),
});
```

Đối chiếu version/11 dòng/tổng và BXH GW/mùa sau tái tính; bản cũ vẫn có lịch sử. Website đã nhận PUBLISHED dừng polling chờ điểm, nên người chơi dùng **Cập nhật kết quả**/reload để nhận bản sửa; BXH đang mở vẫn cập nhật định kỳ. Không hứa thông báo riêng tức thời cho mỗi lần hiệu chỉnh.

## Điểm và bằng chứng nguồn

- Đọc đội đã chốt từ `fantasy_entries` và snapshot `fantasy_submitted_picks`; không lấy bản nháp, lựa chọn trình duyệt hoặc đội của GW trước.
- Đọc `manual_fixture_player_stats` của league 39, season 2026, fixture thuộc đúng GW. Rating có giá trị là điểm SofaScore. `DID_NOT_PLAY` đã xác nhận nhận 0; `PLAYED` chỉ nhận 0 khi có xác nhận SofaScore không chấm. NULL chưa thu thập chặn công bố.
- Tính bằng `BigDecimal`; tổng lưu `DECIMAL(12,2)`. Một người có nhiều trận trong GW được cộng từng khóa fixture/player đúng một lần. Không dùng `fantasy_points` v1 hay OVR làm điểm.
- Kiểm tra tính hợp lệ bằng snapshot lúc chốt: 11 người/ô duy nhất, đúng sơ đồ và quyền vị trí đã lưu, tối đa 3 người/CLB, OVR không quá 910 theo yêu cầu 06/10 (local, chờ migration/phát hành), thời điểm chốt trước deadline. Không áp lại rosterAsOf/hồ sơ hiện tại để thay đổi đội chốt.
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

Hai bảng kết quả: `fantasy_result_publications` (PK mùa/GW/version, hash, người/thời điểm, PUBLISH/RECALCULATE, lý do) và `fantasy_team_results` (PK tài khoản/mùa/GW/version, submitted_version, tổng và JSON breakdown). BXH GW/mùa truy vấn phiên bản mới nhất từ hai bảng này, không có bảng BXH riêng.

## Giao diện và dữ liệu riêng

Fantasy có khu vực kết quả đội mình: tải/lỗi/thử lại, “Đang chờ kết quả” không kèm điểm; giao diện mới ngày 06/10 (local, chưa phát hành) đưa điểm từng người lên góc trên phải avatar và ô kết quả chỉ hiển thị tổng điểm. API vẫn giữ breakdown/version/thời gian và lý do 0 để đối chiếu; không trả điểm tạm. Không lưu đội thì NOT_PARTICIPATING, không tự tạo điểm 0. ADMIN có kiểm tra readiness, danh sách blocker, lý do, nút công bố/tái tính và lịch sử phiên bản. Sự kiện công bố tải lại trạng thái GW.

Đổi account/GW hủy request cũ và xóa kết quả trước; backend luôn xác định chủ từ session. API/result không công khai email hoặc bản nháp. Không tự nhập lựa chọn localStorage, không đổi đội chốt, không có API xem kết quả người khác trong bước này.

## Migration và kiểm chứng lịch sử

Schema local: [fantasy-result-schema.sql](../backend/src/main/resources/fantasy-result-schema.sql). Migration: [2026-10-05-fantasy-results-mysql.sql](../backend/sql/2026-10-05-fantasy-results-mysql.sql), MySQL 8.0.17+, bốn bảng bổ sung, FK/index/unique, UTC, CREATE TABLE IF NOT EXISTS; cần các migration tài khoản/GW/entry và bảng dữ liệu bóng đá trước đó. Không có DROP/reset/seed hay sao chép tài khoản H2. Production giữ SQL init tắt; bước backup/nhập migration/phát hành chỉ thực hiện khi được giao riêng.

- 10 test backend `FantasyResultIntegrationTest` đạt: thập phân, DNP/unrated, NULL chờ và thiếu dòng, hạn/trận hoãn/chưa xác nhận, nhiều trận, nhập lặp/xung đột, ba header sidecar, công bố lặp/tái tính, thay ngoại lệ bằng rating, rollback lỗi đội thứ hai, ADMIN/CSRF/quyền chủ. Chỉ chạy phần mới, không chạy lại toàn bộ auth.
- 4 test frontend `results.test.js` đạt; Maven package và Vite build đạt. Các thay đổi phát hiện trong kiểm tra được xác minh lại đúng phạm vi. Maven dùng POM build kiểm tra/classifier trong target vì lỗi đổi tên JAR Windows trước đó; JUnit TEMP/TMP được đặt trong target để tránh quyền dọn thư mục temp hệ thống. Không commit đầu ra build.
- MySQL 9.6 loopback riêng, database `fantasy_results_20261005`: service thực tạo 2 version/4 kết quả cho 2 đội; tổng 66.77 → 67.77, công bố lặp không thêm bản, rating NULL thô giữ nguyên. Chạy migration lại giữ 2 version/4 kết quả. Đây là kiểm chứng MySQL local, chưa phải Railway.
- Command từ JAR cũng đã chạy với datasource MySQL cô lập trên: exit 0, `fixtures=2 fixture_changes=0 unrated_changes=0`. Lịch sử version/lý do/người công bố được đối chiếu API và giao diện mở ở 390px; không tràn ngang.
- Trình duyệt gọi backend H2 cô lập qua frontend local: chờ không điểm → ADMIN readiness/công bố → trạng thái PUBLISHED → gọi lặp giữ version → xem 11 dòng/0 có lý do → reload → đổi GW không mang kết quả cũ. GW thử có fixture hoãn hiển thị đúng fixture và chặn nút công bố. Desktop 1440px và mobile 390px không tràn ngang/chồng hàng; không có lỗi JavaScript.
- Toàn bộ seed/helper/ảnh/report nằm trong target được ignore. Dữ liệu giả lập có 24/22 dòng mỗi fixture, chứng minh readiness không yêu cầu 40. Không đọc/nhập/công bố cuộc thi thật GW1–5 trong lượt này.

## Còn lại và bước tiếp theo

GW6 đã OPEN, schema/mã và luồng lưu production đã đạt; ID 2 đã được cấp ADMIN và readiness bằng phiên mới đã 200, USER ID 1 vẫn bị 403. Để vòng chạy trọn: thu thập đủ chỉ số và danh sách/sơ đồ thực cả hai đội của từng trận FINISHED; nhận rating SofaScore hoặc xác nhận không chấm; chuẩn bị/kiểm tra file, backup mới và nhập thống kê/đội hình/bằng chứng khi được giao; sau deadline và mọi fixture FINISHED, xử lý readiness rồi ADMIN công bố. Sau đó đối chiếu điểm 11 người/tổng/BXH GW/mùa bằng tài khoản thật. Chưa nhập thống kê/công bố kết quả GW6.

Transaction toàn GW cần giữ khóa nguồn/đội trong lúc chấm; đã kiểm chứng rollback/nhất quán local, chưa đo tải công bố production. Source_ref lưu đường dẫn/nguồn và hash dữ liệu database; phải giữ file nguồn/checkpoint, không coi file nguồn tồn tại là bằng chứng đã thu thập xong. BXH GW/mùa đã phát hành; xem đội người khác/phân trang/thông báo ngoài website chưa có. Sửa rating đã có số còn thiếu command hiệu chỉnh được duyệt; không tự ghi đè để vượt xung đột.

File của triển khai bước 4 trước đây (target/dist không commit):

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

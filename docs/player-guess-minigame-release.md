# Chuẩn bị phát hành Minigame PrismaXI — 08/10/2026

## Phạm vi và trạng thái

Người dùng đã chơi và xác nhận local hoạt động đúng. Lượt này chuẩn bị phát hành, không kết nối/ghi production, commit, push hay deploy. Bản local đã được người dùng commit tới `c944be8`; không cần commit lại ba commit Minigame `da93711`, `901cda2`, `c944be8`.

Đích theo cấu hình repo: frontend `https://premierhub.vercel.app`, API qua `/api` của Vercel tới Railway. Database production là MySQL hiện có; không chép file H2 hoặc database/tài khoản test lên đó. Phiên bản MySQL production trong biên bản auth trước đây là 9.7.2; chưa đối chiếu lại ở lượt này.

## Những thay đổi chuẩn bị

- [Migration MySQL](../backend/sql/2026-10-08-player-guess-mysql.sql): bảy bảng `player_guess_*`, InnoDB, unique/FK/CHECK/index, thời gian `DATETIME(6)` UTC, snapshot `MEDIUMTEXT` để chứa toàn roster. Ghi đúng một selector 2026 có chu kỳ rỗng; không seed câu hỏi, cầu thủ, tài khoản hoặc kết quả.
- [Preflight chỉ đọc](../backend/sql/2026-10-08-player-guess-preflight.sql): phiên bản/database/schema, pool đúng ngày Việt Nam, baseline tài khoản/Fantasy/tổng thống kê. Không in tên đáp án, mật khẩu, token, email hay identity.
- `AuthProxyFilter`: API Minigame cá nhân yêu cầu proof proxy giống Fantasy trong production; `GET /info` và `GET /leaderboard` vẫn công khai. Local tắt proxy vẫn chơi qua HTTP. Định danh session, CSRF và `X-PrismaXI-Account-ID` vẫn do các lớp hiện có kiểm tra.
- Test proxy và [test MySQL riêng](../backend/src/test/java/com/premierhub/minigame/PlayerGuessMySqlReleaseTest.java); không thêm dependency/framework.

## Rà phạm vi Git

Phạm vi được rà là diff từ trước `da93711` tới `c944be8` và các file chuẩn bị mới. Minigame sử dụng tài khoản sẵn có, bảng riêng, không ghi các bảng bóng đá hoặc Fantasy. Các thay đổi frontend ngoài trang Minigame là nav/route, mở menu tài khoản hiện có và xóa các câu chú thích theo yêu cầu người dùng.

Các file H2, MySQL datadir thử, tài khoản/identity/session đã lưu khi thử, backup, helpers trình duyệt, log, `target/`, `frontend/dist/`, `node_modules/`, `.env.local` đều được ignore và không có trong danh sách tracked. Không sao chép các file này vào migration, resources, public assets hoặc Git. Snapshot CSV bóng đá đã review trong repo là dữ liệu nguồn có căn cứ, không phải dump database/tài khoản thử; bootstrap CSV chỉ hoạt động với `minigame-local & !prod` và kiểm tra đích H2 riêng. Bộ test trong `src/test` có fixture tổng hợp để kiểm tra hành vi; Maven không đóng gói test đó vào ứng dụng, không nạp fixture khi khởi động production.

Frontend không chứa pool/đáp án hoặc bộ máy tự tính điểm. API `/players` chỉ trả danh sách ID/tên hợp lệ của ván; câu hỏi, gợi ý ẩn và đáp án được giữ phía server. Ba SVG của mockup là hình minh họa, không phải pool runtime. Không đưa mockup HTML vào public bundle. Hai trò còn lại giữ trạng thái sắp ra mắt.

Không đặt DB credentials, proxy secret, Google secret hoặc câu trả lời thật vào tên biến `VITE_*`; Vite công khai các giá trị này trong bundle. Migration và preflight không có credentials. Secret thực của Vercel/Railway giữ trong cấu hình dịch vụ hiện có, không thay bằng fixture test.

## Pool đáp án production

Luồng `PlayerGuessService.start` luôn gọi `PlayerGuessRepository.candidates` trên datasource runtime, rồi lọc `Candidate.eligible`. Không có fallback/mock pool. Chỉ đọc:

- `manual_player_memberships` của league 39, season 2026, `start_date <= ngày Việt Nam < end_date` (hoặc end NULL), đúng một membership active trên mọi CLB.
- CLB thuộc `season_clubs` cùng league/mùa; ID/tên từ `players` và `clubs`.
- Hồ sơ `player_season_profiles` khớp cầu thủ/CLB/mùa: quốc tịch không rỗng, ngày sinh có và không ở tương lai, chiều cao 100–250 cm, chân `LEFT/RIGHT/BOTH`, số áo 1–99, OVR **75–99**.
- `player_specific_positions.primary_position` thuộc `GK, LB, CB, RB, CM, CAM, LM, RM, LW, ST, RW`. Không bù bằng vị trí rộng trong thống kê mùa.

Tuổi tính tại ngày tạo câu hỏi, không lấy từ mockup. OVR thủ công đã được người dùng xác nhận vẫn được dùng như dữ liệu repo; không gắn nhãn toàn bộ là rating chính thức EA. Không sửa NULL để ép đủ pool.

Danh sách đoán có thể gồm cầu thủ OVR thấp/thiếu hồ sơ nếu tên và membership hợp lệ; đáp án luôn qua toàn bộ điều kiện trên. Daily cần ít nhất một ứng viên; để cả daily và practice chơi được cần **ít nhất hai**. Không chỉ kiểm tra OVR hoặc số lượng players.

Ngày kiểm tra local 08/10: snapshot thật trong repo có 534 người, 20 CLB, 364 đáp án hợp lệ. **Đây không phải xác nhận số lượng production.** Phải chạy preflight đúng MySQL đích vào ngày phát hành; pool thay đổi theo ngày/membership. Nếu số lượng hoặc hồ sơ lệch, dừng và review dữ liệu, không nhập bộ local tự động.

## Migration đầy đủ và chạy lại

| Bảng | Ràng buộc/chức năng |
| --- | --- |
| `player_guess_selector` | Một dòng/mùa 2026; chu kỳ JSON hợp lệ, giữ nguyên khi chạy lại |
| `player_guess_questions` | UUID, một daily/mùa/ngày, nhiều practice nhờ daily_date NULL, snapshot JSON, ngày/hết hạn đúng mode |
| `player_guess_games` | UUID, sequence tự tăng, FK account/question, một daily/account/mùa/ngày, score/guesses/hints/version/status/finished_at CHECK; index đọc practice và expire |
| `player_guess_practice_state` | Một ván hiện tại/account, FK account/game |
| `player_guess_guesses` | Một số lượt và một player/ván, FK game, penalty/correct CHECK |
| `player_guess_actions` | action UUID/account unique, fingerprint và FK game; retry không trừ hai lần |
| `player_guess_daily_results` | Một dòng/game và một dòng/account/mùa/ngày, FK game/account, score 0–100; nguồn BXH |

Không ALTER/DROP/TRUNCATE bảng hiện có, không cập nhật Fantasy, accounts, sessions hoặc mùa 2024/25, không cascade xóa tài khoản. Các FK account mới chặn xóa account có lịch sử Minigame; repo hiện chưa cung cấp thao tác xóa tài khoản. Người chơi có thể đoán những ID được snapshot nhưng không còn membership hiện tại; lịch sử không phụ thuộc hồ sơ bóng đá thay đổi.

Migration cần **MySQL >=8.0.17**, không dùng MariaDB; accounts.id phải là BIGINT signed và InnoDB. Index nằm trong CREATE TABLE, không dùng cú pháp H2 `CREATE INDEX IF NOT EXISTS`. `IF NOT EXISTS` chỉ an toàn với schema tương thích: trước chạy, xem `SHOW CREATE TABLE` từng bảng Minigame đã tồn tại, đối chiếu các cột/type/nullability/collation/index/constraints với file. Nếu lệch, dừng để chuẩn bị ALTER có review; không coi chạy không lỗi là schema đúng.

DDL MySQL tự commit, không thể bọc bảy CREATE trong transaction để rollback toàn bộ. Nếu dừng giữa chừng, giữ feature tắt, đối chiếu các bảng đã tạo rồi chạy tiếp migration phù hợp; không drop bảng có dữ liệu để làm lại. Selector chỉ được INSERT khi chưa có. Chạy lại một operator tại một thời điểm, sau đó so schema, số dòng và cycle/ledger với trước chạy.

Nguồn chính thức đã đối chiếu: [CREATE TABLE và IF NOT EXISTS](https://dev.mysql.com/doc/refman/8.4/en/create-table.html), [DDL implicit commit](https://dev.mysql.com/doc/refman/8.4/en/implicit-commit.html), [DATETIME/TIMESTAMP](https://dev.mysql.com/doc/refman/8.4/en/datetime.html).

## Cấu hình và ngày daily

| Nơi/biến | Yêu cầu khi phát hành |
| --- | --- |
| Railway `SPRING_PROFILES_ACTIVE` | `prod`; không ghép `minigame-local`, `auth-local` hoặc profile test |
| Railway `PREMIERHUB_MINIGAME_ENABLED` | `true` chỉ sau khi migration và preflight đạt; mặc định chưa bật |
| Railway `PREMIERHUB_MINIGAME_LOCAL_DATA_ENABLED` | `false`; không chạy bootstrap local |
| `SPRING_SQL_INIT_MODE`, `SPRING_SESSION_JDBC_INITIALIZE_SCHEMA` | Giữ `never` theo cấu hình prod/hiện có; không nạp schema H2 khi startup |
| `PREMIERHUB_JDBC_URL` | MySQL hiện có, giữ TLS phù hợp; `connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true` |
| `JAVA_TOOL_OPTIONS` | Giữ options hiện có và `-Duser.timezone=UTC`; không ghi đè options khác khi bổ sung |
| DB runtime privileges | SELECT/INSERT/UPDATE trên bảy bảng Minigame; giữ quyền auth/session/Fantasy/bóng đá hiện có. DDL dùng migration operator, không bật tự tạo bảng runtime |
| Proxy/cookie/CSRF | Giữ public origin frontend, secret hai dịch vụ khớp, Secure/HttpOnly/host-only/Path=/SameSite=Lax, forwarding=none và CORS chính xác như [auth-release](auth-release-2026.md) |
| Vercel | Root `frontend`, build `npm run build`, output `dist`, route `/api` hiện có; không cần route Minigame riêng hoặc dependency mới |
| Vercel API/secret | Bỏ `VITE_API_BASE_URL`; giữ proxy secret riêng không có VITE_, không đổi Google callback đang dùng |
| Replica | Giữ một replica như release auth hiện có; rate limit auth chưa chia sẻ giữa nhiều instance |

Clock của Minigame là `Clock.systemUTC()`, ngày tính bằng `Asia/Ho_Chi_Minh`, hết hạn là 00:00 ngày kế tiếp (17:00 UTC hôm trước). Không cần đổi timezone hệ điều hành/DB sang Việt Nam. DATETIME lưu UTC; session JDBC cũng ở MySQL, frontend gọi cùng origin với cookie và CSRF.

Câu hỏi daily được tạo lazily khi người đầu tiên bấm bắt đầu (practice cũng bảo đảm daily tồn tại để loại đáp án daily). Transaction khóa selector/account/game, unique bảo đảm một câu chung/ngày và một ván/account/ngày. Chu kỳ, snapshot, version, lịch sử đoán và action retry đều lưu SQL; restart không reset. Daily dở được chuyển EXPIRED/0 khi đọc/ghi/lịch sử/BXH, không cho tiếp tục sau hạn. **Không có job ghi đúng từng dòng ở 00:00**; API đổi ngày đúng mốc dù việc cập nhật dòng cũ diễn ra khi truy cập. Không thêm cron hoặc Pre-deploy Command.

BXH chỉ SUM điểm cuối từ daily ledger 2026, đồng hạng `1,1,3`, không cộng practice hoặc điểm tạm. Các page GET không tự bắt đầu ván. Ván mới mở sẵn chiều cao/chân thuận/tuổi, vẫn 100 điểm; ván cũ giữ tiến trình.

## Thứ tự phát hành sau khi được giao

**Backup → migration → deploy backend → deploy frontend → kiểm tra tài khoản thật** phù hợp repo. Cần phối hợp Git với auto-deploy: nếu push kích hoạt Railway/Vercel, giữ feature tắt và tạm giữ auto-deploy hoặc chỉ push sau migration. Commit local có thể chuẩn bị trước; push không thay thế thao tác SQL. Không bật feature hoặc thêm migration tự chạy vào Pre-deploy Command để né thứ tự.

1. Chốt SHA sẽ phát hành (gồm ba commit Minigame hiện có và commit chuẩn bị). Xác nhận đúng MySQL host/port/database/version, tài khoản migration, InnoDB/accounts.id, schema bóng đá và bảy bảng nếu đã có. Chạy preflight, cần eligible_answers >=2. Chưa đọc/ghi production trong lượt này.
2. Backup đầy đủ database **ngoài Git**, ghi thời gian/đích/SHA-256, xác nhận đủ bảng và dump hoàn tất; thử restore vào MySQL cô lập. Lưu baseline schema và các bảng accounts/identities/sessions, Fantasy, thống kê 2024/2026 để đối chiếu. Không xóa hay reset tài khoản thật để thử.
3. Giữ Minigame tắt. Chạy migration bằng operator DDL trên đích đã xác nhận, không chạy auth/Fantasy migrations lại. Kiểm tra bảy bảng/index/FK/CHECK, một selector rỗng lần đầu và không có question/game/result. Chạy lại migration trên schema tương thích, xác nhận không thêm/reset dòng và baseline ngoài phạm vi không đổi.
4. Deploy backend đúng SHA với prod/MySQL/UTC và bật `PREMIERHUB_MINIGAME_ENABLED=true`. `/actuator/health` đạt; `/api/minigame/2026/player-guess/info` trả season 2026, timezone Việt Nam, initiallyRevealedHints=3. `GET /leaderboard` có thể rỗng lần đầu. Private API trực tiếp Railway thiếu proxy proof phải trả 403, không phải lỗi cần bỏ bảo vệ.
5. Deploy frontend cùng SHA trên Vercel, giữ route/env proxy đã có. Xác nhận `/api` trả JSON đúng backend và không cache dữ liệu phiên. Nếu frontend đã phát hành trước, vẫn deploy/đối chiếu lại đúng SHA; không báo backend đã bật chỉ vì thấy tab.
6. Chủ tài khoản thật đăng nhập qua frontend; bắt đầu một practice, thấy 3 gợi ý/100 điểm, mở một gợi ý còn 90 và không dùng lượt. Reload, chuyển trang/đăng nhập lại bằng cùng tài khoản phải giữ ván; thử restart backend có phối hợp rồi đối chiếu tiến trình. Không cần tạo tài khoản giả trên production.
7. Người dùng tự chơi daily khi sẵn sàng: một ván/tài khoản/ngày, kết thúc ghi một dòng BXH/lịch sử; practice không đổi BXH. Nếu kiểm tra daily thật, điểm đó là kết quả thật và không xóa/reset sau kiểm tra. Đối chiếu quanh 00:00 Việt Nam: ván dở cũ EXPIRED/0, daily mới 100/3 gợi ý, practice còn lưu. Không điều chỉnh đồng hồ production hoặc tạo kết quả mẫu để thử.
8. Kiểm tra đăng xuất không tiếp tục đọc/đoán ván, đổi tài khoản không thấy tiến trình người khác, POST thiếu CSRF bị chặn; Google/email/Fantasy/trang bóng đá vẫn hoạt động. Chỉ đọc schema/count/baseline khi đối chiếu, không in đáp án/cycle_json/snapshot_json/identity/session/credential vào biên bản công khai.

Nếu lỗi: tắt feature, rollback backend/frontend về SHA đã chạy ổn theo quy trình dịch vụ. Giữ bảy bảng và dữ liệu đã chơi; không DROP để rollback. Khôi phục toàn DB từ backup chỉ trong tình huống cần thiết, được cho phép riêng và có đánh giá các ghi tài khoản/Fantasy/bóng đá phát sinh sau backup.

### Lệnh SQL dành cho lượt được phép nhập production

Chạy từ gốc repo, chỉ sau xác nhận đích, backup và quyền thực hiện. Các biến MYSQL_* và MYSQL_PWD được nạp bí mật ở terminal; không dùng password trên argv hoặc ghi vào file tracked. Không in env/URL/secret để kiểm tra.

```powershell
$mysqlClient = 'C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe'
$mysqlDump = 'C:\Program Files\MySQL\MySQL Server 9.6\bin\mysqldump.exe'
foreach ($requiredName in @('MYSQL_HOST','MYSQL_PORT','MYSQL_USER','MYSQL_DATABASE','MYSQL_PWD','MYSQL_BACKUP_PATH')) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($requiredName))) {
        throw "Missing environment variable: $requiredName"
    }
}
$releaseRepoPath = [IO.Path]::GetFullPath((Get-Location).Path)
$releaseBackupPath = [IO.Path]::GetFullPath($env:MYSQL_BACKUP_PATH)
if ($releaseBackupPath.StartsWith($releaseRepoPath + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Choose a backup path outside the repository.'
}
if (Test-Path -LiteralPath $releaseBackupPath) { throw 'Choose a new backup filename; do not overwrite an older backup.' }
$mysqlTargetArgs = @('--no-defaults', '--protocol=TCP', "--host=$env:MYSQL_HOST", "--port=$env:MYSQL_PORT", "--user=$env:MYSQL_USER", '--ssl-mode=REQUIRED', '--default-character-set=utf8mb4')
# Confirm this exact target/schema separately before running any SQL or dump.
Get-Content -LiteralPath 'backend/sql/2026-10-08-player-guess-preflight.sql' -Raw -Encoding UTF8 | & $mysqlClient @mysqlTargetArgs "--database=$env:MYSQL_DATABASE"
if ($LASTEXITCODE -ne 0) { throw 'Preflight failed; stop.' }
& $mysqlDump @mysqlTargetArgs --single-transaction --routines --triggers --events --no-tablespaces --set-gtid-purged=OFF --databases $env:MYSQL_DATABASE "--result-file=$env:MYSQL_BACKUP_PATH"
if ($LASTEXITCODE -ne 0) { throw 'Backup failed; stop.' }
# Verify completeness, SHA-256 and an isolated restore before proceeding.
Get-Content -LiteralPath 'backend/sql/2026-10-08-player-guess-mysql.sql' -Raw -Encoding UTF8 | & $mysqlClient @mysqlTargetArgs "--database=$env:MYSQL_DATABASE" --show-warnings
if ($LASTEXITCODE -ne 0) { throw 'Migration failed; keep feature disabled and inspect schema.' }
```

Không dùng `--force`; không chạy toàn đoạn chỉ vì lệnh preflight thành công. Chạy lại migration sau đối chiếu schema/baseline, không chạy đồng thời. Nếu thiếu quyền backup routines/events hoặc DDL thì dừng để dùng operator phù hợp, không bỏ qua lỗi hoặc cấp lại quyền runtime tùy tiện.

## Kiểm tra của lượt chuẩn bị

**11 test liên quan đạt, 0 lỗi/thất bại**: 7 `AuthProxyFilterTest`, 3 `AuthProxyIntegrationTest`, 1 `PlayerGuessMySqlReleaseTest`. Sau khi bổ sung kiểm tra unique trực tiếp, chỉ chạy lại test MySQL và vẫn đạt. Không chạy lại toàn suite hoặc build frontend vì giao diện không sửa trong lượt này. Các lỗi toàn suite có sẵn ở tài liệu API không được sửa hoặc báo đã hết lỗi.

MySQL Community **9.6.0**, server mới bind `127.0.0.1:33028`, datadir riêng dưới `target/minigame-release-check/mysql`, không dùng service/database MySQL hiện có. Migration tạo đủ 7 bảng; sau khi chơi bằng roster thật, chạy lại hai lần giữ nguyên schema, cycle, snapshots, versions, actions và daily ledger. Snapshot toàn bộ dữ liệu ngoài Minigame trong database thử (có account/identity, Fantasy entry và thống kê 2024) giữ nguyên. Đã kiểm tra FK/CHECK, unique daily và ledger, retry cùng action từ hai connection chỉ trừ một lần, xung đột version, rollback sau ghi, mở lại service/datasource giữ tiến trình, practice loại daily/không cộng BXH, đổi ngày và expire đúng mốc Việt Nam.

Preflight chạy thật qua CLI trên MySQL thử: 534 active/guess choices, 364 eligible, 0 membership trùng, 3 thiếu OVR, 167 OVR thấp, 2 thiếu trường gợi ý khác. Kết quả khớp `Candidate.eligible`; không dùng pool giả. 2 tài khoản và 1 identity tổng hợp chỉ ở database thử và không đưa vào production/Git dưới dạng dump. Tính an toàn của migration được xác nhận cho schema tương thích trên MySQL thử; chưa xác nhận schema/phiên bản production hiện tại.

Rà 40 đường dẫn thuộc ba commit Minigame và lượt chuẩn bị: không có build/DB/backup/env/private key/token thật tracked, không phát hiện các mẫu credentials đã quét; đọc code/config xác nhận chỉ tham chiếu tên biến bí mật. Bundle frontend hiện có không chứa các marker fixture hoặc JDBC URL đã kiểm tra; không build lại. Rà source xác nhận không có pool/đáp án mẫu runtime. Đây là rà phạm vi Minigame, không phải chứng nhận mọi lịch sử Git hoặc mọi dạng secret có thể có.

Artefacts tại `backend/target/minigame-release-check/`: `affected-tests.log`, `mysql-final-tests.log`, `preflight-results.log`, `git-audit-result.json` và datadir/helpers, đều được ignore. Không đưa datadir, tài khoản/identity thử hoặc log vào commit. Server MySQL thử được dừng sau kiểm tra; backend/frontend local của người dùng không bị dừng. Cấu hình production và frontend không đổi trong lượt này.

Test MySQL được bật riêng bằng `PRISMAXI_MINIGAME_MYSQL_TEST=true`, chỉ kết nối loopback `127.0.0.1:33028/minigame_release_check`, từ chối database không rỗng. Cần **server/datadir dùng một lần**, không trỏ vào service MySQL có sẵn; không có tham số URL production. Khi không bật, test được skip. Dữ liệu review và fixture chỉ nạp trong test này, không đưa vào migration/runtime prod.

Lệnh từ `backend/`, sau khi MySQL thử đã được khởi tạo cô lập:

```powershell
$env:PRISMAXI_MINIGAME_MYSQL_TEST='true'
mvn '-Dtest=AuthProxyFilterTest,AuthProxyIntegrationTest,PlayerGuessMySqlReleaseTest' '-DargLine=-Duser.timezone=UTC' test
Remove-Item Env:PRISMAXI_MINIGAME_MYSQL_TEST
```

## File cần commit và điểm chặn

Commit mới chỉ gồm:

- `backend/sql/2026-10-08-player-guess-mysql.sql`
- `backend/sql/2026-10-08-player-guess-preflight.sql`
- `backend/src/main/java/com/premierhub/accounts/AuthProxyFilter.java`
- `backend/src/test/java/com/premierhub/accounts/AuthProxyFilterTest.java`
- `backend/src/test/java/com/premierhub/minigame/PlayerGuessMySqlReleaseTest.java`
- `docs/player-guess-minigame-release.md`
- Các cập nhật link/trạng thái ở `README.md`, `docs/player-guess-minigame-api.md`, `docs/player-guess-minigame-plan.md`.

Commit message: `feat(minigame): prepare MySQL migration and production release`.

Chặn phát hành thực tế: chưa xác nhận schema/version/quyền và pool **production hiện tại**; chưa có backup mới/restore được kiểm tra; chưa áp dụng migration hoặc xác nhận env/UTC/feature flag; chưa deploy đồng bộ và kiểm tra bằng tài khoản thật. Local/MySQL thử không thay thế các bước này. Không cần nhập lại dữ liệu bóng đá nếu preflight production đạt, và không dùng bootstrap H2 trên production. Lượt này không commit, push, deploy hoặc ghi production.

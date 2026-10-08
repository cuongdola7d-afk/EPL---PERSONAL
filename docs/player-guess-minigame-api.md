# Backend Minigame Đoán cầu thủ — local 2026/27

Ngày triển khai: 08/10/2026. Đặc tả chính thức: [kế hoạch Minigame](player-guess-minigame-plan.md). Backend đã có luật, snapshot, tiến trình, daily/practice, lịch sử và BXH. Frontend đã nối API theo mockup; xem [hướng dẫn giao diện local](player-guess-minigame-frontend.md). Chưa áp dụng migration hoặc dữ liệu lên production.

## Mockup và quyết định đã chốt

Đã đọc `PrismaXI · Minigame (mockup) (1).html` do người dùng cung cấp. API phục vụ banner daily, hộp chọn chế độ, đồng hồ đổi câu hỏi, điểm/lượt/gợi ý, danh sách chọn cầu thủ, lịch sử đoán, kết quả và BXH đồng hạng. Dữ liệu/engine mô phỏng trong file không được nhập vào hệ thống.

Ngày 08/10/2026 người dùng chọn các mặc định đã đề xuất:

- Practice tránh 5 đáp án gần nhất theo tài khoản; nếu pool nhỏ thì nới từ cũ nhất. Luôn loại đáp án daily ngày tạo ván, kể cả đã hoàn thành daily.
- Danh sách đoán gồm mọi cầu thủ có membership duy nhất thuộc CLB Premier League mùa 2026/27 tại ngày tạo câu hỏi, không chỉ pool OVR >=75. Danh sách ID/tên này cũng được snapshot để chuyển nhượng không thay validation của ván.
- BXH công khai, chỉ tên hiển thị, ID tài khoản, hạng, tổng điểm và số ván daily đã kết thúc; không xuất email.
- Daily giữ danh sách ứng viên từ lúc bắt đầu chu kỳ, loại người không còn hợp lệ khi chọn ngày tiếp theo; ứng viên mới vào chu kỳ sau. Khi không còn ứng viên chưa dùng hợp lệ, bắt đầu chu kỳ mới từ pool hiện tại. Tránh lặp ngay ngày trước nếu có ít nhất hai ứng viên.
- Đồng hạng kiểu `1,1,3`; thứ tự account ID trong nhóm đồng điểm chỉ giúp phân trang ổn định, không phá đồng hạng.

Khác với hành vi mẫu, **cả practice và daily cần đăng nhập**; vị trí dùng mã cụ thể như `RW`, không đổi thành nhóm DEF/MID/FWD; tuổi tính theo ngày Việt Nam tại lúc tạo câu hỏi. Điểm vẫn theo luật chính thức.

## Chạy local

Từ `backend/`:

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=minigame-local'
```

Profile [minigame-local](../backend/src/main/resources/application-minigame-local.properties) bật `premierhub.minigame.enabled=true`, ép datasource H2 riêng `./target/minigame-local`, user `sa`/mật khẩu trống, nạp schema trò chơi và tắt auth proxy. Không kế thừa JDBC URL/user/password production. Không kết hợp với profile `prod`.

Feature mặc định tắt ở các profile khác. `application.properties` và cấu hình datasource production không bị sửa; production vẫn `spring.sql.init.mode=never`. [Schema Minigame](../backend/src/main/resources/player-guess-schema.sql) hiện phục vụ H2 local/test, chưa phải migration MySQL đã được xác minh.

Profile hiện tự nạp roster/membership/hồ sơ/vị trí từ snapshot đã chốt trong repo vào H2 riêng: 534 cầu thủ/20 CLB, 364 đáp án đủ điều kiện. Nạp lại cùng snapshot thêm 0 dòng, không tạo ván hay sửa tiến trình. Xem [quy trình và kiểm tra dữ liệu local](player-guess-minigame-local-data.md). Có thể tắt bước này bằng `--premierhub.minigame.local-data.enabled=false`; database rỗng khi tắt vẫn trả `INSUFFICIENT_DATA`. Dữ liệu demo của mockup không được nhập. File H2 local nằm trong `target/`, mất nếu chạy `clean`; restart thông thường giữ dữ liệu trên file.

Maven hiện chạy **JDK 26** theo môi trường người dùng; POM hiện giữ `<java.version>21</java.version>`, tức mã được biên dịch với release 21. JavaSE 26 của IDE và release Maven là hai cấu hình khác nhau; lượt này chưa sửa POM hoặc IDE.

## Giao thức chung

Prefix: `/api/minigame/2026/player-guess`.

- `/info` và `/leaderboard` công khai; mọi endpoint chơi/lịch sử/tìm cầu thủ cần phiên đăng nhập.
- Request cá nhân gửi cookie bằng `credentials: 'include'`, dùng URL cùng origin. Gửi `X-PrismaXI-Account-ID` lấy từ `/api/auth/me`; backend luôn lấy chủ ván từ phiên, header chỉ phát hiện tab đã đổi tài khoản.
- Mọi POST cần token/header từ `/api/auth/csrf`. Thiếu/sai CSRF trả 403. Cơ chế đăng nhập email hoặc Google được dùng lại, không tạo auth riêng.
- Các response của controller có `Cache-Control: no-store`.
- `actionId` là UUID. Một thao tác thành công giữ cùng key khi retry, kể cả mất response. Key duy nhất theo tài khoản; dùng lại key với nội dung khác trả `ACTION_KEY_REUSED`.
- `expectedVersion` là version vừa đọc từ server. Hai lệnh mới cùng version chỉ một lệnh được áp dụng; lệnh còn lại nhận `VERSION_CONFLICT`, cần tải lại trạng thái trước tạo thao tác mới.
- Start/retry cũng có key. Để tạo practice đầu tiên: `expectedGameId=null`, `expectedVersion=0`. Khi chủ động chơi tiếp sau kết thúc, gửi ID và version của ván hiện hành. Hai tab không thể thay nhau tạo thêm ván ngoài ý muốn.
- Không gửi điểm, lượt, trạng thái, đáp án hoặc chỉ số gợi ý tùy chọn từ client. Backend chỉ mở gợi ý kế tiếp.

## Endpoint

| Method | Đường dẫn sau prefix | Kết quả |
| --- | --- | --- |
| GET | `/info` | Luật hiển thị, thứ tự nhãn gợi ý, `serverTime`, timezone và `nextDailyAt`; không tạo câu hỏi/ván. |
| GET | `/practice/current` | Ván practice hiện hành hoặc `NOT_STARTED`; kết thúc vẫn trả kết quả, không tạo tiếp. |
| POST | `/practice/start` | Bắt đầu/chơi tiếp chủ động; còn ván đang chơi thì tiếp tục ván đó. |
| GET | `/daily/current` | Trạng thái daily hôm nay hoặc `NOT_STARTED`; không tạo ván. |
| POST | `/daily/start` | Tạo/đọc đúng một daily của tài khoản/ngày, kết quả đã kết thúc không được chơi lại. |
| GET | `/games/{gameId}` | Tiến trình/kết quả thuộc tài khoản, gồm daily đã hết hạn. |
| GET | `/players?gameId=...&q=...` | Danh sách snapshot ID/tên được phép đoán. `q` tối đa 100 ký tự, bỏ dấu/hoa thường như tiện ích hiện có. Không gửi CLB, OVR hay dữ liệu gợi ý. |
| POST | `/games/{gameId}/guesses` | Đoán bằng `player_id`, không nhận tên nhập tự do làm đáp án. |
| POST | `/games/{gameId}/hints/next` | Mở đúng gợi ý tiếp theo. |
| GET | `/daily/history?offset=0&limit=20` | Ván daily của tài khoản theo ngày giảm dần; không tạo bù ngày không chơi. |
| GET | `/leaderboard?offset=0&limit=20` | Tổng điểm daily kết thúc, hạng và nhãn đồng hạng. |

`offset` 0–100000, `limit` 1–100. Danh sách `/players` không phân trang vì là roster snapshot của một mùa, chỉ trả ID/tên tối thiểu. UI có thể tải cả danh sách rồi dùng `matchesPlayerSearch` hiện có, hoặc tìm bằng `q` và hiển thị tối đa sáu lựa chọn như mockup. Danh sách không đánh dấu người nào là đáp án; frontend tự đối chiếu lịch sử để hiển thị “Đã đoán”.

## Request mẫu

Bắt đầu daily hoặc practice lần đầu, sau khi có phiên, account header và CSRF:

```json
{
  "actionId": "e5209f3b-99c8-4b80-81c0-cf625b801e94",
  "expectedGameId": null,
  "expectedVersion": 0
}
```

Đoán: ID và version chỉ mang tính minh họa, phải dùng dữ liệu API của database local:

```json
{
  "actionId": "3dc17464-c4c3-4ef8-878b-732a9215e990",
  "expectedVersion": 0,
  "player_id": 123
}
```

Mở gợi ý gửi `actionId` và `expectedVersion`, không có `player_id` hay index gợi ý. Chơi practice tiếp gửi UUID hành động mới, `expectedGameId` và `expectedVersion` của ván practice vừa kết thúc.

## Response phục vụ bố cục

GET current trả `status`, `date` Việt Nam, `serverTime`, `nextDailyAt` và `game` (`null` nếu chưa bắt đầu). `status` là `NOT_STARTED`, `IN_PROGRESS`, `WON`, `LOST` hoặc `EXPIRED`.

POST trả `code`, `message`, `game` và `effect`. Khi thành công `code=OK`; replay trả trạng thái **mới nhất** của đúng ván đã nhận hành động, `effect.replayed=true`, không áp dụng thêm hành động hoặc đổi practice hiện hành về ván cũ.

| Trường `game` | Chỗ dùng trong mockup |
| --- | --- |
| `gameId`, `accountId`, `season`, `mode`, `questionDate`, `version` | Chế độ, thông tin phiên và request kế tiếp. Không phải ID câu hỏi/đáp án. |
| `status`, `currentScore`, `finalScore` | Điểm tạm lúc chơi, điểm cuối ở thẻ kết quả. Thua/hết hạn dùng `finalScore=0` dù điểm tạm còn dương. |
| `guessesUsed`, `guessesRemaining` | Ba dấu lượt đoán và thông báo còn bao nhiêu lượt. Lượt đúng cũng là một lượt hợp lệ. |
| `revealedHintCount`, `totalHints` | Số gợi ý mở khi chơi (ván mới ban đầu 3/8: chiều cao, chân thuận, tuổi); khi kết thúc đủ tám ô hiển thị dù số gợi ý đã mua/tự mở vẫn giữ giá trị lịch sử. Ván đã lưu theo luật cũ có thể bắt đầu từ 2/8. |
| `canGuess`, `canRevealHint`, `nextHintKey` | Khóa nút và tô ô gợi ý kế tiếp. Ván kết thúc khóa cả hai hành động. |
| `hints[]` | Đúng tám mục theo thứ tự, mỗi mục có `key`, `label`, `revealed`, `value`. Khi ẩn `value=null`; khi kết thúc mọi `revealed=true`. |
| `guesses[]` | `number`, `playerId`, tên snapshot, `correct`, `penalty` (0/20) và `autoRevealedHint`; đủ hiển thị lịch sử và nhãn đã đoán. |
| `answer` | `null` lúc chơi. Kết thúc mới trả ID/tên/CLB/vị trí cụ thể/số áo cho thẻ đáp án và chữ viết tắt avatar. |
| `serverTime`, `expiresAt`, `nextDailyAt` | Đếm ngược theo server; practice có `expiresAt=null`. Thời gian trình duyệt chỉ giúp hiển thị, không quyết định hạn. |

`effect` gồm `type` (`STARTED`, `RESUMED`, `HINT_REVEALED`, `WRONG`, `CORRECT`, `REPLAY`), `scoreChange` (thay đổi điểm tạm thực tế, đã chặn về 0), `revealedHintKey` và `replayed`. UI dùng hiệu ứng này cho thông báo/rung/ô vừa mở; điểm nhận khi thắng lấy `game.finalScore`, không cộng lại `scoreChange`. Replay không chạy lại hiệu ứng trừ điểm.

BXH trả `season`, `serverTime`, `players`, `offset`, `limit`. Mỗi dòng có `rank`, `accountId`, `displayName`, `totalPoints`, `dailyGames`, `tied`. Hạng và cờ đồng hạng tính trước phân trang. UI xác định dòng “Bạn” bằng account ID, không so tên như dữ liệu mô phỏng.

## Lỗi và trạng thái

| HTTP | Code/trường hợp | Cách xử lý |
| --- | --- | --- |
| 401 | `AUTH_REQUIRED` | Mở luồng đăng nhập hiện có. |
| 403 | `ACCESS_DENIED` | Làm mới phiên/CSRF, không tự gửi thao tác với key mới. |
| 400 | `MINIGAME_INPUT` | Thiếu header, UUID, version, ID hoặc body sai; không tính lượt/điểm. |
| 404 | `GAME_NOT_FOUND` | Ván không tồn tại hoặc không thuộc tài khoản; không lộ chủ ván thật. |
| 409 | `SESSION_CHANGED` | Hủy request/state của tài khoản cũ và tải lại phiên. |
| 409 | `VERSION_CONFLICT` | Đọc lại ván; response thao tác có trạng thái hiện tại. |
| 409 | `ACTION_KEY_REUSED` | Key cũ có payload khác; không áp dụng lại. |
| 409 | `INSUFFICIENT_DATA`, `PRACTICE_POOL_TOO_SMALL` | Hiển thị thiếu dữ liệu; không tạo đáp án giả và không nới điều kiện loại daily. |
| 409 | `GAME_FINISHED`, `GAME_EXPIRED` | Hiển thị kết quả/đáp án, khóa nút. Expiry được commit cùng kết quả 0. |
| 409 | `DAY_CHANGED_RETRY` | Ngày đổi trong lúc SQL đang xử lý: transaction hành động rollback; đọc lại ván để chuẩn hóa hết hạn rồi quyết định thao tác mới. |
| 422 | `INVALID_PLAYER`, `PLAYER_ALREADY_GUESSED`, `ALL_HINTS_REVEALED` | Hiển thị lỗi và trạng thái trả về; không trừ thêm điểm/lượt. |

Key của request bị từ chối trước khi áp dụng không được ghi vào sổ hành động; client không tự sửa payload rồi retry mù. Cần đọc lại trạng thái khi không biết request trước đã thành công hay chưa.

## Cơ chế lưu và kiểm thử

- Selector daily là một dòng JSON trong `player_guess_selector` lưu chu kỳ, queue còn lại và đáp án trước. Câu hỏi daily đã dùng lưu trong `player_guess_questions`; restart không xóa queue hoặc lịch sử. Thiết kế này giản lược nhóm bảng chu kỳ đề xuất trong kế hoạch, vẫn giữ các luật không lặp.
- Snapshot trong câu hỏi chứa tám gợi ý và roster ID/tên hợp lệ lúc tạo. DTO riêng đảm bảo response đang chơi không xuất đáp án, ngày sinh, dữ liệu snapshot nội bộ hay giá trị gợi ý ẩn.
- Khi start: khóa selector -> tài khoản -> ván; khi read/change: tài khoản -> ván. Version, action key và UNIQUE bảo vệ tạo ván/lượt đồng thời. `game_sequence` giữ thứ tự practice chính xác cả khi hai ván có cùng timestamp.
- Hoàn tất ván và ghi sổ `player_guess_daily_results` trong cùng transaction. BXH SUM sổ này, không có thao tác cộng vào bảng tổng dễ bị lặp. Practice không ghi sổ daily.
- Daily quá hạn chuẩn hóa khi đọc/ghi/lịch sử/BXH. Không dùng cron. Ván chưa được truy cập sau hạn có thể chưa đổi dòng SQL ngay, nhưng API không cho đoán quá hạn; đọc kết quả trả `EXPIRED`/0.
- Các test JUnit kiểm tra luật, lỗi đầu vào, đủ dữ liệu/membership, OVR thủ công, snapshot, timezone/sinh nhật/nửa đêm, chu kỳ, practice pool nhỏ, retry/version, nhiều tab, rollback, ownership/CSRF, cookie JDBC và không lộ đáp án.
- Test persistence mở H2 trên file trong thư mục tạm của workspace, shutdown rồi mở datasource/service mới để đối chiếu tiến trình practice, snapshot và selector daily.

Chạy test từ `backend/` trên Windows với thư mục tạm thuộc workspace ngay từ lúc JVM khởi động:

```powershell
$minigameTempRoot = Join-Path (Get-Location) 'target/test-tmp'
New-Item -ItemType Directory -Path $minigameTempRoot -Force | Out-Null
$env:TEMP = $minigameTempRoot
$env:TMP = $minigameTempRoot
mvn '-Dtest=PlayerGuessRulesTest,PlayerGuessIntegrationTest,PlayerGuessPersistenceTest' test
```

Kết quả thực tế ngày 08/10/2026, Maven 3.9.14 chạy JDK 26, release 21:

| Nhóm kiểm tra cuối | Test | Thất bại/lỗi |
| --- | --- | --- |
| `PlayerGuessRulesTest` | 20 | 0/0 |
| `PlayerGuessIntegrationTest` | 25 | 0/0 |
| `PlayerGuessPersistenceTest` | 1 | 0/0 |
| Fantasy Entry/Result Integration | 25 | 0/0 |
| Auth Rate Limit/Proxy Integration | 8 | 0/0 |
| Tổng nhóm liên quan | 79 | 0/0 |

Lượt toàn backend gần nhất chạy 408 test: 7 thất bại, 0 lỗi. Sáu thất bại trong `AuthIntegrationTest`/`GoogleOAuthIntegrationTest` do request liên tiếp chạm giới hạn và nhận 429; một thất bại trong `ArsenalUnitedPositionBatchTest` do thứ tự danh sách vị trí khác kỳ vọng. Đã tạo bản HEAD cô lập dưới `backend/target/minigame-baseline-20261008`, chạy riêng ba lớp này và tái hiện đúng cả 7 thất bại (18 test, 7 thất bại). Vì vậy không báo toàn suite đã qua và không sửa code/test ngoài phạm vi Minigame để che các lỗi có sẵn. Nhóm kiểm tra cuối 79 test ở bảng trên chạy sau lần chỉnh thứ tự khóa BXH và bổ sung test đồng thời cuối cùng.

Lượt chạy đầu gặp lỗi cleanup `@TempDir` do thư mục Windows Temp ngoài workspace; chạy lại với `TEMP`/`TMP` trong `target/test-tmp` đã hết lỗi này, kể cả test persistence. Log/report/bản HEAD đối chiếu đều nằm dưới `target/`, không phải nguồn để commit.

Ở lượt backend ban đầu chưa kiểm chứng khóa/migration trên MySQL. Lượt chuẩn bị phát hành đã thêm migration riêng và kiểm tra trên MySQL 9.6 cô lập; xem [hướng dẫn phát hành](player-guess-minigame-release.md). Chưa đối chiếu hoặc áp dụng lên production.

## Trạng thái sau lượt nối frontend

Tab/hash Minigame, chọn chế độ, gợi ý, chọn ID, lịch sử đoán/daily, kết quả, BXH, countdown và đăng nhập đã nối API. Frontend chỉ đọc tiến trình khi vào trang; bắt đầu ván do người chơi bấm. Xem kết quả kiểm tra và cách chạy trong [tài liệu frontend](player-guess-minigame-frontend.md).

Roster local có căn cứ đã được nạp và kiểm tra daily/luyện tập qua giao diện. Migration MySQL, preflight và thứ tự backup → migration → backend/frontend → tài khoản thật đã được chuẩn bị ở [hướng dẫn phát hành](player-guess-minigame-release.md). Production vẫn cần xác nhận schema/pool hiện tại và backup mới trước khi được phép áp dụng; chưa ghi production/commit/push/deploy trong lượt chuẩn bị.

# Đo độ trễ đọc Render → Aiven

## Trạng thái ngày 08/10/2026

Đây là kiểm tra đọc, không thay dữ liệu, schema/index, hosting, gói dịch vụ hoặc proxy Vercel. Bộ đo mới chỉ được chuẩn bị và test local; chưa commit/push/deploy. Không có kết quả tối ưu trước/sau từ JVM Render ở thời điểm viết tài liệu.

- Render API xác nhận service `epl-personal` ở **Singapore**, plan Free; image đang live là `61de7ae015756c7fc1d3d6d8538746a6cc8f7363`.
- Aiven MySQL 8.4.8 / `defaultdb`; connection Connector/J đang dùng `TLS_AES_256_GCM_SHA384`. CA và hostname verification giữ nguyên. **Cloud/region Aiven chưa xác nhận**: đọc mục Overview của đúng service, không suy ra từ IP/hostname hoặc thời gian kết nối PC.
- Chưa sửa pool: maximum 4, minimum idle 0, idle timeout 120 giây, max lifetime 300 giây, keepalive 0. Các giới hạn này có thể tạo connection mới sau khoảng nghỉ; chưa đủ bằng chứng để đổi.

Đã gọi 5 GET CLB và 5 GET cầu thủ công khai, không cookie, trực tiếp Render. Sau từng request đọc `performance_schema.events_statements_history`, chỉ lấy thread có connection attributes **Connector/J**, digest chuẩn hóa và timer MySQL. Không dùng thời gian SQL từ connection PC. Lịch sử này chỉ giữ ít event và có thể xen kẽ health/session cleanup; không suy ra tổng số SQL HTTP từ mọi event xuất hiện cùng khoảng thời gian.

| Workload mùa 2026/27 | SELECT nghiệp vụ quan sát/request | Thời gian MySQL quan sát |
| --- | --- | --- |
| CLB, 20 kết quả | 1 | 0,79–1,51 ms; median 1,06 ms |
| Cầu thủ: vị trí đủ điều kiện | 1 | 3,93–5,63 ms; median 4,37 ms |
| Cầu thủ: membership, profile, tổng bàn/kiến tạo; 534 kết quả | 1 | 25,07–66,67 ms; median 26,19 ms |

Một physical thread Connector/J được dùng lại cho nhiều request liên tiếp: pool có tái sử dụng kết nối. Trong cửa sổ quan sát cũng có khởi tạo connection mới; chưa đo được riêng thời gian TLS/chờ pool. Thống kê digest toàn database còn chứa thao tác backup/đối chiếu từ PC, nên **không dùng digest toàn database để đại diện chính xác cho Render**.

Tổng hai SELECT cầu thủ có median 30,69 ms trong 5 request. Đây vẫn chỉ là timer MySQL, không phải tổng API. Năm GET health trực tiếp Render trả `UP`; không quan sát được SELECT1 mới trong statement history (health/driver có thể dùng ping). Vì vậy SELECT1, thời gian borrow và round trip JDBC từ Render vẫn cần probe mới, không lấy con số SELECT1 trong digest toàn server làm baseline Render.

`EXPLAIN ANALYZE` đọc trên Aiven cho SQL danh sách cầu thủ: khoảng 33 ms, dùng khóa chính ở membership/player/stats/profile; phần tổng hợp đọc 2.000 dòng thống kê trận. Thử thêm lọc mùa sớm cho ra khoảng 40 ms ở một lượt; không coi đó là tối ưu và không sửa SQL. Chưa có lý do thêm index production.

## Kết quả rà code

- CLB: 1 SELECT; danh sách cầu thủ 2026: 2 SELECT. Vị trí và membership đã gom; không gọi membership riêng từng cầu thủ.
- Fantasy đội đã tồn tại: 3 SELECT nghiệp vụ (`fantasy_entries`, draft, submitted) trong transaction read-only / repeatable read. Controller còn 1 SELECT tài khoản. Session/security thêm truy vấn ngoài các số trên.
- Minigame current đang chơi: tài khoản ở controller, lock account, tìm ván daily cũ, tìm current, question snapshot và guesses. Thường 6 SELECT nghiệp vụ khi không có ván hết hạn; xử lý ván hết hạn có thể tăng SQL và ghi ledger. Không tự động gọi current/history/leaderboard production để benchmark vì GET này có thể ghi dữ liệu.
- Minigame history có 2 SELECT bổ sung/ván (`question`, `guesses`) trong vòng lặp `view()`: **N+1 thật trong code**, nhưng chưa đo request history Render nên chưa thay hành vi trong lượt chuẩn bị này. Có thể gom question/guesses theo tập game ID nếu số đo cho thấy history chậm.
- Player detail hiện tải danh sách rồi lọc Java: đọc dư dữ liệu, không phải N+1. Chỉ đổi sau khi đo endpoint detail nếu nó là nút thắt thực tế.
- Không cache tài khoản, đội Fantasy, deadline, session hoặc tiến trình chơi.

## Bộ đo tùy chọn

Không thêm dependency. Khi bật, `MeasuredDataSource` đo các JDBC call của request trong JVM. Filter chạy trước Spring Session và Spring Security, để tính cả session lookup/save trong tổng request. Mặc định không đăng ký filter/decorator, endpoint giữ bị chặn bởi security hiện có.

Biến mới, **chưa tự thay environment Render**:

| Tên | Mặc định / mục đích |
| --- | --- |
| `PREMIERHUB_READ_LATENCY_ENABLED` | `false`; bật `true` để ghi số đo GET vào log `READ_LATENCY` |
| `PREMIERHUB_READ_LATENCY_PROBE_ENABLED` | `false`; bật `true` để cho phép workload SELECT riêng của operator |
| `PREMIERHUB_READ_LATENCY_KEY` | Không có mặc định; secret riêng 32 byte, biểu diễn 64 ký tự hex; khác proxy secret |

Probe cần đồng thời operator key và proxy secret hiện có. Không cho frontend/Vercel biết operator key; không sửa proxy hoặc công khai Actuator metrics. Probe chạy trước session filter, không đăng nhập/khởi tạo session, không trả tài khoản, đội hình, tiến trình, đáp án, SQL/parameters hoặc thông tin kết nối. Chỉ nhận GET và tên workload cố định; tối đa một probe chạy cùng lúc. Khi probe bật mà key cấu hình sai, backend từ chối khởi động.

Log chỉ chứa route chuẩn hóa, HTTP status, các số đo và fingerprint hash SQL. Không có query string, account ID, game ID, token/cookie, parameter, đáp án, credentials hoặc SQL text. Fingerprint lặp phản ánh cùng **hình dạng SQL**, chưa khẳng định parameters giống nhau.

| Trường | Ý nghĩa / giới hạn |
| --- | --- |
| `apiMs` | Log GET thường: từ lúc vào filter đến sau session/security/controller/serialize response. Không gồm network PC→Vercel→Render, hàng đợi trước servlet hoặc cold start. Probe: thời gian thực hiện workload trước serialize báo cáo; **không đại diện tổng API riêng của tài khoản**. |
| `connectionMs` | Tổng `getConnection`: pool wait, validation và tạo connection mới/TLS nếu có. Không thể tách ba phần này chỉ từ tổng này. |
| `sqlExecuteMs` | Tổng JDBC execute từ Render, gồm round trip/driver và MySQL; **khác** timer server-only của performance_schema. |
| `sqlFetchMs` | Thời gian `ResultSet.next`; driver có thể đã buffer rows trong execute. |
| `jdbcControlMs` | `setAutoCommit`, read-only/isolation, commit/rollback, đóng connection. Đây cũng có thể là các round trip. |
| `statements` / `sessionStatements` / `executeCalls` | Số statement logic (prepared batch đếm từng item), phần SQL Spring Session, và số lần gọi JDBC execute. Không gồm SQL driver nội bộ/COM_PING; driver có thể gộp batch hoặc chạy các lệnh nội bộ. Phải dùng performance_schema để đối chiếu số statement thực thi phía server. |
| `acquisitions` / `reusedConnections` | Số borrow và số lần gặp lại physical connection đã thấy trong các trace trước. Lần đầu thấy không có nghĩa chắc chắn vừa tạo mới. |
| `queries` | Hash SQL + loại, số execute và thời gian cộng. Cùng hash nhiều lần giúp phát hiện vòng lặp/N+1 để rà code. |

Instrumentation có chi phí đo/reflection; so trước/sau phải dùng cùng bộ đo và cùng workloads, warmup, tập dữ liệu/thời điểm. Không lấy số H2/local thay số Render.

## Phát hành bản đo và thu baseline

1. Người dùng tự review/commit/push bản đo nếu đồng ý; Render hiện auto-deploy off nên cần deploy đúng commit trên service hiện tại. Không đổi JDBC/TLS hoặc proxy Vercel. Đợt này chưa tự commit/push/deploy.
2. Đã chuẩn bị `backend/secrets/read-latency.json` trong Git ignore, gồm `operator_key` mới và `proxy_secret` khớp Render, không in giá trị hoặc đổi environment. Kiểm tra `git check-ignore backend/secrets/read-latency.json`. Nếu cần tạo lại, dùng key `secrets.token_hex(32)`, khác proxy secret. Không gửi các giá trị trong chat hoặc đặt trên command line. Lưu operator key mới vào Render qua Dashboard; bật hai biến `...ENABLED=true`. Không in env/log chứa secret.
3. Từ root repo chạy:

   ```powershell
   python backend/scripts/measure-render-read-latency.py --label before --samples 5
   ```

   Script chỉ gửi các workload SELECT trên Render, không có cookies. Nó lấy **timings trả từ JVM**, không tính elapsed của PC. Mỗi workload có 1 warmup riêng và 5 mẫu; p95 với 5 mẫu là max, không phải p95 tin cậy cho tải thực tế. Lưu `backend/target/read-latency/before.json` ngoài Git. Nếu service đang ngủ, warmup có thể timeout; đợi health UP rồi chạy lại, không coi cold start là DB latency.

4. Workloads: `select1`, CLB và cầu thủ dùng chính `FootballQueries`; Fantasy lấy 1 entry hiện có rồi gọi `FantasyEntryService.read` trong read-only repeatable read (**thêm 1 SELECT chọn mẫu**); Minigame **projection SELECT-only**, gồm chọn mẫu + account + game/question/guesses trong cùng transaction, không gọi service expiry/FOR UPDATE. Đây không phải toàn bộ current endpoint, không đo lock/expiry/session của người chơi. Không có entry/game thì dừng, không tự tạo dữ liệu thử. Đo API riêng thật bằng log thụ động khi người dùng sử dụng bình thường, không lấy projection làm số đo full API.
5. Đối chiếu log `READ_LATENCY` của GET đội Fantasy/current Minigame trong thao tác sử dụng bình thường với probe/SELECT 1. Không tự mở ván hoặc tự gọi các GET có thể expire ván trong production. Không đưa session/token vào script.
6. Chốt đúng Aiven Cloud/Region từ Overview. Đánh giá connection time warm/reuse, JDBC execute so với timer server, SQL count và transaction control trước khi sửa. Không giả định khác region là nguyên nhân duy nhất.
7. Nếu SELECT1 JDBC nhanh, application SQL/server chậm: EXPLAIN/index/overfetch. Nếu nhiều execute nhỏ: gom dữ liệu, giữ snapshot/ownership/deadline/transaction. Nếu warm SELECT1 JDBC lớn nhưng server-only nhỏ: ưu tiên giảm round trip, xem driver/region/pool; không cache dữ liệu riêng để che vấn đề. Nếu chỉ cold borrow chậm: xem vòng đời pool trong giới hạn Aiven; không tự nâng gói hoặc chuyển hosting.
8. Sửa đúng nguyên nhân, chạy test liên quan, user tự phát hành bản sửa; chạy `--label after` với cùng phương pháp. Báo median, sample max/p95 nhỏ và SQL counts trước/sau. Nếu cần index/schema, chuẩn bị migration riêng + tác dụng/EXPLAIN, chưa chạy production.
9. Sau đo, tắt `PREMIERHUB_READ_LATENCY_ENABLED` và `PREMIERHUB_READ_LATENCY_PROBE_ENABLED`, xóa operator key ở Render và file tạm. Deploy lại cấu hình nếu Render yêu cầu. Không thay auth/session/Fantasy/Minigame hoặc dữ liệu.

## Kiểm thử local

Java 21 target; Maven đang chạy JDK 26. Đã chạy đúng nhóm bị ảnh hưởng, không chạy toàn suite:

```powershell
cd backend
mvn "-Dtest=ReadLatencyTest,ReadLatencyContextTest,RenderConfigurationTest,FantasyEntryIntegrationTest,PlayerGuessIntegrationTest,PlayerControllerTest,ClubControllerTest" test
```

**65 test đạt**, không failure/error/skip. Bộ đo được kiểm tra trong Spring Boot context bật instrumentation, kiểm tra tái sử dụng physical connection Hikari, exception JDBC, commit/rollback, fingerprint/session SQL và từng item trong prepared batch, key/bypass/POST protection, bỏ game ID khỏi route, và projection Minigame không đổi tiến trình. Các test cũ xác nhận hành vi khi mặc định tắt. Không sửa quy tắc/deadline hoặc mùa 2024/25.

## File để review/phát hành bản đo

Chỉ các source/config/tài liệu sau, không đưa `backend/secrets/` hoặc `backend/target/` vào Git:

- `backend/src/main/java/com/premierhub/diagnostics/`: `MeasuredDataSource.java`, `ReadTrace.java`, `ReadLatencyConfiguration.java`, `ReadLatencyFilter.java`, `ReadLatencyProbe.java`.
- `backend/src/main/resources/application.properties`.
- `backend/src/test/java/com/premierhub/diagnostics/`: `ReadLatencyTest.java`, `ReadLatencyContextTest.java`.
- `backend/scripts/measure-render-read-latency.py`.
- `docs/render-aiven-read-latency.md`.

Commit message đề xuất: `feat(diagnostics): measure Render database read latency`. Chưa thực hiện commit/push hoặc thay environment/deployment Render. Các file frontend, proxy Vercel, Docker/TLS/pool, nghiệp vụ và SQL migrations giữ nguyên.

Nguồn: [Render Free không có SSH/shell/one-off jobs](https://render.com/docs/free), [latency metrics tích hợp cần Pro workspace](https://render.com/docs/service-metrics), [HTTP request logs cần Pro workspace](https://render.com/docs/logging). Vì bản Render hiện tại chưa có bộ đo, không thể báo connection/JDBC/full API trước/sau chỉ từ code local hoặc timer MySQL.

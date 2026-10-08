# Render/Aiven — cutover cuối ngày 08/10/2026

Người dùng cho phép dừng riêng backend Railway, giữ MySQL nguồn chạy, suspend Render trong restore, backup đầy đủ mới ngoài Git, thay bản diễn tập Aiven và chuyển traffic Vercel. Không commit/push, xóa Railway hoặc sửa luật/deadline Fantasy. Mọi thời gian dưới đây dùng Asia/Ho_Chi_Minh nếu không ghi UTC.

## Chuẩn bị trước bảo trì

- Render deployment `dep-db3qliijnfac738efoo0` live lúc 21:40, SHA `61de7ae015756c7fc1d3d6d8538746a6cc8f7363`. Cấu hình runtime và CA đã đọc qua API: prod,render; đúng Aiven/defaultdb; VERIFY_IDENTITY không bị override; SQL/session init never; Minigame bật, dữ liệu local tắt.
- Google Client ID **và Secret** khớp Railway trong bộ nhớ. Public origin `https://premierhub.vercel.app`, callback `/api/auth/google/callback` cùng domain, giữ nguyên credentials Google.
- Project Vercel `premierhub`, Root Directory `frontend`, Node 24.x. Secret proxy production là biến sensitive, API không trả giá trị; GET cùng origin `/api/auth/me` trả 401 và vượt qua gate proxy. Render dùng secret khớp nguồn. Không in giá trị secret.
- Deployment Vercel gốc để quay lại: `dpl_3GvyzHMFiHAZUsxsGQ4Xs9kMiiCd`, commit SHA trên; proxy Railway xác nhận từ cấu hình của đúng commit deployment.
- Bản frontend trỏ Render đã build READY: `dpl_CvEfnDgLT7nFoethVNDsLiAkPr3H`. Bản bảo trì production đã build READY: `dpl_EnRxyrYTXvK4LryxW4DD733T8WdF`, không gọi backend, trả 503/no-store/Retry-After cho API và trang thông báo bảo trì. Các bản này không được build lại trong cửa sổ sao chép.
- Khi stage production với autoAssignCustomDomains=false, Vercel vẫn tự gán alias phụ `premierhub-cuong-pl.vercel.app` cho bản frontend Render; đã trả alias đó về deployment gốc. Domain chính `premierhub.vercel.app` không đổi trong bước này. Bản bảo trì preview ban đầu không promote trực tiếp được (API 422); đã dựng lại production và kiểm tra READY trước bảo trì, không dừng backend khi gặp lỗi staging. Gate cuối kiểm tra cả ba alias cùng deployment nguồn trước bảo trì.
- Các alias giữ nguyên tên: `premierhub.vercel.app`, `premierhub-cuong-pl.vercel.app`, `premierhub-git-main-cuong-pl.vercel.app`. Khi bảo trì/cutover/quay lại, xác minh cả ba cùng trỏ deployment phù hợp để tránh đường phụ vẫn nhận ghi.
- Railway backend đang chạy deployment **`0c33c59d-493a-452d-97c4-3333b51fa5f9`**, SHA **`161c19f816de5bfc3b9c884c11c8d358b637b97b`**. Deployment gần nhất SHA 61de đã FAILED, vì vậy **không redeploy latest** để quay lại. Dừng đúng deployment đang chạy, không xóa service/project/MySQL; quay lại bằng `deploymentRedeploy` với ID gốc và `usePreviousImageTag=true`.
- Render auto deploy off. Không commit/push hoặc chạy importer trong cửa sổ; kiểm tra activeDeployments nguồn và connections/transactions cả hai DB trước dump/restore, đối chiếu lại fingerprint nguồn sau thao tác. Không thay cấu hình nguồn chỉ để kích hoạt một deployment mới. Sau đối chiếu và trước mở traffic, đã dùng toggle API `serviceInstanceAutoDeployUpdate(enabled:false)` cho riêng backend Railway; trạng thái trước enabled=true, sau false, danh sách deployments không đổi, không disconnect repo/áp dụng staged changes hoặc đụng MySQL. Tránh nguồn tự bật khi push sau cutover. [Railway autodeploy toggle](https://docs.railway.com/deployments/github-autodeploys).
- Schema nguồn được so với dump đã restore thành công sang MySQL 8.4.8. Kiểm tra lại deadline gần nhất trước bảo trì; không đổi deadline. Dự kiến 15–30 phút, bắt đầu trước 23:00 để tránh mốc 00:00.
- Local sửa `frontend/vercel.json` chỉ upstream thành `https://epl-personal.onrender.com/api/$1`, cập nhật kỳ vọng `frontend/src/api/deployment.test.js`; test này **2/2 đạt**, diff check đạt. Giữ `/api` cùng origin, secret transform, no-store và SPA fallback; không khôi phục VITE_API_BASE_URL. Có cấu hình quay lại ở `docs/deployment/vercel.railway.rollback.json`.
- Credentials Vercel/Render/Railway/Aiven nằm riêng trong `backend/secrets/`, Git ignore. Upload Vercel chỉ các source/build input frontend được theo dõi, bỏ test, secret, .env, node_modules và dist. Không cần user push để chuyển domain bằng API.

## Thứ tự và phương án quay lại

1. Promote bản bảo trì và kiểm tra 503; ngừng backend Railway đúng deployment gốc, suspend Render. Giữ Railway MySQL chạy. Chờ activeDeployments nguồn rỗng, Render suspended, không còn connection ứng dụng/transaction ghi.
2. Tạo `D:\File Jva\PrismaXI-backups\render-aiven-final-<thời gian>`, fingerprint toàn nguồn và dump đầy đủ mới; fingerprint sau dump phải không đổi. Backup thêm bản Aiven trước thay thế. Nếu có thay đổi nghiệp vụ ngoài bản diễn tập Aiven, báo và giữ freeze trước khi ghi đè.
3. Pin đúng UUID Railway/railway và Aiven/defaultdb; CLI đích VERIFY_IDENTITY với CA service. Xóa **chỉ 42 bảng ứng dụng đích đã xác nhận và backup**, giữ database managed; nhập byte dump mới không --force. Không xóa/reset MySQL nguồn, nhập CSV hoặc sửa dữ liệu mùa 2024/25.
4. Đối chiếu cả 42 bảng: case/tên, counts/hash rows gồm BLOB session, schema/collation/index/constraints/AUTO_INCREMENT; kiểm tra 56 FK không mồ côi. Chỉ đổi sql_mode của session đọc SHOW CREATE để chuẩn hóa cách hiển thị ANSI; không đổi global SQL_MODE.
5. Resume và warm Render trong khi Railway backend vẫn dừng, kiểm tra health/API và TLS backend. Chỉ promote bản frontend Render khi đủ gate; đối chiếu private API qua Vercel và dữ liệu công khai, giữ nguyên domain/callback.
6. Chủ tài khoản thật kiểm tra đăng nhập Google, đội Fantasy draft/submitted đã lưu và tiến trình Minigame sau tải lại. Không tự tạo tài khoản, ván hoặc đội thử.

Nếu lỗi **trước khi chuyển traffic/nhận ghi nghiệp vụ Aiven**, giữ trang bảo trì, suspend Render và khôi phục backend Railway bằng image gốc đã pin. Xác minh nguồn nguyên vẹn và health rồi promote deployment frontend gốc, chỉ một backend nhận ghi. Không xóa partial target để né lỗi, không dùng backup cũ ghi đè nguồn.

Từ lúc domain trỏ Render, phải coi Aiven **có thể đã nhận ghi mới**. Khi có lỗi: đưa lại trang bảo trì, suspend Render và giữ Railway backend dừng, backup Aiven mới, sửa đích hoặc lập quy trình chuyển ngược/reconcile. **Không đổi proxy ngay về database Railway cũ**, tránh mất đội/game người dùng vừa lưu.

Các helper/journal và báo cáo vận hành nằm ở backend/target (ignored); bằng chứng final phải chép ra thư mục backup ngoài Git. Không chạy lại toàn bộ test; không sửa backend trong lượt cutover.

## Kết quả thực hiện

- Bảo trì Vercel bắt đầu **22:19:40**. Trang chính và API trả **503**, no-store, API JSON `MAINTENANCE`; cả ba alias trỏ cùng bản bảo trì.
- Đã ngừng đúng deployment backend Railway, suspend Render; xác minh nguồn không còn active deployment, MySQL Railway vẫn SUCCESS, Render suspended và cả hai DB không còn connection ứng dụng/transaction InnoDB trước backup/restore.
- Backup cuối: **`D:\File Jva\PrismaXI-backups\render-aiven-final-20261008-222053\railway-full.sql`**, dump từ **22:21:47 đến 22:24:10**, exit 0, **42 bảng / 1.238.135 byte**, footer đầy đủ, không CREATE DATABASE/USE/DROP TABLE. SHA-256 **`86d1147c632cdff20fb2d7470ff2ee826f96aad550defc3b162a2228811f92bc`**.
- Fingerprint nguồn trước/sau dump khớp toàn bộ **42 bảng / 9.558 dòng**. Đây là backup mới sau freeze, không tái dùng file diễn tập dù counts giống nhau.
- Đã xuất `aiven-before-final.sql` cùng thư mục, đủ 42 CREATE TABLE và engine/charset options; kết thúc backup gate lúc **22:27:47**. Fingerprint dữ liệu nghiệp vụ Aiven vẫn khớp bản diễn tập, không phát hiện ghi mới ngoài hai bảng session được kiểm tra riêng. Chưa tự tạo account/game/team.
- Restore cuối bắt đầu **22:29:19**; chỉ thay các bảng ứng dụng Aiven/defaultdb đã backup, không ghi Railway. Import exit **0**, mất **122,73 giây**. Gate đối chiếu hoàn tất **22:33:37**: **42 bảng / 9.558 dòng**, 0 bảng lệch counts/hash, schema/collation/index/constraints hoặc AUTO_INCREMENT; case tên bảng khớp. Cả 56 FK đều không có dòng mồ côi. Fingerprint toàn nguồn vẫn nguyên vẹn sau restore.
- Counts quan trọng: accounts **5**, identities **4**; Fantasy entries **4**, draft/submitted picks **44/44**; Minigame games **12**, actions **73**, daily_results **1**; thống kê theo trận **2.000**, tổng mùa **656**. Hai bảng session đều **0 dòng ở snapshot cuối**, đã chuyển đủ schema và kiểm tra hash; không bỏ qua/xóa phiên để import. Tài khoản chưa có session hoạt động cần đăng nhập lại bình thường.
- Render được resume **22:34:28**, Railway backend vẫn dừng. Những GET startup đầu timeout 45 giây; retry sau startup trả health **200/UP**, Google status **200/enabled=true**, auth chưa login **401 AUTH_REQUIRED**, Minigame info **200**, timezone Asia/Ho_Chi_Minh và initialScore 100. Aiven có **1 connection Connector/J với TLS**, runtime CA/VERIFY_IDENTITY đã được kiểm chứng trước maintenance. Đây là kiểm tra sau resume/warm, không phải cam kết mọi request cold qua Vercel đều dưới 120 giây.
- Autodeploy backend Railway được tắt và đọc lại verified lúc **22:36:46**; source connection/repo và MySQL không thay đổi, không có deployment mới.
- Chuyển traffic bắt đầu **22:37:58**, cả ba alias xác nhận trỏ frontend Render lúc **22:38:07**. Khoảng bảo trì người dùng khoảng **18 phút 27 giây**. Domain/public-origin/Google callback giữ nguyên; frontend và proxy `/api` trên Vercel, backend Render → Aiven/defaultdb là đích authoritative.
- Smoke check **11 GET qua domain thật đều đạt**: clubs/standings 2024 và 2026 **200/20 đội**; players 2026 **200/534**, matches 2026 **200/380**; Minigame info 200; Google status 200/enabled; auth/me, practice/current và Fantasy/me không login **401 AUTH_REQUIRED**, chứng minh request vượt gate proxy. Các response JSON/no-store đúng kỳ vọng. Không start/guess/login/create account/team tự động.
- Postflight hoàn tất **22:39:15**: Railway backend không có active deployment, autodeploy false; MySQL Railway vẫn SUCCESS; Render running; cả ba alias Render; Vercel Root Directory/Node/deployment protection nguyên vẹn, autoAssignCustomDomains=true. Hai asset frontend công khai trả 200 và không chứa các giá trị secret DB/Google/proxy đã kiểm tra. Deadline vẫn **09/10/2026 00:00 Việt Nam**. Inventory đích **42 PK, 11 UNIQUE, 56 FK, 56 CHECK**. Không thay luật/deadline hoặc dữ liệu thống kê mùa 2024/25.
- Journal, fingerprints toàn bảng, backup/restore report, FK, runtime, staging metadata và HTTP reports đã chép vào thư mục backup cuối ngoài Git. Credentials/log private/dump/build không được Git theo dõi; không commit/push trong lượt này.
- **Còn xác minh bởi chủ tài khoản thật:** đăng nhập Google trên domain Vercel; kiểm tra Fantasy draft/submitted đã lưu; đọc và tiếp tục tiến trình Minigame rồi tải lại. Các kiểm tra tự động trên chỉ chứng minh configuration/gate/data copy, chưa thay thế OAuth round-trip hoặc thao tác có session thật.

Sau cutover Aiven có thể đã nhận ghi của người dùng: giữ backend Railway và autodeploy off. File rollback Railway chỉ là cấu hình lưu để review; **không tự promote hoặc push cấu hình đó** sau khi đã chuyển traffic. Khi lỗi, đưa lại trang bảo trì và freeze đích, backup Aiven mới rồi xử lý theo nhánh reconcile ở trên.

Render Free vẫn có trễ lúc thức sau sleep; startup đã có timeout thực tế. Khi gặp trang loading/network timeout, đợi GET API trở lại JSON trước thao tác lưu; với thao tác đã gửi mà kết quả chưa chắc chắn, đọc lại đội/tiến trình trước khi gửi lại theo cơ chế version/action ID hiện có. Không tạo cron/keep-warm hoặc đổi timeout/rules để né lỗi trong lượt này.

## File cần commit sau khi chủ dự án yêu cầu

- `frontend/vercel.json`
- `frontend/src/api/deployment.test.js`
- `docs/deployment/vercel.render.cutover.json`
- `docs/deployment/vercel.railway.rollback.json`
- `docs/render-aiven-migration.md`
- `docs/render-aiven-rehearsal-2026-10-08.md`
- `docs/render-aiven-final-cutover-2026-10-08.md`

Message: `chore(deploy): route Vercel API to Render and document Aiven cutover`.

Đây là danh sách working tree hiện có, không bao gồm credentials, dump, log hoặc build output. Commit/push không được thực hiện tự động. Vercel đang chạy bản upload/API chưa commit: **trước lần push/deploy Git kế tiếp, phải commit cả proxy Render và test tương ứng**, tránh autodeploy Vercel lấy cấu hình Railway cũ từ Git. Không bật lại backend Railway/autodeploy nguồn với database cũ.

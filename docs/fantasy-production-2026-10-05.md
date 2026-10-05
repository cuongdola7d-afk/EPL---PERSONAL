# Fantasy production — mở GW6 ngày 05/10/2026

Người dùng cho phép backup, bốn migration Fantasy và công bố riêng GW6 mùa 2026/27 với deadline/mốc roster cụ thể. Người dùng chọn ID 1 “song cock lee” để ghi lịch sử. Không commit/push, không đổi quyền tài khoản, không tạo đội thử hoặc công bố kết quả.

## Database, backup và migration

- Đọc cấu hình Railway đang lưu của cả backend/MySQL và đối chiếu host, database, username/password trong bộ nhớ; không in giá trị bí mật. Backend production `epl-personal-production.up.railway.app` dùng MySQL service `d000f42d-bf5c-40df-a0e1-eb0bc2705711`, database `railway`, MySQL 9.7.2, server UUID `8835db23-b8ca-11f1-89a0-a2aa18198d9d`; kết nối public proxy dùng TLS REQUIRED. Trước migration chưa có bảng Fantasy nào.
- Backup mới ngoài Git: `backend/local-backups/fantasy-production-20261005-141648/railway-before-fantasy-20261005-141648.sql`, **848.411 byte**, SHA-256 `ed72f28ebda33da147f10f14164b3d8f6a40970db78893a208e581a999ba20ba`.
- Dump single-transaction hoàn tất exit 0, có footer hoàn tất và đủ CREATE TABLE cho 26 bảng hiện có. Đã xác minh trước mọi ghi migration; chưa thử restore. Lần chuẩn bị trước đó thất bại vì mysqldump không nhận connect-timeout, chưa ghi migration; đã bỏ riêng tham số đó khỏi lệnh dump để tạo backup hoàn tất trên.
- Áp dụng đúng nội dung migration đã commit theo thứ tự:
  1. `backend/sql/2026-10-05-fantasy-gameweeks-mysql.sql`
  2. `backend/sql/2026-10-05-fantasy-entries-mysql.sql`
  3. `backend/sql/2026-10-05-fantasy-roster-reference-mysql.sql`
  4. `backend/sql/2026-10-05-fantasy-results-mysql.sql`
- Tạo đủ chín bảng mới; bảng hiện có được giữ. Chạy lại bốn migration xác nhận schema không đổi và cả chín bảng trống trước khi mở GW6. Không seed H2, xóa/reset, nhập thống kê, sửa roster/membership hoặc dữ liệu mùa 2024/25. SQL init production vẫn tắt.

## Công bố GW6

Mốc `2026-10-05` có 534 cầu thủ roster hiệu lực thuộc 20 CLB. Lịch có đủ 10 kickoff UTC, trận sớm nhất Arsenal–Leeds, fixture `1000560593`, `2026-10-10T11:30:00Z` = 10/10/2026 18:30 Việt Nam. Service tính đúng deadline theo ngày Việt Nam:

- deadlineUtc: **2026-10-08T17:00:00Z** = **09/10/2026 00:00 Asia/Ho_Chi_Minh**.
- rosterAsOf: **2026-10-05**.
- deadlinePublishedAt: `2026-10-05T07:20:55.666694Z`.
- workflow/status: **OPEN**, canEdit **true**, revision **1**.
- Một dòng `fantasy_deadline_changes`: old deadline NULL, new deadline trên, changed_by **1**, lý do mở production theo xác nhận chủ dự án.

Production chưa có ADMIN. Đã dùng helper vận hành một lần, gọi **GameweekService.publishDeadline** và repository hiện có trong TransactionTemplate, kiểm tra đúng UUID, không có cuộc thi trước đó, đủ lịch, deadline tương lai và roster hợp lệ trước ghi. Không tự tạo endpoint công khai, giả mạo phiên người chơi, cấp ADMIN hoặc sửa bảo vệ API. Tài khoản ID 1 và 2 vẫn USER. Các lần quản trị tiếp theo qua website/API ADMIN cần quy trình cấp quyền được chủ dự án cho phép riêng.

Sau commit transaction, đọc lại xác nhận chỉ có GW6 và audit revision 1, không có entry hoặc result publication. Không mở GW khác/công bố điểm.

## Deployment và kiểm tra thật

- Railway deployment `e329226b-4b0c-41f2-aa1a-0ee159d77d0e`, SUCCESS, commit `14440ae730bf0ba2cdf0272979ea32ba9aa3f9ce`. Backend health 200/UP. Không sửa mã nên không chạy lại test/build local; không cần redeploy.
- Qua origin `https://premierhub.vercel.app`: overview, GW6, roster và leaderboard GW6 đều **200/no-store**. Overview khuyến nghị GW6; GW6 trả OPEN/canEdit=true/deadline/roster đúng. Roster trả 534 người, 531 có OVR/vị trí cơ bản hợp lệ; ba người thiếu dữ liệu vẫn bị luật chọn loại, không tự bổ sung.
- `/api/auth/me` và `/api/fantasy/2026/me/gameweeks/6` khi chưa đăng nhập trả **401 AUTH_REQUIRED**, không lộ đội riêng. CLB và lịch GW6 tiếp tục 200.
- Chrome riêng trên website production, chưa đăng nhập: desktop 1440px/390px đều có 11 ô, mở danh sách LW có 68 ứng viên, không tràn ngang/lỗi JavaScript. Không chọn/lưu đội production; kiểm chứng này chưa xác nhận phiên người chơi thật.
- Mã backend đã phát hành vẫn lấy chủ từ session; assertion account ID phát hiện tab cũ. Validator đọc rosterAsOf database, kiểm tra đúng 11 ID/vị trí, tối đa 3 mỗi CLB/860 OVR; transaction khóa GW/account/entry, expectedVersion chống ghi đè, Clock sau khóa/trước và sau ghi; snapshot đội đã lưu giữ nguyên. Đây là đối chiếu code đã có, không phải thử gửi đội sai lên production.

## Thử bằng tài khoản thật — đã đối chiếu

1. Mở `https://premierhub.vercel.app/#fantasy`, đăng nhập, reload và chọn GW6. Cần thấy OPEN/deadline 09/10 00:00 và danh sách chọn.
2. Chọn đủ 11 người hợp lệ, bấm **Lưu đội hình**; cần thấy Đã lưu và thời điểm lưu.
3. Sửa một cầu thủ; trước khi lưu mới đội dự thi trước vẫn có hiệu lực. Lưu lại rồi reload; sân phải giữ đội mới.
4. Báo bước đã đạt/lỗi để đọc lại đúng ID 1/GW6: version/submitted_version, 11 ID duy nhất, tổng OVR, số người mỗi CLB, quyền vị trí snapshot và submitted_at trước deadline. Không gửi mật khẩu/cookie/token.

Người dùng đã xác nhận **lưu lại và reload đúng đội**. Đọc riêng ID 1/2026/GW6 sau phản hồi xác nhận:

- version/submitted_version **2/2**, sơ đồ **4-2-1-3**, 11 ID duy nhất.
- Tổng OVR metadata và snapshot cùng **850**, dưới 860; CLB đông nhất **3** người.
- Đúng toàn bộ 11 slot của sơ đồ, required_position khớp slot và nằm trong eligible_positions của từng snapshot; không có vị trí sai.
- Nháp hiện tại khớp đúng slot/player_id của đội đã chốt lần hai.
- submitted_at `2026-10-05 07:24:58.826386 UTC` = **05/10/2026 14:24:58 Việt Nam**, trước deadline đã công bố.
- Không có GW khác hoặc result publication. Đội trên do chính người dùng thao tác, không phải seed/helper tạo.

Luồng UI hiện có một nút **Lưu đội hình** theo quyết định trước đó: lưu hợp lệ chính là chốt/thay thế đội dự thi. API `/draft` vẫn tương thích cho nháp thiếu người nhưng website không có nút nháp riêng. Xác nhận production dựa trên thao tác người dùng và đọc database đúng phạm vi; chưa thử gửi đội sai/hết hạn lên production hay thay Clock production. Không gọi chuỗi nút “nháp → chốt” tách riêng là đã kiểm chứng trên UI.

Helpers/report/ảnh riêng nằm `backend/target/fantasy-production-release`, được ignore; backup chứa dữ liệu riêng tư, không đưa vào Git. Chỉ cần commit tài liệu checkpoint và kế hoạch. Commit message đề xuất: `docs(fantasy): record production migrations and GW6 opening`.

# Minigame production — 08/10/2026

Minigame **Đoán cầu thủ** đã bật tại <https://premierhub.vercel.app/#minigame>. Người dùng giao hoàn thiện trên web sau lượt chuẩn bị, xác nhận riêng việc xuất backup đầy đủ production ra ngoài repo/thử restore local, và xác nhận **chơi được bằng tài khoản thật, reload giữ đúng tiến trình** sau deployment. Hai trò còn lại trong mockup vẫn là “Sắp ra mắt” theo phạm vi triển khai đã chốt.

## Backup và restore

Đối chiếu cấu hình backend/MySQL Railway trong bộ nhớ: database `railway`, MySQL **9.7.2**, server UUID `8835db23-b8ca-11f1-89a0-a2aa18198d9d`. Không in credentials, session, email, identity hoặc đáp án. Trước phát hành có 35 bảng InnoDB, chưa có bảng Minigame, feature chưa bật.

Backup mới được người dùng cho phép ở:

```text
D:\File Jva\PrismaXI-backups\minigame-production-20261008-152810\railway-before-minigame-20261008-152810.sql
```

- 868.459 byte; SHA-256 `1acc85f0078bfe3a459008d80bc891574b028677fcaa36cc14b78687848c9b8f`.
- Dump single-transaction, routines/triggers/events và binary dạng hex, đủ CREATE TABLE cho 35 bảng và footer hoàn tất; exit 0. File nằm ngoài repository.
- Restore thành công toàn bộ 35 bảng trên MySQL 9.6 mới, bind `127.0.0.1:33029`, datadir riêng `backend/target/minigame-production-release/restore-mysql`; không sử dụng database/service local hiện có.
- So số dòng và SHA-256 từng dòng của mọi bảng ổn định: dữ liệu khớp production. `SPRING_SESSION*` được restore nhưng không yêu cầu hash bằng snapshot đọc sau dump vì phiên có thể cập nhật/hết hạn hợp lệ trong thời gian đó. Không dùng phiên được restore để đăng nhập hoặc mạo danh người chơi.
- Server restore đã dừng. Dump và clone chứa dữ liệu tài khoản/phiên nên giữ riêng, không đưa vào Git hoặc public assets.

## Migration và nguồn đáp án

Áp dụng đúng [migration đã commit](../backend/sql/2026-10-08-player-guess-mysql.sql), SHA-256 `4bcf0fc12cee37d0874cc09c03639a6041e8095ef7aaf9b72065ea75522cc179`. Tạo đủ bảy bảng `player_guess_*`; chỉ seed selector mùa 2026 với chu kỳ rỗng. Không tạo câu hỏi, ván, kết quả, tài khoản thử hoặc pool mockup.

Lần đối chiếu đọc đầu tiên sau DDL hết thời gian chờ 50 giây qua proxy MySQL. Feature giữ tắt, DDL đã hoàn tất. Tiếp tục từ baseline đã lưu với thời gian chờ đọc dài hơn; không DROP/reset/tạo lại bảng. Đối chiếu đạt, rồi chạy lại migration lần thứ hai: schema và dữ liệu Minigame không đổi.

So fingerprint schema và dữ liệu trước/sau migration xác nhận mọi bảng ổn định đã có được giữ nguyên, gồm accounts, identity, Fantasy và bóng đá mùa **2024/25**. Chỉ loại phiên đăng nhập khỏi phép so dữ liệu vì có ghi tự nhiên của ứng dụng. Migration không ALTER/UPDATE các bảng đó.

Preflight production tại ngày Việt Nam 08/10/2026:

| Chỉ tiêu | Số lượng |
| --- | ---: |
| Cầu thủ active / danh sách chọn đoán | 534 |
| Đủ điều kiện làm đáp án | 364 |
| Membership active bị trùng | 0 |
| Thiếu OVR | 3 |
| OVR dưới 75 | 167 |
| Thiếu trường gợi ý khác | 2 |

Nguồn runtime là membership/hồ sơ/vị trí thật league 39, season 2026; đáp án cần OVR 75–99, membership hợp lệ duy nhất và đủ trường gợi ý. Không nhập lại CSV hoặc pool local vào production, không bù NULL để ép dữ liệu hợp lệ. Các nhóm thiếu dữ liệu trong bảng không nhất thiết tách rời nhau; xem điều kiện đầy đủ ở [hướng dẫn phát hành](player-guess-minigame-release.md).

## Cấu hình và deployment

Chỉ đổi hai biến Railway: `PREMIERHUB_MINIGAME_ENABLED=true`, `PREMIERHUB_MINIGAME_LOCAL_DATA_ENABLED=false`. Đọc lại xác nhận các biến khác được giữ nguyên. Profile `prod`, SQL/session schema init `never`, JDBC TLS REQUIRED và UTC, `-Duser.timezone=UTC`, proxy/cookie/Google hiện có được giữ.

- Backend deployment **`0c33c59d-493a-452d-97c4-3333b51fa5f9`**, **SUCCESS**, commit **`161c19f816de5bfc3b9c884c11c8d358b637b97b`**, xác nhận lúc `2026-10-08T08:44:19Z` (15:44 Việt Nam).
- Frontend Vercel đã deploy SUCCESS cùng SHA trước khi bật feature, deployment `72PF4J5pbwAanYXp96fgXjbCkKJz`; đã kiểm tra bản đang phục vụ web. Không tạo thêm deployment frontend vì bundle không đổi.
- Thứ tự thực tế: backup → restore cô lập → migration/đối chiếu/chạy lại → bật feature và redeploy backend → kiểm tra frontend cùng SHA → tài khoản thật. Frontend đã lên trước nhưng Minigame chỉ hoạt động sau khi backend đủ schema/config.
- Không sửa mã nghiệp vụ, POM, frontend hoặc thêm dependency; không chạy lại toàn bộ test/build local. Railway thực hiện build khi deploy. Các test liên quan của lượt chuẩn bị đã đạt như [biên bản chuẩn bị](player-guess-minigame-release.md); không báo các thất bại có sẵn của toàn suite đã được sửa.

## Kiểm tra trực tiếp

Qua frontend Vercel, trừ health gọi trực tiếp Railway:

- Health **200/UP**, clubs/players mùa 2026 và Fantasy overview/GW6 **200**.
- Minigame info và leaderboard **200/no-store**. Info: season 2026, timezone `Asia/Ho_Chi_Minh`, 100 điểm, 3 lượt, **3 gợi ý mặc định**. `nextDailyAt=2026-10-08T17:00:00Z`, tức **09/10/2026 00:00 giờ Việt Nam**. BXH ban đầu rỗng, không tạo kết quả mẫu.
- Private Minigame/Fantasy qua frontend khi chưa đăng nhập **401**; private Minigame gọi trực tiếp Railway thiếu proof proxy **403**.
- CSRF/session ẩn danh hợp lệ; cookie Secure/HttpOnly/host-only/Path=/SameSite=Lax, no-store. POST thiếu CSRF **403**; guest có CSRF vẫn **401**, không tạo ván. Logout riêng phiên ẩn danh của kiểm tra **204**.
- Chrome riêng ở 1440px và 390px: hub/countdown, chọn luyện tập, yêu cầu đăng nhập, hộp tài khoản và BXH tải được; không tràn ngang, không có uncaught JavaScript exception hoặc response 5xx trong các màn hình đã kiểm tra. Không POST Minigame từ trình duyệt guest, tạo tài khoản giả hoặc dùng phiên người khác.
- Người dùng xác nhận **“Chơi được, tải lại giữ đúng tiến trình”** sau yêu cầu dùng tài khoản thật bắt đầu practice ở 100 điểm/3 gợi ý, mở thêm một gợi ý rồi reload kiểm tra giữ 90 điểm và gợi ý đã mở. Đây là xác nhận qua người dùng, không phải automation đăng nhập. Không yêu cầu hoặc lưu mật khẩu/token của người dùng.
- Đối chiếu chỉ đọc MySQL lúc `2026-10-08T08:52:20Z`: có practice state liên kết ván `PRACTICE` được lưu trong database production. Đọc lại feature/local-bootstrap và deployment xác nhận cấu hình đúng/SUCCESS; leaderboard vẫn trả 200. Không đọc hoặc in đáp án, account ID hay session của người chơi.

Giới hạn kiểm chứng: chưa quan sát production thực tế qua mốc 00:00, phối hợp restart với một ván thật, hoặc nhận xác nhận hoàn tất daily/ghi điểm BXH thật. Luật, daily ledger, persistence sau mở lại service và đổi ngày đã được kiểm tra trên MySQL cô lập ở lượt chuẩn bị. Không thay đồng hồ production hoặc tạo điểm giả để thử; không khẳng định Google OAuth thật đã được kiểm tra chỉ từ xác nhận đăng nhập bằng tài khoản.

Helpers, báo cáo số liệu, logs, screenshots, profile Chrome và datadir restore nằm trong `backend/target/minigame-production-release/`, được Git ignore. Không commit/push trong lượt phát hành này. Các thay đổi tài liệu hiện tại chỉ ghi biên bản, không cần redeploy mã ứng dụng.

Nếu cần rollback: tắt feature và redeploy bản backend ổn định; giữ bảy bảng và lịch sử đã chơi. Không DROP bảng hoặc restore toàn bộ backup vào production khi chưa có đánh giá các ghi phát sinh và quyền khôi phục riêng.

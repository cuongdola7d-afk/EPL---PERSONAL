# Mục lục tài liệu PremierHub / PrismaXI

> Rà soát tài liệu/code/config local ngày **10/11/2026**. Đây là mục lục theo công việc, không phải xác nhận mới về production. Các file được giữ nguyên vị trí; không chạy lệnh nhập/release chỉ vì gặp trong tài liệu.

## Cách đọc trạng thái

- **Hiện hành**: hướng dẫn/contract đang được dùng, vẫn đối chiếu code/config trước thao tác.
- **Hỗn hợp**: có cơ chế còn dùng và checkpoint/đề xuất cũ; đọc mục cập nhật liên quan, giữ ngày và nguồn của số liệu.
- **Lịch sử**: biên bản hoặc bài học tại một mốc; có thể vẫn cần để truy vết, hiểu code hoặc lên kế hoạch vận hành mới.

Tên có chữ plan/release hoặc ngày cũ không tự quyết định tài liệu hết hiệu lực. Ví dụ auth release, migration Render/Aiven và kết quả Fantasy vẫn chứa hướng dẫn cần dùng. Ngược lại, câu “chưa triển khai” ở checkpoint cũ không phủ nhận code hoặc checkpoint mới. Code local không chứng minh đã deploy.

## Bắt đầu

| Đọc gì | Mục đích / trạng thái |
| --- | --- |
| [README dự án](../README.md) | Hiện hành: giới thiệu, kiến trúc, local, test/build và console |
| [AGENTS.md](../AGENTS.md) | Hiện hành: cộng tác, luật dữ liệu và ranh giới quyền thao tác |
| [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md) | Hỗn hợp: hồ sơ bàn giao, code/API/schema và checkpoint có ngày/nguồn |
| [LEARNING_TASKS.md](LEARNING_TASKS.md) | Lịch sử học tập: các bài cũ đã hoàn thành, có vị trí implementation/test |
| [requests.http](../requests.http) | Bộ GET local chính đã hợp nhất: nhóm 2026/2024, validation dự kiến 400 và 404 có điều kiện; thay placeholder ID theo dataset cùng mùa trước khi dùng. Không cần body/auth/CSRF cho các mẫu hiện có |

Muốn tiếp tục một GW, đọc workflow và `sources.md` đúng GW trước. Muốn sửa sản phẩm, chọn tài liệu tính năng và code liên quan. Không tự mở lại bài tập cũ hoặc khởi chạy tác vụ production từ danh sách việc tiếp theo.

## Phát triển local

| Tài liệu | Trạng thái và cách dùng |
| --- | --- |
| [auth-local-2026.md](auth-local-2026.md) | Hỗn hợp: local email/session/CSRF/rate limit và các thay đổi proxy; phần production đọc cùng auth release/cutover |
| [player-guess-minigame-local-data.md](player-guess-minigame-local-data.md) | Hiện hành: profile `minigame-local`, H2 riêng, bảy CSV bootstrap và giới hạn snapshot local |

Lệnh nhanh ở [README — Chạy local](../README.md#chạy-local) và [Test và build](../README.md#test-và-build). Profile local không kết hợp `prod`; không sao chép credential hoặc H2 production để làm fixture. Snapshot roster local không phải dữ liệu production mới nhất. Google local có cấu hình riêng trong hướng dẫn tính năng bên dưới.

## Tính năng

| Tài liệu | Trạng thái và cách dùng |
| --- | --- |
| [club-detail-2026.md](club-detail-2026.md) | Hiện hành: trang CLB, thống kê/rating và điều hướng |
| [google-auth-2026.md](google-auth-2026.md) | Hỗn hợp: OIDC login/link, proof/callback, cấu hình và checkpoint; không gộp tài khoản chỉ theo email |
| [fantasy-lineup-validation-2026.md](fantasy-lineup-validation-2026.md) | Hỗn hợp: validator/sơ đồ ban đầu; đoạn 860/localStorage là lịch sử, luồng hiện tại xem player-flow |
| [fantasy-pitch-recovery-2026.md](fantasy-pitch-recovery-2026.md) | Hiện hành: giữ/khôi phục lựa chọn khi đổi sơ đồ |
| [fantasy-multiplayer-2026-plan.md](fantasy-multiplayer-2026-plan.md) | Hỗn hợp: luật/thiết kế nhiều tài khoản và tiến trình triển khai; không coi mọi “bước tiếp theo” cũ là chưa làm |
| [fantasy-gameweeks-2026.md](fantasy-gameweeks-2026.md) | Hỗn hợp: trạng thái/deadline/mở vòng còn dùng; thông tin GW9 ở đầu là checkpoint 26/10, không phải vòng hiện tại tự cập nhật |
| [fantasy-roster-reference-2026.md](fantasy-roster-reference-2026.md) | Hỗn hợp: rosterAsOf riêng theo GW, snapshot và khóa; migration/checkpoint theo mốc |
| [fantasy-user-lineups-2026.md](fantasy-user-lineups-2026.md) | Hỗn hợp: entry/draft/submitted, ownership/version; luồng nút lưu cuối xem player-flow |
| [fantasy-player-flow-2026.md](fantasy-player-flow-2026.md) | Hỗn hợp: luồng một nút Lưu đội hình, giới hạn 910 và thay đổi từ 860; đọc phần cập nhật cuối |
| [fantasy-results-2026.md](fantasy-results-2026.md) | Hỗn hợp, vẫn dùng vận hành: bằng chứng unrated/not-registered, readiness/hash, công bố/tái tính/BXH; các đoạn GW6 là checkpoint |
| [team-of-week-2026.md](team-of-week-2026.md) | Hiện hành: tối ưu Đội hình tiêu biểu, GW1–38, tab mặc định và độ phủ |
| [player-guess-minigame-plan.md](player-guess-minigame-plan.md) | Hỗn hợp: đặc tả đã chốt và lịch sử triển khai; tên plan không có nghĩa trò chơi chưa tồn tại |
| [player-guess-minigame-api.md](player-guess-minigame-api.md) | Hỗn hợp: contract/version/actionId/expiry và trạng thái kiểm thử theo mốc; không suy full suite hiện tại từ báo cáo cũ |
| [player-guess-minigame-frontend.md](player-guess-minigame-frontend.md) | Hỗn hợp: UI, API hooks, khôi phục tiến trình và kiểm tra đã thực hiện |

Luật hiện tại: Fantasy 2026 lấy rating người dùng cung cấp, OVR dùng cho chọn đội; điểm v1/2024 và điểm minigame là hai hệ khác. Các quyền nhập dữ liệu, mở vòng và công bố phải đọc cùng AGENTS.

## Dữ liệu

| Tài liệu | Trạng thái và cách dùng |
| --- | --- |
| [gameweek-data-workflow.md](gameweek-data-workflow.md) | Hiện hành, bắt buộc khi thu thập GW: nguồn/ID/membership, backup/import, lineup/evidence, tổng mùa/TOW/readiness |
| [player-profiles-2026-import.md](player-profiles-2026-import.md) | Hỗn hợp: header/validation/import hồ sơ và các batch cũ; không nhập snapshot cũ đè bổ sung mới |
| [2026-player-positions.md](2026-player-positions.md) | Hỗn hợp: primary/eligible, ngoại lệ đã chốt, quy trình và checkpoint nhập vị trí |
| [player-season-stats-2026.md](player-season-stats-2026.md) | Hiện hành: tổng mùa, NULL, idempotence và giới hạn validator 40/400 đang tồn tại |
| [club-information-2026.md](club-information-2026.md) | Hỗn hợp: thông tin HLV/sân, CSV/importer và nguồn theo ngày |
| [match-lineups-2026.md](match-lineups-2026.md) | Hỗn hợp: parser/role/formation/source, bộ ba importer, SQL và CSV fallback; checkpoint thiếu metadata không phải xác nhận chưa có thống kê |

Checkpoint và dữ liệu nằm ngoài `docs/`:

| Nguồn | Cách dùng |
| --- | --- |
| [GW6](../backend/data/gw6-2026/sources.md), [GW7](../backend/data/gw7-2026/sources.md), [GW8](../backend/data/gw8-2026/sources.md), [GW9](../backend/data/gw9-2026/sources.md) | Nguồn/kiểm tra/nhập theo từng vòng; có cả mốc chuẩn bị và hoàn tất |
| [GW10](../backend/data/gw10-2026/sources.md), [GW11](../backend/data/gw11-2026/sources.md) | Checkpoint bàn giao gần nhất được dẫn trong PROJECT_CONTEXT; mốc production 09/11/2026, không phải kiểm tra live ngày cập nhật mục lục |
| [Bộ hồ sơ hợp nhất](../backend/data/player-data-production-2026-10-04/README.md) | Snapshot và manifest nguồn; chữ production trong tên không tự cấp quyền ghi |
| [backend/data](../backend/data), [backend/sql](../backend/sql) | Giữ batch/nguồn/bằng chứng và migration/preflight; CSV không tự được nhập khi commit/deploy |

“Thu thập” bao gồm nhập production phần đã xác minh sau kiểm đích và backup, theo phạm vi người dùng giao. Mở GW, đổi deadline/roster, công bố/tái công bố và deploy là phạm vi riêng. Không tạo dòng giả để đủ 40/400 hoặc suy rating 0 từ NULL. Giữ nguồn/sidecar theo fixture/GW dù file chỉ có header.

## Vận hành

| Tài liệu | Trạng thái và cách dùng |
| --- | --- |
| [auth-release-2026.md](auth-release-2026.md) | Hỗn hợp, vẫn dùng: session/CSRF, proxy cùng origin, env/migration/checklist; host Railway và SHA trong checkpoint cũ phải đối chiếu cutover |
| [auth-production-2026-10-04.md](auth-production-2026-10-04.md) | Lịch sử kiểm chứng auth/cookie/proxy; hữu ích để truy vết và thiết kế smoke mới, không phải runtime SHA hiện tại |
| [auth-performance-2026-10-04.md](auth-performance-2026-10-04.md) | Lịch sử đo trên Railway; không áp dụng số đo/serverless setting đó cho Render, xem hướng dẫn latency riêng |
| [fantasy-production-2026-10-05.md](fantasy-production-2026-10-05.md) | Lịch sử release/mở GW6; còn dùng truy vết migration và kết quả lúc đó, không chạy lại để mở vòng |
| [player-guess-minigame-release.md](player-guess-minigame-release.md) | Hỗn hợp, vẫn dùng: checklist schema/flag/backup/release; đầu file cập nhật đã phát hành, các đoạn “chưa” ở cuối thuộc lượt chuẩn bị |
| [player-guess-minigame-production-2026-10-08.md](player-guess-minigame-production-2026-10-08.md) | Lịch sử production/backup/restore/smoke theo ngày; làm căn cứ feature đã được kiểm tra, không suy đã deploy code mới |
| [render-aiven-migration.md](render-aiven-migration.md) | Hỗn hợp, vẫn dùng: container/TLS/env/pool và quy trình chuyển; trạng thái “chưa chuyển” của lượt chuẩn bị đã được biên bản cuối cập nhật |
| [render-aiven-rehearsal-2026-10-08.md](render-aiven-rehearsal-2026-10-08.md) | Lịch sử rehearsal/restore/reconcile; mẫu kiểm tra cho lượt được giao, không chứng minh mọi backup sau đã thử restore |
| [render-aiven-final-cutover-2026-10-08.md](render-aiven-final-cutover-2026-10-08.md) | Hỗn hợp, vẫn dùng: mốc chuyển authoritative Railway → Aiven và các ràng buộc freeze/rollback/reconcile |
| [render-aiven-read-latency.md](render-aiven-read-latency.md) | Hỗn hợp: workload/timer/probe và phần còn phải đo/rollout; có code diagnostics không có nghĩa đã bật trên production |

[render.yaml](../render.yaml), [Dockerfile](../backend/Dockerfile) và [mẫu rewrite Vercel](deployment/vercel.render.example.json) là cấu hình/tham chiếu để đối chiếu, không tự xác nhận trạng thái dịch vụ. Thao tác release/migration/restore cần đúng phạm vi và backup; không tự tạo cron, keep-warm hoặc Pre-deploy Command. Không mở rộng quyền từ một lệnh mẫu trong tài liệu cũ.

## Lịch sử

| Tài liệu | Cách dùng |
| --- | --- |
| [phase-1.md](phase-1.md) | Java/OOP/CSV và console; còn dùng học reader/model, không đại diện kiến trúc web hiện tại |
| [PROGRESS.md](PROGRESS.md) | Các mốc đầu dự án; câu chưa có frontend/database là trạng thái lúc đó |
| [stage-2-player-api.md](stage-2-player-api.md) | Bài học API CSV/in-memory, DTO và phân lớp; luồng classpath là lịch sử |
| [stage-2-match-api.md](stage-2-match-api.md) | Bài học Match API/filter; kiểm code hiện tại trước dùng tên service/loader cũ |
| [stage-2-standing-api.md](stage-2-standing-api.md) | Bài học Standing API; không kết luận endpoint chưa có từ giai đoạn trước |
| [stage-2-api-errors.md](stage-2-api-errors.md) | Bài học validation/HTTP/error DTO còn hữu ích; kiểm exception/contract hiện tại trước sửa |
| [stage-2-production-readiness.md](stage-2-production-readiness.md) | Chuẩn bị executable JAR/deploy ban đầu; mô tả đóng gói CSV demo và chưa có DB là lịch sử |
| [gw3-2026-production-import.md](gw3-2026-production-import.md) | Biên bản nhập GW3; giữ căn cứ, không nhập lại như batch mới |
| [Phụ lục README cũ](../README.md#phụ-lục-lịch-sử-readme) | Ghi chép v1/2024, API-Football, Railway và CLI cũ được bảo tồn; một số cơ chế vẫn tham chiếu theo đúng mùa/phạm vi |

## Cập nhật mục lục

Mục lục này liên kết cả 41 tài liệu Markdown có trước đợt dọn 3; tính cả chính nó, `docs/` có 42 file Markdown tại mốc rà soát. Khi thêm hoặc đổi trạng thái một hướng dẫn, cập nhật nhóm/nhãn và PROJECT_CONTEXT tương ứng. Giữ checkpoint kèm ngày/nguồn, phân biệt kết quả local với production và không xóa lịch sử chỉ vì có tài liệu mới.

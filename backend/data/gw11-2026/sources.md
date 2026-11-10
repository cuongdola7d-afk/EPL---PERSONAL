# Fantasy GW11 mùa 2026/27 — đã mở production ngày 09/11/2026

Người dùng yêu cầu thu thập đủ GW10, bỏ qua công bố kết quả GW10 và bắt đầu GW11. Việc mở GW11 được cho phép riêng trong yêu cầu này; không cần mở bù GW10.

## Lịch và cấu hình đã xác nhận

- Đã đối chiếu đủ 10 fixture GW11 với PremierHub và endpoint riêng football-data.org; tất cả có kickoff UTC, provider TIMED/SCHEDULED và chưa bắt đầu.
- Trận đầu Manchester City–Fulham: nội bộ **1000560643**, provider **560643**, kickoff **21/11/2026 12:30 UTC = 19:30 Việt Nam**.
- Deadline theo `GameweekService.calculateDeadline`: **19/11/2026 17:00 UTC = 20/11/2026 00:00 Việt Nam**.
- Mốc roster: **09/11/2026**; API trả **564 cầu thủ duy nhất**, đã đối chiếu membership đúng ngày cho từng người. Hai hồ sơ tối thiểu mới có OVR/primaryPosition/eligiblePositions trống đúng quyền đã cho, không tự bổ sung quyền chơi Fantasy.
- Đã mở bằng `GameweekService.publishDeadline(11,2,reason,2026-11-09)` lúc UTC **09/11/2026 16:02:27.251919**, commit **16:02:28**. ADMIN ID 2 được đối chiếu role hiện hành; cấu hình + audit dùng transaction SERIALIZABLE cùng batch GW10. Trạng thái **OPEN/canEdit=true**, revision **1**. Lặp bị từ chối **409** và giữ nguyên deadline/roster/cấu hình.
- MySQL và API xác nhận recommendedGameweek=11, GW10 configured=false, không có công bố kết quả GW10/GW11. Đội nháp, đội đã chốt, cấu hình/deadline/rosterAsOf và kết quả các vòng cũ khớp snapshot trước nhập.

## Nguồn và kiểm tra

[Lịch GW11](https://premierhub.vercel.app/api/matches?season=2026&matchweek=11), [cấu hình Fantasy](https://premierhub.vercel.app/api/fantasy/2026/gameweeks), [roster GW11](https://premierhub.vercel.app/api/fantasy/2026/gameweeks/11/players).

Backup và kiểm tra H2 trước ghi dùng cùng batch với [GW10](../gw10-2026/sources.md). H2 mở đúng OPEN/canEdit, revision=1, deadline/roster; gọi lặp bị 409 và giữ cấu hình. Không tạo đội thay người dùng, không sao chép đội vòng cũ.

Chrome production không đăng nhập ở 1440px và 390px: Vòng thi mặc định **GW11**, deadline **20/11/2026 · 00:00**, roster **09/11/2026**. Tab đội tiêu biểu mặc định GW11 và vẫn chọn xem GW10/GW7; riêng GW10 có đủ 11 người và tổng 92,40 khớp API. Không tràn ngang/lỗi JavaScript/request ghi. Các ảnh/report nằm trong `backend/target/gw10-collection/`, Git ignore.

Bước tiếp: người chơi lưu đội GW11 trước deadline. Khi có fixture FINISHED và ảnh cuối, tiếp tục thu thập GW11 theo workflow. Chưa nhập thống kê hoặc công bố kết quả GW11; chưa commit/push/deploy.

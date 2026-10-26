# Fantasy GW9 mùa 2026/27 — bắt đầu ngày 26/10/2026

Người dùng yêu cầu tiếp tục dữ liệu GW8, bỏ qua công bố Fantasy GW8 vì chưa bắt đầu, rồi bắt đầu GW9. Đã mở GW9 production bằng `GameweekService.publishDeadline` hiện có, không tạo cuộc thi/kết quả GW8 hoặc công bố điểm vòng nào. Công việc nhập GW8 còn theo checkpoint riêng; phần không phụ thuộc hồ sơ mới được tiếp tục độc lập.

- Đích đã đối chiếu: Render dùng Aiven `defaultdb`, UUID `715946cc-c33a-11f1-8fd9-8e0831c19405`, TLS VERIFY_IDENTITY.
- Trước ghi đã tạo và kiểm tra backup 21 bảng: `backend/local-backups/gw8-and-start-gw9-20261026-121828/aiven-before-gw8-and-gw9.sql`, 1.002.361 byte, SHA-256 `a2adb2ccd32c1eeffb1c12feb7da70562140bd9557ab8b41fffdf9a20c0662a3`; UTC 26/10/2026 12:19:03.110865. Đủ DDL/số dòng/marker hoàn tất, được Git ignore; chưa thử restore.
- Đã xác nhận đủ 10 fixture GW9 có kickoff UTC, chưa bắt đầu; lịch được đối chiếu football-data.org. Không lấy ngày trống giờ hoặc dữ liệu một GW để giả batch mùa đầy đủ.
- Trận đầu: Chelsea FC – Manchester United FC, fixture nội bộ **1000560626**, provider **560626**, **31/10/2026 12:30 UTC = 19:30 Việt Nam**.
- Deadline đã ghi: **30/10/2026 00:00 Việt Nam = 29/10/2026 17:00 UTC**, đúng quy tắc ngày liền trước ngày trận đầu. Roster cố định **26/10/2026**, revision **1**, workflow **OPEN**.
- Mở thực tế lúc UTC **26/10/2026 12:23:55.743271**, dùng ADMIN ID 2 đã được chủ dự án cho phép từ trước và đối chiếu role hiện hành. Service ghi cấu hình + audit trong transaction SERIALIZABLE; gọi lặp bị từ chối 409 và giữ nguyên cấu hình. Không giả session hoặc sửa quyền tài khoản.
- API `GET /api/fantasy/2026/gameweeks`: `recommendedGameweek=9`, GW9 `configured=true/status=OPEN/canEdit=true`, GW8 `configured=false`; deadline/rosterAsOf đúng bản ghi. API roster GW9 ban đầu trả 559 người tại ngày 26/10. Roster có thể phản ánh membership đã xác minh bổ sung trong bước nhập GW8; không đổi mốc ngày đã chốt.
- Đã đối chiếu giữ nguyên cấu hình/lịch sử các vòng cũ, đội nháp/đội chốt và mọi kết quả đã công bố. Không sao chép đội GW7, tạo đội thay người dùng, mở bù GW8 hoặc công bố/tái công bố kết quả.
- Kiểm chứng: mở GW9 được kiểm tra trên H2 cô lập trước production; kiểm tra dữ liệu thực tế/API công khai như trên. Không thay mã ứng dụng/schema/dependency; chưa commit, push hoặc deploy.
- Chrome production không đăng nhập ở 1440px và 390px: mặc định GW9, trạng thái đang mở, hạn 30/10/2026 00:00 và mốc roster 26/10 hiển thị đúng, sân 11 ô; không tràn ngang/lỗi JavaScript hoặc request ghi. Sau batch dữ liệu GW8, đối chiếu lại cấu hình + audit GW9 giữ nguyên; đội người chơi và kết quả cũ không đổi.
- Ngày 27/10 đã hoàn tất 10/10 trận GW8 sau khi chủ dự án duyệt hồ sơ tối thiểu Teddy. MySQL và API xác nhận GW9 vẫn OPEN/canEdit=true, recommendedGameweek=9, deadline 29/10 17:00 UTC và rosterAsOf 26/10 giữ nguyên; không mở hoặc công bố Fantasy GW8. Xem checkpoint GW8 về dữ liệu, backup và kiểm chứng cuối.

Nguồn API: [lịch GW9](https://premierhub.vercel.app/api/matches?season=2026&matchweek=9), [các vòng Fantasy](https://premierhub.vercel.app/api/fantasy/2026/gameweeks), [roster GW9](https://premierhub.vercel.app/api/fantasy/2026/gameweeks/9/players). Chi tiết dữ liệu GW8 tại [checkpoint GW8](../gw8-2026/sources.md).

Bước tiếp theo: người chơi lưu đội GW9 trước deadline. Sau khi có trận FINISHED và ảnh cuối, thu thập GW9 theo workflow; ADMIN tự công bố sau khi dữ liệu đủ và readiness đạt. Hiện chưa thu thập thống kê/rating GW9 hoặc tạo bằng chứng trận chưa đá.

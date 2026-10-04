# Chi tiết CLB 2026/27

Thẻ CLB trên trang chủ mở `#clubs/<club_id>?season=2026`. Giao diện theo
`club-mockup-v2.html`: danh tính CLB, trận trước/kế tiếp, phong độ năm trận và
bốn tab BXH, Trận đấu, Cầu thủ, Thống kê. HLV/sân nhà hiện tại mùa 2026/27 được bổ sung
trong [quy trình thông tin CLB](club-information-2026.md); thiếu hiển thị “Chưa cập nhật”.

## Dữ liệu và API

- CLB, roster/membership hiện hành, hồ sơ, fixtures và BXH dùng API đã có.
- `GET /api/clubs/{id}/statistics?season=2026` là API mới, chỉ đọc dữ liệu.
- Rating của đội = tổng rating hợp lệ / số lượt `PLAYED` có rating, không phải
  trung bình không có trọng số của điểm trung bình từng người. DNP và rating NULL
  bị loại khỏi phép tính; nếu không có rating thì `averageRating: null`.
- Chỉ lấy fixture FINISHED, đúng mùa/giải/CLB và membership hợp lệ ở ngày thi đấu.
  Dùng EXISTS để membership trùng khoảng thời gian không làm nhân đôi số liệu.
- API trả rating trung bình, số lượt được chấm, số trận có thống kê và các tổng
  riêng theo player_id. Goals/assists NULL nếu có lượt PLAYED thiếu chỉ số tương ứng.
- Tổng bàn thắng/thua của đội lấy từ standings; không cộng goals cá nhân để thay
  kết quả đội, vì phản lưới và dữ liệu thiếu có thể làm lệch tổng.
- Top cầu thủ chỉ hiển thị người trong roster hiện hành; tổng của từng người chỉ
  tính khi chơi cho đúng CLB. Rating toàn đội vẫn gồm người đã rời CLB sau trận đó.
- Không thêm bảng/cột, không sửa roster, membership, dữ liệu trận, OVR hoặc Fantasy.
- Đường đọc thống kê mới chỉ hỗ trợ 2026. Trang 2024 dùng API cũ cho roster/lịch/BXH;
  thống kê rating mới và vạch nhóm thứ hạng không áp dụng cho mùa 2024.

Ví dụ response:

```json
{
  "clubId": 1000000064,
  "season": 2026,
  "averageRating": 6.975324675324676,
  "ratedAppearances": 77,
  "recordedMatches": 5,
  "players": [
    {"playerId": 123, "appearances": 2, "goals": null, "assists": 0,
     "averageRating": 7.0, "ratedAppearances": 1}
  ]
}
```

Player ID trong mục players ở ví dụ chỉ minh họa cấu trúc.

## Giao diện và điều hướng

- BXH 2026: hạng 1–5 vạch xanh dương, 6–7 cam, 18–20 đỏ theo nhóm người dùng yêu cầu.
  CLB đang xem có nền nổi bật, vạch màu không bị ghi đè. Áp dụng cả BXH trang CLB,
  trang BXH và bảng rút gọn ở trang chủ.
- Chân thuận hiển thị LEFT → Chân trái, RIGHT → Chân phải, BOTH → Hai chân.
  Dữ liệu thiếu vẫn là NULL, hiển thị — hoặc Chưa có dữ liệu.
- Chân thuận có trong roster CLB và thẻ cầu thủ dạng lưới/danh sách mùa 2026.
  Tab thông tin hồ sơ cầu thủ đã có trường này từ trước.
- URL giữ tab, lọc trận, chế độ squad/top, chỉ số ranking và số hàng đang xem.
  Từ cầu thủ/trận đấu, nút quay lại phục hồi đúng URL CLB trước đó; reload giữ trạng thái.
- Desktop và 390px đã kiểm tra; bảng/biểu đồ dài cuộn trong panel, không tràn cả trang.

## Kiểm tra và phát hành

Backend: `mvn "-Dtest=ClubStatisticsServiceTest,ClubControllerTest" test`;
3 test SQL/H2 cô lập và 8 test controller đạt. `mvn -DskipTests package` đạt.
JDK chạy Maven là 26, code biên dịch với release 21 theo POM.

Frontend: `node --test src/utils/clubView.test.js src/utils/matchRoute.test.js
src/utils/playerDetail.test.js src/utils/homeView.test.js`; 15 test đạt.
`npm.cmd run build` đạt.

Browser dùng bản sao đọc API vào H2 cô lập, gồm roster Liverpool 31 người và 5 trận
(200 dòng hai đội). Rating API khớp phép tính trực tiếp từ 77 lượt có rating.
Không ghi MySQL production. Artifact kiểm tra ở `backend/target/club-ui-check/`, không commit.

Cần phát hành cả backend (endpoint statistics mới) và frontend. Không cần migration/import SQL.
Chưa commit, push hoặc deploy tự động.

## Các file cần commit

Backend:

- `backend/src/main/java/com/premierhub/service/ClubStatisticsService.java`
- `backend/src/main/java/com/premierhub/web/ClubController.java`
- `backend/src/main/java/com/premierhub/web/dto/ClubStatisticsResponse.java`
- `backend/src/test/java/com/premierhub/service/ClubStatisticsServiceTest.java`
- `backend/src/test/java/com/premierhub/web/ClubControllerTest.java`

Frontend:

- `frontend/src/App.jsx`
- `frontend/src/api/clubs.js`
- `frontend/src/components/ClubCard.jsx`
- `frontend/src/components/ClubDetailPage.jsx`
- `frontend/src/components/ClubDetailPage.css`
- `frontend/src/components/ClubPage.jsx`
- `frontend/src/components/StandingsPage.jsx`
- `frontend/src/components/StandingsBands.css`
- `frontend/src/components/MatchPage.jsx`
- `frontend/src/components/MatchDetail.jsx`
- `frontend/src/components/PlayerCard.jsx`
- `frontend/src/components/PlayerPage.css`
- `frontend/src/components/PlayerDetailPage.jsx`
- `frontend/src/hooks/useApiResource.js`
- `frontend/src/utils/clubRoute.js`
- `frontend/src/utils/clubView.js`
- `frontend/src/utils/clubView.test.js`
- `frontend/src/utils/matchRoute.js`
- `frontend/src/utils/playerRoute.js`

Tài liệu: `docs/club-detail-2026.md`.

Commit message: `feat(clubs): add club detail pages, rating stats and preferred foot`

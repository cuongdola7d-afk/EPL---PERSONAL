# Fantasy 2026/27: chọn đúng ô và kiểm tra đội hình

## Nguồn và phạm vi

- Chỉ Fantasy 2026/27. Giữ nguyên nhóm rộng và dữ liệu mùa 2024/25.
- Đọc roster/OVR/vị trí qua `GET /api/players?season=2026&asOf=2026-10-02`.
- Giữ mốc roster `2026-10-02` hiện có ở frontend và backend. Không dùng CLB do client gửi.
- `eligiblePositions` là quyền của từng cầu thủ; không suy từ DEF/MID/FWD.
- Ô LCB/RCB dùng quyền CB, LCM/RCM dùng quyền CM. Không thêm phép đổi RW/RM.
- Thiếu OVR hoặc vị trí chính không nằm trong tập hợp: không được chọn.
- OVR 72 của Dowman đọc nguyên giá trị đang lưu. Nguồn vẫn là mức người dùng cung cấp,
  không biến thành rating EA đã xác minh. API hiện trả Mantato 62 nên dùng đúng 62.
  Wellity Lucky có OVR NULL nên vẫn bị chặn; không tự gán thay thế.
- Đội đủ 11 ID khác nhau, tối đa 3 mỗi CLB, tổng OVR không quá 860.
- OVR phục vụ điều kiện chọn đội; không cộng OVR thành điểm trận. Điểm trận vẫn từ rating đã nhập.

## Các ô đọc từ code

Mỗi hàng được đánh số từ 0; ô có khóa `hàng-cột`. Bộ sơ đồ ở
`frontend/src/fantasy/lineup.js` và bộ kiểm tra backend phải cùng khóa.

| Sơ đồ | Các hàng ô thực tế từ trên xuống |
| --- | --- |
| 4-2-1-3 | LW/ST/RW; CAM; LCM/RCM; LB/LCB/RCB/RB; GK |
| 4-3-3 | LW/ST/RW; LCM/CM/RCM; LB/LCB/RCB/RB; GK |
| 4-4-2 | ST/ST; LM/LCM/RCM/RM; LB/LCB/RCB/RB; GK |
| 3-5-2 | ST/ST; LM/LCM/CM/RCM/RM; LCB/CB/RCB; GK |

## API kiểm tra tối thiểu

`POST /api/fantasy/2026/validate`, `Content-Type: application/json`.
Request chỉ cần `formation` và `picks: {"0-0": player_id, ...}`.
Backend đọc dữ liệu bằng FootballQueries từ database cho mùa 2026, mốc roster nói trên.
OVR, CLB, vị trí frontend gửi thêm không được dùng để quyết định hợp lệ.
Endpoint chỉ đọc, không lưu đội, không thay đổi MySQL hoặc dữ liệu trận.

HTTP 200 với `valid=false` là đội chưa hợp lệ. Mỗi lỗi có `code`, `slotKey`,
`playerId`, `message`; lỗi tổng đội có slot/player NULL. Ví dụ một lỗi trong `issues`:

```json
{
  "code": "POSITION",
  "slotKey": "2-1",
  "playerId": 2000006021,
  "message": "Ô 2-1 (CB), Luke Shaw: không được chơi vị trí CB."
}
```

Đội hợp lệ: `{"valid":true,"totalOvr":856,"issues":[]}`.
Thiếu formation/picks hoặc JSON sai định dạng trả HTTP 400.
CORS cho POST ở đúng endpoint trên, giữ các API hiện hành chỉ cho GET.
Frontend chỉ báo đã xác nhận khi endpoint trả valid=true; lỗi mạng không được bỏ qua.
Kết quả kiểm tra cũ bị bỏ qua nếu lựa chọn thay đổi trong khi request đang chạy.

## Đổi sơ đồ và lưu trong trình duyệt

- Dùng ghép cặp cực đại cầu thủ–ô với đường tăng: có thể chuyển người linh hoạt
  sang vị trí phụ để giải phóng ô cho người ít lựa chọn.
- Không xếp được: giữ ID trong `unassigned`, hiển thị tên/vị trí/lý do cần thay.
  Đổi lại sơ đồ vẫn xét các ID này. Không đặt người sai ô hoặc tự xóa lựa chọn.
- Người chưa xếp cũng được tính vào số người giữ, giới hạn CLB và tổng OVR.
- localStorage giữ `formation`, `picks`, `unassigned`, `notices`; không xây tài khoản.
- Tải lại đối chiếu dữ liệu API mới. Lựa chọn sai ô, thiếu OVR/vị trí hoặc không còn
  trong roster được giữ để review; ID trùng được gộp thành một người và báo rõ.
  Đội cũ vượt giới hạn vẫn hiển thị nhưng không qua kiểm tra.

## Đội mẫu từ dữ liệu thật hiện có

Đọc API production ngày 2026-10-03 (roster asOf 2026-10-02), không ghi production:
[Roster production](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-02)

110 người có cả OVR và vị trí hợp lệ. Mỗi đội dưới đây đã được kiểm tra bằng logic
frontend và endpoint mới chạy local với H2 cô lập lấy dữ liệu từ cùng response API.
Các đội là ví dụ chứng minh có thể xếp hợp lệ, không phải đội tối ưu điểm trận.
Mỗi đội có Arsenal 3, MU 3, Liverpool 3, Man City 2.

### 4-2-1-3 — 856/860 OVR

| Ô (khóa) | Cầu thủ | player_id | CLB | OVR |
| --- | --- | --- | --- | --- |
| LW (0-0) | Mason Mount | 2000006007 | Manchester United FC | 78 |
| ST (0-1) | Joshua Zirkzee | 2000006011 | Manchester United FC | 77 |
| RW (0-2) | Victor Munoz | 2000003014 | Liverpool FC | 79 |
| CAM (1-0) | Kai Havertz | 2000001018 | Arsenal FC | 81 |
| LCM (2-0) | Manuel Ugarte | 2000006022 | Manchester United FC | 77 |
| RCM (2-1) | Wataru Endo | 2000003003 | Liverpool FC | 78 |
| LB (3-0) | Myles Lewis-Skelly | 2000001024 | Arsenal FC | 78 |
| LCB (3-1) | Jeremy Jacquet | 2000003005 | Liverpool FC | 77 |
| RCB (3-2) | Vitor Reis | 2000004013 | Manchester City FC | 76 |
| RB (3-3) | Rico Lewis | 2000004025 | Manchester City FC | 77 |
| GK (4-0) | Kepa Arrizabalaga | 2000001011 | Arsenal FC | 78 |

### 4-3-3 — 857/860 OVR

| Ô (khóa) | Cầu thủ | player_id | CLB | OVR |
| --- | --- | --- | --- | --- |
| LW (0-0) | Mason Mount | 2000006007 | Manchester United FC | 78 |
| ST (0-1) | Joshua Zirkzee | 2000006011 | Manchester United FC | 77 |
| RW (0-2) | Victor Munoz | 2000003014 | Liverpool FC | 79 |
| LCM (1-0) | Manuel Ugarte | 2000006022 | Manchester United FC | 77 |
| CM (1-1) | Wataru Endo | 2000003003 | Liverpool FC | 78 |
| RCM (1-2) | Ayyoub Bouaddi | 2000004017 | Manchester City FC | 80 |
| LB (2-0) | Myles Lewis-Skelly | 2000001024 | Arsenal FC | 78 |
| LCB (2-1) | Jeremy Jacquet | 2000003005 | Liverpool FC | 77 |
| RCB (2-2) | Cristhian Mosquera | 2000001003 | Arsenal FC | 78 |
| RB (2-3) | Rico Lewis | 2000004025 | Manchester City FC | 77 |
| GK (3-0) | Kepa Arrizabalaga | 2000001011 | Arsenal FC | 78 |

### 4-4-2 — 856/860 OVR

| Ô (khóa) | Cầu thủ | player_id | CLB | OVR |
| --- | --- | --- | --- | --- |
| ST (0-0) | Joshua Zirkzee | 2000006011 | Manchester United FC | 77 |
| ST (0-1) | Federico Chiesa | 2000030231 | Liverpool FC | 80 |
| LM (1-0) | Kostas Tsimikas | 2000003012 | Liverpool FC | 76 |
| LCM (1-1) | Manuel Ugarte | 2000006022 | Manchester United FC | 77 |
| RCM (1-2) | Ayyoub Bouaddi | 2000004017 | Manchester City FC | 80 |
| RM (1-3) | Diogo Dalot | 2000006002 | Manchester United FC | 78 |
| LB (2-0) | Myles Lewis-Skelly | 2000001024 | Arsenal FC | 78 |
| LCB (2-1) | Jeremy Jacquet | 2000003005 | Liverpool FC | 77 |
| RCB (2-2) | Cristhian Mosquera | 2000001003 | Arsenal FC | 78 |
| RB (2-3) | Rico Lewis | 2000004025 | Manchester City FC | 77 |
| GK (3-0) | Kepa Arrizabalaga | 2000001011 | Arsenal FC | 78 |

### 3-5-2 — 852/860 OVR

| Ô (khóa) | Cầu thủ | player_id | CLB | OVR |
| --- | --- | --- | --- | --- |
| ST (0-0) | Joshua Zirkzee | 2000006011 | Manchester United FC | 77 |
| ST (0-1) | Federico Chiesa | 2000030231 | Liverpool FC | 80 |
| LM (1-0) | Kostas Tsimikas | 2000003012 | Liverpool FC | 76 |
| LCM (1-1) | Rico Lewis | 2000004025 | Manchester City FC | 77 |
| CM (1-2) | Manuel Ugarte | 2000006022 | Manchester United FC | 77 |
| RCM (1-3) | Myles Lewis-Skelly | 2000001024 | Arsenal FC | 78 |
| RM (1-4) | Diogo Dalot | 2000006002 | Manchester United FC | 78 |
| LCB (2-0) | Jeremy Jacquet | 2000003005 | Liverpool FC | 77 |
| CB (2-1) | Cristhian Mosquera | 2000001003 | Arsenal FC | 78 |
| RCB (2-2) | Vitor Reis | 2000004013 | Manchester City FC | 76 |
| GK (3-0) | Kepa Arrizabalaga | 2000001011 | Arsenal FC | 78 |

## Kiểm tra đã chạy

- Frontend: 10 test logic/API Fantasy (4 sơ đồ; RM/RB/RW; sai ô; thiếu dữ liệu;
  trùng; CLB; OVR; ghép cặp có vị trí phụ; không thể ghép; khôi phục lưu cũ;
  đọc đúng trạng thái MISSING và từ chối định dạng API sai).
- Backend: 22 test trong FantasyLineupServiceTest, FantasyControllerTest,
  ApiCorsConfigurationTest và PlayerControllerTest; bao gồm H2 cô lập, dữ liệu giả,
  OVR/vị trí giả do client gửi, membership hết hiệu lực, CORS và giữ API cũ.
- `mvn -DskipTests package` và `npm.cmd run build` đạt.
- Chrome desktop 1366px và mobile 390px: chọn Amad vào RB, chặn chọn lại,
  loại Shaw khỏi RB; lọc tên/CLB; thông báo CLB/OVR; đổi sơ đồ hợp lệ/không hợp lệ;
  giữ 11 lựa chọn khi reload; chặn Shaw lưu ở ST; không tràn ngang; không lỗi JS.
- Không nhập production, không thu thập vị trí mới, không chạy toàn bộ Gameweek/test CSV.

Khi phát hành cần cả backend lẫn frontend vì có endpoint mới. Chưa commit/push/deploy.

## File cần commit

- `backend/src/main/java/com/premierhub/config/ApiCorsConfiguration.java`
- `backend/src/main/java/com/premierhub/service/FantasyLineupService.java`
- `backend/src/main/java/com/premierhub/web/FantasyController.java`
- `backend/src/main/java/com/premierhub/web/dto/FantasyLineupRequest.java`
- `backend/src/main/java/com/premierhub/web/dto/FantasyValidationResponse.java`
- `backend/src/test/java/com/premierhub/service/FantasyLineupServiceTest.java`
- `backend/src/test/java/com/premierhub/web/FantasyControllerTest.java`
- `frontend/src/api/fantasy.js`
- `frontend/src/api/fantasy.test.js`
- `frontend/src/components/FantasyPage.jsx`
- `frontend/src/components/FantasyPage.css`
- `frontend/src/fantasy/lineup.js`
- `frontend/src/fantasy/lineup.test.js`
- `docs/fantasy-lineup-validation-2026.md`

Không commit file trong `backend/target/`, dữ liệu H2, log, ảnh kiểm tra,
hoặc `frontend/dist/`. Không thay schema hoặc hai batch vị trí đã có.

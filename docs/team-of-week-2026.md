# Đội hình tiêu biểu PrismaXI 2026/27

Trong tab Fantasy, chọn **Đội hình tiêu biểu**, rồi chọn GW1–GW5. Đội người dùng, sơ đồ, luật OVR và giới hạn CLB vẫn dùng luồng Fantasy hiện có. Đổi GW hoặc chuyển giữa hai tab con không thay đổi đội đã chọn.

## API và dữ liệu

`GET /api/fantasy/2026/team-of-week?gameweek=1`

- Chỉ hỗ trợ mùa 2026/27, Premier League `league_id=39`, GW1–GW5. GW ngoài khoảng trả HTTP 400; không có endpoint mùa 2024/25.
- Mỗi request đọc `manual_fixture_player_stats`, nối `fixtures` để lấy đúng vòng. Chỉ `participation_status='PLAYED'` có rating khác SQL `NULL` và có `eligiblePositions` mới được phân công. Không lấy OVR, không fetch SofaScore và không lưu kết quả vào bảng SQL.
- Tên CLB lấy qua `manual_fixture_player_stats.club_id`, là đội cầu thủ đại diện trong trận. Không lọc membership, ngày hiện tại, roster Fantasy hoặc `player_season_stats`.
- `player_eligible_positions` được đọc theo đúng league/mùa/player. Không suy diễn CB, CM hoặc vị trí khác từ vị trí tổng quát. Chỉ chuẩn hóa tên ô LCB/RCB → CB và LCM/RCM → CM.
- `excludedMissingPositions` đếm mọi người PLAYED thiếu vị trí; `excludedNullRatings` đếm mọi người PLAYED chưa có rating. Hai số có thể giao nhau nếu một người thiếu cả hai. `candidateCount` đếm người có cả rating và vị trí, kể cả vị trí không phù hợp sơ đồ 4-3-3.
- `completedFixtures` là số trận đã kết thúc; `recordedFixtures` là số trận trong đó có ít nhất một dòng thống kê. Đây là thông tin độ phủ, không chứng nhận mọi chỉ số của trận đều đã được nhập đủ.

## Phân công và kết quả

Thứ tự ô cố định: **GK, LB, LCB, RCB, RB, LCM, CM, RCM, LW, ST, RW**.

`TeamOfWeekOptimizer` dùng quy hoạch động trên 2.048 trạng thái ô đã lấp. Mỗi cầu thủ được xét một lần; duyệt mask giảm dần nên không thể dùng cùng player_id ở hai ô. Cách này xét việc di chuyển cầu thủ đa vị trí để đạt tổng rating lớn nhất, thay vì chọn người tốt nhất riêng cho từng ô. Rating được đổi thành số nguyên phần trăm để so sánh chính xác, không cộng số thực.

Nếu bằng tổng rating, chọn dãy player_id nhỏ nhất theo thứ tự ô trên. Sắp xếp đầu vào theo player_id và quy tắc phụ này giữ kết quả ổn định khi thứ tự đọc dữ liệu thay đổi.

- `COMPLETE`: đủ 11 người hợp lệ; `totalRating` là tổng rating của 11 người.
- `INSUFFICIENT_DATA`: ưu tiên lấp nhiều ô nhất, rồi tổng rating cao nhất và cùng quy tắc phụ. `picks` giữ `player: null` ở ô thiếu, `missingSlots` liệt kê các ô đó, `totalRating: null`. Giao diện không tự gán sai vị trí hoặc biến rating NULL thành 0.
- `MULTIPLE_MATCHES`: phát hiện một player_id có nhiều lần PLAYED trong cùng GW, kể cả khi một trận chưa có rating. `conflicts` trả tên, player_id và fixtureIds; toàn bộ việc chọn đội dừng lại để chốt cách tính. Chưa áp dụng max/trung bình/tổng rating giữa các trận.

Không áp dụng giới hạn 860 OVR, tối đa 3 người/CLB hoặc yêu cầu có OVR cho đội tiêu biểu.

## Đối chiếu dữ liệu đã lưu

Đối chiếu GW1–GW5 bằng repository/service mới, kết nối MySQL trong transaction `READ ONLY` và `ROLLBACK`. Không chạy Spring Boot khởi tạo schema, importer hoặc SQL ghi production. Một phép kiểm tra độc lập bằng min-cost flow xác nhận số ô và tổng rating tối ưu, đồng thời đối chiếu ID, vị trí, rating, CLB lịch sử với từng dòng nguồn.

| GW | Trạng thái | Tổng rating | PLAYED | Có rating | Ứng viên | Thiếu vị trí | Rating NULL | Trận có thống kê/đã kết thúc |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 1 | COMPLETE | 92.90 | 310 | 304 | 274 | 30 | 6 | 10/10 |
| 2 | COMPLETE | 96.30 | 312 | 303 | 276 | 28 | 9 | 10/10 |
| 3 | COMPLETE | 91.70 | 307 | 301 | 281 | 20 | 6 | 10/10 |
| 4 | COMPLETE | 94.50 | 307 | 304 | 287 | 17 | 3 | 10/10 |
| 5 | COMPLETE | 93.30 | 302 | 298 | 281 | 17 | 4 | 10/10 |

Mỗi vòng đủ 11 ô, không trùng player_id, không phát hiện cầu thủ PLAYED nhiều trận trong cùng vòng. Đây là kết quả tối ưu trên dữ liệu đã lưu; người thiếu vị trí hoặc rating chưa thể tham gia tính toán. Kết quả được tính lại nếu dữ liệu nguồn thay đổi.

### GW1

| Slot | Player ID | Name | Match club | Rating |
| --- | ---: | --- | --- | ---: |
| GK | 2000020085 | Jordan Pickford | Everton FC | 8.20 |
| LB | 2000030042 | Maxim De Cuyper | Brighton & Hove Albion FC | 8.20 |
| LCB | 2000004004 | Marc Guehi | Manchester City FC | 8.30 |
| RCB | 2000010003 | Nobel Mendy | Hull City AFC | 8.00 |
| RB | 2000030041 | Jack Hinshelwood | Brighton & Hove Albion FC | 9.00 |
| LCM | 2000001008 | Martin Odegaard | Arsenal FC | 8.40 |
| CM | 2000005025 | Romeo Lavia | Chelsea FC | 8.90 |
| RCM | 2000015001 | Kiernan Dewsbury-Hall | Everton FC | 8.60 |
| LW | 2000005012 | Morgan Rogers | Chelsea FC | 8.40 |
| ST | 2000005008 | Joao Pedro | Chelsea FC | 7.90 |
| RW | 2000005009 | Cole Palmer | Chelsea FC | 9.00 |

### GW2

| Slot | Player ID | Name | Match club | Rating |
| --- | ---: | --- | --- | ---: |
| GK | 2000020030 | Robin Roefs | Sunderland AFC | 8.70 |
| LB | 2000030128 | Lewis Hall | Newcastle United FC | 9.70 |
| LCB | 2000003005 | Jeremy Jacquet | Liverpool FC | 8.20 |
| RCB | 2000020049 | Nordi Mukiele | Sunderland AFC | 8.10 |
| RB | 2000020053 | Neco Williams | Nottingham Forest FC | 8.90 |
| LCM | 2000006008 | Bruno Fernandes | Manchester United FC | 10.00 |
| CM | 2000012002 | Granit Xhaka | Sunderland AFC | 8.40 |
| RCM | 2000030110 | Alex Scott | AFC Bournemouth | 8.50 |
| LW | 2000012001 | Nilson Angulo | Sunderland AFC | 8.90 |
| ST | 2000005008 | Joao Pedro | Chelsea FC | 8.30 |
| RW | 2000004008 | Rayan Cherki | Manchester City FC | 8.60 |

### GW3

| Slot | Player ID | Name | Match club | Rating |
| --- | ---: | --- | --- | ---: |
| GK | 2000004001 | Gianluigi Donnarumma | Manchester City FC | 8.80 |
| LB | 2000030187 | Ben Chilwell | Crystal Palace FC | 7.70 |
| LCB | 2000002003 | Bobby Thomas | Coventry City FC | 8.40 |
| RCB | 2000003004 | Virgil van Dijk | Liverpool FC | 8.00 |
| RB | 2000030182 | Axel Disasi | Crystal Palace FC | 7.70 |
| LCM | 2000001008 | Martin Odegaard | Arsenal FC | 9.20 |
| CM | 2000004003 | Elliot Anderson | Manchester City FC | 8.50 |
| RCM | 2000030083 | Joshua King | Fulham FC | 8.10 |
| LW | 2000030031 | Marcus Tavernier | AFC Bournemouth | 8.30 |
| ST | 2000003009 | Alexander Isak | Liverpool FC | 8.70 |
| RW | 2000006018 | Bryan Mbeumo | Manchester United FC | 8.30 |

### GW4

| Slot | Player ID | Name | Match club | Rating |
| --- | ---: | --- | --- | ---: |
| GK | 2000030055 | Bart Verbruggen | Brighton & Hove Albion FC | 9.80 |
| LB | 2000004014 | Josko Gvardiol | Manchester City FC | 7.90 |
| LCB | 2000003005 | Jeremy Jacquet | Liverpool FC | 8.50 |
| RCB | 2000030045 | Lewis Dunk | Brighton & Hove Albion FC | 9.10 |
| RB | 2000020095 | Daniel Muñoz | Nottingham Forest FC | 8.00 |
| LCM | 2000002004 | Jack Rudoni | Coventry City FC | 8.50 |
| CM | 2000020091 | James Garner | Everton FC | 8.30 |
| RCM | 2000030049 | Pascal Groß | Brighton & Hove Albion FC | 8.70 |
| LW | 2000030094 | Kevin Schade | Brentford FC | 8.50 |
| ST | 2000020067 | Dominic Calvert-Lewin | Leeds United FC | 8.70 |
| RW | 2000020009 | Mohamed Belloumi | Hull City AFC | 8.50 |

### GW5

| Slot | Player ID | Name | Match club | Rating |
| --- | ---: | --- | --- | ---: |
| GK | 2000006001 | Senne Lammens | Manchester United FC | 8.60 |
| LB | 2000002002 | Jay Dasilva | Coventry City FC | 7.90 |
| LCB | 2000002001 | Ethan Pinnock | Coventry City FC | 8.20 |
| RCB | 2000030099 | Jannik Schuster | Brentford FC | 8.50 |
| RB | 2000030109 | James Hill | AFC Bournemouth | 8.10 |
| LCM | 2000004011 | Enzo Fernandez | Manchester City FC | 8.20 |
| CM | 2000020055 | Morgan Gibbs-White | Nottingham Forest FC | 8.10 |
| RCM | 2000030049 | Pascal Groß | Brighton & Hove Albion FC | 8.40 |
| LW | 2000030061 | Emiliano Buendía | Aston Villa FC | 8.20 |
| ST | 2000020038 | Brian Brobbey | Sunderland AFC | 9.80 |
| RW | 2000004020 | Antoine Semenyo | Manchester City FC | 9.30 |


## Kiểm tra

- Backend: 17 test trong `TeamOfWeekOptimizerTest`, `TeamOfWeekServiceTest`, `TeamOfWeekControllerTest`, `FantasyLineupServiceTest`, `FantasyControllerTest`; Maven package thành công. Dùng H2 riêng cho test, không cấu hình datasource production.
- Sau rà soát cuối về điều kiện PLAYED và cách đếm thiếu vị trí, đã chạy lại đúng bước kiểm tra/build backend; frontend test/build và browser chạy một lượt.
- Frontend: 14 test trong `teamOfWeek.test.js`, `lineup.test.js`, `api/fantasy.test.js`; Vite build thành công.
- Browser Chrome: desktop 1440px và mobile 390px không tràn ngang; GW1–GW5 hiển thị đúng cầu thủ và tổng rating từ kết quả service đọc MySQL. Kiểm tra loading, lỗi/thử lại, thiếu dữ liệu, xung đột nhiều trận và đường dẫn chi tiết mùa 2026.
- Đổi GW, chuyển tab rồi quay lại giữ nguyên cầu thủ, sơ đồ và localStorage đội người dùng. GW đã chọn cũng được giữ khi chuyển tab con.

Ảnh và script đối chiếu nằm trong `backend/target/team-of-week-check/`, là đầu ra kiểm tra bị Git ignore; không đưa vào commit. Không thêm dependency, migration hoặc dữ liệu mùa 2024/25.

## File cần commit

Backend:

- `backend/src/main/java/com/premierhub/repository/TeamOfWeekRepository.java`
- `backend/src/main/java/com/premierhub/service/TeamOfWeekOptimizer.java`
- `backend/src/main/java/com/premierhub/service/TeamOfWeekService.java`
- `backend/src/main/java/com/premierhub/web/TeamOfWeekController.java`
- `backend/src/main/java/com/premierhub/web/dto/TeamOfWeekResponse.java`
- `backend/src/test/java/com/premierhub/service/TeamOfWeekOptimizerTest.java`
- `backend/src/test/java/com/premierhub/service/TeamOfWeekServiceTest.java`
- `backend/src/test/java/com/premierhub/web/TeamOfWeekControllerTest.java`

Frontend:

- `frontend/src/components/FantasyPage.jsx`
- `frontend/src/components/FantasyPitch.jsx`
- `frontend/src/components/FantasyPlayerAvatar.jsx`
- `frontend/src/components/TeamOfWeek.jsx`
- `frontend/src/components/TeamOfWeek.css`
- `frontend/src/api/teamOfWeek.js`
- `frontend/src/fantasy/teamOfWeek.js`
- `frontend/src/fantasy/teamOfWeek.test.js`

Tài liệu: `docs/team-of-week-2026.md`.

Commit message đề xuất: `feat: add optimal 2026/27 Fantasy team of the week`

# Fantasy GW6 2026/27 — vắng mặt vì chấn thương ngoài danh sách trận

Ngày 12/10/2026, người dùng xác nhận sáu cầu thủ trong [confirmed-not-registered.csv](confirmed-not-registered.csv) không được đăng ký đúng trận GW6 vì chấn thương và chốt điểm Fantasy 0 cho tám lượt chọn liên quan. Căn cứ là ảnh đội hình hai đội đã đối chiếu và xác nhận trực tiếp này. Mỗi người có membership tại CLB/fixture tương ứng nhưng không có trong CSV thống kê, SQL thống kê hoặc danh sách vai trò trận. Họ không phải `SUB_UNUSED`/`DID_NOT_PLAY`; không tạo dòng thống kê, không điền rating 0 và không áp dụng xác nhận cho fixture/GW khác.

| Fixture | Cầu thủ | Lượt chọn |
| --- | --- | ---: |
| `1000560595` Chelsea–Bournemouth | Reece James `2000005017` | 1 |
| `1000560596` Palace–Forest | Nikola Milenković `2000020057` | 1 |
| `1000560596` Palace–Forest | Nicolò Savona `2000030244` | 1 |
| `1000560598` Liverpool–Man City | Dominik Szoboszlai `2000003008` | 2 |
| `1000560598` Liverpool–Man City | Alexander Isak `2000003009` | 2 |
| `1000560601` Aston Villa–Brentford | James Wright `2000030151` | 1 |

Schema thêm `fantasy_match_absence_confirmations` với khóa chính `(fixture_id, player_id)`, CLB, mã `INJURY_NOT_REGISTERED`, nguồn và thời điểm xác nhận. Importer chỉ nhận fixture FINISHED, membership đúng CLB, không có dòng thống kê hoặc vai trò trận; file nguồn và số lượt chọn đã được đối chiếu trước khi ghi. Scoring có lý do riêng `INJURY_NOT_REGISTERED_CONFIRMED`, giữ rating `NULL` và chỉ áp dụng cho đúng fixture. Bằng chứng này nằm trong hash nguồn tổng của lần công bố, nhưng không đổi hash xác nhận CSV/đội hình của chín trận đã xong.

Đã xác nhận Aiven `defaultdb` UUID `715946cc-c33a-11f1-8fd9-8e0831c19405` đúng backend Render. Backup mới ngoài Git trước migration/nhập: `backend/local-backups/gw6-injury-absence-20261012-160709/aiven-before-injury-absence-20261012-160709.sql`, 755.177 byte, SHA-256 `0ec7d179830a13b555be5a30a1939968a44cdd2f90fca95f1a5afb06d79894bb`; đủ 13 bảng, DDL và số dòng khớp SQL. Migration chạy lại không đổi schema. Importer lần đầu: `fixtures=9 fixtureChanges=0 unratedChanges=0 absenceChanges=6`; chạy lại mọi thay đổi=0. Đọc lại Aiven: đúng sáu cặp nguồn, 13 bảng còn lại gồm thống kê/rating, đội hình, membership, 44 picks của bốn đội và bảng công bố/kết quả không đổi.

Backend mã mới chạy read-only trên Aiven xác nhận **cả tám lượt chọn** đều có điểm `0.00`, rating `NULL`, đúng fixture và lý do `INJURY_NOT_REGISTERED_CONFIRMED`. Readiness: `ready=false`, 10 fixture, 4 đội chốt, `currentVersion=0`. Còn đúng **6 cảnh báo**, đều ở Coventry–Newcastle `1000560597`: `FIXTURE_PENDING=1`, `LINEUP_UNCONFIRMED=2`, `STARTERS_UNCONFIRMED=2`, `FIXTURE_UNCONFIRMED=1`. Không còn `SELECTED_STAT_MISSING`, `PLAYER_MATCH_UNRESOLVED`, `RATING_PENDING`, `SOURCE_CHANGED` ở chín trận đã xong. Trận cuối vẫn SCHEDULED; không tạo dữ liệu giả.

Suite `FantasyResultIntegrationTest` liên quan đạt 16 test, gồm kiểm tra điểm 0 và trường hợp phải từ chối xác nhận chồng với đội hình. **Cần deploy backend mã mới** để API readiness/scoring production đọc bảng vắng mặt và hiển thị chỉ sáu cảnh báo còn lại; frontend không sửa. Không tự deploy, commit, push hoặc công bố Fantasy.

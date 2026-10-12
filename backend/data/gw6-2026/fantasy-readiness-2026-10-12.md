# Fantasy GW6 2026/27 — đối chiếu readiness ngày 12/10/2026

Checkpoint này ghi trạng thái **trước** xác nhận chấn thương ngoài danh sách trận. Kết quả mới nhất và số cảnh báo còn lại ở [fantasy-absence-confirmed.md](fantasy-absence-confirmed.md).

Readiness của backend chạy với đúng Aiven production `defaultdb` (UUID `715946cc-c33a-11f1-8fd9-8e0831c19405`) trước nhập có 4 đội đã chốt, 10 fixture, `currentVersion=0`. Chín fixture FINISHED đều có đúng CSV thống kê 360 dòng và 18 đội hình đã xác minh; nhưng `fantasy_fixture_confirmations` và `fantasy_unrated_confirmations` đều có 0 dòng. `FantasyResultService` đòi xác nhận file cuối và ngoại lệ không được chấm **được lưu trong database**. CSV/ghi chú nguồn và rating SQL `NULL` tự chúng chưa phải xác nhận Fantasy.

Đã dùng `FantasyEvidenceImporter` hiện có với `manual-match-stats-2026-GW6.csv`, `confirmed-unrated.csv` và `sources.md`. Importer đối chiếu chính xác tập cầu thủ và từng chỉ số CSV với SQL, kiểm tra vai trò/đội hình và membership tại ngày trận rồi ghi hash nguồn. Lần đầu: `fixtures=9`, `fixtureChanges=9`, `unratedChanges=4`; lần hai: cả hai thay đổi bằng 0. Merino, Ayden Heaven, Marcus Rashford và Dwight McNeil vẫn `PLAYED`, rating/fantasy_points SQL `NULL`.

Backup trước ghi nằm ngoài Git: `backend/local-backups/gw6-fantasy-evidence-20261012-154524/aiven-before-fantasy-evidence-20261012-154524.sql`, 752.713 byte, SHA-256 `a917b4089722be5937dd77f6451d43b3eb7259b6ff8da4368f03385c03a39276`. Đã kiểm tra dump hoàn chỉnh, DDL và số dòng của 13 bảng. So sánh hash toàn bộ các bảng được bảo vệ sau nhập xác nhận thống kê, rating, đội hình, membership, tổng mùa, đội người chơi, cấu hình GW và bảng kết quả/công bố không đổi. Không có bản công bố kết quả.

## Cầu thủ được chọn nhưng thiếu dòng GW6

Cả sáu cầu thủ dưới đây có membership hợp lệ với CLB của trận nhưng **không có trong CSV thống kê, SQL thống kê hay `lineups/players.csv`/bảng đội hình trận**. Vì danh sách 20 người của mỗi đội đã được xác minh từ ảnh, không có căn cứ coi họ là `DID_NOT_PLAY`, gán 0 hoặc thêm dòng giả. Một số được chọn trong hai đội Fantasy nên tạo 8 lượt cảnh báo. `PLAYER_MATCH_UNRESOLVED` là hệ quả thứ hai của cùng 8 lượt này, không phải 8 cầu thủ khác.

| Cầu thủ | Fixture | Tài khoản có lượt chọn | Phân loại |
| --- | --- | --- | --- |
| Nicolò Savona `2000030244` | `1000560596` Palace–Forest | 1 | Ngoài danh sách trận đã xác minh |
| Nikola Milenković `2000020057` | `1000560596` Palace–Forest | 2 | Ngoài danh sách trận đã xác minh |
| Alexander Isak `2000003009` | `1000560598` Liverpool–Man City | 2, 3 | Ngoài danh sách trận đã xác minh |
| Dominik Szoboszlai `2000003008` | `1000560598` Liverpool–Man City | 2, 3 | Ngoài danh sách trận đã xác minh |
| Reece James `2000005017` | `1000560595` Chelsea–Bournemouth | 5 | Ngoài danh sách trận đã xác minh |
| James Wright `2000030151` | `1000560601` Aston Villa–Brentford | 5 | Ngoài danh sách trận đã xác minh |

Ảnh readiness người dùng gửi còn có `2000002005`; ID đó không nằm trong 44 lượt chọn của bốn đội đã chốt đang lưu trên Aiven lúc kiểm tra. Không suy đoán lý do khác biệt giữa thời điểm chụp ảnh và dữ liệu hiện tại, cũng không sửa snapshot.

## Readiness sau nhập

`ready=false`, 10 fixture, 4 đội đã chốt, `currentVersion=0`:

| Nguyên nhân | Số cảnh báo | Phạm vi |
| --- | ---: | --- |
| `FIXTURE_UNCONFIRMED`, `RATING_PENDING` của 9 trận FINISHED | 0 | Đã giải quyết bằng 9 xác nhận CSV cuối và 4 ngoại lệ rating |
| `SELECTED_STAT_MISSING` | 8 | Sáu cầu thủ ngoài danh sách đã xác minh ở trên |
| `PLAYER_MATCH_UNRESOLVED` | 8 | Hệ quả của cùng 8 lượt chọn thiếu dòng |
| `FIXTURE_PENDING` | 1 | Coventry–Newcastle `1000560597` còn SCHEDULED |
| `LINEUP_UNCONFIRMED` | 2 | Hai đội Coventry–Newcastle đang chờ |
| `STARTERS_UNCONFIRMED` | 2 | Hai đội Coventry–Newcastle đang chờ |
| `FIXTURE_UNCONFIRMED` | 1 | Chưa có CSV cuối của Coventry–Newcastle |

Không có `SOURCE_CHANGED` hay lỗi dữ liệu của chín fixture đã xác nhận. Để giải quyết sáu cầu thủ ngoài danh sách cần bằng chứng trận mới có thể kiểm chứng họ thực sự có mặt/dự bị, hoặc quyết định nghiệp vụ riêng về cầu thủ hoàn toàn ngoài danh sách; không tự áp điểm 0. Coventry–Newcastle xử lý sau khi trận kết thúc và đủ dữ liệu. Chưa công bố hoặc tái tính kết quả Fantasy.

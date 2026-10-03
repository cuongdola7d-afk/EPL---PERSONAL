# Vị trí cầu thủ Fantasy 2026/27 — bước dữ liệu

## Các ô hiện có

Đọc từ `frontend/src/fantasy/lineup.js` (mỗi dòng từ tấn công về thủ môn):

| Sơ đồ | Các ô thực tế |
| --- | --- |
| 4-2-1-3 | `LW ST RW` / `CAM` / `LCM RCM` / `LB LCB RCB RB` / `GK` |
| 4-3-3 | `LW ST RW` / `LCM CM RCM` / `LB LCB RCB RB` / `GK` |
| 4-4-2 | `ST ST` / `LM LCM RCM RM` / `LB LCB RCB RB` / `GK` |
| 3-5-2 | `ST ST` / `LM LCM CM RCM RM` / `LCB CB RCB` / `GK` |

Mã quyền chơi được lưu: `GK`, `LB`, `CB`, `RB`, `CM`, `CAM`, `LM`, `RM`, `LW`, `ST`, `RW`.
Các nhãn ô `LCB`/`RCB` cùng yêu cầu quyền `CB`; `LCM`/`RCM` cùng yêu cầu quyền `CM`.
Không có ô `CDM`, `LWB`, `RWB` hoặc `CF` trong bốn sơ đồ hiện tại.
Quyền `RM` của một người có vị trí chính `RW` phải được nhập riêng; không có quy tắc tự suy từ mã chính hoặc nhóm rộng.

## Nơi lưu và trạng thái thiếu dữ liệu

`player_season_stats` tiếp tục giữ nhóm rộng `position` và các thống kê cũ, khóa gồm cả `club_id`.
`player_specific_positions` giữ đúng một `primary_position` cho khóa `(league_id, season_year, player_id)`.
`player_eligible_positions` giữ từng mã được phép cho cùng khóa cộng `position_code`.
Tập vị trí phải chứa vị trí chính. Không tạo thêm dòng `player_season_stats` hoặc membership.
Cầu thủ chưa được xác minh không có dòng trong hai bảng mới; API 2026 trả
`primaryPosition: null`, `eligiblePositions: []`, `positionStatus: "MISSING"`.
Khi đã nhập, trạng thái là `VERIFIED`; API 2024 trả `NOT_APPLICABLE` và không đọc bảng mới.

Snapshot CSV roster 30/09 có 534 dòng/534 `player_id` duy nhất, đủ nhóm rộng:
60 GK, 175 DEF, 239 MID, 60 FWD. Hai mươi CSV hồ sơ ngày 02/10 cũng có 534 ID duy nhất.
Đây là độ phủ của file đã lưu trong repo, không phải phép đo MySQL production tại thời điểm chạy.
H2 local `backend/premierhub-local.mv.db` hiện có 0 dòng 2026 trong `player_season_stats`,
0 membership hiện hành và 0 hồ sơ 2026; test importer dùng H2 cô lập với roster giả.
Chưa có vị trí cụ thể nào được gán tự động từ các file này.

## Nhập batch nhỏ

CSV UTF-8 có đúng sáu cột:

```csv
player_id,season_year,expected_primary_position,expected_eligible_positions,primary_position,eligible_positions
101,2026,,,RW,RW|RM
102,2026,,,RW,RW
```

Các ID trên chỉ là ví dụ test, không phải dữ liệu roster để nhập production.
Hai cột `expected_*` để trống khi cầu thủ chưa có vị trí trong DB.
Khi sửa, ghi đúng vị trí đang lưu vào hai cột đó; ví dụ đổi tập `RW|RM` thành `RW`:

```csv
101,2026,RW,RW|RM,RW,RW
```

Importer kiểm tra ID đã có trong `player_season_stats` mùa 2026, mã hợp lệ, ID/mã không lặp,
và vị trí chính nằm trong tập được phép. Nếu nội dung đã lưu bằng nội dung mới thì thêm/cập nhật 0.
Nếu nội dung đã lưu khác cả nội dung mới lẫn `expected_*`, importer báo xung đột và rollback cả batch.
Không dùng file này để xóa vị trí: cầu thủ chưa xác minh chỉ cần bỏ khỏi batch.

Chạy từ `backend/` với database đã cấu hình và file CSV đã review:

```powershell
mvn spring-boot:run '-Dspring-boot.run.arguments=--premierhub.player-positions.enabled=true --premierhub.player-positions.file=data/positions-reviewed.csv'
```

Lượt này chỉ tạo schema/importer/API và test H2; chưa nhập Railway, chưa đổi UI hoặc luật chọn đội Fantasy.

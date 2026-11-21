# Thu thập số liệu trận từ FPL (từ GW11 mùa 2026/27)

> Áp dụng từ **GW11 mùa 2026/27**. Không nhập lại, không sửa dữ liệu **GW1–GW10** (giữ nguyên nguồn StatMuse) và không backfill bảng chỉ số mở rộng cho GW1–GW10.

## 1. Phân chia nguồn dữ liệu từ GW11

| Dữ liệu | Nguồn từ GW11 | Ghi chú |
| --- | --- | --- |
| Rating trận | **Ảnh SofaScore** (người dùng gửi) | Giữ nguyên luật `confirmed-unrated.csv`: có ra sân nhưng không được chấm thì `rating = NULL`, điểm Fantasy = 0 |
| Số phút, bàn thắng, kiến tạo, thẻ vàng, thẻ đỏ | **FPL API** (thay StatMuse) | Kiến tạo theo định nghĩa FPL |
| Chỉ số mở rộng (bảng `manual_fixture_player_extra_stats`) | **FPL API** | Chỉ nhập từ GW11 trở đi (`gameweek >= 11`) |
| Lịch, tỉ số, trạng thái trận, BXH | **football-data.org** (không đổi) | Đồng bộ trước khi chạy thu thập FPL |

## 2. FPL API và yêu cầu kỹ thuật

Base URL: `https://fantasy.premierleague.com/api/` (công khai, không cần đăng nhập).

Các endpoint được sử dụng:
1. `bootstrap-static/`: danh sách 20 CLB (`teams[]`), toàn bộ cầu thủ (`elements[]`), các vòng (`events[]`).
2. `fixtures/?event={gw}`: lịch/tỉ số/trạng thái từng trận (`id`, `kickoff_time`, `team_h`, `team_a`, `team_h_score`, `team_a_score`, `finished`, `finished_provisional`).
3. `event/{gw}/live/`: thống kê tổng hợp theo cầu thủ trong vòng (`elements[].stats`, `elements[].explain[].fixture`).
4. `element-summary/{id}/`: **chỉ gọi** cho cầu thủ có nhiều hơn 1 trận trong cùng vòng (Double Gameweek) để tách chỉ số theo từng `fixture` từ `history[]`.

Ràng buộc kỹ thuật trong `FplClient` / `FplJsonParser` / `FplCollectorService`:
- Gửi `User-Agent: PremierHub-PrismaXI/0.1 (FPL-collector)`, timeout 20 giây, tối đa 2 lần thử lại, gọi tuần tự (không gọi song song).
- Kiểm tra nghiêm ngặt đủ trường và đúng kiểu dữ liệu (chuỗi số `influence`, `creativity`, `threat`, `ict_index`, `expected_*` chuyển sang `BigDecimal`). Nếu thiếu trường hoặc sai kiểu thì **dừng và báo lỗi rõ ràng**, không tự điền `0`.
- Chỉ cho phép chạy khi **tất cả** trận của vòng đều có `finished = true` (không chấp nhận chỉ có `finished_provisional = true`). Nếu còn trận chưa xong, lệnh dừng và liệt kê các trận chưa kết thúc.

## 3. Đối chiếu mã (lưu trong repo, không tạo bảng DB)

- **CLB:** `backend/data/fpl-2026/team-map.csv` (`fpl_team_id,club_id,fpl_name,club_name`) — đủ 20 CLB Premier League 2026/27.
- **Cầu thủ:** `backend/data/fpl-2026/player-map.csv` (`fpl_element_id,player_id,fpl_name,player_name,source,verified_on`).
  - Khởi tạo từ các cặp `fpl_id` trong `backend/data/roster-2026-09-30/*.json` sau khi đối chiếu CLB theo membership hiện hành, bổ sung các trường hợp khớp **duy nhất** theo `(club_id, họ tên chuẩn hóa bỏ dấu)`.
  - Các trường hợp chưa chắc chắn hoặc thiếu membership được ghi tại `backend/data/fpl-2026/player-map-pending.csv` để chủ dự án xác nhận; **không đoán**.
- **Trận:** tính tự động lúc chạy theo bộ `(home_club_id, away_club_id, kickoff_date_utc)` khớp 1-1 với bảng `fixtures` mùa 2026 trong DB. Nếu không khớp đủ 1-1 cho cả vòng thì dừng ngay. Kết quả ghi ra `fixture-map.csv` trong thư mục vòng.

## 4. Bảng chỉ số mở rộng (`manual_fixture_player_extra_stats`)

- Khóa chính: `(fixture_id, player_id)`; kèm `league_id`, `season_year`, `club_id`, `source` (`'FPL'`), `fetched_at` (ràng buộc mùa/vòng đặt ở tầng importer `ManualExtraStatsImporter`, không đặt `CHECK (season_year = 2026)` trên DDL để không phải `ALTER` ở mùa sau).
- 13 cột số nguyên (đều cho phép `NULL`): `starts`, `clean_sheets`, `goals_conceded`, `own_goals`, `penalties_saved`, `penalties_missed`, `saves`, `bonus`, `bps`, `clearances_blocks_interceptions`, `recoveries`, `tackles`, `defensive_contribution`.
- 4 cột `DECIMAL(5,2)` (cho phép `NULL`): `expected_goals`, `expected_assists`, `expected_goal_involvements`, `expected_goals_conceded`.
- 4 cột `DECIMAL(6,1)` (cho phép `NULL`): `influence`, `creativity`, `threat`, `ict_index`.
- Schema local/H2: `backend/src/main/resources/schema.sql`.
- Migration MySQL (chạy sau preflight và backup trên Aiven):
  - Preflight: `backend/sql/2026-11-21-fpl-extra-stats-preflight.sql`
  - Migration: `backend/sql/2026-11-21-fpl-extra-stats-mysql.sql`

## 5. Công cụ CLI và cách chạy từng bước cho một vòng

Cả hai cờ CLI mới đều chạy chế độ non-web (`WebApplicationType.NONE`) và thoát tiến trình (`System.exit`), không khởi chạy Tomcat:
- `--premierhub.fpl-collect.enabled=true` (thu thập chỉ đọc DB + FPL, tùy chọn ghép rating)
- `--premierhub.manual-extra-stats.enabled=true` (nhập bảng `manual_fixture_player_extra_stats`)

### Bước 1 — Đồng bộ lịch/tỉ số `fixtures` từ football-data.org

Đảm bảo các trận trong vòng đã được đồng bộ trạng thái `FINISHED` và tỉ số vào DB theo `docs/gameweek-data-workflow.md`.

### Bước 2 — Thu thập dữ liệu FPL (chỉ đọc DB + FPL)

Từ thư mục `backend/`:

```powershell
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar `
  --premierhub.fpl-collect.enabled=true `
  --gameweek=11 `
  --season=2026 `
  --mapping-dir=data/fpl-2026 `
  --output-dir=data/gw11-2026/fpl
```

(Khi chạy đối chiếu với DB production, truyền cấu hình datasource chỉ đọc phù hợp + `--spring.sql.init.mode=never` và `--spring.profiles.active=prod` nếu cần.)

Các file tạo ra trong `backend/data/gwNN-2026/fpl/`:
1. `raw/`: JSON gốc `bootstrap-static.json`, `fixtures-gwNN.json`, `event-gwNN-live.json` (và `element-summary-{id}.json` nếu là DGW).
2. `fixture-map.csv`: bảng đối chiếu `fpl_fixture_id ↔ fixture_id`.
3. `unmapped.csv`: danh sách cầu thủ FPL có `minutes > 0` (hoặc nhận thẻ với `minutes = 0`) nhưng chưa có trong `player-map.csv`. **Nếu file này không rỗng, công cụ báo lỗi và KHÔNG tạo 2 file CSV cuối.**
4. `bench-cards.csv`: danh sách cầu thủ FPL có `minutes = 0` nhưng `yellow_cards > 0` hoặc `red_cards > 0` (thẻ trên ghế dự bị) để bước ghép chặn ghi `0` thẻ âm thầm.
5. `fpl-match-stats-2026-GWNN.csv`: `fixture_id,player_id,minutes,goals,assists,yellow_cards,red_cards` (chỉ cầu thủ `minutes > 0`).
6. `fpl-extra-stats-2026-GWNN.csv`: đủ 26 cột của bảng mở rộng (chỉ cầu thủ `minutes > 0`).

Nếu `unmapped.csv` có dữ liệu: xác minh danh tính/membership, bổ sung vào `backend/data/fpl-2026/player-map.csv` rồi chạy lại Bước 2 (có thể thêm `--input-dir=data/gwNN-2026/fpl/raw` để chạy lại từ JSON đã lưu mà không gọi lại mạng).

### Bước 3 — Ghép ảnh SofaScore + danh sách trận + FPL thành CSV nhập chuẩn

Có thể chạy chung ngay ở Bước 2 bằng `--merge=true`, hoặc chạy riêng sau khi đã chuẩn bị file rating từ ảnh bằng `--merge-only=true`:

```powershell
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar `
  --premierhub.fpl-collect.enabled=true `
  --gameweek=11 `
  --season=2026 `
  --output-dir=data/gw11-2026/fpl `
  --merge-only=true `
  --ratings-file=data/gw11-2026/screenshot-evidence.csv `
  --lineup-players-file=data/gw11-2026/lineups/fixture-lineup-players.csv `
  --merge-output-dir=data/gw11-2026
```

Đầu vào:
- `--ratings-file`: hỗ trợ cả định dạng `screenshot-evidence.csv` của GW10 (`fixture_id,club_id,shirt_number,player_name,rating,...` — giải mã `player_id` qua membership ngày trận và số áo) lẫn định dạng có sẵn cột `player_id` (`fixture_id,player_id,rating,unrated,...`). Ô không được SofaScore chấm ghi `UNRATED` / `không có rating` (hoặc cột `unrated=true` / `rating_status=UNRATED`).
- `--lineup-players-file`: file `fixture-lineup-players.csv` (danh sách trận gồm `STARTER`, `SUB_USED`, `SUB_UNUSED`). Nếu bỏ qua, hệ thống lấy danh sách trận từ các hàng trong `--ratings-file`.

Đầu ra trong `--merge-output-dir` (đúng cấu trúc GW10):
- `manual-match-stats-2026-GWNN.csv`: 10 cột `season,fixture_id,player_id,status,rating,minutes,goals,assists,yellow_cards,red_cards` cho toàn bộ cầu thủ trong danh sách trận của các fixture không có bất thường.
- `confirmed-unrated.csv`: các cầu thủ `PLAYED` nhưng ảnh xác nhận không có rating.
- `missing.csv`: các trường hợp `PLAYED` nhưng chưa cung cấp rating (chờ bổ sung ảnh/xác minh).
- `anomalies.csv`: danh sách các bất thường phát hiện được.

### Bước 4 — Luật ghép và xử lý bất thường

4 luật ghép bắt buộc:
1. FPL `minutes > 0` và có rating → `PLAYED`, giữ rating SofaScore, lấy `minutes/goals/assists/yellow_cards/red_cards` từ FPL.
2. FPL `minutes > 0` và ảnh ghi rõ "không có rating" → `PLAYED`, `rating` để trống (`NULL`), ghi vào `confirmed-unrated.csv` (điểm Fantasy = 0 theo luật cũ).
3. Trong danh sách trận nhưng FPL `minutes = 0` → `DID_NOT_PLAY`, `rating` để trống (`NULL`), `minutes = 0, goals = 0, assists = 0, yellow_cards = 0, red_cards = 0` (giữ đúng định dạng `manual-match-stats` của GW10).
4. **Bất thường** — ghi vào `anomalies.csv` và **không xuất dòng của trận đó vào `manual-match-stats-2026-GWNN.csv`** cho tới khi đối chiếu và xử lý xong:
   - `FPL_PLAYED_NOT_IN_SQUAD`: FPL ghi `minutes > 0` nhưng cầu thủ không có trong ảnh hoặc danh sách trận.
   - `RATING_WITH_ZERO_MINUTES`: ảnh có rating nhưng FPL ghi `0` phút.
   - `BENCH_CARD`: cầu thủ trong danh sách trận có FPL `minutes = 0` nhưng `yellow_cards > 0` hoặc `red_cards > 0` (không ghi `0` thẻ âm thầm).
   - `SCORE_MISMATCH`: tỉ số FPL khác tỉ số `fixtures` trong DB.

### Bước 5 — Backup và nhập dữ liệu vào DB (thử H2 trước, sau đó production)

Thực hiện theo `docs/gameweek-data-workflow.md` (xác minh đúng đích, tạo backup mới ngoài Git, thử trên H2 trước):
1. Nhập thống kê chính bằng importer hiện có: `--premierhub.manual-match-stats.enabled=true --premierhub.manual-match-stats.file=data/gwNN-2026/manual-match-stats-2026-GWNN.csv`.
2. Nhập chỉ số mở rộng bằng importer mới (idempotent, transactional, từ chối ghi đè giá trị khác `NULL`, chặn `gameweek < 11`):

```powershell
java -jar target/premierhub-backend-0.1.0-SNAPSHOT.jar `
  --premierhub.manual-extra-stats.enabled=true `
  --premierhub.manual-extra-stats.file=data/gw11-2026/fpl/fpl-extra-stats-2026-GW11.csv
```

Chạy lặp lần 2 để xác nhận `inserted=0, unchanged=N`.
3. Tiếp tục nhập lineups, Fantasy evidence (`confirmed-unrated.csv`, `confirmed-not-registered.csv`), tái tính tổng mùa, đối chiếu Đội hình tiêu biểu và readiness theo `docs/gameweek-data-workflow.md`.

## 6. Giới hạn cần lưu ý

- **Định nghĩa kiến tạo (Assists) của FPL rộng hơn thống kê chính thức (Opta/StatMuse):** FPL tính kiến tạo cả cho tình huống kiếm phạt đền, kiếm đá phạt dẫn đến bàn thắng, hoặc cú sút bật cột/xà/thủ môn/hậu vệ để đồng đội ghi bàn. Do đó từ GW11 trở đi, số `assists` tuân theo định nghĩa FPL và không so sánh 1-1 tuyệt đối với GW1–GW10 (StatMuse).
- **FPL API không có tài liệu chính thức (unofficial):** cấu trúc JSON hoặc tên trường có thể thay đổi bất ngờ giữa mùa. `FplJsonParser` kiểm tra chặt mọi trường bắt buộc và kiểu dữ liệu, dừng ngay kèm thông báo lỗi cụ thể thay vì tự điền `0`.
- **Không hồi tố GW1–GW10:** `ManualExtraStatsImporter` chặn nhập mọi fixture có `gameweek < 11`. Dữ liệu GW1–GW10 giữ nguyên nguồn StatMuse.
- **Chưa có API/UI cho bảng mở rộng:** `manual_fixture_player_extra_stats` hiện chỉ lưu trữ dữ liệu cho các bước sử dụng sau này, chưa mở endpoint hoặc hiển thị trên frontend.

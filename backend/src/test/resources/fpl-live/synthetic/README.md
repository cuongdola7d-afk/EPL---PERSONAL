# Dữ liệu tự tạo từ file thật, dùng cho test (`synthetic/`)

> **Lưu ý quan trọng:** Toàn bộ các file trong `synthetic/fixtures/` là **dữ liệu tự tạo từ file thật** (`../real/fixtures-gw11-20261122.json`), **chỉ dùng cho test và kiểm thử replay chuỗi thời gian** (`LIVE → bàn thắng → FT`). Đây **không phải** bản ghi trực tiếp theo thời gian thực khi trận đang diễn ra.

## Cách tạo và chi tiết chỉnh sửa từng file

Các file được sinh có kiểm soát từ bản gốc `../real/fixtures-gw11-20261122.json` bằng cách giữ nguyên toàn bộ ID trận, cặp đội (`team_h`, `team_a`), `element` cầu thủ thật của FPL, và chỉ lùi `started`, `minutes`, `finished_provisional`, `team_h_score`, `team_a_score`, `goals_scored`, `assists`, `own_goals` của 2 trận ngày `2026-11-21` (`id = 104` Chelsea – Leeds và `id = 105` Coventry – Crystal Palace), đồng thời đặt `id = 106` (trận Chủ nhật Hull – Brighton) về `started = false, minutes = 0`:

1. **`fixtures/01-synthetic-from-real-gw11-18m.json`** (tự tạo từ file thật — mốc phút 18):
   - Trận `id = 104` (Chelsea – Leeds): đặt `started = true`, `finished_provisional = false`, `minutes = 18`, tỉ số `1–0`; `goals_scored.h = [{"value": 1, "element": 170}]` (Emegha), `assists.h = [{"value": 1, "element": 40}]` (Morgan Rogers), `goals_scored.a = []`, `assists.a = []`.
   - Trận `id = 105` (Coventry – Crystal Palace): đặt `started = true`, `finished_provisional = false`, `minutes = 18`, tỉ số `1–0`; `goals_scored.h = [{"value": 1, "element": 185}]` (Sakamoto), `goals_scored.a = []`, `own_goals.a = []`.
2. **`fixtures/02-synthetic-from-real-gw11-36m.json`** (tự tạo từ file thật — mốc phút 36):
   - Trận `id = 104` (Chelsea – Leeds): đặt `minutes = 36`, tỉ số `1–1`; thêm bàn gỡ hòa của Leeds `goals_scored.a = [{"value": 1, "element": 346}]` (Calvert-Lewin), `assists.a = [{"value": 1, "element": 338}]` (Ampadu).
   - Trận `id = 105` (Coventry – Crystal Palace): đặt `minutes = 36`, tỉ số `1–1`; thêm bàn của Crystal Palace `goals_scored.a = [{"value": 1, "element": 211}]` (Yéremy Pino).
3. **`fixtures/03-synthetic-from-real-gw11-55m.json`** (tự tạo từ file thật — mốc phút 55):
   - Trận `id = 104` (Chelsea – Leeds): đặt `minutes = 55`, tỉ số `1–2`; thêm bàn thứ nhất của Noah Okafor (`element = 336`, `value = 1`) và kiến tạo của Jayden Bogle (`element = 330`, `value = 1`).
   - Trận `id = 105` (Coventry – Crystal Palace): đặt `minutes = 55`, tỉ số `2–1`; thêm bàn phản lưới thật của Dwight McNeil (`own_goals.a = [{"value": 1, "element": 241}]`).
4. **`fixtures/04-synthetic-from-real-gw11-71m.json`** (tự tạo từ file thật — mốc phút 71):
   - Trận `id = 104` (Chelsea – Leeds): đặt `minutes = 71`, tỉ số `1–3`; tăng số bàn của Noah Okafor (`element = 336`) từ `value = 1` lên `value = 2`, thêm kiến tạo của Calvert-Lewin (`element = 346`, `value = 1`).
   - Trận `id = 105` (Coventry – Crystal Palace): đặt `minutes = 71`, tỉ số `3–2`; thêm bàn của Taiwo Awoniyi (`element = 492`, `value = 1`) và Jørgen Strand Larsen (`element = 222`, `value = 1`).
5. **`fixtures/05-synthetic-from-real-gw11-90m-ft.json`** (tự tạo từ file thật — mốc phút 90 kết thúc `FT`):
   - Trận `id = 104` (Chelsea – Leeds) và `id = 105` (Coventry – Crystal Palace): đặt `minutes = 90`, `finished_provisional = true`, khôi phục nguyên văn `team_h_score`, `team_a_score` và toàn bộ mảng `stats` thật như trong `../real/fixtures-gw11-20261122.json` (Chelsea 1–4 Leeds với bàn ấn định của Brenden Aaronson `element = 337`, kiến tạo Harry Wilson `element = 260`; Coventry 3–2 Crystal Palace).

# Minigame Wordle cầu thủ — Mùa 2026/27

## 1. Luật chơi

- Mỗi ngày một cầu thủ Premier League mùa 2026/27, tối đa **6 lượt**, đổi câu đố mới lúc **00:00 giờ Việt Nam (`Asia/Ho_Chi_Minh`)**.
- **Chỉ có chế độ Daily.** Không tính điểm, không có bảng xếp hạng; chỉ lưu thống kê cá nhân (`played`, `wins`, `winPercentage`, `currentStreak`, `maxStreak`, `distribution` 1–6).
- **Bắt buộc đăng nhập** (session + header `X-PrismaXI-Account-ID`).
- **Server giữ đáp án:** khi ván đang `PLAYING`, server chỉ trả về màu từng ô (`H` = đúng chữ đúng chỗ, `N` = có chữ sai chỗ, `M` = không có). Đáp án đầy đủ chỉ được trả về khi ván đã kết thúc (`WON` hoặc `LOST`).
- **Gợi ý CLB:** mở được sau khi đã gửi ít nhất **4 lượt đoán** (1 lần duy nhất cho mỗi ván).
- **Ván qua nửa đêm:** ván `PLAYING` của ngày hôm trước bị khóa (không đoán tiếp được) và **không tính là thua** trong thống kê. Chỉ các ván `WON` hoặc `LOST` mới tính vào `played`; chuỗi thắng bị đứt khi thua hoặc bỏ qua một ngày.

## 2. Quy tắc chuẩn hóa tên (`WordleNames.normalize`)

1. Bỏ từ đầu tiên (tên gọi) nếu tên có nhiều từ (`Virgil van Dijk` → `van Dijk`, `Alexis Mac Allister` → `Mac Allister`). Tên 1 từ giữ nguyên (`Estêvão` → `Estêvão`).
2. Chuyển ký tự đặc biệt (`Ø→O`, `Æ→AE`, `ß→SS`, `Ł→L`, `Đ→D`, `İ/ı→I`, `Œ→OE`), tách dấu bằng NFD rồi bỏ toàn bộ dấu phụ.
3. Bỏ dấu nháy (`'` `’`) và dấu chấm (`.`); đổi gạch nối (`-`) thành dấu cách (`Aaron Wan-Bissaka` → `WAN BISSAKA`).
4. Viết hoa còn lại `A–Z` và một dấu cách giữa các từ:
   - `word`: giữ dấu cách để hiển thị hình dạng tên (`VAN DIJK`).
   - `letters`: bỏ dấu cách để chấm (`VANDIJK`, độ dài 7).
5. Nếu cầu thủ có dòng trong `wordle_name_overrides`, dùng `word` ghi đè (không bỏ từ đầu) rồi tính lại `letters`.

**Đáp án & Từ điển:**
- **Đáp án daily:** cầu thủ đang thuộc đúng 1 CLB Premier League 2026/27 vào ngày tạo câu đố, `fc27_overall >= 75`, độ dài `letters` từ **4 đến 11**. Chọn theo chu kỳ không lặp (hết chu kỳ mới trộn chu kỳ mới và tránh lặp lại đáp án ngày hôm trước).
- **Từ điển đoán:** mọi cầu thủ đang thuộc đúng 1 CLB Premier League 2026/27 (không giới hạn OVR) có độ dài `letters` bằng độ dài đáp án (không yêu cầu cùng số từ). Trùng `letters` thì giữ người có OVR cao hơn khi hiển thị tên.

## 3. Bảng dữ liệu

Schema H2 local/test: [wordle-schema.sql](../backend/src/main/resources/wordle-schema.sql).  
Migration MySQL production (chỉ chạy sau khi backup & rà soát ở bước 3): [2026-11-28-wordle-mysql.sql](../backend/sql/2026-11-28-wordle-mysql.sql) và preflight [2026-11-28-wordle-preflight.sql](../backend/sql/2026-11-28-wordle-preflight.sql).

| Bảng | Mục đích |
|---|---|
| `wordle_name_overrides` | Ghi đè thủ công (`player_id` PK, `word`, `note`). |
| `wordle_selector` | Trạng thái chu kỳ không lặp cho mùa 2026 (`season` PK, `cycle_json`). |
| `wordle_puzzles` | Câu đố từng ngày (`id`, `puzzle_date` UNIQUE, `puzzle_number` UNIQUE, `answer_player_id`, `answer_word`, `answer_letters`, `created_at`). |
| `wordle_games` | Ván chơi của từng tài khoản (`id`, `account_id`, `puzzle_id`, `status`, `hint_used`, `started_at`, `finished_at`, UNIQUE `(account_id, puzzle_id)`). |
| `wordle_guesses` | Từng lượt đoán 1–6 (`game_id`, `turn`, `guessed_word`, `guessed_letters`, `player_id`, `feedback`, `created_at`, PK `(game_id, turn)`, UNIQUE `(game_id, guessed_letters)`). |

## 4. API (`/api/minigame/2026/wordle`)

Bật bằng cờ `premierhub.wordle.enabled=true` (biến môi trường `PREMIERHUB_WORDLE_ENABLED`, mặc định `false` — khi tắt trả **404**). Mọi endpoint yêu cầu đăng nhập + header `X-PrismaXI-Account-ID` (POST kèm CSRF).

| Endpoint | Mô tả |
|---|---|
| `GET /today` | Lấy câu đố hôm nay và ván của tài khoản (chưa có thì tạo ván `PLAYING` rỗng). |
| `POST /today/guesses` | Body `{"letters":"BARCOLA"}` (tùy chọn `date`). Trả lại toàn bộ trạng thái ván. |
| `POST /today/hint` | Mở gợi ý CLB sau khi đã đoán đủ 4 lượt. |
| `GET /dictionary` | Danh sách `[{ letters, word, name, club }]` cùng độ dài đáp án hôm nay (có cache theo `puzzleId`, không chứa ID/cờ đáp án). |
| `GET /stats` | Thống kê cá nhân của tài khoản. |

**Mã lỗi chuẩn:** `WRONG_LENGTH` (422), `INVALID_CHARACTERS` (422), `NOT_A_PLAYER` (422), `ALREADY_GUESSED` (422), `HINT_LOCKED` (422), `HINT_USED` (409), `GAME_FINISHED` (409), `SESSION_CHANGED` (409), `AUTH_REQUIRED` (401).

## 5. Chạy local trên H2

Profile `minigame-local` đã nạp sẵn `wordle-schema.sql` và bật `premierhub.wordle.enabled=true` cùng dữ liệu snapshot H2:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=minigame-local"
```

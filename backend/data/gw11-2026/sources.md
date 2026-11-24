# Fantasy GW11 mùa 2026/27 — đã mở production ngày 09/11/2026

Người dùng yêu cầu thu thập đủ GW10, bỏ qua công bố kết quả GW10 và bắt đầu GW11. Việc mở GW11 được cho phép riêng trong yêu cầu này; không cần mở bù GW10.

## Lịch và cấu hình đã xác nhận

- Đã đối chiếu đủ 10 fixture GW11 với PremierHub và endpoint riêng football-data.org; tất cả có kickoff UTC, provider TIMED/SCHEDULED và chưa bắt đầu.
- Trận đầu Manchester City–Fulham: nội bộ **1000560643**, provider **560643**, kickoff **21/11/2026 12:30 UTC = 19:30 Việt Nam**.
- Deadline theo `GameweekService.calculateDeadline`: **19/11/2026 17:00 UTC = 20/11/2026 00:00 Việt Nam**.
- Mốc roster: **09/11/2026**; API trả **564 cầu thủ duy nhất**, đã đối chiếu membership đúng ngày cho từng người. Hai hồ sơ tối thiểu mới có OVR/primaryPosition/eligiblePositions trống đúng quyền đã cho, không tự bổ sung quyền chơi Fantasy.
- Đã mở bằng `GameweekService.publishDeadline(11,2,reason,2026-11-09)` lúc UTC **09/11/2026 16:02:27.251919**, commit **16:02:28**. ADMIN ID 2 được đối chiếu role hiện hành; cấu hình + audit dùng transaction SERIALIZABLE cùng batch GW10. Trạng thái **OPEN/canEdit=true**, revision **1**. Lặp bị từ chối **409** và giữ nguyên deadline/roster/cấu hình.
- MySQL và API xác nhận recommendedGameweek=11, GW10 configured=false, không có công bố kết quả GW10/GW11. Đội nháp, đội đã chốt, cấu hình/deadline/rosterAsOf và kết quả các vòng cũ khớp snapshot trước nhập.

## Nguồn và kiểm tra

[Lịch GW11](https://premierhub.vercel.app/api/matches?season=2026&matchweek=11), [cấu hình Fantasy](https://premierhub.vercel.app/api/fantasy/2026/gameweeks), [roster GW11](https://premierhub.vercel.app/api/fantasy/2026/gameweeks/11/players).

Backup và kiểm tra H2 trước ghi dùng cùng batch với [GW10](../gw10-2026/sources.md). H2 mở đúng OPEN/canEdit, revision=1, deadline/roster; gọi lặp bị 409 và giữ cấu hình. Không tạo đội thay người dùng, không sao chép đội vòng cũ.

Chrome production không đăng nhập ở 1440px và 390px: Vòng thi mặc định **GW11**, deadline **20/11/2026 · 00:00**, roster **09/11/2026**. Tab đội tiêu biểu mặc định GW11 và vẫn chọn xem GW10/GW7; riêng GW10 có đủ 11 người và tổng 92,40 khớp API. Không tràn ngang/lỗi JavaScript/request ghi. Các ảnh/report nằm trong `backend/target/gw10-collection/`, Git ignore.

## Thu thập đợt 1 — Chelsea FC 1–4 Leeds United FC (22/11/2026)

> **Lưu ý dữ liệu tạm (provisional):** Số liệu FPL lấy khi trận mới `finished_provisional = true`, chưa `finished = true`; người dùng đồng ý ngày **22/11/2026**.

- **Trận đã thu thập:** `1000560642` (football-data `560642`, FPL `104`) — **Chelsea FC (`1000000061`) 1–4 Leeds United FC (`1000000341`)**, kickoff `2026-11-21T15:00:00Z` (22:00 giờ Việt Nam).
- **Trạng thái nguồn lúc thu thập (22/11/2026 ~02:13 giờ Việt Nam / 21/11/2026 ~19:13 UTC):**
  - football-data.org: `FINISHED`, tỉ số `1–4` (đồng bộ 20 CLB / 380 fixture / 20 BXH).
  - FPL API (`fixtures/?event=11`, `event/11/live/`, `bootstrap-static/`): `finished=false`, `finished_provisional=true`, tỉ số `1–4`; thu thập bằng `--fixture-ids=1000560642 --allow-provisional=true` (ghi `fpl_status=PROVISIONAL` trong `fpl/fixture-map.csv`).
  - Ảnh SofaScore (2 ảnh người dùng cung cấp ngày 22/11/2026: ảnh 1 sân 11v11 + rating đá chính, ảnh 2 dự bị vào sân/không vào sân + rating).
- **Tổng hợp cầu thủ trận `1000560642` (40 dòng):**
  - **Chelsea FC (20 người, `4-2-3-1`):** 16 `PLAYED` (11 đá chính + 5 dự bị vào sân), 4 `DID_NOT_PLAY`, 0 `UNRATED`. Ghi bàn: Emmanuel Emegha (1); kiến tạo: Morgan Rogers (1); thẻ vàng: Romeo Lavia (1); thẻ đỏ: 0.
  - **Leeds United FC (20 người, `3-4-2-1`):** 16 `PLAYED` (11 đá chính + 5 dự bị vào sân, gồm **2 `UNRATED`**: Harry Wilson `2000020066` 1 phút và Melvin Bard `2000030225` 1 phút), 4 `DID_NOT_PLAY`. Ghi bàn: Noah Okafor (2), Dominic Calvert-Lewin (1), Brenden Aaronson (1) — tổng 4 bàn (0 phản lưới); kiến tạo theo định nghĩa FPL: Jayden Bogle (1), Harry Wilson (1), Dominic Calvert-Lewin (1), Ethan Ampadu (1); thẻ vàng: Jayden Bogle (1), Sean Longstaff (1), Gabriel Gudmundsson (1); thẻ đỏ: 0.
  - **Kiểm tra đối chiếu:** `unmapped.csv` = 0, `bench-cards.csv` = 0, `missing.csv` = 0, `anomalies.csv` = 0.
- **File tạo ra cho đợt 1:**
  - `backend/data/gw11-2026/fpl/raw/{bootstrap-static.json,fixtures-gw11.json,event-gw11-live.json}`
  - `backend/data/gw11-2026/fpl/{fixture-map.csv,unmapped.csv,bench-cards.csv,fpl-match-stats-2026-GW11.csv,fpl-extra-stats-2026-GW11.csv}`
  - `backend/data/gw11-2026/{screenshot-evidence.csv,manual-match-stats-2026-GW11.csv,confirmed-unrated.csv,confirmed-not-registered.csv,missing.csv,anomalies.csv}`
  - `backend/data/gw11-2026/lineups/{clubs.csv,formations.csv,players.csv}` (kế thừa 100 quan sát GW6–GW10 + thêm 2 quan sát GW11 Chelsea/Leeds = 102).
- **Trạng thái kiểm tra & nhập:**
  - **H2 cô lập (`backend/target/gw11-che-lee-h2`):** sync football-data `FINISHED 1–4`, nhập lần 1 (`manual-match-stats` +40, `extra-stats` +32, `lineups` +40 vai trò/102 sơ đồ, `fantasy-evidence` +1 fixture/+2 unrated), chạy lặp lần 2 tất cả đều `0`; kiểm tra giao diện local 1440px đạt (tab Đội hình, Thống kê, thẻ cầu thủ Noah Okafor, 0 lỗi JS).
  - **Backup Aiven trước ghi (ngoài Git):** `backend/local-backups/gw11-che-lee-20261121-192153/aiven-before-gw11-che-lee-20261121-192153.sql` (`3.300.875` byte, 44 bảng, SHA-256 `7755d622345d605092d652ccc62549d8960d57eb023d6a29e377c81c401aaba2`).
  - **Production (Aiven `defaultdb`, MySQL 8.4.11, UUID `71f8960b-df63-11f1-abd5-5e7cbc02b771`):**
    - Đồng bộ football-data.org (`3` request, 20 CLB/380 trận/20 BXH): `1000560642` chuyển `FINISHED`, `1–4`.
    - Lần 1: `manual-match-stats` `inserted=40`, `manual-extra-stats` `inserted=32`, `match-lineups` `clubChanges=2 fixtureChanges=4 playerChanges=40`, `fantasy-evidence` `fixtures=1 fixture_changes=1 unrated_changes=2 absence_changes=0`.
    - Lần 2 (idempotent): tất cả báo `inserted=0` / `changes=0`.
    - Đối chiếu MySQL sau nhập: `manual_fixture_player_stats` mùa 2026 = `4.040` (GW1–GW10 vẫn `4.000` nguyên vẹn; trận `1000560642` = `40`); `manual_fixture_player_extra_stats` mùa 2026 = `3.112` (GW1–GW10 vẫn `3.080` nguyên vẹn; trận `1000560642` = `32`); `fixture_lineups` = `102`; `fixture_lineup_players` = `2.040`; `fantasy_fixture_confirmations` = `51`; `fantasy_unrated_confirmations` = `28`.
    - Tổng mùa `player_season_stats`: giữ nguyên qua GW10 (chưa đủ 10 trận GW11).
    - Fantasy GW11: `AWAITING_RESULTS`, deadline `2026-11-19T17:00:00Z`, `rosterAsOf=2026-11-09`, chưa công bố kết quả.
- **9 trận GW11 còn lại chưa thu thập:** `1000560643` (Man City–Fulham), `1000560644` (Aston Villa–Sunderland), `1000560645` (Liverpool–Man Utd), `1000560646` (Hull–Brighton), `1000560647` (Bournemouth–Nott'm Forest), `1000560648` (Tottenham–Ipswich), `1000560649` (Brentford–Everton), `1000560650` (Coventry–Crystal Palace), `1000560651` (Newcastle–Arsenal).

## Thu thập đợt 2 — 6/8 trận GW11 đã nhập, 2 trận chờ hồ sơ dự bị mới (23/11/2026)

> **Lưu ý dữ liệu tạm (provisional):** Cả 8 trận đều ở trạng thái `finished = false`, `finished_provisional = true` trên FPL tại thời điểm thu thập (23/11/2026 ~13:32 giờ Việt Nam); thu thập với `--allow-provisional=true` (ghi `fpl_status=PROVISIONAL` trong `fpl/fixture-map.csv`), cần đối chiếu lại khi FPL chốt `finished = true`.

- **Đồng bộ football-data.org (23/11/2026):** `3` request, 20 CLB / 380 trận / 20 BXH. Cả 9 trận đã đá của GW11 (`1000560642..1000560648, 1000560650, 1000560651`) đều chuyển `FINISHED` với tỉ số chính thức (riêng `1000560651` Newcastle–Arsenal hết kẹt `LIVE` và chuyển `FINISHED 2–2`); `1000560649` (Brentford–Everton) giữ `SCHEDULED`.
- **Đối chiếu 2 cầu thủ trùng ID:**
  - **Iliman Ndiaye** (`1000560643` Man City–Fulham): FPL collector gắn đúng ID hiện hành `2000004005` (Manchester City FC, membership `[2026-09-03, NULL)`), không dùng ID cũ `2000015003`.
  - **Chema Andrés** (`1000560646` Hull–Brighton): FPL collector gắn đúng ID hiện hành `2000030174` (José María Andrés Baixauli, Brighton, membership `[2026-09-30, NULL)`), không dùng ID cũ `2000030259`.
- **6 trận đã thu thập & nhập production đầy đủ (`240` dòng thống kê mới, `191` dòng chỉ số mở rộng mới, `12` sơ đồ/đội hình mới, `7` confirmed-unrated mới):**
  - `1000560645` (FPL `107`) — **Liverpool FC 4–0 Manchester United FC** (22/11, ảnh 3–4): 32 `PLAYED`, 8 `DID_NOT_PLAY`, 1 `UNRATED` (Andrey Santos `2000006016`, 3'). Sơ đồ `4-2-3-1` / `4-2-3-1`.
  - `1000560646` (FPL `106`) — **Hull City AFC 2–0 Brighton & Hove Albion FC** (22/11, ảnh 17–18): 32 `PLAYED`, 8 `DID_NOT_PLAY`, 1 `UNRATED` (Ibrahim Osman `2000030052`, 1'). Sơ đồ `4-2-3-1` / `4-2-3-1`.
  - `1000560647` (FPL `102`) — **AFC Bournemouth 2–0 Nottingham Forest FC** (21/11, ảnh 9–10): 32 `PLAYED`, 8 `DID_NOT_PLAY`, 1 `UNRATED` (Ben Gannon-Doak `2000030034`, 1'). Sơ đồ `4-2-3-1` / `3-4-2-1`.
  - `1000560648` (FPL `110`) — **Tottenham Hotspur FC 2–0 Ipswich Town FC** (21/11, ảnh 13–14): 32 `PLAYED`, 8 `DID_NOT_PLAY`, 1 `UNRATED` (Archie Gray `2000030017`, 1'). Sơ đồ `4-2-3-1` / `4-2-3-1`.
  - `1000560650` (FPL `105`) — **Coventry City FC 3–2 Crystal Palace FC** (21/11, ảnh 11–12): 31 `PLAYED`, 9 `DID_NOT_PLAY`, 1 `UNRATED` (Darío Osorio `2000030189`, 1'). Ghi bàn: Sakamoto (1), Awoniyi (1), McNeil (OG 1) / Pino (1), Strand Larsen (1). Sơ đồ `4-2-3-1` / `4-2-3-1`.
  - `1000560651` (FPL `109`) — **Newcastle United FC 2–2 Arsenal FC** (21/11, ảnh 15–16): 32 `PLAYED`, 8 `DID_NOT_PLAY`, 2 `UNRATED` (Mikel Merino `2000001017` 1', Viktor Gyökeres `2000001012` 1'). Sơ đồ `4-2-3-1` / `4-2-3-1`.
  - Kiểm tra đối chiếu 6 trận: `unmapped.csv` = 0, `bench-cards.csv` = 0, `anomalies.csv` = 0, tổng bàn + phản lưới khớp tỉ số 6/6 trận.
- **2 trận tạm bỏ qua nhập thống kê/đội hình (chờ quyền tạo hồ sơ tối thiểu cầu thủ dự bị mới):**
  - `1000560643` (FPL `108`) — **Manchester City FC 1–1 Fulham FC** (21/11, ảnh 5–6): đủ ảnh và tỉ số khớp (31 `PLAYED`, 9 `DID_NOT_PLAY`, 0 `UNRATED`), nhưng dự bị không vào sân **#37 Dino Kaiser** (Fulham FC `1000000063`, FPL `element_id=674`, `GOALKEEPER`) chưa có trong bảng `players`/`player_season_stats`/`manual_player_memberships`. Đã ghi vào `backend/data/fpl-2026/player-map-pending.csv` và `backend/data/gw11-2026/missing.csv`.
  - `1000560644` (FPL `101`) — **Aston Villa FC 0–0 Sunderland AFC** (21/11, ảnh 7–8): đủ ảnh và tỉ số khớp (27 `PLAYED`, 13 `DID_NOT_PLAY`, 0 `UNRATED`), nhưng dự bị không vào sân **#8 Alan Browne** (Sunderland AFC `1000000071`, FPL `element_id=673`, `MIDFIELDER`) chưa có trong bảng `players`/`player_season_stats`/`manual_player_memberships`. Đã ghi vào `backend/data/fpl-2026/player-map-pending.csv` và `backend/data/gw11-2026/missing.csv`.
- **Trận còn lại chưa đá:** `1000560649` — **Brentford FC – Everton FC** (24/11/2026, `SCHEDULED`).
- **Trạng thái kiểm tra & nhập đợt 2:**
  - **H2 cô lập (`backend/target/gw11-batch2-h2`):** sync football-data đạt; nhập lượt 1 (`manual-match-stats` +240, `manual-extra-stats` +191, `match-lineups` `clubChanges=12 fixtureChanges=24 playerChanges=240`, `fantasy-evidence` `fixture_changes=6 unrated_changes=7`); nhập lượt 2 tất cả đều `0`.
  - **Backup Aiven mới trước ghi (ngoài Git):** `backend/local-backups/gw11-batch2-20261123-071001/aiven-before-gw11-batch2-20261123-071001.sql` (`3.376.633` byte, 44 bảng, `Dump completed` OK, SHA-256 `cf8c1deadd36670ab1d555b5267f70a789b57c5e8f2c718e890da2f3b26860dd`).
  - **Production (Aiven `defaultdb`, MySQL 8.4.11, UUID `71f8960b-df63-11f1-abd5-5e7cbc02b771`):**
    - Sync football-data: 20 CLB / 380 trận / 20 BXH.
    - Lần 1: `manual-match-stats` `inserted=240`, `manual-extra-stats` `inserted=191`, `match-lineups` `clubChanges=12 fixtureChanges=24 playerChanges=240`, `fantasy-evidence` `fixtures=7 fixture_changes=6 unrated_changes=7 absence_changes=0`.
    - Lần 2 (idempotent): tất cả `inserted=0` / `changes=0`.
    - Đối chiếu MySQL sau nhập: GW1–GW10 vẫn nguyên vẹn (`4.000` stats / `3.080` extra), Chelsea–Leeds vẫn nguyên vẹn (`40` stats / `32` extra); tổng mùa 2026 = `4.280` stats, `3.303` extra, `114` lineups, `2.280` lineup_players, `57` fixture confirmations, `35` unrated confirmations. Fantasy GW11 chưa công bố kết quả, `gw11_picks = 0`.

## Thu thập đợt 3 — Tạo 2 hồ sơ tối thiểu & nhập nốt Man City–Fulham, Aston Villa–Sunderland (23/11/2026)

Người dùng đã duyệt ngày **23/11/2026** tạo 2 hồ sơ tối thiểu (không thêm OVR, vị trí Fantasy chi tiết, giá hay ảnh) cho 2 cầu thủ dự bị không vào sân và nhập nốt 2 trận còn lại đã đá của GW11:

- **2 hồ sơ tối thiểu đã tạo qua `ManualRosterCommand` (`backend/data/gw11-2026/manual-memberships-current-2026-GW11.csv`):**
  - **Dino Kaiser** (`2000030265`, Fulham FC `1000000063`, `GOALKEEPER`, membership `[2026-11-21, NULL)`, số áo `37` trên ảnh lineup GW11, FPL `element_id=674`). Đã chuyển từ `player-map-pending.csv` sang `player-map.csv` (nguồn `gw11-lineup-and-fpl-bootstrap`).
  - **Alan Browne** (`2000030266`, Sunderland AFC `1000000071`, `MIDFIELDER`, membership `[2026-11-21, NULL)`, số áo `8` trên ảnh lineup GW11, FPL `element_id=673`). Đã chuyển từ `player-map-pending.csv` sang `player-map.csv` (nguồn `gw11-lineup-and-fpl-bootstrap`).
  - Kiểm tra trước khi nhập: cả 2 ID `2000030265` và `2000030266` chưa tồn tại trong `players`. Sau khi nhập: `player_season_profiles = 0` và `player_specific_positions = 0` cho cả 2 cầu thủ.
- **2 trận đã nhập thêm (`80` dòng thống kê mới, `58` dòng chỉ số mở rộng mới, `4` sơ đồ/đội hình mới, `0` unrated mới, dữ liệu FPL ở trạng thái `PROVISIONAL`):**
  - `1000560643` (FPL `108`) — **Manchester City FC 1–1 Fulham FC** (21/11, ảnh 5–6): 31 `PLAYED`, 9 `DID_NOT_PLAY`, 0 `UNRATED`; sơ đồ `4-2-3-1` / `4-4-2`.
  - `1000560644` (FPL `101`) — **Aston Villa FC 0–0 Sunderland AFC** (21/11, ảnh 7–8): 27 `PLAYED`, 13 `DID_NOT_PLAY`, 0 `UNRATED`; sơ đồ `4-2-3-1` / `4-2-3-1`.
  - Kiểm tra đối chiếu: `unmapped.csv = 0`, `bench-cards.csv = 0`, `anomalies.csv = 0`. `missing.csv` đã xóa 2 dòng trên, chỉ còn 1 dòng `1000560649` (Brentford – Everton chưa đá).
- **Trạng thái kiểm tra & nhập đợt 3:**
  - **H2 cô lập (`backend/target/gw11-batch2-h2`):** lượt 1 (`manual-roster` +2/+2/+2, `manual-match-stats` +80, `manual-extra-stats` +58, `match-lineups` `clubChanges=4 fixtureChanges=8 playerChanges=80`, `fantasy-evidence` `fixture_changes=2 unrated_changes=0`), lượt 2 tất cả đều `0`.
  - **Backup Aiven mới trước ghi (ngoài Git):** `backend/local-backups/gw11-batch3-20261123-073706/aiven-before-gw11-batch3-20261123-073706.sql` (`3.443.189` byte, 44 bảng, `Dump completed` OK, SHA-256 `88994f7a6ea9ee8b44bddc0cf2dde9bd98e9864fc64cdda9ef571572dea28928`).
  - **Production (Aiven `defaultdb`, MySQL 8.4.11, UUID `71f8960b-df63-11f1-abd5-5e7cbc02b771`):**
    - Lần 1: `manual-roster` `playersInserted=2 membershipsInserted=2 intervalsInserted=2 intervalsUpdated=0`, `manual-match-stats` `rows=360 inserted=80`, `manual-extra-stats` `rows=281 inserted=58`, `match-lineups` `clubChanges=4 verifiedFormations=118 fixtureChanges=8 players=360 playerChanges=80`, `fantasy-evidence` `fixtures=9 fixture_changes=2 unrated_changes=0 absence_changes=0`.
    - Lần 2 (idempotent): tất cả đều `0`.
    - Đối chiếu MySQL sau nhập: GW1–GW10 nguyên vẹn (`4.000` stats / `3.080` extra), Chelsea–Leeds nguyên vẹn (`40` / `32`), 6 trận đợt 2 nguyên vẹn (`240` / `191`); tổng mùa 2026 = `4.360` stats, `3.361` extra, `118` lineups, `2.360` lineup players, `59` fixture confirmations, `35` unrated confirmations. Fantasy GW11 chưa công bố kết quả.

## Thu thập đợt 4 — Brentford FC 4–0 Everton FC (24/11/2026) — hoàn tất 10/10 trận GW11

- **Trận đã thu thập:** `1000560649` (football-data `560649`, FPL `103`) — **Brentford FC (`1000000402`) 4–0 Everton FC (`1000000062`)**, kickoff `2026-11-23T20:00:00Z` (03:00 giờ Việt Nam ngày 24/11/2026).
- **Trạng thái nguồn lúc thu thập (24/11/2026 ~21:55 giờ Việt Nam):**
  - FPL API (`fixtures/?event=11`, `event/11/live/`, `bootstrap-static/`): `started=true`, `finished_provisional=true`, `finished=true` (`fpl_status=FINISHED` trong `fpl/fixture-map.csv`), tỉ số `4–0`. Bản raw JSON lưu riêng tại `backend/data/gw11-2026/fpl/raw-1000560649/` để không ảnh hưởng snapshot `raw/` của các đợt trước.
  - Ảnh SofaScore (2 ảnh người dùng gửi ngày 24/11/2026: ảnh 19 sơ đồ 11v11 + rating đá chính, ảnh 20 danh sách thay người/dự bị + rating). **3 điểm trong ảnh khác bản chép tay của người dùng (đã nhập theo ảnh):**
    - `M. Kayode` (`2000030004`, #33 Brentford): bản chép ghi `8.3` → ảnh là thẻ cam **`6.3`**.
    - `J. O'Brien` (`2000020112`, #15 Everton): bản chép ghi `8.5` → ảnh là thẻ vàng **`6.5`**.
    - `H. Armstrong` (`2000015004`, #45 Everton): bản chép ghi `8.5` → ảnh là thẻ vàng **`6.5`**.
- **Hồ sơ tối thiểu:** `0` — toàn bộ 40 cầu thủ (20 Brentford + 20 Everton) đã có sẵn trong `players`, `player_season_stats` và `manual_player_memberships` đang mở (`end_date IS NULL`).
- **Tổng hợp cầu thủ trận `1000560649` (40 dòng):**
  - **Brentford FC (20 người, `4-2-3-1`):** 16 `PLAYED` (11 đá chính + 5 dự bị vào sân), 4 `DID_NOT_PLAY`, 0 `UNRATED`. Ghi bàn: K. Lewis-Potter (2), V. Janelt (1), F. Carvalho (1); kiến tạo: J. Anthony (2), M. Jensen (1); thẻ vàng/đỏ: 0.
  - **Everton FC (20 người, `4-2-3-1`):** 16 `PLAYED` (11 đá chính + 5 dự bị vào sân), 4 `DID_NOT_PLAY`, 0 `UNRATED`. Thẻ vàng: V. Mykolenko (1); thẻ đỏ: J. Tarkowski (1).
  - **Đối chiếu Fantasy GW11:** `fantasy_submitted_picks` cho GW11 = `0`, không phát sinh `confirmed-not-registered.csv`; `missing.csv` = `0` dòng (đủ 10/10 trận GW11).
- **Trạng thái kiểm tra & nhập đợt 4 (nhập vòng thường — không bắt buộc backup Aiven theo quy tắc 23/11/2026):**
  - **H2 cô lập (2 lượt):** lượt 1 `MANUAL_MATCH_STATS inserted=40`, `MANUAL_EXTRA_STATS inserted=32`, `MATCH_LINEUPS clubChanges=2 fixtureChanges=4 playerChanges=40`, `FANTASY_EVIDENCE fixture_changes=1`, `MANUAL_SEASON_STATS throughGW=11 fixtures=110 matchRows=4400 seasonRows=600 updated=0`; lượt 2 tất cả `0`.
  - **Production (Aiven `defaultdb`, 2 lượt):**
    - Lần 1: `MANUAL_MATCH_STATS rows=400 inserted=40`, `MANUAL_EXTRA_STATS rows=313 inserted=32`, `MATCH_LINEUPS clubs=20 clubChanges=2 verifiedFormations=120 fixtureChanges=4 players=400 playerChanges=40`, `FANTASY_EVIDENCE fixtures=10 fixture_changes=1 unrated_changes=0 absence_changes=0`, `MANUAL_SEASON_STATS throughGW=11 fixtures=110 matchRows=4400 seasonRows=599 updated=0 missingMinutes=1 missingGoals=2 missingAssists=4`.
    - Lần 2 (idempotent): toàn bộ `inserted=0` / `changes=0` / `updated=0`.
    - Đối chiếu MySQL sau nhập: GW1–GW10 nguyên vẹn (`4.000` stats / `3.084` extra); GW11 đủ `400` stats (`10` trận) / `313` extra (`10` trận); `fixture_lineups` = `120`; `fixture_lineup_players` = `2.400`; `fantasy_fixture_confirmations` = `60`. Tổng mùa `K. Lewis-Potter` (`2000030002`) cập nhật lên `11` trận / `846` phút / `4` bàn / `1` kiến tạo.
- **Đối chiếu bảng tạm FPL (`fpl_provisional_player_stats`, giữ nguyên không sửa/xóa):**
  - `fpl_provisional_player_stats` cho `1000560649` có `32` dòng (`fpl_status=FINAL_FPL`, `fetched_at=2026-11-24T14:49:24Z`), `fpl_provisional_unmapped` = `0`.
  - So sánh 13 cột (`minutes, goals, assists, yellow_cards, red_cards, saves, bonus, bps, expected_goals, expected_assists, tackles, clearances_blocks_interceptions, recoveries`) với `manual_fixture_player_stats` + `manual_fixture_player_extra_stats`: **32/32 cầu thủ khớp tuyệt đối, `0` sai lệch**, `0` cầu thủ ra sân bị thiếu ở bảng tạm.
- **Đội hình tiêu biểu GW11 (`/api/fantasy/2026/team-of-week?gameweek=11`):**
  - `status=COMPLETE`, sơ đồ `4-3-3`, tổng rating **`97.60`**, `completedFixtures=10`, `recordedFixtures=10`, `playedCount=313`, `ratedCount=304`, `candidateCount=283`, `excludedMissingPositions=21`, `excludedNullRatings=9`, `missingSlots=[]`, `conflicts=[]`.
  - 11 cầu thủ: GK Christian Walton (`2000020044`, IPS, `9.2`), LB Destiny Udogie (`2000030016`, TOT, `8.0`), LCB António Silva (`2000030035`, BOU, `8.2`), RCB James Hill (`2000030109`, BOU, `8.2`), RB Dominik Szoboszlai (`2000003008`, LIV, `8.8`), LCM Florian Wirtz (`2000003007`, LIV, `10.0`), CM Vitaly Janelt (`2000030003`, BRE, `9.2`), RCM Yehor Yarmoliuk (`2000030011`, BRE, `8.7`), LW Rio Ngumoha (`2000003024`, LIV, `10.0`), ST Oli McBurnie (`2000020008`, HUL, `8.7`), RW Noni Madueke (`2000001015`, ARS, `8.6`).
- **Kiểm tra website production (`https://premierhub.vercel.app`):**
  - `/api/matches/1000560649/details?season=2026` trả `dataStatus=CONFIRMED`, `evidenceStatus=MANUAL_VERIFIED`, sơ đồ `4-2-3-1` hai bên.
  - Giao diện trận `1000560649` bỏ nhãn vàng `Số liệu tạm`, mở khóa tab Đội hình (sơ đồ 4-2-3-1 + rating đầy đủ), tab Thống kê hiện `Rating trung bình` (`7.4` vs `6.5`), tab Tổng quan hiện `Cầu thủ xuất sắc nhất trận: K. Lewis-Potter (9.2)`.
  - Hồ sơ `K. Lewis-Potter` (`2000030002`): GW11 có `dataStatus=CONFIRMED`, không còn nhãn `Tạm`, tổng mùa `11/11` trận · `846'` · `4` bàn · `1` kiến tạo · rating TB `7.0`.
  - Fantasy GW11 giữ `status=AWAITING_RESULTS`, **chưa công bố**.

## Bước tiếp theo

1. **Đối chiếu lại 9 trận lấy từ trạng thái `PROVISIONAL`** (`1000560642..1000560648, 1000560650, 1000560651`) khi FPL đã chốt `finished = true` nếu cần; không tự ghi đè dữ liệu đã nhập.
2. **ADMIN tự công bố Fantasy GW11** (khi sẵn sàng). Chưa commit/push/deploy.

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

## Bước tiếp theo

1. **Đối chiếu lại trận `1000560642` khi FPL chốt `finished = true`:** chạy collector riêng vào thư mục tách biệt (ví dụ `backend/data/gw11-2026/fpl-recheck/`), so sánh từng ô với bản đã thu thập ở `fpl/` và báo chủ dự án mọi khác biệt (thường là `bonus`, `bps`, `influence`/`creativity`/`threat`/`ict_index`, hoặc `assists`). **Không tự ghi đè dữ liệu đã nhập.**
2. Khi 9 trận còn lại của GW11 đạt `FINISHED` (cả football-data.org và FPL `finished=true`) và người dùng cung cấp ảnh rating/lineups SofaScore:
   - Rà soát `backend/data/fpl-2026/player-map-pending.csv` và kiểm tra 2 cầu thủ trùng ID (Iliman Ndiaye, Chema Andrés — xem mục 6 `docs/fpl-collection-2026.md`) nếu họ ra sân.
   - Chạy collector + `--merge-only=true` với `--fixture-ids` cho các trận mới (tự gộp vào các CSV GW11 hiện có), đối chiếu tổng bàn/anomalies/missing.
   - Thử trên H2 → backup Aiven mới → nhập production phần còn lại, tái tính tổng mùa, đối chiếu Đội hình tiêu biểu GW11 và readiness theo [docs/gameweek-data-workflow.md](../../../docs/gameweek-data-workflow.md). Chưa công bố kết quả GW11; chưa commit/push/deploy.

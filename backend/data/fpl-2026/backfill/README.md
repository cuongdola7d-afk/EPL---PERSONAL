# Đợt bổ sung chỉ số mở rộng FPL — GW1–GW10 mùa 2026/27

> Ngày chuẩn bị: **21/11/2026**. Trạng thái: **đã chuẩn bị CSV + kiểm chứng 2 lần trên H2 tạm; chưa chạy migration và chưa nhập lên Aiven production.**

## Phạm vi và nguyên tắc

- **Chỉ bổ sung bảng `manual_fixture_player_extra_stats`** cho 100 trận GW1–GW10 mùa 2026/27.
- **Tuyệt đối không thay đổi `manual_fixture_player_stats`** (rating SofaScore, số phút/bàn/kiến tạo/thẻ StatMuse của GW1–GW10 giữ nguyên 100%). Không tạo `manual-match-stats-*.csv`; các file `fpl-match-stats-2026-GWNN.csv` trong thư mục này **chỉ để đối chiếu tham khảo**, không dùng để nhập.
- Các thư mục vòng cũ (`gw10-2026/`, `gw2-first-two/`, ...) không bị sửa đổi.

## Cấu trúc mỗi thư mục `gw01/` .. `gw10/`

- `raw/`: JSON gốc từ FPL API (`bootstrap-static.json`, `fixtures-gwNN.json`, `event-gwNN-live.json`), thu thập tuần tự qua `FplCollectCommand`.
- `fixture-map.csv`: bảng đối chiếu 1-1 `fpl_fixture_id ↔ fixture_id` theo `(home_club_id, away_club_id)` toàn mùa + ngày UTC (đủ 100/100 trận GW1–GW10, không có trận bị FPL dời sang `event` khác).
- `unmapped.csv`: cầu thủ FPL `minutes > 0` chưa có trong `player-map.csv`.
- `bench-cards.csv`: cầu thủ `minutes = 0` nhận thẻ (trống cho cả 10 vòng).
- `consistency.csv`: kết quả đối chiếu với `manual_fixture_player_stats` (loại bỏ các dòng chưa ghép mã hoặc không khớp `PLAYED`).
- `fpl-extra-stats-2026-GWNN.csv`: **CSV sẵn sàng nhập** vào `manual_fixture_player_extra_stats` (chỉ gồm dòng có cả `manual_fixture_player_stats.participation_status = 'PLAYED'` lẫn FPL `minutes > 0`; `club_id` lấy theo membership tại ngày đá lúc nhập).
- `fpl-match-stats-2026-GWNN.csv`: chỉ dùng để đối chiếu tham khảo.

## Tóm tắt kết quả chuẩn bị (21/11/2026)

| Vòng | Trận ghép | Dòng nhập (`extra`) | Bị loại (`UNMAPPED_PLAYER`) | Bị loại (`FPL_PLAYED_DB_NOT_PLAYED`) | Đối ứng DB (`DB_PLAYED_UNMAPPED_PLAYER`) | Ghi chú |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| GW01 | 10/10 | 309 | 0 | 1 | 1 | Trùng ID lịch sử Iliman Ndiaye (`2000015003` vs `2000004005`) |
| GW02 | 10/10 | 311 | 0 | 1 | 1 | Trùng ID lịch sử Iliman Ndiaye (`2000015003` vs `2000004005`) |
| GW03 | 10/10 | 307 | 0 | 0 | 0 | Sạch 100% |
| GW04 | 10/10 | 306 | 0 | 1 | 1 | Trùng ID lịch sử Chema Andrés (`2000030259` vs `2000030174`) |
| GW05 | 10/10 | 301 | 0 | 1 | 1 | Trùng ID lịch sử Chema Andrés (`2000030259` vs `2000030174`) |
| GW06 | 10/10 | 315 | 0 | 0 | 0 | Sạch 100% |
| GW07 | 10/10 | 305 | 0 | 0 | 0 | Sạch 100% |
| GW08 | 10/10 | 303 | 0 | 0 | 0 | Sạch 100% |
| GW09 | 10/10 | 310 | 0 | 0 | 0 | Sạch 100% |
| GW10 | 10/10 | 313 | 0 | 0 | 0 | Sạch 100% |
| **Tổng** | **100/100** | **3.080** | **0** | **4** | **4** | 3.080 / 3.084 lượt `PLAYED` (4 lượt trùng ID lịch sử đã tách rõ trong `consistency.csv`) |

## Trình tự nhập production (khi được cho phép)

1. Xác minh đích Aiven MySQL và tạo backup mới ngoài Git.
2. Chạy preflight `backend/sql/2026-11-21-fpl-extra-stats-preflight.sql` và migration `backend/sql/2026-11-21-fpl-extra-stats-mysql.sql`.
3. Chạy tuần tự `--premierhub.manual-extra-stats.enabled=true --premierhub.manual-extra-stats.file=data/fpl-2026/backfill/gwNN/fpl-extra-stats-2026-GWNN.csv` cho GW01..GW10 (với `--spring.profiles.active=prod --spring.sql.init.mode=never`).
4. Chạy lặp lần 2 để xác nhận `inserted=0` và kiểm tra tổng/checksum `manual_fixture_player_stats` không đổi.

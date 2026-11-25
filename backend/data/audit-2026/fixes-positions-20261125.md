# Biên bản nhập vị trí 36 cầu thủ mùa 2026/27 (dọn dữ liệu bước 3 — 25/11/2026)

## Nguồn & phạm vi
- Đầu vào: `backend/data/audit-2026/positions-proposal-20261125-1738.csv` (commit `6e64e8b`) + 10 điều chỉnh đã duyệt từ người dùng (ngày 25/11/2026).
- File chốt đã nhập: `backend/data/audit-2026/positions-final-20261125-1808.csv` (`36` cầu thủ).
- Importer: `PlayerPositionImporter` (`--premierhub.player-positions.enabled=true`).
- Không thay đổi `player_season_stats.position` của bất kỳ cầu thủ nào (SHA-256 cột `position` toàn mùa 2026 trước và sau giữ nguyên `50eab30a8e934b55`).

## 10 điều chỉnh áp dụng so với file đề xuất
| `player_id` | Cầu thủ | Đề xuất cũ | Chốt (`primary → eligible`) | Lý do |
|---:|---|---|---|---|
| `2000030060` | Alejandro Garnacho | `CAM → CAM` | `LW → LW\|CAM` | Tiền đạo cánh trái; lần vào sân thay McGinn chỉ là thay người theo phút |
| `2000020012` | Cody Drameh | `CB → CB` | `RB → RB` | Hậu vệ phải |
| `2000030114` | Juanlu Sánchez | `LW → LW\|RB` | `RB → RB\|RW` | Hậu vệ hoặc tiền vệ cánh phải, không đá cánh trái |
| `2000020013` | Liam Millar | `CM → CM` | `LW → LW\|LM` | Tiền đạo cánh |
| `2000030021` | Mikey Moore | `CM → CM` | `LW → LW\|RW` | Tiền đạo cánh |
| `2000020051` | Romaine Mundle | `CM → CM` | `LW → LW\|LM` | Tiền đạo cánh trái |
| `2000020060` | Ola Aina | `CB → CB\|RB` | `RB → RB\|CB` | Hậu vệ phải là vị trí chính |
| `2000030044` | Georginio Rutter | `ST → ST` | `ST → ST\|CAM` | Hay đá lùi |
| `2000005021` | Mahdi Nicoll-Jazuli | `CM → CM` | `CM → CM` | Người dùng xác nhận trên SofaScore |
| `2000030053` | Zadok Yohanna | `CM → CM` | `RW → RW\|RM` | Người dùng xác nhận trên SofaScore: RM/RW |

## Backup & thực thi
- Backup Aiven: `backend/local-backups/nhap-vi-tri-20261125-180626/aiven-nhap-vi-tri-20261125-180626.sql` (`3,491,289` bytes, `49` bảng, SHA-256 `8a6a6bb2f2ec8adcbdc999e1b1f2d9debcf211c1c33ae798c17cafab70cec0d2`).
- H2 (dựng từ dump mới nhất):
  - Trước: `player_specific_positions=532`, `player_eligible_positions=989`, `player_season_stats=599`.
  - Lượt 1: `rows=36 inserted=36 updated=0` → `specific=568 (+36)`, `eligible=1046 (+57)`.
  - Lượt 2: `rows=36 inserted=0 updated=0` (idempotent).
- Production (Aiven):
  - Đối chiếu trước nhập: `expected_mismatches=0` (khớp 36/36 `player_id, name, club, position_group`), `played_missing_gw6_11=26`.
  - Lượt 1: `rows=36 inserted=36 updated=0` → `specific=568 (+36)`, `eligible=1046 (+57)`, `played_missing_gw6_11=0`.
  - Lượt 2: `rows=36 inserted=0 updated=0` (idempotent).
  - Không có dòng nào bị importer từ chối.

## Kiểm tra sau nhập trên production
- Cầu thủ `PLAYED` thiếu vị trí GW6–GW11: `26 → 0`.
- Đội hình tiêu biểu GW6–GW11:
  - GW6–GW10: không đổi tổng điểm và 11 người (`excludedMissingPositions` đều giảm về `0`).
  - GW11: `97.6 → 98.8` (+1.2), `excludedMissingPositions: 21 → 0`; vào `LB` Keane Lewis-Potter (`9.2`), ra `LB` Destiny Udogie (`8.0`).
- BXH Fantasy đã công bố (tổng + GW6–GW11): giữ nguyên 100% (không đổi).
- API GW12 `/api/fantasy/2026/gameweeks/12/players` (`rosterAsOf=2026-11-25`, tổng `566` cầu thủ):
  - `27/36` cầu thủ có mặt trong roster GW12 và cả `27/27` đã có `positionStatus=VERIFIED` cùng đầy đủ `primaryPosition`/`eligiblePositions`.
  - `9/36` cầu thủ không thuộc roster GW12 vì `manual_player_memberships` chỉ có các khoảng ngắn tháng 8/2026 (trước `2026-11-25`).
  - Lưu ý: cả `27/27` cầu thủ trong roster GW12 hiện có `fc27Overall=null` (hồ sơ tối thiểu), nên modal chọn cầu thủ trên UI Fantasy vẫn lọc ẩn do điều kiện bắt buộc có OVR (`1..99`) trong `lineup.js`.

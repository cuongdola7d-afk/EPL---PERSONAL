# Dữ liệu FPL ghi thật chọn lọc — GW11 (22/11/2026 – 23/11/2026)

- **Nguồn gốc:** Chép **nguyên văn (verbatim, không chỉnh sửa)** 32 file chọn lọc từ bộ 542 file ghi thật tại `D:\File Jva\fpl-live-capture\GW11-2026-11-22\fixtures\` (script `fpl-live-capture.ps1` gọi `https://fantasy.premierleague.com/api/fixtures/?event=11` mỗi 30 giây từ `20:55:01` ngày `22/11/2026` đến `01:28:02` ngày `23/11/2026` giờ Việt Nam `UTC+7`, 0 lỗi).
- **Trận đấu bao gồm:**
  1. **Hull City 2–0 Brighton & Hove Albion** (FPL `id = 106`, DB `fixtureId = 1000560646`, kickoff `14:00 UTC` / `21:00` giờ VN)
  2. **Liverpool 4–0 Manchester United** (FPL `id = 107`, DB `fixtureId = 1000560645`, kickoff `16:30 UTC` / `23:30` giờ VN)

## Danh sách 32 file thật và mốc sự kiện của từng file

| STT | Tên file | Giờ ghi (VN) | Trận | Mốc sự kiện / Trạng thái trong file | Có đổi state? |
| :-: | :--- | :---: | :---: | :--- | :-: |
| 1 | `20261122-205501.json` | 20:55:01 | Cả hai | Trước giờ bóng lăn (`id=106` và `id=107` đều `started=false, minutes=0`) | Không |
| 2 | `20261122-210105.json` | 21:01:05 | Hull – Brighton | **Bóng lăn**: `started=true, minutes=0, score=null-null` | Có (#1) |
| 3 | `20261122-210135.json` | 21:01:35 | Hull – Brighton | File giữa: giữ nguyên `minutes=0, score=null-null` | Không |
| 4 | `20261122-210507.json` | 21:05:07 | Hull – Brighton | Phút `2`: FPL cập nhật tỉ số `0–0` | Có (#2) |
| 5 | `20261122-212517.json` | 21:25:17 | Hull – Brighton | File giữa hiệp 1: `minutes=21`, tỉ số `0–0` | Có (#3) |
| 6 | `20261122-214828.json` | 21:48:28 | Hull – Brighton | **Chạm phút 45** (bù giờ hiệp 1 / nghỉ giữa hiệp): `minutes=45`, `0–0` | Có (#4) |
| 7 | `20261122-215803.json` | 21:58:03 | Hull – Brighton | File giữa giờ nghỉ giải lao: đứng yên ở `minutes=45`, `0–0` | Không |
| 8 | `20261122-221039.json` | 22:10:39 | Hull – Brighton | **Bắt đầu hiệp 2**: nhảy từ `45` lên `minutes=47`, `0–0` | Có (#5) |
| 9 | `20261122-223049.json` | 22:30:49 | Hull – Brighton | File giữa hiệp 2: `minutes=67`, tỉ số `0–0` | Có (#6) |
| 10 | `20261122-225300.json` | 22:53:00 | Hull – Brighton | Phút `89`: tỉ số `0–0` ngay trước bù giờ hiệp 2 | Có (#7) |
| 11 | `20261122-225330.json` | 22:53:30 | Hull – Brighton | **Chạm phút 90 + Thẻ đỏ Brighton**: `element=119` (Costinha, `playerId=2000030050`) | Có (#8) |
| 12 | `20261122-225601.json` | 22:56:01 | Hull – Brighton | **Bàn 1–0 Hull** ở phút `90` (bù giờ): `element=295` (Oli McBurnie, `playerId=2000020008`), kiến tạo `element=589` (Lucas Gourna-Douath, `playerId=2000020016`) | Có (#9) |
| 13 | `20261122-225702.json` | 22:57:02 | Hull – Brighton | File giữa bù giờ: giữ nguyên `minutes=90`, tỉ số `1–0` | Không |
| 14 | `20261122-225903.json` | 22:59:03 | Hull – Brighton | **Bàn 2–0 Hull** ở phút `90` (bù giờ): `element=295` (Oli McBurnie cú đúp `value=2`), kiến tạo `element=286` (Mohamed Belloumi, `playerId=2000020009`) | Có (#10) |
| 15 | `20261122-230104.json` | 23:01:04 | Hull – Brighton | **Hết trận (`FT`)**: `finished_provisional=true, minutes=90`, tỉ số `2–0` | Có (#11) |
| 16 | `20261122-231611.json` | 23:16:11 | Cả hai | File giữa 2 trận: Hull – Brighton đã `FT 2–0`, Liverpool – MU chưa đá | Không |
| 17 | `20261122-233018.json` | 23:30:18 | Liverpool – MU | **Bóng lăn**: `started=true, minutes=0, score=null-null` | Có (#12) |
| 18 | `20261122-233250.json` | 23:32:50 | Liverpool – MU | Phút `1`: FPL cập nhật tỉ số `0–0` | Có (#13) |
| 19 | `20261122-233953.json` | 23:39:53 | Liverpool – MU | **Thẻ đỏ MU** ở phút `8`: `element=420` (Leny Yoro, `playerId=2000006014`) | Có (#14) |
| 20 | `20261122-234154.json` | 23:41:54 | Liverpool – MU | **Bàn 1–0 Liverpool** ở phút `10`: `element=368` (Dominik Szoboszlai, `playerId=2000003008`), chưa có kiến tạo | Có (#15) |
| 21 | `20261122-234224.json` | 23:42:24 | Liverpool – MU | **Kiến tạo bàn 1** (30s sau, phút `10`): `element=379` (Alexander Isak, `playerId=2000003009`) | Có (#16) |
| 22 | `20261122-234456.json` | 23:44:56 | Liverpool – MU | **Bàn 2–0 Liverpool** ở phút `13`: `element=366` (Florian Wirtz, `playerId=2000003007`), chưa có kiến tạo | Có (#17) |
| 23 | `20261122-234556.json` | 23:45:56 | Liverpool – MU | **Kiến tạo bàn 2** (60s sau, phút `14`): `element=369` (Rio Ngumoha, `playerId=2000003024`) | Có (#18) |
| 24 | `20261123-000138.json` | 00:01:38 | Liverpool – MU | File giữa hiệp 1: `minutes=30`, tỉ số `2–0` | Có (#19) |
| 25 | `20261123-001617.json` | 00:16:17 | Liverpool – MU | **Chạm phút 45** (bù giờ hiệp 1 / nghỉ giữa hiệp): `minutes=45`, `2–0` | Có (#20) |
| 26 | `20261123-002652.json` | 00:26:52 | Liverpool – MU | File giữa giờ nghỉ giải lao: đứng yên ở `minutes=45`, `2–0` | Không |
| 27 | `20261123-003758.json` | 00:37:58 | Liverpool – MU | **Bắt đầu hiệp 2**: nhảy từ `45` lên `minutes=47`, `2–0` | Có (#21) |
| 28 | `20261123-005710.json` | 00:57:10 | Liverpool – MU | **Bàn 3–0 Liverpool** ở phút `66`: `element=369` (Rio Ngumoha, `playerId=2000003024`), kiến tạo `element=627` (Bradley Barcola, `playerId=2000003017`) | Có (#22) |
| 29 | `20261123-011324.json` | 01:13:24 | Liverpool – MU | **Bàn 4–0 Liverpool** ở phút `82`: `element=377` (Victor Munoz, `playerId=2000003014`), chưa có kiến tạo | Có (#23) |
| 30 | `20261123-011424.json` | 01:14:24 | Liverpool – MU | **Kiến tạo bàn 4** (60s sau, phút `83`): `element=366` (Florian Wirtz, `playerId=2000003007`) | Có (#24) |
| 31 | `20261123-012128.json` | 01:21:28 | Liverpool – MU | **Chạm phút 90** (bù giờ hiệp 2): `minutes=90`, tỉ số `4–0` | Có (#25) |
| 32 | `20261123-012731.json` | 01:27:31 | Liverpool – MU | **Hết trận (`FT`)**: `finished_provisional=true, minutes=90`, tỉ số `4–0` | Có (#26) |

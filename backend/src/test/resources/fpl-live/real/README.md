# Dữ liệu FPL thật (Real FPL Fixtures Snapshot)

- **File:** `fixtures-gw11-20261122.json`
- **Nguồn:** Gọi trực tiếp 1 lần tới `GET https://fantasy.premierleague.com/api/fixtures/?event=11`
- **Thời điểm tải:** `2026-11-22T14:02:21Z` (`2026-11-22 21:02:21` giờ Việt Nam `UTC+7`)
- **Trạng thái dữ liệu:** Lưu **nguyên văn (verbatim)** từ phản hồi của FPL API, không chỉnh sửa bất kỳ trường nào.
  - 7 trận ngày `2026-11-21` đã đá xong (`started = true`, `finished_provisional = true`, `minutes = 90`) kèm đầy đủ mảng `stats` thật:
    1. `id = 108`: Man City (`team_h = 15`) 1–1 Fulham (`team_a = 10`)
    2. `id = 101`: Aston Villa (`team_h = 2`) 0–0 Sunderland (`team_a = 20`)
    3. `id = 102`: Bournemouth (`team_h = 3`) 2–0 Nott'm Forest (`team_a = 18`)
    4. `id = 104`: Chelsea (`team_h = 6`) 1–4 Leeds (`team_a = 13`)
    5. `id = 105`: Coventry City (`team_h = 7`) 3–2 Crystal Palace (`team_a = 8`)
    6. `id = 110`: Spurs (`team_h = 19`) 2–0 Ipswich Town (`team_a = 12`)
    7. `id = 109`: Newcastle (`team_h = 17`) 2–2 Arsenal (`team_a = 1`)
  - 3 trận còn lại của GW11 tại thời điểm tải (`14:02:21Z` ngày `2026-11-22`) chưa có kết quả (`finished_provisional = false`).

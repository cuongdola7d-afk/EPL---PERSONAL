# GW3 2026/27 — production import record

Imported on 2026-10-01 into the Railway MySQL database `railway` on `altaria.proxy.rlwy.net`, which serves the public PremierHub backend. Before the first write, a read-only check found all ten GW3 fixtures `FINISHED`, 40 saved GW2 player-match rows for fixture `1000560555`, and no saved GW3 player-match rows or conflicting keys.

## Backup and input

- Fresh SQL backup before import: `backend/local-backups/gw3-import-20261001/premierhub-before-gw3-20261001-194554.sql` (620,734 bytes; SHA-256 `FB32B92294AA878DDA06CE1588FFFF5F40BE95293FEC3D1BEB013259A4E7D6CC`). This local backup is Git-ignored.
- Roster CSVs, imported first: `backend/data/gw3-first-five/manual-players-2026-GW3-first-five.csv` (161 rows), then `backend/data/gw3-last-five/manual-players-2026-GW3-last-five.csv` (160 rows). Together they inserted one player, one season membership, and 321 membership intervals.
- Stats CSVs, imported second: `backend/data/gw3-first-five/manual-match-stats-2026-GW3-first-five.csv` (200 rows), then `backend/data/gw3-last-five/manual-match-stats-2026-GW3-last-five.csv` (200 rows). Only these final CSVs were used; DRAFT and RATINGS files were not imported.

## Saved result

Each saved player-match row was compared with its final CSV row, including club, match-day membership, participation status, rating, fantasy points, minutes, goals, assists, and cards. No extra saved key or differing value was found.

| Batch | Fixture ID | Match | Saved rows | Rated | Played, unrated | Did not play |
| --- | ---: | --- | ---: | ---: | ---: | ---: |
| First five | 1000560566 | Ipswich Town–Liverpool | 40 | 30 | 0 | 10 |
| First five | 1000560562 | Nottingham Forest–Tottenham | 40 | 30 | 1 | 9 |
| First five | 1000560563 | Manchester City–Coventry City | 40 | 30 | 0 | 10 |
| First five | 1000560564 | Brighton–Leeds United | 40 | 28 | 1 | 11 |
| First five | 1000560565 | Brentford–Sunderland | 40 | 30 | 0 | 10 |
| Last five | 1000560568 | Fulham–Crystal Palace | 40 | 32 | 0 | 8 |
| Last five | 1000560569 | Hull City–Aston Villa | 40 | 32 | 0 | 8 |
| Last five | 1000560571 | Newcastle United–Bournemouth | 40 | 30 | 2 | 8 |
| Last five | 1000560567 | Everton–Manchester United | 40 | 29 | 1 | 10 |
| Last five | 1000560570 | Arsenal–Chelsea | 40 | 30 | 1 | 9 |
| **Total** | **10 fixtures** | | **400** | **301** | **6** | **93** |

All six `PLAYED` rows without a rating have both `rating` and `fantasy_points` set to `NULL`. The 93 `DID_NOT_PLAY` statuses and their other stored values match the final CSVs. Running both roster imports again inserted/updated 0 records; running both stats imports again inserted 0 player-match rows. A final read-only check still found 40 matching rows for every fixture.

## Public API checks

- `/api/matches?season=2026&matchweek=3` returned ten matches, all with `hasManualStats=true`.
- `/api/matches/1000560566/details?season=2026` and `/api/matches/1000560568/details?season=2026` each returned 20 home and 20 away players with `evidenceStatus=MANUAL_VERIFIED`.
- `/api/players/2000020023/matches?season=2026` returned fixture `1000560566` for Dara O'Shea; `/api/players/2000030136/matches?season=2026` returned fixture `1000560568`. Both included their saved stats and `MANUAL_VERIFIED` status.
- Public match-detail responses also returned `PLAYED` with both `rating` and `fantasyPoints` null for Ousmane Diomande (`2000020074`), Pascal Struijk (`2000030119`), Valentino Livramento (`158694`), Ryan Christie (`1125`), Noussair Mazraoui (`2000006003`), and Noni Madueke (`2000001015`).

No application code, 2024/25 data, deployment configuration, or scheduled job was changed by this import.

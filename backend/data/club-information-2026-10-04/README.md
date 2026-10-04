# Club information import — 2026-10-04

- Input: `clubs.csv`, 20 clubs in PremierHub's 2026/27 roster. Sources and access limitations: `sources.md`.
- MySQL production: 20 new rows in `club_season_information`, 19 manager names/statuses, 20 home stadiums, verified on 2026-10-04. Hull City `1000000322` manager name/status remain SQL NULL.
- Repeated importer: 20 rows checked, `inserted=0`, no conflicts.
- Existing clubs, season roster, players, memberships, profiles/positions, fixtures, statistics and standings have unchanged MySQL checksums. No 2024/25 club information rows.
- New SQL backup created before DDL/data writes, ignored by Git: `backend/local-backups/club-info-2026-10-04/prismaxi-before-club-information-20261004-102142.sql` (832838 bytes). Hash and results: `production-summary.json`.
- Production backend was at commit `2623cae424b9e2e3d831e70893303f7c47d17b69`. Its current HTTP API does not expose the new manager/stadium fields. Existing club IDs were compared with MySQL; the new local API query code was then run against production and all 20 results matched saved SQL values (`production-import-2.json`). HTTP verification of the new fields remains pending release.
- Local checks: 13 focused backend tests passed, backend package and frontend build passed. Liverpool detail checked at 1440px and 390px, including simulated missing and interim states. No UI page overflow or JavaScript errors.
- No commit, push or deployment performed. Backend and frontend code need release for the new two-line identity card to appear on production.

Import behaviour and reusable command: `docs/club-information-2026.md`.

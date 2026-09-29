# GW1 2026/27 — statistical completion progress

Checked 2026-09-30. All `rating` values already saved from SofaScore are retained.
The five statistical columns come from the **Player Stats** of each linked
StatMuse Premier League match. Blank source values stay NULL. Each patch CSV is
restricted to an existing fixture and is applied in fill-missing mode; new
matchday players use the ordinary importer after a dated roster import.

## Group 1: fixtures 1000560542–1000560544

Sources: [Arsenal–Coventry](https://www.statmuse.com/fc/match/8-21-2026-ars-vs-cov-112763),
[Hull–Manchester United](https://www.statmuse.com/fc/match/8-22-2026-hul-vs-mun-112764),
[Ipswich–Sunderland](https://www.statmuse.com/fc/match/8-22-2026-ips-vs-sun-112767).
Each StatMuse Lineup contains 40 names; player goals equal the final scores,
and individual yellow/red cards equal the team card totals.

| Fixture | Existing rows | Missing matchday row added | Existing rows patched | H2 NULL cells filled |
| --- | ---: | --- | ---: | ---: |
| `1000560542` | 39 | Bobby Thomas (`2000002003`, rating NULL) | 29 | 138 |
| `1000560543` | 39 | Bryan Mbeumo (`2000006018`, rating NULL) | 30 | 144 |
| `1000560544` | 40 | None | 31 | 148 |

Bobby Thomas and Bryan Mbeumo reuse their existing IDs and Fantasy positions.
The StatMuse match lineups evidence membership on the match dates; the new
one-day intervals do not overlap their September memberships.

Conflicting non-NULL values were **not** overwritten. The entire affected row
was excluded from the patch, leaving its other blank columns for review:

- `1000560542`, Declan Rice `2000001023`: saved `yellow_cards=1`, StatMuse `0`.
- `1000560543`, Kobbie Mainoo `2000006025`: saved `minutes=23`, StatMuse `24`.
- `1000560544`, Jack Clarke `2000011003`: saved `minutes=24`, StatMuse `20`.

H2 isolated trial: dated roster import added 2 intervals; ordinary match-stats
import added 2 rows. The three patch files filled 138, 144 and 148 NULL cells.
Imported to production after a verified SQL backup; re-import added 0 rows and
filled 0 cells.

## Group 2: fixtures 1000560545–1000560547

Sources: [Forest–Leeds](https://www.statmuse.com/fc/match/8-22-2026-for-vs-lee-112766),
[Everton–Palace](https://www.statmuse.com/fc/match/8-22-2026-eve-vs-cry-112765),
[Brentford–Tottenham](https://www.statmuse.com/fc/match/8-22-2026-bre-vs-tot-112768).
Each match has 40 matching player IDs and statuses. Player goals equal the
scores; individual yellow/red cards equal the team card totals. Existing IDs
were retained for display-name variants, including John Victor (`John` in
StatMuse, Forest goalkeeper #12; [match report](https://www.leedsunited.com/en/news/report-nottingham-forest-0-1-leeds-united))
and Jair (`Jair Cunha` in StatMuse).

| Fixture | PLAYED | DID_NOT_PLAY | H2 rows patched | H2 NULL cells filled | Conflicts |
| --- | ---: | ---: | ---: | ---: | ---: |
| `1000560545` | 28 | 12 | 28 | 136 | 0 |
| `1000560546` | 31 | 9 | 31 | 150 | 0 |
| `1000560547` | 32 | 8 | 32 | 154 | 0 |

The isolated H2 database is the same trial copy used for group 1. Production
import and re-import matched the H2 result; re-import filled 0 cells.

## Group 3: fixtures 1000560548–1000560549

Sources: [Man City–Bournemouth](https://www.statmuse.com/fc/match/8-23-2026-mci-vs-bou-112769),
[Brighton–Villa](https://www.statmuse.com/fc/match/8-23-2026-bha-vs-avl-112770).
The fixture patches add only previously NULL statistics. The four
Brighton goals include one Villa own goal by Victor Nilsson-Lindelöf; the
individual `goals` sum is 3, with no player goal assigned for the own goal.
Individual yellow/red counts match StatMuse team totals.

| Fixture | PLAYED | DID_NOT_PLAY | H2 rows patched | H2 NULL cells filled | Conflict |
| --- | ---: | ---: | ---: | ---: | --- |
| `1000560548` | 30 | 10 | 30 | 141 | None |
| `1000560549` | 32 | 8 | 31 | 147 | Jack Hinshelwood `2000030041`: saved minutes 64, StatMuse 63 |

Jack Hinshelwood's whole row is excluded from this patch. His saved 64 minutes
were retained. StatMuse's Villa bench contains an unnamed #74 (`source_player_id`
`64850`); the existing Luka Lynch DID_NOT_PLAY row matches the earlier supplied
Lineups image by club and shirt number. StatMuse does not independently supply
his name, so no new identity or statistic was inferred for him.

Production import and re-import matched the H2 result; re-import filled 0 cells.

## Group 4: fixtures 1000560550–1000560551

Sources: [Newcastle–Liverpool](https://www.statmuse.com/fc/match/8-23-2026-new-vs-liv-112771),
[Fulham–Chelsea](https://www.statmuse.com/fc/match/8-24-2026-ful-vs-che-112772).
All 40 player keys and five statistical columns in the existing `1000560550`
patch were compared with StatMuse; no differences were found. It was **not
re-imported**. Fulham–Chelsea has 40 matched names/statuses, five player goals
matching the 2–3 result, and individual yellow/red totals matching StatMuse's
team totals. Existing IDs were retained for Josh/Joshua Acheampong and Jamie
Bynoe-Gittens/Jamie Gittens.

| Fixture | PLAYED | DID_NOT_PLAY | H2 rows patched | H2 NULL cells filled | Conflict |
| --- | ---: | ---: | ---: | ---: | --- |
| `1000560550` | 31 | 9 | 0 | 0 | Already patched; one SofaScore rating still NULL |
| `1000560551` | 31 | 9 | 31 | 141 | None |

Fixture `1000560551` was imported to production and re-import filled 0 cells.

## Production reconciliation

SQL backup before writing: `backend/target/backups/premierhub-prod-pre-gw1-stats-20260929T173244Z.sql`
(ignored by Git; verified complete, 539160 bytes). The backend `prod` database
settings matched the Railway production MySQL service. Two existing player IDs
received matchday membership intervals and two missing player-match rows were
inserted. Nine fixture patches filled 1299 previously NULL cells. The already
patched `1000560550` was checked against StatMuse but was not written again.
All eleven input files were re-imported with 0 inserts/updates/filled cells.

The six missing-value columns below count **PLAYED rows only** in this order:
`rating / minutes / goals / assists / yellow_cards / red_cards`. A
`DID_NOT_PLAY` row intentionally has no rating or playing statistics.

| Fixture | Before rows; PLAYED/DNP; missing | After rows; PLAYED/DNP; missing |
| --- | --- | --- |
| `1000560542` | 39; 30/9; 0/30/27/28/27/30 | 40; 31/9; 1/1/1/1/0/1 |
| `1000560543` | 39; 31/8; 0/30/29/30/28/31 | 40; 32/8; 1/0/1/1/1/1 |
| `1000560544` | 40; 32/8; 0/31/29/30/29/32 | 40; 32/8; 0/0/0/1/1/1 |
| `1000560545` | 40; 28/12; 1/28/27/28/25/28 | 40; 28/12; 1/0/0/0/0/0 |
| `1000560546` | 40; 31/9; 1/31/29/29/30/31 | 40; 31/9; 1/0/0/0/0/0 |
| `1000560547` | 40; 32/8; 0/32/29/31/30/32 | 40; 32/8; 0/0/0/0/0/0 |
| `1000560548` | 40; 30/10; 1/29/27/28/27/30 | 40; 30/10; 1/0/0/0/0/0 |
| `1000560549` | 40; 32/8; 0/31/30/30/28/31 | 40; 32/8; 0/0/0/1/1/1 |
| `1000560550` | 40; 31/9; 1/0/0/0/0/0 | 40; 31/9; 1/0/0/0/0/0 |
| `1000560551` | 40; 31/9; 0/31/26/27/26/31 | 40; 31/9; 0/0/0/0/0/0 |

All ten fixtures now have 40 distinct player-match keys: 310 PLAYED and 90
DID_NOT_PLAY. The saved GW4/GW5 player stats, 2024 raw stats and 2024 fixture
score evidence had identical before/after hashes. The 2024 fixture `1208021`
still returns `VERIFIED` and 40/40 `COMPLETE` over the production API.
The production database reports 0 missing matchdate memberships, 0 PLAYED
rating/Fantasy-points mismatches and 0 DID_NOT_PLAY rows with invalid
rating/points. It retains 120 GW4/GW5 manual rows, 400 raw 2024 player-match
rows and 10 score-evidence rows.

The following PLAYED ratings are still unavailable in the supplied SofaScore
images/CSV; their Fantasy points remain NULL: Bobby Thomas (`1000560542`,
`2000002003`), Bryan Mbeumo (`1000560543`, `2000006018`), Arnaud Kalimuendo
(`1000560545`, `2000020064`), Carlos Alcaraz (`1000560546`, `2000020094`),
Ben Gannon-Doak (`1000560548`, `2000030034`) and Fabian Schär (`1000560550`,
`2000030075`). A SofaScore player rating screenshot or explicit no-rating
confirmation is needed for each; StatMuse ratings must not substitute.

Four source conflicts require the original lineups/statistics images before
further patching. The entire conflicting rows remain untouched:

| Fixture | Player ID | Column | Saved | StatMuse |
| --- | ---: | --- | ---: | ---: |
| `1000560542` Declan Rice | `2000001023` | yellow_cards | 1 | 0 |
| `1000560543` Kobbie Mainoo | `2000006025` | minutes | 23 | 24 |
| `1000560544` Jack Clarke | `2000011003` | minutes | 24 | 20 |
| `1000560549` Jack Hinshelwood | `2000030041` | minutes | 64 | 63 |

Alexis Mac Allister (`2000003010`) has an evidenced Liverpool membership for
2026-08-23 and a saved `1000560550` row with 28 minutes, 0 goals/assists/cards,
rating and Fantasy points 6.90. Production player and history APIs both return
HTTP 200 and the GW1 history row is `MANUAL_VERIFIED`; the old cropped browser
screenshot is inconsistent with the current response. A separate history API
guard rejected players whose membership had ended by today's date even when
their GW1 row existed. The local controller/query fix now checks season
membership for history without changing the current-roster list; it requires a
future deploy and was not applied to production in this data-only release.

Verification: all files imported twice into one isolated H2 database; the
second pass added or filled 0. Production import and second pass returned the
same result. The final H2 and MySQL counts match for all ten fixtures. The
targeted importer, roster and player-controller suite passed 28 tests with
zero failures; `git diff --check` passed. No deployment was performed.

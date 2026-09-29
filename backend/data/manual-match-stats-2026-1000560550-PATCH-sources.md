# Newcastle United 2–2 Liverpool, GW1 2026/27 (fixture 1000560550)

Checked 2026-09-29. This patch contains only the 40 player-match keys already in
`manual-match-stats-2026-GW1-last-five-LOCAL.csv`: 31 PLAYED and 9 DID_NOT_PLAY.
The original SofaScore Lineups screenshots supplied for GW1 establish the matchday
lineup and existing ratings. This patch does not change any rating. Fabian Schär
played but has no displayed SofaScore rating; his rating and Fantasy points stay NULL.

The [StatMuse match player statistics](https://www.statmuse.com/fc/match/8-23-2026-new-vs-liv-112771)
provide explicit Minutes Played, Goals, Assists, Yellow Cards and Red Cards for each
of the 31 PLAYED players. All 31 names match the existing roster IDs. The four
goals, three assists and eight yellow cards match the match event timeline and
the 2–2 score. The 140 previously NULL statistical cells in the CSV are filled
from those per-player values, including explicit zeroes. The nine DID_NOT_PLAY
rows are unchanged.

The 40 matchday players in the supplied Lineups images each already had one
CSV and MySQL row. Five players in the *current* Liverpool roster have no GW1
row: Joe Gomez, Hugo Ekitike, Freddie Woodman, Bradley Barcola and Jayden
Danns. Their saved Liverpool memberships begin 2026-09-03, after this match;
they are not in the supplied matchday Lineups, so no DID_NOT_PLAY row was
invented for them.

The same URL is the source for all filled statistical cells. StatMuse ratings
differ from the previously selected SofaScore ratings and were **not** imported.
SofaScore's detailed statistics endpoint was inaccessible in this session;
using StatMuse for these five statistical columns follows the user's later
request to look beyond SofaScore. The existing 2026/27 Fantasy rating rule
remains unchanged.

To apply, use the fill-missing mode of the manual match-stats command with
`--premierhub.manual-match-stats.fill-missing-fixture=1000560550`. The command
requires an existing player-match row and membership for the match date, fills
only NULL values, rejects conflicting non-NULL values, and runs as one transaction.
The standard importer remains the path for a genuinely new player-match row.

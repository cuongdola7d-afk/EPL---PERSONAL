# Arsenal player profile sources — 2026/27

Roster: [PremierHub API as of 2026-10-02](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-02), filtered to `clubId=1000000057` (Arsenal). The 24 returned `player_id` values and their membership are authoritative for this batch.

A blank CSV cell represents SQL `NULL`, never zero. Every roster player remains in the player profile list. A player whose `fc27_overall` is `NULL` is **not eligible for Fantasy selection** until an official base OVR is verified. This dataset does not change saved membership or match data.

SofaScore fields were read from each linked player profile on 2026-10-02. Country names are kept as displayed in the English profile; dates are converted to `YYYY-MM-DD`; feet are converted from Left/Right to `LEFT`/`RIGHT`. Shirt numbers come only from each player's SofaScore profile.

EA Overall values use the [official FC 27 Arsenal rating table](https://www.ea.com/games/ea-sports-fc/ratings/teams-ratings/arsenal/1) and the linked official player profiles. EA says this table contains launch Gold, Silver and Bronze player items, excluding campaign and other special items. EA club labels are not used to alter PremierHub membership.

| PremierHub ID | Player | SofaScore profile | EA SPORTS FC 27 profile | Note |
| --- | --- | --- | --- | --- |
| 2000001004 | Ben White | [SofaScore](https://www.sofascore.com/football/player/ben-white/846036) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/benjamin-white/231936) | EA displays Benjamin White; Arsenal, England and right-back role match. |
| 2000001022 | Bruno Guimaraes | [SofaScore](https://www.sofascore.com/football/player/bruno-guimaraes/866469) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bruno-guimaraes/247851) |  |
| 2000001007 | Bukayo Saka | [SofaScore](https://www.sofascore.com/football/player/bukayo-saka/934235) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bukayo-saka/246669) |  |
| 2000001014 | Christos Tzolis | [SofaScore](https://www.sofascore.com/football/player/christos-tzolis/1031259) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/christos-tzolis/256948) |  |
| 2000001003 | Cristhian Mosquera | [SofaScore](https://www.sofascore.com/football/player/cristhian-mosquera/1144630) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/cristhian-mosquera/264846) |  |
| 2000001001 | David Raya | [SofaScore](https://www.sofascore.com/football/player/david-raya/581310) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/david-raya/220901) |  |
| 2000001023 | Declan Rice | [SofaScore](https://www.sofascore.com/football/player/declan-rice/856714) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/declan-rice/234378) |  |
| 2000001009 | Eberechi Eze | [SofaScore](https://www.sofascore.com/football/player/eberechi-eze/864921) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/eberechi-eze/235794) |  |
| 2000001013 | Ezri Konsa | [SofaScore](https://www.sofascore.com/football/player/ezri-konsa/827679) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ezri-konsa/227678) | SofaScore profile explicitly lists Arsenal; first-page club label was omitted by the text extractor. |
| 2000001006 | Gabriel Magalhaes | [SofaScore](https://www.sofascore.com/football/player/gabriel-magalhaes/869792) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/gabriel/232580) | EA displays Gabriel; Arsenal, Brazil and center-back role match. |
| 2000001019 | Illan Meslier | [SofaScore](https://www.sofascore.com/football/player/illan-meslier/906076) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/illan-meslier/242656) |  |
| 2000001010 | Jurrien Timber | [SofaScore](https://www.sofascore.com/football/player/jurrien-timber/958959) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jurrien-timber/251805) |  |
| 2000001018 | Kai Havertz | [SofaScore](https://www.sofascore.com/football/player/kai-havertz/836705) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kai-havertz/235790) |  |
| 2000001011 | Kepa Arrizabalaga | [SofaScore](https://www.sofascore.com/football/player/kepa-arrizabalaga/232422) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kepa/206585) | EA displays Kepa; Arsenal, Spain and goalkeeper role match. |
| 2000001008 | Martin Odegaard | [SofaScore](https://www.sofascore.com/football/player/martin-degaard/547410) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/martin-degaard/222665) | EA URL omits Ø; profile displays Martin Ødegaard of Arsenal, Norway. |
| 2000001021 | Martin Zubimendi | [SofaScore](https://www.sofascore.com/football/player/martin-zubimendi/966837) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/zubimendi/248148) | EA displays Zubimendi; Arsenal, Spain and defensive-midfield role match. |
| 2000001025 | Max Dowman | [SofaScore](https://www.sofascore.com/football/player/max-dowman/1917626) | — | `fc27_overall=72` at the user's explicit request. This is a PremierHub estimate, not an EA verified base rating. |
| 2000001017 | Mikel Merino | [SofaScore](https://www.sofascore.com/football/player/mikel-merino/592010) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mikel-merino/225193) |  |
| 2000001024 | Myles Lewis-Skelly | [SofaScore](https://www.sofascore.com/football/player/lewis-skelly-myles/1423711) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/myles-lewis-skelly/278773) |  |
| 2000001015 | Noni Madueke | [SofaScore](https://www.sofascore.com/football/player/noni-madueke/966547) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/noni-madueke/254796) |  |
| 2000001005 | Piero Hincapie | [SofaScore](https://www.sofascore.com/football/player/piero-hincapie/1002837) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/piero-hincapie/256197) |  |
| 2000001020 | Riccardo Calafiori | [SofaScore](https://www.sofascore.com/football/player/riccardo-calafiori/957602) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/riccardo-calafiori/257711) |  |
| 2000001012 | Viktor Gyokeres | [SofaScore](https://www.sofascore.com/football/player/viktor-gyokeres/804508) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/viktor-gyokeres/241651) |  |
| 2000001002 | William Saliba | [SofaScore](https://www.sofascore.com/football/player/william-saliba/941168) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/william-saliba/243715) |  |

Coverage: 24 PremierHub roster IDs; SofaScore nationality, birth date, height, preferred foot and shirt number each 24/24. `fc27_overall` is populated for 24/24: 23 verified EA FC 27 base ratings and one user-specified PremierHub estimate (Max Dowman, 72). No official EA FC 27 profile was verified for Dowman. This field value no longer distinguishes the estimate from verified EA data by itself; consult this source note before using it as an official rating.

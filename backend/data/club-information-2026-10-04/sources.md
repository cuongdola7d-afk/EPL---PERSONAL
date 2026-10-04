# Current club information — 2026/27

Checked: **2026-10-04** (Asia/Saigon). This is a current information snapshot, not manager history and not fixture data. Empty CSV cells represent SQL NULL.

IDs were read from Railway MySQL `clubs JOIN season_clubs WHERE league_id=39 AND season_year=2026` and matched to `/api/clubs?season=2026`. Club websites are the information sources; search snippets of official pages were used where those sites return an empty JavaScript shell. No third-party manager name was imported.

`PERMANENT` means a regular manager/head coach appointment; `INTERIM` means an explicitly temporary appointment. No verified current appointment in this batch was labelled interim. The importer rejects a manager without a corresponding status.

| club_id | Club | Manager source | Stadium source |
|---|---|---|---|
| 1000001044 | AFC Bournemouth | [Club article: Marco Rose names starting XI](https://next.gc.afcbournemouthservices.co.uk/news/2026/september/12/rose-reverts-back-to-newcastle-starting-line-up/) | [2026/27 ticket office at Vitality Stadium](https://www.afcb.co.uk/tickets/ticket-office-opening-hours) |
| 1000000057 | Arsenal FC | [Current club homepage, Arteta and 2026/27](https://www.arsenal.com/) | [The Club: Emirates Stadium](https://www.arsenal.com/the-club) |
| 1000000058 | Aston Villa FC | [Club article: back-to-back wins before the break](https://next.gc.avfcservices.co.uk/news/2026/september/22/feature-back-to-back-wins-before-the-break/) | [Villa Park first-time guide](https://www.avfc.co.uk/villa-park/first-time) |
| 1000000402 | Brentford FC | [Current Keith Andrews staff profile](https://www.brentfordfc.com/en/teams/staff-men-s-first-team/keith-andrews) | [Visiting the Gtech](https://www.brentfordfc.com/en/visiting-the-gtech) |
| 1000000397 | Brighton & Hove Albion FC | [Hurzeler contract to June 2029](https://www.brightonandhovealbion.com/media-article/hurzeler-signs-new-long-term-deal) | [Club contact address: American Express Stadium](https://www.brightonandhovealbion.com/contact-us) |
| 1000000061 | Chelsea FC | [June 2026 club statement: Xabi Alonso](https://www.chelseafc.com/en/news/article/club-statement-june-2026) | [Official Stamford Bridge tour booking](https://tours.chelseafc.com/booking/stadium-tours.htm) |
| 1000001076 | Coventry City FC | [Official manager awards article](https://www.ccfc.co.uk/news/2026/march/12/frank-lampard-and-haji-wright-nominated-for-monthly-awards/) | [2026/27 home Premier League tickets](https://www.ccfc.co.uk/tickets/home-tickets) |
| 1000000354 | Crystal Palace FC | [Current Palace programme: Manager Pierre Sage, 2026/27](https://www.cpfc.co.uk/news/programme/) | [Current club homepage: men's home match at Selhurst Park](https://www.cpfc.co.uk/?pub1abt=690) |
| 1000000062 | Everton FC | [Official Coleman interview: manager David Moyes](https://stories.evertonfc.com/long-read-coleman/index.html) and [current news: Moyes/unbeaten Blues](https://www.evertonfc.com/?page_id=10223) | [Club ticketing charter: men's team at Hill Dickinson, women's at Goodison](https://www.evertonfc.com/fans/fans-charter/ticketing) |
| 1000000063 | Fulham FC | [Current Álvaro Arbeloa profile, contract until 2029](https://www.fulhamfc.com/players/alvaroarbeloa/) | [Craven Cottage](https://www.fulhamfc.com/visit/craven-cottage) |
| 1000000322 | Hull City AFC | **NULL** — see unresolved item below | [Getting to MKM Stadium](https://www.wearehullcity.co.uk/getting-to-the-mkm-stadium/) |
| 1000000349 | Ipswich Town FC | [Gary O'Neil appointment, 23 June 2026](https://www.itfc.co.uk/news/2026/june/23/gary-oneil-appointed-ipswich-town-manager/) | [Portman Road: all men's home fixtures](https://www.itfc.co.uk/portmanroad) |
| 1000000341 | Leeds United FC | [Farke pre-match interview, 2026/27](https://www.leedsunited.com/en/news/daniel-farke-we-travel-very-respectful) | [Enhancing Elland Road](https://www.leedsunited.com/en/ellandroad) |
| 1000000064 | Liverpool FC | [Iraola returning to Bournemouth, 18 September 2026](https://www.liverpoolfc.com/news/have-be-ruthless-andoni-iraola-returning-bournemouth-liverpool?amp=1) | [Official Anfield tour booking](https://bookings.liverpoolfc.com/stadiumtours/booking/default.htm?firstAvailable=true&search=tours&tourType=11) |
| 1000000065 | Manchester City FC | [Maresca unveiling press conference](https://www.mancity.com/news/mens/enzo-maresca-man-city-unveiling-press-conference-written-two-63920485) | [Visiting Etihad Stadium](https://www.mancity.com/etihad-stadium/visiting-the-etihad-stadium) |
| 1000000066 | Manchester United FC | [Carrick continues; contract to 2028](https://www.manutd.com/ko/news/michael-carrick-continues-as-man-united-head-coach-2026) | [Visit Old Trafford](https://www.manutd.com/en/club/visit-old-trafford) |
| 1000000067 | Newcastle United FC | [Jaissle's first press conference](https://www.newcastleunited.com/en/news/matthias-jaissles-first-press-conference-as-newcastle-united-head-coach) | [At St James' Park](https://www.newcastleunited.com/en/st-james-park/visitor-information/at-the-stadium) |
| 1000000351 | Nottingham Forest FC | [Glasner: first home match after summer appointment](https://www.nottinghamforest.co.uk/news/2026/august/12/glasner--a-really-good-test-for-us/) | [Same official article: home match at City Ground](https://www.nottinghamforest.co.uk/news/2026/august/12/glasner--a-really-good-test-for-us/) |
| 1000000071 | Sunderland AFC | [Current men's news: Le Bris, September 2026](https://www.safc.com/news/?page_id=12374) | [Club matchday guide](https://www.safc.com/matchday) |
| 1000000073 | Tottenham Hotspur FC | [Roberto De Zerbi joins as men's head coach](https://www.tottenhamhotspur.com/news/1018014/roberto-de-zerbi-joins-as-mens-head-coach) | [Inside Tottenham Hotspur Stadium](https://www.tottenhamhotspur.com/the-stadium/attending-matches/inside-the-stadium/) |

## Source access and unresolved fields

- **Hull City AFC — 1000000322:** `manager_name`, `manager_status` remain NULL. [Official appointment URL attempted](https://www.wearehullcity.co.uk/news/2025/june/11/jakirovic-appointed-new-head-coach/) returned an empty/unreadable page. An indexed official video mentions only “Sergej”; that does not independently verify the full current name and appointment status. Third-party pages were not substituted. Stadium verified separately.
- The AFC Bournemouth and Aston Villa `next.gc.*services.co.uk` links are the club-branded publishing pages found in the official website's indexed content; the main-domain equivalents currently return a JavaScript shell. Those articles explicitly identify the men's head coach in September 2026.
- Coventry's accessible indexed official manager article is from March 2026. Its URL and publication timing are retained rather than presented as a fresh October announcement.
- Club names and IDs stay as recorded in PrismaXI. Stadium names use the current official naming (not historical Goodison Park for Everton men). No player, roster, statistics, logo or Fantasy data was changed.

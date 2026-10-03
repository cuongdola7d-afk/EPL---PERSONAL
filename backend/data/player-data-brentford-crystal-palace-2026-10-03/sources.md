# Brentford – Crystal Palace: hồ sơ, OVR và vị trí 2026/27

Ngày chuẩn bị: 2026-10-03. Chỉ dữ liệu local; chưa ghi production.

## Roster và hồ sơ giữ nguyên

- Roster PremierHub season=2026, asOf=2026-10-03: Brentford 28, Crystal Palace 24;
  toàn bộ 52 ID hiện hành, không giới hạn 25 người. ID/CLB trùng hai CSV hồ sơ cũ 02/10.
- API đã có đủ nationality, birthDate, heightCm, preferredFoot, shirtNumber, fc27Overall
  cho 52 người. profiles.csv giữ nguyên toàn bộ sáu giá trị, không thu thập lại SofaScore.
  URL SofaScore từng người được kế thừa từ sources.md của hai batch hồ sơ cũ.
- Đã mở 52 hồ sơ EA FC 27 để lấy Position/Alt Positions và đối chiếu OVR thẻ cơ bản.
  OVR cả 52 người khớp dữ liệu hiện có; không dùng thẻ sự kiện hay FC 26.
- Ghép theo EA ID/URL đã có, tên và biến thể tên, quốc tịch và tuổi theo ngày sinh
  hiện có tại 03/10. Netherlands/Holland, Ireland/Republic of Ireland, USA/United States,
  South Korea/Korea Republic là các tên quốc gia tương ứng. Giữ membership PremierHub
  khi EA ghi một CLB khác. Không cập nhật dữ liệu hồ sơ bằng các trường sinh học EA.
- Chỉ ánh xạ CDM → CM cho chính/phụ, loại mã trùng sau ánh xạ. Không suy từ nhóm rộng
  hoặc tự cho RW quyền RM. Mã chính là Position EA; eligible gồm chính và Alt Positions.
- 52 incoming vị trí; expected_* trống vì snapshot API chưa có vị trí. Không có MISSING.
- Kim Ji-soo và Jean-Philippe Mateta: công cụ duyệt web lỗi timeout. Đã tải trực tiếp
  HTML của đúng URL EA dưới đây, xác nhận tiêu đề FC 27/canonical EA ID và đọc
  __NEXT_DATA__.props.pageProps.ratingsEntries.items[0]. Position tương ứng CB/ST;
  alternatePositions=null nên chỉ có vị trí chính. Ngày sinh EA khớp SofaScore đã lưu.
  Không lấy vị trí từ khối Similar players. Tệp HTML tạm ở backend/target/, không commit.

## Roster và nguồn hồ sơ cũ

- [Brentford FC](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Brentford+FC), club_id=1000000402. Hồ sơ cũ: backend/data/brentford-profiles-2026-10-02/players.csv và sources.md.
- [Crystal Palace FC](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Crystal+Palace+FC), club_id=1000000354. Hồ sơ cũ: backend/data/crystal-palace-profiles-2026-10-02/players.csv và sources.md.

## Độ phủ local

| CLB | Roster | Quốc tịch | DOB | Cao | Chân thuận | Số áo | OVR | Primary + eligible | MISSING |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Brentford | 28 | 28 | 28 | 28 | 28 | 28 | 28 | 28 | 0 |
| Crystal Palace | 24 | 24 | 24 | 24 | 24 | 24 | 24 | 24 | 0 |

## Nguồn từng cầu thủ

Alt “—” nghĩa là hồ sơ không ghi vị trí phụ. Hồ sơ SofaScore dưới đây là nguồn đã
được lưu cho năm trường hồ sơ; lượt này không đọc lại vì không có ô thiếu.

| CLB | player_id | Tên PremierHub | OVR giữ nguyên | SofaScore đã lưu | EA FC 27 | EA chính; phụ | Nhập chính; eligible | Ghi chú |
| --- | --- | --- | ---: | --- | --- | --- | --- | --- |
| Crystal Palace | 2000020098 | Adam Wharton | 82 | [SofaScore](https://www.sofascore.com/football/player/adam-wharton/1109771) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/adam-wharton/259240) | CM; CDM | CM; CM | CDM → CM; gộp mã trùng. |
| Crystal Palace | 2000030182 | Axel Disasi | 77 | [SofaScore](https://www.sofascore.com/football/player/axel-disasi/827243) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/axel-disasi/229942) | CB; RB | CB; CB, RB | EA ghi Chelsea; giữ membership PremierHub. |
| Crystal Palace | 2000030187 | Ben Chilwell | 77 | [SofaScore](https://www.sofascore.com/football/player/ben-chilwell/802695) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ben-chilwell/229984) | LB; LM | LB; LB, LM | EA ghi Strasbourg; giữ membership PremierHub. |
| Crystal Palace | 2000020104 | Chadi Riad | 74 | [SofaScore](https://www.sofascore.com/football/player/chadi-riad/1064218) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/chadi-riad/258490) | CB; — | CB; CB |  |
| Crystal Palace | 3339 | Cheick Doucouré | 76 | [SofaScore](https://www.sofascore.com/football/player/cheick-doucoure/906047) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/cheick-doucoure/242619) | CDM; CM | CM; CM | CDM → CM; gộp mã trùng. |
| Crystal Palace | 2000020099 | Chris Richards | 80 | [SofaScore](https://www.sofascore.com/football/player/chris-richards/931844) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/chris-richards/250954) | CB; — | CB; CB |  |
| Crystal Palace | 2000016001 | Daichi Kamada | 79 | [SofaScore](https://www.sofascore.com/football/player/daichi-kamada/794338) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/daichi-kamada/232730) | CM; CDM | CM; CM | CDM → CM; gộp mã trùng. |
| Crystal Palace | 2000030189 | Darío Osorio | 74 | [SofaScore](https://www.sofascore.com/football/player/dario-osorio/1106824) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/dario-osorio/278012) | RM; RW, CAM, ST | RM; RM, RW, CAM, ST | EA ghi FC Midtjylland; giữ membership PremierHub. |
| Crystal Palace | 2000020100 | Dean Henderson | 82 | [SofaScore](https://www.sofascore.com/football/player/dean-henderson/788134) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/dean-henderson/233306) | GK; — | GK; GK |  |
| Crystal Palace | 2000020097 | Dwight McNeil | 76 | [SofaScore](https://www.sofascore.com/football/player/dwight-mcneil/935543) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/dwight-mc-neil/243282) | RM; LM, CAM | RM; RM, LM, CAM |  |
| Crystal Palace | 2000020101 | Eddie Nketiah | 75 | [SofaScore](https://www.sofascore.com/football/player/eddie-nketiah/858194) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/eddie-nketiah/236988) | ST; — | ST; ST |  |
| Crystal Palace | 2000030186 | Honest Ahanor | 74 | [SofaScore](https://www.sofascore.com/football/player/honest-ahanor/1634980) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/honest-ahanor/74483) | CB; — | CB; CB | EA ghi Bergamo Calcio; giữ membership PremierHub. |
| Crystal Palace | 2218 | Ismaïla Sarr | 81 | [SofaScore](https://www.sofascore.com/football/player/ismaila-sarr/845286) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ismaila-sarr/235353) | RW; RM | RW; RW, RM |  |
| Crystal Palace | 2000020096 | Jaydee Canvot | 76 | [SofaScore](https://www.sofascore.com/football/player/jaydee-canvot/1471671) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jaydee-canvot/74133) | CB; — | CB; CB |  |
| Crystal Palace | 2000020102 | Jean-Philippe Mateta | 81 | [SofaScore](https://www.sofascore.com/football/player/jean-philippe-mateta/848276) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jean-philippe-mateta/236461) | ST; — | ST; ST | Đọc trực tiếp HTML EA FC 27; alternatePositions=null. |
| Crystal Palace | 2490 | Jefferson Lerma | 76 | [SofaScore](https://www.sofascore.com/football/player/jefferson-lerma/355796) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jefferson-lerma/213991) | CDM; CM, CB | CM; CM, CB | CDM → CM; gộp mã trùng. |
| Crystal Palace | 2000030183 | Joél Drakes-Thomas | 62 | [SofaScore](https://www.sofascore.com/football/player/joel-drakes-thomas/2007717) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/joel-drakes-thomas/82815) | LW; CAM, RW, LM | LW; LW, CAM, RW, LM |  |
| Crystal Palace | 2000020106 | Jørgen Strand Larsen | 77 | [SofaScore](https://www.sofascore.com/football/player/jorgen-strand-larsen/876599) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/j-rgen-strand-larsen/238756) | ST; — | ST; ST |  |
| Crystal Palace | 2000030188 | Quinten Timber | 79 | [SofaScore](https://www.sofascore.com/football/player/quinten-timber/959805) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/quinten-timber/251806) | CM; CDM, CAM | CM; CM, CAM | CDM → CM; gộp mã trùng. EA ghi OM; giữ membership PremierHub. |
| Crystal Palace | 2000030184 | Remi Matthews | 63 | [SofaScore](https://www.sofascore.com/football/player/remi-matthews/581610) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/remi-matthews/203824) | GK; — | GK; GK |  |
| Crystal Palace | 2000020108 | Takehiro Tomiyasu | 76 | [SofaScore](https://www.sofascore.com/football/player/takehiro-tomiyasu/804434) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/takehiro-tomiyasu/232938) | LB; RB, CB, CM | LB; LB, RB, CB, CM |  |
| Crystal Palace | 2000020114 | Walter Benítez | 78 | [SofaScore](https://www.sofascore.com/football/player/walter-benitez/249859) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/walter-benitez/215223) | GK; — | GK; GK |  |
| Crystal Palace | 2000020117 | Will Hughes | 77 | [SofaScore](https://www.sofascore.com/football/player/will-hughes/193554) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/will-hughes/206516) | CDM; CM | CM; CM | CDM → CM; gộp mã trùng. |
| Crystal Palace | 2000030185 | Zavier Gozo | 71 | [SofaScore](https://www.sofascore.com/football/player/zavier-gozo/1578999) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/zavier-gozo/278875) | RM; CAM, RW, CM | RM; RM, CAM, RW, CM |  |
| Brentford | 2000030008 | Aaron Hickey | 75 | [SofaScore](https://www.sofascore.com/football/player/aaron-hickey/966869) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/aaron-hickey/248808) | RB; LB, RM, LM | RB; RB, LB, RM, LM |  |
| Brentford | 2000030160 | Antoni Milambo | 73 | [SofaScore](https://www.sofascore.com/football/player/antoni-milambo/1126692) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/antoni-milambo/263640) | CM; CAM | CM; CM, CAM |  |
| Brentford | 2000030165 | Benjamin Fredrick | 63 | [SofaScore](https://www.sofascore.com/football/player/fredrick-benjamin/1470532) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/benjamin-fredrick/70781) | CB; RB | CB; CB, RB |  |
| Brentford | 2000030007 | Callum Wilson | 76 | [SofaScore](https://www.sofascore.com/football/player/callum-wilson/113956) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/callum-wilson/196978) | ST; — | ST; ST |  |
| Brentford | 2000030001 | Caoimhin Kelleher | 80 | [SofaScore](https://www.sofascore.com/football/player/caoimhin-kelleher/827362) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/caoimhin-kelleher/240913) | GK; — | GK; GK |  |
| Brentford | 2000030012 | Dango Ouattara | 79 | [SofaScore](https://www.sofascore.com/football/player/dango-ouattara/1106451) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/dango-ouattara/265552) | RM; LM, ST, RW | RM; RM, LM, ST, RW |  |
| Brentford | 2000030163 | El Hadji Malick Diouf | 78 | [SofaScore](https://www.sofascore.com/football/player/diouf-el-hadji-malick/1471764) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/el-hadji-malick-diouf/274915) | LB; — | LB; LB | EA hiện ghi West Ham; giữ membership Brentford của PremierHub. EA ghi West Ham; giữ membership PremierHub. |
| Brentford | 2000030166 | Ellery Balcombe | 63 | [SofaScore](https://www.sofascore.com/football/player/ellery-balcombe/860205) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ellery-balcombe/236663) | GK; — | GK; GK |  |
| Brentford | 153066 | Fabio Carvalho | 74 | [SofaScore](https://www.sofascore.com/football/player/fabio-carvalho/991604) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/fabio-carvalho/256725) | CAM; CM, CDM | CAM; CAM, CM | CDM → CM; gộp mã trùng. |
| Brentford | 2000030164 | Gustavo Nunes Fernandes Gomes | 67 | [SofaScore](https://www.sofascore.com/football/player/gustavo-nunes/1514747) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/gustavo-nunes/74759) | LM; RM, LW, RW | LM; LM, RM, LW, RW | SofaScore và EA dùng tên ngắn Gustavo Nunes; [hồ sơ CLB Brentford](https://www.brentfordfc.com/en/teams/mens/gustavo-nunes?tab=bio) cùng ngày sinh/CLB xác nhận người. |
| Brentford | 2000030096 | Hákon Rafn Valdimarsson | 69 | [SofaScore](https://www.sofascore.com/football/player/hakon-rafn-valdimarsson/1005959) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/hakon-valdimarsson/264133) | GK; — | GK; GK | EA rút gọn tên đệm thành Hákon Valdimarsson; [hồ sơ CLB Brentford](https://www.brentfordfc.com/en/teams/profile/hakon-valdimarsson) và ngày sinh xác nhận người. |
| Brentford | 2000030006 | Igor Thiago | 82 | [SofaScore](https://www.sofascore.com/football/player/igor-thiago/1016907) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/igor-thiago/275771) | ST; — | ST; ST |  |
| Brentford | 2000030010 | Jaidon Anthony | 76 | [SofaScore](https://www.sofascore.com/football/player/jaidon-anthony/1020680) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jaidon-anthony/243669) | LM; RM, LW, RW | LM; LM, RM, LW, RW |  |
| Brentford | 2000030099 | Jannik Schuster | 67 | [SofaScore](https://www.sofascore.com/football/player/schuster-jannik/1474198) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jannik-schuster/79918) | CB; — | CB; CB |  |
| Brentford | 2000030161 | Josh Dasilva | 71 | [SofaScore](https://www.sofascore.com/football/player/josh-dasilva/856260) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/josh-dasilva/231445) | CM; CDM | CM; CM | CDM → CM; gộp mã trùng. |
| Brentford | 2000030162 | Kaye Furo | 65 | [SofaScore](https://www.sofascore.com/football/player/kaye-iyowuna-furo/1403098) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kaye-furo/75452) | ST; — | ST; ST |  |
| Brentford | 2000030094 | Kevin Schade | 79 | [SofaScore](https://www.sofascore.com/football/player/kevin-schade/1006387) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kevin-schade/260926) | LM; ST, LW | LM; LM, ST, LW |  |
| Brentford | 2000030159 | Kim Ji-soo | 65 | [SofaScore](https://www.sofascore.com/football/player/kim-ji-soo/1185061) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kim-ji-soo/267844) | CB; — | CB; CB | Đọc trực tiếp HTML EA FC 27; alternatePositions=null. |
| Brentford | 2000030095 | Kristoffer Ajer | 77 | [SofaScore](https://www.sofascore.com/football/player/kristoffer-ajer/576384) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kristoffer-ajer/224258) | CB; RB, LB | CB; CB, RB, LB |  |
| Brentford | 2000030005 | Mamadou Sangare | 81 | [SofaScore](https://www.sofascore.com/football/player/mamadou-sangare/1064697) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mamadou-sangare/258601) | CM; CDM | CM; CM | CDM → CM; gộp mã trùng. |
| Brentford | 2000030013 | Mathias Jensen | 80 | [SofaScore](https://www.sofascore.com/football/player/mathias-jensen/799251) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mathias-jensen/229723) | CM; CDM, CAM | CM; CM, CAM | CDM → CM; gộp mã trùng. |
| Brentford | 2000030004 | Michael Kayode | 81 | [SofaScore](https://www.sofascore.com/football/player/michael-kayode/1137431) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/michael-kayode/277869) | RB; RM | RB; RB, RM |  |
| Brentford | 2000030009 | Mikkel Damsgaard | 80 | [SofaScore](https://www.sofascore.com/football/player/mikkel-damsgaard/907072) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mikkel-damsgaard/241508) | CAM; CM, LM | CAM; CAM, CM, LM |  |
| Brentford | 2000030014 | Nathan Collins | 79 | [SofaScore](https://www.sofascore.com/football/player/nathan-collins/958916) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/nathan-collins/248484) | CB; — | CB; CB |  |
| Brentford | 2000030097 | Rico Henry | 76 | [SofaScore](https://www.sofascore.com/football/player/rico-henry/599128) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/rico-henry/224494) | LB; LM | LB; LB, LM |  |
| Brentford | 2000030158 | Sepp van den Berg | 78 | [SofaScore](https://www.sofascore.com/football/player/sepp-van-den-berg/924378) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/sepp-van-den-berg/242453) | CB; — | CB; CB |  |
| Brentford | 2000030003 | Vitaly Janelt | 78 | [SofaScore](https://www.sofascore.com/football/player/vitaly-janelt/814873) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/vitaly-janelt/235167) | CDM; CM | CM; CM | CDM → CM; gộp mã trùng. |
| Brentford | 2000030011 | Yehor Yarmoliuk | 78 | [SofaScore](https://www.sofascore.com/football/player/yehor-yarmoliuk/1031258) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/yehor-yarmoliuk/270608) | CDM; CM | CM; CM | CDM → CM; gộp mã trùng. |

# Everton – Fulham: hồ sơ, OVR và vị trí 2026/27

Ngày chuẩn bị và bổ sung: 2026-10-03. Chỉ dữ liệu local, chưa ghi production.

## Roster và dữ liệu hiện có

- Roster PremierHub season=2026, asOf=2026-10-03: Everton 20, Fulham 26; toàn bộ 46 ID.
  Giữ nguyên player_id/club_id, membership, nhóm rộng, thống kê trận và mùa 2024/25.
- profiles.csv và roster-status.csv có đúng 46 người. positions.csv có 46 primary,
  79 mã eligible: 44 bộ vị trí xác minh từ EA, hai bộ do người dùng cung cấp.
- Cả 46 hồ sơ local đều đủ năm trường hồ sơ, OVR số và vị trí sau bổ sung.
  Không còn MISSING/REVIEW_REQUIRED. Hai OVR/vị trí người dùng chưa có nguồn EA;
  không gọi các giá trị đó là OVR/vị trí chính thức đã xác minh trên EA.
- Giữ các giá trị trước ngoài đúng năm ô hồ sơ được bổ sung/xác nhận và hai bộ
  vị trí mới dưới đây. Không gán 0 hoặc suy quyền chơi từ nhóm vị trí rộng.

## Bổ sung người dùng ngày 03/10/2026

Nguồn: tin nhắn người dùng “Braiden Graham: st, overall 60 , Macaulay Zepa: rw/rm,
overall 60, 178 cm, số áo 35, Gonzalo García tây ban nha”. Chuẩn hóa vị trí chữ hoa,
“Tây Ban Nha” thành Spain. Vị trí đầu tiên trong danh sách làm primary.

| player_id | CLB | Tên | Bổ sung/xác nhận |
| --- | --- | --- | --- |
| 2000030192 | Everton, 1000000062 | Braiden Graham | primary ST; eligible ST; fc27_overall 60 |
| 2000030194 | Fulham, 1000000063 | Macaulay Zepa | primary RW; eligible RW/RM; fc27_overall 60; height_cm 178; shirt_number 35 |
| 2000030082 | Fulham, 1000000063 | Gonzalo García | nationality Spain thay D Mallorca Yo; giữ OVR75 và ST/ST |

Braiden/Zepa: ovr_source=USER_PROVIDED_2026-10-03, position_status=USER_PROVIDED.
Quốc tịch Gonzalo có nguồn người dùng, không lấy EA thay SofaScore cho trường hồ sơ.
Giữ năm trường cũ của Braiden, nationality/DOB/LEFT của Zepa và bốn trường hồ sơ
còn lại của Gonzalo. Các OVR 60 là giá trị do người dùng chốt, không phải chứng
nhận đã tìm được thẻ cơ bản EA FC27 cho hai người này.

## Nguồn EA và quy tắc vị trí

- Đã đọc đúng 44 hồ sơ EA FC27 chính thức ở lượt thu thập: Position, Alt Positions,
  OVR thẻ cơ bản; giữ các giá trị đó, không thu thập lại ở lượt bổ sung này.
- Kiểm tra tiêu đề FC27, canonical URL/EA ID và lấy đúng
  __NEXT_DATA__.props.pageProps.ratingsEntries.items[0], không dùng Similar players,
  FC26 hoặc thẻ chiến dịch. alternatePositions=null nghĩa là không có vị trí phụ.
- 44 DOB khớp dữ liệu hồ sơ cũ; 43 quốc tịch khớp trực tiếp hoặc tên tương ứng
  (Ireland/Republic of Ireland, Netherlands/Holland, USA/United States).
  Quốc tịch Gonzalo cũ bất thường đã được người dùng xác nhận Spain ở lượt này.
- Joshua King là Josh King (EA70786, DOB2007-01-03, Fulham), không ghép tiền đạo
  Norway. Manuel Ángel (EA84290, DOB2004-03-15) là Manuel Ángel Morán Ibáñez.
  Gonzalo (EA278399, DOB2004-03-24, Fulham) là Gonzalo García.
- CLB EA khác không đổi PremierHub: Maitland-Niles OL, Grealish Manchester City,
  Affengruber Elche CF, Larsson Frankfurt, Manuel Ángel Real Madrid.
- Chỉ CDM → CM cho chính/phụ và gộp trùng, không tự thêm quyền chơi khác.
  RW/RM của Zepa chỉ áp dụng cho anh theo xác nhận người dùng.
- expected_* giữ trống theo snapshot API chưa có vị trí; cả 46 incoming hợp lệ,
  primary nằm trong eligible. Không có dòng incoming trống.

## URL roster và hồ sơ đã lưu

- [Everton roster](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Everton+FC), club_id=1000000062.
- [Fulham roster](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Fulham+FC), club_id=1000000063.
- Hồ sơ cũ: backend/data/everton-profiles-2026-10-02/players.csv và sources.md;
  backend/data/fulham-profiles-2026-10-02/players.csv và sources.md. Không sửa hai batch cũ.
- [EA Everton FC27](https://www.ea.com/games/ea-sports-fc/ratings/teams-ratings/everton/7).
- [EA Fulham FC27](https://www.ea.com/games/ea-sports-fc/ratings/teams-ratings/fulham/144).
- SofaScore Zepa đã mở ở lượt thu thập khớp Cameroon, DOB2008-07-07, LEFT, Fulham U21
  nhưng không ghi height/shirt. Giá trị mới 178cm/số áo35 do người dùng; không lấy
  followers19 làm số áo, không ghi hai giá trị mới là thu thập từ SofaScore.

## Độ phủ local sau bổ sung

| CLB | Roster | Quốc tịch | DOB | Cao | Chân thuận | Số áo | OVR số | Primary + eligible | EA positions | User positions | MISSING |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Everton | 20 | 20 | 20 | 20 | 20 | 20 | 20 | 20 | 19 | 1 | 0 |
| Fulham | 26 | 26 | 26 | 26 | 26 | 26 | 26 | 26 | 25 | 1 | 0 |

## Bản kê áp dụng hồ sơ sau này

profile-updates.csv kê đúng năm ô thay đổi, không phải đầu vào PlayerProfileCsvReader.
profiles.csv là 46 hồ sơ cuối. Importer hồ sơ chỉ chèn và báo xung đột nếu hồ sơ
đã tồn tại khác nội dung; không tự ghi đè.

Snapshot trước bổ sung: Braiden OVR NULL; Zepa OVR/height/shirt NULL; Gonzalo
nationality D Mallorca Yo. Khi được phép ghi MySQL sau này, kiểm tra lại đích,
membership/giá trị hiện hành, tạo SQL backup mới rồi áp dụng đúng năm ô có điều kiện
theo giá trị cũ trước khi chạy hồ sơ cuối. Nationality Gonzalo là ô khác NULL được
người dùng xác nhận sửa; không dùng điều kiện IS NULL cho ô này. Nếu giá trị hiện
hành bằng giá trị cuối thì không cập nhật; nếu khác cả cũ và mới thì báo xung đột.
Lượt này chưa kết nối/ghi MySQL, không tuyên bố dữ liệu đã có production, không
sửa importer/schema để ép cập nhật hoặc nhập lại các batch cũ.

## Nguồn theo từng cầu thủ

URL SofaScore là nguồn năm trường hồ sơ đã lưu; giá trị mới người dùng ghi riêng.
EA chính/phụ là nguyên bản, cột nhập sau CDM → CM. Alt “—” của 44 hồ sơ đã đọc
nghĩa là không có vị trí phụ; hai USER_PROVIDED chưa có hồ sơ EA đã xác minh.

| CLB | player_id | Tên PremierHub | OVR bản cuối | SofaScore đã lưu | EA FC 27 | EA chính; phụ | Nhập chính; eligible | Ghi chú |
| --- | --- | --- | ---: | --- | --- | --- | --- | --- |
| Everton | 2000030191 | Ainsley Maitland-Niles | 77 | [SofaScore](https://www.sofascore.com/football/player/ainsley-maitland-niles/352768) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ainsley-maitland-niles/225782) | RB; RM, CM, RW | RB; RB, RM, CM, RW | EA còn ghi OL; giữ membership Everton của PremierHub. EA ghi OL; giữ membership PremierHub. |
| Everton | 2000030192 | Braiden Graham | 60 | [SofaScore](https://www.sofascore.com/football/player/braiden-graham/1481988) | Chưa xác minh | — | ST; ST | OVR60 và ST do người dùng cung cấp 03/10/2026; không ghi là EA xác minh. |
| Everton | 2000020094 | Carlos Alcaraz | 75 | [SofaScore](https://www.sofascore.com/football/player/carlos-alcaraz/1017392) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/carlos-alcaraz/256402) | CAM; CM, CDM | CAM; CAM, CM | CDM → CM; gộp mã trùng. |
| Everton | 30407 | Christian Nørgaard | 79 | [SofaScore](https://www.sofascore.com/football/player/christian-nrgaard/135256) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/christian-n-rgaard/210697) | CDM; CM | CM; CM | CDM → CM; gộp mã trùng. |
| Everton | 2000015004 | Harrison Armstrong | 73 | [SofaScore](https://www.sofascore.com/football/player/harrison-armstrong/1627560) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/harrison-armstrong/74413) | CM; CDM, CAM | CM; CM, CAM | CDM → CM; gộp mã trùng. |
| Everton | 2000020088 | Hayden Hackney | 77 | [SofaScore](https://www.sofascore.com/football/player/hayden-hackney/1008471) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/hayden-hackney/253054) | CM; CDM, CAM | CM; CM, CAM | CDM → CM; gộp mã trùng. |
| Everton | 2000030029 | Jack Grealish | 82 | [SofaScore](https://www.sofascore.com/football/player/jack-grealish/189061) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jack-grealish/206517) | LM; LW | LM; LM, LW | EA còn ghi Manchester City; giữ membership Everton của PremierHub. EA ghi Manchester City; giữ membership PremierHub. |
| Everton | 2000020112 | Jake O'Brien | 76 | [SofaScore](https://www.sofascore.com/football/player/jake-obrien/998253) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jake-o-brien/253510) | RB; CB, RM | RB; RB, CB, RM |  |
| Everton | 2000020091 | James Garner | 82 | [SofaScore](https://www.sofascore.com/football/player/james-garner/927361) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/james-garner/243657) | CDM; RB, CM | CM; CM, RB | CDM → CM; gộp mã trùng. |
| Everton | 2000020089 | James Tarkowski | 80 | [SofaScore](https://www.sofascore.com/football/player/james-tarkovsky/145188) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/james-tarkowski/202695) | CB; — | CB; CB | URL SofaScore viết Tarkovsky nhưng hồ sơ hiển thị James Tarkowski; ngày sinh và CLB khớp. |
| Everton | 2000015005 | Jarrad Branthwaite | 79 | [SofaScore](https://www.sofascore.com/football/player/jarrad-branthwaite/979563) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jarrad-branthwaite/247649) | CB; — | CB; CB |  |
| Everton | 2000020085 | Jordan Pickford | 85 | [SofaScore](https://www.sofascore.com/football/player/jordan-pickford/138530) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jordan-pickford/204935) | GK; — | GK; GK |  |
| Everton | 2000015001 | Kiernan Dewsbury-Hall | 81 | [SofaScore](https://www.sofascore.com/football/player/kiernan-dewsbury-hall/861970) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kiernan-dewsbury-hall/237386) | CAM; CM, CDM | CAM; CAM, CM | CDM → CM; gộp mã trùng. |
| Everton | 2000020109 | Mark Travers | 72 | [SofaScore](https://www.sofascore.com/football/player/mark-travers/856720) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mark-travers/232284) | GK; — | GK; GK |  |
| Everton | 2000020090 | Merlin Röhl | 74 | [SofaScore](https://www.sofascore.com/football/player/merlin-rohl/1064082) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/merlin-rohl/259714) | CAM; CM, CDM | CAM; CAM, CM | CDM → CM; gộp mã trùng. |
| Everton | 2000020111 | Michael Keane | 76 | [SofaScore](https://www.sofascore.com/football/player/michael-keane/110846) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/michael-keane/207599) | CB; — | CB; CB |  |
| Everton | 2000015002 | Thierno Barry | 76 | [SofaScore](https://www.sofascore.com/football/player/thierno-barry/1395746) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/thierno-barry/276295) | ST; — | ST; ST |  |
| Everton | 2000030190 | Tom King | 64 | [SofaScore](https://www.sofascore.com/football/player/tom-king/603922) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/tom-king/225650) | GK; — | GK; GK |  |
| Everton | 2000020113 | Tyler Dibling | 74 | [SofaScore](https://www.sofascore.com/football/player/tyler-dibling/1174472) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/tyler-dibling/279497) | RM; CAM, RW | RM; RM, CAM, RW |  |
| Everton | 2000020086 | Vitaliy Mykolenko | 77 | [SofaScore](https://www.sofascore.com/football/player/vitaliy-mykolenko/876643) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/vitaliy-mykolenko/244380) | LB; LM | LB; LB, LM |  |
| Fulham | 2000030136 | Alex Iwobi | 80 | [SofaScore](https://www.sofascore.com/football/player/alex-iwobi/352770) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/alex-iwobi/213655) | LM; CDM, LW, CM | LM; LM, CM, LW | CDM → CM; gộp mã trùng. |
| Fulham | 2000030086 | Antonee Robinson | 80 | [SofaScore](https://www.sofascore.com/football/player/antonee-robinson/803174) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/antonee-robinson/229348) | LB; LM | LB; LB, LM |  |
| Fulham | 2000030139 | Benjamin Lecomte | 73 | [SofaScore](https://www.sofascore.com/football/player/benjamin-lecomte/123207) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/benjamin-lecomte/200726) | GK; — | GK; GK |  |
| Fulham | 2000030138 | Bernd Leno | 79 | [SofaScore](https://www.sofascore.com/football/player/bernd-leno/103335) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bernd-leno/192563) | GK; — | GK; GK |  |
| Fulham | 2000030087 | Calvin Bassey | 78 | [SofaScore](https://www.sofascore.com/football/player/calvin-bassey/861972) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/calvin-bassey/241436) | CB; — | CB; CB |  |
| Fulham | 2000030091 | César Palacios | 67 | [SofaScore](https://www.sofascore.com/football/player/cesar-palacios/1402688) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/cesar-palacios/83235) | CAM; CM, ST | CAM; CAM, CM, ST |  |
| Fulham | 2000030197 | David Affengruber | 76 | [SofaScore](https://www.sofascore.com/football/player/david-affengruber/988672) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/david-affengruber/261264) | CB; — | CB; CB | EA ghi Elche CF; giữ membership PremierHub. |
| Fulham | 2000030142 | Emile Smith Rowe | 77 | [SofaScore](https://www.sofascore.com/football/player/emile-smith-rowe/867445) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/emile-smith-rowe/240273) | CAM; LM, CM, LW | CAM; CAM, LM, CM, LW |  |
| Fulham | 2000030082 | Gonzalo García | 75 | [SofaScore](https://www.sofascore.com/football/player/gonzalo-garcia/1402716) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/gonzalo/278399) | ST; — | ST; ST | Nationality Spain do người dùng xác nhận 03/10, sửa D Mallorca Yo. OVR75 và ST/ST giữ nguồn EA. |
| Fulham | 2000030143 | Harrison Reed | 72 | [SofaScore](https://www.sofascore.com/football/player/harrison-reed/365802) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/harrison-reed/205990) | CDM; CM | CM; CM | CDM → CM; gộp mã trùng. |
| Fulham | 2000030195 | Hugo Larsson | 77 | [SofaScore](https://www.sofascore.com/football/player/hugo-larsson/1142211) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/hugo-larsson/268896) | CM; CDM | CM; CM | CDM → CM; gộp mã trùng. EA ghi Frankfurt; giữ membership PremierHub. |
| Fulham | 2729 | Joachim Andersen | 79 | [SofaScore](https://www.sofascore.com/football/player/joachim-andersen/362682) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/joachim-andersen/224221) | CB; — | CB; CB |  |
| Fulham | 2000030193 | Jonah Kusi-Asare | 62 | [SofaScore](https://www.sofascore.com/football/player/jonah-kusi-asare/1503757) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jonah-kusi-asare/278578) | ST; — | ST; ST |  |
| Fulham | 2000030137 | Jorge Cuenca | 76 | [SofaScore](https://www.sofascore.com/football/player/jorge-cuenca/868715) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jorge-cuenca/237522) | CB; — | CB; CB |  |
| Fulham | 2000030083 | Joshua King | 74 | [SofaScore](https://www.sofascore.com/football/player/joshua-king/1546231) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/josh-king/70786) | CAM; CM, CDM | CAM; CAM, CM | CDM → CM; gộp mã trùng. EA Josh King ID70786, DOB2007-01-03, Fulham; đúng người trẻ England. |
| Fulham | 2000030140 | Kenny Tete | 78 | [SofaScore](https://www.sofascore.com/football/player/kenny-tete/190877) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kenny-tete/216266) | RB; RM | RB; RB, RM |  |
| Fulham | 2000030090 | Kevin | 76 | [SofaScore](https://www.sofascore.com/football/player/kevin/1112879) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kevin/70571) | LM; LW | LM; LM, LW |  |
| Fulham | 2000030194 | Macaulay Zepa | 60 | [SofaScore](https://www.sofascore.com/football/player/macaulay-zepa/2495972) | Chưa xác minh | — | RW; RW, RM | Người dùng 03/10: OVR60, primary RW, eligible RW/RM, height178cm, shirt35. Các giá trị mới không gắn nguồn EA/SofaScore. |
| Fulham | 2000030196 | Manuel Ángel Morán Ibáñez | 65 | [SofaScore](https://www.sofascore.com/football/player/manuel-angel-moran/1142683) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/manuel-angel/84290) | CM; CDM, CAM | CM; CM, CAM | SofaScore ghi Manuel Ángel Morán. EA dùng tên Manuel Ángel; giữ membership Fulham của PremierHub. CDM → CM; gộp mã trùng. EA ghi Real Madrid; giữ membership PremierHub. |
| Fulham | 2000030093 | Oscar Bobb | 76 | [SofaScore](https://www.sofascore.com/football/player/oscar-bobb/1065216) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/oscar-bobb/277295) | RM; LM, RW, LW | RM; RM, LM, RW, LW |  |
| Fulham | 2000030085 | Rodrigo Muniz | 76 | [SofaScore](https://www.sofascore.com/football/player/rodrigo-muniz/1015256) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/rodrigo-muniz/264337) | ST; — | ST; ST |  |
| Fulham | 2000030088 | Ryan Sessegnon | 77 | [SofaScore](https://www.sofascore.com/football/player/ryan-sessegnon/836698) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ryan-sessegnon/235883) | LB; LM | LB; LB, LM |  |
| Fulham | 2000030089 | Sander Berge | 79 | [SofaScore](https://www.sofascore.com/football/player/sander-berge/793167) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/sander-berge/228092) | CDM; CM | CM; CM | CDM → CM; gộp mã trùng. |
| Fulham | 2000030092 | Shea Charles | 73 | [SofaScore](https://www.sofascore.com/football/player/shea-charles/1131449) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/shea-charles/276695) | CDM; CM | CM; CM | CDM → CM; gộp mã trùng. |
| Fulham | 2000030084 | Timothy Castagne | 75 | [SofaScore](https://www.sofascore.com/football/player/timothy-castagne/329417) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/timothy-castagne/222501) | RB; RM | RB; RB, RM |  |
| Fulham | 19025 | Tom Cairney | 74 | [SofaScore](https://www.sofascore.com/football/player/tom-cairney/82566) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/tom-cairney/195202) | CM; CDM | CM; CM | CDM → CM; gộp mã trùng. |

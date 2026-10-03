# Everton – Fulham: hồ sơ, OVR và vị trí 2026/27

Ngày chuẩn bị: 2026-10-03. Chỉ dữ liệu local, chưa ghi production.

## Roster và bảo toàn dữ liệu

- Roster hiện hành PremierHub season=2026, asOf=2026-10-03: Everton 20, Fulham 26.
  Toàn bộ 46 ID/CLB khớp hai batch hồ sơ 02/10; không thêm người từ roster bên ngoài.
- profiles.csv có đúng 46 dòng. Giữ nguyên mọi giá trị đã có trong API và CSV hồ sơ
  cũ; không đổi NULL thành 0, không sửa membership, nhóm rộng hoặc thống kê trận.
- 44 OVR số đang có, hai OVR NULL (Braiden Graham và Macaulay Zepa). Không có OVR
  người dùng mới cho hai người này. File missing-fields người dùng 02/10 đánh dấu x;
  lượt này không tự cấp OVR thay thế. Người thiếu OVR/vị trí chưa đủ chọn Fantasy.
- Năm trường hồ sơ đã đủ của người khác không thu thập lại. URL SofaScore bên dưới
  kế thừa nguồn hồ sơ 02/10. Chỉ hồ sơ Macaulay Zepa được mở lại để tìm hai ô thiếu.

## Nguồn vị trí và OVR

- Đã đọc đúng 44 hồ sơ EA SPORTS FC 27 từ URL/EA ID đã có: Position, Alt Positions,
  overallRating của thẻ cơ bản. Các OVR khớp giá trị đang lưu, nên không cập nhật lại.
- Đọc HTML chính thức, kiểm tra tiêu đề FC 27, canonical EA ID, và lấy đúng đối tượng
  __NEXT_DATA__.props.pageProps.ratingsEntries.items[0]. Không lấy từ Similar players,
  FC 26 hay thẻ chiến dịch. alternatePositions=null nghĩa là không có vị trí phụ.
- Ghép bằng tên/biến thể tên, EA ID, ngày sinh chính xác; 44 DOB khớp hồ sơ hiện có.
  43 quốc tịch khớp trực tiếp hoặc qua Ireland/Republic of Ireland, Netherlands/Holland,
  USA/United States. Gonzalo có giá trị quốc tịch cũ cần review riêng; tên đầy đủ,
  DOB 2004-03-24, EA ID278399 và CLB Fulham xác nhận đúng người cho OVR/vị trí.
- Joshua King là Josh King sinh 2007-01-03, EA ID70786 (Fulham); không ghép với
  tiền đạo Joshua King người Norway. Manuel Ángel Morán Ibáñez là Manuel Ángel
  sinh 2004-03-15, EA ID84290. Gonzalo García là Gonzalo, EA ID278399.
- CLB trên EA có thể khác: Maitland-Niles OL, Grealish Manchester City,
  Affengruber Elche CF, Larsson Frankfurt, Manuel Ángel Real Madrid. Giữ CLB PremierHub.
- Chỉ CDM → CM cho chính và phụ, gộp mã trùng sau ánh xạ. Không suy từ nhóm rộng,
  không thêm RM cho một RW không có căn cứ. Primary luôn nằm trong eligible.
- positions.csv có 44 dòng: Everton 19, Fulham 25. expected_* trống theo snapshot API.
  Hai MISSING không có dòng incoming trống vì reader yêu cầu vị trí hợp lệ;
  vẫn có dòng profiles.csv và roster-status.csv cho mỗi ID thuộc roster.

## Roster và nguồn hồ sơ cũ

- [Everton FC roster](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Everton+FC); club_id=1000000062; nguồn hồ sơ cũ: backend/data/everton-profiles-2026-10-02/players.csv và sources.md.
- [Fulham FC roster](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Fulham+FC); club_id=1000000063; nguồn hồ sơ cũ: backend/data/fulham-profiles-2026-10-02/players.csv và sources.md.

- [EA Everton FC 27](https://www.ea.com/games/ea-sports-fc/ratings/teams-ratings/everton/7).
- [EA Fulham FC 27](https://www.ea.com/games/ea-sports-fc/ratings/teams-ratings/fulham/144).

## Độ phủ local

| CLB | Roster | Quốc tịch có ô | DOB | Cao | Chân thuận | Số áo | Đủ 5 ô hồ sơ | OVR | Primary + eligible | MISSING vị trí |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Everton | 20 | 20 | 20 | 20 | 20 | 20 | 20 | 19 | 19 | 1 |
| Fulham | 26 | 26 | 26 | 25 | 26 | 25 | 25 | 25 | 25 | 1 |

“Có ô” chỉ thống kê giá trị không NULL, không khẳng định nội dung quốc tịch bất thường
của Gonzalo đúng. Fulham có 24 hồ sơ đủ năm ô không bị gắn review, một hồ sơ
REVIEW_REQUIRED (Gonzalo) và một hồ sơ MISSING (Zepa).

## Thiếu và cần xác nhận

- **Braiden Graham – 2000030192, Everton:** fc27_overall, primary_position,
  eligible_positions. Năm trường hồ sơ đã đủ. Tìm chính xác tên và biến thể trên EA
  (có và không thêm FC27, player-ratings); chưa tìm được hồ sơ FC 27 đúng người.
  Không suy từ MIDFIELDER hay nguồn khác. OVR và vị trí giữ NULL/MISSING.
- **Macaulay Zepa – 2000030194, Fulham:** height_cm, shirt_number, fc27_overall,
  primary_position, eligible_positions. Đã mở đúng
  https://www.sofascore.com/football/player/macaulay-zepa/2495972 : tên, Cameroon,
  DOB 2008-07-07, LEFT và Fulham U21 khớp dữ liệu đã có. General info không có
  chiều cao hoặc số áo; không lấy số 19 followers thành số áo. Thử tải HTML/API
  SofaScore trực tiếp bị ngắt kết nối; không suy chiều cao/số áo từ nguồn khác.
  Tìm tên đầy đủ/Zepa trên EA chưa có hồ sơ FC27 đủ chắc chắn. Giữ các ô thiếu NULL.
- **Gonzalo García – 2000030082, Fulham:** nationality cần xác nhận; giá trị hiện có
  “D Mallorca Yo” không phải tên quốc gia đã xác minh, EA ghi Spain. Giữ nguyên theo
  yêu cầu bảo toàn dữ liệu; không lấy EA thay SofaScore cho trường hồ sơ và không
  sửa ô đã điền trong lượt này. profile_status=REVIEW_REQUIRED, không đánh dấu
  nationality là ô NULL. OVR75, ST và tập ST vẫn xác minh được bằng đúng người.
  Nguồn hồ sơ đã lưu: https://www.sofascore.com/football/player/gonzalo-garcia/1402716 .

roster-status.csv missing_fields chỉ kê ô thực sự thiếu; trường cần review nhưng đã
có giá trị được nhận diện bởi profile_status=REVIEW_REQUIRED và ghi rõ ở phần này
cùng missing-fields.txt. Không tuyên bố cả 46 người đủ dữ liệu hoặc đủ Fantasy.

## Nguồn theo từng cầu thủ

URL SofaScore là nguồn lưu từ batch cũ; chỉ Macaulay đọc lại ô thiếu. EA ghi nguyên
Position và Alt Positions, cột nhập ghi sau CDM → CM. Alt “—” là không có vị trí phụ.

| CLB | player_id | Tên PremierHub | OVR giữ nguyên | SofaScore đã lưu | EA FC 27 | EA chính; phụ | Nhập chính; eligible | Ghi chú |
| --- | --- | --- | ---: | --- | --- | --- | --- | --- |
| Everton | 2000030191 | Ainsley Maitland-Niles | 77 | [SofaScore](https://www.sofascore.com/football/player/ainsley-maitland-niles/352768) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ainsley-maitland-niles/225782) | RB; RM, CM, RW | RB; RB, RM, CM, RW | EA còn ghi OL; giữ membership Everton của PremierHub. EA ghi OL; giữ membership PremierHub. |
| Everton | 2000030192 | Braiden Graham | NULL | [SofaScore](https://www.sofascore.com/football/player/braiden-graham/1481988) | Chưa xác minh | MISSING | MISSING | Chưa tìm được hồ sơ EA FC 27 đúng người; thiếu fc27_overall, primary_position, eligible_positions. Giữ NULL, không suy vị trí. |
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
| Fulham | 2000030082 | Gonzalo García | 75 | [SofaScore](https://www.sofascore.com/football/player/gonzalo-garcia/1402716) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/gonzalo/278399) | ST; — | ST; ST | REVIEW_REQUIRED nationality: giữ D Mallorca Yo; EA Spain không được dùng sửa hồ sơ. |
| Fulham | 2000030143 | Harrison Reed | 72 | [SofaScore](https://www.sofascore.com/football/player/harrison-reed/365802) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/harrison-reed/205990) | CDM; CM | CM; CM | CDM → CM; gộp mã trùng. |
| Fulham | 2000030195 | Hugo Larsson | 77 | [SofaScore](https://www.sofascore.com/football/player/hugo-larsson/1142211) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/hugo-larsson/268896) | CM; CDM | CM; CM | CDM → CM; gộp mã trùng. EA ghi Frankfurt; giữ membership PremierHub. |
| Fulham | 2729 | Joachim Andersen | 79 | [SofaScore](https://www.sofascore.com/football/player/joachim-andersen/362682) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/joachim-andersen/224221) | CB; — | CB; CB |  |
| Fulham | 2000030193 | Jonah Kusi-Asare | 62 | [SofaScore](https://www.sofascore.com/football/player/jonah-kusi-asare/1503757) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jonah-kusi-asare/278578) | ST; — | ST; ST |  |
| Fulham | 2000030137 | Jorge Cuenca | 76 | [SofaScore](https://www.sofascore.com/football/player/jorge-cuenca/868715) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jorge-cuenca/237522) | CB; — | CB; CB |  |
| Fulham | 2000030083 | Joshua King | 74 | [SofaScore](https://www.sofascore.com/football/player/joshua-king/1546231) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/josh-king/70786) | CAM; CM, CDM | CAM; CAM, CM | CDM → CM; gộp mã trùng. EA Josh King ID70786, DOB2007-01-03, Fulham; đúng người trẻ England. |
| Fulham | 2000030140 | Kenny Tete | 78 | [SofaScore](https://www.sofascore.com/football/player/kenny-tete/190877) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kenny-tete/216266) | RB; RM | RB; RB, RM |  |
| Fulham | 2000030090 | Kevin | 76 | [SofaScore](https://www.sofascore.com/football/player/kevin/1112879) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kevin/70571) | LM; LW | LM; LM, LW |  |
| Fulham | 2000030194 | Macaulay Zepa | NULL | [SofaScore](https://www.sofascore.com/football/player/macaulay-zepa/2495972) | Chưa xác minh | MISSING | MISSING | Chưa tìm được hồ sơ EA FC 27 đúng người; thiếu height_cm, shirt_number, fc27_overall, primary_position, eligible_positions. Giữ NULL, không suy vị trí. Đã mở SofaScore; vẫn không ghi height/shirt number. |
| Fulham | 2000030196 | Manuel Ángel Morán Ibáñez | 65 | [SofaScore](https://www.sofascore.com/football/player/manuel-angel-moran/1142683) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/manuel-angel/84290) | CM; CDM, CAM | CM; CM, CAM | SofaScore ghi Manuel Ángel Morán. EA dùng tên Manuel Ángel; giữ membership Fulham của PremierHub. CDM → CM; gộp mã trùng. EA ghi Real Madrid; giữ membership PremierHub. |
| Fulham | 2000030093 | Oscar Bobb | 76 | [SofaScore](https://www.sofascore.com/football/player/oscar-bobb/1065216) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/oscar-bobb/277295) | RM; LM, RW, LW | RM; RM, LM, RW, LW |  |
| Fulham | 2000030085 | Rodrigo Muniz | 76 | [SofaScore](https://www.sofascore.com/football/player/rodrigo-muniz/1015256) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/rodrigo-muniz/264337) | ST; — | ST; ST |  |
| Fulham | 2000030088 | Ryan Sessegnon | 77 | [SofaScore](https://www.sofascore.com/football/player/ryan-sessegnon/836698) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ryan-sessegnon/235883) | LB; LM | LB; LB, LM |  |
| Fulham | 2000030089 | Sander Berge | 79 | [SofaScore](https://www.sofascore.com/football/player/sander-berge/793167) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/sander-berge/228092) | CDM; CM | CM; CM | CDM → CM; gộp mã trùng. |
| Fulham | 2000030092 | Shea Charles | 73 | [SofaScore](https://www.sofascore.com/football/player/shea-charles/1131449) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/shea-charles/276695) | CDM; CM | CM; CM | CDM → CM; gộp mã trùng. |
| Fulham | 2000030084 | Timothy Castagne | 75 | [SofaScore](https://www.sofascore.com/football/player/timothy-castagne/329417) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/timothy-castagne/222501) | RB; RM | RB; RB, RM |  |
| Fulham | 19025 | Tom Cairney | 74 | [SofaScore](https://www.sofascore.com/football/player/tom-cairney/82566) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/tom-cairney/195202) | CM; CDM | CM; CM | CDM → CM; gộp mã trùng. |

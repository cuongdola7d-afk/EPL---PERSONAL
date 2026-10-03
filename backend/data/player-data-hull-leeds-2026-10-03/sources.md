# Hull City AFC – Leeds United FC: hồ sơ, OVR và vị trí 2026/27

Ngày chuẩn bị: 2026-10-03. Chỉ dữ liệu local, chưa ghi production.

## Roster và bảo toàn dữ liệu

- Danh sách lấy từ PremierHub season=2026, asOf=2026-10-03; toàn bộ roster hiện hành,
  không giới hạn25 người, không thêm ID từ EA hoặc SofaScore.
- profiles.csv và roster-status.csv có đúng56 ID/CLB, một dòng mỗi ID hiện hành.
  Giữ nguyên sáu giá trị hồ sơ/OVR từ API; không thu thập lại ô đã đủ hoặc gán0.
- Không đổi membership, nhóm rộng, thống kê trận, OVR đã có hay dữ liệu/logic2024/25.
- URL SofaScore giữ từ các batch hồ sơ02/10 bên dưới. Nếu thấy khác dữ liệu nhận dạng
  giữa nguồn, chỉ đọc lại hồ sơ để xác minh đúng người; không dùng EA sửa trường hồ sơ.
- positions.csv có56 incoming. expected_* trống vì snapshot API chưa có
  vị trí. Primary luôn trong eligible; chỉ CDM → CM cho chính/phụ và gộp mã trùng.
  Không tự thêm quyền RM cho RW hoặc suy từ nhóm vị trí rộng.

## Nguồn EA và nhận dạng

- Đọc đúng hồ sơ EA FC27 chính thức đã lưu URL/EA ID: Position, Alt Positions,
  OVR thẻ cơ bản. Không dùng FC26, thẻ chiến dịch hay Similar players.
- Đa số trang đọc trực tiếp HTML, xác minh tiêu đềFC27/canonicalEA ID, lấy đúng
  __NEXT_DATA__.props.pageProps.ratingsEntries.items[0]. alternatePositions=null
  là không có vị trí phụ. Tệp HTML/log tạm ở backend/target/, không commit.
- Giữ các OVR hiện có vì toàn bộ OVR từ các hồ sơ đọc được đều khớp API.
- Ghép theo tên/biến thể tên, EA ID, quốc tịch, ngày sinh/tuổi và CLB. Các tên quốc gia
  Ireland/Republic of Ireland, Netherlands/Holland, USA/United States, DR Congo/Congo DR
  là tên tương ứng. Nhãn CLB EA/SofaScore khác không thay membership PremierHub.
- Toàn sáu CLB có157 hồ sơ EA:152 DOB khớp chính xác,4 DOB khác nguồn và1 Omar
  chỉ xác minh tuổi28 qua trang EA truy cập được. Khác nguồn được kê riêng bên dưới,
  không sửa DOB/quốc tịch để ép khớp và không tuyên bố cả157 DOB trùng.

## URL roster và nguồn hồ sơ cũ

- [Hull City AFC roster](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Hull+City+AFC), club_id=1000000322, 34 người. Nguồn hồ sơ cũ: backend/data/hull-city-profiles-2026-10-02/players.csv và sources.md.
- [Leeds United FC roster](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Leeds+United+FC), club_id=1000000341, 22 người. Nguồn hồ sơ cũ: backend/data/leeds-profiles-2026-10-02/players.csv và sources.md.

## Alfie Cresswell và ngôn ngữ nguồn

Alfie Cresswell2000020081 có height_cm178 do người dùng cung cấp02/10, đã có trong
API; giữ178. CSV hồ sơ02/10 là snapshot cũ còn ô trống; không lấy snapshot đó thay
giá trị API. Không thu thập lại chiều cao hoặc tạo cập nhật SQL mới cho ô đã đủ này.
Nguồn bổ sung: backend/data/player-profile-updates-2026-10-02.csv và file.md tương ứng.

Bahoya: URL nguồn cũ tiếng Tây Ban Nha ghi mã MI/MCO/EI/MC; đã đọc đúng hồ sơ
tiếng Anh cùng EA ID269728, Position LM; Alt CAM,LW,CM. Không thêm quyền ngoài nguồn.

## Khác biệt giữa nguồn đã ghi nhận

Các ô này đã có giá trị, không phải NULL/MISSING; giữ hồ sơ hiện có theo SofaScore.
Chỉ ghi khác biệt để review, không tự sửa theo EA. Ghép người bằng tên duy nhất,
EA ID/URL, quốc tịch/ngày sinh còn lại và CLB; không có bằng chứng ghép một người khác.
Gyabi/Slater/Mendy được mở lại đúng SofaScore: tên/CLB Hull, quốc tịch và DOB trùng
profile đã lưu. Sidiki cùng tên/DOB/CLB Coventry dù quốc tịch EA khác. Juan dùng tên
đầy đủ/Ecuador/EA85136 và SofaScore đúng người, DOB lệch1 ngày; không đổi hồ sơ.

| player_id | Tên | Trường | Giữ hiện có/SofaScore | EA ghi |
| --- | --- | --- | --- | --- |
| 2000030204 | Darko Gyabi | birth_date | 2004-02-17 | 2004-02-18 |
| 2000010004 | Regan Slater | birth_date | 1999-09-11 | 1999-09-10 |
| 2000010003 | Nobel Mendy | birth_date | 2004-08-16 | 2004-09-03 |

## Độ phủ local

| CLB | Roster | Quốc tịch | DOB | Cao | Chân thuận | Số áo | OVR | Vị trí | MISSING vị trí |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Hull City AFC | 34 | 34 | 34 | 34 | 34 | 34 | 34 | 34 | 0 |
| Leeds United FC | 22 | 22 | 22 | 22 | 22 | 22 | 22 | 22 | 0 |

## Nguồn từng cầu thủ

SofaScore là nguồn năm trường hồ sơ giữ từ batch cũ; EA là nguồn OVR/chính/phụ.
Alt “—” là không có vị trí phụ trên hồ sơ đã đọc; George chưa có EA là MISSING.

| CLB | player_id | Tên PremierHub | OVR giữ | SofaScore | EA FC27 | EA chính; phụ | Nhập chính; eligible | Ghi chú |
| --- | --- | --- | ---: | --- | --- | --- | --- | --- |
| Hull City AFC | 2000030202 | Abdülkadir Ömür | 69 | [SofaScore](https://www.sofascore.com/football/player/abdulkadir-omur/826155) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/abdulkadir-omur/231777) | RM; CAM, CM, RB | RM; RM, CAM, CM, RB | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000030210 | Brooke Norton-Cuffy | 75 | [SofaScore](https://www.sofascore.com/football/player/brooke-norton-cuffy/1087514) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/brooke-norton-cuffy/260653) | RB; RM | RB; RB, RM | EA CLB: Genoa; membership giữ PremierHub. |
| Hull City AFC | 2000030200 | Cathal McCarthy | 60 | [SofaScore](https://www.sofascore.com/football/player/cathal-mccarthy/1845232) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/cathal-mc-carthy/79579) | CB; CDM, CM | CB; CB, CM | SofaScore và EA hiện ghi Kilmarnock; giữ membership Hull của PremierHub, số áo lấy từ SofaScore. CDM → CM; gộp trùng. EA CLB: Kilmarnock; membership giữ PremierHub. |
| Hull City AFC | 2000030199 | Charlie Hughes | 74 | [SofaScore](https://www.sofascore.com/football/player/charlie-hughes/1138398) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/charlie-hughes/272895) | CB; — | CB; CB | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000030212 | Christos Mouzakitis | 73 | [SofaScore](https://www.sofascore.com/football/player/christos-mouzakitis/1416540) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/christos-mouzakitis/73884) | CM; CAM, CDM | CM; CM, CAM | EA còn ghi Olympiacos; giữ membership Hull của PremierHub. CDM → CM; gộp trùng. EA CLB: Olympiacos FC; membership giữ PremierHub. |
| Hull City AFC | 2000030204 | Darko Gyabi | 68 | [SofaScore](https://www.sofascore.com/football/player/darko-gyabi/1067117) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/darko-gyabi/270209) | CM; CDM, CAM | CM; CM, CAM | CDM → CM; gộp trùng. Khác nguồn birth_date: giữ2004-02-17; EA2004-02-18. EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000020015 | Dillon Phillips | 66 | [SofaScore](https://www.sofascore.com/football/player/dillon-phillips/377166) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/dillon-phillips/220058) | GK; — | GK; GK | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000030201 | Eliot Matazo | 70 | [SofaScore](https://www.sofascore.com/football/player/eliot-matazo/1046150) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/eliot-matazo/258433) | CDM; CM | CM; CM | CDM → CM; gộp trùng. EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000020006 | Elliot Stroud | 74 | [SofaScore](https://www.sofascore.com/football/player/stroud-elliot/1383695) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/elliot-stroud/272712) | LB; LM, LW | LB; LB, LM, LW | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000030206 | Hidemasa Morita | 78 | [SofaScore](https://www.sofascore.com/football/player/hidemasa-morita/926560) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/hidemasa-morita/242087) | CM; CDM | CM; CM | CDM → CM; gộp trùng. EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000030208 | Ilyas Ansah | 73 | [SofaScore](https://www.sofascore.com/football/player/ansah-ilyas/1462742) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ilyas-ansah/276372) | ST; CAM, LW, LM | ST; ST, CAM, LW, LM | EA còn ghi Union Berlin; giữ membership Hull của PremierHub. EA CLB: Union Berlin; membership giữ PremierHub. |
| Hull City AFC | 2000030198 | Jack Butland | 75 | [SofaScore](https://www.sofascore.com/football/player/jack-butland/98448) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jack-butland/203042) | GK; — | GK; GK | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000020018 | Jens Hjertø-Dahl | 68 | [SofaScore](https://www.sofascore.com/football/player/jens-hjerto-dahl/1427971) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jens-hjert-dahl/274979) | CM; CDM | CM; CM | CDM → CM; gộp trùng. EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000030205 | Joe Gelhardt | 73 | [SofaScore](https://www.sofascore.com/football/player/joe-gelhardt/945806) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/joe-gelhardt/246053) | CAM; RM, CM, RW | CAM; CAM, RM, CM, RW | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000010005 | John Egan | 72 | [SofaScore](https://www.sofascore.com/football/player/john-egan/100578) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/john-egan/204936) | CB; — | CB; CB | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000020017 | Kieran Dowell | 68 | [SofaScore](https://www.sofascore.com/football/player/kieran-dowell/784503) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kieran-dowell/226401) | RM; CAM, RW, CM | RM; RM, CAM, RW, CM | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000010001 | Konstantinos Tzolakis | 79 | [SofaScore](https://www.sofascore.com/football/player/konstantinos-tzolakis/953414) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/konstantinos-tzolakis/252552) | GK; — | GK; GK | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000020004 | Lewie Coyle | 70 | [SofaScore](https://www.sofascore.com/football/player/lewie-coyle/827587) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/lewie-coyle/224099) | RB; LB, RM, LM | RB; RB, LB, RM, LM | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000020016 | Lucas Gourna-Douath | 70 | [SofaScore](https://www.sofascore.com/football/player/lucas-gourna-douath/1012657) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/lucas-gourna-douath/257271) | CDM; CM | CM; CM | CDM → CM; gộp trùng. EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000020010 | Lucas Herrington | 66 | [SofaScore](https://www.sofascore.com/football/player/lucas-herrington/1646783) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/lucas-herrington/75145) | CB; — | CB; CB | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000020007 | Matt Crooks | 71 | [SofaScore](https://www.sofascore.com/football/player/matt-crooks/140479) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/matt-crooks/202693) | CAM; CM, CDM | CAM; CAM, CM | CDM → CM; gộp trùng. EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000020014 | Matt Targett | 72 | [SofaScore](https://www.sofascore.com/football/player/matt-targett/368134) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/matt-targett/218659) | LB; LM, CB | LB; LB, LM, CB | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000020009 | Mohamed Belloumi | 73 | [SofaScore](https://www.sofascore.com/football/player/mohamed-belloumi/1125839) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mohamed-belloumi/277663) | RM; RW | RM; RM, RW | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000030207 | Mohamed-Ali Cho | 74 | [SofaScore](https://www.sofascore.com/football/player/mohamed-ali-cho/1063235) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mohamed-ali-cho/256476) | RW; ST, RM | RW; RW, ST, RM | SofaScore hiển thị Mohamed Ali Cho; EA còn ghi OGC Nice. Giữ membership Hull của PremierHub. EA CLB: OGC Nice; membership giữ PremierHub. |
| Hull City AFC | 2000010003 | Nobel Mendy | 75 | [SofaScore](https://www.sofascore.com/football/player/nobel-mendy/1458073) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/nobel-mendy/276633) | CB; LB | CB; CB, LB | Khác nguồn birth_date: giữ2004-08-16; EA2004-09-03. EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000020008 | Oli McBurnie | 75 | [SofaScore](https://www.sofascore.com/football/player/oli-mcburnie/367228) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/oli-mc-burnie/220031) | ST; — | ST; ST | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000020011 | Paddy McNair | 72 | [SofaScore](https://www.sofascore.com/football/player/paddy-mcnair/592876) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/paddy-mc-nair/213697) | CB; — | CB; CB | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000010004 | Regan Slater | 72 | [SofaScore](https://www.sofascore.com/football/player/regan-slater/864474) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/regan-slater/241953) | CDM; CM | CM; CM | CDM → CM; gộp trùng. Khác nguồn birth_date: giữ1999-09-11; EA1999-09-10. EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000030209 | Robinio Vaz | 71 | [SofaScore](https://www.sofascore.com/football/player/robinio-vaz/1514800) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/robinio-vaz/76410) | ST; — | ST; ST | EA còn ghi AS Roma; giữ membership Hull của PremierHub. EA CLB: AS Roma; membership giữ PremierHub. |
| Hull City AFC | 2000020005 | Ryan Giles | 72 | [SofaScore](https://www.sofascore.com/football/player/ryan-giles/931304) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ryan-giles/243608) | LB; LM | LB; LB, LM | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000010002 | Semi Ajayi | 70 | [SofaScore](https://www.sofascore.com/football/player/semi-ajayi/307274) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/semi-ajayi/207952) | CB; — | CB; CB | EA CLB: Hull City; membership giữ PremierHub. |
| Hull City AFC | 2000030211 | Sorba Thomas | 75 | [SofaScore](https://www.sofascore.com/football/player/sorba-thomas/911039) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/sorba-thomas/260478) | LM; RM, RB, LW | LM; LM, RM, RB, LW | EA còn ghi Stoke City; giữ membership Hull của PremierHub. EA CLB: Stoke City; membership giữ PremierHub. |
| Hull City AFC | 284500 | Tim Iroegbunam | 76 | [SofaScore](https://www.sofascore.com/football/player/tim-iroegbunam/1085950) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/tim-iroegbunam/266609) | CDM; CM | CM; CM | EA còn ghi Everton; giữ membership Hull của PremierHub. CDM → CM; gộp trùng. EA CLB: Everton; membership giữ PremierHub. |
| Hull City AFC | 2000030203 | Óscar Zambrano | 70 | [SofaScore](https://www.sofascore.com/football/player/oscar-zambrano/1145130) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/oscar-zambrano/268599) | CDM; CM | CM; CM | CDM → CM; gộp trùng. EA CLB: Hull City; membership giữ PremierHub. |
| Leeds United FC | 2000030223 | Alex Cairns | 62 | [SofaScore](https://www.sofascore.com/football/player/alex-cairns/162417) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/alex-cairns/203215) | GK; — | GK; GK | EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000020081 | Alfie Cresswell | 60 | [SofaScore](https://www.sofascore.com/football/player/alfie-cresswell/1809530) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/alfie-cresswell/85912) | CM; CDM | CM; CM | Giữ height178 người dùng02/10, đã có API; note NULL trong snapshot02/10 đã cũ. CDM → CM; gộp trùng. EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000014001 | Anton Stach | 80 | [SofaScore](https://www.sofascore.com/football/player/anton-stach/889861) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/anton-stach/257191) | CM; CDM | CM; CM | CDM → CM; gộp trùng. EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000020082 | Ao Tanaka | 76 | [SofaScore](https://www.sofascore.com/football/player/ao-tanaka/871886) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ao-tanaka/236764) | CM; CDM | CM; CM | CDM → CM; gộp trùng. EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000020083 | Daniel James | 75 | [SofaScore](https://www.sofascore.com/football/player/daniel-james/828639) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/daniel-james/232104) | RW; RM | RW; RW, RM | EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000020067 | Dominic Calvert-Lewin | 79 | [SofaScore](https://www.sofascore.com/football/player/dominic-calvert-lewin/372344) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/dominic-calvert-lewin/221479) | ST; — | ST; ST | EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000030220 | Gabriel Gudmundsson | 78 | [SofaScore](https://www.sofascore.com/football/player/gabriel-gudmundsson/834308) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/gabriel-gudmundsson/236822) | LB; LM | LB; LB, LM | EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000020066 | Harry Wilson | 81 | [SofaScore](https://www.sofascore.com/football/player/harry-wilson/355528) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/harry-wilson/220710) | RW; RM, CAM | RW; RW, RM, CAM | EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000030221 | Ilia Gruev | 75 | [SofaScore](https://www.sofascore.com/football/player/ilia-gruev/911679) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ilia-gruev/248384) | CDM; CM | CM; CM | CDM → CM; gộp trùng. EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000014003 | Jaka Bijol | 77 | [SofaScore](https://www.sofascore.com/football/player/jaka-bijol/886930) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jaka-bijol/244238) | CB; — | CB; CB | EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000014004 | James Justin | 76 | [SofaScore](https://www.sofascore.com/football/player/james-justin/827681) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/james-justin/231554) | RB; LB, CB | RB; RB, LB, CB | EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000014002 | James Trafford | 79 | [SofaScore](https://www.sofascore.com/football/player/james-trafford/980643) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/james-trafford/263063) | GK; — | GK; GK | EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000020079 | Jayden Lienou | 60 | [SofaScore](https://www.sofascore.com/football/player/jayden-lienou/1899485) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jayden-lienou/85700) | LB; LM | LB; LB, LM | EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000030226 | Jean-Mattéo Bahoya | 76 | [SofaScore](https://www.sofascore.com/football/player/jean-matteo-bahoya/1146148) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jean-matteo-bahoya/269728) | LM; CAM, LW, CM | LM; LM, CAM, LW, CM | EA còn ghi Frankfurt; giữ membership Leeds của PremierHub. EA CLB: Frankfurt; membership giữ PremierHub. |
| Leeds United FC | 2000014005 | Joe Rodon | 77 | [SofaScore](https://www.sofascore.com/football/player/joe-rodon/828640) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/joe-rodon/229266) | CB; — | CB; CB | EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000020084 | Lukas Nmecha | 76 | [SofaScore](https://www.sofascore.com/football/player/lukas-nmecha/803185) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/lukas-nmecha/230084) | ST; — | ST; ST | EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000030222 | Mateo Joseph Fernández-Regatillo | 72 | [SofaScore](https://www.sofascore.com/football/player/mateo-joseph/1170693) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mateo-joseph/270206) | ST; RM, CAM, RW | ST; ST, RM, CAM, RW | SofaScore và EA dùng tên ngắn Mateo Joseph; ngày sinh và CLB khớp roster PremierHub. EA dùng tên Mateo Joseph. EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000030225 | Melvin Bard | 76 | [SofaScore](https://www.sofascore.com/football/player/melvin-bard/906075) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/melvin-bard/254548) | LB; LM, CB, CDM | LB; LB, LM, CB, CM | EA còn ghi OGC Nice; giữ membership Leeds của PremierHub. CDM → CM; gộp trùng. EA CLB: OGC Nice; membership giữ PremierHub. |
| Leeds United FC | 2000030224 | Michael Zetterer | 75 | [SofaScore](https://www.sofascore.com/football/player/michael-zetterer/190161) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/michael-zetterer/227370) | GK; — | GK; GK | EA còn ghi Frankfurt; giữ membership Leeds của PremierHub. EA CLB: Frankfurt; membership giữ PremierHub. |
| Leeds United FC | 2000020080 | Nico Elvedi | 78 | [SofaScore](https://www.sofascore.com/football/player/nico-elvedi/282229) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/nico-elvedi/221491) | CB; — | CB; CB | EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000020072 | Sean Longstaff | 76 | [SofaScore](https://www.sofascore.com/football/player/sean-longstaff/866191) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/sean-longstaff/237161) | CM; CDM | CM; CM | CDM → CM; gộp trùng. EA CLB: Leeds United; membership giữ PremierHub. |
| Leeds United FC | 2000020070 | Tarik Muharemović | 75 | [SofaScore](https://www.sofascore.com/football/player/tarik-muharemovic/1118177) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/tarik-muharemovic/262027) | CB; — | CB; CB | EA CLB: Leeds United; membership giữ PremierHub. |

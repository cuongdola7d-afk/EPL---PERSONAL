# Ipswich Town FC – Sunderland AFC: hồ sơ, OVR và vị trí 2026/27

Ngày chuẩn bị: 2026-10-03. Chỉ dữ liệu local, chưa ghi production.

## Roster và bảo toàn dữ liệu

- Danh sách lấy từ PremierHub season=2026, asOf=2026-10-03; toàn bộ roster hiện hành,
  không giới hạn25 người, không thêm ID từ EA hoặc SofaScore.
- profiles.csv và roster-status.csv có đúng48 ID/CLB, một dòng mỗi ID hiện hành.
  Giữ nguyên sáu giá trị hồ sơ/OVR từ API; không thu thập lại ô đã đủ hoặc gán0.
- Không đổi membership, nhóm rộng, thống kê trận, OVR đã có hay dữ liệu/logic2024/25.
- URL SofaScore giữ từ các batch hồ sơ02/10 bên dưới. Nếu thấy khác dữ liệu nhận dạng
  giữa nguồn, chỉ đọc lại hồ sơ để xác minh đúng người; không dùng EA sửa trường hồ sơ.
- positions.csv có48 incoming. expected_* trống vì snapshot API chưa có
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

- [Ipswich Town FC roster](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Ipswich+Town+FC), club_id=1000000349, 26 người. Nguồn hồ sơ cũ: backend/data/ipswich-town-profiles-2026-10-02/players.csv và sources.md.
- [Sunderland AFC roster](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Sunderland+AFC), club_id=1000000071, 22 người. Nguồn hồ sơ cũ: backend/data/sunderland-profiles-2026-10-02/players.csv và sources.md.

## Juan Angulo và tên viết tắt

Juan Angulo là Juan Riquelme Angulo, EA85136, Ecuador, IDV; SofaScore dùng tên đầy đủ
và chuyển sang Sunderland khớp membership. Đã đọc đúng hồ sơ EA tiếng Anh để lấy
Position ST và không có Alt; không dùng mã tiếng Tây Ban Nha DC nguyên dạng trong CSV.
Giữ DOB/SofaScoreLEFT/số áo50 hiện có dù EA khác DOB/chân thuận. Abdul Fatawu là
Abdul Fatawu Issahaku; Florentino là Florentino Ibrain Morris Luís; Julio César Enciso
là Julio Enciso; Jaden Philogene là Jaden Philogene-Bidace; Reinildo là Reinildo Mandava.
Các EA ID, quốc tịch/ngày sinh và thông tin CLB/định danh hiện có xác nhận đúng người.

## Khác biệt giữa nguồn đã ghi nhận

Các ô này đã có giá trị, không phải NULL/MISSING; giữ hồ sơ hiện có theo SofaScore.
Chỉ ghi khác biệt để review, không tự sửa theo EA. Ghép người bằng tên duy nhất,
EA ID/URL, quốc tịch/ngày sinh còn lại và CLB; không có bằng chứng ghép một người khác.
Gyabi/Slater/Mendy được mở lại đúng SofaScore: tên/CLB Hull, quốc tịch và DOB trùng
profile đã lưu. Sidiki cùng tên/DOB/CLB Coventry dù quốc tịch EA khác. Juan dùng tên
đầy đủ/Ecuador/EA85136 và SofaScore đúng người, DOB lệch1 ngày; không đổi hồ sơ.

| player_id | Tên | Trường | Giữ hiện có/SofaScore | EA ghi |
| --- | --- | --- | --- | --- |
| 2000030255 | Juan Angulo | birth_date | 2008-01-11 | 2008-01-12 |

## Độ phủ local

| CLB | Roster | Quốc tịch | DOB | Cao | Chân thuận | Số áo | OVR | Vị trí | MISSING vị trí |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Ipswich Town FC | 26 | 26 | 26 | 26 | 26 | 26 | 26 | 26 | 0 |
| Sunderland AFC | 22 | 22 | 22 | 22 | 22 | 22 | 22 | 22 | 0 |

## Nguồn từng cầu thủ

SofaScore là nguồn năm trường hồ sơ giữ từ batch cũ; EA là nguồn OVR/chính/phụ.
Alt “—” là không có vị trí phụ trên hồ sơ đã đọc; George chưa có EA là MISSING.

| CLB | player_id | Tên PremierHub | OVR giữ | SofaScore | EA FC27 | EA chính; phụ | Nhập chính; eligible | Ghi chú |
| --- | --- | --- | ---: | --- | --- | --- | --- | --- |
| Ipswich Town FC | 2000020025 | Abdul Fatawu Issahaku | 75 | [SofaScore](https://www.sofascore.com/football/player/abdul-fatawu-issahaku/1103589) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/abdul-fatawu/267680) | RM; RW | RM; RM, RW | EA dùng tên Abdul Fatawu. EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000030213 | Alex Palmer | 71 | [SofaScore](https://www.sofascore.com/football/player/alex-palmer/796143) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/alex-palmer/223909) | GK; — | GK; GK | EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000030216 | Anis Mehmeti | 72 | [SofaScore](https://www.sofascore.com/football/player/anis-mehmeti/1007072) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/anis-mehmeti/250816) | CAM; LM, CM, LW | CAM; CAM, LM, CM, LW | EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000030215 | Azor Matusiwa | 75 | [SofaScore](https://www.sofascore.com/football/player/azor-matusiwa/856495) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/azor-matusiwa/239356) | CDM; CM | CM; CM | SofaScore hiển thị Ipswich Town U21; giữ membership PremierHub. CDM → CM; gộp trùng. EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000020044 | Christian Walton | 73 | [SofaScore](https://www.sofascore.com/football/player/christian-walton/198038) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/christian-walton/206561) | GK; — | GK; GK | EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000020027 | Chuba Akpom | 71 | [SofaScore](https://www.sofascore.com/football/player/chuba-akpom/190867) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/chuba-akpom/213418) | ST; CAM | ST; ST, CAM | EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000020046 | Cédric Kipré | 74 | [SofaScore](https://www.sofascore.com/football/player/cedric-kipre/886802) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/cedric-kipre/235458) | CB; — | CB; CB | EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000020023 | Dara O'Shea | 75 | [SofaScore](https://www.sofascore.com/football/player/dara-oshea/856719) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/dara-o-shea/235405) | CB; — | CB; CB | EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000020045 | Darnell Furlong | 73 | [SofaScore](https://www.sofascore.com/football/player/darnell-furlong/786022) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/darnell-furlong/223877) | RB; RM | RB; RB, RM | EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000030214 | David Button | 62 | [SofaScore](https://www.sofascore.com/football/player/david-button/32673) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/david-button/173533) | GK; — | GK; GK | EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000011001 | Emersonn | 74 | [SofaScore](https://www.sofascore.com/football/player/emersonn/1128844) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/emersonn/76803) | ST; — | ST; ST | EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000030218 | Exequiel Palacios | 83 | [SofaScore](https://www.sofascore.com/football/player/exequiel-palacios/822600) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/exequiel-palacios/231521) | CM; CDM | CM; CM | CDM → CM; gộp trùng. EA CLB: Leverkusen; membership giữ PremierHub. |
| Ipswich Town FC | 2000030217 | Florentino Ibrain Morris Luís | 77 | [SofaScore](https://www.sofascore.com/football/player/florentino-luis/855845) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/florentino/234569) | CDM; CM | CM; CM | SofaScore ghi Florentino Luís. CDM → CM; gộp trùng. EA dùng tên Florentino. EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000020022 | Issa Diop | 75 | [SofaScore](https://www.sofascore.com/football/player/issa-diop/825719) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/issa-diop/231633) | CB; — | CB; CB | EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000011003 | Jack Clarke | 75 | [SofaScore](https://www.sofascore.com/football/player/jack-clarke/921005) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jack-clarke/242908) | LM; LW | LM; LM, LW | EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 18397 | Jack Taylor | 72 | [SofaScore](https://www.sofascore.com/football/player/jack-taylor/856669) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jack-taylor/233851) | CDM; CM, CAM | CM; CM, CAM | CDM → CM; gộp trùng. EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000020021 | Jacob Greaves | 74 | [SofaScore](https://www.sofascore.com/football/player/jacob-greaves/990821) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jacob-greaves/248602) | CB; LB | CB; CB, LB | EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 138931 | Jaden Philogene-Bidace | 76 | [SofaScore](https://www.sofascore.com/football/player/jaden-philogene-bidace/1014461) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jaden-philogene/261336) | LM; RM, LW, RW | LM; LM, RM, LW, RW | EA dùng tên Jaden Philogene. EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000011002 | Julio Enciso | 78 | [SofaScore](https://www.sofascore.com/football/player/julio-enciso/973556) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/julio-cesar-enciso/255434) | CAM; ST | CAM; CAM, ST | EA dùng tên Julio César Enciso. EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000020026 | Kasey McAteer | 71 | [SofaScore](https://www.sofascore.com/football/player/kasey-mcateer/1065134) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kasey-mc-ateer/265801) | RM; RW, CAM, CM | RM; RM, RW, CAM, CM | EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000020019 | Kjell Scherpen | 76 | [SofaScore](https://www.sofascore.com/football/player/kjell-scherpen/852394) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kjell-scherpen/243675) | GK; — | GK; GK | EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000020020 | Leif Davis | 75 | [SofaScore](https://www.sofascore.com/football/player/leif-davis/963493) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/leif-davis/246685) | LB; LM | LB; LB, LM | EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000011005 | Marcelino Núñez | 73 | [SofaScore](https://www.sofascore.com/football/player/marcelino-nunez/1014801) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/marcelino-nunez/256051) | CAM; CDM, CM | CAM; CAM, CM | CDM → CM; gộp trùng. EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000011004 | Saša Lukić | 78 | [SofaScore](https://www.sofascore.com/football/player/sasa-lukic/371222) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/sasa-lukic/236699) | CDM; CM | CM; CM | CDM → CM; gộp trùng. EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000020029 | Sindre Walle Egeli | 70 | [SofaScore](https://www.sofascore.com/football/player/sindre-walle-egeli/1154598) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/sindre-walle-egeli/71110) | RM; RW | RM; RM, RW | EA CLB: Ipswich; membership giữ PremierHub. |
| Ipswich Town FC | 2000030219 | Zian Flemming | 76 | [SofaScore](https://www.sofascore.com/football/player/zian-flemming/875137) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/zian-flemming/239373) | ST; — | ST; ST | EA CLB: Burnley; membership giữ PremierHub. |
| Sunderland AFC | 2000020038 | Brian Brobbey | 78 | [SofaScore](https://www.sofascore.com/football/player/brian-brobbey/910048) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/brian-brobbey/251810) | ST; — | ST; ST | EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000020039 | Chemsdine Talbi | 77 | [SofaScore](https://www.sofascore.com/football/player/chemsdine-talbi/1142675) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/chemsdine-talbi/275048) | RM; LM, RW, LW | RM; RM, LM, RW, LW | EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000020042 | Chris Rigg | 74 | [SofaScore](https://www.sofascore.com/football/player/chris-rigg/1394089) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/chris-rigg/273459) | CAM; RM, CM | CAM; CAM, RM, CM | EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000020032 | Daniel Ballard | 78 | [SofaScore](https://www.sofascore.com/football/player/daniel-ballard/958878) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/daniel-ballard/243908) | CB; — | CB; CB | EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000030252 | Dayann Méthalie | 75 | [SofaScore](https://www.sofascore.com/football/player/dayann-methalie/1894003) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/dayann-methalie/74491) | LB; LM | LB; LB, LM | EA dùng tên Dayann Methalie. EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000020037 | Enzo Le Fée | 80 | [SofaScore](https://www.sofascore.com/football/player/enzo-le-fee/984014) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/enzo-le-fee/246350) | CAM; LM, CM, LW | CAM; CAM, LM, CM, LW | EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000012002 | Granit Xhaka | 85 | [SofaScore](https://www.sofascore.com/football/player/granit-xhaka/117777) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/granit-xhaka/199503) | CDM; CM | CM; CM | CDM → CM; gộp trùng. EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000020043 | Habib Diarra | 77 | [SofaScore](https://www.sofascore.com/football/player/habib-diarra/1128532) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/habib-diarra/264293) | CM; CAM | CM; CM, CAM | EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000020050 | Jocelin Ta Bi | 69 | [SofaScore](https://www.sofascore.com/football/player/jocelin-ta-bi/2138729) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jocelin-ta-bi/83236) | RM; RW | RM; RM, RW | EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000030255 | Juan Angulo | 62 | [SofaScore](https://www.sofascore.com/football/player/juan-angulo/2057385) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/juan-riquelme-angulo/85136) | ST; — | ST; ST | SofaScore ghi Juan Riquelme Angulo. EA còn ghi IDV và chân phải; số áo 50 và chân LEFT lấy từ SofaScore. Khác nguồn birth_date: giữ2008-01-11; EA2008-01-12. EA dùng tên Juan Riquelme Angulo. EA CLB: IDV; membership giữ PremierHub. |
| Sunderland AFC | 2000030253 | Jules Ahoka | 61 | [SofaScore](https://www.sofascore.com/football/player/jules-ahoka/2137673) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jules-ahoka/85425) | CDM; CM | CM; CM | CDM → CM; gộp trùng. EA CLB: Royal Antwerp FC; membership giữ PremierHub. |
| Sunderland AFC | 2000030103 | Kevin Danso | 79 | [SofaScore](https://www.sofascore.com/football/player/kevin-danso/794953) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kevin-danso/237985) | CB; — | CB; CB | EA CLB: Spurs; membership giữ PremierHub. |
| Sunderland AFC | 2000030254 | Malick Fofana | 77 | [SofaScore](https://www.sofascore.com/football/player/malick-fofana/1195784) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/malick-fofana/256420) | LM; LW | LM; LM, LW | EA CLB: OL; membership giữ PremierHub. |
| Sunderland AFC | 2000020048 | Melker Ellborg | 68 | [SofaScore](https://www.sofascore.com/football/player/melker-ellborg/1099795) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/melker-ellborg/259987) | GK; — | GK; GK | EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000012001 | Nilson Angulo | 73 | [SofaScore](https://www.sofascore.com/football/player/nilson-angulo/1116571) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/nilson-angulo/264205) | LM; RM, LW, RW | LM; LM, RM, LW, RW | EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000020036 | Noah Sadiki | 80 | [SofaScore](https://www.sofascore.com/football/player/noah-sadiki/1171539) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/noah-sadiki/269230) | CDM; CM, CAM | CM; CM, CAM | CDM → CM; gộp trùng. EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000020049 | Nordi Mukiele | 81 | [SofaScore](https://www.sofascore.com/football/player/nordi-mukiele/780014) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/nordi-mukiele/226166) | RB; CB, CDM | RB; RB, CB, CM | CDM → CM; gộp trùng. EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000020040 | Omar Alderete | 81 | [SofaScore](https://www.sofascore.com/football/player/omar-alderete/805137) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/omar-alderete/240359) | CB; — | CB; CB | EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000020034 | Reinildo Mandava | 80 | [SofaScore](https://www.sofascore.com/football/player/reinildo-mandava/831424) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/reinildo/236045) | LB; — | LB; LB | EA dùng tên Reinildo. EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000020030 | Robin Roefs | 81 | [SofaScore](https://www.sofascore.com/football/player/robin-roefs/1012928) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/robin-roefs/263798) | GK; — | GK; GK | EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000020031 | Thomas Meunier | 78 | [SofaScore](https://www.sofascore.com/football/player/thomas-meunier/128587) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/thomas-meunier/202371) | RB; RM | RB; RB, RM | EA CLB: Sunderland; membership giữ PremierHub. |
| Sunderland AFC | 2000020041 | Wilson Isidor | 75 | [SofaScore](https://www.sofascore.com/football/player/wilson-isidor/877980) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/wilson-isidor/247335) | ST; — | ST; ST | EA CLB: Sunderland; membership giữ PremierHub. |

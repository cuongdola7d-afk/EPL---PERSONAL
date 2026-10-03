# Coventry City FC – Nottingham Forest FC: hồ sơ, OVR và vị trí 2026/27

Ngày chuẩn bị: 2026-10-03. Chỉ dữ liệu local, chưa ghi production.

## Roster và bảo toàn dữ liệu

- Danh sách lấy từ PremierHub season=2026, asOf=2026-10-03; toàn bộ roster hiện hành,
  không giới hạn25 người, không thêm ID từ EA hoặc SofaScore.
- profiles.csv và roster-status.csv có đúng54 ID/CLB, một dòng mỗi ID hiện hành.
  Giữ nguyên sáu giá trị hồ sơ/OVR từ API; không thu thập lại ô đã đủ hoặc gán0.
- Không đổi membership, nhóm rộng, thống kê trận, OVR đã có hay dữ liệu/logic2024/25.
- URL SofaScore giữ từ các batch hồ sơ02/10 bên dưới. Nếu thấy khác dữ liệu nhận dạng
  giữa nguồn, chỉ đọc lại hồ sơ để xác minh đúng người; không dùng EA sửa trường hồ sơ.
- positions.csv có53 incoming. expected_* trống vì snapshot API chưa có
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

- [Coventry City FC roster](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Coventry+City+FC), club_id=1000001076, 30 người. Nguồn hồ sơ cũ: backend/data/coventry-city-profiles-2026-10-02/players.csv và sources.md.
- [Nottingham Forest FC roster](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Nottingham+Forest+FC), club_id=1000000351, 24 người. Nguồn hồ sơ cũ: backend/data/nottingham-forest-profiles-2026-10-02/players.csv và sources.md.

## George Shepherd: dữ liệu còn thiếu

- Coventry City, player_id2000030180, club_id1000001076: thiếu height_cm,
  preferred_foot, shirt_number, fc27_overall, primary_position, eligible_positions.
- Đã mở đúng SofaScore https://www.sofascore.com/football/player/george-shepherd/2318050
  khớp tên, England, DOB2008-12-09 và Coventry CityU21. General info không ghi
  chiều cao/chân thuận/số áo. Số27 trên trang là followers, không dùng làm số áo.
- Tìm tên đầy đủ/Shepherd trên EA cùng FC27/player-ratings chưa tìm được hồ sơ đúng
  người. Các kết quả George/George King/George Shores/Corey Shephard không khớp
  tên, DOB, CLB nên không ghép. Không suy CM từ MIDFIELDER hoặc tự cấp OVR.
- Giữ SQL NULL cho ô hồ sơ/OVR thiếu và trạng thái vị trí MISSING. Vẫn có dòng hồ sơ
  và roster-status; không có dòng positions incoming trống vì reader yêu cầu hợp lệ.
  George chưa đủ điều kiện chọn Fantasy; không dừng batch vì người này.

## Omar Richards: nguồn EA truy cập được

URL tiếng Anh/Tây Ban Nha hiện trả404 khi tải trực tiếp. Đã mở được đúng EA FC27
Arabic https://www.ea.com/ar/games/ea-sports-fc/ratings/player-ratings/omar-richards/235026
bằng công cụ web: Position LB; Alt Positions LM,LW; OVR71; tuổi28; England;
Nott'm Forest. EA ID235026/tên/CLB/tuổi khớp; không tự dịch LW thành một quyền khác.
Không xác minh DOB đầy đủ từ trang truy cập này; giữ DOB SofaScore đã có.

## Khác biệt giữa nguồn đã ghi nhận

Các ô này đã có giá trị, không phải NULL/MISSING; giữ hồ sơ hiện có theo SofaScore.
Chỉ ghi khác biệt để review, không tự sửa theo EA. Ghép người bằng tên duy nhất,
EA ID/URL, quốc tịch/ngày sinh còn lại và CLB; không có bằng chứng ghép một người khác.
Gyabi/Slater/Mendy được mở lại đúng SofaScore: tên/CLB Hull, quốc tịch và DOB trùng
profile đã lưu. Sidiki cùng tên/DOB/CLB Coventry dù quốc tịch EA khác. Juan dùng tên
đầy đủ/Ecuador/EA85136 và SofaScore đúng người, DOB lệch1 ngày; không đổi hồ sơ.

| player_id | Tên | Trường | Giữ hiện có/SofaScore | EA ghi |
| --- | --- | --- | --- | --- |
| 2000002029 | Sidiki Cherif | nationality | Guinea | France |

## Độ phủ local

| CLB | Roster | Quốc tịch | DOB | Cao | Chân thuận | Số áo | OVR | Vị trí | MISSING vị trí |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Coventry City FC | 30 | 30 | 30 | 29 | 29 | 29 | 29 | 29 | 1 |
| Nottingham Forest FC | 24 | 24 | 24 | 24 | 24 | 24 | 24 | 24 | 0 |

## Nguồn từng cầu thủ

SofaScore là nguồn năm trường hồ sơ giữ từ batch cũ; EA là nguồn OVR/chính/phụ.
Alt “—” là không có vị trí phụ trên hồ sơ đã đọc; George chưa có EA là MISSING.

| CLB | player_id | Tên PremierHub | OVR giữ | SofaScore | EA FC27 | EA chính; phụ | Nhập chính; eligible | Ghi chú |
| --- | --- | --- | ---: | --- | --- | --- | --- | --- |
| Coventry City FC | 2000002021 | Aurele Amenda | 74 | [SofaScore](https://www.sofascore.com/football/player/aurele-amenda/999276) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/aurele-amenda/270846) | CB; RB | CB; CB, RB | EA dùng tên Aurèle Amenda. EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002012 | Ben Wilson | 68 | [SofaScore](https://www.sofascore.com/football/player/ben-wilson/216590) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ben-wilson/204825) | GK; — | GK; GK | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002003 | Bobby Thomas | 74 | [SofaScore](https://www.sofascore.com/football/player/bobby-thomas/1033352) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bobby-thomas/257253) | CB; — | CB; CB | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002020 | Brandon Thomas-Asante | 73 | [SofaScore](https://www.sofascore.com/football/player/brandon-thomas-asante/846061) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/brandon-thomas-asante/235744) | ST; CAM, RM, RW | ST; ST, CAM, RM, RW | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002007 | Caleb Yirenkyi | 73 | [SofaScore](https://www.sofascore.com/football/player/caleb-yirenkyi/1986794) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/caleb-yirenkyi/73638) | CM; CB, CDM | CM; CM, CB | CDM → CM; gộp trùng. EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002016 | Carl Rushworth | 76 | [SofaScore](https://www.sofascore.com/football/player/carl-rushworth/1005800) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/carl-rushworth/263339) | GK; — | GK; GK | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002022 | Dan Bentley | 70 | [SofaScore](https://www.sofascore.com/football/player/daniel-bentley/101367) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/daniel-bentley/198350) | GK; — | GK; GK | SofaScore ghi Daniel Bentley. EA dùng tên Daniel Bentley. EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002008 | Ellis Simms | 71 | [SofaScore](https://www.sofascore.com/football/player/ellis-simms/991614) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ellis-simms/251198) | ST; — | ST; ST | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002009 | Ephron Mason-Clark | 75 | [SofaScore](https://www.sofascore.com/football/player/ephron-mason-clark/861315) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ephron-mason-clark/236784) | LM; LW | LM; LM, LW | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002001 | Ethan Pinnock | 75 | [SofaScore](https://www.sofascore.com/football/player/ethan-pinnock/855864) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ethan-pinnock/238717) | CB; — | CB; CB | EA CLB: Brentford; membership giữ PremierHub. |
| Coventry City FC | 2000002014 | Frank Onyeka | 75 | [SofaScore](https://www.sofascore.com/football/player/frank-onyeka/885864) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/frank-onyeka/239529) | CDM; CM | CM; CM | CDM → CM; gộp trùng. EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000030180 | George Shepherd | NULL | [SofaScore](https://www.sofascore.com/football/player/george-shepherd/2318050) | Chưa xác minh | MISSING | MISSING | SofaScore chưa ghi height/foot/shirt, EA FC27 chưa xác minh; giữ NULL/MISSING. |
| Coventry City FC | 2000002028 | Gustavo Hamer | 75 | [SofaScore](https://www.sofascore.com/football/player/gustavo-hamer/837495) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/gustavo-hamer/234741) | LM; CAM, LW, CM | LM; LM, CAM, LW, CM | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000030181 | Haji Wright | 74 | [SofaScore](https://www.sofascore.com/football/player/haji-wright/818391) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/haji-wright/238743) | ST; LM, LW | ST; ST, LM, LW | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002004 | Jack Rudoni | 75 | [SofaScore](https://www.sofascore.com/football/player/jack-rudoni/979565) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jack-rudoni/251690) | CAM; CM, CDM | CAM; CAM, CM | CDM → CM; gộp trùng. EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002018 | Jake Bidwell | 68 | [SofaScore](https://www.sofascore.com/football/player/jake-bidwell/103115) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jake-bidwell/196952) | LB; LM | LB; LB, LM | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002002 | Jay Dasilva | 73 | [SofaScore](https://www.sofascore.com/football/player/jay-dasilva/791985) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jay-dasilva/232755) | LB; LM | LB; LB, LM | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002019 | Joel Latibeaudiere | 71 | [SofaScore](https://www.sofascore.com/football/player/joel-latibeaudiere/859763) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/joel-latibeaudiere/233047) | CB; RB | CB; CB, RB | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000020003 | Josh Eccles | 71 | [SofaScore](https://www.sofascore.com/football/player/josh-eccles/944245) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/josh-eccles/251555) | CDM; CM, CAM | CM; CM, CAM | CDM → CM; gộp trùng. EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000030179 | Kaine Kesler-Hayden | 72 | [SofaScore](https://www.sofascore.com/football/player/kaine-hayden/1099074) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kaine-kesler-hayden/261335) | RB; RM, LB, RW | RB; RB, RM, LB, RW | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002015 | Loum Tchaouna | 73 | [SofaScore](https://www.sofascore.com/football/player/loum-tchaouna/1004505) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/loum-tchaouna/264880) | RM; RW, ST | RM; RM, RW, ST | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 17714 | Luke Woolfenden | 72 | [SofaScore](https://www.sofascore.com/football/player/luke-woolfenden/893478) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/luke-woolfenden/240500) | CB; — | CB; CB | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002005 | Matt Grimes | 76 | [SofaScore](https://www.sofascore.com/football/player/matt-grimes/340713) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/matt-grimes/212118) | CDM; CM | CM; CM | CDM → CM; gộp trùng. EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002024 | Milan van Ewijk | 75 | [SofaScore](https://www.sofascore.com/football/player/milan-van-ewijk/948723) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/milan-van-ewijk/251626) | RB; RM | RB; RB, RM | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002029 | Sidiki Cherif | 72 | [SofaScore](https://www.sofascore.com/football/player/sidiki-cherif/1514916) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/sidiki-cherif/277485) | ST; — | ST; ST | Khác nguồn nationality: giữGuinea; EAFrance. EA dùng tên Sidiki Chérif. EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002011 | Stephen Mfuni | 69 | [SofaScore](https://www.sofascore.com/football/player/stephen-mfuni/1402791) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/stephen-mfuni/81538) | CB; LB | CB; CB, LB | EA CLB: Manchester City; membership giữ PremierHub. |
| Coventry City FC | 2000002013 | Taiwo Awoniyi | 74 | [SofaScore](https://www.sofascore.com/football/player/taiwo-awoniyi/359664) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/taiwo-awoniyi/230978) | ST; — | ST; ST | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002006 | Tatsuhiro Sakamoto | 74 | [SofaScore](https://www.sofascore.com/football/player/tatsuhiro-sakamoto/978927) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/tatsuhiro-sakamoto/254910) | RM; RW | RM; RM, RW | EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002026 | Victor Torp | 73 | [SofaScore](https://www.sofascore.com/football/player/victor-torp/837011) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/victor-torp/241645) | CDM; CM, CAM | CM; CM, CAM | CDM → CM; gộp trùng. EA CLB: Coventry City; membership giữ PremierHub. |
| Coventry City FC | 2000002027 | Yann Gboho | 77 | [SofaScore](https://www.sofascore.com/football/player/yann-gboho/911851) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/yann-gboho/252937) | LW; CAM, LM, ST | LW; LW, CAM, LM, ST | EA CLB: Toulouse FC; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020064 | Arnaud Kalimuendo | 78 | [SofaScore](https://www.sofascore.com/football/player/arnaud-kalimuendo/954061) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/arnaud-kalimuendo/253444) | ST; LM, RM, CAM | ST; ST, LM, RM, CAM | EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020077 | Callum Hudson-Odoi | 78 | [SofaScore](https://www.sofascore.com/football/player/callum-hudson-odoi/867442) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/callum-hudson-odoi/240740) | LM; LW | LM; LM, LW | EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020061 | Chris Wood | 81 | [SofaScore](https://www.sofascore.com/football/player/chris-wood/50480) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/chris-wood/192123) | ST; — | ST; ST | EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020058 | Dan Ndoye | 77 | [SofaScore](https://www.sofascore.com/football/player/dan-ndoye/944327) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/dan-ndoye/257980) | LM; RM, LW, RW | LM; LM, RM, LW, RW | EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020095 | Daniel Muñoz | 82 | [SofaScore](https://www.sofascore.com/football/player/daniel-munoz/870360) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/daniel-munoz/237646) | RB; RM | RB; RB, RM | EA còn ghi Crystal Palace; giữ membership Nottingham Forest. EA CLB: Crystal Palace; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020054 | Ibrahim Sangaré | 78 | [SofaScore](https://www.sofascore.com/football/player/ibrahim-sangare/843754) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ibrahim-sangare/235173) | CDM; CM | CM; CM | CDM → CM; gộp trùng. EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020056 | Igor Jesus | 77 | [SofaScore](https://www.sofascore.com/football/player/igor-jesus/981619) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/igor-jesus/79402) | ST; — | ST; ST | EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020059 | Jair | 75 | [SofaScore](https://www.sofascore.com/football/player/jair-paula/1170722) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jair-cunha/79629) | CB; — | CB; CB | Hồ sơ SofaScore có URL jair-paula, CLB và ngày sinh khớp. EA dùng tên Jair Cunha. EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000013001 | James McAtee | 73 | [SofaScore](https://www.sofascore.com/football/player/james-mcatee/1003334) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/james-mc-atee/264349) | RM; CAM, RW, CM | RM; RM, CAM, RW, CM | EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020073 | John Victor | 71 | [SofaScore](https://www.sofascore.com/football/player/john-victor/840103) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/john-victor/234724) | GK; — | GK; GK | EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 161948 | Liam Delap | 77 | [SofaScore](https://www.sofascore.com/football/player/liam-delap/997087) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/liam-delap/259031) | ST; — | ST; ST | EA CLB: Chelsea; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020062 | Luca Netz | 73 | [SofaScore](https://www.sofascore.com/football/player/netz-luca/979144) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/luca-netz/256781) | LB; LM, LW | LB; LB, LM, LW | EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020052 | Matz Sels | 81 | [SofaScore](https://www.sofascore.com/football/player/matz-sels/78152) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/matz-sels/199641) | GK; — | GK; GK | EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020055 | Morgan Gibbs-White | 83 | [SofaScore](https://www.sofascore.com/football/player/morgan-gibbs-white/865912) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/morgan-gibbs-white/236015) | CAM; CM | CAM; CAM, CM | EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000013002 | Murillo | 82 | [SofaScore](https://www.sofascore.com/football/player/murillo/1199282) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/murillo/278016) | CB; — | CB; CB | EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020053 | Neco Williams | 80 | [SofaScore](https://www.sofascore.com/football/player/neco-williams/927356) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/neco-williams/243057) | LB; RB | LB; LB, RB | EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020076 | Nicolás Domínguez | 78 | [SofaScore](https://www.sofascore.com/football/player/nicolas-dominguez/871765) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/nicolas-dominguez/237819) | CDM; CM, LM, LW | CM; CM, LM, LW | CDM → CM; gộp trùng. EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000030244 | Nicolò Savona | 75 | [SofaScore](https://www.sofascore.com/football/player/nicolo-savona/1010220) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/nicolo-savona/74310) | RB; RM | RB; RB, RM | EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020057 | Nikola Milenković | 80 | [SofaScore](https://www.sofascore.com/football/player/nikola-milenkovic/836168) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/nikola-milenkovic/238095) | CB; — | CB; CB | EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000030245 | Omar Richards | 71 | [SofaScore](https://www.sofascore.com/football/player/omar-richards/892483) | [EA FC27](https://www.ea.com/ar/games/ea-sports-fc/ratings/player-ratings/omar-richards/235026) | LB; LM, LW | LB; LB, LM, LW | SofaScore hiển thị Aris Thessaloniki; giữ membership PremierHub. EA FC 27 ghi Nottingham Forest, SofaScore hiện Aris; giữ membership PremierHub. Đọc EA Arabic qua web; LB,AltLM/LW,OVR71,age28; DOB giữ nguồn cũ. EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020074 | Ousmane Diomande | 81 | [SofaScore](https://www.sofascore.com/football/player/ousmane-diomande/1394342) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ousmane-diomande/270531) | CB; — | CB; CB | EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 19305 | Ryan Yates | 75 | [SofaScore](https://www.sofascore.com/football/player/ryan-yates/846252) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ryan-yates/235642) | CDM; CM | CM; CM | CDM → CM; gộp trùng. EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 19312 | Steven Benda | 68 | [SofaScore](https://www.sofascore.com/football/player/steven-benda/860764) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/steven-benda/241076) | GK; — | GK; GK | EA CLB: Nott'm Forest; membership giữ PremierHub. |
| Nottingham Forest FC | 2000020063 | Xaver Schlager | 78 | [SofaScore](https://www.sofascore.com/football/player/xaver-schlager/791079) | [EA FC27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/xaver-schlager/233195) | CDM; CM | CM; CM | CDM → CM; gộp trùng. EA CLB: Nott'm Forest; membership giữ PremierHub. |

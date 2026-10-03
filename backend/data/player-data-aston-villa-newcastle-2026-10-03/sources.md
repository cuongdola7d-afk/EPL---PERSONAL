# Aston Villa – Newcastle: hồ sơ và vị trí 2026/27

Ngày chuẩn bị: 2026-10-03. Đây là batch local, chưa ghi MySQL production.

## Roster và nguyên tắc

- ID/CLB lấy từ API PremierHub `season=2026&asOf=2026-10-03`, với đúng tên CLB.
  Có 27 Aston Villa và 28 Newcastle; không giới hạn 25 người. Cùng 55 cặp ID/CLB
  với hai CSV hồ sơ ngày 02/10; không thêm cầu thủ từ danh sách EA hoặc SofaScore.
- Hồ sơ ban đầu lấy sáu trường đang có từ API; trường đã đủ giữ nguyên nguồn cũ.
  Đã mở lại SofaScore cho Kyran Thompson, Mason Miley và Michael Mills, nguồn
  không ghi các trường còn thiếu. Sau đó người dùng cung cấp dữ liệu ngày 03/10
  như bảng bổ sung dưới đây. Các giá trị mới không được ghi là SofaScore xác minh.
- profiles.csv là bản cuối 55 người sau bổ sung: đủ sáu trường. OVR James Wright
  61, Leon Goretzka 78, Miodrag Pivas 62 giữ nguyên nguồn người dùng 02/10.
  Bốn OVR 60 mới là mức người dùng đặt, lưu trực tiếp fc27_overall theo yêu cầu;
  không phải OVR EA FC 27 đã xác minh. Không dùng bảng ước tính riêng.
- Importer hồ sơ hiện tại chỉ chèn và báo xung đột khi dữ liệu đã lưu khác CSV.
  profile-updates.csv ghi riêng đúng tám ô NULL được bổ sung, định dạng
  player_id,club_id,field,value giống bản bổ sung 02/10. File này là bản kê thay
  đổi để cập nhật có điều kiện khi được phép ghi production, không phải đầu vào
  PlayerProfileCsvReader. Không chạy profiles.csv cuối vào production còn chứa
  NULL để mong importer tự ghi đè; phải áp dụng tám ô đã review trước. Hiện chỉ local.
- Với 48 người đã có nguồn EA, vị trí lấy từ Position và Alt Positions trên EA SPORTS FC 27
  dưới đây, đọc ngày 03/10. Trang ghi dữ liệu Gold/Silver/Bronze lúc phát hành;
  không lấy thẻ sự kiện hoặc FC 26. Cả 48 OVR đọc được đều khớp hồ sơ hiện có.
- Ghép theo URL/EA ID đã lưu, tên/biến thể tên, quốc tịch, tuổi và dấu hiệu CLB.
  DR Congo/Congo DR, Netherlands/Holland, Czechia/Czech Republic và Ivory Coast/
  Côte d'Ivoire là tên quốc gia tương ứng. Tuổi Tammy Abraham trên EA là 28,
  ngày sinh SofaScore hiện có 1997-10-02 (29 tuổi tại 03/10): cùng tên, quốc tịch,
  CLB và EA ID 231352; không sửa ngày sinh theo tuổi EA chưa cập nhật.
- Nico González dùng hồ sơ Tây Ban Nha, EA ID 255069, tuổi 24/ngày sinh 2002-01-03;
  không ghép người Argentina EA ID 240690. Valentino Livramento = Tino Livramento.
  Nhãn CLB EA khác PremierHub không thay membership.
- Với dữ liệu EA, chỉ ánh xạ CDM → CM cho vị trí chính và phụ, gộp mã trùng sau ánh xạ. Không suy
  quyền chơi từ nhóm rộng; không thêm vị trí mà Alt Positions không ghi.
- positions.csv cuối có đủ 55 người: 48 nguồn EA và bảy nguồn người dùng.
  Vị trí đầu tiên người dùng ghi là primary, eligible đúng tập họ cung cấp;
  không thêm vị trí khác. expected_* giữ trống theo snapshot API chưa có vị trí.
  roster-status.csv đủ 55 người, 48 VERIFIED và bảy USER_PROVIDED, không còn MISSING.

## Bổ sung từ người dùng ngày 03/10/2026

Nguồn là tin nhắn trong cuộc trao đổi này; chưa xác minh thêm EA/SofaScore.

| player_id | Cầu thủ | Primary | Eligible | Hồ sơ/OVR bổ sung |
| --- | --- | --- | --- | --- |
| 2000030151 | James Wright | GK | GK | Giữ OVR 61 |
| 2000030149 | Leon Goretzka | CM | CM | Giữ OVR 78 |
| 2000030240 | Miodrag Pivas | CB | CB | Giữ OVR 62 |
| 2000030243 | Kyran Thompson | RW | RW, RM | height_cm=183; fc27_overall=60 |
| 2000030239 | Mason Miley | CM | CM, RB | height_cm=181; fc27_overall=60 |
| 2000030242 | Michael Mills | ST | ST | height_cm=179; preferred_foot=LEFT; fc27_overall=60 |
| 2000030241 | Vakhtang Salia | ST | ST | fc27_overall=60 |

## URL roster và hồ sơ tái sử dụng

- [Aston Villa FC roster](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Aston+Villa+FC); `club_id=1000000058`; nguồn cũ: `backend/data/aston-villa-profiles-2026-10-02/players.csv` và `sources.md`.
- [Newcastle United FC roster](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Newcastle+United+FC); `club_id=1000000067`; nguồn cũ: `backend/data/newcastle-united-profiles-2026-10-02/players.csv` và `sources.md`.

## Độ phủ

| CLB | Roster/hồ sơ | Quốc tịch | Ngày sinh | Chiều cao | Chân thuận | Số áo | OVR số | OVR EA xác minh | Vị trí có nguồn | MISSING vị trí |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Aston Villa | 27 | 27 | 27 | 27 | 27 | 27 | 27 | 25 | 27 | 0 |
| Newcastle | 28 | 28 | 28 | 28 | 28 | 28 | 28 | 23 | 28 | 0 |
| Tổng | 55 | 55 | 55 | 55 | 55 | 55 | 55 | 48 | 55 | 0 |

## Trạng thái thiếu dữ liệu và nguồn

Không còn trường dữ liệu MISSING trong batch local sau bổ sung của người dùng.
EA FC 27 vẫn chưa có URL hồ sơ xác minh cho bảy người trong bảng bổ sung;
quyền vị trí mới của họ được ghi rõ nguồn người dùng, không giả định nguồn EA.
Ba chiều cao, chân thuận Michael và bốn OVR 60 là người dùng bổ sung.
Đây là thiếu xác minh nguồn chính thức, không phải các ô dữ liệu còn NULL.

Đã tìm trên miền EA bằng tên James Wright, Leon Goretzka, Kyran Thompson,
Mason Miley, Michael Mills, Miodrag Pivas và Vakhtang Salia, kèm FC 27/ratings và
player-ratings. Không tìm được hồ sơ FC 27 đủ căn cứ cho bảy người. URL Goretzka
`https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/leon-goretzka/209658`
được mở lại và trả 404; kết quả ngôn ngữ khác ghi FC 26 bị loại. Không lấy vị trí
chính/phụ hoặc OVR từ FC 26, diễn đàn hay trang bên thứ ba.

## Nguồn theo cầu thủ

SofaScore là URL hồ sơ gốc đã dùng cho năm trường. Chỉ ba người có ô hồ sơ còn
thiếu được đọc lại. EA ghi Position; Alt Positions nguyên bản để kiểm tra ánh xạ.
`—` ở Alt là không có vị trí phụ trên trang. Ghi chú nguồn cũ được giữ để nhận dạng;
ba OVR người dùng 02/10 đã có trên API; bốn OVR 60 và vị trí mới từ người dùng 03/10 chỉ có local.

| CLB | player_id | Tên PremierHub | OVR bản cuối local | Nguồn OVR | SofaScore | EA FC 27 | EA chính; phụ | Nhập chính; tập | Ghi chú |
| --- | --- | --- | ---: | --- | --- | --- | --- | --- | --- |
| Aston Villa | 18846 | Aaron Wan-Bissaka | 78 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/aaron-wan-bissaka/863653) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/aaron-wan-bissaka/229880) | RB; RM | RB; RB, RM |  |
| Aston Villa | 162714 | Amadou Onana | 81 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/amadou-onana/923973) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/amadou-onana/257057) | CDM; CM | CM; CM | CDM → CM, gộp mã trùng. |
| Aston Villa | 2000030145 | Andrés García | 73 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/andres-garcia/1457536) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/andres-garcia/275468) | RB; RM | RB; RB, RM | EA ghi Getafe CF; giữ CLB PremierHub. |
| Aston Villa | 2000030122 | Boubacar Kamara | 84 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/boubacar-kamara/826204) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/boubacar-kamara/236987) | CDM; CM | CM; CM | CDM → CM, gộp mã trùng. |
| Aston Villa | 2000030126 | Bradley Burrowes | 65 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/bradley-burrowes/1899712) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bradley-burrowes/80816) | RM; ST, RW | RM; RM, ST, RW |  |
| Aston Villa | 2000030148 | Brian Madjo | 65 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/brian-madjo/2070311) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/brian-madjo/80652) | ST; — | ST; ST |  |
| Aston Villa | 2000030061 | Emiliano Buendía | 80 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/emiliano-buendia/783126) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/emiliano-buendia/226162) | LM; CAM, LW, CM | LM; LM, CAM, LW, CM |  |
| Aston Villa | 2000030123 | George Hemmings | 65 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/george-hemmings/1398204) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/george-hemmings/82350) | CDM; CM, CAM | CM; CM, CAM | CDM → CM, gộp mã trùng. |
| Aston Villa | 2000030064 | Ian Maatsen | 79 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/ian-maatsen/976263) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ian-maatsen/248465) | LB; LM | LB; LB, LM |  |
| Aston Villa | 2000030150 | Ibrahim Mbaye | 76 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/ibrahim-mbaye/1590918) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ibrahim-mbaye/74449) | RW; LW, RM, LM | RW; RW, LW, RM, LM | EA ghi Paris SG; giữ CLB PremierHub. |
| Aston Villa | 2000030151 | James Wright | 61 | USER_PROVIDED_2026-10-02 | [SofaScore](https://www.sofascore.com/football/player/wright-james/1138445) | — | — | GK; GK | Vị trí do người dùng cung cấp 03/10/2026; chưa xác minh EA FC 27. Giữ OVR 61 nguồn người dùng 02/10. |
| Aston Villa | 2000030146 | Johan Manzambi | 80 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/johan-manzambi/1518931) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/johan-manzambi/276694) | CM; CDM, CAM | CM; CM, CAM | CDM → CM, gộp mã trùng. |
| Aston Villa | 2000030059 | John McGinn | 83 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/john-mcginn/250223) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/john-mc-ginn/210881) | RM; LM, CAM, RW | RM; RM, LM, CAM, RW |  |
| Aston Villa | 2000030057 | João Gomes | 78 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/joao-gomes/1015267) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/joao-gomes/273463) | CDM; CM | CM; CM | CDM → CM, gộp mã trùng. |
| Aston Villa | 2000030067 | Lamare Bogarde | 74 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/lamare-bogarde/1089388) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/lamare-bogarde/264209) | CDM; CM, RB | CM; CM, RB | CDM → CM, gộp mã trùng. |
| Aston Villa | 2000030149 | Leon Goretzka | 78 | USER_PROVIDED_2026-10-02 | [SofaScore](https://www.sofascore.com/football/player/leon-goretzka/184661) | — | — | CM; CM | Vị trí do người dùng cung cấp 03/10/2026; chưa xác minh EA FC 27. Giữ OVR 78 nguồn người dùng 02/10. |
| Aston Villa | 2000030069 | Marco Bizot | 77 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/marco-bizot/100390) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/marco-bizot/200110) | GK; — | GK; GK |  |
| Aston Villa | 2000030063 | Matteo Ruggeri | 78 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/matteo-ruggeri/965011) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/matteo-ruggeri/259584) | LB; LM | LB; LB, LM |  |
| Aston Villa | 2000030058 | Matty Cash | 81 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/matty-cash/833956) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/matty-cash/227174) | RB; RM | RB; RB, RM |  |
| Aston Villa | 283058 | Nicolas Jackson | 79 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/nicolas-jackson/1085381) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/nicolas-jackson/259197) | ST; — | ST; ST | EA ghi Chelsea; giữ CLB PremierHub. |
| Aston Villa | 2000030066 | Pau Torres | 81 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/pau-torres/864169) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/pau-torres/241464) | CB; — | CB; CB |  |
| Aston Villa | 2000030068 | Ross Barkley | 77 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/ross-barkley/98435) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ross-barkley/199189) | CM; CAM, CDM | CM; CM, CAM | CDM → CM, gộp mã trùng. |
| Aston Villa | 2000030147 | Tammy Abraham | 77 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/tammy-abraham/610766) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/tammy-abraham/231352) | ST; — | ST; ST | Tuổi EA trước sinh nhật; giữ DOB SofaScore đã lưu. |
| Aston Villa | 144729 | Taylor Harwood-Bellis | 75 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/taylor-harwood-bellis/980637) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/taylor-harwood-bellis/252793) | CB; — | CB; CB | EA ghi Southampton; giữ CLB PremierHub. |
| Aston Villa | 2000030065 | Tyrone Mings | 77 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/tyrone-mings/303638) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/tyrone-mings/212419) | CB; — | CB; CB |  |
| Aston Villa | 2000030056 | Victor Lindelöf | 77 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/victor-lindelof/143334) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/victor-lindelof/221660) | CB; — | CB; CB |  |
| Aston Villa | 2000030124 | Zion Suzuki | 76 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/suzuki-zion/905351) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/zion-suzuki/255981) | GK; — | GK; GK |  |
| Newcastle | 2000030077 | Aladji Bamba | 72 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/aladji-bamba/1868591) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/aladji-bamba/75734) | CM; CDM | CM; CM | CDM → CM, gộp mã trùng. |
| Newcastle | 2000030081 | Amar Dedić | 76 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/amar-dedic/1102791) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/amar-dedic/257345) | RB; — | RB; RB |  |
| Newcastle | 2000030134 | Bazoumana Touré | 78 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/bazoumana-toure/1568123) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bazoumana-toure/71418) | LW; LM, CAM, CM | LW; LW, LM, CAM, CM |  |
| Newcastle | 18961 | Dan Burn | 79 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/dan-burn/99090) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/dan-burn/198032) | CB; LB | CB; CB, LB |  |
| Newcastle | 2000030132 | Ewen Jaouen | 70 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/ewen-jaouen/1154665) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ewen-jaouen/72022) | GK; — | GK; GK |  |
| Newcastle | 2000030075 | Fabian Schär | 81 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/fabian-schar/101882) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/fabian-schar/210047) | CB; — | CB; CB |  |
| Newcastle | 2000030129 | Harvey Barnes | 80 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/harvey-barnes/855647) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/harvey-barnes/234742) | LW; RW, LM, RM | LW; LW, RW, LM, RM |  |
| Newcastle | 2000030079 | Jacob Ramsey | 78 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/jacob-ramsey/975937) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jacob-ramsey/246923) | CM; LW, CAM, LM | CM; CM, LW, CAM, LM |  |
| Newcastle | 2000030071 | Joe Willock | 76 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/joe-willock/888550) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/joe-willock/237329) | CM; CAM | CM; CM, CAM |  |
| Newcastle | 723 | Joelinton | 80 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/joelinton/560128) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/joelinton/223334) | CM; CDM | CM; CM | CDM → CM, gộp mã trùng. |
| Newcastle | 2000030243 | Kyran Thompson | 60 | USER_PROVIDED_2026-10-03 | [SofaScore](https://www.sofascore.com/football/player/kyran-thompson/2219229) | — | — | RW; RW, RM | Vị trí do người dùng cung cấp 03/10/2026; chưa xác minh EA FC 27. Bổ sung cùng nguồn: height_cm=183, fc27_overall=60. |
| Newcastle | 2000030128 | Lewis Hall | 83 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/lewis-hall/1136730) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/lewis-hall/266127) | LB; — | LB; LB |  |
| Newcastle | 2000030078 | Lewis Miley | 77 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/lewis-miley/1400650) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/lewis-miley/274246) | CM; RB, CDM, RM | CM; CM, RB, RM | CDM → CM, gộp mã trùng. |
| Newcastle | 2000030074 | Lukáš Horníček | 77 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/lukas-hornicek/963744) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/lukas-hornicek/258936) | GK; — | GK; GK |  |
| Newcastle | 2000030131 | Malick Thiaw | 81 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/malick-thiaw/1014286) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/malick-thiaw/256261) | CB; — | CB; CB |  |
| Newcastle | 2000030237 | Mark Gillespie | 62 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/mark-gillespie/108508) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mark-gillespie/198039) | GK; — | GK; GK |  |
| Newcastle | 2000030239 | Mason Miley | 60 | USER_PROVIDED_2026-10-03 | [SofaScore](https://www.sofascore.com/football/player/mason-miley/1947565) | — | — | CM; CM, RB | Vị trí do người dùng cung cấp 03/10/2026; chưa xác minh EA FC 27. Bổ sung cùng nguồn: height_cm=181, fc27_overall=60. |
| Newcastle | 2000030238 | Matias Fernandez-Pardo | 78 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/fernandez-matias/1149144) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/matias-fernandez-pardo/276048) | ST; LM, LW | ST; ST, LM, LW | SofaScore dùng Matías Fernández-Pardo; EA còn ghi Lille. Giữ membership Newcastle của PremierHub. EA ghi LOSC Lille; giữ CLB PremierHub. |
| Newcastle | 2000030242 | Michael Mills | 60 | USER_PROVIDED_2026-10-03 | [SofaScore](https://www.sofascore.com/football/player/michael-mills/1936217) | — | — | ST; ST | Vị trí do người dùng cung cấp 03/10/2026; chưa xác minh EA FC 27. Bổ sung cùng nguồn: height_cm=179, preferred_foot=LEFT, fc27_overall=60. |
| Newcastle | 2000030240 | Miodrag Pivas | 62 | USER_PROVIDED_2026-10-02 | [SofaScore](https://www.sofascore.com/football/player/miodrag-pivas/1129829) | — | — | CB; CB | Vị trí do người dùng cung cấp 03/10/2026; chưa xác minh EA FC 27. Giữ OVR 62 nguồn người dùng 02/10. |
| Newcastle | 18911 | Nick Pope | 78 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/nick-pope/162653) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/nick-pope/203841) | GK; — | GK; GK |  |
| Newcastle | 2000030106 | Nico González | 81 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/nico-gonzalez/954056) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/nico-gonzalez/255069) | CDM; CM | CM; CM | EA có hai cầu thủ trùng tên; chọn hồ sơ người Tây Ban Nha, tiền vệ của Manchester City (EA ID 255069), khớp SofaScore/ngày sinh; không dùng hồ sơ người Argentina (EA ID 240690). Giữ membership Newcastle. CDM → CM, gộp mã trùng. EA ghi Manchester City; giữ CLB PremierHub. |
| Newcastle | 2000030080 | Sean Steur | 72 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/sean-steur/1859920) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/sean-steur/77152) | CM; CDM, CAM | CM; CM, CAM | CDM → CM, gộp mã trùng. |
| Newcastle | 2000030130 | Sven Botman | 81 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/sven-botman/910046) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/sven-botman/251809) | CB; — | CB; CB |  |
| Newcastle | 2000030241 | Vakhtang Salia | 60 | USER_PROVIDED_2026-10-03 | [SofaScore](https://www.sofascore.com/football/player/vakhtang-salia/1426358) | — | — | ST; ST | Vị trí do người dùng cung cấp 03/10/2026; chưa xác minh EA FC 27. Bổ sung cùng nguồn: fc27_overall=60. |
| Newcastle | 158694 | Valentino Livramento | 81 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/tino-livramento/980634) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/tino-livramento/262118) | RB; LB, RM, LM | RB; RB, LB, RM, LM | SofaScore và EA dùng tên Tino Livramento; cùng ngày sinh và Newcastle xác nhận người. |
| Newcastle | 2000030073 | William Osula | 75 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/william-osula/1122603) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/william-osula/270519) | ST; — | ST; ST |  |
| Newcastle | 2000030072 | Yoane Wissa | 80 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/yoane-wissa/805123) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/yoane-wissa/234824) | ST; — | ST; ST |  |

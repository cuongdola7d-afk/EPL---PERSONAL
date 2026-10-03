# Brighton – Bournemouth: hồ sơ và vị trí 2026/27

Ngày chuẩn bị: 2026-10-03. Đây là batch local; chưa ghi MySQL production.

## Roster và dữ liệu được giữ nguyên

- Roster hiện hành từ API PremierHub season=2026, asOf=2026-10-03: Brighton 30,
  Bournemouth 26. Có đúng 56 ID duy nhất, cùng cặp ID/CLB với hai batch hồ sơ 02/10.
  Không giới hạn 25 người, không thêm ID từ danh sách EA hoặc SofaScore.
- Dùng hồ sơ hiện có từ API. Trường đã đủ được giữ nguyên, không thu thập lại.
  Chema Andrés (José María Andrés Baixauli) OVR 69 và Nehemiah Oriola OVR 60 là
  người dùng bổ sung ngày 02/10, đã có trong API; không đổi thành NULL hoặc số EA khác.
  Nguồn: backend/data/player-profile-updates-2026-10-02.csv và file .md tương ứng.
- Chỉ thiếu một trường SofaScore: height_cm của Younes Ibrahim. Đã mở đúng hồ sơ
  SofaScore ID 1899640: General info ghi Height 182 cm và phần mô tả lặp lại 182 cm;
  khớp quốc tịch Egypt, DOB 2008-02-16, Brighton U21 và số áo 59. Bổ sung **182**.
  Giữ nguyên quốc tịch/DOB/chân LEFT/số áo đang có, không sửa theo nguồn khác.
- profiles.csv có đủ 56 người, đúng header PlayerProfileCsvReader. Chỉ chiều cao
  Younes khác snapshot API đã đọc; các ô OVR đã có được giữ nguyên. OVR Younes trống
  tương ứng SQL NULL, không điền 0. profile-updates.csv ghi đúng một ô mới để review.
- Importer hồ sơ chỉ chèn và từ chối nội dung khác đã lưu. profile-updates.csv
  dùng định dạng bản kê bổ sung player_id,club_id,field,value giống batch trước,
  không phải đầu vào PlayerProfileCsvReader. Khi được phép ghi production sau này,
  phải cập nhật có điều kiện đúng ô height_cm NULL trước khi chạy profiles.csv cuối;
  không sửa importer hoặc tự ghi đè hồ sơ trong lượt này.

## Nguồn EA và ánh xạ vị trí

- Lấy Position và Alt Positions trên đúng hồ sơ EA SPORTS FC 27 chính thức ghi
  trong bảng nguồn. Trang ghi Gold/Silver/Bronze lúc phát hành, không dùng thẻ
  chiến dịch hay FC 26. Các OVR trên 53 hồ sơ đã đọc đều khớp OVR hiện có.
- Chỉ CDM → CM cho chính/phụ, gộp mã trùng sau ánh xạ. Không suy vị trí từ nhóm
  rộng, không tự thêm RM cho RW hoặc vị trí khác. Primary luôn có trong eligible.
- Ghép bằng URL/EA ID đã có, tên/biến thể tên, quốc tịch và tuổi theo DOB hiện có.
  Netherlands/Holland, Ireland/Republic of Ireland, Ivory Coast/Côte d'Ivoire,
  USA/United States là các tên quốc gia tương ứng. Cả 53 tuổi EA khớp DOB tại 03/10.
  Julio Soler Barreto = Julio Soler; Junior Kroupi = Eli Junior Kroupi;
  Julian Araujo = Julián Araujo. Nhãn CLB EA/SofaScore khác không đổi PremierHub.
- positions.csv có 53 người: Brighton 27, Bournemouth 26. expected_* trống vì
  API chưa có vị trí. Reader không nhận incoming vị trí trống, nên ba người
  MISSING không nằm trong CSV nhập vị trí; vẫn có đủ dòng profiles và tracking.
- Rayan: mở URL trực tiếp gặp lỗi truy cập; đã đọc được đúng hồ sơ bằng liên kết
  Rayan trên trang đội AFC Bournemouth của EA. Xác minh Position RM, Alt RW,
  Brazil, tuổi 20, OVR 79. Không suy RW từ RM hay lấy một Rayan khác.

## URL roster và nguồn cũ

- [Brighton & Hove Albion FC roster](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Brighton+%26+Hove+Albion+FC); club_id=1000000397; nguồn hồ sơ cũ: backend/data/brighton-profiles-2026-10-02/players.csv và sources.md.
- [AFC Bournemouth roster](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=AFC+Bournemouth); club_id=1000001044; nguồn hồ sơ cũ: backend/data/bournemouth-profiles-2026-10-02/players.csv và sources.md.

## Độ phủ bản cuối local

| CLB | Roster/hồ sơ | Quốc tịch | DOB | Chiều cao | Chân thuận | Số áo | OVR số | OVR EA đã đọc | Vị trí | MISSING vị trí |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Brighton | 30 | 30 | 30 | 30 | 30 | 30 | 29 | 27 | 27 | 3 |
| Bournemouth | 26 | 26 | 26 | 26 | 26 | 26 | 26 | 26 | 26 | 0 |
| Tổng | 56 | 56 | 56 | 56 | 56 | 56 | 55 | 53 | 53 | 3 |

## Người còn thiếu và lý do

- José María Andrés Baixauli / Chema Andrés (2000030174, Brighton): thiếu
  primary_position, eligible_positions. Giữ OVR 69 người dùng 02/10. Tìm được
  các trang EA ngôn ngữ khác ghi FC 26 (không sử dụng); URL tiếng Anh FC 27 thử
  https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/chema-andres/75056
  trả 404. Chưa có nguồn FC 27 đủ chắc để lấy Position/Alt Positions.
- Nehemiah Oriola (2000030170, Brighton): thiếu primary_position,
  eligible_positions; giữ OVR 60 người dùng 02/10. Chưa tìm thấy hồ sơ EA FC 27
  đúng người; không suy từ nhóm rộng. Năm trường hồ sơ đã đủ.
- Younes Ibrahim (2000030176, Brighton): thiếu fc27_overall, primary_position,
  eligible_positions. Năm trường hồ sơ đã đủ sau bổ sung chiều cao từ SofaScore;
  chưa tìm thấy hồ sơ EA FC 27 đúng người. OVR giữ NULL, chưa đủ chọn Fantasy.

Các kết quả tên gần như Younes Namli, Younes El Bahraoui hoặc Ibrahim Salah
không khớp người Egypt/DOB 2008-02-16, nên không được ghép vào Younes Ibrahim.
Đã tìm tên đầy đủ và biến thể Chema Andrés/Chema Andres, Oriola và Younes Ibrahim
trên miền EA, kèm player-ratings/FC 27. Thiếu xác minh URL EA cho OVR người dùng
Chema/Oriola là thiếu nguồn, không phải thiếu giá trị OVR.

## Nguồn theo từng cầu thủ

URL SofaScore là hồ sơ dùng trong batch cũ; chỉ Younes được đọc lại trường thiếu.
OVR bản cuối giữ dữ liệu API. EA chính/phụ ghi nguyên bản, cột nhập ghi sau CDM → CM.
Alt “—” là trang EA không có vị trí phụ, không phải một quyền tự suy.

| CLB | player_id | Tên PremierHub | OVR bản cuối | Nguồn OVR | SofaScore | EA FC 27 | EA chính; phụ | Nhập chính; tập | Ghi chú |
| --- | --- | --- | ---: | --- | --- | --- | --- | --- | --- |
| Brighton | 2000030055 | Bart Verbruggen | 81 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/bart-verbruggen/994363) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bart-verbruggen/258498) | GK; — | GK; GK |  |
| Brighton | 2000030051 | Charalampos Kostoulas | 73 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/kostoulas-charalampos/1416535) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/charalampos-kostoulas/73885) | ST; — | ST; ST |  |
| Brighton | 2000030050 | Costinha | 75 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/costinha/988333) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/costinha/251942) | RB; RM | RB; RB, RM |  |
| Brighton | 2000030168 | Diego Coppola | 75 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/diego-coppola/1050692) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/diego-coppola/269181) | CB; — | CB; CB | SofaScore hiện ghi Paris FC (cho mượn) và số áo 42; giữ membership Brighton của PremierHub. EA ghi Paris FC; giữ membership PremierHub. |
| Brighton | 2000030043 | Diego Gómez | 78 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/diego-gomez/1065588) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/diego-gomez/269278) | CM; CAM, RM, CDM | CM; CM, CAM, RM | CDM → CM, gộp mã trùng. |
| Brighton | 2000030172 | Evan Ferguson | 74 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/evan-ferguson/999231) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/evan-ferguson/259608) | ST; CAM | ST; ST, CAM |  |
| Brighton | 2000030175 | Femi Azeez | 75 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/femi-azeez/1007093) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/femi-azeez/253433) | RM; LM, RW, LW | RM; RM, LM, RW, LW | EA hiện ghi Millwall; giữ membership Brighton của PremierHub và số áo SofaScore. EA ghi Millwall; giữ membership PremierHub. |
| Brighton | 2000030167 | Ferdi Kadıoğlu | 80 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/ferdi-kadioglu/825844) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ferdi-kad-oglu/235152) | LB; RB, CM | LB; LB, RB, CM |  |
| Brighton | 2000030052 | Ibrahim Osman | 72 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/osman-ibrahim/1471095) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ibrahim-osman/274516) | LM; RM, LW, RW | LM; LM, RM, LW, RW |  |
| Brighton | 2000030041 | Jack Hinshelwood | 77 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/jack-hinshelwood/1142315) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jack-hinshelwood/277283) | CDM; CAM, RB, CM | CM; CM, CAM, RB | CDM → CM, gộp mã trùng. |
| Brighton | 2000030173 | Jaouen Hadjam | 69 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/jaouen-hadjam/1049534) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jaouen-hadjam/258230) | LB; LM, CB, LW | LB; LB, LM, CB, LW | EA hiện ghi BSC Young Boys; giữ membership Brighton của PremierHub. EA ghi BSC Young Boys; giữ membership PremierHub. |
| Brighton | 2000030118 | Jason Steele | 73 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/jason-steele/31867) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jason-steele/188836) | GK; — | GK; GK |  |
| Brighton | 2000030174 | José María Andrés Baixauli | 69 | USER_PROVIDED_2026-10-02 | [SofaScore](https://www.sofascore.com/football/player/chema-andres/1464641) | — | — | MISSING | Chưa xác minh hồ sơ EA FC 27 đúng người. Giữ OVR 69 người dùng 02/10, đã có trên API. Trang EA tìm được ghi FC 26 bị loại; URL tiếng Anh trả 404. |
| Brighton | 106835 | Kaoru Mitoma | 81 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/kaoru-mitoma/936849) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kaoru-mitoma/255565) | LM; LW | LM; LM, LW |  |
| Brighton | 2000030045 | Lewis Dunk | 77 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/lewis-dunk/115365) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/lewis-dunk/199915) | CB; — | CB; CB |  |
| Brighton | 2000030046 | Luka Vušković | 81 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/luka-vuskovic/1405212) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/luka-vuskovic/275192) | CB; — | CB; CB |  |
| Brighton | 2000030054 | Malick Yalcouyé | 70 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/malick-yalcouye/1568186) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/malick-yalcouye/70841) | CM; RM, CAM, RW | CM; CM, RM, CAM, RW |  |
| Brighton | 2000030047 | Mats Wieffer | 79 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/mats-wieffer/959628) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mats-wieffer/248793) | RB; CDM, CM, RM | RB; RB, CM, RM | CDM → CM, gộp mã trùng. |
| Brighton | 2000030169 | Matt O'Riley | 77 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/matt-oriley/891829) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/matt-o-riley/240734) | CAM; CM, CDM | CAM; CAM, CM | CDM → CM, gộp mã trùng. |
| Brighton | 2000030042 | Maxim De Cuyper | 78 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/maxim-de-cuyper/997152) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/maxim-de-cuyper/251479) | LB; LM | LB; LB, LM |  |
| Brighton | 2000030120 | Michael Svoboda | 72 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/michael-svoboda/888340) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/michael-svoboda/250779) | CB; — | CB; CB |  |
| Brighton | 2000030170 | Nehemiah Oriola | 60 | USER_PROVIDED_2026-10-02 | [SofaScore](https://www.sofascore.com/football/player/nehemiah-oriola/1899611) | — | — | MISSING | Chưa xác minh hồ sơ EA FC 27 đúng người. Giữ OVR 60 người dùng 02/10, đã có trên API. |
| Brighton | 2000030117 | Olivier Boscagli | 78 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/olivier-boscagli/788784) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/olivier-boscagli/225863) | CB; — | CB; CB |  |
| Brighton | 2000030049 | Pascal Groß | 79 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/pascal-gro/48480) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/pascal-gro/190765) | CDM; CM | CM; CM | CDM → CM, gộp mã trùng. |
| Brighton | 2000030119 | Pascal Struijk | 76 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/pascal-struijk/836675) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/pascal-struijk/239360) | CB; — | CB; CB |  |
| Brighton | 2000030121 | Promise David | 76 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/promise-david/1119328) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/promise-david/73070) | ST; — | ST; ST |  |
| Brighton | 2000030171 | Stefanos Tzimas | 73 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/tzimas-stefanos/1155321) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/stefanos-tzimas/274699) | ST; — | ST; ST |  |
| Brighton | 383685 | Yankuba Minteh | 80 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/yankuba-minteh/1400106) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/yankuba-minteh/271800) | RM; LM, RW, LW | RM; RM, LM, RW, LW |  |
| Brighton | 2000030048 | Yasin Ayari | 79 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/yasin-ayari/1036269) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/yasin-ayari/257400) | CDM; CM | CM; CM | CDM → CM, gộp mã trùng. |
| Brighton | 2000030176 | Younes Ibrahim | NULL | MISSING | [SofaScore](https://www.sofascore.com/football/player/younes-ibrahim/1899640) | — | — | MISSING | Chưa xác minh hồ sơ EA FC 27 đúng người. SofaScore đọc lại ghi 182 cm, bổ sung height_cm; OVR NULL. |
| Bournemouth | 2000030108 | Adam Smith | 73 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/adam-smith/44566) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/adam-smith/190885) | RB; RM | RB; RB, RM |  |
| Bournemouth | 2000030111 | Adrien Truffert | 80 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/truffert-adrien/999028) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/adrien-truffert/256769) | LB; LM | LB; LB, LM |  |
| Bournemouth | 2000030110 | Alex Scott | 81 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/alex-scott/1104986) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/alex-scott/261299) | CM; CDM, CAM | CM; CM, CAM | CDM → CM, gộp mã trùng. |
| Bournemouth | 2000030038 | Alex Tóth | 73 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/alex-toth/1476524) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/alex-toth/70903) | CM; CDM | CM; CM | CDM → CM, gộp mã trùng. |
| Bournemouth | 2000030155 | Amine Adli | 77 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/amine-adli/991478) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/amine-adli/255223) | LM; ST, LW, CAM | LM; LM, ST, LW, CAM |  |
| Bournemouth | 2000030035 | António Silva | 77 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/antonio-silva/1006069) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/antonio-silva/270086) | CB; — | CB; CB |  |
| Bournemouth | 2000030113 | Bafodé Diakité | 79 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/bafode-diakite/962408) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bafode-diakite/246565) | CB; — | CB; CB |  |
| Bournemouth | 2000030034 | Ben Gannon-Doak | 71 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/ben-doak/1154861) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ben-gannon-doak/266815) | RM; RW | RM; RM, RW |  |
| Bournemouth | 2000030040 | Daniel Jebbison | 68 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/daniel-jebbison/1096136) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/daniel-jebbison/262227) | ST; — | ST; ST |  |
| Bournemouth | 2000030115 | David Brooks | 76 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/david-brooks/855731) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/david-brooks/220196) | RM; RW | RM; RM, RW |  |
| Bournemouth | 2000030032 | Evanilson | 79 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/evanilson/998490) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/evanilson/256612) | ST; — | ST; ST |  |
| Bournemouth | 2000030152 | Fraser Forster | 71 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/fraser-forster/19314) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/fraser-forster/172203) | GK; — | GK; GK |  |
| Bournemouth | 2000030109 | James Hill | 78 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/james-hill/1156586) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/james-hill/248492) | CB; RB | CB; CB, RB |  |
| Bournemouth | 51051 | Julian Araujo | 74 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/julian-araujo/978150) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/julian-araujo/247678) | RB; RM | RB; RB, RM |  |
| Bournemouth | 2000030154 | Julio Soler Barreto | 68 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/julio-soler/1201520) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/julio-soler/269796) | LB; LM | LB; LB, LM | SofaScore ghi Julio Soler. |
| Bournemouth | 2000030156 | Junior Kroupi | 80 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/eli-junior-kroupi/1426228) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/eli-junior-kroupi/277909) | ST; CAM | ST; ST, CAM | SofaScore ghi Eli Junior Kroupi. |
| Bournemouth | 2000030039 | Justin Kluivert | 79 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/justin-kluivert/851596) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/justin-kluivert/236920) | CAM; LM, LW | CAM; CAM, LM, LW |  |
| Bournemouth | 2000030037 | Lewis Cook | 78 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/lewis-cook/548188) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/lewis-cook/224294) | CDM; RB, CM | CM; CM, RB | CDM → CM, gộp mã trùng. |
| Bournemouth | 2000030031 | Marcus Tavernier | 79 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/marcus-tavernier/895576) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/marcus-tavernier/237477) | LM; RM, CAM, LW | LM; LM, RM, CAM, LW |  |
| Bournemouth | 2000030157 | Michele Di Gregorio | 80 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/michele-di-gregorio/844609) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/michele-di-gregorio/235840) | GK; — | GK; GK | EA có thể còn ghi Juventus; giữ membership Bournemouth của PremierHub. EA ghi Juventus; giữ membership PremierHub. |
| Bournemouth | 2000030033 | Rayan | 79 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/rayan/1464966) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/rayan/83494) | RM; RW | RM; RM, RW | Đọc hồ sơ qua liên kết Rayan trên trang đội EA sau lỗi mở trực tiếp. |
| Bournemouth | 1125 | Ryan Christie | 78 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/ryan-christie/322391) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ryan-christie/213884) | CDM; CM | CM; CM | CDM → CM, gộp mã trùng. |
| Bournemouth | 2000030036 | Tyler Adams | 79 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/tyler-adams/800419) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/tyler-adams/232999) | CDM; CM | CM; CM | CDM → CM, gộp mã trùng. |
| Bournemouth | 2000030153 | Veljko Milosavljevic | 72 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/milosavljevic-veljko/1406130) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/veljko-milosavljevic/81319) | CB; — | CB; CB |  |
| Bournemouth | 2000030116 | Álvaro Rodriguez | 75 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/alvaro-rodriguez/1154587) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/alvaro-rodriguez/272445) | ST; CAM | ST; ST, CAM |  |
| Bournemouth | 2000030030 | Đorđe Petrović | 79 | EA_FC27_EXISTING_PROFILE | [SofaScore](https://www.sofascore.com/football/player/dorde-petrovic/882604) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/or-e-petrovic/269626) | GK; — | GK; GK |  |

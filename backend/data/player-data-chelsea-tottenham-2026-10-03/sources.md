# Chelsea và Tottenham — hồ sơ/vị trí 2026/27, batch local 03/10/2026

## Roster, hồ sơ hiện có và phạm vi

- Chốt roster hiện hành qua API PremierHub mùa 2026, `asOf=2026-10-03`; đối chiếu
  với snapshot `asOf=2026-10-02` và hai batch hồ sơ ngày 02/10. Có đúng **56 ID/CLB**:
  Chelsea 27 (`club_id=1000000061`), Tottenham 29 (`club_id=1000000073`). Không giới hạn
  25 người và không thêm ID bên ngoài; hai ngày có cùng 56 cặp ID/CLB.
- API là nguồn ID, CLB, hồ sơ đang lưu. Cả 56 người đã có đủ năm trường SofaScore.
  Tái sử dụng hai `players.csv`/`sources.md` hồ sơ cũ; không thu thập lại quốc tịch,
  ngày sinh, chiều cao, chân thuận hoặc số áo. URL SofaScore gốc vẫn được liệt kê.
- `profiles.csv` có 56 dòng, đúng header PlayerProfileCsvReader, khớp sáu trường
  hiện có trên API. OVR vẫn lấy nguyên giá trị đã lưu, không đổi hoặc tự cấp OVR.
  Mahdi Nicoll-Jazuli còn NULL; James Rowswell **61** lấy từ bản bổ sung của người dùng
  `backend/data/player-profile-updates-2026-10-02.csv` và API, **không gắn nhãn EA xác minh**.
  File cũ Tottenham để trống OVR James là snapshot trước bổ sung; không chạy lại file cũ.
- Nguồn vị trí mới là **Position** và **Alt Positions** trên đúng hồ sơ **EA SPORTS FC 27**
  trong bảng dưới, đọc ngày 03/10/2026. Các trang ghi thẻ cơ bản Gold/Silver/Bronze lúc
  phát hành, không dùng thẻ chiến dịch hoặc FC 26. Các OVR đọc được khi lấy vị trí
  đều khớp giá trị hiện có. Không đổi CLB PremierHub theo nhãn CLB EA có thể còn cũ.
- Ghép theo ID EA/URL đã ghi ở hồ sơ cũ, tên/biến thể tên và quốc tịch/tuổi khi trang
  hiển thị. Holland/Netherlands và Czech Republic/Czechia là tên quốc gia tương ứng;
  giữ quốc tịch hồ sơ SofaScore. Các ghi chú nhận dạng/CLB từ batch trước được giữ ở bảng.
- Chỉ ánh xạ **CDM → CM** cho chính và phụ, gộp mã trùng sau ánh xạ. Không tự thêm RM
  cho RW, LB cho CB hoặc bất kỳ quyền khác; nhóm vị trí rộng không tham gia ánh xạ.
- API hiện trả MISSING cho cả 56 người. `positions.csv` chỉ có 54 người đã xác minh,
  đúng header PlayerPositionCsvReader; `expected_*` trống vì chưa có vị trí đang lưu.
  Reader không nhận dòng có vị trí mới trống, nên hai người MISSING chỉ nằm trong
  `roster-status.csv` (đủ 56 người, ô vị trí để trống) và `missing-fields.txt`.
- Không sửa importer, UI Fantasy, roster, membership, thống kê, dữ liệu 2024/25;
  chưa ghi MySQL production, commit, push hay deploy.

## URL roster và nguồn đã có

- [Chelsea FC roster hiện hành](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Chelsea+FC); hồ sơ/SofaScore đã thu thập: `backend/data/chelsea-profiles-2026-10-02/players.csv` và `sources.md`.
- [Tottenham Hotspur FC roster hiện hành](https://epl-personal-production.up.railway.app/api/players?season=2026&asOf=2026-10-03&club=Tottenham+Hotspur+FC); hồ sơ/SofaScore đã thu thập: `backend/data/tottenham-profiles-2026-10-02/players.csv` và `sources.md`.
- OVR James Rowswell: `backend/data/player-profile-updates-2026-10-02.csv`, dòng
  `2000030248,1000000073,fc27_overall,61`; nguồn người dùng được ghi trong file `.md` tương ứng.

## Độ phủ

| CLB | Roster/hồ sơ | Quốc tịch | Ngày sinh | Chiều cao | Chân thuận | Số áo | OVR có số | OVR EA đã xác minh | Có vị trí | MISSING vị trí |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Chelsea | 27 | 27 | 27 | 27 | 27 | 27 | 26 | 26 | 26 | 1 |
| Tottenham | 29 | 29 | 29 | 29 | 29 | 29 | 29 | 28 | 28 | 1 |
| Tổng | 56 | 56 | 56 | 56 | 56 | 56 | 55 | 54 | 54 | 2 |

## Trường còn thiếu hoặc cần nguồn bổ sung

| CLB | player_id | Cầu thủ | Cần bổ sung | Lý do / giá trị giữ nguyên |
| --- | --- | --- | --- | --- |
| Chelsea | 2000005021 | Mahdi Nicoll-Jazuli | fc27_overall, primary_position, eligible_positions | Chưa tìm được hồ sơ EA FC 27 phù hợp; giữ NULL/MISSING. Năm trường SofaScore đã đủ. |
| Tottenham | 2000030248 | James Rowswell | primary_position, eligible_positions; URL EA để xác minh OVR chính thức | Chưa tìm được hồ sơ EA phù hợp theo cả Rowswell/Roswell. OVR 61 do người dùng bổ sung vẫn giữ; không cần cấp lại OVR. |

Đã tìm trên miền EA bằng Mahdi Nicoll-Jazuli / Nicoll Jazuli và James Rowswell /
James Roswell, kèm FC 27/ratings; chưa có kết quả ghép đủ chắc chắn. Không lấy vị trí
rộng từ SofaScore hay PremierHub thay cho Position/Alt Positions của EA.

## Nguồn theo người

OVR trong bảng là giá trị đã có; vị trí EA gốc giúp kiểm tra phép ánh xạ CDM → CM.
Vị trí phụ `—` là trang EA không ghi Alt Positions, không phải tự suy vị trí phụ.

| CLB | player_id | PremierHub name | OVR đang lưu | SofaScore đã dùng trước | EA FC 27 | EA chính; phụ | Vị trí nhập (chính; tập) | Ghi chú |
| --- | --- | --- | ---: | --- | --- | --- | --- | --- |
| Chelsea | 2000030178 | Aaron Anselmino | 72 | [SofaScore](https://www.sofascore.com/football/player/anselmino-aaron/1500263) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/aaron-anselmino/278455) | CB; — | CB; CB |  |
| Chelsea | 2000005009 | Cole Palmer | 85 | [SofaScore](https://www.sofascore.com/football/player/cole-palmer/982780) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/cole-palmer/257534) | CAM; RM, CM, RW | CAM; CAM, RM, CM, RW |  |
| Chelsea | 2000005013 | Danny Welbeck | 80 | [SofaScore](https://www.sofascore.com/football/player/danny-welbeck/33902) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/danny-welbeck/186146) | ST; — | ST; ST |  |
| Chelsea | 2000005001 | Emiliano Martinez | 85 | [SofaScore](https://www.sofascore.com/football/player/emiliano-martinez/158263) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/emiliano-martinez/202811) | GK; — | GK; GK | EA still lists Aston Villa. |
| Chelsea | 2000005015 | Emmanuel Emegha | 78 | [SofaScore](https://www.sofascore.com/football/player/emanuel-emegha/1048333) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/emanuel-emegha/258437) | ST; — | ST; ST | Profile uses Emmanuel Emegha; name variant checked against club and identity. |
| Chelsea | 2000005024 | Estevao | 80 | [SofaScore](https://www.sofascore.com/football/player/estevao/1597265) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/estevao/76687) | RM; RW | RM; RM, RW |  |
| Chelsea | 2000005016 | Geovany Quenda | 76 | [SofaScore](https://www.sofascore.com/football/player/geovany-quenda/1403165) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/geovany-quenda/279044) | RM; LM, CAM, RW | RM; RM, LM, CAM, RW |  |
| Chelsea | 2000005010 | Jamie Bynoe-Gittens | 77 | [SofaScore](https://www.sofascore.com/football/player/jamie-bynoe-gittens/1140599) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jamie-gittens/266032) | LM; LW | LM; LM, LW | Profile uses Jamie Gittens; name variant checked against club and identity. |
| Chelsea | 2000005008 | Joao Pedro | 83 | [SofaScore](https://www.sofascore.com/football/player/joao-pedro/975079) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/joao-pedro/252042) | ST; CAM | ST; ST, CAM |  |
| Chelsea | 2000005011 | Jordan Henderson | 78 | [SofaScore](https://www.sofascore.com/football/player/jordan-henderson/42694) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jordan-henderson/183711) | CDM; CM | CM; CM | CDM → CM, gộp mã trùng. |
| Chelsea | 2000005014 | Jorrel Hato | 78 | [SofaScore](https://www.sofascore.com/football/player/jorrel-hato/1153079) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jorrel-hato/272978) | LB; CB, CDM | LB; LB, CB, CM | CDM → CM, gộp mã trùng. |
| Chelsea | 2000005022 | Josh Acheampong | 75 | [SofaScore](https://www.sofascore.com/football/player/josh-acheampong/1403050) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/josh-acheampong/71651) | CB; RB | CB; CB, RB |  |
| Chelsea | 2000005006 | Levi Colwill | 80 | [SofaScore](https://www.sofascore.com/football/player/levi-colwill/996911) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/levi-colwill/262859) | CB; — | CB; CB |  |
| Chelsea | 2000005021 | Mahdi Nicoll-Jazuli | NULL | [SofaScore](https://www.sofascore.com/football/player/mahdi-nicoll-jazuli/2037476) | — | — | MISSING | Chưa ghép được hồ sơ EA FC 27. OVR NULL. |
| Chelsea | 2000005019 | Malo Gusto | 79 | [SofaScore](https://www.sofascore.com/football/player/gusto-malo/996958) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/malo-gusto/259307) | RB; CM | RB; RB, CM |  |
| Chelsea | 2000005002 | Marco Palestra | 78 | [SofaScore](https://www.sofascore.com/football/player/palestra-marco/1397736) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/marco-palestra/278339) | RB; RM | RB; RB, RM |  |
| Chelsea | 2000005005 | Maxence Lacroix | 82 | [SofaScore](https://www.sofascore.com/football/player/maxence-lacroix/879674) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/maxence-lacroix/244067) | CB; — | CB; CB |  |
| Chelsea | 2000005023 | Mike Penders | 78 | [SofaScore](https://www.sofascore.com/football/player/mike-penders/1149148) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mike-penders/270821) | GK; — | GK; GK |  |
| Chelsea | 2000005018 | Moises Caicedo | 86 | [SofaScore](https://www.sofascore.com/football/player/moises-caicedo/987650) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/moises-caicedo/256079) | CDM; CM | CM; CM | CDM → CM, gộp mã trùng. |
| Chelsea | 2000005012 | Morgan Rogers | 84 | [SofaScore](https://www.sofascore.com/football/player/morgan-rogers/948261) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/morgan-rogers/260247) | CAM; LM, CM, LW | CAM; CAM, LM, CM, LW |  |
| Chelsea | 2000005007 | Pedro Neto | 81 | [SofaScore](https://www.sofascore.com/football/player/pedro-neto/879349) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/pedro-neto/238616) | RM; LM, RW, LW | RM; RM, LM, RW, LW |  |
| Chelsea | 2000005020 | Pep Chavarria | 79 | [SofaScore](https://www.sofascore.com/football/player/josep-chavarria/1010421) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/pep-chavarria/258371) | LB; LM | LB; LB, LM |  |
| Chelsea | 2000005017 | Reece James | 84 | [SofaScore](https://www.sofascore.com/football/player/reece-james/885908) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/reece-james/238074) | RB; CDM, CM | RB; RB, CM | CDM → CM, gộp mã trùng. |
| Chelsea | 2000005025 | Romeo Lavia | 78 | [SofaScore](https://www.sofascore.com/football/player/romeo-lavia/1069488) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/romeo-lavia/263620) | CDM; CM | CM; CM | CDM → CM, gộp mã trùng. |
| Chelsea | 2000030177 | Shumaira Mheuka | 66 | [SofaScore](https://www.sofascore.com/football/player/shumaira-mheuka/1402787) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/shumaira-mheuka/77540) | ST; — | ST; ST | SofaScore displays Celtic on loan from Chelsea; shirt number 29 is taken from SofaScore, PremierHub membership is retained. |
| Chelsea | 2000005004 | Valentin Barco | 79 | [SofaScore](https://www.sofascore.com/football/player/valentin-barco/1127057) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/valentin-barco/263370) | CM; CDM | CM; CM | CDM → CM, gộp mã trùng. |
| Chelsea | 2000005003 | Wesley Fofana | 79 | [SofaScore](https://www.sofascore.com/football/player/wesley-fofana/923894) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/wesley-fofana/248695) | CB; — | CB; CB |  |
| Tottenham | 2000030027 | Andy Robertson | 80 | [SofaScore](https://www.sofascore.com/football/player/andy-robertson/262911) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/andrew-robertson/216267) | LB; LM | LB; LB, LM | Profile uses Andy Robertson; name variant checked against club and identity. |
| Tottenham | 2000030015 | Antonín Kinský | 77 | [SofaScore](https://www.sofascore.com/football/player/antonin-kinsky/1031251) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/antonin-kinsky/73580) | GK; — | GK; GK |  |
| Tottenham | 2000030017 | Archie Gray | 77 | [SofaScore](https://www.sofascore.com/football/player/archie-gray/1142335) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/archie-gray/270208) | CDM; CB, RB, CM | CM; CM, CB, RB | CDM → CM, gộp mã trùng. |
| Tottenham | 2000030104 | Ben Davies | 74 | [SofaScore](https://www.sofascore.com/football/player/ben-davies/94758) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ben-davies/205923) | CB; — | CB; CB |  |
| Tottenham | 156428 | Brandon Austin | 67 | [SofaScore](https://www.sofascore.com/football/player/brandon-austin/859896) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/brandon-austin/236568) | GK; — | GK; GK |  |
| Tottenham | 2000030251 | Callum Olusesi | 62 | [SofaScore](https://www.sofascore.com/football/player/callum-olusesi/1403190) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/callum-olusesi/75770) | CM; CAM, CDM | CM; CM, CAM | SofaScore displays the U21 side; shirt number is from that profile, PremierHub senior-club membership is retained. CDM → CM, gộp mã trùng. |
| Tottenham | 2000030025 | Conor Gallagher | 78 | [SofaScore](https://www.sofascore.com/football/player/conor-gallagher/904970) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/conor-gallagher/238216) | CM; CDM, CAM | CM; CM, CAM | CDM → CM, gộp mã trùng. |
| Tottenham | 30435 | Dejan Kulusevski | 81 | [SofaScore](https://www.sofascore.com/football/player/dejan-kulusevski/928124) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/dejan-kulusevski/247394) | CM; RW, CAM, RM | CM; CM, RW, CAM, RM |  |
| Tottenham | 2000030016 | Destiny Udogie | 79 | [SofaScore](https://www.sofascore.com/football/player/destiny-udogie/983572) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/destiny-udogie/259583) | LB; LM | LB; LB, LM |  |
| Tottenham | 2000030018 | Dominic Solanke | 79 | [SofaScore](https://www.sofascore.com/football/player/dominic-solanke/361420) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/dominic-solanke/225539) | ST; — | ST; ST |  |
| Tottenham | 2000030020 | James Maddison | 82 | [SofaScore](https://www.sofascore.com/football/player/james-maddison/356398) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/james-maddison/220697) | CM; CAM | CM; CM, CAM |  |
| Tottenham | 2000030248 | James Rowswell | 61 | [SofaScore](https://www.sofascore.com/football/player/james-roswell/1403130) | — | — | MISSING | Chưa ghép được hồ sơ EA FC 27. OVR 61 nguồn người dùng; đã có trong API. |
| Tottenham | 2000030028 | Jan Paul van Hecke | 81 | [SofaScore](https://www.sofascore.com/football/player/jan-paul-van-hecke/962012) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jan-paul-van-hecke/258908) | CB; — | CB; CB |  |
| Tottenham | 2000030247 | Jun'ai Byfield | 62 | [SofaScore](https://www.sofascore.com/football/player/junai-byfield/1913038) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jun-ai-byfield/81899) | CB; — | CB; CB | SofaScore displays the U21 side; shirt number is from that profile, PremierHub senior-club membership is retained. |
| Tottenham | 2000030022 | Lucas Bergvall | 78 | [SofaScore](https://www.sofascore.com/football/player/lucas-bergvall/1391251) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/lucas-bergvall/272926) | CM; CAM, CDM | CM; CM, CAM | CDM → CM, gộp mã trùng. |
| Tottenham | 2000030026 | Marcos Senesi | 82 | [SofaScore](https://www.sofascore.com/football/player/marcos-senesi/830659) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/marcos-senesi/236506) | CB; — | CB; CB |  |
| Tottenham | 2000030102 | Martin Dúbravka | 77 | [SofaScore](https://www.sofascore.com/football/player/martin-dubravka/42209) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/martin-dubravka/220407) | GK; — | GK; GK |  |
| Tottenham | 2000030024 | Mateus Fernandes | 80 | [SofaScore](https://www.sofascore.com/football/player/mateus-fernandes/1142562) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mateus-fernandes/270857) | CM; CAM, CDM | CM; CM, CAM | CDM → CM, gộp mã trùng. |
| Tottenham | 152849 | Micky van de Ven | 81 | [SofaScore](https://www.sofascore.com/football/player/micky-van-de-ven/998247) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/micky-van-de-ven/264453) | CB; LB | CB; CB, LB |  |
| Tottenham | 15911 | Mohammed Kudus | 81 | [SofaScore](https://www.sofascore.com/football/player/mohammed-kudus/905163) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mohammed-kudus/245155) | RM; RW, ST, CAM | RM; RM, RW, ST, CAM |  |
| Tottenham | 63577 | Mykhaylo Mudryk | 75 | [SofaScore](https://www.sofascore.com/football/player/mykhailo-mudryk/958966) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mykhailo-mudryk/246340) | LM; LW | LM; LM, LW | Profile uses Mykhailo Mudryk; name variant checked against club and identity. EA still lists Chelsea. |
| Tottenham | 2000030107 | Omar Marmoush | 82 | [SofaScore](https://www.sofascore.com/football/player/omar-marmoush/873554) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/omar-marmoush/256675) | LW; ST, LM, CAM | LW; LW, ST, LM, CAM | EA still lists Manchester City. EA ghi Manchester City; vẫn giữ CLB PremierHub. |
| Tottenham | 47519 | Pedro Porro | 83 | [SofaScore](https://www.sofascore.com/football/player/pedro-porro/913654) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/pedro-porro/243576) | RB; RM | RB; RB, RM |  |
| Tottenham | 2000030023 | Rodrigo Bentancur | 79 | [SofaScore](https://www.sofascore.com/football/player/rodrigo-bentancur/791190) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/rodrigo-bentancur/227535) | CDM; CM | CM; CM | CDM → CM, gộp mã trùng. |
| Tottenham | 2000030100 | Sandro Tonali | 85 | [SofaScore](https://www.sofascore.com/football/player/sandro-tonali/892673) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/sandro-tonali/241096) | CDM; CM | CM; CM | CDM → CM, gộp mã trùng. |
| Tottenham | 2000030246 | Sávio Moreira de Oliveira | 80 | [SofaScore](https://www.sofascore.com/football/player/savio/1046795) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/savinho/270409) | RW; LW, RM, LM | RW; RW, LW, RM, LM | Profile uses Sávio; name variant checked against club and identity. EA displays Savinho and still lists Manchester City. EA ghi Manchester City; vẫn giữ CLB PremierHub. |
| Tottenham | 19145 | Tosin Adarabioyo | 77 | [SofaScore](https://www.sofascore.com/football/player/tosin-adarabioyo/352668) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/tosin-adarabioyo/222104) | CB; — | CB; CB | EA still lists Chelsea. |
| Tottenham | 2000030250 | Wilson Odobert | 78 | [SofaScore](https://www.sofascore.com/football/player/wilson-odobert/1142679) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/wilson-odobert/270579) | LM; LW, RM, RW | LM; LM, LW, RM, RW |  |
| Tottenham | 2000030249 | Xavi Simons | 81 | [SofaScore](https://www.sofascore.com/football/player/xavi-simons/997183) | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/xavi-simons/245367) | CAM; LM, LW, ST | CAM; CAM, LM, LW, ST |  |

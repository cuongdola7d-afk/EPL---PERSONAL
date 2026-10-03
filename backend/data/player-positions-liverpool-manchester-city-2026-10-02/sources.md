# Liverpool và Manchester City — vị trí cầu thủ 2026/27

## Roster và quy tắc nhập

- Lấy `player_id` từ roster PremierHub `backend/data/roster-2026-09-30/batch-*.csv` có membership hiệu lực ngày 2026-10-02; đối chiếu đủ 31 Liverpool và 26 Manchester City với các CSV hồ sơ 2026-10-02. Không thêm ID từ nguồn bên ngoài và không đổi CLB theo nhãn trên EA.
- Nguồn chính là mục `Position` và `Alt Positions` trên hồ sơ cầu thủ **EA SPORTS FC 27** được liên kết từng người bên dưới. URL từ batch hồ sơ trước được mở và đọc lại trong lượt này. Sáu cầu thủ không có hồ sơ EA phù hợp dùng vị trí người dùng cung cấp ngày 03/10/2026, ghi riêng ở bảng dưới; không gán nhãn là đã xác minh qua EA.
- Mã lưu hợp lệ: `GK LB CB RB CM CAM LM RM LW ST RW`. Quy đổi `CDM → CM` cho vị trí chính và phụ theo quy tắc người dùng; nếu `CM` đã có thì chỉ lưu một lần. Giữ mã EA gốc trong bảng nguồn để kiểm tra được phép quy đổi.
- CSV chứa 51 người có vị trí từ EA và sáu người có vị trí do người dùng cung cấp. Với ba cầu thủ có nhiều mã do người dùng nêu, mã đầu tiên là vị trí chính. Hai cột `expected_*` để trống vì đây là bản nhập đầu. Không thay đổi nhóm vị trí rộng, OVR, thống kê trận, membership hoặc mùa 2024/25.

## Độ phủ

| CLB | Roster | Có vị trí trong CSV | MISSING |
| --- | ---: | ---: | ---: |
| Liverpool | 31 | 31 | 0 |
| Manchester City | 26 | 26 | 0 |
| Tổng | 57 | 57 | 0 |

## Vị trí người dùng bổ sung

| CLB | player_id | Cầu thủ | Vị trí chính | Tập vị trí hợp lệ |
| --- | ---: | --- | --- | --- |
| Liverpool | 2000003025 | Jayden Danns | ST | ST |
| Liverpool | 2000030229 | Wellity Lucky | CB | CB |
| Manchester City | 2000004019 | Allan Andrade Elias | RM | RM, RW |
| Manchester City | 2000030232 | Floyd Samba | CAM | CAM, CM |
| Manchester City | 2000004024 | Kaden Braithwaite | CB | CB |
| Manchester City | 2000004023 | Ryan McAidoo | RW | RW, RM |

## Nguồn theo cầu thủ

| CLB | player_id | Cầu thủ | Hồ sơ EA FC 27 | EA: chính; phụ | Vị trí lưu / nguồn bổ sung |
| --- | ---: | --- | --- | --- | --- |
| Liverpool | 2000003009 | Alexander Isak | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/alexander-isak/233731) | ST | ST; ST |
| Liverpool | 2000003010 | Alexis Mac Allister | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/alexis-mac-allister/239837) | CM; CDM | CM; CM (CDM→CM) |
| Liverpool | 2000003001 | Alisson Becker | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/alisson/212831) | GK | GK; GK |
| Liverpool | 2000003017 | Bradley Barcola | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bradley-barcola/264652) | LW; RW, LM, RM | LW; LW, RW, LM, RM |
| Liverpool | 2000003011 | Cody Gakpo | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/cody-gakpo/242516) | LM; ST, LW | LM; LM, ST, LW |
| Liverpool | 180317 | Conor Bradley | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/conor-bradley/264298) | RB; RM | RB; RB, RM |
| Liverpool | 2000003008 | Dominik Szoboszlai | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/dominik-szoboszlai/236772) | CAM; RB, CDM, CM | CAM; CAM, RB, CM (CDM→CM) |
| Liverpool | 2000030231 | Federico Chiesa | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/federico-chiesa/235805) | RM; ST, RW | RM; RM, ST, RW |
| Liverpool | 2000003007 | Florian Wirtz | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/florian-wirtz/256630) | CAM; LM, CM, LW | CAM; CAM, LM, CM, LW |
| Liverpool | 2000003016 | Freddie Woodman | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/freddie-woodman/222514) | GK | GK; GK |
| Liverpool | 2000003015 | Giorgi Mamardashvili | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/giorgi-mamardashvili/262621) | GK | GK; GK |
| Liverpool | 2000030230 | Giovanni Leoni | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/giovanni-leoni/70824) | CB | CB; CB |
| Liverpool | 2000030228 | Harvey Davies | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/harvey-davies/271977) | GK | GK; GK |
| Liverpool | 2000003013 | Hugo Ekitike | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/hugo-ekitike/257289) | ST; CAM | ST; ST, CAM |
| Liverpool | 2000003022 | James McConnell | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/james-mc-connell/278292) | CDM; CM | CM; CM (CDM→CM) |
| Liverpool | 2000003025 | Jayden Danns | — | — | ST; ST (người dùng cung cấp) |
| Liverpool | 2000003018 | Jeremie Frimpong | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jeremie-frimpong/253149) | RB; RM, RW | RB; RB, RM, RW |
| Liverpool | 2000003005 | Jeremy Jacquet | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jeremy-jacquet/278903) | CB | CB; CB |
| Liverpool | 2000003002 | Joe Gomez | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/joe-gomez/225100) | CB; RB, LB | CB; CB, RB, LB |
| Liverpool | 2000003012 | Kostas Tsimikas | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kostas-tsimikas/232223) | LB; LM | LB; LB, LM |
| Liverpool | 2000003023 | Lewis Koumas | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/lewis-koumas/70994) | LM; RM, LW, RW | LM; LM, RM, LW, RW |
| Liverpool | 2000003006 | Milos Kerkez | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/milos-kerkez/260908) | LB; LM | LB; LB, LM |
| Liverpool | 2000003024 | Rio Ngumoha | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/rio-ngumoha/80376) | LM; LW | LM; LM, LW |
| Liverpool | 2000003019 | Ronald Araujo | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ronald-araujo/253163) | CB; RB | CB; CB, RB |
| Liverpool | 2000003020 | Ryan Gravenberch | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ryan-gravenberch/246104) | CDM; CM | CM; CM (CDM→CM) |
| Liverpool | 2000003021 | Trey Nyoni | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/trey-nyoni/279128) | CM; CAM, CDM | CM; CM, CAM (CDM→CM) |
| Liverpool | 2000003014 | Victor Munoz | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/victor-munoz/76042) | LM; LW, RW, RM | LM; LM, LW, RW, RM |
| Liverpool | 2000003004 | Virgil van Dijk | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/virgil-van-dijk/203376) | CB | CB; CB |
| Liverpool | 2000030227 | Vitezslav Jaros | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/vitezslav-jaros/253428) | GK | GK; GK |
| Liverpool | 2000003003 | Wataru Endo | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/wataru-endo/232487) | CDM; CM | CM; CM (CDM→CM) |
| Liverpool | 2000030229 | Wellity Lucky | — | — | CB; CB (người dùng cung cấp) |
| Manchester City | 2000004021 | Abdukodir Khusanov | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/abdukodir-khusanov/277031) | CB | CB; CB |
| Manchester City | 2000004019 | Allan Andrade Elias | — | — | RM; RM, RW (người dùng cung cấp) |
| Manchester City | 2000004020 | Antoine Semenyo | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/antoine-semenyo/241236) | RW; LW, RM, LM | RW; RW, LW, RM, LM |
| Manchester City | 2000004017 | Ayyoub Bouaddi | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ayyoub-bouaddi/278901) | CDM; CM | CM; CM (CDM→CM) |
| Manchester City | 2000004003 | Elliot Anderson | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/elliot-anderson/254243) | CDM; CM | CM; CM (CDM→CM) |
| Manchester City | 2000004011 | Enzo Fernandez | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/enzo-fernandez/247090) | CM; CDM, CAM | CM; CM, CAM (CDM→CM) |
| Manchester City | 2000004007 | Erling Haaland | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/erling-haaland/239085) | ST | ST; ST |
| Manchester City | 2000030232 | Floyd Samba | — | — | CAM; CAM, CM (người dùng cung cấp) |
| Manchester City | 2000004016 | Geronimo Rulli | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/geronimo-rulli/215316) | GK | GK; GK |
| Manchester City | 2000004001 | Gianluigi Donnarumma | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/gianluigi-donnarumma/230621) | GK | GK; GK |
| Manchester City | 2000004005 | Iliman Ndiaye | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/iliman-ndiaye/261188) | LM; RM, LW, RW | LM; LM, RM, LW, RW |
| Manchester City | 2000004009 | Jeremy Doku | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jeremy-doku/246420) | LW; LM, RW, RM | LW; LW, LM, RW, RM |
| Manchester City | 2000004014 | Josko Gvardiol | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/josko-gvardiol/251517) | CB; LB | CB; CB, LB |
| Manchester City | 2000004024 | Kaden Braithwaite | — | — | CB; CB (người dùng cung cấp) |
| Manchester City | 2000004004 | Marc Guehi | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/marc-guehi/241159) | CB | CB; CB |
| Manchester City | 2000004010 | Marcus Bettinelli | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/marcus-bettinelli/204246) | GK | GK; GK |
| Manchester City | 2000004006 | Mateo Kovacic | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mateo-kovacic/207410) | CM; CDM, CAM | CM; CM, CAM (CDM→CM) |
| Manchester City | 2000004015 | Matheus Nunes | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/matheus-nunes/253124) | RB; CM | RB; RB, CM |
| Manchester City | 2000004018 | Nico O'Reilly | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/nico-o-reilly/277427) | LB; CM, CDM | LB; LB, CM (CDM→CM) |
| Manchester City | 2000004022 | Phil Foden | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/phil-foden/237692) | CAM; CM, RW, RM | CAM; CAM, CM, RW, RM |
| Manchester City | 2000004012 | Rayan Ait-Nouri | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/rayan-ait-nouri/242641) | LB; LM | LB; LB, LM |
| Manchester City | 2000004008 | Rayan Cherki | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/rayan-cherki/251570) | RW; RM, CAM, CM | RW; RW, RM, CAM, CM |
| Manchester City | 2000004025 | Rico Lewis | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/rico-lewis/271574) | RB; LB, CM, CDM | RB; RB, LB, CM (CDM→CM) |
| Manchester City | 2000004002 | Ruben Dias | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ruben-dias/239818) | CB | CB; CB |
| Manchester City | 2000004023 | Ryan McAidoo | — | — | RW; RW, RM (người dùng cung cấp) |
| Manchester City | 2000004013 | Vitor Reis | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/vitor-reis/76624) | CB | CB; CB |

## Phạm vi

`PlayerPositionCsvReader` hiện có đọc thành công 57 dòng, gồm kiểm tra mã, vị trí chính thuộc tập hợp và ID trùng. Đối chiếu roster hiệu lực 02/10/2026: 57 ID trong CSV thuộc đúng hai CLB, không có ID ngoài roster hoặc trùng. Không cần nhập H2 vì importer và schema không thay đổi trong lượt này.

Batch này chỉ chuẩn bị dữ liệu local. Chưa ghi Railway MySQL production, chưa sửa UI Fantasy, commit, push hoặc deploy.

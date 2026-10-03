# Arsenal và Manchester United — vị trí cầu thủ 2026/27

## Quy tắc nguồn

- Roster và `player_id`: hai CSV hồ sơ 02/10/2026, đối chiếu 54/54 ID với `backend/data/roster-2026-09-30/batch-*.csv` theo CLB. Membership PremierHub quyết định CLB; nhãn CLB trên EA không đổi ID hoặc membership.
- Nguồn vị trí cho các cầu thủ đã có hồ sơ: [EA SPORTS FC 27](https://www.ea.com/games/ea-sports-fc/ratings), dùng mục `Position` và `Alt Positions` trên từng hồ sơ được liên kết dưới đây. Max Dowman, Bendito Mantato và Tyler Fletcher dùng vị trí do người dùng cung cấp ngày 03/10/2026 vì chưa xác minh được từ hồ sơ EA; Luke Shaw chỉ giữ `LB` theo chỉ định riêng của người dùng.
- Chỉ lưu mã importer hỗ trợ: `GK LB CB RB CM CAM LM RM LW ST RW`. Theo quy tắc người dùng chốt, `CDM` EA ghi được quy đổi thành `CM`; nếu đã có `CM` thì chỉ lưu một lần. Riêng Luke Shaw không áp dụng vị trí phụ `CDM → CM` theo chỉ định mới. Cột nguồn bên dưới vẫn giữ mã EA gốc để phân biệt dữ liệu nguồn với quy tắc của PremierHub. Các vị trí phụ khác chỉ lấy từ hồ sơ từng cầu thủ hoặc chỉ định riêng nêu trên.
- `LCB/RCB` dùng quyền `CB`; `LCM/RCM` dùng quyền `CM`. Các mã ô trái/phải này không phải quyền phụ tự phát sinh.
- CSV chứa cả vị trí từ EA và vị trí người dùng cung cấp; hai cột `expected_*` trống để nhập lần đầu. Không thay đổi nhóm rộng, OVR, thống kê trận hoặc mùa 2024/25.

## Độ phủ

| CLB | Roster | Hoàn chỉnh trong CSV | MISSING |
| --- | ---: | ---: | ---: |
| Arsenal | 24 | 24 | 0 |
| Manchester United | 30 | 30 | 0 |
| Tổng | 54 | 54 | 0 |

## Vị trí người dùng bổ sung và điều chỉnh

| CLB | player_id | Cầu thủ | Vị trí lưu | Nguồn |
| --- | ---: | --- | --- | --- |
| Arsenal | 2000001025 | Max Dowman | `CAM`; `CAM, LM, RM, LW, RW` | Người dùng cung cấp 03/10/2026; lấy mã đầu tiên làm vị trí chính. |
| Manchester United | 2000030235 | Bendito Mantato | `ST`; `ST` | Người dùng cung cấp 03/10/2026. |
| Manchester United | 2000030236 | Tyler Fletcher | `CAM`; `CAM, CM` | Người dùng cung cấp 03/10/2026; lấy mã đầu tiên làm vị trí chính. |
| Manchester United | 2000006021 | Luke Shaw | `LB`; `LB` | Người dùng chỉ định 03/10/2026, loại vị trí phụ `CM` đã quy đổi trước đó. |

## Nguồn theo cầu thủ

| CLB | player_id | Cầu thủ | Hồ sơ EA FC 27 | EA: chính; phụ | Vị trí lưu trong CSV / ghi chú |
| --- | ---: | --- | --- | --- | --- |
| Arsenal | 2000001004 | Ben White | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/benjamin-white/231936) | RB; RM | RB, RM |
| Arsenal | 2000001022 | Bruno Guimaraes | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bruno-guimaraes/247851) | CM; CDM | CM |
| Arsenal | 2000001007 | Bukayo Saka | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bukayo-saka/246669) | RW; RM | RW, RM |
| Arsenal | 2000001014 | Christos Tzolis | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/christos-tzolis/256948) | LW; LM | LW, LM |
| Arsenal | 2000001003 | Cristhian Mosquera | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/cristhian-mosquera/264846) | CB; RB | CB, RB |
| Arsenal | 2000001001 | David Raya | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/david-raya/220901) | GK | GK |
| Arsenal | 2000001023 | Declan Rice | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/declan-rice/234378) | CDM; CM | CM (quy đổi CDM) |
| Arsenal | 2000001009 | Eberechi Eze | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/eberechi-eze/235794) | CAM; LW, CM, LM | CAM, LW, CM, LM |
| Arsenal | 2000001013 | Ezri Konsa | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ezri-konsa/227678) | CB | CB |
| Arsenal | 2000001006 | Gabriel Magalhaes | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/gabriel/232580) | CB | CB |
| Arsenal | 2000001019 | Illan Meslier | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/illan-meslier/242656) | GK | GK |
| Arsenal | 2000001010 | Jurrien Timber | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jurrien-timber/251805) | RB; LB, CB, RM | RB, LB, CB, RM |
| Arsenal | 2000001018 | Kai Havertz | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kai-havertz/235790) | ST; CAM, CM | ST, CAM, CM |
| Arsenal | 2000001011 | Kepa Arrizabalaga | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kepa/206585) | GK | GK |
| Arsenal | 2000001008 | Martin Odegaard | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/martin-degaard/222665) | CM; CAM | CM, CAM |
| Arsenal | 2000001021 | Martin Zubimendi | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/zubimendi/248148) | CDM; CM | CM (quy đổi CDM) |
| Arsenal | 2000001025 | Max Dowman | — | — | CAM, LM, RM, LW, RW (người dùng cung cấp) |
| Arsenal | 2000001017 | Mikel Merino | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mikel-merino/225193) | CM; ST, CAM | CM, ST, CAM |
| Arsenal | 2000001024 | Myles Lewis-Skelly | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/myles-lewis-skelly/278773) | LB; CDM, CM | LB, CM |
| Arsenal | 2000001015 | Noni Madueke | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/noni-madueke/254796) | RW; RM | RW, RM |
| Arsenal | 2000001005 | Piero Hincapie | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/piero-hincapie/256197) | LB; CB | LB, CB |
| Arsenal | 2000001020 | Riccardo Calafiori | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/riccardo-calafiori/257711) | LB; CB, CM | LB, CB, CM |
| Arsenal | 2000001012 | Viktor Gyokeres | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/viktor-gyokeres/241651) | ST | ST |
| Arsenal | 2000001002 | William Saliba | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/william-saliba/243715) | CB | CB |
| Manchester United | 2000006015 | Amad Diallo | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/amad/254088) | RM; RB, RW | RM, RB, RW |
| Manchester United | 2000006016 | Andrey Santos | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/andrey-santos/273018) | CM; CDM | CM |
| Manchester United | 2000006023 | Ayden Heaven | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ayden-heaven/75087) | CB | CB |
| Manchester United | 2000030235 | Bendito Mantato | — | — | ST (người dùng cung cấp) |
| Manchester United | 2000006024 | Benjamin Sesko | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/benjamin-sesko/260592) | ST | ST |
| Manchester United | 2000006008 | Bruno Fernandes | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bruno-fernandes/212198) | CAM; CM | CAM, CM |
| Manchester United | 2000006018 | Bryan Mbeumo | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bryan-mbeumo/243014) | RM; RW, ST | RM, RW, ST |
| Manchester United | 2000006019 | Carlos Baleba | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/carlos-baleba/272500) | CDM; CM | CM (quy đổi CDM) |
| Manchester United | 2000006002 | Diogo Dalot | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/diogo-dalot/234574) | RB; LB, RM, LM | RB, LB, RM, LM |
| Manchester United | 2000030233 | Harry Amass | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/harry-amass/273599) | LB; LM, CM, LW | LB, LM, CM, LW |
| Manchester United | 2000006005 | Harry Maguire | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/harry-maguire/203263) | CB | CB |
| Manchester United | 2000030234 | Jack Fletcher | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jack-fletcher/75439) | CM; CDM, CAM | CM, CAM |
| Manchester United | 2000006011 | Joshua Zirkzee | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/joshua-zirkzee/250961) | ST | ST |
| Manchester United | 2000006012 | Karl Darlow | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/karl-darlow/193331) | GK | GK |
| Manchester United | 2000006025 | Kobbie Mainoo | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kobbie-mainoo/269136) | CDM; CM | CM (quy đổi CDM) |
| Manchester United | 2000006014 | Leny Yoro | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/leny-yoro/269087) | CB | CB |
| Manchester United | 2000006006 | Lisandro Martinez | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/lisandro-martinez/239301) | CB | CB |
| Manchester United | 2000006021 | Luke Shaw | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/luke-shaw/205988) | LB; CDM | LB (người dùng chỉ định, bỏ CM phụ) |
| Manchester United | 2000006022 | Manuel Ugarte | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/manuel-ugarte/253306) | CDM; CM | CM (quy đổi CDM) |
| Manchester United | 2000006009 | Marcus Rashford | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/marcus-rashford/231677) | LW; LM, ST | LW, LM, ST |
| Manchester United | 2000006007 | Mason Mount | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mason-mount/233064) | CAM; LM, CM, LW | CAM, LM, CM, LW |
| Manchester United | 2000006010 | Matheus Cunha | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/matheus-cunha/240243) | LM; CAM, ST, LW | LM, CAM, ST, LW |
| Manchester United | 2000006004 | Matthijs de Ligt | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/matthijs-de-ligt/235243) | CB | CB |
| Manchester United | 2000006003 | Noussair Mazraoui | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/noussair-mazraoui/236401) | RB; CB | RB, CB |
| Manchester United | 2000006013 | Patrick Dorgu | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/patrick-dorgu/277432) | LM; LB | LM, LB |
| Manchester United | 2000006001 | Senne Lammens | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/senne-lammens/254803) | GK | GK |
| Manchester United | 2000006026 | Shea Lacey | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/shea-lacey/278124) | RM; RW | RM, RW |
| Manchester United | 2000006020 | Tom Heaton | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/tom-heaton/163264) | GK | GK |
| Manchester United | 2000030236 | Tyler Fletcher | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/tyler-fletcher/82899) | — | CAM, CM (người dùng cung cấp) |
| Manchester United | 2000006017 | Youri Tielemans | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/youri-tielemans/216393) | CM; CDM, CAM | CM, CAM |

Trang EA của Tyler Fletcher có URL trong batch hồ sơ cũ nhưng chưa mở được trực tiếp qua nguồn kiểm tra. Vị trí trong CSV của Tyler lấy từ chỉ định người dùng, không gán là đã xác minh qua EA.

## Số lựa chọn theo mã vị trí

Mỗi số là số **cầu thủ duy nhất** trong 54 người có mã đó trong `eligiblePositions`; một cầu thủ có thể được tính ở nhiều mã.

| Mã | Arsenal | Manchester United | Cộng |
| --- | ---: | ---: | ---: |
| GK | 3 | 3 | 6 |
| LB | 4 | 4 | 8 |
| CB | 7 | 6 | 13 |
| RB | 3 | 3 | 6 |
| CM | 9 | 10 | 19 |
| CAM | 5 | 6 | 11 |
| LM | 3 | 6 | 9 |
| RM | 5 | 4 | 9 |
| LW | 3 | 4 | 7 |
| ST | 3 | 6 | 9 |
| RW | 3 | 3 | 6 |

## Số lựa chọn cho từng ô Fantasy

Trong bảng dưới, con số là tổng Arsenal + Manchester United. Hai ô ST hoặc hai ô CB/CM cùng mã có cùng số ứng viên; một cầu thủ vẫn chỉ được chọn một lần trong đội hình.

| Sơ đồ | Từ hàng công tới thủ môn, từng ô (số ứng viên) |
| --- | --- |
| 4-2-1-3 | LW (7) · ST (9) · RW (6) / CAM (11) / LCM (19) · RCM (19) / LB (8) · LCB (13) · RCB (13) · RB (6) / GK (6) |
| 4-3-3 | LW (7) · ST (9) · RW (6) / LCM (19) · CM (19) · RCM (19) / LB (8) · LCB (13) · RCB (13) · RB (6) / GK (6) |
| 4-4-2 | ST (9) · ST (9) / LM (9) · LCM (19) · RCM (19) · RM (9) / LB (8) · LCB (13) · RCB (13) · RB (6) / GK (6) |
| 3-5-2 | ST (9) · ST (9) / LM (9) · LCM (19) · CM (19) · RCM (19) · RM (9) / LCB (13) · CB (13) · RCB (13) / GK (6) |

## Phạm vi kiểm tra

Phiên bản trước của batch đã được nhập thử trên H2 cô lập. Các điều chỉnh ngày 03/10/2026 ở trên chưa chạy lại test theo yêu cầu người dùng. Chưa ghi Railway MySQL production, chưa thay giao diện hoặc kiểm tra vị trí khi chọn đội Fantasy.

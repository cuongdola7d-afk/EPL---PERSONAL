# Arsenal và Manchester United — vị trí cầu thủ 2026/27

## Quy tắc nguồn

- Roster và `player_id`: hai CSV hồ sơ 02/10/2026, đối chiếu 54/54 ID với `backend/data/roster-2026-09-30/batch-*.csv` theo CLB. Membership PremierHub quyết định CLB; nhãn CLB trên EA không đổi ID hoặc membership.
- Nguồn **duy nhất cho vị trí chính và vị trí phụ**: hồ sơ cầu thủ [EA SPORTS FC 27](https://www.ea.com/games/ea-sports-fc/ratings). Dùng mục `Position` và `Alt Positions` trên từng hồ sơ được liên kết dưới đây. Đây là vị trí của thẻ cơ bản FC 27; không lấy vị trí từ nhóm FPL, SofaScore hoặc suy theo chân thuận.
- Chỉ lưu mã importer hỗ trợ: `GK LB CB RB CM CAM LM RM LW ST RW`. Mã `CDM` xuất hiện ở EA nhưng không có trong bốn sơ đồ/importer, nên không được đổi thành `CM`. Khi vị trí chính là `CDM`, giữ cả cầu thủ ở trạng thái `MISSING`, kể cả khi EA cũng ghi `CM` là vị trí phụ. Mã `CDM` phụ được bỏ khỏi tập lưu, còn mọi mã phụ hợp lệ được giữ nguyên theo từng người.
- `LCB/RCB` dùng quyền `CB`; `LCM/RCM` dùng quyền `CM`. Các mã ô trái/phải này không phải quyền phụ tự phát sinh.
- CSV chỉ chứa người đã xác minh; hai cột `expected_*` trống để nhập lần đầu. Người thiếu không có dòng vị trí, API trả `MISSING`. Không thay đổi nhóm rộng, OVR, thống kê trận hoặc mùa 2024/25.

## Độ phủ

| CLB | Roster | Hoàn chỉnh trong CSV | MISSING |
| --- | ---: | ---: | ---: |
| Arsenal | 24 | 21 | 3 |
| Manchester United | 30 | 25 | 5 |
| Tổng | 54 | 46 | 8 |

## MISSING cần bổ sung

| CLB | player_id | Cầu thủ | Lý do |
| --- | ---: | --- | --- |
| Arsenal | 2000001023 | Declan Rice | EA ghi vị trí chính `CDM`, importer chưa hỗ trợ. |
| Arsenal | 2000001021 | Martin Zubimendi | EA ghi vị trí chính `CDM`, importer chưa hỗ trợ. |
| Arsenal | 2000001025 | Max Dowman | Chưa có hồ sơ EA FC 27 để xác minh vị trí. |
| Manchester United | 2000030235 | Bendito Mantato | Chưa có hồ sơ EA FC 27 để xác minh vị trí. |
| Manchester United | 2000006019 | Carlos Baleba | EA ghi vị trí chính `CDM`, importer chưa hỗ trợ; membership vẫn thuộc Manchester United. |
| Manchester United | 2000006025 | Kobbie Mainoo | EA ghi vị trí chính `CDM`, importer chưa hỗ trợ. |
| Manchester United | 2000006022 | Manuel Ugarte | EA ghi vị trí chính `CDM`, importer chưa hỗ trợ. |
| Manchester United | 2000030236 | Tyler Fletcher | Không mở được trực tiếp hồ sơ EA FC 27 để xác minh. |

## Nguồn theo cầu thủ

| CLB | player_id | Cầu thủ | Hồ sơ EA FC 27 | EA: chính; phụ | Lưu trong CSV / lý do MISSING |
| --- | ---: | --- | --- | --- | --- |
| Arsenal | 2000001004 | Ben White | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/benjamin-white/231936) | RB; RM | RB, RM |
| Arsenal | 2000001022 | Bruno Guimaraes | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bruno-guimaraes/247851) | CM; CDM | CM |
| Arsenal | 2000001007 | Bukayo Saka | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bukayo-saka/246669) | RW; RM | RW, RM |
| Arsenal | 2000001014 | Christos Tzolis | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/christos-tzolis/256948) | LW; LM | LW, LM |
| Arsenal | 2000001003 | Cristhian Mosquera | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/cristhian-mosquera/264846) | CB; RB | CB, RB |
| Arsenal | 2000001001 | David Raya | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/david-raya/220901) | GK | GK |
| Arsenal | 2000001023 | Declan Rice | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/declan-rice/234378) | CDM; CM | EA ghi vị trí chính CDM; importer chưa hỗ trợ mã này |
| Arsenal | 2000001009 | Eberechi Eze | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/eberechi-eze/235794) | CAM; LW, CM, LM | CAM, LW, CM, LM |
| Arsenal | 2000001013 | Ezri Konsa | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/ezri-konsa/227678) | CB | CB |
| Arsenal | 2000001006 | Gabriel Magalhaes | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/gabriel/232580) | CB | CB |
| Arsenal | 2000001019 | Illan Meslier | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/illan-meslier/242656) | GK | GK |
| Arsenal | 2000001010 | Jurrien Timber | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jurrien-timber/251805) | RB; LB, CB, RM | RB, LB, CB, RM |
| Arsenal | 2000001018 | Kai Havertz | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kai-havertz/235790) | ST; CAM, CM | ST, CAM, CM |
| Arsenal | 2000001011 | Kepa Arrizabalaga | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kepa/206585) | GK | GK |
| Arsenal | 2000001008 | Martin Odegaard | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/martin-degaard/222665) | CM; CAM | CM, CAM |
| Arsenal | 2000001021 | Martin Zubimendi | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/zubimendi/248148) | CDM; CM | EA ghi vị trí chính CDM; importer chưa hỗ trợ mã này |
| Arsenal | 2000001025 | Max Dowman | — | — | Không có hồ sơ EA FC 27 |
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
| Manchester United | 2000030235 | Bendito Mantato | — | — | Không có hồ sơ EA FC 27 |
| Manchester United | 2000006024 | Benjamin Sesko | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/benjamin-sesko/260592) | ST | ST |
| Manchester United | 2000006008 | Bruno Fernandes | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bruno-fernandes/212198) | CAM; CM | CAM, CM |
| Manchester United | 2000006018 | Bryan Mbeumo | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/bryan-mbeumo/243014) | RM; RW, ST | RM, RW, ST |
| Manchester United | 2000006019 | Carlos Baleba | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/carlos-baleba/272500) | CDM; CM | EA ghi vị trí chính CDM; importer chưa hỗ trợ mã này |
| Manchester United | 2000006002 | Diogo Dalot | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/diogo-dalot/234574) | RB; LB, RM, LM | RB, LB, RM, LM |
| Manchester United | 2000030233 | Harry Amass | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/harry-amass/273599) | LB; LM, CM, LW | LB, LM, CM, LW |
| Manchester United | 2000006005 | Harry Maguire | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/harry-maguire/203263) | CB | CB |
| Manchester United | 2000030234 | Jack Fletcher | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/jack-fletcher/75439) | CM; CDM, CAM | CM, CAM |
| Manchester United | 2000006011 | Joshua Zirkzee | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/joshua-zirkzee/250961) | ST | ST |
| Manchester United | 2000006012 | Karl Darlow | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/karl-darlow/193331) | GK | GK |
| Manchester United | 2000006025 | Kobbie Mainoo | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/kobbie-mainoo/269136) | CDM; CM | EA ghi vị trí chính CDM; importer chưa hỗ trợ mã này |
| Manchester United | 2000006014 | Leny Yoro | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/leny-yoro/269087) | CB | CB |
| Manchester United | 2000006006 | Lisandro Martinez | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/lisandro-martinez/239301) | CB | CB |
| Manchester United | 2000006021 | Luke Shaw | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/luke-shaw/205988) | LB; CDM | LB |
| Manchester United | 2000006022 | Manuel Ugarte | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/manuel-ugarte/253306) | CDM; CM | EA ghi vị trí chính CDM; importer chưa hỗ trợ mã này |
| Manchester United | 2000006009 | Marcus Rashford | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/marcus-rashford/231677) | LW; LM, ST | LW, LM, ST |
| Manchester United | 2000006007 | Mason Mount | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/mason-mount/233064) | CAM; LM, CM, LW | CAM, LM, CM, LW |
| Manchester United | 2000006010 | Matheus Cunha | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/matheus-cunha/240243) | LM; CAM, ST, LW | LM, CAM, ST, LW |
| Manchester United | 2000006004 | Matthijs de Ligt | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/matthijs-de-ligt/235243) | CB | CB |
| Manchester United | 2000006003 | Noussair Mazraoui | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/noussair-mazraoui/236401) | RB; CB | RB, CB |
| Manchester United | 2000006013 | Patrick Dorgu | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/patrick-dorgu/277432) | LM; LB | LM, LB |
| Manchester United | 2000006001 | Senne Lammens | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/senne-lammens/254803) | GK | GK |
| Manchester United | 2000006026 | Shea Lacey | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/shea-lacey/278124) | RM; RW | RM, RW |
| Manchester United | 2000006020 | Tom Heaton | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/tom-heaton/163264) | GK | GK |
| Manchester United | 2000030236 | Tyler Fletcher | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/tyler-fletcher/82899) | — | Trang EA không truy cập trực tiếp để xác minh |
| Manchester United | 2000006017 | Youri Tielemans | [EA FC 27](https://www.ea.com/games/ea-sports-fc/ratings/player-ratings/youri-tielemans/216393) | CM; CDM, CAM | CM, CAM |

Trang EA của Tyler Fletcher có URL trong batch hồ sơ cũ nhưng hiện không mở được trực tiếp qua nguồn kiểm tra. Kết quả tìm kiếm EA bản địa hóa có gợi ý `MC/MCD`; batch này vẫn giữ `MISSING` vì chưa đọc được hồ sơ trực tiếp để chốt.

## Số lựa chọn theo mã vị trí

Mỗi số là số **cầu thủ duy nhất** trong 46 người hoàn chỉnh có mã đó trong `eligiblePositions`; một cầu thủ có thể được tính ở nhiều mã.

| Mã | Arsenal | Manchester United | Cộng |
| --- | ---: | ---: | ---: |
| GK | 3 | 3 | 6 |
| LB | 4 | 4 | 8 |
| CB | 7 | 6 | 13 |
| RB | 3 | 3 | 6 |
| CM | 7 | 6 | 13 |
| CAM | 4 | 5 | 9 |
| LM | 2 | 6 | 8 |
| RM | 4 | 4 | 8 |
| LW | 2 | 4 | 6 |
| ST | 3 | 5 | 8 |
| RW | 2 | 3 | 5 |

## Số lựa chọn cho từng ô Fantasy

Trong bảng dưới, con số là tổng Arsenal + Manchester United. Hai ô ST hoặc hai ô CB/CM cùng mã có cùng số ứng viên; một cầu thủ vẫn chỉ được chọn một lần trong đội hình.

| Sơ đồ | Từ hàng công tới thủ môn, từng ô (số ứng viên) |
| --- | --- |
| 4-2-1-3 | LW (6) · ST (8) · RW (5) / CAM (9) / LCM (13) · RCM (13) / LB (8) · LCB (13) · RCB (13) · RB (6) / GK (6) |
| 4-3-3 | LW (6) · ST (8) · RW (5) / LCM (13) · CM (13) · RCM (13) / LB (8) · LCB (13) · RCB (13) · RB (6) / GK (6) |
| 4-4-2 | ST (8) · ST (8) / LM (8) · LCM (13) · RCM (13) · RM (8) / LB (8) · LCB (13) · RCB (13) · RB (6) / GK (6) |
| 3-5-2 | ST (8) · ST (8) / LM (8) · LCM (13) · CM (13) · RCM (13) · RM (8) / LCB (13) · CB (13) · RCB (13) / GK (6) |

## Phạm vi kiểm tra

Batch này chỉ được nhập vào H2 cô lập để kiểm chứng. Chưa ghi Railway MySQL production, chưa thay giao diện hoặc kiểm tra vị trí khi chọn đội Fantasy.

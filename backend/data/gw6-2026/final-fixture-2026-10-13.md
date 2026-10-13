# GW6 2026/27 — Coventry City–Newcastle United, 13/10/2026

Fixture nội bộ `1000560597`, provider `560597`, CLB Coventry `1000001076` và Newcastle `1000000067`; kickoff `2026-10-12T19:00:00Z`, ngày membership/importer `2026-10-12`. football-data.org qua `FootballDataClient.download()` đã xác nhận FINISHED, Coventry thắng 4–1; mapping và cấu hình Render được đối chiếu trực tiếp với Aiven `defaultdb` UUID `715946cc-c33a-11f1-8fd9-8e0831c19405`.

## Nguồn và xác nhận

- Phút, bàn, kiến tạo, thẻ: [trang trận StatMuse](https://www.statmuse.com/fc/match/-112820), payload trận đã kết thúc, đúng GW6/2026–27 và tỉ số 4–1. Dùng chỉ số Player Stats; không lấy rating StatMuse hoặc tự tính 90 trừ thời điểm thay người.
- Rating, danh sách đủ hai đội, vai trò/thay người và vị trí sân: hai ảnh SofaScore người dùng gửi ngày 13/10/2026, một ảnh sân và một ảnh Substitutions. Nguồn đội hình dùng `USER_SCREENSHOT: ...`, không yêu cầu/bịa URL SofaScore. Cả hai đội có sơ đồ 4-2-3-1 thực tế, mỗi đội 11 STARTER, 5 SUB_USED, 4 SUB_UNUSED; ID roster và nguồn trận khớp.
- Dan Burn `18961` vào sân phút 90 thay Bazoumana Touré; StatMuse ghi 1 phút. Người dùng xác nhận rõ ngày 13/10/2026 rằng Dan Burn không được SofaScore chấm. Giữ PLAYED, rating/fantasy_points NULL; thêm đúng cặp fixture/player vào `confirmed-unrated.csv`, không gán rating 0.
- SUB_UNUSED: ảnh danh sách dự bị + danh sách thay người đầy đủ; tất cả bàn/kiến tạo/thẻ của đội đã được giải thích bởi PLAYED và không có sự kiện mâu thuẫn cho dự bị. Rating NULL, minutes/goals/assists/yellow_cards/red_cards=0, fantasy_points=0.
- Jacob Murphy `2000030076` đã có ID/dòng mùa Newcastle và vị trí FORWARD. Khoảng cũ: 23/08→24/08, 29/08→30/08, 05/09→06/09, 14/09→15/09, 19/09→20/09 (tất cả 2026, cùng CLB). Ảnh và danh sách trận xác nhận vẫn thuộc Newcastle: thêm khoảng mở từ ngày trận `2026-10-12`, end_date NULL, không suy ngày chuyển nhượng hoặc kéo lùi đầu mùa; giữ năm khoảng cũ và ID. 39 người còn lại đã có membership hợp lệ; không thay membership của họ.
- Bốn đội Fantasy đã chốt không có lượt chọn Coventry/Newcastle theo snapshot đọc trước nhập. Giữ sáu xác nhận chấn thương ngoài danh sách của chín trận trước và năm xác nhận unrated toàn GW; không tạo thêm dòng vắng mặt suy đoán.

## Dữ liệu đã chuẩn bị — trạng thái nhập ở checkpoint chính

Trận cuối: 40 dòng = 32 PLAYED (31 rating + Dan Burn không chấm) và 8 DID_NOT_PLAY. Toàn GW: 400 dòng thực tế = 315 PLAYED, 85 DID_NOT_PLAY, 310 PLAYED có rating, 5 PLAYED confirmed-unrated. Bộ đội hình có 20 đội/220 STARTER/95 SUB_USED/85 SUB_UNUSED; 20 sơ đồ đã xác minh. Thêm dữ liệu mới vào CSV cuối hiện có, giữ từng giá trị của chín trận đã nhập; không sửa GW1–5 hoặc mùa 2024/25.

| CLB | Player ID | Tên đang lưu | Vai trò | Rating | Phút | Bàn | Kiến tạo | Vàng | Đỏ |
| --- | --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: |
| Coventry | 2000002016 | Carl Rushworth | STARTER | 6.5 | 90 | 0 | 0 | 0 | 0 |
| Coventry | 2000002002 | Jay Dasilva | STARTER | 6.8 | 90 | 0 | 1 | 0 | 0 |
| Coventry | 2000002001 | Ethan Pinnock | STARTER | 6.1 | 90 | 0 | 0 | 0 | 0 |
| Coventry | 2000002003 | Bobby Thomas | STARTER | 8.3 | 90 | 0 | 0 | 0 | 0 |
| Coventry | 2000002024 | Milan van Ewijk | STARTER | 7.2 | 90 | 0 | 0 | 1 | 0 |
| Coventry | 2000002005 | Matt Grimes | STARTER | 6.6 | 82 | 0 | 0 | 0 | 0 |
| Coventry | 2000002007 | Caleb Yirenkyi | STARTER | 6.9 | 77 | 0 | 0 | 0 | 0 |
| Coventry | 2000002009 | Ephron Mason-Clark | STARTER | 6.2 | 60 | 0 | 0 | 0 | 0 |
| Coventry | 2000002004 | Jack Rudoni | STARTER | 7.5 | 82 | 1 | 0 | 0 | 0 |
| Coventry | 2000002020 | Brandon Thomas-Asante | STARTER | 7.3 | 90 | 0 | 1 | 0 | 0 |
| Coventry | 2000002029 | Sidiki Cherif | STARTER | 7.5 | 77 | 1 | 0 | 0 | 0 |
| Coventry | 2000002006 | Tatsuhiro Sakamoto | SUB_USED | 6.7 | 30 | 0 | 0 | 0 | 0 |
| Coventry | 2000002008 | Ellis Simms | SUB_USED | 7.5 | 13 | 1 | 0 | 0 | 0 |
| Coventry | 2000002011 | Stephen Mfuni | SUB_USED | 8.0 | 8 | 1 | 0 | 0 | 0 |
| Coventry | 2000002014 | Frank Onyeka | SUB_USED | 6.5 | 13 | 0 | 0 | 0 | 0 |
| Coventry | 2000002019 | Joel Latibeaudiere | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| Coventry | 2000002022 | Dan Bentley | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| Coventry | 2000002026 | Victor Torp | SUB_USED | 6.9 | 8 | 0 | 0 | 0 | 0 |
| Coventry | 2000002027 | Yann Gboho | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| Coventry | 2000002028 | Gustavo Hamer | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| Newcastle | 2000030074 | Lukáš Horníček | STARTER | 5.4 | 90 | 0 | 0 | 0 | 0 |
| Newcastle | 2000030128 | Lewis Hall | STARTER | 6.0 | 90 | 0 | 0 | 0 | 0 |
| Newcastle | 2000030130 | Sven Botman | STARTER | 7.5 | 90 | 1 | 0 | 0 | 0 |
| Newcastle | 2000030131 | Malick Thiaw | STARTER | 6.2 | 90 | 0 | 0 | 0 | 0 |
| Newcastle | 2000030078 | Lewis Miley | STARTER | 6.5 | 58 | 0 | 0 | 0 | 0 |
| Newcastle | 2000030071 | Joe Willock | STARTER | 6.2 | 58 | 0 | 0 | 0 | 0 |
| Newcastle | 2000030106 | Nico González | STARTER | 7.3 | 90 | 0 | 1 | 0 | 0 |
| Newcastle | 2000030129 | Harvey Barnes | STARTER | 6.3 | 90 | 0 | 0 | 0 | 0 |
| Newcastle | 2000030072 | Yoane Wissa | STARTER | 6.3 | 77 | 0 | 0 | 0 | 0 |
| Newcastle | 2000030076 | Jacob Murphy | STARTER | 6.4 | 58 | 0 | 0 | 0 | 0 |
| Newcastle | 2000030238 | Matias Fernandez-Pardo | STARTER | 7.1 | 90 | 0 | 0 | 0 | 0 |
| Newcastle | 723 | Joelinton | SUB_USED | 6.3 | 32 | 0 | 0 | 1 | 0 |
| Newcastle | 18911 | Nick Pope | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| Newcastle | 18961 | Dan Burn | SUB_USED | NULL | 1 | 0 | 0 | 0 | 0 |
| Newcastle | 2000030073 | William Osula | SUB_USED | 6.5 | 13 | 0 | 0 | 0 | 0 |
| Newcastle | 2000030075 | Fabian Schär | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| Newcastle | 2000030077 | Aladji Bamba | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| Newcastle | 2000030080 | Sean Steur | SUB_UNUSED | NULL | 0 | 0 | 0 | 0 | 0 |
| Newcastle | 2000030081 | Amar Dedić | SUB_USED | 6.2 | 32 | 0 | 0 | 0 | 0 |
| Newcastle | 2000030134 | Bazoumana Touré | SUB_USED | 7.4 | 31 | 0 | 0 | 0 | 0 |

## Kết quả nhập và kiểm chứng — 13/10/2026

- Datasource Render đã đối chiếu đúng Aiven `defaultdb`, UUID nêu trên; kết nối TLS kiểm tra CA/hostname. Không dùng Railway.
- Backup trước ghi: `backend/local-backups/gw6-final-20261013-111429/aiven-before-gw6-final-20261013-111429.sql`, 808892 byte, SHA-256 `7a04778c207a7f6327986eceeb9bb40264c4461260e9c9425b4a5843105e5a25`. Đã kiểm tra 19 bảng, DDL, số dòng, marker hoàn tất, file đọc được và Git ignore; chưa thử restore. Hoàn tất UTC `2026-10-13T11:14:57`.
- Local trên bản sao dữ liệu đạt reader/importer/statistics/lineup/evidence/totals/readiness. Không sửa mã ứng dụng, không chạy full suite/build.
- `FootballDataSync`: cập nhật đúng một fixture hiện hữu FINISHED 4–1 và 20 hàng BXH mùa 2026; không tạo fixture/CLB mới.
- Membership: thêm một khoảng Jacob Murphy. Thống kê: thêm 40 dòng. Đội hình: thêm 2 metadata CLB, 4 thay đổi fixture (2 trạng thái đá chính + 2 sơ đồ), 40 vai trò. Fantasy evidence: thêm 1 xác nhận CSV cuối và 1 unrated; sáu căn cứ vắng mặt giữ nguyên. Chạy lại từng importer, tất cả thay đổi bằng 0.
- Tổng mùa được service hiện có cập nhật tự động: 315 dòng tổng mùa thay đổi so với trước nhập, đối chiếu MySQL khớp PLAYED thực tế GW1–6; 593 dòng mùa, 60 fixture, 2400 dòng trận. Tái tính lại hai lần đều `updatedRows=0`. NULL cũ chưa xác minh (1 minutes, 2 goals, 4 assists) giữ nguyên.
- MySQL khớp CSV cả 400 dòng, 20 đội hình/220 STARTER/95 SUB_USED/85 SUB_UNUSED, từng chỉ số/rating/vai trò/thay người. Chín fixture trước, thống kê GW1–5, khoảng membership cũ, danh tính và mùa 2024/25 không thay đổi. Bốn đội Fantasy, draft picks, deadline, rosterAsOf và kết quả công bố giữ nguyên.
- API qua Vercel: cả 10 trận trả đúng 11–11 đá chính, đủ dự bị/vị trí/thay người và thống kê. 40 cầu thủ trận cuối có lịch sử GW6; 163 payload GW1–5 của họ giống trước nhập. List/detail hiện Jacob Murphy đúng Newcastle. Dan Burn PLAYED, 1 phút, rating/fantasyPoints NULL; 8 SUB_UNUSED rating NULL/điểm 0.
- Chrome production 1440px/390px: sân trận cuối có 22 đúng ID đá chính, hai sơ đồ 4-2-3-1, mỗi đội 5 dự bị vào sân + 4 không vào sân; không chồng lấn, tràn ngang, lỗi JavaScript hoặc request ghi. Chín sân trước đã kiểm tra trong checkpoint cũ; API cả 10 kiểm tra lại trong lượt này.

## Readiness và bước cần deploy

Mã hiện tại chạy trên Aiven trả `ready=true`, 10 fixture, 4 người chơi, `currentVersion=0`, 0 issue và lịch sử công bố rỗng. Không gọi publish/recompute kết quả Fantasy.

API ADMIN qua Vercel, tài khoản 2 vừa đăng nhập, UTC `2026-10-13T11:24:58`: `ready=false`, còn 16 issue: 8 `SELECTED_STAT_MISSING` và 8 `PLAYER_MATCH_UNRESOLVED`. Không còn lỗi fixture pending, CSV cuối, lineup hoặc unrated. Render đang chạy `0b3fd0eb4a1f6401dd412368d064e6eea2a22fee`, chưa chứa `e3ef0f4` xử lý sáu căn cứ chấn thương đã nhập. Mỗi lượt chọn trong bảng dưới bị hai cảnh báo; không tạo thống kê giả để lách mã cũ.

| Fixture | Player ID | Cầu thủ | Account ID bị ảnh hưởng |
| --- | --- | --- | --- |
| 1000560595 | 2000005017 | Reece James | 5 |
| 1000560596 | 2000020057 | Nikola Milenković | 2 |
| 1000560596 | 2000030244 | Nicolò Savona | 1 |
| 1000560598 | 2000003008 | Dominik Szoboszlai | 2, 3 |
| 1000560598 | 2000003009 | Alexander Isak | 2, 3 |
| 1000560601 | 2000030151 | James Wright | 5 |

**Cần deploy backend Render** phiên bản chứa `e3ef0f4` (HEAD `0cca281` đã chứa), rồi kiểm tra readiness API ADMIN về ready=true trước khi ADMIN tự công bố. Frontend không cần sửa/deploy. Không còn trận pending hoặc dữ liệu GW6 cần thu thập thêm. Chưa deploy, commit, push hoặc công bố.

Báo cáo thô/ảnh kiểm chứng ở `backend/target/gw6-final/` được Git ignore; checkpoint và CSV trong `backend/data/gw6-2026/` là phần cần commit. Commit message đề xuất: `data(gw6): complete Coventry-Newcastle stats and Fantasy evidence`.

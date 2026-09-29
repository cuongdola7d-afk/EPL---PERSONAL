# GW1 2026/27 — năm fixture còn lại, batch Lineups local

Kiểm tra ngày **2026-09-29**. Fixture/club ID, ngày và tỉ số lấy từ H2 local. Nguồn cầu thủ và thống kê là **ảnh Lineups SofaScore do người dùng cung cấp** cho đúng năm trận, cùng diễn biến SofaScore đã ghi ở phiên trước. Không sử dụng nguồn điểm hoặc thống kê từ nhà cung cấp khác. CSV roster có 7 cột; CSV cầu thủ–trận có đúng 10 cột.

| Fixture | Trận | `PLAYED` | `DID_NOT_PLAY` | Rating `NULL` của `PLAYED` | Phút `NULL` của `PLAYED` |
| --- | --- | ---: | ---: | ---: | ---: |
| `1000560547` | Brentford 3–0 Tottenham, 22/08 | 32 | 8 | 0 | 32 |
| `1000560548` | Man City 2–1 Bournemouth, 23/08 | 30 | 10 | 1 | 29 |
| `1000560549` | Brighton 4–0 Aston Villa, 23/08 | 32 | 8 | 0 | 31 |
| `1000560550` | Newcastle 2–2 Liverpool, 23/08 | 31 | 9 | 1 | 31 |
| `1000560551` | Fulham 2–3 Chelsea, 24/08 | 31 | 9 | 0 | 31 |
| **Tổng** | | **156** | **44** | **2** | **154** |

Mỗi ảnh gồm đội hình chính và danh sách thay người/dự bị của hai CLB. **Mỗi fixture có đúng 40 người trong CSV**; `DID_NOT_PLAY` chỉ dành cho người được ảnh ghi ở danh sách dự bị nhưng không có ký hiệu đã vào sân. Importer lưu phút và điểm Fantasy bằng `0` cho các dòng này; rating và những chỉ số không áp dụng vẫn trống. Với `PLAYED`, chỉ rating thấy rõ trong ảnh được điền. Không có ô số 0 nào được suy ra từ chỗ trống.

## Đối chiếu và giới hạn còn lại

- Ảnh Lineups xác nhận Jack Hinshelwood **9.0** (`1000560549`), ưu tiên ảnh trước [bài SofaScore có hai số mâu thuẫn](https://www.sofascore.com/news/brighton-4-0-aston-villa-hinshelwood-brace-headlines-a-dominant-premier-league-opener). Cùng nguyên tắc ảnh áp dụng cho Senesi **7.6**, Gvardiol **7.8**, Dunk **7.7** và Palmer **9.0**. Hai người vào sân quá muộn không có rating trong ảnh: **Ben Gannon-Doak** (`1000560548`) và **Fabian Schär** (`1000560550`); rating và điểm của họ giữ `NULL`.
- Ảnh Lineups cho biết **thời điểm thay người**, nhưng không hiển thị số phút thi đấu thống kê cho từng người. Không suy ra phút từ đồng hồ thay người hoặc thời gian bù giờ. Chỉ giữ **90 phút của Marc Guéhi** và **64 phút của Jack Hinshelwood** đã được bài SofaScore đúng trận xác nhận; 154 ô phút `PLAYED` khác còn `NULL`.
- Bàn thắng, kiến tạo và thẻ dương đã có từ [diễn biến Brentford–Tottenham](https://www.sofascore.com/football/match/brentford-tottenham-hotspur/Isab), [Man City–Bournemouth](https://www.sofascore.com/football/match/bournemouth-manchester-city/rkb), [Brighton–Villa](https://www.sofascore.com/football/match/aston-villa-brighton-and-hove-albion/FP), [Newcastle–Liverpool](https://www.sofascore.com/football/match/liverpool-fc-newcastle-united/OU) và [Fulham–Chelsea](https://www.sofascore.com/football/match/fulham-chelsea/NsT) được giữ nguyên. Ô `goals`, `assists` hoặc thẻ còn trống chưa được coi là `0`; muốn hoàn thiện cần ảnh Stats chi tiết hoặc bằng chứng rõ của chính SofaScore. Bàn đầu Brighton là **phản lưới Victor Lindelöf**: không ghi bàn thắng cầu thủ cho anh. Thẻ vàng bị VAR đổi thành đỏ của João Gomes chưa được dùng để suy ra số thẻ vàng cuối cùng; chỉ ghi thẻ đỏ `1`.
- ID đã có trong roster repo hoặc CSV trước được giữ nguyên. 51 ID mới bắt đầu từ `2000030094`, không tái sử dụng ID cũ. Các membership mới chỉ có hiệu lực `[ngày trận, ngày kế tiếp)`. [Hồ sơ Enzo Fernández](https://www.sofascore.com/football/player/enzo-fernandez/974505) xác nhận anh thuộc Chelsea ở GW1 trước khi chuyển Man City ngày 01/09; dòng Chelsea giữ ID `2000004011`. Tên **Jamie Gittens** trong ảnh là cùng người đã có ID với tên lưu **Jamie Bynoe-Gittens**, xác nhận bởi [hồ sơ SofaScore](https://www.sofascore.com/football/player/jamie-bynoe-gittens/1140599). Vị trí của cầu thủ mới/ít xuất hiện được đối chiếu trên hồ sơ và trang đội SofaScore, gồm [Luca Williams-Barnett](https://www.sofascore.com/football/player/luca-williams-barnett/1471591), [Triston Rowe](https://www.sofascore.com/football/player/triston-rowe/1400655), [Bradley Burrowes](https://www.sofascore.com/football/player/bradley-burrowes/1899712), [Luka Lynch](https://www.sofascore.com/football/player/luka-lynch/1656223) và [Leo Shahar](https://www.sofascore.com/football/player/leo-shahar/1479682).

[`*-missing.csv`](manual-match-stats-2026-GW1-last-five-LOCAL-missing.csv) liệt kê từng ô còn `NULL` của người `PLAYED`; ảnh đội hình của cả năm trận **đã có đủ**, phần còn thiếu là rating của hai người nêu trên và các chỉ số cá nhân ảnh không hiển thị.

## Kiểm tra local

Tạo H2 cô lập `backend/target/gw1-last-five-lineups-20260929.mv.db` từ bản local, nhập ba roster Man City/Liverpool/Chelsea đã phát hành, sau đó nhập CSV roster batch này. Importer thêm **144 player, 145 cặp player–club và 200 khoảng**; chạy roster lần hai thêm **0**. Importer thống kê thêm **200** dòng, lần hai thêm **0**. Đọc lại H2: năm fixture đều có **40** dòng; không trùng `(fixture_id, player_id)`, không có membership sai ngày, `PLAYED` không bị ghi phút `0`, `DID_NOT_PLAY` có rating `NULL` và điểm `0`, 400 thống kê thô 2024/25 vẫn còn.

Đây là dữ liệu **local**, chưa nhập MySQL production. Importer hiện báo xung đột khi cùng khóa có nội dung khác. Bổ sung các ô `NULL` sau này cần một quy trình cập nhật có kiểm chứng; không nhập đè CSV mới và giả định importer sẽ tự sửa bản ghi.

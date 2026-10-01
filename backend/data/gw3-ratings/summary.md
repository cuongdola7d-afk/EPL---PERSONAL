# Rating GW3 2026/27 từ ảnh người dùng

Người dùng gửi ảnh đội hình và dự bị của cả 10 fixture GW3. Rating được chép trực tiếp từ ảnh vào hai file `*-RATINGS.csv`; sau khi người dùng xác nhận sáu cầu thủ vào sân quá ít phút không được chấm, hai file thống kê và hai file roster không có hậu tố `DRAFT` là bản đã chốt để xem xét nhập sau. Các file `*-DRAFT.csv` ban đầu được giữ nguyên. Không tra rating từ SofaScore hay StatMuse.

| Batch | Người PLAYED | Có rating trong ảnh | Được xác nhận không chấm |
| --- | ---: | ---: | ---: |
| `gw3-first-five` | 150 | 148 | 2 |
| `gw3-last-five` | 157 | 153 | 4 |
| Tổng | 307 | 301 | 6 |

Sáu người không được chấm được ghi đúng fixture, CLB, `player_id` và tên trong `confirmed-unrated.csv`. Cả sáu vẫn `PLAYED`, giữ `rating=NULL` và `fantasy_points=NULL`; không thay bằng 0. Không còn rating cần xin thêm. Người `DID_NOT_PLAY` vẫn có rating trống và điểm Fantasy 0 theo importer.

## Kiểm tra trên H2 cô lập

Bản H2 mới sao chép từ trạng thái sau GW2. Đã nhập roster của hai batch GW3 trước, rồi nhập hai CSV `*-RATINGS.csv`: mỗi CSV thêm 200 dòng; nhập lại từng CSV đều thêm 0. API chi tiết của cả 10 fixture trả 20 người mỗi đội và khớp từng rating, điểm Fantasy, trạng thái, phút, bàn, kiến tạo, thẻ trong CSV. 301 người có rating nhận điểm Fantasy đúng bằng rating đã lưu; sáu người nêu trên giữ hai giá trị `NULL`. Hai file thống kê đã chốt là bản sao byte-for-byte của `*-RATINGS.csv` đã thử trên H2.

Chưa ghi MySQL production, commit, push hoặc deploy; không sửa mùa 2024/25.

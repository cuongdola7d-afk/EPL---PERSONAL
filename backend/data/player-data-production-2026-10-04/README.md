# Phát hành dữ liệu cầu thủ 2026/27 — 04/10/2026

## Đầu vào cuối và nguồn

`input-manifest.json` liệt kê chính xác CSV và ghi chú đã đọc cùng SHA-256. Chỉ đọc dữ liệu local đã chốt; không thu thập lại nguồn. Hai mươi CSV hồ sơ 02/10 là snapshot nền; bản bổ sung 02/10 được áp dụng trước các batch cuối 03/10. CSV Brighton bổ sung thay giá trị cuối của ba ID; vị trí James Rowswell dùng file bổ sung riêng. Không nhập DRAFT/RATINGS hoặc chạy snapshot cũ sau bổ sung.

- profiles-final.csv: 534 hồ sơ hợp nhất theo ID/CLB hiện hành.
- positions-final.csv: 532 bộ vị trí cuối; không tạo dòng vị trí giả cho hai người MISSING.
- profile-updates-expected.csv: 15 ô cần sửa, có giá trị cũ và mới, tên/ID/CLB và nguồn. Ô expected trống là SQL NULL; Gonzalo nationality chỉ được đổi từ D Mallorca Yo sang Spain theo ghi chú đã chốt.
- club-coverage.csv / production-results.json: kết quả đọc MySQL và API sau nhập.
- missing-fields.txt: các trường còn NULL/MISSING trên production.

Primary/eligible giữ nguyên CSV và ngoại lệ người dùng; chỉ CDM → CM theo batch đã chốt. Dowman OVR 72, Luke Shaw chỉ LB, Carlos Baleba giữ membership MU. Không ghi nhãn EA đã xác minh cho giá trị do người dùng đặt.

## Xác nhận production và sao lưu

Railway production, service EPL---PERSONAL, database railway; đã đối chiếu biến backend với MySQL và proxy local mà không in credential. Schema hồ sơ/vị trí và các trường API có sẵn; không cần phát hành mã trước. Commit Railway và thông tin sao lưu SQL mới nằm trong production-results.json. Bản sao lưu nằm backend/local-backups/, được Git ignore, có SHA-256 và đủ schema/data trước ghi.

## Cách nhập và kết quả

Importer hồ sơ hiện có chỉ chèn, không có chế độ patch profile-updates.csv. Dùng quy trình SQL có điều kiện đã áp dụng ngày 02/10, khóa đúng hồ sơ/membership và so sánh giá trị cũ/mới, để sửa 15 ô đã review. Sau đó gọi PlayerProfileImporter và PlayerPositionImporter hiện có trên hai CSV cuối trong cùng transaction. Bất kỳ xung đột nào rollback toàn bộ. Wrapper thao tác local và log lưu ngoài Git; không thêm API, dependency hay mã importer mới.

- Trước nhập: 534 hồ sơ đã có; 111 bộ vị trí đã có.
- Lần 1: hồ sơ thêm 0, cập nhật 15 ô; vị trí thêm 421, cập nhật 0.
- Lần 2, đúng các đầu vào trên: hồ sơ thêm 0/cập nhật 0; vị trí thêm 0/cập nhật 0.
- Sau nhập: 534 hồ sơ; 533 đủ năm trường; 531 OVR số; 532 primary/tập hợp hợp lệ; 989 dòng eligible.
- 531 cầu thủ đủ điều kiện dữ liệu để chọn Fantasy (có OVR hợp lệ và primary trong eligible). Mỗi đội vẫn cần đúng ô, 11 người khác nhau, tối đa 3/CLB, tổng OVR <= 860; không coi 531 người là một đội hợp lệ.
- Wellity Lucky có CB/CB nhưng OVR vẫn NULL đúng CSV; không tự cấp OVR. Mahdi Nicoll-Jazuli và George Shepherd thiếu cả OVR lẫn vị trí. Danh sách chính xác tại missing-fields.txt.
- MySQL đối chiếu từng trường của 534 hồ sơ và 532 bộ vị trí; API danh sách đối chiếu 534 người theo ngày luật Fantasy đang dùng (02/10), API chi tiết kiểm tra 18 mẫu và ngoại lệ theo 04/10.
- Fingerprint players, memberships, player_season_stats, manual_fixture_player_stats, fixtures và dữ liệu hồ sơ/vị trí ngoài mùa 2026 giữ nguyên. Không sửa mùa 2024/25 hoặc luật Fantasy.

## Kiểm tra và phát hành

Hai reader hiện có đọc CSV hợp nhất đạt: profiles=534, positions=532; kiểm tra ID/CLB, khóa trùng, mã và primary trong eligible đạt. Không tạo test Java riêng, nhập H2, chạy full test/build vì không sửa mã/schema.

Chỉ thay dữ liệu MySQL và thêm tài liệu/CSV tổng hợp local. API đọc trực tiếp dữ liệu mới, không cần deploy để dữ liệu hiện trên web. Không commit, push hoặc deploy. Các batch nguồn đã commit giữ nguyên.

Commit message đề xuất:

```text
data(players): record production import of 2026 profiles and positions
```

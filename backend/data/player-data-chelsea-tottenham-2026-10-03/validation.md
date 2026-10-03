# Kiểm tra batch Chelsea–Tottenham 2026/27

Ngày chuẩn bị: 2026-10-03. Code chung tại working tree/commit `e53890c`.
Lượt này chỉ thêm dữ liệu local; không sửa Java/JavaScript/schema, không tạo test
Java riêng, không chạy toàn bộ test hoặc build. Các file batch hồ sơ cũ được giữ nguyên.

## Đối chiếu nguồn dữ liệu đã lưu

- API production `season=2026&asOf=2026-10-03`, lọc bằng đúng tên CLB
  `Chelsea FC` và `Tottenham Hotspur FC`: 27 + 29 người, 56 ID duy nhất.
- So với API `asOf=2026-10-02` và hai CSV hồ sơ cũ: cùng 56 cặp `player_id,club_id`,
  không thiếu/thừa người, không giới hạn 25 người.
- `profiles.csv`: 56 dòng, đúng một dòng mỗi ID roster, sáu trường khớp API đang lưu.
  Năm trường hồ sơ mỗi CLB đủ 100%; 55 OVR số và một OVR NULL (Mahdi).
- Khác biệt duy nhất với hai CSV gốc là James Rowswell `fc27_overall` từ trống
  thành **61**, đã có trong API và file bổ sung của người dùng ngày 02/10/2026.
  CSV batch mới giữ dữ liệu này; không thay thế bằng NULL hoặc gắn nguồn EA cho 61.
- `positions.csv`: 54 ID đều thuộc roster nói trên, 26 Chelsea + 28 Tottenham.
  Chính/phụ lấy từ hồ sơ EA FC 27 ghi trong `sources.md`; chỉ CDM → CM và gộp mã trùng.
- `roster-status.csv`: 56 dòng/56 ID duy nhất; 54 VERIFIED và hai MISSING,
  ô vị trí của Mahdi/James để trống. Hai người này không nằm trong file nhập vị trí.
- Importer vị trí không nhận dòng primary/eligible mới trống. Không sửa importer
  để bỏ qua dòng lỗi; tách danh sách MISSING khỏi file có thể nhập.

## Chạy reader/importer chung trên H2 cô lập

Dùng JAR backend đã có tại `backend/target/premierhub-backend-0.1.0-SNAPSHOT.jar`.
DB local: `backend/target/chelsea-tottenham-data/check-db.mv.db`, không dùng cấu hình
MySQL hay `.env.local`. Mỗi lần gọi đều ghi rõ URL H2, username `sa`, password trống,
`spring.sql.init.mode=never`, `spring.main.web-application-type=none`.

DB được dựng riêng từ schema hiện có; seed ID/CLB/nhóm rộng, hồ sơ và số goals/assists
đã đọc từ response API của 56 người. Membership local chỉ mô phỏng hiệu lực ngày
03/10/2026 từ kết quả roster; ngày bắt đầu mô phỏng không phải ngày chuyển nhượng thật.
Ban đầu có 56 hồ sơ, không có vị trí của hai CLB. Không đọc/ghi MySQL để seed H2.

Các reader và importer hiện có chịu trách nhiệm kiểm tra: ID, membership trong mùa/CLB,
ngày/chiều cao/chân thuận/số áo/OVR, mã vị trí, khóa trùng, primary nằm trong eligible,
xung đột và tính lặp lại của thao tác. Không thêm bộ kiểm tra Java cho batch này.

| File | Lần | Rows | Inserted | Updated |
| --- | ---: | ---: | ---: | ---: |
| profiles.csv | 1 | 56 | 0 | Không có thao tác cập nhật |
| profiles.csv | 2 | 56 | 0 | Không có thao tác cập nhật |
| positions.csv | 1 | 54 | 54 | 0 |
| positions.csv | 2 | 54 | 0 | 0 |

Hồ sơ thêm 0 ở cả hai lần vì CSV khớp dữ liệu đang có. Vị trí nhập lần đầu 54,
nhập lại thêm/cập nhật 0. Tất cả bốn lệnh kết thúc thành công.

## Đọc lại H2

| CLB | Hồ sơ | Mỗi trường SofaScore | OVR có số | Primary | MISSING vị trí |
| --- | ---: | ---: | ---: | ---: | ---: |
| Chelsea | 27 | 27 | 26 | 26 | 1 |
| Tottenham | 29 | 29 | 29 | 28 | 1 |

- 54 primary, **106 dòng eligible**, 0 primary nằm ngoài tập hợp.
- Mahdi `2000005021`: OVR SQL NULL, không có dòng vị trí.
- James `2000030248`: OVR 61, không có dòng vị trí.
- 56 players và 56 memberships trong DB cô lập sau kiểm tra; importer không tạo roster mới.
- Log, response API, dữ liệu EA đọc được và DB H2 nằm dưới `backend/target/`, Git ignored.

## File mới và phát hành sau này

Sáu file cần review/commit trong thư mục này:

- `profiles.csv`: snapshot hồ sơ hiện có, định dạng importer hồ sơ (56 người).
- `positions.csv`: dữ liệu vị trí đã xác minh, định dạng importer vị trí (54 người).
- `roster-status.csv`: toàn bộ roster và trạng thái/các ô còn thiếu (56 người).
- `sources.md`: URL gốc và quy tắc ánh xạ, phân biệt OVR EA với OVR người dùng bổ sung.
- `missing-fields.txt`: hai người cần bổ sung và đúng các trường còn thiếu.
- `validation.md`: báo cáo này.

Commit message đề xuất:

```text
data(players): prepare Chelsea and Tottenham 2026 profiles and eligible positions
```

Chưa ghi production, commit, push hoặc deploy. Batch không đổi logic Fantasy,
OVR hiện có, dữ liệu trận, membership hay mùa 2024/25. Khi nhập production sau này,
vẫn phải đối chiếu bản hiện hành và sao lưu SQL mới trước khi ghi theo quy trình dự án.

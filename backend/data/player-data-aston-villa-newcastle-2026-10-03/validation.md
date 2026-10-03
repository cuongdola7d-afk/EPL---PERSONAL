# Kiểm tra batch Aston Villa–Newcastle 2026/27

Ngày chuẩn bị và kiểm tra: 2026-10-03. Chỉ thêm dữ liệu local, không sửa code,
schema hoặc UI. Không tạo test Java riêng, không chạy toàn bộ test/build.

## Đối chiếu dữ liệu

- API roster `season=2026&asOf=2026-10-03`: Aston Villa FC 27 người, Newcastle
  United FC 28 người. Cùng 55 cặp player_id/club_id với hai CSV hồ sơ 02/10.
- profiles.csv và roster-status.csv có đúng 55 ID duy nhất, một dòng mỗi người.
  profiles.csv khớp sáu trường đang có trên API, kể cả ô SQL NULL.
- Ba khác biệt với CSV hồ sơ cũ là OVR James Wright 61, Leon Goretzka 78,
  Miodrag Pivas 62: đều là bổ sung người dùng đã lưu, được giữ nguyên.
- 48 trang EA FC 27 có Position/Alt Positions được ghi ở sources.md; 48 OVR EA
  khớp hiện có. Chỉ CDM → CM và gộp mã trùng, không thêm quyền vị trí từ nhóm rộng.
- positions.csv có 48 dòng/48 ID, 25 Aston Villa + 23 Newcastle. Bảy MISSING được
  giữ trong tracking và missing-fields.txt; không đưa dòng incoming trống vào reader.
- Các hồ sơ SofaScore còn thiếu đã được mở lại; vẫn không có chiều cao Kyran
  Thompson/Mason Miley/Michael Mills và chân thuận Michael Mills. Không dùng số
  followers làm chiều cao. Năm trường hồ sơ của các người khác không thu thập lại.

## Reader/importer chung trên H2 cô lập

Chạy JAR backend đã có: backend/target/premierhub-backend-0.1.0-SNAPSHOT.jar.
H2: backend/target/aston-villa-newcastle-data/check-db.mv.db.
Mỗi lệnh đặt rõ URL jdbc:h2:file, username sa, password trống,
spring.sql.init.mode=never, spring.main.web-application-type=none.
Không dùng MySQL hay cấu hình .env.local để kiểm tra.

Seed từ schema hiện có và response API của 55 người: ID/CLB, nhóm rộng, hồ sơ,
goals/assists hiện có. Membership local mô phỏng hiệu lực 03/10 từ kết quả API,
không khẳng định ngày này là ngày chuyển nhượng thật. Ban đầu có 55 hồ sơ,
không có vị trí. Không thu thập hoặc sửa Gameweek.

Hai reader/importer hiện có kiểm tra ID, membership đúng CLB/mùa, định dạng
ngày/chiều cao/chân thuận/số áo/OVR, mã vị trí, ID trùng, primary thuộc eligible,
xung đột và nhập lặp. Không tạo luồng nhập mới.

| File | Lần | Rows | Inserted | Updated |
| --- | ---: | ---: | ---: | ---: |
| profiles.csv | 1 | 55 | 0 | Không có thao tác cập nhật |
| profiles.csv | 2 | 55 | 0 | Không có thao tác cập nhật |
| positions.csv | 1 | 48 | 48 | 0 |
| positions.csv | 2 | 48 | 0 | 0 |

Cả bốn lệnh exit code 0. Hồ sơ thêm 0 vì khớp dữ liệu đã seed từ API, không phải
vì bỏ qua kiểm tra. Vị trí lần đầu thêm 48, nhập lại không thêm/cập nhật.

## Đọc lại H2

| CLB | Hồ sơ | Quốc tịch | DOB | Chiều cao | Chân thuận | Số áo | OVR số | Primary | MISSING vị trí |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Aston Villa | 27 | 27 | 27 | 27 | 27 | 27 | 27 | 25 | 2 |
| Newcastle | 28 | 28 | 28 | 25 | 27 | 28 | 24 | 23 | 5 |

- 48 primary, 87 dòng eligible, 0 primary nằm ngoài eligible.
- James Wright 61, Leon Goretzka 78, Miodrag Pivas 62 vẫn có OVR như trước;
  không có dòng vị trí do thiếu nguồn.
- OVR SQL NULL của Kyran Thompson, Mason Miley, Michael Mills, Vakhtang Salia
  được giữ nguyên; không đổi thành 0. Bốn người vẫn thiếu vị trí.
- 55 players và 55 memberships sau kiểm tra: importer không tạo roster mới.
- Log, snapshot API, dữ liệu EA đã đọc và DB H2 nằm trong backend/target/, Git ignored.

## Các file cần review/commit

- profiles.csv: toàn bộ 55 hồ sơ hiện hành, đúng định dạng importer hồ sơ.
- positions.csv: 48 bộ vị trí có nguồn, đúng định dạng importer vị trí.
- roster-status.csv: toàn bộ 55 người, trạng thái và trường thiếu.
- sources.md: URL từng người, dữ liệu EA gốc, ánh xạ, nguồn người dùng.
- missing-fields.txt: bảy người cần bổ sung và chính xác từng trường.
- validation.md: báo cáo này.

Bổ sung James Rowswell CB/RB nằm riêng tại
backend/data/player-position-supplements-2026-10-03/, không sửa snapshot
Chelsea–Tottenham đã chốt. Các file batch đó được đối chiếu SHA-256 trước/sau.

Commit message đề xuất (gộp batch và bổ sung):

```text
data(players): prepare Villa and Newcastle 2026 profiles and positions; add Rowswell CB/RB
```

Chưa ghi MySQL production, commit, push hoặc deploy. Không thay dữ liệu trận,
membership, nhóm rộng, OVR hiện có hoặc dữ liệu/logic mùa 2024/25.

# Kiểm tra batch Everton – Fulham sau bổ sung người dùng

Ngày 2026-10-03, chỉ mùa 2026/27; không sửa mã/schema/importer hoặc batch khác.

## Dữ liệu đã chốt local

- Roster giữ nguyên Everton20, Fulham26: 46 ID duy nhất, đúng cặp ID/CLB đã lấy từ
  PremierHub season2026, asOf2026-10-03; không đọc lại production trong lượt bổ sung.
- 46 dòng profiles.csv, positions.csv, roster-status.csv; đủ năm trường hồ sơ,
  46 OVR số, 46 primary, 79 mã eligible. Không còn MISSING/REVIEW_REQUIRED.
- Đúng năm ô hồ sơ thay đổi so với batch trước bổ sung trong Git:
  Braiden2000030192 OVR60; Zepa2000030194 OVR60/height178/shirt35;
  Gonzalo2000030082 nationality Spain. Mọi ô khác và ID/CLB giữ nguyên.
- Thêm đúng hai bộ vị trí: Braiden ST/ST, Zepa RW/RW|RM. Giữ nguyên44 bộ EA;
  không thu thập lại EA hoặc sửa quyền chơi của người khác.
- OVR và vị trí Braiden/Zepa, chiều cao/số áo Zepa và quốc tịch Gonzalo có nguồn
  người dùng03/10, ghi rõ ở sources.md. Hai OVR60 không được ghi là OVR EA xác minh.
- profile-updates.csv kê đúng năm ô hồ sơ, đúng ID/CLB và không trùng khóa.
  File này không phải đầu vào PlayerProfileCsvReader. Quy trình áp dụng có điều kiện
  sau này và giá trị cũ/null đã ghi trong sources.md; chưa áp dụng SQL.

## Reader và kiểm tra dữ liệu chung

Đã gọi hai reader hiện có từ backend/target/classes qua Java source launcher
chỉ đọc file batch mới. Không tạo test Java riêng, H2 hoặc full test/build.

```text
COMMON_READERS_OK profiles=46 positions=46
exit code=0
USER_SUPPLEMENTS_CHECKS_OK profiles=46 positions=46 eligible=79 updates=5 missing=0
```

Reader kiểm tra header/số cột, ID duy nhất, ngày, chiều cao, chân thuận, số áo/OVR,
season2026, mã vị trí và primary trong eligible. Kiểm tra chung đối chiếu ID/CLB,
ba CSV cùng46 ID, đúng năm ô đổi so với Git, các dòng vị trí cũ giữ nguyên và
hai vị trí mới đúng người dùng. Không kiểm tra lại batch CLB khác.

| CLB | Roster | Đủ5 trường | OVR số | Có vị trí | EA positions | User positions | MISSING |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Everton | 20 | 20 | 20 | 20 | 19 | 1 | 0 |
| Fulham | 26 | 26 | 26 | 26 | 25 | 1 | 0 |

Không kết nối/ghi MySQL production, commit, push/deploy hoặc sửa mùa2024/25.
Không tuyên bố đã kiểm tra nhập DB hoặc idempotent trong lượt này.

## File thay đổi

profiles.csv, positions.csv, roster-status.csv, sources.md, missing-fields.txt,
validation.md; thêm profile-updates.csv. Các script/log tạm trong target không commit.

```text
data(players): complete Everton and Fulham profiles with user-supplied values
```

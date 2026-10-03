# Kiểm tra bổ sung Brighton 2026/27

- Ba ID lấy từ batch roster/hồ sơ đã chuẩn bị: 2000030174, 2000030170, 2000030176;
  club_id=1000000397, season_year=2026. Không thu thập lại roster/hồ sơ Brighton.
- Theo người dùng 03/10: Chema CM; Oriola primary LW, eligible LW|LM;
  Younes CAM và fc27_overall60. Giữ OVR69/60 Chema/Oriola và năm trường hồ sơ đã đủ.
- profiles.csv và roster-status.csv: 3 dòng; positions.csv: 3 primary, 4 mã eligible.
  Không có trường dữ liệu MISSING trong bản bổ sung. Nguồn người dùng được ghi riêng.
- profile-updates.csv kê đúng một ô OVR60 mới; không phải đầu vào reader hồ sơ.
  Điều kiện áp dụng profile và chiều cao Younes từ batch cũ được ghi tại sources.md.

Chỉ gọi hai reader hiện có cho đúng file bổ sung mới:

```text
COMMON_READERS_OK profiles=3 positions=3
exit code=0
```

Không chạy reader/test/importer lại cho batch Brighton–Bournemouth. Chỉ so SHA-256
bảy file cũ với lúc bắt đầu lượt này để xác nhận chúng giữ nguyên từng byte.
Snapshot ba MISSING trong batch cũ giữ nguyên; trạng thái đã bổ sung ghi ở thư mục này.

Không tạo test Java, nhập H2, chạy full test/build, ghi MySQL, commit, push/deploy.
Không sửa OVR khác, membership, nhóm vị trí rộng, thống kê trận hoặc mùa 2024/25.

File mới: profiles.csv, positions.csv, profile-updates.csv, roster-status.csv,
sources.md, missing-fields.txt, validation.md.

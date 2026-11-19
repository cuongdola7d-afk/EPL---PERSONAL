# Minigame “Cao hay thấp” — Premier League 2026/27 (Frontend-only)

> Trạng thái: **Hiện hành**. Trò chơi tự do chạy hoàn toàn trên trình duyệt; không thêm endpoint backend, bảng database, migration, chế độ hằng ngày hay bảng xếp hạng.

Tham chiếu giao diện: [mockup Cao hay thấp v2](mockups/minigame-cao-hay-thap-v2.html).

## 1. Đường dẫn và tích hợp

- Route: `#minigame/higher` (parse qua `parseMinigameRoute('#minigame/higher')` trả `{ game: 'HIGHER', mode: null }` trong `frontend/src/minigame/playerGuess.js`).
- Điểm vào từ Hub `#minigame`: thẻ **Cao hay thấp** (badge **Chơi tự do**, nút **Chơi ngay** dẫn tới `#minigame/higher`).
- Không yêu cầu đăng nhập và không gọi các API riêng tư của **Đoán cầu thủ** (`/api/minigame/2026/player-guess/*`).

## 2. Nguồn dữ liệu và điều kiện pool

- Đọc dữ liệu một lần khi vào màn chơi qua `fetchPlayers({}, signal, 2026)` (`GET /api/players?season=2026`, roster mùa 2026/27 theo ngày hiện tại) và giữ trong bộ nhớ.
- **Điều kiện cầu thủ vào pool (`eligiblePlayers`):** phải có đủ các trường hợp lệ:
  - `fc27Overall` (số nguyên 1–99)
  - `nationality` (chuỗi không rỗng)
  - `birthDate` (ngày hợp lệ `YYYY-MM-DD`, tuổi tính theo ngày hiện tại giờ Việt Nam `Asia/Ho_Chi_Minh`)
  - `heightCm` (số nguyên dương)
  - `preferredFoot` (`LEFT` → *Trái*, `RIGHT` → *Phải*, `BOTH` → *Hai chân*)
  - `shirtNumber` (số nguyên dương)
  - `primaryPosition` (chuỗi không rỗng)
  - `club` (chuỗi không rỗng)
- Thiếu bất kỳ trường bắt buộc nào ở trên sẽ bị loại khỏi pool; **không tự điền giá trị thay thế, `NULL` không phải `0`**.
- `goals` và `assists` là tổng mùa 2026/27 mà API trả về tính đến vòng đã cập nhật gần nhất. Cầu thủ có `goals === null` hoặc `assists === null` không được dùng cho lượt so chỉ số tương ứng nhưng vẫn hợp lệ cho lượt so OVR (và các chỉ số khác không `null`).

## 3. Luật chơi

1. Mỗi lượt hiển thị **2 thẻ cầu thủ**: thẻ bên trái lộ chỉ số, thẻ bên phải ẩn chỉ số của lượt đó (các thông tin còn lại vẫn hiển thị).
2. Chỉ số so sánh đổi ngẫu nhiên giữa **OVR** (`fc27Overall`), **Bàn thắng** (`goals`) và **Kiến tạo** (`assists`).
3. Cặp người chơi luôn thỏa: không trùng cầu thủ, đều có dữ liệu hợp lệ ở chỉ số được chọn, giá trị hai bên **luôn khác nhau** (không hòa), và ưu tiên tránh lặp lại 5 cầu thủ gần nhất khi pool đủ lớn. Riêng lượt so **Bàn thắng** hoặc **Kiến tạo** chỉ được chọn khi cầu thủ bên trái có giá trị `>= 1` (nếu bằng `0` thì chỉ chọn trong các chỉ số còn hợp lệ như OVR).
4. Người chơi chọn mũi tên lên (**Cao hơn**, phím `↑`) hoặc mũi tên xuống (**Thấp hơn**, phím `↓`) để đoán người bên phải so với người bên trái.
5. **Đoán đúng:** Điểm Số `+1`, thẻ phải lật số thật, sau khoảng 2 giây chuyển sang bên trái và xuất hiện đối thủ mới bên phải.
6. **Đoán sai:** Hiệu ứng sai, sau đó mở popup **Kết Thúc** (hoặc **Kỷ Lục Mới!** nếu vừa phá kỷ lục). Popup dùng `role="alertdialog"`, không đóng bằng Esc hoặc bấm nền, chỉ chọn **Chơi Lại** hoặc **Về Minigame**.
7. **Kỷ lục cá nhân:** lưu ở `localStorage` với key `prismaxi-higher-best`, bọc `try/catch` (nếu trình duyệt chặn storage vẫn chơi bình thường trong phiên hiện tại).
8. Hỗ trợ `prefers-reduced-motion` (tắt lật thẻ, rung, đếm số) và đồng bộ giao diện Light/Dark qua `data-mg-theme`.

## 4. Giới hạn đã biết

- Kỷ lục chỉ lưu cục bộ trên từng trình duyệt/thiết bị, không đồng bộ tài khoản và không có bảng xếp hạng.
- Dữ liệu `goals` và `assists` phụ thuộc tiến độ nhập thống kê mùa 2026/27 trên backend.
- Nếu pool không đủ ít nhất 2 cầu thủ có giá trị khác nhau ở bất kỳ chỉ số nào, màn hình báo **Chưa đủ dữ liệu cầu thủ để chơi** và không tự tạo dữ liệu mẫu.

## 5. Cách kiểm tra

Từ thư mục `frontend/`:

```powershell
npm.cmd test
npm.cmd run build
npm.cmd run dev
```

- Test logic thuần nằm tại `frontend/src/minigame/higherLower.test.js` (lọc pool, xử lý `null`, không lặp/không hòa, chấm đúng/sai, chuyển lượt, lưu kỷ lục an toàn khi storage lỗi, pool quá nhỏ, tính tuổi theo giờ Việt Nam) và test route tại `frontend/src/minigame/playerGuess.test.js`.
- Khi chạy `npm.cmd run dev`, Vite proxy `/api` về `http://localhost:8080` (cần backend có dữ liệu mùa 2026; chỉ đọc `GET /api/players?season=2026`).

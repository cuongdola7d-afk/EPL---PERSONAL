# Giao diện Minigame — local 2026/27

Ngày 08/10/2026, người dùng giao nối frontend sau bước backend. Giao diện dùng bố cục, màu và minh họa SVG từ mockup đã gửi; không dùng danh sách cầu thủ mẫu, engine tính điểm trong trình duyệt hay điều khiển giả lập đăng nhập của mockup. Không thêm dependency. Hợp đồng backend ở [tài liệu API](player-guess-minigame-api.md).

## Chạy và mở trang

Terminal backend, từ `backend/`:

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=minigame-local'
```

Terminal frontend, từ `frontend/`:

```powershell
npm.cmd run dev
```

Mở `http://localhost:5173/#minigame`. Liên kết trực tiếp:

- Daily: `http://localhost:5173/#minigame/guess/daily`.
- Luyện tập: `http://localhost:5173/#minigame/guess/practice`.
- BXH: chọn “Bảng xếp hạng” ở một trong hai màn chơi; khách chưa đăng nhập vẫn xem được.

Backend cần cổng 8080, frontend 5173 để khớp proxy và origin local đã được cho phép. Nếu Vite báo cổng 5173 bận, dùng đúng tiến trình đang chạy hoặc dừng tiến trình đó trước khi chạy lại. Dùng một host nhất quán khi đăng nhập; cookie localhost và 127.0.0.1 là hai phiên khác nhau. Tài khoản email/mật khẩu dùng được khi Google chưa được cấu hình.

`minigame-local` dùng H2 riêng dưới `backend/target/minigame-local`, không tự nhập roster. Thấy “Chưa có đủ cầu thủ…” khi bấm bắt đầu nghĩa là API đã hoạt động nhưng pool chưa đủ hồ sơ/membership/OVR thật. Không thêm dữ liệu giả vào database này để che trạng thái thiếu dữ liệu. Chưa bật feature thì API 404 và trang báo Minigame chưa sẵn sàng; cần chạy đúng profile. H2 dưới `target/` bị xóa khi `mvn clean`; restart thông thường giữ tiến trình.

## Hành vi giao diện

- Tab Minigame nằm sau Fantasy, tự cuộn vào vùng thấy được trên thanh điều hướng mobile. Hub có banner daily, đồng hồ theo server, ba thẻ trò chơi; hai trò tiếp theo vẫn “Sắp ra mắt”. Có sáng/tối, chọn chế độ và hộp luật chơi.
- Daily và luyện tập đều cần đăng nhập bằng hộp tài khoản hiện có. Vào trang/reload chỉ GET current. Ván mới chỉ tạo khi bấm bắt đầu; luyện tập tiếp theo chỉ tạo khi ván trước đã kết thúc và bấm “Chơi ván mới”.
- Điểm, lượt, gợi ý và kết quả đều do server trả. Tên tìm kiếm hỗ trợ tiếng Việt không dấu; bắt buộc chọn ID, có phím lên/xuống/Enter/Escape và đánh dấu cầu thủ đã đoán. Không gửi toàn bộ hồ sơ cầu thủ về trang chơi.
- Kết thúc hiển thị đáp án/tất cả gợi ý và `finalScore`; thua nhận 0 dù điểm tạm còn lại lớn hơn 0. Đoán đúng ở 0 vẫn là thắng. Daily có lịch sử 20 ván gần đây, gồm ván hết hạn, chỉ xem kết quả cũ; luyện tập không ghi BXH.
- BXH lấy hạng do server tính, giữ đồng hạng, đánh dấu tài khoản hiện tại, phân trang 20 dòng. Đồng hồ dùng `serverTime` + thời gian trôi qua của `performance.now()`, không lấy ngày/giờ máy làm luật. Qua mốc đổi ngày hoặc trở lại tab sẽ đọc lại tiến trình, không tự tạo câu hỏi.
- Request riêng dùng URL cùng origin, cookie, account ID và CSRF. Đổi tài khoản/unmount hủy request và bỏ dữ liệu riêng đang hiển thị. Không lưu tiến trình vào localStorage.
- Đang gửi thì khóa thao tác ngay để chặn bấm đúp. Mất phản hồi POST thì khóa thao tác mới; “Gửi lại cùng thao tác” giữ nguyên action ID/payload, hoặc “Kiểm tra tiến trình” đọc lại server. Xung đột version lấy trạng thái trong phản hồi 409, không tự đoán lại. Phiên hết hạn/đổi tài khoản yêu cầu kiểm tra lại phiên.

## File và kiểm tra

`frontend/src/components/MinigamePage.jsx` và CSS riêng chứa giao diện; `src/api/playerGuess.js` gọi/kiểm tra DTO; `src/hooks/usePlayerGuessGame.js` quản lý đọc/ghi/hủy/retry; `src/minigame/playerGuess.js` có route/countdown/action payload. `App.jsx` nối tab/route/session; `AccountMenu.jsx` nhận sự kiện mở hộp đăng nhập. SVG nằm ở `frontend/public/minigame/`.

Từ `frontend/`:

```powershell
npm.cmd test
npm.cmd run build
```

Kết quả cuối lượt: 103 test frontend qua, gồm 12 test Minigame; build Vite qua. Test mới kiểm tra route, countdown, DTO che đáp án/ownership, GET không tạo ván, CSRF, action/version, retry giữ key, lỗi mất phản hồi/dữ liệu sai và đồng hạng.

Chrome thật đã kiểm tra ở 1440px và 390px với Vite cổng 5185 nối backend cổng 8085, datasource H2 riêng `backend/target/minigame-ui-check/db`. Database này chứa cầu thủ có nhãn `UI Test`/`UI Low OVR` chỉ để kiểm thử, không phải roster thật, không dùng database người dùng hay production. Đã xác nhận: đăng ký/login email, khách bị chặn chơi nhưng xem được BXH, chỉ bắt đầu bằng nút, bấm đúp chỉ một POST, reload tiếp tục ván, chọn tên bằng bàn phím, đoán sai -20/tự mở hint, đoán lặp bị khóa, thắng daily 70 và BXH đồng hạng, luyện tập loại daily, thua kết quả 0, ván mới, logout xóa UI. Không có lỗi JavaScript hoặc tràn ngang ở hai kích thước.

Kiểm tra lỗi riêng trên cùng backend thật: cho POST mở hint thành công rồi giả lập mất phản hồi tại lớp fetch; gửi lại cùng key chỉ mất 10 điểm một lần. Gửi hint từ phiên request khác rồi thao tác với version cũ: UI nhận 409 và tiến trình mới, không thêm phạt. Mở toàn bộ hint, sai hai lần rồi đúng lần ba: hiển thị thắng +0. Logout từ request khác rồi tiếp tục: UI bỏ game và yêu cầu đăng nhập. Sáng/tối, hộp chọn chế độ và tab Minigame mobile đã được kiểm tra hình ảnh/focus/geometry. Script, database và ảnh kiểm tra nằm trong `backend/target/minigame-ui-check/`, bị Git bỏ qua.

Lượt này không sửa backend/POM, không chạy lại toàn suite backend. Luật nửa đêm/persistence đã được kiểm tra trong bước backend; test countdown frontend kiểm tra mốc giờ nhưng không giả vờ đã chờ đến 00:00 thật. Kết quả backend trước đó và các lỗi sẵn có của toàn suite vẫn được ghi trong tài liệu API.

## Còn lại

Chuẩn bị roster thật có căn cứ trong database local riêng nếu cần chơi bằng cầu thủ thực tế. Trước phát hành còn kiểm thử migration/khóa trên MySQL thử và cấu hình môi trường phát hành. Chưa ghi production, commit, push hoặc deploy.

Bài thực hành nhỏ: chọn một chuỗi mở hint/đoán sai/đoán đúng, tính điểm trên giấy, rồi đối chiếu số điểm/lượt trong tab Network và UI; thử reload giữa ván để xác nhận không sinh ván mới. Không cần sửa luật hay dữ liệu để làm bài này.

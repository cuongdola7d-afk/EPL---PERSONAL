# Minigame Đoán cầu thủ — đặc tả và kế hoạch PrismaXI 2026/27

Ngày ghi nhận: 08/10/2026. Trạng thái hiện tại: **backend đã hoàn thành và kiểm thử local; frontend chưa nối**. Các mục phạm vi lượt lưu kế hoạch bên dưới ghi lại lượt đầu; phần cập nhật triển khai ở cuối tài liệu và [hợp đồng API](player-guess-minigame-api.md).

## Đặc tả chính thức của người dùng

Phần dưới giữ nguyên yêu cầu về ngữ cảnh và luật chơi. Kế hoạch kỹ thuật ở cuối tài liệu là đề xuất triển khai; không thay thế các luật đã chốt. Mockup chưa được cung cấp trong lượt này.

Bạn đang tiếp tục dự án **PrismaXI**, website thống kê Premier League và Fantasy cá nhân của tôi. Tôi muốn bổ sung một tab **Minigame**, bắt đầu với trò **Đoán cầu thủ qua gợi ý**.

**Tôi đã có sẵn bố cục giao diện và sẽ gửi file mockup riêng.** Không tự thiết kế lại bố cục. File đó là tham chiếu giao diện; các luật dưới đây mới là yêu cầu chính thức. Nếu hành vi trong mockup khác luật, giữ bố cục và điều chỉnh hành vi theo luật.

Lượt này hãy hiểu ngữ cảnh, ghi lại đặc tả và lập kế hoạch triển khai. **Chưa sửa code, schema hoặc database.**

### 1. Ngữ cảnh dự án

- Backend Java/Spring Boot, frontend React/Vite, database MySQL.
- Đã có tài khoản email–mật khẩu, đăng nhập Google, phiên đăng nhập và CSRF. Tái sử dụng hệ thống hiện có.
- Đã có dữ liệu hồ sơ cầu thủ: quốc tịch, ngày sinh, chiều cao, chân thuận, số áo, OVR FC 27, vị trí chính và vị trí được phép chơi.
- Membership lưu khoảng thời gian cầu thủ thuộc CLB.
- Chỉ phát triển cho **mùa 2026/27**. Không sửa hoặc bổ sung mùa 2024/25.
- Minigame có điểm và BXH riêng, không ảnh hưởng điểm hoặc luật Fantasy.
- Không sử dụng tổng thống kê mùa làm gợi ý.

### 2. Hai chế độ chơi

**Luyện tập**
- Bắt buộc đăng nhập.
- Có thể chơi nhiều ván.
- Hiển thị điểm từng ván nhưng không cộng vào BXH.
- Reload hoặc đổi trang không tự tạo ván mới, không làm mất ván đang chơi.
- Chỉ tạo ván mới khi người chơi chủ động chọn chơi tiếp.

**Thử thách hằng ngày**
- Bắt buộc đăng nhập.
- Mỗi tài khoản có một ván mỗi ngày.
- Mọi người nhận cùng một đáp án và cùng dữ liệu gợi ý trong ngày đó.
- Lưu tiến trình trên server để reload hoặc đổi thiết bị vẫn tiếp tục đúng ván.
- Chỉ điểm kết thúc của chế độ này được cộng vào BXH.

### 3. Cầu thủ được chọn làm đáp án

Chỉ chọn cầu thủ:
- Thuộc một CLB Premier League mùa 2026/27 theo membership có hiệu lực tại ngày tạo câu hỏi.
- Có **OVR FC 27 ≥ 75**.
- Có đủ dữ liệu cho tất cả gợi ý.

OVR đã được tôi xác nhận hoặc đặt thủ công vẫn được sử dụng; không tự coi mọi OVR là số chính thức từ EA.

Không tự điền dữ liệu còn thiếu để đưa cầu thủ vào pool. Pool không có ứng viên thì báo trạng thái chưa đủ dữ liệu, không tạo đáp án giả.

### 4. Gợi ý và thứ tự mở

Mỗi ván có tám gợi ý, theo thứ tự:

1. Chiều cao.
2. Chân thuận.
3. Tuổi.
4. OVR FC 27.
5. Quốc tịch.
6. Vị trí chính.
7. CLB.
8. Số áo.

Ban đầu mở sẵn **chiều cao và chân thuận**.

Các gợi ý còn lại bị ẩn. Người chơi chỉ được mở gợi ý tiếp theo, không được nhảy thẳng đến CLB hoặc số áo.

- Tuổi tính từ ngày sinh tại ngày tạo câu hỏi theo giờ Việt Nam.
- Vị trí dùng `primaryPosition` cụ thể hiện có, không đổi thành nhóm rộng DEF/MID/FWD.
- Chân thuận hai chân hiển thị “Hai chân”.

### 5. Lượt đoán và tính điểm

- Bắt đầu với **100 điểm**.
- Có tối đa **3 lượt đoán hợp lệ**.
- Chủ động mở một gợi ý: trừ **10 điểm**, không mất lượt đoán.
- Đoán sai: mất một lượt, trừ **20 điểm**, đồng thời tự mở gợi ý tiếp theo nếu còn.
- Gợi ý tự mở do đoán sai không bị trừ thêm 10 điểm.
- Nếu đã mở hết gợi ý, đoán sai vẫn mất lượt và trừ 20 điểm.
- Điểm không xuống dưới 0.
- Đoán đúng: kết thúc thắng, điểm cuối bằng điểm còn lại.
- Đoán đúng khi còn 0 điểm vẫn là thắng với 0 điểm.
- Sai cả ba lượt: kết thúc thua, điểm cuối bằng 0.

Tên chưa chọn từ danh sách, ID không hợp lệ hoặc đoán lại một cầu thủ đã đoán không được tính là lượt đoán và không trừ điểm.

Tìm kiếm hỗ trợ tên có/không dấu theo tiện ích hiện có. Người chơi chọn cầu thủ từ danh sách để gửi `player_id`; không tự suy đoán đáp án từ chuỗi tên nhập tự do.

Kết thúc ván thì hiện đáp án và toàn bộ gợi ý, khóa thao tác đoán/mở gợi ý.

### 6. Đổi câu hỏi hằng ngày

Mốc đổi ngày là **00:00 Asia/Ho_Chi_Minh**.

- Ván daily chưa hoàn thành khi hết ngày chuyển sang `EXPIRED`, điểm cuối 0.
- Không được tiếp tục đoán ván cũ sau thời hạn.
- Có thể xem lại kết quả và đáp án của ván đã hết hạn.
- Ngày mới có một ván mới với 100 điểm và ba lượt.
- Ván luyện tập không bị kết thúc khi sang ngày mới.

Thời gian server quyết định trạng thái, không dựa vào đồng hồ trình duyệt.

### 7. Chọn đáp án và hạn chế lặp

Đối với daily:
- Xáo trộn danh sách ứng viên hợp lệ thành một chu kỳ.
- Không lặp đáp án cho đến khi đã dùng hết ứng viên trong chu kỳ.
- Khi bắt đầu chu kỳ mới, tránh lặp ngay đáp án ngày trước nếu có ít nhất hai ứng viên.
- Lưu cơ chế chọn trên database để restart không làm mất lịch sử.

Đối với luyện tập:
- Chọn ngẫu nhiên, hạn chế lặp các đáp án gần đây khi pool cho phép.
- **Không chọn đáp án daily của ngày hiện tại**, kể cả người chơi đã hoàn thành daily.
- Xử lý pool quá nhỏ bằng trạng thái rõ ràng, không chạy vòng lặp vô hạn.

Câu hỏi phải lưu snapshot dữ liệu tại lúc tạo. Hồ sơ hoặc CLB thay đổi sau đó không được làm đổi gợi ý của ván đang diễn ra.

### 8. Bảng xếp hạng

- BXH riêng của trò Đoán cầu thủ.
- Xếp theo **tổng điểm daily tích lũy**, không chia tuần hoặc tháng.
- Điểm cao đứng trên; bằng điểm thì đồng hạng.
- Không dùng thời gian hoàn thành làm tiêu chí phụ.
- Không cộng điểm luyện tập hoặc điểm tạm thời của ván chưa kết thúc.
- Một kết quả chỉ được cộng một lần, kể cả reload, gửi lại request hoặc mở nhiều tab.

### 9. Nguyên tắc kỹ thuật

- Backend quyết định đáp án, điểm, lượt đoán, gợi ý được mở và trạng thái.
- Không gửi tên/ID đáp án hoặc các gợi ý còn ẩn xuống frontend trước khi ván kết thúc.
- Kiểm tra đăng nhập, quyền sở hữu ván và CSRF bằng cơ chế hiện có.
- Lưu tiến trình và kết quả trong database; localStorage không phải nguồn dữ liệu chính.
- Có cơ chế chống double-click, request trùng và thao tác đồng thời từ nhiều tab.
- Đảm bảo chỉ có một câu hỏi daily mỗi ngày và một ván daily mỗi tài khoản/ngày.
- Không xây hệ thống chống gian lận phức tạp ở phiên bản đầu. Tôi chấp nhận khả năng người chơi chia sẻ đáp án.

Mockup có thể chứa dữ liệu mẫu, logic chạy trong trình duyệt hoặc cho khách chơi luyện tập. Không sử dụng những phần đó làm dữ liệu thật hoặc luật chính thức.

## 10. Phạm vi lượt lưu kế hoạch

- Đã đọc `AGENTS.md` và đối chiếu có giới hạn các phần tài khoản, hồ sơ, membership, điều hướng và BXH liên quan.
- Chỉ tạo tài liệu này; chưa sửa mã nguồn, schema hoặc database. Không chạy test/build, commit, push, deploy; không yêu cầu credentials production.
- Không thu thập thêm dữ liệu, không tự điền hồ sơ thiếu, không thay membership hay dữ liệu mùa 2024/25.
- Khi có mockup: giữ bố cục được cung cấp, thay dữ liệu mẫu và logic trình duyệt bằng API theo đặc tả. Khách không được chơi cả luyện tập lẫn daily.
- Commit message đề xuất khi người dùng cho phép commit tài liệu: `docs(minigame): define player guessing rules and implementation plan`.

## 11. Kết quả đối chiếu với repo

Đây là bằng chứng từ mã nguồn và tài liệu trong workspace, không phải xác nhận database production hay số lượng ứng viên hiện tại. Một số tài liệu cũ mô tả giai đoạn trước; với chức năng đang tồn tại, đối chiếu mã nguồn tương ứng.

| Phần hiện có | Nguồn tham chiếu | Áp dụng cho Minigame |
| --- | --- | --- |
| Tài khoản chung và định danh Google | [auth-schema.sql](../backend/src/main/resources/auth-schema.sql), [AuthController](../backend/src/main/java/com/premierhub/accounts/AuthController.java), [tài khoản local](auth-local-2026.md) | Ván liên kết `accounts.id`, không liên kết email hoặc `player_id` để xác định người chơi. Email/mật khẩu và Google sử dụng cùng hệ thống tài khoản; không làm thêm hệ thống đăng nhập. |
| Phiên JDBC, CSRF và quyền truy cập | [SecurityConfiguration](../backend/src/main/java/com/premierhub/config/SecurityConfiguration.java) | Dùng phiên hiện có, CSRF cho các thao tác ghi. Phải bổ sung matcher Minigame khi triển khai vì cấu hình hiện tại kết thúc bằng `denyAll`. Không miễn CSRF cho trò chơi. |
| Chống thao tác dưới tài khoản cũ | [FantasyEntryController](../backend/src/main/java/com/premierhub/fantasy/FantasyEntryController.java), [fantasyEntries.js](../frontend/src/api/fantasyEntries.js) | Tái sử dụng cách kiểm tra `X-PrismaXI-Account-ID` với tài khoản thực trong phiên và thông báo `SESSION_CHANGED`. Header chỉ phát hiện tab cũ, không cấp quyền hoặc chọn chủ ván. |
| Hồ sơ theo cầu thủ/CLB/mùa | [schema.sql](../backend/src/main/resources/schema.sql), [hồ sơ 2026/27](player-profiles-2026-import.md) | Lấy `nationality`, `birth_date`, `height_cm`, `preferred_foot`, `shirt_number`, `fc27_overall` từ `player_season_profiles` đúng membership. Chấp nhận OVR thủ công đã được người dùng xác nhận; không gắn nhãn tất cả là rating chính thức EA. |
| Membership và danh sách CLB mùa | [schema.sql](../backend/src/main/resources/schema.sql), [FootballQueries](../backend/src/main/java/com/premierhub/service/FootballQueries.java) | Mùa lưu là `season_year=2026`, Premier League là `league_id=39`. Membership có khoảng nửa mở `[start_date, end_date)`: `start_date <= questionDate` và `end_date IS NULL OR end_date > questionDate`. Phải đối chiếu thêm `season_clubs` để CLB thực sự thuộc mùa. |
| Vị trí cụ thể | [vị trí 2026/27](2026-player-positions.md), [PlayerResponse](../backend/src/main/java/com/premierhub/web/dto/PlayerResponse.java) | `player_specific_positions.primary_position` là gợi ý; `eligiblePositions` là quyền chơi Fantasy, không thay thế vị trí chính và không thành gợi ý thứ chín. Không suy từ nhóm rộng. |
| API hồ sơ đang có | [FootballQueries](../backend/src/main/java/com/premierhub/service/FootballQueries.java), [PlayerResponse](../backend/src/main/java/com/premierhub/web/dto/PlayerResponse.java) | API cầu thủ hiện tại còn đọc tổng thống kê và mặc định ngày UTC. Đề xuất truy vấn pool riêng chỉ lấy định danh, membership, hồ sơ và vị trí; chốt ngày Việt Nam rõ ràng. Không gửi nguyên `PlayerResponse` làm response ván hoặc danh sách chọn. |
| Điều hướng và tài khoản frontend | [App.jsx](../frontend/src/App.jsx) | Điều hướng đang dùng `PAGES` và hash, có `AccountMenu` cùng state phiên. Thêm tab Minigame theo cách này khi đến bước giao diện, không cần thêm router/framework. |
| Tìm tên không dấu | [playerSort.js](../frontend/src/utils/playerSort.js) | Tái sử dụng `matchesPlayerSearch`: NFD, bỏ dấu, không phân biệt hoa/thường và `đ`/`d`. Sau khi chọn mới gửi ID. |
| Request công khai và riêng tư | [request.js](../frontend/src/api/request.js), [auth.js](../frontend/src/api/auth.js), [fantasyEntries.js](../frontend/src/api/fantasyEntries.js) | Request ván phải cùng origin, `credentials: 'include'`, CSRF và kiểm tra phiên. Helper công khai đang `credentials: 'omit'`, không phù hợp để đọc/ghi tiến trình cá nhân. |
| BXH Fantasy và cập nhật đồng thời | [FantasyResultService](../backend/src/main/java/com/premierhub/fantasy/FantasyResultService.java), [FantasyEntryService](../backend/src/main/java/com/premierhub/fantasy/FantasyEntryService.java) | Có mẫu đồng hạng, transaction, khóa và version để tham khảo. Tạo bảng/API/BXH Minigame riêng; không sử dụng điểm, workflow công bố hoặc giới hạn GW6–38 của Fantasy. |
| Đồng hồ có thể thay trong test | [GameweekConfiguration](../backend/src/main/java/com/premierhub/fantasy/GameweekConfiguration.java) | Dùng cùng mẫu Java `Clock`, nhưng cấu hình riêng cho Minigame để kiểm tra nửa đêm mà không ảnh hưởng Fantasy. |

## 12. Thiết kế backend đề xuất

### 12.1. Bộ luật độc lập và trạng thái ván

Một lớp luật Java nhỏ nhận trạng thái hiện tại và hành động, trả trạng thái mới; không đọc HTTP, CSV hay database. Service chịu trách nhiệm xác thực, thời gian, truy vấn và lưu transaction. Cách tách này giúp kiểm tra phép tính trước khi ghép API.

Trạng thái đề xuất: `IN_PROGRESS`, `WON`, `LOST`, `EXPIRED`. Ván lưu riêng `currentScore` và `finalScore`: điểm cuối chưa có khi đang chơi; kết thúc thắng lấy điểm còn lại, thua/hết hạn là 0. `revealedHintCount` khởi tạo 2; gợi ý đã mở luôn là một đoạn liên tiếp của danh sách tám mục. Không nhận điểm, trạng thái, số lượt hay vị trí gợi ý muốn mở từ client.

| Hành động | Điều kiện | Thay đổi |
| --- | --- | --- |
| Mở tiếp | Đang chơi, còn gợi ý, daily chưa hết hạn | Tăng số gợi ý mở 1; điểm `max(0, currentScore - 10)`; giữ số lượt đã đoán. |
| Đoán sai hợp lệ | Đang chơi, ID được phép, chưa từng đoán, chưa hết hạn | Tăng số lượt đã dùng 1; điểm `max(0, currentScore - 20)`; mở thêm đúng một gợi ý nếu còn, không trừ thêm 10. Sai lần ba chuyển `LOST`, điểm cuối 0. |
| Đoán đúng hợp lệ | Cùng điều kiện kiểm tra đầu vào | Ghi lượt hợp lệ, chuyển `WON`; điểm cuối bằng điểm trước lượt đúng, kể cả 0. |
| Đầu vào không hợp lệ/trùng cầu thủ | Chưa chọn ID, ID không hợp lệ hoặc đã đoán | Báo lỗi rõ; không mất lượt, không trừ điểm, không mở gợi ý. |
| Daily hết hạn | Thời gian server `>= expiresAt`, đang chơi | Chuyển `EXPIRED`, điểm cuối 0, không áp dụng hành động đoán/mở tiếp. |
| Thao tác vào ván kết thúc | Đã thắng/thua/hết hạn | Không thay đổi kết quả; có thể đọc đáp án và toàn bộ gợi ý. |
| Mở tiếp khi đã đủ tám gợi ý | Đang chơi | Báo đã mở hết; không trừ điểm hay mất lượt. |

Ví dụ: sai lượt đầu từ trạng thái ban đầu còn 80 điểm, hai lượt và mở tới tuổi. Mở tiếp OVR còn 70; đoán đúng kết thúc 70 điểm. Mở sáu gợi ý bằng tay còn 40; sai hai lần còn 0; đoán đúng lượt ba vẫn là thắng 0. Sai cả ba lượt luôn có điểm cuối 0, dù điểm tạm còn dương.

Khi kết thúc, response hiển thị toàn bộ gợi ý miễn phí; không giả lập sáu hành động mở tiếp để trừ điểm. Lịch sử vẫn phân biệt gợi ý mở chủ động và tự mở nếu cần giải thích kết quả.

### 12.2. Pool và snapshot câu hỏi

- Tại ngày tạo theo `Asia/Ho_Chi_Minh`, chọn duy nhất một membership hợp lệ cho cầu thủ trong Premier League 2026/27, CLB nằm trong `season_clubs`, OVR >= 75 và đủ tám gợi ý. Membership chồng lấn hoặc hồ sơ mâu thuẫn phải bị loại kèm lý do, không tùy tiện chọn một CLB.
- Loại `NULL`, chuỗi rỗng và giá trị không hợp lệ; không dùng dữ liệu thống kê mùa để bù hồ sơ. Ngày sinh phải cho phép tính tuổi hợp lệ tại ngày chốt; chân thuận dùng `LEFT`, `RIGHT`, `BOTH` và `BOTH` hiển thị “Hai chân”.
- Snapshot gồm ngày chốt, ID/tên đáp án, ID/tên CLB và tám giá trị gợi ý. Lưu tuổi đã tính và ngày sinh làm căn cứ nội bộ. Ghi phiên bản luật để các ván đã tạo giữ cách xử lý nhất quán.
- Snapshot daily là chung cho ngày, không tính tuổi hoặc đọc lại CLB riêng lúc từng tài khoản bắt đầu. Snapshot luyện tập chốt lúc tạo ván đó, giữ nguyên qua ngày mới và thay đổi hồ sơ.
- Response đang chơi chỉ có metadata cần thiết, điểm, số lượt, lịch sử ID đã đoán và giá trị gợi ý được mở. Nhãn của các ô ẩn có thể gửi; giá trị ẩn, tên/ID đáp án, ngày sinh nội bộ, FK câu hỏi để suy ra đáp án và nội dung snapshot không được xuất ra. Kết thúc mới trả đáp án và đủ tám gợi ý.
- Danh sách tìm chọn chỉ trả ID/tên tối thiểu, không đánh dấu đáp án hoặc gửi hồ sơ đầy đủ. Pool đáp án và danh sách cầu thủ có thể đoán là hai khái niệm khác nhau; phạm vi danh sách đoán cần chốt ở mục 14.
- Pool rỗng trả trạng thái như `INSUFFICIENT_DATA`. Luyện tập phải tạo/đảm bảo câu hỏi daily ngày hiện tại trước để biết ID cần loại. Nếu loại daily xong không còn ai, trả trạng thái như `PRACTICE_POOL_TOO_SMALL`, không trả ID daily để giải thích lỗi.
- Hạn chế lặp luyện tập bằng lịch sử theo tài khoản trong database. Lọc một lần rồi chọn từ danh sách; nếu hết lựa chọn vì lịch sử gần đây, nới lịch sử theo chính sách đã chốt, luôn giữ điều kiện loại daily hiện tại.

### 12.3. Nhóm dữ liệu và ràng buộc dự kiến

Đây là thiết kế logic để review; **chưa tạo DDL hoặc sửa schema**. Tên bảng có thể điều chỉnh trước triển khai.

| Nhóm bảng đề xuất | Nội dung và ràng buộc cần có |
| --- | --- |
| `player_guess_questions` | Mode, mùa 2026, ngày Việt Nam, đáp án và snapshot, thời điểm tạo/hết hạn, phiên bản luật. UNIQUE ngày daily trong phạm vi trò/mùa; câu hỏi practice không chịu unique ngày. |
| `player_guess_daily_cycles`, `player_guess_daily_cycle_items` | Chu kỳ, danh sách ID đã xáo trộn, thứ tự, mục đã dùng/bỏ qua và lý do, con trỏ, đáp án trước đó. Lưu DB, khóa trạng thái chọn khi tạo daily để nhiều request chỉ lấy một đáp án. |
| `player_guess_games` | Chủ ván `accounts.id`, câu hỏi, mode, ngày daily nếu có, trạng thái, điểm hiện tại/cuối, lượt dùng, số gợi ý mở, version, thời điểm kết thúc. UNIQUE tài khoản/ngày daily; không áp quy tắc một ván/ngày cho practice. |
| `player_guess_practice_state` | Một dòng mỗi tài khoản trỏ tới ván practice hiện hành; khóa dòng hoặc tài khoản khi bắt đầu/chơi tiếp. Giữ ván kết thúc để reload vẫn đọc được kết quả; không tự tạo tiếp. |
| `player_guess_guesses` | Lịch sử lượt hợp lệ và ID cầu thủ; UNIQUE ván/cầu thủ, UNIQUE ván/thứ tự lượt. Không ghi đầu vào lỗi thành lượt hợp lệ. |
| `player_guess_actions` | `actionId`, chủ ván, payload hoặc fingerprint, kết quả/version đã áp dụng; UNIQUE phạm vi chủ sở hữu/actionId để chống request lặp, gồm cả tạo ván. Cùng key nhưng khác payload bị từ chối. |
| `player_guess_daily_results` | Sổ kết quả daily cuối cùng, điểm 0–100, FK ván; UNIQUE ván và UNIQUE tài khoản/ngày daily. Practice không được ghi vào đây. BXH cộng sổ này để không cộng hai lần. |

FK và CHECK bảo vệ chủ ván, mode, mùa, miền điểm/lượt/gợi ý và tính nhất quán điểm cuối với trạng thái. UNIQUE là hàng rào database khi hai request cùng vượt qua kiểm tra trong Java. Cần review hành vi cột nullable/unique trên MySQL thật khi viết migration; không chỉ dựa vào H2.

Không tạo bảng tổng điểm tích lũy ở bản đầu nếu truy vấn SUM từ sổ kết quả đáp ứng tải. Mỗi kết quả có đúng một dòng nên retry không làm tăng tổng. Không chỉnh các bảng kết quả Fantasy.

### 12.4. Transaction, retry và đổi ngày

- Mỗi thao tác ghi dùng `actionId` và `expectedVersion`. Retry giữ nguyên `actionId`; thao tác mới có key mới. Lưu dấu hành động và thay đổi ván trong cùng transaction; lỗi giữa chừng rollback cả hai.
- Backend luôn xác định tài khoản từ phiên, kiểm tra chủ ván trước đọc/trả dữ liệu hoặc tìm kết quả hành động cũ. Không tin account ID trong body/header.
- Đề xuất một thứ tự khóa thống nhất cho các nhánh liên quan: trạng thái chọn daily -> tài khoản/trạng thái practice -> ván -> hành động/kết quả. Khóa daily chỉ dùng khi cần tạo câu hỏi. Khi viết repository phải rà mọi nhánh để không khóa ngược thứ tự.
- Hai lệnh với cùng version: chỉ một lệnh được áp dụng; lệnh còn lại nhận xung đột và phải tải tiến trình mới. Hai request cùng key/payload chỉ tạo một hiệu ứng; client không được tự gửi lại với key mới khi chưa biết request trước đã thành công hay chưa.
- Ghi trạng thái kết thúc và dòng kết quả daily trong một transaction. Sổ kết quả UNIQUE đảm bảo không cộng lại qua refresh, nhiều tab hoặc retry. Practice chỉ lưu kết quả ván.
- `questionDate` tính từ `Clock` server trong zone Việt Nam. Daily hết hạn ở đầu ngày kế tiếp trong zone này rồi chuyển sang Instant UTC để lưu; tại đúng 00:00 đã hết hạn. Ví dụ daily 08/10/2026 hết hạn lúc 09/10/2026 00:00 Việt Nam, tức 08/10/2026 17:00 UTC.
- Đọc lại clock sau khi lấy khóa và trước khi chấp nhận thay đổi; request tới trước nửa đêm nhưng phải chờ khóa qua nửa đêm không được đoán ngày cũ. Nếu ngày đổi trong lúc tạo daily/practice thì kiểm tra lại ngày và thực hiện lại lựa chọn cần thiết.
- Lazy creation: khi bắt đầu daily hoặc practice, tạo daily hôm nay nếu chưa có, không cần cron. Không tạo bù daily cho những ngày không có câu hỏi/người chơi.
- Đề xuất expiry được đối chiếu trên mọi lần đọc/ghi ván, đọc daily hiện tại/lịch sử và trước tính BXH; các ván quá hạn chưa kết thúc được chuyển `EXPIRED` và ghi kết quả 0 một lần. API không được phục vụ trạng thái đang chơi đã quá hạn. Không cần cron ở bản đầu; dòng chưa truy cập có thể được chuẩn hóa trong lượt đọc tiếp theo.
- Khi phát hiện hết hạn trong request đoán/mở gợi ý, phải commit việc chuyển `EXPIRED` và kết quả 0 rồi trả trạng thái hết hạn; không ném exception khiến transaction rollback mất cập nhật hết hạn. Endpoint đọc có chuẩn hóa expiry cũng cần transaction ghi phù hợp, không đánh dấu toàn bộ luồng đó là read-only.
- Nếu retry một hành động cũ sau khi ván đã hết hạn/kết thúc, vẫn không áp dụng lại; response phải phản ánh trạng thái mới nhất để không hiển thị ván cũ còn chơi được.

## 13. Kế hoạch triển khai theo năm bước

### Bước 1 — Schema riêng và bộ luật xử lý ván backend

**Đây là bước đầu tiên sẽ triển khai khi được giao lượt code.** Phần mở đầu là mô hình trạng thái và bộ luật Java thuần; sau đó hoàn thiện thiết kế schema, truy vấn pool/snapshot và migration chỉ cho Minigame. Các quyết định về chu kỳ/lịch sử ở mục 14 cần chốt trước phần chọn đáp án tương ứng, nhưng không chặn việc viết luật tính điểm.

- Dự kiến package `backend/src/main/java/com/premierhub/minigame/` cho model, rules, repository và service nhỏ. Không thêm framework/dependency; giữ JDBC và transaction đã có.
- Dự kiến DDL local riêng `backend/src/main/resources/player-guess-schema.sql` và migration MySQL theo quy ước repo; kiểm tra cách nạp schema hiện hữu trước nối vào cấu hình. DDL không tự áp dụng production.
- Tách `Clock`, tính ngày Việt Nam, điều kiện đủ dữ liệu và snapshot khỏi phép tính lượt/điểm. Làm selector daily có trạng thái bền vững và practice có danh sách loại trừ hữu hạn.
- Codex làm khung và phần khóa/transaction. Bài vừa sức đề xuất cho người học ở lượt triển khai: tự viết các test tính điểm từ chuỗi hành động ở bảng mục 12.1; không tự hoàn thành phần đã giao cho người học.
- Tiêu chí xong: kiểm thử có ý nghĩa cho mọi chuyển trạng thái và lỗi đầu vào; snapshot không đổi theo hồ sơ; truy vấn chỉ mùa 2026/27; không chọn cầu thủ thiếu dữ liệu. Review DDL/FK/UNIQUE và kế hoạch rollback trên database thử.

### Bước 2 — API practice, daily và lưu tiến trình

Các endpoint dưới đây là hợp đồng dự kiến, chưa tồn tại. Prefix `/api/minigame/2026/player-guess` giữ trò và mùa rõ ràng.

| Method / đường dẫn sau prefix | Mục đích |
| --- | --- |
| `GET /practice/current` | Đọc ván practice đang chơi hoặc kết quả hiện hành; chưa có thì trả trạng thái chưa bắt đầu, không tạo ván. |
| `POST /practice/start` | Người chơi chủ động bắt đầu/chơi tiếp; nếu còn ván đang chơi thì trả ván đó, không thay bằng ván mới. |
| `GET /daily/current` | Đọc trạng thái hôm nay và ván của tài khoản nếu đã bắt đầu; không dùng GET để tạo một ván chơi mới. |
| `POST /daily/start` | Đảm bảo câu hỏi daily và một ván tài khoản/ngày; đã có thì tiếp tục/trả kết quả cũ. |
| `GET /games/{gameId}` | Đọc tiến trình/kết quả ván thuộc tài khoản, gồm ván cũ đã hết hạn. |
| `GET /daily/history` | Danh sách ván daily đã tham gia, ngày và kết quả; có phân trang. |
| `GET /players` | Danh sách ID/tên tối thiểu để tìm chọn; frontend dùng tiện ích không dấu hiện có. Không trả đáp án được đánh dấu hay hồ sơ gợi ý. |
| `POST /games/{gameId}/guesses` | Body gồm `player_id`, `actionId`, `expectedVersion`; backend kiểm tra rồi đoán. |
| `POST /games/{gameId}/hints/next` | Body `actionId`, `expectedVersion`; mở tiếp, không nhận index tùy ý. |
| `GET /leaderboard` | BXH tích lũy ở bước 3; quyền xem cần chốt ở mục 14. |

- DTO riêng cho trạng thái đang chơi/kết thúc; `serverTime`, `expiresAt`, version, điểm, lượt còn và gợi ý đã mở giúp frontend hiển thị đúng. Không serialize trực tiếp entity/snapshot.
- Mọi API chơi cần đăng nhập. Request ghi dùng CSRF và cookie cùng origin; thêm security matcher rõ ràng. Response cá nhân dùng `Cache-Control: no-store` và xử lý `SESSION_CHANGED` theo mẫu hiện có.
- Phân biệt chưa đăng nhập, không có quyền ván, ID đoán lỗi/trùng, version xung đột, pool thiếu, ván kết thúc/hết hạn bằng mã lỗi/trạng thái dễ hiển thị. Lỗi không lộ đáp án.
- Tiêu chí xong: reload/đổi thiết bị giữ đúng ván, không tự tạo practice; cùng ngày/cùng tài khoản không có daily thứ hai; request lặp và nhiều tab không mất thêm điểm/lượt; không đọc ván người khác hoặc lộ dữ liệu ẩn.

### Bước 3 — BXH daily tích lũy

- SUM `finalScore` từ sổ kết quả daily mùa 2026/27; chỉ kết quả kết thúc. Thua và hết hạn là 0; practice và điểm tạm không vào truy vấn.
- Mượn cách đồng hạng hiện có nhưng dùng service/repository/API riêng. Đề xuất hạng kiểu `1, 1, 3` như Fantasy; thứ tự ổn định giữa người đồng điểm có thể dùng account ID để phân trang, không đổi hạng và không dùng thời gian hoàn thành.
- Chỉ xuất tên hiển thị, hạng, tổng điểm và định danh cần cho đánh dấu người hiện tại; không xuất email hoặc thông tin đăng nhập. Đề xuất hiển thị người đã có ít nhất một kết quả daily, kể cả 0 điểm.
- Không có scope tuần/tháng/GW, không cần quản trị viên công bố từng ngày. Hợp đồng pagination phải giữ đồng hạng qua ranh giới trang.
- Tiêu chí xong: retry hoàn tất nhiều lần tổng vẫn không đổi; tổng khớp sổ kết quả, bằng điểm đồng hạng, practice không ảnh hưởng; không truy vấn/ghi điểm Fantasy.

### Bước 4 — Kết nối giao diện theo mockup

**Chờ file mockup người dùng gửi trước khi làm bố cục.** Không dựng giao diện mới trong lượt lưu kế hoạch.

- Dự kiến nối tab/hash Minigame tại `frontend/src/App.jsx`, component trò/BXH và `frontend/src/api/playerGuess.js`; dùng CSS/assets theo mockup và hệ thống hiện tại.
- Truyền phiên từ `AccountMenu` như Fantasy; có loading, lỗi mạng, yêu cầu đăng nhập, thiếu dữ liệu, xung đột, đã kết thúc và hết hạn trong bố cục được cung cấp.
- Đọc ván hiện hành khi vào trang; hành động tạo ván chỉ do người chơi chủ động. Không tạo ván trong effect, do remount hoặc reload. Khi đổi tài khoản, hủy request cũ và xóa state riêng tư trước tải ván mới.
- Tìm không dấu, chọn ID từ danh sách; sửa chuỗi tên sau khi chọn phải bỏ ID lựa chọn cũ. Không tính luật trong trình duyệt hoặc dùng dữ liệu mẫu làm dữ liệu thật.
- Chỉ hiển thị các giá trị gợi ý được server trả; khóa nút trong lúc request và sau kết thúc. Khóa nút là hỗ trợ UX; server vẫn chống trùng/đồng thời.
- Khi quay lại tab hoặc đồng hồ hiển thị tới hạn, đọc server để đồng bộ; thời gian client chỉ hiển thị. Nếu mất mạng, không tự tăng điểm/lượt hoặc tạo ván mới.
- Tiêu chí xong: đối chiếu bố cục mockup trên desktop/mobile; thực hiện đủ hai chế độ và trạng thái; điều hướng không làm mất tiến trình; không có dữ liệu bí mật trong response, DOM hoặc localStorage.

### Bước 5 — Kiểm thử và chuẩn bị phát hành

Chỉ chạy các kiểm tra này ở lượt triển khai được giao, không chạy trong lượt viết tài liệu.

| Nhóm | Trường hợp phải xác minh |
| --- | --- |
| Luật | 100 điểm, hai gợi ý mở sẵn, thứ tự sáu gợi ý tiếp; mở tay -10 không mất lượt; sai -20 và tự mở miễn phí; sai khi đủ tám gợi ý; đúng 0 điểm; sai ba lần điểm cuối 0; ID lỗi/trùng không thay đổi ván. |
| Pool/snapshot | Membership start/end đúng khoảng nửa mở, CLB thuộc mùa, OVR 74/75/NULL/thủ công, từng trường thiếu, vị trí cụ thể, chân BOTH, tuổi tại sinh nhật, pool rỗng/chỉ một người; sửa hồ sơ không đổi snapshot. |
| Chọn đáp án | Daily không lặp trong chu kỳ, tránh lặp ở biên chu kỳ khi >=2; restart giữ lịch sử; nhiều người cùng ngày cùng snapshot; practice luôn loại daily lúc tạo, xử lý lịch sử/pool nhỏ không lặp vô hạn. |
| Thời gian | Ngay trước/đúng/sau 00:00 Việt Nam; request chờ khóa qua hạn; daily hôm cũ EXPIRED/0 và vẫn xem đáp án; ngày mới 100/3; practice không hết hạn; tạo câu hỏi qua nửa đêm. |
| Dữ liệu/đồng thời | Double-click/retry, cùng key khác payload, hai tab cùng version, tạo daily/practice đồng thời, rollback giữa kết thúc và ghi kết quả; sổ kết quả một dòng/ván và tổng BXH không nhân đôi. |
| Bảo mật | Guest không chơi; đọc/ghi ván người khác; CSRF thiếu/sai; phiên đổi tài khoản; API đang chơi không có tên/ID đáp án hoặc gợi ý ẩn; retry cũ không phục hồi trạng thái đã hết hạn. |
| UI/tích hợp | Reload, đổi trang/thiết bị, mất mạng, đổi tài khoản, tìm tên không dấu, sửa tên đã chọn, khôi phục sau xung đột; giữ layout mockup và hiển thị đúng kết quả/BXH. |
| Phát hành | DDL/unique/khóa/timezone trên MySQL thử có dữ liệu mẫu; regression tài khoản/Fantasy; cấu hình proxy/cookie/CSRF và bundle không chứa dữ liệu mẫu hay đáp án. |

Dùng JUnit Jupiter cho luật/service; tích hợp với database thử và frontend theo công cụ hiện có. H2 hỗ trợ kiểm tra nhanh nhưng không thay xác minh MySQL về khóa/unique. Không thêm dependency trước khi giải thích nhu cầu cụ thể.

Chuẩn bị migration, rollback, kiểm tra chỉ mục/truy vấn BXH và checklist vận hành ngắn. Khi người dùng yêu cầu phát hành, hoàn tất review/test trước, xác nhận database đích và backup ngoài Git trước migration. Lượt hiện tại không cho phép nhập production, commit, push hoặc deploy; không tự thêm cron/Pre-deploy Command.

## 14. Những đầu vào/quyết định thực sự còn thiếu

Các luật điểm, lượt, gợi ý, login, ngày Việt Nam, loại daily khỏi practice và BXH đã chốt, không hỏi lại. Những mục dưới là đầu vào hoặc lựa chọn chưa được đặc tả; các đề xuất chưa trở thành luật chính thức.

1. **Mockup**: cần file riêng để xác định bố cục, assets và trạng thái giao diện. Chỉ chặn bước 4, không chặn bộ luật backend.
2. **Pool thay đổi giữa chu kỳ daily**: ứng viên mới vào/chuyển CLB/thiếu dữ liệu thì cập nhật chu kỳ ra sao? Đề xuất đóng danh sách ID tại đầu chu kỳ, kiểm tra lại hợp lệ trước mỗi ngày, bỏ qua người không còn hợp lệ và đưa ứng viên mới vào chu kỳ tiếp. Khi không còn ứng viên hợp lệ chưa dùng, kết thúc chu kỳ và lập chu kỳ mới từ pool hiện tại; phải review tác động tới yêu cầu không lặp trước khi dùng hết chu kỳ. Snapshot câu hỏi đã tạo luôn giữ nguyên.
3. **Cửa sổ hạn chế lặp practice**: bao nhiêu ván gần đây theo tài khoản? Đề xuất tránh 5 đáp án gần nhất, nếu hết lựa chọn thì nới từ cũ nhất; không bao giờ nới việc loại daily hôm nay. Chưa chốt con số 5.
4. **Phạm vi danh sách đoán**: chỉ ứng viên đáp án đủ OVR/hồ sơ hay mọi cầu thủ có membership Premier League 2026/27 tại ngày câu hỏi? Đề xuất phương án mọi cầu thủ thuộc mùa tại ngày câu hỏi, gồm người OVR <75; giữ tập ID được phép ổn định cho ván để chuyển nhượng không làm đổi tính hợp lệ của lượt đoán. Cần chốt trước hợp đồng tìm/chọn và validation ID.
5. **Quyền xem BXH**: đặc tả yêu cầu login để chơi nhưng chưa quy định khách có được xem BXH. Đề xuất công khai tên hiển thị/điểm như BXH Fantasy hiện tại, không lộ thông tin tài khoản; cũng có thể chỉ cho người đã đăng nhập xem.

Các lựa chọn kỹ thuật như tên bảng, endpoint và cách phân trang có thể hoàn thiện khi triển khai, không cần hỏi lại mọi chi tiết. Đề xuất dùng kiểu đồng hạng hiện có `1,1,3`, sổ kết quả + SUM và lazy creation/expiry để giảm phần vận hành của bản đầu.

Bài thực hành tiếp theo khi bắt đầu bước 1: lập bảng tính tay cho các chuỗi mở gợi ý/đoán sai/đoán đúng, rồi tự viết test luật tính điểm theo hướng dẫn. Tiêu chí đạt: bao phủ đúng 0 điểm, sai lần ba và đầu vào không hợp lệ, không chỉ assert các hằng số.

## 15. Lượt triển khai backend local — 08/10/2026

Người dùng đã gửi `PrismaXI · Minigame (mockup) (1).html`, giao triển khai backend và kiểm thử logic local, chưa sửa frontend. Đồng thời chọn các mặc định đề xuất ở mục 14: tránh 5 ván practice gần nhất và nới từ cũ nhất khi cần; danh sách đoán gồm mọi membership hợp lệ tại ngày câu hỏi; BXH công khai; chu kỳ daily giữ queue đầu chu kỳ, bỏ người không còn hợp lệ và nhận ứng viên mới ở chu kỳ sau. Không còn cần hỏi lại bốn lựa chọn này. Mockup dùng để nối giao diện ở lượt sau, không làm nguồn dữ liệu thật hoặc thay luật.

Đã thêm package `minigame`, schema/profile H2 local riêng, API phục vụ bố cục và test luật/tích hợp/persistence. Selector chu kỳ được giản lược thành một dòng JSON có khóa transaction thay vì hai bảng cycle/items; câu hỏi daily đã dùng vẫn được lưu riêng, queue và đáp án trước sống qua restart. Danh sách cầu thủ đoán cũng snapshot ID/tên. Chi tiết endpoint, DTO, retry, local và phần chưa phát hành ở [tài liệu API](player-guess-minigame-api.md).

Người dùng cập nhật project sang JavaSE 26. Maven đã chạy JDK 26; POM vẫn release 21 và chưa sửa ở lượt Minigame. Bộ test tính điểm do Codex triển khai theo yêu cầu kiểm thử backend local trong lượt này; bài tính tay vẫn có thể dùng để người học luyện đối chiếu hành vi.

Không sửa frontend hoặc dữ liệu mùa 2024/25; không đọc/ghi production, commit, push hay deploy. Migration MySQL thật chưa áp dụng và cần kiểm chứng trên database thử ở bước phát hành.

Kiểm tra cuối: 46 test Minigame và 33 test tích hợp Fantasy/auth liên quan đều qua (79 test, 0 thất bại/lỗi). Toàn suite gần nhất còn 7 thất bại có sẵn; đã tái hiện cùng 7 trường hợp trên bản HEAD chưa có Minigame. Chi tiết môi trường, nguyên nhân và bằng chứng kiểm tra ở tài liệu API. Phần tiếp theo được đề xuất là nối frontend theo mockup và API, sau khi người dùng giao lượt đó.

## 16. Lượt nối frontend local — 08/10/2026

Người dùng đã giao làm giao diện frontend và nối backend. Tab/hash Minigame, bố cục theo mockup, daily/luyện tập, gợi ý/đoán/kết quả, lịch sử, BXH, đồng hồ server và phiên đăng nhập đã triển khai. Không dùng engine/dữ liệu mẫu của mockup làm nghiệp vụ hay roster.

Kiểm tra: 103 test frontend qua, build Vite qua; Chrome desktop/mobile với backend/H2 test riêng đã xác nhận luồng chơi, reload, bấm đúp, retry cùng action ID sau mất phản hồi, xung đột version, thắng ở 0 điểm, đăng xuất và sáng/tối. Chi tiết ở [hướng dẫn frontend](player-guess-minigame-frontend.md). Không sửa backend/POM, ghi production, commit, push hoặc deploy.

Còn chuẩn bị dữ liệu cầu thủ thật có căn cứ trên local nếu cần chơi bằng roster thực tế; trước phát hành vẫn cần migration và kiểm tra MySQL thử. Các lựa chọn đã chốt ở mục 15 giữ nguyên.

# Giai đoạn 1 — Java và CSV

## Lộ trình từng bước

1. `Club`: đối tượng câu lạc bộ, kiểm tra đầu vào, unit test và bài tập tìm tên.
2. `Player`: cầu thủ gắn với câu lạc bộ qua `clubId`.
3. `Match`: trận đấu, phân biệt trận chưa đá và đã có kết quả.
4. `Standing`: số trận, thắng/hòa/thua, bàn thắng/thua, hiệu số và điểm.
5. Chốt định dạng CSV và đọc dữ liệu, báo lỗi kèm dòng dữ liệu.
6. Tìm kiếm/lọc câu lạc bộ, cầu thủ và trận đấu.
7. Tính bảng xếp hạng từ trận đã kết thúc, có test điểm và thứ tự.
8. Bổ sung dữ liệu thống kê cầu thủ theo trận rồi tổng hợp và viết test.

Điểm số trận đấu không đủ để suy ra ai ghi bàn hoặc kiến tạo; bước thống kê cầu thủ cần dữ liệu riêng. Chưa triển khai Spring Boot hoặc MySQL ở giai đoạn này.

## Bài 1: Club

Code mẫu đã có trong `backend/src/main/java/com/premierhub/model/Club.java`.

- `model` chứa đối tượng nghiệp vụ, không đọc file hoặc in console.
- Constructor chỉ cho tạo `Club` với ID dương, tên và thành phố có nội dung.
- `private final` cùng getter cho phép đọc dữ liệu nhưng không thay đổi các trường sau khi khởi tạo.
- `requireText` dùng chung quy tắc kiểm tra, bỏ khoảng trắng ở hai đầu và giữ khoảng trắng bên trong.
- `IllegalArgumentException` báo rằng tham số người gọi truyền vào không hợp lệ.
- `App` chịu trách nhiệm định dạng và hiển thị; dữ liệu Sample United là dữ liệu giả để học.

Trong test, `assertEquals` so sánh kết quả với mong đợi. `assertThrows` xác nhận dữ liệu sai bị từ chối. `@ParameterizedTest` chạy cùng một quy tắc với nhiều đầu vào.

Chạy từ `backend/`:

```powershell
mvn clean test
mvn compile exec:java
```

Console mong đợi: `1 | Sample United | Sample City`.

## Implementation tìm theo tên

Thêm phương thức sau vào `Club` và viết test trong `ClubTest`:

```java
public boolean matchesName(String keyword)
```

Yêu cầu với câu lạc bộ có tên `Sample United`:

| Keyword | Kết quả |
| --- | --- |
| `united` | `true` |
| `SAMPLE` | `true` |
| `  united  ` | `true` |
| `City` | `false` |
| chuỗi rỗng hoặc chỉ khoảng trắng | `false` |
| `null` | `false` |

Gợi ý: kiểm tra `null` trước khi gọi phương thức trên chuỗi; dùng `isBlank()`, `strip()`, `toLowerCase(Locale.ROOT)` và `contains()`. `Locale.ROOT` giúp chuyển chữ thường nhất quán, không phụ thuộc ngôn ngữ máy chạy.

Phương thức này đã được triển khai đầy đủ cùng test.

## Sprint 1: Import Club từ CSV

Theo yêu cầu mới, chức năng import `Club` được thực hiện trước `Player`. Bài tập `matchesName` vẫn dành cho bạn, không phải điều kiện để dùng reader.

`ClubCsvReader` nằm trong package `com.premierhub.csv`. Phương thức `read(Path)` đọc file UTF-8 và trả `List<Club>` theo thứ tự dòng. Đây là import vào bộ nhớ, chưa lưu database.

Quy ước file:

- Header bắt buộc ở dòng đầu là `id,name,city`, phân biệt hoa/thường; cho phép khoảng trắng quanh tên cột và BOM đầu file.
- Mỗi dòng dữ liệu gồm đúng ba cột; bỏ qua dòng trắng, bỏ khoảng trắng hai đầu giá trị.
- ID phải là số nguyên 32-bit dương và không trùng trong cùng file. Tên/thành phố phải có nội dung, do constructor `Club` kiểm tra.
- File chỉ có header trả danh sách rỗng; file rỗng báo lỗi thiếu header.
- Đây là CSV đơn giản: không hỗ trợ trường có dấu ngoặc kép, dấu phẩy hoặc xuống dòng bên trong giá trị. Reader từ chối dấu ngoặc kép và sai số cột; không dùng bản này cho CSV tổng quát xuất từ công cụ khác nếu chưa kiểm tra định dạng.

Ví dụ giả để học nằm tại `backend/data/clubs.csv`.

```java
List<Club> clubs = new ClubCsvReader().read(Path.of("data", "clubs.csv"));
```

Đường dẫn này tính từ thư mục làm việc `backend/`. Nếu chạy ở gốc repo, dùng `Path.of("backend", "data", "clubs.csv")`.

Cách đọc code:

1. `Files.newBufferedReader` đọc lần lượt từng dòng bằng UTF-8. `try-with-resources` đóng file cả khi đọc thành công lẫn khi có exception.
2. `split(",", -1)` giữ cột rỗng cuối dòng: `1,United,` vẫn có ba cột và bị `Club` từ chối vì thiếu thành phố.
3. `Integer.parseInt` chuyển ID thành số, rồi constructor `Club` kiểm tra nghiệp vụ. Không lặp lại quy tắc của model trong reader.
4. `HashSet<Integer>` phát hiện ID trùng; `ArrayList<Club>` giữ thứ tự các câu lạc bộ đã đọc.
5. Nội dung sai gây `IllegalArgumentException` kèm số dòng thực trong file, kể cả dòng trắng. Import dừng tại lỗi đầu tiên, không trả danh sách nhập dở. Lỗi hệ thống file giữ kiểu `IOException` cho nơi gọi xử lý.

Test dùng `@TempDir` để tạo file riêng cho từng test; kiểm tra UTF-8, BOM, CRLF, header, dòng trắng, cột thiếu/thừa, ID sai/trùng, dữ liệu trống và file không tồn tại.

### Implementation nối reader vào App

Reader và phần tích hợp console đã hoàn chỉnh với các bước sau:

1. Trong `App.main`, thay câu lạc bộ tạo thủ công bằng lời gọi reader theo ví dụ trên.
2. Dùng vòng lặp `for` và `formatClub` đang có để in từng câu lạc bộ.
3. In tổng số câu lạc bộ đã import.
4. Bắt `IOException` và `IllegalArgumentException`, in thông báo lỗi ra `System.err`; chỉ in tổng số khi import thành công.

Các import cần dùng: `com.premierhub.csv.ClubCsvReader`, `java.nio.file.Path`, `java.io.IOException`; có thể dùng `var` hoặc thêm `java.util.List`.

Chạy từ `backend/`:

```powershell
mvn clean test
mvn compile exec:java
```

Ví dụ kết quả console:

```text
1 | Sample United | London
2 | Sample City | Manchester
3 | Sample Rovers | Liverpool
Imported 6 clubs
```

`App` hiện đọc đủ `clubs.csv`, `players.csv`, `matches.csv`, thực hiện tìm kiếm, hiển thị thống kê và in bảng xếp hạng. Xem README để chạy demo hoàn chỉnh.

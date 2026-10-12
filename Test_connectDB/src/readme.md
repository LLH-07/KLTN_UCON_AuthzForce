# README — Demo lưu policy trong Database và đánh giá bằng AuthzForce

## 1. Mục đích của demo

Demo này được xây dựng để trả lời câu hỏi:

- **Policy có thể lưu trong cơ sở dữ liệu hay không?**
- **AuthzForce đọc policy đó như thế nào để đánh giá quyền truy cập?**
- **Khi policy thay đổi trong database, hệ thống có thể nạp lại như thế nào?**

Trong bản demo này:

- **Policy** được lưu trong **PostgreSQL**.
- **AuthzForce Core** được nhúng trực tiếp vào ứng dụng Java để làm nhiệm vụ **đánh giá quyền**.
- Ứng dụng không tự viết `if/else` để quyết định `Permit` hay `Deny`, mà **gửi request cho AuthzForce** và nhận kết quả.

Nói ngắn gọn:

> **Database là nơi lưu policy và dữ liệu nghiệp vụ. AuthzForce là nơi đọc policy, so khớp request với policy, rồi trả kết quả đánh giá.**

---

## 2. Ý tưởng chính của hệ thống

Hệ thống có 3 phần:

### 2.1. Dữ liệu policy
Policy được lưu trong bảng:

- `public.xacml_policies`
- `public.xacml_root_pointer`

Trong đó:

- `xacml_policies.xml_body` chứa **toàn bộ nội dung policy ở dạng XML XACML**.
- `xacml_root_pointer` chỉ ra **policy gốc đang được dùng**.

### 2.2. Dữ liệu nghiệp vụ
Để dựng request thử nghiệm, demo tạo thêm các bảng:

- `public.users_demo`
- `public.books`
- `public.reading_sessions`

Đây là **bảng do demo tự tạo thêm**, không phải bảng bắt buộc của AuthzForce.

Chúng được dùng để mô phỏng hệ thống đọc sách, ví dụ:

- người dùng đang ở trạng thái nào
- có phải khách xem thử không
- sách còn hoạt động không
- giới hạn số chunk preview là bao nhiêu
- phiên đọc đang đi theo đường truy cập nào

### 2.3. Bộ máy đánh giá
AuthzForce Core được dùng như **PDP** (Policy Decision Point), tức là nơi đưa ra quyết định.

PDP sẽ:

1. nhận policy
2. nhận request
3. so khớp request với policy
4. trả kết quả như:
   - `Permit`
   - `Deny`

---

## 3. Những gì cần cài đặt

### 3.1. Phần mềm cần có

Cần cài các công cụ sau:

- **JDK 17**
- **Apache Maven**
- **PostgreSQL**
- **VSCode**
- **Extension Pack for Java** trong VSCode
- **pgAdmin** hoặc `psql` để chạy lệnh SQL

### 3.2. Thư viện Maven dùng trong project

Trong `pom.xml`, project dùng các dependency chính:

- `authzforce-ce-core-pdp-engine`
- `postgresql`
- `slf4j-simple` (khuyên thêm để hiển thị log dễ hơn)

Ý nghĩa:

- `authzforce-ce-core-pdp-engine`: cung cấp bộ máy AuthzForce Core
- `postgresql`: cho phép Java kết nối PostgreSQL
- `slf4j-simple`: giúp hiển thị log gọn hơn, tránh warning khó đọc

---

## 4. Cấu trúc thư mục gợi ý

```text
02/
  pom.xml
  src/
    main/
      java/
        com/
          demo/
            ebook/
              App.java
              Db.java
              RequestContextRepository.java
              AuthzforceEvaluator.java
              PolicyFingerprint.java
              PolicyMetadataRepository.java
              PolicyEngineManager.java
          authz/
            dbprovider/
              PostgresPolicyProviderDescriptor.java
              PostgresPolicyProviderFactory.java
              PostgresPolicyProvider.java
      resources/
        pdp.xml
        pdp-ext.xsd
        pdp-ext-base.xsd
        xacml-core-mini.xsd
        xml-catalog.xml
        META-INF/
          services/
            org.ow2.authzforce.core.pdp.api.PdpExtension
        sql/
          schema.sql
          data.sql
          policy_preview.xml
```

---

## 5. Câu trả lời ngắn gọn cho ba câu hỏi quan trọng

### 5.1. Policy được lưu ở đâu?

Policy được lưu trong:

- `public.xacml_policies.xml_body`

Dữ liệu ở cột này là **chuỗi XML XACML đầy đủ**.

### 5.2. AuthzForce đọc policy ở đâu?

AuthzForce đọc policy thông qua file:

- `PostgresPolicyProvider.java`

File này truy vấn database để lấy XML policy, sau đó chuyển XML đó thành đối tượng `PolicySet`.

### 5.3. AuthzForce đánh giá request ở đâu?

Việc đánh giá diễn ra trong:

- `AuthzforceEvaluator.java`

Cụ thể, file này tạo request và gọi:

- `BasePdpEngine.evaluate(request)`

---

## 6. Luồng hoạt động tổng quát

Quy trình chạy của demo như sau:

1. Chạy ứng dụng bằng `mvn exec:java`
2. `App.java` khởi động chương trình
3. `PolicyEngineManager` nạp policy hiện tại từ database và khởi tạo engine của AuthzForce
4. Khi có yêu cầu đánh giá, `AuthzforceEvaluator` lấy dữ liệu nghiệp vụ từ database
5. `AuthzforceEvaluator` dựng request đưa vào AuthzForce
6. AuthzForce dùng policy đã nạp để đánh giá
7. Hệ thống trả về `Permit` hoặc `Deny`
8. `App.java` in kết quả ra màn hình

Nói đơn giản hơn:

> **Database cung cấp policy và dữ liệu đầu vào. AuthzForce dùng các dữ liệu đó để ra quyết định.**

---

## 7. Sơ đồ dễ hiểu

```text
[App.java]
    |
    v
[PolicyEngineManager] --------------------> [PostgreSQL]
    |                                          |
    |                                          |- xacml_policies
    |                                          |- xacml_root_pointer
    |                                          |- users_demo
    |                                          |- books
    |                                          |- reading_sessions
    |
    v
[BasePdpEngine của AuthzForce]
    ^
    |
[AuthzforceEvaluator.java] <----- [RequestContextRepository.java]
    |
    v
[Decision: Permit / Deny]
```

---

## 8. Giải thích vai trò từng file

## 8.1. `App.java`

### Vai trò
Là điểm bắt đầu của chương trình.

### Làm gì?

- tạo `PolicyEngineManager`
- khởi tạo engine AuthzForce một lần
- bật chế độ kiểm tra policy thay đổi theo chu kỳ (nếu muốn)
- tạo `AuthzforceEvaluator`
- cung cấp lệnh thử nghiệm như:
  - `eval 1 3`
  - `reload`
  - `status`
  - `quit`

### Có tự đánh giá quyền không?
Không.

`App.java` chỉ gọi evaluator và in kết quả.

---

## 8.2. `Db.java`

### Vai trò
Quản lý kết nối tới PostgreSQL.

### Làm gì?

- khai báo JDBC URL
- khai báo user/password
- trả về `Connection`

### Vì sao quan trọng?
Vì tất cả các file cần đọc database đều dùng `Db.java`.

---

## 8.3. `RequestContextRepository.java`

### Vai trò
Lấy **dữ liệu nghiệp vụ** từ database để dựng request.

### Dữ liệu lấy ra gồm gì?

- trạng thái user
- user có phải khách xem thử hay không
- trạng thái sách
- giới hạn preview
- đường truy cập của phiên đọc

### Lấy từ bảng nào?

- `public.users_demo`
- `public.books`
- `public.reading_sessions`

### Có đánh giá quyền không?
Không.

File này chỉ đọc dữ liệu để chuẩn bị request.

---

## 8.4. `AuthzforceEvaluator.java`

### Vai trò
Là nơi **gọi AuthzForce để đánh giá**.

### Làm gì?

1. gọi `RequestContextRepository` để lấy dữ liệu nghiệp vụ
2. chuyển dữ liệu đó thành request theo dạng thuộc tính
3. đưa request cho `BasePdpEngine`
4. nhận kết quả `Permit` hoặc `Deny`

### Đây có phải nơi đánh giá chính không?
Có.

Nếu cần trả lời rất ngắn gọn rằng “AuthzForce đánh giá ở đâu?”, thì câu trả lời là:

> **Trong `AuthzforceEvaluator.java`, tại đoạn gọi `pdp.evaluate(request)`.**

---

## 8.5. `PostgresPolicyProviderDescriptor.java`

### Vai trò
Là lớp cấu hình cho custom policy provider.

### Chứa gì?

- `jdbcUrl`
- `jdbcUser`
- `jdbcPassword`

### Tại sao cần?
Vì trong `pdp.xml`, AuthzForce cần biết thông tin kết nối DB để custom provider dùng.

---

## 8.6. `PostgresPolicyProviderFactory.java`

### Vai trò
Tạo ra `PostgresPolicyProvider` thật sự.

### Tại sao không tạo trực tiếp luôn?
Vì cơ chế extension của AuthzForce yêu cầu custom provider phải có **factory** riêng.

### Hiểu đơn giản
File này giống như “xưởng tạo provider”.

---

## 8.7. `PostgresPolicyProvider.java`

### Vai trò
Đây là file quan trọng nhất ở phía policy.

### Làm gì?

1. đọc root policy hiện tại từ database
2. lấy nội dung XML policy
3. biến XML đó thành đối tượng `PolicySet`
4. giao lại cho built-in `StaticPolicyProvider` của AuthzForce xử lý tiếp

### Nói ngắn gọn
Đây là nơi trả lời cho câu hỏi:

> **“AuthzForce lấy policy từ database như thế nào?”**

### Có tự trả `Permit` hay `Deny` không?
Không trực tiếp.

Nó chỉ cung cấp policy cho AuthzForce.

---

## 8.8. `PolicyFingerprint.java`

### Vai trò
Lưu “dấu vân tay” của policy hiện tại.

### Gồm gì?

- `rootPolicyId`
- `rootPolicyVersion`
- `updatedAt`

### Dùng để làm gì?
Dùng để so sánh xem policy hiện tại trong DB có khác với policy đang nằm trong bộ nhớ hay không.

---

## 8.9. `PolicyMetadataRepository.java`

### Vai trò
Đọc **metadata nhẹ** của policy trong database.

### Tại sao cần?
Để hệ thống biết policy có đổi không mà **không cần đọc lại toàn bộ XML ở mỗi lần kiểm tra**.

### Ý tưởng
Nếu `updated_at` đổi, nghĩa là policy đã thay đổi.

---

## 8.10. `PolicyEngineManager.java`

### Vai trò
Đây là nơi quản lý vòng đời của engine AuthzForce.

### Làm gì?

- nạp engine lần đầu khi app khởi động
- giữ engine trong RAM để tái sử dụng
- kiểm tra policy có thay đổi không
- reload engine tự động nếu policy đổi
- reload engine thủ công khi cần

### Vì sao file này rất quan trọng?
Vì nó giúp tránh việc:

- mỗi request lại phải nạp lại policy
- mỗi request lại phải tạo lại cả engine

### Tư tưởng chính

> **Nạp một lần, dùng nhiều lần. Chỉ reload khi policy đổi hoặc khi người quản trị yêu cầu.**

---

## 8.11. `pdp.xml`

### Vai trò
Là file cấu hình chính của PDP AuthzForce.

### Chứa gì?

- khai báo custom policy provider
- khai báo root policy đang dùng

### Ý nghĩa
Nó giống như “bản hướng dẫn” để AuthzForce biết:

- dùng provider nào
- bắt đầu đánh giá từ policy nào

---

## 8.12. `pdp-ext.xsd`

### Vai trò
Định nghĩa kiểu XML cho custom provider của bạn.

### Ý nghĩa
Nó giúp AuthzForce hiểu rằng trong `pdp.xml`, kiểu `PostgresPolicyProviderDescriptor` là hợp lệ.

---

## 8.13. `pdp-ext-base.xsd`

### Vai trò
Là file hỗ trợ để demo chạy ổn khi nạp schema.

### Lưu ý
Đây là file hỗ trợ cho demo, giúp đơn giản hóa quá trình kiểm tra schema khi chạy local.

---

## 8.14. `xacml-core-mini.xsd`

### Vai trò
Là schema tối thiểu cho phần XACML mà `pdp.xml` cần tham chiếu.

### Lưu ý
Nó không phải toàn bộ schema XACML đầy đủ, mà chỉ là phần vừa đủ để demo hoạt động.

---

## 8.15. `xml-catalog.xml`

### Vai trò
Giúp ánh xạ các schema mà AuthzForce cần tìm khi khởi động.

### Nếu thiếu file này thì sao?
Thường sẽ gặp lỗi kiểu không tìm thấy XSD hoặc không resolve được type.

---

## 8.16. `META-INF/services/org.ow2.authzforce.core.pdp.api.PdpExtension`

### Vai trò
Đăng ký custom extension để AuthzForce nhìn thấy `PostgresPolicyProviderFactory`.

### Nếu thiếu file này thì sao?
AuthzForce sẽ không biết custom provider của bạn tồn tại.

---

## 8.17. `schema.sql`

### Vai trò
Tạo toàn bộ bảng cho demo.

### Bao gồm

- bảng policy
- bảng root policy
- bảng user/book/session
- cột `updated_at` để hỗ trợ reload

---

## 8.18. `data.sql`

### Vai trò
Nạp dữ liệu mẫu ban đầu.

### Bao gồm

- user mẫu
- sách mẫu
- session mẫu
- policy XML mẫu
- root policy pointer

---

## 8.19. `policy_preview.xml`

### Vai trò
Chứa policy XML mẫu trước khi chèn vào database.

### Lưu ý quan trọng
Trong phần `Condition`, các `AttributeDesignator` phải được bọc bằng:

- `string-one-and-only`
- `boolean-one-and-only`
- `integer-one-and-only`

Lý do là vì trong `Condition`, giá trị lấy ra là kiểu “bag”, còn các hàm so sánh như `string-equal` cần một giá trị đơn.

---

## 9. Policy lưu trong DB như thế nào?

Policy được lưu bằng cách:

- tạo một bản XML XACML hoàn chỉnh
- lưu toàn bộ chuỗi XML đó vào cột `xml_body`

Ví dụ:

- `policy_id = root-preview-policy`
- `version_no = 1.0`
- `xml_body = toàn bộ XML policy`

Như vậy, cơ sở dữ liệu đóng vai trò như **kho chứa policy**.

---

## 10. AuthzForce đọc policy từ DB như thế nào?

Quy trình đọc policy như sau:

1. AuthzForce đọc `pdp.xml`
2. thấy có custom `policyProvider`
3. tìm `PostgresPolicyProviderFactory`
4. factory tạo `PostgresPolicyProvider`
5. `PostgresPolicyProvider` truy vấn bảng:
   - `public.xacml_root_pointer`
   - `public.xacml_policies`
6. lấy `xml_body`
7. parse XML thành `PolicySet`
8. giao `PolicySet` đó cho built-in `StaticPolicyProvider`

Nói ngắn gọn:

> **AuthzForce không đọc XML policy trực tiếp từ file policy riêng trong demo này. Nó đọc policy từ DB thông qua custom provider.**

---

## 11. AuthzForce đánh giá ở đâu và dùng file nào?

### 11.1. Đánh giá ở đâu?
Trong:

- `AuthzforceEvaluator.java`

Cụ thể là đoạn:

- tạo request
- gọi `pdp.evaluate(request)`

### 11.2. Những file trực tiếp tham gia vào quá trình đánh giá

- `App.java`
- `PolicyEngineManager.java`
- `AuthzforceEvaluator.java`
- `RequestContextRepository.java`
- `Db.java`
- `pdp.xml`
- `PostgresPolicyProviderFactory.java`
- `PostgresPolicyProvider.java`
- `META-INF/services/...PdpExtension`

### 11.3. Những bảng DB tham gia

- `public.xacml_policies`
- `public.xacml_root_pointer`
- `public.users_demo`
- `public.books`
- `public.reading_sessions`

---

## 12. Có cần Docker AuthzForce không?

Không, với bản demo hiện tại thì **không cần**.

### Vì sao?
Vì demo này dùng:

- **AuthzForce Core nhúng trực tiếp trong ứng dụng Java**

không dùng:

- **AuthzForce Server chạy riêng bằng Docker**

Nói dễ hiểu:

- ứng dụng Java của bạn tự mang theo AuthzForce Core
- khi chạy `mvn exec:java`, AuthzForce chạy trong chính process Java đó

---

## 13. Cơ chế nạp policy: hiện tại và hướng tối ưu

## 13.1. Cách đơn giản nhưng tốn chi phí
Nếu mỗi request đều tạo lại `BasePdpEngine`, thì:

- mỗi request lại đọc policy
- mỗi request lại parse policy
- mỗi request lại build engine

Cách này dễ hiểu nhưng tốn chi phí.

## 13.2. Cách tốt hơn
Bản cải tiến dùng `PolicyEngineManager` để:

- nạp engine một lần
- giữ engine trong bộ nhớ
- chỉ reload khi policy đổi hoặc khi người dùng yêu cầu

Đây là cách hợp lý hơn cho hệ thống thật.

---

## 14. Manual reload là gì?

Manual reload nghĩa là **người quản trị chủ động yêu cầu hệ thống nạp lại policy**.

Có thể thực hiện bằng nhiều cách:

### Cách 1. Gọi trực tiếp trong code
```java
manager.manualReload();
```

### Cách 2. Dùng lệnh console
Ví dụ gõ:

```text
reload
```

### Cách 3. Sau này nếu có web app
Tạo API kiểu:

```text
POST /admin/reload-policy
```

---

## 15. Auto reload hoạt động như thế nào?

Auto reload nghĩa là hệ thống **tự kiểm tra xem policy có đổi không**.

Trong demo này, cứ sau một khoảng thời gian nhất định, scheduler sẽ làm:

1. đọc fingerprint hiện tại từ database
2. so với fingerprint đang nằm trong RAM
3. nếu giống nhau thì bỏ qua
4. nếu khác nhau thì build engine mới
5. chỉ khi build thành công mới thay engine cũ

### Điểm rất quan trọng
Engine cũ **không bị xóa trước**.

Điều này giúp tránh trường hợp:

- policy mới bị lỗi XML
- policy mới sai cú pháp
- policy mới sai cấu trúc

khi đó engine cũ vẫn còn hoạt động.

---

## 16. Cập nhật DB thế nào để hệ thống tự nhận ra?

Có hai trường hợp thường gặp:

### Trường hợp 1. Sửa đè policy hiện tại

Ví dụ:

- vẫn là `policy_id = root-preview-policy`
- vẫn là `version_no = 1.0`
- chỉ thay `xml_body`

Khi đó, `updated_at` đổi, nên fingerprint đổi, hệ thống tự reload.

### Trường hợp 2. Đổi sang version mới

Ví dụ:

- policy mới có `version_no = 1.1`
- đồng thời sửa `public.xacml_root_pointer`

Khi đó fingerprint cũng đổi, hệ thống sẽ reload.

---

## 17. Lệnh thử nghiệm trong App

Trong bản có console, có thể dùng các lệnh:

### `status`
Xem fingerprint hiện tại của policy đang dùng.

### `eval <sessionId> <requestedChunk>`
Ví dụ:

```text
eval 1 3
```

nghĩa là:

- dùng `session_id = 1`
- thử xin đọc `requested_chunk = 3`
- hệ thống sẽ in `Permit` hoặc `Deny`

### `reload`
Yêu cầu manual reload.

### `quit`
Thoát ứng dụng.

---

## 18. Cách chạy từ đầu đến cuối

## Bước 1. Build project

```powershell
mvn clean compile -e
```

## Bước 2. Chạy app

```powershell
mvn exec:java
```

## Bước 3. Kiểm tra trạng thái

Trong console của app, gõ:

```text
status
```

## Bước 4. Test quyết định ban đầu

```text
eval 1 3
eval 1 8
eval 2 2
eval 3 4
eval 4 1
```

### Kết quả kỳ vọng

- `eval 1 3` → `Permit`
- `eval 1 8` → `Deny`
- `eval 2 2` → `Deny`
- `eval 3 4` → `Permit`
- `eval 4 1` → `Deny`

---

## 19. Cách test khi database update

Giữ app đang chạy.

### 19.1. Ví dụ đổi policy để từ PREVIEW thành FREE

Mở PostgreSQL và chạy:

```sql
update public.xacml_policies
set xml_body = replace(xml_body, '>PREVIEW<', '>FREE<')
where policy_id = 'root-preview-policy'
  and version_no = '1.0';
```

### Điều gì xảy ra?

Trước đó policy yêu cầu:

- `env:access_path = PREVIEW`

Sau khi sửa, policy yêu cầu:

- `env:access_path = FREE`

Nếu `session 1` đang dùng `PREVIEW`, thì:

- trước khi reload: `eval 1 3` có thể là `Permit`
- sau khi reload: `eval 1 3` sẽ thành `Deny`

### 19.2. Nếu dùng auto reload
Chờ theo chu kỳ kiểm tra, ví dụ 10 giây hoặc 60 giây.

### 19.3. Nếu muốn reload ngay
Trong app gõ:

```text
reload
```

### 19.4. Test lại

```text
eval 1 3
```

---

## 20. Cách khôi phục policy cũ để test lại

Nếu muốn đổi từ `FREE` về `PREVIEW`, chạy:

```sql
update public.xacml_policies
set xml_body = replace(xml_body, '>FREE<', '>PREVIEW<')
where policy_id = 'root-preview-policy'
  and version_no = '1.0';
```

Sau đó:

- chờ auto reload
- hoặc gõ `reload`

rồi test lại:

```text
eval 1 3
```

---

## 21. Những lỗi thường gặp

### 21.1. Lỗi schema XML
Nguyên nhân thường là:

- thiếu `xml-catalog.xml`
- thiếu `pdp-ext-base.xsd`
- thiếu `xacml-core-mini.xsd`
- `accessExternalSchema` chưa bật

### 21.2. Lỗi không thấy bảng
Nguyên nhân thường là:

- chưa chạy `schema.sql`
- đang vào sai DB
- đang ở sai schema

### 21.3. Lỗi policy XML sai
Ví dụ thường gặp là:

- quên `string-one-and-only`
- quên `boolean-one-and-only`
- quên `integer-one-and-only`

### 21.4. Lỗi extension không load
Nguyên nhân thường là:

- thiếu file `META-INF/services/...PdpExtension`
- khai báo sai class factory

---

## 22. Giới hạn của demo hiện tại

Đây là một **bản demo kỹ thuật** để chứng minh ý tưởng:

- policy có thể lưu trong DB
- AuthzForce có thể đọc policy đó để đánh giá
- hệ thống có thể reload policy khi policy thay đổi

Nó chưa phải là một hệ thống production hoàn chỉnh.

Ví dụ:

- schema XSD đang dùng là bản hỗ trợ cho demo
- built-in static provider đang được dùng làm delegate
- cơ chế repository policy còn đơn giản

Tuy nhiên, xét về mặt nghiên cứu và mô phỏng nguyên lý, demo này đã trả lời được câu hỏi chính của bài toán.

---

## 23. Kết luận

Bản demo này cho thấy rằng:

1. **Policy hoàn toàn có thể lưu trong database** dưới dạng XML XACML.
2. **AuthzForce có thể đọc policy từ database** thông qua custom policy provider.
3. **Việc đánh giá quyền diễn ra trong AuthzForce**, không phải bằng `if/else` viết tay trong ứng dụng.
4. **Engine có thể được nạp một lần và tái sử dụng**, không cần khởi tạo lại ở mỗi request.
5. **Hệ thống có thể hỗ trợ cả manual reload và auto reload** khi policy trong DB thay đổi.

Nói cách ngắn nhất:

> **Database giữ policy. AuthzForce đọc policy. Ứng dụng chỉ chuẩn bị dữ liệu và nhận kết quả đánh giá.**

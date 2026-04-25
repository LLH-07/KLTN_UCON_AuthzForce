
# I. AuthzForce đọc policy bằng file nào?
Theo luồng file:
1. `pdp.xml` nói rằng phải dùng custom provider nào
2. `META-INF/services/...PdpExtension` giúp AuthzForce tìm thấy `PostgresPolicyProviderFactory`
3. `PostgresPolicyProviderFactory.java` tạo `PostgresPolicyProvider`
4. `PostgresPolicyProvider.java` đọc xml_body từ DB
5. `StaticPolicyProvider` built-in nhận `PolicySet` đó và biến thành runtime evaluator

`StaticPolicyProvider` descriptor của AuthzForce được thiết kế để nhận PolicySet hoặc `policyLocation`    

# II. AuthzForce đánh giá request ở đâu?
Đánh giá request ở `AuthzforceEvaluator.java`. Cụ thể ở:
- `PdpEngineConfiguration.getInstance(...)`
- `new BasePdpEngine(...)`
- `newRequestBuilder(...)`
- `pdp.evaluate(request)`

# III. App dùng file nào để đánh giá?
Các file dùng để đánh giá:
- `App.java`
- `AuthzforceEvaluator.java`
- `pdp.xml`
- `PostgresPolicyProviderFactory.java`
- `PostgresPolicyProvider.java`
- `RequestContextRepository.java`
- `Db.java`

và phía dữ liệu:
- `public.xacml_policies`
- `public.xacml_root_pointer`
- `public.users_demo`
- `public.books`
- `public.reading_sessions`

# IV. Luồng vận hành (ở bản demo này)
Luồng vận hành ở bản demo này:
1. Chạy `mvn exec:java`
2. Maven gọi `App.main()`
3. `App.java` gọi `evaluator.evaluate(sessionId, requestedChunk)`
4. `AuthzforceEvaluator` nạp:
   - `pdp.xml`
   - `xml-catalog.xml`
   - `pdp-ext.xsd`
5. AuthzForce đọc cấu hình trong `pdp.xml` và thấy có custom `<policyProvider>`
6. AuthzForce dùng `ServiceLoader` để tìm `PostgresPolicyProviderFactory` qua `META-INF/services/...PdpExtension`
7. Factory tạo `PostgresPolicyProvider`
8. `PostgresPolicyProvider` đọc root policy XML từ:
   - `public.xacml_root_pointer`
   - `public.xacml_policies`
9. Policy XML được parse thành `PolicySet`, rồi đưa vào built-in `StaticPolicyProvider`
10. `RequestContextRepository` đọc business data từ:
    - `session`
    - `user`
    - `book`
11. `AuthzforceEvaluator` biến dữ liệu đó thành XACML request gồm:
    - `subject attrs`
    - `resource attrs`
    - `environment attrs`
    - `action id`
12. AuthzForce so khớp request với policy theo:
    - `target`
    - `condition`
    - `rule`
    - `combining algorithm`
13. PDP trả về `Permit` hoặc `Deny`
14. `App.java` in kết quả ra console

# V. Sơ đồ tổng quát
```
                            [App.java]
                                |
                                v
                            [AuthzforceEvaluator.java]
                                | 1) đọc cấu hình PDP
                                | 2) dựng request từ DB nghiệp vụ
                                v
                            [PdpEngineConfiguration + BasePdpEngine]
                                |
                                | đọc pdp.xml
                                v
                            [Custom Policy Provider]
                            (PostgresPolicyProviderFactory -> PostgresPolicyProvider)
                                |
                                | query policy XML
                                v
                            [PostgreSQL]
                            - public.xacml_policies
                            - public.xacml_root_pointer
                            - public.users_demo
                            - public.books
                            - public.reading_sessions
                                |
                                | policy XML
                                v
                            [Built-in StaticPolicyProvider của AuthzForce]
                                |
                                | parse/compile policy
                                v
                            [AuthzForce PDP evaluate(request)]
                                |
                                v
                            [Decision: Permit / Deny]
                                |
                                v
                            [App.java in kết quả]
```
<br>

# VI. Bảng luồng chi tiết
| Bước | Thành phần                   | File / Bảng                                                      | Đầu vào                                         | Xử lý                                                                                      | Đầu ra                             | Ghi chú                                                                              |
| ---- | ---------------------------- | ---------------------------------------------------------------- | ----------------------------------------------- | ------------------------------------------------------------------------------------------ | ---------------------------------- | ------------------------------------------------------------------------------------ |
| 1    | Điểm bắt đầu                 | `App.java`                                                       | `sessionId`, `requestedChunk`                   | Gọi evaluator                                                                              | yêu cầu đánh giá                   | `App.java` không tự quyết định quyền                                                 |
| 2    | Bộ điều phối đánh giá        | `AuthzforceEvaluator.java`                                       | session + chunk                                 | Nạp `pdp.xml`, `xml-catalog.xml`, `pdp-ext.xsd`; tạo `BasePdpEngine`; dựng request         | `DecisionRequest`                  | Đây là nơi **AuthzForce được gọi để đánh giá**                                       |
| 3    | Cấu hình PDP                 | `pdp.xml`                                                        | cấu hình XML                                    | Khai báo `policyProvider` và `rootPolicyRef`                                               | cấu hình cho PDP                   | `rootPolicyRef` xác định policy gốc; `policyProvider` là nơi resolve policy đó       |
| 4    | Cơ chế extension             | `META-INF/services/org.ow2.authzforce.core.pdp.api.PdpExtension` | tên class factory                               | AuthzForce dùng ServiceLoader để tìm extension                                             | tìm thấy custom factory            | Không có file này thì custom provider không được load                                |
| 5    | Factory tạo policy provider  | `PostgresPolicyProviderFactory.java`                             | JAXB config từ `pdp.xml`                        | Tạo `PostgresPolicyProvider`                                                               | policy provider runtime            | Custom policy provider của AuthzForce phải có factory riêng                          |
| 6    | Custom policy provider       | `PostgresPolicyProvider.java`                                    | JDBC URL/user/password + root policy id/version | Query DB để lấy `xml_body`, parse XML thành `PolicySet`, rồi delegate sang static provider | policy runtime evaluator           | Đây là chỗ **đọc policy từ DB**                                                      |
| 7    | Kho policy                   | `public.xacml_policies`                                          | `policy_id`, `version_no`                       | Lưu **toàn bộ XACML PolicySet XML** trong `xml_body`                                       | XML policy                         | Đây là cách “lưu policy trong DB”                                                    |
| 8    | Trỏ policy gốc               | `public.xacml_root_pointer`                                      | root policy id/version                          | Xác định policy gốc đang dùng                                                              | root policy record                 | Dùng để custom provider biết lấy policy nào                                          |
| 9    | Built-in provider            | `StaticPolicyProvider` của AuthzForce                            | `PolicySet` hoặc `policyLocation`               | Parse/compile policy thành evaluator                                                       | `TopLevelPolicyElementEvaluator`   | AuthzForce built-in provider nhận `PolicySet` hoặc `policyLocation`                  |
| 10   | Repository dữ liệu nghiệp vụ | `RequestContextRepository.java`                                  | `sessionId`                                     | Join `users_demo`, `books`, `reading_sessions`                                             | subject/resource/environment attrs | Đây là nơi lấy dữ liệu để dựng request                                               |
| 11   | Kho dữ liệu nghiệp vụ        | `public.users_demo`, `public.books`, `public.reading_sessions`   | dữ liệu user/book/session                       | Cung cấp thuộc tính cho request                                                            | raw business data                  | Không chứa logic đánh giá                                                            |
| 12   | PDP thực thi                 | `BasePdpEngine.evaluate(request)`                                | request + policy evaluator                      | So khớp Target, Condition, Rule                                                            | `Permit` / `Deny`                  | `PolicyProvider#get(...)` trả `TopLevelPolicyElementEvaluator` để PDP dùng đánh giá  |
| 13   | Kết quả cuối                 | `App.java`                                                       | decision                                        | in console                                                                                 | `Permit` / `Deny`                  | Chỉ hiển thị kết quả                                                                 |
<br>

# VII. Bảng vai trò từng file
| File                                    | Vai trò chính                                           | Có đánh giá quyền không?                                    |
| --------------------------------------- | ------------------------------------------------------- | ----------------------------------------------------------- |
| `App.java`                              | chạy demo, gọi evaluator, in kết quả                    | Không                                                       |
| `AuthzforceEvaluator.java`              | tạo PDP, dựng request, gọi `evaluate()`                 | Có, đây là nơi gọi AuthzForce                               |
| `Db.java`                               | mở kết nối PostgreSQL                                   | Không                                                       |
| `RequestContextRepository.java`         | lấy dữ liệu user/book/session                           | Không                                                       |
| `PostgresPolicyProviderDescriptor.java` | map XML config từ `pdp.xml`                             | Không                                                       |
| `PostgresPolicyProviderFactory.java`    | tạo custom provider runtime                             | Không trực tiếp                                             |
| `PostgresPolicyProvider.java`           | đọc policy XML từ DB, giao cho built-in static provider | Không trực tiếp ra Permit/Deny, nhưng là đầu vào quyết định |
| `pdp.xml`                               | cấu hình PDP                                            | Không                                                       |
| `pdp-ext.xsd`                           | định nghĩa custom provider type                         | Không                                                       |
| `xml-catalog.xml`                       | map schema/catalog                                      | Không                                                       |
| `META-INF/services/...PdpExtension`     | đăng ký extension                                       | Không                                                       |
<br>

# VII. Bảng "Policy nằm đâu, đánh giá ở đâu?"
| Câu hỏi                             | Câu trả lời                                                                                                                  |
| ----------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| Policy lưu ở đâu?                   | Trong `public.xacml_policies.xml_body` dưới dạng **XACML PolicySet XML**                                                     |
| Root policy nằm ở đâu?              | `public.xacml_root_pointer` và đồng thời được chỉ trong `pdp.xml` qua `rootPolicyRef`                                        |
| AuthzForce đọc policy ở đâu?        | `PostgresPolicyProvider.java` query DB, lấy XML policy, parse thành `PolicySet`, rồi đưa cho built-in `StaticPolicyProvider` |
| AuthzForce đánh giá ở đâu?          | Trong `AuthzforceEvaluator.java`, tại đoạn tạo `BasePdpEngine` và gọi `pdp.evaluate(request)`                                |
| App có tự if/else quyết định không? | Không. App chỉ gọi evaluator và in kết quả |
<br>                                                                                   

# VIII. Bảng luồng dữ liệu theo loại
| Loại dữ liệu           | Nơi xuất phát                              | Đi qua đâu                                          | Dùng để làm gì       |
| ---------------------- | ------------------------------------------ | --------------------------------------------------- | -------------------- |
| Policy XML             | `public.xacml_policies`                    | `PostgresPolicyProvider` -> `StaticPolicyProvider`  | tạo policy evaluator |
| Root policy id/version | `public.xacml_root_pointer` hoặc `pdp.xml` | `PostgresPolicyProvider` / `PdpEngineConfiguration` | xác định policy gốc  |
| Business data          | `users_demo`, `books`, `reading_sessions`  | `RequestContextRepository`                          | dựng request         |
| XACML Request          | `AuthzforceEvaluator`                      | `BasePdpEngine`                                     | đầu vào cho PDP      |
| Decision               | `BasePdpEngine.evaluate()`                 | `App.java`                                          | in `Permit/Deny`     |

# Giải thích toàn bộ file `ebook-ucon-way1-ver3.0.xml`

Tài liệu này giải thích file XACML theo kiểu **người mới bắt đầu hoàn toàn**.  
Mục tiêu là để bạn trả lời được 5 câu hỏi:

1. XACML là gì?
2. File này đang quyết định chuyện gì?
3. Mỗi khối `PolicySet`, `Policy`, `Target`, `Rule`, `Condition`, `VariableDefinition` làm gì?
4. Luồng chạy PRE → ONGOING → POST nối với nhau ra sao?
5. Mỗi thuộc tính trong request nghĩa là gì và được dùng ở đâu?

---

## 1) XACML là gì?

XACML là ngôn ngữ viết **chính sách ra quyết định**.

Nó không trực tiếp:
- tạo session trong database
- ghi log trong database
- pause giao diện
- đóng session

Nó chỉ làm việc sau:

- nhận **Request** từ backend / PEP
- đọc các **attribute** trong request
- so với các **rule** trong policy
- trả về **Decision**
  - `Permit`
  - `Deny`
- và có thể kèm theo **Obligations** để backend/PEP bắt buộc làm tiếp

Trong file này:
- `Permit` nghĩa là cho phép tiếp tục luồng đọc ở pha tương ứng
- `Deny` nghĩa là chặn
- `ObligationExpression` là chỉ thị cho backend, ví dụ:
  - tạo `reading_session`
  - tạo `ad log`
  - `pause` session
  - `revoke` session
  - `resume` session
  - `write-usage-record`
  - `close-session`

---

## 2) File này đang điều khiển nghiệp vụ gì?

File này mô hình hóa quyền đọc ebook theo UCON với 3 pha:

- **PRE**: trước khi bắt đầu đọc
- **ONGOING**: trong lúc đang đọc
- **POST**: khi kết thúc đọc

Và chia theo loại tài nguyên:

- `book_volume`: sách / tập sách
- `reading_session`: phiên đọc đang tồn tại

### Quy tắc lớn

- Ở **PRE**:
  - nếu là `PREVIEW` thì chỉ được đọc trong giới hạn preview
  - nếu là `FULL` thì phải có quyền đọc full
  - nếu có quyền đọc full nhưng còn thiếu quảng cáo đầu sách thì vẫn bị chặn

- Ở **ONGOING**:
  - nếu session rental mà thuê hết hạn thì revoke
  - nếu đến hạn quảng cáo định kỳ mà chưa xong thì pause
  - nếu vừa hoàn tất quảng cáo định kỳ thì resume
  - nếu không có gì chặn thì đọc tiếp bình thường

- Ở **POST**:
  - nếu là session rental thì ghi usage record rồi đóng session
  - nếu không phải rental thì chỉ đóng session

---

## 3) Từ điển cú pháp XACML dùng trong file

### `PolicySet`
Là cái hộp lớn chứa policy hoặc policyset con.

### `Policy`
Là nhóm rule cùng mục đích.

### `Target`
Là bộ lọc xem request có thuộc phạm vi của policy/policyset đó không.

### `Rule`
Là luật cụ thể. Mỗi rule có:
- `RuleId`
- `Effect="Permit"` hoặc `Effect="Deny"`

### `Condition`
Là biểu thức logic của rule.

### `Apply`
Là lời gọi hàm logic:
- `and`
- `or`
- `not`
- `string-equal`
- `boolean-one-and-only`
- `integer-less-than-or-equal`
- ...

### `AttributeDesignator`
Lấy một attribute từ request.

### `AttributeValue`
Giá trị viết cứng trong policy.

### `VariableDefinition`
Định nghĩa một biểu thức logic có tên để dùng lại.

### `VariableReference`
Gọi lại biến logic đã định nghĩa.

### `ObligationExpression`
Việc backend/PEP bắt buộc phải làm khi quyết định được trả về.

---

## 4) Luồng chạy tổng quát của hệ thống

### Bước 1: người dùng yêu cầu đọc sách
Backend tạo request PRE với:
- `resource:resource_type = book_volume`
- `action:id = read`
- `environment:phase = PRE`
- `environment:requested_read_scope = PREVIEW` hoặc `FULL`
- và các thuộc tính quyền / quảng cáo tương ứng

### Bước 2: PDP đánh giá policy PRE
Kết quả có thể là:
- `Permit` preview
- `Deny` preview
- `Permit` full + obligation tạo session
- `Deny` full + obligation tạo ad log

### Bước 3: backend thực hiện obligation
Nếu permit full:
- backend tạo `reading_session`

Nếu deny vì thiếu quảng cáo:
- backend tạo `ad_view_log` / trạng thái chờ quảng cáo

### Bước 4: trong lúc đang đọc
Backend gửi request ONGOING với:
- `resource:resource_type = reading_session`
- `action:id = read`
- `environment:phase = ONGOING`
- các trạng thái session / thuê / quảng cáo định kỳ

Kết quả có thể là:
- đọc tiếp bình thường
- pause vì quảng cáo
- revoke vì thuê hết hạn
- resume sau khi hoàn thành quảng cáo

### Bước 5: kết thúc đọc
Backend gửi request POST với:
- `resource:resource_type = reading_session`
- `action:id = read`
- `environment:phase = POST`
- `environment:lifecycle_event = END`

Kết quả:
- rental: ghi usage + close session
- non-rental: close session

---

## 5) Từ điển thuộc tính trong file

### Nhóm `subject`
| Attribute | Ý nghĩa |
|---|---|
| `subject:purchase_active` | Người dùng đang có quyền đọc do đã mua |
| `subject:shared_active` | Người dùng đang có quyền đọc do được share |
| `subject:rental_active` | Người dùng đang còn hiệu lực thuê |

### Nhóm `resource`
| Attribute | Ý nghĩa |
|---|---|
| `resource:resource_type` | Loại tài nguyên: `book_volume` hoặc `reading_session` |
| `resource:preview_chunk_limit` | Giới hạn preview tối đa được đọc |
| `resource:effective_access_type` | Loại truy cập hiệu lực đã được backend tổng hợp: `free`, `rental`, ... |
| `resource:session_active` | Session đang còn hoạt động hay không |
| `resource:session_access_path` | Đường truy cập của session: `PURCHASE`, `SHARED`, `RENTAL`, `FREE` |

### Nhóm `action`
| Attribute | Ý nghĩa |
|---|---|
| `action:id` | Trong file này luôn là `read` |

### Nhóm `environment`
| Attribute | Ý nghĩa |
|---|---|
| `environment:phase` | Pha của vòng đời: `PRE`, `ONGOING`, `POST` |
| `environment:requested_read_scope` | Phạm vi đọc ở PRE: `PREVIEW` hoặc `FULL` |
| `environment:requested_chunk_no` | Chunk người dùng đang xin đọc ở preview |
| `environment:book_entry_ad_required` | Có bắt buộc xem quảng cáo đầu sách không |
| `environment:book_entry_ad_completed` | Quảng cáo đầu sách đã xem xong chưa |
| `environment:periodic_ad_required` | Có bật cơ chế quảng cáo định kỳ không |
| `environment:periodic_ad_due` | Đã tới kỳ phải xem quảng cáo định kỳ chưa |
| `environment:periodic_ad_completed_for_cycle` | Quảng cáo của chu kỳ hiện tại đã hoàn thành chưa |
| `environment:resolved_access_path` | Backend tự tính sẵn nhánh truy cập: `PURCHASE`, `SHARED`, `RENTAL`, `FREE` |
| `environment:usage_tracking_mode` | Backend tự tính sẵn chế độ tracking usage: `REQUIRED`, `OPTIONAL`, `NONE` |
| `environment:lifecycle_event` | Sự kiện vòng đời, ở đây dùng `END` để báo kết thúc đọc |

---

## 6) So sánh nhanh bản compact với bản trước

Bản này thay đổi lớn nhất ở chỗ:
- dùng `VariableDefinition` để gom logic lặp
- phần `FULL/PRE` từ nhiều permit rule rút thành:
  - 1 rule deny
  - 1 rule permit
- đẩy việc tính sẵn `resolved_access_path` và `usage_tracking_mode` sang backend

Điều đó làm file:
- ngắn hơn
- dễ bảo trì hơn
- nhưng cũng phụ thuộc backend nhiều hơn

---

# 7) Giải thích theo từng dòng / cụm dòng

Lưu ý: để đọc đỡ rối, tài liệu này giải thích theo **cụm dòng logic**, nhưng trong từng cụm tôi sẽ đi sát từng dòng quan trọng để bạn đối chiếu với file đánh số dòng.

## Dòng 1
```xml
<?xml version="1.0" encoding="UTF-8"?>
```
Đây là khai báo chuẩn của XML:
- file này là XML
- dùng bảng mã UTF-8

## Dòng 2–40
Đây là comment đầu file.

Máy không dùng comment này để ra quyết định, nhưng con người dùng nó để hiểu thiết kế.
Comment này nói rất rõ:
- bản này là bản gọn hơn
- vẫn giữ logic UCON
- dùng `VariableDefinition`
- FULL/PRE được rút còn 1 deny + 1 permit
- backend phải tính sẵn một số thuộc tính tổng hợp

---

## Dòng 41–47: `PolicySet root`
```xml
<PolicySet ... PolicySetId="root" ... deny-unless-permit">
    <Target/>
```

### Ý nghĩa
- Đây là policyset gốc chứa tất cả policyset con.
- `PolicyCombiningAlgId="deny-unless-permit"` nghĩa là:
  - mặc định coi như deny
  - chỉ khi có nhánh rõ ràng permit thì mới permit
- `Target` rỗng nghĩa là:
  - root không lọc request
  - mọi request đều có thể đi vào root
  - việc lọc sẽ do policyset con làm

---

## Dòng 49–84: PolicySet A = PRE trên `book_volume`

### Dòng 53–56
Mở một `PolicySet` tên:
- `ps-book-volume-read-pre`

Ý nghĩa:
- đây là khối chuyên xử lý request ở pha PRE, áp lên tài nguyên loại `book_volume`

### Dòng 58–84: `Target` của PolicySet A

Trong `Target` này có 3 `Match` chính:

#### Dòng 61–67
So sánh:
- `resource:resource_type`
- với giá trị `"book_volume"`

Nếu bằng nhau thì đúng.

#### Dòng 68–74
So sánh:
- `action:id`
- với `"read"`

Nếu bằng nhau thì đúng.

#### Dòng 75–81
So sánh:
- `environment:phase`
- với `"PRE"`

Nếu bằng nhau thì đúng.

### Kết luận của target này
Chỉ các request thỏa cả 3 điều kiện mới được đi vào nhánh A:
- resource là `book_volume`
- action là `read`
- phase là `PRE`

---

## Dòng 86–126: Policy A1 = `policy-preview-pre`

### Dòng 89–92
Mở policy:
- `PolicyId="policy-preview-pre"`
- dùng `deny-unless-permit`

Nghĩa là:
- nếu không có rule nào permit thì mặc định deny

### Dòng 94–106: Target của policy preview
Chỉ match khi:
- `environment:requested_read_scope = PREVIEW`

Tức là:
- policy này chỉ áp cho yêu cầu đọc preview

### Dòng 108–125: Rule `permit-preview-within-limit`

#### Dòng 108
`Effect="Permit"`

Tức là nếu condition đúng thì cho phép.

#### Dòng 109–124
Condition dùng hàm:
- `integer-less-than-or-equal`

Nó đang so:
- `environment:requested_chunk_no`
- với `resource:preview_chunk_limit`

### Diễn giải
Nếu:
- chunk mà người dùng xin đọc
- nhỏ hơn hoặc bằng giới hạn preview của sách

thì permit.

### Nếu không đúng thì sao?
Vì policy này dùng `deny-unless-permit`, nên:
- không permit được
- thì kết quả cuối cùng là deny

---

## Dòng 128–300: Policy A2 = `policy-read-full-pre`

Đây là policy xử lý đọc full ở pha PRE.

### Dòng 132–135
Mở policy:
- `PolicyId="policy-read-full-pre"`
- `RuleCombiningAlgId="deny-overrides"`

`deny-overrides` nghĩa là:
- nếu có rule deny khớp thì deny thắng
- kể cả có rule permit khác

Điều này rất hợp với logic “cổng quảng cáo”:
- user vốn đủ quyền đọc full
- nhưng nếu còn thiếu quảng cáo đầu vào thì phải chặn

### Dòng 137–149: Target của policy full
Chỉ match khi:
- `environment:requested_read_scope = FULL`

Nghĩa là:
- policy này chỉ áp cho yêu cầu đọc full

---

### Dòng 151–167: `var.purchase_or_shared`
Biến này đúng khi:
- `subject:purchase_active = true`
- hoặc `subject:shared_active = true`

Tức là:
- người dùng có quyền đọc vì đã mua
- hoặc vì được share

---

### Dòng 169–190: `var.effective_access_type_is_free_or_rental`
Biến này đúng khi:
- `resource:effective_access_type = free`
- hoặc `resource:effective_access_type = rental`

Tức là:
- backend đã tổng hợp và nói rằng loại truy cập hiệu lực hiện tại là `free` hoặc `rental`

---

### Dòng 192–202: `var.rental_entitled`
Biến này đúng khi:
- `subject:rental_active = true`
- và `var.effective_access_type_is_free_or_rental = true`

Nghĩa là:
- người dùng còn hạn thuê
- và loại truy cập hiệu lực của tài nguyên phù hợp với đường rental

---

### Dòng 204–214: `var.free_entitled`
Biến này đúng khi:
- `resource:effective_access_type = free`

Nghĩa là:
- sách/tài nguyên hiện tại thuộc loại free, nên có thể đọc full theo nhánh free

---

### Dòng 216–222: `var.full_access_authorized`
Biến này là OR của 3 biến:
- `var.purchase_or_shared`
- `var.rental_entitled`
- `var.free_entitled`

### Ý nghĩa
Chỉ cần **một** trong 3 nhánh đúng thì:
- người dùng được coi là **đủ quyền đọc full**

Đây là biến cốt lõi của toàn bộ phần FULL/PRE.

---

### Dòng 224–231: `var.pre_book_ad_required`
Lấy giá trị boolean:
- `environment:book_entry_ad_required`

Nếu request gửi vào là `true` thì biến này đúng.

---

### Dòng 233–240: `var.pre_book_ad_completed`
Lấy giá trị boolean:
- `environment:book_entry_ad_completed`

Nếu request gửi vào là `true` thì biến này đúng.

---

### Dòng 242–249: `var.pre_book_ad_missing`
Biến này là:
- `var.pre_book_ad_required`
- AND
- NOT `var.pre_book_ad_completed`

### Ý nghĩa
- quảng cáo đầu sách là bắt buộc
- nhưng người dùng chưa hoàn thành quảng cáo đó

Đây là trạng thái “còn thiếu quảng cáo đầu vào”.

---

### Dòng 252–270: Rule `deny-when-book-entry-ad-missing`

#### Condition
Điều kiện là:
- `var.full_access_authorized`
- AND
- `var.pre_book_ad_missing`

### Diễn giải
Nếu:
- người dùng vốn có quyền đọc full
- nhưng quảng cáo đầu sách còn thiếu

thì deny.

#### Obligation
Backend phải:
- `create-book-entry-ad-log`
- đặt `ad-log-status = PENDING`
- đặt `reading-session-id = NULL`

### Ý nghĩa nghiệp vụ
- chưa cho vào đọc
- chưa có session
- tạo log chờ user xem quảng cáo

---

### Dòng 273–299: Rule `permit-when-full-access-authorized`

#### Condition
Điều kiện chỉ là:
- `var.full_access_authorized`

Tức là:
- đủ quyền đọc full thì permit

Vì policy dùng `deny-overrides`, nên:
- nếu trước đó rule deny quảng cáo đầu sách khớp thì deny đã thắng rồi
- rule permit này chỉ có tác dụng khi không bị ad gate chặn

#### Obligation
Backend phải:
- `create-reading-session`

Và có 2 giá trị obligation được lấy từ request:

##### `session-access-path`
Lấy từ:
- `environment:resolved_access_path`

##### `usage-tracking`
Lấy từ:
- `environment:usage_tracking_mode`

### Ý nghĩa thiết kế
Bản compact này đẩy cho backend tính sẵn:
- đây là session theo nhánh nào: `PURCHASE`, `SHARED`, `RENTAL`, `FREE`
- tracking usage ở mức nào

Policy không tự suy chi tiết path trong nhiều permit rule nữa.

---

## Dòng 303–533: PolicySet B = ONGOING trên `reading_session`

### Dòng 308–311
Mở policyset:
- `ps-reading-session-read-ongoing`

### Dòng 313–339: Target của PolicySet B
Chỉ match khi:
- `resource:resource_type = reading_session`
- `action:id = read`
- `environment:phase = ONGOING`

Tức là:
- đây là request điều khiển phiên đọc đang tồn tại

### Dòng 341–346
Mở policy:
- `policy-read-session-ongoing`
- dùng `deny-overrides`

Điều đó hợp lý, vì:
- nếu đang có lý do phải chặn thì deny phải thắng

---

### Dòng 348–355: `var.session_active`
Lấy:
- `resource:session_active`

Biến này nói:
- session hiện tại có đang active không

---

### Dòng 357–367: `var.is_rental_session`
Đúng khi:
- `resource:session_access_path = RENTAL`

Nghĩa là:
- đây là một phiên đọc đi theo nhánh thuê

---

### Dòng 369–382: `var.rental_expired`
Biến này đúng khi:
- session active
- session là rental
- nhưng `subject:rental_active = false`

### Nghĩa là
- đang đọc bằng nhánh thuê
- nhưng giờ gói thuê đã hết hạn

---

### Dòng 384–391: `var.periodic_ad_required`
Lấy:
- `environment:periodic_ad_required`

---

### Dòng 393–400: `var.periodic_ad_due`
Lấy:
- `environment:periodic_ad_due`

---

### Dòng 402–409: `var.periodic_ad_completed_for_cycle`
Lấy:
- `environment:periodic_ad_completed_for_cycle`

---

### Dòng 411–420: `var.periodic_ad_blocking`
Đúng khi:
- session active
- periodic ad required
- periodic ad due
- NOT `periodic_ad_completed_for_cycle`

### Diễn giải
- đã tới hạn quảng cáo định kỳ
- nhưng quảng cáo của chu kỳ hiện tại chưa được hoàn thành
- nên đang ở trạng thái phải chặn

---

### Dòng 422–429: `var.periodic_ad_just_completed`
Đúng khi:
- session active
- periodic ad required
- periodic ad due
- periodic ad completed for cycle

### Diễn giải
- vẫn là chu kỳ đến hạn đó
- nhưng giờ quảng cáo đã được hoàn tất
- nên có thể cho resume

---

### Dòng 431–470: `var.session_access_path_allowed`
Biến này đúng nếu `session_access_path` là một trong các nhánh hợp lệ:

- `PURCHASE`
- `SHARED`
- `FREE`
- hoặc `RENTAL` và `subject:rental_active = true`

### Ý nghĩa
Đây là tập các session được coi là hợp lệ để tiếp tục tồn tại trong pha ONGOING.

Nếu là rental nhưng thuê đã hết hạn thì không còn hợp lệ.

---

### Dòng 472–483: `var.ongoing_read_permitted_normally`
Biến này đúng khi:
- session active
- session_access_path_allowed
- không bị `periodic_ad_blocking`
- và cũng không phải `periodic_ad_just_completed`

### Ý nghĩa
Đây là trạng thái “đọc tiếp bình thường”:
- không bị ad chặn
- cũng không phải trường hợp đặc biệt vừa hoàn tất ad để resume

Tác giả tách riêng trường hợp resume để backend có thể chạy obligation resume khác với permit thường.

---

### Dòng 485–496: Rule `deny-rental-expired`
Nếu:
- `var.rental_expired = true`

thì:
- `Deny`

Obligation:
- `revoke-reading-session`
- `revoked-reason = rental_expired`

### Ý nghĩa nghiệp vụ
- session rental phải bị ngắt ngay nếu thuê hết hạn

---

### Dòng 498–512: Rule `deny-when-periodic-ad-blocking`
Nếu:
- `var.periodic_ad_blocking = true`

thì:
- `Deny`

Obligation:
- `create-periodic-ad-log-and-pause-session`
- `session-status = PAUSED_FOR_AD`
- `ad-log-status = PENDING`

### Ý nghĩa nghiệp vụ
- tới hạn quảng cáo giữa phiên
- user chưa hoàn tất ad
- backend phải pause session và tạo ad log đang chờ

---

### Dòng 514–525: Rule `permit-after-periodic-ad-completed`
Nếu:
- `var.periodic_ad_just_completed = true`

thì:
- `Permit`

Obligation:
- `resume-session-and-advance-next-ad-due`
- `session-status = ACTIVE`

### Ý nghĩa nghiệp vụ
- user vừa hoàn tất quảng cáo của chu kỳ hiện tại
- backend phải resume session
- đồng thời dời mốc quảng cáo lần sau

---

### Dòng 527–531: Rule `permit-continue-read-normal`
Nếu:
- `var.ongoing_read_permitted_normally = true`

thì:
- `Permit`

Không có obligation đặc biệt ở đây.

### Ý nghĩa
- session vẫn hợp lệ
- không bị ad chặn
- không ở trạng thái “vừa hoàn thành ad”
- nên chỉ cho tiếp tục đọc như bình thường

---

## Dòng 535–633: PolicySet C = POST trên `reading_session`

### Dòng 540–543
Mở policyset:
- `ps-reading-session-read-post`

### Dòng 545–578: Target của PolicySet C
Chỉ match khi:
- `resource:resource_type = reading_session`
- `action:id = read`
- `environment:phase = POST`
- `environment:lifecycle_event = END`

Tức là:
- request này là request kết thúc vòng đời đọc

### Dòng 580–585
Mở policy:
- `policy-read-session-post`
- dùng `deny-unless-permit`

---

### Dòng 587–597: `var.is_rental_session`
Đúng khi:
- `resource:session_access_path = RENTAL`

Biến này sẽ quyết định:
- có cần ghi usage record hay không

---

### Dòng 599–615: Rule `permit-rental-end-read-post-update`
Nếu:
- `var.is_rental_session = true`

thì:
- `Permit`

Obligations:
- `write-usage-record`
- `usage-source = RENTAL`
- `close-session`
- `final-status = ENDED`

### Ý nghĩa
- nếu session là rental thì khi kết thúc phải:
  1. ghi usage record
  2. rồi đóng session

---

### Dòng 617–630: Rule `permit-non-rental-end-read-cleanup`
Nếu:
- NOT `var.is_rental_session`

thì:
- `Permit`

Obligation:
- `close-session`
- `final-status = ENDED`

### Ý nghĩa
- nếu session là PURCHASE / SHARED / FREE
- thì không cần ghi usage rental
- chỉ cần cleanup và đóng session

---

# 8) Pseudocode toàn file

## PREVIEW
```text
if resource_type == book_volume and action == read and phase == PRE and requested_read_scope == PREVIEW:
    permit when requested_chunk_no <= preview_chunk_limit
    otherwise deny
```

## FULL/PRE
```text
full_access_authorized =
    purchase_active
    OR shared_active
    OR (rental_active AND effective_access_type in {free, rental})
    OR (effective_access_type == free)

pre_book_ad_missing =
    book_entry_ad_required AND NOT book_entry_ad_completed

if full_access_authorized AND pre_book_ad_missing:
    deny + create ad log pending
else if full_access_authorized:
    permit + create reading session using resolved_access_path and usage_tracking_mode
else:
    deny
```

## ONGOING
```text
rental_expired =
    session_active AND is_rental_session AND NOT rental_active

periodic_ad_blocking =
    session_active AND periodic_ad_required AND periodic_ad_due AND NOT periodic_ad_completed_for_cycle

periodic_ad_just_completed =
    session_active AND periodic_ad_required AND periodic_ad_due AND periodic_ad_completed_for_cycle

session_access_path_allowed =
    PURCHASE OR SHARED OR FREE OR (RENTAL AND rental_active)

ongoing_read_permitted_normally =
    session_active
    AND session_access_path_allowed
    AND NOT periodic_ad_blocking
    AND NOT periodic_ad_just_completed

if rental_expired:
    deny + revoke session
else if periodic_ad_blocking:
    deny + pause session + create periodic ad log
else if periodic_ad_just_completed:
    permit + resume session + move next ad due
else if ongoing_read_permitted_normally:
    permit
else:
    deny
```

## POST
```text
if reading_session and phase == POST and lifecycle_event == END:
    if session_access_path == RENTAL:
        permit + write usage record + close session
    else:
        permit + close session
```

---

# 9) Mối liên kết giữa 3 pha

## PRE tạo nền cho ONGOING
Ở PRE, khi permit full:
- backend tạo `reading_session`
- session này có `session_access_path`
- session này có thể có `usage_tracking`

## ONGOING dùng `reading_session` đó
Trong ONGOING, policy đọc:
- `resource:session_active`
- `resource:session_access_path`

để biết:
- session có còn hoạt động không
- session là PURCHASE/SHARED/FREE/RENTAL
- nếu rental thì thuê còn hạn không

## POST dùng chính `reading_session` đó để cleanup
Khi kết thúc:
- nếu path là RENTAL thì ghi usage
- nếu không phải RENTAL thì chỉ close

---

# 10) Điều cần nhớ nhất

1. File này **không tự cập nhật DB**
2. Nó chỉ:
   - đọc request
   - quyết định permit/deny
   - gửi obligation cho backend
3. Backend mới là nơi phải làm thật:
   - tạo session
   - tạo ad log
   - pause/revoke/resume session
   - ghi usage
   - close session

---

# 11) Kết luận

Bản `compact` này:
- giữ nguyên logic nghiệp vụ chính
- gọn hơn nhờ `VariableDefinition`
- dễ đọc hơn vì các biểu thức được đặt tên
- đẩy cho backend tính sẵn:
  - `resolved_access_path`
  - `usage_tracking_mode`

Điểm mạnh:
- code ngắn hơn
- logic rõ hơn
- ONGOING rất sạch

Điểm cần nhớ:
- backend phải gửi đúng các thuộc tính tổng hợp
- nếu backend gửi sai `resolved_access_path` hoặc `usage_tracking_mode`, policy hiện tại sẽ tin vào giá trị đó

---

## 12) Tài liệu đi kèm

- File nguồn có đánh số dòng: `ebook-ucon-way1-compact.numbered.txt`
- Tài liệu này: `ebook_ucon_way1_compact_full_explanation.md`

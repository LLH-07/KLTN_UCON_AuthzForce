# Giải thích rất chi tiết 2 file XACML trong thư mục `policies`

Tài liệu này giải thích theo **line number thật** của 2 file:

- `ebook-ucon-way1.xml`
- `ebook-ucon-way2-student-style.xml`

Mục tiêu là giúp đọc XACML từ con số 0.

---

## 0) Từ điển XACML cực ngắn

- `PolicySet`: cái hộp lớn chứa nhiều policy/policyset con.
- `Policy`: một nhóm rule liên quan cùng một mục tiêu.
- `Rule`: luật nhỏ nhất, có `Effect="Permit"` hoặc `Effect="Deny"`.
- `Target`: bộ lọc sơ bộ. Request không khớp target thì policy/rule đó không được xét tiếp.
- `Condition`: điều kiện logic chi tiết. Nếu condition đúng thì rule mới ra effect.
- `AttributeDesignator`: “lấy giá trị thuộc tính từ Request”.
- `AttributeValue`: giá trị cố định viết cứng trong policy.
- `Apply`: gọi hàm XACML như `and`, `or`, `not`, `string-equal`, `boolean-one-and-only`.
- `ObligationExpression`: việc app/backend phải làm tiếp khi Permit/Deny xảy ra.
- `FulfillOn="Permit"`: obligation chỉ đi kèm khi kết quả là Permit.
- `FulfillOn="Deny"`: obligation chỉ đi kèm khi kết quả là Deny.

### Những hàm XACML bạn gặp lặp đi lặp lại

- `string-equal`: so sánh 2 chuỗi có bằng nhau không.
- `integer-less-than-or-equal`: số bên trái <= số bên phải.
- `and`: tất cả điều kiện con đều đúng.
- `or`: chỉ cần một điều kiện con đúng.
- `not`: phủ định.
- `string-one-and-only`, `boolean-one-and-only`, `integer-one-and-only`:
  “hãy lấy đúng **một** giá trị kiểu này từ Request”.

### Những category trong request

- `action`: hành động đang xin làm, ví dụ `read_full`, `continue_read`.
- `resource`: thuộc tính của tài nguyên / session / sách.
- `subject`: thuộc tính của người dùng.
- `environment`: ngữ cảnh hệ thống, phase, ad flags, v.v.

---

# 1) File `ebook-ucon-way1.xml`

## 1.1 Phần đầu file

### Dòng 1
`<?xml version="1.0" encoding="UTF-8"?>`

Khai báo đây là file XML, dùng UTF-8.

### Dòng 2–14
Đây là comment của tác giả, không tham gia vào việc evaluate.
Nó nói rõ triết lý của file:

- bố cục theo **luồng nghiệp vụ**
- dùng `environment:phase` để biểu diễn `PRE / ONGOING / POST`
- dùng obligation để bảo app/backend làm việc tiếp theo
- một số thuộc tính phải được backend tính sẵn rồi nhét vào request

### Dòng 15–19
Mở `PolicySet` gốc.

- dòng 16: namespace XACML 3.0
- dòng 17: `PolicySetId="root"`
- dòng 18: version
- dòng 19: `deny-unless-permit`

Ý nghĩa của `deny-unless-permit`:

- mặc định là **Deny**
- chỉ khi có permit hợp lệ thì mới Permit

### Dòng 21–22
`<Target/>`

Target rỗng nghĩa là policyset gốc áp dụng cho **mọi request**.

---

## 1.2 Policy 1: đọc preview miễn phí có giới hạn chunk

### Dòng 24–27
Comment mô tả policy 1.

### Dòng 28–31
Mở một `Policy` mới:

- `PolicyId="policy-1-read-preview-pre"`
- rule combining là `deny-unless-permit`

Nghĩa là bên trong policy này, nếu không có rule permit nào khớp thì kết quả sẽ là deny.

### Dòng 33–55: Target của policy 1
Target này lọc request theo 2 điều kiện:

- action phải là `read_preview`
- phase phải là `PRE`

#### Dòng 34–35
`AnyOf -> AllOf`

Trong target XACML:

- `AnyOf` giống OR ở mức cụm
- `AllOf` giống AND ở mức cụm match

Ở đây chỉ có 1 cụm `AllOf`, nên hiểu đơn giản là “cả hai match bên dưới đều phải đúng”.

#### Dòng 36–43
Match thứ nhất.

- dòng 37: dùng hàm `string-equal`
- dòng 38: giá trị cố định là `read_preview`
- dòng 39–42: lấy `action:id` từ request

=> đang hỏi: `action:id == "read_preview"` đúng không?

#### Dòng 45–52
Match thứ hai.

- dòng 47: giá trị cố định `PRE`
- dòng 48–51: lấy `environment:phase` từ request

=> đang hỏi: `environment:phase == "PRE"` đúng không?

#### Dòng 53–55
Đóng `AllOf`, `AnyOf`, `Target`.

### Dòng 57–77: Rule permit

#### Dòng 57
Tạo rule tên `p1-permit-preview-within-limit`, effect là `Permit`.

#### Dòng 58
Bắt đầu `Condition`.

#### Dòng 59
Dùng hàm `integer-less-than-or-equal`.

Nghĩa là condition này kiểm tra:

`vế trái <= vế phải`

#### Dòng 60–66
Vế trái là `environment:requested_chunk_no`.

- dòng 61: lấy đúng 1 số nguyên
- dòng 62–65: thuộc tính lấy từ request

=> lấy số chunk mà người dùng đang xin đọc.

#### Dòng 68–74
Vế phải là `resource:preview_chunk_limit`.

=> lấy giới hạn preview của sách.

#### Dòng 75
So sánh xong: `requested_chunk_no <= preview_chunk_limit`

#### Dòng 76–77
Đóng condition và rule.

#### Dòng 78
Đóng policy 1.

**Dịch cả policy 1 sang tiếng Việt thường:**

> Nếu request là đọc preview ở pha PRE, và chunk yêu cầu không vượt quá giới hạn preview của sách, thì Permit.

---

## 1.3 Policy 2 + 3 + 5: đọc full ở pha PRE

### Dòng 80–85
Comment nói policy này gộp 3 ý:

- Policy 2: mua / share thì đọc full
- Policy 3: thuê còn hạn thì đọc full
- Policy 5: nếu cần quảng cáo đầu sách mà chưa xem thì chặn

### Dòng 86–89
Mở policy `policy-2-3-5-read-full-pre`.

Khác policy 1 ở chỗ rule combining là `deny-overrides`.

`deny-overrides` nghĩa là:

- chỉ cần có một rule deny khớp là deny thắng
- rất hợp với logic “chưa xem quảng cáo bắt buộc thì chặn luôn”

### Dòng 91–113: Target

Target lọc request có:

- `action:id == read_full`
- `environment:phase == PRE`

Cấu trúc hoàn toàn giống policy 1, chỉ đổi giá trị action từ `read_preview` sang `read_full`.

---

## 1.4 Rule deny vì thiếu quảng cáo đầu vào

### Dòng 115–116
Comment nói ý nghĩa nghiệp vụ:

- user vốn đủ điều kiện đọc full
- nhưng còn thiếu quảng cáo bắt buộc trước khi vào sách
- thì Deny
- app/backend phải tạo ad log ở trạng thái pending
- không tạo reading session

### Dòng 117
Tạo rule deny `p5-deny-when-book-entry-ad-missing`.

### Dòng 118–193: Condition của rule deny

Condition này là một biểu thức `and` lớn.
Muốn deny thì **tất cả** phần con đều phải đúng.

#### Dòng 119
Bắt đầu `and` lớn.

### Nhóm A: người dùng vốn đủ điều kiện đọc full

#### Dòng 121–123
Comment mô tả công thức:

`(purchase_active OR shared_active) OR (rental_active AND effective_access_type in {free, rental})`

#### Dòng 124
Bắt đầu `or` lớn cho nhóm A.

### Nhánh A1: đã mua hoặc được share

#### Dòng 126–140
Một `or` nhỏ:

- `subject:purchase_active`
- hoặc `subject:shared_active`

Các dòng 128–139 chỉ làm việc “lấy 1 giá trị boolean” từ request.

=> nhánh này đúng khi user đã mua sách hoặc đang có quyền share.

### Nhánh A2: đang thuê còn hạn và đường truy cập hợp lệ cho gói thuê

#### Dòng 142–172
Một `and` nhỏ:

- dòng 144–149: `subject:rental_active == true`
- dòng 151–171: `resource:effective_access_type == "free" OR "rental"`

Phần `effective_access_type` được backend tính sẵn rồi gửi vào request.
Nó nói volume/chương này được truy cập theo đường nào.

=> nhánh này đúng khi user đang thuê còn hạn và tài nguyên này thuộc nhóm cho phép đọc qua thuê/free.

### Nhóm B: quảng cáo đầu vào là bắt buộc

#### Dòng 175–181
Lấy `environment:book_entry_ad_required`.

Vì nó nằm trực tiếp trong `and`, nên giá trị phải là `true` thì cả condition mới đi tiếp.

### Nhóm C: quảng cáo đầu vào chưa hoàn thành

#### Dòng 183–191
`not(book_entry_ad_completed)`

Tức là `environment:book_entry_ad_completed == false`.

### Dòng 192–193
Đóng `and` lớn và kết thúc condition.

**Dịch trọn condition:**

> Nếu user có quyền đọc full, và quảng cáo đầu vào là bắt buộc, và quảng cáo đó chưa được hoàn thành, thì rule này khớp.

### Dòng 195–204: Obligation khi deny

#### Dòng 195
Bắt đầu cụm obligations.

#### Dòng 196
Tạo obligation `create-book-entry-ad-log`, chỉ gửi ra khi kết quả của rule là Deny.

#### Dòng 197–199
Gán thuộc tính phụ cho obligation:

- `ad-log-status = PENDING`

#### Dòng 200–202
Gán thêm:

- `reading-session-id = NULL`

Ý nghĩa: app/backend phải hiểu là **chưa được mở phiên đọc**.

#### Dòng 203–204
Đóng obligation và khối obligations.

#### Dòng 205
Đóng rule deny.

---

## 1.5 Rule permit cho PURCHASE / SHARED

### Dòng 207
Comment: policy 2 là đường mua hoặc share.

### Dòng 208
Tạo rule permit `p2-permit-purchase-or-share-read-full`.

### Dòng 209–245: Condition

Là một `and` gồm 2 nhóm.

### Nhóm 1: đã mua hoặc được share

#### Dòng 211–225
`purchase_active OR shared_active`

### Nhóm 2: quảng cáo đầu vào không cần, hoặc đã xong

#### Dòng 227–243
`not(book_entry_ad_required) OR book_entry_ad_completed`

Nghĩa là:

- hoặc sách này không bắt buộc xem ad đầu vào
- hoặc có bắt buộc nhưng user đã xem xong rồi

### Dòng 244–245
Đóng condition.

### Dòng 247–253: obligation khi permit

#### Dòng 248
Obligation `create-reading-session`, chỉ đi kèm khi Permit.

#### Dòng 249–251
Gán `usage-tracking = OPTIONAL`

Ý nghĩa:

- app/backend phải mở phiên đọc
- đường PURCHASE/SHARED không bắt buộc tracking usage chặt như RENTAL

#### Dòng 254
Đóng rule.

---

## 1.6 Rule permit cho RENTAL

### Dòng 256
Comment: policy 3 là đường thuê.

### Dòng 257
Tạo rule permit `p3-permit-rental-read-full`.

### Dòng 258–308: Condition

Là `and` gồm 3 nhóm.

### Nhóm 1
#### Dòng 260–266
`subject:rental_active == true`

### Nhóm 2
#### Dòng 268–288
`resource:effective_access_type == "free" OR "rental"`

### Nhóm 3
#### Dòng 290–306
`not(book_entry_ad_required) OR book_entry_ad_completed`

### Dòng 307–308
Đóng condition.

### Dòng 310–316: obligation

#### Dòng 311
Tạo obligation `create-reading-session` khi Permit.

#### Dòng 312–314
Gán `usage-tracking = REQUIRED`

Ý nghĩa:

- phiên RENTAL cần backend theo dõi usage để làm post-update / thống kê / billing / kiểm tra sau dùng

#### Dòng 317–318
Đóng rule và đóng policy PRE đọc full.

---

## 1.7 Policy ongoing cho continue_read

### Dòng 320–324
Comment: policy này xử lý đang đọc.

- hết hạn thuê thì cắt
- tới kỳ quảng cáo định kỳ mà chưa xem thì pause

### Dòng 325–328
Mở policy `policy-4-6-continue-read-ongoing`.

Rule combining tiếp tục là `deny-overrides`.

### Dòng 330–352: Target

Chỉ áp dụng cho request có:

- `action:id == continue_read`
- `environment:phase == ONGOING`

---

## 1.8 Rule deny khi rental đã hết hạn

### Dòng 354
Comment mô tả đúng ý nghiệp vụ.

### Dòng 355
Tạo rule deny `p4-deny-rental-expired`.

### Dòng 356–384: Condition

Là `and` gồm 3 phần.

#### Dòng 358–363
`resource:session_active == true`

=> chỉ xét nếu đang có session thật sự.

#### Dòng 365–373
`resource:session_access_path == "RENTAL"`

=> chỉ xét phiên được mở theo đường thuê.

#### Dòng 375–382
`not(subject:rental_active)`

=> gói thuê đã hết hạn hoặc không còn hiệu lực.

#### Dòng 383–384
Đóng condition.

### Dòng 386–392: obligation

- obligation: `revoke-reading-session`
- assignment: `revoked-reason = rental_expired`

Ý nghĩa: app/backend phải ngắt phiên đọc ngay.

#### Dòng 393
Đóng rule.

---

## 1.9 Rule deny khi quảng cáo định kỳ đến hạn mà chưa hoàn thành

### Dòng 395
Comment mô tả logic pause.

### Dòng 396
Tạo rule deny `p6-deny-when-periodic-ad-is-due-and-incomplete`.

### Dòng 397–429: Condition

Là `and` gồm 4 phần.

#### Dòng 399–404
`resource:session_active == true`

#### Dòng 406–411
`environment:periodic_ad_required == true`

#### Dòng 413–418
`environment:periodic_ad_due == true`

#### Dòng 420–427
`not(environment:periodic_ad_completed_for_cycle)`

=> đến hạn quảng cáo chu kỳ hiện tại nhưng user chưa xem xong.

#### Dòng 428–429
Đóng condition.

### Dòng 431–440: obligation

Obligation là `create-periodic-ad-log-and-pause-session`.

Assignments:

- `session-status = PAUSED_FOR_AD`
- `ad-log-status = PENDING`

Nghĩa là app/backend phải:

- tạo ad log mới cho đợt này
- chuyển session sang trạng thái tạm dừng chờ xem quảng cáo

#### Dòng 441
Đóng rule.

---

## 1.10 Rule permit cho đọc tiếp bình thường

### Dòng 443–446
Comment mô tả logic cho phép.

### Dòng 447
Tạo rule permit `p4-p6-permit-continue-read-normal`.

### Dòng 448–521: Condition

Là `and` gồm 3 nhóm lớn.

### Nhóm 1: session đang active

#### Dòng 451–456
`resource:session_active == true`

### Nhóm 2: đường truy cập hợp lệ

#### Dòng 458–498
Một `or` gồm 3 nhánh:

- dòng 461–469: `session_access_path == PURCHASE`
- dòng 471–479: `session_access_path == SHARED`
- dòng 481–497: `session_access_path == RENTAL AND subject:rental_active == true`

Ý nghĩa:

- phiên mua đứt thì đi tiếp
- phiên share thì đi tiếp
- phiên thuê chỉ đi tiếp nếu gói thuê vẫn còn hiệu lực

### Nhóm 3: không bị chặn bởi ad định kỳ

#### Dòng 500–519
`not(periodic_ad_required) OR not(periodic_ad_due)`

Nghĩa là:

- hoặc không cần ad định kỳ
- hoặc có cần nhưng chưa tới hạn

#### Dòng 520–521
Đóng condition.

### Dòng 522
Đóng rule permit.

Rule này không có obligation vì “đọc tiếp bình thường” thì không cần app làm thao tác bổ sung.

---

## 1.11 Rule permit sau khi đã hoàn thành quảng cáo định kỳ

### Dòng 524–525
Comment nói rõ đây là nhánh resume sau ad.

### Dòng 526
Tạo rule permit `p6-permit-after-periodic-ad-completed`.

### Dòng 527–557: Condition

Là `and` gồm 4 phần:

- dòng 529–534: `session_active == true`
- dòng 536–541: `periodic_ad_required == true`
- dòng 543–548: `periodic_ad_due == true`
- dòng 550–555: `periodic_ad_completed_for_cycle == true`

=> cùng đúng thì nghĩa là ad của chu kỳ hiện tại đã được hoàn tất.

### Dòng 559–565: obligation

Obligation là `resume-session-and-advance-next-ad-due`.

Assignment:

- `session-status = ACTIVE`

Ý nghĩa:

- app/backend phải mở lại session
- đồng thời tính mốc `next_ad_due_at` tiếp theo

### Dòng 566–567
Đóng rule và đóng policy ongoing.

---

## 1.12 Policy POST cho end_read

### Dòng 569–572
Comment: phần post-update của policy thuê.

### Dòng 573–576
Mở policy `policy-3-end-read-post`.

Rule combining quay lại `deny-unless-permit`.

### Dòng 578–600: Target

Target lọc request có:

- `action:id == end_read`
- `environment:phase == POST`

---

## 1.13 Rule permit khi kết thúc phiên RENTAL

### Dòng 602
Comment mô tả: ghi usage + cleanup.

### Dòng 603
Tạo rule permit `p3-permit-rental-end-read-post-update`.

### Dòng 604–614: Condition

- dòng 605–613: `resource:session_access_path == RENTAL`

=> chỉ khi phiên vừa kết thúc là phiên thuê.

### Dòng 616–627: obligations

Obligation 1:

- dòng 617: `write-usage-record`
- dòng 618–620: `usage-source = RENTAL`

Obligation 2:

- dòng 622: `close-session`
- dòng 623–625: `final-status = ENDED`

Ý nghĩa:

- backend phải ghi usage record cho phiên thuê
- sau đó đóng session với trạng thái ended

### Dòng 628
Đóng rule.

---

## 1.14 Rule permit khi kết thúc phiên không phải RENTAL

### Dòng 630
Comment: non-rental chỉ cleanup.

### Dòng 631
Tạo rule permit `permit-non-rental-end-read-cleanup`.

### Dòng 632–644: Condition

`not(session_access_path == RENTAL)`

=> PURCHASE hoặc SHARED thì khớp.

### Dòng 646–652: obligation

- `close-session`
- `final-status = ENDED`

Vì không phải rental nên không cần `write-usage-record` kiểu rental.

### Dòng 653–655
Đóng rule, đóng policy, đóng policyset gốc.

---


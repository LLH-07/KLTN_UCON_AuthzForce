# Giải thích file `ebook-ucon-way1-revised.xml`

Tài liệu này đi kèm file đánh số dòng:

- `ebook-ucon-way1-revised.numbered.txt`

## Cách đọc XACML rất ngắn

- `PolicySet`: hộp lớn chứa các policy/policyset con
- `Policy`: nhóm rule cùng mục đích
- `Target`: bộ lọc sơ bộ
- `Rule`: luật Permit/Deny
- `Condition`: biểu thức logic của rule
- `Apply`: gọi hàm như and/or/not/string-equal
- `AttributeDesignator`: lấy thuộc tính từ Request
- `AttributeValue`: giá trị cố định trong policy
- `ObligationExpression`: việc backend/PEP phải làm tiếp

## Cấu trúc file

- Dòng 1–43: XML declaration + comment mô tả ý tưởng thiết kế
- Dòng 44–50: PolicySet gốc
- Dòng 56–427: PolicySet A, xử lý `book_volume` ở pha `PRE`
- Dòng 434–698: PolicySet B, xử lý `reading_session` ở pha `ONGOING`
- Dòng 705–813: PolicySet C, xử lý `reading_session` ở pha `POST`

## Các cụm dòng quan trọng

- 61–92: Target của PolicySet A (`book_volume`, `read`, `PRE`)
- 103–115: Target của Policy 1 (`requested_read_scope = PREVIEW`)
- 117–137: Rule preview trong giới hạn chunk
- 153–165: Target của Policy full (`requested_read_scope = FULL`)
- 172–267: Rule Deny khi thiếu quảng cáo đầu sách
- 270–316: Rule Permit nhánh purchase/share
- 319–381: Rule Permit nhánh rental
- 384–425: Rule Permit nhánh free full
- 439–470: Target của PolicySet B (`reading_session`, `read`, `ONGOING`)
- 480–518: Rule Deny khi rental hết hạn trong lúc đọc
- 521–566: Rule Deny khi periodic ad đến hạn nhưng chưa xong
- 569–653: Rule Permit đọc tiếp bình thường
- 656–696: Rule Permit sau khi hoàn thành periodic ad
- 710–750: Target của PolicySet C (`reading_session`, `read`, `POST`, `END`)
- 760–785: Rule Permit POST cho session rental
- 788–810: Rule Permit POST cho session không phải rental

## Luồng tổng thể

1. PRE trên `book_volume`:
   - quyết định preview hay full
   - nếu full thì xét purchase/share, rental, free
   - nếu thiếu book-entry ad thì Deny và yêu cầu tạo ad log
   - nếu được phép thì yêu cầu tạo reading session

2. ONGOING trên `reading_session`:
   - nếu rental hết hạn thì revoke
   - nếu periodic ad đến hạn mà chưa xem thì pause
   - nếu không vướng gì thì cho đọc tiếp
   - nếu vừa hoàn thành periodic ad thì resume

3. POST trên `reading_session`:
   - session rental thì ghi usage record rồi close
   - session khác thì chỉ close

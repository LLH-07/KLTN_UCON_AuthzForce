# AuthzForce Server – Hướng dẫn thao tác cơ bản

Tài liệu này hướng dẫn cách:

- chạy **AuthzForce Server** bằng Docker
- tạo **domain**
- tạo và nạp **PolicySet**
- tạo **root policy version mới** để trỏ tới policy set cần dùng
- quản lý **version policy**
- xóa policy / xóa một version policy
- gửi **XACML Request** tới PDP để lấy quyết định

> Ghi chú:
> - Endpoint nạp policy của AuthzForce PAP nhận **XACML `PolicySet`**, không phải `Policy` đơn lẻ.
> - Trong cách làm ở tài liệu này, PDP được dùng thông qua một **root policy** có `PolicySetId="root"`.
> - Mỗi lần muốn đổi policy mà PDP sẽ dùng, ta tạo một **version mới** của `root`, rồi nạp version đó lên PAP.
> - Có thể xem file student-read-policy.xml làm ví dụ về chính sách, các version của root policy được lưu trong root-policy-versions

---

## 1. Yêu cầu

Cần có:

- Docker Desktop hoặc Docker Engine
- `curl`
- file policy XACML 3.0 ở dạng **`PolicySet`**

---

## 2. Chạy AuthzForce Server bằng Docker

### Cú pháp tổng quát

```bash
docker run -d -p <HOST_PORT>:8080 --name <CONTAINER_NAME> authzforce/server:<TAG>
```

### Ví dụ

```bash
docker run -d -p 8080:8080 --name authzforce authzforce/server:12.0.1
```

### Kiểm tra server đã lên chưa

```bash
curl -s "http://localhost:<HOST_PORT>/authzforce-ce/domains"
```

### Ví dụ

```bash
curl -s "http://localhost:8080/authzforce-ce/domains"
```

Nếu thành công, server sẽ trả về XML danh sách domain.

---

## 3. Tạo domain

Mỗi policy và mỗi lần PDP đánh giá request đều diễn ra trong một **domain**.

### Cú pháp tổng quát

```bash
curl --request POST \
  --header "Accept: application/xml" \
  --header "Content-Type: application/xml;charset=UTF-8" \
  --data '<?xml version="1.0" encoding="UTF-8"?><taz:domainProperties xmlns:taz="http://authzforce.github.io/rest-api-model/xmlns/authz/5"/>' \
  "http://localhost:<HOST_PORT>/authzforce-ce/domains"
```

### Ví dụ

```bash
curl --request POST \
  --header "Accept: application/xml" \
  --header "Content-Type: application/xml;charset=UTF-8" \
  --data '<?xml version="1.0" encoding="UTF-8"?><taz:domainProperties xmlns:taz="http://authzforce.github.io/rest-api-model/xmlns/authz/5"/>' \
  "http://localhost:8080/authzforce-ce/domains"
```

Kết quả trả về sẽ có dạng:

```xml
<link rel="item" href="<DOMAIN_ID>" title="<DOMAIN_ID>"/>
```

Giá trị `href` chính là **domainId** cần dùng ở các bước sau.

### Liệt kê domain hiện có

```bash
curl -s "http://localhost:<HOST_PORT>/authzforce-ce/domains"
```

Ví dụ:

```bash
curl -s "http://localhost:8080/authzforce-ce/domains"
```

---

## 4. Tạo PolicySet và nạp lên PAP

### Lưu ý quan trọng

Endpoint:

```text
/domains/<DOMAIN_ID>/pap/policies
```

dùng để nạp một **XACML `PolicySet`** vào kho policy của domain. Nếu gửi `Policy` đơn lẻ vào endpoint này, AuthzForce sẽ báo lỗi.

### Mẫu PolicySet tối giản

Ví dụ file `trivial-policyset.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<PolicySet
    xmlns="urn:oasis:names:tc:xacml:3.0:core:schema:wd-17"
    PolicySetId="trivial-policyset"
    Version="1.0"
    PolicyCombiningAlgId="urn:oasis:names:tc:xacml:3.0:policy-combining-algorithm:deny-unless-permit">

    <Target/>

    <Policy
        PolicyId="trivial-policy"
        Version="1.0"
        RuleCombiningAlgId="urn:oasis:names:tc:xacml:3.0:rule-combining-algorithm:deny-unless-permit">

        <Target/>

        <Rule RuleId="always-deny" Effect="Deny"/>
    </Policy>
</PolicySet>
```

### Cú pháp tổng quát để nạp policy

```bash
curl -i --request POST \
  --header "Content-Type: application/xml; charset=UTF-8" \
  --data-binary @<POLICYSET_FILE>.xml \
  "http://localhost:<HOST_PORT>/authzforce-ce/domains/<DOMAIN_ID>/pap/policies"
```

### Ví dụ

```bash
curl -i --request POST \
  --header "Content-Type: application/xml; charset=UTF-8" \
  --data-binary @trivial-policyset.xml \
  "http://localhost:8080/authzforce-ce/domains/HxXn0yzoEfGh4QJCrBEAAg/pap/policies"
```

Nếu thành công, AuthzForce sẽ trả về XML dạng:

```xml
<link ... href="trivial-policyset/1.0" title="Policy 'trivial-policyset' v1.0"/>
```

Điều đó có nghĩa là `PolicySetId="trivial-policyset"` với `Version="1.0"` đã được nạp vào domain thành công.

---

## 5. Cách dùng root policy để PDP đánh giá

Trong cách làm này, PDP luôn bắt đầu đánh giá từ một `PolicySetId="root"`.

Thay vì sửa trực tiếp policy cũ, mỗi lần cần thay đổi policy mà PDP sẽ dùng, hãy:

1. tạo một file `root` mới với `Version` mới
2. trong file đó, dùng `PolicySetIdReference` để trỏ tới policy set nghiệp vụ cần dùng
3. nạp file `root` mới lên PAP bằng `POST /pap/policies`

Cách này giúp:

- dễ theo dõi lịch sử thay đổi
- dễ quay về version cũ
- tách riêng **policy nghiệp vụ** và **policy gốc dùng để trỏ**

---

## 6. Tạo root policy version mới

### Mẫu tổng quát

```xml
<?xml version="1.0" encoding="UTF-8"?>
<PolicySet
    xmlns="urn:oasis:names:tc:xacml:3.0:core:schema:wd-17"
    PolicySetId="root"
    Version="<ROOT_VERSION>"
    PolicyCombiningAlgId="urn:oasis:names:tc:xacml:3.0:policy-combining-algorithm:deny-unless-permit">

    <Target/>

    <PolicySetIdReference><TARGET_POLICYSET_ID></PolicySetIdReference>
</PolicySet>
```

Trong đó:

- `<ROOT_VERSION>`: version mới của root, ví dụ `0.1.5`
- `<TARGET_POLICYSET_ID>`: `PolicySetId` của policy set nghiệp vụ mà bạn muốn PDP dùng

### Ví dụ cụ thể

File `root-0.1.5.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<PolicySet
    xmlns="urn:oasis:names:tc:xacml:3.0:core:schema:wd-17"
    PolicySetId="root"
    Version="0.1.5"
    PolicyCombiningAlgId="urn:oasis:names:tc:xacml:3.0:policy-combining-algorithm:deny-unless-permit">

    <Target/>

    <PolicySetIdReference>student-read-ucon-policy-set</PolicySetIdReference>
</PolicySet>
```

### Nạp root version mới lên AuthzForce

```bash
curl -i --request POST \
  --header "Content-Type: application/xml; charset=UTF-8" \
  --data-binary @root-0.1.5.xml \
  "http://localhost:8080/authzforce-ce/domains/HxXn0yzoEfGh4QJCrBEAAg/pap/policies"
```

Nếu thành công, kết quả sẽ có dạng:

```xml
<link ... href="root/0.1.5" title="Policy 'root' v0.1.5"/>
```

Điều này nghĩa là version `0.1.5` của policy `root` đã được nạp vào domain.

### Ý nghĩa

Khi domain đang dùng `root` làm root policy, việc nạp một version mới của `root` là cách đơn giản để thay đổi cây policy mà PDP sẽ đi vào đánh giá. Trong thực tế, `root` đóng vai trò “policy gốc”, còn policy set nghiệp vụ thực sự nằm ở các `PolicySetIdReference` mà `root` trỏ tới.

---

## 7. Kiểm tra policy đã nạp

### Liệt kê tất cả policy trong domain

```bash
curl -s \
  -H "Accept: application/xml; charset=UTF-8" \
  "http://localhost:<HOST_PORT>/authzforce-ce/domains/<DOMAIN_ID>/pap/policies"
```

Ví dụ:

```bash
curl -s \
  -H "Accept: application/xml; charset=UTF-8" \
  "http://localhost:8080/authzforce-ce/domains/HxXn0yzoEfGh4QJCrBEAAg/pap/policies"
```

### Liệt kê các version của một policy

```bash
curl -s \
  -H "Accept: application/xml; charset=UTF-8" \
  "http://localhost:<HOST_PORT>/authzforce-ce/domains/<DOMAIN_ID>/pap/policies/<POLICY_ID>"
```

Ví dụ:

```bash
curl -s \
  -H "Accept: application/xml; charset=UTF-8" \
  "http://localhost:8080/authzforce-ce/domains/HxXn0yzoEfGh4QJCrBEAAg/pap/policies/root"
```

### Lấy một version cụ thể

```bash
curl -s \
  -H "Accept: application/xml; charset=UTF-8" \
  "http://localhost:<HOST_PORT>/authzforce-ce/domains/<DOMAIN_ID>/pap/policies/<POLICY_ID>/<VERSION>"
```

### Lấy version mới nhất

```bash
curl -s \
  -H "Accept: application/xml; charset=UTF-8" \
  "http://localhost:<HOST_PORT>/authzforce-ce/domains/<DOMAIN_ID>/pap/policies/<POLICY_ID>/latest"
```

---

## 8. Versioning policy

AuthzForce quản lý policy theo cặp:

- `PolicySetId`
- `Version`

Muốn tạo version mới của cùng một policy, hãy:

- giữ nguyên `PolicySetId`
- đổi `Version`
- nạp lại qua `POST /pap/policies`

### Ví dụ

- version cũ: `PolicySetId="root"` + `Version="0.1.4"`
- version mới: `PolicySetId="root"` + `Version="0.1.5"`

Sau đó có thể:

- xem version cụ thể: `/pap/policies/root/0.1.5`
- xem version mới nhất: `/pap/policies/root/latest`

Versioning theo cách này rất phù hợp với root policy, vì bạn có thể tăng dần `0.1.1`, `0.1.2`, `0.1.3`, `0.1.4`, `0.1.5`, ... mà không phá version cũ.

---

## 9. Xóa policy

AuthzForce hỗ trợ xóa:

- **một version cụ thể**
- hoặc **toàn bộ các version** của một policy

### Xóa một version cụ thể

```bash
curl --request DELETE \
  --header "Accept: application/xml; charset=UTF-8" \
  "http://localhost:<HOST_PORT>/authzforce-ce/domains/<DOMAIN_ID>/pap/policies/<POLICY_ID>/<VERSION>"
```

### Ví dụ

```bash
curl -i --request DELETE \
  "http://localhost:8080/authzforce-ce/domains/HxXn0yzoEfGh4QJCrBEAAg/pap/policies/root/0.1.4"
```

### Xóa toàn bộ các version của một policy

```bash
curl --request DELETE \
  --header "Accept: application/xml; charset=UTF-8" \
  "http://localhost:<HOST_PORT>/authzforce-ce/domains/<DOMAIN_ID>/pap/policies/<POLICY_ID>"
```

### Ví dụ

```bash
curl -i --request DELETE \
  "http://localhost:8080/authzforce-ce/domains/HxXn0yzoEfGh4QJCrBEAAg/pap/policies/root"
```

### Lưu ý khi xóa policy

- Không nên xóa version mà hệ thống đang cần dùng
- Với `root`, nên nạp version mới trước rồi mới cân nhắc xóa version cũ
- Nếu xóa nhầm version đang được sử dụng hoặc đang được tham chiếu, AuthzForce có thể trả lỗi nội bộ hoặc PDP không đánh giá được đúng như mong muốn

Nói thực tế hơn:

1. nạp root version mới
2. test PDP decision ổn
3. sau đó mới dọn version cũ

---

## 10. Gửi request để PDP đánh giá

PDP endpoint của AuthzForce là:

```text
/domains/<DOMAIN_ID>/pdp
```

Request phải là **XACML Request XML**, gửi bằng `POST`.

### Cú pháp tổng quát

```bash
curl --request POST \
  --header "Accept: application/xml; charset=UTF-8" \
  --header "Content-Type: application/xml; charset=UTF-8" \
  --data-binary @<REQUEST_FILE>.xml \
  "http://localhost:<HOST_PORT>/authzforce-ce/domains/<DOMAIN_ID>/pdp"
```

### Ví dụ request file

File `student-read-policy-request.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Request xmlns="urn:oasis:names:tc:xacml:3.0:core:schema:wd-17"
         ReturnPolicyIdList="false"
         CombinedDecision="false">

    <Attributes Category="urn:oasis:names:tc:xacml:3.0:attribute-category:action">
        <Attribute AttributeId="urn:oasis:names:tc:xacml:1.0:action:action-id" IncludeInResult="false">
            <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string">student.read</AttributeValue>
        </Attribute>
    </Attributes>

    <Attributes Category="urn:oasis:names:tc:xacml:1.0:subject-category:access-subject">
        <Attribute AttributeId="urn:oasis:names:tc:xacml:2.0:subject:role" IncludeInResult="false">
            <AttributeValue DataType="http://www.w3.org/2001/XMLSchema#string">SV</AttributeValue>
        </Attribute>
    </Attributes>

</Request>
```

### Ví dụ gửi request

```bash
curl --request POST \
  --header "Accept: application/xml; charset=UTF-8" \
  --header "Content-Type: application/xml; charset=UTF-8" \
  --data-binary @student-read-policy-request.xml \
  "http://localhost:8080/authzforce-ce/domains/HxXn0yzoEfGh4QJCrBEAAg/pdp"
```

Nếu request hợp lệ, kết quả trả về sẽ là XACML `Response`, ví dụ có `<Decision>Permit</Decision>` hoặc `<Decision>Deny</Decision>`. Nếu request XML sai hoặc thiếu attribute cần thiết, AuthzForce thường trả HTTP 400 hoặc kết quả `Indeterminate` tùy trường hợp.

---

## 11. Mẹo debug

### Xem log container

```bash
docker logs authzforce
```

Theo dõi realtime:

```bash
docker logs -f authzforce
```

### Trên zsh, URL có `?` thì phải đặt trong dấu nháy

```bash
curl -v "http://localhost:8080/authzforce-ce/?_wadl"
```

Nếu không, `zsh` có thể hiểu `?` là wildcard và báo lỗi `no matches found`.

### Nếu `curl --data-binary @file.xml` báo không mở được file

Kiểm tra lại file có tồn tại không:

```bash
ls -l
```

Hoặc dùng đúng đường dẫn tương đối:

```bash
--data-binary @policies/root-0.1.5.xml
```

### Nếu `POST /pap/policies` báo lỗi

Những nguyên nhân thường gặp:

- file gửi lên là `Policy` thay vì `PolicySet`
- XML không well-formed
- version bị trùng
- `PolicySetIdReference` trỏ tới policy chưa có trong domain
- `AttributeId`, `Target`, `Condition` viết không hợp lệ với engine

---

## 12. Quy trình thao tác khuyến nghị

1. chạy container AuthzForce
2. tạo domain
3. nạp policy set nghiệp vụ
4. tạo file `root` version mới
5. nạp file `root` mới vào `/pap/policies`
6. kiểm tra version `root` đã có trong domain
7. gửi XACML Request tới PDP
8. xem decision
9. nếu ổn, mới cân nhắc xóa version root cũ

---

## 13. Placeholder cần thay trước khi chạy

Khi copy lệnh, thay các giá trị sau theo máy của bạn:

- `<HOST_PORT>`: cổng máy host, ví dụ `8080`
- `<CONTAINER_NAME>`: tên container, ví dụ `authzforce`
- `<TAG>`: version Docker image, ví dụ `12.0.1`
- `<DOMAIN_ID>`: ID domain do AuthzForce trả về
- `<POLICYSET_FILE>`: file XML policy set
- `<POLICY_ID>`: `PolicySetId` của policy
- `<VERSION>`: version policy, ví dụ `1.0` hoặc `0.1.5`
- `<REQUEST_FILE>`: file XACML Request XML
- `<TARGET_POLICYSET_ID>`: policy set nghiệp vụ mà root sẽ trỏ tới

---

## 14. Ví dụ ngắn gọn end-to-end

### Bước 1: nạp policy nghiệp vụ

```bash
curl -i --request POST \
  --header "Content-Type: application/xml; charset=UTF-8" \
  --data-binary @student-read-ucon-policy-set.xml \
  "http://localhost:8080/authzforce-ce/domains/HxXn0yzoEfGh4QJCrBEAAg/pap/policies"
```

### Bước 2: tạo root version mới

File `root-0.1.5.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<PolicySet
    xmlns="urn:oasis:names:tc:xacml:3.0:core:schema:wd-17"
    PolicySetId="root"
    Version="0.1.5"
    PolicyCombiningAlgId="urn:oasis:names:tc:xacml:3.0:policy-combining-algorithm:deny-unless-permit">

    <Target/>

    <PolicySetIdReference>student-read-ucon-policy-set</PolicySetIdReference>
</PolicySet>
```

### Bước 3: nạp root version mới

```bash
curl -i --request POST \
  --header "Content-Type: application/xml; charset=UTF-8" \
  --data-binary @root-0.1.5.xml \
  "http://localhost:8080/authzforce-ce/domains/HxXn0yzoEfGh4QJCrBEAAg/pap/policies"
```

### Bước 4: gửi request để test PDP

```bash
curl --request POST \
  --header "Accept: application/xml; charset=UTF-8" \
  --header "Content-Type: application/xml; charset=UTF-8" \
  --data-binary @student-read-policy-request.xml \
  "http://localhost:8080/authzforce-ce/domains/HxXn0yzoEfGh4QJCrBEAAg/pdp"
```

---

## 15. Ghi chú cuối

Cách làm trong README này ưu tiên sự đơn giản và dễ thao tác:

- không chỉnh trực tiếp policy cũ
- không sửa lung tung cấu hình PDP
- chỉ cần tạo **version mới của root** và nạp lên PAP

Cách đó rất phù hợp cho việc test policy, demo versioning, và rollback khi cần.

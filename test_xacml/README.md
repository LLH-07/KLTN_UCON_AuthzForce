# Ebook UCON + XACML + AuthzForce

## Thư mục
- `policies/ebook-ucon-way1.xml`: cách 1, bố cục thực dụng.
- `policies/ebook-ucon-way2-student-style.xml`: cách 2, theo style gần giống `student-read-policy.xml`.
- `requests/*.xml`: request mẫu để test.
- `scripts/*.ps1`: script PowerShell để chạy trên Windows.

## Chạy nhanh bằng Docker Desktop trên Windows
Mở PowerShell tại thư mục dự án rồi chạy:

```powershell
docker pull authzforce/server:12.0.1
docker run -d -p 8080:8080 --name authzforce authzforce/server:12.0.1
```

Kiểm tra container lên:

```
docker ps
docker logs authzforce
```

Kiểm tra server lên:

```powershell
curl http://localhost:8080/authzforce-ce/?_wadl
curl http://localhost:8080/authzforce-ce/domains
```

Nếu máy bạn đã lỡ tạo container tên authzforce nhưng chạy lỗi nửa chừng, xóa rồi chạy lại:
```
docker rm -f authzforce
docker run -d -p 8080:8080 --name authzforce authzforce/server:12.0.1
```


## Tạo domain
```powershell
.\scripts\create_domain.ps1
```

Script sẽ trả XML domain. Bạn lấy `domainId`.

## Upload policy
Ví dụ dùng cách 1:

```powershell
.\scripts\upload_policy.ps1 -DomainId "<DOMAIN_ID>" -PolicyFile ".\policies\ebook-ucon-way1.xml"
```

Hoặc dùng cách 2:

```powershell
.\scripts\upload_policy.ps1 -DomainId "<DOMAIN_ID>" -PolicyFile ".\policies\ebook-ucon-way2-student-style.xml"
```

## Test request
Ví dụ:

```powershell
.\scripts\evaluate_request.ps1 -DomainId "<DOMAIN_ID>" -RequestFile ".\requests\01-preview-ok.xml"
.\scripts\evaluate_request.ps1 -DomainId "<DOMAIN_ID>" -RequestFile ".\requests\02-read-full-purchase-ok.xml"
.\scripts\evaluate_request.ps1 -DomainId "<DOMAIN_ID>" -RequestFile ".\requests\04-read-full-missing-ad.xml"
.\scripts\evaluate_request.ps1 -DomainId "<DOMAIN_ID>" -RequestFile ".\requests\06-continue-read-rental-expired.xml"
```

## Ý nghĩa kết quả
- `Permit`: cho phép.
- `Deny`: từ chối.
- `Obligations`: việc app/PEP phải làm tiếp. Ví dụ:
  - tạo session đọc;
  - tạo ad log PENDING;
  - pause session;
  - revoke session;
  - ghi usage record;
  - close session.

## Gợi ý test từng policy
1. `01-preview-ok.xml`: Policy 1.
2. `02-read-full-purchase-ok.xml`: Policy 2.
3. `03-read-full-rental-with-ad-ok.xml`: Policy 3 + 5.
4. `04-read-full-missing-ad.xml`: Policy 5.
5. `05-continue-read-ok.xml`: Policy 4 + 6 nhánh cho đọc tiếp.
6. `06-continue-read-rental-expired.xml`: Policy 4 nhánh revoke.
7. `07-continue-read-periodic-ad-due.xml`: Policy 6 nhánh pause/ad.
8. `08-end-read-post-rental.xml`: Policy 3 phần postUpdate.

## Lưu ý rất quan trọng
AuthzForce là PDP/PAP, không tự:
- phát quảng cáo,
- tạo session,
- sửa database,
- revoke phiên ở app.

Nó chỉ trả về quyết định và obligations/advice. Chính web app / backend / PEP của bạn phải thực thi các obligation đó.

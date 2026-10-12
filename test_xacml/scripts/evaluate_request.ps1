param(
    [Parameter(Mandatory = $true)][string]$DomainId,
    [Parameter(Mandatory = $true)][string]$RequestFile,
    [string]$BaseUrl = "http://localhost:8080/authzforce-ce"
)

$requestText = Get-Content -Path $RequestFile -Raw

$response = Invoke-RestMethod -Method Post `
    -Uri "$BaseUrl/domains/$DomainId/pdp" `
    -ContentType "application/xml; charset=UTF-8" `
    -Headers @{ Accept = "application/xml; charset=UTF-8" } `
    -Body $requestText

$response

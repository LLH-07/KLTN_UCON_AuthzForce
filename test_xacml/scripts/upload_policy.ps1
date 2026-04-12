param(
    [Parameter(Mandatory = $true)][string]$DomainId,
    [Parameter(Mandatory = $true)][string]$PolicyFile,
    [string]$BaseUrl = "http://localhost:8080/authzforce-ce",
    [string]$RootPolicyId = "root"
)

$policyText = Get-Content -Path $PolicyFile -Raw

Invoke-RestMethod -Method Post `
    -Uri "$BaseUrl/domains/$DomainId/pap/policies" `
    -ContentType "application/xml; charset=UTF-8" `
    -Headers @{ Accept = "application/xml; charset=UTF-8" } `
    -Body $policyText | Out-Null

$rootBody = @"
<?xml version="1.0" encoding="UTF-8"?>
<pdpPropertiesUpdate xmlns="http://authzforce.github.io/rest-api-model/xmlns/authz/5">
    <rootPolicyRefExpression>$RootPolicyId</rootPolicyRefExpression>
</pdpPropertiesUpdate>
"@

Invoke-RestMethod -Method Put `
    -Uri "$BaseUrl/domains/$DomainId/pap/pdp.properties" `
    -ContentType "application/xml; charset=UTF-8" `
    -Headers @{ Accept = "application/xml; charset=UTF-8" } `
    -Body $rootBody | Out-Null

Write-Host "Policy uploaded and rootPolicyRefExpression set to '$RootPolicyId'."

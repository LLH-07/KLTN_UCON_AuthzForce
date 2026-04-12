param(
    [string]$BaseUrl = "http://localhost:8080/authzforce-ce",
    [string]$ExternalId = "ebook-ucon-demo"
)

$body = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<domainProperties xmlns="http://authzforce.github.io/rest-api-model/xmlns/authz/5" externalId="$ExternalId">
    <description>Ebook UCON demo domain</description>
</domainProperties>
"@

try {
    $response = Invoke-WebRequest -Method Post `
        -Uri "$BaseUrl/domains" `
        -ContentType "application/xml; charset=UTF-8" `
        -Headers @{ Accept = "application/xml" } `
        -Body $body `
        -ErrorAction Stop

    [xml]$xml = $response.Content
    $ns = New-Object System.Xml.XmlNamespaceManager($xml.NameTable)
    $ns.AddNamespace("atom", "http://www.w3.org/2005/Atom")
    $linkNode = $xml.SelectSingleNode("//*[local-name()='link']", $ns)

    if ($null -eq $linkNode) {
        throw "Domain creation response received, but no domain link was found. Raw response: $($response.Content)"
    }

    $domainId = $linkNode.GetAttribute("href")
    Write-Host "Domain created successfully."
    Write-Host "Domain ID: $domainId"
    $domainId
}
catch {
    Write-Error "Domain creation failed. $($_.Exception.Message)"
    if ($_.Exception.Response -ne $null) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.BaseStream.Position = 0
        $reader.DiscardBufferedData()
        $raw = $reader.ReadToEnd()
        Write-Host "Server response:"
        Write-Host $raw
    }
    exit 1
}
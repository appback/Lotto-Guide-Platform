# DHLottery API Test Script
# This script tests if the DHLottery API is working correctly.

param(
    [int]$DrawNo = 1200  # Draw number to test (default: 1200)
)

# User-Agent string
$userAgent = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'

# Accept header values (to avoid semicolon issues)
$acceptHtml = 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8'
$acceptJson = 'application/json, text/javascript, */*; q=0.01'
$acceptLanguage = 'ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7'

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DHLottery API Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Draw Number: $DrawNo" -ForegroundColor Yellow
Write-Host ""

# Step 1: Access main page to get session cookies
Write-Host "[Step 1] Accessing main page to get session cookies..." -ForegroundColor Green
try {
    $mainHeaders = @{
        "User-Agent" = $userAgent
        "Accept" = $acceptHtml
        "Accept-Language" = $acceptLanguage
    }
    
    $mainResponse = Invoke-WebRequest -Uri "https://www.dhlottery.co.kr/" `
        -Method GET `
        -Headers $mainHeaders `
        -UseBasicParsing `
        -SessionVariable session
    
    Write-Host "OK Main page access successful" -ForegroundColor Green
    Write-Host "  Status: $($mainResponse.StatusCode)" -ForegroundColor Gray
    Write-Host "  Cookie count: $($session.Cookies.Count)" -ForegroundColor Gray
    
    # Print cookie information
    foreach ($cookie in $session.Cookies) {
        $cookieValue = if ($cookie.Value.Length -gt 30) { $cookie.Value.Substring(0, 30) + "..." } else { $cookie.Value }
        Write-Host "  - $($cookie.Name)=$cookieValue" -ForegroundColor Gray
    }
    
    Start-Sleep -Milliseconds 500
    
} catch {
    Write-Host "FAILED Main page access failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 2: Access draw result page
Write-Host ""
Write-Host "[Step 2] Accessing draw result page..." -ForegroundColor Green
try {
    $resultHeaders = @{
        "User-Agent" = $userAgent
        "Referer" = "https://www.dhlottery.co.kr/"
        "Accept" = $acceptHtml
    }
    
    $resultResponse = Invoke-WebRequest -Uri "https://www.dhlottery.co.kr/gameResult.do?method=byWin" `
        -Method GET `
        -WebSession $session `
        -Headers $resultHeaders `
        -UseBasicParsing `
        -MaximumRedirection 5
    
    Write-Host "OK Draw result page access successful" -ForegroundColor Green
    Write-Host "  Status: $($resultResponse.StatusCode)" -ForegroundColor Gray
    $contentType = if ($resultResponse.Headers['Content-Type']) { $resultResponse.Headers['Content-Type'] } else { "N/A" }
    Write-Host "  Content-Type: $contentType" -ForegroundColor Gray
    
    Start-Sleep -Milliseconds 500
    
} catch {
    Write-Host "FAILED Draw result page access failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  (May be redirected, continuing...)" -ForegroundColor Yellow
}

# Step 3: API call - Try HAR confirmed endpoint first
Write-Host ""
Write-Host "[Step 3] Calling DHLottery API..." -ForegroundColor Green

# Method 1: HAR confirmed endpoint (selectPstLt645Info.do - returns all draws)
Write-Host "  [Method 1] Trying HAR confirmed endpoint (selectPstLt645Info.do)..." -ForegroundColor Yellow
$listApiUrl = 'https://www.dhlottery.co.kr/lt645/selectPstLt645Info.do?srchLtEpsd=all'
Write-Host "  URL: $listApiUrl" -ForegroundColor Gray

try {
    $listApiHeaders = @{
        "User-Agent" = $userAgent
        "Referer" = "https://www.dhlottery.co.kr/lt645/result"
        "Accept" = $acceptJson
        "X-Requested-With" = "XMLHttpRequest"
        "Origin" = "https://www.dhlottery.co.kr"
        "Accept-Language" = $acceptLanguage
        "Sec-Fetch-Dest" = "empty"
        "Sec-Fetch-Mode" = "cors"
        "Sec-Fetch-Site" = "same-origin"
    }
    
    $listApiResponse = Invoke-WebRequest -Uri $listApiUrl `
        -Method GET `
        -WebSession $session `
        -Headers $listApiHeaders `
        -UseBasicParsing
    
    Write-Host "  OK List API call successful" -ForegroundColor Green
    Write-Host "    Status: $($listApiResponse.StatusCode)" -ForegroundColor Gray
    $listContentType = if ($listApiResponse.Headers['Content-Type']) { $listApiResponse.Headers['Content-Type'] } else { "N/A" }
    Write-Host "    Content-Type: $listContentType" -ForegroundColor Gray
    Write-Host "    Response length: $($listApiResponse.Content.Length) bytes" -ForegroundColor Gray
    
    $listContent = $listApiResponse.Content
    if ($listContent.Trim().StartsWith("{") -or $listContent.Trim().StartsWith("[")) {
        Write-Host "    ✓ JSON response received!" -ForegroundColor Green
        Write-Host "    First 500 characters:" -ForegroundColor Yellow
        $previewLength = [Math]::Min(500, $listContent.Length)
        Write-Host $listContent.Substring(0, $previewLength) -ForegroundColor Gray
        
        # Try to parse and find the specific draw
        try {
            $listJson = $listContent | ConvertFrom-Json
            if ($listJson.data -and $listJson.data.list) {
                $targetDraw = $listJson.data.list | Where-Object { $_.drwNo -eq $DrawNo }
                if ($targetDraw) {
                    Write-Host ""
                    Write-Host "  ✓ Found draw $DrawNo in list!" -ForegroundColor Green
                    $targetDraw | ConvertTo-Json -Depth 10 | Write-Host -ForegroundColor Gray
                } else {
                    Write-Host "  ? Draw $DrawNo not found in list" -ForegroundColor Yellow
                }
            }
        } catch {
            Write-Host "  ? Could not parse JSON structure" -ForegroundColor Yellow
        }
    } else {
        Write-Host "    ✗ Not JSON response" -ForegroundColor Red
    }
} catch {
    Write-Host "  ✗ List API call failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "  [Method 2] Trying original endpoint (common.do)..." -ForegroundColor Yellow
# Method 2: Original endpoint (for comparison)
$apiUrl = 'https://www.dhlottery.co.kr/common.do?method=getLottoNumber&drwNo=' + $DrawNo
Write-Host "  URL: $apiUrl" -ForegroundColor Gray

try {
    $apiHeaders = @{
        "User-Agent" = $userAgent
        "Referer" = "https://www.dhlottery.co.kr/gameResult.do?method=byWin"
        "Accept" = $acceptJson
        "X-Requested-With" = "XMLHttpRequest"
        "Origin" = "https://www.dhlottery.co.kr"
        "Accept-Language" = $acceptLanguage
        "Sec-Fetch-Dest" = "empty"
        "Sec-Fetch-Mode" = "cors"
        "Sec-Fetch-Site" = "same-origin"
    }
    
    $apiResponse = Invoke-WebRequest -Uri $apiUrl `
        -Method GET `
        -WebSession $session `
        -Headers $apiHeaders `
        -UseBasicParsing
    
    Write-Host "OK API call successful" -ForegroundColor Green
    Write-Host "  Status: $($apiResponse.StatusCode)" -ForegroundColor Gray
    $apiContentType = if ($apiResponse.Headers['Content-Type']) { $apiResponse.Headers['Content-Type'] } else { "N/A" }
    Write-Host "  Content-Type: $apiContentType" -ForegroundColor Gray
    Write-Host "  Response length: $($apiResponse.Content.Length) bytes" -ForegroundColor Gray
    Write-Host ""
    
    # Check response content
    $content = $apiResponse.Content
    Write-Host "[Response Content]" -ForegroundColor Cyan
    
    if ($content.Trim().StartsWith("<")) {
        Write-Host "FAILED Received HTML response (API blocked or error)" -ForegroundColor Red
        Write-Host "  First 500 characters:" -ForegroundColor Yellow
        $previewLength = [Math]::Min(500, $content.Length)
        Write-Host $content.Substring(0, $previewLength) -ForegroundColor Gray
    } elseif ($content.Trim().StartsWith("{") -or $content.Trim().StartsWith("[")) {
        Write-Host "OK Received JSON response" -ForegroundColor Green
        Write-Host "  Response content:" -ForegroundColor Yellow
        
        # Try to format JSON
        try {
            $json = $content | ConvertFrom-Json
            $json | ConvertTo-Json -Depth 10 | Write-Host -ForegroundColor Gray
        } catch {
            Write-Host $content -ForegroundColor Gray
        }
    } else {
        Write-Host "? Unknown response format" -ForegroundColor Yellow
        Write-Host "  First 200 characters:" -ForegroundColor Yellow
        $previewLength = [Math]::Min(200, $content.Length)
        Write-Host $content.Substring(0, $previewLength) -ForegroundColor Gray
    }
    
} catch {
    Write-Host "FAILED API call failed: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "  HTTP Status: $statusCode" -ForegroundColor Yellow
        
        try {
            $errorStream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($errorStream)
            $errorContent = $reader.ReadToEnd()
            Write-Host "  Error response content (first 500 chars):" -ForegroundColor Yellow
            $errorPreviewLength = [Math]::Min(500, $errorContent.Length)
            Write-Host $errorContent.Substring(0, $errorPreviewLength) -ForegroundColor Gray
        } catch {
            Write-Host "  Cannot read error response content" -ForegroundColor Yellow
        }
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

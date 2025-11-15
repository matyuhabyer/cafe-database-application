# PowerShell Script to Test Servlets
# Usage: .\test-servlets.ps1

$baseUrl = "http://localhost:8080/cafedbapp/api/auth"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Servlet Testing Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test Register
Write-Host "Testing Register Servlet..." -ForegroundColor Yellow
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$registerData = @{
    username = "testuser$timestamp"
    password = "password123"
    name = "Test User"
    email = "test$timestamp@example.com"
    phone_num = "1234567890"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$baseUrl/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $registerData
    
    Write-Host "✅ Register Success!" -ForegroundColor Green
    Write-Host "Response:" -ForegroundColor Cyan
    $registerResponse | ConvertTo-Json -Depth 10
    Write-Host ""
    
    $testUsername = $registerData | ConvertFrom-Json | Select-Object -ExpandProperty username
    $testPassword = $registerData | ConvertFrom-Json | Select-Object -ExpandProperty password
    
} catch {
    Write-Host "❌ Register Failed!" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    
    # Use default test credentials
    $testUsername = "testuser"
    $testPassword = "password123"
}

# Test Login - Customer
Write-Host "Testing Login Servlet (Customer)..." -ForegroundColor Yellow
$loginData = @{
    username = $testUsername
    password = $testPassword
    role = "customer"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginData `
        -SessionVariable session
    
    Write-Host "✅ Login Success!" -ForegroundColor Green
    Write-Host "Response:" -ForegroundColor Cyan
    $loginResponse | ConvertTo-Json -Depth 10
    Write-Host ""
} catch {
    Write-Host "❌ Login Failed!" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

# Test Login - Staff
Write-Host "Testing Login Servlet (Staff)..." -ForegroundColor Yellow
$staffLoginData = @{
    username = "staff1"
    password = "password123"
    role = "staff"
} | ConvertTo-Json

try {
    $staffLoginResponse = Invoke-RestMethod -Uri "$baseUrl/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $staffLoginData
    
    Write-Host "✅ Staff Login Success!" -ForegroundColor Green
    Write-Host "Response:" -ForegroundColor Cyan
    $staffLoginResponse | ConvertTo-Json -Depth 10
    Write-Host ""
} catch {
    Write-Host "❌ Staff Login Failed (this is OK if staff user doesn't exist)" -ForegroundColor Yellow
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testing Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan


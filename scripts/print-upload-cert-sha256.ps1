# Prints the upload keystore SHA-256 (NOT the Play App Signing key).
# For App Links on Play-installed builds, use Play Console → App integrity → App signing.

$ErrorActionPreference = "Stop"
$propsFile = Join-Path $PSScriptRoot "..\release\signing.properties"
if (-not (Test-Path $propsFile)) {
    Write-Error "Missing release/signing.properties"
}

$props = @{}
Get-Content $propsFile | ForEach-Object {
    if ($_ -match '^\s*([^#=]+)=(.*)$') {
        $props[$matches[1].Trim()] = $matches[2].Trim()
    }
}

$store = $props["PREZZENCE_UPLOAD_STORE_FILE"]
$alias = $props["PREZZENCE_UPLOAD_KEY_ALIAS"]
$storePass = $props["PREZZENCE_UPLOAD_STORE_PASSWORD"]
$keyPass = $props["PREZZENCE_UPLOAD_KEY_PASSWORD"]

if (-not [System.IO.Path]::IsPathRooted($store)) {
    $store = Join-Path (Resolve-Path (Join-Path $PSScriptRoot "..")) $store
}

Write-Host "Upload keystore: $store"
Write-Host ""
keytool -list -v -keystore $store -alias $alias -storepass $storePass -keypass $keyPass |
    Select-String -Pattern "SHA256:"
Write-Host ""
Write-Host "For production App Links, paste the Play App Signing SHA-256 into Render:"
Write-Host "  ANDROID_APP_SHA256_FINGERPRINTS=<Play Console SHA-256>"

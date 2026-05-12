$ErrorActionPreference = 'Stop'
$accountsPath = Join-Path $PSScriptRoot 'customers-100.json'

k6 run `
  -e BASE_URL=http://localhost:8080 `
  -e CUSTOMER_ACCOUNTS_PATH="$accountsPath" `
  $PSScriptRoot\order-load.test.js

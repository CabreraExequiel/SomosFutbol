$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$destination = Join-Path $projectRoot '.env'
if (Test-Path $destination) { throw 'Ya existe .env; no se modificó.' }
$contents = [IO.File]::ReadAllText((Join-Path $projectRoot '.env.example'))
function New-RandomBytes([int] $Count) {
    $buffer = New-Object byte[] $Count
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($buffer) } finally { $generator.Dispose() }
    return ,$buffer
}
$values = @{
    DB_PASSWORD = [BitConverter]::ToString((New-RandomBytes 24)).Replace('-', '').ToLowerInvariant()
    DB_ROOT_PASSWORD = [BitConverter]::ToString((New-RandomBytes 32)).Replace('-', '').ToLowerInvariant()
    JWT_SECRET = [BitConverter]::ToString((New-RandomBytes 48)).Replace('-', '').ToLowerInvariant()
    MP_ENCRYPTION_KEY = [Convert]::ToBase64String((New-RandomBytes 32))
}
foreach ($key in $values.Keys) {
    $contents = $contents.Replace("$key=`n", "$key=$($values[$key])`n").Replace("$key=`r`n", "$key=$($values[$key])`r`n")
}
$stream = [IO.File]::Open($destination, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
try {
    $bytes = [Text.UTF8Encoding]::new($false).GetBytes($contents)
    $stream.Write($bytes, 0, $bytes.Length)
} finally { $stream.Dispose() }
Write-Output '.env creado. Completá dominio, datos legales y servicios. Conservá este archivo en privado.'

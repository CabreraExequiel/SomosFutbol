# Etapa 10A - Solo sobre la API LOCAL: no admite destino remoto deliberadamente.
$ErrorActionPreference = 'Stop'
$api = 'http://localhost:8080'

function Comprobar([bool]$condicion, [string]$mensaje) {
    if (-not $condicion) { throw "[ERROR] $mensaje" }
    Write-Host "[OK] $mensaje" -ForegroundColor Green
}
function EstadoHttp([scriptblock]$accion) {
    try {
        & $accion | Out-Null
        return 200
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw
    }
}

Write-Host 'Etapa 10A - Comprobaciones de publicidades en LOCAL'
Write-Host "API: $api"
try { Invoke-WebRequest -UseBasicParsing "$api/actuator/health" -TimeoutSec 5 | Out-Null } catch {
    throw 'El backend local no responde en http://localhost:8080. Iniciá Spring Boot con XAMPP.'
}

$sinSesion = EstadoHttp { Invoke-RestMethod -Uri "$api/api/admin/publicidades" -Method Get }
Comprobar ($sinSesion -eq 401) 'Sin sesión, publicidades responde HTTP 401'

$email = Read-Host 'Correo del administrador LOCAL'
$contraseniaSegura = Read-Host 'Contraseña del administrador LOCAL' -AsSecureString
$puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($contraseniaSegura)
try {
    $contrasenia = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
    $payload = @{ email = $email; password = $contrasenia } | ConvertTo-Json
    $login = Invoke-RestMethod -Method Post -Uri "$api/api/auth/login" -ContentType 'application/json' -Body $payload
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
    $contrasenia = $null
    $payload = $null
}
if (-not $login.token) { throw 'La API local no devolvió un token de autenticación.' }
$cabeceras = @{ Authorization = "Bearer $($login.token)" }
$login = $null

$lista = Invoke-RestMethod -Method Get -Uri "$api/api/admin/publicidades?pagina=0" -Headers $cabeceras
Comprobar ($null -ne $lista.content) 'El administrador puede consultar la lista'

# Se prueba validación ANTES de crear la publicidad.
$bodyInvalido = @{
    titulo='Prueba de URL insegura'; descripcion='solo local'; destinoUrl='javascript:alert(1)';
    ubicacion='FEED'; prioridad=0
} | ConvertTo-Json
$estadoInvalido = EstadoHttp {
    Invoke-RestMethod -Method Post -Uri "$api/api/admin/publicidades" -Headers $cabeceras -ContentType 'application/json' -Body $bodyInvalido
}
Comprobar ($estadoInvalido -eq 400) 'Se rechaza URL javascript con HTTP 400'

# Crea un borrador de prueba y lo archiva, sin publicar anuncios reales.
$body = @{
    titulo='Prueba local 10A'; descripcion='Creada por PROBAR_PUBLICIDADES_10A.ps1';
    imagenUrl='https://example.com/prueba.jpg'; destinoUrl='https://example.com';
    ubicacion='FEED'; prioridad=1
} | ConvertTo-Json
$creada = Invoke-RestMethod -Method Post -Uri "$api/api/admin/publicidades" -Headers $cabeceras -ContentType 'application/json' -Body $body
Comprobar (($creada.estado -eq 'BORRADOR') -and ($creada.id -gt 0)) 'Crea publicidad como BORRADOR'
$id = [long]$creada.id
$archivada = Invoke-RestMethod -Method Patch -Uri "$api/api/admin/publicidades/$id/estado" -Headers $cabeceras -ContentType 'application/json' -Body '{"estado":"ARCHIVADA"}'
Comprobar ($archivada.estado -eq 'ARCHIVADA') 'Permite archivar el anuncio de prueba'
$reactivacion = EstadoHttp {
    Invoke-RestMethod -Method Patch -Uri "$api/api/admin/publicidades/$id/estado" -Headers $cabeceras -ContentType 'application/json' -Body '{"estado":"ACTIVA"}'
}
Comprobar ($reactivacion -eq 409) 'Impide reactivar una publicidad archivada (409)'
Write-Host "Comprobaciones correctas. Anuncio de prueba archivado: ID $id"
$cabeceras = $null

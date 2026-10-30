# SOMOS FUTBOL 10E - Solo localhost:8080 y solicitudes de SOLO LECTURA.
# No crea anuncios, no cambia estados y no registra impresiones/clics.
param([switch]$ProbarJugador)
$ErrorActionPreference = 'Stop'
$api = 'http://localhost:8080'
$ok = 0

function Afirmar([bool]$condicion, [string]$descripcion) {
    if (-not $condicion) { throw "[ERROR] $descripcion" }
    $script:ok++
    Write-Host "[OK] $descripcion" -ForegroundColor Green
}

function Estado([scriptblock]$peticion) {
    try {
        & $peticion | Out-Null
        return 200
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw
    }
}

function ObtenerToken([string]$rol) {
    Write-Host "Autenticacion de $rol (solo LOCAL)." -ForegroundColor Cyan
    $email = Read-Host "Correo de $rol LOCAL"
    $segura = Read-Host "Contrasena de $rol LOCAL" -AsSecureString
    $puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
    $password = $null
    $payload = $null
    try {
        $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
        $payload = @{ email = $email; password = $password } | ConvertTo-Json
        $login = Invoke-RestMethod -Uri "$api/api/auth/login" -Method Post `
            -ContentType 'application/json' -Body $payload -TimeoutSec 10
        if (-not $login.token) { throw "No se recibio token de $rol LOCAL" }
        return [string]$login.token
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
        $password = $null
        $payload = $null
        $segura = $null
    }
}

Write-Host 'ETAPA 10E - Verificacion de publicidades (SOLO LOCAL)' -ForegroundColor Cyan
Write-Host "API: $api"
try {
    Invoke-WebRequest -UseBasicParsing -Uri "$api/actuator/health" -TimeoutSec 5 | Out-Null
} catch { throw 'No responde Spring Boot LOCAL en http://localhost:8080.' }

# Endpoints publicos: lectura; no se registran contadores.
$respuestaPublica = Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$api/api/publicidades/visibles?ubicacion=FEED" -TimeoutSec 10
Afirmar ($respuestaPublica.StatusCode -eq 200) 'Listado publico FEED responde (200)'
$publicos = ConvertFrom-Json -InputObject $respuestaPublica.Content
foreach ($anuncio in @($publicos)) {
    if ($null -eq $anuncio) { continue }
    Afirmar (($anuncio.PSObject.Properties.Name -notcontains 'creadoPorId') -and
             ($anuncio.PSObject.Properties.Name -notcontains 'actualizadoPorId') -and
             ($anuncio.PSObject.Properties.Name -notcontains 'estado')) `
             'La respuesta publica no expone auditoria ni estado interno'
}
Afirmar ((Estado { Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$api/api/publicidades/visibles?ubicacion=NO_EXISTE" -TimeoutSec 10 }) -eq 400) `
    'Ubicacion invalida rechazada (400)'

# Endpoint protegido sin token. Ni siquiera se permite entrar a los controles.
Afirmar ((Estado { Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$api/api/admin/publicidades?pagina=0" -TimeoutSec 10 }) -eq 401) `
    'Listado administrativo anonimo rechazado (401)'
Afirmar ((Estado { Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$api/api/admin/publicidades/estadisticas?dias=7" -TimeoutSec 10 }) -eq 401) `
    'Estadisticas administrativas anonimas rechazadas (401)'

# Validaciones publicas: no deben modificar la tabla de metricas.
Afirmar ((Estado { Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$api/api/publicidades/0/impresiones" `
    -ContentType 'application/json' -Body '{"ubicacion":"FEED"}' -TimeoutSec 10 }) -eq 400) `
    'ID 0 de impresion es rechazado (400)'
Afirmar ((Estado { Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$api/api/publicidades/999999999/clics" `
    -ContentType 'application/json' -Body '{"ubicacion":"FEED"}' -TimeoutSec 10 }) -eq 404) `
    'Clic en anuncio inexistente rechazado (404)'

$token = ObtenerToken 'administrador'
$cabeceras = @{ Authorization = "Bearer $token" }
$token = $null
try {
    $lista = Invoke-RestMethod -Method Get -Uri "$api/api/admin/publicidades?pagina=0" -Headers $cabeceras -TimeoutSec 10
    Afirmar ($null -ne $lista.content) 'Administrador: listado accesible (200)'
    $datos7 = Invoke-RestMethod -Method Get -Uri "$api/api/admin/publicidades/estadisticas?dias=7" -Headers $cabeceras -TimeoutSec 10
    Afirmar (($datos7.dias -eq 7) -and (@($datos7.evolucion).Count -eq 7)) 'Administrador: estadisticas de 7 dias completas'
    $datos30 = Invoke-RestMethod -Method Get -Uri "$api/api/admin/publicidades/estadisticas?dias=30&ubicacion=FEED" -Headers $cabeceras -TimeoutSec 10
    Afirmar (($datos30.dias -eq 30) -and (@($datos30.evolucion).Count -eq 30)) 'Administrador: estadisticas de 30 dias completas'
    Afirmar ((Estado { Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$api/api/admin/publicidades/estadisticas?dias=365" -Headers $cabeceras -TimeoutSec 10 }) -eq 400) `
        'Administrador: periodo de 365 dias rechazado (400)'
    Afirmar ((Estado { Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$api/api/admin/publicidades/estadisticas?dias=7&publicidadId=-1" -Headers $cabeceras -TimeoutSec 10 }) -eq 400) `
        'Administrador: ID negativo rechazado (400)'
    Afirmar ((Estado { Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$api/api/admin/publicidades?pagina=-1" -Headers $cabeceras -TimeoutSec 10 }) -eq 400) `
        'Administrador: pagina negativa rechazada (400)'
    Afirmar ((Estado { Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$api/api/admin/publicidades/999999999" -Headers $cabeceras -TimeoutSec 10 }) -eq 404) `
        'Administrador: detalle inexistente rechazado (404)'
} finally {
    $cabeceras = $null
}

if ($ProbarJugador) {
    $tokenJugador = ObtenerToken 'jugador'
    $cabecerasJugador = @{ Authorization = "Bearer $tokenJugador" }
    $tokenJugador = $null
    try {
        Afirmar ((Estado { Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$api/api/admin/publicidades?pagina=0" -Headers $cabecerasJugador -TimeoutSec 10 }) -eq 403) `
            'Jugador: no accede al panel de publicidades (403)'
        Afirmar ((Estado { Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$api/api/admin/publicidades/estadisticas?dias=7" -Headers $cabecerasJugador -TimeoutSec 10 }) -eq 403) `
            'Jugador: no accede a estadisticas (403)'
    } finally { $cabecerasJugador = $null }
} else {
    Write-Host '[AVISO] Para probar HTTP 403 con un jugador, ejecuta el mismo script con -ProbarJugador.' -ForegroundColor Yellow
}

Write-Host "10E completa: $ok verificaciones correctas, 0 fallidas. Sin modificar datos locales." -ForegroundColor Green

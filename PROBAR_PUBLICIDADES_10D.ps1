# Comprueba exclusivamente localhost:8080. No acepta otro host a propÃ³sito.
$ErrorActionPreference = 'Stop'
$api = 'http://localhost:8080'
function Codigo([scriptblock]$accion) {
  try { & $accion | Out-Null; return 200 }
  catch {
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
      return [int]$_.Exception.Response.StatusCode
    }
    throw
  }
}
function Afirmar([bool]$vale, [string]$mensaje) {
  if (-not $vale) { throw "[ERROR] $mensaje" }
  Write-Host "[OK] $mensaje" -ForegroundColor Green
}
Write-Host 'Etapa 10D - Estadisticas de publicidades en LOCAL' -ForegroundColor Cyan
Invoke-RestMethod "$api/actuator/health" -TimeoutSec 5 | Out-Null
$sinSesion = Codigo { Invoke-RestMethod -Uri "$api/api/admin/publicidades/estadisticas?dias=7" -Method Get }
Afirmar ($sinSesion -eq 401) 'Estadisticas administrativas sin sesion: HTTP 401'
$malaFecha = Codigo { Invoke-RestMethod -Uri "$api/api/admin/publicidades/estadisticas?dias=999" -Method Get }
Afirmar ($malaFecha -eq 401) 'Sin sesion, parametros invalidos tambien se bloquean (401)'
$email = Read-Host 'Correo de administrador LOCAL'
$contraseniaSegura = Read-Host 'Contrasena de administrador LOCAL' -AsSecureString
$puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($contraseniaSegura)
try {
  $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
  $payload = @{email=$email;password=$password}|ConvertTo-Json
  $login = Invoke-RestMethod -Method Post -Uri "$api/api/auth/login" -Body $payload -ContentType 'application/json'
} finally {
  [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
  $password=$null; $payload=$null
}
if (-not $login.token) { throw 'No se recibio token local.' }
$headers=@{Authorization="Bearer $($login.token)"}
$login=$null
$datos7 = Invoke-RestMethod -Method Get -Uri "$api/api/admin/publicidades/estadisticas?dias=7" -Headers $headers
Afirmar (($datos7.dias -eq 7) -and (@($datos7.evolucion).Count -eq 7)) 'Estadisticas de 7 dias: HTTP 200 y serie completa'
$datos30 = Invoke-RestMethod -Method Get -Uri "$api/api/admin/publicidades/estadisticas?dias=30&ubicacion=FEED" -Headers $headers
Afirmar (($datos30.dias -eq 30) -and (@($datos30.evolucion).Count -eq 30)) 'Estadisticas de 30 dias filtradas por FEED'
$invalido = Codigo { Invoke-RestMethod -Method Get -Uri "$api/api/admin/publicidades/estadisticas?dias=365" -Headers $headers }
Afirmar ($invalido -eq 400) 'Periodo invalido se rechaza con HTTP 400'
$activos = @(Invoke-RestMethod -Method Get -Uri "$api/api/publicidades/visibles?ubicacion=FEED")
if ($activos.Count -gt 0 -and $null -ne $activos[0].id) {
  $id = [long]$activos[0].id
  $body='{"ubicacion":"FEED"}'
  $antes = Invoke-RestMethod -Method Get -Uri "$api/api/admin/publicidades/estadisticas?dias=7&publicidadId=$id" -Headers $headers
  $v = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$api/api/publicidades/$id/impresiones" -ContentType 'application/json' -Body $body
  Afirmar ($v.StatusCode -eq 204) "Impresion publica del anuncio #${id}: HTTP 204"
  $c = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$api/api/publicidades/$id/clics" -ContentType 'application/json' -Body $body
  Afirmar ($c.StatusCode -eq 204) "Clic publico del anuncio #${id}: HTTP 204"
  $despues = Invoke-RestMethod -Method Get -Uri "$api/api/admin/publicidades/estadisticas?dias=7&publicidadId=$id" -Headers $headers
  Afirmar (($despues.totales.impresiones -eq ($antes.totales.impresiones+1)) -and
           ($despues.totales.clics -eq ($antes.totales.clics+1))) 'Los dos contadores incrementaron exactamente una vez'
} else {
  Write-Host '[AVISO] No hay anuncio ACTIVO de FEED. Activa uno para probar impresiones y clics.' -ForegroundColor Yellow
}
Write-Host 'Pruebas 10D terminadas. Se usaron solamente servicios y datos locales.' -ForegroundColor Green
$headers=$null


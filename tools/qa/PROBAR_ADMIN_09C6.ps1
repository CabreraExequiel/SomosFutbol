# Somos Futbol - 09C6: regresion de permisos y moderacion (LOCAL UNICAMENTE).
# Compatible con Windows PowerShell 5.1 y PowerShell 7. Sin modulos externos.
[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [Nullable[long]]$CanchaId,
    [Nullable[long]]$DuenoId,
    [switch]$ProbarPublicacionBloqueada
)
$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
try { $destino = [Uri]$BaseUrl } catch { throw 'BaseUrl no es una URL valida.' }
if ($destino.Scheme -ne 'http' -or $destino.Host -notin @('localhost', '127.0.0.1', '::1')) {
    throw 'Por seguridad, 09C6 se ejecuta SOLO contra HTTP localhost, nunca contra Render/Aiven/produccion.'
}
$script:correctas = 0
$script:fallidas = 0
$script:omitidas = 0

function Leer-Token([string]$rol) {
    Write-Host "Token JWT ${rol}: pegalo oculto. ENTER para omitir ese rol." -ForegroundColor Cyan
    $s = Read-Host -AsSecureString
    if ($null -eq $s -or $s.Length -eq 0) { return '' }
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}

function Peticion([string]$metodo,[string]$ruta,[string]$jwt='', [string]$json='') {
    $headers = @{ Accept = 'application/json' }
    if ($jwt) { $headers['Authorization'] = 'Bearer ' + $jwt }
    $args = @{ Uri = $BaseUrl + $ruta; Method = $metodo; Headers = $headers; TimeoutSec = 15;
               ErrorAction = 'Stop'; UseBasicParsing = $true }
    if ($json) { $args['ContentType'] = 'application/json'; $args['Body'] = $json }
    try {
        $r = Invoke-WebRequest @args
        return @{ Status=[int]$r.StatusCode; Body=[string]$r.Content }
    } catch {
        if ($null -eq $_.Exception.Response) { throw "No se pudo contactar al backend local: $($_.Exception.Message)" }
        $respuesta = $_.Exception.Response
        return @{ Status=[int]$respuesta.StatusCode; Body='' }
    }
}

function Probar([string]$titulo,[string]$metodo,[string]$ruta,[int[]]$esperados,[string]$jwt='',[string]$json='') {
    try {
        $r = Peticion $metodo $ruta $jwt $json
        if ($r.Status -in $esperados) {
            Write-Host "  OK  $titulo - HTTP $($r.Status)" -ForegroundColor Green
            $script:correctas++
        } else {
            Write-Host "  FALLO $titulo - HTTP $($r.Status), esperado $($esperados -join '/')" -ForegroundColor Red
            $script:fallidas++
        }
        return $r
    } catch {
        Write-Host "  ERROR $titulo - $($_.Exception.Message)" -ForegroundColor Red
        $script:fallidas++
        return @{ Status=0; Body='' }
    }
}

function Omitir([string]$titulo) {
    Write-Host "  OMITIDA $titulo - falta JWT o identificador" -ForegroundColor DarkYellow
    $script:omitidas++
}

Write-Host "`n=== Somos Futbol Â· 09C6 Â· $BaseUrl ===" -ForegroundColor Cyan
Write-Host 'No se guardaran ni mostraran los tokens JWT. No los compartas por el chat.'
Write-Host 'Para pruebas completas usÃ¡ cuentas LOCAL de admin, jugador y dueno.'
$admin = Leer-Token 'ADMINISTRADOR'
$jugador = Leer-Token 'USUARIO (no administrador)'
$dueno = Leer-Token 'DUENO (no administrador)'

# GET: lecturas; no cambian cuentas, canchas ni reservas.
$checks = @('/api/admin/resumen', '/api/admin/usuarios', '/api/admin/canchas')
if ($null -ne $CanchaId -and $CanchaId -gt 0) {
    $checks += "/api/admin/canchas/$CanchaId"
    $checks += "/api/admin/canchas/$CanchaId/historial"
}
if ($null -ne $DuenoId -and $DuenoId -gt 0) {
    $checks += "/api/admin/complejos/$DuenoId"
    $checks += "/api/admin/complejos/$DuenoId/historial"
}
foreach ($ruta in $checks) {
    Probar "Anonimo bloqueado $ruta" 'GET' $ruta @(401) | Out-Null
    if ($jugador) { Probar "Jugador bloqueado $ruta" 'GET' $ruta @(403) $jugador | Out-Null }
    else { Omitir "Jugador bloqueado $ruta" }
    if ($dueno) { Probar "Dueno bloqueado $ruta" 'GET' $ruta @(403) $dueno | Out-Null }
    else { Omitir "Dueno bloqueado $ruta" }
    if ($admin) { Probar "Admin autorizado $ruta" 'GET' $ruta @(200) $admin | Out-Null }
    else { Omitir "Admin autorizado $ruta" }
}

if ($admin) {
    Probar 'Admin: cancha inexistente retorna 404' 'GET' '/api/admin/canchas/9223372036854775807' @(404) $admin | Out-Null
    Probar 'Admin: historial con pagina negativa retorna 400' 'GET' '/api/admin/canchas/1/historial?pagina=-1' @(400) $admin | Out-Null
}

if ($ProbarPublicacionBloqueada) {
    Write-Host "`n=== Prueba opcional de escritura (SOLO cancha de prueba) ===" -ForegroundColor Yellow
    if (-not $admin -or -not $dueno -or $null -eq $CanchaId -or $CanchaId -le 0) {
        Write-Host 'ERROR: se requieren token ADMIN, token DUENO y -CanchaId para esta prueba.' -ForegroundColor Red
        $script:fallidas++
    } else {
        $previa = Peticion 'GET' "/api/admin/canchas/$CanchaId" $admin
        $detalle = $null
        if ($previa.Status -eq 200) {
            try { $detalle = $previa.Body | ConvertFrom-Json } catch {}
        }
        if ($null -eq $detalle -or $detalle.activa -ne $false -or
            [string]::IsNullOrWhiteSpace([string]$detalle.motivoDesactivacionAdmin) -or
            $detalle.complejoSuspendido -eq $true) {
            Write-Host 'OMITIDA: la cancha debe estar INACTIVA por admin, con motivo y complejo habilitado.' -ForegroundColor DarkYellow
            $script:omitidas++
        } else {
            Write-Host 'Esta prueba intenta publicar la cancha de prueba; el backend DEBE responder HTTP 409.' -ForegroundColor Yellow
            $confirmacion = Read-Host 'Escribi PROBAR para enviar la peticion PATCH'
            if ($confirmacion -ceq 'PROBAR') {
                $r = Probar 'Dueno NO puede revocar desactivacion administrativa' 'PATCH' "/api/canchas/$CanchaId/publicacion" @(409) $dueno '{"activa":true}'
                $posterior = Peticion 'GET' "/api/admin/canchas/$CanchaId" $admin
                if ($posterior.Status -eq 200) {
                    $despues = $posterior.Body | ConvertFrom-Json
                    if ($despues.activa -eq $false -and -not [string]::IsNullOrWhiteSpace([string]$despues.motivoDesactivacionAdmin)) {
                        Write-Host '  OK  Estado y motivo administrativo conservados' -ForegroundColor Green
                        $script:correctas++
                    } else {
                        Write-Host '  FALLO Cancha cambio de estado. REVISAR/REVERTIR INMEDIATAMENTE EN PANEL ADMIN LOCAL.' -ForegroundColor Red
                        $script:fallidas++
                    }
                } else {
                    Write-Host '  FALLO No pudo verificarse estado posterior de la cancha' -ForegroundColor Red
                    $script:fallidas++
                }
            } else { Omitir 'Publicacion bloqueada' }
        }
    }
} else {
    Write-Host 'La prueba de PATCH se omite por defecto. Para ejecutarla, ver LEEME_09C6.md.' -ForegroundColor DarkYellow
}

Write-Host "`nRESULTADO: OK=$script:correctas | Fallidas=$script:fallidas | Omitidas=$script:omitidas"
if ($script:fallidas -gt 0) { exit 1 }
if ($script:omitidas -gt 0) { Write-Host 'Parcial: faltan pruebas omitidas; NO afirmar validacion completa.' -ForegroundColor Yellow }
else { Write-Host 'Todas las pruebas ejecutadas pasaron.' -ForegroundColor Green }


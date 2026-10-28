# Somos Futbol - Etapa 09D5 - pruebas HTTP de solo lectura.
# Compatible con Windows PowerShell 5.1 y PowerShell 7.
# Solo utiliza solicitudes GET contra localhost:8080.
param(
    [string]$BaseUrl = "http://localhost:8080",
    [long]$ReservaId = 0
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
$uriBase = [Uri]$BaseUrl
if ($uriBase.Scheme -ne 'http' -or $uriBase.Host -notin @('localhost','127.0.0.1','::1')) {
    throw '09D5 solo permite HTTP local (localhost o 127.0.0.1). No utilizar con produccion.'
}

$script:ok = 0
$script:fallidas = 0
$script:omitidas = 0

function Obtener-Token([string]$rol) {
    Write-Host ("Token JWT {0} (entrada oculta, ENTER para omitir):" -f $rol)
    $secure = Read-Host -AsSecureString
    if ($null -eq $secure -or $secure.Length -eq 0) { return '' }
    $ptr = [IntPtr]::Zero
    try {
        $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        $valor = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr).Trim()
        if ($valor.StartsWith('Bearer ', [StringComparison]::OrdinalIgnoreCase)) {
            $valor = $valor.Substring(7).Trim()
        }
        return $valor
    } finally {
        if ($ptr -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
        }
    }
}

function Peticion([string]$ruta, [string]$jwt = '') {
    $headers = @{'Accept'='application/json'}
    if ($jwt) { $headers['Authorization'] = 'Bearer ' + $jwt }
    try {
        $resp = Invoke-WebRequest -Uri ($BaseUrl + $ruta) -Method Get -Headers $headers -UseBasicParsing -TimeoutSec 20
        return [pscustomobject]@{ status = [int]$resp.StatusCode; body = [string]$resp.Content }
    } catch {
        $status = 0
        if ($null -ne $_.Exception.Response) {
            try { $status = [int]$_.Exception.Response.StatusCode } catch { $status = 0 }
        }
        return [pscustomobject]@{ status = $status; body = '' }
    }
}

function Resultado([string]$nombre, [bool]$correcto, [string]$detalle) {
    if ($correcto) {
        $script:ok++
        Write-Host ("  OK  {0} - {1}" -f $nombre, $detalle) -ForegroundColor Green
    } else {
        $script:fallidas++
        Write-Host ("  FALLA  {0} - {1}" -f $nombre, $detalle) -ForegroundColor Red
    }
}

function Omitir([string]$nombre) {
    $script:omitidas++
    Write-Host ("  OMITIDA  {0} - falta token o ID" -f $nombre) -ForegroundColor Yellow
}

function Probar-Codigo([string]$nombre, [string]$ruta, [string]$jwt, [int]$esperado, [bool]$requiereToken = $true) {
    if ($requiereToken -and [string]::IsNullOrWhiteSpace($jwt)) { Omitir $nombre; return $null }
    $resp = Peticion $ruta $jwt
    Resultado $nombre ($resp.status -eq $esperado) ("HTTP {0} (esperado {1})" -f $resp.status, $esperado)
    return $resp
}

function Probar-Estructura([string]$nombre, [object]$resp, [string[]]$propiedades) {
    if ($null -eq $resp -or $resp.status -ne 200) { Omitir $nombre; return }
    try {
        $data = $resp.body | ConvertFrom-Json
        $faltantes = @($propiedades | Where-Object { $_ -notin @($data.PSObject.Properties.Name) })
        Resultado $nombre ($faltantes.Count -eq 0) $(if ($faltantes.Count -eq 0) { 'JSON valido' } else { 'faltan: ' + ($faltantes -join ', ') })
    } catch {
        Resultado $nombre $false 'JSON no valido'
    }
}

Write-Host ""
Write-Host ("=== Somos Futbol - QA 09D5 - {0} ===" -f $BaseUrl)
Write-Host 'Solo GET. No cancela reservas, no cambia pagos ni modifica MySQL.'
Write-Host 'No mostrar ni enviar tokens JWT por chat.'

$admin = Obtener-Token 'ADMINISTRADOR'
$jugador = Obtener-Token 'USUARIO'
$dueno = Obtener-Token 'DUENO'

Write-Host "`n--- Permisos de endpoints ---"
$endpoints = @(
    '/api/admin/resumen',
    '/api/admin/reservas?pagina=0',
    '/api/admin/pagos?pagina=0',
    '/api/admin/conciliacion/resumen',
    '/api/admin/conciliacion/casos?tipo=CONFIRMADA_SIN_PAGO_APROBADO&pagina=0'
)
foreach ($ruta in $endpoints) {
    [void](Probar-Codigo "Anonimo $ruta" $ruta '' 401 $false)
    [void](Probar-Codigo "Jugador $ruta" $ruta $jugador 403)
    [void](Probar-Codigo "Dueno $ruta" $ruta $dueno 403)
    [void](Probar-Codigo "Admin $ruta" $ruta $admin 200)
}

Write-Host "`n--- Esquemas y consultas sin escritura ---"
$reservas = Probar-Codigo 'Listado de reservas' '/api/admin/reservas?pagina=0' $admin 200
Probar-Estructura 'Campos del listado de reservas' $reservas @('reservas','pagina','totalPaginas','total')
$pagos = Probar-Codigo 'Listado de pagos' '/api/admin/pagos?pagina=0' $admin 200
Probar-Estructura 'Campos del listado de pagos' $pagos @('pagos','pagina','totalPaginas','total')
$resumen = Probar-Codigo 'Resumen de conciliacion' '/api/admin/conciliacion/resumen' $admin 200
Probar-Estructura 'Campos del resumen de conciliacion' $resumen @('confirmadasSinPagoAprobado','pagosAprobadosSinReservaConfirmada','pendientesVencidasSinPagoAprobado')

foreach ($estado in @('PENDIENTE','CONFIRMADA','CANCELADA')) {
    [void](Probar-Codigo ("Reserva filtro $estado") ("/api/admin/reservas?estado=$estado&pagina=0") $admin 200)
}
foreach ($estado in @('PENDIENTE','APROBADO','RECHAZADO','REEMBOLSADO')) {
    [void](Probar-Codigo ("Pago filtro $estado") ("/api/admin/pagos?estado=$estado&pagina=0") $admin 200)
}
foreach ($tipo in @('CONFIRMADA_SIN_PAGO_APROBADO','PAGO_APROBADO_RESERVA_NO_CONFIRMADA','PENDIENTE_VENCIDA_SIN_PAGO_APROBADO')) {
    $casos = Probar-Codigo ("Conciliacion $tipo") ("/api/admin/conciliacion/casos?tipo=$tipo&pagina=0") $admin 200
    Probar-Estructura ("Campos conciliacion $tipo") $casos @('tipo','casos','pagina','totalPaginas','total')
}

[void](Probar-Codigo 'Tipo de conciliacion invalido' '/api/admin/conciliacion/casos?tipo=NO_EXISTE&pagina=0' $admin 400)
[void](Probar-Codigo 'Reserva inexistente' '/api/admin/reservas/999999999' $admin 404)
if ($ReservaId -gt 0) {
    [void](Probar-Codigo ("Detalle de reserva $ReservaId") ("/api/admin/reservas/$ReservaId") $admin 200)
} else {
    Omitir 'Detalle de una reserva existente'
}

Write-Host ""
Write-Host ("RESULTADO: OK={0} | Fallidas={1} | Omitidas={2}" -f $script:ok, $script:fallidas, $script:omitidas)
if ($script:fallidas -gt 0) { Write-Host 'Hay pruebas fallidas: NO cerrar 09D5.' -ForegroundColor Red; exit 1 }
if ($script:omitidas -gt 0) { Write-Host 'Validacion parcial: faltan pruebas omitidas.' -ForegroundColor Yellow; exit 2 }
Write-Host 'Pruebas HTTP de solo lectura superadas.' -ForegroundColor Green
exit 0

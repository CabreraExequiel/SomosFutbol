# Somos Futbol - Etapa 09E5. Prueba de permisos de API LOCAL.
# No escribe datos reales: el PATCH utiliza un ID inexistente y solo valida 401/403/404.
# Compatible con Windows PowerShell 5.1. NO imprime ni guarda tokens.
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [switch]$ConTokens
)

$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
try { $destino = [Uri]$BaseUrl }
catch { throw 'La URL de API local no es valida.' }
if ($destino.Scheme -ne 'http' -or $destino.Host -notin @('localhost','127.0.0.1','::1')) {
    throw 'Por seguridad, esta prueba solo puede ejecutarse contra http://localhost o 127.0.0.1.'
}
$fallos = 0
$aprobadas = 0

function LeerTokenSeguro([string]$etiqueta) {
    $seguro = Read-Host "$etiqueta (JWT sin 'Bearer'; no compartas el token)" -AsSecureString
    $puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero) }
}

function ObtenerCodigo([string]$metodo, [string]$ruta, [string]$token = '') {
    $parametros = @{
        Uri = "$BaseUrl$ruta"
        Method = $metodo
        UseBasicParsing = $true
        TimeoutSec = 20
        ErrorAction = 'Stop'
    }
    if (-not [string]::IsNullOrWhiteSpace($token)) {
        $parametros.Headers = @{ Authorization = "Bearer $token" }
    }
    if ($metodo -eq 'PATCH') {
        $parametros.ContentType = 'application/json'
        $parametros.Body = '{"estado":"RESUELTO"}'
    }
    try {
        $respuesta = Invoke-WebRequest @parametros
        return [int]$respuesta.StatusCode
    }
    catch {
        if ($null -ne $_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw "No fue posible conectar con $BaseUrl. Verifica que Spring Boot este encendido."
    }
}

function Comprobar([string]$titulo, [string]$metodo, [string]$ruta, [int]$esperado, [string]$token = '') {
    $codigo = ObtenerCodigo $metodo $ruta $token
    if ($codigo -eq $esperado) {
        Write-Host "[OK] $titulo (HTTP $codigo)" -ForegroundColor Green
        $script:aprobadas++
    }
    else {
        Write-Host "[FALLÓ] $titulo (esperado $esperado; recibido $codigo)" -ForegroundColor Red
        $script:fallos++
    }
}

Write-Host "`n09E5 - Verificacion local de acceso a la bitacora" -ForegroundColor Cyan
Write-Host "API: $BaseUrl"
$inexistente = '9223372036854775807'

# Sin JWT: no se debe consultar ni modificar la bitacora.
Comprobar 'Anonimo: listado' 'GET' '/api/admin/bitacora' 401
Comprobar 'Anonimo: estadisticas' 'GET' '/api/admin/bitacora/estadisticas?dias=7' 401
Comprobar 'Anonimo: acciones' 'GET' '/api/admin/bitacora/acciones' 401
Comprobar 'Anonimo: incidentes' 'GET' '/api/admin/bitacora/incidentes' 401
Comprobar 'Anonimo: historial' 'GET' "/api/admin/bitacora/$inexistente/historial" 401
Comprobar 'Anonimo: cambiar estado' 'PATCH' "/api/admin/bitacora/$inexistente/estado" 401

if ($ConTokens) {
    Write-Host "`nLos tokens se ingresan de forma oculta y no se muestran en el resultado." -ForegroundColor Yellow
    $usuario = LeerTokenSeguro 'Token de un JUGADOR o DUEÑO de prueba'
    $admin = LeerTokenSeguro 'Token de un ADMINISTRADOR de prueba'
    if ([string]::IsNullOrWhiteSpace($usuario) -or [string]::IsNullOrWhiteSpace($admin)) {
        throw 'Se necesitan ambos JWT para ejecutar la prueba de roles. No se realizaron mutaciones.'
    }
    # Sin permisos administrativos: ni lectura ni escritura.
    Comprobar 'Jugador: estadisticas' 'GET' '/api/admin/bitacora/estadisticas?dias=7' 403 $usuario
    Comprobar 'Jugador: incidentes' 'GET' '/api/admin/bitacora/incidentes' 403 $usuario
    Comprobar 'Jugador: acciones' 'GET' '/api/admin/bitacora/acciones' 403 $usuario
    Comprobar 'Jugador: cambiar estado' 'PATCH' "/api/admin/bitacora/$inexistente/estado" 403 $usuario

    # Administrador: lectura válida y validaciones con errores esperados.
    Comprobar 'Admin: listado' 'GET' '/api/admin/bitacora' 200 $admin
    Comprobar 'Admin: estadisticas' 'GET' '/api/admin/bitacora/estadisticas?dias=7' 200 $admin
    Comprobar 'Admin: acciones' 'GET' '/api/admin/bitacora/acciones' 200 $admin
    Comprobar 'Admin: incidentes' 'GET' '/api/admin/bitacora/incidentes' 200 $admin
    Comprobar 'Admin: periodo invalido' 'GET' '/api/admin/bitacora/estadisticas?dias=8' 400 $admin
    Comprobar 'Admin: filtro incompatible' 'GET' '/api/admin/bitacora/incidentes?sinGestion=true&ultimoAdminId=1' 400 $admin
    Comprobar 'Admin: cambiar estado de ID inexistente' 'PATCH' "/api/admin/bitacora/$inexistente/estado" 404 $admin
    $usuario = $null
    $admin = $null
}
else {
    Write-Host '`nPara probar tambien 403 (jugador) y 200/400/404 (admin), ejecuta con -ConTokens.' -ForegroundColor Yellow
}

Write-Host "`nResultado: $aprobadas comprobaciones correctas, $fallos fallidas."
if ($fallos -gt 0) { exit 1 }
exit 0

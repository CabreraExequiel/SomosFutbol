# Consulta publica anónima de anuncios. No requiere credenciales.
param([string]$Api = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'
Write-Host 'Etapa 10C - Verificacion PUBLICA en LOCAL' -ForegroundColor Cyan
$ubicaciones = @('INICIO','FEED','BUSCAR_CANCHAS')
foreach ($ubicacion in $ubicaciones) {
    $uri = "$Api/api/publicidades/visibles?ubicacion=$ubicacion"
    try {
        $respuesta = Invoke-RestMethod -Uri $uri -Method GET -ErrorAction Stop
        $items = @($respuesta)
        if ($null -eq $respuesta) { $items = @() }
        if ($items.Count -gt 2) { throw "Se esperaban maximo 2 anuncios para $ubicacion" }
        foreach ($item in $items) {
            if ($item.PSObject.Properties.Name -contains 'creadoPorId') { throw 'Se filtra informacion del administrador' }
            if ($item.PSObject.Properties.Name -contains 'estado') { throw 'No se deben exponer campos administrativos' }
            if ($item.ubicacion -ne $ubicacion) { throw "Anuncio fuera de ubicacion: $ubicacion" }
        }
        Write-Host "[OK] $ubicacion`: HTTP 200, $($items.Count) anuncios visibles" -ForegroundColor Green
    } catch {
        Write-Host "[ERROR] $ubicacion`: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}
try {
    Invoke-WebRequest -Uri "$Api/api/publicidades/visibles?ubicacion=NO_EXISTE" -Method GET -UseBasicParsing -ErrorAction Stop | Out-Null
    Write-Host '[ERROR] Debio rechazar ubicacion invalida' -ForegroundColor Red
    exit 1
} catch [System.Net.WebException] {
    $codigo = [int]$_.Exception.Response.StatusCode
    if ($codigo -ne 400) { Write-Host "[ERROR] Ubicacion invalida: HTTP $codigo" -ForegroundColor Red; exit 1 }
    Write-Host '[OK] Ubicacion invalida: HTTP 400' -ForegroundColor Green
}
Write-Host 'Pruebas publicas 10C correctas.' -ForegroundColor Green

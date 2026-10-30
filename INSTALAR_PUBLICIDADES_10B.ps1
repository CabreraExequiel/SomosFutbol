# Somos Futbol - Etapa 10B. Modifica SOLO el dashboard actual sin reemplazarlo.
# Ejecutar en la raiz del repositorio despues de Expand-Archive.
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $MyInvocation.MyCommand.Path
$ts = Join-Path $raiz 'frontend\src\app\componentes\admin-dashboard\admin-dashboard.component.ts'
$html = Join-Path $raiz 'frontend\src\app\componentes\admin-dashboard\admin-dashboard.component.html'
$componente = Join-Path $raiz 'frontend\src\app\componentes\admin-publicidades\admin-publicidades.component.ts'
$servicio = Join-Path $raiz 'frontend\src\app\services\publicidades-admin.service.ts'
$utf8 = New-Object System.Text.UTF8Encoding($false)

foreach ($ruta in @($ts, $html, $componente, $servicio)) {
    if (-not [IO.File]::Exists($ruta)) {
        throw "No se encontro el archivo necesario: $ruta. Extrae primero el ZIP completo en la raiz del proyecto."
    }
}

$textoTs = [IO.File]::ReadAllText($ts, [Text.Encoding]::UTF8)
$textoHtml = [IO.File]::ReadAllText($html, [Text.Encoding]::UTF8)

# Analizar y validar ANTES de escribir algun archivo.
if (-not $textoTs.Contains("import { AdminPublicidadesComponent }")) {
    $importBase = "import { RouterLink } from '@angular/router';"
    if (-not $textoTs.Contains($importBase)) { throw 'No se encontro la importacion RouterLink. No se modifico el dashboard.' }
    $textoTs = $textoTs.Replace($importBase, $importBase + "`nimport { AdminPublicidadesComponent } from '../admin-publicidades/admin-publicidades.component';")
}
if (-not $textoTs.Contains('CommonModule, RouterLink, AdminPublicidadesComponent')) {
    $importsBase = 'imports: [CommonModule, RouterLink]'
    if (-not $textoTs.Contains($importsBase)) { throw 'No se encontro el arreglo imports esperado. No se modifico el dashboard.' }
    $textoTs = $textoTs.Replace($importsBase, 'imports: [CommonModule, RouterLink, AdminPublicidadesComponent]')
}
if (-not $textoHtml.Contains('<app-admin-publicidades')) {
    $ancla = '<section class="surface-card sf-admin-section sf-admin-next">'
    if (-not $textoHtml.Contains($ancla)) { throw 'No se encontro la seccion de bitacora esperada. No se modifico el dashboard.' }
    $textoHtml = $textoHtml.Replace($ancla, "<app-admin-publicidades />`n`n    " + $ancla)
}

foreach ($ruta in @($ts, $html)) {
    $respaldo = "$ruta.pre10b.bak"
    if (-not [IO.File]::Exists($respaldo)) { [IO.File]::Copy($ruta, $respaldo) }
}
[IO.File]::WriteAllText($ts, $textoTs, $utf8)
[IO.File]::WriteAllText($html, $textoHtml, $utf8)
Write-Host '[OK] Etapa 10B instalada sobre el dashboard actual.' -ForegroundColor Green
Write-Host '[OK] Respaldos: admin-dashboard.component.ts.pre10b.bak y .html.pre10b.bak.' -ForegroundColor Green
Write-Host 'Siguiente paso: cd frontend; npm run build; npm start' -ForegroundColor Cyan

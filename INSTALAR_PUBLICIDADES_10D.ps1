# Etapa 10D - Integra metrica publica + panel sin reemplazar dashboard ni componentes previos.
# Ejecutar desde la raiz del monorepo, despues de Expand-Archive.
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $MyInvocation.MyCommand.Path
$fuente = Join-Path $raiz 'etapa10d_archivos'
$utf8 = New-Object System.Text.UTF8Encoding($false)

$seguridad = Join-Path $raiz 'backend\src\main\java\com\canchasya\api\config\SecurityConfig.java'
$adminTs = Join-Path $raiz 'frontend\src\app\componentes\admin-publicidades\admin-publicidades.component.ts'
$adminHtml = Join-Path $raiz 'frontend\src\app\componentes\admin-publicidades\admin-publicidades.component.html'
$visiblesTs = Join-Path $raiz 'frontend\src\app\componentes\publicidades-visibles\publicidades-visibles.component.ts'
$visiblesHtml = Join-Path $raiz 'frontend\src\app\componentes\publicidades-visibles\publicidades-visibles.component.html'

foreach ($ruta in @($seguridad, $adminTs, $adminHtml, $visiblesTs, $visiblesHtml)) {
    if (-not [IO.File]::Exists($ruta)) { throw "Falta el archivo $ruta. Verifica la instalacion 10A-10C." }
}
if (-not (Test-Path (Join-Path $fuente 'backend\src\main\resources\db\migration\V19__metricas_publicidades_10d.sql'))) {
    throw 'No se encontro el payload etapa10d_archivos. Descomprimi el ZIP en la raiz del repositorio.'
}

$sec = [IO.File]::ReadAllText($seguridad,[Text.Encoding]::UTF8)
$ts = [IO.File]::ReadAllText($adminTs,[Text.Encoding]::UTF8)
$html = [IO.File]::ReadAllText($adminHtml,[Text.Encoding]::UTF8)
if (-not $sec.Contains('"/api/publicidades/visibles"')) {
    throw 'La ruta publica 10C no esta presente en SecurityConfig. No se modifico ningun archivo.'
}
if (-not $sec.Contains('"/api/publicidades/*/impresiones"')) {
    $ancla = '.requestMatchers(org.springframework.http.HttpMethod.GET,'
    if (-not $sec.Contains($ancla)) { throw 'No se encontro el anclaje de GET publico. No se modifico ningun archivo.' }
    $permiso = '.requestMatchers(org.springframework.http.HttpMethod.POST,' + "`n" +
        '                                "/api/publicidades/*/impresiones",' + "`n" +
        '                                "/api/publicidades/*/clics"' + "`n" +
        '                        ).permitAll()' + "`n                        "
    $sec = $sec.Replace($ancla, $permiso + $ancla)
}
if (-not $ts.Contains('PublicidadesEstadisticasComponent')) {
    $ancla = "import { FormsModule } from '@angular/forms';"
    if (-not $ts.Contains($ancla)) { throw 'No se encontro FormsModule en admin-publicidades.ts.' }
    $ts = $ts.Replace($ancla, $ancla + "`nimport { PublicidadesEstadisticasComponent } from '../publicidades-estadisticas/publicidades-estadisticas.component';")
}
if (-not $ts.Contains('CommonModule, FormsModule, PublicidadesEstadisticasComponent')) {
    $ancla = 'imports: [CommonModule, FormsModule]'
    if (-not $ts.Contains($ancla)) { throw 'No se encontraron las importaciones del componente 10B.' }
    $ts = $ts.Replace($ancla, 'imports: [CommonModule, FormsModule, PublicidadesEstadisticasComponent]')
}
if (-not $html.Contains('<app-publicidades-estadisticas')) {
    $ancla = '<form class="sf-ads-filtros"'
    if (-not $html.Contains($ancla)) { throw 'No se encontro el formulario de filtros del panel 10B.' }
    $html = $html.Replace($ancla, "<app-publicidades-estadisticas />`n`n  " + $ancla)
}
$viejo = 'Los anuncios <strong>todavía no se muestran a los jugadores</strong>: su publicación en el feed y otras ubicaciones se habilitará en la etapa 10C.'
$nuevo = 'Los anuncios <strong>ACTIVOS</strong> se muestran a los jugadores cuando corresponden a la ubicación y están dentro de sus fechas de publicación.'
$html = $html.Replace($viejo,$nuevo)

# Todas las comprobaciones anteriores se realizan ANTES de alterar el proyecto.
foreach ($ruta in @($seguridad,$adminTs,$adminHtml,$visiblesTs,$visiblesHtml)) {
    $backup = "$ruta.pre10d.bak"
    if (-not [IO.File]::Exists($backup)) { [IO.File]::Copy($ruta,$backup) }
}
$archivos = Get-ChildItem -LiteralPath $fuente -File -Recurse
foreach ($archivo in $archivos) {
    $rel = $archivo.FullName.Substring($fuente.Length).TrimStart([char]92,[char]47)
    $destino = Join-Path $raiz $rel
    $carpeta = Split-Path -Parent $destino
    if (-not (Test-Path $carpeta)) { New-Item -ItemType Directory -Path $carpeta -Force | Out-Null }
    [IO.File]::Copy($archivo.FullName, $destino, $true)
}
[IO.File]::WriteAllText($seguridad,$sec,$utf8)
[IO.File]::WriteAllText($adminTs,$ts,$utf8)
[IO.File]::WriteAllText($adminHtml,$html,$utf8)
Write-Host '[OK] Etapa 10D instalada: endpoints, metricas, Angular y panel estadistico.' -ForegroundColor Green
Write-Host '[OK] Respaldos .pre10d.bak creados antes de modificar archivos existentes.' -ForegroundColor Green
Write-Host 'IMPORTANTE: Ejecutar UNA vez V19__metricas_publicidades_10d.sql en MySQL de XAMPP.' -ForegroundColor Yellow
Write-Host 'Luego reiniciar Spring Boot, mvn verify, npm run build y PROBAR_PUBLICIDADES_10D.ps1.' -ForegroundColor Cyan

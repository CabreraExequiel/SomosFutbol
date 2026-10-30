# Somos Futbol - ETAPA 10C. Integracion aditiva y reversible.
# Ejecutar desde la raiz del monorepo, despues de Expand-Archive.
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $MyInvocation.MyCommand.Path
$seguridad = Join-Path $raiz 'backend\src\main\java\com\canchasya\api\config\SecurityConfig.java'
$feedTs = Join-Path $raiz 'frontend\src\app\componentes\feed\feed.component.ts'
$feedHtml = Join-Path $raiz 'frontend\src\app\componentes\feed\feed.component.html'
$canchasTs = Join-Path $raiz 'frontend\src\app\componentes\canchas\canchas.component.ts'
$canchasHtml = Join-Path $raiz 'frontend\src\app\componentes\canchas\canchas.component.html'
$servicio = Join-Path $raiz 'frontend\src\app\services\publicidades-publicas.service.ts'
$componente = Join-Path $raiz 'frontend\src\app\componentes\publicidades-visibles\publicidades-visibles.component.ts'
$controlador = Join-Path $raiz 'backend\src\main\java\com\canchasya\api\controller\PublicidadPublicaController.java'
$codificacion = New-Object System.Text.UTF8Encoding($false)

foreach ($archivo in @($seguridad,$feedTs,$feedHtml,$canchasTs,$canchasHtml,$servicio,$componente,$controlador)) {
    if (-not [IO.File]::Exists($archivo)) {
        throw "Falta el archivo $archivo. Descomprimi primero el ZIP completo en la raiz del proyecto."
    }
}

# Preparar cambios en memoria: si falta algun anclaje, no se escribe nada.
$cambios = @{}
$seg = [IO.File]::ReadAllText($seguridad,[Text.Encoding]::UTF8)
if (-not $seg.Contains('"/api/publicidades/visibles"')) {
    $ancla = '"/api/config",'
    if (-not $seg.Contains($ancla)) { throw 'No se encontro el anclaje GET en SecurityConfig. No se modifico ningun archivo.' }
    $seg = $seg.Replace($ancla, "$ancla`n                                `"/api/publicidades/visibles`",")
}
$cambios[$seguridad] = $seg

function Agregar-Componente($ruta, $rutaImport) {
    $texto = [IO.File]::ReadAllText($ruta,[Text.Encoding]::UTF8)
    $import = "import { PublicidadesVisiblesComponent } from '$rutaImport';"
    if (-not $texto.Contains($import)) {
        $ancla = "import { CommonModule } from '@angular/common';"
        if (-not $texto.Contains($ancla)) { throw "No se encontro CommonModule en $ruta. No se modifico ningun archivo." }
        $texto = $texto.Replace($ancla, "$ancla`n$import")
    }
    if ($texto -notmatch '(?s)imports\s*:\s*\[[^\]]*\bPublicidadesVisiblesComponent\b') {
        $patron = '(?s)(imports\s*:\s*\[)([^\]]*)(\])'
        if (-not [regex]::IsMatch($texto, $patron)) { throw "No se encontro imports en $ruta. No se modifico ningun archivo." }
        $texto = [regex]::Replace($texto, $patron, '${1}PublicidadesVisiblesComponent, ${2}${3}')
    }
    return $texto
}

$cambios[$feedTs] = Agregar-Componente $feedTs '../publicidades-visibles/publicidades-visibles.component'
$cambios[$canchasTs] = Agregar-Componente $canchasTs '../publicidades-visibles/publicidades-visibles.component'

$feed = [IO.File]::ReadAllText($feedHtml,[Text.Encoding]::UTF8)
if (-not $feed.Contains('ubicacion="INICIO"')) {
    $ancla = '<div class="feed-columna">'
    if (-not $feed.Contains($ancla)) { throw 'No se encontro el contenedor del feed. No se modifico ningun archivo.' }
    $feed = $feed.Replace($ancla, "$ancla`n    <app-publicidades-visibles ubicacion=`"INICIO`" />")
}
if (-not $feed.Contains('ubicacion="FEED"')) {
    $ancla = '<nav class="feed-filtros"'
    if (-not $feed.Contains($ancla)) { throw 'No se encontraron los filtros del feed. No se modifico ningun archivo.' }
    $feed = $feed.Replace($ancla, "<app-publicidades-visibles ubicacion=`"FEED`" />`n  $ancla")
}
$cambios[$feedHtml] = $feed

$canchas = [IO.File]::ReadAllText($canchasHtml,[Text.Encoding]::UTF8)
if (-not $canchas.Contains('ubicacion="BUSCAR_CANCHAS"')) {
    $ancla = '<section class="surface-card filtros-panel"'
    if (-not $canchas.Contains($ancla)) { throw 'No se encontro el panel de filtros de canchas. No se modifico ningun archivo.' }
    $canchas = $canchas.Replace($ancla, "<app-publicidades-visibles ubicacion=`"BUSCAR_CANCHAS`" />`n $ancla")
}
$cambios[$canchasHtml] = $canchas

# Todo validado: crear respaldos solo de archivos preexistentes que se van a modificar.
foreach ($ruta in @($seguridad,$feedTs,$feedHtml,$canchasTs,$canchasHtml)) {
    $respaldo = "$ruta.pre10c.bak"
    if (-not [IO.File]::Exists($respaldo)) { [IO.File]::Copy($ruta,$respaldo) }
    [IO.File]::WriteAllText($ruta,[string]$cambios[$ruta],$codificacion)
}
Write-Host '[OK] 10C instalada en SecurityConfig, inicio/feed y busqueda de canchas.' -ForegroundColor Green
Write-Host '[OK] Se crearon respaldos .pre10c.bak de los cinco archivos modificados.' -ForegroundColor Green
Write-Host 'Siguiente: reiniciar Spring Boot, ejecutar mvn verify, npm run build y probar anuncios ACTIVOS.' -ForegroundColor Cyan

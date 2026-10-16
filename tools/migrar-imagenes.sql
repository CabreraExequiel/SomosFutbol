-- Ejecutar primero en una copia de la base, después de copiar uploads/ al volumen.
-- Conserva el archivo local de fotos antiguas cuyo dominio era localhost o ngrok.
UPDATE users SET avatar=SUBSTRING(avatar,LOCATE('/api/media/',avatar)) WHERE avatar LIKE '%/api/media/%';
UPDATE canchas SET imagen=SUBSTRING(imagen,LOCATE('/api/media/',imagen)) WHERE imagen LIKE '%/api/media/%';

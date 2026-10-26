/**
 * Borradores del editor de publicaciones, únicamente en sessionStorage.
 * No se almacenan videos, menciones ni credenciales.
 */
export interface BorradorFeed {
  contenido: string;
  youtubeUrl: string;
}

type AlmacenamientoBorrador = Pick<Storage, 'getItem' | 'setItem' | 'removeItem'>;

export function claveBorradorFeed(usuarioId: unknown): string | null {
  if (usuarioId === null || usuarioId === undefined || usuarioId === '') return null;
  const id = Number(usuarioId);
  return Number.isSafeInteger(id) && id > 0
    ? `somosFutbolBorradorFeedV1:${id}`
    : null;
}

export function leerBorradorFeed(
  storage: AlmacenamientoBorrador, clave: string
): BorradorFeed | null {
  try {
    const valor = storage.getItem(clave);
    if (!valor || valor.length > 10000) return null;
    const parsed: unknown = JSON.parse(valor);
    if (!parsed || typeof parsed !== 'object') return null;
    const data = parsed as Record<string, unknown>;
    if (data['v'] !== 1 || typeof data['contenido'] !== 'string' ||
        typeof data['youtubeUrl'] !== 'string') return null;
    const contenido = data['contenido'].slice(0, 2000);
    const youtubeUrl = data['youtubeUrl'].slice(0, 500);
    return contenido.trim() || youtubeUrl.trim()
      ? { contenido, youtubeUrl }
      : null;
  } catch {
    return null;
  }
}

export function guardarBorradorFeed(
  storage: AlmacenamientoBorrador, clave: string, borrador: BorradorFeed
): boolean {
  try {
    if (!borrador.contenido.trim() && !borrador.youtubeUrl.trim()) {
      storage.removeItem(clave);
      return false;
    }
    storage.setItem(clave, JSON.stringify({
      v: 1,
      contenido: borrador.contenido.slice(0, 2000),
      youtubeUrl: borrador.youtubeUrl.slice(0, 500)
    }));
    return true;
  } catch {
    return false; // navegadores que bloquean almacenamiento
  }
}

export function eliminarBorradorFeed(
  storage: AlmacenamientoBorrador, clave: string
): void {
  try { storage.removeItem(clave); } catch { /* storage deshabilitado */ }
}

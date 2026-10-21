/** Datos públicos de una cancha, devueltos por GET /api/canchas. */
export interface CanchaPublica {
  id: number;
  duenoId: number;
  duenoNombre: string;
  nombreComplejo: string;
  ciudadId: number;
  ciudadNombre: string;
  nombre: string;
  direccion: string | null;
  tipo: string | null;
  superficie: string | null;
  cantJugadores: number | null;
  precioHora: number;
  techada: boolean;
  imagen: string | null;
  descripcion: string | null;
  activa: boolean;
}

export interface Franja { horaInicio: string; horaFin: string; }
export interface Horario extends Franja { id?: number; diaSemana: number; }
export interface Bloqueo { id: number; fecha: string; horaInicio: string | null; horaFin: string | null; motivo: string | null; }
export interface Disponibilidad { canchaId: number; fecha: string; zonaHoraria: string; publicados: Franja[]; libres: Franja[]; ocupados: Franja[]; }
export interface DuenoPublico { id: number; nombre: string; avatar: string | null; imagen: string | null; cantidadCanchas: number; precioDesde: number; ciudades: string[]; tipos: string[]; }
export function fechaArgentina(): string {
  const partes=new Intl.DateTimeFormat('en-CA',{timeZone:'America/Argentina/Buenos_Aires',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(new Date());
  const valor=(tipo:string)=>partes.find(p=>p.type===tipo)?.value;
  return `${valor('year')}-${valor('month')}-${valor('day')}`;
}
export const DIAS = [{id:1,nombre:'Lunes'},{id:2,nombre:'Martes'},{id:3,nombre:'Miércoles'},{id:4,nombre:'Jueves'},{id:5,nombre:'Viernes'},{id:6,nombre:'Sábado'},{id:0,nombre:'Domingo'}];

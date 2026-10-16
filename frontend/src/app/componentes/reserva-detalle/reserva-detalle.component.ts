import { AfterViewInit, Component, Input, Output, EventEmitter, ElementRef, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReservaService } from '../../services/reserva.service';
@Component({selector:'app-reserva-detalle',imports:[CommonModule],templateUrl:'./reserva-detalle.component.html',styleUrl:'./reserva-detalle.component.css',changeDetection:ChangeDetectionStrategy.Eager})
export class ReservaDetalleComponent implements AfterViewInit {
 @Input({required:true}) reservaId!:number; @Output() cerrar=new EventEmitter<void>();
 @ViewChild('dialogo') dialogo!:ElementRef<HTMLDialogElement>;
 detalle:any=null;error='';cargando=true;
 constructor(private service:ReservaService){}
 ngAfterViewInit():void {this.dialogo.nativeElement.showModal();this.cargar();}
 cargar():void {this.cargando=true;this.error='';this.service.detalle(this.reservaId).subscribe({next:d=>{this.detalle=d;this.cargando=false;},error:e=>{this.error=e.error?.message||'No pudimos cargar el detalle.';this.cargando=false;}});}
 salir():void {this.dialogo.nativeElement.close();}
 telefono():string {return (this.detalle?.usuario?.telefono||'').replace(/[^0-9+]/g,'');}
}

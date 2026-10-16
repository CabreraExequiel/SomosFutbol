import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { CanchaService } from '../../services/cancha.service';
import { DuenoPublico } from '../../models/agenda';
@Component({selector:'app-canchas',imports:[CommonModule,FormsModule,RouterLink],templateUrl:'./canchas.component.html',styleUrl:'./canchas.component.css',changeDetection:ChangeDetectionStrategy.Eager})
export class CanchasComponent implements OnInit,OnDestroy {
 duenos:DuenoPublico[]=[];ciudades:any[]=[];ciudadSeleccionada='';tipoSeleccionado='';texto='';orden='nombre';cargando=true;error='';private peticion?:Subscription;
 constructor(private service:CanchaService){}
 ngOnInit():void{this.service.listarCiudades().subscribe({next:r=>this.ciudades=r,error:()=>this.error='No se pudieron cargar las ciudades. Podés explorar todos los complejos.'});this.buscar();}
 ngOnDestroy():void{this.peticion?.unsubscribe();}
 buscar():void{this.peticion?.unsubscribe();this.cargando=true;this.error='';this.peticion=this.service.duenos({ciudadId:this.ciudadSeleccionada?Number(this.ciudadSeleccionada):undefined,tipo:this.tipoSeleccionado||undefined}).subscribe({next:r=>{this.duenos=r;this.cargando=false;},error:()=>{this.error='No pudimos cargar los complejos. Revisá tu conexión.';this.cargando=false;}});}
 get visibles():DuenoPublico[]{const q=this.texto.trim().toLocaleLowerCase();return this.duenos.filter(d=>(d.nombre+' '+d.ciudades.join(' ')).toLocaleLowerCase().includes(q)).sort((a,b)=>this.orden==='precio'?a.precioDesde-b.precioDesde:a.nombre.localeCompare(b.nombre));}
 limpiarFiltros():void{this.ciudadSeleccionada='';this.tipoSeleccionado='';this.texto='';this.buscar();}
 ocultarImagen(e:Event):void{(e.target as HTMLImageElement).style.display='none';}
}

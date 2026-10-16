import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, switchMap } from 'rxjs';
import { CanchaService } from '../../services/cancha.service';
@Component({selector:'app-dueno-publico',imports:[CommonModule,FormsModule,RouterLink],templateUrl:'./dueno-publico.component.html',styleUrls:['../canchas/canchas.component.css','./dueno-publico.component.css'],changeDetection:ChangeDetectionStrategy.Eager})
export class DuenoPublicoComponent implements OnInit,OnDestroy {
 canchas:any[]=[];tipo='';cargando=true;error='';private sub?:Subscription;
 constructor(private route:ActivatedRoute,private service:CanchaService){}
 ngOnInit():void{this.sub=this.route.paramMap.pipe(switchMap(p=>{this.cargando=true;this.error='';return this.service.porDueno(Number(p.get('id')));})).subscribe({next:r=>{this.canchas=r;this.cargando=false;},error:()=>{this.error='No pudimos cargar las canchas de este dueño.';this.cargando=false;}});}
 ngOnDestroy():void{this.sub?.unsubscribe();}
 get visibles():any[]{return this.canchas.filter(c=>!this.tipo||c.tipo===this.tipo);}
 ocultarImagen(e:Event):void{(e.target as HTMLImageElement).style.display='none';}
}

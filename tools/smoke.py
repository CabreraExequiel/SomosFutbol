#!/usr/bin/env python3
"""Prueba funcional: usar EXCLUSIVAMENTE una instalación y base descartables.
SF_TEST_API=http://127.0.0.1:18080/api python tools/smoke.py
Crea datos de prueba y los conserva para inspección; no usar en producción.
"""
import urllib.request,urllib.error,urllib.parse,json,os,uuid,datetime,concurrent.futures
from pathlib import Path
from zoneinfo import ZoneInfo
BASE=os.environ.get('SF_TEST_API','http://127.0.0.1:18080/api').rstrip('/')
if urllib.parse.urlparse(BASE).hostname not in ('localhost','127.0.0.1'):
 raise SystemExit('Por seguridad esta prueba solo admite un servidor local y descartable.')
opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
checks=[]
def request(method,path,data=None,token=None,expected=200):
 headers={'Content-Type':'application/json'}
 if token:headers['Authorization']='Bearer '+token
 req=urllib.request.Request(BASE+path,data=None if data is None else json.dumps(data).encode(),method=method,headers=headers)
 try:
  with opener.open(req,timeout=30) as res:status=res.status;body=res.read()
 except urllib.error.HTTPError as e:status=e.code;body=e.read()
 result=json.loads(body) if body else None
 if expected is not None:assert status==expected,(method,path,status,result)
 return (status,result) if expected is None else result

def passed(name):checks.append(name);print('OK',name,flush=True)
tag=uuid.uuid4().hex[:10];password='Prueba-SF-'+uuid.uuid4().hex
accounts={}
for name,role in [('dueno','dueno'),('jugador','usuario'),('otro','dueno')]:
 payload={'name':{'dueno':'Complejo La Gambeta','jugador':'Jugador de Prueba','otro':'Complejo El Encuentro'}[name], 'email':name+'-'+tag+'@example.test','password':password,'telefono':'+54 9 297 400 0000','ciudad':'Comodoro Rivadavia','role':role,'aceptaTerminos':True,'aceptaPrivacidad':True}
 if role=='dueno':payload['aliasMercadoPago']='prueba.'+tag
 accounts[name]=request('POST','/auth/register',payload)
 accounts[name]['credentials']={'email':payload['email'],'password':password}
u=accounts['jugador']['token'];d=accounts['dueno']['token'];o=accounts['otro']['token']
passed('Registro de jugadores y dueños con consentimiento')
request('GET','/reservas/recibidas',expected=401)
request('GET','/users',token=u,expected=403)
request('POST','/auth/register',{'name':'Intruso','email':'admin-'+tag+'@example.test','password':password,'telefono':'12345678','role':'administrador','aceptaTerminos':True,'aceptaPrivacidad':True},expected=403)
passed('Protección de sesiones y del rol administrador')
# Configurar cuentas de transferencia para los dos duenos de prueba.
# Los datos se guardan mediante el endpoint correspondiente,
# no durante el registro de usuarios.
request('PUT','/users/me/cuenta-cobro',{'aliasMercadoPago':'prueba.'+tag},d)
request('PUT','/users/me/cuenta-cobro',{'aliasMercadoPago':'otro.'+tag},o)
passed('Configuración de cuentas de cobro de los dueños')
ciudad=request('GET','/ciudades')[0]['id']
def cancha(nombre,token):return request('POST','/canchas',{'nombre':nombre,'ciudadId':ciudad,'direccion':'Av. del Deporte 123','tipo':'futbol 5','superficie':'sintetico','cantJugadores':10,'precioHora':15000,'techada':True,'descripcion':'Cancha de prueba funcional.'},token)
c=cancha('Cancha Central',d);c2=cancha('Cancha Norte',d);c3=cancha('Cancha del Otro Dueño',o)
request('POST','/canchas',{'nombre':'No autorizado','ciudadId':ciudad,'precioHora':100},u,403)
fecha=(datetime.datetime.now(ZoneInfo('America/Argentina/Buenos_Aires')).date()+datetime.timedelta(days=2)).isoformat()
def reservar(inicio='18:00',fin='19:00',token=u,id=c['id'],expected=200):return request('POST','/reservas',{'canchaId':id,'fecha':fecha,'horaInicio':inicio,'horaFin':fin},token,expected)
reservar(expected=409)
agenda=[{'diaSemana':dia,'horaInicio':'17:00','horaFin':'23:00'} for dia in range(7)]
request('PUT',f"/canchas/{c['id']}/horarios",{'horarios':agenda},d)
request('PUT',f"/canchas/{c['id']}/horarios",{'horarios':[]},o,403)
request('PUT',f"/canchas/{c['id']}/horarios",{'horarios':agenda+[agenda[0]]},d,409)
assert len(request('GET',f"/canchas/{c['id']}/horarios"))==7
request('PUT',f"/canchas/{c['id']}/horarios",{'horarios':[{'diaSemana':7,'horaInicio':'17:00','horaFin':'23:00'}]},d,422)
passed('Agenda obligatoria, validación de días, superposición y permisos')
bloqueo=request('POST',f"/canchas/{c['id']}/bloqueos",{'fecha':fecha,'horaInicio':'19:00','horaFin':'20:00','motivo':'Mantenimiento'},d)
reservar('19:00','20:00',expected=409)
request('DELETE',f"/canchas/{c['id']}/bloqueos/{bloqueo['id']}",token=o,expected=403)
passed('Bloqueo por fecha y protección del bloqueo')
reserva=reservar();reservar(expected=409)
passed('Reserva dentro de horario y rechazo de duplicados')
availability=request('GET',f"/reservas/disponibilidad?canchaId={c['id']}&fecha={fecha}",token=u)
assert availability['ocupados']==[{'horaInicio':'18:00:00','horaFin':'19:00:00'}],availability
assert not any(k in json.dumps(availability) for k in ['userNombre','userId','email','telefono'])
passed('Disponibilidad pública sin datos de los jugadores')
detalle=request('GET',f"/reservas/{reserva['id']}/detalle",token=d)
assert detalle['usuario']['telefono']=='+54 9 297 400 0000'
assert detalle['usuario']['email']==accounts['jugador']['user']['email']
assert 'password' not in json.dumps(detalle) and 'cbu' not in json.dumps(detalle)
request('GET',f"/reservas/{reserva['id']}/detalle",token=o,expected=403)
request('GET',f"/reservas/{reserva['id']}/datos-transferencia",token=o,expected=403)
passed('Tarjeta del jugador restringida a su reserva')
cuenta=request('GET',f"/reservas/{reserva['id']}/datos-transferencia",token=u)
assert cuenta['aliasMercadoPago']=='prueba.'+tag
request('PUT',f"/reservas/{reserva['id']}/confirmar",{},o,403)
confirmada=request('PUT',f"/reservas/{reserva['id']}/confirmar",{},d)
assert confirmada['estado']=='CONFIRMADA'
request('PUT',f"/reservas/{reserva['id']}/cancelar",{},u,409)
passed('Confirmación manual del dueño y protección de reservas confirmadas')
# Two players compete for exactly the same slot on the same court.
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
 results=list(pool.map(lambda tok:reservar('20:00','21:00',tok,expected=None),[u,o]))
assert sorted(status for status,_ in results)==[200,409],results
passed('Concurrencia: dos solicitudes simultáneas generan una sola reserva')
r=request('POST','/reservas',{'canchaId':c['id'],'fecha':fecha,'horaInicio':'21:00','horaFin':'21:30'},u)
assert r['monto']==7500
request('PUT',f"/reservas/{r['id']}/cancelar",{},u)
reservar('21:00','21:30')
passed('Importes exactos y liberación del horario al cancelar')
owners=request('GET','/canchas/duenos')
assert {accounts['dueno']['user']['id'],accounts['otro']['user']['id']}.issubset({owner['id'] for owner in owners})
assert len(request('GET',f"/canchas/duenos/{accounts['dueno']['user']['id']}"))==2
request('PATCH',f"/canchas/{c2['id']}/publicacion",{'activa':False},d)
assert len(request('GET',f"/canchas/duenos/{accounts['dueno']['user']['id']}"))==1
request('GET',f"/canchas/{c2['id']}",expected=404)
request('PATCH',f"/canchas/{c2['id']}/publicacion",{'activa':True},d)
passed('Directorio por dueño y pausa/republicación de canchas')
post=request('POST','/publicaciones',{'contenido':'Prueba funcional de comunidad'},u)
assert request('POST',f"/publicaciones/{post['id']}/like",{},d)['likeado']
comment=request('POST',f"/publicaciones/{post['id']}/comentarios",{'contenido':'¡Nos vemos en la cancha!'},d)
assert comment['contenido']=='¡Nos vemos en la cancha!'
request('DELETE',f"/publicaciones/{post['id']}",token=o,expected=403)
request('GET','/notificaciones',token=d)
passed('Publicaciones, comentarios, me gusta, permisos y notificaciones')
if os.environ.get('SF_TEST_SESSION'):
 Path(os.environ['SF_TEST_SESSION']).write_text(json.dumps({'accounts':accounts,'cancha':c,'reserva':reserva,'fecha':fecha}))
print(json.dumps({'passed':len(checks),'checks':checks},ensure_ascii=False),flush=True)

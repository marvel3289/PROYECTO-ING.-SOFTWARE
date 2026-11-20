#!/usr/bin/env python3
"""Prueba de extremo a extremo de la API; solamente módulos estándar de Python."""
import os, sys, time, json, tempfile, subprocess, urllib.request, urllib.parse, urllib.error, http.cookiejar
from pathlib import Path

BASE=Path(__file__).resolve().parent
PORT='18976'
URL=f'http://127.0.0.1:{PORT}'
count=0

def check(name, predicate):
    global count
    if not predicate: raise AssertionError(name)
    count += 1
    print(f'OK {count:02d}  {name}')

class Cliente:
    def __init__(self):
        self.jar=http.cookiejar.CookieJar()
        self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
    def req(self,path,data=None):
        body=urllib.parse.urlencode(data).encode() if data is not None else None
        request=urllib.request.Request(URL+path,body,headers={'Content-Type':'application/x-www-form-urlencoded'})
        try:
            with self.opener.open(request, timeout=5) as result:
                return result.status,json.loads(result.read().decode())
        except urllib.error.HTTPError as e:
            return e.code,json.loads(e.read().decode())

with tempfile.TemporaryDirectory(prefix='biblioteca-verificacion-') as tmp:
    env=dict(os.environ,BIBLIOTECA_PUERTO=PORT,BIBLIOTECA_DATOS=str(Path(tmp)/'biblioteca.dat'))
    def iniciar():
        process=subprocess.Popen(['java','-Dfile.encoding=UTF-8','-jar','biblioteca-web.jar'],env=env,cwd=BASE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        for _ in range(70):
            try:
                with urllib.request.urlopen(URL+'/',timeout=.5) as r:
                    if r.status==200:return process
            except Exception: time.sleep(.1)
        print(process.stdout.read().decode())
        raise RuntimeError('No se pudo iniciar el servidor')
    p=iniciar()
    try:
        with urllib.request.urlopen(URL+'/') as r:
            check('HTML servido al navegador',r.status==200 and b'Iniciar sesi' in r.read())
        with urllib.request.urlopen(URL+'/app.js') as r:
            check('JavaScript entregado',r.status==200 and b'function modal' in r.read())
        anon=Cliente();admin=Cliente();lector=Cliente()
        check('API privada requiere sesión',anon.req('/api/me')[0]==401)
        check('Credenciales incorrectas rechazadas',anon.req('/api/login',{'correo':'admin@biblioteca.gt','contrasena':'incorrecta'})[0]>=400)
        code,me=admin.req('/api/login',{'correo':'admin@biblioteca.gt','contrasena':'Admin12345'})
        check('Inicio sesión administrador',code==200 and me['rol']=='ADMINISTRADOR')
        code,me2=lector.req('/api/login',{'correo':'lector@biblioteca.gt','contrasena':'Lector12345'})
        check('Inicio sesión lector',code==200 and me2['rol']=='LECTOR')
        check('El lector no accede a usuarios',lector.req('/api/usuarios')[0]==403)
        check('El lector no crea libros',lector.req('/api/libros/crear',{'titulo':'X','autor':'X','isbn':'123'})[0]==403)
        code,r=admin.req('/api/resumen')
        check('Resumen devuelve el libro inicial',code==200 and r['titulos']==1 and r['disponibles']==1)
        code,l=admin.req('/api/libros/crear',{'titulo':'Libro de prueba','autor':'Autora de prueba','isbn':'ISBN-TEST-001'})
        check('Registrar libro',code==201 and l['id']==2)
        check('Impedir ISBN duplicado',admin.req('/api/libros/crear',{'titulo':'Otro','autor':'Autor','isbn':'ISBN-TEST-001'})[0]==400)
        code,c=admin.req('/api/ejemplares/crear',{'libroId':l['id'],'codigo':'TEST-001'})
        check('Registrar ejemplar',code==201 and c['id']==2)
        check('Impedir código duplicado',admin.req('/api/ejemplares/crear',{'libroId':l['id'],'codigo':'TEST-001'})[0]==400)
        code,u=admin.req('/api/usuarios/crear',{'nombre':'Prueba Integration','correo':'prueba@test.gt','contrasena':'ContraseñaPrueba123','rol':'LECTOR'})
        check('Registrar usuario',code==201 and u['id']==3 and 'hash' not in u)
        code,p1=admin.req('/api/prestamos/crear',{'usuarioId':2,'codigo':'TEST-001'})
        check('Realizar préstamo',code==201 and p1['estado']=='ACTIVO' and p1['usuarioId']==2)
        check('No prestar ejemplar ocupado',admin.req('/api/prestamos/crear',{'usuarioId':3,'codigo':'TEST-001'})[0]==400)
        code,libros=lector.req('/api/libros')
        check('Cambio de inventario visible al lector',code==200 and next(l for l in libros if l['id']==2)['disponibles']==0)
        code,h=lector.req('/api/prestamos?usuarioId=3')
        check('Lector solo ve préstamos propios',code==200 and len(h)==1 and h[0]['usuarioId']==2)
        code,all_loans=admin.req('/api/prestamos')
        check('Personal ve todos los préstamos',code==200 and len(all_loans)==1)
        check('El lector no puede devolver',lector.req('/api/prestamos/devolver',{'prestamoId':1})[0]==403)
        code,p2=admin.req('/api/prestamos/devolver',{'prestamoId':p1['id']})
        check('Devolución funciona',code==200 and p2['estado']=='DEVUELTO')
        check('Impide devolver dos veces',admin.req('/api/prestamos/devolver',{'prestamoId':p1['id']})[0]==400)
        check('Ejemplar vuelve a estar disponible',next(l for l in admin.req('/api/libros')[1] if l['id']==2)['disponibles']==1)
        code,look=admin.req('/api/libros?q=prueba')
        check('Búsqueda del catálogo',code==200 and len(look)==1)
        code,res=admin.req('/api/logout',{})
        check('Cerrar sesión revoca cookie',code==200 and admin.req('/api/me')[0]==401)
    finally:
        p.terminate()
        try:p.wait(timeout=8)
        except subprocess.TimeoutExpired:p.kill()
    p=iniciar()
    try:
        restored=Cliente()
        code,me=restored.req('/api/login',{'correo':'prueba@test.gt','contrasena':'ContraseñaPrueba123'})
        check('Usuarios persistidos después de reiniciar',code==200 and me['id']==3)
        code,cat=restored.req('/api/libros')
        check('Libros y ejemplares persistidos',code==200 and any(l['isbn']=='ISBN-TEST-001' and l['disponibles']==1 for l in cat))
        code,h=restored.req('/api/prestamos')
        check('Lector nuevo solo ve su historial',code==200 and h==[])
        code,adm=restored.req('/api/logout',{})
        code,adm=restored.req('/api/login',{'correo':'admin@biblioteca.gt','contrasena':'Admin12345'})
        code,h=restored.req('/api/prestamos')
        check('Historial de préstamos persistido',code==200 and len(h)==1 and h[0]['estado']=='DEVUELTO')
    finally:
        p.terminate()
        try:p.wait(timeout=8)
        except subprocess.TimeoutExpired:p.kill()
print(f'\nTOTAL: {count} comprobaciones aprobadas')

'use strict';
const $ = (sel, root=document) => root.querySelector(sel);
const $$ = (sel, root=document) => [...root.querySelectorAll(sel)];
const state = {me:null, libros:[], usuarios:[], ejemplares:[], page:'dashboard'};
const roles = {ADMINISTRADOR:'Administrador',OPERADOR:'Operador',LECTOR:'Lector'};
const estadoColor = {ACTIVO:'',DEVUELTO:'neutral',VENCIDO:'danger'};
let toastTimeout;
function aviso(text, error=false){const t=$('#toast'); t.textContent=text; t.className='toast show'+(error?' error':''); clearTimeout(toastTimeout);toastTimeout=setTimeout(()=>t.className='toast',4500);}
async function api(path, values){
  const options = values === undefined ? {method:'GET'} : {method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:new URLSearchParams(values)};
  let r; try{r=await fetch('/api/'+path, options);}catch(e){throw Error('No se pudo conectar con el servidor.');}
  let json;try{json=await r.json();}catch(e){throw Error('Respuesta inesperada del servidor.');}
  if(!r.ok){if(r.status===401 && path!=='login'){salirVisual();}throw Error(json.error||'No se pudo completar la operación.');}
  return json;
}
function html(s){return String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
function fecha(s){return s?new Date(s+'T12:00:00').toLocaleDateString('es-GT',{day:'2-digit',month:'short',year:'numeric'}):'—';}
function staff(){return state.me?.rol!=='LECTOR';}
function admin(){return state.me?.rol==='ADMINISTRADOR';}
function salirVisual(){state.me=null;$('#appScreen').classList.add('hidden');$('#loginScreen').classList.remove('hidden');$('#loginForm').reset();}
function mostrarApp(me){
 state.me=me;$('#loginScreen').classList.add('hidden');$('#appScreen').classList.remove('hidden');
 $$('.staff-only').forEach(el=>el.classList.toggle('hidden',!staff()));
 $$('.admin-only').forEach(el=>el.classList.toggle('hidden',!admin()));
 $('#welcomeName').textContent=me.nombre.split(' ')[0];$('#sideName').textContent=me.nombre;
 $('#sideRole').textContent=roles[me.rol]||me.rol;
 $('#avatar').textContent=me.nombre.charAt(0).toUpperCase();$('#avatarTop').textContent=me.nombre.charAt(0).toUpperCase();
 $('#todayLabel').textContent=new Date().toLocaleDateString('es-GT',{weekday:'long',day:'numeric',month:'long'});
 pagina('dashboard');
}
function pagina(page){
 if((page==='usuarios'&&!admin())||(page==='alertas'&&!staff())) return;
 state.page=page;$$('.page').forEach(el=>el.classList.toggle('hidden',el.id!==`page-${page}`));
 $$('.nav-link').forEach(el=>el.classList.toggle('active',el.dataset.page===page));
 $('#breadcrumb').textContent=({dashboard:'Resumen',catalogo:'Catálogo',prestamos:'Préstamos',alertas:'Alertas',usuarios:'Usuarios'})[page];
 $('#sidebar').classList.remove('open');
 cargarPagina().catch(e=>aviso(e.message,true));
}
async function cargarPagina(){
 if(state.page==='dashboard') await resumen();
 if(state.page==='catalogo') await catalogo();
 if(state.page==='prestamos') await prestamos();
 if(state.page==='alertas') await alertas();
 if(state.page==='usuarios') await usuarios();
}
async function resumen(){
 const [r,libros]=await Promise.all([api('resumen'),api('libros')]);
 state.libros=libros;
 $('#statTitulos').textContent=r.titulos;$('#statEjemplares').textContent=r.ejemplares;$('#statDisponibles').textContent=r.disponibles;$('#statPrestamos').textContent=r.prestamosActivos;
 const list=$('#dashBooks');list.replaceChildren();
 if(!libros.length){list.innerHTML='<div class="empty-state"><span class="empty-symbol">▤</span>No hay libros registrados todavía.</div>';return;}
 libros.slice(0,5).forEach((l)=>{const div=document.createElement('div');div.className='compact-item';div.innerHTML=`<div class="book-tile">▤</div><div class="compact-book"><b>${html(l.titulo)}</b><small>${html(l.autor)}</small></div><span class="availability ${l.disponibles?'':'none'}">${l.disponibles} disponibles</span>`;list.append(div);});
 if(staff())api('alertas').then(a=>{const count=$('#alertNavCount'); count.textContent=a.length;count.classList.toggle('hidden',!a.length);}).catch(()=>{});
}
async function catalogo(){state.libros=await api('libros');renderLibros();}
function renderLibros(){
 const q=$('#bookSearch').value.toLocaleLowerCase().trim();const libros=state.libros.filter(l=>[l.titulo,l.autor,l.isbn].some(v=>v.toLocaleLowerCase().includes(q)));
 $('#bookCount').textContent=libros.length+' títulos';const body=$('#bookRows');body.replaceChildren();
 if(!libros.length){empty(body,staff()?5:4,'No hay libros que coincidan con tu búsqueda.');return;}
 libros.forEach(l=>{const tr=document.createElement('tr');tr.innerHTML=`<td><div class="td-book"><div class="book-tile">▤</div><div><strong>${html(l.titulo)}</strong><small>${html(l.autor)}</small></div></div></td><td>${html(l.isbn)}</td><td>${l.disponibles} de ${l.total}</td><td><span class="tag ${l.disponibles?'':'warn'}">${l.disponibles?'Disponible':'Sin disponibilidad'}</span></td>${staff()?'<td><button class="row-btn" data-add-copy="'+l.id+'">+ Ejemplar</button></td>':''}`;body.append(tr);});
}
async function prestamos(){
 const id=$('#loanUserFilter').value;let url='prestamos';if(staff()&&id)url+='?usuarioId='+encodeURIComponent(id);
 const ps=await api(url);const body=$('#loanRows');body.replaceChildren();
 if(!ps.length){empty(body,staff()?6:5,'No hay préstamos para este usuario.');return;}
 ps.sort((a,b)=>b.id-a.id).forEach(p=>{const tr=document.createElement('tr');tr.innerHTML=`<td><strong>#${p.id}</strong><small>${fecha(p.fechaPrestamo)}</small></td><td><strong>${html(p.libro)}</strong></td><td>#${p.usuarioId}</td><td>${fecha(p.fechaVencimiento)}</td><td><span class="tag ${estadoColor[p.estado]}">${html(p.estado)}</span></td>${staff()?'<td>'+(p.fechaDevolucion?'—':'<button class="row-btn" data-return="'+p.id+'">Registrar devolución</button>')+'</td>':''}`;body.append(tr);});
}
async function alertas(){const a=await api('alertas');const body=$('#alertRows');body.replaceChildren();$('#alertNavCount').textContent=a.length;$('#alertNavCount').classList.toggle('hidden',!a.length);
 if(!a.length){empty(body,5,'¡Muy bien! No hay préstamos con alertas pendientes.');return;}
 a.forEach(p=>{let tr=document.createElement('tr');tr.innerHTML=`<td><strong>#${p.id}</strong></td><td>${html(p.libro)}</td><td>#${p.usuarioId}</td><td>${fecha(p.fechaVencimiento)}</td><td><span class="tag ${p.vencido?'danger':'warn'}">${p.vencido?'Vencido':'Por vencer'}</span></td>`;body.append(tr);});
}
async function usuarios(){state.usuarios=await api('usuarios');const body=$('#userRows');body.replaceChildren();if(!state.usuarios.length){empty(body,4,'No hay usuarios.');return;}
 state.usuarios.forEach(u=>{const tr=document.createElement('tr');tr.innerHTML=`<td><strong>${html(u.nombre)}</strong><small>${html(u.correo)}</small></td><td>#${u.id}</td><td><span class="tag neutral">${html(roles[u.rol])}</span></td><td><span class="tag ${u.activo?'':'danger'}">${u.activo?'Activo':'Inactivo'}</span></td>`;body.append(tr);});
}
function empty(el,cols,msg){const tr=document.createElement('tr');const td=document.createElement('td');td.colSpan=cols;td.className='empty-state';td.textContent=msg;tr.append(td);el.append(tr);}
function modal(title,eyebrow,fields,onSubmit){$('#modalTitle').textContent=title;$('#modalEyebrow').textContent=eyebrow;const container=$('#modalBody');container.replaceChildren();const form=document.createElement('form');form.className='modal-form';
 fields.forEach(f=>{if(f.info){const p=document.createElement('p');p.className='modal-help';p.textContent=f.info;form.append(p);return;}
 const label=document.createElement('label');label.textContent=f.label;let inp;
 if(f.options){inp=document.createElement('select');f.options.forEach(o=>{const op=document.createElement('option');op.value=o.value;op.textContent=o.label;inp.append(op);});}
 else {inp=document.createElement('input');inp.type=f.type||'text';inp.placeholder=f.placeholder||'';if(f.min)inp.min=f.min;}
 inp.name=f.name;inp.required=f.required!==false;if(f.value!==undefined)inp.value=f.value;label.append(inp);form.append(label);});
 const button=document.createElement('button');button.className='btn btn-primary full';button.type='submit';button.textContent='Guardar cambios →';form.append(button);
 form.addEventListener('submit',async event=>{event.preventDefault();button.disabled=true;try{await onSubmit(Object.fromEntries(new FormData(form)));$('#modal').close();aviso('Operación guardada correctamente.');await cargarPagina();}catch(e){aviso(e.message,true);}finally{button.disabled=false;}});
 container.append(form);$('#modal').showModal();form.querySelector('input,select')?.focus();
}
function nuevoLibro(){modal('Registrar libro','CATÁLOGO',[
 {name:'titulo',label:'Título del libro',placeholder:'Ej. El principito'},{name:'autor',label:'Autor',placeholder:'Nombre del autor'},{name:'isbn',label:'ISBN',placeholder:'Ej. 978-0307474728'},
 {info:'Después de registrar un libro, puedes agregar sus ejemplares desde el catálogo.'}],
 f=>api('libros/crear',f));}
function nuevoEjemplar(libroId){const l=state.libros.find(l=>l.id===Number(libroId));modal('Agregar ejemplar','INVENTARIO',[
 {name:'libroId',label:'ID del libro',type:'number',value:libroId,min:1},{name:'codigo',label:'Código único del ejemplar',placeholder:'Ej. CAS-002'},
 {info:l?'Libro: '+l.titulo:'Cada ejemplar debe tener un código único.'}],f=>api('ejemplares/crear',f));}
async function nuevoPrestamo(){
 const [usuarios,ejemplares,libros]=await Promise.all([api('usuarios').catch(()=>null),api('ejemplares'),api('libros')]);
 const libres=ejemplares.filter(e=>e.disponible);const disponibles=libres.map(e=>({value:e.codigo,label:e.codigo+' · '+(libros.find(l=>l.id===e.libroId)?.titulo||'Libro')}));
 let usuariosOpts=[];
 if(usuarios)usuariosOpts=usuarios.filter(u=>u.activo).map(u=>({value:String(u.id),label:'#'+u.id+' · '+u.nombre+' ('+roles[u.rol]+')'}));
 const fields=usuariosOpts.length?[{name:'usuarioId',label:'Persona que recibe el libro',options:usuariosOpts}]:[{name:'usuarioId',label:'ID del lector',type:'number',min:1,placeholder:'Ej. 2'}];
 fields.push({name:'codigo',label:'Ejemplar disponible',options:disponibles.length?disponibles:[{value:'',label:'No hay ejemplares disponibles'}]});
 fields.push({info:'El sistema establece automáticamente una fecha de devolución 7 días después del préstamo.'});
 modal('Registrar préstamo','CIRCULACIÓN',fields,f=>api('prestamos/crear',f));
 if(!disponibles.length){const btn=$('#modal .btn-primary');btn.disabled=true;aviso('Primero registra un ejemplar disponible.',true);}
}
function nuevoUsuario(){modal('Crear usuario','ADMINISTRACIÓN',[
 {name:'nombre',label:'Nombre completo',placeholder:'Ej. María López'},{name:'correo',label:'Correo electrónico',type:'email',placeholder:'persona@correo.gt'},
 {name:'contrasena',label:'Contraseña temporal (mínimo 8 caracteres)',type:'password',placeholder:'Al menos 8 caracteres'},
 {name:'rol',label:'Rol del usuario',options:[{value:'LECTOR',label:'Lector'},{value:'OPERADOR',label:'Operador'},{value:'ADMINISTRADOR',label:'Administrador'}]}],f=>api('usuarios/crear',f));}
$('#loginForm').addEventListener('submit',async e=>{e.preventDefault();$('#loginError').textContent='';const button=$('#loginForm button[type=submit]');button.disabled=true;try{const me=await api('login',Object.fromEntries(new FormData(e.target)));mostrarApp(me);}catch(e){$('#loginError').textContent=e.message;}finally{button.disabled=false;}});
$('#logoutBtn').addEventListener('click',async()=>{try{await api('logout',{});}catch(e){}salirVisual();});
$$('.nav-link').forEach(btn=>btn.addEventListener('click',()=>pagina(btn.dataset.page)));
$$('[data-goto]').forEach(btn=>btn.addEventListener('click',()=>pagina(btn.dataset.goto)));
$('#menuBtn').addEventListener('click',()=>$('#sidebar').classList.toggle('open'));
$('#refreshBtn').addEventListener('click',()=>cargarPagina().then(()=>aviso('Datos actualizados.')).catch(e=>aviso(e.message,true)));
$('#bookSearch').addEventListener('input',renderLibros);$('#addBookBtn').addEventListener('click',nuevoLibro);
$('#bookRows').addEventListener('click',e=>{const target=e.target.closest('[data-add-copy]');if(target)nuevoEjemplar(target.dataset.addCopy);});
$('#loanBtn').addEventListener('click',()=>nuevoPrestamo().catch(e=>aviso(e.message,true)));
$('#filterLoans').addEventListener('click',()=>prestamos().catch(e=>aviso(e.message,true)));
$('#loanRows').addEventListener('click',async e=>{const btn=e.target.closest('[data-return]');if(!btn)return;if(!confirm('¿Confirmar la devolución del préstamo #'+btn.dataset.return+'?'))return;
 btn.disabled=true;try{await api('prestamos/devolver',{prestamoId:btn.dataset.return});aviso('Devolución registrada correctamente.');await prestamos();}catch(e){aviso(e.message,true);}finally{btn.disabled=false;}});
$('#addUserBtn').addEventListener('click',nuevoUsuario);$('#closeModal').addEventListener('click',()=>$('#modal').close());
api('me').then(mostrarApp).catch(()=>salirVisual());

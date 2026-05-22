/**
 * ================================================================
 * BARBER POST — dashboard-owner.js
 * Lógica completa del panel del dueño / administrador
 * ================================================================
 */

// ----------------------------------------------------------------
// INICIALIZACIÓN
// ----------------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    checkAuth('owner');
    initSidebar();
    setTopbarDate();
    loadResumen();         // sección por defecto
});

// ----------------------------------------------------------------
// AUTENTICACIÓN — verificar sesión en sessionStorage
// ----------------------------------------------------------------
function checkAuth(rolRequerido) {
    const raw = sessionStorage.getItem('bp_user');
    if (!raw) { redirectLogin(); return; }

    try {
        const user = JSON.parse(raw);
        if (rolRequerido === 'owner' && !user.isOwner) { redirectLogin(); return; }

        // Mostrar nombre e inicial en el sidebar
        setText('dashUserName',   user.nombre || user.username);
        setText('dashUserAvatar', (user.nombre || user.username || 'A')[0].toUpperCase());
    } catch {
        redirectLogin();
    }
}

function redirectLogin() {
    sessionStorage.removeItem('bp_user');
    window.location.href = 'login.html';
}

// ----------------------------------------------------------------
// SIDEBAR — navegación entre secciones
// ----------------------------------------------------------------
function initSidebar() {
    // Toggle sidebar en mobile
    const openBtn  = document.getElementById('sidebarOpen');
    const closeBtn = document.getElementById('sidebarClose');
    const sidebar  = document.getElementById('sidebar');

    openBtn?.addEventListener('click', () => {
        sidebar.classList.add('open');
        getOrCreateOverlay().classList.add('active');
    });
    closeBtn?.addEventListener('click', closeSidebar);

    // Navegación por ítems del sidebar
    document.querySelectorAll('.bp-nav-item[data-section]').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const section = item.dataset.section;
            navigateTo(section, item);
            if (window.innerWidth < 992) closeSidebar();
        });
    });

    // Logout
    document.getElementById('btnLogout')?.addEventListener('click', logout);
}

function closeSidebar() {
    document.getElementById('sidebar')?.classList.remove('open');
    getOrCreateOverlay().classList.remove('active');
}

function getOrCreateOverlay() {
    let ov = document.querySelector('.bp-sidebar-overlay');
    if (!ov) {
        ov = document.createElement('div');
        ov.className = 'bp-sidebar-overlay';
        ov.addEventListener('click', closeSidebar);
        document.body.appendChild(ov);
    }
    return ov;
}

// Mapa: clave de sección → loader
const SECTION_LOADERS = {
    resumen:     () => loadResumen(),
    turnos:      () => loadTurnos(),
    clientes:    () => loadClientes(),
    estadisticas:() => loadEstadisticas(),
    servicios:   () => loadServicios(),
};

function navigateTo(section, clickedItem) {
    // Actualizar estado activo en nav
    document.querySelectorAll('.bp-nav-item').forEach(i => i.classList.remove('active'));
    clickedItem?.classList.add('active');

    // Mostrar sección correcta
    document.querySelectorAll('.bp-section-content').forEach(s => s.classList.add('d-none'));
    const target = document.getElementById(`sec${capitalize(section)}`);
    if (target) target.classList.remove('d-none');

    // Actualizar título del topbar
    const titles = {
        resumen: 'Resumen del Día', turnos: 'Gestión de Turnos',
        clientes: 'Clientes Frecuentes', estadisticas: 'Estadísticas',
        servicios: 'Servicios y Precios',
    };
    setText('topbarTitle', titles[section] || section);

    // Cargar datos de la sección
    if (SECTION_LOADERS[section]) SECTION_LOADERS[section]();
}

// ----------------------------------------------------------------
// LOGOUT
// ----------------------------------------------------------------
async function logout() {
    try { await fetch('api/auth/logout', { method: 'POST' }); } catch {}
    sessionStorage.removeItem('bp_user');
    window.location.href = 'login.html';
}

// ----------------------------------------------------------------
// SECCIÓN: RESUMEN
// ----------------------------------------------------------------
async function loadResumen() {
    try {
        const resp = await fetch('api/dashboard/resumen');
        const json = await resp.json();
        if (json.success) renderResumen(json.data);
        else throw new Error();
    } catch {
        renderResumen(DEMO_RESUMEN);
    }
}

const DEMO_RESUMEN = {
    turnosHoy: 8, turnosPendientes: 3, totalClientes: 142, ingresosMes: 185000,
    proximosTurnos: [
        { hora: '10:30', clienteNombre: 'Carlos Rodríguez', clienteDni: '32.541.990', servicioNombre: 'Corte Clásico',  estado: 'confirmado' },
        { hora: '11:00', clienteNombre: 'Matías López',     clienteDni: '28.114.221', servicioNombre: 'Corte + Barba',  estado: 'pendiente' },
        { hora: '11:30', clienteNombre: 'Lucas Fernández',  clienteDni: '40.009.874', servicioNombre: 'Corte Difuminado',estado: 'confirmado'},
        { hora: '12:00', clienteNombre: 'Nicolás Torres',   clienteDni: '35.772.100', servicioNombre: 'Arreglo de Barba',estado:'pendiente' },
    ]
};

function renderResumen(data) {
    // El servidor devuelve { hoy:{pendientes,confirmados,completados,cancelados}, ingresosMes, totalClientes, proximos }
    const hoy = data.hoy || {};
    const turnosHoy = (hoy.pendientes || 0) + (hoy.confirmados || 0) + (hoy.completados || 0);

    setText('swTurnosHoy',  turnosHoy || data.turnosHoy || '—');
    setText('swPendientes', hoy.pendientes ?? data.turnosPendientes ?? '—');
    setText('swClientes',   data.totalClientes ?? '—');
    setText('swIngresos',   formatPrecio(data.ingresosMes ?? 0));

    const tbody = document.getElementById('proximosTurnosBody');
    if (!tbody) return;

    // El servidor puede devolver el array como 'proximos' o 'proximosTurnos'
    const turnos = data.proximos || data.proximosTurnos || [];
    if (!turnos.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center py-3" style="color:rgba(255,255,255,.3)">Sin turnos próximos</td></tr>';
        return;
    }

    tbody.innerHTML = turnos.map(t => {
        // Datos de cliente pueden venir anidados (t.cliente.nombre) o planos (t.clienteNombre)
        const clienteNombre = (t.cliente ? `${t.cliente.nombre || ''} ${t.cliente.apellido || ''}`.trim() : t.clienteNombre) || '—';
        const clienteDni    = (t.cliente ? t.cliente.dni : t.clienteDni) || '';
        const servicioNombre = (t.servicio ? t.servicio.nombre : t.servicioNombre) || '—';
        return `
        <tr>
            <td><strong>${t.hora || '—'}</strong></td>
            <td>${escHtml(clienteNombre)}</td>
            <td style="color:rgba(255,255,255,.5)">${escHtml(clienteDni)}</td>
            <td>${escHtml(servicioNombre)}</td>
            <td>${buildBadge(t.estado)}</td>
        </tr>`;
    }).join('');
}

// ----------------------------------------------------------------
// SECCIÓN: TURNOS
// ----------------------------------------------------------------
async function loadTurnos(desde, hasta, estado) {
    const params = new URLSearchParams();
    if (desde)  params.set('desde',  desde);
    if (hasta)  params.set('hasta',  hasta);
    if (estado) params.set('estado', estado);

    try {
        const resp = await fetch(`api/reservas/all?${params}`);
        const json = await resp.json();
        if (json.success) renderTurnos(json.data);
        else throw new Error();
    } catch {
        renderTurnos(DEMO_TURNOS);
    }
}

const DEMO_TURNOS = [
    { id: 1, fecha: '2025-01-15', hora: '09:00', clienteNombre: 'Carlos Rodríguez', servicioNombre: 'Corte Clásico',  precio: 1500, estado: 'completado' },
    { id: 2, fecha: '2025-01-15', hora: '09:30', clienteNombre: 'Matías López',     servicioNombre: 'Corte + Barba',  precio: 2500, estado: 'confirmado' },
    { id: 3, fecha: '2025-01-15', hora: '10:00', clienteNombre: 'Lucas Fernández',  servicioNombre: 'Corte Difuminado',precio: 1800, estado: 'pendiente'  },
    { id: 4, fecha: '2025-01-15', hora: '10:30', clienteNombre: 'Nicolás Torres',   servicioNombre: 'Teñido Completo', precio: 3500, estado: 'pendiente'  },
    { id: 5, fecha: '2025-01-14', hora: '15:00', clienteNombre: 'Diego Martínez',   servicioNombre: 'Barba',           precio: 900,  estado: 'cancelado'  },
];

function renderTurnos(turnos) {
    const tbody = document.getElementById('turnosBody');
    if (!tbody) return;

    if (!turnos.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-3" style="color:rgba(255,255,255,.3)">No se encontraron turnos</td></tr>';
        return;
    }

    tbody.innerHTML = turnos.map(t => {
        // Datos anidados del servidor: t.cliente.nombre, t.servicio.nombre, t.precioCobrado
        const clienteNombre = (t.cliente ? `${t.cliente.nombre || ''} ${t.cliente.apellido || ''}`.trim() : t.clienteNombre) || '—';
        const servicioNombre = (t.servicio ? t.servicio.nombre : t.servicioNombre) || '—';
        const precio = t.precioCobrado ?? t.precio ?? 0;
        return `
        <tr>
            <td>#${t.id}</td>
            <td>${formatFecha(t.fecha)}</td>
            <td><strong>${t.hora || '—'}</strong></td>
            <td>${escHtml(clienteNombre)}</td>
            <td>${escHtml(servicioNombre)}</td>
            <td>${formatPrecio(precio)}</td>
            <td>${buildBadge(t.estado)}</td>
            <td>
                <button class="bp-btn-outline btn-sm" onclick="abrirModalEstado(${t.id},'${t.estado}','${escHtml(clienteNombre)}')">
                    <i class="fas fa-pen me-1"></i>Estado
                </button>
            </td>
        </tr>`;
    }).join('');
}

function filtrarTurnos() {
    const desde  = document.getElementById('filtroDesde')?.value;
    const hasta  = document.getElementById('filtroHasta')?.value;
    const estado = document.getElementById('filtroEstado')?.value;
    loadTurnos(desde, hasta, estado);
}

// ----------------------------------------------------------------
// SECCIÓN: CLIENTES FRECUENTES
// ----------------------------------------------------------------
async function loadClientes() {
    try {
        const resp = await fetch('api/dashboard/clientes-top?limite=20');
        const json = await resp.json();
        if (json.success) renderClientes(json.data);
        else throw new Error();
    } catch {
        renderClientes(DEMO_CLIENTES);
    }
}

const DEMO_CLIENTES = [
    { nombre: 'Carlos Rodríguez', apellido: '', dni: '32.541.990', telefono: '1155443322', totalVisitas: 18 },
    { nombre: 'Matías López',     apellido: '', dni: '28.114.221', telefono: '1144556677', totalVisitas: 12 },
    { nombre: 'Lucas Fernández',  apellido: '', dni: '40.009.874', telefono: '1133445566', totalVisitas: 9  },
    { nombre: 'Nicolás Torres',   apellido: '', dni: '35.772.100', telefono: '1122334455', totalVisitas: 7  },
    { nombre: 'Diego Martínez',   apellido: '', dni: '31.098.765', telefono: '1199887766', totalVisitas: 5  },
];

function renderClientes(clientes) {
    const tbody = document.getElementById('clientesBody');
    if (!tbody) return;

    if (!clientes.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center py-3" style="color:rgba(255,255,255,.3)">Sin datos de clientes</td></tr>';
        return;
    }

    tbody.innerHTML = clientes.map((c, i) => {
        // La API real devuelve { cliente: {...}, totalVisitas } — el demo usa campos planos
        const cl = c.cliente || c;
        return `
        <tr>
            <td>${i + 1}</td>
            <td><strong>${escHtml(`${cl.nombre || ''} ${cl.apellido || ''}`.trim() || '—')}</strong></td>
            <td>${escHtml(cl.dni || '—')}</td>
            <td>${escHtml(cl.telefono || '—')}</td>
            <td>
                <span class="bp-badge bp-badge-confirmado">
                    <i class="fas fa-crown me-1" style="font-size:.65rem"></i>${c.totalVisitas}
                </span>
            </td>
        </tr>`;
    }).join('');
}

// ----------------------------------------------------------------
// SECCIÓN: ESTADÍSTICAS (Chart.js)
// ----------------------------------------------------------------
let chartFacturacion = null;
let chartServicios   = null;

async function loadEstadisticas() {
    const anio = new Date().getFullYear();

    try {
        const [r1, r2] = await Promise.all([
            fetch(`api/dashboard/facturacion?anio=${anio}`).then(r => r.json()),
            fetch('api/dashboard/servicios').then(r => r.json()),
        ]);

        // r1.data = { anio, meses: [{mes, cantidad, total}] } — hay que extraer el array
        const meses = (r1.success && r1.data?.meses) ? r1.data.meses : DEMO_FACTURACION;
        renderChartFacturacion(meses);
        // r2.data = [{servicio, cantidad, total}] — campo nombre es 'servicio' en la API real
        renderChartServicios(r2.success ? r2.data : DEMO_SERVICIOS);
    } catch {
        renderChartFacturacion(DEMO_FACTURACION);
        renderChartServicios(DEMO_SERVICIOS);
    }
}

const DEMO_FACTURACION = [
    { mes: 1, total: 52000 }, { mes: 2, total: 61000 }, { mes: 3, total: 74000 },
    { mes: 4, total: 68000 }, { mes: 5, total: 82000 }, { mes: 6, total: 95000 },
    { mes: 7, total: 88000 }, { mes: 8, total: 91000 }, { mes: 9, total: 105000 },
    { mes: 10, total: 112000 }, { mes: 11, total: 98000 }, { mes: 12, total: 130000 },
];
const DEMO_SERVICIOS = [
    { nombre: 'Corte Clásico', total: 45 }, { nombre: 'Corte + Barba', total: 32 },
    { nombre: 'Arreglo Barba', total: 28 }, { nombre: 'Difuminado', total: 21 },
    { nombre: 'Teñido', total: 9 },         { nombre: 'Otros', total: 15 },
];

const MESES = ['Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic'];

function renderChartFacturacion(data) {
    const ctx = document.getElementById('chartFacturacion');
    if (!ctx) return;
    if (chartFacturacion) chartFacturacion.destroy();

    chartFacturacion = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: data.map(d => MESES[(d.mes || 1) - 1]),
            datasets: [{
                label: 'Facturación Mensual ($)',
                data: data.map(d => d.total),
                backgroundColor: 'rgba(201,168,76,0.75)',
                borderColor: '#C9A84C',
                borderWidth: 1.5,
                borderRadius: 4,
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { labels: { color: 'rgba(255,255,255,.6)', font: { size: 12 } } } },
            scales: {
                x: { ticks: { color: 'rgba(255,255,255,.5)' }, grid: { color: 'rgba(255,255,255,.05)' } },
                y: {
                    ticks: { color: 'rgba(255,255,255,.5)', callback: v => '$' + (v/1000).toFixed(0) + 'K' },
                    grid:  { color: 'rgba(255,255,255,.05)' }
                }
            }
        }
    });
}

function renderChartServicios(data) {
    const ctx = document.getElementById('chartServicios');
    if (!ctx) return;
    if (chartServicios) chartServicios.destroy();

    const COLORS = ['#C9A84C','#e8c46e','#f0d698','#b8922f','#9c7a26','#7a601d'];

    chartServicios = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: data.map(d => d.servicio || d.nombre),
            datasets: [{
                data: data.map(d => d.cantidad ?? d.total),
                backgroundColor: COLORS,
                borderWidth: 0,
                hoverOffset: 8,
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: { position: 'bottom', labels: { color: 'rgba(255,255,255,.6)', padding: 14, font: { size: 12 } } }
            },
            cutout: '62%',
        }
    });
}

// ----------------------------------------------------------------
// SECCIÓN: SERVICIOS Y PRECIOS
// ----------------------------------------------------------------
async function loadServicios() {
    try {
        const resp = await fetch('api/servicios/todos');
        const json = await resp.json();
        if (json.success) renderServiciosTabla(json.data);
        else throw new Error();
    } catch {
        renderServiciosTabla(DEMO_SERVICIOS_FULL);
    }
}

const DEMO_SERVICIOS_FULL = [
    { id:1, nombre:'Corte Clásico',    precio:1500, duracionMinutos:30, activo:1 },
    { id:2, nombre:'Corte + Barba',    precio:2500, duracionMinutos:50, activo:1 },
    { id:3, nombre:'Arreglo de Barba', precio:900,  duracionMinutos:20, activo:1 },
    { id:4, nombre:'Corte Difuminado', precio:1800, duracionMinutos:35, activo:1 },
    { id:5, nombre:'Teñido Completo',  precio:3500, duracionMinutos:90, activo:1 },
    { id:6, nombre:'Afeitado Clásico', precio:1400, duracionMinutos:35, activo:1 },
    { id:7, nombre:'Corte Infantil',   precio:1200, duracionMinutos:25, activo:1 },
];

function renderServiciosTabla(servicios) {
    const tbody = document.getElementById('serviciosBody');
    if (!tbody) return;

    tbody.innerHTML = servicios.map(s => `
        <tr>
            <td>${escHtml(s.nombre)}</td>
            <td>${s.duracionMinutos} min</td>
            <td>
                <input class="bp-price-input" type="number" min="0" step="50"
                       value="${s.precio}" id="priceInput_${s.id}">
            </td>
            <td>${s.activo ? '<span class="bp-badge bp-badge-completado">Activo</span>' : '<span class="bp-badge bp-badge-cancelado">Inactivo</span>'}</td>
            <td>
                <button class="bp-btn-primary btn-sm" onclick="guardarPrecio(${s.id})">
                    <i class="fas fa-save me-1"></i>Guardar
                </button>
            </td>
        </tr>`).join('');
}

async function guardarPrecio(servicioId) {
    const input  = document.getElementById(`priceInput_${servicioId}`);
    const precio = parseFloat(input?.value);

    if (isNaN(precio) || precio < 0) { showToast('Precio inválido', 'error'); return; }

    try {
        const resp = await fetch(`api/servicios/${servicioId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ precio }),
        });
        const json = await resp.json();
        showToast(json.success ? '✓ Precio actualizado' : json.message, json.success ? 'ok' : 'error');
    } catch {
        showToast('✓ Precio guardado (demo)', 'ok');
    }
}

// ----------------------------------------------------------------
// MODAL — Cambiar estado de turno
// ----------------------------------------------------------------
function abrirModalEstado(turnoId, estadoActual, cliente) {
    document.getElementById('modalTurnoId').value  = turnoId;
    document.getElementById('modalCliente').textContent = cliente;
    const select = document.getElementById('modalEstado');
    if (select) select.value = estadoActual;
    new bootstrap.Modal(document.getElementById('modalEstadoTurno')).show();
}

async function guardarEstado() {
    const id     = document.getElementById('modalTurnoId')?.value;
    const estado = document.getElementById('modalEstado')?.value;
    const notas  = document.getElementById('modalNotas')?.value || '';

    try {
        const resp = await fetch(`api/reservas/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ estado, notas }),
        });
        const json = await resp.json();
        showToast(json.success ? '✓ Estado actualizado' : json.message, json.success ? 'ok' : 'error');
    } catch {
        showToast('✓ Estado actualizado (demo)', 'ok');
    }

    bootstrap.Modal.getInstance(document.getElementById('modalEstadoTurno'))?.hide();
    loadTurnos();
    loadResumen();
}

// ----------------------------------------------------------------
// UTILIDADES
// ----------------------------------------------------------------
function setTopbarDate() {
    const el = document.getElementById('topbarDate');
    if (el) el.textContent = new Date().toLocaleDateString('es-AR', { weekday:'long', day:'numeric', month:'long', year:'numeric' });
}

function buildBadge(estado) {
    const estados = { pendiente:'pendiente', confirmado:'confirmado', completado:'completado', cancelado:'cancelado' };
    const cls = estados[estado] || 'pendiente';
    return `<span class="bp-badge bp-badge-${cls}">${estado}</span>`;
}

function formatPrecio(v) {
    return '$' + (parseFloat(v)||0).toLocaleString('es-AR', { minimumFractionDigits: 0 });
}

function formatFecha(dateStr) {
    if (!dateStr) return '—';
    const [y, m, d] = dateStr.split('-');
    return `${d}/${m}/${y}`;
}

function escHtml(s) {
    return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = String(val || '');
}

function capitalize(s) { return s ? s.charAt(0).toUpperCase() + s.slice(1) : ''; }

function showToast(msg, type = 'info') {
    const div = document.createElement('div');
    div.style.cssText = `
        position:fixed;bottom:1.5rem;right:1.5rem;
        background:${type==='error'?'#ef4444':'#C9A84C'};color:${type==='error'?'#fff':'#1C1C1C'};
        padding:.65rem 1.3rem;border-radius:6px;font-size:.84rem;font-weight:600;
        z-index:9999;box-shadow:0 4px 16px rgba(0,0,0,.4);
    `;
    div.textContent = msg;
    document.body.appendChild(div);
    setTimeout(() => div.remove(), 3000);
}

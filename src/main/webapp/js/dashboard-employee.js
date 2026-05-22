/**
 * ================================================================
 * BARBER POST — dashboard-employee.js
 * Lógica del panel del empleado / barbero
 * ================================================================
 */

// ----------------------------------------------------------------
// INICIALIZACIÓN
// ----------------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    initSidebar();
    setTopbarDate();
    loadAgendaHoy();
});

// ----------------------------------------------------------------
// AUTENTICACIÓN — verificar sesión en sessionStorage
// ----------------------------------------------------------------
function checkAuth() {
    const raw = sessionStorage.getItem('bp_user');
    if (!raw) { redirectLogin(); return; }

    try {
        const user = JSON.parse(raw);
        setText('dashUserName',   user.nombre || user.username || 'Empleado');
        setText('dashUserAvatar', (user.nombre || user.username || 'E')[0].toUpperCase());
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
    const openBtn  = document.getElementById('sidebarOpen');
    const closeBtn = document.getElementById('sidebarClose');
    const sidebar  = document.getElementById('sidebar');

    openBtn?.addEventListener('click', () => {
        sidebar.classList.add('open');
        getOrCreateOverlay().classList.add('active');
    });
    closeBtn?.addEventListener('click', closeSidebar);

    document.querySelectorAll('.bp-nav-item[data-section]').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            navigateTo(item.dataset.section, item);
            if (window.innerWidth < 992) closeSidebar();
        });
    });

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

const SECTION_TITLES = {
    agenda:       'Mi Agenda de Hoy',
    turnosSemana: 'Turnos de la Semana',
    estadisticas: 'Mis Estadísticas',
};

function navigateTo(section, clickedItem) {
    document.querySelectorAll('.bp-nav-item').forEach(i => i.classList.remove('active'));
    clickedItem?.classList.add('active');

    document.querySelectorAll('.bp-section-content').forEach(s => s.classList.add('d-none'));
    const target = document.getElementById(`sec${capitalize(section)}`);
    if (target) target.classList.remove('d-none');

    setText('topbarTitle', SECTION_TITLES[section] || section);

    if (section === 'agenda')        loadAgendaHoy();
    if (section === 'turnosSemana')  loadSemana();
    if (section === 'estadisticas')  loadEstadisticas();
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
// AGENDA DEL DÍA
// ----------------------------------------------------------------
async function loadAgendaHoy() {
    const container = document.getElementById('agendaGrid');
    if (container) container.innerHTML = '<div class="text-center py-4"><div class="spinner-border bp-spinner" role="status"></div></div>';

    // Mostrar fecha actual
    const hoy = new Date().toLocaleDateString('es-AR', { weekday:'long', day:'numeric', month:'long' });
    setText('fechaHoyLabel', capitalize(hoy));

    try {
        const resp = await fetch('api/dashboard/turnos-hoy');
        const json = await resp.json();
        if (json.success) renderAgenda(json.data);
        else throw new Error();
    } catch {
        renderAgenda(DEMO_TURNOS_HOY);
    }
}

const DEMO_TURNOS_HOY = [
    { id:1, hora:'09:00', clienteNombre:'Carlos Rodríguez', clienteDni:'32.541.990', servicioNombre:'Corte Clásico',    precio:1500, estado:'completado' },
    { id:2, hora:'09:30', clienteNombre:'Matías López',     clienteDni:'28.114.221', servicioNombre:'Corte + Barba',    precio:2500, estado:'confirmado'  },
    { id:3, hora:'10:30', clienteNombre:'Lucas Fernández',  clienteDni:'40.009.874', servicioNombre:'Corte Difuminado', precio:1800, estado:'pendiente'   },
    { id:4, hora:'11:30', clienteNombre:'Nicolás Torres',   clienteDni:'35.772.100', servicioNombre:'Arreglo de Barba', precio:900,  estado:'pendiente'   },
    { id:5, hora:'12:00', clienteNombre:'Diego Martínez',   clienteDni:'31.098.765', servicioNombre:'Afeitado Clásico', precio:1400, estado:'cancelado'   },
];

function renderAgenda(turnos) {
    const container = document.getElementById('agendaGrid');
    if (!container) return;

    if (!turnos.length) {
        container.innerHTML = '<p class="text-center py-4" style="color:rgba(255,255,255,.35)">No hay turnos para hoy 🎉</p>';
        return;
    }

    // Actualizar counters
    let pendientes = 0, confirmados = 0, completados = 0;
    turnos.forEach(t => {
        if (t.estado === 'pendiente')  pendientes++;
        if (t.estado === 'confirmado') confirmados++;
        if (t.estado === 'completado') completados++;
    });
    setText('swPendientes',    pendientes);
    setText('swEnCurso',       confirmados);
    setText('swCompletadosHoy', completados);

    container.innerHTML = turnos.map(t => {
        const [hh, mm] = (t.hora || '00:00').split(':');
        const ampm = parseInt(hh) < 12 ? 'AM' : 'PM';

        const acciones = (t.estado !== 'completado' && t.estado !== 'cancelado')
            ? `<button class="bp-btn-primary btn-sm" onclick="abrirModalAtender(${t.id},'${escHtml(t.clienteNombre||'')}','${escHtml(t.clienteDni||'')}','${escHtml(t.servicioNombre||'')}','${t.hora||''}')">
                    <i class="fas fa-play me-1"></i>Atender
               </button>`
            : '';

        return `
        <div class="bp-agenda-card ${t.estado}">
            <div class="bp-agenda-time">
                <div class="hour">${hh}:${mm}</div>
                <div class="ampm">${ampm}</div>
            </div>
            <div class="bp-agenda-info">
                <div class="bp-agenda-client">${escHtml(t.clienteNombre || '—')}</div>
                <div class="bp-agenda-service">${escHtml(t.servicioNombre || '—')} · ${formatPrecio(t.precio)}</div>
                <div class="bp-agenda-meta">
                    ${buildBadge(t.estado)}
                    <span style="font-size:.75rem;color:rgba(255,255,255,.35)">DNI: ${escHtml(t.clienteDni || '—')}</span>
                </div>
            </div>
            <div class="bp-agenda-actions">${acciones}</div>
        </div>`;
    }).join('');
}

// ----------------------------------------------------------------
// TURNOS DE LA SEMANA
// ----------------------------------------------------------------
async function loadSemana() {
    const tbody = document.getElementById('semanaBody');
    if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="text-center py-3">Cargando...</td></tr>';

    try {
        const resp = await fetch('api/dashboard/turnos-semana');
        const json = await resp.json();
        if (json.success) renderSemana(json.data);
        else throw new Error();
    } catch {
        renderSemana(DEMO_SEMANA);
    }
}

const DEMO_SEMANA = [
    { id:1,  fecha:'2025-01-13', hora:'09:00', clienteNombre:'Andrea García',   servicioNombre:'Corte Clásico',    precio:1500, estado:'completado' },
    { id:2,  fecha:'2025-01-13', hora:'10:00', clienteNombre:'Pablo Ruiz',      servicioNombre:'Corte Difuminado', precio:1800, estado:'completado' },
    { id:3,  fecha:'2025-01-14', hora:'09:30', clienteNombre:'Facundo Gómez',   servicioNombre:'Corte + Barba',    precio:2500, estado:'completado' },
    { id:4,  fecha:'2025-01-14', hora:'11:00', clienteNombre:'Santiago Pérez',  servicioNombre:'Arreglo de Barba', precio:900,  estado:'completado' },
    { id:5,  fecha:'2025-01-15', hora:'09:00', clienteNombre:'Carlos Rodríguez',servicioNombre:'Corte Clásico',    precio:1500, estado:'completado' },
    { id:6,  fecha:'2025-01-15', hora:'10:30', clienteNombre:'Lucas Fernández', servicioNombre:'Corte Difuminado', precio:1800, estado:'pendiente'  },
    { id:7,  fecha:'2025-01-15', hora:'11:30', clienteNombre:'Nicolás Torres',  servicioNombre:'Arreglo Barba',    precio:900,  estado:'pendiente'  },
];

function renderSemana(turnos) {
    const tbody = document.getElementById('semanaBody');
    if (!tbody) return;

    if (!turnos.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-3" style="color:rgba(255,255,255,.3)">Sin turnos esta semana</td></tr>';
        return;
    }

    tbody.innerHTML = turnos.map(t => `
        <tr>
            <td>${formatFecha(t.fecha)}</td>
            <td>${escHtml(t.clienteNombre || '—')}</td>
            <td>${escHtml(t.servicioNombre || '—')}</td>
            <td><strong>${t.hora || '—'}</strong></td>
            <td>${formatPrecio(t.precio)}</td>
            <td>${buildBadge(t.estado)}</td>
            <td>
                ${(t.estado !== 'completado' && t.estado !== 'cancelado')
                    ? `<button class="bp-btn-primary btn-sm" onclick="abrirModalAtender(${t.id},'${escHtml(t.clienteNombre||'')}','','${escHtml(t.servicioNombre||'')}','${t.hora}')">
                            <i class="fas fa-check me-1"></i>Atender
                       </button>`
                    : '—'}
            </td>
        </tr>`).join('');
}

// ----------------------------------------------------------------
// ESTADÍSTICAS DEL EMPLEADO
// ----------------------------------------------------------------
let chartEmpServicios = null;

async function loadEstadisticas() {
    try {
        const resp = await fetch('api/dashboard/resumen');
        const json = await resp.json();
        const data = json.success ? json.data : DEMO_STATS;
        renderStatsEmployee(data);
    } catch {
        renderStatsEmployee(DEMO_STATS);
    }
}

const DEMO_STATS = { turnosHoy: 5, turnosSemana: 22, turnosMes: 91 };

function renderStatsEmployee(data) {
    setText('statHoy',    data.turnosHoy    || data.statHoy    || 5);
    setText('statSemana', data.turnosSemana || data.statSemana || 22);
    setText('statMes',    data.turnosMes    || data.statMes    || 91);

    const ctx = document.getElementById('chartEmpServicios');
    if (!ctx || typeof Chart === 'undefined') return;
    if (chartEmpServicios) chartEmpServicios.destroy();

    chartEmpServicios = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['Corte Clásico', 'Corte + Barba', 'Difuminado', 'Barba', 'Otros'],
            datasets: [{
                label: 'Atenciones',
                data: [34, 22, 18, 10, 7],
                backgroundColor: ['#C9A84C','#e8c46e','#f0d698','#b8922f','#9c7a26'],
                borderRadius: 4,
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: {
                x: { ticks: { color: 'rgba(255,255,255,.5)' }, grid: { color: 'rgba(255,255,255,.04)' } },
                y: { ticks: { color: 'rgba(255,255,255,.5)' }, grid: { color: 'rgba(255,255,255,.04)' } },
            }
        }
    });
}

// ----------------------------------------------------------------
// MODAL — Atender cliente (cambiar estado)
// ----------------------------------------------------------------
function abrirModalAtender(id, clienteNombre, dni, servicio, hora) {
    document.getElementById('atendTurnoId').value = id;
    setText('atendCliente',  clienteNombre);
    setText('atendDni',      dni || '—');
    setText('atendServicio', servicio);
    setText('atendHora',     hora);
    document.getElementById('atendNotas').value = '';
    new bootstrap.Modal(document.getElementById('modalAtender')).show();
}

async function cambiarEstadoEmp(nuevoEstado) {
    const id    = document.getElementById('atendTurnoId')?.value;
    const notas = document.getElementById('atendNotas')?.value || '';

    try {
        const resp = await fetch(`api/reservas/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ estado: nuevoEstado, notas }),
        });
        const json = await resp.json();
        showToast(json.success ? '✓ Estado actualizado' : json.message, json.success ? 'ok' : 'error');
    } catch {
        showToast('✓ Estado actualizado (demo)', 'ok');
    }

    bootstrap.Modal.getInstance(document.getElementById('modalAtender'))?.hide();
    loadAgendaHoy();
}

// ----------------------------------------------------------------
// UTILIDADES
// ----------------------------------------------------------------
function setTopbarDate() {
    const el = document.getElementById('topbarDate');
    if (el) el.textContent = new Date().toLocaleDateString('es-AR', { weekday:'long', day:'numeric', month:'long', year:'numeric' });
}

function buildBadge(estado) {
    return `<span class="bp-badge bp-badge-${estado || 'pendiente'}">${estado || 'pendiente'}</span>`;
}

function formatPrecio(v) {
    return '$' + (parseFloat(v)||0).toLocaleString('es-AR', { minimumFractionDigits: 0 });
}

function formatFecha(dateStr) {
    if (!dateStr) return '—';
    const [y, m, d] = dateStr.split('-');
    return `${d}/${m}`;
}

function escHtml(s) {
    return String(s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
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

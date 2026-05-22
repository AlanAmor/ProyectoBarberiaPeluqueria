/**
 * ================================================================
 * BARBER POST — reserva.js
 * Wizard de reserva: servicio → fecha → hora → datos → confirmación
 * ================================================================
 */

// ----------------------------------------------------------------
// ESTADO DE LA RESERVA
// ----------------------------------------------------------------
const reserva = {
    servicioId:     null,
    servicioNombre: '',
    precio:         0,
    duracion:       30,
    fecha:          '',
    hora:           '',
    nombre:         '',
    apellido:       '',
    dni:            '',
    telefono:       '',
    email:          '',
};

let pasoActual    = 1;
let flatpickrInst = null;

// Mapa: número de paso → ID del elemento en el HTML
const PASO_IDS = { 1: 'step1', 2: 'step2', 3: 'step3', 4: 'step4', 5: 'step5', 6: 'stepExito' };

// ----------------------------------------------------------------
// INICIALIZACIÓN
// ----------------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    cargarServicios();
    initFlatpickr();
    irAPaso(1);
});

// ----------------------------------------------------------------
// NAVEGACIÓN ENTRE PASOS
// ----------------------------------------------------------------
function irAPaso(n) {
    pasoActual = n;

    document.querySelectorAll('.bp-wizard-step').forEach(el => el.classList.add('d-none'));

    const target = document.getElementById(PASO_IDS[n]);
    if (target) target.classList.remove('d-none');

    // Actualizar indicadores de progreso (.bp-step-item)
    document.querySelectorAll('.bp-step-item').forEach((el, idx) => {
        el.classList.remove('active', 'done');
        const stepNum = idx + 1;
        if (stepNum < n)   el.classList.add('done');
        if (stepNum === n) el.classList.add('active');
    });
}

// ----------------------------------------------------------------
// PASO 1 — Cargar servicios
// ----------------------------------------------------------------
async function cargarServicios() {
    const grid = document.getElementById('serviciosListReserva');
    if (!grid) return;

    try {
        const resp = await fetch('api/servicios');
        if (!resp.ok) throw new Error();
        const json = await resp.json();
        if (!json.success) throw new Error();
        renderServicios(json.data, grid);
    } catch {
        renderServicios(SERVICIOS_DEMO, grid);
    }
}

const SERVICIOS_DEMO = [
    { id: 1, nombre: 'Corte Clásico',    precio: 1500, duracionMinutos: 30, descripcion: 'Tijera o máquina a elección' },
    { id: 2, nombre: 'Corte + Barba',    precio: 2500, duracionMinutos: 50, descripcion: 'Combo completo' },
    { id: 3, nombre: 'Arreglo de Barba', precio: 900,  duracionMinutos: 20, descripcion: 'Perfilado y delineado' },
    { id: 4, nombre: 'Corte Difuminado', precio: 1800, duracionMinutos: 35, descripcion: 'Fade / degradado' },
    { id: 5, nombre: 'Teñido Completo',  precio: 3500, duracionMinutos: 90, descripcion: 'Coloración completa' },
    { id: 6, nombre: 'Afeitado Clásico', precio: 1400, duracionMinutos: 35, descripcion: 'Con navaja y toalla caliente' },
    { id: 7, nombre: 'Corte Infantil',   precio: 1200, duracionMinutos: 25, descripcion: 'Para niños hasta 12 años' },
    { id: 8, nombre: 'Tratamiento',      precio: 2800, duracionMinutos: 60, descripcion: 'Hidratación profunda' },
];

function renderServicios(servicios, container) {
    container.innerHTML = servicios.map(s => `
        <div class="col-md-6 col-lg-4">
            <div class="bp-service-card dark-variant"
                 onclick="seleccionarServicio(${s.id}, '${escHtml(s.nombre)}', ${s.precio}, ${s.duracionMinutos}, this)">
                <div class="bp-sc-name">${escHtml(s.nombre)}</div>
                <div class="bp-sc-desc">${escHtml(s.descripcion || '')}</div>
                <div class="bp-sc-footer">
                    <span class="bp-sc-price">${formatPrecio(s.precio)}</span>
                    <span class="bp-sc-duration"><i class="far fa-clock me-1"></i>${s.duracionMinutos} min</span>
                </div>
            </div>
        </div>`).join('');
}

function seleccionarServicio(id, nombre, precio, duracion, el) {
    reserva.servicioId     = id;
    reserva.servicioNombre = nombre;
    reserva.precio         = precio;
    reserva.duracion       = duracion;

    document.querySelectorAll('#serviciosListReserva .bp-service-card').forEach(c => c.classList.remove('selected'));
    el.classList.add('selected');

    // Mostrar cotizador
    const cot = document.getElementById('cotizador');
    if (cot) {
        cot.style.display = 'block';
        setText('cotizadorPrecio',  formatPrecio(precio));
        setText('cotizadorDuracion', `${duracion} min`);
    }

    const btn = document.getElementById('btnSiguiente1');
    if (btn) btn.disabled = false;
}

// ----------------------------------------------------------------
// PASO 2 — Flatpickr (selector de fecha)
// ----------------------------------------------------------------
function initFlatpickr() {
    const input = document.getElementById('fechaPicker');
    if (!input || typeof flatpickr === 'undefined') return;

    flatpickrInst = flatpickr(input, {
        locale:     'es',
        minDate:    'today',
        maxDate:    new Date().fp_incr(60),
        disable:    [date => date.getDay() === 0],   // sin domingos
        dateFormat: 'Y-m-d',
        altInput:   true,
        altFormat:  'D, d \\d\\e F',
        allowInput: false,
        onChange: (selectedDates, dateStr) => {
            reserva.fecha = dateStr;
            reserva.hora  = '';

            const btn = document.getElementById('btnSiguiente2');
            if (btn) btn.disabled = false;

            cargarHorarios(dateStr);
        },
    });
}

// ----------------------------------------------------------------
// PASO 3 — Horarios disponibles
// ----------------------------------------------------------------
async function cargarHorarios(fecha) {
    const grid    = document.getElementById('horariosGrid');
    const loading = document.getElementById('horariosLoading');
    const vacio   = document.getElementById('horariosVacio');
    if (!grid) return;

    grid.classList.add('d-none');
    vacio.classList.add('d-none');
    loading.classList.remove('d-none');

    const btn = document.getElementById('btnSiguiente3');
    if (btn) btn.disabled = true;

    try {
        const url  = `api/turnos/disponibles?fecha=${fecha}&servicioId=${reserva.servicioId || 1}`;
        const resp = await fetch(url);
        if (!resp.ok) throw new Error();
        const json = await resp.json();

        loading.classList.add('d-none');

        if (json.data?.cerrado) { vacio.classList.remove('d-none'); return; }

        const rawSlots = json.data?.disponibles || [];
        const slots = rawSlots.map(s => typeof s === 'string' ? { hora: s, disponible: true } : s);
        if (!slots.length) { vacio.classList.remove('d-none'); return; }

        renderHorarios(slots);
    } catch {
        loading.classList.add('d-none');
        renderHorarios(generarSlotsDemo());
    }
}

function generarSlotsDemo() {
    const horas = [9, 9.5, 10, 10.5, 11, 11.5, 12, 14, 14.5, 15, 15.5, 16, 16.5, 17, 17.5, 18, 18.5];
    return horas.map(h => {
        const hh = String(Math.floor(h)).padStart(2, '0');
        const mm = h % 1 === 0 ? '00' : '30';
        return { hora: `${hh}:${mm}`, disponible: Math.random() > 0.3 };
    });
}

function renderHorarios(slots) {
    const grid = document.getElementById('horariosGrid');
    grid.classList.remove('d-none');
    grid.innerHTML = slots.map(s => {
        if (s.disponible === false) {
            return `<button class="bp-hora-btn occupied" disabled>${s.hora}</button>`;
        }
        return `<button class="bp-hora-btn" onclick="seleccionarHora('${s.hora}', this)">${s.hora}</button>`;
    }).join('');
}

function seleccionarHora(hora, el) {
    reserva.hora = hora;
    document.querySelectorAll('.bp-hora-btn').forEach(b => b.classList.remove('selected'));
    el.classList.add('selected');
    const btn = document.getElementById('btnSiguiente3');
    if (btn) btn.disabled = false;
}

// ----------------------------------------------------------------
// PASO 4 — Validación y avance
// ----------------------------------------------------------------
function validarDatosYAvanzar() {
    let valido = true;

    const campos = [
        { id: 'inputNombre',   errId: 'errorNombre',   msg: 'El nombre es requerido.' },
        { id: 'inputApellido', errId: 'errorApellido', msg: 'El apellido es requerido.' },
        { id: 'inputDni',      errId: 'errorDni',      msg: 'El DNI es requerido.' },
        { id: 'inputTelefono', errId: 'errorTelefono', msg: 'El teléfono es requerido.' },
    ];

    campos.forEach(c => {
        const input = document.getElementById(c.id);
        const error = document.getElementById(c.errId);
        const val   = (input?.value || '').trim();
        if (!val) {
            if (error) { error.textContent = c.msg; error.style.display = 'block'; }
            valido = false;
        } else {
            if (error) error.style.display = 'none';
        }
    });

    const dni = document.getElementById('inputDni')?.value.trim();
    if (dni && !/^\d{7,8}$/.test(dni)) {
        const err = document.getElementById('errorDni');
        if (err) { err.textContent = 'El DNI debe tener 7 u 8 números.'; err.style.display = 'block'; }
        valido = false;
    }

    const tel = document.getElementById('inputTelefono')?.value.trim();
    if (tel && tel.replace(/\D/g, '').length < 8) {
        const err = document.getElementById('errorTelefono');
        if (err) { err.textContent = 'Ingresá un teléfono válido.'; err.style.display = 'block'; }
        valido = false;
    }

    if (!valido) return;

    reserva.nombre   = document.getElementById('inputNombre').value.trim();
    reserva.apellido = document.getElementById('inputApellido').value.trim();
    reserva.dni      = dni;
    reserva.telefono = tel;
    reserva.email    = document.getElementById('inputEmail')?.value.trim() || '';

    popularResumen();
    irAPaso(5);
}

// ----------------------------------------------------------------
// PASO 5 — Resumen
// ----------------------------------------------------------------
function popularResumen() {
    const fechaLegs = reserva.fecha
        ? new Date(reserva.fecha + 'T12:00:00')
              .toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
        : '—';

    setText('resServicio', reserva.servicioNombre);
    setText('resFecha',    capitalize(fechaLegs));
    setText('resHora',     reserva.hora);
    setText('resCliente',  `${reserva.nombre} ${reserva.apellido}`);
    setText('resTelefono', reserva.telefono);
    setText('resPrecio',   formatPrecio(reserva.precio));
}

// ----------------------------------------------------------------
// CONFIRMAR RESERVA → POST a la API
// ----------------------------------------------------------------
async function confirmarReserva() {
    const btn = document.getElementById('btnConfirmar');
    if (btn) {
        btn.disabled  = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Procesando...';
    }

    const payload = {
        servicioId: reserva.servicioId,
        fecha:      reserva.fecha,
        hora:       reserva.hora,
        nombre:     reserva.nombre,
        apellido:   reserva.apellido,
        dni:        reserva.dni,
        telefono:   reserva.telefono,
        email:      reserva.email,
    };

    try {
        const resp = await fetch('api/reservas', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(payload),
        });
        const json = await resp.json();
        if (json.success) {
            mostrarExito(json.data);
        } else {
            showToast(json.message || 'No se pudo confirmar. Intentá de nuevo.', 'error');
            if (btn) {
                btn.disabled  = false;
                btn.innerHTML = '<i class="fab fa-whatsapp me-2"></i>Confirmar y enviar WhatsApp';
            }
        }
    } catch {
        mostrarExito({
            turnoId:    Math.floor(Math.random() * 9000) + 1000,
            whatsappUrl: buildWhatsAppUrl(),
        });
    }
}

function mostrarExito(data) {
    const fl = reserva.fecha
        ? new Date(reserva.fecha + 'T12:00:00')
              .toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'long' })
        : '';

    const turnoStr = data.turnoId ? ` — Turno #${data.turnoId}` : '';
    setText('exiMensaje',
        `${reserva.nombre} ${reserva.apellido} — ${reserva.servicioNombre}${turnoStr}\n` +
        `${capitalize(fl)} a las ${reserva.hora}`
    );

    const btnWa = document.getElementById('btnWhatsApp');
    if (btnWa) {
        btnWa.href   = data.whatsappUrl || buildWhatsAppUrl();
        btnWa.target = '_blank';
        btnWa.rel    = 'noopener noreferrer';
    }

    irAPaso(6);
}

function buildWhatsAppUrl() {
    const msg = encodeURIComponent(
        `¡Hola Barber Post! 💈 Confirmo mi turno:\n` +
        `👤 ${reserva.nombre} ${reserva.apellido}\n` +
        `✂ ${reserva.servicioNombre}\n` +
        `📅 ${reserva.fecha} a las ${reserva.hora}\n` +
        `Muchas gracias!`
    );
    return `https://wa.me/5491100000000?text=${msg}`;
}

// ----------------------------------------------------------------
// UTILIDADES
// ----------------------------------------------------------------
function formatPrecio(v) {
    return '$' + (parseFloat(v) || 0).toLocaleString('es-AR', { minimumFractionDigits: 0 });
}
function escHtml(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
function setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val || '';
}
function capitalize(str) {
    return str ? str.charAt(0).toUpperCase() + str.slice(1) : '';
}
function showToast(msg, type = 'info') {
    const div = document.createElement('div');
    div.style.cssText = `
        position:fixed;bottom:2rem;left:50%;transform:translateX(-50%);
        background:${type === 'error' ? '#ef4444' : '#C9A84C'};
        color:${type === 'error' ? '#fff' : '#1C1C1C'};
        padding:.7rem 1.5rem;border-radius:6px;font-size:.88rem;font-weight:600;
        z-index:99999;box-shadow:0 4px 16px rgba(0,0,0,.4);
    `;
    div.textContent = msg;
    document.body.appendChild(div);
    setTimeout(() => div.remove(), 3200);
}

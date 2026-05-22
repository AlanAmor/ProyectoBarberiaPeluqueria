/**
 * ================================================================
 * BARBER POST — main.js
 * Landing page: carga servicios desde API, navbar, AOS, chatbot
 * ================================================================
 */

// Contexto: si el backend no está corriendo, se usa la URL base vacía
// y la API responderá desde el mismo servidor Tomcat.
const API_BASE = '';

// ----------------------------------------------------------------
// INICIALIZACIÓN PRINCIPAL
// ----------------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    initAOS();
    initNavbar();
    loadServicios();
    initSmoothScroll();
    setCurrentYear();
});

// ----------------------------------------------------------------
// AOS — Animate On Scroll
// ----------------------------------------------------------------
function initAOS() {
    if (typeof AOS !== 'undefined') {
        AOS.init({
            duration: 700,
            easing: 'ease-out-cubic',
            once: true,          // solo anima una vez
            offset: 60,
        });
    }
}

// ----------------------------------------------------------------
// NAVBAR — efecto al hacer scroll
// ----------------------------------------------------------------
function initNavbar() {
    const navbar = document.querySelector('.bp-navbar');
    if (!navbar) return;

    const onScroll = () => {
        if (window.scrollY > 60) {
            navbar.classList.add('scrolled');
        } else {
            navbar.classList.remove('scrolled');
        }
    };

    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll(); // estado inicial
}

// ----------------------------------------------------------------
// CARGAR SERVICIOS desde la API y renderizarlos
// ----------------------------------------------------------------
async function loadServicios() {
    const container = document.getElementById('serviciosGrid');
    if (!container) return;

    try {
        const resp = await fetch('api/servicios');
        if (!resp.ok) throw new Error('API no disponible');

        const json = await resp.json();
        if (!json.success || !Array.isArray(json.data)) throw new Error('Sin datos');

        container.innerHTML = ''; // limpiar skeleton
        json.data.forEach(s => {
            container.insertAdjacentHTML('beforeend', buildServiceCard(s));
        });

        // Re-animar AOS en los nuevos cards
        if (typeof AOS !== 'undefined') AOS.refresh();

    } catch (err) {
        // Muestra datos de ejemplo si la API no responde (demo offline)
        container.innerHTML = buildFallbackServices();
    }
}

/**
 * Construye el HTML de una tarjeta de servicio
 * @param {Object} servicio
 * @returns {string} HTML
 */
function buildServiceCard(s) {
    const iconMap = {
        'Corte Clásico':        'fas fa-cut',
        'Corte + Barba':        'fas fa-user-tie',
        'Arreglo de Barba':     'fas fa-beard',
        'Corte Difuminado':     'fas fa-magic',
        'Teñido Completo':      'fas fa-palette',
        'Balayage / Mechitas':  'fa-solid fa-paintbrush',
        'Corte Infantil':       'fas fa-child',
        'Tratamiento Capilar':  'fas fa-flask',
        'Afeitado Clásico':     'fas fa-soap',
        'Diseño de Cejas':      'fas fa-eye',
    };
    const icon = iconMap[s.nombre] || 'fas fa-scissors';
    const precio = formatPrecio(s.precio);
    const duracion = s.duracionMinutos ? `${s.duracionMinutos} min` : '';

    return `
    <div class="bp-service-card" data-aos="fade-up">
        <div class="bp-sc-icon"><i class="${icon}"></i></div>
        <div class="bp-sc-name">${escHtml(s.nombre)}</div>
        <div class="bp-sc-desc">${escHtml(s.descripcion || '')}</div>
        <div class="bp-sc-footer">
            <span class="bp-sc-price">${precio}</span>
            <span class="bp-sc-duration">
                <i class="far fa-clock"></i>${duracion}
            </span>
        </div>
    </div>`;
}

/**
 * Tarjetas de fallback para demo sin backend
 */
function buildFallbackServices() {
    const demos = [
        { nombre: 'Corte Clásico',    descripcion: 'Corte tradicional con tijera o máquina.',        precio: 1500, icon: 'fas fa-cut',     dur: '30 min' },
        { nombre: 'Corte + Barba',    descripcion: 'Combo completo: corte y arreglo de barba.',       precio: 2500, icon: 'fas fa-user-tie',dur: '50 min' },
        { nombre: 'Arreglo de Barba', descripcion: 'Perfilado y delineado completo de barba.',        precio: 900,  icon: 'fas fa-beard',   dur: '20 min' },
        { nombre: 'Corte Difuminado', descripcion: 'Fade / degradado profesional con máquina.',       precio: 1800, icon: 'fas fa-magic',   dur: '35 min' },
        { nombre: 'Teñido Completo',  descripcion: 'Coloración completa con productos premium.',      precio: 3500, icon: 'fas fa-palette', dur: '90 min' },
        { nombre: 'Afeitado Clásico', descripcion: 'Afeitado tradicional con navaja y toalla caliente.', precio: 1400, icon: 'fas fa-soap', dur: '35 min' },
        { nombre: 'Corte Infantil',   descripcion: 'Para niños hasta 12 años.',                       precio: 1200, icon: 'fas fa-child',   dur: '25 min' },
        { nombre: 'Tratamiento',      descripcion: 'Hidratación y tratamiento capilar profundo.',     precio: 2800, icon: 'fas fa-flask',   dur: '60 min' },
    ];

    return demos.map(d => `
        <div class="bp-service-card" data-aos="fade-up">
            <div class="bp-sc-icon"><i class="${d.icon}"></i></div>
            <div class="bp-sc-name">${d.nombre}</div>
            <div class="bp-sc-desc">${d.descripcion}</div>
            <div class="bp-sc-footer">
                <span class="bp-sc-price">${formatPrecio(d.precio)}</span>
                <span class="bp-sc-duration"><i class="far fa-clock"></i>${d.dur}</span>
            </div>
        </div>`).join('');
}

// ----------------------------------------------------------------
// SMOOTH SCROLL para links de ancla internos
// ----------------------------------------------------------------
function initSmoothScroll() {
    document.querySelectorAll('a[href^="#"]').forEach(link => {
        link.addEventListener('click', (e) => {
            const href = link.getAttribute('href');
            const target = href === '#' ? document.documentElement : document.querySelector(href);
            if (!target) return;
            e.preventDefault();

            const offset = 72; // altura del navbar fijo
            const top = target.getBoundingClientRect().top + window.scrollY - offset;
            window.scrollTo({ top, behavior: 'smooth' });

            // cerrar navbar mobile si está abierto
            const navCollapse = document.getElementById('navMenu');
            if (navCollapse && navCollapse.classList.contains('show')) {
                navCollapse.classList.remove('show');
            }
        });
    });
}

// ----------------------------------------------------------------
// AÑO ACTUAL en el footer
// ----------------------------------------------------------------
function setCurrentYear() {
    const el = document.getElementById('currentYear');
    if (el) el.textContent = new Date().getFullYear();
}

// ----------------------------------------------------------------
// UTILIDADES
// ----------------------------------------------------------------

/** Formatea precio en pesos argentinos: $1.500 */
function formatPrecio(value) {
    const num = parseFloat(value) || 0;
    return '$' + num.toLocaleString('es-AR', { minimumFractionDigits: 0 });
}

/** Sanitiza texto para insertar como HTML */
function escHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

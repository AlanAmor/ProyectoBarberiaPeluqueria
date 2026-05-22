/**
 * ================================================================
 * BARBER POST — chatbot.js
 * Motor del chatbot basado en reglas (sin IA externa)
 * ================================================================
 */

document.addEventListener('DOMContentLoaded', () => {
    initChatbot();
});

// ----------------------------------------------------------------
// ESTADO DEL CHATBOT
// ----------------------------------------------------------------
let chatOpened = false;

// ----------------------------------------------------------------
// INICIALIZACIÓN
// ----------------------------------------------------------------
function initChatbot() {
    const toggleBtn   = document.getElementById('chatbotToggle');
    const closeBtn    = document.getElementById('chatbotClose');
    const window_     = document.getElementById('chatbotWindow');
    const inputEl     = document.getElementById('chatInput');
    const sendBtn     = document.getElementById('chatSendBtn');

    if (!toggleBtn) return; // la página no usa chatbot

    // Abrir/cerrar con el botón flotante
    toggleBtn.addEventListener('click', () => {
        const hidden = window_.classList.contains('bp-chat-hidden');
        if (hidden) openChat(); else closeChat();
    });

    // Cerrar desde el header
    if (closeBtn) closeBtn.addEventListener('click', closeChat);

    // Enviar mensaje con el botón
    if (sendBtn) sendBtn.addEventListener('click', handleSend);

    // Enviar mensaje con Enter (Shift+Enter = nueva línea)
    if (inputEl) {
        inputEl.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSend();
            }
        });
    }
}

function openChat() {
    const window_ = document.getElementById('chatbotWindow');
    const badge   = document.getElementById('chatBadge');

    window_.classList.remove('bp-chat-hidden');
    if (badge) badge.style.display = 'none';

    if (!chatOpened) {
        chatOpened = true;
        // Mostrar mensaje inicial con efecto de escritura
        setTimeout(() => {
            appendBotMessage('¡Hola! 👋 Soy el asistente virtual de *Barber Post*. ¿En qué te puedo ayudar?\n\nPodés preguntarme sobre precios, servicios, horarios o cómo reservar tu turno.');
        }, 300);
    }

    // Hacer foco en el input
    const inputEl = document.getElementById('chatInput');
    if (inputEl) setTimeout(() => inputEl.focus(), 200);
}

function closeChat() {
    const window_ = document.getElementById('chatbotWindow');
    window_.classList.add('bp-chat-hidden');
}

// ----------------------------------------------------------------
// MANEJAR ENVÍO DE MENSAJE
// ----------------------------------------------------------------
function handleSend() {
    const inputEl = document.getElementById('chatInput');
    const msg = (inputEl?.value || '').trim();
    if (!msg) return;

    appendUserMessage(msg);
    inputEl.value = '';

    // Mostrar indicador de escritura y responder después de un delay natural
    showTyping();
    const delay = 700 + Math.random() * 600;
    setTimeout(() => {
        hideTyping();
        const response = getResponse(msg);
        appendBotMessage(response);
    }, delay);
}

// ----------------------------------------------------------------
// MOTOR DE RESPUESTAS — sistema basado en reglas con palabras clave
// ----------------------------------------------------------------

/**
 * Normaliza texto: lowercase, elimina acentos y puntuación
 */
function normalizeText(text) {
    return text
        .toLowerCase()
        .normalize('NFD').replace(/[\u0300-\u036f]/g, '') // quitar acentos
        .replace(/[¿¡.,!?;:()""'']/g, ' ')
        .replace(/\s+/g, ' ')
        .trim();
}

/**
 * Categorías del chatbot con palabras clave y respuestas
 */
const CHAT_CATEGORIES = [
    {
        name: 'greeting',
        keywords: ['hola', 'holis', 'buenas', 'buenos dias', 'buenas tardes', 'buenas noches', 'hey', 'hi', 'saludos', 'que tal', 'como estas', 'buen dia'],
        responses: [
            '¡Hola! Bienvenido a Barber Post ✂ ¿En qué te puedo ayudar?',
            '¡Buenas! ¿Cómo te va? Estoy acá para ayudarte con consultas sobre precios, servicios o reservas 😊',
            '¡Hola! ¿Querés reservar un turno o tenés alguna consulta?',
        ]
    },
    {
        name: 'farewell',
        keywords: ['chau', 'adios', 'hasta luego', 'bye', 'nos vemos', 'gracias', 'cheers', 'buenas noches'],
        responses: [
            '¡Hasta luego! Fue un gusto. 👋',
            '¡Chau! Cualquier cosa que necesites, acá estamos ✂',
            '¡Hasta la próxima! Recordá que podés reservar tu turno online 🗓',
        ]
    },
    {
        name: 'prices',
        keywords: ['precio', 'costo', 'cuanto cuesta', 'cuanto sale', 'cuanto cobra', 'cuanto vale', 'tarifa', 'valor', 'plata', 'pesos', 'cuanto es', 'lista de precios', 'precios'],
        responses: [
            `💈 **Lista de Precios Barber Post**\n\n✂ Corte Clásico ......... $1.500\n✂ Corte + Barba ......... $2.500\n✂ Arreglo de Barba ..... $900\n✂ Corte Difuminado ...... $1.800\n✂ Teñido Completo ....... $3.500\n✂ Balayage/Mechitas ..... $4.500\n✂ Corte Infantil ........ $1.200\n✂ Tratamiento Capilar ... $2.800\n✂ Afeitado Clásico ...... $1.400\n✂ Diseño de Cejas ....... $700\n\n¿Querés reservar alguno? 😊`,
        ]
    },
    {
        name: 'corte_clasico',
        keywords: ['corte clasico', 'corte de pelo', 'corte de cabello', 'cortarme el pelo', 'corte simple', 'clasico'],
        responses: [
            '✂ El **Corte Clásico** tiene un precio de **$1.500** y dura aproximadamente 30 minutos.\nIncluye lavado, corte con tijera o máquina a elección.\n\n¿Querés reservar un turno?',
        ]
    },
    {
        name: 'barba',
        keywords: ['barba', 'afeitar', 'afeitado', 'corte barba', 'arreglo barba', 'delinear barba', 'perfil barba'],
        responses: [
            '🪒 Tenemos dos opciones para la barba:\n\n• **Arreglo de Barba** — $900 (20 min): perfilado y delineado completo.\n• **Corte + Barba** — $2.500 (50 min): el combo ideal.\n• **Afeitado Clásico** — $1.400 (35 min): con navaja y toalla caliente.\n\n¿Te agendo un turno?',
        ]
    },
    {
        name: 'tenido',
        keywords: ['tenido', 'coloracion', 'teñir', 'color pelo', 'cambiar color', 'pintarme', 'tinte', 'tintura'],
        responses: [
            '🎨 Para coloración tenemos:\n\n• **Teñido Completo** — $3.500 (90 min): coloración de todo el cabello.\n• **Balayage / Mechitas** — $4.500 (120 min): técnica de iluminación natural.\n\nTe recomendamos reservar con anticipación ya que estos servicios requieren más tiempo 😊',
        ]
    },
    {
        name: 'infantil',
        keywords: ['niño', 'nene', 'chico', 'infantil', 'hijo', 'nena', 'pequeño', 'child', 'kids'],
        responses: [
            '👶 ¡Claro que sí! El **Corte Infantil** es para niños hasta 12 años.\n\n💈 Precio: **$1.200** | Duración: ~25 minutos.\n\nNuestros barberos tienen mucha paciencia 😄 ¿Te agendo un turno?',
        ]
    },
    {
        name: 'tratamiento',
        keywords: ['tratamiento', 'hidratacion', 'caida de pelo', 'capilar', 'keratina', 'hidrar', 'tratamiento capilar'],
        responses: [
            '💊 El **Tratamiento Capilar** tiene un valor de **$2.800** y dura 60 minutos.\nIncluye hidratación profunda y tratamiento reconstructor.\n\n¡Ideal si tu cabello necesita un mimo! 😊',
        ]
    },
    {
        name: 'cejas',
        keywords: ['cejas', 'diseno cejas', 'diseño de cejas', 'depilacion cejas'],
        responses: [
            '👁 El **Diseño de Cejas** cuesta **$700** y dura solo 15 minutos.\nDepilación y diseño especializado para hombres.\n\n¿Te lo sumamos a un corte? 😊',
        ]
    },
    {
        name: 'hours',
        keywords: ['horario', 'hora', 'cuando abren', 'cuando cierran', 'a que hora', 'atienden', 'dias', 'abierto', 'cerrado', 'atienden sabados', 'domingo'],
        responses: [
            `🕐 **Horarios de Atención Barber Post**\n\n📅 Lunes a Viernes: 9:00 - 20:00\n📅 Sábados: 9:00 - 18:00\n❌ Domingos: cerrado\n\nPodés reservar tu turno online las 24hs ✨`,
        ]
    },
    {
        name: 'location',
        keywords: ['donde estan', 'donde quedan', 'ubicacion', 'direccion', 'como llego', 'sucursal', 'local', 'barrio'],
        responses: [
            '📍 Podés encontrarnos en nuestras sucursales en Buenos Aires.\n\nConsultá la dirección más cercana a vos enviándonos un WhatsApp o reservando tu turno online donde elegís la sede.',
        ]
    },
    {
        name: 'booking',
        keywords: ['reservar', 'reserva', 'turno', 'sacar turno', 'pedir turno', 'como reservo', 'como saco', 'agenda', 'agendar', 'cita'],
        responses: [
            '📱 ¡Reservar es muy fácil! Solo seguí estos pasos:\n\n1️⃣ Hacé clic en **"Reservar Turno"**\n2️⃣ Elegí el servicio que querés\n3️⃣ Seleccioná la fecha y el horario disponible\n4️⃣ Completá tus datos\n5️⃣ ¡Listo! Te mandamos confirmación por WhatsApp 🟢\n\n<a href="reservar.html" style="color:#C9A84C;font-weight:600;">→ Reservar ahora</a>',
        ]
    },
    {
        name: 'whatsapp',
        keywords: ['whatsapp', 'wasap', 'wsp', 'mensaje', 'escribirles', 'contactarlos'],
        responses: [
            '📲 ¡Podés escribirnos directamente por WhatsApp! Al confirmar tu reserva online, el sistema te abre el chat automáticamente con los datos de tu turno.\n\nO si preferís, hacé clic en **"Reservar Turno"** y al final del proceso te llevamos al WhatsApp 🟢',
        ]
    },
    {
        name: 'promotions',
        keywords: ['descuento', 'promocion', 'promo', 'oferta', 'precio especial', 'combo', '2x1', 'especiales', 'beneficio'],
        responses: [
            '🎉 ¡Tenemos promos especiales! El **Combo Corte + Barba** ya es un precio especial combinado ($2.500 en vez de pagar por separado).\n\nSeguinos en redes para enterarte de las promos del mes 📲',
            '👀 ¡Hay sorpresas! Consultá nuestras promos del mes siguiéndonos en Instagram. También tenemos precios especiales en días y horarios específicos.',
        ]
    },
    {
        name: 'staff',
        keywords: ['barbero', 'quien atiende', 'estilista', 'peluquero', 'profesional', 'personal', 'staff', 'empleado'],
        responses: [
            '💈 Nuestro equipo está formado por **barberos profesionales** con años de experiencia.\n\nCada uno especializado en diferentes técnicas: clásicos, degradados, diseño de barba y coloración.',
        ]
    },
    {
        name: 'cancel',
        keywords: ['cancelar', 'cancelación', 'anular', 'anulacion', 'cambiar turno', 'reprogramar', 'modificar turno'],
        responses: [
            '🔄 Para **cancelar o modificar** tu turno, escribinos por WhatsApp con tu nombre y DNI y te ayudamos enseguida.\n\nTe pedimos que avises con al menos **2 horas de anticipación** si necesitás cancelar 🙏',
        ]
    },
    {
        name: 'payment',
        keywords: ['pago', 'como pago', 'efectivo', 'tarjeta', 'mercadopago', 'transferencia', 'débito', 'credito', 'debito'],
        responses: [
            '💳 Aceptamos:\n\n💵 Efectivo\n💳 Tarjeta de débito y crédito\n📱 Mercado Pago / transferencia\n\nEl pago se realiza al finalizar el servicio en el local.',
        ]
    },
    {
        name: 'services_list',
        keywords: ['que servicios', 'que ofrecen', 'que hacen', 'servicios', 'que tipos', 'menu', 'lista'],
        responses: [
            '✂ **Nuestros servicios:**\n\n• Corte Clásico\n• Corte + Barba (combo)\n• Arreglo de Barba\n• Corte Difuminado (fade)\n• Teñido Completo\n• Balayage / Mechitas\n• Corte Infantil\n• Tratamiento Capilar\n• Afeitado Clásico\n• Diseño de Cejas\n\nPreguntame por cualquiera para saber el precio y duración 😊',
        ]
    },
];

const DEFAULT_RESPONSES = [
    'No estoy seguro de cómo ayudarte con eso 🤔 Podés preguntarme sobre **precios**, **servicios**, **horarios** o cómo **reservar un turno**.',
    '¡Mmm! No lo entendí bien. Podés preguntarme: "¿cuánto sale un corte?", "¿qué servicios tienen?" o "¿cómo reservo?"',
    'Disculpá, eso escapa a mis posibilidades 😅 Pero sí puedo ayudarte con información sobre nuestros servicios, precios y reservas.',
];

/**
 * Obtiene la respuesta correspondiente al mensaje del usuario
 * @param {string} userMsg
 * @returns {string}
 */
function getResponse(userMsg) {
    const normalized = normalizeText(userMsg);

    for (const category of CHAT_CATEGORIES) {
        for (const kw of category.keywords) {
            if (normalized.includes(kw)) {
                return randomPick(category.responses);
            }
        }
    }

    return randomPick(DEFAULT_RESPONSES);
}

/** Selecciona un ítem aleatorio de un array */
function randomPick(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

// ----------------------------------------------------------------
// RENDER de mensajes en el DOM
// ----------------------------------------------------------------

let typingEl = null;

function appendUserMessage(text) {
    const container = document.getElementById('chatMessages');
    if (!container) return;
    const div = document.createElement('div');
    div.className = 'bp-msg bp-msg-user';
    div.textContent = text;
    container.appendChild(div);
    scrollToBottom();
}

function appendBotMessage(text) {
    const container = document.getElementById('chatMessages');
    if (!container) return;
    const div = document.createElement('div');
    div.className = 'bp-msg bp-msg-bot';
    // Soporte básico de markdown: **negrita** y saltos de línea
    div.innerHTML = formatBotText(text);
    container.appendChild(div);
    scrollToBottom();
}

/** Formatea texto del bot: convierte **negrita**, saltos y links */
function formatBotText(text) {
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;').replace(/>/g, '&gt;')
        // Restaurar links que habíamos sanitizado (en respuestas hardcodeadas)
        .replace(/&lt;a href="([^"]+)" style="([^"]+)"&gt;(.+?)&lt;\/a&gt;/g,
            '<a href="$1" style="$2">$3</a>')
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        .replace(/\n/g, '<br>');
}

function showTyping() {
    const container = document.getElementById('chatMessages');
    if (!container) return;
    typingEl = document.createElement('div');
    typingEl.className = 'bp-msg-typing';
    typingEl.innerHTML = '<span></span><span></span><span></span>';
    container.appendChild(typingEl);
    scrollToBottom();
}

function hideTyping() {
    if (typingEl) {
        typingEl.remove();
        typingEl = null;
    }
}

function scrollToBottom() {
    const container = document.getElementById('chatMessages');
    if (container) container.scrollTop = container.scrollHeight;
}

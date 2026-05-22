package com.barberpost.servlet;

import com.barberpost.util.JsonUtil;

import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

/**
 * ChatbotServlet — Motor de respuestas automáticas del chatbot
 *
 * Endpoint:
 *   POST /api/chatbot
 *   Body: { "mensaje": "cuanto cuesta un corte?" }
 *   Respuesta: { "success": true, "data": { "respuesta": "..." } }
 *
 * LÓGICA: Sistema de reglas basado en palabras clave (keyword matching).
 * No requiere IA externa ni APIs de pago. El mensaje se normaliza
 * (minúsculas, sin tildes) y se busca coincidencia en cada categoría.
 * Si no hay coincidencia exacta, retorna una respuesta por defecto amigable.
 *
 * Para agregar nuevas categorías, simplemente agregar una entrada al
 * mapa CATEGORIAS con sus keywords y respuestas.
 *
 * @version 1.0.0
 */
public class ChatbotServlet extends HttpServlet {

    // ============================================================
    // Estructura del chatbot: categorías con keywords y respuestas
    // ============================================================
    private static final List<Categoria> CATEGORIAS = new ArrayList<>();

    static {
        // ---- Saludos ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("hola","buenas","buenas tardes","buenos dias","buenas noches",
                          "hey","saludos","buen dia","hi","que tal","como estan","ola"),
            Arrays.asList(
                "¡Hola! Bienvenido a Barber Post ✂️ ¿En qué te puedo ayudar hoy?",
                "¡Buenas! Soy el asistente de Barber Post. Puedo ayudarte con precios, servicios, horarios y más. ¿Qué necesitás?",
                "¡Hola! Qué bueno que nos visitás 💈 ¿Tenés alguna consulta?"
            )
        ));

        // ---- Precios generales ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("precio","cuanto cuesta","cuanto sale","cuanto vale","tarifa",
                          "valor","costo","plata","pesos","cobran","cuanto es","caro","barato",
                          "economico","arancel","presupuesto","cuanto cobran","que sale"),
            Arrays.asList(
                "💰 Nuestros precios son:\n\n" +
                "✂️ Corte Clásico: $1.500\n" +
                "✂️ Corte + Barba: $2.500\n" +
                "🧔 Arreglo de Barba: $900\n" +
                "🌀 Corte Difuminado (Fade): $1.800\n" +
                "🎨 Teñido Completo: $3.500\n" +
                "✨ Balayage / Mechitas: $4.500\n" +
                "👦 Corte Infantil: $1.200\n" +
                "💆 Tratamiento Capilar: $2.800\n" +
                "🪒 Afeitado Clásico: $1.400\n" +
                "👁️ Diseño de Cejas: $700\n\n" +
                "¿Querés reservar un turno? 📅"
            )
        ));

        // ---- Corte clásico ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("corte clasico","corte de pelo","corte normal","corte simple",
                          "solo corte","quiero cortarme","cortarme el pelo"),
            Arrays.asList(
                "El ✂️ *Corte Clásico* tiene un precio de *$1.500* y tarda aproximadamente 30 minutos. Incluye lavado y secado.\n\n¿Querés reservar? Podés hacerlo desde el botón 'Reservar Turno' en el inicio.",
                "El corte clásico con tijera o máquina (a tu elección) cuesta *$1.500*. ¡Es nuestro servicio estrella! ¿Te reservo un turno?"
            )
        ));

        // ---- Corte + Barba ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("corte barba","corte con barba","combo","todo junto",
                          "corte y barba","pelo y barba","cabello y barba"),
            Arrays.asList(
                "El 💈 *Combo Corte + Barba* tiene un precio de *$2.500* y dura unos 50 minutos. Incluye corte completo más delineado de barba con productos premium.\n\n¡El más popular de la casa! 🔥",
                "El combo completo *Corte + Barba* cuesta *$2.500*. Incluye corte + arreglo y delineado de barba. ¿Reservamos?"
            )
        ));

        // ---- Barba ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("barba","arreglar barba","arreglo barba","perfilar","delineado",
                          "barba sola","solo barba","beard"),
            Arrays.asList(
                "El 🧔 *Arreglo de Barba* cuesta *$900* y tarda unos 20 minutos. Incluye perfilado, delineado y arreglo completo.",
                "Para arreglar la barba solamente el precio es *$900*. Rápido y prolijo ✨"
            )
        ));

        // ---- Fade / Difuminado ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("fade","difuminado","degradado","maquina","zero","skin fade",
                          "high fade","low fade","mid fade","corte maquina"),
            Arrays.asList(
                "El 🌀 *Corte Difuminado (Fade)* cuesta *$1.800* y dura unos 35 minutos. Técnica profesional de máquina con transición suave.",
                "El fade/degradado tiene un precio de *$1.800*. ¡Quedás 🔥! ¿Reservamos turno?"
            )
        ));

        // ---- Teñido / Color ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("tenido","tintura","color","coloracion","tinte","tenar","pintarme",
                          "pintura","rubio","oscuro","cambiar color","decoloracion"),
            Arrays.asList(
                "El 🎨 *Teñido Completo* tiene un precio de *$3.500* y dura aproximadamente 90 minutos. Usamos tintes de alta calidad e incluye tratamiento post-color.",
                "Para coloración completa del cabello el precio es *$3.500*. Si te interesa, podés reservar un turno en el sitio 📅"
            )
        ));

        // ---- Balayage / Mechitas ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("balayage","mechitas","mechas","highlights","iluminacion","iluminar",
                          "californianas","babylights","reflejos"),
            Arrays.asList(
                "El ✨ *Balayage / Mechitas* tiene un precio de *$4.500* y puede durar hasta 2 horas. Es una técnica de iluminación artística que queda espectacular 🌟",
                "Las mechitas o balayage cuestan *$4.500*. Incluye coloración parcial + consulta de técnica. ¿Te interesa reservar?"
            )
        ));

        // ---- Corte infantil ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("nino","niño","chico","bebe","bebe","infantil","hijo","pequeño",
                          "menor","pequeño","kids","kid","junior"),
            Arrays.asList(
                "El 👦 *Corte Infantil* (hasta 12 años) tiene un precio de *$1.200* y dura unos 25 minutos. ¡Atendemos a los más chicos con toda la paciencia del mundo!",
                "Para los más pequeños (hasta 12 años) tenemos el Corte Infantil a *$1.200*. ¡Un ambiente divertido para ellos! 🎈"
            )
        ));

        // ---- Tratamiento capilar ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("tratamiento","hidratacion","keratina","mascarilla","nutritivo",
                          "reconstruccion","brillo","cabello seco","puntas","pelo seco","pelo dañado"),
            Arrays.asList(
                "El 💆 *Tratamiento Capilar* cuesta *$2.800* y dura 60 minutos. Hidratación profunda con keratina o mascarillas nutritivas premium. ¡El cabello te queda increíble!",
                "El tratamiento de hidratación profunda tiene un precio de *$2.800*. Usamos productos premium para reconstruir el cabello dañado ✨"
            )
        ));

        // ---- Afeitado ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("afeitado","navaja","afeitar","rasuradora","rasuracion",
                          "toalla caliente","shaving","afeitarse"),
            Arrays.asList(
                "El 🪒 *Afeitado Clásico* con navaja de barbero y toalla caliente cuesta *$1.400* y dura 35 minutos. ¡Una experiencia de barbería clásica como las de antes!",
                "El afeitado artesanal con navaja + aceites post-afeitado es una experiencia única y cuesta *$1.400* ⭐"
            )
        ));

        // ---- Diseño de cejas ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("cejas","depilacion cejas","cejas hombre","eyebrow","diseño cejas"),
            Arrays.asList(
                "El 👁️ *Diseño de Cejas* masculinas cuesta *$700* y dura solo 15 minutos. Depilación y diseño para un look prolijo y moderno.",
                "Las cejas las ejecutamos a *$700*. Rápido, sencillo y marca una diferencia enorme en el look ✌️"
            )
        ));

        // ---- Horarios ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("horario","hora","cuando abren","cuando cierran","a que hora",
                          "dias","lunes","sabado","atienden","trabajan","abierto","cerrado",
                          "que dias","horarios","funcionan","atiende","trabaja"),
            Arrays.asList(
                "⏰ Nuestros horarios de atención:\n\n" +
                "📆 Lunes a Viernes: 9:00 a 20:00 hs\n" +
                "📆 Sábados: 9:00 a 18:00 hs\n" +
                "❌ Domingos: Cerrado\n\n" +
                "Podés reservar tu turno online las 24 horas 💻",
                "Atendemos de lunes a viernes de *9 a 20 hs* y los sábados de *9 a 18 hs*. Los domingos descansamos 😴\n\nReserva online disponible siempre!"
            )
        ));

        // ---- Reservar ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("reservar","turno","cita","agendar","sacar turno","pedir turno",
                          "como reservo","quiero un turno","disponibilidad","libre","disponible"),
            Arrays.asList(
                "¡Reservar es muy fácil! 📱\n\n1️⃣ Click en 'Reservar Turno'\n2️⃣ Elegí el servicio\n3️⃣ Seleccioná fecha y hora\n4️⃣ Completá tus datos\n5️⃣ ¡Listo! Recibís confirmación por WhatsApp\n\n¿Querés empezar ahora?",
                "Para sacar turno click en el botón *'Reservar Turno'* en el inicio. Solo tomás unos minutos y recibís la confirmación por WhatsApp 📲"
            )
        ));

        // ---- WhatsApp ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("whatsapp","wsp","mensaje","confirmacion","confirma","recibo",
                          "aviso","notificacion"),
            Arrays.asList(
                "Sí, al confirmar tu reserva te mandamos un mensaje de WhatsApp con todos los detalles del turno 📲 ¡Es automático!",
                "Al completar la reserva, el sistema te enviará automáticamente un WhatsApp con la fecha, hora y servicio. Muy práctico ✅"
            )
        ));

        // ---- Ubicación ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("donde","ubicacion","direccion","donde estan","local","sucursal",
                          "lugar","como llego","mapa","como los encuentro","domicilio"),
            Arrays.asList(
                "📍 Encontranos en nuestras sucursales en toda la ciudad.\n\nPara ver la dirección exacta, contactanos por WhatsApp o revisá nuestra sección de contacto en el sitio.",
                "Tenemos varias sucursales disponibles. Podés consultarnos la más cercana a tu zona por WhatsApp o el formulario de contacto 📍"
            )
        ));

        // ---- Contacto ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("contac","telefono","llamar","escribir","comunicar","hablar",
                          "redes","instagram","facebook","social","hablarles"),
            Arrays.asList(
                "Podés contactarnos por:\n📲 WhatsApp (disponible en el sitio)\n📸 Instagram: @barberpost\n📘 Facebook: Barber Post\n\n¿En qué más te puedo ayudar?",
                "Estamos en Instagram como *@barberpost* y podés escribirnos por WhatsApp desde el sitio. ¡Te respondemos rápido! ⚡"
            )
        ));

        // ---- Promociones ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("promo","descuento","oferta","especial","2x1","promocion","gratis",
                          "beneficio","cupon","rebaja","mas barato"),
            Arrays.asList(
                "🎉 Seguinos en Instagram @barberpost para no perderte nuestras promos especiales, descuentos por temporada y beneficios para clientes frecuentes.",
                "Tenemos promos y descuentos especiales publicados en nuestras redes. ¡Seguinos en Instagram @barberpost para estar al tanto! 📸"
            )
        ));

        // ---- Personal / Barberos ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("barbero","barberos","estilista","quien atiende","quien corta",
                          "staff","equipo","profesional","peluquero"),
            Arrays.asList(
                "Nuestros barberos son profesionales con años de experiencia en barbería clásica y moderna. Todos están matriculados y actualizados en las últimas tendencias ✂️",
                "Contamos con un equipo de barberos expertos y apasionados. Cada uno especializado en diferentes técnicas. ¡Vas a quedar encantado! 💈"
            )
        ));

        // ---- Cancelar turno ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("cancelar","cancelo","no puedo ir","cambiar turno","reprogramar",
                          "modificar turno","cancelacion","anular"),
            Arrays.asList(
                "Para cancelar o modificar un turno, por favor contactanos con al menos 2 horas de anticipación por WhatsApp o llamando directamente al local. ¡Entendemos que los planes cambian! 📞",
                "Si necesitás cancelar tu reserva, avisanos con anticipación por WhatsApp. Así liberamos el turno para otro cliente. ¡Gracias por avisarnos! 🙏"
            )
        ));

        // ---- Despedidas ----
        CATEGORIAS.add(new Categoria(
            Arrays.asList("chau","adios","hasta luego","bye","gracias","muchas gracias",
                          "ok gracias","listo","de nada","perfecto"),
            Arrays.asList(
                "¡Hasta pronto! 👋 Que te vaya muy bien. Nos vemos en la barbería ✂️",
                "¡Gracias a vos! 😊 Si tenés más preguntas, estoy acá. ¡Hasta la próxima!",
                "¡Chau! Que tengas un excelente día. ¡Reservá tu turno cuando quieras! 💈"
            )
        ));
    }

    // ============================================================
    // Servlet handler
    // ============================================================

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> body = JsonUtil.GSON.fromJson(req.getReader(), Map.class);

            if (body == null || !body.containsKey("mensaje")) {
                res.setStatus(400);
                res.getWriter().write(JsonUtil.error("Campo 'mensaje' requerido"));
                return;
            }

            String mensajeUsuario = body.get("mensaje");
            String respuesta      = procesarMensaje(mensajeUsuario);

            Map<String, String> data = new HashMap<>();
            data.put("respuesta", respuesta);

            res.getWriter().write(JsonUtil.success(data));

        } catch (Exception e) {
            res.setStatus(500);
            res.getWriter().write(JsonUtil.error("Error en chatbot: " + e.getMessage()));
        }
    }

    // ============================================================
    // Motor de respuestas
    // ============================================================

    /**
     * Busca la mejor respuesta para el mensaje del usuario.
     *
     * Algoritmo:
     * 1. Normalizar el mensaje (minúsculas, sin tildes, sin puntuación)
     * 2. Para cada categoría, verificar si algún keyword aparece en el mensaje
     * 3. Retornar una respuesta aleatoria de la categoría que matchee
     * 4. Si no hay coincidencia, retornar respuesta por defecto
     *
     * @param mensaje texto ingresado por el usuario
     * @return respuesta del chatbot
     */
    private String procesarMensaje(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return "No entendí tu consulta. ¿Podés escribirla de otra forma? 🤔";
        }

        String normalizado = normalizar(mensaje);

        // Buscar coincidencias en todas las categorías
        for (Categoria cat : CATEGORIAS) {
            for (String keyword : cat.keywords) {
                if (normalizado.contains(normalizar(keyword))) {
                    // Retornar respuesta aleatoria de esta categoría
                    return cat.respuestas.get(new Random().nextInt(cat.respuestas.size()));
                }
            }
        }

        // Sin coincidencia — respuesta amigable por defecto
        return obtenerRespuestaDefault();
    }

    /**
     * Normaliza el texto para mejorar el matching:
     * - Convierte a minúsculas
     * - Elimina tildes y caracteres especiales del español
     * - Elimina signos de puntuación
     * - Elimina espacios extras
     */
    private String normalizar(String texto) {
        return texto.toLowerCase()
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u").replace("ü", "u")
                .replace("ñ", "n").replace("¿", "").replace("?", "")
                .replace("¡", "").replace("!", "").replace(",", "")
                .replace(".", "").replace(";", "").replace(":", "")
                .trim();
    }

    /** Una de las respuestas por defecto cuando no hay coincidencia */
    private String obtenerRespuestaDefault() {
        List<String> defaults = Arrays.asList(
            "No estoy seguro de entender tu consulta 🤔 Podés preguntarme sobre:\n• Precios y servicios\n• Horarios de atención\n• Cómo reservar un turno\n• Contacto y ubicación",
            "Mmm, no capto bien la pregunta 😅 Pero podés consultarme sobre precios, servicios disponibles, horarios o cómo hacer una reserva.",
            "¿Podés ser un poco más específico? 😊 Estoy aquí para ayudarte con precios, turnos, horarios y todo lo relacionado con Barber Post.",
            "Esa consulta me supera un poco 🙈 Para más información, contactanos directamente por WhatsApp. Pero sí puedo ayudarte con precios, horarios y reservas!"
        );
        return defaults.get(new Random().nextInt(defaults.size()));
    }

    // ============================================================
    // Clase auxiliar
    // ============================================================

    private static class Categoria {
        final List<String> keywords;
        final List<String> respuestas;

        Categoria(List<String> keywords, List<String> respuestas) {
            this.keywords   = keywords;
            this.respuestas = respuestas;
        }
    }
}

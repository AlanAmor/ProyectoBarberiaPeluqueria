package com.barberpost.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * JsonUtil — Utilidades para serialización JSON con Gson
 *
 * Centraliza la instancia de Gson con configuración consistente
 * y provee helpers para respuestas API estándar.
 *
 * Todas las respuestas siguen el esquema:
 *   { "success": true/false, "data": {...}, "message": "..." }
 *
 * @version 1.0.0
 */
public class JsonUtil {

    /** Instancia de Gson compartida con configuración estándar */
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            // Serializa fechas Java 8 (LocalDate, LocalTime, LocalDateTime)
            .registerTypeAdapter(java.time.LocalDate.class,
                (com.google.gson.JsonSerializer<java.time.LocalDate>)
                (src, type, ctx) -> new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalTime.class,
                (com.google.gson.JsonSerializer<java.time.LocalTime>)
                (src, type, ctx) -> new com.google.gson.JsonPrimitive(src.toString().substring(0, 5)))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                (com.google.gson.JsonSerializer<java.time.LocalDateTime>)
                (src, type, ctx) -> new com.google.gson.JsonPrimitive(src.toString()))
            .create();

    private JsonUtil() {}

    // ============================================================
    // Builders de respuestas estandarizadas
    // ============================================================

    /**
     * Construye una respuesta de éxito con datos.
     *
     * @param data el objeto a serializar como "data"
     * @return JSON string: { "success": true, "data": {...} }
     */
    public static String success(Object data) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", data);
        return GSON.toJson(resp);
    }

    /**
     * Construye una respuesta de éxito con datos y mensaje.
     */
    public static String success(Object data, String message) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", data);
        resp.put("message", message);
        return GSON.toJson(resp);
    }

    /**
     * Construye una respuesta de error.
     *
     * @param message descripción del error
     * @return JSON string: { "success": false, "message": "..." }
     */
    public static String error(String message) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("message", message);
        return GSON.toJson(resp);
    }

    /**
     * Serializa cualquier objeto a JSON.
     */
    public static String toJson(Object object) {
        return GSON.toJson(object);
    }
}

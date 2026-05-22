package com.barberpost.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * PasswordUtil — Utilidades para manejo de contraseñas
 *
 * Implementa hashing SHA-256 para almacenar y comparar contraseñas.
 *
 * NOTA DE SEGURIDAD: SHA-256 sin sal (salt) es vulnerable a ataques
 * de diccionario / rainbow tables. Para producción, usar BCrypt o
 * Argon2 con factor de costo adecuado. Esta implementación es
 * suficiente para el prototipo.
 *
 * @version 1.0.0
 */
public class PasswordUtil {

    /** Constructor privado — clase estática, no se instancia */
    private PasswordUtil() {}

    /**
     * Genera el hash SHA-256 de una contraseña en texto plano.
     *
     * @param password contraseña en texto plano
     * @return hash hexadecimal de 64 caracteres
     */
    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest    = md.digest(password.getBytes(StandardCharsets.UTF_8));

            // Convertir bytes a representación hexadecimal
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 está disponible en todos los JRE estándar
            throw new RuntimeException("SHA-256 no disponible en esta JVM", e);
        }
    }

    /**
     * Verifica si una contraseña en texto plano coincide con el hash guardado.
     *
     * @param passwordPlano contraseña ingresada por el usuario
     * @param hashGuardado  hash almacenado en la base de datos
     * @return true si la contraseña es correcta
     */
    public static boolean verificar(String passwordPlano, String hashGuardado) {
        if (passwordPlano == null || hashGuardado == null) return false;
        return hash(passwordPlano).equals(hashGuardado);
    }
}

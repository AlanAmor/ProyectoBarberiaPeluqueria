package com.barberpost.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConnection — Fábrica de conexiones JDBC
 *
 * Gestiona la conexión a la base de datos MySQL.
 * En el prototipo se usa una conexión directa por petición.
 * Para producción, reemplazar por un pool (HikariCP, DBCP2, etc.)
 *
 * CONFIGURACIÓN:
 *   Modificar las constantes DB_URL, DB_USER y DB_PASSWORD
 *   según el entorno de despliegue, o mejor aún, externalizarlas
 *   a un archivo de propiedades (ver README para instrucciones).
 *
 * @version 1.0.0
 */
public class DatabaseConnection {

    // ============================================================
    // Configuración de conexión — AJUSTAR AL ENTORNO
    // ============================================================

    /** URL de conexión JDBC. Incluye timezone de Argentina y charset UTF-8. */
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/barberpost"
            + "?useSSL=false"
            + "&serverTimezone=America/Argentina/Buenos_Aires"
            + "&characterEncoding=UTF-8"
            + "&useUnicode=true"
            + "&allowPublicKeyRetrieval=true";

    /** Usuario de MySQL — cambiar si no es root */
    private static final String DB_USER = "root";

    /**
     * Contraseña de MySQL.
     * IMPORTANTE: en producción, no dejar en texto plano en el código.
     * Usar variables de entorno o un archivo de configuración externo.
     */
    private static final String DB_PASSWORD = "23738686";

    // ============================================================
    // Inicialización del driver
    // ============================================================

    static {
        try {
            // Carga explícita del driver (necesario en algunos Tomcat 9)
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                    "No se encontró el driver MySQL. ¿Agregaste mysql-connector-java al pom.xml? Error: " + e.getMessage()
            );
        }
    }

    /** Constructor privado — clase utilitaria, no se instancia */
    private DatabaseConnection() {}

    // ============================================================
    // API pública
    // ============================================================

    /**
     * Retorna una nueva conexión a la base de datos.
     *
     * Cada DAO llama a este método y cierra la conexión después
     * usando try-with-resources, garantizando liberación de recursos.
     *
     * Ejemplo de uso:
     *   try (Connection conn = DatabaseConnection.getConnection()) {
     *       // ... usar conn
     *   }
     *
     * @return una conexión válida y abierta
     * @throws SQLException si falla la conexión (DB apagada, credenciales erróneas, etc.)
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Verifica si la base de datos está accesible.
     * Útil para el health-check en el setup inicial.
     *
     * @return true si la conexión se pudo establecer
     */
    public static boolean isAvailable() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}

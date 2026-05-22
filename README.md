# ✂ Barber Post

Sistema web de reservas y gestión para barberías.

## Descripción

**Barber Post** es una plataforma que facilita la gestión integral de servicios de barberías. Permite a los clientes reservar turnos online de forma sencilla, mientras que propietarios y empleados cuentan con herramientas para administrar la operación del negocio, consultar agendas y obtener estadísticas.

## Tecnologías Utilizadas

- **Backend**: Java con Servlets
- **Frontend**: HTML5, CSS3, JavaScript
- **Base de Datos**: MySQL
- **Servidor**: Apache Tomcat
- **Build**: Apache Maven
- **Arquitectura**: MVC (Model-View-Controller)

## Funcionalidades

- **Clientes**: Reserva de turnos online con confirmación automática
- **Propietarios**: Gestión completa de turnos, precios y estadísticas
- **Empleados**: Vista de agenda y registro de atenciones
- **Chatbot**: Asistencia automática para consultas
mvn -version        # Debe mostrar 3.6 o superior
mysql --version     # Debe mostrar 8.x
```

---

## Instalación Paso a Paso

### Paso 1 — Configurar la base de datos MySQL

```sql
-- Conectarse a MySQL como root
mysql -u root -p

-- Ejecutar el script de creación
SOURCE /ruta/al/proyecto/database/barberpost.sql;

-- Verificar creación
SHOW DATABASES;
USE barberpost;
SHOW TABLES;
```

### Paso 2 — Configurar la conexión a la base de datos

Editá el archivo `DatabaseConnection.java` en:
```
src/main/java/com/barberpost/dao/DatabaseConnection.java
```

Modificá estas constantes:
```java
private static final String DB_URL  = "jdbc:mysql://localhost:3306/barberpost?...";
private static final String DB_USER     = "root";       // tu usuario MySQL
private static final String DB_PASSWORD = "tu_password"; // tu contraseña MySQL
```

### Paso 3 — Compilar el proyecto

```bash
# Desde la raíz del proyecto (donde está pom.xml)
cd C:\Users\pc\Desktop\Proyectos\SoftwareBarberias

# Compilar y empaquetar
mvn clean package

# El archivo WAR se genera en:
# target/barberpost.war
```

### Paso 4 — Desplegar en Tomcat

**Opción A — Maven Tomcat Plugin (recomendado para desarrollo):**
```bash
mvn tomcat7:run
# La aplicación quedará disponible en:
# http://localhost:8080/barberpost
```

**Opción B — Tomcat standalone:**
1. Copiar `target/barberpost.war` a la carpeta `webapps/` de Tomcat
2. Iniciar Tomcat
3. Acceder a `http://localhost:8080/barberpost`

### Paso 5 — Crear usuarios iniciales

En el navegador, accedé una sola vez a:
```
http://localhost:8080/barberpost/api/setup/init
```

Esto crea los usuarios por defecto:
- **Dueño:** usuario `admin` / contraseña `admin123`
- **Empleado:** usuario `empleado1` / contraseña `emp123`

> ⚠️ **Importante:** Cambiar estas contraseñas en producción. Ver sección [Seguridad](#seguridad).

### Paso 6 — Acceder al sistema

| Rol | URL |
|-----|-----|
| Landing page | `http://localhost:8080/barberpost/index.html` |
| Reservar turno | `http://localhost:8080/barberpost/reservar.html` |
| Login admin/empleado | `http://localhost:8080/barberpost/login.html` |

---

## Estructura del Proyecto

```
SoftwareBarberias/
│
├── pom.xml                          # Configuración Maven
├── README.md                        # Este archivo
│
├── database/
│   └── barberpost.sql               # Schema + datos iniciales
│
└── src/main/
    ├── java/com/barberpost/
    │   ├── model/                   # Capa Modelo (POJOs)
    │   │   ├── Cliente.java
    │   │   ├── Servicio.java
    │   │   ├── Turno.java
    │   │   └── Usuario.java
    │   │
    │   ├── dao/                     # Acceso a datos (JDBC)
    │   │   ├── DatabaseConnection.java
    │   │   ├── ClienteDAO.java
    │   │   ├── ServicioDAO.java
    │   │   ├── TurnoDAO.java
    │   │   └── UsuarioDAO.java
    │   │
    │   ├── servlet/                 # Capa Controlador (REST)
    │   │   ├── AuthServlet.java
    │   │   ├── ChatbotServlet.java
    │   │   ├── DashboardServlet.java
    │   │   ├── ReservaServlet.java
    │   │   ├── ServiciosServlet.java
    │   │   ├── SetupServlet.java
    │   │   └── TurnosServlet.java
    │   │
    │   ├── filter/                  # Filtros HTTP
    │   │   ├── AuthFilter.java      # Protección de rutas
    │   │   └── CorsFilter.java      # Headers CORS
    │   │
    │   └── util/                    # Utilidades
    │       ├── JsonUtil.java        # Respuestas JSON estándar
    │       └── PasswordUtil.java    # Hashing de contraseñas
    │
    └── webapp/                      # Capa Vista (Frontend)
        ├── WEB-INF/web.xml          # Configuración Servlet
        │
        ├── index.html               # Landing page con chatbot
        ├── reservar.html            # Wizard de reserva (5 pasos)
        ├── login.html               # Login dueño/empleado
        ├── dashboard-owner.html     # Panel dueño
        ├── dashboard-employee.html  # Panel empleado
        │
        ├── css/
        │   ├── style.css            # Estilos landing + reserva + login
        │   └── dashboard.css        # Estilos dashboards
        │
        └── js/
            ├── main.js              # Landing page logic
            ├── reserva.js           # Wizard de reserva
            ├── chatbot.js           # Motor del chatbot
            ├── dashboard-owner.js   # Panel dueño logic
            └── dashboard-employee.js# Panel empleado logic
```

---

## Arquitectura MVC

```
CLIENTE (Browser)
      │
      ▼  HTML / CSS / JS (Bootstrap 5)
  Vista (webapp/)
      │  AJAX fetch() API calls
      ▼
 Controlador (servlet/)
      │  Java HttpServlet → lee request, responde JSON
      ▼
   Modelo (model/ + dao/)
      │  JDBC queries
      ▼
  Base de datos (MySQL 8)
```

### Flujo de una reserva:

```
1. Cliente completa wizard en reservar.html
2. JS hace POST /api/reservas con datos JSON
3. ReservaServlet valida y procesa la solicitud
4. ClienteDAO.guardarOActualizar() → upsert por DNI
5. TurnoDAO.crear() → inserta el turno
6. Servlet genera URL de WhatsApp con el mensaje
7. Servidor responde JSON { success, turnoId, whatsappUrl }
8. JS muestra pantalla de éxito + abre WhatsApp
```

---

## Endpoints de la API

Todos los endpoints tienen prefijo `/api/`. Respuesta estándar:
```json
{
  "success": true,
  "data": { ... },
  "message": "Operación exitosa"
}
```

### Endpoints públicos (sin autenticación)

| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/api/servicios` | Lista servicios activos |
| GET | `/api/turnos/disponibles?fecha=YYYY-MM-DD&servicioId=N` | Horarios disponibles |
| POST | `/api/reservas` | Crear nueva reserva |
| POST | `/api/auth/login` | Iniciar sesión |
| POST | `/api/chatbot` | Consulta al chatbot |
| GET | `/api/setup/init` | Crear usuarios iniciales (una sola vez) |

### Endpoints protegidos (requieren sesión activa)

| Método | URL | Descripción | Rol |
|--------|-----|-------------|-----|
| GET | `/api/reservas/all` | Todos los turnos | Ambos |
| PUT | `/api/reservas/{id}` | Cambiar estado de turno | Ambos |
| GET | `/api/dashboard/resumen` | Stats del día | Ambos |
| GET | `/api/dashboard/clientes-top` | Clientes frecuentes | Dueño |
| GET | `/api/dashboard/facturacion?anio=N` | Facturación mensual | Dueño |
| GET | `/api/servicios/todos` | Todos los servicios | Dueño |
| PUT | `/api/servicios/{id}` | Actualizar precio | Dueño |
| POST | `/api/auth/logout` | Cerrar sesión | Ambos |

### Ejemplo — POST /api/reservas

**Request:**
```json
{
  "servicioId": 1,
  "fecha": "2025-02-15",
  "hora": "10:30",
  "nombre": "Carlos",
  "apellido": "García",
  "dni": "32541990",
  "telefono": "1155443322",
  "email": "carlos@email.com"
}
```

**Response exitosa:**
```json
{
  "success": true,
  "data": {
    "turnoId": 42,
    "whatsappUrl": "https://wa.me/5491100000000?text=...",
    "fecha": "2025-02-15",
    "hora": "10:30",
    "servicio": "Corte Clásico",
    "precio": 1500
  }
}
```

---

## Panel del Dueño

### Acceso
- URL: `login.html` → credenciales de dueño → redirige a `dashboard-owner.html`
- Credenciales por defecto: `admin` / `admin123`

### Secciones

**Resumen del Día:**
- Cards de estado: turnos hoy, pendientes, total clientes, ingresos del mes
- Tabla de próximos turnos del día

**Gestión de Turnos:**
- Tabla completa de todos los turnos
- Filtros por: fecha desde/hasta y estado
- Acción: cambiar estado de cualquier turno (pendiente → confirmado → completado / cancelado)

**Clientes Frecuentes:**
- Listado ordenado por cantidad de visitas (basado en repetición de DNI)
- Datos: nombre, DNI, teléfono, total de visitas

**Estadísticas:**
- Gráfico de barras: facturación mensual del año actual
- Gráfico de dona: distribución de servicios más solicitados

**Servicios y Precios:**
- Tabla con todos los servicios
- Campo editable de precio con botón "Guardar" por fila
- Cambios se guardan en tiempo real en la base de datos

---

## Panel del Empleado

### Acceso
- URL: `login.html` → credenciales de empleado → redirige a `dashboard-employee.html`
- Credenciales por defecto: `empleado1` / `emp123`

### Secciones

**Mi Agenda (hoy):**
- Cards resumen: pendientes, confirmados, completados
- Lista cronológica de turnos del día
- Botón "Atender" por turno → modal para:
  - Agregar notas/observaciones
  - Marcar como "Completado"
  - Marcar como "Cancelado"

**Semana:**
- Tabla con todos los turnos de la semana en curso
- Misma acción de atender disponible

**Mis Estadísticas:**
- Contador de atenciones: hoy / semana / mes
- Gráfico de barras con servicios más atendidos del mes

---

## Proceso de Reserva del Cliente

### Wizard de 5 pasos en `reservar.html`

```
Paso 1: Elegir servicio
  → Tarjetas con nombre, descripción, precio y duración
  → Se muestra cotizador con precio seleccionado

Paso 2: Elegir fecha
  → Calendario Flatpickr
  → Días habilitados: Lunes a Sábado, máximo 60 días adelante

Paso 3: Elegir horario
  → Grilla de slots disponibles (9:00 a 20:00, cada 30 min)
  → Los slots ocupados aparecen tachados/deshabilitados
  → Solo muestra slots donde el cliente puede entrar según duración del servicio

Paso 4: Datos personales
  → Nombre, Apellido, DNI, Teléfono (obligatorios), Email (opcional)
  → Validación en tiempo real de formato DNI y teléfono

Paso 5: Confirmación
  → Resumen completo de la reserva
  → Botón "Confirmar Reserva" → POST al servidor

Paso 6 (éxito):
  → Número de turno asignado
  → Botón verde de WhatsApp → abre chat con mensaje pre-llenado
```

---

## Chatbot

El chatbot es **puramente basado en reglas** (sin IA externa ni APIs de terceros).

### Cómo funciona

1. Usuario escribe un mensaje
2. `chatbot.js` normaliza el texto (minúsculas, elimina acentos y signos)
3. Busca palabras clave en categorías predefinidas
4. Si encuentra coincidencia → devuelve respuesta de esa categoría (aleatoria si hay varias)
5. Si no hay coincidencia → respuesta de fallback genérica

### Categorías disponibles

| Categoría | Palabras clave ejemplo |
|-----------|----------------------|
| Saludo | hola, buenas, hey |
| Precios | precio, cuánto cuesta, tarifa |
| Corte Clásico | corte de pelo, corte clasico |
| Barba | barba, afeitar, delinear |
| Teñido | teñido, coloración, mechitas |
| Infantil | niño, nene, hijo |
| Horarios | horario, cuando abren, días |
| Ubicación | dónde están, dirección |
| Reservar | turno, reservar, agendar |
| Pagos | pago, efectivo, tarjeta |
| Cancelar | cancelar, modificar turno |
| WhatsApp | whatsapp, contactarlos |
| Promociones | descuento, promo, oferta |

### Cómo agregar respuestas

En `js/chatbot.js`, dentro del array `CHAT_CATEGORIES`, agregar o modificar entradas:

```javascript
{
    name: 'nueva_categoria',
    keywords: ['palabra1', 'palabra2', 'frase de ejemplo'],
    responses: [
        'Respuesta 1 para esta categoría',
        'Respuesta 2 alternativa (se elige aleatoriamente)',
    ]
}
```

---

## Integración con WhatsApp

El sistema usa la API gratuita **WhatsApp Click-to-Chat** (sin costo, sin API key):

```
https://wa.me/[número]?text=[mensaje_codificado]
```

### Configurar el número de WhatsApp

En `src/main/java/com/barberpost/servlet/ReservaServlet.java`:

```java
// Cambiar por el número de la barbería (sin + ni espacios, con código de país)
private static final String WHATSAPP_NUMBER = "5491130000000";
// Argentina (+54) + código área (11) + número (30000000)
```

### Formato del número

Para Argentina:
- `+54 9 11 XXXX-XXXX` → se escribe como `5491XXXXXXXX`
- El `9` después de `54` es necesario para celulares argentinos

### Mensaje automático que recibe el cliente

Cuando confirma la reserva, WhatsApp se abre con este mensaje pre-llenado:
```
¡Hola Barber Post! ✂️
Confirmo mi turno:
👤 Carlos García
✂️ Corte Clásico
📅 Lunes 15 de febrero a las 10:30
Muchas gracias!
```

El cliente solo necesita presionar **Enviar** en WhatsApp para confirmar.

---

## Mantenimiento y Administración

### Agregar un nuevo servicio

1. Conectarse a MySQL y ejecutar:
```sql
USE barberpost;
INSERT INTO servicios (nombre, descripcion, precio, duracion_minutos) 
VALUES ('Nuevo Servicio', 'Descripción del servicio', 2000.00, 40);
```

2. El servicio aparecerá automáticamente en la página de reservas y el dashboard.

### Cambiar precio de un servicio

**Desde el dashboard del dueño:**
- Ir a "Servicios y Precios" → editar el precio → "Guardar"

**Directamente en la base de datos:**
```sql
UPDATE servicios SET precio = 1800.00 WHERE nombre = 'Corte Clásico';
```

### Agregar un nuevo empleado

```sql
-- Primero generar el hash de contraseña usando el endpoint de setup,
-- o ejecutar en Java: PasswordUtil.hash("nueva_contraseña")
INSERT INTO usuarios (username, password_hash, nombre, rol)
VALUES ('empleado2', 'HASH_AQUI', 'María López', 'employee');
```

O usar el endpoint de setup con parámetros adicionales (ver SetupServlet.java).

### Ver estadísticas directamente en SQL

```sql
-- Turnos del día actual
SELECT t.hora, c.nombre, c.apellido, s.nombre as servicio, t.estado
FROM turnos t
JOIN clientes c ON c.id = t.cliente_id
JOIN servicios s ON s.id = t.servicio_id
WHERE t.fecha = CURDATE()
ORDER BY t.hora;

-- Facturación del mes actual
SELECT SUM(s.precio) as total, COUNT(*) as cantidad
FROM turnos t
JOIN servicios s ON s.id = t.servicio_id
WHERE YEAR(t.fecha) = YEAR(CURDATE())
AND MONTH(t.fecha) = MONTH(CURDATE())
AND t.estado = 'completado';

-- Top 10 clientes más frecuentes
SELECT c.nombre, c.apellido, c.dni, COUNT(t.id) as visitas
FROM clientes c
JOIN turnos t ON t.cliente_id = c.id
WHERE t.estado = 'completado'
GROUP BY c.id
ORDER BY visitas DESC
LIMIT 10;
```

### Backup de la base de datos

```bash
# Exportar backup completo
mysqldump -u root -p barberpost > backup_$(date +%Y%m%d).sql

# Restaurar backup
mysql -u root -p barberpost < backup_20250115.sql
```

---

## Seguridad

> ⚠️ Las siguientes medidas son **obligatorias** antes de usar en producción.

### 1. Cambiar contraseñas por defecto

Después del primer login, cambiar las contraseñas de `admin` y `empleado1` desde la base de datos:

```sql
-- Generar hash con PasswordUtil.java o endpoint /api/setup/changePassword
UPDATE usuarios SET password_hash = 'NUEVO_HASH' WHERE username = 'admin';
```

### 2. Migrar a BCrypt (recomendado)

El sistema actual usa SHA-256 para hashear contraseñas. En producción, usar BCrypt:

1. Agregar dependencia en `pom.xml`:
```xml
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>
```

2. Modificar `PasswordUtil.java`:
```java
import org.mindrot.jbcrypt.BCrypt;

public static String hash(String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt(12));
}

public static boolean verificar(String plain, String hash) {
    return BCrypt.checkpw(plain, hash);
}
```

### 3. Configurar HTTPS

En producción, usar siempre HTTPS. Configurar en `server.xml` de Tomcat o poner un proxy inverso con SSL (nginx/caddy).

### 4. Proteger el endpoint de setup

Después de la primera instalación, deshabilitar el endpoint `/api/setup/init`:
- Comentar o eliminar el mapeo en `web.xml`
- O agregar una validación para que solo funcione cuando no hay usuarios

### 5. Variables de entorno para credenciales DB

En lugar de hardcodear usuario/contraseña en DatabaseConnection.java, usar variables de entorno:

```java
String dbUrl  = System.getenv("DB_URL");
String dbUser = System.getenv("DB_USER");
String dbPass = System.getenv("DB_PASSWORD");
```

---

## Problemas Frecuentes

### "No se pueden cargar los servicios"

**Causa:** El backend no está corriendo o la URL base es incorrecta.
**Solución:** Verificar que Tomcat está corriendo y que el contexto es `/barberpost`. La aplicación funciona en modo demo offline si la API no responde.

### "Error al conectar a la base de datos"

**Causa:** Credenciales incorrectas o MySQL no está corriendo.
**Solución:**
```bash
# Verificar que MySQL corre
net start MySQL80  # Windows
# Verificar credenciales en DatabaseConnection.java
```

### "Los slots de horarios no cargan"

**Causa:** El parámetro `fecha` tiene formato incorrecto.
**Solución:** La fecha debe ser `YYYY-MM-DD`. Verificar que Flatpickr envía el formato correcto.

### "Login no funciona / redirige a login"

**Causa:** Los usuarios no fueron creados en la DB.
**Solución:** Acceder a `http://localhost:8080/barberpost/api/setup/init` para crear usuarios iniciales.

### "El gráfico de estadísticas no aparece"

**Causa:** Chart.js no se cargó (requiere conexión a internet para CDN).
**Solución:** Si no hay internet, descargar Chart.js localmente y actualizar el `<script>` en el HTML.

### CORS errors en el navegador

**Causa:** Peticiones desde un origen distinto al servidor.
**Solución:** El `CorsFilter.java` ya maneja CORS. Si persiste, verificar el mapeo en `web.xml`.

---

## Tecnologías Utilizadas

### Backend
| Tecnología | Versión | Propósito |
|-----------|---------|-----------|
| Java | 11+ | Lenguaje del backend |
| Apache Tomcat | 9.0 | Servidor de aplicaciones |
| Java Servlets | 4.0.1 | Controladores REST |
| MySQL | 8.0 | Base de datos |
| JDBC | nativo | Conexión a MySQL |
| Gson | 2.10.1 | Serialización JSON |
| Maven | 3.6+ | Gestión de dependencias y build |

### Frontend
| Tecnología | Versión | Propósito |
|-----------|---------|-----------|
| Bootstrap | 5.3.2 | Framework CSS responsive |
| Font Awesome | 6.5.0 | Íconos |
| Google Fonts | — | Playfair Display + Poppins |
| AOS | 2.3.1 | Animaciones al scroll |
| Flatpickr | latest | Selector de fechas |
| Chart.js | 4.4.0 | Gráficos del dashboard |
| WhatsApp Click-to-Chat | — | Confirmación por WA (gratis) |

---

## Créditos

**Sistema desarrollado para Barber Post** — Sistema de reservas para cadenas de barberías.

Imágenes de hero: [Unsplash.com](https://unsplash.com) — libres de uso comercial.

---

*Última actualización: 2025*

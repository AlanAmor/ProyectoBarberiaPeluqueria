-- ============================================================
--  BARBER POST — Script completo de base de datos
--  Sistema de Reservas para Cadena de Barberías
-- ============================================================
--  Autor   : Barber Post Dev Team
--  Versión : 1.0.0
--  Motor   : MySQL 8.0+
--  Charset : utf8mb4 (soporte completo de emojis y tildes)
-- ============================================================
--  USO:
--    mysql -u root -p < barberpost.sql
-- ============================================================

-- Crear y seleccionar la base de datos
CREATE DATABASE IF NOT EXISTS barberpost
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE barberpost;

-- Eliminamos tablas si existen (orden inverso por FK)
DROP TABLE IF EXISTS turnos;
DROP TABLE IF EXISTS clientes;
DROP TABLE IF EXISTS servicios;
DROP TABLE IF EXISTS usuarios;
DROP TABLE IF EXISTS horarios;

-- ============================================================
-- TABLA: servicios
-- Catálogo de servicios que ofrece la barbería con precio y
-- duración. El precio puede editarse desde el dashboard del
-- dueño. El flag 'activo' permite desactivar sin borrar.
-- ============================================================
CREATE TABLE servicios (
    id               INT            NOT NULL AUTO_INCREMENT,
    nombre           VARCHAR(100)   NOT NULL COMMENT 'Nombre visible del servicio',
    descripcion      TEXT               NULL COMMENT 'Descripción detallada',
    precio           DECIMAL(10,2)  NOT NULL COMMENT 'Precio en pesos argentinos',
    duracion_minutos INT            NOT NULL DEFAULT 30 COMMENT 'Duración estimada en minutos',
    activo           TINYINT(1)     NOT NULL DEFAULT 1  COMMENT '1=visible, 0=oculto',
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  COMMENT='Servicios disponibles en la barbería';

-- ============================================================
-- TABLA: clientes
-- Datos personales de los clientes. El DNI es único y sirve
-- para identificar clientes recurrentes en el dashboard.
-- ============================================================
CREATE TABLE clientes (
    id         INT          NOT NULL AUTO_INCREMENT,
    nombre     VARCHAR(100) NOT NULL COMMENT 'Nombre del cliente',
    apellido   VARCHAR(100) NOT NULL COMMENT 'Apellido del cliente',
    dni        VARCHAR(20)  NOT NULL COMMENT 'DNI — identificador único de persona',
    telefono   VARCHAR(20)  NOT NULL COMMENT 'Teléfono con código de área (para WhatsApp)',
    email      VARCHAR(150)     NULL COMMENT 'Email opcional',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dni     (dni),
    INDEX      idx_apellido (apellido),
    INDEX      idx_telefono (telefono)
) ENGINE=InnoDB
  COMMENT='Datos personales de los clientes registrados';

-- ============================================================
-- TABLA: turnos
-- Reservas del sistema. Relaciona cliente + servicio + fecha/hora.
-- Estados: pendiente → confirmado → completado / cancelado
-- ============================================================
CREATE TABLE turnos (
    id          INT          NOT NULL AUTO_INCREMENT,
    cliente_id  INT          NOT NULL COMMENT 'FK → clientes.id',
    servicio_id INT          NOT NULL COMMENT 'FK → servicios.id',
    fecha       DATE         NOT NULL COMMENT 'Fecha del turno',
    hora        TIME         NOT NULL COMMENT 'Hora de inicio del turno',
    estado      ENUM('pendiente','confirmado','completado','cancelado')
                             NOT NULL DEFAULT 'pendiente',
    precio_cobrado DECIMAL(10,2) NULL COMMENT 'Precio al momento de la reserva (snapshot)',
    notas       TEXT             NULL COMMENT 'Observaciones del empleado',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY fk_turno_cliente  (cliente_id)  REFERENCES clientes(id)  ON DELETE CASCADE,
    FOREIGN KEY fk_turno_servicio (servicio_id) REFERENCES servicios(id) ON DELETE RESTRICT,
    INDEX idx_fecha        (fecha),
    INDEX idx_fecha_hora   (fecha, hora),
    INDEX idx_estado       (estado),
    INDEX idx_cliente_fecha (cliente_id, fecha)
) ENGINE=InnoDB
  COMMENT='Reservas y turnos del sistema';

-- ============================================================
-- TABLA: usuarios
-- Acceso al dashboard. Rol 'owner' tiene permisos completos.
-- Contraseña almacenada como SHA-256 hex (64 chars).
-- NOTA SEGURIDAD: En producción migrar a BCrypt.
-- ============================================================
CREATE TABLE usuarios (
    id            INT          NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL UNIQUE COMMENT 'Usuario para login',
    password_hash VARCHAR(64)  NOT NULL COMMENT 'SHA-256 de la contraseña',
    nombre        VARCHAR(100) NOT NULL COMMENT 'Nombre completo visible',
    rol           ENUM('owner','employee') NOT NULL COMMENT 'Nivel de acceso',
    activo        TINYINT(1)   NOT NULL DEFAULT 1,
    ultimo_acceso TIMESTAMP        NULL COMMENT 'Última sesión iniciada',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_username (username)
) ENGINE=InnoDB
  COMMENT='Usuarios administrativos del sistema';

-- ============================================================
-- TABLA: horarios
-- Horario de atención por día de la semana.
-- dia_semana: 1=Lunes … 7=Domingo
-- activo=0 significa que ese día la barbería está cerrada.
-- ============================================================
CREATE TABLE horarios (
    id            INT     NOT NULL AUTO_INCREMENT,
    dia_semana    TINYINT NOT NULL COMMENT '1=Lun, 2=Mar, 3=Mié, 4=Jue, 5=Vie, 6=Sáb, 7=Dom',
    hora_apertura TIME    NOT NULL COMMENT 'Hora de apertura',
    hora_cierre   TIME    NOT NULL COMMENT 'Hora de cierre (último turno antes de esta hora)',
    activo        TINYINT(1) NOT NULL DEFAULT 1 COMMENT '0=cerrado ese día',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dia (dia_semana)
) ENGINE=InnoDB
  COMMENT='Horarios de atención por día de la semana';


-- ============================================================
-- DATOS INICIALES — Servicios
-- ============================================================
INSERT INTO servicios (nombre, descripcion, precio, duracion_minutos) VALUES
('Corte Clásico',       'Corte de cabello tradicional con tijera o máquina a elección del cliente. Incluye lavado y secado.',  1500.00,  30),
('Corte + Barba',       'Combo premium: corte de cabello completo más arreglo y delineado de barba con productos de primera.',   2500.00,  50),
('Arreglo de Barba',    'Perfilado, delineado y arreglo completo de barba. Ideal para mantener el look entre cortes.',           900.00,   20),
('Corte Difuminado',    'Fade o degradado profesional. Técnica de máquina con transición suave entre largos.',                  1800.00,  35),
('Teñido Completo',     'Coloración completa del cabello con tintes de alta calidad. Incluye tratamiento post-color.',          3500.00,  90),
('Balayage / Mechitas', 'Técnica de iluminación natural (balayage) o mechitas tradicionales. Coloración parcial artística.',   4500.00, 120),
('Corte Infantil',      'Corte de cabello para niños hasta 12 años. Atención especial con paciencia y juegos incluidos.',      1200.00,  25),
('Tratamiento Capilar', 'Hidratación profunda y reconstrucción capilar con keratina o mascarillas nutritivas premium.',        2800.00,  60),
('Afeitado Clásico',    'Afeitado artesanal con navaja de barbero, toalla caliente, espuma y aceites post-afeitado.',          1400.00,  35),
('Diseño de Cejas',     'Depilación, diseño y definición de cejas masculinas para un look prolijo y moderno.',                  700.00,  15);

-- ============================================================
-- DATOS INICIALES — Horarios
-- ============================================================
INSERT INTO horarios (dia_semana, hora_apertura, hora_cierre, activo) VALUES
(1, '09:00:00', '20:00:00', 1),  -- Lunes
(2, '09:00:00', '20:00:00', 1),  -- Martes
(3, '09:00:00', '20:00:00', 1),  -- Miércoles
(4, '09:00:00', '20:00:00', 1),  -- Jueves
(5, '09:00:00', '20:00:00', 1),  -- Viernes
(6, '09:00:00', '18:00:00', 1),  -- Sábado (horario reducido)
(7, '00:00:00', '00:00:00', 0);  -- Domingo (cerrado)

-- ============================================================
-- DATOS INICIALES — Usuarios del sistema
--
-- Las contraseñas son hash SHA-256:
--   admin     → admin123
--   empleado1 → emp123
--
-- Para regenerar hashes usar: /api/setup/init (ver README)
-- ============================================================
INSERT INTO usuarios (username, password_hash, nombre, rol) VALUES
('admin',     '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Administrador',  'owner'),
('empleado1', 'e03d3ec8d5035f8721f5dc64546e59ed790dbcb3b7b598fe57057ccd7b683b00', 'Juan García',    'employee');

-- Nota: Si los hashes no coinciden, ejecutar:
--   GET http://localhost:8080/barberpost/api/setup/init
-- que regenerará los usuarios con los hashes correctos.

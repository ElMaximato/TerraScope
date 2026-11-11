-- Datos propios de cada usuario. Todo se borra en cascada si se elimina el usuario.

-- Propiedades registradas a mano (datos aportados por el usuario).
CREATE TABLE IF NOT EXISTS properties (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id       INT UNSIGNED NOT NULL,
  name          VARCHAR(120) NOT NULL,
  property_type ENUM('land','commercial','residential','other') NOT NULL DEFAULT 'other',
  address       VARCHAR(255) NULL,
  location      POINT NOT NULL SRID 4326,
  area_value    DECIMAL(14,2) NULL,
  area_unit     ENUM('sqft','acre','m2') NULL,
  asking_price  DECIMAL(14,2) NULL,
  rent_price    DECIMAL(12,2) NULL,
  notes         TEXT NULL,
  source_note   VARCHAR(255) NULL,
  review_status ENUM('pending','verified') NOT NULL DEFAULT 'pending',
  zone_id       INT UNSIGNED NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  SPATIAL INDEX sp_properties_location (location),
  INDEX idx_properties_user (user_id),
  CONSTRAINT fk_properties_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_properties_zone FOREIGN KEY (zone_id) REFERENCES zones(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Zonas guardadas (Watchlist).
CREATE TABLE IF NOT EXISTS watchlist_items (
  id         INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id    INT UNSIGNED NOT NULL,
  zone_id    INT UNSIGNED NOT NULL,
  note       VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_watchlist_user_zone (user_id, zone_id),
  CONSTRAINT fk_watchlist_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_watchlist_zone FOREIGN KEY (zone_id) REFERENCES zones(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sesiones de exploración del reloj. El id lo genera el cliente (UUID) para que
-- reenviar la misma sesión no cree duplicados (sincronización idempotente).
CREATE TABLE IF NOT EXISTS field_sessions (
  id         CHAR(36) NOT NULL PRIMARY KEY,
  user_id    INT UNSIGNED NOT NULL,
  started_at DATETIME(3) NOT NULL,
  ended_at   DATETIME(3) NULL,
  device     VARCHAR(40) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_sessions_user (user_id),
  CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Puntos registrados durante un recorrido (id UUID del cliente, idempotente).
CREATE TABLE IF NOT EXISTS field_observations (
  id          CHAR(36) NOT NULL PRIMARY KEY,
  user_id     INT UNSIGNED NOT NULL,
  session_id  CHAR(36) NULL,
  tag         ENUM('land_potential','commercial_space','construction','competitor',
                   'infrastructure','road_access','observed_risk','needs_research') NOT NULL,
  note        VARCHAR(500) NULL,
  location    POINT NOT NULL SRID 4326,
  observed_at DATETIME(3) NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  SPATIAL INDEX sp_observations_location (location),
  INDEX idx_observations_user (user_id),
  CONSTRAINT fk_observations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_observations_session FOREIGN KEY (session_id) REFERENCES field_sessions(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Fuentes, zonas y lugares (equivale al antiguo schema.sql; IF NOT EXISTS lo hace seguro de repetir).
CREATE TABLE IF NOT EXISTS data_sources (
  id               INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name             VARCHAR(120) NOT NULL,
  organization     VARCHAR(120) NOT NULL,
  url              VARCHAR(500) NULL,
  license          VARCHAR(200) NULL,
  reference_period VARCHAR(40)  NULL,
  retrieved_at     DATETIME     NULL,
  data_type        ENUM('official','estimate','derived','user') NOT NULL,
  notes            TEXT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS zones (
  id        INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  code      VARCHAR(40)  NOT NULL UNIQUE,
  name      VARCHAR(120) NOT NULL,
  level     ENUM('place','tract','block_group') NOT NULL,
  boundary  GEOMETRY NOT NULL SRID 4326,
  source_id INT UNSIGNED NULL,
  SPATIAL INDEX sp_zones_boundary (boundary),
  CONSTRAINT fk_zones_source FOREIGN KEY (source_id) REFERENCES data_sources(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS places (
  id        INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  osm_id    BIGINT NULL,
  name      VARCHAR(160) NULL,
  category  VARCHAR(60)  NULL,
  location  POINT NOT NULL SRID 4326,
  source_id INT UNSIGNED NULL,
  SPATIAL INDEX sp_places_location (location),
  CONSTRAINT fk_places_source FOREIGN KEY (source_id) REFERENCES data_sources(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

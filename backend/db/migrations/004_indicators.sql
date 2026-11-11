-- Indicadores territoriales y sociodemográficos por zona (Etapa 4).
-- Permite guardar métricas cuantitativas trazables con fuente, año y margen de error.

CREATE TABLE IF NOT EXISTS indicators (
  id          INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  code        VARCHAR(50)  NOT NULL UNIQUE,
  name        VARCHAR(120) NOT NULL,
  category    VARCHAR(50)  NOT NULL,
  unit        VARCHAR(30)  NULL,
  description TEXT         NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Definiciones iniciales de indicadores del MVP
INSERT INTO indicators (code, name, category, unit, description) VALUES
  ('population', 'Población total', 'demografia', 'personas', 'Población total estimada'),
  ('households', 'Total de hogares', 'demografia', 'hogares', 'Número total de hogares particulares habitados'),
  ('median_income', 'Ingreso mediano del hogar', 'economia', 'USD', 'Ingreso familiar mediano en los últimos 12 meses'),
  ('median_age', 'Edad mediana', 'demografia', 'años', 'Edad mediana de la población residente'),
  ('population_density', 'Densidad poblacional', 'territorio', 'hab/km²', 'Habitantes por kilómetro cuadrado de superficie terrestre')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

CREATE TABLE IF NOT EXISTS indicator_values (
  id               INT UNSIGNED   NOT NULL AUTO_INCREMENT PRIMARY KEY,
  zone_id          INT UNSIGNED   NOT NULL,
  indicator_id     INT UNSIGNED   NOT NULL,
  value            DECIMAL(14, 4) NOT NULL,
  margin_of_error  DECIMAL(14, 4) NULL,
  source_id        INT UNSIGNED   NOT NULL,
  reference_period VARCHAR(40)    NOT NULL,
  created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_indval_zone FOREIGN KEY (zone_id) REFERENCES zones(id) ON DELETE CASCADE,
  CONSTRAINT fk_indval_ind FOREIGN KEY (indicator_id) REFERENCES indicators(id) ON DELETE CASCADE,
  CONSTRAINT fk_indval_source FOREIGN KEY (source_id) REFERENCES data_sources(id),
  UNIQUE KEY uq_zone_indicator_period (zone_id, indicator_id, reference_period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Porcentaje de superposición territorial del tract con el place de Somerton.
-- Esta migración solo define el indicador; los valores se cargan por importador.
INSERT INTO indicators (code, name, category, unit, description)
VALUES (
  'overlap_pct',
  'Porcentaje de traslape',
  'territorio',
  '%',
  'Porcentaje de un tract que cae dentro del place de Somerton'
)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  category = VALUES(category),
  unit = VALUES(unit),
  description = VALUES(description);

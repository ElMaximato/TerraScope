// Revisa la sintaxis de todos los .js del backend sin ejecutarlos ni instalar nada.
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), '..');
const files = [];
(function walk(dir) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (e.name === 'node_modules') continue;
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p);
    else if (e.name.endsWith('.js')) files.push(p);
  }
})(root);

let bad = 0;
for (const f of files) {
  try { execFileSync(process.execPath, ['--check', f], { stdio: 'pipe' }); }
  catch (err) { bad++; console.error(`FALLA ${path.relative(root, f)}\n${err.stderr}`); }
}
console.log(bad ? `${bad} archivo(s) con errores` : `Sintaxis correcta en ${files.length} archivos`);
process.exitCode = bad ? 1 : 0;

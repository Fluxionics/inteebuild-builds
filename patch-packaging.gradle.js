const fs = require('fs');
const p = 'android/app/build.gradle';
if (!fs.existsSync(p)) { console.log('build.gradle no existe, packaging omitido'); process.exit(0); }
let src = fs.readFileSync(p, 'utf8');
if (src.includes('META-INF/DISCLAIMER')) { console.log('packaging ya excluye DISCLAIMER'); process.exit(0); }
const block = '\n    packaging {\n        resources {\n            excludes += "META-INF/DISCLAIMER"\n        }\n    }\n';
src = src.replace(/android\s*\{/, m => m + block);
fs.writeFileSync(p, src);
console.log('packaging excludes parcheado');

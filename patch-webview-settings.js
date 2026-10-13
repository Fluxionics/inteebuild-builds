const fs=require('fs');
const NL=String.fromCharCode(10);
const pkg='com.nikita.brutalbird';
const mp='android/app/src/main/java/com/nikita/brutalbird/MainActivity.java';
let src=fs.readFileSync(mp,'utf8');
let changed=false;

if(src.indexOf('WebView')!==-1 && src.indexOf('setBuiltInZoomControls')===-1){
  src=src.replace(/wv\.getSettings\(\)\.setJavaScriptEnabled\(true\);/,'wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setBuiltInZoomControls(false); wv.getSettings().setDisplayZoomControls(false);');
  changed=true;
}
if(changed) fs.writeFileSync(mp,src);
console.log('WebView settings patch applied:'+changed);
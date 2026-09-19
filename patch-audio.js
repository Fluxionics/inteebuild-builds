const fs=require('fs');
const NL=String.fromCharCode(10);
const pkg=JSON.parse(fs.readFileSync('build-config.json','utf8')).packageName;
const mp='android/app/src/main/java/'+pkg.split('.').join('/')+'/MainActivity.java';
let src=fs.readFileSync(mp,'utf8');
let changed=false;
if(src.indexOf('InteeAudio')===-1){
  src=src.replace(/super\.onCreate\(savedInstanceState\);/,m=>m+NL+'    try{ getBridge().getWebView().addJavascriptInterface(new AudioBridge(this), "InteeAudio"); }catch(Exception ignored){}');
  changed=true;
  fs.writeFileSync(mp,src);
}
console.log('AudioBridge patch applied:'+changed);

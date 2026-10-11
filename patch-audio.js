const fs=require('fs');
const NL=String.fromCharCode(10);
const pkg=JSON.parse(fs.readFileSync('build-config.json','utf8')).packageName;
const mp='android/app/src/main/java/'+pkg.split('.').join('/')+'/MainActivity.java';
let src=fs.readFileSync(mp,'utf8');
let changed=false;
if(/extends\s+BridgeActivity/.test(src)&&src.indexOf('InteeAudio')===-1){
  if(!/super\s*\.\s*onCreate\s*\(/.test(src)){
    src=src.replace(/public class MainActivity extends BridgeActivity\s*\{/,m=>m+NL+'  @Override protected void onCreate(android.os.Bundle ibState) {'+NL+'    super.onCreate(ibState);'+NL+'  }'+NL);
  }
  src=src.replace(/super\.onCreate\([^)]*\);/,m=>m+NL+'    try{ getBridge().getWebView().addJavascriptInterface(new AudioBridge(this), "InteeAudio"); }catch(Exception ignored){}');
  changed=true;
  fs.writeFileSync(mp,src);
}
console.log('AudioBridge patch applied:'+changed);

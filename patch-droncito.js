const fs=require('fs');
const NL=String.fromCharCode(10);
const cfg=JSON.parse(fs.readFileSync('build-config.json','utf8'));
const pkg=cfg.packageName;
const mp='android/app/src/main/java/'+pkg.split('.').join('/')+'/MainActivity.java';
let src=fs.readFileSync(mp,'utf8');
let changed=false;
if(src.indexOf('DroncitoBridge')===-1 && /extends\s+BridgeActivity/.test(src)){
  if(!/super\s*\.\s*onCreate\s*\(/.test(src)){
    src=src.replace(/public class MainActivity extends BridgeActivity\s*\{/,m=>m+NL+'  @Override protected void onCreate(android.os.Bundle ibState) {'+NL+'    super.onCreate(ibState);'+NL+'  }'+NL);
  }
  src=src.replace(/super\.onCreate\([^)]*\);/,m=>m+NL+'    try{ getBridge().getWebView().addJavascriptInterface(new '+pkg+'.DroncitoBridge(this), \'Droncito\'); }catch(Exception ignored){}'+NL+'    try{ android.webkit.WebView wv2=getBridge().getWebView(); wv2.getSettings().setMediaPlaybackRequiresUserGesture(false); }catch(Exception ignored){}');
  changed=true;
}
if(changed) fs.writeFileSync(mp,src);
console.log('Droncito patch applied:'+changed);

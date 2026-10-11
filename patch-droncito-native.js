const fs=require('fs');
const NL=String.fromCharCode(10);
const cfg=JSON.parse(fs.readFileSync('build-config.json','utf8'));
const pkg=cfg.packageName;
const mp='android/app/src/main/java/'+pkg.split('.').join('/')+'/MainActivity.java';
let src=fs.readFileSync(mp,'utf8');
let changed=false;
if(src.indexOf('DroncitoBridge')===-1){
  src=src.replace(/import\s+androidx\.appcompat\.app\.AppCompatActivity\s*;/,'import android.webkit.JavascriptInterface; import androidx.appcompat.app.AppCompatActivity;');
  src=src.replace(/wv\.loadUrl\(/,'try{ wv.addJavascriptInterface(new '+pkg+'.DroncitoBridge(this), "Droncito"); }catch(Exception ignored){} '+NL+'    wv.loadUrl(');
  changed=true;
}
if(changed) fs.writeFileSync(mp,src);
console.log('Droncito native patch applied:'+changed);

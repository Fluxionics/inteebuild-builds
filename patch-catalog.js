const fs=require('fs');
const NL=String.fromCharCode(10);
const cfg=JSON.parse(fs.readFileSync('build-config.json','utf8'));
const pkg=cfg.packageName;
const mp='android/app/src/main/java/'+pkg.split('.').join('/')+'/MainActivity.java';
let src=fs.readFileSync(mp,'utf8');
let changed=false;
// FLAG_SECURE
if(cfg.flagSecure && src.indexOf('FLAG_SECURE')===-1){
  src=src.replace(/super\.onCreate\(savedInstanceState\);/,m=>m+NL+'    if(true) getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE);');
  changed=true;
}
// DownloadManager + tel/mailto intents + catalog JS injection
if(src.indexOf('DownloadListener')===-1){
  src=src.replace(/import\s+com\.getcapacitor\.BridgeActivity\s*;/,'import android.app.DownloadManager; import android.content.Intent; import android.net.Uri; import android.webkit.DownloadListener; import android.webkit.WebView; import android.webkit.WebViewClient; import com.getcapacitor.BridgeActivity;');
  src=src.replace(/super\.onCreate\(savedInstanceState\);/,m=>m+NL+'    try{ getBridge().getWebView().setDownloadListener(new DownloadListener(){ public void onDownloadStart(String url, String ua, String cd, String mime, long len){ try{ Intent i=new Intent(Intent.ACTION_VIEW); i.setData(Uri.parse(url)); startActivity(i);}catch(Exception e){ try{ DownloadManager dm=(DownloadManager)getSystemService(DOWNLOAD_SERVICE); DownloadManager.Request r=new DownloadManager.Request(Uri.parse(url)); r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); dm.enqueue(r);}catch(Exception ignored){}} } }); }catch(Exception ignored){}'+NL+'    try{ getBridge().getWebView().setWebViewClient(new WebViewClient(){ public boolean shouldOverrideUrlLoading(WebView v, String url){ if(url.startsWith("tel:")||url.startsWith("mailto:")||url.startsWith("sms:")||url.startsWith("whatsapp://")||url.startsWith("intent:")){ try{ startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); return true;}catch(Exception e){ return false;}} return false; } public void onPageFinished(WebView v, String url){ super.onPageFinished(v,url); try{ java.io.InputStream is=getAssets().open("public/catalog.js"); java.io.BufferedReader br=new java.io.BufferedReader(new java.io.InputStreamReader(is)); StringBuilder sb=new StringBuilder(); String line; while((line=br.readLine())!=null) sb.append(line).append("\\n"); br.close(); v.evaluateJavascript(sb.toString(),null);}catch(Exception ignored){} } }); }catch(Exception ignored){}');
  changed=true;
}
// Back button behavior
if(cfg.backButtonBehavior && src.indexOf('onBackPressed')===-1){
  const behavior=cfg.backButtonBehavior;
  let code='  @Override public void onBackPressed(){ try{ if(getBridge().getWebView().canGoBack()) getBridge().getWebView().goBack(); else super.onBackPressed(); }catch(Exception e){ super.onBackPressed(); }}';
  if(behavior==='exit') code='  @Override public void onBackPressed(){ finishAffinity(); }';
  if(behavior==='confirm') code='  @Override public void onBackPressed(){ new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Salir?").setMessage("¿Deseas salir de la app?").setPositiveButton("Salir", (d,w)->finishAffinity()).setNegativeButton("Cancelar", null).show(); }';
  if(behavior==='none') code='  @Override public void onBackPressed(){ }';
  src=src.replace(/public class MainActivity extends BridgeActivity\s*\{/,m=>m+NL+code);
  changed=true;
}
// Root detection
if(cfg.rootDetection && src.indexOf('RootCheck')===-1){
  src=src.replace(/super\.onCreate\(savedInstanceState\);/,m=>m+NL+'    try{ boolean rooted=new java.io.File("/system/bin/su").exists()||new java.io.File("/system/xbin/su").exists()||new java.io.File("/system/bin/magisk").exists(); if(rooted) android.util.Log.w("InteeBuild","Root detected"); }catch(Exception ignored){}');
  changed=true;
}
if(changed) fs.writeFileSync(mp,src);
console.log('Catalog patch applied:'+changed);

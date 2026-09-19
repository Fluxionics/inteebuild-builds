const fs=require('fs');
const NL=String.fromCharCode(10);
const pkg=JSON.parse(fs.readFileSync('build-config.json','utf8')).packageName;
const mp='android/app/src/main/java/'+pkg.split('.').join('/')+'/MainActivity.java';
let src=fs.readFileSync(mp,'utf8');
let changed=false;
if(src.indexOf('NativePermissions')===-1){
  src=src.replace(/import\s+com\.getcapacitor\.BridgeActivity\s*;/,'import android.Manifest; import android.content.pm.PackageManager; import android.webkit.PermissionRequest; import android.webkit.WebChromeClient; import androidx.core.content.ContextCompat; import com.getcapacitor.BridgeActivity;');
  src=src.replace(/public class MainActivity extends BridgeActivity\s*\{/,m=>m+NL+'  private static final int REQ_PERMS=9001;'+NL+'  private boolean hasPerm(String p){ try{ return ContextCompat.checkSelfPermission(this,p)==PackageManager.PERMISSION_GRANTED; }catch(Exception e){ return false; }}'+NL+'  private boolean wants(String res){ try{ String[] declared=getPackageManager().getPackageInfo(getPackageName(),PackageManager.GET_PERMISSIONS).requestedPermissions; if(declared==null) return false; for(String d:declared){ if(res.contains("VIDEO")&&(d.equals(Manifest.permission.CAMERA))) return true; if(res.contains("AUDIO")&&(d.equals(Manifest.permission.RECORD_AUDIO)||d.equals(Manifest.permission.MODIFY_AUDIO_SETTINGS))) return true; if(res.contains("GEOLOCATION")&&(d.equals(Manifest.permission.ACCESS_FINE_LOCATION)||d.equals(Manifest.permission.ACCESS_COARSE_LOCATION))) return true; } return false; }catch(Exception e){ return false; }}'+NL+'  @Override public void onStart(){ super.onStart(); try{ NativePermissions.requestAll(this, REQ_PERMS);}catch(Exception ignored){}}'+NL+'  @Override public void onRequestPermissionsResult(int c,String[] p,int[] r){ super.onRequestPermissionsResult(c,p,r); }');
  if(src.indexOf('onPermissionRequest')===-1){
    src=src.replace(/super\.onCreate\(savedInstanceState\);/,s=>s+NL+'    try{ getBridge().getWebView().setWebChromeClient(new WebChromeClient(){ @Override public void onPermissionRequest(final PermissionRequest request){ runOnUiThread(new Runnable(){ public void run(){ try{ String[] res=request.getResources(); java.util.List<String> ok=new java.util.ArrayList<>(); for(String r:res){ if(r.contains("VIDEO")&&wants(r)&&hasPerm(Manifest.permission.CAMERA)) ok.add(r); else if(r.contains("AUDIO")&&wants(r)&&hasPerm(Manifest.permission.RECORD_AUDIO)) ok.add(r); else if(r.contains("GEOLOCATION")&&wants(r)&&(hasPerm(Manifest.permission.ACCESS_FINE_LOCATION)||hasPerm(Manifest.permission.ACCESS_COARSE_LOCATION))) ok.add(r); } if(!ok.isEmpty()) request.grant(ok.toArray(new String[0])); else request.deny(); }catch(Exception e){ try{request.deny();}catch(Exception ignored){}} }}); } }); }catch(Exception ignored){}');
  }
  fs.writeFileSync(mp,src); changed=true;
}
console.log('Permissions patch applied:'+changed+' hasNative:'+(src.indexOf('NativePermissions')!==-1));

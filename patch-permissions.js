const fs=require('fs');
const NL=String.fromCharCode(10);
const pkg=JSON.parse(fs.readFileSync('build-config.json','utf8')).packageName;
const mp='android/app/src/main/java/'+pkg.split('.').join('/')+'/MainActivity.java';
let src=fs.readFileSync(mp,'utf8');
let changed=false;
if(src.indexOf('NativePermissions')===-1){
  src=src.replace(/import\s+com\.getcapacitor\.BridgeActivity\s*;/,'import android.Manifest; import android.content.pm.PackageManager; import android.webkit.PermissionRequest; import android.webkit.WebChromeClient; import com.getcapacitor.BridgeActivity;');
  src=src.replace(/public class MainActivity extends BridgeActivity\s*\{/,m=>m+NL+'  private static final int REQ_PERMS=9001;'+NL+'  private boolean hasPerm(String p){ try{ return checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED; }catch(Exception e){ return false; }}'+NL+'  private boolean declared(String p){ try{ String[] d=getPackageManager().getPackageInfo(getPackageName(),PackageManager.GET_PERMISSIONS).requestedPermissions; return d!=null && java.util.Arrays.asList(d).contains(p); }catch(Exception e){ return false; }}'+NL+'  private boolean wantsVideo(){ return false; }'+NL+'  private boolean hasVideo(){ return false; }'+NL+'  private boolean wantsAudio(){ return declared(Manifest.permission.RECORD_AUDIO) || declared(Manifest.permission.MODIFY_AUDIO_SETTINGS); }'+NL+'  private boolean hasAudio(){ return hasPerm(Manifest.permission.RECORD_AUDIO) || hasPerm(Manifest.permission.MODIFY_AUDIO_SETTINGS); }'+NL+'  private boolean wantsGeo(){ return false; }'+NL+'  private boolean hasGeo(){ return false; }'+NL+'  @Override public void onStart(){ super.onStart(); try{ NativePermissions.requestAll(this, REQ_PERMS);}catch(Exception ignored){}}'+NL+'  @Override public void onRequestPermissionsResult(int c,String[] p,int[] r){ super.onRequestPermissionsResult(c,p,r); try{ if(c==NativePermissions.REQ_BATCH) NativePermissions.requestBackground(this); }catch(Exception ignored){} }');
  fs.writeFileSync(mp,src); changed=true;
}
console.log('Permissions patch applied:'+changed+' hasNative:'+(src.indexOf('NativePermissions')!==-1));

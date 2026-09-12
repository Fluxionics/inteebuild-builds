const fs = require('fs');
const NL = String.fromCharCode(10);
const pkg = JSON.parse(fs.readFileSync('build-config.json', 'utf8')).packageName;
const mp = 'android/app/src/main/java/' + pkg.split('.').join('/') + '/MainActivity.java';
let src = fs.readFileSync(mp, 'utf8');
if (src.indexOf('RadioService') === -1) {
  src = src.split('import com.getcapacitor.BridgeActivity;').join(['import android.content.Intent;', 'import android.os.Bundle;', 'import com.getcapacitor.BridgeActivity;'].join(NL));
  src = src.replace(/public class MainActivity extends BridgeActivity\s*\{/, function (m) {
    return m + NL + '  @Override' + NL + '  public void onCreate(Bundle savedInstanceState) {' + NL + '    super.onCreate(savedInstanceState);' + NL + '    try { startForegroundService(new Intent(this, RadioService.class)); } catch (Exception ignored) {}' + NL + '  }' + NL;
  });
  fs.writeFileSync(mp, src);
}
console.log('RadioService hook present: ' + (src.indexOf('RadioService') !== -1));

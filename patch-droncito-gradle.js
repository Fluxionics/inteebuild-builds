const fs=require('fs');
const p='android/app/build.gradle';
let src=fs.readFileSync(p,'utf8');
if(src.indexOf('Droncito Pack')===-1){
  const deps="    // Droncito Pack (pesado permitido: AR / IA / Geo)\n    implementation 'com.google.ar:core:1.42.0'\n    implementation 'com.google.mlkit:image-labeling:17.0.9'\n    implementation 'com.google.mlkit:barcode-scanning:17.2.0'\n    implementation 'com.google.mlkit:face-detection:16.1.7'\n    implementation 'com.google.mlkit:text-recognition:16.0.1'\n    implementation 'com.google.android.gms:play-services-location:21.3.0'\n    // Droncito Oleada 2 (Blockchain + IoT)\n    implementation 'org.web3j:core:4.12.0'\n    implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'";
  src=src.replace(/dependencies\s*\{/,m=>m+'\n'+deps);
  fs.writeFileSync(p,src);
  console.log('Droncito gradle deps applied');
} else { console.log('Droncito gradle deps already present'); }

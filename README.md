# Centro Automotriz Sanchéz (generado con InteeBuild)

- URL: https://centroautomotriz.vercel.app/
- Package: centroautomotriz.sanchez.intee
- compileSdk: 36 / targetSdk: 36 / minSdk: 23

## Compilar localmente

```bash
npm install
npx cap add android
npx cap sync android
cd android && ./gradlew assembleDebug
```

El APK queda en `android/app/build/outputs/apk/debug/app-debug.apk`.

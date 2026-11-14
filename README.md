# Glass Keyboard (Android)

iPhone style liquid glass keyboard. English, Bangla Avro (phonetic), Bangla Bijoy. Dark and light follow the phone theme.

## APK build (free, GitHub Actions)
1. Make a free GitHub account, create a new repository (private is fine)
2. Upload everything in this folder, including the hidden .github folder
3. Open the Actions tab, run "Build APK" (it also runs on every push)
4. When it finishes (about 4 to 6 minutes), open the run and download the artifact "GlassKeyboard-apk"
5. Unzip, copy app-debug.apk to your phone, install it (allow install from unknown sources)
6. Open the app, tap Enable keyboard, then Choose keyboard

## Notes
- Globe key: tap to switch English, Avro, Bijoy. Long press opens the system keyboard picker
- Avro: use capital letters for special sounds (T Th D Dh N S O OI OU R), ` breaks a conjunct
- Bijoy: vowel signs are typed after the consonant (Unicode order). All key positions live in Layouts.kt, edit there
- Real blur of the app behind the keyboard only works on Android 12 and newer, on phones that allow window blur
- Not included in v1: GIF, translate, voice typing, Bangla word suggestions

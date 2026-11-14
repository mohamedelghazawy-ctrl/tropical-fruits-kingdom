# جزيرة القصر — نقل أندرويد

- `island/IslandEngine.kt` : محرّك Kotlin نقي (مختبَر بـ kotlinc) مطابق لقواعد نسخة الويب.
- `island/IslandData.kt` : إحداثيات المباني وقياسات الصور وأسماء السكان وجمل الافتتاحية.
- `ui/screens/IslandScreen.kt`, `SplashScreen.kt` : واجهات Compose (**لم تُترجَم هنا — لا يوجد Android SDK**؛ ابنِ وراجع على Android Studio).
- `LevelConfig` : معايرة الصعوبة (95 جزيرة، needScale، target=حركات×(130+n)) مطابقة للويب.
- `GameEngine.findSmartHint` : التلميح الذكي (يدوي بعد ٥ ثوانٍ خمول).
- الأصول في `res/drawable-nodpi` (isl_*, resident_*, splash_*).
- ناقص: إعلانات AdMob حقيقية (الآن AdGate تجريبي/مكافأة فورية)، إشعارات "جاهز"، قصر الملكة مرسوم بإيموجي 🏰، (تم) جواهر القاعة والإحياء المجاني موصولان.

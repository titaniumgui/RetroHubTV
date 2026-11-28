package com.retrohub.tv.i18n

/**
 * Arabic (Modern Standard Arabic) translations, keyed by the English source text.
 *
 * RetroArch's own menu items (Load Content, Close Content, Online Updater…) stay in English because
 * that is what RetroArch shows. Menu paths use ← so they read right to left.
 */
internal object ArabicCatalog : Catalog {
    override val strings: Map<String, String> = mapOf(
        // ---- Language screen ----
        "Language" to "اللغة",

        // ---- Navigation and common actions ----
        "Home" to "الرئيسية",
        "Games" to "الألعاب",
        "Search" to "بحث",
        "Settings" to "الإعدادات",
        "Save" to "حفظ",
        "Clear" to "مسح",
        "Cancel" to "إلغاء",
        "Delete" to "حذف",
        "Remove" to "حذف",
        "Back" to "رجوع",
        "Done" to "تم",
        "Stop" to "إيقاف",
        "Options" to "خيارات",
        "Not set" to "غير محدد",
        "Unknown" to "غير معروف",
        "Just now" to "الآن",
        "Yesterday" to "أمس",

        // ---- Home ----
        "CONTINUE PLAYING" to "تابع اللعب",
        "FAVOURITE" to "مفضلة",
        "Find my games automatically" to "ابحث عن ألعابي تلقائيًا",
        "RetroHub reads the games RetroArch already knows about, then searches your storage for ROMs. You review the result before anything is added." to
            "يقرأ RetroHub الألعاب التي يعرفها RetroArch، ثم يبحث عن ملفات الألعاب في وحدة التخزين. تراجع النتيجة قبل إضافة أي شيء.",
        "Continue playing" to "تابع اللعب",
        "Favourites" to "المفضلة",
        "WELCOME" to "أهلًا بك",
        "Hello, {0}" to "مرحبًا، {0}",
        "Hello" to "مرحبًا",
        "{0} in your library" to "{0} في مكتبتك",
        "Played {0}" to "آخر لعب {0}",
        "Play" to "تشغيل",
        "Details" to "التفاصيل",
        "Systems" to "الأنظمة",
        "All games" to "كل الألعاب",
        "Every system" to "كل الأنظمة",
        "+ Add system" to "+ إضافة نظام",
        "ROM folder · core · BIOS" to "مجلد الألعاب · النواة · BIOS",

        // ---- Library ----
        "All" to "الكل",
        "★ Favourites" to "★ المفضلة",
        "Scan" to "فحص",
        "Edit system" to "تعديل النظام",
        "No favourites yet. Open a game and press ☆ to add it." to "لا توجد ألعاب مفضلة بعد. افتح لعبة واضغط \"إضافة إلى المفضلة\".",
        "No games here yet. Check the system's ROM folder and extensions, then scan." to
            "لا توجد ألعاب هنا بعد. تحقق من مجلد ألعاب النظام وامتداداته، ثم افحص.",

        // ---- Search ----
        "Search your games" to "ابحث في ألعابك",
        "Type a title. Matches file names and database names." to "اكتب اسم لعبة. يُطابق أسماء الملفات وأسماء قاعدة البيانات.",
        "Searching…" to "جارٍ البحث…",
        "No games match \"{0}\"" to "لا توجد ألعاب تطابق \"{0}\"",

        // ---- Profiles ----
        "Manage profiles" to "إدارة الملفات الشخصية",
        "Who's playing?" to "من يلعب؟",
        "Select a profile to rename, recolour or delete it." to "اختر ملفًا شخصيًا لإعادة تسميته أو تغيير لونه أو حذفه.",
        "Each profile keeps its own favourites, history and save games." to "لكل ملف شخصي مفضلته وسجله وملفات حفظه الخاصة.",
        "Add profile" to "إضافة ملف شخصي",
        "New profile" to "ملف شخصي جديد",
        "Name shown on the dashboard." to "الاسم الذي يظهر في الشاشة الرئيسية.",
        "Rename" to "إعادة تسمية",
        "Change colour" to "تغيير اللون",
        "Rename profile" to "إعادة تسمية الملف الشخصي",
        "Delete {0}?" to "حذف {0}؟",
        "Removes this profile's favourites and play history. Its save files stay on storage in the RetroHub profiles folder." to
            "يحذف مفضلة هذا الملف الشخصي وسجل لعبه. تبقى ملفات الحفظ في مجلد الملفات الشخصية لـ RetroHub.",
        "Current" to "الحالي",
        "Player {0}" to "اللاعب {0}",
        "Keep at least one profile" to "يجب الإبقاء على ملف شخصي واحد على الأقل",
        "Deleted {0}. Their save files stay in the profile folder." to "حُذف {0}. تبقى ملفات الحفظ في مجلد الملف الشخصي.",

        // ---- Game page ----
        "Resume" to "استئناف",
        "Start fresh" to "بدء من جديد",
        "Favourite" to "مفضلة",
        "Add to favourites" to "إضافة إلى المفضلة",
        "Back to RetroHub: {0}." to "العودة إلى RetroHub: {0}.",
        "Core for this game" to "النواة لهذه اللعبة",
        "Edit {0}" to "تعديل {0}",
        "Delete saved progress" to "حذف التقدم المحفوظ",
        "Hide technical details" to "إخفاء التفاصيل التقنية",
        "Show technical details" to "إظهار التفاصيل التقنية",
        "Core name (the part before _libretro_android.so) or a full .so path. Leave empty to use the system core" to
            "اسم النواة (الجزء الذي يسبق _libretro_android.so) أو المسار الكامل لملف ‎.so. اتركه فارغًا لاستخدام نواة النظام",
        "Delete saved progress?" to "حذف التقدم المحفوظ؟",
        "Removes this profile's {0} save state(s) for {1}, including the auto-save. In-game battery saves are kept. This cannot be undone." to
            "يحذف حالات الحفظ ({0}) لهذا الملف الشخصي في {1}، بما فيها الحفظ التلقائي. تبقى ملفات الحفظ داخل اللعبة. لا يمكن التراجع عن ذلك.",
        "This game is no longer in your library" to "لم تعد هذه اللعبة في مكتبتك",
        "It was removed by a scan or its system was deleted." to "أُزيلت أثناء فحص أو حُذف نظامها.",
        "Core" to "النواة",
        "If RetroArch says the core is missing: RetroArch → Online Updater → Core Downloader → {0}" to
            "إذا أخبرك RetroArch أن النواة مفقودة: RetroArch ← Online Updater ← Core Downloader ← {0}",
        "Developer" to "المطوّر",
        "Publisher" to "الناشر",
        "Played" to "مرات اللعب",
        "Never" to "أبدًا",
        "Once" to "مرة واحدة",
        "Last played" to "آخر لعب",
        "File" to "الملف",
        "Size" to "الحجم",
        "Core override" to "نواة مخصصة",
        "Save states" to "حالات الحفظ",
        "No saves yet for this profile. Progress is saved automatically whenever you return to RetroHub (controller shortcut or Close Content). To save mid-game: RetroArch's menu → Save States → Save State." to
            "لا توجد ملفات حفظ لهذا الملف الشخصي بعد. يُحفظ التقدم تلقائيًا كلما عدت إلى RetroHub (باختصار يد التحكم أو Close Content). للحفظ أثناء اللعب: قائمة RetroArch ← Save States ← Save State.",
        "Resume loads the auto save. Load other slots in RetroArch: menu → Save States → State Slot → Load State. Leaving with the TV's Home button does not update the auto save." to
            "يحمّل الاستئناف الحفظ التلقائي. لتحميل خانات أخرى في RetroArch: القائمة ← Save States ← State Slot ← Load State. الخروج بزر Home في التلفاز لا يحدّث الحفظ التلقائي.",
        "Technical details" to "التفاصيل التقنية",
        "Path" to "المسار",
        "No filesystem path (SAF only)" to "لا يوجد مسار في نظام الملفات (SAF فقط)",
        "Database name" to "الاسم في قاعدة البيانات",
        "Core file" to "ملف النواة",
        "Config" to "الإعدادات",
        "RetroArch default" to "إعدادات RetroArch الافتراضية",
        "Equivalent adb command" to "أمر adb المكافئ",
        "Auto save" to "الحفظ التلقائي",
        "Slot {0}" to "الخانة {0}",

        // ---- Returning from a game ----
        "Hold Start (2 seconds)" to "اضغط مطولًا على Start (ثانيتان)",
        "Hold Select (2 seconds)" to "اضغط مطولًا على Select (ثانيتان)",
        "Off (use Close Content only)" to "إيقاف (استخدم Close Content فقط)",
        "Hold Start" to "اضغط مطولًا على Start",
        "Hold Select" to "اضغط مطولًا على Select",
        "Custom ({0})" to "مخصص ({0})",
        "open RetroArch's menu (Start + Select by default) → Close Content" to
            "افتح قائمة RetroArch (Start + Select افتراضيًا) ← Close Content",
        "{0} on your controller, or open RetroArch's menu (Start + Select by default) → Close Content" to
            "{0} على يد التحكم، أو افتح قائمة RetroArch (Start + Select افتراضيًا) ← Close Content",

        // ---- Find my games ----
        "Find my games" to "ابحث عن ألعابي",
        "RetroArch {0} · uses RetroArch's playlists, then searches your storage" to "RetroArch {0} · يستخدم قوائم تشغيل RetroArch، ثم يبحث في وحدة التخزين",
        "Search now" to "ابحث الآن",
        "Search again" to "ابحث مجددًا",
        "RetroHub needs storage access to look for games. Grant it in Settings → Storage, then come back." to
            "يحتاج RetroHub إلى إذن الوصول إلى التخزين للبحث عن الألعاب. امنحه من الإعدادات ← التخزين، ثم عُد إلى هنا.",
        "Open Settings" to "فتح الإعدادات",
        "No games found. Put ROMs in a folder on internal storage (for example ROMs/snes) or scan them in RetroArch (Import Content), then search again. You can also add a system by hand under Settings → Manage systems." to
            "لم يُعثر على ألعاب. ضع ملفات الألعاب في مجلد على وحدة التخزين الداخلية (مثل ROMs/snes) أو افحصها في RetroArch (Import Content)، ثم ابحث مجددًا. يمكنك أيضًا إضافة نظام يدويًا من الإعدادات ← إدارة الأنظمة.",
        "Choose what to add, then press Add." to "اختر ما تريد إضافته، ثم اضغط إضافة.",
        "Cores are picked for you. If RetroArch says a core is missing, install it once from RetroArch → Online Updater → Core Downloader. You can switch cores per system at any time." to
            "تُختار النوى نيابةً عنك. إذا أخبرك RetroArch أن نواة مفقودة، فثبّتها مرة واحدة من RetroArch ← Online Updater ← Core Downloader. يمكنك تغيير النواة لكل نظام في أي وقت.",
        "From RetroArch" to "من RetroArch",
        "Already added" to "مضاف مسبقًا",
        "Core: no recommendation for this system — choose one later" to "النواة: لا توجد توصية لهذا النظام، اختر واحدة لاحقًا",
        "Core: {0} · the one RetroArch already uses" to "النواة: {0} · التي يستخدمها RetroArch حاليًا",
        "Core: {0} · chosen automatically" to "النواة: {0} · اختيرت تلقائيًا",
        "Reading RetroArch playlists…" to "جارٍ قراءة قوائم تشغيل RetroArch…",
        "Looking for games on storage…" to "جارٍ البحث عن الألعاب في وحدة التخزين…",
        "Looking for games… {0} found" to "جارٍ البحث عن الألعاب… عُثر على {0}",
        "Nothing selected" to "لم يُحدَّد شيء",

        // ---- Systems ----
        "Each system is a ROM folder, its file types and the RetroArch core that runs them" to
            "كل نظام هو مجلد ألعاب وأنواع ملفاته ونواة RetroArch التي تشغّلها",
        "Add system" to "إضافة نظام",
        "Scan all" to "فحص الكل",
        "No systems yet. Press \"Add system\" and start from a preset (it fills in the name, file types and metadata database)." to
            "لا توجد أنظمة بعد. اضغط \"إضافة نظام\" وابدأ من إعداد مسبق (يملأ الاسم وأنواع الملفات وقاعدة بيانات المعلومات).",
        "No ROM folder" to "لا يوجد مجلد ألعاب",
        "Core: {0}" to "النواة: {0}",
        "not set" to "غير محددة",
        "no extensions" to "بلا امتدادات",
        "Never scanned" to "لم يُفحص قط",
        "Scanned {0}" to "فُحص في {0}",
        "Start from" to "البدء من",

        // ---- Edit system ----
        "Name" to "الاسم",
        "Shown on the dashboard." to "يظهر في الشاشة الرئيسية.",
        "ROM folder (path)" to "مجلد الألعاب (مسار)",
        "Absolute path in internal storage, e.g. the folder holding this system's games." to
            "مسار كامل في وحدة التخزين الداخلية، مثل المجلد الذي يحتوي على ألعاب هذا النظام.",
        "File extensions" to "امتدادات الملفات",
        "Comma separated, without dots." to "مفصولة بفواصل، بدون نقاط.",
        "Chosen automatically. Pick another compatible core from the list if you prefer." to
            "تُختار تلقائيًا. يمكنك اختيار نواة متوافقة أخرى من القائمة.",
        "Core path (optional)" to "مسار النواة (اختياري)",
        "Full path to a core .so. Overrides the core name." to "المسار الكامل لملف نواة ‎.so. يتقدّم على اسم النواة.",
        "BIOS folder" to "مجلد BIOS",
        "Must match RetroArch → Settings → Directory → System/BIOS." to "يجب أن يطابق RetroArch ← Settings ← Directory ← System/BIOS.",
        "Required BIOS files" to "ملفات BIOS المطلوبة",
        "Comma separated file names to verify, e.g. as listed in the core's documentation." to
            "أسماء الملفات المطلوب التحقق منها مفصولة بفواصل، كما هي مذكورة في توثيق النواة.",
        "Artwork folder (optional)" to "مجلد الصور (اختياري)",
        "Images named like the ROM: <rom name>.png / .jpg / .webp" to "صور تحمل اسم ملف اللعبة: ‎<rom name>.png / .jpg / .webp",
        "Metadata database" to "قاعدة بيانات المعلومات",
        "Exact libretro-database name, e.g. \"Nintendo - Game Boy\" (see github.com/libretro/libretro-database/tree/master/rdb)." to
            "الاسم الدقيق في libretro-database، مثل \"Nintendo - Game Boy\" (راجع github.com/libretro/libretro-database/tree/master/rdb).",
        "This folder has no filesystem path. RetroHub can list it, but RetroArch cannot open its games." to
            "ليس لهذا المجلد مسار في نظام الملفات. يستطيع RetroHub عرضه، لكن RetroArch لا يستطيع فتح ألعابه.",
        "This TV has no system folder picker. Type the path instead and grant storage access in Settings." to
            "لا يحتوي هذا التلفاز على منتقي مجلدات. اكتب المسار بدلًا من ذلك وامنح إذن الوصول إلى التخزين من الإعدادات.",
        "New system" to "نظام جديد",
        "Nothing is guessed: set the folder, file types and the core installed in your RetroArch." to
            "لا شيء يُخمَّن: حدّد المجلد وأنواع الملفات والنواة المثبتة في RetroArch.",
        "Save & scan" to "حفظ وفحص",
        "ROMs" to "ملفات الألعاب",
        "Select to type a path" to "اضغط لكتابة مسار",
        "Pick folder (system picker)" to "اختيار مجلد (منتقي النظام)",
        "Scan sub-folders" to "فحص المجلدات الفرعية",
        "Up to 6 levels deep" to "حتى 6 مستويات",
        "RetroArch core" to "نواة RetroArch",
        "Resolved core" to "النواة المستخدمة",
        "Set a core name or path" to "حدّد اسم النواة أو مسارها",
        "Set the core directory in Settings" to "حدّد مجلد النوى في الإعدادات",
        "Game info & artwork" to "معلومات الألعاب والصور",
        "Metadata database (libretro)" to "قاعدة بيانات المعلومات (libretro)",
        "Covers, screenshots and game details are downloaded from this libretro database." to
            "تُنزَّل الأغلفة ولقطات الشاشة وتفاصيل الألعاب من قاعدة بيانات libretro هذه.",
        "Your own artwork always wins." to "صورك الخاصة لها الأولوية دائمًا.",
        "Pick artwork folder" to "اختيار مجلد الصور",
        "(recommended)" to "(موصى بها)",
        "Other… (type a core name)" to "أخرى… (اكتب اسم النواة)",
        "Core for {0}" to "النواة لـ {0}",
        "this system" to "هذا النظام",
        "None (no metadata)" to "لا شيء (بدون معلومات)",
        "Other… (type the name)" to "أخرى… (اكتب الاسم)",
        "Removes the system and its game list from RetroHub (favourites and play history for it too). ROM files are not touched." to
            "يزيل النظام وقائمة ألعابه من RetroHub (مع المفضلة وسجل اللعب الخاصين به). لن تُمس ملفات الألعاب.",
        "RetroArch loads BIOS files from its own System/BIOS directory; RetroHub cannot pass a BIOS path at launch. List required files here to check they are present." to
            "يحمّل RetroArch ملفات BIOS من مجلد System/BIOS الخاص به، ولا يستطيع RetroHub تمرير مسار BIOS عند التشغيل. أدرج الملفات المطلوبة هنا للتحقق من وجودها.",
        "Set the BIOS folder to verify these files." to "حدّد مجلد BIOS للتحقق من هذه الملفات.",
        "Grant storage access in Settings to verify BIOS files." to "امنح إذن الوصول إلى التخزين من الإعدادات للتحقق من ملفات BIOS.",
        "✓ found" to "✓ موجود",
        "✗ missing" to "✗ مفقود",
        "not checked" to "لم يُتحقق",
        "Saved {0}" to "حُفظ {0}",
        "Deleted {0}" to "حُذف {0}",

        // ---- Settings ----
        "Profiles" to "الملفات الشخصية",
        "Switch profile ({0})" to "تبديل الملف الشخصي ({0})",
        "Ask who's playing at start" to "اسأل من يلعب عند البدء",
        "Shown when there is more than one profile" to "يظهر عند وجود أكثر من ملف شخصي",
        "Separate saves per profile + Resume" to "حفظ منفصل لكل ملف شخصي + استئناف",
        "Each profile gets its own RetroArch save and state folders. Games auto-save when you leave with Close Content and resume next time." to
            "يحصل كل ملف شخصي على مجلدات حفظ وحالات خاصة به في RetroArch. تُحفظ الألعاب تلقائيًا عند الخروج بـ Close Content وتُستأنف في المرة التالية.",
        "Profile folder" to "مجلد الملفات الشخصية",
        "Where each profile's saves and states are kept. Default: RetroArch's own media folder, which every RetroArch build can write to." to
            "مكان ملفات الحفظ والحالات لكل ملف شخصي. الافتراضي: مجلد الوسائط الخاص بـ RetroArch، الذي يمكن لكل إصدارات RetroArch الكتابة فيه.",
        "Set \"Your RetroArch config\" below. Without it, games started from a profile use RetroArch's default settings instead of yours." to
            "حدّد \"إعدادات RetroArch الخاصة بك\" أدناه. بدونها، تستخدم الألعاب التي تبدأ من ملف شخصي إعدادات RetroArch الافتراضية بدلًا من إعداداتك.",
        "RetroArch ({0}) is not installed or not visible. Install RetroArch 32-bit, or fix the package name." to
            "RetroArch ({0}) غير مثبت أو غير مرئي. ثبّت RetroArch بنسخة 32 بت، أو صحّح اسم الحزمة.",
        "Installed" to "المثبت",
        "Data dir" to "مجلد البيانات",
        "Game access" to "الوصول إلى الألعاب",
        "No folders yet" to "لا توجد مجلدات بعد",
        "This RetroArch (Play Store edition) can only open games in folders it has been given. RetroHub learns them from RetroArch's history: in RetroArch, Load Content → open your games folder and start any game once." to
            "لا يستطيع RetroArch هذا (إصدار متجر Play) فتح الألعاب إلا في المجلدات التي مُنح إذن الوصول إليها. يتعرّف RetroHub عليها من سجل RetroArch: في RetroArch، Load Content ← افتح مجلد ألعابك وشغّل أي لعبة مرة واحدة.",
        "RetroArch package" to "حزمة RetroArch",
        "Detected automatically: com.retroarch.aarch64, com.retroarch or com.retroarch.ra32." to
            "تُكتشف تلقائيًا: com.retroarch.aarch64 أو com.retroarch أو com.retroarch.ra32.",
        "RetroArch activity" to "نشاط RetroArch",
        "Core directory" to "مجلد النوى",
        "Folder with *_libretro_android.so files. Detected automatically from the installed RetroArch." to
            "المجلد الذي يحتوي على ملفات ‎*_libretro_android.so. يُكتشف تلقائيًا من RetroArch المثبت.",
        "Detect RetroArch" to "اكتشاف RetroArch",
        "Open RetroArch" to "فتح RetroArch",
        "Install cores inside RetroArch (Main Menu → Load Core → Download a Core). RetroHub cannot list RetroArch's private core folder, so type core names exactly as installed. Open RetroArch once and allow its storage permission." to
            "ثبّت النوى داخل RetroArch (Main Menu ← Load Core ← Download a Core). لا يستطيع RetroHub عرض مجلد النوى الخاص بـ RetroArch، لذا اكتب أسماء النوى كما هي مثبتة تمامًا. افتح RetroArch مرة واحدة واسمح له بإذن التخزين.",
        "Your RetroArch config (retroarch.cfg)" to "إعدادات RetroArch الخاصة بك (retroarch.cfg)",
        "Detected automatically (RetroArch's default location). Profiles include it, so your RetroArch settings still apply." to
            "تُكتشف تلقائيًا (موقع RetroArch الافتراضي). تتضمنها الملفات الشخصية، لذا تبقى إعدادات RetroArch الخاصة بك سارية.",
        "External directory (EXTERNAL)" to "المجلد الخارجي (EXTERNAL)",
        "Optional. Leave empty unless your RetroArch setup needs it." to "اختياري. اتركه فارغًا ما لم تحتج إليه إعدادات RetroArch لديك.",
        "Return to RetroHub from a game" to "العودة إلى RetroHub من اللعبة",
        "Controller shortcut that closes the game (progress auto-saves) and comes back here. Close Content in RetroArch's menu also returns here." to
            "اختصار يد التحكم الذي يغلق اللعبة (مع حفظ التقدم تلقائيًا) ويعود إلى هنا. يعود Close Content في قائمة RetroArch إلى هنا أيضًا.",
        "Send IME extra" to "إرسال لوحة المفاتيح (IME)",
        "Passes the current keyboard so RetroArch's on-screen keyboard works" to "يمرّر لوحة المفاتيح الحالية لتعمل لوحة مفاتيح RetroArch على الشاشة",
        "Start RetroArch fresh for each game" to "تشغيل RetroArch من جديد لكل لعبة",
        "Closes a RetroArch left running in the background before each game. Turning this off can cause black screens." to
            "يغلق RetroArch الذي بقي يعمل في الخلفية قبل كل لعبة. قد يؤدي إيقاف هذا الخيار إلى شاشة سوداء.",
        "Covers, screenshots and details (genre, developer, publisher, year, players) come from libretro-database and libretro-thumbnails. Games are matched by checksum, then by name. Needs an internet connection." to
            "تأتي الأغلفة ولقطات الشاشة والتفاصيل (النوع والمطوّر والناشر والسنة وعدد اللاعبين) من libretro-database وlibretro-thumbnails. تُطابق الألعاب بالمجموع الاختباري ثم بالاسم. يتطلب اتصالًا بالإنترنت.",
        "Download after every scan" to "التنزيل بعد كل فحص",
        "Include screenshots" to "تضمين لقطات الشاشة",
        "Used as backgrounds on the dashboard and game pages" to "تُستخدم خلفياتٍ للشاشة الرئيسية وصفحات الألعاب",
        "Download game info now" to "تنزيل معلومات الألعاب الآن",
        "Retry unmatched games" to "إعادة محاولة الألعاب غير المطابقة",
        "Remove downloaded artwork" to "حذف الصور المنزّلة",
        "Artwork stored" to "الصور المخزّنة",
        "Storage" to "التخزين",
        "All-files access granted: path folders can be scanned and profile saves written." to
            "مُنح إذن الوصول إلى كل الملفات: يمكن فحص مجلدات المسارات وكتابة ملفات حفظ الملفات الشخصية.",
        "Storage permission granted: path folders can be scanned." to "مُنح إذن التخزين: يمكن فحص مجلدات المسارات.",
        "Android {0}: ROM files are not media, so scanning by path and writing profile saves needs \"All files access\". Without it, use the system folder picker for each ROM folder (works only if your TV has one)." to
            "Android {0}: ملفات الألعاب ليست وسائط، لذا يتطلب الفحص بالمسار وكتابة ملفات الحفظ إذن \"All files access\". بدونه، استخدم منتقي مجلدات النظام لكل مجلد ألعاب (يعمل فقط إذا كان تلفازك يحتوي عليه).",
        "Storage permission not granted yet." to "لم يُمنح إذن التخزين بعد.",
        "Picked SAF folders" to "مجلدات SAF المختارة",
        "Grant storage access" to "منح إذن التخزين",
        "This TV has no All-files access screen. Use the adb command shown below." to
            "لا يحتوي هذا التلفاز على شاشة إذن الوصول إلى كل الملفات. استخدم أمر adb الموضّح أدناه.",
        "If the TV hides the permission screen, run from a computer:" to "إذا كان التلفاز يخفي شاشة الإذن، فشغّل هذا من حاسوب:",
        "Performance" to "الأداء",
        "Lite mode: {0}" to "الوضع الخفيف: {0}",
        "Auto" to "تلقائي",
        "On" to "تشغيل",
        "Off" to "إيقاف",
        "Auto / On / Off. Lite: smaller image cache and thumbnails, no zoom, no backgrounds." to
            "تلقائي / تشغيل / إيقاف. الوضع الخفيف: ذاكرة صور مؤقتة وصور مصغّرة أصغر، بلا تكبير وبلا خلفيات.",
        "Restart RetroHub to apply." to "أعد تشغيل RetroHub للتطبيق.",
        "Active now: {0}." to "المُفعّل الآن: {0}.",
        "Lite" to "خفيف",
        "Standard" to "عادي",
        "Device RAM" to "ذاكرة الجهاز",
        "{0} KB" to "{0} كيلوبايت",
        "{0} MB" to "{0} ميغابايت",
        "{0} GB" to "{0} غيغابايت",
        "(low-RAM device)" to "(جهاز بذاكرة منخفضة)",
        "Image cache" to "ذاكرة الصور المؤقتة",
        "{0} MB memory, {1} MB disk" to "{0} ميغابايت في الذاكرة، {1} ميغابايت على القرص",
        "Library" to "المكتبة",
        "Manage systems" to "إدارة الأنظمة",
        "Clear recently played" to "مسح الألعاب الأخيرة",
        "Return to RetroHub" to "العودة إلى RetroHub",
        "Which RetroArch?" to "أي RetroArch؟",
        "Remove downloaded artwork?" to "حذف الصور المنزّلة؟",
        "Deletes covers and screenshots RetroHub downloaded (artwork from your own artwork folders is kept). Game info can be downloaded again." to
            "يحذف الأغلفة ولقطات الشاشة التي نزّلها RetroHub (تبقى الصور من مجلدات صورك الخاصة). يمكن تنزيل معلومات الألعاب مرة أخرى.",
        "ABOUT" to "حول التطبيق",
        "Version {0}" to "الإصدار {0}",
        "Co-developed with Claude" to "طُوّر بالتعاون مع Claude",

        // ---- Status messages ----
        "Already working — please wait" to "جارٍ العمل، يُرجى الانتظار",
        "Add a system first" to "أضف نظامًا أولًا",
        "Scanning {0}…" to "جارٍ فحص {0}…",
        "Scanning {0}…  {1} found" to "جارٍ فحص {0}…  عُثر على {1}",
        "({0} removed)" to "(حُذف {0})",
        "Getting game info…" to "جارٍ جلب معلومات الألعاب…",
        "Game info: {0} matched" to "معلومات الألعاب: طوبقت {0}",
        ", {0} not in database" to "، و{0} غير موجودة في قاعدة البيانات",
        "Game info is up to date" to "معلومات الألعاب محدّثة",
        "Downloaded artwork removed. Use \"Download game info now\" to fetch it again." to
            "حُذفت الصور المنزّلة. استخدم \"تنزيل معلومات الألعاب الآن\" لجلبها مرة أخرى.",
        "Recently played cleared" to "مُسحت الألعاب الأخيرة",
        "Couldn't prepare RetroArch's settings ({0}). Starting with RetroArch's own config." to
            "تعذّر تجهيز إعدادات RetroArch ({0}). سيبدأ التشغيل بإعدادات RetroArch الخاصة.",
        "this game's folder" to "مجلد هذه اللعبة",
        "RetroArch can't open {0} yet. In RetroArch: Load Content → open that folder and start any game from it once. RetroHub then launches all games there automatically." to
            "لا يستطيع RetroArch فتح {0} بعد. في RetroArch: Load Content ← افتح ذلك المجلد وشغّل أي لعبة منه مرة واحدة. بعدها يشغّل RetroHub جميع الألعاب فيه تلقائيًا.",
        "No RetroArch found. Install RetroArch (any official build) and try again." to
            "لم يُعثر على RetroArch. ثبّت RetroArch (أي إصدار رسمي) ثم حاول مرة أخرى.",
        "Using {0}. Cores and config detected." to "يُستخدم {0}. اكتُشفت النوى والإعدادات.",
        "Found RetroArch ({0}). Settings updated automatically." to "عُثر على RetroArch ({0}). حُدّثت الإعدادات تلقائيًا.",

        // ---- Launching ----
        "Set the RetroArch package name in Settings." to "حدّد اسم حزمة RetroArch في الإعدادات.",
        "Set the RetroArch activity in Settings." to "حدّد نشاط RetroArch في الإعدادات.",
        "This game was found through a Storage Access Framework folder that has no filesystem path. RetroArch cannot open content:// URIs, only file paths. Keep ROMs in internal storage and add the folder by path (or pick it from the \"Internal storage\" root in the folder picker)." to
            "عُثر على هذه اللعبة عبر مجلد من إطار الوصول إلى التخزين (SAF) ليس له مسار في نظام الملفات. لا يستطيع RetroArch فتح عناوين content://، بل مسارات الملفات فقط. احتفظ بملفات الألعاب في وحدة التخزين الداخلية وأضف المجلد بمساره (أو اختره من جذر \"Internal storage\" في منتقي المجلدات).",
        "This game has no file path." to "ليس لهذه اللعبة مسار ملف.",
        "No core configured. Set a core name or core path for this system (or this game)." to
            "لم تُحدَّد نواة. حدّد اسم النواة أو مسارها لهذا النظام (أو لهذه اللعبة).",
        "Core directory not set. Open Settings and press \"Detect\" next to Core directory, or type the folder that holds the *{0} files." to
            "لم يُحدَّد مجلد النوى. افتح الإعدادات واضغط \"اكتشاف RetroArch\"، أو اكتب المجلد الذي يحتوي على ملفات ‎*{0}.",
        "RetroArch ({0}) is not installed or not visible to RetroHub." to "RetroArch ({0}) غير مثبت أو غير مرئي لـ RetroHub.",
        "RetroArch activity {0} was not found in {1}." to "لم يُعثر على نشاط RetroArch {0} في {1}.",
        "Android blocked the launch: {0}" to "منع Android التشغيل: {0}",
        "RetroArch ({0}) is not installed." to "RetroArch ({0}) غير مثبت.",
        "Could not open RetroArch." to "تعذّر فتح RetroArch.",

        // ---- Scanning and storage ----
        "No ROM folder set" to "لم يُحدَّد مجلد الألعاب",
        "No ROM folder set." to "لم يُحدَّد مجلد الألعاب.",
        "No file extensions set" to "لم تُحدَّد امتدادات الملفات",
        "Permission denied: {0}" to "رُفض الإذن: {0}",
        "grant storage access or pick the folder again" to "امنح إذن الوصول إلى التخزين أو اختر المجلد مرة أخرى",
        "found no games in {0}. Nothing was removed — check that the storage is connected and readable." to
            "لم يُعثر على ألعاب في {0}. لم يُحذف شيء، تأكد من أن وحدة التخزين متصلة وقابلة للقراءة.",
        "Folder not found or not readable: {0}" to "المجلد غير موجود أو غير قابل للقراءة: {0}",
        "no storage access, so {0} can't be read. Grant access in Settings → Storage." to
            "لا يوجد إذن وصول إلى التخزين، لذا تتعذر قراءة {0}. امنح الإذن من الإعدادات ← التخزين.",
        "Folder permission missing. Pick the folder again." to "إذن المجلد مفقود. اختر المجلد مرة أخرى.",
        "SAF folder, but the permission is gone. Pick the folder again." to "مجلد SAF، لكن الإذن لم يعد موجودًا. اختر المجلد مرة أخرى.",
        "SAF folder → {0}. RetroArch will be given this path." to "مجلد SAF ← {0}. سيُمرَّر هذا المسار إلى RetroArch.",
        "SAF folder from {0}. It has no filesystem path, so RetroArch cannot open these games (it does not accept content:// URIs). Move the ROMs to internal storage and pick them from the \"Internal storage\" root, or type the path." to
            "مجلد SAF من {0}. ليس له مسار في نظام الملفات، لذا لا يستطيع RetroArch فتح هذه الألعاب (لا يقبل عناوين content://). انقل ملفات الألعاب إلى وحدة التخزين الداخلية واخترها من جذر \"Internal storage\"، أو اكتب المسار.",
        "Path folder. RetroArch receives the file paths directly." to "مجلد بمسار. يتلقى RetroArch مسارات الملفات مباشرة.",
        "Path folder, but RetroHub cannot list it yet: grant storage access in Settings (or pick the folder with the system folder picker)." to
            "مجلد بمسار، لكن RetroHub لا يستطيع عرض محتواه بعد: امنح إذن الوصول إلى التخزين من الإعدادات (أو اختر المجلد بمنتقي مجلدات النظام).",

        // ---- Game info downloads ----
        "{0}: reading checksums" to "{0}: جارٍ قراءة المجاميع الاختبارية",
        "{0}: updating game database" to "{0}: جارٍ تحديث قاعدة بيانات الألعاب",
        "{0}: artwork {1}/{2}" to "{0}: الصور {1}/{2}",
        "network error" to "خطأ في الشبكة",
    )

    override val plurals: Map<String, List<String>> = mapOf(
        "{0} games" to listOf("لا توجد ألعاب", "لعبة واحدة", "لعبتان", "{0} ألعاب", "{0} لعبة", "{0} لعبة"),
        "{0} players" to listOf("بلا لاعبين", "لاعب واحد", "لاعبان", "{0} لاعبين", "{0} لاعبًا", "{0} لاعب"),
        "{0} times" to listOf("ولا مرة", "مرة واحدة", "مرتان", "{0} مرات", "{0} مرة", "{0} مرة"),
        "{0} minutes ago" to listOf("الآن", "منذ دقيقة", "منذ دقيقتين", "منذ {0} دقائق", "منذ {0} دقيقة", "منذ {0} دقيقة"),
        "{0} hours ago" to listOf("الآن", "منذ ساعة", "منذ ساعتين", "منذ {0} ساعات", "منذ {0} ساعة", "منذ {0} ساعة"),
        "{0} days ago" to listOf("اليوم", "منذ يوم", "منذ يومين", "منذ {0} أيام", "منذ {0} يومًا", "منذ {0} يوم"),
        "Deleted {0} save-state files" to listOf(
            "لم يُحذف أي ملف حفظ", "حُذف ملف حفظ واحد", "حُذف ملفا حفظ", "حُذفت {0} ملفات حفظ", "حُذف {0} ملف حفظ", "حُذف {0} ملف حفظ",
        ),
        "Cores chosen automatically for {0} systems" to listOf(
            "لم تُختر نوى لأي نظام", "اختيرت النواة تلقائيًا لنظام واحد", "اختيرت النوى تلقائيًا لنظامين",
            "اختيرت النوى تلقائيًا لـ {0} أنظمة", "اختيرت النوى تلقائيًا لـ {0} نظامًا", "اختيرت النوى تلقائيًا لـ {0} نظام",
        ),
        "Added {0} systems. Scanning…" to listOf(
            "لم يُضف أي نظام.", "أُضيف نظام واحد. جارٍ الفحص…", "أُضيف نظامان. جارٍ الفحص…",
            "أُضيفت {0} أنظمة. جارٍ الفحص…", "أُضيف {0} نظامًا. جارٍ الفحص…", "أُضيف {0} نظام. جارٍ الفحص…",
        ),
        "Add {0} systems" to listOf("لا شيء للإضافة", "إضافة نظام واحد", "إضافة نظامين", "إضافة {0} أنظمة", "إضافة {0} نظامًا", "إضافة {0} نظام"),
        "Found {0} systems" to listOf(
            "لم يُعثر على أي نظام", "عُثر على نظام واحد", "عُثر على نظامين", "عُثر على {0} أنظمة", "عُثر على {0} نظامًا", "عُثر على {0} نظام",
        ),
        "{0} RetroArch playlists read" to listOf(
            "لم تُقرأ أي قائمة تشغيل", "قُرئت قائمة تشغيل واحدة من RetroArch", "قُرئت قائمتا تشغيل من RetroArch",
            "قُرئت {0} قوائم تشغيل من RetroArch", "قُرئت {0} قائمة تشغيل من RetroArch", "قُرئت {0} قائمة تشغيل من RetroArch",
        ),
    )
}

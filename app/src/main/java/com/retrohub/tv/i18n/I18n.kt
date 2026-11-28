package com.retrohub.tv.i18n

import java.util.Locale

/** Languages RetroHub can show. Add one here and give it a catalog in [I18n.catalogFor]. */
enum class AppLanguage(
    val code: String,
    val nativeName: String,
    /** The name in the other language, shown under [nativeName] on the language screen. */
    val altName: String,
    val rtl: Boolean,
) {
    ENGLISH("en", "English", "الإنجليزية", false),
    ARABIC("ar", "العربية", "Arabic", true);

    companion object {
        fun fromCode(code: String?): AppLanguage? = entries.firstOrNull { it.code == code }

        /** The language closest to the device's own setting, used to pre-select the first-launch choice. */
        fun forLocale(locale: Locale): AppLanguage = fromCode(locale.language) ?: ENGLISH
    }
}

/** A translation catalog keyed by the English source text. */
interface Catalog {
    val strings: Map<String, String>
    /** Plural forms keyed by the English "other" form, in CLDR order: zero, one, two, few, many, other. */
    val plurals: Map<String, List<String>>
}

/**
 * Lightweight, Context-free translations. Every user-visible English string goes through [tr], so
 * pure-Kotlin code (launch planning, scanning) can produce translated messages too. A missing
 * translation falls back to English, so an untranslated string is never blank.
 */
object I18n {
    @Volatile var language: AppLanguage = AppLanguage.ENGLISH
        private set

    /** The device's own locale while RetroHub has replaced the default; null when it hasn't. */
    @Volatile private var replacedLocale: Locale? = null

    /** Switches the language for the whole process. Date and time formats follow it. */
    @Synchronized
    fun apply(lang: AppLanguage) {
        language = lang
        if (lang == AppLanguage.ARABIC) {
            if (replacedLocale == null) replacedLocale = Locale.getDefault()
            // Arabic text with Western digits: matches the numbers RetroHub builds itself.
            Locale.setDefault(Locale.forLanguageTag("ar-u-nu-latn"))
        } else {
            replacedLocale?.let(Locale::setDefault)
            replacedLocale = null
        }
    }

    fun deviceLanguage(): AppLanguage = AppLanguage.forLocale(replacedLocale ?: Locale.getDefault())

    internal fun catalogFor(lang: AppLanguage): Catalog? = when (lang) {
        AppLanguage.ENGLISH -> null
        AppLanguage.ARABIC -> ArabicCatalog
    }

    fun lookup(text: String, lang: AppLanguage = language): String =
        catalogFor(lang)?.strings?.get(text)?.let { rtlStart(it, lang) } ?: text

    fun plural(count: Int, one: String, other: String, lang: AppLanguage = language): String {
        val forms = catalogFor(lang)?.plurals?.get(other)
        val pattern = if (forms == null) (if (count == 1) one else other) else rtlStart(forms[arabicPluralIndex(count)], lang)
        return pattern.replace("{0}", count.toString())
    }

    /**
     * A right-to-left sentence that happens to start with a Latin word ("RetroArch غير مثبت") would
     * otherwise be laid out left to right. A leading right-to-left mark fixes the paragraph direction.
     */
    private fun rtlStart(text: String, lang: AppLanguage): String {
        if (!lang.rtl) return text
        val first = text.firstOrNull { it.isLetter() } ?: return text
        return if (first.code < 0x0590) "‏$text" else text
    }

    /** CLDR Arabic plural category: 0 zero, 1 one, 2 two, 3 few (3–10), 4 many (11–99), 5 other. */
    fun arabicPluralIndex(n: Int): Int {
        val mod = n % 100
        return when {
            n == 0 -> 0
            n == 1 -> 1
            n == 2 -> 2
            mod in 3..10 -> 3
            mod in 11..99 -> 4
            else -> 5
        }
    }
}

/** Translates [text] and fills `{0}`, `{1}`… with [args]. */
fun tr(text: String, vararg args: Any?): String {
    var s = I18n.lookup(text)
    args.forEachIndexed { i, a -> s = s.replace("{$i}", a.toString()) }
    return s
}

/** Count-aware text: `trCount(n, "{0} game", "{0} games")`. Arabic uses all six plural forms. */
fun trCount(count: Int, one: String, other: String): String = I18n.plural(count, one, other)

/** "5 minutes ago" style text in the app language (Android's own version follows the system language). */
fun relativeTime(then: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - then).coerceAtLeast(0) / 60_000).toInt()
    return when {
        minutes < 1 -> tr("Just now")
        minutes < 60 -> trCount(minutes, "{0} minute ago", "{0} minutes ago")
        minutes < 60 * 24 -> trCount(minutes / 60, "{0} hour ago", "{0} hours ago")
        minutes < 60 * 24 * 2 -> tr("Yesterday")
        else -> trCount(minutes / (60 * 24), "{0} day ago", "{0} days ago")
    }
}

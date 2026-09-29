package io.github.chrislo27.toolboks.i18n

import java.util.*


class LanguageObject {

    lateinit var name: String
    lateinit var locale: LocaleObject

    fun toNamedLocale(): NamedLocale =
            NamedLocale(
                name,
                Locale.Builder()
                    .setLanguage(locale.language ?: "")
                    .setRegion(locale.country ?: "")
                    .setVariant(locale.variant ?: "")
                    .build()
            )
}

class LocaleObject {

    var language: String? = null
    var country: String? = null
    var variant: String? = null

}

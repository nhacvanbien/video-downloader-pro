package com.smarttool.videodownloader.core

object SystemUtil {

    /** Language codes the app ships translations for. */
    fun getLanguageApp(): List<String> {
        val languages: MutableList<String> = ArrayList()
        languages.add("en")
        languages.add("zh")
        languages.add("zh-TW")
        languages.add("hi")
        languages.add("es")
        languages.add("pt-BR")
        languages.add("pt")
        languages.add("fr")
        languages.add("ar")
        languages.add("bn")
        languages.add("ru")
        languages.add("de")
        languages.add("ja")
        languages.add("tr")
        languages.add("ko")
        languages.add("in")
        return languages
    }
}

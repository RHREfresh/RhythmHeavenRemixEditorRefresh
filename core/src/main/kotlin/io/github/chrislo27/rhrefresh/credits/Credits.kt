package io.github.chrislo27.rhrefresh.credits

import io.github.chrislo27.rhrefresh.RHREfresh
import io.github.chrislo27.rhrefresh.sfxdb.SFXDatabase
import io.github.chrislo27.toolboks.i18n.Localization
import java.util.*


object Credits {

    private val sfxCreditsLegacyFallback: String = listOf("Lvl100Feraligatr", "GenericArrangements", "Draster", "NP", "Eggman199", "Huebird", "oofie", "Miracle22", "MF5K", "The Golden Station", "GuardedLolz", "GlitchyPSIX", "sp00pster", "Maxanum").sortedBy { it.lowercase(Locale.ROOT) }.joinToString(separator = ", ")
    private val sfxCreditsFallback: String = listOf("Dracobot", "dexiedoo_octo", "viviancherry", "Katie1118").sortedBy { it.lowercase(Locale.ROOT) }.joinToString(separator = ", ")

    fun generateList(): List<Credit> {
        return listOf(
                "title" crediting RHREfresh.GITHUB,
                "fresh" crediting "",
                "programming" crediting "TheAlternateDoctor\n${Localization["credits.title.programming.contributions", "ThatZeoGal, iestyn129"]}",
                "sfx" crediting (SFXDatabase.let { if (!it.isDataLoading()) it.data.sfxCredits.sortedBy { it.lowercase(Locale.ROOT) }.joinToString(separator = ", ") else null } ?: sfxCreditsLegacyFallback),
                "gfx" crediting "dexiedoo_octo, Katie1118, viviancherry",
                "consulting" crediting "chrislo27, Kievit",
                "logo" crediting "dexiedoo_octo, Katie1118, Kievit, viviancherry",
                "updateutil" crediting "Zeo",
                "localization" crediting
                        """[LIGHT_GRAY]Français (French)[]
                |TheAlternateDoctor
                |
                |[LIGHT_GRAY]Español (Spanish)[]
                |patataofcourse
                |
                |[LIGHT_GRAY]Deutsch (German)[]
                |Blizeatos""".trimMargin(),
                "playtest" crediting "patataofcourse, Haikaede, Mizu Bunny, Maddy, viviancherry, Chloe, conhlee, dexiedoo_octo, Dracobot, elp, Gosh, Killble, OpaliteDelight, Seanski2, Yumiko!, Zeo",
                "resources" crediting
                        """Rhythm Heaven assets by Nintendo
[#FF8900]Kotlin[]
[DARK_GRAY]lib[][#E10000]GDX[]
LWJGL
Toolboks
Beads
FFMPEG
Async HTTP Client
Jackson
JGit
Apache Commons IO
SLF4J
OSHI
jump3r
rhmodding/bread
JCommander
zip4j
Jam3/glsl-fast-gaussian-blur""",

                "rhre3" crediting "",
                "programming" crediting "chrislo27\n${Localization["credits.title.programming.contributions", "Kamayana"]}",
                "localization" crediting
                        """[LIGHT_GRAY]Français (French)[]
                |inkedsplat, minenice55, Pengu123
                |
                |[LIGHT_GRAY]Español (Spanish)[]
                |chipdamage, Cosmicfab, (◉.◉)☂, GlitchyPSIX, Killble, meuol, quantic, SJGarnet, Suwa-ko
                |
                |[LIGHT_GRAY]Deutsch (German)[]
                |Zenon""".trimMargin(),
                "sfx" crediting (SFXDatabase.let { if (!it.isDataLoading()) it.data.sfxCreditsLegacy.sortedBy { it.lowercase(Locale.ROOT) }.joinToString(separator = ", ") else null } ?: sfxCreditsLegacyFallback),
                "gfx" crediting "GlitchyPSIX, lilbitdun, Steppy, Tickflow",
                "extras" crediting "GenericArrangements, Malalaika, The Drummer",
                "specialThanks" crediting """Alchemyking, AngryTapper, ArsenArsen, baguette, bin5s5, Chillius, ChorusSquid, Clone5184, danthonywalker, Dracobot, Draster, Dream Top, Dylstructor, EBPB2K, Fco, flyance, Fringession, garbo, GenericArrangements, (◉.◉)☂, GinoTitan, GlitchyPSIX, GrueKun, inkedsplat, iRonnoc5, jos, Lvl100Feraligatr, Malalaika, Maziodyne, Mezian, minenice55, Miracle22, Mixelz, nave, nerd, oofie, Pengu123, PikaMasterJesi, Rabbidking, RobSetback, SJGarnet, sp00pster, Ssure2, SuicuneWiFi, susmobile, TheRhythmKid, Turtike, Zenon, RHModding and Custom Remix Tourney Discord servers""",
                "donators" crediting "",
                "you" crediting ""
                     )
    }

    private infix fun String.crediting(persons: String): Credit =
            Credit(this, persons)

    data class Credit(val type: String, val persons: String) {

        private val localization: String by lazy {
            "credits.title.$type"
        }
        private val isTitle by lazy { type == "title" }

        val text: String = if (isTitle) RHREfresh.TITLE else Localization[localization]

    }

}
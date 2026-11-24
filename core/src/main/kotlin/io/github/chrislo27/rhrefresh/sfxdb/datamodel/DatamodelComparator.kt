package io.github.chrislo27.rhrefresh.sfxdb.datamodel

import io.github.chrislo27.rhrefresh.sfxdb.datamodel.impl.Cue
import io.github.chrislo27.rhrefresh.sfxdb.datamodel.impl.MultiCue

object DatamodelComparator : Comparator<Datamodel> {

    private fun Datamodel.shouldBeLower(): Boolean
    {
        return this is Cue || (this is MultiCue && !canSplit)
    }

    override fun compare(o1: Datamodel, o2: Datamodel): Int {
        return when {
            o1.shouldBeLower() && !o2.shouldBeLower() -> 1
            !o1.shouldBeLower() && o2.shouldBeLower() -> -1
            else -> 0
        }
    }
}
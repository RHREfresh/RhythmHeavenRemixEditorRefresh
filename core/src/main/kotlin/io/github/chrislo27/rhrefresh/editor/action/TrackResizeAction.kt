package io.github.chrislo27.rhrefresh.editor.action

import io.github.chrislo27.rhrefresh.PreferenceKeys.SETTINGS_NEW_TRACKS_ON_TOP
import io.github.chrislo27.rhrefresh.editor.Editor
import io.github.chrislo27.rhrefresh.entity.model.special.EndRemixEntity
import io.github.chrislo27.rhrefresh.undoredo.ReversibleAction
import io.github.chrislo27.rhrefresh.track.Remix


class TrackResizeAction(val editor: Editor, val oldSize: Int, val newSize: Int)
    : ReversibleAction<Remix> {

    override fun redo(context: Remix) {
        context.trackCount = newSize
        if (!editor.main.preferences.getBoolean(SETTINGS_NEW_TRACKS_ON_TOP)) {
            for(entity in editor.remix.entities) {
                if (entity !is EndRemixEntity) {
                    entity.updateBounds {
                        entity.bounds.setPosition(entity.bounds.x, entity.bounds.y + (newSize-oldSize))
                    }
                }
            }
        }
    }

    override fun undo(context: Remix) {
        context.trackCount = oldSize
        if (!editor.main.preferences.getBoolean(SETTINGS_NEW_TRACKS_ON_TOP)) {
            for(entity in editor.remix.entities) {
                if (entity !is EndRemixEntity) {
                    entity.updateBounds {
                        entity.bounds.setPosition(entity.bounds.x, entity.bounds.y + (oldSize-newSize))
                    }
                }
            }
        }
    }

}

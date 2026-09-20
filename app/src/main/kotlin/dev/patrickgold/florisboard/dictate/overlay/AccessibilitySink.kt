/*
 * Copyright (C) 2026 DevEmperor (Dictate)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.overlay

import android.view.KeyEvent
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.dictate.DictationSink

/**
 * [DictationSink] backed by [DictateAccessibilityService], or [FlorisImeService] when the Dictate
 * keyboard itself is extended. Used by the floating dictation button (issue #88) to write the
 * transcription into whichever app's text field is focused.
 */
class AccessibilitySink : DictationSink {
    override fun commitText(text: String, verify: Boolean): Boolean {
        FlorisImeService.currentInputConnection()?.let { ic ->
            if (ic.commitText(text, 1)) return true
        }
        return DictateAccessibilityService.injectText(text, verify)
    }

    override fun selectedText(): String {
        FlorisImeService.currentInputConnection()?.let { ic ->
            val sel = ic.getSelectedText(0)?.toString()
            if (sel != null) return sel
        }
        return DictateAccessibilityService.selectedText()
    }

    override fun fullText(): String = DictateAccessibilityService.fullText()

    override fun selectAll() {
        FlorisImeService.currentInputConnection()?.performContextMenuAction(android.R.id.selectAll)
            ?: DictateAccessibilityService.selectAll()
    }

    override fun performEnter(): Boolean {
        FlorisImeService.currentInputConnection()?.let { ic ->
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            return true
        }
        return DictateAccessibilityService.performEnter()
    }

    override fun deleteLastText(text: String): Boolean {
        FlorisImeService.currentInputConnection()?.let { ic ->
            if (text.isNotEmpty()) {
                ic.deleteSurroundingText(text.length, 0)
                return true
            }
        }
        return DictateAccessibilityService.deleteLastText(text)
    }

    override fun setDictationPreview(newText: String, prevText: String) {
        FlorisImeService.currentInputConnection()?.let { ic ->
            if (prevText == newText) return
            val cp = prevText.commonPrefixWith(newText).length
            val deleteLen = prevText.length - cp
            ic.beginBatchEdit()
            ic.finishComposingText()
            if (deleteLen > 0) ic.deleteSurroundingText(deleteLen, 0)
            if (newText.length - cp > 0) ic.commitText(newText.substring(cp), 1)
            ic.endBatchEdit()
            return
        }
        DictateAccessibilityService.setPreview(newText)
    }

    override fun commitDictationFinal(finalText: String, prevText: String): Boolean {
        FlorisImeService.currentInputConnection()?.let { ic ->
            if (prevText == finalText) return true
            val cp = prevText.commonPrefixWith(finalText).length
            val deleteLen = prevText.length - cp
            ic.beginBatchEdit()
            ic.finishComposingText()
            if (deleteLen > 0) ic.deleteSurroundingText(deleteLen, 0)
            if (finalText.length - cp > 0) ic.commitText(finalText.substring(cp), 1)
            ic.endBatchEdit()
            return true
        }
        val landed = DictateAccessibilityService.commitPreviewFinal(finalText, prevText)
        if (landed) return true
        return DictateAccessibilityService.injectText(finalText, verify = true)
    }

    override fun clearDictationPreview(prevText: String) {
        FlorisImeService.currentInputConnection()?.let { ic ->
            if (prevText.isNotEmpty()) {
                ic.beginBatchEdit()
                ic.finishComposingText()
                ic.deleteSurroundingText(prevText.length, 0)
                ic.endBatchEdit()
            }
            return
        }
        DictateAccessibilityService.clearPreview()
    }
}

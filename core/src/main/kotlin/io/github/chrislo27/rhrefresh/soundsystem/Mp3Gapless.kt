package io.github.chrislo27.rhrefresh.soundsystem

/**
 * MP3 files do not start exactly at their first sample. The decoder used here outputs, before the
 * music itself: the encoder's information frame as silence (if present), the encoder delay, and the
 * decoder delay of 529 samples. That made every MP3 music play late by about 51 ms.
 *
 * Measured with this decoder: LAME/FFmpeg files with an Info/Xing frame +2257 samples at 44.1/48 kHz
 * (1152 + 576 + 529), MPEG-2 22.05 kHz +1681 (576 + 576 + 529), files without an Info frame +1105.
 */
object Mp3Gapless {

    private const val DECODER_DELAY = 529
    private const val USUAL_ENCODER_DELAY = 576

    /** Number of samples (per channel) to drop from the start of the decoded audio. */
    fun leadingSamples(bytes: ByteArray): Int {
        try {
            var p = 0
            if (bytes.size > 10 && bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) {
                val size = ((bytes[6].toInt() and 0x7f) shl 21) or ((bytes[7].toInt() and 0x7f) shl 14) or
                        ((bytes[8].toInt() and 0x7f) shl 7) or (bytes[9].toInt() and 0x7f)
                p = 10 + size + (if (bytes[5].toInt() and 0x10 != 0) 10 else 0)
            }
            while (p + 4 < bytes.size && !(bytes[p].toInt() and 0xff == 0xff && bytes[p + 1].toInt() and 0xe0 == 0xe0)) p++
            if (p + 4 >= bytes.size) return 0
            val version = (bytes[p + 1].toInt() shr 3) and 3        // 3 = MPEG-1
            val mono = ((bytes[p + 3].toInt() shr 6) and 3) == 3
            val mpeg1 = version == 3
            val samplesPerFrame = if (mpeg1) 1152 else 576
            val sideInfo = if (mpeg1) (if (mono) 17 else 32) else (if (mono) 9 else 17)
            val x = p + 4 + sideInfo
            if (x + 8 > bytes.size) return DECODER_DELAY + USUAL_ENCODER_DELAY
            val tag = String(bytes, x, 4, Charsets.ISO_8859_1)
            if (tag != "Xing" && tag != "Info") {
                return DECODER_DELAY + USUAL_ENCODER_DELAY
            }
            val flags = ((bytes[x + 4].toInt() and 0xff) shl 24) or ((bytes[x + 5].toInt() and 0xff) shl 16) or
                    ((bytes[x + 6].toInt() and 0xff) shl 8) or (bytes[x + 7].toInt() and 0xff)
            var q = x + 8
            if (flags and 1 != 0) q += 4
            if (flags and 2 != 0) q += 4
            if (flags and 4 != 0) q += 100
            if (flags and 8 != 0) q += 4
            var encoderDelay = USUAL_ENCODER_DELAY
            if (q + 24 <= bytes.size) {
                val b0 = bytes[q + 21].toInt() and 0xff
                val b1 = bytes[q + 22].toInt() and 0xff
                val delay = (b0 shl 4) or (b1 shr 4)
                // Only trust the field when an encoder string is there (LAME, Lavc, Lavf...)
                val enc = String(bytes, q, 4, Charsets.ISO_8859_1)
                if (enc.all { it.isLetterOrDigit() } && delay in 1..4000) encoderDelay = delay
            }
            return samplesPerFrame + encoderDelay + DECODER_DELAY
        } catch (e: Exception) {
            return 0
        }
    }
}

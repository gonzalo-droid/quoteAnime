package com.gondroid.quoteanime.presentation.components

/** Quote background images are hosted on Cloudinary, uploaded at whatever resolution the
 *  source scan/art came in — some are already phone-sized, others can be several times
 *  larger than anything this app ever displays. Cloudinary supports on-the-fly delivery
 *  transformations via a URL segment, so instead of always fetching the original we ask
 *  for a capped, auto-compressed, auto-format (WebP/AVIF where supported) version — a
 *  no-op for images that were already small, a real save for the ones that weren't. */
private const val CLOUDINARY_UPLOAD_MARKER = "/upload/"
private const val MAX_DISPLAY_WIDTH = 1080

fun String.optimizedForDisplay(maxWidth: Int = MAX_DISPLAY_WIDTH): String {
    if (!contains("res.cloudinary.com")) return this
    val markerIndex = indexOf(CLOUDINARY_UPLOAD_MARKER)
    if (markerIndex == -1) return this
    val insertAt = markerIndex + CLOUDINARY_UPLOAD_MARKER.length
    // Already has a transformation segment (e.g. re-processed URL) — don't stack another one.
    val afterMarker = substring(insertAt)
    if (afterMarker.startsWith("f_") || afterMarker.startsWith("q_") || afterMarker.startsWith("w_")) {
        return this
    }
    return substring(0, insertAt) + "f_auto,q_auto,w_$maxWidth/" + afterMarker
}

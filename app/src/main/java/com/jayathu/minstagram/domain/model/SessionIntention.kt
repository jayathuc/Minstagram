package com.jayathu.minstagram.domain.model

enum class SessionIntention(
    val label: String,
    val description: String,
    val emoji: String
) {
    CHECK_DMS("Check DMs", "Reply to messages", "\uD83D\uDCAC"),
    POST_STORY("Post a story", "Share a moment", "\uD83D\uDCF8"),
    BROWSE_FEED("Browse feed", "See what friends are up to", "\uD83C\uDFE0"),
    WATCH_REELS("Watch Reels", "Entertainment time", "\uD83C\uDFAC"),
    JUST_BROWSING("Just browsing", "No specific goal", "\uD83D\uDC40")
}

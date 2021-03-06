package com.foxwoosh.radio.domain.models

data class LyricsReport(
    val reportID: String,
    val lyricsID: Int,
    val title: String,
    val artist: String,
    val comment: String,
    val state: LyricsReportState?,
    val moderatorID: Long?,
    val moderatorComment: String?,
    val createdAt: Long,
    val updatedAt: Long?
)
package com.jackleow.presentation.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Transcript(
    @SerialName("transcriptionText") val text: String
)

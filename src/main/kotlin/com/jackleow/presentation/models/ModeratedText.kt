package com.jackleow.presentation.models

import kotlinx.serialization.Serializable

@Serializable
data class ModeratedText(val chatText: List<String>)

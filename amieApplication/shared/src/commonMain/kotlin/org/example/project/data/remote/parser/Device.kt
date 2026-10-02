package org.example.project.data.remote.parser

import kotlinx.serialization.Serializable

@Serializable
data class Device(
    val name: String,
    val port: String,
    val deviceEndpoint: String? = null
)

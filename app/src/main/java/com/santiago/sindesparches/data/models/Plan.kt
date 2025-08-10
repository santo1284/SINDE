package com.santiago.sindesparches.data.models

data class Plan(
    val id: String = "",
    val userId: String = "",
    val createdAt: Long = 0,
    val title: String = "",
    val updatedAt: Long? = null,
    val description: String = "",
    val date: Long = 0,
    val timeString: String = "",
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrls: List<String> = emptyList(),
    val enableWhatsapp: Boolean? = false,
    val phoneNumber: String = "",
    val likes: List<String> = emptyList(),
    val participants: List<String> = emptyList(),
    val shares: Int = 0,
    val commentCount: Int = 0
)

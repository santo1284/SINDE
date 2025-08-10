package com.santiago.sindesparches.data.models

data class UserProfile(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val ciudad: String? = null,
    val celular: String? = null,
    val edad: Int? = null,
    val createdAt: Long? = null
)

package domain.profile

import domain.auth.User


data class CommonUser(
    val data: User,
    val group: String,
)
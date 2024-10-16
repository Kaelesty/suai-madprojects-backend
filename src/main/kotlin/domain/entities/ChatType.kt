package org.example.domain.entities

import kotlinx.serialization.Serializable

@Serializable
enum class ChatType {
    Public, MembersPrivate, CuratorPrivate
}
package entities

import kotlinx.serialization.Serializable
import shared_domain.entities.KanbanState

@Serializable
sealed interface Action {

    @Serializable
    object KeepAlive: Action

    @Serializable
    object Unauthorized: Action

    @Serializable
    sealed interface Messenger: Action {
        @Serializable
        class NewMessage(
            val chatId: Int,
            val message: Message,
            val projectId: Int,
        ): Messenger

        @Serializable
        class SendChatsList(
            val chats: List<Chat>,
            val projectId: Int,
            val senders: List<ChatSender>
        ): Messenger

        @Serializable
        class NewChat(
            val chat: Chat,
            val projectId: Int,
        ): Messenger

        @Serializable
        class SendChatMessages(
            val chatId: Int,
            val readMessages: List<Message>,
            val unreadMessages: List<Message>,
            val projectId: Int,
        ): Messenger


        @Serializable
        class MessageReadRecorded(
            val messageId: Int,
            val chatId: Int,
            val projectId: Int,
        ): Messenger

        @Serializable
        class UpdateChatUnreadCount(
            val chatId: Int,
            val count: Int,
            val projectId: Int,
        ): Messenger
    }

    @Serializable
    sealed interface Kanban: Action {

        @Serializable
        data class SetState(
            val kanban: KanbanState,
            val projectId: Int,
        ): Kanban
    }
}
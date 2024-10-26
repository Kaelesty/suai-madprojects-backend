package entities

import kotlinx.serialization.Serializable
import shared_domain.entities.KanbanState

@Serializable
sealed interface Action {

    @Serializable
    sealed interface Messenger: Action {
        @Serializable
        class NewMessage(
            val chatId: Int,
            val message: Message,
        ): Messenger

        @Serializable
        class SendChatsList(
            val chats: List<Chat>
        ): Messenger
        // On connect to WS

        @Serializable
        class NewChat(
            val chat: Chat,
        ): Messenger

        @Serializable
        class SendChatMessages(
            val chatId: Int,
            val readMessages: List<Message>,
            val unreadMessages: List<Message>,
        ): Messenger


        @Serializable
        class MessageReadRecorded(
            val messageId: Int,
            val chatId: Int,
        ): Messenger

        @Serializable
        class UpdateChatUnreadCount(
            val chatId: Int,
            val count: Int,
        ): Messenger
    }

    @Serializable
    sealed interface Kanban: Action {

        @Serializable
        data class SetState(val kanban: KanbanState): Kanban
    }
}
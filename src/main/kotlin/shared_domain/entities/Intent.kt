package entities

import kotlinx.serialization.Serializable

@Serializable
sealed interface Intent {

    @Serializable
    class Authorize(
        val jwt: String,
        val projectId: Int,
    ): Intent

    @Serializable
    data object CloseSession : Intent

    @Serializable
    sealed interface Messenger: Intent {

        @Serializable
        data object Start: Messenger

        @Serializable
        data object Stop: Messenger

        @Serializable
        class SendMessage(
            val chatId: Int,
            val message: String
        ): Messenger

        @Serializable
        class CreateChat(
            val projectId: Int,
            val chatTitle: String,
            val chatType: ChatType
        ): Messenger

        @Serializable
        class RequestChatMessages(
            val chatId: Int,
        ): Messenger

        @Serializable
        class RequestChatsList(
            val projectId: Int,
        ): Messenger

        @Serializable
        class ReadMessage(
            val messageId: Int,
            val chatId: Int,
        ): Messenger

        @Serializable
        class ReadMessagesBefore(
            val messageId: Int,
            val chatId: Int,
        ): Messenger
    }

    @Serializable
    sealed interface Kanban: Intent {

        @Serializable
        data object Start: Kanban

        @Serializable
        data object Stop: Kanban

        @Serializable
        data class GetKanban(
            val projectId: Int,
        ): Kanban

        @Serializable
        data class CreateKard(
            val name: String,
            val desc: String,
            val rowId: Int,
        ): Kanban

        @Serializable
        data class MoveKard(
            val id: Int,
            val rowId: Int,
            val newColumnId: Int,
            val newPosition: Int,
        ): Kanban

        @Serializable
        data class CreateRow(
            val name: String,
        ): Kanban

        @Serializable
        data class MoveRow(
            val id: Int,
            val newPosition: Int,
        ): Kanban
    }
}
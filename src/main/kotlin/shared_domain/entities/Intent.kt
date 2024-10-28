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
        data object GetKanban: Kanban

        @Serializable
        data class CreateKard(
            val name: String,
            val desc: String,
            val columnId: Int,
        ): Kanban

        @Serializable
        data class MoveKard(
            val id: Int,
            val columnId: Int,
            val newColumnId: Int,
            val newPosition: Int,
        ): Kanban

        @Serializable
        data class CreateColumn(
            val name: String,
        ): Kanban

        @Serializable
        data class MoveColumn(
            val id: Int,
            val newPosition: Int,
        ): Kanban

        @Serializable
        data class UpdateKard(
            val id: Int,
            val name: String?,
            val desc: String?
        ): Kanban

        @Serializable
        data class UpdateColumn(
            val id: Int,
            val name: String?,
        ): Kanban

        @Serializable
        data class DeleteKard(
            val id: Int,
        ): Kanban

        @Serializable
        data class DeleteColumn(
            val id: Int,
        ): Kanban
    }
}
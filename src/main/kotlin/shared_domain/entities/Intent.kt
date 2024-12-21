package entities

import kotlinx.serialization.Serializable

@Serializable
sealed interface Intent {

    @Serializable
    object KeepAlive: Intent

    @Serializable
    class Authorize(
        val jwt: String,
    ): Intent

    @Serializable
    data object CloseSession : Intent

    @Serializable
    sealed interface Messenger: Intent {

        @Serializable
        data class Start(
            val projectId: Int,
        ): Messenger

        @Serializable
        data class Stop(
            val projectId: Int,
        ): Messenger

        @Serializable
        class SendMessage(
            val chatId: Int,
            val message: String,
            val projectId: Int,
        ): Messenger

        @Serializable
        class CreateChat(
            val projectId: Int,
            val chatTitle: String,
            val chatType: ChatType,
        ): Messenger

        @Serializable
        class RequestChatMessages(
            val chatId: Int,
            val projectId: Int,
        ): Messenger

        @Serializable
        class RequestChatsList(
            val projectId: Int,
        ): Messenger

        @Serializable
        class ReadMessage(
            val messageId: Int,
            val chatId: Int,
            val projectId: Int,
        ): Messenger

        @Serializable
        class ReadMessagesBefore(
            val messageId: Int,
            val chatId: Int,
            val projectId: Int,
        ): Messenger
    }

    @Serializable
    sealed interface Kanban: Intent {

        @Serializable
        data class Start(
            val projectId: Int,
        ): Kanban

        @Serializable
        data class Stop(
            val projectId: Int,
        ): Kanban

        @Serializable
        data class GetKanban(
            val projectId: Int,
        ): Kanban

        @Serializable
        data class CreateKard(
            val name: String,
            val desc: String,
            val columnId: Int,
            val projectId: Int,
        ): Kanban

        @Serializable
        data class MoveKard(
            val id: Int,
            val columnId: Int,
            val newColumnId: Int,
            val newPosition: Int,
            val projectId: Int,
        ): Kanban

        @Serializable
        data class CreateColumn(
            val name: String,
            val projectId: Int,
        ): Kanban

        @Serializable
        data class MoveColumn(
            val id: Int,
            val newPosition: Int,
            val projectId: Int,
        ): Kanban

        @Serializable
        data class UpdateKard(
            val id: Int,
            val name: String?,
            val desc: String?,
            val projectId: Int,
        ): Kanban

        @Serializable
        data class UpdateColumn(
            val id: Int,
            val name: String?,
            val projectId: Int,
        ): Kanban

        @Serializable
        data class DeleteKard(
            val id: Int,
            val projectId: Int,
        ): Kanban

        @Serializable
        data class DeleteColumn(
            val id: Int,
            val projectId: Int,
        ): Kanban
    }
}
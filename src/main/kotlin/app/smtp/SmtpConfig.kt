package app.smtp

data object SmtpConfig {
    val host: String = "mad-projects.ru"
    val port: Int =587
    const val username = "user1"
    const val password = "188348"
    val fromEmail = "noreply@madprojects.ru"
}
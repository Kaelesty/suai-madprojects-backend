package data.database

import org.jetbrains.exposed.sql.Database

class DbConnector(
    private val driver: String,
    private val url: String
) {

    fun getAdminDatabase(password: String) = Database.connect(
        url = url,
        user = "editor",
        driver = driver,
        password = password
    )

    fun getCommonDatabase() = Database.connect(
        url = url,
        user = "viewer",
        driver = driver,
        password = "viewer_password"
    )
}
import app.features.database.KeyValidator

fun main() {
    val validator = KeyValidator()

    println(validator.sha256("v1ewer"))
    println(validator.sha256("sudo_"))

    println(validator.validate("viewer"))
    println(validator.validate("v1ewer"))
    println(validator.validate("sudo"))
    println(validator.validate("sudo_"))
}
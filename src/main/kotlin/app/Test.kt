package app

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import java.security.Key


fun decryptJwtToken(token: String, secretKey: String): Claims? {
    return try {
        // Создаем ключ из секретной строки
        val key: Key = Keys.hmacShaKeyFor(secretKey.toByteArray())

        // Дешифруем токен и получаем его содержимое
        Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    } catch (e: Exception) {
        println("Ошибка при дешифровке токена: ${e.message}")
        null
    }
}

fun main() {
    val secretKey = "jakslhdALHlashd3672689264993469yfy9s9sfdiwsohf9y392ihoawhdoawawdKAdwljadopq314pj12" // Замените на ваш секретный ключ
    val token = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6ImdyaWdvcnkuYXJAZ21haWwuY29tIiwiZ2l2ZW5fbmFtZSI6IkdyZWciLCJuYmYiOjE3MzMzNDA5NTcsImV4cCI6MTczMzk0NTc1NywiaWF0IjoxNzMzMzQwOTU3LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjUyNTciLCJhdWQiOiJodHRwOi8vbG9jYWxob3N0OjUyNTcifQ.zLDJ3RE6X4WGP_kv6V_SduVbYW9FWeQTgRFpgf0M6dfaMZd12Me10WaHa_8uFPKVcVdLlP-9072899H5Jx8P6A" // Замените на ваш JWT токен
    val claims = decryptJwtToken(token, secretKey)
    claims?.let {
        println("Дешифрованные данные: $it")
    }
}
package domain.auth

sealed interface LoginResult {
    object NoUser: LoginResult
    object BadPassword: LoginResult
    class Ok(val userId: String): LoginResult
}
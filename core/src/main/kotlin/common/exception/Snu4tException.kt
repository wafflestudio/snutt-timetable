package com.wafflestudio.snu4t.common.exception

open class Snu4tException(
    val error: ErrorType = ErrorType.DEFAULT_ERROR,
    val errorMessage: String = error.errorMessage,
    val displayMessage: String = error.displayMessage,
) : RuntimeException(errorMessage)

object WrongApiKeyException : Snu4tException(ErrorType.WRONG_API_KEY)
object NoUserTokenException : Snu4tException(ErrorType.NO_USER_TOKEN)
object WrongUserTokenException : Snu4tException(ErrorType.WRONG_USER_TOKEN)
object InvalidLocalIdException : Snu4tException(ErrorType.INVALID_LOCAL_ID)
object InvalidPasswordException : Snu4tException(ErrorType.INVALID_PASSWORD)
object InvalidEmailException : Snu4tException(ErrorType.INVALID_EMAIL)
object DuplicateLocalIdException : Snu4tException(ErrorType.DUPLICATE_LOCAL_ID)

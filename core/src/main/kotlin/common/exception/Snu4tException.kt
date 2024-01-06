package com.wafflestudio.snu4t.common.exception

open class Snu4tException(
    val error: ErrorType = ErrorType.DEFAULT_ERROR,
    val errorMessage: String = error.errorMessage,
    val displayMessage: String = error.displayMessage,
    // TODO: 구버전 대응용 ext 필드. 추후 삭제
    val ext: Map<String, String> = mapOf(),
) : RuntimeException(errorMessage)

object InvalidTimeException : Snu4tException(ErrorType.INVALID_TIME)
object InvalidTimetableTitleException : Snu4tException(ErrorType.INVALID_TIMETABLE_TITLE)
object WrongApiKeyException : Snu4tException(ErrorType.WRONG_API_KEY)
object NoUserTokenException : Snu4tException(ErrorType.NO_USER_TOKEN)
object WrongUserTokenException : Snu4tException(ErrorType.WRONG_USER_TOKEN)
object UserNotAdminException : Snu4tException(ErrorType.USER_NOT_ADMIN)
object WrongLocalIdException : Snu4tException(ErrorType.WRONG_LOCAL_ID)
object WrongPasswordException : Snu4tException(ErrorType.WRONG_PASSWORD)

object InvalidLocalIdException : Snu4tException(ErrorType.INVALID_LOCAL_ID)
object InvalidPasswordException : Snu4tException(ErrorType.INVALID_PASSWORD)
object DuplicateLocalIdException : Snu4tException(ErrorType.DUPLICATE_LOCAL_ID)
object InvalidEmailException : Snu4tException(ErrorType.INVALID_EMAIL)

object LectureNotFoundException : Snu4tException(ErrorType.LECTURE_NOT_FOUND)
object UserNotFoundException : Snu4tException(ErrorType.USER_NOT_FOUND)

class MissingRequiredParameterException(fieldName: String) :
    Snu4tException(ErrorType.MISSING_PARAMETER, "필수값이 누락되었습니다. ($fieldName)")

class InvalidPathParameterException(fieldName: String) :
    Snu4tException(ErrorType.INVALID_PARAMETER, "잘못된 값입니다. (path parameter: $fieldName)")

class InvalidQueryParameterException(fieldName: String) :
    Snu4tException(ErrorType.INVALID_PARAMETER, "잘못된 값입니다. (query parameter: $fieldName)")

class InvalidBodyFieldValueException(fieldName: String) :
    Snu4tException(ErrorType.INVALID_BODY_FIELD_VALUE, "잘못된 값입니다. (request body: $fieldName)")

object InvalidOsTypeException : Snu4tException(ErrorType.INVALID_OS_TYPE)
object InvalidAppTypeException : Snu4tException(ErrorType.INVALID_APP_TYPE)
object InvalidNicknameException : Snu4tException(ErrorType.INVALID_NICKNAME)
object InvalidDisplayNameException : Snu4tException(ErrorType.INVALID_DISPLAY_NAME)
object TableDeleteErrorException : Snu4tException(ErrorType.TABLE_DELETE_ERROR)

object NoUserFcmKeyException : Snu4tException(ErrorType.NO_USER_FCM_KEY)
object InvalidRegistrationForPreviousSemesterCourseException :
    Snu4tException(ErrorType.INVALID_REGISTRATION_FOR_PREVIOUS_SEMESTER_COURSE)

object DuplicateTimetableTitleException : Snu4tException(ErrorType.DUPLICATE_TIMETABLE_TITLE)
object DuplicateTimetableLectureException : Snu4tException(ErrorType.DUPLICATE_LECTURE)
object WrongSemesterException : Snu4tException(ErrorType.WRONG_SEMESTER)
class LectureTimeOverlapException(confirmMessage: String) : Snu4tException(
    error = ErrorType.LECTURE_TIME_OVERLAP,
    displayMessage = confirmMessage,
    ext = mapOf("confirm_message" to confirmMessage)
)
object CustomLectureResetException : Snu4tException(ErrorType.CANNOT_RESET_CUSTOM_LECTURE)
object TimetableNotFoundException : Snu4tException(ErrorType.TIMETABLE_NOT_FOUND)
object PrimaryTimetableNotFoundException : Snu4tException(ErrorType.TIMETABLE_NOT_FOUND)
object TimetableNotPrimaryException : Snu4tException(ErrorType.DEFAULT_ERROR)
object ConfigNotFoundException : Snu4tException(ErrorType.CONFIG_NOT_FOUND)
object FriendNotFoundException : Snu4tException(ErrorType.FRIEND_NOT_FOUND)
object UserNotFoundByNicknameException : Snu4tException(ErrorType.USER_NOT_FOUND_BY_NICKNAME)

object DuplicateVacancyNotificationException : Snu4tException(ErrorType.DUPLICATE_VACANCY_NOTIFICATION)
object DuplicateEmailException : Snu4tException(ErrorType.DUPLICATE_EMAIL)
object DuplicateFriendException : Snu4tException(ErrorType.DUPLICATE_FRIEND)
object InvalidFriendException : Snu4tException(ErrorType.INVALID_FRIEND)

object DynamicLinkGenerationFailedException : Snu4tException(ErrorType.DYNAMIC_LINK_GENERATION_FAILED)

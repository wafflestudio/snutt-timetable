package com.wafflestudio.snu4t.common.exception

import org.springframework.http.HttpStatus

enum class ErrorType(
    val httpStatus: HttpStatus,
    val errorCode: Long,
    val errorMessage: String,
    val displayMessage: String = "현재 서비스 이용이 원활하지 않습니다. 이용에 불편을 드려 죄송합니다.",
) {
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 0x0000, "API 호출에 실패하였습니다."),

    INVALID_TIME(HttpStatus.BAD_REQUEST, 0x100C, "time이 유효하지 않습니다."),
    INVALID_TIMETABLE_TITLE(HttpStatus.BAD_REQUEST, 0x1007, "should provide title"),

    WRONG_API_KEY(HttpStatus.FORBIDDEN, 0x2000, "API 키가 잘못되었습니다."),
    NO_USER_TOKEN(HttpStatus.UNAUTHORIZED, 0x2001, "유저 토큰이 존재하지 않습니다."),
    WRONG_USER_TOKEN(HttpStatus.FORBIDDEN, 0x2002, "유저 토큰이 유효하지 않습니다", "로그인이 필요합니다."),
    USER_NOT_ADMIN(HttpStatus.FORBIDDEN, 0x2003, "어드민 권한이 없습니다."),
    WRONG_LOCAL_ID(HttpStatus.FORBIDDEN, 0x2004, "잘못된 id 입니다."),
    WRONG_PASSWORD(HttpStatus.FORBIDDEN, 0x2005, "잘못된 password 입니다."),

    INVALID_LOCAL_ID(HttpStatus.FORBIDDEN, 0x3000, "localId가 유효하지 않습니다.", "아이디는 4~32자의 영문자와 숫자로 이루어져야 합니다."),
    INVALID_PASSWORD(HttpStatus.FORBIDDEN, 0x3001, "password가 유효하지 않습니다.", "비밀번호는 6~20자로 영문자와 숫자를 모두 포함해야 합니다."),
    DUPLICATE_LOCAL_ID(HttpStatus.FORBIDDEN, 0x3002, "localId가 중복되었습니다.", "이미 사용 중인 아이디입니다."),
    DUPLICATE_TIMETABLE_TITLE(HttpStatus.FORBIDDEN, 0x3003, "timetable title이 중복되었습니다.", "이미 사용중인 시간표 이름입니다."),
    DUPLICATE_LECTURE(HttpStatus.FORBIDDEN, 0x3004, "duplicate lecture", "duplicate lecture"),
    WRONG_SEMESTER(HttpStatus.FORBIDDEN, 0x300A, "잘못된 학기입니다."),
    LECTURE_TIME_OVERLAP(HttpStatus.FORBIDDEN, 0x300C, "강의 시간이 겹칩니다."),
    CANNOT_RESET_CUSTOM_LECTURE(HttpStatus.FORBIDDEN, 0x300D, "cannot reset custom lectures"),
    INVALID_EMAIL(HttpStatus.FORBIDDEN, 0x300F, "email이 유효하지 않습니다.", "이메일 형식이 올바르지 않습니다."),

    LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND, 0x4003, "lecture가 없습니다.", "해당 강의는 존재하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 0x4004, "user가 없습니다.", "해당 유저는 존재하지 않습니다."),

    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, 40000, "파라미터 누락"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, 40001, "파라미터 값 오류"),
    INVALID_BODY_FIELD_VALUE(HttpStatus.BAD_REQUEST, 40002, "요청 바디 값 오류"),
    INVALID_REGISTRATION_FOR_PREVIOUS_SEMESTER_COURSE(HttpStatus.BAD_REQUEST, 40005, "이전 학기에는 빈자리 알림을 등록할 수 없습니다."),
    INVALID_OS_TYPE(HttpStatus.BAD_REQUEST, 40006, "잘못된 OS 타입입니다."),
    INVALID_APP_TYPE(HttpStatus.BAD_REQUEST, 40007, "잘못된 APP 타입입니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, 40008, "nickname이 유효하지 않습니다.", "조건에 맞지 않는 이름입니다."),
    INVALID_DISPLAY_NAME(HttpStatus.BAD_REQUEST, 40009, "displayName이 유효하지 않습니다.", "조건에 맞지 않는 이름입니다."),
    TABLE_DELETE_ERROR(HttpStatus.BAD_REQUEST, 40010, "하나 남은 시간표는 삭제할 수 없습니다."),
    TIMETABLE_NOT_PRIMARY(HttpStatus.BAD_REQUEST, 40011, "대표 시간표가 아닙니다."),
    INVALID_THEME_COLOR_COUNT(HttpStatus.BAD_REQUEST, 40012, "테마의 색상 개수가 적절하지 않습니다.", "테마의 색상 개수가 적절하지 않습니다."),
    DEFAULT_THEME_DELETE_ERROR(HttpStatus.BAD_REQUEST, 40013, "default 테마는 삭제할 수 없습니다.", "default 테마는 삭제할 수 없습니다."),
    NOT_DEFAULT_THEME_ERROR(HttpStatus.BAD_REQUEST, 40014, "default 테마가 아닙니다.", "default 테마가 아닙니다."),
    TOO_MANY_FILES(HttpStatus.BAD_REQUEST, 40015, "파일 개수가 너무 많습니다.", "파일 개수가 너무 많습니다."),
    EMAIL_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, 40016, "이미 메일 인증이 완료된 유저입니다.", "이미 메일 인증이 완료된 유저입니다."),
    TOO_MANY_VERIFICATION_CODE_REQUEST(HttpStatus.BAD_REQUEST, 40017, "인증 코드 요청이 너무 많습니다.", "인증 코드 요청이 너무 많습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, 40018, "인증 코드가 유효하지 않습니다.", "인증 코드가 유효하지 않습니다."),
    ALREADY_LOCAL_ACCOUNT(HttpStatus.BAD_REQUEST, 40019, "이미 로컬 계정이 존재합니다.", "이미 로컬 계정이 존재합니다."),

    SOCIAL_CONNECT_FAIL(HttpStatus.UNAUTHORIZED, 40100, "소셜 로그인에 실패했습니다.", "소셜 로그인에 실패했습니다."),

    TIMETABLE_NOT_FOUND(HttpStatus.NOT_FOUND, 40400, "timetable_id가 유효하지 않습니다", "존재하지 않는 시간표입니다."),
    PRIMARY_TIMETABLE_NOT_FOUND(HttpStatus.NOT_FOUND, 40401, "timetable_id가 유효하지 않습니다", "대표 시간표가 존재하지 않습니다."),
    NO_USER_FCM_KEY(HttpStatus.NOT_FOUND, 40402, "유저 FCM 키가 존재하지 않습니다."),
    CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, 40403, "config가 존재하지 않습니다."),
    FRIEND_NOT_FOUND(HttpStatus.NOT_FOUND, 40404, "친구 관계가 존재하지 않습니다.", "친구 관계가 존재하지 않습니다."),
    USER_NOT_FOUND_BY_NICKNAME(HttpStatus.NOT_FOUND, 40405, "해당 닉네임의 유저를 찾을 수 없습니다.", "해당 닉네임의 유저를 찾을 수 없습니다."),
    THEME_NOT_FOUND(HttpStatus.NOT_FOUND, 40406, "테마를 찾을 수 없습니다.", "테마를 찾을 수 없습니다."),
    EV_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, 40407, "강의평 데이터를 찾을 수 없습니다.", "강의평 데이터를 찾을 수 없습니다."),

    DUPLICATE_VACANCY_NOTIFICATION(HttpStatus.CONFLICT, 40900, "빈자리 알림 중복"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, 40901, "이미 사용 중인 이메일입니다.", "이미 사용 중인 이메일입니다."),
    DUPLICATE_FRIEND(HttpStatus.CONFLICT, 40902, "이미 친구 관계이거나 친구 요청을 보냈습니다.", "이미 친구 관계이거나 친구 요청을 보냈습니다."),
    INVALID_FRIEND(HttpStatus.CONFLICT, 40903, "친구 요청을 보낼 수 없는 유저입니다.", "친구 요청을 보낼 수 없는 유저입니다."),
    DUPLICATE_THEME_NAME(HttpStatus.CONFLICT, 40904, "중복된 테마 이름입니다.", "중복된 테마 이름입니다."),
    INVALID_THEME_TYPE(HttpStatus.CONFLICT, 40905, "적절하지 않은 유형의 테마입니다.", "적절하지 않은 유형의 테마입니다."),
    DUPLICATE_POPUP_KEY(HttpStatus.CONFLICT, 40906, "중복된 팝업 키입니다.", "중복된 팝업 키입니다."),

    DYNAMIC_LINK_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 50001, "링크 생성 실패", "링크 생성에 실패했습니다. 잠시 후 다시 시도해주세요."),
}

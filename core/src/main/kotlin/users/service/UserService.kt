package com.wafflestudio.snu4t.users.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.wafflestudio.snu4t.auth.SocialProvider
import com.wafflestudio.snu4t.common.cache.Cache
import com.wafflestudio.snu4t.common.cache.CacheKey
import com.wafflestudio.snu4t.common.exception.AlreadyLocalAccountException
import com.wafflestudio.snu4t.common.exception.DuplicateEmailException
import com.wafflestudio.snu4t.common.exception.DuplicateLocalIdException
import com.wafflestudio.snu4t.common.exception.EmailAlreadyVerifiedException
import com.wafflestudio.snu4t.common.exception.InvalidEmailException
import com.wafflestudio.snu4t.common.exception.InvalidLocalIdException
import com.wafflestudio.snu4t.common.exception.InvalidPasswordException
import com.wafflestudio.snu4t.common.exception.InvalidVerificationCodeException
import com.wafflestudio.snu4t.common.exception.Snu4tException
import com.wafflestudio.snu4t.common.exception.TooManyVerificationCodeRequestException
import com.wafflestudio.snu4t.common.exception.UserNotFoundException
import com.wafflestudio.snu4t.common.exception.WrongLocalIdException
import com.wafflestudio.snu4t.common.exception.WrongPasswordException
import com.wafflestudio.snu4t.common.exception.WrongUserTokenException
import com.wafflestudio.snu4t.mail.data.UserMailType
import com.wafflestudio.snu4t.mail.service.MailService
import com.wafflestudio.snu4t.notification.service.DeviceService
import com.wafflestudio.snu4t.timetables.service.TimetableService
import com.wafflestudio.snu4t.users.data.Credential
import com.wafflestudio.snu4t.users.data.RedisVerificationValue
import com.wafflestudio.snu4t.users.data.User
import com.wafflestudio.snu4t.users.dto.LocalLoginRequest
import com.wafflestudio.snu4t.users.dto.LocalRegisterRequest
import com.wafflestudio.snu4t.users.dto.LoginResponse
import com.wafflestudio.snu4t.users.dto.LogoutRequest
import com.wafflestudio.snu4t.users.dto.PasswordChangeRequest
import com.wafflestudio.snu4t.users.dto.SocialLoginRequest
import com.wafflestudio.snu4t.users.dto.TokenResponse
import com.wafflestudio.snu4t.users.dto.UserPatchRequest
import com.wafflestudio.snu4t.users.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.getAndAwait
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.Base64
import kotlin.random.Random

interface UserService {
    suspend fun getUser(userId: String): User

    suspend fun patchUserInfo(userId: String, userPatchRequest: UserPatchRequest): User

    suspend fun getUserByCredentialHash(credentialHash: String): User

    suspend fun registerLocal(localRegisterRequest: LocalRegisterRequest): LoginResponse

    suspend fun loginLocal(localRegisterRequest: LocalLoginRequest): LoginResponse

    suspend fun loginFacebook(socialLoginRequest: SocialLoginRequest): LoginResponse

    suspend fun loginGoogle(socialLoginRequest: SocialLoginRequest): LoginResponse

    suspend fun loginKakao(socialLoginRequest: SocialLoginRequest): LoginResponse

    suspend fun logout(userId: String, logoutRequest: LogoutRequest)

    suspend fun update(user: User): User

    suspend fun sendVerificationCode(user: User, email: String)

    suspend fun verifyEmail(user: User, code: String)

    suspend fun resetEmailVerification(user: User)

    suspend fun attachLocal(user: User, localLoginRequest: LocalLoginRequest): TokenResponse

    suspend fun changePassword(user: User, passwordChangeRequest: PasswordChangeRequest): TokenResponse

    suspend fun sendLocalIdToEmail(email: String)

    suspend fun sendResetPasswordCode(email: String)

    suspend fun verifyResetPasswordCode(localId: String, code: String)

    suspend fun getMaskedEmail(localId: String): String

    suspend fun resetPassword(localId: String, newPassword: String, code: String)
}

@Service
class UserServiceImpl(
    private val authService: AuthService,
    private val timetableService: TimetableService,
    private val deviceService: DeviceService,
    private val userRepository: UserRepository,
    private val userNicknameService: UserNicknameService,
    private val cache: Cache,
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val mapper: ObjectMapper,
    private val mailService: MailService,
) : UserService {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun getUser(userId: String): User {
        return userRepository.findByIdAndActiveTrue(userId) ?: throw UserNotFoundException
    }

    override suspend fun patchUserInfo(userId: String, userPatchRequest: UserPatchRequest): User {
        val user = getUser(userId)

        with(userPatchRequest) {
            nickname?.trim()?.let {
                val prevNickname = userNicknameService.getNicknameDto(user.nickname!!).nickname
                if (it != prevNickname) {
                    user.nickname = userNicknameService.appendNewTag(it)
                }
            }
        }

        return userRepository.save(user)
    }

    override suspend fun getUserByCredentialHash(credentialHash: String): User =
        userRepository.findByCredentialHashAndActive(credentialHash, true) ?: throw WrongUserTokenException

    override suspend fun registerLocal(localRegisterRequest: LocalRegisterRequest): LoginResponse {
        val localId = localRegisterRequest.id
        val password = localRegisterRequest.password
        val email = localRegisterRequest.email

        val cacheKey = CacheKey.LOCK_REGISTER_LOCAL.build(localId)

        runCatching {
            if (!cache.acquireLock(cacheKey)) throw DuplicateLocalIdException

            if (!authService.isValidLocalId(localId)) throw InvalidLocalIdException
            if (!authService.isValidPassword(password)) throw InvalidPasswordException
            email?.let {
                if (!authService.isValidEmail(email)) throw InvalidEmailException
                userRepository.findByEmailAndIsEmailVerifiedTrueAndActiveTrue(email)?.let {
                    throw DuplicateEmailException(getSocialProvider(it))
                }
            }

            if (userRepository.existsByCredentialLocalIdAndActiveTrue(localId)) throw DuplicateLocalIdException

            val credential = authService.buildLocalCredential(localId, password)
            return signup(credential, email, isEmailVerified = false)
        }.getOrElse {
            if (it is Snu4tException) cache.releaseLock(cacheKey)
            throw it
        }
    }

    override suspend fun loginLocal(localRegisterRequest: LocalLoginRequest): LoginResponse {
        val localId = localRegisterRequest.id
        val password = localRegisterRequest.password

        val user = userRepository.findByCredentialLocalIdAndActiveTrue(localId) ?: throw WrongLocalIdException

        if (!authService.isMatchedPassword(user, password)) throw WrongPasswordException

        return LoginResponse(
            userId = user.id!!,
            token = user.credentialHash,
        )
    }

    override suspend fun loginFacebook(socialLoginRequest: SocialLoginRequest): LoginResponse {
        val token = socialLoginRequest.token
        val oauth2UserResponse = authService.socialLoginWithAccessToken(SocialProvider.FACEBOOK, token)

        val user = userRepository.findByCredentialFbIdAndActiveTrue(oauth2UserResponse.socialId)

        if (user != null) {
            return LoginResponse(
                userId = user.id!!,
                token = user.credentialHash,
            )
        }

        val credential = authService.buildFacebookCredential(oauth2UserResponse)

        if (oauth2UserResponse.email != null) {
            userRepository.findByEmailAndIsEmailVerifiedTrueAndActiveTrue(oauth2UserResponse.email)?.let {
                throw DuplicateEmailException(getSocialProvider(it))
            }
        } else {
            log.warn("facebook email is null: $oauth2UserResponse")
        }

        return signup(credential, oauth2UserResponse.email, oauth2UserResponse.isEmailVerified)
    }

    override suspend fun loginGoogle(socialLoginRequest: SocialLoginRequest): LoginResponse {
        val token = socialLoginRequest.token
        val oauth2UserResponse = authService.socialLoginWithAccessToken(SocialProvider.GOOGLE, token)

        val user = userRepository.findByCredentialGoogleSubAndActiveTrue(oauth2UserResponse.socialId)

        if (user != null) {
            return LoginResponse(
                userId = user.id!!,
                token = user.credentialHash,
            )
        }

        checkNotNull(oauth2UserResponse.email) { "google email is null: $oauth2UserResponse" }
        userRepository.findByEmailAndIsEmailVerifiedTrueAndActiveTrue(oauth2UserResponse.email)?.let {
            throw DuplicateEmailException(getSocialProvider(it))
        }

        val credential = authService.buildGoogleCredential(oauth2UserResponse)

        return signup(credential, oauth2UserResponse.email, oauth2UserResponse.isEmailVerified)
    }

    override suspend fun loginKakao(socialLoginRequest: SocialLoginRequest): LoginResponse {
        val token = socialLoginRequest.token
        val oauth2UserResponse = authService.socialLoginWithAccessToken(SocialProvider.KAKAO, token)

        val user = userRepository.findByCredentialKakaoSubAndActiveTrue(oauth2UserResponse.socialId)

        if (user != null) {
            return LoginResponse(
                userId = user.id!!,
                token = user.credentialHash,
            )
        }

        checkNotNull(oauth2UserResponse.email) { "kakao email is null: $oauth2UserResponse" }
        userRepository.findByEmailAndIsEmailVerifiedTrueAndActiveTrue(oauth2UserResponse.email)?.let {
            throw DuplicateEmailException(getSocialProvider(it))
        }

        val credential = authService.buildKakaoCredential(oauth2UserResponse)

        return signup(credential, oauth2UserResponse.email, oauth2UserResponse.isEmailVerified)
    }

    private fun getSocialProvider(user: User): SocialProvider {
        return when {
            user.credential.fbId != null -> SocialProvider.FACEBOOK
            user.credential.appleSub != null -> SocialProvider.APPLE
            user.credential.googleSub != null -> SocialProvider.GOOGLE
            user.credential.kakaoSub != null -> SocialProvider.KAKAO
            user.credential.localId != null -> SocialProvider.LOCAL
            else -> throw IllegalStateException("Unknown social provider")
        }
    }

    private suspend fun signup(
        credential: Credential,
        email: String?,
        isEmailVerified: Boolean,
    ): LoginResponse {
        val credentialHash = authService.generateCredentialHash(credential)

        val randomNickname = userNicknameService.generateUniqueRandomNickname()

        val user = User(
            email = email,
            isEmailVerified = if (email != null) isEmailVerified else false,
            credential = credential,
            credentialHash = credentialHash,
            nickname = randomNickname,
            fcmKey = null,
        ).let { userRepository.save(it) }

        timetableService.createDefaultTable(user.id!!)

        return LoginResponse(
            userId = user.id,
            token = credentialHash,
        )
    }

    override suspend fun logout(userId: String, logoutRequest: LogoutRequest) {
        val user = userRepository.findByIdAndActiveTrue(userId) ?: throw UserNotFoundException
        deviceService.removeRegistrationId(user.id!!, logoutRequest.registrationId)
    }

    override suspend fun update(user: User): User {
        return userRepository.save(user)
    }

    override suspend fun sendVerificationCode(user: User, email: String) {
        if (user.isEmailVerified == true) throw EmailAlreadyVerifiedException
        if (!authService.isValidEmail(email)) throw InvalidEmailException
        if (userRepository.existsByEmailAndIsEmailVerifiedTrueAndActiveTrue(email)) throw DuplicateEmailException(getSocialProvider(user))
        val key = VERIFICATION_CODE_PREFIX + user.id
        val code = (Math.random() * 1000000).toInt().toString().padStart(6, '0')
        saveNewVerificationValue(email, code, key)
        mailService.sendUserMail(type = UserMailType.VERIFICATION, to = email, code = code)
    }

    override suspend fun verifyEmail(user: User, code: String) {
        val key = VERIFICATION_CODE_PREFIX + user.id
        val value = checkVerificationValue(key, code)
        user.apply {
            email = value.email
            isEmailVerified = true
        }
        userRepository.save(user)
        redisTemplate.delete(key).subscribe()
    }

    override suspend fun resetEmailVerification(user: User) {
        user.isEmailVerified = false
        userRepository.save(user)
    }

    override suspend fun attachLocal(user: User, localLoginRequest: LocalLoginRequest): TokenResponse {
        if (user.credential.localId != null) throw AlreadyLocalAccountException
        val localId = localLoginRequest.id
        val password = localLoginRequest.password
        if (!authService.isValidLocalId(localId)) throw InvalidLocalIdException
        if (!authService.isValidPassword(password)) throw InvalidPasswordException
        if (userRepository.existsByCredentialLocalIdAndActiveTrue(localId)) throw DuplicateLocalIdException
        val localCredential = authService.buildLocalCredential(localId, password)
        user.apply {
            credential.localId = localCredential.localId
            credential.localPw = localCredential.localPw
            credentialHash = authService.generateCredentialHash(credential)
        }
        userRepository.save(user)
        return TokenResponse(token = user.credentialHash)
    }

    override suspend fun changePassword(user: User, passwordChangeRequest: PasswordChangeRequest): TokenResponse {
        if (!authService.isMatchedPassword(user, passwordChangeRequest.oldPassword)) throw WrongPasswordException
        if (!authService.isValidPassword(passwordChangeRequest.newPassword)) throw InvalidPasswordException
        user.apply {
            credential.localPw = authService.buildLocalCredential(user.credential.localId!!, passwordChangeRequest.newPassword).localPw
            credentialHash = authService.generateCredentialHash(credential)
        }
        userRepository.save(user)
        return TokenResponse(token = user.credentialHash)
    }

    override suspend fun sendLocalIdToEmail(email: String) {
        if (!authService.isValidEmail(email)) throw InvalidEmailException
        val user = userRepository.findByEmailAndActiveTrue(email) ?: throw UserNotFoundException
        mailService.sendUserMail(type = UserMailType.FIND_ID, to = email, localId = user.credential.localId ?: throw UserNotFoundException)
    }

    override suspend fun sendResetPasswordCode(email: String) {
        if (!authService.isValidEmail(email)) throw InvalidEmailException
        val user = userRepository.findByEmailAndActiveTrue(email) ?: throw UserNotFoundException
        val key = RESET_PASSWORD_CODE_PREFIX + user.id
        val code = Base64.getUrlEncoder().encodeToString(Random.nextBytes(6))
        saveNewVerificationValue(email, code, key)
        mailService.sendUserMail(type = UserMailType.PASSWORD_RESET, to = email, code = code)
    }

    override suspend fun verifyResetPasswordCode(localId: String, code: String) {
        val user = userRepository.findByCredentialLocalIdAndActiveTrue(localId) ?: throw UserNotFoundException
        val key = RESET_PASSWORD_CODE_PREFIX + user.id
        checkVerificationValue(key, code)
        redisTemplate.opsForValue().getAndExpire(key, Duration.ofMinutes(3)).subscribe()
    }

    override suspend fun getMaskedEmail(localId: String): String {
        val user = userRepository.findByCredentialLocalIdAndActiveTrue(localId) ?: throw UserNotFoundException
        val email = user.email ?: throw UserNotFoundException
        val maskedEmail = email.replace(emailMaskRegex, "*")
        return maskedEmail
    }

    override suspend fun resetPassword(localId: String, newPassword: String, code: String) {
        val user = userRepository.findByCredentialLocalIdAndActiveTrue(localId) ?: throw UserNotFoundException
        verifyResetPasswordCode(localId, code)
        if (!authService.isValidPassword(newPassword)) throw InvalidPasswordException
        user.apply {
            credential.localPw = authService.buildLocalCredential(user.credential.localId!!, newPassword).localPw
            credentialHash = authService.generateCredentialHash(user.credential)
        }
        userRepository.save(user)
        redisTemplate.delete(RESET_PASSWORD_CODE_PREFIX + user.id).subscribe()
    }

    private suspend fun saveNewVerificationValue(email: String, code: String, key: String): RedisVerificationValue {
        val existing = readVerificationValue(key)
        if (existing != null && existing.count > 4) throw TooManyVerificationCodeRequestException
        val value = RedisVerificationValue(email, code, (existing?.count ?: 0) + 1)
        redisTemplate.opsForValue()
            .set(key, mapper.writeValueAsString(value), Duration.ofMinutes(3))
            .subscribe()
        return value
    }

    private suspend fun checkVerificationValue(key: String, code: String): RedisVerificationValue {
        val value = readVerificationValue(key)
        if (value == null || value.code != code) throw InvalidVerificationCodeException
        return value
    }

    private suspend fun readVerificationValue(key: String): RedisVerificationValue? {
        return redisTemplate.opsForValue().getAndAwait(key)?.let {
            mapper.readValue<RedisVerificationValue>(it)
        }
    }

    companion object {
        private val emailMaskRegex = Regex("(?<=.{3}).(?=.*@)")
        const val VERIFICATION_CODE_PREFIX = "verification-code-"
        const val RESET_PASSWORD_CODE_PREFIX = "reset-password-code-"
    }
}

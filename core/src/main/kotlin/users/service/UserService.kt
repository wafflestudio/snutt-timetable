package com.wafflestudio.snu4t.users.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.wafflestudio.snu4t.auth.AuthProvider
import com.wafflestudio.snu4t.auth.OAuth2UserResponse
import com.wafflestudio.snu4t.common.cache.Cache
import com.wafflestudio.snu4t.common.cache.CacheKey
import com.wafflestudio.snu4t.common.exception.AlreadyLocalAccountException
import com.wafflestudio.snu4t.common.exception.AlreadySocialAccountException
import com.wafflestudio.snu4t.common.exception.CannotRemoveLastAuthProviderException
import com.wafflestudio.snu4t.common.exception.DuplicateEmailException
import com.wafflestudio.snu4t.common.exception.DuplicateLocalIdException
import com.wafflestudio.snu4t.common.exception.DuplicateSocialAccountException
import com.wafflestudio.snu4t.common.exception.EmailAlreadyVerifiedException
import com.wafflestudio.snu4t.common.exception.InvalidEmailException
import com.wafflestudio.snu4t.common.exception.InvalidLocalIdException
import com.wafflestudio.snu4t.common.exception.InvalidPasswordException
import com.wafflestudio.snu4t.common.exception.InvalidVerificationCodeException
import com.wafflestudio.snu4t.common.exception.Snu4tException
import com.wafflestudio.snu4t.common.exception.SocialProviderNotAttachedException
import com.wafflestudio.snu4t.common.exception.TooManyVerificationCodeRequestException
import com.wafflestudio.snu4t.common.exception.UserNotFoundException
import com.wafflestudio.snu4t.common.exception.WrongLocalIdException
import com.wafflestudio.snu4t.common.exception.WrongPasswordException
import com.wafflestudio.snu4t.common.exception.WrongUserTokenException
import com.wafflestudio.snu4t.mail.data.UserMailType
import com.wafflestudio.snu4t.mail.service.MailService
import com.wafflestudio.snu4t.notification.service.DeviceService
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
import com.wafflestudio.snu4t.users.event.data.SignupEvent
import com.wafflestudio.snu4t.users.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.getAndAwait
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.Base64
import kotlin.random.Random

interface UserService {
    suspend fun getUser(userId: String): User

    suspend fun getUsers(userIds: List<String>): List<User>

    suspend fun patchUserInfo(
        userId: String,
        userPatchRequest: UserPatchRequest,
    ): User

    suspend fun getUserByCredentialHash(credentialHash: String): User

    suspend fun registerLocal(localRegisterRequest: LocalRegisterRequest): LoginResponse

    suspend fun loginLocal(localRegisterRequest: LocalLoginRequest): LoginResponse

    suspend fun socialLogin(
        socialLoginRequest: SocialLoginRequest,
        authProvider: AuthProvider,
    ): LoginResponse

    suspend fun logout(
        userId: String,
        logoutRequest: LogoutRequest,
    )

    suspend fun update(user: User): User

    suspend fun sendVerificationCode(
        user: User,
        email: String,
    )

    suspend fun verifyEmail(
        user: User,
        code: String,
    )

    suspend fun resetEmailVerification(user: User)

    suspend fun attachLocal(
        user: User,
        localLoginRequest: LocalLoginRequest,
    ): TokenResponse

    suspend fun attachSocial(
        user: User,
        socialLoginRequest: SocialLoginRequest,
        authProvider: AuthProvider,
    ): TokenResponse

    suspend fun detachSocial(
        user: User,
        authProvider: AuthProvider,
    ): TokenResponse

    suspend fun changePassword(
        user: User,
        passwordChangeRequest: PasswordChangeRequest,
    ): TokenResponse

    suspend fun sendLocalIdToEmail(email: String)

    suspend fun sendResetPasswordCode(email: String)

    suspend fun verifyResetPasswordCode(
        localId: String,
        code: String,
    )

    suspend fun getMaskedEmail(localId: String): String

    suspend fun resetPassword(
        localId: String,
        newPassword: String,
        code: String,
    )
}

@Service
class UserServiceImpl(
    private val authService: AuthService,
    private val deviceService: DeviceService,
    private val userRepository: UserRepository,
    private val userNicknameService: UserNicknameService,
    private val cache: Cache,
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val mapper: ObjectMapper,
    private val mailService: MailService,
    private val eventPublisher: ApplicationEventPublisher,
) : UserService {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun getUser(userId: String): User {
        return userRepository.findByIdAndActiveTrue(userId) ?: throw UserNotFoundException
    }

    override suspend fun getUsers(userIds: List<String>): List<User> {
        return userRepository.findAllByIdInAndActiveTrue(userIds)
    }

    override suspend fun patchUserInfo(
        userId: String,
        userPatchRequest: UserPatchRequest,
    ): User {
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

        return runCatching {
            if (!cache.acquireLock(cacheKey)) throw DuplicateLocalIdException

            if (!authService.isValidLocalId(localId)) throw InvalidLocalIdException
            if (!authService.isValidPassword(password)) throw InvalidPasswordException
            email?.let {
                if (!authService.isValidEmail(email)) throw InvalidEmailException
                userRepository.findByEmailIgnoreCaseAndIsEmailVerifiedTrueAndActiveTrue(email)?.let {
                    throw DuplicateEmailException(getAttachedAuthProviders(it))
                }
            }

            if (userRepository.existsByCredentialLocalIdAndActiveTrue(localId)) throw DuplicateLocalIdException

            val credential = authService.buildLocalCredential(localId, password)
            val user = signup(credential, email, isEmailVerified = false)

            LoginResponse(
                userId = user.id!!,
                token = user.credentialHash,
            )
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

    override suspend fun socialLogin(
        socialLoginRequest: SocialLoginRequest,
        authProvider: AuthProvider,
    ): LoginResponse {
        val token = socialLoginRequest.token
        val oauth2UserResponse = authService.socialLoginWithAccessToken(authProvider, token)

        val user =
            when (authProvider) {
                AuthProvider.FACEBOOK ->
                    userRepository.findByCredentialFbIdAndActiveTrue(oauth2UserResponse.socialId) ?: signupFacebook(oauth2UserResponse)
                AuthProvider.GOOGLE ->
                    userRepository.findByCredentialGoogleSubAndActiveTrue(oauth2UserResponse.socialId) ?: signupGoogle(oauth2UserResponse)
                AuthProvider.KAKAO ->
                    userRepository.findByCredentialKakaoSubAndActiveTrue(oauth2UserResponse.socialId) ?: signupKakao(oauth2UserResponse)
                AuthProvider.APPLE -> {
                    if (oauth2UserResponse.transferInfo != null) {
                        transferAppleCredential(oauth2UserResponse.transferInfo, oauth2UserResponse.socialId, oauth2UserResponse.email)
                    }
                    userRepository.findByCredentialAppleSubAndActiveTrue(oauth2UserResponse.socialId) ?: signupApple(oauth2UserResponse)
                }
                AuthProvider.LOCAL -> throw IllegalStateException("local login is not supported in this method")
            }
        return LoginResponse(
            userId = user.id!!,
            token = user.credentialHash,
        )
    }

    private suspend fun signupFacebook(oauth2UserResponse: OAuth2UserResponse): User {
        if (oauth2UserResponse.email != null) {
            userRepository.findByEmailIgnoreCaseAndIsEmailVerifiedTrueAndActiveTrue(oauth2UserResponse.email)?.let {
                throw DuplicateEmailException(getAttachedAuthProviders(it))
            }
        } else {
            log.warn("facebook email is null: $oauth2UserResponse")
        }

        val credential = authService.buildFacebookCredential(oauth2UserResponse)
        return signup(credential, oauth2UserResponse.email, false)
    }

    private suspend fun signupGoogle(oauth2UserResponse: OAuth2UserResponse): User {
        checkNotNull(oauth2UserResponse.email) { "google email is null: $oauth2UserResponse" }
        userRepository.findByEmailIgnoreCaseAndIsEmailVerifiedTrueAndActiveTrue(oauth2UserResponse.email)?.let {
            throw DuplicateEmailException(getAttachedAuthProviders(it))
        }

        val credential = authService.buildGoogleCredential(oauth2UserResponse)
        return signup(credential, oauth2UserResponse.email, false)
    }

    private suspend fun signupKakao(oauth2UserResponse: OAuth2UserResponse): User {
        checkNotNull(oauth2UserResponse.email) { "kakao email is null: $oauth2UserResponse" }
        userRepository.findByEmailIgnoreCaseAndIsEmailVerifiedTrueAndActiveTrue(oauth2UserResponse.email)?.let {
            throw DuplicateEmailException(getAttachedAuthProviders(it))
        }

        val credential = authService.buildKakaoCredential(oauth2UserResponse)
        return signup(credential, oauth2UserResponse.email, false)
    }

    private suspend fun signupApple(oauth2UserResponse: OAuth2UserResponse): User {
        checkNotNull(oauth2UserResponse.email) { "apple email is null: $oauth2UserResponse" }
        userRepository.findByEmailIgnoreCaseAndIsEmailVerifiedTrueAndActiveTrue(oauth2UserResponse.email)?.let {
            throw DuplicateEmailException(getAttachedAuthProviders(it))
        }

        val credential = authService.buildAppleCredential(oauth2UserResponse)
        return signup(credential, oauth2UserResponse.email, false)
    }

    private suspend fun transferAppleCredential(
        transferSub: String,
        sub: String,
        email: String?,
    ) {
        val exisitingUser = userRepository.findByCredentialAppleTransferSubAndActiveTrue(transferSub)
        if (exisitingUser != null) {
            exisitingUser.credential.apply {
                appleSub = sub
                appleEmail = email
            }
            exisitingUser.credentialHash = authService.generateCredentialHash(exisitingUser.credential)
            userRepository.save(exisitingUser)
        }
    }

    private fun getAttachedAuthProviders(user: User): List<AuthProvider> =
        listOfNotNull(
            user.credential.localId?.let { AuthProvider.LOCAL },
            user.credential.fbId?.let { AuthProvider.FACEBOOK },
            user.credential.googleSub?.let { AuthProvider.GOOGLE },
            user.credential.kakaoSub?.let { AuthProvider.KAKAO },
            user.credential.appleSub?.let { AuthProvider.APPLE },
        )

    private suspend fun signup(
        credential: Credential,
        email: String?,
        isEmailVerified: Boolean,
    ): User {
        val credentialHash = authService.generateCredentialHash(credential)
        val randomNickname = userNicknameService.generateUniqueRandomNickname()

        val user =
            User(
                email = email,
                isEmailVerified = if (email != null) isEmailVerified else false,
                credential = credential,
                credentialHash = credentialHash,
                nickname = randomNickname,
                fcmKey = null,
            ).let { userRepository.save(it) }

        eventPublisher.publishEvent(SignupEvent(user.id!!))
        return user
    }

    override suspend fun logout(
        userId: String,
        logoutRequest: LogoutRequest,
    ) {
        val user = userRepository.findByIdAndActiveTrue(userId) ?: throw UserNotFoundException
        deviceService.removeRegistrationId(user.id!!, logoutRequest.registrationId)
    }

    override suspend fun update(user: User): User {
        return userRepository.save(user)
    }

    override suspend fun sendVerificationCode(
        user: User,
        email: String,
    ) {
        if (user.isEmailVerified == true) throw EmailAlreadyVerifiedException
        if (!authService.isValidSnuMail(email)) throw InvalidEmailException
        if (userRepository.existsByEmailIgnoreCaseAndIsEmailVerifiedTrueAndActiveTrue(
                email,
            )
        ) {
            throw DuplicateEmailException(getAttachedAuthProviders(user))
        }
        val key = VERIFICATION_CODE_PREFIX + user.id
        val code = (Math.random() * 1000000).toInt().toString().padStart(6, '0')
        saveNewVerificationValue(email, code, key)
        mailService.sendUserMail(type = UserMailType.VERIFICATION, to = email, code = code)
    }

    override suspend fun verifyEmail(
        user: User,
        code: String,
    ) {
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

    override suspend fun attachLocal(
        user: User,
        localLoginRequest: LocalLoginRequest,
    ): TokenResponse {
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

    override suspend fun attachSocial(
        user: User,
        socialLoginRequest: SocialLoginRequest,
        authProvider: AuthProvider,
    ): TokenResponse {
        val token = socialLoginRequest.token
        val oauth2UserResponse = authService.socialLoginWithAccessToken(authProvider, token)
        if (oauth2UserResponse.email != null) {
            val presentUser = userRepository.findByEmailIgnoreCaseAndIsEmailVerifiedTrueAndActiveTrue(oauth2UserResponse.email)
            if (presentUser != null && presentUser.id != user.id) {
                throw DuplicateEmailException(getAttachedAuthProviders(presentUser))
            }
        }

        when (authProvider) {
            AuthProvider.FACEBOOK -> {
                if (user.credential.fbId != null) throw AlreadySocialAccountException
                if (userRepository.existsByCredentialFbIdAndActiveTrue(oauth2UserResponse.socialId)) {
                    throw DuplicateSocialAccountException
                }
                val facebookCredential = authService.buildFacebookCredential(oauth2UserResponse)
                user.credential.apply {
                    fbId = facebookCredential.fbId
                    fbName = facebookCredential.fbName
                }
            }
            AuthProvider.GOOGLE -> {
                if (user.credential.googleSub != null) throw AlreadySocialAccountException
                if (userRepository.existsByCredentialGoogleSubAndActiveTrue(oauth2UserResponse.socialId)) {
                    throw DuplicateSocialAccountException
                }
                val googleCredential = authService.buildGoogleCredential(oauth2UserResponse)
                user.credential.apply {
                    googleSub = googleCredential.googleSub
                    googleEmail = googleCredential.googleEmail
                }
            }
            AuthProvider.KAKAO -> {
                if (user.credential.kakaoSub != null) throw AlreadySocialAccountException
                if (userRepository.existsByCredentialKakaoSubAndActiveTrue(oauth2UserResponse.socialId)) {
                    throw DuplicateSocialAccountException
                }
                val kakaoCredential = authService.buildKakaoCredential(oauth2UserResponse)
                user.credential.apply {
                    kakaoSub = kakaoCredential.kakaoSub
                    kakaoEmail = kakaoCredential.kakaoEmail
                }
            }
            AuthProvider.APPLE -> throw IllegalStateException("Apple login is not supported")
            AuthProvider.LOCAL -> throw IllegalStateException("Cannot attach local account")
        }

        user.credentialHash = authService.generateCredentialHash(user.credential)
        userRepository.save(user)
        return TokenResponse(token = user.credentialHash)
    }

    override suspend fun detachSocial(
        user: User,
        authProvider: AuthProvider,
    ): TokenResponse {
        val attachedAuthProviders = getAttachedAuthProviders(user)
        if (!attachedAuthProviders.contains(authProvider)) throw SocialProviderNotAttachedException
        if (attachedAuthProviders.size == 1) throw CannotRemoveLastAuthProviderException
        when (authProvider) {
            AuthProvider.FACEBOOK -> {
                user.credential.apply {
                    fbId = null
                    fbName = null
                }
            }
            AuthProvider.GOOGLE -> {
                user.credential.apply {
                    googleSub = null
                    googleEmail = null
                }
            }
            AuthProvider.KAKAO -> {
                user.credential.apply {
                    kakaoSub = null
                    kakaoEmail = null
                }
            }
            AuthProvider.APPLE -> {
                user.credential.apply {
                    appleSub = null
                    appleEmail = null
                    appleTransferSub = null
                }
            }
            AuthProvider.LOCAL -> throw IllegalStateException("Cannot detach local account")
        }
        user.credentialHash = authService.generateCredentialHash(user.credential)
        userRepository.save(user)
        return TokenResponse(token = user.credentialHash)
    }

    override suspend fun changePassword(
        user: User,
        passwordChangeRequest: PasswordChangeRequest,
    ): TokenResponse {
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
        val user = userRepository.findByEmailIgnoreCaseAndIsEmailVerifiedTrueAndActiveTrue(email) ?: throw UserNotFoundException
        mailService.sendUserMail(type = UserMailType.FIND_ID, to = email, localId = user.credential.localId ?: throw UserNotFoundException)
    }

    override suspend fun sendResetPasswordCode(email: String) {
        val user = userRepository.findByEmailIgnoreCaseAndIsEmailVerifiedTrueAndActiveTrue(email) ?: throw UserNotFoundException
        val key = RESET_PASSWORD_CODE_PREFIX + user.id
        val code = Base64.getUrlEncoder().encodeToString(Random.nextBytes(6))
        saveNewVerificationValue(email, code, key)
        mailService.sendUserMail(type = UserMailType.PASSWORD_RESET, to = email, code = code)
    }

    override suspend fun verifyResetPasswordCode(
        localId: String,
        code: String,
    ) {
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

    override suspend fun resetPassword(
        localId: String,
        newPassword: String,
        code: String,
    ) {
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

    private suspend fun saveNewVerificationValue(
        email: String,
        code: String,
        key: String,
    ): RedisVerificationValue {
        val existing = readVerificationValue(key)
        if (existing != null && existing.count > 4) throw TooManyVerificationCodeRequestException
        val value = RedisVerificationValue(email, code, (existing?.count ?: 0) + 1)
        redisTemplate.opsForValue()
            .set(key, mapper.writeValueAsString(value), Duration.ofMinutes(3))
            .subscribe()
        return value
    }

    private suspend fun checkVerificationValue(
        key: String,
        code: String,
    ): RedisVerificationValue {
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

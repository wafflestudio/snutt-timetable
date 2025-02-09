package com.wafflestudio.snutt.users.repository

import com.wafflestudio.snutt.users.data.User
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface UserRepository : CoroutineCrudRepository<User, String> {
    suspend fun findByIdAndActiveTrue(id: String): User?

    suspend fun findByIdInAndActiveTrue(ids: List<String>): List<User>

    suspend fun findByCredentialHashAndActive(
        credentialHash: String,
        active: Boolean,
    ): User?

    suspend fun existsByCredentialLocalIdAndActiveTrue(localId: String): Boolean

    suspend fun findByCredentialLocalIdAndActiveTrue(localId: String): User?

    suspend fun findByCredentialFbIdAndActiveTrue(fbId: String): User?

    suspend fun findByCredentialGoogleSubAndActiveTrue(googleSub: String): User?

    suspend fun findByCredentialKakaoSubAndActiveTrue(kakaoSub: String): User?

    suspend fun findByCredentialAppleSubAndActiveTrue(appleSub: String): User?

    suspend fun findByNicknameAndActiveTrue(nickname: String): User?

    suspend fun findAllByIdInAndActiveTrue(ids: List<String>): List<User>

    suspend fun existsByEmailIgnoreCaseAndIsEmailVerifiedTrueAndActiveTrue(email: String): Boolean

    suspend fun findByEmailIgnoreCaseAndIsEmailVerifiedTrueAndActiveTrue(email: String): User?

    suspend fun existsByCredentialFbIdAndActiveTrue(fbId: String): Boolean

    suspend fun existsByCredentialGoogleSubAndActiveTrue(googleSub: String): Boolean

    suspend fun existsByCredentialKakaoSubAndActiveTrue(kakaoSub: String): Boolean

    suspend fun existsByCredentialAppleSubAndActiveTrue(appleSub: String): Boolean

    fun findAllByNicknameStartingWith(nickname: String): Flow<User>

    suspend fun findByCredentialAppleTransferSubAndActiveTrue(appleTransferSub: String): User?
}

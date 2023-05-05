package com.wafflestudio.snu4t.users.repository

import com.wafflestudio.snu4t.users.data.User
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface UserRepository : CoroutineCrudRepository<User, String> {
    suspend fun findByCredentialHashAndActive(credentialHash: String, active: Boolean): User?

    suspend fun existsByCredentialLocalIdAndActiveTrue(localId: String): Boolean

    suspend fun findByCredentialLocalIdAndActiveTrue(localId: String): User?
}

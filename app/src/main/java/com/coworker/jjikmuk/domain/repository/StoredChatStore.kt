package com.coworker.jjikmuk.domain.repository

import com.coworker.jjikmuk.domain.model.ChatConversation
import com.coworker.jjikmuk.domain.model.ChatResponse
import com.coworker.jjikmuk.feature.auth.domain.AuthResult

data class StoredChatQuestion(val conversationId: Long, val clientMessageId: String,
    val message: String, val barcode: String? = null, val externalAiConsent: Boolean = true)
data class StoredChatTurn(val id: Long, val clientMessageId: String, val message: String,
    val barcode: String?, val status: String, val response: ChatResponse? = null, val errorCode: String? = null, val replayed: Boolean = false)
data class StoredChatRooms(val items: List<ChatConversation>, val page: Int, val hasNext: Boolean)
data class StoredChatTurns(val items: List<StoredChatTurn>, val nextAfterId: Long, val hasNext: Boolean)
interface StoredChatStore {
    fun captureAccount(): FamilyAccount?
    suspend fun create(account: FamilyAccount, clientId: String, title: String): AuthResult<ChatConversation>
    suspend fun rooms(account: FamilyAccount, page: Int): AuthResult<StoredChatRooms>
    suspend fun room(account: FamilyAccount, id: Long): AuthResult<ChatConversation>
    suspend fun turns(account: FamilyAccount, id: Long, after: Long): AuthResult<StoredChatTurns>
    suspend fun turn(account: FamilyAccount, id: Long, clientId: String): AuthResult<StoredChatTurn>
    suspend fun turnById(account: FamilyAccount, id: Long, turnId: Long, clientId: String): AuthResult<StoredChatTurn> = turn(account, id, clientId)
    suspend fun send(account: FamilyAccount, question: StoredChatQuestion): AuthResult<StoredChatTurn>
    suspend fun delete(account: FamilyAccount, id: Long): AuthResult<Unit>
}

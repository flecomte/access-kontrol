package io.github.flecomte

/** Responses of AccessKontrol */
enum class AccessDecision {
    GRANTED,
    DENIED;

    /**
     * Convert decision to boolean
     */
    fun toBoolean(): Boolean = when (this) {
        GRANTED -> true
        DENIED -> false
    }
}

abstract class AccessKontrol {
    /**
     * A Shortcut for return a GrantedResponse
     */
    protected fun granted(message: String? = null, code: String? = null): GrantedResponse = GrantedResponse(this, message, code)

    /**
     * A Shortcut for return a DeniedResponse
     */
    protected fun denied(message: String, code: String): DeniedResponse = DeniedResponse(this, message, code)

    /**
     * A helper to convert a list of subject into one response
     */
    protected fun <S : List<T>, T> canAll(items: S, action: (T) -> AccessResponse): AccessResponses = items
        .map { action(it) }.let { responses ->
            if (responses.any { it is DeniedResponse }) {
                DeniedResponses(responses)
            } else {
                GrantedResponses(responses)
            }
        }
}

/**
 * Throw an Exception if AccessKontrol return a DENIED response
 */
fun <T : AccessKontrol> T.assert(action: T.() -> AccessResponse) {
    action().assert()
}

typealias AccessResponses = List<AccessResponse>

/**
 * Check all responses and return DENIED if one is DENIED
 *
 * If the list of responses is empty, return GRANTED
 */
fun AccessResponses.getFirstDecisionResponse(): AccessResponse = this.firstOrNull { it.decision == AccessDecision.DENIED } ?: this.first { it.decision == AccessDecision.GRANTED }

/**
 * Throw an Exception if one response is DENIED
 */
fun AccessResponses.assert() {
    if (!toBoolean()) {
        throw AccessDeniedException(this)
    }
}
val AccessResponses.grantedResponses get(): AccessResponses = this.filterIsInstance<GrantedResponse>()
val AccessResponses.deniedResponses get(): AccessResponses = this.filterIsInstance<DeniedResponse>()

/**
 * Convert responses as boolean
 */
fun AccessResponses.toBoolean(): Boolean = deniedResponses.isEmpty()

class AccessDeniedException(val accessResponses: AccessResponses) : Throwable(accessResponses.deniedResponses.first().message) {
    constructor(accessResponse: AccessResponse) : this(listOf(accessResponse))

    /**
     * Get first response
     */
    fun first(): AccessResponse = accessResponses.deniedResponses.first()

    /**
     * Check if the error code is present into the responses
     */
    fun hasErrorCode(code: String): Boolean = accessResponses
        .deniedResponses
        .any { it.code == code }

    /**
     * Find and return the response than match with the error code
     */
    fun getErrorCode(code: String): AccessResponse? = accessResponses
        .deniedResponses
        .firstOrNull { it.code == code }

    /**
     * Get a list of messages of all responses
     */
    fun getMessages(): List<String> = accessResponses
        .deniedResponses
        .map { it.message!! }

    /**
     * Get the first message
     */
    fun getFirstMessage(): String? = accessResponses
        .deniedResponses
        .first()
        .message
}

/**
 * The response that all AccessKontrol method return
 * @see GrantedResponse
 * @see DeniedResponse
 */
sealed class AccessResponse(
    val decision: AccessDecision,
    val accessControl: AccessKontrol,
    val message: String?,
    val code: String?
) {
    /**
     * Convert response as boolean
     */
    fun toBoolean(): Boolean = decision.toBoolean()

    /**
     * Throw Exception if response if DENIED
     */
    fun assert() {
        if (this.decision == AccessDecision.DENIED) {
            throw AccessDeniedException(this)
        }
    }
}

open class GrantedResponse(
    accessControl: AccessKontrol,
    message: String? = null,
    code: String? = null
) : AccessResponse(AccessDecision.GRANTED, accessControl, message, code)

open class DeniedResponse(
    accessControl: AccessKontrol,
    message: String,
    code: String
) : AccessResponse(AccessDecision.DENIED, accessControl, message, code)

class GrantedResponses(
    accessResponses: List<AccessResponse>
) : AccessResponses by accessResponses,
    GrantedResponse(
        accessResponses.grantedResponses.first().accessControl,
        accessResponses.grantedResponses.first().message,
        accessResponses.grantedResponses.first().code
    )

class DeniedResponses(
    accessResponses: List<AccessResponse>
) : AccessResponses by accessResponses,
    DeniedResponse(
        accessResponses.deniedResponses.first().accessControl,
        accessResponses.deniedResponses.firstOrNull()?.message ?: error("DeniedResponses cannot be empty"),
        accessResponses.deniedResponses.firstOrNull()?.code ?: error("DeniedResponses cannot be empty")
    )

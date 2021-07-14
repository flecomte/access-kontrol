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
     *
     * @throws [NoDecision] if the list of responses is empty
     */
    protected fun <S : List<T>, T> canAll(items: S, action: (T) -> AccessResponse): AccessResponses = items
        .ifEmpty { throw NoDecision() }
        .map { action(it) }
        .let { responses ->
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
 * @throws [NoDecision] if the list of responses is empty
 */
fun AccessResponses.getFirstDecisionResponse(): AccessResponse {
    ifEmpty { throw NoDecision() }
    return firstOrNull { it.decision == AccessDecision.DENIED } ?: first()
}

/**
 * Throw an Exception if one response is DENIED
 */
fun AccessResponses.assert() {
    if (!toBoolean()) {
        throw AccessDeniedException(this)
    }
}
val AccessResponses.grantedResponses get(): List<GrantedResponse> = this.filterIsInstance<GrantedResponse>()
val AccessResponses.deniedResponses get(): List<DeniedResponse> = this.filterIsInstance<DeniedResponse>()

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

    val deniedResponses: List<DeniedResponse>
        get() = this.accessResponses.deniedResponses

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
        .map { it.message }

    /**
     * Get the first message
     */
    fun getFirstMessage(): String = accessResponses
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
    open val message: String?,
    open val code: String?
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
    override val message: String,
    override val code: String
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
        accessResponses.deniedResponses.firstOrNull()?.accessControl ?: error("DeniedResponses cannot be empty"),
        accessResponses.deniedResponses.first().message,
        accessResponses.deniedResponses.first().code
    )

class NoDecision : RuntimeException("No decision has been taken")

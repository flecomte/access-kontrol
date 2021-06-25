package fr.dcproject.common.security

/** Responses of AccessControl */
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

abstract class AccessControl {
    /**
     * A Shortcut for return a GrantedResponse
     */
    protected fun granted(message: String? = null, code: String? = null): GrantedResponse = GrantedResponse(this, message, code)

    /**
     * A Shortcut for return a DeniedResponse
     */
    protected fun denied(message: String, code: String): DeniedResponse = DeniedResponse(this, message, code)

    /**
     * Check all responses and return DENIED if one is DENIED
     *
     * If the list of responses is empty, return GRANTED
     */
    private fun AccessResponses.getOneResponse(): AccessResponse = this.firstOrNull { it.decision == AccessDecision.DENIED } ?: granted()

    /**
     * A helper to convert a list of subject into one response
     */
    protected fun <S : List<T>, T> canAll(items: S, action: (T) -> AccessResponse): AccessResponse = items
        .map { action(it) }
        .getOneResponse()
}

/**
 * Throw an Exception if AccessControl return a DENIED response
 */
fun <T : AccessControl> T.assert(action: T.() -> AccessResponse) {
    action().assert()
}

/**
 * Check all responses and return DENIED if one is DENIED
 *
 * If the list of responses is empty, return GRANTED
 */
fun AccessResponses.getOneResponse(): AccessResponse = this.firstOrNull { it.decision == AccessDecision.DENIED } ?: GrantedResponse(first().accessControl)

/**
 * Throw an Exception if one response is DENIED
 */
fun AccessResponses.assert() = this.getOneResponse().assert()

class AccessDeniedException(private val accessResponses: AccessResponses) : Throwable(accessResponses.first().message) {
    constructor(accessResponse: AccessResponse) : this(listOf(accessResponse))

    /**
     * Get first response
     */
    fun first(): AccessResponse = accessResponses.first()

    /**
     * Check if the error code is present into the responses
     */
    fun hasErrorCode(code: String): Boolean = accessResponses
        .filter { it.decision == AccessDecision.DENIED }
        .any { it.code == code }

    /**
     * Find and return the response than match with the error code
     */
    fun getErrorCode(code: String): AccessResponse? = accessResponses
        .firstOrNull { it.decision == AccessDecision.DENIED && it.code == code }

    /**
     * Get a list of messages of all responses
     */
    fun getMessages(): List<String> = accessResponses
        .mapNotNull { it.message }

    /**
     * Get the first message
     */
    fun getFirstMessage(): String? = accessResponses
        .first()
        .message
}

/**
 * The response that all AccessControl method return
 * @see GrantedResponse
 * @see DeniedResponse
 */
sealed class AccessResponse(
    val decision: AccessDecision,
    val accessControl: AccessControl,
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

class GrantedResponse(
    accessControl: AccessControl,
    message: String? = null,
    code: String? = null
) : AccessResponse(AccessDecision.GRANTED, accessControl, message, code)

class DeniedResponse(
    accessControl: AccessControl,
    message: String,
    code: String
) : AccessResponse(AccessDecision.DENIED, accessControl, message, code)

typealias AccessResponses = List<AccessResponse>

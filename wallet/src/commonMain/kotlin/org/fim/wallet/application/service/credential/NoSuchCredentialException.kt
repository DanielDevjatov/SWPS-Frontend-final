package org.fim.wallet.application.service.credential

import org.fim.wallet.domain.credential.Credential

/**
 * Exception that will be thrown if a query for a specific [Credential] returns null.
 * This Exception should be used at the service level, not at the repository level.
 *
 * @param msg   the error message that describes the exception
 * @param cause optional exception received from the database
 */
class NoSuchCredentialException(msg: String, cause: Throwable? = null) : Exception(msg, cause)

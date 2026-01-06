package org.fim.wallet.domain

import org.fim.wallet.domain.credential.CredentialFactory

/**
 * Exception that will be thrown if [CreationExtras] passed to the [CredentialFactory] is missing a required value.
 *
 * @param msg   the error message that describes the exception
 * @param cause optional exception that caused this exception
 */
class MissingCreationExtraException(msg: String, cause: Throwable? = null): Exception(msg, cause)

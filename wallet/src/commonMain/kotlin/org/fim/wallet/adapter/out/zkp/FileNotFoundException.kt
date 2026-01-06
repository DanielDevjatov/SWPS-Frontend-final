package org.fim.wallet.adapter.out.zkp

/**
 * Multiplatform alternative for java.io.FileNotFoundException.
 *
 * @param msg   the error message
 * @param cause optional Exception that caused this Exception
 */
class FileNotFoundException(msg: String?, cause: Throwable? = null) : Exception(msg, cause)

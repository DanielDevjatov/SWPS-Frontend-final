package org.fim.wallet.application.port.`in`.credential

import org.fim.wallet.domain.credential.Credential
import org.fim.wallet.domain.eddsa.PublicKey
import org.fim.wallet.domain.eddsa.Signature

/**
 * Use-case to verify the [Signature] of a given [Credential].
 */
interface VerifyCredentialUseCase {

  /**
   * Verify the [Signature] of a credential.
   *
   * This function verifies the signature together with the [PublicKey] of the issuer and the other credential
   * properties given in the credential.
   *
   * @return `true` if the signature is valid, `false` otherwise
   */
  fun <T : Credential> verifyCredential(credential: T): Boolean
}

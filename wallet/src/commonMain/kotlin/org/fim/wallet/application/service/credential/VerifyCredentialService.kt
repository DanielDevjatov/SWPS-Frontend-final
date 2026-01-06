package org.fim.wallet.application.service.credential

import org.fim.wallet.application.port.`in`.credential.VerifyCredentialUseCase
import org.fim.wallet.application.port.out.EDDSASignatureProvider
import org.fim.wallet.domain.credential.Credential


/**
 * Implementation of the [VerifyCredentialUseCase] using an [EDDSASignatureProvider].
 *
 * @property eddsaService Dependency injection of the [EDDSASignatureProvider] to be used
 *
 * @see VerifyCredentialUseCase
 * @see EDDSASignatureProvider
 */
class VerifyCredentialService(private val eddsaService: EDDSASignatureProvider) : VerifyCredentialUseCase {
  override fun <T : Credential> verifyCredential(credential: T): Boolean {
    val tree = credential.generateTree()
    return eddsaService.verify(tree.root(),credential.signature,credential.issuer)
  }
}

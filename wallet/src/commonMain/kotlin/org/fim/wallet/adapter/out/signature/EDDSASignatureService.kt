package org.fim.wallet.adapter.out.signature

import org.fim.wallet.application.port.out.EDDSASignatureProvider

/**
 * Expected platform-specific implementation for [EDDSASignatureProvider] port.
 *
 * **Note:**
 * The invoke-operator was added because the JS implementation builds async, so a "suspend constructor" was necessary.
 */
expect class EDDSASignatureService : EDDSASignatureProvider {
  companion object {
    suspend operator fun invoke(): EDDSASignatureService
  }
}

package org.fim.wallet.adapter.out.zkp

import org.fim.wallet.application.port.out.ZKPProvider

/**
 * Implementation of [ZKPProvider] using the SnarkJS library.
 */
expect class SnarkJSService : ZKPProvider {
  companion object {
    fun getInstance(): SnarkJSService
  }
}

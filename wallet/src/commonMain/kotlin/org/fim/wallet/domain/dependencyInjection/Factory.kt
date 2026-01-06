package org.fim.wallet.domain.dependencyInjection

/**
 * Implementation of [Dependency] that will return a new instance of the injected service each time.
 *
 * The [Dependency] will init a new service with the given lambda every time [getInstance] is called.
 *
 * @property service initialization lambda for the injected service.
 */
internal class Factory<T>(private val service: () -> T) : Dependency<T> {
  override fun getInstance(): T = service()
}

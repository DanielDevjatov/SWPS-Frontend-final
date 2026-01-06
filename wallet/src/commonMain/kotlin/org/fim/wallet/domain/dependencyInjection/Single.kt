package org.fim.wallet.domain.dependencyInjection

/**
 * Implementation of [Dependency] for injected Singletons.
 *
 * The [Dependency] will init the internal singleton with the given lambda.
 *
 * @property service initialization lambda for the injected service.
 */
internal class Single<T> (private val service: () -> T) : Dependency<T> {
  private var _instance: T? = null

  override fun getInstance(): T = _instance ?: service().also { _instance = it }
}

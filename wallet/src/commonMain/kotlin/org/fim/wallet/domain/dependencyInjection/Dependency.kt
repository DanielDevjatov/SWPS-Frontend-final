package org.fim.wallet.domain.dependencyInjection

/**
 * Parent interface all types of dependencies injected through the [DependencyProvider] must implement.
 *
 * @param T type of injected service
 */
internal interface Dependency<T> {
  /**
   * Get the instance of the injected service of Type [T]
   *
   * @return the injected implementation
   */
  fun getInstance(): T
}

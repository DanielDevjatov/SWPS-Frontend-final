package org.fim.wallet.application.port.`in`.credential

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import org.fim.wallet.domain.credential.Credential

/**
 * Use-case to store (generic) Credentials inside the wallet.
 */
interface StoreCredentialUseCase <T: Credential> {

  /**
   * Store a given [Credential] inside the wallet.
   *
   * Example of usage:
   * ```
   *    val service = StoreCredentialService(InMemoryGenericCredentialRepository())
   *    val credential = PrequalificationCredential()
   *
   *    service.storeCredential(credential)
   * ```
   *
   * @param credential the [Credential] instance to be stored
   */
  fun storeCredential(credential: T)

  /**
   * Parse a given [JsonElement] into the given [Credential] type and store the result inside the wallet.
   *
   * Example of usage:
   * ```
   *    val service = StoreCredentialService(InMemoryGenericCredentialRepository())
   *    val json = Json.encodeToJsonElement( /* ... */ )
   *
   *    service.storeCredential(PrequalificationCredential::class, json)
   * ```
   *
   * @param jsonElement     the [JsonElement] to be parsed
   *
   * @throws SerializationException   if the given JSON element is not a valid JSON input for the Credential implementation T
   * @throws IllegalArgumentException if the decoded input cannot be represented as a valid instance of Credential implementation T
   */
  fun storeCredential(jsonElement: JsonElement)
}

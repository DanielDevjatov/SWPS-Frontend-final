package org.fim.wallet.application.port.`in`.credential

import org.fim.wallet.adapter.`in`.module.Wallet
import org.fim.wallet.domain.CreationExtras
import org.fim.wallet.domain.MissingCreationExtraException
import org.fim.wallet.domain.credential.ConsentCredential
import org.fim.wallet.domain.credential.Credential
import org.fim.wallet.domain.crypto.HashableCreationExtras
import org.fim.wallet.domain.eddsa.PrivateKey
import org.fim.wallet.domain.eddsa.PublicKey
import org.fim.wallet.domain.poseidon.PoseidonHashable
import kotlin.reflect.KClass

/**
 * Use-case to issue a new credential with given parameters.
 */
interface IssueCredentialUseCase {

  /**
   * Returns a new [Credential] of the specified type and given parameters.
   *
   * The credentials signature will be created during this process.
   * The id and the timestamp of the credential are set by the system.
   *
   * Example of usage:
   * ```
   *    val service: IssueCredentialUseCase = IssueCredentialService()
   *    val credential = service.issueCredential( /*...*/, PrequalificationCredential::class)
   * ```
   *
   * @param issuer          the issuers (this [Wallet]) [PublicKey] used as unique id
   * @param holder          the holders [PublicKey] used as unique id
   * @param extras          [HashableCreationExtras] containing key-value pairs of [Credential] specific properties
   * @param credentialType  Class reflection of the [Credential] type to create
   * @param privateKey      The [PrivateKey] of this [Wallet] used to sign
   * @param privateKey2      optional second [PrivateKey] depending on the credential
   *
   * @return a new instance of the given [Credential] type
   *
   * @throws [MissingCreationExtraException] for invalid extras
   *
   * @see Credential
   * @see CreationExtras
   */
  fun <T: Credential> issueCredential(
    issuer: PublicKey,
    holder: PublicKey,
    extras: HashableCreationExtras<PoseidonHashable<out Any>>,
    credentialType: KClass<T>,
    privateKey: PrivateKey,
    privateKey2: PrivateKey? = null
  ): T
}

/**
 * Extension function for the IssueCredentialUseCase to create a new Credential with given parameters.
 * This function uses the reified idiom to avoid explicit declaration of the Credential type
 * and is a wrapper function around the issueCredential() method of IssueCredentialUseCase.
 *
 * Example of usage:
 * ```
 *    val service: IssueCredentialUseCase = IssueCredentialService()
 *    val credential: PrequalificationCredential = service.issueCredential( /*...*/)
 * ```
 * @receiver [IssueCredentialUseCase]
 *
 * @param issuer          the issuers unique id
 * @param holder          the holders unique id
 * @param extras          [HashableCreationExtras] containing key-value pairs of Credential specific properties
 * @param privateKey      The [PrivateKey] of this [Wallet] used to sign
 * @param privateKey2     optional second [PrivateKey] for secondary signatures, specified in the [Credential] when needed, e.g. [ConsentCredential]
 *
 * @return a new instance of the given Credential type
 *
 * @see IssueCredentialUseCase.issueCredential
 * @see Credential
 * @see CreationExtras
 */
inline fun <reified T : Credential> IssueCredentialUseCase.issueCredential(
  issuer: PublicKey,
  holder: PublicKey,
  extras: HashableCreationExtras<PoseidonHashable<out Any>>,
  privateKey: PrivateKey,
  privateKey2: PrivateKey? = null
): T = issueCredential(issuer, holder, extras, T::class, privateKey,privateKey2)

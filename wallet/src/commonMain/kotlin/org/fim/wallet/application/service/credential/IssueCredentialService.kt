package org.fim.wallet.application.service.credential

import org.fim.wallet.application.port.`in`.credential.IssueCredentialUseCase
import org.fim.wallet.application.port.out.EDDSASignatureProvider
import org.fim.wallet.domain.Timestamp
import org.fim.wallet.domain.credential.ConsentCredential
import org.fim.wallet.domain.credential.Credential
import org.fim.wallet.domain.credential.CredentialFactory
import org.fim.wallet.domain.crypto.HashableCreationExtras
import org.fim.wallet.domain.eddsa.PrivateKey
import org.fim.wallet.domain.eddsa.PublicKey
import org.fim.wallet.domain.poseidon.PoseidonHashable
import org.fim.wallet.domain.poseidon.toPoseidonHashable
import kotlin.reflect.KClass
import kotlin.reflect.createInstance
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Implementation of the [IssueCredentialUseCase] using the [CredentialFactory].
 *
 * @param eddsaService  injection of the [EDDSASignatureProvider] to be used for signatures
 *
 * @see IssueCredentialUseCase
 * @see CredentialFactory
 */
class IssueCredentialService(
  private val eddsaService: EDDSASignatureProvider
) : IssueCredentialUseCase {

  private val factory = CredentialFactory()

  @OptIn(ExperimentalJsReflectionCreateInstance::class, ExperimentalUuidApi::class)
  override fun <T : Credential> issueCredential(
    issuer: PublicKey,
    holder: PublicKey,
    extras: HashableCreationExtras<PoseidonHashable<out Any>>,
    credentialType: KClass<T>,
    privateKey: PrivateKey,
    privateKey2: PrivateKey?
  ): T {
    val id = Uuid.random()
    val timestamp = Timestamp.now()

    if (credentialType == ConsentCredential::class) {
      require(privateKey2 != null)
      val tree = credentialType.createInstance().generateTree(id, issuer, holder, timestamp, extras)
      val hemSignature = eddsaService.sign(tree.root(), privateKey2)
      extras[ConsentCredential::hemSignature] = hemSignature.toPoseidonHashable()
    }

    val tree = credentialType.createInstance().generateTree(id, issuer, holder, timestamp, extras)
    val root = tree.root()
    extras[Credential::root] = root.toPoseidonHashable()

    val signature = eddsaService.sign(root, privateKey)
    extras[Credential::signature] = signature.toPoseidonHashable()

    return factory.create(id, issuer, holder, signature, timestamp, root, extras, credentialType)
  }
}

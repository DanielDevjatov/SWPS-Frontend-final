@file:UseSerializers(BigIntegerSerializer::class, UuidSerializer::class)

package org.fim.wallet.domain.credential

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import org.fim.wallet.domain.Timestamp
import org.fim.wallet.domain.crypto.HashableCreationExtras
import org.fim.wallet.domain.eddsa.PublicKey
import org.fim.wallet.domain.eddsa.Signature
import org.fim.wallet.domain.poseidon.PoseidonHashable
import org.fim.wallet.domain.poseidon.toPoseidonHashable
import org.fim.wallet.domain.serialize.BigIntegerSerializer
import org.fim.wallet.domain.serialize.UuidSerializer
import kotlin.reflect.KProperty1
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Implementation of the Prequalification Credential.
 * For meta-data properties and serialization methods see the Credential interface.
 *
 * @property deviceID              the unique ID of the device this credential was issued for
 * @property prequalificationType  the type of pre-qualification granted by the issuer
 * @property gridConnectionArea    grid area the system is connected to
 *
 * @see Credential
 * @see PrequalificationTypes
 */
@Serializable
@SerialName("PrequalificationCredential")
class PrequalificationCredential internal constructor(
  override val id: Uuid,
  override val issuer: PublicKey,
  override val holder: PublicKey,
  override val signature: Signature,
  override val timestamp: Timestamp,
  override val root: BigInteger,
  val deviceID: BigInteger,
  val prequalificationType: PrequalificationTypes,
  val gridConnectionArea: String
) : Credential() {

  @Transient
  override val extras: HashableCreationExtras<PoseidonHashable<out Any>> =
    HashableCreationExtras(
      PrequalificationCredential::deviceID to deviceID.toPoseidonHashable(),
      PrequalificationCredential::prequalificationType to prequalificationType.toPoseidonHashable(),
      PrequalificationCredential::gridConnectionArea to gridConnectionArea.toPoseidonHashable()
    )

  override fun create(
    id: Uuid,
    issuer: PublicKey,
    holder: PublicKey,
    signature: Signature,
    timestamp: Timestamp,
    root: BigInteger,
    extras: HashableCreationExtras<PoseidonHashable<out Any>>
  ): Credential = PrequalificationCredential(
    id, issuer, holder, signature, timestamp, root,
    extras[PrequalificationCredential::deviceID].value,
    extras[PrequalificationCredential::prequalificationType].value,
    extras[PrequalificationCredential::gridConnectionArea].value
  )

  override fun toExtendedBigIntegerList(): List<BigInteger> {
    return this.toBigIntegerList() +
        this.baseToExtendedBigIntegerList() +
        deviceID
  }

  override fun members(): List<KProperty1<PrequalificationCredential, Any>> = MEMBERS

  private companion object {
    /**
     * Predefined list of properties used as return value of [members].
     *
     * @see [members]
     */
    @OptIn(ExperimentalUuidApi::class)
    val MEMBERS = listOf(
      PrequalificationCredential::id,
      PrequalificationCredential::issuer,
      PrequalificationCredential::holder,
      PrequalificationCredential::signature,
      PrequalificationCredential::timestamp,
      PrequalificationCredential::root,
      PrequalificationCredential::deviceID,
      PrequalificationCredential::prequalificationType,
      PrequalificationCredential::gridConnectionArea
    )
  }
}


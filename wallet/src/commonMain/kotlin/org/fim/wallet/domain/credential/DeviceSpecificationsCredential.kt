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
 * Implementation of the Device Specifications Credential.
 * For meta-data properties and serialization methods see the Credential interface.
 *
 * @param deviceName              readable name of the device
 * @param deviceID                the unique ID of the device this credential was issued for
 * @param deviceOEM               OEM name of the device
 * @param electronicDescription   description of electronic specifications for the device
 *
 * @see Credential
 */
@Serializable
@SerialName("DeviceSpecificationsCredential")
class DeviceSpecificationsCredential internal constructor(
  override val id: Uuid,                //1
  override val issuer: PublicKey,       //2,3
  override val holder: PublicKey,       //4,5
  override val signature: Signature,    //6,7,8
  override val timestamp: Timestamp,    //9
  override val root: BigInteger,        //10
  val deviceID: BigInteger,             //11
  val deviceName: String,               //12
  val deviceOEM: String,                //13
  val electronicDescription: String,    //14
) : Credential() {

  @Transient
  override val extras: HashableCreationExtras<PoseidonHashable<out Any>> =
    HashableCreationExtras(
      DeviceSpecificationsCredential::deviceID to deviceID.toPoseidonHashable(),
      DeviceSpecificationsCredential::deviceName to deviceName.toPoseidonHashable(),
      DeviceSpecificationsCredential::deviceOEM to deviceOEM.toPoseidonHashable(),
      DeviceSpecificationsCredential::electronicDescription to electronicDescription.toPoseidonHashable()
    )

  @OptIn(ExperimentalUuidApi::class)
  override fun create(
    id: Uuid,
    issuer: PublicKey,
    holder: PublicKey,
    signature: Signature,
    timestamp: Timestamp,
    root: BigInteger,
    extras: HashableCreationExtras<PoseidonHashable<out Any>>
  ): Credential = DeviceSpecificationsCredential(
    id, issuer, holder, signature, timestamp, root,
    extras[DeviceSpecificationsCredential::deviceID].value,
    extras[DeviceSpecificationsCredential::deviceName].value,
    extras[DeviceSpecificationsCredential::deviceOEM].value,
    extras[DeviceSpecificationsCredential::electronicDescription].value,
  )

  override fun toExtendedBigIntegerList(): List<BigInteger> {
    return this.toBigIntegerList() +
        this.baseToExtendedBigIntegerList() +
        deviceID
  }

  override fun members(): List<KProperty1<DeviceSpecificationsCredential, Any>> = MEMBERS

  private companion object {
    /**
     * Predefined list of properties used as return value of [members].
     *
     * @see [members]
     */
    @OptIn(ExperimentalUuidApi::class)
    val MEMBERS = listOf(
      DeviceSpecificationsCredential::id,
      DeviceSpecificationsCredential::issuer,
      DeviceSpecificationsCredential::holder,
      DeviceSpecificationsCredential::signature,
      DeviceSpecificationsCredential::timestamp,
      DeviceSpecificationsCredential::root,
      DeviceSpecificationsCredential::deviceID,
      DeviceSpecificationsCredential::deviceName,
      DeviceSpecificationsCredential::deviceOEM,
      DeviceSpecificationsCredential::electronicDescription
    )
  }
}

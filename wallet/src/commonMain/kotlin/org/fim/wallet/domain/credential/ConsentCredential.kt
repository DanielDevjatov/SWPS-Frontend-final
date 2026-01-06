@file:UseSerializers(BigIntegerSerializer::class, UuidSerializer::class)

package org.fim.wallet.domain.credential

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import org.fim.wallet.application.service.convertToBigInteger
import org.fim.wallet.domain.Timestamp
import org.fim.wallet.domain.crypto.HashableCreationExtras
import org.fim.wallet.domain.eddsa.PrivateKey
import org.fim.wallet.domain.eddsa.PublicKey
import org.fim.wallet.domain.eddsa.Signature
import org.fim.wallet.domain.eddsa.toBigIntegerList
import org.fim.wallet.domain.poseidon.PoseidonHashable
import org.fim.wallet.domain.poseidon.toPoseidonHashable
import org.fim.wallet.domain.serialize.BigIntegerSerializer
import org.fim.wallet.domain.serialize.UuidSerializer
import kotlin.reflect.KProperty1
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Implementation of the Consent Credential. Issuance requires secondary [PrivateKey].
 * For meta-data properties and serialization methods see the Credential interface.
 *
 * @param deviceID          the unique ID of the device this credential was issued for
 * @param hemIssuer         HEM [PublicKey]
 * @param hemSignature      HEM-generated [Signature]
 * @param flexibilityGive   amount of energy that can be inserted into the network (wh)
 * @param flexibilityReceive amount of energy that can be received from the network (wh)
 * @param consentForUsage   the type of usage granted by the issuer; overlaps with [PrequalificationTypes]
 * @param tStart            start of the granted time window
 * @param tEnd              end of the granted time window
 *
 * @see Credential
 * @see PrequalificationTypes
 */
@Serializable
@SerialName("ConsentCredential")
class ConsentCredential internal constructor(
  override val id: Uuid,                      //1
  override val issuer: PublicKey,             //2,3
  override val holder: PublicKey,             //4,5
  override val signature: Signature,          //6,7,8
  override val timestamp: Timestamp,          //9
  override val root: BigInteger,              //10
  val deviceID: BigInteger,                   //11
  val hemIssuer: PublicKey,                   //12,13
  val hemSignature: Signature,                //14,15,16
  val flexibilityGive: BigInteger,            //17
  val flexibilityReceive: BigInteger,         //18
  val consentForUsage: PrequalificationTypes, //19
  val tStart: Timestamp,                      //20
  val tEnd: Timestamp                         //21
) : Credential() {

  @Transient
  override val extras: HashableCreationExtras<PoseidonHashable<out Any>> =
    HashableCreationExtras(
      ConsentCredential::deviceID to deviceID.toPoseidonHashable(),
      ConsentCredential::hemIssuer to hemIssuer.toPoseidonHashable(),
      ConsentCredential::hemSignature to hemSignature.toPoseidonHashable(),
      ConsentCredential::flexibilityGive to flexibilityGive.toPoseidonHashable(),
      ConsentCredential::flexibilityReceive to flexibilityReceive.toPoseidonHashable(),
      ConsentCredential::consentForUsage to consentForUsage.toPoseidonHashable(),
      ConsentCredential::tStart to tStart.toPoseidonHashable(),
      ConsentCredential::tEnd to tEnd.toPoseidonHashable()
    )

  init {
    require(tStart <= tEnd) { "tStart must be less than or equal to tEnd." }
  }

  override fun create(
    id: Uuid,
    issuer: PublicKey,
    holder: PublicKey,
    signature: Signature,
    timestamp: Timestamp,
    root: BigInteger,
    extras: HashableCreationExtras<PoseidonHashable<out Any>>
  ): Credential = ConsentCredential(
    id, issuer, holder, signature, timestamp, root,
    extras[ConsentCredential::deviceID].value,
    extras[ConsentCredential::hemIssuer].value,
    extras[ConsentCredential::hemSignature].value,
    extras[ConsentCredential::flexibilityGive].value,
    extras[ConsentCredential::flexibilityReceive].value,
    extras[ConsentCredential::consentForUsage].value,
    extras[ConsentCredential::tStart].value,
    extras[ConsentCredential::tEnd].value,
  )

  override fun toExtendedBigIntegerList(): List<BigInteger> {
    return this.toBigIntegerList() + this.baseToExtendedBigIntegerList() +
        deviceID +
        hemIssuer.toBigIntegerList() +
        hemSignature.toBigIntegerList() +
        flexibilityGive +
        flexibilityReceive +
        tStart.convertToBigInteger() +
        tEnd.convertToBigInteger()
  }

  override fun members(): List<KProperty1<ConsentCredential, Any>> = MEMBERS

  private companion object {
    /**
     * Predefined list of properties used as return value of [members].
     *
     * @see [members]
     */
    @OptIn(ExperimentalUuidApi::class)
    val MEMBERS = listOf(
      ConsentCredential::id,
      ConsentCredential::issuer,
      ConsentCredential::holder,
      ConsentCredential::signature,
      ConsentCredential::timestamp,
      ConsentCredential::root,
      ConsentCredential::deviceID,
      ConsentCredential::hemIssuer,
      ConsentCredential::hemSignature,
      ConsentCredential::flexibilityGive,
      ConsentCredential::flexibilityReceive,
      ConsentCredential::consentForUsage,
      ConsentCredential::tStart,
      ConsentCredential::tEnd,
    )
  }
}

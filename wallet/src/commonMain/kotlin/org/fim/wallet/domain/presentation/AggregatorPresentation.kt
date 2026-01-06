@file:UseSerializers(BigIntegerSerializer::class, UuidSerializer::class)

package org.fim.wallet.domain.presentation

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.fim.wallet.domain.CreationExtras
import org.fim.wallet.domain.MissingCreationExtraException
import org.fim.wallet.domain.credential.ConsentCredential
import org.fim.wallet.domain.credential.Credential
import org.fim.wallet.domain.credential.DeviceSpecificationsCredential
import org.fim.wallet.domain.credential.PrequalificationCredential
import org.fim.wallet.domain.eddsa.PublicKey
import org.fim.wallet.domain.serialize.BigIntegerSerializer
import org.fim.wallet.domain.serialize.UuidSerializer
import org.fim.wallet.domain.zkp.CircuitInputs
import org.fim.wallet.domain.zkp.Proof
import kotlin.uuid.Uuid

/**
 * Implementation of the presentation, the aggregator presents the TSO.
 *
 * @property deviceSpecificationIssuer  [List] of [PublicKey]s used to sign the [DeviceSpecificationsCredential]s
 * @property prequalificationIssuer     [List] of [PublicKey]s used to sign the [PrequalificationCredential]s
 * @property flexibilityGiveSum         total energy that can be inserted into the network (wh)
 * @property flexibilityReceiveSum      total energy that can be removed from the network (wh)
 * @property tStart                     start of the timeframe (required in CreationExtra)
 * @property tEnd                       the end of the timeframe (required in CreationExtra)
 */
@Serializable
@SerialName("AggregatorPresentation")
class AggregatorPresentation internal constructor(
  override val publicSignals: List<BigInteger>,
  override val proof: Proof,
  override val id: Uuid,
  val deviceSpecificationIssuer: List<PublicKey>,
  val prequalificationIssuer: List<PublicKey>,
  val flexibilityGiveSum: BigInteger,
  val flexibilityReceiveSum: BigInteger,
  val tStart: BigInteger,
  val tEnd: BigInteger,
) : VerifiablePresentation() {

  override fun create(
    publicSignals: List<BigInteger>,
    proof: Proof,
    namedCreationExtras: CreationExtras?
  ): VerifiablePresentation {
    val agentCount: Int = (publicSignals.size - 4) / 4

    val deviceSpecificationIssuer = List(agentCount) {
      i -> PublicKey(publicSignals[3 + i] to publicSignals[4 + 2 * i])
    }

    val prequalificationIssuer = List(agentCount) {
      i -> PublicKey(publicSignals[3 + i + 2 * agentCount] to publicSignals[4 + 2 * i + 2 * agentCount])
    }

    return AggregatorPresentation(
      publicSignals,
      proof,
      Uuid.random(),
      deviceSpecificationIssuer,
      prequalificationIssuer,
      publicSignals[0],
      publicSignals[1],
      publicSignals[2],
      publicSignals[3],
    )
  }

  /**
   * Creates the [CircuitInputs] for an [AggregatorPresentation].
   *
   * @param credentials   [List] of [Credential]s to be included in the presentation.
   *                      Expects the Credentials to be in triples ([DeviceSpecificationsCredential], [PrequalificationCredential], [ConsentCredential]).
   * @param creationExtras  [CreationExtras] containing tStart and tEnd
   */
  override fun createCircuitInputs(credentials: List<Credential>, creationExtras: CreationExtras?): CircuitInputs {
    require((credentials.size % 3) == 0) {
      "The presentation requires three credentials per agent, but ${credentials.size} credentials were provided."
    }
    requireNotNull(creationExtras) { "The presentation requires the CreationExtras to contain tstart and tend." }

    val agentNumber = (credentials.size / 3)
    val deviceSpecificationsCredentialList = credentials.filterIsInstance<DeviceSpecificationsCredential>()
    val prequalificationCredentialList = credentials.filterIsInstance<PrequalificationCredential>()
    val consentCredentialList = credentials.filterIsInstance<ConsentCredential>()

    require(deviceSpecificationsCredentialList.size == agentNumber) {
      "The presentation requires $agentNumber DeviceSpecificationsCredentials, " +
          "but ${deviceSpecificationsCredentialList.size} DeviceSpecificationsCredentials were provided."
    }
    require(prequalificationCredentialList.size == agentNumber) {
      "The presentation requires $agentNumber PrequalificationCredentials, " +
          "but ${prequalificationCredentialList.size} PrequalificationCredentials were provided."
    }
    require(consentCredentialList.size == agentNumber) {
      "The presentation requires $agentNumber ConsentCredentials, " +
          "but ${consentCredentialList.size} ConsentCredentials were provided."
    }

    val timeframeStart = creationExtras[::tStart] as BigInteger?
      ?: throw MissingCreationExtraException("Missing value for tStart")
    val timeframeStop = creationExtras[::tEnd] as BigInteger?
      ?: throw MissingCreationExtraException("Missing value for tEnd")

    val inputs = emptyList<Pair<String, BigInteger>>().toMutableList()
    val arrayInputs = listOf(
      Pair(
        "deviceSpecificationCredentials",
        deviceSpecificationsCredentialList.flatMap { it.toExtendedBigIntegerList() }),
      Pair(
        "prequalificationCredentials",
        prequalificationCredentialList.flatMap { it.toExtendedBigIntegerList() }),
      Pair("consentCredentials", consentCredentialList.flatMap { it.toExtendedBigIntegerList() }),
      Pair("timeframe", listOf(timeframeStart, timeframeStop)),
    )
    return CircuitInputs(inputs, arrayInputs)
  }

  override fun circuitName(): String = "aggregatedFlexibilityPresentation"
}

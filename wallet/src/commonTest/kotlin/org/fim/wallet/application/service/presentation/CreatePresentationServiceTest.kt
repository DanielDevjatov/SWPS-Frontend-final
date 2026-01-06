package org.fim.wallet.application.service.presentation

import com.ionspin.kotlin.bignum.integer.BigInteger
import createRandomCredentialTrio
import kotlinx.coroutines.test.runTest
import org.fim.wallet.adapter.out.zkp.SnarkJSService
import org.fim.wallet.adapter.out.zkp.terminateSnarkJS
import org.fim.wallet.application.port.`in`.presentation.fullPresentationOf
import org.fim.wallet.domain.MutableCreationExtras
import org.fim.wallet.domain.credential.Credential
import org.fim.wallet.domain.presentation.AggregatorPresentation
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource


class CreatePresentationServiceTest {
  @Test
  fun createAggregatorPresentation() = runTest(timeout = 600.seconds) {
    val zkpService = SnarkJSService.getInstance()
    val service = CreatePresentationService(zkpService)

    val n = 1

    var credentials = emptyList<Credential>().toMutableList()

    for (i in 1..n){
      credentials.addAll(createRandomCredentialTrio())
    }


    var creationExtras = MutableCreationExtras()
    creationExtras[AggregatorPresentation::tStart] = BigInteger(0)
    creationExtras[AggregatorPresentation::tEnd] = BigInteger(15)

    val presentation: AggregatorPresentation =
      service.fullPresentationOf(
        credentials, creationExtras
      )

    terminateSnarkJS()
  }

  @Test
  fun verifyAggregatorPresentation() = runTest(timeout = 600.seconds) {
    val zkpService = SnarkJSService.getInstance()
    val service = CreatePresentationService(zkpService)
    var credentials = createRandomCredentialTrio()


    var creationExtras = MutableCreationExtras()
    creationExtras[AggregatorPresentation::tStart] = BigInteger(0)
    creationExtras[AggregatorPresentation::tEnd] = BigInteger(15)
    val timeSource = TimeSource.Monotonic
    val markStart = timeSource.markNow()

    val presentation: AggregatorPresentation =
      service.fullPresentationOf(
        credentials,
        creationExtras
      )
    val ellapsed  = timeSource.markNow() -markStart

    println("time: $ellapsed")
    assertTrue(
      zkpService.verify(
        "aggregatedFlexibilityPresentation",
        presentation.publicSignals,
        presentation.proof
      )
    )
    terminateSnarkJS()
  }
}

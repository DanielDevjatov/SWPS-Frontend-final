package org.fim.wallet.adapter.out.persistence

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.fim.wallet.application.port.out.persistence.GenericPresentationRepository
import org.fim.wallet.application.service.convertToBigInteger
import org.fim.wallet.domain.poseidon.PoseidonHash
import org.fim.wallet.domain.presentation.VerifiablePresentation
import kotlin.uuid.ExperimentalUuidApi

/**
 * In memory implementation of the [GenericPresentationRepository] that stores the [VerifiablePresentation]s inside a [Map].
 *
 * @see GenericPresentationRepository
 */
class InMemoryGenericPresentationRepository<T : VerifiablePresentation> : GenericPresentationRepository<T> {
  private val presentations = mutableMapOf<BigInteger, T>()

  @OptIn(ExperimentalUuidApi::class)
  override fun save(presentation: T) {
    presentations[hasher.digest(presentation.id.convertToBigInteger())] = presentation
  }

  override fun getById(idHash: BigInteger): T? {
    return presentations[idHash]
  }

  override fun getAll(): Set<T> {
    return presentations.values.toSet()
  }

  override fun delete(idHash: BigInteger) {
    presentations.remove(idHash)
  }

  companion object{
    val hasher = PoseidonHash()
  }
}

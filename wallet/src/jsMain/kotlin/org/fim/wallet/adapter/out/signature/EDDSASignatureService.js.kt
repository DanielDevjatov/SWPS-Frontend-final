package org.fim.wallet.adapter.out.signature

import com.ionspin.kotlin.bignum.integer.BigInteger
import js.core.BigInt
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import node.buffer.Buffer
import org.fim.wallet.application.port.out.EDDSASignatureProvider
import org.fim.wallet.domain.eddsa.PrivateKey
import org.fim.wallet.domain.eddsa.PublicKey
import org.fim.wallet.domain.eddsa.Signature
import org.fim.wallet.domain.eddsa.toBigIntegerList

actual class EDDSASignatureService private constructor(val service: Eddsa) : EDDSASignatureProvider {

    override fun publicKey(privateKey: PrivateKey): PublicKey {
        val dynamicPublicKey = service.prv2pub(Buffer.from(privateKey.bytes))
        return PublicKey(
            Pair(
                BigInteger.parseString(service.babyJub.F.toString(dynamicPublicKey[0])),
                BigInteger.parseString(service.babyJub.F.toString(dynamicPublicKey[1])),
            )
        )
    }

    override fun sign(message: BigInteger, privateKey: PrivateKey): Signature {
        val result = service.signPoseidon(
            Buffer.from(privateKey.bytes),
            service.babyJub.F.e(message.toString())
        )

        val r80: String = service.babyJub.F.toString(result["R8"][0])
        val r81: String = service.babyJub.F.toString(result["R8"][1])
        val s: String = result["S"].toString()
        return Signature(
            listOf(
                BigInteger.parseString(r80),
                BigInteger.parseString(r81),
                BigInteger.parseString(s)
            )
        )
    }

    override fun verify(message: BigInteger, signature: Signature, publicKey: PublicKey): Boolean {
        val jsSig = js("{}")
        jsSig.R8 =
            arrayOf(
                service.babyJub.F.e(signature.r.first.toString()),
                service.babyJub.F.e(signature.r.second.toString())
            )

        jsSig.S = BigInt(signature.s.toString())
        return service.verifyPoseidon(
            service.babyJub.F.e(message.toString()),
            jsSig,
            publicKey.toBigIntegerList().map { service.babyJub.F.e(it.toString()) }.toTypedArray()
        )
    }

    actual companion object {
        /**
         * Override the invoke-operator in combination with a private constructor to mock a suspend constructor.
         *
         * Since the [buildEddsa] method from circomlibjs is async all service functions would be suspend-functions too.
         * To limit this scope-creep, this could be used to init the service instance thus making only the initialization
         * a suspend call.
         *
         * Example of Usage:
         * ```
         *    // Only this call would be a coroutine
         *    val service = EDDSASignatureService() // this won't call the constructor but this invoke operator
         *
         *    // Sync function calls using the service
         *    service.sign(/* ... */)
         *    service.verify(/* ... */)
         * ```
         *
         * @return [EDDSASignatureService] instance for JS platform
         */
        actual suspend operator fun invoke(): EDDSASignatureService {
            @OptIn(DelicateCoroutinesApi::class)
            return EDDSASignatureService(GlobalScope.async { buildEddsa().await() }.await())
        }
    }
}

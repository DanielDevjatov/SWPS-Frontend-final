@file:JsModule("circomlibjs")
@file:JsNonModule

package org.fim.wallet.adapter.out.signature

import com.ionspin.kotlin.bignum.integer.BigInteger
import js.typedarrays.Uint8Array
import node.buffer.Buffer
import kotlin.js.Promise

/**
 * External function to create a new instance of [Eddsa] from circomlibjs.
 *
 * @return [Promise] of the new object instance
 */
@JsName("buildEddsa")
external fun buildEddsa(): Promise<Eddsa>

external interface Eddsa {
  val babyJub: BabyJub

  /**
   * External function of circomlibjs to get the public key to a given private key.
   *
   * Example:
   * ```
   * val dynamicPublicKey = service.prv2pub(Buffer.from(privateKey.bytes))
   *     return PublicKey(
   *         Pair(
   *             BigInteger.parseString(service.babyJub.F.toString(dynamicPublicKey[0])),
   *             BigInteger.parseString(service.babyJub.F.toString(dynamicPublicKey[1]))
   *         )
   *     )
   *```
   * @param prv the private key represented as [Buffer] of a [ByteArray]
   *
   * @return the public key represented as an array of two [Uint8Array]s
   */
  fun prv2pub(prv: dynamic): dynamic

  /**
   * External function of circomlibjs to sing a given message with a given private key using Poseidon hashes.
   *
   * **Note**: It's only possible to sign messages represented as **one** [BigInteger].
   *
   * @param prv the private key represented as [Uint8Array]
   * @param msg the message represented as one [BigInteger]
   *
   * @return the unpacked signature object
   *
   * @see packSignature
   */
  fun signPoseidon(prv: Buffer, msg: dynamic): dynamic

  /**
   * External function of circomlibjs to verify a signature of a given message and given public key.
   *
   * **Note**: It's only possible to verify messages represented as **one** [BigInteger].
   *
   * @param msg the message represented as one [BigInteger]
   * @param sig the **unpacked** signature object
   * @param pub the public key represented as an array of two [Uint8Array]s
   *
   * @return `true` if the signature is valid, `false` otherwise
   *
   * @see unpackSignature
   */
  fun verifyPoseidon(msg: dynamic, sig: dynamic, pub: dynamic): Boolean

  /**
   * External function of circomlibjs to pack a dynamic js signature object into a [Uint8Array].
   *
   * @param sig the **unpacked** signature object

   *
   * @return the signature represented as an array of two [Uint8Array]s
   */
  fun packSignature(sig: dynamic): Uint8Array

  /**
   * External function of circomlibjs to unpack a signature [Uint8Array] into a dynamic js signature object.
   *
   * @param sig the **packed** signature as [Uint8Array]
   * @return the dynamic js signature object
   */
  fun unpackSignature(sig: Uint8Array): dynamic
}

external interface BabyJub {
  val F: Field
  fun packPoint(P:dynamic): Uint8Array
}

external interface Field {
  fun toString(a: dynamic): String
  fun toObject(a: dynamic): dynamic
  fun eq(a: dynamic, b: dynamic): Boolean
  fun e(number: String): dynamic       //nominated BigInteger
  val type: String
}

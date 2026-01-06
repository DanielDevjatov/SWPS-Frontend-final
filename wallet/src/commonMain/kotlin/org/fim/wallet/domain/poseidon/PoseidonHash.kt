@file:UseSerializers(BigIntegerSerializer::class, UuidSerializer::class)
package org.fim.wallet.domain.poseidon

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.fim.wallet.domain.extensions.toArbitraryBigInteger
import org.fim.wallet.domain.serialize.BigIntegerSerializer
import org.fim.wallet.domain.serialize.UuidSerializer
import kotlin.math.ceil
import kotlin.math.log
import kotlin.math.pow

/**
 * [Poseidon hash implementation of circomlib](https://github.com/iden3/circomlib/blob/master/circuits/poseidon.circom) translation to Kotlin.
 *
 * The hash generator is initialized with a block size and is capable of hashing 1 to 16 inputs at once.
 * The hash output is a [BigInteger].
 *
 * @param   blockSize     the number of inputs hashed together, 1 by default.
 *
 * @property t            the T constant, circomlib implement hashes for t = 2 .. 17 allowing 1 up to 16 inputs.
 * @property p            the prime number used for zk-SNARK Proofs in F_p arithmetic circuits; see [circom documentation](https://docs.circom.io/background/background/#arithmetic-circuits)
 * @property nRoundsP     number of partial hashing iterations initialized with recommended parameters from [whitepaper](https://eprint.iacr.org/2019/458.pdf) (table 2, table 8)
 * @property nRoundsF     number of full hashing iterations; defined as 8 in the circom circuit
 * @property constantsC   round constants used by circom
 * @property constantsM   mix layer matrix used by circom
 * @property constantsP   partial round constants used by circom
 * @property constantsS   partial mix layer matrix used by circom
 */
@Serializable
class PoseidonHash (val blockSize: Int = 1) {
  private val t = blockSize + 1

  private val p: BigInteger = SNARK_SCALAR_FIELD

  private val nRoundsP: Int = nRoundsPConstants[t-2]
  private val nRoundsF: Int = 8

  private val constantsC: Array<BigInteger> = poseidonC(t)
  private val constantsM: Array<Array<BigInteger>> = poseidonM(t)
  private val constantsP: Array<Array<BigInteger>> = poseidonP(t)
  private val constantsS: Array<BigInteger> = poseidonS(t)

  init {
    require(blockSize in 1..16) {
      "Circom doesn't support more than 16 inputs. Please make sure bockSize is between 1 and 16. (Was $blockSize)."
    }
  }

  /**
   * Returns the digest for given strings.
   *
   * Iff the block size is one, only one input may be hashed since no compression is possible.
   *
   * For larger block sizes and a number of inputs not equal to the block size provided, the input will be padded
   * before it is compressed into one hash value.
   *
   * The input will be patted to a length if blockSize ** n, for minimal n.
   * Missing inputs will be filled with zeros. The last padded number will be the size of the original input.
   * The resulting hash is equivalent to the Merkle Root from a Merkle Tree, which has the padded inputs as leaves.
   *
   * @param inputs the [BigInteger]s to be hashed
   *
   * @return the compressed [BigInteger] hash value of the (padded) inputs.
   *
   * @throws IllegalArgumentException iff the block size is 1 but more inputs were provided
   * @throws IllegalArgumentException if no input was provided
   */
  fun digest(vararg inputs: BigInteger): BigInteger {
    require(inputs.isNotEmpty()) { "Input must not be empty." }

    if (blockSize == 1) return hash(*inputs)

    var words = inputs

    do {
      words = compress(*words)
    } while (words.size > 1)

    return words.first()
  }

  /**
   * Returns the digest for given strings.
   * The Strings will not be interpreted as a number.
   * For Strings that represent a number with a given base, convert them with BigInteger.parseString() and use the
   * digest method for BigIntegers instead.
   *
   * **Note:**
   *
   * Only strings with up to 17,179,869,176 chars are supported. (= Int.MAX_VALUE * 8)
   * Strings that result in a larger BigInteger than the prime p used by circom will be reduced to the reminder of mod p.
   *
   * @throws IllegalArgumentException if no input was provided.
   */
  fun digest(vararg inputs: String): BigInteger {
    require(inputs.isNotEmpty()) { "Input must not be empty." }
    return digest(*inputs.map { it.toArbitraryBigInteger() }.toTypedArray())
  }

  /**
   * After padding the inputs to match the block size, this function returns the hash of each block.
   *
   * The input will be patted to a length if blockSize ** n, for minimal n.
   * Missing inputs will be filled with zeros. The last padded number will be the size of the original input.
   *
   * The size of the output will be a multiple of the block size.
   *
   * @param inputs  the inputs to be hashed
   *
   * @return Array of [BigInteger] hash values for all blocks of the padded input
   */
  private fun compress(vararg inputs: BigInteger): Array<BigInteger> {

    val numberOfLeaves = blockSize.toDouble().pow(ceil(log(inputs.size.toDouble(), blockSize.toDouble())))

    val paddingSize = numberOfLeaves - inputs.size
    val paddingWords = Array(paddingSize.toInt()) { BigInteger.ZERO }
    paddingWords[paddingWords.lastIndex] = BigInteger(inputs.size)

    return (inputs.toList() + paddingWords).windowed(blockSize, blockSize).map { block ->
      hash(*block.toTypedArray())
    }.toTypedArray()
  }

  /**
   * Hash implementation of the circom circuit.
   * Since circom can only handle numbers, all inputs are passed as [BigInteger].
   * The hash is returned as [BigInteger] too.
   *
   * @param inputs   [Array] of inputs as [BigInteger]
   *
   * @return the hash of inputs as [BigInteger]
   *
   * @throws IllegalArgumentException if more than t-1 inputs are passed
   */
  private fun hash(vararg inputs: BigInteger): BigInteger {
    val arcOut = Array(nRoundsF) { Array(t) { BigInteger.ZERO } }
    val sigmaFOut = Array(nRoundsF) { Array(t) { BigInteger.ZERO } }
    val mixOut = Array(nRoundsF - 1) { Array(t) { BigInteger.ZERO } }
    val sigmaPOut = Array( nRoundsP ) { BigInteger.ZERO }
    val mixSOut = Array(nRoundsP) { Array(t) { BigInteger.ZERO } }

    require(inputs.size == t - 1) {"To many inputs (${inputs.size}) for t = $t."}

    val initialState = Array(t) { i -> inputs.getOrElse(i - 1) { BigInteger.ZERO } }

    arcOut[0] = poseidonArc(constantsC, initialState, 0, p)

    for(round in 0  until nRoundsF/2 - 1) {
      sigmaFOut[round] = poseidonSBox(mixOut.getOrElse(round - 1) { arcOut[0] }, p)

      arcOut[round + 1] = poseidonArc(constantsC, sigmaFOut[round], (round + 1) * t, p)
      mixOut[round] = poseidonMix(arcOut[round+1], constantsM, p)
    }

    sigmaFOut[nRoundsF/2 - 1] = poseidonSBox(mixOut[nRoundsF/2 - 2], p)
    arcOut[nRoundsF/2] = poseidonArc(constantsC, sigmaFOut[nRoundsF/2  - 1], (nRoundsF / 2) * t, p)
    mixOut[nRoundsF/2 - 1] = poseidonMix(arcOut[nRoundsF/2], constantsP, p)

    for(round in 0 until nRoundsP) {
      if (round == 0) sigmaPOut[round] = (mixOut[nRoundsF/2-1][0].pow(5)) % p
      else sigmaPOut[round] = (mixSOut[round-1][0].pow(5)) % p

      val mixSInput = if (round == 0) mixOut[nRoundsF/2-1] else mixSOut[round-1]
      mixSInput[0] = sigmaPOut[round] + constantsC[(nRoundsF/2 + 1) * t + round]

      if (round == 0) mixSOut[round] = poseidonMixS(mixSInput, constantsS, round, p)
      else mixSOut[round] = poseidonMixS(mixSInput, constantsS, round, p)
    }

    for (round in 0 until nRoundsF/2 - 1) {
      if (round == 0) sigmaFOut[nRoundsF/2 + round] = poseidonSBox(mixSOut[nRoundsP - 1], p)
      else sigmaFOut[nRoundsF/2 + round] = poseidonSBox(mixOut[nRoundsF/2 + round - 1], p)

      arcOut[nRoundsF/2 + round + 1] = poseidonArc(constantsC, sigmaFOut[nRoundsF/2 + round], (nRoundsF/2 + 1) * t + nRoundsP + round * t, p)
      mixOut[nRoundsF/2 + round] = poseidonMix(arcOut[nRoundsF/2 + round + 1], constantsM, p)
    }

    sigmaFOut[nRoundsF - 1] = poseidonSBox(mixOut[nRoundsF - 2], p)

    //val buffer = Array(outputs) { BigInteger.ZERO }
    //for (i in 0 until nRoundsP) {
    //  buffer[i] = poseidonMixLast(sigmaFOut[nRoundsF - 1], constantsM, i, p)
    //}
    return poseidonMixLast(sigmaFOut[nRoundsF - 1], constantsM, 0, p)
  }

  companion object Default{
    private val SNARK_SCALAR_FIELD = BigInteger.parseString("21888242871839275222246405745257275088548364400416034343698204186575808495617", 10)

    private val nRoundsPConstants = arrayOf(56, 57, 56, 60, 60, 63, 64, 63, 60, 66, 60, 65, 70, 60, 64, 68)

    /**
     * Sigma function over all sub words with fixed exponent = 5, as it is defined in the circom circuit.
     * All operations are calculated over mod p.
     *
     * @param inputs  current states as [Array] of [BigInteger]
     * @param p       the prime number of the finite field
     *
     * @return [BigInteger] [Array] of new states
     */
    private fun poseidonSBox(inputs: Array<BigInteger>, p: BigInteger): Array<BigInteger> =
      inputs.map { it.pow(5) % p }.toTypedArray()

    /**
     * "Add round constants" function.
     * All operations are calculated over mod p.
     *
     * @param constantsC  the round constants used in the hash
     * @param inputs      current states as [Array] of [BigInteger]
     * @param p           the prime number of the finite field
     *
     * @return [BigInteger] [Array] of new states
     */
    private fun poseidonArc(constantsC: Array<BigInteger>, inputs: Array<BigInteger>, round: Int, p: BigInteger): Array<BigInteger> =
      inputs.mapIndexed { i, input -> (input + constantsC[i + round]) % p }.toTypedArray() // t := inputs.size

    /**
     * Mix layer function defined in the circuit.
     * All operations are calculated over mod p.
     *
     * @param constantsM  the mix layer matrix used in the hash
     * @param inputs      current states as [Array] of [BigInteger]
     * @param p           the prime number of the finite field
     *
     * @return [BigInteger] [Array] of new states
     */
    private fun poseidonMix(inputs: Array<BigInteger>, constantsM: Array<Array<BigInteger>>, p: BigInteger): Array<BigInteger> =
      inputs.indices.map { i ->
        inputs.foldIndexed(BigInteger.ZERO) { j, acc, current ->
          acc + constantsM[j][i] * current
        } % p
      }.toTypedArray()

    /**
     * Mix layer function for partial rounds defined in the circuit.
     * All operations are calculated over mod p.
     *
     * @param constantsS  the partial mix layer constants used in the hash
     * @param inputs      current states as [Array] of [BigInteger]
     * @param r           current hash iteration
     * @param p           the prime number of the finite field
     *
     * @return [BigInteger] [Array] of new states
     */
    private fun poseidonMixS(inputs: Array<BigInteger>, constantsS: Array<BigInteger>, r: Int, p: BigInteger): Array<BigInteger> =
      inputs.indices.map { i ->
        if (i == 0) {
          inputs.foldIndexed(BigInteger.ZERO) { j, acc, current ->
            acc + (constantsS[(inputs.size * 2 - 1) * r + j] * current)
          } % p
        } else {
          (inputs[i] + inputs[0] * constantsS[(inputs.size * 2 - 1) * r + inputs.size + i - 1]) % p
        }
      }.toTypedArray()

    /**
     * The last mix layer as it is defined in the circuit.
     * All operations are calculated over mod p.
     *
     * @param constantsM  the mix layer matrix used in the hash
     * @param inputs      current states as [Array] of [BigInteger]
     * @param s           the used column of the mix layer matrix
     * @param p           the prime number of the finite field
     *
     * @return [BigInteger] [Array] of new states
     */
    private fun poseidonMixLast(
      inputs: Array<BigInteger>,
      constantsM: Array<Array<BigInteger>>,
      s: Int,
      p: BigInteger
    ): BigInteger =
      inputs.foldIndexed(BigInteger.ZERO) { j, acc, current ->
        acc + (constantsM[j][s] * current)
      } % p

    /**
     * Default function to hash one [String]. The [String] will not be interpreted as a number.
     * For [String]s that represent a number with a given base convert them with [BigInteger.parseString] and use the
     * digest method for [BigInteger]s instead.
     *
     * **Note:**
     *
     * Only strings with up to 17,179,869,176 chars are supported. (= Int.MAX_VALUE * 8)
     * [String]s that result in a larger [BigInteger] than the prime p used by circom will be reduced to the reminder of mod p.
     *
     * @param str the [String] to be hashed
     *
     * @return the [BigInteger] hash value of string
     */
    fun digest(str: String): BigInteger = PoseidonHash().digest(str)

    /**
     * Default hash function to hash up to 16 [BigInteger]s.
     * The block size is chosen according to the given number of inputs.
     * For more than 16 inputs a custom hash generator must be used.
     *
     * @param inputs  the [BigInteger]s to be hashed
     *
     * @throws IllegalArgumentException if no, or more than 16 inputs were provided
     */
    fun digest(vararg inputs: BigInteger): BigInteger {
      require(inputs.isNotEmpty()) { "Input must not be empty." }
      require(inputs.size <= 16) { "Please use a custom generator with specified block size for more than 16 inputs." }
      return PoseidonHash(inputs.size).digest(*inputs)
    }
  }
}

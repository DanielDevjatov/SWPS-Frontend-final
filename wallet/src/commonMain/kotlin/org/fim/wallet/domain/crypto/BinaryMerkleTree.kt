package org.fim.wallet.domain.crypto

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.fim.wallet.domain.poseidon.PoseidonHash
import org.fim.wallet.domain.poseidon.PoseidonHashable
import kotlin.math.ceil
import kotlin.math.log
import kotlin.math.pow

/**
 * Implementation of a binary Merkle Tree using Poseidon hash function.
 * The tree is constructed from an array of [PoseidonHashable] leaves and maintains
 * a complete binary structure by padding with zeros if necessary.
 *
 * @property leaves   Array of [PoseidonHashable] values that form the tree leaves
 * @property data     List of [BigInteger] containing the values of all tree nodes (leaves and internal nodes)
 * @property depth    Depth of the tree, calculated from the given number of leaves
 */
data class BinaryMerkleTree(var leaves: Array<PoseidonHashable<*>>) {
    val data: List<BigInteger>
    val depth: Int
    //val salt: Array<BigInteger>

  /*
   * Initializes the Merkle Tree by calculating tree depth, padding leaves if necessary to a power of 2,
   * and computing all internal node hashes using Poseidon hash function.
   */
  init {
        val lengthLog = log(leaves.size.toDouble(),2.0)
        depth = ceil(lengthLog).toInt()

        leaves = padLeaves(leaves, depth)

        data = leaves.map { it.hash() }.toMutableList()

        val size = (2.0).pow(depth+1).toInt()-1

        val hash2 = PoseidonHash(2)
        for (i in 0..<size-1 step 2){
            data.add(hash2.digest(data[i], data[i+1]))
        }
    }

    /**
     * Returns the root hash of the Merkle Tree.
     * The root is calculated during tree construction as the hash of the two child nodes
     * at the highest level of the tree.
     *
     * @return [BigInteger] root hash value
     */
    fun root(): BigInteger = data.last()

    /**
     * Creates a [BinaryMerkleProof] for the element at 'index'.
     *
     * @return [BinaryMerkleProof] for the element at 'index'.
     */
    fun createMerkleProof(index: Int): BinaryMerkleProof{

        if (leaves.size <= index) {
            throw IndexOutOfBoundsException("Leave index out of bounds!")
        }

        val lemma = mutableListOf<BigInteger>()
        val path = List(depth) { i -> (index shr i) and 1 == 1 }

        var offset = 0
        var pos = index
        var width = leaves.size

        for(i in 0..<depth){
            lemma.add(if (path[i]) data[offset+pos-1] else data[offset+pos+1])
            pos = pos shr 1
            offset += width
            width = width shr 1
        }

        lemma.add(root())

        return BinaryMerkleProof(leaves[index], lemma, path)
    }

    /**
     * Verifies a [BinaryMerkleProof] and checks that it is connected to this [BinaryMerkleTree].
     *
     * @return [Boolean] corresponding to the validity of the [BinaryMerkleProof]
     */
    fun verifyMerkleProof(proof: BinaryMerkleProof): Boolean{
        if (proof.subject != leaves[proof.index()] || proof.root() != root()){
            return false
        }

        return proof.verify()
    }

    override fun equals(other: Any?): Boolean {
        return other is BinaryMerkleTree && other.root() == root()
    }

    override fun hashCode(): Int {
        return this.root().hashCode()
    }

    private companion object {
        /**
         * Adds padding leaves (value is 0) to the given array of [PoseidonHashable] on tree construction if necessary.
         *
         * @param   leaves      [Array] of [BigInteger] to be extended
         * @param   depth       depth of the target [BinaryMerkleTree]
         */
        fun padLeaves(leaves: Array<PoseidonHashable<*>>, depth: Int) =
            leaves + Array((2.0).pow(depth).toInt() - leaves.size) {
                PoseidonHashable(BigInteger.ZERO) { v: BigInteger ->
                    PoseidonHashable.poseidonHasher.digest(v)
                }
            }
    }
}

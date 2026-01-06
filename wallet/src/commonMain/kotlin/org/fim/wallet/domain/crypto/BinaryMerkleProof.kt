package org.fim.wallet.domain.crypto

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.fim.wallet.domain.poseidon.PoseidonHash
import org.fim.wallet.domain.poseidon.PoseidonHashable
import kotlin.math.pow

data class BinaryMerkleProof(
    val subject: PoseidonHashable<*>,
    val lemma: List<BigInteger>,
    val path: List<Boolean>
) {
    /**
     * Returns the root hash of the corresponding Merkle Tree.
     *
     * @return [BigInteger] root hash value
     */
    fun root(): BigInteger = lemma.last()

    /**
     * Verifies that the proof is valid, that is the subject and lemma along the path lead to the root.
     */
    fun verify(): Boolean{
        var currentHash = subject.hash()

        path.forEachIndexed { index, pathCurrent ->
            currentHash = if(pathCurrent) {
                hasher.digest(lemma[index],currentHash)
            } else {
                hasher.digest(currentHash,lemma[index])
            }
        }

        return currentHash == root()
    }

    /**
     * Calculates the index of the subject from the path.
     *
     * @return index of the subject in the [BinaryMerkleTree]
     */
    fun index() : Int {
        return path.foldIndexed(0) { index, acc, pathCurrent ->
            if(pathCurrent) acc + 2.0.pow(index).toInt()
            else acc
        }
    }

    private companion object {
        val hasher = PoseidonHash(2)
    }
}

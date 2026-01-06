package org.fim.wallet.domain.crypto

import com.ionspin.kotlin.bignum.integer.BigInteger
import org.fim.wallet.domain.poseidon.PoseidonHashable
import org.fim.wallet.domain.poseidon.toPoseidonHashable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class BinaryMerkleTreeTest {

    private val leaves = Array(8){n -> BigInteger(n).toPoseidonHashable() as PoseidonHashable<*> }

    @Test
    fun createBinaryMerkleTree(){
        val tree = BinaryMerkleTree(leaves)

        assertEquals(tree.data,
            listOf(
                BigInteger.parseString("19014214495641488759237505126948346942972912379615652741039992445865937985820"),
                BigInteger.parseString("18586133768512220936620570745912940619677854269274689475585506675881198879027"),
                BigInteger.parseString("8645981980787649023086883978738420856660271013038108762834452721572614684349"),
                BigInteger.parseString("6018413527099068561047958932369318610297162528491556075919075208700178480084"),
                BigInteger.parseString("9900412353875306532763997210486973311966982345069434572804920993370933366268"),
                BigInteger.parseString("19065150524771031435284970883882288895168425523179566388456001105768498065277"),
                BigInteger.parseString("4204312525841135841975512941763794313765175850880841168060295322266705003157"),
                BigInteger.parseString("7061949393491957813657776856458368574501817871421526214197139795307327923534"),

                BigInteger.parseString("375297826003036628543272924666094926569481681557326698706250789536843815120"),
                BigInteger.parseString("9074156429624010912339881513060534659777325054059394844024237786906260220601"),
                BigInteger.parseString("9658095687448412201870270621676483010844577614912451396411610346398764394959"),
                BigInteger.parseString("8310622300666652057380641263095364654753830566096320120115556928900605887337"),

                BigInteger.parseString("17474248862608787121273565973042536939567170640149812272454991795751426969927"),
                BigInteger.parseString("5471014806144453753598890707104428678638390763888239083625266915883087418957"),

                BigInteger.parseString("16014178617822694630481959657040975713602678743322519454424121501495751569030")

            ),"Incorrect Merkle Tree created!")
    }

    @Test
    fun createBinaryMerkleProof(){
        val tree = BinaryMerkleTree(leaves)
        val proof = tree.createMerkleProof(1)

        assertEquals(leaves[1],proof.subject,"Wrong MerkleProof subject!")
        assertEquals(1, proof.index(),"Wrong MerkleProof index!")

        assertTrue(proof.verify(),"Invalid MerkleProof!")
        assertTrue(tree.verifyMerkleProof(proof),"MerkleProof invalid for tree!")
    }
}

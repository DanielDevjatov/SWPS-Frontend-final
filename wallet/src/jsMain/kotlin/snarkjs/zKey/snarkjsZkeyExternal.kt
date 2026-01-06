@file:JsModule("snarkjs")
@file:JsQualifier("zKey")
@file:JsNonModule

package snarkjs.zKey

import kotlin.js.Promise

/**
 * External function of SnarkJS to verify a zKey-file for a given circuit (r1cs-file) and ceremony (ptau-file).
 *
 * This function is async will return a [Promise].
 *
 * @param r1csFileName  the full path to the r1cs-file
 * @param pTauFileName  the full path to the ptau-file
 * @param zKeyFileName  the full path to the zKey-file
 * @param logger        optional logger instance of [logplease.create]
 *
 * @return true if the zKey-file is valid, false otherwise
 */
@JsName("verifyFromR1cs")
external fun verify(r1csFileName: String, pTauFileName: String, zKeyFileName: String, logger: dynamic): Promise<Boolean>

/**
 * External function of SnarkJS to export the verification key from a zKey-file to a JSON string.
 *
 * This function is async will return a [Promise].
 *
 * @param zKeyName  the full path to the zKey-file
 * @param logger    optional logger instance of [logplease.create]
 *
 * @return JSON object of the verification key
 */
external fun exportVerificationKey(zKeyName: String, logger: dynamic): Promise<Any>

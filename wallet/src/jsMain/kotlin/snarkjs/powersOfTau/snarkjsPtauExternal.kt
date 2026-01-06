@file:JsModule("snarkjs")
@file:JsQualifier("powersOfTau")
@file:JsNonModule

package snarkjs.powersOfTau

import kotlin.js.Promise

/**
 * External function of SnarkJS to verify a ceremony respectively the according ptau-file.
 *
 * This function is async will return a [Promise].
 *
 * @param tauFilename   the full path to the ptau-file
 * @param logger        optional logger instance of [logplease.create]
 *
 * @return true if the ceremony / ptau-file is valid, false otherwise
 */
external fun verify(tauFilename: String, logger: dynamic): Promise<Boolean>

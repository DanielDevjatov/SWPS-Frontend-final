@file:JsModule("snarkjs")
@file:JsQualifier("r1cs")
@file:JsNonModule

package snarkjs.r1cs

import kotlin.js.Promise

/**
 * External function of SnarkJS to export a r1cs-file to a JSON string.
 *
 * This function is async will return a [Promise].
 *
 * @param r1csFileName  the full path to the r1cs-file
 * @param logger        optional logger instance of [logplease.create]
 *
 * @return JSON object of the r1cs-file
 */
external fun exportJson(r1csFileName: String, logger: dynamic): Promise<Any>

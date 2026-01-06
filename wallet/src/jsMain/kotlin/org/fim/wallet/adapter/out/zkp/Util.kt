package org.fim.wallet.adapter.out.zkp

/**
 * SnarkJS internal auxiliary function to solve the issue of [SnarkJS Issue #152](https://github.com/iden3/snarkjs/issues/152).
 *
 * Since SnarkJS doesn't terminate its own workers after program execution, this function will terminate the bn128 curve
 * worker by hand.
 *
 * If not used at least once after library calls of SnarkJS the program or unit tests will get stuck in an endless loop
 * after execution. One call at the end of execution is fine. Multiple calls, even in between proof generations, are also
 * possible but not recommended due to possible performance cuts.
 */
suspend fun terminateSnarkJS() {
  js.globals.globalThis.curve_bn128.terminate()
}

suspend fun asyncTerminateSnarkJS() {
  js.globals.globalThis.curve_bn128.terminate().await()
}

package org.fim.wallet.adapter.out.zkp

import node.fs.existsSync

/**
 * Throws a [FileNotFoundException] with the result of calling [lazyMessage] if no file could be found with given [path].
 *
 * This is a JS specific implementation due to the usage of [node.fs.existsSync].
 */
inline fun requireFile(path: String, lazyMessage: () -> Any) {
  val exists = try { existsSync(path) } catch (e: Throwable) {
    val fs = js("require('fs')")
    fs.existsSync(path) as Boolean
  }
  if (!exists) {
    val message = lazyMessage()
    throw FileNotFoundException(message.toString())
  }
}

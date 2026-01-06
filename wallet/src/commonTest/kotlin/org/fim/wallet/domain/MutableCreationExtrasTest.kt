package org.fim.wallet.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class MutableCreationExtrasTest {

  private val first = 1
  private val second = "test"
  private val value = "test"

  @Test
  fun initCreationExtras() {
    val extras = MutableCreationExtras(
      ::first to (1).toString(),
      ::second to "test"
    )

    assertEquals((1).toString(), extras[::first])
    assertEquals("test", extras[::second])
  }

  @Test
  fun setCreationExtras() {
    val extras = MutableCreationExtras()
    extras[::value] = "test"

    assertEquals("test", extras[::value])
  }
}

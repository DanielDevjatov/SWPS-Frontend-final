package org.fim.wallet.adapter.out.zkp

import kotlin.test.Test
import kotlin.test.assertFailsWith

class PreconditionsTest {

  @Test
  fun existingFile() {
    requireFile("kotlin/test_file.txt") { "Expected test file." }
  }

  @Test
  fun nonExistingFile() {
    assertFailsWith<FileNotFoundException> {
      requireFile("kotlin/not_a_file.txt") { "Expected test file." }
    }
  }
}

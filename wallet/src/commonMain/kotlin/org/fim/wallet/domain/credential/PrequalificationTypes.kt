package org.fim.wallet.domain.credential

/**
 * Enum of possible pre-qualifications, inorder to avoid an implicit definition and parsing strings.
 * Currently, there is only FLEX, but it's likely to be expanded in the future.
 */
enum class PrequalificationTypes {
  NONE, FLEX
}

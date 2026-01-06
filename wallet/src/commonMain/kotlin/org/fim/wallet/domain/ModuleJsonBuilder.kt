/**
 * This module handles the serialization configuration for credentials in the wallet domain.
 * It preregisters known certificate types and provides JSON serialization capabilities.
 */
package org.fim.wallet.domain

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.fim.wallet.domain.credential.ConsentCredential
import org.fim.wallet.domain.credential.Credential
import org.fim.wallet.domain.credential.DeviceSpecificationsCredential
import org.fim.wallet.domain.credential.PrequalificationCredential
import org.fim.wallet.domain.presentation.AggregatorPresentation
import org.fim.wallet.domain.presentation.VerifiablePresentation
import org.fim.wallet.domain.serialize.BigIntegerSerializer


/**
 * Internal serialization module that registers known credential types and their serializers.
 * This module is used to properly serialize/deserialize polymorphic credential classes.
 */
internal val internalModule = SerializersModule {
  polymorphic(Credential::class) {
    subclass(ConsentCredential::class)
    subclass(DeviceSpecificationsCredential::class)
    subclass(PrequalificationCredential::class)
  }
  polymorphic(VerifiablePresentation::class) {
    subclass(AggregatorPresentation::class)
  }
  contextual(BigInteger::class, BigIntegerSerializer)
}

/**
 * Internal JSON configuration instance with custom serialization settings.
 * Uses lenient parsing and polymorphic class discrimination for credential types.
 */
@OptIn(ExperimentalSerializationApi::class)
internal var _moduleJsonBuilder = Json {
  isLenient = true
  serializersModule = internalModule
  classDiscriminatorMode = ClassDiscriminatorMode.POLYMORPHIC
}

/**
 * Public accessor for the JSON builder configuration.
 * Provides read-only access to the internal JSON configuration.
 */
val moduleJsonBuilder
  get() = _moduleJsonBuilder

package org.fim.wallet.adapter.`in`.module

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import org.fim.wallet.adapter.out.persistence.InMemoryGenericCredentialRepository
import org.fim.wallet.adapter.out.persistence.InMemoryGenericPresentationRepository
import org.fim.wallet.adapter.out.signature.EDDSASignatureService
import org.fim.wallet.adapter.out.zkp.SnarkJSService
import org.fim.wallet.application.port.`in`.credential.*
import org.fim.wallet.application.port.`in`.presentation.CreatePresentationUseCase
import org.fim.wallet.application.port.`in`.presentation.PersistencePresentationUseCase
import org.fim.wallet.application.port.`in`.presentation.VerifyPresentationUseCase
import org.fim.wallet.application.port.out.EDDSASignatureProvider
import org.fim.wallet.application.port.out.ZKPProvider
import org.fim.wallet.application.port.out.persistence.GenericCredentialRepository
import org.fim.wallet.application.port.out.persistence.GenericPresentationRepository
import org.fim.wallet.application.service.credential.*
import org.fim.wallet.application.service.presentation.CreatePresentationService
import org.fim.wallet.application.service.presentation.PersistencePresentationService
import org.fim.wallet.application.service.presentation.VerifyPresentationService
import org.fim.wallet.domain.*
import org.fim.wallet.domain.credential.*
import org.fim.wallet.domain.crypto.HashableCreationExtras
import org.fim.wallet.domain.dependencyInjection.DependencyProvider
import org.fim.wallet.domain.dependencyInjection.Factory
import org.fim.wallet.domain.dependencyInjection.Single
import org.fim.wallet.domain.eddsa.PrivateKey
import org.fim.wallet.domain.eddsa.PublicKey
import org.fim.wallet.domain.poseidon.PoseidonHashable
import org.fim.wallet.domain.presentation.AggregatorPresentation
import org.fim.wallet.domain.presentation.VerifiablePresentation
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.uuid.Uuid

/**
 * This class provides an interface that could be used outside this module to hold an instance of a wallet.
 *
 * Injected dependencies will be managed by an internal [DependencyProvider].
 *
 * Example of usage:
 * ```
 * val wallet = Wallet() {
 *   single<GenericCredentialRepository<ConsentCredential>> {
 *     InMemoryGenericCredentialRepository<ConsentCredential>()
 *   }
 *   single<GenericCredentialRepository<PrequalificationCredential>> {
 *     InMemoryGenericCredentialRepository<PrequalificationCredential>()
 *   }
 *   single<GenericCredentialRepository<DeviceSpecificationsCredential>> {
 *     InMemoryGenericCredentialRepository<DeviceSpecificationsCredential>()
 *   }
 *
 *   /* ... */
 * }
 * ```
 *
 * @see [Wallet.Companion.invoke] for more information about instantiation.
 */
class Wallet private constructor(
  private val privateKey: PrivateKey,
  val publicKey: PublicKey,
) {
  private val dependencies = DependencyProvider()

  //region dependency injection methods
  /**
   * Auxiliary function to inject a custom **singleton** dependency by associating service instances with use case and repository
   * interfaces that will be used by the wallet at runtime.
   *
   * This function can be used inside the init lambda but can be also used to change service instances at runtime.
   *
   * Note that even if the lambda passed to this function is allowed to return [Any] it's up to the user
   * to ensure to only bind valid instances of the correct service to avoid runtime errors due to wrong mapped services.
   *
   * Example of usage:
   * ```
   *  val wallet = Wallet()
   *
   *  // override UseCase implementation at runtime after wallet initialization
   *  wallet.single<IssueCredentialUseCase> { IssueCredentialService() }
   * ```
   *
   * @param type    [KType] reflection of the use case or repository
   * @param service lambda in with the service instance to be associated is initialized
   */
  fun single(type: KType, service: () -> Any) {
    dependencies[type] = Single(service)
  }

  /**
   * Auxiliary function to inject a **factory** for a custom dependency by associating service instances with use case
   * and repository interfaces that will be used by the wallet at runtime.
   *
   * This function can be used inside the init lambda but can be also used to change service instances at runtime.
   *
   * Note that even if the lambda passed to this function is allowed to return [Any] it's up to the user
   * to ensure to only bind valid instances of the correct service to avoid runtime errors due to wrong mapped services.
   *
   * Example of usage:
   * ```
   *  val wallet = Wallet()
   *
   *  // override UseCase implementation at runtime after wallet initialization
   *  wallet.factory<IssueCredentialUseCase> { IssueCredentialService() }
   * ```
   *
   * @param type    [KType] reflection of the use case or repository
   * @param service lambda in with the service instance to be associated is initialized
   */
  fun factory(type: KType, service: () -> Any) {
    dependencies[type] = Factory(service)
  }

  /**
   * Auxiliary function to get the instance of injected custom dependencies at runtime.
   *
   * This function can be used to initialize dependencies that depend on other, already injected dependencies.
   *
   * Example of usage:
   * ```
   *  val wallet = Wallet() {
   *    single<GenericCredentialRepository<ConsentCredential>> {
   *      InMemoryGenericCredentialRepository<ConsentCredential>()
   *    }
   *
   *    factory<DeleteCredentialUseCase<ConsentCredential>> {
   *      DeleteCredentialService<ConsentCredential>(
   *        get(typeOf<GenericCredentialRepository<ConsentCredential>>())
   *      )
   *    }
   *  }
   * ```
   * @param type  [KType] reflection the injected service was associated beforehand
   *
   * @return services implementation associated with given [KType]
   *
   * @throws NotImplementedError if no service was associated before
   */
  fun <T> get(type: KType): T = dependencies[type]

  /**
   * Registers an externally defined credential type within the wallet's internal JSON builder to enable
   * serialization and deserialization of credentials.
   *
   * This function is required when working with custom credential types that are defined outside the wallet.
   * It adds the credential type to the polymorphic serialization module, allowing the wallet to properly
   * serialize and deserialize instances of this credential type to and from JSON.
   *
   * Example of usage:
   * ```
   * val wallet = Wallet()
   * wallet.registerCredential(CustomCredential::class)
   * ```
   *
   * @param credentialType The Kotlin class of the credential type to be registered
   * @param T The type of the credential that extends the base [Credential] class
   *
   * @see Credential
   */
  @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
  fun <T: Credential> registerCredential(credentialType: KClass<T>) {
    _moduleJsonBuilder = Json {
      isLenient = true
      serializersModule = internalModule.plus(SerializersModule {
        polymorphic(Credential::class) {
          subclass(credentialType, credentialType.serializer())
        }
      })
      classDiscriminatorMode = ClassDiscriminatorMode.POLYMORPHIC
    }
  }

  /**
   * Registers an externally defined presentation type within the wallet's internal JSON builder to enable
   * serialization and deserialization of presentations.
   *
   * This function is required when working with custom presentation types that are defined outside the wallet.
   * It adds the presentation type to the polymorphic serialization module, allowing the wallet to properly
   * serialize and deserialize instances of this presentation type to and from JSON.
   *
   * Example of usage:
   * ```
   * val wallet = Wallet()
   * wallet.registerPresentation(CustomPresentation::class)
   * ```
   *
   * @param presentationType The Kotlin class of the credential type to be registered
   * @param T The type of the presentation that extends the base [VerifiablePresentation] class
   *
   * @see VerifiablePresentation
   */
  @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
  fun <T: VerifiablePresentation> registerPresentation(presentationType: KClass<T>) {
    _moduleJsonBuilder = Json {
      isLenient = true
      serializersModule = internalModule.plus(SerializersModule {
        polymorphic(VerifiablePresentation::class) {
          subclass(presentationType, presentationType.serializer())
        }
      })
      classDiscriminatorMode = ClassDiscriminatorMode.POLYMORPHIC
    }
  }

  /**
   * Returns a read-only instance of the wallet's JSON serializer configuration.
   *
   * This method provides access to the wallet's internal JSON serializer to ensure consistent
   * serialization and deserialization of credentials outside the wallet.
   * The returned serializer is configured with all registered credential types and the wallet's serialization settings.
   *
   * Example of usage:
   * ```
   * val jsonSerializer = wallet.getModuleSerializer()
   *
   * // Serialize a credential to JSON
   * val jsonString = jsonSerializer.encodeToString(customCredential)
   *
   * // Deserialize JSON back to a credential
   * val credential = jsonSerializer.decodeFromString<CustomCredential>(jsonString)
   * ```
   *
   * @return A read-only [Json] instance configured with the wallet's serialization settings
   *
   * @see Json
   * @see Credential
   */
  fun getModuleSerializer(): Json {
    return moduleJsonBuilder
  }

  /**
   *  Auxiliary function to init the wallet instance with default services if no init lambda was provided.
   */
  suspend fun defaultDependencyInit() {
    val eddsa = EDDSASignatureService()
    single<EDDSASignatureProvider> { eddsa }

    single<ZKPProvider> { SnarkJSService.getInstance() }

    single<GenericCredentialRepository<ConsentCredential>> { InMemoryGenericCredentialRepository<ConsentCredential>() }
    single<GenericCredentialRepository<PrequalificationCredential>> { InMemoryGenericCredentialRepository<PrequalificationCredential>() }
    single<GenericCredentialRepository<DeviceSpecificationsCredential>> { InMemoryGenericCredentialRepository<DeviceSpecificationsCredential>() }

    factory<VerifyCredentialUseCase> { VerifyCredentialService(get<EDDSASignatureProvider>()) }

    factory<DeleteCredentialUseCase<ConsentCredential>> { DeleteCredentialService(get<GenericCredentialRepository<ConsentCredential>>()) }
    factory<DeleteCredentialUseCase<PrequalificationCredential>> { DeleteCredentialService(get<GenericCredentialRepository<PrequalificationCredential>>()) }
    factory<DeleteCredentialUseCase<DeviceSpecificationsCredential>> { DeleteCredentialService(get<GenericCredentialRepository<DeviceSpecificationsCredential>>()) }

    factory<StoreCredentialUseCase<ConsentCredential>> { StoreCredentialService(get<GenericCredentialRepository<ConsentCredential>>()) }
    factory<StoreCredentialUseCase<PrequalificationCredential>> { StoreCredentialService(get<GenericCredentialRepository<PrequalificationCredential>>()) }
    factory<StoreCredentialUseCase<DeviceSpecificationsCredential>> { StoreCredentialService(get<GenericCredentialRepository<DeviceSpecificationsCredential>>()) }

    factory<GetCredentialUseCase<ConsentCredential>> { GetCredentialService(get<GenericCredentialRepository<ConsentCredential>>()) }
    factory<GetCredentialUseCase<PrequalificationCredential>> { GetCredentialService(get<GenericCredentialRepository<PrequalificationCredential>>()) }
    factory<GetCredentialUseCase<DeviceSpecificationsCredential>> { GetCredentialService(get<GenericCredentialRepository<DeviceSpecificationsCredential>>()) }

    factory<IssueCredentialUseCase> { IssueCredentialService(get<EDDSASignatureProvider>()) }

    factory<CreatePresentationUseCase> { CreatePresentationService(get<ZKPProvider>()) }
    factory<VerifyPresentationUseCase> { VerifyPresentationService(get<ZKPProvider>()) }

    single<GenericPresentationRepository<AggregatorPresentation>> { InMemoryGenericPresentationRepository<AggregatorPresentation>() }
    single<PersistencePresentationUseCase<AggregatorPresentation>> { PersistencePresentationService(get<GenericPresentationRepository<AggregatorPresentation>>()) }
  }

  //endregion

  //region credential-related methods
  /**
   * Store a given [Credential] inside the wallet.
   *
   * @param credentialType  the [KType] reflection of the credential
   * @param credential      the [Credential] instance to be stored
   */
  fun <T: Credential> storeCredential(credentialType: KType, credential: T) {
    val service: StoreCredentialService<T> = dependencies[typeOf<StoreCredentialUseCase<*>>(), credentialType]
    service.storeCredential(credential)
  }

  /**
   * Parse a given [JsonElement] into the given [Credential] type and store the result inside the wallet.
   *
   * Example of usage:
   * ```
   *    val wallet = Wallet()
   *    val json = Json.encodeToJsonElement( /* ... */ )
   *
   *    wallet.storeCredential(PrequalificationCredential::class, json)
   * ```
   *
   * @param jsonElement     the [JsonElement] to be parsed
   *
   * @throws SerializationException   if the given JSON element is not a valid JSON input for the [Credential] implementation T
   * @throws IllegalArgumentException if the decoded input cannot be represented as a valid instance of [Credential] implementation T
   */
  fun storeCredential(jsonElement: JsonElement) {
    println("called store")
    val credential = CredentialFactory().fromJson(jsonElement)
    println("got credential")
    val service: StoreCredentialUseCase<*> = dependencies[StoreCredentialUseCase::class, credential::class]
    println("got service")
    service.storeCredential(jsonElement)
  }

  /**
   * Issue a new [Credential] of the specified type and given parameters.
   * The id of the credential and the issuer, the timestamp of the credential are set by the system.
   *
   * Example of usage:
   * ```
   *    val wallet = Wallet()
   *    val credential = wallet.issueCredential(PrequalificationCredential::class, /* ... */)
   * ```
   *
   * @param credentialType  [KClass] reflection of the [Credential] type to create
   * @param holder          the holders [PublicKey] used as unique id
   * @param extras          [CreationExtras] containing key-value pairs of Credential specific properties
   *
   * @return a new instance of the given [Credential] type
   *
   * @throws [MissingCreationExtraException] for invalid extras
   *
   * @see Credential
   * @see CreationExtras
   */
  fun <T : Credential> issueCredential(
    credentialType: KClass<out T>, holder: PublicKey, extras: HashableCreationExtras<PoseidonHashable<out Any>>,
                                       privateKey2: PrivateKey? = null): T {
    val service: IssueCredentialService = dependencies[typeOf<IssueCredentialUseCase>()]
    return service.issueCredential(publicKey, holder, extras, credentialType, privateKey,privateKey2)
  }

  /**
   * Query a [Credential] by id and type.
   *
   * @param credentialType  the [KType] reflection of the credential
   * @param id              the id of the [Credential]
   *
   * @return stored [Credential] with the given ID, if it exists
   *
   * @throws NoSuchCredentialException if no [Credential] can be found for the given ID
   */
  fun <T: Credential> getCredential(credentialType: KType, id: Uuid) : T {
    val service: GetCredentialService<T> = dependencies[typeOf<GetCredentialUseCase<*>>(), credentialType]
    return service.getCredential(id)
  }

  /**
   * Get all [Credential] of type [T].
   *
   * @param credentialType  the [KType] reflection of the credential
   *
   * @return [Set] of all stored [Credential]s of type [T],
   * the set will be empty if no credentials are stored in the wallet.
   */
  fun <T: Credential> getAllCredentials(credentialType: KType) : Set<T> {
    val service: GetCredentialService<T> = dependencies[typeOf<GetCredentialUseCase<*>>(), credentialType]
    return service.getAllCredentials()
  }

  /**
   * Delete the stored [Credential] with the given ID and type [T].
   *
   * @param credentialType  the [KType] reflection of the credential
   * @param id  the ID of the credential
   */
  fun <T: Credential> deleteCredential(credentialType: KType, id: Uuid) {
    val service: DeleteCredentialService<T> = dependencies[typeOf<DeleteCredentialUseCase<*>>(), credentialType]
    service.deleteCredential(id)
  }

  /**
   * Verify the signature of an existing [Credential].
   *
   * @param credential  the given [Credential]
   * @return  `true` if the signature is valid for the given issuer, `false` otherwise
   */
  fun <T : Credential> verifyCredential(credential: T): Boolean {
    val service: VerifyCredentialService = dependencies[typeOf<VerifyCredentialUseCase>()]
    return service.verifyCredential(credential)
  }
  //endregion

  //region presentation-related methods
  /**
   * Create an arbitrary [VerifiablePresentation] of a given [Credential].
   *
   * @param credentials       the [List] of [Credential]s the [VerifiablePresentation] should be created from
   * @param presentationType  reflection class of the requested [VerifiablePresentation] type
   * @param creationExtras  [CreationExtras] of additional properties required for the presentation
   *
   * @return  a [VerifiablePresentation] that contains the salted poseidon hashes of all credential properties and
   *          the preimages of the given properties
   */
  suspend fun <T : VerifiablePresentation> createPresentation(
    credentials: List<Credential>,
    presentationType: KClass<T>,
    creationExtras: CreationExtras?
  ): T {
    val service: CreatePresentationUseCase = dependencies[typeOf<CreatePresentationUseCase>()]
    return service.fullPresentationOf(credentials, presentationType, creationExtras)
  }

  /**
   * Verify a given [VerifiablePresentation].
   *
   * @param presentation      the [VerifiablePresentation] to be verified
   * @param presentationType  reflection class of the given [VerifiablePresentation]
   *
   * @return true if the ZKP of the presentation is valid, false otherwise
   */
  suspend fun <T : VerifiablePresentation> verifyPresentation(
    presentation: T,
    presentationType: KClass<out T>,
  ): Boolean {
    val service: VerifyPresentationUseCase = dependencies[typeOf<VerifyPresentationUseCase>()]
    return service.verifyPresentation(presentation, presentationType)
  }

  /**
   * Store a given [VerifiablePresentation] inside the wallet.
   *
   * @param presentationType  the [KType] reflection of the presentation
   * @param presentation      the [VerifiablePresentation] instance to be stored
   */
  fun <T : VerifiablePresentation> storePresentation(presentationType: KType, presentation: T) {
    val service: PersistencePresentationService<T> =
      dependencies[typeOf<PersistencePresentationUseCase<*>>(), presentationType]
    service.storePresentation(presentation)
  }

  /**
   * Query a [VerifiablePresentation] by id and type.
   *
   * @param presentationType  the [KType] reflection of the presentation
   * @param idHash            the poseidon hash of the ID of the [VerifiablePresentation]
   *
   * @return stored [VerifiablePresentation] with the given ID, if it exists
   */
  fun <T : VerifiablePresentation> getPresentation(presentationType: KType, idHash: BigInteger): T? {
    val service: PersistencePresentationService<T> =
      dependencies[typeOf<PersistencePresentationUseCase<*>>(), presentationType]
    return service.getPresentation(idHash)
  }

  /**
   * Get all [VerifiablePresentation] of type [T].
   *
   * @param presentationType  the [KType] reflection of the presentation
   *
   * @return [Set] of all stored [VerifiablePresentation]s of type [T],
   * the set will be empty if no presentations are stored in the wallet.
   */
  fun <T : VerifiablePresentation> getAllPresentations(presentationType: KType): Set<T> {
    val service: PersistencePresentationService<T> =
      dependencies[typeOf<PersistencePresentationUseCase<*>>(), presentationType]
    return service.getAllPresentations()
  }

  /**
   * Delete the stored [VerifiablePresentation] with the given ID and type [T].
   *
   * @param presentationType  the [KType] reflection of the presentation
   * @param idHash            the poseidon hash of the presentation ID
   */
  fun <T : VerifiablePresentation> deletePresentation(presentationType: KType, idHash: BigInteger) {
    val service: PersistencePresentationService<T> =
      dependencies[typeOf<PersistencePresentationUseCase<*>>(), presentationType]
    service.deletePresentation(idHash)
  }
  //endregion

  companion object {
    /**
     * Invoke operator to mimic a suspend constructor for [Wallet].
     *
     * This is because the EDDSASignatureService has a suspend build function.
     * Kotlin doesn't allow suspend-calls in constructors or init blocks.
     * So by overriding the invoke operator [Wallet] objects can be created with the same syntax but in a suspend manner.
     *
     * Example of usage:
     * ```
     *  val wallet = Wallet() // This will call the invoke operator not the constructor!
     * ```
     *
     * @param init  lambda that allows to bind custom service implementations at instance creation using [Wallet.single] or
     * [Wallet.factory], if no init lambda is provided, the wallet will be initialized with generic default implementations.
     */
    suspend operator fun invoke(init: (suspend Wallet.() -> Unit)? = null): Wallet {
      val eddsa = EDDSASignatureService()
      val (private, public) = eddsa.keyPair()

      val wallet = Wallet(private, public)
      init?.invoke(wallet) ?: wallet.defaultDependencyInit()

      return wallet
    }
  }
}

//region reified dependency injection methods
/**
 * Extension function for [Wallet] to bind a custom **singleton** dependency to [KType] [T].
 *
 * This function uses the reified idiom ot avoid explicit declaration of [T] and is a wrapper around
 * [Wallet.single].
 *
 * Example of usage:
 * ```
 * val wallet = Wallet {
 *   single<GenericCredentialRepository<ConsentCredential>> {
 *     InMemoryGenericCredentialRepository<ConsentCredential>()
 *   }
 * }
 * ```
 *
 * @param service init lambda for the service to inject
 *
 * @receiver [Wallet]
 */
inline fun <reified T> Wallet.single(noinline service: () -> Any) = single(typeOf<T>(), service)

/**
 * Extension function for [Wallet] to **bind a factory** for a custom dependency to [KType] [T].
 *
 * This function uses the reified idiom ot avoid explicit declaration of [T] and is a wrapper around
 * [Wallet.factory].
 *
 * Example of usage:
 * ```
 * val wallet = Wallet {
 *   factory<StoreCredentialUseCase<ConsentCredential>> {
 *     StoreCredentialService<ConsentCredential>(
 *       get<GenericCredentialRepository<ConsentCredential>>()
 *     )
 *   }
 * }
 * ```
 *
 * @param service init lambda for the service to inject
 *
 * @receiver [Wallet]
 */
inline fun <reified T> Wallet.factory(noinline service: () -> Any) = factory(typeOf<T>(), service)

/**
 * Extension function for [Wallet] to get the instance of injected custom dependencies at runtime.
 *
 * This function uses the reified idiom ot avoid explicit declaration of [T] and is a wrapper around
 * [Wallet.get].
 *
 * Example of usage:
 * ```
 * val wallet = Wallet {
 *   factory<StoreCredentialUseCase<ConsentCredential>> {
 *     StoreCredentialService<ConsentCredential>(
 *       get<GenericCredentialRepository<ConsentCredential>>()
 *     )
 *   }
 * }
 * ```
 *
 * @param T the [KType] reflection the service was associated to beforehand
 *
 * @return the injected service
 *
 * @throws NotImplementedError if no service was associated beforehand
 *
 * @receiver [Wallet]
 */
inline fun <reified T> Wallet.get(): T = get(typeOf<T>())

/**
 * Extension function that provides a more concise way to register credential types using type inference.
 *
 * This inline function serves as a convenience wrapper around [Wallet.registerCredential], allowing the
 * registration of credential types without explicitly specifying the class reference.
 *
 * Example of usage:
 * ```
 * val wallet = Wallet()
 * wallet.registerCredential<CustomCredential>() // Uses type inference
 * ```
 *
 * @param T The type of the credential that extends the base [Credential] class
 *
 * @see Wallet.registerCredential
 * @see Credential
 */
inline fun <reified T: Credential> Wallet.registerCredential() = registerCredential(T::class)

/**
 * Extension function that provides a more concise way to register presentation types using type inference.
 *
 * This inline function serves as a convenience wrapper around [Wallet.registerPresentation], allowing the
 * registration of presentation types without explicitly specifying the class reference.
 *
 * Example of usage:
 * ```
 * val wallet = Wallet()
 * wallet.registerPresentation<CustomPresentation>() // Uses type inference
 * ```
 *
 * @param T The type of the presentation that extends the base [VerifiablePresentation] class
 *
 * @see Wallet.registerPresentation
 * @see VerifiablePresentation
 */
inline fun <reified T: VerifiablePresentation> Wallet.registerPresentation() = registerPresentation(T::class)
//endregion

//region reified credential-related methods
/**
 * Extension function for the [Wallet] to store a given [Credential] inside the wallet.
 * This function uses the reified idiom to avoid explicit declaration of the Credential type
 * and is a wrapper function around the storeCredential() method of [Wallet].
 *
 * Example of usage:
 * ```
 *    val wallet = Wallet()
 *    val credential = ConsentCredential()
 *
 *    wallet.storeCredential(credential)
 * ```
 *
 * @receiver [Wallet]
 *
 * @param credential  the [Credential] instance to be stored
 *
 * @see Wallet.storeCredential
 */
inline fun <reified T: Credential> Wallet.storeCredential(credential: T) = storeCredential(typeOf<T>(), credential)

/**
 * Extension function for [Wallet] to issue a new [Credential] of the specified type and given parameters.
 *
 * This function utilise the reified idiom to avoid explicit declaration of the [Credential] type
 * and is a wrapper function around the issueCredential() method of [Wallet].
 *
 * Example of usage:
 * ```
 *    val wallet = Wallet()
 *    val credential: PrequalificationCredential = wallet.issueCredential(/* ... */)
 * ```
 *
 * @receiver [Wallet]
 *
 * @param holder          the holders [PublicKey] used as unique id
 * @param extras          [CreationExtras] containing key-value pairs of [Credential] specific properties
 *
 * @return a new instance of the given [Credential] type
 *
 * @throws [MissingCreationExtraException] for invalid extras
 *
 * @see Wallet.issueCredential
 */
inline fun <reified T : Credential> Wallet.issueCredential(
  holder: PublicKey,
  extras: HashableCreationExtras<PoseidonHashable<out Any>>,
  privateKey2: PrivateKey? = null
): T =
  issueCredential(T::class, holder, extras,privateKey2)

/**
 * Extension function for the [Wallet] to query all [Credential] by given type [T].
 * This function utilise the reified idiom to avoid explicit declaration of the [Credential] type
 * and is a wrapper function around the getCredential() method of [Wallet].
 *
 * Example of usage:
 * ```
 *    val wallet = Wallet()
 *    val credential: PrequalificationCredential = wallet.getCredential(/* ... */)
 * ```
 *
 * @receiver [Wallet]
 *
 * @param id the id of the [Credential]
 *
 * @return stored [Credential] with the given ID, if it exists
 *
 * @throws NoSuchCredentialException if no [Credential] can be found for the given ID
 *
 * @see Wallet.getCredential
 */
inline fun <reified T: Credential> Wallet.getCredential(id: Uuid): T = getCredential(typeOf<T>(), id)

/**
 * Extension function for the [Wallet] to query all [Credential]s of a given type [T].
 * This function utilise the reified idiom to avoid explicit declaration of the [Credential] type
 * and is a wrapper function around the getAllCredentials() method of [Wallet].
 *
 * Example of usage:
 * ```
 *    val wallet = Wallet()
 *    val credentials: Set<PrequalificationCredential> = wallet.getAllCredential(/* ... */)
 * ```
 *
 * @receiver [Wallet]
 *
 * @return [Set] of all stored Credentials of type T, the set will be empty if no credentials are stored in the wallet.
 *
 * @see Wallet.getAllCredentials
 */
inline fun <reified T: Credential> Wallet.getAllCredentials(): Set<T> = getAllCredentials(typeOf<T>())

/**
 * Extension function for the [Wallet] to delete the stored [Credential] with the given ID and type [T].
 * This function utilise the reified idiom to avoid explicit declaration of the [Credential] type
 * and is a wrapper function around the deleteCredential() method of [Wallet].
 *
 * Example of usage:
 * ```
 *    wallet.deleteCredential<ConsentCredential>(/* ... */)
 * ```
 *
 * @receiver [Wallet]
 *
 * @param id the ID of the credential
 *
 * @see Wallet.deleteCredential
 */
inline fun <reified T: Credential> Wallet.deleteCredential(id: Uuid) = deleteCredential<T>(typeOf<T>(), id)
//endregion

//region reified presentation-related methods
/**
 * Extension function to create an arbitrary [VerifiablePresentation] of a given [Credential]
 * @receiver [Wallet]
 *
 * @param credentials   the [Credential] the [VerifiablePresentation] should be created from
 *
 * @return  a [VerifiablePresentation] that contains the salted poseidon hashes of all credential properties and
 *          the preimages of the given properties
 *
 * @see Wallet.createPresentation
 */
suspend inline fun <reified T : VerifiablePresentation> Wallet.createPresentation(
  credentials: List<Credential>,
  creationExtras: CreationExtras?
  ): T = createPresentation(credentials, T::class, creationExtras)

/**
 * Extension function to verify an all-purpose [VerifiablePresentation] without explicit declaration of the presentation
 * type.
 *
 * @receiver [Wallet]
 *
 * @param presentation  the [VerifiablePresentation] to be verified
 *
 * @return true if the ZKP of the presentation is valid, false otherwise
 *
 * @see Wallet.verifyPresentation
 */
suspend inline fun <reified T : VerifiablePresentation> Wallet.verifyPresentation(
  presentation: T
): Boolean = verifyPresentation(presentation, presentation::class)

/**
 * Extension function for the [Wallet] to store a given [VerifiablePresentation] inside the wallet.
 * This function uses the reified idiom to avoid explicit declaration of the Presentation type
 * and is a wrapper function around the storePresentation() method of [Wallet].
 *
 * Example of usage:
 * ```
 *    val wallet = Wallet()
 *    val presentation = ConsentPresentation()
 *
 *    wallet.storePresentation(presentation)
 * ```
 *
 * @receiver [Wallet]
 *
 * @param presentation  the [VerifiablePresentation] instance to be stored
 *
 * @see Wallet.storePresentation
 */
inline fun <reified T : VerifiablePresentation> Wallet.storePresentation(presentation: T) =
  storePresentation(typeOf<T>(), presentation)

/**
 * Extension function for the [Wallet] to query all [VerifiablePresentation] by given type [T].
 * This function utilise the reified idiom to avoid explicit declaration of the [VerifiablePresentation] type
 * and is a wrapper function around the getPresentation() method of [Wallet].
 *
 * Example of usage:
 * ```
 *    val wallet = Wallet()
 *    val presentation: PrequalificationPresentation = wallet.getPresentation(/* ... */)
 * ```
 *
 * @receiver [Wallet]
 *
 * @param idHash the poseidon hash of the id of the [VerifiablePresentation]
 *
 * @return stored [VerifiablePresentation] with the given ID, if it exists
 *
 *
 * @see Wallet.getPresentation
 */
inline fun <reified T : VerifiablePresentation> Wallet.getPresentation(idHash: BigInteger): T? =
  getPresentation(typeOf<T>(), idHash)

/**
 * Extension function for the [Wallet] to query all [VerifiablePresentation]s of a given type [T].
 * This function utilise the reified idiom to avoid explicit declaration of the [VerifiablePresentation] type
 * and is a wrapper function around the getAllPresentations() method of [Wallet].
 *
 * Example of usage:
 * ```
 *    val wallet = Wallet()
 *    val presentations: Set<PrequalificationVerifiablePresentation<*>> = wallet.getAllPresentation(/* ... */)
 * ```
 *
 * @receiver [Wallet]
 *
 * @return [Set] of all stored Presentations of type T, the set will be empty if no presentations are stored in the wallet.
 *
 * @see Wallet.getAllPresentations
 */
inline fun <reified T : VerifiablePresentation> Wallet.getAllPresentations(): Set<T> =
  getAllPresentations(typeOf<T>())

/**
 * Extension function for the [Wallet] to delete the stored [VerifiablePresentation] with the given ID and type [T].
 * This function utilise the reified idiom to avoid explicit declaration of the [VerifiablePresentation] type
 * and is a wrapper function around the deletePresentation() method of [Wallet].
 *
 * Example of usage:
 * ```
 *    wallet.deletePresentation<ConsentVerifiablePresentation<*>>(/* ... */)
 * ```
 *
 * @receiver [Wallet]
 *
 * @param idHash the poseidon hash of the id of the [VerifiablePresentation]
 *
 * @see Wallet.deletePresentation
 */
inline fun <reified T : VerifiablePresentation> Wallet.deletePresentation(idHash: BigInteger) =
  deletePresentation<T>(typeOf<T>(), idHash)
//endregion

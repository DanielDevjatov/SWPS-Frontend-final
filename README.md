## Getting Started

Make sure to see the Wiki for more information.

- [Prerequisites](#prerequisites)
- [Import the wallet](#import-the-wallet-as-a-dependency)
- [Basic Usage](#basic-usage)
- [Creating a Custom Credential](#creating-a-custom-credential)
- [Creating a Custom Presentation](#creating-a-custom-presentation)
- [Creating a Custom Circuit](#creating-a-custom-circuit)

---

### Prerequisites

* Install **JDK 21.0.7**
```bash
sudo apt install openjdk-21-jdk
```
* Ensure **Gradle** is installed
```bash
sudo apt install gradle
```
* Install NodeJS (if you are using the JS target)

---

### Contribute

If you want to contribute to the wallet, please refer to the 
[Contributing Guide](https://gitlab.cc-asp.fraunhofer.de/deer/deer-prototype/-/wikis/Contribution) in the Wiki.
This Readme focuses on the usage of the wallet and its import as a Gradle dependency.

---

### Import the wallet as a dependency

To import the published wallet package into your project, you have to configure projects `gradle.build` file with
more than just the dependency.
For more details refer to the [Import](https://gitlab.cc-asp.fraunhofer.de/deer/deer-prototype/-/wikis/Import) section 
in the project wiki.

#### Add required plugins

```kotlin
plugins {
    kotlin("multiplatform") version "<Version>"
    kotlin("plugin.serialization") version "<Version>"
    id("dev.opensavvy.resources.consumer") version "0.5.1"
}
```

#### Configure the maven repository

```kotlin
repositories {
    mavenCentral()
    gradlePluginPortal()
    maven {
        url = uri("https://gitlab.cc-asp.fraunhofer.de/api/v4/projects/64448/packages/maven")
        credentials(HttpHeaderCredentials::class) {
            name = "Private-Token"
            value = findProperty("gitLabPrivateToken") as String? 
            // the variable resides in $GRADLE_USER_HOME/gradle.properties
        }
        authentication {
            create("header", HttpHeaderAuthentication::class)
        }
    }
}
```

#### Add the dependencies and include required JS resources in your build

```kotlin
kotlin {
    
    js(IR) { /*...*/ }

    // Configure the JS resources consumer plugin
    kotlinJsResConsumer {
        directory.set("")
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.fim.wallet:wallet:0.1.0")
        }
        jsMain.dependencies {
            // Include the JS dependencies of the wallet package explicitly
            implementation("org.fim.wallet:wallet-js:0.1.0")
        }
        dependencies {
            // Configure the JS dependencies of the wallet package as the source for the JS resources consumer plugin
            jsConsumedResources("org.fim.wallet:wallet-js:0.1.0")
        }
    }
}
```

For larger ZKPs, the `NODE_OPTIONS="--max-old-space-size=12288"` has to be used

After cloning the repository, run the following command in the project root directory to initialize the circomlib submodule, required to compile the circom-files:

```bash
git submodule update --init --recursive --remote
```

When run the following in the project root directory to verify everything is working:
```bash
./gradlew check
```

---

### Basic Usage

For a more detailed usage example, refer to the [Getting Started](https://gitlab.cc-asp.fraunhofer.de/deer/deer-prototype/-/wikis/Home) 
section in the Wiki.

Your entry point is the `Wallet` class.
Everything should be done using this class.
```kotlin
//import org.fim.wallet.adapter.`in`.module.Wallet

val myWallet = Wallet() { /* optional custom initialization */ }
```

In case you add custom initialization steps but still want to use parts of the default `Wallet` initialization,
you can call `defaultDependencyInit()` **at the beginning** of the init lambda.
This will register an `EDDSASignatureProvider`, a `SnarkJSService` as `ZKPProvider` as well as all the default services
for `ConsentCredential`, `DeviceSpecificationsCredential`, `PrequalificationCredential` and the `AggregatorPresentation`.

**Attention:** If you do not call  `defaultDependencyInit()` your custom initialization will be used *exclusively*
(and thus may be incomplete).

---

### Creating a Custom Credential

To add a custom credential type:

1. **Extend the `Credential` class** and implement all abstract methods.
   
```kotlin
@Serializable
class MyCredential(
    override val id: Uuid,
    override val issuer: PublicKey,
    override val holder: PublicKey,
    override val signature: Signature,
    override val timestamp: Timestamp,
    override val root: BigInteger,
    // your custom members go here
    val myCustomMember: String,
    // ...
) : Credential() {
    // make sure to exclude the extras from serialization
    @Transient
    override val extras: HashableCreationExtras<PoseidonHashable<out Any>> /* = ... */
    
    // ...
    
    override fun members(): List<KProperty1<MyCredential, Any>> = listOf(
        MyCredential::id,
        MyCredential::issuer,
        MyCredential::holder,
        MyCredential::signature,
        MyCredential::timestamp,
        MyCredential::root,
        MyCredential::myCustomMember,
        /* ... */
    )
    
    // ...
}
```

2. **Add dependencies** to enable persistence and management of credentials:
    * `GenericCredentialRepository`
    * `DeleteCredentialUseCase`
    * `StoreCredentialUseCase`
    * `GetCredentialUseCase`

   You may use the generic predefined services or implement your own.
3. **Register the credential for serialization**

```kotlin
val myWallet = Wallet() {
    defaultDependencyInit() // make sure to call this first if you don't initialize all services on your own
    // ...
    // add your custom credential types here
    // make sure the DB repository is registered as singleton
    single<GenericCredentialRepository<MyCredential>> { InMemoryGenericCredentialRepository<MyCredential>() }
    factory<DeleteCredentialUseCase<MyCredential>> { DeleteCredentialService(get<GenericCredentialRepository<MyCredential>>()) }
    factory<StoreCredentialUseCase<MyCredential>> { StoreCredentialService(get<GenericCredentialRepository<MyCredential>>()) }
    factory<GetCredentialUseCase<MyCredential>> { GetCredentialService(get<GenericCredentialRepository<MyCredential>>()) }
    
    // eventually, register the credential for serialization
    registerCredential(MyCredential::class)

    // ...
}
```

---

### Creating a Custom Presentation

To add a custom presentation type:

1. **Extend the `VerifiablePresentation` class** and implement all abstract methods.
   
2. **Define and compile the proof circuit**

3. **Add a presentation repository and register the presentation for serialization**

```kotlin
val myWallet = Wallet() {
    //...
    single<GenericPresentationRepository<MyPresentation>> { InMemoryGenericPresentationRepository<MyPresentation>() }
    registerPresentation(MyPresentation::class)
    //...
}
```

---

### Creating a Custom Circuit

Compiling custom circuits requires the Circom compiler.
You can find the installation instructions [here](https://docs.circom.io/getting-started/installation/).

Further you need to install the `snarkjs` tool.
You can find the installation instructions [here](https://github.com/iden3/snarkjs#preliminaries).

To add a custom circuit to your project create a new directory in `src/main/resources/snarkjs_ceremony` and add your
circom-file there. Depending on your target, the path may differ with `commonMain` or `jsMain`.

You may also want to import the [circomlib](https://github.com/iden3/circomlib) as a submodule in your project to use
predefined circuits.

To compile your circuit copy this bash script into your `snarkjs_ceremony` folder and name it `compile.sh`:
```shell
#!/bin/bash

if [ -z "$1" ]
  then
    echo "Pass the circuit name (without file ending)"
    exit 1
fi

circom $1.circom --sym --r1cs --wasm -o $1

snarkjs groth16 setup $1/$1.r1cs pot17_final.ptau $1/$1.zkey
```

Now you can compile your circuit by running:
```shell
chmod +x compile.sh #Give the script execution permissions (only required once)
./compile.sh my_custom_circuit
```

`my_custom_circuit` is the name of your circuit without the `.circom` file ending.

This will create a `my_custom_circuit` folder in your `snarkjs_ceremony` directory there the wallet will be able to
locate all the required artifacts to generate proofs using this circuit.

---

### Custom Use Case Implementations

For all **predefined** use cases you may also implement your own custom services.
This may be useful if you want to add custom logic that extends the functionality of the general service implementations.
For example, that could be adapter classes for other (not In-Memory) Databases or an Issuer Service for a custom credential that involves
additional steps.

To inject a custom service implementation for a specific credential type:

1. Implement a use case interface
2. Register the use case implementation inside the `Wallet` init lambda.

```kotlin

class MyDataBaseService <T: Credential> : GenericCredentialRepository<T> {
    // ...
}

class MyIssueCredentialService : IssueCredentialUseCase {
    // ...
}
```
```kotlin
val myWallet = Wallet() {
    //...
    single<GenericPresentationRepository<MyCredential>> { MyDataBaseService() }
    factory<IssueCredentialUseCase> { MyIssueCredentialService() }
}

```

**Note:** Custom implementations are limited to existing use cases and their definitions.
If you want to add a complete new use case or if you like to adapt the interface of an existing use case
(not only use a custom implementation), you must contribute to the Wallet itself and implement the new use case
interface in the 'wallet' module.
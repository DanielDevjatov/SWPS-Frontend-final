import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.encodeToDynamic
import org.fim.wallet.adapter.`in`.module.Wallet
import org.fim.wallet.application.port.`in`.credential.GetCredentialUseCase
import org.fim.wallet.application.service.credential.GetCredentialService
import org.fim.wallet.domain.credential.ConsentCredential
import org.fim.wallet.domain.credential.Credential
import org.fim.wallet.domain.credential.DeviceSpecificationsCredential
import org.fim.wallet.domain.credential.PrequalificationCredential
import org.fim.wallet.domain.moduleJsonBuilder
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.uuid.Uuid

@JsModule("express")
@JsNonModule
external fun express(): dynamic

@JsModule("body-parser")
@JsNonModule
external val bodyParser: dynamic

@JsModule("swagger-jsdoc")
@JsNonModule
external fun swaggerJSDoc(options: dynamic): dynamic

@JsModule("swagger-ui-express")
@JsNonModule
external val swaggerUiExpress: dynamic

@JsModule("fs")
@JsNonModule
external val fs: dynamic

@Serializable
data class GetCredentialsResponse(
  val credentials: Credential
)

@Serializable
data class GetAllCredentialsResponse(
  val credentials: Set<Credential>
)

@OptIn(ExperimentalSerializationApi::class)
actual fun main() {
    val scope = MainScope()
    scope.launch {

        val wallet = Wallet()

        val path = "./kotlin/docs.js"
        if (!fs.existsSync(path)) {
            console.error("❌ File not found:", path)
        }

        val app = express()

        val types = listOf(
            typeOf<GetCredentialUseCase<PrequalificationCredential>>(),
            typeOf<GetCredentialUseCase<DeviceSpecificationsCredential>>(),
            typeOf<GetCredentialUseCase<ConsentCredential>>(),
        )

        fun getServiceFromTypeString(typeString: String): KType {
            return when (typeString) {
                "PrequalificationCredential" -> typeOf<GetCredentialUseCase<PrequalificationCredential>>()
                "DeviceSpecificationsCredential" -> typeOf<GetCredentialUseCase<DeviceSpecificationsCredential>>()
                "ConsentCredential" -> typeOf<GetCredentialUseCase<ConsentCredential>>()
                else -> throw IllegalArgumentException("No corresponding Use Case for input: $typeString")
            }
        }

        val swaggerSpec = swaggerJSDoc(
            js(
                """
        ({
            definition: {
                openapi: '3.0.0',
                info: {
                    title: 'Wallet API',
                    version: '1.0.0'
                }
            },
            apis: ['./kotlin/docs.js']
        })
    """
            )
        )

        app.use("/api-docs", swaggerUiExpress.serve, swaggerUiExpress.setup(swaggerSpec))
        app.use(bodyParser.json())

        app.post("/wallet/credential") { req, res ->
            try {
                val jsonElement = moduleJsonBuilder.parseToJsonElement(JSON.stringify(req.body.credential))

                wallet.storeCredential(jsonElement)

              res.send("Credential stored")
            } catch (_: SerializationException) {
              res.status(400).send("Invalid JSON")
            } catch (e: dynamic) {
              res.status(500).send(e.message ?: "Unknown error")
            }
        }

        app.get("/wallet/credential/:id") { req, res ->
            try {
                // Extract the id from the URL parameter
                val id = Uuid.parse(req.params.id.toString())

                // Assuming you want to get a specific credential type, you can adjust the credentialType here
                for (type in types) {
                    try {
                        val service: GetCredentialService<*> = wallet.get(type)
                        // Retrieve the credential from the wallet
                        val credential = service.getCredential(id)
                        // Send the found credential as the response
                      res.send(moduleJsonBuilder.encodeToDynamic(GetCredentialsResponse(credential)))
                      return@get
                    } catch (_: Exception) {
                      //Ignore not found exceptions
                    }
                }
              res.status(404).send("No credential found")
            } catch (_: IllegalArgumentException) {
              res.status(400).send("Invalid uuid format")
            } catch (e: dynamic) {
              res.status(500).send(e.message ?: "Unknown error")
            }
        }

        app.get("/wallet/credentials/:type") { req, res ->
           try {
             val type = req.params.type.toString()
               val service: GetCredentialService<*> = wallet.get(getServiceFromTypeString(type))

               val credentials = service.getAllCredentials()

               if (credentials.isEmpty()) {
                 res.status(404).send("No Credentials for $type found")
                   return@get
               }

             res.send(moduleJsonBuilder.encodeToDynamic(GetAllCredentialsResponse(credentials)))
           } catch (e: IllegalArgumentException) {
             res.status(400).send(e.message ?: "Invalid format")
           } catch (e: dynamic) {
             res.status(500).send(e.message ?: "Unknown error")
           }
        }

        app.listen(3001) {
            console.log("Docs at http://localhost:3001/api-docs")
        }
    }
}

/**
 * @openapi
 * components:
 *   schemas:
 *     CredentialType:
 *       type: string
 *       enum:
 *         - ConsentCredential
 *         - PrequalificationCredential
 *         - DeviceSpecificationsCredential
 *     Credential:
 *       type: object
 *       properties:
 *         type:
 *           $ref: '#/components/schemas/CredentialType'
 *         id:
 *           type: string
 *           format: uuid
 *         issuer:
 *           type: array
 *           items:
 *             type: array
 *             items:
 *               type: integer
 *         holder:
 *           type: array
 *           items:
 *             type: array
 *             items:
 *               type: integer
 *         signature:
 *           type: array
 *           items:
 *             type: integer
 *         timestamp:
 *           type: integer
 *           format: int64
 *         root:
 *           type: integer
 *           format: string
 *         deviceName:
 *           type: string
 *         deviceID:
 *           type: integer
 *           format: string
 *         deviceOEM:
 *           type: string
 *         electronicDescription:
 *           type: string
 *       required:
 *         - id
 *         - issuer
 *         - holder
 *         - signature
 *         - timestamp
 * paths:
 *   /wallet/credential:
 *     post:
 *       summary: Store a credential in the wallet
 *       requestBody:
 *         required: true
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 credential:
 *                   $ref: '#/components/schemas/Credential'
 *               required:
 *                 - credential
 *             example:
 *               credential:
 *                 type: "DeviceSpecificationsCredential"
 *                 id: "571a6bd5-d041-45e3-9842-378d2f1d791b"
 *                 issuer: [[-24,23,-96,119,-76,-74,111,-124,-97,121,-31,107,45,-51,-55,107,30,-57,86,-19,32,54,-47,-99,35,74,117,27,3,-18,105,91], [-28,-30,-119,-24,-23,-54,-54,-92,-27,-41,-128,13,49,4,-37,25,-124,19,12,-29,117,-38,-68,46,62,-55,8,-18,38,127,72,35]]
 *                 holder: [[-6,107,-114,-86,89,-48,60,-47,97,-88,76,97,-89,-128,-91,34,67,42,-38,-124,113,31,43,-90,60,5,-43,72,8,42,-89,-90], [-114,3,68,85,-46,2,-119,105,-79,59,-40,95,55,111,-77,14,126,45,-36,-43,-93,-56,107,63,105,49,-38,-76,89,88,32,42]]
 *                 signature: [92,72,47,-83,-124,-78,-57,32,-77,104,41,-94,10,-64,98,24,92,59,-91,-46,-34,77,-36,3,2,-43,43,84,-108,33,-8,94,9,-26,108,83,-8,-10,1,50,-107,-100,113,-42,75,100,-99,65,103,61,-59,6,-95,-44,91,-112,-6,-65,-117,55,-71,20,-86,-116]
 *                 timestamp: 9021718010370851243
 *                 root: "1234"
 *                 deviceName: "device01"
 *                 deviceID: "1234"
 *                 deviceOEM: "OEM"
 *                 electronicDescription: "some info"
 *       responses:
 *         '200':
 *           description: Credential stored successfully
 *           content:
 *             application/json:
 *               schema:
 *                 type: object
 *                 properties:
 *                   message:
 *                     type: string
 *                     example: Credential stored
 *         '400':
 *           description: Invalid input
 *         '500':
 *           description: Internal server error
 *   /wallet/credential/{id}:
 *     get:
 *       summary: Retrieve a credential by its ID
 *       parameters:
 *         - name: id
 *           in: path
 *           required: true
 *           description: The ID of the credential to retrieve
 *           schema:
 *             type: string
 *             format: uuid
 *       responses:
 *         '200':
 *           description: Successfully retrieved the credential
 *           content:
 *             application/json:
 *               schema:
 *                 type: object
 *                 properties:
 *                   credential:
 *                     $ref: '#/components/schemas/Credential'
 *         '404':
 *           description: Credential not found
 *         '400':
 *           description: Invalid ID format
 *         '500':
 *           description: Internal server error
 *   /wallet/credentials/{type}:
 *     get:
 *       summary: Retrieve all credentials of a certain type
 *       parameters:
 *         - name: type
 *           in: path
 *           required: true
 *           description: The type of credentials to retrieve (e.g., ConsentCredential, DeviceSpecificationsCredential, etc.)
 *           schema:
 *             $ref: '#/components/schemas/CredentialType'
 *             example: "DeviceSpecificationsCredential"
 *       responses:
 *         '200':
 *           description: Successfully retrieved the credentials
 *           content:
 *             application/json:
 *               schema:
 *                 type: object
 *                 properties:
 *                   credentials:
 *                     type: array
 *                     items:
 *                       $ref: '#/components/schemas/Credential'
 *         '400':
 *           description: Invalid type
 *         '500':
 *           description: Internal server error
 */
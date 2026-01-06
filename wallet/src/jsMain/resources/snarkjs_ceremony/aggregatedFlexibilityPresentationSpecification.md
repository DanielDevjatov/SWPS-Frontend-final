# AggregatedFlexiblityPresentation Specification
## Parameters
- agentCount: Number of Agents to be aggregated
- contentDepth: Depth of the content hashtree
- deviceSpecificationDepth: Depth of the deviceSpecification hashtree
- prequalificationDepth: Depth of the prequalification hashtree
- issuerCountContent: Number of possible Issuers
- issuerCountDeviceSpecification: Number of possible Issuers
- issuerCountPrequalification: Number of possible Issuers

## Input
- contentCredentials[*][agentCount]: Array containing the contentCredentials
- deviceSpecificationCredentials[*][agentCount]: Array containing the deviceSpecificationCredentials
- prequalificationCredentials[*][agentCount]: Array containing the prequalificationCredentials
- signatures[3][3][agentCount]: Array containing the Signatures of all credentials
- 
## Output
- 
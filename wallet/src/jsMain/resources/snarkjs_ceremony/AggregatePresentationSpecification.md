# Specification: aggregatedFlexibilityPresentation

## Purpose

The `aggregatedFlexibilityPresentation` circuit aggregates `n` Verifiable Credential Triples, ensuring the Verifier can be convinced that:

* Each credential is individually valid.
* All three credentials in each Triple correspond to the same entity.
* The aggregated flexibility (both input and output) across all Triples is correctly computed and presented.

## Inputs

* `deviceSpecificationCredentials[n]`: Array of device specification credentials.
* `prequalificationCredentials[n]`: Array of prequalification credentials.
* `consentCredentials[n]`: Array of consent credentials.
* `timeFrameStart`: Start of the evaluation time window.
* `timeFrameStop`: End of the evaluation time window.

## Outputs

* `flexibilityIn`: Total flexible energy available **for grid injection**.
* `flexibilityOut`: Total flexible energy available **for grid extraction**.
* **Error** if any credential fails verification or the Triple linkage is invalid.

## Algorithm

```rust
flexibilityIn = 0;
flexibilityOut = 0;

for i in 0..n {
    verify(deviceSpecificationCredentials[i], timeFrameStart, timeFrameStop);
    verify(prequalificationCredentials[i], timeFrameStart, timeFrameStop);
    verify(consentCredentials[i], timeFrameStart, timeFrameStop);

    verifyCredentialLink(
        deviceSpecificationCredentials[i],
        prequalificationCredentials[i],
        consentCredentials[i]
    );

    flexibilityIn += consentCredentials[i].flexibilityIn;
    flexibilityOut += consentCredentials[i].flexibilityOut;
}
return (flexibilityIn, flexibilityOut);
```
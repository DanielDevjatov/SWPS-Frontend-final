pragma circom 2.1.6;

include "SaltedPoseidonHashTemplate.circom";

template hashFlatCredential(N) {
    signal input salt;
    signal input id;
    signal input issuer[2];
    signal input holder[2];
    signal input signature;
    signal input timestamp;
    signal input fields[N-5];

    signal output hashes[N];

    component idHash = saltedPoseidon(1);
    component issuerHash = saltedPoseidon(2);
    component holderHash = saltedPoseidon(2);
    component signatureHash = saltedPoseidon(1);
    component timestampHash = saltedPoseidon(1);

    idHash.in[0] <== id;
    idHash.salt <== salt;
    hashes[0] <== idHash.hash;

    issuerHash.in[0] <== issuer[0];
    issuerHash.in[1] <== issuer[1];
    issuerHash.salt <== salt;
    hashes[1] <== issuerHash.hash;

    holderHash.in[0] <== holder[0];
    holderHash.in[1] <== holder[1];
    holderHash.salt <== salt;
    hashes[2] <== holderHash.hash;

    signatureHash.in[0] <== signature;
    signatureHash.salt <== salt;
    hashes[3] <== signatureHash.hash;

    timestampHash.in[0] <== timestamp;
    timestampHash.salt <== salt;
    hashes[4] <== timestampHash.hash;

    component generator[N-5];

    for (var i = 0; i < N-5; i++) {
        generator[i] = saltedPoseidon(1);

        generator[i].in[0] <== fields[i];
        generator[i].salt <== salt;

        hashes[i+5] <== generator[i].hash;
    }
}

pragma circom 2.1.6;

include "circomlib/circuits/poseidon.circom";

template saltedPoseidon(N) {
    signal input in[N];
    signal input salt;

    signal output hash;

    component generator = Poseidon(N+1);

    for (var i = 0; i < N; i++) {
        generator.inputs[i] <== in[i];
    }

    generator.inputs[N] <== salt;

    hash <== generator.out;
}

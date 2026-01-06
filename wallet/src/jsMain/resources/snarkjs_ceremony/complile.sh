#!/bin/bash

if [ -z "$1" ]
  then
    echo "Pass the circuit name (without file ending)"
    exit 1
fi

circom $1.circom --sym --r1cs --wasm -o $1

snarkjs groth16 setup $1/$1.r1cs pot17_final.ptau $1/$1.zkey


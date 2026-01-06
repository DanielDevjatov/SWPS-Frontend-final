pragma circom 2.1.6;

include "HashFlatCredentialTemplate.circom";

component main { public [salt] }= hashFlatCredential(8);

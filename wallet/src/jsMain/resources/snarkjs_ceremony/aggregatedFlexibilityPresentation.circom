pragma circom 2.1.6;

include "circomlib/circuits/eddsaposeidon.circom";
include "circomlib/circuits/poseidon.circom";
include "circomlib/circuits/comparators.circom";
include "lib/hashTree.circom";

template aggregatedFlexibilityPresentation(agentCount, deviceSpecificationDepth, prequalificationDepth, consentDepth) {
    var deviceSpecificationWidth = 2**deviceSpecificationDepth;
    var prequalificationWidth = 2**prequalificationDepth;
    var consentWidth = 2**consentDepth;

    signal input deviceSpecificationCredentials[agentCount][20];
    signal input prequalificationCredentials[agentCount][19];
    signal input consentCredentials[agentCount][33];
    signal input timeframe[2];

    signal output sumGive;
    signal output sumReceive;
    signal output timeframeOut[2];

    signal output deviceSpecificationPublicKeys[agentCount][2];
    signal output prequalificationPublicKeys[agentCount][2];
    signal output gridConnectionArea;


    //Checking deviceSpecification Credential integrity
    component deviceSpecificationTrees[agentCount];
    component deviceSpecificationSignatureVerifier[agentCount];

    for (var i = 0; i< agentCount; i++){

        //re-building deviceSpecification Credential tree
        deviceSpecificationTrees[i] = hashTree(2,deviceSpecificationDepth);

        for (var j = 0; j < 3; j++){
            deviceSpecificationTrees[i].values[j] <== deviceSpecificationCredentials[i][j];
        }
        deviceSpecificationTrees[i].values[3] <== deviceSpecificationCredentials[i][4];

        for (var j = 4; j < 8; j++){
            deviceSpecificationTrees[i].values[j] <== deviceSpecificationCredentials[i][j+2];
        }

        deviceSpecificationTrees[i].root === deviceSpecificationCredentials[i][5];


        deviceSpecificationSignatureVerifier[i] = EdDSAPoseidonVerifier();

        deviceSpecificationSignatureVerifier[i].enabled <== 1;
        deviceSpecificationSignatureVerifier[i].Ax <== deviceSpecificationCredentials[i][11];
        deviceSpecificationSignatureVerifier[i].Ay <== deviceSpecificationCredentials[i][12];
        deviceSpecificationSignatureVerifier[i].R8x <== deviceSpecificationCredentials[i][15];
        deviceSpecificationSignatureVerifier[i].R8y <== deviceSpecificationCredentials[i][16];
        deviceSpecificationSignatureVerifier[i].S <== deviceSpecificationCredentials[i][17];
        deviceSpecificationSignatureVerifier[i].M <== deviceSpecificationTrees[i].root;

        deviceSpecificationPublicKeys[i][0] <== deviceSpecificationCredentials[i][11];
        deviceSpecificationPublicKeys[i][1] <== deviceSpecificationCredentials[i][12];

    }

    //Checking prequalification Credential
    component prequalificationTrees[agentCount];
    component prequalificationSignatureVerifier[agentCount];

    for (var i = 0; i< agentCount; i++){
        prequalificationTrees[i] = hashTree(2,prequalificationDepth);


        for (var j = 0; j < 3; j++){
            prequalificationTrees[i].values[j] <== prequalificationCredentials[i][j];
        }
        prequalificationTrees[i].values[3] <== prequalificationCredentials[i][4];

        for (var j = 4; j < 7; j++){
            prequalificationTrees[i].values[j] <== prequalificationCredentials[i][j+2];
        }

        for (var j = 7; j < prequalificationWidth; j++){
            prequalificationTrees[i].values[j] <== 19014214495641488759237505126948346942972912379615652741039992445865937985820;
        }

        prequalificationTrees[i].root === prequalificationCredentials[i][5];


        prequalificationSignatureVerifier[i] = EdDSAPoseidonVerifier();

        prequalificationSignatureVerifier[i].enabled <== 1;
        prequalificationSignatureVerifier[i].Ax <== prequalificationCredentials[i][10];
        prequalificationSignatureVerifier[i].Ay <== prequalificationCredentials[i][11];
        prequalificationSignatureVerifier[i].R8x <== prequalificationCredentials[i][14];
        prequalificationSignatureVerifier[i].R8y <== prequalificationCredentials[i][15];
        prequalificationSignatureVerifier[i].S <== prequalificationCredentials[i][16];
        prequalificationSignatureVerifier[i].M <== prequalificationTrees[i].root;

        prequalificationPublicKeys[i][0] <== prequalificationCredentials[i][10];
        prequalificationPublicKeys[i][1] <== prequalificationCredentials[i][11];

    }


    //Checking consent Credential
        component consentTrees[agentCount];
        component consentTrees2[agentCount];
        component consentSignatureVerifier[agentCount];
        component consentSignatureVerifier2[agentCount];

        for (var i = 0; i< agentCount; i++){

            //re-building consent Credential tree (without HEM signatures)
            consentTrees[i] = hashTree(2,consentDepth);

            for (var j = 0; j < 3; j++){
                consentTrees[i].values[j] <== consentCredentials[i][j];
            }

            consentTrees[i].values[3] <== consentCredentials[i][4];

            for (var j = 4; j < 12; j++){
                consentTrees[i].values[j] <== consentCredentials[i][j+2];
            }

            for (var j = 12; j < consentWidth; j++){
                consentTrees[i].values[j] <== 19014214495641488759237505126948346942972912379615652741039992445865937985820;
            }

            consentTrees[i].root === consentCredentials[i][5];

            //verifying consent Credential HEM-signature
            consentSignatureVerifier[i] = EdDSAPoseidonVerifier();

            consentSignatureVerifier[i].enabled <== 1;
            consentSignatureVerifier[i].Ax <== consentCredentials[i][15];
            consentSignatureVerifier[i].Ay <== consentCredentials[i][16];
            consentSignatureVerifier[i].R8x <== consentCredentials[i][19];
            consentSignatureVerifier[i].R8y <== consentCredentials[i][20];
            consentSignatureVerifier[i].S <== consentCredentials[i][21];
            consentSignatureVerifier[i].M <== consentTrees[i].root;


            //re-building consent Credential tree (with HEM signatures)
            consentTrees2[i] = hashTree(2,consentDepth);

            for (var j = 0; j < 3; j++){
                consentTrees2[i].values[j] <== consentCredentials[i][j];
            }

            consentTrees2[i].values[3] <== consentCredentials[i][4];

            for (var j = 4; j < 6; j++){
                consentTrees2[i].values[j] <== consentCredentials[i][j+2];
            }

            for (var j = 6; j < 11; j++){
                consentTrees2[i].values[j] <== consentCredentials[i][j+3];
            }

            for (var j = 11; j < consentWidth; j++){
                consentTrees2[i].values[j] <== 19014214495641488759237505126948346942972912379615652741039992445865937985820;
            }


            //verifying consent Credential signature
            consentSignatureVerifier2[i] = EdDSAPoseidonVerifier();

            consentSignatureVerifier2[i].enabled <== 1;
            consentSignatureVerifier2[i].Ax <== consentCredentials[i][24];
            consentSignatureVerifier2[i].Ay <== consentCredentials[i][25];
            consentSignatureVerifier2[i].R8x <== consentCredentials[i][26];
            consentSignatureVerifier2[i].R8y <== consentCredentials[i][27];
            consentSignatureVerifier2[i].S <== consentCredentials[i][28];
            consentSignatureVerifier2[i].M <== consentTrees2[i].root;



        }

    //Checking Credential connection

    //same id
    for (var i = 0; i< agentCount; i++){
        prequalificationCredentials[i][18] === deviceSpecificationCredentials[i][19];
        consentCredentials[i][23] === deviceSpecificationCredentials[i][19];
    }

   //owner is holder of prequalification and issuer of consent
   for (var i = 0; i< agentCount; i++){
       prequalificationCredentials[i][2] === consentCredentials[i][1];
   }

   //HEM is holder of deviceSpecification and has secondary signature in consent
   for (var i = 0; i< agentCount; i++){
       deviceSpecificationCredentials[i][2] === consentCredentials[i][7];
   }

    //ConsentForUsage is "FLEX"
    //Prequalification is "FLEX"
    for( var i = 0; i < agentCount; i++){
        consentCredentials[i][11] === 12093093422125889691716787179872312759268893118944400500597376732005129433846;
        prequalificationCredentials[i][7] === 12093093422125889691716787179872312759268893118944400500597376732005129433846;
    }

    //calculating the total flexibilitygive
    signal aggregatedFlexibilityGive[agentCount];

    aggregatedFlexibilityGive[0] <== consentCredentials[0][23];
    for (var i = 1; i< agentCount; i++){
        aggregatedFlexibilityGive[i] <== aggregatedFlexibilityGive[i-1] + consentCredentials[i][23];
    }


    sumGive <== aggregatedFlexibilityGive[agentCount-1];

     //calculating the total flexibilityreceive
    signal aggregatedFlexibilityReceive[agentCount];

    aggregatedFlexibilityReceive[0] <== consentCredentials[0][24];
    for (var i = 1; i< agentCount; i++){
        aggregatedFlexibilityReceive[i] <== aggregatedFlexibilityReceive[i-1] + consentCredentials[i][24];
    }


    sumReceive <== aggregatedFlexibilityReceive[agentCount-1];


    //validating timeframe
    component startTimeVerifier[agentCount];
    component endTimeVerifier[agentCount];
    for (var i = 0; i< agentCount; i++){
        startTimeVerifier[i] = LessEqThan(32);
        startTimeVerifier[i].in[0] <== consentCredentials[i][31];
        startTimeVerifier[i].in[1] <== timeframe[0];
        
        endTimeVerifier[i] = LessEqThan(32);
        endTimeVerifier[i].in[0] <==  timeframe[1];
        endTimeVerifier[i].in[1] <==  consentCredentials[i][32];
    }

    timeframeOut[0] <== timeframe[0];
    timeframeOut[1] <== timeframe[1];

    //same gridConnection area;
    gridConnectionArea <== prequalificationCredentials[0][8];
    for (var i = 1; i < agentCount; i++){
        prequalificationCredentials[i][8] === prequalificationCredentials[i-1][8];
    }
}

component main = aggregatedFlexibilityPresentation(1, 3, 3, 4);
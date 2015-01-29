#! /bin/bash

export IN=horizons-planet-data
export OUT=php-ephem
export CLASSPATH=../bin
export MAIN=com.marklipson.astrotools.ProcessHorizonData

java -cp $CLASSPATH $MAIN -php $IN/sun.txt xxx $OUT/sun.lng
java -cp $CLASSPATH $MAIN -php $IN/moon1.txt xxx $OUT/moon1.lng
java -cp $CLASSPATH $MAIN -php $IN/moon2.txt xxx $OUT/moon2.lng
java -cp $CLASSPATH $MAIN -php $IN/mercury.txt xxx $OUT/mercury.lng
java -cp $CLASSPATH $MAIN -php $IN/venus.txt xxx $OUT/venus.lng
java -cp $CLASSPATH $MAIN -php $IN/mars.txt xxx $OUT/mars.lng
java -cp $CLASSPATH $MAIN -php $IN/jupiter.txt xxx $OUT/jupiter.lng
java -cp $CLASSPATH $MAIN -php $IN/saturn.txt xxx $OUT/saturn.lng
java -cp $CLASSPATH $MAIN -php $IN/uranus.txt xxx $OUT/uranus.lng
java -cp $CLASSPATH $MAIN -php $IN/neptune.txt xxx $OUT/neptune.lng
java -cp $CLASSPATH $MAIN -php $IN/pluto.txt xxx $OUT/pluto.lng

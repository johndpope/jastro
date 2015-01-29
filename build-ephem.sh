#! /bin/bash

class=com.marklipson.astrotools.ProcessHorizonData
inPath=ephem-sources
outPath=src/com/marklipson/astrologyclock/resources/ephemeris

java -cp bin $class -oe $inPath/chiron-elements-1600-2200.txt chiron $outPath/chiron.elem
java -cp bin $class -oe $inPath/ceres-elements-1600-2200.txt ceres $outPath/ceres.elem
java -cp bin $class -oe $inPath/sedna-elements-1600-2200.txt sedna $outPath/sedna.elem

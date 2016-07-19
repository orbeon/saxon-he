#!/bin/sh

#jdkdir=/usr/lib/jvm/java-7-oracle/include

# $jdkdir/bin/javac MyClassInDll.java

#jc =p MyDll.prj
#rm -rf MyDll_jetpdb

gcc -m32 -I$jdkdir/include -I$jdkdir/include/linux  ../../Saxon.C.API/SaxonCGlue.c ../../Saxon.C.API/SaxonCProcessor.c ../../Saxon.C.API/SaxonCXPath.c  testXSLT.c -o testXSLT -ldl -lc $1

gcc -m32 -I$jdkdir/include -I$jdkdir/include/linux  ../../Saxon.C.API/SaxonCGlue.c ../../Saxon.C.API/SaxonCProcessor.c  testXQuery.c -o testXQuery -ldl -lc $1

gcc -m32 -I$jdkdir/include -I$jdkdir/include/linux  ../../Saxon.C.API/SaxonCGlue.c ../../Saxon.C.API/SaxonCProcessor.c ../../Saxon.C.API/SaxonCXPath.c testXPath.c -o testXPath -ldl -lc $1


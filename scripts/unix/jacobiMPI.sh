#!/bin/sh

echo
echo --- MPI deployment example ---------------------------------------------

echo " --- RUNNING JACOBI ON LOCALHOST ---"

workingDir=`dirname $0`
. $workingDir/env.sh
PROACTIVE=$workingDir/../..
EXAMPLES=$PROACTIVE/src/org/objectweb/proactive/examples/mpi
if [ -f /usr/bin/mpicc ]
then
echo "Compiling source file..."
/usr/bin/mpicc $EXAMPLES/jacobi.c -lm -o $EXAMPLES/jacobi
else
echo "ERROR: you need \"/usr/bin/mpicc\" to compile MPI\C source file"
exit 127
fi
if [ -f /usr/bin/lamboot ]
then 
/usr/bin/lamboot
else 
echo "ERROR: you need \"lamboot\" to start a Local Area Multicomputer simulator"	
fi

XMLDESCRIPTOR=$PROACTIVE/descriptors/MPI-descriptor.xml

$JAVACMD -Dlog4j.configuration=file:$PROACTIVE/scripts/proactive-log4j -Dproactive.rmi.port=6099 org.objectweb.proactive.examples.mpi.Jacobi  $XMLDESCRIPTOR 

echo "Killing lam daemon..."
killall lamd

echo
echo ------------------------------------------------------------

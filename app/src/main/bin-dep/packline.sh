#!/bin/bash
# BASHMODE="readlink"

# set -x #echo on

#--------------------------------------------
#  set parameters
#--------------------------------------------

if [ "$BASHMODE" = "readlink" ] ; then
# Absolute path to this script, e.g. /home/user/bin/foo.sh
SCRIPT=$(readlink -f $0)
# Absolute path this script is in, thus /home/user/bin
SCRIPTDIR=$(dirname $SCRIPT)
else
# Absolute path to this script
SCRIPT="${BASH_SOURCE[0]}"
# Absolute path this script is in
SCRIPTDIR="$(cd $(dirname ${SCRIPT}) ; pwd)"
fi
# echo "Script path: ${SCRIPTDIR}/$(basename "${SCRIPT}")"
# Change dir to script dir
RESOLVED_APP_HOME=`cd "$SCRIPTDIR/.."; pwd`

# Determine OS type and processor architecture
case "$OSTYPE" in
  darwin*)
    ARCH=mac
    ;;
  *)
    # Linux OS detected, determine processrot architecture
    if $(uname -p | grep 'i686'); then
	ARCH=linux-i686
    else
	ARCH=linux-x86-64
    fi
    ;;
esac

JARFILE=$RESOLVED_APP_HOME/bin/packline.jar
LIBDIR=$RESOLVED_APP_HOME/lib/*.jar
APP_CONFIG=$RESOLVED_APP_HOME/conf/packline.xconf
RXTX_LIBS=$RESOLVED_APP_HOME/lib/rxtx/$ARCH

SAVEIFS=$IFS
IFS=$(echo -en "\n\b")

LOCALCLASSPATH=""
for i in $LIBDIR
do
    # if the directory is empty, then it will return the input string this is stupid, so case for it
    if [ "$i" != "$LIBDIR" ] ; then
      if [ -z "$LOCALCLASSPATH" ] ; then
        LOCALCLASSPATH=$i
      else
        LOCALCLASSPATH=$i:$LOCALCLASSPATH
      fi
    fi
done
LOCALCLASSPATH=$JARFILE:$LOCALCLASSPATH

IFS=$SAVEIFS

RXTX_PORTS=/dev/ttyS0:/dev/ttyS1:/dev/ttyS2:/dev/ttyS3:/dev/ttyACM0:/dev/ttyACM1:/dev/ttyACM2:/dev/ttyACM3:/dev/ttyUSB0:/dev/ttyUSB1:/dev/ttyUSB2:/dev/ttyUSB3:/dev/rfcomm0:/dev/rfcomm1:/dev/rfcomm2:/dev/rfcomm3

JAVA_OPTS="$JAVA_OPTS -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger -Dprism.verbose=true \"-Djava.library.path=$RXTX_LIBS\" -Dgnu.io.rxtx.SerialPorts=$RXTX_PORTS -Xms1024m -Xmx1024m"

#--------------------------------------------
#  gather command line arguments
#--------------------------------------------

CMD_LINE_ARGS=
for ARG in "$@" ; do
  CMD_LINE_ARGS="$CMD_LINE_ARGS \"$ARG\""
done 

# CMD_LINE_ARGS="$CMD_LINE_ARGS \"--debug=true\""

#--------------------------------------------
#  run program
#--------------------------------------------

exec_command="java $JAVA_OPTS -cp \"$LOCALCLASSPATH\" ru.aplix.packline.App \"--config=$APP_CONFIG\" $CMD_LINE_ARGS"
eval $exec_command

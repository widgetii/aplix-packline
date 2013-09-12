#!/bin/bash
# BASHMODE="readlink"

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
SCRIPTDIR="$(cd "$(dirname "${SCRIPT}")" ; pwd)"
fi
# echo "Script path: ${SCRIPTDIR}/$(basename "${SCRIPT}")"
# Change dir to script dir
RESOLVED_APP_HOME=`cd "$SCRIPTDIR/.."; pwd`

JARFILE=$RESOLVED_APP_HOME/bin/packline.jar
LIBDIR=$RESOLVED_APP_HOME/lib/*.jar
APP_CONFIG=$RESOLVED_APP_HOME/conf/packline.xconf

LOCALCLASSPATH=$JARFILE
for i in ${LIBDIR}
do
    # if the directory is empty, then it will return the input string this is stupid, so case for it
    if [ "$i" != "${LIBDIR}" ] ; then
      if [ -z "$LOCALCLASSPATH" ] ; then
        LOCALCLASSPATH=$i
      else
        LOCALCLASSPATH="$i":$LOCALCLASSPATH
      fi
    fi
done 

JAVA_OPTS="$JAVA_OPTS -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger -Dprism.verbose=true"

#--------------------------------------------
#  gather command line arguments
#--------------------------------------------

CMD_LINE_ARGS=
for ARG in "$@" ; do
  CMD_LINE_ARGS="$CMD_LINE_ARGS \"$ARG\""
done 

#--------------------------------------------
#  run program
#--------------------------------------------

exec_command="java $JAVA_OPTS -cp \"$LOCALCLASSPATH\" com.javafx.main.Main \"--config=$APP_CONFIG\" $CMD_LINE_ARGS"
eval $exec_command

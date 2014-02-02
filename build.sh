#!/bin/bash

#
# build libs and apps
#

mvn -Plibs clean install
if [ $? != 0 ]; then
exit 1
fi

mvn -Papps clean package
if [ $? != 0 ]; then
exit 1
fi

#
# build installer and launcher
#

# Absolute path to this script
SCRIPT="${BASH_SOURCE[0]}"
# Absolute path this script is in
SCRIPTDIR="$(cd $(dirname ${SCRIPT}) ; pwd)"
cd ${SCRIPTDIR}/jws

chmod +x build.sh
./build.sh

cd ${SCRIPTDIR}

#
# move all parts of distribution pack to one place
#

echo
echo Moving all parts of distribution pack to one place

rm -r -f "${SCRIPTDIR}/jws/target/dist/"
mkdir -p "${SCRIPTDIR}/jws/target/dist/"

mv -f "${SCRIPTDIR}/app/target/dist-pack/"/* -t "${SCRIPTDIR}/jws/target/dist/"
mv -f "${SCRIPTDIR}/jws/target/installer/dist/"/* -t "${SCRIPTDIR}/jws/target/dist/"
mv -f "${SCRIPTDIR}/jws/target/launcher/dist/"/* -t "${SCRIPTDIR}/jws/target/dist/"

echo Done

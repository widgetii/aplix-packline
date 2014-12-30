#!/bin/bash

mvn -Pinstaller
if [ $? != 0 ]; then
exit 1
fi

mvn -Plauncher
if [ $? != 0 ]; then
exit 1
fi

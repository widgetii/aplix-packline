#!/bin/bash

mvn -Pinstaller clean package
if [ $? != 0 ]; then
exit 1
fi

mvn -Plauncher clean package
if [ $? != 0 ]; then
exit 1
fi

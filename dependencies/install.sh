#!/bin/sh

mvn install:install-file -DgroupId=ru.aplix -DartifactId=mera-driver -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true -Dfile=ru.aplix.mera.driver.jar

mvn install:install-file -DgroupId=ru.aplix -DartifactId=purejavacomm -Dversion=0.17-fixed -Dpackaging=jar -DgeneratePom=true -Dfile=ru.aplix.mera.pjcomm.jar

mvn install:install-file -DgroupId=org.rxtx -DartifactId=rxtxcomm -Dversion=2.2pre2 -Dpackaging=jar -DgeneratePom=true -Dfile=RXTXcomm.jar

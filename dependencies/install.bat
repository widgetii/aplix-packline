CALL mvn install:install-file -DgroupId=ru.aplix -DartifactId=mera-driver -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true -Dfile=ru.aplix.mera.driver.jar

CALL mvn install:install-file -DgroupId=ru.aplix -DartifactId=purejavacomm -Dversion=0.17-fixed -Dpackaging=jar -DgeneratePom=true -Dfile=ru.aplix.mera.pjcomm.jar

CALL mvn install:install-file -DgroupId=org.rxtx -DartifactId=rxtxcomm -Dversion=2.2pre2 -Dpackaging=jar -DgeneratePom=true -Dfile=RXTXcomm.jar

CALL mvn install:install-file -DgroupId=sk.gnome -DartifactId=morena -Dversion=6.4 -Dpackaging=jar -DgeneratePom=true -Dfile=morena6.jar

CALL mvn install:install-file -DgroupId=eu.gnome -DartifactId=morena -Dversion=7.1 -Dpackaging=jar -DgeneratePom=true -Dfile=morena7.jar

ECHO.
PAUSE
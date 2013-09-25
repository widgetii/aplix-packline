CALL mvn install:install-file -DgroupId=com.microsoft.sqlserver -DartifactId=sqljdbc4 -Dversion=3.0 -Dpackaging=jar -DgeneratePom=true -Dfile=sqljdbc4.jar

CALL mvn install:install-file -DgroupId=ru.aplix -DartifactId=mera-driver -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true -Dfile=ru.aplix.mera.driver.jar

CALL mvn install:install-file -DgroupId=org.rxtx -DartifactId=rxtxcomm -Dversion=2.2pre2 -Dpackaging=jar -DgeneratePom=true -Dfile=RXTXcomm.jar

ECHO.
PAUSE
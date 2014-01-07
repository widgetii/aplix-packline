========================================================================

							PackLine

========================================================================

'PackLine' is a desktop application, written in JavaFX, that runs on workstation 
of marking and package of mailings.


BUILDING THE APPLICATION
------------------------

1. Requirements

In order to build application you will need:
	- Maven 3.0.x or later
	- JDK 1.6 or later

Also optional additional software would make it easy for you to work 
with the sources of the application:

    - Spring Tool Suite IDE (to explore and run the application 
      in IDE). You're obviously free to use any other IDE you wish. 

2. Install Maven dependencies

Run /dependencies/install.* script. Maven will install several dependencies
in its local repository.

3. Build

This Maven project is aggregate project which is intended for compiling all
nested sub-projects. To build all projects you need to navigate to the root 
folder of aggregate project and run:

	mvn clean install
	
Maven will read project specification in pom.xml file, will download all 
dependencies, will compile all projects and installs them in local repository. 

4. Deploy

The complete and final distribution is placed in /app/target/dist/ folder as 
.zip archive.


RUNNING THE APPLICATION
-----------------------

This application requires Java runtime with JavaFX support. In order to run 
the app, navigate to /packline-vX.X/bin/ folder and run packline.bat or 
packline.sh script.

On Windows the app is running by default with opened console window. In order 
to hide console, launch run.bat instead of packline.bat.


RELEASE NOTES
-------------

Version 1.0


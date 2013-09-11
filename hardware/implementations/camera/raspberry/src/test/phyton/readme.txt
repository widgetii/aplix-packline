SCRIPT INSTALL UNDER IIS
------------------------
1. Copy this script to the path of default IIS site.
OR
1.1. Create a directory to hold this python script, 
for example c:\dev\python, then copy this script.
1.2. Set the permissions on the files in the directory 
c:\dev\python to allow IIS to read and execute (for 
IUSR and IIS_IUSRS users).
1.3. Create a new application, specify the virtual path
as /py and the physical path as c:\dev\python.

2. Within that IIS application or default IIS site, in 
"Handler Mappings" section add a script map for *.py, 
and map it to "c:\python27\python.exe %s %s" path.

3. Invoke from browser: 
http://localhost/py/take.py?1234567890, remove py/ if
you used the path of default IIS web site.

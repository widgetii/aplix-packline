SCRIPT INSTALL UNDER IIS
------------------------
1. Copy these scripts to the path \flussonic\api of default 
IIS site.

2. Within default IIS site, in "Handler Mappings" section 
add a script map for "*.py", and map it to 
"c:\python27\python.exe %s %s" path.

3. Within default IIS site, in "Handler Mappings" section 
add a script map for "flussonic/api/dvr_enable", 
and map it to "c:\python27\python.exe %s.py %s" path.

4. Within default IIS site, in "Handler Mappings" section 
add a script map for "flussonic/api/dvr_disable", 
and map it to "c:\python27\python.exe %s.py %s" path.

5. Within default IIS site, in "Handler Mappings" section 
add a script map for "flussonic/api/stream_health", 
and map it to "c:\python27\python.exe %s.py %s" path.

6. Invoke from browser: 
http://localhost/flussonic/api/dvr_enable/STREAM_NAME
or
http://localhost/flussonic/api/dvr_disable/STREAM_NAME
or
http://localhost/flussonic/api/stream_health/STREAM_NAME

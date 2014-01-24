import os, sys, logging, random, string

logging.basicConfig(filename='d:\camera.log', level=logging.DEBUG, format='%(asctime)s - %(levelname)s - %(message)s')

print 'Content-Type: text/plain'
print ''

scriptName = os.environ['SCRIPT_NAME'] + '/'
pathInfo = os.environ['PATH_INFO']	
if string.find(pathInfo, scriptName) == 0:
	pathInfo = pathInfo[len(scriptName):]
	
rNum = random.randrange(0,1+1)
if rNum == 0:
	imageGUID = pathInfo
	logMsg = 'Image GUID: ' + imageGUID
	logging.debug(logMsg);

	print 'OK'
else:
	print 'BUSY'
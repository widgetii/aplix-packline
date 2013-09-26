import sys, logging, random

logging.basicConfig(filename='d:\camera.log', level=logging.DEBUG, format='%(asctime)s - %(levelname)s - %(message)s')

print 'Content-Type: text/plain'
print ''

if len(sys.argv) < 2:
	print 'ERROR: No guid has been specified'
	sys.exit(0)
	
rNum = random.randrange(0,1+1)
if rNum == 0:
	imageGUID = sys.argv[1]
	logMsg = 'Image GUID: ' + imageGUID
	logging.debug(logMsg);

	print 'OK'
else:
	print 'BUSY'
import sys, logging

logging.basicConfig(filename='d:\camera.log', level=logging.DEBUG, format='%(asctime)s - %(levelname)s - %(message)s')

print 'Content-Type: text/plain'
print ''

print 'Camera controller'
print '-----------------'
print ''

if len(sys.argv) < 2:
	print 'No guid has been specified.'
	sys.exit(0)
	
imageGUID = sys.argv[1]
logMsg = 'Image GUID: ' + imageGUID
print logMsg
logging.debug(logMsg);


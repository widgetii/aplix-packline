import serial, random, time

portname = 'COM42'
ser = serial.Serial(portname , 115200)
print 'Mera scales on ' + portname

try:
	while True:
		weight = random.uniform(0.1, 2.0)
		weightproduct = weight * 20
		text = '+{:07.3f}'.format(weightproduct) + '#+{:07.3f}S\r'.format(weight)
		print '{:.3f} kg'.format(weight)

		for x in range(0, 50):
			ser.write(text)
	
		time.sleep(5)

finally:
	ser.close()
	print 'Port ' + portname + ' closed'
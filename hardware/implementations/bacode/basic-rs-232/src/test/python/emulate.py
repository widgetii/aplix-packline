import serial

ser = serial.Serial('COM40', 57600)
ser.write("4627085462743\n")
ser.close()
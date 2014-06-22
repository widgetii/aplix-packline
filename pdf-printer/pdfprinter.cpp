// pdfprinter.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"

/*
*	Constants/Declarations
*/
LPCTSTR REG_INSTALL_KEY = TEXT("SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths\\AcroRd32.exe");
LPCTSTR DDE_CMD_PRINT = TEXT("[FilePrintSilent(\"%s\")]");
LPCTSTR DDE_CMD_EXIT= TEXT("[AppExit()]");
LPCTSTR ACRO_DDESERVER = TEXT("acroview");
LPCTSTR ACRO_DDETOPIC = TEXT("control");
LPCTSTR ACRO_DRIVER_NAME_REG_EXP = TEXT("Adobe\\s+PDF");

const DWORD CONNECT_TIMEOUT_SHORT = 1000;
const DWORD CONNECT_TIMEOUT_LONG = 5000;
const DWORD PRINT_JOB_TIMEOUT = 30000;

/*
*	Functions/Declarations
*/
HDDEDATA CALLBACK DDE_ProcessMessage (UINT uType, UINT uFmt, HCONV hconv, HSZ hsz1, HSZ hsz2,
									  HDDEDATA hdata, DWORD dwData1, DWORD dwData2);
DWORD runAcrobatServer();
HCONV connectToDDEServer(DWORD id, bool startServer, DWORD timeout);
BOOL getPrinterName(LPTSTR *printerName);
BOOL canCloseAcrobat(LPTSTR printerName);
void waitForPrintJobComplete(HANDLE hChange, PRINTER_NOTIFY_OPTIONS pnOptions, LPCTSTR documentName);

/*
*	Main entrypoint for application.
*/
int _tmain(int argc, _TCHAR* argv[])
{	
	printf("PDF Printer, version: %d.%d, (c) APLIX LLC, 2014\n", 1, 0);
	if (argc == 1)
	{
		printf("Usage: pdfprinter.exe [OPTIONS] <file1> <file2> <file3> ...\n");
		printf("Options:\n");	
		printf("	-p - printer name\n");
		return 0;
	}
	printf("\n");

	// Start the DDE work now that the viewer is launched.	
	DWORD id = 0;
	if (DdeInitialize (&id, &DDE_ProcessMessage, APPCMD_CLIENTONLY, 0) != DMLERR_NO_ERROR) {
		printf("ERROR: can't initialize Dynamic Data Exchange Management Library (DDEML).\n");
		return -1;
	}

	TCHAR ddeCmdBuf[MAX_PATH + 1];	
	memset(ddeCmdBuf, 0, sizeof(ddeCmdBuf));

	__try {
		// Initialize DDE conversation with server.
		// If server is not running - it's fine, just go further
		HCONV hConversation = connectToDDEServer(id, false, CONNECT_TIMEOUT_SHORT);
		if (hConversation != NULL) 
		{
			// Stop running Acrobat, because we need to change printer
			_tcscpy_s(ddeCmdBuf, DDE_CMD_EXIT);
			HDDEDATA transaction = DdeClientTransaction((LPBYTE)ddeCmdBuf, (DWORD)_tcslen(ddeCmdBuf), hConversation,
									0L, CF_TEXT, XTYP_EXECUTE, INFINITE-1, NULL);

			// Close DDE connection, because server is gone
			DdeDisconnect(hConversation);
		}

		// Get default printer name
		LPTSTR defaultPrinterName = NULL;
		getPrinterName(&defaultPrinterName);
		BOOL printerChanged = FALSE;
		LPTSTR prinerName = defaultPrinterName;

		// Set default printer
		for (int i = 1; i < argc; i++) {
			if (_tcscmp(TEXT("-p"), argv[i]) == 0)
			{
				i++;
				if (i < argc)
				{		
					if (_tcscmp(defaultPrinterName, argv[i]) != 0) 
					{
						printerChanged = SetDefaultPrinter(argv[i]);
						if (printerChanged) 
						{
							prinerName = argv[i];
						}
					}
				}
			}		
		}

		// Initialize DDE conversation with server again,
		// starting the server if necessary
		hConversation = connectToDDEServer(id, true, CONNECT_TIMEOUT_LONG);
		if (hConversation == NULL) 
		{
			printf("ERROR: can't connect to Acrobat.\n");
			return -1;
		}

		/*
		// Open printer
		HANDLE hPrinter;
		if (!OpenPrinter(printerName, &hPrinter, NULL)) {
			return -1;
		}

		// Initialize print notification structures
		WORD fields[2];
		fields[0] = JOB_NOTIFY_FIELD_DOCUMENT;
		fields[1] = JOB_NOTIFY_FIELD_STATUS;

		PRINTER_NOTIFY_OPTIONS_TYPE pnoTypes[1];
		pnoTypes[0].Type = JOB_NOTIFY_TYPE;
		pnoTypes[0].Count = 2;
		pnoTypes[0].pFields = &fields[0];

		PRINTER_NOTIFY_OPTIONS pnOptions;
		pnOptions.Version = 2;
		pnOptions.Flags = 0;
		pnOptions.Count = 1;
		pnOptions.pTypes = &pnoTypes[0];

		// Register print change notification
		HANDLE chgObject = FindFirstPrinterChangeNotification(hPrinter, 0, 0, &pnOptions);*/
		
		// Execute the DDE Command		
		for (int i = 1; i < argc; i++) {
			if (_tcscmp(TEXT("-p"), argv[i]) == 0)
			{
				i++;
				continue;
			}

			// Print file			
			printf("Printing file \"%s\"...", argv[i]);	
			_sntprintf_s(ddeCmdBuf, MAX_PATH, DDE_CMD_PRINT, argv[i]);
			HDDEDATA transaction = DdeClientTransaction((LPBYTE)ddeCmdBuf, (DWORD)_tcslen(ddeCmdBuf), hConversation,
									0L, CF_TEXT, XTYP_EXECUTE, INFINITE-1, NULL);
			/*UINT error = DdeGetLastError(id);
			if (error != DMLERR_NO_ERROR) 
			{
				continue;
			}*/		

			// Extract file name 
			/*TCHAR documentName[_MAX_FNAME];
			TCHAR fileName[_MAX_FNAME];
			TCHAR fileExt[_MAX_EXT];
			_tsplitpath_s(argv[i], NULL, 0, NULL, 0, &fileName[0], _MAX_FNAME, &fileExt[0], _MAX_EXT);
			_sntprintf_s(documentName, _MAX_FNAME, TEXT("%s%s"), fileName, fileExt);
						
			// Wait for print job
			waitForPrintJobComplete(chgObject, pnOptions, documentName);*/

			DdeFreeDataHandle(transaction);
			printf("OK\n");	
		}
				
		// Unregister print change notification
		//FindClosePrinterChangeNotification(chgObject);

		// CLose printer descriptor
		//ClosePrinter(hPrinter);

		// Stop running Acrobat
		if (canCloseAcrobat(prinerName))
		{
			_tcscpy_s(ddeCmdBuf, DDE_CMD_EXIT);
			HDDEDATA transaction = DdeClientTransaction((LPBYTE)ddeCmdBuf, (DWORD)_tcslen(ddeCmdBuf), hConversation,
								0L, CF_TEXT, XTYP_EXECUTE, INFINITE-1, NULL);
		}

		// Close DDE connection
		DdeDisconnect(hConversation);

		// Restore default printer
		if (printerChanged == TRUE) 
		{
			SetDefaultPrinter(defaultPrinterName);
		}

		// Free allocated memory
		if (defaultPrinterName != NULL)
		{
			free(defaultPrinterName);
		}
	}
	__finally {
		// Release resources.
		DdeUninitialize(id);
	}

	return 0;
}

HDDEDATA CALLBACK DDE_ProcessMessage (UINT uType, UINT uFmt, HCONV hconv, HSZ hsz1, HSZ hsz2,
									  HDDEDATA hdata, DWORD dwData1, DWORD dwData2)
{
	return NULL;
}

DWORD runAcrobatServer() 
{
	long lRetCode;
	HKEY hkey;
	TCHAR pathBuf[MAX_PATH + 1];
	DWORD size = MAX_PATH + 1;

	// Determine if a PDF viewer is installed.
	lRetCode = RegOpenKeyEx(HKEY_LOCAL_MACHINE, REG_INSTALL_KEY, 0, KEY_READ, &hkey);
	if (lRetCode != ERROR_SUCCESS) {		
		return lRetCode;
	}

	// Get the path to the viewer executable and launch it.
	lRetCode = RegQueryValueEx(hkey, TEXT(""), 0, 0, (LPBYTE) pathBuf, &size);
	RegCloseKey (hkey);

	// Launch the viewer in hidden mode.		
	HINSTANCE hinst = ShellExecute(NULL, TEXT("open"), (LPCTSTR)pathBuf, NULL, NULL, SW_HIDE);
	if ((int)hinst > 32) {
		return ERROR_SUCCESS;
	}
	else {
		return (DWORD)hinst;
	}
}

HCONV connectToDDEServer(DWORD id, bool startServer, DWORD timeout) 
{
	HCONV hConversation = NULL;
	bool acrobatStart = false;

	// Initialize DDE conversation with server.
	HSZ hszServerName = DdeCreateStringHandle(id, ACRO_DDESERVER, 0);
	HSZ hszTopicName = DdeCreateStringHandle(id, ACRO_DDETOPIC, 0);
	__try {
		// Acrobat can take a while to launch. We repeatedly attempt
		// to connect to the server until timeout expires.
		DWORD elapsed = 0;
		DWORD started = GetTickCount();
		do {		
			hConversation = DdeConnect(id, hszServerName, hszTopicName, NULL);
			if (hConversation == NULL)
			{
				// Start Acrobat server
				if (startServer && !acrobatStart) 
				{
					acrobatStart = true;
					runAcrobatServer();
				}
	
				elapsed = GetTickCount() - started;
				if (elapsed < timeout)
				{
					Sleep(200);
				}
			}
		} while ((hConversation == NULL) && (elapsed < timeout));
	}
	__finally {
		// Release resources.
		DdeFreeStringHandle(id, hszServerName);
		DdeFreeStringHandle(id, hszTopicName);
	}

	return hConversation;
}

BOOL getPrinterName(LPTSTR *printerName) 
{
	DWORD bufferLength;
	if (!GetDefaultPrinter(NULL, &bufferLength))
	{
		*printerName = (LPTSTR)malloc(bufferLength);

		return GetDefaultPrinter(*printerName, &bufferLength);
	}

	return FALSE;
}

BOOL canCloseAcrobat(LPTSTR printerName)
{
	BOOL result = TRUE;
	HANDLE hPrinter;
	if (OpenPrinter(printerName, &hPrinter, NULL))
	{
		DWORD pcbNeeded;
		if (!GetPrinter(hPrinter, 2, NULL, 0, &pcbNeeded))
		{
			DWORD cbBuf = pcbNeeded;
			LPPRINTER_INFO_2 pPrinterInfo = (LPPRINTER_INFO_2)malloc(pcbNeeded);			
			if (GetPrinter(hPrinter, 2, (LPBYTE) pPrinterInfo, cbBuf, &pcbNeeded))
			{
				std::string target(printerName);
				std::regex rx(ACRO_DRIVER_NAME_REG_EXP);
				if (std::regex_match(target.cbegin(), target.cend(), rx)) 
				{
					// Prevent closing acrobat if we are printing on virtual PDF printer
					// because it will likely open the printed PDF file 
					result = FALSE;
				}
			}
			free(pPrinterInfo);
		}

		ClosePrinter(hPrinter);
	}

	return result;
}

void waitForPrintJobComplete(HANDLE hChange, PRINTER_NOTIFY_OPTIONS pnOptions, LPCTSTR documentName)
{
	if (hChange == INVALID_HANDLE_VALUE) {
		return;
	}

	// Repeat waiting loop until job with a given name changes
	bool wait = true;
	DWORD elapsed = 0;
	DWORD started = GetTickCount();
	while (wait && elapsed < PRINT_JOB_TIMEOUT) {		
		// Wait for the change notification		
		if (WaitForSingleObject(hChange, PRINT_JOB_TIMEOUT - elapsed) != WAIT_TIMEOUT) {
			// Retreive change notification details
			DWORD dwChange;
			PRINTER_NOTIFY_INFO* ppInfo;
			BOOL fcnreturn = FindNextPrinterChangeNotification(hChange, &dwChange, &pnOptions, (LPVOID *)&ppInfo);
			if (fcnreturn) {
				// Look for job with our document name
				bool foundName = false, foundPrinted = false;
				for (DWORD i = 0; i < ppInfo->Count; i++) {
					PRINTER_NOTIFY_INFO_DATA pniData = ppInfo->aData[i];
									
					switch (pniData.Field) {
					case JOB_NOTIFY_FIELD_DOCUMENT:
						if (_tcscmp((LPTSTR) pniData.NotifyData.Data.pBuf, documentName) == 0) {
							foundName = true;
						}
						break;	
					case JOB_NOTIFY_FIELD_STATUS:
						foundPrinted = (pniData.NotifyData.adwData[0] & JOB_STATUS_PRINTED) == JOB_STATUS_PRINTED;						
						break;
					}
				}

				if (foundName && foundPrinted) 
				{
					wait = false;
				}				
			}
			else 
			{
				wait = false;
			}

			// Free system allocated structure
			FreePrinterNotifyInfo(ppInfo);
		}
		else
		{
			wait = false;
		}

		elapsed = GetTickCount() - started;
	}
}
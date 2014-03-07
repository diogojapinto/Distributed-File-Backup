package com.sdis.sharedbackup.backend;

import java.util.ArrayList;
import java.util.Map;

public class ConfigManager {
	// TODO: save files and configuration data in appropriated data structures
	String backupFolder;
	int maxBackupSize;
	
	// map saving the filepath on this machine and respective SharedFile object
	Map<String, SharedFile> myFiles;

	// map saving the fileId of a shared file and respective SharedFile object
	Map<String, SharedFile> sharedFile;
}
package com.sdis.sharedbackup.backend;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public class ConfigManager implements Serializable {

	// constants
	private static final String VERSION = "1.0";

	// static members
	private static ConfigManager sInstance = null;

	// private members
	private boolean mCheckState;
	private InetAddress mMCaddr = null, mMDBaddr = null, mMDRaddr = null;
	private int mMCport = 0, mMDBport = 0, mMDRport = 0;
	// TODO: save files and configuration data in appropriated data structures
	private String mBackupFolder;
	private int maxBackupSize;

	private ConfigManager() {
	}

	public static ConfigManager getInstance() {
		if (sInstance == null) {
			sInstance = new ConfigManager();
		}
		return sInstance;
	}

	// map saving the filepath on this machine and respective SharedFile object
	Map<String, SharedFile> myFiles;

	// map saving the fileId of a shared file and respective SharedFile object
	Map<String, SharedFile> sharedFile;

	public String getVersion() {
		return VERSION;
	}

	public boolean isToCheckState() {
		return mCheckState;
	}

	public boolean setMulticastAddrs(String mcAddr, int mcPort, String mdbAddr, int mdbPort, String mdrAddr, int mdrPort) {
		try {
			mMCaddr = InetAddress.getByName(mcAddr);
			mMDBaddr = InetAddress.getByName(mdbAddr);
			mMDRaddr = InetAddress.getByName(mdrAddr);
			
			mMCport = mcPort;
			mMDBport = mdbPort;
			mMDRport = mdrPort;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public InetAddress getMCAddr() {
		return mMCaddr;
	}
	
	public int getMCPort() {
		return mMCport;
	}
	
	public InetAddress getMDBAddr() {
		return mMDBaddr;
	}
	
	public int getMDBPort() {
		return mMDBport;
	}
	
	public InetAddress getMDRAddr() {
		return mMDRaddr;
	}
	
	public int getMDRPort() {
		return mMDRport;
	}

}
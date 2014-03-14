package com.sdis.sharedbackup.utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Encoder {
	
	// generate the SHA256 hash key for some desired file. use filename, datemodified, owner, filedata
	public static String generateBitString(File f) {
	//TODO: Testar se funca
		MessageDigest md= null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Selected algorithm does not exit");
			e.printStackTrace();
		}
		// get string
		String file_string = f.getName();
		file_string = file_string + f.lastModified();
		try {
			md.update(file_string.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			System.out.println("Unsuported Encoding");
			e.printStackTrace();
		}
		byte[] digest =md.digest();
		String key = digest.toString();
		
		
		return key;	}
}

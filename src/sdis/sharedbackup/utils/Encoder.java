package sdis.sharedbackup.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Encoder {

	private static final int LAST_FILE_BYTES_SIZE = 8;
	private static final String ASCII_CODE = "US-ASCII";

	// generate the SHA256 hash key for some desired file. use filename,
	// datemodified, owner, filedata
	public static String generateBitString(File f) {
		// TODO: Testar se funca
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Selected algorithm does not exit");
			e.printStackTrace();
		}
		// get string
		String file_string = f.getName();
		file_string += f.lastModified();

		FileInputStream in = null;
		try {
			in = new FileInputStream(f);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		byte[] lastFileBytes = new byte[LAST_FILE_BYTES_SIZE];
		try {
			in.read(lastFileBytes, (int) f.length() - LAST_FILE_BYTES_SIZE - 1,
					LAST_FILE_BYTES_SIZE);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try {
			file_string += new String(lastFileBytes, ASCII_CODE);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

		try {
			md.update(file_string.getBytes(ASCII_CODE));
		} catch (UnsupportedEncodingException e) {
			System.out.println("Unsuported Encoding");
			e.printStackTrace();
		}
		byte[] digest = md.digest();
		String key = digest.toString();

		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return key;
	}
}

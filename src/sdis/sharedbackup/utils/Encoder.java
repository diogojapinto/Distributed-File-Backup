package sdis.sharedbackup.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Encoder {

	private static final int LAST_FILE_BYTES_SIZE = 8;
	//private static final String ASCII_CODE = "US-ASCII";

	// generate the SHA256 hash key for some desired file. use filename,
	// datemodified, owner, filedata
	public static String generateBitString(File f) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Selected algorithm does not exit");
			e.printStackTrace();
			System.exit(1);
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

		int skipBytes = Math.min(LAST_FILE_BYTES_SIZE, (int) f.length());

		byte[] lastFileBytes = new byte[skipBytes];

		try {
			in.skip(f.length() - skipBytes);
			in.read(lastFileBytes, 0, skipBytes);
		} catch (IndexOutOfBoundsException e2) {
			e2.printStackTrace();
			System.exit(1);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(1);
		}

		file_string += new String(lastFileBytes);

		md.update(file_string.getBytes());
		/*try {
			md.update(file_string.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}*/

		byte[] digest = md.digest();
		
		//convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
          sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
        }

		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
}

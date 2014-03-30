package sdis.sharedbackup.utils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import sdis.sharedbackup.backend.MulticastComunicator;

public class Splitter {

	private Splitter() {

	}

	public static SplittedMessage split(byte[] messg) {

		byte[] headerEnd = null;

		try {
			headerEnd = new String(MulticastComunicator.CRLF
					+ MulticastComunicator.CRLF)
					.getBytes(MulticastComunicator.ASCII_CODE);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		int headerEndIndex = findIndexOfSubByteArray(messg, headerEnd);

		SplittedMessage splittedMessage = new SplittedMessage();

		splittedMessage.setHeader(new String(Arrays.copyOfRange(messg, 0,
				headerEndIndex)));
		splittedMessage.setBody(Arrays.copyOfRange(messg, headerEndIndex + 4,
				messg.length));

		return splittedMessage;
	}

	private static int findIndexOfSubByteArray(byte[] original, byte[] separator) {
		for (int i = 0; i < original.length; i++) {
			for (int j = 0; j < separator.length; j++) {
				if (separator[j] != original[i + j]) {
					break;
				} else {
					if (j == separator.length - 1) {
						return i;
					}
				}
			}
		}

		// did not find intex
		return -1;
	}
}

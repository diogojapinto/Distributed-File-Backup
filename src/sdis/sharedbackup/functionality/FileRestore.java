package sdis.sharedbackup.functionality;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.SharedFile;

public class FileRestore {
	private static FileRestore mInstance = null;
	
	private Map<String, ArrayList<FileChunk>> receivedChunks;

	private FileRestore() {
	}

	public static FileRestore getInstance() {
		return (mInstance == null) ? mInstance = new FileRestore() : mInstance;
	}

	private boolean receivedAllChunks(SharedFile file) {
		if (receivedChunks.size() == file.getChunkList().size()) {
			return true;
		} else {
			return false;
		}
	}

	private boolean rebuildFile(String filePath, ArrayList<FileChunk> chunks,
			SharedFile shared) {
		byte[] body = new byte[(int) shared.getFileSize()];
		int i;
		for (i = 0; i < chunks.size(); i++) {
			// add chunkBody to body
		}
		try {
			writeFile(body, filePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	private boolean writeFile(byte[] data, String filePath) throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(filePath);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fos.write(data);
		fos.close();
		return true;
	}
}

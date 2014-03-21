package sdis.sharedbackup.protocols;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.SharedFile;

public class ChunkRestore {

	// melhor ordenar isto pelo chunkno sempre que se recebe um novo chunk?
	private ArrayList<FileChunk> receivedChunks;

	// is the name ok?
	private boolean receivedAllChunks(SharedFile file) {
		// usar o list.size() ou o chunkCounter?
		if (receivedChunks.size() == file.getChunkList().size()) {
			return true;
		} else {
			return false;
		}
	}

	private boolean rebuildFile(String filePath, ArrayList<FileChunk> chunks, SharedFile shared) {
		byte [] body = new byte [(int) shared.getFileSize()];
		int i;
		for (i = 0; i < chunks.size(); i++) {
			//add chunkBody to body	
		}
		try {
			writeFile(body, filePath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	
	private boolean writeFile (byte [] data, String filePath) throws IOException {
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



package sdis.sharedbackup.functionality;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.FileChunkWithData;
import sdis.sharedbackup.backend.SharedFile;
import sdis.sharedbackup.protocols.ChunkRestore;

public class FileRestore {
	private static FileRestore mInstance = null;

	private Map<String, ArrayList<FileChunk>> receivedChunks;

	private FileRestore() {
	}

	public static FileRestore getInstance() {
		return (mInstance == null) ? mInstance = new FileRestore() : mInstance;
	}

	public boolean restoreFile(SharedFile file) {
		// TODO: set file's path ???
		ArrayList<FileChunkWithData> receivedChunks = new ArrayList<FileChunkWithData>();

		for (FileChunk chunk : file.getChunkList()) {
			receivedChunks.add(ChunkRestore.getInstance().requestChunk(
					chunk.getFileId(), chunk.getChunkNo()));
		}

		rebuildFile(file, receivedChunks);

		return true;
	}

	private boolean rebuildFile(SharedFile file,
			ArrayList<FileChunkWithData> chunks) {

		for (FileChunkWithData chunk : chunks) {
			if (!appendToFile(chunk.getData(), file.getFilePath())) {
				return false;
			}
		}

		return true;
	}

	private boolean appendToFile(byte[] data, String filePath) {

		FileOutputStream os = null;
		try {
			os = new FileOutputStream(new File(filePath), true);

			os.write(data);
			os.close();
			return true;

		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}
}

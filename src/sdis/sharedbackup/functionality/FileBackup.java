package sdis.sharedbackup.functionality;

import java.util.ArrayList;

import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.SharedFile;
import sdis.sharedbackup.protocols.ChunkBackup;

public class FileBackup {
	private static FileBackup mInstance = null;

	private FileBackup() {
	}

	public static FileBackup getInstance() {
		return (mInstance == null) ? mInstance = new FileBackup() : mInstance;
	}

	// call putChunk for each chunk in SharedFile
	public boolean saveFile(SharedFile file) {
		ArrayList<FileChunk> list = file.getChunkList();
		int i;
		for (i = 0; i < list.size(); i++) {
			final FileChunk chunk = list.get(i);
			new Thread(new Runnable() {

				@Override
				public void run() {
					ChunkBackup.getInstance().putChunk(chunk);
				}

			}).start();
		}
		return true;
	}
}
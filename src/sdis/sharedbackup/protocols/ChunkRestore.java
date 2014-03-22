package sdis.sharedbackup.protocols;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.SharedFile;

public class ChunkRestore {

	private static ChunkRestore sInstance = null;

	public static ChunkRestore getInstance() {

		if (sInstance == null) {
			sInstance = new ChunkRestore();
		}
		return sInstance;
	}

	private ChunkRestore() {
	}
	
	public boolean sendChunk(FileChunk chunk) {
		
	}
}

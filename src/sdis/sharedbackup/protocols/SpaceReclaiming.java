package sdis.sharedbackup.protocols;

import java.util.ArrayList;

import sdis.sharedbackup.backend.FileChunkWithData;

public class SpaceReclaiming {

	private static SpaceReclaiming sInstance = null;
	
	public static SpaceReclaiming getInstance() {

		if (sInstance == null) {
			sInstance = new SpaceReclaiming();
		}
		return sInstance;
	}

	private SpaceReclaiming() {
	}
	
	public boolean reclaimSpace(FileChunk chunk) {
		
	}
	
}

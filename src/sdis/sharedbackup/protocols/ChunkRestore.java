package sdis.sharedbackup.protocols;

import java.util.ArrayList;

import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.SharedFile;

public class ChunkRestore {

	//melhor ordenar isto pelo chunkno sempre que se recebe um novo chunk?
	private ArrayList <FileChunk> receivedChunks;
	
	// is the name ok?
	private boolean receivedAllChunks (SharedFile file){
		
		if( receivedChunks.size() == file.getChunkList().size()){
			return true;
		}else		
		return false;
	}
	
	
}

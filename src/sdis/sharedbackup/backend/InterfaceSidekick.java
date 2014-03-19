package sdis.sharedbackup.backend;

import java.io.File;



public class InterfaceSidekick {
	
	private InterfaceSidekick () {
		
	}
	// in for loop create chunk, add it to
	// SharedFile, and call putChunk()
	
	public static boolean isValidFile (String filePath){
		
		File validFile = new File(filePath);
		return validFile.exists();
	}
	
	
	

}

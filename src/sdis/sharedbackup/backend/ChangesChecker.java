package sdis.sharedbackup.backend;

public class ChangesChecker implements Runnable {
	
	private static final int THIRTY_SECONDS = 30000;

	@Override
	public void run() {
		while(ConfigsManager.getInstance().isToCheckState()) {
			checkDeletions();
			checkDiskSpace();
			
			try {
				Thread.sleep(THIRTY_SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void checkDeletions() {
		// TODO
	}
	
	public void checkDiskSpace() {
		// TODO
	}
}

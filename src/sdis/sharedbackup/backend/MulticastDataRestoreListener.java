package sdis.sharedbackup.backend;

/*
 * Class that receives and dispatches messages from the multicast data restore channel
 */
public class MulticastDataRestoreListener implements Runnable {

	private static MulticastDataRestoreListener mInstance = null;

	private MulticastDataRestoreListener() {
	}

	public static MulticastDataRestoreListener getInstance() {
		if (mInstance == null) {
			mInstance = new MulticastDataRestoreListener();
		}
		return mInstance;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

}

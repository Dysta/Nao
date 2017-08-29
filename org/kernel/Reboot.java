package org.kernel;

import org.client.Characters;
import org.common.SQLManager;
import org.common.SocketManager;
import org.common.World;

public class Reboot {

	public static void start(){
		if (!Config.isSaving)
			World.saveAll(null);
		
		SQLManager.LOAD_ACTION();

		Main.isRunning = false;
		
		for(Characters p : World.getOnlinePersos())
			SocketManager.REALM_SEND_MESSAGE(p.get_compte().getGameThread().get_out(), "4|");
		
		try {
			Main.loginServer.stop();
		} catch (Exception e) {
			Logs.addToRealmLog("Error stopping login server during reboot");
		}
		
		try {
			Main.gameServer.kickAll();
		} catch (Exception e) {
			Logs.addToGameLog("Error in the kick process during reboot");
		}
		Main.gameServer.stop();


		Main.loginServer = null;
		Main.gameServer = null;

		try {
			World.clearAllVar();
		} catch (Exception e) {
			Logs.addToDebug("Error in variable cleaning process during reboot");
		}
		try {
			Main.listThreads(true);
		} catch (Exception e) {
			Logs.addToDebug("Error in the listing thread process during reboot");
		}

		SQLManager.closeCons();
		System.gc();

		Main.main(null);
	}
}

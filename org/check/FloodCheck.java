package org.check;

import org.client.Accounts;
import org.client.Characters;
import org.common.SocketManager;

public class FloodCheck {

	public static void updateFloodInfos(Characters perso, String msg) {
		if (perso.getLastMessSent().size() > 50)
			perso.getLastMessSent().clear();

		perso.setLastMess(System.currentTimeMillis());
		perso.getLastMessSent().put(msg, System.currentTimeMillis());
		perso.setContent(msg);
	}

	public static boolean isBypass(Characters perso, String mess) {
		String last = perso.getContent();
		try {
			if ((mess.length() > 1 && mess.substring(0, mess.length() - 2).equals(last.substring(0, mess.length() - 2)))
					|| (mess.length() > 2 && mess.substring(0, mess.length() - 3).equals(last.substring(0, mess.length() - 3)))
					|| (mess.length() > 3 && mess.substring(0, mess.length() - 4).equals(last.substring(0, mess.length() - 4)))) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean isFlooding(Characters perso, String mess) {
		if (perso.get_compte().get_gmLvl() > 0)
			return false;

		long actualTime = System.currentTimeMillis();
		Accounts acc = perso.get_compte();

		// Si il ne parle pas pendant 5 minutes on démute
		if (actualTime - perso.getLastMess() > 60000 * 5) {
			if (acc.isAFlooder() != false)
				acc.setAFlooder(false);
			acc.setFloodGrade(0);
			updateFloodInfos(perso, mess);
			acc.unMute();
			return false;
		}
		if (actualTime - perso.getLastMess() > 1000 * 3){
			if(acc.getFloodGrade() > 0)
				acc.setFloodGrade(acc.getFloodGrade() - 1);
			updateFloodInfos(perso, mess);
			return false;
		}
		
		// Si il a trop flood on le mute
		if (acc.getFloodGrade() >= 20 && acc.isAFlooder() != true) {
			acc.setAFlooder(true);
			acc.mute(perso.get_name(), 5);
			SocketManager.GAME_SEND_Im_PACKET(perso, "Im1124;" + Math.round(10/60));
			updateFloodInfos(perso, mess);
		}
		// On regarde si il flood
		if (actualTime - perso.getLastMess() < 1000) {
			updateFloodInfos(perso, mess);
			acc.setFloodGrade(acc.getFloodGrade() + 1);
			if (acc.getFloodGrade() > 7)
				return true;
			
			return false;
		}

		if (perso.getContent().equals(mess) || isBypass(perso, mess)) {
			updateFloodInfos(perso, mess);
			acc.setFloodGrade(acc.getFloodGrade() + 1);
			if (acc.getFloodGrade() > 7)
				return true;
			
			return false;
		}
		return false;
	}

}

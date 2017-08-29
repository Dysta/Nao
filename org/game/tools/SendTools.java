package org.game.tools;

import org.client.Characters;
import org.common.SocketManager;
import org.kernel.Config;

public class SendTools {

	public static void sendVoteInfos(Characters _perso, boolean isCheck, String contents) {
		
		String link = Config.CONFIG_LINK_VOTE;
		String content = "";
		if (!isCheck)
			content = "CONTENT";
		else
			content = contents;
		SocketManager.send(_perso, 101 + "|" + link + "|" + content);
	}
	
	public static void sendMessBox(Characters _perso, String title, String content) {
		
		String packet = "100MSGBOX" + "|" + title + "|" + content;
		SocketManager.send(_perso, packet);
	}
	
	public static void sendBigMessBox(Characters _perso, String title, String content) {
		
		String packet = "100BIGMSGBOX" + "|" + title + "|" + content;
		SocketManager.send(_perso, packet);
	}
	
	public static void sendShopStore(Characters _perso) {
		
		if (!ParseTools.isFirstLoad)
			Utils.loadShopStore();
		SocketManager.send(_perso, ParseTools.storePacket);
	}
	
	public static void sendPointsByUser(Characters _perso) {
		
		int points = Utils.loadPointsByAccount(_perso.get_compte());
		SocketManager.send(_perso, 102 + "NBPTS" + "|" + points);
	}
	
	public static void sendHonorLadder(Characters _perso) {
		
		String packet = FunctionTools.loadTop10ByType("honor");
		SocketManager.send(_perso, 103 + "HONOR" + packet);
	}
	
	public static void sendExpLadder(Characters _perso) {
		
		String packet = FunctionTools.loadTop10ByType("xp");
		SocketManager.send(_perso, 103 + "EXP" + packet);
	}
	
	public static void sendVoteLadder(Characters _perso) {
		
		String packet = FunctionTools.loadTop10ByType("vote");
		SocketManager.send(_perso, 103 + "VOTE" + packet);
	}

	public static void sendLastShops(Characters _perso) {
		
		String packet = Utils.loadShopLogsByCharacterPacket(_perso);
		SocketManager.send(_perso, 104 + "LIST" + packet);
	}

	public static void sendKolizeumInfos(Characters _perso) {
		
		String packet = FunctionTools.loadKolizeumInfos(_perso);
		String isOk = "";
		SocketManager.send(_perso, 110 + "UPDATE" + isOk + "|" + packet.split("-")[0]);
		SocketManager.send(_perso, 110 + "READY" + "|" + packet.split("-")[1]);
	}

	public static void joinTeam(Characters _perso, int id) {
		// TODO Auto-generated method stub
		
	}

	public static void exitKolizeum(Characters _perso) {
		// TODO Auto-generated method stub
		
	}

	public static void joinGroupTeam(Characters _perso) {
		// TODO Auto-generated method stub
		
	}

	public static void createKoliTeam(Characters _perso) {
		// TODO Auto-generated method stub
		
	}
	
}

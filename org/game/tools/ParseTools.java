package org.game.tools;

import java.util.HashMap;
import java.util.Map;

import org.client.Characters;
import org.common.SocketManager;
import org.utils.Colors;

public class ParseTools {

	//Variables
	public static Map<Integer, Integer> shopStore = new HashMap<Integer, Integer>();
	public static boolean isFirstLoad = false;
	//Available reloaded packets
	public static String storePacket = "102";

	public static void parsePacket(String packet, Characters _perso) {

		if (_perso == null) return;

		switch(packet.charAt(0)) {
		case '1':
			switch(packet.charAt(1)) {
			case '0':
				switch (packet.charAt(2)) 
				{
				case '1': /** Vote System **/

					if (packet.substring(3).equals("VOTE")) {
						FunctionTools.checkVote(_perso);
					}
					break;
				case '2': /** Shop System **/

					if (packet.substring(3).equals("SHOP")) {
						SendTools.sendShopStore(_perso);

					} 
					else if (packet.substring(3).equals("NBPTS")) { 
						SendTools.sendPointsByUser(_perso);

					} 
					else if (packet.substring(3).contains("BUY")) {
						try {
							String [] mySplit = packet.replace("102BUY", "").split("\\|");
							int itemTemplate = Integer.parseInt(mySplit[0].toString());
							int quantity = Integer.parseInt(mySplit[1].toString());
							
							if (quantity <= 0)
								SocketManager.GAME_SEND_MESSAGE(_perso, "La quantité désirée est incorrecte !", Colors.RED);
							FunctionTools.shopItemExecute(itemTemplate, quantity,  _perso);
								
						}
						catch(Exception e){e.printStackTrace();}
					}
					break;
				case '3': /** Ladder System: Retiré pour le moment **/

					if (packet.substring(3).equals("HONOR")) {
						SendTools.sendHonorLadder(_perso);

					} 
					else if (packet.substring(3).equals("EXP")) {
						SendTools.sendExpLadder(_perso);

					} 
					else if (packet.substring(3).equals("VOTE")) {
						SendTools.sendVoteLadder(_perso);
					}
					break;
				}
				break;
			case '1': /** Kolizeum Interface **/
				switch (packet.charAt(2)) 
				{
				case 0:
					SendTools.sendKolizeumInfos(_perso);
					break;
				case 1:
					int id = Integer.parseInt(packet.replace("111|", ""));
					SendTools.joinTeam(_perso, id);
					break;
				case 2:
					SendTools.exitKolizeum(_perso);
					break;
				case 3:
					SendTools.joinGroupTeam(_perso);
					break;
				case 4:
					SendTools.createKoliTeam(_perso);
					break;
				}
				break;
			}
			break;
		}
	}
}

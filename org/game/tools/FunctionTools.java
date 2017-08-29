package org.game.tools;

import java.util.ArrayList;
import java.util.Map;

import org.client.Accounts;
import org.client.Characters;
import org.common.SocketManager;
import org.common.World;
import org.game.GameServer;
import org.utils.Colors;

public class FunctionTools {

	public static void checkVote(Characters perso) {

		Map<String, Long> lastIpTiming = GameServer.lastIpTiming;
		long timeLastVote = 0;
		String thanks = "Merci d'avoir voter ! Passez d'agréables moments sur notre serveur";
		
		if (GameServer.lastIpTiming.containsKey(perso.get_compte().get_curIP())){
			if(System.currentTimeMillis() - (lastIpTiming.get(perso.get_compte().get_curIP()))  < 7200000) {
				SocketManager.GAME_SEND_MESSAGE(perso, thanks, Colors.RED);
				return;
			} else
	        	lastIpTiming.put(perso.get_compte().get_curIP(), (long)-1);
		}
			
		timeLastVote = Utils.loadLastVoteByAccount(timeLastVote, perso.get_compte());
		
		if (System.currentTimeMillis() - (timeLastVote*1000) > 7200000 || timeLastVote == 0){
			SendTools.sendVoteInfos(perso, true, "Attention\n\n Notre système ne détecte aucun vote\n N'oubliez pas de vous connecter avant de voter");
		} else {
			lastIpTiming.put(perso.get_compte().get_curIP(), System.currentTimeMillis());
			Utils.updateAllIps(lastIpTiming);
			SocketManager.GAME_SEND_MESSAGE(perso, thanks, Colors.RED);
		}
	}

	public static void shopItemExecute(int templateID, int quantity, Characters _perso) {
		
		int points = Utils.loadPointsByAccount(_perso.get_compte());
		int price = Utils.loadPriceByTemplateID(templateID) * quantity;
		
		if (price == 0) return;
		int diff = 0;
		
		if (points < price) {
			SocketManager.GAME_SEND_MESSAGE(_perso, "Vous n'avez pas assez de points, il vous manque "+ (price-points) + " points !", Colors.RED);
			return;
			
		} else {
			
			diff = (points - price);
			Utils.updatePointsByAccount(_perso.get_compte(), diff);
			if (templateID > 0) {
				Utils.addObjectToCharacter(_perso, templateID, quantity, true, price);
				SocketManager.GAME_SEND_MESSAGE(_perso, "Vous avez reçu l'objet <b>"+World.getObjTemplate(templateID).getName()+"</b> (x"+quantity+") au prix de <b>"+price+"</b> points et il vous reste actuellement <b>"+diff+"</b> points.", Colors.RED);
			return;
			} else {
				Utils.addPackToCharacter(_perso, templateID);
				SocketManager.GAME_SEND_MESSAGE(_perso, "Vous avez reçu le pack '<b>"+getPackDescriptionByType(templateID)+"</b>' (x"+quantity+") au prix de <b>"+price+"</b> points et il vous reste actuellement <b>"+diff+"</b> points.", Colors.RED);
			}
		}
		
	}
	
	public static String loadTop10ByType(String type) {
		
		boolean isFirst = true;
		String packet = "";
		
		if (!type.equals("vote")) 
		{
			ArrayList<Characters> characters = Utils.loadRankingByType(type);
			
			for (Characters perso: characters)
			{
				if (!isFirst) packet += "|";
				if (!type.equals("xp")) packet += perso.get_name() + ";" + perso.get_honor() + ";" + perso.get_lvl();
				else packet += perso.get_name() + ";" + perso.get_curExp() + ";" + perso.get_lvl();
				isFirst = false;
			}
		} 
		else 
		{
			ArrayList<Accounts> accounts = Utils.loadRankingVote();
			for (Accounts account: accounts){
				int votes = Utils.loadVotesByAccount(account);
				if (!isFirst) packet += "|";
				packet += account.get_pseudo() + ";" + votes;
				isFirst = false;
			}
		}
		
		return packet;
	}

	public static String getPackDescriptionByType(int type) {
		String description = "Error";
		type *= -1;
		
		switch (type){
			case 1:
				description = "Skunk pack: Monter de 1 levelUp";
				break;
			case 2:
				description = "Skunk pack: Obtenir 500 000 kamas";
				break;
			case 3:
				description = "Skunk pack: Obtenir le Grade 10";
				break;
			case 4:
				description = "Skunk pack: Devenir VIP (Voir .vip)";
				break;
			case 5:
				description = "Skunk pack: Devenir Mercenaire grade 10";
				break;
			case 6:
				description = "Skunk pack: Pack de sorts spéciaux";
				break;
			case 7:
				description = "Skunk pack: Réduction 100 Runes pa/pm";
		default:
			break;
		}
		
		return description;
	}

	public static String loadKolizeumInfos(Characters _perso) {
		// TODO Auto-generated method stub
		return null;
	}
	
}

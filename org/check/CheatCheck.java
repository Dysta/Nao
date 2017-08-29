package org.check;

import org.client.Accounts;
import org.client.Characters;
import org.common.SocketManager;

public class CheatCheck {
	
	public static boolean check(String packet, Characters character) {
		boolean exploit = false;

		Accounts account = character.get_compte();
		
		// suceptible d'être faillé
		switch (packet.substring(0, 2)) {
		case "DR": // Dialogue Réponse
			switch (packet.substring(3)) {
			case "DR677|605": // Chacha + tp
				if (character.get_curCarte().get_id() != 2084)
					exploit = true;
				break;

			case "DR3234|2874": // kamas + oeuf de tofu obèse
				exploit = true;
				break;

			case "DR333|414": // Dofus cawotte TODO : à corriger des l'implantation en jeux!
				exploit = true;
				break;

			case "DR318|259": // Ouverture de la banque
				String map = character.get_curCarte().get_id() + "";
				String banqueMap = "7549 8366 1674 10216 10217 10370";

				if (!banqueMap.contains(map))
					exploit = true;
				break;
			}
			break;

		case "OD": // Objet au sol
		case "Od": // Objet Détruire
			if (packet.contains("-"))
				exploit = true;
			break;

		default: // Valeur négative
			if (packet.contains("-"))
				exploit = true;
			break;
		}

		if (exploit) {
			// on kick le joueur
			try {
				// kick du serveur de jeu
				if (account.getGameThread() != null){
					SocketManager.REALM_SEND_MESSAGE(account.getGameThread().get_out(), "7|");
					account.getGameThread().kick();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				// kick du serveur de connection
				if (account.getClient() != null || account.getClient().getIoSession().isConnected()){
					SocketManager.REALM_SEND_MESSAGE(account.getGameThread().get_out(), "7|");
					account.getClient().kick();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return exploit;
	}
}

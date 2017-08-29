package org.command.player;

import org.client.Characters;
import org.common.Constant;
import org.common.SQLManager;
import org.common.SocketManager;
import org.common.World;
import org.kernel.Config;
import org.object.Objects;
import org.spell.SpellEffect;

public class FmCac {
	
	public static boolean exec(Characters _perso, String msg) {
		if(_perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
		{
			SocketManager.PERSO_SEND_EXCHANGE_REQUEST_ERROR(_perso,'S');
			return true;
		}
		
		Objects obj = _perso.getObjetByPos(Constant.ITEM_POS_ARME);

		if(SQLManager.GET_ACCOUNT_POINTS(_perso.getAccID()) < Config.PRICE_FM_CAC) {
			SocketManager.GAME_SEND_MESSAGE(_perso, "Action impossible : vous avez moins de "+Config.PRICE_FM_CAC+" points.", Config.CONFIG_MOTD_COLOR);
			return true;
			
		} else if(_perso.get_fight() != null) {
			SocketManager.GAME_SEND_MESSAGE(_perso, "Action impossible : vous ne devez pas être en combat", Config.CONFIG_MOTD_COLOR);
			return true;
		
		} else if(obj == null) {
			SocketManager.GAME_SEND_MESSAGE(_perso, "Action impossible : vous ne portez pas d'arme", Config.CONFIG_MOTD_COLOR);
			return true;
		}

		boolean containNeutre = false;
		
		for(SpellEffect effect : obj.getEffects()) {
			if(effect.getEffectID() == 100 || effect.getEffectID() == 95)
				containNeutre = true;
		}
		if(!containNeutre) {
			SocketManager.GAME_SEND_MESSAGE(_perso, "Action impossible : votre arme n'a pas de dégats neutre", Config.CONFIG_MOTD_COLOR);
			return true;
		}
		
		String answer;
		
		try {
			answer = msg.substring(9, msg.length() - 1);
		} catch(Exception e) {
			SocketManager.GAME_SEND_MESSAGE(_perso, "Action impossible : vous n'avez pas spécifié l'élément (air, feu, terre, eau) qui remplacera les dégats/vols de vies neutres", Config.CONFIG_MOTD_COLOR);
			return true;
		}

		if(!answer.equalsIgnoreCase("air") && !answer.equalsIgnoreCase("terre") && !answer.equalsIgnoreCase("feu") && !answer.equalsIgnoreCase("eau")) {
			SocketManager.GAME_SEND_MESSAGE(_perso, "Action impossible : l'élément " + answer + " n'existe pas ! (dispo : air, feu, terre, eau)", Config.CONFIG_MOTD_COLOR);
			return true;
		}

		for(int i = 0; i < obj.getEffects().size(); i++) {
			
			if(obj.getEffects().get(i).getEffectID() == 100) {
				if(answer.equalsIgnoreCase("air"))
					obj.getEffects().get(i).setEffectID(98);
				
				if(answer.equalsIgnoreCase("feu"))
					obj.getEffects().get(i).setEffectID(99);
				
				if(answer.equalsIgnoreCase("terre"))
					obj.getEffects().get(i).setEffectID(97);
				
				if(answer.equalsIgnoreCase("eau"))
					obj.getEffects().get(i).setEffectID(96);
			}

			if(obj.getEffects().get(i).getEffectID() == 95) {
				if(answer.equalsIgnoreCase("air"))
					obj.getEffects().get(i).setEffectID(93);
				
				if(answer.equalsIgnoreCase("feu"))
					obj.getEffects().get(i).setEffectID(94);
					
				if(answer.equalsIgnoreCase("terre"))
					obj.getEffects().get(i).setEffectID(92);
				
				if(answer.equalsIgnoreCase("eau"))
					obj.getEffects().get(i).setEffectID(91);
				
			}
		}
		
		int new_points = SQLManager.GET_ACCOUNT_POINTS(_perso.getAccID()) - Config.PRICE_FM_CAC ;
		if(new_points < 0) new_points = 0;
		SQLManager.SET_ACCOUNT_POINTS(new_points, _perso.getAccID());

		SocketManager.GAME_SEND_STATS_PACKET(_perso);
		SocketManager.GAME_SEND_MESSAGE(_perso, "Votre objet <b>" + obj.getTemplate().getName() + "</b> a été forgemagé avec succès en " + answer, Config.CONFIG_MOTD_COLOR);
		SQLManager.SAVE_PERSONNAGE(_perso, false);
		/**GameThread.Object_move(_perso, _perso.get_compte().getGameThread().get_out(), 1, obj.getGuid(), 1);
		_perso.removeItem(obj.getGuid());
		_perso.addObjet(World.getObjet(obj.getGuid()));**/
		
		_perso.removeItem(obj.getGuid());
		World.removeItem(obj.getGuid());
		SQLManager.DELETE_ITEM(obj.getGuid());
		SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, obj.getGuid());
		
		if(_perso.addObjet(obj, true))// Si le joueur n'avait pas d'item
			World.addObjet(obj, true);
		
		SocketManager.GAME_SEND_STATS_PACKET(_perso);
		SocketManager.GAME_SEND_Ow_PACKET(_perso);
		return true;
	
	}
}

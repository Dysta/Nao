package org.command.player;

import java.util.ArrayList;

import org.client.Characters;
import org.common.Formulas;
import org.common.SocketManager;
import org.utils.Colors;

public class Maitre
{
	public ArrayList<Characters> _esclaves = new ArrayList<Characters>();
	private Characters _maitre = null;
	
	public Maitre(Characters maitre)
	{
		_maitre = maitre;
		_esclaves = getpEsclavesInMap();
	}
	
	public void refreh()
	{
		if(_esclaves.size() < 1)
			_maitre.set_maitre(null);
	}
	
	
	public void teleportAllEsclaves() {
		for (Characters p : getEsclaves()) {
			if (p.get_fight() != null || p.get_curExchange() != null) {
				SocketManager.GAME_SEND_MESSAGE(p, "Vous n'avez pas pus être téléporter car vous êtes occuper.", Colors.RED);
				
				SocketManager.GAME_SEND_MESSAGE(_maitre, "Le membre <b>" + p.get_name() + "</b> n'a pas été téléporté car il est occupé", Colors.RED);
				continue;
			}
			if(p.get_curCarte() == _maitre.get_curCarte())
				continue;
			int[] cellTab = Formulas.getAdjacentCells(_maitre.get_curCell().getID());
			int cell = cellTab[Formulas.getRandomValue(0, cellTab.length-1)];
			p.teleport(_maitre.get_curCarte().get_id(), cell);
			SocketManager.GAME_SEND_MESSAGE(p, "Votre chef <b>" + _maitre.get_name() + "</b> vous a téléporté à ces côtés.", Colors.RED);
		}

	}
	

	public boolean isEsclave(Characters perso) {
		for (Characters p : getEsclaves())
			if (p.get_GUID() == perso.get_GUID())
				return true;
		return false;
	}
	
	public ArrayList<Characters> getEsclaves()
	{
		refreh();	
		return new ArrayList<Characters>(_esclaves);
	}
	
	private ArrayList<Characters> getpEsclavesInMap()
	{
		ArrayList<Characters> list = new ArrayList<Characters>();
		
		for (Characters p : _maitre.get_curCarte().getPersos()) {
			if (p == null || !p.isOnline())
				continue;
			if (p.get_GUID() == _maitre.get_GUID())
				continue;
			if (p.get_curCarte().get_id() != _maitre.get_curCarte().get_id())
				continue;
			if (p.get_fight() != null)
				continue;
			if (!p.get_compte().get_curIP().equals(_maitre.get_compte().get_curIP()))
				continue;

			list.add(p);
			SocketManager.GAME_SEND_MESSAGE(p, "Félicitation ! Vous venez de rentrer dans l'escouade de <b>" + _maitre.get_name() + "</b>.", Colors.RED);
			p.setEsclave(true);
		}
		
		return list;
	}
	
	public static boolean hasMaitreInMap(Characters perso)
	{
		if(perso.get_maitre()!=null)
			return false;
		
		for(Characters p: perso.get_curCarte().getPersos())
			if(p.get_maitre() != null)
				if(p.get_maitre().isEsclave(perso))
					return true;
		return false;
	}
	
}

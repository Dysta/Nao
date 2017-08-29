package org.fight.extending;

import java.util.ArrayList;
import java.util.Random;

import org.client.Characters;
import org.client.Characters.Group;
import org.common.SocketManager;
import org.fight.Fight;
import org.kernel.Config;
import org.utils.Colors;

public class Kolizeum {

	public synchronized static void addPlayer(Characters player)
	{
		Team actualTeam = null;
		boolean hasFind = false;
		int lLevel = 0;
		
		if(Team.koliTeams.size() > 0)
		{
			for (Team team: Team.koliTeams)
			{
				int tLvl = team.getkLevel();
				int diff = player.get_lvl() - tLvl;
				if (diff < 0)
					diff *= -1;
				
				if (diff < Config.KOLI_LEVEL && team.getkCharacters().size() < Config.KOLIMAX_PLAYER)
				{
					team.getkCharacters().add(player);
					actualTeam = team;
					player.setKolizeum(0);
					hasFind = true;
					lLevel = team.getkLevel();
					SocketManager.GAME_SEND_MESSAGE(player, "<b>Kolizeum:</b> Inscription prise en compte !", Colors.RED);
					break;
				}
			}
		}
		
		if (!hasFind)
		{
			actualTeam = new Team(player, player.get_lvl());
			Team.koliTeams.add(actualTeam);
			player.setKolizeum(0);
			int level = Config.KOLI_LEVEL;
			if (player.get_lvl() + level > 201){
				while(player.get_lvl() + level > 201)
					level --;
			}
			SocketManager.GAME_SEND_MESSAGE(player, "<b>Kolizeum:</b> Inscription prise en compte !", Colors.RED);
			SocketManager.GAME_SEND_MESSAGE_TO_ALL( "<b>Kolizeum:</b> "+(Config.KOLIMAX_PLAYER - 1)+" joueurs à l'appel de niveau "+(player.get_lvl()-Config.KOLI_LEVEL)+" - "+(player.get_lvl()+level)+" pour compléter une team !", Colors.RED);
			return;
		}
		
		if (actualTeam.getkCharacters().size() == Config.KOLIMAX_PLAYER)
		{
			for (Team team: Team.koliTeams)
			{
				if (team.getkCharacters().contains(player))
					continue;
				
				int tLvl = team.getkLevel();
				int diff = actualTeam.getkLevel() - tLvl;
				if (diff < 0)
					diff *= -1;
				
				if (diff < Config.KOLI_LEVEL && team.getkCharacters().size() == Config.KOLIMAX_PLAYER)
				{
					if (kickBusyTeam(team))
						continue;
					else if (kickBusyTeam(actualTeam))
						return;
					
					newKolizeum(actualTeam, team);
					Team.koliTeams.remove(team);
					Team.koliTeams.remove(actualTeam);
					String team1 = null;
					String team2 = null;
					
					for (Characters c: team.getkCharacters()){
						if (team1 != null)
							team1 += " <b>,</b> " + c.get_name();
						else
							team1 = c.get_name();
					}
					for (Characters c: actualTeam.getkCharacters()){
						if (team2 != null) 
							team2 += " <b>,</b> " + c.get_name();
						else 
							team2 = c.get_name();
						
					}
					SocketManager.GAME_SEND_MESSAGE_TO_ALL("<b>Kolizeum:</b> "+team1+" <b>VS</b> "+team2, Colors.RED);
					return;
				}
				
			}
			int level = Config.KOLI_LEVEL;
			int kLevel = lLevel;
			if (kLevel + level > 201){
				while(kLevel + level > 201)
					level --;
			}
			SocketManager.GAME_SEND_MESSAGE_TO_ALL( "<b>Kolizeum:</b> "+Config.KOLIMAX_PLAYER+" joueurs manquants "+(kLevel-Config.KOLI_LEVEL)+" - "+(kLevel+level)+" pour débuter un nouveau match !", Colors.RED);
		}
		return;
	}
	
	public synchronized static void addGroup(Group group)
	{
		Team actualTeam = new Team(group.getPersos(), group.getGroupLevel());
		Team.koliTeams.add(actualTeam);
		
		for (Characters player: group.getPersos()) {
			player.setKolizeum(0);
			SocketManager.GAME_SEND_MESSAGE(player, "<b>Kolizeum:</b> Inscription type groupe prise en compte !", Colors.RED);
		}
		
		for (Team team: Team.koliTeams)
		{
			if (team.getkCharacters().contains(group.getPersos().get(1)))
				continue;
			
			int tLvl = team.getkLevel();
			int diff = actualTeam.getkLevel() - tLvl;
			if (diff < 0)
				diff *= -1;
			
			if (diff < Config.KOLI_LEVEL && team.getkCharacters().size() == Config.KOLIMAX_PLAYER)
			{
				if (kickBusyTeam(team))
					continue;
				else if (kickBusyTeam(actualTeam))
					return;
				
				newKolizeum(actualTeam, team);
				Team.koliTeams.remove(team);
				Team.koliTeams.remove(actualTeam);
				String team1 = null;
				String team2 = null;
				
				for (Characters c: team.getkCharacters()){
					if (team1 != null)
						team1 += " <b>,</b> " + c.get_name();
					else
						team1 = c.get_name();
				}
				for (Characters c: actualTeam.getkCharacters()){
					if (team2 != null) 
						team2 += " <b>,</b> " + c.get_name();
					else 
						team2 = c.get_name();
					
				}
				SocketManager.GAME_SEND_MESSAGE_TO_ALL("<b>Kolizeum:</b> "+team1+" <b>VS</b> "+team2, Colors.RED);
				return;
			}
		}
		int level = Config.KOLI_LEVEL;
		if ((group.getGroupLevel()/Config.KOLIMAX_PLAYER) + level > 201){
			while((group.getGroupLevel()/Config.KOLIMAX_PLAYER) + level > 201)
				level --;
		}
		SocketManager.GAME_SEND_MESSAGE_TO_ALL( "<b>Kolizeum:</b> "+Config.KOLIMAX_PLAYER+" joueurs manquants "+((group.getGroupLevel()/group.getPersos().size() + level)-Config.KOLI_LEVEL)+" - "+((group.getGroupLevel()/group.getPersos().size())+level)+" pour débuter un nouveau match !", Colors.RED);
		return;
	}
	
	
	
	public synchronized static void newKolizeum (Team team1, Team team2)
	{
		for (Characters p: team1.getkCharacters()){
			p.setLastMapFight(p.get_curCarte().get_id());
			p.setKolizeum(1);
		}
		for (Characters p: team2.getkCharacters()){
			p.setLastMapFight(p.get_curCarte().get_id());
			p.setKolizeum(1);
		}
		teleport(team1.getkCharacters(), team2.getkCharacters());
		SocketManager.GAME_SEND_MAP_NEW_DUEL_TO_MAP(team1.getkCharacters().get(0).get_curCarte(), team1.getkCharacters().get(0).get_GUID(), team2.getkCharacters().get(0).get_GUID());
		try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
		SocketManager.GAME_SEND_MAP_START_DUEL_TO_MAP(team2.getkCharacters().get(0).get_curCarte(), team1.getkCharacters().get(0).get_GUID(), team2.getkCharacters().get(0).get_GUID());
		@SuppressWarnings("unused")
		Fight f = team1.getkCharacters().get(0).get_curCarte().newKoli(team1.getkCharacters(), team2.getkCharacters());
		try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
		for (int i=1; i<Config.KOLIMAX_PLAYER; i++) {
			team1.getkCharacters().get(0).get_fight().joinFight(team1.getkCharacters().get(i), team1.getkCharacters().get(0).get_GUID());
			team2.getkCharacters().get(0).get_fight().joinFight(team2.getkCharacters().get(i), team2.getkCharacters().get(0).get_GUID());
		}
		return;
	}
	
	public synchronized static void delPlayer(Characters player)
	{
		try {
			for(Team team: Team.koliTeams)
			{
				if (team.getkCharacters().contains(player))
				{
					team.getkCharacters().remove(player);
					if (team.getkCharacters().size() == 0)
						Team.koliTeams.remove(team);
					player.setKolizeum(-1);
					SocketManager.GAME_SEND_MESSAGE(player, "<b>Kolizeum:</b> Désinscription acceptée !", Colors.RED);
				}
			}
		}catch (Exception e){}
			return;
	}
	
	public static boolean kickBusyTeam(Team t)
	{
		for (Characters c: t.getkCharacters())
		{
			if (c.get_fight() != null) {
				c.get_fight().leftFight(null, c, false);
				try {
					Thread.sleep(2000);
				} catch(Exception e){}
			}
			if (c==null || c.get_fight()!=null || !c.isOnline()) {
				c.setKolizeum(-1);
				t.getkCharacters().remove(c);
				if (t.getkCharacters().size() == 0)
					Team.koliTeams.remove(t);
				SocketManager.GAME_SEND_MESSAGE(c, "<b>Kolizeum:</b> Vous avez été désinscris du kolizeum pour indisponibilité !", Colors.RED);
				return true;
			}
		}
		return false;
	}
	
	private static int getRandomMap() {
		Random rand = new Random();
		switch(rand.nextInt(4)+1) {
		case 1 : return Integer.parseInt(Config.KOLIMAPS.split(",")[0]);
		case 2 : return Integer.parseInt(Config.KOLIMAPS.split(",")[1]);
		case 3 : return Integer.parseInt(Config.KOLIMAPS.split(",")[2]);
		case 4 : return Integer.parseInt(Config.KOLIMAPS.split(",")[3]);
		default : return Integer.parseInt(Config.KOLIMAPS.split(",")[0]);
		}
	}

	private static void teleport(ArrayList<Characters> team1, ArrayList<Characters> team2) {
		short MAP_ID = (short) getRandomMap();
		for (Characters p : team1)
			p.teleport(MAP_ID, 1);
		for (Characters p : team2)
			p.teleport(MAP_ID, 0);
	}
}

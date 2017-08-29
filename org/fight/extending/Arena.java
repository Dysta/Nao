package org.fight.extending;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.client.Characters;
import org.common.Constant;
import org.common.SocketManager;
import org.common.World;
import org.fight.Fight;
import org.game.tools.AllColors;
import org.kernel.Config;

import java.util.Random;
import java.util.TreeMap;

public class Arena {

	//Teams en attente
	public static Map<Integer, Integer> teamInWaiting = new TreeMap<Integer, Integer>(); //TeamID | LevelMoyen
	
	
	public synchronized static void addTeam(Team team)
	{
		Characters first = Team.getPlayer(team, 1);
		Characters second = Team.getPlayer(team, 2);
		int actualLevelTeam = (first.get_lvl() + second.get_lvl()) / 2;
		
		teamInWaiting.put(team.getId(), actualLevelTeam);
		for (Characters c: Team.getPlayers(team)){
			c.setArena(0);
		}
		SocketManager.GAME_SEND_MESSAGE_TO_ALL("<b>Arena:</b> La team <b>"+team.getName()+"</b> (Côte: "+team.getCote()+", Level Moyen: "+actualLevelTeam+") est en attente d'arène 2v2!", AllColors.RED);
		
		for (Entry<Integer, Integer> t: teamInWaiting.entrySet())
		{
			if (team.getId() == Team.getTeamByID(t.getKey()).getId())
				continue;
			
			int tLvl = t.getValue();
			int diff = actualLevelTeam - tLvl;
			if (diff < 0)
				diff *= -1;
			
			if (diff < Config.KOLI_LEVEL)
			{
				if (kickBusyTeam(Team.getTeamByID(t.getKey())))
					continue;
				else if (kickBusyTeam(team))
					return;
				
				newArena(Team.getTeamByID(t.getKey()), team);
				teamInWaiting.remove(t.getKey());
				teamInWaiting.remove(team.getId());
				SocketManager.GAME_SEND_MESSAGE_TO_ALL("<b>Arena:</b>  <b>"+team.getName()+"</b> (Côte: "+team.getCote()+")  | VS | <b>"+Team.getTeamByID(t.getKey()).getName()+"</b> (Côte: "+Team.getTeamByID(t.getKey()).getCote()+") ", AllColors.RED);
				return;
			}
			
		}
		return;
	}
	
	public synchronized static void newArena (Team team1, Team team2)
	{
		for (Characters p: Team.getPlayers(team1)){
			p.setLastMapFight(p.get_curCarte().get_id());
			p.setArena(1);
		}
		for (Characters p: Team.getPlayers(team2)){
			p.setLastMapFight(p.get_curCarte().get_id());
			p.setArena(1);
		}
		teleport(Team.getPlayers(team1), Team.getPlayers(team2));
		SocketManager.GAME_SEND_MAP_NEW_DUEL_TO_MAP(Team.getPlayers(team1).get(0).get_curCarte(), Team.getPlayers(team1).get(0).get_GUID(), Team.getPlayers(team2).get(0).get_GUID());
		try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
		SocketManager.GAME_SEND_MAP_START_DUEL_TO_MAP(Team.getPlayers(team2).get(0).get_curCarte(), Team.getPlayers(team1).get(0).get_GUID(), Team.getPlayers(team2).get(0).get_GUID());
		@SuppressWarnings("unused")
		Fight f = Team.getPlayers(team1).get(0).get_curCarte().newKoli(Team.getPlayers(team1), Team.getPlayers(team2));
		try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }
		for (int i=1; i<2; i++) {
			Team.getPlayers(team1).get(0).get_fight().joinFight(Team.getPlayers(team1).get(i), Team.getPlayers(team1).get(0).get_GUID());
			Team.getPlayers(team2).get(0).get_fight().joinFight(Team.getPlayers(team2).get(i), Team.getPlayers(team2).get(0).get_GUID());
		}
		return;
	}
	
	public static void delTeam(Team team)
	{
		if (teamInWaiting.containsKey(team.getId()))
		{
			SocketManager.GAME_SEND_MESSAGE_TO_ALL("<b>Arena:</b> La team <b>"+team.getName()+"</b> vient de se désinscrire de l'arène 2v2!", AllColors.RED);
			for (Characters c: Team.getPlayers(team))
				c.setArena(-1);
			teamInWaiting.remove(team.getId());
			return;
		}
	}
	
	public static boolean kickBusyTeam(Team t)
	{
		for (String cc: t.getCharacters().split(","))
		{
			Characters c = World.getPersonnage(Integer.parseInt(cc));
			if (c.get_fight() != null) {
				c.get_fight().leftFight(null, c, false);
				try {
					Thread.sleep(2000);
				} catch(Exception e){}
			}
			if (c==null || c.get_fight()!=null || !c.isOnline()) {
				SocketManager.GAME_SEND_MESSAGE_TO_ALL("<b>Arena:</b> La team d'arène 2v2 ("+t.getName()+") a été désinscrite suite à l'innactivité ou indisponibilité du joueur "+c.get_name()+" !", Config.CONFIG_MOTD_COLOR);
				for (String ccc: t.getCharacters().split(","))
				{
					Characters cccc = World.getPersonnage(Integer.parseInt(ccc));
					cccc.setArena(-1);
				}
				teamInWaiting.remove(t.getId());
				return true;
			}
			c.setArena(1);
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
	
	public static void sendReward(Team winners, Team loosers) {
		
		for (Characters c: Team.getPlayers(winners))
		{
			if (c.getArena() == -1){
				return;
			}
		}
		int points;
		if (loosers.getCote() - winners.getCote() < 0)
			points = 25;
		else if (winners.getCote() - loosers.getCote() < 0)
			points = 75;
		else
			points = 50;
		winners.setCote(winners.getCote() + points);
		Team.updateTeam(winners.getId());
		for (Characters c: Team.getPlayers(winners))
		{
			SocketManager.GAME_SEND_MESSAGE(c, "Félicitation, vous venez de remporter <b>"+points+"</b> points de quote ! Faites .infoteam pour voir votre total de points !", Config.CONFIG_MOTD_COLOR);
		}
		SocketManager.GAME_SEND_MESSAGE_TO_ALL("<b>Arena:</b> La team <b>"+winners.getName()+"</b> (Côte: "+winners.getCote()+") gagne contre la team <b>"+loosers.getName()+"</b> (Côte: "+loosers.getCote()+")!", AllColors.RED);
		return;
	}
	
	public static void withdrawPoints(Team loosers, Team winners) {
		
		for (Characters c: Team.getPlayers(loosers))
		{
			if (c.getArena() == -1){
				return;
			}
		}
		int points;
		if (loosers.getCote() - winners.getCote() < 0)
			points = 25;
		else if (winners.getCote() - loosers.getCote() < 0)
			points = 75;
		else
			points = 50;
		if (loosers.getCote() - points > 0)
			loosers.setCote(loosers.getCote() - points);
		else
			loosers.setCote(0);
		Team.updateTeam(loosers.getId());
		for (Characters c: Team.getPlayers(loosers))
		{
			SocketManager.GAME_SEND_MESSAGE(c, "Dommage, vous venez de perdre <b>"+points+"</b> points de quote ! Faites .infoteam pour voir votre total de points !", Config.CONFIG_MOTD_COLOR);
		}
		return;
	}
	
	public static boolean isVerifiedTeam (int class1, int class2)
	{
		String paliers = Constant.CLASS_OSAMODAS+","+Constant.CLASS_SACRIEUR+","+Constant.CLASS_ENIRIPSA+","+Constant.CLASS_XELOR;
		for (String classes: paliers.split(","))
		{
			if (class1 == Integer.parseInt(classes))
			{
				for (String classes2: paliers.split(","))
				{
					if (class1 == Integer.parseInt(classes2))
						continue;
					else if (class2 == Integer.parseInt(classes2))
						return false;
				}
			}
		}
		return true;
	}
}

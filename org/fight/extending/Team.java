package org.fight.extending;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.client.Characters;
import org.common.SQLManager;
import org.common.SocketManager;
import org.common.World;
import org.game.tools.AllColors;
import org.kernel.Config;

import com.mysql.jdbc.PreparedStatement;

public class Team {

	//Arena 2v2
	private int id;
	private String name;
	private String characters; //Guid,Guid	
	private int cote;
	private int rank;
	//Kolizeum
	private ArrayList<Characters> kCharacters= new ArrayList<Characters>(); 
	private int kLevel;
	
	public static Map<Integer, Team> Teams = new HashMap<Integer, Team>();
	public static ArrayList<Team> koliTeams = new ArrayList<Team>(); //Teams temporaires
	
	public Team (int id, String name, String characters, int quote, int rank)
	{
		setId(id);
		setName(name);
		setCharacters(characters);
		setCote(quote);
		setRank(rank);
	}
	
	public Team (Characters characters, int kLevel)
	{
		kCharacters.add(characters);
		setkLevel(kLevel);
	}
	
	public Team (ArrayList<Characters> characters, int kLevel)
	{
		setkCharacters(characters);
		setkLevel(kLevel);
	}
	
	public static void addTeamToMap(Team x)
	{
		Teams.put(x.getId(), x);
	}
	
	public static void removeTeam(Team team, Characters p)
	{
		for(String c: team.getCharacters().split(","))
		{
			Characters player = World.getPersonnage(Integer.parseInt(c));
			player.setTeamID(-1);
			if(player.isOnline())
			{
				if (player != p)
					SocketManager.GAME_SEND_MESSAGE(player, "La team "+team.getName()+" dont vous faisiez parti est dissoute par "+p.get_name()+" !", AllColors.RED);	
				else
					SocketManager.GAME_SEND_MESSAGE(player, "Vous venez de dissoudre la team "+team.getName()+" !", AllColors.RED);
			}
			SQLManager.SAVE_PERSONNAGE(player, false);
		}
		deleteTeam(team.getId());
	}
	
	
	public static Team getTeamByCharacter(Characters c)
	{
		for(Entry<Integer, Team> team: Teams.entrySet())
		{
			for(String guid: team.getValue().getCharacters().split(","))
			{
				if (Integer.parseInt(guid) == c.get_GUID())
					return Teams.get(team.getKey());
			}
		}
		return null;
	}
	
	public static Characters getPlayer(Team team, int number)
	{
		Characters player = null;
		if (number == 1)
			player = World.getPersonnage(Integer.parseInt(team.getCharacters().split(",")[0]));
		else if (number == 2)
			player = World.getPersonnage(Integer.parseInt(team.getCharacters().split(",")[1]));
		return player;
	}
	
	public static ArrayList<Characters> getPlayers(Team team)
	{
		ArrayList<Characters> players = new ArrayList<Characters>();
		for (String player: team.getCharacters().split(","))
		{
			players.add(World.getPersonnage(Integer.parseInt(player)));
		}
		return players;
	}
	
	public static Team getTeamByID(int id)
	{
		if (Teams.containsKey(id))
			return Teams.get(id);
		return null;
	}
	 
	public static void deleteTeam(int teamID)
	{
		String baseQuery = "DELETE FROM arena WHERE id = ?;";
		try {
			PreparedStatement p = SQLManager.newTransact(baseQuery, SQLManager.Connection());
			p.setInt(1, teamID);
			p.execute();
			SQLManager.closePreparedStatement(p);
		} catch (SQLException e) {
			System.out.println("Game: SQL ERROR: " + e.getMessage());
			System.out.println("Game: Query: " + baseQuery);
		}
		Teams.remove(teamID);
	}
	
	public synchronized static boolean addTeam(String name, String players, int quote, int rank)
	{
		
		int nextID = 0;
		if (Teams.size() < 1)
			nextID = 1;
		if (World.getPersonnage(Integer.parseInt(players.split(",")[0])).getTeamID() != -1 || World.getPersonnage(Integer.parseInt(players.split(",")[1])).getTeamID() != -1)
		{
			for (String player: players.split(","))
			{
				SocketManager.GAME_SEND_MESSAGE(World.getPersonnage(Integer.parseInt(player)), "Le joueur "+World.getPersonnage(Integer.parseInt(player)).get_name()+" a déjà une équipe d'arène !", AllColors.RED);
				return false;
			}
		}
		for (Entry<Integer, Team> team: Team.getTeams().entrySet()){
			
			if (team.getValue().getId() >= nextID)
				nextID = team.getValue().getId()+1;
			
			if (team.getValue().getName().equals(name))
			{
				for (String player: players.split(","))
				{
					if (World.getPersonnage(Integer.parseInt(player)).isOnline())
						SocketManager.GAME_SEND_MESSAGE(World.getPersonnage(Integer.parseInt(player)), "Le nom d'équipe <b>'"+name+"</b> est déjà pris, choisissez en un autre !", AllColors.RED);
				}
				return false;
			}
		}
		int newRank = Teams.size()+1;
		String Request = "INSERT INTO `arena` VALUES(?,?,?,?,?);";
		try {
			PreparedStatement PS = SQLManager.newTransact(Request, SQLManager.Connection());
			PS.setInt(1, nextID);
			PS.setString(2, name);
			PS.setString(3, players);
			PS.setInt(4, quote);
			PS.setInt(5, newRank);
			PS.executeUpdate();
			
			SQLManager.closePreparedStatement(PS);
		} catch (SQLException e) {e.printStackTrace();}
		Teams.put(nextID, new Team(nextID, name, players, quote, newRank));
		for (String player: players.split(","))
		{
			World.getPersonnage(Integer.parseInt(player)).setTeamID(nextID);
			SQLManager.SAVE_PERSONNAGE(World.getPersonnage(Integer.parseInt(player)), false);
		}
		return true;
	}
	
	public static void calculAllRank()
	{
		
		try{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM arena ORDER BY quote DESC LIMIT 0, 1000", Config.DB_NAME);
			int rank = 0;
			while(RS.next()){
				rank++;
				Team.getTeamByID(RS.getInt("id")).setRank(rank);
			}
			RS.getStatement().close();
			RS.close();
		}catch (Exception e){e.printStackTrace();}
	}
	
	public static void updateTeam(int teamID)
	{
		
		Team team = getTeamByID(teamID);
		try {
			String baseQuery = "UPDATE `arena` SET `quote` = ? WHERE id = ?;";
			PreparedStatement p = SQLManager.newTransact(baseQuery, SQLManager.Connection());
			p.setInt(1, team.getCote());
			p.setInt(2, team.getId());
			p.execute();
			SQLManager.closePreparedStatement(p);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		updateAllRank();
	}
	
	public static void updateAllRank()
	{
		calculAllRank();
		for (Entry<Integer, Team> team: Team.Teams.entrySet())
		{
			try {
				String baseQuery = "UPDATE `arena` SET `rank` = ? WHERE id = ?;";
				PreparedStatement p = SQLManager.newTransact(baseQuery, SQLManager.Connection());
				p.setInt(1, team.getValue().getRank());
				p.setInt(2, team.getValue().getId());
				p.execute();
				SQLManager.closePreparedStatement(p);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCharacters() {
		return characters;
	}

	public void setCharacters(String characters) {
		this.characters = characters;
	}

	public int getCote() {
		return cote;
	}

	public void setCote(int cote) {
		this.cote = cote;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public static Map<Integer, Team> getTeams() {
		return Teams;
	}

	public static void setTeams(Map<Integer, Team> teams) {
		Teams = teams;
	}

	public ArrayList<Characters> getkCharacters() {
		return kCharacters;
	}

	public void setkCharacters(ArrayList<Characters> kCharacters) {
		this.kCharacters = kCharacters;
	}

	public int getkLevel() {
		return kLevel;
	}

	public void setkLevel(int kLevel) {
		this.kLevel = kLevel;
	}

}

package org.command.player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.client.Characters;
import org.common.SQLManager;
import org.common.World;
import org.object.Objects;

import com.mysql.jdbc.PreparedStatement;

public class RapidStuff { /** Author: Return **/

	//Content
	private int id;
	private String name;
	private String itemGuids;
	private int owner;
	//AllRapidStuffs
	public static Map <Integer, RapidStuff> rapidStuffs = new HashMap <Integer, RapidStuff>();
	
	public RapidStuff (int _id, String _name, String _itemGuids, int _owner)
	{
		setId(_id);
		setName(_name);
		setItemGuids(_itemGuids);
		setOwner(_owner);
	}

	public String getItemGuids() {
		return itemGuids;
	}

	public static RapidStuff getRapidStuffByID(int id)
	{
		return (rapidStuffs.get(id));
	}
	
	public ArrayList<Objects> getObjects() 
	{
		ArrayList<Objects> toReturn = new ArrayList<Objects>();
		for (String s: getItemGuids().split(","))
		{
			if (Integer.parseInt(s) != 0)
				toReturn.add(World.getObjet(Integer.parseInt(s)));
		}
		return toReturn;
	}
	
	public Objects getObject(int place)
	{
		return World.getObjet(Integer.parseInt(getItemGuids().split(",")[(place-1)]));
	}
	
	public static boolean addRapidStuff(Characters player, String name, String objects)
	{
		for (RapidStuff rs: player.getRapidStuffs())
		{
			if (rs.getName().equals(rs) || rs.getItemGuids().equals(objects))
				return false;
		}
		int nextID = getNextID();
		String Request = "INSERT INTO `rapidstuff` VALUES(?,?,?,?);";
		try {
			PreparedStatement PS = SQLManager.newTransact(Request, SQLManager.Connection());
			PS.setInt(1, nextID);
			PS.setString(2, name);
			PS.setString(3, objects);
			PS.setInt(4, player.get_GUID());
			PS.executeUpdate();
			
			SQLManager.closePreparedStatement(PS);
		} catch (SQLException e) {e.printStackTrace();}
		
		rapidStuffs.put(nextID, new RapidStuff(nextID, name, objects, player.get_GUID()));
		SQLManager.SAVE_PERSONNAGE(player, false);
		return true;
	}
	
	public static boolean removeRapidStuff(RapidStuff rs)
	{
		int id = rs.getId();
		String baseQuery = "DELETE FROM rapidstuff WHERE id = ?;";
		try {
			PreparedStatement p = SQLManager.newTransact(baseQuery, SQLManager.Connection());
			p.setInt(1, id);
			p.execute();
			SQLManager.closePreparedStatement(p);
		} catch (SQLException e) {
			System.out.println("Game: SQL ERROR: " + e.getMessage());
			System.out.println("Game: Query: " + baseQuery);
			return false; //Useless tout ça mais osef xD Flemme de changer
		}
		rapidStuffs.remove(id);
		return true;
	}
	
	public synchronized static int getNextID()
	{
		int max = 1;
		for(int a : rapidStuffs.keySet())if(a > max)max = a;
		return max+1;
	}
	
	public void setItemGuids(String itemGuids) {
		this.itemGuids = itemGuids;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getOwner() {
		return owner;
	}

	public void setOwner(int owner) {
		this.owner = owner;
	}
	
	public Characters getCharacter() {
		return World.getPersonnage(getOwner());
	}
}

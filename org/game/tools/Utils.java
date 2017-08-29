package org.game.tools;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;

import org.client.Accounts;
import org.client.Characters;
import org.common.SQLManager;
import org.common.SocketManager;
import org.common.World;
import org.game.GameServer;
import org.kernel.Config;
import org.object.Objects;
import org.object.Objects.ObjTemplate;

import com.mysql.jdbc.PreparedStatement;

public class Utils {
	
	public static void loadShopStore() {
		
		
		if(!ParseTools.shopStore.isEmpty())
			ParseTools.shopStore.clear();
		if (ParseTools.storePacket.length() > 3)
			ParseTools.storePacket = "102";
		
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM shop_store;", Config.DB_NAME);
			while(RS.next()){
				ParseTools.shopStore.put(RS.getInt("template"), RS.getInt("price"));
			}
			ParseTools.isFirstLoad = true;
			RS.getStatement().close();
			RS.close();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		int number = 1;
		for (Entry<Integer, Integer> item: ParseTools.shopStore.entrySet()){
			if (item.getKey() > 0)
				ParseTools.storePacket += number + ";" + item.getKey() + ";" + item.getValue() + ";" + World.getObjTemplate(item.getKey()).getStrTemplate() + "|";
			else
				ParseTools.storePacket += number + ";" + item.getKey() + ";" + item.getValue() + ";undefined;" + FunctionTools.getPackDescriptionByType(item.getKey()) + "|";
			number ++;
		}
		
	}
	
	public static long loadLastVoteByAccount(long timeLastVote, Accounts accounts) {
		
		try{
			ResultSet RS = SQLManager.executeQuery("SELECT heurevote FROM accounts WHERE guid = '"+accounts.get_GUID()+"';", Config.DB_NAME);
			while(RS.next()){
				if (RS.getInt("heurevote") <= 0)
					timeLastVote = 0;
				else
					timeLastVote = RS.getInt("heurevote");
			}
			RS.getStatement().close();
			RS.close();
		}catch (Exception e){e.printStackTrace();}
		
		return timeLastVote;
		
	}
	
	public static void updateAllIps(Map<String, Long> lastIpTiming) {
		
		String Request = "REPLACE INTO `ip_timing` VALUES(?,?);";
		try {
			PreparedStatement PS = SQLManager.newTransact(Request, SQLManager.Connection());
			for (Entry<String, Long> map: lastIpTiming.entrySet()){
				PS.setString(1, map.getKey());
				PS.setLong(2, map.getValue());
				PS.executeUpdate();
			}
			SQLManager.closePreparedStatement(PS);
		} catch (SQLException e) {e.printStackTrace();}
		
	}
	
	public static void loadIpTiming(Map<String, Long> lastIpTiming) {
		
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM ip_timing;", Config.DB_NAME);
			while(RS.next()){
				lastIpTiming.put(RS.getString("ip"), RS.getLong("time"));
			}
			GameServer.isFirstLoad = true;
			RS.getStatement().close();
			RS.close();
		}catch (Exception e) {e.printStackTrace();}
		
	}
	
	public static int loadPointsByAccount(Accounts account) {
			
			int points = 0;
			try{
				ResultSet RS = SQLManager.executeQuery("SELECT points FROM accounts WHERE guid = '"+account.get_GUID()+"';", Config.DB_NAME);
				while(RS.next()){
					if (RS.getInt("points") <= 0)
						points = 0;
					else
						points = RS.getInt("points");
				}
				RS.getStatement().close();
				RS.close();
			}catch (Exception e){e.printStackTrace();}
			
			return points;
		}
	
	public static int loadPointsPackByAccount(Accounts account) {
		
		int points = 0;
		try{
			ResultSet RS = SQLManager.executeQuery("SELECT pack_points FROM accounts WHERE guid = '"+account.get_GUID()+"';", Config.DB_NAME);
			while(RS.next()){
				if (RS.getInt("pack_points") <= 0)
					points = 0;
				else
					points = RS.getInt("pack_points");
			}
			RS.getStatement().close();
			RS.close();
		}catch (Exception e){e.printStackTrace();}
		
		return points;
	}
	
	public static int loadVotesByAccount(Accounts account) {
		
		int votes = 0;
		try{
			ResultSet RS = SQLManager.executeQuery("SELECT vote FROM accounts WHERE guid = '"+account.get_GUID()+"';", Config.DB_NAME);
			while(RS.next()){
				if (RS.getInt("vote") <= 0)
					votes = 0;
				else
					votes = RS.getInt("vote");
			}
			RS.getStatement().close();
			RS.close();
		}catch (Exception e){e.printStackTrace();}
		
		return votes;
	}
	
	public static int loadPriceByTemplateID(int templateID) {
		
		int price = 0;
		try{
			ResultSet RS = SQLManager.executeQuery("SELECT price FROM shop_store WHERE template = '"+templateID+"';", Config.DB_NAME);
			while(RS.next()){
				if (RS.getInt("price") <= 0)
					price = 0;
				else
					price = RS.getInt("price");
			}
			RS.getStatement().close();
			RS.close();
		}catch (Exception e){e.printStackTrace();}
		
		return price;
	}
	
	public static void updatePointsByAccount(Accounts account, int diff) {
		
		String Request = "UPDATE accounts SET points = '"+diff+"' WHERE guid = '"+account.get_GUID()+"';";
		try {
			PreparedStatement PS = SQLManager.newTransact(Request, SQLManager.Connection());
			PS.execute();
			SQLManager.closePreparedStatement(PS);
		} catch (SQLException e) {e.printStackTrace();}
		
	}
	
	public static void updatePointsPackByAccount(Accounts account, int diff) {
	
		String Request = "UPDATE accounts SET pack_points = '"+diff+"' WHERE guid = '"+account.get_GUID()+"';";
		try {
			PreparedStatement PS = SQLManager.newTransact(Request, SQLManager.Connection());
			PS.execute();
			SQLManager.closePreparedStatement(PS);
		} catch (SQLException e) {e.printStackTrace();}
		
	}
	
	public static void addObjectToCharacter(Characters _perso, int templateID, int quantity, boolean isMax, int price) {
		
		ObjTemplate t = World.getObjTemplate(templateID);
		Objects obj =t.createNewItem(quantity, isMax, -1);
		if (_perso.addObjetShop(obj, true, _perso, templateID, quantity, price))
			World.addObjet(obj, true);
		SocketManager.GAME_SEND_Ow_PACKET(_perso);
		SQLManager.SAVE_PERSONNAGE(_perso, true);
		
	}
	
	public static String getActualDate() { //Leur calendar pourrit faut vraiment l'améliorer u_u 
		
		int year = Calendar.getInstance().get(Calendar.YEAR);
		String mounth = String.valueOf((Calendar.getInstance().get(Calendar.MONTH)+1));
		String dayMounth = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
		String hours = String.valueOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
		String minutes = String.valueOf(Calendar.getInstance().get(+Calendar.MINUTE));
		String seconds = String.valueOf(Calendar.getInstance().get(Calendar.SECOND));
		int mounthM = Integer.parseInt(mounth); int dayMounthM = Integer.parseInt(dayMounth), hoursM = Integer.parseInt(hours), 
			minutesM = Integer.parseInt(minutes), secondsM =Integer.parseInt(seconds);
		
		if (mounthM == 13) mounth = "1"; if (mounthM < 10) mounth = 0 + mounth;
		if (dayMounthM < 10) dayMounth = 0 + dayMounth; if (hoursM < 10) hours = 0 + hours;
		if (minutesM < 10) minutes = 0 + minutes; if (secondsM < 10) seconds = 0 + seconds;
		
		return "[" + dayMounth + "/" + mounth + "/" + year + "] " + hours + ":" + minutes + ":" + seconds;
	}
	
	public static void addToShopLogs(Characters _perso, int templateID, int quantity, int price, int itemGuid) {
		
		String Request = null;
		if (templateID > 0)
			Request = "INSERT INTO `shop_logs` VALUES('"+_perso.get_name()+"','"+World.getObjTemplate(templateID).getName()+"','"+templateID+"','"+itemGuid+"','"+quantity+"','"+price+"','"+getActualDate()+"');";
		else
			Request = "INSERT INTO `shop_logs` VALUES('"+_perso.get_name()+"','"+FunctionTools.getPackDescriptionByType(templateID)+"','-1','-1','1','"+price+"','"+getActualDate()+"');";
		
		try {
			PreparedStatement PS = SQLManager.newTransact(Request, SQLManager.Connection());
				PS.execute();
			SQLManager.closePreparedStatement(PS);
		} catch (SQLException e) {e.printStackTrace();}
		
	}
	
	public static void deleteShopItemLogsByGuid(int guid) {
		
		String Request = "DELETE FROM shop_logs WHERE itemGuid = '"+guid+"';";
		try {
			PreparedStatement PS = SQLManager.newTransact(Request, SQLManager.Connection());
				PS.execute();
			SQLManager.closePreparedStatement(PS);
		} catch (SQLException e) {e.printStackTrace();}
		
	}
	
	public static String loadShopLogsByCharacter(Characters perso) {
		
		String logs = "";
		
		try{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM shop_logs;", Config.DB_NAME);
			while(RS.next()){
				if (RS.getString("character").equals(perso.get_name()))
					logs += "<b>Objet : </b>" + RS.getString("objectName") + " (T: " + RS.getInt("itemTemplate") + ", G: " + RS.getInt("itemGuid") + ")" + " [ Prix: " + RS.getInt("price") + " - Date: " + RS.getString("date") + " ]\n";
			}
			RS.getStatement().close();
			RS.close();
		}catch (Exception e){e.printStackTrace();}
		
		return logs;
		
	}
	
	public static String loadShopLogsByCharacterPacket(Characters perso) {
		
		String logs = "";
		boolean isFirst = true;
		try{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM shop_logs;", Config.DB_NAME);
			while(RS.next()){
				if (RS.getString("character").equals(perso.get_name())) {
					if (!isFirst)
						logs += "|";
					logs += RS.getInt("itemTemplate") + ";" + RS.getInt("itemGuid") + ";" + World.getObjTemplate(RS.getInt("itemTemplate")).getStrTemplate() + ";" + RS.getInt("price") + ";" + RS.getString("date");
					isFirst = false;
				}
			}
			RS.getStatement().close();
			RS.close();
		}catch (Exception e){e.printStackTrace();}
		
		return logs;
		
	}
	
	public static ArrayList<Integer> loadShopGuidsLogsByCharacter(Characters perso) {
		
		ArrayList<Integer> guids = new ArrayList<Integer>();
		
		try{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM shop_logs;", Config.DB_NAME);
			while(RS.next()){
				if (RS.getString("character").equals(perso.get_name())) {
					guids.add(RS.getInt("itemGuid"));
				}
			}
			RS.getStatement().close();
			RS.close();
		}catch (Exception e){e.printStackTrace();}
		
		return guids;
		
	}
	
	public static ArrayList<Characters> loadRankingByType(String type) {
		
		ArrayList<Characters> characters = new ArrayList<Characters>();
		
		try{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM personnages ORDER BY "+type+" DESC LIMIT 0, 10", Config.DB_NAME);
			while(RS.next()){
				characters.add(World.getPersonnage(RS.getInt("guid")));
			}
			RS.getStatement().close();
			RS.close();
		}catch (Exception e){e.printStackTrace();}
		
		return characters;
	}
	
	public static ArrayList<Accounts> loadRankingVote() {
		
		ArrayList<Accounts> Accounts = new ArrayList<Accounts>();
		
		try{
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM accounts ORDER BY vote DESC LIMIT 0, 10", Config.DB_NAME);
			while(RS.next()){
				Accounts.add(World.getCompte(RS.getInt("guid")));
			}
			RS.getStatement().close();
			RS.close();
		}catch (Exception e){e.printStackTrace();}
		
		return Accounts;
	}

	public static void addPackToCharacter(Characters _perso, int type) {
		
		addToShopLogs(_perso, type, 1, loadPriceByTemplateID(type), type);
		type *= -1;
		
		switch (type) {
			case 1:
				_perso.levelUp(true, true);
				SQLManager.SAVE_PERSONNAGE(_perso, false);
				break;
			case 2:
				_perso.addKamas(500000);
				SQLManager.SAVE_PERSONNAGE(_perso, false);
				break;
			case 3:
				_perso.addHonor(18000-_perso.get_honor());
				SQLManager.SAVE_PERSONNAGE(_perso, false);
				break;
			case 4:
				Accounts c = _perso.get_compte();
				if(c.get_vip() == 0)
				{
					c.set_vip(1);
					SQLManager.UPDATE_ACCOUNT_VIP(c);
				}
				break;
			case 5:
				_perso.modifAlignement((byte) 3);
				if(_perso.get_honor() < 18000)
				{
					_perso.addHonor(18000-_perso.get_honor());
				}
				_perso.set_title(100);
				SocketManager.GAME_SEND_Im_PACKET(_perso, "021;1~6971");
				SocketManager.GAME_SEND_Ow_PACKET(_perso);
				SQLManager.SAVE_PERSONNAGE(_perso, true);
				break;
			case 6:
				String sortsList = "367,370,373,366,390,391,392,393,394,395,396,397,350,364";
				String sorts[] = sortsList.split(",");
				for(String sort:sorts)
				{
					if(sort.isEmpty()) continue;
					if(World.getSort(Integer.parseInt(sort)) == null) return;
					_perso.learnSpell(Integer.parseInt(sort), 1, false,true);
				}
				SQLManager.SAVE_PERSONNAGE(_perso, false);
				break;
			case 7:
				ObjTemplate t = World.getObjTemplate(1557);
				ObjTemplate t2 = World.getObjTemplate(1558);
				Objects obj =t.createNewItem(100, false, -1);
				Objects obj2 =t2.createNewItem(100, false, -1);
				if (_perso.addObjet(obj, true))
					World.addObjet(obj, true);
				if (_perso.addObjet(obj2, true))
					World.addObjet(obj2, true);
				SocketManager.GAME_SEND_Ow_PACKET(_perso);
				SQLManager.SAVE_PERSONNAGE(_perso, true);
			default:
				break;
	    }
		
	}
	
	
}

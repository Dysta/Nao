package org.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.client.Accounts;
import org.client.Characters;
import org.common.World.Area;
import org.common.World.Couple;
import org.common.World.Drop;
import org.common.World.IOTemplate;
import org.common.World.ItemSet;
import org.common.World.SubArea;
import org.fight.extending.Team;
import org.fight.object.Collector;
import org.fight.object.Monster;
import org.fight.object.Prism;
import org.command.GmCommandManager;
import org.command.PlayerCommandManager;
import org.command.player.RapidStuff;
import org.kernel.Config;
import org.kernel.Console;
import org.kernel.Console.Color;
import org.kernel.Logs;
import org.object.Action;
import org.object.AuctionHouse;
import org.object.Guild;
import org.object.Houses;
import org.object.Hustle;
import org.object.Maps;
import org.object.Mount;
import org.object.NpcTemplates;
import org.object.Objects;
import org.object.Trunk;
import org.object.AuctionHouse.HdvEntry;
import org.object.Guild.GuildMember;
import org.object.Maps.MountPark;
import org.object.NpcTemplates.NPC_question;
import org.object.NpcTemplates.NPC_reponse;
import org.object.Objects.ObjTemplate;
import org.object.job.Job;
import org.spell.Spell;
import org.spell.Spell.SortStats;

import com.mysql.jdbc.PreparedStatement;

public class SQLManager {

	private static Connection Connection;
	private static Lock myConnectionLocker = new ReentrantLock();
	private static Timer timerCommit;
	private static boolean needCommit;

	public static void closeResultSet(ResultSet RS) {
		try {
			RS.getStatement().close();
			RS.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void closePreparedStatement(PreparedStatement p) {
		try {
			p.clearParameters();
			p.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static final boolean setUpConnexion() {
		try {
			myConnectionLocker.lock();
			Connection = DriverManager.getConnection("jdbc:mysql://" + Config.DB_HOST + "/" + Config.DB_NAME,
					Config.DB_USER, Config.DB_PASS);
			Connection.setAutoCommit(false);

			if (!Connection.isValid(1000)) {
				Console.println("Conection to the database invalid", Color.RED);
				return false;
			}

			needCommit = false;
			TIMER(true);
			return true;
		} catch (SQLException e) {
			String error = e.getMessage();
			Console.println("connection to the database error : " + error, Color.RED);
			e.printStackTrace();
		} finally {
			myConnectionLocker.unlock();
		}
		return false;
	}

	public static Connection Connection() {
		try {
			myConnectionLocker.lock();
			boolean valid = true;
			try {
				valid = !Connection.isClosed();
			} catch (SQLException e) {
				valid = false;
			}
			if (Connection == null || !valid) {
				closeCons();
				setUpConnexion();
			}
			return Connection;
		} finally {
			myConnectionLocker.unlock();
		}
	}

	public static ResultSet executeQuery(String query, String DBNAME) throws SQLException {
		Connection DB;

		DB = Connection();

		Statement stat = DB.createStatement();
		ResultSet RS = stat.executeQuery(query);
		stat.setQueryTimeout(300);
		return RS;
	}

	public static PreparedStatement newTransact(String baseQuery, Connection dbCon) throws SQLException {
		PreparedStatement toReturn = (PreparedStatement) dbCon.prepareStatement(baseQuery);

		needCommit = true;
		return toReturn;
	}

	public static void commitTransacts() {
		try {
			myConnectionLocker.lock();
			Connection().commit();
		} catch (SQLException e) {
			Console.println("SQL ERROR:" + e.getMessage(), Color.RED);
			e.printStackTrace();
		} finally {
			myConnectionLocker.unlock();
		}
	}

	public static void rollBack(Connection con) {
		try {
			synchronized (con) {
				con.rollback();
			}
		} catch (SQLException e) {
			Console.println("SQL ERROR: " + e.getMessage(), Color.RED);
			e.printStackTrace();
		}
	}

	public static void closeCons() {
		try {
			commitTransacts();
			try {
				myConnectionLocker.lock();
				Connection.close();
			} finally {
				myConnectionLocker.unlock();
			}
		} catch (Exception e) {
			Console.println("Erreur a la fermeture des connexions SQL:" + e.getMessage(), Color.RED);
			e.printStackTrace();
		}
	}

	public static void UPDATE_SUBSCRIPTION(Accounts acc, int subscription) {
		String baseQuery = "UPDATE accounts SET " + "`subscription` = ?" + " WHERE `guid` = ?;";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection);
			p.setInt(1, subscription);
			p.setInt(2, acc.get_GUID());
			p.executeUpdate();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void UPDATE_ACCOUNT_DATA(Accounts acc) {
		try {
			String baseQuery = "UPDATE accounts SET " + "`bankKamas` = ?," + "`bank` = ?," + "`level` = ?,"
					+ "`pseudo` = ?," + "`banned` = ?," + "`banned_time` = ?," + "`friends` = ?," + "`enemy` = ?,"
					+ "`mute_time` = ?," + "`mute_raison` = ?," + "`mute_pseudo` = ?" + " WHERE `guid` = ?;";
			PreparedStatement p = newTransact(baseQuery, Connection());

			p.setLong(1, acc.getBankKamas());
			p.setString(2, acc.parseBankObjetsToDB());
			p.setInt(3, acc.get_gmLvl());
			p.setString(4, acc.get_pseudo());
			p.setInt(5, (acc.isBanned() ? 1 : 0));
			p.setLong(6, acc.getBannedTime());
			p.setString(7, acc.parseFriendListToDB());
			p.setString(8, acc.parseEnemyListToDB());
			p.setLong(9, acc.getMuteTime());
			p.setString(10, acc.getMuteRaison());
			p.setString(11, acc.getMutePseudo());
			p.setInt(12, acc.get_GUID());

			p.executeUpdate();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_CRAFTS() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from crafts;", Config.DB_NAME);
			Console.print("[...] loading crafts", Color.YELLOW);
			while (RS.next()) {
				ArrayList<Couple<Integer, Integer>> m = new ArrayList<Couple<Integer, Integer>>();

				boolean cont = true;
				for (String str : RS.getString("craft").split(";")) {
					try {
						int tID = Integer.parseInt(str.split("\\*")[0]);
						int qua = Integer.parseInt(str.split("\\*")[1]);
						m.add(new Couple<Integer, Integer>(tID, qua));
					} catch (Exception e) {
						e.printStackTrace();
						cont = false;
					}
				}
				// s'il y a eu une erreur de parsing, on ignore cette recette
				if (!cont)
					continue;

				World.addCraft(RS.getInt("id"), m);
			}
			Console.print("\r[OK] loading crafts ", Color.GREEN);
			closeResultSet(RS);
			;
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_CHALLENGES() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from challenge;", Config.DB_NAME);
			Console.print("[...] loading challenges", Color.YELLOW);
			while (RS.next()) {
				StringBuilder chal = new StringBuilder();
				chal.append(RS.getInt("id")).append(",");
				chal.append(RS.getInt("gainXP")).append(",");
				chal.append(RS.getInt("gainDrop")).append(",");
				chal.append(RS.getInt("gainParMob")).append(",");
				chal.append(RS.getInt("conditions"));
				World.addChallenge(chal.toString());
			}
			Console.print("\r[OK] loading challenges ", Color.GREEN);
			closeResultSet(RS);
			;
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void UPDATE_SUBAREA(SubArea subarea) {
		try {
			String baseQuery = "UPDATE `subarea_data` SET `alignement` = ?, `Prisme` = ? WHERE id = ?;";
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, subarea.getalignement());
			p.setInt(2, subarea.getPrismeID());
			p.setInt(3, subarea.getID());
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void UPDATE_AREA(Area area) {
		try {
			String baseQuery = "UPDATE `area_data` SET `alignement` = ?, `Prisme` = ? WHERE id = ?;";
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, area.getalignement());
			p.setInt(2, area.getPrismeID());
			p.setInt(3, area.getID());
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static int LOAD_PRISMES() {
		int numero = 0;
		try {
			ResultSet RS = executeQuery("SELECT * from prismes;", Config.DB_NAME);
			Console.print("[...] loading prismes", Color.YELLOW);
			while (RS.next()) {
				World.addPrisme(new Prism(RS.getInt("id"), RS.getInt("alignement"), RS.getInt("level"),
						RS.getShort("Carte"), RS.getInt("cell"), RS.getInt("honor"), RS.getInt("area")));
			}
			Console.print("\r[OK] loading prismes ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Console.println("SQL ERROR: " + e.getMessage(), Color.RED);
			e.printStackTrace();
			numero = 0;
		}
		return numero;
	}

	public static void ADD_PRISME(Prism Prisme) {
		try {
			String baseQuery = "INSERT INTO prismes(`id`,`alignement`,`level`,`Carte`,`cell`,`area`, `honor`) VALUES(?,?,?,?,?,?,?);";
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, Prisme.getID());
			p.setInt(2, Prisme.getalignement());
			p.setInt(3, Prisme.getlevel());
			p.setInt(4, Prisme.getCarte());
			p.setInt(5, Prisme.getCell());
			p.setInt(6, Prisme.getAreaConquest());
			p.setInt(7, Prisme.getHonor());
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void DELETE_PRISME(int id) {
		String baseQuery = "DELETE FROM prismes WHERE id = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, id);
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
			Console.println("Game: Query: " + baseQuery);
		}
	}

	public static void SAVE_PRISME(Prism Prisme) {
		String baseQuery = "UPDATE prismes SET `level` = ?, `honor` = ?, `area`= ? WHERE `id` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, Prisme.getlevel());
			p.setInt(2, Prisme.getHonor());
			p.setInt(3, Prisme.getAreaConquest());
			p.setInt(4, Prisme.getID());
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Console.println("SQL ERROR: " + e.getMessage(), Color.RED);
			Console.println("Query: " + baseQuery);
			e.printStackTrace();
		}
	}

	public static void LOAD_GUILDS() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from guilds;", Config.DB_NAME);
			Console.print("[...] loading guilds", Color.YELLOW);
			while (RS.next()) {
				World.addGuild(new Guild(RS.getInt("id"), RS.getString("name"), RS.getString("emblem"),
						RS.getInt("lvl"), RS.getLong("xp"), RS.getInt("capital"), RS.getInt("nbrmax"),
						RS.getString("sorts"), RS.getString("stats")), false);
			}
			Console.print("\r[OK] loading guilds ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_GUILD_MEMBERS() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from guild_members;", Config.DB_NAME);
			Console.print("[...] loading guild members", Color.YELLOW);
			while (RS.next()) {
				Guild G = World.getGuild(RS.getInt("guild"));
				if (G == null)
					continue;
				G.addMember(RS.getInt("guid"), RS.getString("name"), RS.getInt("level"), RS.getInt("gfxid"),
						RS.getInt("rank"), RS.getByte("pxp"), RS.getLong("xpdone"), RS.getInt("rights"),
						RS.getByte("align"), RS.getString("lastConnection").toString().replaceAll("-", "~"));
			}
			Console.print("\r[OK] loading guild members ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_MOUNTS() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from mounts_data;", Config.DB_NAME);
			Console.print("[...] loading mounts", Color.YELLOW);
			while (RS.next()) {
				World.addDragodinde(new Mount(RS.getInt("id"), RS.getInt("color"), RS.getInt("sexe"),
						RS.getInt("amour"), RS.getInt("endurance"), RS.getInt("level"), RS.getLong("xp"),
						RS.getString("name"), RS.getInt("fatigue"), RS.getInt("energie"), RS.getInt("reproductions"),
						RS.getInt("maturite"), RS.getInt("serenite"), RS.getString("items"), RS.getString("ancetres"),
						RS.getString("ability")));
			}
			Console.print("\r[OK] loading mounts ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_DROPS() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from drops;", Config.DB_NAME);
			Console.print("[...] loading drops", Color.YELLOW);
			while (RS.next()) {
				Monster MT = World.getMonstre(RS.getInt("mob"));
				MT.addDrop(new Drop(RS.getInt("item"), RS.getInt("seuil"), RS.getFloat("taux"), RS.getInt("max")));
			}
			Console.print("\r[OK] loading drops ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_ITEMSETS() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from itemsets;", Config.DB_NAME);
			Console.print("[...] loading itemsets", Color.YELLOW);
			while (RS.next()) {
				World.addItemSet(new ItemSet(RS.getInt("id"), RS.getString("items"), RS.getString("bonus")));
			}
			Console.print("\r[OK] loading itemsets ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_IOTEMPLATE() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from interactive_objects_data;", Config.DB_NAME);
			Console.print("[...] loading IO template", Color.YELLOW);
			while (RS.next()) {
				World.addIOTemplate(new IOTemplate(RS.getInt("id"), RS.getInt("respawn"), RS.getInt("duration"),
						RS.getInt("unknow"), RS.getInt("walkable") == 1));
			}
			Console.print("\r[OK] loading IO template ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static int LOAD_MOUNTPARKS() {
		int nbr = 0;
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from mountpark_data;", Config.DB_NAME);
			Console.print("[...] loading mountparks", Color.YELLOW);
			while (RS.next()) {
				Maps map = World.getCarte(RS.getShort("mapid"));
				if (map == null)
					continue;
				World.addMountPark(new MountPark(RS.getInt("owner"), map, RS.getInt("cellid"), RS.getInt("size"),
						RS.getString("data"), RS.getInt("guild"), RS.getInt("price")));
			}
			Console.print("\r[OK] loading mountparks ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
			nbr = 0;
		}
		return nbr;
	}

	public static void LOAD_JOBS() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from jobs_data;", Config.DB_NAME);
			Console.print("[...] loading jobs", Color.YELLOW);
			while (RS.next()) {
				World.addJob(new Job(RS.getInt("id"), RS.getString("tools"), RS.getString("crafts")));
			}
			Console.print("\r[OK] loading jobs ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_AREA() {
		try {
			ResultSet RS = executeQuery("SELECT * from area_data;", Config.DB_NAME);
			Console.print("[...] loading areas", Color.YELLOW);
			while (RS.next()) {
				World.Area A = new World.Area(RS.getInt("id"), RS.getInt("superarea"), RS.getString("name"),
						RS.getInt("alignement"), RS.getInt("Prisme"));

				World.addArea(A);
				A.getSuperArea().addArea(A);
			}
			Console.print("\r[OK] loading areas ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_SUBAREA() {
		try {
			ResultSet RS = executeQuery("SELECT * from subarea_data;", Config.DB_NAME);
			Console.print("[...] loading subareas", Color.YELLOW);
			while (RS.next()) {
				World.SubArea SA = new World.SubArea(RS.getInt("id"), RS.getInt("area"), RS.getInt("alignement"),
						RS.getString("name"), RS.getInt("isFree"), RS.getInt("Prisme"),
						(RS.getInt("subscribeNeed") == 1 ? true : false));

				World.addSubArea(SA);

				if (SA.getArea() != null)
					SA.getArea().addSubArea(SA);
			}
			Console.print("\r[OK] loading subareas ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static int LOAD_NPCS() {
		int nbr = 0;
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from npcs;", Config.DB_NAME);
			Console.print("[...] loading npcs", Color.YELLOW);
			while (RS.next()) {
				Maps map = World.getCarte(RS.getShort("mapid"));
				if (map == null)
					continue;
				map.addNpc(RS.getInt("npcid"), RS.getInt("cellid"), RS.getInt("orientation"));
			}
			Console.print("\r[OK] loading npcs ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
			nbr = 0;
		}
		return nbr;
	}

	public static int LOAD_PERCEPTEURS() {
		int nbr = 0;
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from percepteurs;", Config.DB_NAME);
			Console.print("[...] loading percepteurs", Color.YELLOW);
			while (RS.next()) {
				Maps map = World.getCarte(RS.getShort("mapid"));
				if (map == null)
					continue;

				World.addPerco(new Collector(RS.getInt("guid"), RS.getShort("mapid"), RS.getInt("cellid"),
						RS.getByte("orientation"), RS.getInt("guild_id"), RS.getShort("N1"), RS.getShort("N2"),
						RS.getString("objets"), RS.getLong("kamas"), RS.getLong("xp")));
			}
			Console.print("\r[OK] loading percepteurs ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
			nbr = 0;
		}
		return nbr;
	}

	public static int LOAD_HOUSES() {
		int nbr = 0;
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from houses;", Config.DB_NAME);
			Console.print("[...] loading houses", Color.YELLOW);
			while (RS.next()) {
				Maps map = World.getCarte(RS.getShort("map_id"));
				if (map == null)
					continue;

				World.addHouse(new Houses(RS.getInt("id"), RS.getShort("map_id"), RS.getInt("cell_id"),
						RS.getInt("owner_id"), RS.getInt("sale"), RS.getInt("guild_id"), RS.getInt("access"),
						RS.getString("key"), RS.getInt("guild_rights"), RS.getInt("mapid"), RS.getInt("caseid")));
			}
			Console.print("\r[OK] loading houses ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
			nbr = 0;
		}
		return nbr;
	}

	public static void SET_ACCOUNT_POINTS(int points, int accID) {
		String executeQuery = "UPDATE `accounts` SET `points`=? WHERE `guid`= ?";
		try {
			PreparedStatement PS = newTransact(executeQuery, Connection);
			PS.setInt(1, points);
			PS.setInt(2, accID);
			PS.executeUpdate();
			closePreparedStatement(PS);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static int GET_ACCOUNT_POINTS(int accID) {
		int points = 0;
		try {
			ResultSet RS = executeQuery("SELECT `points` FROM `accounts` WHERE `guid` = " + accID + ";",
					Config.DB_NAME);
			boolean encounter = RS.first();
			if (encounter)
				points = RS.getInt("points");
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
		return points;
	}

	public static void LOAD_COMPTES() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from accounts;", Config.DB_NAME);
			String baseQuery = "UPDATE accounts " + "SET `reload_needed` = 0 " + "WHERE guid = ?;";
			PreparedStatement p = newTransact(baseQuery, Connection());
			Console.print("[...] loading accounts", Color.YELLOW);
			while (RS.next()) {
				Accounts C = new Accounts(RS.getInt("guid"), RS.getString("account").toLowerCase(),
						RS.getString("pass"), RS.getString("pseudo"), RS.getString("question"), RS.getString("reponse"),
						RS.getInt("level"), RS.getInt("subscription"), RS.getInt("vip"), (RS.getInt("banned") == 1),
						RS.getLong("banned_time"), RS.getString("lastIP"), RS.getString("lastConnectionDate"),
						RS.getString("bank"), RS.getInt("bankKamas"), RS.getString("friends"), RS.getString("enemy"),
						RS.getInt("cadeau"), RS.getLong("mute_time"), RS.getString("mute_raison"),
						RS.getString("mute_pseudo"));
				World.addAccount(C);
				World.addAccountbyName(C);

				p.setInt(1, RS.getInt("guid"));
				p.executeUpdate();
			}
			Console.print("\r[OK] loading accounts ", Color.GREEN);
			closePreparedStatement(p);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static int getNextPersonnageGuid() {
		try {
			ResultSet RS = executeQuery("SELECT guid FROM personnages ORDER BY guid DESC LIMIT 1;", Config.DB_NAME);
			if (!RS.first())
				return 1;
			int guid = RS.getInt("guid");
			guid++;
			closeResultSet(RS);
			return guid;
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}
		return 0;
	}

	public static void LOAD_PERSO_BY_ACCOUNT(int accID) {
		try {
			Accounts c = World.getCompte(accID);
			if (c != null) {
				Map<Integer, Characters> persos = c.get_persos();
				if (persos != null) {
					for (Characters p : persos.values()) {
						if (p == null)
							continue;
						World.verifyClone(p);
					}
				}
			}
		} catch (Exception e) {
			Console.println("Anti clone fail : " + accID + " > " + e.getMessage(), Color.RED);
			e.printStackTrace();
		}
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM personnages WHERE account = '" + accID + "';",
					Config.DB_NAME);
			while (RS.next()) {
				TreeMap<Integer, Integer> stats = new TreeMap<Integer, Integer>();
				stats.put(Constant.STATS_ADD_VITA, RS.getInt("vitalite"));
				stats.put(Constant.STATS_ADD_FORC, RS.getInt("force"));
				stats.put(Constant.STATS_ADD_SAGE, RS.getInt("sagesse"));
				stats.put(Constant.STATS_ADD_INTE, RS.getInt("intelligence"));
				stats.put(Constant.STATS_ADD_CHAN, RS.getInt("chance"));
				stats.put(Constant.STATS_ADD_AGIL, RS.getInt("agilite"));

				Characters p = World.getPersonnage(RS.getInt("guid"));
				if (p != null) {

					if (p.get_fight() != null) {
						if (World.getCompte(accID) != null)
							World.getCompte(accID).addPerso(p);
						continue;
					}
				}
				Characters perso = new Characters(RS.getInt("guid"), RS.getString("name"), RS.getString("tag"), RS.getInt("sexe"),
						RS.getInt("class"), RS.getInt("color1"), RS.getInt("color2"), RS.getInt("color3"),
						RS.getLong("kamas"), RS.getInt("spellboost"), RS.getInt("capital"), RS.getInt("energy"),
						RS.getInt("level"), RS.getLong("xp"), RS.getInt("size"), RS.getInt("gfx"),
						RS.getByte("alignement"), RS.getInt("account"), stats, RS.getByte("seeFriend"),
						RS.getByte("seeAlign"), RS.getByte("seeSeller"), RS.getString("canaux"), RS.getShort("map"),
						RS.getInt("cell"), RS.getString("objets"), RS.getString("storeObjets"), RS.getInt("pdvper"),
						RS.getString("spells"), RS.getString("savepos"), RS.getString("jobs"), RS.getInt("mountxpgive"),
						RS.getInt("mount"), RS.getInt("honor"), RS.getInt("deshonor"), RS.getInt("alvl"),
						RS.getString("zaaps"), RS.getByte("title"), RS.getInt("wife"), RS.getInt("teamID"),
						RS.getString("quests"));
				// Vérifications pré-connexion
				perso.VerifAndChangeItemPlace();
				World.addPersonnage(perso);
				int guildId = isPersoInGuild(RS.getInt("guid"));
				if (guildId >= 0) {
					perso.setGuildMember(World.getGuild(guildId).getMember(RS.getInt("guid")));
				}
				if (World.getCompte(accID) != null)
					World.getCompte(accID).addPerso(perso);
			}

			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static void LOAD_PERSO(int persoID) {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM personnages WHERE guid = '" + persoID + "';",
					Config.DB_NAME);
			int accID;
			while (RS.next()) {
				TreeMap<Integer, Integer> stats = new TreeMap<Integer, Integer>();
				stats.put(Constant.STATS_ADD_VITA, RS.getInt("vitalite"));
				stats.put(Constant.STATS_ADD_FORC, RS.getInt("force"));
				stats.put(Constant.STATS_ADD_SAGE, RS.getInt("sagesse"));
				stats.put(Constant.STATS_ADD_INTE, RS.getInt("intelligence"));
				stats.put(Constant.STATS_ADD_CHAN, RS.getInt("chance"));
				stats.put(Constant.STATS_ADD_AGIL, RS.getInt("agilite"));

				accID = RS.getInt("account");

				Characters p = World.getPersonnage(RS.getInt("guid"));
				if (p != null) {
					if (p.get_fight() != null)
						return;
				}
				Characters perso = new Characters(RS.getInt("guid"), RS.getString("name"), RS.getString("tag"), RS.getInt("sexe"),
						RS.getInt("class"), RS.getInt("color1"), RS.getInt("color2"), RS.getInt("color3"),
						RS.getLong("kamas"), RS.getInt("spellboost"), RS.getInt("capital"), RS.getInt("energy"),
						RS.getInt("level"), RS.getLong("xp"), RS.getInt("size"), RS.getInt("gfx"),
						RS.getByte("alignement"), accID, stats, RS.getByte("seeFriend"), RS.getByte("seeAlign"),
						RS.getByte("seeSeller"), RS.getString("canaux"), RS.getShort("map"), RS.getInt("cell"),
						RS.getString("objets"), RS.getString("storeObjets"), RS.getInt("pdvper"),
						RS.getString("spells"), RS.getString("savepos"), RS.getString("jobs"), RS.getInt("mountxpgive"),
						RS.getInt("mount"), RS.getInt("honor"), RS.getInt("deshonor"), RS.getInt("alvl"),
						RS.getString("zaaps"), RS.getByte("title"), RS.getInt("wife"), RS.getInt("teamID"),
						RS.getString("quests"));
				// Vérifications pré-connexion
				perso.VerifAndChangeItemPlace();
				World.addPersonnage(perso);
				int guildId = isPersoInGuild(RS.getInt("guid"));
				if (guildId >= 0) {
					perso.setGuildMember(World.getGuild(guildId).getMember(RS.getInt("guid")));
				}
				if (World.getCompte(accID) != null)
					World.getCompte(accID).addPerso(perso);
			}

			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static void LOAD_PERSOS() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM personnages;", Config.DB_NAME);
			Console.print("[...] loading characters", Color.YELLOW);
			while (RS.next()) {
				TreeMap<Integer, Integer> stats = new TreeMap<Integer, Integer>();
				stats.put(Constant.STATS_ADD_VITA, RS.getInt("vitalite"));
				stats.put(Constant.STATS_ADD_FORC, RS.getInt("force"));
				stats.put(Constant.STATS_ADD_SAGE, RS.getInt("sagesse"));
				stats.put(Constant.STATS_ADD_INTE, RS.getInt("intelligence"));
				stats.put(Constant.STATS_ADD_CHAN, RS.getInt("chance"));
				stats.put(Constant.STATS_ADD_AGIL, RS.getInt("agilite"));

				Characters perso = new Characters(RS.getInt("guid"), RS.getString("name"), RS.getString("tag"), RS.getInt("sexe"),
						RS.getInt("class"), RS.getInt("color1"), RS.getInt("color2"), RS.getInt("color3"),
						RS.getLong("kamas"), RS.getInt("spellboost"), RS.getInt("capital"), RS.getInt("energy"),
						RS.getInt("level"), RS.getLong("xp"), RS.getInt("size"), RS.getInt("gfx"),
						RS.getByte("alignement"), RS.getInt("account"), stats, RS.getByte("seeFriend"),
						RS.getByte("seeAlign"), RS.getByte("seeSeller"), RS.getString("canaux"), RS.getShort("map"),
						RS.getInt("cell"), RS.getString("objets"), RS.getString("storeObjets"), RS.getInt("pdvper"),
						RS.getString("spells"), RS.getString("savepos"), RS.getString("jobs"), RS.getInt("mountxpgive"),
						RS.getInt("mount"), RS.getInt("honor"), RS.getInt("deshonor"), RS.getInt("alvl"),
						RS.getString("zaaps"), RS.getByte("title"), RS.getInt("wife"), RS.getInt("teamID"),
						RS.getString("quests"));
				// Vérifications pré-connexion
				perso.VerifAndChangeItemPlace();
				World.addPersonnage(perso);

				if (World.getCompte(RS.getInt("account")) != null)
					World.getCompte(RS.getInt("account")).addPerso(perso);
			}
			Console.print("\r[OK] loading characters ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static boolean DELETE_PERSO_IN_BDD(Characters perso) {
		int guid = perso.get_GUID();
		String baseQuery = "DELETE FROM personnages WHERE guid = ?;";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, guid);

			p.execute();

			if (!perso.getItemsIDSplitByChar(",").equals("")) {
				baseQuery = "DELETE FROM items WHERE guid IN (?);";
				p = newTransact(baseQuery, Connection());
				p.setString(1, perso.getItemsIDSplitByChar(","));

				p.execute();
			}
			if (!perso.getStoreItemsIDSplitByChar(",").equals("")) {
				baseQuery = "DELETE FROM items WHERE guid IN (?);";
				p = newTransact(baseQuery, Connection());
				p.setString(1, perso.getStoreItemsIDSplitByChar(","));

				p.execute();
			}
			if (perso.getMount() != null) {
				baseQuery = "DELETE FROM mounts_data WHERE id = ?";
				p = newTransact(baseQuery, Connection());
				p.setInt(1, perso.getMount().get_id());

				p.execute();
				World.delDragoByID(perso.getMount().get_id());
			}

			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
			Logs.addToGameLog("Game: Supression du personnage echouee");
			return false;
		}
	}

	public static boolean ADD_PERSO_IN_BDD(Characters perso) {
		String baseQuery = "INSERT INTO personnages( `guid` , `name` , `sexe` , `class` , `color1` , `color2` , `color3` , `kamas` , `spellboost` , `capital` , `energy` , `level` , `xp` , `size` , `gfx` , `account`,`cell`,`map`,`spells`,`objets`, `storeObjets`)"
				+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'','');";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection);

			p.setInt(1, perso.get_GUID());
			p.setString(2, perso.get_name());
			p.setInt(3, perso.get_sexe());
			p.setInt(4, perso.get_classe());
			p.setInt(5, perso.get_color1());
			p.setInt(6, perso.get_color2());
			p.setInt(7, perso.get_color3());
			p.setLong(8, perso.get_kamas());
			p.setInt(9, perso.get_spellPts());
			p.setInt(10, perso.get_capital());
			p.setInt(11, perso.get_energy());
			p.setInt(12, perso.get_lvl());
			p.setLong(13, perso.get_curExp());
			p.setInt(14, perso.get_size());
			p.setInt(15, perso.get_gfxID());
			p.setInt(16, perso.getAccID());
			p.setInt(17, perso.get_curCell().getID());
			p.setInt(18, perso.get_curCarte().get_id());
			p.setString(19, perso.parseSpellToDB());

			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
			Logs.addToGameLog("Game: Creation du personnage echouee");
			return false;
		}
	}

	public static void LOAD_EXP() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from experience;", Config.DB_NAME);
			Console.print("[...] loading levels", Color.YELLOW);
			while (RS.next()) {
				World.addExpLevel(RS.getInt("lvl"), new World.ExpLevel(RS.getLong("perso"), RS.getInt("metier"),
						RS.getInt("dinde"), RS.getInt("pvp")));
			}
			Console.print("\r[OK] loading levels  ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("Game: SQL ERROR during loading levels: " + e.getMessage());
			System.exit(1);
		}
	}

	public static void RESET_MOUNTPARKS() {
		PreparedStatement p;
		String query = "UPDATE mountpark_data SET data='' WHERE owner=-1;";
		try {
			p = newTransact(query, Connection());
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToSQLLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToSQLLog("Game: Query: " + query);
		}
	}

	public static void LOAD_TRIGGERS() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM `scripted_cells`", Config.DB_NAME);
			Console.print("[...] loading triggers", Color.YELLOW);
			while (RS.next()) {
				if (World.getCarte(RS.getShort("MapID")) == null)
					continue;
				if (World.getCarte(RS.getShort("MapID")).getCase(RS.getInt("CellID")) == null)
					continue;

				switch (RS.getInt("EventID")) {
				case 1:// Stop sur la case (triggers)
					World.getCarte(RS.getShort("MapID")).getCase(RS.getInt("CellID")).addOnCellStopAction(
							RS.getInt("ActionID"), RS.getString("ActionsArgs"), RS.getString("Conditions"));
					break;
				default:
					Logs.addToGameLog("action event " + RS.getInt("EventID") + " not implanted pour le trigger map "
							+ RS.getShort("MapID") + " Cell id " + RS.getInt("CellID"));
					break;
				}
			}
			Console.print("\r[OK] loading triggers   ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
			System.exit(1);
		}
	}

	public static void LOAD_MAPS() {
		try {
			Console.print("[...] loading maps", Color.YELLOW);
			ResultSet RS;
			RS = SQLManager.executeQuery("SELECT  * from maps LIMIT " + Constant.DEBUG_MAP_LIMIT + ";", Config.DB_NAME);
			while (RS.next()) {
				World.addCarte(new Maps(RS.getShort("id"), RS.getString("date"), RS.getByte("width"),
						RS.getByte("heigth"), RS.getString("key"), RS.getString("places"), RS.getString("mapData"),
						RS.getString("cells"), RS.getString("monsters"), RS.getString("mappos"), RS.getByte("numgroup"),
						RS.getByte("groupmaxsize")));
			}
			Console.println("\r[OK] loading maps  ", Color.GREEN);
			SQLManager.closeResultSet(RS);

			Console.print("[...] loading mobgroups_fix", Color.YELLOW);
			RS = SQLManager.executeQuery("SELECT  * from mobgroups_fix;", Config.DB_NAME);
			while (RS.next()) {
				Maps c = World.getCarte(RS.getShort("mapid"));
				if (c == null)
					continue;
				if (c.getCase(RS.getInt("cellid")) == null)
					continue;
				c.addStaticGroup(RS.getInt("cellid"), RS.getString("groupData"));
				World.addGroupFix(RS.getInt("mapid") + ";" + RS.getInt("cellid"), RS.getString("groupData"),
						RS.getInt("Timer"));
			}
			Console.print("\r[OK] loading mobgroups_fix  ", Color.GREEN);
			SQLManager.closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("Game: SQL ERROR: " + e.getMessage());
			System.exit(1);
		}
	}

	public static void SAVE_PERSONNAGE(Characters _perso, boolean saveItem) {
		String baseQuery = "UPDATE `personnages` SET " + "`name`= ?," + "`tag` = ?," + "`kamas`= ?," + "`spellboost`= ?,"
				+ "`capital`= ?," + "`energy`= ?," + "`level`= ?," + "`xp`= ?," + "`size` = ?," + "`gfx`= ?,"
				+ "`alignement`= ?," + "`honor`= ?," + "`deshonor`= ?," + "`alvl`= ?," + "`vitalite`= ?,"
				+ "`force`= ?," + "`sagesse`= ?," + "`intelligence`= ?," + "`chance`= ?," + "`agilite`= ?,"
				+ "`seeSpell`= ?," + "`seeFriend`= ?," + "`seeAlign`= ?," + "`seeSeller`= ?," + "`canaux`= ?,"
				+ "`map`= ?," + "`cell`= ?," + "`pdvper`= ?," + "`spells`= ?," + "`objets`= ?," + "`storeObjets`= ?,"
				+ "`savepos`= ?," + "`zaaps`= ?," + "`jobs`= ?," + "`mountxpgive`= ?," + "`mount`= ?," + "`title`= ?,"
				+ "`wife`= ?," + "`teamID` = ?" + " WHERE `personnages`.`guid` = ? LIMIT 1 ;";

		PreparedStatement p = null;

		try {
			p = newTransact(baseQuery, Connection());

			p.setString(1, _perso.get_name());
			p.setString(2, _perso.get_tag());
			p.setLong(3, _perso.get_kamas());
			p.setInt(4, _perso.get_spellPts());
			p.setInt(5, _perso.get_capital());
			p.setInt(6, _perso.get_energy());
			p.setInt(7, _perso.get_lvl());
			p.setLong(8, _perso.get_curExp());
			p.setInt(9, _perso.get_size());
			p.setInt(10, _perso.get_gfxID());
			p.setInt(11, _perso.get_align());
			p.setInt(12, _perso.get_honor());
			p.setInt(13, _perso.getDeshonor());
			p.setInt(14, _perso.getALvl());
			p.setInt(15, _perso.get_baseStats().getEffect(Constant.STATS_ADD_VITA));
			p.setInt(16, _perso.get_baseStats().getEffect(Constant.STATS_ADD_FORC));
			p.setInt(17, _perso.get_baseStats().getEffect(Constant.STATS_ADD_SAGE));
			p.setInt(18, _perso.get_baseStats().getEffect(Constant.STATS_ADD_INTE));
			p.setInt(19, _perso.get_baseStats().getEffect(Constant.STATS_ADD_CHAN));
			p.setInt(20, _perso.get_baseStats().getEffect(Constant.STATS_ADD_AGIL));
			p.setInt(21, (_perso.is_showSpells() ? 1 : 0));
			p.setInt(22, (_perso.is_showFriendConnection() ? 1 : 0));
			p.setInt(23, (_perso.is_showWings() ? 1 : 0));
			p.setInt(24, (_perso.is_showSeller() ? 1 : 0));
			p.setString(25, _perso.get_canaux());
			p.setInt(26, _perso.get_curCarte().get_id());
			p.setInt(27, _perso.get_curCell().getID());
			p.setInt(28, _perso.get_pdvper());
			p.setString(29, _perso.parseSpellToDB());
			p.setString(30, _perso.parseObjetsToDB());
			p.setString(31, _perso.parseStoreItemstoBD());
			p.setString(32, _perso.get_savePos());
			p.setString(33, _perso.parseZaaps());
			p.setString(34, _perso.parseJobData());
			p.setInt(35, _perso.getMountXpGive());
			p.setInt(36, (_perso.getMount() != null ? _perso.getMount().get_id() : -1));
			p.setInt(37, _perso.get_title());
			p.setInt(38, _perso.getWife());
			p.setInt(39, _perso.getTeamID());
			p.setInt(40, _perso.get_GUID());

			p.executeUpdate();

			if (_perso.getGuildMember() != null)
				UPDATE_GUILDMEMBER(_perso.getGuildMember());
			if (_perso.getMount() != null)
				UPDATE_MOUNT_INFOS(_perso.getMount());
			Logs.addToSQLLog("Personnage " + _perso.get_name() + " sauvegarde");
		} catch (Exception e) {
			Logs.addToSQLLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToSQLLog("Requete: " + baseQuery);
			Logs.addToSQLLog("Le personnage " + _perso.get_name() + " n'a pas ete sauvegarde");
			System.exit(1);
		}
		if (saveItem) {
			baseQuery = "UPDATE `items` SET qua = ?, pos= ?, stats = ?" + " WHERE guid = ?;";
			try {
				p = newTransact(baseQuery, Connection());
			} catch (SQLException e1) {
				e1.printStackTrace();
			}

			for (String idStr : _perso.getItemsIDSplitByChar(":").split(":")) {
				try {
					int guid = Integer.parseInt(idStr);
					Objects obj = World.getObjet(guid);
					if (obj == null)
						continue;

					p.setInt(1, obj.getQuantity());
					p.setInt(2, obj.getPosition());
					p.setString(3, obj.parseToSave());
					p.setInt(4, Integer.parseInt(idStr));

					p.execute();
				} catch (Exception e) {
					continue;
				}
				;

			}

			if (_perso.get_compte() == null)
				return;
			for (String idStr : _perso.getBankItemsIDSplitByChar(":").split(":")) {
				try {
					int guid = Integer.parseInt(idStr);
					Objects obj = World.getObjet(guid);
					if (obj == null)
						continue;

					p.setInt(1, obj.getQuantity());
					p.setInt(2, obj.getPosition());
					p.setString(3, obj.parseToSave());
					p.setInt(4, Integer.parseInt(idStr));

					p.execute();
				} catch (Exception e) {
					continue;
				}
				;

			}
		}

		closePreparedStatement(p);
	}

	public static void LOAD_SORTS() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT  * from sorts;", Config.DB_NAME);
			Console.print("[...] loading spells", Color.YELLOW);
			while (RS.next()) {
				int id = RS.getInt("id");
				Spell sort = new Spell(id, RS.getInt("sprite"), RS.getString("spriteInfos"),
						RS.getString("effectTarget"));
				SortStats l1 = parseSortStats(id, 1, RS.getString("lvl1"));
				SortStats l2 = parseSortStats(id, 2, RS.getString("lvl2"));
				SortStats l3 = parseSortStats(id, 3, RS.getString("lvl3"));
				SortStats l4 = parseSortStats(id, 4, RS.getString("lvl4"));
				SortStats l5 = null;
				if (!RS.getString("lvl5").equalsIgnoreCase("-1"))
					l5 = parseSortStats(id, 5, RS.getString("lvl5"));
				SortStats l6 = null;
				if (!RS.getString("lvl6").equalsIgnoreCase("-1"))
					l6 = parseSortStats(id, 6, RS.getString("lvl6"));
				sort.addSortStats(1, l1);
				sort.addSortStats(2, l2);
				sort.addSortStats(3, l3);
				sort.addSortStats(4, l4);
				sort.addSortStats(5, l5);
				sort.addSortStats(6, l6);
				World.addSort(sort);
			}
			Console.print("\r[OK] loading spells ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
			System.exit(1);
		}
	}

	public static void LOAD_OBJ_TEMPLATE() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT  * from item_template;", Config.DB_NAME);
			Console.print("[...] loading objects template", Color.YELLOW);
			while (RS.next()) {
				World.addObjTemplate(new ObjTemplate(RS.getInt("id"), RS.getString("statsTemplate"),
						RS.getString("name"), RS.getInt("type"), RS.getInt("level"), RS.getInt("pod"),
						RS.getInt("prix"), RS.getInt("panoplie"), RS.getString("condition"), RS.getString("armesInfos"),
						RS.getInt("sold"), RS.getInt("avgPrice"), RS.getInt("points")));
			}
			closeResultSet(RS);
			Console.print("\r[OK] loading objects template ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
			System.exit(1);
		}
	}

	private static SortStats parseSortStats(int id, int lvl, String str) {
		try {
			SortStats stats = null;
			String[] stat = str.split(",");
			String effets = stat[0];
			String CCeffets = stat[1];
			int PACOST = 6;
			try {
				PACOST = Integer.parseInt(stat[2].trim());
			} catch (NumberFormatException e) {
			}
			;

			int POm = Integer.parseInt(stat[3].trim());
			int POM = Integer.parseInt(stat[4].trim());
			int TCC = Integer.parseInt(stat[5].trim());
			int TEC = Integer.parseInt(stat[6].trim());
			boolean line = stat[7].trim().equalsIgnoreCase("true");
			boolean LDV = stat[8].trim().equalsIgnoreCase("true");
			boolean emptyCell = stat[9].trim().equalsIgnoreCase("true");
			boolean MODPO = stat[10].trim().equalsIgnoreCase("true");
			// int unk = Integer.parseInt(stat[11]);//All 0
			int MaxByTurn = Integer.parseInt(stat[12].trim());
			int MaxByTarget = Integer.parseInt(stat[13].trim());
			int CoolDown = Integer.parseInt(stat[14].trim());
			String type = stat[15].trim();
			int level = Integer.parseInt(stat[stat.length - 2].trim());
			boolean endTurn = stat[19].trim().equalsIgnoreCase("true");
			stats = new SortStats(id, lvl, PACOST, POm, POM, TCC, TEC, line, LDV, emptyCell, MODPO, MaxByTurn,
					MaxByTarget, CoolDown, level, endTurn, effets, CCeffets, type);
			return stats;
		} catch (Exception e) {
			e.printStackTrace();
			int nbr = 0;
			Console.println("[DEBUG]Sort " + id + " lvl " + lvl);
			for (String z : str.split(",")) {
				Console.println("[DEBUG]" + nbr + " " + z);
				nbr++;
			}
			System.exit(1);
			return null;
		}
	}

	public static void LOAD_MOB_TEMPLATE() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM monsters;", Config.DB_NAME);
			Console.print("[...] loading monsters", Color.YELLOW);
			while (RS.next()) {
				int id = RS.getInt("id");
				int gfxID = RS.getInt("gfxID");
				int align = RS.getInt("align");
				String colors = RS.getString("colors");
				String grades = RS.getString("grades");
				String spells = RS.getString("spells");
				String stats = RS.getString("stats");
				String pdvs = RS.getString("pdvs");
				String pts = RS.getString("points");
				String inits = RS.getString("inits");
				int mK = RS.getInt("minKamas");
				int MK = RS.getInt("maxKamas");
				int IAType = RS.getInt("AI_Type");
				String xp = RS.getString("exps");
				boolean capturable;
				if (RS.getInt("capturable") == 1) {
					capturable = true;
				} else {
					capturable = false;
				}

				World.addMobTemplate(id, new Monster(id, gfxID, align, colors, grades, spells, stats, pdvs, pts, inits,
						mK, MK, xp, IAType, capturable));
			}
			Console.print("\r[OK] loading monsters ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
			System.exit(1);
		}
	}

	public static void LOAD_NPC_TEMPLATE() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM npc_template;", Config.DB_NAME);
			Console.print("[...] loading npcs template", Color.YELLOW);
			while (RS.next()) {
				int id = RS.getInt("id");
				int bonusValue = RS.getInt("bonusValue");
				int gfxID = RS.getInt("gfxID");
				int scaleX = RS.getInt("scaleX");
				int scaleY = RS.getInt("scaleY");
				int sex = RS.getInt("sex");
				int color1 = RS.getInt("color1");
				int color2 = RS.getInt("color2");
				int color3 = RS.getInt("color3");
				String access = RS.getString("accessories");
				int extraClip = RS.getInt("extraClip");
				int customArtWork = RS.getInt("customArtWork");
				int initQId = RS.getInt("initQuestion");
				String ventes = RS.getString("ventes");
				String quests = RS.getString("quests");
				World.addNpcTemplate(new NpcTemplates(id, bonusValue, gfxID, scaleX, scaleY, sex, color1, color2,
						color3, access, extraClip, customArtWork, initQId, ventes, quests));
			}
			Console.print("\r[OK] loading npcs template ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
			System.exit(1);
		}
	}

	public static void SAVE_NEW_ITEM(Objects item) {
		try {
			String baseQuery = "REPLACE INTO `items` VALUES(?,?,?,?,?);";

			PreparedStatement p = newTransact(baseQuery, Connection());

			p.setInt(1, item.getGuid());
			p.setInt(2, item.getTemplate().getID());
			p.setInt(3, item.getQuantity());
			p.setInt(4, item.getPosition());
			p.setString(5, item.parseToSave());

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static boolean SAVE_NEW_FIXGROUP(int mapID, int cellID, String groupData) {
		try {
			String baseQuery = "REPLACE INTO `mobgroups_fix` VALUES(?,?,?)";
			PreparedStatement p = newTransact(baseQuery, Connection());

			p.setInt(1, mapID);
			p.setInt(2, cellID);
			p.setString(3, groupData);

			p.execute();
			closePreparedStatement(p);

			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean DELETE_FIXGROUP(int mapID) {
		try {
			String baseQuery = "DELETE FROM `mobgroups_fix` WHERE mapid = ?";
			PreparedStatement p = newTransact(baseQuery, Connection());

			p.setInt(1, mapID);

			p.execute();
			closePreparedStatement(p);

			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void LOAD_NPC_QUESTIONS() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM npc_questions;", Config.DB_NAME);
			Console.print("[...] loading npcs questions", Color.YELLOW);
			while (RS.next()) {
				World.addNPCQuestion(new NPC_question(RS.getInt("ID"), RS.getString("responses"),
						RS.getString("params"), RS.getString("cond"), RS.getInt("ifFalse")));
			}
			Console.print("\r[OK] loading npcs questions ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
			System.exit(1);
		}
	}

	public static void LOAD_NPC_ANSWERS() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM npc_reponses_actions;", Config.DB_NAME);
			Console.print("[...] loading npcs answers", Color.YELLOW);
			while (RS.next()) {
				int id = RS.getInt("ID");
				int type = RS.getInt("type");
				String args = RS.getString("args");
				if (World.getNPCreponse(id) == null)
					World.addNPCreponse(new NPC_reponse(id));
				World.getNPCreponse(id).addAction(new Action(type, args, ""));
			}
			Console.print("\r[OK] loading npcs answers ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
			System.exit(1);
		}
	}

	public static int LOAD_ENDFIGHT_ACTIONS() {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM endfight_action;", Config.DB_NAME);
			Console.print("[...] loading end fight actions", Color.YELLOW);
			while (RS.next()) {
				Maps map = World.getCarte(RS.getShort("map"));
				if (map == null)
					continue;
				map.addEndFightAction(RS.getInt("fighttype"),
						new Action(RS.getInt("action"), RS.getString("args"), RS.getString("cond")));
			}
			Console.print("\r[OK] loading trigd fight actions ", Color.GREEN);
			closeResultSet(RS);
			return 0;
		} catch (SQLException e) {
			Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
			System.exit(1);
		}
		return 0;
	}

	public static int LOAD_ITEM_ACTIONS() {
		int nbr = 0;
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * FROM use_item_actions;", Config.DB_NAME);
			Console.print("[...] loading items action", Color.YELLOW);
			while (RS.next()) {
				int id = RS.getInt("template");
				int type = RS.getInt("type");
				String args = RS.getString("args");
				if (World.getObjTemplate(id) == null)
					continue;
				World.getObjTemplate(id).addAction(new Action(type, args, ""));
			}
			Console.print("\r[OK] loading items action ", Color.GREEN);
			closeResultSet(RS);
			return nbr;
		} catch (SQLException e) {
			Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
			System.exit(1);
		}
		return nbr;
	}

	public static void LOAD_ITEMS(String ids) {
		String req = "SELECT * FROM items WHERE guid IN (" + ids + ");";
		try {
			ResultSet RS = SQLManager.executeQuery(req, Config.DB_NAME);
			while (RS.next()) {
				int guid = RS.getInt("guid");
				int tempID = RS.getInt("template");
				int qua = RS.getInt("qua");
				int pos = RS.getInt("pos");
				String stats = RS.getString("stats");
				World.addObjet(World.newObjet(guid, tempID, qua, pos, stats), false);
			}
			closeResultSet(RS);
		} catch (SQLException e) {
			Console.println("Game: SQL ERROR: " + e.getMessage(), Color.RED);
			Console.println("Requete: \n" + req);
			System.exit(1);
		}
	}

	public static void DELETE_ITEM(int guid) {
		String baseQuery = "DELETE FROM items WHERE guid = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, guid);

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
	}

	public static void SAVE_ITEM(Objects item) {
		String baseQuery = "REPLACE INTO `items` VALUES (?,?,?,?,?);";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, item.getGuid());
			p.setInt(2, item.getTemplate().getID());
			p.setInt(3, item.getQuantity());
			p.setInt(4, item.getPosition());
			p.setString(5, item.parseToSave());

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
	}

	public static void CREATE_MOUNT(Mount DD) {
		String baseQuery = "REPLACE INTO `mounts_data`(`id`,`color`,`sexe`,`name`,`xp`,`level`,"
				+ "`endurance`,`amour`,`maturite`,`serenite`,`reproductions`,`fatigue`,`items`,"
				+ "`ancetres`,`energie`, `ability`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, DD.get_id());
			p.setInt(2, DD.get_color());
			p.setInt(3, DD.get_sexe());
			p.setString(4, DD.get_nom());
			p.setLong(5, DD.get_exp());
			p.setInt(6, DD.get_level());
			p.setInt(7, DD.get_endurance());
			p.setInt(8, DD.get_amour());
			p.setInt(9, DD.get_maturite());
			p.setInt(10, DD.get_serenite());
			p.setInt(11, DD.get_reprod());
			p.setInt(12, DD.get_fatigue());
			p.setString(13, DD.parseObjDB());
			p.setString(14, DD.get_ancetres());
			p.setInt(15, DD.get_energie());
			p.setString(16, DD.get_ability());

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
	}

	public static void REMOVE_MOUNT(int DID) {
		String baseQuery = "DELETE FROM `mounts_data` WHERE `id` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, DID);

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
	}

	public static void LOAD_ACCOUNT_BY_IP(String ip) {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from accounts WHERE `lastIP` = '" + ip + "';",
					Config.DB_NAME);

			String baseQuery = "UPDATE accounts " + "SET `reload_needed` = 0 " + "WHERE guid = ?;";
			PreparedStatement p = newTransact(baseQuery, Connection());

			while (RS.next()) {
				// Si le compte est déjà connecté, on zap
				if (World.getCompte(RS.getInt("guid")) != null)
					if (World.getCompte(RS.getInt("guid")).isOnline())
						continue;

				Accounts C = new Accounts(RS.getInt("guid"), RS.getString("account").toLowerCase(),
						RS.getString("pass"), RS.getString("pseudo"), RS.getString("question"), RS.getString("reponse"),
						RS.getInt("level"), RS.getInt("subscription"), RS.getInt("vip"), (RS.getInt("banned") == 1),
						RS.getLong("banned_time"), RS.getString("lastIP"), RS.getString("lastConnectionDate"),
						RS.getString("bank"), RS.getInt("bankKamas"), RS.getString("friends"), RS.getString("enemy"),
						RS.getInt("cadeau"), RS.getLong("mute_time"), RS.getString("mute_raison"),
						RS.getString("mute_pseudo"));
				World.addAccount(C);
				World.ReassignAccountToChar(C);

				p.setInt(1, RS.getInt("guid"));
				p.executeUpdate();
			}

			closePreparedStatement(p);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_ACCOUNT_BY_GUID(int user) {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from accounts WHERE `guid` = '" + user + "';",
					Config.DB_NAME);

			String baseQuery = "UPDATE accounts " + "SET `reload_needed` = 0 " + "WHERE guid = ?;";
			PreparedStatement p = newTransact(baseQuery, Connection());

			while (RS.next()) {
				// Si le compte est déjà connecté, on zap
				if (World.getCompte(RS.getInt("guid")) != null)
					if (World.getCompte(RS.getInt("guid")).isOnline())
						continue;

				Accounts C = new Accounts(RS.getInt("guid"), RS.getString("account").toLowerCase(),
						RS.getString("pass"), RS.getString("pseudo"), RS.getString("question"), RS.getString("reponse"),
						RS.getInt("level"), RS.getInt("subscription"), RS.getInt("vip"), (RS.getInt("banned") == 1),
						RS.getLong("banned_time"), RS.getString("lastIP"), RS.getString("lastConnectionDate"),
						RS.getString("bank"), RS.getInt("bankKamas"), RS.getString("friends"), RS.getString("enemy"),
						RS.getInt("cadeau"), RS.getLong("mute_time"), RS.getString("mute_raison"),
						RS.getString("mute_pseudo"));
				World.addAccount(C);
				World.ReassignAccountToChar(C);

				p.setInt(1, RS.getInt("guid"));
				p.executeUpdate();
			}

			closePreparedStatement(p);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_ACCOUNT_BY_USER(String user) {
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from accounts WHERE `account` LIKE '" + user + "';",
					Config.DB_NAME);

			String baseQuery = "UPDATE accounts " + "SET `reload_needed` = 0 " + "WHERE guid = ?;";
			PreparedStatement p = newTransact(baseQuery, Connection());

			while (RS.next()) {
				// Si le compte est déjà connecté, on zap
				if (World.getCompte(RS.getInt("guid")) != null)
					if (World.getCompte(RS.getInt("guid")).isOnline())
						continue;

				Accounts C = new Accounts(RS.getInt("guid"), RS.getString("account").toLowerCase(),
						RS.getString("pass"), RS.getString("pseudo"), RS.getString("question"), RS.getString("reponse"),
						RS.getInt("level"), RS.getInt("subscription"), RS.getInt("vip"), (RS.getInt("banned") == 1),
						RS.getLong("banned_time"), RS.getString("lastIP"), RS.getString("lastConnectionDate"),
						RS.getString("bank"), RS.getInt("bankKamas"), RS.getString("friends"), RS.getString("enemy"),
						RS.getInt("cadeau"), RS.getLong("mute_time"), RS.getString("mute_raison"),
						RS.getString("mute_pseudo"));
				World.addAccount(C);
				World.ReassignAccountToChar(C);

				p.setInt(1, RS.getInt("guid"));
				p.executeUpdate();
			}

			closePreparedStatement(p);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void UPDATE_ACCOUNT_SUBSCRIBE(int guid, int SubScribe) {
		String baseQuery = "UPDATE accounts SET " + "`subscription` = ?" + " WHERE `guid` = ?;";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection);

			p.setInt(1, SubScribe);
			p.setInt(2, guid);

			p.executeUpdate();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void UPDATE_LASTCONNECTION_INFO(Accounts compte) {
		String baseQuery = "UPDATE accounts SET " + "`lastIP` = ?," + "`lastConnectionDate` = ?" + " WHERE `guid` = ?;";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection());

			p.setString(1, compte.get_curIP());
			p.setString(2, compte.getLastConnectionDate());
			p.setInt(3, compte.get_GUID());
			p.executeUpdate();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			Logs.addToRealmLog("Query: " + baseQuery);
			e.printStackTrace();
		}
	}

	public static void UPDATE_ACCOUNT_VIP(Accounts compte) {
		String baseQuery = "UPDATE accounts SET " + "`vip` = ?" + " WHERE `guid` = ?;";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection());

			p.setInt(1, compte.get_vip());
			p.setInt(2, compte.get_GUID());

			p.executeUpdate();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			Logs.addToRealmLog("Query: " + baseQuery);
			e.printStackTrace();
		}
	}

	public static void UPDATE_MOUNT_INFOS(Mount DD) {
		String baseQuery = "UPDATE mounts_data SET " + "`name` = ?," + "`xp` = ?," + "`level` = ?," + "`endurance` = ?,"
				+ "`amour` = ?," + "`maturite` = ?," + "`serenite` = ?," + "`reproductions` = ?," + "`fatigue` = ?,"
				+ "`energie` = ?," + "`ancetres` = ?," + "`items` = ?," + "`ability` = ?" + " WHERE `id` = ?;";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setString(1, DD.get_nom());
			p.setLong(2, DD.get_exp());
			p.setInt(3, DD.get_level());
			p.setInt(4, DD.get_endurance());
			p.setInt(5, DD.get_amour());
			p.setInt(6, DD.get_maturite());
			p.setInt(7, DD.get_serenite());
			p.setInt(8, DD.get_reprod());
			p.setInt(9, DD.get_fatigue());
			p.setInt(10, DD.get_energie());
			p.setString(11, DD.get_ancetres());
			p.setString(12, DD.parseObjDB());
			p.setString(13, DD.get_ability());
			p.setInt(14, DD.get_id());

			p.execute();
			closePreparedStatement(p);

		} catch (SQLException e) {
			Logs.addToGameLog("SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Query: " + baseQuery);
			e.printStackTrace();
		}
	}

	public static void SAVE_MOUNTPARK(MountPark MP) {
		String baseQuery = "REPLACE INTO `mountpark_data`( `mapid` , `cellid`, `size` , `owner` , `guild` , `price` , `data` )"
				+ " VALUES (?,?,?,?,?,?,?);";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, MP.get_map().get_id());
			p.setInt(2, MP.get_cellid());
			p.setInt(3, MP.get_size());
			p.setInt(4, MP.get_owner());
			p.setInt(5, (MP.get_guild() == null ? -1 : MP.get_guild().get_id()));
			p.setInt(6, MP.get_price());
			p.setString(7, MP.parseDBData());

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
	}

	public static void UPDATE_MOUNTPARK(MountPark MP) {
		String baseQuery = "UPDATE `mountpark_data` SET " + "`data` = ?" + " WHERE mapid = ?;";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setString(1, MP.parseDBData());
			p.setShort(2, MP.get_map().get_id());

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
	}

	public static boolean SAVE_TRIGGER(int mapID1, int cellID1, int action, int event, String args, String cond) {
		String baseQuery = "REPLACE INTO `scripted_cells`" + " VALUES (?,?,?,?,?,?);";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, mapID1);
			p.setInt(2, cellID1);
			p.setInt(3, action);
			p.setInt(4, event);
			p.setString(5, args);
			p.setString(6, cond);

			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
		return false;
	}

	public static boolean REMOVE_TRIGGER(int mapID, int cellID) {
		String baseQuery = "DELETE FROM `scripted_cells` WHERE " + "`MapID` = ? AND " + "`CellID` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, mapID);
			p.setInt(2, cellID);

			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
		return false;
	}

	public static boolean SAVE_MAP_DATA(Maps map) {
		String baseQuery = "UPDATE `maps` SET " + "`places` = ?, " + "`numgroup` = ? " + "WHERE id = ?;";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setString(1, map.get_placesStr());
			p.setInt(2, map.getMaxGroupNumb());
			p.setInt(3, map.get_id());

			p.executeUpdate();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
		return false;
	}

	public static boolean DELETE_NPC_ON_MAP(int m, int c) {
		String baseQuery = "DELETE FROM npcs WHERE mapid = ? AND cellid = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, m);
			p.setInt(2, c);

			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
		return false;
	}

	public static boolean DELETE_PERCO(int id) {
		String baseQuery = "DELETE FROM percepteurs WHERE guid = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, id);

			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
		return false;
	}

	public static boolean ADD_NPC_ON_MAP(int m, int id, int c, int o) {

		String baseQuery = "INSERT INTO `npcs`" + " VALUES (?,?,?,?);";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, m);
			p.setInt(2, id);
			p.setInt(3, c);
			p.setInt(4, o);

			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
		return false;
	}

	public static boolean ADD_PERCO_ON_MAP(int guid, int mapid, int guildID, int cellid, int o, short N1, short N2) {
		String baseQuery = "INSERT INTO `percepteurs`" + " VALUES (?,?,?,?,?,?,?,?,?,?);";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, guid);
			p.setInt(2, mapid);
			p.setInt(3, cellid);
			p.setInt(4, o);
			p.setInt(5, guildID);
			p.setShort(6, N1);
			p.setShort(7, N2);
			p.setString(8, "");
			p.setLong(9, 0);
			p.setLong(10, 0);
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
		return false;
	}

	public static void UPDATE_PERCO(Collector P) {
		String baseQuery = "UPDATE `percepteurs` SET " + "`objets` = ?," + "`kamas` = ?," + "`xp` = ?"
				+ " WHERE guid = ?;";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setString(1, P.parseItemPercepteur());
			p.setLong(2, P.getKamas());
			p.setLong(3, P.getXp());
			p.setInt(4, P.getGuid());

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
	}

	public static boolean ADD_ENDFIGHTACTION(int mapID, int type, int Aid, String args, String cond) {
		if (!DEL_ENDFIGHTACTION(mapID, type, Aid))
			return false;
		String baseQuery = "INSERT INTO `endfight_action` " + "VALUES (?,?,?,?,?);";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, mapID);
			p.setInt(2, type);
			p.setInt(3, Aid);
			p.setString(4, args);
			p.setString(5, cond);

			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
		return false;
	}

	public static boolean DEL_ENDFIGHTACTION(int mapID, int type, int aid) {
		String baseQuery = "DELETE FROM `endfight_action` " + "WHERE map = ? AND " + "fighttype = ? AND "
				+ "action = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, mapID);
			p.setInt(2, type);
			p.setInt(3, aid);

			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
			return false;
		}
	}

	public static void SAVE_NEWGUILD(Guild g) {
		String baseQuery = "INSERT INTO `guilds` " + "VALUES (?,?,?,1,0,0,0,?,?);";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, g.get_id());
			p.setString(2, g.get_name());
			p.setString(3, g.get_emblem());
			p.setString(4, "462;0|461;0|460;0|459;0|458;0|457;0|456;0|455;0|454;0|453;0|452;0|451;0|");
			p.setString(5, "176;100|158;1000|124;100|");

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
	}

	public static void DEL_GUILD(int id) {
		String baseQuery = "DELETE FROM `guilds` " + "WHERE `id` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, id);

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
	}

	public static void DEL_ALL_GUILDMEMBER(int guildid) {
		String baseQuery = "DELETE FROM `guild_members` " + "WHERE `guild` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, guildid);

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
	}

	public static void DEL_GUILDMEMBER(int id) {
		String baseQuery = "DELETE FROM `guild_members` " + "WHERE `guid` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, id);

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
	}

	public static void UPDATE_GUILD(Guild g) {
		String baseQuery = "UPDATE `guilds` SET " + "`lvl` = ?," + "`xp` = ?," + "`capital` = ?," + "`nbrmax` = ?,"
				+ "`sorts` = ?," + "`stats` = ?," + "`emblem` = ?" + " WHERE id = ?;";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, g.get_lvl());
			p.setLong(2, g.get_xp());
			p.setInt(3, g.get_Capital());
			p.setInt(4, g.get_nbrPerco());
			p.setString(5, g.compileSpell());
			p.setString(6, g.compileStats());
			p.setString(7, g.get_emblem());
			p.setInt(8, g.get_id());

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
	}

	public static void UPDATE_GUILDMEMBER(GuildMember gm) {
		String baseQuery = "REPLACE INTO `guild_members` " + "VALUES(?,?,?,?,?,?,?,?,?,?,?);";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, gm.getGuid());
			p.setInt(2, gm.getGuild().get_id());
			p.setString(3, gm.getName());
			p.setInt(4, gm.getLvl());
			p.setInt(5, gm.getGfx());
			p.setInt(6, gm.getRank());
			p.setLong(7, gm.getXpGave());
			p.setInt(8, gm.getPXpGive());
			p.setInt(9, gm.getRights());
			p.setInt(10, gm.getAlign());
			p.setString(11, gm.getLastCo());

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
	}

	public static int isPersoInGuild(int guid) {
		int guildId = -1;

		try {
			ResultSet GuildQuery = SQLManager.executeQuery("SELECT guild FROM `guild_members` WHERE guid=" + guid + ";",
					Config.DB_NAME);

			boolean found = GuildQuery.first();

			if (found)
				guildId = GuildQuery.getInt("guild");

			closeResultSet(GuildQuery);
		} catch (SQLException e) {
			Logs.addToGameLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}

		return guildId;
	}

	public static int[] isPersoInGuild(String name) {
		int guildId = -1;
		int guid = -1;
		try {
			ResultSet GuildQuery = SQLManager
					.executeQuery("SELECT guild,guid FROM `guild_members` WHERE name='" + name + "';", Config.DB_NAME);
			boolean found = GuildQuery.first();

			if (found) {
				guildId = GuildQuery.getInt("guild");
				guid = GuildQuery.getInt("guid");
			}

			closeResultSet(GuildQuery);
		} catch (SQLException e) {
			Logs.addToGameLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
		int[] toReturn = { guid, guildId };
		return toReturn;
	}

	public static boolean ADD_REPONSEACTION(int repID, int type, String args) {
		String baseQuery = "DELETE FROM `npc_reponses_actions` " + "WHERE `ID` = ? AND " + "`type` = ?;";
		PreparedStatement p;
		try {
			p = newTransact(baseQuery, Connection());
			p.setInt(1, repID);
			p.setInt(2, type);

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
		baseQuery = "INSERT INTO `npc_reponses_actions` " + "VALUES (?,?,?);";
		try {
			p = newTransact(baseQuery, Connection());
			p.setInt(1, repID);
			p.setInt(2, type);
			p.setString(3, args);

			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
		return false;
	}

	public static boolean UPDATE_INITQUESTION(int id, int q) {
		String baseQuery = "UPDATE `npc_template` SET " + "`initQuestion` = ? " + "WHERE `id` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, q);
			p.setInt(2, id);

			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
		return false;
	}

	public static boolean UPDATE_NPCREPONSES(int id, String reps) {
		String baseQuery = "UPDATE `npc_questions` SET " + "`responses` = ? " + "WHERE `ID` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setString(1, reps);
			p.setInt(2, id);

			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
		return false;
	}

	public static void LOAD_ACTION() {
		/* Variables représentant les champs de la base */
		Characters perso;
		int action;
		int nombre;
		int id;
		String sortie;
		String couleur = "DF0101"; // La couleur du message envoyer a l'utilisateur (couleur en code HTML)
		ObjTemplate t;
		Objects obj;
		PreparedStatement p;
		/* FIN */
		try {
			Logs.addToSQLLog("Lancement de l'application des Lives Actions");
			ResultSet RS = executeQuery("SELECT * from live_action;", Config.DB_NAME);
			while (RS.next()) {
				perso = World.getPersonnage(RS.getInt("PlayerID"));
				if (perso == null) {
					Logs.addToSQLLog("Personnage " + RS.getInt("PlayerID") + " non trouve, personnage non charge ?");
					continue;
				}
				if (!perso.isOnline()) {
					Logs.addToSQLLog("Personnage " + RS.getInt("PlayerID") + " hors ligne");
					continue;
				}
				if (perso.get_compte() == null) {
					Logs.addToSQLLog("Le Personnage " + RS.getInt("PlayerID") + " n'est attribue a aucun compte charge");
					continue;
				}
				if (perso.get_compte().getGameThread() == null) {
					Logs.addToSQLLog("Le Personnage " + RS.getInt("PlayerID") + " n'a pas de thread associe, le personnage est il hors ligne ?");
					continue;
				}
				if (perso.get_fight() != null)
					continue;
				action = RS.getInt("Action");
				nombre = RS.getInt("Nombre");
				id = RS.getInt("ID");
				sortie = "+";

				switch (action) {
				case 1: // Monter d'un level
					if (perso.get_lvl() == World.getExpLevelSize())
						continue;
					for (int n = nombre; n > 1; n--)
						perso.levelUp(false, true);
					perso.levelUp(true, true);
					sortie += nombre + " Niveau(x)";
					break;
				case 2: // Ajouter X point d'experience
					if (perso.get_lvl() == World.getExpLevelSize())
						continue;
					perso.addXp(nombre);
					sortie += nombre + " Xp";
					break;
				case 3: // Ajouter X kamas
					perso.addKamas(nombre);
					sortie += nombre + " Kamas";
					break;
				case 4: // Ajouter X point de capital
					perso.addCapital(nombre);
					sortie += nombre + " Point(s) de capital";
					break;
				case 5: // Ajouter X point de sort
					perso.addSpellPoint(nombre);
					sortie += nombre + " Point(s) de sort";
					break;
				case 20: // Ajouter un item avec des jets aléatoire
					t = World.getObjTemplate(nombre);
					if (t == null)
						continue;
					obj = t.createNewItem(1, false, -1); // Si mis à "true"
															// l'objet à des
															// jets max. Sinon
															// ce sont des jets
															// aléatoire
					if (obj == null)
						continue;
					if (perso.addObjet(obj, true))// Si le joueur n'avait pas
													// d'item similaire
						World.addObjet(obj, true);
					Logs.addToGameSockLog("Objet " + nombre + " ajouter a " + perso.get_name() + " avec des stats aleatoire");
					SocketManager.GAME_SEND_MESSAGE(perso,
							"L'objet \"" + t.getName() + "\" vient d'etre ajouter a votre personnage", couleur);
					break;
				case 21: // Ajouter un item avec des jets MAX
					t = World.getObjTemplate(nombre);
					if (t == null)
						continue;
					obj = t.createNewItem(1, true, -1); // Si mis à "true" l'objet à des jets max. Sinon ce sont des jets aléatoire
					if (obj == null)
						continue;
					if (perso.addObjet(obj, true))// Si le joueur n'avait pas d'item similaire
						World.addObjet(obj, true);
					Logs.addToGameSockLog("Objet " + t.getName() + " (" + nombre +") ajoute a " + perso.get_name() + " avec des stats MAX");
					SocketManager.GAME_SEND_MESSAGE(perso, "L'objet \"" + t.getName()
							+ "\" avec des stats maximum, vient d'etre ajoute a votre personnage", couleur);
					break;
				case 118:// Force
					perso.get_baseStats().addOneStat(action, nombre);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					sortie += nombre + " force";
					break;
				case 119:// Agilite
					perso.get_baseStats().addOneStat(action, nombre);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					sortie += nombre + " agilite";
					break;
				case 123:// Chance
					perso.get_baseStats().addOneStat(action, nombre);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					sortie += nombre + " chance";
					break;
				case 124:// Sagesse
					perso.get_baseStats().addOneStat(action, nombre);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					sortie += nombre + " sagesse";
					break;
				case 125:// Vita
					perso.get_baseStats().addOneStat(action, nombre);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					sortie += nombre + " vita";
					break;
				case 126:// Intelligence
					int statID = action;
					perso.get_baseStats().addOneStat(statID, nombre);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					sortie += nombre + " intelligence";
					break;
				}
				SocketManager.GAME_SEND_STATS_PACKET(perso);
				if (action != 20 && action != 21)
					SocketManager.GAME_SEND_MESSAGE(perso, sortie + " a votre personnage", couleur); // Si l'action n'est pas un ajout d'objet on envoye un message a l'utilisateur

				Logs.addToSQLLog("(Commande " + id + ")Action " + action + " Nombre: " + nombre
						+ " appliquee sur le personnage " + RS.getInt("PlayerID") + "(" + perso.get_name() + ")");
				try {
					String query = "DELETE FROM live_action WHERE ID=" + id + ";";
					p = newTransact(query, Connection());
					p.execute();
					closePreparedStatement(p);
					Logs.addToSQLLog("Commande " + id + " supprimee.");
				} catch (SQLException e) {
					Logs.addToSQLLog("SQL ERROR: " + e.getMessage());
					e.printStackTrace();
				}
				SQLManager.SAVE_PERSONNAGE(perso, true);
			}
			closeResultSet(RS);
			Logs.addToSQLLog("Application des Lives Actions effectue avec succes");
		} catch (Exception e) {
			Logs.addToGameLog("ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOG_OUT(int accID, int logged) {
		PreparedStatement p;
		String query = "UPDATE `accounts` SET logged=? WHERE `guid`=?;";
		try {
			p = newTransact(query, Connection());
			p.setInt(1, logged);
			p.setInt(2, accID);

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + query);
		}
	}

	public static void SETONLINE(int accID) {
		String query = "UPDATE `accounts` SET logged='1' WHERE `guid`=" + accID + ";";
		try {
			PreparedStatement p = newTransact(query, Connection());
			p.execute();
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + query);
		}
	}

	public static void SETOFFLINE(int accID) {
		String query = "UPDATE `accounts` SET logged='0' WHERE `guid`=" + accID + ";";
		try {
			PreparedStatement p = newTransact(query, Connection());
			p.execute();
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + query);
		}
	}

	public static void LOGGED_ZERO() {
		Console.print("[...] reset logged", Color.YELLOW);
		PreparedStatement p;
		String query = "UPDATE `accounts` SET logged=0;";
		try {
			p = newTransact(query, Connection());

			p.execute();
			closePreparedStatement(p);

			Console.print("\r[OK] reset logged ", Color.GREEN);
		} catch (SQLException e) {
			Logs.addToSQLLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToSQLLog("Game: Query: " + query);
			System.exit(0);
		}

	}

	public static void LOAD_ITEMS_FULL() {
		try {
			ResultSet RS = executeQuery("SELECT * FROM items;", Config.DB_NAME);
			Console.print("[...] loading items", Color.YELLOW);
			while (RS.next()) {
				int guid = RS.getInt("guid");
				int tempID = RS.getInt("template");
				int qua = RS.getInt("qua");
				int pos = RS.getInt("pos");
				String stats = RS.getString("stats");
				World.addObjet(new Objects(guid, tempID, qua, pos, stats), false);
			}
			Console.print("\r[OK] loading items ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("Game: SQL ERROR: " + e.getMessage());
			System.exit(1);
		}
	}

	public static void TIMER(boolean start) {
		if (start) {
			timerCommit = new Timer();
			timerCommit.schedule(new TimerTask() {

				public void run() {
					if (!needCommit)
						return;

					commitTransacts();
					needCommit = false;

				}
			}, Config.CONFIG_DB_COMMIT, Config.CONFIG_DB_COMMIT);
		} else
			timerCommit.cancel();
	}

	public static boolean persoExist(String name) {
		boolean exist = false;
		PreparedStatement p;
		String query = "SELECT COUNT(*) AS exist FROM personnages WHERE name LIKE ?;";
		try {
			p = newTransact(query, Connection());
			p.setString(1, name);
			ResultSet RS = p.executeQuery();

			boolean found = RS.first();

			if (found) {
				if (RS.getInt("exist") != 0)
					exist = true;
			}

			closeResultSet(RS);
			//closePreparedStatement(p);
		} catch (SQLException e) {
			 //FIXME : Changer la requête flood Log Error..
			 Logs.addToSQLLog("SQL ERROR: "+e.getMessage());
			 e.printStackTrace();
		}
		return exist;
	}

	public static void HOUSE_BUY(Characters P, Houses h) {

		PreparedStatement p;
		String query = "UPDATE `houses` SET `sale`='0', `owner_id`=?, `guild_id`='0', `access`='0', `key`='-', `guild_rights`='0' WHERE `id`=?;";
		try {
			p = newTransact(query, Connection());
			p.setInt(1, P.getAccID());
			p.setInt(2, h.get_id());

			p.execute();
			closePreparedStatement(p);

			h.set_sale(0);
			h.set_owner_id(P.getAccID());
			h.set_guild_id(0);
			h.set_access(0);
			h.set_key("-");
			h.set_guild_rights(0);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + query);
		}

		ArrayList<Trunk> trunks = Trunk.getTrunksByHouse(h);
		for (Trunk trunk : trunks) {
			trunk.set_owner_id(P.getAccID());
			trunk.set_key("-");
		}

		query = "UPDATE `coffres` SET `owner_id`=?, `key`='-' WHERE `id_house`=?;";
		try {
			p = newTransact(query, Connection());
			p.setInt(1, P.getAccID());
			p.setInt(2, h.get_id());
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + query);
		}
	}

	public static void HOUSE_SELL(Houses h, int price) {
		h.set_sale(price);

		PreparedStatement p;
		String query = "UPDATE `houses` SET `sale`=? WHERE `id`=?;";
		try {
			p = newTransact(query, Connection());
			p.setInt(1, price);
			p.setInt(2, h.get_id());

			p.execute();
			closePreparedStatement(p);

		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + query);
		}
	}

	public static void HOUSE_CODE(Characters P, Houses h, String packet) {
		PreparedStatement p;
		String query = "UPDATE `houses` SET `key`=? WHERE `id`=? AND owner_id=?;";
		try {
			p = newTransact(query, Connection());
			p.setString(1, packet);
			p.setInt(2, h.get_id());
			p.setInt(3, P.getAccID());

			p.execute();
			closePreparedStatement(p);

			h.set_key(packet);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + query);
		}
	}

	public static void HOUSE_GUILD(Houses h, int GuildID, int GuildRights) {
		PreparedStatement p;
		String query = "UPDATE `houses` SET `guild_id`=?, `guild_rights`=? WHERE `id`=?;";
		try {
			p = newTransact(query, Connection());
			p.setInt(1, GuildID);
			p.setInt(2, GuildRights);
			p.setInt(3, h.get_id());

			p.execute();
			closePreparedStatement(p);

			h.set_guild_id(GuildID);
			h.set_guild_rights(GuildRights);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + query);
		}
	}

	public static void HOUSE_GUILD_REMOVE(int GuildID) {
		PreparedStatement p;
		String query = "UPDATE `houses` SET `guild_rights`='0', `guild_id`='0' WHERE `guild_id`=?;";
		try {
			p = newTransact(query, Connection());
			p.setInt(1, GuildID);

			p.execute();
			closePreparedStatement(p);

		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + query);
		}
	}

	public static void UPDATE_HOUSE(Houses h) {
		String baseQuery = "UPDATE `houses` SET " + "`owner_id` = ?," + "`sale` = ?," + "`guild_id` = ?,"
				+ "`access` = ?," + "`key` = ?," + "`guild_rights` = ?" + " WHERE id = ?;";

		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, h.get_owner_id());
			p.setInt(2, h.get_sale());
			p.setInt(3, h.get_guild_id());
			p.setInt(4, h.get_access());
			p.setString(5, h.get_key());
			p.setInt(6, h.get_guild_rights());
			p.setInt(7, h.get_id());

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
	}

	public static int GetNewIDPercepteur() {
		int i = -50;// Pour éviter les conflits avec touts autre NPC
		try {
			String query = "SELECT `guid` FROM `percepteurs` ORDER BY `guid` ASC LIMIT 0 , 1;";

			ResultSet RS = executeQuery(query, Config.DB_NAME);
			while (RS.next()) {
				i = RS.getInt("guid") - 1;
			}

			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
		return i;
	}

	public static void LOAD_ZAAPIS() {
		try {
			String Bonta = "";
			String Brak = "";
			String Neutre = "";
			ResultSet RS = SQLManager.executeQuery("SELECT mapid, align from zaapi;", Config.DB_NAME);
			Console.print("[...] loading zaapis", Color.YELLOW);
			while (RS.next()) {
				if (RS.getInt("align") == Constant.ALIGNEMENT_BONTARIEN) {
					Bonta += RS.getString("mapid");
					if (!RS.isLast())
						Bonta += ",";
				} else if (RS.getInt("align") == Constant.ALIGNEMENT_BRAKMARIEN) {
					Brak += RS.getString("mapid");
					if (!RS.isLast())
						Brak += ",";
				} else {
					Neutre += RS.getString("mapid");
					if (!RS.isLast())
						Neutre += ",";
				}
			}
			Constant.ZAAPI.put(Constant.ALIGNEMENT_BONTARIEN, Bonta);
			Constant.ZAAPI.put(Constant.ALIGNEMENT_BRAKMARIEN, Brak);
			Constant.ZAAPI.put(Constant.ALIGNEMENT_NEUTRE, Neutre);
			Console.print("\r[OK] loading zaapis ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static int LOAD_ZAAPS() {
		int nbr = 0;
		try {
			Console.print("[...] loading zaaps", Color.YELLOW);
			ResultSet RS = SQLManager.executeQuery("SELECT mapID, cellID from zaaps;", Config.DB_NAME);
			while (RS.next()) {
				Constant.ZAAPS.put(RS.getInt("mapID"), RS.getInt("cellID"));
			}
			Console.print("\r[OK] loading zaaps ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
			nbr = 0;
		}
		return nbr;
	}

	public static int getNextObjetID() {
		try {
			ResultSet RS = executeQuery("SELECT MAX(guid) AS max FROM items;", Config.DB_NAME);

			int guid = 0;
			boolean found = RS.first();

			if (found)
				guid = RS.getInt("max");

			closeResultSet(RS);
			return guid;
		} catch (SQLException e) {
			Logs.addToRealmLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}
		return 0;
	}

	public static void LOAD_BANIP() {
		try {
			Console.print("[...] loading banip", Color.YELLOW);
			ResultSet RS = executeQuery("SELECT ip, time from banip;", Config.DB_NAME);
			while (RS.next()) {
				World.addBanip(RS.getString("ip"), RS.getLong("time"));
			}
			Console.print("\r[OK] loading banip ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static boolean INSERT_DEBUG_LOG(String character_name, String account_name, int account_gm, short mapid,
			int cellid) {
		String baseQuery = "INSERT INTO `debug_logs`" + " VALUES (?,?,?,?,?);";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setString(1, character_name);
			p.setString(2, account_name);
			p.setInt(3, account_gm);
			p.setInt(4, mapid);
			p.setInt(5, cellid);
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
		return false;
	}

	public static boolean ADD_BANIP(String ip, long time) {
		String baseQuery = "INSERT INTO `banip`" + " VALUES (?,?);";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setString(1, ip);
			p.setLong(2, time);
			p.execute();
			closePreparedStatement(p);
			return true;
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
		return false;
	}

	public static void REMOVE_BANIP(String ip) {
		String baseQuery = "DELETE FROM `banip` WHERE ip LIKE ?";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setString(1, ip);
			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + baseQuery);
		}
	}

	public static void LOAD_QUESTS() {
		try {
			Console.print("[...] loading quests", Color.YELLOW);
			ResultSet RS = executeQuery("SELECT * FROM `quests`", Config.DB_NAME);
			while (RS.next()) {
				World.addQuest(RS.getInt("id"), RS.getString("steps"), RS.getInt("startQuestion"),
						RS.getInt("endQuestion"), RS.getInt("minLvl"), RS.getInt("questRequired"));
			}
			Console.print("\r[OK] loading quests ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("Game: SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_QUEST_STEPS() {
		try {
			Console.print("[...] loading quests steps", Color.YELLOW);
			ResultSet RS = executeQuery("SELECT * FROM `quest_steps`", Config.DB_NAME);
			while (RS.next()) {
				World.addQuestStep(RS.getInt("id"), RS.getString("objectives"), RS.getInt("question"),
						RS.getInt("gainExp"), RS.getInt("gainKamas"), RS.getString("gainItems"));
			}
			Console.print("\r[OK] loading quests steps ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("Game: SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_QUEST_OBJECTIVES() {
		try {
			Console.print("[...] loading quests objectives", Color.YELLOW);
			ResultSet RS = executeQuery("SELECT * FROM `quest_objectives`", Config.DB_NAME);
			while (RS.next()) {
				World.addQuestObjective(RS.getInt("id"), RS.getString("type"), RS.getString("args"),
						RS.getInt("optNpcTarget"), RS.getInt("optQuestion"), RS.getInt("optAnswer"));
			}
			Console.print("\r[OK] loading quests objectives ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("Game: SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_HDVS() {
		try {
			ResultSet RS = executeQuery("SELECT * FROM `hdvs` ORDER BY id ASC", Config.DB_NAME);
			Console.print("[...] loading hdvs", Color.YELLOW);
			while (RS.next()) {
				World.addHdv(new AuctionHouse(RS.getInt("map"), RS.getFloat("sellTaxe"), RS.getShort("sellTime"),
						RS.getShort("accountItem"), RS.getShort("lvlMax"), RS.getString("categories")));
			}
			RS = executeQuery("SELECT id MAX FROM `hdvs`", Config.DB_NAME);
			RS.first();
			World.setNextHdvID(RS.getInt("MAX"));
			Console.print("\r[OK] loading hdvs ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("Game: SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_HDVS_ITEMS() {
		try {
			Console.print("[...] loading hdvs items", Color.YELLOW);
			ResultSet RS = executeQuery(
					"SELECT i.*" + " FROM `items` AS i,`hdvs_items` AS h" + " WHERE i.guid = h.itemID", Config.DB_NAME);

			// Load items
			while (RS.next()) {
				int guid = RS.getInt("guid");
				int tempID = RS.getInt("template");
				int qua = RS.getInt("qua");
				int pos = RS.getInt("pos");
				String stats = RS.getString("stats");
				World.addObjet(World.newObjet(guid, tempID, qua, pos, stats), false);
			}
			Console.println("\r[OK] loading hdvs items ", Color.GREEN);
			// Load HDV entry
			Console.print("[...] loading hdvs items entry", Color.YELLOW);
			RS = executeQuery("SELECT * FROM `hdvs_items`", Config.DB_NAME);
			while (RS.next()) {
				AuctionHouse tempHdv = World.getHdv(RS.getInt("map"));
				if (tempHdv == null)
					continue;

				tempHdv.addEntry(new AuctionHouse.HdvEntry(RS.getInt("price"), RS.getByte("count"),
						RS.getInt("ownerGuid"), World.getObjet(RS.getInt("itemID"))));
			}
			Console.print("\r[OK] loading hdvs items entry ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("Game: SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void SAVE_HDVS_ITEMS(ArrayList<HdvEntry> liste) {
		PreparedStatement queries = null;
		try {
			String emptyQuery = "TRUNCATE TABLE `hdvs_items`";
			PreparedStatement emptyTable = newTransact(emptyQuery, Connection());
			emptyTable.execute();
			closePreparedStatement(emptyTable);

			String baseQuery = "INSERT INTO `hdvs_items` " + "(`map`,`ownerGuid`,`price`,`count`,`itemID`) "
					+ "VALUES(?,?,?,?,?);";
			queries = newTransact(baseQuery, Connection());
			for (HdvEntry curEntry : liste) {

				if (curEntry.getOwner() == -1)
					continue;
				queries.setInt(1, curEntry.getHdvID());
				queries.setInt(2, curEntry.getOwner());
				queries.setInt(3, curEntry.getPrice());
				queries.setInt(4, curEntry.getAmount(false));
				queries.setInt(5, curEntry.getObjet().getGuid());

				queries.execute();
			}
			closePreparedStatement(queries);
			SAVE_HDV_AVGPRICE();
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void SAVE_HDV_AVGPRICE() {
		String baseQuery = "UPDATE `item_template`" + " SET sold = ?,avgPrice = ?" + " WHERE id = ?;";

		PreparedStatement queries = null;

		try {
			queries = newTransact(baseQuery, Connection());

			for (ObjTemplate curTemp : World.getObjTemplates()) {
				if (curTemp.getSold() == 0)
					continue;

				queries.setLong(1, curTemp.getSold());
				queries.setInt(2, curTemp.getAvgPrice());
				queries.setInt(3, curTemp.getID());
				queries.executeUpdate();
			}
			closePreparedStatement(queries);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_ANIMATIONS() {
		try {
			ResultSet RS = executeQuery("SELECT * from animations;", Config.DB_NAME);
			Console.print("[...] loading animations", Color.YELLOW);
			while (RS.next()) {
				World.addAnimation(new Hustle(RS.getInt("guid"), RS.getInt("id"), RS.getString("nom"),
						RS.getInt("area"), RS.getInt("action"), RS.getInt("size")));
			}
			Console.print("\r[OK] loading animations ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void LOAD_COMMANDS() {
		try {
			ResultSet RS = executeQuery("SELECT * from commands_player;", Config.DB_NAME);
			Console.print("[...] loading player commands", Color.YELLOW);
			while (RS.next()) {
				String name = null;
				String description = null;

				if (RS.getString("name").contains("-")) {
					description = RS.getString("name").split("-")[1];
					name = RS.getString("name").split("-")[0];
				} else {
					name = RS.getString("name");
				}
				PlayerCommandManager command = new PlayerCommandManager(RS.getInt("id"), name, description,
						RS.getString("functions"), RS.getString("conditions"), RS.getInt("count"));
				PlayerCommandManager.addCommand(command);
			}
			Console.print("\r[OK] loading player commands ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e);
			e.printStackTrace();
		}
	}
	
	public static void LOAD_GMCOMMANDS() {
		try {
			ResultSet RS = executeQuery("SELECT * from commands_staff;", Config.DB_NAME);
			Console.print("[...] loading staff commands", Color.YELLOW);
			while (RS.next()) {
				GmCommandManager command = new GmCommandManager(RS.getInt("id"), RS.getString("name"), RS.getInt("gmlvl"),
						RS.getInt("function"), RS.getString("description"), RS.getString("args"));
				GmCommandManager.addCommand(command);
			}
			Console.print("\r[OK] loading staff commands ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e);
			e.printStackTrace();
		}
	}

	public static void LOAD_RAPIDSTUFFS() {
		try {
			ResultSet RS = executeQuery("SELECT * from rapidstuff;", Config.DB_NAME);
			Console.print("[...] loading rapidstuff", Color.YELLOW);
			while (RS.next()) {
				RapidStuff.rapidStuffs.put(RS.getInt("id"), new RapidStuff(RS.getInt("id"), RS.getString("name"),
						RS.getString("items"), RS.getInt("owner")));
			}
			Console.print("\r[OK] loading rapidstuff ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("Game: SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void LOAD_ARENA_TEAM() {
		try {
			Console.print("[...] loading arena teams", Color.YELLOW);
			String query = "SELECT * from arena;";
			ResultSet RS = SQLManager.executeQuery(query, Config.DB_NAME);
			while (RS.next()) {
				Team x = new Team(RS.getInt("id"), RS.getString("name"), RS.getString("players"), RS.getInt("quote"),
						RS.getInt("Rank"));
				Team.addTeamToMap(x);
			}
			Console.print("\r[OK] loading arena teams ", Color.GREEN);
			SQLManager.closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e);
			e.printStackTrace();
		}
	}

	public static int LOAD_TRUNK() {
		int nbr = 0;
		try {
			ResultSet RS = SQLManager.executeQuery("SELECT * from coffres;", Config.DB_NAME);
			Console.print("[...] loading trunks", Color.YELLOW);
			while (RS.next()) {
				World.addTrunk(new Trunk(RS.getInt("id"), RS.getInt("id_house"), RS.getShort("mapid"),
						RS.getInt("cellid"), RS.getString("object"), RS.getInt("kamas"), RS.getString("key"),
						RS.getInt("owner_id")));
			}
			Console.print("\r[OK] loading trunks ", Color.GREEN);
			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
			nbr = 0;
		}
		return nbr;
	}

	public static void TRUNK_CODE(Characters P, Trunk t, String packet) {
		PreparedStatement p;
		String query = "UPDATE `coffres` SET `key`=? WHERE `id`=? AND owner_id=?;";
		try {
			p = newTransact(query, Connection());
			p.setString(1, packet);
			p.setInt(2, t.get_id());
			p.setInt(3, P.getAccID());

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + query);
		}
	}

	public static void UPDATE_TRUNK(Trunk t) {
		PreparedStatement p;
		String query = "UPDATE `coffres` SET `kamas`=?, `object`=? WHERE `id`=?";

		try {
			p = newTransact(query, Connection());
			p.setLong(1, t.get_kamas());
			p.setString(2, t.parseTrunkObjetsToDB());
			p.setInt(3, t.get_id());

			p.execute();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToGameLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToGameLog("Game: Query: " + query);
		}
	}

	public static void UPDATE_GIFT(Accounts compte) {
		String baseQuery = "UPDATE accounts SET `cadeau` = 0 WHERE `guid` = ?;";
		try {
			PreparedStatement p = newTransact(baseQuery, Connection());
			p.setInt(1, compte.get_GUID());
			p.executeUpdate();
			closePreparedStatement(p);
		} catch (SQLException e) {
			Logs.addToSQLLog("SQL ERROR: " + e.getMessage());
			Logs.addToSQLLog("Query: " + baseQuery);
			e.printStackTrace();
		}
	}

	public static boolean ACCOUNT_IS_VIP(Accounts compte) {
		try {
			java.sql.PreparedStatement p = Connection().prepareStatement("SELECT * FROM accounts WHERE guid = ?");
			p.setInt(1, compte.get_GUID());
			ResultSet RS = p.executeQuery();

			while (RS.next()) {
				return (RS.getInt("vip") == 1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void SAVE_PERSONNAGE_COLORS(Characters _perso) {
		String baseQuery = "UPDATE `personnages` SET " + "`name`= ?, " + "`color1`= ?, " + "`color2`= ?, "
				+ "`color3`= ?" + " WHERE `personnages`.`guid` = ? LIMIT 1 ;";

		PreparedStatement p = null;

		try {
			p = newTransact(baseQuery, Connection());

			p.setString(1, _perso.get_name());
			p.setInt(2, _perso.get_color1());
			p.setInt(3, _perso.get_color2());
			p.setInt(4, _perso.get_color3());
			p.setInt(5, _perso.get_GUID());

			p.executeUpdate();

			Logs.addToSQLLog("Personnage " + _perso.get_name() + " sauvegarde");
		} catch (Exception e) {
			Logs.addToSQLLog("Game: SQL ERROR: " + e.getMessage());
			Logs.addToSQLLog("Requete: " + baseQuery);
			Logs.addToSQLLog("Le personnage " + _perso.get_name() +" n'a pas ete sauvegarde");
			System.exit(1);
		}
		;
		closePreparedStatement(p);
	}

	public static byte TotalMPGuild(int getId) {
		byte i = 0;
		try {
			String query = "SELECT *FROM mountpark_data WHERE guild='" + getId + "';";

			ResultSet RS = executeQuery(query, Config.DB_NAME);
			while (RS.next()) {
				i = (byte) (i + 1);
			}

			closeResultSet(RS);
		} catch (SQLException e) {
			Logs.addToGameLog("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		}
		return i;
	}
	
	public static String getStaffOnline(){
		StringBuilder staff = new StringBuilder();
		try {
		    ResultSet RS = SQLManager.executeQuery("SELECT pseudo from accounts WHERE logged = 1 AND level > 0", Config.DB_NAME);
		    while (RS.next()) {
		        staff.append("- ").append(RS.getString("pseudo")).append("\n");
		    }
		    RS.close();
		    return staff.toString();
		} catch (SQLException e) {
		    Logs.addToSQLLog("SQL Error getStaffOnline: " + e.getMessage());
		}
		return "Pas de staff en ligne";
	}
}
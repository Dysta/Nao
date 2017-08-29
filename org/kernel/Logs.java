package org.kernel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;

import org.kernel.Console.Color;

public class Logs {
	
	public static PrintStream PS;
	public static BufferedWriter Log_GameSock;
	public static BufferedWriter Log_Game;
	public static BufferedWriter Log_Realm;
	public static BufferedWriter Log_IpCheck;
	public static BufferedWriter Log_MJ;
	public static BufferedWriter Log_RealmSock;
	public static BufferedWriter Log_Shop;
	public static BufferedWriter Log_Debug;
	public static BufferedWriter Log_Chat;
	public static BufferedWriter Log_FM;
	public static BufferedWriter Log_SQL;
	public static BufferedWriter Log_All;
	
	public static boolean createLog(){
		try {
			String date = Calendar.getInstance().get(Calendar.YEAR) + "-"
					+ (Calendar.getInstance().get(Calendar.MONTH) + 1) + "-"
					+ Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

			new File("Logs").mkdir();
			new File("Logs/" + "Game_logs").mkdir();
			new File("Logs/" + "Realm_logs").mkdir();
			new File("Logs/" + "Shop_logs").mkdir();
			new File("Logs/" + "Error_logs").mkdir();
			new File("Logs/" + "IpCheck_logs").mkdir();
			new File("Logs/" + "FM_logs").mkdir();
			new File("Logs/" + "Thread_logs").mkdir();
			new File("Logs/" + "Gms_logs").mkdir();
			new File("Logs/" + "Chat_logs").mkdir();
			new File("Logs/" + "SQL_logs").mkdir();
			new File("Logs/" + "All_Logs").mkdirs();
			Log_FM = new BufferedWriter(new FileWriter("Logs/" + "FM_logs/" + date + ".txt", true));
			Log_Realm = new BufferedWriter(new FileWriter("Logs/" + "Realm_logs/" + date + ".txt", true));
			Log_IpCheck = new BufferedWriter(new FileWriter("Logs/" + "IpCheck_logs/" + date + ".txt", true));
			Log_RealmSock = new BufferedWriter(new FileWriter("Logs/" + "Realm_logs/" + date + "_packets.txt", true));
			Log_Shop = new BufferedWriter(new FileWriter("Logs/" + "Shop_logs/" + date + ".txt", true));
			Log_GameSock = new BufferedWriter(new FileWriter("Logs/" + "Game_logs/" + date + "_packets.txt", true));
			Log_Game = new BufferedWriter(new FileWriter("Logs/" + "Game_logs/" + date + ".txt", true));
			Log_All = new BufferedWriter(new FileWriter("Logs/" + "All_Logs/" + date + ".txt", true));

			if ("Logs/".isEmpty()) {
				Log_MJ = new BufferedWriter(new FileWriter("Logs/" + "Gms_logs/" + date + "_GM.txt", true));
				Log_Debug = new BufferedWriter(new FileWriter("Logs/" + "Thread_logs/" + date + "_Thread.txt", true));
				Log_Chat = new BufferedWriter(new FileWriter("Logs/" + "Chat_logs/" + date + "_Chat.txt", true));
				Log_SQL = new BufferedWriter(new FileWriter("Logs/" + "SQL_logs/" + date + "_SQL.txt", true));
				String nom = "Logs/" + "Error_logs/" + date + "_error.txt";
				int i = 0;
				while (new File(nom).exists()) {
					nom = "Logs/" + "Error_logs/" + date + "_error_" + i + ".txt";
					i++;
				}
				PS = new PrintStream(new File(nom));
			} else {
				Log_MJ = new BufferedWriter(new FileWriter("Logs" + "/Gms_logs/" + date + "_GM.txt", true));
				Log_Debug = new BufferedWriter(new FileWriter("Logs" + "/Thread_logs/" + date + "_Thread.txt", true));
				Log_Chat = new BufferedWriter(new FileWriter("Logs" + "/Chat_logs/" + date + "_Chat.txt", true));
				Log_SQL = new BufferedWriter(new FileWriter("Logs/" + "SQL_logs/" + date + "_SQL.txt", true));
				
				String nom = "Logs" + "/Error_logs/" + date + "_error.txt";
				int i = 0;
				while (new File(nom).exists()) {
					nom = "Logs" + "/Error_logs/" + date + "_error_" + i + ".txt";
					i++;
				}
				PS = new PrintStream(new File(nom));
			}
			System.setErr(PS);
			PS.flush();
			Log_GameSock.flush();
			Log_Game.flush();
			Log_MJ.flush();
			Log_Realm.flush();
			Log_Shop.flush();
			Log_FM.flush();
			Log_SQL.flush();
			Log_Chat.flush();
			Log_Debug.newLine();
			Log_Debug.flush();
			Log_IpCheck.flush();
			Log_RealmSock.flush();
			Log_All.flush();
		} catch (IOException e) {
			Console.println(e.getMessage(), Color.RED);
			System.exit(0);
			return false;
		}
		return true;
	}
	
	public synchronized static void addToHistoricLog(String str) {
		try {
			String date = Calendar.HOUR_OF_DAY + ":" + Calendar.MINUTE + ":" + Calendar.SECOND;
			Log_All.write("[" + date + "] : " + str);
			Log_All.newLine();
			Log_All.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} // ne devrait pas avoir lieu
	}

	public synchronized static void addToRealmLog(String str) {
		if (Config.write_in_console)
			Console.println(str, Color.YELLOW);
		try {
			String date = Calendar.HOUR_OF_DAY + ":" + Calendar.MINUTE + ":" + Calendar.SECOND;
			Log_Realm.write("[" + date + "] : " + str);
			Log_Realm.newLine();
			Log_Realm.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} // ne devrait pas avoir lieu
	}

	public synchronized static void addToRealmSockLog(String str) {
		if (Config.write_in_console)
			Console.println(str, Color.YELLOW);
		try {
			String date = Calendar.HOUR_OF_DAY + ":" + Calendar.MINUTE + ":" + Calendar.SECOND;
			Log_RealmSock.write("[" + date + "] : " + str);
			Log_RealmSock.newLine();
			Log_RealmSock.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} // ne devrait pas avoir lieu
	}

	public synchronized static void addToGameLog(String str) {
		if (Config.write_in_console)
			Console.println(str, Color.CYAN);
		try {
			String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":"
					+ Calendar.getInstance().get(+Calendar.MINUTE) + ":" + Calendar.getInstance().get(Calendar.SECOND);
			Log_Game.write("[" + date + "] : " + str);
			Log_Game.newLine();
			Log_Game.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} // ne devrait pas avoir lieu
	}

	public synchronized static void addToGameSockLog(String str) {
		if (Config.write_in_console)
			Console.println(str, Color.CYAN);
		try {
			String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":"
					+ Calendar.getInstance().get(+Calendar.MINUTE) + ":" + Calendar.getInstance().get(Calendar.SECOND);
			Log_GameSock.write("[" + date + "] : " + str);
			Log_GameSock.newLine();
			Log_GameSock.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} // ne devrait pas avoir lieu
	}

	public static void addToDebug(String str) {
		if (Config.write_in_console)
			Console.println(str, Color.CYAN);
		try {
			String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":"
					+ Calendar.getInstance().get(+Calendar.MINUTE) + ":" + Calendar.getInstance().get(Calendar.SECOND);
			Log_Debug.write("[" + date + "] : " + str);
			Log_Debug.newLine();
			Log_Debug.flush();
		} catch (IOException e) {
		}
	}

	public static void addToMjLog(String str) {
		if (Config.write_in_console)
			Console.println(str, Color.RED);
		try {
			String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":"
					+ Calendar.getInstance().get(+Calendar.MINUTE) + ":" + Calendar.getInstance().get(Calendar.SECOND);
			Log_MJ.write("[" + date + "] : " + str);
			Log_MJ.newLine();
			Log_MJ.flush();
		} catch (IOException e) {
		}
	}

	public static void addToShopLog(String str) {
		if (Config.write_in_console)
			Console.println(str, Color.CYAN);
		try {
			String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":"
					+ Calendar.getInstance().get(+Calendar.MINUTE) + ":" + Calendar.getInstance().get(Calendar.SECOND);
			Log_Shop.write("[" + date + "] : " + str);
			Log_Shop.newLine();
			Log_Shop.flush();
		} catch (IOException e) {
		}
	}

	public static void addToFmLog(String str) {
		if (Config.write_in_console)
			Console.println(str, Color.CYAN);
		try {
			String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":"
					+ Calendar.getInstance().get(+Calendar.MINUTE) + ":" + Calendar.getInstance().get(Calendar.SECOND);
			Log_FM.write("[" + date + "] : " + str);
			Log_FM.newLine();
			Log_FM.flush();
		} catch (IOException e) {
		}
	}

	public static void addToChatLog(String str) {
		if (Config.write_in_console)
			Console.println(str, Color.WHITE);
		String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":"
				+ Calendar.getInstance().get(+Calendar.MINUTE) + ":" + Calendar.getInstance().get(Calendar.SECOND);
		try {
			Log_Chat.write("[" + date + "] : " + str);
			Log_Chat.newLine();
			Log_Chat.flush();
		} catch (IOException e) {
		}
	}
	
	public static void addToSQLLog(String str) {
		if (Config.write_in_console)
			Console.println(str, Color.MAGENTA);
		String date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":"
				+ Calendar.getInstance().get(+Calendar.MINUTE) + ":" + Calendar.getInstance().get(Calendar.SECOND);
		try {
			Log_SQL.write("[" + date + "] : " + str);
			Log_SQL.newLine();
			Log_SQL.flush();
		} catch (IOException e) {
		}
	}
}

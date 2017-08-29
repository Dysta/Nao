package org.kernel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;

import org.kernel.Console.Color;

public class Config {
	public static Timer repeatFmTimer = new Timer(); // Skryn/Return
	public static Timer fightTimers = new Timer();
	public static String Time = "";
	public static boolean Allow_Clear_Console = true;
	public static boolean Allow_Refresh_Title = true;
	public static final double REDUCTION = 0.5;
	public static boolean ALLOW_DISANKALIKE_STATS = false;
	public static String CONFIG_LINK_VOTE;
	public static int PRISMES_DELAIS_NEW_POSE = 60;
	public static ArrayList<Integer> INCARNATIONS_ARMES = new ArrayList<Integer>();
	private static Map<Integer, Long> WhenHasPosePrism = new HashMap<Integer, Long>();
	private static int PosePrism = 0;
	public static ArrayList<Integer> CartesWithoutPrismes = new ArrayList<Integer>();
	public static ArrayList<String> nicknameProhibited = new ArrayList<String>();
	public static ArrayList<String> wordProhibited = new ArrayList<String>();
	public static String DB_NAME;
	public static int RATE_FM = 1;
	public static String IP = "127.0.0.1";
	public static int CONFIG_SERVER_ID = 1;
	public static boolean isInit = false;
	public static short CONFIG_MAP_JAIL = 8534;
	public static int CONFIG_CELL_JAIL = 297;
	public static boolean ANTI_SURCHARGE = false;
	public static String DB_HOST;
	public static String DB_USER;
	public static String DB_PASS;
	public static long FLOOD_TIME = 30000;
	public static String GAMESERVER_IP;
	public static int CONFIG_TRAQUE_DIFFERENCE = 15;
	public static String CONFIG_MOTD = "Bienvenue sur Rubrum Solem";
	public static boolean write_in_console = false;
	public static boolean CONFIG_POLICY = false;
	public static int CONFIG_REALM_PORT = 443;
	public static int CONFIG_GAME_PORT = 5555;
	public static int CONFIG_MAX_PERSOS = 5;
	public static int CONFIG_MAX_PLAYER_PER_IP = 5;
	public static short CONFIG_START_MAP = 10298;
	public static int CONFIG_START_CELL = 314;
	public static boolean CONFIG_ALLOW_MULTI = false;
	public static int CONFIG_START_LEVEL = 1;
	public static int CONFIG_START_KAMAS = 0;
	public static int CONFIG_REBOOT_TIME = 12;
	public static int CONFIG_DROP = 1;
	public static boolean CONFIG_ZAAP = false;
	public static int CONFIG_LOAD_DELAY = 60000;
	public static int CONFIG_LOAD_KICK = 60000;
	public static boolean USE_LIVE_ACTION = true;
	public static int CONFIG_RELOAD_MOB_DELAY = 18000000;
	public static int CONFIG_RELOAD_MOUNT_DELAY = 1080000;
	public static int CONFIG_PLAYER_LIMIT = 200;
	public static boolean CONFIG_IP_LOOPBACK = true;
	public static int XP_PVP = 10;
	public static boolean ALLOW_MULE_PVP = false;
	public static int XP_PVM = 1;
	public static int KAMAS = 1;
	public static int HONOR = 1;
	public static int XP_METIER = 1;
	public static boolean CONFIG_CUSTOM_STARTMAP;
	public static boolean CONFIG_USE_MOBS = false;
	public static boolean CONFIG_USE_IP = false;
	public static boolean isSaving = false;
	public static boolean AURA_SYSTEM = true;
	public static int GUILD_VIEW = 10;
	public static ArrayList<Integer> craqueleurMap = new ArrayList<Integer>(41);
	public static ArrayList<Integer> abraMap = new ArrayList<Integer>(11);
	// Arene
	public static ArrayList<Integer> arenaMap = new ArrayList<Integer>(8);
	public static int CONFIG_ARENA_TIMER = 10 * 60 * 1000;// 10 minutes
	// BDD
	public static int CONFIG_DB_COMMIT = 30 * 1000;
	// Inactivité
	public static int CONFIG_MAX_IDLE_TIME = 60000;// En millisecondes
	// HDV
	public static ArrayList<Integer> NOTINHDV = new ArrayList<Integer>();
	// Challenges et Etoiles
	public static int CONFIG_SECONDS_FOR_BONUS = 3600;
	public static int CONFIG_BONUS_MAX = 100;
	// Temps en combat
	public static long CONFIG_MS_PER_TURN = 45000;
	public static long CONFIG_MS_FOR_START_FIGHT = 45000;
	// Taille Percepteur
	public static boolean CONFIG_TAILLE_VAR = true;
	// Sauvegarde automatique
	public static int CONFIG_SAVE_TIME = 1800000;
	// Xp en défi
	public static boolean CONFIG_XP_DEFI = false;
	// Message de "bienvenue"
	public static String CONFIG_MESSAGE_BIENVENUE = "";
	// Système de pub : Taparisse
	public static int CONFIG_LOAD_PUB = 60000;
	public static String PUB1 = "";
	public static String PUB2 = "";
	public static String PUB3 = "";
	public static String RESTRICTED_MAPS = "";
	public static String CONFIG_COLOR_BLEU = "3366FF";
	public static boolean CONFIG_PUB = false;
	// Reben
	public static String CONFIG_SORT_INDEBUFFABLE = new String();
	public static ArrayList<Integer> CONFIG_MORPH_ALLOWED = new ArrayList<Integer>();
	// KOLI
	public static String KOLIMAPS;
	public static int KOLIMAX_PLAYER;
	public static int KOLI_LEVEL;
	public static int COINS_ID;
	public static int RATE_COINS;
	public static int KOLIZEUM_DELAIS_LEFT_FIGHT = 5; // Par default 5 minutes
	// Formule de tacle
	// 0% | 50% | 50% | 80% | 100%
	// AgiTacleur>X , AgiTacleur>X,X , AgiCible>X,X , AgiCible>X,X , AgiCible>X
	public static String TACLE_FORMULA = "50,1,50,1,50,50,100,100";
	public static String TACLE_PERCENTAGE = "0,50,50,80,100";
	public static int PRICE_FM_CAC; // Prix en points boutiques pour FM un cac.
	// Abonnement
	public static boolean USE_SUBSCRIBE = false;
	// Achat d'Abonnement IG by Starlight
	public static boolean ENABLE_IG_SUBSCRIPTION_BUY = false;
	public static int PRICE_BUY_7DAY = 0;
	public static int PRICE_BUY_1MONTH = 0;
	public static int PRICE_BUY_3MONTH = 0;
	public static int PRICE_BUY_6MONTH = 0;
	public static int PRICE_BUY_YEAR = 0;
	// Staff
	public static String STAFF_GM1 = "Staff en test";
	public static String STAFF_GM2 = "Animateur";
	public static String STAFF_GM3 = "Modérateur";
	public static String STAFF_GM4 = "Maitre du jeu";
	public static String STAFF_GM5 = "Administrateur";

	boolean isLoaded = false;

	public static boolean load() {
		Properties prop = new Properties();
		InputStream config = null;
		
		try {
			config = new FileInputStream("config.properties");
			// load config file
			prop.load(config);
			if(prop.isEmpty()){
				//makeConfiguration();
				System.exit(0);
				return false;
			}

			// get the property value for database
			DB_HOST = prop.getProperty("DB_HOST");
			DB_USER = prop.getProperty("DB_USER");
			DB_PASS = prop.getProperty("DB_PASS") == null ? "" : prop.getProperty("DB_PASS");
			DB_NAME = prop.getProperty("DB_NAME");

			CONFIG_GAME_PORT = Integer.parseInt(prop.getProperty("GAME_PORT"));
			CONFIG_REALM_PORT = Integer.parseInt(prop.getProperty("REALM_PORT"));

			write_in_console = Boolean.parseBoolean(prop.getProperty("WRITE_IN_CONSOLE"));
			CONFIG_USE_IP = Boolean.parseBoolean(prop.getProperty("USE_IP"));
			IP = prop.getProperty("HOST_IP");
			CONFIG_IP_LOOPBACK = Boolean.parseBoolean(prop.getProperty("LOCALIP_LOOPBACK"));

			CONFIG_POLICY = Boolean.parseBoolean(prop.getProperty("SEND_POLICY"));
			CONFIG_MOTD = prop.getProperty("MOTD");

			Allow_Clear_Console = Boolean.parseBoolean(prop.getProperty("ALLOW_CLEAR_CONSOLE"));
			Allow_Refresh_Title = Boolean.parseBoolean(prop.getProperty("ALLOW_REFRESH_TITLE"));
			CONFIG_ALLOW_MULTI = Boolean.parseBoolean(prop.getProperty("ALLOW_MULTI_ACCOUNT"));
			ALLOW_DISANKALIKE_STATS = Boolean.parseBoolean(prop.getProperty("USE_CARAC_2.0"));
			CONFIG_MAX_PLAYER_PER_IP = Integer.parseInt(prop.getProperty("MAX_PLAYER_PER_IP"));
			
			for (String curNickname : prop.getProperty("BLACKLIST_PSEUDO").split(","))
				nicknameProhibited.add(curNickname.toLowerCase());
			
			if(nicknameProhibited == null)
				Console.println("The blacklist of the nicknames is empty, be careful!", Color.RED);
			
			for (String curWords : prop.getProperty("BLACKLIST_WORDS").split(","))
				wordProhibited.add(curWords.toLowerCase());
			
			if(wordProhibited == null)
				Console.println("The blacklist of the words is empty, be careful!", Color.RED);

			CONFIG_PLAYER_LIMIT = Integer.parseInt(prop.getProperty("PLAYER_LIMIT"));
			CONFIG_LOAD_DELAY = (Integer.parseInt(prop.getProperty("LOAD_ACTION_DELAY")) * 60000);
			USE_LIVE_ACTION = Boolean.parseBoolean(prop.getProperty("USE_LIVE_ACTION"));
			CONFIG_LOAD_KICK = (Integer.parseInt(prop.getProperty("AUTO_KICK")) * 60000);

			CONFIG_MAX_IDLE_TIME = (Integer.parseInt(prop.getProperty("MAX_IDLE_TIME")) * 60000);
			CONFIG_SERVER_ID = Integer.parseInt(prop.getProperty("SERVER_ID"));
			CONFIG_REBOOT_TIME = Integer.parseInt(prop.getProperty("LOAD_SAVE_DELAY"));
			CONFIG_SAVE_TIME =  (Integer.parseInt(prop.getProperty("AUTO_SAVE")) * 3600000);
			FLOOD_TIME = (Integer.parseInt(prop.getProperty("CANAL_DELAY")) * 1000);

			// Get the property value of rate
			XP_PVM = Integer.parseInt(prop.getProperty("XP_PVM"));
			XP_PVP = Integer.parseInt(prop.getProperty("XP_PVP"));
			XP_METIER = Integer.parseInt(prop.getProperty("XP_METIER"));
			CONFIG_DROP = Integer.parseInt(prop.getProperty("DROP"));
			KAMAS = Integer.parseInt(prop.getProperty("KAMAS"));
			HONOR = Integer.parseInt(prop.getProperty("HONOR"));
			RATE_FM = Integer.parseInt(prop.getProperty("RATE_FM"));

			// Get the property value for subscription
			ENABLE_IG_SUBSCRIPTION_BUY = Boolean.parseBoolean(prop.getProperty("ENABLE_IG_SUBSCRIPTION_BUY"));
			PRICE_BUY_7DAY = Integer.parseInt(prop.getProperty("PRICE_BUY_7DAY"));
			PRICE_BUY_1MONTH = Integer.parseInt(prop.getProperty("PRICE_BUY_1MONTH"));
			PRICE_BUY_3MONTH = Integer.parseInt(prop.getProperty("PRICE_BUY_3MONTH"));
			PRICE_BUY_6MONTH = Integer.parseInt(prop.getProperty("PRICE_BUY_6MONTH"));
			PRICE_BUY_YEAR = Integer.parseInt(prop.getProperty("PRICE_BUY_YEAR"));

			// Get the property value for gameplay
			USE_SUBSCRIBE = Boolean.parseBoolean(prop.getProperty("USE_SUBSCRIBE"));
			CONFIG_MAX_PERSOS = Integer.parseInt(prop.getProperty("MAX_CHARACTERS_PER_ACCOUNT"));
			CONFIG_USE_MOBS = Boolean.parseBoolean(prop.getProperty("USE_MOBS"));
			ALLOW_MULE_PVP = Boolean.parseBoolean(prop.getProperty("ALLOW_MULE_PVP"));
			CONFIG_ZAAP = Boolean.parseBoolean(prop.getProperty("ZAAP"));
			AURA_SYSTEM = Boolean.parseBoolean(prop.getProperty("AURA_SYSTEM"));
			PRICE_FM_CAC = Integer.parseInt(prop.getProperty("PRICE_FM_CAC"));
			GUILD_VIEW = Integer.parseInt(prop.getProperty("GUILD_VIEW"));

			CONFIG_CUSTOM_STARTMAP = Boolean.parseBoolean(prop.getProperty("USE_CUSTOM_START"));
			CONFIG_START_MAP = Short.parseShort(prop.getProperty("START_MAP"));
			CONFIG_START_CELL = Short.parseShort(prop.getProperty("START_CELL"));
			CONFIG_START_LEVEL = Short.parseShort(prop.getProperty("START_LEVEL"));
			CONFIG_START_KAMAS = Integer.parseInt(prop.getProperty("START_KAMAS"));

			if (CONFIG_START_LEVEL > 200)
				CONFIG_START_LEVEL = 200;
			if (CONFIG_START_KAMAS < 0)
				CONFIG_START_KAMAS = 0;
			if (CONFIG_START_KAMAS > 1000000000)
				CONFIG_START_KAMAS = 1000000000;

			RATE_COINS = Integer.parseInt(prop.getProperty("RATE_COINS"));

			// CONFIG_LVL_PVP = Integer.parseInt(prop.getProperty("LVL_PVP"));

			for (String curID : prop.getProperty("NOT_IN_HDV").split(","))
				NOTINHDV.add(Integer.parseInt(curID));

			RESTRICTED_MAPS = prop.getProperty("RESTRICTED_MAPS").trim();

			CONFIG_SORT_INDEBUFFABLE = prop.getProperty("CONFIG_SORT_INDEBUFFABLE").trim();

			for (String morphID : prop.getProperty("CONFIG_MORPH_ALLOWED").split(","))
				CONFIG_MORPH_ALLOWED.add(Integer.parseInt(morphID));

			PRISMES_DELAIS_NEW_POSE = Integer.parseInt(prop.getProperty("PRISMES_DELAIS_NEW_POSE"));

			for (String curID : prop.getProperty("PRISMES_DELAIS_NEW_POSE").split(","))
				CartesWithoutPrismes.add(Integer.parseInt(curID));

			TACLE_FORMULA = prop.getProperty("TACLE_FORMULA");
			TACLE_PERCENTAGE = prop.getProperty("TACLE_PERCENTAGE");
			CONFIG_MAP_JAIL = Short.parseShort(prop.getProperty("MAP_JAIL"));
			CONFIG_CELL_JAIL = Short.parseShort(prop.getProperty("CELL_JAIL"));

			KOLIMAPS = prop.getProperty("KOLIMAP");
			KOLIMAX_PLAYER = Integer.parseInt(prop.getProperty("KOLIMAX_PLAYER"));
			KOLI_LEVEL = Integer.parseInt(prop.getProperty("KOLI_LEVEL"));
			KOLIZEUM_DELAIS_LEFT_FIGHT = Integer.parseInt(prop.getProperty("INSCRIPTION_KOLIZEUM_DELAIS"));
			KOLI_LEVEL = Integer.parseInt(prop.getProperty("TRANCHE_LVL"));
			COINS_ID = Integer.parseInt(prop.getProperty("COINS_ID"));

			for (String curID : prop.getProperty("ARENA_MAP").split(","))
				arenaMap.add(Integer.parseInt(curID));

			CONFIG_ARENA_TIMER = Integer.parseInt(prop.getProperty("ARENA_TIMER"));

			for (String curID : prop.getProperty("CRAQUELEUR_MAP").split(","))
				craqueleurMap.add(Integer.parseInt(curID));

			for (String curID : prop.getProperty("ABRA_MAP").split(","))
				abraMap.add(Integer.parseInt(curID));

			CONFIG_PUB = Boolean.parseBoolean(prop.getProperty("USE_PUB_SYSTEM"));
			CONFIG_LOAD_PUB = (Integer.parseInt(prop.getProperty("LOAD_PUB_DELAY")) * 60000);
			PUB1 = prop.getProperty("PUB1");
			PUB2 = prop.getProperty("PUB2");
			PUB3 = prop.getProperty("PUB3");

			if (DB_NAME == null || DB_HOST == null || DB_PASS == null || DB_USER == null)
				throw new Exception();
			
			return true;

		} catch (Exception e) {
			Console.println("Error Loading Configuration File.", Color.RED);
			//makeConfiguration();
			System.exit(0);
			return false;
		} finally {
			if (config != null) {
				try {
					config.close();
				} catch (IOException e) {
					Console.println(e.getMessage(), Color.RED);
				}
			}
		}
	}
		
	// TODO
	@SuppressWarnings(value = { "unused" })
	private static void makeConfiguration() {
	    try {    
	    File configFile = new File(String.format("%s\\configg.properties", System.getProperty("user.dir")));
	    if (!configFile.exists()) configFile.createNewFile();
	            
	        BufferedWriter writer = new BufferedWriter(new FileWriter(configFile, true));
	        writer.write("# TEUB\n");
	        writer.flush();
	        writer.close();
	        
			if (configFile.exists())
				Console.println("Configuration file created", Color.GREEN);
	    } catch (IOException e) {
	        Console.println("Unable to create the configuration file", Color.RED);
	    }
	}

	public void setWhenHasPosePrism(Map<Integer, Long> whenHasPosePrism) {
		WhenHasPosePrism = whenHasPosePrism;
	}

	public static Map<Integer, Long> getWhenHasPosePrism() {
		return WhenHasPosePrism;
	}

	public static void setPosePrism(int posePrism) {
		PosePrism = posePrism;
	}

	public static int getPosePrism() {
		return PosePrism;
	}
}
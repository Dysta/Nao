package org.kernel;

import java.io.Console;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.client.Characters;
import org.common.Constant;
import org.common.SQLManager;
import org.common.SocketManager;
import org.common.World;
import org.game.GameThread;
import org.game.GameServer.SaveThread;
import org.kernel.Config;
import org.kernel.Console.Color;

public class ConsoleInputAnalyzer implements Runnable {
	private Thread _t;
	Characters _perso;

	public ConsoleInputAnalyzer() {
		this._t = new Thread(this);
		_t.setDaemon(true);
		_t.start();
	}

	@Override
	public void run() {
		while (Main.isRunning) {
			Console console = System.console();
			String command = console.readLine();
			try {
				evalCommand(command);
			} catch (Exception e) {
			} finally {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public void evalCommand(String command) throws FileNotFoundException, IOException {
		String[] args = command.split(" ");
		String fct = args[0].toUpperCase();
		
		switch(fct){
		case "?":
		case "HELP":
			sendInfo("<> Commands <>");
			sendInfo("- HELP or ? for the command list.");
			sendInfo("- SAVE for save the server.");
			sendInfo("- EXIT for close the server.");
			sendInfo("- STAFF for find connected staff members.");
			sendInfo("- WRITE for enable/disable the write in consol.");
			sendInfo("- ANNOUNCE/AN [Message] for send a message to player.");
			sendInfo("- CLS/CLR/CLEAR for clean the console.");
			sendInfo("- KICK [Pseudo] for kick a player.");
			sendInfo("- RELOADSERV/RLD/RELOAD/RLDSERV for reload the server.");
			sendInfo("- INFO/INFOS for show server infos.");
			sendInfo("- WHO for show connected players.");
			sendInfo("- PURGE/PURGERAM for purge the ram.");
			sendInfo("- LOCK [0][1][2] for change server state.");
			sendInfo("- TELEPORT/TP [MapId] [CellId] [Pseudo] for teleport a player.");
			sendInfo("- GIFT/ALLGIFT [IDItem] for give a item to all connected players.");
			sendInfo("- PING <> PONG");
			sendInfo("- ECHO [Message] Repeat after me bro o_o.");
			sendInfo("- CTRL + C for stop the server (only if really needed).");
			sendInfo("<> -------- <>");
			break;
		case "SAVE":
			Thread save = new Thread(new SaveThread());
			save.start();
			break;
		case "EXIT":
			Reboot.start();
			break;
		case "STAFF":
			sendInfo("<> Staff Online <>");
			sendStaff(SQLManager.getStaffOnline());
			sendInfo("<> ------------ <>");
			break;
		case "WRITE":
			Config.write_in_console = !Config.write_in_console;
			if(Config.write_in_console)
				sendInfo("Write in console actived !");
			else
				sendInfo("Write in console disabled !");
			break;
		case "AN":
		case "ANNOUNCE":
			String announce = null;
			try {
				announce = command.substring(9);
			} catch (Exception e) {
				sendError("Faites ANNOUNCE/AN [Message]");
			}
			if(announce != null){
				SocketManager.GAME_SEND_Im_PACKET_TO_ALL("116;<b>Serveur</b>~" + announce);
				sendEcho("<> Serveur : " + announce + " <>");
			} else
				sendError("Faites announce message");
			break;
		case "CLS":
		case "CLR":
		case "CLEAR":
			Main.Clear();
			break;
		case "KICK":
			String name = null;
			try {
				name = command.substring(5);
			}catch (Exception e){
				sendError("Faites kick [Player]");
			}
			Characters perso = World.getPersoByName(name);
			
			if(perso == null){
				sendError("The player " + name.trim() + " doesn't exist");
				break;
			}
			
			if(perso.isOnline()){
				perso.get_compte().getGameThread().kick();
				sendInfo("The player " + perso.get_name().trim() + " have been kick succesfully");
			}else{
				sendError("The player " + perso.get_name().trim() + " is not connected");
			}
			break;
		case "RLD":
		case "RELOAD":
		case "RLDSERV":
		case "RELOADSERV":
			org.kernel.Console.bright();
			sendConfig("Loading Configuration : " + Config.load());
			
			org.kernel.Console.bright();
			sendDebug("\n<> Reload datas <>");
			SQLManager.LOAD_ITEMS_FULL();
			System.out.println();
			SQLManager.LOAD_MOB_TEMPLATE();
			System.out.println();
			SQLManager.LOAD_NPC_TEMPLATE();
			System.out.println();
			SQLManager.LOAD_NPC_QUESTIONS();
			System.out.println();
			SQLManager.LOAD_NPC_ANSWERS();
			System.out.println();
			SQLManager.LOAD_COMPTES();
			System.out.println();
			SQLManager.LOAD_MOUNTPARKS();
			System.out.println();
			SQLManager.LOAD_MOUNTS();
			System.out.println();
			SQLManager.LOAD_TRIGGERS();
			System.out.println();
			SQLManager.LOAD_OBJ_TEMPLATE();
			System.out.println();
			SQLManager.LOAD_ITEM_ACTIONS();
			System.out.println();
			SQLManager.LOAD_SORTS();
			System.out.println();
			org.kernel.Console.bright();
			sendInfo("\n<> All datas has been reloaded <>");
			break;
		case "INFO":
		case "INFOS":
			long uptime = System.currentTimeMillis() - Main.gameServer.getStartTime();
			int jour = (int) (uptime / (1000 * 3600 * 24));
			uptime %= (1000 * 3600 * 24);
			int hour = (int) (uptime / (1000 * 3600));
			uptime %= (1000 * 3600);
			int min = (int) (uptime / (1000 * 60));
			uptime %= (1000 * 60);
			int sec = (int) (uptime / (1000));

			String uptimeStr = "Uptime : " + jour + "d " + hour + "h " + min + "m " + sec + "s\n"
					+ "Connected : " + Main.gameServer.getPlayerNumber() + "\n" 
					+ "Max Connected : " + Main.gameServer.getMaxPlayer() + "\n";
			sendInfo("<> Infos <>");
			sendStaff(uptimeStr);
			sendInfo("<> ----- <>");
			break;
		case "WHO":
			sendInfo("<> List of connected players <>");
			String mess = "";
			int diff = Main.gameServer.getClients().size() - 30;
			for (byte b = 0; b < 30; b++) {
				if (b == Main.gameServer.getClients().size())
					break;
				GameThread GT = Main.gameServer.getClients().get(b);
				Characters P = GT.getPerso();
				if (P == null)
					continue;
				mess = P.get_name() + "(" + P.get_GUID() + ") ";

				switch (P.get_classe()) {
				case Constant.CLASS_FECA:
					mess += "Fec";
					break;
				case Constant.CLASS_OSAMODAS:
					mess += "Osa";
					break;
				case Constant.CLASS_ENUTROF:
					mess += "Enu";
					break;
				case Constant.CLASS_SRAM:
					mess += "Sra";
					break;
				case Constant.CLASS_XELOR:
					mess += "Xel";
					break;
				case Constant.CLASS_ECAFLIP:
					mess += "Eca";
					break;
				case Constant.CLASS_ENIRIPSA:
					mess += "Eni";
					break;
				case Constant.CLASS_IOP:
					mess += "Iop";
					break;
				case Constant.CLASS_CRA:
					mess += "Cra";
					break;
				case Constant.CLASS_SADIDA:
					mess += "Sad";
					break;
				case Constant.CLASS_SACRIEUR:
					mess += "Sac";
					break;
				case Constant.CLASS_PANDAWA:
					mess += "Pan";
					break;
				default:
					mess += "Unk";
				}
				mess += " ";
				mess += (P.get_sexe() == 0 ? "M" : "F") + " ";
				mess += P.get_lvl() + " ";
				mess += P.get_curCarte().get_id() + "(" + P.get_curCarte().getX() + "/" + P.get_curCarte().getY()
						+ ") ";
				mess += P.get_fight() == null ? "" : "Fight ";
				sendInfo(mess);
			}
			if (diff > 0) {
				sendInfo("And " + diff + " other characters.");
			}
			sendInfo("<> ------------------------- <>");
			break;
		case "PURGE":
		case "PURGERAM":
			try {
				sendEcho("Try to purge Ram...");
				Runtime r = Runtime.getRuntime();
				try {
					r.runFinalization();
					r.gc();
					System.gc();
					sendInfo("Ram purged.");
				} catch (Exception e) {
					sendError("Impossible to purge the Ram.");
				}
			} catch (Exception e) {
				sendError("Impossible to purge the Ram.");
			}
			break;
		case "LOCK":
			byte lockValue = 1;// Accessible
			try {
				lockValue = Byte.parseByte(args[1]);
			}catch (Exception e){
				sendError("Faites lock [statut]");
				break;
			}
			switch(lockValue){
			case 0:
				World.set_state((short) 0);
				sendEcho("Server no available");
				break;
			case 1:
				World.set_state((short) 1);
				sendEcho("Server accessible");
				break;
			case 2:
				World.set_state((short) 2);
				sendEcho("Server in save");
				break;
			default:
				sendError("Invalid value");
				break;
			}
			break;
		case "TP":
		case "TELEPORT":
			short mapID = -1;
			int cellID = -1;
			Characters player = null;
			try {
				mapID = Short.parseShort(args[1]);
				cellID = Integer.parseInt(args[2]);
				player = World.getPersoByName(args[3]);
			} catch (Exception e) {
				sendError("Faites TP ou TELEPORT [MapID] [CellID] [Player]");
				break;
			}
			;
			if (mapID == -1 || cellID == -1 
					|| World.getCarte(mapID) == null 
					|| World.getCarte(mapID).getCase(cellID) == null
					|| player == null || player.get_fight() != null) {
				sendError("MapID, CellID or Player invalid");
				break;
			}
			player.teleport(mapID, cellID);
			sendInfo("The player " + player.get_name().trim() + " has been teleported");
			break;
		case "GIFT":
		case "ALLGIFT":
			int gift = 0;

			try {
				gift = Integer.parseInt(args[1]);
			} catch (Exception e) {
				sendError("Faites ALLGIFT [IDcadeau]");
				break;
			}
			for (Characters pj : World.getOnlinePersos()) {
				pj.get_compte().setCadeau(gift);
			}
			sendEcho("The item " + gift + " is given to all connected players.");
			break;
		case "PING":
			sendMsg("PONG !");
			break;
		case "PONG":
			sendMsg("PING !");
			break;
		case "ECHO":
			try {
				String message = command.substring(5);
				org.kernel.Console.bright();
				sendEcho(message.trim());
			} catch (Exception e) {
				sendError("Faite echo [Message]");
			}
			break;
		default:
			sendError("Command unrecognized. Type ? or HELP to display the list of commands");
			break;
		}
		send("\nCommand > ");
	}

	public static void send(String msg) {
		org.kernel.Console.print(msg, Color.WHITE);
	}
	
	public static void sendMsg(String msg){
		org.kernel.Console.println(msg, Color.WHITE);
	}
	
	public static void sendConfig(String msg){
		org.kernel.Console.println(msg, Color.MAGENTA);
	}
	
	public static void sendInfo(String msg) {
		org.kernel.Console.println(msg, Color.GREEN);
	}

	public static void sendError(String msg) {
		org.kernel.Console.println(msg, Color.RED);
	}

	public static void sendDebug(String msg) {
		org.kernel.Console.println(msg, Color.YELLOW);
	}
	
	public static void sendStaff(String msg) {
		org.kernel.Console.print(msg, Color.GREEN);
	}

	public static void sendEcho(String msg) {
		org.kernel.Console.println(msg, Color.BLUE);
	}
}

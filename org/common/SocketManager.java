package org.common;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.client.Characters;
import org.client.Characters.Group;
import org.common.World.ItemSet;
import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.object.Collector;
import org.fight.object.Prism;
import org.fight.object.Monster.MobGroup;
import org.game.GameSendThread;
import org.game.GameServer;
import org.kernel.Logs;
import org.kernel.Main;
import org.object.AuctionHouse;
import org.object.Guild;
import org.object.Maps;
import org.object.Mount;
import org.object.Objects;
import org.object.Trunk;
import org.object.AuctionHouse.HdvEntry;
import org.object.Guild.GuildMember;
import org.object.Maps.Case;
import org.object.Maps.InteractiveObject;
import org.object.Maps.MountPark;
import org.object.NpcTemplates.NPC;
import org.object.Objects.ObjTemplate;
import org.object.job.Job.StatsMetier;

public class SocketManager {

	public static void send(Characters p, String packet) {
		if (p == null || p.get_compte() == null)
			return;
		if (p.get_compte().getGameThread() == null)
			return;
		GameSendThread out = p.get_compte().getGameThread().get_out();
		if (out != null) {
			packet = CryptManager.toUtf(packet);
			out.send(packet);
		}
	}

	public static void send(GameSendThread out, String packet) {
		if (out != null) {
			packet = CryptManager.toUtf(packet);
			out.send(packet);
			Logs.addToHistoricLog("Game: Packet: " + packet +" to " + out);
		}
	}
	
	public static void send(PrintWriter out, String packet) {
		long t = System.currentTimeMillis();
		if (out != null && !packet.equals("") && !packet.equals("\0")) {
			packet = CryptManager.toUtf(packet);
			out.print((new StringBuilder(String.valueOf(packet))).append('\0').toString());
			out.flush();
		}
		if (System.currentTimeMillis() - t > 5000L)
			GAME_SEND_cMK_PACKET_TO_ADMIN("@", 0, "DEBUG-SOCKET-OUT",
					(new StringBuilder("SocketManager.send( ____OUT ); = ")).append(System.currentTimeMillis() - t)
							.append("; ").toString());
	}
	
	/* Start Packet */

	public static void MULTI_SEND_Af_PACKET(GameSendThread out, int position, int totalAbo, int totalNonAbo,
			String subscribe, int queueID) {
		StringBuilder packet = new StringBuilder();
		packet.append("Af").append(position).append("|").append(totalAbo).append("|").append(totalNonAbo).append("|")
				.append(subscribe).append("|").append(queueID);
		send(out, packet.toString());

		Logs.addToRealmSockLog("Serv: Send>>" + packet.toString());
	}

	public static void GAME_SEND_HELLOGAME_PACKET(GameSendThread out) {
		String packet = "HG";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_ATTRIBUTE_FAILED(GameSendThread out) {
		String packet = "ATE";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_ATTRIBUTE_SUCCESS(GameSendThread out) {
		String packet = "ATK0";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_AV0(GameSendThread out) {
		String packet = "AV0";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_HIDE_GENERATE_NAME(GameSendThread out) {
		String packet = "APE2";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_PERSO_LIST(GameSendThread out, Map<Integer, Characters> persos, int subscriber) {
		StringBuilder packet = new StringBuilder();
		packet.append("ALK").append((subscriber * 60) + "000").append("|").append(persos.size());
		for (Entry<Integer, Characters> entry : persos.entrySet()) {
			packet.append(entry.getValue().parseALK());

		}
		send(out, packet.toString());

		Logs.addToGameSockLog("Game: Send >> " + packet.toString());

	}

	public static void GAME_SEND_SUBSCRIBE_MESSAGE(GameSendThread out, String str) {
		StringBuilder packet = new StringBuilder();
		packet.append("BP").append(str);
		send(out, packet.toString());

		Logs.addToGameSockLog("Game: Send >> " + packet.toString());
	}

	public static void GAME_SEND_SUBSCRIBE_MESSAGE(Characters p, String str) {
		StringBuilder packet = new StringBuilder();
		packet.append("BP").append(str);
		send(p, packet.toString());

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
				+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_CREATE_DOC(Characters p, String doc) {
		String packet = "dC|" + doc;
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
				+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_LEAVE_DOC(Characters p) {
		String packet = "dV";
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
				+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	/** TODO: Packets prismes **/
	public static void GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(Characters p, String str) {
		String packet = "am" + str;
		send(p, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID() + ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_aM_ALIGN_PACKET_TO_AREA(Characters p, String str) {
		String packet = "aM" + str;
		send(p, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID() + ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() +")");
		
	}

	public static void SEND_Cb_CONQUETE(Characters p, String str) {
		String packet = "Cb" + str;
		send(p, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID() + ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() +")");
		
	}

	public static void SEND_GA_Action_ALL_MAPS(Maps Carte, String gameActionID, int actionID, String s1, String s2) {
		String packet = "GA" + gameActionID + ";" + actionID + ";" + s1;
		if (!s2.equals(""))
			packet += ";" + s2;
		for (Characters z : Carte.getPersos())
			send(z, packet);
		Logs.addToGameSockLog("Game: Send >> ALL(" + Carte.getPersos().size() +") " + packet.toString());
	}

	public static void SEND_CIV_INFOS_CONQUETE(Characters p) {
		String packet = "CIV";
		send(p, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID() + ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() +")");	
	}

	public static void SEND_CW_INFO_CONQUETE(Characters p, String str) {
		String packet = "CW" + str;
		send(p, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID() + ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() +")");
		
	}

	public static void SEND_CB_BONUS_CONQUETE(Characters p, String str) {
		String packet = "CB" + str;
		send(p, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID() + ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() +")");
		
	}

	public static void SEND_Wp_MENU_Prisme(Characters p) {
		String packet = "Wp" + p.parsePrismesList();
		send(p.get_compte().getGameThread().get_out(), packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID() + ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() +")");
	}

	public static void SEND_Ww_CLOSE_Prisme(Characters p) {
		String packet = "Ww";
		send(p, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID() + ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() +")");
		
	}

	public static void SEND_CP_INFO_DEFENSEURS_PRISME(Characters p, String str) {
		String packet = "CP" + str;
		send(p, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID() + ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() +")");
		
	}

	public static void SEND_Cp_INFO_ATTAQUANT_PRISME(Characters p, String str) {
		String packet = "Cp" + str;
		send(p, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID() + ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() +")");
		
	}

	public static void SEND_CA_ATTAQUE_MESSAGE_PRISME(Characters p, String str) {
		String packet = "CA" + str;
		send(p, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID() + ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() +")");
	}

	public static void SEND_CS_SURVIVRE_MESSAGE_PRISME(Characters perso, String str) {
		String packet = "CS" + str;
		send(perso, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void SEND_CD_MORT_MESSAGE_PRISME(Characters perso, String str) {
		String packet = "CD" + str;
		send(perso, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void SEND_CIJ_INFO_JOIN_PRISME(Characters perso, String str) {
		String packet = "CIJ" + str;
		send(perso, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}
	//TODO OCO
	public static void GAME_SEND_OCO_PACKET_REMOVE(Characters perso, Objects obj) {
		String packet = "OCO" + obj.parseItem() + "*" + obj.getGuid();
		send(perso, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void SEND_GM_PRISME_TO_MAP(GameSendThread _out, Maps Carte) {
		String packet = Carte.getPrismeGMPacket();
		if (packet == "" || packet.isEmpty())
			return;
		send(_out, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString());
	}

	public static void GAME_SEND_PRISME_TO_MAP(Maps Carte, Prism Prisme) {
		String packet = Prisme.getGMPrisme();
		for (Characters z : Carte.getPersos())
			send(z, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString());
	}
	/** TODO: Fin packets prismes **/

	public static void REALM_SEND_MESSAGE_DECO(Characters perso, int MSG_ID, String args) {
		String packet = "M0" + MSG_ID + "|" + args;
		send(perso, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}
	
	public static void SEND_MESSAGE_DECO_ALL() {
		String packet = "M04|";
		for (Characters perso : World.getOnlinePersos()) {
			send(perso, packet);
		}
		Logs.addToGameSockLog("Game: Send ALL(" + World.getOnlinePersos().size() + ")>> " + packet.toString());
	}

	public static void GAME_SEND_NAME_ALREADY_EXIST(GameSendThread out) {
		String packet = "AAEa";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}
	
	public static void GAME_SEND_NAME_INCORRECT(GameSendThread out) {
		String packet = "AAEn";
		send(out, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_CREATE_PERSO_FULL(GameSendThread out) {
		String packet = "AAEf";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_CREATE_OK(GameSendThread out) {
		String packet = "AAK";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_DELETE_PERSO_FAILED(GameSendThread out) {
		String packet = "ADE";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_CREATE_FAILED(GameSendThread out) {
		String packet = "AAEF";
		send(out, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_PERSO_SELECTION_FAILED(GameSendThread out) {
		String packet = "ASE";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_STATS_PACKET(Characters perso) {
		String packet = perso.getAsPacket();
		send(perso, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_Rx_PACKET(Characters perso) {
		String packet = "Rx" + perso.getMountXpGive();
		send(perso, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_Rn_PACKET(Characters perso, String name) {
		String packet = "Rn" + name;
		send(perso, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_Re_PACKET(Characters perso, String sign, Mount DD) {
		String packet = "Re" + sign;
		if (sign.equals("+"))
			packet += DD.parse();

		send(perso, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_ASK(GameSendThread out, Characters perso) {
		StringBuilder packet = new StringBuilder();
		packet.append("ASK|").append(perso.get_GUID()).append("|").append(perso.get_name()).append("|");
		packet.append(perso.get_lvl()).append("|").append(perso.get_classe()).append("|").append(perso.get_sexe());
		packet.append("|").append(perso.get_gfxID()).append("|")
				.append((perso.get_color1() == -1 ? "-1" : Integer.toHexString(perso.get_color1())));
		packet.append("|").append((perso.get_color2() == -1 ? "-1" : Integer.toHexString(perso.get_color2())))
				.append("|");
		packet.append((perso.get_color3() == -1 ? "-1" : Integer.toHexString(perso.get_color3()))).append("|");
		packet.append(perso.parseItemToASK());

		send(out, packet.toString());

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_ALIGNEMENT(GameSendThread out, int alliID) {
		String packet = "ZS" + alliID;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_ADD_CANAL(GameSendThread out, String chans) {
		String packet = "cC+" + chans;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_ZONE_ALLIGN_STATUT(GameSendThread out) {
		String packet = "al" + World.getSousZoneStateString();
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_SEESPELL_OPTION(GameSendThread out, boolean spells) {
		String packet = "SLo" + (spells ? "+" : "-");
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_RESTRICTIONS(GameSendThread out) {
		String packet = "AR6bk";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_Ow_PACKET(Characters perso) {
		String packet = "Ow" + perso.getPodUsed() + "|" + perso.getMaxPod();
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_OT_PACKET(GameSendThread out, int id) {
		String packet = "OT";
		if (id > 0)
			packet += id;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_SEE_FRIEND_CONNEXION(GameSendThread out, boolean see) {
		String packet = "FO" + (see ? "+" : "-");
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_GAME_CREATE(GameSendThread out, String _name) {
		String packet = "GCK|1|" + _name;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_SERVER_HOUR(GameSendThread out) {
		String packet = GameServer.getServerTime();
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_SERVER_DATE(GameSendThread out) {
		String packet = GameServer.getServerDate();
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_MAPDATA(GameSendThread out, int id, String date, String key) {
		String packet = "GDM|" + id + "|" + date + "|" + key;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_GDK_PACKET(GameSendThread out) {
		String packet = "GDK";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_MAP_MOBS_GMS_PACKETS(GameSendThread out, Maps carte) {
		String packet = carte.getMobGroupGMsPackets();
		if (packet.equals(""))
			return;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_MAP_OBJECTS_GDS_PACKETS(GameSendThread out, Maps carte) {
		String packet = carte.getObjectsGDsPackets();
		if (packet.equals(""))
			return;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_MAP_NPCS_GMS_PACKETS(GameSendThread _out, Maps carte) {
		String packet = carte.getNpcsGMsPackets();
		if (packet.equals("") && packet.length() < 4)
			return;
		send(_out, packet);

		Logs.addToDebug((new StringBuilder("Game: Send >> ")).append(packet).toString());
	}

	public static void GAME_SEND_MAP_PERCO_GMS_PACKETS(GameSendThread out, Maps carte) {
		String packet = Collector.parseGM(carte);
		if (packet.length() < 5)
			return;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}
	/*
	 * public static void GAME_SEND_MAP_GMS_PACKETS(GameSendThread out, Carte
	 * carte) { String packet = carte.getGMsPackets(); send(out,packet);
	 * if(Ancestra.debug) Logs.addToGameSockLog("Game: Send >> "+packet); }
	 */

	public static void GAME_SEND_ERASE_ON_MAP_TO_MAP(Maps map, int guid) {
		String packet = "GM|-" + guid;
		if (map == null || map.getPersos() == null)
			return;
		for (Characters z : map.getPersos()) {
			if (z.get_compte().getGameThread() == null)
				continue;
			send(z.get_compte().getGameThread().get_out(), packet);
		}

		Logs.addToGameSockLog("Game: Map " + map.get_id() + " (" + map.getPersos().size() + "): Send>>" + packet);
	}

	public static void GAME_SEND_ERASE_ON_MAP_TO_FIGHT(Fight f, int guid) {
		String packet = "GM|-" + guid;
		for (int z = 0; z < f.getFighters(1).size(); z++) {
			if (f.getFighters(1).get(z).getPersonnage().get_compte().getGameThread() == null)
				continue;
			send(f.getFighters(1).get(z).getPersonnage(), packet);
		}
		for (int z = 0; z < f.getFighters(2).size(); z++) {
			if (f.getFighters(2).get(z).getPersonnage().get_compte().getGameThread() == null)
				continue;
			send(f.getFighters(2).get(z).getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fighter ID " + f.get_id() + ": Send>>" + packet);
	}

	public static void GAME_SEND_ON_FIGHTER_KICK(Fight f, int guid, int team) {
		String packet = "GM|-" + guid;
		for (Fighter F : f.getFighters(team)) {
			if (F.getPersonnage() == null || F.getPersonnage().get_compte().getGameThread() == null
					|| F.getPersonnage().get_GUID() == guid)
				continue;
			send(F.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fighter ID " + f.get_id() + ": Send>>" + packet);
	}

	public static void GAME_SEND_ALTER_FIGHTER_MOUNT(Fight fight, Fighter fighter, int guid, int team, int otherteam) {
		StringBuilder packet = new StringBuilder();
		packet.append("GM|-").append(guid).append((char) 0x00).append(fighter.getGmPacket('~'));
		for (Fighter F : fight.getFighters(team)) {
			if (F.getPersonnage() == null || F.getPersonnage().get_compte().getGameThread() == null
					|| !F.getPersonnage().isOnline())
				continue;
			send(F.getPersonnage(), packet.toString());
		}
		if (otherteam > -1) {
			for (Fighter F : fight.getFighters(otherteam)) {
				if (F.getPersonnage() == null || F.getPersonnage().get_compte().getGameThread() == null
						|| !F.getPersonnage().isOnline())
					continue;
				send(F.getPersonnage(), packet.toString());
			}
		}

		Logs.addToGameSockLog("Game: Fight ID " + fight.get_id() + ": Send>>" + packet);
	}

	public static void GAME_SEND_ADD_PLAYER_TO_MAP(Maps map, Characters perso) {
		String packet = "GM|+" + perso.parseToGM();
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map " + map.get_id() + " (" + map.getPersos().size() + "): Send>>" + packet);
	}

	public static void GAME_SEND_DUEL_Y_AWAY(GameSendThread out, int guid) {
		String packet = "GA;903;" + guid + ";o";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_DUEL_E_AWAY(GameSendThread out, int guid) {
		String packet = "GA;903;" + guid + ";z";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_MAP_NEW_DUEL_TO_MAP(Maps map, int guid, int guid2) {
		String packet = "GA;900;" + guid + ";" + guid2;
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map " + map.get_id() + " (" + map.getPersos().size() + "): Send>>" + packet);
	}

	public static void GAME_SEND_CANCEL_DUEL_TO_MAP(Maps map, int guid, int guid2) {
		String packet = "GA;902;" + guid + ";" + guid2;
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map(" + map.getPersos().size() + "): Send>>" + packet);
	}

	public static void GAME_SEND_MAP_START_DUEL_TO_MAP(Maps map, int guid, int guid2) {
		String packet = "GA;901;" + guid + ";" + guid2;
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map(" + map.getPersos().size() + "): Send>>" + packet);
	}

	public static void GAME_SEND_MAP_FIGHT_COUNT(GameSendThread out, Maps map) {
		String packet = "fC" + map.getNbrFight();
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(Fight fight, int teams, int state, int cancelBtn, int duel,
			int spec, long time, int type) {
		StringBuilder packet = new StringBuilder();
		packet.append("GJK").append(state).append("|");
		packet.append(cancelBtn).append("|").append(duel).append("|");
		packet.append(spec).append("|").append(time).append("|").append(type);
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			send(f.getPersonnage(), packet.toString());
		}

		Logs.addToGameSockLog("Game: Map: Send>>" + packet.toString());
	}

	public static void GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(Fight fight, int teams, String places, int team) {
		String packet = "GP" + places + "|" + team;
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Map: Send>>" + packet);
	}

	public static void GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(Maps map) {
		String packet = "fC" + map.getNbrFight();
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map: Send>>" + packet);
	}

	public static void GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(Maps map, int arg1, int guid1, int guid2, int cell1,
			String str1, int cell2, String str2) {
		StringBuilder packet = new StringBuilder();
		packet.append("Gc+").append(guid1).append(";").append(arg1).append("|").append(guid1).append(";").append(cell1)
				.append(";").append(str1).append("|").append(guid2).append(";").append(cell2).append(";").append(str2);
		for (Characters z : map.getPersos())
			send(z, packet.toString());

		Logs.addToGameSockLog("Game: Map: Send>>" + packet.toString());
	}

	public static void GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(Characters p, Maps map, int arg1, int guid1, int guid2,
			int cell1, String str1, int cell2, String str2) {
		StringBuilder packet = new StringBuilder();
		packet.append("Gc+").append(guid1).append(";").append(arg1).append("|").append(guid1).append(";").append(cell1)
				.append(";").append(str1).append("|").append(guid2).append(";").append(cell2).append(";").append(str2);
		send(p, packet.toString());

		Logs.addToGameSockLog("Game: Map: Send>>" + packet.toString());
	}

	public static void GAME_SEND_GAME_REMFLAG_PACKET_TO_MAP(Maps map, int guid) {
		String packet = "Gc-" + guid;
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map: Send>>" + packet);
	}

	public static void GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(Maps map, int teamID, Fighter perso) {
		StringBuilder packet = new StringBuilder();
		packet.append("Gt").append(teamID).append("|+").append(perso.getGUID()).append(";")
				.append(perso.getPacketsName()).append(";").append(perso.get_lvl());
		for (Characters z : map.getPersos())
			send(z, packet.toString());

		Logs.addToGameSockLog("Game: Map: Send>>" + packet.toString());
	}

	public static void GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(Characters p, Maps map, int teamID, Fighter perso) {
		StringBuilder packet = new StringBuilder();
		packet.append("Gt").append(teamID).append("|+").append(perso.getGUID()).append(";")
				.append(perso.getPacketsName()).append(";").append(perso.get_lvl());
		send(p, packet.toString());

		Logs.addToGameSockLog("Game: Map: Send>>" + packet.toString());
	}

	public static void GAME_SEND_REMOVE_IN_TEAM_PACKET_TO_MAP(Maps map, int teamID, Fighter perso) {
		StringBuilder packet = new StringBuilder();
		packet.append("Gt").append(teamID).append("|-").append(perso.getGUID()).append(";")
				.append(perso.getPacketsName()).append(";").append(perso.get_lvl());
		for (Characters z : map.getPersos())
			send(z, packet.toString());

		Logs.addToGameSockLog("Game: Map: Send>>" + packet.toString());
	}

	public static void GAME_SEND_MAP_MOBS_GMS_PACKETS_TO_MAP(Maps map) {
		String packet = map.getMobGroupGMsPackets(); // Un par un comme sa lors
														// du respawn :)
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map: Send>>" + packet);
	}

	public static void GAME_SEND_MAP_MOBS_GM_PACKET(Maps map, MobGroup current_Mobs) {
		if (!Main.isRunning)
			return;
		String packet = "GM|";
		packet += current_Mobs.parseGM();// Un par un comme sa lors du respawn
											// :)
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map: Send>>" + packet);
	}

	public static void GAME_SEND_MAP_GMS_PACKETS(Maps map, Characters _perso) {
		map.sendGMsPackets(_perso);
	}

	public static void GAME_SEND_MAP_GMS_PACKETS(Characters perso, String packet) {
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_ON_EQUIP_ITEM(Maps map, Characters _perso) {
		String packet = _perso.parseToOa();
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map: Send>>" + packet);
	}

	public static void GAME_SEND_ON_EQUIP_ITEM_FIGHT(Characters _perso, Fighter f, Fight F) {
		String packet = _perso.parseToOa();
		for (Fighter z : F.getFighters(f.getTeam2())) {
			if (z.getPersonnage() == null)
				continue;
			send(z.getPersonnage(), packet);
		}
		for (Fighter z : F.getFighters(f.getOtherTeam())) {
			if (z.getPersonnage() == null)
				continue;
			send(z.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Map: Send>>" + packet);
	}

	public static void GAME_SEND_FIGHT_CHANGE_PLACE_PACKET_TO_FIGHT(Fight fight, int teams, Maps map, int guid,
			int cell) {
		String packet = "GIC|" + guid + ";" + cell + ";1";
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(Maps map, char s, char option, int guid) {
		String packet = "Go" + s + option + guid;
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_FIGHT_PLAYER_READY_TO_FIGHT(Fight fight, int teams, int guid, boolean b) {
		String packet = "GR" + (b ? "1" : "0") + guid;
		if (fight.get_state() != 2)
			return;
		for (Fighter f : fight.getFighters(teams)) {
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			if (f.hasLeft())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight: Send>>" + packet);
	}

	public static void GAME_SEND_GJK_PACKET(Characters perso, int state, int cancelBtn, int duel, int spec, long time,
			int unknown) {
		StringBuilder packet = new StringBuilder();
		packet.append("GJK").append(state).append("|").append(cancelBtn).append("|").append(duel).append("|")
				.append(spec).append("|").append(time).append("|").append(unknown);
		send(perso, packet.toString());

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_FIGHT_PLACES_PACKET(GameSendThread out, String places, int team) {
		String packet = "GP" + places + "|" + team;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_Im_PACKET_TO_ALL(String s) {
		String packet = "Im" + s;
		for (Characters perso : World.getOnlinePersos())
			send(perso, packet);

		Logs.addToGameSockLog("Game: Send ALL(" + World.getOnlinePersos().size() + ") >> " + packet);
	}

	public static void GAME_SEND_Im_PACKET(Characters out, String str) {
		String packet = "Im" + str;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_ILS_PACKET(Characters perso, int i) {
		String packet = "ILS" + i;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_ILF_PACKET(Characters perso, int i) {
		String packet = "ILF" + i;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void SEND_Im1223_MESSAGE_TO_ALL(String str) {
		String packet = "Im1223;" + str;
		for (Characters perso : World.getOnlinePersos())
			send(perso, packet);

		Logs.addToGameSockLog("Game: Send ALL(" + World.getOnlinePersos().size() + ") >> " + packet);
	}

	public static void GAME_SEND_Im_PACKET_TO_MAP(Maps map, String id) {
		String packet = "Im" + id;
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map: Send MAP(" + map.getPersos().size() +")>>" + packet);
	}

	public static void GAME_SEND_eUK_PACKET_TO_MAP(Maps map, int guid, int emote) {
		String packet = "eUK" + guid + "|" + emote;
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map: Send>>" + packet);
	}

	public static void GAME_SEND_Im_PACKET_TO_FIGHT(Fight fight, int teams, String id) {
		String packet = "Im" + id;
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Map: Send>>" + packet);
	}

	public static void GAME_SEND_MESSAGE(Characters perso, String mess, String color) {
		String packet = "cs<font color='#" + color + "'>" + mess + "</font>";
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_MESSAGE_TO_MAP(Maps map, String mess, String color) {
		String packet = "cs<font color='#" + color + "'>" + mess + "</font>";
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map: Send>>" + packet);
	}

	public static void GAME_SEND_GA903_ERROR_PACKET(GameSendThread out, char c, int guid) {
		String packet = "GA;903;" + guid + ";" + c;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_GIC_PACKETS_TO_FIGHT(Fight fight, int teams) {
		StringBuilder packet = new StringBuilder();
		packet.append("GIC|");
		for (Fighter p : fight.getFighters(3)) {
			if (p.get_fightCell() == null)
				continue;
			packet.append(p.getGUID()).append(";").append(p.get_fightCell().getID()).append(";1|");
		}
		for (Fighter perso : fight.getFighters(teams)) {
			if (perso.hasLeft())
				continue;
			if (perso.getPersonnage() == null || !perso.getPersonnage().isOnline())
				continue;
			send(perso.getPersonnage(), packet.toString());
		}

		Logs.addToGameSockLog("Game: Fight: Send>>" + packet.toString());
	}

	public static void GAME_SEND_GIC_PACKET_TO_FIGHT(Fight fight, int teams, Fighter f) {
		StringBuilder packet = new StringBuilder();
		packet.append("GIC|").append(f.getGUID()).append(";").append(f.get_fightCell().getID()).append(";1|");

		for (Fighter perso : fight.getFighters(teams)) {
			if (perso.hasLeft())
				continue;
			if (perso.getPersonnage() == null || !perso.getPersonnage().isOnline())
				continue;
			send(perso.getPersonnage(), packet.toString());
		}

		Logs.addToGameSockLog("Game: Fight: Send>>" + packet.toString());
	}

	public static void GAME_SEND_GS_PACKET_TO_FIGHT(Fight fight, int teams) {
		String packet = "GS";
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			f.initBuffStats();
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight : Send>>" + packet);
	}

	public static void GAME_SEND_GS_PACKET(Characters perso) {
		String packet = "GS";
		send(perso, packet);

		Logs.addToGameSockLog("Game: Fight : Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_GTL_PACKET_TO_FIGHT(Fight fight, int teams) {
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), fight.getGTL());
		}

		Logs.addToGameSockLog("Game: Fight : Send>>" + fight.getGTL());
	}

	public static void GAME_SEND_GTL_PACKET(Characters perso, Fight fight) {
		String packet = fight.getGTL();
		send(perso, packet);

		Logs.addToGameSockLog("Game: Fight : Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_GTM_PACKET_TO_FIGHT(Fight fight, int teams) {
		StringBuilder packet = new StringBuilder();
		packet.append("GTM");
		for (Fighter f : fight.getFighters(3)) {
			packet.append("|").append(f.getGUID()).append(";");
			if (f.isDead()) {
				packet.append("1");
				continue;
			} else
				packet.append("0;").append(f.getPDV() + ";").append(f.getPA() + ";").append(f.getPM() + ";");
			packet.append((f.isHide() ? "-1" : f.get_fightCell().getID())).append(";");// On envoie pas la cell d'un invisible :p
			packet.append(";");// ??
			packet.append(f.getPDVMAX());
		}
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet.toString());
		}

		Logs.addToGameSockLog("Game: Fight : Send>>" + packet.toString());
	}

	public static void GAME_SEND_GAMETURNSTART_PACKET_TO_FIGHT(Fight fight, int teams, int guid, int time) {
		String packet = "GTS" + guid + "|" + time;
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight : Send>>" + packet);
	}

	public static void GAME_SEND_GAMETURNSTART_PACKET(Characters perso, int guid, int time) {
		String packet = "GTS" + guid + "|" + time;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Fight : Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_GV_PACKET(Characters perso) {
		String packet = "GV";
		send(perso, packet);

		Logs.addToGameSockLog("Game: Fight : Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_PONG(GameSendThread out) {
		String packet = "pong";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_QPONG(GameSendThread out) {
		String packet = "qpong";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_GAS_PACKET_TO_FIGHT(Fight fight, int teams, int guid) {
		String packet = "GAS" + guid;
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight : Send>>" + packet);
	}

	public static void GAME_SEND_GA_PACKET_TO_FIGHT(Fight fight, int teams, int actionID, String s1, String s2) {
		String packet = "GA;" + actionID + ";" + s1;
		if (!s2.equals(""))
			packet += ";" + s2;

		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight(" + fight.getFighters(teams).size() + ") : Send>>" + packet);
	}

	public static void GAME_SEND_GA_PACKET_TO_FIGHT(Fight fight, int teams, int actionID, int s1, String s2) {
		String packet = "GA;" + actionID + ";" + s1;
		if (!s2.equals(""))
			packet += ";" + s2;

		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight(" + fight.getFighters(teams).size() + ") : Send>>" + packet);
	}

	public static void GAME_SEND_GA_PACKET(GameSendThread out, String actionID, String s0, String s1, String s2) {
		String packet = "GA" + actionID + ";" + s0;
		if (!s1.equals(""))
			packet += ";" + s1;
		if (!s2.equals(""))
			packet += ";" + s2;

		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_GA_PACKET_TO_FIGHT(Fight fight, int teams, int gameActionID, String s1, String s2,
			String s3) {
		String packet = "GA" + gameActionID + ";" + s1 + ";" + s2 + ";" + s3;
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight : Send>>" + packet);
	}

	public static void GAME_SEND_GAMEACTION_TO_FIGHT(Fight fight, int teams, String packet) {
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight : Send>>" + packet);
	}

	public static void GAME_SEND_GAF_PACKET_TO_FIGHT(Fight fight, int teams, int i1, int guid) {
		String packet = "GAF" + i1 + "|" + guid;
		for (Fighter f : fight.getFighters(teams)) {
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight : Send>>" + packet);
	}

	public static void GAME_SEND_BN(Characters perso) {
		String packet = "BN";
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_BN(GameSendThread out) {
		String packet = "BN";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_GAMETURNSTOP_PACKET_TO_FIGHT(Fight fight, int teams, int guid) {
		String packet = "GTF" + guid;
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight : Send>>" + packet);
	}

	public static void GAME_SEND_GTR_PACKET_TO_FIGHT(Fight fight, int teams, int guid) {
		String packet = "GTR" + guid;
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight : Send>>" + packet);
	}

	public static void GAME_SEND_EMOTICONE_TO_MAP(Maps map, int guid, int id) {
		String packet = "cS" + guid + "|" + id;
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map: Send>>" + packet);
	}

	public static void GAME_SEND_SPELL_UPGRADE_FAILED(GameSendThread _out) {
		String packet = "SUE";
		send(_out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_SPELL_UPGRADE_SUCCED(GameSendThread _out, int spellID, int level) {
		String packet = "SUK" + spellID + "~" + level;
		send(_out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_SPELL_LIST(Characters perso) {
		String packet = perso.parseSpellList();
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_FIGHT_PLAYER_DIE_TO_FIGHT(Fight fight, int teams, int guid) {
		String packet = "GA;103;" + guid + ";" + guid;
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft() || f.getPersonnage() == null)
				continue;
			if (f.getPersonnage().isOnline())
				send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight : Send>>" + packet);
	}

	public static void GAME_SEND_FIGHT_GE_PACKET_TO_FIGHT(Fight fight, int teams, int win) {
		String packet = fight.GetGE(win);
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft() || f.getPersonnage() == null)
				continue;
			if (f.getPersonnage().isOnline())
				send(f.getPersonnage(), packet);
			// After
			/**
			 * Personnage perso = f.getPersonnage();
			 * SocketManager.GAME_SEND_GV_PACKET(perso); perso.set_duelID(-1);
			 * perso.set_ready(false); perso.fullPDV(); perso.set_fight(null);
			 * SocketManager.GAME_SEND_GV_PACKET(perso);
			 * SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.get_curCarte(),
			 * perso); perso.get_curCell().addPerso(perso);
			 **/
		}

		Logs.addToGameSockLog("Game: Fight : Send>>" + packet);
	}

	public static void GAME_SEND_FIGHT_GIE_TO_FIGHT(Fight fight, int teams, int mType, int cible, int value,
			String mParam2, String mParam3, String mParam4, int turn, int spellID) {
		StringBuilder packet = new StringBuilder();
		packet.append("GIE").append(mType).append(";").append(cible).append(";").append(value).append(";")
				.append(mParam2).append(";").append(mParam3).append(";").append(mParam4).append(";").append(turn)
				.append(";").append(spellID);
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft() || f.getPersonnage() == null)
				continue;
			if (f.getPersonnage().isOnline())
				send(f.getPersonnage(), packet.toString());
		}

		Logs.addToGameSockLog("Game: Fight : Send>>" + packet.toString());
	}

	public static void GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(Fight fight, int teams, Maps map) {
		String packet = map.getFightersGMsPackets();
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight: Send>>" + packet);
	}

	public static void GAME_SEND_MAP_FIGHT_GMS_PACKETS(Fight fight, Maps map, Characters _perso) {
		String packet = map.getFightersGMsPackets();
		send(_perso, packet);

		Logs.addToGameSockLog("Game: Fight: Send>>" + packet);
	}

	public static void GAME_SEND_FIGHT_PLAYER_JOIN(Fight fight, int teams, Fighter _fighter) {
		String packet = _fighter.getGmPacket('+');

		for (Fighter f : fight.getFighters(teams)) {
			if (f != _fighter) {
				if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
					continue;
				if (f.getPersonnage() != null && f.getPersonnage().get_compte().getGameThread() != null)
					send(f.getPersonnage(), packet);
			}
		}

		Logs.addToGameSockLog("Game: Fight: Send>>" + packet);
	}
	
	public static void GAME_SEND_PUB(String msg) {
		String packet = "cMKF|-1|Serveur|" + msg;
		for(Characters perso : World.getOnlinePersos())
			send(perso, packet);

		Logs.addToGameSockLog("Game: Send ALL(" + World.getOnlinePersos().size() + ") >> " + packet);
	}

	public static void GAME_SEND_cMK_PACKET(Characters perso, String suffix, int guid, String name, String msg) {
		String packet = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_FIGHT_LIST_PACKET(GameSendThread out, Maps map) {
		StringBuilder packet = new StringBuilder();
		packet.append("fL");
		for (Entry<Integer, Fight> entry : map.get_fights().entrySet()) {
			if (packet.length() > 2) {
				packet.append("|");
			}
			packet.append(entry.getValue().parseFightInfos());
		}
		send(out, packet.toString());

		Logs.addToGameSockLog("Game: Send >> " + packet.toString());
	}

	public static void GAME_SEND_cMK_PACKET_TO_MAP(Maps map, String suffix, int guid, String name, String msg) {
		String packet = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg;
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map: Send>>" + packet);
	}

	public static void GAME_SEND_cMK_PACKET_TO_GUILD(Guild g, String suffix, int guid, String name, String msg) {
		String packet = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg;
		for (Characters perso : g.getMembers()) {
			if (perso == null || !perso.isOnline())
				continue;
			send(perso, packet);
		}

		Logs.addToGameSockLog("Game: Guild: Send>>" + packet);
	}

	public static void GAME_SEND_cMK_PACKET_TO_ALL(String suffix, int guid, String name, String msg) {
		String packet = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg;
		for (Characters perso : World.getOnlinePersos())
			send(perso, packet);

		Logs.addToGameSockLog("Game: ALL(" + World.getOnlinePersos().size() + "): Send>>" + packet);
	}

	public static void GAME_SEND_cMK_PACKET_TO_ALIGN(String suffix, int guid, String name, String msg,
			Characters _perso) {
		String packet = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg;
		for (Characters perso : World.getOnlinePersos()) {
			if (perso.get_align() == _perso.get_align()) {
				send(perso, packet);
			}
		}

		Logs.addToGameSockLog("Game: ALL(" + World.getOnlinePersos().size() + "): Send>>" + packet);
	}

	public static void GAME_SEND_cMK_PACKET_TO_ADMIN(String suffix, int guid, String name, String msg) {
		String packet = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg;
		for (Characters perso : World.getOnlinePersos())
			if (perso.isOnline())
				if (perso.get_compte() != null)
					if (perso.get_compte().get_gmLvl() > 0)
						send(perso, packet);

		Logs.addToGameSockLog("Game: ALL(" + World.getOnlinePersos().size() + "): Send>>" + packet);
	}

	public static void GAME_SEND_cMK_PACKET_TO_FIGHT(Fight fight, int teams, String suffix, int guid, String name,
			String msg) {
		String packet = (new StringBuilder("cMK")).append(suffix).append("|").append(guid).append("|").append(name)
				.append("|").append(msg).toString();
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight: Send>>" + packet);
	}

	public static void GAME_SEND_GDZ_PACKET_TO_FIGHT(Fight fight, int teams, String suffix, int cell, int size,
			int unk) {
		String packet = "GDZ" + suffix + cell + ";" + size + ";" + unk;

		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight: Send>>" + packet);
	}

	public static void GAME_SEND_GDC_PACKET_TO_FIGHT(Fight fight, int teams, int cell) {
		String packet = "GDC" + cell;

		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight: Send>>" + packet);
	}

	public static void GAME_SEND_GDC_PACKET(Maps map, int cell) {
		String packet = "GDC" + cell + ";aaWaaaaaaa800;1";
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_GDC_PACKET(Maps map, String str) {
		String packet = "GDC" + str;
		for (Characters perso : map.getPersos())
			send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_GDF_PACKET(Maps map, String str) {
		String packet = "GDF|" + str;
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_GA2_PACKET(GameSendThread out, int guid) {
		String packet = "GA;2;" + guid + ";";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_CHAT_ERROR_PACKET(GameSendThread out, String name) {
		String packet = "cMEf" + name;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_eD_PACKET_TO_MAP(Maps map, int guid, int dir) {
		String packet = "eD" + guid + "|" + dir;
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map: Send>>" + packet);
	}

	public static void GAME_SEND_ECK_PACKET(Characters perso, int type, String str) {
		String packet = "ECK" + type;
		if (!str.equals(""))
			packet += "|" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_ECK_PACKET(GameSendThread out, int type, String str) {
		String packet = "ECK" + type;
		if (!str.equals(""))
			packet += "|" + str;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_ITEM_VENDOR_LIST_PACKET(Characters perso, NPC npc) {
		String packet = "EL" + npc.get_template().getItemVendorList();
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_ITEM_LIST_PACKET_PERCEPTEUR(GameSendThread out, Collector perco) {
		String packet = "EL" + perco.getItemPercepteurList();
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_ITEM_LIST_PACKET_SELLER(Characters p, Characters out) {
		String packet = "EL" + p.parseStoreItemsList();
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + out.get_name() + "(" + out.get_GUID() + ") ~ account : " + out.get_compte().get_name() + "(" + out.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_EV_PACKET(GameSendThread out) {
		String packet = "EV";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_EV_PACKET(Characters perso) {
		String packet = "EV";
		send(perso, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_DCK_PACKET(GameSendThread out, int id) {
		String packet = "DCK" + id;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_QUESTION_PACKET(GameSendThread out, String str) {
		String packet = "DQ" + str;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_END_DIALOG_PACKET(GameSendThread out) {
		String packet = "DV";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_CONSOLE_SUCCESS_PACKET(GameSendThread out, String mess) {
		String packet = "BAT2" + mess;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_CONSOLE_ERROR_PACKET(GameSendThread out, String mess) {
		String packet = "BAT1" + mess;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_CONSOLE_NEUTRAL_PACKET(GameSendThread out, String mess) {
		String packet = "BAT0" + mess;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_BUY_ERROR_PACKET(GameSendThread out) {
		String packet = "EBE";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_SELL_ERROR_PACKET(GameSendThread out) {
		String packet = "ESE";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_BUY_OK_PACKET(GameSendThread out) {
		String packet = "EBK";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_OBJECT_QUANTITY_PACKET(Characters perso, Objects obj) {
		String packet = "OQ" + obj.getGuid() + "|" + obj.getQuantity();
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_OAKO_PACKET(Characters perso, Objects obj) {
		String packet = "OAKO" + obj.parseItem();
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_ESK_PACKEt(Characters perso) {
		String packet = "ESK";
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_REMOVE_ITEM_PACKET(Characters perso, int guid) {
		String packet = "OR" + guid;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_DELETE_OBJECT_FAILED_PACKET(GameSendThread out) {
		String packet = "OdE";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_OBJET_MOVE_PACKET(Characters perso, Objects obj) {
		String packet = "OM" + obj.getGuid() + "|";
		if (obj.getPosition() != Constant.ITEM_POS_NO_EQUIPED)
			packet += obj.getPosition();

		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_EMOTICONE_TO_FIGHT(Fight fight, int teams, int guid, int id) {
		String packet = (new StringBuilder("cS")).append(guid).append("|").append(id).toString();
		for (Fighter f : fight.getFighters(teams)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight: Send>>" + packet);
	}

	public static void GAME_SEND_OAEL_PACKET(GameSendThread out) {
		String packet = "OAEL";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_NEW_LVL_PACKET(GameSendThread out, int lvl) {
		String packet = "AN" + lvl;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_MESSAGE_TO_ALL(String msg, String color) {
		String packet = "cs<font color='#" + color + "'>" + msg + "</font>";
		for (Characters P : World.getOnlinePersos()) {
			send(P, packet);
		}

		Logs.addToGameSockLog("Game: ALL (" + World.getOnlinePersos().size() + "): Send>>" + packet);
	}

	public static void GAME_SEND_EXCHANGE_REQUEST_OK(GameSendThread out, int guid, int guidT, int msgID) {
		String packet = "ERK" + guid + "|" + guidT + "|" + msgID;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_EXCHANGE_REQUEST_ERROR(GameSendThread out, char c) {
		String packet = "ERE" + c;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void PERSO_SEND_EXCHANGE_REQUEST_ERROR(Characters perso, char c) {
		String packet = "ERE" + c;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_EXCHANGE_CONFIRM_OK(GameSendThread out, int type) {
		String packet = "ECK" + type;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_EXCHANGE_MOVE_OK(Characters perso, char type, String signe, String s1) {
		String packet = "EMK" + type + signe;
		if (!s1.equals(""))
			packet += s1;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_EXCHANGE_MOVE_OK_FM(Characters perso, char type, String signe, String s1) {
		String packet = "EmK" + type + signe;
		if (!s1.equals(""))
			packet += s1;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_EXCHANGE_OTHER_MOVE_OK(GameSendThread out, char type, String signe, String s1) {
		String packet = "EmK" + type + signe;
		if (!s1.equals(""))
			packet += s1;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_EXCHANGE_OTHER_MOVE_OK_FM(GameSendThread out, char type, String signe, String s1) {
		String packet = "EMK" + type + signe;
		if (!s1.equals(""))
			packet += s1;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_EXCHANGE_OK(GameSendThread out, boolean ok, int guid) {
		String packet = "EK" + (ok ? "1" : "0") + guid;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_EXCHANGE_VALID(GameSendThread out, char c) {
		String packet = "EV" + c;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_GROUP_INVITATION_ERROR(GameSendThread out, String s) {
		String packet = "PIE" + s;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_GROUP_INVITATION(GameSendThread out, String n1, String n2) {
		String packet = "PIK" + n1 + "|" + n2;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_GROUP_CREATE(GameSendThread out, Group g) {
		String packet = "PCK" + g.getChief().get_name();
		send(out, packet);

		Logs.addToGameSockLog("Game: Groupe: Send>>" + packet);
	}

	public static void GAME_SEND_PL_PACKET(GameSendThread out, Group g) {
		String packet = "PL" + g.getChief().get_GUID();
		send(out, packet);

		Logs.addToGameSockLog("Game: Groupe: Send>>" + packet);
	}

	public static void GAME_SEND_PR_PACKET(Characters perso) {
		String packet = "PR";
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_PV_PACKET(GameSendThread out, String s) {
		String packet = "PV" + s;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_ALL_PM_ADD_PACKET(GameSendThread out, Group g) {
		StringBuilder packet = new StringBuilder();
		packet.append("PM+");
		boolean first = true;
		for (Characters p : g.getPersos()) {
			if (!first)
				packet.append("|");
			packet.append(p.parseToPM());
			first = false;
		}
		send(out, packet.toString());

		Logs.addToGameSockLog("Game: Send >> " + packet.toString());
	}

	public static void GAME_SEND_PM_ADD_PACKET_TO_GROUP(Group g, Characters p) {
		String packet = "PM+" + p.parseToPM();
		for (Characters P : g.getPersos())
			send(P, packet);

		Logs.addToGameSockLog("Game: Groupe: Send>>" + packet);
	}

	public static void GAME_SEND_PM_MOD_PACKET_TO_GROUP(Group g, Characters p) {
		String packet = "PM~" + p.parseToPM();
		for (Characters P : g.getPersos())
			send(P, packet);

		Logs.addToGameSockLog("Game: Groupe: Send>>" + packet);
	}

	public static void GAME_SEND_PM_DEL_PACKET_TO_GROUP(Group g, int guid) {
		String packet = "PM-" + guid;
		for (Characters P : g.getPersos())
			send(P, packet);

		Logs.addToGameSockLog("Game: Groupe: Send>>" + packet);
	}

	public static void GAME_SEND_cMK_PACKET_TO_GROUP(Group g, String s, int guid, String name, String msg) {
		String packet = "cMK" + s + "|" + guid + "|" + name + "|" + msg + "|";
		for (Characters P : g.getPersos())
			send(P, packet);

		Logs.addToGameSockLog("Game: Groupe: Send>>" + packet);
	}

	public static void GAME_SEND_FIGHT_DETAILS(GameSendThread out, Fight fight) {
		if (fight == null)
			return;
		StringBuilder packet = new StringBuilder();
		packet.append("fD").append(fight.get_id()).append("|");
		for (Fighter f : fight.getFighters(1))
			packet.append(f.getPacketsName()).append("~").append(f.get_lvl()).append(";");
		packet.append("|");
		for (Fighter f : fight.getFighters(2))
			packet.append(f.getPacketsName()).append("~").append(f.get_lvl()).append(";");
		send(out, packet.toString());

		Logs.addToGameSockLog("Game: Send >> " + packet.toString());
	}

	public static void GAME_SEND_IQ_PACKET(Characters perso, int guid, int qua) {
		String packet = "IQ" + guid + "|" + qua;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_JN_PACKET(Characters perso, int jobID, int lvl) {
		String packet = "JN" + jobID + "|" + lvl;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_GDF_PACKET_TO_MAP(Maps map, Case cell) {
		int cellID = cell.getID();
		InteractiveObject object = cell.getObject();
		String packet = "GDF|" + cellID + ";" + object.getState() + ";" + (object.isInteractive() ? "1" : "0");
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map(" + map.getPersos().size() + "): Send>>" + packet);
	}

	public static void GAME_SEND_GA_PACKET_TO_MAP(Maps map, String gameActionID, int actionID, String s1, String s2) {
		String packet = "GA" + gameActionID + ";" + actionID + ";" + s1;
		if (!s2.equals(""))
			packet += ";" + s2;

		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map(" + map.getPersos().size() + "): Send>>" + packet);
	}

	public static void GAME_SEND_EL_BANK_PACKET(Characters perso) {
		String packet = "EL" + perso.parseBankPacket();
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_EL_TRUNK_PACKET(Characters perso, Trunk t) {
		String packet = "EL" + t.parseToTrunkPacket();
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_JX_PACKET(Characters perso, ArrayList<StatsMetier> SMs) {
		StringBuilder packet = new StringBuilder();
		packet.append("JX");
		for (StatsMetier sm : SMs) {
			packet.append("|").append(sm.getTemplate().getId()).append(";").append(sm.get_lvl()).append(";")
					.append(sm.getXpString(";")).append(";");
		}
		send(perso, packet.toString());

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_JO_PACKET(Characters perso, ArrayList<StatsMetier> SMs) {
		for (StatsMetier sm : SMs) {
			String packet = "JO" + sm.getID() + "|" + sm.getOptBinValue() + "|2";// FIXME
																					// 2=?
			send(perso, packet);

			Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
			
		}
	}

	public static void GAME_SEND_JS_PACKET(Characters perso, ArrayList<StatsMetier> SMs) {
		String packet = "JS";
		for (StatsMetier sm : SMs) {
			packet += sm.parseJS();
		}
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_EsK_PACKET(Characters perso, String str) {
		String packet = "EsK" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_FIGHT_SHOW_CASE(ArrayList<GameSendThread> PWs, int guid, int cellID) {
		String packet = "Gf" + guid + "|" + cellID;
		for (GameSendThread PW : PWs) {
			send(PW, packet);
		}

		Logs.addToGameSockLog("Game: Fight: Send>>" + packet);
	}

	public static void GAME_SEND_Ea_PACKET(Characters perso, String str) {
		String packet = "Ea" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_EA_PACKET(Characters perso, String str) {
		String packet = "EA" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_Ec_PACKET(Characters perso, String str) {
		String packet = "Ec" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_Em_PACKET(Characters perso, String str) {
		String packet = "Em" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_IO_PACKET_TO_MAP(Maps map, int guid, String str) {
		String packet = "IO" + guid + "|" + str;
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map(" + map.getPersos().size() + "): Send>>" + packet);
	}

	public static void GAME_SEND_FRIENDLIST_PACKET(Characters perso) {
		String packet = "FL" + perso.get_compte().parseFriendList();
		send(perso, packet);
		if (perso.getWife() != 0) {
			String packet2 = "FS" + perso.get_wife_friendlist();
			send(perso, packet2);

			Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
			
		}

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_FRIEND_ONLINE(Characters logando, Characters perso) {
		String packet = "Im0143;" + logando.get_compte().get_pseudo()
				+ " (<b><a href='asfunction:onHref,ShowPlayerPopupMenu," + logando.get_name() + "'>"
				+ logando.get_name() + "</a></b>)";
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_FA_PACKET(Characters perso, String str) {
		String packet = "FA" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_FD_PACKET(Characters perso, String str) {
		String packet = "FD" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_Rp_PACKET(Characters perso, MountPark MP) {
		StringBuilder packet = new StringBuilder();
		if (MP == null)
			return;

		packet.append("Rp").append(MP.get_owner()).append(";").append(MP.get_price()).append(";").append(MP.get_size())
				.append(";").append(MP.getObjectNumb()).append(";");

		Guild G = MP.get_guild();
		// Si une guilde est definie
		if (G != null) {
			packet.append(G.get_name()).append(";").append(G.get_emblem());
		} else {
			packet.append(";");
		}

		send(perso, packet.toString());

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_OS_PACKET(Characters perso, int pano) {
		StringBuilder packet = new StringBuilder();
		packet.append("OS");
		int num = perso.getNumbEquipedItemOfPanoplie(pano);
		if (num <= 0)
			packet.append("-").append(pano);
		else {
			packet.append("+").append(pano).append("|");
			ItemSet IS = World.getItemSet(pano);
			if (IS != null) {
				StringBuilder items = new StringBuilder();
				// Pour chaque objet de la pano
				for (ObjTemplate OT : IS.getItemTemplates()) {
					// Si le joueur l'a équipé
					if (perso.hasEquiped(OT.getID())) {
						// On l'ajoute au packet
						if (items.length() > 0)
							items.append(";");
						items.append(OT.getID());
					}
				}
				packet.append(items.toString()).append("|")
						.append(IS.getBonusStatByItemNumb(num).parseToItemSetStats());
			}
		}
		send(perso, packet.toString());

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_MOUNT_DESCRIPTION_PACKET(Characters perso, Mount DD) {
		String packet = "Rd" + DD.parse();
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_Rr_PACKET(Characters perso, String str) {
		String packet = "Rr" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_ALTER_GM_PACKET(Maps map, Characters perso) {
		String packet = "GM|~" + perso.parseToGM();
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map(" + map.getPersos().size() + "): Send>>" + packet);
	}

	public static void GAME_SEND_Ee_PACKET(Characters perso, char c, String s) {
		String packet = "Ee" + c + s;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_cC_PACKET(Characters perso, char c, String s) {
		String packet = "cC" + c + s;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_ADD_NPC_TO_MAP(Maps map, NPC npc) {
		String packet = "GM|" + npc.parseGM();
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToDebug("Game: Map(" + map.getPersos().size() + "): Send>>" + packet);
	}

	public static void GAME_SEND_ADD_PERCO_TO_MAP(Maps map) {
		String packet = "GM|" + Collector.parseGM(map);
		for (Characters z : map.getPersos())
			send(z, packet);
		Logs.addToGameSockLog("Game: Map(" + map.getPersos().size() + "): Send>>" + packet);
	}
	
	public static void GAME_SEND_ATTACK_DEATH_PERCO(Maps map){
		String packet = "GD|" + Collector.parseGM(map);
		for (Characters z : map.getPersos())
			send(z, packet);
		Logs.addToGameLog("Game: Map(" + map.getPersos().size() + "): Send>> " + packet); 
	}

	public static void GAME_SEND_GDO_PACKET_TO_MAP(Maps map, char c, int cell, int itm, int i) {
		String packet = "GDO" + c + cell + ";" + itm + ";" + i;
		for (Characters z : map.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Map(" + map.getPersos().size() + "): Send>>" + packet);
	}

	public static void GAME_SEND_GDO_PACKET(Characters perso, char c, int cell, int itm, int i) {
		String packet = "GDO" + c + cell + ";" + itm + ";" + i;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID() + ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_ZC_PACKET(Characters p, int a) {
		String packet = "ZC" + a;
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID() + ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() +")");
		
	}

	public static void GAME_SEND_GIP_PACKET(Characters p, int a) {
		String packet = "GIP" + a;
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID() + ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_gn_PACKET(Characters p) {
		String packet = "gn";
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID() + ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_gC_PACKET(Characters p, String s) {
		String packet = "gC" + s;
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID() + ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_gV_PACKET(Characters p) {
		String packet = "gV";
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID() + ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() +")");
	}

	public static void GAME_SEND_gIM_PACKET(Characters p, Guild g, char c) {
		String packet = "gIM" + c;
		switch (c) {
		case '+':
			packet += g.parseMembersToGM();
			break;
		}
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
				+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_gIB_PACKET(Characters p, String infos) {
		String packet = "gIB" + infos;
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_gIH_PACKET(Characters p, String infos) {
		String packet = "gIH" + infos;
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_gS_PACKET(Characters p, GuildMember gm) {
		StringBuilder packet = new StringBuilder();
		packet.append("gS").append(gm.getGuild().get_name()).append("|")
				.append(gm.getGuild().get_emblem().replace(',', '|')).append("|").append(gm.parseRights());
		send(p, packet.toString());

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_gJ_PACKET(Characters p, String str) {
		String packet = "gJ" + str;
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_gK_PACKET(Characters p, String str) {
		String packet = "gK" + str;
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_gIG_PACKET(Characters p, Guild g) {
		long xpMin = World.getExpLevel(g.get_lvl()).guilde;
		long xpMax;
		if (World.getExpLevel(g.get_lvl() + 1) == null) {
			xpMax = -1;
		} else {
			xpMax = World.getExpLevel(g.get_lvl() + 1).guilde;
		}
		StringBuilder packet = new StringBuilder();
		packet.append("gIG").append((g.getSize() > 9 ? 1 : 0)).append("|").append(g.get_lvl()).append("|").append(xpMin)
				.append("|").append(g.get_xp()).append("|").append(xpMax);
		send(p, packet.toString());

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	// args : 0 = Spam | 1 = Inactif | 2 = Lvl max atteint | 3 = Inactif | 
	// 4 = Interrompu pour maintenance | 5 = Achat maison | 6 = Max objet 
	// 7 = Opération nn auto | 8 = Objet non dispo | 9 = Magazin vide
	// 10 = Interrompu | 11 = Interrompu
	public static void REALM_SEND_MESSAGE(GameSendThread out, String args) {
		String packet = "M0" + args;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_WC_PACKET(Characters p) {
		String packet = "WC" + p.parseZaapList();
		send(p.get_compte().getGameThread().get_out(), packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_WV_PACKET(Characters p) {
		String packet = "WV";
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_ZAAPI_PACKET(Characters p, String list) {
		String packet = "Wc" + p.get_curCarte().get_id() + "|" + list;
		send(p, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_CLOSE_ZAAPI_PACKET(Characters p) {
		String packet = "Wv";
		send(p, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_WUE_PACKET(Characters p) {
		String packet = "WUE";
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_EMOTE_LIST(Characters p, String s, String s1) {
		String packet = "eL" + s + "|" + s1;
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_NO_EMOTE(Characters p) {
		String packet = "eUE";
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_ADD_ENEMY(Characters p, Characters enemie) {

		String packet = "iAK" + enemie.get_compte().get_name() + ";2;" + enemie.get_name() + ";36;10;0;100.FL.";
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_iAEA_PACKET(Characters p) {

		String packet = "iAEA.";
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_ENEMY_LIST(Characters p) {

		String packet = "iL" + p.get_compte().parseEnemyList();
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_iD_COMMANDE(Characters p, String str) {
		String packet = "iD" + str;
		send(p, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_BWK(Characters perso, String str) {
		String packet = "BWK" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
				+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_KODE(Characters perso, String str) {
		String packet = "K" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_hOUSE(Characters perso, String str) {
		String packet = "h" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");

	}

	public static void GAME_SEND_FORGETSPELL_INTERFACE(char sign, Characters perso) {
		String packet = "SF" + sign;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_R_PACKET(Characters perso, String str) {
		String packet = "R" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_gIF_PACKET(Characters perso, String str) {
		String packet = "gIF" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_gITM_PACKET(Characters perso, String str) {
		String packet = "gITM" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_gITp_PACKET(Characters perso, String str) {
		String packet = "gITp" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_gITP_PACKET(Characters perso, String str) {
		String packet = "gITP" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_IH_PACKET(Characters perso, String str) {
		String packet = "IH" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_FLAG_PACKET(Characters perso, Characters cible) {
		String packet = "IC" + cible.get_curCarte().getX() + "|" + cible.get_curCarte().getY();
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_DELETE_FLAG_PACKET(Characters perso) {
		String packet = "IC|";
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_gT_PACKET(Characters perso, String str) { //Perco packet (pos/retirer)
		String packet = "gT" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}
	
	public static void GAME_SEND_gA_PACKET(Characters perso, String str) { //Perco fight packet (attack/survive/death)
		String packet = "gA" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_GUILDHOUSE_PACKET(Characters perso) {
		String packet = "gUT";
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_GUILDENCLO_PACKET(Characters perso) {
		String packet = "gUF";
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	/** HDV **/
	public static void GAME_SEND_EHm_PACKET(Characters out, String sign, String str) {
		String packet = "EHm" + sign + str;

		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + out.get_name() + "(" + out.get_GUID()
				+ ") ~ account : " + out.get_compte().get_name() + "(" + out.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_EHM_PACKET(Characters out, String sign, String str) {
		String packet = "EHM" + sign + str;

		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + out.get_name() + "(" + out.get_GUID()
		+ ") ~ account : " + out.get_compte().get_name() + "(" + out.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_EHP_PACKET(Characters out, int templateID) // Packet
																			// d'envoie
																			// du
																			// prix
																			// moyen
																			// du
																			// template
																			// (En
																			// réponse
																			// a
																			// un
																			// packet
																			// EHP)
	{

		String packet = "EHP" + templateID + "|" + World.getObjTemplate(templateID).getAvgPrice();

		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + out.get_name() + "(" + out.get_GUID()
		+ ") ~ account : " + out.get_compte().get_name() + "(" + out.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_EHl(Characters out, AuctionHouse seller, int templateID) {
		String packet = "EHl" + seller.parseToEHl(templateID);
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + out.get_name() + "(" + out.get_GUID()
		+ ") ~ account : " + out.get_compte().get_name() + "(" + out.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_EHL_PACKET(Characters out, int categ, String templates) // Packet
																							// de
																							// listage
																							// des
																							// templates
																							// dans
																							// une
																							// catégorie
																							// (En
																							// réponse
																							// au
																							// packet
																							// EHT)
	{
		String packet = "EHL" + categ + "|" + templates;

		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + out.get_name() + "(" + out.get_GUID()
		+ ") ~ account : " + out.get_compte().get_name() + "(" + out.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_EHL_PACKET(Characters out, String items) // Packet
																			// de
																			// listage
																			// des
																			// objets
																			// en
																			// vente
	{
		String packet = "EHL" + items;

		send(out, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + out.get_name() + "(" + out.get_GUID()
		+ ") ~ account : " + out.get_compte().get_name() + "(" + out.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_HDVITEM_SELLING(Characters perso) {
		String packet = "EL";
		HdvEntry[] entries = perso.get_compte().getHdvItems(Math.abs(perso.get_isTradingWith())); // Récupère
																									// un
																									// tableau
																									// de
																									// tout
																									// les
																									// items
																									// que
																									// le
																									// personnage
																									// à
																									// en
																									// vente
																									// dans
																									// l'HDV
																									// où
																									// il
																									// est
		boolean isFirst = true;
		for (HdvEntry curEntry : entries) {
			if (curEntry == null)
				break;
			if (!isFirst)
				packet += "|";
			packet += curEntry.parseToEL();

			isFirst = false;
		}
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_WEDDING(Maps c, int action, int homme, int femme, int parlant) {
		String packet = "GA;" + action + ";" + homme + ";" + homme + "," + femme + "," + parlant;
		Characters Homme = World.getPersonnage(homme);
		send(Homme, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_PF(Characters perso, String str) {
		String packet = "PF" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_MERCHANT_LIST(Characters perso, short mapID) {
		StringBuilder packet = new StringBuilder();
		packet.append("GM|~");
		if (World.getSeller(perso.get_curCarte().get_id()) == null)
			return;
		for (Integer pID : World.getSeller(perso.get_curCarte().get_id())) {
			if (!World.getPersonnage(pID).isOnline() && World.getPersonnage(pID).is_showSeller()) {
				packet.append(World.getPersonnage(pID).parseToMerchant()).append("|");
			}
		}
		if (packet.length() < 5)
			return;
		send(perso, packet.toString());

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_cMK_PACKET_INCARNAM_CHAT(Characters perso, String suffix, int guid, String name,
			String msg) {
		String packet = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg;
		/*
		if (perso.get_lvl() > 15) {
			GAME_SEND_BN(perso);
			return;
		}
		*/
		for (Characters perso1 : World.getOnlinePersos()) {
			send(perso1, packet);
		}

		Logs.addToGameSockLog("Game: ALL(" + World.getOnlinePersos().size() + "): Send>>" + packet);
	}

	public static void GAME_SEND_PACKET_TO_FIGHT(Fight fight, int i, String packet) {
		for (Fighter f : fight.getFighters(i)) {
			if (f.hasLeft())
				continue;
			if (f.getPersonnage() == null || !f.getPersonnage().isOnline())
				continue;
			send(f.getPersonnage(), packet);
		}

		Logs.addToGameSockLog("Game: Fight : Send>>" + packet);
	}

	public static void GAME_SEND_CIN_Packet(Characters perso, String num) { // Jouer cinématique
		String packet = "GA;2;" + perso.get_GUID() + ";" + num;
		send(perso, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_TB_Packet(GameSendThread out) { // Jouer la cinématique de départ
		String packet = "TB";
		send(out, packet);
		Logs.addToGameLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(Characters perso, Objects item) {
		StringBuilder packet = new StringBuilder();
		packet.append("OCO").append(item.parseItem());
		send(perso, packet.toString());

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_Carte_ACTUALIZAR_CELDA(Maps Carte, String str) { // Actualiser une cellule
		String packet = "GDO+" + str;
		for (Characters z : Carte.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Send >>   " + packet);
	}

	//GAME_SEND
	public static void ENVIAR_GDO_PONER_OBJETO_CRIA_EN_MAPA(Maps mapa, String str) { // Actualiser une cellule 3.0
		String packet = "GDO+" + str;
		for (Characters z : mapa.getPersos())
			send(z, packet);

		Logs.addToGameSockLog("Game: Send >>  " + packet);
	}

	public static void GAME_SEND_DELETE_STATS_ITEM_FM(Characters perso, int id) {
		String packet = "OR" + id;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_Ew_PACKET(Characters perso, int pods, int podsMax) { // Pods de la dinde
		String packet = "Ew" + pods + ";" + podsMax + "";
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_BAIO_PACKET(Characters perso, String str) { // Afficher le panel d'info
		String packet = "BAIO" + str;
		if (perso.get_compte().get_gmLvl() > 0)
			send(perso, packet);
		else
			perso.get_compte().getGameThread().kick();

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}
	
	public static void GAME_SEND_BAIC_PACKET(Characters perso, String str) { // Afficher le panel d'info
		String packet = "BAIC" + str;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_EJ_PACKET(Characters perso, int metid, int pid, StatsMetier sm) { // Regarder un livre de métier
		
		Characters p = World.getPersonnage(pid);
		if (p == null)
			return;
		String a = p.parse_tojobbook(metid);
		if (a == null)
			return;
		String packet = "EJ+" + metid + ";" + pid + ";" + a;
		send(perso, packet);
		
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_Ag_PACKET(GameSendThread out, int idObject, String codeObject) { // Cadeau à la connexion
		String packet = "Ag1|" + idObject + "|Cadeau !!!| Voilà un joli cadeau pour vous ! "
				+ "Un jeune aventurier comme vous sera sans servir de la meilleur façon ! "
				+ "Bonne continuation avec ceci ! |DOFUS|" + codeObject;
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >>  " + packet);
	}

	public static void GAME_SEND_AGK_PACKET(GameSendThread out) { // Cadeau à la connexion
		String packet = "AGK";
		send(out, packet);

		Logs.addToGameSockLog("Game: Send >>  " + packet);
	}

	public static void GAME_SEND_dCK_PACKET(Characters perso, String id) // Ouvrir
																		// un
																		// livre
	{
		String packet = "dCK" + id;
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_SB_PACKET(Characters p, Characters.BoostSpellStats stats, boolean isAdd) {
		StringBuilder packet = new StringBuilder();
		boolean isFirst = false;
		Characters.BoostSpellStats trueStats = p.getTotalBoostSpellStats();
		int trueval;
		if (!isAdd) {
			for (Entry<Integer, Map<Integer, Integer>> entry : stats.getAllEffects().entrySet()) {
				if (entry == null || entry.getValue() == null)
					continue;
				for (Entry<Integer, Integer> stat : entry.getValue().entrySet()) {
					if (!isFirst)
						packet.append("\0");
					packet.append("SB").append(stat.getKey()).append(";").append(entry.getKey()).append(";-1");
					isFirst = false;
				}
			}
		} else {
			for (Entry<Integer, Map<Integer, Integer>> entry : stats.getAllEffects().entrySet()) {
				if (entry == null || entry.getValue() == null)
					continue;
				for (Entry<Integer, Integer> stat : entry.getValue().entrySet()) {
					if (!isFirst)
						packet.append("\0");
					switch (stat.getKey()) {
					case Constant.STATS_BOOST_SPELL_CASTOUTLINE:
					case Constant.STATS_BOOST_SPELL_NOLINEOFSIGHT:
					case Constant.STATS_BOOST_SPELL_RANGEABLE:
						packet.append("SB").append(stat.getKey()).append(";").append(entry.getKey()).append(";1");
						break;
					default:
						trueval = trueStats.getStat(entry.getKey(), stat.getKey());
						packet.append("SB").append(stat.getKey()).append(";").append(entry.getKey()).append(";")
								.append(trueval);
					}
					isFirst = false;
				}
			}
		}
		send(p, packet.toString());

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + p.get_name() + "(" + p.get_GUID()
		+ ") ~ account : " + p.get_compte().get_name() + "(" + p.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_FIGHT_PLAYER_JOIN(Characters perso, Fighter f) {
		String packet = f.getGmPacket('+');
		send(perso, packet);

		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_MOUNT_PODS(Characters perso, int pods) {
		String packet = "Ew" + pods + ";1000";
		send(perso, packet);
		
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_EL_MOUNT_INVENTAIRE(GameSendThread _out, Mount DD) {
		String packet = "EL" + DD.getInventaire();
		send(_out, packet);
		
		Logs.addToGameSockLog("Game: Send >> " + packet);
	}

	public static void GAME_SEND_OCO_PACKET(Characters perso, Objects obj) {
		String packet = "OCO" + obj.parseItem();
		send(perso, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}

	public static void GAME_SEND_Eq_PACKET(Characters perso, long Taxe) {
		String packet = "Eq|1|" + Taxe;
		send(perso, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}
	
	public static void GAME_SEND_POPUP(Characters perso, String msg) {
		String packet = "LP" + msg;
		send(perso, packet);
		Logs.addToGameSockLog("Game: Send >> " + packet.toString() + " to " + perso.get_name() + "(" + perso.get_GUID()
		+ ") ~ account : " + perso.get_compte().get_name() + "(" + perso.get_compte().get_GUID() + ")");
	}
	
	public static void GAME_SEND_POPUP_TO_All(String msg) {
        String packet = "LP" + msg;
        for (Characters P : World.getOnlinePersos()) {
            send(P, packet);
        }
        Logs.addToGameLog("Game: Send ALL(" + World.getOnlinePersos() +") >> " + packet);
    }

}

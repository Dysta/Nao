package org.game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.check.CheatCheck;
import org.check.FloodCheck;
import org.check.IpCheck;
import org.client.Accounts;
import org.client.Characters;
import org.client.Characters.Group;
import org.command.GmCommand;
import org.command.PlayerCommand;
import org.common.ConditionParser;
import org.common.Constant;
import org.common.CryptManager;
import org.common.Formulas;
import org.common.Pathfinding;
import org.common.SQLManager;
import org.common.SocketManager;
import org.common.World;
import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.extending.Arena;
import org.fight.extending.Kolizeum;
import org.fight.extending.Team;
import org.fight.object.Collector;
import org.fight.object.Prism;
import org.fight.object.Stalk;
import org.fight.object.Stalk.Traque;
import org.game.tools.ParseTools;
import org.kernel.*;
import org.object.AuctionHouse;
import org.object.Guild;
import org.object.Houses;
import org.object.Maps;
import org.object.Mount;
import org.object.NpcTemplates;
import org.object.Objects;
import org.object.Trunk;
import org.object.AuctionHouse.HdvEntry;
import org.object.Guild.GuildMember;
import org.object.Maps.Case;
import org.object.Maps.MountPark;
import org.object.NpcTemplates.NPC;
import org.object.NpcTemplates.NPC_question;
import org.object.NpcTemplates.NPC_reponse;
import org.object.Objects.ObjTemplate;
import org.object.job.Job.StatsMetier;
import org.simplyfilter.filter.Filter;
import org.simplyfilter.filter.Filters;
import org.spell.Spell.SortStats;

import vpn.detection.VPNDetection;

public class GameThread implements Runnable { 
	
	private BufferedReader _in;
	private Thread _t;
	private Filter filter = Filters.createNewSafe(20, 500); //20 packets authorize in 0.5s
	private GameSendThread _out;
	private Socket _s;
	private Accounts _compte;
	private Characters _perso;
	private Map<Integer,GameAction> _actions = new TreeMap<Integer,GameAction>();
	private long _timeLastTradeMsg = 0, _timeLastRecrutmentMsg = 0, _timeLastsave = 0, _timeLastAlignMsg = 0;
	private long _timeLastIncarnamMsg = 0;
	private GmCommand command;
	private GameAction GA_wait;
	private int wait = 0;
	
	public static class GameAction {
		public int _id;
		public int _actionID;
		public String _packet;
		public String _args;
		
		public GameAction(int aId, int aActionId,String aPacket) {
			_id = aId;
			_actionID = aActionId;
			_packet = aPacket;
		}
	}
	
	/**
	 * 
	 * @param sock
	 */
	public GameThread(Socket sock) {
		try {
			_s = sock;
			_in = new BufferedReader(new InputStreamReader(_s.getInputStream()));

			try {
				_out = new GameSendThread(this, _s, new PrintWriter(_s.getOutputStream()));
			} catch (Exception e) {
				e.printStackTrace();
			}
			_t = new Thread(Main.THREAD_GAME, this);
			_t.setDaemon(true);
			try {
				_t.start();
			} catch (OutOfMemoryError e) {
				Logs.addToDebug("OutOfMemory dans le Game");
				e.printStackTrace();
				try {
					Main.listThreads(true);
				} catch (Exception ed) {
				}
				try {
					_t.start();
				} catch (OutOfMemoryError e1) {
				}
			}
		}

		catch (IOException e) {
			try {
				Logs.addToGameLog(e.getMessage());
				if (!_s.isClosed())
					_s.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public void run() {
		try {
			String packet = "";
			char charCur[] = new char[1];
			SocketManager.GAME_SEND_HELLOGAME_PACKET(_out);
			
			int result;
			
			synchronized(_in) {
				result = _in.read(charCur, 0, 1);
			}
			
	    	while(result!=-1 && Main.isRunning) {
	    		if(Thread.interrupted()){
	    			try { throw new InterruptedException();
	    			} catch(InterruptedException ie) { 
	    				Logs.addToDebug("GameThread interrompu ");
	    			}
	    		}
	    		
	    		if (charCur[0] != '\u0000' && charCur[0] != '\n' && charCur[0] != '\r')
	    			packet += charCur[0];
	    		else if(!packet.isEmpty()) {
	    			packet = CryptManager.toUnicode(packet);
	    			if(_perso != null){
	    				Logs.addToGameSockLog("Game: Recv << account(" + _compte.get_GUID() + ") ~ player(" + _perso.get_GUID()+ ") - packet : " + packet);
                    }else{
                        if(_compte != null){
                        	Logs.addToGameSockLog("Game: Recv << account(" + _compte.get_GUID() + ") ~ player(unedfined) - packet : " + packet);
                        }else{
                        	Logs.addToGameSockLog("Game: Recv << packet : " + packet);
                        }
                    }
	    			//Protection anti flood packet
	    			if(!IpCheck.onGamePacket(_s.getInetAddress().getHostAddress(), packet))
	    				_s.close();
	    			parsePacket(packet);
	    			ParseTools.parsePacket(packet, _perso);
	    			////System.out.println(packet);
	    			packet = "";
	    		}
	    		
				synchronized(_in) {
					result = _in.read(charCur, 0, 1);
				}
	    	}
		} catch(IOException e) {
			try {
				Logs.addToGameLog(e.getMessage());
				_in.close();
	    		_out.close();
	    		_out=null;
	    		_in=null;
	    		
	    		if(_compte != null) {
	    			_compte.setCurPerso(null);
	    			_compte.setGameThread(null);
	    			_compte.setClient(null);
	    		}
	    		if(!_s.isClosed())_s.close();
			} catch(IOException e1) { e1.printStackTrace(); };
		} catch(Exception e) {
			e.printStackTrace();
			Logs.addToGameLog(e.getMessage());
		} finally {
			try {
				if(_out != null) _out.close();
				if(_s != null && !_s.isClosed()) _s.close();
				_in = null;
				_out = null;
			} catch(Exception e) {
				e.printStackTrace();
			}
			kick();
		}
	}
	
	/**
	 * 
	 * @param packet
	 */
	private void parsePacket(String packet) {
		if (!this.filter.authorize(this._s.getInetAddress().getHostAddress()))
			this.kick();
		
		if(_perso != null)
			_perso.refreshLastPacketTime();
		if (packet.length() > 250)
			return;
		
		switch(packet.charAt(0)) {
		case 'p': // 'p'
			if(packet.equals("ping"))
				SocketManager.GAME_SEND_PONG(_out);
			break;
			
		case 'q': // 'q'
			if (!packet.equals("qping"))
				return;
			if (_perso == null)
				return;
			if (_perso.get_fight() == null)
				return;
			SocketManager.GAME_SEND_QPONG(_out);
			break;
			
		case 'A':
			parseAccountPacket(packet);
			break;
		case 'C':
			ParseConquetePacket(packet);
			break;
		case 'B':
			parseBasicsPacket(packet);
			break;
			
		case 'c':
			parseChanelPacket(packet);
			break;
			
		case 'd':
			parseDocPacket(packet);
		break;
			
		case 'D':
			parseDialogPacket(packet);
			break;
			
		case 'E':
			parseExchangePacket(packet);
			break;
			
		case 'e':
			parse_environementPacket(packet);
			break;
			
		case 'F':
			parse_friendPacket(packet);
			break;
			
		case 'f':
			parseFightPacket(packet);
			break;
			
		case 'G':
			parseGamePacket(packet);
			break;
			
		case 'g':
			parseGuildPacket(packet);
			break;
			
		case 'h':
			parseHousePacket(packet);
			break;
			
		case 'i':
			parse_enemyPacket(packet);
			break;
			
		case 'K':
			parseHouseKodePacket(packet);
			break;
			
		case 'O':
			parseObjectPacket(packet);
			break;
			
		case 'P':
			parseGroupPacket(packet);
			break;
			
		case 'R':
			parseMountPacket(packet);
			break;
			
		case 'S':
			parseSpellPacket(packet);
			break;
			
		case 'W':
			parseWaypointPacket(packet);
			break;
			
		default: //Pour éviter les packets chinoix qui génères des <<Dofus ne répond pas>>
			SocketManager.GAME_SEND_BN(_out);
			Logs.addToGameLog("------------------------------------- Packet " + packet + " non gerer dans le parsePacket() !");
			break;
		}
	}
	
	private void parseHousePacket(String packet) {
		switch(packet.charAt(1))
		{
		case 'B'://Acheter la maison
			packet = packet.substring(2);
			Houses.HouseAchat(_perso);
		break;
		case 'G'://Maison de guilde
			packet = packet.substring(2);
			if(packet.isEmpty()) packet = null;
			Houses.parseHG(_perso, packet);
		break;
		case 'Q'://Quitter/Expulser de la maison
			packet = packet.substring(2);
			Houses.Leave(_perso, packet);
		break;
		case 'S'://Modification du prix de vente
			packet = packet.substring(2);
			Houses.SellPrice(_perso, packet);
		break;
		case 'V'://Fermer fenetre d'achat
			Houses.closeBuy(_perso);
		break;
		}
	}
	
	private void parseHouseKodePacket(String packet)
	{
		switch(packet.charAt(1))
		{
		case 'V'://Fermer fenetre du code
			Houses.closeCode(_perso);
		break;
		case 'K'://Envoi du code
			House_code(packet);
		break;
		}
	}
	
	private void House_code(String packet)
	{
		switch(packet.charAt(2))
		{
		case '0'://Envoi du code
			packet = packet.substring(4);
			if(_perso.getInTrunk() != null)
					Trunk.OpenTrunk(_perso, packet, false);
				else
					Houses.OpenHouse(_perso, packet, false);
		break;
		case '1'://Changement du code
			packet = packet.substring(4);
			if(_perso.getInTrunk() != null)
				Trunk.LockTrunk(_perso, packet);
			else
			    Houses.LockHouse(_perso, packet);
		break;
		}
	}
	public void parseDocPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'V':
				SocketManager.GAME_SEND_LEAVE_DOC(_perso);
			break;
		}
	}
	private void ParseConquetePacket(String packet) {
		switch (packet.charAt(1)) {

			case 'b':
				SocketManager.SEND_Cb_CONQUETE(_perso, World.getWorldBalance(_perso.get_align()) + ";"
						+ World.getAreaBalance(_perso.get_align()));
				break;
			case 'B':
				int bonus = World.getAlignementBonus(_perso);
				int multiplier = World.getMultiplierBonus(_perso);
				int align = _perso.get_align();
				SocketManager.SEND_CB_BONUS_CONQUETE(_perso, bonus + "," + bonus + "," + bonus + ";" + multiplier + "," + multiplier + ","
						+ multiplier + ";" + align + "," + align + "," + align);
				break;
			case 'W':
				geoConquest(packet);
				break;
			case 'I':
				protectConquest(packet);
				break;
			case 'F':
				joinProtectorsOfPrisme(packet);
				break;
		}
	}
	private void protectConquest(String packet) {
		switch (packet.charAt(2)) {
			case 'J':
				String str = _perso.parsePrisme();
				
				Prism Prismes = World.getPrisme(_perso.get_curCarte().getSubArea().getPrismeID());
				if (Prismes != null) {
					Prism.parseAttack(_perso);
					Prism.parseDefense(_perso);
				}
				SocketManager.SEND_CIJ_INFO_JOIN_PRISME(_perso, str);
				break;
			case 'V':
				SocketManager.SEND_CIV_INFOS_CONQUETE(_perso);
				break;
		}
	}
	private void geoConquest(String packet) {
		switch (packet.charAt(2)) {
			case 'J':
				SocketManager.SEND_CW_INFO_CONQUETE(_perso, World.PrismesGeoposition(1));
				SocketManager.SEND_CW_INFO_CONQUETE(_perso, World.PrismesGeoposition(2));
				break;
			case 'V':
				SocketManager.SEND_CW_INFO_CONQUETE(_perso, World.PrismesGeoposition(1));
				SocketManager.SEND_CW_INFO_CONQUETE(_perso, World.PrismesGeoposition(2));
				break;
		}
	}
	private void joinProtectorsOfPrisme(String packet) {
		switch (packet.charAt(2)) {
			case 'J':
				int PrismeID = _perso.get_curCarte().getSubArea().getPrismeID();
				Prism Prismes = World.getPrisme(PrismeID);
				if (Prismes == null)
					return;
				int FightID = -1;
				try {
					FightID = Prismes.getFightID();
				} catch (Exception e) {}
				short CarteID = -1;
				try {
					CarteID = Prismes.getCarte();
				} catch (Exception e) {}
				int cellID = -1;
				try {
					cellID = Prismes.getCell();
				} catch (Exception e) {}
				if (PrismeID == -1 || FightID == -1 || CarteID == -1 || cellID == -1)
					return;
				if (Prismes.getalignement() != _perso.get_align() || _perso.get_fight() != null){
					SocketManager.GAME_SEND_BN(_perso);
					return;
				}
				if (_perso.get_curCarte().get_id() != CarteID) {
					_perso.setMapProt(_perso.get_curCarte());
					_perso.setCellProt(_perso.get_curCell());
					try {
						Thread.sleep(200);
						_perso.teleport(CarteID, cellID);
						Thread.sleep(400);
					} catch (Exception e) {}
				}
				//TODO finir défense
				World.getCarte(CarteID).getFight(FightID).joinPrismeFight(_perso, _perso.get_GUID(), PrismeID);
				for (Characters z : World.getOnlinePersos()) {
					if (z == null)
						continue;
					if (z.get_align() != _perso.get_align())
						continue;
					Prism.parseDefense(z);
				}
				break;
		}
	}
	private void parse_enemyPacket(String packet)
	{
		switch(packet.charAt(1))
		{
		case 'A'://Ajouter
			Enemy_add(packet);
		break;
		case 'D'://Delete
			Enemy_delete(packet);
		break;
		case 'L'://Liste
			SocketManager.GAME_SEND_ENEMY_LIST(_perso);
		break;
		}
	}
	
	private void Enemy_add(String packet)
	{
		if(_perso == null)return;
		int guid = -1;
		switch(packet.charAt(2))
		{
			case '%'://Nom de perso
				packet = packet.substring(3);
				Characters P = World.getPersoByName(packet);
				if(P == null)
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = P.getAccID();
				
			break;
			case '*'://Pseudo
				packet = packet.substring(3);
				Accounts C = World.getCompteByPseudo(packet);
				if(C==null)
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = C.get_GUID();
			break;
			default:
				packet = packet.substring(2);
				if (packet.contains("[") || packet.contains("]"))
					packet = packet.split("-")[1];
				Characters Pr = World.getPersoByName(packet);
				if(Pr == null?true:!Pr.isOnline())
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = Pr.get_compte().get_GUID();
			break;
		}
		if(guid == -1)
		{
			SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
			return;
		}
		_compte.addEnemy(packet, guid);
	}

	private void Enemy_delete(String packet)
	{
		if(_perso == null)return;
		int guid = -1;
		switch(packet.charAt(2))
		{
			case '%'://Nom de perso
				packet = packet.substring(3);
				Characters P = World.getPersoByName(packet);
				if(P == null)
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = P.getAccID();
				
			break;
			case '*'://Pseudo
				packet = packet.substring(3);
				Accounts C = World.getCompteByPseudo(packet);
				if(C==null)
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = C.get_GUID();
			break;
			default:
				packet = packet.substring(2);
				Characters Pr = World.getPersoByName(packet);
				if(Pr == null?true:!Pr.isOnline())
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = Pr.get_compte().get_GUID();
			break;
		}
		if(guid == -1 || !_compte.isEnemyWith(guid))
		{
			SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
			return;
		}
		_compte.removeEnemy(guid);
	}
	
	private void parseWaypointPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'U'://Use
				Waypoint_use(packet);
			break;
			case 'u'://use zaapi
				Zaapi_use(packet);
			break;
			case 'v'://quitter zaapi
				Zaapi_close();
			break;
			case 'V'://Quitter
				Waypoint_quit();
			break;
			case 'w':
				Prisme_close();
				break;
			case 'p':
				Prisme_use(packet);
				break;
		}
	}

	private void Zaapi_close()
	{
		_perso.Zaapi_close();
	}
	private void Prisme_close() {
		_perso.Prisme_close();
	}
	private void Prisme_use(String packet) {
		if (_perso.getDeshonor() >= 2) {
			SocketManager.GAME_SEND_Im_PACKET(_perso, "183");
			return;
		}
		_perso.usePrisme(packet);
	}
	private void Zaapi_use(String packet)
	{
		if(_perso.getDeshonor() >= 2) 
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "183");
			return;
		}
		_perso.Zaapi_use(packet);
	}
	
	private void Waypoint_quit()
	{
		_perso.stopZaaping();
	}

	private void Waypoint_use(String packet)
	{
		short id = -1;
		try
		{
			id = Short.parseShort(packet.substring(2));
		}catch(Exception e){};
		if( id == -1)return;
		_perso.useZaap(id);
	}
	private void parseGuildPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'B'://Stats
				if(_perso.get_guild() == null)return;
				Guild G = _perso.get_guild();
				if(!_perso.getGuildMember().canDo(Constant.G_BOOST))return;
				switch(packet.charAt(2))
				{
					case 'p'://Prospec
						if(G.get_Capital() < 1)return;
						if(G.get_Stats(176) >= 500)return;
						G.set_Capital(G.get_Capital()-1);
						G.upgrade_Stats(176, 1);
					break;
					case 'x'://Sagesse
						if(G.get_Capital() < 1)return;
						if(G.get_Stats(124) >= 400)return;
						G.set_Capital(G.get_Capital()-1);
						G.upgrade_Stats(124, 1);
					break;
					case 'o'://Pod
						if(G.get_Capital() < 1)return;
						if(G.get_Stats(158) >= 5000)return;
						G.set_Capital(G.get_Capital()-1);
						G.upgrade_Stats(158, 20);
					break;
					case 'k'://Nb Perco
						if(G.get_Capital() < 10)return;
						if(G.get_nbrPerco() >= 50)return;
						G.set_Capital(G.get_Capital()-10);
						G.set_nbrPerco(G.get_nbrPerco()+1);
					break;
				}
				SQLManager.UPDATE_GUILD(G);
				SocketManager.GAME_SEND_gIB_PACKET(_perso, _perso.get_guild().parsePercotoGuild());
			break;
			case 'b'://Sorts
				if(_perso.get_guild() == null)return;
				Guild G2 = _perso.get_guild();
				if(!_perso.getGuildMember().canDo(Constant.G_BOOST))return;
				int spellID = Integer.parseInt(packet.substring(2));
				if(G2.getSpells().containsKey(spellID))
				{
					if(G2.get_Capital() < 5)return;
					G2.set_Capital(G2.get_Capital()-5);
					G2.boostSpell(spellID);
					SQLManager.UPDATE_GUILD(G2);
					SocketManager.GAME_SEND_gIB_PACKET(_perso, _perso.get_guild().parsePercotoGuild());
				}else
				{
					Logs.addToGameLog("[ERROR]Sort "+spellID+" non trouve.");
				}
			break;
			case 'C'://Creation
				guild_create(packet);
			break;
			case 'f'://Téléportation enclo de guilde
				guild_enclo(packet.substring(2));
			break;
			case 'F'://Retirer percepteur
				guild_remove_perco(packet.substring(2));
			break;
			case 'h'://Téléportation maison de guilde
				guild_house(packet.substring(2));
			break;
			case 'H'://Poser un percepteur
				guild_add_perco();
			break;
			case 'I'://Infos
				guild_infos(packet.charAt(2));
			break;
			case 'J'://Join
				guild_join(packet.substring(2));
			break;
			case 'K'://Kick
				guild_kick(packet.substring(2));
			break;
			case 'P'://Promote
				guild_promote(packet.substring(2));
			break;
			case 'T'://attaque sur percepteur
				guild_perco_join_fight(packet.substring(2));
			break;
			case 'V'://Ferme le panneau de création de guilde
				guild_CancelCreate();
			break;
		}
		if(_perso.get_guild() != null)
			SQLManager.UPDATE_GUILD(_perso.get_guild());
	}
	
		private void guild_perco_join_fight(String packet) 
		{
			switch(packet.charAt(0))
			{
			case 'J'://Rejoindre
				String PercoID = Integer.toString(Integer.parseInt(packet.substring(1)), 36);
				
				int TiD = -1;
				try
				{
					TiD = Integer.parseInt(PercoID);
				}catch(Exception e){};
				
				Collector perco = World.getPerco(TiD);
				if(perco == null) return;
				
				int FightID = -1;
				try
				{
					FightID = perco.get_inFightID();
				}catch(Exception e){};
				
				short MapID = -1;
				try
				{
					MapID = World.getCarte((short)perco.get_mapID()).getFight(FightID).get_map().get_id();
				}catch(Exception e){};
				
				int CellID = -1;
				try
				{
					CellID = perco.get_cellID();
				}catch(Exception e){};
				
				Logs.addToGameLog("[DEBUG] Percepteur INFORMATIONS : TiD:"+TiD+", FightID:"+FightID+", MapID:"+MapID+", CellID"+CellID);
				if(TiD == -1 || FightID == -1 || MapID == -1 || CellID == -1) return;
				if(_perso.get_fight() == null && !_perso.is_away())
				{
					if(_perso.get_curCarte().get_id() != MapID)
					{
						_perso.teleport(MapID, CellID);
					}
					World.getCarte(MapID).getFight(FightID).joinPercepteurFight(_perso,_perso.get_GUID(), TiD);
				}
			break;
		}
	}

	private void guild_remove_perco(String packet) 
	{
		if (_perso.get_guild() == null || _perso.get_fight() != null || _perso.is_away())
			return;
		if (!_perso.getGuildMember().canDo(Constant.G_POSPERCO))
			return;// On peut le retirer si on a le droit de le poser
		int IDPerco = Integer.parseInt(packet);
		Collector perco = World.getPerco(IDPerco);
		if (perco == null || perco.get_inFight() > 0)
			return;
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.get_curCarte(), IDPerco);
		SQLManager.DELETE_PERCO(perco.getGuid());
		perco.DelPerco(perco.getGuid());
		for (Characters z : _perso.get_guild().getMembers()) {
			if (z.isOnline()) {
				SocketManager.GAME_SEND_gITM_PACKET(z, Collector.parsetoGuild(z.get_guild().get_id()));
				String str = "";
				str += "R" + perco.get_N1() + "," + perco.get_N2() + "|";
				str += perco.get_mapID() + "|";
				str += World.getCarte((short) perco.get_mapID()).getX() + "|"
						+ World.getCarte((short) perco.get_mapID()).getY() + "|" + _perso.get_name();
				SocketManager.GAME_SEND_gT_PACKET(z, str);
			}
		}
	}

	private void guild_add_perco() 
	{
		if (_perso.get_guild() == null || _perso.get_fight() != null || _perso.is_away())
			return;
		if (!_perso.getGuildMember().canDo(Constant.G_POSPERCO))
			return;// Pas le droit de le poser
		if (_perso.get_guild().getMembers().size() < 1)
			return;// Guilde invalide
		short price = (short) (1000 + 10 * _perso.get_guild().get_lvl());// Calcul du prix du percepteur
		if (_perso.get_kamas() < price)// Kamas insuffisants
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "182");
			return;
		}
		if (Collector.GetPercoGuildID(_perso.get_curCarte().get_id()) > 0)// La carte possède un perco
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1168;1");
			return;
		}
		if (_perso.get_curCarte().get_placesStr().length() < 5)// La map ne possède pas de "places"
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "113");
			return;
		}
		if (Collector.CountPercoGuild(_perso.get_guild().get_id()) >= _perso.get_guild().get_nbrPerco())
			return;// Limite de percepteur
		short random1 = (short) (Formulas.getRandomValue(1, 39));
		short random2 = (short) (Formulas.getRandomValue(1, 71));
		
		// Ajout du Perco.
		int id = SQLManager.GetNewIDPercepteur();
		Collector perco = new Collector(id, _perso.get_curCarte().get_id(), _perso.get_curCell().getID(), (byte) 3, _perso.get_guild().get_id(), random1, random2, "", 0, 0);
		World.addPerco(perco);
		SocketManager.GAME_SEND_ADD_PERCO_TO_MAP(_perso.get_curCarte());
		SQLManager.ADD_PERCO_ON_MAP(id, _perso.get_curCarte().get_id(), _perso.get_guild().get_id(), _perso.get_curCell().getID(), 3, random1, random2);
		for (Characters z : _perso.get_guild().getMembers()) {
			if (z != null && z.isOnline()) {
				SocketManager.GAME_SEND_gITM_PACKET(z, Collector.parsetoGuild(z.get_guild().get_id()));
				String str = "";
				str += "S" + perco.get_N1() + "," + perco.get_N2() + "|";
				str += perco.get_mapID() + "|";
				str += World.getCarte((short) perco.get_mapID()).getX() + "|" + World.getCarte((short) perco.get_mapID()).getY() 
						+ "|" + _perso.get_name();
				SocketManager.GAME_SEND_gT_PACKET(z, str);
			}
		}
	}

	private void guild_enclo(String packet)
	{
		if(_perso.get_guild() == null)
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1135");
			return;
		}
		
		if(_perso.get_fight() != null || _perso.is_away())return;
		short MapID = Short.parseShort(packet);
		MountPark MP = World.getCarte(MapID).getMountPark();
		if(MP.get_guild().get_id() != _perso.get_guild().get_id())
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1135");
			return;
		}
		int CellID = World.getEncloCellIdByMapId(MapID);
		if (_perso.hasItemTemplate(9035, 1))
		{
			_perso.removeByTemplateID(9035,1);
			_perso.teleport(MapID, CellID);
		}else
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1159");
			return;
		}
	}
	
	private void guild_house(String packet)
	{
		if(_perso.get_guild() == null)
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1135");
			return;
		}
		
		if(_perso.get_fight() != null || _perso.is_away())return;
		int HouseID = Integer.parseInt(packet);
		Houses h = World.getHouses().get(HouseID);
		if(h == null) return;
		if(_perso.get_guild().get_id() != h.get_guild_id()) 
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1135");
			return;
		}
		if(!h.canDo(Constant.H_GTELE))
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1136");
			return;
		}
		if (_perso.hasItemTemplate(8883, 1))
		{
			_perso.removeByTemplateID(8883,1);
			_perso.teleport((short)h.get_mapid(), h.get_caseid());
		}else
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1137");
			return;
		}
	}
	
	private void guild_promote(String packet)
	{
		if(_perso.get_guild() == null)return;	//Si le personnage envoyeur n'a même pas de guilde
		
		String[] infos = packet.split("\\|");
		
		int guid = Integer.parseInt(infos[0]);
		int rank = Integer.parseInt(infos[1]);
		byte xpGive = Byte.parseByte(infos[2]);
		int right = Integer.parseInt(infos[3]);
		
		Characters p = World.getPersonnage(guid);	//Cherche le personnage a qui l'on change les droits dans la mémoire
		GuildMember toChange;
		GuildMember changer = _perso.getGuildMember();
		
		//Récupération du personnage à changer, et verification de quelques conditions de base
		if(p == null)	//Arrive lorsque le personnage n'est pas chargé dans la mémoire
		{
			int guildId = SQLManager.isPersoInGuild(guid);	//Récupère l'id de la guilde du personnage qui n'est pas dans la mémoire
			
			if(guildId < 0)return;	//Si le personnage à qui les droits doivent être modifié n'existe pas ou n'a pas de guilde
			
			
			if(guildId != _perso.get_guild().get_id())					//Si ils ne sont pas dans la même guilde
			{
				SocketManager.GAME_SEND_gK_PACKET(_perso, "Ed");
				return;
			}
			toChange = World.getGuild(guildId).getMember(guid);
		}
		else
		{
			if(p.get_guild() == null)return;	//Si la personne à qui changer les droits n'a pas de guilde
			if(_perso.get_guild().get_id() != p.get_guild().get_id())	//Si ils ne sont pas de la meme guilde
			{
				SocketManager.GAME_SEND_gK_PACKET(_perso, "Ea");
				return;
			}
			
			toChange = p.getGuildMember();
		}
		
		//Vérifie ce que le personnage changeur à le droit de faire
		
		if(changer.getRank() == 1)	//Si c'est le meneur
		{
			if(changer.getGuid() == toChange.getGuid())	//Si il se modifie lui même, reset tout sauf l'XP
			{
				rank = -1;
				right = -1;
			}
			else //Si il modifie un autre membre
			{
				if(rank == 1) //Si il met un autre membre "Meneur"
				{
					changer.setAllRights(2, (byte) -1, 29694);	//Met le meneur "Bras droit" avec tout les droits
					
					//Défini les droits à mettre au nouveau meneur
					rank = 1;
					xpGive = -1;
					right = 1;
				}
			}
		}
		else	//Sinon, c'est un membre normal
		{
			if(toChange.getRank() == 1)	//S'il veut changer le meneur, reset tout sauf l'XP
			{
				rank = -1;
				right = -1;
			}
			else	//Sinon il veut changer un membre normal
			{
				if(!changer.canDo(Constant.G_RANK) || rank == 1)	//S'il ne peut changer les rang ou qu'il veut mettre meneur
					rank = -1; 	//"Reset" le rang
				
				if(!changer.canDo(Constant.G_RIGHT) || right == 1)	//S'il ne peut changer les droits ou qu'il veut mettre les droits de meneur
					right = -1;	//"Reset" les droits
				
				if(!changer.canDo(Constant.G_HISXP) && !changer.canDo(Constant.G_ALLXP) && changer.getGuid() == toChange.getGuid())	//S'il ne peut changer l'XP de personne et qu'il est la cible
					xpGive = -1; //"Reset" l'XP
			}
			
			if(!changer.canDo(Constant.G_ALLXP) && !changer.equals(toChange))	//S'il n'a pas le droit de changer l'XP des autres et qu'il n'est pas la cible
				xpGive = -1; //"Reset" L'XP
		}

		toChange.setAllRights(rank,xpGive,right);
		
		SocketManager.GAME_SEND_gS_PACKET(_perso,_perso.getGuildMember());
		
		if(p != null && p.get_GUID() != _perso.get_GUID())
			SocketManager.GAME_SEND_gS_PACKET(p,p.getGuildMember());
	}
	
	private void guild_CancelCreate()
	{
		SocketManager.GAME_SEND_gV_PACKET(_perso);
	}

	private void guild_kick(String name)
	{
		if (_perso.get_guild() == null)
			return;
		Characters P = World.getPersoByName(name);
		int guid = -1, guildId = -1;
		Guild toRemGuild;
		GuildMember toRemMember;
		if (P == null) {
			int infos[] = SQLManager.isPersoInGuild(name);
			guid = infos[0];
			guildId = infos[1];
			if (guildId < 0 || guid < 0)
				return;
			toRemGuild = World.getGuild(guildId);
			toRemMember = toRemGuild.getMember(guid);
		} else {
			toRemGuild = P.get_guild();
			if (toRemGuild == null)// La guilde du personnage n'est pas charger ?
			{
				toRemGuild = World.getGuild(_perso.get_guild().get_id());// On prend la guilde du perso qui l'éjecte
			}
			toRemMember = toRemGuild.getMember(P.get_GUID());
			if (toRemMember == null)
				return;// Si le membre n'est pas dans la guilde.
			if (toRemMember.getGuild().get_id() != _perso.get_guild().get_id())
				return;// Si guilde différente
		}
		// si pas la meme guilde
		if (toRemGuild.get_id() != _perso.get_guild().get_id()) {
			SocketManager.GAME_SEND_gK_PACKET(_perso, "Ea");
			return;
		}
		// S'il n'a pas le droit de kick, et que ce n'est pas lui même la cible
		if (!_perso.getGuildMember().canDo(Constant.G_BAN)
				&& _perso.getGuildMember().getGuid() != toRemMember.getGuid()) {
			SocketManager.GAME_SEND_gK_PACKET(_perso, "Ed");
			return;
		}
		// Si différent : Kick
		if (_perso.getGuildMember().getGuid() != toRemMember.getGuid()) {
			if (toRemMember.getRank() == 1) // S'il veut kicker le meneur
				return;

			toRemGuild.removeMember(toRemMember.getPerso());
			if (P != null)
				P.setGuildMember(null);

			SocketManager.GAME_SEND_gK_PACKET(_perso, "K" + _perso.get_name() + "|" + name);
			if (P != null)
				SocketManager.GAME_SEND_gK_PACKET(P, "K" + _perso.get_name());
		} else// si quitter
		{
			Guild G = _perso.get_guild();
			if (_perso.getGuildMember().getRank() == 1 && G.getMembers().size() > 1) // Si le meneur veut quitter la guilde mais qu'il reste d'autre joueurs
			{
				// TODO : Envoyer le message qu'il doit mettre un autre membre meneur (Pas vraiment....)
				return;
			}
			G.removeMember(_perso);
			_perso.setGuildMember(null);
			// S'il n'y a plus personne
			if (G.getMembers().isEmpty())
				World.removeGuild(G.get_id());
			SocketManager.GAME_SEND_gK_PACKET(_perso, "K" + name + "|" + name);
		}
	}
	
	private void guild_join(String packet)
	{
		switch(packet.charAt(0))
		{
		case 'R'://Nom perso
			String name = packet.substring(1);
			if(name.contains("[") || name.contains("]")) 
				name = name.split("-")[1];
			
			Characters P = World.getPersoByName(name);
			
			if(P == null || _perso.get_guild() == null)
			{
				SocketManager.GAME_SEND_gJ_PACKET(_perso, "Eu");
				return;
			}
			if(!P.isOnline())
			{
				SocketManager.GAME_SEND_gJ_PACKET(_perso, "Eu");
				return;
			}
			if(P.is_away())
			{
				SocketManager.GAME_SEND_gJ_PACKET(_perso, "Eo");
				return;
			}
			if(P.get_guild() != null)
			{
				SocketManager.GAME_SEND_gJ_PACKET(_perso, "Ea");
				return;
			}
			if(!_perso.getGuildMember().canDo(Constant.G_INVITE))
			{
				SocketManager.GAME_SEND_gJ_PACKET(_perso, "Ed");
				return;
			}
			if(_perso.get_guild().getMembers().size() >= (40+_perso.get_guild().get_lvl()))//Limite membres max
			{
				SocketManager.GAME_SEND_Im_PACKET(_perso, "155;"+(40+_perso.get_guild().get_lvl()));
				return;
			}
			
			_perso.setInvitation(P.get_GUID());
			P.setInvitation(_perso.get_GUID());

			SocketManager.GAME_SEND_gJ_PACKET(_perso,"R"+packet.substring(1));
			SocketManager.GAME_SEND_gJ_PACKET(P,"r"+_perso.get_GUID()+"|"+_perso.get_name()+"|"+_perso.get_guild().get_name());
		break;
		case 'E'://ou Refus
			if(packet.substring(1).equalsIgnoreCase(_perso.getInvitation()+""))
			{
				Characters p = World.getPersonnage(_perso.getInvitation());
				if(p == null)return;//Pas censé arriver
				SocketManager.GAME_SEND_gJ_PACKET(p,"Ec");
			}
		break;
		case 'K'://Accepte
			if(packet.substring(1).equalsIgnoreCase(_perso.getInvitation()+""))
			{
				Characters p = World.getPersonnage(_perso.getInvitation());
				if(p == null)return;//Pas censé arriver
				Guild G = p.get_guild();
				GuildMember GM = G.addNewMember(_perso);
				SQLManager.UPDATE_GUILDMEMBER(GM);
				_perso.setGuildMember(GM);
				_perso.setInvitation(-1);
				p.setInvitation(-1);
				//Packet
				SocketManager.GAME_SEND_gJ_PACKET(p,"Ka"+_perso.get_name());
				SocketManager.GAME_SEND_gS_PACKET(_perso, GM);
				SocketManager.GAME_SEND_gJ_PACKET(_perso,"Kj");
			}
		break;
		}
	}

	private void guild_infos(char c)
	{
		switch(c)
		{
		case 'B'://Perco
			SocketManager.GAME_SEND_gIB_PACKET(_perso, _perso.get_guild().parsePercotoGuild());
		break;
		case 'F'://Enclos
			SocketManager.GAME_SEND_gIF_PACKET(_perso, World.parseMPtoGuild(_perso.get_guild().get_id()));
		break;
		case 'G'://General
			SocketManager.GAME_SEND_gIG_PACKET(_perso, _perso.get_guild());
		break;
		case 'H'://House
			SocketManager.GAME_SEND_gIH_PACKET(_perso, Houses.parseHouseToGuild(_perso));
		break;
		case 'M'://Members
			SocketManager.GAME_SEND_gIM_PACKET(_perso, _perso.get_guild(),'+');
		break;
		case 'T'://Perco
			SocketManager.GAME_SEND_gITM_PACKET(_perso, Collector.parsetoGuild(_perso.get_guild().get_id()));
			Collector.parseAttaque(_perso, _perso.get_guild().get_id());
			Collector.parseDefense(_perso, _perso.get_guild().get_id());
		break;
		}
	}

	private void guild_create(String packet)
	{
		if (_perso == null)
			return;
		if (!(_perso.hasItemTemplate(1575, 1)))// Guildalogemme
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "14");
			SocketManager.GAME_SEND_gV_PACKET(_perso);
			return;
		}
		if (_perso.get_guild() != null || _perso.getGuildMember() != null) {
			SocketManager.GAME_SEND_gC_PACKET(_perso, "Ea");
			return;
		}
		if (_perso.get_fight() != null || _perso.is_away())
			return;
		try {
			String[] infos = packet.substring(2).split("\\|");
			// base 10 => 36
			String bgID = Integer.toString(Integer.parseInt(infos[0]), 36);
			String bgCol = Integer.toString(Integer.parseInt(infos[1]), 36);
			String embID = Integer.toString(Integer.parseInt(infos[2]), 36);
			String embCol = Integer.toString(Integer.parseInt(infos[3]), 36);
			String name = infos[4];

			if (World.guildNameIsUsed(name)) {
				SocketManager.GAME_SEND_gC_PACKET(_perso, "Ean");
				return;
			}

			// Validation du nom de la guilde
			String tempName = name.toLowerCase();
			// Vérifie d'abord si il contient des termes définit
			for (String blacklisted : Config.nicknameProhibited) {
				if (tempName.replaceAll("-", "").contains(blacklisted)) {
					SocketManager.GAME_SEND_gC_PACKET(_perso, "Ean");
					return;
				}
			}
			// Si le nom passe le test, on vérifie que les caractère entré sont correct.
			if (!Pattern.matches("^[A-Za-z]{1}[a-z]{2,}(-?[A-Za-z]{1}[a-z]{2,})?$", name) || name.length() > 20) {
				SocketManager.GAME_SEND_gC_PACKET(_perso, "Ean");
				return;
			}
			// FIN de la validation
			String emblem = bgID + "," + bgCol + "," + embID + "," + embCol;// 9,6o5nc,2c,0;
			if (World.guildEmblemIsUsed(emblem)) {
				SocketManager.GAME_SEND_gC_PACKET(_perso, "Eae");
				return;
			}
			_perso.removeByTemplateID(1575, 1);
			Guild G = new Guild(_perso, name, emblem);
			GuildMember gm = G.addNewMember(_perso);
			gm.setAllRights(1, (byte) 0, 1);// 1 => Meneur (Tous droits)
			_perso.setGuildMember(gm);// On ajoute le meneur
			World.addGuild(G, true);
			SQLManager.UPDATE_GUILDMEMBER(gm);
			// Packets
			SocketManager.GAME_SEND_Im_PACKET(_perso, "022;1~1575");
			SocketManager.GAME_SEND_gS_PACKET(_perso, gm);
			SocketManager.GAME_SEND_gC_PACKET(_perso, "K");
			SocketManager.GAME_SEND_gV_PACKET(_perso);
			SocketManager.GAME_SEND_Ow_PACKET(_perso);
		} catch (Exception e) {
			return;
		}
	}

	private void parseChanelPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'C'://Changement des Canaux
				Chanels_change(packet);
			break;
		}
	}

	private void Chanels_change(String packet)
	{
		String chan = packet.charAt(3)+"";
		switch(packet.charAt(2))
		{
			case '+'://Ajout du Canal
				_perso.addChanel(chan);
			break;
			case '-'://Desactivation du canal
				_perso.removeChanel(chan);
			break;
		}
		SQLManager.SAVE_PERSONNAGE(_perso, false);
	}

	private void parseMountPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'b'://Achat d'un enclos
				SocketManager.GAME_SEND_R_PACKET(_perso, "v");//Fermeture du panneau
				MountPark MP = _perso.get_curCarte().getMountPark();
				Characters Seller = World.getPersonnage(MP.get_owner());
				if(MP.get_owner() == -1)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "196");
					return;
				}
				if(MP.get_price() == 0)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "197");
					return;
				}
				if(_perso.get_guild() == null)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "1135");
					return;
				}
				if(_perso.getGuildMember().getRank() != 1)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "198"); 
					return;
				}
				byte enclosMax = (byte)Math.floor(_perso.get_guild().get_lvl()/10);
				byte TotalEncloGuild = SQLManager.TotalMPGuild(_perso.get_guild().get_id()); 
				if(TotalEncloGuild >= enclosMax)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "1103");
					return;
				}
				if(_perso.get_kamas() < MP.get_price())
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "182");
					return;
				}
				long NewKamas = _perso.get_kamas()-MP.get_price();
				_perso.set_kamas(NewKamas);
				if(Seller != null)
				{
					long NewSellerBankKamas = Seller.getBankKamas()+MP.get_price();
					Seller.setBankKamas(NewSellerBankKamas);
					if(Seller.isOnline())
					{
						SocketManager.GAME_SEND_MESSAGE(_perso, "Un enclo a ete vendu a "+MP.get_price()+".", Config.CONFIG_MOTD_COLOR);
					}
				}
				MP.set_price(0);//On vide le prix
				MP.set_owner(_perso.get_GUID());
				MP.set_guild(_perso.get_guild());
				SQLManager.SAVE_MOUNTPARK(MP);
				SQLManager.SAVE_PERSONNAGE(_perso, true);
				//On rafraichit l'enclo
				for(Characters z:_perso.get_curCarte().getPersos())
				{
					SocketManager.GAME_SEND_Rp_PACKET(z, MP);
				}
			break;
		
			case 'd'://Demande Description
				Mount_description(packet);
			break;
			
			case 'n'://Change le nom
				Mount_name(packet.substring(2));
			break;
			
			case 'r'://Monter sur la dinde
				Mount_ride();
			break;
			case 's'://Vendre l'enclo
				SocketManager.GAME_SEND_R_PACKET(_perso, "v");//Fermeture du panneau
				int price = Integer.parseInt(packet.substring(2));
				MountPark MP1 = _perso.get_curCarte().getMountPark();
				if(MP1.get_owner() == -1)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "194");
					return;
				}
				if(MP1.get_owner() != _perso.get_GUID())
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "195");
					return;
				}
				MP1.set_price(price);
				SQLManager.SAVE_MOUNTPARK(MP1);
				SQLManager.SAVE_PERSONNAGE(_perso, true);
				//On rafraichit l'enclo
				for(Characters z:_perso.get_curCarte().getPersos())
				{
					SocketManager.GAME_SEND_Rp_PACKET(z, MP1);
				}
			break;
			case 'v'://Fermeture panneau d'achat
				SocketManager.GAME_SEND_R_PACKET(_perso, "v");
			break;
			case 'x'://Change l'xp donner a la dinde
				Mount_changeXpGive(packet);
			break;
		}
	}

	private void Mount_changeXpGive(String packet)
	{
		try
		{
			int xp = Integer.parseInt(packet.substring(2));
			if(xp <0)xp = 0;
			if(xp >90)xp = 90;
			_perso.setMountGiveXp(xp);
			SocketManager.GAME_SEND_Rx_PACKET(_perso);
		}catch(Exception e){};
	}

	private void Mount_name(String name)
	{
		if(_perso.getMount() == null)return;
		_perso.getMount().setName(name);
		SocketManager.GAME_SEND_Rn_PACKET(_perso, name);
	}
	
	private void Mount_ride()
	{
		if(_perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
		{
			SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
			return;
		}
		if(_perso.get_lvl()<60 || _perso.getMount() == null || !_perso.getMount().isMountable() || _perso._isGhosts)
		{
			SocketManager.GAME_SEND_Re_PACKET(_perso,"Er", null);
			return;
		}
		_perso.toogleOnMount();
	}
	
	private void Mount_description(String packet)
	{
		int DDid = -1;
		try
		{
			DDid = Integer.parseInt(packet.substring(2).split("\\|")[0]);
			//on ignore le temps?
		}catch(Exception e){};
		if(DDid == -1)return;
		Mount DD = World.getDragoByID(DDid);
		if(DD == null)return;
		SocketManager.GAME_SEND_MOUNT_DESCRIPTION_PACKET(_perso,DD);
	}
	

	private void parse_friendPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'A'://Ajouter
				Friend_add(packet);
			break;
			case 'D'://Effacer un ami
				Friend_delete(packet);
			break;
			case 'L'://Liste
				SocketManager.GAME_SEND_FRIENDLIST_PACKET(_perso);
			break;
			case 'O':
				switch(packet.charAt(2))
				{
				case '-':
					 _perso.SetSeeFriendOnline(false);
					 SocketManager.GAME_SEND_BN(_perso);
					 break;
				 case'+':
					 _perso.SetSeeFriendOnline(true);
					 SocketManager.GAME_SEND_BN(_perso);
					 break;
				}
			break;
			case 'J': //Wife
				FriendLove(packet);
			break;
		}
	}

	private void FriendLove(String packet)
	{
		Characters Wife = World.getPersonnage(_perso.getWife());
		if(Wife == null) return;
		_perso.RejoindeWife(Wife); // Correcion téléportation mariage par Taparisse
		if(!Wife.isOnline())
		{
			if(Wife.get_sexe() == 0) SocketManager.GAME_SEND_Im_PACKET(_perso, "140");
			else SocketManager.GAME_SEND_Im_PACKET(_perso, "139");
			
			SocketManager.GAME_SEND_FRIENDLIST_PACKET(_perso);
			return;
		}
		switch(packet.charAt(2))
		{
			case 'S'://Teleportation
				if(_perso.get_fight() != null)
					return;
				else
					_perso.meetWife(Wife);
			break;
			case 'C'://Suivre le deplacement
				if(packet.charAt(3) == '+'){//Si lancement de la traque
					if(_perso._Follows != null)
					{
						_perso._Follows._Follower.remove(_perso.get_GUID());
					}
					SocketManager.GAME_SEND_FLAG_PACKET(_perso, Wife);
					_perso._Follows = Wife;
					Wife._Follower.put(_perso.get_GUID(), _perso);
				}else{//On arrete de suivre
					SocketManager.GAME_SEND_DELETE_FLAG_PACKET(_perso);
					_perso._Follows = null;
					Wife._Follower.remove(_perso.get_GUID());
				}
			break;
		}
	} 
	
	private void Friend_delete(String packet) {
		if(_perso == null)return;
		int guid = -1;
		switch(packet.charAt(2))
		{
			case '%'://Nom de perso
				packet = packet.substring(3);
				Characters P = World.getPersoByName(packet);
				if(P == null)//Si P est nul, ou si P est nonNul et P offline
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = P.getAccID();
				
			break;
			case '*'://Pseudo
				packet = packet.substring(3);
				Accounts C = World.getCompteByPseudo(packet);
				if(C==null)
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = C.get_GUID();
			break;
			default:
				packet = packet.substring(2);
				Characters Pr = World.getPersoByName(packet);
				if(Pr == null?true:!Pr.isOnline())//Si P est nul, ou si P est nonNul et P offline
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = Pr.get_compte().get_GUID();
			break;
		}
		if(guid == -1 || !_compte.isFriendWith(guid))
		{
			SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
			return;
		}
		_compte.removeFriend(guid);
	}

	private void Friend_add(String packet)
	{
		if(_perso == null)return;
		int guid = -1;
		switch(packet.charAt(2))
		{
			case '%'://Nom de perso
				packet = packet.substring(3);
				Characters P = World.getPersoByName(packet);
				if(P == null?true:!P.isOnline())//Si P est nul, ou si P est nonNul et P offline
				{
					SocketManager.GAME_SEND_FA_PACKET(_perso, "Ef");
					return;
				}
				guid = P.getAccID();
			break;
			case '*'://Pseudo
				packet = packet.substring(3);
				Accounts C = World.getCompteByPseudo(packet);
				if(C==null?true:!C.isOnline())
				{
					SocketManager.GAME_SEND_FA_PACKET(_perso, "Ef");
					return;
				}
				guid = C.get_GUID();
			break;
			default:
				packet = packet.substring(2);
				if(packet.contains("[") || packet.contains("]"))
					packet = packet.split("-")[1];
				Characters Pr = World.getPersoByName(packet);
				if(Pr == null?true:!Pr.isOnline())//Si P est nul, ou si P est nonNul et P offline
				{
					SocketManager.GAME_SEND_FA_PACKET(_perso, "Ef");
					return;
				}
				guid = Pr.get_compte().get_GUID();
			break;
		}
		if(guid == -1)
		{
			SocketManager.GAME_SEND_FA_PACKET(_perso, "Ef");
			return;
		}
		_compte.addFriend(guid);
	}

	private void parseGroupPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'A'://Accepter invitation
				group_accept(packet);
			break;
			
			case 'F'://Suivre membre du groupe PF+GUID
				Group g = _perso.getGroup();
				if(g == null)return;
				
				int pGuid = -1;
				try
				{
					pGuid = Integer.parseInt(packet.substring(3));
				}catch(NumberFormatException e){return;};
				
				if(pGuid == -1) return;
				
				Characters P = World.getPersonnage(pGuid);
				
				if(P == null || !P.isOnline()) return;
				
				if(packet.charAt(2) == '+')//Suivre
				{
					if(_perso._Follows != null)
					{
						_perso._Follows._Follower.remove(_perso.get_GUID());
					}
					SocketManager.GAME_SEND_FLAG_PACKET(_perso, P);
					SocketManager.GAME_SEND_PF(_perso, "+"+P.get_GUID());
					_perso._Follows = P;
					P._Follower.put(_perso.get_GUID(), _perso);
				}
				else if(packet.charAt(2) == '-')//Ne plus suivre
				{
					SocketManager.GAME_SEND_DELETE_FLAG_PACKET(_perso);
					SocketManager.GAME_SEND_PF(_perso, "-");
					_perso._Follows = null;
					P._Follower.remove(_perso.get_GUID());
				}
			break;
			case 'G'://Suivez le tous PG+GUID
				Group g2 = _perso.getGroup();
				if(g2 == null)return;
				
				int pGuid2 = -1;
				try
				{
					pGuid2 = Integer.parseInt(packet.substring(3));
				}catch(NumberFormatException e){return;};
				
				if(pGuid2 == -1) return;
				
				Characters P2 = World.getPersonnage(pGuid2);
				
				if(P2 == null || !P2.isOnline()) return;
				
				if(packet.charAt(2) == '+')//Suivre
				{
					for(Characters T : g2.getPersos())
					{
						if(T.get_GUID() == P2.get_GUID()) continue;
						if(T._Follows != null)
						{
							T._Follows._Follower.remove(_perso.get_GUID());
						}
						SocketManager.GAME_SEND_FLAG_PACKET(T, P2);
						SocketManager.GAME_SEND_PF(T, "+"+P2.get_GUID());
						T._Follows = P2;
						P2._Follower.put(T.get_GUID(), T);
					}
				}
				else if(packet.charAt(2) == '-')//Ne plus suivre
				{
					for(Characters T : g2.getPersos())
					{
						if(T.get_GUID() == P2.get_GUID()) continue;
						SocketManager.GAME_SEND_DELETE_FLAG_PACKET(T);
						SocketManager.GAME_SEND_PF(T, "-");
						T._Follows = null;
						P2._Follower.remove(T.get_GUID());
					}
				}
			break;
			
			case 'I'://inviation
				group_invite(packet);
			break;
			
			case 'R'://Refuse
				group_refuse(_perso);
			break;
			
			case 'V'://Quitter
				group_quit(packet);
			break;
			case 'W'://Localisation du groupe
				group_locate();
			break;
		}
	}
	
	private void group_locate()
	{
		if(_perso == null)return;
		Group g = _perso.getGroup();
		if(g == null)return;
		String str = "";
		boolean isFirst = true;
		for(Characters GroupP : _perso.getGroup().getPersos())
		{
			if(!isFirst) str += "|";
			str += GroupP.get_curCarte().getX()+";"+GroupP.get_curCarte().getY()+";"+GroupP.get_curCarte().get_id()+";2;"+GroupP.get_GUID()+";"+GroupP.get_name();
			isFirst = false;
		}
		SocketManager.GAME_SEND_IH_PACKET(_perso, str);
	}
	
	private void group_quit(String packet)
	{
		if(_perso == null)return;
		Group g = _perso.getGroup();
		if(g == null)return;
		if(packet.length() == 2)//Si aucun guid est spécifié, alors c'est que le joueur quitte
		{
			 g.leave(_perso);
			 SocketManager.GAME_SEND_PV_PACKET(_out,"");
			SocketManager.GAME_SEND_IH_PACKET(_perso, "");
		}else if(g.isChief(_perso.get_GUID()))//Sinon, c'est qu'il kick un joueur du groupe
		{
			int guid = -1;
			try
			{
				guid = Integer.parseInt(packet.substring(2));
			}catch(NumberFormatException e){return;};
			if(guid == -1)return;
			Characters t = World.getPersonnage(guid);
			g.leave(t);
			SocketManager.GAME_SEND_PV_PACKET(t.get_compte().getGameThread().get_out(),""+_perso.get_GUID());
			SocketManager.GAME_SEND_IH_PACKET(t, "");
		}
	}

	private void group_invite(String packet)
	{
		if(_perso == null)return;
		String name = packet.substring(2);
		if (name.contains("[") || name.contains("]")) 
			name = name.split("-")[1];
		if(name.equalsIgnoreCase("Serveur")){
			SocketManager.GAME_SEND_BN(_perso);
			return;
		}
		Characters target = World.getPersoByName(name);
		if(target == null)return;
		
		if(!target.isOnline())
		{
			SocketManager.GAME_SEND_GROUP_INVITATION_ERROR(_out,"n"+name);
			return;
		}
		if(target.getGroup() != null)
		{
			SocketManager.GAME_SEND_GROUP_INVITATION_ERROR(_out, "a"+name);
			return;
		}
		if(_perso.getGroup() != null && _perso.getGroup().getPersosNumber() == 8)
		{
			SocketManager.GAME_SEND_GROUP_INVITATION_ERROR(_out, "f");
			return;
		}
		target.setInvitation(_perso.get_GUID());	
		_perso.setInvitation(target.get_GUID());
		SocketManager.GAME_SEND_GROUP_INVITATION(_out,_perso.get_name(),name);
		SocketManager.GAME_SEND_GROUP_INVITATION(target.get_compte().getGameThread().get_out(),_perso.get_name(),name);
	}

	public static void group_refuse(Characters _perso)
	{
	    if(_perso == null)
	        return;
	    if(_perso.getInvitation() == 0)
	        return;

	    SocketManager.GAME_SEND_BN(_perso.get_compte().getGameThread().get_out());
	    Characters t = World.getPersonnage(_perso.getInvitation());
	    if(t == null)
	        return;
	    SocketManager.GAME_SEND_PR_PACKET(t);

	    t.setInvitation(0);
	    _perso.setInvitation(0);
	}

	private void group_accept(String packet)
	{
		if(_perso == null)return;
		if(_perso.getInvitation() == 0)return;
		Characters t = World.getPersonnage(_perso.getInvitation());
		if(t == null) return;
		Group g = t.getGroup();
		if(g == null)
		{
			g = new Group(t,_perso);
			SocketManager.GAME_SEND_GROUP_CREATE(_out,g);
			SocketManager.GAME_SEND_PL_PACKET(_out,g);
			SocketManager.GAME_SEND_GROUP_CREATE(t.get_compte().getGameThread().get_out(),g);
			SocketManager.GAME_SEND_PL_PACKET(t.get_compte().getGameThread().get_out(),g);
			t.setGroup(g);
			SocketManager.GAME_SEND_ALL_PM_ADD_PACKET(t.get_compte().getGameThread().get_out(),g);
		}
		else
		{
			SocketManager.GAME_SEND_GROUP_CREATE(_out,g);
			SocketManager.GAME_SEND_PL_PACKET(_out,g);
			SocketManager.GAME_SEND_PM_ADD_PACKET_TO_GROUP(g, _perso);
			g.addPerso(_perso);
		}
		_perso.setGroup(g);
		SocketManager.GAME_SEND_ALL_PM_ADD_PACKET(_out,g);
		SocketManager.GAME_SEND_PR_PACKET(t);
	}

	private void parseObjectPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'd'://Supression d'un objet
				Object_delete(packet);
				break;
			case 'D'://Depose l'objet au sol
				Object_drop(packet);
				break;
			case 'M'://Bouger un objet (Equiper/déséquiper) // Associer obvijevan
				String[] infos = packet.substring(2).split(""+(char)0x0A)[0].split("\\|");
				int qua;
				int guid = Integer.parseInt(infos[0]);
				int pos = Integer.parseInt(infos[1]);
				try
				{
					qua = Integer.parseInt(infos[2]);
				}catch(Exception e)
				{
					qua = 1;
				}
				Object_move(_perso, _out, qua, guid, pos, false);
				break;
			case 'U'://Utiliser un objet (potions)
				Object_use(packet);
				break;
			case 'x':
			    Object_obvijevan_desassocier(packet);
			    break;
			case 'f':
				Object_obvijevan_feed(packet);
				break;
			case 's':
				Object_obvijevan_changeApparence(packet);
		}
	}

	private void Object_drop(String packet)
	{
		//Verification des exploit
		if(CheatCheck.check(packet, _perso)) 
			return;
		
		int guid = -1;
		int qua = -1;
		try
		{
			guid = Integer.parseInt(packet.substring(2).split("\\|")[0]);
			qua = Integer.parseInt(packet.split("\\|")[1]);
		}catch(Exception e){};
		if(guid == -1 || qua <= 0 || !_perso.hasItemGuid(guid) || _perso.get_fight() != null || _perso.is_away())return;
		Objects obj = World.getObjet(guid);
		
		_perso.set_curCell(_perso.get_curCell());
		int cellPosition = Constant.getNearCellidUnused(_perso);
		if(cellPosition < 0)
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1145");
			return;
		}
		if(obj.getPosition() != Constant.ITEM_POS_NO_EQUIPED)
		{
			obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
			SocketManager.GAME_SEND_OBJET_MOVE_PACKET(_perso,obj);
			if(obj.getPosition() == Constant.ITEM_POS_ARME 		||
				obj.getPosition() == Constant.ITEM_POS_COIFFE 		||
				obj.getPosition() == Constant.ITEM_POS_FAMILIER 	||
				obj.getPosition() == Constant.ITEM_POS_CAPE		||
				obj.getPosition() == Constant.ITEM_POS_BOUCLIER	||
				obj.getPosition() == Constant.ITEM_POS_NO_EQUIPED)
					SocketManager.GAME_SEND_ON_EQUIP_ITEM(_perso.get_curCarte(), _perso);
		}
		if(qua >= obj.getQuantity())
		{
			_perso.removeItem(guid);
			_perso.get_curCarte().getCase(_perso.get_curCell().getID()+cellPosition).addDroppedItem(obj);
			obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
			SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, guid);
		}else
		{
			obj.setQuantity(obj.getQuantity() - qua);
			Objects obj2 = Objects.getCloneObjet(obj, qua);
			obj2.setPosition(Constant.ITEM_POS_NO_EQUIPED);
			_perso.get_curCarte().getCase(_perso.get_curCell().getID()+cellPosition).addDroppedItem(obj2);
			SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj);
		}
		SocketManager.GAME_SEND_Ow_PACKET(_perso);
		SocketManager.GAME_SEND_GDO_PACKET_TO_MAP(_perso.get_curCarte(),'+',_perso.get_curCarte().getCase(_perso.get_curCell().getID()+cellPosition).getID(),obj.getTemplate().getID(),0);
		SocketManager.GAME_SEND_STATS_PACKET(_perso);
	}

	private void Object_use(String packet)
	{
		int guid = -1;
		int targetGuid = -1;
		short cellID = -1;
		Characters Target = null;
		try {
			String[] infos = packet.substring(2).split("\\|");
			guid = Integer.parseInt(infos[0]);
			try {
				targetGuid = Integer.parseInt(infos[1]);
			} catch (Exception e) {
				targetGuid = -1;
			}
			;
			try {
				cellID = Short.parseShort(infos[2]);
			} catch (Exception e) {
				cellID = -1;
			}
		} catch (Exception e) {
			return;
		}
		// Si le joueur n'a pas l'objet
		if (World.getPersonnage(targetGuid) != null) {
			Target = World.getPersonnage(targetGuid);
		}
		if (!_perso.hasItemGuid(guid) || _perso.get_fight() != null || _perso.is_away())
			return;
		if (Target != null && (Target.get_fight() != null || Target.is_away()))
			return;
		Objects obj = World.getObjet(guid);
		if (obj == null)
			return;
		ObjTemplate T = obj.getTemplate();
		if ((!obj.getTemplate().getConditions().equalsIgnoreCase("")
				&& !ConditionParser.validConditions(_perso, obj.getTemplate().getConditions()))) {
			SocketManager.GAME_SEND_Im_PACKET(_perso, "119|43");
			return;
		}
		T.applyAction(_perso, Target, guid, cellID);
		SQLManager.SAVE_PERSONNAGE(_perso, true);
	}
	
	public static synchronized void Object_move(Characters _perso, GameSendThread _out, int qua, int guid, int pos, boolean isRapidStuff)
	{
		try
		{
			Objects obj = World.getObjet(guid);
			// LES VERIFS
			if(!_perso.hasItemGuid(guid) || obj == null) // item n'existe pas ou perso n'a pas l'item
				return;
			if(_perso.get_fight() != null) // si en combat démarré
			{
				if(_perso.get_fight().get_state() != Constant.FIGHT_STATE_PLACE) return;
			}
			if(!Constant.isValidPlaceForItem(obj.getTemplate(),pos) && pos != Constant.ITEM_POS_NO_EQUIPED) // si mauvaise place
				return;
			if(!obj.getTemplate().getConditions().equalsIgnoreCase("") && !ConditionParser.validConditions(_perso,obj.getTemplate().getConditions())) {
				SocketManager.GAME_SEND_Im_PACKET(_perso, "119|43"); // si le perso ne vérifie pas les conditions diverses
				return;
			}
			if(obj.getTemplate().getLevel() > _perso.get_lvl())  {// si le perso n'a pas le level
				SocketManager.GAME_SEND_OAEL_PACKET(_out);
				return;
			}
			if (obj.getTemplate().getType() == Constant.ITEM_TYPE_BOUCLIER) {
				Objects cur_arme = _perso.getObjetByPos(Constant.ITEM_POS_ARME);
				if (cur_arme != null && cur_arme.getTemplate() != null && cur_arme.getTemplate().isTwoHanded()) {
					if (isRapidStuff)
						_perso.setEquip(true);
					// Cool on doit enlever le bouclier !
					_perso.DesequiperItem(cur_arme);
					SocketManager.GAME_SEND_Im_PACKET(_perso, "078");
					if (_perso.getObjetByPos(Constant.ITEM_POS_ARME) == null)
						SocketManager.GAME_SEND_OT_PACKET(_out, -1);
				}
			}
			if (pos == Constant.ITEM_POS_ARME) {
				Objects cur_boubou = _perso.getObjetByPos(Constant.ITEM_POS_BOUCLIER);
				if (cur_boubou != null && obj.getTemplate() != null && obj.getTemplate().isTwoHanded()) {
					if (isRapidStuff)
						_perso.setEquip(true);
					_perso.DesequiperItem(cur_boubou);
					SocketManager.GAME_SEND_Im_PACKET(_perso, "079");
					if (_perso.getObjetByPos(Constant.ITEM_POS_ARME) == null)
						SocketManager.GAME_SEND_OT_PACKET(_out, -1);
				}
			}
			//On ne peut équiper 2 items de panoplies identiques, ou 2 Dofus identiques
			if(pos != Constant.ITEM_POS_NO_EQUIPED && (obj.getTemplate().getPanopID() != -1 || obj.getTemplate().getType() == Constant.ITEM_TYPE_DOFUS )&& _perso.hasEquiped(obj.getTemplate().getID()))
				return;
			if(pos == Constant.ITEM_POS_FAMILIER && _perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
			{
				SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
				return;
			}
			// FIN DES VERIFS
			
			
			Objects exObj = _perso.getObjetByPos(pos);//Objet a l'ancienne position
		    int objGUID = obj.getTemplate().getID();
		    // CODE OBVI
			if (obj.getTemplate().getType() == 113)
			{
				// LES VERFIS
				if (exObj == null) 	{// si on place l'obvi sur un emplacement vide
					SocketManager.send(_perso, "Im1161");
					return;	
				}
				if (exObj.getObvijevanPos() != 0) {// si il y a déjà un obvi
					SocketManager.GAME_SEND_BN(_perso);
					return;
				}
				// FIN DES VERIFS
		        		
				exObj.setObvijevanPos(obj.getObvijevanPos()); // L'objet qui était en place a Configtenant un obvi
					
				_perso.removeItem(obj.getGuid(), 1, false, false); // on enlève l'existance de l'obvi en lui-même
				SocketManager.send(_perso, "OR" + obj.getGuid()); // on le précise au org.walaka.rubrumsolem.client
					
				StringBuilder cibleNewStats = new StringBuilder();
				int t = exObj.getTemplate().getID();
				if (t != 8714 && t != 8718 && t!= 8638 && t != 8717 && t != 8719 && t != 8716 && t != 8725 && t != 8724 && t != 8723 && t != 8722 && t != 8721 && t != 8715 && t != 8720 && t != 8668 && t != 8667 && t != 8669 && t != 8665 && t != 8663 && t != 8664 && t != 8670 && t != 8713 && t != 8726 && t != 8727 && t != 8728 && t != 8666 && t != 8647 && t != 8642 && t != 8641 && t != 8640 && t != 8639 && t != 8643 && t != 8650 && t != 8644 && t != 8645 && t != 8648 && t != 8649 && t != 8646 && t != 8636 && t != 8630 && t != 8629 && t != 8631 && t != 8628 && t != 8619 && t != 8632 && t != 8635 && t != 8633 && t != 8634 && t != 8634 && t != 8637 && t != 8660 && t != 8657 && t != 8658 && t != 8651 && t != 8652 && t != 8656 && t != 8659 && t != 8662 && t != 8654 && t != 8653 && t != 8661 && t != 8655){
					cibleNewStats.append(obj.parseStatsStringSansUserObvi(true)).append(",").append(exObj.parseStatsStringSansUserObvi());
					cibleNewStats.append(",3ca#").append(Integer.toHexString(objGUID)).append("#0#0#0d0+").append(objGUID);
				}else {
					cibleNewStats.append(obj.parseStatsStringSansUserObvi(true)).append(",").append(exObj.parseStatsStringSansUserObvi(true));
					cibleNewStats.append(",3ca#").append(Integer.toHexString(objGUID)).append("#0#0#0d0+").append(objGUID);
				}
				exObj.clearStats();
				exObj.parseStringToStats(cibleNewStats.toString());
					
				SocketManager.send(_perso, exObj.obvijevanOCO_Packet(pos));
				SocketManager.GAME_SEND_ON_EQUIP_ITEM(_perso.get_curCarte(), _perso); // Si l'obvi était cape ou coiffe : packet au org.walaka.rubrumsolem.client
				// S'il y avait plusieurs objets
				if(obj.getQuantity() > 1)
				{
					if(qua > obj.getQuantity())
						qua = obj.getQuantity();
					
					if(obj.getQuantity() - qua > 0)//Si il en reste
					{
						int newItemQua = obj.getQuantity()-qua;
						Objects newItem = Objects.getCloneObjet(obj,newItemQua);
						_perso.addObjet(newItem,false);
						World.addObjet(newItem,true);
						obj.setQuantity(qua);
						SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj);
					}
				}
				
				return; // on s'arrête là pour l'obvi
			} // FIN DU CODE OBVI
			
			if(exObj != null)//S'il y avait déja un objet sur cette place on déséquipe
			{
				_perso.setEquip(true);
				_perso.DesequiperItem(exObj);
				if(_perso.getObjetByPos(Constant.ITEM_POS_ARME) == null)
					SocketManager.GAME_SEND_OT_PACKET(_out, -1);
			}else//getNumbEquipedItemOfPanoplie(exObj.getTemplate().getPanopID()
			{
				Objects obj2;
				//On a un objet similaire
				if((obj2 = _perso.getSimilarItem(obj)) != null)
				{
					if(qua > obj.getQuantity()) qua = 
							obj.getQuantity();
					
					obj2.setQuantity(obj2.getQuantity()+qua);
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj2);
					
					if(obj.getQuantity() - qua > 0)//Si il en reste
					{
						obj.setQuantity(obj.getQuantity()-qua);
						SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj);
					}else//Sinon on supprime
					{
						World.removeItem(obj.getGuid());
						_perso.removeItem(obj.getGuid());
						SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, obj.getGuid());
					}
				}
				else//Pas d'objets similaires
				{
					obj.setPosition(pos);
					SocketManager.GAME_SEND_OBJET_MOVE_PACKET(_perso,obj);
					if(obj.getQuantity() > 1)
					{
						if(qua > obj.getQuantity()) qua = obj.getQuantity();
						
						if(obj.getQuantity() - qua > 0)//Si il en reste
						{
							int newItemQua = obj.getQuantity()-qua;
							Objects newItem = Objects.getCloneObjet(obj,newItemQua);
							_perso.addObjet(newItem,false);
							World.addObjet(newItem,true);
							obj.setQuantity(qua);
							SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj);
						}
					}
				}
			}
			if(_perso.CheckItemConditions() != 0)
			{
				pos = obj.getPosition();
			}
			SocketManager.GAME_SEND_Ow_PACKET(_perso);
			_perso.refreshStats();
			if(_perso.getGroup() != null)
			{
				SocketManager.GAME_SEND_PM_MOD_PACKET_TO_GROUP(_perso.getGroup(),_perso);
			}
			SocketManager.GAME_SEND_STATS_PACKET(_perso);
	        if(obj.hasSpellBoostStats())
	        {
        		SocketManager.GAME_SEND_SB_PACKET(_perso, obj.getBoostSpellStats(), (pos!=Constant.ITEM_POS_NO_EQUIPED)?true:false);
	        }
			if( pos == Constant.ITEM_POS_ARME 		||
				pos == Constant.ITEM_POS_COIFFE 	||
				pos == Constant.ITEM_POS_FAMILIER 	||
				pos == Constant.ITEM_POS_CAPE		||
				pos == Constant.ITEM_POS_BOUCLIER	||
				pos == Constant.ITEM_POS_NO_EQUIPED)
				SocketManager.GAME_SEND_ON_EQUIP_ITEM(_perso.get_curCarte(), _perso);
		
			//Si familier
			if(pos == Constant.ITEM_POS_FAMILIER && _perso.isOnMount())_perso.toogleOnMount();
			//Nourir dragodinde
			Mount DD = _perso.getMount();
			if(pos == Constant.ITEM_POS_DRAGODINDE) {
				if(_perso.getMount() == null) {
					SocketManager.GAME_SEND_MESSAGE(_perso, "Votre personnage ne possède pas de dragodinde sur lui, il ne peut donc en nourir ...", Config.CONFIG_MOTD_COLOR);
				} else {
					if (obj.getTemplate().getType() == 41 || obj.getTemplate().getType() == 63) {
						int totalwin = qua * 10;
						if(DD.isInfatiguable() == true) totalwin = qua * 10 * 2;
						int winEnergie = DD.get_energie()+totalwin;
						DD.setEnergie(winEnergie);
						SocketManager.GAME_SEND_Re_PACKET(_perso, "+", DD);
						_perso.deleteItem(guid);
						World.removeItem(guid);
						SocketManager.GAME_SEND_DELETE_STATS_ITEM_FM(_perso, guid);
						SocketManager.GAME_SEND_MESSAGE(_perso, "Votre dragodinde a gagné "+totalwin+" en énergie.", Config.CONFIG_MOTD_COLOR);
					} else {
						SocketManager.GAME_SEND_MESSAGE(_perso, "Nourriture pour dragodinde incomestible !", Config.CONFIG_MOTD_COLOR);
					}
				}
			}
			//Verif pour les outils de métier
			if(pos == Constant.ITEM_POS_NO_EQUIPED && _perso.getObjetByPos(Constant.ITEM_POS_ARME) == null)
				SocketManager.GAME_SEND_OT_PACKET(_out, -1);
			
			if(pos == Constant.ITEM_POS_ARME && _perso.getObjetByPos(Constant.ITEM_POS_ARME) != null)
			{
				int ID = _perso.getObjetByPos(Constant.ITEM_POS_ARME).getTemplate().getID();
				for(Entry<Integer,StatsMetier> e : _perso.getMetiers().entrySet())
				{
					if(e.getValue().getTemplate().isValidTool(ID))
						SocketManager.GAME_SEND_OT_PACKET(_out,e.getValue().getTemplate().getId());
				}
			}
			//Si objet de panoplie
			if(obj.getTemplate().getPanopID() > 0)SocketManager.GAME_SEND_OS_PACKET(_perso,obj.getTemplate().getPanopID());
			//Si en combat
			SQLManager.SAVE_PERSONNAGE(_perso, true);
			if(_perso.get_fight() != null)
			{
				SocketManager.GAME_SEND_ON_EQUIP_ITEM_FIGHT(_perso, _perso.get_fight().getFighterByPerso(_perso), _perso.get_fight());
			}
		}catch(Exception e)
		{
			e.printStackTrace();
			SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(_out);
		}
	}

	private void Object_delete(String packet)
	{
		//Verification des exploit
		if(CheatCheck.check(packet, _perso)) return;
		
		String[] infos = packet.substring(2).split("\\|");
		try
		{
			int guid = Integer.parseInt(infos[0]);
			int qua = 1;
			try
			{
				qua = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			Objects obj = World.getObjet(guid);
			if(obj == null || !_perso.hasItemGuid(guid) || qua <= 0 || _perso.get_fight() != null || _perso.is_away())
			{
				SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(_out);
				return;
			}
			int newQua = obj.getQuantity()-qua;
			if(newQua <=0)
			{
				_perso.removeItem(guid);
				World.removeItem(guid);
				SQLManager.DELETE_ITEM(guid);
				SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, guid);
			}else
			{
				obj.setQuantity(newQua);
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj);
			}
			SocketManager.GAME_SEND_STATS_PACKET(_perso);
			SocketManager.GAME_SEND_Ow_PACKET(_perso);
		}catch(Exception e)
		{
			SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(_out);
		}
	}

	private void parseDialogPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'C'://Demande de l'initQuestion
				Dialog_start(packet);
			break;
			
			case 'R'://Réponse du joueur
				Dialog_response(packet);
			break;
			
			case 'V'://Fin du dialog
				Dialog_end();
			break;
		}
	}

	private void Dialog_response(String packet) {
		//vérification si failles : 
		if(CheatCheck.check(packet, _perso)) return;
		if(_perso.get_isTalkingWith() == 0)return;
		String[] infos = packet.substring(2).split("\\|");
		try
		{
			int qID = Integer.parseInt(infos[0]);
			int rID = Integer.parseInt(infos[1]);
			NPC_question quest = World.getNPCQuestion(qID, _perso);
			NPC_reponse rep = World.getNPCreponse(rID);
			if(quest == null || rep == null || !rep.isAnotherDialog())
			{
				SocketManager.GAME_SEND_END_DIALOG_PACKET(_out);
				_perso.set_isTalkingWith(0);
			}
			rep.apply(_perso);
		}catch(Exception e)
		{
			SocketManager.GAME_SEND_END_DIALOG_PACKET(_out);
		}
	}

	private void Dialog_end()
	{
		SocketManager.GAME_SEND_END_DIALOG_PACKET(_out);
		if(_perso.get_isTalkingWith() != 0)
			_perso.set_isTalkingWith(0);
	}

	private void Dialog_start(String packet)
	{
		try
		{
			int npcID = Integer.parseInt(packet.substring(2).split((char)0x0A+"")[0]);
			NPC npc = _perso.get_curCarte().getNPC(npcID);
			if( npc == null)
				return;
			if(_perso.get_curCarte().getSubArea().get_subscribe() && _perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
			{
				SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
				return;
			}
			SocketManager.GAME_SEND_DCK_PACKET(_out,npcID);
			int qID = npc.get_template().get_initQuestionID();
			NPC_question quest = World.getNPCQuestion(qID, _perso);
			if(quest == null)
			{
				SocketManager.GAME_SEND_END_DIALOG_PACKET(_out);
				return;
			}
			SocketManager.GAME_SEND_QUESTION_PACKET(_out,quest.parseToDQPacket(_perso));
			_perso.set_isTalkingWith(npcID);
		}catch(NumberFormatException e){};
	}

	private void parseExchangePacket(String packet)
	{	
		switch(packet.charAt(1))
		{
			case 'A'://Accepter demande d'échange
				Exchange_accept();
			break;
			case 'B'://Achat
				Exchange_onBuyItem(packet);
			break;
			
			case 'H'://Demande prix moyen + catégorie
				Exchange_HDV(packet);
			break;
			
			case 'K'://Ok
				Exchange_isOK();
			break;
			case 'L'://jobAction : Refaire le craft précedent
				Exchange_doAgain();
			break;
			
			case 'M'://Move (Ajouter//retirer un objet a l'échange)
				Exchange_onMoveItem(packet);
			break;
			
		case 'q':// Mode marchand (demande taxe)
			if (_perso.get_isTradingWith() > 0 || _perso.get_fight() != null || _perso.is_away())
				return;
			if (_perso.get_curCarte().getStoreCount() >= 5) {
				SocketManager.GAME_SEND_Im_PACKET(_perso, "125;5");
				return;
			}
			if (_perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE) {
				SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
				return;
			}
			if (_perso.parseStoreItemsList().isEmpty()) {
				SocketManager.GAME_SEND_Im_PACKET(_perso, "123");
				return;
			}
			long askTaxe = _perso.storeAllBuy() / 1000;
			SocketManager.GAME_SEND_Eq_PACKET(_perso, askTaxe);
			break;
		case 'Q': //Mode marchand (déco)
			if (_perso.get_isTradingWith() > 0 || _perso.get_fight() != null || _perso.is_away())
				return;
			if (_perso.get_curCarte().getStoreCount() >= 5) {
				SocketManager.GAME_SEND_Im_PACKET(_perso, "125;5");
				return;
			}
			if (_perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE) {
				SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
				return;
			}
			if (_perso.parseStoreItemsList().isEmpty()) {
				SocketManager.GAME_SEND_Im_PACKET(_perso, "123");
				return;
			}
			long taxe = _perso.storeAllBuy() / 1000;
			
			if(taxe < 0)
				return;
			if(_perso.get_kamas() < taxe){
				SocketManager.GAME_SEND_Im_PACKET(_perso, "176");
	            return;
			}
			
			_perso.set_kamas(_perso.get_kamas() - taxe);
			int orientation = Formulas.getRandomValue(1, 3);
			_perso.set_orientation(orientation);
			Maps map = _perso.get_curCarte();
			_perso.set_showSeller(true);
			World.addSeller(_perso);
			kick();
			for (Characters z : map.getPersos()) {
				if (z != null && z.isOnline())
					SocketManager.GAME_SEND_MERCHANT_LIST(z, z.get_curCarte().get_id());
			}
			break;
			case 'r'://Rides => Monture
				Exchange_mountPark(packet);
			break;
			
			case 'R'://liste d'achat NPC
				Exchange_start(packet);
			break;
			case 'S'://Vente
				Exchange_onSellItem(packet);
			break;
			
			case 'V'://Fin de l'échange
				Exchange_finish_buy();
			break;
			case 'J'://Livre de métiers
				Book_open(packet.substring(3));
			break;
		}
	}
	
	private void Book_open(String packet) {
		int v = Integer.parseInt(packet);
		if(!_perso.get_curCarte().hasatelierfor(v))return;
		switch(v){
		case 2:
			for(Entry<Integer,StatsMetier> al : World.upB.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 2,i , al.getValue());
			}
			break;
		case 11:
			for(Entry<Integer,StatsMetier> al : World.upFE.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 11,i , al.getValue());
			}
			break;
		case 13:
			for(Entry<Integer,StatsMetier> al : World.upSA.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 13,i , al.getValue());
			}
			break;
		case 14:
			for(Entry<Integer,StatsMetier> al : World.upFM.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 14,i , al.getValue());
			}
			break;
		case 15:
			for(Entry<Integer,StatsMetier> al : World.upCo.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 15,i , al.getValue());
			}
			break;
		case 16:
			for(Entry<Integer,StatsMetier> al : World.upBi.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 16,i , al.getValue());
			}
			break;
		case 17:
			for(Entry<Integer,StatsMetier> al : World.upFD.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 17,i , al.getValue());
			}
			break;
		case 18:
			for(Entry<Integer,StatsMetier> al : World.upSB.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 18,i , al.getValue());
			}
			break;
		case 19:
			for(Entry<Integer,StatsMetier> al : World.upSBg.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 19,i , al.getValue());
			}
			break;
		case 20:
			for(Entry<Integer,StatsMetier> al : World.upFP.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 20,i , al.getValue());
			}
			break;
		case 24:
			for(Entry<Integer,StatsMetier> al : World.upM.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 24,i , al.getValue());
			}
			break;
		case 25:
			for(Entry<Integer,StatsMetier> al : World.upBou.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 25,i , al.getValue());
			}
			break;
		case 26:
			for(Entry<Integer,StatsMetier> al : World.upAlchi.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 26,i , al.getValue());
			}
			break;
		case 27:
			for(Entry<Integer,StatsMetier> al : World.upT.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 27,i , al.getValue());
			}
			break;
		case 28:
			for(Entry<Integer,StatsMetier> al : World.upP.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 28,i , al.getValue());
			}
			break;
		case 31:
			for(Entry<Integer,StatsMetier> al : World.upFH.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 31,i , al.getValue());
			}
			break;
		case 36:
			for(Entry<Integer,StatsMetier> al : World.upFPc.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 36,i , al.getValue());
			}
			break;
		case 41:
			for(Entry<Integer,StatsMetier> al : World.upC.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 41,i , al.getValue());
			}
			break;
		case 43:
			for(Entry<Integer,StatsMetier> al : World.upFMD.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 43,i , al.getValue());
			}
			break;
		case 44:
			for(Entry<Integer,StatsMetier> al : World.upFME.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 44,i , al.getValue());
			}
			break;
		case 45:
			for(Entry<Integer,StatsMetier> al : World.upFMM.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 45,i , al.getValue());
			}
			break;
		case 46:
			for(Entry<Integer,StatsMetier> al : World.upFMP.entrySet()){
				int i = al.getKey();
				SocketManager.GAME_SEND_EJ_PACKET(_perso, 46,i , al.getValue());
			}
			break;
		case 47:
				for(Entry<Integer,StatsMetier> al : World.upFMH.entrySet()){
					int i = al.getKey();
					SocketManager.GAME_SEND_EJ_PACKET(_perso, 47,i , al.getValue());
				}
				break;
			case 48:
				for(Entry<Integer,StatsMetier> al : World.upSMA.entrySet()){
					int i = al.getKey();
					SocketManager.GAME_SEND_EJ_PACKET(_perso, 48,i , al.getValue());
				}
				break;
			case 49:
				for(Entry<Integer,StatsMetier> al : World.upSMB.entrySet()){
					int i = al.getKey();
					SocketManager.GAME_SEND_EJ_PACKET(_perso, 49,i , al.getValue());
				}
				break;
			case 50:
				for(Entry<Integer,StatsMetier> al : World.upSMBg.entrySet()){
					int i = al.getKey();
					SocketManager.GAME_SEND_EJ_PACKET(_perso, 50,i , al.getValue());
				}
				break;
			case 56:
				for(Entry<Integer,StatsMetier> al : World.upBouc.entrySet()){
					int i = al.getKey();
					SocketManager.GAME_SEND_EJ_PACKET(_perso, 56,i , al.getValue());
				}
				break;
			case 58:
				for(Entry<Integer,StatsMetier> al : World.upPO.entrySet()){
					int i = al.getKey();
					SocketManager.GAME_SEND_EJ_PACKET(_perso, 58,i , al.getValue());
				}
				break;
			case 60:
				for(Entry<Integer,StatsMetier> al : World.upFBou.entrySet()){
					int i = al.getKey();
					SocketManager.GAME_SEND_EJ_PACKET(_perso, 60,i , al.getValue());
				}
				break;
			case 63:
				for(Entry<Integer,StatsMetier> al : World.upJM.entrySet()){
					int i = al.getKey();
					SocketManager.GAME_SEND_EJ_PACKET(_perso, 62,i , al.getValue());
				}
				break;
			case 64:
				for(Entry<Integer,StatsMetier> al : World.upCRM.entrySet()){
					int i = al.getKey();
					SocketManager.GAME_SEND_EJ_PACKET(_perso, 64,i , al.getValue());
				}
				break;
			case 65:
				for(Entry<Integer,StatsMetier> al : World.upBrico.entrySet()){
					int i = al.getKey();
					SocketManager.GAME_SEND_EJ_PACKET(_perso, 65,i , al.getValue());
				}
				break;
		default:
			return;
		}
	}
	
	private void Exchange_HDV(String packet)
	{
		if(_perso.get_isTradingWith() > 0 || _perso.get_fight() != null || _perso.is_away())return;
		int templateID;
		switch(packet.charAt(2))
		{
			case 'B': //Confirmation d'achat
				String[] info = packet.substring(3).split("\\|");//ligneID|amount|price
				
				AuctionHouse curHdv = World.getHdv(Math.abs(_perso.get_isTradingWith()));
				
				int ligneID = Integer.parseInt(info[0]);
				byte amount = Byte.parseByte(info[1]);
				
				//verification des failles
				if(CheatCheck.check(packet, _perso)) return;
				
				if(curHdv.buyItem(ligneID,amount,Integer.parseInt(info[2]),_perso))
				{
					SocketManager.GAME_SEND_EHm_PACKET(_perso,"-",ligneID+"");//Enleve la ligne
					if(curHdv.getLigne(ligneID) != null && !curHdv.getLigne(ligneID).isEmpty())
						SocketManager.GAME_SEND_EHm_PACKET(_perso, "+", curHdv.getLigne(ligneID).parseToEHm());//Réajoute la ligne si elle n'est pas vide
					
					/*if(curHdv.getLigne(ligneID) != null)
					{
						String str = curHdv.getLigne(ligneID).parseToEHm();
						SocketManager.GAME_SEND_EHm_PACKET(_perso,"+",str);
					}*/
					
					
					_perso.refreshStats();
					SocketManager.GAME_SEND_Ow_PACKET(_perso);
					SocketManager.GAME_SEND_Im_PACKET(_perso,"068");//Envoie le message "Lot acheté"
				}
				else
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso,"172");//Envoie un message d'erreur d'achat
				}
			break;
			case 'l'://Demande listage d'un template (les prix)
				templateID = Integer.parseInt(packet.substring(3));
				try
				{
					SocketManager.GAME_SEND_EHl(_perso,World.getHdv(Math.abs(_perso.get_isTradingWith())),templateID);
				}catch(NullPointerException e)//Si erreur il y a, retire le template de la liste chez le org.walaka.rubrumsolem.client
				{
					SocketManager.GAME_SEND_EHM_PACKET(_perso,"-",templateID+"");
				}
				
			break;
			case 'P'://Demande des prix moyen
				templateID = Integer.parseInt(packet.substring(3));
				SocketManager.GAME_SEND_EHP_PACKET(_perso,templateID);
			break;			
			case 'T'://Demande des template de la catégorie
				int categ = Integer.parseInt(packet.substring(3));
				String allTemplate = World.getHdv(Math.abs(_perso.get_isTradingWith())).parseTemplate(categ);
				SocketManager.GAME_SEND_EHL_PACKET(_perso,categ,allTemplate);
			break;			
		}
	}
	
	private void Exchange_mountPark(String packet)
	{
		//Si dans un enclos
		if(_perso.getInMountPark() != null)
		{
			MountPark MP = _perso.getInMountPark();
			if(_perso.get_isTradingWith() > 0 || _perso.get_fight() != null)return;
			char c = packet.charAt(2);
			packet = packet.substring(3);
			int guid = -1;
			try
			{
				guid = Integer.parseInt(packet);
			}catch(Exception e){};
			switch(c)
			{
				case 'C'://Parcho => Etable (Stocker)
					if(guid == -1 || !_perso.hasItemGuid(guid))return;
					if(MP.get_size() <= MP.MountParkDATASize())
					{
						SocketManager.GAME_SEND_Im_PACKET(_perso, "1145");
						return;
					}
					Objects obj = World.getObjet(guid);
					//on prend la DD demandée
					int DDid = obj.getStats().getEffect(995);
					Mount DD = World.getDragoByID(DDid);
					//FIXME mettre return au if pour ne pas créer des nouvelles dindes
					if(DD == null)
					{
						int color = Constant.getMountColorByParchoTemplate(obj.getTemplate().getID());
						if(color <1)return;
						DD = new Mount(color);
					}
					//On enleve l'objet du Monde et du Perso
					_perso.removeItem(guid);
					World.removeItem(guid);
					//on ajoute la dinde a l'étable
					MP.addData(DD.get_id(), _perso.get_GUID());
					SQLManager.UPDATE_MOUNTPARK(MP);
					//On envoie les packet
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso,obj.getGuid());
					SocketManager.GAME_SEND_Ee_PACKET(_perso, '+', DD.parse());
				break;
				case 'c'://Etable => Parcho(Echanger)
					Mount DD1 = World.getDragoByID(guid);
					//S'il n'a pas la dinde
					if(DD1 == null || !MP.getData().containsKey(DD1.get_id()))return;
					if(MP.getData().get(DD1.get_id()) != _perso.get_GUID() && 
						World.getPersonnage(MP.getData().get(DD1.get_id())).get_guild() != _perso.get_guild())
					{
						//Pas la même guilde, pas le même perso
						return;
					}
					if(MP.getData().get(DD1.get_id()) != _perso.get_GUID() && 
							World.getPersonnage(MP.getData().get(DD1.get_id())).get_guild() == _perso.get_guild() &&
							!_perso.getGuildMember().canDo(Constant.G_OTHDINDE))
					{
						//Même guilde, pas le droit
						SocketManager.GAME_SEND_Im_PACKET(_perso, "1101");
						return;
					}
					//on retire la dinde de l'étable
					MP.removeData(DD1.get_id());
					SQLManager.UPDATE_MOUNTPARK(MP);
					//On créer le parcho
					ObjTemplate T = Constant.getParchoTemplateByMountColor(DD1.get_color());
					Objects obj1 = T.createNewItem(1, false, -1);
					//On efface les stats
					obj1.clearStats();
					//on ajoute la possibilité de voir la dinde
					obj1.getStats().addOneStat(995, DD1.get_id());
					obj1.addTxtStat(996, _perso.get_name());
					obj1.addTxtStat(997, DD1.get_nom());
					
					//On ajoute l'objet au joueur
					World.addObjet(obj1, true);
					_perso.addObjet(obj1, false);//Ne seras jamais identique de toute
					
					//Packets
					SocketManager.GAME_SEND_Ow_PACKET(_perso);
					SocketManager.GAME_SEND_Ee_PACKET(_perso,'-',DD1.get_id()+"");
				break;
				case 'g'://Equiper
					Mount DD3 = World.getDragoByID(guid);
					//S'il n'a pas la dinde
					if(DD3 == null || !MP.getData().containsKey(DD3.get_id()) || _perso.getMount() != null)return;
					if(World.getPersonnage(MP.getData().get(DD3.get_id())) == null) return;
					if(MP.getData().get(DD3.get_id()) != _perso.get_GUID() && 
							World.getPersonnage(MP.getData().get(DD3.get_id())).get_guild() != _perso.get_guild())
					{
						//Pas la même guilde, pas le même perso
						return;
					}
					if(MP.getData().get(DD3.get_id()) != _perso.get_GUID() && 
							World.getPersonnage(MP.getData().get(DD3.get_id())).get_guild() == _perso.get_guild() &&
							!_perso.getGuildMember().canDo(Constant.G_OTHDINDE))
					{
						//Même guilde, pas le droit
						SocketManager.GAME_SEND_Im_PACKET(_perso, "1101");
						return;
					}
					
					MP.removeData(DD3.get_id());
					SQLManager.UPDATE_MOUNTPARK(MP);
					_perso.setMount(DD3);
					
					//Packets
					SocketManager.GAME_SEND_Re_PACKET(_perso, "+", DD3);
					SocketManager.GAME_SEND_Ee_PACKET(_perso,'-',DD3.get_id()+"");
					SocketManager.GAME_SEND_Rx_PACKET(_perso);
				break;
				case 'p'://Equipé => Stocker
					//Si c'est la dinde équipé
					if(_perso.getMount()!=null?_perso.getMount().get_id() == guid:false)
					{
						//Si le perso est sur la monture on le fait descendre
						if(_perso.isOnMount())_perso.toogleOnMount();
						//Si ca n'a pas réussie, on s'arrete là (Items dans le sac ?)
						if(_perso.isOnMount())return;
						
						Mount DD2 = _perso.getMount();
						MP.addData(DD2.get_id(), _perso.get_GUID());
						SQLManager.UPDATE_MOUNTPARK(MP);
						_perso.setMount(null);
						
						//Packets
						SocketManager.GAME_SEND_Ee_PACKET(_perso,'+',DD2.parse());
						SocketManager.GAME_SEND_Re_PACKET(_perso, "-", null);
						SocketManager.GAME_SEND_Rx_PACKET(_perso);
					}else//Sinon...
					{
						
					}
				break;
			}
		}
	}

	private void Exchange_doAgain() {
		if (_perso.getCurJobAction() != null) {
			_perso.getCurJobAction().putLastCraftIngredients();
		}
	}

	private void Exchange_isOK() {
		if (_perso.getCurJobAction() != null) {
			//Si pas action de craft, on s'arrete la
			if (!_perso.getCurJobAction().isCraft()) {
				return;
			}
			_perso.getCurJobAction().startCraft(_perso);
		}
		if (_perso.get_curExchange() == null) {
			return;
		}
		_perso.get_curExchange().toogleOK(_perso.get_GUID());
	}
	
	/**
	 * 
	 * @param packet
	 */
	private void Exchange_onMoveItem(String packet) {

		//Dragodinde (inventaire)
		if (_perso.isInDinde()) {
			Mount drago = _perso.getMount();
			if (drago == null)
				return;
			switch (packet.charAt(2)) {
			case 'O':// Objet
				int id = 0;
				int qua = 0;
				try {
					id = Integer.parseInt(packet.substring(4).split("\\|")[0]);
					qua = Integer.parseInt(packet.substring(4).split("\\|")[1]);
					//verification des failles
					/**if(Security.isCompromised(packet, _perso)) return;**/// Echange impossible ! Bouleto, packet.contains("-"), il y en aura toujours !
				} catch (Exception e) {e.printStackTrace();}
				if (id < 0 || qua <= 0)
					return;
				if (World.getObjet(id) == null) {
					SocketManager.GAME_SEND_MESSAGE(_perso,
							"Cet objet n'existe pas ou bug, changes de perso pour en être sûr.", Config.CONFIG_MOTD_COLOR);
					return;
				}
				switch (packet.charAt(3)) {
				case '+':
					drago.addObjToSac(id, qua, _perso);
					break;
				case '-':
					drago.deleteFromSac(id, qua, _perso);
					break;
				}
				break;
			}
			return;
		}	
		//Store
		if(_perso.get_isTradingWith() == _perso.get_GUID()) {
			switch(packet.charAt(2)) {
			case 'O'://Objets
				if(packet.charAt(3) == '+') {
					String[] infos = packet.substring(4).split("\\|");
					try {
						//verification des failles
						/**if(Security.isCompromised(packet, _perso)) return;**/// Echange impossible ! Bouleto, packet.contains("-"), il y en aura toujours !
						
						int guid = Integer.parseInt(infos[0]);
						int qua  = Integer.parseInt(infos[1]);
						int price  = Integer.parseInt(infos[2]);
						
						Objects obj = World.getObjet(guid);
						if(obj == null)return;
						
						if(qua > obj.getQuantity())
							qua = obj.getQuantity();
						
						_perso.addinStore(obj.getGuid(), price, qua);
						
					} catch(NumberFormatException e) { };
				} else {
					String[] infos = packet.substring(4).split("\\|");
					try {
						int guid = Integer.parseInt(infos[0]);
						int qua  = Integer.parseInt(infos[1]);
						
						//verification des failles
						/**if(Security.isCompromised(packet, _perso)) return;**/// Echange impossible ! Bouleto, packet.contains("-"), il y en aura toujours !
						
						if(qua <= 0)return;
						
						Objects obj = World.getObjet(guid);
						if(obj == null)return;
						if(qua > obj.getQuantity())return;
						if(qua < obj.getQuantity()) qua = obj.getQuantity();
						
						_perso.removeFromStore(obj.getGuid(), qua);
					} catch(NumberFormatException e) { };
				}
				break;
			}
			return;
		}
		
		//Percepteur
		if(_perso.get_isOnPercepteurID() != 0) {
			Collector perco = World.getPerco(_perso.get_isOnPercepteurID());
			
			if(perco == null || perco.get_inFight() > 0)return;
			
			switch(packet.charAt(2)) {
			case 'G'://Kamas
				if(packet.charAt(3) == '-') { //On retire
					long P_Kamas = Integer.parseInt(packet.substring(4));
					if(P_Kamas < 0)
						return;
					if(perco.getKamas() >= P_Kamas)	{//FIXME: A tester Faille non connu ! :p
						long P_Retrait = perco.getKamas()-P_Kamas;
						perco.setKamas(perco.getKamas() - P_Kamas);
						//verification des failles
						/**if(Security.isCompromised(packet, _perso)) return;**/// Echange impossible ! Bouleto, packet.contains("-"), il y en aura toujours !	
						if(P_Retrait < 0) {
							P_Retrait = 0;
							P_Kamas = perco.getKamas();
						}
						perco.setKamas(P_Retrait);
						_perso.addKamas(P_Kamas);
						SocketManager.GAME_SEND_STATS_PACKET(_perso);
						SocketManager.GAME_SEND_EsK_PACKET(_perso,"G"+perco.getKamas());
					}
				}
				break;
				
			case 'O'://Objets
				if(packet.charAt(3) == '-') { //On retire
					String[] infos = packet.substring(4).split("\\|");
					int guid = 0;
					int qua = 0;
					
					try {
						//verification des failles
						/**if(Security.isCompromised(packet, _perso)) return;**/// Echange impossible ! Bouleto, packet.contains("-"), il y en aura toujours !
						
						guid = Integer.parseInt(infos[0]);
						qua  = Integer.parseInt(infos[1]);
					} catch(NumberFormatException e) { };
					
					if(guid <= 0 || qua <= 0) return;
					
					Objects obj = World.getObjet(guid);
					if(obj == null)return;

					if(perco.HaveObjet(guid))
						perco.removeFromPercepteur(_perso, guid, qua);
					
					perco.LogObjetDrop(guid, obj);
				}
				break;
			}
			_perso.get_guild().addXp(perco.getXp());
			perco.LogXpDrop(perco.getXp());
			perco.setXp(0);
			SQLManager.UPDATE_GUILD(_perso.get_guild());
			return;
		}	
		//HDV
		if(_perso.get_isTradingWith() < 0) {
			switch(packet.charAt(3)) {
			case '-'://Retirer un objet de l'HDV
				int cheapestID = Integer.parseInt(packet.substring(4).split("\\|")[0]);
				int count = Integer.parseInt(packet.substring(4).split("\\|")[1]);
				if(count <= 0)return;
				
				//verification des failles
				/**if(Security.isCompromised(packet, _perso)) return;**/// Echange impossible ! Bouleto, packet.contains("-"), il y en aura toujours !
				
				_perso.get_compte().recoverItem(cheapestID,count);//Retire l'objet de la liste de vente du compte
				SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(_out,'-',"",cheapestID+"");
				break;
				
			case '+'://Mettre un objet en vente
				int itmID = Integer.parseInt(packet.substring(4).split("\\|")[0]);
				byte amount = Byte.parseByte(packet.substring(4).split("\\|")[1]);
				int price = Integer.parseInt(packet.substring(4).split("\\|")[2]);
				if(amount <= 0 || price <= 0)return;
				
				//verification des failles
				if(CheatCheck.check(packet, _perso)) return;
					
				AuctionHouse curHdv = World.getHdv(Math.abs(_perso.get_isTradingWith()));
				int taxe = (int)(price * (curHdv.getTaxe()/100));
					
					
				if(!_perso.hasItemGuid(itmID))//Vérifie si le personnage a bien l'item spécifié et l'argent pour payer la taxe
					return;
				
				if(_perso.get_compte().countHdvItems(curHdv.getHdvID()) >= curHdv.getMaxItemCompte()) {
					SocketManager.GAME_SEND_Im_PACKET(_perso, "058");
					return;
				}
				
				if(_perso.get_kamas() < taxe) {
					SocketManager.GAME_SEND_Im_PACKET(_perso, "176");
					return;
				}
				
				_perso.addKamas(taxe *-1);//Retire le montant de la taxe au personnage
					
				SocketManager.GAME_SEND_STATS_PACKET(_perso);//Met a jour les kamas du org.walaka.rubrumsolem.client
					
				Objects obj = World.getObjet(itmID);//Récupère l'item
				if(amount > obj.getQuantity())//S'il veut mettre plus de cette objet en vente que ce qu'il possède
					return;
					
				int rAmount = (int)(Math.pow(10,amount)/10);
				int newQua = (obj.getQuantity()-rAmount);
					
				if(newQua <= 0) { //Si c'est plusieurs objets ensemble enleve seulement la quantité de mise en vente
					_perso.removeItem(itmID);//Enlève l'item de l'inventaire du personnage
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso,itmID);//Envoie un packet au org.walaka.rubrumsolem.client pour retirer l'item de son inventaire
				} else {
					obj.setQuantity(obj.getQuantity() - rAmount);
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso,obj);
						
					Objects newObj = Objects.getCloneObjet(obj, rAmount);
					World.addObjet(newObj, true);
					obj = newObj;
				}
					
				HdvEntry toAdd = new HdvEntry(price,amount,_perso.get_compte().get_GUID(),obj);
				curHdv.addEntry(toAdd);	//Ajoute l'entry dans l'HDV
					
				SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(_out,'+',"",toAdd.parseToEmK());	//Envoie un packet pour ajouter l'item dans la fenetre de l'HDV du org.walaka.rubrumsolem.client
				break;
			}
			return;
		}
		
		//Metier
		//Metier
		if (_perso.getCurJobAction() != null) {
			//Si pas action de craft, on s'arrete la
			if (!_perso.getCurJobAction().isCraft()) {
				SocketManager.GAME_SEND_MESSAGE(_perso, "Vous êtes déjà entrain de craft !", Config.CONFIG_MOTD_COLOR);
				return;
			}
			if (packet.charAt(2) == 'O')//Ajout d'objet
			{
				if (packet.charAt(3) == '+') {
					//FIXME gerer les packets du genre  EMO+173|5+171|5+172|5 (split sur '+' ?:/)
					String[] infos = packet.substring(3).split("\\|");
					try {
						int guid = Integer.parseInt(infos[0]);
						int qua = Integer.parseInt(infos[1]);
						if (qua <= 0) {
							return;
						}
						
						//verification des failles
						/**if(Security.isCompromised(packet, _perso)) return;**/// Echange impossible ! Bouleto, packet.contains("-"), il y en aura toujours !
						
						
						if (!_perso.hasItemGuid(guid)) {
							return;
						}
						Objects obj = World.getObjet(guid);
						if (obj == null) {
							return;
						}
						if (obj.getQuantity() < qua) {
							qua = obj.getQuantity();
						}
						_perso.getCurJobAction().modifIngredient(_perso, guid, qua);
					} catch (NumberFormatException e) {
					}
					;
				} else {
					String[] infos = packet.substring(4).split("\\|");
					try {
						int guid = Integer.parseInt(infos[0]);
						int qua = Integer.parseInt(infos[1]);
						
						
						if (qua <= 0) {
							return;
						}
						Objects obj = World.getObjet(guid);
						if (obj == null) {
							return;
						}
						_perso.getCurJobAction().modifIngredient(_perso, guid, -qua);
					} catch (NumberFormatException e) {
					}
					;
				}

			} else if (packet.charAt(2) == 'R') {
				try {
					int c = Integer.parseInt(packet.substring(3));
					_perso.getCurJobAction().startRepeat(c, _perso);
					Logs.addToFmLog("Personnage "+_perso.get_name()+" has started FmRepeat.");
				} catch (Exception e) {
				}
				;
			} else if (packet.charAt(2) == 'r') { //Return | Skryn :D
				try {
					_perso.getCurJobAction().breakFM();
					Logs.addToFmLog("Personnage "+_perso.get_name()+" has broke Fm in Repeat.");
				} catch (Exception e) {
				}
				;
			}
			return;
		}
		//Banque
		if(_perso.isInBank()) 
		{
			if(_perso.get_curExchange() != null)
				return;
			switch(packet.charAt(2)) 
			{
				case 'G'://Kamas
					long kamas = 0;
					try {
						kamas = Integer.parseInt(packet.substring(3));
					} catch(Exception e) { };
					if(kamas == 0)
						return;
					if(kamas > 0) { //Si On ajoute des kamas a la banque
						//verification des failles
						/**if(Security.isCompromised(packet, _perso)) return;**/// Echange impossible ! Bouleto, packet.contains("-"), il y en aura toujours !
							
						if(_perso.get_kamas() < kamas)kamas = _perso.get_kamas();
						_perso.setBankKamas(_perso.getBankKamas()+kamas);//On ajoute les kamas a la banque
						_perso.set_kamas(_perso.get_kamas()-kamas);//On retire les kamas du personnage
						SocketManager.GAME_SEND_STATS_PACKET(_perso);
						SocketManager.GAME_SEND_EsK_PACKET(_perso,"G"+_perso.getBankKamas());
					} else {
						kamas = -kamas;//On repasse en positif
						if(_perso.getBankKamas() < kamas)kamas = _perso.getBankKamas();
						_perso.setBankKamas(_perso.getBankKamas()-kamas);//On retire les kamas de la banque
						_perso.set_kamas(_perso.get_kamas()+kamas);//On ajoute les kamas du personnage
						SocketManager.GAME_SEND_STATS_PACKET(_perso);
						SocketManager.GAME_SEND_EsK_PACKET(_perso,"G"+_perso.getBankKamas());
					}
				break;
					
				case 'O'://Objet
					int guid = 0;
					int qua = 0;
					try {
						guid = Integer.parseInt(packet.substring(4).split("\\|")[0]);
						qua = Integer.parseInt(packet.substring(4).split("\\|")[1]);
						
						//verification des failles
						/**if(Security.isCompromised(packet, _perso)) return;**/// Echange impossible ! Bouleto, packet.contains("-"), il y en aura toujours !
					}catch(Exception e){};
					if(guid == 0 || qua <= 0 || qua > 100000)return;
						
					switch(packet.charAt(3)) {
					case '+'://Ajouter a la banque
						_perso.addInBank(guid,qua);
						break;
							
					case '-'://Retirer de la banque
						_perso.removeFromBank(guid,qua);
						break;
					}
				break;
			}
			return;
		}
		//Coffre
		if(_perso.getInTrunk() != null) {
			if(_perso.get_curExchange() != null)return;
			if(_perso.get_fight() != null) return;
			Trunk t = _perso.getInTrunk();
			if(t == null) return;
               
			switch(packet.charAt(2)) {
			case 'G'://Kamas
				long kamas = 0;
				try {
					kamas = Integer.parseInt(packet.substring(3));
					
				} catch(Exception e) { };
				if(kamas == 0)return;
                               
				if(kamas > 0) { //Si On ajoute des kamas au coffre
					if(_perso.get_kamas() < kamas)kamas = _perso.get_kamas();
					t.set_kamas(t.get_kamas() + kamas);//On ajoute les kamas au coffre
					_perso.set_kamas(_perso.get_kamas()-kamas);//On retire les kamas du personnage
					SocketManager.GAME_SEND_STATS_PACKET(_perso);
				} else { // On retire des kamas au coffre
					kamas = -kamas;//On repasse en positif
					if(t.get_kamas() < kamas)kamas = t.get_kamas();
					t.set_kamas(t.get_kamas()-kamas);//On retire les kamas de la banque
					_perso.set_kamas(_perso.get_kamas()+kamas);//On ajoute les kamas du personnage
					SocketManager.GAME_SEND_STATS_PACKET(_perso);
				}
				
				for(Characters P : World.getOnlinePersos())
					if(P.getInTrunk() != null && _perso.getInTrunk().get_id() == P.getInTrunk().get_id())
						SocketManager.GAME_SEND_EsK_PACKET(P,"G"+t.get_kamas());
				SQLManager.UPDATE_TRUNK(t);
				break;
              	
			case 'O'://Objet
				int guid = 0;
				int qua = 0;
				try {
					guid = Integer.parseInt(packet.substring(4).split("\\|")[0]);
					qua = Integer.parseInt(packet.substring(4).split("\\|")[1]);
					
					//verification des failles
					/**if(Security.isCompromised(packet, _perso)) return;**/// Echange impossible ! Bouleto, packet.contains("-"), il y en aura toujours !
				} catch(Exception e) { };
				if (guid == 0 || qua <= 0)
					return;
				switch(packet.charAt(3)) {
				case '+'://Ajouter a la banque
					t.addInTrunk(guid, qua, _perso);
					break;
                                       
				case '-'://Retirer de la banque
					t.removeFromTrunk(guid,qua, _perso);
					break;
				}
				break;
			}
			return;
		}
		
		if(_perso.get_curExchange() == null)
			return;
		switch(packet.charAt(2)) {
			case 'O'://Objet ?
				if(packet.charAt(3) == '+') {
					String[] infos = packet.substring(4).split("\\|");
					try {
						int guid = Integer.parseInt(infos[0]);
						int qua  = Integer.parseInt(infos[1]);
						//verification des failles
						if(CheatCheck.check(packet, _perso)) return;
						int quaInExch = _perso.get_curExchange().getQuaItem(guid, _perso.get_GUID());
						
						if(!_perso.hasItemGuid(guid))return;
						if(_perso.getItems().get(guid).getQuantity() < qua)return;
						Objects obj = World.getObjet(guid);
						if(obj == null)return;
						if (obj.getQuantity() < qua)
							return;
						if(qua > obj.getQuantity()-quaInExch)
							qua = obj.getQuantity()-quaInExch;
						if(qua <= 0)return;
						
						_perso.get_curExchange().addItem(guid,qua,_perso.get_GUID());
					} catch(NumberFormatException e) { };
				} else {
					String[] infos = packet.substring(4).split("\\|");
					try {
						int guid = Integer.parseInt(infos[0]);
						int qua  = Integer.parseInt(infos[1]);
						
						//verification des failles
						//System.out.println(Security.isCompromised(packet, _perso)); //Already is TRUE contains '-' !
						/**if(Security.isCompromised(packet, _perso)) // Echange impossible ! Bouleto, packet.contains("-"), il y en aura toujours !
							return;**/
						System.out.println("2");
						if(qua <= 0)
							return;
						if(!_perso.hasItemGuid(guid))
							return;
						if(_perso.getItems().get(guid).getQuantity() < qua)
							return;
						Objects obj = World.getObjet(guid);
						if(obj == null)
							return;
						if (obj.getQuantity() < qua)
							return;
						if(qua > _perso.get_curExchange().getQuaItem(guid, _perso.get_GUID()))
							return;
						_perso.get_curExchange().removeItem(guid,qua,_perso.get_GUID());
					} catch(NumberFormatException e) { };
				}
				break;
				
			case 'G'://Kamas
				try {
					long numb = Integer.parseInt(packet.substring(3));
					if(_perso.get_kamas() < numb)
						numb = _perso.get_kamas();
					_perso.get_curExchange().setKamas(_perso.get_GUID(), numb);
				} catch(NumberFormatException e) { };
				break;
		}
	}

	private void Exchange_accept()
	{
		if(_perso.get_isTradingWith() == 0)return;
		Characters target = World.getPersonnage(_perso.get_isTradingWith());
		if(target == null)return;
		SocketManager.GAME_SEND_EXCHANGE_CONFIRM_OK(_out,1);
		SocketManager.GAME_SEND_EXCHANGE_CONFIRM_OK(target.get_compte().getGameThread().get_out(),1);
		World.Exchange echg = new World.Exchange(target,_perso);
		_perso.setCurExchange(echg);
		_perso.set_isTradingWith(target.get_GUID());
		target.setCurExchange(echg);
		target.set_isTradingWith(_perso.get_GUID());
	}

	private void Exchange_onSellItem(String packet)
	{
		try
		{
			String[] infos = packet.substring(2).split("\\|");
			int guid = Integer.parseInt(infos[0]);
			int qua = Integer.parseInt(infos[1]);
			if(!_perso.hasItemGuid(guid))
			{
				SocketManager.GAME_SEND_SELL_ERROR_PACKET(_out);
				return;
			}
			_perso.sellItem(guid, qua);
		}catch(Exception e)
		{
			SocketManager.GAME_SEND_SELL_ERROR_PACKET(_out);
		}
	}
	// TODO MODE MARCHAND PB
	private void Exchange_onBuyItem(String packet)
	{
		String[] infos = packet.substring(2).split("\\|");
		
        if (_perso.get_isTradingWith() > 0) 
        {
            Characters seller = World.getPersonnage(_perso.get_isTradingWith());
            if (seller != null) 
            {
            	int itemID = 0;
            	int qua = 0;
            	int price = 0;
            	
            	try
        		{
            		itemID = Integer.valueOf(infos[0]);
            		qua = Integer.valueOf(infos[1]);
        		}catch(Exception e){return;}
        		
                if (!seller.getStoreItems().containsKey(itemID) || qua <= 0) 
                {
                    SocketManager.GAME_SEND_BUY_ERROR_PACKET(_out);
                    return;
                }
                price = seller.getStoreItems().get(itemID);
                Objects itemStore = World.getObjet(itemID);

                if(itemStore == null) return;
                
                if(qua > itemStore.getQuantity()) qua = itemStore.getQuantity();
                if(qua == itemStore.getQuantity())
                {
                	seller.getStoreItems().remove(itemStore.getGuid());
                	_perso.addObjet(itemStore, true);
                }
                else // si l'échange peut se faire
                {
                	seller.getStoreItems().remove(itemStore.getGuid()); // on enlève entièrement l'objet en vente
                	itemStore.setQuantity(itemStore.getQuantity()-qua); // on modifie la quantité dans le magasin
                	SQLManager.SAVE_ITEM(itemStore);					// on sauvegarde le magasin
                	seller.addStoreItem(itemStore.getGuid(), price);	// on remet dans le magasin
                	
                	Objects clone = Objects.getCloneObjet(itemStore, qua);	// on clone l'objet acheté
                    SQLManager.SAVE_NEW_ITEM(clone);					// on sauvegarde celui-ci
                    _perso.addObjet(clone, true);						// et on le donne au joueur
                }
	            //remove kamas
	            _perso.addKamas(-price * qua);
	            //add seller kamas
	            seller.addKamas(price * qua);
	            SQLManager.SAVE_PERSONNAGE(seller, true);
	            SQLManager.SAVE_PERSONNAGE(this._perso, true);
	            //send packets
	            SocketManager.GAME_SEND_STATS_PACKET(_perso);
	            SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(seller, _perso);
	            SocketManager.GAME_SEND_BUY_OK_PACKET(_out);
	            if(seller.getStoreItems().isEmpty())
	            {
	            	if(World.getSeller(seller.get_curCarte().get_id()) != null && World.getSeller(seller.get_curCarte().get_id()).contains(seller.get_GUID()))
	        		{
	        			World.removeSeller(seller.get_GUID(), seller.get_curCarte().get_id());
	        			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(seller.get_curCarte(), seller.get_GUID());
	        			Exchange_finish_buy();
	        		}
	            }
            }
            return;
        }
        
		try
		{
			int tempID = Integer.parseInt(infos[0]);
			int qua = Integer.parseInt(infos[1]);
			
			//verification des failles
			if(CheatCheck.check(packet, _perso)) return;
			
			if(qua <= 0) return;
			
			ObjTemplate template = World.getObjTemplate(tempID);
			if(template == null)//Si l'objet demandé n'existe pas(ne devrait pas arrivé)
			{
				Logs.addToGameLog(_perso.get_name()+" tente d'acheter l'itemTemplate "+tempID+" qui est inexistant");
				SocketManager.GAME_SEND_BUY_ERROR_PACKET(_out);
				return;
			}
			if(!_perso.get_curCarte().getNPC(_perso.get_isTradingWith()).get_template().haveItem(tempID))//Si le PNJ ne vend pas l'objet voulue
			{
				Logs.addToGameLog(_perso.get_name()+" tente d'acheter l'itemTemplate "+tempID+" que le present PNJ ne vend pas");
				SocketManager.GAME_SEND_BUY_ERROR_PACKET(_out);
				return;
			}
			int prix = template.getPrix() * qua;
			int priceVIP = template.getPointsPrice();
			if ((priceVIP > 0)) {
				prix = priceVIP * qua;
				int AccPoints = SQLManager.GET_ACCOUNT_POINTS(_perso.getAccID());
				if (AccPoints < prix) {
					SocketManager.GAME_SEND_MESSAGE(_perso, "Action impossible, il te manque "
					+ (prix - AccPoints) + " points.", Config.CONFIG_MOTD_COLOR);
					return;
				}
				int newPoints = AccPoints - prix;
				SQLManager.SET_ACCOUNT_POINTS(newPoints, _perso.getAccID());
				SocketManager.GAME_SEND_MESSAGE(
						_perso,
						"<b>Le Serveur</b> vous remercie de votre achat , il vous reste<b> "
								+ newPoints + " </b>points.", Config.CONFIG_MOTD_COLOR);
			}
			if(_perso.get_kamas()<prix)//Si le joueur n'a pas assez de kamas
			{
				Logs.addToGameLog(_perso.get_name()+" tente d'acheter l'itemTemplate "+tempID+" mais n'a pas l'argent necessaire");
				SocketManager.GAME_SEND_BUY_ERROR_PACKET(_out);
				return;
			}
			Objects newObj = template.createNewItem(qua,false,-1);
			long newKamas = _perso.get_kamas() - prix;
			_perso.set_kamas(newKamas);
			if(_perso.addObjet(newObj,true))//Return TRUE si c'est un nouvel item
				World.addObjet(newObj,true);
			SocketManager.GAME_SEND_BUY_OK_PACKET(_out);
			SocketManager.GAME_SEND_STATS_PACKET(_perso);
			SocketManager.GAME_SEND_Ow_PACKET(_perso);
		}catch(Exception e)
		{
			e.printStackTrace();
			SocketManager.GAME_SEND_BUY_ERROR_PACKET(_out);
			return;
		};
	}

	private void Exchange_finish_buy()
	{
		if(_perso.get_isTradingWith() == 0 &&
		   _perso.get_curExchange() == null &&
		   _perso.getCurJobAction() == null &&
		   _perso.getInMountPark() == null &&
		   !_perso.isInBank() &&
		   _perso.get_isOnPercepteurID() == 0 &&
		   _perso.getInTrunk() == null)
			return;
		
		//Si échange avec un personnage
		if(_perso.get_curExchange() != null)
		{
			_perso.get_curExchange().cancel();
			_perso.set_isTradingWith(0);
			_perso.set_away(false);
			return;
		}
		//Si métier
		if(_perso.getCurJobAction() != null)
		{
			_perso.getCurJobAction().resetCraft();
		}
		//Si dans un enclos
		if(_perso.getInMountPark() != null)_perso.leftMountPark();
		//prop d'echange avec un joueur
		if(_perso.get_isTradingWith() > 0)
		{
			Characters p = World.getPersonnage(_perso.get_isTradingWith());
			if(p != null)
			{
				if(p.isOnline())
				{
					GameSendThread out = p.get_compte().getGameThread().get_out();
					SocketManager.GAME_SEND_EV_PACKET(out);
					p.set_isTradingWith(0);
				}
			}
		}
		//Si perco
		if(_perso.get_isOnPercepteurID() != 0)
		{
			Collector perco = World.getPerco(_perso.get_isOnPercepteurID());
			if(perco == null) return;
			for(Characters z : World.getGuild(perco.get_guildID()).getMembers())
			{
				if(z == null) continue;
				if(z.isOnline())
				{
					SocketManager.GAME_SEND_gITM_PACKET(z, Collector.parsetoGuild(z.get_guild().get_id()));
					String str = "";
					str += "G"+perco.get_N1()+","+perco.get_N2();
					str += "|.|"+World.getCarte((short)perco.get_mapID()).getX()+"|"+World.getCarte((short)perco.get_mapID()).getY()+"|";
					str += _perso.get_name()+"|";
					str += perco.get_LogXp();
					str += perco.get_LogItems();
					SocketManager.GAME_SEND_gT_PACKET(z, str);
				}
			}
			_perso.get_curCarte().RemoveNPC(perco.getGuid());
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.get_curCarte(), perco.getGuid());
			perco.DelPerco(perco.getGuid());
			SQLManager.DELETE_PERCO(perco.getGuid());
			_perso.set_isOnPercepteurID(0);
		}
		
		SQLManager.SAVE_PERSONNAGE(_perso,true);
		SocketManager.GAME_SEND_EV_PACKET(_out);
		_perso.set_isTradingWith(0);
		_perso.set_away(false);
		_perso.setInBank(false);
		_perso.setInTrunk(null);
	}

	private void Exchange_start(String packet)
	{
		if(packet.substring(2,4).equals("11"))//Ouverture HDV achat
		{
			if(_perso.get_isTradingWith() < 0)//Si déjà ouvert
				SocketManager.GAME_SEND_EV_PACKET(_out);
			
			if(_perso.getDeshonor() >= 5) 
			{
				SocketManager.GAME_SEND_Im_PACKET(_perso, "183");
				return;
			}
			
			if(_perso.get_curCarte().getSubArea().get_subscribe() && _perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
			{
				SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
				return;
			}
			
			AuctionHouse toOpen = World.getHdv(_perso.get_curCarte().get_id());
			
			if(toOpen == null) return;
			
			String info = "1,10,100;"+
						toOpen.getStrCategories()+
						";"+toOpen.parseTaxe()+
						";"+toOpen.getLvlMax()+
						";"+toOpen.getMaxItemCompte()+
						";-1;"+
						toOpen.getSellTime();
			SocketManager.GAME_SEND_ECK_PACKET(_perso,11,info);
			_perso.set_isTradingWith(0 - _perso.get_curCarte().get_id());	//Récupère l'ID de la map et rend cette valeur négative
			return;
		}
		else if(packet.substring(2,4).equals("10"))//Ouverture HDV vente
		{
			if(_perso.get_isTradingWith() < 0)//Si déjà ouvert
				SocketManager.GAME_SEND_EV_PACKET(_out);
			
			if(_perso.getDeshonor() >= 5) 
			{
				SocketManager.GAME_SEND_Im_PACKET(_perso, "183");
				return;
			}
			
			if(_perso.get_curCarte().getSubArea().get_subscribe() && _perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
			{
				SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
				return;
			}
			
			AuctionHouse toOpen = World.getHdv(_perso.get_curCarte().get_id());
			
			if(toOpen == null) return;
			
			String info = "1,10,100;"+
						toOpen.getStrCategories()+
						";"+toOpen.parseTaxe()+
						";"+toOpen.getLvlMax()+
						";"+toOpen.getMaxItemCompte()+
						";-1;"+
						toOpen.getSellTime();
			SocketManager.GAME_SEND_ECK_PACKET(_perso,10,info);
			_perso.set_isTradingWith(0 - _perso.get_curCarte().get_id());	//Récupère l'ID de la map et rend cette valeur négative
			
			SocketManager.GAME_SEND_HDVITEM_SELLING(_perso);
			return;
		} else if (packet.substring(2, 4).equals("15")) {//Dinde (inventaire)
					try {
						Mount mount = _perso.getMount();
						int mountID = mount.get_id();
						SocketManager.GAME_SEND_ECK_PACKET(_out, 15, _perso.getMount().get_id() + "");
						SocketManager.GAME_SEND_EL_MOUNT_INVENTAIRE(_out, mount);
						SocketManager.GAME_SEND_MOUNT_PODS(_perso, mount.getPodsActuels());
						_perso.set_isTradingWith(mountID);
						_perso.setInDinde(true);
						_perso.set_away(true);
					} catch (Exception e) {}
					return;
				}
		switch(packet.charAt(2))
		{
			case '0'://Si NPC
				try
				{
					int npcID = Integer.parseInt(packet.substring(4));
					NpcTemplates.NPC npc = _perso.get_curCarte().getNPC(npcID);
					if(_perso.get_curCarte().getSubArea().get_subscribe() && _perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
					{
						SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
						return;
					}
					if(npc == null)return;
					SocketManager.GAME_SEND_ECK_PACKET(_out, 0, npcID+"");
					SocketManager.GAME_SEND_ITEM_VENDOR_LIST_PACKET(_perso,npc);
					_perso.set_isTradingWith(npcID);
				}catch(NumberFormatException e){};
			break;
			case '1'://Si joueur
				try
				{
				int guidTarget = Integer.parseInt(packet.substring(4));
				Characters target = World.getPersonnage(guidTarget);
				if(target == null )
				{
					SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(_out,'E');
					return;
				}
				if(target.get_curCarte()!= _perso.get_curCarte() || !target.isOnline())//Si les persos ne sont pas sur la meme map
				{
					SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(_out,'E');
					return;
				}
				if(target.is_away() || _perso.is_away() || target.get_isTradingWith() != 0)
				{
					SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(_out,'O');
					return;
				}
				SocketManager.GAME_SEND_EXCHANGE_REQUEST_OK(_out, _perso.get_GUID(), guidTarget,1);
				SocketManager.GAME_SEND_EXCHANGE_REQUEST_OK(target.get_compte().getGameThread().get_out(),_perso.get_GUID(), guidTarget,1);
				_perso.set_isTradingWith(guidTarget);
				target.set_isTradingWith(_perso.get_GUID());
			}catch(NumberFormatException e){}
			break;
			case '4':// StorePlayer
				int pID = 0;
				// int cellID = 0;//Inutile
				try {
					pID = Integer.valueOf(packet.split("\\|")[1]);
					// cellID = Integer.valueOf(packet.split("\\|")[2]);
				} catch (NumberFormatException e) {
					return;
				}
				;
				if (_perso.get_isTradingWith() > 0 || _perso.get_fight() != null || _perso.is_away())
					return;
				Characters seller = World.getPersonnage(pID);
				if (seller == null)
					return;
				_perso.set_isTradingWith(pID);
				SocketManager.GAME_SEND_ECK_PACKET(_perso, 4, seller.get_GUID() + "");
				SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(seller, _perso);
				break;
			case '6'://StoreItems
				if(_perso.get_isTradingWith() > 0 || _perso.get_fight() != null || _perso.is_away())return;
                _perso.set_isTradingWith(_perso.get_GUID());
                SocketManager.GAME_SEND_ECK_PACKET(_perso, 6, "");
                SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(_perso, _perso);
			break;
			case '8'://Si Percepteur
				try
				{
					int PercepteurID = Integer.parseInt(packet.substring(4));
					Collector perco = World.getPerco(PercepteurID);
					if(perco == null || perco.get_inFight() > 0) return;
					if(perco.get_Exchange())
					{
						Characters p = World.getPersonnage(perco.getExchangeWith());
						if(p != null && p.isOnline() && p.get_isOnPercepteurID() == PercepteurID)
						{
							SocketManager.GAME_SEND_Im_PACKET(_perso, "1180");
							return;
						}
						else
						{
							perco.set_Exchange(false);
							perco.setExchangeWith(-1);
						}
					}
					perco.set_Exchange(true);
					perco.setExchangeWith(_perso.get_GUID());
					SocketManager.GAME_SEND_ECK_PACKET(_out, 8, perco.getGuid()+"");
					SocketManager.GAME_SEND_ITEM_LIST_PACKET_PERCEPTEUR(_out, perco);
					_perso.set_isTradingWith(perco.getGuid());
					_perso.set_isOnPercepteurID(perco.getGuid());
				}catch(NumberFormatException e){};
			break;
		}
	}

	private void parse_environementPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'D'://Change direction
				Environement_change_direction(packet);
			break;
			
			case 'U'://Emote
				Environement_emote(packet);
			break;
		}
	}

	private void Environement_emote(String packet)
	{
		int emote = -1;
		try
		{
			emote = Integer.parseInt(packet.substring(2));
		}catch(Exception e){};
		if(emote == -1)return;
		if(_perso == null)return;
		if(_perso.get_fight() != null)return;//Pas d'émote en combat
		
		switch(emote)//effets spéciaux des émotes
		{
			case 19://s'allonger 
			case 1:// s'asseoir
				_perso.setSitted(!_perso.isSitted());
			break;
		}
		if(_perso.emoteActive() == emote)_perso.setEmoteActive(0);
		else _perso.setEmoteActive(emote);
		
		//System.out.println("Set Emote "+_perso.emoteActive());
		//System.out.println("Is sitted "+_perso.isSitted());
		
		SocketManager.GAME_SEND_eUK_PACKET_TO_MAP(_perso.get_curCarte(), _perso.get_GUID(), _perso.emoteActive());
	}

	private void Environement_change_direction(String packet)
	{
		try
		{
			if(_perso.get_fight() != null)return;
			int dir = Integer.parseInt(packet.substring(2));
			_perso.set_orientation(dir);
			SocketManager.GAME_SEND_eD_PACKET_TO_MAP(_perso.get_curCarte(),_perso.get_GUID(),dir);
		}catch(NumberFormatException e){return;};
	}

	private void parseSpellPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'B':
				boostSort(packet);
			break;
			case 'F'://Oublie de sort
				forgetSpell(packet);
			break;
			case'M':
				addToSpellBook(packet);
			break;
		}
	}

	private void addToSpellBook(String packet)
	{
		try
		{
			int SpellID = Integer.parseInt(packet.substring(2).split("\\|")[0]);
			int Position = Integer.parseInt(packet.substring(2).split("\\|")[1]);
			SortStats Spell = _perso.getSortStatBySortIfHas(SpellID);
			
			if(Spell != null)
			{
				_perso.set_SpellPlace(SpellID, CryptManager.getHashedValueByInt(Position));
			}
				
			SocketManager.GAME_SEND_BN(_out);
		}catch(Exception e){};
	}

	private void boostSort(String packet)
	{
		try
		{
			int id = Integer.parseInt(packet.substring(2));
			Logs.addToGameLog("Info: "+_perso.get_name()+": Tente BOOST sort id="+id);
			if(_perso.boostSpell(id))
			{
				Logs.addToGameLog("Info: "+_perso.get_name()+": OK pour BOOST sort id="+id);
				SocketManager.GAME_SEND_SPELL_UPGRADE_SUCCED(_out, id, _perso.getSortStatBySortIfHas(id).getLevel());
				SocketManager.GAME_SEND_STATS_PACKET(_perso);
			}else
			{
				Logs.addToGameLog("Info: "+_perso.get_name()+": Echec BOOST sort id="+id);
				SocketManager.GAME_SEND_SPELL_UPGRADE_FAILED(_out);
				return;
			}
		}catch(NumberFormatException e){SocketManager.GAME_SEND_SPELL_UPGRADE_FAILED(_out);return;};
	}

	private void forgetSpell(String packet)
	{
		if(!_perso.isForgetingSpell())return;
		
		int id = Integer.parseInt(packet.substring(2));
		
		Logs.addToGameLog("Info: "+_perso.get_name()+": Tente Oublie sort id="+id);
		
		if(_perso.forgetSpell(id))
		{
			Logs.addToGameLog("Info: "+_perso.get_name()+": OK pour Oublie sort id="+id);
			SocketManager.GAME_SEND_SPELL_UPGRADE_SUCCED(_out, id, _perso.getSortStatBySortIfHas(id).getLevel());
			SocketManager.GAME_SEND_STATS_PACKET(_perso);
			_perso.setisForgetingSpell(false);
		}
	}

	private void parseFightPacket(String packet)
	{
		try
		{
			switch(packet.charAt(1))
			{
				case 'D'://Détails d'un combat (liste des combats)
					int key = -1;
					try
					{
						key = Integer.parseInt(packet.substring(2).replace(((int)0x0)+"", ""));
					}catch(Exception e){};
					if(key == -1)return;
					SocketManager.GAME_SEND_FIGHT_DETAILS(_out,_perso.get_curCarte().get_fights().get(key));
				break;
				
				case 'H'://Aide
					if(_perso.get_fight() == null)return;
					_perso.get_fight().toggleHelp(_perso.get_GUID());
				break;
				
				case 'L'://Lister les combats
					SocketManager.GAME_SEND_FIGHT_LIST_PACKET(_out, _perso.get_curCarte());
				break;
				case 'N'://Bloquer le combat
					if(_perso.get_fight() == null)return;
					_perso.get_fight().toggleLockTeam(_perso.get_GUID());
				break;
				case 'P'://Seulement le groupe
					if(_perso.get_fight() == null || _perso.getGroup() == null)return;
					_perso.get_fight().toggleOnlyGroup(_perso.get_GUID());
				break;
				case 'S'://Bloquer les specs
					if(_perso.get_fight() == null)return;
					_perso.get_fight().toggleLockSpec(_perso.get_GUID());
				break;
				
			}
		}catch(Exception e){e.printStackTrace();};
	}

	private void parseBasicsPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'A'://Console
				Basic_console(packet);
			break;
			case 'a' : //Téléportation via géoposition... //-WalakaZ- & Skryn
				switch(packet.charAt(2)) {
				case 'M' :
					try {
						if (_perso.get_compte().get_gmLvl() > 0){
							
							String posComplete = packet.substring(3).trim() 
									+ "," 
									+ 0;
							if(World.cartesByPos.containsKey(posComplete))
							{
								Maps c = World.cartesByPos.get(posComplete);
								if(Constant.getMapForbidden(c.get_id()))
									return;
								_perso.teleport((short)c.get_id(), c.getRandomFreeCellID());
							}
						}else 
							return;
						
					} catch(Exception e) { }
					break;
					
					default : //fuck
						break;
				}
				break;
			case 'D':
				Basic_send_Date_Hour();
			break;
			case 'M':
				Basic_chatMessage(packet);
			break;
			case 'W':
				Basic_infosmessage(packet);
			break;
			case 'S':
				_perso.emoticone(packet.substring(2));
			break;
			case 'Y':
				Basic_state(packet);
			break;
		}
	}
	public void Basic_state(String packet)
	{
		switch(packet.charAt(2))
		{
			case 'A': //Absent
				if(_perso._isAbsent)
				{

					SocketManager.GAME_SEND_Im_PACKET(_perso, "038");

					_perso._isAbsent = false;
				}
				else

				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "037");
					_perso._isAbsent = true;
				}
			break;
			case 'I': //Invisible
				if(_perso._isInvisible)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "051");
					_perso._isInvisible = false;
				}
				else
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "050");
					_perso._isInvisible = true;
				}
			break;
		}
	}
	
	public Characters getPerso()
	{
		return _perso;
	}
	  
	private void Basic_console(String packet)
	{
		if (command == null)
			command = new GmCommand(_perso);
		command.consoleCommand(packet);
	}

	public void closeSocket()
	{
		try {
			this._s.close();
		} catch (IOException e) {}
	}
	
	private void Basic_chatMessage(String packet)
	{
		String msg = "";
		String log_msg = "";
		if (_perso.isMuted()) {
			if (_perso.get_compte() != null){
				int temps_restant = (int) (_perso.get_compte().getMuteTime() - System.currentTimeMillis() / 1000);
				SocketManager.GAME_SEND_Im_PACKET(_perso, "1124;" + temps_restant);
				return;
			}
			return;
		}
		packet = packet.replace("<", "");
		packet = packet.replace(">", "");
		if(packet.length() == 3)return;
		switch(packet.charAt(2))
		{
			case '*'://Canal noir
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				msg = packet.split("\\|",2)[1];
				if(msg.isEmpty()) return;
				//On check si le gars viens faire sa pub
				for(String blacklisted : Config.wordProhibited){
				    if(msg.toLowerCase().replaceAll("-", "").contains(blacklisted) 
				    		|| msg.toLowerCase().replaceAll(" ", "").contains(blacklisted))
				    {
				    	SocketManager.GAME_SEND_Im_PACKET(_perso, "116;<b>Serveur</b>~Votre message contient un mot interdit, il n'a donc pas été envoyé.");
						return;
				    }
				}
				// On check si le fils de pute flood
				if (FloodCheck.isFlooding(_perso, msg)){
					SocketManager.GAME_SEND_Im_PACKET(_perso, "116;<b>Serveur</b>~Votre message a été modéré par l'antiflood automatique."
							+ " Modérez votre vitesse de frappe où vous risquez d'en perdre la voix.");
					return;
				}
				if(msg.charAt(0) == '.' && msg.charAt(1) != '.')
				{					
				if (PlayerCommand.launchNewCommand(msg, _perso, _compte, _timeLastsave, _out))
					return;
				}
				
				if(_perso.get_fight() == null)
				{
					if(_perso.get_curCarte().isMuted() && _perso.get_compte().get_gmLvl() == 0)
					{
						SocketManager.GAME_SEND_MESSAGE(_perso, "La map actuelle a été mutée.", Config.CONFIG_MOTD_COLOR);
						return;
					}
					log_msg = "[Map "+_perso.get_curCarte().get_id()+"] : "+msg;
					SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(_perso.get_curCarte(), "", _perso.get_GUID(), _perso.get_tag() != null ? _perso.get_tag() + _perso.get_name() : _perso.get_name(), msg);
				}
				else
				{
					SocketManager.GAME_SEND_cMK_PACKET_TO_FIGHT(_perso.get_fight(), 7, "", _perso.get_GUID(), _perso.get_tag() != null ? _perso.get_tag() + _perso.get_name() : _perso.get_name(), msg);
					log_msg = "[Map "+_perso.get_curCarte().get_id()+" Combat "+_perso.get_fight().get_id()+"] : "+msg;
				}
				FloodCheck.updateFloodInfos(_perso, msg);
			break;
			case '#'://Canal Equipe
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				if(_perso.get_fight() != null)
				{
					msg = packet.split("\\|",2)[1];
					//On check si le gars viens faire sa pub
					for(String blacklisted : Config.wordProhibited){
					    if(msg.toLowerCase().replaceAll("-", "").contains(blacklisted) 
					    		|| msg.toLowerCase().replaceAll(" ", "").contains(blacklisted))
					    {
					    	SocketManager.GAME_SEND_Im_PACKET(_perso, "116;<b>Serveur</b>~Votre message contient un mot interdit, il n'a donc pas été envoyé.");
							return;
					    }
					}
					if (PlayerCommand.launchNewCommand(msg, _perso, _compte, _timeLastsave, _out))
						return;
					if (FloodCheck.isFlooding(_perso, msg))
						return;
					int team = _perso.get_fight().getTeamID(_perso.get_GUID());
					if(team == -1)return;
					SocketManager.GAME_SEND_cMK_PACKET_TO_FIGHT(_perso.get_fight(), team, "#", _perso.get_GUID(), _perso.get_tag() != null ? _perso.get_tag() + _perso.get_name() : _perso.get_name(), msg);
					log_msg = "[Map "+_perso.get_curCarte().get_id()+" Combat "+_perso.get_fight().get_id()+" Equipe "+team+"] : "+msg;
					FloodCheck.updateFloodInfos(_perso, msg);
				}
			break;
			case '$'://Canal groupe
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				if(_perso.getGroup() == null)break;
				msg = packet.split("\\|",2)[1];
				//On check si le gars viens faire sa pub
				for(String blacklisted : Config.wordProhibited){
				    if(msg.toLowerCase().replaceAll("-", "").contains(blacklisted) 
				    		|| msg.toLowerCase().replaceAll(" ", "").contains(blacklisted))
				    {
				    	SocketManager.GAME_SEND_Im_PACKET(_perso, "116;<b>Serveur</b>~Votre message contient un mot interdit, il n'a donc pas été envoyé.");
						return;
				    }
				}
				if (PlayerCommand.launchNewCommand(msg, _perso, _compte, _timeLastsave, _out))
					return;
				if (FloodCheck.isFlooding(_perso, msg))
					return;
				SocketManager.GAME_SEND_cMK_PACKET_TO_GROUP(_perso.getGroup(), "$", _perso.get_GUID(), _perso.get_tag() != null ? _perso.get_tag() + _perso.get_name() : _perso.get_name(), msg);
				log_msg = "[Groupe "+_perso.getGroup().getChief().get_GUID()+"] : "+msg;
				FloodCheck.updateFloodInfos(_perso, msg);
			break;
			
			case ':'://Canal commerce
				if(_perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE || _perso.get_lvl() < 6)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "0157;"+ "6");
					return;
				}
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				long l;
				if((l = System.currentTimeMillis() - _timeLastTradeMsg) < Config.FLOOD_TIME && _perso.get_compte().get_gmLvl() < 3)
				{
					l = (Config.FLOOD_TIME  - l)/1000;//On calcul la différence en secondes
					SocketManager.GAME_SEND_Im_PACKET(_perso, "0115;"+((int)Math.ceil(l)+1));
					FloodCheck.updateFloodInfos(_perso, msg);
					return;
				}
				_timeLastTradeMsg = System.currentTimeMillis();
				msg = packet.split("\\|",2)[1];
				//On check si le gars viens faire sa pub
				for(String blacklisted : Config.wordProhibited){
				    if(msg.toLowerCase().replaceAll("-", "").contains(blacklisted) 
				    		|| msg.toLowerCase().replaceAll(" ", "").contains(blacklisted))
				    {
				    	SocketManager.GAME_SEND_Im_PACKET(_perso, "116;<b>Serveur</b>~Votre message contient un mot interdit, il n'a donc pas été envoyé.");
						return;
				    }
				}
				if (PlayerCommand.launchNewCommand(msg, _perso, _compte, _timeLastsave, _out))
					return;
				log_msg = "[Commerce] : "+msg;
				FloodCheck.updateFloodInfos(_perso, msg);
				SocketManager.GAME_SEND_cMK_PACKET_TO_ALL(":", _perso.get_GUID(), _perso.get_tag() != null ? _perso.get_tag() + _perso.get_name() : _perso.get_name(), msg);
			break;
			case '@'://Canal Admin
				if(_perso.get_compte().get_gmLvl() ==0)return;
				msg = packet.split("\\|",2)[1];
				if (PlayerCommand.launchNewCommand(msg, _perso, _compte, _timeLastsave, _out))
					return;
				if (FloodCheck.isFlooding(_perso, msg))
					return;
				SocketManager.GAME_SEND_cMK_PACKET_TO_ADMIN("@", _perso.get_GUID(), _perso.get_name(), msg);
				log_msg = "[Admin] : "+msg;
				FloodCheck.updateFloodInfos(_perso, msg);
			break;
			case '?'://Canal recrutement
				if(_perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE || _perso.get_lvl() < 6)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "0157;"+ "6");
					return;
				}
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				long j;
				if((j = System.currentTimeMillis() - _timeLastRecrutmentMsg) < Config.FLOOD_TIME && _perso.get_compte().get_gmLvl() < 3)
				{
					j = (Config.FLOOD_TIME  - j)/1000;//On calcul la différence en secondes
					SocketManager.GAME_SEND_Im_PACKET(_perso, "0115;"+((int)Math.ceil(j)+1));
					FloodCheck.updateFloodInfos(_perso, msg);
					return;
				}
				_timeLastRecrutmentMsg = System.currentTimeMillis();
				msg = packet.split("\\|",2)[1];
				//On check si le gars viens faire sa pub
				for(String blacklisted : Config.wordProhibited){
				    if(msg.toLowerCase().replaceAll("-", "").contains(blacklisted) 
				    		|| msg.toLowerCase().replaceAll(" ", "").contains(blacklisted))
				    {
				    	SocketManager.GAME_SEND_Im_PACKET(_perso, "116;<b>Serveur</b>~Votre message contient un mot interdit, il n'a donc pas été envoyé.");
						return;
				    }
				}
				if (PlayerCommand.launchNewCommand(msg, _perso, _compte, _timeLastsave, _out))
					return;
				SocketManager.GAME_SEND_cMK_PACKET_TO_ALL("?", _perso.get_GUID(), _perso.get_tag() != null ? _perso.get_tag() + _perso.get_name() : _perso.get_name(), msg);
				log_msg = "[Recrutement] : "+msg;
				FloodCheck.updateFloodInfos(_perso, msg);
			break;
			case '%'://Canal guilde
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				if(_perso.get_guild() == null)return;
				msg = packet.split("\\|",2)[1];
				//On check si le gars viens faire sa pub
				for(String blacklisted : Config.wordProhibited){
				    if(msg.toLowerCase().replaceAll("-", "").contains(blacklisted) 
				    		|| msg.toLowerCase().replaceAll(" ", "").contains(blacklisted))
				    {
				    	SocketManager.GAME_SEND_Im_PACKET(_perso, "116;<b>Serveur</b>~Votre message contient un mot interdit, il n'a donc pas été envoyé.");
						return;
				    }
				}
				if (PlayerCommand.launchNewCommand(msg, _perso, _compte, _timeLastsave, _out))
					return;
				if (FloodCheck.isFlooding(_perso, msg))
					return;
				SocketManager.GAME_SEND_cMK_PACKET_TO_GUILD(_perso.get_guild(), "%", _perso.get_GUID(), _perso.get_tag() != null ? _perso.get_tag() + _perso.get_name() : _perso.get_name(), msg);
				log_msg = "[Guide "+_perso.get_guild().get_name()+"] : "+msg;
				FloodCheck.updateFloodInfos(_perso, msg);
			break;
			case 0xC2://Canal 
			break;
			case '!'://Alignement
				if(_perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE || _perso.get_lvl() < 6)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "0157;"+ "6");
					return;
				}
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				if(_perso.get_align() == 0) return;
				if(_perso.getDeshonor() >= 1) 
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "183");
					return;
				}
				long k;
				if((k = System.currentTimeMillis() - _timeLastAlignMsg) < Config.FLOOD_TIME && _perso.get_compte().get_gmLvl() < 3)
				{
					k = (Config.FLOOD_TIME  - k)/1000;//On calcul la différence en secondes
					SocketManager.GAME_SEND_Im_PACKET(_perso, "0115;"+((int)Math.ceil(k)+1));
					return;
				}
				_timeLastAlignMsg = System.currentTimeMillis();
				msg = packet.split("\\|",2)[1];
				//On check si le gars viens faire sa pub
				for(String blacklisted : Config.wordProhibited){
				    if(msg.toLowerCase().replaceAll("-", "").contains(blacklisted) 
				    		|| msg.toLowerCase().replaceAll(" ", "").contains(blacklisted))
				    {
				    	SocketManager.GAME_SEND_Im_PACKET(_perso, "116;<b>Serveur</b>~Votre message contient un mot interdit, il n'a donc pas été envoyé.");
						return;
				    }
				}
				if (PlayerCommand.launchNewCommand(msg, _perso, _compte, _timeLastsave, _out))
					return;
				SocketManager.GAME_SEND_cMK_PACKET_TO_ALIGN("!", _perso.get_GUID(), _perso.get_tag() != null ? _perso.get_tag() + _perso.get_name() : _perso.get_name(), msg, _perso);
				log_msg = "[Alignement "+_perso.get_align()+"] : "+msg;
				FloodCheck.updateFloodInfos(_perso, msg);
			break;
			case '^':// Canal Incarnam 
				msg = packet.split("\\|", 2)[1]; 
				//On check si le gars viens faire sa pub
				for(String blacklisted : Config.wordProhibited){
				    if(msg.toLowerCase().replaceAll("-", "").contains(blacklisted) 
				    		|| msg.toLowerCase().replaceAll(" ", "").contains(blacklisted))
				    {
				    	SocketManager.GAME_SEND_Im_PACKET(_perso, "116;<b>Serveur</b>~Votre message contient un mot interdit, il n'a donc pas été envoyé.");
						return;
				    }
				}
				long x; 
				if((x = System.currentTimeMillis() - _timeLastIncarnamMsg) < Config.FLOOD_TIME && _perso.get_compte().get_gmLvl() < 3) 
				{
					x = (Config.FLOOD_TIME - x)/1000;//On calcul la différence en secondes
					SocketManager.GAME_SEND_Im_PACKET(_perso, "0115;"+((int)Math.ceil(x)+1)); 
					return; 
				} 
				_timeLastIncarnamMsg = System.currentTimeMillis();
				if (PlayerCommand.launchNewCommand(msg, _perso, _compte, _timeLastsave, _out))
					return;
				SocketManager.GAME_SEND_cMK_PACKET_INCARNAM_CHAT(_perso, "^", _perso.get_GUID(), _perso.get_tag() != null ? _perso.get_tag() + _perso.get_name() : _perso.get_name(), msg); 
				log_msg = "[Incarnam] : "+msg;
				FloodCheck.updateFloodInfos(_perso, msg);
			break;
			default:
				String nom = packet.substring(2).split("\\|")[0];
				msg = packet.split("\\|",2)[1];
				// Pour MP un gars qui à un tag
				if (nom.contains("[") || nom.contains("]")) 
					nom = nom.split("-")[1];
				
				if(nom.length() <= 1)
					Logs.addToGameLog("ChatHandler: Chanel non gere : "+nom);
				else
				{
					Characters target = World.getPersoByName(nom);
					if(target == null)//si le personnage n'existe pas
					{
						SocketManager.GAME_SEND_CHAT_ERROR_PACKET(_out, nom);
						return;
					}
					if(target.get_compte() == null)
					{
						SocketManager.GAME_SEND_CHAT_ERROR_PACKET(_out, nom);
						return;
					}
					if(target.get_compte().getGameThread() == null)//si le perso n'est pas co
					{
						SocketManager.GAME_SEND_CHAT_ERROR_PACKET(_out, nom);
						return;
					}
					if(target.get_compte().isEnemyWith(_perso.get_compte().get_GUID()) == true || !target.isDispo(_perso))
					{
						SocketManager.GAME_SEND_Im_PACKET(_perso, "114;"+target.get_name());
						return;
					}
					if(target.get_compte().isMuted()) // si le perso est muté
						SocketManager.GAME_SEND_Im_PACKET(_perso, "0168;" + target.get_name() + "~" + (target.get_compte().getMuteTime() - System.currentTimeMillis() / 1000));
						
					SocketManager.GAME_SEND_cMK_PACKET(target, "F", _perso.get_GUID(), _perso.get_tag() != null ? _perso.get_tag() + _perso.get_name() : _perso.get_name().trim(), msg);
					SocketManager.GAME_SEND_cMK_PACKET(_perso, "T", target.get_GUID(), target.get_tag() != null ? target.get_tag() + target.get_name() : target.get_name().trim(), msg);
					log_msg = "[MP à "+target.get_name()+"] : "+msg;
				}
			break;
		}
		Logs.addToChatLog("["+_compte.get_curIP()+"] ("+_compte.get_name()+", "+_perso.get_name()+") "+log_msg);
	}

	private void Basic_send_Date_Hour()
	{
		SocketManager.GAME_SEND_SERVER_DATE(_out);
		SocketManager.GAME_SEND_SERVER_HOUR(_out);
	}
	
	private void Basic_infosmessage(String packet)
	{
		packet = packet.substring(2);
		if(packet.equalsIgnoreCase("Serveur")){
			SocketManager.GAME_SEND_BN(_perso);
			return;
		}
		if (packet.contains("[") || packet.contains("]")) 
			packet = packet.split("-")[1];
		Characters T = World.getPersoByName(packet);
		if (T == null)
			return;
		if(T.get_compte().isFriendWith(_perso.get_GUID()) || T == _perso){
			if(T.get_compte().get_gmLvl() > 0)
				SocketManager.GAME_SEND_BWK(_perso, "<b>[STAFF]</b> " + T.get_compte().get_pseudo() + "|1|" + T.get_name() + "|" + T.get_curCarte().getSubArea().getArea().getID());
			else
				SocketManager.GAME_SEND_BWK(_perso, T.get_compte().get_pseudo() + "|1|" + T.get_name() + "|" + T.get_curCarte().getSubArea().getArea().getID());	
		}
		else if(T.get_compte().get_gmLvl() > 0)
			SocketManager.GAME_SEND_BWK(_perso, "<b>[STAFF]</b> " + T.get_compte().get_pseudo() + "|1|" + T.get_name() + "|-1");
		else
			SocketManager.GAME_SEND_BWK(_perso, T.get_compte().get_pseudo() + "|1|" + T.get_name() + "|-1");
	}

	private void parseGamePacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'A':
				if(_perso == null)
					return;
				parseGameActionPacket(packet);
			break;
			case 'C':
				if(_perso == null)
					return;
				_perso.sendGameCreate();
			break;
			case 'd': // demande de reciblage challenge
				Game_on_Gdi_packet(packet);
			case 'f':
				Game_on_showCase(packet);
			break;
			case 'I':
				Game_on_GI_packet();
			break;
			case 'K':
				Game_on_GK_packet(packet);
			break;
			case 'P'://PvP Toogle
				_perso.toggleWings(packet.charAt(2));
			break;
			case 'p':
				Game_on_ChangePlace_packet(packet);
			break;
			case 'Q':
				Game_onLeftFight(packet);
			break;
			case 'R':
				Game_on_Ready(packet);
			break;
			case 't':
				if(_perso.get_fight() == null)return;
				_perso.get_fight().playerPass(_perso);
			break;
		}
	}

	
	private void Game_onLeftFight(String packet)
	{
		int targetID = -1;
		if(!packet.substring(2).isEmpty())
		{
			try
			{
				targetID = Integer.parseInt(packet.substring(2));
			}catch(Exception e){};
		}
		if(_perso.get_fight() == null)return;
		if(targetID > 0)//Expulsion d'un joueurs autre que soi-meme
		{
			if(_perso.getArena() == 1 || _perso.getKolizeum() == 1){
	    		SocketManager.GAME_SEND_MESSAGE(_perso, "Impossible d'expulser un joueur en arène !", Config.CONFIG_MOTD_COLOR);
	    		return;
	    	}
			Characters target = World.getPersonnage(targetID);
			//On ne quitte pas un joueur qui : est null, ne combat pas, n'est pas de ça team.
			if(target == null || target.get_fight() == null || target.get_fight().getTeamID(target.get_GUID()) != _perso.get_fight().getTeamID(_perso.get_GUID()))return;
			_perso.get_fight().leftFight(_perso, target, false);
			
		}
		else
		{
			if(_perso.getArena() == 1 || _perso.getKolizeum() == 1){
	    		SocketManager.GAME_SEND_MESSAGE(_perso, "Impossible d'habandonner un match en arène !", Config.CONFIG_MOTD_COLOR);
	    		return;
	    	}
			_perso.get_fight().leftFight(_perso, null, false);
		}
	}

	private void Game_on_showCase(String packet)
	{
		if(_perso == null)return;
		if(_perso.get_fight() == null)return;
		if(_perso.get_fight().get_state() != Constant.FIGHT_STATE_ACTIVE)return;
		int cellID = -1;
		try
		{
			cellID = Integer.parseInt(packet.substring(2));
		}catch(Exception e){};
		if(cellID == -1)return;
		_perso.get_fight().showCaseToTeam(_perso.get_GUID(),cellID);
	}

	private void Game_on_Ready(String packet)
	{
		if(_perso.get_fight() == null)return;
		if(_perso.get_fight().get_state() != Constant.FIGHT_STATE_PLACE)return;
		_perso.set_ready(packet.substring(2).equalsIgnoreCase("1"));
		_perso.get_fight().verifIfAllReady();
		SocketManager.GAME_SEND_FIGHT_PLAYER_READY_TO_FIGHT(_perso.get_fight(),3,_perso.get_GUID(),packet.substring(2).equalsIgnoreCase("1"));
	}

	private void Game_on_ChangePlace_packet(String packet)
	{
		if(_perso.get_fight() == null)return;
		try
		{
			int cell = Integer.parseInt(packet.substring(2));
			_perso.get_fight().changePlace( _perso, cell);
		}catch(NumberFormatException e){return;};
	}
	
	private void Game_on_Gdi_packet(String packet)
	{
		int chalID = 0;
		chalID = Integer.parseInt(packet.split("i")[1]);
		if(chalID != 0 && _perso.get_fight() != null) {
			 Fight fight = _perso.get_fight();
			 if(fight.get_challenges().containsKey(chalID))
				 fight.get_challenges().get(chalID).show_cibleToPerso(_perso);
		}
			
	}

	private void Game_on_GK_packet(String packet)
	{	
		int GameActionId = -1;
		String[] infos = packet.substring(3).split("\\|");
		try {
			GameActionId = Integer.parseInt(infos[0]);
		} catch (Exception e) {
			return;
		}
		;
		if (GameActionId == -1) {
			return;
		}
		GameAction GA = _actions.get(GameActionId);
		if (GA == null) {
			return;
		}
		boolean isOk = packet.charAt(2) == 'K';

		switch (GA._actionID) {
		case 1:// Deplacement
			if (isOk) {
				// Hors Combat
				if (_perso.get_fight() == null) {
					_perso.get_curCell().removePlayer(_perso.get_GUID());
					SocketManager.GAME_SEND_BN(_out);
					String path = GA._args;
					// On prend la case ciblï¿½e
					Case nextCell = _perso.get_curCarte()
							.getCase(CryptManager.cellCode_To_ID(path.substring(path.length() - 2)));
					Case targetCell = _perso.get_curCarte()
							.getCase(CryptManager.cellCode_To_ID(GA._packet.substring(GA._packet.length() - 2)));
					if (nextCell == null) {
						nextCell = _perso.get_curCell();
					}
					if (targetCell == null) {
						targetCell = _perso.get_curCell();
					}
					// On dï¿½finie la case et on ajoute le personnage sur la case
					_perso.set_curCell(nextCell);
					_perso.set_orientation(CryptManager.getIntByHashedValue(path.charAt(path.length() - 3)));
					_perso.get_curCell().addPerso(_perso);
					if (!_perso._isGhosts)
						_perso.set_away(false);
					if (targetCell.getObject() != null) {
						String docName;
						// Si c'est une "borne" comme Emotes, ou CrÃ©ation guilde
						if (targetCell.getObject().getID() == 1324) {
							Constant.applyPlotIOAction(_perso, _perso.get_curCarte().get_id(), targetCell.getID());
						}
						// Statues phoenix
						else if (targetCell.getObject().getID() == 542) {
							if (_perso._isGhosts)
								_perso.set_Alive();
						}
						// Pancartes missions de recherches
						else if ((docName = Constant.getDocNameByBornePos(targetCell.getObject().getID(),
								targetCell.getID())) != "") {
							SocketManager.GAME_SEND_CREATE_DOC(_perso, docName);
						}
					}
					_perso.get_curCarte().onPlayerArriveOnCell(_perso, _perso.get_curCell().getID());
				} else {// En combat
					_perso.get_fight().onGK(_perso);
					return;
				}

			} else { // Si le joueur s'arrete sur une case
				int newCellID = -1;
				try {
					newCellID = Integer.parseInt(infos[1]);
				} catch (Exception e) {
					return;
				}
				;
				if (newCellID == -1) {
					return;
				}
				String path = GA._args;
				_perso.get_curCell().removePlayer(_perso.get_GUID());
				_perso.set_curCell(_perso.get_curCarte().getCase(newCellID));
				_perso.set_orientation(CryptManager.getIntByHashedValue(path.charAt(path.length() - 3)));
				_perso.get_curCell().addPerso(_perso);
				SocketManager.GAME_SEND_BN(_out);
			}
			break;

		case 500:// Action Sur Map
			_perso.finishActionOnCell(GA);
		}
		int a = 0;
		if (GA._actionID == 1)
			a = 1;
		removeAction(GA);
		if (wait != 0)
			wait = 0;
		if ((a == 1) && (GA_wait != null))
			parseGameActionPacket(GA_wait._packet);
	}
	
	private void Game_on_GI_packet() {
		Maps map = _perso.get_curCarte();
		int debug = 0;

		try {
			if (_perso.get_fight() != null) {
				// Only percepteur
				SocketManager.GAME_SEND_MAP_GMS_PACKETS(_perso.get_curCarte(), _perso);
				SocketManager.GAME_SEND_GDK_PACKET(_out);
				return;
			}

			Houses.LoadHouse(this._perso, this._perso.get_curCarte().get_id());
			System.out.println(debug++); //0
			SocketManager.GAME_SEND_MAP_NPCS_GMS_PACKETS(this._out, this._perso.get_curCarte());
			System.out.println(debug++);
			SocketManager.GAME_SEND_MAP_MOBS_GMS_PACKETS(this._perso.get_compte().getGameThread().get_out(), this._perso.get_curCarte());
			System.out.println(debug++);
			
			SocketManager.GAME_SEND_MAP_PERCO_GMS_PACKETS(this._out, this._perso.get_curCarte());
			System.out.println(debug++);

			SocketManager.GAME_SEND_MAP_OBJECTS_GDS_PACKETS(this._out, this._perso.get_curCarte());
			System.out.println(debug++);
			
			SocketManager.GAME_SEND_MAP_FIGHT_COUNT(this._out, this._perso.get_curCarte());
			System.out.println(debug++);

			SocketManager.GAME_SEND_MERCHANT_LIST(_perso, _perso.get_curCarte().get_id());
			System.out.println(debug++);
			
			SocketManager.SEND_GM_PRISME_TO_MAP(this._out, map);
			System.out.println(debug++);

			this._perso.get_curCarte().sendFloorItems(this._perso);
			System.out.println(debug++);
			
			// Envoie des persos
			SocketManager.GAME_SEND_MAP_GMS_PACKETS(this._perso.get_curCarte(), this._perso);
			System.out.println(debug++);
			
			// Envoie des MountsPark
			SocketManager.GAME_SEND_Rp_PACKET(this._perso, this._perso.get_curCarte().getMountPark());
			System.out.println(debug++);
			
			Fight.FightStateAddFlag(this._perso.get_curCarte(), this._perso);
			System.out.println(debug++);
			
			// Envoie des OnMapLoaded
			SocketManager.GAME_SEND_GDK_PACKET(this._out);
			System.out.println(debug++); // 12
		} catch (Exception e) {
			SocketManager.send(this._perso, "cC+i");
			System.out.println("Erreur GI numéro: " + debug);
		}
	}

	private void parseGameActionPacket(String packet)
	{
		int actionID;
		try
		{
		actionID = Integer.parseInt(packet.substring(2, 5));
		}
		catch (NumberFormatException e)
		{
		return;
		}
		if (this._perso.get_gfxID() == this._perso.get_classe() * 10 + 3) return;
		int nextGameActionID = 0;
		if (this._actions.size() > 0)
		{
		nextGameActionID = ((Integer)this._actions.keySet().toArray()[(this._actions.size() - 1)]).intValue() + 1;
		}
		GameAction GA = new GameAction(nextGameActionID, actionID, packet);
		int cellID = -1;
		@SuppressWarnings("unused")
		int action = -1;
		if (actionID == 500)
		{
		String packets = GA._packet.substring(5);
		try
		{
		cellID = Integer.parseInt(packets.split(";")[0]);
		action = Integer.parseInt(packets.split(";")[1]); } catch (Exception localException) {
		}
		}
		if ((actionID == 500) && (this.GA_wait == null) && (this.wait == 1) && (!Pathfinding.isNextTo(this._perso.get_curCell().getID(), cellID)))
		{
		GA_wait = GA;
		return;
	    }
		GA_wait = null;
		if (actionID == 1) {
		wait = 1;
		}
		
		switch(actionID)
		{
			case 1://Deplacement
				game_parseDeplacementPacket(GA);
			break;
			
			case 300://Sort
				game_tryCastSpell(packet);
			break;
			
			case 303://Attaque CaC
				game_tryCac(packet);
			break;
			
			case 500://Action Sur Map
				if(_perso.get_curCarte().getSubArea().get_subscribe() && _perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
				{
					SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
					return;
				}
				game_action(GA);
			break;
			
			case 512: //Prismes
				if(_perso.get_curCarte().getSubArea().get_subscribe() && _perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
				{
					SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
					return;
				}
				if (_perso.get_align() == Constant.ALIGNEMENT_NEUTRE) 
					return;
				_perso.openPrismeMenu();
			break;
			
			case 507://Panneau intérieur de la maison
				if(_perso.get_curCarte().getSubArea().get_subscribe() && _perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
				{
					SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
					return;
				}
				house_action(packet);
			break;
			
			case 618://Mariage oui
				if(_perso.get_curCarte().getSubArea().get_subscribe() && _perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
				{
					SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
					return;
				}
				_perso.setisOK(Integer.parseInt(packet.substring(5,6)));
				SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(_perso.get_curCarte(), "", _perso.get_GUID(), _perso.get_name(), "Oui");
				if(World.getMarried(0).getisOK() > 0 && World.getMarried(1).getisOK() > 0)
				{
					World.Wedding(World.getMarried(0), World.getMarried(1), 1);
				}
				if(World.getMarried(0) != null && World.getMarried(1) != null)
				{
					World.PriestRequest((World.getMarried(0)==_perso?World.getMarried(1):World.getMarried(0)), (World.getMarried(0)==_perso?World.getMarried(1).get_curCarte():World.getMarried(0).get_curCarte()), _perso.get_isTalkingWith());
				}
			break;
			case 619://Mariage non
				if(_perso.get_curCarte().getSubArea().get_subscribe() && _perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
				{
					SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
					return;
				}
				_perso.setisOK(0);
				SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(_perso.get_curCarte(), "", _perso.get_GUID(), _perso.get_name(), "Non");
				World.Wedding(World.getMarried(0), World.getMarried(1), 0);
			break;
			
			case 900://Demande Defie
				game_ask_duel(packet);
			break;
			case 901://Accepter Defie
				game_accept_duel(packet);
			break;
			case 902://Refus/Anuler Defie
				game_cancel_duel(packet);
			break;
			case 903://Rejoindre combat
				game_join_fight(packet);
			break;
			case 906://Agresser
				game_aggro(packet);
			break;
			case 909://Perco
				game_perco(packet);
			break;
			case 912:// Attaquer prisme
				if(_perso.get_curCarte().getSubArea().get_subscribe() && _perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
				{
					SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
					return;
				}
				if (_perso.get_align() == Constant.ALIGNEMENT_NEUTRE) 
					return;
				Game_attaque_prisme(packet);
			break;
		}	
	}
	private void Game_attaque_prisme(String packet) {
		try {
			if (_perso == null)
				return;
			if (_perso.get_fight() != null)
				return;
			if (_perso.get_isTalkingWith() != 0 || _perso.get_isTradingWith() != 0 || _perso.getCurJobAction() != null) {
				return;
			}
			if (_perso.get_align() == Constant.ALIGNEMENT_NEUTRE) 
				return;
			int id = Integer.parseInt(packet.substring(5));
			Prism Prisme = World.getPrisme(id);
			if ( (Prisme.getInFight() == 0 || Prisme.getInFight() == -2))
				return;
			SocketManager.SEND_GA_Action_ALL_MAPS(_perso.get_curCarte(), "", 909, _perso.get_GUID() + "", id + "");
			_perso.get_curCarte().startFightVSPrisme(_perso, Prisme);
			
		} catch (Exception e) {}
	}
	private void house_action(String packet)
	{
		int actionID = Integer.parseInt(packet.substring(5));
		Houses h = _perso.getInHouse();
		if(h == null) return;
		switch(actionID)
		{
			case 81://Vérouiller maison
				h.Lock(_perso);
			break;
			case 97://Acheter maison
				h.BuyIt(_perso);
			break;
			case 98://Vendre
			case 108://Modifier prix de vente
				h.SellIt(_perso);
			break;
		}
	}
	
	
	private void game_perco(String packet)
	{
		try
		{
			if(_perso == null)return;
			if(_perso.get_fight() != null)return;
			if(_perso.get_isTalkingWith() != 0 ||
			   _perso.get_isTradingWith() != 0 ||
			   _perso.getCurJobAction() != null ||
			   _perso.get_curExchange() != null ||
			   _perso.is_away())
					{
						return;
					}
			if(_perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
			{
				SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
				return;
			}
			int id = Integer.parseInt(packet.substring(5));
			Collector target = World.getPerco(id);
			if(target == null || target.get_inFight() > 0) return;
			if(target.get_Exchange())
			{
				Characters p = World.getPersonnage(target.getExchangeWith());
				if(p != null && p.isOnline() && p.get_isOnPercepteurID() == id)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "1180");
					return;
				}
				else
				{
					target.set_Exchange(false);
					target.setExchangeWith(-1);
				}
			}
			SocketManager.GAME_SEND_GA_PACKET_TO_MAP(_perso.get_curCarte(),"", 909, _perso.get_GUID()+"", id+"");
			_perso.get_curCarte().startFigthVersusPercepteur(_perso, target);
		}catch(Exception e){};
	}
	
	private synchronized void game_aggro(String packet)
	{
		try
		{
			int id = Integer.parseInt(packet.substring(5));
			Characters target = World.getPersonnage(id);
			if(_perso == null)return;
			if(_perso.get_fight() != null || target.get_fight() != null)return; //Return / Skryn
			if(target.get_curCarte().getSubArea().get_subscribe() && target.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE) {
				SocketManager.GAME_SEND_Im_PACKET(_perso, "122;" + target.get_name());
				return;
			}
			if(target == null || !target.isOnline() || target.get_fight() != null
			|| target.get_curCarte().get_id() != _perso.get_curCarte().get_id()
			|| target.get_align() == _perso.get_align()
			|| _perso.get_curCarte().get_placesStr().equalsIgnoreCase("|")
			|| !target.canAggro())
				return;
			
			if(_perso.get_curCarte().getSubArea().get_subscribe() && _perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
			{
				SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
				return;
			}
			
			if(World.isRestrictedMap(_perso.get_curCarte().get_id()))
			{
				Traque traque = Stalk.getTraqueByOwner(_perso);
				if(traque == null || traque.getTarget() != target.get_GUID())
				{
					SocketManager.GAME_SEND_MESSAGE(_perso, "Vous ne pouvez pas agresser d'autre joueurs sur cette map.", "A00000");
					return;
				}
			}
			if(target.get_align() == 0) {
				
				_perso.setDeshonor(_perso.getDeshonor()+1);
				SocketManager.GAME_SEND_Im_PACKET(_perso, "084;1");
				_perso.toggleWings('+');
				_perso.get_curCarte().newFight(_perso, target, Constant.FIGHT_TYPE_AGRESSION, true);
			} else 
				_perso.get_curCarte().newFight(_perso, target, Constant.FIGHT_TYPE_AGRESSION, false);
			
		}catch(Exception e){};
	}

	private void game_action(GameAction GA)
	{
		String packet = GA._packet.substring(5);
		int cellID = -1;
		int actionID = -1;

		try {
			cellID = Integer.parseInt(packet.split(";")[0]);
			actionID = Integer.parseInt(packet.split(";")[1]);
		} catch (Exception e) {
		}
		// Si packet invalide, ou cellule introuvable
		if (cellID == -1 || actionID == -1 || _perso == null || _perso.get_curCarte() == null
				|| _perso.get_curCarte().getCase(cellID) == null)
			return;
		GA._args = cellID + ";" + actionID;
		_perso.get_compte().getGameThread().addAction(GA);
		_perso.startActionOnCell(GA);
	}

	private synchronized void game_tryCac(String packet)
	{
		try
		{
			if(_perso.get_fight() ==null)return;
			int cellID = -1;
			try
			{
				cellID = Integer.parseInt(packet.substring(5));
			}catch(Exception e){return;};
			
			_perso.get_fight().tryCaC(_perso,cellID);
		}catch(Exception e){};
	}

	private synchronized void game_tryCastSpell(String packet)
	{
		try
		{
			String[] splt = packet.split(";");
			int spellID = Integer.parseInt(splt[0].substring(5));
			int caseID = Integer.parseInt(splt[1]);
			if(_perso.get_fight() != null)
			{
				SortStats SS = _perso.getSortStatBySortIfHas(spellID);
				if(SS == null)return;
				_perso.get_fight().tryCastSpell(_perso.get_fight().getFighterByPerso(_perso),SS,caseID);
			}
		}catch(NumberFormatException e){return;};
	}

	private void game_join_fight(String packet)
	{
		if (_perso.get_fight() != null)
			return;
		String[] infos = packet.substring(5).split(";");
		if (infos.length == 1) {
			try {
				Fight F = _perso.get_curCarte().getFight(Integer.parseInt(infos[0]));
				F.joinAsSpect(_perso);

			} catch (Exception e) {
				return;
			}
			;
		} else {
			try {
				int guid = Integer.parseInt(infos[1]);
				if (_perso.is_away()) {
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(_out, 'o', guid);
					return;
				}
				if (World.getPersonnage(guid) == null)
					return;
				if ((World.getPersonnage(guid).get_fight().get_type() == Constant.FIGHT_TYPE_AGRESSION
						|| World.getPersonnage(guid).get_fight().get_type() == Constant.FIGHT_TYPE_CONQUETE
						|| World.getPersonnage(guid).get_fight().get_type() == Constant.FIGHT_TYPE_PVT)
						&& _perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE) {
					SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(_perso, "+5");
					return;
				}
				World.getPersonnage(guid).get_fight().joinFight(_perso, guid);

			} catch (Exception e) {
				return;
			}
			;
		}
	}

	private void game_accept_duel(String packet)
	{
		int guid = -1;
		try{guid = Integer.parseInt(packet.substring(5));}catch(NumberFormatException e){return;};
		if(_perso.get_duelID() != guid || _perso.get_duelID() == -1)return;
		SocketManager.GAME_SEND_MAP_START_DUEL_TO_MAP(_perso.get_curCarte(),_perso.get_duelID(),_perso.get_GUID());
		
		Fight fight = _perso.get_curCarte().newFight(World.getPersonnage(_perso.get_duelID()),_perso,Constant.FIGHT_TYPE_CHALLENGE,false);
		_perso.set_fight(fight);
		World.getPersonnage(_perso.get_duelID()).set_fight(fight);
		
	}

	private void game_cancel_duel(String packet)
	{
		try
		{
			if(_perso.get_duelID() == -1)return;
			SocketManager.GAME_SEND_CANCEL_DUEL_TO_MAP(_perso.get_curCarte(),_perso.get_duelID(),_perso.get_GUID());
			World.getPersonnage(_perso.get_duelID()).set_away(false);
			World.getPersonnage(_perso.get_duelID()).set_duelID(-1);
			_perso.set_away(false);
			_perso.set_duelID(-1);	
		}catch(NumberFormatException e){return;};
	}

	private void game_ask_duel(String packet)
	{
		if(_perso.get_curCarte().get_placesStr().equalsIgnoreCase("|"))
		{
			SocketManager.GAME_SEND_DUEL_Y_AWAY(_out, _perso.get_GUID());
			return;
		}
		try
		{
			int guid = Integer.parseInt(packet.substring(5));
			if(_perso.is_away() || _perso.get_fight() != null){SocketManager.GAME_SEND_DUEL_Y_AWAY(_out, _perso.get_GUID());return;}
			Characters Target = World.getPersonnage(guid);
			if(Target == null) return;
			if(Target.is_away() || Target.get_fight() != null || Target.get_curCarte().get_id() != _perso.get_curCarte().get_id()){SocketManager.GAME_SEND_DUEL_E_AWAY(_out, _perso.get_GUID());return;}
			
			_perso.set_duelID(guid);
			_perso.set_away(true);
			World.getPersonnage(guid).set_duelID(_perso.get_GUID());
			World.getPersonnage(guid).set_away(true);
			SocketManager.GAME_SEND_MAP_NEW_DUEL_TO_MAP(_perso.get_curCarte(),_perso.get_GUID(),guid);
		}catch(NumberFormatException e){return;}
	}

	private synchronized void game_parseDeplacementPacket(GameAction GA)
	{
		String path = GA._packet.substring(5);
		if(_perso.get_fight() == null)
		{
			if(_perso.getPodUsed() > _perso.getMaxPod())
			{
				SocketManager.GAME_SEND_Im_PACKET(_perso, "112");
				SocketManager.GAME_SEND_GA_PACKET(_out, "", "0", "", "");
				removeAction(GA);
				return;
			}
			AtomicReference<String> pathRef = new AtomicReference<String>(path);
			int result = Pathfinding.isValidPath(_perso.get_curCarte(),_perso.get_curCell().getID(),pathRef, null);
			
			//Si déplacement inutile
			if(result == 0)
			{
				SocketManager.GAME_SEND_GA_PACKET(_out, "", "0", "", "");
				removeAction(GA);
				return;
			}
			if(result != -1000 && result < 0)result = -result;
			
			//On prend en compte le nouveau path
			path = pathRef.get();
			//Si le path est invalide
			if(result == -1000)
			{
				Logs.addToGameLog(_perso.get_name()+"("+_perso.get_GUID()+") Tentative de  deplacement avec un path invalide");
				path = CryptManager.getHashedValueByInt(_perso.get_orientation())+CryptManager.cellID_To_Code(_perso.get_curCell().getID());	
			}
			//On sauvegarde le path dans la variable
			GA._args = path;
			
			SocketManager.GAME_SEND_GA_PACKET_TO_MAP(_perso.get_curCarte(), ""+GA._id, 1, _perso.get_GUID()+"", "a"+CryptManager.cellID_To_Code(_perso.get_curCell().getID())+path);
			addAction(GA);
			if(_perso.isSitted())_perso.setSitted(false);
			_perso.set_away(true);
		}else
		{
			Fighter F = _perso.get_fight().getFighterByPerso(_perso);
			if(F == null)return;
			GA._args = path;
			_perso.get_fight().fighterDeplace(F,GA);
		}
	}

	public GameSendThread get_out() {
		return _out;
	}
	
	public void kick()
	{
		try
		{
			try{
				if (_perso != null){
					if (_perso.getArena() != -1)
						 Arena.delTeam(Team.getTeamByID(_perso.getTeamID()));
					
					if (_perso.getKolizeum() != -1)
						Kolizeum.delPlayer(_perso);
				}
			} catch (Exception e) {
				Logs.addToGameLog("Une erreur vient de se produire lors de la déconnexion du compteID : " + _compte.get_GUID());
			}
			Main.gameServer.delClient(this);
			IpCheck.delGameConnection(_s.getInetAddress().getHostAddress());
			IpCheck.delRealmConnection(_s.getInetAddress().getHostAddress());
			synchronized(_compte)
			{
	    		if(_compte != null)
	    			_compte.deconnexion(_compte);
			}
    		if(_s != null)
    			if(!_s.isClosed()) 
    				_s.close();
    		
    		if(_in != null) 
    			_in.close();
    		if(_out != null)
    			_out.close();
    		Logs.addToGameLog("Le compteID " + _compte.get_GUID() +" a ete kick");
		}catch(IOException e1){
			e1.printStackTrace();	
			Logs.addToGameLog("Une erreur vient de se produire lors de la déconnexion du compteID : " + _compte.get_GUID());
		}
	}

	private void parseAccountPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'A':
				Accounts.createCharacter(packet,  this);
			break;
			
			case 'B':
				int stat = -1;
				if (Config.ALLOW_DISANKALIKE_STATS) {
					try {
						if (packet.substring(2).contains(";")) {
							stat = Integer.parseInt(packet.substring(2).split(";")[0]);
							if (stat > 0) {
								int code = 0;
								code = Integer.parseInt(packet.substring(2).split(";")[1]);
								if (code < 0)
									return;
								if (_perso.get_capital() < code) {
									code = _perso.get_capital();
								}
								_perso.boostStatFixedCount(stat, code);
							}
						} else {
							stat = Integer.parseInt(packet.substring(2).split("/u000A")[0]);
							_perso.set_savestat(stat);
							SocketManager.GAME_SEND_KODE(_perso, "CK0|5");
						}
					} catch (Exception e) {
						return;
					}
				} else {
					try {
						stat = Integer.parseInt(packet.substring(2).split("/u000A")[0]);
						_perso.boostStat(stat);
					} catch (NumberFormatException e) {
						return;
					}
					;
					break;
				}
				break;
			case 'g'://Cadeaux à la connexion
				int gift  = _compte.getCadeau();
				if (gift  != 0) {
					String idModObjeto = Integer.toString(gift, 16);
					String effects = World.getObjTemplate(gift).getStrTemplate();
					SocketManager.GAME_SEND_Ag_PACKET(_out, gift, "1~" + idModObjeto + "~1~~" + effects);
				}
				break;
			case 'G':
				giveGiftToAccount(packet.substring(2));
				break;
			case 'D': //Delete perso
				String[] split = packet.substring(2).split("\\|");
				int GUID = Integer.parseInt(split[0]);
				String reponse = split.length>1?split[1]:"";
				
				if(_compte.get_persos().containsKey(GUID))
				{
					if(_compte.get_persos().get(GUID).get_lvl() <20 ||
							(_compte.get_persos().get(GUID).get_lvl() >=20 && reponse.equals(_compte.get_reponse())))
					{
						_compte.deletePerso(GUID);
						SocketManager.GAME_SEND_PERSO_LIST(_out, _compte.get_persos(), _compte.get_subscriber());;
					}
					else
						SocketManager.GAME_SEND_DELETE_PERSO_FAILED(_out);
				}else
					SocketManager.GAME_SEND_DELETE_PERSO_FAILED(_out);
			break;
			
			case 'f':
				int queueID = 1;
				int position = 1;
				SocketManager.MULTI_SEND_Af_PACKET(_out,position,1,1,""+1,queueID);
			break;
			
			case 'i':
				_compte.setClientKey(packet.substring(2));
			break;
			
			case 'L':
				Map<Integer, Characters> persos = _compte.get_persos();
				for(Characters p : persos.values())
				{
					if(p.get_fight() != null && p.get_fight().getFighterByPerso(p) != null)
					{
						_compte.setGameThread(this);
						_perso = p;
						if(_perso != null)
						{
							_perso.OnJoinGame();
							return;
						}
					}
				}
				
				SocketManager.GAME_SEND_PERSO_LIST(_out, persos, _compte.get_subscriber());
				//SocketManager.GAME_SEND_HIDE_GENERATE_NAME(_out);
			break;
			
			case 'S':
				int charID = Integer.parseInt(packet.substring(2));
				if(_compte.get_persos().get(charID) != null)
				{
					_compte.setGameThread(this);
					_perso = _compte.get_persos().get(charID);
					if(_perso != null)
					{
						_perso.OnJoinGame();
						return;
					}
				}
				SocketManager.GAME_SEND_PERSO_SELECTION_FAILED(_out);
			break;
				
			case 'T':
				int guid = Integer.parseInt(packet.substring(2));
				_compte = Main.gameServer.getWaitingCompte(guid);
				if(_compte != null)
				{
					String ip = _s.getInetAddress().getHostAddress();
					Boolean isHostingorVPN = false;
					try {
						isHostingorVPN = new VPNDetection().getResponse(ip).hostip;
					} catch (Exception ex) {
						Logs.addToGameLog(ex.getMessage());
					}
					if (!isHostingorVPN){
						_compte.setGameThread(this);
						_compte.setCurIP(ip);
						Main.gameServer.delWaitingCompte(_compte);
						SocketManager.GAME_SEND_ATTRIBUTE_SUCCESS(_out);
					} else {
						SocketManager.send(_out, "AlEw");
						try {
							_s.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}else
				{
					SocketManager.GAME_SEND_ATTRIBUTE_FAILED(_out);
				}
			break;
			
			case 'V':
				SocketManager.GAME_SEND_AV0(_out);
			break;
			
			case 'P':
				SocketManager.send(_out, "AP" + RandomCharacterName.get());
				break;
		}
	}
	
	private void giveGiftToAccount(String packet) {
		String[] info = packet.split("\\|");
		int idObject = Integer.parseInt(info[0]);
		int idPlayer = Integer.parseInt(info[1]);
		Characters player = null;
		Objects object = null;
		try {
			player = World.getPersonnage(idPlayer);
			object = World.getObjTemplate(idObject).createNewItem(1, true, -1);
		} catch (Exception e) {}
		if (player == null || object == null) {
			return;
		}
		player.addObjet(object, false);
		World.addObjet(object, true);
		_compte.setCadeau();
		SQLManager.UPDATE_GIFT(_compte);
		SocketManager.GAME_SEND_AGK_PACKET(_out);
	}

	public Thread getThread()
	{
		return _t;
	}

	public void removeAction(GameAction GA)
	{
		//* DEBUG
		//System.out.println("Supression de la GameAction id = "+GA._id);
		//*/
		_actions.remove(GA._id);
	}
	
	public void addAction(GameAction GA)
	{
		_actions.put(GA._id, GA);
		//* DEBUG
		//System.out.println("Ajout de la GameAction id = "+GA._id);
		//System.out.println("Packet: "+GA._packet);
		//*/
	}
	
	private void Object_obvijevan_changeApparence(String packet)
	{
		int guid = -1;
		int pos = -1;
		int val = -1;
		try
		{
			guid = Integer.parseInt(packet.substring(2).split("\\|")[0]);
			pos = Integer.parseInt(packet.split("\\|")[1]);
			val = Integer.parseInt(packet.split("\\|")[2]); } catch (Exception e) {
				return;
			}if ((guid == -1) || (!_perso.hasItemGuid(guid))) return;
			Objects obj = World.getObjet(guid);
			if ((val >= 21) || (val <= 0)) return;
			
			obj.obvijevanChangeStat(972, val);
			SocketManager.send(_perso, obj.obvijevanOCO_Packet(pos));
			if (pos != -1) SocketManager.GAME_SEND_ON_EQUIP_ITEM(_perso.get_curCarte(), _perso);
	}
	private void Object_obvijevan_feed(String packet)
	{
		int guid = -1;
		int pos = -1;
		int victime = -1;
		try
		{
			guid = Integer.parseInt(packet.substring(2).split("\\|")[0]);
			pos = Integer.parseInt(packet.split("\\|")[1]);
			victime = Integer.parseInt(packet.split("\\|")[2]);
		} catch (Exception e) {return;}
		
		if ((guid == -1) || (!_perso.hasItemGuid(guid)))
			return;
		Objects obj = World.getObjet(guid);
		Objects objVictime = World.getObjet(victime);
		obj.obvijevanNourir(objVictime);
		
		int qua = objVictime.getQuantity();
		if (qua <= 1)
		{
			_perso.removeItem(objVictime.getGuid());
			SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, objVictime.getGuid());
		} else {
			objVictime.setQuantity(qua - 1);
			SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, objVictime);
		}
		SocketManager.send(_perso, obj.obvijevanOCO_Packet(pos));
	}
	
	private void Object_obvijevan_desassocier(String packet)
	{
		int guid = -1;
		int pos = -1;
		try
		{
			guid = Integer.parseInt(packet.substring(2).split("\\|")[0]);
			pos = Integer.parseInt(packet.split("\\|")[1]); } catch (Exception e) {
				return;
			}if ((guid == -1) || (!_perso.hasItemGuid(guid))) return;
			Objects obj = World.getObjet(guid);
			int idOBVI = 0;
			
			if (obj.getObvijevanPos() != 0) { //On vérifie si il y a bien un obvi
				idOBVI = obj.getObviID();
			} else {
				SocketManager.GAME_SEND_MESSAGE(_perso, "Erreur lors de la dissociation ! Contactez l'administrateur..", "000000");
			}
			
			Objects.ObjTemplate t = World.getObjTemplate(idOBVI);
			Objects obV = t.createNewItem(1, true, -1);
			String obviStats = obj.getObvijevanStatsOnly();
			if (obviStats == "") {
				SocketManager.GAME_SEND_MESSAGE(_perso, "Erreur lors de la dissociation ! Contactez l'administrateur...", "000000");
				return;
			}
			obV.clearStats();
			obV.parseStringToStats(obviStats);
			if (_perso.addObjet(obV, true)) {
				World.addObjet(obV, true);
			}
			obj.removeAllObvijevanStats();
			SocketManager.send(_perso, obj.obvijevanOCO_Packet(pos));
			SocketManager.GAME_SEND_ON_EQUIP_ITEM(_perso.get_curCarte(), _perso);
	}
	
	public Accounts getAccount() {
		return this._compte;
	}
	
	public GameSendThread getOut() {
		return _out;
	}
}
	
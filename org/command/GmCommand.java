package org.command;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.util.Map.Entry;

import javax.swing.Timer;

import org.client.Accounts;
import org.client.Characters;
import org.command.GmCommandManager;
import org.common.Constant;
import org.common.CryptManager;
import org.common.Formulas;
import org.common.Pathfinding;
import org.common.SQLManager;
import org.common.SocketManager;
import org.common.World;
import org.common.World.ItemSet;
import org.fight.Fight;
import org.fight.extending.Team;
import org.fight.object.Monster.MobGroup;
import org.game.GameSendThread;
import org.game.GameThread;
import org.game.GameServer.SaveThread;
import org.game.tools.Utils;
import org.kernel.Config;
import org.kernel.Logs;
import org.kernel.Main;
import org.kernel.Reboot;
import org.object.Action;
import org.object.Maps;
import org.object.NpcTemplates;
import org.object.Objects;
import org.object.AuctionHouse.HdvEntry;
import org.object.Maps.MountPark;
import org.object.NpcTemplates.NPC;
import org.object.NpcTemplates.NPC_question;
import org.object.NpcTemplates.NPC_reponse;
import org.object.Objects.ObjTemplate;
import org.object.job.Job.StatsMetier;
import org.utils.Colors;

public class GmCommand {
	Accounts _compte;
	Characters _perso;
	GameSendThread _out;
	// Sauvegarde
	private boolean _TimerStart = false;
	Timer _timer;

	private Timer createTimer(final int time) {
		ActionListener action = new ActionListener() {
			int Time = time;

			public void actionPerformed(ActionEvent event) {
				Time = Time - 1;
				if (Time == 1) {
					SocketManager.GAME_SEND_Im_PACKET_TO_ALL("115;" + Time + " minute");
				} else {
					SocketManager.GAME_SEND_Im_PACKET_TO_ALL("115;" + Time + " minutes");
				}
				if (Time <= 0) {
					for (Characters perso : World.getOnlinePersos()) {
						perso.get_compte().getGameThread().kick();
					}
					Reboot.start();
				}
			}
		};
		// Génération du repeat toutes les minutes.
		return new Timer(60000, action);// 60000
	}

	public GmCommand(Characters perso) {
		this._compte = perso.get_compte();
		this._perso = perso;

		this._out = _compte.getGameThread().get_out();
	}

	public void consoleCommand(String packet) {

		if (_compte.get_gmLvl() < 1) {
			_compte.getGameThread().closeSocket();
			return;
		}

		String msg = packet.substring(2);
		String[] infos = msg.split(" ");
		if (infos.length == 0)
			return;
		
		GmCommandManager command = GmCommandManager.getCommandByName(infos[0]);
		
		Logs.addToMjLog("[" + _compte.get_curIP() + "] : " + _compte.get_name() + " / " + _perso.get_name() + " => " + msg);
		if (command != null)
			launchNewGmCommand(command, infos, msg);
		else
			SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Commande non reconnu");
	}

	private void launchNewGmCommand(GmCommandManager command, String[] infos, String msg) {
		if (command.getGMlevel() > _compte.get_gmLvl()) {
			SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Vous devez avoir le GM de niveau " + command.getGMlevel() + " pour utiliser cette commande.");
			return;
		}

		switch (command.getFunction()) {
		case -1: // debug
			Characters perso = _perso;
			try {
				perso = World.getPersoByName(infos[1]);
			} catch (Exception e) {
				getMan(command);
				break;
			}
			if (perso == null) {
				String str = "Le personnage n'a pas ete trouve";
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, str);
				break;
			}

			SocketManager.GAME_SEND_GV_PACKET(perso);
			perso.set_duelID(-1);
			perso.set_ready(false);
			perso.fullPDV();
			try {
				perso.get_fight().leftFight(perso, null, true);
			} catch (Exception e) {
			}
			perso.set_fight(null);
			SocketManager.GAME_SEND_GV_PACKET(perso);
			SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.get_curCarte(), perso);
			perso.get_curCell().addPerso(perso);
			break;
		case 1: // Liste des commandes
			String cmd = GmCommandManager.getCommandList(_compte.get_gmLvl());
			SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, cmd);
			break;
		case 2: // uptime
			long uptime = System.currentTimeMillis() - Main.gameServer.getStartTime();
			int jour = (int) (uptime / (1000 * 3600 * 24));
			uptime %= (1000 * 3600 * 24);
			int hour = (int) (uptime / (1000 * 3600));
			uptime %= (1000 * 3600);
			int min = (int) (uptime / (1000 * 60));
			uptime %= (1000 * 60);
			int sec = (int) (uptime / (1000));

			String uptimeMsg = "===========\nUptime : " + jour + "j " + hour + "h " + min + "m "
					+ sec + "s\n" + "Connected : " + Main.gameServer.getPlayerNumber() + "\n" + "Max Connected : "
					+ Main.gameServer.getMaxPlayer() + "\n" + "===========";
			SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, uptimeMsg);
			break;
		case 3: // Refreshmobs
			_perso.get_curCarte().refreshSpawns();
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Mob Spawn refreshed!");
			break;
		case 4: //Annonce
			try {
				infos = msg.split(" ", 2);
				if (infos.length < 2) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "ERREUR : Message trop court");
					break;
				}
				SocketManager.GAME_SEND_Im_PACKET_TO_ALL("116;<b>Serveur</b>~" + infos[1]);
			} catch (Exception localException8) {
				getMan(command);
			}
			break;
		case 5: //NameAnnounce
			try {
				infos = msg.split(" ", 2);
				if (infos.length < 2) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "ERREUR : Message trop court");
					break;
				}
				String clicker_name = "<a href='asfunction:onHref,ShowPlayerPopupMenu," + _perso.get_name() + "'>"
						+ _perso.get_name() + "</a>";
				SocketManager.GAME_SEND_Im_PACKET_TO_ALL("116;<b>" + clicker_name + "</b>~" + infos[1]);
			} catch (Exception localException8) {
				getMan(command);
			}
			break;
		case 7: // Antiflood
			if (infos[1] == null) {
				getMan(command);
				break;
			}
			Characters target = World.getPersoByName(infos[1]);
			if (target == null) {
				String str = "Le personnage n'existe pas";
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, str);
				break;
			}
			if (!target.get_compte().isAFlooder()) {
				target.get_compte().setAFlooder(true);
				SocketManager.GAME_SEND_MESSAGE_TO_ALL("Le joueur <b>" + target.get_name()
						+ "</b> est désormais soumis à l'antiflood du serveur par le modérateur " + _perso.get_name()
						+ " !", Colors.RED);
			} else {
				target.get_compte().setAFlooder(false);
				target.get_compte().setFloodGrade(0);
				SocketManager
						.GAME_SEND_MESSAGE_TO_ALL(
								"Le modérateur <b>" + _perso.get_name()
										+ "</b> a désactivé l'antiflood actif sur le joueur " + target.get_name() + " !",
								Colors.RED);
			}
			break;
		case 8: //mapinfo
			String mapInfos = "==========\n" + "Liste des Npcs de la carte:";
			SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, mapInfos);
			Maps map = _perso.get_curCarte();
			for (Entry<Integer, NPC> entry : map.get_npcs().entrySet()) {
				mapInfos = entry.getKey() + " " + entry.getValue().get_template().get_id() + " "
						+ entry.getValue().get_cellID() + " " + entry.getValue().get_template().get_initQuestionID();
				SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, mapInfos);
			}
			mapInfos = "Liste des groupes de monstres:";
			SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, mapInfos);
			for (Entry<Integer, MobGroup> entry : map.getMobGroups().entrySet()) {
				mapInfos = entry.getKey() + " " + entry.getValue().getCellID() + " " + entry.getValue().getAlignement()
						+ " " + entry.getValue().getSize();
				SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, mapInfos);
			}
			mapInfos = "==========";
			SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, mapInfos);
			break;
		case 9: // WHO
			String mess = "==========\n" + "Liste des joueurs en ligne:";
			SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, mess);
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
				mess += P.get_fight() == null ? "" : "Combat ";
				SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, mess);
			}
			if (diff > 0) {
				mess = "Et " + diff + " autres personnages";
				SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, mess);
			}
			mess = "==========\n";
			SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, mess);
			break;
		case 10: // demorph
			Characters demorph = _perso;
			if (infos.length > 1)// Si un nom de perso est spécifié
			{
				demorph = World.getPersoByName(infos[1]);
				if (demorph == null) {
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, str);
					break;
				}
			}
			int morphID = demorph.get_classe() * 10 + demorph.get_sexe();
			demorph.set_gfxID(morphID);
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(demorph.get_curCarte(), demorph.get_GUID());
			SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(demorph.get_curCarte(), demorph);
			String str = "Le joueur a ete transforme";
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, str);
			break;
		case 11: //goname
			try {
				Characters P = World.getPersoByName(infos[1]);
				if (P == null) {
					String str1 = "Le personnage n'existe pas";
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, str1);
					break;
				}
				short mapID = P.get_curCarte().get_id();
				int cellID = P.get_curCell().getID();
	
				Characters target1 = _perso;
				if (infos.length > 2)// Si un nom de perso est spécifié
				{
					target1 = World.getPersoByName(infos[2]);
					if (target1 == null) {
						String str1 = "Le personnage n'a pas ete trouve";
						SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, str1);
						break;
					}
					if (target1.get_fight() != null) {
						String str1 = "La cible est en combat";
						SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, str1);
						break;
					}
				}
				target1.teleport(mapID, cellID);
				String str1 = "Le joueur a ete teleporte";
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, str1);
			} catch (Exception e) {
				getMan(command);
			}
			break;
		case 12: // namego
			try {
				Characters target11 = World.getPersoByName(infos[1]);
				if (target11 == null) {
					String str2 = "Le personnage n'existe pas";
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, str2);
					break;
				}
				if (target11.get_fight() != null) {
					String str2 = "La cible est en combat";
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, str2);
					break;
				}
				Characters P1 = _perso;
				if (infos.length > 2)// Si un nom de perso est spécifié
				{
					P1 = World.getPersoByName(infos[2]);
					if (P1 == null) {
						SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'a pas ete trouve");
						break;
					}
					if (P1.get_fight() != null) {
						SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personange est en combat");
						break;
					}
				}
				if (P1.isOnline()) {
					short mapID1 = P1.get_curCarte().get_id();
					int cellID1 = P1.get_curCell().getID();
					target11.teleport(mapID1, cellID1);
					String str2 = "Le joueur a ete teleporte";
					SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, str2);
				} else {
					String str2 = "Le joueur n'est pas en ligne";
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, str2);
				}
			} catch (Exception e) {
				getMan(command);
			}
			break;
		case 13 :// TP
			short mapID2 = -1;
			int cellID2 = -1;
			try {
				mapID2 = Short.parseShort(infos[1]);
				cellID2 = Integer.parseInt(infos[2]);
			} catch (Exception e) {
				getMan(command);
				break;
			}
			;
			if (mapID2 == -1 || cellID2 == -1 || World.getCarte(mapID2) == null) {
				String str3 = "MapID ou cellID invalide";
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, str3);
				break;
			}
			if (World.getCarte(mapID2).getCase(cellID2) == null) {
				String str3 = "MapID ou cellID invalide";
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, str3);
				break;
			}
			Characters target3 = _perso;
			if (infos.length > 3)// Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[3]);
				if (target == null || target.get_fight() != null) {
					String str3 = "Le personnage n'a pas ete trouve ou est en combat";
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, str3);
					break;
				}
			}
			target3.teleport(mapID2, cellID2);
			String str3 = "Le joueur a ete teleporte";
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, str3);
			break;
		case 14: //MP
			infos = msg.split(" ", 3);
			if (infos.length > 1) {

				Characters Pl = World.getPersoByName(infos[1]);
				if ((Pl == null) || (Pl.get_name() == this._perso.get_name()) || (!Pl.isOnline())) {
					String msg1 = "Erreur : Impossible de parler à ce personnage";
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(this._out, msg1);
					break;
				}
				if (infos.length > 3) {
					getMan(command);
					break;
				}

				String prefix1 = "<i>de</i> [<b><a href='asfunction:onHref,ShowPlayerPopupMenu,"
						+ this._perso.get_name() + "'>" + this._perso.get_name() + "</a></b>] : ";
				String prefix2 = "Message a \"" + Pl.get_name() + "\" : ";
				SocketManager.GAME_SEND_MESSAGE(Pl, prefix1 + infos[2], Colors.RED);
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(this._out, prefix2 + infos[2]);
			}
			break;
		case 15: //FIGHTPOS
			String mess5 = "Liste des StartCell [teamID][cellID]:";
			SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, mess5);
			String places = _perso.get_curCarte().get_placesStr();
			if (places.indexOf('|') == -1 || places.length() < 2) {
				mess5 = "Les places n'ont pas ete definies";
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, mess5);
				break;
			}
			String team0 = "", team1 = "";
			String[] p = places.split("\\|");
			try {
				team0 = p[0];
			} catch (Exception e) {
			}
			;
			try {
				team1 = p[1];
			} catch (Exception e) {
			}
			;
			mess5 = "Team 0:\n";
			for (int a = 0; a <= team0.length() - 2; a += 2) {
				String code = team0.substring(a, a + 2);
				mess5 += CryptManager.cellCode_To_ID(code);
			}
			SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, mess5);
			mess5 = "Team 1:\n";
			for (int a = 0; a <= team1.length() - 2; a += 2) {
				String code = team1.substring(a, a + 2);
				mess5 += CryptManager.cellCode_To_ID(code) + " , ";
			}
			SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, mess5);
			break;
		case 16: //Creer une guilde
			Characters gPerso = _perso;
			if (infos.length > 1) {
				gPerso = World.getPersoByName(infos[1]);
			}
			if (gPerso == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'existe pas.");
				break;
			}

			if (!gPerso.isOnline()) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage " + gPerso.get_name() + " n'etait pas connecte");
				break;
			}
			if (gPerso.get_guild() != null || gPerso.getGuildMember() != null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage " + gPerso.get_name() + " a deja une guilde");
				break;
			}
			if(!gPerso.hasItemTemplate(1575, 1)) {
				ObjTemplate guildalo = World.getObjTemplate(1575);
				Objects t_objectGive = guildalo.createNewItem(1, false, -1); 
				if (gPerso.addObjet(t_objectGive, true))// On regarde si il n'a pas déjà un item
					World.addObjet(t_objectGive, true); // On envoi l'item
			}
			SocketManager.GAME_SEND_gn_PACKET(gPerso);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, gPerso.get_name() + ": Panneau de creation de guilde ouvert");
			break;
		case 17: //activer/desactiver les aggros
			Characters persoTP = _perso;

			String name = null;
			try {
				name = infos[1];
			} catch (Exception e) {
			}
			;

			persoTP = World.getPersoByName(name);

			if (persoTP == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'existe pas.");
				break;
			}

			persoTP.set_canAggro(!persoTP.canAggro());
			String info = persoTP.get_name();
			if (persoTP.canAggro())
				info += " peut maintenant etre aggresser";
			else
				info += " ne peut plus etre agresser";
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, info);

			if (!persoTP.isOnline()) {
				info = "(Le personnage " + persoTP.get_name() + " n'etait pas connecte)";
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, info);
			}
			break;
		case 18: //TP avec pos X/Y
			int mapX = 0;
			int mapY = 0;
			int cell = 311;
			int contID = 0;// Par défaut Amakna
			try {
				mapX = Integer.parseInt(infos[1]);
				mapY = Integer.parseInt(infos[2]);
				cell = Integer.parseInt(infos[3]);
				contID = Integer.parseInt(infos[4]);
			} catch (Exception e) {
				getMan(command);
				break;
			}
			;
			Maps destination = World.getCarteByPosAndCont(mapX, mapY, contID);
			if (destination == null) {
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Position ou continent invalide");
				break;
			}
			if (destination.getCase(cell) == null) {
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "CellID invalide");
				break;
			}
			Characters persoT = _perso;
			if (infos.length > 5)// Si un nom de perso est spécifié
			{
				persoT = World.getPersoByName(infos[5]);
				if (persoT == null || persoT.get_fight() != null) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'a pas ete trouve ou est en combat");
					break;
				}
				if (persoT.get_fight() != null) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "La cible est en combat");
					break;
				}
			}
			persoT.teleport(destination.get_id(), cell);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Le joueur a ete teleporte");
			break;
		case 19: //Envoyer un popup à un joueur
			if (infos.length < 3) {
				getMan(command);
				break;
			} else {
				Characters recever = World.getPersoByName(infos[1]);
				if (recever == null) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Personnage inconnu.");
				} else {
					String Message = msg.split(" ", 3)[2];
					SocketManager.GAME_SEND_POPUP(recever, Message);
					SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Vous venez d'envoyer un popup à " + infos[1]);
				}
			}
			break;
		case 20: //Envoyer un popup à tlm
			if (infos.length < 2) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Erreur de syntaxe : "+command.getName() + " " + command.getArgs());
			} else {
	            String Message = msg.split(" ", 2)[1];
	            SocketManager.GAME_SEND_POPUP_TO_All(Message);
	            SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Vous venez d'envoyer un popup à tous les joueurs connecté.");
	        }
			break;
		case 21: //Muter une map
			if (infos.length < 2) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Vous devez préciser un temps en secondes");
				break;
			}
			long time = Long.parseLong(infos[1]);
			Maps mutedMap = _perso.get_curCarte();
			mutedMap.muteMap(time);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Vous avez effectué un mute sur la map actuelle.");
			break;
		case 22: //demute une map
			Maps unemuteMap = _perso.get_curCarte();
			if (unemuteMap.isMuted()) {
				unemuteMap.unMuteMap();
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "La map a été démutée");
			} else {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "La map n'est pas mutée");
			}
			break;
		case 23: //envoyer un message sur la carte
			if (infos.length >= 2) {
				String message = msg.split(" ", 2)[1];
				message = BeautifullMessage(message);
				SocketManager.GAME_SEND_Im_PACKET_TO_MAP(_perso.get_curCarte(),
						new StringBuilder("1243;").append(_perso.get_name()).append("~").append(message).toString());
				for (Fight f : _perso.get_curCarte().get_fights2().values()) {
					SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(f, 7, new StringBuilder("1243;")
							.append(_perso.get_name()).append("~").append(message).toString());
				}
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Message envoyé.");
			} else
				getMan(command);
			break;
		case 24: //muter un joueur
			Characters mutePerso = _perso;
			String nameMute = null;
			int timeMute = 0;
			try {
				nameMute = infos[1];
				timeMute = Integer.parseInt(infos[2]);
			} catch (Exception e) {
				getMan(command);
				break;
			}

			mutePerso = World.getPersoByName(nameMute);
			if (mutePerso == null || mutePerso.get_compte() == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'existe pas ou est invalide.");
				break;
			}
			mutePerso.get_compte().mute(mutePerso.get_name(), timeMute);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out,
					"Vous avez mute " + mutePerso.get_name() + " pour " + timeMute + " minutes");

			if (mutePerso.isOnline())
				SocketManager.GAME_SEND_Im_PACKET(mutePerso, "117;" + _perso.get_name() + "~" + timeMute);
			else
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out,
						"(Le personnage " + mutePerso.get_name() + " n'est pas connecte)");
			break;
		case 25: //demuter un joueur
			Characters persoUnemute = _perso;

			String nameUnmute = null;
			try {
				nameUnmute = infos[1];
			} catch (Exception e) {
			}
			;

			persoUnemute = World.getPersoByName(nameUnmute);
			if (persoUnemute == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'existe pas.");
				break;
			}

			persoUnemute.get_compte().unMute();
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Vous avez unmute " + persoUnemute.get_name());

			if (!persoUnemute.isOnline()) {
				mess = "(Le personnage " + persoUnemute.get_name() + " n'etait pas connecte)";
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, mess);
			}
			break;
		case 26: //kick
			Characters kickPerso = _perso;
			String kickName = null;
			String message = "";
			try {
				kickName = infos[1];
				if (infos.length >= 3)
					message = msg.split(" ", 3)[2];
			} catch (Exception e) {
			}

			kickPerso = World.getPersoByName(kickName);
			if (kickPerso == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'existe pas.");
				break;
			}
			if (kickPerso.get_compte() != null) {
				if (kickPerso.get_compte().get_gmLvl() > _perso.get_compte().get_gmLvl()) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Impossible sur un GM supérieur à vous");
					break;
				}
			}
			if (kickPerso.isOnline()) {
				kickPerso.get_compte().getGameThread().kick();
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Vous avez kick " + kickPerso.get_name());
				SocketManager.GAME_SEND_Im_PACKET_TO_ALL(
						"1240;" + kickPerso.get_name() + "~" + _perso.get_name() + "~" + message);
			} else {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage " + kickPerso.get_name() + " n'est pas connecte");
			}
			break;
		case 27: //Mettre un align
			byte align = -1;
			try {
				align = Byte.parseByte(infos[1]);
			} catch (Exception e) {
				getMan(command);
				break;
			}
			;
			if (align < Constant.ALIGNEMENT_NEUTRE || align > Constant.ALIGNEMENT_MERCENAIRE) {
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Valeur invalide");
				break;
			}
			Characters alignPerso = _perso;
			if (infos.length > 2)// Si un nom de perso est spécifié
			{
				alignPerso = World.getPersoByName(infos[2]);
				if (alignPerso == null) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'a pas ete trouve");
					break;
				}
			}

			alignPerso.modifAlignement(align);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "L'alignement du joueur a ete modifie");
			break;
		case 28: // Mettre une reponse à un NPC
			if (infos.length < 3) {
				getMan(command);
				break;
			}
			int id = 0;
			try {
				id = Integer.parseInt(infos[1]);
			} catch (Exception e) {
			}
			;
			String reps = infos[2];
			NPC_question Q = World.getNPCQuestion(id, _perso);
			String msg1 = "";
			if (id == 0 || Q == null) {
				msg1 = "QuestionID invalide";
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, msg1);
				break;
			}
			Q.setReponses(reps);
			boolean a = SQLManager.UPDATE_NPCREPONSES(id, reps);
			msg1 = "Liste des reponses pour la question " + id + ": " + Q.getReponses();
			if (a)
				msg1 += "(sauvegarde dans la BDD)";
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, msg1);
			break;
		case 29: //Voir reponse d'un NPC
			int npcID = 0;
			try {
				npcID = Integer.parseInt(infos[1]);
			} catch (Exception e) {
			}
			;
			NPC_question question = World.getNPCQuestion(npcID, _perso);
			if (npcID == 0 || question == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "QuestionID invalide");
				break;
			}
			SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, "Liste des reponses pour la question " + npcID + ": " + question.getReponses() );
			break;
		case 30: //Ajouter de l'xp à un métier 
			int job = -1;
			int xp = -1;
			try {
				job = Integer.parseInt(infos[1]);
				xp = Integer.parseInt(infos[2]);
			} catch (Exception e) {
			}
			;
			if (job == -1 || xp < 0) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Valeurs invalides");
				break;
			}
			Characters targetJob = _perso;
			if (infos.length > 3)// Si un nom de perso est spécifié
			{
				targetJob = World.getPersoByName(infos[3]);
				if (targetJob == null) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'a pas ete trouve");
					break;
				}
			}
			StatsMetier SM = targetJob.getMetierByID(job);
			if (SM == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le joueur ne connais pas le metier demande");
				break;
			}

			SM.addXp(targetJob, xp);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Le metier a ete experimenter");
			break;
		case 31: //Apprendre un metier
			int jobToLearn = -1;
			try {
				jobToLearn = Integer.parseInt(infos[1]);
			} catch (Exception e) {
			}
			;
			if (jobToLearn == -1 || World.getMetier(jobToLearn) == null) {
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Valeur invalide");
				break;
			}
			Characters targetLearnJob = _perso;
			if (infos.length > 2)// Si un nom de perso est spécifié
			{
				targetLearnJob = World.getPersoByName(infos[2]);
				if (targetLearnJob == null) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'a pas ete trouve");
					break;
				}
			}

			targetLearnJob.learnJob(World.getMetier(jobToLearn));

			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Le metier a ete appris");
			break;
		case 32: //Changer de taille
			int size = -1;
			try {
				size = Integer.parseInt(infos[1]);
			} catch (Exception e) {
				getMan(command);
				break;
			}
			if (size < 0 || size > 400) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Taille invalide");
				break;
			}
			Characters targetSize = _perso;
			if (infos.length > 2)// Si un nom de perso est spécifié
			{
				targetSize = World.getPersoByName(infos[2]);
				if (targetSize == null) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'a pas ete trouve");
					break;
				}
			}
			targetSize.set_size(size);
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(targetSize.get_curCarte(), targetSize.get_GUID());
			SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(targetSize.get_curCarte(), targetSize);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "La taille du joueur a ete modifiee");
			break;
		case 33: //Morph
			int newMorph = -1;
			try {
				newMorph = Integer.parseInt(infos[1]);
			} catch (Exception e) {
				getMan(command);
				break;
			}
			Characters targetMorph = _perso;
			if (infos.length > 2)// Si un nom de perso est spécifié
			{
				targetMorph = World.getPersoByName(infos[2]);
				if (targetMorph == null) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'a pas ete trouve");
					break;
				}
			}
			targetMorph.set_gfxID(newMorph);
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(targetMorph.get_curCarte(), targetMorph.get_GUID());
			SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(targetMorph.get_curCarte(), targetMorph);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "L'apparence du joueur a ete modifiee");
			break;
		case 34: //Bouger un PNJ
			int pnjID = 0;
			try {
				pnjID = Integer.parseInt(infos[1]);
			} catch (Exception e) {
			}
			;
			NPC npc = _perso.get_curCarte().getNPC(pnjID);
			if (pnjID == 0 || npc == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Npc GUID invalide");
				break;
			}
			int exC = npc.get_cellID();
			// on l'efface de la map
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.get_curCarte(), pnjID);
			// on change sa position/orientation
			npc.setCellID(_perso.get_curCell().getID());
			npc.setOrientation((byte) _perso.get_orientation());
			// on envoie la modif
			SocketManager.GAME_SEND_ADD_NPC_TO_MAP(_perso.get_curCarte(), npc);
			String infoNPC = "Le PNJ a ete deplace";
			if (_perso.get_orientation() == 0 || _perso.get_orientation() == 2 || _perso.get_orientation() == 4
					|| _perso.get_orientation() == 6)
				infoNPC += " mais est devenu invisible (orientation diagonale invalide).";
			if (SQLManager.DELETE_NPC_ON_MAP(_perso.get_curCarte().get_id(), exC)
					&& SQLManager.ADD_NPC_ON_MAP(_perso.get_curCarte().get_id(), npc.get_template().get_id(),
							_perso.get_curCell().getID(), _perso.get_orientation()))
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, infoNPC);
			else
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Erreur au moment de sauvegarder la position");
			break;
		case 35: //BAN
			int nb_heures = -1;
			Characters bannedP = null;
			try {
				bannedP = World.getPersoByName(infos[1]);
				nb_heures = Integer.parseInt(infos[2]);
			} catch (Exception e) {
			}
			if (nb_heures < 0) {
				getMan(command);
				SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, "Un temps de 0 équivaut à un ban définitif.");
				break;
			}
			if (bannedP == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'existe pas.");
				break;
			}
			if (bannedP.get_compte() == null)
				SQLManager.LOAD_ACCOUNT_BY_GUID(bannedP.getAccID());
			if (bannedP.get_compte() == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le compte du personnage n'existe pas ou plus.");
				break;
			}
			if (bannedP.get_compte().get_gmLvl() >= _perso.get_compte().get_gmLvl()) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Impossible sur un GM supérieur ou égal à vous");
				break;
			}
			// On peut le bannir pour de bon
			if (nb_heures > 0)
				bannedP.get_compte().ban(nb_heures * 3600, false);
			else
				bannedP.get_compte().ban(-1, false);
			if (bannedP.get_fight() == null) {
				if (bannedP.get_compte().getGameThread() != null)
					SocketManager.REALM_SEND_MESSAGE(bannedP.get_compte().getGameThread().get_out(),
							"18|" + _perso.get_name() + ";<br />Ton compte a été banni.");
				bannedP.get_compte().getGameThread().kick();
			} else {
				SocketManager.GAME_SEND_Im_PACKET(bannedP, "1201;" + _perso.get_name());
				SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out,
						"Le joueur " + bannedP.get_name() + " est en combat. Il sera kick après celui-ci.");
			}
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Vous avez banni " + bannedP.get_name());
			break;
		case 36: //Fixer les pdv à un certain %age
			int count = 0;
			try {
				count = Integer.parseInt(infos[1]);
				if (count < 0)
					count = 0;
				if (count > 100)
					count = 100;
				Characters PDVperso = _perso;
				if (infos.length == 3)// Si le nom du perso est spécifié
				{
					String PDVname = infos[2];
					PDVperso = World.getPersoByName(PDVname);
					if (PDVperso == null)
						PDVperso = _perso;
				}
				int newPDV = PDVperso.get_PDVMAX() * count / 100;
				PDVperso.set_PDV(newPDV);
				if (PDVperso.isOnline())
					SocketManager.GAME_SEND_STATS_PACKET(PDVperso);
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Vous avez fixer le pourcentage de pdv de " + PDVperso.get_name() + " a " + count);
			} catch (Exception e) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Valeur incorecte");
				break;
			}
			break;
		case 37: //Ajouter des kamas
			int kamas = 0;
			try {
				kamas = Integer.parseInt(infos[1]);
			} catch (Exception e) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Valeur incorecte");
				break;
			}
			;
			if (kamas == 0)
				break;

			Characters persoK = _perso;
			if (infos.length == 3)// Si le nom du perso est spécifié
			{
				String nameK = infos[2];
				persoK = World.getPersoByName(nameK);
				if (persoK == null)
					persoK = _perso;
			}
			long curKamas = persoK.get_kamas();
			long newKamas = curKamas + kamas;
			if (newKamas < 0)
				newKamas = 0;
			if (newKamas > 1000000000)
				newKamas = 1000000000;
			persoK.set_kamas(newKamas);
			if (persoK.isOnline())
				SocketManager.GAME_SEND_STATS_PACKET(persoK);
			String kamasMess = "Vous avez ";
			kamasMess += (kamas < 0 ? "retirer" : "ajouter") + " ";
			kamasMess += Math.abs(kamas) + " kamas a " + persoK.get_name();
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, kamasMess);
			break;
		case 38: //Spawn des monstres
			String Mob = null;
			try {
				Mob = infos[1];
			} catch (Exception e) {
			}
			;
			if (Mob == null)
				break;
			_perso.get_curCarte().spawnGroupOnCommand(_perso.get_curCell().getID(), Mob, true);
			break;
		case 39: //Vider les enclos public
			System.out.println("Suppression des dragodindes de l'enclos sur la MapID .enclos.");
			SQLManager.RESET_MOUNTPARKS();
			SQLManager.LOAD_MOUNTPARKS();
			System.out.println("Suppression des dragodindes de l'enclos sur la MapID .enclos : OK !");
			SocketManager.GAME_SEND_Im_PACKET_TO_ALL(
					"116;<b>Serveur</b>~L'enclos publique en <b>.enclos</b> vient d'être vidé.");
			break;
		case 40: //Reboot
			Reboot.start();
			break;
		case 41: //Mettre un titre
			Characters persoTitle = null;
			int TitleID = 0;
			try {
				persoTitle = World.getPersoByName(infos[1]);
				TitleID = Integer.parseInt(infos[2]);
			} catch (Exception e) {
			}
			;

			if (persoTitle == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'a pas ete trouve");
				break;
			}

			persoTitle.set_title(TitleID);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Titre mis en place.");
			SQLManager.SAVE_PERSONNAGE(persoTitle, false);
			if (persoTitle.get_fight() == null)
				SocketManager.GAME_SEND_ALTER_GM_PACKET(persoTitle.get_curCarte(), persoTitle);
			break;
		case 42: // Spawn des étoiles
			//TODO
			break;
		case 43: // Sauvegarder le serv
			if (!Config.isSaving) {
				Thread t = new Thread(Main.THREAD_SAVE, new SaveThread(), "SaveCommand");
				t.start();
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Sauvegarde lancee!");
			} else 
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Une sauvegarde est déjà en cours !");
			break;
		case 44: // Avoir les coordonnées
			int cordCell = _perso.get_curCell().getID();
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "[" + Pathfinding.getCellXCoord(_perso.get_curCarte(), cordCell) + ","
					+ Pathfinding.getCellYCoord(_perso.get_curCarte(), cordCell) + "]");
			break;
		case 45: // Supprimer une case de combat
			int delFightCell = -1;
			try {
				delFightCell = Integer.parseInt(infos[2]);
			} catch (Exception e) {
			}
			;
			if (delFightCell < 0 || _perso.get_curCarte().getCase(delFightCell) == null) {
				delFightCell = _perso.get_curCell().getID();
			}
			String delPlace = _perso.get_curCarte().get_placesStr();
			String[] delPlac = delPlace.split("\\|");
			String newPlaces = "";
			String delTeam0 = "", delTeam1 = "";
			try {
				delTeam0 = delPlac[0];
			} catch (Exception e) {
			}
			;
			try {
				delTeam1 = delPlac[1];
			} catch (Exception e) {
			}
			;

			for (int i = 0; i <= delTeam0.length() - 2; i += 2) {
				String c = delPlac[0].substring(i, i + 2);
				if (delFightCell == CryptManager.cellCode_To_ID(c))
					continue;
				newPlaces += c;
			}
			newPlaces += "|";
			for (int j = 0; j <= delTeam1.length() - 2; j += 2) {
				String c = delPlac[1].substring(j, j + 2);
				if (delFightCell == CryptManager.cellCode_To_ID(c))
					continue;
				newPlaces += c;
			}
			_perso.get_curCarte().setPlaces(newPlaces);
			if (!SQLManager.SAVE_MAP_DATA(_perso.get_curCarte()))
				break;
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Les places ont ete modifiees (" + newPlaces + ")");
			break;
		case 46: // Ajouter des cellules de combats
			int team = -1;
			int addCell = -1;
			try {
				team = Integer.parseInt(infos[1]);
				addCell = Integer.parseInt(infos[2]);
			} catch (Exception e) {
			}
			;
			if (team < 0 || team > 1) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Team ou cellID incorects");
				break;
			}
			if (addCell < 0 || _perso.get_curCarte().getCase(addCell) == null
					|| !_perso.get_curCarte().getCase(addCell).isWalkable(true)) {
				addCell = _perso.get_curCell().getID();
			}
			String addPlace = _perso.get_curCarte().get_placesStr();
			String[] addPlaces = addPlace.split("\\|");
			boolean already = false;
			String addTeam0 = "", addTeam1 = "";
			try {
				addTeam0 = addPlaces[0];
			} catch (Exception e) {
			}
			;
			try {
				addTeam1 = addPlaces[1];
			} catch (Exception e) {
			}
			;

			// Si case déjà utilisée
			System.out.println("0 => " + addTeam0 + "\n1 =>" + addTeam1 + "\nCell: " + CryptManager.cellID_To_Code(addCell));
			for (int y = 0; y <= addTeam0.length() - 2; y += 2)
				if (addCell == CryptManager.cellCode_To_ID(addTeam0.substring(y, y + 2)))
					already = true;
			for (int z = 0; z <= addTeam1.length() - 2; z += 2)
				if (addCell == CryptManager.cellCode_To_ID(addTeam1.substring(z, z + 2)))
					already = true;
			if (already) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "La case est deja dans la liste");
				break;
			}
			if (team == 0)
				addTeam0 += CryptManager.cellID_To_Code(addCell);
			else if (team == 1)
				addTeam1 += CryptManager.cellID_To_Code(addCell);

			String newAddPlaces = addTeam0 + "|" + addTeam1;

			_perso.get_curCarte().setPlaces(newAddPlaces);
			if (!SQLManager.SAVE_MAP_DATA(_perso.get_curCarte()))
				break;
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Les places ont ete modifiees (" + newAddPlaces + ")");
			break;
		case 47: // debannir 
			Characters unbanPlayer = World.getPersoByName(infos[1]);
			String unbanMsg = "";
			if (infos.length >= 3)
				unbanMsg = msg.split(" ", 3)[2];
			if (unbanPlayer == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Personnage non trouve");
				break;
			}
			if (unbanPlayer.get_compte() == null)
				SQLManager.LOAD_ACCOUNT_BY_GUID(unbanPlayer.getAccID());
			if (unbanPlayer.get_compte() == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Erreur");
				break;
			}
			unbanPlayer.get_compte().unBan();
			StringBuilder im_mess = new StringBuilder("1244;").append(unbanPlayer.get_name()).append("~")
					.append(_perso.get_name()).append("~").append(unbanMsg);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Vous avez debanni " + unbanPlayer.get_name());
			SocketManager.GAME_SEND_Im_PACKET_TO_ALL(im_mess.toString());
			break;
		case 48: //Mettre un groupe max
			infos = msg.split(" ", 4);
			byte maxID = -1;
			try {
				maxID = Byte.parseByte(infos[1]);
			} catch (Exception e) {
			}
			;
			if (maxID == -1) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Valeur invalide");
				break;
			}
			String addMess = "Le nombre de groupe a ete fixe";
			_perso.get_curCarte().setMaxGroup(maxID);
			boolean ok = SQLManager.SAVE_MAP_DATA(_perso.get_curCarte());
			if (ok)
				addMess += " et a ete sauvegarder a la BDD";
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, addMess);
			break;
		case 49: // Ajouter une réponses_action
			infos = msg.split(" ", 4);
			int npccID = -30;
			int repID = 0;
			String args = infos[3];
			try {
				repID = Integer.parseInt(infos[1]);
				npccID = Integer.parseInt(infos[2]);
			} catch (Exception e) {
			}
			;
			NPC_reponse rep = World.getNPCreponse(repID);
			if (npccID == -30 || rep == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Au moins une des valeur est invalide");
				break;
			}
			String resMess = "L'action a ete ajoute";

			rep.addAction(new Action(npccID, args, ""));
			boolean isGood = SQLManager.ADD_REPONSEACTION(repID, npccID, args);
			if (isGood)
				resMess += " et ajoute a la BDD";
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, resMess);
			break;
		case 50: //Mettre une question à un pnj
			infos = msg.split(" ", 4);
			int id1 = -30;
			int q = 0;
			try {
				q = Integer.parseInt(infos[1]);
				id1 = Integer.parseInt(infos[2]);
			} catch (Exception e) {
			}
			;
			if (id1 == -30) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "NpcID invalide");
				break;
			}
			String messQ = "L'action a ete ajoute";
			NpcTemplates npcT = World.getNPCTemplate(id1);

			npcT.setInitQuestion(q);
			boolean okette = SQLManager.UPDATE_INITQUESTION(id1, q);
			if (okette)
				messQ += " et ajoute a la BDD";
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, messQ);
			break;
		case 51: //Ajouter une endfight 
			infos = msg.split(" ", 4);
			int idEnd = -30;
			int type = 0;
			String argsEnd = infos[3];
			String cond = infos[4];
			try {
				type = Integer.parseInt(infos[1]);
				idEnd = Integer.parseInt(infos[2]);

			} catch (Exception e) {
			}
			;
			if (idEnd == -30) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Au moins une des valeur est invalide");
				break;
			}
			String messEnd = "L'action a ete ajoute";
			_perso.get_curCarte().addEndFightAction(type, new Action(idEnd, argsEnd, cond));
			boolean oki = SQLManager.ADD_ENDFIGHTACTION(_perso.get_curCarte().get_id(), type, idEnd, argsEnd, cond);
			if (oki)
				messEnd += " et ajoute a la BDD";
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, messEnd);
			break;
		case 52: //SpawnFix
			String groupData = infos[1];
			_perso.get_curCarte().addStaticGroup(_perso.get_curCell().getID(), groupData);
			String spawnStr = "Le grouppe a ete fixe";
			// Sauvegarde DB de la modif
			if (SQLManager.SAVE_NEW_FIXGROUP(_perso.get_curCarte().get_id(), _perso.get_curCell().getID(), groupData))
				spawnStr += " et a ete sauvegarde dans la BDD";
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, spawnStr);
			break;
		case 85: //Delete SpawnFix
			
			if (SQLManager.DELETE_FIXGROUP(_perso.get_curCarte().get_id()))
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Le groupefix a été supprimé avec succès");
			else
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Erreur lors de la suppression");
			break;
		case 53: // Ajouter un npc 
			int IDnpc = 0;
			try {
				IDnpc = Integer.parseInt(infos[1]);
			} catch (Exception e) {
			}
			;
			if (IDnpc == 0 || World.getNPCTemplate(IDnpc) == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "NpcID invalide");
				break;
			}
			NPC newNPC = _perso.get_curCarte().addNpc(IDnpc, _perso.get_curCell().getID(), _perso.get_orientation());
			SocketManager.GAME_SEND_ADD_NPC_TO_MAP(_perso.get_curCarte(), newNPC);
			String addStr = "Le PNJ a ete ajoute";
			if (_perso.get_orientation() == 0 || _perso.get_orientation() == 2 || _perso.get_orientation() == 4
					|| _perso.get_orientation() == 6)
				addStr += " mais est invisible (orientation diagonale invalide).";

			if (SQLManager.ADD_NPC_ON_MAP(_perso.get_curCarte().get_id(), IDnpc, _perso.get_curCell().getID(),
					_perso.get_orientation()))
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, addStr);
			else
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Erreur au moment de sauvegarder la position");
			break;
		case 54: // supprimer un npc
			int delID = 0;
			try {
				delID = Integer.parseInt(infos[1]);
			} catch (Exception e) {
			}
			;
			NPC delNPC = _perso.get_curCarte().getNPC(delID);
			if (delID == 0 || delNPC == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Npc GUID invalide");
				break;
			}
			int curCellDel = delNPC.get_cellID();
			// on l'efface de la map
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.get_curCarte(), delID);
			_perso.get_curCarte().removeNpcOrMobGroup(delID);

			if (SQLManager.DELETE_NPC_ON_MAP(_perso.get_curCarte().get_id(), curCellDel))
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Le PNJ a ete supprime");
			else
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Erreur au moment de sauvegarder la position");
			break;
		case 55: // Voir le stuff d'un joueur
			Characters viewer = null;
			try {
				viewer = World.getPersoByName(infos[1]);
			} catch (Exception e) {
			}
			;
			if (viewer == null || !viewer.isOnline()) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'a pas ete trouve.");
				break;
			}
			// Récupération du stuff
			int nb_ob = 0;
			StringBuilder mess_player = new StringBuilder();
			StringBuilder mess_items = new StringBuilder();
			Objects obj = viewer.getObjetByPos(Constant.ITEM_POS_COIFFE);
			if (obj != null) {
				mess_player.append("°0, ");
				mess_items.append(obj.getTemplate().getID()).append("!").append(obj.parseStatsString());
				nb_ob++;
			} else
				mess_player.append("Pas de coiffe, ");
			obj = viewer.getObjetByPos(Constant.ITEM_POS_CAPE);
			if (obj != null) {
				mess_player.append("°").append(nb_ob).append(", ");
				if (nb_ob != 0)
					mess_items.append("!");
				mess_items.append(obj.getTemplate().getID()).append("!").append(obj.parseStatsString());
				nb_ob++;
			} else
				mess_player.append("Pas de cape, ");
			obj = viewer.getObjetByPos(Constant.ITEM_POS_FAMILIER);
			if (obj != null) {
				mess_player.append("°").append(nb_ob).append(", ");
				if (nb_ob != 0)
					mess_items.append("!");
				mess_items.append(obj.getTemplate().getID()).append("!").append(obj.parseStatsString());
				nb_ob++;
			} else
				mess_player.append("Pas de familier, ");
			obj = viewer.getObjetByPos(Constant.ITEM_POS_AMULETTE);
			if (obj != null) {
				mess_player.append("°").append(nb_ob).append(", ");
				if (nb_ob != 0)
					mess_items.append("!");
				mess_items.append(obj.getTemplate().getID()).append("!").append(obj.parseStatsString());
				nb_ob++;
			} else
				mess_player.append("Pas d'amulette, ");
			obj = viewer.getObjetByPos(Constant.ITEM_POS_CEINTURE);
			if (obj != null) {
				mess_player.append("°").append(nb_ob).append(", ");
				if (nb_ob != 0)
					mess_items.append("!");
				mess_items.append(obj.getTemplate().getID()).append("!").append(obj.parseStatsString());
				nb_ob++;
			} else
				mess_player.append("Pas de ceinture, ");
			obj = viewer.getObjetByPos(Constant.ITEM_POS_BOTTES);
			if (obj != null) {
				mess_player.append("°").append(nb_ob).append(", ");
				if (nb_ob != 0)
					mess_items.append("!");
				mess_items.append(obj.getTemplate().getID()).append("!").append(obj.parseStatsString());
				nb_ob++;
			} else
				mess_player.append("Pas de bottes.");
			SocketManager.GAME_SEND_cMK_PACKET(_perso, "F", viewer.get_GUID(), viewer.get_name(),
					mess_player.append("|").append(mess_items).toString());
			// SUITE DES ITEMS
			nb_ob = 0;
			mess_player = new StringBuilder();
			mess_items = new StringBuilder();
			obj = viewer.getObjetByPos(Constant.ITEM_POS_ANNEAU1);
			if (obj != null) {
				mess_player.append("°0, ");
				mess_items.append(obj.getTemplate().getID()).append("!").append(obj.parseStatsString());
				nb_ob++;
			} else
				mess_player.append("Pas d'anneau 1, ");
			obj = viewer.getObjetByPos(Constant.ITEM_POS_ANNEAU2);
			if (obj != null) {
				mess_player.append("°").append(nb_ob).append(", ");
				if (nb_ob != 0)
					mess_items.append("!");
				mess_items.append(obj.getTemplate().getID()).append("!").append(obj.parseStatsString());
				nb_ob++;
			} else
				mess_player.append("Pas d'anneau 2, ");
			obj = viewer.getObjetByPos(Constant.ITEM_POS_ARME);
			if (obj != null) {
				mess_player.append("°").append(nb_ob).append(", ");
				if (nb_ob != 0)
					mess_items.append("!");
				mess_items.append(obj.getTemplate().getID()).append("!").append(obj.parseStatsString());
				nb_ob++;
			} else
				mess_player.append("Pas d'arme, ");
			obj = viewer.getObjetByPos(Constant.ITEM_POS_DOFUS1);
			if (obj != null) {
				mess_player.append("°").append(nb_ob).append(", ");
				if (nb_ob != 0)
					mess_items.append("!");
				mess_items.append(obj.getTemplate().getID()).append("!").append(obj.parseStatsString());
				nb_ob++;
			} else
				mess_player.append("Pas de dofus 1, ");
			obj = viewer.getObjetByPos(Constant.ITEM_POS_DOFUS2);
			if (obj != null) {
				mess_player.append("°").append(nb_ob).append(", ");
				if (nb_ob != 0)
					mess_items.append("!");
				mess_items.append(obj.getTemplate().getID()).append("!").append(obj.parseStatsString());
				nb_ob++;
			} else
				mess_player.append("Pas de dofus 2, ");
			obj = viewer.getObjetByPos(Constant.ITEM_POS_DOFUS3);
			if (obj != null) {
				mess_player.append("°").append(nb_ob).append(", ");
				if (nb_ob != 0)
					mess_items.append("!");
				mess_items.append(obj.getTemplate().getID()).append("!").append(obj.parseStatsString());
				nb_ob++;
			} else
				mess_player.append("Pas de dofus 3.");
			SocketManager.GAME_SEND_cMK_PACKET(_perso, "F", viewer.get_GUID(), viewer.get_name(),
					mess_player.append("|").append(mess_items).toString());
			// SUITE ET FIN ITEMS
			nb_ob = 0;
			mess_player = new StringBuilder();
			mess_items = new StringBuilder();
			obj = viewer.getObjetByPos(Constant.ITEM_POS_BOUCLIER);
			if (obj != null) {
				mess_player.append("°0, ");
				mess_items.append(obj.getTemplate().getID()).append("!").append(obj.parseStatsString());
				nb_ob++;
			} else
				mess_player.append("Pas de bouclier, ");
			obj = viewer.getObjetByPos(Constant.ITEM_POS_DOFUS4);
			if (obj != null) {
				mess_player.append("°").append(nb_ob).append(", ");
				if (nb_ob != 0)
					mess_items.append("!");
				mess_items.append(obj.getTemplate().getID()).append("!").append(obj.parseStatsString());
				nb_ob++;
			} else
				mess_player.append("Pas de dofus 4, ");
			obj = viewer.getObjetByPos(Constant.ITEM_POS_DOFUS5);
			if (obj != null) {
				mess_player.append("°").append(nb_ob).append(", ");
				if (nb_ob != 0)
					mess_items.append("!");
				mess_items.append(obj.getTemplate().getID()).append("!").append(obj.parseStatsString());
				nb_ob++;
			} else
				mess_player.append("Pas de dofus 5, ");
			obj = viewer.getObjetByPos(Constant.ITEM_POS_DOFUS6);
			if (obj != null) {
				mess_player.append("°").append(nb_ob).append(", ");
				if (nb_ob != 0)
					mess_items.append("!");
				mess_items.append(obj.getTemplate().getID()).append("!").append(obj.parseStatsString());
				nb_ob++;
			} else
				mess_player.append("Pas de dofus 6.");
			SocketManager.GAME_SEND_cMK_PACKET(_perso, "F", viewer.get_GUID(), viewer.get_name(),
					mess_player.append("|").append(mess_items).toString());
			break;
		case 56: // bannip
			int time_heure = -1;
			Characters bannedPlayer = null;
			try {
				bannedPlayer = World.getPersoByName(infos[1]);
				time_heure = Integer.parseInt(infos[2]);
			} catch (Exception e) {
			}
			if (time_heure < 0) {
				getMan(command);
				SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, "Un temps de 0 équivaut à un ban définitif.");
				break;
			}
			if (bannedPlayer == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'existe pas.");
				break;
			}
			if (bannedPlayer.get_compte() == null)
				SQLManager.LOAD_ACCOUNT_BY_GUID(bannedPlayer.getAccID());
			if (bannedPlayer.get_compte() == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le compte du personnage n'existe plus.");
				break;
			}
			if (bannedPlayer.get_compte().get_gmLvl() >= _perso.get_compte().get_gmLvl()) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Impossible sur un GM supérieur ou égal à vous");
				break;
			}
			// On peut le bannir pour de bon
			if (time_heure != 0) {
				World.Banip(bannedPlayer.get_compte(), time_heure);
			} else {
				World.Banip(bannedPlayer.get_compte(), 0);
			}
			if (bannedPlayer.get_compte().getGameThread() != null)
				bannedPlayer.get_compte().getGameThread().kick();
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Vous avez banni l'ip de " + bannedPlayer.get_name());
			break;
		case 57: //Unbann ip
			Characters unban_Player = World.getPersoByName(infos[1]);
			String unbanStr = "";
			if (infos.length >= 3)
				unbanStr = msg.split(" ", 3)[2];
			if (unban_Player == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Personnage non trouve");
				break;
			}
			if (unban_Player.get_compte() == null)
				SQLManager.LOAD_ACCOUNT_BY_GUID(unban_Player.getAccID());
			if (unban_Player.get_compte() == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Erreur");
				break;
			}
			World.unBanip(unban_Player.get_compte());
			StringBuilder unbanIm = new StringBuilder("1246;").append(unban_Player.get_name()).append("~")
					.append(_perso.get_name()).append("~").append(unbanStr);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Vous avez debanni l'ip de " + unban_Player.get_name());
			SocketManager.GAME_SEND_Im_PACKET_TO_ALL(unbanIm.toString());
			break;
		case 58: //del un trigger
			int trigCellID = -1;
			try {
				trigCellID = Integer.parseInt(infos[1]);
			} catch (Exception e) {
			}
			;
			if (trigCellID == -1 || _perso.get_curCarte().getCase(trigCellID) == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "CellID invalide");
				break;
			}

			_perso.get_curCarte().getCase(trigCellID).clearOnCellAction();
			boolean success = SQLManager.REMOVE_TRIGGER(_perso.get_curCarte().get_id(), trigCellID);
			String triggerStr = "";
			if (success)
				triggerStr = "Le trigger a ete retire";
			else
				triggerStr = "Le trigger n'a pas ete retire";
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, triggerStr);
			break;
		case 59: // add un trigger
			int actionID = -1;
			String trigArgs = "", TrigCond = "";
			try {
				actionID = Integer.parseInt(infos[1]);
				trigArgs = infos[2];
				TrigCond = infos[3];
			} catch (Exception e) {
				getMan(command);
				break;
			}
			if (trigArgs.equals("") || actionID <= -3) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Valeur invalide");
				break;
			}

			_perso.get_curCell().addOnCellStopAction(actionID, trigArgs, TrigCond);
			boolean trigSuccess = SQLManager.SAVE_TRIGGER(_perso.get_curCarte().get_id(), _perso.get_curCell().getID(),
					actionID, 1, trigArgs, TrigCond);
			String TrigStr = "";
			if (trigSuccess)
				TrigStr = "Le trigger a ete ajoute";
			else
				TrigStr = "Le trigger n'a pas ete ajoute";
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, TrigStr);
			break;
		case 60: //Supprimer un item d'un pnj
			if (_compte.get_gmLvl() < 3)
				break;
			int npcGUID = 0;
			int itmID = -1;
			try {
				npcGUID = Integer.parseInt(infos[1]);
				itmID = Integer.parseInt(infos[2]);
			} catch (Exception e) {
			}
			;
			NpcTemplates npcEdit = _perso.get_curCarte().getNPC(npcGUID).get_template();
			if (npcGUID == 0 || itmID == -1 || npcEdit == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "NpcGUID ou itmID invalide");
				break;
			}

			String NPCdtr = "";
			if (npcEdit.delItemVendor(itmID))
				NPCdtr = "L'objet a ete retire";
			else
				NPCdtr = "L'objet n'a pas ete retire";
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, NPCdtr);
			break;
		case 61: //Ajouter un item a un pnj
			if (_compte.get_gmLvl() < 3)
				break;
			int npcModifID = 0;
			int newItemID = -1;
			try {
				npcModifID = Integer.parseInt(infos[1]);
				newItemID = Integer.parseInt(infos[2]);
			} catch (Exception e) {
				getMan(command);
				break;
			}
			;
			NpcTemplates modifNPC = _perso.get_curCarte().getNPC(npcModifID).get_template();
			ObjTemplate item = World.getObjTemplate(newItemID);
			if (npcModifID == 0 || newItemID == -1 || modifNPC == null || item == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "NpcGUID ou itmID invalide");
				break;
			}

			String modifStr
			= "";
			if (modifNPC.addItemVendor(item))
				modifStr = "L'objet a ete rajoute";
			else
				modifStr = "L'objet n'a pas ete rajoute";
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, modifStr);
			break;
		case 62: // Add un enclo
			int parksize = -1;
			int owner = -2;
			int price = -1;
			try {
				parksize = Integer.parseInt(infos[1]);
				owner = Integer.parseInt(infos[2]);
				price = Integer.parseInt(infos[3]);
				if (price > 20000000)
					price = 20000000;
				if (price < 0)
					price = 0;
			} catch (Exception e) {
			}
			;
			if (parksize == -1 || owner == -2 || price == -1 || _perso.get_curCarte().getMountPark() != null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Infos invalides ou map deja config.");
				break;
			}
			MountPark MP = new MountPark(owner, _perso.get_curCarte(), _perso.get_curCell().getID(), parksize, "", -1,
					price);
			_perso.get_curCarte().setMountPark(MP);
			SQLManager.SAVE_MOUNTPARK(MP);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "L'enclos a ete config. avec succes");
			break;
		case 63: //Programmer un reboot
			int shutdownTime = 30, OffOn = 0;
			try {
				OffOn = Integer.parseInt(infos[1]);
				shutdownTime = Integer.parseInt(infos[2]);
			} catch (Exception e) {
			}
			;

			if (OffOn == 1 && _TimerStart)// demande de démarer le reboot
			{
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Un shutdown est deja programmer.");
			} else if (OffOn == 1 && !_TimerStart) {
				_timer = createTimer(shutdownTime);
				_timer.start();
				_TimerStart = true;
				String timeMSG = "minutes";
				if (shutdownTime <= 1) {
					timeMSG = "minute";
				}
				SocketManager.GAME_SEND_Im_PACKET_TO_ALL("115;" + shutdownTime + " " + timeMSG);
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Shutdown lance.");
			} else if (OffOn == 0 && _TimerStart) {
				_timer.stop();
				_TimerStart = false;
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Shutdown arrete.");
			} else if (OffOn == 0 && !_TimerStart) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Aucun shutdown n'est lance.");
			}
			break;
		case 65: //Modifier le gm
			int gmLvl = -100;
			try {
				gmLvl = Integer.parseInt(infos[1]);
			} catch (Exception e) {
			}
			;
			if (gmLvl == -100 || gmLvl < 0) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Valeur incorrecte");
				break;
			}
			Characters GMtarget = _perso;
			if (infos.length > 2)// Si un nom de perso est spécifié
			{
				GMtarget = World.getPersoByName(infos[2]);
				if (GMtarget == null) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'a pas ete trouve");
					break;
				}
			}
			GMtarget.get_compte().setGmLvl(gmLvl);
			SQLManager.UPDATE_ACCOUNT_DATA(GMtarget.get_compte());
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Le niveau GM du joueur a ete modifie");
			if(gmLvl > 0)
				SocketManager.GAME_SEND_BAIO_PACKET(GMtarget, _perso.get_name());
			else
				SocketManager.GAME_SEND_BAIC_PACKET(GMtarget, _perso.get_name());
			
			break;
		case 66: //Faire une action (Action.java)
			// DOACTION NAME TYPE ARGS COND
			if (infos.length < 4) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Nombre d'argument de la commande incorect !");
				break;
			}
			int actionType = -100;
			String actionArgs = "", actionCond = "";
			Characters actionPerso = _perso;
			try {
				actionPerso = World.getPersoByName(infos[1]);
				if (actionPerso == null)
					actionPerso = _perso;
				actionType = Integer.parseInt(infos[2]);
				actionArgs = infos[3];
				if (infos.length > 4)
					actionCond = infos[4];
			} catch (Exception e) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Arguments de la commande incorect !");
				break;
			}
			(new Action(actionType, actionArgs, actionCond)).apply(actionPerso, null, -1, -1);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Action effectuee !");
			break;
		case 67: //Modifier le level
			int newLvl = 0;
			try {
				newLvl = Integer.parseInt(infos[1]);
				if (newLvl < 1)
					newLvl = 1;
				if (newLvl > World.getExpLevelSize())
					newLvl = World.getExpLevelSize();
				Characters levelPerso = _perso;
				if (infos.length == 3)// Si le nom du perso est spécifié
				{
					String levelName = infos[2];
					levelPerso = World.getPersoByName(levelName);
					if (levelPerso == null)
						levelPerso = _perso;
				}
				if (levelPerso.get_lvl() < newLvl) {
					while (levelPerso.get_lvl() < newLvl) {
						levelPerso.levelUp(false, true);
					}
					if (levelPerso.isOnline()) {
						SocketManager.GAME_SEND_SPELL_LIST(levelPerso);
						SocketManager.GAME_SEND_NEW_LVL_PACKET(levelPerso.get_compte().getGameThread().get_out(),
								levelPerso.get_lvl());
						SocketManager.GAME_SEND_STATS_PACKET(levelPerso);
					}
				}
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Vous avez fixer le niveau de " + levelPerso.get_name() + " a " + newLvl);
			} catch (Exception e) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Valeur incorecte");
				break;
			}
			break;
		case 68: // Ajouter du capital
			int pts = -1;
			try {
				pts = Integer.parseInt(infos[1]);
			} catch (Exception e) {
			}
			;
			if (pts == -1 || pts < 0) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Valeur invalide");
				break;
			}
			Characters capitalPerso = _perso;
			if (infos.length > 2)// Si un nom de perso est spécifié
			{
				capitalPerso = World.getPersoByName(infos[2]);
				if (capitalPerso == null) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'a pas ete trouve");
					break;
				}
			}
			capitalPerso.addCapital(pts);
			SocketManager.GAME_SEND_STATS_PACKET(capitalPerso);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Le capital a ete modifiee");
			break;
		case 69: // Donner une pano
			int tID = 0;
			String nom = null;
			try {
				if (infos.length > 3)
					nom = infos[3];
				else if (infos.length > 1)
					tID = Integer.parseInt(infos[1]);

			} catch (Exception e) {
			}
			;
			ItemSet IS = World.getItemSet(tID);
			if (tID == 0 || IS == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "La panoplie " + tID + " n'existe pas ");
				break;
			}
			boolean useMax = false;
			if (infos.length > 2)
				useMax = infos[2].equals("MAX");// Si un jet est spécifié

			Characters itemsetPerso = _perso;
			if (nom != null)
				try {
					itemsetPerso = World.getPersoByName(nom);
				} catch (Exception e) {
				}
			for (ObjTemplate t : IS.getItemTemplates()) {
				Objects addObj = t.createNewItem(1, useMax, -1);
				if (itemsetPerso != null) {
					if (itemsetPerso.addObjet(addObj, true))// Si le joueur n'avait pas d'item similaire
						World.addObjet(addObj, true);
				} else if (_perso.addObjet(addObj, true))// Si le joueur n'avait pas d'item similaire
					World.addObjet(addObj, true);
			}
			String setStr = "Creation de la panoplie " + tID + " reussie";
			if (useMax)
				setStr += " avec des stats maximums";
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, setStr);
			break;
		case 70: //Donner un item
			int itemID = 0;
			try {
				itemID = Integer.parseInt(infos[1]);
			} catch (Exception e) {
			}
			;
			if (itemID == 0) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le template " + itemID + " n'existe pas ");
				break;
			}
			int qua = 1;
			if (infos.length >= 3)// Si une quantité est spécifiée
			{
				try {
					qua = Integer.parseInt(infos[2]);
				} catch (Exception e) {
				}
				;
			}
			boolean jetMax = false;
			if (infos.length == 4)// Si un jet est spécifié
			{
				jetMax = infos[3].equalsIgnoreCase("MAX");
			}
			ObjTemplate t = World.getObjTemplate(itemID);
			if (t == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le template " + itemID + " n'existe pas ");
				break;
			}
			if (qua < 1)
				qua = 1;
			Objects givedObj = t.createNewItem(qua, jetMax, -1);
			if (_perso.addObjet(givedObj, true))// Si le joueur n'avait pas d'item
											// similaire
				World.addObjet(givedObj, true);
			String givedStr = "Creation de l'item " + itemID + " reussie";
			if (jetMax)
				givedStr += " avec des stats maximums";
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, givedStr);
			SocketManager.GAME_SEND_Ow_PACKET(_perso);
			break;
		case 71: //Lister/Supprimer les thread les threads
			boolean delete = false;
			try {
				delete = infos[1].equalsIgnoreCase("delete");
			} catch (Exception e) {
				getMan(command);
				break;
			}
			try {
				Main.listThreads(delete);
			} catch (Exception e) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Impossible de lister les thread. Erreur : " + e.getMessage());
			}
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Les threads ont été listés");
			break;
		case 72: // Faire un morphItem
			int statsID = 0;
			int morphIDs = 0;
			try {
				statsID = Integer.parseInt(infos[1]);
				morphIDs = Integer.parseInt(infos[2]);
			} catch (Exception e) {
			}
			;
			if (statsID == 0 || morphIDs == 0) {
				SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, "MORPHITEM id_stats id_morph quantite jet");
				break;
			}
			int qtt = 1;
			if (infos.length >= 4)// Si une quantité est spécifiée
			{
				try {
					qtt = Integer.parseInt(infos[3]);
				} catch (Exception e) {
				}
				;
			}
			boolean isMax = false;
			boolean usePM = false;
			boolean usePA = false;
			int i;
			if (infos.length >= 5)// Si un jet est spécifié
			{
				for (i = 4; i < infos.length; i++) {
					if (infos[i].equalsIgnoreCase("MAX"))
						isMax = true;
					if (infos[i].equalsIgnoreCase("PA"))
						usePA = true;
					if (infos[i].equalsIgnoreCase("PM"))
						usePM = true;
				}
			}
			ObjTemplate tstats = World.getObjTemplate(statsID);
			if (tstats == null) {
				String mess1 = "Le template stats " + statsID + " n'existe pas ";
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, mess1);
				break;
			}
			ObjTemplate tmorph = World.getObjTemplate(morphIDs);
			if (tmorph == null) {
				String mess2 = "Le template stats " + morphIDs + " n'existe pas ";
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, mess2);
				break;
			}
			if (tmorph.getType() != tstats.getType()) {
				String mess3 = "Les deux items doivent être de même type.";
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, mess3);
				break;
			}

			if (qtt < 1)
				qtt = 1;
			// tmorph.getStrTemplate()
			int val = -1;
			if (usePA && usePM)
				val = 3;
			else if (usePA)
				val = 1;
			else if (usePM)
				val = 2;

			Objects morphObj = new Objects(World.getNewItemGuid(), tmorph.getID(), qtt, Constant.ITEM_POS_NO_EQUIPED,
					tstats.generateNewStatsFromTemplate(tstats.getStrTemplate(), isMax, val),
					tstats.getEffectTemplate(tstats.getStrTemplate()),
					tstats.getBoostSpellStats(tstats.getStrTemplate()));
			// tmorph.createNewItem(qua,useMax,-1);
			if (_perso.addObjet(morphObj, true))// Si le joueur n'avait pas d'item
											// similaire

				World.addObjet(morphObj, true);
			String morphStr = "Creation de l'item " + statsID + " => " + morphIDs + " reussie";
			if (isMax)
				morphStr += " avec des stats maximums";
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, morphStr);
			SocketManager.GAME_SEND_Ow_PACKET(_perso);
			break;
		case 73: // Donner un cadeau à un joueur
			int regalo = 0;
			try {
				regalo = Integer.parseInt(infos[1]);
			} catch (Exception e) {
			}
			Characters objetivo = _perso;
			if (infos.length > 2) {
				objetivo = World.getPersoByName(infos[2]);
				if (objetivo == null) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'est pas reconnu.");
					break;
				}
			}
			objetivo.get_compte().setCadeau(regalo);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Don de " + regalo + " à " + objetivo.get_name());
			break;
			// TODO fusionner les deux commandes
		case 74: //Donner un cadeau à tout les joueurs co
			int gift = 0;
			try {
				gift = Integer.parseInt(infos[1]);
			} catch (Exception e) {
			}
			for (Characters pj : World.getOnlinePersos()) {
				pj.get_compte().setCadeau(gift);
			}
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Don de " + gift + " à tous les joueurs en ligne.");
			break;
		case 75: // EXO un item
			int exoItemID = Integer.parseInt(infos[1]);
			int exoQtt = Integer.parseInt(infos[2]);
			boolean max = Integer.parseInt(infos[3]) == 1;
			String donnate = infos[4];
			if (exoQtt < 1)
				exoQtt = 1;
			if (!donnate.equalsIgnoreCase("PA") && !donnate.equalsIgnoreCase("PM")) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out,
						"Vous devez choisir soit de d'attribuer un PA ou un PM");
				break;
			}
			ObjTemplate OT = World.getObjTemplate(exoItemID);
			if (OT == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "L'item indiqué n'a aucun template de définie");
				break;
			}
			int value = 1;
			if (donnate.equalsIgnoreCase("PA"))
				value = 1;
			else if (donnate.equalsIgnoreCase("PM"))
				value = 2;
			Objects exoObj = OT.createNewItem(exoQtt, max, value);
			if (_perso.addObjet(exoObj, true))
				World.addObjet(exoObj, true);
			if (exoObj != null && _perso.isOnline()) {
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out,
						"Vous venez de créer l'item " + exoObj.getTemplate().getName() + " avec 1 " + donnate);
			}
			SocketManager.GAME_SEND_Ow_PACKET(_perso);
			break;
		case 76: //Apprendre un sort
			int spell = -1;
			try {
				spell = Integer.parseInt(infos[1]);
			} catch (Exception e) {
			}
			;
			if (spell == -1) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Valeur invalide");
				break;
			}
			Characters learner = _perso;
			if (infos.length > 2)// Si un nom de perso est spécifié
			{
				learner = World.getPersoByName(infos[2]);
				if (learner == null) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'a pas ete trouve");
					break;
				}
			}

			learner.learnSpell(spell, 1, true, true);

			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Le sort a ete appris");
			break;
		case 77: //Donner des points de sort
			int ptsQtt = -1;
			try {
				ptsQtt = Integer.parseInt(infos[1]);
			} catch (Exception e) {
			}
			;
			if (ptsQtt == -1) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Valeur invalide");
				break;
			}
			Characters pointsTarget = _perso;
			if (infos.length > 2)// Si un nom de perso est spécifié
			{
				pointsTarget = World.getPersoByName(infos[2]);
				if (pointsTarget == null) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'a pas ete trouve");
					break;
				}
			}
			pointsTarget.addSpellPoint(ptsQtt);
			SocketManager.GAME_SEND_STATS_PACKET(pointsTarget);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Le nombre de point de sort a ete modifiee");
			break;
		case 78: // Recompense arena (fin de saison)
			try {
				ResultSet RS = SQLManager.executeQuery("SELECT * FROM arena ORDER BY quote DESC LIMIT 0, 10",
						Config.DB_NAME);
				int rank = 0;
				while (RS.next()) {
					rank++;
					if (rank < 5) {
						for (Characters c : Team.getPlayers(Team.getTeamByID(RS.getInt("id")))) {
							if (rank == 1) {
								int newPackPoints = Utils.loadPointsPackByAccount(c.get_compte()) + 500;
								Utils.updatePointsPackByAccount(c.get_compte(), newPackPoints);
								if (c.isOnline())
									SocketManager.GAME_SEND_MESSAGE(c,
											"Félicitation, vous venez de remporter 500 points pack avec la 1ere place du Top Pvp 2v2 !",
											Colors.RED);
							} else if (rank == 2) {
								int newPackPoints = Utils.loadPointsPackByAccount(c.get_compte()) + 400;
								Utils.updatePointsPackByAccount(c.get_compte(), newPackPoints);
								if (c.isOnline())
									SocketManager.GAME_SEND_MESSAGE(c,
											"Félicitation, vous venez de remporter 400 points pack avec la 2e place du Top Pvp 2v2 !",
											Colors.RED);
							} else if (rank == 3) {
								int newPackPoints = Utils.loadPointsPackByAccount(c.get_compte()) + 300;
								Utils.updatePointsPackByAccount(c.get_compte(), newPackPoints);
								if (c.isOnline())
									SocketManager.GAME_SEND_MESSAGE(c,
											"Félicitation, vous venez de remporter 300 points pack avec la 3e place du Top Pvp 2v2 !",
											Colors.RED);
							} else if (rank == 4) {
								int newPackPoints = Utils.loadPointsPackByAccount(c.get_compte()) + 200;
								Utils.updatePointsPackByAccount(c.get_compte(), newPackPoints);
								if (c.isOnline())
									SocketManager.GAME_SEND_MESSAGE(c,
											"Félicitation, vous venez de remporter 200 points pack avec la 4e place du Top Pvp 2v2 !",
											Colors.RED);
							} else if (rank == 5) {
								int newPackPoints = Utils.loadPointsPackByAccount(c.get_compte()) + 200;
								Utils.updatePointsPackByAccount(c.get_compte(), newPackPoints);
								if (c.isOnline())
									SocketManager.GAME_SEND_MESSAGE(c,
											"Félicitation, vous venez de remporter 200 points pack avec la 5e place du Top Pvp 2v2 !",
											Colors.RED);
							}
						}
					}
				}
				RS.getStatement().close();
				RS.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			for (Entry<Integer, Team> arenaTeam : Team.getTeams().entrySet()) {
				arenaTeam.getValue().setCote(0);
				Team.updateTeam(arenaTeam.getKey());
			}
			SocketManager.GAME_SEND_MESSAGE_TO_ALL(
					"Les récompenses d'arène 2v2 sont livrées ! La période d'arène est terminée, le classement retourne à 0. Bonne chance pour la nouvelle saison !!",
					Colors.RED);
			break;
		case 79: // Ajouter de l'honneur
			int honor = 0;
			try {
				honor = Integer.parseInt(infos[1]);
			} catch (Exception e) {
			}
			;
			Characters honorTarget = _perso;
			if (infos.length > 2)// Si un nom de perso est spécifié
			{
				honorTarget = World.getPersoByName(infos[2]);
				if (honorTarget == null) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'a pas ete trouve");
					break;
				}
			}
			if (honorTarget.get_align() == Constant.ALIGNEMENT_NEUTRE) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le joueur est neutre ...");
				break;
			}
			honorTarget.addHonor(honor);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Vous avez ajouté " + honor + " honneur a " + honorTarget.get_name());
			break;
		case 80: //Vérouiller le serveur
			byte LockValue = 1;// Accessible
			try {
				LockValue = Byte.parseByte(infos[1]);
			} catch (Exception e) {
				getMan(command);
				break;
			}

			if (LockValue > 2)
				LockValue = 2;
			if (LockValue < 0)
				LockValue = 0;
			World.set_state((short) LockValue);
			if (LockValue == 1) {
				SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, "Serveur accessible.");
			} else if (LockValue == 0) {
				SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, "Serveur inaccessible.");
			} else if (LockValue == 2) {
				SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, "Serveur en sauvegarde.");
			}
			break;
		case 81: //Purger la ram
			try {
				SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, "Tentative de purge de la ram.");
				Runtime r = Runtime.getRuntime();
				try {
					r.runFinalization();
					r.gc();
					System.gc();
					SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Ram purgée.");
					SocketManager.GAME_SEND_cMK_PACKET_TO_ADMIN("@", 0, "Legend'Emu", "Ram Purged");
				} catch (Exception e) {
					SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Impossible de purger la ram.");
				}
			} catch (Exception e) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Impossible de supprimer les clones");
			}
			break;
		case 82: //Kick et bloquer les comptes < GM
			byte GmAccess = 0;
			byte KickPlayer = 0;
			try {
				GmAccess = Byte.parseByte(infos[1]);
				KickPlayer = Byte.parseByte(infos[2]);
			} catch (Exception e) {
			}
			;

			World.setGmAccess(GmAccess);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Serveur bloque au GmLevel : " + GmAccess);
			if (KickPlayer > 0) {
				for (Characters z : World.getOnlinePersos()) {
					if (z.get_compte().get_gmLvl() < GmAccess)
						z.get_compte().getGameThread().closeSocket();
				}
				SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out,
						"Les joueurs de GmLevel inferieur a " + GmAccess + " ont ete kicks.");
			}
			break;
		case 83: // Remplir les HDV
			int numb = 1;
			try {
				numb = Integer.parseInt(infos[1]);
			} catch (Exception e) {
			}
			;
			fullHdv(numb);
			break;
		case 84: //Changer la vitesse d'un joueur
			int speed = 0;
			Characters speedPerso = _perso;
			try {
				speed = Integer.parseInt(infos[1]);
			} catch (Exception e){
				getMan(command);
				break;
			}
			if(infos.length > 2) {
				speedPerso = World.getPersoByName(infos[2]);
			}
			speedPerso.set_Speed(speed);
			SocketManager.GAME_SEND_ALTER_GM_PACKET(speedPerso.get_curCarte(), speedPerso);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Vous avez fixé la vitesse à " + speed);
			break;
		case 86: // Ajouter des pb
			Characters ptsPerso = _perso;
			int addPts = 0;
			
			try {
				addPts = Integer.parseInt(infos[1]);
				if(infos.length > 2)
					ptsPerso = World.getPersoByName(infos[2]);
			} catch (Exception e) {
				getMan(command);
				break;
			}
			if(ptsPerso == null) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Le personnage n'a pas été trouvé");
				break;
			}
			
			int actualPoint = Utils.loadPointsByAccount(ptsPerso.get_compte());
			Utils.updatePointsByAccount(ptsPerso.get_compte(), actualPoint + addPts);
			SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Vous avez ajouté " + addPts + " points à " + ptsPerso.get_name());
			break;
		case 100: // Envoyer un packet (risqué pour les naabs)
			try {
				infos = msg.split(" ", 2);
				SocketManager.send(_out, infos[1]);
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out, "Le packet a été envoyé.");
			} catch (Exception e) {
				SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Argument incorrect.");
			}
			break;
		default :
			SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "Fonction " + command.getFunction() +" non implanté");
			break;
		}
	}
	
	private void getMan(GmCommandManager command) {
		SocketManager.GAME_SEND_CONSOLE_ERROR_PACKET(_out, "<u>Erreur de syntaxe !</u>");
		SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, "Faites " + command.getName() + " " + command.getArgs() + "pour utiliser cette commande.");
	}

	private void fullHdv(int ofEachTemplate) {
		SocketManager.GAME_SEND_CONSOLE_NEUTRAL_PACKET(_out, "Démarrage du remplissage!");

		Objects objet = null;
		HdvEntry entry = null;
		byte amount = 0;
		int hdv = 0;

		int lastSend = 0;
		long time1 = System.currentTimeMillis();// TIME
		for (ObjTemplate curTemp : World.getObjTemplates())// Boucler dans les
															// template
		{
			try {
				if (Config.NOTINHDV.contains(curTemp.getID()))
					continue;
				for (int j = 0; j < ofEachTemplate; j++)// Ajouter plusieur fois
														// le template
				{
					if (curTemp.getType() == 85)
						break;

					objet = curTemp.createNewItem(1, false, -1);
					hdv = getHdv(objet.getTemplate().getType());

					if (hdv < 0)
						break;

					amount = (byte) Formulas.getRandomValue(1, 3);

					entry = new HdvEntry(calculPrice(objet, amount), amount, -1, objet);
					objet.setQuantity(entry.getAmount(true));

					World.getHdv(hdv).addEntry(entry);
					World.addObjet(objet, false);
				}
			} catch (Exception e) {
				continue;
			}

			if ((System.currentTimeMillis() - time1) / 1000 != lastSend
					&& (System.currentTimeMillis() - time1) / 1000 % 3 == 0) {
				lastSend = (int) ((System.currentTimeMillis() - time1) / 1000);
				SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out,
						(System.currentTimeMillis() - time1) / 1000 + "sec Template: " + curTemp.getID());
			}
		}
		SocketManager.GAME_SEND_CONSOLE_SUCCESS_PACKET(_out,
				"Remplissage fini en " + (System.currentTimeMillis() - time1) + "ms");
		World.saveAll(null);
		SocketManager.GAME_SEND_MESSAGE_TO_ALL("HDV remplis!", Colors.RED);
	}

	private int getHdv(int type) {
		int rand = Formulas.getRandomValue(1, 4);
		int map = -1;

		switch (type) {
		case 12:
		case 14:
		case 26:
		case 43:
		case 44:
		case 45:
		case 66:
		case 70:
		case 71:
		case 86:
			if (rand == 1) {
				map = 4271;
			} else if (rand == 2) {
				map = 4607;
			} else {
				map = 7516;
			}
			return map;
		case 1:
		case 9:
			if (rand == 1) {
				map = 4216;
			} else if (rand == 2) {
				map = 4622;
			} else {
				map = 7514;
			}
			return map;
		case 18:
		case 72:
		case 77:
		case 90:
		case 97:
		case 113:
		case 116:
			if (rand == 1) {
				map = 8759;
			} else {
				map = 8753;
			}
			return map;
		case 63:
		case 64:
		case 69:
			if (rand == 1) {
				map = 4287;
			} else if (rand == 2) {
				map = 4595;
			} else if (rand == 3) {
				map = 7515;
			} else {
				map = 7350;
			}
			return map;
		case 33:
		case 42:
			if (rand == 1) {
				map = 2221;
			} else if (rand == 2) {
				map = 4630;
			} else {
				map = 7510;
			}
			return map;
		case 84:
		case 93:
		case 112:
		case 114:
			if (rand == 1) {
				map = 4232;
			} else if (rand == 2) {
				map = 4627;
			} else {
				map = 12262;
			}
			return map;
		case 38:
		case 95:
		case 96:
		case 98:
		case 108:
			if (rand == 1) {
				map = 4178;
			} else if (rand == 2) {
				map = 5112;
			} else {
				map = 7289;
			}
			return map;
		case 10:
		case 11:
			if (rand == 1) {
				map = 4183;
			} else if (rand == 2) {
				map = 4562;
			} else {
				map = 7602;
			}
			return map;
		case 13:
		case 25:
		case 73:
		case 75:
		case 76:
			if (rand == 1) {
				map = 8760;
			} else {
				map = 8754;
			}
			return map;
		case 5:
		case 6:
		case 7:
		case 8:
		case 19:
		case 20:
		case 21:
		case 22:
			if (rand == 1) {
				map = 4098;
			} else if (rand == 2) {
				map = 5317;
			} else {
				map = 7511;
			}
			return map;
		case 39:
		case 40:
		case 50:
		case 51:
		case 88:
			if (rand == 1) {
				map = 4179;
			} else if (rand == 2) {
				map = 5311;
			} else {
				map = 7443;
			}
			return map;
		case 87:
			if (rand == 1) {
				map = 6159;
			} else {
				map = 6167;
			}
			return map;
		case 34:
		case 52:
		case 60:
			if (rand == 1) {
				map = 4299;
			} else if (rand == 2) {
				map = 4629;
			} else {
				map = 7397;
			}
			return map;
		case 41:
		case 49:
		case 62:
			if (rand == 1) {
				map = 4247;
			} else if (rand == 2) {
				map = 4615;
			} else if (rand == 3) {
				map = 7501;
			} else {
				map = 7348;
			}
			return map;
		case 15:
		case 35:
		case 36:
		case 46:
		case 47:
		case 48:
		case 53:
		case 54:
		case 55:
		case 56:
		case 57:
		case 58:
		case 59:
		case 65:
		case 68:
		case 103:
		case 104:
		case 105:
		case 106:
		case 107:
		case 109:
		case 110:
		case 111:
			if (rand == 1) {
				map = 4262;
			} else if (rand == 2) {
				map = 4646;
			} else {
				map = 7413;
			}
			return map;
		case 78:
			if (rand == 1) {
				map = 8757;
			} else {
				map = 8756;
			}
			return map;
		case 2:
		case 3:
		case 4:
			if (rand == 1) {
				map = 4174;
			} else if (rand == 2) {
				map = 4618;
			} else {
				map = 7512;
			}
			return map;
		case 16:
		case 17:
		case 81:
			if (rand == 1) {
				map = 4172;
			} else if (rand == 2) {
				map = 4588;
			} else {
				map = 7513;
			}
			return map;
		case 83:
			if (rand == 1) {
				map = 10129;
			} else {
				map = 8482;
			}
			return map;
		case 82:
			return 8039;
		default:
			return -1;
		}
	}

	private int calculPrice(Objects obj, int logAmount) {
		int amount = (byte) (Math.pow(10, (double) logAmount) / 10);
		int stats = 0;

		for (int curStat : obj.getStats().getMap().values()) {
			stats += curStat;
		}
		if (stats > 0)
			return (int) (((Math.cbrt(stats) * Math.pow(obj.getTemplate().getLevel(), 2)) * 10
					+ Formulas.getRandomValue(1, obj.getTemplate().getLevel() * 100)) * amount);
		else
			return (int) ((Math.pow(obj.getTemplate().getLevel(), 2) * 10
					+ Formulas.getRandomValue(1, obj.getTemplate().getLevel() * 100)) * amount);
	}

	private String BeautifullMessage(String str) {
		str = str.trim();
		if (str.equalsIgnoreCase("stop flood") || str.equalsIgnoreCase("stop flood !"))
			return "Veuillez s'il vous plait arreter de flooder.";
		return str;
	}
}
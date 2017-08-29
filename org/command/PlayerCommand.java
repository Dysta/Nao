package org.command;

import java.util.regex.Pattern;

import org.client.Accounts;
import org.client.Characters;
import org.client.Characters.Stats;
import org.command.player.Maitre;
import org.command.player.RapidStuff;
import org.common.ConditionParser;
import org.common.Constant;
import org.common.SQLManager;
import org.common.SocketManager;
import org.common.World;
import org.fight.extending.Arena;
import org.fight.extending.Kolizeum;
import org.fight.extending.Team;
import org.fight.object.Stalk;
import org.game.GameSendThread;
import org.game.GameThread;
import org.game.tools.Utils;
import org.kernel.Config;
import org.kernel.Logs;
import org.kernel.Main;
import org.object.Maps;
import org.object.Objects;
import org.object.Objects.ObjTemplate;
import org.object.job.Job.StatsMetier;
import org.spell.Spell.SortStats;
import org.utils.Colors;
import org.spell.SpellEffect;

public class PlayerCommand {

	public static boolean launchNewCommand(String msg, Characters _perso, Accounts _compte, long _timeLastsave,
			GameSendThread _out) {

		if (msg.charAt(0) == '.') {
			PlayerCommandManager command = PlayerCommandManager.getCommandByName(msg.substring(1, msg.length() - 1));

			if (command != null) {
				boolean useSuccess = false;
				
				int points = Utils.loadPointsByAccount(_perso.get_compte());
				int price = command.getPrice();
				int diff = 0;
				if (command.getCond() != null && !ConditionParser.validConditions(_perso, command.getCond())) {
					SocketManager.GAME_SEND_MESSAGE(_perso, "Vous ne remplissez pas les conditions nécessaires pour éxécuter cette commande !", Colors.RED);
					return false;
				}

				if (command.getPrice() > 0) {
					points = Utils.loadPointsByAccount(_perso.get_compte());
					price = command.getPrice();
					diff = 0;

					if (points < price) {
						SocketManager.GAME_SEND_MESSAGE(_perso, "Vous n'avez pas assez de points, il vous manque " + (price - points) + " points !", Colors.RED);
						return true;
					}
				}

				for (String f : command.getFunction().split(";")) {
					int type = -1;
					String args = null;

					if (f.contains("-")) {
						args = f.split("-")[1];
						type = Integer.parseInt(f.split("-")[0]);
					} else {
						type = Integer.parseInt(f);
					}
					
					switch (type) {
					case 0: // Liste des commandes
						SocketManager.GAME_SEND_MESSAGE(_perso, PlayerCommandManager.getCommandList(false), Colors.RED);
						useSuccess = true;
						break;
					case 1: // Liste des commandes VIP
						SocketManager.GAME_SEND_MESSAGE(_perso, PlayerCommandManager.getCommandList(true), Colors.RED);
						useSuccess = true;
						break;
					case 2: // Envoyer une message
						String mess = args.split(",")[0];
						String color = args.split(",")[1];
						SocketManager.GAME_SEND_MESSAGE(_perso, mess, color);
						useSuccess = true;
						break;
					case 3: // Envoyer un popup
						String boxMess = args;
						SocketManager.GAME_SEND_POPUP(_perso, boxMess);
						useSuccess = true;
						break;
					case 4: // Informations serveur
						long uptime = System.currentTimeMillis() - Main.gameServer.getStartTime();
						int jour = (int) (uptime / (1000 * 3600 * 24));
						uptime %= (1000 * 3600 * 24);
						int hour = (int) (uptime / (1000 * 3600));
						uptime %= (1000 * 3600);
						int min = (int) (uptime / (1000 * 60));
						uptime %= (1000 * 60);
						int sec = (int) (uptime / (1000));

						String infos = "Uptime : <b>" + jour + "j " + hour + "h " + min + "m " + sec + "s</b>\n"
								+ "Personnages en ligne : <b>" + Main.gameServer.getPlayerNumber() + "</b>\n"
								+ "Personne en ligne : <b>" + Main.gameServer.getClientUnique() + "</b>\n"
								+ "Record de connecté : <b>" + Main.gameServer.getMaxPlayer() + "</b>";
						SocketManager.GAME_SEND_MESSAGE(_perso, infos, Colors.RED);
						useSuccess = true;
						break;
					case 5: // Staff en ligne
						String staff = "Membres du staff connectés :\n";
						boolean allOffline = true;

						for (int i = 0; i < World.getOnlinePersos().size(); i++) {
							/*
							 * if(World.getOnlinePersos().get(i).get_compte() &&
							 * !World.getOnlinePersos().get(i).get_name())
							 * continue;
							 */
							if (World.getOnlinePersos().get(i).get_compte().get_gmLvl() > 0) {
								staff += "- " + World.getOnlinePersos().get(i).get_name() + " (";

								if (World.getOnlinePersos().get(i).get_compte().get_gmLvl() == 1)
									staff += Config.STAFF_GM1 + ")";
								else if (World.getOnlinePersos().get(i).get_compte().get_gmLvl() == 2)
									staff += Config.STAFF_GM2 + ")";
								else if (World.getOnlinePersos().get(i).get_compte().get_gmLvl() == 3)
									staff += Config.STAFF_GM3 + ")";
								else if (World.getOnlinePersos().get(i).get_compte().get_gmLvl() == 4)
									staff += Config.STAFF_GM4 + ")";
								else if (World.getOnlinePersos().get(i).get_compte().get_gmLvl() == 5)
									staff += Config.STAFF_GM5 + ")";
								else
									staff += "Unknown";
								staff += "\n";
								allOffline = false;
							}
						}
						if (!staff.isEmpty() && !allOffline) {
							SocketManager.GAME_SEND_MESSAGE(_perso, staff, Colors.RED);
						} else if (allOffline) {
							SocketManager.GAME_SEND_MESSAGE(_perso, "Aucun membre du staff est présent !",
									Colors.RED);
						}
						useSuccess = true;
						break;
					case 6: // Sauvegarde du personnage
						if ((System.currentTimeMillis() - _timeLastsave) < 360000 || _perso.get_fight() != null)
							break;

						_timeLastsave = System.currentTimeMillis();
						SQLManager.SAVE_PERSONNAGE(_perso, true);
						SocketManager.GAME_SEND_MESSAGE(_perso, "Votre personnage <b>" + _perso.get_name() + "</b> est sauvegardé.",
								Colors.RED);
						useSuccess = true;
						break;
					case 7: // Rafraichissement de Map
						if (_perso.get_fight() != null)
							break;
						Maps map = _perso.get_curCarte();
						map.refreshSpawns();
						useSuccess = true;
						break;
					case 8: // Vitalité Max
						if (_perso.get_fight() != null)
							break;
						int count = 100;
						Characters perso = _perso;
						int newPDV = (perso.get_PDVMAX() * count) / 100;

						perso.set_PDV(newPDV);
						if (perso.isOnline())
							SocketManager.GAME_SEND_STATS_PACKET(perso);
						SocketManager.GAME_SEND_MESSAGE(_perso, "Votre vie est désormais au maximum !", Colors.RED);
						useSuccess = true;
						break;
					case 9: // Création de guilde
						if (_perso.get_guild() != null || _perso.getGuildMember() != null || !_perso.isOnline() || _perso == null || _perso.get_fight() != null) 
							break;
						else
							SocketManager.GAME_SEND_gn_PACKET(_perso);
						useSuccess = true;
						break;
					case 10: // Parchotage
						if (_perso.get_fight() != null)
							break;

						int nbreElement = 0;

						if (_perso.get_baseStats().getEffect(125) < 101) {
							_perso.get_baseStats().addOneStat(125, 101 - _perso.get_baseStats().getEffect(125));
							nbreElement++;
						}

						if (_perso.get_baseStats().getEffect(124) < 101) {
							_perso.get_baseStats().addOneStat(124, 101 - _perso.get_baseStats().getEffect(124));
							nbreElement++;
						}

						if (_perso.get_baseStats().getEffect(118) < 101) {
							_perso.get_baseStats().addOneStat(118, 101 - _perso.get_baseStats().getEffect(118));
							if (nbreElement == 0)
								nbreElement++;
						}

						if (_perso.get_baseStats().getEffect(126) < 101) {
							_perso.get_baseStats().addOneStat(126, 101 - _perso.get_baseStats().getEffect(126));
							if (nbreElement == 0)
								nbreElement++;
						}

						if (_perso.get_baseStats().getEffect(119) < 101) {
							_perso.get_baseStats().addOneStat(119, 101 - _perso.get_baseStats().getEffect(119));
							if (nbreElement == 0)
								nbreElement++;
						}

						if (_perso.get_baseStats().getEffect(123) < 101) {
							_perso.get_baseStats().addOneStat(123, 101 - _perso.get_baseStats().getEffect(123));
							if (nbreElement == 0)
								nbreElement++;
						}

						if (nbreElement == 0) {
							SocketManager.GAME_SEND_MESSAGE(_perso, "Vous êtes déjà parchotté dans tout les éléments !",
									Colors.RED);
						} else {
							SocketManager.GAME_SEND_STATS_PACKET(_perso);
							SocketManager.GAME_SEND_MESSAGE(_perso, "Vous êtes parchotté dans tout les éléments !",
									Colors.RED);
						}
						useSuccess = true;
						break;
					case 11: // Apprendre un sort
						if (_perso.get_fight() != null)
							break;
						int spellID = Integer.parseInt(args.split(",")[0]);
						int level = Integer.parseInt(args.split(",")[1]);
						_perso.learnSpell(spellID, level, true, true);
						useSuccess = true;
						break;
					case 12: // Alignement
						if (_perso.get_fight() != null)
							break;
						byte align = (byte) Integer.parseInt(args);
						_perso.modifAlignement(align);
						if (_perso.isOnline())
							SocketManager.GAME_SEND_STATS_PACKET(_perso);
						useSuccess = true;
						break;
					case 13: // Ajouter Kamas
						int kamas = Integer.parseInt(args);
						_perso.addKamas(kamas);
						SocketManager.GAME_SEND_STATS_PACKET(_perso);
						useSuccess = true;
						break;
					case 14: // Ajouter Item
						int tID = Integer.parseInt(args.split(",")[0]);
						int nbr = Integer.parseInt(args.split(",")[1]);
						boolean isMax = Boolean.parseBoolean(args.split(",")[2]);

						ObjTemplate t = World.getObjTemplate(tID);
						Objects obj = t.createNewItem(nbr, isMax, -1);
						if (_perso.addObjet(obj, true))
							World.addObjet(obj, true);
						useSuccess = true;
						break;
					case 15: // Devenir VIP
						Accounts account = _perso.get_compte();
						if (account == null)
							return true;
						if (account.get_vip() == 0) {
							account.set_vip(1);
							SQLManager.UPDATE_ACCOUNT_VIP(account);
						}
						useSuccess = true;
						break;
					case 16: // Ajouter un titre
						if (_perso.get_title() == (byte) Integer.parseInt(args))
							break;
						_perso.set_title((byte) Integer.parseInt(args));
						if (_perso.get_fight() == null)
							SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.get_curCarte(), _perso);
						useSuccess = true;
						break;
					case 17: // Reset des caractéristiques
						if (_perso.get_fight() != null)
							break;
						_perso.get_baseStats().addOneStat(125, -_perso.get_baseStats().getEffect(125));
						_perso.get_baseStats().addOneStat(124, -_perso.get_baseStats().getEffect(124));
						_perso.get_baseStats().addOneStat(118, -_perso.get_baseStats().getEffect(118));
						_perso.get_baseStats().addOneStat(123, -_perso.get_baseStats().getEffect(123));
						_perso.get_baseStats().addOneStat(119, -_perso.get_baseStats().getEffect(119));
						_perso.get_baseStats().addOneStat(126, -_perso.get_baseStats().getEffect(126));
						_perso.addCapital((_perso.get_lvl() - 1) * 5 - _perso.get_capital());
						SocketManager.GAME_SEND_STATS_PACKET(_perso);
						useSuccess = true;
						break;
					case 18: // Lancer une traque
						Stalk.newTraque(_perso);
						useSuccess = true;
						break;
					case 19: // Récompense de traque
						Stalk.getRecompense(_perso);
						useSuccess = true;
						break;
					case 20: // Géoposition de la cible
						Stalk.getTraquePosition(_perso);
						useSuccess = true;
						break;
					case 21: // Désaprendre un sort
						if (_perso.get_fight() != null)
							break;
						_perso.setisForgetingSpell(true);
						SocketManager.GAME_SEND_FORGETSPELL_INTERFACE('+', _perso);
						useSuccess = true;
						break;
					case 22: // Morph
						if (_perso.get_fight() != null)
							break;
						int morphID = -1;
						try {
							morphID = Integer.parseInt(msg.substring(command.getName().length() + 2, msg.length() - 1).trim());
						} catch (Exception e) {
							SocketManager.GAME_SEND_MESSAGE(_perso, "Faites ." + command.getName() + " morphID", Colors.RED);
							break;
						}
						if (!Config.CONFIG_MORPH_ALLOWED.contains(morphID) || morphID == -1) {
							SocketManager.GAME_SEND_MESSAGE(_perso, "MorphID non autorisé. MorphID autorisé : " + Config.CONFIG_MORPH_ALLOWED, Colors.RED);
							break;
						}
						_perso.set_gfxID(morphID);
						SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.get_curCarte(), _perso);
						useSuccess = true;
						break;
					case 23: // Donner une statistique
						int statID = Integer.parseInt(args.split(",")[0]);
						int value = Integer.parseInt(args.split(",")[1]);
						_perso.get_baseStats().addOneStat(statID, value);
						SocketManager.GAME_SEND_STATS_PACKET(_perso);
						useSuccess = true;
						break;
					case 24: // Ouvrir banque
						// Sauvagarde du perso et des item avant.
						if (_perso.get_fight() != null)
							break;
						SQLManager.SAVE_PERSONNAGE(_perso, true);
						if (_perso.getDeshonor() >= 1) {
							SocketManager.GAME_SEND_Im_PACKET(_perso, "183");
							break;
						}
						int cost = _perso.getBankCost();
						if (cost > 0) {
							long nKamas = _perso.get_kamas() - cost;
							if (nKamas < 0)// Si le joueur n'a pas assez de kamas pour ouvrir la banque
							{
								SocketManager.GAME_SEND_Im_PACKET(_perso, "1128;" + cost);
								break;
							}
							_perso.set_kamas(nKamas);
							SocketManager.GAME_SEND_STATS_PACKET(_perso);
							SocketManager.GAME_SEND_Im_PACKET(_perso, "020;" + cost);
						}
						SocketManager.GAME_SEND_ECK_PACKET(_perso.get_compte().getGameThread().get_out(), 5, "");
						SocketManager.GAME_SEND_EL_BANK_PACKET(_perso);
						_perso.set_away(true);
						_perso.setInBank(true);
						useSuccess = true;
						break;
					case 25: // Ajouter des points de sort
						if (_perso.get_fight() != null)
							break;
						int pts = Integer.parseInt(args);
						_perso.addSpellPoint(pts);
						SocketManager.GAME_SEND_STATS_PACKET(_perso);
						useSuccess = true;
						break;
					case 26: // Ajouter des points de capital
						if (_perso.get_fight() != null)
							break;
						int capital = Integer.parseInt(args);
						_perso.addCapital(capital);
						SocketManager.GAME_SEND_STATS_PACKET(_perso);
						useSuccess = true;
						break;
					case 27: // Commandes Kolizeum
						String split;
						try {
							split = msg.substring(command.getName().length() + 2, msg.length() - 1);
						} catch (Exception e) {
							if (_perso.getKolizeum() == 0)
								SocketManager.GAME_SEND_MESSAGE(_perso,"Vous êtes déjà inscris au Kolizeum ! Faites |<b> ." + command.getName()+ " off </b>| pour vous désinscrire...",Colors.RED);
							else if (_perso.getKolizeum() == 1)
								SocketManager.GAME_SEND_MESSAGE(_perso, "Vous êtes en combat d'arène !", Colors.RED);
							else
								SocketManager.GAME_SEND_MESSAGE(_perso, "Vous n'êtes pas inscris, faites |<b> ." + command.getName() + " on </b>| pour vous inscrire !", Colors.RED);
							break;
						}
						if (split.equals("on")) {
							if (_perso.getKolizeum() != -1) {
								SocketManager.GAME_SEND_MESSAGE(_perso,"Vous êtes déjà inscris ou participez déjà au Kolizeum !",Colors.RED);
								break;
							}
							if (_perso.getGroup() != null) {
								if (_perso.getGroup().getPersos().size() != Config.KOLIMAX_PLAYER) {
									SocketManager.GAME_SEND_MESSAGE(_perso, "Votre groupe doit contenir exactement " + Config.KOLIMAX_PLAYER + " joueurs !", Colors.RED);
									break;
								} else if (!_perso.getGroup().isChief(_perso.get_GUID())) {
									SocketManager.GAME_SEND_MESSAGE(_perso, "Vous devez être le chef de groupe pour vous inscrire !", Colors.RED);
									break;
								}
								for (Characters c : _perso.getGroup().getPersos()) {
									try {
										if (c.getKolizeum() != -1) {
											SocketManager.GAME_SEND_MESSAGE(_perso, "Le joueur " + c.get_name() + " est déjà inscris ou participe actuellement au kolizeum !", Colors.RED);
											break;
										}

										else if (!c.isOnline()) {
											SocketManager.GAME_SEND_MESSAGE(_perso, "Le joueur " + c.get_name() + " n'est pas connecté !", Colors.RED);
											break;
										} else if (c.get_fight() != null) {
											SocketManager.GAME_SEND_MESSAGE(_perso, "Le joueur " + c.get_name() + " est en combat !", Colors.RED);
											break;
										} else {
											Kolizeum.addGroup(_perso.getGroup());
											break;
										}
									} catch (Exception e) {
									}
								}
							} else {
								if (_perso.get_fight() != null) {
									SocketManager.GAME_SEND_MESSAGE(_perso, "Vous êtes actuellement en combat !",
											Colors.RED);
									break;
								} else {
									Kolizeum.addPlayer(_perso);
									useSuccess = true;
									break;
								}
							}
						} else if (split.equals("off")) {
							try {

								if (_perso.getKolizeum() == 1 && _perso.get_fight() != null) {
									SocketManager.GAME_SEND_MESSAGE(_perso, "Vous êtes en plein tournoi !",
											Colors.RED);
									break;
								} else if (_perso.getKolizeum() == -1) {
									SocketManager.GAME_SEND_MESSAGE(_perso, "Vous n'êtes pas inscris au kolizeum !",
											Colors.RED);
									break;
								} else {
									Kolizeum.delPlayer(_perso);
									useSuccess = true;
									break;
								}
							} catch (Exception e) {
							}

						} else if (split.equals("infos")) {
							if (_perso.getKolizeum() == 1 && _perso.get_fight() != null) {
								SocketManager.GAME_SEND_MESSAGE(_perso, "Vous êtes en plein tournoi !",
										Colors.RED);
								break;
							} else if (_perso.getKolizeum() == -1) {
								SocketManager.GAME_SEND_MESSAGE(_perso, "Vous n'êtes pas inscris au kolizeum !",
										Colors.RED);
								break;
							} else {
								String content = null;
								if (_perso.getKoliTeam().getkCharacters().size() < Config.KOLIMAX_PLAYER)
									content = "Il manque <b>"
											+ (Config.KOLIMAX_PLAYER - _perso.getKoliTeam().getkCharacters().size())
											+ "</b> joueurs dans votre équipe !";
								else
									content = "Votre équipe est au complet ! Patientez le temps qu'une autre équipe soit formée..";

								SocketManager.GAME_SEND_MESSAGE(_perso, content, Colors.RED);
								break;
							}
						} else {
							SocketManager.GAME_SEND_MESSAGE(_perso, "Tâche non reconnue, faites ." + command.getName()
									+ " pour avoir des informations sur la commande.", Colors.RED);
							break;
						}
						break;
					case 28: // Canal sans limite
						try {
							String RangP = "";
							if (_perso.get_compte().get_gmLvl() == 0 && _perso.get_compte().get_vip() == 0)
								RangP = "Joueur";
							if (_perso.get_compte().get_gmLvl() >= 1)
								RangP = "Staff";
							if (_perso.get_compte().get_vip() >= 1 && _perso.get_compte().get_gmLvl() == 0)
								RangP = "VIP";
							String prefix = _perso.get_name();
							String sp = msg.substring(command.getName().length() + 2, msg.length() - 1);
							String clicker_name = "<a href='asfunction:onHref,ShowPlayerPopupMenu," + _perso.get_name() + "'>" + prefix + "</a>";
							SocketManager.GAME_SEND_MESSAGE_TO_ALL("(" + RangP + ")<b> " + clicker_name + "</b> : " + sp, "000099");
							Logs.addToChatLog(sp);
							useSuccess = true;
						} catch (Exception e) {};
						break;
					case 29: // Création de Team Arena
						String s;
						try {
							s = msg.substring(command.getName().length() + 2, msg.length() - 1);
						} catch (Exception e) {
							SocketManager
									.GAME_SEND_MESSAGE(_perso,
											"Pour créer une équipe, invitez votre partenaire dans votre groupe, puis éxécutez ."
													+ command.getName() + " + Nom de votre Team",
											Colors.RED);
							break;
						}
						if (s.length() > 20 || s.contains("#") || s.contains(",") || s.contains(";") || s.contains("/")
								|| s.contains("!") || s.contains(".") || s.contains("'") || s.contains("*")
								|| s.contains("$") || s.contains("+") || s.contains("-") || s.contains("|")
								|| s.contains("~") || s.contains("(") || s.contains(")") || s.contains("[")
								|| s.contains("]") || s.contains("%"))

						{
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"Le nom de votre équipe ne doit pas contenir de caractères spéciaux !",
									Colors.RED);
							break;
						}
						if (_perso.getGroup() != null) {
							if (_perso.getGroup().getPersos().size() > 2) {
								SocketManager.GAME_SEND_MESSAGE(_perso,
										"Votre groupe comporte plus de 2 joueurs ! L'arène est de type 2v2, ne l'oubliez pas !",
										Colors.RED);
								break;
							}
							if (!_perso.getGroup().isChief(_perso.get_GUID())) {
								SocketManager.GAME_SEND_MESSAGE(_perso, "Vous n'êtes pas chef de groupe !",
										Colors.RED);
								break;
							}
							String players = "";
							boolean first = true;
							int classe = 0;
							for (Characters c : _perso.getGroup().getPersos()) {
								if (classe == c.get_classe()) {
									SocketManager.GAME_SEND_MESSAGE(_perso,
											"Vous ne pouvez pas créer de team avec deux mêmes classes !", Colors.RED);
									break;
								}
								if (!Arena.isVerifiedTeam(_perso.get_classe(), c.get_classe())) {
									SocketManager.GAME_SEND_MESSAGE(_perso,
											"Vous ne pouvez pas créer de team avec deux classes type pillier ! (Xélor, Sacrieur, Eniripsa, Osamodas)",
											Colors.RED);
									break;
								}
								if (first) {
									classe = c.get_classe();
									players = String.valueOf(c.get_GUID());
									first = false;
								} else if (!first) {
									players += "," + c.get_GUID();
								}
							}
							if (Team.addTeam(s, players, 0, 0)) {
								for (Characters c : _perso.getGroup().getPersos()) {
									SocketManager.GAME_SEND_MESSAGE(c,
											"La Team '<b>" + Team.getTeamByID(_perso.getTeamID()).getName()
													+ "</b>' a été créée avec succès !",
											Colors.RED);
								}
								useSuccess = true;
							}
							break;
						} else {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"Vous n'avez pas de groupe, et par conséquent, aucun partenaire à ajouter dans votre Team !",
									Colors.RED);
							break;
						}
					case 30: // Delete de Team Arena
						String string;
						try {
							string = msg.substring(command.getName().length() + 2, msg.length() - 1);
						} catch (Exception e) {
							if (_perso.getTeamID() != -1)
								SocketManager.GAME_SEND_MESSAGE(_perso,
										"Etes vous sûr de vouloir détruire votre team ? (Quôte d'arène: "
												+ Team.getTeamByID(_perso.getTeamID()).getCote()
												+ ")\n Si oui, faites ." + command.getName() + " ok !",
										Colors.RED);
							else
								SocketManager.GAME_SEND_MESSAGE(_perso, "Vous n'avez actuellement aucune team !",
										Colors.RED);
							break;
						}
						if (string.equals("ok")) {
							if (_perso.getArena() > -1)
								SocketManager.GAME_SEND_MESSAGE(_perso,
										"Action impossible, vous êtes inscris, ou déjà en combat d'arène !",
										Colors.RED);
							else {
								Team.removeTeam(Team.getTeamByID(_perso.getTeamID()), _perso);
								useSuccess = true;
							}
							break;
						} else {
							SocketManager.GAME_SEND_MESSAGE(_perso, "Tâche non reconnue, faites ." + command.getName()
									+ " pour avoir des informations sur la commande.", Colors.RED);
							break;
						}
					case 31: // Informations de Team Arena
						if (_perso.getTeamID() != -1) {
							Characters coep = Team.getPlayer(Team.getTeamByID(_perso.getTeamID()), 1);
							if (Team.getPlayer(Team.getTeamByID(_perso.getTeamID()), 1) == _perso){
								coep = Team.getPlayer(Team.getTeamByID(_perso.getTeamID()), 2);
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"<b>" + Team.getTeamByID(_perso.getTeamID()).getName() + "</b>\n"
											+ "Partenaire: <b>" + coep.get_name() + "</b>\n" + "Côte: <b>"
											+ Team.getTeamByID(_perso.getTeamID()).getCote() + "</b>\n" + "Rang: <b>"
											+ Team.getTeamByID(_perso.getTeamID()).getRank() + "</b>",
									Colors.RED);
								useSuccess = true;
							}
							break;
						} else {
							SocketManager.GAME_SEND_MESSAGE(_perso, "Vous n'avez pas d'équipe d'arène 2v2 !",
									Colors.RED);
							break;
						}
					case 32: // Arena Inscription/Désinscription
						String arena;
						try {
							arena = msg.substring(command.getName().length() + 2, msg.length() - 1);
						} catch (Exception e) {
							if (_perso.getArena() == 0)
								SocketManager.GAME_SEND_MESSAGE(_perso,
										"Vous êtes déjà inscris au tournoi d'arène 2v2 ! Faites |<b> ."
												+ command.getName() + " off </b>| pour vous désinscrire...",
										Colors.RED);
							else if (_perso.getArena() == 1)
								SocketManager.GAME_SEND_MESSAGE(_perso, "Vous êtes en combat d'arène !",
										Colors.RED);
							else
								SocketManager.GAME_SEND_MESSAGE(_perso, "Vous n'êtes pas inscris, faites |<b> ."
										+ command.getName() + " on </b>| pour vous inscrire !",
										Colors.RED);
							break;
						}
						if (arena.equals("on")) {
							if (_perso.getTeamID() < 0) {
								SocketManager.GAME_SEND_MESSAGE(_perso, "Vous ne possédez aucune Team ! (<b>."
										+ command.getName() + "</b> pour plus d'informations)",
										Colors.RED);
								break;
							} else if (_perso.getGroup() == null) {
								SocketManager.GAME_SEND_MESSAGE(_perso,
										"Vous devez être grouppé avec votre partenaire de Team ! (<b>."
												+ command.getName() + "</b> pour plus d'informations)",
										Colors.RED);
								break;
							} else {
								if (_perso.getGroup().getPersos().size() > 2
										|| _perso.getGroup().getPersos().size() < 2) {
									SocketManager.GAME_SEND_MESSAGE(_perso,
											"Votre groupe doit contenir exactement deux joueurs ! Vous, et votre partenaire de Team !",
											Colors.RED);
									break;
								} else if (!_perso.getGroup().isChief(_perso.get_GUID())) {
									SocketManager.GAME_SEND_MESSAGE(_perso,
											"Vous devez être le chef de groupe pour vous inscrire !",
											Colors.RED);
									break;
								}
								for (Characters c : _perso.getGroup().getPersos()) {
									try {
										if (!Team.getPlayers(Team.getTeamByID(_perso.getTeamID())).contains(c)) {
											SocketManager.GAME_SEND_MESSAGE(_perso,
													"Le joueur " + c.get_name()
															+ " n'est pas votre partenaire de Team !",
													Colors.RED);
											break;
										}

										else if (!c.isOnline()) {
											SocketManager.GAME_SEND_MESSAGE(_perso,
													"Le joueur " + c.get_name() + " n'est pas connecté !",
													Colors.RED);
											break;
										} else if (c.get_fight() != null) {
											SocketManager.GAME_SEND_MESSAGE(_perso,
													"Le joueur " + c.get_name() + " est en combat !",
													Colors.RED);
											break;
										} else if (c.getArena() != -1) {
											SocketManager.GAME_SEND_MESSAGE(_perso,
													"Le joueur " + c.get_name()
															+ " est déjà inscris au tournoi d'arène 2v2 !",
													Colors.RED);
											break;
										} else {
											Arena.addTeam(Team.getTeamByID(_perso.getTeamID()));
											useSuccess = true;
											break;
										}
									} catch (Exception e) {
									}
								}
							}
						} else if (arena.equals("off")) {
							try {
								if (_perso.getTeamID() < 0) {
									SocketManager
											.GAME_SEND_MESSAGE(_perso,
													"Vous ne possédez aucune Team ! (<b>." + command.getName()
															+ "</b> pour plus d'informations)",
													Colors.RED);
									break;
								} else if (_perso.getArena() == 1 && _perso.get_fight() != null) {
									SocketManager.GAME_SEND_MESSAGE(_perso, "Vous êtes en plein tournoi !",
											Colors.RED);
									break;
								} else {
									Arena.delTeam(Team.getTeamByID(_perso.getTeamID()));
									useSuccess = true;
									break;
								}
							} catch (Exception e) {
							}
						} else {
							SocketManager.GAME_SEND_MESSAGE(_perso, "Tâche non reconnue, faites ." + command.getName()
									+ " pour avoir des informations sur la commande.", Colors.RED);
							break;
						}
						break;
					case 33: // RapidStuffs
						if (_perso.get_fight() != null)
							break;
						String rs;
						try {
							rs = msg.substring(command.getName().length() + 2, msg.length() - 1);
						} catch (Exception e) {
							SocketManager.GAME_SEND_MESSAGE(_perso, "<b>Equipements rapides: </b>\n" + "<b>."
									+ command.getName() + " create [name]</b> pour créer un nouveau stuff\n" + "<b>."
									+ command.getName() + " remove [name]</b> pour supprimer un stuff\n" + "<b>."
									+ command.getName() + " view</b> pour voir tous vos stuffs rapides disponibles\n"
									+ "<b>." + command.getName() + " equip [name]</b> pour équiper un stuff rapidement",
									Colors.RED);
							break;
						}

						if (msg.length() >= command.getName().length() + 8
								&& msg.substring(command.getName().length() + 2, command.getName().length() + 8)
										.equals("create")) {
							if (_perso.getRapidStuffs().size() > 9) {
								SocketManager.GAME_SEND_MESSAGE(_perso,
										"Vous ne pouvez pas avoir plus de 10 stuffs rapides !",
										Colors.RED);
								break;
							}
							String name = "";
							try {
								name = msg.substring(command.getName().length() + 9, msg.length() - 1);
							} catch (Exception e) {
								SocketManager.GAME_SEND_MESSAGE(_perso, "Erreur ! Entrez un nom à votre stuff rapide: ."
										+ command.getName() + " create [name]", Colors.RED);
								break;
							}

							int coiffe   = _perso.getObjetByPosSpece(Constant.ITEM_POS_COIFFE);
							int cape     = _perso.getObjetByPosSpece(Constant.ITEM_POS_CAPE);
							int arme 	 = _perso.getObjetByPosSpece(Constant.ITEM_POS_ARME);
							int anneau1  = _perso.getObjetByPosSpece(Constant.ITEM_POS_ANNEAU1);
							int amulette = _perso.getObjetByPosSpece(Constant.ITEM_POS_AMULETTE);
							int ceinture = _perso.getObjetByPosSpece(Constant.ITEM_POS_CEINTURE);
							int bottes   = _perso.getObjetByPosSpece(Constant.ITEM_POS_BOTTES);
							int bouclier = _perso.getObjetByPosSpece(Constant.ITEM_POS_BOUCLIER);
							int anneau2  = _perso.getObjetByPosSpece(Constant.ITEM_POS_ANNEAU2);
							int dofus1   = _perso.getObjetByPosSpece(Constant.ITEM_POS_DOFUS1);
							int dofus2   = _perso.getObjetByPosSpece(Constant.ITEM_POS_DOFUS2);
							int dofus3   = _perso.getObjetByPosSpece(Constant.ITEM_POS_DOFUS3);
							int dofus4   = _perso.getObjetByPosSpece(Constant.ITEM_POS_DOFUS4);
							int dofus5   = _perso.getObjetByPosSpece(Constant.ITEM_POS_DOFUS5);
							int dofus6   = _perso.getObjetByPosSpece(Constant.ITEM_POS_DOFUS6);
							int familier = _perso.getObjetByPosSpece(Constant.ITEM_POS_FAMILIER);

							if (!RapidStuff.addRapidStuff(_perso, name,
									coiffe + "," + cape + "," + anneau1 + "," + amulette + "," + ceinture + "," + bottes
											+ "," + familier + "," + bouclier + "," + arme + "," + anneau2 + ","
											+ dofus1 + "," + dofus2 + "," + dofus3 + "," + dofus4 + "," + dofus5 + ","
											+ dofus6))
								SocketManager.GAME_SEND_MESSAGE(_perso,
										"Erreur ! Un stuff rapide est identique ou le nom est déjà utilisé !",
										Colors.RED);
							else {
								SocketManager.GAME_SEND_MESSAGE(_perso, "Nouveau stuff enregistré avec succès !",
										Colors.RED);
								useSuccess = true;
							}
							break;
						}

						else if (msg.length() >= command.getName().length() + 8
								&& msg.substring(command.getName().length() + 2, command.getName().length() + 8)
										.equals("remove")) {
							String name = "";
							try {
								name = msg.substring(command.getName().length() + 9, msg.length() - 1);
							} catch (Exception e) {
								SocketManager
										.GAME_SEND_MESSAGE(_perso,
												"Erreur ! Entrez le nom du stuff rapide à supprimer: "
														+ command.getName() + " remove [name]",
												Colors.RED);
								break;
							}
							if (_perso.getRapidStuffByName(name) != null
									&& RapidStuff.removeRapidStuff(_perso.getRapidStuffByName(name))){
								SocketManager.GAME_SEND_MESSAGE(_perso,
										"Le stuff <b>" + name + "</b> a été supprimé avec succès !",
										Colors.RED);
								useSuccess = true;
							}
							else
								SocketManager.GAME_SEND_MESSAGE(_perso,
										"Le stuff rapide <b>" + name + "</b> est innéxistant ! Faites ."
												+ command.getName() + " view pour avoir la liste.",
										Colors.RED);
							break;
						}

						else if (rs.equals("view")) {
							String list = null;
							if (_perso.getRapidStuffs().isEmpty()) {
								SocketManager.GAME_SEND_MESSAGE(_perso, "Vous n'avez aucun équipement rapide !",
										Colors.RED);
								break;
							} else {
								for (RapidStuff ss : _perso.getRapidStuffs()) {
									if (list == null) {
										list = "-" + ss.getName();
									} else {
										list += "\n-" + ss.getName();
									}
								}
								SocketManager.GAME_SEND_MESSAGE(_perso,
										"\n\n<b>Faites " + command.getName() + " equip + [name] :</b>\n" + list,
										Colors.RED);
								break;
							}
						}

						else if (msg.length() >= command.getName().length() + 7
								&& msg.substring(command.getName().length() + 2, command.getName().length() + 7)
										.equals("equip")) {
							String name = "";
							try {
								name = msg.substring(command.getName().length() + 8, msg.length() - 1);
							} catch (Exception e) {
								SocketManager
										.GAME_SEND_MESSAGE(
												_perso, "Erreur ! Entrez le nom du stuff rapide à équiper: "
														+ command.getName() + " equip [name]",
												Colors.RED);
								break;
							}

							if (_perso.getRapidStuffByName(name) != null) {
								boolean first = true;
								int number = 1;

								for (Objects rapidStuff : _perso.getRapidStuffByName(name).getObjects()) {
									if (rapidStuff == null)
										continue;
									if (!_perso.hasItemGuid(rapidStuff.getGuid())) {
										SocketManager.GAME_SEND_MESSAGE(_perso,
												"L'item <b>"
														+ World.getObjet(rapidStuff.getGuid()).getTemplate().getName()
														+ "</b> ne vous appartient plus et n'a pas pu être équipé !",
												Colors.RED);
										continue;
									} else {
										int pos = Constant.getObjectPosByType(rapidStuff.getTemplate().getType()); //pos
										if (rapidStuff.getTemplate().isArm())
											pos = 1;
										if (rapidStuff.getTemplate().getType() == Constant.ITEM_TYPE_ANNEAU) {
											if (first) {
												pos = 2;
												first = false;
											} else
												pos = 4;
										}
										if (rapidStuff.getTemplate().getType() == Constant.ITEM_TYPE_DOFUS) {
											switch (number) {
											case 1:
												pos = Constant.ITEM_POS_DOFUS1;
												number++;
												break;
											case 2:
												pos = Constant.ITEM_POS_DOFUS2;
												number++;
												break;
											case 3:
												pos = Constant.ITEM_POS_DOFUS3;
												number++;
												break;
											case 4:
												pos = Constant.ITEM_POS_DOFUS4;
												number++;
												break;
											case 5:
												pos = Constant.ITEM_POS_DOFUS5;
												number++;
												break;
											case 6:
												number = Constant.ITEM_POS_DOFUS6;
												break;
											}
										}
										GameThread.Object_move(_perso, _out, 1, rapidStuff.getGuid(), pos, true); // On double pour les déséquipements autos
										if (_perso.isEquip()) { // C'est balow
											GameThread.Object_move(_perso, _out, 1, rapidStuff.getGuid(), pos, false);
											_perso.setEquip(false);
										}
									}
								}
								break;
							} else {
								SocketManager.GAME_SEND_MESSAGE(_perso,
										"Erreur ! Nom incorrect: " + command.getName() + " equip [name]",
										Colors.RED);
								break;
							}
						}

						else {
							SocketManager.GAME_SEND_MESSAGE(_perso, "<b>Equipements rapides: </b>\n\n" + "<b>"
									+ command.getName() + " create [name]</b> pour créer un nouveau stuff\n" + "<b>"
									+ command.getName() + " remove [name]</b> pour supprimer un stuff\n" + "<b>"
									+ command.getName() + " view</b> pour voir tous vos stuffs rapides disponbiles\n"
									+ "<b>" + command.getName() + " equip [name]</b> pour équiper un stuff rapidement",
									Colors.RED);
							break;
						}
					case 34: // Affichage des points du compte
						int accountPoints = Utils.loadPointsByAccount(_perso.get_compte());
						SocketManager.GAME_SEND_MESSAGE(_perso,
								"Vous avez " + accountPoints + " points boutiques !",
								Colors.RED);
						useSuccess = true;
						break;
					case 35: // Job levelUp
						String st;
						int job = 0;
						int item = 0;
						try {
							st = msg.substring(command.getName().length() + 2, msg.length() - 1);
						} catch (Exception e) {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"<b>Liste(Faites . "+command.getName()+" + Job):<br /></b> Cordomage,joaillomage,costumage,dague,epee,marteau,pelle,hache,arc,baguette,baton",
									Colors.RED);
							return true;
						}

						if (st.equalsIgnoreCase("cordomage")) {
							item = 7495;
							job = 62;
						} else if (st.equalsIgnoreCase("joaillomage")) {
							item = 7493;
							job = 63;
						} else if (st.equalsIgnoreCase("costumage")) {
							item = 7494;
							job = 64;
						} else if (st.equalsIgnoreCase("dague")) {
							item = 1520;
							job = 43;
						} else if (st.equalsIgnoreCase("epee")) {
							item = 1339;
							job = 44;
						} else if (st.equalsIgnoreCase("marteau")) {
							item = 1561;
							job = 45;
						} else if (st.equalsIgnoreCase("pelle")) {
							item = 1560;
							job = 46;
						} else if (st.equalsIgnoreCase("hache")) {
							item = 1562;
							job = 47;
						} else if (st.equalsIgnoreCase("arc")) {
							item = 1563;
							job = 48;
						} else if (st.equalsIgnoreCase("baguette")) {
							item = 1564;
							job = 49;
						} else if (st.equalsIgnoreCase("baton")) {
							item = 1565;
							job = 50;
						} else {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"Metier non reconnu, faites ."+command.getName()+" pour avoir la liste", Colors.RED);
							return true;
						}

						_perso.learnJob(World.getMetier(job));
						ObjTemplate a = World.getObjTemplate(item);
						Objects objs = a.createNewItem(1, false, -1);
						if (_perso.addObjet(objs, true))// Si le joueur n'avait pas d'item similaire
							World.addObjet(objs, true);

						StatsMetier SM = _perso.getMetierByID(job);
						SM.addXp(_perso, 1000000);
						SocketManager.GAME_SEND_MESSAGE(_perso,
								"Vous avez appris le métier avec succès et avez reçu l'arme de FM disponibles dans votre inventaire",
								Colors.RED);
						useSuccess = true;
						break;
					case 36: // Modifier la taille du personnage
						if (_perso.get_fight() != null){
							SocketManager.GAME_SEND_Im_PACKET(_perso, "116;<b>Erreur</b>~Vous ne devez pas être en combat pour utiliser cette commande.");
							break;
						}
						int size = 100;
						try {
							size = Integer.parseInt(msg.substring(command.getName().length() + 2, msg.length() - 1));
						} catch (Exception e) {
							SocketManager.GAME_SEND_Im_PACKET(_perso, "116;<b>Erreur</b>~Vous devez saisir un chiffre.");
							break;
						}
						if (size == _perso.get_size()){
							SocketManager.GAME_SEND_Im_PACKET(_perso, "116;<b>Erreur</b>~Nouvelle taille identique à l'ancienne.");
							break;
						}
						if (size < 80 || size > 130){
							SocketManager.GAME_SEND_Im_PACKET(_perso, "116;<b>Erreur</b>~Vous devez saisir une taille entre <b>80</b> et <b>130</b>.");
							break;
						}
						_perso.set_size(size);
						SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.get_curCarte(), _perso);
						useSuccess = true;
						break;
					case 37: // FmCac
						try {
							Objects object = _perso.getObjetByPos(Constant.ITEM_POS_ARME);
							int prix = Integer.parseInt(args);

							if (_perso.get_kamas() < prix) {
								SocketManager.GAME_SEND_MESSAGE(_perso,
										"Action impossible : vous avez moins de " + prix + " k",
										Colors.RED);
								return true;

							} else if (_perso.get_fight() != null) {
								SocketManager.GAME_SEND_MESSAGE(_perso,
										"Action impossible : vous ne devez pas être en combat",
										Colors.RED);
								return true;

							} else if (object == null) {
								SocketManager.GAME_SEND_MESSAGE(_perso, "Action impossible : vous ne portez pas d'arme",
										Colors.RED);
								return true;
							}

							boolean containNeutre = false;

							for (SpellEffect effect : object.getEffects()) {
								if (effect.getEffectID() == 100 || effect.getEffectID() == 95)
									containNeutre = true;
							}
							if (!containNeutre) {
								SocketManager.GAME_SEND_MESSAGE(_perso,
										"Action impossible : votre arme n'a pas de dégats neutre",
										Colors.RED);
								return true;
							}

							String answer;

							try {
								answer = msg.substring(command.getName().length() + 2, msg.length() - 1);
							} catch (Exception e) {
								SocketManager.GAME_SEND_MESSAGE(_perso,
										"Action impossible : vous n'avez pas spécifié l'élément (air, feu, terre, eau) qui remplacera les dégats/vols de vies neutres",
										Colors.RED);
								return true;
							}

							if (!answer.equalsIgnoreCase("air") && !answer.equalsIgnoreCase("terre")
									&& !answer.equalsIgnoreCase("feu") && !answer.equalsIgnoreCase("eau")) {
								SocketManager.GAME_SEND_MESSAGE(_perso,
										"Action impossible : l'élément " + answer
												+ " n'existe pas ! (dispo : air, feu, terre, eau)",
										Colors.RED);
								return true;
							}

							for (int i = 0; i < object.getEffects().size(); i++) {

								if (object.getEffects().get(i).getEffectID() == 100) {
									if (answer.equalsIgnoreCase("air"))
										object.getEffects().get(i).setEffectID(98);

									if (answer.equalsIgnoreCase("feu"))
										object.getEffects().get(i).setEffectID(99);

									if (answer.equalsIgnoreCase("terre"))
										object.getEffects().get(i).setEffectID(97);

									if (answer.equalsIgnoreCase("eau"))
										object.getEffects().get(i).setEffectID(96);
								}

								if (object.getEffects().get(i).getEffectID() == 95) {
									if (answer.equalsIgnoreCase("air"))
										object.getEffects().get(i).setEffectID(93);

									if (answer.equalsIgnoreCase("feu"))
										object.getEffects().get(i).setEffectID(94);

									if (answer.equalsIgnoreCase("terre"))
										object.getEffects().get(i).setEffectID(92);

									if (answer.equalsIgnoreCase("eau"))
										object.getEffects().get(i).setEffectID(91);

								}
							}

							long new_kamas = _perso.get_kamas() - price;
							if (new_kamas < 0)
								new_kamas = 0;
							_perso.set_kamas(new_kamas);

							SocketManager.GAME_SEND_STATS_PACKET(_perso);
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"Votre objet <b>" + object.getTemplate().getName()
											+ "</b> a été forgemagé avec succès en " + answer,
									Colors.RED);
							SocketManager.GAME_SEND_OCO_PACKET(_perso, object);
							useSuccess = true;
						} catch (Exception e) {
						}
						break;
					case 38: // Téléportation
						if (_perso.get_fight() != null)
							break;
						short mapID = (short) Integer.parseInt(args.split(",")[0]);
						int cellID = Integer.parseInt(args.split(",")[1]);
						_perso.teleport(mapID, cellID);
						useSuccess = true;
						break;
					case 39: // Type LevelUp des sorts
						try {
							if (_perso.get_fight() != null)
								break;
							String[] mySplit = args.split(",");
							int levelUp = Integer.parseInt(mySplit[0]) > 6 ? (6) : (Integer.parseInt(mySplit[0]));
							boolean isFree = Boolean.parseBoolean(mySplit[1].toLowerCase());

							for (SortStats spell : _perso.getSorts()) {
								int curLevel = _perso.getSortStatBySortIfHas(spell.getSpellID()).getLevel();
								if (curLevel != 6 || curLevel < levelUp) {
									while (curLevel < levelUp) {
										if (!isFree) {
											if (_perso.get_spellPts() >= curLevel && World.getSort(spell.getSpellID())
													.getStatsByLevel(curLevel + 1).getReqLevel() <= _perso.get_lvl()) {
												if (_perso.learnSpell(spell.getSpellID(), curLevel + 1, false, false)) {
													_perso.set_spellPts(_perso.get_spellPts() - curLevel);
												}
											}
											curLevel++;
										} else {
											if (World.getSort(spell.getSpellID()).getStatsByLevel(curLevel + 1)
													.getReqLevel() <= _perso.get_lvl())
												_perso.learnSpell(spell.getSpellID(), curLevel + 1, false, false);
											curLevel++;
										}
									}
								}
							}
							SocketManager.GAME_SEND_STATS_PACKET(_perso);
							SocketManager.GAME_SEND_SPELL_LIST(_perso);
							useSuccess = true;
						} catch (Exception e) {
						}
						break;
					case 41: // Mettre un ancien titre
						try {
							if (_perso.get_fight() != null)
								break;
							int answer = -1;
							try {
								answer = Integer.parseInt(msg.substring(command.getName().length() + 2, msg.length() - 1));
							} catch (Exception e) {
								answer = -1;
							}
							;
							if (answer > 0) {
								boolean verif = false;
								/* TODO String titles = _perso.getTitleAdded();
								for (String title : titles.split(",")) {
									if (Integer.parseInt(title) == answer) {
										verif = true;
									}
								}
								*/
								if (verif) {
									_perso.set_title((byte) answer);
									SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.get_curCarte(), _perso);
								}
								return true;
							} else {
								/* TODO
								String titles = _perso.getTitleAdded();
								String liste = "Vous posseder les titre : \n";
								for (String title : titles.split(",")) {
									liste += "<b>" + title + "</b> \n";
								}
								SocketManager.GAME_SEND_MESSAGE(_perso, liste, Colors.RED);
								 */
								return true;
							}
						} catch (Exception e) {
						}
						;
						break;
					case 42: // Tag V.I.P
						try {
							if (_perso.get_fight() != null)
								break;
							if (_perso.get_name().contains("[V.I.P]")) {
								_perso.set_name(_perso.get_name().replace("[V.I.P] ", ""));
								_perso.set_name(_perso.get_name().replace("[V.I.P]", ""));
								SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.get_curCarte(), _perso);
								SocketManager.GAME_SEND_MESSAGE(_perso, "La tag V.I.P a bien été enlever de votre nom.",
										Colors.RED);
								break;
							} else {
								_perso.set_name("[V.I.P] " + _perso.get_name());
								SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.get_curCarte(), _perso);
								SocketManager.GAME_SEND_MESSAGE(_perso, "La tag V.I.P a bien été ajouter à votre nom.",
										Colors.RED);
								useSuccess = true;
								break;
							}
						} catch (Exception e) {}
						break;
					case 46: // Demorph
						if (_perso.get_fight() != null)
							break;
						int normal = _perso.get_classe() * 10 + _perso.get_sexe();
						if (_perso.get_gfxID() == normal) {
							SocketManager.GAME_SEND_MESSAGE(_perso, "Votre apparence est déjà normal !",
									Colors.RED);
							break;
						}
						_perso.set_gfxID(normal);
						// TODO _perso.add_lastSkin(normal);
						SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.get_curCarte(), _perso);
						useSuccess = true;
						break;
					case 47: // EXO PM
						String cat = "Coiffe, Cape, AnneauD, AnneauG, Amulette, Ceinture, Bottes";
						String choix = null;
						Objects items = null;

						if (_perso.get_fight() != null) {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"Commande inutilisable en combat.",
									Colors.RED);
							break;
						}

						try {
							choix = msg.substring(command.getName().length() + 2, msg.length() - 1);
						} catch (Exception e) {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"Faite ." + command.getName()
											+ " <b>catégorie</b> pour ajouter 1 PA à votre objet. " 
											+ "Catégorie : " + cat,
									Colors.RED);
							break;
						}

						if (choix.equalsIgnoreCase("Coiffe")) {
							items = _perso.getObjetByPos(Constant.ITEM_POS_COIFFE);
						} else if (choix.equalsIgnoreCase("Cape")) {
							items = _perso.getObjetByPos(Constant.ITEM_POS_CAPE);
						} else if (choix.equalsIgnoreCase("AnneauD")) {
							items = _perso.getObjetByPos(Constant.ITEM_POS_ANNEAU2);
						} else if (choix.equalsIgnoreCase("AnneauG")) {
							items = _perso.getObjetByPos(Constant.ITEM_POS_ANNEAU1);
						} else if (choix.equalsIgnoreCase("Ceinture")) {
							items = _perso.getObjetByPos(Constant.ITEM_POS_CEINTURE);
						} else if (choix.equalsIgnoreCase("Bottes")) {
							items = _perso.getObjetByPos(Constant.ITEM_POS_BOTTES);
						} else if (choix.equalsIgnoreCase("Amulette")) {
							items = _perso.getObjetByPos(Constant.ITEM_POS_AMULETTE);
						} else {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"<b>Vous avez indiqué une catégorie inexistante</b>. <br />Catégorie : " + cat
											+ "<br />Vous êtes remboursé.",
									Colors.RED);
							Utils.updatePointsByAccount(_perso.get_compte(),
									command.getPrice() + Utils.loadPointsByAccount(_perso.get_compte()));
							break;
						}

						if (items == null) {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"<b>Vous n'avez pas d'item</b>.",
									Colors.RED);
							break;
						}

						Stats stats = items.getStats();

						if (stats.getEffect(111) > 0 || stats.getEffect(128) > 0) {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"<b>L'item choisi donne déjà 1 PA ou 1 PM</b>.",
									Colors.RED);
							break;
						} else {
							items.getStats().addOneStat(128, 1);
							SocketManager.GAME_SEND_STATS_PACKET(_perso);
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"Votre <b>" + ((Objects) items).getTemplate().getName()
											+ "</b> donne désormais +1 PM en plus de ses jets habituels !",
									Colors.RED);
							SocketManager.GAME_SEND_OCO_PACKET(_perso, items);
							useSuccess = true;
						}
						break;
					case 49: // EXO PA
						String cat1 = "Coiffe, Cape, AnneauD, AnneauG, Amulette, Ceinture, Bottes";
						String choix1 = null;
						Objects items1 = null;

						if (_perso.get_fight() != null) {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"Commande inutilisable en combat.",
									Colors.RED);
							break;
						}

						try {
							choix1 = msg.substring(command.getName().length() + 2, msg.length() - 1);
						} catch (Exception e) {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"Faite ." + command.getName()
											+ " <b>catégorie</b> pour ajouter 1 PA à votre objet. "
											+ "Catégorie : " + cat1,
									Colors.RED);
							break;
						}

						if (choix1.equalsIgnoreCase("Coiffe")) {
							items1 = _perso.getObjetByPos(Constant.ITEM_POS_COIFFE);
						} else if (choix1.equalsIgnoreCase("Cape")) {
							items1 = _perso.getObjetByPos(Constant.ITEM_POS_CAPE);
						} else if (choix1.equalsIgnoreCase("AnneauD")) {
							items1 = _perso.getObjetByPos(Constant.ITEM_POS_ANNEAU2);
						} else if (choix1.equalsIgnoreCase("AnneauG")) {
							items1 = _perso.getObjetByPos(Constant.ITEM_POS_ANNEAU1);
						} else if (choix1.equalsIgnoreCase("Ceinture")) {
							items1 = _perso.getObjetByPos(Constant.ITEM_POS_CEINTURE);
						} else if (choix1.equalsIgnoreCase("Bottes")) {
							items1 = _perso.getObjetByPos(Constant.ITEM_POS_BOTTES);
						} else if (choix1.equalsIgnoreCase("Amulette")) {
							items1 = _perso.getObjetByPos(Constant.ITEM_POS_AMULETTE);
						} else {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"<b>Vous avez indiqué une catégorie inexistante</b>. "
									+ "Catégorie : " + cat1,
									Colors.RED);
							break;
						}

						if (items1 == null) {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"<b>Vous n'avez pas d'item</b>.",
									Colors.RED);
							break;
						}

						Stats stats1 = items1.getStats();

						if (stats1.getEffect(111) > 0 || stats1.getEffect(128) > 0) {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"<b>L'item choisi donne déjà 1 PA ou 1 PM</b>.",
									Colors.RED);
							break;
						} else {
							items1.getStats().addOneStat(111, 1);
							SocketManager.GAME_SEND_STATS_PACKET(_perso);
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"Votre <b>" + ((Objects) items1).getTemplate().getName()
											+ "</b> donne désormais +1 PA en plus de ses jets habituels !",
									Colors.RED);
							SocketManager.GAME_SEND_OCO_PACKET(_perso, items1);
							useSuccess = true;
						}
						break;
					case 50: // JP
						String cat3 = "Coiffe, Cape, AnneauD, AnneauG, Amulette, Ceinture, Bottes";
						String choix3 = null;
						Objects items3 = null;
						
						if (_perso.get_fight() != null) {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"Commande inutilisable en combat.",
									Colors.RED);
							break;
						}

						try {
							choix3 = msg.substring(command.getName().length() + 2, msg.length() - 1);
						} catch (Exception e) {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"Faite ." + command.getName()
											+ " <b>catégorie</b> pour obtenir le même objet en JP. "
											+ "Catégorie : " + cat3,
									Colors.RED);
							break;
						}

						if (choix3.equalsIgnoreCase("Coiffe")) {
							items3 = _perso.getObjetByPos(Constant.ITEM_POS_COIFFE);
						} else if (choix3.equalsIgnoreCase("Cape")) {
							items3 = _perso.getObjetByPos(Constant.ITEM_POS_CAPE);
						} else if (choix3.equalsIgnoreCase("AnneauD")) {
							items3 = _perso.getObjetByPos(Constant.ITEM_POS_ANNEAU2);
						} else if (choix3.equalsIgnoreCase("AnneauG")) {
							items3 = _perso.getObjetByPos(Constant.ITEM_POS_ANNEAU1);
						} else if (choix3.equalsIgnoreCase("Ceinture")) {
							items3 = _perso.getObjetByPos(Constant.ITEM_POS_CEINTURE);
						} else if (choix3.equalsIgnoreCase("Bottes")) {
							items3 = _perso.getObjetByPos(Constant.ITEM_POS_BOTTES);
						} else if (choix3.equalsIgnoreCase("Amulette")) {
							items3 = _perso.getObjetByPos(Constant.ITEM_POS_AMULETTE);
						} else {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"<b>Vous avez indiqué une catégorie inexistante</b>. "
									+ "Catégorie : " + cat3,
									Colors.RED);
							break;
						}

						if (items3 == null) {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"<b>Vous n'avez pas d'item</b>.",
									Colors.RED);
							break;
						} else {
							ObjTemplate t2 = World.getObjTemplate(items3.getTemplate().getID()); // On créer le template de l'objet
							Objects obj2 = t2.createNewItem(1, true, -1); // On créer l'objet en qtt 1 et avec jet max
							if (_perso.addObjet(obj2, true))// On regarde si il n'a pas déjà un item
								World.addObjet(obj2, true); // On envoi l'item
							SocketManager.GAME_SEND_MESSAGE(_perso, "Vous avez obtenu <b>"
									+ items3.getTemplate().getName() + "</b> avec ces effets maximum",
									Colors.RED);
							SocketManager.GAME_SEND_Ow_PACKET(_perso);
							useSuccess = true;
						}
						break;
					case 51: // Tag
						try {
							if (_perso.get_fight() != null)
								break;
							String tag = "";
							try {
								tag = msg.substring(command.getName().length() + 2, msg.length() - 1);
							} catch (Exception e) {
								SocketManager.GAME_SEND_Im_PACKET(_perso, "116;<b>Erreur</b>~Vous avez saisis un tag incorrect.");
								break;
							};
							tag = tag.toUpperCase(); // On met le tag en majuscule
							if (!Pattern.matches("^[A-Z]{2,4}$", tag)) {
								SocketManager.GAME_SEND_Im_PACKET(_perso, "116;<b>Erreur</b>~Vous avez saisis un tag incorrect.");
								break;
							}
							_perso.set_tag("["+tag+"]-");
							SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.get_curCarte(), _perso);
							SocketManager.GAME_SEND_MESSAGE(_perso, "Le tag a été mis en place.",Colors.RED);
							useSuccess = true;
							break;
						} catch (Exception e) {
							Logs.addToDebug("Erreur execution commande" + command.getName() + "\n" + e.getMessage());
						};
						break;
					case 52: // DelTag
						try {
							if (_perso.get_fight() != null)
								break;
							if(_perso.get_tag() == null){ // On évite le spam fumé
								SocketManager.GAME_SEND_MESSAGE(_perso, "Vous n'avez pas de tag !", Colors.RED);
								break;
							}
							_perso.set_tag(null);
							SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.get_curCarte(), _perso);
							SocketManager.GAME_SEND_MESSAGE(_perso, "Le tag a été supprimé.",Colors.RED);
							useSuccess = true;
							break;
						} catch (Exception e) {
							Logs.addToDebug(e.getMessage());
						};
						break;
						case 53: //Changer de classe
						try {
							int classe = -1;
							try{
								classe = Integer.parseInt(msg.substring(command.getName().length() + 2, msg.length() - 1));
							} catch (Exception e){
								SocketManager.GAME_SEND_MESSAGE(_perso, 
										"Faites ." + command.getName() + " 1/2/3/4/5/6/7/8/9/10/11/12 pour changer de classe" , Colors.RED);
								break;
							}
			                if (classe == _perso.get_classe()) { // On évite le reset perso gratuit ;p
			                    SocketManager.GAME_SEND_MESSAGE(_perso, "Vous etes déjà de cette classe", Colors.RED);
			                    break;
			                }
			                if(classe < 1 || classe > 12) { // On évite les bugs chelou
			                    SocketManager.GAME_SEND_MESSAGE(_perso, "Classe invalide", Colors.RED);
			                    break;
			                }

			                // On reset tout les caracs
			                _perso.get_baseStats().clear();
			                
			                // On réajoute les caracs de base (sinon il a plus de PA/PM/Pods/etc..)
			                _perso.addCapital((_perso.get_lvl() - 1) * 5 - _perso.get_capital());
			                _perso.get_baseStats().addOneStat(Constant.STATS_ADD_PA, _perso.get_lvl() < 100 ? 6 : 7);
			                _perso.get_baseStats().addOneStat(Constant.STATS_ADD_PM, 3);
			                _perso.get_baseStats().addOneStat(Constant.STATS_ADD_PROS, _perso.get_classe() == Constant.CLASS_ENUTROF ? 120 : 100);
			                _perso.get_baseStats().addOneStat(Constant.STATS_ADD_PODS, 1000);
			                _perso.get_baseStats().addOneStat(Constant.STATS_CREATURE, 1);
			                _perso.get_baseStats().addOneStat(Constant.STATS_ADD_INIT, 1);							

			                // On désaprend tout les sorts
			                _perso.unlearAllSpells();

			                // On change de classe
			                _perso.set_classe(classe);

			                // On apprend les nouveaus sort
			                _perso.setSpells(Constant.getStartSorts(classe));
			                for(int l=1; l <= _perso.get_lvl(); l++) {
			                    Constant.onLevelUpSpells(_perso, l);
			                }
			                _perso.setSpellsPlace(Constant.getStartSortsPlaces(classe));

			                // On change l'apparence
			                int morph = classe * 10 + _perso.get_sexe();
			                _perso.set_gfxID(morph);

			                // Envoi des packets
			                SocketManager.GAME_SEND_ALTER_GM_PACKET(_perso.get_curCarte(), _perso);
			                SocketManager.GAME_SEND_Ow_PACKET(_perso);
			                SocketManager.GAME_SEND_ASK(_perso.get_compte().getGameThread().get_out(), _perso);
			                SocketManager.GAME_SEND_SPELL_LIST(_perso);
			                SocketManager.GAME_SEND_MESSAGE(_perso, "Félicitation ! Vous avez changé de classe avec succès !", Colors.RED);
			                useSuccess = true;

						} catch (Exception e) {
							Logs.addToGameLog(e.getMessage());
						}
			            break;
					case 54 : // Gang
						if (_perso.get_fight() != null) {
							SocketManager.GAME_SEND_MESSAGE(_perso,
									"Vous ne pouvez pas utiliser cette commande en combat.", Colors.RED);
							break;
						}
						
						try {
							// On créer l'escouade
							if ((msg.substring(command.getName().length() + 2, msg.length() - 1)).equalsIgnoreCase("create")){
								if(_perso.get_maitre() != null || _perso.isEsclave()){
									SocketManager.GAME_SEND_MESSAGE(_perso, "Vous êtes déjà membre d'une escouade. Faites <b>." + command.getName() + " delete</b> pour la supprimer.", Colors.RED);
									break;
								}
								_perso.set_maitre(new Maitre(_perso));
								if (_perso.get_maitre() != null) {
									SocketManager.GAME_SEND_MESSAGE(_perso, "Vous venez de créer votre escouade. Vous avez <b>" + _perso.get_maitre().getEsclaves().size()
											+ " membres</b> dedans.", Colors.RED);
									useSuccess = true;
									break;
								} else {
									SocketManager.GAME_SEND_MESSAGE(_perso, "Aucun joueur n'a pus être ajouté à votre escouade.", Colors.RED);
									break;
								}
							}
							
							// On dissous l'escouade
							if ((msg.substring(command.getName().length() + 2, msg.length() - 1)).equalsIgnoreCase("delete")){
								if(_perso.isEsclave()){
									SocketManager.GAME_SEND_MESSAGE(_perso, "Vous ne pouvez pas donner cette ordre, vous n'êtes pas le chef.", Colors.RED);
									break;
								}
								if (_perso.get_maitre() != null) {
									for(Characters p : _perso.get_maitre().getEsclaves()){
										p.setEsclave(false);
									}
									_perso.set_maitre(null);
									SocketManager.GAME_SEND_MESSAGE(_perso, "Votre escouade a été dissoute.", Colors.RED);
									useSuccess = true;
									break;
								} else {
									SocketManager.GAME_SEND_MESSAGE(_perso, "Vous n'avez pas d'escouade.", Colors.RED);
									break;
								}
							}
							
							// On tp l'escouade
							if ((msg.substring(command.getName().length() + 2, msg.length() - 1)).equalsIgnoreCase("join")){
								/* TODO
								if(System.currentTimeMillis() - _perso.getGameClient().timeLastTP < 10000) {
				                    SocketManager.GAME_SEND_MESSAGE(_perso, "Cette commande est disponible toute les 10 secondes.", Colors.RED);
				                    break;
				                }
								
				                if (_perso.isInDungeon()) {
				                    SocketManager.GAME_SEND_MESSAGE(_perso, "Vous ne pouvez pas utiliser cette commande en donjon.", Colors.RED);
				                    break;
				                }
				                */
								if (_perso.get_curCarte().haveMobFix()) {
									SocketManager.GAME_SEND_MESSAGE(_perso, "Votre escouade ne peux pas vous rejoindre pendant un donjon.", Colors.RED);
									break;
								}
								if (_perso.get_fight() != null){
									SocketManager.GAME_SEND_MESSAGE(_perso, "Votre escouade ne peux pas vous rejoindre pendant un combat.", Colors.RED);
									break;
								}
								if (_perso.get_curExchange() != null){
									SocketManager.GAME_SEND_MESSAGE(_perso, "Votre escouade ne peux pas vous rejoindre pendant que vous êtes occupé.", Colors.RED);
									break;
								}
								if(_perso.isEsclave()){
									SocketManager.GAME_SEND_MESSAGE(_perso, "Vous ne pouvez pas donner cette ordre, vous n'êtes pas le chef.", Colors.RED);
									break;
								}
								if(_perso.get_maitre() != null){
									_perso.get_maitre().teleportAllEsclaves();
									SocketManager.GAME_SEND_MESSAGE(_perso, "<b>" + _perso.get_maitre().getEsclaves().size() + " membres </b> de votre escoude vous ont rejoins.", Colors.RED);
									useSuccess = true;
								} else {
									SocketManager.GAME_SEND_MESSAGE(_perso, "Vous n'avez pas d'escouade. Faites <b>." + command.getName() + " create </b> pour en créer une.", Colors.RED);
									break;
								}
							}
						} catch(Exception e){
							SocketManager.GAME_SEND_MESSAGE(_perso, "Faites <b>." + command.getName() + " create/delete/join</b>.", Colors.RED);
						}
						break;
					case 55: // Parchotage vs kamas
						if (_perso.get_fight() != null)
							break;
						
						int kamasPrice = Integer.parseInt(args);
						
						if (_perso.get_kamas() < kamasPrice){
							SocketManager.GAME_SEND_Im_PACKET(_perso, "1128;" + kamasPrice);
							break;
						}
						
						int nbreElemente = 0;

						if (_perso.get_baseStats().getEffect(125) < 101) {
							_perso.get_baseStats().addOneStat(125, 101 - _perso.get_baseStats().getEffect(125));
							nbreElemente++;
						}

						if (_perso.get_baseStats().getEffect(124) < 101) {
							_perso.get_baseStats().addOneStat(124, 101 - _perso.get_baseStats().getEffect(124));
							nbreElemente++;
						}

						if (_perso.get_baseStats().getEffect(118) < 101) {
							_perso.get_baseStats().addOneStat(118, 101 - _perso.get_baseStats().getEffect(118));
							if (nbreElemente == 0)
								nbreElemente++;
						}

						if (_perso.get_baseStats().getEffect(126) < 101) {
							_perso.get_baseStats().addOneStat(126, 101 - _perso.get_baseStats().getEffect(126));
							if (nbreElemente == 0)
								nbreElemente++;
						}

						if (_perso.get_baseStats().getEffect(119) < 101) {
							_perso.get_baseStats().addOneStat(119, 101 - _perso.get_baseStats().getEffect(119));
							if (nbreElemente == 0)
								nbreElemente++;
						}

						if (_perso.get_baseStats().getEffect(123) < 101) {
							_perso.get_baseStats().addOneStat(123, 101 - _perso.get_baseStats().getEffect(123));
							if (nbreElemente == 0)
								nbreElemente++;
						}

						if (nbreElemente == 0) {
							SocketManager.GAME_SEND_MESSAGE(_perso, "Vous êtes déjà parchotté dans tout les éléments !", Colors.RED);
							break;
						} else {
							_perso.set_kamas(_perso.get_kamas() - kamasPrice);
							SocketManager.GAME_SEND_STATS_PACKET(_perso);
							SocketManager.GAME_SEND_MESSAGE(_perso, "Vous êtes parchotté dans tout les éléments !",Colors.RED);
							SocketManager.GAME_SEND_Im_PACKET(_perso, "046;" + kamasPrice);
							useSuccess = true;
						}
						break;
					default:
						Logs.addToDebug("ActionID " + command.getFunction().split(";") + " non existant");
						break;
					}
				}
				if(useSuccess && price > 0) {
					diff = (points - price);
					Utils.updatePointsByAccount(_perso.get_compte(), diff);
					int newpoint = Utils.loadPointsByAccount(_perso.get_compte());
					SocketManager.GAME_SEND_MESSAGE(_perso, "Vous venez de perdre " + price + " points boutiques !<br />Il vous reste " + newpoint + " points.", Colors.RED);
				}
				SQLManager.SAVE_PERSONNAGE(_perso, true);
				return true;
			} else {
				SocketManager.GAME_SEND_MESSAGE(_perso, "Commande non reconnue ou incomplète !",
						Colors.RED);
				return true;
			}
		}
		return false;
	}
}

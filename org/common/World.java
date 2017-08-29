package org.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.client.Accounts;
import org.client.Characters;
import org.client.Characters.Stats;
import org.fight.object.Collector;
import org.fight.object.Monster;
import org.fight.object.Prism;
import org.kernel.Config;
import org.kernel.Console;
import org.kernel.Logs;
import org.login.server.Client;
import org.login.server.Client.Status;
import org.kernel.Console.Color;
import org.object.AuctionHouse;
import org.object.Guild;
import org.object.Houses;
import org.object.Hustle;
import org.object.Maps;
import org.object.Mount;
import org.object.NpcTemplates;
import org.object.Objects;
import org.object.SoulStone;
import org.object.Trunk;
import org.object.AuctionHouse.HdvEntry;
import org.object.NpcTemplates.NPC_question;
import org.object.NpcTemplates.NPC_reponse;
import org.object.Objects.ObjTemplate;
import org.object.job.Job;
import org.object.job.Job.StatsMetier;
import org.spell.Spell;

import java.util.TreeMap;

public class World {
	private static Map<Integer, Accounts> Comptes = new TreeMap<Integer, Accounts>();
	private static ArrayList<BanIp> Banips = new ArrayList<BanIp>();
	private static Map<String, Integer> ComptebyName = new TreeMap<String, Integer>();
	private static StringBuilder Challenges = new StringBuilder();
	private static Map<Integer, Characters> Persos = new TreeMap<Integer, Characters>();
	private static Map<Short, Maps> Cartes = new TreeMap<Short, Maps>();
	private static Map<Integer, Objects> Objets = new TreeMap<Integer, Objects>();
	private static Map<Integer, ExpLevel> ExpLevels = new TreeMap<Integer, ExpLevel>();
	private static Map<Integer, Spell> Sorts = new TreeMap<Integer, Spell>();
	private static Map<Integer, ObjTemplate> ObjTemplates = new TreeMap<Integer, ObjTemplate>();
	private static Map<Integer, Monster> MobTemplates = new TreeMap<Integer, Monster>();
	private static Map<Integer, NpcTemplates> NPCTemplates = new TreeMap<Integer, NpcTemplates>();
	private static Map<Integer, NPC_question> NPCQuestions = new TreeMap<Integer, NPC_question>();
	private static Map<Integer, NPC_reponse> NPCReponses = new TreeMap<Integer, NPC_reponse>();
	private static Map<Integer, IOTemplate> IOTemplate = new TreeMap<Integer, IOTemplate>();
	private static Map<Integer, Mount> Dragodindes = new TreeMap<Integer, Mount>();
	private static Map<Integer, SuperArea> SuperAreas = new TreeMap<Integer, SuperArea>();
	private static Map<Integer, Area> Areas = new TreeMap<Integer, Area>();
	private static Map<Integer, SubArea> SubAreas = new TreeMap<Integer, SubArea>();
	private static Map<Integer, Job> Job = new TreeMap<Integer, Job>();
	private static Map<Integer, ArrayList<Couple<Integer, Integer>>> Crafts = new TreeMap<Integer, ArrayList<Couple<Integer, Integer>>>();
	private static Map<Integer, ItemSet> ItemSets = new TreeMap<Integer, ItemSet>();
	private static Map<Integer, Guild> Guildes = new TreeMap<Integer, Guild>();
	private static Map<Integer, AuctionHouse> Hdvs = new TreeMap<Integer, AuctionHouse>();
	private static Map<Integer, Map<Integer, ArrayList<HdvEntry>>> _hdvsItems = new HashMap<Integer, Map<Integer, ArrayList<HdvEntry>>>(); // Contient
																																			// tout
																																			// les
																																			// items
																																			// en
																																			// ventes
																																			// des
																																			// comptes
																																			// dans
																																			// le
																																			// format<compteID,<hdvID,items<>>>
	private static Map<Integer, Characters> Married = new TreeMap<Integer, Characters>();
	private static Map<Integer, Hustle> Animations = new TreeMap<Integer, Hustle>();
	private static Map<Short, Maps.MountPark> MountPark = new TreeMap<Short, Maps.MountPark>();
	private static Map<Integer, Trunk> Trunks = new TreeMap<Integer, Trunk>();
	private static Map<Integer, Collector> Percepteurs = new TreeMap<Integer, Collector>();
	private static Map<Integer, Houses> House = new TreeMap<Integer, Houses>();
	private static Map<Short, Collection<Integer>> Seller = new TreeMap<Short, Collection<Integer>>();
	public static Map<Integer, StatsMetier> upAlchi = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upBrico = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upCM = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upB = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upFE = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upSA = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upFM = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upCo = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upBi = new TreeMap<Integer, StatsMetier>(); // Bijou
	public static Map<Integer, StatsMetier> upFD = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upSB = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upSBg = new TreeMap<Integer, StatsMetier>(); // S // baguette
	public static Map<Integer, StatsMetier> upFP = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upM = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upBou = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upT = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upP = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upFH = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upFPc = new TreeMap<Integer, StatsMetier>(); // Pï¿½cheurman...
																							// 42..
																							// Sydney...
																							// avenue
																							// Walaby
	public static Map<Integer, StatsMetier> upC = new TreeMap<Integer, StatsMetier>();// Chasseurs
	public static Map<Integer, StatsMetier> upFMD = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upFME = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upFMM = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upFMP = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upFMH = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upSMA = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upSMB = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upSMBg = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upBouc = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upPO = new TreeMap<Integer, StatsMetier>(); // Poissonniers.
	public static Map<Integer, StatsMetier> upFBou = new TreeMap<Integer, StatsMetier>();
	public static Map<Integer, StatsMetier> upJM = new TreeMap<Integer, StatsMetier>(); // Joaillo
	public static Map<Integer, StatsMetier> upCRM = new TreeMap<Integer, StatsMetier>();// Cordomages
	public static ArrayList<Short> restrictedMaps = null;
	private static Map<Integer, Prism> Prismes = new TreeMap<Integer, Prism>(); // Prismes
	public final static HashMap<String, Maps> cartesByPos = new HashMap<String, Maps>();
	private static java.util.Map<String, java.util.Map<String, String>> mobGroupFix = new HashMap<String, java.util.Map<String, String>>();

	private static int nextHdvID; // Contient le derniere ID utilisï¿½ pour
									// crï¿½e un HDV, pour obtenir un ID non
									// utilisï¿½ il faut impï¿½rativement
									// l'incrï¿½menter
	private static int nextLigneID; // Contient le derniere ID utilisï¿½ pour
									// crï¿½e une ligne dans un HDV

	private static int saveTry = 1;
	// Statut du serveur 1: accesible; 0: inaccesible; 2: sauvegarde
	private static short _state = 1;

	private static byte _GmAccess = 0;

	private static int nextObjetID; // Contient le derniere ID utilisï¿½ pour crï¿½e un Objet

	// Quest
	private static Map<Integer, Map<String, String>> quests = new HashMap<Integer, Map<String, String>>();
	private static Map<Integer, Map<String, String>> questSteps = new HashMap<Integer, Map<String, String>>();
	private static Map<Integer, Map<String, String>> questObjetives = new HashMap<Integer, Map<String, String>>();

	public static class Drop {
		private int _itemID;
		private int _prosp;
		private float _taux;
		private int _max;

		public Drop(int itm, int p, float t, int m) {
			_itemID = itm;
			_prosp = p;
			_taux = t;
			_max = m;
		}

		public void setMax(int m) {
			_max = m;
		}

		public int get_itemID() {
			return _itemID;
		}

		public int getMinProsp() {
			return _prosp;
		}

		public float get_taux() {
			return _taux;
		}

		public int get_max() {
			return _max;
		}
	}

	public static class ItemSet {
		private int _id;
		private ArrayList<ObjTemplate> _itemTemplates = new ArrayList<ObjTemplate>();
		private ArrayList<Stats> _bonuses = new ArrayList<Stats>();

		public ItemSet(int id, String items, String bonuses) {
			_id = id;
			// parse items String
			for (String str : items.split(",")) {
				try {
					ObjTemplate t = World.getObjTemplate(Integer.parseInt(str.trim()));
					if (t == null)
						continue;
					_itemTemplates.add(t);
				} catch (Exception e) {
				}
				;
			}

			// on ajoute un bonus vide pour 1 item
			_bonuses.add(new Stats());
			// parse bonuses String
			for (String str : bonuses.split(";")) {
				Stats S = new Stats();
				// sï¿½paration des bonus pour un mï¿½me nombre d'item
				for (String str2 : str.split(",")) {
					try {
						String[] infos = str2.split(":");
						int stat = Integer.parseInt(infos[0]);
						int value = Integer.parseInt(infos[1]);
						// on ajoute a la stat
						S.addOneStat(stat, value);
					} catch (Exception e) {
					}
					;
				}
				// on ajoute la stat a la liste des bonus
				_bonuses.add(S);
			}
		}

		public int getId() {
			return _id;
		}

		public Stats getBonusStatByItemNumb(int numb) {
			if (numb > _bonuses.size())
				return new Stats();
			return _bonuses.get(numb - 1);
		}

		public ArrayList<ObjTemplate> getItemTemplates() {
			return _itemTemplates;
		}
	}

	public static class Area {
		private int _id;
		private SuperArea _superArea;
		private String _nom;
		private ArrayList<SubArea> _subAreas = new ArrayList<SubArea>();
		private int _alignement;
		public static int _bontas = 0;
		public static int _brakmars = 0;
		private int _Prisme = 0;

		public Area(int id, int superArea, String nom, int alignement, int Prisme) {
			_id = id;
			_nom = nom;
			_superArea = World.getSuperArea(superArea);
			if (_superArea == null) {
				_superArea = new SuperArea(superArea);
				World.addSuperArea(_superArea);
			}
			_alignement = 0;
			_Prisme = Prisme;
			if (World.getPrisme(Prisme) != null) {
				_alignement = alignement;
				_Prisme = Prisme;
			}
			if (_alignement == 1)
				_bontas++;
			else if (_alignement == 2)
				_brakmars++;
		}

		public static int subareasBontas() {
			return _bontas;
		}

		public static int subareasBrakmars() {
			return _brakmars;
		}

		public int getalignement() {
			return _alignement;
		}

		public int getPrismeID() {
			return _Prisme;
		}

		public void setPrismeID(int Prisme) {
			_Prisme = Prisme;
		}

		public void setalignement(int alignement) {
			if (_alignement == 1 && alignement == -1)
				_bontas--;
			else if (_alignement == 2 && alignement == -1)
				_brakmars--;
			else if (_alignement == -1 && alignement == 1)
				_bontas++;
			else if (_alignement == -1 && alignement == 2)
				_brakmars++;
			_alignement = alignement;
		}

		public String getnom() {
			return _nom;
		}

		public int getID() {
			return _id;
		}

		public SuperArea getSuperArea() {
			return _superArea;
		}

		public void addSubArea(SubArea sa) {
			_subAreas.add(sa);
		}

		public ArrayList<SubArea> getSubAreas() {
			return _subAreas;
		}

		public ArrayList<Maps> getMaps() {
			ArrayList<Maps> maps = new ArrayList<Maps>();
			for (SubArea SA : _subAreas)
				maps.addAll(SA.getCartes());
			return maps;
		}
	}

	public static class SubArea {
		private int _id;
		private Area _area;
		private int _alignement;
		private String _nom;
		private boolean _subscribeNeed;
		private ArrayList<Maps> _Cartes = new ArrayList<Maps>();
		private boolean _canConquest;
		private int _Prisme;
		public static int _bontas = 0;
		public static int _brakmars = 0;

		public SubArea(int id, int areaID, int alignement, String nom, int conquistable, int Prisme,
				boolean subscribe) {
			_id = id;
			_nom = nom;
			_area = World.getArea(areaID);
			_alignement = alignement;
			_subscribeNeed = subscribe;
			_canConquest = conquistable == 0;
			_Prisme = Prisme;
			if (World.getPrisme(Prisme) != null) {
				_alignement = alignement;
				_Prisme = Prisme;
			}
			if (_alignement == 1)
				_bontas++;
			else if (_alignement == 2)
				_brakmars++;
		}

		public String getnom() {
			return _nom;
		}

		public int getPrismeID() {
			return _Prisme;
		}

		public void setPrismeID(int Prisme) {
			_Prisme = Prisme;
		}

		public boolean getConquistable() {
			return _canConquest;
		}

		public int getID() {
			return _id;
		}

		public Area getArea() {
			return _area;
		}

		public int getalignement() {
			return _alignement;
		}

		public void setalignement(int alignement) {
			if (_alignement == 1 && alignement == -1)
				_bontas--;
			else if (_alignement == 2 && alignement == -1)
				_brakmars--;
			else if (_alignement == -1 && alignement == 1)
				_bontas++;
			else if (_alignement == -1 && alignement == 2)
				_brakmars++;
			_alignement = alignement;
		}

		public ArrayList<Maps> getCartes() {
			return _Cartes;
		}

		public void addCarte(Maps Carte) {
			_Cartes.add(Carte);
		}

		public boolean get_subscribe() {
			return _subscribeNeed;
		}

		public static int subareasBontas() {
			return _bontas;
		}

		public static int subareasBrakmars() {
			return _brakmars;
		}
	}

	public static class SuperArea {
		private int _id;
		private ArrayList<Area> _areas = new ArrayList<Area>();

		public SuperArea(int a_id) {
			_id = a_id;
		}

		public void addArea(Area A) {
			_areas.add(A);
		}

		public int getID() {
			return _id;
		}
	}

	public static class Couple<L, R> {
		public L first;
		public R second;

		public Couple(L s, R i) {
			this.first = s;
			this.second = i;
		}
	}

	public static class IOTemplate {
		private int _id;
		private int _respawnTime;
		private int _duration;
		private int _unk;
		private boolean _walkable;

		public IOTemplate(int a_i, int a_r, int a_d, int a_u, boolean a_w) {
			_id = a_i;
			_respawnTime = a_r;
			_duration = a_d;
			_unk = a_u;
			_walkable = a_w;
		}

		public int getId() {
			return _id;
		}

		public boolean isWalkable() {
			return _walkable;
		}

		public int getRespawnTime() {
			return _respawnTime;
		}

		public int getDuration() {
			return _duration;
		}

		public int getUnk() {
			return _unk;
		}
	}

	public static class Exchange {
		private Characters perso1;
		private Characters perso2;
		private long kamas1 = 0;
		private long kamas2 = 0;
		private ArrayList<Couple<Integer, Integer>> items1 = new ArrayList<Couple<Integer, Integer>>();
		private ArrayList<Couple<Integer, Integer>> items2 = new ArrayList<Couple<Integer, Integer>>();
		private boolean ok1;
		private boolean ok2;

		public Exchange(Characters p1, Characters p2) {
			perso1 = p1;
			perso2 = p2;
		}

		synchronized public long getKamas(int guid) {
			int i = 0;
			if (perso1.get_GUID() == guid)
				i = 1;
			else if (perso2.get_GUID() == guid)
				i = 2;

			if (i == 1)
				return kamas1;
			else if (i == 2)
				return kamas2;
			return 0;
		}

		synchronized public void toogleOK(int guid) {
			int i = 0;
			if (perso1.get_GUID() == guid)
				i = 1;
			else if (perso2.get_GUID() == guid)
				i = 2;

			if (i == 1) {
				ok1 = !ok1;
				SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameThread().get_out(), ok1, guid);
				SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameThread().get_out(), ok1, guid);
			} else if (i == 2) {
				ok2 = !ok2;
				SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameThread().get_out(), ok2, guid);
				SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameThread().get_out(), ok2, guid);
			} else
				return;

			if (ok1 && ok2)
				apply();
		}

		synchronized public void setKamas(int guid, long k) {
			ok1 = false;
			ok2 = false;

			int i = 0;
			if (perso1.get_GUID() == guid)
				i = 1;
			else if (perso2.get_GUID() == guid)
				i = 2;
			SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameThread().get_out(), ok1, perso1.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameThread().get_out(), ok1, perso1.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameThread().get_out(), ok2, perso2.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameThread().get_out(), ok2, perso2.get_GUID());
			if (k < 0)
				return;
			if (i == 1) {
				kamas1 = k;
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'G', "", k + "");
				SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2.get_compte().getGameThread().get_out(), 'G', "",
						k + "");
			} else if (i == 2) {
				kamas2 = k;
				SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.get_compte().getGameThread().get_out(), 'G', "",
						k + "");
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'G', "", k + "");
			}
		}

		synchronized public void cancel() {
			if (perso1.get_compte() != null)
				if (perso1.get_compte().getGameThread() != null)
					SocketManager.GAME_SEND_EV_PACKET(perso1.get_compte().getGameThread().get_out());
			if (perso2.get_compte() != null)
				if (perso2.get_compte().getGameThread() != null)
					SocketManager.GAME_SEND_EV_PACKET(perso2.get_compte().getGameThread().get_out());
			perso1.set_isTradingWith(0);
			perso2.set_isTradingWith(0);
			perso1.setCurExchange(null);
			perso2.setCurExchange(null);
		}

		synchronized public void apply() {
			// Gestion des Kamas
			perso1.addKamas((-kamas1 + kamas2));
			perso2.addKamas((-kamas2 + kamas1));
			for (Couple<Integer, Integer> couple : items1) {
				if (couple.second == 0)
					continue;
				if (!perso1.hasItemGuid(couple.first))// Si le perso n'a pas l'item (Ne devrait pas arriver)
				{
					couple.second = 0;// On met la quantitï¿½ a 0 pour ï¿½viter les problemes
					continue;
				}
				Objects obj = World.getObjet(couple.first);
				if ((obj.getQuantity() - couple.second) < 1)// S'il ne reste plus d'item apres l'ï¿½change
				{
					perso1.removeItem(couple.first);
					couple.second = obj.getQuantity();
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(perso1, couple.first);
					if (!perso2.addObjet(obj, true))// Si le joueur avait un item similaire
						World.removeItem(couple.first);// On supprime l'item inutile
				} else {
					obj.setQuantity(obj.getQuantity() - couple.second);
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(perso1, obj);
					Objects newObj = Objects.getCloneObjet(obj, couple.second);
					if (perso2.addObjet(newObj, true))// Si le joueur n'avait
														// pas d'item similaire
						World.addObjet(newObj, true);// On ajoute l'item au
														// World
				}
			}
			for (Couple<Integer, Integer> couple : items2) {
				if (couple.second == 0)
					continue;
				if (!perso2.hasItemGuid(couple.first))// Si le perso n'a pas
														// l'item (Ne devrait
														// pas arriver)
				{
					couple.second = 0;// On met la quantitï¿½ a 0 pour ï¿½viter
										// les problemes
					continue;
				}
				Objects obj = World.getObjet(couple.first);
				if ((obj.getQuantity() - couple.second) < 1)// S'il ne reste
															// plus d'item apres
															// l'ï¿½change
				{
					perso2.removeItem(couple.first);
					couple.second = obj.getQuantity();
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(perso2, couple.first);
					if (!perso1.addObjet(obj, true))// Si le joueur avait un
													// item similaire
						World.removeItem(couple.first);// On supprime l'item
														// inutile
				} else {
					obj.setQuantity(obj.getQuantity() - couple.second);
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(perso2, obj);
					Objects newObj = Objects.getCloneObjet(obj, couple.second);
					if (perso1.addObjet(newObj, true))// Si le joueur n'avait
														// pas d'item similaire
						World.addObjet(newObj, true);// On ajoute l'item au
														// World
				}
			}
			// Fin
			perso1.set_isTradingWith(0);
			perso2.set_isTradingWith(0);
			perso1.setCurExchange(null);
			perso2.setCurExchange(null);
			SocketManager.GAME_SEND_Ow_PACKET(perso1);
			SocketManager.GAME_SEND_Ow_PACKET(perso2);
			SocketManager.GAME_SEND_STATS_PACKET(perso1);
			SocketManager.GAME_SEND_STATS_PACKET(perso2);
			SocketManager.GAME_SEND_EXCHANGE_VALID(perso1.get_compte().getGameThread().get_out(), 'a');
			SocketManager.GAME_SEND_EXCHANGE_VALID(perso2.get_compte().getGameThread().get_out(), 'a');
			SQLManager.SAVE_PERSONNAGE(perso1, true);
			SQLManager.SAVE_PERSONNAGE(perso2, true);
		}

		synchronized public void addItem(int guid, int qua, int pguid) {
			ok1 = false;
			ok2 = false;

			Objects obj = World.getObjet(guid);
			int i = 0;

			if (perso1.get_GUID() == pguid)
				i = 1;
			if (perso2.get_GUID() == pguid)
				i = 2;

			if (qua == 1)
				qua = 1;
			String str = guid + "|" + qua;
			if (obj == null)
				return;
			String add = "|" + obj.getTemplate().getID() + "|" + obj.parseStatsString();
			SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameThread().get_out(), ok1, perso1.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameThread().get_out(), ok1, perso1.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameThread().get_out(), ok2, perso2.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameThread().get_out(), ok2, perso2.get_GUID());
			if (i == 1) {
				Couple<Integer, Integer> couple = getCoupleInList(items1, guid);
				if (couple != null) {
					couple.second += qua;
					SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'O', "+", "" + guid + "|" + couple.second);
					// FIXME :: FAILLE GENERATION ITEM
					SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2.get_compte().getGameThread().get_out(), 'O',
							"+", "" + guid + "|" + couple.second + add);
					System.out.println(couple.second);
					System.out.println(couple.first);
					return;
				}
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'O', "+", str);
				/// FIXME :: FAILLE GENERATION ITEM
				SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2.get_compte().getGameThread().get_out(), 'O', "+",
						str + add);
				System.out.println("str = " + str);
				System.out.println("add = " + add);
				items1.add(new Couple<Integer, Integer>(guid, qua));
			} else if (i == 2) {
				Couple<Integer, Integer> couple = getCoupleInList(items2, guid);
				if (couple != null) {
					couple.second += qua;
					SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'O', "+", "" + guid + "|" + couple.second);
					// FIXME :: FAILLE GENERATION ITEM
					SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.get_compte().getGameThread().get_out(), 'O',
							"+", "" + guid + "|" + couple.second + add);
					System.out.println(couple.second);
					System.out.println(couple.first);
					return;
				}
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'O', "+", str);
				SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.get_compte().getGameThread().get_out(), 'O', "+",
						str + add);
				System.out.println("str2 = " + str);
				System.out.println("add2 = " + add);
				items2.add(new Couple<Integer, Integer>(guid, qua));
			}
		}

		synchronized public void removeItem(int guid, int qua, int pguid) {
			int i = 0;
			if (perso1.get_GUID() == pguid)
				i = 1;
			else if (perso2.get_GUID() == pguid)
				i = 2;
			ok1 = false;
			ok2 = false;

			SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameThread().get_out(), ok1, perso1.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameThread().get_out(), ok1, perso1.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameThread().get_out(), ok2, perso2.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameThread().get_out(), ok2, perso2.get_GUID());

			Objects obj = World.getObjet(guid);
			if (obj == null)
				return;
			String add = "|" + obj.getTemplate().getID() + "|" + obj.parseStatsString();
			if (i == 1) {
				Couple<Integer, Integer> couple = getCoupleInList(items1, guid);
				int newQua = couple.second - qua;
				if (newQua < 1)// Si il n'y a pu d'item
				{
					items1.remove(couple);
					SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'O', "-", "" + guid);
					SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2.get_compte().getGameThread().get_out(), 'O',
							"-", "" + guid);
				} else {
					couple.second = newQua;
					SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'O', "+", "" + guid + "|" + newQua);
					SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2.get_compte().getGameThread().get_out(), 'O',
							"+", "" + guid + "|" + newQua + add);
				}
			} else if (i == 2) {
				Couple<Integer, Integer> couple = getCoupleInList(items2, guid);
				int newQua = couple.second - qua;

				if (newQua < 1)// Si il n'y a pu d'item
				{
					items2.remove(couple);
					SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.get_compte().getGameThread().get_out(), 'O',
							"-", "" + guid);
					SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'O', "-", "" + guid);
				} else {
					couple.second = newQua;
					SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.get_compte().getGameThread().get_out(), 'O',
							"+", "" + guid + "|" + newQua + add);
					SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'O', "+", "" + guid + "|" + newQua);
				}
			}
		}

		synchronized private Couple<Integer, Integer> getCoupleInList(ArrayList<Couple<Integer, Integer>> items,
				int guid) {
			for (Couple<Integer, Integer> couple : items) {
				if (couple.first == guid)
					return couple;
			}
			return null;
		}

		public synchronized int getQuaItem(int itemID, int playerGuid) {
			ArrayList<Couple<Integer, Integer>> items;
			if (perso1.get_GUID() == playerGuid)
				items = items1;
			else
				items = items2;

			for (Couple<Integer, Integer> curCoupl : items) {
				if (curCoupl.first == itemID) {
					return curCoupl.second;
				}
			}

			return 0;
		}

	}

	public static class ExpLevel {
		public long perso;
		public int metier;
		public int dinde;
		public int pvp;
		public long guilde;

		public ExpLevel(long c, int m, int d, int p) {
			perso = c;
			metier = m;
			dinde = d;
			pvp = p;
			guilde = perso * 10;
		}

	}

	public static void createWorld() {

		Console.bright();
		Console.println("\n <> Load the static datas <>", Color.YELLOW);

		Console.print("\n");
		SQLManager.LOAD_EXP();
		Console.print("\n");
		SQLManager.LOAD_COMMANDS();
		Console.print("\n");
		SQLManager.LOAD_GMCOMMANDS();
		Console.print("\n");
		SQLManager.LOAD_SORTS();
		Console.print("\n");
		SQLManager.LOAD_MOB_TEMPLATE();
		Console.print("\n");
		SQLManager.LOAD_OBJ_TEMPLATE();
		Console.print("\n");
		SQLManager.LOAD_NPC_TEMPLATE();
		Console.print("\n");
		SQLManager.LOAD_NPC_QUESTIONS();
		Console.print("\n");
		SQLManager.LOAD_NPC_ANSWERS();
		Console.print("\n");
		SQLManager.LOAD_PRISMES();
		Console.print("\n");
		SQLManager.LOAD_AREA();
		Console.print("\n");
		SQLManager.LOAD_SUBAREA();
		Console.print("\n");
		SQLManager.LOAD_IOTEMPLATE();
		Console.print("\n");
		SQLManager.LOAD_ITEMSETS();
		Console.print("\n");
		SQLManager.LOAD_MAPS();
		Console.print("\n");
		SQLManager.LOAD_CRAFTS();
		Console.print("\n");
		SQLManager.LOAD_JOBS();
		Console.print("\n");
		SQLManager.LOAD_TRIGGERS();
		Console.print("\n");
		SQLManager.LOAD_ENDFIGHT_ACTIONS();
		Console.print("\n");
		SQLManager.LOAD_NPCS();
		Console.print("\n");
		SQLManager.LOAD_ITEM_ACTIONS();
		Console.print("\n");
		SQLManager.LOAD_DROPS();
		Console.print("\n");
		SQLManager.LOAD_ANIMATIONS();
		Console.print("\r\n\n\n");
		Console.bright();
		Console.println(" <> Load the dynamic datas <>", Color.YELLOW);

		Console.print("\n");
		SQLManager.LOGGED_ZERO();
		Console.print("\n");
		SQLManager.LOAD_MOUNTS();
		Console.print("\n");
		SQLManager.LOAD_ITEMS_FULL();
		Console.print("\n");
		SQLManager.LOAD_COMPTES();
		Console.print("\n");
		SQLManager.LOAD_GUILDS();
		Console.print("\n");
		SQLManager.LOAD_GUILD_MEMBERS();
		Console.print("\n");
		SQLManager.LOAD_PERSOS();
		Console.print("\n");
		SQLManager.LOAD_CHALLENGES();
		Console.print("\n");
		SQLManager.LOAD_MOUNTPARKS();
		Console.print("\n");
		SQLManager.LOAD_PERCEPTEURS();
		Console.print("\n");
		SQLManager.LOAD_HOUSES();
		Console.print("\n");
		SQLManager.LOAD_TRUNK();
		Console.print("\n");
		SQLManager.LOAD_ZAAPS();
		Console.print("\n");
		SQLManager.LOAD_ARENA_TEAM();
		Console.print("\n");
		SQLManager.LOAD_RAPIDSTUFFS();
		Console.print("\n");
		SQLManager.LOAD_ZAAPIS();
		Console.print("\n");
		SQLManager.LOAD_BANIP();
		Console.print("\n");
		SQLManager.LOAD_HDVS();
		Console.print("\n");
		SQLManager.LOAD_HDVS_ITEMS();
		Console.print("\n");
		SQLManager.LOAD_QUESTS();
		Console.print("\n");
		SQLManager.LOAD_QUEST_STEPS();
		Console.print("\n");
		SQLManager.LOAD_QUEST_OBJECTIVES();
		Console.print("\n");
		SQLManager.RESET_MOUNTPARKS();
		nextObjetID = SQLManager.getNextObjetID();
	}

	public static Area getArea(int areaID) {
		return getAreas().get(areaID);
	}

	public static SuperArea getSuperArea(int areaID) {
		return getSuperAreas().get(areaID);
	}

	public static SubArea getSubArea(int areaID) {
		return getSubAreas().get(areaID);
	}

	public static void addArea(Area area) {
		getAreas().put(area.getID(), area);
	}

	public static void addSuperArea(SuperArea SA) {
		getSuperAreas().put(SA.getID(), SA);
	}

	public static void addSubArea(SubArea SA) {
		getSubAreas().put(SA.getID(), SA);
	}

	public static void addNPCreponse(NPC_reponse rep) {
		getNPCReponses().put(rep.get_id(), rep);
	}

	public static NPC_reponse getNPCreponse(int guid) {
		return getNPCReponses().get(guid);
	}

	public static int getExpLevelSize() {
		return getExpLevels().size();
	}

	public static void addExpLevel(int lvl, ExpLevel exp) {
		getExpLevels().put(lvl, exp);
	}

	public static Accounts getCompte(int guid) {
		return getComptes().get(guid);
	}

	public static void addNPCQuestion(NPC_question quest) {
		NPCQuestions.put(quest.get_id(), quest);
	}
	// quests

	public static NPC_question getNPCQuestion(int guid, Characters perso) {
		NPC_question baseQuestion = NPCQuestions.get(guid);

		Entry<Integer, Map<String, String>> questObjective = getObjectiveByOptQuestion(guid);
		if (questObjective != null && perso.hasObjective(questObjective.getKey())) // Il
																					// y
																					// a
																					// un
																					// objectif
																					// de
																					// quete
																					// avec
																					// cette
																					// question
		{
			NPC_question questQuestion = new NPC_question(guid, questObjective.getValue().get("optAnswer"), "", "", 0);
			return questQuestion;
		}

		return baseQuestion;
	}

	public static NpcTemplates getNPCTemplate(int guid) {
		return getNPCTemplates().get(guid);
	}

	public static void addNpcTemplate(NpcTemplates temp) {
		getNPCTemplates().put(temp.get_id(), temp);
	}

	public static Maps getCarte(short id) {
		return getCartes().get(id);
	}

	public static void addCarte(Maps map) {
		if (!getCartes().containsKey(Short.valueOf(map.get_id())))
			getCartes().put(Short.valueOf(map.get_id()), map);
		cartesByPos.put(map.getX() + "," + map.getY() + "," + map.getSubArea().getArea().getSuperArea().getID(), map);
	}

	public static void delCarte(Maps map) {
		if (getCartes().containsKey(map.get_id()))
			getCartes().remove(map.get_id());
	}

	public static Accounts getCompteByName(String name) {
		return (getComptebyName().get(name.toLowerCase()) != null
				? getComptes().get(getComptebyName().get(name.toLowerCase())) : null);
	}

	public static Characters getPersonnage(int guid) {
		return getPersos().get(guid);
	}

	public static void addAccount(Accounts compte) {
		getComptes().put(compte.get_GUID(), compte);
		getComptebyName().put(compte.get_name().toLowerCase(), compte.get_GUID());
	}

	public static void addChallenge(String chal) {
		// ChalID,gainXP,gainDrop,gainParMob,Conditions;...
		if (!getChallenges().toString().isEmpty())
			getChallenges().append(";");
		getChallenges().append(chal);
	}

	public static String getChallengeFromConditions(boolean sevEnn, boolean sevAll, boolean bothSex, boolean EvenEnn,
			boolean MoreEnn, boolean hasCaw, boolean hasChaf, boolean hasRoul, boolean hasArak, boolean isBoss) {
		String noBossChals = ";2;5;9;17;19;24;38;47;50;"; // ceux impossibles
															// contre boss
		StringBuilder toReturn = new StringBuilder();
		boolean isFirst = true, isGood = false;
		int cond = 0;
		for (String chal : getChallenges().toString().split(";")) {
			if (!isFirst && isGood)
				toReturn.append(";");
			isGood = true;
			cond = Integer.parseInt(chal.split(",")[4]);
			// Nï¿½cessite plusieurs ennemis
			if (((cond & 1) == 1) && !sevEnn)
				isGood = false;
			// Nï¿½cessite plusieurs alliï¿½s
			if ((((cond >> 1) & 1) == 1) && !sevAll)
				isGood = false;
			// Nï¿½cessite les deux sexes
			if ((((cond >> 2) & 1) == 1) && !sevAll)
				isGood = false;
			// Nï¿½cessite un nombre pair d'ennemis
			if ((((cond >> 3) & 1) == 1) && !sevAll)
				isGood = false;
			// Nï¿½cessite plus d'ennemis que d'alliï¿½s
			if ((((cond >> 4) & 1) == 1) && !sevAll)
				isGood = false;
			// Jardinier
			if (!hasCaw && (Integer.parseInt(chal.split(",")[0]) == 7))
				isGood = false;
			// Fossoyeur
			if (!hasChaf && (Integer.parseInt(chal.split(",")[0]) == 12))
				isGood = false;
			// Casino Royal
			if (!hasRoul && (Integer.parseInt(chal.split(",")[0]) == 14))
				isGood = false;
			// Araknophile
			if (!hasArak && (Integer.parseInt(chal.split(",")[0]) == 15))
				isGood = false;
			// Contre un boss de donjon
			if (noBossChals.contains(";" + chal.split(",")[0] + ";"))
				isGood = false;
			if (isGood)
				toReturn.append(chal);
			isFirst = false;
		}
		return toReturn.toString();
	}

	public static ArrayList<String> getRandomChallenge(int nombreChal, String challenges) {
		String MovingChals = ";1;2;8;36;37;39;40;"; // Challenges de
													// dï¿½placements
													// incompatibles
		boolean hasMovingChal = false;
		String TargetChals = ";3;4;10;25;31;32;34;35;38;42;"; // ceux qui
																// ciblent
		boolean hasTargetChal = false;
		String SpellChals = ";5;6;9;11;19;20;24;41;"; // ceux qui obligent ï¿½
														// caster spï¿½cialement
		boolean hasSpellChal = false;
		String KillerChals = ";28;29;30;44;45;46;48;"; // ceux qui disent qui
														// doit tuer
		boolean hasKillerChal = false;
		String HealChals = ";18;43;"; // ceux qui empï¿½chent de soigner
		boolean hasHealChal = false;

		int compteur = 0, i = 0;
		ArrayList<String> toReturn = new ArrayList<String>();
		String chal = new String();
		while (compteur < 100 && toReturn.size() < nombreChal) {

			compteur++;
			i = Formulas.getRandomValue(1, challenges.split(";").length);
			chal = challenges.split(";")[i - 1]; // challenge au hasard dans la
													// liste

			if (!toReturn.contains(chal)) {// si le challenge n'y ï¿½tait pas
											// encore
				if (MovingChals.contains(";" + chal.split(",")[0] + ";")) // s'il
																			// appartient
																			// ï¿½
																			// une
																			// liste
					if (!hasMovingChal) { // et qu'aucun de la liste n'a ï¿½tï¿½
											// choisi dï¿½jï¿½
						hasMovingChal = true;
						toReturn.add(chal);
						continue;
					} else
						continue;
				if (TargetChals.contains(";" + chal.split(",")[0] + ";"))
					if (!hasTargetChal) {
						hasTargetChal = true;
						toReturn.add(chal);
						continue;
					} else
						continue;
				if (SpellChals.contains(";" + chal.split(",")[0] + ";"))
					if (!hasSpellChal) {
						hasSpellChal = true;
						toReturn.add(chal);
						continue;
					} else
						continue;
				if (KillerChals.contains(";" + chal.split(",")[0] + ";"))
					if (!hasKillerChal) {
						hasKillerChal = true;
						toReturn.add(chal);
						continue;
					} else
						continue;
				if (HealChals.contains(";" + chal.split(",")[0] + ";"))
					if (!hasHealChal) {
						hasHealChal = true;
						toReturn.add(chal);
						continue;
					} else
						continue;
				toReturn.add(chal); // s'il n'appartient ï¿½ aucune liste

			}
			compteur++;
		}
		// System.out.println(toReturn.toString());
		return toReturn;
	}

	public static void addAccountbyName(Accounts compte) {
		getComptebyName().put(compte.get_name(), compte.get_GUID());
	}

	public static void addPersonnage(Characters perso) {
		synchronized (getPersos()) {
			getPersos().put(perso.get_GUID(), perso);
		}
	}

	public static Characters getPersoByName(String name) {
		ArrayList<Characters> Ps = new ArrayList<Characters>();
		synchronized (getPersos()) {
			Ps.addAll(getPersos().values());
		}
		for (Characters P : Ps)
			if (P.get_name().equalsIgnoreCase(name))
				return P;
		return null;
	}

	public static void deletePerso(Characters perso) {
		if (perso.get_guild() != null) {
			if (perso.get_guild().getMembers().size() <= 1)// Il est tout seul dans la guilde : Supression
			{
				World.removeGuild(perso.get_guild().get_id());
			} else if (perso.getGuildMember().getRank() == 1)// On passe les pouvoir a celui qui a le plus de  droits si il est meneur
			{
				int curMaxRight = 0;
				Characters Meneur = null;
				for (Characters newMeneur : perso.get_guild().getMembers()) {
					if (newMeneur == perso)
						continue;
					if (newMeneur.getGuildMember().getRights() < curMaxRight) {
						Meneur = newMeneur;
					}
				}
				perso.get_guild().removeMember(perso);
				Meneur.getGuildMember().setRank(1);
			} else// Supression simple
			{
				perso.get_guild().removeMember(perso);
			}
		}
		perso.remove();// Supression BDD Perso, items, monture.
		World.unloadPerso(perso.get_GUID());// UnLoad du perso+item
	}

	public static String getSousZoneStateString() {
		 String str = "";
	        for (SubArea subarea : SubAreas.values()) {
	            if (!subarea.getConquistable())
	                continue;
	            str += "|" + subarea.getID() + ";" + subarea.getalignement();
	        }
	        return str;
	}

	public static long getPersoXpMin(int _lvl) {
		if (_lvl > getExpLevelSize())
			_lvl = getExpLevelSize();
		if (_lvl < 1)
			_lvl = 1;
		return getExpLevels().get(_lvl).perso;
	}

	public static long getPersoXpMax(int _lvl) {
		if (_lvl >= getExpLevelSize())
			_lvl = (getExpLevelSize() - 1);
		if (_lvl <= 1)
			_lvl = 1;
		return getExpLevels().get(_lvl + 1).perso;
	}

	public static void addSort(Spell sort) {
		getSorts().put(sort.getSpellID(), sort);
	}

	public static void addObjTemplate(ObjTemplate obj) {
		ObjTemplates.put(obj.getID(), obj);
	}

	public static Spell getSort(int id) {
		return getSorts().get(id);
	}

	public static ObjTemplate getObjTemplate(int id) {
		return ObjTemplates.get(id);
	}

	public synchronized static int getNewItemGuid() {
		return nextObjetID++;
	}

	public static void addMobTemplate(int id, Monster mob) {
		getMobTemplates().put(id, mob);
	}

	public static Monster getMonstre(int id) {
		return getMobTemplates().get(id);
	}

	public static int countPersoOnMap(short mapid) {
		Maps map = getCarte(mapid);
		if (map == null)
			return 0;
		if (map.getPersos() == null)
			return 0;
		return map.getPersos().size();
	}

	public static List<Characters> getOnlinePersos() {
		Map<Integer, Characters> persos = new TreeMap<Integer, Characters>();
		synchronized (getPersos()) {
			persos.putAll(getPersos());
		}
		List<Characters> online = new ArrayList<Characters>();
		for (Entry<Integer, Characters> perso : persos.entrySet()) {
			if (perso.getValue() != null && perso.getValue().isOnline() && perso.getValue().get_compte() != null
					&& perso.getValue().get_compte().getGameThread() != null) {
				if (perso.getValue().get_compte().getGameThread().get_out() != null) {
					online.add(perso.getValue());
				} else {
					World.verifyClone(perso.getValue());
				}
			}
		}
		return online;
	}

	public static void addObjet(Objects item, boolean saveSQL) {
		getObjets().put(item.getGuid(), item);
		if (saveSQL)
			SQLManager.SAVE_NEW_ITEM(item);
	}

	public static Objects getObjet(int guid) {
		return getObjets().get(guid);
	}

	public static void removeItem(int guid) {
		getObjets().remove(guid);
		SQLManager.DELETE_ITEM(guid);
	}

	public static void addIOTemplate(IOTemplate IOT) {
		IOTemplate.put(IOT.getId(), IOT);
	}

	public static Mount getDragoByID(int id) {
		return getDragodindes().get(id);
	}

	public static void addDragodinde(Mount DD) {
		getDragodindes().put(DD.get_id(), DD);
	}

	public static void removeDragodinde(int DID) {
		getDragodindes().remove(DID);
	}

	public static void saveAll(Characters saver) {
		try {
			Logs.addToSQLLog("Lancement de la sauvegarde du serveur.");
			set_state((short) 2);
			SocketManager.GAME_SEND_Im_PACKET_TO_ALL("1164");
			Config.isSaving = true;
			SQLManager.commitTransacts();
			SQLManager.TIMER(false);// Arrete le timer d'enregistrement SQL
			
			// sauvegarde des personnages
			for (Characters perso : getPersos().values()) {
				if (!perso.isOnline())
					continue;
				Thread.sleep(10); //0.1 sec. pour 1 objets
				SQLManager.SAVE_PERSONNAGE(perso, true);
			}

			//Thread.sleep(1000);

			for (Guild guilde : getGuildes().values()) {
				Thread.sleep(10);//0.1 sec. pour 1 guilde
				SQLManager.UPDATE_GUILD(guilde);
			}

			//Thread.sleep(1000);

			for (Collector perco : getPercepteurs().values()) {
				if (perco.get_inFight() > 0)
					continue;
				Thread.sleep(10);//0.1 sec. pour 1 percepteur
				SQLManager.UPDATE_PERCO(perco);
			}

			//Thread.sleep(1000);
			
			for (Prism Prisme : Prismes.values()) {
				boolean toDelete = true;
				for (SubArea subarea : getSubAreas().values()) {
					if (subarea.getPrismeID() == Prisme.getID())
						toDelete = false;
				}
				if (toDelete)
					SQLManager.DELETE_PRISME(Prisme.getID());
				else
					SQLManager.SAVE_PRISME(Prisme);
			}
			
			//Thread.sleep(1000);
			
			for (Houses house : getHouse().values()) {
				if (house.get_owner_id() > 0) {
					Thread.sleep(100); //0.1 sec. pour 1 maison
					SQLManager.UPDATE_HOUSE(house);
				}
			}

			//Thread.sleep(1000);

			for (Trunk t : getTrunks().values()) {
				if (t.get_owner_id() > 0) {
					Thread.sleep(10); //0.1 sec. pour 1 coffre
					SQLManager.UPDATE_TRUNK(t);
				}
			}

			//Thread.sleep(1000);

			for (Maps.MountPark mp : getMountPark().values()) {
				if (mp.get_owner() > 0 || mp.get_owner() == -1) {
					Thread.sleep(10); //0.1 sec. pour 1 enclo
					SQLManager.UPDATE_MOUNTPARK(mp);
				}
			}
			
			//Thread.sleep(1000);
			
			for (Area area : getAreas().values()) {
				SQLManager.UPDATE_AREA(area);
			}
			
			//Thread.sleep(1000);
			
			for (SubArea subarea : getSubAreas().values()) {
				SQLManager.UPDATE_SUBAREA(subarea);
			}
			
			//Thread.sleep(1000);

			ArrayList<HdvEntry> toSave = new ArrayList<HdvEntry>();
			for (AuctionHouse curHdv : getHdvs().values()) {
				Thread.sleep(10);
				toSave.addAll(curHdv.getAllEntry());
			}
			SQLManager.SAVE_HDVS_ITEMS(toSave);

			//Thread.sleep(1000);

		} catch (ConcurrentModificationException e) {
			if (saveTry < 10) {
				Logs.addToSQLLog("Nouvelle tentative de sauvegarde");
				saveTry++;
				saveAll(saver);
			} else {
				set_state((short) 1);
				Logs.addToSQLLog("Echec de la sauvegarde apres " + saveTry + " tentatives");
			}

		} catch (Exception e) {
			Logs.addToSQLLog("Erreur lors de la sauvegarde : " + e.getMessage());
			e.printStackTrace();
		} finally {
			SQLManager.commitTransacts();
			SQLManager.TIMER(true); // Redémarre le timer d'enregistrement SQL
			Config.isSaving = false;
			saveTry = 1;
		}
		set_state((short) 1);
		SocketManager.GAME_SEND_Im_PACKET_TO_ALL("1165");
		Logs.addToSQLLog("Sauvegarde du serveur effectue avec succes.");
	}

	public static void RefreshAllMob() {
		SocketManager.GAME_SEND_MESSAGE_TO_ALL("Recharge des monstres en cours, des latences peuvent survenir.",
				Config.CONFIG_MOTD_COLOR);
		for (Maps map : getCartes().values()) {
			map.refreshSpawns();
		}
		SocketManager.GAME_SEND_MESSAGE_TO_ALL("Recharge des monstres terminée. La prochaine recharge aura lieu dans 5 heures.",
				Config.CONFIG_MOTD_COLOR);
	}

	public static ExpLevel getExpLevel(int lvl) {
		return getExpLevels().get(lvl);
	}

	public static IOTemplate getIOTemplate(int id) {
		return IOTemplate.get(id);
	}

	public static Job getMetier(int id) {
		return getJob().get(id);
	}

	public static void addJob(Job metier) {
		getJob().put(metier.getId(), metier);
	}

	public static void addCraft(int id, ArrayList<Couple<Integer, Integer>> m) {
		getCrafts().put(id, m);
	}

	public static ArrayList<Couple<Integer, Integer>> getCraft(int i) {
		return getCrafts().get(i);
	}

	public static int getObjectByIngredientForJob(ArrayList<Integer> list, Map<Integer, Integer> ingredients) {
		if (list == null)
			return -1;
		for (int tID : list) {
			ArrayList<Couple<Integer, Integer>> craft = World.getCraft(tID);
			if (craft == null) {
				Logs.addToGameLog("Recette pour l'objet " + tID + " non existante");
				continue;
			}
			if (craft.size() != ingredients.size())
				continue;
			boolean ok = true;
			for (Couple<Integer, Integer> c : craft) {
				// si ingredient non prï¿½sent ou mauvaise quantitï¿½
				if (ingredients.get(c.first) != c.second)
					ok = false;
			}
			if (ok)
				return tID;
		}
		return -1;
	}

	public static Accounts getCompteByPseudo(String p) {
		for (Accounts C : getComptes().values())
			if (C.get_pseudo().equals(p))
				return C;
		return null;
	}

	public static void addItemSet(ItemSet itemSet) {
		getItemSets().put(itemSet.getId(), itemSet);
	}

	public static ItemSet getItemSet(int tID) {
		return getItemSets().get(tID);
	}

	public static int getItemSetNumber() {
		return getItemSets().size();
	}

	public static int getNextIdForMount() {
		int max = 1;
		for (int a : getDragodindes().keySet())
			if (a > max)
				max = a;
		return max + 1;
	}

	public static Maps getCarteByPosAndCont(int mapX, int mapY, int contID) {
		for (Maps map : getCartes().values()) {
			if (map.getX() == mapX && map.getY() == mapY && map.getSubArea().getArea().getSuperArea().getID() == contID)
				return map;
		}
		return null;
	}

	public synchronized static int getNextIDPrisme() {
		int max = 1;
		for (int a : Prismes.keySet())
			if (a > max)
				max = a;
		return max + 1;
	}

	public synchronized static void addPrisme(Prism Prisme) {
		Prismes.put(Prisme.getID(), Prisme);
	}

	public static Prism getPrisme(int id) {
		return Prismes.get(id);
	}

	public static void removePrisme(int id) {
		Prismes.remove(id);
	}

	public static Collection<Prism> AllPrisme() {
		if (Prismes.size() > 0)
			return Prismes.values();
		return null;
	}

	public static String PrismesGeoposition(int alignement) {
		String str = "";
		boolean first = false;
		int subareas = 0;
		for (SubArea subarea : getSubAreas().values()) {
			if (!subarea.getConquistable() 
					|| subarea.getalignement() == Constant.ALIGNEMENT_NEUTRE)
				continue;
			if (first)
				str += ";";
			str += subarea.getID() + "," + (subarea.getalignement() == Constant.ALIGNEMENT_NEUTRE ? -1 : subarea.getalignement()) + ",0,";
			if (World.getPrisme(subarea.getPrismeID()) == null)
				str += 0 + ",1";
			else
				str += (subarea.getPrismeID() == 0 ? 0 : World.getPrisme(subarea.getPrismeID()).getCarte()) + ",1";
			first = true;
			subareas++;
		}
		if (alignement == 1)
			str += "|" + Area._bontas;
		else if (alignement == 2)
			str += "|" + Area._brakmars;
		str += "|" + getAreas().size() + "|";
		first = false;
		for (Area area : getAreas().values()) {
			if (area.getalignement() == Constant.ALIGNEMENT_NEUTRE)
				continue;
			if (first)
				str += ";";
			str += area.getID() + "," + area.getalignement() + ",1," + (area.getPrismeID() == 0 ? 0 : 1);
			first = true;
		}
		if (alignement == 1)
			str = Area._bontas + "|" + subareas + "|" + (subareas - (SubArea._bontas + SubArea._brakmars)) + "|" + str;
		else if (alignement == 2)
			str = Area._brakmars + "|" + subareas + "|" + (subareas - (SubArea._brakmars + SubArea._bontas)) + "|" + str;
		return str;
	}

	public static void showPrismes(Characters perso) {
		for (SubArea subarea : getSubAreas().values()) {
			if (subarea.getalignement() == 0)
				continue;
			SocketManager.GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(perso,
					subarea.getID() + "|" + subarea.getalignement() + "|2");
		}
	}

	public static void addGuild(Guild g, boolean save) {
		getGuildes().put(g.get_id(), g);
		if (save)
			SQLManager.SAVE_NEWGUILD(g);
	}

	public static int getNextHighestGuildID() {
		if (getGuildes().isEmpty())
			return 1;
		int n = 0;
		for (int x : getGuildes().keySet())
			if (n < x)
				n = x;
		return n + 1;
	}

	public static boolean guildNameIsUsed(String name) {
		for (Guild g : getGuildes().values())
			if (g.get_name().equalsIgnoreCase(name))
				return true;
		return false;
	}

	public static boolean guildEmblemIsUsed(String emb) {
		for (Guild g : getGuildes().values()) {
			if (g.get_emblem().equals(emb))
				return true;
		}
		return false;
	}

	public static Guild getGuild(int i) {
		return getGuildes().get(i);
	}

	public static long getGuildXpMax(int _lvl) {
		if (_lvl >= 200)
			_lvl = 199;
		if (_lvl <= 1)
			_lvl = 1;
		return getExpLevels().get(_lvl + 1).guilde;
	}

	public static void ReassignAccountToChar(Accounts C) {
		C.get_persos().clear();
		SQLManager.LOAD_PERSO_BY_ACCOUNT(C.get_GUID());
		Map<Integer, Characters> persos = new TreeMap<Integer, Characters>();
		synchronized (getPersos()) {
			persos.putAll(getPersos());
		}
		for (Characters P : persos.values()) {
			if (P.getAccID() == C.get_GUID()) {
				C.addPerso(P);
				P.setAccount(C);
			}
		}
	}

	public static int getZaapCellIdByMapId(short i) {
		for (Entry<Integer, Integer> zaap : Constant.ZAAPS.entrySet()) {
			if (zaap.getKey() == i)
				return zaap.getValue();
		}
		return -1;
	}

	public static int getEncloCellIdByMapId(short i) {
		if (World.getCarte(i).getMountPark() != null) {
			if (World.getCarte(i).getMountPark().get_cellid() > 0) {
				return World.getCarte(i).getMountPark().get_cellid();
			}
		}

		return -1;
	}

	public static void delDragoByID(int getId) {
		getDragodindes().remove(getId);
	}

	public static void removeGuild(int id) {
		if (World.getGuild(id) == null)
			return;
		// Maison de guilde+SQL
		Houses.removeHouseGuild(id);
		// Enclo+SQL
		Maps.MountPark.removeGuildMountParks(id);
		// Percepteur+SQL
		Collector.removePercepteur(id);
		// Guilde
		getGuildes().remove(id);
		SQLManager.DEL_ALL_GUILDMEMBER(id);// Supprime les membres
		SQLManager.DEL_GUILD(id);// Supprime la guilde
	}

	public static boolean ipIsUsed(String ip) {
		for (Accounts c : getComptes().values())
			if (c.get_curIP().compareTo(ip) == 0)
				return true;
		return false;
	}
	
	public static int numberOfSameIp(String ip) {
		int nombre = 0;
		for (Accounts c : Comptes.values()) {
			if (c.get_curIP().compareTo(ip) == 0) {
				nombre++;
			}
		}
		return nombre;
	}

	public static void unloadPerso(int g) {
		Characters toRem = null;
		synchronized (getPersos()) {
			toRem = getPersos().get(g);
		}
		if (toRem == null)
			return;
		if (!toRem.getItems().isEmpty()) {
			for (Entry<Integer, Objects> curObj : toRem.getItems().entrySet()) {
				getObjets().remove(curObj.getKey());
			}
		}
		toRem = null;
		// Persos.remove(g);
	}

	public static boolean isArenaMap(int mapID) {
		for (int curID : Config.arenaMap) {
			if (curID == mapID)
				return true;
		}
		return false;
	}

	public static boolean isCraqueleurMap(int mapID) {
		for (int curID : Config.craqueleurMap) {
			if (curID == mapID)
				return true;
		}
		return false;
	}

	public static boolean isAbraMap(int mapID) {
		for (int curID : Config.abraMap) {
			if (curID == mapID)
				return true;
		}
		return false;
	}

	public static Objects newObjet(int Guid, int template, int qua, int pos, String strStats) {
		if (World.getObjTemplate(template) == null) {
			System.out.println("ItemTemplate " + template + " inexistant, GUID dans la table `items`:" + Guid);
			System.exit(0);
		}

		if (World.getObjTemplate(template).getType() == 85)
			return new SoulStone(Guid, qua, template, pos, strStats);
		else
			return new Objects(Guid, template, qua, pos, strStats);
	}

	public static short get_state() {
		return _state;
	}

	public static void set_state(short state) {
		_state = state;
		for (Entry<Long, Client> c : Client.clients.entrySet()) {
			Client client = c.getValue();
			if (client.getStatus() == Status.LOGIN) {
				client.send("AH" + Config.CONFIG_SERVER_ID + ";"+state+";110;1");
			}
		}
	}

	public static byte getGmAccess() {
		return _GmAccess;
	}

	public static void setGmAccess(byte GmAccess) {
		_GmAccess = GmAccess;
	}

	public static AuctionHouse getHdv(int mapID) {
		return getHdvs().get(mapID);
	}

	public synchronized static int getNextHdvID()// ATTENTION A NE PAS EXECUTER
													// POUR RIEN CETTE METHODE
													// CHANGE LE PROCHAIN ID DE
													// L'HDV LORS DE SON
													// EXECUTION
	{
		nextHdvID++;
		return nextHdvID;
	}

	public synchronized static void setNextHdvID(int nextID) {
		nextHdvID = nextID;
	}

	public synchronized static int getNextLigneID() {
		nextLigneID++;
		return nextLigneID;
	}

	public synchronized static void setNextLigneID(int ligneID) {
		nextLigneID = ligneID;
	}

	public static void addHdvItem(int compteID, int hdvID, HdvEntry toAdd) {
		if (get_hdvsItems().get(compteID) == null) // Si le compte n'est pas
													// dans la memoire
			get_hdvsItems().put(compteID, new HashMap<Integer, ArrayList<HdvEntry>>()); // Ajout
																						// du
																						// compte
																						// clï¿½:compteID
																						// et
																						// un
																						// nouveau
																						// map<hdvID,items<>>

		if (get_hdvsItems().get(compteID).get(hdvID) == null)
			get_hdvsItems().get(compteID).put(hdvID, new ArrayList<HdvEntry>());

		get_hdvsItems().get(compteID).get(hdvID).add(toAdd);
	}

	// Quests
	public static void addQuest(int questId, String steps, int startQuestion, int endQuestion, int minLvl,
			int questRequired) {
		if (quests.get(questId) != null)
			return;

		quests.put(questId, new HashMap<String, String>());

		quests.get(questId).put("steps", steps);
		quests.get(questId).put("startQuestion", startQuestion + "");
		quests.get(questId).put("endQuestion", endQuestion + "");
		quests.get(questId).put("minLvl", minLvl + "");
		quests.get(questId).put("questRequired", questRequired + "");
	}

	public static void addQuestStep(int stepId, String objectives, int question, int gainExp, int gainKamas,
			String gainItems) {
		if (questSteps.get(stepId) != null)
			return;

		questSteps.put(stepId, new HashMap<String, String>());

		questSteps.get(stepId).put("objectives", objectives);
		questSteps.get(stepId).put("question", question + "");
		questSteps.get(stepId).put("gainExp", gainExp + "");
		questSteps.get(stepId).put("gainKamas", gainKamas + "");
		questSteps.get(stepId).put("gainItems", gainItems);
	}

	public static void addQuestObjective(int id, String type, String args, int npcTarget, int questionTarget,
			int answerTarget) {
		if (questObjetives.get(id) != null)
			return;

		questObjetives.put(id, new HashMap<String, String>());
		questObjetives.get(id).put("type", type);
		questObjetives.get(id).put("args", args);
		questObjetives.get(id).put("npcTarget", npcTarget + "");
		questObjetives.get(id).put("optQuestion", questionTarget + "");
		questObjetives.get(id).put("optAnswer", answerTarget + "");
	}

	public static Map<String, String> getQuest(int questId) {
		return quests.get(questId);
	}

	public static Map<String, String> getStep(int StepId) {
		return questSteps.get(StepId);
	}

	public static Map<String, String> getObjective(int objectiveId) {
		return questObjetives.get(objectiveId);
	}

	public static Map<Integer, Map<String, String>> getObjectiveByNpcTarget(int npcId) {
		Map<Integer, Map<String, String>> results = new HashMap<Integer, Map<String, String>>();

		for (Entry<Integer, Map<String, String>> entry : questObjetives.entrySet()) {
			if (entry.getValue().get("npcTarget").equals(npcId + "")) {
				results.put(entry.getKey(), entry.getValue());
			}
		}
		if (!results.isEmpty())
			return results;

		return null; // Aucun objectif avec le pnj
	}

	public static Map<Integer, Map<String, String>> getObjectiveByOptAnswer(int answerId) {

		Map<Integer, Map<String, String>> results = new HashMap<Integer, Map<String, String>>();

		for (Entry<Integer, Map<String, String>> entry : questObjetives.entrySet()) {
			if (entry.getValue().get("optAnswer").equals(answerId + ""))
				results.put(entry.getKey(), entry.getValue());
		}

		if (!results.isEmpty())
			return results;

		return null; // Aucun objectif avec le pnj
	}

	public static Entry<Integer, Map<String, String>> getObjectiveByOptQuestion(int questionId) {

		for (Entry<Integer, Map<String, String>> entry : questObjetives.entrySet()) {
			if (entry.getValue().get("optQuestion").equals(questionId + "")) {
				return entry;
			}
		}

		return null; // Aucun objectif avec le pnj
	}

	public static void removeHdvItem(int compteID, int hdvID, HdvEntry toDel) {
		get_hdvsItems().get(compteID).get(hdvID).remove(toDel);
	}

	public static int getHdvNumber() {
		return getHdvs().size();
	}

	public static int getHdvObjetsNumber() {
		int size = 0;

		for (Map<Integer, ArrayList<HdvEntry>> curCompte : get_hdvsItems().values()) {
			for (ArrayList<HdvEntry> curHdv : curCompte.values()) {
				size += curHdv.size();
			}
		}
		return size;
	}

	public static void addHdv(AuctionHouse toAdd) {
		getHdvs().put(toAdd.getHdvID(), toAdd);
	}

	public static Map<Integer, ArrayList<HdvEntry>> getMyItems(int compteID) {
		if (get_hdvsItems().get(compteID) == null)// Si le compte n'est pas dans
													// la memoire
			get_hdvsItems().put(compteID, new HashMap<Integer, ArrayList<HdvEntry>>());// Ajout
																						// du
																						// compte
																						// clï¿½:compteID
																						// et
																						// un
																						// nouveau
																						// map<hdvID,items

		return get_hdvsItems().get(compteID);
	}

	public static Collection<ObjTemplate> getObjTemplates() {
		return ObjTemplates.values();
	}

	public static boolean mariageok() { // Le mariage est-il ok ?
		boolean a = false;
		boolean b = false;
		try {
			if (getMarried().get(1) != null)
				a = true;
			if (getMarried().get(2) != null)
				b = true;
		} catch (Exception e) {

		}
		if (a == true && b == true)
			return true;
		return false;
	}

	public static Characters getMarried(int ordre) {
		return Married.get(ordre);
	}

	public static void AddMarried(int ordre, Characters perso) {
		Characters Perso = getMarried().get(ordre);
		if (Perso != null) {
			if (perso.get_GUID() == Perso.get_GUID()) // Si c'est le meme
														// joueur...
				return;
			if (Perso.isOnline())// Si perso en ligne...
			{
				getMarried().remove(ordre);
				getMarried().put(ordre, perso);
				return;
			}

			return;
		} else {
			getMarried().put(ordre, perso);
			return;
		}
	}

	public static void PriestRequest(Characters perso, Maps carte, int IdPretre) {
		Characters Homme = getMarried().get(0);
		Characters Femme = getMarried().get(1);
		if (Homme.getWife() != 0) {
			SocketManager.GAME_SEND_MESSAGE_TO_MAP(carte, Homme.get_name() + " est deja marier!",
					Config.CONFIG_MOTD_COLOR);
			return;
		}
		if (Femme.getWife() != 0) {
			SocketManager.GAME_SEND_MESSAGE_TO_MAP(carte, Femme.get_name() + " est deja marier!",
					Config.CONFIG_MOTD_COLOR);
			return;
		}
		SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(perso.get_curCarte(), "", -1, "Prï¿½tre", perso.get_name()
				+ " acceptez-vous d'ï¿½pouser " + getMarried((perso.get_sexe() == 1 ? 0 : 1)).get_name() + " ?");
		SocketManager.GAME_SEND_WEDDING(carte, 617, (Homme == perso ? Homme.get_GUID() : Femme.get_GUID()),
				(Homme == perso ? Femme.get_GUID() : Homme.get_GUID()), IdPretre);
	}

	public static void Wedding(Characters Homme, Characters Femme, int isOK) {
		if (isOK > 0) {
			SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(Homme.get_curCarte(), "", -1, "Prï¿½tre", "Je dï¿½clare "
					+ Homme.get_name() + " et " + Femme.get_name() + " unis par les liens sacrï¿½s du mariage.");
			Homme.MarryTo(Femme);
			Femme.MarryTo(Homme);
		} else {
			SocketManager.GAME_SEND_Im_PACKET_TO_MAP(Homme.get_curCarte(),
					"048;" + Homme.get_name() + "~" + Femme.get_name());
		}
		getMarried().get(0).setisOK(0);
		getMarried().get(1).setisOK(0);
		getMarried().clear();
	}

	public static Hustle getAnimation(int AnimationId) {
		return getAnimations().get(AnimationId);
	}

	public static void addAnimation(Hustle animation) {
		getAnimations().put(animation.getId(), animation);
	}

	public static void addHouse(Houses house) {
		getHouse().put(house.get_id(), house);
	}

	public static Map<Integer, Houses> getHouses() {
		return getHouse();
	}

	public static Houses getHouse(int id) {
		return House.get(id);
	}

	public static void addPerco(Collector perco) {
		getPercepteurs().put(perco.getGuid(), perco);
	}

	public static Collector getPerco(int percoID) {
		return getPercepteurs().get(percoID);
	}

	public static Map<Integer, Collector> getPercos() {
		return getPercepteurs();
	}

	public static void addTrunk(Trunk trunk) {
		getTrunks().put(trunk.get_id(), trunk);
	}

	public static Trunk getTrunk(int id) {
		return getTrunks().get(id);
	}

	public static Map<Integer, Trunk> getTrunks() {
		return Trunks;
	}

	public static void addMountPark(Maps.MountPark mp) {
		getMountPark().put(mp.get_map().get_id(), mp);
	}

	public static Map<Short, Maps.MountPark> getMountPark() {
		return MountPark;
	}

	public static String parseMPtoGuild(int GuildID) {
		Guild G = World.getGuild(GuildID);
		byte enclosMax = (byte) Math.floor(G.get_lvl() / 10);
		StringBuilder packet = new StringBuilder();
		packet.append(enclosMax);

		for (Entry<Short, Maps.MountPark> mp : getMountPark().entrySet()) {
			if (mp.getValue().get_guild() != null && mp.getValue().get_guild().get_id() == GuildID) {
				packet.append("|").append(mp.getValue().get_map().get_id()).append(";").append(mp.getValue().get_size())
						.append(";").append(mp.getValue().getObjectNumb());// Nombre
																			// d'objets
																			// pour
																			// le
																			// dernier
			} else {
				continue;
			}
		}
		return packet.toString();
	}

	public static int totalMPGuild(int GuildID) {
		int i = 0;
		for (Entry<Short, Maps.MountPark> mp : getMountPark().entrySet()) {
			if (mp.getValue().get_guild().get_id() == GuildID) {
				i++;
			} else {
				continue;
			}
		}
		return i;
	}

	public static void addSeller(Characters p) {
		if (getSeller().get(p.get_curCarte().get_id()) == null) {
			ArrayList<Integer> PersoID = new ArrayList<Integer>();
			PersoID.add(p.get_GUID());
			getSeller().put(p.get_curCarte().get_id(), PersoID);
		} else {
			ArrayList<Integer> PersoID = new ArrayList<Integer>();
			PersoID.addAll(getSeller().get(p.get_curCarte().get_id()));
			PersoID.add(p.get_GUID());
			getSeller().remove(p.get_curCarte().get_id());
			getSeller().put(p.get_curCarte().get_id(), PersoID);
		}
	}

	public static Collection<Integer> getSeller(short mapID) {
		return Seller.get(mapID);
	}

	public static void removeSeller(int pID, short mapID) {
		if(getSeller(mapID) != null)
			Seller.get(mapID).remove(pID);
	}

	public static boolean isRestrictedMap(short mapid) {
		if (restrictedMaps == null) {
			restrictedMaps = new ArrayList<Short>();
			for (String map : Config.RESTRICTED_MAPS.split(",")) {
				if (map.isEmpty())
					continue;
				restrictedMaps.add(Short.parseShort(map));
			}
		}
		return restrictedMaps.contains(mapid);
	}

	public static void verifyClone(Characters p) {
		if (p.get_curCell() != null && p.get_fight() == null) {
			if (p.get_curCell().getPersos().containsKey(p.get_GUID())) {
				p.set_Online(false);
				Logs.addToDebug(p.get_name() + " avait un clone.");
				p.get_curCell().removePlayer(p.get_GUID());
				SQLManager.SAVE_PERSONNAGE(p, true);
			}
		}
		if (p.isOnline()) {
			p.set_Online(false);
			SQLManager.SAVE_PERSONNAGE(p, true);
		}
	}

	public static void Banip(Accounts c, int nb_heures) {
		// Action de bannir une ip
		if (nb_heures <= 0)
			nb_heures = -1;
		else
			nb_heures = nb_heures * 3600;
		String ip = c.get_curIP();
		if (ip.equals(""))
			ip = c.get_lastIP();
		if (ip.equals("")){
			Logs.addToSQLLog("Le compte " + c + " n'a pas d'IP enregistré pour le ban");
			return;
		}
		SQLManager.LOAD_ACCOUNT_BY_IP(ip);
		BanIp ban;
		if (nb_heures == -1)
			ban = new BanIp(ip, nb_heures);
		else
			ban = new BanIp(ip, (long) (System.currentTimeMillis() / 1000 + nb_heures));
		getBanips().add(ban);
		SQLManager.ADD_BANIP(ip, ban.getTime());
		for (Accounts compte : getComptes().values()) {
			if ((compte.get_lastIP().equals(ip) || compte.get_curIP().equals(ip)) && compte.get_gmLvl() < 4) {
				compte.ban(ban.getTime(), true);
				if (compte.getGameThread() != null)
					compte.getGameThread().kick();
			}
		}
	}

	public static void unBanip(Accounts c) {
		String ip = c.get_lastIP();
		if (ip.equals(""))
			return;
		c.unBan();
		SQLManager.LOAD_ACCOUNT_BY_IP(ip);
		removeBanip(ip);
		SQLManager.REMOVE_BANIP(ip);
		for (Accounts compte : getComptes().values()) {
			if (compte.get_lastIP().equals(ip) || compte.get_curIP().equals(ip)) {
				compte.unBan();
			}
		}
	}

	public static void addBanip(String ip, long time) {
		if (time < System.currentTimeMillis() / 1000 && time != -1) {
			SQLManager.REMOVE_BANIP(ip);
			return;
		}
		getBanips().add(new BanIp(ip, time));
	}

	public static void removeBanip(String ip) {
		ArrayList<BanIp> bans = new ArrayList<BanIp>();
		for (BanIp ban : getBanips()) {
			if (!ban.isBanned()) {
				SQLManager.REMOVE_BANIP(ban.getIp());
				continue;
			}
			if (ban.getIp().equalsIgnoreCase(ip))
				continue;
			bans.add(ban);
		}
		getBanips().clear();
		getBanips().addAll(bans);
	}

	public static boolean isIpBanned(String ip, Accounts c) {
		boolean isIpBanned = false;
		ArrayList<BanIp> bans = new ArrayList<BanIp>();
		for (BanIp ban : getBanips()) {
			if (!ban.isBanned()) {
				SQLManager.REMOVE_BANIP(ban.getIp());
				continue;
			}
			if (ban.getIp().equalsIgnoreCase(ip)) {
				if (!c.isBanned())
					c.ban(ban.getTime(), true);
				isIpBanned = true;
			}
			bans.add(ban);
		}
		getBanips().clear();
		getBanips().addAll(bans);
		return isIpBanned;
	}

	public static class BanIp {
		private String _ip;
		private long _time;

		public BanIp(String ip, long time) {
			_ip = ip;
			_time = time;
		}

		public String getIp() {
			return _ip;
		}

		public long getTime() {
			return _time;
		}

		public boolean isBanned() {
			if (_time == -1)
				return true;
			if (_time < System.currentTimeMillis() / 1000)
				return false;
			return true;
		}
	}
	
	public static double getBalanceArea(Area area, int alignement) {
		int cant = 0;
		for (SubArea subarea : getSubAreas().values()) {
			if (subarea.getArea() == area && subarea.getalignement() == alignement)
				cant++;
		}
		if (cant == 0)
			return 0;
		return Math.rint((1000 * cant / (area.getSubAreas().size())) / 10);
	}

	public static double getBalanceMundo(int alignement) {
		int cant = 0;
		for (SubArea subarea : getSubAreas().values()) {
			if (subarea.getalignement() == alignement)
				cant++;
		}
		if (cant == 0)
			return 0;
		return Math.rint((10 * cant / 4) / 10);
	}
	
	public static int getWorldBalance(int alignment) {
	    int totalSubAreas = 0;
	    int subAreasAnnexed = 0;
	    
	    for (SubArea subArea : getSubAreas().values()) {
	        if (subArea.getalignement() != -1) {
	            totalSubAreas = (totalSubAreas + 1);
	        }
	        
	        if (subArea.getalignement() == alignment) {
	            subAreasAnnexed = (subAreasAnnexed + 1);
	        }
	    }
	    
	    return (int) (((double) subAreasAnnexed / (double) totalSubAreas) * 100.0);
	}

	public static int getAreaBalance(int alignement) {
	    
	    int totalSubAreas = Areas.size();
	    int subAreasAnnexed = 0;

	    for (SubArea subArea : SubAreas.values()) {
	        if (subArea.getalignement() == alignement) {
	            subAreasAnnexed = (subAreasAnnexed + 1);
	        }
	    }
	    
	    return (int) (((double) subAreasAnnexed / (double) totalSubAreas) * 100.0);
	}
	
	public static int getMultiplierBonus(Characters perso){
		int multiplier = 1;
		switch (perso.getGrade()) {
		case 1:
        case 2:
        case 3:
            multiplier = 1;
            break;
           
        case 4:
        case 5:
        case 6:
        case 7:
            multiplier = 2;
            break;
        case 8:
        case 9:
            multiplier = 3;
            break;
        case 10:
            multiplier = 4;
            break;
        default:
            multiplier = 1;
            break;
   
		}
		return multiplier;
	}
	
	public static int getAlignementBonus(Characters character) {
        double worldBalance = getWorldBalance(character.get_align());
        int bonus = 25;
        if (worldBalance >= 83 && worldBalance <= 100) {
            bonus = 2;
        } else if (worldBalance >= 62 && worldBalance <= 83) {
            bonus = 3;
        } else if (worldBalance > 50 && worldBalance < 62) {
            bonus = 4;
        } else if (worldBalance > 41 && worldBalance < 50) {
            bonus = 5;
        } else if (worldBalance > 35 && worldBalance < 41) {
            bonus = 6;
        } else if (worldBalance > 31 && worldBalance < 35) {
            bonus = 7;
        } else if (worldBalance > 27 && worldBalance < 31) {
            bonus = 8;
        } else if (worldBalance > 25 && worldBalance < 27) {
            bonus = 9;
        } else if (worldBalance > 25 && worldBalance < 22) {
            bonus = 10;
        } else if (worldBalance > 20 && worldBalance < 22) {
            bonus = 11;
        } else if (worldBalance > 19 && worldBalance < 20) {
            bonus = 12;
        } else if (worldBalance > 17 && worldBalance < 19) {
            bonus = 13;
        } else if (worldBalance > 16 && worldBalance < 17) {
            bonus = 14;
        } else if (worldBalance > 15 && worldBalance < 16) {
            bonus = 15;
        } else if (worldBalance > 14 && worldBalance < 15) {
            bonus = 16;
        } else if (worldBalance > 13 && worldBalance < 14) {
            bonus = 17;
        } else if (worldBalance == 13) {
            bonus = 18;
        } else if (worldBalance > 12 && worldBalance < 13) {
            bonus = 19;
        } else if (worldBalance > 11 && worldBalance < 12) {
            bonus = 20;
        } else if (worldBalance == 11) {
            bonus = 21;
        } else if (worldBalance > 10 && worldBalance < 11) {
            bonus = 22;
        } else if (worldBalance == 10) {
            bonus = 23;
        } else if (worldBalance > 9 && worldBalance < 10) {
            bonus = 24;
        } else if (worldBalance > 0 && worldBalance < 10) {
            bonus = 25;
        } else {
            bonus = 25;
        }
      
        return bonus;
    }

	public static Map<Integer, Accounts> getComptes() {
		return Comptes;
	}

	public static void setComptes(Map<Integer, Accounts> comptes) {
		Comptes = comptes;
	}

	public static ArrayList<BanIp> getBanips() {
		return Banips;
	}

	public static void setBanips(ArrayList<BanIp> banips) {
		Banips = banips;
	}

	public static Map<String, Integer> getComptebyName() {
		return ComptebyName;
	}

	public static void setComptebyName(Map<String, Integer> comptebyName) {
		ComptebyName = comptebyName;
	}

	public static StringBuilder getChallenges() {
		return Challenges;
	}

	public static void setChallenges(StringBuilder challenges) {
		Challenges = challenges;
	}

	public static Map<Integer, Characters> getPersos() {
		return Persos;
	}

	public static void setPersos(Map<Integer, Characters> persos) {
		Persos = persos;
	}

	public static Map<Short, Maps> getCartes() {
		return Cartes;
	}

	public static void setCartes(Map<Short, Maps> cartes) {
		Cartes = cartes;
	}

	public static Map<Integer, Objects> getObjets() {
		return Objets;
	}

	public static void setObjets(Map<Integer, Objects> objets) {
		Objets = objets;
	}

	public static Map<Integer, ExpLevel> getExpLevels() {
		return ExpLevels;
	}

	public static void setExpLevels(Map<Integer, ExpLevel> expLevels) {
		ExpLevels = expLevels;
	}

	public static Map<Integer, Spell> getSorts() {
		return Sorts;
	}

	public static void setSorts(Map<Integer, Spell> sorts) {
		Sorts = sorts;
	}

	public static void setObjTemplates(Map<Integer, ObjTemplate> objTemplates) {
		ObjTemplates = objTemplates;
	}

	public static Map<Integer, Monster> getMobTemplates() {
		return MobTemplates;
	}

	public static void setMobTemplates(Map<Integer, Monster> mobTemplates) {
		MobTemplates = mobTemplates;
	}

	public static Map<Integer, NpcTemplates> getNPCTemplates() {
		return NPCTemplates;
	}

	public static void setNPCTemplates(Map<Integer, NpcTemplates> nPCTemplates) {
		NPCTemplates = nPCTemplates;
	}

	public static Map<Integer, NPC_question> getNPCQuestions() {
		return NPCQuestions;
	}

	public static void setNPCQuestions(Map<Integer, NPC_question> nPCQuestions) {
		NPCQuestions = nPCQuestions;
	}

	public static Map<Integer, NPC_reponse> getNPCReponses() {
		return NPCReponses;
	}

	public static void setNPCReponses(Map<Integer, NPC_reponse> nPCReponses) {
		NPCReponses = nPCReponses;
	}

	public static Map<Integer, IOTemplate> getIOTemplate() {
		return IOTemplate;
	}

	public static void setIOTemplate(Map<Integer, IOTemplate> nIOTemplate) {
		IOTemplate = nIOTemplate;
	}

	public static Map<Integer, Mount> getDragodindes() {
		return Dragodindes;
	}

	public static void setDragodindes(Map<Integer, Mount> dragodindes) {
		Dragodindes = dragodindes;
	}

	public static Map<Integer, SuperArea> getSuperAreas() {
		return SuperAreas;
	}

	public static void setSuperAreas(Map<Integer, SuperArea> superAreas) {
		SuperAreas = superAreas;
	}

	public static Map<Integer, Area> getAreas() {
		return Areas;
	}

	public static void setAreas(Map<Integer, Area> areas) {
		Areas = areas;
	}

	public static Map<Integer, SubArea> getSubAreas() {
		return SubAreas;
	}

	public static void setSubAreas(Map<Integer, SubArea> subAreas) {
		SubAreas = subAreas;
	}

	public static Map<Integer, Job> getJob() {
		return Job;
	}

	public static void setJob(Map<Integer, Job> job) {
		Job = job;
	}

	public static Map<Integer, ArrayList<Couple<Integer, Integer>>> getCrafts() {
		return Crafts;
	}

	public static void setCrafts(Map<Integer, ArrayList<Couple<Integer, Integer>>> crafts) {
		Crafts = crafts;
	}

	public static Map<Integer, ItemSet> getItemSets() {
		return ItemSets;
	}

	public static void setItemSets(Map<Integer, ItemSet> itemSets) {
		ItemSets = itemSets;
	}

	public static Map<Integer, Guild> getGuildes() {
		return Guildes;
	}

	public static void setGuildes(Map<Integer, Guild> guildes) {
		Guildes = guildes;
	}

	public static Map<Integer, AuctionHouse> getHdvs() {
		return Hdvs;
	}

	public static void setHdvs(Map<Integer, AuctionHouse> hdvs) {
		Hdvs = hdvs;
	}

	public static Map<Integer, Map<Integer, ArrayList<HdvEntry>>> get_hdvsItems() {
		return _hdvsItems;
	}

	public static void set_hdvsItems(Map<Integer, Map<Integer, ArrayList<HdvEntry>>> _hdvsItems) {
		World._hdvsItems = _hdvsItems;
	}

	public static Map<Integer, Characters> getMarried() {
		return Married;
	}

	public static void setMarried(Map<Integer, Characters> married) {
		Married = married;
	}

	public static Map<Integer, Hustle> getAnimations() {
		return Animations;
	}

	public static void setAnimations(Map<Integer, Hustle> animations) {
		Animations = animations;
	}

	public static void setMountPark(Map<Short, Maps.MountPark> mountPark) {
		MountPark = mountPark;
	}

	public static void setTrunks(Map<Integer, Trunk> trunks) {
		Trunks = trunks;
	}

	public static Map<Integer, Collector> getPercepteurs() {
		return Percepteurs;
	}

	public static void setPercepteurs(Map<Integer, Collector> percepteurs) {
		Percepteurs = percepteurs;
	}

	public static Map<Integer, Houses> getHouse() {
		return House;
	}

	public static void setHouse(Map<Integer, Houses> house) {
		House = house;
	}

	public static Map<Short, Collection<Integer>> getSeller() {
		return Seller;
	}

	public static void setSeller(Map<Short, Collection<Integer>> seller) {
		Seller = seller;
	}

	public static void MoveMobsOnMaps() {
		for (Maps map : Cartes.values()) {
			map.onMap_MonstersDisplacement();
		}
	}

	public static void clearAllVar() {
		Comptes.clear();
		Banips.clear();
		ComptebyName.clear();
		Challenges = null;
		Persos.clear();
		Cartes.clear();
		Objets.clear();
		ExpLevels.clear();
		Sorts.clear();
		ObjTemplates.clear();
		MobTemplates.clear();
		NPCTemplates.clear();
		NPCQuestions.clear();
		IOTemplate.clear();
		Dragodindes.clear();
		SuperAreas.clear();
		Areas.clear();
		SubAreas.clear();
		Job.clear();
		Crafts.clear();
		ItemSets.clear();
		Guildes.clear();
		Hdvs.clear();
		_hdvsItems.clear();
		Married.clear();
		Animations.clear();
		MountPark.clear();
		Trunks.clear();
		Percepteurs.clear();
		House.clear();
		Seller.clear();
	}

	public static java.util.Map<String, String> getGroupFix(int map, int cell) {
		return mobGroupFix.get(map + ";" + cell);
	}

	public static void addGroupFix(String str, String mob, int Time) {
		mobGroupFix.put(str, new HashMap<String, String>());
		mobGroupFix.get(str).put("groupData", mob);
		mobGroupFix.get(str).put("timer", Time + "");
	}
}

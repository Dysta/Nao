package org.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimerTask;
import java.util.TreeMap;

//Quests
import java.util.HashMap;

import javax.swing.Timer;

import org.command.player.Maitre;
import org.command.player.RapidStuff;
import org.common.ConditionParser;
import org.common.Constant;
import org.common.Formulas;
import org.common.SQLManager;
import org.common.SocketManager;
import org.common.World;
import org.common.World.Exchange;
import org.common.World.ItemSet;
import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.extending.Arena;
import org.fight.extending.Team;
import org.fight.object.Collector;
import org.fight.object.Prism;
import org.fight.object.Stalk;
import org.game.GameSendThread;
import org.game.GameThread.GameAction;
import org.game.tools.Utils;
import org.kernel.Config;
import org.kernel.Logs;
import org.object.Guild;
import org.object.Houses;
import org.object.Maps;
import org.object.Mount;
import org.object.Objects;
import org.object.Trunk;
import org.object.Guild.GuildMember;
import org.object.Maps.Case;
import org.object.Maps.InteractiveObject;
import org.object.Maps.MountPark;
import org.object.Objects.ObjTemplate;
import org.object.job.Job;
import org.object.job.Job.JobAction;
import org.object.job.Job.StatsMetier;
import org.spell.SpellEffect;
import org.spell.Spell.SortStats;
import org.utils.Colors;

public class Characters {

	private int _GUID;
	private String _name;
	private String _tag;
	private int _sexe;
	private int _classe;
	private int _color1;
	private int _color2;
	private int _color3;
	private long _kamas;
	private int _spellPts;
	private int _capital;
	private int _energy;
	private int _lvl;
	private long _curExp;
	private int _size;
	private int _gfxID;
	private int _orientation = 1;
	private Accounts _compte;
	private int _accID;
	private boolean _canAggro = true;
	private String _emotes = "7667711";
	private TimerTask actTimerTask;

	// Variables d'ali
	private byte _align = 0;
	private int _deshonor = 0;
	private int _honor = 0;
	private boolean _showWings = false;
	private int _aLvl = 0;
	// Fin ali

	private GuildMember _guildMember;
	private boolean _showFriendConnection;
	private String _canaux;
	Stats _baseStats;
	private Fight _fight;

	private boolean _away;
	private Maps _curCarte;
	private Case _curCell;
	private Maps _curCarteDefPerco = null;
	private Case _curCellDefPerco = null;
	private boolean _sitted;
	private boolean _ready = false;
	private boolean _isOnline = false;
	private Group _group = null;
	private int _duelID = -1;
	private Map<Integer, SpellEffect> _buffs = new TreeMap<Integer, SpellEffect>();
	private Map<Integer, Objects> _items = new TreeMap<Integer, Objects>();
	private Timer _sitTimer;
	private String _savePos;
	private int _emoteActive = 0;
	// PDV
	private int _PDV;
	private int _PDVMAX;
	private int _exPdv;
	// Echanges
	private int _isTradingWith = 0;
	private Exchange _curExchange;
	// Dialogue
	private int _isTalkingWith = 0;
	// Invitationg
	private int _inviting = 0;
	// Job
	private JobAction _curJobAction;
	private Map<Integer, StatsMetier> _metiers = new TreeMap<Integer, StatsMetier>();
	// Enclos
	private MountPark _inMountPark;
	// Monture
	private Mount _mount;
	private int _mountXpGive = 0;
	private boolean _onMount = false;
	// Banque
	private boolean _isInBank;
	// Zaap
	private boolean _isZaaping = false;
	private ArrayList<Short> _zaaps = new ArrayList<Short>();
	// Disponibilit�
	public boolean _isAbsent = false;
	public boolean _isInvisible = false;
	// Sort
	public boolean _seeSpell = false;
	private boolean _isForgetingSpell = false;
	private Map<Integer, SortStats> _sorts = new TreeMap<Integer, SortStats>();
	private Map<Integer, Character> _sortsPlaces = new TreeMap<Integer, Character>();
	// Double
	public boolean _isClone = false;
	// Percepteurs
	private int _isOnPercepteurID = 0;
	// Titre
	private byte _title = 0;
	// Inactivit�
	protected long _lastPacketTime;
	// Mariage
	private int _wife = 0;
	private int _isOK = 0;
	// Suiveur - Suivi
	public Map<Integer, Characters> _Follower = new TreeMap<Integer, Characters>();
	public Characters _Follows = null;
	// Fantome
	public boolean _isGhosts = false;
	private int _Speed = 0;
	// Coffre
	private Trunk _curTrunk;
	// Maison
	private Houses _curHouse;
	// Marchand
	public boolean _seeSeller = false;
	private Map<Integer, Integer> _storeItems = new TreeMap<Integer, Integer>();// <ObjID, Prix>
	// Inventaire dragodine
	private boolean _isInDinde;
	// Nouveau personnage
	private boolean _isNew = false;
	//Maitre
	public Maitre _maitre = null;
	private boolean esclave = false;

	private boolean _hasEndfight = false;
	// kolizeum + Arena
	private int arena = -1;
	private int kolizeum = -1;
	private short LastMapFight = -1;
	private Map<String, Integer> LastPvPFighters = new TreeMap<String, Integer>();

	// Anti flood canaux
	private long lastMess = 0;
	private String content = ".";

	private Map<String, Long> lastMessSent = new TreeMap<String, Long>();
	private int lastFloodTime = 0;
	private int averto = 1;
	
	// Equipements rapides
	private ArrayList<RapidStuff> rapidStuffs = new ArrayList<RapidStuff>();
	private boolean Equip = false;
	// Quests
	private Map<Integer, Map<String, String>> _quests = new HashMap<Integer, Map<String, String>>();
	// Pnjs suiveurs
	private String _followers = "";

	public static class Group {
		private ArrayList<Characters> _persos = new ArrayList<Characters>();
		private Characters _chief;

		public Group(Characters p1, Characters p2) {
			synchronized (_persos) {
				_chief = p1;
				_persos.add(p1);
				_persos.add(p2);
			}
		}

		public boolean isChief(int guid) {
			return _chief.get_GUID() == guid;
		}

		public void addPerso(Characters p) {
			synchronized (_persos) {
				_persos.add(p);
			}
		}

		public int getPersosNumber() {
			return _persos.size();
		}

		public int getGroupLevel() {
			int lvls = 0;
			synchronized (_persos) {
				for (Characters p : _persos) {
					lvls += p.get_lvl();
				}
			}
			return lvls;
		}

		public ArrayList<Characters> getPersos() {
			ArrayList<Characters> ps = new ArrayList<Characters>();
			synchronized (_persos) {
				ps.addAll(_persos);
			}
			return ps;
		}

		public Characters getChief() {
			return _chief;
		}

		public void leave(Characters p) {
			synchronized (_persos) {
				if (p.getArena() == 0) {
					Arena.delTeam(Team.getTeamByID(p.getTeamID()));
				}
				if (!_persos.contains(p))
					return;
				p.setGroup(null);
				_persos.remove(p);
				if (_persos.size() == 1) {
					_persos.get(0).setGroup(null);
					if (_persos.get(0).get_compte() == null || _persos.get(0).get_compte().getGameThread() == null)
						return;
					SocketManager.GAME_SEND_PV_PACKET(_persos.get(0).get_compte().getGameThread().get_out(), "");
				} else
					SocketManager.GAME_SEND_PM_DEL_PACKET_TO_GROUP(this, p.get_GUID());
			}
		}
	}

	public static class BoostSpellStats {
		private Map<Integer, Map<Integer, Integer>> Effects = new TreeMap<Integer, Map<Integer, Integer>>();

		public BoostSpellStats() {
		}

		public BoostSpellStats(BoostSpellStats stats) {
			cumulStats(stats);
		}

		public boolean haveStats() {
			if (Effects.size() != 0)
				return true;
			return false;
		}

		public void cumulStats(BoostSpellStats stats) {
			for (Entry<Integer, Map<Integer, Integer>> entry : stats.getAllEffects().entrySet()) {
				if (entry == null || entry.getValue() == null)
					continue;
				if (!Effects.containsKey(entry.getKey()))
					Effects.put(entry.getKey(), new TreeMap<Integer, Integer>());
				for (Entry<Integer, Integer> stat : entry.getValue().entrySet()) {
					if (Effects.get(entry.getKey()).containsKey(stat.getKey())
							&& Effects.get(entry.getKey()).get(stat.getKey()) != null) {
						Effects.get(entry.getKey()).put(stat.getKey(),
								Effects.get(entry.getKey()).get(stat.getKey()) + stat.getValue());
					} else {
						Effects.get(entry.getKey()).put(stat.getKey(), stat.getValue());
					}
				}
			}
		}

		public void addStat(int spellId, int statId, int value) {
			if (!Effects.containsKey(spellId))
				Effects.put(spellId, new TreeMap<Integer, Integer>());
			if (Effects.get(spellId).get(statId) != null) {
				Effects.get(spellId).put(statId, Effects.get(spellId).get(statId) + value);
			} else {
				Effects.get(spellId).put(statId, value);
			}
		}

		public int getStat(int spellId, int statId) {
			if (!Effects.containsKey(spellId) || Effects.get(spellId) == null)
				return 0;
			if (!Effects.get(spellId).containsKey(statId) || Effects.get(spellId).get(statId) == null)
				return 0;
			return Effects.get(spellId).get(statId);
		}

		public boolean haveStat(int spellId, int statId) {
			if (!Effects.containsKey(spellId) || Effects.get(spellId) == null)
				return false;
			if (!Effects.get(spellId).containsKey(statId) || Effects.get(spellId).get(statId) == null)
				return false;
			return true;
		}

		public Map<Integer, Map<Integer, Integer>> getAllEffects() {
			return Effects;
		}
	}

	public static class Stats {
		private Map<Integer, Integer> Effects = new TreeMap<Integer, Integer>();

		public Stats(boolean addBases, Characters perso) {
			Effects = new TreeMap<Integer, Integer>();
			if (!addBases)
				return;
			Effects.put(Constant.STATS_ADD_PA, perso.get_lvl() < 100 ? 6 : 7);
			Effects.put(Constant.STATS_ADD_PM, 3);
			Effects.put(Constant.STATS_ADD_PROS, perso.get_classe() == Constant.CLASS_ENUTROF ? 120 : 100);
			Effects.put(Constant.STATS_ADD_PODS, 1000);
			Effects.put(Constant.STATS_CREATURE, 1);
			Effects.put(Constant.STATS_ADD_INIT, 1);
		}

		public Stats(Map<Integer, Integer> stats, boolean addBases, Characters perso) {
			Effects = stats;
			if (!addBases)
				return;
			Effects.put(Constant.STATS_ADD_PA, perso.get_lvl() < 100 ? 6 : 7);
			Effects.put(Constant.STATS_ADD_PM, 3);
			Effects.put(Constant.STATS_ADD_PROS, perso.get_classe() == Constant.CLASS_ENUTROF ? 120 : 100);
			Effects.put(Constant.STATS_ADD_PODS, 1000);
			Effects.put(Constant.STATS_CREATURE, 1);
			Effects.put(Constant.STATS_ADD_INIT, 1);
		}
		
		public Stats(Map<Integer, Integer> stats) {
			Effects = stats;
		}

		public Stats() {
			Effects = new TreeMap<Integer, Integer>();
		}

		public int addOneStat(int id, int val) {
			if (Effects.get(id) == null || Effects.get(id) == 0)
				Effects.put(id, val);
			else {
				int newVal = (Effects.get(id) + val);
				Effects.put(id, newVal);
			}
			return Effects.get(id);
		}
		

		public boolean isSameStats(Stats other) {
			for (Entry<Integer, Integer> entry : Effects.entrySet()) {
				// Si la stat n'existe pas dans l'autre map
				if (other.getMap().get(entry.getKey()) == null)
					return false;
				// Si la stat existe mais n'a pas la m�me valeur
				if (other.getMap().get(entry.getKey()) != entry.getValue())
					return false;
			}
			for (Entry<Integer, Integer> entry : other.getMap().entrySet()) {
				// Si la stat n'existe pas dans l'autre map
				if (Effects.get(entry.getKey()) == null)
					return false;
				// Si la stat existe mais n'a pas la m�me valeur
				if (Effects.get(entry.getKey()) != entry.getValue())
					return false;
			}
			return true;
		}

		public int getEffect(int id) {
			int val;
			if (Effects.get(id) == null)
				val = 0;
			else
				val = Effects.get(id);
			switch (id)// Bonus/Malus TODO
			{
			case Constant.STATS_ADD_AFLEE:
				if (Effects.get(Constant.STATS_REM_AFLEE) != null)
					val -= (int) (getEffect(Constant.STATS_REM_AFLEE));
				if (Effects.get(Constant.STATS_ADD_SAGE) != null)
					val += (int) (getEffect(Constant.STATS_ADD_SAGE) / 4);
				break;
			case Constant.STATS_ADD_MFLEE:
				if (Effects.get(Constant.STATS_REM_MFLEE) != null)
					val -= (int) (getEffect(Constant.STATS_REM_MFLEE));
				if (Effects.get(Constant.STATS_ADD_SAGE) != null)
					val += (int) (getEffect(Constant.STATS_ADD_SAGE) / 4);
				break;
			case Constant.STATS_ADD_INIT:
				if (Effects.get(Constant.STATS_REM_INIT) != null)
					val -= Effects.get(Constant.STATS_REM_INIT);
				break;
			case Constant.STATS_ADD_AGIL:
				if (Effects.get(Constant.STATS_REM_AGIL) != null)
					val -= Effects.get(Constant.STATS_REM_AGIL);
				break;
			case Constant.STATS_ADD_FORC:
				if (Effects.get(Constant.STATS_REM_FORC) != null)
					val -= Effects.get(Constant.STATS_REM_FORC);
				break;
			case Constant.STATS_ADD_CHAN:
				if (Effects.get(Constant.STATS_REM_CHAN) != null)
					val -= Effects.get(Constant.STATS_REM_CHAN);
				break;
			case Constant.STATS_ADD_INTE:
				if (Effects.get(Constant.STATS_REM_INTE) != null)
					val -= Effects.get(Constant.STATS_REM_INTE);
				break;
			case Constant.STATS_ADD_PA:
				if (Effects.get(Constant.STATS_ADD_PA2) != null)
					val += Effects.get(Constant.STATS_ADD_PA2);
				if (Effects.get(Constant.STATS_REM_PA) != null)
					val -= Effects.get(Constant.STATS_REM_PA);
				if (Effects.get(Constant.STATS_REM_PA2) != null)// Non
																// esquivable
					val -= Effects.get(Constant.STATS_REM_PA2);
				break;
			case Constant.STATS_ADD_PM:
				if (Effects.get(Constant.STATS_ADD_PM2) != null)
					val += Effects.get(Constant.STATS_ADD_PM2);
				if (Effects.get(Constant.STATS_REM_PM) != null)
					val -= Effects.get(Constant.STATS_REM_PM);
				if (Effects.get(Constant.STATS_REM_PM2) != null)// Non
																// esquivable
					val -= Effects.get(Constant.STATS_REM_PM2);
				break;
			case Constant.STATS_ADD_PO:
				if (Effects.get(Constant.STATS_REM_PO) != null)
					val -= Effects.get(Constant.STATS_REM_PO);
				break;
			case Constant.STATS_ADD_VITA:
				if (Effects.get(Constant.STATS_REM_VITA) != null)
					val -= Effects.get(Constant.STATS_REM_VITA);
				break;
			case Constant.STATS_ADD_DOMA:
				if (Effects.get(Constant.STATS_REM_DOMA) != null)
					val -= Effects.get(Constant.STATS_REM_DOMA);
				break;
			case Constant.STATS_ADD_PODS:
				if (Effects.get(Constant.STATS_REM_PODS) != null)
					val -= Effects.get(Constant.STATS_REM_PODS);
				break;
			case Constant.STATS_ADD_PROS:
				if (Effects.get(Constant.STATS_REM_PROS) != null)
					val -= Effects.get(Constant.STATS_REM_PROS);
				break;
			case Constant.STATS_ADD_R_TER:
				if (Effects.get(Constant.STATS_REM_R_TER) != null)
					val -= Effects.get(Constant.STATS_REM_R_TER);
				break;
			case Constant.STATS_ADD_R_EAU:
				if (Effects.get(Constant.STATS_REM_R_EAU) != null)
					val -= Effects.get(Constant.STATS_REM_R_EAU);
				break;
			case Constant.STATS_ADD_R_AIR:
				if (Effects.get(Constant.STATS_REM_R_AIR) != null)
					val -= Effects.get(Constant.STATS_REM_R_AIR);
				break;
			case Constant.STATS_ADD_R_FEU:
				if (Effects.get(Constant.STATS_REM_R_FEU) != null)
					val -= Effects.get(Constant.STATS_REM_R_FEU);
				break;
			case Constant.STATS_ADD_R_NEU:
				if (Effects.get(Constant.STATS_REM_R_NEU) != null)
					val -= Effects.get(Constant.STATS_REM_R_NEU);
				break;
			case Constant.STATS_ADD_RP_TER:
				if (Effects.get(Constant.STATS_REM_RP_TER) != null)
					val -= Effects.get(Constant.STATS_REM_RP_TER);
				break;
			case Constant.STATS_ADD_RP_EAU:
				if (Effects.get(Constant.STATS_REM_RP_EAU) != null)
					val -= Effects.get(Constant.STATS_REM_RP_EAU);
				break;
			case Constant.STATS_ADD_RP_AIR:
				if (Effects.get(Constant.STATS_REM_RP_AIR) != null)
					val -= Effects.get(Constant.STATS_REM_RP_AIR);
				break;
			case Constant.STATS_ADD_RP_FEU:
				if (Effects.get(Constant.STATS_REM_RP_FEU) != null)
					val -= Effects.get(Constant.STATS_REM_RP_FEU);
				break;
			case Constant.STATS_ADD_RP_NEU:
				if (Effects.get(Constant.STATS_REM_RP_NEU) != null)
					val -= Effects.get(Constant.STATS_REM_RP_NEU);
				break;
			case Constant.STATS_ADD_MAITRISE:
				if (Effects.get(Constant.STATS_ADD_MAITRISE) != null)
					val = Effects.get(Constant.STATS_ADD_MAITRISE);
				break;
			}
			/*
			 * if(_fightStatic != null) { if(_fightStatic.get_type() ==
			 * Constant.FIGHT_TYPE_AGRESSION || _fightStatic.get_type() ==
			 * Constant.FIGHT_TYPE_PVT || _fightStatic.get_type() ==
			 * Constant.FIGHT_TYPE_CHALLENGE || _fightStatic.get_type() ==
			 * Constant.FIGHT_TYPE_CONQUETE) if(id == Constant.STATS_ADD_PA)
			 * if(val > 12) val = 12; if(id == Constant.STATS_ADD_PM) if(val >
			 * 6) val = 6; }
			 */
			return val;
		}

		public void clear() {
			Effects.clear();
		}

		public static Stats cumulStat(Stats s1, Stats s2) {
			TreeMap<Integer, Integer> effets = new TreeMap<Integer, Integer>();
			for (int a = 0; a <= Constant.MAX_EFFECTS_ID; a++) {
				if ((s1.Effects.get(a) == null || s1.Effects.get(a) == 0)
						&& (s2.Effects.get(a) == null || s2.Effects.get(a) == 0))
					continue;
				int som = 0;
				if (s1.Effects.get(a) != null)
					som += s1.Effects.get(a);

				if (s2.Effects.get(a) != null)
					som += s2.Effects.get(a);

				effets.put(a, som);
			}
			return new Stats(effets, false, null);
		}

		public Map<Integer, Integer> getMap() {
			return Effects;
		}

		public String parseToItemSetStats() {
			StringBuilder str = new StringBuilder();
			if (Effects.isEmpty())
				return "";
			for (Entry<Integer, Integer> entry : Effects.entrySet()) {
				if (str.length() > 0)
					str.append(",");
				str.append(Integer.toHexString(entry.getKey())).append("#")
						.append(Integer.toHexString(entry.getValue())).append("#0#0");
			}
			return str.toString();
		}

		
	}
	
	public Characters(int _guid, String _name, String _tag, int _sexe, int _classe, int _color1, int _color2, int _color3,
			long _kamas, int pts, int _capital, int _energy, int _lvl, long exp, int _size, int _gfxid, byte alignement,
			int _compte, Map<Integer, Integer> stats, byte seeFriend, byte seeAlign, byte seeSeller, String canaux,
			short map, int cell, String stuff, String storeObjets, int pdvPer, String spells, String savePos,
			String jobs, int mountXp, int mount, int honor, int deshonor, int alvl, String z, byte title, int wifeGuid,
			int teamID, String quests) {
		this._GUID = _guid;
		this._name = _name;
		this._tag = _tag;
		this._sexe = _sexe;
		this._classe = _classe;
		this._color1 = _color1;
		this._color2 = _color2;
		this._color3 = _color3;
		savestat = 0;
		this._kamas = _kamas;
		this._spellPts = pts;
		this._capital = _capital;
		this._align = alignement;
		this._honor = honor;
		this._deshonor = deshonor;
		this._aLvl = alvl;
		this._energy = _energy;
		this._lvl = _lvl;
		this._curExp = exp;
		if (mount != -1)
			this._mount = World.getDragoByID(mount);
		this._size = _size;
		this._gfxID = _gfxid;
		this._mountXpGive = mountXp;
		this._baseStats = new Stats(stats, true, this);
		this._accID = _compte;
		this._compte = World.getCompte(_compte);
		this._showFriendConnection = seeFriend == 1;
		this._wife = wifeGuid;
		this.teamID = teamID;
		if (this.get_align() != 0) {
			this._showWings = seeAlign == 1;
		} else {
			this._showWings = false;
		}
		this._canaux = canaux;
		this._curCarte = World.getCarte(map);
		this._savePos = savePos;
		if (_curCarte == null && World.getCarte(Config.CONFIG_START_MAP) != null) {
			this._curCarte = World.getCarte(Config.CONFIG_START_MAP);
			this._curCell = _curCarte.getCase(Config.CONFIG_START_CELL);
		} else if (_curCarte == null && World.getCarte(Config.CONFIG_START_MAP) == null) {
			Logs.addToGameLog("Personnage mal positione, et position de d�part non valide. Fermeture du serveur.");
			System.exit(0);
		} else if (_curCarte != null) {
			this._curCell = _curCarte.getCase(cell);
			if (_curCell == null) {
				this._curCarte = World.getCarte(Config.CONFIG_START_MAP);
				this._curCell = _curCarte.getCase(Config.CONFIG_START_CELL);
			}
		}
		for (String str : z.split(",")) {
			try {
				_zaaps.add(Short.parseShort(str));
			} catch (Exception e) {
			}
			;
		}
		if (_curCarte == null || _curCell == null) {
			Logs.addToGameLog("Map ou case de d�part du personnage " + _name + " invalide");
			Logs.addToGameLog("Map ou case par d�faut invalide");
			Logs.addToGameLog("Le serveur ne peut se lancer");
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
			}
			System.exit(0);
		}

		if (!stuff.equals("")) {
			if (stuff.charAt(stuff.length() - 1) == '|')
				stuff = stuff.substring(0, stuff.length() - 1);
			SQLManager.LOAD_ITEMS(stuff.replace("|", ","));
		}
		for (String item : stuff.split("\\|")) {
			if (item.equals(""))
				continue;
			String[] infos = item.split(":");

			int guid = 0;
			try {
				guid = Integer.parseInt(infos[0]);
			} catch (Exception e) {
				continue;
			}
			;

			Objects obj = World.getObjet(guid);
			if (obj == null)
				continue;
			_items.put(obj.getGuid(), obj);
		}
		if (!storeObjets.equals("")) {
			for (String _storeObjets : storeObjets.split("\\|")) {
				String[] infos = _storeObjets.split(",");
				int guid = 0;
				int price = 0;
				try {
					guid = Integer.parseInt(infos[0]);
					price = Integer.parseInt(infos[1]);
				} catch (Exception e) {
					continue;
				}
				;

				Objects obj = World.getObjet(guid);
				if (obj == null)
					continue;

				_storeItems.put(obj.getGuid(), price);
			}
		}
		this._PDVMAX = (_lvl - 1) * 5 + Constant.getBasePdv(_classe)
				+ getTotalStats().getEffect(Constant.STATS_ADD_VITA)
				+ getTotalStats().getEffect(Constant.STATS_ADD_VIE);
		this._PDV = (_PDVMAX * pdvPer) / 100;
		parseSpells(spells);

		_sitTimer = new Timer(2000, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				regenLife();
			}
		});

		_exPdv = _PDV;

		// Chargement des m�tiers
		if (!jobs.equals("")) {
			for (String aJobData : jobs.split(";")) {
				String[] infos = aJobData.split(",");
				try {
					int jobID = Integer.parseInt(infos[0]);
					long xp = Long.parseLong(infos[1]);
					Job m = World.getMetier(jobID);
					StatsMetier SM = _metiers.get(learnJob(m));
					SM.addXp(this, xp);
				} catch (Exception e) {
					e.getStackTrace();
				}
			}
		}

		this._title = title;
		if (_energy == 0)
			set_Ghosts();
		// _quests = quests;
		this.initializeQuests(quests);
	}

	// Clone double revu par Return,Skrn
	public Characters(int _guid, String _name, int _sexe, int _classe, int _color1, int _color2, int _color3, int _lvl,
			int _size, int _gfxid, Map<Integer, Integer> stats, Map<Integer, Objects> objets, int pdvPer, byte seeAlign,
			int mount, int alvl, byte alignement) {
		this._GUID = _guid;
		this._name = _name;
		this._sexe = _sexe;
		this._classe = _classe;
		this._color1 = _color1;
		this._color2 = _color2;
		this._color3 = _color3;
		this._lvl = _lvl;
		this._aLvl = alvl;
		this._size = _size;
		this._gfxID = _gfxid;
		this._baseStats = new Stats(stats, true, this);
		this._items.putAll(objets);
		this._PDVMAX = (_lvl - 1) * 5 + Constant.getBasePdv(_classe)
				+ getTotalStats().getEffect(Constant.STATS_ADD_VITA)
				+ getTotalStats().getEffect(Constant.STATS_ADD_VIE);
		this._PDV = (_PDVMAX * pdvPer) / 100;

		_exPdv = _PDV;

		this._align = alignement;
		if (this.get_align() != 0) {
			this._showWings = seeAlign == 1;
		} else {
			this._showWings = false;
		}
		if (mount != -1)
			this._mount = World.getDragoByID(mount);
	}

	public void regenLife() {
		// Joueur pas en jeu
		if (_curCarte == null)
			return;
		// Pas de regen en combat
		if (_fight != null)
			return;
		// D�j� Full PDV
		if (_PDV == _PDVMAX)
			return;
		_PDV++;
	}
	
	public void boostStatFixedCount(int stat, int countVal) {
		Logs.addToGameLog("Perso " + _name + ": tentative de boost stat " + stat + " " + countVal + " fois");
		for (int i = 0; i < countVal; i++) {
			int value = 0;
			switch (stat) {
			case 10:// Force
				value = _baseStats.getEffect(Constant.STATS_ADD_FORC);
				break;
			case 13:// Chance
				value = _baseStats.getEffect(Constant.STATS_ADD_CHAN);
				break;
			case 14:// Agilit�
				value = _baseStats.getEffect(Constant.STATS_ADD_AGIL);
				break;
			case 15:// Intelligence
				value = _baseStats.getEffect(Constant.STATS_ADD_INTE);
				break;
			}
			int cout = Constant.getReqPtsToBoostStatsByClass(_classe, stat, value);
			if (cout <= _capital) {
				switch (stat) {
				case 11:// Vita
					if (_classe != Constant.CLASS_SACRIEUR)
						_baseStats.addOneStat(Constant.STATS_ADD_VITA, 1);
					else
						_baseStats.addOneStat(Constant.STATS_ADD_VITA, 2);
					break;
				case 12:// Sage
					_baseStats.addOneStat(Constant.STATS_ADD_SAGE, 1);
					break;
				case 10:// Force
					_baseStats.addOneStat(Constant.STATS_ADD_FORC, 1);
					break;
				case 13:// Chance
					_baseStats.addOneStat(Constant.STATS_ADD_CHAN, 1);
					break;
				case 14:// Agilit�
					_baseStats.addOneStat(Constant.STATS_ADD_AGIL, 1);
					break;
				case 15:// Intelligence
					_baseStats.addOneStat(Constant.STATS_ADD_INTE, 1);
					break;
				default:
					return;
				}
				_capital -= cout;
			}
		}
		SocketManager.GAME_SEND_STATS_PACKET(this);
		SQLManager.SAVE_PERSONNAGE(this, true);
	}

	public static Characters CREATE_PERSONNAGE(String name, int sexe, int classe, int color1, int color2, int color3,
			Accounts compte) {
		String z = "";
		if (Config.CONFIG_ZAAP) {
			for (Entry<Integer, Integer> i : Constant.ZAAPS.entrySet()) {
				if (z.length() != 0)
					z += ",";
				z += i.getKey();
			}
		}
		Characters perso = new Characters(SQLManager.getNextPersonnageGuid(), name, null, sexe, classe, color1, color2,
				color3, Config.CONFIG_START_KAMAS, ((Config.CONFIG_START_LEVEL - 1) * 1),
				((Config.CONFIG_START_LEVEL - 1) * 5), 10000, Config.CONFIG_START_LEVEL,
				World.getPersoXpMin(Config.CONFIG_START_LEVEL), 100, Integer.parseInt(classe + "" + sexe), (byte) 0,
				compte.get_GUID(), new TreeMap<Integer, Integer>(), (byte) 1, (byte) 0, (byte) 0, "*#%!pi$:?",
				(short) Constant.getStartMap(classe), Constant.getStartCell(classe), "", "", 100, "", "10298,314", "",
				0, -1, 0, 0, 0, z, (byte) 0, 0, -1, "");
		perso._sorts = Constant.getStartSorts(classe);
		for (int a = 1; a <= perso.get_lvl(); a++) {
			Constant.onLevelUpSpells(perso, a);
		}
		perso._sortsPlaces = Constant.getStartSortsPlaces(classe);
		if (!SQLManager.ADD_PERSO_IN_BDD(perso))
			return null;

		World.addPersonnage(perso);

		perso.setIsNew(true); // Nouveau perso
		return perso;
	}

	public void set_Online(boolean d) {
		_isOnline = d;
	}

	public boolean isOnline() {
		return _isOnline;
	}

	public void setGroup(Group g) {
		_group = g;
	}

	public Group getGroup() {
		return _group;
	}

	public String parseSpellToDB() {
		StringBuilder sorts = new StringBuilder();
		if (_sorts.isEmpty())
			return "";
		for (int key : _sorts.keySet()) {
			// 3;1;a,4;3;b
			SortStats SS = _sorts.get(key);
			sorts.append(SS.getSpellID()).append(";").append(SS.getLevel()).append(";");
			if (_sortsPlaces.get(key) != null)
				sorts.append(_sortsPlaces.get(key));
			else
				sorts.append("_");
			sorts.append(",");
		}
		return sorts.substring(0, sorts.length() - 1).toString();
	}

	private void parseSpells(String str) {
		String[] spells = str.split(",");
		for (String e : spells) {
			try {
				int id = Integer.parseInt(e.split(";")[0]);
				int lvl = Integer.parseInt(e.split(";")[1]);
				char place = e.split(";")[2].charAt(0);
				learnSpell(id, lvl, false, false);
				_sortsPlaces.put(id, place);
			} catch (NumberFormatException e1) {
				continue;
			}
			;
		}
	}

	public String get_savePos() {
		return _savePos;
	}

	public void set_savePos(String savePos) {
		_savePos = savePos;
	}

	public int get_isTradingWith() {
		return _isTradingWith;
	}

	public void set_isTradingWith(int tradingWith) {
		_isTradingWith = tradingWith;
	}

	public int get_isTalkingWith() {
		return _isTalkingWith;
	}

	public void set_isTalkingWith(int talkingWith) {
		_isTalkingWith = talkingWith;
	}

	public long get_kamas() {
		return _kamas;
	}

	public Map<Integer, SpellEffect> get_buff() {
		return _buffs;
	}

	public void set_kamas(long l) {
		this._kamas = l;
	}

	public Accounts get_compte() {
		return _compte;
	}

	public int get_spellPts() {
		return _spellPts;
	}

	public void set_spellPts(int pts) {
		_spellPts = pts;
	}

	public Guild get_guild() {
		if (_guildMember == null)
			return null;
		return _guildMember.getGuild();
	}

	public void setGuildMember(GuildMember _guild) {
		this._guildMember = _guild;
	}

	public boolean is_ready() {
		return _ready;
	}

	public void set_ready(boolean _ready) {
		this._ready = _ready;
	}

	public int get_duelID() {
		return _duelID;
	}

	public Fight get_fight() {
		return _fight;
	}

	public void set_duelID(int _duelid) {
		_duelID = _duelid;
	}

	public int get_energy() {
		return _energy;
	}

	public boolean is_showFriendConnection() {
		return _showFriendConnection;
	}

	public boolean is_showSpells() {
		return _seeSpell;
	}

	public boolean is_showWings() {
		return _showWings;
	}

	public boolean is_showSeller() {
		return _seeSeller;
	}

	public void set_showSeller(boolean is) {
		_seeSeller = is;
	}

	public String get_canaux() {
		return _canaux;
	}

	public void set_energy(int _energy) {
		this._energy = _energy;
	}

	public int get_lvl() {
		return _lvl;
	}

	public void set_lvl(int _lvl) {
		this._lvl = _lvl;
	}

	public long get_curExp() {
		return _curExp;
	}

	public Maps.Case get_curCell() {
		return _curCell;
	}

	public void set_curCell(Maps.Case cell) {
		_curCell = cell;
	}

	public void set_curExp(long exp) {
		_curExp = exp;
	}

	public int get_size() {
		return _size;
	}

	public void set_size(int _size) {
		this._size = _size;
	}

	public void set_fight(Fight _fight) {
		this._fight = _fight;
	}

	public int get_gfxID() {
		return _gfxID;
	}

	public void set_gfxID(int _gfxid) {
		_gfxID = _gfxid;
	}

	public int get_GUID() {
		return _GUID;
	}

	public Maps get_curCarte() {
		return _curCarte;
	}

	public String get_name() {
		return _name;
	}

	public void set_name(String name) {
		_name = name;
	}
	
	public String get_tag() {
		return _tag;
	}

	public void set_tag(String tag) {
		_tag = tag;
	}

	public boolean is_away() {
		return _away;
	}

	public void set_away(boolean _away) {
		this._away = _away;
	}

	public boolean isSitted() {
		return _sitted;
	}

	public int get_sexe() {
		return _sexe;
	}

	public int get_classe() {
		return _classe;
	}
	
	public int set_classe(int classe) {
		return _classe = classe;
	}

	public int get_color1() {
		return _color1;
	}

	public int get_color2() {
		return _color2;
	}

	public Stats get_baseStats() {
		return _baseStats;
	}

	public int get_color3() {
		return _color3;
	}

	public void set_colors(int c1, int c2, int c3) {
		_color1 = c1;
		_color2 = c2;
		_color3 = c3;
	}

	public int get_capital() {
		return _capital;
	}
	
	public void setSpells(TreeMap<Integer, SortStats> startSorts) {
		_sorts = startSorts;
	}
	
	public void setSpellsPlace(TreeMap<Integer, Character> startSortsPlaces) {
		_sortsPlaces = startSortsPlaces;
	}

	public boolean learnSpell(int spellID, int level, boolean save, boolean send) {
		if (World.getSort(spellID).getStatsByLevel(level) == null) {
			Logs.addToGameLog("[ERROR]Sort " + spellID + " lvl " + level + " non trouve.");
			return false;
		}
		_sorts.put(spellID, World.getSort(spellID).getStatsByLevel(level));

		if (send) {
			SocketManager.GAME_SEND_SPELL_LIST(this);
			SocketManager.GAME_SEND_Im_PACKET(this, "03;" + spellID);
		}
		if (save)
			SQLManager.SAVE_PERSONNAGE(this, false);
		return true;
	}

	public boolean boostSpell(int spellID) {
		if (getSortStatBySortIfHas(spellID) == null) {
			Logs.addToGameLog(_name + " n'a pas le sort " + spellID);
			return false;
		}
		int AncLevel = getSortStatBySortIfHas(spellID).getLevel();
		if (AncLevel == 6)
			return false;
		if (_spellPts >= AncLevel && World.getSort(spellID).getStatsByLevel(AncLevel + 1).getReqLevel() <= _lvl) {
			if (learnSpell(spellID, AncLevel + 1, true, false)) {
				_spellPts -= AncLevel;
				SQLManager.SAVE_PERSONNAGE(this, false);
				return true;
			} else {
				Logs.addToGameLog(_name + " : Echec LearnSpell " + spellID);
				return false;
			}
		} else// Pas le niveau ou pas les Points
		{
			if (_spellPts < AncLevel)
				Logs.addToGameLog(_name + " n'a pas les points requis pour booster le sort " + spellID + " " + _spellPts
						+ "/" + AncLevel);
			if (World.getSort(spellID).getStatsByLevel(AncLevel + 1).getReqLevel() > _lvl)
				Logs.addToGameLog(_name + " n'a pas le niveau pour booster le sort " + spellID + " " + _lvl + "/"
						+ World.getSort(spellID).getStatsByLevel(AncLevel + 1).getReqLevel());
			return false;
		}
	}
	
	public void unlearAllSpells() {
		for(SortStats spell : this.getSorts()){
			forgetSpell(spell.getSpellID());
		}
    }

	public boolean forgetSpell(int spellID) {
		if (getSortStatBySortIfHas(spellID) == null) {
			Logs.addToGameLog(_name + " n'a pas le sort " + spellID);
			return false;
		}
		int AncLevel = getSortStatBySortIfHas(spellID).getLevel();
		if (AncLevel <= 1)
			return false;

		if (learnSpell(spellID, 1, true, false)) {
			_spellPts += Formulas.spellCost(AncLevel);

			SQLManager.SAVE_PERSONNAGE(this, false);
			return true;
		} else {
			Logs.addToGameLog(_name + " : Echec LearnSpell " + spellID);
			return false;
		}

	}

	public String parseSpellList() {
		StringBuilder packet = new StringBuilder();
		packet.append("SL");
		for (Iterator<SortStats> i = _sorts.values().iterator(); i.hasNext();) {
			SortStats SS = i.next();
			if ((SS.getSpellID() == 415 || SS.getSpellID() == 411) && this.getGrade() == 0) {
				this.forgetSpell(SS.getSpellID());
				continue;
			}
			packet.append(SS.getSpellID()).append("~").append(SS.getLevel()).append("~")
					.append(_sortsPlaces.get(SS.getSpellID())).append(";");
		}
		return packet.toString();
	}

	public void set_SpellPlace(int SpellID, char Place) {
		replace_SpellInBook(Place);
		_sortsPlaces.remove(SpellID);
		_sortsPlaces.put(SpellID, Place);
		SQLManager.SAVE_PERSONNAGE(this, false);// On sauvegarde les changements
	}

	private void replace_SpellInBook(char Place) {
		for (int key : _sorts.keySet()) {
			if (_sortsPlaces.get(key) != null) {
				if (_sortsPlaces.get(key).equals(Place)) {
					_sortsPlaces.remove(key);
				}
			}
		}
	}

	public SortStats getSortStatBySortIfHas(int spellID) {
		return _sorts.get(spellID);
	}

	public boolean hasSpell(int spellID) {
		return (getSortStatBySortIfHas(spellID) == null ? false : true);
	}

	public String parseALK() {
		StringBuilder perso = new StringBuilder();
		perso.append("|");
		perso.append(this._GUID).append(";");
		perso.append(this._name).append(";");
		perso.append(this._lvl).append(";");
		perso.append(this._gfxID).append(";");
		perso.append((this._color1 != -1 ? Integer.toHexString(this._color1) : "-1")).append(";");
		perso.append((this._color2 != -1 ? Integer.toHexString(this._color2) : "-1")).append(";");
		perso.append((this._color3 != -1 ? Integer.toHexString(this._color3) : "-1")).append(";");
		perso.append(getGMStuffString()).append(";");
		perso.append((this.is_showSeller() ? 1 : 0)).append(";");
		perso.append("1;");
		perso.append(";");// DeathCount this.deathCount;
		perso.append(";");// LevelMax
		return perso.toString();
	}

	public void remove() {
		SQLManager.DELETE_PERSO_IN_BDD(this);
	}

	public void OnJoinGame() {
		if (_compte.getGameThread() == null)
			return;
		GameSendThread out = _compte.getGameThread().get_out();
		_compte.setCurPerso(this);
		_isOnline = true;
		
		if(this.is_showSeller()){
			this.set_showSeller(false);
			World.removeSeller(this.get_GUID(), this.get_curCarte().get_id());
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.get_curCarte(), this.get_GUID());
			SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(this.get_curCarte(), this);
			SocketManager.GAME_SEND_ALTER_GM_PACKET(this.get_curCarte(), this);
			Maps map = this._curCarte;
			for (Characters z : map.getPersos()) {
				if (z != null && z.isOnline()) {
					SocketManager.GAME_SEND_MAP_GMS_PACKETS(map, z);
					SocketManager.GAME_SEND_MERCHANT_LIST(z, map.get_id());
				}
			}
		}

		if (_mount != null){
			SocketManager.GAME_SEND_Rx_PACKET(this);
		}

		SocketManager.GAME_SEND_ASK(out, this);
		// Envoie des bonus pano si besoin
		for (int a = 1; a < World.getItemSetNumber(); a++) {
			int num = getNumbEquipedItemOfPanoplie(a);
			if (num == 0)
				continue;
			SocketManager.GAME_SEND_OS_PACKET(this, a);
		}

		// envoie des donn�es de m�tier
		if (_metiers.size() > 0) {
			ArrayList<StatsMetier> list = new ArrayList<StatsMetier>();
			list.addAll(_metiers.values());
			// packet JS
			SocketManager.GAME_SEND_JS_PACKET(this, list);
			// packet JX
			SocketManager.GAME_SEND_JX_PACKET(this, list);
			// Packet JO (Job Option)
			SocketManager.GAME_SEND_JO_PACKET(this, list);
			Objects obj = getObjetByPos(Constant.ITEM_POS_ARME);
			if (obj != null) {
				for (StatsMetier sm : list)
					if (sm.getTemplate().isValidTool(obj.getTemplate().getID()))
						SocketManager.GAME_SEND_OT_PACKET(_compte.getGameThread().get_out(), sm.getTemplate().getId());
			}
		}
		// Fin m�tier
		SocketManager.GAME_SEND_ALIGNEMENT(out, _align);
		SocketManager.GAME_SEND_ADD_CANAL(out, _canaux + "^" + (_compte.get_gmLvl() > 0 ? "@�" : ""));
		if (_guildMember != null)
			SocketManager.GAME_SEND_gS_PACKET(this, _guildMember);
		SocketManager.GAME_SEND_ZONE_ALLIGN_STATUT(out);
		SocketManager.GAME_SEND_SPELL_LIST(this);
		SocketManager.GAME_SEND_EMOTE_LIST(this, _emotes, "0");
		SocketManager.GAME_SEND_RESTRICTIONS(out);
		SocketManager.GAME_SEND_Ow_PACKET(this);
		SocketManager.GAME_SEND_SEE_FRIEND_CONNEXION(out, _showFriendConnection);
		SocketManager.GAME_SEND_SB_PACKET(this, this.getTotalBoostSpellStats(), true);
		this._compte.SendOnline();

		// Messages de bienvenue
		SocketManager.GAME_SEND_Im_PACKET(this, "189");
		if (!_compte.getLastConnectionDate().equals("") && !_compte.get_lastIP().equals(""))
			SocketManager.GAME_SEND_Im_PACKET(this,
					"0152;" + _compte.getLastConnectionDate() + "~" + _compte.get_lastIP());
		SocketManager.GAME_SEND_Im_PACKET(this, "0153;" + _compte.get_curIP());

		// Fin messages
		// Actualisation de l'ip
		_compte.setLastIP(_compte.get_curIP());

		// Mise a jour du lastConnectionDate
		Date actDate = new Date();
		DateFormat dateFormat = new SimpleDateFormat("dd");
		String jour = dateFormat.format(actDate);
		dateFormat = new SimpleDateFormat("MM");
		String mois = dateFormat.format(actDate);
		dateFormat = new SimpleDateFormat("yyyy");
		String annee = dateFormat.format(actDate);
		dateFormat = new SimpleDateFormat("HH");
		String heure = dateFormat.format(actDate);
		dateFormat = new SimpleDateFormat("mm");
		String min = dateFormat.format(actDate);
		_compte.setLastConnectionDate(annee + "~" + mois + "~" + jour + "~" + heure + "~" + min);
		if (_guildMember != null)
			_guildMember.setLastCo(annee + "~" + mois + "~" + jour + "~" + heure + "~" + min);
		// Affichage des prismes
		//World.showPrismes(this);
		// Actualisation dans la DB
		SQLManager.UPDATE_LASTCONNECTION_INFO(_compte);

		//Envoi du message de bienvenue personnalis� 
		if (!Config.CONFIG_MOTD.equals(""))
			SocketManager.GAME_SEND_MESSAGE(this, Config.CONFIG_MOTD, Colors.BLACK_BLUE);

		if (_fight == null) {
			// on d�marre le Timer pour la Regen de Pdv
			_sitTimer.start();
			// on le demarre cot� org.walaka.rubrumsolem.client
			SocketManager.GAME_SEND_ILS_PACKET(this, 2000);
		}
		Stalk.notifyToOwner(this, Stalk.CONNEXION);

		if (get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)// Non abo
		{
			if (_curCarte.getSubArea().get_subscribe())// Se connecte dans une zone abo
			{
				// On le place a sa statue
				_curCarte = World.getCarte(Constant.getClassStatueMap(get_classe()));
				_curCell = World.getCarte(Constant.getClassStatueMap(get_classe()))
						.getCase(Constant.getClassStatueCell(get_classe()));
			}

			Objects obj = getObjetByPos(Constant.ITEM_POS_FAMILIER);
			if (obj != null)// Familier
			{
				obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				SocketManager.GAME_SEND_OBJET_MOVE_PACKET(this, obj);
			}

			if (isOnMount())// Pas de monture non plus
			{
				toogleOnMount();
			}
		}
		
		// Envoie d'un message � tout les joueurs si un staff ce connecte
		/*
		if(_compte.get_gmLvl() > 0){
			String clicker_name = "<a href='asfunction:onHref,ShowPlayerPopupMenu," + get_name() + "'>" + get_name() + "</a>";
			SocketManager.GAME_SEND_Im_PACKET_TO_ALL("116;<b>Serveur</b>~Le membre du staff <b>" + clicker_name + "</b> vient de se connecter.");
		}
		*/
	}

	public void SetSeeFriendOnline(boolean bool) {
		_showFriendConnection = bool;
	}

	public void sendGameCreate() {
		if (_compte.getGameThread() == null)
			return;
		GameSendThread out = _compte.getGameThread().get_out();

		if (is_showSeller() == true && World.getSeller(get_curCarte().get_id()) != null
				&& World.getSeller(get_curCarte().get_id()).contains(get_GUID())) {
			World.removeSeller(get_GUID(), get_curCarte().get_id());
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(get_curCarte(), get_GUID());
			set_showSeller(false);
		}
		SQLManager.SETONLINE(_compte.get_GUID());
		SocketManager.GAME_SEND_GAME_CREATE(out, _name);
		SocketManager.GAME_SEND_STATS_PACKET(this);
		if (_fight != null) {
			if (_fight.getFighterByPerso(this) != null && !_fight.getFighterByPerso(this).isDeconnected()
					&& _fight.get_state() != Constant.FIGHT_STATE_FINISHED)
				return;
			SocketManager.GAME_SEND_MAPDATA(out, _curCarte.get_id(), _curCarte.get_date(), _curCarte.get_key());
			if (_fight.reconnexion(this))
				return;
		}

		SocketManager.GAME_SEND_MAPDATA(out, _curCarte.get_id(), _curCarte.get_date(), _curCarte.get_key());
		SocketManager.GAME_SEND_MAP_FIGHT_COUNT(out, this.get_curCarte());
		_curCarte.addPlayer(this);
	}

	public String parseToOa() {
		StringBuilder packetOa = new StringBuilder();
		packetOa.append("Oa").append(_GUID).append("|").append(getGMStuffString());
		return packetOa.toString();
	}

	public String parseToGM() {
		StringBuilder str = new StringBuilder();
		if (_fight == null)// Hors combat
		{
			str.append(_curCell.getID()).append(";").append(_orientation).append(";");
			str.append("0").append(";");// FIXME:?
			str.append(_GUID).append(";").append(_tag != null ? _tag + _name : _name).append(";").append(_classe);
			str.append((this.get_title() > 0 ? ("," + this.get_title() + ";") : (";")));
			str.append(_gfxID).append("^").append(_size) // gfxID^size //FIXME ,GFXID pnj suiveur
					.append(",").append(_followers).append(";");
			str.append(_sexe).append(";").append(_align).append(",");// 1,0,0,4055064
			str.append("0,");// FIXME:?
			str.append((_showWings ? getGrade() : "0")).append(",");
			str.append(_lvl + _GUID);
			if (_showWings && _deshonor > 0)
				str.append(",1");
			str.append(";"); // Ne pas me demander qui est l'imb�cile qui a eu la brillante id�e d'ajouter l'ID...
			str.append((_color1 == -1 ? "-1" : Integer.toHexString(_color1))).append(";");
			str.append((_color2 == -1 ? "-1" : Integer.toHexString(_color2))).append(";");
			str.append((_color3 == -1 ? "-1" : Integer.toHexString(_color3))).append(";");
			str.append(getGMStuffString()).append(";");
			if (Config.AURA_SYSTEM) {
				if (hasEquiped(10054) || hasEquiped(10055) || hasEquiped(10056) || hasEquiped(10058)
						|| hasEquiped(10061) || hasEquiped(10102)) {
					str.append(3).append(";");
				} else {
					str.append((_lvl > 159 ? (_lvl > 199 ? (2) : (1)) : (0))).append(";");
				}
			} else 
				str.append("0;");
			
			str.append(";");// Emote
			str.append(";");// Emote timer
			if (this._guildMember != null && this._guildMember.getGuild().getMembers().size() >= Config.GUILD_VIEW) {
				str.append(this._guildMember.getGuild().get_name()).append(";")
						.append(this._guildMember.getGuild().get_emblem()).append(";");
			} else
				str.append(";;");
			str.append(get_Speed()).append(";");// Restriction
			str.append((_onMount && _mount != null ? _mount.get_color(parsecolortomount()) : "")).append(";");
			str.append(";");
		}
		return str.toString();
	}

	public String parsecolortomount() {
		return (this._color1 == -1 ? "" : Integer.toHexString(this._color1)) + ","
				+ (this._color2 == -1 ? "" : Integer.toHexString(this._color2)) + ","
				+ (this._color3 == -1 ? "" : Integer.toHexString(this._color3));
	}

	public String parseToMerchant() {
		StringBuilder str = new StringBuilder();
		str.append(_curCell.getID()).append(";");
		str.append(_orientation).append(";");
		str.append("0").append(";");
		str.append(_GUID).append(";");
		str.append(_name).append(";");
		str.append("-5").append(";");// Merchant identifier
		str.append(_gfxID).append("^").append(_size).append(";");
		str.append((_color1 == -1 ? "-1" : Integer.toHexString(_color1))).append(";");
		str.append((_color2 == -1 ? "-1" : Integer.toHexString(_color2))).append(";");
		str.append((_color3 == -1 ? "-1" : Integer.toHexString(_color3))).append(";");
		str.append(getGMStuffString()).append(";");// acessories
		str.append((_guildMember != null ? _guildMember.getGuild().get_name() : "")).append(";");// guildName
		str.append((_guildMember != null ? _guildMember.getGuild().get_emblem() : "")).append(";");// emblem
		str.append("0;");// offlineType

		return str.toString();
	}

	public String getGMStuffString() {
		StringBuilder str = new StringBuilder();
		// pour chaque place d'objet apparent, en Hexa, l'ID de l'objet
		// 2411 pour la Cape Objivejan (9233)
		// 2412 pour la Coiffe Objivejan (9234)
		if (getObjetByPos(Constant.ITEM_POS_ARME) != null)
			str.append(Integer.toHexString(getObjetByPos(Constant.ITEM_POS_ARME).getTemplate().getID()));
		str.append(",");
		if (getObjetByPos(Constant.ITEM_POS_COIFFE) != null) {
			if (getObjetByPos(Constant.ITEM_POS_COIFFE).getObvijevanPos() != 10)
				str.append(Integer.toHexString(getObjetByPos(Constant.ITEM_POS_COIFFE).getTemplate().getID()));
			else
				str.append(Integer.toHexString(getObjetByPos(Constant.ITEM_POS_COIFFE).getObviID()) + "~16~")
						.append(getObjetByPos(Constant.ITEM_POS_COIFFE).getObvijevanLook());
		}
		str.append(",");
		if (getObjetByPos(Constant.ITEM_POS_CAPE) != null) {
			if (getObjetByPos(Constant.ITEM_POS_CAPE).getObvijevanPos() != 11)
				str.append(Integer.toHexString(getObjetByPos(Constant.ITEM_POS_CAPE).getTemplate().getID()));
			else
				str.append(Integer.toHexString(getObjetByPos(Constant.ITEM_POS_CAPE).getObviID()) + "~17~")
						.append(getObjetByPos(Constant.ITEM_POS_CAPE).getObvijevanLook());
		}
		str.append(",");
		if (getObjetByPos(Constant.ITEM_POS_FAMILIER) != null)
			str.append(Integer.toHexString(getObjetByPos(Constant.ITEM_POS_FAMILIER).getTemplate().getID()));
		str.append(",");
		if (getObjetByPos(Constant.ITEM_POS_BOUCLIER) != null)
			str.append(Integer.toHexString(getObjetByPos(Constant.ITEM_POS_BOUCLIER).getTemplate().getID()));
		return str.toString();
	}

	public String getAsPacket() {
		refreshStats();

		StringBuilder ASData = new StringBuilder();
		ASData.append("As").append(xpString(",")).append("|");
		ASData.append(_kamas).append("|").append(_capital).append("|").append(_spellPts).append("|");
		ASData.append(_align).append("~").append(_align).append(",").append(_aLvl).append(",").append(getGrade())
				.append(",").append(_honor).append(",").append(_deshonor + ",").append((_showWings ? "1" : "0"))
				.append("|");

		int pdv = get_PDV();
		int pdvMax = get_PDVMAX();

		if (_fight != null) {
			Fighter f = _fight.getFighterByPerso(this);
			if (f != null) {
				pdv = f.getPDV();
				pdvMax = f.getPDVMAX();
			}
		}

		ASData.append(pdv).append(",").append(pdvMax).append("|");
		ASData.append(_energy).append(",10000|");

		ASData.append(getInitiative()).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_PROS) + getStuffStats().getEffect(Constant.STATS_ADD_PROS)
				+ ((int) Math.ceil(_baseStats.getEffect(Constant.STATS_ADD_CHAN) / 10))
				+ getBuffsStats().getEffect(Constant.STATS_ADD_PROS)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_PA)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_PA)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_ADD_PA)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_PA)).append(",")
				.append(getTotalStats().getEffect(Constant.STATS_ADD_PA)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_PM)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_PM)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_ADD_PM)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_PM)).append(",")
				.append(getTotalStats().getEffect(Constant.STATS_ADD_PM)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_FORC)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_FORC)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_ADD_FORC)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_FORC)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_VITA)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_VITA)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_ADD_VITA)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_VITA)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_SAGE)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_SAGE)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_ADD_SAGE)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_SAGE)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_CHAN)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_CHAN)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_ADD_CHAN)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_CHAN)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_AGIL)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_AGIL)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_ADD_AGIL)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_AGIL)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_INTE)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_INTE)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_ADD_INTE)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_INTE)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_PO)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_PO)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_ADD_PO)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_PO)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_CREATURE)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_CREATURE)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_CREATURE)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_CREATURE)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_DOMA)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_DOMA)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_ADD_DOMA)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_DOMA)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_PDOM)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_PDOM)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_ADD_PDOM)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_PDOM)).append("|");
		ASData.append("0,0,0,0|");// Maitrise ?
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_PERDOM)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_PERDOM))
				.append("," + getDonsStats().getEffect(Constant.STATS_ADD_PERDOM)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_PERDOM)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_SOIN)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_SOIN)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_ADD_SOIN)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_SOIN)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_TRAPDOM)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_TRAPDOM)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_TRAPDOM)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_TRAPDOM)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_TRAPPER)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_TRAPPER)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_TRAPPER)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_TRAPPER)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_RETDOM)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_RETDOM)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_RETDOM)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_RETDOM)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_CC)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_CC)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_ADD_CC)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_CC)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_EC)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_EC)).append(",")
				.append(getDonsStats().getEffect(Constant.STATS_ADD_EC)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_EC)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_AFLEE)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_AFLEE)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_AFLEE)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_AFLEE)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_MFLEE)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_MFLEE)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_MFLEE)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_MFLEE)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_R_NEU)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_R_NEU)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_NEU)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_NEU)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_RP_NEU)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_RP_NEU)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_NEU)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_NEU)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_R_PVP_NEU)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_R_PVP_NEU)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_PVP_NEU)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_PVP_NEU)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_RP_PVP_NEU)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_RP_PVP_NEU)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_PVP_NEU)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_PVP_NEU)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_R_TER)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_R_TER)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_TER)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_TER)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_RP_TER)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_RP_TER)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_TER)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_TER)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_R_PVP_TER)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_R_PVP_TER)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_PVP_TER)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_PVP_TER)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_RP_PVP_TER)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_RP_PVP_TER)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_PVP_TER)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_PVP_TER)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_R_EAU)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_R_EAU)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_EAU)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_EAU)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_RP_EAU)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_RP_EAU)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_EAU)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_EAU)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_R_PVP_EAU)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_R_PVP_EAU)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_PVP_EAU)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_PVP_EAU)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_RP_PVP_EAU)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_RP_PVP_EAU)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_PVP_EAU)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_PVP_EAU)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_R_AIR)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_R_AIR)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_AIR)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_AIR)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_RP_AIR)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_RP_AIR)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_AIR)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_AIR)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_R_PVP_AIR)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_R_PVP_AIR)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_PVP_AIR)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_PVP_AIR)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_RP_PVP_AIR)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_RP_PVP_AIR)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_PVP_AIR)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_PVP_AIR)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_R_FEU)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_R_FEU)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_FEU)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_FEU)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_RP_FEU)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_RP_FEU)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_FEU)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_FEU)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_R_PVP_FEU)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_R_PVP_FEU)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_PVP_FEU)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_R_PVP_FEU)).append("|");
		ASData.append(_baseStats.getEffect(Constant.STATS_ADD_RP_PVP_FEU)).append(",")
				.append(getStuffStats().getEffect(Constant.STATS_ADD_RP_PVP_FEU)).append(",").append(0).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_PVP_FEU)).append(",")
				.append(getBuffsStats().getEffect(Constant.STATS_ADD_RP_PVP_FEU)).append("|");

		return ASData.toString();
	}

	public int getGrade() {
		if (_align == Constant.ALIGNEMENT_NEUTRE)
			return 0;
		if (_honor >= 17500)
			return 10;
		for (int n = 1; n <= 10; n++) {
			if (_honor < World.getExpLevel(n).pvp)
				return n - 1;
		}
		return 0;
	}

	public String xpString(String c) {
		return _curExp + c + World.getPersoXpMin(_lvl) + c + World.getPersoXpMax(_lvl);
	}

	public int emoteActive() {
		return _emoteActive;
	}

	public void setEmoteActive(int emoteActive) {
		this._emoteActive = emoteActive;
	}

	public Stats getStuffStats() {
		Stats stats = new Stats(false, null);
		ArrayList<Integer> itemSetApplied = new ArrayList<Integer>();

		for (Entry<Integer, Objects> entry : _items.entrySet()) {
			if (entry.getValue().getPosition() != Constant.ITEM_POS_NO_EQUIPED) {
				stats = Stats.cumulStat(stats, entry.getValue().getStats());
				int panID = entry.getValue().getTemplate().getPanopID();
				// Si panoplie, et si l'effet de pano n'a pas encore ete ajout�
				if (panID > 0 && !itemSetApplied.contains(panID)) {
					itemSetApplied.add(panID);
					ItemSet IS = World.getItemSet(panID);
					// Si la pano existe
					if (IS != null) {
						// on ajoute le bonus de pano en fonction du nombre d'item
						stats = Stats.cumulStat(stats,
								IS.getBonusStatByItemNumb(this.getNumbEquipedItemOfPanoplie(panID)));
					}
				}
			}
		}
		if (_onMount && _mount != null) {
			stats = Stats.cumulStat(stats, _mount.get_stats());
		}
		return stats;
	}

	public Stats getBuffsStats() {
		Stats stats = new Stats(false, null);
		for (Map.Entry<Integer, SpellEffect> entry : _buffs.entrySet()) {
			stats.addOneStat(entry.getValue().getEffectID(), entry.getValue().getValue());
		}
		return stats;
	}

	public int get_orientation() {
		return _orientation;
	}

	public void set_orientation(int _orientation) {
		this._orientation = _orientation;
	}

	public int getInitiative() {
		int fact = 4;
		int pvmax = _PDVMAX - Constant.getBasePdv(_classe);
		int pv = _PDV - Constant.getBasePdv(_classe);
		if (_classe == Constant.CLASS_SACRIEUR)
			fact = 8;
		double coef = pvmax / fact;

		coef += getStuffStats().getEffect(Constant.STATS_ADD_INIT);

		coef += getTotalStats().getEffect(Constant.STATS_ADD_AGIL);
		coef += getTotalStats().getEffect(Constant.STATS_ADD_CHAN);
		coef += getTotalStats().getEffect(Constant.STATS_ADD_INTE);
		coef += getTotalStats().getEffect(Constant.STATS_ADD_FORC);

		int init = 1;
		if (pvmax != 0)
			init = (int) (coef * ((double) pv / (double) pvmax));
		if (init < 0)
			init = 0;
		return init;
	}

	public BoostSpellStats getTotalBoostSpellStats() {
		BoostSpellStats total = new BoostSpellStats();

		for (Entry<Integer, Objects> entry : _items.entrySet()) {
			if (entry.getValue().getPosition() != Constant.ITEM_POS_NO_EQUIPED) {
				total.cumulStats(entry.getValue().getBoostSpellStats());
			}
		}

		return total;
	}

	public Stats getTotalStats() {
		return getTotalStats(false);
	}

	public Stats getTotalStats(boolean noBuff) {
		Stats total = new Stats(false, null);
		total = Stats.cumulStat(total, _baseStats);
		total = Stats.cumulStat(total, getStuffStats());
		total = Stats.cumulStat(total, getDonsStats());
		if (_fight != null) {
			if (_fight.get_type() == Constant.FIGHT_TYPE_AGRESSION || _fight.get_type() == Constant.FIGHT_TYPE_PVT
					|| _fight.get_type() == Constant.FIGHT_TYPE_CHALLENGE
					|| _fight.get_type() == Constant.FIGHT_TYPE_CONQUETE
					|| _fight.get_type() == Constant.FIGHT_TYPE_PVM) {
				int cur_pa = total.getEffect(Constant.STATS_ADD_PA);
				if (cur_pa > 12)
					total.addOneStat(Constant.STATS_ADD_PA, 12 - cur_pa);
				int cur_pm = total.getEffect(Constant.STATS_ADD_PM);
				if (cur_pm > 6)
					total.addOneStat(Constant.STATS_ADD_PM, 6 - cur_pm);
				int cur_po = total.getEffect(Constant.STATS_ADD_PO);
				if (cur_po > 9)
					total.addOneStat(Constant.STATS_ADD_PO, 9 - cur_po);
			}
		}
		if (_fight == null && !noBuff)
			total = Stats.cumulStat(total, getBuffsStats());

		return total;
	}

	public void sendLimitationIm() {
		if (_fight != null) {
			if (_fight.get_type() == Constant.FIGHT_TYPE_AGRESSION || _fight.get_type() == Constant.FIGHT_TYPE_PVT
					|| _fight.get_type() == Constant.FIGHT_TYPE_CHALLENGE
					|| _fight.get_type() == Constant.FIGHT_TYPE_CONQUETE
					|| _fight.get_type() == Constant.FIGHT_TYPE_PVM) {
				Stats total = new Stats(false, null);
				total = Stats.cumulStat(total, _baseStats);
				total = Stats.cumulStat(total, getStuffStats());
				total = Stats.cumulStat(total, getDonsStats());
				if (total.getEffect(Constant.STATS_ADD_PA) > 12 || total.getEffect(Constant.STATS_ADD_PM) > 6
						|| total.getEffect(Constant.STATS_ADD_PO) > 9) {
					SocketManager.GAME_SEND_Im_PACKET(this, "16;<b>Serveur</b>~Vous avez �t� soumis � la limitation PA/PM.");
				}
			}
		}
	}

	public Stats getDonsStats() {
		/* TODO */
		Stats stats = new Stats(false, null);
		return stats;
	}

	public int getPodUsed() {
		int pod = 0;
		for (Entry<Integer, Objects> entry : _items.entrySet()) {
			pod += entry.getValue().getTemplate().getPod() * entry.getValue().getQuantity();
		}
		return pod;
	}

	public int getMaxPod() {
		int pods = getTotalStats().getEffect(Constant.STATS_ADD_PODS);
		pods += getTotalStats().getEffect(Constant.STATS_ADD_FORC) * 5;
		for (StatsMetier SM : _metiers.values()) {
			pods += SM.get_lvl() * 5;
			if (SM.get_lvl() == 100)
				pods += 1000;
		}
		return pods;
	}

	public int get_PDV() {
		return _PDV;
	}

	public void set_PDV(int _pdv) {
		_PDV = _pdv;
		if (_fight != null) {
			if (_fight.getFighterByPerso(this) != null && _fight.get_state() == Constant.FIGHT_STATE_PLACE) {
				_fight.getFighterByPerso(this).set_PDV(_pdv);
			}
		}
		if (_group != null) {
			SocketManager.GAME_SEND_PM_MOD_PACKET_TO_GROUP(_group, this);
		}
	}

	public int get_PDVMAX() {
		return _PDVMAX;
	}

	public void set_PDVMAX(int _pdvmax) {
		_PDVMAX = _pdvmax;
		if (_fight != null) {
			if (_fight.getFighterByPerso(this) != null && _fight.get_state() == Constant.FIGHT_STATE_PLACE) {
				_fight.getFighterByPerso(this).set_PDVMAX(_pdvmax);
			}
		}
		if (_group != null) {
			SocketManager.GAME_SEND_PM_MOD_PACKET_TO_GROUP(_group, this);
		}
	}

	public void actualizarInfoGrupo() {
		if (_group != null)
			SocketManager.GAME_SEND_PM_MOD_PACKET_TO_GROUP(_group, this);
	}

	public void setSitted(boolean b) {
		_sitted = b;
		int diff = _PDV - _exPdv;
		int time = (b ? 1000 : 2000);

		_exPdv = _PDV;
		if (_isOnline) {
			// On envoie le message "Vous avez recuperer X pdv"
			SocketManager.GAME_SEND_ILF_PACKET(this, diff);
			// On envoie la modif du Timer de regenPdv cot�
			// org.walaka.rubrumsolem.client
			SocketManager.GAME_SEND_ILS_PACKET(this, time);
		}
		// on modifie le delay cot� Serveur du timer de regenPDV
		_sitTimer.setDelay(time);
		// Si on se leve, on desactive l'�mote
		if ((_emoteActive == 1 || _emoteActive == 19) && b == false)
			_emoteActive = 0;
	}

	public byte get_align() {
		return _align;
	}

	public void set_follow(String follow) {
		_followers = follow;
		if (get_fight() == null)
			SocketManager.GAME_SEND_ALTER_GM_PACKET(get_curCarte(), this);
	}

	public void remove_follow() {
		_followers = "";
		if (get_fight() == null)
			SocketManager.GAME_SEND_ALTER_GM_PACKET(get_curCarte(), this);
	}

	public String get_followers() {
		return _followers;
	}

	public void setMapProt(Maps Carte) {
		_curCarteDefPerco = Carte;
	}

	public void setCellProt(Case celda) {
		_curCellDefPerco = celda;
	}

	public Case getCeldaDefPerco() {
		return _curCellDefPerco;
	}

	public Maps getCarteDefPerco() {
		return _curCarteDefPerco;
	}

	public int get_pdvper() {
		int pdvper = 100;
		pdvper = (100 * _PDV) / _PDVMAX;
		return pdvper;
	}

	public void emoticone(String str) {
		try {
			int id = Integer.parseInt(str);
			Maps map = _curCarte;
			if (_fight == null)
				SocketManager.GAME_SEND_EMOTICONE_TO_MAP(map, _GUID, id);
			else
				SocketManager.GAME_SEND_EMOTICONE_TO_FIGHT(_fight, 7, _GUID, id);
		} catch (NumberFormatException e) {
			return;
		}
		;
	}

	public void refreshMapAfterFight() {
		if (_compte.isBanned()) {
			if (_compte.getGameThread() != null){
				SocketManager.REALM_SEND_MESSAGE(_compte.getGameThread().get_out(), "18|le serveur;<br />Ton compte a �t� banni.");
				_compte.getGameThread().kick();
			}
		}
		_curCarte.addPlayer(this);
		if (_compte.getGameThread() != null && _compte.getGameThread().get_out() != null) {
			SocketManager.GAME_SEND_STATS_PACKET(this);
			SocketManager.GAME_SEND_ILS_PACKET(this, 1000);
		}
		_hasEndfight = true;
		_fight = null;
		_away = false;
	}

	public void boostStat(int stat) {
		Logs.addToGameLog("Perso " + _name + ": tentative de boost stat " + stat);
		int value = 0;
		switch (stat) {
		case 10:// Force
			value = _baseStats.getEffect(Constant.STATS_ADD_FORC);
			break;
		case 13:// Chance
			value = _baseStats.getEffect(Constant.STATS_ADD_CHAN);
			break;
		case 14:// Agilit�
			value = _baseStats.getEffect(Constant.STATS_ADD_AGIL);
			break;
		case 15:// Intelligence
			value = _baseStats.getEffect(Constant.STATS_ADD_INTE);
			break;
		}
		int cout = Constant.getReqPtsToBoostStatsByClass(_classe, stat, value);
		if (cout <= _capital) {
			switch (stat) {
			case 11:// Vita
				if (_classe != Constant.CLASS_SACRIEUR)
					_baseStats.addOneStat(Constant.STATS_ADD_VITA, 1);
				else
					_baseStats.addOneStat(Constant.STATS_ADD_VITA, 2);
				break;
			case 12:// Sage
				_baseStats.addOneStat(Constant.STATS_ADD_SAGE, 1);
				break;
			case 10:// Force
				_baseStats.addOneStat(Constant.STATS_ADD_FORC, 1);
				break;
			case 13:// Chance
				_baseStats.addOneStat(Constant.STATS_ADD_CHAN, 1);
				break;
			case 14:// Agilit�
				_baseStats.addOneStat(Constant.STATS_ADD_AGIL, 1);
				break;
			case 15:// Intelligence
				_baseStats.addOneStat(Constant.STATS_ADD_INTE, 1);
				break;
			default:
				return;
			}
			_capital -= cout;
			SocketManager.GAME_SEND_STATS_PACKET(this);
			SQLManager.SAVE_PERSONNAGE(this, false);
		}
	}

	public boolean isMuted() {
		return _compte.isMuted();
	}

	public void set_curCarte(Maps carte) {
		_curCarte = carte;
	}

	public String parseObjetsToDB() {
		StringBuilder str = new StringBuilder();
		if (_items.isEmpty())
			return "";
		for (Entry<Integer, Objects> entry : _items.entrySet()) {
			Objects obj = entry.getValue();
			str.append(obj.getGuid()).append("|");
		}
		return str.toString();
	}

	public boolean addObjet(Objects newObj, boolean stackIfSimilar) {
		for (Entry<Integer, Objects> entry : _items.entrySet()) {
			Objects obj = entry.getValue();
			if (obj.getTemplate().getID() == newObj.getTemplate().getID()
					&& obj.getStats().isSameStats(newObj.getStats()) && stackIfSimilar
					&& newObj.getTemplate().getType() != 85 && obj.getPosition() == Constant.ITEM_POS_NO_EQUIPED
					&& newObj.getTemplate().getType() != Constant.ITEM_TYPE_FAMILIER)// Si meme Template et Memes Stats et Objet non �quip�
			{
				obj.setQuantity(obj.getQuantity() + newObj.getQuantity());// On ajoute QUA item a la quantit� de l'objet existant
				SQLManager.SAVE_ITEM(obj);
				if (_isOnline)
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, obj);
				return false;
			}
		}
		_items.put(newObj.getGuid(), newObj);
		SocketManager.GAME_SEND_OAKO_PACKET(this, newObj);
		return true;
	}

	public void addObjet(Objects newObj) {
		if (newObj == null)
			return;
		_items.put(newObj.getGuid(), newObj);
	}

	public Map<Integer, Objects> getItems() {
		return _items;
	}

	public String parseItemToASK() {
		StringBuilder str = new StringBuilder();
		if (_items.isEmpty())
			return "";
		for (Objects obj : _items.values()) {
			str.append(obj.parseItem());
		}
		return str.toString();
	}

	public String getBankItemsIDSplitByChar(String splitter) {
		StringBuilder str = new StringBuilder();
		if (_compte.getBank().isEmpty())
			return "";
		for (int entry : _compte.getBank().keySet()) {
			str.append(entry).append(splitter);
		}
		return str.toString();
	}

	public String getItemsIDSplitByChar(String splitter) {
		StringBuilder str = new StringBuilder();
		if (_items.isEmpty())
			return "";
		for (int entry : _items.keySet()) {
			if (str.length() != 0)
				str.append(splitter);
			str.append(entry);
		}
		return str.toString();
	}

	public String getStoreItemsIDSplitByChar(String splitter) {
		StringBuilder str = new StringBuilder();
		if (_storeItems.isEmpty())
			return "";
		for (int entry : _storeItems.keySet()) {
			if (str.length() != 0)
				str.append(splitter);
			str.append(entry);
		}
		return str.toString();
	}

	public Map<String, Integer> getLastPvPFighters() {
        return this.LastPvPFighters;
    }
	
    public void AddLastPvPFighters(String TeamId) {
        if(this.LastPvPFighters.containsKey(TeamId)) {
            this.LastPvPFighters.put(TeamId, this.LastPvPFighters.get(TeamId)+1);
        } else {
            this.LastPvPFighters.put(TeamId, 1);
        }
    }
    
    
    
	public boolean hasItemGuid(int guid) {
		return _items.get(guid) != null ? _items.get(guid).getQuantity() > 0 : false;
	}

	public void sellItem(int guid, int qua) {
		if (qua <= 0)
			return;
		if (_items.get(guid).getQuantity() < qua)// Si il a moins d'item que ce
													// qu'on veut Del
			qua = _items.get(guid).getQuantity();

		int pointsPrice = _items.get(guid).getTemplate().getPointsPrice();
		int prix = qua * (_items.get(guid).getTemplate().getPrix() / 10);// Calcul
																			// du
																			// prix
																			// de
																			// vente
																			// (prix
																			// d'achat/10)
		int newQua = _items.get(guid).getQuantity() - qua;

		if ((prix == 0) && (pointsPrice > 0)) {
			int totalPoints = qua * pointsPrice;
			int ActualAccPoints = SQLManager.GET_ACCOUNT_POINTS(getAccID());
			SQLManager.SET_ACCOUNT_POINTS(totalPoints + ActualAccPoints, _compte.get_GUID());
		}
		if (newQua <= 0)// Ne devrait pas etre <0, S'il n'y a plus d'item apres
						// la vente
		{
			_items.remove(guid);
			World.removeItem(guid);
			SQLManager.DELETE_ITEM(guid);
			SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, guid);
		} else// S'il reste des items apres la vente
		{
			_items.get(guid).setQuantity(newQua);
			SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, _items.get(guid));
		}
		_kamas = _kamas + prix;
		SocketManager.GAME_SEND_STATS_PACKET(this);
		SocketManager.GAME_SEND_Ow_PACKET(this);
		SocketManager.GAME_SEND_ESK_PACKEt(this);
	}

	public void removeItem(int guid) {
		_items.remove(guid);
	}

	public void removeItem(int guid, int nombre, boolean send, boolean deleteFromWorld) {
		Objects obj = _items.get(guid);

		if (nombre > obj.getQuantity())
			nombre = obj.getQuantity();

		if (obj.getQuantity() >= nombre) {
			int newQua = obj.getQuantity() - nombre;
			if (newQua > 0) {
				obj.setQuantity(newQua);
				if (send && _isOnline)
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, obj);
			} else {
				// on supprime de l'inventaire et du Monde
				_items.remove(obj.getGuid());
				if (deleteFromWorld)
					World.removeItem(obj.getGuid());
				// on envoie le packet si connect�
				if (send && _isOnline)
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, obj.getGuid());
			}
		}
	}

	public void deleteItem(int guid) {
		_items.remove(guid);
		World.removeItem(guid);
	}

	public Objects getObjetByPos(int pos) {
		if (pos == Constant.ITEM_POS_NO_EQUIPED)
			return null;

		for (Entry<Integer, Objects> entry : _items.entrySet()) {
			Objects obj = entry.getValue();
			if (obj.getPosition() == pos)
				return obj;
		}
		return null;
	}

	public int getObjetByPosSpece(int pos) {
		if (pos == Constant.ITEM_POS_NO_EQUIPED)
			return 0;

		for (Entry<Integer, Objects> entry : _items.entrySet()) {
			Objects obj = entry.getValue();
			if (obj.getPosition() == pos)
				return obj.getGuid();
		}
		return 0;
	}

	public void refreshStats() {
		double actPdvPer = (100 * (double) _PDV) / (double) _PDVMAX;
		set_PDVMAX((_lvl - 1) * 5 + Constant.getBasePdv(_classe) + getTotalStats().getEffect(Constant.STATS_ADD_VITA)
				+ getTotalStats().getEffect(Constant.STATS_ADD_VIE));
		set_PDV((int) Math.round(_PDVMAX * actPdvPer / 100));
	}

	public void levelUp(boolean send, boolean addXp) {
		if (_lvl == World.getExpLevelSize())
			return;
		_lvl++;
		_capital += 5;
		_spellPts++;
		_PDVMAX += 5;
		_PDV = _PDVMAX;
		if (_lvl == 100)
			_baseStats.addOneStat(Constant.STATS_ADD_PA, 1);
		Constant.onLevelUpSpells(this, _lvl);
		if (addXp)
			_curExp = World.getExpLevel(_lvl).perso;
		if (get_guild() != null) {
			SQLManager.UPDATE_GUILDMEMBER(getGuildMember());
			getGuildMember().setLevel(_lvl);
		}
		if (send && _isOnline && _compte != null && _compte.getGameThread() != null) {
			SocketManager.GAME_SEND_NEW_LVL_PACKET(_compte.getGameThread().get_out(), _lvl);
			SocketManager.GAME_SEND_STATS_PACKET(this);
			SocketManager.GAME_SEND_SPELL_LIST(this);
		}
	}

	public void addXp(long winxp) {
		_curExp += winxp;
		int exLevel = _lvl;
		while (_curExp >= World.getPersoXpMax(_lvl) && _lvl < World.getExpLevelSize())
			levelUp(false,
					false);/**
							 * @TODO Fix Generes uniquement une fentre au dernier
							 *       lv gagné pour eviter un crash clientnier
							 **/
		if (_isOnline) {
			if (exLevel < _lvl)
				SocketManager.GAME_SEND_NEW_LVL_PACKET(_compte.getGameThread().get_out(), _lvl);
			SocketManager.GAME_SEND_STATS_PACKET(this);
		}
	}

	public void addKamas(long l) {
		_kamas += l;
	}

	public Objects getSimilarItem(Objects exObj) {
		for (Entry<Integer, Objects> entry : _items.entrySet()) {
			Objects obj = entry.getValue();

			if (obj.getTemplate().getID() == exObj.getTemplate().getID() && obj.getStats().isSameStats(exObj.getStats())
					&& obj.getGuid() != exObj.getGuid() && obj.getPosition() == Constant.ITEM_POS_NO_EQUIPED
					&& obj.getTemplate().getType() != Constant.ITEM_TYPE_FAMILIER)
				return obj;
		}
		return null;
	}

	public void setCurExchange(Exchange echg) {
		_curExchange = echg;
	}

	public Exchange get_curExchange() {
		return _curExchange;
	}

	public int learnJob(Job m) {
		for (Entry<Integer, StatsMetier> entry : _metiers.entrySet()) {
			if (entry.getValue().getTemplate().getId() == m.getId())// Si le
																	// joueur a
																	// d�j�
																	// le
																	// m�tier
				return -1;
		}
		int Msize = _metiers.size();
		if (Msize == 6)// Si le joueur a d�j� 6 m�tiers
			return -1;
		int pos = 0;
		if (Constant.isMageJob(m.getId())) {
			if (_metiers.get(5) == null)
				pos = 5;
			if (_metiers.get(4) == null)
				pos = 4;
			if (_metiers.get(3) == null)
				pos = 3;
		} else {
			if (_metiers.get(2) == null)
				pos = 2;
			if (_metiers.get(1) == null)
				pos = 1;
			if (_metiers.get(0) == null)
				pos = 0;
		}

		StatsMetier sm = new StatsMetier(pos, m, 1, 0);
		_metiers.put(pos, sm);// On apprend le m�tier lvl 1 avec 0 xp
		if (_isOnline) {
			// on cr�er la listes des statsMetier a envoyer (Seulement celle
			// ci)
			ArrayList<StatsMetier> list = new ArrayList<StatsMetier>();
			list.add(sm);

			SocketManager.GAME_SEND_Im_PACKET(this, "02;" + m.getId());
			// packet JS
			SocketManager.GAME_SEND_JS_PACKET(this, list);
			// packet JX
			SocketManager.GAME_SEND_JX_PACKET(this, list);
			// Packet JO (Job Option)
			SocketManager.GAME_SEND_JO_PACKET(this, list);

			Objects obj = getObjetByPos(Constant.ITEM_POS_ARME);
			if (obj != null)
				if (sm.getTemplate().isValidTool(obj.getTemplate().getID()))
					SocketManager.GAME_SEND_OT_PACKET(_compte.getGameThread().get_out(), m.getId());
		}
		return pos;
	}

	public void unlearnJob(int m) {
		_metiers.remove(m);
	}

	public boolean hasEquiped(int id) {
		for (Entry<Integer, Objects> entry : _items.entrySet())
			if (entry.getValue().getTemplate().getID() == id
					&& entry.getValue().getPosition() != Constant.ITEM_POS_NO_EQUIPED)
				return true;
		return false;
	}

	public boolean hasEquipedGuid(int guid) {
		for (Entry<Integer, Objects> entry : _items.entrySet())
			if (entry.getValue().getGuid() == guid && entry.getValue().getPosition() != Constant.ITEM_POS_NO_EQUIPED)
				return true;
		return false;
	}

	public void setInvitation(int target) {
		_inviting = target;
	}

	public int getInvitation() {
		return _inviting;
	}

	public String parseToPM() {
		StringBuilder str = new StringBuilder();
		str.append(_GUID).append(";");
		str.append(_name).append(";");
		str.append(_gfxID).append(";");
		str.append(_color1).append(";");
		str.append(_color2).append(";");
		str.append(_color3).append(";");
		str.append(getGMStuffString()).append(";");
		str.append(_PDV).append(",").append(_PDVMAX).append(";");
		str.append(_lvl).append(";");
		str.append(getInitiative()).append(";");
		str.append(getTotalStats().getEffect(Constant.STATS_ADD_PROS)).append(";");
		str.append("0");// Side = ?
		return str.toString();
	}

	public int getNumbEquipedItemOfPanoplie(int panID) {
		int nb = 0;
		for (Entry<Integer, Objects> i : _items.entrySet()) {
			// On ignore les objets non �quip�s
			if (i.getValue().getPosition() == Constant.ITEM_POS_NO_EQUIPED)
				continue;
			// On prend que les items de la pano demand�e, puis on augmente le
			// nombre si besoin
			if (i.getValue().getTemplate().getPanopID() == panID)
				nb++;
		}
		return nb;
	}

	public void startActionOnCell(GameAction GA) {
		int cellID = -1;
		int action = -1;
		try {
			cellID = Integer.parseInt(GA._args.split(";")[0]);
			action = Integer.parseInt(GA._args.split(";")[1]);
		} catch (Exception e) {
		}
		;
		if (cellID == -1 || action == -1)
			return;
		// Si case invalide
		if (!_curCarte.getCase(cellID).canDoAction(action))
			return;
		_curCarte.getCase(cellID).startAction(this, GA);
	}

	public void finishActionOnCell(GameAction GA) {
		int cellID = -1;
		try {
			cellID = Integer.parseInt(GA._args.split(";")[0]);
		} catch (Exception e) {
		}
		;
		if (cellID == -1)
			return;
		if (_curCarte.getCase(cellID) == null)
			return;
		_curCarte.getCase(cellID).finishAction(this, GA);
	}

	public void teleport(short newMapID, int newCellID) {
		GameSendThread PW = null;
		if (_compte == null)
			return;
		if (_compte.getGameThread() != null) {
			PW = _compte.getGameThread().get_out();
		}
		if (World.getCarte(newMapID) == null) {
			Logs.addToGameLog("Game: INVALID MAP : " + newMapID);
			return;
		}
		if (World.getCarte(newMapID).getCase(newCellID) == null) {
			Logs.addToGameLog("Game: INVALID CELL : " + newCellID + " ON MAP : " + newMapID);
			return;
		}
		if (PW != null) {
			SocketManager.GAME_SEND_GA2_PACKET(PW, _GUID);
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_curCarte, _GUID);
		}
		_curCell.removePlayer(_GUID);
		_curCarte = World.getCarte(newMapID);
		_curCell = _curCarte.getCase(newCellID);

		// Verification de la carte
		// Verifier la validit� du mountpark
		if (_curCarte.getMountPark() != null 
				&& _curCarte.getMountPark().get_owner() > 0
				&& _curCarte.getMountPark().get_guild() == null) {
			Logs.addToGameLog("[MountPark] Suppression d'un enclos a guilde invalide : " + _curCarte.getMountPark().get_owner());
			Maps.MountPark.removeMapMountPark(_curCarte.get_id());
		}
		// Verifier la validit� du percepteur
		if (Collector.GetPercoByMapID(_curCarte.get_id()) != null) {
			if (World.getGuild(Collector.GetPercoByMapID(_curCarte.get_id()).get_guildID()) == null)// Ne devrait pas arriver
			{
				Logs.addToGameLog("[Percepteur] Suppression d'un Percepteur a Guild invalide. GuildID : "
						+ Collector.GetPercoByMapID(_curCarte.get_id()).get_guildID());
				Collector.removePercepteur(Collector.GetPercoByMapID(_curCarte.get_id()).get_guildID());
			}
		}

		if (PW != null) {
			SocketManager.GAME_SEND_MAPDATA(PW, newMapID, _curCarte.get_date(), _curCarte.get_key());
			_curCarte.addPlayer(this);
		}

		synchronized (_Follower) {
			if (!_Follower.isEmpty())// On met a jour la carte des personnages
										// qui nous suivent
			{
				for (Characters t : _Follower.values()) {
					if (t.isOnline())
						SocketManager.GAME_SEND_FLAG_PACKET(t, this);
					else
						_Follower.remove(t.get_GUID());

				}
			}
			if (get_compte().get_subscriber() == 0 && _curCarte.getSubArea().get_subscribe()
					&& get_compte().get_subscriberMessage() && Config.USE_SUBSCRIBE) {
				SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(this, "+10"); // Si le
																		// joueur
																		// p�netre
																		// dans
																		// une
																		// zone
																		// abo,
																		// on
																		// envoie
																		// le
																		// cadenas
				get_compte().set_subscriberMessage(false);
			} else {
				if (get_compte().get_subscriber() == 0 && _curCarte.getSubArea().get_subscribe() == false
						&& get_compte().get_subscriberMessage() == false && Config.USE_SUBSCRIBE) {
					SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(this, "0"); // Si
																			// le
																			// joueur
																			// reviens
																			// en
																			// zone
																			// non
																			// abo,
																			// on
																			// supprime
																			// le
																			// cadenas
					get_compte().set_subscriberMessage(true);
				}
			}
		}
	}

	public int getBankCost() {
		return _compte.getBank().size();
	}

	public String getStringVar(String str) {
		// TODO completer
		if (str.equals("name"))
			return _name;
		if (str.equals("bankCost")) {
			return getBankCost() + "";
		}
		return "";
	}

	public void setBankKamas(long i) {
		_compte.setBankKamas(i);
		SQLManager.UPDATE_ACCOUNT_DATA(_compte);
	}

	public long getBankKamas() {
		return _compte.getBankKamas();
	}

	public void setInBank(boolean b) {
		_isInBank = b;
	}

	public boolean isInBank() {
		return _isInBank;
	}

	public String parseBankPacket() {
		StringBuilder packet = new StringBuilder();
		for (Entry<Integer, Objects> entry : _compte.getBank().entrySet())
			packet.append("O").append(entry.getValue().parseItem()).append(";");
		if (getBankKamas() != 0)
			packet.append("G").append(getBankKamas());
		return packet.toString();
	}

	public void addCapital(int pts) {
		_capital += pts;
	}

	public void addSpellPoint(int pts) {
		_spellPts += pts;
	}

	public void addInBank(int guid, int qua) {
		Objects PersoObj = World.getObjet(guid);

		// Si le joueur n'a pas l'item dans son sac ...
		if (_items.get(guid) == null) {
			Logs.addToGameLog("Le joueur " + _name + " a tent� d'ajouter un objet en banque qu'il n'avait pas.");
			return;
		}
		if (_items.get(guid).getQuantity() >= qua) {
			// Si c'est un item �quip� ...
			if (PersoObj.getPosition() != Constant.ITEM_POS_NO_EQUIPED)
				return;

			Objects BankObj = getSimilarBankItem(PersoObj);
			int newQua = PersoObj.getQuantity() - qua;
			if (BankObj == null)// S'il n'y pas d'item du meme Template
			{
				// S'il ne reste pas d'item dans le sac
				if (newQua <= 0) {
					// On enleve l'objet du sac du joueur
					removeItem(PersoObj.getGuid());
					// On met l'objet du sac dans la banque, avec la meme
					// quantit�
					_compte.getBank().put(PersoObj.getGuid(), PersoObj);
					String str = "O+" + PersoObj.getGuid() + "|" + PersoObj.getQuantity() + "|"
							+ PersoObj.getTemplate().getID() + "|" + PersoObj.parseStatsString();
					SocketManager.GAME_SEND_EsK_PACKET(this, str);
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, guid);

				} else// S'il reste des objets au joueur
				{
					// on modifie la quantit� d'item du sac
					PersoObj.setQuantity(newQua);
					// On ajoute l'objet a la banque et au monde
					BankObj = Objects.getCloneObjet(PersoObj, qua);
					World.addObjet(BankObj, true);
					_compte.getBank().put(BankObj.getGuid(), BankObj);

					// Envoie des packets
					String str = "O+" + BankObj.getGuid() + "|" + BankObj.getQuantity() + "|"
							+ BankObj.getTemplate().getID() + "|" + BankObj.parseStatsString();
					SocketManager.GAME_SEND_EsK_PACKET(this, str);
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj);

				}
			} else // S'il y avait un item du meme template
			{
				// S'il ne reste pas d'item dans le sac
				if (newQua <= 0) {
					// On enleve l'objet du sac du joueur
					removeItem(PersoObj.getGuid());
					// On enleve l'objet du monde
					World.removeItem(PersoObj.getGuid());
					// On ajoute la quantit� a l'objet en banque
					BankObj.setQuantity(BankObj.getQuantity() + PersoObj.getQuantity());
					// on envoie l'ajout a la banque de l'objet
					String str = "O+" + BankObj.getGuid() + "|" + BankObj.getQuantity() + "|"
							+ BankObj.getTemplate().getID() + "|" + BankObj.parseStatsString();
					SocketManager.GAME_SEND_EsK_PACKET(this, str);
					// on envoie la supression de l'objet du sac au joueur
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, guid);

				} else // S'il restait des objets
				{
					// on modifie la quantit� d'item du sac
					PersoObj.setQuantity(newQua);
					BankObj.setQuantity(BankObj.getQuantity() + qua);
					String str = "O+" + BankObj.getGuid() + "|" + BankObj.getQuantity() + "|"
							+ BankObj.getTemplate().getID() + "|" + BankObj.parseStatsString();
					SocketManager.GAME_SEND_EsK_PACKET(this, str);
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj);

				}
			}
			SocketManager.GAME_SEND_Ow_PACKET(this);
			SQLManager.UPDATE_ACCOUNT_DATA(_compte);
		}
	}

	private Objects getSimilarBankItem(Objects obj) {
		for (Objects value : _compte.getBank().values()) {
			if (value.getTemplate().getType() == 85)
				continue;
			if (value.getTemplate().getID() == obj.getTemplate().getID()
					&& value.getStats().isSameStats(obj.getStats()))
				return value;
		}
		return null;
	}

	public void removeFromBank(int guid, int qua) {
		Objects BankObj = World.getObjet(guid);
		// Si le joueur n'a pas l'item dans sa banque ...
		if (_compte.getBank().get(guid) == null) {
			Logs.addToGameLog("Le joueur " + _name + " a tenter de retirer un objet en banque qu'il n'avait pas.");
			return;
		}

		if (_compte.getBank().get(guid).getQuantity() >= qua) {
			Objects PersoObj = getSimilarItem(BankObj);

			int newQua = BankObj.getQuantity() - qua;

			if (PersoObj == null)// Si le joueur n'avait aucun item similaire
			{
				// S'il ne reste rien en banque
				if (newQua <= 0) {
					// On retire l'item de la banque
					_compte.getBank().remove(guid);
					// On l'ajoute au joueur
					_items.put(guid, BankObj);

					// On envoie les packets
					SocketManager.GAME_SEND_OAKO_PACKET(this, BankObj);
					String str = "O-" + guid;
					SocketManager.GAME_SEND_EsK_PACKET(this, str);

				} else // S'il reste des objets en banque
				{
					// On cr�e une copy de l'item en banque
					PersoObj = Objects.getCloneObjet(BankObj, qua);
					// On l'ajoute au monde
					World.addObjet(PersoObj, true);
					// On retire X objet de la banque
					BankObj.setQuantity(newQua);
					// On l'ajoute au joueur
					_items.put(PersoObj.getGuid(), PersoObj);

					// On envoie les packets
					SocketManager.GAME_SEND_OAKO_PACKET(this, PersoObj);
					String str = "O+" + BankObj.getGuid() + "|" + BankObj.getQuantity() + "|"
							+ BankObj.getTemplate().getID() + "|" + BankObj.parseStatsString();
					SocketManager.GAME_SEND_EsK_PACKET(this, str);

				}
			} else {
				// S'il ne reste rien en banque
				if (newQua <= 0) {
					// On retire l'item de la banque
					_compte.getBank().remove(BankObj.getGuid());
					World.removeItem(BankObj.getGuid());
					// On Modifie la quantit� de l'item du sac du joueur
					PersoObj.setQuantity(PersoObj.getQuantity() + BankObj.getQuantity());

					// On envoie les packets
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj);
					String str = "O-" + guid;
					SocketManager.GAME_SEND_EsK_PACKET(this, str);

				} else// S'il reste des objets en banque
				{
					// On retire X objet de la banque
					BankObj.setQuantity(newQua);
					// On ajoute X objets au joueurs
					PersoObj.setQuantity(PersoObj.getQuantity() + qua);

					// On envoie les packets
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj);
					String str = "O+" + BankObj.getGuid() + "|" + BankObj.getQuantity() + "|"
							+ BankObj.getTemplate().getID() + "|" + BankObj.parseStatsString();
					SocketManager.GAME_SEND_EsK_PACKET(this, str);

				}
			}
			SocketManager.GAME_SEND_Ow_PACKET(this);
			SQLManager.UPDATE_ACCOUNT_DATA(_compte);
		}
	}

	public void openMountPark() {
		if (getDeshonor() >= 5) {
			SocketManager.GAME_SEND_Im_PACKET(this, "183");
			return;
		}

		_inMountPark = _curCarte.getMountPark();
		_away = true;
		String str = _inMountPark.parseData(get_GUID(), (_inMountPark.get_owner() == -1 ? true : false));

		if (_inMountPark.get_owner() == -1 || _inMountPark.get_owner() == this.get_GUID())// Public
																							// ou
																							// le
																							// proprio
		{
			SocketManager.GAME_SEND_ECK_PACKET(this, 16, str);
		} else if (get_guild() != null && World.getPersonnage(_inMountPark.get_owner()).get_guild() != null
				&& World.getPersonnage(_inMountPark.get_owner()).get_guild() == get_guild()
				&& getGuildMember().canDo(Constant.G_USEENCLOS))// Meme guilde +
																// droits
		{
			SocketManager.GAME_SEND_ECK_PACKET(this, 16, str);
		} else {
			SocketManager.GAME_SEND_Im_PACKET(this, "1101");
			_inMountPark = null;
			_away = false;
		}
	}

	public void leftMountPark() {
		if (_inMountPark == null)
			return;
		_inMountPark = null;
	}

	public MountPark getInMountPark() {
		return _inMountPark;
	}

	public void fullPDV() {
		_PDV = _PDVMAX;
	}

	public void warpToSavePos() {
		try {
			String[] infos = _savePos.split(",");
			teleport(Short.parseShort(infos[0]), Integer.parseInt(infos[1]));
		} catch (Exception e) {
		}
		;
	}

	public void removeByTemplateID(int tID, int count) {
		// Copie de la liste pour eviter les modif concurrentes
		ArrayList<Objects> list = new ArrayList<Objects>();
		list.addAll(_items.values());

		ArrayList<Objects> remove = new ArrayList<Objects>();
		int tempCount = count;

		// on verifie pour chaque objet
		for (Objects obj : list) {
			// Si mauvais TemplateID, on passe
			if (obj.getTemplate().getID() != tID)
				continue;

			if (obj.getQuantity() >= count) {
				int newQua = obj.getQuantity() - count;
				if (newQua > 0) {
					obj.setQuantity(newQua);
					if (_isOnline)
						SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, obj);
				} else {
					// on supprime de l'inventaire et du Monde
					_items.remove(obj.getGuid());
					World.removeItem(obj.getGuid());
					// on envoie le packet si connect�
					if (_isOnline)
						SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, obj.getGuid());
				}
				return;
			} else// Si pas assez d'objet
			{
				if (obj.getQuantity() >= tempCount) {
					int newQua = obj.getQuantity() - tempCount;
					if (newQua > 0) {
						obj.setQuantity(newQua);
						if (_isOnline)
							SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, obj);
					} else
						remove.add(obj);

					for (Objects o : remove) {
						// on supprime de l'inventaire et du Monde
						_items.remove(o.getGuid());
						World.removeItem(o.getGuid());
						// on envoie le packet si connect�
						if (_isOnline)
							SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, o.getGuid());
					}
				} else {
					// on r�duit le compteur
					tempCount -= obj.getQuantity();
					remove.add(obj);
				}
			}
		}
	}

	public Map<Integer, StatsMetier> getMetiers() {
		return _metiers;
	}

	public void doJobAction(int actionID, InteractiveObject object, GameAction GA, Case cell) {
		StatsMetier SM = getMetierBySkill(actionID);
		if (SM == null)
			return;
		SM.startAction(actionID, this, object, GA, cell);
	}

	public void finishJobAction(int actionID, InteractiveObject object, GameAction GA, Case cell) {
		StatsMetier SM = getMetierBySkill(actionID);
		if (SM == null)
			return;
		SM.endAction(actionID, this, object, GA, cell);
	}

	public String parseJobData() {
		StringBuilder str = new StringBuilder();
		if (_metiers.isEmpty())
			return "";
		for (StatsMetier SM : _metiers.values()) {
			if (str.length() > 0)
				str.append(";");
			str.append(SM.getTemplate().getId()).append(",").append(SM.getXp());
		}
		return str.toString();
	}

	public int totalJobBasic() {
		int i = 0;

		for (StatsMetier SM : _metiers.values()) {
			// Si c'est un m�tier 'basic' :
			if (SM.getTemplate().getId() == 2 || SM.getTemplate().getId() == 11 || SM.getTemplate().getId() == 13
					|| SM.getTemplate().getId() == 14 || SM.getTemplate().getId() == 15
					|| SM.getTemplate().getId() == 16 || SM.getTemplate().getId() == 17
					|| SM.getTemplate().getId() == 18 || SM.getTemplate().getId() == 19
					|| SM.getTemplate().getId() == 20 || SM.getTemplate().getId() == 24
					|| SM.getTemplate().getId() == 25 || SM.getTemplate().getId() == 26
					|| SM.getTemplate().getId() == 27 || SM.getTemplate().getId() == 28
					|| SM.getTemplate().getId() == 31 || SM.getTemplate().getId() == 36
					|| SM.getTemplate().getId() == 41 || SM.getTemplate().getId() == 56
					|| SM.getTemplate().getId() == 58 || SM.getTemplate().getId() == 60
					|| SM.getTemplate().getId() == 65) {
				i++;
			}
		}
		return i;
	}

	public int totalJobFM() {
		int i = 0;

		for (StatsMetier SM : _metiers.values()) {
			// Si c'est une sp�cialisation 'FM' :
			if (SM.getTemplate().getId() == 43 || SM.getTemplate().getId() == 44 || SM.getTemplate().getId() == 45
					|| SM.getTemplate().getId() == 46 || SM.getTemplate().getId() == 47
					|| SM.getTemplate().getId() == 48 || SM.getTemplate().getId() == 49
					|| SM.getTemplate().getId() == 50 || SM.getTemplate().getId() == 62
					|| SM.getTemplate().getId() == 63 || SM.getTemplate().getId() == 64) {
				i++;
			}
		}
		return i;
	}

	public boolean canAggro() {
		return _canAggro;
	}

	public void set_canAggro(boolean canAggro) {
		_canAggro = canAggro;
	}

	public void setCurJobAction(JobAction JA) {
		_curJobAction = JA;
	}

	public JobAction getCurJobAction() {
		return _curJobAction;
	}

	public StatsMetier getMetierBySkill(int skID) {
		for (StatsMetier SM : _metiers.values())
			if (SM.isValidMapAction(skID))
				return SM;
		return null;
	}

	public String parseToFriendList(int guid) {
		StringBuilder str = new StringBuilder();
		str.append(";");
		str.append("?;");// FIXME
		str.append(_name).append(";");
		if (_compte.isFriendWith(guid)) {
			str.append(_lvl).append(";");
			str.append(_align).append(";");
		} else {
			str.append("?;");
			str.append("-1;");
		}
		str.append(_classe).append(";");
		str.append(_sexe).append(";");
		str.append(_gfxID);
		return str.toString();
	}

	public String parseToEnemyList(int guid) {
		StringBuilder str = new StringBuilder();
		str.append(";");
		str.append("?;");// FIXME
		str.append(_name).append(";");
		if (_compte.isFriendWith(guid)) {
			str.append(_lvl).append(";");
			str.append(_align).append(";");
		} else {
			str.append("?;");
			str.append("-1;");
		}
		str.append(_classe).append(";");
		str.append(_sexe).append(";");
		str.append(_gfxID);
		return str.toString();
	}

	public StatsMetier getMetierByID(int job) {
		for (StatsMetier SM : _metiers.values())
			if (SM.getTemplate().getId() == job)
				return SM;
		return null;
	}

	public boolean isOnMount() {
		return _onMount;
	}

	public void toogleOnMount() {
		if (!isOnMount() && get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE) {
			SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(get_compte().getGameThread().get_out(), "+5");
			return;
		}
		_onMount = !_onMount;
		Objects obj = getObjetByPos(Constant.ITEM_POS_FAMILIER);
		if (_onMount && obj != null) {
			obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
			SocketManager.GAME_SEND_OBJET_MOVE_PACKET(this, obj);
		}
		if (_mount.get_energie() <= 0) {
			_mount.setEnergie(0);
			return;
		}
		// on envoie les packets
		if (get_fight() != null && get_fight().get_state() == 2) {
			SocketManager.GAME_SEND_ALTER_FIGHTER_MOUNT(get_fight(), get_fight().getFighterByPerso(this), get_GUID(),
					get_fight().getTeamID(get_GUID()), get_fight().getOtherTeamID(get_GUID()));
		} else {
			SocketManager.GAME_SEND_ALTER_GM_PACKET(_curCarte, this);
		}
		SocketManager.GAME_SEND_Re_PACKET(this, "+", _mount);
		SocketManager.GAME_SEND_Rr_PACKET(this, _onMount ? "+" : "-");
		SocketManager.GAME_SEND_STATS_PACKET(this);
		int EnergyoLose = _mount.get_energie() - 10;
		_mount.setEnergie(EnergyoLose);
	}

	public int getMountXpGive() {
		return _mountXpGive;
	}

	public Mount getMount() {
		return _mount;
	}

	public void setMount(Mount DD) {
		_mount = DD;
	}

	public void setMountGiveXp(int parseInt) {
		_mountXpGive = parseInt;
	}

	public void resetVars() {
		_isTradingWith = 0;
		_isTalkingWith = 0;
		_away = false;
		_emoteActive = 0;
		_fight = null;
		_duelID = 0;
		_ready = false;
		_curExchange = null;
		_group = null;
		_isInBank = false;
		_inviting = 0;
		_sitted = false;
		_curJobAction = null;
		_isZaaping = false;
		_inMountPark = null;
		_onMount = false;
		_isOnPercepteurID = 0;
		_isClone = false;
		_isForgetingSpell = false;
		_isAbsent = false;
		_isInvisible = false;
		_Follower.clear();
		_Follows = null;
		_Follower.clear();
		_Follows = null;
		_curTrunk = null;
		_curHouse = null;
		_isGhosts = false;
		_isInDinde = false;
		_curCarteDefPerco = null;
		_curCellDefPerco = null;
	}

	public void addChanel(String chan) {
		if (_canaux.indexOf(chan) >= 0)
			return;
		_canaux += chan;
		SocketManager.GAME_SEND_cC_PACKET(this, '+', chan);
	}

	public void removeChanel(String chan) {
		_canaux = _canaux.replace(chan, "");
		SocketManager.GAME_SEND_cC_PACKET(this, '-', chan);
	}

	public void modifAlignement(byte a) {
		// Reset Variables
		_honor = 0;
		_deshonor = 0;
		_align = a;
		_aLvl = 1;
		// envoies des packets
		// Im022;10~42 ?
		SocketManager.GAME_SEND_ZC_PACKET(this, a);
		SocketManager.GAME_SEND_STATS_PACKET(this);
		// Im045;50 ?
	}

	public void setDeshonor(int deshonor) {
		_deshonor = deshonor;
	}

	public int getDeshonor() {
		return _deshonor;
	}

	public void setShowWings(boolean showWings) {
		_showWings = showWings;
	}

	public int get_honor() {
		return _honor;
	}

	public void set_honor(int honor) {
		_honor = honor;
	}

	public void setALvl(int a) {
		_aLvl = a;
	}

	public int getALvl() {
		return _aLvl;
	}

	public void toggleWings(char c) {
		if (_align == Constant.ALIGNEMENT_NEUTRE)
			return;
		int hloose = _honor * 5 / 100;// FIXME: perte de X% honneur
		switch (c) {
		case '*':
			SocketManager.GAME_SEND_GIP_PACKET(this, hloose);
			return;
		case '+':
			setShowWings(true);
			SocketManager.GAME_SEND_STATS_PACKET(this);
			SQLManager.SAVE_PERSONNAGE(this, false);
			break;
		case '-':
			setShowWings(false);
			_honor -= hloose;
			SocketManager.GAME_SEND_STATS_PACKET(this);
			SQLManager.SAVE_PERSONNAGE(this, false);
			break;
		}
		// SocketManager.GAME_SEND_ALTER_GM_PACKET(_curCarte, this);
	}

	public void addHonor(int winH) {
		int g = getGrade();
		_honor += winH;
		// Changement de grade
		if (getGrade() != g) {
			// TODO: Message IG
		}
	}

	public GuildMember getGuildMember() {
		return _guildMember;
	}

	public int getAccID() {
		return _accID;
	}

	public void setAccount(Accounts c) {
		_compte = c;
	}

	public String parsePrismesList() {
		String map = _curCarte.get_id() + "";
		String str = map + "";
		int SubAreaID = _curCarte.getSubArea().getArea().getSuperArea().getID();
		for (Prism Prisme : World.AllPrisme()) {
			if (Prisme.getalignement() != _align)
				continue;
			short CarteID = Prisme.getCarte();
			if (World.getCarte(CarteID) == null)
				continue;
			if (World.getCarte(CarteID).getSubArea().getArea().getSuperArea().getID() != SubAreaID)
				continue;
			if (Prisme.getInFight() == 0 || Prisme.getInFight() == -2) {
				str += "|" + CarteID + ";*";
			} else {
				int costo = Formulas.calculZaapCost(_curCarte, World.getCarte(CarteID));
				if (CarteID == _curCarte.get_id())
					costo = 0;
				str += "|" + CarteID + ";" + costo;
			}
		}
		return str;
	}

	public String parseZaapList()// Pour le packet WC
	{
		String map = _curCarte.get_id() + "";
		try {
			map = _savePos.split(",")[0];
		} catch (Exception e) {
		}
		;

		StringBuilder str = new StringBuilder();
		str.append(map);
		int SubAreaID = _curCarte.getSubArea().getArea().getSuperArea().getID();
		for (short i : _zaaps) {
			if (World.getCarte(i) == null)
				continue;
			if (World.getCarte(i).getSubArea().getArea().getSuperArea().getID() != SubAreaID)
				continue;
			int cost = Formulas.calculZaapCost(_curCarte, World.getCarte(i));
			if (i == _curCarte.get_id())
				cost = 0;
			str.append("|").append(i).append(";").append(cost);
		}
		return str.toString();
	}

	public boolean hasZaap(int mapID) {
		for (int i : _zaaps)
			if (i == mapID)
				return true;
		return false;
	}

	public void openPrismeMenu() {
		if (this._fight == null) {
			if (getDeshonor() >= 3) {
				SocketManager.GAME_SEND_Im_PACKET(this, "183");
				return;
			}

			_isZaaping = true;
			SocketManager.SEND_Wp_MENU_Prisme(this);
		}
	}

	public void openZaapMenu() {
		if (this._fight == null)// On ouvre si il n'est pas en combat
		{
			if (getDeshonor() >= 3) {
				SocketManager.GAME_SEND_Im_PACKET(this, "183");
				return;
			}
			_isZaaping = true;
			if (!hasZaap(_curCarte.get_id()))// Si le joueur ne connaissait pas
												// ce zaap
			{
				_zaaps.add(_curCarte.get_id());
				SocketManager.GAME_SEND_Im_PACKET(this, "024");
				SQLManager.SAVE_PERSONNAGE(this, false);
			}
			SocketManager.GAME_SEND_WC_PACKET(this);
		}
	}

	public void usePrisme(String packet) {
		int cellID = 340;
		short mapID = 7411;
		for (Prism Prisme : World.AllPrisme()) {
			if (Prisme.getCarte() == Short.valueOf(packet.substring(2))) {
				cellID = Prisme.getCell();
				mapID  = Prisme.getCarte();
				break;
			}
		}
		int cost = Formulas.calculZaapCost(_curCarte, World.getCarte(mapID));
		if (mapID == _curCarte.get_id())
			cost = 0;
		if (_kamas < cost) {
			SocketManager.GAME_SEND_Im_PACKET(this, "182");
			SocketManager.GAME_SEND_MESSAGE(this, "Vous n'avez pas sufisamment de Kamas pour r�aliser cette action.",
					Colors.RED);
			return;
		}
		_kamas -= cost;
		teleport(mapID, cellID);
		SocketManager.GAME_SEND_STATS_PACKET(this);
		SocketManager.SEND_Ww_CLOSE_Prisme(this);
	}

	public void useZaap(short id) {
		if (!_isZaaping)
			return;// S'il n'a pas ouvert l'interface Zaap(hack?)
		if (_fight != null)
			return;// Si il combat
		if (!hasZaap(id))
			return;// S'il n'a pas le zaap demand�(ne devrais pas arriver)
		int cost = Formulas.calculZaapCost(_curCarte, World.getCarte(id));
		if (_kamas < cost)
			return;// S'il n'a pas les kamas (verif cot�
					// org.walaka.rubrumsolem.client)
		short mapID = id;
		int SubAreaID = _curCarte.getSubArea().getArea().getSuperArea().getID();
		int cellID = World.getZaapCellIdByMapId(id);
		if (World.getCarte(mapID) == null) {
			Logs.addToGameLog("La map " + id + " n'est pas implantee, Zaap refuse");
			SocketManager.GAME_SEND_WUE_PACKET(this);
			return;
		}
		if (World.getCarte(mapID).getCase(cellID) == null) {
			Logs.addToGameLog("La cellule associee au zaap " + id + " n'est pas implantee, Zaap refuse");
			SocketManager.GAME_SEND_WUE_PACKET(this);
			return;
		}
		if (!World.getCarte(mapID).getCase(cellID).isWalkable(true)) {
			Logs.addToGameLog("La cellule associee au zaap " + id + " n'est pas 'walkable', Zaap refuse");
			SocketManager.GAME_SEND_WUE_PACKET(this);
			return;
		}
		if (World.getCarte(mapID).getSubArea().getArea().getSuperArea().getID() != SubAreaID) {
			SocketManager.GAME_SEND_WUE_PACKET(this);
			return;
		}
		_kamas -= cost;
		teleport(mapID, cellID);
		SocketManager.GAME_SEND_STATS_PACKET(this);// On envoie la perte de kamas
		SocketManager.GAME_SEND_WV_PACKET(this);// On ferme l'interface Zaap
		_isZaaping = false;
	}

	public String parseZaaps() {
		StringBuilder str = new StringBuilder();
		boolean first = true;

		if (_zaaps.isEmpty())
			return "";
		for (int i : _zaaps) {
			if (!first)
				str.append(",");
			first = false;
			str.append(i);
		}
		return str.toString();
	}

	public void stopZaaping() {
		if (!_isZaaping)
			return;
		_isZaaping = false;
		SocketManager.GAME_SEND_WV_PACKET(this);
	}

	public void Prisme_close() {
		if (!_isZaaping)
			return;
		_isZaaping = false;
		SocketManager.SEND_Ww_CLOSE_Prisme(this);
	}

	public void Zaapi_close() {
		if (!_isZaaping)
			return;
		_isZaaping = false;
		SocketManager.GAME_SEND_CLOSE_ZAAPI_PACKET(this);
	}

	public void Zaapi_use(String packet) {
		Maps map = World.getCarte(Short.valueOf(packet.substring(2)));
		
		short cellID = -1;
		if (map != null) {
			for (Entry<Integer, Case> entry : map.GetCases().entrySet()) {
				InteractiveObject obj = entry.getValue().getObject();
				if (obj != null) {
					if (obj.getID() == 7031 || obj.getID() == 7030) {
						cellID = (short) (entry.getValue().getID() + 18);
					}
				}
			}
		}
		if ((map.getSubArea().getArea().getID() == 7 || map.getSubArea().getArea().getID() == 11)
				&& cellID != -1) {
			int price = 20;
			if (this.get_align() == 1 || this.get_align() == 2)
				price = 10;
			_kamas -= price;
			SocketManager.GAME_SEND_STATS_PACKET(this);
			this.teleport(Short.valueOf(packet.substring(2)), cellID);
			SocketManager.GAME_SEND_CLOSE_ZAAPI_PACKET(this);
		}
	}

	public String parsePrisme() {
		String str = "";
		Prism Prisme = World.getPrisme(_curCarte.getSubArea().getPrismeID());
		if (Prisme == null)
			str = "-3";
		else if (Prisme.getInFight() == 0) {
			str = "0;" + Prisme.getTurnTime() + ";45000;7";
		} else {
			str = Prisme.getInFight() + "";
		}
		return str;
	}

	public boolean hasItemTemplate(int i, int q) {
		for (Objects obj : _items.values()) {
			if (obj.getPosition() != Constant.ITEM_POS_NO_EQUIPED && obj.getPosition() != Constant.ITEM_POS_FAMILIER)
				continue;
			if (obj.getTemplate().getID() != i)
				continue;
			if (obj.getQuantity() >= q)
				return true;
		}
		return false;
	}

	public void SetZaaping(boolean zaaping) {
		_isZaaping = zaaping;

	}

	public void setisForgetingSpell(boolean isForgetingSpell) {
		_isForgetingSpell = isForgetingSpell;
	}

	public boolean isForgetingSpell() {
		return _isForgetingSpell;
	}

	public boolean isDispo(Characters sender) {
		if (_isAbsent)
			return false;

		if (_isInvisible) {
			return _compte.isFriendWith(sender.get_compte().get_GUID());
		}

		return true;
	}

	public boolean get_isClone() {
		return _isClone;
	}

	public void set_isClone(boolean isClone) {
		_isClone = isClone;
	}

	public int get_isOnPercepteurID() {
		return _isOnPercepteurID;
	}

	public void set_isOnPercepteurID(int isOnPercepteurID) {
		_isOnPercepteurID = isOnPercepteurID;
	}

	public byte get_title() {
		return _title;
	}

	public void set_title(int title) {
		_title = (byte) title;
	}

	public long getLastPacketTime() {
		return _lastPacketTime;
	}

	public void refreshLastPacketTime() {
		_lastPacketTime = System.currentTimeMillis();
	}

	public static Characters ClonePerso(Characters P, int id) // Return, Skryn
	{
		TreeMap<Integer, Integer> stats = new TreeMap<Integer, Integer>();
		stats.put(Constant.STATS_ADD_VITA, P.get_baseStats().getEffect(Constant.STATS_ADD_VITA));
		stats.put(Constant.STATS_ADD_FORC, P.get_baseStats().getEffect(Constant.STATS_ADD_FORC));
		stats.put(Constant.STATS_ADD_SAGE, P.get_baseStats().getEffect(Constant.STATS_ADD_SAGE));
		stats.put(Constant.STATS_ADD_INTE, P.get_baseStats().getEffect(Constant.STATS_ADD_INTE));
		stats.put(Constant.STATS_ADD_CHAN, P.get_baseStats().getEffect(Constant.STATS_ADD_CHAN));
		stats.put(Constant.STATS_ADD_AGIL, P.get_baseStats().getEffect(Constant.STATS_ADD_AGIL));
		stats.put(Constant.STATS_ADD_PA, P.get_baseStats().getEffect(Constant.STATS_ADD_PA));
		stats.put(Constant.STATS_ADD_PM, P.get_baseStats().getEffect(Constant.STATS_ADD_PM));
		stats.put(Constant.STATS_ADD_RP_NEU, P.get_baseStats().getEffect(Constant.STATS_ADD_RP_NEU));
		stats.put(Constant.STATS_ADD_RP_TER, P.get_baseStats().getEffect(Constant.STATS_ADD_RP_TER));
		stats.put(Constant.STATS_ADD_RP_FEU, P.get_baseStats().getEffect(Constant.STATS_ADD_RP_FEU));
		stats.put(Constant.STATS_ADD_RP_EAU, P.get_baseStats().getEffect(Constant.STATS_ADD_RP_EAU));
		stats.put(Constant.STATS_ADD_RP_AIR, P.get_baseStats().getEffect(Constant.STATS_ADD_RP_AIR));
		stats.put(Constant.STATS_ADD_AFLEE, P.get_baseStats().getEffect(Constant.STATS_ADD_AFLEE));
		stats.put(Constant.STATS_ADD_MFLEE, P.get_baseStats().getEffect(Constant.STATS_ADD_MFLEE));

		byte showWings = 0;
		int alvl = 0;
		if (P.get_align() != 0 && P._showWings) {
			showWings = 1;
			alvl = P.getGrade();
		}
		int mountID = -1;
		if (P.getMount() != null) {
			mountID = P.getMount().get_id();
		}

		Characters Clone = new Characters(id, P.get_name(), P.get_sexe(), P.get_classe(), P.get_color1(),
				P.get_color2(), P.get_color3(), P.get_lvl(), 100, P.get_gfxID(), stats, P.getItems(), 100, showWings,
				mountID, alvl, P.get_align());

		Clone.set_isClone(true);
		if (P._onMount) {
			Clone._onMount = true;
		}
		return Clone;
	}

	public void VerifAndChangeItemPlace() {
		boolean isFirstAM = true;
		boolean isFirstAN = true;
		boolean isFirstANb = true;
		boolean isFirstAR = true;
		boolean isFirstBO = true;
		boolean isFirstBOb = true;
		boolean isFirstCA = true;
		boolean isFirstCE = true;
		boolean isFirstCO = true;
		boolean isFirstDa = true;
		boolean isFirstDb = true;
		boolean isFirstDc = true;
		boolean isFirstDd = true;
		boolean isFirstDe = true;
		boolean isFirstDf = true;
		boolean isFirstFA = true;
		for (Objects obj : _items.values()) {
			if (obj.getPosition() == Constant.ITEM_POS_NO_EQUIPED)
				continue;
			if (obj.getPosition() == Constant.ITEM_POS_AMULETTE) {
				if (isFirstAM) {
					isFirstAM = false;
				} else {
					obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				}
				continue;
			} else if (obj.getPosition() == Constant.ITEM_POS_ANNEAU1) {
				if (isFirstAN) {
					isFirstAN = false;
				} else {
					obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				}
				continue;
			} else if (obj.getPosition() == Constant.ITEM_POS_ANNEAU2) {
				if (isFirstANb) {
					isFirstANb = false;
				} else {
					obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				}
				continue;
			} else if (obj.getPosition() == Constant.ITEM_POS_ARME) {
				if (isFirstAR) {
					isFirstAR = false;
				} else {
					obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				}
				continue;
			} else if (obj.getPosition() == Constant.ITEM_POS_BOTTES) {
				if (isFirstBO) {
					isFirstBO = false;
				} else {
					obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				}
				continue;
			} else if (obj.getPosition() == Constant.ITEM_POS_BOUCLIER) {
				if (isFirstBOb) {
					isFirstBOb = false;
				} else {
					obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				}
				continue;
			} else if (obj.getPosition() == Constant.ITEM_POS_CAPE) {
				if (isFirstCA) {
					isFirstCA = false;
				} else {
					obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				}
				continue;
			} else if (obj.getPosition() == Constant.ITEM_POS_CEINTURE) {
				if (isFirstCE) {
					isFirstCE = false;
				} else {
					obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				}
				continue;
			} else if (obj.getPosition() == Constant.ITEM_POS_COIFFE) {
				if (isFirstCO) {
					isFirstCO = false;
				} else {
					obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				}
				continue;
			} else if (obj.getPosition() == Constant.ITEM_POS_DOFUS1) {
				if (isFirstDa) {
					isFirstDa = false;
				} else {
					obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				}
				continue;
			} else if (obj.getPosition() == Constant.ITEM_POS_DOFUS2) {
				if (isFirstDb) {
					isFirstDb = false;
				} else {
					obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				}
				continue;
			} else if (obj.getPosition() == Constant.ITEM_POS_DOFUS3) {
				if (isFirstDc) {
					isFirstDc = false;
				} else {
					obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				}
				continue;
			} else if (obj.getPosition() == Constant.ITEM_POS_DOFUS4) {
				if (isFirstDd) {
					isFirstDd = false;
				} else {
					obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				}
				continue;
			} else if (obj.getPosition() == Constant.ITEM_POS_DOFUS5) {
				if (isFirstDe) {
					isFirstDe = false;
				} else {
					obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				}
				continue;
			} else if (obj.getPosition() == Constant.ITEM_POS_DOFUS6) {
				if (isFirstDf) {
					isFirstDf = false;
				} else {
					obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				}
				continue;
			} else if (obj.getPosition() == Constant.ITEM_POS_FAMILIER) {
				if (isFirstFA) {
					isFirstFA = false;
				} else {
					obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
				}
				continue;
			}
		}

	}

	// Mariage

	public void MarryTo(Characters wife) {
		_wife = wife.get_GUID();
		SQLManager.SAVE_PERSONNAGE(this, true);
	}

	public String get_wife_friendlist() {
		Characters wife = World.getPersonnage(_wife);
		StringBuilder str = new StringBuilder();
		if (wife != null) {
			str.append(wife.get_name()).append("|").append(wife.get_classe() + wife.get_sexe()).append("|")
					.append(wife.get_color1()).append("|").append(wife.get_color2()).append("|")
					.append(wife.get_color3()).append("|");
			if (!wife.isOnline()) {
				str.append("|");
			} else {
				str.append(wife.parse_towife()).append("|");
			}
		} else {
			str.append("|");
		}
		return str.toString();
	}

	public String parse_towife() {
		int f = 0;
		if (_fight != null) {
			f = 1;
		}
		return _curCarte.get_id() + "|" + _lvl + "|" + f;
	}

	public void meetWife(Characters p)// Se teleporter selon les sacro-saintes
										// autorisations du mariage.
	{
		if (p == null)
			return; // Ne devrait theoriquement jamais se produire.

		int dist = (_curCarte.getX() - p.get_curCarte().getX()) * (_curCarte.getX() - p.get_curCarte().getX())
				+ (_curCarte.getY() - p.get_curCarte().getY()) * (_curCarte.getY() - p.get_curCarte().getY());
		if (dist > 100)// La distance est trop grande...
		{
			if (p.get_sexe() == 0) {
				SocketManager.GAME_SEND_Im_PACKET(this, "178");
			} else {
				SocketManager.GAME_SEND_Im_PACKET(this, "179");
			}
			return;
		}

		int cellPositiontoadd = Constant.getNearCellidUnused(p);
		if (cellPositiontoadd == -1) {
			if (p.get_sexe() == 0) {
				SocketManager.GAME_SEND_Im_PACKET(this, "141");
			} else {
				SocketManager.GAME_SEND_Im_PACKET(this, "142");
			}
			return;
		}

		teleport(p.get_curCarte().get_id(), (p.get_curCell().getID() + cellPositiontoadd));
	}

	public void Divorce() {
		if (isOnline())
			SocketManager.GAME_SEND_Im_PACKET(this, "047;" + World.getPersonnage(_wife).get_name());

		_wife = 0;
		SQLManager.SAVE_PERSONNAGE(this, true);
	}

	public int getWife() {
		return _wife;
	}

	public int setisOK(int ok) {
		return _isOK = ok;
	}

	public int getisOK() {
		return _isOK;
	}

	public void changeOrientation(int toOrientation) {
		if (this.get_orientation() == 0 || this.get_orientation() == 2 || this.get_orientation() == 4
				|| this.get_orientation() == 6) {
			this.set_orientation(toOrientation);
			SocketManager.GAME_SEND_eD_PACKET_TO_MAP(get_curCarte(), this.get_GUID(), toOrientation);
		}
	}

	/*
	 * public void set_FuneralStone() { // Ce transformer en tombe TODO
	 * set_gfxID(Integer.parseInt(get_classe()+"3")); }
	 */
	public void set_Ghosts() {
		if (isOnMount())
			toogleOnMount();
		_isGhosts = true;
		set_gfxID(8004);
		set_canAggro(false);
		set_away(true);
		set_Speed(-40);
		teleport((short) 8534, 297);
		// Le teleporter aux zone de mort la plus proche
		/*
		 * for(Carte map : ) FIXME { map. }
		 */
	}

	public void set_Alive() {
		if (!_isGhosts)
			return;
		_isGhosts = false;
		set_energy(1000);
		set_gfxID(Integer.parseInt(get_classe() + "" + get_sexe()));
		set_canAggro(true);
		set_away(false);
		set_Speed(0);
		SocketManager.GAME_SEND_STATS_PACKET(this);
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(get_curCarte(), get_GUID());
		SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(get_curCarte(), this);
	}
	
	public void set_maitre(Maitre maitre)
	{
		_maitre = maitre;
	}
	public Maitre get_maitre()
	{
		if(_maitre !=null)
			_maitre.refreh();
		return _maitre;
	}
	
	public boolean isEsclave() {
		return esclave;
	}
	public void setEsclave(boolean value) {
    	esclave = value;
    }

	public void setInTrunk(Trunk t) {
		_curTrunk = t;
	}

	public Trunk getInTrunk() {
		return _curTrunk;
	}

	public void setInHouse(Houses h) {
		_curHouse = h;
	}

	public Houses getInHouse() {
		return _curHouse;
	}

	public Map<Integer, Integer> getStoreItems() {
		return _storeItems;
	}

	public String parseStoreItemsList() {
		StringBuilder list = new StringBuilder();
		if (_storeItems.isEmpty())
			return "";
		for (Entry<Integer, Integer> obj : _storeItems.entrySet()) {
			Objects O = World.getObjet(obj.getKey());
			if (O == null)
				continue;
			list.append(O.getGuid()).append(";").append(O.getQuantity()).append(";").append(O.getTemplate().getID())
					.append(";").append(O.parseStatsString()).append(";").append(obj.getValue()).append("|");
		}
		return (list.length() > 0 ? list.toString().substring(0, list.length() - 1) : list.toString());
	}

	public String parseStoreItemstoBD() {
		StringBuilder str = new StringBuilder();
		for (Entry<Integer, Integer> _storeObjets : _storeItems.entrySet()) {
			str.append(_storeObjets.getKey()).append(",").append(_storeObjets.getValue()).append("|");
		}
		return str.toString();
	}
	
	 public int storeAllBuy() {
	        int total = 0;
	        for (Entry<Integer, Integer> value : _storeItems.entrySet()) {
	            Objects O = World.getObjet(value.getKey());
	            int multiple = O.getQuantity();
	            int add = value.getValue() * multiple;
	            total += add;
	        }

	        return total;
	    }

	public void addinStore(int ObjID, int price, int qua) {
		Objects PersoObj = World.getObjet(ObjID);
		// Si le joueur n'a pas l'item dans son sac ...
		if (_storeItems.get(ObjID) != null) {
			_storeItems.remove(ObjID);
			_storeItems.put(ObjID, price);
			SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(this, this);
			return;
		}
		if (_items.get(ObjID) == null) {
			Logs.addToGameLog("Le joueur " + _name + " a tenter d'ajouter un objet au store qu'il n'avait pas.");
			return;
		}
		if (_items.get(ObjID).getQuantity() < qua) {
			Logs.addToGameLog("Le joueur " + _name
					+ " a tent� d'ajouter une quantit� d'objet en banque dont il ne poss�dait pas.");
			SocketManager.GAME_SEND_MESSAGE(this, "Alors petit rigolo, �a marche plus ?", Colors.RED);
			return;
		}
		// Si c'est un item �quip� ...
		if (PersoObj.getPosition() != Constant.ITEM_POS_NO_EQUIPED)
			return;

		Objects SimilarObj = getSimilarStoreItem(PersoObj);
		int newQua = PersoObj.getQuantity() - qua;
		if (SimilarObj == null)// S'il n'y pas d'item du meme Template
		{
			// S'il ne reste pas d'item dans le sac
			if (newQua <= 0) {
				// On enleve l'objet du sac du joueur
				removeItem(PersoObj.getGuid());
				// On met l'objet du sac dans le store, avec la meme quantit�
				_storeItems.put(PersoObj.getGuid(), price);
				SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, PersoObj.getGuid());
				SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(this, this);
			} else// S'il reste des objets au joueur
			{
				// on modifie la quantit� d'item du sac
				PersoObj.setQuantity(newQua);
				// On ajoute l'objet a la banque et au monde
				SimilarObj = Objects.getCloneObjet(PersoObj, qua);
				World.addObjet(SimilarObj, true);
				_storeItems.put(SimilarObj.getGuid(), price);

				// Envoie des packets
				SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(this, this);
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj);

			}
		} else // S'il y avait un item du meme template
		{
			// S'il ne reste pas d'item dans le sac
			if (newQua <= 0) {
				// On enleve l'objet du sac du joueur
				removeItem(PersoObj.getGuid());
				// On enleve l'objet du monde
				World.removeItem(PersoObj.getGuid());
				// On ajoute la quantit� a l'objet en banque
				SimilarObj.setQuantity(SimilarObj.getQuantity() + PersoObj.getQuantity());
				_storeItems.remove(SimilarObj.getGuid());
				_storeItems.put(SimilarObj.getGuid(), price);
				// on envoie l'ajout a la banque de l'objet
				SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(this, this);
				// on envoie la supression de l'objet du sac au joueur
				SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, PersoObj.getGuid());
			} else // S'il restait des objets
			{
				// on modifie la quantit� d'item du sac
				PersoObj.setQuantity(newQua);
				SimilarObj.setQuantity(SimilarObj.getQuantity() + qua);
				_storeItems.remove(SimilarObj.getGuid());
				_storeItems.put(SimilarObj.getGuid(), price);
				SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(this, this);
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj);

			}
		}
		SocketManager.GAME_SEND_Ow_PACKET(this);
		SQLManager.SAVE_PERSONNAGE(this, true);
	}

	private Objects getSimilarStoreItem(Objects obj) {
		for (Entry<Integer, Integer> value : _storeItems.entrySet()) {
			Objects obj2 = World.getObjet(value.getKey());
			if (obj2.getTemplate().getType() == 85)
				continue;
			if (obj2.getTemplate().getID() == obj.getTemplate().getID() && obj2.getStats().isSameStats(obj.getStats()))
				return obj2;
		}
		return null;
	}

	public void removeFromStore(int guid, int qua) {
		Objects SimilarObj = World.getObjet(guid);
		// Si le joueur n'a pas l'item dans son store ...
		if (_storeItems.get(guid) == null) {
			Logs.addToGameLog("Le joueur " + _name + " a tenter de retirer un objet du store qu'il n'avait pas.");
			return;
		}

		Objects PersoObj = getSimilarItem(SimilarObj);

		int newQua = SimilarObj.getQuantity() - qua;

		if (PersoObj == null)// Si le joueur n'avait aucun item similaire
		{
			// S'il ne reste rien en store
			if (newQua <= 0) {
				// On retire l'item du store
				_storeItems.remove(guid);
				// On l'ajoute au joueur
				_items.put(guid, SimilarObj);

				// On envoie les packets
				SocketManager.GAME_SEND_OAKO_PACKET(this, SimilarObj);
				SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(this, this);

			}
		} else {
			// S'il ne reste rien en store
			if (newQua <= 0) {
				// On retire l'item de la banque
				_storeItems.remove(SimilarObj.getGuid());
				World.removeItem(SimilarObj.getGuid());
				// On Modifie la quantit� de l'item du sac du joueur
				PersoObj.setQuantity(PersoObj.getQuantity() + SimilarObj.getQuantity());

				// On envoie les packets
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, PersoObj);
				SocketManager.GAME_SEND_ITEM_LIST_PACKET_SELLER(this, this);

			}
		}
		SocketManager.GAME_SEND_Ow_PACKET(this);
		SQLManager.SAVE_PERSONNAGE(this, true);
	}

	public void removeStoreItem(int guid) {
		_storeItems.remove(guid);
	}

	public void addStoreItem(int guid, int price) {
		_storeItems.put(guid, price);
	}

	public void set_Speed(int _Speed) {
		this._Speed = _Speed;
	}

	public int get_Speed() {
		return _Speed;
	}

	private int savestat;
	private int teamID = -1;

	public int get_savestat() {
		return this.savestat;
	}

	public void set_savestat(int stat) {
		this.savestat = stat;
	}

	public void RejoindeWife(Characters p) // Correcion t�l�portation
											// mariage par Taparisse
	{
		if (p == null) {
			return;
		}
		int dist = (_curCarte.getX() - p.get_curCarte().getX()) * (_curCarte.getX() - p.get_curCarte().getX())
				+ (_curCarte.getY() - p.get_curCarte().getY()) * (_curCarte.getY() - p.get_curCarte().getY());
		if (dist > 10) // Distance max accept�e entre les deux amours.
		{
			SocketManager.GAME_SEND_MESSAGE(this,
					"Vous etes trop loin de votre amour. Impossible de vous y teleporter.", Colors.RED);
			return;
		} else {
			teleport(p.get_curCarte().get_id(), p.get_curCell().getID());
			return;
		}
	}

	public void setInDinde(boolean b) {
		_isInDinde = b;
	}

	public boolean isInDinde() {
		return _isInDinde;
	}

	// Quests
	public Map<String, String> get_quest(int questId) {
		return _quests.get(questId);
	}

	// a-t-il cet objectif À FAIRE ?
	public boolean hasObjective(int objectiveId) {
		for (Entry<Integer, Map<String, String>> entry : _quests.entrySet()) // Chaque
																				// quete
		{
			if (!entry.getValue().get("curStep").equals("-1")
					&& entry.getValue().get("objectivesToDo").contains(objectiveId + ""))
				return true;
		}

		return false;
	}

	public boolean hasQuest(int questId, int stepId) {
		Map<String, String> questTarget = _quests.get(questId);
		if (questTarget == null)
			return false;

		if (stepId != -1 && stepId != Integer.parseInt(questTarget.get("curStep"))) // Une
																					// étape
																					// précise
																					// est
																					// renseignée
			return false;

		return true;
	}

	private void initializeQuests(String quests) // Transforme string quest en
													// tableau
	{
		if (quests == null)
			return;
		String[] myQuests = quests.split("\\|");

		for (String quest : myQuests) {
			if (quest == null || (quest != null && !quest.contains(":")) || (quest != null && quest.isEmpty()))
				return;

			String[] questInfos = quest.split(":");
			if (questInfos.length < 2)
				return;
			int questId = Integer.parseInt(questInfos[0]);
			_quests.put(questId, new HashMap<String, String>());

			if (questInfos[1].equals("-1")) {
				_quests.get(questId).put("curStep", "-1");
			} else {
				String[] questDetails = questInfos[1].split(";");
				if (questDetails.length < 2)
					return;
				_quests.get(questId).put("curStep", questDetails[0]);
				_quests.get(questId).put("objectivesToDo", questDetails[1]);
			}
		}
	}

	public String questsToString() {
		String quests = "";

		for (Entry<Integer, Map<String, String>> entry : _quests.entrySet()) {
			quests += entry.getKey() + ":"; // Id quête
			if (entry.getValue().get("curStep").equals("-1")) // Quête finie
			{
				quests += "-1|";
			} else // Quete en cours
			{
				quests += entry.getValue().get("curStep") + ";"; // Etape
																	// actuelle
				if (!entry.getValue().get("objectivesToDo").equals("")) // d'objectif
																		// restant
				{
					quests += entry.getValue().get("objectivesToDo"); // Objectifs
																		// restants
				} else
					quests += " ";
				quests += "|";
			}
		}

		return quests;
	}

	public String parseQuestsList() {

		String packet = "QL|";

		for (Entry<Integer, Map<String, String>> entry : _quests.entrySet()) {
			if (entry != null) {
				packet += entry.getKey() + ";";
				if (entry.getValue().get("curStep").equals("-1")) {
					packet += "1;";
				} else {
					packet += "0;";
				}

				packet += "1|";
			}
		}

		return packet.substring(0, packet.length() - 1);
	}

	public String parseQuestStep(int questId) {
		Map<String, String> questPerso = _quests.get(questId);
		if (questPerso == null) {
			return ""; // On a pas la quête demandée
		}

		Map<String, String> questDetails = World.getQuest(questId);
		if (questDetails == null) {
			Logs.addToGameLog("Game: Error >> Quete " + questId + " inexistante ");
			return ""; // La quête demandée n'existe pas
		}
		String packet = "QS" + questId + "|";

		String questSteps = questDetails.get("steps"); // Etapes de la quête
														// split par ;
		String curStep = questPerso.get("curStep");
		if (curStep.equals("-1")) // Quête finie : on prend la dernière étape
		{
			String[] splitSteps = questSteps.split(";");
			String lastStep = splitSteps[splitSteps.length - 1];
			curStep = lastStep;
		}
		packet += curStep + "|";

		Map<String, String> stepDetails = World.getStep(Integer.parseInt(curStep));
		String[] allObjectives = stepDetails.get("objectives").split(";");

		for (String curObj : allObjectives) // Chaque objectif de l'étape
											// actuelle
		{
			packet += curObj + ",";
			if (!questPerso.get("objectivesToDo").contains(curObj)) // Objectif
																	// fait ?
			{
				packet += "1;";
			} else {
				packet += "0;";
			}

		}
		packet = packet.substring(0, packet.length() - 1) + "|"; // Enlève le
																	// dernier ;
																	// en trop

		int index = questSteps.indexOf(curStep);
		if (index > 0)
			index -= 1;
		packet += questSteps.substring(0, index) + "|"; // Etapes avant
														// l'actuelle
		if (questSteps.length() < questSteps.indexOf(curStep) + 1 + curStep.length() + 1) {
			packet += "|"; // = Etape actuelle est la derniere
		} else // Il reste des etapes, on les prend
		{
			packet += questSteps.substring(questSteps.indexOf(curStep) + curStep.length() + 1) + "|";
		}

		packet += stepDetails.get("question") + ";"; // TODO params de la
														// question quete

		return packet;
	}

	public String confirmObjective(int type, String args, String args2) {
		for (Entry<Integer, Map<String, String>> entry : _quests.entrySet()) // Chaque
																				// quête
		{
			if (!entry.getValue().get("curStep").equals("-1")) // Si non
																// terminée
			{
				String objectivesToDo = entry.getValue().get("objectivesToDo");
				String[] splitObjectives = objectivesToDo.split(",");
				for (String toDo : splitObjectives) {
					if (toDo == null || (toDo != null && toDo.equals(" ")) || (toDo != null && toDo.isEmpty()))
						return "";
					Map<String, String> obj = World.getObjective(Integer.parseInt(toDo));
					if (obj == null)
						return ""; // Objectif inexistant

					if (type == 1 || type == 2 || type == 4 || type == 8) // Objectifs
																			// simple
																			// :
																			// "Aller
																			// voir
																			// PNJ"
																			// OU
																			// "Montrer
																			// Nbre
																			// Item
																			// à
																			// Pnj"
																			// OU
																			// "Découvrir
																			// carte
																			// MAPID"
																			// OU
																			// "Utiliser
																			// objet
																			// X"
					{
						if (obj.get("type").equals(type + "") && obj.get("args").equals(args)) {
							objectivesToDo = objectivesToDo.replaceAll(toDo + ",", "");
							objectivesToDo = objectivesToDo.replaceAll("," + toDo, "");
							objectivesToDo = objectivesToDo.replaceAll(toDo, "");

							_quests.get(entry.getKey()).put("objectivesToDo", objectivesToDo);
							SocketManager.GAME_SEND_Im_PACKET(this, "055;" + entry.getKey()); // Quete
																								// MaJ
						}
					} else if (type == 3) // "Ramener à PNJ NBRE ITEMS"
					{ // Args = ItemId,Quantite
						if (obj.get("type").equals(type + "") && obj.get("args").equals(args)) {
							// On retire l'item demandé
							int itemId = 0;
							int q = 1;

							if (args.contains(",")) {
								itemId = Integer.parseInt(args.split(",")[0]);
								q = Integer.parseInt(args.split(",")[1]);
							} else {
								itemId = Integer.parseInt(args);
							}
							removeByTemplateID(itemId, q);
							SocketManager.GAME_SEND_Ow_PACKET(this);
							SocketManager.GAME_SEND_Im_PACKET(this, "022;" + q + "~" + itemId);

							objectivesToDo = objectivesToDo.replaceAll(toDo + ",", "");
							objectivesToDo = objectivesToDo.replaceAll("," + toDo, "");
							objectivesToDo = objectivesToDo.replaceAll(toDo, "");

							_quests.get(entry.getKey()).put("objectivesToDo", objectivesToDo);
							SocketManager.GAME_SEND_Im_PACKET(this, "055;" + entry.getKey()); // Quete
																								// MaJ
						}
					} else if (type == 6) {
						for (String curMob : args.split(";")) // Différents
																// mobs vaincus
						{
							if (curMob == null || (curMob != null && curMob.isEmpty()))
								return "";

							if (obj.get("type").equals(type + "")) {
								int mobId = Integer.parseInt(curMob.split(",")[0]);
								int curMobId = Integer.parseInt(obj.get("args").split(",")[0]);
								int curQ = Integer.parseInt(obj.get("args").split(",")[1]);
								int mobQ = Integer.parseInt(curMob.split(",")[1]);

								if (curMobId == mobId && curQ <= mobQ) {
									// Avis de recherche : don item follower sur
									// pos bonbon + ajout follower
									int itemFollow;
									@SuppressWarnings("unused")
									int itemFollowPos;
									int skinFollow;
									if ((itemFollow = Constant.getItemByHuntMob(mobId)) != -1
											&& (itemFollowPos = Constant.getAvailableCandyPos(this)) != -1) {
										ObjTemplate t = World.getObjTemplate(itemFollow);
										Objects followObj = t.createNewItem(1, true, -1);
										if (addObjet(followObj, true))// Si le
																		// joueur
																		// n'avait
																		// pas
																		// d'item
																		// similaire
											World.addObjet(followObj, true);

										SocketManager.GAME_SEND_Im_PACKET(this, "021;1~" + itemFollow);
									}
									if ((skinFollow = Constant.getSkinByHuntMob(mobId)) != -1) {
										set_follow(skinFollow + "");
									}
									// Fin avis de recherche
									objectivesToDo = objectivesToDo.replaceAll(toDo + ",", "");
									objectivesToDo = objectivesToDo.replaceAll("," + toDo, "");
									objectivesToDo = objectivesToDo.replaceAll(toDo, "");

									_quests.get(entry.getKey()).put("objectivesToDo", objectivesToDo);
									SocketManager.GAME_SEND_Im_PACKET(this, "055;" + entry.getKey()); // Quete
																										// MaJ
								}
							}
						}
					}
				}

			}
		}

		return "";
	}

	public boolean canDoObjective(int type, String args) // Le perso peut-il
															// faire cet
															// objectif ?
	{

		if (type == 3) // "Ramener à PNJ NBRE ITEMS"
		{ // Args = ItemId,Quantite
			int itemId = 0;
			int q = 1;

			if (args.contains(",")) {
				itemId = Integer.parseInt(args.split(",")[0]);
				q = Integer.parseInt(args.split(",")[1]);
			} else {
				itemId = Integer.parseInt(args);
			}

			if (!hasItemTemplate(itemId, q))
				return false;

			return true;
		}

		return true;
	}

	public void addNewQuest(int questId) {
		Map<String, String> questDetails = World.getQuest(questId);
		if (questDetails == null) // quete inexistante
		{
			Logs.addToGameLog("Game: Error >> Quete " + questId + " inexistante.");
			return;
		}
		if (_quests.get(questId) != null) // Perso a deja la quete
		{
			// SocketManager.GAME_SEND_MESSAGE(this, "Vous avez déjà cette
			// quête !", "FF0000");
			return;
		}
		if (_lvl < Integer.parseInt(questDetails.get("minLvl"))) // Niveau
																	// minimum
		{
			// SocketManager.GAME_SEND_MESSAGE(this, "Vous devez avoir le niveau
			// minimum de " + questDetails.get("minLvl") + " pour faire cette
			// quête !", "FF0000");
			SocketManager.GAME_SEND_Im_PACKET(this, "13");
			return;
		}
		if (!questDetails.get("questRequired").equals("0")) // Il faut avoir
															// fait une quete
		{
			// SocketManager.GAME_SEND_MESSAGE(this, "Cette quête nécessite de
			// faire une autre quête.", "FF0000");
			return;
		}

		String firstStep = questDetails.get("steps").split(";")[0];
		Map<String, String> stepDetails = World.getStep(Integer.parseInt(firstStep));
		if (stepDetails == null) // etape inexistante
		{
			Logs.addToGameLog("Game: Error >> Etape de quete " + firstStep + " inexistante.");
			return;
		}

		String objectives = stepDetails.get("objectives").replaceAll(";", ",");
		_quests.put(questId, new HashMap<String, String>());
		_quests.get(questId).put("curStep", firstStep);
		_quests.get(questId).put("objectivesToDo", objectives);

		SocketManager.GAME_SEND_Im_PACKET(this, "054;" + questId); // New quete
	}

	public void upgradeQuest(int questId) // Monte prochaine étape ou termine
											// quete
	{

		Map<String, String> questDetails = World.getQuest(questId);
		if (questDetails == null) // quete inexistante
		{
			Logs.addToGameLog("Game: Error >> Quete " + questId + " inexistante.");
			return;
		}
		Map<String, String> persoQuest = _quests.get(questId);
		if (persoQuest == null || (persoQuest != null && persoQuest.get("curStep").equals("-1"))) // Perso
																									// n'a
																									// pas
																									// la
																									// quete
																									// OU
																									// quete
																									// deja
																									// finie
		{
			Logs.addToGameLog("Le personnage n'a pas la quete ou l'a terminee");
			return;
		}

		String curStep = persoQuest.get("curStep");
		String questSteps = questDetails.get("steps"); // Etapes de la quete
		String nextSteps = "";
		if (!persoQuest.get("objectivesToDo").isEmpty() && !persoQuest.get("objectivesToDo").equals(" ")) {
			return;
		}
		if (questSteps.length() < questSteps.indexOf(curStep) + 1 + curStep.length() + 1) {
			nextSteps = ""; // = Etape actuelle est la derniere
		} else // Il reste des etapes
		{
			nextSteps = questSteps.substring(questSteps.indexOf(curStep) + curStep.length() + 1);
		}

		// Récompenses
		Map<String, String> stepDetails = World.getStep(Integer.parseInt(curStep));

		if (!stepDetails.get("gainKamas").equals("0")) // Gain de Kamas
		{
			_kamas += Integer.parseInt(stepDetails.get("gainKamas"));

			SocketManager.GAME_SEND_STATS_PACKET(this);
			SocketManager.GAME_SEND_Im_PACKET(this, "045;" + stepDetails.get("gainKamas")); // Tu
																							// as
																							// g²
																							// x
																							// Kamas
		}

		if (!stepDetails.get("gainExp").equals("0")) // Gain d'exp
		{
			long gain = Integer.parseInt(stepDetails.get("gainExp"));
			addXp(gain);

			SocketManager.GAME_SEND_STATS_PACKET(this);
			SocketManager.GAME_SEND_Im_PACKET(this, "08;" + stepDetails.get("gainExp")); // Tu
																							// as
																							// g²
																							// x
																							// XP
		}

		if (stepDetails.get("gainItems") != null) // Gain d'Items
		{
			for (String item : stepDetails.get("gainItems").split(";")) // Différents
																		// items
			{
				if (item == null)
					return;

				int tID = Integer.parseInt(item.split(",")[0]);
				int count = Integer.parseInt(item.split(",")[1]);

				ObjTemplate Template = World.getObjTemplate(tID);
				if (Template == null)
					return;
				Objects newItem = Template.createNewItem(count, false, -1);
				if (addObjet(newItem, true))
					World.addObjet(newItem, true);

				SocketManager.GAME_SEND_Ow_PACKET(this);
				SocketManager.GAME_SEND_Im_PACKET(this, "021;" + count + "~" + tID);
			}
		}
		// Fin récompenses

		if (nextSteps.isEmpty()) // Quete terminee
		{
			Logs.addToGameLog("quete terminee, curStep = -1");
			SocketManager.GAME_SEND_Im_PACKET(this, "056;" + questId); // Fin
																		// quete

			persoQuest.put("curStep", "-1");

		} else // Reste des etapes : on change curStep & objectivesToDo
		{
			Logs.addToGameLog("Etapes restantes " + nextSteps);
			String newCurStep = nextSteps.split(";")[0];
			persoQuest.put("curStep", newCurStep);

			String objectivesToDo = World.getStep(Integer.parseInt(newCurStep)).get("objectives").replaceAll(";", ",");
			persoQuest.put("objectivesToDo", objectivesToDo);

			SocketManager.GAME_SEND_Im_PACKET(this, "055;" + questId); // Quete
																		// MaJ
		}

		_quests.put(questId, persoQuest);
	}

	public void setIsNew(boolean b) {
		_isNew = b;
	}

	public boolean isNew() {
		return _isNew;
	}

	public String parse_tojobbook(int id) {
		StatsMetier sm = getMetierByID(id);
		if (sm == null)
			return null;
		System.out.println(sm.getTemplate().getId());
		String alpha = "0;0";
		if (_curCarte.hasatelierfor(id)) {
			alpha = _curCarte.get_id() + ";1";
		}
		String p = _name + ";" + sm.get_lvl() + ";" + alpha + ";" + _classe + ";" + _sexe + ";" + this._color1 + ","
				+ this._color2 + "," + this._color3 + ";" + getGMStuffString() + ";" + sm.getOptBinValue() + ","
				+ sm.getmaxcase();
		return p;
	}

	public void modif_job_o(String packet) {
		System.out.println(packet);
		String[] p = packet.replace((char) 0x7C, ';').split(";");
		int pp = 0;
		try {
			pp = Integer.parseInt(p[0]);
		} catch (Exception e) {
		}
		StatsMetier s = _metiers.get(pp);
		s.set_o(p);
		ArrayList<StatsMetier> list = new ArrayList<StatsMetier>();
		list.addAll(_metiers.values());
		SocketManager.GAME_SEND_JO_PACKET(this, list);
	}

	public void DeconnexionCombat() {
		// Quand tous les tours sont pass�s, bah on dois d�connecter le gars
		if (get_curExchange() != null)
			get_curExchange().cancel();
		// Si en groupe
		if (getGroup() != null)
			getGroup().leave(this);
		resetVars();
		SQLManager.SAVE_PERSONNAGE(this, true);
		set_isClone(true);
		World.unloadPerso(_GUID);
	}

	public boolean getHasEndFight() {
		return _hasEndfight;
	}

	public void setHasEndFight(boolean v) {
		_hasEndfight = v;
	}

	public int CheckItemConditions() {
		boolean deseq = true;
		int nb = 0;
		Objects obj;
		while (deseq) {
			deseq = false;
			for (Entry<Integer, Objects> entry : _items.entrySet()) {
				obj = entry.getValue();
				if (obj.getPosition() == Constant.ITEM_POS_NO_EQUIPED)
					continue;
				if (!obj.getTemplate().getConditions().equalsIgnoreCase("")
						&& !ConditionParser.validConditions(this, obj.getTemplate().getConditions())) {
					// si le perso ne v�rifie pas les conditions diverses
					this.DesequiperItem(obj);
					deseq = true;
					nb++;
				}
			}
		}
		if (nb > 0) {
			SocketManager.GAME_SEND_Im_PACKET(this, "119|43");
		}
		return nb;
	}

	public synchronized void DesequiperItem(Objects item) {
		if (item == null)
			return;
		if (!hasItemGuid(item.getGuid()))
			return;
		if (item.getPosition() == Constant.ITEM_POS_NO_EQUIPED)
			return;
		if (item.hasSpellBoostStats()) {
			SocketManager.GAME_SEND_SB_PACKET(this, item.getBoostSpellStats(), false);
		}
		Objects obj2;
		if ((obj2 = getSimilarItem(item)) != null)// On le poss�de deja
		{
			obj2.setQuantity(obj2.getQuantity() + item.getQuantity());
			SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, obj2);
			World.removeItem(item.getGuid());
			removeItem(item.getGuid());
			SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this, item.getGuid());
		} else// On ne le poss�de pas
		{
			item.setPosition(Constant.ITEM_POS_NO_EQUIPED);
			SocketManager.GAME_SEND_OBJET_MOVE_PACKET(this, item);
		}
		// Si objet de panoplie
		if (item.getTemplate().getPanopID() > 0)
			SocketManager.GAME_SEND_OS_PACKET(this, item.getTemplate().getPanopID());
	}

	public void setLastMapFight(short lastMapFight) {
		LastMapFight = lastMapFight;
	}

	public short getLastMapFight() {
		return LastMapFight;
	}

	public Collection<SortStats> getSorts() {
		return _sorts.values();
	}

	public void setActTimerTask(TimerTask actTimerTask) {
		this.actTimerTask = actTimerTask;
	}

	public TimerTask getActTimerTask() {
		return actTimerTask;
	}

	public boolean addObjetShop(Objects newObj, boolean stackIfSimilar, Characters _perso, int templateID, int quantity,
			int price) {
		for (Entry<Integer, Objects> entry : _items.entrySet()) {
			Objects obj = entry.getValue();
			if (obj.getTemplate().getID() == newObj.getTemplate().getID()
					&& obj.getStats().isSameStats(newObj.getStats()) && stackIfSimilar
					&& newObj.getTemplate().getType() != 85 && obj.getPosition() == Constant.ITEM_POS_NO_EQUIPED)// Si
																													// meme
																													// Template
																													// et
																													// Memes
																													// Stats
																													// et
																													// Objet
																													// non
																													// �quip�
			{
				obj.setQuantity(obj.getQuantity() + newObj.getQuantity());// On
																			// ajoute
																			// QUA
																			// item
																			// a
																			// la
																			// quantit�
																			// de
																			// l'objet
																			// existant
				SQLManager.SAVE_ITEM(obj);
				Utils.addToShopLogs(_perso, templateID, quantity, price, obj.getGuid());
				if (_isOnline)
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this, obj);
				return false;
			}
		}
		_items.put(newObj.getGuid(), newObj);
		Utils.addToShopLogs(_perso, templateID, quantity, price, newObj.getGuid());
		SocketManager.GAME_SEND_OAKO_PACKET(this, newObj);
		return true;
	}

	public long getLastMess() {
		return lastMess;
	}

	public void setLastMess(long lastMess) {
		this.lastMess = lastMess;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Map<String, Long> getLastMessSent() {
		return lastMessSent;
	}

	public void setLastMessSent(Map<String, Long> lastMessSent) {
		this.lastMessSent = lastMessSent;
	}

	public int getLastFloodTime() {
		return lastFloodTime;
	}

	public void setLastFloodTime(int lastFloodTime) {
		this.lastFloodTime = lastFloodTime;
	}

	public int getAverto() {
		return averto;
	}

	public void setAverto(int averto) {
		this.averto = averto;
	}

	public int getTeamID() {
		return teamID;
	}

	public void setTeamID(int teamID) {
		this.teamID = teamID;
	}

	public Team getKoliTeam() {
		for (Team team : Team.koliTeams) {
			for (Characters c : team.getkCharacters()) {
				if (c.get_GUID() == this.get_GUID()) {
					return team;
				}
			}
		}
		return null;
	}

	public int getArena() {
		return arena;
	}

	public void setArena(int arena) {
		this.arena = arena;
	}

	public int getKolizeum() {
		return kolizeum;
	}

	public void setKolizeum(int kolizeum) {
		this.kolizeum = kolizeum;
	}

	public ArrayList<RapidStuff> getRapidStuffs() {
		ArrayList<RapidStuff> toReturn = new ArrayList<RapidStuff>();

		if (!this.rapidStuffs.isEmpty()) {
			return rapidStuffs;
		} else {
			for (Entry<Integer, RapidStuff> rs : RapidStuff.rapidStuffs.entrySet()) {
				if (rs.getValue().getOwner() == this.get_GUID()) {
					toReturn.add(rs.getValue());
				}
			}
		}
		return toReturn;
	}

	public RapidStuff getRapidStuffByName(String name) {
		RapidStuff rapidStuff = null;
		for (RapidStuff rs : getRapidStuffs()) {
			if (rs.getName().equals(name)) {
				rapidStuff = rs;
			}
		}
		return rapidStuff;
	}

	public RapidStuff getRapidStuffByID(int id) {
		RapidStuff rapidStuff = null;
		for (RapidStuff rs : getRapidStuffs()) {
			if (rs.getId() == id) {
				rapidStuff = rs;
			}
		}
		return rapidStuff;
	}

	public boolean isEquip() {
		return Equip;
	}

	public void setEquip(boolean equip) {
		Equip = equip;
	}
}

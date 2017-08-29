package org.fight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import org.client.Characters;
import org.client.Characters.Group;
import org.client.Characters.Stats;
import org.common.Constant;
import org.common.CryptManager;
import org.common.Formulas;
import org.common.Pathfinding;
import org.common.SQLManager;
import org.common.SocketManager;
import org.common.World;
import org.common.World.Couple;
import org.common.World.Drop;
import org.fight.IA.IA;
import org.fight.IA.IA.IAThread;
import org.fight.extending.Arena;
import org.fight.extending.Team;
import org.fight.object.Challenge;
import org.fight.object.Collector;
import org.fight.object.Prism;
import org.fight.object.Stalk;
import org.fight.object.Monster.MobGrade;
import org.fight.object.Monster.MobGroup;
import org.fight.object.Stalk.Traque;
import org.game.GameSendThread;
import org.game.GameThread.GameAction;
import org.kernel.Config;
import org.kernel.Logs;
import org.object.Guild;
import org.object.Maps;
import org.object.Objects;
import org.object.SoulStone;
import org.object.Maps.Case;
import org.object.Objects.ObjTemplate;
import org.spell.Spell;
import org.spell.SpellEffect;
import org.spell.Spell.SortStats;

public class Fight {
	private String _defenseurs = "";

	public void setDefenseurs(String str) {
		_defenseurs = str;
	}

	public String getDefenseurs() {
		return _defenseurs;
	}

	public static class Piege {
		private Fighter _caster;
		private Case _cell;
		private byte _size;
		private int _spell;
		private SortStats _trapSpell;
		private Fight _fight;
		private int _color;
		private boolean _isunHide = true;
		private int _teamUnHide = -1;

		public Piege(Fight fight, Fighter caster, Case cell, byte size, SortStats trapSpell, int spell) {
			_fight = fight;
			_caster = caster;
			_cell = cell;
			_spell = spell;
			_size = size;
			_trapSpell = trapSpell;
			_color = Constant.getTrapsColor(spell);
		}

		public Case get_cell() {
			return _cell;
		}

		public byte get_size() {
			return _size;
		}

		public Fighter get_caster() {
			return _caster;
		}

		public void set_isunHide(Fighter f) {
			_isunHide = true;
			_teamUnHide = f.getTeam();
		}

		public boolean get_isunHide() {
			return _isunHide;
		}

		public void desappear() {
			StringBuilder str = new StringBuilder();
			StringBuilder str2 = new StringBuilder();
			StringBuilder str3 = new StringBuilder();
			StringBuilder str4 = new StringBuilder();

			int team = _caster.getTeam() + 1;
			str.append("GDZ-").append(_cell.getID()).append(";").append(_size).append(";").append(_color);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, team, 999, _caster.getGUID() + "", str.toString());
			str2.append("GDC" + _cell.getID());
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, team, 999, _caster.getGUID() + "", str2.toString());
			if (get_isunHide()) {
				int team2 = _teamUnHide + 1;
				str3.append("GDZ-").append(_cell.getID()).append(";").append(_size).append(";").append(_color);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, team2, 999, _caster.getGUID() + "", str3.toString());
				str4.append("GDC").append(_cell.getID());
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, team2, 999, _caster.getGUID() + "", str4.toString());
			}
		}

		public void appear(Fighter f) {
			StringBuilder str = new StringBuilder();
			StringBuilder str2 = new StringBuilder();

			int team = f.getTeam() + 1;
			str.append("GDZ+").append(_cell.getID()).append(";").append(_size).append(";").append(_color);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, team, 999, _caster.getGUID() + "", str.toString());
			str2.append("GDC").append(_cell.getID()).append(";Haaaaaaaaz3005;");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, team, 999, _caster.getGUID() + "", str2.toString());
		}

		public void onTraped(Fighter target) {
			if (target.isDead())
				return;
			_fight.get_traps().remove(this); // on enlève le piège sur lequel
												// target a marché
			desappear(); // On efface le piege
			// On déclenche ses effets
			String str = _spell + "," + _cell.getID() + ",0,1,1," + _caster.getGUID();
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 307, target.getGUID() + "", str);

			ArrayList<Case> cells = new ArrayList<Case>();
			cells.add(_cell);
			// on ajoute les autres cases que couvre le piège
			for (int a = 0; a < _size; a++) {
				char[] dirs = { 'b', 'd', 'f', 'h' };
				ArrayList<Case> cases2 = new ArrayList<Case>();// on évite les
																// modifications
																// concurrentes
				cases2.addAll(cells);
				for (Case aCell : cases2) {
					for (char d : dirs) {
						Case cell = _fight.get_map()
								.getCase(Pathfinding.GetCaseIDFromDirrection(aCell.getID(), d, _fight.get_map(), true));
						if (cell == null)
							continue;
						if (!cells.contains(cell)) {
							cells.add(cell);
						}
					}
				}
			}
			Fighter fakeCaster;
			if (_caster.getPersonnage() == null)
				fakeCaster = new Fighter(_fight, _caster.getMob());
			else
				fakeCaster = new Fighter(_fight, _caster.getPersonnage());
			fakeCaster.set_fightCell(_cell);
			_trapSpell.applySpellEffectToFight(_fight, fakeCaster, target.get_fightCell(), cells, false);
			_fight.verifIfTeamAllDead();
		}

		public int get_color() {
			return _color;
		}
	}

	public static class Fighter {
		private int _id = 0;
		private boolean _canPlay = false;
		private Fight _fight;
		private int _type = 0; // 1 : Personnage, 2 : Mob, 5 : Perco
		private MobGrade _mob = null;
		private Characters _perso = null;
		private Collector _Perco = null;
		private Characters _double = null;
		private int _team = -2;
		private Case _cell;
		private ArrayList<SpellEffect> _fightBuffs = new ArrayList<SpellEffect>();
		private Map<Integer, Integer> _chatiValue = new TreeMap<Integer, Integer>();
		private int _orientation;
		private Fighter _invocator;
		public int _nbInvoc = 0;
		private int _PDVMAX;
		private int _PDV;
		private boolean _isDead;
		private boolean _hasLeft;
		private int _gfxID;
		private Map<Integer, Integer> _state = new TreeMap<Integer, Integer>();
		private Fighter _isHolding;
		private Fighter _holdedBy;
		private ArrayList<LaunchedSort> _launchedSort = new ArrayList<LaunchedSort>();
		private Fighter _oldCible = null;
		private int _LifeLoose = 0;
		private Prism _Prisme = null;
		private String _defenseurs = "";

		public void setDefenseurs(String str) {
			_defenseurs = str;
		}

		public String getDefenseurs() {
			return _defenseurs;
		}

		private boolean _isDeconnected = false;
		private int _tourRestants = 0;
		private int _nbDeco = 0;

		private Characters.BoostSpellStats _SpellBoost = null;

		public Fighter get_oldCible() {
			return _oldCible;
		}

		public void set_oldCible(Fighter cible) {
			_oldCible = cible;
		}

		public Fighter(Fight f, MobGrade mob) {
			_fight = f;
			_type = 2;
			_mob = mob;
			_id = mob.getInFightID();
			_PDVMAX = mob.getPDVMAX();
			_PDV = mob.getPDV();
			_gfxID = getDefaultGfx();
		}

		public Fighter(Fight f, Characters perso) {
			_fight = f;
			if (perso._isClone) {
				_type = 10;
				set_double(perso);
			} else {
				_type = 1;
				_perso = perso;
			}
			_id = perso.get_GUID();
			_PDVMAX = perso.get_PDVMAX();
			_PDV = perso.get_PDV();
			_gfxID = getDefaultGfx();
		}

		public void set_double(Characters _double) {
			this._double = _double;
		}

		public Fighter(Fight Fight, Prism Prisme) {
			_fight = Fight;
			_type = 7;
			_Prisme = Prisme;
			_id = -1;
			_PDVMAX = Prisme.getlevel() * 10000;
			_PDV = Prisme.getlevel() * 10000;
			_gfxID = Prisme.getalignement() == 1 ? 8101 : 8100;
			Prisme.refreshStats();
		}

		public Fighter(Fight f, Collector Perco) {
			_fight = f;
			_type = 5;
			_Perco = Perco;
			_id = -1;
			_PDVMAX = (World.getGuild(Perco.get_guildID()).get_lvl() * 100);
			_PDV = (World.getGuild(Perco.get_guildID()).get_lvl() * 100);
			_gfxID = 6000;
		}

		public void set_PDV(int pdv) {
			_PDV = pdv;
		}

		public void set_PDVMAX(int pdv_max) {
			_PDVMAX = pdv_max;
		}

		public ArrayList<LaunchedSort> getLaunchedSorts() {
			return _launchedSort;
		}

		public void ActualiseLaunchedSort() {
			ArrayList<LaunchedSort> copie = new ArrayList<LaunchedSort>();
			copie.addAll(_launchedSort);
			int i = 0;
			for (LaunchedSort S : copie) {
				S.ActuCooldown();
				if (S.getCooldown() <= 0) {
					_launchedSort.remove(i);
					i--;
				}
				i++;
			}
		}

		public void addLaunchedSort(Fighter target, SortStats sort) {
			LaunchedSort launched = new LaunchedSort(target, sort);
			_launchedSort.add(launched);
		}

		public void addLaunchedFakeSort(Fighter target, SortStats sort, int cooldown) {
			LaunchedSort launched = new LaunchedSort(target, sort);
			launched.setCooldown(cooldown);
			_launchedSort.add(launched);
		}

		public int getGUID() {
			return _id;
		}

		public Fighter get_isHolding() {
			return _isHolding;
		}

		public void set_isHolding(Fighter isHolding) {
			_isHolding = isHolding;
		}

		public Fighter get_holdedBy() {
			return _holdedBy;
		}

		public void set_holdedBy(Fighter holdedBy) {
			_holdedBy = holdedBy;
		}

		public int get_gfxID() {
			return _gfxID;
		}

		public void set_gfxID(int gfxID) {
			_gfxID = gfxID;
		}

		public ArrayList<SpellEffect> get_fightBuff() {
			return _fightBuffs;
		}

		public void set_fightCell(Case cell) {
			_cell = cell;
		}

		public boolean isHide() {
			return hasBuff(150);
		}

		public Case get_fightCell() {
			return _cell;
		}

		public void setTeam(int i) {
			_team = i;
		}

		public boolean isDead() {
			return _isDead;
		}

		public void setDead(boolean isDead) {
			_isDead = isDead;
		}

		public boolean hasLeft() {
			return _hasLeft;
		}

		public void setLeft(boolean hasLeft) {
			_hasLeft = hasLeft;
		}

		public Characters getPersonnage() {
			if (_type == 1)
				return _perso;
			return null;
		}

		public Collector getPerco() {
			if (_type == 5)
				return _Perco;
			return null;
		}

		public Prism getPrisme() {
			if (_type == 7)
				return _Prisme;
			return null;
		}

		public boolean testIfCC(int tauxCC) {
			if (tauxCC < 2)
				return false;
			int agi = getTotalStats().getEffect(Constant.STATS_ADD_AGIL);
			if (agi < 0)
				agi = 0;
			tauxCC -= getTotalStats().getEffect(Constant.STATS_ADD_CC);
			tauxCC = (int) ((tauxCC * 2.9901) / Math.log(agi + 12));// Influence
																	// de l'agi
			if (tauxCC < 2)
				tauxCC = 2;
			int jet = Formulas.getRandomValue(1, tauxCC);
			return (jet == tauxCC);
		}

		public Stats getTotalStats() {
			Stats stats = new Stats(new TreeMap<Integer, Integer>());
			if (_type == 1)// Personnage
				stats = _perso.getTotalStats();
			if (_type == 2)// Mob
				stats = _mob.getStats();
			if (_type == 5)// Percepteur
				stats = World.getGuild(_Perco.get_guildID()).getStatsFight();
			if (_type == 7)
				stats = _Prisme.getStats();
			if (_type == 10)// Double
				stats = _double.getTotalStats();

			stats = Stats.cumulStat(stats, getFightBuffStats());
			return stats;
		}

		public void initBuffStats() {
			if (_type == 1) {
				for (Map.Entry<Integer, SpellEffect> entry : _perso.get_buff().entrySet()) {
					_fightBuffs.add(entry.getValue());
				}
			}
		}

		private Stats getFightBuffStats() {
			Stats stats = new Stats();
			for (SpellEffect entry : _fightBuffs) {
				stats.addOneStat(entry.getEffectID(), entry.getValue());
			}
			return stats;
		}

		public String getGmPacket(char c) {
			StringBuilder str = new StringBuilder();
			str.append("GM|").append(c);
			if (isHide()) {
				str.append("0;");
			} else {
				str.append(_cell.getID()).append(";");
			}
			_orientation = 1;
			str.append(_orientation).append(";");
			str.append("0;");
			str.append(getGUID()).append(";");
			str.append(getPacketsName()).append(";");

			switch (_type) {
			case 1:// Perso
				str.append(_perso.get_classe()).append(";");
				str.append(_perso.get_gfxID()).append("^").append(_perso.get_size()).append(";");
				str.append(_perso.get_sexe()).append(";");
				str.append(_perso.get_lvl()).append(";");
				str.append(_perso.get_align()).append(",");
				str.append("0,");// TODO
				str.append((_perso.is_showWings() ? _perso.getGrade() : "0")).append(",");
				str.append(_perso.get_GUID()).append(";");
				str.append((_perso.get_color1() == -1 ? "-1" : Integer.toHexString(_perso.get_color1()))).append(";");
				str.append((_perso.get_color2() == -1 ? "-1" : Integer.toHexString(_perso.get_color2()))).append(";");
				str.append((_perso.get_color3() == -1 ? "-1" : Integer.toHexString(_perso.get_color3()))).append(";");
				str.append(_perso.getGMStuffString()).append(";");
				str.append(getPDV()).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_PA)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_PM)).append(";");
				if (getTotalStats().getEffect(Constant.STATS_ADD_RP_NEU) >= 50)
					str.append("50").append(";");
				else
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_NEU)).append(";");
				if (getTotalStats().getEffect(Constant.STATS_ADD_RP_TER) >= 50)
					str.append("50").append(";");
				else
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_TER)).append(";");
				if (getTotalStats().getEffect(Constant.STATS_ADD_RP_FEU) >= 50)
					str.append("50").append(";");
				else
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_FEU)).append(";");
				if (getTotalStats().getEffect(Constant.STATS_ADD_RP_EAU) >= 50)
					str.append("50").append(";");
				else
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_EAU)).append(";");
				if (getTotalStats().getEffect(Constant.STATS_ADD_RP_AIR) >= 50)
					str.append("50").append(";");
				else
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_AIR)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_AFLEE)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_MFLEE)).append(";");
				str.append(_team).append(";");
				if (_perso.isOnMount() && _perso.getMount() != null)
					str.append(_perso.getMount().get_color(_perso.parsecolortomount()));
				str.append(";");
				break;
			case 2:// Mob
				str.append("-2;");
				str.append(_mob.getTemplate().getGfxID()).append("^100;");
				str.append(_mob.getGrade()).append(";");
				str.append(_mob.getTemplate().getColors().replace(",", ";")).append(";");
				str.append("0,0,0,0;");
				str.append(this.getPDVMAX()).append(";");
				str.append(_mob.getPA()).append(";");
				str.append(_mob.getPM()).append(";");
				str.append(_team);
				break;
			case 5:// Perco
				str.append("-6;");// Perco
				str.append("6000^");// GFXID^
				Guild G = World.getGuild(Collector.GetPercoGuildID(_fight._mapOld.get_id()));
				str.append(50 + G.get_lvl()).append(";"); // Size
				str.append(G.get_lvl()).append(";");
				str.append("1;");// FIXME
				str.append("2;4;");// FIXME
				str.append((int) Math.floor(G.get_lvl() / 2)).append(";").append((int) Math.floor(G.get_lvl() / 2))
						.append(";").append((int) Math.floor(G.get_lvl() / 2)).append(";")
						.append((int) Math.floor(G.get_lvl() / 2)).append(";").append((int) Math.floor(G.get_lvl() / 2))
						.append(";").append((int) Math.floor(G.get_lvl() / 2)).append(";")
						.append((int) Math.floor(G.get_lvl() / 2)).append(";");// Résistances
				str.append(_team);
				break;
			case 7:// Prisme
				str.append("-2;");
				str.append((_Prisme.getalignement() == 1 ? 8101 : 8100) + "^100;");
				str.append(_Prisme.getlevel() + ";");
				str.append("-1;-1;-1;");
				str.append("0,0,0,0;");
				str.append(getPDVMAX() + ";");
				str.append(0 + ";");
				str.append(0 + ";");
				str.append(_team);
				break;
			case 10:// Double
				str.append(get_double().get_classe()).append(";");
				str.append(get_double().get_gfxID()).append("^").append(get_double().get_size()).append(";");
				str.append(get_double().get_sexe()).append(";");
				str.append(get_double().get_lvl()).append(";");
				str.append(get_double().get_align()).append(",");
				str.append("0,");// TODO
				str.append((get_double().is_showWings() ? get_double().getALvl() : "0")).append(",");
				str.append(get_double().get_GUID()).append(";");
				str.append((get_double().get_color1() == -1 ? "-1" : Integer.toHexString(get_double().get_color1())))
						.append(";");
				str.append((get_double().get_color2() == -1 ? "-1" : Integer.toHexString(get_double().get_color2())))
						.append(";");
				str.append((get_double().get_color3() == -1 ? "-1" : Integer.toHexString(get_double().get_color3())))
						.append(";");
				str.append(get_double().getGMStuffString()).append(";");
				str.append(getPDV()).append(";");

				int pa = getTotalStats().getEffect(Constant.STATS_ADD_PA) > 12 ? 12
						: getTotalStats().getEffect(Constant.STATS_ADD_PA);
				int pm = getTotalStats().getEffect(Constant.STATS_ADD_PM) > 6 ? 6
						: getTotalStats().getEffect(Constant.STATS_ADD_PM);

				str.append(pa).append(";");
				str.append(pm).append(";");
				if (getTotalStats().getEffect(Constant.STATS_ADD_RP_NEU) >= 50)
					str.append("50").append(";");
				else
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_NEU)).append(";");
				if (getTotalStats().getEffect(Constant.STATS_ADD_RP_TER) >= 50)
					str.append("50").append(";");
				else
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_TER)).append(";");
				if (getTotalStats().getEffect(Constant.STATS_ADD_RP_FEU) >= 50)
					str.append("50").append(";");
				else
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_FEU)).append(";");
				if (getTotalStats().getEffect(Constant.STATS_ADD_RP_EAU) >= 50)
					str.append("50").append(";");
				else
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_EAU)).append(";");
				if (getTotalStats().getEffect(Constant.STATS_ADD_RP_AIR) >= 50)
					str.append("50").append(";");
				else
					str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_AIR)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_AFLEE)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_MFLEE)).append(";");
				str.append(_team).append(";");
				if (get_double().isOnMount() && get_double().getMount() != null)
					str.append(get_double().getMount().get_color());
				str.append(";");
				break;
			}

			return str.toString();
		}

		public Characters get_double() {
			return _double;
		}

		public void setState(int id, int t) {
			_state.remove(id);
			if (t != 0)
				_state.put(id, t);
		}

		public void sendState(Characters p) {
			if (p.get_compte() == null || p.get_compte().getGameThread() == null)
				return;
			for (Entry<Integer, Integer> state : _state.entrySet()) {
				SocketManager.GAME_SEND_GA_PACKET(p.get_compte().getGameThread().get_out(), 7 + "", 950 + "",
						getGUID() + "", getGUID() + "," + state.getKey() + ",1");
			}
		}

		public boolean isState(int id) {
			if (_state.get(id) == null)
				return false;
			return _state.get(id) != 0;
		}

		public void decrementStates() {
			// Copie pour évident les modif concurrentes
			ArrayList<Entry<Integer, Integer>> entries = new ArrayList<Entry<Integer, Integer>>();
			entries.addAll(_state.entrySet());
			for (Entry<Integer, Integer> e : entries) {
				// Si la valeur est négative, on y touche pas
				if (e.getKey() < 0)
					continue;

				_state.remove(e.getKey());
				int nVal = e.getValue() - 1;
				// Si 0 on ne remet pas la valeur dans le tableau
				if (nVal == 0)// ne pas mettre plus petit, -1 = infinie
				{
					// on envoie au org.walaka.rubrumsolem.client la
					// desactivation de l'état
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 950, getGUID() + "",
							getGUID() + "," + e.getKey() + ",0");
					continue;
				}
				// Sinon on remet avec la nouvelle valeur
				_state.put(e.getKey(), nVal);
			}
		}

		public int getPDV() {
			int pdv = _PDV + getBuffValue(Constant.STATS_ADD_VITA);
			return pdv;
		}

		public void removePDV(int pdv) {
			_LifeLoose += pdv;
			int pdvLoose = (_LifeLoose * 3) / 100;
			if (pdvLoose > 0) {
				_PDVMAX -= pdvLoose;
				_LifeLoose -= pdvLoose;
			}
			_PDV -= pdv;
			if (_PDV > _PDVMAX)
				_PDV = _PDVMAX;
		}

		public void applyBeginningTurnBuff(Fight fight) {
			synchronized (_fightBuffs) {
				for (int effectID : Constant.BEGIN_TURN_BUFF) {
					// On évite les modifications concurrentes
					ArrayList<SpellEffect> buffs = new ArrayList<SpellEffect>();
					buffs.addAll(_fightBuffs);
					for (SpellEffect entry : buffs) {
						if (entry.getEffectID() == effectID) {

							Logs.addToGameLog("Effet de debut de tour : " + effectID);
							entry.applyBeginingBuff(fight, this);
						}
					}
				}
			}
		}

		public SpellEffect getBuff(int id) {
			for (SpellEffect entry : _fightBuffs) {
				if (entry.getEffectID() == id && entry.getDuration() > 0) {
					return entry;
				}
			}
			return null;
		}

		public boolean hasBuff(int id) {
			for (SpellEffect entry : _fightBuffs) {
				if (entry.getEffectID() == id && entry.getDuration() > 0) {
					return true;
				}
			}
			return false;
		}

		public int getBuffValue(int id) {
			int value = 0;
			for (SpellEffect entry : _fightBuffs) {
				if (entry.getEffectID() == id)
					value += entry.getValue();
			}
			return value;
		}

		public int getMaitriseDmg(int id) {
			int value = 0;
			for (SpellEffect entry : _fightBuffs) {
				if (entry.getSpell() == id)
					value += entry.getValue();
			}
			return value;
		}

		public boolean getSpellValueBool(int id) {
			for (SpellEffect entry : _fightBuffs) {
				if (entry.getSpell() == id)
					return true;
			}
			return false;
		}

		public void refreshfightBuff() {
			// Copie pour contrer les modifications Concurentes
			ArrayList<SpellEffect> b = new ArrayList<SpellEffect>();
			for (SpellEffect entry : _fightBuffs) {
				if (entry.decrementDuration() > 0)// Si pas fin du buff
				{
					b.add(entry);
				} else {

					Logs.addToGameLog(
							"Suppression du buff " + entry.getEffectID() + " sur le joueur Fighter ID= " + getGUID());
					switch (entry.getEffectID()) {
					case 108:
						if (entry.getSpell() == 441) {
							// Baisse des pdvs max
							_PDVMAX = (_PDVMAX - entry.getValue());

							// Baisse des pdvs actuel
							int pdv = 0;
							if (_PDV - entry.getValue() <= 0) {
								pdv = 0;
								_fight.onFighterDie(this, this);
								_fight.verifIfTeamAllDead();
							} else
								pdv = (_PDV - entry.getValue());
							_PDV = pdv;
						}
						break;

					case 150:// Invisibilité
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 150, entry.getCaster().getGUID() + "",
								getGUID() + ",0");
						break;

					case 950:
						String args = entry.getArgs();
						int id = -1;
						try {
							id = Integer.parseInt(args.split(";")[2]);
						} catch (Exception e) {
						}
						if (id == -1)
							return;
						setState(id, 0);
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 950, entry.getCaster().getGUID() + "",
								entry.getCaster().getGUID() + "," + id + ",0");
						break;
					}
				}
			}
			_fightBuffs.clear();
			_fightBuffs.addAll(b);
		}

		public void addBuff(int id, int val, int duration, int turns, boolean debuff, int spellID, String args,
				Fighter caster, boolean isPoison) {
			/*
			 * if(spellID == 99 || spellID == 5 || spellID == 20 || spellID ==
			 * 127 || spellID == 89 || spellID == 126 || spellID == 115 ||
			 * spellID == 192 || spellID == 4 || spellID == 1 || spellID == 6 ||
			 * spellID == 14 || spellID == 18 || spellID == 7 || spellID == 284
			 * || spellID == 197 || spellID == 704 ) { //Trêve //Immu
			 * //Prévention //Momification //Dévouement //Mot stimulant //Odorat
			 * //Ronce Apaisante //Renvoi de sort //Armure Incandescente
			 * //Armure Terrestre //Armure Venteuse //Armure Aqueuse //Bouclier
			 * Féca //Accélération Poupesque //Puissance Sylvestre //Pandanlku
			 * /*debuff = true; }
			 */
			debuff = true;
			isPoison = true;
			if (Config.CONFIG_SORT_INDEBUFFABLE != null || !Config.CONFIG_SORT_INDEBUFFABLE.isEmpty()) {
				for (String split : Config.CONFIG_SORT_INDEBUFFABLE.split("\\|")) {
					String[] infos = split.split(":");
					if (!debuff)
						continue;
					int sortID = Integer.parseInt(infos[0]);
					if (spellID == sortID)
						debuff = false;
				}
			}
			// Si c'est le jouer actif qui s'autoBuff, on ajoute 1 a la durée
			if (id == 781) {
				if (caster.getGUID() == this.getGUID())
					return;
				_fightBuffs.add(new SpellEffect(id, val, (_canPlay ? duration + 1 : duration), turns, debuff, caster,
						args, spellID, isPoison));
			} else {
				_fightBuffs.add(new SpellEffect(id, val, (_canPlay ? duration + 1 : duration), turns, debuff, caster,
						args, spellID, isPoison));
			}

			Logs.addToGameLog("Ajout du Buff " + id + " sur le personnage Fighter ID = " + this.getGUID() + "("
					+ caster.getGUID() + ") val : " + val + " duration : " + duration + " turns : " + turns
					+ " debuff : " + debuff + " spellid : " + spellID + " args : " + args);

			switch (id) {
			case 6:// Renvoie de sort
				SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), -1, val + "", "10", "", duration,
						spellID);
				break;

			case 79:// Chance éca
				val = Integer.parseInt(args.split(";")[0]);
				String valMax = args.split(";")[1];
				String chance = args.split(";")[2];
				SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, valMax, chance, "", duration,
						spellID);
				break;

			case 788:// Fait apparaitre message le temps de buff sacri Chatiment
						// de X sur Y tours
				val = Integer.parseInt(args.split(";")[1]);
				String valMax2 = args.split(";")[2];
				if (Integer.parseInt(args.split(";")[0]) == 108)
					return;
				SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, "" + val, "" + valMax2, "",
						duration, spellID);

				break;

			case 98:// Poison insidieux
			case 107:// Mot d'épine (2à3), Contre(3)
			case 100:// Flèche Empoisonnée, Tout ou rien
			case 108:// Mot de Régénération, Tout ou rien
			case 165:// Maîtrises
				val = Integer.parseInt(args.split(";")[0]);
				String valMax1 = args.split(";")[1];
				if (valMax1.compareTo("-1") == 0 || spellID == 82 || spellID == 94) {
					SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, "", "", "", duration,
							spellID);
				} else if (valMax1.compareTo("-1") != 0) {
					SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, valMax1, "", "", duration,
							spellID);
				}
				break;

			default:
				SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, "", "", "", duration,
						spellID);
				break;
			}
		}

		public int getInitiative() {
			if (_type == 1)
				return _perso.getInitiative();
			if (_type == 2)
				return _mob.getInit();
			if (_type == 5)
				return World.getGuild(_Perco.get_guildID()).get_lvl();
			if (_type == 10)
				return _double.getInitiative();

			return 0;
		}

		public int getPDVMAX() {
			return _PDVMAX + getBuffValue(Constant.STATS_ADD_VITA);
		}

		public int get_lvl() {
			if (_type == 1)
				return _perso.get_lvl();
			if (_type == 2)
				return _mob.getLevel();
			if (_type == 5)
				return World.getGuild(_Perco.get_guildID()).get_lvl();
			if (_type == 7)
				return _Prisme.getlevel();
			if (_type == 10)
				return _double.get_lvl();

			return 0;
		}

		public String xpString(String str) {
			if (_perso != null) {
				int max = _perso.get_lvl() + 1;
				if (max > World.getExpLevelSize())
					max = World.getExpLevelSize();
				return World.getExpLevel(_perso.get_lvl()).perso + str + _perso.get_curExp() + str
						+ World.getExpLevel(max).perso;
			}
			return "0" + str + "0" + str + "0";
		}

		public String getPacketsName() {
			if (_type == 1)
				return _perso.get_name();
			if (_type == 2)
				return _mob.getTemplate().getID() + "";
			if (_type == 5)
				return (_Perco.get_N1() + "," + _Perco.get_N2());
			if (_type == 7)
				return (_Prisme.getalignement() == 1 ? 1111 : 1112) + "";
			if (_type == 10)
				return _double.get_name();

			return "";
		}

		public MobGrade getMob() {
			if (_type == 2)
				return _mob;

			return null;
		}

		public int getTeam() {
			return _team;
		}

		public int getTeam2() {
			return _fight.getTeamID(_id);
		}

		public int getOtherTeam() {
			return _fight.getOtherTeamID(_id);
		}

		public boolean canPlay() {
			return _canPlay;
		}

		public void setCanPlay(boolean b) {
			_canPlay = b;
		}

		public ArrayList<SpellEffect> getBuffsByEffectID(int effectID) {
			ArrayList<SpellEffect> buffs = new ArrayList<SpellEffect>();
			for (SpellEffect buff : _fightBuffs) {
				if (buff.getEffectID() == effectID)
					buffs.add(buff);
			}
			return buffs;
		}

		public Stats getTotalStatsLessBuff() {
			Stats stats = new Stats(new TreeMap<Integer, Integer>());
			if (_type == 1)
				stats = _perso.getTotalStats(true);
			if (_type == 2)
				stats = _mob.getStats();
			if (_type == 5)
				stats = World.getGuild(_Perco.get_guildID()).getStatsFight();
			if (_type == 7)
				stats = _Prisme.getStats();
			if (_type == 10)
				stats = _double.getTotalStats(true);

			return stats;
		}

		public int getPA() {
			if (_type == 1)
				return getTotalStats().getEffect(Constant.STATS_ADD_PA);
			if (_type == 2)
				return getTotalStats().getEffect(Constant.STATS_ADD_PA) + _mob.getPA();
			if (_type == 5)
				return getTotalStats().getEffect(Constant.STATS_ADD_PM) + 6;
			if (_type == 10) {
				int PA = getTotalStats().getEffect(Constant.STATS_ADD_PA) > 12 ? 12
						: getTotalStats().getEffect(Constant.STATS_ADD_PA);
				PA += this.getBuffValue(Constant.STATS_ADD_PA);
				return PA;
			}

			return 0;
		}

		public int getPM() {
			if (_type == 1)
				return getTotalStats().getEffect(Constant.STATS_ADD_PM);
			if (_type == 2)
				return getTotalStats().getEffect(Constant.STATS_ADD_PM) + _mob.getPM();
			if (_type == 5)
				return getTotalStats().getEffect(Constant.STATS_ADD_PM) + 3;
			if (_type == 10) {
				int PM = getTotalStats().getEffect(Constant.STATS_ADD_PM) > 6 ? 6
						: getTotalStats().getEffect(Constant.STATS_ADD_PM);
				PM += this.getBuffValue(Constant.STATS_ADD_PM);
				return PM;
			}

			return 0;
		}

		public int getCurPA(Fight fight) {
			return fight._curFighterPA;
		}

		public int getCurPM(Fight fight) {
			return fight._curFighterPM;
		}

		public void setCurPM(Fight fight, int pm) {
			fight._curFighterPM = pm;
		}

		public void setCurPA(Fight fight, int pa) {
			fight._curFighterPA = pa;
		}

		public void setInvocator(Fighter caster) {
			_invocator = caster;
		}

		public Fighter getInvocator() {
			return _invocator;
		}

		public boolean isInvocation() {
			return (_invocator != null);
		}

		public boolean isPerco() {
			return (_Perco != null);
		}

		public boolean isDouble() {
			return (_double != null);
		}

		public boolean isPrisme() {
			return (_Prisme != null);
		}

		public void debuff() {
			ArrayList<SpellEffect> newBuffs = new ArrayList<SpellEffect>();
			// on vérifie chaque buff en cours, si pas débuffable, on l'ajout a
			// la nouvelle liste
			for (SpellEffect SE : _fightBuffs) {
				if (!SE.isDebuffabe())
					newBuffs.add(SE);
				// On envoie les Packets si besoin
				switch (SE.getEffectID()) {
				case Constant.STATS_ADD_PA:
				case Constant.STATS_ADD_PA2:
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 101, getGUID() + "",
							getGUID() + ",-" + SE.getValue());
					break;

				case Constant.STATS_ADD_PM:
				case Constant.STATS_ADD_PM2:
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 127, getGUID() + "",
							getGUID() + ",-" + SE.getValue());
					break;
				case 150:
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 150, getGUID() + "", getGUID() + ",0");
					// On actualise la position
					SocketManager.GAME_SEND_GIC_PACKET_TO_FIGHT(_fight, 7, this);
					break;
				case 950:
					String args = SE.getArgs();
					int id = -1;
					try {
						id = Integer.parseInt(args.split(";")[2]);
					} catch (Exception e) {
					}
					if (id == -1)
						return;
					setState(id, 0);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 950, SE.getCaster().getGUID() + "",
							SE.getCaster().getGUID() + "," + id + ",0");
					break;
				}
			}
			_fightBuffs.clear();
			_fightBuffs.addAll(newBuffs);
			if (_perso != null && !_hasLeft)
				SocketManager.GAME_SEND_STATS_PACKET(_perso);
		}

		public void fullPDV() {
			_PDV = _PDVMAX;
		}

		public void setIsDead(boolean b) {
			_isDead = b;
		}

		public void unHide(int spellid) {
			// on retire le buff invi
			if (spellid != -1)// -1 : CAC
			{
				switch (spellid) {
				case 66:
				case 71:
				case 181:
				case 196:
				case 200:
				case 219:
					return;
				}
			}
			ArrayList<SpellEffect> buffs = new ArrayList<SpellEffect>();
			buffs.addAll(get_fightBuff());
			for (SpellEffect SE : buffs) {
				if (SE.getEffectID() == 150)
					get_fightBuff().remove(SE);
			}
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 150, getGUID() + "", getGUID() + ",0");
			// On actualise la position
			SocketManager.GAME_SEND_GIC_PACKET_TO_FIGHT(_fight, 7, this);
		}

		public int getPdvMaxOutFight() {
			if (_perso != null)
				return _perso.get_PDVMAX();
			if (_mob != null)
				return _mob.getPDVMAX();
			return 0;
		}

		public Map<Integer, Integer> get_chatiValue() {
			return _chatiValue;
		}

		public int getDefaultGfx() {
			if (_perso != null)
				return _perso.get_gfxID();
			if (_mob != null)
				return _mob.getTemplate().getGfxID();
			return 0;
		}

		public long getXpGive() {
			if (_mob != null)
				return _mob.getBaseXp();
			return 0;
		}

		public void addPDV(int max) {
			_PDVMAX = (_PDVMAX + max);
			_PDV = (_PDV + max);
		}

		public boolean canLaunchSpell(int spellID) {
			if (!this.getPersonnage().hasSpell(spellID))
				return false;
			else
				return LaunchedSort.coolDownGood(this, spellID);
		}

		public void deleteBuffByFighter(Fighter F) {
			ArrayList<SpellEffect> b = new ArrayList<SpellEffect>();
			for (SpellEffect entry : _fightBuffs) {
				if (entry.getCaster().getGUID() != F.getGUID())// Si pas fin du
																// buff
				{
					b.add(entry);
				} else {

					Logs.addToGameLog(
							"Suppression du buff " + entry.getEffectID() + " sur le joueur Fighter ID= " + getGUID());
					switch (entry.getEffectID()) {
					case Constant.STATS_ADD_PA:
					case Constant.STATS_ADD_PA2:
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 101, getGUID() + "",
								getGUID() + ",-" + entry.getValue());
						break;

					case Constant.STATS_ADD_PM:
					case Constant.STATS_ADD_PM2:
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 127, getGUID() + "",
								getGUID() + ",-" + entry.getValue());
						break;
					case 108:
						if (entry.getSpell() == 441) {
							// Baisse des pdvs max
							_PDVMAX = (_PDVMAX - entry.getValue());

							// Baisse des pdvs actuel
							int pdv = 0;
							if (_PDV - entry.getValue() <= 0) {
								pdv = 0;
								_fight.onFighterDie(this, entry.getCaster());
								_fight.verifIfTeamAllDead();
							} else
								pdv = (_PDV - entry.getValue());
							_PDV = pdv;
						}
						break;

					case 150:// Invisibilité
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 150, entry.getCaster().getGUID() + "",
								getGUID() + ",0");
						break;

					case 950:
						String args = entry.getArgs();
						int id = -1;
						try {
							id = Integer.parseInt(args.split(";")[2]);
						} catch (Exception e) {
						}
						if (id == -1)
							return;
						setState(id, 0);
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 950, entry.getCaster().getGUID() + "",
								entry.getCaster().getGUID() + "," + id + ",0");
						break;
					}
				}
			}
			_fightBuffs.clear();
			_fightBuffs.addAll(b);
			if (_perso != null && !_hasLeft)
				SocketManager.GAME_SEND_STATS_PACKET(_perso);
		}

		public void setSpellStats() {
			_SpellBoost = _perso.getTotalBoostSpellStats();
		}

		public boolean haveSpellStat(int spellID, int statID) {
			if (_SpellBoost == null)
				return false;
			return _SpellBoost.haveStat(spellID, statID);
		}

		public int getSpellStat(int spellID, int statID) {
			if (_SpellBoost == null)
				return 0;
			return _SpellBoost.getStat(spellID, statID);
		}

		// Déconnexion en combat
		public void Deconnect() {
			if (_isDeconnected)
				return;
			_isDeconnected = true;
			_tourRestants = 20;
			_nbDeco++;
		}

		public int getNBDeco() {
			return _nbDeco;
		}

		public void Reconnect() {
			_isDeconnected = false;
			_tourRestants = 0;
		}

		public boolean isDeconnected() {
			if (_hasLeft)
				return false;
			return _isDeconnected;
		}

		public int getToursRestants() {
			return _tourRestants;
		}

		public void newTurn() {
			_tourRestants--;
		}

		public int getPDVWithBuff() {
			return _PDV + getBuffValue(125);
		}
	}

	public static class Glyphe {
		private Fighter _caster;
		private Case _cell;
		private byte _size;
		private int _spell;
		private SortStats _trapSpell;
		private byte _duration;
		private Fight _fight;
		private int _color;

		public Glyphe(Fight fight, Fighter caster, Case cell, byte size, SortStats trapSpell, byte duration,
				int spell) {
			_fight = fight;
			_caster = caster;
			_cell = cell;
			_spell = spell;
			_size = size;
			_trapSpell = trapSpell;
			_duration = duration;
			_color = Constant.getGlyphColor(spell);
		}

		public Case get_cell() {
			return _cell;
		}

		public byte get_size() {
			return _size;
		}

		public Fighter get_caster() {
			return _caster;
		}

		public byte get_duration() {
			return _duration;
		}

		public int decrementDuration() {
			_duration--;
			return _duration;
		}

		public void onTraped(Fighter target) {
			String str = _spell + "," + _cell.getID() + ",0,1,1," + _caster.getGUID();
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 307, target.getGUID() + "", str);
			_trapSpell.applySpellEffectToFight(_fight, _caster, target.get_fightCell(), false);
			_fight.verifIfTeamAllDead();
		}

		public void desapear() {
			SocketManager.GAME_SEND_GDZ_PACKET_TO_FIGHT(_fight, 7, "-", _cell.getID(), _size, _color);
			SocketManager.GAME_SEND_GDC_PACKET_TO_FIGHT(_fight, 7, _cell.getID());
		}

		public int get_color() {
			return _color;
		}
	}

	public static class LaunchedSort {
		private int _spellId = 0;
		private int _cooldown = 0;
		private Fighter _target = null;

		public LaunchedSort(Fighter t, SortStats SS) {
			_target = t;
			_spellId = SS.getSpellID();
			_cooldown = SS.getCoolDown(t);
		}

		public void ActuCooldown() {
			_cooldown--;
		}

		public int getCooldown() {
			return _cooldown;
		}

		public void setCooldown(int c) {
			_cooldown = c;
		}

		public int getId() {
			return _spellId;
		}

		public Fighter getTarget() {
			return _target;
		}

		public static boolean coolDownGood(Fighter fighter, int id) {

			for (LaunchedSort S : fighter.getLaunchedSorts()) {
				if (S._spellId == id && S.getCooldown() > 0)
					return false;
			}
			return true;
		}

		public static int getNbLaunch(Fighter fighter, int id) {
			int nb = 0;
			for (LaunchedSort S : fighter.getLaunchedSorts()) {
				if (S._spellId == id)
					nb++;
			}
			return nb;
		}

		public static int getNbLaunchTarget(Fighter fighter, Fighter target, int id) {
			int nb = 0;
			for (LaunchedSort S : fighter.getLaunchedSorts()) {
				if (S._target == null || target == null)
					continue;
				if (S._spellId == id && S._target.getGUID() == target.getGUID())
					nb++;
			}
			return nb;
		}

	}

	private int _id;
	private Map<Integer, Fighter> _team0 = new TreeMap<Integer, Fighter>();
	private Map<Integer, Fighter> _team1 = new TreeMap<Integer, Fighter>();
	private Map<Integer, Fighter> deadList = new TreeMap<Integer, Fighter>();
	private Map<Integer, Characters> _spec = new TreeMap<Integer, Characters>();
	private Maps _map;
	private Maps _mapOld;
	private Fighter _init0;
	private Fighter _init1;
	private ArrayList<Case> _start0 = new ArrayList<Case>();
	private ArrayList<Case> _start1 = new ArrayList<Case>();
	private int _state = 0;
	private int _guildID = -1;
	private int _type = -1;
	private boolean locked0 = false;
	private boolean onlyGroup0 = false;
	private boolean locked1 = false;
	private boolean onlyGroup1 = false;
	private boolean specOk = true;
	private boolean help1 = false;
	private boolean help2 = false;
	private int _st2;
	private int _st1;
	private int _curPlayer;
	private long _startTime = 0;
	private int _curFighterPA;
	private int _curFighterPM;
	private int _curFighterUsedPA;
	private int _curFighterUsedPM;
	private String _curAction = "";
	private List<Fighter> _ordreJeu = new ArrayList<Fighter>();
	private List<Glyphe> _glyphs = new ArrayList<Glyphe>();
	private List<Piege> _traps = new ArrayList<Piege>();
	private MobGroup _mobGroup;
	private Collector _perco;
	private Prism _Prisme;
	private IAThread _IAThreads = null;

	private ArrayList<Fighter> _captureur = new ArrayList<Fighter>(8); // Création
																		// d'une
																		// liste
																		// de
																		// longueur
																		// 8.
																		// Les
																		// combats
																		// contiennent
																		// un
																		// max
																		// de 8
																		// Attaquant
	private boolean isCapturable = false;
	private int captWinner = -1;
	private SoulStone pierrePleine;
	private Map<Integer, Challenge> _challenges = new TreeMap<Integer, Challenge>();
	private Map<Integer, Case> _raulebaque = new TreeMap<Integer, Case>();
	private long TimeStartTurn = 0L;

	// Approisement de DD
	private ArrayList<Fighter> _apprivoiseur = new ArrayList<Fighter>(8);
	private boolean CanCaptu = false;
	boolean ThereAreThree = true;
	boolean ThereAreAmandDore = true;
	boolean ThereAreAmandRousse = true;
	boolean ThereAreRousseDore = true;
	boolean ThereIsAmand = true;
	boolean ThereIsDore = true;
	boolean ThereIsRousse = true;

	// Fights
	private boolean FightStarted = false;
	private TimerTask actTimerTask = null;
	// Anti coop/transpo
	private boolean hasUsedCoopTranspo = false;

	private boolean checkTimer;

	// protector
	private Fighter protector;

	/* Timers by Return/Skryn - Noraj ;) */
	public synchronized void startTimer(int time, final boolean isTurn) {
		Config.fightTimers.schedule(actTimerTask = new TimerTask() {
			public void run() {
				try {
					if (isTurn) {
						endTurn(true);
					} else {
						if (_Prisme != null) {
							_Prisme.removeTurnTimer(1000);
						} else if (_perco != null)
							_perco.removeTurnTimer(1000);
						if (!FightStarted)
							startFight();
						if (_Prisme != null)
							_Prisme.setTurnTime(60000);
						else if (_perco != null)
							_perco.set_timeTurn(45000);
					}
					this.cancel();
					return;
				} catch (Exception e) {
					if (!isTurn)
						Logs.addToGameLog(">Début de combat échoué sur la map " + get_map().get_id() + " !");
					else
						Logs.addToGameLog(">Fin de tour échoué ou déjà passé sur la map " + get_map().get_id() + " !");
					this.cancel();
					return;
				}
			}
		}, time);

	}

	public Fight(int id, Maps Carte, Characters perso, Prism Prisme) {

		Prisme.setInFight(0);
		Prisme.setFightID(id);
		_type = Constant.FIGHT_TYPE_CONQUETE; // (0: Défie) 2: Prisme (4: Pvm) (1:PVP) (5:Perco)
		_id = id;
		_map = Carte.getMapCopy();
		_mapOld = Carte;
		_init0 = new Fighter(this, perso);
		_Prisme = Prisme;
		_team0.put(perso.get_GUID(), _init0);
		Fighter lPrisme = new Fighter(this, Prisme);
		_team1.put(-1, lPrisme);
		// TODO prisme
		SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 1, 2, 0, 1, 0, 60000, _type);
		SocketManager.GAME_SEND_ILF_PACKET(perso, 0);
		startTimer(60000, false);
		Random teams = new Random();
		if (teams.nextBoolean()) {
			_start0 = parsePlaces(0);
			_start1 = parsePlaces(1);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 0);
			_st1 = 0;
			_st2 = 1;
		} else {
			_start0 = parsePlaces(1);
			_start1 = parsePlaces(0);
			_st1 = 1;
			_st2 = 0;
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 1);
		}
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID() + "",
				perso.get_GUID() + "," + Constant.ETAT_PORTE + ",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID() + "",
				perso.get_GUID() + "," + Constant.ETAT_PORTEUR + ",0");
		List<Entry<Integer, Fighter>> e = new ArrayList<Entry<Integer, Fighter>>();
		e.addAll(_team1.entrySet());
		for (Entry<Integer, Fighter> entry : e) {
			Fighter f = entry.getValue();
			Case cell = getRandomCell(_start1);
			if (cell == null) {
				_team1.remove(f.getGUID());
				continue;
			}
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.getGUID() + "",
					f.getGUID() + "," + Constant.ETAT_PORTE + ",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.getGUID() + "",
					f.getGUID() + "," + Constant.ETAT_PORTEUR + ",0");
			f.set_fightCell(cell);
			f.get_fightCell().addFighter(f);
			f.setTeam(1);
			f.fullPDV();
		}
		_init0.set_fightCell(getRandomCell(_start0));
		_init0.getPersonnage().get_curCell().removePlayer(_init0.getPersonnage().get_GUID());
		_init0.get_fightCell().addFighter(_init0);
		_init0.getPersonnage().set_fight(this);
		_init0.setTeam(0);
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID());
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), Prisme.getID());
		SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), 0, _init0.getGUID(),
				Prisme.getID(), _init0.getPersonnage().get_curCell().getID(), "0;" + _init0.getPersonnage().get_align(),
				Prisme.getCell(), "0;" + Prisme.getalignement());
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID(),
				_init0);
		for (Fighter f : _team1.values()) {
			SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), Prisme.getID(), f);
		}
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, _map);
		set_state(Constant.FIGHT_STATE_PLACE);

		String str = "";
		if (_Prisme != null)
			str = Prisme.getCarte() + "|" + Prisme.getX() + "|" + Prisme.getY();
		for (Characters z : World.getOnlinePersos()) {
			if (z == null)
				continue;
			if (z.get_align() != Prisme.getalignement())
				continue;
			SocketManager.SEND_CA_ATTAQUE_MESSAGE_PRISME(z, str);
		}
	}

	public Fight(int type, int id, Maps map, Characters init1, Characters init2, boolean init2Protected) {
		_type = type; // 0: Défie (4: Pvm) 1: PVP (5: Perco)
		_id = id;
		_map = map.getMapCopy();
		_mapOld = map;
		_init0 = new Fighter(this, init1);
		_init1 = new Fighter(this, init2);
		_team0.put(init1.get_GUID(), _init0);
		_team1.put(init2.get_GUID(), _init1);
		if (init2Protected) {
			protector = new Fighter(this,
					World.getMonstre(394).getGradeByLevel(Constant.getProtectorLevelByAttacker(_init0)));
			_team1.put(protector.getGUID(), protector);
		}
		// on desactive le timer de regen coté org.walaka.rubrumsolem.client
		SocketManager.GAME_SEND_ILF_PACKET(init1, 0);
		SocketManager.GAME_SEND_ILF_PACKET(init2, 0);

		int cancelBtn = _type == Constant.FIGHT_TYPE_CHALLENGE ? 1 : 0;
		long time = _type == Constant.FIGHT_TYPE_CHALLENGE ? 0 : Config.CONFIG_MS_FOR_START_FIGHT;
		SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 7, 2, cancelBtn, 1, 0, time, _type);
		startTimer(46000, false); // Thread Timer
		Random teams = new Random();
		if (teams.nextBoolean()) {
			_start0 = parsePlaces(0);
			_start1 = parsePlaces(1);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 0);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 2, _map.get_placesStr(), 1);
			_st1 = 0;
			_st2 = 1;
		} else {
			_start0 = parsePlaces(1);
			_start1 = parsePlaces(0);
			_st1 = 1;
			_st2 = 0;
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 1);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 2, _map.get_placesStr(), 0);
		}
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init1.get_GUID() + "",
				init1.get_GUID() + "," + Constant.ETAT_PORTE + ",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init1.get_GUID() + "",
				init1.get_GUID() + "," + Constant.ETAT_PORTEUR + ",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init2.get_GUID() + "",
				init2.get_GUID() + "," + Constant.ETAT_PORTE + ",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init2.get_GUID() + "",
				init2.get_GUID() + "," + Constant.ETAT_PORTEUR + ",0");

		if (init2Protected) {
			Case cell = null;
			do {
				cell = getRandomCell(_start1);
			} while (cell == null);

			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, protector.getGUID() + "",
					protector.getGUID() + "," + Constant.ETAT_PORTE + ",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(Fight.this, 3, 950, protector.getGUID() + "",
					protector.getGUID() + "," + Constant.ETAT_PORTEUR + ",0");
			protector.set_fightCell(cell);
			protector.get_fightCell().addFighter(protector);
			protector.setTeam(1);
			protector.fullPDV();
			SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(this._map, protector.getGUID(), protector);
		}

		_init0.set_fightCell(getRandomCell(_start0));
		_init1.set_fightCell(getRandomCell(_start1));

		_init0.getPersonnage().get_curCell().removePlayer(_init0.getGUID());
		_init1.getPersonnage().get_curCell().removePlayer(_init1.getGUID());

		_init0.get_fightCell().addFighter(_init0);
		_init1.get_fightCell().addFighter(_init1);
		_init0.getPersonnage().set_fight(this);
		_init0.setTeam(0);
		_init1.getPersonnage().set_fight(this);
		_init1.setTeam(1);
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID());
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init1.getPersonnage().get_curCarte(), _init1.getGUID());
		if (_type == 1) {
			SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), 0,
					_init0.getGUID(), _init1.getGUID(), _init0.getPersonnage().get_curCell().getID(),
					"0;" + _init0.getPersonnage().get_align(), _init1.getPersonnage().get_curCell().getID(),
					"0;" + _init1.getPersonnage().get_align());
		} else {
			SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), 0,
					_init0.getGUID(), _init1.getGUID(), _init0.getPersonnage().get_curCell().getID(), "0;-1",
					_init1.getPersonnage().get_curCell().getID(), "0;-1");
		}
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID(),
				_init0);
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), _init1.getGUID(),
				_init1);

		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, _map);

		set_state(Constant.FIGHT_STATE_PLACE);
	}

	public Fight(int id, Maps map, Characters init1, MobGroup group) {
		_mobGroup = group;
		_type = Constant.FIGHT_TYPE_PVM; // (0: Défie) 4: Pvm (1:PVP) (5:Perco)
		_id = id;
		_map = map.getMapCopy();
		_mapOld = map;
		_init0 = new Fighter(this, init1);

		_team0.put(init1.get_GUID(), _init0);
		for (Entry<Integer, MobGrade> entry : group.getMobs().entrySet()) {
			entry.getValue().setInFightID(entry.getKey());
			Fighter mob = new Fighter(this, entry.getValue());
			_team1.put(entry.getKey(), mob);
		}
		// on desactive le timer de regen coté org.walaka.rubrumsolem.client
		SocketManager.GAME_SEND_ILF_PACKET(init1, 0);

		// on envoie le timer ?
		SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 1, 2, 0, 1, 0, Config.CONFIG_MS_FOR_START_FIGHT, _type);
		startTimer(46000, false); // Thread Timer
		Random teams = new Random();
		if (teams.nextBoolean()) {
			_start0 = parsePlaces(0);
			_start1 = parsePlaces(1);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 0);
			_st1 = 0;
			_st2 = 1;
		} else {
			_start0 = parsePlaces(1);
			_start1 = parsePlaces(0);
			_st1 = 1;
			_st2 = 0;
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 1);
		}
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init1.get_GUID() + "",
				init1.get_GUID() + "," + Constant.ETAT_PORTE + ",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, init1.get_GUID() + "",
				init1.get_GUID() + "," + Constant.ETAT_PORTEUR + ",0");

		List<Entry<Integer, Fighter>> e = new ArrayList<Entry<Integer, Fighter>>();
		e.addAll(_team1.entrySet());
		for (Entry<Integer, Fighter> entry : e) {
			Fighter f = entry.getValue();
			Case cell = getRandomCell(_start1);
			if (cell == null) {
				_team1.remove(f.getGUID());
				continue;
			}

			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.getGUID() + "",
					f.getGUID() + "," + Constant.ETAT_PORTE + ",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.getGUID() + "",
					f.getGUID() + "," + Constant.ETAT_PORTEUR + ",0");
			f.set_fightCell(cell);
			f.get_fightCell().addFighter(f);
			f.setTeam(1);
			f.fullPDV();
		}
		_init0.set_fightCell(getRandomCell(_start0));

		_init0.getPersonnage().get_curCell().removePlayer(_init0.getPersonnage().get_GUID());

		_init0.get_fightCell().addFighter(_init0);

		_init0.getPersonnage().set_fight(this);
		_init0.setTeam(0);

		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID());
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), group.getID());

		SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), 4, _init0.getGUID(),
				group.getID(), (_init0.getPersonnage().get_curCell().getID() + 1), "0;-1", group.getCellID(), "1;-1");
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID(),
				_init0);

		for (Fighter f : _team1.values()) {
			SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), group.getID(), f);
		}

		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, _map);

		set_state(Constant.FIGHT_STATE_PLACE);
	}

	public Fight(int id, Maps map, Characters perso, Collector perco) {
		set_guildID(perco.get_guildID());
		perco.set_inFight((byte) 1);
		perco.set_inFightID((byte) id);

		_type = Constant.FIGHT_TYPE_PVT; // (0: Défie) (4: Pvm) (1:PVP) 5:Perco
		_id = id;
		_map = map.getMapCopy();
		_mapOld = map;
		_init0 = new Fighter(this, perso);
		_perco = perco;
		// on desactive le timer de regen coté org.walaka.rubrumsolem.client
		SocketManager.GAME_SEND_ILF_PACKET(perso, 0);

		_team0.put(perso.get_GUID(), _init0);

		Fighter percoF = new Fighter(this, perco);
		_team1.put(-1, percoF);

		SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 1, 2, 0, 1, 0, 45000, _type); // timer de combat
		startTimer(46000, false); // Thread Timer
		Random teams = new Random();
		if (teams.nextBoolean()) {
			_start0 = parsePlaces(0);
			_start1 = parsePlaces(1);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 0);
			_st1 = 0;
			_st2 = 1;
		} else {
			_start0 = parsePlaces(1);
			_start1 = parsePlaces(0);
			_st1 = 1;
			_st2 = 0;
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 1);
		}
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID() + "",
				perso.get_GUID() + "," + Constant.ETAT_PORTE + ",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID() + "",
				perso.get_GUID() + "," + Constant.ETAT_PORTEUR + ",0");

		List<Entry<Integer, Fighter>> e = new ArrayList<Entry<Integer, Fighter>>();
		e.addAll(_team1.entrySet());
		for (Entry<Integer, Fighter> entry : e) {
			Fighter f = entry.getValue();
			Case cell = getRandomCell(_start1);
			if (cell == null) {
				_team1.remove(f.getGUID());
				continue;
			}

			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.getGUID() + "",
					f.getGUID() + "," + Constant.ETAT_PORTE + ",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, f.getGUID() + "",
					f.getGUID() + "," + Constant.ETAT_PORTEUR + ",0");
			f.set_fightCell(cell);
			f.get_fightCell().addFighter(f);
			f.setTeam(1);
			f.fullPDV();

		}
		_init0.set_fightCell(getRandomCell(_start0));

		_init0.getPersonnage().get_curCell().removePlayer(_init0.getPersonnage().get_GUID());

		_init0.get_fightCell().addFighter(_init0);

		_init0.getPersonnage().set_fight(this);
		_init0.setTeam(0);

		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID());
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), perco.getGuid());

		SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), 5, _init0.getGUID(),
				perco.getGuid(), (_init0.getPersonnage().get_curCell().getID() + 1), "0;-1", perco.get_cellID(),
				"3;-1");
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID(),
				_init0);

		for (Fighter f : _team1.values()) {
			SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), perco.getGuid(),
					f);
		}

		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, _map);
		set_state(Constant.FIGHT_STATE_PLACE);

		// On actualise la guilde + Message d'attaque
		for (Characters z : World.getGuild(_guildID).getMembers()) {
			if (z == null)
				continue;
			if (z.isOnline()) {
				SocketManager.GAME_SEND_gITM_PACKET(z, Collector.parsetoGuild(z.get_guild().get_id()));
				Collector.parseAttaque(z, _guildID);
				Collector.parseDefense(z, _guildID);
				String str = "";
				str += "A" + _perco.get_N1() + "," + _perco.get_N2() + "|";
				str += _perco.get_mapID() + "|";
				str += World.getCarte((short) _perco.get_mapID()).getX() + "|" + World.getCarte((short) _perco.get_mapID()).getY();
				SocketManager.GAME_SEND_gA_PACKET(z, str);
			}
		}
	}

	public Fight(int id, Maps map, ArrayList<Characters> team1, ArrayList<Characters> team2) {// TODO KOLI
		_type = 0;
		_id = id;
		_map = map.getMapCopy();
		_mapOld = map;
		_init0 = new Fighter(this, team1.get(0));
		_init1 = new Fighter(this, team2.get(0));
		_team0.put(team1.get(0).get_GUID(), _init0);
		_team1.put(team2.get(0).get_GUID(), _init1);
		SocketManager.GAME_SEND_ILF_PACKET(team1.get(0), 0);
		SocketManager.GAME_SEND_ILF_PACKET(team2.get(0), 0);
		SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this, 7, 2, 0, 1, 0, 45000L, -10);// (Skin Epee invisible)
		startTimer(46000, false); // Thread Timer
		_start0 = parsePlaces(0);
		_start1 = parsePlaces(1);
		SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 1, _map.get_placesStr(), 0);
		SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this, 2, _map.get_placesStr(), 1);
		_st1 = 0;
		_st2 = 1;
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, team1.get(0).get_GUID() + "",
				team1.get(0).get_GUID() + "," + Constant.ETAT_PORTE + ",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, team1.get(0).get_GUID() + "",
				team1.get(0).get_GUID() + "," + Constant.ETAT_PORTEUR + ",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, team2.get(0).get_GUID() + "",
				team2.get(0).get_GUID() + "," + Constant.ETAT_PORTE + ",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, team2.get(0).get_GUID() + "",
				team2.get(0).get_GUID() + "," + Constant.ETAT_PORTEUR + ",0");
		_init0.set_fightCell(getRandomCell(_start0));
		_init1.set_fightCell(getRandomCell(_start1));
		_init0.getPersonnage().get_curCell().removePlayer(_init0.getGUID());
		_init1.getPersonnage().get_curCell().removePlayer(_init1.getGUID());
		_init0.get_fightCell().addFighter(_init0);
		_init1.get_fightCell().addFighter(_init1);
		_init0.getPersonnage().set_fight(this);
		_init0.setTeam(0);
		_init1.getPersonnage().set_fight(this);
		_init1.setTeam(1);
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID());
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init1.getPersonnage().get_curCarte(), _init1.getGUID());
		SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), 0, _init0.getGUID(),
				_init1.getGUID(), _init0.getPersonnage().get_curCell().getID(), "0;-1",
				_init1.getPersonnage().get_curCell().getID(), "0;-1");
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID(),
				_init0);
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), _init1.getGUID(),
				_init1);

		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this, 7, _map);

		set_state(Constant.FIGHT_STATE_PLACE);
	}

	public Maps get_map() {
		return _map;
	}

	public boolean isFightStarted() {
		return FightStarted;
	}

	public void setFightStarted(boolean fightStarted) {
		FightStarted = fightStarted;
	}

	public List<Piege> get_traps() {
		return _traps;
	}

	public List<Glyphe> get_glyphs() {
		return _glyphs;
	}

	private Case getRandomCell(List<Case> cells) {
		Random rand = new Random();
		Case cell;
		if (cells.isEmpty())
			return null;
		int limit = 0;
		do {
			int id = rand.nextInt(cells.size() - 1);
			cell = cells.get(id);
			limit++;
		} while ((cell == null || !cell.getFighters().isEmpty()) && limit < 80);
		if (limit == 80) {

			Logs.addToGameLog("Case non trouve dans la liste");
			return null;
		}
		return cell;
	}

	private ArrayList<Case> parsePlaces(int num) {
		return CryptManager.parseStartCell(_map, num);
	}

	public int get_id() {
		return _id;
	}

	public ArrayList<Fighter> getFighters(int teams)// teams entre 0 et 7, binaire([spec][t2][t1]);
	{
		ArrayList<Fighter> fighters = new ArrayList<Fighter>();

		if (teams - 4 >= 0) {
			for (Entry<Integer, Characters> entry : _spec.entrySet()) {
				fighters.add(new Fighter(this, entry.getValue()));
			}
			teams -= 4;
		}
		if (teams - 2 >= 0) {
			for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
				fighters.add(entry.getValue());
			}
			teams -= 2;
		}
		if (teams - 1 >= 0) {
			for (Entry<Integer, Fighter> entry : _team0.entrySet()) {
				fighters.add(entry.getValue());
			}
		}
		return fighters;
	}

	public synchronized void changePlace(Characters perso, int cell) {
		Fighter fighter = getFighterByPerso(perso);
		int team = getTeamID(perso.get_GUID()) - 1;
		if (fighter == null)
			return;
		if (_map.getCase(cell) == null || _map.getCase(cell).isWalkable(true) == false)
			return;
		if (get_state() != 2 || isOccuped(cell) || perso.is_ready() || (team == 0 && !groupCellContains(_start0, cell))
				|| (team == 1 && !groupCellContains(_start1, cell)))
			return;

		fighter.get_fightCell().getFighters().clear();
		fighter.set_fightCell(_map.getCase(cell));

		_map.getCase(cell).addFighter(fighter);
		SocketManager.GAME_SEND_FIGHT_CHANGE_PLACE_PACKET_TO_FIGHT(this, 3, _map, perso.get_GUID(), cell);
	}

	public boolean isOccuped(int cell) {
		if (_map.getCase(cell) == null)
			return true;
		return _map.getCase(cell).getFighters().size() > 0;
	}

	private boolean groupCellContains(ArrayList<Case> cells, int cell) {
		for (int a = 0; a < cells.size(); a++) {
			if (cells.get(a).getID() == cell)
				return true;
		}
		return false;
	}

	public void verifIfAllReady() {
		boolean val = true;
		for (int a = 0; a < _team0.size(); a++) {
			if (!_team0.get(_team0.keySet().toArray()[a]).getPersonnage().is_ready())
				val = false;
		}
		if (_type != Constant.FIGHT_TYPE_PVM && _type != Constant.FIGHT_TYPE_PVT
				&& _type != Constant.FIGHT_TYPE_CONQUETE && protector == null) {
			for (int a = 0; a < _team1.size(); a++) {
				if (!_team1.get(_team1.keySet().toArray()[a]).getPersonnage().is_ready())
					val = false;
			}
		}
		if (_type == Constant.FIGHT_TYPE_PVT || _type == Constant.FIGHT_TYPE_CONQUETE)
			val = false;// Evite de lancer le combat trop vite
		if (val) {
			startFight();
		}
	}

	public boolean isCheckTimer() {
		return checkTimer;
	}

	public void setCheckTimer(boolean checkTimer) {
		this.checkTimer = checkTimer;
	}

	private void startFight() {
		if (_state >= Constant.FIGHT_STATE_ACTIVE)
			return;
		if (_type == Constant.FIGHT_TYPE_PVT) {
			_perco.set_inFight((byte) 2);
			// On actualise la guilde
			String packet = Collector.parsetoGuild(_guildID);
			for (Characters z : World.getGuild(_guildID).getMembers()) {
				if (z == null)
					continue;
				if (z.isOnline()) {
					SocketManager.GAME_SEND_gITM_PACKET(z, packet);
					Collector.parseAttaque(z, _guildID);
					Collector.parseDefense(z, _guildID);
				}
			}
		}

		_state = Constant.FIGHT_STATE_ACTIVE;
		// Pour le sort corruption
		for (Fighter f : getFighters(3)) {
			if (f == null || f.getPersonnage() == null)
				continue;
			f.setSpellStats();
			f.getPersonnage().sendLimitationIm();
			if (!f.getPersonnage().hasSpell(59))
				continue;
			f.addLaunchedFakeSort(null, f.getPersonnage().getSortStatBySortIfHas(59), 3);
		}

		set_startTime(System.currentTimeMillis());
		SocketManager.GAME_SEND_GAME_REMFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID());
		if (_type == Constant.FIGHT_TYPE_PVM) {
			if (_team1.size() > 0) {
				_team1.get(_team1.keySet().toArray()[0]).getMob().getTemplate().getAlign();
			}
			// Si groupe non fixe
		}
		if (_type == Constant.FIGHT_TYPE_CONQUETE) {
			_Prisme.setInFight(-2);
			for (Characters z : World.getOnlinePersos()) {
				if (z == null)
					continue;
				if (z.get_align() == _Prisme.getalignement()) {
					Prism.parseAttack(z);
					Prism.parseDefense(z);
				}
			}
		}
		setFightStarted(true);
		Logs.addToGameLog(">Un combat vient de débuter");
		SocketManager.GAME_SEND_GIC_PACKETS_TO_FIGHT(this, 7);
		SocketManager.GAME_SEND_GS_PACKET_TO_FIGHT(this, 7);
		InitOrdreJeu();
		_curPlayer = -1;
		SocketManager.GAME_SEND_GTL_PACKET_TO_FIGHT(this, 7);
		SocketManager.GAME_SEND_GTM_PACKET_TO_FIGHT(this, 7);
		if (getActTimerTask() != null)
			getActTimerTask().cancel();
		setActTimerTask(null);

		Logs.addToGameLog("Debut du combat");
		for (Fighter F : getFighters(3)) {
			Characters perso = F.getPersonnage();
			if (perso == null)
				continue;
			if (perso.isOnMount())
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID() + "",
						perso.get_GUID() + "," + Constant.ETAT_CHEVAUCHANT + ",1");

		}

		try {
			if (this._type == 4) {

				boolean hasMale = false, hasFemale = false;
				boolean hasCawotte = false, hasChafer = false, hasRoulette = false, hasArakne = false;
				boolean hasBoss = false, inDungeon = false;
				for (Fighter f : _team0.values()) {
					if (f.getPersonnage() != null) {
						Characters perso = f.getPersonnage();
						if (perso.hasSpell(367))
							hasCawotte = true;
						if (perso.hasSpell(373))
							hasChafer = true;
						if (perso.hasSpell(101))
							hasRoulette = true;
						if (perso.hasSpell(370))
							hasArakne = true;
						if (perso.get_sexe() == 0)
							hasMale = true;
						if (perso.get_sexe() == 1)
							hasFemale = true;
						if (perso.get_curCarte().hasEndFightAction(_type))
							inDungeon = true;
					}
				}
				// BR,tournesol affamé, Mob l'éponge, scara doré, bworker, blops
				// royaux, wa wab,
				// rat noir, rat blanc, spincter, skeunk, croca, toror, tot,
				// meulou, DC, CM, AA
				// Ougah, Krala
				String IDisBoss = ";147;799;928;1001;797;478;1184;1185;1186;1187;1188;180;939;940;943;780;854;121;827;232;113;257;173;1159;423;";
				for (Fighter f : _team1.values()) {
					if (IDisBoss.contains(";" + f.getMob().getTemplate().getID() + ";"))
						hasBoss = true;
				}

				boolean severalEnnemies, severalAllies, bothSexes, EvenEnnemies, MoreEnnemies;
				severalEnnemies = (_team1.size() < 2 ? false : true);
				severalAllies = (_team0.size() < 2 ? false : true);
				bothSexes = (!hasMale || !hasFemale ? false : true);
				EvenEnnemies = (_team1.size() % 2 == 0 ? true : false);
				MoreEnnemies = (_team1.size() < _team0.size() ? false : true);

				String challenges = World.getChallengeFromConditions(severalEnnemies, severalAllies, bothSexes,
						EvenEnnemies, MoreEnnemies, hasCawotte, hasChafer, hasRoulette, hasArakne, hasBoss);

				String[] chalInfo;
				int challengeID, challengeXP, challengeDP, bonusGroupe;
				int challengeNumber = (inDungeon ? 2 : 1);

				for (String chalInfos : World.getRandomChallenge(challengeNumber, challenges)) {
					chalInfo = chalInfos.split(",");
					challengeID = Integer.parseInt(chalInfo[0]);
					challengeXP = Integer.parseInt(chalInfo[1]);
					challengeDP = Integer.parseInt(chalInfo[2]);
					bonusGroupe = Integer.parseInt(chalInfo[3]);
					bonusGroupe *= this._team1.size();
					this._challenges.put(challengeID,
							new Challenge(this, challengeID, challengeXP + bonusGroupe, challengeDP + bonusGroupe));
				}

				for (Map.Entry<Integer, Challenge> c : this._challenges.entrySet()) {
					if (c.getValue() == null)
						continue;
					c.getValue().onFight_start();
					SocketManager.GAME_SEND_PACKET_TO_FIGHT(this, 7, c.getValue().parseToPacket());
				}

			}

		} catch (Exception localException) {
			localException.printStackTrace(System.out);
		}
		if (get_type() == Constant.FIGHT_TYPE_PVM) {
			if (get_team1().size() > 0)
				get_team1().get(get_team1().keySet().toArray()[0]).getMob().getTemplate().getAlign();
			if (!getMobGroup().isFix() && isCheckTimer())
				World.getCarte(get_map().get_id()).spawnAfterTimeGroup(-1, 1, true, -1);// Respawn
																						// d'un
																						// groupe
			if (getMobGroup().isFix() && isCheckTimer() && get_map().get_id() != 6826)
				World.getCarte(get_map().get_id()).spawnAfterTimeGroupFix(getMobGroup().getCellID());// Respawn
																										// d'un
																										// groupe
		}
		startTurn();

		for (Fighter F : getFighters(3)) {
			if (F == null)
				continue;
			_raulebaque.put(F.getGUID(), F.get_fightCell());
		}
	}

	private MobGroup getMobGroup() {
		return _mobGroup;
	}

	private void startTurn() {
		setHasUsedCoopTranspo(false);
		if (Thread.interrupted()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException ie) {
				boolean havePlayers = false;
				ArrayList<Fighter> fighters = this.getFighters(3);
				for (Fighter f : fighters) {
					if (f.isDouble() || f.getPersonnage() == null || f.hasLeft())
						continue;
					Characters p = f.getPersonnage();
					if (p == null || p.get_compte() == null || p.get_compte().getGameThread() == null
							|| p.get_compte().getGameThread().get_out() == null)
						continue;
					havePlayers = true;
					break;
				}
				if (!havePlayers) {
					Logs.addToDebug("Un combat vide trouvé en : " + _map.get_id());
					_state = Constant.FIGHT_STATE_FINISHED;
					// on vire les spec du combat
					for (Characters perso : _spec.values()) {
						// on remet le perso sur la map
						perso.get_curCarte().addPlayer(perso);
						// SocketManager.GAME_SEND_GV_PACKET(perso); //Mauvaise
						// ligne apparemment
						perso.refreshMapAfterFight();
					}

					World.getCarte(_map.get_id()).removeFight(_id);
					SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(World.getCarte(_map.get_id()));
					_map = null;
					_ordreJeu = null;
					_team0.clear();
					_team1.clear();
					return;
				}
			}
		}
		
		//if (!verifyStillInFight())
		verifIfTeamAllDead();

		if (_state >= Constant.FIGHT_STATE_FINISHED)
			return;

		try {
			Thread.sleep(500);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		_curPlayer++;
		_curAction = "";
		if (_ordreJeu == null)
			return;
		if (_curPlayer >= _ordreJeu.size())
			_curPlayer = 0;

		_curFighterPA = _ordreJeu.get(_curPlayer).getPA();
		_curFighterPM = _ordreJeu.get(_curPlayer).getPM();
		_curFighterUsedPA = 0;
		_curFighterUsedPM = 0;
		setTimeStartTurn(System.currentTimeMillis());
		Fighter curPlayer = _ordreJeu.get(_curPlayer);
		if (curPlayer.isDeconnected()) {
			curPlayer.newTurn();
			if (curPlayer.getToursRestants() <= 0) {
				if (curPlayer.getPersonnage() != null) {
					leftFight(curPlayer.getPersonnage(), null, false);
					curPlayer.getPersonnage().DeconnexionCombat();
				} else {
					onFighterDie(curPlayer, curPlayer);
					curPlayer.setLeft(true);
				}
			} else {
				// On envois les IM il reste X tours
				SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 7,
						new StringBuilder("0162;").append(curPlayer.getPacketsName()).append("~")
								.append(curPlayer.getToursRestants()).toString());
				endTurn(true);
				return;
			}
		}
		if (_ordreJeu.get(_curPlayer).hasLeft() || _ordreJeu.get(_curPlayer).isDead())// Si joueur mort
		{
			Logs.addToGameLog("(" + _curPlayer + ") Fighter ID=  " + _ordreJeu.get(_curPlayer).getGUID() + " est mort");
			endTurn(false);
			return;
		}
		Fighter curFighter = _ordreJeu.get(_curPlayer);

		curFighter.applyBeginningTurnBuff(this);
		if (_state == Constant.FIGHT_STATE_FINISHED)
			return;
		if (curFighter.getPDV() <= 0 && !curFighter.isDead()) {
			onFighterDie(_ordreJeu.get(_curPlayer), _ordreJeu.get(_curPlayer));
		}
		// On actualise les sorts launch
		curFighter.ActualiseLaunchedSort();
		// reset des Max des Chatis
		curFighter.get_chatiValue().clear();
		// Gestion des glyphes
		ArrayList<Glyphe> glyphs = new ArrayList<Glyphe>();// Copie du tableau
		glyphs.addAll(_glyphs);

		for (Glyphe g : glyphs) {
			if (_state >= Constant.FIGHT_STATE_FINISHED)
				return;
			if (curFighter.isDead())
				break;
			// Si c'est ce joueur qui l'a lancé
			if (g.get_caster().getGUID() == curFighter.getGUID()) {
				// on réduit la durée restante, et si 0, on supprime
				if (g.decrementDuration() == 0) {
					_glyphs.remove(g);
					g.desapear();
					continue;// Continue pour pas que le joueur active le glyphe
								// s'il était dessus
				}
			}
			// Si dans le glyphe
			int dist = Pathfinding.getDistanceBetween(_map, curFighter.get_fightCell().getID(), g.get_cell().getID());
			if (dist <= g.get_size() && g._spell != 476)// 476 a effet en fin de
														// tour
			{
				// Alors le joueur est dans le glyphe
				g.onTraped(curFighter);
			}
		}
		if (_ordreJeu == null)
			return;
		if (_ordreJeu.size() < _curPlayer) {
			_curPlayer = 0;
		}
		if (_ordreJeu.get(_curPlayer) != curFighter || curFighter.isDead())// Si joueur mort
		{
			Logs.addToGameLog("(" + _curPlayer + ") Fighter ID=  " + _ordreJeu.get(_curPlayer).getGUID() + " est mort");
			endTurn(false);
			return;
		}
		if (_ordreJeu.get(_curPlayer).getPersonnage() != null) {
			SocketManager.GAME_SEND_STATS_PACKET(_ordreJeu.get(_curPlayer).getPersonnage());
		}
		if (_ordreJeu.get(_curPlayer).hasBuff(Constant.EFFECT_PASS_TURN))// Si
																			// il
																			// doit
																			// passer
																			// son
																			// tour
		{

			Logs.addToGameLog(
					"(" + _curPlayer + ") Fighter ID= " + _ordreJeu.get(_curPlayer).getGUID() + " passe son tour");
			endTurn(false);
			return;
		}
		Logs.addToGameLog("(" + _curPlayer + ")Debut du tour de Fighter ID= " + _ordreJeu.get(_curPlayer).getGUID());
		SocketManager.GAME_SEND_GAMETURNSTART_PACKET_TO_FIGHT(this, 7, _ordreJeu.get(_curPlayer).getGUID(),
				Constant.TIME_BY_TURN);
		startTimer(46000, true);

		if (_ordreJeu == null)
			return;
		_ordreJeu.get(_curPlayer).setCanPlay(true);

		if (_ordreJeu.get(_curPlayer).getPersonnage() == null || _ordreJeu.get(_curPlayer)._double != null
				|| _ordreJeu.get(_curPlayer)._Perco != null
				|| (((Fighter) this._ordreJeu.get(this._curPlayer))._Prisme != null))// Si
																						// ce
																						// n'est
																						// pas
																						// un
																						// joueur
		{
			_IAThreads = new IA.IAThread(_ordreJeu.get(_curPlayer), this);

		}
		try {
			if ((this._type == 4) && (this._challenges.size() > 0)
					&& !this._ordreJeu.get(this._curPlayer).isInvocation()
					&& !this._ordreJeu.get(this._curPlayer).isDouble()
					&& !this._ordreJeu.get(this._curPlayer).isPerco()) {
				for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
					if (c.getValue() == null)
						continue;
					c.getValue().onPlayer_startTurn(this._ordreJeu.get(this._curPlayer));
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}

	public synchronized void endTurn(boolean isAuto) {
		if (_IAThreads != null) {
			try {
				if (_IAThreads.getThread() != null && _IAThreads.getThread().isAlive()) {
					_IAThreads.getThread().interrupt();

					try {
						Thread.sleep(10);
					} catch (InterruptedException e1) {
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			boolean bug = false;
			if (_state >= Constant.FIGHT_STATE_FINISHED)
				return;
			if (_curPlayer == -1) {
				bug = true;
				_curPlayer = _ordreJeu.size() - 1;
			}
			if (_curPlayer >= _ordreJeu.size()) {
				_curPlayer = _ordreJeu.size() - 1;
			}
			if (_ordreJeu.get(_curPlayer) == null) {
				boolean noplayer = true;
				List<Fighter> n_list = new ArrayList<Fighter>();
				for (Fighter f : _ordreJeu) {
					if (f == null)
						continue;
					if (f.getPersonnage() != null && !f.isDouble())
						noplayer = false;
					n_list.add(f);
				}
				if (noplayer)
					return;
				_ordreJeu = n_list;
				bug = true;
				_curPlayer = _ordreJeu.size() - 1;
				startTurn();
				return;
			}
			if (bug) {
				endTurn(isAuto);
				return;
			}
			if (_ordreJeu == null)
				return;// Je veux bien être magicien et réparer les combats,
						// mais faut pas abuser non plus >.>
			if (_ordreJeu.get(_curPlayer).hasLeft() || _ordreJeu.get(_curPlayer).isDead()) {
				startTurn();
				return;
			}
			if (getActTimerTask() != null)
				getActTimerTask().cancel();
			setActTimerTask(null);
			if (_ordreJeu == null)
				return;

			if (!isAuto && !_curAction.equals("") && _ordreJeu.get(_curPlayer).getPersonnage() != null) {// On
																											// enlève
																											// la
																											// boucle
																											// si
																											// c'est
																											// un
																											// action
																											// effectuée
																											// à
																											// la
																											// dernière
																											// seconde
																											// -
																											// Skryn/Return
				while (!_curAction.isEmpty()) {
				}
			}
			SocketManager.GAME_SEND_GAMETURNSTOP_PACKET_TO_FIGHT(this, 7, _ordreJeu.get(_curPlayer).getGUID());

			_ordreJeu.get(_curPlayer).setCanPlay(false);
			_curAction = "";
			if (isAuto) { // Puis on laisse le temps de terminer l'action si on
							// a une lancée à la dernière seconde |Skryn/Return
				try {
					Thread.sleep(2100);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// Si empoisonné (Créer une fonction applyEndTurnbuff si d'autres
			// effets existent)
			for (SpellEffect SE : _ordreJeu.get(_curPlayer).getBuffsByEffectID(131)) {
				int pas = SE.getValue();
				int val = -1;
				try {
					val = Integer.parseInt(SE.getArgs().split(";")[1]);
				} catch (Exception e) {
				}
				;
				if (val == -1)
					continue;

				int nbr = (int) Math.floor((double) _curFighterUsedPA / (double) pas);
				int dgt = val * nbr;
				// Si poison paralysant
				if (SE.getSpell() == 200) {
					int inte = SE.getCaster().getTotalStats().getEffect(Constant.STATS_ADD_INTE);
					if (inte < 0)
						inte = 0;
					int pdom = SE.getCaster().getTotalStats().getEffect(Constant.STATS_ADD_PERDOM);
					if (pdom < 0)
						pdom = 0;
					// on applique le boost
					dgt = (int) (((100 + inte + pdom) / 100) * dgt);
				}
				if (_ordreJeu.get(_curPlayer).hasBuff(184)) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 105, _ordreJeu.get(_curPlayer).getGUID() + "",
							_ordreJeu.get(_curPlayer).getGUID() + ","
									+ _ordreJeu.get(_curPlayer).getBuff(184).getValue());
					dgt = dgt - _ordreJeu.get(_curPlayer).getBuff(184).getValue();// Réduction
																					// physique
				}
				if (_ordreJeu.get(_curPlayer).hasBuff(105)) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 105, _ordreJeu.get(_curPlayer).getGUID() + "",
							_ordreJeu.get(_curPlayer).getGUID() + ","
									+ _ordreJeu.get(_curPlayer).getBuff(105).getValue());
					dgt = dgt - _ordreJeu.get(_curPlayer).getBuff(105).getValue();// Immu
				}
				if (dgt <= 0)
					continue;

				if (dgt > _ordreJeu.get(_curPlayer).getPDV())
					dgt = _ordreJeu.get(_curPlayer).getPDV();// va mourrir
				_ordreJeu.get(_curPlayer).removePDV(dgt);
				dgt = -(dgt);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 100, SE.getCaster().getGUID() + "",
						_ordreJeu.get(_curPlayer).getGUID() + "," + dgt);

			}
			ArrayList<Glyphe> glyphs = new ArrayList<Glyphe>();// Copie du
																// tableau
			glyphs.addAll(_glyphs);
			for (Glyphe g : glyphs) {
				if (_state >= Constant.FIGHT_STATE_FINISHED)
					return;
				// Si dans le glyphe
				int dist = Pathfinding.getDistanceBetween(_map, _ordreJeu.get(_curPlayer).get_fightCell().getID(),
						g.get_cell().getID());
				if (dist <= g.get_size() && g._spell == 476)// 476 a effet en
															// fin de tour
				{
					// Alors le joueur est dans le glyphe
					g.onTraped(_ordreJeu.get(_curPlayer));
				}
			}
			if (_ordreJeu.get(_curPlayer).getPDV() <= 0)
				onFighterDie(_ordreJeu.get(_curPlayer), _ordreJeu.get(_curPlayer));

			if ((this._type == 4) && (this._challenges.size() > 0)
					&& !this._ordreJeu.get(this._curPlayer).isInvocation()
					&& !this._ordreJeu.get(this._curPlayer).isDouble() && !this._ordreJeu.get(this._curPlayer).isPerco()
					&& (this._ordreJeu.get(this._curPlayer).getTeam() == 0)) {
				for (Map.Entry<Integer, Challenge> c : this._challenges.entrySet()) {
					if (c.getValue() == null)
						continue;
					c.getValue().onPlayer_endTurn(this._ordreJeu.get(this._curPlayer));
				}
			}
			// reset des valeurs
			_curFighterUsedPA = 0;
			_curFighterUsedPM = 0;
			_curFighterPA = _ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constant.STATS_ADD_PA);
			_curFighterPM = _ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constant.STATS_ADD_PM);
			_ordreJeu.get(_curPlayer).refreshfightBuff();
			if (_ordreJeu.get(_curPlayer).getPersonnage() != null)
				if (_ordreJeu.get(_curPlayer).getPersonnage().isOnline())
					SocketManager.GAME_SEND_STATS_PACKET(_ordreJeu.get(_curPlayer).getPersonnage());

			if (_ordreJeu == null)
				return;
			if (_curPlayer >= _ordreJeu.size())
				_curPlayer = 0;
			SocketManager.GAME_SEND_GTM_PACKET_TO_FIGHT(this, 7);
			SocketManager.GAME_SEND_GTR_PACKET_TO_FIGHT(this, 7,
					_ordreJeu.get(_curPlayer == _ordreJeu.size() ? 0 : _curPlayer).getGUID());

			Logs.addToGameLog("(" + _curPlayer + ")Fin du tour de Fighter ID= " + _ordreJeu.get(_curPlayer).getGUID());
			startTurn();
		} catch (NullPointerException e) {
			e.printStackTrace();
			endTurn(isAuto);
		}
	}

	private void InitOrdreJeu() {
		Fighter curMax = null;
		boolean team1_ready = false;
		boolean team2_ready = false;
		ArrayList<Fighter> fightTeam1 = new ArrayList<Fighter>();
		ArrayList<Fighter> fightTeam2 = new ArrayList<Fighter>();
		int size = 0;
		int y1 = 0;
		int y2 = 0;
		boolean maxTeam1 = false;
		boolean maxTeam2 = false;
		int aleatoire = 0;

		if (!team1_ready) {
			team1_ready = true;
			for (Entry<Integer, Fighter> entry : _team0.entrySet()) {
				if (_ordreJeu.contains(entry.getValue()))
					continue;
				team1_ready = false;

				fightTeam1.add(0, entry.getValue());
			}
		}

		if (!team2_ready) {
			team2_ready = true;
			for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
				if (_ordreJeu.contains(entry.getValue()))
					continue;
				team2_ready = false;

				fightTeam2.add(0, entry.getValue());
			}
		}
		if (fightTeam2.get(fightTeam2.size() - 1).getInitiative() == fightTeam1.get(fightTeam1.size() - 1)
				.getInitiative()) {
			aleatoire = Formulas.getRandomValue(1, 2);
		}
		if (fightTeam2.get(fightTeam2.size() - 1).getInitiative() > fightTeam1.get(fightTeam1.size() - 1)
				.getInitiative() || aleatoire == 2) {
			ArrayList<Fighter> inverseArray = fightTeam1;
			fightTeam1 = fightTeam2;
			fightTeam2 = inverseArray;
		}

		y1 = fightTeam1.size() - 1;
		y2 = fightTeam2.size() - 1;

		if (fightTeam1.size() >= fightTeam2.size())
			size = fightTeam1.size();
		else
			size = fightTeam2.size();

		ArrayList<Fighter> sortByIni = new ArrayList<Fighter>();
		for (int i = 0; i < y1 + 1; i++) {
			int maxIni = 100000000;
			Fighter curFight = null;
			int indexRemove = 0;

			for (int y = 0; y < fightTeam1.size(); y++) {
				if (fightTeam1.get(y).getInitiative() <= maxIni) {
					maxIni = fightTeam1.get(y).getInitiative();
					curFight = fightTeam1.get(y);
					indexRemove = y;
				}
			}

			sortByIni.add(curFight);
			fightTeam1.remove(indexRemove);
		}

		fightTeam1.clear();
		fightTeam1.addAll(sortByIni);
		sortByIni.clear();

		for (int i = 0; i < y2 + 1; i++) {
			int maxIni = 100000000;
			Fighter curFight = null;
			int indexRemove = 0;

			for (int y = 0; y < fightTeam2.size(); y++) {
				if (fightTeam2.get(y).getInitiative() <= maxIni) {
					maxIni = fightTeam2.get(y).getInitiative();
					curFight = fightTeam2.get(y);
					indexRemove = y;
				}
			}

			sortByIni.add(curFight);
			fightTeam2.remove(indexRemove);
		}

		fightTeam2.clear();
		fightTeam2.addAll(sortByIni);

		for (int i = 0; i < size; i++) {
			if (!maxTeam1) {
				curMax = fightTeam1.get(y1);
				if (i == fightTeam1.size() - 1) {
					maxTeam1 = true;
				}
				if (curMax != null)
					_ordreJeu.add(curMax);
				curMax = null;
			}

			if (!maxTeam2) {
				curMax = fightTeam2.get(y2);

				if (i == fightTeam2.size() - 1) {
					maxTeam2 = true;
				}

				if (curMax != null)
					_ordreJeu.add(curMax);
				curMax = null;
			}

			y1--;
			y2--;
		}
	}

	public void joinFight(Characters perso, int guid) {
		if (perso.get_curCarte().getSubArea().get_subscribe() && perso.get_compte().get_subscriber() == 0
				&& Config.USE_SUBSCRIBE) {
			SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(perso, "+5");
			return;
		}
		if (_state == Constant.FIGHT_STATE_ACTIVE || _state == Constant.FIGHT_STATE_FINISHED) {
			perso.get_compte().getGameThread().kick();
			return;
		}
		long timeRestant = Config.CONFIG_MS_FOR_START_FIGHT - (System.currentTimeMillis() - getTimeStartTurn());
		Fighter current_Join = null;
		if (_team0.containsKey(guid)) {
			Case cell = getRandomCell(_start0);
			if (cell == null)
				return;

			if (onlyGroup0) {
				Group g = _init0.getPersonnage().getGroup();
				if (g != null) {
					if (!g.getPersos().contains(perso)) {
						SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(), 'f',
								guid);
						return;
					}
				}
			}
			if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CONQUETE) {
				if (perso.get_align() == Constant.ALIGNEMENT_NEUTRE) {
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(), 'f', guid);
					return;
				}
				if (_init0.getPersonnage().get_align() != perso.get_align()) {
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(), 'f', guid);
					return;
				}
			}
			if (_guildID > -1 && perso.get_guild() != null) {
				if (get_guildID() == perso.get_guild().get_id()) {
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(), 'f', guid);
					return;
				}
			}
			if (locked0) {
				SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(), 'f', guid);
				return;
			}
			if (_type == Constant.FIGHT_TYPE_CHALLENGE) {
				if (perso.getArena() != -1 || perso.getKolizeum() != -1)
					SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, timeRestant, _type);
				else
					SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 1, 1, 0, timeRestant, _type);
			} else {
				SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, timeRestant, _type);
			}
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.get_compte().getGameThread().get_out(),
					_map.get_placesStr(), _st1);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID() + "",
					perso.get_GUID() + "," + Constant.ETAT_PORTE + ",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID() + "",
					perso.get_GUID() + "," + Constant.ETAT_PORTEUR + ",0");
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.get_curCarte(), perso.get_GUID());

			Fighter f = new Fighter(this, perso);
			current_Join = f;
			f.setTeam(0);
			_team0.put(perso.get_GUID(), f);
			perso.set_fight(this);
			f.set_fightCell(cell);
			f.get_fightCell().addFighter(f);
			// Désactive le timer de regen
			SocketManager.GAME_SEND_ILF_PACKET(perso, 0);
		} else if (_team1.containsKey(guid)) {
			Case cell = getRandomCell(_start1);
			if (cell == null)
				return;

			if (onlyGroup1) {
				Group g = _init1.getPersonnage().getGroup();
				if (g != null) {
					if (!g.getPersos().contains(perso)) {
						SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(), 'f',
								guid);
						return;
					}
				}
			}
			if (_type == Constant.FIGHT_TYPE_AGRESSION) {
				if (perso.get_align() == Constant.ALIGNEMENT_NEUTRE) {
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(), 'f', guid);
					return;
				}
				if (_init1.getPersonnage().get_align() != perso.get_align()) {
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(), 'f', guid);
					return;
				}
			}
			if (_type == Constant.FIGHT_TYPE_CONQUETE) {
				if (perso.get_align() == Constant.ALIGNEMENT_NEUTRE) {
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(), 'a', guid);
					return;
				}
				if (_init1.getPrisme().getalignement() != perso.get_align()) {
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(), 'a', guid);
					return;
				}
				perso.toggleWings('+');
			}
			if (_guildID > -1 && perso.get_guild() != null) {
				if (get_guildID() == perso.get_guild().get_id()) {
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(), 'f', guid);
					return;
				}
			}
			if (locked1) {
				SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso.get_compte().getGameThread().get_out(), 'f', guid);
				return;
			}
			if (_type == Constant.FIGHT_TYPE_CHALLENGE) {
				if (perso.getArena() != -1 || perso.getKolizeum() != -1)
					SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, 0, _type);
				else
					SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 1, 1, 0, 0, _type);
			} else {
				SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, 0, _type);
			}
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.get_compte().getGameThread().get_out(),
					_map.get_placesStr(), _st2);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID() + "",
					perso.get_GUID() + "," + Constant.ETAT_PORTE + ",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID() + "",
					perso.get_GUID() + "," + Constant.ETAT_PORTEUR + ",0");
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.get_curCarte(), perso.get_GUID());
			Fighter f = new Fighter(this, perso);
			current_Join = f;
			f.setTeam(1);
			_team1.put(perso.get_GUID(), f);
			perso.set_fight(this);
			f.set_fightCell(cell);
			f.get_fightCell().addFighter(f);
		}
		perso.get_curCell().removePlayer(perso.get_GUID());
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(perso.get_curCarte(),
				(current_Join.getTeam() == 0 ? _init0 : _init1).getGUID(), current_Join);
		SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(this, 7, current_Join);
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this, _map, perso);
		if (_perco != null) {
			for (Characters z : World.getGuild(_guildID).getMembers()) {
				if (z.isOnline()) {
					Collector.parseAttaque(z, _guildID);
					Collector.parseDefense(z, _guildID);
				}
			}
		}
		if (this._Prisme != null) {
			for (Characters z : World.getOnlinePersos()) {
				if (z == null)
					continue;
				if (z.get_align() != _Prisme.getalignement())
					continue;
				Prism.parseAttack(perso);
				//Prism.parseDefense(perso);
			}
		}
	}

	public void joinPrismeFight(Characters perso, int id, int PrismeID) {
			if(perso.get_curCarte().getSubArea().get_subscribe() && perso.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
			{
				SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(perso, "+5");
				return;
			}
			try {
				Thread.sleep(700);
			} catch (InterruptedException e) {}
			Fighter current_Join = null;
			Case cell = getRandomCell(_start1);
			if (cell == null)
				return;
			SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, 0, _type);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.get_compte().getGameThread().get_out(), _map.get_placesStr(), _st2);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID() + "", perso.get_GUID() + ","
					+ Constant.ETAT_PORTE + ",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID() + "", perso.get_GUID() + ","
					+ Constant.ETAT_PORTEUR + ",0");
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.get_curCarte(), perso.get_GUID());
			Fighter f = new Fighter(this, perso);
			current_Join = f;
			f.setTeam(1);
			_team1.put(perso.get_GUID(), f);
			perso.set_fight(this);
			f.set_fightCell(cell);
			f.get_fightCell().addFighter(f);
			SocketManager.GAME_SEND_ILF_PACKET(perso, 0);
			perso.get_curCell().removePlayer(perso.get_GUID());
			SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(perso.get_curCarte(), PrismeID, current_Join);
			SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(this, 7, current_Join);
			SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this, _map, perso);
		}

	public void joinPercepteurFight(Characters perso, int guid, int percoID) {
		if (perso.get_curCarte().getSubArea().get_subscribe() && perso.get_compte().get_subscriber() == 0
				&& Config.USE_SUBSCRIBE) {
			SocketManager.GAME_SEND_SUBSCRIBE_MESSAGE(perso, "+5");
			return;
		}
		try {
			Thread.sleep(700);
		} catch (InterruptedException e) {
		}
		;
		Fighter current_Join = null;
		Case cell = getRandomCell(_start1);
		if (cell == null)
			return;
		SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, 0, _type);
		SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.get_compte().getGameThread().get_out(), _map.get_placesStr(), _st2);
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID() + "", perso.get_GUID() + "," + Constant.ETAT_PORTE + ",0");
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID() + "", perso.get_GUID() + "," + Constant.ETAT_PORTEUR + ",0");
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.get_curCarte(), perso.get_GUID());
		Fighter f = new Fighter(this, perso);
		current_Join = f;
		f.setTeam(1);
		get_team1().put(perso.get_GUID(), f);
		perso.set_fight(this);
		f.set_fightCell(cell);
		f.get_fightCell().addFighter(f);
		SocketManager.GAME_SEND_ILF_PACKET(perso, 0);

		perso.get_curCell().removePlayer(perso.get_GUID());
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(perso.get_curCarte(), percoID, current_Join);
		SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(this, 7, current_Join);
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this, _map, perso);
	}

	public void toggleLockTeam(int guid) {
		if (_init0 != null && _init0.getGUID() == guid) {
			locked0 = !locked0;

			Logs.addToGameLog(locked0 ? "L'equipe 1 devient bloquee" : "L'equipe 1 n'est plus bloquee");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),
					locked0 ? '+' : '-', 'A', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 1, locked0 ? "095" : "096");
		} else if (_init1 != null && _init1.getGUID() == guid) {
			locked1 = !locked1;

			Logs.addToGameLog(locked1 ? "L'equipe 2 devient bloquee" : "L'equipe 2 n'est plus bloquee");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init1.getPersonnage().get_curCarte(),
					locked1 ? '+' : '-', 'A', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 2, locked1 ? "095" : "096");
		}
	}

	public void toggleOnlyGroup(int guid) {
		if (_init0 != null && _init0.getGUID() == guid) {
			onlyGroup0 = !onlyGroup0;

			Logs.addToGameLog(
					locked0 ? "L'equipe 1 n'accepte que les membres du groupe" : "L'equipe 1 n'est plus bloquee");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),
					onlyGroup0 ? '+' : '-', 'P', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 1, onlyGroup0 ? "093" : "094");
		} else if (_init1 != null && _init1.getGUID() == guid) {
			onlyGroup1 = !onlyGroup1;

			Logs.addToGameLog(
					locked1 ? "L'equipe 2 n'accepte que les membres du groupe" : "L'equipe 2 n'est plus bloquee");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init1.getPersonnage().get_curCarte(),
					onlyGroup1 ? '+' : '-', 'P', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 2, onlyGroup1 ? "095" : "096");
		}
	}

	public void toggleLockSpec(int guid) {
		if ((_init0 != null && _init0.getGUID() == guid) || (_init1 != null && _init1.getGUID() == guid)) {
			specOk = !specOk;
			if (!specOk) {
				for (Characters p : _spec.values()) {
					if (p == null)
						continue;
					SocketManager.GAME_SEND_GV_PACKET(p);
					p.setSitted(false);
					p.set_fight(null);
					p.set_away(false);
				}
				_spec.clear();
			}

			Logs.addToGameLog(
					specOk ? "Le combat accepte les spectateurs" : "Le combat n'accepte plus les spectateurs");
			if (_init0 != null && _init0.getPersonnage() != null)
				SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),
						specOk ? '+' : '-', 'S', _init0.getGUID());
			if (_init1 != null && _init1.getPersonnage() != null)
				SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),
						specOk ? '+' : '-', 'S', _init1.getGUID());
			SocketManager.GAME_SEND_Im_PACKET_TO_MAP(_map, specOk ? "039" : "040");
		}
	}

	public void toggleHelp(int guid) {
		if (_init0 != null && _init0.getGUID() == guid) {
			help1 = !help1;

			Logs.addToGameLog(help2 ? "L'equipe 1 demande de l'aide" : "L'equipe 1s ne demande plus d'aide");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),
					locked0 ? '+' : '-', 'H', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 1, help1 ? "0103" : "0104");
		} else if (_init1 != null && _init1.getGUID() == guid) {
			help2 = !help2;

			Logs.addToGameLog(help2 ? "L'equipe 2 demande de l'aide" : "L'equipe 2 ne demande plus d'aide");
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init1.getPersonnage().get_curCarte(),
					locked1 ? '+' : '-', 'H', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 2, help2 ? "0103" : "0104");
		}
	}

	private void set_state(int _state) {
		this._state = _state;
	}

	private void set_guildID(int guildID) {
		this._guildID = guildID;
	}

	public int get_state() {
		return _state;
	}

	public int get_guildID() {
		return _guildID;
	}

	public int get_type() {
		return _type;
	}

	public List<Fighter> get_ordreJeu() {
		return _ordreJeu;
	}

	public Map<Integer, Case> get_raulebaque() {
		return _raulebaque;
	}

	public Map<Integer, Challenge> get_challenges() {
		return this._challenges;
	}

	public synchronized boolean fighterDeplace(Fighter f, GameAction GA) {
		String path = GA._args;
		if (path.equals("")) {

			Logs.addToGameLog("Echec du deplacement: chemin vide");
			return false;
		}
		if (_ordreJeu.size() <= _curPlayer)
			return false;
		if (_ordreJeu.get(_curPlayer) == null)
			return false;

		Logs.addToGameLog("(" + _curPlayer + ")Tentative de deplacement de Fighter ID= " + f.getGUID()
				+ " a partir de la case " + f.get_fightCell().getID());

		Logs.addToGameLog("Path: " + path);
		if (!_curAction.equals("") || _ordreJeu.get(_curPlayer).getGUID() != f.getGUID()
				|| _state != Constant.FIGHT_STATE_ACTIVE) {
			if (!_curAction.equals(""))

				Logs.addToGameLog("Echec du deplacement: il y deja une action en cours");
			if (_ordreJeu.get(_curPlayer).getGUID() != f.getGUID())

				Logs.addToGameLog("Echec du deplacement: ce n'est pas a ce joueur de jouer");
			if (_state != Constant.FIGHT_STATE_ACTIVE)

				Logs.addToGameLog("Echec du deplacement: le combat n'est pas en cours");
			return false;
		}

		ArrayList<Fighter> tmptacle = Pathfinding.getEnemyFighterArround(f.get_fightCell().getID(), _map, this);
		ArrayList<Fighter> tacle = new ArrayList<Fighter>();
		if (tmptacle != null && !f.isState(6) && !f.isHide())// Tentative de
																// Tacle : Si
																// stabilisation
																// alors pas de
																// tacle
																// possible
		{
			boolean mustTacle = false;
			for (Fighter T : tmptacle)// Les stabilisés ne taclent pas
			{
				if (T.isHide())
					continue;
				tacle.add(T);
				if (T.isState(6)) {
					mustTacle = true;
				}
			}
			if (!tacle.isEmpty())// Si tous les tacleur ne sont pas stabilisés
			{

				Logs.addToGameLog("Le personnage est a cote de (" + tacle.size() + ") ennemi(s)");// ("+tacle.getPacketsName()+","+tacle.get_fightCell().getID()+")
																									// =>
																									// Tentative
																									// de
																									// tacle:");
				int chance = Formulas.getTacleChance(f, tacle);
				int rand = Formulas.getRandomValue(0, 99);
				if (rand > chance || mustTacle) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA._id, "104",
							_ordreJeu.get(_curPlayer).getGUID() + ";", "");// Joueur
																			// taclé
					int pertePA = _curFighterPA * chance / 100;

					if (pertePA < 0)
						pertePA = -pertePA;
					if (_curFighterPM < 0)
						_curFighterPM = 0; // -_curFighterPM :: 0 c'est plus
											// simple :)
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA._id, "129", f.getGUID() + "",
							f.getGUID() + ",-" + _curFighterPM);
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA._id, "102", f.getGUID() + "",
							f.getGUID() + ",-" + pertePA);

					_curFighterPM = 0;
					_curFighterPA -= pertePA;

					Logs.addToGameLog("Echec du deplacement: fighter tacle");
					return false;
				}
			}
		}

		// *
		AtomicReference<String> pathRef = new AtomicReference<String>(path);
		int nStep = Pathfinding.isValidPath(_map, f.get_fightCell().getID(), pathRef, this);
		String newPath = pathRef.get();
		if (nStep > _curFighterPM || nStep == -1000) {

			Logs.addToGameLog("(" + _curPlayer + ") Fighter ID= " + _ordreJeu.get(_curPlayer).getGUID()
					+ " a demander un chemin inaccessible ou trop loin");
			if (f.getPersonnage() != null && f.getPersonnage().get_compte() != null
					&& f.getPersonnage().get_compte().getGameThread() != null) {
				SocketManager.GAME_SEND_GA_PACKET(f.getPersonnage().get_compte().getGameThread().get_out(), "" + 151,
						"" + f.getGUID(), "-1", "");
			}
			return false;
		}

		_curFighterPM -= nStep;
		_curFighterUsedPM += nStep;

		int nextCellID = CryptManager.cellCode_To_ID(newPath.substring(newPath.length() - 2));
		// les monstres n'ont pas de GAS//GAF
		if (_ordreJeu.get(_curPlayer).getPersonnage() != null)
			SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this, 7, _ordreJeu.get(_curPlayer).getGUID());
		// Si le joueur n'est pas invisible
		if (!_ordreJeu.get(_curPlayer).isHide())
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA._id, "1", _ordreJeu.get(_curPlayer).getGUID() + "",
					"a" + CryptManager.cellID_To_Code(f.get_fightCell().getID()) + newPath);
		else// Si le joueur est planqué x)
		{
			if (_ordreJeu.get(_curPlayer).getPersonnage() != null) {
				// On envoie le path qu'au joueur qui se déplace
				GameSendThread out = _ordreJeu.get(_curPlayer).getPersonnage().get_compte().getGameThread().get_out();
				SocketManager.GAME_SEND_GA_PACKET(out, GA._id + "", "1", _ordreJeu.get(_curPlayer).getGUID() + "",
						"a" + CryptManager.cellID_To_Code(f.get_fightCell().getID()) + newPath);
			}
		}

		// Si porté
		Fighter po = _ordreJeu.get(_curPlayer).get_holdedBy();
		if (po != null && _ordreJeu.get(_curPlayer).isState(Constant.ETAT_PORTE) && po.isState(Constant.ETAT_PORTEUR)) {
			// si le joueur va bouger
			if (nextCellID != po.get_fightCell().getID()) {
				// on retire les états
				po.setState(Constant.ETAT_PORTEUR, 0);
				_ordreJeu.get(_curPlayer).setState(Constant.ETAT_PORTE, 0);
				// on retire dé lie les 2 fighters
				po.set_isHolding(null);
				_ordreJeu.get(_curPlayer).set_holdedBy(null);
				// La nouvelle case sera définie plus tard dans le code
				// On envoie les packets
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 950, po.getGUID() + "",
						po.getGUID() + "," + Constant.ETAT_PORTEUR + ",0");
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 950, _ordreJeu.get(_curPlayer).getGUID() + "",
						_ordreJeu.get(_curPlayer).getGUID() + "," + Constant.ETAT_PORTE + ",0");
			}
		}

		_ordreJeu.get(_curPlayer).get_fightCell().getFighters().clear();

		Logs.addToGameLog("(" + _curPlayer + ") Fighter ID= " + f.getGUID() + " se deplace de la case "
				+ _ordreJeu.get(_curPlayer).get_fightCell().getID() + " vers "
				+ CryptManager.cellCode_To_ID(newPath.substring(newPath.length() - 2)));
		_ordreJeu.get(_curPlayer).set_fightCell(_map.getCase(nextCellID));
		_ordreJeu.get(_curPlayer).get_fightCell().addFighter(_ordreJeu.get(_curPlayer));
		if (po != null)
			po.get_fightCell().addFighter(po);// même erreur que tantôt, bug ou
												// plus de fighter sur la case
		if (nStep < 0) {

			Logs.addToGameLog("(" + _curPlayer + ") Fighter ID= " + f.getGUID() + " nStep negatives, reconversion");
			nStep = nStep * (-1);
		}
		_curAction = "GA;129;" + _ordreJeu.get(_curPlayer).getGUID() + ";" + _ordreJeu.get(_curPlayer).getGUID() + ",-"
				+ nStep;

		// Si porteur
		po = _ordreJeu.get(_curPlayer).get_isHolding();
		if (po != null && _ordreJeu.get(_curPlayer).isState(Constant.ETAT_PORTEUR) && po.isState(Constant.ETAT_PORTE)) {
			// on déplace le porté sur la case
			po.set_fightCell(_ordreJeu.get(_curPlayer).get_fightCell());

			Logs.addToGameLog(po.getPacketsName() + " se deplace vers la case " + nextCellID);
		}

		if (f.getPersonnage() == null) {
			try {
				Thread.sleep(900 + 100 * nStep);// Estimation de la durée du
												// déplacement
			} catch (InterruptedException e) {
			}
			;
			SocketManager.GAME_SEND_GAMEACTION_TO_FIGHT(this, 7, _curAction);
			_curAction = "";
			ArrayList<Piege> P = new ArrayList<Piege>();
			P.addAll(_traps);
			for (Piege p : P) {
				Fighter F = _ordreJeu.get(_curPlayer);
				int dist = Pathfinding.getDistanceBetween(_map, p.get_cell().getID(), F.get_fightCell().getID());
				// on active le piege
				if (dist <= p.get_size())
					p.onTraped(F);
			}
			return true;
		}

		f.getPersonnage().get_compte().getGameThread().addAction(GA);
		if ((this._type == 4) && (this._challenges.size() > 0) && !this._ordreJeu.get(this._curPlayer).isInvocation()
				&& !this._ordreJeu.get(this._curPlayer).isDouble() && !this._ordreJeu.get(this._curPlayer).isPerco()) {
			for (Map.Entry<Integer, Challenge> c : this._challenges.entrySet()) {
				if (c.getValue() == null)
					continue;
				c.getValue().onPlayer_move(f);
			}
		}

		return true;
	}

	public void onGK(Characters perso) {
		if (_curAction.equals("") || _ordreJeu.get(_curPlayer).getGUID() != perso.get_GUID()
				|| _state != Constant.FIGHT_STATE_ACTIVE)
			return;

		Logs.addToGameLog("(" + _curPlayer + ")Fin du deplacement de Fighter ID= " + perso.get_GUID());
		SocketManager.GAME_SEND_GAMEACTION_TO_FIGHT(this, 7, _curAction);
		SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 2, _ordreJeu.get(_curPlayer).getGUID());
		// copie
		ArrayList<Piege> P = (new ArrayList<Piege>());
		P.addAll(_traps);
		for (Piege p : P) {
			Fighter F = getFighterByPerso(perso);
			int dist = Pathfinding.getDistanceBetween(_map, p.get_cell().getID(), F.get_fightCell().getID());
			// on active le piege
			if (dist <= p.get_size())
				p.onTraped(F);
			if (_state == Constant.FIGHT_STATE_FINISHED)
				break;
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
		;

		_curAction = "";
	}

	public void playerPass(Characters _perso) {
		Fighter f = getFighterByPerso(_perso);
		if (f == null)
			return;
		if (!f.canPlay())
			return;
		if (!_curAction.equals(""))
			return;// TODO
		endTurn(false);
	}

	public synchronized int tryCastSpell(Fighter fighter, SortStats Spell, int caseID) {
		if (!_curAction.equals(""))
			return 10;
		if (Spell == null)
			return 10;

		Case Cell = _map.getCase(caseID);

		if (CanCastSpell(fighter, Spell, Cell, -1)) {
			_curAction = "casting";
			if (fighter.getPersonnage() != null)
				SocketManager.GAME_SEND_STATS_PACKET(fighter.getPersonnage()); // envoi
																				// des
																				// stats
																				// du
																				// lanceur

			Logs.addToGameLog(fighter.getPacketsName() + " tentative de lancer le sort " + Spell.getSpellID()
					+ " sur la case " + caseID);
			_curFighterPA -= Spell.getPACost(fighter);
			_curFighterUsedPA += Spell.getPACost(fighter);
			SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this, 7, fighter.getGUID()); // infos
																						// concernant
																						// la
																						// dépense
																						// de
																						// PA
																						// ?
			boolean isEc = Spell.getTauxEC() != 0 && Formulas.getRandomValue(1, Spell.getTauxEC()) == Spell.getTauxEC();
			if (isEc) {

				Logs.addToGameLog(fighter.getPacketsName() + " Echec critique sur le sort " + Spell.getSpellID());
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 302, fighter.getGUID() + "",
						Spell.getSpellID() + ""); // envoi de l'EC
				SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, fighter.getGUID());
			} else {
				try {
					if ((this._type == 4) && (this._challenges.size() > 0)
							&& !this._ordreJeu.get(this._curPlayer).isInvocation()
							&& !this._ordreJeu.get(this._curPlayer).isDouble()
							&& !this._ordreJeu.get(this._curPlayer).isPerco()) {
						for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
							if (c.getValue() == null)
								continue;
							c.getValue().onPlayer_action(this._ordreJeu.get(this._curPlayer), Spell.getSpellID());
							c.getValue().onPlayer_spell(this._ordreJeu.get(this._curPlayer));

						}
					}
				} catch (Exception e) {
					e.printStackTrace(System.out);
				}

				// Tentative de lancer coop avec transpo dans le même tour
				if (Spell.getSpellID() == 438 || Spell.getSpellID() == 445) {
					setHasUsedCoopTranspo(true);
				}

				boolean isCC = fighter.testIfCC(Spell.getTauxCC(fighter));
				String sort = Spell.getSpellID() + "," + caseID + "," + Spell.getSpriteID() + "," + Spell.getLevel()
						+ "," + Spell.getSpriteInfos();
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 300, fighter.getGUID() + "", sort); // xx
																										// lance
																										// le
																										// sort
				if (isCC) {

					Logs.addToGameLog(fighter.getPacketsName() + " Coup critique sur le sort " + Spell.getSpellID());
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 301, fighter.getGUID() + "", sort); // CC
																											// !
				}
				if (fighter.isHide() && Spell.getSpellID() == 446)
					fighter.unHide(446);

				// Si le joueur est invi, on montre la case
				if (fighter.isHide())
					showCaseToAll(fighter.getGUID(), fighter.get_fightCell().getID());
				// on applique les effets de l'arme
				Spell.applySpellEffectToFight(this, fighter, Cell, isCC);
			}
			// le org.walaka.rubrumsolem.client ne peut continuer sans l'envoi
			// de ce packet qui annonce le coût en PA
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102, fighter.getGUID() + "",
					fighter.getGUID() + ",-" + Spell.getPACost(fighter));
			SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, fighter.getGUID());
			// Refresh des Stats
			// refreshCurPlayerInfos();
			if (!isEc)
				fighter.addLaunchedSort(Cell.getFirstFighter(), Spell);

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			;
			if ((isEc && Spell.isEcEndTurn())) {
				_curAction = "";
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				;
				if (fighter.getMob() != null || fighter.isInvocation())// Mob,
																		// Invoque
				{
					return 5;
				} else {
					endTurn(false);
					return 5;
				}
			}
			verifIfTeamAllDead();
		} else if (fighter.getMob() != null || fighter.isInvocation()) {
			return 10;
		}
		if (fighter.getPersonnage() != null)
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102, fighter.getGUID() + "", fighter.getGUID() + ",-0"); // annonce
																															// le
																															// coût
																															// en
																															// PA

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		;
		_curAction = "";
		return 0;
	}

	public boolean CanCastSpell2(Fighter fighter, Spell.SortStats spell, Maps.Case cell, int launchCase) {
		if (spell == null)// Si le sort n'existe pas
		{

			Logs.addToGameLog("Sort non existant");
			return false;
		}
		if (_ordreJeu.get(_curPlayer) == null)
			return false;
		if (((Fighter) _ordreJeu.get(_curPlayer)).getGUID() != fighter.getGUID())// Si
																					// ce
																					// n'est
																					// pas
																					// sont
																					// tour
		{

			Logs.addToGameLog("Ce n'est pas au joueur de jouer");
			return false;
		}
		if (_curFighterPA < spell.getPACost(fighter))// Si il na pas asser de pa
		{

			Logs.addToGameLog("Le joueur n'a pas assez de PA (" + _curFighterPA + "/" + spell.getPACost(fighter) + ")");
			return false;
		}
		if (cell == null)// Si cellID inexistante
		{

			Logs.addToGameLog("La cellule visee n'existe pas");
			return false;
		}
		if ((spell.isLineLaunch(fighter)) && (!Pathfinding.casesAreInSameLine(_map, launchCase, cell.getID(), 'z')))// Si
																													// ont
																													// lance
																													// le
																													// sort
																													// la
																													// ou
																													// ont
																													// peut
																													// pas
		{

			Logs.addToGameLog("Le sort demande un lancer en ligne, or la case n'est pas alignee avec le joueur");
			return false;
		}
		if ((spell.hasLDV(fighter)) && (!Pathfinding.checkLoS(_map, launchCase, cell.getID(), fighter)))// Si
																										// problÂŽme
																										// de
																										// cellID
		{

			Logs.addToGameLog(
					"Le sort demande une ligne de vue, mais la case visee n'est pas visible pour le joueur (IA)");
			return false;
		}
		int dist = Pathfinding.getDistanceBetween(_map, launchCase, cell.getID());
		int MaxPO = spell.getMaxPO(fighter);
		if (spell.isModifPO(fighter)) {
			MaxPO += fighter.getTotalStats().getEffect(117);
		}
		if ((dist < spell.getMinPO()) || (dist > MaxPO))// Si mauvaise PO
		{

			Logs.addToGameLog("La case est trop proche ou trop eloignee(2) (IA) Min: " + spell.getMinPO() + " Max: "
					+ spell.getMaxPO(fighter) + " Dist: " + dist);
			return false;
		}
		if (!LaunchedSort.coolDownGood(fighter, spell.getSpellID())) {
			return false;
		}
		int nbLancer = spell.getMaxLaunchbyTurn(fighter);
		if ((nbLancer - LaunchedSort.getNbLaunch(fighter, spell.getSpellID()) <= 0) && (nbLancer > 0)) {
			return false;
		}
		Fighter target = cell.getFirstFighter();
		int nbLancerT = spell.getMaxLaunchbyByTarget(fighter);
		return (nbLancerT - LaunchedSort.getNbLaunchTarget(fighter, target, spell.getSpellID()) > 0)
				|| (nbLancerT <= 0);
	}

	public boolean CanCastSpell3(Fighter fighter, SortStats spell, Maps.Case cell, int launchCase)
	/*      */ {
		/*      */ int ValidlaunchCase;
		/* 1738 */ if (launchCase <= -1)
		/*      */ {
			/* 1740 */ ValidlaunchCase = fighter.get_fightCell().getID();
			/*      */ }
		/*      */ else {
			/* 1743 */ ValidlaunchCase = launchCase;
			/*      */ }
		/*      */
		/* 1746 */ Fighter f = (Fighter) this._ordreJeu.get(this._curPlayer);
		/* 1747 */ Characters perso = fighter.getPersonnage();
		/*      */
		/* 1749 */ if (spell == null)
		/*      */ {
			/* 1751 */
			Logs.addToGameLog("(" + this._curPlayer + ") Sort non existant");
			/* 1752 */ if (perso != null)
			/*      */ {
				/* 1754 */ SocketManager.GAME_SEND_Im_PACKET(perso, "1169");
				/*      */ }
			/* 1756 */ return false;
			/*      */ }
		/*      */
		/* 1759 */ if ((f == null) || (f.getGUID() != fighter.getGUID()))
		/*      */ {
			/* 1761 */
			Logs.addToGameLog(
					"Ce n'est pas au joueur. Doit jouer :(" + f.getGUID() + "). Fautif :(" + fighter.getGUID() + ")");
			/* 1762 */ if (perso != null)
			/*      */ {
				/* 1764 */ SocketManager.GAME_SEND_Im_PACKET(perso, "1175");
				/*      */ }
			/* 1766 */ return false;
			/*      */ }
		/*      */
		/* 1769 */ if (this._curFighterPA < spell.getPACost(fighter))
		/*      */ {
			/* 1771 */
			Logs.addToGameLog("(" + this._curPlayer + ") Le joueur n'a pas assez de PA (" + this._curFighterPA + "/"
					+ spell.getPACost(fighter) + ")");
			/* 1772 */ if (perso != null)
			/*      */ {
				/* 1774 */ SocketManager.GAME_SEND_Im_PACKET(perso,
						"1170;" + this._curFighterPA + "~" + spell.getPACost(fighter));
				/*      */ }
			/* 1776 */ return false;
			/*      */ }
		/*      */
		/* 1779 */ if (cell == null)
		/*      */ {
			/* 1781 */
			Logs.addToGameLog("(" + this._curPlayer + ") La cellule visee n'existe pas");
			/* 1782 */ if (perso != null)
			/*      */ {
				/* 1784 */ SocketManager.GAME_SEND_Im_PACKET(perso, "1172");
				/*      */ }
			/* 1786 */ return false;
			/*      */ }
		/*      */
		/* 1789 */ if ((spell.isLineLaunch(fighter))
				&& (!Pathfinding.casesAreInSameLine(this._map, ValidlaunchCase, cell.getID(), 'z')))
		/*      */ {
			/* 1791 */
			Logs.addToGameLog("(" + this._curPlayer
					+ ") Le sort demande un lancer en ligne, or la case n'est pas alignee avec le joueur");
			/* 1792 */ if (perso != null)
			/*      */ {
				/* 1794 */ SocketManager.GAME_SEND_Im_PACKET(perso, "1173");
				/*      */ }
			/* 1796 */ return false;
			/*      */ }
		/*      */
		/* 1799 */ if ((spell.hasLDV(fighter))
				&& (!Pathfinding.checkLoS(this._map, ValidlaunchCase, cell.getID(), fighter)))
		/*      */ {
			/* 1801 */
			Logs.addToGameLog("(" + this._curPlayer
					+ ") Le sort demande une ligne de vue, mais la case visee n'est pas visible pour le joueur");
			/* 1802 */ if (perso != null)
			/*      */ {
				/* 1804 */ SocketManager.GAME_SEND_Im_PACKET(perso, "1174");
				/*      */ }
			/* 1806 */ return false;
			/*      */ }
		/*      */
		/* 1809 */ int dist = Pathfinding.getDistanceBetween(this._map, ValidlaunchCase, cell.getID());
		/* 1810 */ int MaxPO = spell.getMaxPO(fighter);
		/* 1811 */ if (spell.isModifPO(fighter))
		/*      */ {
			/* 1813 */ MaxPO += fighter.getTotalStats().getEffect(117);
			/*      */ }
		/*      */
		/* 1816 */ if ((dist < spell.getMinPO()) || (dist > MaxPO))
		/*      */ {
			/* 1818 */
			Logs.addToGameLog("(" + this._curPlayer + ") La case est trop proche ou trop eloignee Min: "
					+ spell.getMinPO() + " Max: " + spell.getMaxPO(fighter) + " Dist: " + dist);
			/* 1819 */ if (perso != null)
			/*      */ {
				/* 1821 */ SocketManager.GAME_SEND_Im_PACKET(perso,
						"1171;" + spell.getMinPO() + "~" + spell.getMaxPO(fighter) + "~" + dist);
				/*      */ }
			/* 1823 */ return false;
			/*      */ }
		/*      */
		/* 1826 */ if (!LaunchedSort.coolDownGood(fighter, spell.getSpellID()))
		/*      */ {
			/* 1828 */ return false;
			/*      */ }
		/*      */
		/* 1831 */ int nbLancer = spell.getMaxLaunchbyTurn(fighter);
		/* 1832 */ if ((nbLancer - LaunchedSort.getNbLaunch(fighter, spell.getSpellID()) <= 0) && (nbLancer > 0))
		/*      */ {
			/* 1834 */ return false;
			/*      */ }
		/*      */
		/* 1837 */ Fighter target = cell.getFirstFighter();
		/* 1838 */ int nbLancerT = spell.getMaxLaunchbyByTarget(fighter);
		/*      */
		/* 1841 */ return (nbLancerT - LaunchedSort.getNbLaunchTarget(fighter, target, spell.getSpellID()) > 0)
				|| (nbLancerT <= 0);
		/*      */ }

	public synchronized boolean CanCastSpell(Fighter fighter, SortStats spell, Case cell, int launchCase) {
		int ValidlaunchCase;
		if (launchCase <= -1) {
			ValidlaunchCase = fighter.get_fightCell().getID();
		} else {
			ValidlaunchCase = launchCase;
		}
		if (_ordreJeu == null || _ordreJeu.isEmpty() || _ordreJeu.get(_curPlayer) == null)
			return false;
		Fighter f = _ordreJeu.get(_curPlayer);
		if (f == null)
			return false;
		Characters perso = fighter.getPersonnage();
		// Si le sort n'est pas existant
		if (spell == null) {

			Logs.addToGameLog("(" + _curPlayer + ") Sort non existant");
			if (perso != null) {
				SocketManager.GAME_SEND_Im_PACKET(perso, "1169");
			}
			return false;
		}
		// Si ce n'est pas au joueur de jouer
		if (f == null || f.getGUID() != fighter.getGUID()) {

			Logs.addToGameLog(
					"Ce n'est pas au joueur. Doit jouer :(" + f.getGUID() + "). Fautif :(" + fighter.getGUID() + ")");
			if (perso != null) {
				SocketManager.GAME_SEND_Im_PACKET(perso, "1175");
			}
			return false;
		}
		// Si le joueur n'a pas assez de PA
		if (_curFighterPA < spell.getPACost(fighter)) {

			Logs.addToGameLog("(" + _curPlayer + ") Le joueur n'a pas assez de PA (" + _curFighterPA + "/"
					+ spell.getPACost(fighter) + ")");
			if (perso != null) {
				SocketManager.GAME_SEND_Im_PACKET(perso, "1170;" + _curFighterPA + "~" + spell.getPACost(fighter));
			}
			return false;
		}

		// Si la cellule visée n'existe pas
		if (cell == null) {

			Logs.addToGameLog("(" + _curPlayer + ") La cellule visee n'existe pas");
			if (perso != null) {
				SocketManager.GAME_SEND_Im_PACKET(perso, "1172");
			}
			return false;
		}
		// Si la cellule visée n'est pas alignée avec le joueur alors que le
		// sort le demande
		if (spell.isLineLaunch(fighter) && !Pathfinding.casesAreInSameLine(_map, ValidlaunchCase, cell.getID(), 'z')) {

			Logs.addToGameLog("(" + _curPlayer
					+ ") Le sort demande un lancer en ligne, or la case n'est pas alignee avec le joueur");
			if (perso != null) {
				SocketManager.GAME_SEND_Im_PACKET(perso, "1173");
			}
			return false;
		}
		// Si tentative d'ajout d'un objet/invoc sur un joueur invisible
		// |Return,Skryn
		for (SpellEffect s : spell.getEffects()) {
			if (s.getEffectID() == 185 || s.getEffectID() == 181 || s.getEffectID() == 180 || s.getEffectID() == 400
					|| s.getEffectID() == 780) {
				for (Fighter p : getAllFighters()) {
					if (cell == p.get_fightCell()) {
						SocketManager.GAME_SEND_MESSAGE(perso, "La case visée n'est pas disponible.",
								Config.CONFIG_MOTD_COLOR);
						return false;
					}
				}
			}
		}

		/**
		 * if (isMob){ for (Fighter p: getAllFighters()){ if (cell ==
		 * p.get_fightCell()){ if (fighter.getTeam2() != p.getTeam2()){ if
		 * (spell.getSpellID() == 22 || spell.getSpellID() == 2041){ return
		 * false; } } } } }
		 **/
		/**
		 * Si tentative de transfert de vie quand dérobade actif, false
		 * |Return,Skryn, j'ai refais le buff 9 ;) if (spell.getSpellID() ==
		 * 435){ if (fighter.hasBuff(9)){ SocketManager.GAME_SEND_MESSAGE(perso,
		 * "Le sort Transfert de Vie est innutilisable lorsque vous êtes sous
		 * l'emprise du sort Dérobade.", Config.CONFIG_MOTD_COLOR); return
		 * false; } }
		 **/
		// Si le sort demande une ligne de vue et que la case demandée n'en fait
		// pas partie
		if (spell.hasLDV(fighter) && !Pathfinding.checkLoS(_map, ValidlaunchCase, cell.getID(), fighter, false, null)) {

			Logs.addToGameLog("(" + _curPlayer
					+ ") Le sort demande une ligne de vue, mais la case visee n'est pas visible pour le joueur");
			if (perso != null) {
				SocketManager.GAME_SEND_Im_PACKET(perso, "1174");
			}
			return false;
		}

		// Pour peur si la personne poussée a la ligne de vue vers la case
		char dir = Pathfinding.getDirBetweenTwoCase(ValidlaunchCase, cell.getID(), _map, true);
		if (spell.getSpellID() == 67)
			if (!Pathfinding.checkLoS(_map, Pathfinding.GetCaseIDFromDirrection(ValidlaunchCase, dir, _map, true),
					cell.getID(), null, true, getAllFighters())) {

				Logs.addToGameLog("(" + _curPlayer
						+ ") Le sort demande une ligne de vue, mais la case visee n'est pas visible pour le joueur");
				if (perso != null)
					SocketManager.GAME_SEND_Im_PACKET(perso, "1174");
				return false;
			}
		int dist = Pathfinding.getDistanceBetween(_map, ValidlaunchCase, cell.getID());
		int MaxPO = spell.getMaxPO(fighter);

		if (fighter.getPersonnage() != null) {
			if (MaxPO < 1)
				MaxPO = 1;
			if (dist < 1)
				dist = 1;
		}

		if (spell.isModifPO(fighter)) {
			MaxPO += fighter.getTotalStats().getEffect(Constant.STATS_ADD_PO);
		}
		// Vérification Portée mini / maxi
		if (dist < spell.getMinPO() || dist > MaxPO) {

			Logs.addToGameLog("(" + _curPlayer + ") La case est trop proche ou trop eloignee Min: " + spell.getMinPO()
					+ " Max: " + spell.getMaxPO(fighter) + " Dist: " + dist);
			if (perso != null) {
				SocketManager.GAME_SEND_Im_PACKET(perso,
						"1171;" + spell.getMinPO() + "~" + spell.getMaxPO(fighter) + "~" + dist);
			}
			return false;
		}
		// vérification cooldown
		if (!LaunchedSort.coolDownGood(fighter, spell.getSpellID())) {
			if (fighter.getPersonnage() != null && spell.getSpellID() == 59) {
				SocketManager.GAME_SEND_MESSAGE(fighter.getPersonnage(),
						"Vous ne pouvez lancer le sort Corruption qu'au bout de votre troisième tour de jeu.",
						"A00000");
			}
			return false;
		}
		// vérification nombre de lancer par tour
		int nbLancer = spell.getMaxLaunchbyTurn(fighter);
		if (nbLancer - LaunchedSort.getNbLaunch(fighter, spell.getSpellID()) <= 0 && nbLancer > 0) {
			return false;
		}
		// vérification nombre de lancer par cible
		Fighter target = cell.getFirstFighter();
		int nbLancerT = spell.getMaxLaunchbyByTarget(fighter);
		if (nbLancerT - LaunchedSort.getNbLaunchTarget(fighter, target, spell.getSpellID()) <= 0 && nbLancerT > 0) {
			return false;
		}
		// Tentative de lancer coop avec transpo dans le même tour
		if ((spell.getSpellID() == 438 || spell.getSpellID() == 445) && HasUsedCoopTranspo()) {
			SocketManager.GAME_SEND_MESSAGE(perso,
					"Il est impossible de lancer coopération et transposition dans un même tour de jeu !",
					Config.CONFIG_MOTD_COLOR);
			return false;
		}
		return true;
	}

	public ArrayList<Fighter> getAllFighters() {
		ArrayList<Fighter> fighters = new ArrayList<Fighter>();

		for (Entry<Integer, Fighter> entry : _team0.entrySet()) {
			fighters.add(entry.getValue());
		}
		for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
			fighters.add(entry.getValue());
		}
		return fighters;
	}

	public synchronized String GetGE(int win) {
		long time = System.currentTimeMillis() - get_startTime();
		int initGUID = _init0.getGUID();

		int type = Constant.FIGHT_TYPE_CHALLENGE;// toujours 0
		if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CONQUETE)// Sauf si gain d'honneur
			type = _type;

		StringBuilder Packet = new StringBuilder();
		Packet.append("GE").append(time);
		// String Packet = "GE"+time;
		// si c'est un combat PVM alors bonus potentiel en étoiles
		if (_type == Constant.FIGHT_TYPE_PVM && _mobGroup != null)
			Packet.append(";").append(_mobGroup.get_bonusValue());
		Packet.append("|").append(initGUID).append("|").append(type).append("|");
		ArrayList<Fighter> TEAM1 = new ArrayList<Fighter>();
		ArrayList<Fighter> TEAM2 = new ArrayList<Fighter>();
		if (win == 1) {
			TEAM1.addAll(_team0.values());
			TEAM2.addAll(_team1.values());
		} else {
			TEAM1.addAll(_team1.values());
			TEAM2.addAll(_team0.values());
		}
		// Calculs des niveaux de groupes
		// int TEAM1lvl = 0;
		// int TEAM2lvl = 0;
		// Traque
		if (_type == Constant.FIGHT_TYPE_AGRESSION) {
			Characters curp = null;
			int nb_perso = 0;
			// Evaluation du level
			for (Fighter F : TEAM1) {
				if (F.isInvocation() || F.isDouble())
					continue;
				if (F.getPersonnage() != null) {
					curp = F.getPersonnage();
					nb_perso++;
				}
				// TEAM1lvl += F.get_lvl();
			}
			// Evaluation de la présence de la traque
			Traque traque = null;
			ArrayList<Traque> traqued_by = null;
			if (curp != null && nb_perso == 1) {
				traque = Stalk.getTraqueByOwner(curp);
				traqued_by = Stalk.getTraquesByTarget(curp);
				if (traqued_by.size() == 0)
					traqued_by = null;
			}
			for (Fighter F : TEAM2) {
				if (F.isInvocation())
					continue;
				if (F.getPersonnage() != null && traque != null) {
					if (traque.getTarget() == F.getPersonnage().get_GUID()) { // Le mec a gagné face à sa traque
						SocketManager.GAME_SEND_Im_PACKET(curp, "0175");
						traque.valider();
					}
				}
				if (F.getPersonnage() != null && traqued_by != null) {
					for (Traque t : traqued_by) {
						if (t.getOwner() == F.getPersonnage().get_GUID()) { // Le mec a perdu face à sa traque
							SocketManager.GAME_SEND_Im_PACKET(F.getPersonnage(), "0179");
							SocketManager.GAME_SEND_Im_PACKET(curp, "0180;" + F.getPersonnage().get_name());
							t.reset();
						}
					}
				}
				// TEAM2lvl += F.get_lvl();
			}
		}
		// fin
		/*
		 * DEBUG System.out.println("TEAM1: lvl="+TEAM1lvl);
		 * System.out.println("TEAM2: lvl="+TEAM2lvl); //
		 */
		// DROP SYSTEM

		// Challenge augmente la PP totale (atteint plus facilement les seuils)
		double factChalDrop = 100;
		if ((this._type == 4) && (this._challenges.size() > 0)) {
			try {
				for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
					if ((c.getValue() == null) || (!((Challenge) c.getValue()).get_win()))
						continue;
					factChalDrop += c.getValue().get_gainDrop();
				}
			} catch (Exception e) {
			}
			factChalDrop += _mobGroup.get_bonusValue(); // on ajoute le bonus en
														// étoiles
		}
		factChalDrop /= 100;
		// Calcul de la PP de groupe
		int groupPP = 0, minkamas = 0, maxkamas = 0;
		for (Fighter F : TEAM1) {
			if (!F.isInvocation() || (F.getMob() != null && F.getMob().getTemplate().getID() == 285))
				groupPP += F.getTotalStats().getEffect(Constant.STATS_ADD_PROS);
		}
		if (groupPP < 0)
			groupPP = 0;
		groupPP *= factChalDrop;
		// Calcul des drops possibles
		ArrayList<Drop> possibleDrops = new ArrayList<Drop>();
		for (Fighter F : TEAM2) {
			// Evaluation de l'argent à gagner
			if (F.isInvocation() || F.getMob() == null)
				continue;
			minkamas += F.getMob().getTemplate().getMinKamas();
			maxkamas += F.getMob().getTemplate().getMaxKamas();
			// Evaluation de la liste des drops droppable
			for (Drop D : F.getMob().getDrops()) {
				if (D.getMinProsp() <= groupPP) {
					int taux = (int) (D.get_taux() * Config.CONFIG_DROP);
					possibleDrops.add(new Drop(D.get_itemID(), 0, taux, D.get_max()));
				}
			}
		}
		if (_type == Constant.FIGHT_TYPE_PVT) {
			minkamas = (int) _perco.getKamas() / TEAM1.size();
			maxkamas = minkamas;
			possibleDrops = _perco.getDrops();
		}
		// On Réordonne la liste des combattants en fonction de la PP
		ArrayList<Fighter> Temp = new ArrayList<Fighter>();
		Fighter curMax = null;
		while (Temp.size() < TEAM1.size()) {
			int curPP = -1;
			for (Fighter F : TEAM1) {
				// S'il a plus de PP et qu'il n'est pas listé
				if (F.getTotalStats().getEffect(Constant.STATS_ADD_PROS) > curPP && !Temp.contains(F)) {
					curMax = F;
					curPP = F.getTotalStats().getEffect(Constant.STATS_ADD_PROS);
				}
			}
			Temp.add(curMax);
		}
		// On enleve les invocs
		TEAM1.clear();
		TEAM1.addAll(Temp);
		/*
		 * DEBUG System.out.println("DROP: PP ="+groupPP);
		 * System.out.println("DROP: nbr="+possibleDrops.size());
		 * System.out.println("DROP: Kam="+totalkamas); //
		 */
		// FIN DROP SYSTEM
		// XP SYSTEM
		long totalXP = 0;
		for (Fighter F : TEAM2) {
			if (F.isInvocation() || F.getMob() == null)
				continue;
			totalXP += F.getMob().getBaseXp();
		}
		if ((this._type == 4) && (this._challenges.size() > 0)) {
			try {
				long totalGainXp = 0;
				for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
					if ((c.getValue() == null) || (!((Challenge) c.getValue()).get_win()))
						continue;
					totalGainXp += c.getValue().get_gainXp();
				}
				totalGainXp += _mobGroup.get_bonusValue(); // on ajoute le bonus
															// en étoiles
				totalXP *= 100L + totalGainXp; // on multiplie par la somme des
												// boost chal
				totalXP /= 100L;

			} catch (Exception e) {
			}

		}
		/*
		 * DEBUG System.out.println("TEAM1: xpTotal="+totalXP); //
		 */
		// FIN XP SYSTEM
		// Capture d'âmes
		boolean mobCapturable = true;
        for(Fighter F : TEAM2)
        {
        	try
        	{
        		mobCapturable &= F.getMob().getTemplate().isCapturable();
        	}catch (Exception e) {
				mobCapturable = false;
				break;
			}
        }
        isCapturable |= mobCapturable;
        
        if(isCapturable && this.get_type() == Constant.FIGHT_TYPE_PVM)
        {
	        boolean isFirst = true;
	        int maxLvl = 0;
	        String pierreStats = "";
	        
	        for(Fighter F : TEAM2)	//Création de la pierre et verifie si le groupe peut être capturé
	        {
	        	if(!isFirst)
	        		pierreStats += "|";
	        	
	        	pierreStats += F.getMob().getTemplate().getID() + "," + F.get_lvl();//Converti l'ID du monstre en Hex et l'ajoute au stats de la futur pierre d'âme
	        	
	        	isFirst = false;
	        	
	        	if(F.get_lvl() > maxLvl)	//Trouve le monstre au plus haut lvl du groupe (pour la puissance de la pierre)
	        		maxLvl = F.get_lvl();
	        }
	        pierrePleine = new SoulStone(World.getNewItemGuid(),1,7010,Constant.ITEM_POS_NO_EQUIPED,pierreStats);	//Crée la pierre d'âme
	        
	        for(Fighter F : TEAM1)	//Récupère les captureur
	        {
	        	if(!F.isInvocation() && F.isState(Constant.ETAT_CAPT_AME))
	        	{
	        		_captureur.add(F);
	        	}
	        }
	        if(_captureur.size() > 0 && !World.isArenaMap(get_map().get_id()))	//S'il y a des captureurs
    		{
	        	Collections.shuffle(_captureur);
    			for (int i = 0; i < _captureur.size(); i++)
    			{
    				try
    				{
		        		Fighter f = _captureur.get(Formulas.getRandomValue(0, _captureur.size()-1));	//Récupère un captureur au hasard dans la liste
		        		if(!(f.getPersonnage().getObjetByPos(Constant.ITEM_POS_ARME).getTemplate().getType() == Constant.ITEM_TYPE_PIERRE_AME))
	    				{
		    				_captureur.remove(f);
	    					continue;
	    				}

		        		if(f.getPersonnage().getObjetByPos(Constant.ITEM_POS_ARME).getTemplate().getID() != 9718)
		        		{
			        		Couple<Integer,Integer> pierreJoueur = Formulas.decompPierreAme(f.getPersonnage().getObjetByPos(Constant.ITEM_POS_ARME));//Récupère les stats de la pierre équippé
			    			if(pierreJoueur.second < maxLvl)	//Si la pierre est trop faible
			    			{
			    				_captureur.remove(f);
		    					continue;
		    				}
							int captChance = Formulas.totalCaptChance(pierreJoueur.first, f.getPersonnage());
							if (Formulas.getRandomValue(1, 100) > captChance)
								continue;
						}
	    				//Retire la pierre vide au personnage et lui envoie ce changement
	    				int pierreVide = f.getPersonnage().getObjetByPos(Constant.ITEM_POS_ARME).getGuid();
	    				f.getPersonnage().deleteItem(pierreVide);
	    				SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(f.getPersonnage(), pierreVide);
	    				
	    				captWinner = f._id;
	    				break;
	    			}
    				catch(NullPointerException e)
    				{
    					continue;
    				}
    			}
    		}
        }
        // Fin Capture
        for (Fighter i : TEAM1)// Les gagnants
		{
			if (i.isInvocation() && i.getMob() != null && i.getMob().getTemplate().getID() != 285)
				continue;// Pas d'invoc dans les gains
			if (i._double != null)
				continue;// Pas de double dans les gains
			if (_type == Constant.FIGHT_TYPE_PVT || _type == Constant.FIGHT_TYPE_PVM || _type == Constant.FIGHT_TYPE_CHALLENGE)// Perco ou PVM
			{
				/** Objecti quÃªte "Vaincre xNbre MobId en 1 seul combat" **/
				if(i.getPersonnage() != null) // C'est bien un joueur 
				{
					Map<Integer,Integer> mobs = new HashMap<Integer, Integer>();
					for(Fighter mob : TEAM2) // On liste les mobs battus & leur nombre
					{
						if(mob.getMob() != null) // C'est bien un mob
						{
							if(mobs.get(mob.getMob().getTemplate().getID()) != null) // Mob dÃ©jÃ  repertoriÃ©
							{
								mobs.put(mob.getMob().getTemplate().getID(), // Id
										mobs.get(mob.getMob().getTemplate().getID()) + 1); // Quantite
							} else
							{
								mobs.put(mob.getMob().getTemplate().getID(), 1);
							}
						}
					}
					String mobsWon = "";
					for(Entry<Integer,Integer> entry : mobs.entrySet()) // On transforme en String
					{
						mobsWon += entry.getKey() + "," + entry.getValue() + ";";
					}
					i.getPersonnage().confirmObjective(6, mobsWon, null);
				}
				/** Fin Objectif QuÃªte "Vaincre x Mob" **/
			}
		}
		/*
		 * Testons Configtenant de mettre l'apprivoisement des DD comme le
		 * système de capture ^^"
		 */
		boolean mobCanAppri = true;

		for (Fighter F : TEAM2) {
			try {
				mobCanAppri &= F.getMob().getTemplate().isApprivoisable();
			} catch (Exception e) {
				mobCanAppri = false;
				break;
			}
		}
		for (Fighter F : TEAM2) {
			try {
				mobCanAppri &= F.getMob().getTemplate().ThereAreThree();
			} catch (Exception e) {
				ThereAreThree = false;
				break;
			}
		}
		for (Fighter F : TEAM2) {
			try {
				mobCanAppri &= F.getMob().getTemplate().ThereAreAmandRousse();
			} catch (Exception e) {
				ThereAreAmandRousse = false;
				break;
			}
		}
		for (Fighter F : TEAM2) {
			try {
				mobCanAppri &= F.getMob().getTemplate().ThereAreAmandDore();
			} catch (Exception e) {
				ThereAreAmandDore = false;
				break;
			}
		}
		for (Fighter F : TEAM2) {
			try {
				mobCanAppri &= F.getMob().getTemplate().ThereAreRousseDore();
			} catch (Exception e) {
				ThereAreRousseDore = false;
				break;
			}
		}
		for (Fighter F : TEAM2) {
			try {
				mobCanAppri &= F.getMob().getTemplate().ThereIsRousse();
			} catch (Exception e) {
				ThereIsRousse = false;
				break;
			}
		}
		for (Fighter F : TEAM2) {
			try {
				mobCanAppri &= F.getMob().getTemplate().ThereIsDore();
			} catch (Exception e) {
				ThereIsDore = false;
				break;
			}
		}
		for (Fighter F : TEAM2) {
			try {
				mobCanAppri &= F.getMob().getTemplate().ThereIsAmand();
			} catch (Exception e) {
				ThereIsAmand = false;
				break;
			}
		}
		CanCaptu |= mobCanAppri;

		if (CanCaptu) {

			for (Fighter F : TEAM1) // Récupère les captureur
			{
				if (!F.isInvocation() && F.isState(Constant.ETAT_APPRIVOISEMENT)) {
					_apprivoiseur.add(F);
				}
			}
			for (int i = 0; i < _apprivoiseur.size(); i++) {
				if (_apprivoiseur.size() > 0) // Si il y a des captureurs
				{
					try {
						Fighter f = _apprivoiseur.get(Formulas.getRandomValue(0, _apprivoiseur.size() - 1)); // Récupère
																												// un
																												// captureur
																												// au
																												// hasard
																												// dans
																												// la
																												// liste
						if (!(f.getPersonnage().getObjetByPos(Constant.ITEM_POS_ARME).getTemplate()
								.getType() == Constant.ITEM_TYPE_FILET_CAPTURE))// Qui
																				// possèdent
																				// un
																				// filet
						{
							_apprivoiseur.remove(f);
							continue;
						}

						int captChance = Formulas.totalAppriChance(5, f.getPersonnage());

						if (Formulas.getRandomValue(1, 100) <= captChance)// Si
																			// le
																			// joueur
																			// obtiens
																			// la
																			// capture
						{
							// Retire la pierre vide au personnage et lui envoie
							// ce changement
							int pierreVide = f.getPersonnage().getObjetByPos(Constant.ITEM_POS_ARME).getGuid();
							int tID = 0;
							for (Fighter F : TEAM2)
								if (F.getMob().getTemplate().ThereAreThree() == true) {
									tID = Formulas.ChoseIn3Time(7819, 7817, 7811);
								} else {
									if (F.getMob().getTemplate().ThereAreAmandDore() == true) {
										tID = Formulas.getRandomValue(7819, 7817);
									} else {
										if (F.getMob().getTemplate().ThereAreAmandRousse() == true) {
											tID = Formulas.getRandomValue(7819, 7811);
										} else {
											if (F.getMob().getTemplate().ThereAreRousseDore() == true) {
												tID = Formulas.getRandomValue(7811, 7817);
											} else {
												if (F.getMob().getTemplate().ThereIsAmand() == true) {
													tID = 7819;
												} else {
													if (F.getMob().getTemplate().ThereIsRousse() == true) {
														tID = 7811;
													} else {
														if (F.getMob().getTemplate().ThereIsDore() == true) {
															tID = 7817;
														}
													}
												}
											}
										}
									}
								}

							f.getPersonnage().deleteItem(pierreVide);
							SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(f.getPersonnage(), pierreVide);
							ObjTemplate T = World.getObjTemplate(tID);
							Objects O = T.createNewItem(0, false, -1);
							// Si retourne true, on l'ajoute au monde
							if (f.getPersonnage().addObjet(O, true))
								World.addObjet(O, true);

							break;
						}
					} catch (NullPointerException e) {
						continue;
					}
				}
			}
		}
				
		// Fin de l'apprivoisement
		
		
		for (Fighter i : TEAM1) {
			if (i.hasLeft())
				continue;// Si il abandonne, il ne gagne pas d'xp
			if (i._double != null) {
				continue;// Pas de double dans les gains
			}
			if (type == Constant.FIGHT_TYPE_CHALLENGE) {
				if (i.isInvocation() && i.getMob() != null && i.getMob().getTemplate().getID() != 285)
					continue;
				//long winxp = Formulas.getXpWinPvm2(i, TEAM1, TEAM2, totalXP);
				long winxp = Formulas.getXpWinPvm(TEAM1, TEAM2, i.getTotalStats().getEffect(Constant.STATS_ADD_SAGE), _mobGroup, _challenges);
				AtomicReference<Long> XP = new AtomicReference<Long>();
				XP.set(winxp);
				long guildxp = Formulas.getGuildXpWin(i, XP);
				long mountxp = 0;

				if (i.getPersonnage() != null && i.getPersonnage().isOnMount()) {
					mountxp = Formulas.getMountXpWin(i, XP);
					i.getPersonnage().getMount().addXp(mountxp);
					SocketManager.GAME_SEND_Re_PACKET(i.getPersonnage(), "+", i.getPersonnage().getMount());
				}
				String drops = "";
				long winKamas = Formulas.getKamasWin(i, TEAM1, minkamas, maxkamas);
				if (i.getPersonnage() != null && i.getPersonnage().getKolizeum() == 1) {
					/** Xp **/
					winxp = 5000000;
					XP.set(winxp);
					/** Drop **/
					drops += Config.COINS_ID + "~" + 2 * Config.RATE_COINS;
					ObjTemplate t = World.getObjTemplate(Config.COINS_ID);
					Objects obj = t.createNewItem(2 * Config.RATE_COINS, false, -1);
					if (i.getPersonnage().addObjet(obj, true))// Si le joueur
																// n'avait pas
																// d'item
																// similaire
						World.addObjet(obj, true);
					/** Kamas **/
					winKamas = 10000000;
					/** Desinscription kolizeum + message winner **/
					i.getPersonnage().teleport(i.getPersonnage().getLastMapFight(),
							World.getCarte(i.getPersonnage().getLastMapFight()).getRandomFreeCellID());
					i.getPersonnage().setKolizeum(-1);
					SocketManager.GAME_SEND_MESSAGE(i.getPersonnage(),
							"Fécilitation ! Vous remportez ce match de kolizeum."
									+ "\nVous gagnez 5 000 000 d'expérience ainsi que 2 Kolizetons et 10 000 000 de kamas.",
							Config.CONFIG_MOTD_COLOR);
					Logs.addToGameLog("Le personnage " + i.getPersonnage()
							+ " a gagné son match de kolizeum et reçoit les récompenses");
					/** Spécial Kolizeum Débugs **/
					Characters perso = i.getPersonnage();
					SocketManager.GAME_SEND_GV_PACKET(perso);
					perso.set_duelID(-1);
					perso.set_ready(false);
					perso.fullPDV();
					SocketManager.GAME_SEND_GV_PACKET(perso);
					SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.get_curCarte(), perso);
					perso.get_curCell().addPerso(perso);
				}

				if (i.getPersonnage() != null && i.getPersonnage().getArena() == 1) {
					Arena.sendReward(Team.getTeamByID(i.getPersonnage().getTeamID()),
							Team.getTeamByID(TEAM2.get(1).getPersonnage().getTeamID()));
					i.getPersonnage().teleport(i.getPersonnage().getLastMapFight(),
							World.getCarte(i.getPersonnage().getLastMapFight()).getRandomFreeCellID());
					i.getPersonnage().setArena(-1);
					Characters perso = i.getPersonnage();
					SocketManager.GAME_SEND_GV_PACKET(perso);
					perso.set_duelID(-1);
					perso.set_ready(false);
					perso.fullPDV();
					SocketManager.GAME_SEND_GV_PACKET(perso);
					SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.get_curCarte(), perso);
					perso.get_curCell().addPerso(perso);
				}
				
				
				
				// Drop system
				
				ArrayList<Drop> temp = new ArrayList<Drop>();
				temp.addAll(possibleDrops);
				Map<Integer, Integer> itemWon = new TreeMap<Integer, Integer>();
				int PP = i.getTotalStats().getEffect(Constant.STATS_ADD_PROS);
				boolean allIsDropped = false;
				while (!allIsDropped) {
					for (Drop D : temp) {
						int t = (int) (D.get_taux() * PP);// Permet de gerer des
															// taux>0.01
						t = (int) ((double) t * factChalDrop);
						if (_type == Constant.FIGHT_TYPE_PVT)
							t = 10000 / TEAM1.size();
						int jet = Formulas.getRandomValue(0, 100 * 100);
						// System.out.println("PP : "+PP+" chance : "+t+" jet :
						// "+jet);
						if (jet < t) {
							ObjTemplate OT = World.getObjTemplate(D.get_itemID());
							if (OT == null)
								continue;
							// on ajoute a la liste
							itemWon.put(OT.getID(),
									(itemWon.get(OT.getID()) == null ? 0 : itemWon.get(OT.getID())) + 1);

							D.setMax(D.get_max() - 1);
							if (D.get_max() == 0)
								possibleDrops.remove(D);
						}
					}
					allIsDropped = (_type == Constant.FIGHT_TYPE_PVT ? false : true);
					if (possibleDrops.isEmpty())
						allIsDropped = true;
				}
				if (i._id == captWinner && pierrePleine != null) // S'il à
																	// capturé
																	// le groupe
				{
					if (drops.length() > 0)
						drops += ",";
					drops += pierrePleine.getTemplate().getID() + "~" + 1;
					if (i.getPersonnage().addObjet(pierrePleine, false))
						World.addObjet(pierrePleine, true);
				}
				for (Entry<Integer, Integer> entry : itemWon.entrySet()) {
					ObjTemplate OT = World.getObjTemplate(entry.getKey());
					if (OT == null)
						continue;
					if (drops.length() > 0)
						drops += ",";
					drops += entry.getKey() + "~" + entry.getValue();
					Objects obj = OT.createNewItem(entry.getValue(), false, -1);
					if (i.getPersonnage() != null && i.getPersonnage().addObjet(obj, true))
						World.addObjet(obj, true);
					else if (i.isInvocation() && i.getMob().getTemplate().getID() == 285
							&& i.getInvocator().getPersonnage().addObjet(obj, true))
						World.addObjet(obj, true);
				}
				
				// fin drop system
				
				winxp = XP.get();
				if (winxp != 0 && i.getPersonnage() != null)
					i.getPersonnage().addXp(winxp);
				if (winKamas != 0 && i.getPersonnage() != null)
					i.getPersonnage().addKamas(winKamas);
				else if (winKamas != 0 && i.isInvocation() && i.getInvocator().getPersonnage() != null)
					i.getInvocator().getPersonnage().addKamas(winKamas);
				if (guildxp > 0 && i.getPersonnage().getGuildMember() != null)
					i.getPersonnage().getGuildMember().giveXpToGuild(guildxp);

				Packet.append("2;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";")
						.append(i.get_lvl()).append(";").append((i.isDead() ? "1" : "0")).append(";");
				Packet.append(i.xpString(";")).append(";");
				Packet.append((winxp == 0 ? "" : winxp)).append(";");
				Packet.append((guildxp == 0 ? "" : guildxp)).append(";");
				Packet.append((mountxp == 0 ? "" : mountxp)).append(";");
				Packet.append(drops).append(";");// Drop
				Packet.append((winKamas == 0 ? "" : winKamas)).append("|");
			} else {
				// Si c'est un neutre, on ne gagne pas de points
				if (i.isInvocation() && i.getPersonnage() == null)
					continue;// Le bug de pvp
				int winH = 0;
				int winD = 0;
				if (type == Constant.FIGHT_TYPE_AGRESSION) {
					if (_init1.getPersonnage().get_align() != 0 && _init0.getPersonnage().get_align() != 0) {
						if (_init1.getPersonnage().get_compte().get_curIP()
								.compareTo(_init0.getPersonnage().get_compte().get_curIP()) != 0
								|| Config.ALLOW_MULE_PVP) {
							winH = Formulas.calculHonorWin(TEAM1, TEAM2, i);
						}
						if (i.getPersonnage().getDeshonor() > 0)
							winD = -1;
					}

					Characters P = i.getPersonnage();
					if (P.get_honor() + winH < 0)
						winH = -P.get_honor();
					P.addHonor(winH);
					P.setDeshonor(P.getDeshonor() + winD);
					Packet.append("2;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";")
							.append(i.get_lvl()).append(";").append((i.isDead() ? "1" : "0")).append(";");
					Packet.append(
							(P.get_align() != Constant.ALIGNEMENT_NEUTRE ? World.getExpLevel(P.getGrade()).pvp : 0))
							.append(";");
					Packet.append(P.get_honor()).append(";");
					int maxHonor = World.getExpLevel(P.getGrade() + 1).pvp;
					if (maxHonor == -1)
						maxHonor = World.getExpLevel(P.getGrade()).pvp;
					Packet.append((P.get_align() != Constant.ALIGNEMENT_NEUTRE ? maxHonor : 0)).append(";");
					Packet.append(winH).append(";");
					Packet.append(P.getGrade()).append(";");
					Packet.append(P.getDeshonor()).append(";");
					Packet.append(winD);
					Packet.append(";;0;0;0;0;0|");
				} else if (_type == Constant.FIGHT_TYPE_CONQUETE) {
					Characters P = i.getPersonnage();
					if (P != null) {
						winH = 150;
						if (P.get_honor() + winH < 0)
							winH = -P.get_honor();
						P.addHonor(winH);
						P.setDeshonor(P.getDeshonor() + winD);
						SocketManager.GAME_SEND_MESSAGE(P,
								"Vous venez de remporter la victoire ! Vous gagnez " + winH + " points d'honneur !",
								Config.CONFIG_MOTD_COLOR);
						Packet.append("2;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";")
								.append(i.get_lvl()).append(";").append((i.isDead() ? "1" : "0")).append(";");
						Packet.append(
								(P.get_align() != Constant.ALIGNEMENT_NEUTRE ? World.getExpLevel(P.getGrade()).pvp : 0))
								.append(";");
						Packet.append(P.get_honor()).append(";");
						int maxHonor = World.getExpLevel(P.getGrade() + 1).pvp;
						if (maxHonor == -1)
							maxHonor = World.getExpLevel(P.getGrade()).pvp;
						Packet.append((P.get_align() != Constant.ALIGNEMENT_NEUTRE ? maxHonor : 0)).append(";");
						Packet.append(winH).append(";");
						Packet.append(P.getGrade()).append(";");
						Packet.append(P.getDeshonor()).append(";");
						Packet.append(winD);
						Packet.append(";;0;0;0;0;0|");
					} else {
						Prism prisme = i.getPrisme();
						winH = 200;
						if (prisme.getHonor() + winH < 0)
							winH = -prisme.getHonor();
						winH *= 3;
						prisme.addHonor(winH);
						Packet.append("2;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";")
								.append(i.get_lvl()).append(";").append((i.isDead() ? "1" : "0")).append(";");
						Packet.append(World.getExpLevel(prisme.getlevel()).pvp).append(";");
						Packet.append(prisme.getHonor()).append(";");
						int maxHonor = World.getExpLevel(prisme.getlevel() + 1).pvp;
						if (maxHonor == -1)
							maxHonor = World.getExpLevel(prisme.getlevel()).pvp;
						Packet.append(maxHonor).append(";");
						Packet.append(winH).append(";");
						Packet.append(prisme.getlevel()).append(";");
						Packet.append("0;0;;0;0;0;0;0|");
					}
				}
			}
		}
		for (Fighter i : TEAM2) {
			if (i.getPersonnage() != null && i.getPersonnage().getKolizeum() == 1) {
				SocketManager.GAME_SEND_MESSAGE(i.getPersonnage(),
						"Dommage, vous avez perdu ! Mais retentez vite votre chance ...", Config.CONFIG_MOTD_COLOR);
				i.getPersonnage().setKolizeum(-1);
				i.getPersonnage().teleport(i.getPersonnage().getLastMapFight(),
						World.getCarte(i.getPersonnage().getLastMapFight()).getRandomFreeCellID());
			}

			if (i.getPersonnage() != null && i.getPersonnage().getArena() == 1) {
				Arena.withdrawPoints(Team.getTeamByID(i.getPersonnage().getTeamID()),
						Team.getTeamByID(TEAM1.get(1).getPersonnage().getTeamID()));
				i.getPersonnage().teleport(i.getPersonnage().getLastMapFight(),
						World.getCarte(i.getPersonnage().getLastMapFight()).getRandomFreeCellID());
				i.getPersonnage().setArena(-1);
				Characters perso = i.getPersonnage();
				SocketManager.GAME_SEND_GV_PACKET(perso);
				perso.set_duelID(-1);
				perso.set_ready(false);
				perso.fullPDV();
				SocketManager.GAME_SEND_GV_PACKET(perso);
				SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.get_curCarte(), perso);
				perso.get_curCell().addPerso(perso);
			}

			if (i._double != null)
				continue;// Pas de double dans les gains
			if (i.isInvocation() && i.getMob().getTemplate().getID() != 285)
				continue;// On affiche pas les invocs

			if (_type != Constant.FIGHT_TYPE_AGRESSION && _type != Constant.FIGHT_TYPE_CONQUETE) {
				if (i.getPDV() == 0 || i.hasLeft()) {
					Packet.append("0;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";")
							.append(i.get_lvl()).append(";1").append(";").append(i.xpString(";")).append(";;;;|");
				} else {
					Packet.append("0;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";")
							.append(i.get_lvl()).append(";0").append(";").append(i.xpString(";")).append(";;;;|");
				}
			} else {
				// Si c'est un neutre, on ne gagne pas de points
				int winH = 0;
				int winD = 0;
				if (_type == Constant.FIGHT_TYPE_AGRESSION) {
					if (_init1.getPersonnage().get_align() != 0 && _init0.getPersonnage().get_align() != 0) {
						if (_init1.getPersonnage().get_compte().get_curIP()
								.compareTo(_init0.getPersonnage().get_compte().get_curIP()) != 0
								|| Config.ALLOW_MULE_PVP) {
							winH = Formulas.calculHonorWin(TEAM1, TEAM2, i);
						}
					}

					Characters P = i.getPersonnage();
					if (P.get_honor() + winH < 0)
						winH = -P.get_honor();
					P.addHonor(winH);
					if (P.getDeshonor() - winD < 0)
						winD = 0;
					P.setDeshonor(P.getDeshonor() - winD);
					Packet.append("0;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";")
							.append(i.get_lvl()).append(";").append((i.isDead() ? "1" : "0")).append(";");
					Packet.append(
							(P.get_align() != Constant.ALIGNEMENT_NEUTRE ? World.getExpLevel(P.getGrade()).pvp : 0))
							.append(";");
					Packet.append(P.get_honor()).append(";");
					int maxHonor = World.getExpLevel(P.getGrade() + 1).pvp;
					if (maxHonor == -1)
						maxHonor = World.getExpLevel(P.getGrade()).pvp;
					Packet.append((P.get_align() != Constant.ALIGNEMENT_NEUTRE ? maxHonor : 0)).append(";");
					Packet.append(winH).append(";");
					Packet.append(P.getGrade()).append(";");
					Packet.append(P.getDeshonor()).append(";");
					Packet.append(winD);
					Packet.append(";;0;0;0;0;0|");
				} else if (_type == Constant.FIGHT_TYPE_CONQUETE) {
					winH = Formulas.calculHonorWinPrisms(TEAM1, TEAM2, i);
					Characters P = i.getPersonnage();
					if (P != null) {
						winH = -500;
						if (P.get_honor() - 500 < 0)
							P.set_honor(0);
						else
							P.addHonor(winH);
						if (P.getDeshonor() - winD < 0)
							winD = 0;
						P.setDeshonor(P.getDeshonor() - winD);
						Packet.append("0;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";")
								.append(i.get_lvl()).append(";").append((i.isDead() ? "1" : "0")).append(";");
						Packet.append("0;");
						Packet.append(P.get_honor()).append(";");
						int maxHonor = World.getExpLevel(P.getGrade() + 1).pvp;
						if (maxHonor == -1)
							maxHonor = 0;
						Packet.append((P.get_align() != Constant.ALIGNEMENT_NEUTRE ? maxHonor : 0)).append(";");
						Packet.append(winH).append(";");
						Packet.append(P.getGrade()).append(";");
						Packet.append(P.getDeshonor()).append(";");
						Packet.append(winD);
						Packet.append(";;0;0;0;0;0|");
					} else {
						Prism Prisme = i.getPrisme();
						if (Prisme.getHonor() + winH < 0)
							winH = -Prisme.getHonor();
						Prisme.addHonor(winH);
						Packet.append("0;").append(i.getGUID()).append(";").append(i.getPacketsName()).append(";")
								.append(i.get_lvl()).append(";").append((i.isDead() ? "1" : "0")).append(";");
						Packet.append(World.getExpLevel(Prisme.getlevel()).pvp).append(";");
						Packet.append(Prisme.getHonor()).append(";");
						int maxHonor = World.getExpLevel(Prisme.getlevel() + 1).pvp;
						if (maxHonor == -1)
							maxHonor = World.getExpLevel(Prisme.getlevel()).pvp;
						Packet.append(maxHonor).append(";");
						Packet.append(winH).append(";");
						Packet.append(Prisme.getlevel()).append(";");
						Packet.append("0;0;;0;0;0;0;0|");

					}
				}

			}
			if (i.getPersonnage() != null) {
				Characters perso = i.getPersonnage();
				SocketManager.GAME_SEND_GV_PACKET(perso);
				perso.set_duelID(-1);
				perso.set_ready(false);
				perso.fullPDV();
				perso.set_fight(null);
				SocketManager.GAME_SEND_GV_PACKET(perso);
				SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso.get_curCarte(), perso);
				perso.get_curCell().addPerso(perso);
			}
		}
		if (Collector.GetPercoByMapID(_map.get_id()) != null && _type == 4)// On
																			// a
																			// un
																			// percepteur
																			// ONLY
																			// PVM
																			// ?
		{
			Collector p = Collector.GetPercoByMapID(_map.get_id());
			long winxp = (int) Math.floor(Formulas.getXpWinPerco(p, TEAM1, TEAM2, totalXP) / 100);
			long winkamas = (int) Math.floor(Formulas.getKamasWinPerco(minkamas, maxkamas) / 100);
			p.setXp(p.getXp() + winxp);
			p.setKamas(p.getKamas() + winkamas);
			Packet.append("5;").append(p.getGuid()).append(";").append(p.get_N1()).append(",").append(p.get_N2())
					.append(";").append(World.getGuild(p.get_guildID()).get_lvl()).append(";0;");
			Guild G = World.getGuild(p.get_guildID());
			Packet.append(G.get_lvl()).append(";");
			Packet.append(G.get_xp()).append(";");
			Packet.append(World.getGuildXpMax(G.get_lvl())).append(";");
			Packet.append(";");// XpGagner
			Packet.append(winxp).append(";");// XpGuilde
			Packet.append(";");// Monture

			String drops = "";
			ArrayList<Drop> temp1 = new ArrayList<Drop>();
			temp1.addAll(possibleDrops);
			Map<Integer, Integer> itemWon1 = new TreeMap<Integer, Integer>();

			for (Drop D : temp1) {
				int t = (int) (D.get_taux() * 100);// Permet de gerer des
													// taux>0.01
				int jet = Formulas.getRandomValue(0, 100 * 100);
				if (jet < t) {
					ObjTemplate OT = World.getObjTemplate(D.get_itemID());
					if (OT == null)
						continue;
					// on ajoute a la liste
					itemWon1.put(OT.getID(), (itemWon1.get(OT.getID()) == null ? 0 : itemWon1.get(OT.getID())) + 1);

					D.setMax(D.get_max() - 1);
					if (D.get_max() == 0)
						possibleDrops.remove(D);
				}
			}
			for (Entry<Integer, Integer> entry : itemWon1.entrySet()) {
				ObjTemplate OT = World.getObjTemplate(entry.getKey());
				if (OT == null)
					continue;
				if (drops.length() > 0)
					drops += ",";
				drops += entry.getKey() + "~" + entry.getValue();
				Objects obj = OT.createNewItem(entry.getValue(), false, -1);
				p.addObjet(obj);
				World.addObjet(obj, true);
			}
			Packet.append(drops).append(";");// Drop
			Packet.append(winkamas).append("|");

			SQLManager.UPDATE_PERCO(p);
		}
		if ((win != 1) && (_type == 4)) {
			_mapOld.spawnGroupOnDead(this._mobGroup.getAlignement(), 1, true, this._mobGroup.getCellID(),
					this._mobGroup);
		}
		return Packet.toString();
	}

	public boolean verifIfTeamIsDead() {
		boolean fini = true;
		for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
			if (entry.getValue().isInvocation())
				continue;
			if (!entry.getValue().isDead()) {
				fini = false;
				break;
			}
		}
		return fini;
	}

	public void verifIfTeamAllDead() {
		if (_state >= Constant.FIGHT_STATE_FINISHED)
			return;
		boolean team0 = true;
		boolean team1 = true;
		for (Entry<Integer, Fighter> entry : _team0.entrySet()) {
			if (entry.getValue().isInvocation())
				continue;
			if (!entry.getValue().isDead()) {
				team0 = false;
				break;
			}
		}
		for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
			if (entry.getValue().isInvocation())
				continue;
			if (!entry.getValue().isDead()) {
				team1 = false;
				break;
			}
		}
		if (team0 || team1 || !verifyStillInFight()) {
			try {
				if ((this._type == 4) && (this._challenges.size() > 0)) {
					for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
						if (c.getValue() == null)
							continue;
						c.getValue().onFight_end();
					}
				}
			} catch (Exception e) {
			}
			setTimeStartTurn(0L);
			_state = Constant.FIGHT_STATE_FINISHED;
			int winner = team0 ? 2 : 1;

			Logs.addToGameLog("L'equipe " + winner + " gagne !");

			if (getActTimerTask() != null)
				getActTimerTask().cancel();
			setActTimerTask(null);
			// On despawn tous le monde
			_curPlayer = -1;
			for (Entry<Integer, Fighter> entry : _team0.entrySet()) {
				SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_map, entry.getValue().getGUID());
			}
			for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
				SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_map, entry.getValue().getGUID());
			}
			this._init0.getPersonnage().get_curCarte().removeFight(this._id);
			SocketManager.GAME_SEND_FIGHT_GE_PACKET_TO_FIGHT(this, 7, winner);

			for (Entry<Integer, Fighter> entry : _team0.entrySet())// Team
																	// joueurs
			{
				Characters perso = entry.getValue().getPersonnage();
				if (perso == null)
					continue;
				perso.set_duelID(-1);
				perso.set_ready(false);
				perso.set_fight(null);
				Logs.addToGameLog("Le Personnage " + perso.get_name() + " vient de terminer un combat");
			}

			switch (_type)// Team mobs sauf en défi/aggro
			{
			case Constant.FIGHT_TYPE_CHALLENGE:// Défie
				for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
					Characters perso = entry.getValue().getPersonnage();
					if (perso == null)
						continue;
					perso.set_duelID(-1);
					perso.set_ready(false);
					perso.set_fight(null);
					Logs.addToGameLog("Le Personnage " + perso.get_name() + " vient de terminer un combat");

				}
				break;
			case Constant.FIGHT_TYPE_AGRESSION:// Aggro
				for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
					Characters perso = entry.getValue().getPersonnage();
					if (perso == null)
						continue;
					perso.set_duelID(-1);
					perso.set_ready(false);
					perso.set_fight(null);
				}
				break;
			case Constant.FIGHT_TYPE_CONQUETE: // Conquete prisme
				for (Entry<Integer, Fighter> entry : get_team1().entrySet()) {
					Characters perso = entry.getValue().getPersonnage();
					if (perso == null)
						continue;
					perso.set_duelID(-1);
					perso.set_ready(false);
					perso.set_fight(null);
				}
				break;
			case Constant.FIGHT_TYPE_PVM:// PvM
				if (_team1.get(-1) == null)
					return;
				break;
			}
			setFightStarted(false);
			Logs.addToGameLog(">Un combat vient de se terminer avec succes");
			// on vire les spec du combat
			for (Characters perso : _spec.values()) {
				// on remet le perso sur la map
				perso.get_curCarte().addPlayer(perso);
				// SocketManager.GAME_SEND_GV_PACKET(perso); //Mauvaise ligne apparemment
				perso.refreshMapAfterFight();
			}

			World.getCarte(_map.get_id()).removeFight(_id);
			SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(World.getCarte(_map.get_id()));
			_map = null;
			_ordreJeu = null;
			ArrayList<Fighter> winTeam = new ArrayList<Fighter>();
			ArrayList<Fighter> looseTeam = new ArrayList<Fighter>();
			if (team0) {
				looseTeam.addAll(_team0.values());
				winTeam.addAll(_team1.values());
			} else {
				winTeam.addAll(_team0.values());
				looseTeam.addAll(_team1.values());
			}
			try {
				Thread.sleep(1600);
			} catch (Exception E) {
			}
			;
			// Socket de gération du stockage pour faire kolizeum.

			// Pour les gagnants, on active les endFight actions
			String str = "";
			if (_Prisme != null)
				str = _Prisme.getCarte() + "|" + _Prisme.getX() + "|" + _Prisme.getY();
			for (Fighter F : winTeam) {

				if (F._Perco != null) {
					// On actualise la guilde + Message survie
					for (Characters z : World.getGuild(_guildID).getMembers()) {
						if (z == null)
							continue;
						if (z.isOnline()) {
							SocketManager.GAME_SEND_gITM_PACKET(z, Collector.parsetoGuild(z.get_guild().get_id()));
							str += "S" + _perco.get_N1() + "," + _perco.get_N2() + "|";
							str += _perco.get_mapID() + "|";
							str += World.getCarte((short) _perco.get_mapID()).getX() + "|" + World.getCarte((short) _perco.get_mapID()).getY();
							SocketManager.GAME_SEND_gA_PACKET(z, str);
						}
					}
					F._Perco.set_inFight((byte) 0);
					F._Perco.set_inFightID((byte) -1);
					for (Characters z : World.getCarte((short) F._Perco.get_mapID()).getPersos()) {
						if (z == null)
							continue;
						if (z.get_compte() == null || z.get_compte().getGameThread() == null)
							continue;
						SocketManager.GAME_SEND_MAP_PERCO_GMS_PACKETS(z.get_compte().getGameThread().get_out(),
								z.get_curCarte());
					}
				}
				if (F._Prisme != null) {
					for (Characters z : World.getOnlinePersos()) {
						if (z == null)
							continue;
						if (z.get_align() != _Prisme.getalignement())
							continue;
						SocketManager.SEND_CS_SURVIVRE_MESSAGE_PRISME(z, str);
					}
					F._Prisme.setInFight(-1);
					F._Prisme.setFightID(-1);
					for (Characters z : World.getCarte((short) F._Prisme.getCarte()).getPersos()) {
						if (z == null)
							continue;
						SocketManager.SEND_GM_PRISME_TO_MAP(z.get_compte().getGameThread().get_out(), z.get_curCarte());
					}
				}
				if (F.hasLeft())
					continue;
				if (F.getPersonnage() == null)
					continue;
				if (F.isInvocation())
					continue;
				if (!F.getPersonnage().isOnline())
					continue;
				if (_type == 2) {
					if (_Prisme != null)
						SocketManager.SEND_CP_INFO_DEFENSEURS_PRISME(F.getPersonnage(), this.getDefenseurs());
				}
				if (_type != Constant.FIGHT_TYPE_CHALLENGE) {
					if (F.getPDV() <= 0) {
						F.getPersonnage().set_PDV(1);
					} else {
						F.getPersonnage().set_PDV(F.getPDV());
					}
				}
				try {
					Thread.sleep(2000);
				} catch (Exception E) {
				}
				;
				if (_type == Constant.FIGHT_TYPE_CHALLENGE)
					if (F.getPersonnage().get_curCarte().getSubArea().get_subscribe()
							&& F.getPersonnage().get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)// Non abonné
					{
						// On le place a sa statue
						F.getPersonnage().teleport(Constant.getClassStatueMap(F.getPersonnage().get_classe()),
								Constant.getClassStatueCell(F.getPersonnage().get_classe()));
					}
				if (_type != Constant.FIGHT_TYPE_CHALLENGE)
					F.getPersonnage().get_curCarte().applyEndFightAction(_type, F.getPersonnage());
				try {
					Thread.sleep(200);
				} catch (Exception E) {
				}
				;
				F.getPersonnage().refreshMapAfterFight();
			}
			// Pour les perdant on TP au point de sauvegarde
			for (Fighter F : looseTeam) {

				if (F._Perco != null) {
					_mapOld.RemoveNPC(F._Perco.getGuid());
					SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_mapOld, F._Perco.getGuid());
					_perco.DelPerco(F._Perco.getGuid());
					SQLManager.DELETE_PERCO(F._Perco.getGuid());
					// On actualise la guilde + Message défaite
					for (Characters z : World.getGuild(_guildID).getMembers()) {
						if (z == null)
							continue;
						if (z.isOnline()) {
							SocketManager.GAME_SEND_gITM_PACKET(z, Collector.parsetoGuild(z.get_guild().get_id()));
							str += "D" + _perco.get_N1() + "," + _perco.get_N2() + "|";
							str += _perco.get_mapID() + "|";
							str += World.getCarte((short) _perco.get_mapID()).getX() + "|" + World.getCarte((short) _perco.get_mapID()).getY();
							SocketManager.GAME_SEND_gA_PACKET(z, str);
						}
					}
				}
				if (F._Prisme != null) {
					org.common.World.SubArea subarea = _mapOld.getSubArea();
					for (Characters z : World.getOnlinePersos()) {
						if (z == null)
							continue;
						if (z.get_align() == 0) {
							SocketManager.GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(z, subarea.getID() + "|-1|1");
							continue;
						}
						if (z.get_align() == _Prisme.getalignement())
							SocketManager.SEND_CD_MORT_MESSAGE_PRISME(z, str);
						SocketManager.GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(z, subarea.getID() + "|-1|0");
						if (_Prisme.getAreaConquest() != -1) {
							SocketManager.GAME_SEND_aM_ALIGN_PACKET_TO_AREA(z, subarea.getArea().getID() + "|-1");
							subarea.getArea().setPrismeID(0);
							subarea.getArea().setalignement(0);
						}
					}
					int PrismeID = F._Prisme.getID();
					subarea.setPrismeID(0);
					subarea.setalignement(0);
					_mapOld.RemoveNPC(PrismeID);
					SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_mapOld, PrismeID);
					World.removePrisme(PrismeID);
					SQLManager.DELETE_PRISME(PrismeID);
				}
				if (F.hasLeft())
					continue;
				if (F.getPersonnage() == null)
					continue;
				if (F.isInvocation())
					continue;
				if (!F.getPersonnage().isOnline())
					continue;

				/*
				 * if(_type != Constants.FIGHT_TYPE_CHALLENGE) { try {
				 * Thread.sleep(1000); }catch(Exception E){}; int EnergyLoos =
				 * Formulas.getLoosEnergy(F.get_lvl(), _type==1, _type==5); int
				 * Energy = F.getPersonnage().get_energy() - EnergyLoos;
				 * if(Energy < 0) Energy = 0;
				 * F.getPersonnage().set_energy(Energy); if(Energy == 0) {
				 * F.getPersonnage().set_Ghosts(); }else {
				 * F.getPersonnage().warpToSavePos();
				 * F.getPersonnage().set_PDV(1); }
				 * if(F.getPersonnage().isOnline())
				 * SocketManager.GAME_SEND_Im_PACKET(F.getPersonnage(),
				 * "034;"+EnergyLoos); }
				 */
				if (_type != Constant.FIGHT_TYPE_CHALLENGE) {
					F.getPersonnage().warpToSavePos();
					F.getPersonnage().set_PDV(1);
				}
				if (_type == Constant.FIGHT_TYPE_CHALLENGE)
					if (F.getPersonnage().get_curCarte().getSubArea().get_subscribe()
							&& F.getPersonnage().get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)// Non
																											// abonné
					{
						// On le place a sa statue
						F.getPersonnage().teleport(Constant.getClassStatueMap(F.getPersonnage().get_classe()),
								Constant.getClassStatueCell(F.getPersonnage().get_classe()));
					}
				if (_type == Constant.FIGHT_TYPE_CHALLENGE)
					try {
						Thread.sleep(200);
					} catch (Exception E) {
					}
				;
				F.getPersonnage().refreshMapAfterFight();
			}

		}
	}

	public void onFighterDie(Fighter target, Fighter caster) {
		target.setIsDead(true);
		if (!target.hasLeft())
			deadList.put(target.getGUID(), target);// on ajoute le joueur à la liste des cadavres ;)
		SocketManager.GAME_SEND_FIGHT_PLAYER_DIE_TO_FIGHT(this, 7, target.getGUID());
		target.get_fightCell().getFighters().clear();// Supprime tout causait bug si porté/porteur

		if (target.isState(Constant.ETAT_PORTEUR)) {
			Fighter f = target.get_isHolding();
			f.set_fightCell(f.get_fightCell());
			f.get_fightCell().addFighter(f);// Le bug venait par manque de ceci, il ni avait plus de firstFighter
			f.setState(Constant.ETAT_PORTE, 0);// J'ajoute ceci quand même pour signaler qu'ils ne sont plus en état porté/porteur
			target.setState(Constant.ETAT_PORTEUR, 0);
			f.set_holdedBy(null);
			target.set_isHolding(null);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 950, f.getGUID() + "",
					f.getGUID() + "," + Constant.ETAT_PORTE + ",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 950, target.getGUID() + "",
					target.getGUID() + "," + Constant.ETAT_PORTEUR + ",0");
		}

		if (target.getTeam() == 0) {
			TreeMap<Integer, Fighter> team = new TreeMap<Integer, Fighter>();
			team.putAll(_team0);
			for (Entry<Integer, Fighter> entry : team.entrySet()) {
				if (entry.getValue().getInvocator() == null)
					continue;
				if (entry.getValue().hasLeft())
					continue;
				if (entry.getValue().getPDV() == 0)
					continue;
				if (entry.getValue().isDead())
					continue;
				if (entry.getValue().getInvocator().getGUID() == target.getGUID())// si il a été invoqué par le joueur mort
				{
					onFighterDie(entry.getValue(), caster);

					int index = _ordreJeu.indexOf(entry.getValue());
					if (index != -1)
						_ordreJeu.remove(index);

					if (_team0.containsKey(entry.getValue().getGUID()))
						_team0.remove(entry.getValue().getGUID());
					else if (_team1.containsKey(entry.getValue().getGUID()))
						_team1.remove(entry.getValue().getGUID());
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 999, target.getGUID() + "", getGTL());
				}
			}
			if ((this._type == 4) && (this._challenges.size() > 0)) {
				for (Map.Entry<Integer, Challenge> c : this._challenges.entrySet()) {
					if (c.getValue() == null)
						continue;
					c.getValue().onFighter_die(target);
				}
			}
		} else if (target.getTeam() == 1) {
			TreeMap<Integer, Fighter> team = new TreeMap<Integer, Fighter>();
			team.putAll(_team1);
			for (Entry<Integer, Fighter> entry : team.entrySet()) {
				if (entry.getValue().getInvocator() == null)
					continue;
				if (entry.getValue().getPDV() == 0)
					continue;
				if (entry.getValue().isDead())
					continue;
				if (entry.getValue().hasLeft())
					continue;
				if (entry.getValue().getInvocator().getGUID() == target.getGUID())// si il a été invoqué par le joueur mort
				{
					onFighterDie(entry.getValue(), caster);

					int index = _ordreJeu.indexOf(entry.getValue());
					if (index != -1)
						_ordreJeu.remove(index);

					if (_team0.containsKey(entry.getValue().getGUID()))
						_team0.remove(entry.getValue().getGUID());
					else if (_team1.containsKey(entry.getValue().getGUID()))
						_team1.remove(entry.getValue().getGUID());
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 999, target.getGUID() + "", getGTL());
				}
			}
		}
		if (target.getMob() != null) {
			try {
				Iterator<?> iterator = getFighters(target.getTeam2()).iterator();
				while (iterator.hasNext()) {
					Fighter dMob = (Fighter) iterator.next();
					if (dMob.getPersonnage() != null || dMob.isDead() || dMob.isDouble() || dMob.isHide()
							|| Formulas.getRandomValue(1, 2) != 2) {
						continue;
					}
					int emo = 1;
					int Chance = Formulas.getRandomValue(1, 5);
					if (Chance == 2) {
						switch (Formulas.getRandomValue(1, 9)) {
						default:
							continue;
						case 1:
							emo = 12;
							break;
						case 2:
							emo = 7;
							break;
						case 3:
							emo = 3;
							break;
						case 4:
							emo = 8;
							break;
						case 5:
							emo = 5;
							break;
						case 6:
							emo = 10;
							break;
						case 7:
							emo = 4;
							break;
						case 8:
							emo = 9;
							break;
						case 9:
							emo = 11;
							break;
						}
						SocketManager.GAME_SEND_EMOTICONE_TO_FIGHT(this, 7, dMob.getGUID(), emo);
					}
				}
			} catch (Exception exception) {
			}
			// Si c'est une invocation, on la retire de la liste
			try {
				boolean isStatic = false;
				for (int id : Constant.STATIC_INVOCATIONS)
					if (id == target.getMob().getTemplate().getID())
						isStatic = true;
				if (target.isInvocation() && !isStatic) {
					// Il ne peut plus jouer, et est mort on revient au joueur
					// prï¿½cedent pour que le startTurn passe au suivant
					if (!target.canPlay() && _ordreJeu.get(_curPlayer).getGUID() == target.getGUID()) {
						_curPlayer--;
					}
					// Il peut jouer, et est mort alors on passe son tour pour
					// que l'autre joue, puis on le supprime de l'index sans
					// problï¿½mes
					if (target.canPlay() && _ordreJeu.get(_curPlayer).getGUID() == target.getGUID()) {
						_curAction = "";
						endTurn(false);
					}

					// On ne peut pas supprimer l'index tant que le tour du
					// prochain joueur n'est pas lancï¿½
					int index = _ordreJeu.indexOf(target);

					// Si le joueur courant a un index plus ï¿½levï¿½, on le
					// diminue pour ï¿½viter le outOfBound
					if (_curPlayer > index)
						_curPlayer--;

					if (index != -1)
						_ordreJeu.remove(index);

					if (get_team0().containsKey(target.getGUID()))
						get_team0().remove(target.getGUID());
					else if (get_team1().containsKey(target.getGUID()))
						get_team1().remove(target.getGUID());
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 999, target.getGUID() + "", getGTL());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			;
		}
		if ((this._type == Constant.FIGHT_TYPE_PVM) && (this._challenges.size() > 0)) {
			for (Map.Entry<Integer, Challenge> c : this._challenges.entrySet()) {
				if (c.getValue() == null)
					continue;
				c.getValue().onMob_die(target, caster);
			}
		}

		// on supprime les glyphes du joueur
		ArrayList<Glyphe> glyphs = new ArrayList<Glyphe>();// Copie du tableau
		glyphs.addAll(_glyphs);
		for (Glyphe g : glyphs) {
			// Si c'est ce joueur qui l'a lancé
			if (g.get_caster().getGUID() == target.getGUID()) {
				SocketManager.GAME_SEND_GDZ_PACKET_TO_FIGHT(this, 7, "-", g.get_cell().getID(), g.get_size(), 4);
				SocketManager.GAME_SEND_GDC_PACKET_TO_FIGHT(this, 7, g.get_cell().getID());
				_glyphs.remove(g);
			}
		}
		// On supprime les buff lancés par le joueur
		ArrayList<Fighter> tmpTeam = new ArrayList<Fighter>();
		tmpTeam.addAll(_team0.values());
		tmpTeam.addAll(_team1.values());
		for (Fighter ft0 : tmpTeam) {
			if (ft0.isDead() || target.getGUID() == ft0.getGUID())
				continue;
			ft0.deleteBuffByFighter(target);
		}
		// on supprime les pieges du joueur
		try {
			synchronized (_traps) {
				for (Piege p : _traps) {
					if (p.get_caster().getGUID() == target.getGUID()) {
						p.desappear();
						_traps.remove(p);
					}
				}
			}
		} catch (Exception e) {
		}
		if (target.isPerco()) {
			for (Fighter f : this.getFighters(target.getTeam2())) {
				if (f.isDead())
					continue;
				this.onFighterDie(f, target);
				verifIfTeamAllDead();
			}
		}
		try {
			if (target.canPlay() && _ordreJeu.get(_curPlayer) != null)
				if (_ordreJeu.get(_curPlayer).getGUID() == target.getGUID())
					endTurn(false);
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
		;
	}

	public Map<Integer, Fighter> get_team1() {
		return _team1;
	}

	public Map<Integer, Fighter> get_team0() {
		return _team0;
	}

	public int getTeamID(int guid) {
		if (_team0.containsKey(guid))
			return 1;
		if (_team1.containsKey(guid))
			return 2;
		if (_spec.containsKey(guid))
			return 4;
		return -1;
	}

	public int getOtherTeamID(int guid) {
		if (_team0.containsKey(guid))
			return 2;
		if (_team1.containsKey(guid))
			return 1;
		return -1;
	}

	public synchronized void tryCaC(Characters perso, int cellID) {
		Fighter caster = getFighterByPerso(perso);

		if (caster == null)
			return;

		if (_ordreJeu.get(_curPlayer).getGUID() != caster.getGUID())// Si ce
																	// n'est pas
																	// a lui de
																	// jouer
			return;
		// Pour les challenges, vérif sur CaC
		if ((this._type == 4) && (this._challenges.size() > 0) && !this._ordreJeu.get(this._curPlayer).isInvocation()
				&& !this._ordreJeu.get(this._curPlayer).isDouble() && !this._ordreJeu.get(this._curPlayer).isPerco()) {
			for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
				if (c.getValue() == null)
					continue;
				c.getValue().onPlayer_cac(this._ordreJeu.get(this._curPlayer));
			}
		}
		// Fin Challenges
		if (perso.getObjetByPos(Constant.ITEM_POS_ARME) == null)// S'il n'a pas
																// de CaC
		{
			if (_curFighterPA < 4)// S'il n'a pas assez de PA
				return;

			SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this, 7, perso.get_GUID());

			// Si le joueur est invisible
			if (caster.isHide())
				caster.unHide(-1);

			Fighter target = _map.getCase(cellID).getFirstFighter();

			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 303, perso.get_GUID() + "", cellID + "");

			if (target != null) {
				int dmg = Formulas.getRandomJet("1d5+0");
				int finalDommage = Formulas.calculFinalDommage(this, caster, target, Constant.ELEMENT_NEUTRE, dmg,
						false, true, -1, false);
				finalDommage = SpellEffect.applyOnHitBuffs(finalDommage, target, caster, this, false);// S'il
																										// y
																										// a
																										// des
																										// buffs
																										// spéciaux

				if (finalDommage > target.getPDV())
					finalDommage = target.getPDV();// Target va mourir
				target.removePDV(finalDommage);
				finalDommage = -(finalDommage);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 100, caster.getGUID() + "",
						target.getGUID() + "," + finalDommage);
			}
			_curFighterPA -= 4;
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102, perso.get_GUID() + "", perso.get_GUID() + ",-4");
			SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, perso.get_GUID());

			if (target.getPDV() <= 0)
				onFighterDie(target, caster);
			verifIfTeamAllDead();
		} else {
			Objects arme = perso.getObjetByPos(Constant.ITEM_POS_ARME);

			// Pierre d'âmes = EC
			if (arme.getTemplate().getType() == 83) {
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 305, perso.get_GUID() + "", "");// Echec
																									// Critique
																									// Cac
				SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, perso.get_GUID());// Fin
																							// de
																							// l'action
				try {
					Thread.sleep(500);
				} catch (Exception e) {
				}
				endTurn(false);
			}

			int PACost = arme.getTemplate().getPACost();

			if (_curFighterPA < PACost)// S'il n'a pas assez de PA
				return;
			if (!Pathfinding.canUseCaConPO(this, _ordreJeu.get(_curPlayer).get_fightCell().getID(), cellID, arme)) {
				SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, perso.get_GUID());
				return;
			}

			SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this, 7, perso.get_GUID());

			boolean isEc = arme.getTemplate().getTauxEC() != 0
					&& Formulas.getRandomValue(1, arme.getTemplate().getTauxEC()) == arme.getTemplate().getTauxEC();
			if (isEc) {

				Logs.addToGameLog(perso.get_name() + " Echec critique sur le CaC ");
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 305, perso.get_GUID() + "", "");// Echec
																									// Critique
																									// Cac
				SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, perso.get_GUID());// Fin
																							// de
																							// l'action
				endTurn(false);
			} else {
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 303, perso.get_GUID() + "", cellID + "");
				boolean isCC = caster.testIfCC(arme.getTemplate().getTauxCC());
				if (isCC) {

					Logs.addToGameLog(perso.get_name() + " Coup critique sur le CaC");
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 301, perso.get_GUID() + "", "0");
				}

				// Si le joueur est invisible
				if (caster.isHide())
					caster.unHide(-1);

				ArrayList<SpellEffect> effets = arme.getEffects();
				if (isCC) {
					effets = arme.getCritEffects();
				}
				for (SpellEffect SE : effets) {
					if (_state != Constant.FIGHT_STATE_ACTIVE)
						break;
					ArrayList<Fighter> cibles = Pathfinding.getCiblesByZoneByWeapon(this, arme.getTemplate().getType(),
							_map.getCase(cellID), caster.get_fightCell().getID());
					SE.setTurn(0);
					/**
					 * if (caster.hasBuff(9)){ if (SE.getEffectID() == 90){
					 * SocketManager.GAME_SEND_MESSAGE(caster.getPersonnage(),
					 * "Sort innutilisable quand dérobage est actif !",
					 * Config.CONFIG_MOTD_COLOR); } }
					 **/
					SE.applyToFight(this, caster, cibles, true);
				}
				/**
				 * 7172 Baguette Rhon 7156 Marteau Ronton 1355 Arc Hidsad 7182
				 * Racine Hécouanone 7040 Arc de Kuri 6539 Pelle Gicque 6519
				 * Baguette de Kouartz 8118 Baguette du Scarabosse Doré
				 */
				int idArme = arme.getTemplate().getID();
				int basePdvSoin = 1;
				int pdvSoin = -1;
				if (idArme == 7172 || idArme == 7156 || idArme == 1355 || idArme == 7182 || idArme == 7040
						|| idArme == 6539 || idArme == 6519 || idArme == 8118) {
					pdvSoin = Constant.getArmeSoin(idArme);
					if (pdvSoin != -1) {
						if (isCC) {
							basePdvSoin = basePdvSoin + arme.getTemplate().getBonusCC();
							pdvSoin = pdvSoin + arme.getTemplate().getBonusCC();
						}
						int intel = perso.get_baseStats().getEffect(Constant.STATS_ADD_INTE)
								+ perso.getStuffStats().getEffect(Constant.STATS_ADD_INTE)
								+ perso.getDonsStats().getEffect(Constant.STATS_ADD_INTE)
								+ perso.getBuffsStats().getEffect(Constant.STATS_ADD_INTE);
						int soins = perso.get_baseStats().getEffect(Constant.STATS_ADD_SOIN)
								+ perso.getStuffStats().getEffect(Constant.STATS_ADD_SOIN)
								+ perso.getDonsStats().getEffect(Constant.STATS_ADD_SOIN)
								+ perso.getBuffsStats().getEffect(Constant.STATS_ADD_SOIN);
						int minSoin = basePdvSoin * (100 + intel) / 100 + soins;
						int maxSoin = pdvSoin * (100 + intel) / 100 + soins;
						int finalSoin = Formulas.getRandomValue(minSoin, maxSoin);
						Fighter target = _map.getCase(cellID).getFirstFighter();
						if ((finalSoin + target.getPDV()) > target.getPDVMAX())
							finalSoin = target.getPDVMAX() - target.getPDV();// Target
																				// va
																				// mourrir
						target.removePDV(-finalSoin);
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 100, target.getGUID() + "",
								target.getGUID() + ",+" + finalSoin);
					}
				}
				_curFighterPA -= PACost;
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 102, perso.get_GUID() + "",
						perso.get_GUID() + ",-" + PACost);
				SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, perso.get_GUID());
				verifIfTeamAllDead();
			}
		}
	}

	public Fighter getFighterByPerso(Characters perso) {
		Fighter fighter = null;
		if (_team0.get(perso.get_GUID()) != null)
			fighter = _team0.get(perso.get_GUID());
		if (_team1.get(perso.get_GUID()) != null)
			fighter = _team1.get(perso.get_GUID());
		return fighter;
	}

	public Fighter getCurFighter() {
		return _ordreJeu.get(_curPlayer);
	}

	public void refreshCurPlayerInfos() {
		_curFighterPA = _ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constant.STATS_ADD_PA) - _curFighterUsedPA;
		_curFighterPM = _ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constant.STATS_ADD_PM) - _curFighterUsedPM;
	}

	public boolean reconnexion(Characters perso) {
		Fighter f = getFighterByPerso(perso);
		if (f == null)
			return false;
		if (_state == Constant.FIGHT_STATE_INIT)
			return false;
		f.Reconnect();
		if (_state == Constant.FIGHT_STATE_FINISHED)
			return false;
		// Si combat en cours on envois des im
		SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 7,
				new StringBuilder("1184;").append(f.getPacketsName()).toString());
		try {
			Thread.sleep(200);
		} catch (Exception e) {
		}
		if (_state == Constant.FIGHT_STATE_ACTIVE)
			SocketManager.GAME_SEND_GJK_PACKET(perso, _state, 0, 0, 0, 0, _type);// Join
																					// Fight
																					// =>
																					// _state,
																					// pas
																					// d'anulation...
		else {
			if (_type == Constant.FIGHT_TYPE_CHALLENGE) {
				if (perso.getArena() != -1 || perso.getKolizeum() != -1)
					SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, 0, _type);
				else
					SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 1, 1, 0, 0, _type);
			} else {
				SocketManager.GAME_SEND_GJK_PACKET(perso, 2, 0, 1, 0, 0, _type);
			}
		}
		try {
			Thread.sleep(200);
		} catch (Exception e) {
		}
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(perso, _map,
				(f.getTeam() == 0 ? _init0 : _init1).getGUID(), f);// Indication
																	// de la
																	// team
		SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(perso, f);
		SocketManager.GAME_SEND_STATS_PACKET(perso);
		try {
			Thread.sleep(1500);
		} catch (Exception e) {
		}
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this, _map, perso);
		try {
			Thread.sleep(1500);
		} catch (Exception e) {
		}
		if (_state == Constant.FIGHT_STATE_PLACE) {
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso.get_compte().getGameThread().get_out(),
					_map.get_placesStr(), _st1);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID() + "",
					perso.get_GUID() + "," + Constant.ETAT_PORTE + ",0");
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 3, 950, perso.get_GUID() + "",
					perso.get_GUID() + "," + Constant.ETAT_PORTEUR + ",0");
		} else {
			SocketManager.GAME_SEND_GS_PACKET(perso);// Début du jeu
			SocketManager.GAME_SEND_GTL_PACKET(perso, this);// Liste des tours
			SocketManager.GAME_SEND_GAMETURNSTART_PACKET(perso, _ordreJeu.get(_curPlayer).getGUID(),
					Constant.TIME_BY_TURN);
			for (Map.Entry<Integer, Challenge> c : this._challenges.entrySet()) {
				if (c.getValue() == null)
					continue;
				c.getValue().onFight_start();
				SocketManager.send(perso, c.getValue().parseToPacket());
			}
			for (Fighter f1 : getFighters(3)) {
				f1.sendState(perso);
			}
		}
		SocketManager.GAME_SEND_ILF_PACKET(perso, 0);
		return true;
	}

	public boolean deconnexion(Characters perso, boolean verif)// True si entre
																// en mode
																// déconnexion
																// en combat,
																// false sinon
	{
		Fighter f = getFighterByPerso(perso);
		if (f == null)
			return false;
		if (_state == Constant.FIGHT_STATE_INIT || _state == Constant.FIGHT_STATE_FINISHED) {
			if (!verif)
				leftFight(perso, null, false);
			return false;
		}
		if (f.getNBDeco() >= 5) {
			if (!verif) {
				leftFight(perso, null, false);
				for (Fighter e : this.getFighters(7)) {
					if (e.getPersonnage() == null || e.getPersonnage().isOnline() == false)
						continue;
					SocketManager.GAME_SEND_MESSAGE(e.getPersonnage(),
							f.getPacketsName()
									+ " s'est déconnecté plus de 5 fois dans le même combat, nous avons décidé de lui faire abandonner.",
							"A00000");
				}
			}
			return false;
		}
		if (!verif) {
			if (isFightStarted() == false) {
				perso.set_ready(true);
				perso.get_fight().verifIfAllReady();
				SocketManager.GAME_SEND_FIGHT_PLAYER_READY_TO_FIGHT(perso.get_fight(), 3, perso.get_GUID(), true);
			}
		}
		if (!verif) {
			f.Deconnect();
			// Si combat en cours on envois des im
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 7, new StringBuilder("1182;").append(f.getPacketsName())
					.append("~").append(f.getToursRestants()).toString());
		}
		return true;
	}

	public void leftFight(Characters perso, Characters target, boolean isDebug) {
		if (perso == null)
			return;
		Fighter F = this.getFighterByPerso(perso);
		Fighter T = null;
		if (target != null)
			T = this.getFighterByPerso(target);

		{
			if (target != null && T != null) {
				Logs.addToGameLog(perso.get_name() + " expulse " + T.getPersonnage().get_name());
			} else {
				Logs.addToGameLog(perso.get_name() + " a quitter le combat");
			}
		}

		if (F != null) {

			switch (_type) {
			case Constant.FIGHT_TYPE_CHALLENGE:// Défie
			case Constant.FIGHT_TYPE_AGRESSION:// PVP
			case Constant.FIGHT_TYPE_PVM:// PVM
			case Constant.FIGHT_TYPE_PVT:// Perco
			case Constant.FIGHT_TYPE_CONQUETE:// Prismes

				if (_state >= Constant.FIGHT_STATE_ACTIVE) {
					onFighterDie(F, F);
					boolean StillInFight = false;
					if (_type == Constant.FIGHT_TYPE_CHALLENGE || _type == Constant.FIGHT_TYPE_AGRESSION
							|| _type == Constant.FIGHT_TYPE_PVT || _type == Constant.FIGHT_TYPE_CONQUETE) {
						StillInFight = verifyStillInFightTeam(F.getGUID());

					} else {
						StillInFight = verifyStillInFight();
					}

					if (!StillInFight)// S'arrête ici si il ne reste plus
										// personne dans le combat et dans la
										// team
					{
						// Met fin au combat
						verifIfTeamAllDead();
					} else {
						F.setLeft(true);
						SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_map, F.getGUID());

						Characters P = F.getPersonnage();
						P.set_duelID(-1);
						P.set_ready(false);
						P.set_fight(null);
						P.setSitted(false);
						P.set_away(false);

						if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CONQUETE
								|| _type == Constant.FIGHT_TYPE_PVM || _type == Constant.FIGHT_TYPE_PVT) {
							/*
							 * int EnergyLoos =
							 * Formulas.getLoosEnergy(P.get_lvl(), _type==1,
							 * _type==5); int Energy = P.get_energy() -
							 * EnergyLoos; if(Energy < 0) Energy = 0;
							 * P.set_energy(Energy); if(P.isOnline())
							 * SocketManager.GAME_SEND_Im_PACKET(P,
							 * "034;"+EnergyLoos);
							 * 
							 */
							if (_type == Constant.FIGHT_TYPE_AGRESSION) {
								if (isDebug)
									return;
								int honor = P.get_honor() - 500;
								if (honor < 0)
									honor = 0;
								P.set_honor(honor);
								if (P.isOnline())
									SocketManager.GAME_SEND_Im_PACKET(P, "076;" + honor);
							}

							// On le supprime de la team
							if (_type == Constant.FIGHT_TYPE_PVT || _type == Constant.FIGHT_TYPE_PVM
									|| _type == Constant.FIGHT_TYPE_CONQUETE) {
								if (_team0.containsKey(F.getGUID())) {
									F._cell.removeFighter(F);
									_team0.remove(F.getGUID());
								} else if (_team1.containsKey(F.getGUID())) {
									F._cell.removeFighter(F);
									_team1.remove(F.getGUID());
								}
							}
							try {
								Thread.sleep(1000);
							} catch (Exception E) {
							}
							;

							/*
							 * if(Energy == 0) { P.set_Ghosts(); }else {
							 */
							P.warpToSavePos();
							P.set_PDV(1);
							// }
						}

						if (P.isOnline()) {
							try {
								Thread.sleep(500);
							} catch (Exception E) {
							}
							;
							SocketManager.GAME_SEND_GV_PACKET(P);
							P.refreshMapAfterFight();
						}

						// si c'était a son tour de jouer
						if (_ordreJeu == null || _ordreJeu.size() <= _curPlayer)
							return;
						if (_ordreJeu.get(_curPlayer) == null)
							return;
						if (_ordreJeu.get(_curPlayer).getGUID() == F.getGUID()) {
							endTurn(false);
						}
					}
				} else if (_state == Constant.FIGHT_STATE_PLACE) {
					boolean isValid1 = false;
					if (T != null) {
						if (_init0 != null && _init0.getPersonnage() != null) {
							if (F.getPersonnage().get_GUID() == _init0.getPersonnage().get_GUID()) {
								isValid1 = true;
							}
						}
						if (_init1 != null && _init1.getPersonnage() != null) {
							if (F.getPersonnage().get_GUID() == _init1.getPersonnage().get_GUID()) {
								isValid1 = true;
							}
						}
					}

					if (isValid1)// Celui qui fait l'action a lancer le combat
									// et leave un autre personnage
					{
						if ((T.getTeam() == F.getTeam()) && (T.getGUID() != F.getGUID())) {

							System.out.println("EXLUSION DE : " + T.getPersonnage().get_name());
							SocketManager.GAME_SEND_ON_FIGHTER_KICK(this, T.getPersonnage().get_GUID(),
									getTeamID(T.getGUID()));
							if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CHALLENGE
									|| _type == Constant.FIGHT_TYPE_PVT)
								SocketManager.GAME_SEND_ON_FIGHTER_KICK(this, T.getPersonnage().get_GUID(),
										getOtherTeamID(T.getGUID()));
							Characters P = T.getPersonnage();
							P.set_duelID(-1);
							P.set_ready(false);
							P.fullPDV();
							P.set_fight(null);
							P.setSitted(false);
							P.set_away(false);

							if (P.isOnline()) {
								try {
									Thread.sleep(500);
								} catch (Exception E) {
								}
								;
								SocketManager.GAME_SEND_GV_PACKET(P);
								P.refreshMapAfterFight();
							}

							// On le supprime de la team
							if (_team0.containsKey(T.getGUID())) {
								T._cell.removeFighter(T);
								_team0.remove(T.getGUID());
							} else if (_team1.containsKey(T.getGUID())) {
								T._cell.removeFighter(T);
								_team1.remove(T.getGUID());
							}
							for (Characters z : _mapOld.getPersos())
								FightStateAddFlag(this._mapOld, z);
						}
					} else if (T == null)// Il leave de son plein gré donc (T =
											// null)
					{
						boolean isValid2 = false;
						if (_init0 != null && _init0.getPersonnage() != null) {
							if (F.getPersonnage().get_GUID() == _init0.getPersonnage().get_GUID()) {
								isValid2 = true;
							}
						}
						if (_init1 != null && _init1.getPersonnage() != null) {
							if (F.getPersonnage().get_GUID() == _init1.getPersonnage().get_GUID()) {
								isValid2 = true;
							}
						}

						if (isValid2)// Soit il a lancer le combat => annulation
										// du combat
						{
							for (Fighter f : this.getFighters(F.getTeam2())) {
								Characters P = f.getPersonnage();
								P.set_duelID(-1);
								P.set_ready(false);
								P.fullPDV();
								P.set_fight(null);
								P.setSitted(false);
								P.set_away(false);

								if (F.getPersonnage().get_GUID() != f.getPersonnage().get_GUID())// Celui
																									// qui
																									// a
																									// join
																									// le
																									// fight
																									// revient
																									// sur
																									// la
																									// map
								{
									if (P.isOnline()) {
										try {
											Thread.sleep(200);
										} catch (Exception E) {
										}
										;
										SocketManager.GAME_SEND_GV_PACKET(P);
										P.refreshMapAfterFight();
									}
								} else// Celui qui a fait le fight meurt + perte
										// honor
								{
									if (_type == Constant.FIGHT_TYPE_AGRESSION
											|| _type == Constant.FIGHT_TYPE_CONQUETE) {
										startFight();
										onFighterDie(F, F);
										verifIfTeamAllDead();
										F.setLeft(true);
										// si le combat n'est pas terminé
										if (_state == Constant.FIGHT_STATE_ACTIVE) {
											SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_map, F.getGUID());
											if (perso.isOnline()) {
												SocketManager.GAME_SEND_GV_PACKET(perso);
											}
											// si c'était a son tour de jouer
											if (_ordreJeu.get(_curPlayer) == null)
												return;
											if (_ordreJeu.get(_curPlayer).getGUID() == F.getGUID()) {
												endTurn(false);
											}
										}
									} else if (_type == Constant.FIGHT_TYPE_PVM || _type == Constant.FIGHT_TYPE_PVT) {
										/*
										 * int EnergyLoos =
										 * Formulas.getLoosEnergy(P.get_lvl(),
										 * _type==1, _type==5); int Energy =
										 * P.get_energy() - EnergyLoos;
										 * if(Energy < 0) Energy = 0;
										 * P.set_energy(Energy);
										 * if(P.isOnline())
										 * SocketManager.GAME_SEND_Im_PACKET(P,
										 * "034;"+EnergyLoos);
										 * 
										 */
										try {
											Thread.sleep(1000);
										} catch (Exception E) {
										}
										;
										/*
										 * if(Energy == 0) { P.set_Ghosts();
										 * }else {
										 */
										P.warpToSavePos();
										P.set_PDV(1);
										// }
									}

									if (P.isOnline()) {
										try {
											Thread.sleep(200);
										} catch (Exception E) {
										}
										;
										SocketManager.GAME_SEND_GV_PACKET(P);
										P.refreshMapAfterFight();
									}
								}
							}
							setTimeStartTurn(0L);
							if (getActTimerTask() != null)
								getActTimerTask().cancel();
							setActTimerTask(null);
							if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CONQUETE
									|| _type == Constant.FIGHT_TYPE_CHALLENGE || _type == Constant.FIGHT_TYPE_PVT) {
								for (Fighter f : this.getFighters(F.getOtherTeam())) {
									if (f.getPersonnage() == null)
										continue;
									Characters P = f.getPersonnage();
									P.set_duelID(-1);
									P.set_ready(false);
									P.fullPDV();
									P.set_fight(null);
									P.setSitted(false);
									P.set_away(false);

									if (P.isOnline()) {
										try {
											Thread.sleep(200);
										} catch (Exception E) {
										}
										;
										SocketManager.GAME_SEND_GV_PACKET(P);
										P.refreshMapAfterFight();
									}
								}
							}
							_state = 4;// Nous assure de ne pas démarrer le combat
							if (_map == null)
								return;
							World.getCarte(_map.get_id()).removeFight(_id);
							SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(World.getCarte(_map.get_id()));
							SocketManager.GAME_SEND_GAME_REMFLAG_PACKET_TO_MAP(this._mapOld, _init0.getGUID());
							if (_type == Constant.FIGHT_TYPE_CONQUETE) {
								String str = _Prisme.getCarte() + "|" + _Prisme.getX() + "|" + _Prisme.getY();
								for (Characters z : World.getOnlinePersos()) {
									if (z == null)
										continue;
									if (z.get_align() != _Prisme.getalignement())
										continue;
									SocketManager.SEND_CS_SURVIVRE_MESSAGE_PRISME(z, str);
								}
								_Prisme.setInFight(-1);
								_Prisme.setFightID(-1);
								if (perso != null) {
									if (_Prisme != null)
										SocketManager.SEND_CP_INFO_DEFENSEURS_PRISME(perso, getDefenseurs());
								}
								for (Characters z : World.getCarte((short) _Prisme.getCarte()).getPersos()) {
									if (z == null)
										continue;
									SocketManager.SEND_GM_PRISME_TO_MAP(z.get_compte().getGameThread().get_out(),
											z.get_curCarte());
								}
							}
							if (_type == Constant.FIGHT_TYPE_PVT) {
								// On actualise la guilde + Message d'attaque FIXME
								for (Characters z : World.getGuild(_guildID).getMembers()) {
									if (z == null)
										continue;
									if (z.isOnline()) {
										SocketManager.GAME_SEND_gITM_PACKET(z, Collector.parsetoGuild(z.get_guild().get_id()));
										SocketManager.GAME_SEND_MESSAGE(z, "Votre percepteur remporte la victoire.", Config.CONFIG_MOTD_COLOR);
										//TODO Vérif si c'est utile d'envoyer le message
									}
								}
								_perco.set_inFight((byte) 0);
								_perco.set_inFightID((byte) -1);
								for (Characters z : World.getCarte((short) _perco.get_mapID()).getPersos()) {
									if (z == null)
										continue;
									if (z.get_compte() == null || z.get_compte().getGameThread() == null)
										continue;
									SocketManager.GAME_SEND_MAP_PERCO_GMS_PACKETS(z.get_compte().getGameThread().get_out(), z.get_curCarte());
								}
							}
							if (_type == Constant.FIGHT_TYPE_PVM) {
								if (_team1.size() > 0) {
									_team1.get(_team1.keySet().toArray()[0]).getMob().getTemplate().getAlign();
								}
							}
							_map = null;
							_ordreJeu = null;
						} else// Soit il a rejoin le combat => Left de lui seul
						{
							SocketManager.GAME_SEND_ON_FIGHTER_KICK(this, F.getPersonnage().get_GUID(),
									getTeamID(F.getGUID()));
							if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CONQUETE
									|| _type == Constant.FIGHT_TYPE_CHALLENGE || _type == Constant.FIGHT_TYPE_PVT)
								SocketManager.GAME_SEND_ON_FIGHTER_KICK(this, F.getPersonnage().get_GUID(),
										getOtherTeamID(F.getGUID()));
							Characters P = F.getPersonnage();
							P.set_duelID(-1);
							P.set_ready(false);
							P.fullPDV();
							P.set_fight(null);
							P.setSitted(false);
							P.set_away(false);

							if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CONQUETE
									|| _type == Constant.FIGHT_TYPE_PVM || _type == Constant.FIGHT_TYPE_PVT) {
								/*
								 * int EnergyLoos =
								 * Formulas.getLoosEnergy(P.get_lvl(), _type==1,
								 * _type==5); int Energy = P.get_energy() -
								 * EnergyLoos; if(Energy < 0) Energy = 0;
								 * P.set_energy(Energy); if(P.isOnline())
								 * SocketManager.GAME_SEND_Im_PACKET(P,
								 * "034;"+EnergyLoos);
								 */
								if (_type == Constant.FIGHT_TYPE_AGRESSION || _type == Constant.FIGHT_TYPE_CONQUETE) {
									if (isDebug)
										return;
									int honor = P.get_honor() - 500;
									if (honor < 0)
										honor = 0;
									P.set_honor(honor);
									if (P.isOnline())
										SocketManager.GAME_SEND_Im_PACKET(P, "076;" + honor);
								}

								try {
									Thread.sleep(1000);
								} catch (Exception E) {
								}
								;
								/*
								 * if(Energy == 0) { P.set_Ghosts(); }else {
								 */
								P.warpToSavePos();
								P.set_PDV(1);
								// }
							}

							if (P.isOnline()) {
								try {
									Thread.sleep(500);
								} catch (Exception E) {
								}
								;
								SocketManager.GAME_SEND_GV_PACKET(P);
								P.refreshMapAfterFight();
							}

							// On le supprime de la team
							if (_team0.containsKey(F.getGUID())) {
								F._cell.removeFighter(F);
								_team0.remove(F.getGUID());
							} else if (_team1.containsKey(F.getGUID())) {
								F._cell.removeFighter(F);
								_team1.remove(F.getGUID());
							}
							for (Characters z : _mapOld.getPersos())
								FightStateAddFlag(this._mapOld, z);
						}
					}
				} else {

					Logs.addToGameLog("Phase de combat non geree, type de combat:" + _type + " T:" + T + " F:" + F);
				}
				break;
			default:

				Logs.addToGameLog("Type de combat non geree, type de combat:" + _type + " T:" + T + " F:" + F);
				break;
			}
		} else// Si perso en spec
		{
			SocketManager.GAME_SEND_GV_PACKET(perso);
			_spec.remove(perso.get_GUID());
			perso.setSitted(false);
			perso.set_fight(null);
			perso.set_away(false);
		}
	}

	public String getGTL() {
		String packet = "GTL";
		for (Fighter f : get_ordreJeu()) {
			packet += "|" + f.getGUID();
		}
		return packet + (char) 0x00;
	}

	public int getNextLowerFighterGuid() {
		int g = -1;
		for (Fighter f : getFighters(3)) {
			if (f.getGUID() < g)
				g = f.getGUID();
		}
		g--;
		return g;
	}

	public void addFighterInTeam(Fighter f, int team) {
		if (team == 0)
			_team0.put(f.getGUID(), f);
		else if (team == 1)
			_team1.put(f.getGUID(), f);
	}

	public String parseFightInfos() {
		StringBuilder infos = new StringBuilder();
		infos.append(_id).append(";");
		long time = System.nanoTime() - get_startTime();
		infos.append((get_startTime() == 0 ? "-1" : time)).append(";");
		// Team1
		infos.append("0,");// 0 car toujours joueur :)
		switch (_type) {
		case Constant.FIGHT_TYPE_CHALLENGE:
			infos.append("0,");
			infos.append(_team0.size()).append(";");
			// Team2
			infos.append("0,");
			infos.append("0,");
			infos.append(_team1.size()).append(";");
			break;
			
		case Constant.FIGHT_TYPE_CONQUETE:
			infos.append(_init0.getPersonnage().get_align()).append(",");
			infos.append(_team0.size()).append(";");
			// Team 2
			infos.append("0,");
			infos.append(_Prisme.getalignement() + ",");
			infos.append(_team1.size() + ";");
			break;

		case Constant.FIGHT_TYPE_AGRESSION:
			infos.append(_init0.getPersonnage().get_align()).append(",");
			infos.append(_team0.size()).append(";");
			// Team2
			infos.append("0,");
			infos.append(_init1.getPersonnage().get_align()).append(",");
			infos.append(_team1.size()).append(";");
			break;

		case Constant.FIGHT_TYPE_PVM:
			infos.append("0,");
			infos.append(_team0.size()).append(";");
			// Team2
			infos.append("1,");
			infos.append(_team1.get(_team1.keySet().toArray()[0]).getMob().getTemplate().getAlign()).append(",");
			infos.append(_team1.size()).append(";");
			break;

		case Constant.FIGHT_TYPE_PVT:
			infos.append("0,");
			infos.append(_team0.size()).append(";");
			// Team2
			infos.append("4,");
			infos.append("0,");
			infos.append(_team1.size()).append(";");
			break;
		}
		return infos.toString();
	}

	public Fighter get_init1() {
		return _init1;
	}

	public void showCaseToTeam(int guid, int cellID) {
		int teams = getTeamID(guid) - 1;
		if (teams == 4)
			return;// Les spectateurs ne montrent pas
		ArrayList<GameSendThread> PWs = new ArrayList<GameSendThread>();
		if (teams == 0) {
			for (Entry<Integer, Fighter> e : _team0.entrySet()) {
				if (e.getValue().getPersonnage() != null
						&& e.getValue().getPersonnage().get_compte().getGameThread() != null)
					PWs.add(e.getValue().getPersonnage().get_compte().getGameThread().get_out());
			}
		} else if (teams == 1) {
			for (Entry<Integer, Fighter> e : _team1.entrySet()) {
				if (e.getValue().getPersonnage() != null
						&& e.getValue().getPersonnage().get_compte().getGameThread() != null)
					PWs.add(e.getValue().getPersonnage().get_compte().getGameThread().get_out());
			}
		}
		SocketManager.GAME_SEND_FIGHT_SHOW_CASE(PWs, guid, cellID);
	}

	public void showCaseToAll(int guid, int cellID) {
		ArrayList<GameSendThread> PWs = new ArrayList<GameSendThread>();
		for (Entry<Integer, Fighter> e : _team0.entrySet()) {
			if (e.getValue().getPersonnage() != null
					&& e.getValue().getPersonnage().get_compte().getGameThread() != null)
				PWs.add(e.getValue().getPersonnage().get_compte().getGameThread().get_out());
		}
		for (Entry<Integer, Fighter> e : _team1.entrySet()) {
			if (e.getValue().getPersonnage() != null
					&& e.getValue().getPersonnage().get_compte().getGameThread() != null)
				PWs.add(e.getValue().getPersonnage().get_compte().getGameThread().get_out());
		}
		for (Entry<Integer, Characters> e : _spec.entrySet()) {
			PWs.add(e.getValue().get_compte().getGameThread().get_out());
		}
		SocketManager.GAME_SEND_FIGHT_SHOW_CASE(PWs, guid, cellID);
	}

	public void joinAsSpect(Characters p) {
		if (p.get_fight() != null)// Le mec tente de nous arnaquer
		{
			return;
		}
		if ((!specOk && p.get_compte().get_gmLvl() == 0) || _state != Constant.FIGHT_STATE_ACTIVE) {
			SocketManager.GAME_SEND_Im_PACKET(p, "157");
			return;
		}
		p.get_curCell().removePlayer(p.get_GUID());
		SocketManager.GAME_SEND_GJK_PACKET(p, _state, 0, 0, 1, 0, _type);
		SocketManager.GAME_SEND_GS_PACKET(p);
		SocketManager.GAME_SEND_GTL_PACKET(p, this);
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(p.get_curCarte(), p.get_GUID());
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this, _map, p);
		SocketManager.GAME_SEND_GAMETURNSTART_PACKET(p, _ordreJeu.get(_curPlayer).getGUID(), Constant.TIME_BY_TURN);
		_spec.put(p.get_GUID(), p);
		p.set_fight(this);
		if (p.get_compte().get_gmLvl() == 0)
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 7, "036;" + p.get_name());
		if ((this._type == Constant.FIGHT_TYPE_PVM) && (this._challenges.size() > 0)) {
			for (Entry<Integer, Challenge> c : this._challenges.entrySet()) {
				if (c.getValue() == null)
					continue;
				SocketManager.send(p, c.getValue().parseToPacket());
				if (!c.getValue().getAlive()) {
					if (c.getValue().get_win())
						SocketManager.send(p, "GdOK" + c.getValue().getType());
					else
						SocketManager.send(p, "GdKO" + c.getValue().getType());
				}
			}
		}

	}

	public boolean verifyStillInFight()// Return true si au moins un joueur est
										// encore dans le combat
	{
		for (Fighter f : _team0.values()) {
			if (f.isPerco())
				return true;
			if (f.isInvocation() || f.isDead() || f.getPersonnage() == null || f.getMob() != null || f._double != null
					|| f.hasLeft()) {
				continue;
			}
			if (f.getPersonnage() != null && f.getPersonnage().get_fight() != null
					&& f.getPersonnage().get_fight().get_id() == this.get_id()) // Si
																				// il
																				// n'est
																				// plus
																				// dans
																				// ce
																				// combat
			{
				return true;
			}
		}
		for (Fighter f : _team1.values()) {
			if (f.isPerco())
				return true;
			if (f.isInvocation() || f.isDead() || f.getPersonnage() == null || f.getMob() != null || f._double != null
					|| f.hasLeft()) {
				continue;
			}
			if (f.getPersonnage() != null && f.getPersonnage().get_fight() != null
					&& f.getPersonnage().get_fight().get_id() == this.get_id()) // Si
																				// il
																				// n'est
																				// plus
																				// dans
																				// ce
																				// combat
			{
				return true;
			}
		}

		return false;
	}

	public boolean verifyStillInFightTeam(int guid)// Return true si au moins un
													// joueur est encore dans la
													// team
	{
		if (_team0.containsKey(guid)) {
			for (Fighter f : _team0.values()) {
				if (f.isPerco())
					return true;
				if (f.isInvocation() || f.isDead() || f.getPersonnage() == null || f.getMob() != null
						|| f._double != null || f.hasLeft()) {
					continue;
				}
				if (f.getPersonnage() != null && f.getPersonnage().get_fight() != null
						&& f.getPersonnage().get_fight().get_id() == this.get_id()) // Si
																					// il
																					// n'est
																					// plus
																					// dans
																					// ce
																					// combat
				{
					return true;
				}
			}
		} else if (_team1.containsKey(guid)) {
			for (Fighter f : _team1.values()) {
				if (f.isPerco())
					return true;
				if (!f.isInvocation() || f.isDead() || f.getPersonnage() == null || f.getMob() != null
						|| f._double != null || f.hasLeft()) {
					continue;
				}
				if (f.getPersonnage() != null && f.getPersonnage().get_fight() != null
						&& f.getPersonnage().get_fight().get_id() == this.get_id()) // Si
																					// il
																					// n'est
																					// plus
																					// dans
																					// ce
																					// combat
				{
					return true;
				}
			}
		}

		return false;
	}

	public static void FightStateAddFlag(Maps _map, Characters P) {
		for (Entry<Integer, Fight> fight : _map.get_fights().entrySet()) {
			if (fight.getValue()._state == Constant.FIGHT_STATE_PLACE) {
				if (fight.getValue()._type == Constant.FIGHT_TYPE_CHALLENGE) {
					SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(P,
							fight.getValue()._init0.getPersonnage().get_curCarte(), 0,
							fight.getValue()._init0.getGUID(), fight.getValue()._init1.getGUID(),
							fight.getValue()._init0.getPersonnage().get_curCell().getID(), "0;-1",
							fight.getValue()._init1.getPersonnage().get_curCell().getID(), "0;-1");
					for (Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet()) {

						System.out.println(F.getValue().getPersonnage().get_name());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P,
								fight.getValue()._init0.getPersonnage().get_curCarte(),
								fight.getValue()._init0.getGUID(), fight.getValue()._init0);
					}
					for (Entry<Integer, Fighter> F : fight.getValue()._team1.entrySet()) {

						System.out.println(F.getValue().getPersonnage().get_name());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P,
								fight.getValue()._init1.getPersonnage().get_curCarte(),
								fight.getValue()._init1.getGUID(), fight.getValue()._init1);
					}
				} else if (fight.getValue()._type == Constant.FIGHT_TYPE_AGRESSION) {
					SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(P,
							fight.getValue()._init0.getPersonnage().get_curCarte(), 0,
							fight.getValue()._init0.getGUID(), fight.getValue()._init1.getGUID(),
							fight.getValue()._init0.getPersonnage().get_curCell().getID(),
							"0;" + fight.getValue()._init0.getPersonnage().get_align(),
							fight.getValue()._init1.getPersonnage().get_curCell().getID(),
							"0;" + fight.getValue()._init1.getPersonnage().get_align());
					for (Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet()) {

						System.out.println(F.getValue().getPersonnage().get_name());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P,
								fight.getValue()._init0.getPersonnage().get_curCarte(),
								fight.getValue()._init0.getGUID(), fight.getValue()._init0);
					}
					for (Entry<Integer, Fighter> F : fight.getValue()._team1.entrySet()) {

						System.out.println(F.getValue().getPersonnage().get_name());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P,
								fight.getValue()._init1.getPersonnage().get_curCarte(),
								fight.getValue()._init1.getGUID(), fight.getValue()._init1);
					}
				} else if (fight.getValue()._type == Constant.FIGHT_TYPE_PVM) {
					SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(P,
							fight.getValue()._init0.getPersonnage().get_curCarte(), 4,
							fight.getValue()._init0.getGUID(), fight.getValue()._mobGroup.getID(),
							(fight.getValue()._init0.getPersonnage().get_curCell().getID() + 1), "0;-1",
							fight.getValue()._mobGroup.getCellID(), "1;-1");
					for (Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet()) {

						System.out.println("PVM1: " + F.getValue().getPersonnage().get_name());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P,
								fight.getValue()._init0.getPersonnage().get_curCarte(),
								fight.getValue()._init0.getGUID(), fight.getValue()._init0);
					}
					for (Entry<Integer, Fighter> F : fight.getValue()._team1.entrySet()) {

						System.out.println("PVM2: " + F.getValue());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._map,
								fight.getValue()._mobGroup.getID(), F.getValue());
					}
				} else if (fight.getValue()._type == Constant.FIGHT_TYPE_PVT) {
					SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(P,
							fight.getValue()._init0.getPersonnage().get_curCarte(), 5,
							fight.getValue()._init0.getGUID(), fight.getValue()._perco.getGuid(),
							(fight.getValue()._init0.getPersonnage().get_curCell().getID() + 1), "0;-1",
							fight.getValue()._perco.get_cellID(), "3;-1");
					for (Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet()) {

						System.out.println("PVT1: " + F.getValue().getPersonnage().get_name());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P,
								fight.getValue()._init0.getPersonnage().get_curCarte(),
								fight.getValue()._init0.getGUID(), fight.getValue()._init0);
					}
					for (Entry<Integer, Fighter> F : fight.getValue()._team1.entrySet()) {

						System.out.println("PVT2: " + F.getValue());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._map,
								fight.getValue()._perco.getGuid(), F.getValue());
					}
				} else if (fight.getValue()._type == Constant.FIGHT_TYPE_CONQUETE) {
					SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(P,
							fight.getValue()._init0.getPersonnage().get_curCarte(), 0,
							fight.getValue()._init0.getGUID(), fight.getValue()._Prisme.getID(),
							fight.getValue()._init0.getPersonnage().get_curCell().getID(),
							"0;" + fight.getValue()._init0.getPersonnage().get_align(),
							fight.getValue()._Prisme.getCell(), "0;" + fight.getValue()._Prisme.getalignement());
					SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P,
							fight.getValue()._init0.getPersonnage().get_curCarte(), fight.getValue()._init0.getGUID(),
							fight.getValue()._init0);
					for (Entry<Integer, Fighter> F : fight.getValue()._team1.entrySet()) {
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._map,
								fight.getValue()._Prisme.getID(), F.getValue());
					}
				}
			}
		}
	}

	public static int getFightIDByFighter(Maps _map, int guid) {
		for (Entry<Integer, Fight> fight : _map.get_fights().entrySet()) {
			for (Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet()) {
				if (F.getValue().getPersonnage() != null && F.getValue().getGUID() == guid) {
					return fight.getValue().get_id();
				}
			}
		}
		return 0;
	}

	public Map<Integer, Fighter> getDeadList() {
		return deadList;
	}

	public void delOneDead(Fighter target) {
		deadList.remove(target.getGUID());
	}

	public void setTimeStartTurn(long a) {
		this.TimeStartTurn = a;
	}

	public long getTimeStartTurn() {
		return TimeStartTurn;
	}

	public void set_startTime(long _startTime) {
		this._startTime = _startTime;
	}

	public long get_startTime() {
		return _startTime;
	}

	public void setActTimerTask(TimerTask actTimerTask) {
		this.actTimerTask = actTimerTask;
	}

	public TimerTask getActTimerTask() {
		return actTimerTask;
	}

	public boolean HasUsedCoopTranspo() {
		return hasUsedCoopTranspo;
	}

	public void setHasUsedCoopTranspo(boolean hasUsedCoopTranspo) {
		this.hasUsedCoopTranspo = hasUsedCoopTranspo;
	}

	public Object getPrisme() {
		return _Prisme;
	}

}

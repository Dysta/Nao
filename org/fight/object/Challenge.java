package org.fight.object;

import java.util.ArrayList;
import java.util.List;

import org.client.Characters;
import org.common.Formulas;
import org.common.Pathfinding;
import org.common.SocketManager;
import org.fight.Fight;
import org.fight.Fight.Fighter;

public class Challenge
{
  private int _type;
  private Fight _fight;
  private boolean _challengeAlive = false;
  private boolean _challengeOk = false;
  private int _gainXp;
  private int _gainDrop;
  private int _args = -1;
  private Fighter _cible;
  private String _lastActions;
  private String _arguments = new String();
  private long _lastActions_time;
  private List<Fight.Fighter> _ordreJeu = new ArrayList<Fighter>();

  public Challenge(Fight fight, int challengeType, int gainXp, int gainDrop)
  {
    _lastActions = "";
    _lastActions_time = System.currentTimeMillis();

    _challengeAlive = true;
    _fight = fight;
    _type = challengeType;
    _gainXp = gainXp;
    _gainDrop = gainDrop;

    _ordreJeu.clear();
    _ordreJeu.addAll(fight.get_ordreJeu());
  }

	public int get_gainXp() {
		return _gainXp;
	}
	
	public int get_gainDrop() {
		return _gainDrop;
	}
	
	public boolean get_win() {
		return _challengeOk;
	}

	public void onFight_end() {
		if (!_challengeAlive)
			return;
		switch(_type) {
		case 44: // Partage
		case 46: // Chacun son monstre
			for(Fighter fighter : _fight.getFighters(1))
				if(!_arguments.contains(";"+fighter.getGUID()+";")) {
					challenge_Foirer(fighter);
					break;
				}
			break;
	
		}
		challenge_Gagner();
	}

	public void show_cibleToPerso(Characters p) {
		if (!_challengeAlive || _cible == null || _cible.get_fightCell() == null || p == null)
			return;
		StringBuilder packet = new StringBuilder();
		packet.append("Gf").append(_cible.getGUID()).append("|").append(_cible.get_fightCell().getID());
		SocketManager.send(	p, packet.toString());
	}

	public void show_cibleToFight() {
		if (!_challengeAlive || _cible == null || _cible.get_fightCell() == null)
			return;
		StringBuilder packet = new StringBuilder();
		packet.append("Gf").append(_cible.getGUID()).append("|").append(_cible.get_fightCell().getID());
		SocketManager.GAME_SEND_PACKET_TO_FIGHT(_fight, 7, packet.toString());
	}
	
	public String parseToPacket() {
		StringBuilder packet = new StringBuilder();
		packet.append("Gd");
		packet.append(_type).append(";");
		packet.append(_cible != null ? "1" : "0").append(";");
		packet.append(_cible != null ? (Integer.valueOf(_cible.getGUID())) : "").append(";");
		packet.append(_gainXp).append(";0;");
		packet.append(_gainDrop).append(";0;");
		if (!_challengeAlive)
			if (_challengeOk)
				packet.append("").append(_type);
			else
				packet.append("").append(_type);
		return packet.toString();
	}

	public void challenge_Gagner() {
		_challengeOk = true;
		_challengeAlive = false;
	    SocketManager.GAME_SEND_PACKET_TO_FIGHT(_fight, 7, "GdOK" + _type);
	}
	public int getType()
	{
		return _type;
	}
	public boolean getAlive()
	{
		return _challengeAlive;
	}
	public void challenge_Foirer(Fighter graceAqui) {
		String nom = "";
		try {
			nom = graceAqui.getPersonnage().get_name();
		} catch (Exception exception) {}
		_challengeAlive = false;
		SocketManager.GAME_SEND_PACKET_TO_FIGHT(_fight, 7, "GdKO" + _type);
	    SocketManager.GAME_SEND_PACKET_TO_FIGHT(_fight, 7, "Im0188;" + nom);
	}

	  public void onFight_start()
	  {
	    if (!_challengeAlive) return;
	    switch (_type)
	    {
	    case 3: // Désigné Volontaire
	    case 4: // Sursis
	    case 32: // Elitiste
	    case 35: // Tueur à gages
	      try {
	        int noBoucle = 0;
	        while (_cible == null)
	          if (_ordreJeu.size() > 0) {
	            Fight.Fighter f = (Fight.Fighter)_ordreJeu.get(Formulas.getRandomValue(0, _ordreJeu.size() - 1));
	            if (f.getPersonnage() == null)
	              _cible = f;
	            noBoucle++;
	            if (noBoucle > 30) return;
	          }
	        show_cibleToFight();
	        } catch (Exception localException) {}
	      break;
	    case 10 : // Cruel
	    	try {
	    		int levelMin = 2000;
	    		for(Fighter fighter : _fight.getFighters(2)) {
	    			if(fighter.getPersonnage() == null && fighter.get_lvl() < levelMin) {
	    				levelMin = fighter.get_lvl();
	    				_cible = fighter;
	    			}
	    		}
	    		if(!(_cible == null))
	    	        show_cibleToFight();
	    	} catch (Exception localException) {}
	    	break;
	    case 25 : // Ordonné
	    	try {
	    		int levelMax = 0;
	    		for(Fighter fighter : _fight.getFighters(2)) {
	    			if(fighter.getPersonnage() == null && fighter.get_lvl() > levelMax) {
	    				levelMax = fighter.get_lvl();
	    				_cible = fighter;
	    			}
	    		}
	    		if(!(_cible == null))
	    	        show_cibleToFight();
	    	} catch (Exception localException) {}
	    	break;
	    }
	  }
	  
	public void onFighter_die(Fighter fighter) {
		if(!_challengeAlive)
			return;
		switch (_type) {
		case 33: // survivant
			challenge_Foirer(_fight.getCurFighter());
			break;			
			
		case 49: // Protégez vos mules
			int lvlMin = 5000;
			for(Fighter f : _fight.getFighters(1))
				if(f.get_lvl() < lvlMin)
					lvlMin = f.get_lvl();
			if(fighter.get_lvl() <= lvlMin) {
				challenge_Foirer(_fight.getCurFighter());
				break;
			}
							
			
			break;
		}
	}
	
	public void onFighters_attacked(ArrayList<Fighter> targets, Fighter caster, int effectID) {
		if (!_challengeAlive)
			return;
		String DamagingEffects = "|82|85|86|87|88|89|91|92|93|94|95|96|97|98|99|100|141|";
		//String WaterEffects = "|85|91|96|";
		//String EarthEffects = "|86|92|97|";
		//String WindEffects = "|87|93|98|";
		//String FireEffects = "|88|94|99|";
		//String NeutralEffects = "|89|95|100|";
		String HealingEffects = "|108|";
		String MPEffects = "|77|127|";
		String APEffects = "|84|101|";
		String OPEffects = "|116|320|";
		int eID = effectID;
		switch(_type) {
		case 17: // Intouchable
			if(DamagingEffects.contains("|"+effectID+"|"))
				for(Fighter target : targets)
					if(target.getTeam() == 0 && !target.isInvocation()) {
						challenge_Foirer(caster);
						break;
					}
			break;
			
		case 18: // Incurable
			if((caster.getTeam() == 0) && !caster.isInvocation() && HealingEffects.contains("|"+effectID+"|"))
				challenge_Foirer(caster);
			break;
			
		case 19: // Configs propres
			if((caster.getTeam() == 0) && DamagingEffects.contains("|"+effectID+"|"))
				for(Fighter target : targets)
					if(target.getTeam() == 1 && !target.isInvocation()) {
						challenge_Foirer(caster);
						break;
					}
			break;
			
		case 20: // Elémentaire
			if((caster.getTeam() == 0) && DamagingEffects.contains("|"+effectID+"|") && effectID != 141) {
				eID -= 96; // 0 à 4 selon l'élément
				if(eID >= 0) {
					if(_args == -1) // s'il n'y a pas eu d'élément encore
						_args = eID;
					else if(_args != eID) // si l'élément déjà présent est différent
						challenge_Foirer(caster);
				} else eID += 5;
				if(eID >= 0) {
					if(_args == -1)
						_args = eID;
					else if(_args != eID)
						challenge_Foirer(caster);
				} else eID += 6;
				if(eID >= 0) {
					if(_args == -1)
						_args = eID;
					else if(_args != eID)
						challenge_Foirer(caster);
				}
			}				
			break;
			
		case 21: // Circulez !
			if((caster.getTeam() == 0) && MPEffects.contains("|"+effectID+"|"))
				for(Fighter target : targets)
					if(target.getTeam() == 1) {
						challenge_Foirer(caster);
						break;
					}
			break;
			
		case 22: // Le temps qui court !
			if((caster.getTeam() == 0) && APEffects.contains("|"+effectID+"|"))
				for(Fighter target : targets)
					if(target.getTeam() == 1) {
						challenge_Foirer(caster);
						break;
					}
			break;
			
		case 23: // Perdu de vue !
			if((caster.getTeam() == 0) && OPEffects.contains("|"+effectID+"|"))
				for(Fighter target : targets)
					if(target.getTeam() == 1) {
						challenge_Foirer(caster);
						break;
					}
			break;
			
		case 31: // Focus
			if((caster.getTeam() == 0) && DamagingEffects.contains("|"+effectID+"|"))
				for(Fighter target : targets)
					if(target.getTeam() == 1)
						if(_arguments.isEmpty())
							_arguments += ""+target.getGUID();
						else if(!_arguments.contains(""+target.getGUID()))
							challenge_Foirer(caster);
			break;
			
		case 32: // Elitiste
		case 34: // Imprévisible
			if((caster.getTeam() == 0) && DamagingEffects.contains("|"+effectID+"|"))
				for(Fighter target : targets)
					if(target.getTeam() == 1)
						if(_cible == null || _cible.getGUID() != target.getGUID())
							challenge_Foirer(caster);
			break;
			
		case 38: // Blitzkrieg
			if((caster.getTeam() == 0) && DamagingEffects.contains("|"+effectID+"|"))
				for(Fighter target : targets)
					if(target.getTeam() == 1) {
						StringBuilder ID = new StringBuilder();
						ID.append(";").append(target.getGUID()).append(",");
						if(!_arguments.contains(ID.toString())) {
							ID.append(caster.getGUID());
							_arguments += ID.toString();
						}
					}
			
		case 43: // Abnégation
			if((caster.getTeam() == 0) && HealingEffects.contains("|"+effectID+"|") && caster.getInvocator() == null)
				for(Fighter target : targets)
					if(target.getGUID() == caster.getGUID())
						challenge_Foirer(caster);
			break;
			
		case 45: // Duel
		case 46: // Chacun son monstre
			if((caster.getTeam() == 0) && DamagingEffects.contains("|"+effectID+"|"))
				for(Fighter target : targets)
					if(target.getTeam() == 1)
						if(!_arguments.contains(";"+target.getGUID()+","))
							_arguments += ";"+target.getGUID()+","+caster.getGUID()+";";
						else if(_arguments.contains(";"+target.getGUID()+",") && !_arguments.contains(";"+target.getGUID()+","+caster.getGUID()+";"))
							challenge_Foirer(target);
			break;
			
		case 47: // Contamination
			if(DamagingEffects.contains("|"+effectID+"|"))
				for(Fighter target : targets)
					if(target.getTeam() == 0) {
						if(!_arguments.contains(";"+target.getGUID()+","))
							_arguments += ";"+target.getGUID()+","+"3;";
					}
			break;
		}
	}

	public void onMob_die(Fighter mob, Fighter killer) {
		
		boolean isKiller = (killer.getGUID() == mob.getGUID() ? false : true);
		if (!_challengeAlive)
			return;

		switch (_type) {
		case 3: // Désigné Volontaire
			if (_cible == null)
				return;
			
			if (_cible.getGUID() != mob.getGUID()) {
				challenge_Foirer(_fight.getCurFighter());
			} else {
				challenge_Gagner();
			}
			_cible = null;
			break;
		
		
		case 4: // Sursis
			if(_cible == null)
				return;
			
			if(_cible.getGUID() == mob.getGUID() && !_fight.verifIfTeamIsDead()) {
				challenge_Foirer(_fight.getCurFighter());
			}
			break;
			
		case 28: // Ni Pioutes ni Soumises
			if(isKiller && killer.getPersonnage() != null)
				if(killer.getPersonnage().get_sexe() == 0) {
					challenge_Foirer(_fight.getCurFighter());
				}
			break;
		
		case 29: // Ni Pious ni Soumis
			if(isKiller && killer.getPersonnage() != null)
				if(killer.getPersonnage().get_sexe() == 1) {
					challenge_Foirer(_fight.getCurFighter());
				}
			break;
			
		case 31: // Focus
			if(_arguments.contains(""+mob.getGUID()))
				_arguments = "";
			else
				challenge_Foirer(killer);
			break;
			
		case 32: // Elitiste
			if(_cible.getGUID() == mob.getGUID())
				challenge_Gagner();
			break;
			
		case 34: // Imprévisible
			_cible = null;

		case 42: // Deux pour le prix d'un
		case 44: // Partage
		case 46: // Chacun son monstre
			if(isKiller)
				_arguments += ";"+killer.getGUID()+";";		
			break;
		case 30: // Les petits d'abord
		case 48: // Les mules d'abord
			if(isKiller) {
				int lvlMin = 5000;
				for(Fighter f : _fight.getFighters(1))
					if(f.get_lvl() < lvlMin)
						lvlMin = f.get_lvl();
				if(killer.get_lvl() > lvlMin) {
					challenge_Foirer(_fight.getCurFighter());
				}
			}
			break;
			
		case 35: // Tueur à gages
	    	if (_cible == null)
				return;
			
			if (_cible.getGUID() != mob.getGUID()) {
				challenge_Foirer(_fight.getCurFighter());
			}
			try {
				int noBoucle = 0, GUID = 0;
				_cible = null;
				while (_cible == null)
					if (_ordreJeu.size() > 0) {
						GUID = Formulas.getRandomValue(0, _ordreJeu.size() - 1);
						Fight.Fighter f = _ordreJeu.get(GUID);
						if (f.getPersonnage() == null && !f.isDead())
							_cible = f;
						noBoucle++;
						if (noBoucle > 150) return;
					}
				show_cibleToFight();
	        	} catch (Exception localException) {}
	        break;	    
	    
	    case 10 : // Cruel
	    	if (_cible == null)
				return;
			
			if (_cible.getGUID() != mob.getGUID()) {
				challenge_Foirer(_fight.getCurFighter());
			}
	    	try {
	    		int levelMin = 2000;
	    		for(Fighter fighter : _fight.getFighters(2)) {
	    			if(fighter.isDead()) 
	    				continue;
	    			if(fighter.getPersonnage() == null && fighter.get_lvl() < levelMin) {
	    				levelMin = fighter.get_lvl();
	    				_cible = fighter;
	    			}
	    		}
	    		if(!(_cible == null))
	    	        show_cibleToFight();
	    	} catch (Exception localException) {}
	    	break;
	    
	    
	    
	    case 25 : // Ordonné
	    	if (_cible == null)
				return;
			
			if (_cible.getGUID() != mob.getGUID()) {
				challenge_Foirer(_fight.getCurFighter());
			}
	    	try {
	    		int levelMax = 0;
	    		for(Fighter fighter : _fight.getFighters(2)) {
	    			if(fighter.isDead()) 
	    				continue;
	    			if(fighter.getPersonnage() == null && fighter.get_lvl() > levelMax) {
	    				levelMax = fighter.get_lvl();
	    				_cible = fighter;
	    			}
	    		}
	    		if(!(_cible == null))
	    	        show_cibleToFight();
	    	} catch (Exception localException) {}
	    	break;
		}
		return;
	}

	public void onPlayer_move(Fighter fighter)
	{
		if(!_challengeAlive)
			return;
		
		switch(_type)
		{
		case 1: // Zombie
			if(fighter.getPM() - fighter.getCurPM(_fight) > 1)
				challenge_Foirer(_fight.getCurFighter());
			break;
		}
		return;
	}
	
	public void onPlayer_action(Fighter fighter, int actionID)
	{
		if(!_challengeAlive || fighter.getTeam() == 1)
			return;
		if(System.currentTimeMillis() - _lastActions_time < 500)// on evite les doublons
			return;
		
		_lastActions_time = System.currentTimeMillis();
		StringBuilder action = new StringBuilder();
		action.append(";").append(fighter.getGUID());
		action.append(",").append(actionID).append(";");
		switch(_type) {
		case 6: // Versatile
		case 5: // Econome
			if(_lastActions.contains(action.toString())) 
				challenge_Foirer(_fight.getCurFighter());
			_lastActions += action.toString();
			break;
			
		case 24: // Borné
			if(!_lastActions.contains(action.toString()) && _lastActions.contains(";"+fighter.getGUID()+",")) 
				challenge_Foirer(_fight.getCurFighter());
			_lastActions += action.toString();
			break;
			
		}
		return;
		
	}
	
	public void onPlayer_cac(Fighter fighter) {
		
		if (!_challengeAlive)
			return;
		
		switch (_type) {
		case 11: // Mystique			
			challenge_Foirer(_fight.getCurFighter());
			break;
		case 6: // Versatile
		case 5: // Econome
			if(System.currentTimeMillis() - _lastActions_time < 500)// on evite les doublons
				return;
			
			_lastActions_time = System.currentTimeMillis();
			StringBuilder action = new StringBuilder();
			action.append(";").append(fighter.getGUID());
			action.append(",").append("cac").append(";");
			if(_lastActions.contains(action.toString())) 
				challenge_Foirer(_fight.getCurFighter());
			_lastActions += action.toString();
			break;				
		}
		return;
	}

	public void onPlayer_spell(Fighter fighter) {

		if (!_challengeAlive)
			return;
		
		switch (_type) {
		case 9: // Barbare
			challenge_Foirer(_fight.getCurFighter());
			break;
		}
		return;
	}

	public void onPlayer_startTurn(Fighter fighter) {
		
		if (!_challengeAlive)
			return;
		
		
		switch (_type) {
		case 2: // Statue
			_args = fighter.get_fightCell().getID();
		break;

		case 6: // Versatile
			_lastActions = "";
		break;
		
		case 34: // Imprévisible
			if(fighter.getTeam() == 1)
				return;
			try {
				int noBoucle = 0, GUID = 0;
				_cible = null;
				while (_cible == null)
					if (_ordreJeu.size() > 0) {
						GUID = Formulas.getRandomValue(0, _ordreJeu.size() - 1);
						Fighter f = _ordreJeu.get(GUID);
						if (f.getPersonnage() == null && !f.isDead())
							_cible = f;
						noBoucle++;
						if (noBoucle > 150) return;
					}
				show_cibleToFight();
	        	} catch (Exception localException) {}
	        break;
	        
		case 38: // Blitzkrieg
			if(fighter.getTeam() == 1 && _arguments.contains(";"+fighter.getGUID()+",")) {
				// on récupère le premier fighter qui l'a attaqué
				String[] str = _arguments.split(";");
				int fighterID = 0;
				for(String string : str) {
					if(string.contains(""+fighter.getGUID())) {
						for(String test : string.split(",")) {
							fighterID = Integer.parseInt(test);
						}
						break;
					}
				}
				for(Fighter f : _fight.getFighters(1))
					if(f.getGUID() == fighterID)
						challenge_Foirer(f);
			}
			break;
		
		case 47: // Contamination
			if(fighter.getTeam() == 0) {
				String str = ";"+fighter.getGUID()+",";
				if(_arguments.contains(str+"1;"))
					challenge_Foirer(fighter);
				else if(_arguments.contains(str+"2;"))
					_arguments += str+"1;";
				else if(_arguments.contains(str+"3;"))
					_arguments += str+"2;";

			}
			break;
		}
		return;
	}

	public void onPlayer_endTurn(Fighter fighter)
	{
		boolean hasFailed = false;
		if(!_challengeAlive)
			return;
		ArrayList<Fighter> Neighbours = new ArrayList<Fighter>();
		Neighbours = Pathfinding.getFightersAround(fighter.get_fightCell().getID(), _fight.get_map(),_fight);
		switch(_type) {
			case 1: // Zombie
				int diff = fighter.getPM() - fighter.getCurPM(_fight);
				if(diff > 1 || diff < 1)
					challenge_Foirer(fighter);
				break;
				
			case 2: // Statue
				if(fighter.get_fightCell().getID() != _args)
					challenge_Foirer(fighter);
				break;
				
			case 7: // Jardinier (sort #367)
				if(fighter.getPersonnage() != null) {
					if(fighter.canLaunchSpell(367))
						challenge_Foirer(fighter);
				}
				break;
				
			case 8: // Nomade
				if(fighter.getCurPM(_fight) != 0)
					challenge_Foirer(fighter);
				break;
				
			case 12: // Fossoyeur (sort #373)
				if(fighter.getPersonnage() != null) {
					if(fighter.canLaunchSpell(373))
						challenge_Foirer(fighter);
				}
				break;
				
			case 14: // Casino Royal (sort #101)
				if(fighter.getPersonnage() != null) {
					if(fighter.canLaunchSpell(101))
						challenge_Foirer(fighter);
				}
				break;
			
			case 15: // Araknophile (sort #370)
				if(fighter.getPersonnage() != null) {
					if(fighter.canLaunchSpell(370))
						challenge_Foirer(fighter);
				}
				break;
				
			case 36 : // Hardi
				hasFailed = true;
				if(!Neighbours.isEmpty())
					for(Fighter f : Neighbours)
						if(f.getTeam() != fighter.getTeam())
							hasFailed = false;
				break;
				
			case 37 : // Collant
				hasFailed = true;
				if(!Neighbours.isEmpty())
					for(Fighter f : Neighbours)
						if(f.getTeam() == fighter.getTeam())
							hasFailed = false;
				break;
			
			case 39 : // Anachorète
				if(!Neighbours.isEmpty())
					for(Fighter f : Neighbours)
						if(f.getTeam() == fighter.getTeam())
							challenge_Foirer(fighter);
				break;
				
			case 40 : // Pusillanime
				if(!Neighbours.isEmpty())
					for(Fighter f : Neighbours)
						if(f.getTeam() != fighter.getTeam())
							challenge_Foirer(fighter);
				break;
				
			case 41 : // Pétulant
				if(fighter.getCurPA(_fight) > 0)
					challenge_Foirer(fighter);
				break;
				
			case 42 : // Deux pour le prix d'un
				String GUID = ""+fighter.getGUID();
				int compteur = 0;
				for(String ID : _arguments.split(";"))
					if(ID.equals(GUID))
						compteur++;
				if(compteur == 2 || compteur == 0)
					_arguments = "";
				else
					challenge_Foirer(fighter);
				break;
				
				
			default:
				break;
		}
		if(hasFailed)
			challenge_Foirer(fighter);
		return;
	}
}

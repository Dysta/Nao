package org.object.job;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimerTask;
import java.util.TreeMap;

import javax.swing.Timer;

import org.client.Characters;
import org.common.Constant;
import org.common.Formulas;
import org.common.SocketManager;
import org.common.World;
import org.game.GameThread.GameAction;
import org.kernel.*;
import org.object.Objects;
import org.object.Maps.Case;
import org.object.Maps.InteractiveObject;
import org.object.Objects.ObjTemplate;
import org.spell.SpellEffect;

public class Job {

	public static class StatsMetier
	{
		private int _id;
		private Job _template;
		private int _lvl;
		private long _xp;
		private ArrayList<JobAction> _posActions = new ArrayList<JobAction>();
		private boolean _isCheap = false;
		private boolean _freeOnFails = false;
		private boolean _noRessource = false;
		private JobAction _curAction;
		private int max_case = 2;
		
		public StatsMetier(int id,Job tp,int lvl,long xp)
		{
			_id = id;
			_template = tp;
			_lvl = lvl;
			_xp = xp;
			_posActions = Constant.getPosActionsToJob(tp.getId(),lvl);
		}

		public int get_lvl() {
			return _lvl;
		}
		public boolean isCheap() {
			return _isCheap;
		}

		public void setIsCheap(boolean isCheap) {
			_isCheap = isCheap;
		}

		public boolean isFreeOnFails() {
			return _freeOnFails;
		}

		public void setFreeOnFails(boolean freeOnFails) {
			_freeOnFails = freeOnFails;
		}

		public boolean isNoRessource() {
			return _noRessource;
		}

		public void setNoRessource(boolean noRessource) {
			_noRessource = noRessource;
		}

		public void levelUp(Characters P,boolean send)
		{
			_lvl++;
			_posActions = Constant.getPosActionsToJob(_template.getId(),_lvl);
			
			if(send)
			{
				//on créer la listes des statsMetier a envoyer (Seulement celle ci)
				ArrayList<StatsMetier> list = new ArrayList<StatsMetier>();
				list.add(this);
				SocketManager.GAME_SEND_JS_PACKET(P, list);
				SocketManager.GAME_SEND_STATS_PACKET(P);
				SocketManager.GAME_SEND_Ow_PACKET(P);
				SocketManager.GAME_SEND_JN_PACKET(P,_template.getId(),_lvl);
				SocketManager.GAME_SEND_JO_PACKET(P, list);
			}
		}
		public String parseJS()
		{
			StringBuilder str = new StringBuilder();
			str.append("|").append(_template.getId()).append(";");
			boolean first = true;
			for(JobAction JA : _posActions)
			{
				if(!first)str.append(",");
				else first = false;
				str.append(JA.getSkillID()).append("~").append(JA.getMin()).append("~");
				if(JA.isCraft())str.append("0~0~").append(JA.getChance());
				else str.append(JA.getMax()).append("~0~").append(JA.getTime());
			}
			return str.toString();
		}
		public long getXp()
		{
			return _xp;
		}
		
		public void startAction(int id,Characters P,InteractiveObject IO,GameAction GA,Case cell)
		{
			for(JobAction JA : _posActions)
			{
				if(JA.getSkillID() == id)
				{
					_curAction = JA;
					JA.startAction(P,IO,GA,cell);
					return;
				}
			}
		}
		
		public void endAction(int id,Characters P,InteractiveObject IO,GameAction GA,Case cell)
		{
			if(_curAction == null)return;
			_curAction.endAction(P,IO,GA,cell);
			addXp(P,_curAction.getXpWin()*Config.XP_METIER);
			//Packet JX
			//on créer la listes des statsMetier a envoyer (Seulement celle ci)
			ArrayList<StatsMetier> list = new ArrayList<StatsMetier>();
			list.add(this);
			SocketManager.GAME_SEND_JX_PACKET(P, list);
		}
		
		public void addXp(Characters P,long xp)
		{
			if(_lvl >= 30 && P.get_compte().get_subscriber() == 0 && Config.USE_SUBSCRIBE)
			{
				SocketManager.PERSO_SEND_EXCHANGE_REQUEST_ERROR(P,'S');
				return;
			}
			if(_lvl >99)return;
			int exLvl = _lvl;
			_xp += xp;
			
			//Si l'xp dépasse le pallier du niveau suivant
			while(_xp >= World.getExpLevel(_lvl+1).metier && _lvl <100)
				levelUp(P,false);
			
			//s'il y a eu Up
			if(_lvl > exLvl && P.isOnline())
			{
				//on créer la listes des statsMetier a envoyer (Seulement celle ci)
				ArrayList<StatsMetier> list = new ArrayList<StatsMetier>();
				list.add(this);
				//on envoie le packet
				SocketManager.GAME_SEND_JS_PACKET(P, list);
				SocketManager.GAME_SEND_JN_PACKET(P,_template.getId(),_lvl);
				SocketManager.GAME_SEND_STATS_PACKET(P);
				SocketManager.GAME_SEND_Ow_PACKET(P);
				SocketManager.GAME_SEND_JO_PACKET(P, list);
				SocketManager.GAME_SEND_JX_PACKET(P, list);
			}
		}
		
		public String getXpString(String s)
		{
			StringBuilder str = new StringBuilder();
			str.append( World.getExpLevel(_lvl).metier).append(s);
			str.append(_xp).append(s);
			str.append(World.getExpLevel((_lvl<100?_lvl+1:_lvl)).metier);
			return str.toString();
		}
		public Job getTemplate() {
			
			return _template;
		}

		public int getOptBinValue()
		{
			int nbr = 0;
			nbr += (_isCheap?1:0);
			nbr += (_freeOnFails?2:0);
			nbr += (_noRessource?4:0);
			return nbr;
		}
		
		public boolean isValidMapAction(int id)
		{
			for(JobAction JA : _posActions)if(JA.getSkillID() == id) return true;
			return false;
		}
		
		public void setOptBinValue(int bin)
		{
			_isCheap = false;
			_freeOnFails = false;
			_noRessource = false;
			
			if(bin - 4 >=0)
			{
				bin -= 4;
				_isCheap = true;
			}
			if(bin - 2 >=0)
			{
				bin -=2;
				_freeOnFails = true;
			}
			if(bin - 1 >= 0)
			{
				bin -= 1;
				_noRessource = true;
			}
		}

		public int getID()
		{
			return _id;
		}

		/* Livre de métiers
		 * Par Taparisse
		 * Aidé par Nestrya !
		 */ 
		
		public void set_o(String[] pp) {
			System.out.println("pp2 :" + pp[1]);
			System.out.println("pp3 :" + pp[2]);
			setOptBinValue(Integer.parseInt(pp[1]));
			int cm = Constant.getTotalCaseByJobLevel(_lvl);
			if(cm <= Integer.parseInt(pp[2]))
				this.max_case = cm;
			max_case = Integer.parseInt(pp[2]);
		}
		
		public String parsetoJO(){
			return _id+"|"+getOptBinValue()+"|"+max_case;
		}
		
		public int getmaxcase() {
			return max_case;
		}
	}
	
	public static class JobAction
	{
		private int _skID;
		private int _min = 1;
		private int _max = 1;
		private boolean _isCraft;
		private int _chan = 100;
		private int _time = 0;
		private int _xpWin = 0;
		private long _startTime;
		private Map<Integer,Integer> _ingredients = new TreeMap<Integer,Integer>();
		private Map<Integer,Integer> _lastCraft = new TreeMap<Integer,Integer>();
		private Timer _craftTimer;
		private Characters _P;
		private String _data = "";
		private boolean _break = false;
		private boolean _broken = false;
		private int _reConfigingRunes = -1;
		private boolean _isRepeat = false;
		
		public JobAction(int sk,int min, int max,boolean craft, int arg,int xpWin)
		{
			_skID = sk;
			_min = min;
			_max = max;
			_isCraft = craft;
			if(craft)_chan = arg;
			else _time = arg;
			_xpWin = xpWin;
			
			_craftTimer = new Timer(100,new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					craft();
					_craftTimer.stop();
				}
			});
		}
		
		public void endAction(Characters P, InteractiveObject IO, GameAction GA,Case cell)
		{
			if(!_isCraft)
			{
				//Si recue trop tot, on ignore
				if(_startTime - System.currentTimeMillis() > 500)return;
				IO.setState(Constant.IOBJECT_STATE_EMPTY);
				IO.startTimer();
				//Packet GDF (changement d'état de l'IO)
				SocketManager.GAME_SEND_GDF_PACKET_TO_MAP(P.get_curCarte(), cell);
				
				boolean special = Formulas.getRandomValue(0, 99)==0;//Restriction de niveau ou pas ?
				
				//On ajoute X ressources
				int qua = (_max>_min?Formulas.getRandomValue(_min, _max):_min);
				int tID = Constant.getObjectByJobSkill(_skID,special);
								
				ObjTemplate T = World.getObjTemplate(tID);
				if(T == null)return;
				Objects O = T.createNewItem(qua, false, -1);
				//Si retourne true, on l'ajoute au monde
				if(P.addObjet(O, true))
					World.addObjet(O, true);
				SocketManager.GAME_SEND_IQ_PACKET(P,P.get_GUID(),qua);
				SocketManager.GAME_SEND_Ow_PACKET(P);
			}
		}

		public void startAction(Characters P, InteractiveObject IO, GameAction GA,Case cell)
		{
			_P = P;
			if(!_isCraft)
			{
				IO.setInteractive(false);
				IO.setState(Constant.IOBJECT_STATE_EMPTYING);
				SocketManager.GAME_SEND_GA_PACKET_TO_MAP(P.get_curCarte(),""+GA._id, 501, P.get_GUID()+"", cell.getID()+","+_time);
				SocketManager.GAME_SEND_GDF_PACKET_TO_MAP(P.get_curCarte(),cell);
				_startTime = System.currentTimeMillis()+_time;//pour eviter le cheat
			}else
			{
				P.set_away(true);
				IO.setState(Constant.IOBJECT_STATE_EMPTYING);//FIXME trouver la bonne valeur
				P.setCurJobAction(this);
				SocketManager.GAME_SEND_ECK_PACKET(P, 3, _min+";"+_skID);//_min => Nbr de Case de l'interface
				SocketManager.GAME_SEND_GDF_PACKET_TO_MAP(P.get_curCarte(), cell);
			}
		}

		public int getSkillID()
		{
			return _skID;
		}
		public int getMin()
		{
			return _min;
		}
		public int getXpWin()
		{
			return _xpWin;
		}
		public int getMax()
		{
			return _max;
		}
		public int getChance()
		{
			return _chan;
		}
		public int getTime()
		{
			return _time;
		}
		public boolean isCraft()
		{
			return _isCraft;
		}
		
		public void modifIngredient(Characters P,int guid, int qua)
		{
			//on prend l'ancienne valeur
			int q = _ingredients.get(guid)==null?0:_ingredients.get(guid);
			//on enleve l'entrée dans la Map
			_ingredients.remove(guid);
			//on ajoute (ou retire, en fct du signe) X objet
			q += qua;
			if(q > 0)
			{
				_ingredients.put(guid,q);
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(P,'O', "+", guid+"|"+q);
			}else SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(P,'O', "-", guid+"");
		}

		public void craft()
		{
			if(!_isCraft)return;
			boolean signed = false;//TODO
			try
			{
				Thread.sleep(750);
			}catch(Exception e){};
			//Si Forgemagie
			if(_skID == 1
			|| _skID == 113
			|| _skID == 115
			|| _skID == 116
			|| _skID == 117
			|| _skID == 118
			|| _skID == 119
			|| _skID == 120
			|| (_skID >= 163 && _skID <= 169))
			{
				doFmCraft();
				return;
			}
			else
            {
                try {
                	Thread.sleep(750);
                } catch (Exception e) {
                };
            }
			
			Map<Integer,Integer> items = new TreeMap<Integer,Integer>();
			//on retire les items mis en ingrédients
			for(Entry<Integer,Integer> e : _ingredients.entrySet())
			{
				//Si le joueur n'a pas l'objet
				if(!_P.hasItemGuid(e.getKey()))
				{
					SocketManager.GAME_SEND_Ec_PACKET(_P,"EI");
					Logs.addToGameLog(_P.get_name()+" essaye de crafter avec un objet qu'il n'a pas");
					return;
				}
				//Si l'objet n'existe pas
				Objects obj = World.getObjet(e.getKey());
				if(obj == null)
				{
					SocketManager.GAME_SEND_Ec_PACKET(_P,"EI");
					Logs.addToGameLog(_P.get_name()+" essaye de crafter avec un objet qui n'existe pas");
					return;
				}
				//Si la quantité est trop faible
				if(obj.getQuantity() < e.getValue())
				{
					SocketManager.GAME_SEND_Ec_PACKET(_P,"EI");
					Logs.addToGameLog(_P.get_name()+" essaye de crafter avec un objet dont la quantite est trop faible");
					return;
				}
				//On calcule la nouvelle quantité
				int newQua = obj.getQuantity() - e.getValue();
				
				if(newQua <0)return;//ne devrais pas arriver
				if(newQua == 0)
				{
					_P.removeItem(e.getKey());
					World.removeItem(e.getKey());
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_P, e.getKey());
				}else
				{
					obj.setQuantity(newQua);
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_P, obj);
				}
				//on ajoute le couple tID/qua a la liste des ingrédients pour la recherche
				items.put(obj.getTemplate().getID(), e.getValue());
			}
			//On retire les items a ignorer pour la recette
			//Rune de signature
				if(items.containsKey(7508))signed = true;
				items.remove(7508);
			//Fin des items a retirer
			SocketManager.GAME_SEND_Ow_PACKET(_P);
			
			//On trouve le template corespondant si existant
			StatsMetier SM = _P.getMetierBySkill(_skID);
			int tID = World.getObjectByIngredientForJob(SM.getTemplate().getListBySkill(_skID),items);
			
			//Recette non existante ou pas adapté au métier
			if(tID == -1 || !SM.getTemplate().canCraft(_skID, tID))
			{
				SocketManager.GAME_SEND_Ec_PACKET(_P,"EI");
				SocketManager.GAME_SEND_IO_PACKET_TO_MAP(_P.get_curCarte(),_P.get_GUID(),"-");
				_ingredients.clear();
				
				return;
			}
			
			int chan =  Constant.getChanceByNbrCaseByLvl(SM.get_lvl(),_ingredients.size());
			int jet = Formulas.getRandomValue(1, 100);
			boolean success = chan >= jet;
			
			if(!success)//Si echec
			{
				SocketManager.GAME_SEND_Ec_PACKET(_P,"EF");
				SocketManager.GAME_SEND_IO_PACKET_TO_MAP(_P.get_curCarte(),_P.get_GUID(),"-"+tID);
				SocketManager.GAME_SEND_Im_PACKET(_P, "0118");
			}else
			{
				Objects newObj = World.getObjTemplate(tID).createNewItem(1, false, -1);
				//Si signé on ajoute la ligne de Stat "Fabriqué par:"
				if(signed)newObj.addTxtStat(988, _P.get_name());
				boolean add = true;
				int guid = newObj.getGuid();
				
				for(Entry<Integer,Objects> entry : _P.getItems().entrySet())
				{
					Objects obj = entry.getValue();
					if(obj.getTemplate().getID() == newObj.getTemplate().getID()
						&& obj.getStats().isSameStats(newObj.getStats())
						&& obj.getPosition() == Constant.ITEM_POS_NO_EQUIPED)//Si meme Template et Memes Stats et Objet non équipé
					{
						obj.setQuantity(obj.getQuantity()+newObj.getQuantity());//On ajoute QUA item a la quantité de l'objet existant
						SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_P,obj);
						add = false;
						guid = obj.getGuid();
					}
				}
				if(add)
				{
					_P.getItems().put(newObj.getGuid(), newObj);
					SocketManager.GAME_SEND_OAKO_PACKET(_P,newObj);
					World.addObjet(newObj, true);
				}
				
				//on envoie les Packets
				SocketManager.GAME_SEND_Ow_PACKET(_P);
				SocketManager.GAME_SEND_Em_PACKET(_P,"KO+"+guid+"|1|"+tID+"|"+newObj.parseStatsString().replace(";","#"));
				SocketManager.GAME_SEND_Ec_PACKET(_P,"K;"+tID);
				SocketManager.GAME_SEND_IO_PACKET_TO_MAP(_P.get_curCarte(),_P.get_GUID(),"+"+tID);
			}
			
			
			//On donne l'xp
			int winXP =  Constant.calculXpWinCraft(SM.get_lvl(),_ingredients.size()) * Config.XP_METIER;
			if(success)
			{
				SM.addXp(_P,winXP);
				ArrayList<StatsMetier> SMs = new ArrayList<StatsMetier>();
				SMs.add(SM);
				SocketManager.GAME_SEND_JX_PACKET(_P, SMs);
			}
			
			_lastCraft.clear();
			_lastCraft.putAll(_ingredients);
			_ingredients.clear();
			//*/
		}
		
		
/********************************************** FM SYSTEM ********************************************************/
/** 										 By Return/Skryn													 */
/*****************************************************************************************************************/
		
		//TODO: Refresh des runes dans le bloc gauche du panel FM et non fouttre un clear directement.
		private void doFmCraft() {
			boolean isSigningRune = false;
			Objects objectFm = null, signingRune = null, runeOrPotion = null;
			int lvlElementRune = 0, statsID = -1, lvlQuaStatsRune = 0, statsAdd = 0, deleteID = -1, poid = 0;
			boolean bonusRune = false;
			String statsObjectFm = "-1";
			for (int idIngredient : _ingredients.keySet()) {
				Objects ing = World.getObjet(idIngredient);
				if (ing == null || !_P.hasItemGuid(idIngredient)) {
					SocketManager.GAME_SEND_Ec_PACKET(_P, "EI");
					SocketManager.GAME_SEND_IO_PACKET_TO_MAP(_P.get_curCarte(), _P.get_GUID(), "-");
					_ingredients.clear();
					return;
				}
				int templateID = ing.getTemplate().getID();
				switch (templateID) {
					case 1333 :
						statsID = 99;
						lvlElementRune = ing.getTemplate().getLevel();
						runeOrPotion = ing;
						break;
					case 1335 :
						statsID = 96;
						lvlElementRune = ing.getTemplate().getLevel();
						runeOrPotion = ing;
						break;
					case 1337 :
						statsID = 98;
						lvlElementRune = ing.getTemplate().getLevel();
						runeOrPotion = ing;
						break;
					case 1338 :
						statsID = 97;
						lvlElementRune = ing.getTemplate().getLevel();
						runeOrPotion = ing;
						break;
					case 1340 :
						statsID = 97;
						lvlElementRune = ing.getTemplate().getLevel();
						runeOrPotion = ing;
						break;
					case 1341 :
						statsID = 96;
						lvlElementRune = ing.getTemplate().getLevel();
						runeOrPotion = ing;
						break;
					case 1342 :
						statsID = 98;
						lvlElementRune = ing.getTemplate().getLevel();
						runeOrPotion = ing;
						break;
					case 1343 :
						statsID = 99;
						lvlElementRune = ing.getTemplate().getLevel();
						runeOrPotion = ing;
						break;
					case 1345 :
						statsID = 99;
						lvlElementRune = ing.getTemplate().getLevel();
						runeOrPotion = ing;
						break;
					case 1346 :
						statsID = 96;
						lvlElementRune = ing.getTemplate().getLevel();
						runeOrPotion = ing;
						break;
					case 1347 :
						statsID = 98;
						lvlElementRune = ing.getTemplate().getLevel();
						runeOrPotion = ing;
						break;
					case 1348 :
						statsID = 97;
						lvlElementRune = ing.getTemplate().getLevel();
						runeOrPotion = ing;
						break;
					case 1519 :
						runeOrPotion = ing;
						statsObjectFm = "76";
						statsAdd = 1;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1521 :
						runeOrPotion = ing;
						statsObjectFm = "7c";
						statsAdd = 1;
						poid = 6;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1522 :
						runeOrPotion = ing;
						statsObjectFm = "7e";
						statsAdd = 1;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1523 :
						runeOrPotion = ing;
						statsObjectFm = "7d";
						statsAdd = 3;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1524 :
						runeOrPotion = ing;
						statsObjectFm = "77";
						statsAdd = 1;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1525 :
						runeOrPotion = ing;
						statsObjectFm = "7b";
						statsAdd = 1;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1545 :
						runeOrPotion = ing;
						statsObjectFm = "76";
						statsAdd = 3;
						poid = 3;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1546 :
						runeOrPotion = ing;
						statsObjectFm = "7c";
						statsAdd = 3;
						poid = 18;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1547 :
						runeOrPotion = ing;
						statsObjectFm = "7e";
						statsAdd = 3;
						poid = 3;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1548 :
						runeOrPotion = ing;
						statsObjectFm = "7d";
						statsAdd = 10;
						poid = 10;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1549 :
						runeOrPotion = ing;
						statsObjectFm = "77";
						statsAdd = 3;
						poid = 3;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1550 :
						runeOrPotion = ing;
						statsObjectFm = "7b";
						statsAdd = 3;
						poid = 10;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1551 :
						runeOrPotion = ing;
						statsObjectFm = "76";
						statsAdd = 10;
						poid = 10;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1552 :
						runeOrPotion = ing;
						statsObjectFm = "7c";
						statsAdd = 10;
						poid = 50;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1553 :
						runeOrPotion = ing;
						statsObjectFm = "7e";
						statsAdd = 10;
						poid = 10;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1554 :
						runeOrPotion = ing;
						statsObjectFm = "7d";
						statsAdd = 30;
						poid = 10;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1555 :
						runeOrPotion = ing;
						statsObjectFm = "77";
						statsAdd = 10;
						poid = 10;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1556 :
						runeOrPotion = ing;
						statsObjectFm = "7b";
						statsAdd = 10;
						poid = 10;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1557 :
						runeOrPotion = ing;
						statsObjectFm = "6f";
						statsAdd = 1;
						poid = 100;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 1558 :
						runeOrPotion = ing;
						statsObjectFm = "80";
						statsAdd = 1;
						poid = 90;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7433 :
						runeOrPotion = ing;
						statsObjectFm = "73";
						statsAdd = 1;
						poid = 30;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7434 :
						runeOrPotion = ing;
						statsObjectFm = "b2";
						statsAdd = 1;
						poid = 20;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7435 :
						runeOrPotion = ing;
						statsObjectFm = "70";
						statsAdd = 1;
						poid = 20;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7436 :
						runeOrPotion = ing;
						statsObjectFm = "8a";
						statsAdd = 1;
						poid = 2;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7437 :
						runeOrPotion = ing;
						statsObjectFm = "dc";
						statsAdd = 1;
						poid = 2;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7438 :
						runeOrPotion = ing;
						statsObjectFm = "75";
						statsAdd = 1;
						poid = 50;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7442 :
						runeOrPotion = ing;
						statsObjectFm = "b6";
						statsAdd = 1;
						poid = 30;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7443 :
						runeOrPotion = ing;
						statsObjectFm = "9e";
						statsAdd = 10;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7444 :
						runeOrPotion = ing;
						statsObjectFm = "9e";
						statsAdd = 30;
						poid = 1; 
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7445 :
						runeOrPotion = ing;
						statsObjectFm = "9e";
						statsAdd = 100;
						poid = 1; 
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7446 :
						runeOrPotion = ing;
						statsObjectFm = "e1";
						statsAdd = 1;
						poid = 15;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7447 :
						runeOrPotion = ing;
						statsObjectFm = "e2";
						statsAdd = 1;
						poid = 2;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7448 :
						runeOrPotion = ing;
						statsObjectFm = "ae";
						statsAdd = 10;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7449 :
						runeOrPotion = ing;
						statsObjectFm = "ae";
						statsAdd = 30;
						poid = 3;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7450 :
						runeOrPotion = ing;
						statsObjectFm = "ae";
						statsAdd = 100;
						poid = 10;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7451 :
						runeOrPotion = ing;
						statsObjectFm = "b0";
						statsAdd = 1;
						poid = 5;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7452 :
						runeOrPotion = ing;
						statsObjectFm = "f3";
						statsAdd = 1;
						poid = 4;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7453 :
						runeOrPotion = ing;
						statsObjectFm = "f2";
						statsAdd = 1;
						poid = 4;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7454 :
						runeOrPotion = ing;
						statsObjectFm = "f1";
						statsAdd = 1;
						poid = 4;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7455 :
						runeOrPotion = ing;
						statsObjectFm = "f0";
						statsAdd = 1;
						poid = 4;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7456 :
						runeOrPotion = ing;
						statsObjectFm = "f4";
						statsAdd = 1;
						poid = 4;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7457 :
						runeOrPotion = ing;
						statsObjectFm = "d5";
						statsAdd = 1;
						poid = 5;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7458 :
						runeOrPotion = ing;
						statsObjectFm = "d4";
						statsAdd = 1;
						poid = 5;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7459 :
						runeOrPotion = ing;
						statsObjectFm = "d2";
						statsAdd = 1;
						poid = 5;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7460 :
						runeOrPotion = ing;
						statsObjectFm = "d6";
						statsAdd = 1;
						poid = 5;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7560 :
						runeOrPotion = ing;
						statsObjectFm = "d3";
						statsAdd = 1;
						poid = 5;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 8379 :
						runeOrPotion = ing;
						statsObjectFm = "7d";
						statsAdd = 10;
						poid = 10;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 10662 :
						runeOrPotion = ing;
						statsObjectFm = "b0";
						statsAdd = 3;
						poid = 15;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 7508 :
						isSigningRune = true;
						signingRune = ing;
						break;
					case 11118 :
						bonusRune = true;
						runeOrPotion = ing;
						statsObjectFm = "76";
						statsAdd = 15;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 11119 :
						bonusRune = true;
						runeOrPotion = ing;
						statsObjectFm = "7c";
						statsAdd = 15;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 11120 :
						bonusRune = true;
						runeOrPotion = ing;
						statsObjectFm = "7e";
						statsAdd = 15;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 11121 :
						bonusRune = true;
						runeOrPotion = ing;
						statsObjectFm = "7d";
						statsAdd = 45;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 11122 :
						bonusRune = true;
						runeOrPotion = ing;
						statsObjectFm = "77";
						statsAdd = 15;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 11123 :
						bonusRune = true;
						runeOrPotion = ing;
						statsObjectFm = "7b";
						statsAdd = 15;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 11124 :
						bonusRune = true;
						runeOrPotion = ing;
						statsObjectFm = "b0";
						statsAdd = 10;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 11125 :
						bonusRune = true;
						runeOrPotion = ing;
						statsObjectFm = "73";
						statsAdd = 3;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 11126 :
						bonusRune = true;
						runeOrPotion = ing;
						statsObjectFm = "b2";
						statsAdd = 5;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 11127 :
						bonusRune = true;
						runeOrPotion = ing;
						statsObjectFm = "70";
						statsAdd = 5;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 11128 :
						bonusRune = true;
						runeOrPotion = ing;
						statsObjectFm = "8a";
						statsAdd = 10;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					case 11129 :
						bonusRune = true;
						runeOrPotion = ing;
						statsObjectFm = "dc";
						statsAdd = 5;
						poid = 1;
						lvlQuaStatsRune = ing.getTemplate().getLevel();
						break;
					default :
						int type = ing.getTemplate().getType();
						if ((type >= 1 && type <= 11) || (type >= 16 && type <= 22) || type == 81 || type == 102 || type == 114
						|| ing.getTemplate().getPACost() > 0) {
							objectFm = ing;
							SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK_FM(_P.get_compte().getGameThread().get_out(), 'O',"+", objectFm.getGuid() + "|" + 1);
							deleteID = idIngredient;
							Objects newObj = Objects.getCloneObjet(objectFm, 1);
							if (objectFm.getQuantity() > 1) {
								int newQuant = objectFm.getQuantity() - 1;
								objectFm.setQuantity(newQuant);
								SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_P, objectFm);
								break;
							} else {
								World.removeItem(idIngredient);
								_P.removeItem(idIngredient);
								SocketManager.GAME_SEND_DELETE_STATS_ITEM_FM(_P, idIngredient);
							}
							objectFm = newObj;
						}
				}
			}
			StatsMetier job = _P.getMetierBySkill(_skID);
			job.addXp(_P, (int) (Config.XP_METIER + 9.0 / 10.0) * 10);
			if (job == null || objectFm == null || runeOrPotion == null) {
				SocketManager.GAME_SEND_Ec_PACKET(_P, "EI");
				SocketManager.GAME_SEND_IO_PACKET_TO_MAP(_P.get_curCarte(), _P.get_GUID(), "-");
				_ingredients.clear();
				return;
			}
			if (deleteID != -1) {
				_ingredients.remove(deleteID);
			}
			ObjTemplate objTemplate = objectFm.getTemplate();
			int chance = 0;
			int lvlJob = job.get_lvl();
			int objTemaplateID = objTemplate.getID();
			String statStringObj = objectFm.parseStatsString();
			if (lvlElementRune > 0 && lvlQuaStatsRune == 0) {
				chance = Formulas.calculateChanceByElement(lvlJob, objTemplate.getLevel(), lvlElementRune);
				if (chance > 100 - (lvlJob / 20))
					chance = 100 - (lvlJob / 20);
				if (chance < (lvlJob / 20))
					chance = (lvlJob / 20);
			} else if (lvlQuaStatsRune > 0 && lvlElementRune == 0) {
				int currentWeightTotal = 1;
				int currentWeightStats = 1;
				if (!statStringObj.isEmpty()) {
					currentWeightTotal = currentTotalWeigthBase(statStringObj, objectFm);
					currentWeightStats = currentWeithStats(objectFm, statsObjectFm);
				}
				int currentTotalBase = WeithTotalBase(objTemaplateID);
				if (currentTotalBase < 0) {
					currentTotalBase = 0;
				}
				if (currentWeightStats < 0) {
					currentWeightStats = 0;
				}
				if (currentWeightTotal < 0) {
					currentWeightTotal = 0;
				}
				float coef = 1;
				int baseStats = ViewBaseStatsItem(objectFm, statsObjectFm);
				int currentStats = ViewActualStatsItem(objectFm, statsObjectFm);
				if (baseStats == 1 && currentStats == 1 || baseStats == 1 && currentStats == 0) {
					coef = 1.0f;
				} else if (baseStats == 2 && currentStats == 2) {
					coef = 0.50f;
				} else if (baseStats == 0 && currentStats == 0 || baseStats == 0 && currentStats == 1) {
					coef = 0.25f;
				}
				if (getActualJet(objectFm, statsObjectFm) >= getStatBaseMaxs(objectFm.getTemplate(), statsObjectFm))
					coef = 0.15f;
				int diff = (int) (currentTotalBase * 1.3f) - currentWeightTotal;
				chance = Formulas.chanceFM(currentTotalBase, currentWeightTotal, currentWeightStats, poid, diff, coef);
				if (bonusRune)
					chance += 20;
				if (chance < 1)
					chance = 1;
				else if (chance > 100)
					chance = 100;
				
				Logs.addToFmLog("Personnage "+_P.get_name()+": ObjectFM("+objectFm.getTemplate().getName()+"),Rune("+runeOrPotion.getTemplate().getName()+"), ChanceTotale("+chance+")");
			}
			int aleatoryChance = Formulas.getRandomValue(1, 100);
			boolean sucess = chance >= aleatoryChance;
			if (!sucess) { // Si il n'a pas réussi
				Logs.addToFmLog("Personnage "+_P.get_name()+": Object '"+objectFm.getTemplate().getName()+"' hasn't fm witch succes !");
				if (signingRune != null) {
					int newQua = signingRune.getQuantity() - 1;
					if (newQua <= 0) {
						_P.removeItem(signingRune.getGuid());
						World.removeItem(signingRune.getGuid());
						SocketManager.GAME_SEND_DELETE_STATS_ITEM_FM(_P, signingRune.getGuid());
					} else {
						signingRune.setQuantity(newQua);
						SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_P, signingRune);
					}
				}
				if (runeOrPotion != null) {
					int newQua = runeOrPotion.getQuantity() - 1;
					if (newQua <= 0) {
						_P.removeItem(runeOrPotion.getGuid());
						World.removeItem(runeOrPotion.getGuid());
						SocketManager.GAME_SEND_DELETE_STATS_ITEM_FM(_P, runeOrPotion.getGuid());
					} else {
						runeOrPotion.setQuantity(newQua);
						SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_P, runeOrPotion);
					}
				}
					World.addObjet(objectFm, true);
					_P.addObjet(objectFm);
					if (!statStringObj.isEmpty()) {
						String statsStr = objectFm.parseStringStatsEC_FM(objectFm, poid);
						objectFm.clearStats();
						objectFm.parseStringToStats(statsStr);
					}
					SocketManager.GAME_SEND_OAKO_PACKET(_P, objectFm);
					SocketManager.GAME_SEND_Ow_PACKET(_P);
					
					String data = objectFm.getGuid() + "|1|" + objectFm.getTemplate().getID() + "|" + objectFm.parseStatsString();
					if (!_isRepeat)
						_reConfigingRunes = -1;
					if (_reConfigingRunes != 0 || _broken)
						SocketManager.GAME_SEND_EXCHANGE_MOVE_OK_FM(_P, 'O', "+", data);
					_data = data;
				
				
				SocketManager.GAME_SEND_IO_PACKET_TO_MAP(_P.get_curCarte(), _P.get_GUID(), "-" + objTemaplateID);
				SocketManager.GAME_SEND_Ec_PACKET(_P, "EF");
				SocketManager.GAME_SEND_Im_PACKET(_P, "0183");
			} else {// Si réussite :)
				Logs.addToFmLog("Personnage "+_P.get_name()+":  +"+objectFm.getTemplate().getName()+" has fm witch succes !");
				int coef = 0;
				if (lvlElementRune == 1)
					coef = 50;
				else if (lvlElementRune == 25)
					coef = 65;
				else if (lvlElementRune == 50)
					coef = 85;
				if (isSigningRune) {
					objectFm.addTxtStat(985, _P.get_name());
				}
				if (lvlElementRune > 0 && lvlQuaStatsRune == 0) {
					for (SpellEffect effect : objectFm.getEffects()) {
						if (effect.getEffectID() != 100)
							continue;
						String[] infos = effect.getArgs().split(";");
						try {
							int min = Integer.parseInt(infos[0], 16);
							int max = Integer.parseInt(infos[1], 16);
							int newMin = (int) ((min * coef) / 100);
							int newMax = (int) ((max * coef) / 100);
							if (newMin == 0)
								newMin = 1;
							String newRange = "1d" + (newMax - newMin + 1) + "+" + (newMin - 1);
							String newArgs = Integer.toHexString(newMin) + ";" + Integer.toHexString(newMax) + ";-1;-1;0;"
							+ newRange;
							effect.setArgs(newArgs);
							effect.setEffectID(statsID);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} else if (lvlQuaStatsRune > 0 && lvlElementRune == 0) {
					boolean negative = false;
					int currentStats = ViewActualStatsItem(objectFm, statsObjectFm);
					if (currentStats == 2) {
						if (statsObjectFm.compareTo("7b") == 0) {
							statsObjectFm = "98";
							negative = true;
						}
						if (statsObjectFm.compareTo("77") == 0) {
							statsObjectFm = "9a";
							negative = true;
						}
						if (statsObjectFm.compareTo("7e") == 0) {
							statsObjectFm = "9b";
							negative = true;
						}
						if (statsObjectFm.compareTo("76") == 0) {
							statsObjectFm = "9d";
							negative = true;
						}
						if (statsObjectFm.compareTo("7c") == 0) {
							statsObjectFm = "9c";
							negative = true;
						}
						if (statsObjectFm.compareTo("7d") == 0) {
							statsObjectFm = "99";
							negative = true;
						}
					}
					if (currentStats == 1 || currentStats == 2) {
						String statsStr = objectFm.parseFMStatsString(statsObjectFm, objectFm, statsAdd, negative);
						objectFm.clearStats();
						objectFm.parseStringToStats(statsStr);
					} else {
						if (statStringObj.isEmpty()) {
							String statsStr = statsObjectFm + "#" + Integer.toHexString(statsAdd) + "#0#0#0d0+" + statsAdd;
							objectFm.clearStats();
							objectFm.parseStringToStats(statsStr);
						} else {
							String statsStr = objectFm.parseFMStatsString(statsObjectFm, objectFm, statsAdd, negative) + ","
							+ statsObjectFm + "#" + Integer.toHexString(statsAdd) + "#0#0#0d0+" + statsAdd;
							objectFm.clearStats();
							objectFm.parseStringToStats(statsStr);
						}
					}
				}
				if (signingRune != null) {
					int newQua = signingRune.getQuantity() - 1;
					if (newQua <= 0) {
						_P.removeItem(signingRune.getGuid());
						World.removeItem(signingRune.getGuid());
						SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_P, signingRune.getGuid());
					} else {
						signingRune.setQuantity(newQua);
						SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_P, signingRune);
					}
				}
				if (runeOrPotion != null) {
					int newQua = runeOrPotion.getQuantity() - 1;
					if (newQua <= 0) {
						_P.removeItem(runeOrPotion.getGuid());
						World.removeItem(runeOrPotion.getGuid());
						SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_P, runeOrPotion.getGuid());
					} else {
						runeOrPotion.setQuantity(newQua);
						SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_P, runeOrPotion);
					}
				}
				World.addObjet(objectFm, true);
				_P.addObjet(objectFm);
				SocketManager.GAME_SEND_Ow_PACKET(_P);
				SocketManager.GAME_SEND_OAKO_PACKET(_P, objectFm);
				
				String data = objectFm.getGuid() + "|1|" + objectFm.getTemplate().getID() + "|" + objectFm.parseStatsString();
				if (!_isRepeat)
					_reConfigingRunes = -1;
				if (_reConfigingRunes != 0 || _broken)
					SocketManager.GAME_SEND_EXCHANGE_MOVE_OK_FM(_P, 'O', "+", data);
				_data = data;
				SocketManager.GAME_SEND_IO_PACKET_TO_MAP(_P.get_curCarte(), _P.get_GUID(), "+" + objTemaplateID);
				SocketManager.GAME_SEND_Ec_PACKET(_P, "K;" + objTemaplateID);
			}
			_lastCraft.clear();
			_lastCraft.putAll(_ingredients);
			_lastCraft.put(objectFm.getGuid(), 1);
			_ingredients.clear();
			Logs.addToFmLog("Personnage "+_P.get_name()+": End fm to '"+objectFm.getTemplate().getName()+"' sucessfully !");
		}
		
	public static int getStatBaseMaxs(ObjTemplate objMod, String statsModif) {
		String[] split = objMod.getStrTemplate().split(",");
		for (String s : split) {
			String[] stats = s.split("#");
			if (stats[0].toLowerCase().compareTo(statsModif.toLowerCase()) > 0) {
				continue;
			} else if (stats[0].toLowerCase().compareTo(statsModif.toLowerCase()) == 0) {
				int max = Integer.parseInt(stats[2], 16);
				if (max == 0)
					max = Integer.parseInt(stats[1], 16);
				return max;
			}
		}
		return 0;
	}

	public static int WeithTotalBase(int objTemplateID) {
			int weight = 0;
			int alt = 0;
			String statsTemplate = "";
			statsTemplate = World.getObjTemplate(objTemplateID).getStrTemplate();
			if (statsTemplate == null || statsTemplate.isEmpty())
				return 0;
			String[] split = statsTemplate.split(",");
			for (String s : split) {
				String[] stats = s.split("#");
				int statID = Integer.parseInt(stats[0], 16);
				boolean sig = true;
				for (int a : Constant.ARMES_EFFECT_IDS)
					if (a == statID)
						sig = false;
				if (!sig)
					continue;
				String jet = "";
				int value = 1;
				try {
					jet = stats[4];
					value = Formulas.getRandomJet(jet);
					try {
						int min = Integer.parseInt(stats[1], 16);
						int max = Integer.parseInt(stats[2], 16);
						value = min;
						if (max != 0)
							value = max;
					} catch (Exception e) {
						value = Formulas.getRandomJet(jet);
					}
				} catch (Exception e) {}
				int statX = 1;
				if (statID == 125 || statID == 158 || statID == 174)
				{
					statX = 1;
				} else if (statID == 118 || statID == 126 || statID == 119 || statID == 123)
				{
					statX = 2;
				} else if (statID == 138 || statID == 666 || statID == 226 || statID == 220)																																	// de
																																										// daños,Trampas %
				{
					statX = 3;
				} else if (statID == 124 || statID == 176)
				{
					statX = 5;
				} else if (statID == 240 || statID == 241 || statID == 242 || statID == 243 || statID == 244)
													
				{
					statX = 7;
				} else if (statID == 210 || statID == 211 || statID == 212 || statID == 213 || statID == 214)
				
				{
					statX = 8;
				} else if (statID == 225)
				{
					statX = 15;
				} else if (statID == 178 || statID == 112)
				{
					statX = 20;
				} else if (statID == 115 || statID == 182)
				{
					statX = 30;
				} else if (statID == 117)
				{
					statX = 50;
				} else if (statID == 128)
				{
					statX = 90;
				} else if (statID == 111)
				{
					statX = 100;
				}
				weight = value * statX; 
				alt += weight;
			}
			return alt;
		}

		public static int currentWeithStats(Objects obj, String statsModif) {
			for (Entry<Integer, Integer> entry : obj.getStats().getMap().entrySet()) {
				int statID = entry.getKey();
				if (Integer.toHexString(statID).toLowerCase().compareTo(statsModif.toLowerCase()) > 0) {
					continue;
				} else if (Integer.toHexString(statID).toLowerCase().compareTo(statsModif.toLowerCase()) == 0) {
					int statX = 1;
					int coef = 1;
					int BaseStats = ViewBaseStatsItem(obj, Integer.toHexString(statID));
					if (BaseStats == 2) {
						coef = 3;
					} else if (BaseStats == 0) {
						coef = 8;
					}
					if (statID == 125 || statID == 158 || statID == 174)
					{
						statX = 1;
					} else if (statID == 118 || statID == 126 || statID == 119 || statID == 123)
				
					{
						statX = 2;
					} else if (statID == 138 || statID == 666 || statID == 226 || statID == 220)																																					// daños,Trampas
																																											// %
					{
						statX = 3;
					} else if (statID == 124 || statID == 176)
					{
						statX = 5;
					} else if (statID == 240 || statID == 241 || statID == 242 || statID == 243 || statID == 244)
										
					{
						statX = 7;
					} else if (statID == 210 || statID == 211 || statID == 212 || statID == 213 || statID == 214)
					{
						statX = 8;
					} else if (statID == 225)
					{
						statX = 15;
					} else if (statID == 178 || statID == 112)
					{
						statX = 20;
					} else if (statID == 115 || statID == 182)
					{
						statX = 30;
					} else if (statID == 117)
					{
						statX = 50;
					} else if (statID == 128)
					{
						statX = 90;
					} else if (statID == 111)
					{
						statX = 100;
					}
					int Weight = entry.getValue() * statX * coef;
					return Weight;
				}
			}
			return 0;
		}

	public static int currentTotalWeigthBase(String statsModelo, Objects obj) {
			int Weigth = 0;
			int Alto = 0;
			String[] split = statsModelo.split(",");
			for (String s : split) {
				String[] stats = s.split("#");
				int statID = Integer.parseInt(stats[0], 16);
				boolean xy = false;
				for (int a : Constant.ARMES_EFFECT_IDS)
					if (a == statID)
						xy = true;
				if (xy)
					continue;
				String jet = "";
				int qua = 1;
				try {
					jet = stats[4];
					qua = Formulas.getRandomJet(jet);
					try {
						int min = Integer.parseInt(stats[1], 16);
						int max = Integer.parseInt(stats[2], 16);
						qua = min;
						if (max != 0)
							qua = max;
					} catch (Exception e) {
						qua = Formulas.getRandomJet(jet);
					}
				} catch (Exception e) {}
				int statX = 1;
				int coef = 1;
				int statsBase = ViewBaseStatsItem(obj, stats[0]);
				if (statsBase == 2) {
					coef = 3;
				} else if (statsBase == 0) {
					coef = 8;
				}
				if (statID == 125 || statID == 158 || statID == 174)
				{
					statX = 1;
				} else if (statID == 118 || statID == 126 || statID == 119 || statID == 123)
				{
					statX = 2;
				} else if (statID == 138 || statID == 666 || statID == 226 || statID == 220)																															// de
																																										// daños,Trampas %
				{
					statX = 3;
				} else if (statID == 124 || statID == 176)
				{
					statX = 5;
				} else if (statID == 240 || statID == 241 || statID == 242 || statID == 243 || statID == 244)
				{
					statX = 7;
				} else if (statID == 210 || statID == 211 || statID == 212 || statID == 213 || statID == 214)
										
				{
					statX = 8;
				} else if (statID == 225)
				{
					statX = 15;
				} else if (statID == 178 || statID == 112)
				{
					statX = 20;
				} else if (statID == 115 || statID == 182)
				{
					statX = 30;
				} else if (statID == 117)
				{
					statX = 50;
				} else if (statID == 128)
				{
					statX = 90;
				} else if (statID == 111)
				{
					statX = 100;
				}
				Weigth = qua * statX * coef;
				Alto += Weigth;
			}
			return Alto;
		}
	public void startRepeat(int time, Characters P){ //Skryn & Return Enjoy :D
		_P = P;
		if (_skID != 1 && _skID != 113	&& _skID != 115	&& _skID != 116	&& _skID != 117	&& _skID != 118	&& _skID != 119	&& _skID != 120	&& _skID != 163	&& _skID != 164	&& _skID != 165	&& _skID != 166	&& _skID != 167	&& _skID != 168	&& _skID != 169){
			repeat(time, P);
			return;
		}
		_reConfigingRunes = time;
		_craftTimer.stop();
		_lastCraft.clear();
		_lastCraft.putAll(_ingredients);
		
		TimerTask temp = new TimerTask(){
			  public void run(){
				  _isRepeat = true;
				  _ingredients.clear();
				  if (_reConfigingRunes <= 0){
						SocketManager.GAME_SEND_Ea_PACKET(_P, "1");
						sendObject(_data, _P);
						_isRepeat = false;
						_broken = false;
						_break = false;
						Logs.addToFmLog("Personnage "+_P.get_name()+" has finished fmRepeat.");
						this.cancel();
						return;
				  }
			      if(_break || _broken ){
				    	  SocketManager.GAME_SEND_Ea_PACKET(_P, _broken ? "2" : "4");
				    	  sendObject(_data, _P);
						  _isRepeat = false;
						  _broken = false;
						  _break = false;
				    	  this.cancel();
				    	  return;
			      }else {
				    	  _reConfigingRunes -= 1;
				    	  SocketManager.GAME_SEND_EA_PACKET(_P, _reConfigingRunes + "");
						  _ingredients.putAll(_lastCraft);
				    	  doFmCraft();
			      }
			 }
		};
		_P.setActTimerTask(temp);
		Config.repeatFmTimer.schedule(temp, 100, 1000);
	}
	
	
	public void repeat(int time, Characters P) {//Skryn /Return
		_P = P;
		_isRepeat = true;
		_craftTimer.stop();
		_lastCraft.clear();
		_lastCraft.putAll(_ingredients);
		for (int craftRunes = time; craftRunes >= 0; craftRunes--) {
			_ingredients.clear();
			if (_break || _broken) {
				SocketManager.GAME_SEND_Ea_PACKET(_P, _broken ? "2" : "4");
				return;
			}
			SocketManager.GAME_SEND_EA_PACKET(_P, craftRunes + "");
			_ingredients.putAll(_lastCraft);
			craft();
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {}
		}
		SocketManager.GAME_SEND_Ea_PACKET(_P, "1");
		sendObject(_data, P);
		_isRepeat = false;
	}
	public void breakFM() {
		_broken = true;
	}
	
	private void sendObject(String str, Characters P) {
		if (!str.isEmpty())
			SocketManager.GAME_SEND_EXCHANGE_MOVE_OK_FM(_P, 'O', "+", str);
	}
	
	
/********************************* END FM SYSTEM ****************************************/

		public void startCraft(Characters P) {
			//on retarde le lancement du craft en cas de packet EMR (craft auto)
			_craftTimer.start();
		}
	
	public void putLastCraftIngredients() {
		if (_P == null) {
			return;
		}
		if (_lastCraft == null) {
			return;
		}
		if (!_ingredients.isEmpty()) {
			return;//OffiLike, mais possible de faire un truc plus propre en enlevant les objets prï¿½sent et en rajoutant ceux de la recette
		}
		_ingredients.clear();
		_ingredients.putAll(_lastCraft);
		for (Entry<Integer, Integer> e : _ingredients.entrySet()) {
			if (World.getObjet(e.getKey()) == null) {
				return;
			}
			if (World.getObjet(e.getKey()).getQuantity() < e.getValue()) {
				return;
			}
			SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(_P, 'O', "+", e.getKey() + "|" + e.getValue());
		}
	}
	
	public void resetCraft() {
		_ingredients.clear();
		_lastCraft.clear();
	}
	}
	
	//Classe Metier
	private int _id;
	private ArrayList<Integer> _tools = new ArrayList<Integer>();
	private Map<Integer,ArrayList<Integer>> _crafts = new TreeMap<Integer,ArrayList<Integer>>();
	
	public Job(int id,String tools,String crafts)
	{
		_id= id;
		if(!tools.equals(""))
		{
			for(String str : tools.split(","))
			{
				try
				{
					int tool = Integer.parseInt(str);
					_tools.add(tool);
				}catch(Exception e){continue;};
			}
		}
		
		if(!crafts.equals(""))
		{
			for(String str : crafts.split("\\|"))
			{
				try
				{
					int skID = Integer.parseInt(str.split(";")[0]);
					ArrayList<Integer> list = new ArrayList<Integer>();
					for(String str2 : str.split(";")[1].split(","))list.add(Integer.parseInt(str2));
					_crafts.put(skID, list);
				}catch(Exception e){continue;};
			}
		}
	}
	public ArrayList<Integer> getListBySkill(int skID)
	{
		return _crafts.get(skID);
	}
	public boolean canCraft(int skill,int template)
	{
		if(_crafts.get(skill) != null)for(int a : _crafts.get(skill))if(a == template)return true;
		return false;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public boolean isValidTool(int t)
	{
		for(int a : _tools)if(t == a)return true;
		return false;
	}
	public static byte ViewActualStatsItem(Objects obj, String stats)//retourne vrai si le stats est actuellement sur l'item
	{
		if (!obj.parseStatsString().isEmpty()) {
			for (Entry<Integer, Integer> entry : obj.getStats().getMap().entrySet()) {
				if (Integer.toHexString(entry.getKey()).compareTo(stats) > 0)//Effets inutiles
				{
					if (Integer.toHexString(entry.getKey()).compareTo("98") == 0 && stats.compareTo("7b") == 0) {
						return 2;
					} else if (Integer.toHexString(entry.getKey()).compareTo("9a") == 0 && stats.compareTo("77") == 0) {
						return 2;
					} else if (Integer.toHexString(entry.getKey()).compareTo("9b") == 0 && stats.compareTo("7e") == 0) {
						return 2;
					} else if (Integer.toHexString(entry.getKey()).compareTo("9d") == 0 && stats.compareTo("76") == 0) {
						return 2;
					} else if (Integer.toHexString(entry.getKey()).compareTo("74") == 0 && stats.compareTo("75") == 0) {
						return 2;
					} else if (Integer.toHexString(entry.getKey()).compareTo("99") == 0 && stats.compareTo("7d") == 0) {
						return 2;
					} else {
						continue;
					}
				} else if (Integer.toHexString(entry.getKey()).compareTo(stats) == 0)//L'effet existe bien !
				{
					return 1;
				}
			}
			return 0;
		} else {
			return 0;
		}
	}
	
	public static byte ViewBaseStatsItem(Objects obj, String ItemStats)//retourne vrai si le stats existe de base sur l'item
	{
		
		String[] splitted = obj.getTemplate().getStrTemplate().split(",");
		for(String s : splitted)
		{
			String[] stats = s.split("#");
			if(stats[0].compareTo(ItemStats) > 0)//Effets n'existe pas de base
			{
				if(stats[0].compareTo("98") == 0 && ItemStats.compareTo("7b") == 0)
				{
					return 2;
				}
				else if(stats[0].compareTo("9a") == 0 && ItemStats.compareTo("77") == 0)
				{
					return 2;
				}
				else if(stats[0].compareTo("9b") == 0 && ItemStats.compareTo("7e") == 0)
				{
					return 2;
				}
				else if(stats[0].compareTo("9d") == 0 && ItemStats.compareTo("76") == 0)
				{
					return 2;
				}
				else if(stats[0].compareTo("74") == 0 && ItemStats.compareTo("75") == 0)
				{
					return 2;
				}
				else if(stats[0].compareTo("99") == 0 && ItemStats.compareTo("7d") == 0)
				{
					return 2;
				}
				else
				{
					continue;
				}
			}
			else if(stats[0].compareTo(ItemStats) == 0)//L'effet existe bien !
			{
				return 1;
			}
		}
		return 0;
	}
	
	public static int getBaseMaxJet(int templateID, String statsModif)
	{
		ObjTemplate t = World.getObjTemplate(templateID);
		String[] splitted = t.getStrTemplate().split(",");
		for(String s : splitted)
		{
			String[] stats = s.split("#");
			if(stats[0].compareTo(statsModif) > 0)//Effets n'existe pas de base
			{
				continue;
			}
			else if(stats[0].compareTo(statsModif) == 0)//L'effet existe bien !
			{
				int max = Integer.parseInt(stats[2],16);
				if(max == 0) max = Integer.parseInt(stats[1],16);//Pas de jet maximum on prend le minimum
				return max;
			}
		}
		return 0;
	}
	
	public static int getActualJet(Objects obj, String statsModif)
	{
		for(Entry<Integer,Integer> entry : obj.getStats().getMap().entrySet())
		{
			if(Integer.toHexString(entry.getKey()).compareTo(statsModif) > 0)//Effets inutiles
			{
				continue;
			}
			else if(Integer.toHexString(entry.getKey()).compareTo(statsModif) == 0)//L'effet existe bien !
			{
				int JetActual = entry.getValue();		
				return JetActual;
			}
		}	
		return 0;
	}
	
}

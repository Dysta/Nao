package org.object;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.client.Characters;
import org.client.Characters.Stats;
import org.common.Constant;
import org.common.Formulas;
import org.common.SQLManager;
import org.common.SocketManager;
import org.common.World;
import org.kernel.Logs;
import org.object.Objects.ObjTemplate;

public class Mount {

	private int _id;
	private int _color;
	private int _sexe;
	private int _amour;
	private int _endurance;
	private int _level;
	private long _exp;
	private String _nom;
	private int _fatigue;
	private int _energie;
	private int _reprod;
	private int _maturite;
	private int _serenite;
	private Stats _stats = new Stats();
	private String _ancetres = ",,,,,,,,,,,,,";
	private ArrayList<Objects> _items = new ArrayList<Objects>();
	private List<Integer> capacite = new ArrayList<Integer>();
	private String _ability = ",";
	
	public Mount(int color)
	{
		_id = World.getNextIdForMount();
		_color = color;
		_sexe = Formulas.getRandomValue(0, 1);
		_level = 1;
		_exp = 0;
		_nom = "NoName";
		_fatigue = 0;
		_energie = getMaxEnergie();
		_reprod = 0;
		_maturite = getMaxMatu();
		_serenite = 0;
		_stats = Constant.getMountStats(_color,_level);
		_ancetres = ",,,,,,,,,,,,,";
		_ability = ""+Formulas.getChanceCapa(1, 4, 9)+"";
		
		World.addDragodinde(this);
		SQLManager.CREATE_MOUNT(this);
	}
	
	public Mount(int id, int color, int sexe, int amour, int endurance,
			int level, long exp, String nom, int fatigue,
			int energie, int reprod, int maturite, int serenite,String items,String anc, String ability)
	{
		_id = id;
		_color = color;
		_sexe = sexe;
		_amour = amour;
		_endurance = endurance;
		_level = level;
		_exp = exp;
		_nom = nom;
		_fatigue = fatigue;
		_energie = energie;
		_reprod = reprod;
		_maturite = maturite;
		_serenite = serenite;
		_ancetres = anc;
		_stats = Constant.getMountStats(_color,_level);
		_ability = ability;
		for (String s : ability.split(",", 2))
			if (s != null) {
				int a = Integer.parseInt(s);
				try {
					this.capacite.add(Integer.valueOf(a));
				} catch (Exception localException) {}
			}
		for (String str : items.split(";")) {
			try {
				Objects obj = World.getObjet(Integer.parseInt(str));
				if (obj != null)
					_items.add(obj);
			} catch (Exception e) {
				continue;
			}
		}
	}

	public int get_id() {
		return _id;
	}

	public int get_color() {
		return _color;
	}
	
	public String get_color(String a)
	{
		String b = "";
		if (capacite.contains(Integer.valueOf(9))) 
			b = b + "," + a;
		return _color + b;
	}

	public int get_sexe() {
		return _sexe;
	}

	public int get_amour() {
		return _amour;
	}

	public String get_ancetres() {
		return _ancetres;
	}

	public int get_endurance() {
		return _endurance;
	}
	public int get_level() {
		return _level;
	}

	public long get_exp() {
		return _exp;
	}

	public String get_nom() {
		return _nom;
	}

	public int get_fatigue() {
		return _fatigue;
	}

	public int get_energie() {
		return _energie;
	}

	public int get_reprod() {
		return _reprod;
	}

	public int get_maturite() {
		return _maturite;
	}

	public int get_serenite() {
		return _serenite;
	}

	public Stats get_stats() {
		return _stats;
	}

	public ArrayList<Objects> getItems() {
		return _items;
	}
	
	public void CastrerDinde() {
		_reprod = -1;
	}
	
	public String parse()
	{
		StringBuilder str = new StringBuilder();
		str.append(_id).append(":");
		str.append(_color).append(":");
		str.append(_ancetres).append(":");
		str.append(",,").append(_ability).append(":");//FIXME capacités
		str.append(_nom).append(":");
		str.append(_sexe).append(":");
		str.append(parseXpString()).append(":");
		str.append(_level).append(":");
		str.append("1").append(":");//FIXME
		str.append(getTotalPod()).append(":");
		str.append("0").append(":");//FIXME podActuel?
		str.append(_endurance).append(",10000:");
		str.append(_maturite).append(",").append(getMaxMatu()).append(":");
		str.append(_energie).append(",").append(getMaxEnergie()).append(":");
		str.append(_serenite).append(",-10000,10000:");
		str.append(_amour).append(",10000:");
		str.append("-1").append(":");//FIXME
		str.append("0").append(":");//FIXME
		str.append(parseStats()).append(":");
		str.append(_fatigue).append(",240:");
		str.append(_reprod).append(",20:");
		return str.toString();
	}

	private String parseStats()
	{
		String stats = "";
		for(Entry<Integer,Integer> entry : _stats.getMap().entrySet())
		{
			if(entry.getValue() <= 0)continue;
			if(stats.length() >0)stats += ",";
			stats += Integer.toHexString(entry.getKey())+"#"+Integer.toHexString(entry.getValue())+"#0#0";
		}
		return stats;
	}

	private int getMaxEnergie()
	{
		if (isInfatiguable() == true)
		return 2000 + _level * 130;
		return 1000 + _level * 90;
	}

	private int getMaxMatu()
	{
		int matu = 1000;
		return matu;
	}

	private int getTotalPod()
	{
		if(isPorteuse() == true)
			return 500 + _level * 25;
		return 500 + _level * 15;
	}
	
	public int getMaxPod() {
		return getTotalPod();
	}

	private String parseXpString()
	{
		return _exp+","+World.getExpLevel(_level).dinde+","+World.getExpLevel(_level+1).dinde;
	}

	public boolean isMountable()
	{
		if(_energie <10
		|| _maturite < getMaxMatu()
		|| _fatigue == 240)return false;
		return true;
	}

	public void setName(String packet)
	{
		_nom = packet;
		SQLManager.UPDATE_MOUNT_INFOS(this);
	}
	
	public void addXp(long amount)
	{
		if(isSage() == true) {
		_exp += amount * 2;
		}
		_exp += amount;
		while(_exp >= World.getExpLevel(_level+1).dinde && _level<100)
			levelUp();
		
	}
	
	public void levelUp()
	{
		_level++;
		_stats = Constant.getMountStats(_color,_level);
		if(isInfatiguable() == true) {
			_energie = _energie + 130;
			if(_energie > getMaxEnergie()) _energie = getMaxEnergie();
		} else {
			_energie = _energie + 90;
			if(_energie > getMaxEnergie()) _energie = getMaxEnergie();
		}
	}
	
	public String get_ability() {
		return _ability;
	}
	
	public boolean addCapacity(String capacites) {
		int c = 0;
		for (String s : capacites.split(",", 2)) {
			if (capacite.size() >= 2) 
				return false; 
			try
			{
				c = Integer.parseInt(s); 
			} catch (Exception localException) {}
			
			if (c != 0)
				capacite.add(Integer.valueOf(c));
			
			if (capacite.size() == 1)
				_ability = (capacite.get(0) + ",");
			else
				_ability = (capacite.get(0) + "," + this.capacite.get(1));
		}
		return true;
	}
	
	public boolean isInfatiguable() {
		return capacite.contains(Integer.valueOf(1));
	}
	
	public boolean isPorteuse() {
		return capacite.contains(Integer.valueOf(2));
	}
	
	public boolean isSage() {
		return capacite.contains(Integer.valueOf(4));
	}

	public boolean isCameleone() {
		return capacite.contains(Integer.valueOf(9));
	}
	
	public void setEnergie(int energie){
        _energie = energie; 
        if(_energie > getMaxEnergie())
        	_energie = getMaxEnergie();
    }
	
	public int getPodsActuels() {
		int pods = 0;
		for (Objects obj : _items) {
			if (obj == null)
				continue;
			pods += (obj.getTemplate().getPod() * obj.getQuantity());
		}
		return pods;
	}
	
	public String getInventaire() {
		String items = "";
		for (Objects obj : _items) {
			items += "O" + obj.parseItem();
		}
		return items;
	}
	
	public void addObjToSac(int id, int qua, Characters perso) {
		Objects objToAdd = World.getObjet(id);
		if (objToAdd.getPosition() != -1)
			return;
		Objects SameObjInSac = getSimilarObject(objToAdd);
		int newQua = objToAdd.getQuantity() - qua;	
		if(perso.getItems().get(objToAdd.getGuid()) == null)
		{
			Logs.addToGameLog("Le joueur "+perso.get_name()+" a tenter d'ajouter un objet au store qu'il n'avait pas.");
			return;
		}
		if (perso.getItems().get(objToAdd.getGuid()).getQuantity() < qua)
		{
			Logs.addToGameLog("Le joueur "+perso.get_name()+" a tenté d'ajouter une quantité d'objet en banque dont il ne possédait pas.");
			return;
		}	
		if (SameObjInSac == null) {
			if (newQua <= 0) {
				perso.removeItem(objToAdd.getGuid());
				_items.add(objToAdd);
				String str = "O+" + objToAdd.getGuid() + "|" + objToAdd.getQuantity() + "|"
						+ objToAdd.getTemplate().getID() + "|" + objToAdd.parseStatsString();
				SocketManager.GAME_SEND_EsK_PACKET(perso, str);
				SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(perso, id);
			} else {
				objToAdd.setQuantity(newQua);
				SameObjInSac = Objects.getCloneObjet(objToAdd, qua);
				World.addObjet(SameObjInSac, true);
				_items.add(SameObjInSac);
				String str = "O+" + SameObjInSac.getGuid() + "|" + SameObjInSac.getQuantity() + "|"
						+ SameObjInSac.getTemplate().getID() + "|" + SameObjInSac.parseStatsString();
				SocketManager.GAME_SEND_EsK_PACKET(perso, str);
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(perso, objToAdd);
			}
		} else {
			if (newQua <= 0) {
				perso.removeItem(objToAdd.getGuid());
				SameObjInSac.setQuantity(SameObjInSac.getQuantity() + objToAdd.getQuantity());
				String str = "O+" + SameObjInSac.getGuid() + "|" + SameObjInSac.getQuantity() + "|"
						+ SameObjInSac.getTemplate().getID() + "|" + SameObjInSac.parseStatsString();
				World.removeItem(objToAdd.getGuid());
				SocketManager.GAME_SEND_EsK_PACKET(perso, str);
				SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(perso, id);
			} else {
				objToAdd.setQuantity(newQua);
				SameObjInSac.setQuantity(SameObjInSac.getQuantity() + qua);
				String str = "O+" + SameObjInSac.getGuid() + "|" + SameObjInSac.getQuantity() + "|"
						+ SameObjInSac.getTemplate().getID() + "|" + SameObjInSac.parseStatsString();
				SocketManager.GAME_SEND_EsK_PACKET(perso, str);
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(perso, objToAdd);
			}
		}
		SocketManager.GAME_SEND_Ow_PACKET(perso);
		SocketManager.GAME_SEND_MOUNT_PODS(perso, getPodsActuels());
		SQLManager.UPDATE_MOUNT_INFOS(this);
	}
	
	private Objects getSimilarObject(Objects obj) {
		for (Objects value : _items) {
			ObjTemplate item = value.getTemplate();
			if (item.getType() == 85)
				continue;
			if (item.getID() == obj.getTemplate().getID() && value.getStats().isSameStats(obj.getStats()))
				return value;
		}
		return null;
	}
	
	public void deleteFromSac(int id, int qua, Characters perso) {
		Objects objToDelete = World.getObjet(id);
		if (!_items.contains(objToDelete)) {
			return;
		}
		if(_items.get(objToDelete.getGuid()) == null)
		{
			Logs.addToGameLog("Le joueur "+perso.get_name()+" a tenter d'ajouter un objet au store qu'il n'avait pas.");
			return;
		}
		if (_items.get(objToDelete.getGuid()).getQuantity() < qua)
		{
			Logs.addToGameLog("Le joueur "+perso.get_name()+" a tenté d'ajouter une quantité d'objet en banque dont il ne possédait pas.");
			return;
		}
		Objects SameObjInInventaire = perso.getSimilarItem(objToDelete);
		int newQua = objToDelete.getQuantity() - qua;
		if (SameObjInInventaire == null) {
			if (newQua <= 0) {
				_items.remove(objToDelete);
				perso.addObjet(objToDelete, true);
				String str = "O-" + id;
				SocketManager.GAME_SEND_EsK_PACKET(perso, str);
			} else {
				SameObjInInventaire = Objects.getCloneObjet(objToDelete, qua);
				World.addObjet(SameObjInInventaire, true);
				objToDelete.setQuantity(newQua);
				perso.addObjet(SameObjInInventaire);
				SocketManager.GAME_SEND_OAKO_PACKET(perso, SameObjInInventaire);
				String str = "O+" + objToDelete.getGuid() + "|" + objToDelete.getQuantity() + "|"
						+ objToDelete.getTemplate().getID() + "|" + objToDelete.parseStatsString();
				SocketManager.GAME_SEND_EsK_PACKET(perso, str);
			}
		} else {
			if (newQua <= 0) {
				_items.remove(objToDelete);
				SameObjInInventaire.setQuantity(SameObjInInventaire.getQuantity() + objToDelete.getQuantity());
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(perso, SameObjInInventaire);
				World.removeItem(objToDelete.getGuid());
				String str = "O-" + id;
				SocketManager.GAME_SEND_EsK_PACKET(perso, str);
			} else {
				objToDelete.setQuantity(newQua);
				SameObjInInventaire.setQuantity(SameObjInInventaire.getQuantity() + qua);
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(perso, SameObjInInventaire);
				String str = "O+" + objToDelete.getGuid() + "|" + objToDelete.getQuantity() + "|"
						+ objToDelete.getTemplate().getID() + "|" + objToDelete.parseStatsString();
				SocketManager.GAME_SEND_EsK_PACKET(perso, str);
			}
		}
		SocketManager.GAME_SEND_Ow_PACKET(perso);
		SocketManager.GAME_SEND_MOUNT_PODS(perso, getPodsActuels());
		SQLManager.UPDATE_MOUNT_INFOS(this);
	}
	
	public String parseObjDB() {
		String str = "";
		for (Objects obj : _items)
			str += (str.length() > 0 ? ";" : "") + obj.getGuid();
		return str;
	}
	
}

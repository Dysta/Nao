package org.object;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.client.Characters;
import org.client.Characters.BoostSpellStats;
import org.client.Characters.Stats;
import org.common.Constant;
import org.common.Formulas;
import org.common.World;
import org.kernel.Config;
import org.kernel.Logs;
import org.object.job.Job;
import org.spell.SpellEffect;

import java.util.TreeMap;

public class Objects {

	public static class ObjTemplate
	{
		private int ID;
		private String StrTemplate;
		private String name;
		private	int type;
		private int level;
		private int pod;
		private int prix;
		private int _pricePoints;
		private int panopID;
		private String conditions;
		private int PACost,POmin,POmax,TauxCC,TauxEC,BonusCC;
		private boolean isTwoHanded;
		private ArrayList<Action> onUseActions = new ArrayList<Action>();
		private long sold;
		private int avgPrice;
		private boolean isArm = false;
		
		public ObjTemplate(int id, String strTemplate, String name, int type,int level, int pod, int prix, int panopID, String conditions,String armesInfos, int sold, int avgPrice, int pricePoints)
		{
			this.ID = id;
			this.StrTemplate = strTemplate;
			this.name = name;
			this.type = type;
			this.level = level;
			this.pod = pod;
			this.prix = prix;
			this.panopID = panopID;
			this.conditions = conditions;
			this.PACost = -1;
			this.POmin = 1;
			this.POmax = 1;
			this.TauxCC = 100;
			this.TauxEC = 100;
			this.BonusCC = 0;
			this.sold = sold;
			this.avgPrice = avgPrice;
			this._pricePoints = pricePoints;
			
			try
			{
				String[] infos = armesInfos.split(";");
				PACost = Integer.parseInt(infos[0]);
				POmin = Integer.parseInt(infos[1]);
				POmax = Integer.parseInt(infos[2]);
				TauxCC = Integer.parseInt(infos[3]);
				TauxEC = Integer.parseInt(infos[4]);
				BonusCC = Integer.parseInt(infos[5]);
				isTwoHanded = infos[6].equals("1");
				setArm(true);
			}catch(Exception e){};
	
		}
		
		public int get_obviType() {
			try {
				for (String sts : StrTemplate.split(",")) {
					String[] stats = sts.split("#");
					int statID = Integer.parseInt(stats[0], 16);
					if (statID == 973) {
						return Integer.parseInt(stats[3], 16);
					}
				}
			} catch (Exception e) {
				Logs.addToGameLog(e.getMessage());
				return Constant.ITEM_TYPE_OBJET_VIVANT;
			}
			return Constant.ITEM_TYPE_OBJET_VIVANT; //Si erreur on retourne le type de base
		}

		public void addAction(Action A)
		{
			onUseActions.add(A);
		}
		
		public int getPointsPrice() {
			return _pricePoints;
		}
		
		public boolean isTwoHanded()
		{
			return isTwoHanded;
		}
		public void setIsTwoHanded(boolean twohanded)
		{
			isTwoHanded = twohanded;
		}
		public int getBonusCC()
		{
			return BonusCC;
		}
		
		public int getPOmin() {
			return POmin;
		}
		
		public int getPOmax() {
			return POmax;
		}

		public int getTauxCC() {
			return TauxCC;
		}

		public int getTauxEC() {
			return TauxEC;
		}

		public int getPACost()
		{
			return PACost;
		}
		public int getID() {
			return ID;
		}

		public String getStrTemplate() {
			return StrTemplate;
		}

		public String getName() {
			return name;
		}

		public int getType() {
			return type;
		}

		public int getLevel() {
			return level;
		}

		public int getPod() {
			return pod;
		}

		public int getPrix() {
			return prix;
		}

		public int getPanopID() {
			return panopID;
		}

		public String getConditions() {
			return conditions;
		}

		public Characters.BoostSpellStats getBoostSpellStats(String statsTemplate)
		{
			Characters.BoostSpellStats sstats = new Characters.BoostSpellStats();

			if(statsTemplate.equals("") || statsTemplate == null) return sstats;
			
			String[] splitted = statsTemplate.split(",");
			for(String s : splitted)
			{	
				String[] stats = s.split("#");
				int statID = Integer.parseInt(stats[0],16);
				if(Constant.isSpellStat(statID))
				{
					sstats.addStat(Integer.parseInt(stats[1],16), statID, Integer.parseInt(stats[3],16));
				}
			}
			return sstats;
		}
		public Objects createNewItem(int qua,boolean useMax,int effet)
		{		
			Objects item = new Objects(World.getNewItemGuid(), ID, qua, Constant.ITEM_POS_NO_EQUIPED, generateNewStatsFromTemplate(StrTemplate,useMax,effet),getEffectTemplate(StrTemplate), getBoostSpellStats(StrTemplate));
			return item;
		}

		public Stats generateNewStatsFromTemplate(String statsTemplate,boolean useMax, int effet)
		{
			Stats itemStats = new Stats(false, null);
			//Si stats Vides
			if(statsTemplate.equals("") || statsTemplate == null) return itemStats;
			String statsTemplates = statsTemplate;
			if(effet == 1 || effet==3)
				statsTemplates += ",6f#1#0#0#0d0+1";
			if(effet == 2 || effet==3)
				statsTemplates += ",80#1#0#0#0d0+1";
			
			String[] splitted = statsTemplates.split(",");
			for(String s : splitted)
			{	
				String[] stats = s.split("#");
				int statID = Integer.parseInt(stats[0],16);
				if(Constant.isSpellStat(statID)) continue;
				boolean follow = true;
				
				for(int a : Constant.ARMES_EFFECT_IDS)//Si c'est un Effet Actif
					if(a == statID)
						follow = false;
				if(!follow)continue;//Si c'était un effet Actif d'arme
				
				String jet = "";
				int value  = 1;
				try
				{
					jet = stats[4];
					value = Formulas.getRandomJet(jet);
					if(useMax)
					{
						try
						{
							//on prend le jet max
							int min = Integer.parseInt(stats[1],16);
							int max = Integer.parseInt(stats[2],16);
							value = min;
							if(max != 0)value = max;
						}catch(Exception e){value = Formulas.getRandomJet(jet);};			
					}
				}catch(Exception e){};
				itemStats.addOneStat(statID, value);
			}
			return itemStats;
		}
		
		public ArrayList<SpellEffect> getEffectTemplate(String statsTemplate)
		{
			ArrayList<SpellEffect> Effets = new ArrayList<SpellEffect>();
			if(statsTemplate.equals("") || statsTemplate == null) return Effets;
			
			String[] splitted = statsTemplate.split(",");
			for(String s : splitted)
			{	
				String[] stats = s.split("#");
				int statID = Integer.parseInt(stats[0],16);
				for(int a : Constant.ARMES_EFFECT_IDS)
				{
					if(a == statID)
					{
						int id = statID;
						String min = stats[1];
						String max = stats[2];
						String jet = stats[4];
						String args = min+";"+max+";-1;-1;0;"+jet;
						Effets.add(new SpellEffect(id, args,0,-1));
					}
				}
			}
			return Effets;
		}
		
		public String parseItemTemplateStats()
		{
			return (this.ID+";"+StrTemplate);
		}

		public void applyAction(Characters perso, Characters target, int objID, short cellid)
		{
			for (Action a : onUseActions)
				a.apply(perso, target, objID, cellid);
		}
		
		public int getAvgPrice()
		{
			return avgPrice;
		}
		
		public long getSold()
		{
			return this.sold;
		}
		
		public synchronized void newSold(int amount, int price)
		{
			long oldSold = sold;
			sold += amount;
			avgPrice = (int)((avgPrice * oldSold + price) / sold);
		}

		public boolean isArm() {
			return isArm;
		}

		public void setArm(boolean isArm) {
			this.isArm = isArm;
		}
	}

	protected ObjTemplate template;
	protected int quantity = 1;
	protected int position = Constant.ITEM_POS_NO_EQUIPED;
	protected int guid;
	protected int obvijevan;
	protected int obvijevanLook;
	protected int obviID; //Return :)
	private Characters.BoostSpellStats SpellStats = new BoostSpellStats();
	private Characters.Stats Stats = new Stats();
	private ArrayList<SpellEffect> Effects = new ArrayList<SpellEffect>();
	private Map<Integer,String> txtStats = new TreeMap<Integer,String>();
	private Map<Integer,Integer> SoulStats = new TreeMap<Integer,Integer>();
	//Speaking Item
	//private boolean isExchangeable = true;
	protected boolean isSpeaking = false;
	protected boolean isPet = false;
	//private Speaking linkedItem = null;
	//private int linkedItem_id = -1;
	//private Speaking linkedItem = null;
	//private boolean isLinked = false;

	public Objects (int Guid, int template,int qua, int pos, String strStats)
	{
		this.guid = Guid;
		this.template = World.getObjTemplate(template);
		this.quantity = qua;
		this.position = pos;

		Stats = new Stats();
		parseStringToStats(strStats);
	}

	public Objects()
	{
		
	}
	
	public int getObvijevanPos() {
		return obvijevan;
	}
	
	public void setObvijevanPos(int pos) {
		obvijevan = pos;
		
	}
	public int getObvijevanLook() {
		return obvijevanLook;
	}
	
	public void setObvijevanLook(int look) {
		obvijevanLook = look;
	}
	
	public void setObviLastItem(int look) {
		obviID = look;
	}
	
	public int getObviID() {
		return obviID;
	}

	  
	
	public void parseStringToStats(String strStats)
	{
		String[] split = strStats.split(",");
		for(String s : split)
		{	
			try
			{
				String[] stats = s.split("#");
				int statID = Integer.parseInt(stats[0],16);
				
				if(statID == Constant.STATS_PETS_SOUL)
				{
					SoulStats.put(Integer.parseInt(stats[1], 16), Integer.parseInt(stats[3], 16));
					continue;
				}
				//Boost spell stats
				if(Constant.isSpellStat(statID))
				{
					SpellStats.addStat(Integer.parseInt(stats[1],16), statID, Integer.parseInt(stats[3],16));
					continue;
				}
				//Stats spécials
				if(statID == 997 || statID == 996)
				{
					txtStats.put(statID, stats[4]);
					continue;
				}
				//Si stats avec Texte (Signature, apartenance, etc)
				if((!stats[3].equals("") && (!stats[3].equals("0") || statID == Constant.STATS_PETS_DATE || statID == Constant.STATS_PETS_PDV || statID == Constant.STATS_PETS_POIDS || statID == Constant.STATS_PETS_EPO)))//Si le stats n'est pas vide et (n'est pas égale à 0 ou est de type familier)
				{
					txtStats.put(statID, stats[3]);
					continue;
				}
				
				String jet = stats[4];
				boolean follow = true;
				for(int a : Constant.ARMES_EFFECT_IDS)
				{
					if(a == statID)
					{
						int id = statID;
						String min = stats[1];
						String max = stats[2];
						String args = min+";"+max+";-1;-1;0;"+jet;
						Effects.add(new SpellEffect(id, args,0,-1));
						follow = false;
					}
				}
				if(!follow)continue;//Si c'était un effet Actif d'arme ou une signature
				int value = Integer.parseInt(stats[1],16);
				Stats.addOneStat(statID, value);
			}catch(Exception e){continue;};
		}
	}

	public void addSoulStat(int i, int j)
	{
		SoulStats.put(i, j);
	}
	
	public Map<Integer, Integer> getSoulStat()
	{
		return SoulStats;
	}
	
	public void addTxtStat(int i,String s)
	{
		txtStats.put(i, s);
	}
	public String getTxtStat(int i)
    {
        String stat = "";
        try
        {
            stat = (String)txtStats.get(Integer.valueOf(i));
        }
        catch(Exception exception) { }
        if(stat == null)
        {
            return "";
        } else
        {
            return stat;
        }
    }
    
	public Map<Integer, String> getTxtStat()
	{
		return txtStats;
	}
	
	public String getTraquedName()
	{
		for(Entry<Integer,String> entry : txtStats.entrySet())
		{
			if(Integer.toHexString(entry.getKey()).compareTo("3dd") == 0)
			{
				
				return entry.getValue();	
			}
		}
		return null;
	}
	
	public Objects(int Guid, int template, int qua, int pos,	Stats stats,ArrayList<SpellEffect> effects, BoostSpellStats sp)
	{
		this.guid = Guid;
		this.template = World.getObjTemplate(template);
		this.quantity = qua;
		this.position = pos;
		this.Stats = stats;
		this.Effects = effects;
		this.SpellStats = sp;
		this.obvijevan = 0;
	    this.obvijevanLook = 0;
	}
	
	public Characters.Stats getStats() {
		return Stats;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public ObjTemplate getTemplate() {
		return template;
	}

	public int getGuid() {
		return guid;
	}
	
	public String parseItem()
	{
		StringBuilder str = new StringBuilder();
		String posi = position==Constant.ITEM_POS_NO_EQUIPED?"":Integer.toHexString(position);
		str.append(Integer.toHexString(guid)).append("~").append(Integer.toHexString(template.getID())).append("~").append(Integer.toHexString(quantity)).append("~").append(posi).append("~").append(parseStatsString()).append(";");
		return str.toString();
	}
	public String convertStatsAString() {
		int TemplateType = template.getType();
		if (TemplateType == 83)
			return template.getStrTemplate();
		if ((Stats.getMap().isEmpty()) && (Effects.isEmpty())
		&& (txtStats.isEmpty()))
			return "";
		if (Config.INCARNATIONS_ARMES.contains(guid)) {
			return template.getStrTemplate();
		}
		String stats = "";
		boolean first = false;
		for (SpellEffect EH : Effects) {
			if (first)
				stats += ",";
			String[] infos = EH.getArgs().split(";");
			try {
				stats += Integer.toHexString(EH.getEffectID()) + "#" + infos[0] + "#" + infos[1] + "#0#" + infos[5];
			} catch (Exception e) {
				continue;
			}
			first = true;
		}
		for (Entry<Integer, Integer> entry : Stats.getMap().entrySet()) {
			int statID = (entry.getKey());
			if ((statID == 998) || (statID == 997) || (statID == 996) || (statID == 994) || (statID == 988)
			|| (statID == 987) || (statID == 986) || (statID == 985) || (statID == 983) || (statID == 960) || (statID == 961)
			|| (statID == 962) || (statID == 963) || (statID == 964))
				continue;
			if (first)
				stats += ",";
			String jet = "0d0+" + entry.getValue();
			stats += Integer.toHexString(statID) + "#" + Integer.toHexString((entry.getValue())) + "#0#0#" + jet;
			first = true;
		}
		for (Entry<Integer, String> entry : txtStats.entrySet()) {
			int statID = (entry.getKey());
			if (first)
				stats += ",";
			if ((statID == 800) || (statID == 811) || (statID == 961) || (statID == 962) || (statID == 960)
			|| (statID == 950) || (statID == 951))
				stats += Integer.toHexString(statID) + "#0#0#" + entry.getValue();
			else {
				stats += Integer.toHexString(statID) + "#0#0#0#" + entry.getValue();
			}
			first = true;
		}
		return stats;
	}
	
	public String parseStatsString()
	{
		if(getTemplate().getType() == 83)	//Si c'est une pierre d'âme vide
			return getTemplate().getStrTemplate();
		
		StringBuilder stats = new StringBuilder();
		boolean isFirst = true;
		for(SpellEffect SE : Effects)
		{
			if(!isFirst)
				stats.append(",");
			
			String[] infos = SE.getArgs().split(";");
			try
			{
				stats.append(Integer.toHexString(SE.getEffectID())).append("#").append(infos[0]).append("#").append(infos[1]).append("#0#").append(infos[5]);
			}catch(Exception e)
			{
				e.printStackTrace();
				continue;
			};
			
			isFirst = false;
		}
		
		for(Entry<Integer,Integer> entry : Stats.getMap().entrySet())
		{
			if(!isFirst)
				stats.append(",");
			int statID = ((Integer)entry.getKey()).intValue();

			if ((statID == 970) || (statID == 971) || (statID == 972) || (statID == 973) || (statID == 974))
			{
				int jet = ((Integer)entry.getValue()).intValue();
				if ((statID == 974) || (statID == 972) || (statID == 970))
					stats.append(Integer.toHexString(statID)).append("#0#0#").append(Integer.toHexString(jet));
				else {
					stats.append(Integer.toHexString(statID)).append("#0#0#").append(jet);
				}
				if (statID == 973) setObvijevanPos(jet);
				if (statID == 972) setObvijevanLook(jet); 
				if (statID == 970) setObviLastItem(jet);
			}
			else {
				String jet = "0d0+" + entry.getValue();
				stats.append(Integer.toHexString(statID)).append("#");
				stats.append(Integer.toHexString(((Integer)entry.getValue()).intValue())).append("#0#0#").append(jet);
			}
			//String jet = "0d0+"+entry.getValue();
			//stats.append(Integer.toHexString(entry.getKey())).append("#").append(Integer.toHexString(entry.getValue()));
			//stats.append("#0#0#").append(jet);
			isFirst = false;
		}
		for(Entry<Integer, Map<Integer, Integer>> entry : SpellStats.getAllEffects().entrySet())
		{
			if(entry == null || entry.getValue() == null) continue;
			for(Entry<Integer, Integer> stat : entry.getValue().entrySet())
			{
				if(!isFirst)stats.append(",");
				stats.append(Integer.toHexString(stat.getKey())).append("#").append(Integer.toHexString(entry.getKey())).append("#0#").append(Integer.toHexString(stat.getValue())).append("#0d0+").append(entry.getKey());
				isFirst = false;
			}
		}
		
		for(Entry<Integer,String> entry : txtStats.entrySet())
		{
			if(!isFirst)
				stats.append(",");
			
			if(entry.getKey() == Constant.CAPTURE_MONSTRE)
			{
				stats.append(Integer.toHexString(entry.getKey())).append("#0#0#").append(entry.getValue());	
			}
			else
			{
				stats.append(Integer.toHexString(entry.getKey())).append("#0#0#0#").append(entry.getValue());
			}
			isFirst = false;
		}
		return stats.toString();
	}
	
	public String parseStatsStringSansUserObvi()
	{
		return parseStatsStringSansUserObvi(false);
	}

	public String parseStatsStringSansUserObvi(boolean isObj)
	{
		if(getTemplate().getType() == 83)	//Si c'est une pierre d'âme vide
			return getTemplate().getStrTemplate();
		
		StringBuilder stats = new StringBuilder();
		boolean isFirst = true;
		for(SpellEffect SE : Effects)
		{
			if(!isFirst)
				stats.append(",");
			
			String[] infos = SE.getArgs().split(";");
			try
			{
				stats.append(Integer.toHexString(SE.getEffectID())).append("#").append(infos[0]).append("#").append(infos[1]).append("#0#").append(infos[5]);
			}catch(Exception e)
			{
				e.printStackTrace();
				continue;
			};
			
			isFirst = false;
		}
		
		for(Entry<Integer,Integer> entry : Stats.getMap().entrySet())
		{
			if(!isFirst)
				stats.append(",");
			String jet = "0d0+"+entry.getValue();
			stats.append(Integer.toHexString(entry.getKey())).append("#").append(Integer.toHexString(entry.getValue()));
			stats.append("#0#0#").append(jet);
			isFirst = false;
		}
		if(!isObj)
		{
			for(Entry<Integer, Map<Integer, Integer>> entry : SpellStats.getAllEffects().entrySet())
			{
				if(entry == null || entry.getValue() == null) continue;
				for(Entry<Integer, Integer> stat : entry.getValue().entrySet())
				{
					if(!isFirst)stats.append(",");
					stats.append(Integer.toHexString(stat.getKey())).append("#").append(Integer.toHexString(entry.getKey())).append("#0#").append(Integer.toHexString(stat.getValue())).append("#0d0+").append(entry.getKey());
					isFirst = false;
				}
			}
		}
		
		for(Entry<Integer,String> entry : txtStats.entrySet())
		{
			if(!isFirst)
				stats.append(",");
			
			if(entry.getKey() == Constant.CAPTURE_MONSTRE)
			{
				stats.append(Integer.toHexString(entry.getKey())).append("#0#0#").append(entry.getValue());	
			}
			else
			{
				stats.append(Integer.toHexString(entry.getKey())).append("#0#0#0#").append(entry.getValue());
			}
			isFirst = false;
		}
		return stats.toString();
	}
	
	public String parseToSave()
	{
	    	return parseStatsStringSansUserObvi();
	  	}
	
	public String obvijevanOCO_Packet(int pos)
	{
		String strPos = String.valueOf(pos);
		if (pos == -1) strPos = "";
		String upPacket = "OCO";
		upPacket = upPacket + Integer.toHexString(getGuid()) + "~";
		upPacket = upPacket + Integer.toHexString(getTemplate().getID()) + "~";
		upPacket = upPacket + Integer.toHexString(getQuantity()) + "~";
		upPacket = upPacket + strPos + "~";
		upPacket = upPacket + parseStatsString();
		return upPacket;
	}
	
	public void obvijevanNourir(Objects obj) {
		if (obj == null)
			return;
		for (Map.Entry<Integer, Integer> entry : Stats.getMap().entrySet())
		{
			if (entry.getKey().intValue() != 974) // on ne boost que la stat de l'expérience de l'obvi
				continue;
			if (entry.getValue().intValue() > 500) // si le boost a une valeur supérieure à 500 (irréaliste)
				return;
			entry.setValue(Integer.valueOf(entry.getValue().intValue() + obj.getTemplate().getLevel() / 32)); // valeur d'origine + ObjLvl / 32
			// s'il mange un obvi, on récupère son expérience
			/*if (obj.getTemplate().getID() == getTemplate().getID()) {
				for(Map.Entry<Integer, Integer> ent : obj.getStats().getMap().entrySet()) {
					if (entry.getKey().intValue() != 974) // on ne considère que la stat de l'expérience de l'obvi
						continue; 
					entry.setValue(Integer.valueOf(entry.getValue().intValue() + Integer.valueOf(ent.getValue().intValue())));
				}
			}*/
		}
	}
	
	public void obvijevanChangeStat(int statID, int val)
	{
		for (Map.Entry<Integer, Integer> entry : Stats.getMap().entrySet())
		{
			if (((Integer)entry.getKey()).intValue() != statID) continue; entry.setValue(Integer.valueOf(val));
		}
	}
	
	public void removeAllObvijevanStats() {
		setObvijevanPos(0);
		Characters.Stats StatsSansObvi = new Characters.Stats();
		for (Map.Entry<Integer, Integer> entry : Stats.getMap().entrySet())
		{
			int statID = ((Integer)entry.getKey()).intValue();
			if ((statID == 970) || (statID == 971) || (statID == 972) || (statID == 973) || (statID == 974))
				continue;
			StatsSansObvi.addOneStat(statID, ((Integer)entry.getValue()).intValue());
		}
		Stats = StatsSansObvi;
	}
	
	public void removeAll_ExepteObvijevanStats()
	{
		setObvijevanPos(0);
		Characters.Stats StatsSansObvi = new Characters.Stats();
		for (Map.Entry<Integer, Integer> entry : Stats.getMap().entrySet())
		{
			int statID = ((Integer)entry.getKey()).intValue();
			if ((statID != 971) && (statID != 972) && (statID != 973) && (statID != 974))
				continue;
			StatsSansObvi.addOneStat(statID, ((Integer)entry.getValue()).intValue());
		}
		Stats = StatsSansObvi;
		this.SpellStats = new Characters.BoostSpellStats();
	}
	
	public String getObvijevanStatsOnly()
	{
		Objects obj = getCloneObjet(this, 1);
		obj.removeAll_ExepteObvijevanStats();
		return obj.parseStatsStringSansUserObvi(true);
	}
	
	/*public String parseToSave()
	{
		return parseStatsString();
	}*/
	
	
	
	public boolean hasSpellBoostStats()
	{
		return this.SpellStats.haveStats();
	}
	
	public String parseFMStatsString(String statsstr, Objects obj, int add, boolean negatif) {
		StringBuilder stats = new StringBuilder("");
		boolean isFirst = true;
		for (SpellEffect SE : obj.Effects) {
			if (!isFirst) {
				stats.append(",");
			}

			String[] infos = SE.getArgs().split(";");
			try {
				stats.append(Integer.toHexString(SE.getEffectID())).append("#").append(infos[0]).append("#").append(infos[1]).append("#0#").append(infos[5]);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			};

			isFirst = false;
		}

		for (Entry<Integer, Integer> entry : obj.Stats.getMap().entrySet()) {
			if (!isFirst) {
				stats.append(",");
			}
			if (Integer.toHexString(entry.getKey()).compareTo(statsstr) == 0) {
				int newstats = 0;
				if (negatif) {
					newstats = entry.getValue() - add;
					if (newstats < 1) {
						continue;
					}
				} else {
					newstats = entry.getValue() + add;
				}
				stats.append(Integer.toHexString(entry.getKey())).append("#").append(Integer.toHexString(entry.getValue() + add)).append("#0#0#").append("0d0+").append(newstats);
			} else {
				stats.append(Integer.toHexString(entry.getKey())).append("#").append(Integer.toHexString(entry.getValue())).append("#0#0#").append("0d0+").append(entry.getValue());
			}
			isFirst = false;
		}

		for (Entry<Integer, String> entry : obj.txtStats.entrySet()) {
			if (!isFirst) {
				stats.append(",");
			}
			stats.append(Integer.toHexString(entry.getKey())).append("#0#0#0#").append(entry.getValue());
			isFirst = false;
		}
		return stats.toString();
	}

	public String parseFMEchecStatsString(Objects obj, double poid) {
		StringBuilder stats = new StringBuilder("");
		boolean isFirst = true;
		for (SpellEffect SE : obj.Effects) {
			if (!isFirst) {
				stats.append(",");
			}

			String[] infos = SE.getArgs().split(";");
			try {
				stats.append(Integer.toHexString(SE.getEffectID())).append("#").append(infos[0]).append("#").append(infos[1]).append("#0#").append(infos[5]);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			};

			isFirst = false;
		}

		for (Entry<Integer, Integer> entry : obj.Stats.getMap().entrySet()) {
			//En cas d'echec les stats nï¿½gatives Chance,Agi,Intel,Force,Portee,Vita augmentes
			int newstats = 0;

			if (entry.getKey() == 152
					|| entry.getKey() == 154
					|| entry.getKey() == 155
					|| entry.getKey() == 157
					|| entry.getKey() == 116
					|| entry.getKey() == 153) {
				float a = (float) ((entry.getValue() * poid) / 100);
				if (a < 1) {
					a = 1;
				}
				float chute = (float) (entry.getValue() + a);
				newstats = (int) Math.floor(chute);
				//On limite la chute du nï¿½gatif a sont maximum
				if (newstats > Job.getBaseMaxJet(obj.getTemplate().getID(), Integer.toHexString(entry.getKey()))) {
					newstats = Job.getBaseMaxJet(obj.getTemplate().getID(), Integer.toHexString(entry.getKey()));
				}
			} else {
				if (entry.getKey() == 127 || entry.getKey() == 101) {
					continue;//PM, pas de nï¿½gatif ainsi que PA
				}
				float chute = (float) (entry.getValue() - ((entry.getValue() * poid) / 100));
				newstats = (int) Math.floor(chute);
			}
			if (newstats < 1) {
				continue;
			}
			if (!isFirst) {
				stats.append(",");
			}
			stats.append(Integer.toHexString(entry.getKey())).append("#").append(Integer.toHexString(newstats)).append("#0#0#").append("0d0+").append(newstats);
			isFirst = false;
		}

		for (Entry<Integer, String> entry : obj.txtStats.entrySet()) {
			if (!isFirst) {
				stats.append(",");
			}
			stats.append(Integer.toHexString(entry.getKey())).append("#0#0#0#").append(entry.getValue());
			isFirst = false;
		}
		/*if (is_linked())
		{
		if(!isFirst)stats+=",";
		stats += linkedItem.parse_speakingStates();
		isFirst = false;
		}*/
		return stats.toString();
	}

	public Stats generateNewStatsFromTemplate(String statsTemplate, boolean useMax) {
		Stats itemStats = new Stats(false, null);
		//Si stats Vides
		if (statsTemplate.equals("") || statsTemplate == null) {
			return itemStats;
		}

		String[] splitted = statsTemplate.split(",");
		for (String s : splitted) {
			String[] stats = s.split("#");
			int statID = Integer.parseInt(stats[0], 16);
			boolean follow = true;

			for (int a : Constant.ARMES_EFFECT_IDS)//Si c'est un Effet Actif
			{
				if (a == statID) {
					follow = false;
				}
			}
			if (!follow) {
				continue;//Si c'ï¿½tait un effet Actif d'arme
			}
			String jet = "";
			int value = 1;
			try {
				jet = stats[4];
				value = Formulas.getRandomJet(jet);
				if (useMax) {
					try {
						//on prend le jet max
						int min = Integer.parseInt(stats[1], 16);
						int max = Integer.parseInt(stats[2], 16);
						value = min;
						if (max != 0) {
							value = max;
						}
					} catch (Exception e) {
						value = Formulas.getRandomJet(jet);
					}
				}
			} catch (Exception e) {
			}
			itemStats.addOneStat(statID, value);
		}
		return itemStats;
	}

	public void setStats(Stats SS) {
		Stats = SS;
	}

	public void set_Template(int Tid)
	{
		this.template = World.getObjTemplate(Tid);
	}
	
	public static int getPoidOfActualItem(String statsTemplate)//Donne le poid de l'item actuel
	{
		int poid = 0;
		int somme = 0;
		String[] splitted = statsTemplate.split(",");
		for (String s : splitted) {
			String[] stats = s.split("#");
			int statID = Integer.parseInt(stats[0], 16);
			boolean follow = true;

			for (int a : Constant.ARMES_EFFECT_IDS)//Si c'est un Effet Actif
			{
				if (a == statID) {
					follow = false;
				}
			}
			if (!follow) {
				continue;//Si c'ï¿½tait un effet Actif d'arme
			}
			String jet = "";
			int value = 1;
			try {
				jet = stats[4];
				value = Formulas.getRandomJet(jet);
				try {
					//on prend le jet max
					int min = Integer.parseInt(stats[1], 16);
					int max = Integer.parseInt(stats[2], 16);
					value = min;
					if (max != 0) {
						value = max;
					}
				} catch (Exception e) {
					value = Formulas.getRandomJet(jet);
				};
			} catch (Exception e) {
			};

			int multi = 1;
			if (statID == 118 || statID == 126 || statID == 125 || statID == 119 || statID == 123 || statID == 158 || statID == 174)//Force,Intel,Vita,Agi,Chance,Pod,Initiative
			{
				multi = 1;
			} else if (statID == 138 || statID == 666 || statID == 226 || statID == 220)//Domages %,Domage renvoyï¿½,Piï¿½ge %
			{
				multi = 2;
			} else if (statID == 124 || statID == 176)//Sagesse,Prospec
			{
				multi = 3;
			} else if (statID == 240 || statID == 241 || statID == 242 || statID == 243 || statID == 244)//Rï¿½ Feu, Air, Eau, Terre, Neutre
			{
				multi = 4;
			} else if (statID == 210 || statID == 211 || statID == 212 || statID == 213 || statID == 214)//Rï¿½ % Feu, Air, Eau, Terre, Neutre
			{
				multi = 5;
			} else if (statID == 225)//Piï¿½ge
			{
				multi = 15;
			} else if (statID == 178 || statID == 112)//Soins,Dommage
			{
				multi = 20;
			} else if (statID == 115 || statID == 182)//Cri,Invoc
			{
				multi = 30;
			} else if (statID == 117)//PO
			{
				multi = 50;
			} else if (statID == 128)//PM
			{
				multi = 90;
			} else if (statID == 111)//PA
			{
				multi = 100;
			}
			poid = value * multi; //poid de la carac
			somme += poid;
		}
		return somme;
	}

	public static int getPoidOfBaseItem(int i)//Donne le poid de l'item actuel
	{

		int poid = 0;
		int somme = 0;
		ObjTemplate t = World.getObjTemplate(i);
		String[] splitted = t.getStrTemplate().split(",");

		if (t.getStrTemplate().isEmpty()) {
			return 0;
		}
		for (String s : splitted) {
			String[] stats = s.split("#");
			int statID = Integer.parseInt(stats[0], 16);
			boolean follow = true;

			for (int a : Constant.ARMES_EFFECT_IDS)//Si c'est un Effet Actif
			{
				if (a == statID) {
					follow = false;
				}
			}
			if (!follow) {
				continue;//Si c'ï¿½tait un effet Actif d'arme
			}
			String jet = "";
			int value = 1;
			try {
				jet = stats[4];
				value = Formulas.getRandomJet(jet);
				try {
					//on prend le jet max
					int min = Integer.parseInt(stats[1], 16);
					int max = Integer.parseInt(stats[2], 16);
					value = min;
					if (max != 0) {
						value = max;
					}
				} catch (Exception e) {
					value = Formulas.getRandomJet(jet);
				};
			} catch (Exception e) {
			};

			int multi = 1;
			if (statID == 118 || statID == 126 || statID == 125 || statID == 119 || statID == 123 || statID == 158 || statID == 174)//Force,Intel,Vita,Agi,Chance,Pod,Initiative
			{
				multi = 1;
			} else if (statID == 138 || statID == 666 || statID == 226 || statID == 220)//Domages %,Domage renvoyï¿½,Piï¿½ge %
			{
				multi = 2;
			} else if (statID == 124 || statID == 176)//Sagesse,Prospec
			{
				multi = 3;
			} else if (statID == 240 || statID == 241 || statID == 242 || statID == 243 || statID == 244)//Rï¿½ Feu, Air, Eau, Terre, Neutre
			{
				multi = 4;
			} else if (statID == 210 || statID == 211 || statID == 212 || statID == 213 || statID == 214)//Rï¿½ % Feu, Air, Eau, Terre, Neutre
			{
				multi = 5;
			} else if (statID == 225)//Piï¿½ge
			{
				multi = 15;
			} else if (statID == 178 || statID == 112)//Soins,Dommage
			{
				multi = 20;
			} else if (statID == 115 || statID == 182)//Cri,Invoc
			{
				multi = 30;
			} else if (statID == 117)//PO
			{
				multi = 50;
			} else if (statID == 128)//PM
			{
				multi = 90;
			} else if (statID == 111)//PA
			{
				multi = 100;
			}
			poid = value * multi; //poid de la carac
			somme += poid;
		}
		return somme;
	}
	/* *********FM SYSTEM********* */

	public ArrayList<SpellEffect> getEffects()
	{
		return Effects;
	}

	public ArrayList<SpellEffect> getCritEffects()
	{
		ArrayList<SpellEffect> effets = new ArrayList<SpellEffect>();
		for(SpellEffect SE : Effects)
		{
			try
			{
				boolean boost = true;
				for(int i : Constant.NO_BOOST_CC_IDS)if(i == SE.getEffectID())boost = false;
				String[] infos = SE.getArgs().split(";");
				if(!boost)
				{
					effets.add(SE);
					continue;
				}
				int min = Integer.parseInt(infos[0],16)+ (boost?template.getBonusCC():0);
				int max = Integer.parseInt(infos[1],16)+ (boost?template.getBonusCC():0);
				if(max < min) max = min;
				String jet = "1d"+(max-min+1)+"+"+(min-1);
				//exCode: String newArgs = Integer.toHexString(min)+";"+Integer.toHexString(max)+";-1;-1;0;"+jet;
				//osef du minMax, vu qu'on se sert du jet pour calculer les dégats
				String newArgs = "0;0;0;-1;0;"+jet;
				effets.add(new SpellEffect(SE.getEffectID(),newArgs,0,-1));
			}catch(Exception e){continue;};
		}
		return effets;
	}

	public Characters.BoostSpellStats getBoostSpellStats() {
		return SpellStats;
	}

	public static Objects getCloneObjet(Objects obj,int qua)
	{
		Objects ob = new Objects(World.getNewItemGuid(), obj.getTemplate().getID(), qua,Constant.ITEM_POS_NO_EQUIPED, obj.getStats(), obj.getEffects(), new Characters.BoostSpellStats(obj.getBoostSpellStats()));
		return ob;
	}

	public void clearStats()
	{
		//On vide l'item de tous ces effets
		Stats = new Stats();
		Effects.clear();
		txtStats.clear();
	}

	public String parseStringStatsEC_FM(Objects obj, double poid) {
		String stats = "";
		boolean first = false;
		for (SpellEffect EH : obj.Effects) {
			if (first)
				stats += ",";
			String[] infos = EH.getArgs().split(";");
			try {
				stats += Integer.toHexString(EH.getEffectID()) + "#" + infos[0] + "#" + infos[1] + "#0#" + infos[5];
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			first = true;
		}
		for (Entry<Integer, Integer> entry : obj.Stats.getMap().entrySet()) {
			int newstats = 0;
			int statID = (entry.getKey());
			int value = (entry.getValue());
			if ((statID == 152) || (statID == 154) || (statID == 155) || (statID == 157) || (statID == 116)
			|| (statID == 153)) {
				float a = (float) (value * poid / 100.0D);
				if (a < 1.0F)
					a = 1.0F;
				float chute = value + a;
				newstats = (int) Math.floor(chute);
				if (newstats > Job.getBaseMaxJet(obj.getTemplate().getID(), Integer.toHexString(entry.getKey()))) {
					newstats = Job.getBaseMaxJet(obj.getTemplate().getID(), Integer.toHexString(entry.getKey()));
				}
			} else {
				if ((statID == 127) || (statID == 101))
					continue;
				float chute = (float) (value - value * poid / 100.0D);
				newstats = (int) Math.floor(chute);
			}
			if (newstats < 1)
				continue;
			String jet = "0d0+" + newstats;
			if (first)
				stats += ",";
			stats += Integer.toHexString(statID) + "#" + Integer.toHexString(newstats) + "#0#0#" + jet;
			first = true;
		}
		for (Entry<Integer, String> entry : obj.txtStats.entrySet()) {
			if (first)
				stats += ",";
			stats += Integer.toHexString((entry.getKey())) + "#0#0#0#" + entry.getValue();
			first = true;
		}
		return stats;
	}
	
}

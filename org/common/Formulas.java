package org.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.client.Characters;
import org.client.Characters.Stats;
import org.common.World.Couple;
import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.object.Challenge;
import org.fight.object.Collector;
import org.fight.object.Monster.MobGroup;
import org.kernel.Config;
import org.object.Guild;
import org.object.Maps;
import org.object.Objects;
import org.object.Guild.GuildMember;
import org.spell.SpellEffect;

public class Formulas {

	public static int getRandomValue(int min, int max) {
		Random rand = new Random();
		if (min > max) {
			int i = min;
			min = max;
			max = i;
		}
		return (rand.nextInt((max) + 1)) + min;
	}

	public static int getRandomValueOf(int... values) {
		return values[Formulas.getRandomValue(0, values.length - 1)];
	}
	
	public static int getRandomJet(String stat)// 1d5+6
	{
		int num = 0;
		try {
			int des = Integer.parseInt(stat.split("d")[0]);
			int faces = Integer.parseInt(stat.split("d")[1].split("\\+")[0]);
			int add = Integer.parseInt(stat.split("d")[1].split("\\+")[1]);			
			for (int a = 0; a < des; a++)
				num += getRandomValue(1, faces);			
			num += add;
		} 
		catch (NumberFormatException e) {	
			e.printStackTrace();
		}
		return num;
	}
	
	public static int getMiddleJet(String stat)// 1d5+6
	{
		int num = 0;
		try {
			int des = Integer.parseInt(stat.split("d")[0]);
			int faces = Integer.parseInt(stat.split("d")[1].split("\\+")[0]);
			int add = Integer.parseInt(stat.split("d")[1].split("\\+")[1]);
			num += ((1 + faces) / 2) * des;// on calcule moyenne
			num += add;			
		} 
		catch (NumberFormatException e) {
		}
		return num;
	}
	
	public static int getTacleChance(Fighter tacleur, ArrayList<Fighter> tacle) //Formule  de tacle pour Manesia |Return,Skryn
	{
		int chance = 0;
		int agiCible = tacleur.getTotalStats().getEffect(Constant.STATS_ADD_AGIL);
		int agiTacleur = 0;
		//Formule
		int a = Integer.parseInt(Config.TACLE_FORMULA.split(",")[0]);
		int b = Integer.parseInt(Config.TACLE_FORMULA.split(",")[1]);
		int c = Integer.parseInt(Config.TACLE_FORMULA.split(",")[2]);
		int d = Integer.parseInt(Config.TACLE_FORMULA.split(",")[3]);
		int e = Integer.parseInt(Config.TACLE_FORMULA.split(",")[4]);
		int f = Integer.parseInt(Config.TACLE_FORMULA.split(",")[5]);
		int g = Integer.parseInt(Config.TACLE_FORMULA.split(",")[6]);
		int h = Integer.parseInt(Config.TACLE_FORMULA.split(",")[7]);
		//Pourcentages
		int i = Integer.parseInt(Config.TACLE_PERCENTAGE.split(",")[0]);
		int j = Integer.parseInt(Config.TACLE_PERCENTAGE.split(",")[1]);
		int k = Integer.parseInt(Config.TACLE_PERCENTAGE.split(",")[2]);
		int l = Integer.parseInt(Config.TACLE_PERCENTAGE.split(",")[3]);
		int m = Integer.parseInt(Config.TACLE_PERCENTAGE.split(",")[4]);
		for (Fighter T : tacle)
			agiTacleur += T.getTotalStats().getEffect(Constant.STATS_ADD_AGIL);

		if (agiCible - agiTacleur < (-a))
			chance += i;
		if (agiCible - agiTacleur < b && agiCible - agiTacleur >= (-c))
			chance += j;
		if (agiCible - agiTacleur > d && agiCible - agiTacleur <= e)
			chance += k;
		if (agiCible - agiTacleur > f && agiCible - agiTacleur < g)
			chance += l;
		if (agiCible - agiTacleur >= h)
			chance += m;
		return chance;
	}

	public static int calculFinalHeal(Characters caster, int jet) {
		int statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_INTE);
		int soins = caster.getTotalStats().getEffect(Constant.STATS_ADD_SOIN);
		if (statC < 0)
			statC = 0;
		return jet * (100 + statC) / 100 + soins;
	}
	
	/*public static int calculFinalDommage(Fight fight,Fighter caster,Fighter target,int statID,int jet,boolean isHeal, boolean isCaC, int spellid)
	{
		return calculFinalDommage(fight, caster, target, statID, jet, isHeal, isCaC, spellid, false);
	}
	*/
	public static int calculFinalDommage(Fight fight,Fighter caster,Fighter target,int statID,int jet,boolean isHeal, boolean isCaC, int spellid, boolean isB)
	{
		float i = 0;// Bonus maitrise
		float j = 100; // Bonus de Classe
		float a = 1;// Calcul
		float num = 0;
		float statC = 0, domC = 0, perdomC = 0, resfT = 0, respT = 0;
		int multiplier = 0;
		if (!isHeal) {
			domC = caster.getTotalStats().getEffect(Constant.STATS_ADD_DOMA) + caster.getSpellStat(spellid, Constant.STATS_BOOST_SPELL_DMG);
			if(isCaC && caster.getPersonnage().get_classe() == 9 && caster.hasBuff(112) && caster.getBuff(112).getSpell() == 180) {
				domC = domC - caster.getBuff(112).getValue();
			}
			perdomC = caster.getTotalStats().getEffect(Constant.STATS_ADD_PERDOM);
			if(isCaC && caster.getPersonnage().get_classe() == 9 && caster.hasBuff(138) && caster.getBuff(138).getSpell() == 166) {
				perdomC = perdomC - caster.getBuff(138).getValue();
			}
			multiplier = caster.getTotalStats().getEffect(Constant.STATS_MULTIPLY_DOMMAGE);
		} else {
			domC = caster.getTotalStats().getEffect(Constant.STATS_ADD_SOIN) + caster.getSpellStat(spellid, Constant.STATS_BOOST_SPELL_HEAL);
		}

		Stats casterStats = caster.getTotalStats();
		Stats targetStats;
		if (isB)
			targetStats = target.getTotalStatsLessBuff();
		else
			targetStats = target.getTotalStats();
		switch (statID) {
		case Constant.ELEMENT_NULL:// Fixe
			statC = 0;
			resfT = 0;
			respT = 0;
			respT = 0;
			break;
		case Constant.ELEMENT_NEUTRE:// neutre
			statC = casterStats.getEffect(Constant.STATS_ADD_FORC);
			resfT = targetStats.getEffect(Constant.STATS_ADD_R_NEU);
			respT = targetStats.getEffect(Constant.STATS_ADD_RP_NEU);
			if (caster.getPersonnage() != null)// Si c'est un joueur
			{
				respT += targetStats.getEffect(Constant.STATS_ADD_RP_PVP_NEU);
				resfT += targetStats.getEffect(Constant.STATS_ADD_R_PVP_NEU);				
			}
			// on ajoute les dom Physique
			domC += casterStats.getEffect(142);
			// Ajout de la resist Physique
			resfT += targetStats.getEffect(184);
			break;
		case Constant.ELEMENT_TERRE:// force
			statC = casterStats.getEffect(Constant.STATS_ADD_FORC);
			resfT = targetStats.getEffect(Constant.STATS_ADD_R_TER);
			respT = targetStats.getEffect(Constant.STATS_ADD_RP_TER);
			if (caster.getPersonnage() != null)// Si c'est un joueur
			{
				respT += targetStats.getEffect(Constant.STATS_ADD_RP_PVP_TER);
				resfT += targetStats.getEffect(Constant.STATS_ADD_R_PVP_TER);
			}
			// on ajout les dom Physique
			domC += casterStats.getEffect(142);
			// Ajout de la resist Physique
			resfT += targetStats.getEffect(184);
			break;
		case Constant.ELEMENT_EAU:// chance
			statC = casterStats.getEffect(Constant.STATS_ADD_CHAN);
			resfT = targetStats.getEffect(Constant.STATS_ADD_R_EAU);
			respT = targetStats.getEffect(Constant.STATS_ADD_RP_EAU);
			if (caster.getPersonnage() != null)// Si c'est un joueur
			{
				respT += targetStats.getEffect(Constant.STATS_ADD_RP_PVP_EAU);
				resfT += targetStats.getEffect(Constant.STATS_ADD_R_PVP_EAU);
			}
			// Ajout de la resist Magique
			resfT = targetStats.getEffect(183);
			break;
		case Constant.ELEMENT_FEU:// intell
			statC = casterStats.getEffect(Constant.STATS_ADD_INTE);
			resfT = targetStats.getEffect(Constant.STATS_ADD_R_FEU);
			respT = targetStats.getEffect(Constant.STATS_ADD_RP_FEU);
			if (caster.getPersonnage() != null)// Si c'est un joueur
			{
				respT += targetStats.getEffect(Constant.STATS_ADD_RP_PVP_FEU);
				resfT += targetStats.getEffect(Constant.STATS_ADD_R_PVP_FEU);
			}
			// Ajout de la resist Magique
			resfT += targetStats.getEffect(183);
			break;
		case Constant.ELEMENT_AIR:// agilité
			statC = casterStats.getEffect(Constant.STATS_ADD_AGIL);
			resfT = targetStats.getEffect(Constant.STATS_ADD_R_AIR);
			respT = targetStats.getEffect(Constant.STATS_ADD_RP_AIR);
			if (caster.getPersonnage() != null)// Si c'est un joueur
			{
				respT += targetStats.getEffect(Constant.STATS_ADD_RP_PVP_AIR);
				resfT += targetStats.getEffect(Constant.STATS_ADD_R_PVP_AIR);
			}
			// Ajout de la resist Magique
			resfT += targetStats.getEffect(183);
			break;
		}
		
		//On bride la resistance a 50% si c'est un joueur 
		if (target.getMob() == null && respT > 50)
			respT = 50;
		if (statC < 0)
			statC = 0;
		if (isB) {
			switch (spellid) {
			case 181:
			case 200:
				if (target.hasBuff(184)) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID() + "", target.getGUID() + "," + target.getBuff(184).getValue());
					return 0;
				}
			}
			num = (jet * ((100 + statC + perdomC + (multiplier * 100)) / 100)) + domC;
			if (!isHeal)
				num -= resfT;// resis fixe
			int reduc = (int) ((num / 100) * respT);// Reduc %resis
			if (!isHeal)
				num -= reduc;
			return (int) num;
		}
		if (caster.getPersonnage() != null && isCaC) {
			int ArmeType = caster.getPersonnage().getObjetByPos(1).getTemplate().getType();
			if ((caster.getSpellValueBool(392) == true) && ArmeType == 2)// ARC
			{
				i = caster.getMaitriseDmg(392);
			}
			if ((caster.getSpellValueBool(390) == true) && ArmeType == 4)// BATON
			{
				i = caster.getMaitriseDmg(390);
			}
			if ((caster.getSpellValueBool(391) == true) && ArmeType == 6)// EPEE
			{
				i = caster.getMaitriseDmg(391);
			}
			if ((caster.getSpellValueBool(393) == true) && ArmeType == 7)// MARTEAUX
			{
				i = caster.getMaitriseDmg(393);
			}
			if ((caster.getSpellValueBool(394) == true) && ArmeType == 3)// BAGUETTE
			{
				i = caster.getMaitriseDmg(394);
			}
			if ((caster.getSpellValueBool(395) == true) && ArmeType == 5)// DAGUES
			{
				i = caster.getMaitriseDmg(395);
			}
			if ((caster.getSpellValueBool(396) == true) && ArmeType == 8)// PELLE
			{
				i = caster.getMaitriseDmg(396);
			}
			if ((caster.getSpellValueBool(397) == true) && ArmeType == 19)// HACHE
			{
				i = caster.getMaitriseDmg(397);
			}
			a = (((100 + i) / 100) * (j / 100));
		}
		num = a * (jet * ((100 + statC + perdomC + (multiplier * 100)) / 100)) + domC;// dégats bruts	
		
		//Poisons
		if (spellid != -1) {
			switch (spellid) {
			/*
			 * case [SPELLID]: statC =
			 * caster.getTotalStats().getEffect([EFFECT]) num = (jet * ((100 +
			 * statC + perdomC + (multiplier*100)) / 100 ))+ domC; return (int)
			 * num;
			 */
			case 66:
				statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_AGIL);
				num = (jet * ((100 + statC + perdomC + (multiplier * 100)) / 100)) + domC;
				if (target.hasBuff(105)) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID() + "", target.getGUID() + "," + target.getBuff(105).getValue());
					return 0;
				}
				if (target.hasBuff(184)) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID() + "", target.getGUID() + "," + target.getBuff(184).getValue());
					return 0;
				}
				return (int) num;

			case 71:
			case 196:
			case 219:
				statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_FORC);
				num = (jet * ((100 + statC + perdomC + (multiplier * 100)) / 100)) + domC;
				if (target.hasBuff(105)) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID() + "", target.getGUID() + "," + target.getBuff(105).getValue());
					return 0;
				}
				if (target.hasBuff(184)) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID() + "", target.getGUID() + "," + target.getBuff(184).getValue());
					return 0;
				}
				return (int) num;

			case 181:
			case 200:
				statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_INTE);
				num = (jet * ((100 + statC + perdomC + (multiplier * 100)) / 100)) + domC;
				if (target.hasBuff(105)) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID() + "", target.getGUID() + "," + target.getBuff(105).getValue());
					return 0;
				}
				if (target.hasBuff(184)) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID() + "", target.getGUID() + "," + target.getBuff(184).getValue());
					return 0;
				}
				return (int) num;
			}
		}
		
		//Renvoie
		int renvoie = target.getTotalStatsLessBuff().getEffect(Constant.STATS_RETDOM);
		if (renvoie > 0 && !isHeal) {
			if (renvoie > num)
				renvoie = (int) num;
			num -= renvoie;
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 107, "-1", target.getGUID() + "," + renvoie);
			if (renvoie > caster.getPDV())
				renvoie = caster.getPDV();
			if (num < 1)
				num = 0;
			caster.removePDV(renvoie);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID() + "", caster.getGUID() + ",-" + renvoie);
		}
		if (!isHeal)
			num -= resfT;// resis fixe
		int reduc = (int) ((num / 100) * respT);// Reduc %resis
		if (!isHeal)
			num -= reduc;
		int armor = getArmorResist(target, statID);
		if (!isHeal)
			num -= armor;
		if (!isHeal)
			if (armor > 0)
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID() + "", target.getGUID() + "," + armor);
		// dégats finaux
		if (num < 1)
			num = 0;
		
		// Début Formule pour les MOBs
		if(caster.getPersonnage() == null && !caster.isPerco())
		{
			if(caster.getMob().getTemplate().getID() == 116)//Sacrifié Dommage = PDV*2
			{
				return (int)((num/25)*caster.getPDVMAX());
			}
			else
			{
			int niveauMob = caster.get_lvl();
			double CalculCoef = ((niveauMob*0.5)/100);
			int Multiplicateur = (int) Math.ceil(CalculCoef);
			return (int)num*Multiplicateur;
			}
		}
		// Fin Formule pour les MOBs
		else
		{
			return (int)num;
		}
	}

	public static int calculZaapCost(Maps map1, Maps map2) {
		return 10 * (Math.abs(map2.getX() - map1.getX()) + Math.abs(map2.getY() - map1.getY()) - 1);
	}
	
	public static int zaapPrice(Characters perso, Maps start, Maps end) {    
	    double a = Math.pow((end.getX() - (start.getX())), 2);
	    double b = Math.pow((end.getY() - (start.getY())), 2);
	    double distance = Math.sqrt(a + b);
	    
	    double reduction = 0;

        if (perso.get_align() == end.getSubArea().getalignement())
        	reduction = 0.1;
	        
	    return (int) (distance - (distance * reduction));
	}
	
	public static int[] getAdjacentCells(int cellId) {
	    int[] cells = new int[4];
	    int cellWidth = 15;

	    cells[0] = (cellId - cellWidth);
	    cells[1] = ((cellId - cellWidth) + 1);
	    cells[2] = ((cellId + cellWidth) - 1);
	    cells[3] = (cellId + cellWidth);
	    
	    return cells;
	}
	
	private static int getFecaArmorValue(Fighter target, int statID) {
		double armor = 0;
		Fighter fighter = null;		
		for (SpellEffect SE : target.getBuffsByEffectID(265)) {			
			switch (SE.getSpell()) {
				case 1:// Armure incandescente
						// Si pas element feu, on ignore l'armure
					if (statID != Constant.ELEMENT_FEU)
						continue;
					// Les stats du féca sont prises en compte
					fighter = SE.getCaster();
					break;
				case 6:// Armure Terrestre
						// Si pas element terre/neutre, on ignore l'armure
					if (statID != Constant.ELEMENT_TERRE && statID != Constant.ELEMENT_NEUTRE)
						continue;
					// Les stats du féca sont prises en compte
					fighter = SE.getCaster();
					break;
				case 14:// Armure Venteuse
					// Si pas element air, on ignore l'armure
					if (statID != Constant.ELEMENT_AIR)
						continue;
					// Les stats du féca sont prises en compte
					fighter = SE.getCaster();
					break;
				case 18:// Armure aqueuse
					// Si pas element eau, on ignore l'armure
					if (statID != Constant.ELEMENT_EAU)
						continue;
					// Les stats du féca sont prises en compte
					fighter = SE.getCaster();
					break;
			}
			if(fighter != null) 
				armor = 0.08333 * fighter.getTotalStats().getEffect(Constant.STATS_ADD_INTE);			
		}		
		if(fighter == null) 
			return 0;		
		return 75 + (int)armor;
	}
	
	private static int getArmorResist(Fighter target, int statID) {
		int armor = 0;
		int fecaArmorValue = getFecaArmorValue(target, statID);
		for (SpellEffect SE : target.getBuffsByEffectID(265)) {
			Fighter fighter;
			switch (SE.getSpell()) {
			case 1:
			case 6:
			case 14:
			case 18:
				continue;
			default:// Dans les autres cas on prend les stats de la cible et on ignore l'element de l'attaque
				fighter = target;
				break;
			}
			int intell = fighter.getTotalStats().getEffect(Constant.STATS_ADD_INTE);
			int carac = 0;
			switch (statID) {
			case Constant.ELEMENT_AIR:
				carac = fighter.getTotalStats().getEffect(Constant.STATS_ADD_AGIL);
				break;
			case Constant.ELEMENT_FEU:
				carac = fighter.getTotalStats().getEffect(Constant.STATS_ADD_INTE);
				break;
			case Constant.ELEMENT_EAU:
				carac = fighter.getTotalStats().getEffect(Constant.STATS_ADD_CHAN);
				break;
			case Constant.ELEMENT_NEUTRE:
			case Constant.ELEMENT_TERRE:
				carac = fighter.getTotalStats().getEffect(Constant.STATS_ADD_FORC);
				break;
			}
			int value = SE.getValue();
			int a = value * (100 + intell / 2 + carac / 2) / 100;
			armor += a;
		}
		for (SpellEffect SE : target.getBuffsByEffectID(105)) {
			int intell = target.getTotalStats().getEffect(Constant.STATS_ADD_INTE);
			int carac = 0;
			switch (statID) {
			case Constant.ELEMENT_AIR:
				carac = target.getTotalStats().getEffect(Constant.STATS_ADD_AGIL);
				break;
			case Constant.ELEMENT_FEU:
				carac = target.getTotalStats().getEffect(Constant.STATS_ADD_INTE);
				break;
			case Constant.ELEMENT_EAU:
				carac = target.getTotalStats().getEffect(Constant.STATS_ADD_CHAN);
				break;
			case Constant.ELEMENT_NEUTRE:
			case Constant.ELEMENT_TERRE:
				carac = target.getTotalStats().getEffect(Constant.STATS_ADD_FORC);
				break;
			}
			int value = SE.getValue();
			int a = value * (100 + intell / 2 + carac / 2) / 100;
			armor += a;
		}	
		return armor + fecaArmorValue;
	}

	public static int getPointsLost(char z, int value, Fighter caster, Fighter target) {
		float esquiveC = z == 'a' ? caster.getTotalStats().getEffect(Constant.STATS_ADD_AFLEE) : caster.getTotalStats().getEffect(Constant.STATS_ADD_MFLEE);
		float esquiveT = z == 'a' ? target.getTotalStats().getEffect(Constant.STATS_ADD_AFLEE) : target.getTotalStats().getEffect(Constant.STATS_ADD_MFLEE);
		float ptsMax = z == 'a' ? target.getTotalStatsLessBuff().getEffect(Constant.STATS_ADD_PA) : target.getTotalStatsLessBuff().getEffect(Constant.STATS_ADD_PM);
		int retrait = 0;
		for (int i = 0; i < value; i++) {
			if (ptsMax == 0 && target.getMob() != null) {
				ptsMax = z == 'a' ? target.getMob().getPA() : target.getMob().getPM();
			}
			float pts = z == 'a' ? target.getPA() : target.getPM();
			float ptsAct = pts - retrait;
			if (esquiveT == 0)
				esquiveT = 1;
			if (esquiveC == 0)
				esquiveC = 1;
			float a = esquiveC / esquiveT;
			float b = (ptsAct / ptsMax);
			float pourcentage = a * b * 50;
			int chance = (int) Math.ceil(pourcentage);
			if (chance < 0)
				chance = 0;
			if (chance > 100)
				chance = 100;
			int jet = getRandomValue(0, 99);
			if (jet < chance) {
				retrait++;
			}
		}
		return retrait;
	}
	
	public static long getXpWinPerco(Collector perco, ArrayList<Fighter> winners,ArrayList<Fighter> loosers,long groupXP)
	{
			Guild G = World.getGuild(perco.get_guildID());
			float sag = G.get_Stats(Constant.STATS_ADD_SAGE);
			float coef = (sag + 100)/100;
			int taux = Config.XP_PVM;
			long xpWin = 0;
			int lvlmax = 0;
			for(Fighter entry : winners)
			{
				if(entry.get_lvl() > lvlmax)
					lvlmax = entry.get_lvl();
			}
			int nbbonus = 0;
			for(Fighter entry : winners)
			{
				if(entry.get_lvl() > (lvlmax / 3))
					nbbonus += 1;				
			}
			
			double bonus = 1;
			if(nbbonus == 2)
				bonus = 1.1;
			if(nbbonus == 3)
				bonus = 1.3;
			if(nbbonus == 4)
				bonus = 2.2;
			if(nbbonus == 5)
				bonus = 2.5;
			if(nbbonus == 6)
				bonus = 2.8;
			if(nbbonus == 7)
				bonus = 3.1;
			if(nbbonus >= 8)
				bonus = 3.5;
			
			int lvlLoosers = 0;
			for(Fighter entry : loosers)
				lvlLoosers += entry.get_lvl();
			int lvlWinners = 0;
			for(Fighter entry : winners)
				lvlWinners += entry.get_lvl();
			double rapport = 1+((double)lvlLoosers/(double)lvlWinners);
			if (rapport <= 1.3)
				rapport = 1.3;
			/*
			if (rapport > 5)
				rapport = 5;
			//*/
			int lvl = G.get_lvl();
			double rapport2 = 1 + ((double)lvl / (double)lvlWinners);

			xpWin = (long) (groupXP * rapport * bonus * taux *coef * rapport2);
			
			/*/ DEBUG XP
			System.out.println("=========");
			System.out.println("groupXP: "+groupXP);
			System.out.println("rapport1: "+rapport);
			System.out.println("bonus: "+bonus);
			System.out.println("taux: "+taux);
			System.out.println("coef: "+coef);
			System.out.println("rapport2: "+rapport2);
			System.out.println("xpWin: "+xpWin);
			System.out.println("=========");
			//*/
			return xpWin;	
	}
	
	public static long getXpWinPvm2(Fighter perso, ArrayList<Fighter> winners,ArrayList<Fighter> loosers,long groupXP)
	{
		if(perso.getPersonnage()== null)return 0;
		if(winners.contains(perso))//Si winner
		{
			float sag = perso.getTotalStats().getEffect(Constant.STATS_ADD_SAGE);
			float coef = (sag + 100)/100;
			int taux = Config.XP_PVM;
			long xpWin = 0;
			int lvlmax = 0;
			for(Fighter entry : winners)
			{
				if(entry.get_lvl() > lvlmax)
					lvlmax = entry.get_lvl();
			}
			int nbbonus = 0;
			for(Fighter entry : winners)
			{
				if(entry.get_lvl() > (lvlmax / 3))
					nbbonus += 1;				
			}
			
			double bonus = 1;
			if(nbbonus == 2)
				bonus = 1.1;
			if(nbbonus == 3)
				bonus = 1.3;
			if(nbbonus == 4)
				bonus = 2.2;
			if(nbbonus == 5)
				bonus = 2.5;
			if(nbbonus == 6)
				bonus = 2.8;
			if(nbbonus == 7)
				bonus = 3.1;
			if(nbbonus >= 8)
				bonus = 3.5;
			
			int lvlLoosers = 0;
			for(Fighter entry : loosers)
				lvlLoosers += entry.get_lvl();
			int lvlWinners = 0;
			for(Fighter entry : winners)
				lvlWinners += entry.get_lvl();
			double rapport = 1+((double)lvlLoosers/(double)lvlWinners);
			if (rapport <= 1.3)
				rapport = 1.3;
			/*
			if (rapport > 5)
				rapport = 5;
			//*/
			int lvl = perso.get_lvl();
			double rapport2 = 1 + ((double)lvl / (double)lvlWinners);

			xpWin = (long) (groupXP * rapport * bonus * taux *coef * rapport2);
			return xpWin;	
		}
		return 0;
	}
	
	public static long getXpWinPvm3(Fighter perso, ArrayList<Fighter> team,ArrayList<Fighter> loose, long groupXP)
	{
		//int lvlwin = 0;
		//for(Fighter entry : team)lvlwin += entry.get_lvl();
		int lvllos = 0;
		for(Fighter entry : loose)lvllos += entry.get_lvl();
		float bonusSage = (perso.getTotalStats().getEffect(Constant.STATS_ADD_SAGE)+100)/100;
		/* Formule 1
		float taux = perso.get_lvl()/lvlwin;
		long xp = (long)(groupXP * taux * bonusSage * perso.get_lvl());
		//*/
		//* Formule 2
		long sXp = groupXP*lvllos;
		long gXp = 2 * groupXP * perso.get_lvl();
        long xp = (long)((sXp + gXp)*bonusSage);
		//*/
		return xp*Config.XP_PVM;
	}
	
	public static long getXpWinPvm(ArrayList<Fighter> team, ArrayList<Fighter> loose, int sagesse, MobGroup _mobGroup, Map<Integer, Challenge> _challenges) {

		/** Monsters **/
		// Group Level Monsters
		int GroupLevelMonsters = 0;
		for (Fighter entry : loose) 
			GroupLevelMonsters += entry.get_lvl();
		
		// Group Xp Monsters
		long GroupXpMonsters = 0;
		for (Fighter mob : loose)
			if(mob.getMob() != null && !mob.isInvocation())
				GroupXpMonsters += mob.getMob().getBaseXp();	
		
		// Best mob level
		int BestMobLevel = 0;
		for (Fighter mob : loose) 
			if(BestMobLevel < mob.get_lvl())
				BestMobLevel = mob.get_lvl();
					
		/** Fight **/
		// Total Rates Xp Challenge
		long TotalRatesXpChallenge = 0;
		for (Map.Entry<Integer, Challenge> c : _challenges.entrySet()) {
			if ((c.getValue() == null) || (!c.getValue().get_win()))
				continue;
			TotalRatesXpChallenge += c.getValue().get_gainXp();
		}
		
		// Stars
		int NumberOfStars = 0;
		if(_mobGroup != null)
			_mobGroup.get_bonusValue(); // return [0;100]
				
		/** Player(s) **/
		// Get significant number of players
		int SignificantNumberOfPlayers = 0;
		for (Fighter player : team)
			if(player.get_lvl() >= (1/3) * BestMobLevel)
				SignificantNumberOfPlayers++;

		// Number of players
		int NumberOfPlayers = 0;
		// Team level
		int TeamLevel = 0;
		for (Fighter player : team) {
			NumberOfPlayers++;
			TeamLevel += player.get_lvl();
		}
		
		
		/** Formule experience **/ // http://forum.dofus.com/fr/1003-divers/1644516-formule-xp
		// Bonus coefficient
		double BonusCoefficient = 1 + ( (sagesse + ((double)NumberOfStars * 20) + TotalRatesXpChallenge) / 100 );
		
		// Significant number of players (PlayerLevel >= (1/3) BestMobLevel)
		double multiplier = 0.5;
		switch(SignificantNumberOfPlayers) {
			case 1 : multiplier = 1; break;
			case 2 : multiplier = 1.1; break;
			case 3 : multiplier = 1.5; break;
			case 4 : multiplier = 2.3; break;
			case 5 : multiplier = 3.1; break;
			case 6 : multiplier = 3.6; break;
			case 7 : multiplier = 4.2; break;
			case 8 : multiplier = 4.7; break;
		}
		
		// Multiplier Group Level
		double MultiplierGroupLevel = 0;
		if(GroupLevelMonsters >= TeamLevel)
			MultiplierGroupLevel = 1 - (((double)GroupLevelMonsters - (double)TeamLevel) / GroupLevelMonsters);
		else 
			MultiplierGroupLevel = 1 - (((double)TeamLevel - (double)GroupLevelMonsters) / TeamLevel);
			
		// Formule
		long xp = (long) (BonusCoefficient * (multiplier + MultiplierGroupLevel) * (GroupXpMonsters / NumberOfPlayers));		
		return xp * Config.XP_PVM;
	}
	
	public static long getXpWinPvP(Fighter perso, ArrayList<Fighter> winners, ArrayList<Fighter> looser) {
		if (perso.getPersonnage() == null)
			return 0;
		if (winners.contains(perso))// Si winner
		{
			int lvlLoosers = 0;
			for (Fighter entry : looser)
				lvlLoosers += entry.get_lvl();

			int lvlWinners = 0;
			for (Fighter entry : winners)
				lvlWinners += entry.get_lvl();
			int taux = Config.XP_PVP;
			float rapport = (float) lvlLoosers / (float) lvlWinners;
			long xpWin = (long) ((rapport * getXpNeededAtLevel(perso.getPersonnage().get_lvl()) / 100) * taux);
			// DEBUG
			System.out.println("Taux: " + taux);
			System.out.println("Rapport: " + rapport);
			System.out.println("XpNeeded: " + getXpNeededAtLevel(perso.getPersonnage().get_lvl()));
			System.out.println("xpWin: " + xpWin);
			// */
			return xpWin * Config.XP_PVP;
		}
		return 0;
	}
	
	private static long getXpNeededAtLevel(int lvl)
	{
		long xp = (World.getPersoXpMax(lvl) - World.getPersoXpMin(lvl));
		System.out.println("Xp Max => "+World.getPersoXpMax(lvl));
		System.out.println("Xp Min => "+World.getPersoXpMin(lvl));
		
		return xp;
	}

	public static long getGuildXpWin(Fighter perso, AtomicReference<Long> xpWin) {
		if (perso.getPersonnage() == null)
			return 0;
		if (perso.getPersonnage().getGuildMember() == null)
			return 0;
		GuildMember gm = perso.getPersonnage().getGuildMember();
		double xp = xpWin.get(), Lvl = perso.get_lvl(), LvlGuild = perso.getPersonnage().get_guild().get_lvl(),
				pXpGive = (double) gm.getPXpGive() / 100;
		double maxP = xp * pXpGive * 0.10;// Le maximum donné à la guilde est 0% du montant prélevé sur l'xp du combat
		double diff = Math.abs(Lvl - LvlGuild);// Calcul l'écart entre le niveau du personnage et le niveau de la guilde
		double toGuild;
		if (diff >= 70) {
			toGuild = maxP * 0.10;// Si l'écart entre les deux level est de 70 ou plus, l'experience donnée a la
									// guilde est de 10% la valeur maximum de don
		} else if (diff >= 31 && diff <= 69) {
			toGuild = maxP - ((maxP * 0.10) * (Math.floor((diff + 30) / 10)));
		} else if (diff >= 10 && diff <= 30) {
			toGuild = maxP - ((maxP * 0.20) * (Math.floor(diff / 10)));
		} else // Si la différence est [0,9]
		{
			toGuild = maxP;
		}
		xpWin.set((long) (xp - xp * pXpGive));
		return Math.round(toGuild);
	}
	
	public static long getMountXpWin(Fighter perso, AtomicReference<Long> xpWin)
	{
		if (perso.getPersonnage() == null)
			return 0;
		if (perso.getPersonnage().getMount() == null)
			return 0;
		int diff = Math.abs(perso.get_lvl() - perso.getPersonnage().getMount().get_level());
		double coeff = 0;
		double xp = xpWin.get();
		double pToMount = (double) perso.getPersonnage().getMountXpGive() / 100 + 0.2;
		if (diff >= 0 && diff <= 9)
			coeff = 0.1;
		else if (diff >= 10 && diff <= 19)
			coeff = 0.08;
		else if (diff >= 20 && diff <= 29)
			coeff = 0.06;
		else if (diff >= 30 && diff <= 39)
			coeff = 0.04;
		else if (diff >= 40 && diff <= 49)
			coeff = 0.03;
		else if (diff >= 50 && diff <= 59)
			coeff = 0.02;
		else if (diff >= 60 && diff <= 69)
			coeff = 0.015;
		else
			coeff = 0.01;
		if (pToMount > 0.2)
			xpWin.set((long) (xp - (xp * (pToMount - 0.2))));
		return Math.round(xp * pToMount * coeff);
	}

	public static int getKamasWin(Fighter i, ArrayList<Fighter> winners, int maxk, int mink)
	{
		maxk++;
		int rkamas = (int)(Math.random() * (maxk-mink)) + mink;
		return rkamas*Config.KAMAS;
	}
	
	public static int getKamasWinPerco(int maxk, int mink)
	{
		maxk++;
		int rkamas = (int)(Math.random() * (maxk-mink)) + mink;
		return rkamas*Config.KAMAS;
	}
	
	public static int calculElementChangeChance(int lvlM,int lvlA,int lvlP)
	{
		int K = 350;
		if(lvlP == 1)K = 100;
		else if (lvlP == 25)K = 175;
		else if (lvlP == 50)K = 350;
		return (int)((lvlM*100)/(K + lvlA));
	}

	public static int calculHonorWinPrisms(ArrayList<Fighter> winner, ArrayList<Fighter> looser, Fighter F) {
		float totalGradeWin = 0;
		float totalLevelWin = 0;
		float totalGradeLoose = 0;
		float totalLevelLoose = 0;
		boolean Prisme = false;
		int fighters = 0;
		for (Fighter  f : winner) {
			if (f.getPersonnage() == null && f.getPrisme() == null)
				continue;
			if (f.getPersonnage() != null) {
				totalLevelWin += f.get_lvl();
				totalGradeWin += f.getPersonnage().getGrade();
			} else {
				Prisme = true;
				totalLevelWin += 200;
				totalGradeWin += (f.getPrisme().getlevel() * 20) + 100;
			}
		}
		for (Fighter f : looser) {
			if (f.getPersonnage() == null && f.getPrisme() == null)
				continue;
			if (f.getPersonnage() != null) {
				totalLevelLoose += f.get_lvl();
				totalGradeLoose += f.getPersonnage().getGrade();
				fighters++;
			} else {
				Prisme = true;
				totalLevelLoose += 200;
				totalGradeLoose += (f.getPrisme().getlevel() * 15) + 100;
			}
		}
		if (!Prisme)
			if (totalLevelWin - totalLevelLoose > 15 * fighters)
				return 0;
		int base = (int) (100 * (float) ( (totalGradeLoose * totalLevelLoose) / (totalGradeWin * totalLevelWin)))
				/ winner.size();
		if (looser.contains(F))
			base = -base;
		return base * Config.HONOR;
	}
	
	public static int calculHonorWin(ArrayList<Fighter> winners, ArrayList<Fighter> loosers, Fighter F) {
		float totalGradeWin = 0;
		float totalLevelWin = 0;
		float totalGradeLoose = 0;
		float totalLevelLoose = 0;
		for (Fighter f : winners) {
			if (f.getPersonnage() == null)
				continue;
			totalLevelWin += f.get_lvl();
			totalGradeWin += f.getPersonnage().getGrade();

		}
		for (Fighter f : loosers) {
			if (f.getPersonnage() == null)
				continue;
			totalLevelLoose += f.get_lvl();
			totalGradeLoose += f.getPersonnage().getGrade();

		}

		if (totalLevelWin - totalLevelLoose > 10)
			return 0;

		int base = (int) (100 * (float) (totalGradeLoose / totalGradeWin)) / winners.size();
		if (loosers.contains(F))
			base = -base;
		else
			return getRandomValue(100, 300);
		if (F.getPersonnage() == null)
			return base;
		int loose = (int) Math.ceil(F.getPersonnage().get_honor() * 5 / 100);
		if (loose < 50)
			loose = 50;
		if (F.getPersonnage().get_honor() < loose)
			loose = F.getPersonnage().get_honor();

		return -loose * Config.XP_PVP;
		// return base * Ancestra.HONOR;
	}
	
	public static int calculHonorWin2(ArrayList<Fighter> winners,ArrayList<Fighter> loosers,Fighter F) 
	{
		float totalGradeWin = 0, totalLevelWin = 0, totalGradeLoose = 0, totalLevelLoose = 0;
		int xpWin = 0;
		int winnerSize = 0;
		int looserSize = 0;
		
		for (Fighter f : winners) {
			if (f.getPersonnage() == null)
				continue;
			totalLevelWin += f.get_lvl();
			totalGradeWin += f.getPersonnage().getGrade();
			winnerSize++;
		}

		for (Fighter f : loosers) {
			if (f.getPersonnage() == null)
				continue;
			totalLevelLoose += f.get_lvl();
			totalGradeLoose += f.getPersonnage().getGrade();
			looserSize++;
		}
		
		// Found opponents
		ArrayList<Fighter> opponents;
		if (loosers.contains(F))
			opponents = winners;
		else 
			opponents = loosers;
		
		// Check same IP for the current fighter
		
		boolean SameIPIntoTwoFighters = false;
		/*
		for (Fighter f : opponents) {
			if (f.getPersonnage() == null)
				continue;
			if(f.getPersonnage().get_compte().get_curIP().equals(f.getPersonnage().get_compte().get_curIP()))
				SameIPIntoTwoFighters = true;
		}
		*/
		
		// Check same fighters Construct TeamId
		ArrayList<Integer> guids = new ArrayList<Integer>();
		for (Fighter f : opponents) {
			if (f.getPersonnage() == null)
				continue; // Prisme
			guids.add(f.getPersonnage().get_GUID());
		}
		Collections.sort(guids);
		String TeamId = "";
		for (Integer s : guids)
			TeamId += s.toString()+";";
			// Check if exists
		int SameFighters = 0;
		if(F.getPersonnage().getLastPvPFighters().containsKey(TeamId))
			SameFighters = F.getPersonnage().getLastPvPFighters().get(TeamId);
			// Add team
		F.getPersonnage().AddLastPvPFighters(TeamId);
		
		// Ecart de level < 15 par joueur (8*15=120) et IP différente
		if (totalLevelWin - totalLevelLoose < 15*looserSize && Math.abs(totalLevelWin - totalLevelLoose) < 75*looserSize && !SameIPIntoTwoFighters) {

			//int base = (int) ( (totalLevelLoose / totalLevelWin) * (totalGradeLoose / totalGradeWin) * 200 ) / winnerSize;
			if (loosers.contains(F))
				xpWin = (int) -( F.getPersonnage().get_honor()*0.07 ); // 7%			
			else {
				int EcartGrades = (int) ( (totalGradeLoose / looserSize) - (totalGradeWin / winnerSize) ); // Max = 10
				int EcartNiveaux = (int) ( (totalLevelLoose / looserSize) - (totalLevelWin / winnerSize) ); // Max = 15*8 = 120
				xpWin = (int) ( 350 * ( 1 + EcartGrades*0.1 ) * ( 1 + EcartNiveaux*0.02 ) ); // Max = 350*2*3.4
				
				if(xpWin>1000)
					xpWin = 1000;				
			}
			xpWin = (int) ( xpWin / (SameFighters*SameFighters + 1) );			
		}
		return (int) ( xpWin*Config.HONOR );
	}
	
	public static Couple<Integer, Integer> decompPierreAme(Objects toDecomp)
	{
		Couple<Integer, Integer> toReturn;
		String[] stats = toDecomp.parseStatsString().split("#");
		int lvlMax = Integer.parseInt(stats[3],16);
		int chance = Integer.parseInt(stats[1],16);
		toReturn = new Couple<Integer,Integer>(chance,lvlMax);
		
		return toReturn;
	}
	
	public static int totalCaptChance(int pierreChance, Characters p)
	{
		int sortChance = 0;

		switch(p.getSortStatBySortIfHas(413).getLevel())
		{
			case 1:
				sortChance = 1;
				break;
			case 2:
				sortChance = 3;
				break;
			case 3:
				sortChance = 6;
				break;
			case 4:
				sortChance = 10;
				break;
			case 5:
				sortChance = 15;
				break;
			case 6:
				sortChance = 25;
				break;
		}
		
		return sortChance + pierreChance;
	}
	
	public static String parseReponse(String reponse)
	{
		StringBuilder toReturn = new StringBuilder("");
		
		String[] cut = reponse.split("[%]");
		
		if(cut.length == 1)return reponse;
		
		toReturn.append(cut[0]);
		
		char charact;
		for (int i = 1; i < cut.length; i++)
		{
			charact = (char) Integer.parseInt(cut[i].substring(0, 2),16);
			toReturn.append(charact).append(cut[i].substring(2));
		}
		
		return toReturn.toString();
	}
	
	public static int spellCost(int nb)
	{
		int total = 0;
		for (int i = 1; i < nb ; i++)
		{
			total += i;
		}
		
		return total;
	}
	
	public static int chanceFM(int WeightTotalBase, int currentWeithTotal, int currentWeightStats, int weight, int diff,
			float coef) {
				float chance = 0.0F;
				float a = ((WeightTotalBase + diff) * coef * Config.RATE_FM);
				float b = (float) (Math.sqrt(currentWeithTotal + currentWeightStats) + weight);
				if (b < 1.0)
					b = 1.0F;
				chance = a / b;
				return (int) chance;
			}
	
	public static int getTraqueXP(int lvl)
	{
		if (lvl < 50)
			return 10000 * Config.XP_PVM;
		if (lvl < 60)
			return 65000 * Config.XP_PVM;
		if (lvl < 70)
			return 90000 * Config.XP_PVM;
		if (lvl < 80)
			return 120000 * Config.XP_PVM;
		if (lvl < 90)
			return 160000 * Config.XP_PVM;
		if (lvl < 100)
			return 210000 * Config.XP_PVM;
		if (lvl < 110)
			return 270000 * Config.XP_PVM;
		if (lvl < 120)
			return 350000 * Config.XP_PVM;
		if (lvl < 130)
			return 440000 * Config.XP_PVM;
		if (lvl < 140)
			return 540000 * Config.XP_PVM;
		if (lvl < 150)
			return 650000 * Config.XP_PVM;
		if (lvl < 155)
			return 760000 * Config.XP_PVM;
		if (lvl < 160)
			return 880000 * Config.XP_PVM;
		if (lvl < 165)
			return 1000000 * Config.XP_PVM;
		if (lvl < 170)
			return 1130000 * Config.XP_PVM;
		if (lvl < 175)
			return 1300000 * Config.XP_PVM;
		if (lvl < 180)
			return 1500000 * Config.XP_PVM;
		if (lvl < 185)
			return 1700000 * Config.XP_PVM;
		if (lvl < 190)
			return 2000000 * Config.XP_PVM;
		if (lvl < 195)
			return 2500000 * Config.XP_PVM;
		if (lvl < 200)
			return 3000000 * Config.XP_PVM;
		return 0;
	}
	
	public static int getLoosEnergy(int lvl, boolean isAgression, boolean isPerco)
	{
		int returned = 25*lvl;
		if(isAgression) returned *= (7/4);
		if(isPerco) returned *= (3/2);
		return returned;
	}
	
	public static int getChanceCapa(int i1,int i2, int i3)
	{
		int chanceHaveCapa = 0;
		int rand = Formulas.getRandomValue(1, 100);
        switch(rand)
        {
                case 1:
                	chanceHaveCapa = i1;
                break;
                case 2:
                	chanceHaveCapa = i2;
               break;
                case 3:
                	chanceHaveCapa = i3;
               break;
                case 4:
                	chanceHaveCapa = 0;
                	break;
                case 5:
                	chanceHaveCapa = 0;
                	break;
                case 6:
                	chanceHaveCapa = 0;
                	break;
                case 7:
                	chanceHaveCapa = 0;
                	break;
                case 8:
                	chanceHaveCapa = 0;
                	break;
                case 9:
                	chanceHaveCapa = 0;
                	break;
                case 10:
                	chanceHaveCapa = 0;
                	break;
                case 11:
                	chanceHaveCapa = 0;
                	break;
                case 12:
                	chanceHaveCapa = 0;
                	break;
                case 13:
                	chanceHaveCapa = 0;
                	break;
                case 14:
                	chanceHaveCapa = 0;
                	break;
                case 15:
                	chanceHaveCapa = 0;
                	break;
                case 16:
                	chanceHaveCapa = 0;
                	break;
                case 17:
                	chanceHaveCapa = 0;
                	break;
                case 18:
                	chanceHaveCapa = 0;
                	break;
                case 19:
                	chanceHaveCapa = 0;
                	break;
                case 20:
                	chanceHaveCapa = 0;
                	break;
                case 21:
                	chanceHaveCapa = 0;
                break;
                case 22:
                	chanceHaveCapa = 0;
               break;
                case 23:
                	chanceHaveCapa = 0;
               break;
                case 24:
                	chanceHaveCapa = 0;
                	break;
                case 25:
                	chanceHaveCapa = 0;
                	break;
                case 26:
                	chanceHaveCapa = 0;
                	break;
                case 27:
                	chanceHaveCapa = 0;
                	break;
                case 28:
                	chanceHaveCapa = 0;
                	break;
                case 29:
                	chanceHaveCapa = 0;
                	break;
                case 30:
                	chanceHaveCapa = 0;
                	break;
                case 31:
                	chanceHaveCapa = 0;
                break;
                case 32:
                	chanceHaveCapa = 0;
               break;
                case 33:
                	chanceHaveCapa = 0;
               break;
                case 34:
                	chanceHaveCapa = 0;
                	break;
                case 35:
                	chanceHaveCapa = 0;
                	break;
                case 36:
                	chanceHaveCapa = 0;
                	break;
                case 37:
                	chanceHaveCapa = 0;
                	break;
                case 38:
                	chanceHaveCapa = 0;
                	break;
                case 39:
                	chanceHaveCapa = 0;
                	break;
                case 40:
                	chanceHaveCapa = 0;
                	break;
                case 41:
                	chanceHaveCapa = 0;
                break;
                case 42:
                	chanceHaveCapa = 0;
               break;
                case 43:
                	chanceHaveCapa = 0;
               break;
                case 44:
                	chanceHaveCapa = 0;
                	break;
                case 45:
                	chanceHaveCapa = 0;
                	break;
                case 46:
                	chanceHaveCapa = 0;
                	break;
                case 47:
                	chanceHaveCapa = 0;
                	break;
                case 48:
                	chanceHaveCapa = 0;
                	break;
                case 49:
                	chanceHaveCapa = 0;
                	break;
                case 50:
                	chanceHaveCapa = 0;
                	break;
                case 51:
                	chanceHaveCapa = 0;
                break;
                case 52:
                	chanceHaveCapa = 0;
               break;
                case 53:
                	chanceHaveCapa = 0;
               break;
                case 54:
                	chanceHaveCapa = 0;
                	break;
                case 55:
                	chanceHaveCapa = 0;
                	break;
                case 56:
                	chanceHaveCapa = 0;
                	break;
                case 57:
                	chanceHaveCapa = 0;
                	break;
                case 58:
                	chanceHaveCapa = 0;
                	break;
                case 59:
                	chanceHaveCapa = 0;
                	break;
                case 60:
                	chanceHaveCapa = 0;
                	break;
                case 61:
                	chanceHaveCapa = 0;
                break;
                case 62:
                	chanceHaveCapa = 0;
               break;
                case 63:
                	chanceHaveCapa = 0;
               break;
                case 64:
                	chanceHaveCapa = 0;
                	break;
                case 65:
                	chanceHaveCapa = 0;
                	break;
                case 66:
                	chanceHaveCapa = 0;
                	break;
                case 67:
                	chanceHaveCapa = 0;
                	break;
                case 68:
                	chanceHaveCapa = 0;
                	break;
                case 69:
                	chanceHaveCapa = 0;
                	break;
                case 70:
                	chanceHaveCapa = 0;
                	break;
                case 71:
                	chanceHaveCapa = 0;
                break;
                case 72:
                	chanceHaveCapa = 0;
               break;
                case 73:
                	chanceHaveCapa = 0;
               break;
                case 74:
                	chanceHaveCapa = 0;
                	break;
                case 75:
                	chanceHaveCapa = 0;
                	break;
                case 76:
                	chanceHaveCapa = 0;
                	break;
                case 77:
                	chanceHaveCapa = 0;
                	break;
                case 78:
                	chanceHaveCapa = 0;
                	break;
                case 79:
                	chanceHaveCapa = 0;
                	break;
                case 80:
                	chanceHaveCapa = 0;
                	break;
                case 81:
                	chanceHaveCapa = 0;
                break;
                case 82:
                	chanceHaveCapa = 0;
               break;
                case 83:
                	chanceHaveCapa = 0;
               break;
                case 84:
                	chanceHaveCapa = 0;
                	break;
                case 85:
                	chanceHaveCapa = 0;
                	break;
                case 86:
                	chanceHaveCapa = 0;
                	break;
                case 87:
                	chanceHaveCapa = 0;
                	break;
                case 88:
                	chanceHaveCapa = 0;
                	break;
                case 89:
                	chanceHaveCapa = 0;
                	break;
                case 90:
                	chanceHaveCapa = 0;
                	break;
                case 91:
                	chanceHaveCapa = 0;
                break;
                case 92:
                	chanceHaveCapa = 0;
               break;
                case 93:
                	chanceHaveCapa = 0;
               break;
                case 94:
                	chanceHaveCapa = 0;
                	break;
                case 95:
                	chanceHaveCapa = 0;
                	break;
                case 96:
                	chanceHaveCapa = 0;
                	break;
                case 97:
                	chanceHaveCapa = 0;
                	break;
                case 98:
                	chanceHaveCapa = 0;
                	break;
                case 99:
                	chanceHaveCapa = 0;
                	break;
                case 100:
                	chanceHaveCapa = 0;
                	break;
                	
        }
                return chanceHaveCapa;
        }
	public static int totalAppriChance(int FiletChance, Characters p)
	{
		return FiletChance;
	}

	public static int ChoseIn3Time(int a, int b, int c) {
		int TheWinner = 0;
		int rand = Formulas.getRandomValue(1, 3);
        switch(rand)
        {
        case 1:
        	TheWinner = a;
        	break;
        case 2:
        	TheWinner = b;
        	break;
        case 3:
        	TheWinner = c;
        	break;
        }
        return TheWinner;
	}
	public static int calculateChanceByElement(int lvlJob, int lvlObject, int lvlRune) {
		int K = 1;
		if (lvlRune == 1)
			K = 100;
		else if (lvlRune == 25)
			K = 175;
		else if (lvlRune == 50)
			K = 350;
		return lvlJob * 100 / (K + lvlObject);
	}
	
}
package org.fight.IA.type;

import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.IA.utils.Function;

public class IA_01 {
	private static boolean stop = false;
	
	public static void apply(Fighter F, Fight fight) {
		while (!stop && F.canPlay()) {
			int PDVPER = (F.getPDV() * 100) / F.getPDVMAX();
			Fighter T = Function.getNearestEnnemy(fight, F); // Ennemis
			Fighter T2 = Function.getNearestFriend(fight, F); // Amis
			if (T == null) {
				return;
			}
			if (PDVPER > 15) {
				int attack = Function.attackIfPossible(fight, F);
				if (attack != 0)// Attaque
				{
					if (attack == 5) {
						stop = true;// EC
						break;
					}
					if (!Function.moveToAttackIfPossible(fight, F)) 
					{
						if (!Function.buffIfPossible(fight, F, F))// auto-buff
						{
							if (!Function.HealIfPossible(fight, F, false))// soin alli�
							{
								if (!Function.buffIfPossible(fight, F, T2))// buff alli�
								{
									if (T == null || !Function.moveNearIfPossible(fight, F, T))// avancer
									{
										if (!Function.invocIfPossible(fight, F))// invoquer
										{
											stop = true;
											break;
										}
									}
								}
							}
						}
					}
				}
			} else {
				if (!Function.HealIfPossible(fight, F, true))// auto-soin
				{
					int attack = Function.attackIfPossible(fight, F);
					if (attack != 0)// Attaque
					{
						if (attack == 5) {
							stop = true;// EC
							break;
						}
						if (!Function.moveToAttackIfPossible(fight, F)) {
							attack = Function.attackIfPossible(fight, F);
							if (attack != 0) { // retente l'attaque
								if (attack == 5) {
									stop = true; // EC
									break;
								}
								if (!Function.buffIfPossible(fight, F, F))// auto-buff
								{
									if (!Function.HealIfPossible(fight, F, false))// soin alli�
									{
										if (!Function.buffIfPossible(fight, F, T2))// buff alli�
										{
											if (!Function.invocIfPossible(fight, F)) {
												if (!Function.moveFarIfPossible(fight, F))// fuite
												{
													stop = true;
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
}

package org.fight.IA.type;

import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.IA.utils.Function;

public class IA_14 {
	public static void apply(Fighter F, Fight fight, boolean stop) {
		while (!stop && F.canPlay()) {
			int PDVPER = (F.getPDV() * 100) / F.getPDVMAX();
			Fighter T = Function.getNearestEnnemy(fight, F); // Ennemis
			Fighter T2 = Function.getNearestFriend(fight, F); // Amis
			
			if (T == null || T.isDead()) {
				T = Function.getNearestEnnemy(fight, F);
			}
			if (T == null) {
				return;
			}
			
			if (PDVPER > 15) {
				if (Function.moveToAttackIfPossible(fight, F)) {
					int attack = Function.attackIfPossible(fight, T);
					if (attack != 0)// Attaque
					{
						if (attack == 5) { // EC
							stop = true;
							break;
						}
						if (!Function.buffIfPossible(fight, F, F))// auto-buff
						{
							if (!Function.HealIfPossible(fight, F, false))// soin allié
							{
								if (!Function.buffIfPossible(fight, F, T2))// buff allié
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
				} else {
					stop = true;
					break;
				}
			} else {
				if (!Function.HealIfPossible(fight, F, true))// auto-soin
				{
					int attack = Function.attackIfPossible(fight, T);
					if (attack != 0)// Attaque
					{
						if (attack == 5){
							stop = true;
							break;
						}
						if (!Function.moveFarIfPossible(fight, F))// fuite
						{
							if (!Function.buffIfPossible(fight, F, F))// auto-buff
							{
								if (!Function.HealIfPossible(fight, F, false))// soin allié
								{
									if (!Function.buffIfPossible(fight, F, T2))// buff allié
									{
										if (!Function.invocIfPossible(fight, F)) {
											stop = true;
											break;
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

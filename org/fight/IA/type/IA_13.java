package org.fight.IA.type;

import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.IA.utils.Function;

public class IA_13 {
	private static boolean stop = false;
	
	public static void apply(Fighter F, Fight fight) {
		while (!stop && F.canPlay()) {
			Fighter T = Function.getNearestFriend(fight, F);
			if (!Function.HealIfPossible(fight, F, false))// soin alli�
			{
				if (!Function.buffIfPossible(fight, F, T))// buff alli�
				{
					if (!Function.moveNearIfPossible(fight, F, T))// Avancer vers
															// alli�
					{
						if (!Function.HealIfPossible(fight, F, true))// auto-soin
						{
							if (!Function.buffIfPossible(fight, F, F))// auto-buff
							{
								if (!Function.invocIfPossible(fight, F)) {
									T = Function.getNearestEnnemy(fight, F);
									int attack = Function.attackIfPossible(fight, F);
									if (attack != 0)// Attaque
									{
										if (attack == 5) {
											stop = true;// EC
											break;
										}
										if (!Function.moveFarIfPossible(fight, F))// fuite
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
			}
		}
	}
}

package org.fight.IA.type;

import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.IA.utils.Function;

public class IA_06 {
	private static boolean stop = false;
	
	public static void apply(Fighter F, Fight fight) {
		while (!stop && F.canPlay()) {
			if (!Function.invocIfPossible(fight, F)) {
				Fighter T = Function.getNearestFriend(fight, F);
				if (!Function.HealIfPossible(fight, F, false))// soin alli�
				{
					if (!Function.buffIfPossible(fight, F, T))// buff alli�
					{
						if (!Function.buffIfPossible(fight, F, F))// buff alli�
						{
							if (!Function.HealIfPossible(fight, F, true)) {
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

package org.fight.IA.type;

import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.IA.utils.Function;

public class IA_04 {
	public static void apply(Fighter F, Fight fight, boolean stop) {
		while (!stop && F.canPlay()) {
			Fighter T = Function.getNearestEnnemy(fight, F);
			if (T == null) {
				stop = true;
				break;
			}
			int attack = Function.attackIfPossible(fight, F);
			if (attack != 0)// Attaque
			{
				if (attack == 5) {
					stop = true;// EC
					break;
				}
				if (!Function.moveFarIfPossible(fight, F))// fuite
				{
					if (!Function.HealIfPossible(fight, F, false))// soin alli�
					{
						if (!Function.buffIfPossible(fight, F, T))// buff alli�
						{
							if (!Function.HealIfPossible(fight, F, true))// auto-soin
							{
								if (!Function.invocIfPossible(fight, F)) {
									if (!Function.buffIfPossible(fight, F, F))// auto-buff
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

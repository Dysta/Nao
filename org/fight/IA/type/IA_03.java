package org.fight.IA.type;

import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.IA.utils.Function;

public class IA_03 {
	private static boolean stop = false;

	public static void apply(Fighter F, Fight fight) {
		while (!stop && F.canPlay()) {
			Fighter T = Function.getNearestFriend(fight, F);
			if (!Function.moveNearIfPossible(fight, F, T))// Avancer vers alli�
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
								}
							}
						}
					}
				}
			}
		}
	}
}

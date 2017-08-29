package org.fight.IA.type;

import org.common.Pathfinding;
import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.IA.utils.Function;

public class IA_02 {
	public static void apply(Fighter F, Fight fight, boolean stop) {
		boolean modeAttack = false;
		Fighter T = Function.getNearestFriend(fight, F);
		Fighter E = null;
		if (Pathfinding.getDistanceBetween(fight.get_map(), F.get_fightCell().getID(),
				T.get_fightCell().getID()) < 4)
			modeAttack = true;
		while (!stop && F.canPlay()) {
			if (modeAttack) {
				T = Function.getNearestFriend(fight, F);
				if (E == null || E.isDead()) {
					E = Function.getNearestEnnemy(fight, F);
				}
				if (!Function.HealIfPossible(fight, F, false))// soin allié
				{
					if (!Function.buffIfPossible(fight, F, T))// buff allié
					{
						if (!Function.HealIfPossible(fight, F, true))// auto-soin
						{
							if (!Function.buffIfPossible(fight, F, F))// auto-buff
							{
								if (!Function.invocIfPossible(fight, F)) {
									if (!Function.moveToAttackIfPossible(fight, F))// Avancer vers ennemie
									{
										int attack = Function.attackIfPossible(fight, F);
										if (attack == 5) {
											stop = true;
											break;
										}
										if (attack != 0)// Attaque
										{
											stop = true;// EC
											break;
										}
									}
								}
							}
						}
					}
				}

			} else {
				T = Function.getNearestFriend(fight, F);
				if (!Function.HealIfPossible(fight, F, false))// soin allié
				{
					if (!Function.buffIfPossible(fight, F, T))// buff allié
					{
						if (!Function.moveNearIfPossible(fight, F, T))// Avancer
																// vers
																// allié
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
											if (!Function.moveFarIfPossible(fight, F)) {// fuite
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
}

package org.fight.IA.type;

import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.IA.utils.Function;

public class IA_12 {
	public static void apply(Fighter F, Fight fight, boolean stop) {
		int must_stop = 0;
		Fighter T = null;
		while (must_stop < 3 && F.canPlay()) {
			if (T == null || T.isDead()) {
				T = Function.getNearestEnnemy(fight, F);
			}
			if (T == null)
				return;

			int attack = Function.attackIfPossible(fight, T);
			if (attack != 0)// Attaque
			{
				if (attack == 5)
					must_stop = 3;// EC
				if (!Function.moveNearIfPossible(fight, F, T)) {

					if (!Function.moveFarIfPossible(fight, F))// fuite
					{
						stop = true;
					}

				}
			}

			if (stop) {
				must_stop++;
				stop = false;
				T = null;
			}
		}
	}
}

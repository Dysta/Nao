package org.fight.IA.type;

import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.IA.utils.Function;

public class IA_05 {
	public static void apply(Fighter F, Fight fight, boolean stop) {
		while (!stop && F.canPlay()) {
			Fighter T = Function.getNearestEnnemy(fight, F);
			if (T == null)
				return;
			
			// Avancer vers enemis
			if (!Function.moveNearIfPossible(fight, F, T))
				stop = true;
			
		}
	}
}

package org.fight.IA.type;

import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.IA.utils.Function;

public class IA_09 {
	public static void apply(Fighter fighter, Fight fight, boolean stop) {
		int chan = 0;
		while (!stop && fighter.canPlay()) {
			if (++chan >= 12)
				stop = true;
			if (chan > 15)
				return;
			if (!Function.buffIfPossible(fight, fighter, fighter)) {
				int attack = Function.attackIfPossible(fight, fighter);
				while (attack == 0 && !stop) {
					if (attack == 5)
						stop = true;
					attack = Function.attackIfPossible(fight, fighter);
				}
			}
		}
	}
}

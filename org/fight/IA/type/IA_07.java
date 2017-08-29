package org.fight.IA.type;

import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.IA.utils.Function;

public class IA_07 {
	private static boolean stop = false;
	
	public static void apply(Fighter F, Fight fight) {
		int chan = 0;
		while (!stop && F.canPlay()) {
			if (++chan >= 12)
				stop = true;
			if (chan > 15)
				return;
			if (!Function.buffIfPossible(fight, F, F)) {
				int ataque = Function.attackIfPossible(fight, F);
				while (ataque == 0 && !stop) {
					if (ataque == 5)
						stop = true;
					ataque = Function.attackIfPossible(fight, F);
				}
				stop = true;
			}
		}
	}
}

package org.fight.IA.type;

import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.IA.utils.Function;

public class IA_08 {
	public static void apply(Fighter F, Fight fight, boolean stop) {
		int PDVPER = (F.getPDV() * 100) / F.getPDVMAX();
		int attack;
		while (!stop && F.canPlay()) {
			attack = Function.attackIfPossible(fight, F);// attaque
			if (attack != 0) {
				if (attack == 5) {
					stop = true;
					break;
				}
				if (PDVPER > 15) {
					if (!Function.moveToAttackIfPossible(fight, F)) {
						attack = Function.attackIfPossible(fight, F); // retente
																// l'attaque
						if (attack != 0) {
							if (attack == 5) {
								stop = true;
								break;
							}
							if (!Function.moveFarIfPossible(fight, F)) { // fuit
								if (!Function.buffIfPossible(fight, F, F)) { // auto
																	// buff
									if (!Function.HealIfPossible(fight, F, true)) { // auto
																			// soin
										stop = true;
										break;
									}
								}
							}
						}
					}
				} else {
					if (!Function.HealIfPossible(fight, F, true)) { // se soigne
						if (!Function.buffIfPossible(fight, F, F)) { // auto buff
							if (!Function.moveFarIfPossible(fight, F)) { // fuit
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

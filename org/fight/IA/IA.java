package org.fight.IA;

import org.fight.Fight;
import org.fight.Fight.Fighter;
import org.fight.IA.type.IA_01;
import org.fight.IA.type.IA_10;
import org.fight.IA.type.IA_12;
import org.fight.IA.type.IA_13;
import org.fight.IA.type.IA_14;
import org.fight.IA.type.IA_02;
import org.fight.IA.type.IA_03;
import org.fight.IA.type.IA_04;
import org.fight.IA.type.IA_05;
import org.fight.IA.type.IA_06;
import org.fight.IA.type.IA_07;
import org.fight.IA.type.IA_08;
import org.fight.IA.type.IA_09;
import org.fight.IA.utils.Function;

public class IA {

	public static class IAThread implements Runnable {

		private Fight _fight;
		private Fighter _fighter;
		private static boolean stop = false;
		private Thread _t;

		public IAThread(Fighter fighter, Fight fight) {
			_fighter = fighter;
			_fight = fight;
			_t = new Thread(this);
			_t.setDaemon(true);
			_t.start();
		}

		public Thread getThread() {
			return _t;
		}

		public void run() {
			stop = false;
			if (_fighter.getMob() == null) {
				if (_fighter.isDouble()) {
					IA_05.apply(_fighter, _fight);
					try {
						Thread.sleep(2000L);
					} catch (InterruptedException localInterruptedException) {
					}
					_fight.endTurn(true);
				} else if (_fighter.isPerco()) {
					apply_typePerco(_fighter, _fight);
					try {
						Thread.sleep(2000L);
					} catch (InterruptedException localInterruptedException1) {
					}
					_fight.endTurn(true);
				} else {
					try {
						Thread.sleep(2000L);
					} catch (InterruptedException localInterruptedException2) {
					}
					_fight.endTurn(true);
				}
			} else if (_fighter.getMob().getTemplate() == null) {
				_fight.endTurn(true);
			} else {
				int IA_Type = _fighter.getMob().getTemplate().getIAType();
				switch (IA_Type) {
				case 0:// Ne rien faire
					apply_type0(_fighter, _fight);
					break;
				case 1:// Attaque, Buff soi-même, Buff Alliés, Avancer vers ennemis.
					IA_01.apply(_fighter, _fight);
					break;
				case 2:// Soutien
					IA_02.apply(_fighter, _fight);
					break;
				case 3:// Avancer vers Alliés, Buff Alliés, Buff sois même
					IA_03.apply(_fighter, _fight);
					break;
				case 4:// Attaque, Fuite, Buff Alliés, Buff sois même
					IA_04.apply(_fighter, _fight);
					break;
				case 5:// IA propre aux bloqueurs (bloqueuse + double) (avance vers ennemi)
					IA_05.apply(_fighter, _fight);
					break;
				case 6:// IA type invocations
					IA_06.apply(_fighter, _fight);
					break;
				case 7: // IA type Tonneau
					IA_07.apply(_fighter, _fight);
					break;
				case 8: // IA type Cadran Xelor
					IA_08.apply(_fighter, _fight);
					break;
				case 9: // IA type Pandawasta
					IA_09.apply(_fighter, _fight);
					break;
				case 10:// IA specal
					IA_10.apply(_fighter, _fight);
					break;
				case 12:// IA Fuite (Tofu)
					IA_12.apply(_fighter, _fight);
					break;
				case 13:// IA sac animé
					IA_13.apply(_fighter, _fight);
					break;
				case 14:// Attaque, buff sois-même, buff allié mais n'avance pas
					IA_14.apply(_fighter, _fight);
					break;
				}
				try {
					if (IA_Type == 0) {
						Thread.sleep(250L);
					} else {
						Thread.sleep(2000L);
					}
				} catch (InterruptedException localInterruptedException3) {
				}
				if (!_fighter.isDead()) {
					_fight.endTurn(true);
				}
			}
		}

		// IA pourri de merde
		private static void apply_type0(Fighter F, Fight fight) {
			stop = true;
		}
		
		private static void apply_typePerco(Fight.Fighter F, Fight fight) {
			try {
				int noBoucle = 0;
				do {
					noBoucle++;
					if (noBoucle >= 12) {
						stop = true;
					}
					if (noBoucle > 15) {
						return;
					}
					Fight.Fighter T = Function.getNearestEnnemy(fight, F);
					if (T == null) {
						return;
					}
					int attack = Function.attackIfPossiblePerco(fight, F);
					if (attack != 0) {
						if (attack == 5) {
							stop = true;
						}
						if (!Function.moveFarIfPossible(fight, F)) {
							if (!Function.HealIfPossiblePerco(fight, F, false)) {
								if (!Function.buffIfPossiblePerco(fight, F, T)) {
									if (!Function.HealIfPossiblePerco(fight, F, true)) {
										if (!Function.buffIfPossiblePerco(fight, F, F)) {
											stop = true;
										}
									}
								}
							}
						}
					}
				} while (F.canPlay() && !stop);
			} catch (Exception e) {
				return;
			}
		}
	}

	
}

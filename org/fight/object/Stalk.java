package org.fight.object;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.client.Characters;
import org.common.Formulas;
import org.common.SocketManager;
import org.common.World;
import org.object.Objects;
import org.object.Objects.ObjTemplate;
import org.kernel.Config;

public class Stalk {

	public static final short DECONNEXION = 1;
	public static final short CONNEXION = 2;

	public static class TraqueLog {
		// Ici on met volontairement des ID plutot que des instances de perso
		// pour que ça gere meme en cas de déconnexion
		int _target = -1;
		long _time = -1;

		public TraqueLog(Characters target) {
			if (target == null)
				return;
			_target = target.get_GUID();
			_time = (long) System.currentTimeMillis() / 1000;
		}

		public boolean timeElapsed() {
			if (_time < (System.currentTimeMillis() / 1000) - 43200)
				return true;
			return false;
		}

		public int getTarget() {
			return _target;
		}
	}

	public static class Traque {
		private int _owner = -1;
		private int _target = -1;
		private long _lastTraque = -1;
		private boolean _validated = false;
		static ArrayList<TraqueLog> _logs = new ArrayList<TraqueLog>();

		public Traque(Characters owner) {
			_owner = owner.get_GUID();
		}

		public int getTarget() {
			return _target;
		}

		public int getOwner() {
			return _owner;
		}

		public boolean timeElapsed() {
			if (_lastTraque < ((long) System.currentTimeMillis() / 1000) - 900)
				return true;
			return false;
		}

		public void reset() {
			_target = -1;
			_validated = false;
		}

		public void newTraque(Characters p) {
			_lastTraque = (long) System.currentTimeMillis() / 1000;
			_target = p.get_GUID();
			_logs.add(new TraqueLog(p));
			_validated = false;
		}

		public void valider() {
			_validated = true;
		}

		public boolean isValidated() {
			return _validated;
		}

		public ArrayList<Integer> getForbidenTargets() {
			ArrayList<Integer> retour = new ArrayList<Integer>();
			for (TraqueLog tl : _logs) {
				if (!tl.timeElapsed())
					retour.add(tl.getTarget());
			}
			return retour;
		}
	}

	public static Map<Integer, Traque> _traques = new TreeMap<Integer, Traque>();

	public static Traque getTraqueByOwner(Characters p) {
		// On vérifie qu'il est instancié dans la liste
		if (!_traques.containsKey(p.get_GUID())) {
			synchronized (_traques) {
				_traques.put(p.get_GUID(), new Traque(p));
			}
		}
		return _traques.get(p.get_GUID());
	}

	public static void notifyToOwner(Characters p, short notifyType) {
		String message = "";
		if (p == null)
			return;
		if (notifyType == CONNEXION)
			message = new StringBuilder("Votre cible \"").append(p.get_name()).append("\" vient de se connecter")
					.toString();
		else if (notifyType == DECONNEXION) {
			message = new StringBuilder("Votre cible \"").append(p.get_name()).append("\" vient de se déconnecter")
					.toString();
		} else
			return;

		ArrayList<Traque> traques = new ArrayList<Traque>();
		synchronized (_traques) {
			traques.addAll(_traques.values());
		}
		for (Traque t : traques) {
			if (t.getTarget() != p.get_GUID())
				continue;
			Characters owner = World.getPersonnage(t.getOwner());
			if (owner == null || owner.get_compte() == null || owner.get_compte().getGameThread() == null
					|| !owner.isOnline())
				continue;
			SocketManager.GAME_SEND_MESSAGE(owner, message, "0000A0");
		}
	}

	public static ArrayList<Traque> getTraquesByTarget(Characters p) {
		if (p == null)
			return null;
		ArrayList<Traque> retour = new ArrayList<Traque>();
		ArrayList<Traque> traques = new ArrayList<Traque>();
		synchronized (_traques) {
			traques.addAll(_traques.values());
		}
		for (Traque t : traques) {
			if (t.getTarget() != p.get_GUID())
				continue;
			retour.add(t);
		}
		return retour;
	}

	public static void newTraque(Characters p) {
		// On vérifie qu'il n'y a pas d'erreur sur son compte
		if (p.get_compte() == null || p.get_compte().getGameThread() == null)
			return;
		// Puis on regarde s'il est bontarien ou brakmarien
		if (p.get_align() != 1 && p.get_align() != 2 && p.get_align() != 3) {
			SocketManager.GAME_SEND_MESSAGE(p, "Votre alignement ne vous permet pas de faire des traques.", "A00000");
			return;
		}
		// On regarde s'il a ses ailes
		if (!p.is_showWings()) {
			SocketManager.GAME_SEND_MESSAGE(p,
					"Vous devez activer le mode joueur contre joueur pour recevoir une traque.", "A00000");
			return;
		}
		// On vérifie qu'il est instancié dans la liste
		if (!_traques.containsKey(p.get_GUID())) {
			synchronized (_traques) {
				_traques.put(p.get_GUID(), new Traque(p));
			}
		}
		Traque traque = _traques.get(p.get_GUID());
		if (traque == null)
			return;// Ne devrai pas arriver
		// On vérifie si le mec a pas déjà demandé depuis moins de 15 minutes
		if (!traque.timeElapsed()) {
			SocketManager.GAME_SEND_MESSAGE(p, "Vous avez déjà demandé une traque il y a moins de 15 minutes.",
					"A00000");
			return;
		}
		// Bon bah si on est là on a fini nos vérifs ! 
		// C'est partis pour le script de sélection d'une cible
		ArrayList<Characters> targets = new ArrayList<Characters>();
		ArrayList<Integer> forbidens = traque.getForbidenTargets();
		for (Characters perso : World.getOnlinePersos()) {
			if (perso == null || perso.get_compte() == null || !perso.isOnline())
				continue;
			if (perso.get_compte().get_gmLvl() != 0)
				continue;
			// Vérifs d'alignement
			if (perso.get_align() == p.get_align() || perso.get_align() == 0 || !perso.is_showWings())
				continue;
			// On vérifie le niveau
			int diff = 0;
			if (perso.get_lvl() > p.get_lvl())
				diff = perso.get_lvl() - p.get_lvl();
			else
				diff = p.get_lvl() - perso.get_lvl();
			if (diff > Config.CONFIG_TRAQUE_DIFFERENCE)
				continue;
			// Vérifs d'ip
			if (perso.get_compte().get_curIP().equalsIgnoreCase(p.get_compte().get_curIP()))
				continue;
			// On vérifie qu'il est pas dans les interdits
			if (forbidens != null && forbidens.contains(perso.get_GUID()))
				continue;

			// Si on est là, c'est que c'est une cible potentielle, on l'ajoute
			targets.add(perso);
		}
		if (targets.size() == 0)// Pas de cibles...
		{
			SocketManager.GAME_SEND_Im_PACKET(p, "1198;");
			return;
		}
		// Configtenant on choisis une cible au hasard parmis les cibles potentielles
		int id = 0;
		if (targets.size() > 1)
			id = Formulas.getRandomValue(0, targets.size() - 1);
		// On a notre heureux élu !
		Characters target = targets.get(id);
		if (target == null)
			return; // Ne devrai pas arriver

		traque.newTraque(target);// On enregistre notre traque

		// Plus qu'à faire les ptits messages de routine
		SocketManager.GAME_SEND_MESSAGE(p,
				new StringBuilder("Vous êtes en traque de \"").append(target.get_name())
						.append("\", votre cible est de niveau ").append(target.get_lvl())
						.append(".").toString(),
				"00A000");
	}

	public static void getTraquePosition(Characters p) {
		if (!_traques.containsKey(p.get_GUID())) {
			SocketManager.GAME_SEND_MESSAGE(p, "Vous n'avez pas de traque actuellement", "A00000");
			return;
		}
		Traque traque = _traques.get(p.get_GUID());
		if (traque.getTarget() == -1) {
			SocketManager.GAME_SEND_MESSAGE(p, "Vous n'avez pas de traque actuellement", "A00000");
			return;
		}
		Characters perso = World.getPersonnage(traque.getTarget());
		if (perso == null || !perso.isOnline()) {
			SocketManager.GAME_SEND_MESSAGE(p, "Le personnage que vous traquez n'est pas connecté acutellement.",
					"A00000");
			return;
		}
		SocketManager.GAME_SEND_FLAG_PACKET(p, perso);
	}

	public static void getRecompense(Characters p) {
		if (!_traques.containsKey(p.get_GUID())) {
			SocketManager.GAME_SEND_MESSAGE(p, "Vous n'avez pas de traque actuellement.", "A00000");
			return;
		}
		Traque traque = _traques.get(p.get_GUID());
		if (traque.getTarget() == -1) {
			SocketManager.GAME_SEND_MESSAGE(p, "Vous n'avez pas de traque actuellement.", "A00000");
			return;
		}
		if (!traque.isValidated()) {
			SocketManager.GAME_SEND_MESSAGE(p, "Vous n'avez pas encore remplis votre contrat.", "A00000");
			return;
		}
		traque.reset();
		long xp = Formulas.getTraqueXP(p.get_lvl());
		p.addXp(xp);
		ObjTemplate T = World.getObjTemplate(10275);
		Objects newObj = T.createNewItem(1, false, -1);
		if (p.addObjet(newObj, true)) 
			World.addObjet(newObj, true);
		
		SocketManager.GAME_SEND_Ow_PACKET(p);
		SocketManager.GAME_SEND_STATS_PACKET(p);
		SocketManager.GAME_SEND_MESSAGE(p, new StringBuilder("Vous avez reçu ").append(xp)
				.append(" points d'expérience et 1 pévéton suite à l'accomplissement de votre traque").toString(), "00A000");
	}
}

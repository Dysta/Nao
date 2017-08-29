package org.game;

/**
 * Copyright 2012 Lucas de Chaussé - Sérapsis
 */

import java.util.Random;

public class RandomCharacterName {

	static String[] dictionary = { "ae", "au", "ao", "ap", "ka", "ha", "ah", "na", "hi", "he", "eh", "an", "ma", "wa",
			"we", "wh", "sk", "sa", "se", "ne", "ra", "re", "ru", "ri", "ro", "za", "zu", "ta", "te", "ty", "tu", "ti",
			"to", "pa", "pe", "py", "pu", "pi", "po", "da", "de", "du", "di", "do", "fa", "fe", "fu", "fi", "fo", "ga",
			"gu", "ja", "je", "ju", "ji", "jo", "la", "le", "lu", "ma", "me", "mu", "mo", "radio", "kill", "-",
			"explode", "craft", "fight", "shadow", "bouftou", "bouf", "piou", "piaf", "champ", "abra", "grobe", "krala",
			"sasa", "nianne", "miaou", "was", "killed", "born", "storm", "lier", "arm", "hand", "mind", "create",
			"random", "nick", "error", "end", "life", "die", "cut", "make", "spawn", "respawn", "zaap", "zaapis",
			"mobs", "google", "firefox", "rapta", "ewplorer", "men", "women", "dark", "eau", "get", "set", "geek",
			"nolife", "spell", "boost", "gift", "leave", "smiley", "blood", "jean", "yes", "eays", "skha", "rock",
			"stone", "fefe", "sadi", "sacri", "osa", "panda", "xel", "rox", "stuff", "spoon", "days", "mouarf", "plop",
			"after" };

	public static String get() {
		StringBuilder name = new StringBuilder();
		boolean b = true;

		do {
			int numberOfChaine = random(0, 2);

			for (int i = 0; i <= numberOfChaine; i++)
				name.append(dictionary[random(0, dictionary.length - 1)]);

			if (name.toString().length() < 5)
				continue;
			else
				b = false;
		} while (b);

		char c = name.toString().charAt(0);
		return ("" + c).toUpperCase() + name.toString().substring(1);
	}

	public static int random(int min, int max) {
		Random rand = new Random();
		return (rand.nextInt((max - min) + 1)) + min;
	}
}

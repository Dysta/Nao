package org.login.server.packet;

import org.client.Accounts;
import org.common.World;
import org.kernel.Config;
import org.login.server.Client;

/**
 * Copyright 2012 Lucas de Chaussé - Sérapsis
 */

public class FriendServerList {
	
	public static void get(Client client, String packet) {
		try {
			Accounts account = World.getCompteByPseudo(packet);
			
			if(account == null) {
				client.send("AF");
				return;
			}
			
			client.send("AF" + Config.CONFIG_SERVER_ID + ";" + account.GET_PERSO_NUMBER() + ';');
		} catch(Exception e) { }
	}
}

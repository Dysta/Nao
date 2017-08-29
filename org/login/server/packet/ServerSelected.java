package org.login.server.packet;

import org.client.Accounts;
import org.common.World;
import org.kernel.Config;
import org.kernel.Main;
import org.login.server.Client;
import org.login.server.Client.Status;

/**
 * Copyright 2012 Lucas de Chaussé - Sérapsis
 */

public class ServerSelected {
	
	public static void get(Client client, String packet) {
		int i = 0;
		
		try { 
			i = Integer.parseInt(packet);
		} catch(Exception e) {
			client.send("AXEr");
			client.kick();
			return;
		}
		
		if(i != Config.CONFIG_SERVER_ID) {
			client.send("AXEr");
			client.kick();
			return;
		}
		
		if(World.get_state() != 1) {
			client.send("AXEd");
			return;
		}
		
		Accounts account = client.getAccount();
				
		Main.gameServer.addWaitingCompte(account);
		
		String accIp = account.get_curIP(); // On optiens l'adresse IP du compte
		
		String ip = Config.CONFIG_IP_LOOPBACK && accIp.equals("127.0.0.1")?("127.0.0.1"):Config.IP;
		packet = "AYK"+ip+":"+Config.CONFIG_GAME_PORT+";"+account.get_GUID();
		client.setStatus(Status.SERVER);
		client.send(packet);
	}
}

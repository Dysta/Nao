package org.login.server.packet;

import org.client.Accounts;
import org.common.World;
import org.kernel.Config;
import org.login.server.Client;
import org.login.server.Client.Status;

public class AccountQueue {
	
	public static void verify(Client client) {
		Accounts account = client.getAccount();
		Accounts worldAccount = World.getCompteByName(account.get_name());
		byte state = 0;
		
		//Récuperation de l'ip de la connexion entrante
		String ip = client.getIoSession().getLocalAddress().toString();
		ip = ip.substring(1).split("\\:")[0];
		
		// initialisation de la state du client pour lui switcher sa maman!
		if (worldAccount.isOnline() && worldAccount.getGameThread() != null)
			state = 2;
		if (worldAccount.getGameThread() != null)
			state = 2;
		if (worldAccount.isBanned())
			state = 3;
		// if(World.isIpBanned(ip, worldAccount)) state = 3;
		if (!Config.CONFIG_ALLOW_MULTI && World.ipIsUsed(ip))
			state = 4;
		if (Config.CONFIG_ALLOW_MULTI && World.numberOfSameIp(ip) >= Config.CONFIG_MAX_PLAYER_PER_IP)
			state = 4;
		if (World.getGmAccess() > worldAccount.get_gmLvl())
			state = 4;
		/*
		if (World.getOnlinePersos().size() >= Config.CONFIG_PLAYER_LIMIT)
			state = 4;
		*/
		
		switch(state) {
		case 0 : //disconnected
			account.setState(1);
			sendInformation(client);
			break;
			
		case 1 : //in login
			account.setState(0);
			client.send("AlEa");
			client.kick();
			try { 
				World.getCompteByName(account.get_name()).getClient().kick();
			} catch(Exception e) { }
			return;
			
		case 2 : //in game
			account.setState(0);
			client.send("AlEc");
			client.kick();
			try { 
				//TODO
				World.getCompteByName(account.get_name()).getClient().kick();
			} catch(Exception e) { }
			return;
			
		case 3 : //banned
			client.send("AlEb");
			client.kick();
			return;
			
		case 4 : //To many connection
			client.send("AlEw");
			client.kick();
			return;
		}
		
		worldAccount.setClient(client);
		worldAccount.setCurIP(ip);
	}
	
	public static void sendInformation(Client client) {
		Accounts account = client.getAccount();
		
		if(account.get_pseudo().isEmpty()) {
			client.send("AlEr");
			client.setStatus(Status.WAIT_NICKNAME);
			return;
		}
		
		client.send("Af0|0|0|1|-1");
		client.send("Ad" + account.get_pseudo());
		client.send("Ac0");
		client.send("AH" + Config.CONFIG_SERVER_ID + ";" + World.get_state() + ";110;1");
		client.send("AlK" + (account.get_gmLvl()!=0?1:0));
		client.send("AQ" + account.get_question());
	}
}

package org.login.server.packet;

import org.client.Accounts;
import org.common.SQLManager;
import org.common.World;
import org.login.server.Client;
import org.login.server.Client.Status;


public class AccountName {
	
	public static void verify(Client client, String name) {
		try {
			if(World.getCompteByName(name.toLowerCase()) == null)
				SQLManager.LOAD_ACCOUNT_BY_USER(name.toLowerCase());
			
			Accounts account = World.getCompteByName(name.toLowerCase());
			
			client.setAccount(account);
		} catch(Exception e) {
			client.send("AlEf");
			client.kick();
			return;
		}
		
		if(client.getAccount() == null) {
			client.send("AlEf");
			client.kick();
			return;
		}
		
		client.setStatus(Status.WAIT_PASSWORD);
	}
}

package org.login.server.packet;

import org.client.Accounts;
import org.login.server.Client;
import org.login.server.Client.Status;

/**
 * Copyright 2012 Lucas de Chaussé - Sérapsis
 */

public class Password {
	
	public static char[] hash = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 
			'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A',
			'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
			'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4',
			'5', '6', '7', '8', '9', '-', '_'};
	
	public static void verify(Client client, String pass) {
		String accountPass = cryptPassword(client);
		
		if(!accountPass.equalsIgnoreCase(pass)) {
			client.send("AlEf");
			client.kick();
			return;
		}
		
		Accounts account = client.getAccount();
		
		if(account.getClient() != null) {
			account.getClient().kick();
			account.setClient(null);
		}
		
		if(account.getGameThread() != null) {
			account.getGameThread().kick();
			account.setGameThread(null);
		}
		
		account.setClient(client);
		
		client.setStatus(Status.LOGIN);
	}
	
	public static String cryptPassword(Client client) {
		String pass = client.getAccount().get_pass();
		String key = client.getKey();
		int i = hash.length;
		
		StringBuilder crypted = new StringBuilder("#1");
        
		for(int y = 0; y < pass.length(); y++) {
			char c1 = pass.charAt(y);
            char c2 = key.charAt(y);
            double d = Math.floor(c1 / 16);
            int j = c1 % 16;
            
            crypted.append(hash[(int) ((d + c2 % i) % i)])
            .append(hash[(j + c2 % i) % i]);
		}
        
		return crypted.toString();
	}
}

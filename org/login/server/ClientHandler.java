package org.login.server;

/**
 * Copyright 2012 Lucas de Chaussé - Sérapsis
 */

import java.util.Random;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.client.Accounts;
import org.kernel.Logs;
import org.login.server.Client.Status;

public class ClientHandler implements IoHandler {
	
	@Override
	public void exceptionCaught(IoSession arg0, Throwable arg1)
			throws Exception {
		Logs.addToRealmLog("session " + arg0.getId() + " exception : " + arg1.getMessage());
	}
	
	@Override
	public void messageReceived(IoSession arg0, Object arg1) throws Exception {
		String packet = (String) arg1;
		
		String[] s = packet.split("\n");
		int i = 0;
		do {
			Logs.addToRealmLog("Login: Recv << " + s[i] + " to session " + arg0.getId());
			Client.clients.get(arg0.getId()).parser(s[i]);
			i++;
		} while(i == s.length - 1);
	}
	
	@Override
	public void messageSent(IoSession arg0, Object arg1) throws Exception {
		Logs.addToRealmLog("Login: Send >> " + arg1.toString() + " to session " + arg0.getId());
	}
	
	@Override
	public void sessionClosed(IoSession arg0) throws Exception {
		Logs.addToRealmLog("session " + arg0.getId() + " closed");
		Client client = Client.clients.get(arg0.getId());
		Accounts account = client.getAccount();
		account.setState(0);
	}
	
	@Override
	public void sessionCreated(IoSession arg0) throws Exception {
		Logs.addToRealmLog("session " + arg0.getId() + " created");
		
		Client client = new Client(arg0, genKey());
		
		client.send("HC" + client.getKey());
		client.setStatus(Status.WAIT_VERSION);
	}
	
	@Override
	public void sessionIdle(IoSession arg0, IdleStatus arg1) throws Exception {
		Logs.addToRealmLog("session " + arg0.getId() + " idle");
	}
	
	@Override
	public void sessionOpened(IoSession arg0) throws Exception {
		Logs.addToRealmLog("session " + arg0.getId() + " oppened");

	}
	
	public static synchronized void sendToAll(String packet) {
		for(Client client : Client.clients.values()) {
			IoSession ioSession = client.getIoSession();
			
			if(ioSession.isConnected() || !ioSession.isClosing())
				client.send(packet);
		}
	}
	
	public String genKey() {
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
		StringBuilder hashKey = new StringBuilder();
		Random rand = new Random();

		for(int i = 0; i < 32; i++) 
			hashKey.append(alphabet.charAt(rand.nextInt(alphabet.length())));
		return hashKey.toString();
	}
}

package org.login.server;

//Copyright 2012 Lucas de Chaussé - Sérapsis

import java.util.*;

import org.apache.mina.core.session.IoSession;
import org.client.Accounts;
import org.kernel.Logs;
import org.login.server.packet.PacketHandler;

public class Client {
	
	private long id;
	private IoSession ioSession;
	private String key;
	private Status status;
	private Accounts account;
	
	public static Map<Long, Client> clients = new HashMap<Long, Client>();
	
	public Client(IoSession ioSession, String key) {
		setId(ioSession.getId());
		setIoSession(ioSession);
		setKey(key);
		
		clients.put(this.id, this);
	}
	
	public void send(Object object) {
		this.ioSession.write(object);
		Logs.addToHistoricLog("Login: Packet: " + object.toString());
	}
	
	void parser(String packet) {
		PacketHandler.parser(this, packet);
	}
	
	public void kick() {
		ioSession.close(true);
	}
	
	public long getId() {
		return id;
	}
	
	void setId(long l) {
		this.id = l;
	}
	
	public IoSession getIoSession() {
		return ioSession;
	}
	
	void setIoSession(IoSession ioSession) {
		this.ioSession = ioSession;
	}
	
	public String getKey() {
		return key;
	}
	
	void setKey(String key) {
		this.key = key;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public Accounts getAccount() {
		return account;
	}
	
	public void setAccount(Accounts account) {
		this.account = account;
	}
	
	public enum Status {
		WAIT_VERSION, 
		WAIT_PASSWORD, 
		WAIT_ACCOUNT, 
		WAIT_NICKNAME,
		LOGIN,
		SERVER
	}
}

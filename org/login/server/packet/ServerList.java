package org.login.server.packet;

import org.common.SQLManager;
import org.kernel.Config;
import org.login.server.Client;

/**
 * Copyright 2012 Lucas de Chaussé - Sérapsis
 */

public class ServerList {
	
	public static void get(Client client) {
		int i = client.getAccount().GET_PERSO_NUMBER();
		SQLManager.LOAD_PERSO_BY_ACCOUNT(client.getAccount().get_GUID());
		client.send("AxK" + client.getAccount().get_subscriber()*60 + "000" + (i>0?"|" + Config.CONFIG_SERVER_ID + ","+i:""));
	}
}
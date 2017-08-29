package org.login.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.kernel.Config;
import org.kernel.Logs;

public class LoginServer {
	public static String ip, version = "1.29.1";
	private static NioSocketAcceptor acceptor;

	public LoginServer() {// establish variables
		acceptor = new NioSocketAcceptor();
		acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(
				new TextLineCodecFactory(Charset.forName("UTF8"), LineDelimiter.NUL, new LineDelimiter("\n\0"))));
		acceptor.setHandler(new ClientHandler());
	}

	public void start() {// start server
		if (acceptor.isActive())
			return;// if acceptor's already launched

		try {
			acceptor.bind(new InetSocketAddress(Config.CONFIG_REALM_PORT));// enable connection
		} catch (IOException e) {
			Logs.addToRealmLog("Fail to bind acceptor : " + e.toString());
			Logs.addToRealmLog("Fail to bind acceptor : " + e);
		} finally {
			Logs.addToRealmLog("Login server started on port " + Config.CONFIG_REALM_PORT);
		}
	}

	public void stop() throws Exception {// stop server
		if (!acceptor.isActive())
			return;// if acceptor's not launched

		acceptor.unbind();// disable connection

		for (IoSession session : acceptor.getManagedSessions().values()) // kick all clients
			if (session.isConnected() || !session.isClosing())
				session.close(true);

		acceptor.dispose();// closing

		Logs.addToRealmLog("Login server stoped");
	}
}

package io.ess.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.quickfixj.jmx.JmxExporter;
import org.springframework.stereotype.Service;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.FileStoreFactory;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Initiator;
import quickfix.LogFactory;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.RejectLogon;
import quickfix.ScreenLogFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.UnsupportedMessageType;

@Service
public class ClientApplicationService implements Application {
	
	private static Logger logger = Logger.getLogger(ClientApplicationService.class);
	
	private static final CountDownLatch shutdownLatch = new CountDownLatch(1);

	private static final Logger log = Logger.getLogger(ClientApplicationService.class);
	private static ClientApplicationService banzai;
	private boolean initiatorStarted = false;
	private Initiator initiator = null;

	public static void main(String[] args) throws Exception {
		banzai = new ClientApplicationService();
		banzai.init();
		if (!System.getProperties().containsKey("openfix")) {
			banzai.logon();
		}
		shutdownLatch.await();
	}
	
	public void init() throws Exception {
		InputStream inputStream = ClientApplicationService.class.getResourceAsStream("/QuickFix4J.conf");
		SessionSettings settings = new SessionSettings(inputStream);
		inputStream.close();
		boolean logHeartbeats = Boolean.valueOf(System.getProperty("logHeartbeats", "true"));
		ClientApplicationService application = new ClientApplicationService();
		MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
		LogFactory logFactory = new ScreenLogFactory(true, true, true, logHeartbeats);
		MessageFactory messageFactory = new DefaultMessageFactory();
		initiator = new SocketInitiator(application, messageStoreFactory, settings, logFactory, messageFactory);
		JmxExporter exporter = new JmxExporter();
		exporter.register(initiator);
	}
	
	public synchronized void logon() {
		if (!initiatorStarted) {
			try {
				initiator.start();
				initiatorStarted = true;
			} catch (Exception e) {
				log.error("Logon failed", e);
			}
		} else {
			for (SessionID sessionId : initiator.getSessions()) {
				Session.lookupSession(sessionId).logon();
			}
		}
	}

	public void logout() {
		for (SessionID sessionId : initiator.getSessions()) {
			Session.lookupSession(sessionId).logout("user requested");
		}
	}

	public void stop() {
		shutdownLatch.countDown();
	}

	/**
	 * This callback notifies you when an administrative message is sent from a counterparty to 
	 * your FIX engine. This can be usefull for doing extra validation on logon messages such as 
	 * for checking passwords. Throwing a RejectLogon exception will disconnect the counterparty.
	 */
	@Override
	public void fromAdmin(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		logger.info(String.format("Received message %s for session %s", message, sessionId));
	}

	/**
	 * This is one of the core entry points for your FIX application. Every application level 
	 * request will come through here. If, for example, your application is a sell-side OMS, this 
	 * is where you will get your new order requests. If you were a buy side, you would get your 
	 * execution reports here. If a FieldNotFound exception is thrown, the counterparty will 
	 * receive a reject indicating a conditionally required field is missing. The Message class 
	 * will throw this exception when trying to retrieve a missing field, so you will rarely need 
	 * the throw this explicitly. You can also throw an UnsupportedMessageType exception. This will 
	 * result in the counterparty getting a reject informing them your application cannot process 
	 * those types of messages. An IncorrectTagValue can also be thrown if a field contains a value 
	 * that is out of range or you do not support.
	 */
	@Override
	public void fromApp(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
		logger.info(String.format("Received message %s from app with session id %s", message, sessionId));
	}

	/**
	 * This method is called when quickfix creates a new session. A session comes into and 
	 * remains in existence for the life of the application. Sessions exist whether or not 
	 * a counter party is connected to it. As soon as a session is created, you can begin 
	 * sending messages to it. If no one is logged on, the messages will be sent at the time 
	 * a connection is established with the counterparty.
	 * 
	 */
	@Override
	public void onCreate(SessionID session) {
		logger.info(String.format("Created session %s", session));
	}

	/**
	 * This callback notifies you when a valid logon has been established with a counter party. 
	 * This is called when a connection has been established and the FIX logon process has completed 
	 * with both parties exchanging valid logon messages.
	 */
	@Override
	public void onLogon(SessionID session) {
		logger.info(String.format("Logged on %s", session));
	}

	/**
	 * This callback notifies you when an FIX session is no longer online. This could happen during a 
	 * normal logout exchange or because of a forced termination or a loss of network connection.
	 */
	@Override
	public void onLogout(SessionID session) {
		logger.info(String.format("Logged out %s", session));
	}

	/**
	 * This callback provides you with a peak at the administrative messages that are being sent from 
	 * your FIX engine to the counter party. This is normally not useful for an application however it 
	 * is provided for any logging you may wish to do. Notice that the FIX::Message is not const. This 
	 * allows you to add fields before an adminstrative message before it is sent out.
	 */
	@Override
	public void toAdmin(Message message, SessionID session) {
		logger.info(String.format("Message %s send to admin using session id %s", message, session));
	}
	
	/**
	 * This is a callback for application messages that you are being sent to a counterparty. If you throw 
	 * a DoNotSend exception in this function, the application will not send the message. This is mostly 
	 * useful if the application has been asked to resend a message such as an order that is no longer 
	 * relevant for the current market. Messages that are being resent are marked with the PossDupFlag 
	 * in the header set to true; If a DoNotSend exception is thrown and the flag is set to true, a sequence 
	 * reset will be sent in place of the message. If it is set to false, the message will simply not be sent. 
	 * Notice that the FIX::Message is not const. This allows you to add fields before an application message 
	 * before it is sent out.
	 */
	@Override
	public void toApp(Message message, SessionID sessionId) throws DoNotSend {
		logger.info(String.format("Message %s sent to app from session id %s", message, sessionId));
	}

}

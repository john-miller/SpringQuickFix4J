package io.ess.services;

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import quickfix.Acceptor;
import quickfix.Application;
import quickfix.DefaultMessageFactory;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.LogFactory;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;
import quickfix.UnsupportedMessageType;

@Service
public class ClientApplicationService implements Application {
	
	private static Logger logger = Logger.getLogger(ClientApplicationService.class);

	  public static void main(String args[]) throws Exception {
		    // FooApplication is your class that implements the Application interface
		    Application application = new ClientApplicationService();
		    InputStream settingInputStream = ClientApplicationService.class.getResourceAsStream("/QuickFix4J.conf");
		    logger.info(settingInputStream);
		    SessionSettings settings = new SessionSettings(settingInputStream);
		    MessageStoreFactory storeFactory = new FileStoreFactory(settings);
		    LogFactory logFactory = new FileLogFactory(settings);
		    MessageFactory messageFactory = new DefaultMessageFactory();
		    Acceptor acceptor = new SocketAcceptor
		      (application, storeFactory, settings, logFactory, messageFactory);
		    acceptor.start();
		    // while(condition == true) { do something; }
		    acceptor.stop();
		  }
	
	/**
	 * This callback notifies you when an administrative message is sent from a counterparty to 
	 * your FIX engine. This can be usefull for doing extra validation on logon messages such as 
	 * for checking passwords. Throwing a RejectLogon exception will disconnect the counterparty.
	 */
	@Override
	public void fromAdmin(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		logger.info(String.format("Received message 1% for session 2%", message, sessionId));
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
		logger.info(String.format("Received message 1% from app with session id 2%", message, sessionId));
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
		logger.info(String.format("Created session 1%", session));
	}

	/**
	 * This callback notifies you when a valid logon has been established with a counter party. 
	 * This is called when a connection has been established and the FIX logon process has completed 
	 * with both parties exchanging valid logon messages.
	 */
	@Override
	public void onLogon(SessionID session) {
		logger.info(String.format("Logged on 1%", session));
	}

	/**
	 * This callback notifies you when an FIX session is no longer online. This could happen during a 
	 * normal logout exchange or because of a forced termination or a loss of network connection.
	 */
	@Override
	public void onLogout(SessionID session) {
		logger.info(String.format("Logged out 1%", session));
	}

	/**
	 * This callback provides you with a peak at the administrative messages that are being sent from 
	 * your FIX engine to the counter party. This is normally not useful for an application however it 
	 * is provided for any logging you may wish to do. Notice that the FIX::Message is not const. This 
	 * allows you to add fields before an adminstrative message before it is sent out.
	 */
	@Override
	public void toAdmin(Message message, SessionID session) {
		logger.info(String.format("Message 1% send to admin using session id 2%", message, session));
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
		logger.info(String.format("Message 1% sent to app from session id 2%", message, sessionId));
	}

}

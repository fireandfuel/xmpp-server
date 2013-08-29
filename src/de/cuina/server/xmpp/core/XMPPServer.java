package de.cuina.server.xmpp.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import de.cuina.server.xmpp.data.Session;
import de.cuina.server.xmpp.database.IDatabaseProvider;

public class XMPPServer
{
	private ServerSocket serverSocket;
	private ExecutorService connectionPool;
	private ArrayList<Connection> connectionList;
	private HashMap<String, Session> openSessions = new HashMap<String, Session>();
	
	private final String keyStoreFile, keyStorePassword;

	private boolean closeRequest = false;
	private static IDatabaseProvider database;

	public static final String STREAM_FEATURES_INIT = "<stream:features>"
			+ "<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>"
			+ "<mechanism>DIGEST-MD5</mechanism>"
			+ "</mechanisms>"
			+ "</stream:features>";
	
	public static final String STREAM_FEATURES_SSL_INIT = "<stream:features>"
			+ "<starttls xmlns='urn:ietf:params:xml:ns:xmpp-tls'>"
			+ "<required/>" + "</starttls>"
			+ "<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>"
			+ "<mechanism>DIGEST-MD5</mechanism>"
			+ "</mechanisms>"
			+ "</stream:features>";

	public static final String STREAM_FEATURES_BIND = "<stream:features>"
			+ "<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'/>"
			+ "<session xmlns='urn:ietf:params:xml:ns:xmpp-session'/>"
			+ "</stream:features>";

	public static final int UTF8 = 0;
	public static final int ISO88591 = 1;

	public XMPPServer(int port, String keyStoreFile, String keyStorePassword, IDatabaseProvider db) throws XMLStreamException
	{
		this.keyStoreFile = keyStoreFile;
		this.keyStorePassword = keyStorePassword;
		
		database = db;
		
		try
		{
			serverSocket = new ServerSocket(port);
			connectionPool = Executors.newCachedThreadPool();
			connectionList = new ArrayList<Connection>();

			while(!closeRequest)
			{
				if(serverSocket != null)
				{
					Socket socket = serverSocket.accept();
					if(socket != null)
					{
						System.out.println("(S) Create new socket");
						Connection connection = new Connection(socket);
						connectionList.add(connection);
						connectionPool.execute(connection);
					} else break;
					
				}
				
			}
		} catch (IOException e)
		{
			System.exit(0);
		}

		

	}

	public class Connection implements Runnable
	{
		private DataInputStream in;
		private DataOutputStream out;
		private Socket socket;
		private SSLSocket sslSocket;

		private boolean socketClosed = false;

		private XMLInputFactory factory;
		private XMLStreamReader reader;

		private XMPPParser parser = null;
		
		private String userName = null;

		public Connection(Socket socket) throws IOException, XMLStreamException
		{
			this.socket = socket;
			this.in = new DataInputStream(socket.getInputStream());
			this.out = new DataOutputStream(socket.getOutputStream());

			factory = XMLInputFactory.newFactory();
			reader = factory.createXMLStreamReader(this.in);
		}

		public synchronized void setSecureSocket() throws IOException,
				XMLStreamException, NoSuchAlgorithmException,
				KeyManagementException, CertificateException,
				KeyStoreException, UnrecoverableKeyException
		{
			SSLContext sslContext = SSLContext.getInstance("TLS");

			char[] passphrase = keyStorePassword.toCharArray();
			KeyStore keystore = KeyStore.getInstance("JKS");
			keystore.load(new FileInputStream(keyStoreFile), passphrase);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(keystore, passphrase);
			KeyManager[] keyManagers = kmf.getKeyManagers();

			sslContext.init(keyManagers, null, null);
			SSLSocketFactory sslFactory = sslContext.getSocketFactory();
			sslSocket = (SSLSocket) sslFactory.createSocket(socket, null,
					socket.getLocalPort(), false);
			sslSocket.setUseClientMode(false);
			
			this.in = new DataInputStream(sslSocket.getInputStream());
			this.out = new DataOutputStream(sslSocket.getOutputStream());
			
			reader = factory.createXMLStreamReader(this.in);
			parser.setReader(reader);

		}

		@Override
		public void run()
		{
			if(!socket.isClosed())
			{
				parser = new XMPPParser(reader, this);
				parser.start();
			}
			while(!socket.isClosed())
			{
				try
				{
					Thread.sleep(30000);
				} catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			}

			try
			{
				socketClosed = true;
				in.close();
				out.close();
			} catch(IOException e)
			{
				e.printStackTrace();
			}

		}

		public synchronized void sendString(String xml, int encoding)
				throws IOException
		{
			String encodingStr = null;
			switch(encoding) {
			case UTF8:
				encodingStr = "UTF-8";
				break;
			case ISO88591:
				encodingStr = "ISO-8859-1";
				break;
			}

			if(!socket.isClosed())
			{
				System.out.println("send (" + ((userName == null) ? this.hashCode() : userName) + "): " + xml);
				out.write(xml.getBytes(encodingStr));
				out.flush();
			}

		}

		public synchronized void setUserName(String userName)
		{
			this.userName = userName;
		}
		
		public synchronized void close() throws IOException, XMLStreamException
		{
			this.sendString("</stream>", 1);
			try
			{
				in.close();
				out.close();
				socket.close();
			} catch(IOException e)
			{
				e.printStackTrace();
			}
			reader.close();
			socketClosed = true;
			System.out.println("(S) Socket closed");
		}

		public synchronized HashMap<String, Session> getOpenSessions()
		{
			return openSessions;
		}

		public synchronized boolean isClosed()
		{
			return socketClosed;
		}

		public String getKeyStoreFileName()
		{
			return keyStoreFile;
		}
		
		public boolean isSSL()
		{
			return(sslSocket != null);
		}
		
		public void stopServer()
		{
			try
			{
				serverSocket.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			closeRequest = true;
		}

	}
	
	public static IDatabaseProvider getDatabase()
	{
		return database;
	}
}

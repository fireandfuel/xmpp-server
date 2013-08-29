package de.cuina.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Properties;

import javax.xml.stream.XMLStreamException;

import de.cuina.server.plugin.OSGiProvider;
import de.cuina.server.xmpp.core.XMPPServer;
import de.cuina.server.xmpp.database.IDatabaseProvider;
import de.cuina.server.xmpp.database.SQLDatabase;
import de.cuina.server.xmpp.database.TemporaryDatabase;

public class Server
{
	private static String port, keyStoreFile, keyStorePassword, databaseType;
	private static SecureRandom random = new SecureRandom();
	
	public static void main(String[] args)
	{
		String configName = "server.ini";
		if(args.length == 1)
		{
			configName = args[0];
		}
		
		IDatabaseProvider provider = null;
		
		Properties properties = new Properties();
		try
		{
			File config = new File(configName);
			if(config.exists())
			{
				properties.load(new InputStreamReader( new FileInputStream("server.ini"), "UTF-8"));
				if(properties.getProperty("port") != null)
					port = properties.getProperty("port");
				if(properties.getProperty("keystore_file") != null)
					keyStoreFile = properties.getProperty("keystore_file");
				if(properties.getProperty("keystore_password") != null)
					keyStorePassword = properties.getProperty("keystore_password");
				if(properties.getProperty("database_type") != null)
					databaseType = properties.getProperty("database_type");
				
				if(databaseType != null)
					if(databaseType.equalsIgnoreCase("mysql") || databaseType.equalsIgnoreCase("mariadb") || databaseType.equalsIgnoreCase("postgresql"))
					{
						String host = properties.getProperty("sql_host");
						String port = properties.getProperty("sql_port");
						String user = properties.getProperty("sql_user");
						String password = properties.getProperty("sql_password");
						String database = properties.getProperty("sql_database");
						
						String encryptionPassword = properties.getProperty("encryption_key");
						
						provider = new SQLDatabase(databaseType, host, port, database, user, password, encryptionPassword);
					} else 
						
					if(databaseType.equalsIgnoreCase("sqlite"))
					{
						String sqliteDatabase = properties.getProperty("sqlite_database_file");
						
						String encryptionPassword = properties.getProperty("encryption_key");
						
						provider = new SQLDatabase(databaseType, sqliteDatabase, encryptionPassword);
					} else
						
					if(databaseType.equalsIgnoreCase("temporary"))
						provider = new TemporaryDatabase();
					else
					{
						System.err.println("ERROR: Unknown database type: " + databaseType);
						System.err.println("ERROR: You must specify a database type at 'database_type=' in your ini-file. Stop!");
						System.exit(0);
					}
						
				else 
				{
					System.err.println("ERROR: You must specify a database type at 'database_type=' in your ini-file. Stop!");
					System.exit(0);
				}
			} else
			{
				System.out.println("WARNING: No config found, start with default settings");
				port = "5222";
				keyStoreFile = "server.ks";
				keyStorePassword = "";
				databaseType = "sqlite";
				provider = new SQLDatabase("sqlite", "server.db");
			}
			
			

			try
			{
				new OSGiProvider();
				new XMPPServer(Integer.parseInt(port), keyStoreFile, keyStorePassword, provider);
			} catch (NumberFormatException e)
			{
				e.printStackTrace();
			} catch (XMLStreamException e)
			{
				e.printStackTrace();
			}
			
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public final static String getRandomString(int length)
	{
		return new BigInteger(130, random).toString(length);
	}
}

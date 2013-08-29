package de.cuina.server.xmpp.database;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.cuina.server.Base64;
import de.cuina.server.Server;
import de.cuina.server.xmpp.data.JID;
import de.cuina.server.xmpp.data.Property;

public class SQLDatabase implements IDatabaseProvider
{
	private Connection conn;

	private Cipher encryptCipher, decryptCipher;

	/**
	 * @param type
	 *            type of the sql database (mysql, mariadb, postgresql or
	 *            sqlite)
	 * @param args
	 *            for sqlite: file, for mysql and postgresql: host, port,
	 *            database, user, password
	 *            PS: mariadb can handle mysql databases, too
	 */
	public SQLDatabase(String type, String... args)
	{

		// encrypt / decrypt init
		try
		{
			byte[] key = (args[args.length - 1] != null) ? ((!args[args.length - 1]
					.isEmpty()) ? args[args.length - 1].getBytes("UTF-8")
					: null) : null;

			Security.addProvider(new BouncyCastleProvider());

			MessageDigest md = MessageDigest.getInstance("RipeMD128");
			md.update(key);

			byte[] hash = md.digest();

			String initVectorString = null;

			File initVectorFile = new File("server.dat");
			if(initVectorFile.exists())
			{
				FileInputStream fis = new FileInputStream(initVectorFile);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(fis));

				initVectorString = reader.readLine();
				reader.close();
			} else
			{
				BufferedWriter writer = new BufferedWriter(new FileWriter(
						initVectorFile));
				initVectorString = Server.getRandomString(32);

				writer.write(initVectorString);
				writer.close();
			}

			md.update(initVectorString.getBytes());
			byte[] initializationVector = md.digest();

			SecretKey keyValue = new SecretKeySpec(hash, "AES");

			AlgorithmParameterSpec iVSpec = new IvParameterSpec(
					initializationVector);

			encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			encryptCipher.init(Cipher.ENCRYPT_MODE, keyValue, iVSpec);

			decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			decryptCipher.init(Cipher.DECRYPT_MODE, keyValue, iVSpec);

		} catch(NoSuchAlgorithmException | NoSuchProviderException
				| NoSuchPaddingException | InvalidKeyException
				| InvalidAlgorithmParameterException e)
		{
			e.printStackTrace();
		} catch(FileNotFoundException e)
		{
			e.printStackTrace();
		} catch(IOException e)
		{
			e.printStackTrace();
		}

		if(type.equalsIgnoreCase("sqlite") && args.length == 2)
		{
			String file = args[0];

			try
			{
				Class.forName("org.sqlite.JDBC");

				if(!new File(file).exists())
				{
					System.out.println("WARING: SQLite database file at "
							+ file + " not found. Created a new one.");

					conn = DriverManager.getConnection("jdbc:sqlite:" + file);

					if(conn != null)
						createInitialDatabase();

				} else
				{
					conn = DriverManager.getConnection("jdbc:sqlite:" + file);
				}

			} catch(ClassNotFoundException e)
			{
				System.err.println("ERROR: SQLite Driver not found");
				e.printStackTrace();
				System.exit(2);
			} catch(SQLException e)
			{
				System.err.println("ERROR: SQLite Connection failed");
				e.printStackTrace();
				System.exit(2);
			}
		} else

		if(type.equalsIgnoreCase("mysql") && args.length == 6)
		{
			String host = args[0];
			String port = args[1];
			String database = args[2];
			String user = args[3];
			String password = args[4];

			try
			{
				Class.forName("com.mysql.jdbc.Driver");

				conn = DriverManager.getConnection("jdbc:mysql://" + host + ":"
						+ port + "/" + database + "?" + "user=" + user + "&"
						+ "password=" + password);

				if(conn != null && getRegisteredUsers() == null)
					createInitialDatabase();
			} catch(ClassNotFoundException e)
			{
				System.err.println("ERROR: MySQL Driver not found");
				e.printStackTrace();
				System.exit(2);
			} catch(SQLException e)
			{
				System.err.println("ERROR: MySQL Connection failed");
				e.printStackTrace();
				System.exit(2);
			}
		} else

		if(type.equalsIgnoreCase("mariadb") && args.length == 6)
		{
			String host = args[0];
			String port = args[1];
			String database = args[2];
			String user = args[3];
			String password = args[4];

			try
			{
				Class.forName("org.mariadb.jdbc.Driver");

				conn = DriverManager.getConnection("jdbc:mysql://" + host + ":"
						+ port + "/" + database + "?" + "user=" + user + "&"
						+ "password=" + password);

				if(conn != null && getRegisteredUsers() == null)
					createInitialDatabase();
			} catch(ClassNotFoundException e)
			{
				System.err.println("ERROR: MariaDB Driver not found");
				e.printStackTrace();
				System.exit(2);
			} catch(SQLException e)
			{
				System.err.println("ERROR: MariaDB Connection failed");
				e.printStackTrace();
				System.exit(2);
			}
		} else

		if(type.equalsIgnoreCase("postgresql") && args.length == 6)
		{
			String host = args[0];
			String port = args[1];
			String database = args[2];
			String user = args[3];
			String password = args[4];

			try
			{
				Class.forName("org.postgresql.Driver");

				conn = DriverManager.getConnection("jdbc:postgresql://" + host
						+ ":" + port + "/" + database, user, password);

				if(conn != null && getRegisteredUsers() == null)
					createInitialDatabase();
			} catch(ClassNotFoundException e)
			{
				System.err.println("ERROR: PostgreSQL Driver not found");
				e.printStackTrace();
				System.exit(2);
			} catch(SQLException e)
			{
				System.err.println("ERROR: PostgreSQL Connection failed");
				e.printStackTrace();
				System.exit(2);
			}
		}

		System.out.println("Check database for users ...");
		String[] users = getRegisteredUsers();

		if(users == null)
		{
			System.err
					.println("ERROR: Database check failed, getRegisteredUsers() returns null. STOP");
			System.exit(1);
		} else if(users.length == 0)
		{
			System.err
					.println("SECURITY ERROR: Database contains no user.\n"
							+ "Add at least on user before running the server again. STOP");
			System.exit(1);
		} else
			System.out.println("[check done]");
	}

	@Override
	public Property getUser(String name)
	{
		if(conn != null)
		{
			Statement query;

			try
			{
				query = conn.createStatement();
				String sql = "SELECT * FROM `users` WHERE `name`='"
						+ encrypt(name) + "'";
				ResultSet result = query.executeQuery(sql);

				result.next();

				if(!result.isClosed())
				{
					Property property = new Property(
							decrypt(result.getString("password")));
					property.setGroup(decrypt(result.getString("group")));

					query = conn.createStatement();
					sql = "SELECT * FROM `contacts` WHERE `user`='"
							+ encrypt(name) + "'";
					result = query.executeQuery(sql);

					while(result.next())
					{
						JID contact = new JID(decrypt(result.getString("jid")));
						contact.setSubscriptionLevel(decrypt(result
								.getString("subscriptionlevel")));
						property.addJID(contact);
					}

					return property;
				}

			} catch(SQLException e)
			{
				e.printStackTrace();
			}
		}

		return null;
	}

	@Override
	public void addContact(JID jID, String userName)
	{
		if(conn != null)
		{
			Statement query;

			try
			{
				query = conn.createStatement();
				String sql = "INSERT INTO `contacts` (`jid` ,`user` ,`subscriptionlevel`) "
						+ "VALUES ('"
						+ encrypt(jID.getJIDString())
						+ "', '"
						+ encrypt(userName)
						+ "', '"
						+ encrypt(jID.getSubscriptionLevel()) + "')";

				query.executeUpdate(sql);
			} catch(SQLException e)
			{
				e.printStackTrace();
			}
		}

	}

	@Override
	public void removeContact(JID jID, String userName)
	{
		if(conn != null)
		{
			Statement query;

			try
			{
				query = conn.createStatement();
				String sql = "DELETE FROM `contacts` WHERE `contacts`.`jid` = '"
						+ encrypt(jID.getJIDString())
						+ "' AND `contacts`.`user` = '"
						+ encrypt(userName)
						+ "'";

				query.executeUpdate(sql);
			} catch(SQLException e)
			{
				e.printStackTrace();
			}
		}

	}

	@Override
	public void setSubscriptionLevel(JID jID, String userName, String level)
	{
		if(conn != null)
		{
			Statement query;

			try
			{
				query = conn.createStatement();
				String sql = "UPDATE `contacts` SET `subscriptionlevel` = '"
						+ encrypt(level) + "' WHERE `contacts`.`jid` = '"
						+ encrypt(jID.getJIDString())
						+ "' AND `contacts`.`user` = '" + encrypt(userName)
						+ "'";

				query.executeUpdate(sql);
			} catch(SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public String getGroup(String userName)
	{
		if(conn != null)
		{
			Statement query;

			try
			{
				query = conn.createStatement();
				String sql = "SELECT `group` FROM `users` WHERE `name`='"
						+ encrypt(userName) + "'";
				ResultSet result = query.executeQuery(sql);

				result.next();

				return decrypt(result.getString("group"));
			} catch(SQLException e)
			{
				e.printStackTrace();
			}
		}
		return userName;
	}

	@Override
	public void setGroup(String userName, String group)
	{
		if(conn != null)
		{
			Statement query;

			try
			{
				query = conn.createStatement();
				String sql = "UPDATE `users` SET `group` = '" + encrypt(group)
						+ "' WHERE `users`.`name` = '" + encrypt(userName)
						+ "'";

				query.executeUpdate(sql);
			} catch(SQLException e)
			{
				e.printStackTrace();
			}
		}

	}

	@Override
	public String[] getRegisteredUsers()
	{
		if(conn != null)
		{
			Statement query;

			try
			{
				ArrayList<String> users = new ArrayList<String>();

				query = conn.createStatement();
				String sql = "SELECT `name` FROM `users` ORDER BY `users`.`name` ASC";
				ResultSet result = query.executeQuery(sql);

				while(result.next())
				{
					users.add(decrypt(result.getString("name")));
				}

				String[] array = new String[users.size()];

				users.toArray(array);

				return array;
			} catch(SQLException e)
			{
				e.printStackTrace();
			}
		}
		return null;
	}

	private void createInitialDatabase()
	{
		if(conn != null)
		{
			Statement stat;
			try
			{
				stat = conn.createStatement();
				stat.executeUpdate("CREATE TABLE IF NOT EXISTS `users` "
						+ "(`name` varchar(255) NOT NULL, "
						+ "`password` text NOT NULL, "
						+ "`group` text NOT NULL, " + "`vcard` text, "
						+ "PRIMARY KEY (`name`))");

				stat.executeUpdate("CREATE TABLE IF NOT EXISTS `contacts`"
						+ " (`jid` varchar(255) NOT NULL, "
						+ "`user` varchar(255) NOT NULL, "
						+ "`subscriptionlevel` varchar(4) NOT NULL, "
						+ "PRIMARY KEY (`jid`,`user`))");

				Random r = new Random();
				MessageDigest md = null;

				try
				{
					md = MessageDigest.getInstance("SHA-256");
				} catch(NoSuchAlgorithmException e)
				{
					e.printStackTrace();
				}

				byte[] entropy = new byte[1024];
				r.nextBytes(entropy);
				md.update(entropy, 0, 1024);
				String password = new BigInteger(1, md.digest()).toString(16)
						.substring(0, 12);

				System.out.println("Root password is: " + password);
				System.out
						.println("SECURITY WARNING: Please change the root password later");

				stat.executeUpdate("INSERT INTO `users` (`name` ,`password` ,`group`, `vcard`) "
						+ "VALUES ('"
						+ encrypt("root")
						+ "', '"
						+ encrypt(password)
						+ "', '"
						+ encrypt("root")
						+ "', null)");

			} catch(SQLException e)
			{
				System.err.println("ERROR: SQL createInitialDatabase failed");
				e.printStackTrace();
				System.exit(2);
			}
		}

	}

	@Override
	public void addUser(String userName, String password, String group,
			String vcard)
	{
		if(conn != null)
		{
			Statement stat;
			try
			{
				stat = conn.createStatement();

				stat.executeUpdate("INSERT INTO `users` (`name` ,`password` ,`group`, `vcard`) "
						+ "VALUES ('"
						+ encrypt(userName)
						+ "', '"
						+ encrypt(password)
						+ "', '"
						+ encrypt(group)
						+ "', null)");

			} catch(SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setPassword(String userName, String password)
	{
		if(conn != null)
		{
			Statement query;

			try
			{
				query = conn.createStatement();
				String sql = "UPDATE `users` SET `password` = '"
						+ encrypt(password) + "' WHERE `users`.`name` = '"
						+ encrypt(userName) + "'";

				query.executeUpdate(sql);
			} catch(SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	private final String encrypt(String text)
	{
		if(text == null)
			return null;

		if(encryptCipher != null)
		{
			byte[] input;
			try
			{
				input = text.getBytes("UTF-8");

				ByteArrayInputStream reader = new ByteArrayInputStream(input);
				ByteArrayOutputStream writer = new ByteArrayOutputStream();

				byte[] buffer = new byte[16];
				int noBytes = 0;

				byte[] cipherBlock = new byte[encryptCipher
						.getOutputSize(buffer.length)];

				int cipherBytes;
				while((noBytes = reader.read(buffer)) != -1)
				{
					cipherBytes = encryptCipher.update(buffer, 0, noBytes,
							cipherBlock);
					writer.write(cipherBlock, 0, cipherBytes);
				}

				cipherBytes = encryptCipher.doFinal(cipherBlock, 0);
				writer.write(cipherBlock, 0, cipherBytes);

				byte[] output = writer.toByteArray();

				return Base64.encodeBytes(output);
			} catch(ShortBufferException | IOException
					| IllegalBlockSizeException | BadPaddingException e)
			{
				e.printStackTrace();
				return text;
			}
		}
		return text;
	}

	private final String decrypt(String text)
	{
		if(text == null)
			return null;

		if(decryptCipher != null)
		{
			byte[] input;
			try
			{
				input = Base64.decode(text);

				ByteArrayInputStream reader = new ByteArrayInputStream(input);
				ByteArrayOutputStream writer = new ByteArrayOutputStream();

				byte[] buffer = new byte[16];
				int noBytes = 0;

				byte[] cipherBlock = new byte[decryptCipher
						.getOutputSize(buffer.length)];

				int cipherBytes;
				while((noBytes = reader.read(buffer)) != -1)
				{
					cipherBytes = decryptCipher.update(buffer, 0, noBytes,
							cipherBlock);
					writer.write(cipherBlock, 0, cipherBytes);
				}

				cipherBytes = decryptCipher.doFinal(cipherBlock, 0);
				writer.write(cipherBlock, 0, cipherBytes);

				byte[] output = writer.toByteArray();
				return new String(output, "UTF-8");

			} catch(IOException | ShortBufferException
					| IllegalBlockSizeException | BadPaddingException e)
			{
				e.printStackTrace();
				return text;
			}
		}
		return text;
	}

}

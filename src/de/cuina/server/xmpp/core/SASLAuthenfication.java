package de.cuina.server.xmpp.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import javax.xml.stream.XMLStreamException;

import de.cuina.server.Base64;
import de.cuina.server.Server;
import de.cuina.server.xmpp.core.XMPPServer.Connection;
import de.cuina.server.xmpp.data.Stream;
import de.cuina.server.xmpp.database.IDatabaseProvider;

public class SASLAuthenfication
{
	private String nonce, mechanism, clientHash, serverHash;
	private int nc;
	private Connection connection;
	private int state = 0;
	private Stream stream;
	private HashMap<String, Object> clientResponse;

	private String userName = null;

	private IDatabaseProvider db = XMPPServer.getDatabase();

	private final static String[] REQUIRED_MD5_RESPONSE_KEYWORDS = new String[] { "username",
			"realm", "nonce", "nc", "qop", "digest-uri", "response", "charset" };

	public SASLAuthenfication(Connection connection, Stream stream, String mechanism)
	{
		System.out.println("(C) Authentication request received");
		state = 1;

		this.mechanism = mechanism;

		if(this.mechanism.equals("DIGEST-MD5"))
		{
			this.connection = connection;
			this.stream = stream;

			try
			{
				this.nonce = Server.getRandomString(16);
				String challenge = "realm=\"" + stream.getServer() + "\",nonce=\"" + nonce
						+ "\",qop=\"auth\",charset=utf-8,algorithm=md5-sess";
				connection.sendString("<challenge xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>"
						+ Base64.encodeBytes(challenge.getBytes("ISO-8859-1")) + "</challenge>", 1);
				System.out.println("(S) 1st authentication challenge sended");
				state = 2;
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}

	}

	private boolean checkAllRequiredResposeKeywords()
	{
		for(String key : REQUIRED_MD5_RESPONSE_KEYWORDS)
		{
			if(!clientResponse.containsKey(key))
				return false;
		}
		return true;
	}

	private byte[] binaryToHex(byte[] digest) throws UnsupportedEncodingException
	{

		StringBuffer digestString = new StringBuffer();
		for(int i = 0; i < digest.length; i++)
		{
			if((digest[i] & 0x000000ff) < 0x10)
			{
				digestString.append("0" + Integer.toHexString(digest[i] & 0x000000ff));
			} else
			{
				digestString.append(Integer.toHexString(digest[i] & 0x000000ff));
			}
		}
		return digestString.toString().getBytes("ISO-8859-1");
	}

	private String calculateMD5Response(boolean serverClient)
	{
		byte[] hexA1, hexA2;
		ByteArrayOutputStream a2, y, a1, kd;
		try
		{
			MessageDigest md5 = MessageDigest.getInstance("MD5");

			y = new ByteArrayOutputStream();
			y.write(((String) clientResponse.get("username") + ":"
					+ (String) clientResponse.get("realm") + ":" + db.getUser(
					(String) clientResponse.get("username")).getPassword()).getBytes("ISO-8859-1"));

			md5.update(y.toByteArray());
			byte[] digest = md5.digest();

			a1 = new ByteArrayOutputStream();
			a1.write(digest);
			a1.write((":" + clientResponse.get("nonce") + ":" + clientResponse.get("cnonce"))
					.getBytes("ISO-8859-1"));

			md5.update(a1.toByteArray());
			digest = md5.digest();

			hexA1 = binaryToHex(digest);

			a2 = new ByteArrayOutputStream();
			if(!serverClient)
				a2.write((":" + (String) clientResponse.get("digest-uri")).getBytes("ISO-8859-1"));
			else a2.write(("AUTHENTICATE:" + (String) clientResponse.get("digest-uri"))
					.getBytes("ISO-8859-1"));
			md5.update(a2.toByteArray());
			digest = md5.digest();

			hexA2 = binaryToHex(digest);

			y = new ByteArrayOutputStream();
			y.write(((String) clientResponse.get("username") + ":"
					+ (String) clientResponse.get("realm") + ":" + db.getUser(userName)
					.getPassword()).getBytes("ISO-8859-1"));

			md5.update(y.toByteArray());
			digest = md5.digest();

			a1 = new ByteArrayOutputStream();
			a1.write(digest);
			a1.write((":" + clientResponse.get("nonce") + ":" + clientResponse.get("cnonce"))
					.getBytes("ISO-8859-1"));

			md5.update(a1.toByteArray());
			digest = md5.digest();

			hexA1 = binaryToHex(digest);

			kd = new ByteArrayOutputStream();
			kd.write(hexA1);
			kd.write((":" + clientResponse.get("nonce") + ":" + String.format("%08d", nc) + ":"
					+ clientResponse.get("cnonce") + ":" + clientResponse.get("qop") + ":")
					.getBytes("ISO-8859-1"));
			kd.write(hexA2);

			md5.update(kd.toByteArray());
			digest = md5.digest();

			return new String(binaryToHex(digest), "ISO-8859-1");
		} catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		return null;

	}

	public void verifyMD5ClientResponse(String response)
	{
		System.out.println("(C) Authentication response received");
		state = 3;
		try
		{
			clientResponse = new HashMap<String, Object>();
			String decoded = new String(Base64.decode(response), "ISO-8859-1");
			String[] splitted = decoded.split(",");
			for(String token : splitted)
			{
				String[] parts = token.split("=", 2);
				if(parts.length == 2)
				{
					String key = parts[0];
					String tempValue = parts[1];
					String value = "";
					if(tempValue.contains("\""))
						value = tempValue.substring(1, tempValue.length() - 1);
					else value = tempValue;
					clientResponse.put(key, value);
				}
			}

			if(checkAllRequiredResposeKeywords())
			{
				if(this.nonce.equals(clientResponse.get("nonce")))
				{

					nc = Integer.parseInt((String) clientResponse.get("nc"));
					if(nc == 1)
					{
						if(stream.getServer().equals(clientResponse.get("realm")))
						{
							if("auth".equals(clientResponse.get("qop")))
							{
								userName = (String) clientResponse.get("username");
								serverHash = calculateMD5Response(false);
								clientHash = (String) clientResponse.get("response");
								String checkHash = calculateMD5Response(true);

								if(clientHash.equals(checkHash))
								{
									connection.sendString(
											"<challenge xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>"
													+ Base64.encodeBytes(("rspauth=" + serverHash)
															.getBytes("ISO-8859-1"))
													+ "</challenge>", 1);
									System.out.println("(S) 2nd authentication challenge sended");
									state = 4;
									return;
								} else connection.sendString(
										"<failure xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>"
												+ "<not-authorized/></failure>", 1);

							}
						}
					}
				}

			}

		} catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		try
		{
			connection.sendString("<response xmlns='urn:ietf:params:xml:ns:xmpp-sasl'/>", 1);
			connection.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (XMLStreamException e)
		{
			e.printStackTrace();
		}

	}

	public void successAuthenfication()
	{
		System.out.println("(C) Authenfication accepted");
		state = 5;
		try
		{
			connection.sendString("<success xmlns='urn:ietf:params:xml:ns:xmpp-sasl'/>", 1);
			System.out.println("(S) Authenfication accepted");
			stream.getJID().setUserName(userName);
			stream.getJID().setContacts(db.getUser(userName).getJIDs());
			stream.getJID().setGroup(db.getGroupOfUser(userName));
			state = 6;
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public int getState()
	{
		return state;
	}
}

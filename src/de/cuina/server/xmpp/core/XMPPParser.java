package de.cuina.server.xmpp.core;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import de.cuina.server.xmpp.core.XMPPServer.Connection;
import de.cuina.server.xmpp.data.Chat;
import de.cuina.server.xmpp.data.JID;
import de.cuina.server.xmpp.data.Session;
import de.cuina.server.xmpp.data.Stream;

public class XMPPParser extends Thread
{
	private XMLStreamReader reader;
	private Connection connection;
	private SASLAuthenfication authenfication;
	private Session session;

	private Stream stream = null;

	private LinkedList<XMPPStanza> stanzaStack = new LinkedList<XMPPStanza>();

	private String tempMessage = "";

	private class XMPPStanza
	{
		private String name, localName;
		private String value;
		private HashMap<String, String> attributes = new HashMap<String, String>();

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public String getValue()
		{
			return value;
		}

		public void setValue(String value)
		{
			this.value = value;
		}

		public HashMap<String, String> getAttributes()
		{
			return attributes;
		}

		public String getLocalName()
		{
			return localName;
		}

		public void setLocalName(String localName)
		{
			this.localName = localName;
		}
	}

	public XMPPParser(XMLStreamReader reader, Connection connection)
	{
		this.reader = reader;
		this.connection = connection;
	}

	XMPPStanza currentStanza;

	@Override
	public void run()
	{
		try
		{
			while(reader.hasNext() && !connection.isClosed())
			{
				reader.next();

				if(reader.getEventType() == XMLStreamReader.START_ELEMENT)
				{
					currentStanza = new XMPPStanza();
					currentStanza.setName(reader.getName().toString());
					currentStanza.setLocalName(reader.getLocalName());

					for(int i = 0; i < reader.getAttributeCount(); i++)
					{
						currentStanza.getAttributes().put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
					}

					stanzaStack.push(currentStanza);

					if(session == null)
						System.out.println("received (" + connection.hashCode() + "): " + currentStanza.getName());
					else
						System.out.println("received (" + session.getJID().getUserName() + "): "
								+ currentStanza.getName());
					for(int i = 0; i < reader.getAttributeCount(); i++)
					{
						System.out.println(reader.getAttributeLocalName(i) + " : " + reader.getAttributeValue(i));
					}

					if(isCurrent("{http://etherx.jabber.org/streams}stream"))
					{
						System.out.println("(C) Stream request received");

						if(currentStanza.getAttributes().containsKey("to")
								&& currentStanza.getAttributes().containsKey("version"))
						{
							if(stream == null)
							{

								if(connection.isSSL() || !new File(connection.getKeyStoreFileName()).exists())
								{
									stream = new Stream(currentStanza.getAttributes().get("to"), currentStanza
											.getAttributes().get("version"));

									connection.sendString(
											"<?xml version='1.0'?>" + "<stream:stream from='" + stream.getServer()
													+ "' id='" + stream.getId() + "' version='" + stream.getVersion()
													+ "' xml:lang='en' xmlns='jabber:client' "
													+ "xmlns:stream='http://etherx.jabber.org/streams'>"
													+ XMPPServer.STREAM_FEATURES_INIT, 1);
								} else
								{
									if(new File("server.ks").exists())
									{
										Stream tempStream = new Stream(currentStanza.getAttributes().get("to"),
												currentStanza.getAttributes().get("version"));

										connection.sendString("<?xml version='1.0'?>" + "<stream:stream from='"
												+ tempStream.getServer() + "' id='" + tempStream.getId()
												+ "' version='" + tempStream.getVersion()
												+ "' xml:lang='en' xmlns='jabber:client' "
												+ "xmlns:stream='http://etherx.jabber.org/streams'>"
												+ XMPPServer.STREAM_FEATURES_SSL_INIT, 1);
									}

								}
							} else
							{

								connection.sendString(
										"<?xml version='1.0'?>" + "<stream:stream from='" + stream.getServer()
												+ "' id='" + stream.getId() + "' version='" + stream.getVersion()
												+ "' xml:lang='en' xmlns='jabber:client' "
												+ "xmlns:stream='http://etherx.jabber.org/streams'>"
												+ XMPPServer.STREAM_FEATURES_BIND, 1);
							}

							System.out.println("(S) Stream response sended");
						}

					} else

					if(isCurrent("{urn:ietf:params:xml:ns:xmpp-tls}starttls"))
					{
						connection.sendString("<proceed xmlns='urn:ietf:params:xml:ns:xmpp-tls'/>", 1);
						try
						{
							connection.setSecureSocket();
						} catch(NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException
								| CertificateException | KeyStoreException e)
						{
							e.printStackTrace();
						}
					}

					if(isCurrent("{urn:ietf:params:xml:ns:xmpp-sasl}auth"))
					{
						if(currentStanza.getAttributes().containsKey("mechanism"))
						{
							authenfication = new SASLAuthenfication(connection, stream, currentStanza.getAttributes()
									.get("mechanism"));
						} else
						{
							connection.close();
							break;
						}

					} else

					if(isCurrent("{urn:ietf:params:xml:ns:xmpp-sasl}response"))
					{
						if(authenfication != null)
							if(authenfication.getState() == 4)
							{
								authenfication.successAuthenfication();
							}
					} else

					if(isCurrent("{urn:ietf:params:xml:ns:xmpp-session}session"))
					{
						if(isIqSet(1))
						{
							if(!connection.getOpenSessions().containsKey(stream.getJID().getJIDString()))
							{
								session = new Session();
								session.setJID(stream.getJID());
								session.addConnection(connection);
								connection.getOpenSessions().put(stream.getJID().getJIDString(), session);
								connection.setUserName(stream.getJID().getUserName());
							} else
							{
								session = connection.getOpenSessions().get(stream.getJID().getJIDString());
								session.setJID(stream.getJID());
								session.addConnection(connection);
								connection.setUserName(stream.getJID().getUserName());
							}

							connection.sendString("<iq from='" + stream.getServer() + "' " + "type='result' " + "id='"
									+ stanzaStack.get(1).getAttributes().get("id") + "'/>", 1);

						}
					} else

					if(isCurrent("{http://jabber.org/protocol/disco#items}query"))
					{
						if(isIqGet(1))
						{
							connection.sendString("<iq from='" + stream.getServer() + "' " + "type='result' " + "id='"
									+ stanzaStack.get(1).getAttributes().get("id")
									+ "'><query xmlns='http://jabber.org/protocol/disco#items'/></iq>", 1);

						}
					} else

					if(isCurrent("{http://jabber.org/protocol/disco#info}query"))
					{
						if(isIqGet(1))
						{
							connection.sendString("<iq type='result' from='" + stream.getServer() + "' " + "id='"
									+ stanzaStack.get(1).getAttributes().get("id") + "'>"
									+ "<query xmlns='http://jabber.org/protocol/disco#info'>" + "<identity "
									+ "category='conference' " + "type='text' " + "name='Play-Specific Chatrooms'/>"
									+ "<identity " + "category='directory' "
									+ "type='chatroom' "
									+ "name='Play-Specific Chatrooms'/>"
									+ "<feature var='http://jabber.org/protocol/disco#info'/>"
									+ "<feature var='http://jabber.org/protocol/disco#items'/>"
									+ "<feature var='http://jabber.org/protocol/muc'/>"
									// +
									// "<feature var='jabber:iq:register'/>"
									+ "<feature var='jabber:iq:search'/>" + "<feature var='jabber:iq:time'/>"
									+ "<feature var='jabber:iq:version'/>" + "</query>" + "</iq>", 1);

						}
					} else

					if(isCurrent("{vcard-temp}vCard"))
					{
						if(isIqGet(1))
						{
							if(session.getJID().getVCard() != null)
							{
								connection.sendString("<iq type='result' from='" + stream.getServer() + "' " + "id='"
										+ stanzaStack.get(1).getAttributes().get("id") + "'>"
										+ "<vCard xmlns='vcard-temp'>" + session.getJID().getVCard() + "</vCard></iq>",
										1);
							} else
							{
								connection.sendString("<iq type='result' from='" + stream.getServer() + "' " + "id='"
										+ stanzaStack.get(1).getAttributes().get("id") + "'>"
										+ "<vCard xmlns='vcard-temp'/></iq>", 1);
							}
						}

					} else

					if(isCurrent("{jabber:iq:roster}query"))
					{
						if(isIqGet(1))
						{
							ArrayList<JID> items = stream.getJID().getContacts();
							String answer = "<iq type='result' from='" + stream.getServer() + "' " + "id='"
									+ stanzaStack.get(1).getAttributes().get("id") + "'>";
							if(!items.isEmpty())
							{
								answer += "<query xmlns='jabber:iq:roster'>";
								for(JID contact : items)
									answer += "<item jid='" + contact.getJIDString() + "' name='"
											+ contact.getNickName() + "' subscription='"
											+ contact.getSubscriptionLevel() + "'/>";
								answer += "</query>";
							} else
								answer += "<query xmlns='jabber:iq:roster'/>";

							answer += "</iq>";

							connection.sendString(answer, 1);
						}

					} else

					if(isCurrent("{jabber:iq:last}query"))
					{
						if(isIqGet(1))
						{
							JID to = new JID(getParameterAt(1, "to"));
							session.getLastQuery(to, getParameterAt(1, "id"));
						}

						if(isIqResult(1))
						{
							JID to = new JID(getParameterAt(1, "to"));
							session.sendLastQuery(session.getJID(), to, getParameterAt(1, "id"), "result");
						}
					}

					if(isCurrent("{urn:xmpp:ping}ping"))
					{
						connection.sendString("<iq type='result' from='" + stream.getServer() + "' " + "id='"
								+ stanzaStack.get(1).getAttributes().get("id") + "'/>", 1);
					} else

					if(isCurrent("{jabber:client}message"))
					{
						if(session != null && currentStanza.getAttributes().containsKey("to"))
						{
							JID neighbor = new JID(currentStanza.getAttributes().get("to"));
							Chat chat = null;

							if(!session.containsChat(neighbor.getJIDString()))
							{
								chat = new Chat(session, session.getJID(), neighbor);
								session.addChat(neighbor.getJIDString(), chat);
							} else
								chat = session.getChat(neighbor.getJIDString());
						}
					} else

					if(isCurrent("{jabber:iq:roster}item") && getParameterAt(0, "type") == null)
					{
						if(isIqSet(2) && isStackAt(1, "{jabber:iq:roster}query"))
						{
							String id = getParameterAt(0, "jid");
							String nick = getParameterAt(0, "name");
							if(getParameterAt(0, "subscription") == null)
							{
								if(!session.getJID().containsContact(id))
								{
									JID jID = new JID(id);
									if(nick != null)
										jID.setNickName(nick);

									XMPPServer.getDatabase().addContact(jID, session.getJID().getUserName());

									session.getJID().addContact(jID);

									connection.sendString("<iq to='" + session.getJID().getJIDString()
											+ "' type='set' id='" + getParameterAt(2, "id") + "'>"
											+ "<query xmlns='jabber:iq:roster'>" + "<item jid='" + jID.getJIDString()
											+ "' name='" + nick
											+ "' subscription='none'><group></group></item></query></iq>", 0);

									connection.sendString("<iq to='" + session.getJID().getFullJIDString()
											+ "' type='result' id='" + getParameterAt(2, "id") + "'/>", 0);
								} else
								{
									JID jID = session.getJID().getContact(id);
									if(jID != null)
									{
										connection.sendString("<iq to='" + session.getJID().getFullJIDString()
												+ "' type='result' id='" + getParameterAt(2, "id") + "'/>", 0);
									}
								}

							} else
							{
								if(getParameterAt(0, "subscription").equals("remove"))
								{
									if(session.getJID().containsContact(id))
									{
										JID jID = session.getJID().getContact(id);

										XMPPServer.getDatabase().removeContact(jID, session.getJID().getUserName());
										session.getJID().getContacts().remove(jID);
										session.sendPresenceTo(jID, null, null, null, "unsubscribe");
										session.sendPresenceTo(jID, null, null, null, "unsubscribed");
									}

								}

							}

						}

					} else

					if(isCurrent("{jabber:client}presence") && getParameterAt(0, "type") != null)
					{
						if(getParameterAt(0, "type").equals("unsubscribe"))
						{
							String to = getParameterAt(0, "to");
							if(to != null)
							{
								JID jID = session.getJID().getContact(to);
								jID.setSubscriptionLevel("none");
								XMPPServer.getDatabase().setSubscriptionLevel(jID, session.getJID().getUserName(),
										"none");
								session.sendPresenceTo(jID, null, null, null, "unavailable");
								session.sendPresenceTo(jID, null, null, null, "unsubscribed");
								session.sendSubscriptionRoster(jID);
							}
						} else
						if(getParameterAt(0, "type").equals("subscribe"))
						{
							String to = getParameterAt(0, "to");
							if(to != null)
							{
								JID jID = session.getJID().getContact(to);
								if(jID == null)
								{
									jID = new JID(to);
									session.getJID().addContact(jID);
									XMPPServer.getDatabase().addContact(jID, session.getJID().getUserName());
								}
								
								System.out.println(" => " + jID.getSubscriptionLevel() + " x " + jID.getAskLevel());
								
								if(!jID.getAskLevel().equals("subscribe"))
								{
									jID.setSubscriptionLevel("to");
									XMPPServer.getDatabase().setSubscriptionLevel(jID, session.getJID().getUserName(),
											"to");
									session.sendPresenceTo(jID, null, null, null, "subscribe");
									session.sendSubscriptionAskRoster(jID);
								}
								else
								{
									jID.setSubscriptionLevel("to");
									XMPPServer.getDatabase().setSubscriptionLevel(jID, session.getJID().getUserName(),
											"to");
									session.sendPresenceTo(jID, null, null, null, "subscribe");
									session.sendRoster();
								}

							}

						}

						if(getParameterAt(0, "type").equals("subscribed"))
						{
							String to = getParameterAt(0, "to");
							if(to != null)
							{
								JID jID = session.getJID().getContact(to);
								
								if(jID == null)
								{
									jID = new JID(to);
									session.getJID().addContact(jID);
									XMPPServer.getDatabase().addContact(jID, session.getJID().getUserName());
								}
								
								System.out.println(" => " + jID.getSubscriptionLevel() + " x " + jID.getAskLevel());
								
								if(jID.getSubscriptionLevel().equals("none") && !jID.getAskLevel().equals("subscribe"))
								{
									jID.setSubscriptionLevel("from");
									XMPPServer.getDatabase().setSubscriptionLevel(jID, session.getJID().getUserName(),
											"from");
									session.sendPresenceTo(jID, null, null, null, "subscribed");
									session.sendSubscriptionRoster(jID);
								} else

								if((jID.getSubscriptionLevel().equals("none") || jID.getSubscriptionLevel().equals(
										"from") || jID.getSubscriptionLevel().equals("to"))
										&& jID.getAskLevel().equals("subscribe"))
								{
									JID myJid = new JID(session.getJID().getJIDString());
									
									jID.setSubscriptionLevel("both");
									XMPPServer.getDatabase().setSubscriptionLevel(jID, myJid.getUserName(),
											"both");
									
									session.getJID().setSubscriptionLevel("both");
									XMPPServer.getDatabase().setSubscriptionLevel(myJid, jID.getUserName(),
											"both");
									
									session.sendPresenceTo(jID, null, null, null, "subscribed");
									session.sendSubscriptionRoster(jID);
									session.sendRoster();
									session.broadcastPresence();
									
									Session otherSession = session.getOthersSession(jID);
									
									otherSession.sendPresenceTo(myJid, null, null, null, "subscribed");
									otherSession.sendSubscriptionRoster(myJid);
									otherSession.sendRoster();
									otherSession.broadcastPresence();
								}
							}
						}
					} else
					{
						int pos = getPosInStack("{vcard-temp}vCard");
						if(pos > 0)
						{
							if(isIqSet(pos - 1))
							{
								session.getJID().addVCard("<" + currentStanza.getName() + ">");
							}
						}
					}

				} else

				if(reader.getEventType() == XMLStreamReader.CHARACTERS)
				{
					currentStanza.setValue(reader.getText());
					// System.out.println(reader.getText());
					if(isCurrent("{urn:ietf:params:xml:ns:xmpp-sasl}response") && authenfication != null)
					{
						if(authenfication.getState() == 2)
							authenfication.verifyMD5ClientResponse(reader.getText());
					} else

					if(isCurrent("{urn:ietf:params:xml:ns:xmpp-bind}resource"))
					{
						if(isStackAt(1, "{urn:ietf:params:xml:ns:xmpp-bind}bind") && isIqSet(2))
						{
							stream.getJID().setResource(currentStanza.getValue());
						}

					} else if(isCurrent("{jabber:client}body"))
					{
						if(isStackAt(1, "{jabber:client}message"))
						{
							tempMessage += currentStanza.getValue();
						}
					} else

					if(isCurrent("{jabber:client}show"))
					{
						if(isStackAt(1, "{jabber:client}presence") && getParameterAt(1, "type") == null)
						{
							session.setShow(currentStanza.getValue());
						}
					} else

					if(isCurrent("{jabber:client}status"))
					{
						if(isStackAt(1, "{jabber:client}presence") && getParameterAt(1, "type") == null)
						{
							session.setStatus(currentStanza.getValue());
						}
					} else

					if(isCurrent("{jabber:client}priority"))
					{
						if(isStackAt(1, "{jabber:client}presence") && getParameterAt(1, "type") == null)
						{
							session.setPresence(currentStanza.getValue());
						}
					} else

					if(currentStanza.getLocalName().equals("script"))
					{
						if(session.getJID().getGroup() != null)
							if(session.getJID().getGroup().equals("root"))
							{
								String command = currentStanza.value;
								String message = "";

								if(command.equals("get sessions"))
								{
									message = connection.getOpenSessions().size() + " session(s):\n";
									for(String session : connection.getOpenSessions().keySet())
									{
										Session scriptSession = connection.getOpenSessions().get(session);
										message += session + "{" + scriptSession.getConnections().size()
												+ " connection(s), " + scriptSession.getChats().size() + " chat(s), "
												+ scriptSession.getUndeliveredSendCount() + " delivered message(s)}"
												+ "\n";
									}
									connection.sendString("<script type='result'>" + message + "</script>", 0);
								} else

								if(command.equals("get registered users"))
								{
									String[] users = XMPPServer.getDatabase().getRegisteredUsers();
									message = users.length + " registered user(s):\n";
									for(String user : users)
										message += user + "\n";
									connection.sendString("<script type='result'>" + message + "</script>", 0);
								} else

								if(command.startsWith("kick"))
								{
									message = "kick:\n";
									command = command.substring(5, command.length());
									String[] users = command.split(" ");
									if(users.length == 1 && users[0].equals("$all"))
									{
										users = XMPPServer.getDatabase().getRegisteredUsers();
									}
									for(String user : users)
									{
										Session scriptSession = connection.getOpenSessions().get(
												user + "@" + stream.getServer());
										if(scriptSession != null)
										{
											for(Connection c : scriptSession.getConnections())
											{
												c.close();
											}
											connection.getOpenSessions().remove(user + "@" + stream.getServer());
											scriptSession = null;
											message += user + " kicked!\n";
										} else
											message += user + " not found!\n";
									}

									connection.sendString("<script type='result'>" + message + "</script>", 0);
								} else

								if(command.startsWith("get chats"))
								{
									message = "chats:\n";
									command = command.substring(10, command.length());
									String[] users = command.split(" ");
									if(users.length == 1 && users[0].equals("$all"))
									{
										users = XMPPServer.getDatabase().getRegisteredUsers();
									}
									for(String user : users)
									{
										Session scriptSession = connection.getOpenSessions().get(
												user + "@" + stream.getServer());
										if(scriptSession != null)
										{
											message += user + ": { ";
											for(String chatName : scriptSession.getChats().keySet())
											{
												message += chatName + " ";
											}
											message += "}\n";
										} else
											message += user + " not found!\n";
									}

									connection.sendString("<script type='result'>" + message + "</script>", 0);
								} else

								if(command.equals("stop server"))
								{
									connection.sendString("<script type='result'>stop server now</script>", 0);
									connection.stopServer();
								} else

								if(command.startsWith("register"))
								{
									command = command.substring(9, command.length());
									String[] users = command.split(" ");

									if(users.length >= 1)
									{
										for(String user : users)
										{
											if(XMPPServer.getDatabase().getUser(user) == null)
											{
												XMPPServer.getDatabase().addUser(user, "nopw", "users", null);
												connection.sendString("<script type='result'>registered " + user
														+ "</script>", 0);
											} else
												connection.sendString("<script type='error'>" + user
														+ " is allready registered</script>", 0);
										}
									}

								} else

								if(command.startsWith("set password"))
								{
									command = command.substring(13, command.length());
									String[] users = command.split(" ");

									if(users.length == 2)
									{
										String user = users[0];
										String password = users[1];

										if(XMPPServer.getDatabase().getUser(user) != null)
										{
											XMPPServer.getDatabase().setPassword(user, password);
											connection.sendString("<script type='result'>set password for " + user
													+ " done</script>", 0);
										} else
											connection.sendString("<script type='error'>" + user
													+ " not found</script>", 0);
									}

								} else

								if(command.startsWith("set group"))
								{
									command = command.substring(10, command.length());
									String[] users = command.split(" ");

									if(users.length == 2)
									{
										String user = users[0];
										String group = users[1];

										if(XMPPServer.getDatabase().getUser(user) != null)
										{
											XMPPServer.getDatabase().setGroup(user, group);
											connection.sendString("<script type='result'>set group for " + user
													+ " done</script>", 0);
										} else
											connection.sendString("<script type='error'>" + user
													+ " not found</script>", 0);
									}

								} else

								if(command.startsWith("get group"))
								{
									command = command.substring(10, command.length());
									String[] users = command.split(" ");

									if(users.length == 1 && users[0].equals("$all"))
									{
										users = XMPPServer.getDatabase().getRegisteredUsers();
									}
									message = "group:\n";
									for(String user : users)
									{
										if(XMPPServer.getDatabase().getUser(user) != null)
											message += user + " : " + XMPPServer.getDatabase().getGroup(user) + "\n";
										else
											message += user + " not found!\n";
									}
									connection.sendString("<script type='result'>" + message + "</script>", 0);

								} else

								if(command.startsWith("get roster"))
								{
									command = command.substring(11, command.length());
									String[] users = command.split(" ");

									if(users.length == 1 && users[0].equals("$all"))
									{
										users = XMPPServer.getDatabase().getRegisteredUsers();
									}
									message = "roster:\n";
									for(String user : users)
									{
										if(XMPPServer.getDatabase().getUser(user) != null)
										{
											message += user + " : {";

											for(JID jid : XMPPServer.getDatabase().getUser(user).getJIDs())
												message += "\n" + jid.getJIDString() + "~" + jid.getSubscriptionLevel();
											if(XMPPServer.getDatabase().getUser(user).getJIDs().length > 0)
												message += "\n";
											message += "}\n";
										} else
											message += user + " not found!\n";
									}
									connection.sendString("<script type='result'>" + message + "</script>", 0);
								}

								if(command.startsWith("help"))
								{
									connection.sendString("<script type='result'>cuina server script help\n"
											+ "available commands:\n" + "get sessions\n" + "get registered users\n"
											+ "kick [users | $all]\n" + "get chats [users | $all]\n" + "stop server\n"
											+ "register [users]\n" + "set password [user] [password]\n"
											+ "set group [user] [group]\n" + "get group [users | $all]\n"
											+ "get roster [users | $all]\n" + "</script>", 0);
								}
							} else
								connection.sendString("<script type='error'>"
										+ "No permission:\n You are not a member of group 'root'.\n" + "</script>", 0);
						else
							connection.sendString("<script type='error'>"
									+ "No permission:\n You are not a member of group 'root'\n" + "</script>", 0);
					} else
					{
						int pos = getPosInStack("{vcard-temp}vCard");
						if(pos > 0)
						{
							if(isIqSet(pos - 1))
							{
								session.getJID().addVCard(currentStanza.getValue());
							}
						}
					}

				} else

				if(reader.getEventType() == XMLStreamReader.END_ELEMENT)
				{
					if(isCurrent(reader.getName().toString()))
					{
						// System.out.println("end: " +
						// stanzaStack.peek().getName());
						if(isCurrent("{urn:ietf:params:xml:ns:xmpp-bind}bind"))
							connection.sendString(
									"<iq type='result' id='" + stanzaStack.get(1).getAttributes().get("id") + "'>"
											+ "<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>" + "<jid>"
											+ stream.getJID().getJIDString() + "</jid>" + "</bind>" + "</iq>", 0);
						else if(isCurrent("{jabber:client}presence") && getParameterAt(0, "type") == null)
							session.broadcastPresence();
						else if(isCurrent("{jabber:client}message"))
						{
							JID neighbor = new JID(stanzaStack.peek().getAttributes().get("to"));

							if(session.containsChat(neighbor.getJIDString()))
							{
								Chat chat = session.getChat(neighbor.getJIDString());
								chat.sendtoAll(session.getJID(), tempMessage);
								tempMessage = "";
							}
						} else
						{
							int pos = getPosInStack("{vcard-temp}vCard");
							if(pos > 0)
							{
								if(isIqSet(pos - 1))
								{
									session.getJID().addVCard("</" + currentStanza.getName() + ">");
								}
							}
						}
						stanzaStack.pop();
						currentStanza = stanzaStack.peek();
					}

				}
			}
		} catch(XMLStreamException e)
		{
			System.out.println("(C) Connection closed by client");
			if(session != null)
			{
				session.broadcastPresence();
				session.removeConnection(connection);
			}
		} catch(IOException e)
		{
			try
			{
				System.out.println("(S) Connection closed by IOException");
				connection.close();
			} catch(IOException | XMLStreamException e1)
			{
				
			}
		} 
	}

	private boolean isCurrent(String name)
	{
		if(currentStanza != null)
			return currentStanza.getName().equals(name);
		return false;
	}

	private boolean isStackAt(int index, String name)
	{
		if(stanzaStack.get(index) != null)
			return stanzaStack.get(index).getName().equals(name);
		return false;
	}

	private String getParameterAt(int index, String key)
	{
		if(stanzaStack.get(index).getAttributes().containsKey(key))
			return stanzaStack.get(index).getAttributes().get(key);
		return null;
	}

	private boolean isIqGet(int index)
	{
		if(stanzaStack.get(index) != null)
			if(stanzaStack.get(index).getName().equals("{jabber:client}iq"))
				if(stanzaStack.get(index).getAttributes().get("type") != null)
					if(stanzaStack.get(index).getAttributes().get("type").equals("get"))
						return true;
		return false;
	}

	private boolean isIqSet(int index)
	{
		if(stanzaStack.get(index) != null)
			if(stanzaStack.get(index).getName().equals("{jabber:client}iq"))
				if(stanzaStack.get(index).getAttributes().get("type") != null)
					if(stanzaStack.get(index).getAttributes().get("type").equals("set"))
						return true;
		return false;
	}

	private boolean isIqResult(int index)
	{
		if(stanzaStack.get(index) != null)
			if(stanzaStack.get(index).getName().equals("{jabber:client}iq"))
				if(stanzaStack.get(index).getAttributes().get("type") != null)
					if(stanzaStack.get(index).getAttributes().get("type").equals("result"))
						return true;
		return false;
	}

	private int getPosInStack(String name)
	{
		for(int i = 0; i < stanzaStack.size(); i++)
		{
			if(stanzaStack.get(i) != null)
				if(stanzaStack.get(i).getName().equals(name))
					return i;
		}
		return -1;
	}

	public synchronized void setReader(XMLStreamReader reader)
	{
		this.reader = reader;
	}

}
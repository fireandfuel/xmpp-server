package de.cuina.server.xmpp.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import de.cuina.server.xmpp.core.XMPPServer.Connection;

public class Session
{
	private JID jID;
	private HashMap<String, Chat> chats = new HashMap<String, Chat>();
	private ArrayList<Connection> connections = new ArrayList<Connection>();
	private String show, status, presence = null;
	private ArrayList<String> undeliveredSend = new ArrayList<String>();
	private long lastActivity;

	public JID getJID()
	{
		return jID;
	}

	public void setJID(JID jID)
	{
		this.jID = jID;
	}

	public HashMap<String, Chat> getChats()
	{
		return chats;
	}

	public void addChat(String id, Chat chat)
	{
		chats.put(id, chat);
	}

	public boolean containsChat(String id)
	{
		return chats.containsKey(id);
	}

	public Chat getChat(String id)
	{
		return chats.get(id);
	}

	public void addConnection(Connection connection)
	{
		boolean firstConnect = this.connections.isEmpty();
		this.connections.add(connection);
		if(firstConnect)
		{
			for(String send : undeliveredSend)
				try
				{
					connection.sendString(send, 1);
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			undeliveredSend.clear();
		}
	}

	public void removeConnection(Connection connection)
	{
		connections.remove(connection);
	}

	public ArrayList<Connection> getConnections()
	{
		return connections;
	}

	public void sendMessage(JID from, String message)
	{
		if(connections.isEmpty())
			undeliveredSend.add("<message type='chat' to='" + jID.getFullJIDString() + "' from='"
					+ from.getFullJIDString() + "'>" + "<body>" + message + "</body></message>");
		else

		for(Connection connection : connections)
		{
			if(connection != null && !from.getJIDString().equals(jID.getJIDString()))
				try
				{
					connection.sendString("<message type='chat' to='" + jID.getFullJIDString()
							+ "' from='" + from.getFullJIDString() + "'>" + "<body>" + message
							+ "</body></message>", 1);
				} catch (IOException e)
				{
					e.printStackTrace();
				}
		}
	}

	public String getShow()
	{
		return show;
	}

	public void setShow(String show)
	{
		this.show = show;
	}

	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

	public String getPresence()
	{
		return presence;
	}

	public void setPresence(String presence)
	{
		this.presence = presence;
		this.status = null;
		this.show = null;
	}

	public void broadcastPresence()
	{
		ArrayList<JID> jIDs;
		HashMap<String, Session> sessions = new HashMap<String, Session>();

		jIDs = jID.getContacts();
		HashMap<String, Session> allSessions = getConnections().get(0).getOpenSessions();
		for(JID jID : jIDs)
		{
			if(allSessions.containsKey(jID.getJIDString()))
				sessions.put(jID.getJIDString(), allSessions.get(jID.getJIDString()));
			else
			{
				Session newSession = new Session();
				newSession.setJID(jID);
				allSessions.put(jID.getJIDString(), newSession);
				sessions.put(jID.getJIDString(), newSession);
			}
		}

		for(String key : sessions.keySet())
		{
			sessions.get(key).sendPresence(jID, show, status, presence, null, null);
		}
	}

	public void sendPresenceTo(JID to, String show, String status, String presence, String type)
	{
		sendPresenceTo(to, show, status, presence, type, null);
	}

	public void sendPresenceTo(JID to, String show, String status, String presence, String type,
			String id)
	{
		Session otherSession = getOthersSession(to);

		otherSession.sendPresence(this.jID, show, status, presence, type, id);
	}

	private void sendPresence(JID from, String show, String status, String presence, String type,
			String id)
	{
		String toSend = "<presence from='" + from.getJIDString() + "' to='"
				+ jID.getJIDString() + "'";

		if(type != null)
			toSend += " type='" + type + "'";

		if(id != null)
			toSend += " id='" + id + "'";

		if(status == null && presence == null && show == null)
			toSend += "/>";
		else
		{
			toSend += ">";
			if(status != null)
				toSend += "<status>" + status + "</status>";
			if(show != null)
				toSend += "<show>" + show + "</show>";
			if(presence != null)
				toSend += "<priority>" + presence + "</priority>";

			toSend += "</presence>";
		}

		if(connections.isEmpty())
			undeliveredSend.add(toSend);
		else for(Connection connection : connections)
		{
			if(connection != null && !from.getJIDString().equals(jID.getJIDString()))
				try
				{
					connection.sendString(toSend, 1);
				} catch (IOException e)
				{
					e.printStackTrace();
				}
		}



	}

	public void sendRoster()
	{
		ArrayList<JID> items = jID.getContacts();

		String answer = "<iq type='set' from='" + jID.getServer() + "' " + "id='" + Math.random()
				+ "'>";

		if(!items.isEmpty())
		{
			answer += "<query xmlns='jabber:iq:roster'>";
			for(JID contact : items)
				answer += "<item jid='" + contact.getJIDString() + "' name='"
						+ contact.getNickName() + "' subscription='"
						+ contact.getSubscriptionLevel() + "'/>";
			answer += "</query>";
		} else answer += "<query xmlns='jabber:iq:roster'/>";

		answer += "</iq>";

		for(Connection connection : connections)
		{
			if(connection != null)
				try
				{
					connection.sendString(answer, 0);
				} catch (IOException e)
				{
					e.printStackTrace();
				}
		}

	}
	
	public void sendSubscriptionAskRoster(JID contact)
	{
		
		for(Connection connection : connections)
		{
			if(connection != null)
				try
				{
					connection.sendString("<iq type='set' " + "id='" + Math.random()
					+ "' to='" + getJID().getFullJIDString() + "'>"
					+ "<query xmlns='jabber:iq:roster'>"
					+ "<item ask='subscribe' jid='" + contact.getJIDString() + "' name='"
					+ contact.getNickName() + "' subscription='"
					+ contact.getSubscriptionLevel() + "'/></query></iq>", 0);
					contact.setAskLevel("subscribe");
				} catch(IOException e)
				{
					e.printStackTrace();
				}
		}
		
	}
	
	public void sendSubscriptionRoster(JID contact)
	{
		
		for(Connection connection : connections)
		{
			if(connection != null)
				try
				{
					connection.sendString("<iq type='set' " + "id='" + Math.random()
					+ "' to='" + getJID().getFullJIDString() + "'>"
					+ "<query xmlns='jabber:iq:roster'>"
					+ "<item jid='" + contact.getJIDString() + "' name='"
					+ contact.getNickName() + "' subscription='"
					+ contact.getSubscriptionLevel() + "'/></query></iq>", 0);
					contact.setAskLevel("subscribe");
				} catch(IOException e)
				{
					e.printStackTrace();
				}
		}
		
	}
	
	public boolean canDestroy()
	{
		return undeliveredSend.isEmpty() && connections.isEmpty();
	}
	
	public int getUndeliveredSendCount()
	{
		return undeliveredSend.size();
	}

	public void getLastQuery(JID to, String id)
	{
		Session otherSession = getOthersSession(to);
		if(!otherSession.getConnections().isEmpty())
			otherSession.sendLastQuery(getJID(), to, id, "get");
		else
		{
			for(Connection connection : connections)
			{
				String message = "<iq from='" + getJID().getServer() + "' " + "id='" + id
						+ "' to='" + getJID().getJIDString() + "' " + "type='result"
						+ "'><query xmlns='jabber:iq:last'";
				message += ">"
						+ ((System.currentTimeMillis() - otherSession.getLastActivity()) / 1000)
						+ "</query>";
				message += "</iq>";

				try
				{
					connection.sendString(message, 1);
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	public void sendLastQuery(JID from, JID to, String id, String type)
	{
		for(Connection connection : connections)
		{
			String message = "<iq from='" + from.getFullJIDString() + "' " + "id='" + id + "' to='"
					+ to.getJIDString() + "' " + "type='" + type
					+ "'><query xmlns='jabber:iq:last'";
			if(type.equals("get"))
				message += "/>";
			else message += ">" + ((System.currentTimeMillis() - lastActivity) / 1000) + "</query>";
			message += "</iq>";

			try
			{
				connection.sendString(message, 1);
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public Session getOthersSession(JID other)
	{
		HashMap<String, Session> allSessions = getConnections().get(0).getOpenSessions();

		Session otherSession = null;

		if(allSessions.containsKey(other.getJIDString()))
		{
			otherSession = allSessions.get(other.getJIDString());
		} else
		{
			otherSession = new Session();
			otherSession.setJID(other);
			allSessions.put(other.getJIDString(), otherSession);
		}

		return otherSession;
	}

	public long getLastActivity()
	{
		return lastActivity;
	}

}

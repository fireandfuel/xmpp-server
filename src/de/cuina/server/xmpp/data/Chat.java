package de.cuina.server.xmpp.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Chat
{
	private ArrayList<JID> jIDs;
	private HashMap<String, Session> sessions = new HashMap<String, Session>();

	public Chat(Session session, JID... jIDs)
	{
		this.jIDs = new ArrayList<JID>(Arrays.asList(jIDs));
		HashMap<String, Session> allSessions = session.getConnections().get(0)
				.getOpenSessions();
		for(JID jID : jIDs)
		{
			System.out.println(jID.getFullJIDString());
			if(allSessions.containsKey(jID.getJIDString()))
			{
				Session othersSession = allSessions.get(jID.getJIDString());
				
				if(jID != this.jIDs.get(0))
					othersSession.addChat(this.jIDs.get(0).getJIDString(), this);
				sessions.put(jID.getJIDString(), othersSession);
			}

			else
			{
				Session newSession = new Session();
				newSession.setJID(jID);
				allSessions.put(jID.getJIDString(), newSession);
				sessions.put(jID.getJIDString(), newSession);
				newSession.addChat(this.jIDs.get(0).getJIDString(), this);
			}

		}
	}

	public synchronized JID[] getAllJIDs()
	{
		JID[] array = new JID[jIDs.size()];
		jIDs.toArray(array);
		return array;
	}

	public synchronized void sendtoAll(JID sender, String message)
	{
		for(String key : sessions.keySet())
		{
			Session session = sessions.get(key);
			session.sendMessage(sender, message);
		}
	}

}

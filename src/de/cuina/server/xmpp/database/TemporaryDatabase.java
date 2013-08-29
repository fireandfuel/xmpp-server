package de.cuina.server.xmpp.database;

import java.util.HashMap;

import de.cuina.server.xmpp.data.JID;
import de.cuina.server.xmpp.data.Property;

public class TemporaryDatabase implements IDatabaseProvider
{
	private HashMap<String, Property> users = new HashMap<String, Property>();
	
	public TemporaryDatabase()
	{
		Property nk = new Property("test");
		nk.setGroup("root");
		Property test = new Property("testme");
		JID testJID = new JID("test", "localhost");
		testJID.setSubscriptionLevel("none");
		JID nkJID = new JID("nk", "localhost");
		nkJID.setSubscriptionLevel("none");
		nk.addJID(testJID);
		test.addJID(nkJID);
		users.put("nk", nk);
		users.put("test", test);
	}
	
	@Override
	public Property getUser(String userName)
	{
		return users.get(userName);
	}
	
	@Override
	public void addContact(JID jID, String userName)
	{
		users.get(userName).addJID(jID);
	}
	
	@Override
	public void removeContact(JID jID, String userName)
	{
		users.get(userName).removeJID(jID);
	}
	
	@Override
	public void setSubscriptionLevel(JID jID, String userName, String level)
	{
		for(JID searchjID : users.get(userName).getJIDs())
		{
			if(searchjID.equals(jID))
			{	
				searchjID.setSubscriptionLevel(level);
				return;
			}
		}
	}
	
	@Override
	public String getGroup(String userName)
	{
		return users.get(userName).getGroup();
	}
	
	@Override
	public void setGroup(String userName, String group)
	{
		users.get(userName).setGroup(group);
	}
	
	@Override
	public String[] getRegisteredUsers()
	{
		String[] array = new String[users.keySet().size()];
		return users.keySet().toArray(array);
	}

	@Override
	public void addUser(String user, String password, String group, String vcard)
	{

	}

	@Override
	public void setPassword(String userName, String password)
	{

	}

}

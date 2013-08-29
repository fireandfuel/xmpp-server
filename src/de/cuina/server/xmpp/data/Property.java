package de.cuina.server.xmpp.data;

import java.util.LinkedList;

public class Property
{
	private String password;
	private LinkedList<JID> jIDs = new LinkedList<JID>();
	private String group;
	
	public Property(String password)
	{
		this.password = new String(password);
	}
	
	public String getPassword()
	{
		return new String(password);
	}
	
	public void addJID(JID jID)
	{
		jIDs.add(jID);
	}
			
	public void removeJID(JID jID)
	{
		jIDs.remove(jID);
	}
	
	public JID[] getJIDs()
	{
		JID[] array = new JID[jIDs.size()];
		return jIDs.toArray(array);
	}

	public String getGroup()
	{
		return group;
	}

	public void setGroup(String group)
	{
		this.group = group;
	}
}
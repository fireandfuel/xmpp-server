package de.cuina.server.xmpp.data;

import java.util.ArrayList;
import java.util.Arrays;

public class JID
{
	private String userName, password, nickName, server, resource, group;
	private String vCard;
	
	private ArrayList<JID> contacts = new ArrayList<JID>();
	private int status;
	
	private String subscriptionLevel = "none";
	private String askLevel = "";

	public JID()
	{}
	
	public JID(String jIDAddress)
	{
		if(jIDAddress.matches("[a-zA-Z0-9]*@[a-zA-Z0-9]*"))
		{
			userName = jIDAddress.split("@")[0];
			server = jIDAddress.split("@")[1];
		}
		if(jIDAddress.matches("[a-zA-Z0-9]*@[a-zA-Z0-9]*/[a-zA-Z0-9]*"))
		{
			userName = jIDAddress.split("@")[0];
			server = jIDAddress.split("@")[1].split("/")[0];
			resource = jIDAddress.split("@")[1].split("/")[1];
		}
		
	}
	
	public JID(String userName, String server)
	{
		this.userName = userName;
		this.nickName = userName;
		this.server = server;
	}
	
	public String getUserName()
	{
		return userName;
	}

	public void setUserName(String userName)
	{
		this.userName = userName;
	}

	public String getNickName()
	{
		return nickName;
	}

	public void setNickName(String nickName)
	{
		this.nickName = nickName;
	}

	public String getServer()
	{
		return server;
	}
	
	public void setServer(String server)
	{
		this.server = server;
	}
	
	public String getResource()
	{
		return resource;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public void setResource(String resource)
	{
		this.resource = resource;
	}

	public String getJIDString()
	{
		return userName + "@" + server;
	}
	
	public String getFullJIDString()
	{
		if(resource == null)
			return userName + "@" + server;
		return userName + "@" + server + "/" + resource;
	}

	public void addContact(JID jID)
	{
		if(getContact(jID.getJIDString()) == null)
		{
			contacts.add(jID);
		}
	}
	
	public ArrayList<JID> getContacts()
	{
		return contacts;
	}

	public void setContacts(Object[] items)
	{
		JID[] contacts = Arrays.copyOf(items, items.length, JID[].class);
		this.contacts = new ArrayList<JID>(Arrays.asList(contacts));
	}

	public int getStatus()
	{
		return status;
	}

	public void setStatus(int status)
	{
		this.status = status;
	}

	public String getSubscriptionLevel()
	{
		return subscriptionLevel;
	}

	public void setSubscriptionLevel(String subscriptionLevel)
	{
		this.subscriptionLevel = subscriptionLevel;
		
	}
	
	public String getAskLevel()
	{
		return askLevel;
	}

	public void setAskLevel(String askLevel)
	{
		this.askLevel = askLevel;
	}

	public boolean containsContact(String id)
	{
		for(JID contact : contacts)
		{
			if(contact.equalsIgnoreResource(id))
				return true;
		}
		return false;
	}
	
	public JID getContact(String id)
	{
		for(JID contact : contacts)
		{
			if(contact.equalsIgnoreResource(id))
				return contact;
		}
		return null;
	}
	
	public boolean equalsJID(JID other)
	{
		if(other == null)
			return false;
		return this.equalsIgnoreResource(other.getJIDString());
	}
	
	public boolean equalsIgnoreResource(String other)
	{
		if(other == null)
			return false;
		if(other.matches("[a-zA-Z0-9]*@[a-zA-Z0-9]*/[a-zA-Z0-9]*"))
		{
			other = other.split("/")[0];
		}
		return this.getJIDString().equalsIgnoreCase(other);
	}

	public String getGroup()
	{
		return group;
	}

	public void setGroup(String group)
	{
		this.group = group;
	}

	public String getVCard()
	{
		return vCard;
	}

	public void addVCard(String vCard)
	{
		if(this.vCard == null)
			this.vCard = "";
		this.vCard += vCard;
	}
	
	public void deleteVCard()
	{
		this.vCard = "";
	}
}

package de.cuina.server.xmpp.database;

import de.cuina.server.xmpp.data.Group;
import de.cuina.server.xmpp.data.JID;
import de.cuina.server.xmpp.data.Property;

public interface IDatabaseProvider
{
	public Property getUser(String name);

	public void addContact(JID jID, String userName);

	public void removeContact(JID jID, String userName);

	public void setSubscriptionLevel(JID jID, String userName, String level);

	public Group getGroupOfUser(String userName);

	public Group getGroupByName(String groupName);
	
	public String[] getUsersOfGroup(String groupName);

	public void setGroup(String userName, String group);

	public void addGroup(String group);

	public void updateGroup(Group group);

	public void deleteGroup(String group);

	public Group[] getGroups();
	
	public String[] getRegisteredUsers();

	public void addUser(String userName, String password, String group, String vcard);

	public void setPassword(String userName, String password);

}

package de.cuina.server.xmpp.data;

public class Group
{
	private String name;

	private boolean active, canKick, canBan, canSetGroup, canRegister, canAdministrate, groupChat;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public boolean isActive()
	{
		return active;
	}

	public void setActive(boolean active)
	{
		this.active = active;
	}

	public boolean isCanKick()
	{
		return canKick;
	}

	public void setCanKick(boolean canKick)
	{
		this.canKick = canKick;
	}

	public boolean isCanBan()
	{
		return canBan;
	}

	public void setCanBan(boolean canBan)
	{
		this.canBan = canBan;
	}

	public boolean isCanRegister()
	{
		return canRegister;
	}

	public boolean isCanSetGroup()
	{
		return canSetGroup;
	}

	public void setCanSetGroup(boolean canSetGroup)
	{
		this.canSetGroup = canSetGroup;
	}

	public void setCanRegister(boolean canRegister)
	{
		this.canRegister = canRegister;
	}

	public boolean isCanAdministrate()
	{
		return canAdministrate;
	}

	public void setCanAdministrate(boolean canAdministrate)
	{
		this.canAdministrate = canAdministrate;
	}

	public boolean isGroupChat()
	{
		return groupChat;
	}

	public void setGroupChat(boolean groupChat)
	{
		this.groupChat = groupChat;
	}
}

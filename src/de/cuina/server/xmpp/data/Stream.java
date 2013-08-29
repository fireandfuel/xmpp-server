package de.cuina.server.xmpp.data;

public class Stream
{
	private JID jID = new JID();
	private String id;
	private String version;
	
	public Stream(String server, String version)
	{
		this.jID.setServer(server);
		this.version = version;
		this.id = this.hashCode() + "";
	}

	public JID getJID()
	{
		return jID;
	}

	public String getServer()
	{
		return this.jID.getServer();
	}

	public void setServer(String server)
	{
		this.jID.setServer(server);
	}

	public String getId()
	{
		return id;
	}

	public String getVersion()
	{
		return version;
	}

	public void setVersion(String version)
	{
		this.version = version;
	}

	
}

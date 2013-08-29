package de.cuina.server.plugin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class OSGiProvider
{
	List<Bundle> installedBundles = new LinkedList<Bundle>();
	
	public OSGiProvider()
	{
		FrameworkFactory factory = ServiceLoader.
				load(FrameworkFactory.class).iterator().next();
		Map<String, String> config = new HashMap<String, String>();
		
		Framework framework = factory.newFramework(config);
		
		
		
		try
		{
			framework.start();		
		} catch(BundleException e)
		{
			e.printStackTrace();
		}
	}
}

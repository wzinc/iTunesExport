import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.Vector;
import java.util.*;
import java.util.stream.*;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

public class iTunesExport
{
	private static Hashtable<String, Node> _libraryDictionary;
	
	public static void main(String[] args) throws Exception
	{
		if(args.length < 3)
			_printUsage(1);
		
		File libraryFile = new File(args[0]);
		File outputDirectory = new File(args[1]);
		File musicBasePath = new File(args[2]);

		int basePathDepth = 0;

		try
		{
			if (args.length > 3)
				basePathDepth = Integer.parseInt(args[3]);
		}
		catch (Exception e)
		{
			System.err.println("Unable to parse base path depth: " + args[3]);
                        System.exit(1);
		}
		
		if(!libraryFile.exists())
		{
			System.err.println("iTunes library not found: " + args[0]);
			System.exit(1);
		}
		
		if(!outputDirectory.exists() || !outputDirectory.isDirectory())
		{
			System.err.println("Output directory not found: " + args[1]);
			System.exit(1);
		}
		
		if (!musicBasePath.exists())
			System.err.println("Warning: Music base path not found: " + args[2]);
		
		Document iTunesLibrary = null;
		
		try { iTunesLibrary = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(libraryFile); }
		catch (Exception e)
		{
			System.err.println("Error reading library file.");
			e.printStackTrace();
			System.exit(1);
		}
		
		NodeList nodes = iTunesLibrary.getDocumentElement().getChildNodes();
		
		for(int i = 0 ; i < nodes.getLength() ; i++)
			if(nodes.item(i).getNodeName().equalsIgnoreCase("dict"))
			{
				nodes = nodes.item(i).getChildNodes();
				break;
			}

		_libraryDictionary = new Hashtable<String, Node>();
		
		for(int i = 0 ; i < nodes.getLength() ; i++)
		{
			Node node = nodes.item(i);
			
			if (node.getNodeName().equalsIgnoreCase("key"))
			{
				String keyText = node.getTextContent();

				// skip text nodes
				while((node = nodes.item(++i)).getNodeType() == Node.TEXT_NODE);
				_libraryDictionary.put(keyText, node);
			}
		}
		
		NodeList playlistNodes = ((Node)_libraryDictionary.get("Playlists")).getChildNodes();
		
		String playListName = null;
		Vector<Integer> tracks = new Vector<Integer>();
		Hashtable<String, Vector<Integer>> playLists = new Hashtable<String, Vector<Integer>>(); 
		
		for (int i = 0; i < playlistNodes.getLength(); i++)
		{
			Node playlistNode = playlistNodes.item(i);
			
			if (playlistNode.getNodeType() == Node.TEXT_NODE)
				continue;
			
			NodeList playListChildNodes = playlistNodes.item(i).getChildNodes();
			
			for (int j = 0; j < playListChildNodes.getLength(); j++)
			{
				Node currentNode = playListChildNodes.item(j);
				if (currentNode.getNodeType() == Node.TEXT_NODE)
					continue;
				
				if (currentNode.getNodeName().equalsIgnoreCase("key"))
				{
					if (currentNode.getTextContent().equalsIgnoreCase("Name"))
					{
						while (!(currentNode = currentNode.getNextSibling()).getNodeName().equalsIgnoreCase("string"));
							playListName = currentNode.getTextContent();
					}
					else if (currentNode.getTextContent().equalsIgnoreCase("Playlist Items"))
					{
						while (!(currentNode = currentNode.getNextSibling()).getNodeName().equalsIgnoreCase("array")) j++;
						
						NodeList trackNodes = currentNode.getChildNodes();
						for (int k = 0; k < trackNodes.getLength(); k++)
						{
							Node currentTrackNode = trackNodes.item(k);
							if (currentTrackNode.getNodeType() == Node.TEXT_NODE)
								continue;

							NodeList currentTrackNodes = currentTrackNode.getChildNodes();
							for (int l = 0; l < currentTrackNodes.getLength(); l++)
							{
								Node trackChildNode = currentTrackNodes.item(l);
								if (trackChildNode.getNodeType() == Node.TEXT_NODE)
									continue;

								if (trackChildNode.getNodeName().equalsIgnoreCase("key") && trackChildNode.getTextContent().equalsIgnoreCase("Track ID"))
								{
									while (!(trackChildNode = trackChildNode.getNextSibling()).getNodeName().equalsIgnoreCase("integer")) l++;

									tracks.add(Integer.parseInt(trackChildNode.getTextContent()));
								}
							}
						}
					}
				}
			}
			
			if (playListName != null)
				playLists.put(playListName, tracks);
			
			tracks = new Vector<Integer>();
		}
		
		_outputToDisk(playLists, outputDirectory.getPath(), musicBasePath.getPath(), basePathDepth);
	}
	
	private static String _findTrackLocation(int trackID)
	{
		NodeList tracks = (NodeList)_libraryDictionary.get("Tracks").getChildNodes();
		
		for (int i = 0; i < tracks.getLength(); i++)
		{
			Node trackNode = tracks.item(i);
			
			if (trackNode.getNodeType() == Node.TEXT_NODE)
				continue;

			if (trackNode.getNodeName().equalsIgnoreCase("key") && Integer.parseInt(trackNode.getTextContent()) == trackID)
			{
				while (!(trackNode = trackNode.getNextSibling()).getNodeName().equalsIgnoreCase("dict")) i++;
				
				NodeList trackProperties = trackNode.getChildNodes();
				
				for (int j = 0; j < trackProperties.getLength(); j++)
				{
					Node trackPropertyNode = trackProperties.item(j);

					if (trackPropertyNode.getNodeType() == Node.TEXT_NODE)
						continue;
					
					if (trackPropertyNode.getNodeName().equalsIgnoreCase("key") && trackPropertyNode.getTextContent().equalsIgnoreCase("Location"))
					{
						while (!(trackPropertyNode = trackPropertyNode.getNextSibling()).getNodeName().equalsIgnoreCase("string")) j++;
						
						return trackPropertyNode.getTextContent();
					}
				}
			}
		}
		
		return null;
	}
	
	private static void _outputToDisk(Hashtable<String, Vector<Integer>> playLists, String outputDirectoryPath, String musicBasePath, int basePathDepth) throws Exception
	{
		for(String key : playLists.keySet())
		{
			System.out.println(key + ": " + playLists.get(key).size());
			Vector<Integer> tracks = playLists.get(key);
			
			File playListFile = new File(outputDirectoryPath, key + ".m3u8");
			
			try
			{
				PrintWriter writer = new PrintWriter(playListFile);
				writer.println("#EXTM3U");
				
				for (Integer trackID : tracks)
				{
					String location = _findTrackLocation(trackID);

					if (location == null || location.indexOf(".Trash") != -1)
					{
						System.err.println(String.format("WARN: Track has no location: %d", trackID));
						continue;
					}

					if (!location.startsWith("file:///"))
						continue;

					location = location.replaceAll("%20", " ").replace("file:///", "/");
					String originalLocation = location;

					String[] pathParts = location.split("/");

					location = String.format("%s/%s", musicBasePath, String.join("/", Arrays.stream(pathParts, basePathDepth + 1, pathParts.length).toArray(String[]::new)));
					writer.println(location);
				}
				
				writer.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	private static void _printUsage(int exitCode)
	{
		System.out.println("USAGE: iTunesExport <iTunes Library XML file> <output path> <new root path> [new root path base level]");
		System.exit(exitCode);
	}
}

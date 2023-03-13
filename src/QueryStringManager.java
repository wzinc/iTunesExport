import java.net.URLEncoder;
import java.util.*;

public class QueryStringManager extends HashMap<String, String>
{
	private static final long serialVersionUID = -8679782892561683074L;
	
	private String _requestUrl;
	public String GetRequestUrl() { return _requestUrl; }
	public void SetRequestUrl(String value) { _requestUrl = value; }
	
	public  QueryStringManager(String requestUrl)
	{
		_requestUrl = requestUrl;
	}
	
	public String Render() throws Exception
	{
		StringBuilder queryString = new StringBuilder();
		
		if (_requestUrl != null)
			queryString.append(_requestUrl).append('?');
		
		for (String key : this.keySet())
			queryString.append(URLEncoder.encode(key, "US-ASCII")).append('=').append(URLEncoder.encode(get(key), "US-ASCII")).append('&');
		
		return queryString.substring(0, queryString.length() - 1);
	}
	
	public QueryStringManager Copy()
	{
		QueryStringManager copy = (QueryStringManager)clone();
		copy.putAll(this);
		
		return copy;
	}
}
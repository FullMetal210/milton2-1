package io.milton.dns;
/**
 * Factory for zones
 * @author Nick
 *
 */
public interface ZoneFactory {

	/**
	 * Return a zone file that can answer queries about the given domain or its ancestors, 
	 * or null if there is no such zone. If there are multiple such zone files, return the 
	 * closest one. E.g. if there are zone files  "bar.com" and "foo.bar.com", a query for 
	 * "my.foo.bar.com" should return the zone rooted at "foo.bar.com".
	 * 
	 * @param domain a domain name
	 * @return
	 */
	Zone findBestZone(String domain);
	
}

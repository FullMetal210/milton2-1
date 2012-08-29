package io.milton.dns;
/**
 * Factory for Zones
 * 
 * @author Nick
 */
public interface ZoneFactory {

	/**
	 * Return the zone of authority to which the requested domain belongs. If the domain
	 * isn't a member of any zones, then the closest zone should be returned (ie one with 
	 * information on the domain's ancestors. If the domain doesn't fall under any zones,
	 * return null.
	 * 
	 * @param domain
	 * @return
	 */
	Zone findBestZone(String domain);
}

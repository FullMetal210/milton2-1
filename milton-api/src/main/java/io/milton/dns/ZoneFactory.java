package io.milton.dns;
/**
 * Factory for Zones
 * 
 * @author Nick
 */
public interface ZoneFactory {

	/**
	 * Return the zone of authority that most closely contains the  given domain, or null if 
	 * the domain isn't a descendant of any zones.
	 * 
	 * @param domain
	 * @return
	 */
	Zone findBestZone(String domain);
}

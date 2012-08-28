package io.milton.dns;

import io.milton.dns.record.ResourceRecord;

import java.util.Iterator;
import java.util.List;

/**
 * A zone of authority, which holds the records for all of its domains
 * 
 * @author Nick
 */
public interface Zone extends Iterable<String>{
	
	/**
	 * The domain at the root of this zone
	 * @return a domain name
	 */
	String getRootDomain();
	
	/**
	 * Information about this zone
	 * @return
	 */
	ZoneInfo getInfo();
	
	/**
	 * Return an iterator over all the domains contained in this zone's tree,
	 * including delegation points (ie leaves).  Used for outward AXFR transfers.
	 * If there is any non-trivial work involved in enumerating the domains, a lazy
	 * approach should be taken, since most of the time the iterator won't be needed.
	 * 
	 * If AXFR ability isn't needed, just return null.
	 */
	Iterator<String> iterator();
	
	/**
	 * Return the resource records associated with the given domain. The method should return
	 * only the records for the particular node requested, rather than for the whole subtree.
	 * @param domain
	 * @return
	 */
	List<ResourceRecord> getDomainRecords(String domain);

}

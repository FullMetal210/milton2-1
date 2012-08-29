package io.milton.dns;

import io.milton.dns.record.ResourceRecord;

import java.util.List;

/**
 * A dns domain
 * 
 * @author Nick
 */
public interface Domain {

	/**
	 * The zone of authority to which this domain belongs
	 * @return
	 */
	Zone getZone();
	
	/**
	 * The resource records associated with the given domain. The method should return
	 * only the records for the particular node requested, rather than for the whole subtree.
	 * @param domain a queried domain name
	 * @return a list of resource records for domain
	 */
	List<ResourceRecord> getRecords();
}

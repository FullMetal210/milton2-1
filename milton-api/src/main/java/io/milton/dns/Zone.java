package io.milton.dns;

import io.milton.dns.record.ResourceRecord;

import java.util.Iterator;
import java.util.List;

/**
 * A zone of authority
 * 
 * @author Nick
 */
public interface Zone extends Iterable<String>{
	
	/**
	 * The domain at the root of this zone.
	 * @return
	 */
	String getRootDomain();
	
	/**
	 * The resource records associated with the given domain. The method should return
	 * only the records for the particular node requested, rather than for the whole subtree.
	 * @param domain a queried domain name
	 * @return a list of resource records for domain
	 */
	List<ResourceRecord> getRecords(String domain);
	
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
	 * The authoritative nameservers for this zone.
	 * 
	 * @return a list of nameserver domain names
	 */
	List<String> getNameservers();
	
	 /**
     * The "primary master" among the nameservers listed in getNameservers();
     * 
     * @return a domain name
     */
    String getPrimaryMaster();

    /**
     * Email address of the person responsible for administering the primary master's zone files.
     * 
     * @return an email address
     */
    String getAdminEmail();

    /**
     * Counter which should be incremented when the zone file changes
     * 
     * The revision number of this zone file. Increment this number each time the zone file 
     * is changed. It is important to increment this value each time a change is made, so 
     * that the changes will be distributed to any secondary DNS servers. 
     * 
     * @return 
     */
    long getZoneSerialNumber();
    
    /** The time, in seconds, a secondary DNS server waits before querying the primary 
     * DNS server's SOA record to check for changes. When the refresh time expires, 
     * the secondary DNS server requests a copy of the current SOA record from the 
     * primary. The primary DNS server complies with this request. The secondary DNS 
     * server compares the serial number of the primary DNS server's current SOA record 
     * and the serial number in it's own SOA record. If they are different, the secondary 
     * DNS server will request a zone transfer from the primary DNS server. 
     * 
     * A typical value is 3,600. 
     * 
     * @return 
     */
    long getRefresh();
    
    /**
     * The time, in seconds, a secondary server waits before retrying a failed zone 
     * transfer. Normally, the retry time is less than the refresh time. 
     * A typical value is 600. 
     * 
     * @return 
     */
    long getRetry();
    
    /**
     * The time, in seconds, that a secondary server will keep trying to complete a 
     * zone transfer. If this time expires prior to a successful zone transfer, the 
     * secondary server will expire its zone file. This means the secondary will stop 
     * answering queries, as it considers its data too old to be reliable. 
     * 
     * A typical value is 86,400. 
     * 
     * @return 
     */
    long getExpire();
    
    /**
     * The minimum time-to-live value applies to all resource records in the zone file. 
     * This value is supplied in query responses to inform other servers how long they 
     * should keep the data in cache. 
     * 
     * This field has been redefined to mean the maximum amount of time a negative response
     * to a query should be cached
     * 
     * A typical value is 3,600. 
     * 
     * @return 
     */
    long getMinimum();
    
    /**
     * How long data about the zone will be cached. 
     * 
     * A typical value is 7200
     * 
     * @return
     */
    int getTtl();
}

package io.milton.dns.record;

/**
 * A domain with a canonical name record (CNAME) should not have any other 
 * records.
 * 
 * @author Nick
 *
 */
public interface CanonicalNameRecord extends ResourceRecord{

	/**
	 * The domain this one is an alias for
	 * @return
	 */
	String getCanonicalName();
}

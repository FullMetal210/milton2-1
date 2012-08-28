package io.milton.dns.record;

import java.util.List;
/**
 * A resource record that delegates its owner to a different nameserver.
 * 
 * There are a couple of restrictions on the use of delegation records:
 * (1) A domain with delegation records should not contain any other types of records.
 * By definition, authority for the domain has been delegated to other nameservers,
 * so our (authoritative-only) server doesn't maintain information about it.
 * (2) A domain that is the root of its zone can't have a delegation record.
 * A zone-root domain is one that has been delegated to us, and domains can't be
 * re-delegated.
 * @author Nick
 *
 */
public interface DelegationRecord extends ResourceRecord {

	/**
	 * A nameserver with authority over the owner domain
	 * @return
	 */
	String getNameserver();
}

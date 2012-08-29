package io.milton.dns;

/**
 * Factory for domains
 * 
 * @author Nick
 */
public interface DomainFactory {

	/**
	 * A Domain object containing information about the given domain name, or null if 
	 * the domain doesn't exist. 
	 * 
	 * Optionally, a ForeignDomainException can be thrown to signify that the factory doesn't 
	 * have any knowledge of the requested domain or any of its ancestors.
	 * 
	 * @param domainName a domain name without the terminating dot
	 * @return
	 */
	Domain getDomain(String domainName) throws ForeignDomainException;
}

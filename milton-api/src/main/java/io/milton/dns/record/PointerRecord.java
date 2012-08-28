package io.milton.dns.record;

/**
 * A PTR record, generally used for reverse lookup
 * 
 * @author Nick
 */
public interface PointerRecord extends ResourceRecord{

	String getTargetDomain();
}

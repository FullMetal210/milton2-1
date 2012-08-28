package io.milton.dns.record;

/**
 * A domain can zero or one sender policy records, but not more than one.
 * @author Nick
 *
 */
public interface SenderPolicyRecord extends ResourceRecord{

	String getSenderPolicy();
}

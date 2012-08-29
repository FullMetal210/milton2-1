package io.milton.dns;

public class ForeignDomainException extends Exception{

	String domainName;
	
	public ForeignDomainException(String domainName) {
		super("Foreign domain: " + domainName);
		this.domainName = domainName;
	}
	
	public String getDomainName() {
		return domainName;
	}
}

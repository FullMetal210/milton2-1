package io.milton.dns;

/**
 * 
 * @author Administrator
 */
class QueryResult {

	enum Status {
		UNKNOWN,
		NXDOMAIN,
		NXRRSET,
		SUCCESSFUL,
		CNAME,
		DNAME,
		DELEGATION
	}

	static QueryResult nxDomain() {
		QueryResult sr = new QueryResult();
		sr.setStatus(Status.NXDOMAIN);
		return sr;
	}
	
	private Status type;
	private DomainResource domainResource;
	private QueryResult(){
		
	}
	
	QueryResult( Status type, DomainResource domainResource ){
		if ( type == null || domainResource == null) {
			throw new RuntimeException("ResponseSet, null arg");
		}
		this.type = type;
		this.domainResource = domainResource;
	}
	
	Status getStatus(){
		return type;
	}
	
	/**
	 * The relevant domain that was found, or the closest existing
	 * ancestor domain in the case of NXDOMAIN
	 * @param domainResource
	 */
	DomainResource getDomainResource() {
		return domainResource;
	}
	
	void setStatus (Status type) {
		if ( type == null ) {
			throw new RuntimeException("Null type");
		}
		this.type = type;
	}

	void setDomainResource(DomainResource domainResource) {
		if ( domainResource == null ) {
			throw new RuntimeException("Null domainResource");
		}
		this.domainResource = domainResource;
	}
}

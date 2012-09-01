package io.milton.dns.filter;

import io.milton.dns.record.ResourceRecord;

public interface Response extends Message{

	void setId(int id);
	
	void setRcode(Rcode rcode);
	
	void setFlag(Flag flag, boolean on);
	
	void addRecord(ResourceRecord rr, String owner, Section section);
	
}

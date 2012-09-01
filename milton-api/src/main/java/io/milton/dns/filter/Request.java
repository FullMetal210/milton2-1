package io.milton.dns.filter;

import java.net.InetAddress;

public interface Request extends Message{

	int getId();
	
	Opcode getOpcode();
	
	Rcode getRcode();
	
	String getQueryDomain();
	
	QType getQueryType();
	
	QClass getQueryClass();
	
	boolean isSet(Flag flag);

	InetAddress getRemoteAddr();
}

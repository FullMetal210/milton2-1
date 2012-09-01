package io.milton.dns;

import java.io.IOException;

import io.milton.dns.filter.Filter;
import io.milton.dns.filter.FilterChain;
import io.milton.dns.filter.Request;
import io.milton.dns.filter.Response;

public class StandardFilter implements Filter  {

	NameServer nameserver;
	
	public StandardFilter(NameServer nameserver) {
		this.nameserver = nameserver;
	}
	@Override
	public void process(FilterChain chain, Request request, Response response) {
		try {
			nameserver.process();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		
	}

}

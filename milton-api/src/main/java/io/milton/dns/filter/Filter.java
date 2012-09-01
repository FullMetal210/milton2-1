package io.milton.dns.filter;

public interface Filter {

	public void process(FilterChain chain, Request request, Response response);
}

package io.milton.dns.filter;

import java.util.List;

public class FilterChain {

	int curr;
	List<Filter> filters;
	
	public FilterChain(List<Filter> filters) {
		this.filters = filters;
	}
	
	public void process(Request request, Response response) {
		filters.get(curr++).process(this, request, response);
	}
}

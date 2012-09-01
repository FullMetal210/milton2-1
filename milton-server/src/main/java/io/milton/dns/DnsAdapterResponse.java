package io.milton.dns;

import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;

import io.milton.dns.filter.Response;
import io.milton.dns.record.ResourceRecord;

public class DnsAdapterResponse implements Response {

	private static RecordTypes recordTypes = new RecordTypes();
	private Message respMessage;
	
	public DnsAdapterResponse() {
		this.respMessage = new Message();
	}
	
	public DnsAdapterResponse(Message respMessage) {
		this.respMessage = respMessage;
	}
	
	@Override
	public void setId(int id) {
		respMessage.getHeader().setID(id);
	}

	@Override
	public void setRcode(Rcode rcode) {
		respMessage.getHeader().setRcode(rcode.code());
	}

	@Override
	public void setFlag(Flag flag, boolean on) {
		int bit = flag.code();
		if (on) {
			respMessage.getHeader().setFlag(bit);
		} else {
			respMessage.getHeader().unsetFlag(bit);
		}
	}

	@Override
	public void addRecord(ResourceRecord record, String owner, Section section) {
		try {
			Name domainName = Utils.stringToName(owner);
			Record rr = recordTypes.map(domainName, record);
			int sec = section.code();
			if ( rr != null ) {
				respMessage.addRecord(rr, sec);
			}
		} catch (TextParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Message getMessage() {
		return respMessage;
	}
	
	public void setMessage(Message respMessage) {
		this.respMessage = respMessage;
	}
	
}

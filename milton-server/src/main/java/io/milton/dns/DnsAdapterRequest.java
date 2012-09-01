package io.milton.dns;

import io.milton.dns.filter.Request;
import io.milton.dns.filter.Message.QClass;
import io.milton.dns.filter.Message.Opcode;
import io.milton.dns.filter.Message.Rcode;
import io.milton.dns.filter.Message.QType;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.xbill.DNS.Message;
import org.xbill.DNS.Name;

public class DnsAdapterRequest implements Request{

	private Message reqMessage;
	private InetAddress remoteAddr;
	private Socket socket;
	
	public DnsAdapterRequest(byte[] in) throws IOException {
		this.reqMessage = new Message(in);
	}
	
	public DnsAdapterRequest(Message reqMessage, InetAddress remoteAddr, Socket socket) {
		this.reqMessage = reqMessage;
		this.remoteAddr = remoteAddr;
		this.socket = socket;
	}

	@Override
	public int getId() {
		return reqMessage.getHeader().getID();
	}
	
	@Override
	public Opcode getOpcode() {
		int code = reqMessage.getHeader().getOpcode();
		return enumFromCode(Opcode.class, code);
	}

	@Override
	public Rcode getRcode() {
		int code = reqMessage.getRcode();
		return enumFromCode(Rcode.class, code);
	}
	
	@Override
	public String getQueryDomain() {
		Name domainName = reqMessage.getQuestion().getName();
		String domainString = Utils.nameToString(domainName);
		return domainString;
	}

	@Override
	public QType getQueryType() {
		int code = reqMessage.getQuestion().getType();
		return enumFromCode(QType.class,code);
	}

	@Override
	public QClass getQueryClass() {
		int code = reqMessage.getQuestion().getDClass();
		return enumFromCode(QClass.class, code);
	}

	@Override
	public boolean isSet(Flag flag) {
		return reqMessage.getHeader().getFlag(flag.code());
	}

	@Override
	public InetAddress getRemoteAddr() {
		return remoteAddr;
	}

	public Message getMessage() {
		return reqMessage;
	}
	
	public void setMessage(Message reqMessage) {
		this.reqMessage = reqMessage;
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public static <T extends Enum<T>> T enumFromCode(Class<T> enumClass, int code) {
		if (!Encodable.class.isAssignableFrom(enumClass)) {
			return null;
		}
		T[] values = enumClass.getEnumConstants();
		for ( T value : values ) {
			Encodable e = (Encodable) value;
			if ( e.code() == code ) {
				return value;
			}
		}
		return null;
	}
	
	public static void main(String[] args) {
		System.out.println(enumFromCode(QType.class, 255));
	}
}

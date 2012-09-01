package io.milton.dns.filter;

import io.milton.dns.record.ResourceRecord;

import java.net.InetAddress;

public interface Message {

	interface Encodable {
		int code();
	}
	
	enum Section implements Encodable{
		
		QUESTIONS(0),
		ANSWERS(1),
		AUTHORITY(2),
		ADDITIONAL(3);
		
		private int position;
		Section(int position) {
			this.position = position;
		}
		public int code() {
			return position;
		}
	}
	
	enum QType implements Encodable{
		
		SOA(6),
		NS(2),
		A(1),
		AAAA(28),
		MX(15),
		PTR(12),
		SPF(99),
		TXT(16),
		CNAME(5),
		DNAME(39),
		AXFR(252),
		ANY(255);
		
		private int code;
		QType(int code) {
			this.code= code;
		}
		public int code() {
			return code;
		}
	}
	
	enum QClass implements Encodable{
		
		IN(1, "Internet"),
		CH(2, "CHAOS"),
		HS(4, "HESIOD"),
		ANY(255, "*");
		
		private int code;
		private String name;
		QClass(int code, String name) {
			this.code = code;
			this.name = name;
		}
		
		public int code(){
			return code;
		}
		public String desc() {
			return name;
		}
	}
	
	enum Rcode implements Encodable{
		
		NOERROR(0),
		FORMERR(1),
		SERVFAIL(2),
		NXDOMAIN(3),
		NOTIMPL(4),
		REFUSED(5);
		
		private int code;
		Rcode(int code) {
			this.code = code;
		}	
		public int code() {
			return code;
		}
	}

	enum Opcode implements Encodable{
		
		QUERY(0),
		IQUERY(1),
		STATUS(2);
		
		private int code;
		Opcode(int code) {
			this.code = code;
		}		
		public int code() {
			return code;
		}
	}
	
	enum Flag implements Encodable{
		
		QR( 0, "Query/Response" ),
		AA( 5, "Authoritative Answer" ),
		TC( 6, "TrunCation" ),
		RD( 7, "Recursion Desired" ),
		RA( 8, "Recursion Available" ),
		AD( 10, "Authenticated Data"),
		CD( 11, "Checking Disabled");
	
		private int bit;
		private String name;
		Flag(int bit, String name) {
			this.bit = bit;
			this.name = name;
		}
		public int code() {
			return bit;
		}
		public String desc() {
			return name;
		}
	}
}



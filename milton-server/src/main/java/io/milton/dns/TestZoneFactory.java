package io.milton.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;

import io.milton.common.Service;
import io.milton.dns.record.AddressRecord;
import io.milton.dns.record.CanonicalNameRecord;
import io.milton.dns.record.DelegationRecord;
import io.milton.dns.record.MailExchangeRecord;
import io.milton.dns.record.ResourceRecord;

public class TestZoneFactory implements ZoneFactory {

	String zoneRoot = "blah.com";
	Map<String, List<ResourceRecord>> map = new LinkedHashMap<String, List<ResourceRecord>>();

	public TestZoneFactory() throws UnknownHostException {

		List<ResourceRecord> records = new LinkedList<ResourceRecord>();
		records.add(new TestARecord(InetAddress.getByName("123.4.5.67")));
		records.add(new TestMxRecord("mx1.gmail.com", 10));
		records.add(new TestMxRecord("mx2.gmail.com", 20));
		map.put("blah.com", records);

		ResourceRecord rr = new TestDelegationRecord("nameserver.something.com");
		map.put("delegated.blah.com", Collections.singletonList(rr));

		rr = new TestCanonicalNameRecord("target.blah.com");
		map.put("alias.blah.com", Collections.singletonList(rr));

		rr = new TestCanonicalNameRecord("target.otherdomain.com");
		map.put("alias2.blah.com", Collections.singletonList(rr));


	}

	@Override
	public Zone findBestZone(String domain) {
		domain = domain.toLowerCase();
		if ( !domain.endsWith(zoneRoot) ) {
			return null;
		}
		ZoneInfo info = new TestZoneInfo(zoneRoot);
		Zone zone = new TestZone(zoneRoot, info);
		return zone;
	}

	public static void main(String[] args) throws UnknownHostException {
		BasicConfigurator.configure();
		Service service = new NameServer(new TestZoneFactory());
		service.start();
	}
	
	class TestZone implements Zone {

		String root;
		ZoneInfo info;
		
		TestZone(String root, ZoneInfo info) {
			this.root = root;
			this.info = info;
		}
		@Override
		public String getRootDomain() {
			return root;
		}

		@Override
		public ZoneInfo getInfo() {
			return info;
		}

		@Override
		public Iterator<String> iterator() {
			return map.keySet().iterator();
		}

		@Override
		public List<ResourceRecord> getDomainRecords(String domain) {
			return map.get(domain.toLowerCase());
		}
		
	}
	
	class TestZoneInfo implements ZoneInfo {

		String root;
		
		TestZoneInfo(String root) {
			this.root = root;
		}
		
		@Override
		public List<String> getNameservers() {
			List<String> nsList = new LinkedList<String>();
			nsList.add("ns1." + root);
			nsList.add("ns2." + root);
			return nsList;
		}

		@Override
		public String getPrimaryMaster() {
			return "ns1." + root;
		}

		@Override
		public String getAdminEmail() {
			return "admin@" + root;
		}

		@Override
		public long getZoneSerialNumber() {
			return 1;
		}

		@Override
		public long getRefresh() {
			return 0;
		}

		@Override
		public long getRetry() {
			return 0;
		}

		@Override
		public long getExpire() {
			return 0;
		}

		@Override
		public long getMinimum() {
			return 0;
		}

		@Override
		public int getTtl() {
			return 0;
		}
		
	}


	class TestARecord implements AddressRecord {

		InetAddress addr;

		TestARecord(InetAddress addr) {
			this.addr = addr;
		}

		@Override
		public int getTtl() {
			return 3600;
		}

		@Override
		public InetAddress getAddress() {
			return addr;
		}

	}

	class TestMxRecord implements MailExchangeRecord {

		String mailserver;
		int priority;

		TestMxRecord(String mailserver, int priority) {
			this.mailserver = mailserver;
			this.priority = priority;
		}

		@Override
		public int getTtl() {
			return 3600;
		}

		@Override
		public int getPriority() {
			return priority;
		}

		@Override
		public String getMailserver() {
			return mailserver;
		}
	}
	
	class TestCanonicalNameRecord implements CanonicalNameRecord {

		String target;
		
		TestCanonicalNameRecord(String target) {
			this.target = target;
		}
		
		@Override
		public int getTtl() {
			return 3600;
		}

		@Override
		public String getCanonicalName() {
			return target;
		}	
		
	}
	
	class TestDelegationRecord implements DelegationRecord {

		String nameserver;
		
		TestDelegationRecord(String nameserver) {
			this.nameserver = nameserver;
		}
		@Override
		public int getTtl() {
			return 7200;
		}

		@Override
		public String getNameserver() {
			return nameserver;
		}
		
	}


}
package io.milton.dns;

import io.milton.dns.record.ResourceRecord;

import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.DClass;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Record;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/**
 * 
 * @author Nick
 *
 */
class DomainResource {

	private static Logger logger = LoggerFactory.getLogger(DomainResource.class);
	private static RecordTypes recordTypes = new RecordTypes();
	
	/**
	 * A DomainResource containing just the records pertaining to the zone
	 * (NS and SOA)
	 */
	static DomainResource fromZone(Zone zone) throws TextParseException {
		if (zone == null) {
			return null;
		}
		DomainResource dr = new DomainResource();
		dr.zone = zone;
		dr.domainName = Utils.stringToName(zone.getRootDomain());
		dr.addZoneRecords(zone.getInfo());
		return dr;
	}
	
	static DomainResource fromDomain(Zone zone, Name domainName, List<ResourceRecord> recordList) throws TextParseException {
		if (zone == null || domainName == null || recordList == null) {
			return null;
		}
		
		DomainResource dr = new DomainResource();
		dr.zone = zone;
		dr.domainName = domainName;
		String domainLower = Utils.nameToString(domainName).toLowerCase();
		String zoneRootLower = zone.getRootDomain().toLowerCase();
		if (!domainLower.endsWith(zoneRootLower)) {
			throw new RuntimeException("Zone.getRootDomain() = " + zone.getRootDomain() + 
					", but must be an ancestor of domain = " + domainLower);
		}
		try {
			Utils.stringToName(zoneRootLower);
		} catch (TextParseException e) {
			throw e;
		}	
		if (zoneRootLower.equals(domainLower)) {
			dr.addZoneRecords(zone.getInfo());
		}			
		dr.addOrdinaryRecords(recordList);
		return dr;
	}
	
	private Name domainName;
	private Zone zone;
	private List<RRset> allRRsets = new LinkedList<RRset>();
	
	private DomainResource() {
		
	}
	
	Name getName() {
		return domainName;
	}
	
	Zone getZone() {
		return zone;
	}
	
	List<RRset> getAllRRsets() {
		return allRRsets;
	}
	
	RRset getRRset( int recType ) {
		for ( RRset rrset: allRRsets ) {
			if ( rrset.getType() == recType ) {
				return rrset;
			}
		}
		return null;
	}
	
	boolean isZone() {
		if ( zone != null ) {
			String name = Utils.nameToString(domainName);
			String zoneRoot = zone.getRootDomain();
			return name.equalsIgnoreCase(zoneRoot);
		}
		return false;
	}
	
	boolean isDelegation() {
		if ( !this.isZone() && this.getRRset(Type.NS) != null ) {
			return true;
		}
		return false;
	}
	
	private void addRecord(Record newRec) {
		boolean added = false;
		for ( RRset listedRRset : allRRsets ) {
			if ( listedRRset.getType() == newRec.getType() ) {
				listedRRset.addRR(newRec);
				added = true;
				break;
			}
		}
		if ( !added ) {
			RRset newRRset = new RRset(newRec);
			allRRsets.add(newRRset);
		}
	}
	
	private void addOrdinaryRecords( List<ResourceRecord> recordList ) throws TextParseException {
		for ( ResourceRecord rr : recordList ) {
			Record rec = recordTypes.map(domainName, rr );
			if ( rec !=null) {
				addRecord(rec);
			} else {
				logger.warn("Can't map ResourceRecord " + rr.getClass().getName() + " to Record");
			}
		}
	}
	
	private void addZoneRecords( ZoneInfo zoneInfo ) throws TextParseException {
		Name hostName = Utils.stringToName( zoneInfo.getPrimaryMaster() );
		String emailStr = zoneInfo.getAdminEmail();
		if ( emailStr.contains("@")) {
			emailStr = emailStr.replace('@', '.');
		}
		Name emailName = Utils.stringToName(emailStr);
		SOARecord soarr = new SOARecord( domainName, DClass.IN, zoneInfo.getTtl(), 
				hostName, emailName, zoneInfo.getZoneSerialNumber(), zoneInfo.getRefresh(), 
				zoneInfo.getRetry(), zoneInfo.getExpire(), zoneInfo.getMinimum());
		addRecord(soarr);
		
		if ( zoneInfo.getNameservers() == null || zoneInfo.getNameservers().isEmpty() ) {
			throw new RuntimeException("Zone nameserver list is null/empty");
		}
		for ( String nsStr: zoneInfo.getNameservers() ) {
			Name nsName =  Utils.stringToName(nsStr);
			NSRecord nsRec = new NSRecord(domainName, DClass.IN, zoneInfo.getTtl(), nsName);
			addRecord(nsRec);
		}
	}
	
}

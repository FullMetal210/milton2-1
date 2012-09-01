package io.milton.dns;

import io.milton.dns.record.ResourceRecord;

import java.util.Collections;
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
 * Bridges the API and internals
 * @author Nick
 *
 */
class DomainResource {

	private static Logger logger = LoggerFactory.getLogger(DomainResource.class);
	private static RecordTypes recordTypes = new RecordTypes();
	
	/**
	 * A DomainResource containing just the records pertaining to the zone
	 * (NS and SOA)
	 * @param zone
	 * @return
	 * @throws TextParseException
	 */
	static DomainResource fromZone(Zone zone) throws TextParseException {
		if (zone == null) {
			return null;
		}
		DomainResource dr = new DomainResource();
		dr.zone = zone;
		dr.domainName = Utils.stringToName(zone.getRootDomain());
		dr.addZoneRecords(zone);
		return dr;
	}
	
	static DomainResource lookupDomain(Zone zone, Name domainName) throws TextParseException{
		
		if ( zone == null || domainName == null ) {
			return null;
		}
		
		String domainLower = Utils.nameToString(domainName).toLowerCase();
		String zoneRootLower = zone.getRootDomain().toLowerCase();
		logger.info("Fetching records for: " + domainLower);
		List<ResourceRecord> recordList = zone.getRecords(domainLower);
		if (recordList == null) {
			return null;
		}

		if (!domainLower.endsWith(zoneRootLower)) {
			throw new RuntimeException("Zone.getRootDomain() = " + zone.getRootDomain() + 
					", but must be an ancestor of domain = " + domainLower);
		}
		try {
			Utils.stringToName(zoneRootLower);
		} catch (TextParseException e) {
			throw e;
		}
		
		DomainResource dr = new DomainResource();
		dr.zone = zone;
		dr.domainName = domainName;
		dr.addOrdinaryRecords(recordList);
		if (zoneRootLower.equals(domainLower)) {
			dr.addZoneRecords(zone);
		}
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
			String nameString = Utils.nameToString(domainName);
			String zoneRootString = zone.getRootDomain();
			return nameString.equalsIgnoreCase(zoneRootString);
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
	
	private void addZoneRecords( Zone zone ) throws TextParseException {
		Name hostName = Utils.stringToName( zone.getPrimaryMaster() );
		String emailStr = zone.getAdminEmail();
		if ( emailStr.contains("@")) {
			emailStr = emailStr.replace('@', '.');
		}
		Name emailName = Utils.stringToName(emailStr);
		SOARecord soarr = new SOARecord( domainName, DClass.IN, zone.getTtl(), 
				hostName, emailName, zone.getZoneSerialNumber(), zone.getRefresh(), 
				zone.getRetry(), zone.getExpire(), zone.getMinimum());
		addRecord(soarr);
		
		if ( zone.getNameservers() == null || zone.getNameservers().isEmpty() ) {
			throw new RuntimeException("Zone nameserver list is null/empty");
		}
		for ( String nsStr: zone.getNameservers() ) {
			Name nsName =  Utils.stringToName(nsStr);
			NSRecord nsRec = new NSRecord(domainName, DClass.IN, zone.getTtl(), nsName);
			addRecord(nsRec);
		}
	}
	
}

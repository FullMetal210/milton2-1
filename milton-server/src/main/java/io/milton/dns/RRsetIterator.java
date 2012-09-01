package io.milton.dns;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;


/**
 * Iterator over all RRsets for all domains in a zone. Used for outward AXFR transfers.
 *
 */
class RRsetIterator implements Iterator<RRset>{
	
	private static final Logger logger = LoggerFactory.getLogger(RRsetIterator.class);
	
	private DomainResource originNode;
	private Iterator<String> domainIter;
	private Zone zone;
	private String zoneRootString;
	private RRset[] current;
	private int count;
	private boolean wantLastSoa = true;
	
	RRsetIterator(Zone zone)  {

		this.zone = zone;
		this.domainIter = zone.iterator();
		if ( domainIter == null  ) {
			throw new RuntimeException("Null Domain iterator");
		}

		this.zoneRootString = zone.getRootDomain().toLowerCase();
		this.originNode = getDomainResource(zoneRootString);
		
		if ( originNode.getRRset(Type.SOA) == null ) {
			throw new RuntimeException("Zone " + zoneRootString + " missing SOA record");
		}
		if ( originNode.getRRset(Type.NS) == null ){
			throw new RuntimeException("Zone " + zoneRootString + " missing NS rrset");
		}
		
		List<RRset> sets = originNode.getAllRRsets();
		this.current = new RRset[sets.size()];
		for ( int j= 2, k = 0; k < sets.size(); k++ ){
			RRset rrset = sets.get(k);
			int type = rrset.getType();
			
			if ( type == Type.SOA ) {
				current[0] = rrset;
			} else if ( type == Type.NS ) {
				current[1] = rrset;
			} else {
				current[j++] = rrset;
			}
		}
	}
	
	@Override
	public boolean hasNext() {
		return (current !=null || wantLastSoa );
	}

	@Override
	public RRset next() {
		if ( !this.hasNext() ) {
			throw new NoSuchElementException();
		}
		if (current == null) {
			wantLastSoa = false;
			return originNode.getRRset(Type.SOA);
		}
		RRset set = current[count++];
		if ( count == current.length ) {
			current = null;
			while ( domainIter.hasNext() ) {
				
				String domainString = domainIter.next();
				if (domainString.equalsIgnoreCase(zoneRootString)) {
					continue;
				}
				
				DomainResource dr = getDomainResource(domainString);
				if ( dr == null ) {
					continue;
				}
				List<RRset> sets = dr.getAllRRsets();
				if ( sets == null || sets.size() == 0 ) {
					continue;
				}
				current = sets.toArray(new RRset[0]);
				count = 0;
				break;
			}
		}
		return set;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Can't remove RRset");
	}
	
	private DomainResource getDomainResource(String domainString) {
		try {
			Name domainName = Utils.stringToName(domainString);
			DomainResource dr = DomainResource.lookupDomain(zone, domainName);
			return dr;
		} catch (TextParseException e) {
			logger.error("Invalid domain name: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}
}

/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.dns;

import io.milton.dns.record.AddressRecord;
import io.milton.dns.record.CanonicalNameRecord;
import io.milton.dns.record.DelegationRecord;
import io.milton.dns.record.MailExchangeRecord;
import io.milton.dns.record.PointerRecord;
import io.milton.dns.record.ResourceRecord;
import io.milton.dns.record.SenderPolicyRecord;
import java.net.InetAddress;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Address;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.PTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;

/**
 *
 * @author brad
 */
public class RecordTypes {

	private final Map<Class<? extends ResourceRecord>, RecordMapper> mappers;

	public RecordTypes() {
		this.mappers = new ConcurrentHashMap<Class<? extends ResourceRecord>, RecordTypes.RecordMapper>();
		mappers.put(AddressRecord.class, new ARecordMapper());
		mappers.put(MailExchangeRecord.class, new MXRecordMapper());
		mappers.put(DelegationRecord.class, new NSRecordMapper());
		mappers.put(CanonicalNameRecord.class, new CNAMERecordMapper());
		mappers.put(PointerRecord.class, new PtrRecordMapper());
		mappers.put(SenderPolicyRecord.class, new SpfRecordMapper());
	}

	public Record map(Name domainName, ResourceRecord r) throws TextParseException {
		RecordMapper mapper = get(r);
		if (mapper != null) {
			return mapper.map(domainName, r);
		} else {
			return null;
		}
	}

	private RecordMapper get(ResourceRecord r) {
		Class c = r.getClass();
		for( Entry<Class<? extends ResourceRecord>, RecordMapper> entry : mappers.entrySet()) {
			if( entry.getKey().isAssignableFrom(c)) {
				return entry.getValue();
			}
		}
		return null;
	}

	public interface RecordMapper<T extends ResourceRecord> {

		Record map(Name domainName, T r) throws TextParseException;
	}

	public class ARecordMapper implements RecordMapper<AddressRecord> {

		@Override
		public Record map(Name domainName, AddressRecord r) throws TextParseException {
			// Not sure, are we supposed to build the domain name into the record name???
			//Name name = Name.fromString(r.getName(), domainName);
			//Name name = Utils.stringToName(r.getName());
			if( domainName == null ) {
				throw new RuntimeException("resource name is null: " + r.getClass() + " - " + domainName);				 
			}
			Record arr;
			InetAddress add = r.getAddress();
			if (Address.familyOf(add) == Address.IPv4) {
				arr = new ARecord(domainName, DClass.IN, r.getTtl(), add);
			} else if (Address.familyOf(add) == Address.IPv4) {
				arr = new AAAARecord(domainName, DClass.IN, r.getTtl(), add);
			} else {
				throw new RuntimeException("Unknown address type: " + add.getCanonicalHostName());
			}
			return arr;
		}
	}

	public class MXRecordMapper implements RecordMapper<MailExchangeRecord> {

		@Override
		public Record map(Name domainName, MailExchangeRecord r) throws TextParseException {
			//Name thisName = Utils.stringToName(r.getName());
			if( domainName == null ) {
				throw new RuntimeException("resource name is null: " + r.getClass() + " - " + domainName);				 
			}
			Name targetName = Utils.stringToName(r.getMailserver());
			if( targetName == null ) {
				throw new RuntimeException("targetName name is null: " + r.getClass() + " - " + r.getMailserver());
			}			
			MXRecord mxrr = new MXRecord(domainName, DClass.IN, r.getTtl(), r.getPriority(), targetName);
			return mxrr;
		}
	}

	public class NSRecordMapper implements RecordMapper<DelegationRecord> {

		@Override
		public Record map(Name domainName, DelegationRecord r) throws TextParseException {
			//Name thisName = Utils.stringToName(r.getName());
			if( domainName == null ) {
				throw new RuntimeException("resource name is null: " + r.getClass() + " - " + domainName);				 
			}
			Name targetName = Utils.stringToName(r.getNameserver());
			if( targetName == null ) {
				throw new RuntimeException("targetName name is null: " + r.getClass() + " - " + r.getNameserver());
			}	
			NSRecord nsrr = new NSRecord(domainName, DClass.IN, r.getTtl(), targetName);
			return nsrr;
		}
	}
	
	public class CNAMERecordMapper implements RecordMapper<CanonicalNameRecord> {

		@Override
		public Record map(Name domainName, CanonicalNameRecord r) throws TextParseException {
			//Name thisName = Utils.stringToName(r.getName());
			if( domainName == null ) {
				throw new RuntimeException("resource name is null: " + r.getClass() + " - " + domainName);				 
			}
			Name targetName = Utils.stringToName(r.getCanonicalName());
			if( targetName == null ) {
				throw new RuntimeException("targetName name is null: " + r.getClass() + " - " + r.getCanonicalName());
			}	
			CNAMERecord cnamerr = new CNAMERecord(domainName, DClass.IN, r.getTtl(), targetName);
			return cnamerr;
		}
	}
	/*
	public class SOARecordMapper implements RecordMapper<SoaRecord> {

		@Override
		public Record map(Name domainName, SoaRecord r) throws TextParseException {
			Name thisName = Utils.stringToName(r.getName());
			Name hostName = Utils.stringToName(r.getHost());
			String adminEmail = r.getAdminEmail();
			if( adminEmail != null ) {
				if( adminEmail.contains("@")) {
					adminEmail = adminEmail.replace("@", ".");
				}
			}
			Name adminEmailName = Utils.stringToName(adminEmail);
			SOARecord soarr = new SOARecord(thisName, DClass.IN, r.getTtl(),
					hostName, adminEmailName, r.getZoneSerialNumber(),
					r.getRefresh(), r.getRetry(), r.getExpire(),
					r.getMinimum());
			return soarr;
		}
	} */
	
	public class PtrRecordMapper implements RecordMapper<PointerRecord> {

		@Override
		public Record map(Name domainName, PointerRecord r) throws TextParseException {
			if( domainName == null ) {
				throw new RuntimeException("resource name is null: " + r.getClass() + " - " + domainName);				 
			}
			Name targetName = Utils.stringToName(r.getTargetDomain());
			if( targetName == null ) {
				throw new RuntimeException("targetName name is null: " + r.getClass() + " - " + r.getTargetDomain());
			}		
			Record ptrrr = new PTRRecord(domainName, DClass.IN, r.getTtl(), targetName);
			return ptrrr;
		}
	}
	
	public class SpfRecordMapper implements RecordMapper<SenderPolicyRecord> {

		@Override
		public Record map(Name domainName, SenderPolicyRecord r) throws TextParseException {
			if( domainName == null ) {
				throw new RuntimeException("resource name is null: " + r.getClass() + " - " + domainName);				 
			}
			String senderPolicy = r.getSenderPolicy();
			if( senderPolicy == null ) {
				throw new RuntimeException("senderPolicy is null: " + r.getClass() + " - " + r.getSenderPolicy());
			}		
			Record txtrr = new TXTRecord(domainName, DClass.IN, r.getTtl(), senderPolicy);
			return txtrr;
		}
	}
}

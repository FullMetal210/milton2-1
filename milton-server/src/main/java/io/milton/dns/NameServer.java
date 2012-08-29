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
//import jnamed;

import io.milton.common.Service;
import io.milton.dns.QueryResult.Status;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.DNAMERecord;
import org.xbill.DNS.ExtendedFlags;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.NameTooLongException;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Opcode;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.Section;
import org.xbill.DNS.TSIG;
import org.xbill.DNS.TSIGRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

public class NameServer implements Service {

	private static Logger log = LoggerFactory.getLogger(NameServer.class.getName());
	static final int FLAG_DNSSECOK = 1;
	static final int FLAG_SIGONLY = 2;
	
	protected ZoneFactory zoneFactory;
	private List<InetSocketAddress> sockAddrs = new ArrayList<InetSocketAddress>();
	private List<TcpListener> tcpListeners = new ArrayList<TcpListener>();
	private List<UdpListener> udpListeners = new ArrayList<UdpListener>();
	private volatile boolean running;
	private boolean checkWild = true;
	
	public NameServer(ZoneFactory zoneFactory, InetSocketAddress... sockAddrs) {
		
		this.zoneFactory = zoneFactory;
		if (sockAddrs == null || sockAddrs.length == 0 ) {
			this.sockAddrs.add(new InetSocketAddress(53));
		} else {
			this.sockAddrs.addAll(Arrays.asList(sockAddrs));
		}
	}

	@Override
	public void start() {
		log.info("Starting DNS server");
		running = true;
		for (InetSocketAddress sockAddr : sockAddrs) {
			log.info("Listening on interface: " + sockAddr);
			System.out.println(sockAddr);
			TcpListener tl = new TcpListener(sockAddr);
			tcpListeners.add(tl);
			new Thread(tl).start();

			UdpListener ul = new UdpListener(sockAddr);
			udpListeners.add(ul);
			new Thread(ul).start();
		}
	}


	@Override
	public void stop() {
		log.info("Stopping DNS server");
		running = false; 
		for (TcpListener tl : tcpListeners) {
			tl.close();
		}
		for (UdpListener ul : udpListeners) {
			ul.close();
		}
	}

	private void addRRset(Name name, Message response, RRset rrset, int section, int flags) {
		for (int s = 1; s <= section; s++) {
			if (response.findRRset(name, rrset.getType(), s)) {
				return;
			}
		}
		if ((flags & FLAG_SIGONLY) == 0) {
			Iterator it = rrset.rrs();
			while (it.hasNext()) {
				Record r = (Record) it.next();
				if (r.getName().isWild() && !name.isWild()) {
					r = r.withName(name);
				}
				response.addRecord(r, section);
			}
		}
		if ((flags & (FLAG_SIGONLY | FLAG_DNSSECOK)) != 0) {
			Iterator it = rrset.sigs();
			while (it.hasNext()) {
				Record r = (Record) it.next();
				if (r.getName().isWild() && !name.isWild()) {
					r = r.withName(name);
				}
				response.addRecord(r, section);
			}
		}
	}

	private void addSOA(Message response, SOARecord soaRecord) {
		response.addRecord(soaRecord, Section.AUTHORITY);
	}

	private void addNS(Message response, RRset nsRecords, int flags) {
		// RRset nsRecords = zone.getNS();
		addRRset(nsRecords.getName(), response, nsRecords, Section.AUTHORITY,
				flags);
	}

	private void addGlue(Message response, Name name, int flags) {

		Zone zone = getZone(name);
		if (zone == null) {
			return;
		}
		
		DomainResource dr = getDomainResource(zone, name);
		if( dr == null ) {
			return ;
		}

		/* Check for AAAA records too? */
		RRset a = dr.getRRset(Type.A);
		if (a == null) {
			return;
		}
		addRRset(name, response, a, Section.ADDITIONAL, flags);

	}

	private void addAdditional2(Message response, int section, int flags) {
		Record[] records = response.getSectionArray(section);
		for (int i = 0; i < records.length; i++) {
			Record r = records[i];
			Name glueName = r.getAdditionalName();
			if (glueName != null) {
				addGlue(response, glueName, flags);
			}
		}
	}

	private void addAdditional(Message response, int flags) {
		addAdditional2(response, Section.ANSWER, flags);
		addAdditional2(response, Section.AUTHORITY, flags);
	}

	private byte addAnswer(Message response, Name name, int type, int dclass, int iterations, int flags) {
		
		System.out.println("addAnswer: " + name + " type=" + Type.string(type) + " class=" + DClass.string(dclass));
		byte rcode = Rcode.NOERROR;

		if (iterations > 6) {
			log.warn("iterations too high");
			return Rcode.NOERROR;
		}
		
		if (type == Type.SIG || type == Type.RRSIG) {
			type = Type.ANY;
			flags |= FLAG_SIGONLY;
		}

		Zone zone = getZone(name);
		if ( zone == null ) {
			log.info("Zone is null, no authoritative data");
			return Rcode.NXDOMAIN;
		}
		
		QueryResult sr = performQuery(zone, name, type);		
		DomainResource dr = sr.getDomainResource();
		DomainResource zoneDr = null;
		if ( /*dr != null &&*/ iterations == 0 ) {
			try {
				zoneDr = DomainResource.fromZone(zone);
			} catch (TextParseException e) {
				throw new RuntimeException(e);
			}
		}
		
		switch ( sr.getStatus() ) {
		
		case UNKNOWN:
			//addCacheNS(response, getCache(dclass), name);
		case NXDOMAIN:
			log.info("is NX domain");
			response.getHeader().setRcode(Rcode.NXDOMAIN);
			if (iterations == 0 && zoneDr != null) {
				response.getHeader().setFlag(Flags.AA);
				RRset soaRRset = zoneDr.getRRset(Type.SOA);
				if (soaRRset != null) {
					addSOA(response, (SOARecord) soaRRset.first());
				}
			}
			rcode = Rcode.NXDOMAIN;
			break;
		case NXRRSET:
			log.info("isNXRRSET");		
			if (iterations == 0 && zoneDr != null) {
				response.getHeader().setFlag(Flags.AA);
				RRset soaRRset = zoneDr.getRRset(Type.SOA);
				if (soaRRset != null) {
					addSOA(response, (SOARecord) soaRRset.first());
				}
			}
			break;
		case DELEGATION:
			log.info("delegation");
			RRset nsRecords = dr.getRRset(Type.NS);
			addNS(response, nsRecords, flags);
			break;
		case CNAME:
			log.info("isCNAME");
			RRset cnameRRset = dr.getRRset(Type.CNAME);
			addRRset(name, response, cnameRRset, Section.ANSWER, flags);
			if (zoneDr!= null && iterations == 0) {
				response.getHeader().setFlag(Flags.AA);
			}
			CNAMERecord cname = (CNAMERecord) cnameRRset.first();
			rcode = addAnswer(response, cname.getTarget(), type, dclass,
					iterations + 1, flags);
			break;	
		case DNAME:
			log.info("isDNAME");
			RRset dnameRRset = dr.getRRset(Type.DNAME);
			addRRset(name, response, dnameRRset, Section.ANSWER, flags);
			
			DNAMERecord dname = (DNAMERecord) dnameRRset.first();
			Name newname;
			try {
				newname = name.fromDNAME(dname);
			} catch (NameTooLongException e) {
				return Rcode.YXDOMAIN;
			}
			
			RRset synthCnameRRset = new RRset(new CNAMERecord(name, dclass, 0, newname));
			addRRset(name, response, synthCnameRRset, Section.ANSWER, flags);
			if (zoneDr != null && iterations == 0) {
				response.getHeader().setFlag(Flags.AA);
			}
			rcode = addAnswer(response, newname, type, dclass, iterations + 1,
					flags);
			break;
		case SUCCESSFUL:
			log.info("isSUCCESSFUL");
			if ( type == Type.ANY ) {
				RRset[] rrsets = dr.getAllRRsets().toArray(new RRset[0]);
				for (RRset set : rrsets) {
					addRRset(name, response, set, Section.ANSWER, flags);
				}
			} else {
				RRset set = dr.getRRset(type);
				addRRset(name, response, set, Section.ANSWER, flags);
			}
			if (iterations == 0 && zoneDr != null) {
				response.getHeader().setFlag(Flags.AA);
				RRset nsRRset = zoneDr.getRRset(Type.NS);
				if ( nsRRset != null ) {
					addNS(response, nsRRset, flags);
				}
			}
			break;
		}

		log.info("Rcode = " + Rcode.string(rcode));
		return rcode;
	}
	
	
	private QueryResult performQuery(Zone zone, Name name, int type) {

		DomainResource dr = getDomainResource(zone, name);
		
		if (dr != null) {
			
			/* If this is an ANY lookup, return everything. */
			if ( type == Type.ANY) {
				return new QueryResult(Status.SUCCESSFUL, dr);
			}
			/* Look for the actual type or CNAME */
			else {
				RRset rrset = dr.getRRset(type);
				if (rrset != null) {
					return new QueryResult(Status.SUCCESSFUL, dr);
				}
				rrset = dr.getRRset(Type.CNAME);
				if (rrset != null) {
					return new QueryResult(Status.CNAME, dr);
				}
			}
			/* If this is a delegation, return that. */
			if ( dr.isDelegation() ) {
				return new QueryResult(Status.DELEGATION, dr);		
			}
			/* We found the name, but not the type. */
			return new QueryResult(Status.NXRRSET, dr);
		}
		
		int rlabels = Name.root.labels(); //should be 1
		int labels = name.labels();
		/*
		 * Check if ancestor node contains a DNAME or delegation
		 */
		for (int tlabels = labels - 1; tlabels >= rlabels; tlabels--) {
			
			Name tName = new Name(name, labels - tlabels);	
			dr = getDomainResource(zone, tName);	
			if (dr == null) {
				continue;
			}
			
			/* If this is a delegation, return that. */
			if ( dr.isDelegation() ) {
				return new QueryResult(Status.DELEGATION, dr);		
			}
			/* Look for a DNAME */
			RRset rrset = dr.getRRset(Type.DNAME);
			if (rrset != null) {
				return new QueryResult(Status.DNAME, dr);
			}
			/*Optimization*/
			break;
		} 
		/* Check for explicit wildcard matches. E.g. if the query was for foo.bar.com,
		 * invoke drf.getDomainResource on *.bar.com., *.com., *.
		 */
		if ( checkWild ) {
			
			for (int i = 0; i < labels - /*olabels*/ 1; i++) {
				Name tname = name.wild(i + 1);
				dr = getDomainResource(zone, tname);		
				if (dr == null) {
					continue;
				}
				//added
				if ( type == Type.ANY) {
					return new QueryResult(Status.SUCCESSFUL, dr);
				}
				//cname?
				RRset rrset = dr.getRRset(type);
				if (rrset != null) {
					return new QueryResult(Status.SUCCESSFUL, dr);
				}
				rrset = dr.getRRset(Type.CNAME);
				if (rrset != null) {
					return new QueryResult(Status.CNAME, dr);
				}
				//added
				return new QueryResult(Status.NXRRSET, dr);
			}
		}

		return QueryResult.nxDomain();
	}
	

	/*
	 * Note: a null return value means that the caller doesn't need to do
	 * anything. Currently this only happens if this is an AXFR request over
	 * TCP.
	 */
	byte[] generateReply(Message query, byte[] in, int length, Socket s)
			throws IOException {
		
		log.info("Received request, generating reply");
		Header header;
		boolean badversion;
		int maxLength;
		int flags = 0;

		header = query.getHeader();
		if (header.getFlag(Flags.QR)) {
			return null;
		}
		if (header.getRcode() != Rcode.NOERROR) {
			return errorMessage(query, Rcode.FORMERR);
		}
		if (header.getOpcode() != Opcode.QUERY) {
			return errorMessage(query, Rcode.NOTIMP);
		}

		Record queryRecord = query.getQuestion();

		TSIGRecord queryTSIG = query.getTSIG();
		TSIG tsig = null;
		if (queryTSIG != null) {
			/*
			 * tsig = (TSIG) TSIGs.get(queryTSIG.getName()); if (tsig == null ||
			 * tsig.verify(query, in, length, null) != Rcode.NOERROR) return
			 * formerrMessage(in);
			 */
			return errorMessage(query,Rcode.NOTIMP);
		}

		OPTRecord queryOPT = query.getOPT();
		if (queryOPT != null && queryOPT.getVersion() > 0) {
			badversion = true;
		}

		if (s != null) {
			maxLength = 65535;
		} else if (queryOPT != null) {
			maxLength = Math.max(queryOPT.getPayloadSize(), 512);
		} else {
			maxLength = 512;
		}

		if (queryOPT != null && (queryOPT.getFlags() & ExtendedFlags.DO) != 0) {
			flags = FLAG_DNSSECOK;
		}

		Message response = new Message(query.getHeader().getID());
		response.getHeader().setFlag(Flags.QR);
		if (query.getHeader().getFlag(Flags.RD)) {
			response.getHeader().setFlag(Flags.RD);
		}
		response.addRecord(queryRecord, Section.QUESTION);

		Name name = queryRecord.getName();
		int type = queryRecord.getType();
		int dclass = queryRecord.getDClass();
		//
		if ( dclass != DClass.IN ) {
			/* Needs consideration */
			log.info("Unsupported DClass: " + DClass.string(dclass) );
			return errorMessage(query, Rcode.SERVFAIL);
		}
		//
		if (type == Type.AXFR && s != null) {
			return doAXFR(name, query, tsig, queryTSIG, s);
		}
		if (!Type.isRR(type) && type != Type.ANY) {
			return errorMessage(query, Rcode.NOTIMP);
		}

		byte rcode = addAnswer(response, name, type, dclass, 0, flags);
		if (rcode != Rcode.NOERROR && rcode != Rcode.NXDOMAIN) {
			return errorMessage(query, rcode);
		}

		addAdditional(response, flags);

		if (queryOPT != null) {
			int optflags = (flags == FLAG_DNSSECOK) ? ExtendedFlags.DO : 0;
			OPTRecord opt = new OPTRecord((short) 4096, rcode, (byte) 0,
					optflags);
			response.addRecord(opt, Section.ADDITIONAL);
		}

		response.setTSIG(tsig, Rcode.NOERROR, queryTSIG);
		return response.toWire(maxLength);
	}

	byte[] buildErrorMessage(Header header, int rcode, Record question) {
		Message response = new Message();
		response.setHeader(header);
		for (int i = 0; i < 4; i++) {
			response.removeAllRecords(i);
		}
		if (rcode == Rcode.SERVFAIL) {
			response.addRecord(question, Section.QUESTION);
		}
		header.setRcode(rcode);
		return response.toWire();
	}

	byte[] doAXFR(Name name, Message query, TSIG tsig, TSIGRecord qtsig,
			Socket s) {
		
		String requestedName = Utils.nameToString(name);
		Zone zone = zoneFactory.findBestZone(requestedName);
		if ( zone == null ) {
			return errorMessage(query, Rcode.REFUSED);
		}
		String zoneRoot = zone.getRootDomain();
		if ( !zoneRoot.equalsIgnoreCase(requestedName) ) {
			return errorMessage(query, Rcode.REFUSED);
		}
		if ( zone.iterator() == null ) {
			return errorMessage(query, Rcode.REFUSED);
		}

		Iterator<RRset> rrsetIter = new RRsetIterator(zone);
		try {
			DataOutputStream dataOut = new DataOutputStream(s.getOutputStream());
			int id = query.getHeader().getID();
			boolean first = true;
			while ( rrsetIter.hasNext() ) {
				RRset rrset = rrsetIter.next();
				Message response = new Message(id);
				Header header = response.getHeader();
				header.setFlag(Flags.QR);
				header.setFlag(Flags.AA);
				addRRset(rrset.getName(), response, rrset, Section.ANSWER, FLAG_DNSSECOK);
				if ( tsig != null ) {
					tsig.applyStream(response, qtsig, first);
					qtsig = response.getTSIG();
				}
				first = false;
				byte[] out = response.toWire();
				dataOut.writeShort(out.length);
				dataOut.write(out);
			}
		} catch (IOException e) {
			log.error("AXFR failed: " + e.getMessage());
		} finally {
			try{
				s.close();
			} catch( IOException e ) {
				//
			}
		}
		return null;
	}

	public byte[] formerrMessage(byte[] in) {
		Header header;
		try {
			header = new Header(in);
		} catch (IOException e) {
			return null;
		}
		return buildErrorMessage(header, Rcode.FORMERR, null);
	}

	public byte[] errorMessage(Message query, int rcode) {
		return buildErrorMessage(query.getHeader(), rcode, query.getQuestion());
	}

	private DomainResource getDomainResource(Zone zone, Name domainName)  {
				
		try {
			DomainResource dr = DomainResource.lookupDomain(zone, domainName);
			return dr;
		} catch (TextParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Zone getZone( Name domainName) {
		
		String domainString = Utils.nameToString(domainName);
		log.info("Finding best Zone for: " + domainString);
		Zone zone = zoneFactory.findBestZone(domainString);
		return zone;
	}


	class TcpListener implements Runnable {

		InetAddress addr;
		int port;
		ServerSocket sock;

		TcpListener(InetSocketAddress sa) {
			this.addr = sa.getAddress();
			this.port = sa.getPort();
		}

		@Override
		public void run() {
			serveTCP(addr, port);
		}

		public void close() {
			if (sock != null) {
				try {
					sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		public void TCPclient(Socket s) {
			try {
				int inLength;
				DataInputStream dataIn;
				DataOutputStream dataOut;
				byte[] in;

				InputStream is = s.getInputStream();
				dataIn = new DataInputStream(is);
				inLength = dataIn.readUnsignedShort();
				in = new byte[inLength];
				dataIn.readFully(in);

				Message query = null;
				byte[] response = null;
				try {
					query = new Message(in);
					response = generateReply(query, in, in.length, s);
					if (response == null) {
						return;
					}
				} catch (IOException e) {
					log.error("exception", e);
					response = formerrMessage(in);
				} catch (RuntimeException e) {
					log.error("exception", e);
					if (query != null) {
						response = errorMessage(query, Rcode.SERVFAIL);
					} else {
						response = formerrMessage(in);
					}

				}
				dataOut = new DataOutputStream(s.getOutputStream());
				dataOut.writeShort(response.length);
				dataOut.write(response);
			} catch (IOException e) {
				System.out.println("TCPclient("
						+ addrport(s.getLocalAddress(), s.getLocalPort()) + "): "
						+ e);
			} finally {
				try {
					s.close();
				} catch (IOException e) {
				}
			}
		}

		public void serveTCP(InetAddress addr, int port) {
			try {
				sock = new ServerSocket(port, 128, addr);
				while (running) {
					final Socket s = sock.accept();
					Thread t;
					t = new Thread(new Runnable() {
						public void run() {
							TCPclient(s);
						}
					});
					t.start();
				}
			} catch (IOException e) {
				System.out.println("serveTCP(" + addrport(addr, port) + "): " + e);
			} finally {
				if (sock != null&& !sock.isClosed() ) {
					try {
						sock.close();
					} catch (IOException e) {
					}
				}
			}
			
		}
	}

	class UdpListener implements Runnable {

		DatagramSocket sock;
		InetAddress addr;
		int port;

		UdpListener(InetSocketAddress sa) {
			this.addr = sa.getAddress();
			this.port = sa.getPort();
		}

		@Override
		public void run() {
			serveUDP(addr, port);
		}

		public void close() {
			if (sock != null) {
				try {
					sock.close();
				} catch (Exception e) {
					//
				}
			}
		}

		public void serveUDP(InetAddress addr, int port) {
			DatagramSocket sock= null;
			try {
				sock = new DatagramSocket(port, addr);
				final short udpLength = 512;
				byte[] in = new byte[udpLength];
				DatagramPacket indp = new DatagramPacket(in, in.length);
				DatagramPacket outdp = null;
				while (running) {
					indp.setLength(in.length);
					try {
						sock.receive(indp);
					} catch (InterruptedIOException e) {
						continue;
					}
					Message query = null;
					byte[] response = null;
					try {
						query = new Message(in);
						response = generateReply(query, in, indp.getLength(), null);
						if (response == null) {
							continue;
						}
					} catch (IOException e) {
						log.error("Exeption generating DNS response", e);
						response = formerrMessage(in);
					} catch (RuntimeException e) {
						log.error("Exeption generating DNS response", e);
						if (query != null) {
							response = errorMessage(query, Rcode.SERVFAIL);
						} else {
							response = formerrMessage(in);
						}
					}
					if (outdp == null) {
						outdp = new DatagramPacket(response, response.length,
								indp.getAddress(), indp.getPort());
					} else {
						outdp.setData(response);
						outdp.setLength(response.length);
						outdp.setAddress(indp.getAddress());
						outdp.setPort(indp.getPort());
					}
					sock.send(outdp);
				}
			} catch (IOException e) {
				System.out.println("serveUDP(" + addrport(addr, port) + "): " + e);
			} finally {
				if ( sock != null && !sock.isClosed() ) {
					sock.close();
				}
			}
		}
	}

	private String addrport(InetAddress addr, int port) {
		return addr.getHostAddress() + "#" + port;
	}
}



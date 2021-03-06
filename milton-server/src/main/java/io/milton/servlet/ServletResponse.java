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

package io.milton.servlet;

import io.milton.http.AbstractResponse;
import io.milton.http.Cookie;
import io.milton.http.Response;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletResponse extends AbstractResponse {

    private static final Logger log = LoggerFactory.getLogger(ServletResponse.class);
    private static ThreadLocal<HttpServletResponse> tlResponse = new ThreadLocal<HttpServletResponse>();

    /**
     * We make this available via a threadlocal so it can be accessed from parts
     * of the application which don't have a reference to the servletresponse
     */
    public static HttpServletResponse getResponse() {
        return tlResponse.get();
    }
    private final HttpServletResponse r;
//    private ByteArrayOutputStream out = new ByteArrayOutputStream();
    private Response.Status status;
    private Map<String, String> headers = new HashMap<String, String>();

    public ServletResponse(HttpServletResponse r) {
        this.r = r;
        tlResponse.set(r);
    }

    /**
     * Override to use servlets own date setting
     *
     * @param name
     * @param date
     */
    @Override
    protected void setAnyDateHeader(Header name, Date date) {
		if( date != null ) {
			r.setDateHeader(name.code, date.getTime());
		} else {
			r.setHeader(name.code, null);
		}
    }

    @Override
    public String getNonStandardHeader(String code) {
        return headers.get(code);
    }

    @Override
    public void setNonStandardHeader(String name, String value) {
        r.addHeader(name, value);
        headers.put(name, value);
    }

    @Override
    public void setStatus(Response.Status status) {
        if (status.text == null) {
            r.setStatus(status.code);
        } else {
            r.setStatus(status.code, status.text);
        }
        this.status = status;
    }

    @Override
    public Response.Status getStatus() {
        return status;
    }

    @Override
    public OutputStream getOutputStream() {
        try {
//        return out;
            return r.getOutputStream();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void close() {
        try {
            r.flushBuffer();
        } catch (Throwable ex) {
            log.trace("exception closing and flushing", ex);
        }
    }

	@Override
	public void sendError(Status status, String message) {
		log.warn("sendError: " + status);
		try {
			r.sendError(status.code, message);
		} catch (IOException ex) {
			log.error("Failed to send error", ex);
		}
	}


	
	

    @Override
    public void sendRedirect(String url) {
        String u = r.encodeRedirectURL(url);
        try {
            r.sendRedirect(u);
        } catch (IOException ex) {
            log.warn("exception sending redirect", ex);
        }
    }

	@Override
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

	@Override
    public void setAuthenticateHeader(List<String> challenges) {
        for (String ch : challenges) {
            r.addHeader(Response.Header.WWW_AUTHENTICATE.code, ch);
        }
    }

	@Override
    public Cookie setCookie(Cookie cookie) {
        if (cookie instanceof ServletCookie) {
            ServletCookie sc = (ServletCookie) cookie;
            r.addCookie(sc.getWrappedCookie());
            return cookie;
        } else {
            javax.servlet.http.Cookie c = new javax.servlet.http.Cookie(cookie.getName(), cookie.getValue());
            c.setDomain(cookie.getDomain());
            c.setMaxAge(cookie.getExpiry());
            c.setPath(cookie.getPath());
            c.setSecure(cookie.getSecure());
            c.setVersion(cookie.getVersion());

            r.addCookie(c);
            return new ServletCookie(c);
        }
    }

    public Cookie setCookie(String name, String value) {
        javax.servlet.http.Cookie c = new javax.servlet.http.Cookie(name, value);
        c.setPath("/");
        r.addCookie(c);
        return new ServletCookie(c);
    }
}

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

package io.milton.event;

import io.milton.http.Request;
import io.milton.http.Response;

/**
 * Fired after response is complete
 *
 * @author brad
 */
public class ResponseEvent implements Event {
    private final Request request;
    private final Response response;
    private final long duration;

    public ResponseEvent(Request request, Response response, long duration) {
        this.request = request;
        this.response = response;
        this.duration = duration;
    }


    public Request getRequest() {
        return request;
    }

    public Response getResponse() {
        return response;
    }        

    public long getDuration() {
        return duration;
    }        
}

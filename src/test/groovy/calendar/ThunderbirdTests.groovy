package calendar

import carldav.service.generator.IdGenerator
import carldav.service.time.TimeService
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.unitedinternet.cosmo.IntegrationTestSupport

import static org.hamcrest.Matchers.is
import static org.mockito.Mockito.when
import static org.springframework.http.HttpHeaders.ALLOW
import static org.springframework.http.MediaType.TEXT_XML
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static testutil.TestUser.USER01
import static testutil.mockmvc.CustomMediaTypes.TEXT_CALENDAR
import static testutil.mockmvc.CustomRequestBuilders.propfind
import static testutil.mockmvc.CustomRequestBuilders.report
import static testutil.mockmvc.CustomResultMatchers.*

/**
 * @author Kamill Sokol
 */
@WithUserDetails(USER01)
class ThunderbirdTests extends IntegrationTestSupport {

    def VEVENT = """\
                        BEGIN:VCALENDAR
                        PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN
                        VERSION:2.0
                        BEGIN:VTIMEZONE
                        TZID:Europe/Stockholm
                        BEGIN:DAYLIGHT
                        TZOFFSETFROM:+0100
                        TZOFFSETTO:+0200
                        TZNAME:CEST
                        DTSTART:19700329T020000
                        RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3
                        END:DAYLIGHT
                        BEGIN:STANDARD
                        TZOFFSETFROM:+0200
                        TZOFFSETTO:+0100
                        TZNAME:CET
                        DTSTART:19701025T030000
                        RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10
                        END:STANDARD
                        END:VTIMEZONE
                        BEGIN:VEVENT
                        CREATED:20151225T180011Z
                        LAST-MODIFIED:20151225T180151Z
                        DTSTAMP:20151225T180151Z
                        UID:0c3112fa-ba2b-4cb4-b495-1b842e3f3b77
                        SUMMARY:VEvent add
                        ORGANIZER;RSVP=TRUE;PARTSTAT=ACCEPTED;ROLE=CHAIR:mailto:kamill@test01@localhost.d
                         e
                        ATTENDEE;RSVP=TRUE;PARTSTAT=NEEDS-ACTION;ROLE=REQ-PARTICIPANT:attendee1
                        RRULE:FREQ=DAILY;UNTIL=20160226T190000Z;INTERVAL=3
                        CATEGORIES:Business
                        DTSTART;TZID=Europe/Stockholm:20151209T200000
                        DTEND;TZID=Europe/Stockholm:20151209T215500
                        TRANSP:OPAQUE
                        LOCATION:location
                        DESCRIPTION:description
                        X-MOZ-SEND-INVITATIONS:TRUE
                        X-MOZ-SEND-INVITATIONS-UNDISCLOSED:FALSE
                        BEGIN:VALARM
                        ACTION:DISPLAY
                        TRIGGER;VALUE=DATE-TIME:20151225T190000Z
                        DESCRIPTION:Default Mozilla Description
                        END:VALARM
                        END:VEVENT
                        END:VCALENDAR
                        """.stripIndent()

    def VTODO = """\
                        BEGIN:VCALENDAR
                        PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN
                        VERSION:2.0
                        BEGIN:VTIMEZONE
                        TZID:Europe/Berlin
                        BEGIN:DAYLIGHT
                        TZOFFSETFROM:+0100
                        TZOFFSETTO:+0200
                        TZNAME:CEST
                        DTSTART:19700329T020000
                        RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3
                        END:DAYLIGHT
                        BEGIN:STANDARD
                        TZOFFSETFROM:+0200
                        TZOFFSETTO:+0100
                        TZNAME:CET
                        DTSTART:19701025T030000
                        RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10
                        END:STANDARD
                        END:VTIMEZONE
                        BEGIN:VTIMEZONE
                        TZID:Europe/Stockholm
                        BEGIN:DAYLIGHT
                        TZOFFSETFROM:+0100
                        TZOFFSETTO:+0200
                        TZNAME:CEST
                        DTSTART:19700329T020000
                        RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3
                        END:DAYLIGHT
                        BEGIN:STANDARD
                        TZOFFSETFROM:+0200
                        TZOFFSETTO:+0100
                        TZNAME:CET
                        DTSTART:19701025T030000
                        RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10
                        END:STANDARD
                        END:VTIMEZONE
                        BEGIN:VTODO
                        CREATED:20151225T184045Z
                        LAST-MODIFIED:20151225T184131Z
                        DTSTAMP:20151225T184131Z
                        UID:00396957-a9f9-482e-8c51-96d20889ab56
                        SUMMARY:add task
                        STATUS:IN-PROCESS
                        RRULE:FREQ=WEEKLY;UNTIL=20151231T222500Z
                        CATEGORIES:Customer
                        DTSTART;TZID=Europe/Berlin:20151201T232500
                        DUE;TZID=Europe/Stockholm:20151224T232500
                        LOCATION:location
                        PERCENT-COMPLETE:25
                        DESCRIPTION:description
                        BEGIN:VALARM
                        ACTION:DISPLAY
                        TRIGGER;VALUE=DURATION:-PT30M
                        DESCRIPTION:Default Mozilla Description
                        END:VALARM
                        END:VTODO
                        END:VCALENDAR
                        """.stripIndent()

    @Autowired
    private TimeService timeService;

    @Autowired
    private IdGenerator idGenerator;

    @Before
    public void before() {
        when(timeService.getCurrentTime()).thenReturn(new Date(3600));
        when(idGenerator.nextStringIdentifier()).thenReturn("1");
    }

    @Test
    public void fetchingEmptyCalendarFirstTime() {
        def request1 = """\
                        <D:propfind xmlns:D="DAV:" xmlns:CS="http://calendarserver.org/ns/" xmlns:C="urn:ietf:params:xml:ns:caldav">
                            <D:prop>
                                <D:resourcetype/>
                                <D:owner/>
                                <D:current-user-principal/>
                                <D:supported-report-set/>
                                <C:supported-calendar-component-set/>
                                <CS:getctag/>
                            </D:prop>
                        </D:propfind>"""

        def response1 = """\
                        <D:multistatus xmlns:D="DAV:">
                            <D:response>
                                <D:href>/dav/test01@localhost.de/calendar/</D:href>
                                <D:propstat>
                                    <D:prop>
                                        <D:current-user-principal/>
                                        <D:owner/>
                                    </D:prop>
                                    <D:status>HTTP/1.1 404 Not Found</D:status>
                                </D:propstat>
                                <D:propstat>
                                    <D:prop>
                                        <D:supported-report-set>
                                            <D:supported-report>
                                                <D:report>
                                                    <C:calendar-multiget xmlns:C="urn:ietf:params:xml:ns:caldav"/>
                                                </D:report>
                                            </D:supported-report>
                                        </D:supported-report-set>
                                        <D:resourcetype>
                                            <C:calendar xmlns:C="urn:ietf:params:xml:ns:caldav"/>
                                            <D:collection/>
                                        </D:resourcetype>
                                        <C:supported-calendar-component-set xmlns:C="urn:ietf:params:xml:ns:caldav">
                                            <C:comp name="VEVENT"/>
                                            <C:comp name="VTODO"/>
                                        </C:supported-calendar-component-set>
                                        <CS:getctag xmlns:CS="http://calendarserver.org/ns/">NVy57RJot0LhdYELkMDJ9gQZjOM=</CS:getctag>
                                    </D:prop>
                                    <D:status>HTTP/1.1 200 OK</D:status>
                                </D:propstat>
                            </D:response>
                        </D:multistatus>"""

        mockMvc.perform(propfind("/dav/{email}/calendar/", USER01)
                .contentType(TEXT_XML)
                .content(request1))
                .andExpect(status().isMultiStatus())
                .andExpect(textXmlContentType())
                .andExpect(xml(response1))

        def request2 = """\
                        <D:propfind xmlns:D="DAV:">
                            <D:prop>
                                <D:getcontenttype/>
                                <D:resourcetype/>
                                <D:getetag/>
                            </D:prop>
                        </D:propfind>"""

        def response2 = """\
                            <D:multistatus xmlns:D="DAV:">
                                <D:response>
                                    <D:href>/dav/test01@localhost.de/calendar/</D:href>
                                    <D:propstat>
                                        <D:prop>
                                            <D:getcontenttype/>
                                        </D:prop>
                                        <D:status>HTTP/1.1 404 Not Found</D:status>
                                    </D:propstat>
                                    <D:propstat>
                                        <D:prop>
                                            <D:getetag>"NVy57RJot0LhdYELkMDJ9gQZjOM="</D:getetag>
                                            <D:resourcetype>
                                                <C:calendar xmlns:C="urn:ietf:params:xml:ns:caldav"/>
                                                <D:collection/>
                                            </D:resourcetype>
                                        </D:prop>
                                        <D:status>HTTP/1.1 200 OK</D:status>
                                    </D:propstat>
                                </D:response>
                            </D:multistatus>"""

        mockMvc.perform(propfind("/dav/{email}/calendar/", USER01)
                .contentType(TEXT_XML)
                .content(request2))
                .andExpect(status().isMultiStatus())
                .andExpect(textXmlContentType())
                .andExpect(xml(response2))

        mockMvc.perform(options("/dav/{email}/", USER01))
                .andExpect(status().isOk())
                .andExpect(header().string("DAV", "1, 3, calendar-access"))
                .andExpect(header().string(ALLOW, "OPTIONS, GET, HEAD, PROPFIND, PROPPATCH, TRACE, COPY, DELETE, MOVE, REPORT"))
    }

    @Test
    public void addVEvent() {
        mockMvc.perform(put("/dav/{email}/calendar/0c3112fa-ba2b-4cb4-b495-1b842e3f3b77.ics", USER01)
                .contentType(TEXT_CALENDAR)
                .content(VEVENT)
                .header("If-None-Match", "*"))
                .andExpect(status().isCreated())
                .andExpect(etag(is('"1d21bc1d460b1085d53e3def7f7380f6"')))

        def request2 = """\
                        <C:calendar-multiget xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
                            <D:prop>
                                <D:getetag/>
                                <C:calendar-data/>
                            </D:prop>
                            <D:href>/dav/test01%40localhost.de/calendar/0c3112fa-ba2b-4cb4-b495-1b842e3f3b77.ics</D:href>
                        </C:calendar-multiget>"""

        def response2 = """\
                        <D:multistatus xmlns:D="DAV:">
                            <D:response>
                                <D:href>/dav/test01%40localhost.de/calendar/0c3112fa-ba2b-4cb4-b495-1b842e3f3b77.ics</D:href>
                                <D:propstat>
                                    <D:prop>
                                        <D:getetag>"1d21bc1d460b1085d53e3def7f7380f6"</D:getetag>
                                        <C:calendar-data xmlns:C="urn:ietf:params:xml:ns:caldav" C:content-type="text/calendar" C:version="2.0">BEGIN:VCALENDAR&#13;
                                            PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN&#13;
                                            VERSION:2.0&#13;
                                            BEGIN:VTIMEZONE&#13;
                                            TZID:Europe/Stockholm&#13;
                                            BEGIN:DAYLIGHT&#13;
                                            TZOFFSETFROM:+0100&#13;
                                            TZOFFSETTO:+0200&#13;
                                            TZNAME:CEST&#13;
                                            DTSTART:19700329T020000&#13;
                                            RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU&#13;
                                            END:DAYLIGHT&#13;
                                            BEGIN:STANDARD&#13;
                                            TZOFFSETFROM:+0200&#13;
                                            TZOFFSETTO:+0100&#13;
                                            TZNAME:CET&#13;
                                            DTSTART:19701025T030000&#13;
                                            RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU&#13;
                                            END:STANDARD&#13;
                                            END:VTIMEZONE&#13;
                                            BEGIN:VEVENT&#13;
                                            CREATED:20151225T180011Z&#13;
                                            LAST-MODIFIED:20151225T180151Z&#13;
                                            DTSTAMP:20151225T180151Z&#13;
                                            UID:0c3112fa-ba2b-4cb4-b495-1b842e3f3b77&#13;
                                            SUMMARY:VEvent add&#13;
                                            ORGANIZER;RSVP=TRUE;PARTSTAT=ACCEPTED;ROLE=CHAIR:mailto:kamill@test01@localhost.de&#13;
                                            ATTENDEE;RSVP=TRUE;PARTSTAT=NEEDS-ACTION;ROLE=REQ-PARTICIPANT:attendee1&#13;
                                            RRULE:FREQ=DAILY;UNTIL=20160226T190000Z;INTERVAL=3&#13;
                                            CATEGORIES:Business&#13;
                                            DTSTART;TZID=Europe/Stockholm:20151209T200000&#13;
                                            DTEND;TZID=Europe/Stockholm:20151209T215500&#13;
                                            TRANSP:OPAQUE&#13;
                                            LOCATION:location&#13;
                                            DESCRIPTION:description&#13;
                                            X-MOZ-SEND-INVITATIONS:TRUE&#13;
                                            X-MOZ-SEND-INVITATIONS-UNDISCLOSED:FALSE&#13;
                                            BEGIN:VALARM&#13;
                                            ACTION:DISPLAY&#13;
                                            TRIGGER;VALUE=DATE-TIME:20151225T190000Z&#13;
                                            DESCRIPTION:Default Mozilla Description&#13;
                                            END:VALARM&#13;
                                            END:VEVENT&#13;
                                            END:VCALENDAR&#13;
                                        </C:calendar-data>
                                    </D:prop>
                                    <D:status>HTTP/1.1 200 OK</D:status>
                                </D:propstat>
                            </D:response>
                        </D:multistatus>"""

        mockMvc.perform(report("/dav/{email}/calendar/", USER01)
                .content(request2)
                .contentType(TEXT_XML))
                .andExpect(textXmlContentType())
                .andExpect(xml(response2))
    }

    @Test
    public void propfindGetctagAfterAddAndUpdateVEvent() {
        addVEvent()

        def request1 = """\
                        <D:propfind xmlns:D="DAV:" xmlns:CS="http://calendarserver.org/ns/">
                            <D:prop>
                                <CS:getctag/>
                            </D:prop>
                        </D:propfind>"""

        def response1 = """\
                        <D:multistatus xmlns:D="DAV:">
                            <D:response>
                                <D:href>/dav/test01@localhost.de/calendar/</D:href>
                                <D:propstat>
                                    <D:prop>
                                        <CS:getctag xmlns:CS="http://calendarserver.org/ns/">48a4258e4491f632a078122a360d9943</CS:getctag>
                                    </D:prop>
                                    <D:status>HTTP/1.1 200 OK</D:status>
                                </D:propstat>
                            </D:response>
                            <D:response>
                                <D:href>/dav/test01@localhost.de/calendar/0c3112fa-ba2b-4cb4-b495-1b842e3f3b77.ics</D:href>
                                <D:propstat>
                                    <D:prop>
                                        <CS:getctag xmlns:CS="http://calendarserver.org/ns/"/>
                                    </D:prop>
                                    <D:status>HTTP/1.1 404 Not Found</D:status>
                                </D:propstat>
                            </D:response>
                        </D:multistatus>"""

        mockMvc.perform(propfind("/dav/{email}/calendar/", USER01)
                .contentType(TEXT_XML)
                .content(request1))
                .andExpect(status().isMultiStatus())
                .andExpect(textXmlContentType())
                .andExpect(xml(response1))
    }

    @Test
    public void propfindGetcontenttypeResourcetypeGetetagAfterAddAndUpdateVEvent() {
        addVEvent()

        def request1 = """\
                        <D:propfind xmlns:D="DAV:">
                            <D:prop>
                                <D:getcontenttype/>
                                <D:resourcetype/>
                                <D:getetag/>
                            </D:prop>
                        </D:propfind>"""

        def response1 = """\
                        <D:multistatus xmlns:D="DAV:">
                            <D:response>
                                <D:href>/dav/test01@localhost.de/calendar/</D:href>
                                <D:propstat>
                                    <D:prop>
                                        <D:getcontenttype/>
                                    </D:prop>
                                    <D:status>HTTP/1.1 404 Not Found</D:status>
                                </D:propstat>
                                <D:propstat>
                                    <D:prop>
                                        <D:getetag>"48a4258e4491f632a078122a360d9943"</D:getetag>
                                        <D:resourcetype>
                                            <C:calendar xmlns:C="urn:ietf:params:xml:ns:caldav"/>
                                            <D:collection/>
                                        </D:resourcetype>
                                    </D:prop>
                                    <D:status>HTTP/1.1 200 OK</D:status>
                                </D:propstat>
                            </D:response>
                            <D:response>
                                <D:href>/dav/test01@localhost.de/calendar/0c3112fa-ba2b-4cb4-b495-1b842e3f3b77.ics</D:href>
                                <D:propstat>
                                    <D:prop>
                                        <D:getetag>"1d21bc1d460b1085d53e3def7f7380f6"</D:getetag>
                                        <D:getcontenttype>text/calendar; charset=UTF-8</D:getcontenttype>
                                        <D:resourcetype/>
                                    </D:prop>
                                    <D:status>HTTP/1.1 200 OK</D:status>
                                </D:propstat>
                            </D:response>
                        </D:multistatus>"""

        mockMvc.perform(propfind("/dav/{email}/calendar/", USER01)
                .contentType(TEXT_XML)
                .content(request1))
                .andExpect(status().isMultiStatus())
                .andExpect(textXmlContentType())
                .andExpect(xml(response1))
    }

    @Test
    public void addVTodo() {
        mockMvc.perform(put("/dav/{email}/calendar/00396957-a9f9-482e-8c51-96d20889ab56.ics", USER01)
                .contentType(TEXT_CALENDAR)
                .content(VTODO)
                .header("If-None-Match", "*"))
                .andExpect(status().isCreated())
                .andExpect(etag(is('"1d21bc1d460b1085d53e3def7f7380f6"')))

        def request2 = """\
                        <C:calendar-multiget xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
                            <D:prop>
                                <D:getetag/>
                                <C:calendar-data/>
                            </D:prop>
                            <D:href>/dav/test01%40localhost.de/calendar/00396957-a9f9-482e-8c51-96d20889ab56.ics</D:href>
                        </C:calendar-multiget>"""

        def response2 = """\
                        <D:multistatus xmlns:D="DAV:">
                            <D:response>
                                <D:href>/dav/test01%40localhost.de/calendar/00396957-a9f9-482e-8c51-96d20889ab56.ics</D:href>
                                <D:propstat>
                                    <D:prop>
                                        <D:getetag>"1d21bc1d460b1085d53e3def7f7380f6"</D:getetag>
                                        <C:calendar-data xmlns:C="urn:ietf:params:xml:ns:caldav" C:content-type="text/calendar" C:version="2.0">BEGIN:VCALENDAR&#13;
                                            PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN&#13;
                                            VERSION:2.0&#13;
                                            BEGIN:VTIMEZONE&#13;
                                            TZID:Europe/Berlin&#13;
                                            BEGIN:DAYLIGHT&#13;
                                            TZOFFSETFROM:+0100&#13;
                                            TZOFFSETTO:+0200&#13;
                                            TZNAME:CEST&#13;
                                            DTSTART:19700329T020000&#13;
                                            RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU&#13;
                                            END:DAYLIGHT&#13;
                                            BEGIN:STANDARD&#13;
                                            TZOFFSETFROM:+0200&#13;
                                            TZOFFSETTO:+0100&#13;
                                            TZNAME:CET&#13;
                                            DTSTART:19701025T030000&#13;
                                            RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU&#13;
                                            END:STANDARD&#13;
                                            END:VTIMEZONE&#13;
                                            BEGIN:VTIMEZONE&#13;
                                            TZID:Europe/Stockholm&#13;
                                            BEGIN:DAYLIGHT&#13;
                                            TZOFFSETFROM:+0100&#13;
                                            TZOFFSETTO:+0200&#13;
                                            TZNAME:CEST&#13;
                                            DTSTART:19700329T020000&#13;
                                            RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU&#13;
                                            END:DAYLIGHT&#13;
                                            BEGIN:STANDARD&#13;
                                            TZOFFSETFROM:+0200&#13;
                                            TZOFFSETTO:+0100&#13;
                                            TZNAME:CET&#13;
                                            DTSTART:19701025T030000&#13;
                                            RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU&#13;
                                            END:STANDARD&#13;
                                            END:VTIMEZONE&#13;
                                            BEGIN:VTODO&#13;
                                            CREATED:20151225T184045Z&#13;
                                            LAST-MODIFIED:20151225T184131Z&#13;
                                            DTSTAMP:20151225T184131Z&#13;
                                            UID:00396957-a9f9-482e-8c51-96d20889ab56&#13;
                                            SUMMARY:add task&#13;
                                            STATUS:IN-PROCESS&#13;
                                            RRULE:FREQ=WEEKLY;UNTIL=20151231T222500Z&#13;
                                            CATEGORIES:Customer&#13;
                                            DTSTART;TZID=Europe/Berlin:20151201T232500&#13;
                                            DUE;TZID=Europe/Stockholm:20151224T232500&#13;
                                            LOCATION:location&#13;
                                            PERCENT-COMPLETE:25&#13;
                                            DESCRIPTION:description&#13;
                                            BEGIN:VALARM&#13;
                                            ACTION:DISPLAY&#13;
                                            TRIGGER;VALUE=DURATION:-PT30M&#13;
                                            DESCRIPTION:Default Mozilla Description&#13;
                                            END:VALARM&#13;
                                            END:VTODO&#13;
                                            END:VCALENDAR&#13;
                                        </C:calendar-data>
                                    </D:prop>
                                    <D:status>HTTP/1.1 200 OK</D:status>
                                </D:propstat>
                            </D:response>
                        </D:multistatus>"""

        mockMvc.perform(report("/dav/{email}/calendar/", USER01)
                .content(request2)
                .contentType(TEXT_XML))
                .andExpect(textXmlContentType())
                .andExpect(xml(response2))
    }

    @Test
    public void addSameVEvent() {
         addVEvent()

         mockMvc.perform(put("/dav/{email}/calendar/0c3112fa-ba2b-4cb4-b495-1b842e3f3b77.ics", USER01)
                 .contentType(TEXT_CALENDAR)
                 .content(VEVENT)
                 .header("If-Match", '"1d21bc1d460b1085d53e3def7f7380f6"'))
                 .andExpect(status().isNoContent())
                 .andExpect(etag(is('"1d21bc1d460b1085d53e3def7f7380f6"')))
     }

    @Test
    public void addSameVTodo() {
        addVTodo()

        mockMvc.perform(put("/dav/{email}/calendar/00396957-a9f9-482e-8c51-96d20889ab56.ics", USER01)
                .contentType(TEXT_CALENDAR)
                .content(VTODO)
                .header("If-Match", '"1d21bc1d460b1085d53e3def7f7380f6"'))
                .andExpect(status().isNoContent())
                .andExpect(etag(is('"1d21bc1d460b1085d53e3def7f7380f6"')))
    }

    @Test
    public void deleteVEvent() {
        addVEvent()

        mockMvc.perform(delete("/dav/{email}/calendar/0c3112fa-ba2b-4cb4-b495-1b842e3f3b77.ics", USER01)
                .header("If-Match", '"1d21bc1d460b1085d53e3def7f7380f6"'))
                .andExpect(status().isNoContent())
    }

    @Test
    public void deleteVTodo() {
        addVTodo()

        mockMvc.perform(delete("/dav/{email}/calendar/00396957-a9f9-482e-8c51-96d20889ab56.ics", USER01)
                .header("If-Match", '"1d21bc1d460b1085d53e3def7f7380f6"'))
                .andExpect(status().isNoContent())
    }

    @Test
    public void addAndUpdateVEvent() {
        addVEvent()

        def vevent = VEVENT.replace('LOCATION:location', 'LOCATION:newlocation')

        mockMvc.perform(put("/dav/{email}/calendar/0c3112fa-ba2b-4cb4-b495-1b842e3f3b77.ics", USER01)
                .contentType(TEXT_CALENDAR)
                .content(vevent)
                .header("If-Match", '"1d21bc1d460b1085d53e3def7f7380f6"'))
                .andExpect(status().isNoContent())
                .andExpect(etag(is('"1d21bc1d460b1085d53e3def7f7380f6"'))) //TODO same in test
    }
}

package org.unitedinternet.cosmo.dav.impl;

import static carldav.CarldavConstants.GET_CTAG;
import static carldav.CarldavConstants.SUPPORTED_CALENDAR_COMPONENT_SET;
import static carldav.CarldavConstants.SUPPORTED_CALENDAR_DATA;

import carldav.jackrabbit.webdav.property.CustomDavPropertySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unitedinternet.cosmo.calendar.query.CalendarFilter;
import org.unitedinternet.cosmo.dav.CosmoDavException;
import org.unitedinternet.cosmo.dav.DavResourceFactory;
import org.unitedinternet.cosmo.dav.DavResourceLocator;
import org.unitedinternet.cosmo.dav.WebDavResource;
import org.unitedinternet.cosmo.dav.caldav.CaldavConstants;
import org.unitedinternet.cosmo.dav.caldav.property.AddressbookHomeSet;
import org.unitedinternet.cosmo.dav.caldav.property.GetCTag;
import org.unitedinternet.cosmo.dav.caldav.property.SupportedCalendarComponentSet;
import org.unitedinternet.cosmo.dav.caldav.property.SupportedCalendarData;
import org.unitedinternet.cosmo.dav.caldav.property.SupportedCollationSet;
import org.unitedinternet.cosmo.dav.caldav.report.MultigetReport;
import org.unitedinternet.cosmo.dav.caldav.report.QueryReport;
import org.unitedinternet.cosmo.dav.property.DisplayName;
import org.unitedinternet.cosmo.icalendar.ICalendarConstants;
import org.unitedinternet.cosmo.model.hibernate.HibCollectionItem;
import org.unitedinternet.cosmo.model.hibernate.HibICalendarItem;
import org.unitedinternet.cosmo.model.hibernate.HibItem;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

public class DavCalendarCollection extends DavCollectionBase implements CaldavConstants, ICalendarConstants {

    private static final Logger LOG = LoggerFactory.getLogger(DavCalendarCollection.class);

    public DavCalendarCollection(HibCollectionItem collection, DavResourceLocator locator, DavResourceFactory factory) throws CosmoDavException {
        super(collection, locator, factory);

        registerLiveProperty(SUPPORTED_CALENDAR_COMPONENT_SET);
        registerLiveProperty(SUPPORTED_CALENDAR_DATA);
        registerLiveProperty(GET_CTAG);

        reportTypes.add(MultigetReport.REPORT_TYPE_CALDAV_MULTIGET);
        reportTypes.add(QueryReport.REPORT_TYPE_CALDAV_QUERY);
    }

    public String getSupportedMethods() {
        return "OPTIONS, GET, HEAD, TRACE, PROPFIND, PUT, DELETE, REPORT";
    }

    public Set<DavCalendarResource> findMembers(CalendarFilter filter) throws CosmoDavException {
        Set<DavCalendarResource> members = new HashSet<>();

        HibCollectionItem collection = getItem();
        for (HibItem memberItem : getCalendarQueryProcesor().filterQuery(collection, filter)) {
            WebDavResource resource = memberToResource(memberItem);
            members.add((DavCalendarResource) resource);
        }

        return members;
    }

    protected Set<QName> getResourceTypes() {
        Set<QName> rt = super.getResourceTypes();
        rt.add(RESOURCE_TYPE_CALENDAR);
        return rt;
    }

    protected void loadLiveProperties(CustomDavPropertySet properties) {
        super.loadLiveProperties(properties);

        properties.add(new GetCTag(getItem().getEntityTag()));
        properties.add(new SupportedCalendarComponentSet());
        properties.add(new SupportedCollationSet());
        properties.add(new SupportedCalendarData());
        properties.add(new AddressbookHomeSet(getResourceLocator(), getSecurityManager().getSecurityContext().getUser()));
        properties.add(new DisplayName(getItem().getDisplayName()));
    }

    protected void saveContent(DavItemResource member) throws CosmoDavException {
        HibICalendarItem content = (HibICalendarItem) member.getItem();
        final HibItem converted = converter.convert(content);

        if (content.getId() != null) {
            LOG.debug("updating {} {} ", content.getType(), member.getResourcePath());
            getContentService().updateContent(converted);
        } else {
            LOG.debug("creating {} {}", content.getType(), member.getResourcePath());
            getContentService().createContent(getItem(), converted);
        }

        member.setItem(content);
    }
}

/*
 * Copyright 2006-2007 Open Source Applications Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.unitedinternet.cosmo.dav.impl;

import carldav.service.generator.IdGenerator;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import org.unitedinternet.cosmo.dav.CosmoDavException;
import org.unitedinternet.cosmo.dav.DavResourceFactory;
import org.unitedinternet.cosmo.dav.DavResourceLocator;
import org.unitedinternet.cosmo.dav.UnprocessableEntityException;
import org.unitedinternet.cosmo.model.hibernate.EntityConverter;
import org.unitedinternet.cosmo.model.hibernate.HibBaseEventStamp;
import org.unitedinternet.cosmo.model.hibernate.HibICalendarItem;
import org.unitedinternet.cosmo.model.hibernate.HibNoteItem;

/**
 * Extends <code>DavCalendarResource</code> to adapt the Cosmo
 * <code>ContentItem</code> with an <code>EventStamp</code> to 
 * the DAV resource model.
 *
 * This class does not define any live properties.
 */
public class DavEvent extends DavCalendarResource {

    public DavEvent(DavResourceLocator locator,
                    DavResourceFactory factory,
                    IdGenerator idGenerator)
        throws CosmoDavException {
        this(new HibNoteItem(), locator, factory, idGenerator);
        ((HibNoteItem) getItem()).addStamp(new HibBaseEventStamp(getItem()));
    }

    public DavEvent(HibNoteItem item,
                    DavResourceLocator locator,
                    DavResourceFactory factory,
                    IdGenerator idGenerator)
        throws CosmoDavException {
        super(item, locator, factory, idGenerator);
    }

    public Calendar getCalendar() {
        return new EntityConverter(getIdGenerator()).convertNote((HibNoteItem)getItem());
    }

    protected void setCalendar(Calendar calendar)
        throws CosmoDavException {
        
        ComponentList vevents = calendar.getComponents(Component.VEVENT);
        ComponentList vjournal = calendar.getComponents(Component.VJOURNAL);
        if (vevents.isEmpty() && vjournal.isEmpty()) {
            throw new UnprocessableEntityException("VCALENDAR does not contain any VEVENTs or VJOURNAL");
        }

        final HibICalendarItem item = (HibICalendarItem) getItem();
        item.setCalendar(calendar);
    }

    @Override
    public boolean isCollection() {
        return false;
    }   

}

/*
 * Copyright 2006 Open Source Applications Foundation
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
package org.unitedinternet.cosmo.dao.hibernate.query;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.SessionFactoryUtils;
import org.springframework.orm.hibernate4.SessionHolder;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.unitedinternet.cosmo.IntegrationTestSupport;
import org.unitedinternet.cosmo.dao.query.hibernate.StandardItemFilterProcessor;
import org.unitedinternet.cosmo.model.TriageStatusUtil;
import org.unitedinternet.cosmo.model.filter.AttributeFilter;
import org.unitedinternet.cosmo.model.filter.ContentItemFilter;
import org.unitedinternet.cosmo.model.filter.EventStampFilter;
import org.unitedinternet.cosmo.model.filter.ItemFilter;
import org.unitedinternet.cosmo.model.filter.NoteItemFilter;
import org.unitedinternet.cosmo.model.filter.Restrictions;
import org.unitedinternet.cosmo.model.filter.StampFilter;
import org.unitedinternet.cosmo.model.hibernate.HibCollectionItem;
import org.unitedinternet.cosmo.model.hibernate.HibEventStamp;
import org.unitedinternet.cosmo.model.hibernate.HibNoteItem;
import org.unitedinternet.cosmo.model.hibernate.HibQName;

import java.util.Calendar;
import java.util.Date;

public class StandardItemFilterProcessorTest extends IntegrationTestSupport {

    @Autowired
    private SessionFactory sessionFactory;

    private StandardItemFilterProcessor queryBuilder = new StandardItemFilterProcessor();

    private Session session;

    private TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();

    @BeforeTransaction
    public void onSetUpBeforeTransaction() throws Exception {
        // Unbind session from TransactionManager
        session = sessionFactory.openSession();
        TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
    }

    @AfterTransaction
    public void onTearDownAfterTransaction() throws Exception {
        SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
        Session s = holder.getSession();
        TransactionSynchronizationManager.unbindResource(sessionFactory);
        SessionFactoryUtils.closeSession(s);
    }

    /**
     * Tests uid query.
     * @throws Exception - if something is wrong this exception is thrown.
     */
    @Test
    public void testUidQuery() throws Exception {
        ItemFilter filter = new ItemFilter();
        filter.setUid(Restrictions.eq("abc"));
        Query query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibItem i where i.uid=:param0", query.getQueryString());
    }
    
    @Test
    public void testModifiedSinceQuery(){
        NoteItemFilter filter = new NoteItemFilter();
        Calendar c = Calendar.getInstance();
        Date end = c.getTime();
        c.add(Calendar.YEAR, -1);
        filter.setModifiedSince(Restrictions.between(c.getTime(), end));
        Query query = queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibNoteItem i where i.modifiedDate between :param0 and :param1", query.getQueryString());
    }
    
    /**
     * Tests display name query.
     * @throws Exception - if something is wrong this exception is thrown.
     */
    @Test
    public void testDisplayNameQuery() throws Exception {
        ItemFilter filter = new ItemFilter();
        filter.setDisplayName(Restrictions.eq("test"));
        Query query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibItem i where i.displayName=:param0", query.getQueryString());
    
        filter.setDisplayName(Restrictions.neq("test"));
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibItem i where i.displayName!=:param0", query.getQueryString());
    
        filter.setDisplayName(Restrictions.like("test"));
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibItem i where i.displayName like :param0", query.getQueryString());
        
        filter.setDisplayName(Restrictions.nlike("test"));
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibItem i where i.displayName not like :param0", query.getQueryString());
    
        filter.setDisplayName(Restrictions.isNull());
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibItem i where i.displayName is null", query.getQueryString());
    
        filter.setDisplayName(Restrictions.ilike("test"));
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibItem i where lower(i.displayName) like :param0", query.getQueryString());
    
        filter.setDisplayName(Restrictions.nilike("test"));
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibItem i where lower(i.displayName) not like :param0", query.getQueryString());
    
    }
    
    /**
     * Tests parent query.
     * @throws Exception - if something is wrong this exception is thrown.
     */
    @Test
    public void testParentQuery() throws Exception {
        ItemFilter filter = new ItemFilter();
        HibCollectionItem parent = new HibCollectionItem();
        filter.setParent(parent);
        Query query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibItem i join i.parentDetails pd where "
                + "pd.collection=:parent", query.getQueryString());
    }
    
    /**
     * Tests display name and parent query.
     * @throws Exception - if something is wrong this exception is thrown.
     */
    @Test
    public void testDisplayNameAndParentQuery() throws Exception {
        ItemFilter filter = new ItemFilter();
        HibCollectionItem parent = new HibCollectionItem();
        filter.setParent(parent);
        filter.setDisplayName(Restrictions.eq("test"));
        Query query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibItem i join i.parentDetails pd where "
                + "pd.collection=:parent and i.displayName=:param1", query.getQueryString());
    }
    
    /**
     * Tests content item query.
     * @throws Exception - if something is wrong this exception is thrown.
     */
    @Test
    public void testContentItemQuery() throws Exception {
        ContentItemFilter filter = new ContentItemFilter();
        HibCollectionItem parent = new HibCollectionItem();
        filter.setParent(parent);
        filter.setTriageStatusCode(Restrictions.eq(TriageStatusUtil.CODE_DONE));
        Query query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibContentItem i join i.parentDetails pd where "
                + "pd.collection=:parent and i.triageStatus.code=:param1", 
                   query.getQueryString());
    
        filter.setTriageStatusCode(Restrictions.isNull());
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibContentItem i join i.parentDetails pd where "
                + "pd.collection=:parent and i.triageStatus.code is null", 
                   query.getQueryString());
        
        filter.setTriageStatusCode(Restrictions.eq(TriageStatusUtil.CODE_DONE));
        filter.addOrderBy(ContentItemFilter.ORDER_BY_TRIAGE_STATUS_RANK_ASC);
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibContentItem i join i.parentDetails pd where "
                + "pd.collection=:parent and i.triageStatus.code=:param1 order by "
                + "i.triageStatus.rank", query.getQueryString());
    }
    
    /**
     * Tests note item query.
     * @throws Exception - if something is wrong this exception is thrown.
     */
    @Test
    public void testNoteItemQuery() throws Exception {
        NoteItemFilter filter = new NoteItemFilter();
        HibCollectionItem parent = new HibCollectionItem();
        filter.setParent(parent);
        filter.setDisplayName(Restrictions.eq("test"));
        filter.setIcalUid(Restrictions.eq("icaluid"));
        filter.setBody(Restrictions.eq("body"));
        filter.setTriageStatusCode(Restrictions.eq(TriageStatusUtil.CODE_DONE));
        
        Query query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibNoteItem i join i.parentDetails pd where pd.collection=:parent and i.displayName=:param1 and i.triageStatus.code=:param2 and i.icalUid=:param3 and i.body=:param4", query.getQueryString());
        
        filter = new NoteItemFilter();
        filter.setIsModification(true);
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibNoteItem i where i.modifies is not null", query.getQueryString());
       
        filter.setIsModification(false);
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibNoteItem i where i.modifies is null", query.getQueryString());
       
        filter.setIsModification(null);
        
        filter.setHasModifications(true);
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibNoteItem i where size(i.modifications) > 0", query.getQueryString());
        
        filter.setHasModifications(false);
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibNoteItem i where size(i.modifications) = 0", query.getQueryString());
    
        filter =  new NoteItemFilter();
        filter.setMasterNoteItem(new HibNoteItem());
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibNoteItem i where (i=:masterItem or "
                + "i.modifies=:masterItem)", query.getQueryString());
    
        filter = new NoteItemFilter();
        Date date1 = new Date(1000);
        Date date2 = new Date(2000);
        filter.setReminderTime(Restrictions.between(date1,date2));
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibNoteItem i where i.remindertime between :param0 and :param1", query.getQueryString());
    
    }

    /**
     * Tests basic stamp query.
     * @throws Exception - if something is wrong this exception is thrown.
     */
    @Test
    public void testBasicStampQuery() throws Exception {
        NoteItemFilter filter = new NoteItemFilter();
        StampFilter missingFilter = new StampFilter();
        missingFilter.setStampClass(HibEventStamp.class);
        filter.getStampFilters().add(missingFilter);
        Query query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibNoteItem i where exists (select s.id from HibStamp s "
                + "where s.item=i and s.class=HibEventStamp)", query.getQueryString());
        missingFilter.setMissing(true);
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibNoteItem i where not exists "
                + "(select s.id from HibStamp s where s.item=i and s.class=HibEventStamp)",
                query.getQueryString());
    }

    /**
     * Tests basic attribute query.
     * @throws Exception - if something is wrong this exception is thrown.
     */
    @Test
    public void testBasicAttributeQuery() throws Exception {
        NoteItemFilter filter = new NoteItemFilter();
        AttributeFilter missingFilter = new AttributeFilter();
        missingFilter.setQname(new HibQName("ns","name"));
        filter.getAttributeFilters().add(missingFilter);
        Query query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibNoteItem i where exists "
                + "(select a.id from HibAttribute a where a.item=i and a.qname=:param0)",
                query.getQueryString());
        missingFilter.setMissing(true);
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibNoteItem i where not exists"
                + " (select a.id from HibAttribute a where a.item=i and a.qname=:param0)",
                query.getQueryString());
    }

    /**
     * Tests event stamp query.
     * @throws Exception - if something is wrong this exception is thrown.
     */
    @Test
    public void testEventStampQuery() throws Exception {
        NoteItemFilter filter = new NoteItemFilter();
        EventStampFilter eventFilter = new EventStampFilter();
        HibCollectionItem parent = new HibCollectionItem();
        filter.setParent(parent);
        filter.setDisplayName(Restrictions.eq("test"));
        filter.setIcalUid(Restrictions.eq("icaluid"));
        //filter.setBody("body");
        filter.getStampFilters().add(eventFilter);
        Query query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibNoteItem i join i.parentDetails pd, "
                + "HibBaseEventStamp es where pd.collection=:parent and "
                + "i.displayName=:param1 and es.item=i and es.class = 'event' and i.icalUid=:param2", query.getQueryString());

        eventFilter.setIsRecurring(true);
        query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibNoteItem i join i.parentDetails pd, HibBaseEventStamp "
                + "es where pd.collection=:parent and i.displayName=:param1 and "
                + "es.item=i and es.class = 'event' and (es.timeRangeIndex.isRecurring=true or i.modifies is not null) "
                + "and i.icalUid=:param2", query.getQueryString());
    }

    /**
     * Tests event stamp time range query.
     * @throws Exception - if something is wrong this exception is thrown.
     */
    @Test
    public void testEventStampTimeRangeQuery() throws Exception {
        NoteItemFilter filter = new NoteItemFilter();
        EventStampFilter eventFilter = new EventStampFilter();
        Period period = new Period(new DateTime("20070101T100000Z"), new DateTime("20070201T100000Z"));
        eventFilter.setPeriod(period);
        eventFilter.setTimezone(registry.getTimeZone("America/Chicago"));

        HibCollectionItem parent = new HibCollectionItem();
        filter.setParent(parent);
        filter.getStampFilters().add(eventFilter);
        Query query =  queryBuilder.buildQuery(session, filter);
        Assert.assertEquals("select i from HibNoteItem i join i.parentDetails pd, "
                + "HibBaseEventStamp es where pd.collection=:parent and es.item=i "
                + "and es.class = 'event' and ( (es.timeRangeIndex.isFloating=true and "
                + "es.timeRangeIndex.startDate < '20070201T040000' and "
                + "es.timeRangeIndex.endDate > '20070101T040000') or "
                + "(es.timeRangeIndex.isFloating=false and "
                + "es.timeRangeIndex.startDate < '20070201T100000Z' and "
                + "es.timeRangeIndex.endDate > '20070101T100000Z') or "
                + "(es.timeRangeIndex.startDate=es.timeRangeIndex.endDate and "
                + "(es.timeRangeIndex.startDate='20070101T040000' or "
                + "es.timeRangeIndex.startDate='20070101T100000Z')))", query.getQueryString());
    }
}

INSERT INTO users(id,createdate,etag,modifydate,admin,locked,email,password,uid,username) VALUES (1,1447688116670,'c3hz93Topi4igzn82UA+JAOHyVU=',1447688116670,1,0,'root@localhost','098f6bcd4621d373cade4e832627b4f6','6d755da8-0954-4902-8063-3a53c9fc46a6','root');
INSERT INTO users(id,createdate,etag,modifydate,admin,locked,email,password,uid,username) VALUES (2,1447688116670,'q0leu+2ctlWs3jLUakICskqYGms=',1447688116670,0,0,'test01@localhost.de','098f6bcd4621d373cade4e832627b4f6','39b8a610-3a5c-4554-8da9-b98c5113548f','test01@localhost.de');
INSERT INTO users(id,createdate,etag,modifydate,admin,locked,email,password,uid,username) VALUES (3,1447688116670,'10leu+2ctlWs3jLUakICskqYGms=',1447688116670,0,0,'test02@localhost.de','098f6bcd4621d373cade4e832627b4f6','19b8a610-3a5c-4554-8da9-b98c5113548f','test02@localhost.de');

INSERT INTO item (itemtype, id, createdate, etag, modifydate, clientcreatedate, clientmodifieddate, displayname, itemname, uid, version, lastmodification, lastmodifiedby, needsreply, sent, isautotriage, triagestatuscode, triagestatusrank, icaluid, contentEncoding, contentLanguage, contentLength, contentType, hasmodifications, ownerid, contentdataid, modifiesitemid)
VALUES ('homecollection', 1, 1448140260056, 'ghFexXxxU+9KC/of1jmJ82wMFig=', 1448140260056, null, null, null, 'test01@localhost.de', 'de359448-1ee0-4151-872d-eea0ee462bc6', 0, null, null, null, null, null, null, null, null, null, null, null, null, null, 2, null, null);

INSERT INTO item (itemtype, id, createdate, etag, modifydate, clientcreatedate, clientmodifieddate, displayname, itemname, uid, version, lastmodification, lastmodifiedby, needsreply, sent, isautotriage, triagestatuscode, triagestatusrank, icaluid, contentEncoding, contentLanguage, contentLength, contentType, hasmodifications, ownerid, contentdataid, modifiesitemid)
VALUES ('collection', 2, 1448140260072, 'NVy57RJot0LhdYELkMDJ9gQZjOM=', 1448140260072, null, null, 'calendarDisplayName', 'calendar', 'a172ed34-0106-4616-bb40-a416a8305465', 0, null, null, null, null, null, null, null, null, null, null, null, null, null, 2, null, null);

INSERT INTO item (itemtype, id, createdate, etag, modifydate, clientcreatedate, clientmodifieddate, displayname, itemname, uid, version, lastmodification, lastmodifiedby, needsreply, sent, isautotriage, triagestatuscode, triagestatusrank, icaluid, contentEncoding, contentLanguage, contentLength, contentType, hasmodifications, ownerid, contentdataid, modifiesitemid)
VALUES ('collection', 3, 1448140260072, 'njy57RJot0LhdYELkMDJ9gQZiOM=', 1448140260072, null, null, 'contactDisplayName', 'contacts', 'a112ed14-0106-4616-bb40-a416a8305465', 0, null, null, null, null, null, null, null, null, null, null, null, null, null, 2, null, null);

INSERT INTO attribute (attributetype, id, createdate, etag, modifydate, localname, namespace, booleanvalue, textvalue, intvalue, stringvalue, itemid) VALUES ('string', 1, 1448140260077, '', 1448140260077, 'color', 'org.unitedinternet.cosmo.model.CalendarCollectionStamp', null, null, null, '#f0f0f0', 2);
INSERT INTO attribute (attributetype, id, createdate, etag, modifydate, localname, namespace, booleanvalue, textvalue, intvalue, stringvalue, itemid) VALUES ('boolean', 2, 1448140260079, '', 1448140260079, 'visibility', 'org.unitedinternet.cosmo.model.CalendarCollectionStamp', true, null, null, null, 2);

INSERT INTO stamp (stamptype, id, createdate, etag, modifydate, itemid) VALUES ('calendar', 1, 1448140260087, '', 1448140260087, 2);
INSERT INTO stamp (stamptype, id, createdate, etag, modifydate, itemid) VALUES ('card', 2, 1448140260087, '', 1448140260087, 3);

INSERT INTO collection_item (createdate, itemid, collectionid) VALUES (1448140260072, 2, 1);
INSERT INTO collection_item (createdate, itemid, collectionid) VALUES (1448140260072, 3, 1);
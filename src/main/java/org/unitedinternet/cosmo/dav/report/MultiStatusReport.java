/*
 * Copyright 2007 Open Source Applications Foundation
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
package org.unitedinternet.cosmo.dav.report;

import carldav.exception.resolver.ResponseUtils;
import carldav.jackrabbit.webdav.property.DavPropertyNameSet;
import carldav.jackrabbit.webdav.MultiStatus;
import carldav.jackrabbit.webdav.MultiStatusResponse;
import org.unitedinternet.cosmo.dav.CosmoDavException;
import org.unitedinternet.cosmo.dav.WebDavResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletResponse;

/**
 * Base class for WebDAV reports that return multistatus responses.
 */
public abstract class MultiStatusReport extends ReportBase {

    private MultiStatus multistatus = new MultiStatus();
    protected int propfindType = PROPFIND_ALL_PROP;
    private DavPropertyNameSet propfindProps;

    protected void output(HttpServletResponse response)
            throws CosmoDavException {
        try {
            buildMultistatus();
            ResponseUtils.sendXmlResponse(response, multistatus, 207);
        } catch (Exception e) {
            throw new CosmoDavException(e);
        }
    }

    public final void buildMultistatus() throws CosmoDavException {

        DavPropertyNameSet resultProps = createResultPropSpec();

        for (WebDavResource result : getResults()) {
            MultiStatusResponse msr = buildMultiStatusResponse(result, resultProps);
            multistatus.addResponse(msr);
        }
    }

    protected DavPropertyNameSet createResultPropSpec() {
        return new DavPropertyNameSet(propfindProps);
    }

    /**
     * Returns a <code>MultiStatusResponse</code> describing the
     * specified resource including the specified properties.
     */
    protected MultiStatusResponse buildMultiStatusResponse(WebDavResource resource, DavPropertyNameSet props) {
        if (props.isEmpty()) {
            String href = resource.getResourceLocator().
                    getHref(resource.isCollection());
            return new MultiStatusResponse(href, 200);
        }
        return new MultiStatusResponse(resource, props, propfindType);
    }

    protected MultiStatus getMultiStatus() {
        return multistatus;
    }

    public final Element toXml(Document document) {
        try {
            runQuery();
        } catch (CosmoDavException e) {
            throw new RuntimeException(e);
        }

        return multistatus.toXml(document);
    }

    public void setPropFindType(int type) {
        this.propfindType = type;
    }

    public DavPropertyNameSet getPropFindProps() {
        return propfindProps;
    }

    public void setPropFindProps(DavPropertyNameSet props) {
        this.propfindProps = props;
    }
}

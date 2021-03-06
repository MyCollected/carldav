package carldav.jackrabbit.webdav;

import static carldav.CarldavConstants.caldav;

import carldav.jackrabbit.webdav.property.DavPropertyName;
import carldav.jackrabbit.webdav.property.DavPropertyNameSet;
import carldav.jackrabbit.webdav.property.DavPropertySet;
import carldav.jackrabbit.webdav.property.PropContainer;
import carldav.jackrabbit.webdav.xml.DomUtils;
import carldav.jackrabbit.webdav.xml.XmlSerializable;
import org.unitedinternet.cosmo.dav.WebDavResource;
import org.unitedinternet.cosmo.dav.property.WebDavProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>MultiStatusResponse</code> represents the DAV:multistatus element defined
 * by RFC 2518:
 * <pre>
 * &lt;!ELEMENT response (href, ((href*, status)|(propstat+)), responsedescription?) &gt;
 * &lt;!ELEMENT status (#PCDATA) &gt;
 * &lt;!ELEMENT propstat (prop, status, responsedescription?) &gt;
 * &lt;!ELEMENT responsedescription (#PCDATA) &gt;
 * &lt;!ELEMENT prop ANY &gt;
 * </pre>
 */
public class MultiStatusResponse implements XmlSerializable, DavConstants {

    private static final int TYPE_PROPSTAT = 0;
    private static final int TYPE_HREFSTATUS = 1;

    /**
     * The type of MultiStatusResponse
     */
    private int type;

    /**
     * The content the 'href' element for this response
     */
    private String href;

    /**
     * Type of MultiStatus response: Href + Status
     */
    private Status status;

    /**
     * Type of MultiStatus response: PropStat Hashmap containing all status
     */
    private HashMap<Integer, PropContainer> statusMap = new HashMap<>();

    public MultiStatusResponse(final WebDavResource resource, final DavPropertyNameSet propNameSet, final int propFindType) {
        customMultiStatusResponse(resource, propNameSet, propFindType);
    }

    public MultiStatusResponse(final String href, final int i) {
        if (!isValidHref(href)) {
            throw new IllegalArgumentException("Invalid href ('" + href + "')");
        }
        this.href = href;
        this.status = new Status(i);
        this.type = TYPE_HREFSTATUS;
    }

    private void customMultiStatusResponse(String href, int type) {
        if (!isValidHref(href)) {
            throw new IllegalArgumentException("Invalid href ('" + href + "')");
        }
        this.href = href;
        this.type = type;
    }

    /**
     * Constructs a WebDAV multistatus response and retrieves the resource
     * properties according to the given <code>DavPropertyNameSet</code>. It
     * adds all known property to the '200' set, while unknown properties are
     * added to the '404' set.
     * <p>
     * Note, that the set of property names is ignored in case of a {@link
     * #PROPFIND_ALL_PROP} and {@link #PROPFIND_PROPERTY_NAMES} propFindType.
     *
     * @param resource The resource to retrieve the property from
     * @param propNameSet The property name set as obtained from the request
     * body.
     * @param propFindType any of the following values: {@link
     * #PROPFIND_ALL_PROP}, {@link #PROPFIND_BY_PROPERTY}, {@link
     * #PROPFIND_PROPERTY_NAMES}, {@link #PROPFIND_ALL_PROP_INCLUDE}
     */
    public void customMultiStatusResponse(
            WebDavResource resource, DavPropertyNameSet propNameSet,
            int propFindType) {
        customMultiStatusResponse(resource.getHref(), TYPE_PROPSTAT);

        if (propFindType == PROPFIND_PROPERTY_NAMES) {
            // only property names requested
            PropContainer status200 = getPropContainer(200, true);
            for (DavPropertyName propName : resource.getPropertyNames()) {
                status200.addContent(propName);
            }
        } else {
            // all or a specified set of property and their values requested.
            PropContainer status200 = getPropContainer(200, false);

            // Collection of missing property names for 404 responses
            Set<DavPropertyName> missing = new HashSet<>(propNameSet.getContent());

            // Add requested properties or all non-protected properties,
            // or non-protected properties plus requested properties (allprop/include)
            if (propFindType == PROPFIND_BY_PROPERTY) {
                // add explicitly requested properties (proptected or non-protected)
                for (DavPropertyName propName : propNameSet.getContent()) {
                    WebDavProperty<?> prop = resource.getProperty(propName);
                    if (prop != null) {
                        status200.addContent(prop);
                        missing.remove(propName);
                    }
                }
            } else {
                // add all non-protected properties
                for (WebDavProperty<?> property : resource.getProperties()) {
                    boolean allDeadPlusRfc4918LiveProperties =
                            propFindType == PROPFIND_ALL_PROP
                                    || propFindType == PROPFIND_ALL_PROP_INCLUDE;
                    boolean wasRequested = missing.remove(property.getName());

                    if ((allDeadPlusRfc4918LiveProperties
                            && !property.isInvisibleInAllprop())
                            || wasRequested) {
                        status200.addContent(property);
                    }
                }

                // try if missing properties specified in the include section
                // can be obtained using resource.getProperty
                if (propFindType == PROPFIND_ALL_PROP_INCLUDE && !missing.isEmpty()) {
                    for (DavPropertyName propName : new HashSet<>(missing)) {
                        WebDavProperty<?> prop = resource.getProperty(propName);
                        if (prop != null) {
                            status200.addContent(prop);
                            missing.remove(propName);
                        }
                    }
                }
            }

            if (!missing.isEmpty() && propFindType != PROPFIND_ALL_PROP) {
                PropContainer status404 = getPropContainer(404, true);
                for (DavPropertyName propName : missing) {
                    status404.addContent(propName);
                }
            }
        }
    }

    private PropContainer getPropContainer(int status, boolean forNames) {
        PropContainer propContainer = statusMap.get(status);
        if (propContainer == null) {
            if (forNames) {
                propContainer = new DavPropertyNameSet();
            } else {
                propContainer = new DavPropertySet();
            }
            statusMap.put(status, propContainer);
        }
        return propContainer;
    }

    /**
     * @param href
     * @return false if the given href is <code>null</code> or empty string.
     */
    private static boolean isValidHref(String href) {
        return href != null && !"".equals(href);
    }

    /**
     * Adds a property to this response '200' propstat set.
     *
     * @param property the property to add
     */
    public void add(WebDavProperty<?> property) {
        checkType(TYPE_PROPSTAT);
        PropContainer status200 = getPropContainer(200, false);
        status200.addContent(property);
    }

    public String getHref() {
        return href;
    }

    /**
     * Adds a property name to this response '200' propstat set.
     *
     * @param propertyName the property name to add
     */
    public void add(DavPropertyName propertyName) {
        checkType(TYPE_PROPSTAT);
        PropContainer status200 = getPropContainer(200, true);
        status200.addContent(propertyName);
    }

    /**
     * Adds a property to this response
     *
     * @param property the property to add
     * @param status the status of the response set to select
     */
    public void add(WebDavProperty<?> property, int status) {
        checkType(TYPE_PROPSTAT);
        PropContainer propCont = getPropContainer(status, false);
        propCont.addContent(property);
    }

    /**
     * Adds a property name to this response
     *
     * @param propertyName the property name to add
     * @param status the status of the response set to select
     */
    public void add(DavPropertyName propertyName, int status) {
        checkType(TYPE_PROPSTAT);
        PropContainer propCont = getPropContainer(status, true);
        propCont.addContent(propertyName);
    }

    private void checkType(int type) {
        if (this.type != type) {
            throw new IllegalStateException("The given MultiStatusResponse is not of the required type.");
        }
    }

    @Override
    public Element toXml(final Document document) {
        Element response = DomUtils.createElement(document, XML_RESPONSE, caldav(XML_RESPONSE));
        // add '<href>'
        response.appendChild(DomUtils.hrefToXml(href, document));
        if (type == TYPE_PROPSTAT) {
            // add '<propstat>' elements
            for (Integer statusKey : statusMap.keySet()) {
                Status st = new Status(statusKey);
                PropContainer propCont = statusMap.get(statusKey);
                if (!propCont.isEmpty()) {
                    Element propstat = DomUtils.createElement(document, XML_PROPSTAT, caldav(XML_PROPSTAT));
                    propstat.appendChild(propCont.toXml(document));
                    propstat.appendChild(st.toXml(document));
                    response.appendChild(propstat);
                }
            }
        } else {
            // add a single '<status>' element
            // NOTE: a href+status response cannot be created with 'null' status
            response.appendChild(status.toXml(document));
        }
        return response;
    }
}

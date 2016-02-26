package carldav.jackrabbit.webdav.version.report;

import static carldav.jackrabbit.webdav.CustomDavConstants.XML_PROP;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import carldav.CarldavConstants;
import carldav.jackrabbit.webdav.CustomDavConstants;
import carldav.jackrabbit.webdav.property.CustomDavPropertyNameSet;
import carldav.jackrabbit.webdav.xml.CustomDomUtils;
import carldav.jackrabbit.webdav.xml.CustomElementIterator;
import carldav.jackrabbit.webdav.xml.CustomXmlSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unitedinternet.cosmo.dav.CosmoDavException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

public class CustomReportInfo implements CustomXmlSerializable {

    private static Logger LOG = LoggerFactory.getLogger(CustomReportInfo.class);

    private final String typeLocalName;
    private final QName typeNamespace;
    private final int depth;
    private final CustomDavPropertyNameSet propertyNames;
    private final List<Element> content = new ArrayList<>();
    private Element documentElement;

    /**
     * Create a new <code>ReportInfo</code> object from the given Xml element.
     *
     * @param reportElement
     * @param depth Depth value as retrieved from the {@link CustomDavConstants#HEADER_DEPTH}.
     * @throws CosmoDavException if the report element is <code>null</code>.
     */
    public CustomReportInfo(Element reportElement, int depth) {
        if (reportElement == null) {
            LOG.warn("Report request body must not be null.");
            throw new CosmoDavException(BAD_REQUEST.value());
        }

        this.documentElement = (Element) reportElement.cloneNode(true);

        this.typeLocalName = reportElement.getLocalName();
        this.typeNamespace = CustomDomUtils.getNamespace(reportElement);
        this.depth = depth;
        Element propElement = CustomDomUtils.getChildElement(reportElement, CarldavConstants.caldav(XML_PROP));
        if (propElement != null) {
            propertyNames = new CustomDavPropertyNameSet(propElement);
            reportElement.removeChild(propElement);
        } else {
            propertyNames = new CustomDavPropertyNameSet();
        }

        CustomElementIterator it = CustomDomUtils.getChildren(reportElement);
        while (it.hasNext()) {
            Element el = it.nextElement();
            if (!CustomDavConstants.XML_PROP.equals(el.getLocalName())) {
                content.add(el);
            }
        }
    }

    /**
     * Returns the depth field. The request must be applied separately to the
     * collection itself and to all members of the collection that satisfy the
     * depth value.
     *
     * @return depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Name of the report type that will be / has been requested.
     *
     * @return Name of the report type
     */
    public String getReportName() {
        return typeNamespace.toString(); //DomUtil.getExpandedName(typeLocalName, typeNamespace);
    }

    /**
     * Indicates whether this info contains an element with the given name/namespace.
     *
     * @param namespace
     * @return true if an element with the given name/namespace is present in the
     * body of the request info.
     */
    public boolean containsContentElement(QName namespace) {
        if (content.isEmpty()) {
            return false;
        }
        for (Element elem : content) {
            boolean sameNamespace = (namespace == null) ? elem.getNamespaceURI() == null : namespace.getNamespaceURI().equals(elem.getNamespaceURI());
            if (sameNamespace && elem.getLocalName().equals(namespace.getLocalPart())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a list containing all child Xml elements of this info that have
     * the specified name/namespace. If this info contains no such element,
     * an empty list is returned.
     *
     * @param namespace
     * @return List contain all child elements with the given name/namespace
     * or an empty list.
     */
    public List<Element> getContentElements(QName namespace) {
        List<Element> l = new ArrayList<>();
        for (Element elem : content) {
            if (CustomDomUtils.matches(elem, namespace.getLocalPart(), namespace)) {
                l.add(elem);
            }
        }
        return l;
    }

    /**
     * Returns a <code>DavPropertyNameSet</code> providing the property names present
     * in an eventual {@link CustomDavConstants#XML_PROP} child element. If no such
     * child element is present an empty set is returned.
     *
     * @return {@link CustomDavPropertyNameSet} providing the property names present
     * in an eventual {@link CustomDavConstants#XML_PROP DAV:prop} child element or an empty set.
     */
    public CustomDavPropertyNameSet getPropertyNameSet() {
        return propertyNames;
    }


    /**
     * @see CustomXmlSerializable#toXml(Document)
     * @param document
     */
    public Element toXml(Document document) {
        Element reportElement = CustomDomUtils.createElement(document, typeLocalName, typeNamespace);
        if (!content.isEmpty()) {
            for (Element contentEntry : content) {
                Node n = document.importNode(contentEntry, true);
                reportElement.appendChild(n);
            }
        }
        if (!propertyNames.isEmpty()) {
            reportElement.appendChild(propertyNames.toXml(document));
        }
        return reportElement;
    }

    public Element getDocumentElement() {
        return documentElement;
    }
}
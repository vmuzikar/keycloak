package org.keycloak.testsuite.util;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

/**
 *
 * @author tkyjovsk
 */
public class IOUtil {

    public static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load json.", e);
        }
    }

    public static RealmRepresentation loadRealm(String realmConfig) {
        return loadRealm(IOUtil.class.getResourceAsStream(realmConfig));
    }

    public static RealmRepresentation loadRealm(File realmFile) {
        try {
            return loadRealm(new FileInputStream(realmFile));
        } catch (FileNotFoundException ex) {
            throw new IllegalStateException("Test realm file not found: " + realmFile);
        }
    }

    public static RealmRepresentation loadRealm(InputStream is) {
        RealmRepresentation realm = loadJson(is, RealmRepresentation.class);
        System.out.println("Loaded realm " + realm.getRealm());
        return realm;
    }

    public static Document loadXML(InputStream is) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(is);
        } catch (ParserConfigurationException|SAXException|IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String documentToString(Document newDoc) throws TransformerException {
        DOMSource domSource = new DOMSource(newDoc);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter sw = new StringWriter();
        StreamResult sr = new StreamResult(sw);
        transformer.transform(domSource, sr);
        return sw.toString();
    }

    public static void modifyDocElementAttribute(Document doc, String tagName, String attributeName, String regex, String replacement) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() != 1) {
            System.out.println("Not able to find element: " + tagName);
            return;
        }

        Node node = nodes.item(0).getAttributes().getNamedItem(attributeName);
        if (node == null) {
            System.out.println("Not able to find attribute " + attributeName + " within element: " + tagName);
            return;
        }
        node.setTextContent(node.getTextContent().replace(regex, replacement));
    }
}

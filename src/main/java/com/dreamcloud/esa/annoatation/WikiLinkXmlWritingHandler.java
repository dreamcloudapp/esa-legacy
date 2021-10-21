package com.dreamcloud.esa.annoatation;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Map;

public class WikiLinkXmlWritingHandler extends WikiLinkHandler {
    protected WikiLinkAnnotatorOptions options;
    protected XMLStreamWriter xmlWriter;
    public int numSkipped = 0;

    public WikiLinkXmlWritingHandler(Map<String, String> titleMap, Map<String, WikiLinkAnnotation> annotations, WikiLinkAnnotatorOptions options, XMLStreamWriter xmlWriter) {
        super(titleMap, annotations);
        this.options = options;
        this.xmlWriter = xmlWriter;
    }


    public void endElement(String uri, String localName, String qName) {
        if (inDoc && inDocTitle && "title".equals(localName)) {
            inDocTitle = false;
            title = content.toString();
        } else if (inDoc && inDocText && "text".equals(localName)) {
            numRead++;
            inDocText = false;
            WikiLinkAnnotation annotation = annotations.getOrDefault(title, null);
            if (annotation != null) {
                if (annotation.incomingLinks < options.minimumIncomingLinks || annotation.outgoingLinks < options.minimumOutgoingLinks) {
                    numSkipped++;
                } else {
                    try {
                        writeDocument(title, content.toString(), annotation);
                    } catch (XMLStreamException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            } else {
                numSkipped++;
            }

            if (numRead % 1000 == 0) {
                System.out.println("annotated article\t[" + numSkipped + " | " + numRead + "]");
            }
        } else if (inDoc && "doc".equals(localName)) {
            inDoc = false;
        }
    }

    public void writeDocumentBegin() throws XMLStreamException {
        this.xmlWriter.writeStartDocument();
        xmlWriter.writeStartElement("docs");
    }

    public void writeDocumentEnd() throws XMLStreamException {
        xmlWriter.writeEndElement();
        xmlWriter.writeEndDocument();
    }

    public void writeDocument(String title, String text, WikiLinkAnnotation annotation) throws XMLStreamException {
        xmlWriter.writeStartElement("doc");

        xmlWriter.writeStartElement("title");
        xmlWriter.writeCharacters(title);
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("text");
        xmlWriter.writeCharacters(text);
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("incomingLinks");
        xmlWriter.writeCharacters(String.valueOf(annotation.incomingLinks));
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("outgoingLinks");
        xmlWriter.writeCharacters(String.valueOf(annotation.outgoingLinks));
        xmlWriter.writeEndElement();

        xmlWriter.writeEndElement();
    }
}

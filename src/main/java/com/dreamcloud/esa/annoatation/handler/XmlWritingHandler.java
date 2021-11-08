package com.dreamcloud.esa.annoatation.handler;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;

abstract public class XmlWritingHandler extends XmlReadingHandler implements AutoCloseable {
    private XMLStreamWriter xmlWriter;
    private OutputStream outputStream;

    public XmlWritingHandler() {
    }

    public void open(File outputFile) throws IOException, XMLStreamException {
        outputStream = new FileOutputStream(outputFile);
        outputStream = new BufferedOutputStream(outputStream, 4096 * 4);
        outputStream = new BZip2CompressorOutputStream(outputStream);
        this.xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");
    }

    public void writeDocumentBegin(String openTag) throws XMLStreamException, IOException {
        xmlWriter.writeStartDocument();
        xmlWriter.writeStartElement(openTag);
    }

    public void writeDocumentEnd() throws XMLStreamException, IOException {
        xmlWriter.writeEndElement();
        xmlWriter.writeEndDocument();
    }

    public void writeStartElement(String tagName) throws XMLStreamException, IOException {
        xmlWriter.writeStartElement(tagName);
    }

    public void writeEndElement() throws XMLStreamException, IOException {
        xmlWriter.writeEndElement();
    }

    public void writeElement(String tagName, String content) throws XMLStreamException, IOException {
        xmlWriter.writeStartElement(tagName);
        xmlWriter.writeCharacters(content);
        xmlWriter.writeEndElement();
    }

    public void close() throws Exception {
        if (xmlWriter != null) {
            xmlWriter.flush();
            xmlWriter.close();
            outputStream.flush();
            outputStream.close();
            xmlWriter = null;
            outputStream = null;
            super.close();
        }
    }
}

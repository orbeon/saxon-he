package net.sf.saxon.xqj;

import net.sf.saxon.Configuration;
import net.sf.saxon.javax.xml.xquery.XQConnection;
import net.sf.saxon.javax.xml.xquery.XQDataSource;
import net.sf.saxon.javax.xml.xquery.XQException;

import java.io.PrintWriter;
import java.sql.Connection;
import java.util.Properties;

/**
 * Saxon implementation of the XQJ XQDataSource interface. The first action of a client application
 * is to instantiate a SaxonXQDataSource. This is done directly: there is no factory class as with JAXP.
 * An application that does not want compile-time references to the Saxon XQJ implementation can instantiate
 * this class dynamically using the reflection API (class.newInstance()).
 * <p>
 * For full Javadoc descriptions of the public methods, see the XQJ specification.
 */
public class SaxonXQDataSource implements XQDataSource {

    private Configuration config;
    private PrintWriter logger;

    /**
     * Create a SaxonXQDataSource using a default configuration.
     * A schema-aware configuration will be created if Saxon-SA can be loaded from the
     * classpath; otherwise a basic (non-schema-aware) configuration is created
     */

    public SaxonXQDataSource() {
        try {
            config = Configuration.makeSchemaAwareConfiguration(null, null);
        } catch (RuntimeException err) {
            config = new Configuration();
        }
    }

    /**
     * Create a Saxon XQDataSource with a specific configuration
     * @param config The Saxon configuration to be used
     */

    public SaxonXQDataSource(Configuration config) {
        this.config = config;
    }

    /**
     * Get the configuration in use. Changes made to this configuration will affect this
     * data source and XQJ connections created from it (either before or afterwards).
     * @return the configuration in use.
     */

    public Configuration getConfiguration() {
        return config;
    }

    public XQConnection getConnection() throws XQException {
        return new SaxonXQConnection(this);
    }

    /**
     * Get a connection based on an underlying JDBC connection
     * @param con the JDBC connection
     * @return a connection based on an underlying JDBC connection
     * @throws XQException The Saxon implementation of this method always throws
     * an XQException, indicating that Saxon does not support connection to a JDBC data source.
     */

    public XQConnection getConnection(Connection con) throws XQException {
        throw new XQException("Saxon cannot connect to a SQL data source");
    }

    /**
     * Get a connection, by supplying a username and password. The Saxon implementation of this is equivalent
     * to the default constructor: the username and password are ignored.
     * @param username
     * @param password
     * @return a connection
     * @throws XQException
     */

    public XQConnection getConnection(String username, String password) throws XQException {
        return getConnection();
    }

    public int getLoginTimeout() {
        return 0;
    }

    public PrintWriter getLogWriter() {
        return logger;
    }

    public String getProperty(String name) throws XQException {
        throw new XQException("Property " + name + " is not recognized");
    }

    public String[] getSupportedPropertyNames() {
        return new String[] {};
    }

    public void setLoginTimeout(int seconds) throws XQException {
        // no-op
    }

    public void setLogWriter(PrintWriter out) throws XQException {
        logger = out;
    }

    public void setProperties(Properties props) throws XQException {
        if (!props.isEmpty()) {
            throw new XQException("Property " + props.keys().nextElement() + " is not recognized");
        }
    }

    public void setProperty(String name, String value) throws XQException {
        throw new XQException("Property " + name + " is not recognized");
    }

//    public static void main(String[] args) throws Exception {
//                XQConnection conn = new SaxonXQDataSource().getConnection();
//                DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
//                    System.err.println("Using DocumentBuilderFactory " + dfactory.getClass());
//
//                dfactory.setNamespaceAware(true);
//
//                DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
//                    System.err.println("Using DocumentBuilder " + docBuilder.getClass());
//
//
//                Document doc = docBuilder.parse(
//                              new InputSource(new File("data/books.xml").toURL().toString()));
////                File inputFile = new File("data/books.xml");
////                if (!inputFile.exists()) {
////                    System.out.println("*** Current directory must be the samples directory");
////                    return;
////                }
////                StreamSource source = new StreamSource(inputFile);
////                URI schemaURI = inputFile.toURI().resolve("books.xsd");
////                XQItemType booksElement = conn.createSchemaElementType(
////                        new QName("", "BOOKLIST"),
////                        XQItemType.XQITEMKIND_ELEMENT,
////                        schemaURI);
////                XQItemType booksDocument = conn.createDocumentElementType(booksElement);
////                XQItem input = conn.createItemFromDocument(source, booksDocument);
//                //org.w3c.dom.Document w3cdoc =.....;
//                ArrayList items = new ArrayList();
//                items.add(new Integer(1));
//                items.add(doc);
//                items.add(new Integer(2));
//                XQSequence result = conn.createSequence(items.iterator());
//
//                if (result != null) {
//                    Properties pro = new Properties();
//                    pro.setProperty("method", "xml");
//                    pro.setProperty("indent", "yes");
//
//                    BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream("c:/temp/test.out"));
//                    result.writeSequence(fos, pro);
//                    fos.flush();
//                    fos.close();
//                }
//            
//    }
}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s):
//
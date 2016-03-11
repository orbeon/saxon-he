package net.sf.saxon.resource;


import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.Resource;
import net.sf.saxon.lib.ResourceFactory;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Base64BinaryValue;

import java.io.*;
import java.net.URLConnection;


public class BinaryResource implements Resource {

    private String href = null;
    private String contentType = null;
    private byte[] data;
    private URLConnection connection = null;
    private File file = null;
    private InputStream inputStream = null;

    public static final ResourceFactory FACTORY = new ResourceFactory() {
        public Resource makeResource(Configuration config, String resourceURI, String contentType, AbstractResourceCollection.InputDetails details) throws XPathException {
            return new BinaryResource(resourceURI, contentType, details.inputStream);
        }
    };

    public BinaryResource(String href, String contentType, InputStream in) {
        this.contentType = contentType;
        this.href = href;
        this.inputStream = in;
    }


    public void setData(byte [] data){
        this.data = data;
    }

    public String getResourceURI() {
        return href;
    }

   private byte [] readBinaryFromConn(String href, URLConnection con) throws XPathException {
        InputStream raw = null;
       this.connection = con;
        try {
            raw = connection.getInputStream();

            int contentLength = connection.getContentLength();
            InputStream in = new BufferedInputStream(raw);
            byte[] data = new byte[contentLength];
            int bytesRead = 0;
            int offset = 0;
            while (offset < contentLength) {
                bytesRead = in.read(data, offset, data.length - offset);
                if (bytesRead == -1)
                    break;
                offset += bytesRead;
            }
            in.close();

            if (offset != contentLength) {
                throw new XPathException("Only read " + offset + " bytes; Expected " + contentLength + " bytes");
            }
            return data;
        } catch (IOException e) {
            throw new XPathException(e);
        }
    }

    public static byte[] readBinaryFromStream(InputStream in, String path) throws XPathException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        try {
            while ((nRead = in.read(data, 0, data.length)) != -1) {
              buffer.write(data, 0, nRead);
            }
             buffer.flush();
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new XPathException("Failed to read: " + path +" " + e);
        }



    }

    public static byte[] readBinary(File file, String path) throws XPathException {
        try {
            File sFile = file;

            long fileLength = sFile.length();

            byte[] content = new byte[(int) fileLength];
            FileInputStream in = new FileInputStream(sFile);

            //TODO: error if file is longer than int
            int bytes = in.read(content, 0, (int) fileLength);
            if (bytes != content.length) {
                in.close();
                throw new XPathException("Number of bytes read does not match file length");
            }
            in.close(); // ensure input stream is closed

            return content;
        } catch (IOException e) {
            throw new XPathException("Failed to read file " + path, e);
        }


    }

    public Item getItem(XPathContext context) throws XPathException {
        if (data != null) {
            return new Base64BinaryValue(data);
        }
        if(file != null) {
            data = readBinary(file, href);
            return new Base64BinaryValue(data);
        } else if(connection != null) {
           data = readBinaryFromConn(href, connection);
            return new Base64BinaryValue(data);
        } else if(inputStream != null) {
            data = readBinaryFromStream(inputStream, href);
            return new Base64BinaryValue(data);
        }
        return null;

    }

    public String getContentType() {
        return contentType;
    }


}

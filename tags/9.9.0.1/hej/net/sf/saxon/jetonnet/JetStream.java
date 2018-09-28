package net.sf.saxon.jetonnet;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ParseOptions;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.option.cpp.SaxonCAPI;
import net.sf.saxon.pull.PullSource;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by ond1 on 06/03/2018.
 */
public class JetStream extends InputStream {
    private Object stream;
    private String baseURI;
    long currentOffset;
    long markedOffset = 0;


    /**
     * Create a DocumentBuilder. This is a protected constructor. Users should construct a DocumentBuilder
     * by calling the factory method {@link Processor#newDocumentBuilder()}.
     */
    public JetStream() {

    }

    public JetStream(Object stream, String baseUri) {
        this.stream = stream;
        this.baseURI = baseUri;
    }

    public Object getPointerObject() {
        return stream;
    }

    public native int Readc();

    public native int ReadArray(byte b[], int off, int len);

    public native boolean get_CanSeek();

    public native long Seek(long l, int seekOrigin);

    public native void Close();

    @Override
    public int read() throws IOException {
        int i = Readc();
        if (i != -1) {
            currentOffset++;
            return i;
        } else {
            return -1;
        }
    }

    private int tempLen;
    private int tempOff;
    private byte [] tempB;

    public int getLength(){
        return tempLen;
    }

    public int getOffset(){
        return tempOff;
    }

    public byte [] getTempB(){
        return tempB;
    }

    public void setTempB(byte [] b){
        tempB = b;
    }

    public int read(byte b[], int off, int len) throws IOException {
       // System.err.println("Java - ReadArrayi off="+off + ", len= "+len);
        if(len == 0) {
            return 0;

        }
        setTempB(new byte[len]);
        tempLen = len;
        int c =  ReadArray(b,off,len);


        for(int i=0;i<c;i++ ){
            b[off+i] = tempB[i];

        }


        /*read();

               if (c == -1) {
                   return -1;
               }
               b[off] = (byte)c;

               int i = 0;
               try {
                   for (; i < len ; i++) {
                       c = ReadArray(b,off,len);
                       if (c == -1) {
                           break;
                       }
                       b[off + i] = (byte)c;
                   }
               } catch (IOException ee) {
               }    */
               return c;

       /* tempLen = off;
        tempLen = len;
        tempB = b;
        int i = ReadArray(b, off, len);
        for(int ii=0; ii<ii;ii++){
            b[off+ii] = tempB[ii];
        }
        System.err.println("Java - ReadArray - after copy i="+i + ", currentOffset="+currentOffset);
        System.err.println("Java - ReadArray - b[0]="+b[0]);
        if (i > 0) {
            currentOffset += i;
            return i;
        } else {
            // EOF returns 0 in .NET, -1 in Java
            return -1;
        }   */
    }


    public boolean markSupported() {
        return get_CanSeek();
    }


    public synchronized void mark(int readlimit) {
        markedOffset = currentOffset;
    }


    public synchronized void reset() throws IOException {
        currentOffset = markedOffset;
        Seek(markedOffset, 0);
    }

    public void close() throws IOException {
        Close();
    }


    public static void BuildStreamV(Processor processor, Object stream, String baseUri) throws Exception {
        System.err.println("Java - JetStream.BuildStreamv - BaseUri = " + baseUri);
        if (baseUri != null) {
            System.err.println("Java - JetStream.BuildStreamv - BaseUri = " + baseUri);
        } else {
            System.err.println("Java - JetStream.BuildStreamv - baseUris is null");

        }
        if (stream != null) {
            System.err.println("Java - JetStream.BuildStreamv - stream");
        } else {
            System.err.println("Java - JetStream.BuildStreamv - stream is null");

        }
        if (processor == null) {
            System.err.println("Java - JetStream.BuildStreamv - processor is null");

        }

    }


    public static String BuildStreamString(Processor processor, Object stream, String baseUri) throws Exception {
        if (baseUri != null) {
            System.err.println("Java - JetStream.BuildStream - BaseUri = " + baseUri);
        } else {
            System.err.println("Java - JetStream.BuildStream - baseUris is null");
            return null;
        }
        if (stream != null) {
            System.err.println("Java - JetStream.BuildStream - stream");
        } else {
            System.err.println("Java - JetStream.BuildStream - stream is null");
            return null;
        }
        if (processor == null) {
            System.err.println("Java - JetStream.BuildStream - processor is null");
            return null;
        }
        return "String value returned from BuildStream";
    }

    public static XdmNode BuildStream(Processor processor, Object stream, String baseUri) throws Exception {

        Source source = null;
        ParseOptions options = null;
        Configuration config = null;
        if (baseUri != null) {
            System.err.println("Java - JetStream.BuildStream - BaseUri = " + baseUri);
        } else {
            System.err.println("Java - JetStream.BuildStream - baseUris is null");
            return null;
        }
        if (processor == null) {
            System.err.println("Java - JetStream.BuildStream - processor is null");
            return null;
        }
        try {
            config = processor.getUnderlyingConfiguration();

            options = new ParseOptions(config.getParseOptions());

            source = new StreamSource(new JetStream(stream, baseUri));
            if (source == null) {
                System.err.println("Java - source is null");
            } else {
                source.setSystemId(baseUri);
            }


            NodeInfo doc = config.buildDocumentTree(source, options).getRootNode();
            return (XdmNode) XdmValue.wrap(doc);
        } catch (Exception ex) {
            ex.printStackTrace();
            //throw new XPathException(ex);
            return null;
        }


    }


    /*public static void main(String [] args) throws Exception {

        XdmNode node1 = JetStream.BuildStream(new Processor(true), null, "http://example.com");
    }   */
}

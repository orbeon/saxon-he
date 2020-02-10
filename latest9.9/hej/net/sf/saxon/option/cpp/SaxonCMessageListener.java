package net.sf.saxon.option.cpp;

import net.sf.saxon.s9api.MessageListener;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import javax.xml.transform.SourceLocator;
import java.io.*;

public class SaxonCMessageListener implements MessageListener {


    int mode = 0;
    String messageFilename = null;
    File file = null;

    public SaxonCMessageListener(String cwd, String mode) throws SaxonApiException {
        if (mode.equals("-:on")) {
            this.mode = 1;

        } else if (mode.equals("-:off")) {
            this.mode = 0;
        } else {
            this.mode = 2;
            messageFilename = cwd + mode;
            file = SaxonCAPI.resolveFile(cwd, messageFilename);

        }
    }

    public void message(XdmNode content, boolean terminate, SourceLocator locator) {

        if (mode == 1) {
            System.err.println(content.getStringValue());
        } else if (mode == 2) {

            try (FileWriter fw = new FileWriter(file, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {
                out.println(content.getStringValue());

            } catch (IOException e) {
                System.err.println("Error could not write message to file:" + messageFilename + " xsl:mesage=" + content.getStringValue());
            }

        }


        //xslMessages.add(content);
    }

}

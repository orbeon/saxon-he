package net.sf.saxon.trans;


import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.style.StylesheetPackage;
import javax.xml.transform.Source;

public interface IPackageLoader {

    StylesheetPackage loadPackageDoc(NodeInfo doc) throws XPathException;

    StylesheetPackage loadPackage(Source source) throws XPathException;
}

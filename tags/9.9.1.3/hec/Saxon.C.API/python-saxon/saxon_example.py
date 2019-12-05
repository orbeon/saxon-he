import saxonc

proc = saxonc.PySaxonProcessor(license=False)
print("Test Python")
print(proc.version())
xdmAtomicval = proc.makeBooleanValue(4)

xsltproc = proc.newXsltProcessor()
outputi = xsltproc.transformToString(sourcefile="cat.xml", stylesheetfile="test.xsl")
print(outputi)
document = proc.parseXml(xmlText="<out><person>text1</person><person>text2</person><person>text3</person></out>")
xsltproc.setSource(node=document)
xsltproc.compileStylesheet(stylesheetText="<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>       <xsl:param name='values' select='(2,3,4)' /><xsl:output method='xml' indent='yes' /><xsl:template match='*'><output><xsl:value-of select='//person[1]'/><xsl:for-each select='$values' ><out><xsl:value-of select='. * 3'/></out></xsl:for-each></output></xsl:template></xsl:stylesheet>")
#xsltproc.setJustInTimeCompilation(True) #Available in Saxon 9.9 library

output2 = xsltproc.transformToString()
print(output2)
proc.release()

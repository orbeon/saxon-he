import saxonc

proc = saxonc.PySaxonProcessor(license=False)
print("Test Python")
print(proc.version())
xsltproc = proc.newXsltProcessor()
outputi = xsltproc.transformFileToString(sourcefile="cat.xml", stylesheetfile="test.xsl")
print(outputi)
xsltproc.setSourceFromFile("cat.xml")
xsltproc.compileFromFile("test.xsl")
output2 = xsltproc.transformToString()
print(output2)
proc.release()

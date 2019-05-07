import saxonc

proc = saxonc.PySaxonProcessor(False)
print(proc.version())
xsltproc = proc.newXsltProcessor()
outputi = xsltproc.transformFileToString("cat.xml", "test.xsl")
print(outputi)
proc.release()

import saxonc

proc = saxonc.PySaxonProcessor(False)
print(proc.version())
xsltproc = proc.newXsltProcessor()
proc.release()

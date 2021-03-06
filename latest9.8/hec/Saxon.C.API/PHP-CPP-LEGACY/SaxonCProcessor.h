#ifndef SAXONCPROC_H 
#define SAXONCPROC_H

#include "SaxonCGlue.h"





/*! <code>sxnc_processor</code>. This struct is used to capture the Java processor objects created in C for XSLT, XQuery and XPath 
 * <p/>
 */
typedef struct {
	jobject xqueryProc;
	jobject xsltProc;
	jobject xpathProc;
} sxnc_processor;

EXTERN_C
/*
 * Get the Saxon version 
 */
const char * version(sxnc_environment environ) ;

const char * getProductVariantAndVersion(sxnc_environment environ);

void initSaxonc(sxnc_environment ** environ, sxnc_processor ** proc, sxnc_parameter **param, sxnc_property ** prop,int cap, int propCap);

void freeSaxonc(sxnc_environment ** environ, sxnc_processor ** proc, sxnc_parameter **param, sxnc_property ** prop);

void xsltSaveResultToFile(sxnc_environment environ, sxnc_processor ** proc, char * cwd, char * source, char* stylesheet, char* outputfile, sxnc_parameter *parameters, sxnc_property * properties, int parLen, int propLen);

const char * xsltApplyStylesheet(sxnc_environment environ, sxnc_processor ** proc, char * cwd, const char * source, const char* stylesheet, sxnc_parameter *parameters, sxnc_property * properties, int parLen, int propLen);

void executeQueryToFile(sxnc_environment environ, sxnc_processor ** proc, char * cwd, char* outputfile, sxnc_parameter *parameters, sxnc_property * properties, int parLen, int propLen);

const char * executeQueryToString(sxnc_environment environ, sxnc_processor ** proc, char * cwd, sxnc_parameter *parameters, sxnc_property * properties, int parLen, int propLen);

EXTERN_C_END

#endif 

#include <boost/python.hpp>
#include "../SaxonProcessor.h"
using namespace boost::python;

BOOST_PYTHON_MODULE(saxonc)
{
    class_<SaxonProcessor>("SaxonProcessor")
        .def("version", &SaxonProcessor::version)
    ;
}
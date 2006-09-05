using System;
using System.Reflection;
using net.sf.saxon;
using net.sf.saxon.dotnet;

namespace Saxon.Cmd {
    ///<summary>
    ///This class provides the command line interface for the .NET executable
    ///</summary>

    public class DotNetQuery : Query {

        // This class is never instantiated
         
        private DotNetQuery() {
        }

        ///<summary>
        /// Set the configuration in the TransformerFactory. This is designed to be
        /// overridden in a subclass
        ///</summary>

        protected override Configuration makeConfiguration(bool schemaAware) {
            if (schemaAware) {
                try {
                    // try to load the saxon8sa.dll assembly
                    Assembly asm = Assembly.Load("saxon8sa");
                } catch (Exception) {
                    quit("Cannot load Saxon-SA software (assembly saxon8sa.dll)", 2);
                }
                config = Configuration.makeSchemaAwareConfiguration(null);
            } else {
                config = new Configuration();
            }
            return config;
        }

        ///<summary>
        /// Entry point for use from the .NET command line
        /// <param name="args">command line arguments</param>
        /// </summary>

        public static void Main(String[] args) {
            new DotNetQuery().doQuery(args, "Query");
        }
    }

}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

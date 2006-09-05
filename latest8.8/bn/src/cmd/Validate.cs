using System;
using System.Text;
using net.sf.saxon.dotnet;

namespace Saxon.Cmd {

    /// <summary>
    /// This class provides the command line interface for the .NET executable
    /// </summary>

    public class DotNetValidate : com.saxonica.Validate {

        /// <summary>
        /// Private constructor, ensuring the class can only be used via its "main" method.
        /// </summary>

        private DotNetValidate() {
        }

        /// <summary>
        /// Create the configuration. This method is intended to be overridden in a subclass
        /// </summary>

        protected override void setConfiguration() {
            base.setConfiguration();
            //config.setPlatform(DotNetPlatform.getInstance());
        }

        /// <summary>
        /// Entry point for use from the .NET command line
        /// <param name="args">command line arguments</param>
        /// </summary>

        public static void Main(String[] args) {
            new DotNetValidate().doValidate(args, "Validate");
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

package net.sf.saxon.style;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Int64Value;

import javax.xml.xpath.XPath;
import java.util.ArrayList;

/**
 * A class to represent an XSLT package version such as 1.12.5 or 3.0-alpha
 *
 * <p>This implements the semantics given in
 * <a href="http://www.w3.org/TR/xslt-30/#package-versions">http://www.w3.org/TR/xslt-30/#package-versions</a>,
 * where a series of dot-separated integers may be followed optionally by '-'NCName</p>
 *
 * Created by jwl on 08/10/2014.
 */
public class PackageVersion implements Comparable {
    public ArrayList<Integer> parts;
    public String suffix;

    /**
     *
     */
    public static PackageVersion ZERO = new PackageVersion(new int[]{0});
    public static PackageVersion ONE_ZERO = new PackageVersion(new int[]{1,0});
    public static PackageVersion MAX_VALUE = new PackageVersion(new int[]{Integer.MAX_VALUE});


    /**
     * Return a package version defined by a fixed sequence of int values, which implies no suffix
     *
     * @param values the sequence of integer components
     */
    public PackageVersion(int[] values) {
        parts = new ArrayList<Integer>();
        for (int i = 0; i < values.length; i++) {
            parts.add(new Integer(values[i]));
        }
        trimTrailingZeroes();
    }

    /**
     * Remove any trailing zero components from the package version
     */
    private void trimTrailingZeroes() {
        for (int i = parts.size() - 1; i > 0; i--) {
            if (parts.get(i).intValue() != 0) {
                return;
            } else parts.remove(i);
        }
    }

    /**
     * Generate a package version from a string description
     *
     * @param s The input string describing the package version according to the grammar given at:
     *          <a href="http://www.w3.org/TR/xslt-30/#package-versions">http://www.w3.org/TR/xslt-30/#package-versions</a>
     * @throws net.sf.saxon.trans.XPathException
     */
    public PackageVersion(String s) throws XPathException {
        parts = new ArrayList<Integer>();
        String original = s;
        if (s.contains("-")) {
            int i = s.indexOf('-');
            suffix = s.substring(i + 1);
            if (!isNCName(suffix)) {
                throw new XPathException("Illegal NCName as package-version NamePart: " + original, "XTSE0020");
            }
            s = s.substring(0, i);
        }
        if(s.equals("")) {
            throw new XPathException("No numeric component of package-version: " + original, "XTSE0020");
        }
        for (String p : s.trim().split("\\.")) {
            try {
                parts.add(new Integer(p));
            } catch (NumberFormatException e) {
                throw new XPathException("Error in package-version: " + e.getMessage(), "XTSE0020");
            }
        }
        trimTrailingZeroes();
    }

    public boolean isNCName(String s) {
        return s.matches("^[A-Z_a-z\\u00C0\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02ff\\u0370-\\u037d"
                + "\\u037f-\\u1fff\\u200c\\u200d\\u2070-\\u218f\\u2c00-\\u2fef\\u3001-\\ud7ff"
                + "\\uf900-\\ufdcf\\ufdf0-\\ufffd\\x10000-\\xEFFFF]"
                + "[A-Z_a-z\\u00C0\\u00D6\\u00D8-\\u00F6"
                + "\\u00F8-\\u02ff\\u0370-\\u037d\\u037f-\\u1fff\\u200c\\u200d\\u2070-\\u218f"
                + "\\u2c00-\\u2fef\\u3001-\\udfff\\uf900-\\ufdcf\\ufdf0-\\ufffd\\-\\.0-9"
                + "\\u00b7\\u0300-\\u036f\\u203f-\\u2040]*");
    }
   /* public PackageVersion copy() throws XPathException {
        PackageVersion n = new PackageVersion();
        n.parts = this.parts;
        return n;
    }*/

    @Override
    public boolean equals(Object o) {
        if (o instanceof PackageVersion) {
            PackageVersion p = (PackageVersion) o;
            if (parts.equals(p.parts)) {
                if (suffix != null) {
                    return suffix.equals(p.suffix);
                } else {
                    return p.suffix == null;
                }
            }
        }
        return false;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof PackageVersion) {
            PackageVersion pv = ((PackageVersion) o);
            ArrayList<Integer> p = pv.parts;
            int extent = parts.size() - p.size();
            int len = Math.min(parts.size(), p.size());
            for (int i = 0; i < len; i++) {
                int comp = parts.get(i).compareTo(p.get(i));
                if (comp != 0) {
                    return comp;
                }
            }
            if (extent == 0 && suffix != null) {
                if (pv.suffix == null) {
                    return 1;
                } else {
                    return suffix.compareTo(pv.suffix);
                }
            }
            return extent;
        } else {
            throw new ClassCastException("Cannot compare a PackageVersion against " + o.getClass().toString());
        }
    }

    public String getStringValue() {
        String result = "";
        for(Integer i : parts) {
            result += "." + i;
        }
        if(parts.size() > 0) {
            result = result.substring(1);
        }
        if(suffix != null) {
            result += "-" + suffix;
        }
        return result;
    }

    /**
     * Tests whether this package version is a prefix (i.e. shares all its components in order)
     * of another package version, and thus this version.* should match it.
     *
     * @param v The version to be checked that it is equal, or 'extends'  this version
     * @return true if this is a prefix, false if not.
     */
    public boolean isPrefix(PackageVersion v) {
        if (v.parts.size() >= parts.size()) {
            for (int i = 0; i < parts.size(); i++) {
                int comp = parts.get(i).compareTo(v.parts.get(i));
                if (!parts.get(i).equals(v.parts.get(i))) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }
}

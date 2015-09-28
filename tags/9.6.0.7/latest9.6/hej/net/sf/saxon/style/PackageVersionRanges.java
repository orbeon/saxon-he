package net.sf.saxon.style;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.trans.XPathException;

import java.util.ArrayList;

/**
 * A class to handle a set of package version ranges
 * <p>This implements the semantics given in
 * <a href="http://www.w3.org/TR/xslt-30/#package-versions">http://www.w3.org/TR/xslt-30/#package-versions</a>,
 * where a comma-separated sequence of package-version ranges are declared as one of:</p>
 * <ul>
 * <li>'*' - any version</li>
 * <li><code>package-version</code> - an exact match</li>
 * <li><code>package-version'.*'</code> - any package whose version starts with the given prefix</li>
 * <li><code>package-version'+'</code> - any package whose version orders equal or later than the given version</li>
 * <li><code>'to 'package-version</code> - any package whose version orders equal or earlier than the given version</li>
 * <li><code>package-version' to 'package-version</code> - any package whose version orders equal to or between the given versions</li>
 * </ul>
 * Created by jwl on 08/10/2014.
 */
public class PackageVersionRanges {
    ArrayList<PackageVersionRange> ranges;

    private class PackageVersionRange {
        PackageVersion low;
        PackageVersion high;
        boolean all = false;
        boolean prefix = false;

        PackageVersionRange(String s) throws XPathException {
            if ("*".equals(s)) {
                all = true;
            } else if (s.endsWith("+")) {
                low = new PackageVersion(s.replace("+", ""));
                high = PackageVersion.MAX_VALUE;
            } else if (s.endsWith(".*")) {
                prefix = true;
                low = new PackageVersion(s.replace(".*", ""));
            } else if (s.matches(".*\\s*to\\s+.*")) {
                String range[] = s.split("\\s*to\\s+");
                if (range.length > 2) {
                    throw new XPathException("Invalid version range:" + s, "XTSE0020");
                }
                low = range[0].equals("") ? PackageVersion.ZERO : new PackageVersion(range[0]);
                high = new PackageVersion(range[1]);
            } else {
                low = new PackageVersion(s);
                high = low;
            }
        }

        boolean contains(PackageVersion v) {
            if (all) {
                return true;
            } else if (prefix) {
                return low.isPrefix(v);
            } else {
                return low.compareTo(v) <= 0 && v.compareTo(high) <= 0;
            }
        }

    }

    /**
     * Generate a set of package version ranges
     *
     * @param s Input string describing the ranges in the grammar described in
     *          <a href="http://www.w3.org/TR/xslt-30/#package-versions">http://www.w3.org/TR/xslt-30/#package-versions</a>
     * @throws net.sf.saxon.trans.XPathException
     */
    public PackageVersionRanges(String s) throws XPathException {
        ranges = new ArrayList<PackageVersionRange>();
        for (String p : s.trim().split("\\s*,\\s*")) {
            ranges.add(new PackageVersionRange(p));
        }
    }

    /**
     * Test whether a given package version lies within any of the ranges described in this PackageVersionRanges
     * @param version The version to be checked
     * @return  true if the version is contained in any of the ranges, false otherwise
     */
    public boolean contains(PackageVersion version) {
        for (PackageVersionRange r : ranges) {
            if (r.contains(version)) {
                return true;
            }
        }
        return false;
    }
}

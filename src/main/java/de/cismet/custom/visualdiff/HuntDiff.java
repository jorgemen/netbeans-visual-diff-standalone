/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.custom.visualdiff;
/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

import org.netbeans.api.diff.Difference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class HuntDiff {

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new HuntDiff object.
     */
    private HuntDiff() {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   lines1            array of lines from the first source
     * @param   lines2            array of lines from the second source
     * @param   ignoreWhitespace  true to ignore leading and trailing whitespace when computing diff, false to also find
     *                            differences in whitespace
     *
     * @return  computed diff
     */
    public static Difference[] diff(String[] lines1, String[] lines2, final boolean ignoreWhitespace) {
        final int m = lines1.length;
        final int n = lines2.length;
        final String[] lines1_original = lines1;
        final String[] lines2_original = lines2;

        if (ignoreWhitespace) {
            lines1 = new String[lines1_original.length];
            lines2 = new String[lines2_original.length];

            for (int i = 0; i < lines1_original.length; i++) {
                lines1[i] = lines1_original[i].trim();
            }

            for (int i = 0; i < lines2_original.length; i++) {
                lines2[i] = lines2_original[i].trim();
            }
        }

        Line[] l2s = new Line[n + 1];
        // In l2s we have sorted lines of the second file <1, n>
        for (int i = 1; i <= n; i++) {
            l2s[i] = new Line(i, lines2[i - 1]);
        }
        Arrays.sort(l2s, 1, n + 1, new Comparator<Line>() {

                @Override
                public int compare(final Line l1, final Line l2) {
                    return l1.line.compareTo(l2.line);
                }

                @Override
                public boolean equals(final Object obj) {
                    return obj == this;
                }
            });

        final int[] equivalenceLines = new int[n + 1];
        final boolean[] equivalence = new boolean[n + 1];
        for (int i = 1; i <= n; i++) {
            final Line l = l2s[i];
            equivalenceLines[i] = l.lineNo;
            equivalence[i] = (i == n) || !l.line.equals(l2s[i + 1].line); // ((Line) l2s.get(i)).line);
        }
        equivalenceLines[0] = 0;
        equivalence[0] = true;

        final int[] equivalenceAssoc = new int[m + 1];
        for (int i = 1; i <= m; i++) {
            equivalenceAssoc[i] = findAssoc(lines1[i - 1], l2s, equivalence);
        }

        l2s = null;
        final Candidate[] K = new Candidate[Math.min(m, n) + 2];
        K[0] = new Candidate(0, 0, null);
        K[1] = new Candidate(m + 1, n + 1, null);
        int k = 0;
        for (int i = 1; i <= m; i++) {
            if (equivalenceAssoc[i] != 0) {
                k = merge(K, k, i, equivalenceLines, equivalence, equivalenceAssoc[i]);
            }
        }
        final int[] J = new int[m + 2]; // Initialized with zeros

        Candidate c = K[k];
        while (c != null) {
            J[c.a] = c.b;
            c = c.c;
        }

        final List<Difference> differences = getDifferences(J, lines1_original, lines2_original);
        cleanup(differences);
        return differences.toArray(new Difference[0]);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   line1        DOCUMENT ME!
     * @param   l2s          DOCUMENT ME!
     * @param   equivalence  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static int findAssoc(final String line1, final Line[] l2s, final boolean[] equivalence) {
        int idx = binarySearch(l2s, line1, 1, l2s.length - 1);

        if (idx < 1) {
            return 0;
        } else {
            int lastGoodIdx = 0;

            for (; (idx >= 1) && l2s[idx].line.equals(line1); idx--) {
                if (equivalence[idx - 1]) {
                    lastGoodIdx = idx;
                }
            }

            return lastGoodIdx;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   L     DOCUMENT ME!
     * @param   key   DOCUMENT ME!
     * @param   low   DOCUMENT ME!
     * @param   high  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static int binarySearch(final Line[] L, final String key, int low, int high) {
        while (low <= high) {
            final int mid = (low + high) >> 1;
            final String midVal = L[mid].line;
            final int comparison = midVal.compareTo(key);

            if (comparison < 0) {
                low = mid + 1;
            } else if (comparison > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }

        return -(low + 1);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   K     DOCUMENT ME!
     * @param   key   DOCUMENT ME!
     * @param   low   DOCUMENT ME!
     * @param   high  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static int binarySearch(final Candidate[] K, final int key, int low, int high) {
        while (low <= high) {
            final int mid = (low + high) >> 1;
            final int midVal = K[mid].b;

            if (midVal < key) {
                low = mid + 1;
            } else if (midVal > key) {
                high = mid - 1;
            } else {
                return mid;
            }
        }

        return -(low + 1);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   K                DOCUMENT ME!
     * @param   k                DOCUMENT ME!
     * @param   i                DOCUMENT ME!
     * @param   equvalenceLines  DOCUMENT ME!
     * @param   equivalence      DOCUMENT ME!
     * @param   p                DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static int merge(final Candidate[] K,
            int k,
            final int i,
            final int[] equvalenceLines,
            final boolean[] equivalence,
            int p) {
        int r = 0;
        Candidate c = K[0];

        do {
            final int j = equvalenceLines[p];
            int s = binarySearch(K, j, r, k);
            if (s >= 0) {
                // j was found in K[]
                s = k + 1;
            } else {
                s = -s - 2;
                if ((s < r) || (s > k)) {
                    s = k + 1;
                }
            }

            if (s <= k) {
                if (K[s + 1].b > j) {
                    final Candidate newc = new Candidate(i, j, K[s]);
                    K[r] = c;
                    r = s + 1;
                    c = newc;
                }

                if (s == k) {
                    K[k + 2] = K[k + 1];
                    k++;
                    break;
                }
            }

            if (equivalence[p] == true) {
                break;
            } else {
                p++;
            }
        } while (true);

        K[r] = c;

        return k;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   J       DOCUMENT ME!
     * @param   lines1  DOCUMENT ME!
     * @param   lines2  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static List<Difference> getDifferences(final int[] J, final String[] lines1, final String[] lines2) {
        final List<Difference> differences = new ArrayList<Difference>();
        final int n = lines1.length;
        final int m = lines2.length;
        int start1 = 1;
        int start2 = 1;

        do {
            while ((start1 <= n) && (J[start1] == start2)) {
                start1++;
                start2++;
            }

            if (start1 > n) {
                break;
            }

            if (J[start1] < start2) { // There's something extra in the first file
                int end1 = start1 + 1;
                final StringBuffer deletedText = new StringBuffer();
                deletedText.append(lines1[start1 - 1]).append('\n');

                while ((end1 <= n) && (J[end1] < start2)) {
                    final String line = lines1[end1 - 1];
                    deletedText.append(line).append('\n');
                    end1++;
                }

                differences.add(new Difference(
                        Difference.DELETE,
                        start1,
                        end1
                                - 1,
                        start2
                                - 1,
                        0,
                        deletedText.toString(),
                        null));
                start1 = end1;
            } else { // There's something extra in the second file
                final int end2 = J[start1];
                final StringBuffer addedText = new StringBuffer();

                for (int i = start2; i < end2; i++) {
                    final String line = lines2[i - 1];
                    addedText.append(line).append('\n');
                }

                differences.add(new Difference(
                        Difference.ADD,
                        (start1 - 1),
                        0,
                        start2,
                        (end2 - 1),
                        null,
                        addedText.toString()));
                start2 = end2;
            }
        } while (start1 <= n);

        if (start2 <= m) { // There's something extra at the end of the second file
            int end2 = start2 + 1;
            final StringBuilder addedText = new StringBuilder();
            addedText.append(lines2[start2 - 1]).append('\n');

            while (end2 <= m) {
                final String line = lines2[end2 - 1];
                addedText.append(line).append('\n');
                end2++;
            }

            differences.add(new Difference(Difference.ADD, n, 0, start2, m, null, addedText.toString()));
        }

        return differences;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  diffs  DOCUMENT ME!
     */
    private static void cleanup(final List<Difference> diffs) {
        Difference last = null;

        for (int i = 0; i < diffs.size(); i++) {
            Difference diff = diffs.get(i);

            if (last != null) {
                if (((diff.getType() == Difference.ADD) && (last.getType() == Difference.DELETE))
                            || ((diff.getType() == Difference.DELETE) && (last.getType() == Difference.ADD))) {
                    Difference add;
                    Difference del;

                    if (Difference.ADD == diff.getType()) {
                        add = diff;
                        del = last;
                    } else {
                        add = last;
                        del = diff;
                    }

                    final int d1f1l1 = add.getFirstStart() - (del.getFirstEnd() - del.getFirstStart());
                    final int d2f1l1 = del.getFirstStart();

                    if (d1f1l1 == d2f1l1) {
                        final Difference newDiff = new Difference(
                                Difference.CHANGE,
                                d1f1l1,
                                del.getFirstEnd(),
                                add.getSecondStart(),
                                add.getSecondEnd(),
                                del.getFirstText(),
                                add.getSecondText());
                        diffs.set(i - 1, newDiff);
                        diffs.remove(i);
                        i--;
                        diff = newDiff;
                    }
                }
            }

            last = diff;
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static class Line {

        //~ Instance fields ----------------------------------------------------

        public int lineNo;
        public String line;
        public int hash;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new Line object.
         *
         * @param  lineNo  DOCUMENT ME!
         * @param  line    DOCUMENT ME!
         */
        public Line(final int lineNo, final String line) {
            this.lineNo = lineNo;
            this.line = line;
            this.hash = line.hashCode();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static class Candidate {

        //~ Instance fields ----------------------------------------------------

        private int a;
        private int b;
        private Candidate c;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new Candidate object.
         *
         * @param  a  DOCUMENT ME!
         * @param  b  DOCUMENT ME!
         * @param  c  DOCUMENT ME!
         */
        public Candidate(final int a, final int b, final Candidate c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.gizmo;

import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;

import java.io.*;
import java.util.List;
import java.util.SortedSet;

import static jline.internal.Preconditions.checkNotNull;

/**
 * Handles terminal interaction for Gizmo using the JLine2 library: https://github.com/jline/jline2
 */

public class JLine2Talker implements Talker {

    private ConsoleReader console;
    private Completer completer;

    public JLine2Talker() throws IOException {
        console = new ConsoleReader(System.in, System.out);
        console.setExpandEvents(false);
        console.setHistoryEnabled(true);
    }

    /**
     * Send some output to the user and get some input back
     *
     * @param message output to be sent to the user
     * @return the user's response
     */
    @Override
    public String exchange(String message) {
        try {
            if (message != null && !message.isEmpty()) {
                console.println(message);
            }
            String in = console.readLine("/>");
            if (in == null) {
                System.exit(0);
            }

            return in.trim();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void log(String message) {
        try (FileWriter fw = new FileWriter("/Users/mike/Desktop/log.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
             out.println(message);
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }

    public static class XPathCompleter extends StringsCompleter {

        public XPathCompleter(List<String> candidates) {
            super(candidates);
        }

        @Override
        public int complete(String buffer, int cursor, List<CharSequence> candidates) {
            // Consider using a filename completer
            int space = buffer.indexOf(' ');
            if (space > 0) {
                String command = buffer.substring(0, space);
                if (command.equals("load") || command.equals("save") || command.equals("transform") ||
                        command.equals("schema")) {
                    FileNameCompleter fnc = new FileNameCompleter() {
                        public int complete(String buffer, final int cursor, final List<CharSequence> candidates) {
                            // buffer can be null
                            checkNotNull(candidates);

                            if (buffer == null) {
                                buffer = "";
                            }

                            if (File.separator.equals("\\")) {
                                buffer = buffer.replace('/', '\\');
                            }

                            String translated = buffer.substring(space+1);

                            File homeDir = getUserHome();

                            // Special character: ~ maps to the user's home directory
                            if (translated.startsWith("~" + separator())) {
                                translated = homeDir.getPath() + translated.substring(1);
                            } else if (translated.startsWith("~")) {
                                translated = homeDir.getParentFile().getAbsolutePath();
                            } else if (!translated.contains(separator())) {
                                String cwd = getUserDir().getAbsolutePath();
                                translated = cwd + separator() + translated;
                            } else if (!(new File(translated).isAbsolute())) {
                                String cwd = getUserDir().getAbsolutePath();
                                translated = cwd + separator() + translated;
                            }

                            File file = new File(translated);
                            final File dir;

                            if (translated.endsWith(separator())) {
                                dir = file;
                            } else {
                                dir = file.getParentFile();
                            }

                            File[] entries = dir == null ? new File[0] : dir.listFiles();

                            int index = matchFiles(buffer, translated, entries, candidates);
                            return index==0 ? space+1 : index;
                        }
                    };
                    return fnc.complete(buffer, cursor, candidates);
                }
            }
            int lastDelimiter = Integer.max(cursor - 1, 0);
            while (lastDelimiter >= 0) {
                char c = buffer.charAt(lastDelimiter);
                if (Character.isAlphabetic((c)) || Character.isDigit(c) || c == '-' || c== '_' || c=='@' || c=='.') {
                    lastDelimiter--;
                } else {
                    break;
                }
            }
            String currentWord = buffer.substring(lastDelimiter+1);

            for (String match : ((SortedSet<String>)getStrings()).tailSet(currentWord)) {
                if (!match.startsWith(currentWord)) {
                    break;
                }
                candidates.add(match);
            }

            return candidates.isEmpty() ? -1 : lastDelimiter+1;
        }
    }


    @Override
    public void setAutoCompletion(List<String> candidates) {
        if (completer != null) {
            console.removeCompleter(completer);
        }
        completer = new XPathCompleter(candidates);
        console.addCompleter(completer);
    }

}


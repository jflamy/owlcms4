/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.technicalofficial;

import java.util.Arrays;

public final class TechnicalOfficialNameParser {

    private TechnicalOfficialNameParser() {
        // utility class
    }

    public static NameParts parse(String value) {
        if (value == null || value.isBlank()) {
            return new NameParts("", "");
        }
        String trimmed = value.trim();
        String[] segments = trimmed.split("[,]+", -1);
        String lastName = segments.length > 0 ? segments[0].trim() : "";
        String firstName = segments.length > 1 ? segments[1].trim() : "";

        if (firstName.isEmpty() && !lastName.isEmpty() && lastName.contains(" ")) {
            String[] words = lastName.split("\\s+");
            if (words.length > 1) {
                firstName = String.join(" ", Arrays.copyOf(words, words.length - 1));
                lastName = words[words.length - 1];
            }
        }

        return new NameParts(lastName, firstName);
    }

    public static final class NameParts {

        private final String lastName;
        private final String firstName;

        public NameParts(String lastName, String firstName) {
            this.lastName = lastName != null ? lastName : "";
            this.firstName = firstName != null ? firstName : "";
        }

        public String getLastName() {
            return this.lastName;
        }

        public String getFirstName() {
            return this.firstName;
        }

    }
}

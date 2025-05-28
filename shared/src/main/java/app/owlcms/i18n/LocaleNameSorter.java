package app.owlcms.i18n;

import java.lang.Character.UnicodeScript;
import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Utility class for sorting locale names according to specific criteria:
 * 1. "English" (and its variants) always comes first.
 * 2. Other Latin-script names are grouped together and sorted alphabetically.
 * 3. Non-Latin-script names are grouped together and sorted alphabetically.
 */
public class LocaleNameSorter {

    // Set of Unicode Scripts considered "Latin" for sorting purposes.
    private static final Set<UnicodeScript> ALLOWED_LATIN_SCRIPTS = new HashSet<>(Arrays.asList(
        UnicodeScript.LATIN,
        UnicodeScript.COMMON,    // Includes digits, basic punctuation, spaces
        UnicodeScript.INHERITED  // Characters that inherit script from surrounding characters
    ));

    /**
     * Determines if a string predominantly uses Latin script characters.
     *
     * @param text The string to check.
     * @return true if the string is Latin-script, false otherwise.
     */
    private static boolean isLatinScript(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        for (int i = 0; i < text.length(); i++) {
            int codePoint = text.codePointAt(i);
            if (Character.isSupplementaryCodePoint(codePoint)) {
                i++;
            }
            UnicodeScript script = UnicodeScript.of(codePoint);
            if (!ALLOWED_LATIN_SCRIPTS.contains(script)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sorts a list of locale names according to the following rules:
     * 1. "English" (and its variants like "English (United States)") always comes first.
     * 2. All other Latin-script names are grouped together and sorted alphabetically
     * (e.g., "French" before "German").
     * 3. All non-Latin-script names are grouped last and sorted alphabetically
     * (e.g., "Arabic" before "Chinese (Simplified)").
     *
     * Sorting within each group (English, other Latin, non-Latin) is done using
     * a consistent {@code Locale.US} collator for predictable alphabetical order.
     *
     * @param localeNames The list of locale names (as Strings) to be sorted.
     * This list will be modified in-place.
     */
    public static void sortLocaleNames(List<String> localeNames) {
        // Use a final Collator instance for consistent alphabetical sorting across all groups.
        final Collator englishNameCollator = Collator.getInstance(Locale.US);

        // Define the custom Comparator with our multi-tiered sorting logic.
        Comparator<String> customLocaleNameComparator = (name1, name2) -> {
            // Rule 1: Prioritize "English" and its variants
            boolean isName1English = name1.startsWith("English");
            boolean isName2English = name2.startsWith("English");

            if (isName1English && !isName2English) {
                return -1; // name1 is English, name2 is not => name1 comes first
            }
            if (!isName1English && isName2English) {
                return 1;  // name1 is not English, name2 is => name2 comes first
            }
            // If both are English variants or neither are, proceed to the next rule.

            // Rule 2: Group Latin-script names before non-Latin-script names
            boolean isName1Latin = isLatinScript(name1);
            boolean isName2Latin = isLatinScript(name2);

            if (isName1Latin && !isName2Latin) {
                return -1; // name1 (Latin) comes before name2 (non-Latin)
            }
            if (!isName1Latin && isName2Latin) {
                return 1;  // name1 (non-Latin) comes after name2 (Latin)
            }

            // Rule 3: Within each group (English variants, other Latin, non-Latin),
            // sort alphabetically using the English Collator.
            return englishNameCollator.compare(name1, name2);
        };

        // Apply the custom sort to the provided list.
        Collections.sort(localeNames, customLocaleNameComparator);
    }
    
    
    /**
     * Sorts a list of {@code Locale} objects according to the following rules,
     * based on their display names as rendered in {@code Locale.US}:
     * 1. Locales whose display name starts with "English" always come first.
     * 2. Other Locales whose display name is Latin-script are grouped together and sorted alphabetically.
     * 3. Locales whose display name is non-Latin-script are grouped last and sorted alphabetically.
     *
     * Sorting within each group (English, other Latin, non-Latin) is done using
     * a consistent {@code Locale.US} collator for predictable alphabetical order of their display names.
     *
     * @param locales The list of {@code Locale} objects to be sorted.
     * This list will be modified in-place.
     */
    public static void sortLocales(List<Locale> locales) {
        // The display locale for rendering names. Crucial for consistent sorting.
        final Locale displayLocale = Locale.US;

        // The collator for alphabetical comparison of the rendered display names.
        final Collator englishNameCollator = Collator.getInstance(displayLocale);

        Comparator<Locale> customLocaleComparator = (loc1, loc2) -> {
            // Get the display names for comparison.
            String name1 = loc1.getDisplayName(displayLocale);
            String name2 = loc2.getDisplayName(displayLocale);

            // Rule 1: Prioritize "English" and its variants
            boolean isName1English = name1.startsWith("English");
            boolean isName2English = name2.startsWith("English");

            if (isName1English && !isName2English) {
                return -1; // name1 is English, name2 is not => name1 comes first
            }
            if (!isName1English && isName2English) {
                return 1;  // name1 is not English, name2 is => name2 comes first
            }
            // If both are English variants or neither are, proceed.

            // Rule 2: Group Latin-script display names before non-Latin-script display names
            boolean isName1Latin = isLatinScript(name1); // isLatinScript checks the display name
            boolean isName2Latin = isLatinScript(name2); // isLatinScript checks the display name

            if (isName1Latin && !isName2Latin) {
                return -1; // name1 (Latin) comes before name2 (non-Latin)
            }
            if (!isName1Latin && isName2Latin) {
                return 1;  // name1 (non-Latin) comes after name2 (Latin)
            }

            // Rule 3: Within each group, sort alphabetically using the English Collator on display names.
            return englishNameCollator.compare(name1, name2);
        };

        Collections.sort(locales, customLocaleComparator);
    }
}
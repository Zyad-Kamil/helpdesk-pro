
package com.helpdeskpro.service;

import java.util.Locale;

/** Normalization of user input shared by all services. */
final class TextNormalizer {

	private TextNormalizer() {
	}

	static String singleLine(String value) {
		return value != null
				? value.strip().replaceAll("\\s+", " ")
				: null;
	}

	static String text(String value) {
		return (value == null || value.isBlank())
				? null : value.strip();
	}

	static String email(String value) {
		return value != null
				? value.strip().toLowerCase(Locale.ROOT)
				: null;
	}

	/** Lower-case "contains" pattern for {@code like ... escape '!'}, or {@code null} for a blank keyword. */
	static String likePattern(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}

		// "!" is the escape character declared in the repository queries ("escape '!'")
		String escaped = keyword.strip()
				.toLowerCase(Locale.ROOT)
				.replace("!", "!!")
				.replace("%", "!%")
				.replace("_", "!_");

		return "%" + escaped + "%";
	}
}

package it.unical.mat.embasp.base;

/** Represents a single option for a generic ASP programs */
public class OptionDescriptor {
	/** where options are stored */
	private final String option;

	public OptionDescriptor(final String option) {
		this.option = option;
	}

	/**
	 * Returns the represented option as a string.
	 *
	 * @return {@link #option}'s data in a String format
	 */
	public String getOption() {
		return option;
	}

}

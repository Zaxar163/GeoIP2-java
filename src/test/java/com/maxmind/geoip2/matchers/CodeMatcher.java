package com.maxmind.geoip2.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import com.maxmind.geoip2.exception.InvalidRequestException;

public class CodeMatcher extends TypeSafeMatcher<InvalidRequestException> {

	public static CodeMatcher hasCode(String item) {
		return new CodeMatcher(item);
	}
	private String foundErrorCode;

	private final String expectedErrorCode;

	private CodeMatcher(String expectedErrorCode) {
		this.expectedErrorCode = expectedErrorCode;
	}

	@Override
	public void describeTo(Description description) {
		description.appendValue(this.foundErrorCode).appendText(" was not found instead of ")
				.appendValue(this.expectedErrorCode);
	}

	@Override
	protected boolean matchesSafely(final InvalidRequestException exception) {
		this.foundErrorCode = exception.getCode();
		return this.foundErrorCode.equalsIgnoreCase(this.expectedErrorCode);
	}
}

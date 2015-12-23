/*package com.qa.selenium.junit.rules;

import org.apache.commons.lang3.StringUtils;
import org.junit.runner.Description;

import com.saucelabs.common.SauceOnDemandAuthentication;
import com.saucelabs.common.SauceOnDemandSessionIdProvider;
import com.saucelabs.junit.SauceOnDemandTestWatcher;

public class CustomTestWatcher extends SauceOnDemandTestWatcher {
	public CustomTestWatcher(SauceOnDemandSessionIdProvider sessionIdProvider,
			SauceOnDemandAuthentication authentication) {
		super(sessionIdProvider, authentication);
	}

	protected void succeeded(Description description) {
		if (StringUtils.isNotBlank(readPropertyOrEnv("SAUCE_ONDEMAND_BROWSERS",
				null))) {
			super.succeeded(description);
		}
	}

	protected void failed(Throwable e, Description description) {
		if (StringUtils.isNotBlank(readPropertyOrEnv("SAUCE_ONDEMAND_BROWSERS",
				null))) {
			super.failed(e, description);
		} else {
			// TODO: Take screenshot
		}
	}

	private static String readPropertyOrEnv(String key, String defaultValue) {
		String v = System.getProperty(key);
		if (v == null)
			v = System.getenv(key);
		if (v == null)
			v = defaultValue;
		return v;
	}
}
*/
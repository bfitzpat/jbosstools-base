/*******************************************************************************
 * Copyright (c) 2010 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.usage.googleanalytics.eclipse;

import java.util.Random;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.usage.googleanalytics.AbstractGoogleAnalyticsParameters;
import org.jboss.tools.usage.googleanalytics.IGoogleAnalyticsParameters;
import org.jboss.tools.usage.googleanalytics.IUserAgent;
import org.jboss.tools.usage.internal.JBossToolsUsageActivator;
import org.jboss.tools.usage.preferences.IUsageReportPreferenceConstants;
import org.jboss.tools.usage.util.PreferencesUtils;

/**
 * @author Andre Dietisheim
 */
public abstract class AbstractEclipseEnvironment extends AbstractGoogleAnalyticsParameters implements IGoogleAnalyticsParameters {

	private static final String SYSPROP_JAVA_VERSION = "java.version";

	private String screenResolution;
	private String screenColorDepth;
	private Random random;
	private IEclipsePreferences preferences;
	private String firstVisit;
	private String lastVisit;
	private String currentVisit;
	private long visitCount;
	private IUserAgent eclipseUserAgent;

	public AbstractEclipseEnvironment(String accountName, String hostName, IEclipsePreferences preferences) {
		super(accountName, hostName);
		this.random = new Random();
		this.preferences = preferences;
		eclipseUserAgent = createEclipseUserAgent();
		initScreenSettings();
		initVisits();
	}

	protected void initScreenSettings() {
		final Display display = getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				screenColorDepth = display.getDepth() + SCREENCOLORDEPTH_POSTFIX;

				Rectangle bounds = display.getBounds();
				screenResolution = bounds.width + SCREERESOLUTION_DELIMITER + bounds.height;
			}
		});
	}

	private void initVisits() {
		String currentTime = String.valueOf(System.currentTimeMillis());
		this.currentVisit = currentTime;
		this.firstVisit = preferences.get(IUsageReportPreferenceConstants.FIRST_VISIT, null);
		if (firstVisit == null) {
			this.firstVisit = currentTime;
			preferences.put(IUsageReportPreferenceConstants.FIRST_VISIT, firstVisit);
		}
		lastVisit = preferences.get(IUsageReportPreferenceConstants.LAST_VISIT, currentTime);
		visitCount = preferences.getLong(IUsageReportPreferenceConstants.VISIT_COUNT, 1);
	}

	protected IUserAgent createEclipseUserAgent() {
		return new EclipseUserAgent();
	}

	public String getBrowserLanguage() {
		return eclipseUserAgent.getBrowserLanguage();
	}

	public String getScreenResolution() {
		return screenResolution;
	}

	public String getScreenColorDepth() {
		return screenColorDepth;
	}

	protected Display getDisplay() {
		if (PlatformUI.isWorkbenchRunning()) {
			return PlatformUI.getWorkbench().getDisplay();
		}

		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}

	public String getUserAgent() {
		return eclipseUserAgent.toString();
	}

	public String getUserId() {
		String userId = preferences.get(IUsageReportPreferenceConstants.ECLIPSE_INSTANCE_ID, null);
		if (userId == null) {
			userId = createIdentifier();
			preferences.put(IUsageReportPreferenceConstants.ECLIPSE_INSTANCE_ID, userId);
			PreferencesUtils.checkedSavePreferences(preferences, JBossToolsUsageActivator.getDefault(), GoogleAnalyticsEclipseMessages.EclipseEnvironment_Error_SavePreferences);
		}
		return userId;
	}

	/**
	 * Creates an unique identifier.
	 * 
	 * @return the identifier
	 */
	private String createIdentifier() {
		StringBuilder builder = new StringBuilder();
		builder.append(Math.abs(random.nextLong()));
		builder.append(System.currentTimeMillis());
		return builder.toString();
	}

	public abstract String getKeyword();
	
	public String getCurrentVisit() {
		return currentVisit;
	}

	public String getFirstVisit() {
		return firstVisit;
	}

	public String getLastVisit() {
		return lastVisit;
	}

	public long getVisitCount() {
		return visitCount;
	}

	public void visit() {
		lastVisit = currentVisit;
		preferences.put(IUsageReportPreferenceConstants.LAST_VISIT, lastVisit);
		currentVisit = String.valueOf(System.currentTimeMillis());
		visitCount++;
		preferences.putLong(IUsageReportPreferenceConstants.VISIT_COUNT, visitCount);
		PreferencesUtils.checkedSavePreferences(preferences, JBossToolsUsageActivator.getDefault(), GoogleAnalyticsEclipseMessages.EclipseEnvironment_Error_SavePreferences);
	}

	public String getFlashVersion() {
		return getJavaVersion();
	}

	private String getJavaVersion() {
		return System.getProperty(SYSPROP_JAVA_VERSION);
	}
}

/*******************************************************************************
 * Copyright (c) 2012 sfleury.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     sfleury - initial API and implementation
 ******************************************************************************/
package org.gots.weather;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.gots.garden.GardenInterface;
import org.gots.garden.sql.GardenDBHelper;
import org.gots.weather.exception.UnknownWeatherException;
import org.gots.weather.provider.DatabaseWeatherTask;
import org.gots.weather.provider.WeatherTask;
import org.gots.weather.provider.google.GoogleWeatherTask;
import org.gots.weather.provider.previmeteo.PrevimeteoWeatherTask;
import org.gots.weather.sql.WeatherDBHelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class WeatherManager {

	private WeatherSet ws;
	private Integer temperatureLimitHot;
	private Integer temperatureLimitCold;
	private Integer runningLimit;
	// private Date today;
	private Context mContext;
	private static SharedPreferences preferences;
	private Calendar weatherday;

	public WeatherManager(Context context) {
		this.mContext = context;
		update();

	}

	public void update() {
		weatherday = new GregorianCalendar();

		preferences = mContext.getSharedPreferences("org.gots.preference", 0);

		GardenDBHelper helper = new GardenDBHelper(mContext);
		GardenInterface garden = helper.getGarden(preferences.getInt("org.gots.preference.gardenid", 0));
		getWeather(garden);

	}

	private void getWeather(GardenInterface garden) {
		WeatherDBHelper helper = new WeatherDBHelper(mContext);
		WeatherConditionInterface wc;

		wc = getCondition(0);

		if (wc == null || true) {
			// today = Calendar.getInstance().getTime();

			try {
				for (int forecastDay = 0; forecastDay < 4; forecastDay++) {
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.DAY_OF_YEAR, forecastDay);
					
//					WeatherTask wt = new GoogleWeatherTask(garden.getAddress(), cal.getTime());
					WeatherTask wt = new PrevimeteoWeatherTask(garden.getAddress(), cal.getTime());
					WeatherConditionInterface conditionInterface = wt.execute().get();

					if (conditionInterface != null)
						updateCondition(conditionInterface, forecastDay);

				}

			} catch (Exception e) {
				if (e.getMessage() != null)
					Log.e("WeatherManager", e.getMessage());
			}
		}
	}

	private void updateCondition(WeatherConditionInterface condition, int day) {
		WeatherDBHelper helper = new WeatherDBHelper(mContext);

		// weatherday.add(Calendar.DAY_OF_YEAR, day);
		Calendar conditionDate = Calendar.getInstance();
		conditionDate.setTime(weatherday.getTime());
		conditionDate.add(Calendar.DAY_OF_YEAR, day);

		condition.setDate(conditionDate.getTime());
		condition.setDayofYear(conditionDate.get(Calendar.DAY_OF_YEAR));

		WeatherConditionInterface wc = helper.getWeatherByDayofyear(conditionDate.get(Calendar.DAY_OF_YEAR));

		if (wc == null)
			helper.insertWeather(condition);
		else
			helper.updateWeather(condition);
		return;

	}

	public Integer getTemperatureLimitHot() {
		return temperatureLimitHot;
	}

	public void setTemperatureLimitHot(Integer temperatureLimitHot) {
		this.temperatureLimitHot = temperatureLimitHot;
	}

	public Integer getTemperatureLimitCold() {
		return temperatureLimitCold;
	}

	public void setTemperatureLimitCold(Integer temperatureLimitCold) {
		this.temperatureLimitCold = temperatureLimitCold;
	}

	public Integer getRunningLimit() {
		return runningLimit;
	}

	public void setRunningLimit(Integer runningLimit) {
		this.runningLimit = runningLimit;
	}

	public WeatherConditionInterface getCondition(int i) {
		WeatherConditionInterface conditionInterface;

		Calendar weatherCalendar = Calendar.getInstance();
		weatherCalendar.add(Calendar.DAY_OF_YEAR, i);

		Date weatherDate = weatherCalendar.getTime();

		try {
			WeatherTask wt = new DatabaseWeatherTask(mContext, weatherDate);
			conditionInterface = wt.execute().get();
		} catch (Exception e) {
			conditionInterface = new WeatherCondition(weatherDate);
		}

		return conditionInterface;
	}

	public List<WeatherConditionInterface> getConditionSet(int nbDays) {
		List<WeatherConditionInterface> conditions = new ArrayList<WeatherConditionInterface>();
		for (int i = -nbDays; i <= nbDays; i++) {

			try {

				conditions.add(getCondition(i));
			} catch (Exception e) {
				conditions.add(new WeatherCondition());

				e.printStackTrace();
			}
		}
		return conditions;
	}
}

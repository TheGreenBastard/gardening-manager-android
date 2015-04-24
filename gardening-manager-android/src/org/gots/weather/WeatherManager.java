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
import java.util.List;

import org.gots.context.GotsContext;
import org.gots.preferences.GotsPreferences;
import org.gots.weather.provider.local.LocalWeatherProvider;
import org.gots.weather.provider.previmeteo.PrevimeteoWeatherProvider;
import org.gots.weather.provider.previmeteo.WeatherProvider;

import android.content.Context;

public class WeatherManager implements WeatherProvider {

    private Integer temperatureLimitHot;

    private Integer temperatureLimitCold;

    private Integer runningLimit;

    // private Date today;
    private Context mContext;

    WeatherProvider provider;

    private GotsPreferences gotsPrefs;

    private GotsContext getGotsContext() {
        return GotsContext.get(mContext);
    }

    public WeatherManager(Context context) {
        this.mContext = context;
        gotsPrefs = (GotsPreferences) getGotsContext().getServerConfig();

        provider = new PrevimeteoWeatherProvider(mContext);
        // provider = new LocalWeatherProvider(mContext);
    }

    @Override
    public short fetchWeatherForecast(String forecastLocality) {
        return provider.fetchWeatherForecast(forecastLocality);
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

    /*
     * GetCondition from today until passed argument (-i or +i)
     */
    public WeatherConditionInterface getCondition(int i) {

        Calendar weatherCalendar = Calendar.getInstance();
        weatherCalendar.add(Calendar.DAY_OF_YEAR, i);

        Date weatherDate = weatherCalendar.getTime();

        return getCondition(weatherDate);
    }

    public WeatherConditionInterface getCondition(Date weatherDate) {
        WeatherConditionInterface conditionInterface;
        try {
            conditionInterface = provider.getCondition(weatherDate);
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

    @Override
    public WeatherConditionInterface updateCondition(WeatherConditionInterface condition, Date day) {
        return provider.updateCondition(condition, day);
    }

    @Override
    public WeatherConditionInterface insertCondition(WeatherConditionInterface weatherCondition) {
        return provider.insertCondition(weatherCondition);
    }

}

package org.gots.weather.provider.previmeteo;

import java.util.Date;

import org.gots.bean.Address;
import org.gots.weather.WeatherConditionInterface;

public interface WeatherProvider {

    public abstract WeatherConditionInterface getCondition(Date requestedDay);
   
    public abstract WeatherConditionInterface updateCondition(WeatherConditionInterface condition, Date day);

    public abstract WeatherConditionInterface insertCondition(WeatherConditionInterface weatherCondition);

    public abstract short fetchWeatherForecast(Address address);

}
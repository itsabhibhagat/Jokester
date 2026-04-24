package com.application.jokester.random.provider;

import com.application.jokester.random.dto.RandomJokeResponse;

public interface JokeProvider {

    public String getProviderName();

    public RandomJokeResponse fetchJoke();

}

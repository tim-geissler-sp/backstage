@ignore
Feature: Poll Redis for test value

  Background:
    * url baseUrl
    * header Authorization = 'Bearer ' + token

  Scenario:
    * print __arg
    Given path 'ets/debug/redis/retrieve/' + key
    When method get

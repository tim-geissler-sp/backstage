@ignore
Feature: Poll Redis for test value

  Background:
    * url baseUrl
    * header Authorization = 'Bearer ' + token

  Scenario:
    * print __arg
    Given path 'sp-identity-event/debug/redis/retrieve/' + type + '/' + id
    When method get

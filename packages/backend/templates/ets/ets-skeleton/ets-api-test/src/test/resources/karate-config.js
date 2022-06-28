function fn() {
	// This is needed because of a bug in cloud-api-test that prevents logging
	// when https://sailpoint.atlassian.net/browse/PLTDEVX-639 is fixed this can be removed.
	karate.configure('logPrettyRequest', true);
	karate.configure('logPrettyResponse', true);
}

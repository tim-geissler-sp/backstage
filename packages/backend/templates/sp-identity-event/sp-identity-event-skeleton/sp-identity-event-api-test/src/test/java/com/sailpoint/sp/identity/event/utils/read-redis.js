function readFromRedis(x) {
	var attempts = x.attempts || 10;
	while (attempts-- > 0) {
		var result = karate.call('classpath:com/sailpoint/sp/identity/event/utils/redis_retrieve.feature', {type: x.type, id: x.id, token: x.token});
		if (result.responseStatus == 200) {
			karate.log('condition satisfied, exiting ' + result);
			return result.response;
		}
		karate.log('attempt #' + attempts + ' sleeping ...');

		// The test value in redis typically has a TTL of a minute, so we need to poll more frequently than that.
		java.lang.Thread.sleep(1000); // sleep for a second
	}
	return -1;
}

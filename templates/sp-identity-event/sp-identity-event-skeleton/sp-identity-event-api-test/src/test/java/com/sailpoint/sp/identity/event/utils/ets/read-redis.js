function readFromRedis(x) {
	var attempts = 10;
	while (attempts-- > 0) {
		var result = karate.call('classpath:com/sailpoint/sp/identity/event/utils/ets/redis_retrieve.feature', {key: x.key, token: x.token});
		if (result.responseStatus == 200) {
			karate.log('condition satisfied, exiting ' + result);
			return result.response;
		}
		karate.log('attempt #' + attempts + ' sleeping ...');

		java.lang.Thread.sleep(500);
	}
	return -1;
}

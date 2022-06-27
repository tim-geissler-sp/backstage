function sleep(x) {
	karate.log('sleeping ' + x.interval);
	java.lang.Thread.sleep(x.interval);
}
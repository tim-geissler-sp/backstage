/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.ets.tools.triggers;

import java.io.IOException;

public class TriggerSpecDriver {

	public static void main(String[] args) throws IOException{
		TriggersSpecGenerator triggersSpecGenerator = new TriggersSpecGenerator();
		triggersSpecGenerator.generateTriggerSpecs();
	}

}

package com.redhat.victims;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ RecordStreamTest.class, VictimsScannerTest.class,
		VictimsDatabaseTest.class })
public class OfflineTests {

}

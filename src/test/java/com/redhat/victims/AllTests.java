package com.redhat.victims;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ OfflineTests.class, OnlineTests.class})
public class AllTests {

}

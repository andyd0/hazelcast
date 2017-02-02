/*
 * Copyright (c) 2008-2017, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.map;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.SampleObjects;
import com.hazelcast.query.SqlPredicate;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.ParallelTest;
import com.hazelcast.test.annotation.SlowTest;
import com.hazelcast.test.bounce.BounceMemberRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Random;

import static com.hazelcast.test.TimeConstants.MINUTE;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.Assert.assertEquals;

/**
 * Query map while members of the cluster are being shutdown and started
 */
@RunWith(HazelcastParallelClassRunner.class)
@Category({SlowTest.class, ParallelTest.class})
public class QueryBounceTest {

    private static final String TEST_MAP_NAME = "employees";
    private static final int COUNT_ENTRIES = 100000;
    private static final int CONCURRENCY = 10;

    private IMap<String, SampleObjects.Employee> map;

    @Rule
    public BounceMemberRule bounceMemberRule = BounceMemberRule.with(new Config()).build();

    @Rule
    public TestName testName = new TestName();

    @Before
    public void setup() {
        if (testName.getMethodName().contains("Indexes")) {
            map = getMapWithIndexes();
        } else {
            map = getMap();
        }
        populateMap(map);
    }

    @Test(timeout = 4 * MINUTE)
    public void testQuery() {
        prepareAndRunQueryTasks();
    }

    @Test(timeout = 4 * MINUTE)
    public void testQueryWithIndexes() {
        prepareAndRunQueryTasks();
    }

    private void prepareAndRunQueryTasks() {
        QueryRunnable[] testTasks = new QueryRunnable[CONCURRENCY];
        for (int i = 0; i < CONCURRENCY; i++) {
            testTasks[i] = new QueryRunnable(bounceMemberRule.getNextTestDriver());
        }
        bounceMemberRule.testRepeatedly(testTasks, MINUTES.toSeconds(3));
    }

    private IMap<String, SampleObjects.Employee> getMap() {
        return bounceMemberRule.getSteadyMember().getMap(TEST_MAP_NAME);
    }

    // obtain a reference to test map from 0-th member with indexes created for Employee attributes
    private IMap<String, SampleObjects.Employee> getMapWithIndexes() {
        IMap<String, SampleObjects.Employee> map = bounceMemberRule.getSteadyMember().getMap(TEST_MAP_NAME);
        map.addIndex("id", false);
        map.addIndex("age", true);
        return map;
    }

    private void populateMap(IMap<String, SampleObjects.Employee> map) {
        for (int i = 0; i < COUNT_ENTRIES; i++) {
            SampleObjects.Employee e = new SampleObjects.Employee(i, "name" + i, i, true, i);
            map.put("name" + i, e);
        }
    }

    // Thread-safe querying runnable
    public static class QueryRunnable implements Runnable {

        private final IMap map;
        // query age min-max range, min is randomized, max = min+1000
        private final Random random = new Random();
        private final int numberOfResults = 1000;

        public QueryRunnable(HazelcastInstance hz) {
            this.map = hz.getMap(TEST_MAP_NAME);
        }

        @Override
        public void run() {
            int min, max;
            min = random.nextInt(COUNT_ENTRIES - numberOfResults);
            max = min + numberOfResults;
            String sql = (min % 2 == 0)
                    ? "age >= " + min + " AND age < " + max // may use sorted index
                    : "id >= " + min + " AND id < " + max;  // may use unsorted index
            Collection<SampleObjects.Employee> employees = map.values(new SqlPredicate(sql));
            assertEquals("Obtained " + employees.size() + " results for query '" + sql + "'",
                    numberOfResults, employees.size());
        }
    }

}

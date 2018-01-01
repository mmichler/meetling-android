package app.meetling;

import android.database.sqlite.SQLiteConstraintException;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import app.meetling.io.Host;
import app.meetling.io.LocalStorage;
import app.meetling.io.Then;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for the local storage access methods.
 */
@RunWith(AndroidJUnit4.class)
public class LocalStorageTest {
    @Rule
    public ExpectedException mException = ExpectedException.none();
    private LocalStorage mLocalStorage;
    private CountDownLatch mLatch = null;

    @Before
    public void setUp() {
        mLocalStorage = new LocalStorage(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), true);
        mLatch = new CountDownLatch(1);
    }

    @Test
    public void addHost() throws InterruptedException {
        List<Host> actualHosts = new ArrayList<>();

        Then.Callback<List<Host>> countHosts = new Then.Callback<List<Host>>() {
            @Override
            public void call(List<Host> hosts) {
                actualHosts.addAll(hosts);
                mLatch.countDown();
            }
        };

        Then.Callback<Host> getHosts = new Then.Callback<Host>() {
            @Override
            public void call(Host host) {
                mLocalStorage.getHosts().then(countHosts);
            }
        };

        mLocalStorage.addHost("https://testling.org").then(getHosts);
        mLatch.await();
        assertEquals(2, actualHosts.size());
    }

    @Test
    public void addHostNotUnique() throws InterruptedException {
        RuntimeException[] exception = new RuntimeException[1];
        mException.expect(SQLiteConstraintException.class);
        mException.expectMessage("2067"); // SQLITE_CONSTRAINT_UNIQUE

        Then.Callback<Host> waitForException = new Then.Callback<Host>() {
            @Override
            public void call(Host host) {
                mLatch.countDown();
                exception[0] = getError();
            }
        };

        Then.Callback<Host> addTheSameHost = new Then.Callback<Host>() {
            @Override
            public void call(Host host) {
                mLocalStorage.addHost("https://testling.org").then(waitForException);
            }
        };

        mLocalStorage.addHost("https://testling.org").then(addTheSameHost);

        mLatch.await();

        throw exception[0];
    }

    @Test
    public void removeHost() throws InterruptedException {
        mException.expect(IllegalArgumentException.class);
        RuntimeException[] exception = new RuntimeException[1];
        final Host[] testHost = new Host[1];

        Then.Callback<Host> waitForException = new Then.Callback<Host>() {
            @Override
            public void call(Host host) {
                mLatch.countDown();
                exception[0] = getError();
            }
        };

        Then.Callback<Void> getHost = new Then.Callback<Void>() {
            @Override
            public void call(Void aVoid) {
                mLocalStorage.getHost(testHost[0].getId()).then(waitForException);
            }
        };

        Then.Callback<Host> deleteHost = new Then.Callback<Host>() {
            @Override
            public void call(Host host) {
                testHost[0] = host;
                mLocalStorage.removeHost(host.getId()).then(getHost);
            }
        };

        mLocalStorage.addHost("https://testling.org").then(deleteHost);

        mLatch.await();

        throw exception[0];
    }

    @Test
    public void storeCredentials() throws InterruptedException {
        final String userIdExpected = "id";
        final String authSecretExpected = "secret";
        final Pair[] credentialsActual = new Pair[1];


        final Then.Callback<Host> readCredentials = new Then.Callback<Host>() {
            @Override
            public void call(Host host) {
                credentialsActual[0] = new Pair<>(host.getUserId(), host.getAuthSecret());
                mLatch.countDown();
            }
        };

        Then.Callback<Host> getCredentials = new Then.Callback<Host>() {
            @Override
            public void call(Host host) {
                mLocalStorage.getHost(host.getId()).then(readCredentials);
            }
        };

        Then.Callback<Host> setCredentials = new Then.Callback<Host>() {
            @Override
            public void call(Host host) {
                mLocalStorage.setCredentials(host, userIdExpected, authSecretExpected).then(getCredentials);
            }
        };

        mLocalStorage.addHost("https://testling.org").then(setCredentials);
        mLatch.await();

        assertEquals(userIdExpected, credentialsActual[0].first);
        assertEquals(authSecretExpected, credentialsActual[0].second);
    }

    @Test
    public void storeHistory() throws InterruptedException {
        final List<String> meetingIdsExpected = Arrays.asList("m1", "m2", "m3");
        final List<String> meetingIdsActual = new ArrayList<>();

        final Then.Callback<List<String>> readHistory = new Then.Callback<List<String>>() {
            @Override
            public void call(List<String> meetingIds) {
                meetingIdsActual.addAll(meetingIds);
                mLatch.countDown();
            }
        };

        Then.Callback<Void> getHistory = new Then.Callback<Void>() {
            @Override
            public void call(Void aVoid) {
                mLocalStorage.getHistory().then(readHistory);
            }
        };

        mLocalStorage.setHistory(meetingIdsExpected).then(getHistory);
        mLatch.await();

        for (int i = 0; i < meetingIdsActual.size(); i++) {
            assertEquals(meetingIdsExpected.get(i), meetingIdsActual.get(i));
        }
    }
}

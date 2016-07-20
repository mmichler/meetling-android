package app.meetling;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.util.Pair;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import app.meetling.io.LocalStorage;
import app.meetling.io.Then;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the local storage access methods.
 */
@RunWith(AndroidJUnit4.class)
public class LocalStorageTest {
    private LocalStorage mLocalStorage;
    private CountDownLatch mLatch = null;

    @Before
    public void setUp() {
        mLocalStorage = new LocalStorage(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), true);
        mLatch = new CountDownLatch(1);
    }

    @Test
    public void storeCredentials() throws InterruptedException {
        final String userIdExpected = "id";
        final String authSecretExpected = "secret";
        final Pair<String, String>[] credentialsActual = new Pair[1];


        final Then.Callback<Pair<String, String>> readCredentials
                = new Then.Callback<Pair<String, String>>() {
            @Override
            public void call(Pair<String, String> credentials) {
                credentialsActual[0] = credentials;
                mLatch.countDown();
            }
        };

        Then.Callback<Void> getCredentials = new Then.Callback<Void>() {
            @Override
            public void call(Void aVoid) {
                mLocalStorage.getCredentials().then(readCredentials);
            }
        };

        mLocalStorage.setCredentials(userIdExpected, authSecretExpected).then(getCredentials);
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

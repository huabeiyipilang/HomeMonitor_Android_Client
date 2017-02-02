package com.penghaonan.homemonitorclient;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void getProfileTest() {
        String test = "getprofile:hehe";
        handleResponse(test);
    }

    private void log(String log) {
        System.out.println(log);
    }


    private void handleResponse(String response) {
        String CMD_GET_PROFILE = "getprofile";
        if (response.startsWith(CMD_GET_PROFILE) && response.length() > CMD_GET_PROFILE.length() + 1) {
            String body = response.substring(CMD_GET_PROFILE.length() + 1);
            log(body);
        }
    }
}
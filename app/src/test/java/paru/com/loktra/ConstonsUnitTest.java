package paru.com.loktra;

import org.junit.Test;

import paru.com.loktra.util.loktraConstons;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ConstonsUnitTest {

    @Test
    public void constant_value() throws Exception {
        assertEquals(loktraConstons.GPS_DISCONNECTED, "gps_disconnected");
        assertEquals(loktraConstons.GPS_STATUS, "gps_status");
        assertEquals(loktraConstons.GPS_BUNDLE, "gps_bundle");
    }
}
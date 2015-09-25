package ovh.adiantek.android.androjavame;

import com.sun.midp.midlet.MIDletState;

import javax.microedition.lcdui.Display;

/**
 * Created by barwnikk on 24.09.15.
 */
public class Natives {

    /**
     * Requests that the device handle (e.g. display or install)
     * the indicated URL.
     *
     * <p>If the platform has the appropriate capabilities and
     * resources available, it SHOULD bring the appropriate
     * application to the foreground and let the user interact with
     * the content, while keeping the MIDlet suite running in the
     * background. If the platform does not have appropriate
     * capabilities or resources available, it MAY wait to handle the
     * URL request until after the MIDlet suite exits. In this case,
     * when the requesting MIDlet suite exits, the platform MUST then
     * bring the appropriate application to the foreground to let the
     * user interact with the content.</p>
     *
     * <p>This is a non-blocking method. In addition, this method does
     * NOT queue multiple requests. On platforms where the MIDlet
     * suite must exit before the request is handled, the platform
     * MUST handle only the last request made. On platforms where the
     * MIDlet suite and the request can be handled concurrently, each
     * request that the MIDlet suite makes MUST be passed to the
     * platform software for handling in a timely fashion.</p>
     *
     * <p>If the URL specified refers to a MIDlet suite (either an
     * Application Descriptor or a JAR file), the request is
     * interpreted as a request to install the named package. In this
     * case, the platform's normal MIDlet suite installation process
     * SHOULD be used, and the user MUST be allowed to control the
     * process (including cancelling the download and/or
     * installation). If the MIDlet suite being installed is an
     * <em>update</em> of the currently running MIDlet suite, the
     * platform MUST first stop the currently running MIDlet suite
     * before performing the update. On some platforms, the currently
     * running MIDlet suite MAY need to be stopped before any
     * installations can occur.</p>
     *
     * <p>If the URL specified is of the form
     * <code>tel:&lt;number&gt;</code>, as specified in <a
     * href="http://rfc.net/rfc2806.html">RFC2806</a>, then the
     * platform MUST interpret this as a request to initiate a voice
     * call. The request MUST be passed to the &quot;phone&quot;
     * application to handle if one is present in the platform.</p>
     *
     * <p>Devices MAY choose to support additional URL schemes beyond
     * the requirements listed above.</p>
     *
     * <p>Many of the ways this method will be used could have a
     * financial impact to the user (e.g. transferring data through a
     * wireless network, or initiating a voice call). Therefore the
     * platform MUST ask the user to explicitly acknowlege each
     * request before the action is taken. Implementation freedoms are
     * possible so that a pleasant user experience is retained. For
     * example, some platforms may put up a dialog for each request
     * asking the user for permission, while other platforms may
     * launch the appropriate application and populate the URL or
     * phone number fields, but not take the action until the user
     * explicitly clicks the load or dial buttons.</p>
     *
     * @return true if the MIDlet suite MUST first exit before the
     * content can be fetched.
     *
     * @param URL The URL for the platform to load.
     *
     * @exception ConnectionNotFoundException if
     * the platform cannot handle the URL requested.
     *
     * @since MIDP 2.0
     */
    public static final boolean Java_com_sun_midp_midlet_MIDletState_platformRequest(MIDletState state,String URL) {
        return false;
    }
    /**
     * play a sound.
     * @param alertType type of alert
     * @return true, if sound was played.
     */
    public static boolean Java_javax_microedition_lcdui_Display_playAlertSound(Display display, int alertType) {
        return false;
    }

    /**
     * Plays the vibration.
     * @param dur the duration in milli seconds
     * @return if it's successful, return 1, othewise return 0
     *
     */
    public static int Java_javax_microedition_lcdui_Display_nVibrate(Display display, int dur) {
        if(MainActivity.vibrator==null)
            return 0;
        if(!MainActivity.vibrator.hasVibrator())
            return 0;
        MainActivity.vibrator.vibrate(dur);
        return 1;
    }
}

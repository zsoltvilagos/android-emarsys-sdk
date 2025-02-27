package com.emarsys.mobileengage.iam.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;

import com.emarsys.core.provider.timestamp.TimestampProvider;
import com.emarsys.mobileengage.iam.dialog.action.OnDialogShownAction;
import com.emarsys.mobileengage.iam.webview.IamWebViewProvider;
import com.emarsys.testUtil.InstrumentationRegistry;
import com.emarsys.testUtil.TimeoutUtils;
import com.emarsys.testUtil.fake.FakeActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static android.os.Build.VERSION_CODES.KITKAT;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SdkSuppress(minSdkVersion = KITKAT)
public class IamDialogTest {

    public static final String CAMPAIGN_ID = "id_value";
    public static final String ON_SCREEN_TIME_KEY = "on_screen_time";
    public static final String CAMPAIGN_ID_KEY = "id";
    public static final String REQUEST_ID_KEY = "request_id";
    private TestIamDialog dialog;

    @Rule
    public TestRule timeout = TimeoutUtils.getTimeoutRule();

    @Rule
    public ActivityTestRule<FakeActivity> activityRule = new ActivityTestRule<>(FakeActivity.class);

    @Before
    public void init() throws InterruptedException {
        dialog = TestIamDialog.create(
                CAMPAIGN_ID,
                new CountDownLatch(1),
                new CountDownLatch(1),
                new CountDownLatch(1),
                new CountDownLatch(1)
        );

        initWebViewProvider();
    }

    @After
    public void tearDown() throws Exception {
        setWebViewInProvider(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreate_campaignIdMustNotBeNull() {
        IamDialog.create(null, "requestId");
    }

    @Test
    public void testCreate_shouldReturnImageDialogInstance() {
        assertNotNull(IamDialog.create("", "requestId"));
    }

    @Test
    public void testCreate_shouldInitializeDialog_withCampaignId() {
        String campaignId = "123456789";
        IamDialog dialog = IamDialog.create(campaignId, "requestId");

        Bundle result = dialog.getArguments();
        assertEquals(campaignId, result.getString(CAMPAIGN_ID_KEY));
    }

    @Test
    public void testCreate_shouldInitializeDialog_withRequestId() {
        String requestId = "requestId";
        String campaignId = "campaignId";
        IamDialog dialog = IamDialog.create(campaignId, requestId);

        Bundle result = dialog.getArguments();
        assertEquals(requestId, result.getString(REQUEST_ID_KEY));
    }

    @Test
    public void testCreate_shouldInitializeDialog_withOutRequestId() {
        String campaignId = "campaignId";
        IamDialog dialog = IamDialog.create(campaignId, null);

        Bundle result = dialog.getArguments();
        assertNull(result.getString(REQUEST_ID_KEY));
    }

    @Test
    public void testInitialization_setsDimAmountToZero() throws InterruptedException {
        displayDialog();

        float expected = 0.0f;
        float actual = dialog.getDialog().getWindow().getAttributes().dimAmount;

        assertEquals(expected, actual, 0.0000001);
    }

    @Test
    public void testInitialization_setsDialogToFullscreen() throws InterruptedException {
        displayDialog();

        float dialogWidth = activityRule.getActivity().getWindow().getAttributes().width;
        float dialogHeight = activityRule.getActivity().getWindow().getAttributes().height;

        float windowWidth = dialog.getDialog().getWindow().getAttributes().width;
        float windowHeight = dialog.getDialog().getWindow().getAttributes().height;

        assertEquals(windowWidth, dialogWidth, 0.0001);
        assertEquals(windowHeight, dialogHeight, 0.0001);
    }

    @Test
    public void testDialog_stillVisible_afterOrientationChange() throws InterruptedException {
        final IamDialog iamDialog = IamDialog.create(CAMPAIGN_ID, REQUEST_ID_KEY);
        final AppCompatActivity activity = activityRule.getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                iamDialog.show((activity).getSupportFragmentManager(), "testDialog");
            }
        });
        activityRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getInstrumentation().waitForIdleSync();
        initWebViewProvider();
        activityRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getInstrumentation().waitForIdleSync();
        initWebViewProvider();

        float dialogWidth = activityRule.getActivity().getWindow().getAttributes().width;
        float dialogHeight = activityRule.getActivity().getWindow().getAttributes().height;

        float windowWidth = iamDialog.getDialog().getWindow().getAttributes().width;
        float windowHeight = iamDialog.getDialog().getWindow().getAttributes().height;

        assertEquals(windowWidth, dialogWidth, 0.0001);
        assertEquals(windowHeight, dialogHeight, 0.0001);
    }

    @Test
    public void testDialog_cancel_turnsRetainInstanceOff() {
        final IamDialog iamDialog = IamDialog.create(CAMPAIGN_ID, REQUEST_ID_KEY);
        final AppCompatActivity activity = activityRule.getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                iamDialog.show((activity).getSupportFragmentManager(), "testDialog");
            }
        });
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getInstrumentation().waitForIdleSync();

        iamDialog.getDialog().cancel();

        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getInstrumentation().waitForIdleSync();

        assertFalse(iamDialog.getRetainInstance());
    }

    @Test
    public void testDialog_dismiss_turnsRetainInstanceOff() {
        final IamDialog iamDialog = IamDialog.create(CAMPAIGN_ID, REQUEST_ID_KEY);
        final AppCompatActivity activity = activityRule.getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                iamDialog.show((activity).getSupportFragmentManager(), "testDialog");
            }
        });
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getInstrumentation().waitForIdleSync();

        iamDialog.dismiss();

        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getInstrumentation().waitForIdleSync();

        assertFalse(iamDialog.getRetainInstance());
    }

    @Test
    public void testOnResume_callsActions_ifProvided() throws InterruptedException {
        Bundle args = new Bundle();
        args.putString(CAMPAIGN_ID_KEY, "123456789");
        dialog.setArguments(args);

        List<OnDialogShownAction> actions = createMockActions();
        dialog.setActions(actions);

        displayDialog();

        for (OnDialogShownAction action : actions) {
            verify(action).execute(eq("123456789"));
        }
    }

    @Test
    public void testOnResume_callsActions_onlyOnce() throws InterruptedException {
        List<OnDialogShownAction> actions = createMockActions();
        dialog.setActions(actions);

        displayDialog();

        dismissDialog();

        displayDialog();

        for (OnDialogShownAction action : actions) {
            verify(action, times(1)).execute(any(String.class));
        }
    }

    @Test
    public void testOnScreenTime_savesDuration_betweenResumeAndPause() throws InterruptedException {
        TimestampProvider timestampProvider = mock(TimestampProvider.class);
        when(timestampProvider.provideTimestamp()).thenReturn(100L, 250L);

        dialog.timestampProvider = timestampProvider;

        displayDialog();

        pauseDialog();

        long onScreenTime = dialog.getArguments().getLong("on_screen_time");
        assertEquals(150L, onScreenTime);
    }

    @Test
    public void testOnScreenTime_aggregatesDurations_betweenMultipleResumeAndPause() throws InterruptedException {
        TimestampProvider timestampProvider = mock(TimestampProvider.class);
        when(timestampProvider.provideTimestamp()).thenReturn(100L, 250L, 1000L, 1003L);

        dialog.timestampProvider = timestampProvider;

        displayDialog();

        pauseDialog();

        assertEquals(150L, dialog.getArguments().getLong("on_screen_time"));

        resumeDialog();

        pauseDialog();

        assertEquals(150L + 3, dialog.getArguments().getLong("on_screen_time"));
    }

    private void displayDialog() throws InterruptedException {
        dialog.resumeLatch = new CountDownLatch(1);
        final AppCompatActivity activity = activityRule.getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.show((activity).getSupportFragmentManager(), "testDialog");
            }
        });
        dialog.resumeLatch.await();
    }

    private void resumeDialog() throws InterruptedException {
        dialog.resumeLatch = new CountDownLatch(1);

        final Activity activity = activityRule.getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.onResume();
            }
        });

        dialog.resumeLatch.await();
    }

    private void pauseDialog() throws InterruptedException {
        dialog.pauseLatch = new CountDownLatch(1);

        final Activity activity = activityRule.getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.onPause();
            }
        });

        dialog.pauseLatch.await();
    }

    private void cancelDialog() throws InterruptedException {
        dialog.cancelLatch = new CountDownLatch(1);

        final Activity activity = activityRule.getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.onCancel(mock(DialogInterface.class));
            }
        });

        dialog.cancelLatch.await();
    }

    private void dismissDialog() throws InterruptedException {
        dialog.stopLatch = new CountDownLatch(1);

        final Activity activity = activityRule.getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
            }
        });

        dialog.stopLatch.await();
    }

    private void setWebViewInProvider(WebView webView) throws Exception {
        Field webViewField = IamWebViewProvider.class.getDeclaredField("webView");
        webViewField.setAccessible(true);
        webViewField.set(null, webView);
    }

    private void initWebViewProvider() throws InterruptedException {
        final CountDownLatch initLatch = new CountDownLatch(1);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    setWebViewInProvider(new WebView(InstrumentationRegistry.getTargetContext()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                initLatch.countDown();
            }
        });

        initLatch.await();
    }

    private List<OnDialogShownAction> createMockActions() {
        return Arrays.asList(
                mock(OnDialogShownAction.class),
                mock(OnDialogShownAction.class),
                mock(OnDialogShownAction.class)
        );
    }


}
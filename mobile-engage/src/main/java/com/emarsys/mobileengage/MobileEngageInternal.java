package com.emarsys.mobileengage;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.emarsys.core.DefaultCoreCompletionHandler;
import com.emarsys.core.api.result.CompletionListener;
import com.emarsys.core.request.RequestManager;
import com.emarsys.core.request.model.RequestModel;
import com.emarsys.core.util.Assert;
import com.emarsys.core.util.TimestampUtils;
import com.emarsys.mobileengage.event.applogin.AppLoginParameters;
import com.emarsys.mobileengage.storage.MeIdStorage;
import com.emarsys.mobileengage.util.RequestHeaderUtils;
import com.emarsys.mobileengage.util.RequestModelUtils;
import com.emarsys.mobileengage.util.RequestPayloadUtils;
import com.emarsys.mobileengage.util.RequestUrlUtils;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.emarsys.mobileengage.endpoint.Endpoint.ME_LOGOUT_V2;

public class MobileEngageInternal {
    public static final String MOBILEENGAGE_SDK_VERSION = BuildConfig.VERSION_NAME;

    String pushToken;
    final RequestManager manager;
    final DefaultCoreCompletionHandler coreCompletionHandler;
    final Handler uiHandler;
    final RequestContext requestContext;

    public MobileEngageInternal(
            RequestManager manager,
            Handler uiHandler,
            DefaultCoreCompletionHandler coreCompletionHandler,
            RequestContext requestContext
    ) {
        Assert.notNull(manager, "Manager must not be null!");
        Assert.notNull(requestContext, "RequestContext must not be null!");
        Assert.notNull(coreCompletionHandler, "CoreCompletionHandler must not be null!");

        this.manager = manager;
        this.requestContext = requestContext;
        this.uiHandler = uiHandler;
        this.coreCompletionHandler = coreCompletionHandler;
        try {
            this.pushToken = FirebaseInstanceId.getInstance().getToken();
        } catch (Exception ignore) {
        }
    }

    RequestManager getManager() {
        return manager;
    }

    public RequestContext getRequestContext() {
        return requestContext;
    }


    String getPushToken() {
        return pushToken;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
        if (requestContext.getAppLoginParameters() != null) {
            sendAppLogin(null);
        }
    }

    public String appLogin(CompletionListener completionListener) {
        requestContext.setAppLoginParameters(new AppLoginParameters());
        return sendAppLogin(completionListener);
    }

    public String appLogin(String contactFieldValue, CompletionListener completionListener) {
        requestContext.setAppLoginParameters(new AppLoginParameters(requestContext.getContactFieldId(), contactFieldValue));
        return sendAppLogin(completionListener);
    }

    String sendAppLogin(CompletionListener completionListener) {
        RequestModel model = RequestModelUtils.createAppLogin_V2(
                requestContext,
                pushToken);

        Integer storedHashCode = requestContext.getAppLoginStorage().get();
        int currentHashCode = model.getPayload().hashCode();

        if (!shouldDoAppLogin(storedHashCode, currentHashCode, requestContext.getMeIdStorage())) {
            model = RequestModelUtils.createLastMobileActivity(requestContext);
        } else {
            requestContext.getAppLoginStorage().set(currentHashCode);
        }

        manager.submit(model, completionListener);
        return model.getId();
    }

    public String appLogout(CompletionListener completionListener) {
        requestContext.setAppLoginParameters(null);

        RequestModel model = new RequestModel.Builder(requestContext.getTimestampProvider(), requestContext.getUUIDProvider())
                .url(ME_LOGOUT_V2)
                .payload(RequestPayloadUtils.createBasePayload(requestContext))
                .headers(RequestHeaderUtils.createBaseHeaders_V2(requestContext))
                .build();

        manager.submit(model, completionListener);
        requestContext.getMeIdStorage().remove();
        requestContext.getMeIdSignatureStorage().remove();
        requestContext.getAppLoginStorage().remove();
        return model.getId();
    }

    public String trackCustomEvent(String eventName, Map<String, String> eventAttributes, CompletionListener completionListener) {
        return trackCustomEvent_V3(eventName, eventAttributes, completionListener);
    }

    private String trackCustomEvent_V3(String eventName, Map<String, String> eventAttributes, CompletionListener completionListener) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "custom");
        event.put("name", eventName);
        event.put("timestamp", TimestampUtils.formatTimestampWithUTC(requestContext.getTimestampProvider().provideTimestamp()));
        if (eventAttributes != null && !eventAttributes.isEmpty()) {
            event.put("attributes", eventAttributes);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("clicks", new ArrayList<>());
        payload.put("viewed_messages", new ArrayList<>());
        payload.put("events", Collections.singletonList(event));

        RequestModel model = new RequestModel.Builder(requestContext.getTimestampProvider(), requestContext.getUUIDProvider())
                .url(RequestUrlUtils.createEventUrl_V3(requestContext.getMeIdStorage().get()))
                .payload(payload)
                .headers(RequestHeaderUtils.createBaseHeaders_V3(requestContext))
                .build();

        manager.submit(model, completionListener);
        return model.getId();
    }

    public String trackInternalCustomEvent(
            String eventName,
            Map<String, String> eventAttributes,
            CompletionListener completionListener) {
        Assert.notNull(eventName, "EventName must not be null!");

        if (requestContext.getMeIdStorage().get() != null && requestContext.getMeIdSignatureStorage().get() != null) {
            RequestModel model = RequestModelUtils.createInternalCustomEvent(
                    eventName,
                    eventAttributes,
                    requestContext);

            manager.submit(model, completionListener);
            return model.getId();
        } else {
            return requestContext.getUUIDProvider().provideId();
        }
    }

    public String trackMessageOpen(Intent intent, CompletionListener completionListener) {
        Assert.notNull(intent, "Intent must not be null!");

        String messageId = getMessageId(intent);

        return handleMessageOpen(messageId, completionListener);
    }

    String getMessageId(Intent intent) {
        String sid = null;
        Bundle payload = intent.getBundleExtra("payload");
        if (payload != null) {
            String customData = payload.getString("u");
            if (customData != null) {
                try {
                    sid = new JSONObject(customData).getString("sid");
                } catch (JSONException e) {
                }
            }
        }
        return sid;
    }

    private String handleMessageOpen(String messageId, final CompletionListener completionListener) {
        if (messageId != null) {
            return handleMessageOpen_V3(messageId, completionListener);

        } else {
            final String uuid = requestContext.getUUIDProvider().provideId();
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    completionListener.onCompleted(new IllegalArgumentException("No messageId found!"));
                }
            });
            return uuid;
        }
    }

    private String handleMessageOpen_V3(String messageId, CompletionListener completionListener) {
        HashMap<String, String> attributes = new HashMap<>();
        attributes.put("sid", messageId);
        attributes.put("origin", "main");
        return trackInternalCustomEvent("push:click", attributes, completionListener);
    }

    private boolean shouldDoAppLogin(Integer storedHashCode, int currentHashCode, MeIdStorage meIdStorage) {
        return storedHashCode == null || currentHashCode != storedHashCode || meIdStorage.get() == null;
    }

}

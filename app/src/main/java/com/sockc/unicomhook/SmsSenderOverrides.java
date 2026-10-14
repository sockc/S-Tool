package com.sockc.unicomhook;

import android.content.SharedPreferences;
import android.os.Bundle;
import java.util.Map;

/** Only manually confirmed labels are transferred between S-Tool and Messages. */
final class SmsSenderOverrides {
    static final String PREFIX = "sms_sender_override.";
    private SmsSenderOverrides() { }

    static Bundle snapshot(SharedPreferences prefs) {
        Bundle result = new Bundle();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (!entry.getKey().startsWith(PREFIX)
                    || !(entry.getValue() instanceof String)) continue;
            String sender = entry.getKey().substring(PREFIX.length());
            String label = (String) entry.getValue();
            if (SmsSenderRecognizer.normalizeSender(sender) != null
                    && SmsSenderRecognizer.validName(label)) {
                result.putString(sender, label);
            }
        }
        return result;
    }
}

package com.scamshield.app.util;

import android.content.Context;
import android.content.Intent;

import com.scamshield.app.data.model.ScamAnalysisResult;

public class ShareUtils {

    public static void shareAssessmentResult(Context context, ScamAnalysisResult result) {
        if (context == null || result == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("🛡️ ScamShield Security Assessment\n");
        sb.append("Risk Level: ").append(result.getRiskLevel().getTitle()).append("\n");
        sb.append("Risk Score: ").append(result.getRiskScore()).append(" / 100\n\n");

        if (!result.getWhyFlagged().isEmpty()) {
            sb.append("Key Flags Detected:\n");
            for (String flag : result.getWhyFlagged()) {
                sb.append("• ").append(flag).append("\n");
            }
            sb.append("\n");
        }

        if (!result.getRecommendedActions().isEmpty()) {
            sb.append("Recommended Next Action:\n");
            for (String action : result.getRecommendedActions()) {
                sb.append("✓ ").append(action).append("\n");
            }
            sb.append("\n");
        }

        sb.append("ScamShield cannot guarantee that content is 100% safe. Always verify senders independently.");

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, "Share ScamShield Assessment");
        context.startActivity(shareIntent);
    }
}

/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settingslib.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.StyleRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.widget.preference.barchart.R;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CircularGraphicPreference extends Preference {
    private Map<String, Pair<Integer, String>> mUsages = new HashMap<>();
    private String mCenterText = "";

    public CircularGraphicPreference(Context context, AttributeSet attrs,
                                     @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public CircularGraphicPreference(Context context, AttributeSet attrs,
                                     @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CircularGraphicPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularGraphicPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.circular_graphic);
        setSelectable(false);
    }

    /**
     * Sets permission group usages: map of group name to usage count.
     */
    public void setUsages(Map<String, Pair<Integer, String>> usages) {
        if (mUsages != usages) {
            mUsages = usages;
            notifyChanged();
        }
    }

    public void setCentreLabel(String centreText) {
        if (!mCenterText.equals(centreText)) {
            mCenterText = centreText;
            notifyChanged();
        }
    }

    @SuppressLint("Recycle")
    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        boolean isUsagesEmpty = mUsages.isEmpty();

        CompositeCircleView ccv = (CompositeCircleView) holder.findViewById(R.id.composite_circle_view);
        CompositeCircleViewLabeler ccvl = (CompositeCircleViewLabeler) holder.findViewById(R.id.composite_circle_view_labeler);

        // Set center text.
        TextView centerLabel = new TextView(getContext());
        centerLabel.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        centerLabel.setTextAppearance(R.style.CircularGraphicLabel);

        // Create labels, counts, and colors.
        TextView[] labels;
        Pair<Integer, String>[] counts;
        int[] colors = new int[]{
                getContext().getColor(R.color.settings_bar_view_1_color),
                getContext().getColor(R.color.settings_bar_view_2_color),
                getContext().getColor(R.color.settings_bar_view_3_color),
                getContext().getColor(R.color.settings_bar_view_4_color)
        };

        if (isUsagesEmpty) {
            // Special case if usages are empty.
            labels = new TextView[0];
            counts = new Pair[]{new Pair<>(0, "")};
        } else {
            labels = new TextView[]{
                    new TextView(getContext()),
                    new TextView(getContext()),
                    new TextView(getContext()),
                    new TextView(getContext())
            };
            counts = new Pair[]{
                    getUsageCount("Value 1"),
                    getUsageCount("Value 2"),
                    getUsageCount("Value 3"),
                    getUsageCount("Value 4")
            };
            for (int i = 0; i < counts.length; i++) {
                labels[i].setText(counts[i].second);
            }
            centerLabel.setText(mCenterText);

            // Set label styles.
            for (TextView label : labels) {
                if (label != null) {
                    label.setTextAppearance(R.style.CircularGraphicLabel);
                }
            }
        }

        // Get circle-related dimensions.
        TypedValue outValue = new TypedValue();
        getContext().getResources().getValue(R.dimen.circular_label_radius_scalar, outValue, true);
        float labelRadiusScalar = outValue.getFloat();
        int circleStrokeWidth = (int) getContext().getResources().getDimension(R.dimen.circular_circle_stroke_width);

        // Configure circle and labeler.
        ccvl.configure(R.id.composite_circle_view, centerLabel, labels, labelRadiusScalar);
        // Start at angle 300 (top right) to allow for small segments.
        ccv.configure(300f, counts, colors, circleStrokeWidth, labels);
    }

    private Pair<Integer, String> getUsageCount(String group) {
        Pair<Integer, String> count = mUsages.get(group);
        if (count == null) {
            return new Pair<>(0, "");
        }
        return count;
    }

    private int getUsageCountExcluding(String... excludeGroups) {
        int count = 0;
        List<String> exclude = Arrays.asList(excludeGroups);
        for (Map.Entry<String, Pair<Integer, String>> entry : mUsages.entrySet()) {
            if (exclude.indexOf(entry.getKey()) >= 0) {
                continue;
            }
            count += entry.getValue().first;
        }
        return count;
    }

    private boolean isUsagesEmpty() {
        return getUsageCountExcluding() == 0;
    }
}

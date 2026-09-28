package dev.betterendfield.android;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

/**
 * A numeric camera setting: label, current value, and a slider.
 *
 * The desktop module takes movement speed and both fields of view as free-form
 * floats from an ini file. Typing a float into a text box on a phone is a poor
 * trade, so the Android page bounds each value and exposes it as a slider; the
 * configuration written out is still the same float the desktop module parses.
 * Precision controls retain their exact stored value until the user moves the
 * slider, so saving another setting cannot quantize a first-person eye offset.
 */
final class ValueSlider extends LinearLayout {
    /** Slider resolution. The step is (maximum - minimum) / STEPS. */
    private static final int STEPS = 200;

    private final TextView value;
    private final SeekBar bar;
    private final float minimum;
    private final float maximum;
    private final String unit;
    private final int steps;
    private final int decimalPlaces;
    private float preciseValue;
    private Runnable listener = () -> {};

    ValueSlider(Context context, String label, String unit, float minimum, float maximum) {
        this(context, label, unit, minimum, maximum, STEPS, -1);
    }

    ValueSlider(Context context, String label, String unit, float minimum, float maximum,
            int steps, int decimalPlaces) {
        super(context);
        this.minimum = minimum;
        this.maximum = maximum;
        this.unit = unit;
        this.steps = steps;
        this.decimalPlaces = decimalPlaces;
        this.preciseValue = minimum;
        setOrientation(VERTICAL);
        setPadding(dp(14), dp(10), dp(14), dp(12));
        setBackgroundResource(R.drawable.bg_setting_row);

        LinearLayout heading = new LinearLayout(context);
        heading.setOrientation(HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText(label);
        title.setTextSize(13);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setTextColor(context.getColor(R.color.text_secondary));
        heading.addView(title, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        value = new TextView(context);
        value.setTextSize(13);
        value.setTypeface(Typeface.MONOSPACE);
        value.setTextColor(context.getColor(R.color.accent));
        value.setBackgroundResource(R.drawable.bg_chip);
        value.setPadding(dp(9), dp(3), dp(9), dp(3));
        heading.addView(value);
        addView(heading, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        bar = new SeekBar(context);
        bar.setMax(steps);
        // The platform SeekBar draws a 2dp track and a small thumb, both of which
        // disappear against this palette. Supplying the drawables directly is the
        // only way to get a track that is visible and a thumb that is a
        // comfortable touch target on a phone.
        bar.setProgressDrawable(track(context));
        bar.setThumb(thumb(context));
        bar.setSplitTrack(false);
        bar.setPadding(dp(11), 0, dp(11), 0);
        bar.setContentDescription(label);
        LayoutParams barParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(40));
        barParams.topMargin = dp(4);
        addView(bar, barParams);

        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) preciseValue = minimum + (maximum - minimum) * progress / (float) steps;
                showValue();
                // Only a drag should rewrite the configuration; the initial
                // setValue() call must not look like a user edit.
                if (fromUser) listener.run();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        showValue();
    }

    void setValue(float current) {
        float clamped = Math.max(minimum, Math.min(maximum, current));
        preciseValue = clamped;
        bar.setProgress(Math.round((clamped - minimum) / (maximum - minimum) * steps));
        showValue();
    }

    float getValue() {
        return decimalPlaces >= 0 ? preciseValue
                : minimum + (maximum - minimum) * bar.getProgress() / (float) steps;
    }

    void onChanged(Runnable changed) {
        listener = changed;
    }

    void setAvailable(boolean available) {
        bar.setEnabled(available);
        setAlpha(available ? 1f : 0.42f);
    }

    private void showValue() {
        float current = getValue();
        if (decimalPlaces >= 0) {
            value.setText(String.format(Locale.ROOT, "%." + decimalPlaces + "f%s", current, unit));
            return;
        }
        value.setText(current >= 10f
                ? String.format(Locale.ROOT, "%.0f%s", current, unit)
                : String.format(Locale.ROOT, "%.1f%s", current, unit));
    }

    private android.graphics.drawable.Drawable track(Context context) {
        android.graphics.drawable.GradientDrawable background =
                new android.graphics.drawable.GradientDrawable();
        background.setColor(context.getColor(R.color.slider_track));
        background.setCornerRadius(dp(3));

        android.graphics.drawable.GradientDrawable filled =
                new android.graphics.drawable.GradientDrawable();
        filled.setColor(context.getColor(R.color.accent));
        filled.setCornerRadius(dp(3));

        android.graphics.drawable.LayerDrawable layers =
                new android.graphics.drawable.LayerDrawable(
                        new android.graphics.drawable.Drawable[]{
                                background,
                                new android.graphics.drawable.ClipDrawable(filled,
                                        android.view.Gravity.START,
                                        android.graphics.drawable.ClipDrawable.HORIZONTAL)});
        layers.setId(0, android.R.id.background);
        layers.setId(1, android.R.id.progress);
        // A fixed height keeps the track a bar rather than letting it stretch to
        // the SeekBar's touch height.
        layers.setLayerHeight(0, dp(6));
        layers.setLayerHeight(1, dp(6));
        layers.setLayerGravity(0, android.view.Gravity.CENTER_VERTICAL);
        layers.setLayerGravity(1, android.view.Gravity.CENTER_VERTICAL);
        return layers;
    }

    private android.graphics.drawable.Drawable thumb(Context context) {
        android.graphics.drawable.GradientDrawable shape =
                new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        shape.setColor(context.getColor(R.color.accent));
        shape.setStroke(dp(3), context.getColor(R.color.surface_row));
        shape.setSize(dp(22), dp(22));
        return shape;
    }

    private int dp(int amount) {
        return Math.round(amount * getResources().getDisplayMetrics().density);
    }
}

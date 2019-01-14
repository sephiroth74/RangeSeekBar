package it.sephiroth.android.rangeseekbardemo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import it.sephiroth.android.library.rangeseekbar.RangeSeekBar;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RangeSeekBar seekBar1 = findViewById(R.id.rangeSeekBar);
        final TextView textView1 = findViewById(R.id.textView2);

        final RangeSeekBar seekBar2 = findViewById(R.id.rangeSeekBar2);
        final TextView textView2 = findViewById(R.id.textView3);

        initializeSeekBar(seekBar1, textView1);
        initializeSeekBar(seekBar2, textView2);

    }

    private void initializeSeekBar(RangeSeekBar seekbar, TextView textView) {
        seekbar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
            @Override
            public void onProgressChanged(
                final RangeSeekBar seekBar, final int progressStart, final int progressEnd, final boolean fromUser) {
                updateRangeText(textView, seekBar);
            }

            @Override
            public void onStartTrackingTouch(final RangeSeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(final RangeSeekBar seekBar) { }
        });

        updateRangeText(textView, seekbar);
    }

    @SuppressLint ("SetTextI18n")
    private void updateRangeText(TextView textView, RangeSeekBar seekBar) {
        textView.setText(seekBar.getProgressStart() + " - " + seekBar.getProgressEnd());

    }
}

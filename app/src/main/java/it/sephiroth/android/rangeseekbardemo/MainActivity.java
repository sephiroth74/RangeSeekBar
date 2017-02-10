package it.sephiroth.android.rangeseekbardemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import it.sephiroth.android.library.rangeseekbar.RangeSeekBar;

public class MainActivity extends AppCompatActivity {
    RangeSeekBar mSeekbar1;
    RangeSeekBar mSeekbar2;
    TextView mTextView1;
    TextView mTextView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSeekbar1 = (RangeSeekBar) findViewById(R.id.rangeSeekBar);
        mSeekbar2 = (RangeSeekBar) findViewById(R.id.rangeSeekBar2);
        mTextView1 = (TextView) findViewById(R.id.textView2);
        mTextView2 = (TextView) findViewById(R.id.textView3);

        mSeekbar1.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
            @Override
            public void onProgressChanged(
                final RangeSeekBar seekBar, final int progressStart, final int progressEnd, final boolean fromUser) {
                updateRangeText(mTextView1, seekBar);
            }

            @Override
            public void onStartTrackingTouch(final RangeSeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(final RangeSeekBar seekBar) { }
        });

        mSeekbar2.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
            @Override
            public void onProgressChanged(
                final RangeSeekBar seekBar, final int progressStart, final int progressEnd, final boolean fromUser) {
                updateRangeText(mTextView2, seekBar);
            }

            @Override
            public void onStartTrackingTouch(final RangeSeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(final RangeSeekBar seekBar) { }
        });

        updateRangeText(mTextView1, mSeekbar1);
        updateRangeText(mTextView2, mSeekbar2);
    }

    private void updateRangeText(TextView textView, RangeSeekBar seekBar) {
        textView.setText(seekBar.getProgressStart() + " - " + seekBar.getProgressEnd());

    }
}

package com.example.jalkster;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwnerKt;

import com.example.jalkster.db.JalkDatabase;
import com.example.jalkster.db.JalkSessionDao;
import com.example.jalkster.ui.ActivityDistributionView;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;

public class StatsActivity extends AppCompatActivity {

    private static final int DEFAULT_SPINNER_INDEX = 1; // 1m (30 days)

    private JalkSessionDao jalkSessionDao;

    private TextView lastWeekSessionsValue;
    private TextView lastWeekJogTimeValue;
    private TextView lastWeekWalkTimeValue;
    private TextView lastWeekXcSkiTimeValue;
    private TextView lastWeekMovementTimeValue;

    private TextView customRangeSessionsValue;
    private TextView customRangeJogTimeValue;
    private TextView customRangeWalkTimeValue;
    private TextView customRangeXcSkiTimeValue;
    private TextView customRangeMovementTimeValue;

    private Spinner rangeSpinner;
    private ActivityDistributionView distributionLastWeek;
    private ActivityDistributionView distributionLastRange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        MaterialToolbar toolbar = findViewById(R.id.stats_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        jalkSessionDao = JalkDatabase.getInstance(getApplicationContext()).jalkSessionDao();

        lastWeekSessionsValue = findViewById(R.id.lastWeekSessionsValue);
        lastWeekJogTimeValue = findViewById(R.id.lastWeekJogTimeValue);
        lastWeekWalkTimeValue = findViewById(R.id.lastWeekWalkTimeValue);
        lastWeekXcSkiTimeValue = findViewById(R.id.lastWeekXcSkiTimeValue);
        lastWeekMovementTimeValue = findViewById(R.id.lastWeekMovementTimeValue);

        customRangeSessionsValue = findViewById(R.id.customRangeSessionsValue);
        customRangeJogTimeValue = findViewById(R.id.customRangeJogTimeValue);
        customRangeWalkTimeValue = findViewById(R.id.customRangeWalkTimeValue);
        customRangeXcSkiTimeValue = findViewById(R.id.customRangeXcSkiTimeValue);
        customRangeMovementTimeValue = findViewById(R.id.customRangeMovementTimeValue);

        distributionLastWeek = findViewById(R.id.distributionLastWeek);
        distributionLastRange = findViewById(R.id.distributionLastRange);
        if (distributionLastWeek != null) {
            distributionLastWeek.setTimes(0, 0, 0, 0);
        }
        if (distributionLastRange != null) {
            distributionLastRange.setTimes(0, 0, 0, 0);
        }

        rangeSpinner = findViewById(R.id.rangeSpinner);
        setupSpinner();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.range_options,
                R.layout.spinner_item_white);
        adapter.setDropDownViewResource(R.layout.spinner_item_white);
        rangeSpinner.setAdapter(adapter);

        rangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadStatsForRanges(getDaysForPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No-op
            }
        });

        rangeSpinner.setSelection(DEFAULT_SPINNER_INDEX, false);
        loadStatsForRanges(getDaysForPosition(DEFAULT_SPINNER_INDEX));
    }

    private int getDaysForPosition(int position) {
		int[] days = getResources().getIntArray(R.array.range_options_values);
		if (position > days.length - 1 || position < 0) {
			return 30;
		}
		return days[position];
    }

    private void loadStatsForRanges(final int customRangeDays) {
        final long now = System.currentTimeMillis();
        final long sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7);
        final long customRangeStart = now - TimeUnit.DAYS.toMillis(customRangeDays);

        BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(this), Dispatchers.getIO(), CoroutineStart.DEFAULT,
                new kotlin.jvm.functions.Function2<CoroutineScope, Continuation<? super Unit>, Object>() {
                    @Override
                    public Object invoke(CoroutineScope coroutineScope, Continuation<? super Unit> continuation) {
                        final int totalSessionsSevenDays = jalkSessionDao.getTotalSessionsSince(sevenDaysAgo);
                        final int totalSessionsCustomRange = jalkSessionDao.getTotalSessionsSince(customRangeStart);

                        Long jogSevenDaysMillis = jalkSessionDao.getTotalJogMillisSince(sevenDaysAgo);
                        Long jogCustomRangeMillis = jalkSessionDao.getTotalJogMillisSince(customRangeStart);
                        Long walkSevenDaysMillis = jalkSessionDao.getTotalWalkMillisSince(sevenDaysAgo);
                        Long walkCustomRangeMillis = jalkSessionDao.getTotalWalkMillisSince(customRangeStart);
                        Long restSevenDaysMillis = jalkSessionDao.getTotalRestMillisSince(sevenDaysAgo);
                        Long restCustomRangeMillis = jalkSessionDao.getTotalRestMillisSince(customRangeStart);
                        Long xcSkiSevenDaysMillis = jalkSessionDao.getTotalXcSkiMillisSince(sevenDaysAgo);
                        Long xcSkiCustomRangeMillis = jalkSessionDao.getTotalXcSkiMillisSince(customRangeStart);

                        if (jogSevenDaysMillis == null) jogSevenDaysMillis = 0L;
                        if (jogCustomRangeMillis == null) jogCustomRangeMillis = 0L;
                        if (walkSevenDaysMillis == null) walkSevenDaysMillis = 0L;
                        if (walkCustomRangeMillis == null) walkCustomRangeMillis = 0L;
                        if (restSevenDaysMillis == null) restSevenDaysMillis = 0L;
                        if (restCustomRangeMillis == null) restCustomRangeMillis = 0L;
                        if (xcSkiSevenDaysMillis == null) xcSkiSevenDaysMillis = 0L;
                        if (xcSkiCustomRangeMillis == null) xcSkiCustomRangeMillis = 0L;

                        final long finalJogSevenDaysMillis = jogSevenDaysMillis;
                        final long finalJogCustomRangeMillis = jogCustomRangeMillis;
                        final long finalWalkSevenDaysMillis = walkSevenDaysMillis;
                        final long finalWalkCustomRangeMillis = walkCustomRangeMillis;
                        final long finalRestSevenDaysMillis = restSevenDaysMillis;
                        final long finalRestCustomRangeMillis = restCustomRangeMillis;
                        final long finalXcSkiSevenDaysMillis = xcSkiSevenDaysMillis;
                        final long finalXcSkiCustomRangeMillis = xcSkiCustomRangeMillis;
                        final long finalMovementSevenDaysMillis = jogSevenDaysMillis
                                + walkSevenDaysMillis
                                + xcSkiSevenDaysMillis;
                        final long finalMovementCustomRangeMillis = jogCustomRangeMillis
                                + walkCustomRangeMillis
                                + xcSkiCustomRangeMillis;

                        runOnUiThread(() -> {
                            lastWeekSessionsValue.setText(String.valueOf(totalSessionsSevenDays));
                            lastWeekJogTimeValue.setText(formatDuration(finalJogSevenDaysMillis));
                            lastWeekWalkTimeValue.setText(formatDuration(finalWalkSevenDaysMillis));
                            lastWeekXcSkiTimeValue.setText(formatDuration(finalXcSkiSevenDaysMillis));
                            lastWeekMovementTimeValue.setText(formatDuration(finalMovementSevenDaysMillis));
                            if (distributionLastWeek != null) {
                                distributionLastWeek.setTimes(finalJogSevenDaysMillis,
                                        finalWalkSevenDaysMillis,
                                        finalRestSevenDaysMillis,
                                        finalXcSkiSevenDaysMillis);
                            }

                            customRangeSessionsValue.setText(String.valueOf(totalSessionsCustomRange));
                            customRangeJogTimeValue.setText(formatDuration(finalJogCustomRangeMillis));
                            customRangeWalkTimeValue.setText(formatDuration(finalWalkCustomRangeMillis));
                            customRangeXcSkiTimeValue.setText(formatDuration(finalXcSkiCustomRangeMillis));
                            customRangeMovementTimeValue.setText(formatDuration(finalMovementCustomRangeMillis));
                            if (distributionLastRange != null) {
                                distributionLastRange.setTimes(finalJogCustomRangeMillis,
                                        finalWalkCustomRangeMillis,
                                        finalRestCustomRangeMillis,
                                        finalXcSkiCustomRangeMillis);
                            }
                        });
                        return Unit.INSTANCE;
                    }
                });
    }

    private String formatDuration(long durationMillis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(Locale.getDefault(), "%dm %02ds", minutes, seconds);
    }
}

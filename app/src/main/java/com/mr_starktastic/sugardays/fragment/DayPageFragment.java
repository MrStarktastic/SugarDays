package com.mr_starktastic.sugardays.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.data.BloodSugar;
import com.mr_starktastic.sugardays.data.Day;
import com.mr_starktastic.sugardays.data.SugarEntry;
import com.mr_starktastic.sugardays.util.NumericTextUtil;
import com.mr_starktastic.sugardays.util.PrefUtil;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.time.FastDateFormat;

public class DayPageFragment extends Fragment {
    private static final String ARG_DAY_ID = "DAY_CODE";

    private static final FastDateFormat TIME_FORMAT =
            FastDateFormat.getTimeInstance(FastDateFormat.SHORT);

    private static SpaceDecoration spaceDecoration;

    private int dayId;

    private OnLogCardSelectedListener listener;

    private RecyclerView recyclerView;

    public DayPageFragment() {
        // Required empty public constructor
    }

    /**
     * A factory method creates a new instance of this fragment using the provided parameters.
     *
     * @param dayId The required day's ID.
     * @return A new instance of fragment {@link DayPageFragment}.
     */
    public static DayPageFragment newInstance(int dayId) {
        final DayPageFragment fragment = new DayPageFragment();
        final Bundle args = new Bundle();
        args.putInt(ARG_DAY_ID, dayId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();

        if (args != null)
            dayId = args.getInt(ARG_DAY_ID);
        if (spaceDecoration == null)
            spaceDecoration = new SpaceDecoration(
                    getResources().getDimensionPixelSize(R.dimen.normal_margin));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_day_page, container, false);
        final Context context = getContext();

        recyclerView = (RecyclerView) root.findViewById(R.id.day_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.addItemDecoration(spaceDecoration);
        /*// Nested scrolling causes odd behavior with the calendar view; workaround needed
        ViewCompat.setNestedScrollingEnabled(recyclerView, false);*/

        final Day day = Day.findById(dayId);

        if (day != null) {
            recyclerView.setAdapter(new LogAdapter(context, day.getEntries()));
            root.findViewById(R.id.empty_day_text).setVisibility(View.GONE);
        } else root.findViewById(R.id.empty_day_text).setVisibility(View.VISIBLE);

        return root;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnLogCardSelectedListener)
            listener = (OnLogCardSelectedListener) context;
        else throw new RuntimeException(context.toString() +
                " must implement OnLogCardSelectedListener");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface OnLogCardSelectedListener {
        void onLogCardSelected(int dayId, int entryIndex, View sharedView);
    }

    private static class SpaceDecoration extends RecyclerView.ItemDecoration {
        private int space;

        private SpaceDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            outRect.left = space;
            outRect.right = space;
            outRect.bottom = space;

            if (parent.getChildAdapterPosition(view) == 0)
                outRect.top = space;
        }
    }

    private class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
        private Context context;
        private SugarEntry[] entries;
        private String[] types;
        private int bgUnitIdx;
        private String bgUnit;

        private float hypo, minTargetRng, maxTargetRng, hyper;
        private int badBgColor, medBgColor, goodBgColor;

        private LogAdapter(Context context, SugarEntry[] entries) {
            this.context = context;
            this.entries = entries;
            types = context.getResources().getStringArray(R.array.entry_types);
            bgUnitIdx = PrefUtil.getBgUnitIdx(
                    PreferenceManager.getDefaultSharedPreferences(context));
            bgUnit = context.getResources().getStringArray(R.array.pref_bgUnits_entries)[bgUnitIdx];
            final SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(context);
            hypo = PrefUtil.getHypo(preferences).get(bgUnitIdx);
            final BloodSugar[] targetRngBG = PrefUtil.getTargetRange(preferences);
            minTargetRng = targetRngBG[0].get(bgUnitIdx);
            maxTargetRng = targetRngBG[1].get(bgUnitIdx);
            hyper = PrefUtil.getHyper(preferences).get(bgUnitIdx);
            badBgColor = ContextCompat.getColor(context, R.color.colorBadBloodGlucose);
            medBgColor = ContextCompat.getColor(context, R.color.colorIntermediateBloodGlucose);
            goodBgColor = ContextCompat.getColor(context, R.color.colorGoodBloodGlucose);
        }

        @Override
        public LogAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder((CardView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_view_sugar, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final SugarEntry entry = entries[position];
            final String photoPath = entry.getPhotoPath();

            if (photoPath != null)
                Picasso.with(context).load(photoPath).fit().centerCrop().into(holder.imgView);

            holder.typeText.setText(types[entry.getType()]);
            holder.timeText.setText(TIME_FORMAT.format(entry.getTime()));
            final String location = entry.getLocation();

            if (location != null)
                holder.locationText.setText(location);
            else holder.locationText.setVisibility(View.GONE);

            final BloodSugar bg = entry.getBloodSugar();

            if (bg != null) {
                final float val = bg.get(bgUnitIdx);
                ViewCompat.setBackgroundTintList(holder.bgText, ColorStateList.valueOf(
                        val <= hypo || val >= hyper ? badBgColor : val >= minTargetRng &&
                                val <= maxTargetRng ? goodBgColor : medBgColor));
                holder.bgText.setText(NumericTextUtil.trim(val) + " " + bgUnit);
            } else holder.bgText.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return entries.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private ImageView imgView;
            private TextView typeText, timeText, locationText, bgText;

            private ViewHolder(CardView cardView) {
                super(cardView);
                cardView.setOnClickListener(this);

                imgView = (ImageView) cardView.findViewById(R.id.photo);
                typeText = (TextView) cardView.findViewById(R.id.entry_type_text);
                timeText = (TextView) cardView.findViewById(R.id.entry_time_text);
                locationText = (TextView) cardView.findViewById(R.id.entry_location_text);
                bgText = (TextView) cardView.findViewById(R.id.entry_blood_glucose_text);
            }

            @Override
            public void onClick(View view) {
                if (listener != null)
                    listener.onLogCardSelected(dayId, getAdapterPosition(), view);
            }
        }
    }
}

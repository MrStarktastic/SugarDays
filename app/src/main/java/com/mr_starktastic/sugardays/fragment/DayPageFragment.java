package com.mr_starktastic.sugardays.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.net.Uri;
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
import com.mr_starktastic.sugardays.data.Log;
import com.mr_starktastic.sugardays.util.NumericTextUtil;
import com.mr_starktastic.sugardays.util.PrefUtil;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.time.FastDateFormat;

import java.util.ArrayList;

import io.paperdb.Paper;

public class DayPageFragment extends Fragment {
    private static final String ARG_DAY_CODE = "DAY_CODE";

    private static final FastDateFormat TIME_FORMAT =
            FastDateFormat.getTimeInstance(FastDateFormat.SHORT);

    private static SpaceDecoration spaceDecoration;

    private String dayCode;

    private OnFragmentInteractionListener listener;

    private RecyclerView recyclerView;

    public DayPageFragment() {
        // Required empty public constructor
    }

    /**
     * A factory method creates a new instance of this fragment using the provided parameters.
     *
     * @param dayCode A year + month + day_of_month combination of the required day.
     * @return A new instance of fragment {@link DayPageFragment}.
     */
    public static DayPageFragment newInstance(String dayCode) {
        final DayPageFragment fragment = new DayPageFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_DAY_CODE, dayCode);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();

        if (args != null)
            dayCode = args.getString(ARG_DAY_CODE);
        if (spaceDecoration == null)
            spaceDecoration = new SpaceDecoration(
                    getResources().getDimensionPixelSize(R.dimen.normal_margin));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_day_page, container, false);

        recyclerView = (RecyclerView) root.findViewById(R.id.day_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ViewCompat.setNestedScrollingEnabled(recyclerView, false);
        recyclerView.addItemDecoration(spaceDecoration);

        final Day day = Paper.book().read(dayCode);
        final ArrayList<Log> logs;

        if (day != null && (logs = day.getLogs()) != null) {
            recyclerView.setAdapter(new LogAdapter(getContext(), logs));
        } else recyclerView.setAdapter(new LogAdapter(getContext()));

        return root;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (listener != null)
            listener.onFragmentInteraction(uri);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
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

    private static class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {
        private Context context;
        private ArrayList<Log> logs;
        private String[] types;
        private int bgUnitIdx;
        private String bgUnit;

        private float hypo, minTargetRng, maxTargetRng, hyper;
        private int badBgColor, medBgColor, goodBgColor;

        private LogAdapter(Context context) {
            this.context = context;
            logs = new ArrayList<>();
        }

        private LogAdapter(Context context, ArrayList<Log> logs) {
            this.context = context;
            this.logs = logs;
            types = context.getResources().getStringArray(R.array.log_types);
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
            final Log log = logs.get(position);
            final String photoPath = log.getPhotoPath();

            if (photoPath != null)
                Picasso.with(context).load(photoPath).fit().centerCrop().into(holder.imgView);

            holder.typeText.setText(types[log.getType()]);
            holder.timeText.setText(TIME_FORMAT.format(log.getTime()));
            final String location = log.getLocation();

            if (location != null)
                holder.locationText.setText(location);
            else holder.locationText.setVisibility(View.GONE);

            final BloodSugar bg = log.getBloodSugar();

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
            return logs.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView imgView;
            private TextView typeText, timeText, locationText, bgText;

            private ViewHolder(CardView cardView) {
                super(cardView);

                imgView = (ImageView) cardView.findViewById(R.id.photo);
                typeText = (TextView) cardView.findViewById(R.id.log_type_text);
                timeText = (TextView) cardView.findViewById(R.id.log_time_text);
                locationText = (TextView) cardView.findViewById(R.id.log_location_text);
                bgText = (TextView) cardView.findViewById(R.id.log_blood_glucose_text);
            }
        }
    }
}

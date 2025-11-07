package com.quantiagents.app.ui.myevents;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quantiagents.app.R;
import com.quantiagents.app.models.Event;

import java.util.ArrayList;
import java.util.List;

/** Adapter for BrowseEvents list. */
public class BrowseEventsAdapter extends RecyclerView.Adapter<BrowseEventsAdapter.EventVH> {

    public interface OnEventClick {
        void onJoinWaitlist(@NonNull Event event);
        void onViewEvent(@NonNull Event event);
    }

    private final List<Event> data;
    private final OnEventClick cb;

    public BrowseEventsAdapter(List<Event> initial, OnEventClick cb) {
        this.data = initial == null ? new ArrayList<>() : new ArrayList<>(initial);
        this.cb = cb;
    }
    public void setData(List<Event> newEvents) {
        data.clear();
        data.addAll(newEvents);
    }

    public void replace(List<Event> next) {
        data.clear();
        if (next != null) data.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_browse_event, parent, false);
        return new EventVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EventVH h, int pos) {
        h.bind(data.get(pos), cb);
    }

    @Override
    public int getItemCount() { return data.size(); }

    static final class EventVH extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView subtitle;
        private final TextView textClosed;
        private final Button join;
        private final Button view;

        EventVH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_title);
            subtitle = itemView.findViewById(R.id.text_subtitle);
            textClosed = itemView.findViewById(R.id.text_closed);
            join = itemView.findViewById(R.id.button_join);
            view = itemView.findViewById(R.id.button_view);
        }

        void bind(final Event e, final OnEventClick cb) {
            title.setText(nullSafe(e.getTitle()));
            // fallback empty
            String desc = "";
            try {
                Object d = Event.class.getMethod("getDescription").invoke(e);
                if (d instanceof String) desc = (String) d;
            } catch (Exception ignore) {}
            subtitle.setText(desc);

            boolean open = BrowseEventsFragment.isOpen(e);
            textClosed.setVisibility(open ? View.GONE : View.VISIBLE);
            join.setVisibility(open ? View.VISIBLE : View.GONE);

            join.setOnClickListener(v -> cb.onJoinWaitlist(e));
            view.setOnClickListener(v -> cb.onViewEvent(e));
            itemView.setOnClickListener(v -> cb.onViewEvent(e));
        }

        private static String nullSafe(String s) { return s == null ? "" : s; }
    }
}
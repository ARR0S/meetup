package com.example.meetup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.ViewHolder> {
    private List<Event> events;
    private EventClickListener listener;

    public EventsAdapter(List<Event> events, EventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_item, parent, false);
        return new ViewHolder(view, listener, events);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);
        holder.nameTextView.setText(event.getName());
        holder.dateTextView.setText(event.getDate());
        holder.timeTextView.setText(event.getTime());
        holder.placeTextView.setText(event.getPlace());
        holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView dateTextView;
        TextView timeTextView;
        TextView placeTextView;

        public ViewHolder(View itemView, final EventClickListener listener, List<Event> events) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.eventNameTextView);
            dateTextView = itemView.findViewById(R.id.eventDateTextView);
            timeTextView = itemView.findViewById(R.id.eventTimeTextView);
            placeTextView = itemView.findViewById(R.id.eventPlaceTextView);

            itemView.setOnClickListener(view -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    Event event = events.get(position);
                    listener.onEventClick(event);
                }
            });
        }
    }


    public interface EventClickListener {
        void onEventClick(Event event);
    }

}


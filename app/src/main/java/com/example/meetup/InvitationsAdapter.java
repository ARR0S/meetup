package com.example.meetup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class InvitationsAdapter extends RecyclerView.Adapter<InvitationsAdapter.ViewHolder> {
    private List<Invitation> invitations;
    private OnInvitationClickListener listener;

    public interface OnInvitationClickListener {
        void onInvitationClick(Invitation invitation);
    }

    public InvitationsAdapter(List<Invitation> invitations, OnInvitationClickListener listener) {
        this.invitations = invitations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.invitation_item, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Invitation invitation = invitations.get(position);
        holder.invitationName.setText(invitation.getFullName());
        if (Boolean.TRUE.equals(invitation.getIsAccepted())) {
            holder.invitationStatusIcon.setImageResource(R.drawable.ic_check_green);
        } else {
            holder.invitationStatusIcon.setImageResource(R.drawable.ic_cross_red);
        }
        holder.bind(position, listener, invitations);
    }

    @Override
    public int getItemCount() {
        return invitations != null ? invitations.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView invitationName;
        ImageView invitationStatusIcon;

        public ViewHolder(View itemView, final OnInvitationClickListener listener) {
            super(itemView);
            invitationName = itemView.findViewById(R.id.invitationName);
            invitationStatusIcon = itemView.findViewById(R.id.invitationStatusIcon);
        }

        public void bind(final int position, final OnInvitationClickListener listener, List<Invitation> invitations) {
            itemView.setOnClickListener(v -> {
                if (position != RecyclerView.NO_POSITION) {
                    listener.onInvitationClick(invitations.get(position));
                }
            });
        }
    }
}


package com.example.meetup;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InvitationsActivity extends AppCompatActivity implements InvitationsAdapter.OnInvitationClickListener {
    private RecyclerView recyclerView;
    private InvitationsAdapter adapter;
    private List<Invitation> invitations = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String eventId;
    private ImageButton addInvitationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        recyclerView = findViewById(R.id.invitationsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InvitationsAdapter(invitations, this);
        recyclerView.setAdapter(adapter);

        eventId = getIntent().getStringExtra("eventId");
        if (eventId != null) {
            loadInvitations(eventId);
        }

        addInvitationButton = findViewById(R.id.addInvitationButton);
        addInvitationButton.setOnClickListener(view -> {
            showAddInvitationDialog();
        });
    }
    private void showAddInvitationDialog() {
        Dialog dialog = new Dialog(this, R.style.CustomDialog);
        dialog.setContentView(R.layout.dialog_add_invitation);
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button saveButton = dialog.findViewById(R.id.saveButton);
        EditText text = dialog.findViewById(R.id.editText);

        cancelButton.setOnClickListener(v -> { dialog.dismiss();});
        saveButton.setOnClickListener(v -> { String fullName = text.getText().toString().trim();
            if (!fullName.isEmpty()) {
                addInvitationToFirestore(eventId, fullName);
            } else {
                Toast.makeText(InvitationsActivity.this, "Неверный ввод", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();});
        dialog.show();
    }

    private void addInvitationToFirestore(String eventId, String fullName) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && eventId != null) {
            String userId = currentUser.getUid();
            Map<String, Object> invitationData = new HashMap<>();
            invitationData.put("fullName", fullName);
            invitationData.put("isAccepted", false);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId)
                    .collection("events").document(eventId)
                    .collection("invitations")
                    .add(invitationData)
                    .addOnSuccessListener(documentReference -> {
                        String qrCodeString = Invitation.generateQRCodeString(this, eventId, documentReference.getId());
                        documentReference
                                .update("qrCodeString", qrCodeString)
                                .addOnSuccessListener(aVoid -> {
                                    Invitation newInvitation = new Invitation(fullName, qrCodeString);
                                    newInvitation.setId(documentReference.getId());

                                    runOnUiThread(() -> {
                                        invitations.add(newInvitation);
                                        adapter.notifyItemInserted(invitations.size() - 1);
                                    });
                                })
                                .addOnFailureListener(e -> Log.w("InvitationsActivity", "Error setting QR code for invitation", e));

                        Toast.makeText(InvitationsActivity.this, "Приглашение добавлено", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(InvitationsActivity.this, "Ошибка при добавлении приглашения", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Вы должны авторизоваться", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadInvitations(String eventId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).collection("events").document(eventId)
                    .collection("invitations")
                    .orderBy("fullName")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        invitations.clear();
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            Invitation invitation = documentSnapshot.toObject(Invitation.class);
                            if (invitation != null) {
                                invitation.setId(documentSnapshot.getId());
                                String qrCodeString = Invitation.generateQRCodeString(this, eventId, documentSnapshot.getId());
                                DocumentReference invitationRef = db.collection("users").document(userId)
                                        .collection("events").document(eventId)
                                        .collection("invitations").document(documentSnapshot.getId());

                                invitationRef
                                        .update("qrCodeString", qrCodeString)
                                        .addOnSuccessListener(aVoid -> {
                                            invitation.setQrCodeString(qrCodeString);
                                            invitations.add(invitation);
                                            adapter.notifyItemInserted(invitations.size() - 1);
                                        })
                                        .addOnFailureListener(e -> Log.w("InvitationsActivity", "Error setting QR code for invitation", e));
                            }
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(InvitationsActivity.this, "Ошибка загрузки приглашений", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Вы должны авторизоваться", Toast.LENGTH_SHORT).show();
        }
    }



    private void showInvitationActionsDialog(Invitation invitation) {
        Dialog dialog = new Dialog(this, R.style.CustomDialog);
        dialog.setContentView(R.layout.dialog_invitation_details);
        Button sendQRButton = dialog.findViewById(R.id.sendQRButton);
        Button showQRButton = dialog.findViewById(R.id.showQRButton);
        Button deleteButton = dialog.findViewById(R.id.deleteButton);
        TextView name = dialog.findViewById(R.id.nameTextView);
        name.setText(invitation.getFullName());
        sendQRButton.setOnClickListener(v -> {fetchEventDetailsAndSendEmail(invitation); dialog.dismiss();});
        showQRButton.setOnClickListener(v -> {showQRCode(invitation); dialog.dismiss();});
        deleteButton.setOnClickListener(v -> {deleteInvitation(invitation); dialog.dismiss();});
        dialog.show();
    }
    private void fetchEventDetailsAndSendEmail(Invitation invitation) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && eventId != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(currentUser.getUid()).collection("events").document(eventId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String eventName = documentSnapshot.getString("name");
                            String eventDate = documentSnapshot.getString("date");
                            String eventTime = documentSnapshot.getString("time");
                            String eventPlace = documentSnapshot.getString("place");
                            sendQRCodeByEmail(invitation, eventName, eventDate, eventTime, eventPlace);
                        } else {
                            Log.d("InvitationsActivity", "Event not found");
                        }
                    })
                    .addOnFailureListener(e -> Log.w("InvitationsActivity", "Error getting event details", e));
        }
    }

    @Override
    public void onInvitationClick(Invitation invitation) {
        showInvitationActionsDialog(invitation);
    }
    private void showQRCode(Invitation invitation) {
        byte[] decodedString = Base64.decode(invitation.getQrCodeString(), Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(decodedByte);
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(imageView);
        imageView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }



    private void sendQRCodeByEmail(Invitation invitation, String eventName, String eventDate, String eventTime, String eventPlace) {
            createAndSendEmail(invitation, eventName, eventDate, eventTime, eventPlace);
    }

    private void createAndSendEmail(Invitation invitation, String eventName, String eventDate, String eventTime, String eventPlace) {
        ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        emailExecutor.execute(() -> {
            byte[] decodedString = Base64.decode(invitation.getQrCodeString(), Base64.DEFAULT);
            Bitmap qrCodeBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            Bitmap combinedBitmap = createImageWithTextAndQrCode(eventName, eventDate, eventTime, eventPlace, qrCodeBitmap);

            handler.post(() -> sendEmailWithBitmap(combinedBitmap, invitation, eventName, eventDate, eventTime, eventPlace));
        });
    }

    private Bitmap createImageWithTextAndQrCode(String eventName, String eventDate, String eventTime, String eventPlace, Bitmap qrCodeBitmap) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40);
        textPaint.setTextAlign(Paint.Align.LEFT);
        int textHeight = (int) (textPaint.descent() - textPaint.ascent()) * 4+6;
        Bitmap combinedBitmap = Bitmap.createBitmap(qrCodeBitmap.getWidth(), qrCodeBitmap.getHeight() + textHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(combinedBitmap);
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, combinedBitmap.getWidth(), textHeight, backgroundPaint);
        float x = 20;
        float y = 40;
        canvas.drawText("Мероприятие: " + eventName, x, y, textPaint);
        y += textPaint.descent() - textPaint.ascent();
        canvas.drawText("Дата: " + eventDate, x, y, textPaint);
        y += textPaint.descent() - textPaint.ascent();
        canvas.drawText("Время: " + eventTime, x, y, textPaint);
        y += textPaint.descent() - textPaint.ascent();
        canvas.drawText("Место: " + eventPlace, x, y, textPaint);
        canvas.drawBitmap(qrCodeBitmap, 0, textHeight, null);
        return combinedBitmap;
    }


    private void sendEmailWithBitmap(Bitmap bitmap, Invitation invitation, String eventName, String eventDate, String eventTime, String eventPlace ) {
        Uri bmpUri = getContentUriFromBitmap(bitmap, invitation);
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("image/png");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Приглашение на " + eventName);
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Здравствуйте, " + invitation.getFullName() + ", вы были приглашены на мероприятие " + eventName + ".\nВремя: " + eventDate+ " " + eventTime+".\nМесто проведения: "+eventPlace+".\nВаш QR-code прикреплен к сообщению:");
        emailIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
        startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }

    @SuppressLint("RestrictedApi")
    private Uri getContentUriFromBitmap(Bitmap bitmap, Invitation invitation) {
        final File file = new File(getCacheDir(), invitation.getId() + ".png");
        try {
            FileOutputStream cacheStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, cacheStream);
            cacheStream.flush();
            cacheStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return FileProvider.getUriForFile(this, "com.example.meetup.provider", file);
    }

    private void deleteInvitation(Invitation invitation) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && invitation.getId() != null && eventId != null) {
            String userId = currentUser.getUid();

            db.collection("users").document(userId)
                    .collection("events").document(eventId)
                    .collection("invitations").document(invitation.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        int position = invitations.indexOf(invitation);
                        if (position > -1) {
                            invitations.remove(position);
                            adapter.notifyItemRemoved(position);
                        }
                        Toast.makeText(InvitationsActivity.this, "Приглашение удалено", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка при удалении", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Потеряна информация", Toast.LENGTH_SHORT).show();
        }
    }


}

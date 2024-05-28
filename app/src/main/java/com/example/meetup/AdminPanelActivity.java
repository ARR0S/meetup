package com.example.meetup;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminPanelActivity extends AppCompatActivity {

    private ImageButton addEventButton;
    private RecyclerView eventsRecyclerView;
    private List<Event> eventsList = new ArrayList<>();
    private EventsAdapter eventsAdapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        eventsAdapter = new EventsAdapter(eventsList, this::showEventActionsDialog);
        addEventButton = findViewById(R.id.addEventButton);
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView);

        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventsRecyclerView.setAdapter(eventsAdapter);
        addEventButton.setOnClickListener(view -> showAddEventDialog());

        loadEvents();
    }

    private void loadEvents() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).collection("events")
                    .orderBy("date", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        eventsList.clear();
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                            Event event = documentSnapshot.toObject(Event.class);
                            if (event != null) {
                                event.setId(documentSnapshot.getId());
                                eventsList.add(event);
                            }
                        }
                        eventsAdapter.notifyDataSetChanged();
                    })

                    .addOnFailureListener(e -> Toast.makeText(AdminPanelActivity.this, "Error loading events", Toast.LENGTH_SHORT).show());
        }
    }


    private void showAddEventDialog() {
        Dialog dialog = new Dialog(this,R.style.CustomDialog);
        dialog.setContentView(R.layout.dialog_add_event);
        EditText eventNameEditText = dialog.findViewById(R.id.eventNameEditText);
        EditText eventIdEditText = dialog.findViewById(R.id.eventIdEditText);
        TextView eventDateTextView = dialog.findViewById(R.id.eventDateTextView);
        TextView eventTimeTextView = dialog.findViewById(R.id.eventTimeTextView);
        EditText eventLocationTextView = dialog.findViewById(R.id.eventLocationTextView);
        Button saveEventButton = dialog.findViewById(R.id.saveEventButton);
        Button cancelEventButton = dialog.findViewById(R.id.cancelEventButton);

        eventDateTextView.setOnClickListener(v -> showDatePickerDialog(eventDateTextView));

        eventTimeTextView.setOnClickListener(v -> showTimePickerDialog(eventTimeTextView, eventDateTextView));


        saveEventButton.setOnClickListener(v -> {
            String eventName = eventNameEditText.getText().toString();
            String eventId = eventIdEditText.getText().toString();
            String eventDate = eventDateTextView.getText().toString();
            String eventTime = eventTimeTextView.getText().toString();
            String eventPlace = eventLocationTextView.getText().toString();

            if (eventName.isEmpty() || eventId.isEmpty() || eventDate.isEmpty() || eventPlace.isEmpty() || (eventDateTextView.getVisibility() == View.VISIBLE && eventTime.isEmpty())) {
                Toast.makeText(AdminPanelActivity.this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
            } else {
                checkEventIdAndAdd(eventName, eventId, eventDate, eventTime, eventPlace, dialog);
            }
        });

        cancelEventButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showDatePickerDialog(final TextView dateTextView) {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            // Устанавливаем время на начало дня для сравнения
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 99);
            if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
                dateTextView.setText(new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(calendar.getTime()));
            } else {
                Toast.makeText(AdminPanelActivity.this, "Пожалуйста, выберите будущую дату", Toast.LENGTH_SHORT).show();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showTimePickerDialog(final TextView timeTextView, TextView eventDateTextView) {
        final Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);

            // Проверка на выбранную дату
            Calendar selectedDate = Calendar.getInstance();
            try {
                selectedDate.setTime(new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).parse(eventDateTextView.getText().toString()));
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDate.set(Calendar.MINUTE, minute);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (selectedDate.getTimeInMillis() >= System.currentTimeMillis()) {
                timeTextView.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.getTime()));
            } else {
                Toast.makeText(AdminPanelActivity.this, "Пожалуйста, выберите будущее время", Toast.LENGTH_SHORT).show();
            }
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
        timePickerDialog.show();
    }
    private void checkEventIdAndAdd(String name, String id, String date, String time, String place, Dialog dialog) {
        db.collection("events").document(id).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Toast.makeText(AdminPanelActivity.this, "Событие с таким идентификатором уже существует", Toast.LENGTH_SHORT).show();
                } else {
                    addEvent(name, id, date, time, place);
                    dialog.dismiss();
                }
            } else {
                Toast.makeText(AdminPanelActivity.this, "Ошибка при проверке идентификатора", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addEvent(String name, String id, String date, String time, String place) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Map<String, Object> event = new HashMap<>();
            event.put("name", name);
            event.put("date", date);
            event.put("time", time);
            event.put("place", place);
            db.collection("users").document(userId).collection("events").document(id).set(event)
                    .addOnSuccessListener(documentReference -> {
                        Map<String, Object> eventIdMap = new HashMap<>();
                        eventIdMap.put("userid", userId);
                        db.collection("events").document(id).set(eventIdMap)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(AdminPanelActivity.this, "Мероприятие добавлено", Toast.LENGTH_SHORT).show();
                                    loadEvents();
                                })
                                .addOnFailureListener(e -> Toast.makeText(AdminPanelActivity.this, "Ошибка при добавлении идентификатора мероприятия", Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e -> Toast.makeText(AdminPanelActivity.this, "Ошибка при добавлении", Toast.LENGTH_SHORT).show());

        } else {
            Toast.makeText(this, "Вы должны авторизоваться", Toast.LENGTH_SHORT).show();
        }
    }


    private void showEventActionsDialog(Event event) {
        Dialog dialog = new Dialog(this, R.style.CustomDialog);
        dialog.setContentView(R.layout.dialog_event_details);
        Button addInvitationButton = dialog.findViewById(R.id.addInvitationButton);
        Button showInvitationsButton = dialog.findViewById(R.id.showInvitationsButton);
        Button showCodeButton = dialog.findViewById(R.id.showCodeButton);
        Button deleteEventButton = dialog.findViewById(R.id.deleteEventButton);
        TextView name = dialog.findViewById(R.id.eventNameTextView);
        name.setText(event.getName());
        addInvitationButton.setOnClickListener(v -> {showAddInvitationDialog(event); dialog.dismiss();});
        showInvitationsButton.setOnClickListener(v -> {viewInvitations(event); dialog.dismiss();});
        showCodeButton.setOnClickListener(v -> {showEventCode(event); dialog.dismiss();});
        deleteEventButton.setOnClickListener(v -> {deleteEvent(event); dialog.dismiss();});
        dialog.show();
    }
    private void showEventCode(Event event) {
        Dialog dialog = new Dialog(this, R.style.CustomDialog);
        dialog.setContentView(R.layout.dialog_code);
        Button copyButton = dialog.findViewById(R.id.copyButton);
        Button okButton = dialog.findViewById(R.id.okButton);
        TextView name = dialog.findViewById(R.id.eventNameTextView);
        TextView code = dialog.findViewById(R.id.codeView);
        name.setText(event.getName());
        code.setText(event.getId());
        copyButton.setOnClickListener(v -> {copyId(event.getId()); dialog.dismiss();});
        okButton.setOnClickListener(v -> {dialog.dismiss();});
        dialog.show();
    }

    private void copyId(String eventId) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Event ID", eventId);
        clipboard.setPrimaryClip(clip);
    }

    private void viewInvitations(Event event) {
        Intent intent = new Intent(this, InvitationsActivity.class);
        intent.putExtra("eventId", event.getId());
        startActivity(intent);
    }
    private void showAddInvitationDialog(Event event) {
        Dialog dialog = new Dialog(this, R.style.CustomDialog);
        dialog.setContentView(R.layout.dialog_add_invitation);
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button saveButton = dialog.findViewById(R.id.saveButton);
        EditText text = dialog.findViewById(R.id.editText);

        cancelButton.setOnClickListener(v -> { dialog.dismiss();});
        saveButton.setOnClickListener(v -> { String fullName = text.getText().toString().trim();
            if (!fullName.isEmpty()) {
                addInvitationToFirestore(event.getId(), fullName);
            } else {
                Toast.makeText(AdminPanelActivity.this, "Неверный ввод", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();});
        dialog.show();
    }

    private void addInvitationToFirestore(String eventId, String fullName) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && eventId != null) {
            String userId = currentUser.getUid();
            Map<String, Object> invitation = new HashMap<>();
            invitation.put("fullName", fullName);
            invitation.put("isAccepted", false);
            invitation.put("qrCodeString","");
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId)
                    .collection("events").document(eventId)
                    .collection("invitations")
                    .add(invitation)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(AdminPanelActivity.this, "Приглашение добавлено", Toast.LENGTH_SHORT).show();;
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AdminPanelActivity.this, "Ошибка при добавлении", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Вы должны авторизоваться", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteEvent(Event event) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            CollectionReference invitationsRef = db.collection("users").document(userId)
                    .collection("events").document(event.getId())
                    .collection("invitations");

            invitationsRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot invitationSnapshot : task.getResult()) {
                        batch.delete(invitationSnapshot.getReference());
                    }

                    // Применение пакета
                    batch.commit().addOnCompleteListener(batchTask -> {
                        if (batchTask.isSuccessful()) {
                            db.collection("users").document(userId).collection("events").document(event.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        db.collection("events").document(event.getId())
                                                .delete()
                                                .addOnSuccessListener(aVoid2 -> {
                                                    Toast.makeText(AdminPanelActivity.this, "Мероприятие и все приглашения удалены", Toast.LENGTH_SHORT).show();
                                                    loadEvents();
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(AdminPanelActivity.this, "Ошибка при удалении мероприятия из основной коллекции", Toast.LENGTH_SHORT).show());
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(AdminPanelActivity.this, "Ошибка при удалении мероприятия из коллекции пользователя", Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(AdminPanelActivity.this, "Ошибка при удалении приглашений", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(AdminPanelActivity.this, "Ошибка при получении приглашений", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Вы должны авторизоваться", Toast.LENGTH_SHORT).show();
        }
    }
}

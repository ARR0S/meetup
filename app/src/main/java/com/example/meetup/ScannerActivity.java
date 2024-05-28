package com.example.meetup;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.budiyev.android.codescanner.AutoFocusMode;
import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import java.util.ArrayList;
import java.util.List;

public class ScannerActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST_PERMISSION = 101;
    private CodeScanner mCodeScanner;
    private FloatingActionButton switchModeFab, enterEventCodeFab, logoutFab;
    private String currentUserCode;
    private String currentEventCode;
    private GoogleSignInClient mGoogleSignInClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        if (savedInstanceState != null) {
            currentEventCode = savedInstanceState.getString("currentEventCode");
        }
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        CodeScannerView scannerView = findViewById(R.id.scanner_view);
        switchModeFab = findViewById(R.id.fab_switch_mode);
        enterEventCodeFab = findViewById(R.id.fab_enter_event_code);
        enterEventCodeFab.setOnClickListener(view -> promptForEventCode());
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_PERMISSION);
        } else {
            setupScanner(scannerView);
        }
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        switchModeFab.setOnClickListener(view -> {
            enterAdminMode();
        });
        logoutFab = findViewById(R.id.fab_logout);
        logoutFab.setOnClickListener(view -> showLogoutDialog());
    }

    private void showLogoutDialog() {
        Dialog dialog = new Dialog(this, R.style.CustomDialog);
        dialog.setContentView(R.layout.dialog_exit);
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button exitButton = dialog.findViewById(R.id.exitButton);

        cancelButton.setOnClickListener(v -> { dialog.dismiss();});
        exitButton.setOnClickListener(v -> { logout();dialog.dismiss();});
        dialog.show();
    }

    private void logout() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            FirebaseAuth.getInstance().signOut();

            startActivity(new Intent(ScannerActivity.this, LoginActivity.class));
            finish();
        });
    }


    private void enterAdminMode() {
        startActivity(new Intent(ScannerActivity.this, AdminPanelActivity.class));
    }

    private void promptForEventCode() {
        Dialog dialog = new Dialog(this, R.style.CustomDialog);
        dialog.setContentView(R.layout.setup_scanner);
        Button cancelButton = dialog.findViewById(R.id.cancelButton);
        Button okButton = dialog.findViewById(R.id.okButton);
        EditText text = dialog.findViewById(R.id.editText);

        cancelButton.setOnClickListener(v -> { dialog.dismiss();});
        okButton.setOnClickListener(v -> { String eventId = text.getText().toString().trim();
            if (!eventId.isEmpty()) {
                checkEventForUser(eventId);
            } else {
                Toast.makeText(ScannerActivity.this, "Введите код мероприятия", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();});
        dialog.show();
    }

    private void checkEventForUser(String eventId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events").document(eventId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String userId = document.getString("userid");
                    if (userId != null) {
                        Toast.makeText(ScannerActivity.this, "Сканер настроен", Toast.LENGTH_SHORT).show();
                        currentUserCode = userId;
                        currentEventCode = eventId;
                    } else {
                        Toast.makeText(ScannerActivity.this, "Ошибка получения данных пользователя", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ScannerActivity.this, "Нет такого мероприятия", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(ScannerActivity.this, "Ошибка", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupScanner(CodeScannerView scannerView) {
        mCodeScanner = new CodeScanner(this, scannerView);
        List<BarcodeFormat> formats = new ArrayList<>();
        formats.add(BarcodeFormat.QR_CODE);
        mCodeScanner.setFormats(formats);
        mCodeScanner.setAutoFocusMode(AutoFocusMode.CONTINUOUS);
        mCodeScanner.setAutoFocusEnabled(true);
        mCodeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull com.google.zxing.Result result) {
                runOnUiThread(() -> {
                    if (currentEventCode == null) {
                        Toast.makeText(ScannerActivity.this, "Сначала заполните код для сканера!", Toast.LENGTH_LONG).show();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            mCodeScanner.startPreview();
                        }, 400);
                        return;
                    }
                    String[] parts = result.getText().split(":");
                    if (parts.length == 2) {
                        String SECRET_KEY = getString(R.string.secret_key);
                        String scannedEventId = null;
                        try {
                            scannedEventId = CryptoUtils.decrypt(parts[0],SECRET_KEY);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        String scannedInvitationId = parts[1];
                        if (scannedEventId.equals(currentEventCode)) {
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            String finalScannedEventId = scannedEventId;
                            db.collection("users").document(currentUserCode).collection("events").document(scannedEventId)
                                    .collection("invitations").document(scannedInvitationId).get().addOnCompleteListener(eventTask -> {
                                if (eventTask.isSuccessful()) {
                                            DocumentReference invitationRef = db.collection("users").document(currentUserCode)
                                                    .collection("events").document(finalScannedEventId)
                                                    .collection("invitations").document(scannedInvitationId);
                                            invitationRef.get().addOnCompleteListener(invitationTask -> {
                                                if (invitationTask.isSuccessful()) {
                                                    DocumentSnapshot invitationDocument = invitationTask.getResult();
                                                    if (invitationDocument.exists() && invitationDocument.getBoolean("isAccepted") != Boolean.TRUE) {
                                                        invitationRef.update("isAccepted", true)
                                                                .addOnSuccessListener(aVoid -> {
                                                                    displayResult(0);
                                                                })
                                                                .addOnFailureListener(e -> displayResult(3));
                                                    } else if (invitationDocument.getBoolean("isAccepted") == Boolean.TRUE) {
                                                        displayResult(2);
                                                    };
                                                } else {
                                                    displayResult(3);
                                                }
                                            });
                                } else {
                                    displayResult(3);
                                }
                            });
                        } else {
                            displayResult(4);
                        }
                    } else {
                        displayResult(4);
                    }
                });
            }
        });
        scannerView.setOnClickListener(view -> mCodeScanner.startPreview());
    }


    private void displayResult(int result) {
        FrameLayout colorOverlay = findViewById(R.id.color_overlay);
        ImageView resultIcon = findViewById(R.id.result_icon);
        MediaPlayer mediaPlayer;

        if (result == 0) {
            colorOverlay.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            resultIcon.setImageResource(R.drawable.ic_check);
            mediaPlayer = MediaPlayer.create(this, R.raw.succes);
        } else if (result == 1) {
            colorOverlay.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            resultIcon.setImageResource(R.drawable.ic_cross);
            mediaPlayer = MediaPlayer.create(this, R.raw.fail);
        } else if (result == 2) {
            colorOverlay.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
            resultIcon.setImageResource(R.drawable.ic_check);
            mediaPlayer = MediaPlayer.create(this, R.raw.warning);
        } else if (result == 3) {
            colorOverlay.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            resultIcon.setImageResource(R.drawable.ic_cross);
            mediaPlayer = MediaPlayer.create(this, R.raw.fail);
        } else if (result == 4) {
            colorOverlay.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            resultIcon.setImageResource(R.drawable.ic_cross);
            mediaPlayer = MediaPlayer.create(this, R.raw.fail);
        } else {
            mediaPlayer = null;
        }

        if (mediaPlayer != null) {
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
            });
        }

        colorOverlay.setAlpha(0f);
        colorOverlay.setVisibility(View.VISIBLE);
        colorOverlay.animate().alpha(1f).setDuration(500).setListener(null);

        resultIcon.setAlpha(0f);
        resultIcon.setVisibility(View.VISIBLE);
        resultIcon.animate().alpha(1f).setDuration(500).setListener(null);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            mCodeScanner.startPreview();
        }, 1900);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            colorOverlay.animate().alpha(0f).setDuration(500).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    colorOverlay.setVisibility(View.GONE);
                }
            });

            resultIcon.animate().alpha(0f).setDuration(500).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    resultIcon.setVisibility(View.GONE);
                }
            });
        }, 2000);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mCodeScanner != null) {
            mCodeScanner.startPreview();
        }
    }

    @Override
    protected void onPause() {
        if (mCodeScanner != null) {
            mCodeScanner.releaseResources();
        }
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            CodeScannerView scannerView = findViewById(R.id.scanner_view);
            setupScanner(scannerView);
        } else {
            Toast.makeText(this, "Camera permission is required to use QR Scanner", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("currentEventCode", currentEventCode);
    }
}

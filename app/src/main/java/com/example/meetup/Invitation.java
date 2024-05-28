package com.example.meetup;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.util.Hashtable;

public class Invitation{
    private String id;
    private String fullName;
    private String qrCodeString;

    private boolean isAccepted;

    public Invitation() {
    }

    public Invitation(String fullName, String qrCodeString) {
        this.fullName = fullName;
        this.qrCodeString = qrCodeString;
        this.isAccepted = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean getIsAccepted() {
        return isAccepted;
    }

    public String getQrCodeString() {
        return qrCodeString;
    }

    public void setQrCodeString(String qrCodeString) {
        this.qrCodeString = qrCodeString;
    }

    public static String generateQRCodeString(Context context, String eventId, String invitationId) {
        String SECRET_KEY = context.getString(R.string.secret_key);
        try {
            String encryptedEventId = CryptoUtils.encrypt(eventId, SECRET_KEY);
            String data = encryptedEventId + ":" + invitationId;
            Bitmap bitmap = encodeAsBitmap(data, BarcodeFormat.QR_CODE, 1000, 1000);
            return bitmapToBase64(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap encodeAsBitmap(String data, BarcodeFormat format, int width, int height) throws WriterException {
        BitMatrix result;
        try {
            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            result = new MultiFormatWriter().encode(data, format, width, height, hints);
        } catch (IllegalArgumentException iae) {
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, w, h);
        return bitmap;
    }

    private static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
}



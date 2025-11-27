package com.quantiagents.app.ui.ScanQRCode;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.QRCodeService;
import com.quantiagents.app.models.QRCode;
import com.quantiagents.app.ui.CreateEventFragment;
import com.quantiagents.app.ui.main.MainActivity;


public class ScanQRCodeFragment extends Fragment {

    private EventService _eventService;
    private QRCodeService _qrCodeService;

    public static ScanQRCodeFragment newInstance(){
        return new ScanQRCodeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan_q_r_code, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize services
        App app = (App) requireActivity().getApplication();
        _eventService = app.locator().eventService();
        _qrCodeService = app.locator().qrCodeService();

        bindViews(view);

        // Set UP Onclick Listeners

    }

    private void bindViews(View view)
    {

    }



}
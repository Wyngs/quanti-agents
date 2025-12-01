package com.quantiagents.app.ui.ScanQRCode;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.quantiagents.app.App;
import com.quantiagents.app.R;
import com.quantiagents.app.Services.EventService;
import com.quantiagents.app.Services.QRCodeService;
import com.quantiagents.app.Services.ServiceLocator;
import com.quantiagents.app.models.Event;
import com.quantiagents.app.models.QRCode;
import com.quantiagents.app.ui.ViewEventDetailsFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ExperimentalGetImage
@SuppressWarnings("UnsafeOptInUsageError")
public class ScanQRCodeFragment extends Fragment {

    private EventService eventService;
    private QRCodeService qrCodeService;

    // Views
    private PreviewView previewView;
    private TextInputEditText qrCodeInput;
    private MaterialCardView errorCard;
    private TextView errorText;
    private MaterialCardView quickAccessCard;
    private RecyclerView eventsRecyclerView;

    // Camera and scanning
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ImageAnalysis imageAnalysis;
    private ExecutorService cameraExecutor;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // State
    private boolean isScanning = false;
    private long lastScanTime = 0;
    private static final long SCAN_THROTTLE_MS = 2000; // Prevent duplicate scans

    // Quick Access
    private QRQuickAccessAdapter quickAccessAdapter;

    /**
     * Creates a new instance of ScanQRCodeFragment.
     *
     * @return A new ScanQRCodeFragment instance
     */
    public static ScanQRCodeFragment newInstance() {
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
        ServiceLocator locator = app.locator();
        eventService = locator.eventService();
        qrCodeService = locator.qrCodeService();

        bindViews(view);
        setupClickListeners();
        setupPermissionLauncher();
        setupQuickAccess();

        // Start camera if permission granted
        if (hasCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }

        // Load quick access events
        loadQuickAccessEvents();
    }

    private void bindViews(View view) {
        previewView = view.findViewById(R.id.previewView);
        qrCodeInput = view.findViewById(R.id.qrCodeInput);
        errorCard = view.findViewById(R.id.errorCard);
        errorText = view.findViewById(R.id.errorText);
        quickAccessCard = view.findViewById(R.id.quickAccessCard);
        eventsRecyclerView = view.findViewById(R.id.eventsRecyclerView);
    }

    private void setupClickListeners() {
        // Search button click
        View view = getView();
        if (view != null) {
            View searchButton = view.findViewById(R.id.searchButton);
            if (searchButton != null) {
                searchButton.setOnClickListener(v -> handleManualSearch());
            }
        }

        // Manual input Enter key
        if (qrCodeInput != null) {
            qrCodeInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getAction() == android.view.KeyEvent.ACTION_DOWN &&
                     event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                    handleManualSearch();
                    return true;
                }
                return false;
            });
        }
    }

    private void setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startCamera();
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.scan_qr_permission_required), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupQuickAccess() {
        quickAccessAdapter = new QRQuickAccessAdapter((event, qrCodeValue) -> {
            // Simulate scanning by directly finding the event
            onEventFound(event.getEventId());
        });
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        eventsRecyclerView.setAdapter(quickAccessAdapter);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void startCamera() {
        if (cameraExecutor == null) {
            cameraExecutor = Executors.newSingleThreadExecutor();
        }

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider provider = cameraProviderFuture.get();
                bindCameraUseCases(provider);
            } catch (Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), getString(R.string.scan_qr_camera_error), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        // Unbind use cases before rebinding
        if (this.cameraProvider != null) {
            this.cameraProvider.unbindAll();
        }
        this.cameraProvider = cameraProvider;

        // Preview
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image Analysis for QR code scanning
        ImageAnalysis.Builder imageAnalysisBuilder = new ImageAnalysis.Builder();
        imageAnalysis = imageAnalysisBuilder.build();
        imageAnalysis.setAnalyzer(cameraExecutor, new QRCodeAnalyzer());

        // Camera selector - use back camera
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll();

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
            );
        } catch (Exception e) {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), getString(R.string.scan_qr_camera_error), Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    @ExperimentalGetImage
    private class QRCodeAnalyzer implements ImageAnalysis.Analyzer {
        private final com.google.mlkit.vision.barcode.BarcodeScanner barcodeScanner;

        QRCodeAnalyzer() {
            barcodeScanner = BarcodeScanning.getClient();
        }

        @Override
        public void analyze(@NonNull androidx.camera.core.ImageProxy image) {
            if (isScanning) {
                image.close();
                return;
            }

            // Throttle scans to avoid duplicate processing
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastScanTime < SCAN_THROTTLE_MS) {
                image.close();
                return;
            }

            InputImage inputImage = createInputImageFromProxy(image);

            isScanning = true;
            barcodeScanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty() && !TextUtils.isEmpty(barcodes.get(0).getRawValue())) {
                            String qrValue = barcodes.get(0).getRawValue();
                            lastScanTime = currentTime;
                            handleQRCodeScanned(qrValue);
                        }
                        isScanning = false;
                        image.close();
                    })
                    .addOnFailureListener(e -> {
                        isScanning = false;
                        image.close();
                    });
        }
        
        @ExperimentalGetImage
        private InputImage createInputImageFromProxy(androidx.camera.core.ImageProxy image) {
            return InputImage.fromMediaImage(
                    image.getImage(),
                    image.getImageInfo().getRotationDegrees()
            );
        }
    }

    private void handleQRCodeScanned(String qrCodeValue) {
        if (!isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            // Update input field with scanned value
            if (qrCodeInput != null) {
                qrCodeInput.setText(qrCodeValue);
            }
            // Find and navigate to event
            findEventByQRCode(qrCodeValue);
        });
    }

    private void handleManualSearch() {
        if (qrCodeInput == null) return;

        String qrCodeValue = qrCodeInput.getText() != null ? qrCodeInput.getText().toString().trim() : "";
        if (TextUtils.isEmpty(qrCodeValue)) {
            showError(getString(R.string.scan_qr_error_not_found));
            return;
        }

        hideError();
        findEventByQRCode(qrCodeValue);
    }

    private void findEventByQRCode(String qrCodeValue) {
        // Find QR code by value, then get the event
        new Thread(() -> {
            try {
                // Get all QR codes and find matching one
                List<QRCode> allQRCodes = qrCodeService.getAllQRCodes();
                QRCode foundQR = null;

                for (QRCode qr : allQRCodes) {
                    if (qr != null && qrCodeValue.equals(qr.getQrCodeValue())) {
                        foundQR = qr;
                        break;
                    }
                }

                if (foundQR != null && !TextUtils.isEmpty(foundQR.getEventId())) {
                    // Found QR code, get the event
                    final String eventId = foundQR.getEventId();
                    Event event = eventService.getEventById(eventId);

                    if (event != null && isAdded()) {
                        requireActivity().runOnUiThread(() -> onEventFound(eventId));
                    } else if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            showError(getString(R.string.scan_qr_error_not_found));
                        });
                    }
                } else if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        showError(getString(R.string.scan_qr_error_not_found));
                    });
                }
            } catch (Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        showError(getString(R.string.scan_qr_error_not_found));
                    });
                }
            }
        }).start();
    }

    private void onEventFound(String eventId) {
        hideError();
        // Navigate to event details
        Fragment eventDetailsFragment = ViewEventDetailsFragment.newInstance(eventId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_container, eventDetailsFragment)
                .addToBackStack(null)
                .commit();
    }

    private void loadQuickAccessEvents() {
        // Load all events and their QR codes for quick access
        eventService.getAllEvents(
                events -> {
                    if (!isAdded() || events == null || events.isEmpty()) {
                        return;
                    }

                    // Load QR codes for each event
                    new Thread(() -> {
                        List<QRQuickAccessAdapter.EventQRPair> pairs = new ArrayList<>();
                        for (Event event : events) {
                            if (event == null || TextUtils.isEmpty(event.getEventId())) continue;

                            List<QRCode> qrCodes = qrCodeService.getQRCodesByEventId(event.getEventId());
                            if (!qrCodes.isEmpty() && qrCodes.get(0) != null) {
                                String qrValue = qrCodes.get(0).getQrCodeValue();
                                if (!TextUtils.isEmpty(qrValue)) {
                                    pairs.add(new QRQuickAccessAdapter.EventQRPair(event, qrValue));
                                }
                            }
                        }

                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                if (!pairs.isEmpty()) {
                                    quickAccessAdapter.setItems(pairs);
                                    quickAccessCard.setVisibility(View.VISIBLE);
                                } else {
                                    quickAccessCard.setVisibility(View.GONE);
                                }
                            });
                        }
                    }).start();
                },
                e -> {
                    // Error loading events - just hide quick access
                    if (isAdded()) {
                        quickAccessCard.setVisibility(View.GONE);
                    }
                }
        );
    }

    private void showError(String message) {
        if (errorCard != null && errorText != null) {
            errorText.setText(message);
            errorCard.setVisibility(View.VISIBLE);
        }
    }

    private void hideError() {
        if (errorCard != null) {
            errorCard.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
            cameraExecutor = null;
        }
        isScanning = false;
    }
}

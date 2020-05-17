package jp.co.recruit_lifestyle.sample.fragment;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import jp.co.recruit.floatingview.R;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewManager;
import jp.co.recruit_lifestyle.sample.service.ChatHeadService;
import jp.co.recruit_lifestyle.sample.service.CustomFloatingViewService;
import jp.co.recruit_lifestyle.sample.service.FloatingViewService;

public class FloatingViewControlFragment extends Fragment {

    private static final String TAG = "FloatingViewControl";

    private static final int CHATHEAD_OVERLAY_PERMISSION_REQUEST_CODE = 100;

    private static final int CUSTOM_OVERLAY_PERMISSION_REQUEST_CODE = 101;

    private static final int MENU_OVERLAY_PERMISSION_REQUEST_CODE = 102;

    public static boolean created = false;

    public static FloatingViewControlFragment newInstance() {
        final FloatingViewControlFragment fragment = new FloatingViewControlFragment();
        return fragment;
    }

    public FloatingViewControlFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_floating_view_control, container, false);

        //showFloatingView(getActivity(), true, 0);
        showFloatingView(getActivity(), true, 2);

        //startService(new Intent(MainActivity.this, FloatingViewService.class));
        /*
        rootView.findViewById(R.id.show_demo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFloatingView(getActivity(), true, false);
            }
        });

        rootView.findViewById(R.id.show_customized_demo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFloatingView(getActivity(), true, true);
            }
        });

        rootView.findViewById(R.id.show_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.container, FloatingViewSettingsFragment.newInstance());
                ft.addToBackStack(null);
                ft.commit();
            }
        });


         */
        return rootView;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHATHEAD_OVERLAY_PERMISSION_REQUEST_CODE) {
            showFloatingView(getActivity(), false, 0);
        } else if (requestCode == CUSTOM_OVERLAY_PERMISSION_REQUEST_CODE) {
            showFloatingView(getActivity(), false, 1);
        }
        else if (requestCode == MENU_OVERLAY_PERMISSION_REQUEST_CODE){
            showFloatingView(getActivity(), false, 2);
        }
    }

    @SuppressLint("NewApi")
    public void showFloatingView(Context context, boolean isShowOverlayPermission, int floatingViewNo) {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            startFloatingViewService(getActivity(), floatingViewNo);
            return;
        }

        if (Settings.canDrawOverlays(context)) {
            startFloatingViewService(getActivity(), floatingViewNo);
            return;
        }

        if (isShowOverlayPermission) {
            final Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
            switch(floatingViewNo){
                case 0:
                    startActivityForResult(intent, CHATHEAD_OVERLAY_PERMISSION_REQUEST_CODE);
                    break;
                case 1:
                    startActivityForResult(intent, CUSTOM_OVERLAY_PERMISSION_REQUEST_CODE);
                    break;
                default:
                    startActivityForResult(intent, MENU_OVERLAY_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private static void startFloatingViewService(Activity activity, int floatingViewNo) {
        if (!created) {
            // *** You must follow these rules when obtain the cutout(FloatingViewManager.findCutoutSafeArea) ***
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // 1. 'windowLayoutInDisplayCutoutMode' do not be set to 'never'
                if (activity.getWindow().getAttributes().layoutInDisplayCutoutMode == WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER) {
                    throw new RuntimeException("'windowLayoutInDisplayCutoutMode' do not be set to 'never'");
                }
            }

            // launch service
            Class<? extends Service> service;
            String key;
            switch (floatingViewNo) {
                case 0:
                    service = ChatHeadService.class;
                    key = ChatHeadService.EXTRA_CUTOUT_SAFE_AREA;
                    break;
                case 1:
                    service = CustomFloatingViewService.class;
                    key = CustomFloatingViewService.EXTRA_CUTOUT_SAFE_AREA;
                    break;
                default:
                    service = FloatingViewService.class;
                    key = FloatingViewService.EXTRA_CUTOUT_SAFE_AREA;
            }
        /*
        if (isCustomFloatingView) {
            service = CustomFloatingViewService.class;
            key = CustomFloatingViewService.EXTRA_CUTOUT_SAFE_AREA;
        } else {
            service = ChatHeadService.class;
            key = ChatHeadService.EXTRA_CUTOUT_SAFE_AREA;
        }

         */
            final Intent intent = new Intent(activity, service);
            intent.putExtra(key, FloatingViewManager.findCutoutSafeArea(activity));
            ContextCompat.startForegroundService(activity, intent);
            created = true;
        }
    }
}
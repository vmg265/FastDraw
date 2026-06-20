package com.vmg265.fastdrawplus.fork.dialogs;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

import com.vmg265.fastdrawplus.fork.R;
import com.vmg265.fastdrawplus.fork.activities.MainActivity;
import com.vmg265.fastdrawplus.fork.iconpack.IconPackManager;

public class IconPickerBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_COMPONENT_NAME = "component_name";

    private ComponentName componentName;
    private IconPackManager iconPackManager;
    private ListView packList;
    private GridView iconGrid;

    public static IconPickerBottomSheet newInstance(ComponentName componentName) {
        IconPickerBottomSheet fragment = new IconPickerBottomSheet();
        Bundle args = new Bundle();
        args.putParcelable(ARG_COMPONENT_NAME, componentName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            componentName = getArguments().getParcelable(ARG_COMPONENT_NAME);
        }
        iconPackManager = IconPackManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // We will create views programmatically to save time/complexity.
        Context context = requireContext();
        
        ViewGroup root = new android.widget.LinearLayout(context);
        ((android.widget.LinearLayout) root).setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        TextView title = new TextView(context);
        title.setText(R.string.select_icon);
        title.setTextSize(18);
        title.setPadding(0, 0, 0, 32);
        root.addView(title);

        packList = new ListView(context);
        iconGrid = new GridView(context);
        iconGrid.setNumColumns(4);
        iconGrid.setVerticalSpacing(16);
        iconGrid.setHorizontalSpacing(16);
        iconGrid.setVisibility(View.GONE);

        root.addView(packList, new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        root.addView(iconGrid, new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        loadPacks();

        return root;
    }

    private void loadPacks() {
        packList.setVisibility(View.VISIBLE);
        iconGrid.setVisibility(View.GONE);
        List<String> packs = iconPackManager.getAvailableIconPacks();
        packs.add(0, ""); // Reset

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, packs) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                String pack = getItem(position);
                if (pack == null || pack.isEmpty()) {
                    tv.setText("Reset to Default");
                } else {
                    tv.setText(iconPackManager.getIconPackLabel(pack));
                }
                return view;
            }
        };

        packList.setAdapter(adapter);
        packList.setOnItemClickListener((parent, view, position, id) -> {
            String pack = packs.get(position);
            if (pack.isEmpty()) {
                saveSelection(null);
            } else {
                loadIcons(pack);
            }
        });
    }

    private void loadIcons(String pack) {
        packList.setVisibility(View.GONE);
        iconGrid.setVisibility(View.VISIBLE);
        List<String> drawables = iconPackManager.getAllDrawables(pack);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), 0, drawables) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                ImageView iv;
                if (convertView == null) {
                    iv = new ImageView(requireContext());
                    int size = (int) (48 * getResources().getDisplayMetrics().density);
                    iv.setLayoutParams(new GridView.LayoutParams(size, size));
                    iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                } else {
                    iv = (ImageView) convertView;
                }
                String drawableName = getItem(position);
                Drawable d = iconPackManager.getDrawable(pack, drawableName);
                iv.setImageDrawable(d);
                return iv;
            }
        };

        iconGrid.setAdapter(adapter);
        iconGrid.setOnItemClickListener((parent, view, position, id) -> {
            String drawableName = drawables.get(position);
            saveSelection(pack + "|" + drawableName);
        });
    }

    private void saveSelection(String value) {
        SharedPreferences perAppPrefs = requireContext().getSharedPreferences("per_app_icons", Context.MODE_PRIVATE);
        if (value == null) {
            perAppPrefs.edit().remove(componentName.flattenToString()).apply();
        } else {
            perAppPrefs.edit().putString(componentName.flattenToString(), value).apply();
        }
        dismiss();
        MainActivity.forceFinish(); // To force restart and reload
    }
}

package com.vmg265.fastdrawplus.fork.launcher;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Context;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import com.vmg265.fastdrawplus.fork.Postable;
import com.vmg265.fastdrawplus.fork.R;
import com.vmg265.fastdrawplus.fork.launcher.displayitem.DisplayItem;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ItemViewHolder> {
    private final LaunchManager launchManager;
    private final Postable dragEndService;
    private final SortedList<DisplayItem> items;

    public CategoryAdapter(@NonNull final LaunchManager launchManager, final Postable dragEndService) {
        this.launchManager = launchManager;
        this.dragEndService = dragEndService;
        this.items = new SortedList<>(DisplayItem.class, new SortedList.Callback<>() {
            @Override
            public int compare(final DisplayItem o1, final DisplayItem o2) {
                return o1.compareTo(o2);
            }

            @Override
            public void onChanged(final int position, final int count) {
                notifyItemRangeChanged(position, count);
            }

            @Override
            public boolean areContentsTheSame(final DisplayItem oldItem, final DisplayItem newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areItemsTheSame(final DisplayItem item1, final DisplayItem item2) {
                return item1.id.equals(item2.id);
            }

            @Override
            public void onInserted(final int position, final int count) {
                notifyItemRangeInserted(position, count);
            }

            @Override
            public void onRemoved(final int position, final int count) {
                notifyItemRangeRemoved(position, count);
            }

            @Override
            public void onMoved(final int fromPosition, final int toPosition) {
                notifyItemMoved(fromPosition, toPosition);
            }
        });
    }

    SortedList<DisplayItem> getItems() {
        return items;
    }

    @Override
    @NonNull
    public ItemViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_item, parent, false);
        return new CategoryAdapter.ItemViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final ItemViewHolder holder, final int position) {
        holder.bind(items.get(position), launchManager, dragEndService);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final View view;
        private final TextView label;
        private final ImageView icon;

        ItemViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.view = itemView;
            this.label = itemView.findViewById(R.id.app_item_name);
            this.icon = itemView.findViewById(R.id.app_item_icon);
        }

        @SuppressLint("ClickableViewAccessibility")
        void bind(final DisplayItem item, final LaunchManager launchManager, final Postable dragEndService) {
            final Context context = view.getContext();
            com.vmg265.fastdrawplus.fork.prefs.Preferences prefs = new com.vmg265.fastdrawplus.fork.prefs.Preferences(context);

            if (prefs.hideAppTitles) {
                label.setVisibility(View.GONE);
            } else {
                label.setVisibility(View.VISIBLE);
                label.setText(item.label);
            }

            ViewGroup.LayoutParams lp = icon.getLayoutParams();
            int baseSize = context.getResources().getDimensionPixelSize(R.dimen.app_item_icon_size);
            lp.width = (int) (baseSize * prefs.iconSizeMultiplier);
            lp.height = (int) (baseSize * prefs.iconSizeMultiplier);
            icon.setLayoutParams(lp);

            android.graphics.drawable.Drawable displayIcon = item.icon;
            if (item.source instanceof com.vmg265.fastdrawplus.fork.launcher.launcheritem.AppItem) {
                com.vmg265.fastdrawplus.fork.launcher.launcheritem.AppItem appItem = (com.vmg265.fastdrawplus.fork.launcher.launcheritem.AppItem) item.source;
                android.content.ComponentName componentName = appItem.getComponentName();
                
                android.content.SharedPreferences perAppPrefs = context.getSharedPreferences("per_app_icons", Context.MODE_PRIVATE);
                String customIcon = perAppPrefs.getString(componentName.flattenToString(), null);
                
                com.vmg265.fastdrawplus.fork.iconpack.IconPackManager iconPackManager = com.vmg265.fastdrawplus.fork.iconpack.IconPackManager.getInstance(context);
                if (customIcon != null && customIcon.contains("|")) {
                    String[] parts = customIcon.split("\\|");
                    android.graphics.drawable.Drawable customD = iconPackManager.getDrawable(parts[0], parts[1]);
                    if (customD != null) {
                        displayIcon = customD;
                    }
                } else if (prefs.globalIconPack != null && !prefs.globalIconPack.isEmpty()) {
                    android.graphics.drawable.Drawable packIcon = iconPackManager.getIcon(prefs.globalIconPack, componentName);
                    if (packIcon != null) {
                        displayIcon = packIcon;
                    }
                }
            }
            icon.setImageDrawable(displayIcon);

            view.setOnClickListener(view -> {
                final ActivityOptions opts;
                opts = ActivityOptions.makeClipRevealAnimation(view, 0, 0, view.getWidth(), view.getHeight());

                final Context onClickContext = view.getContext();

                try {
                    item.launchable.launch(onClickContext, launchManager, opts.toBundle(), view.getClipBounds());
                } catch (final Exception e) {
                    Log.e("OreoShortcutLaunchable", "Failed to launch shortcut " + item.id, e);
                    Toast.makeText(context, R.string.error_launch_exception, Toast.LENGTH_LONG).show();
                }
            });

            final PointF touchPoint = new PointF();
            view.setOnTouchListener((view, event) -> {
                touchPoint.set(event.getX(), event.getY());
                return false;
            });
            view.setOnLongClickListener(view -> {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

                final float x = view.getX() + touchPoint.x;
                final float y = view.getY() + touchPoint.y;

                // start drag
                final View.DragShadowBuilder shadow = new OffsetDragShadowBuilder(view, x, y);
                if (view.startDragAndDrop(null, shadow, item.source, 0)) {
                    final Paint silhouettePaint = new Paint();
                    silhouettePaint.setColorFilter(new LightingColorFilter(Color.BLACK, Color.BLACK));
                    view.setLayerType(View.LAYER_TYPE_SOFTWARE, silhouettePaint);
                }

                dragEndService.post(()-> view.setLayerType(View.LAYER_TYPE_NONE, null)); // avoid creating an OnDragListener to prevent stealing ACTION_DROP from android.R.id.content

                return false;
            });
        }
    }
}

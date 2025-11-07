package com.quantiagents.app.testutil;

import android.content.res.Resources;
import android.view.View;
import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public final class RecyclerViewMatcher {
    private final int recyclerViewId;
    public RecyclerViewMatcher(@IdRes int recyclerViewId) { this.recyclerViewId = recyclerViewId; }

    public Matcher<View> atPosition(final int position) {
        return atPositionOnView(position, -1);
    }

    public Matcher<View> atPositionOnView(final int position, @IdRes final int targetViewId) {
        return new TypeSafeMatcher<View>() {
            Resources resources; View childView;

            @Override public void describeTo(Description d) {
                String idDesc = Integer.toString(recyclerViewId);
                if (resources != null) {
                    try { idDesc = resources.getResourceName(recyclerViewId); } catch (Resources.NotFoundException ignored) {}
                }
                d.appendText("with id: " + idDesc + " at position: " + position);
            }

            @Override public boolean matchesSafely(View view) {
                resources = view.getResources();
                if (childView == null) {
                    RecyclerView rv = view.getRootView().findViewById(recyclerViewId);
                    if (rv == null || rv.getId() != recyclerViewId) return false;
                    RecyclerView.ViewHolder vh = rv.findViewHolderForAdapterPosition(position);
                    if (vh == null) return false;
                    childView = vh.itemView;
                }
                if (targetViewId == -1) return view == childView;
                View target = childView.findViewById(targetViewId);
                return view == target;
            }
        };
    }
}

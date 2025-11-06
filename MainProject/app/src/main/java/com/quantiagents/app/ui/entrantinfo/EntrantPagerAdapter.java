package com.quantiagents.app.ui.entrantinfo;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Pager adapter that provides 4 pages to {@link androidx.viewpager2.widget.ViewPager2}:
 * Waiting, Selected, Confirmed, Canceled. Each page is an {@link EntrantListFragment}
 * configured with the target status.
 *
 * Keeping this tiny lets most behavior live inside the list fragment.
 */
public class EntrantPagerAdapter extends FragmentStateAdapter {

    private final String eventId;

    /**
     * @param host    Fragment host that owns this adapter (for child FragmentManager).
     * @param eventId Event identifier to pass into each page.
     */
    public EntrantPagerAdapter(@NonNull Fragment host, @NonNull String eventId) {
        super(host);
        this.eventId = eventId;
    }

    /**
     * Creates one {@link EntrantListFragment} per position with the correct status.
     *
     * @param position 0..3 mapping to WAITING, SELECTED, CONFIRMED, CANCELED.
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        final String status = switch (position) {
            case 1 -> "SELECTED";
            case 2 -> "CONFIRMED";
            case 3 -> "CANCELED";
            default -> "WAITING";
        };
        return EntrantListFragment.newInstance(eventId, status);
    }

    /** Fixed page count: four statuses. */
    @Override
    public int getItemCount() {
        return 4;
    }
}

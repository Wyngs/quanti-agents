package com.quantiagents.app.ui.manageeventinfo;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Pager adapter for the manage-event-info screen. Provides four pages
 * (WAITING, SELECTED, CONFIRMED, CANCELED) scoped to the same event.
 */
public class ManageEventInfoPagerAdapter extends FragmentStateAdapter {

    private final String eventId;

    /**
     * Constructs the adapter using the host fragment (so it uses the host's child FragmentManager).
     *
     * @param host    Fragment host that owns this adapter.
     * @param eventId Event identifier to pass to each page.
     */
    public ManageEventInfoPagerAdapter(@NonNull Fragment host, @NonNull String eventId) {
        super(host);
        this.eventId = eventId;
    }

    /**
     * Creates the page fragment for a given position.
     * Position 0 = WAITLIST, 1 = SELECTED, 2 = CONFIRMED, 3 = CANCELLED.
     *
     * @param position The position of the page (0-3)
     * @return A new ManageEventInfoListFragment for the specified status
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        final String status = switch (position) {
            case 1 -> "SELECTED";
            case 2 -> "CONFIRMED";
            case 3 -> "CANCELLED";
            default -> "WAITLIST";
        };
        return ManageEventInfoListFragment.newInstance(eventId, status);
    }

    /**
     * Returns the fixed page count: 4 statuses (WAITLIST, SELECTED, CONFIRMED, CANCELLED).
     *
     * @return The number of pages (4)
     */
    @Override
    public int getItemCount() { return 4; }
}